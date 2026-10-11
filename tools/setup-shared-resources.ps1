param([string]$CacheDirectory)
$ErrorActionPreference = 'Stop'
$arguments = @((Join-Path $PSScriptRoot 'shared_resources.py'))
if ($CacheDirectory) { $arguments += @('--cache', $CacheDirectory) }
& python @arguments
if ($LASTEXITCODE -ne 0) { throw 'Resource package initialization failed.' }
