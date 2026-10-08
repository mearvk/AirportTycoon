# Airport Tycoon — Windows 10+ build
# Requires Java 21 and Maven 3.9+.

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
Set-Location $root

foreach ($edition in 1..8) {
    $build = Join-Path $root "build/$edition"
    New-Item -ItemType Directory -Force -Path $build | Out-Null
    Write-Host "== Airport Tycoon Edition $edition / Windows =="
    $ui = Join-Path $root "$edition/ui"
    Push-Location $ui
    try { mvn -q -DskipTests=false test } finally { Pop-Location }
}
New-Item -ItemType Directory -Force -Path (Join-Path $root "build/dungeons-of-moria") | Out-Null
Write-Host "Windows build/test complete."
