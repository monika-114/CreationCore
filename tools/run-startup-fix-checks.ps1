param([Parameter(Mandatory=$true)][string]$Workspace)
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path $PSScriptRoot -Parent
$gradle = Join-Path $Workspace 'build-tools/gradle/gradle-9.2.1/bin/gradle.bat'
$ae = Join-Path $Workspace 'build-tools/test-mods/appliedenergistics2-19.2.18.jar'
$create = Join-Path $Workspace 'build-tools/create-test-mods/create-1.21.1-6.0.10.jar'
$old = Join-Path $Workspace 'build-tools/compat-old-test-mods'
$new = Join-Path $Workspace 'build-tools/all-test-mods'
$common = @('-p',$projectPath,"-Pae2DevJar=$ae","-PcreateDevJar=$create","-PcompatDevMods=$old",'--no-daemon','--console=plain','--no-configuration-cache','--offline')
function Set-CheckWorld([string]$name) {
    $props = Join-Path $projectPath 'run/server.properties'
    [IO.File]::WriteAllText($props,([IO.File]::ReadAllText($props) -replace '(?m)^level-name=.*',('level-name=compat3-' + $name)),[Text.UTF8Encoding]::new($false))
}
function Require-Report([string]$name, [string]$marker, [string]$log) {
    $report = Join-Path $projectPath ('build/' + $name + '.txt')
    if (!(Test-Path $report) -or !([IO.File]::ReadAllText($report).Contains($marker))) {
        Get-Content $log -Tail 50
        if (Test-Path $report) { Get-Content $report }
        throw "Test failed: $name"
    }
}
Set-CheckWorld 'new-checks'
$log = Join-Path $Workspace 'build-tools/compat3-new-run.log'
[IO.File]::WriteAllText((Join-Path $projectPath 'build/compat-checks.txt'), 'PENDING')
& $gradle @common runServer -I tools/compat-checks.init.gradle "-PcompatTestMods=$new" *> $log
Require-Report 'compat-checks' 'ALL COMPAT CHECKS PASSED' $log
Copy-Item -LiteralPath (Join-Path $projectPath 'build/compat-checks.txt') -Destination (Join-Path $projectPath 'build/compat-new-checks.txt') -Force
Write-Output 'NEW SOPHISTICATED VERSIONS PASSED'
Set-CheckWorld 'standalone'
$log = Join-Path $Workspace 'build-tools/compat3-standalone.log'
& $gradle @common runServer -I tools/gameplay-checks.init.gradle *> $log
Require-Report 'gameplay-checks' 'ALL GAMEPLAY CHECKS PASSED' $log
Require-Report 'fluid-checks' 'ALL CORE FLUID CHECKS PASSED' $log
Write-Output 'STANDALONE GAMEPLAY AND FLUID PASSED'
$core = (Get-ChildItem $old -Filter '*core*.jar' | Select-Object -First 1).FullName
$backpack = (Get-ChildItem $old -Filter '*backpacks*.jar' | Select-Object -First 1).FullName
$storage = (Get-ChildItem $old -Filter 'sophisticatedstorage*.jar' | Select-Object -First 1).FullName
$variants = [ordered]@{core=@($core);backpack=@($core,$backpack);storage=@($core,$storage)}
$reports = Join-Path $projectPath 'build/old-isolation'
New-Item -ItemType Directory -Path $reports -Force | Out-Null
foreach ($name in $variants.Keys) {
    $dir = Join-Path $Workspace ('build-tools/compat3-isolation/' + $name)
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
    foreach ($file in $variants[$name]) { Copy-Item -LiteralPath $file -Destination $dir -Force }
    Set-CheckWorld ('isolation-' + $name)
    $log = Join-Path $Workspace ('build-tools/compat3-isolation-' + $name + '.log')
    [IO.File]::WriteAllText((Join-Path $projectPath 'build/isolation-checks.txt'), 'PENDING')
    & $gradle @common runServer -I tools/isolation-checks.init.gradle "-PisolationMods=$dir" *> $log
    Require-Report 'isolation-checks' 'ALL ISOLATION CHECKS PASSED' $log
    Copy-Item -LiteralPath (Join-Path $projectPath 'build/isolation-checks.txt') -Destination (Join-Path $reports ($name + '.txt')) -Force
    Write-Output "OLD ISOLATION PASSED: $name"
}
$log = Join-Path $Workspace 'build-tools/compat3-final-build.log'
& $gradle @common build *> $log
if ($LASTEXITCODE -ne 0) { Get-Content $log -Tail 45; throw 'Final build failed' }
Write-Output 'FINAL BUILD PASSED'
