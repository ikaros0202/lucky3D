$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$config = Get-Content -LiteralPath (Join-Path $projectRoot 'shared-resources.json') -Raw | ConvertFrom-Json
$sharedRoot = Join-Path (Split-Path $projectRoot -Parent) 'fc3d-resources'
if (!(Test-Path -LiteralPath $sharedRoot)) {
    git clone $config.repository $sharedRoot
    if ($LASTEXITCODE -ne 0) { throw 'Shared resource clone failed.' }
    git -C $sharedRoot checkout --detach $config.revision
    if ($LASTEXITCODE -ne 0) { throw 'Shared resource revision unavailable.' }
}
$top = git -C $sharedRoot rev-parse --show-toplevel
if ($LASTEXITCODE -ne 0 -or [IO.Path]::GetFullPath($top) -ne [IO.Path]::GetFullPath($sharedRoot)) { throw 'Shared resource path is not its own Git repository.' }
$actual = git -C $sharedRoot rev-parse HEAD
if ($LASTEXITCODE -ne 0 -or $actual -ne $config.revision) { throw 'Shared resource version differs. Update both project locks together; no files were overwritten.' }
$dirty = git -C $sharedRoot status --porcelain
if ($LASTEXITCODE -ne 0 -or $dirty) { throw 'Shared resources contain uncommitted changes; review them before proceeding.' }
Write-Output "Shared resources ready: $actual"
