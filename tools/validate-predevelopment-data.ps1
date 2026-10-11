param(
    [string]$SeedPath = (Join-Path $PSScriptRoot '..\data\fc3d-seed.json'),
    [string]$GoldenCasesPath = (Join-Path $PSScriptRoot '..\..\fc3d-resources\testdata\fc3d-golden-cases.json')
)

$ErrorActionPreference = 'Stop'
$sharedScript = Join-Path $PSScriptRoot '../../fc3d-resources/tools/validate-predevelopment-data.ps1'
if (!(Test-Path -LiteralPath $sharedScript)) { throw 'Run tools/setup-shared-resources.ps1 first.' }
& $sharedScript  -SeedPath $SeedPath -GoldenCasesPath $GoldenCasesPath
