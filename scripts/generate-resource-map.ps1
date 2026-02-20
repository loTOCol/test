$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$resRoot = Join-Path $projectRoot "app\src\main\res"
$outFile = Join-Path $projectRoot "app\docs\resource-map.md"

function Get-FeatureName([string]$name) {
    $n = $name.ToLowerInvariant()

    if ($n -match "mate|mate|schedule|review") { return "mate" }
    if ($n -match "feed") { return "feed" }
    if ($n -match "walk|map|track|marker|goal_steps|path") { return "walk_map" }
    if ($n -match "chat|emoji|msg|chatroom") { return "chat" }
    if ($n -match "music|bpm|view_music_notification") { return "music" }
    if ($n -match "coinshop|coin|shop|profile1|profile2") { return "shop" }
    if ($n -match "user|profile|friend|help|app_info|manage") { return "user" }
    if ($n -match "date_selector|time_selector|search|view_common_toolbar|dialog|custom_dialog") { return "common" }
    return "misc"
}

function Group-Resources([string]$dirName, [string]$title) {
    $dir = Join-Path $resRoot $dirName
    if (!(Test-Path $dir)) { return @() }

    $groups = @{}
    Get-ChildItem -Path $dir -File | ForEach-Object {
        $feature = Get-FeatureName $_.Name
        if (-not $groups.ContainsKey($feature)) {
            $groups[$feature] = New-Object System.Collections.Generic.List[string]
        }
        $groups[$feature].Add($_.Name)
    }

    $orderedFeatures = $groups.Keys | Sort-Object
    $lines = @("## $title")
    foreach ($f in $orderedFeatures) {
        $lines += "- $f"
        $files = $groups[$f] | Sort-Object
        foreach ($name in $files) {
            $lines += "  - $name"
        }
    }
    $lines += ""
    return $lines
}

$content = @(
    "# Resource Map",
    "",
    'Android `res`는 임의 하위 폴더 분리가 불가하므로 파일명 기반으로 논리 그룹화한 목록입니다.',
    ""
)

$content += Group-Resources -dirName "layout" -title "Layouts"
$content += Group-Resources -dirName "drawable" -title "Drawables"

Set-Content -Path $outFile -Value ($content -join "`r`n") -Encoding UTF8
Write-Output "Generated: $outFile"
