param([Parameter(Mandatory=$true)][string]$Workspace)
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path $PSScriptRoot -Parent
$gradle = Join-Path $Workspace 'build-tools/gradle/gradle-9.2.1/bin/gradle.bat'
$ae = Join-Path $Workspace 'build-tools/test-mods/appliedenergistics2-19.2.18.jar'
$create = Join-Path $Workspace 'build-tools/create-test-mods/create-1.21.1-6.0.10.jar'
$mods = Join-Path $Workspace 'build-tools/compat-test-mods'
$all = Join-Path $Workspace 'build-tools/all-test-mods'
$nested = Join-Path $Workspace 'build-tools/create-reference'
$common = @('-p',$projectPath,"-Pae2DevJar=$ae","-PcreateDevJar=$create","-PcompatDevMods=$mods",'--no-daemon','--console=plain','--no-configuration-cache','--offline')
foreach ($target in @('network','create')) {
    $props = Join-Path $projectPath 'run/server.properties'
    [IO.File]::WriteAllText($props,([IO.File]::ReadAllText($props) -replace '(?m)^level-name=.*',('level-name=compat-regression-' + $target)),[Text.UTF8Encoding]::new($false))
    $log = Join-Path $Workspace ('build-tools/compat-regression-' + $target + '.log')
    $report = Join-Path $projectPath ('build/' + $target + '-checks.txt')
    [IO.File]::WriteAllText($report, 'PENDING')
    if ($target -eq 'network') {
        & $gradle @common runServer -I tools/network-checks.init.gradle "-Pae2TestMods=$all" *> $log
        $marker = 'ALL NETWORK CHECKS PASSED'
    } else {
        [IO.File]::WriteAllText((Join-Path $projectPath 'build/fluid-checks.txt'), 'PENDING')
        & $gradle @common runServer -I tools/create-checks.init.gradle "-PcreateTestMods=$all" "-PcreateNestedJars=$nested" *> $log
        $marker = 'ALL CREATE FLUID CHECKS PASSED'
    }
    if ($LASTEXITCODE -ne 0 -or !([IO.File]::ReadAllText($report).Contains($marker))) {
        Get-Content $log -Tail 45
        Get-Content $report
        throw "Regression failed: $target"
    }
    Write-Output "REGRESSION PASSED: $target"
}
$buildLog = Join-Path $Workspace 'build-tools/compat-final-build.log'
& $gradle @common build *> $buildLog
if ($LASTEXITCODE -ne 0) { Get-Content $buildLog -Tail 60; throw 'Final build failed' }
Write-Output 'FINAL BUILD PASSED'
