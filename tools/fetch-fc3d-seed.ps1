param(
    [string]$OutputPath = (Join-Path $PSScriptRoot '..\data\fc3d-seed.json'),
    [string]$IssueStart = '2017001',
    [string]$IssueEnd = '9999999',
    [int]$PageSize = 100
)

$ErrorActionPreference = 'Stop'
$sharedScript = Join-Path $PSScriptRoot '../../fc3d-resources/tools/fetch-fc3d-seed.ps1'
if (!(Test-Path -LiteralPath $sharedScript)) { throw 'Run tools/setup-shared-resources.ps1 first.' }
& $sharedScript  -OutputPath $OutputPath -IssueStart $IssueStart -IssueEnd $IssueEnd -PageSize $PageSize
