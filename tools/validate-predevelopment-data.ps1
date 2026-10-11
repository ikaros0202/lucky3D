param(
    [string]$SeedPath = (Join-Path $PSScriptRoot '../data/fc3d-seed.json'),
    [string]$GoldenCasesPath
)
$ErrorActionPreference = 'Stop'
$sharedRoot = & (Join-Path $PSScriptRoot 'setup-shared-resources.ps1')
if (!$GoldenCasesPath) { $GoldenCasesPath = Join-Path $sharedRoot 'testdata/fc3d-golden-cases.json' }
& (Join-Path $sharedRoot 'tools/validate-predevelopment-data.ps1') -SeedPath $SeedPath -GoldenCasesPath $GoldenCasesPath
