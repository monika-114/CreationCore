param([Parameter(Mandatory=$true)][string]$Workspace)
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path $PSScriptRoot -Parent
$gradle = Join-Path $Workspace 'build-tools/gradle/gradle-9.2.1/bin/gradle.bat'
$ae = Join-Path $Workspace 'build-tools/test-mods/appliedenergistics2-19.2.18.jar'
$create = Join-Path $Workspace 'build-tools/create-test-mods/create-1.21.1-6.0.10.jar'
$mods = Join-Path $Workspace 'build-tools/compat-test-mods'
$matrixRoot = Join-Path $Workspace 'build-tools/isolation-mods'
$reportRoot = Join-Path $projectPath 'build/isolation'
New-Item -ItemType Directory -Path $matrixRoot,$reportRoot -Force | Out-Null
$core = (Get-ChildItem $mods -Filter '*core*.jar' | Select-Object -First 1).FullName
$backpack = (Get-ChildItem $mods -Filter '*backpacks*.jar' | Select-Object -First 1).FullName
$storage = (Get-ChildItem $mods -Filter '*storage*.jar' | Where-Object Name -notlike '*toms*' | Select-Object -First 1).FullName
$toms = (Get-ChildItem $mods -Filter '*toms*.jar' | Select-Object -First 1).FullName
$beyond = (Get-ChildItem $mods -Filter '*beyond*.jar' | Select-Object -First 1).FullName
$guide = Join-Path $Workspace 'build-tools/test-mods/guideme-21.1.19.jar'
$variants = [ordered]@{ none=@(); core=@($core); backpack=@($core,$backpack); storage=@($core,$storage);
    toms=@($toms); beyond=@($beyond); ae2=@($ae,$guide); create=@($create) }
foreach ($name in $variants.Keys) {
    $dir = Join-Path $matrixRoot $name
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
    foreach ($file in $variants[$name]) { Copy-Item -LiteralPath $file -Destination $dir -Force }
    $props = Join-Path $projectPath 'run/server.properties'
    [IO.File]::WriteAllText($props,([IO.File]::ReadAllText($props) -replace '(?m)^level-name=.*',('level-name=isolation-' + $name)),[Text.UTF8Encoding]::new($false))
    $log = Join-Path $Workspace ('build-tools/isolation-' + $name + '.log')
    [IO.File]::WriteAllText((Join-Path $projectPath 'build/isolation-checks.txt'), 'PENDING')
    & $gradle -p $projectPath runServer -I tools/isolation-checks.init.gradle "-Pae2DevJar=$ae" "-PcreateDevJar=$create" "-PcompatDevMods=$mods" "-PisolationMods=$dir" --no-daemon --console=plain --no-configuration-cache --offline *> $log
    $report = Join-Path $projectPath 'build/isolation-checks.txt'
    if ($LASTEXITCODE -ne 0 -or !(Test-Path $report) -or !([IO.File]::ReadAllText($report).Contains('ALL ISOLATION CHECKS PASSED'))) {
        Get-Content $log -Tail 45
        if (Test-Path $report) { Get-Content $report }
        throw "Isolation variant failed: $name"
    }
    Copy-Item -LiteralPath $report -Destination (Join-Path $reportRoot ($name + '.txt')) -Force
    Write-Output "ISOLATION PASSED: $name"
}
