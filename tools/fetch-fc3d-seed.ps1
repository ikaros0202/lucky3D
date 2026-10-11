param(
    [string]$OutputPath = (Join-Path $PSScriptRoot '../data/fc3d-seed.json'),
    [string]$IssueStart = '2017001',
    [string]$IssueEnd = '9999999',
    [int]$PageSize = 100
)
$ErrorActionPreference = 'Stop'
$sharedRoot = & (Join-Path $PSScriptRoot 'setup-shared-resources.ps1')
& (Join-Path $sharedRoot 'tools/fetch-fc3d-seed.ps1') -OutputPath $OutputPath -IssueStart $IssueStart -IssueEnd $IssueEnd -PageSize $PageSize
