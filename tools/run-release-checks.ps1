param([Parameter(Mandatory=$true)][string]$Workspace)
$ErrorActionPreference='Stop'
$projectPath=Split-Path $PSScriptRoot -Parent
$gradle=Join-Path $Workspace 'build-tools/gradle/gradle-9.2.1/bin/gradle.bat'
$ae=Join-Path $Workspace 'build-tools/test-mods/appliedenergistics2-19.2.18.jar'
$create=Join-Path $Workspace 'build-tools/create-test-mods/create-1.21.1-6.0.10.jar'
$old=Join-Path $Workspace 'build-tools/compat-old-test-mods'
$new=Join-Path $Workspace 'build-tools/all-test-mods'
$nested=Join-Path $Workspace 'build-tools/create-reference'
$common=@('-p',$projectPath,"-Pae2DevJar=$ae","-PcreateDevJar=$create","-PcompatDevMods=$old",'--no-daemon','--console=plain','--no-configuration-cache','--offline')
function Run-Check([string]$name,[string]$init,[string[]]$extra,[string]$report,[string]$marker) {
    $props=Join-Path $projectPath 'run/server.properties'
    [IO.File]::WriteAllText($props,([IO.File]::ReadAllText($props) -replace '(?m)^level-name=.*',('level-name=release-' + $name)),[Text.UTF8Encoding]::new($false))
    $path=Join-Path $projectPath ('build/'+$report+'.txt')
    [IO.File]::WriteAllText($path,'PENDING')
    $log=Join-Path $Workspace ('build-tools/release-'+$name+'.log')
    & $gradle @common runServer -I $init @extra *> $log
    if ($LASTEXITCODE -ne 0 -or !([IO.File]::ReadAllText($path).Contains($marker))) {
        Get-Content $log -Tail 35; Get-Content $path; throw "Failed $name"
    }
    $saved=Join-Path $projectPath ('build/'+$name+'-checks.txt')
    if ($saved -ne $path) { Copy-Item -LiteralPath $path -Destination $saved -Force }
    Write-Output "RELEASE CHECK PASSED: $name"
}
Run-Check 'network-crafter' 'tools/network-checks.init.gradle' @('-PnetworkCrafter',"-Pae2TestMods=$old") 'network-checks' 'ALL NETWORK CHECKS PASSED'
Run-Check 'network-table' 'tools/network-checks.init.gradle' @("-Pae2TestMods=$old") 'network-checks' 'ALL NETWORK CHECKS PASSED'
Run-Check 'compat-crafter' 'tools/compat-checks.init.gradle' @('-PnetworkCrafter',"-PcompatTestMods=$old") 'compat-checks' 'ALL COMPAT CHECKS PASSED'
Run-Check 'compat-table' 'tools/compat-checks.init.gradle' @("-PcompatTestMods=$new") 'compat-checks' 'ALL COMPAT CHECKS PASSED'
Run-Check 'gameplay' 'tools/gameplay-checks.init.gradle' @() 'gameplay-checks' 'ALL GAMEPLAY CHECKS PASSED'
Run-Check 'create' 'tools/create-checks.init.gradle' @("-PcreateTestMods=$old","-PcreateNestedJars=$nested") 'create-checks' 'ALL CREATE FLUID CHECKS PASSED'
Run-Check 'redstone' 'tools/redstone-checks.init.gradle' @("-PredstoneMods=$old") 'redstone-checks' 'ALL REDSTONE CHECKS PASSED'
& (Join-Path $PSScriptRoot 'run-isolation-matrix.ps1') -Workspace $Workspace
if ($LASTEXITCODE -ne 0) { throw 'Isolation matrix failed' }
$log=Join-Path $Workspace 'build-tools/release-final-build.log'
& $gradle @common build *> $log
if ($LASTEXITCODE -ne 0) { Get-Content $log -Tail 50; throw 'Build failed' }
Write-Output 'RELEASE FINAL BUILD PASSED'
