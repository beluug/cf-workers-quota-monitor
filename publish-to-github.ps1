$ErrorActionPreference = "Stop"

$repoName = "cf-workers-quota-monitor"
$repoDescription = "A secure, local-first Android and Windows dashboard for Cloudflare Workers daily quota"
$releaseDir = Join-Path $PSScriptRoot "release"
$apkFiles = @(Get-ChildItem -LiteralPath $releaseDir -Filter "*.apk" -File)
$checksumPath = Join-Path $releaseDir "SHA256SUMS.txt"
$windowsChecksumPath = Join-Path $releaseDir "SHA256SUMS-Windows.txt"
$releaseNotesPath = Join-Path $releaseDir "RELEASE_NOTES.md"
$installPath = Join-Path $releaseDir "INSTALL.txt"

Set-Location $PSScriptRoot
$proxyUrl = "http://127.0.0.1:7897"
if (-not $env:HTTPS_PROXY -and (Test-NetConnection 127.0.0.1 -Port 7897 -InformationLevel Quiet -WarningAction SilentlyContinue)) {
    $env:HTTP_PROXY = $proxyUrl
    $env:HTTPS_PROXY = $proxyUrl
    Write-Host "Using the detected Clash proxy at $proxyUrl" -ForegroundColor Cyan
}
$repoPath = (Resolve-Path $PSScriptRoot).Path.Replace('\', '/')
$safeDirectories = @(git config --global --get-all safe.directory 2>$null)
if ($safeDirectories -notcontains $repoPath) {
    git config --global --add safe.directory $repoPath
}

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI was not found. Run: winget install --id GitHub.cli --exact"
}

gh auth status | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "GitHub CLI is not signed in. Run: gh auth login"
}

if ($apkFiles.Count -ne 1) {
    throw "Expected exactly one APK in the release directory, found $($apkFiles.Count)."
}
$windowsSetup = @(Get-ChildItem -LiteralPath $releaseDir -Filter "CF-Quota-Monitor-v1.0.1-Windows-*-Setup.exe" -File)
$windowsPortable = @(Get-ChildItem -LiteralPath $releaseDir -Filter "CF-Quota-Monitor-v1.0.1-Windows-*-Portable.zip" -File)
if ($windowsSetup.Count -ne 2 -or $windowsPortable.Count -ne 2) {
    throw "Expected x64 and arm64 Windows setup/portable files. Run .\build-windows.ps1 -Architecture all first."
}

foreach ($requiredFile in @($checksumPath, $windowsChecksumPath, $releaseNotesPath, $installPath)) {
    if (-not (Test-Path -LiteralPath $requiredFile)) {
        throw "Missing release file: $requiredFile"
    }
}

$login = (gh api user --jq .login).Trim()
if ([string]::IsNullOrWhiteSpace($login)) {
    throw "Unable to read the GitHub username."
}

if (-not (Test-Path -LiteralPath ".git")) {
    git init
}

git branch -M main
git config user.name $login
git config user.email "$login@users.noreply.github.com"
git add .

$stagedFiles = @(git diff --cached --name-only)
$blocked = @($stagedFiles | Where-Object {
    $_ -match '(^|/)(\.signing|local\.properties|keystore\.properties)(/|$)' -or
    $_ -match '\.(jks|keystore|apk)$' -or
    $_ -match '(^|/)(build|\.gradle)/'
})

if ($blocked.Count -gt 0) {
    $blocked | ForEach-Object { Write-Host "Blocked file: $_" -ForegroundColor Red }
    throw "A private or generated file was staged. Publishing has stopped."
}

if ($stagedFiles.Count -eq 0) {
    throw "No source files are available to commit."
}

Write-Host "Committing $($stagedFiles.Count) public files. Signing keys, local settings and release binaries are excluded." -ForegroundColor Green
git commit -m "Fix Cloudflare response parsing on Windows"

$origin = git remote get-url origin 2>$null
if ([string]::IsNullOrWhiteSpace($origin)) {
    gh repo create $repoName --public --source . --remote origin --push --description $repoDescription
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create or push the GitHub repository."
    }
} else {
    git push -u origin main
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to push the main branch."
    }
}

$releaseAssets = @($apkFiles[0].FullName) + @($windowsSetup.FullName) + @($windowsPortable.FullName) + @($checksumPath, $windowsChecksumPath, $installPath)
$savedErrorPreference = $ErrorActionPreference
$ErrorActionPreference = 'SilentlyContinue'
gh release view v1.3.1 *> $null
$releaseExists = $LASTEXITCODE -eq 0
$ErrorActionPreference = $savedErrorPreference
if ($releaseExists) {
    gh release upload v1.3.1 @releaseAssets --clobber
    gh release edit v1.3.1 --title "CF Quota Monitor | Android v1.3.0 | Windows v1.0.1" --notes-file $releaseNotesPath
} else {
    gh release create v1.3.1 @releaseAssets --title "CF Quota Monitor | Android v1.3.0 | Windows v1.0.1" --notes-file $releaseNotesPath
}
if ($LASTEXITCODE -ne 0) { throw "Failed to create or update the GitHub Release." }

Write-Host "Published: https://github.com/$login/$repoName" -ForegroundColor Green
