$ErrorActionPreference = "Stop"

$repoName = "cf-workers-quota-monitor"
$repoDescription = "A secure, local-first Android dashboard for Cloudflare Workers daily quota"
$releaseDir = Join-Path $PSScriptRoot "release"
$apkFiles = @(Get-ChildItem -LiteralPath $releaseDir -Filter "*.apk" -File)
$checksumPath = Join-Path $releaseDir "SHA256SUMS.txt"
$releaseNotesPath = Join-Path $releaseDir "RELEASE_NOTES.md"

Set-Location $PSScriptRoot
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
$apkPath = $apkFiles[0].FullName

foreach ($requiredFile in @($checksumPath, $releaseNotesPath)) {
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

Write-Host "Committing $($stagedFiles.Count) public files. Signing keys, local settings and APK files are excluded." -ForegroundColor Green
git commit -m "Initial release: CF Quota Monitor v1.0.0"

gh repo create $repoName --public --source . --remote origin --push --description $repoDescription
if ($LASTEXITCODE -ne 0) {
    throw "Failed to create or push the GitHub repository."
}

gh release create v1.0.0 $apkPath $checksumPath --title "CF Quota Monitor v1.0.0" --notes-file $releaseNotesPath
if ($LASTEXITCODE -ne 0) {
    throw "Failed to create the GitHub Release."
}

Write-Host "Published: https://github.com/$login/$repoName" -ForegroundColor Green
