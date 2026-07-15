param(
    [ValidateSet('x64', 'arm64', 'all')]
    [string]$Architecture = 'all'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$project = Join-Path $root 'windows\CFQuotaMonitor.Windows.csproj'
$projectXml = [xml](Get-Content -LiteralPath $project -Raw)
$version = [string]($projectXml.Project.PropertyGroup.Version | Select-Object -First 1)
if ([string]::IsNullOrWhiteSpace($version)) { throw 'Windows project version was not found.' }
$release = Join-Path $root 'release'
$icon = Join-Path $root 'windows\Assets\app.ico'
$installerScript = Join-Path $root 'windows\installer.nsi'
New-Item -ItemType Directory -Force -Path $release | Out-Null

$nsis = Get-ChildItem "$env:LOCALAPPDATA\electron-builder\Cache\nsis-*\*\makensis.exe" -ErrorAction SilentlyContinue |
    Sort-Object FullName -Descending | Select-Object -First 1
if (-not $nsis) {
    $nsis = Get-Command makensis.exe -ErrorAction SilentlyContinue
}
if (-not $nsis) {
    $standardNsisPaths = @(
        if (${env:ProgramFiles(x86)}) { Join-Path ${env:ProgramFiles(x86)} 'NSIS\makensis.exe' }
        if ($env:ProgramFiles) { Join-Path $env:ProgramFiles 'NSIS\makensis.exe' }
        if ($env:ChocolateyInstall) { Join-Path $env:ChocolateyInstall 'bin\makensis.exe' }
    ) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

    if ($standardNsisPaths) {
        $nsis = Get-Item -LiteralPath ($standardNsisPaths | Select-Object -First 1)
    }
}
if (-not $nsis) {
    throw 'NSIS makensis.exe was not found. Install NSIS, then run this script again.'
}

$targets = if ($Architecture -eq 'all') { @('x64', 'arm64') } else { @($Architecture) }
foreach ($arch in $targets) {
    $rid = "win-$arch"
    $publish = Join-Path $root "windows\bin\publish\$rid"
    dotnet restore $project -r $rid --ignore-failed-sources
    dotnet publish $project -c Release -r $rid --self-contained true --no-restore `
        -p:PublishSingleFile=true -p:EnableCompressionInSingleFile=true `
        -p:DebugType=None -p:DebugSymbols=false -o $publish

    $portable = Join-Path $release "CF-Quota-Monitor-v$version-Windows-$arch-Portable.zip"
    if (Test-Path $portable) { Remove-Item -LiteralPath $portable -Force }
    Compress-Archive -Path (Join-Path $publish '*') -DestinationPath $portable -CompressionLevel Optimal

    & $nsis.FullName "/DARCH=$arch" "/DPRODUCT_VERSION=$version" "/DPUBLISH_DIR=$publish" "/DOUTPUT_DIR=$release" "/DAPP_ICON=$icon" $installerScript
    if ($LASTEXITCODE -ne 0) { throw "NSIS packaging failed for $arch" }
}

$artifacts = Get-ChildItem $release -File -Filter "CF-Quota-Monitor-v$version-Windows-*" |
    Where-Object { $_.Extension -in '.exe', '.zip' } | Sort-Object Name
$checksumLines = foreach ($artifact in $artifacts) {
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $artifact.FullName).Hash.ToLowerInvariant()
    "$hash  $($artifact.Name)"
}
Set-Content -LiteralPath (Join-Path $release 'SHA256SUMS-Windows.txt') -Value $checksumLines -Encoding utf8
$artifacts | Select-Object Name, Length
