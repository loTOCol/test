param(
    [int]$Top = 30
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$resRoot = Join-Path $projectRoot "app\src\main\res"

if (-not (Test-Path $resRoot)) {
    throw "Resource path not found: $resRoot"
}

Write-Host "== Resource Size Audit ==" -ForegroundColor Cyan
Write-Host "Root: $resRoot"
Write-Host ""

$files = Get-ChildItem -Path $resRoot -Recurse -File
$totalMb = [math]::Round((($files | Measure-Object Length -Sum).Sum / 1MB), 2)
$rawDir = Join-Path $resRoot "raw"
$rawCount = if (Test-Path $rawDir) { (Get-ChildItem $rawDir -File | Measure-Object).Count } else { 0 }

Write-Host ("Total files: {0}" -f $files.Count)
Write-Host ("Total size: {0} MB" -f $totalMb)
Write-Host ("raw/ track count: {0}" -f $rawCount)
Write-Host ""

Write-Host ("Top {0} largest resource files" -f $Top) -ForegroundColor Yellow
$files |
    Sort-Object Length -Descending |
    Select-Object -First $Top @{Name="SizeMB";Expression={[math]::Round($_.Length / 1MB, 3)}}, FullName |
    Format-Table -AutoSize
