$ErrorActionPreference = 'Stop'
$projectDir = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$libDir = Join-Path $projectDir 'libs'
$devDir = Join-Path $projectDir 'devlibs'
New-Item -ItemType Directory -Force $libDir,$devDir | Out-Null
$deps = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'dependencies.json') -Raw -Encoding UTF8 | ConvertFrom-Json
foreach ($dep in $deps) {
    $dest = Join-Path $libDir $dep.file
    if (!(Test-Path -LiteralPath $dest) -or (Get-FileHash -LiteralPath $dest -Algorithm SHA512).Hash.ToLowerInvariant() -ne $dep.sha512) {
        Write-Host "Downloading $($dep.file)"
        Invoke-WebRequest -Uri $dep.url -OutFile $dest
    }
    if ((Get-FileHash -LiteralPath $dest -Algorithm SHA512).Hash.ToLowerInvariant() -ne $dep.sha512) { throw "SHA512 mismatch: $dest" }
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Expand-NestedDependency([string]$path) {
    $zip = [IO.Compression.ZipFile]::OpenRead($path)
    try {
        foreach ($entry in $zip.Entries) {
            if (!$entry.FullName.EndsWith('.jar') -or $entry.Name.Contains('mixinextras')) { continue }
            $target = Join-Path $devDir $entry.Name
            if (!(Test-Path -LiteralPath $target)) {
                [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$target,$false)
                Expand-NestedDependency $target
            }
        }
    } finally { $zip.Dispose() }
}
Get-ChildItem -LiteralPath $libDir -Filter '*.jar' | ForEach-Object { Expand-NestedDependency $_.FullName }
Write-Host 'Dependencies verified. Run .\gradlew.bat build with Java 21.'
