param(
    [string]$SeedPath = (Join-Path $PSScriptRoot '..\data\fc3d-seed.json'),
    [int]$SamplesPerYear = 2,
    [int]$RandomSeed = 20260728
)

$ErrorActionPreference = 'Stop'
$seed = Get-Content -Raw -Encoding UTF8 -LiteralPath ([IO.Path]::GetFullPath($SeedPath)) |
    ConvertFrom-Json
$random = New-Object System.Random($RandomSeed)
$results = New-Object System.Collections.Generic.List[object]

foreach ($year in 2017..2026) {
    $yearDraws = @($seed.draws | Where-Object {
        $_.issue.StartsWith([string]$year)
    })
    if ($yearDraws.Count -eq 0) {
        throw "Seed has no draws for year $year"
    }

    $sampleCount = [Math]::Min($SamplesPerYear, $yearDraws.Count)
    $indices = New-Object 'System.Collections.Generic.HashSet[int]'
    while ($indices.Count -lt $sampleCount) {
        $null = $indices.Add($random.Next(0, $yearDraws.Count))
    }

    foreach ($index in @($indices | Sort-Object)) {
        $draw = $yearDraws[$index]
        $issue = [string]$draw.issue
        $auditUrl = "https://www.3d178.cn/kaijiang/$year/$issue.shtml"
        $response = Invoke-WebRequest `
            -UseBasicParsing `
            -Uri $auditUrl `
            -TimeoutSec 30

        if ($response.StatusCode -ne 200) {
            throw "Independent source returned HTTP $($response.StatusCode) for issue $issue"
        }

        $anchor = '<strong style="font-size:14px;">' + $issue + '</strong>'
        $anchorIndex = $response.Content.IndexOf($anchor)
        if ($anchorIndex -lt 0) {
            throw "Independent page does not identify issue $issue"
        }

        $chunkLength = [Math]::Min(8000, $response.Content.Length - $anchorIndex)
        $issueChunk = $response.Content.Substring($anchorIndex, $chunkLength)
        $ballMatches = @(
            [regex]::Matches(
                $issueChunk,
                'class="ball_orange">\s*(\d)\s*</li>'
            ) | Select-Object -First 3
        )
        if ($ballMatches.Count -ne 3) {
            throw "Could not parse three result digits for issue $issue"
        }

        $independentDigits = ($ballMatches | ForEach-Object {
            $_.Groups[1].Value
        }) -join ''
        $dateMatch = [regex]::Match(
            $issueChunk,
            '<span>[^<]*?(\d{4})\D+(\d{2})\D+(\d{2})\D*</span>'
        )
        if (-not $dateMatch.Success) {
            throw "Could not parse draw date for issue $issue"
        }
        $independentDate = '{0}-{1}-{2}' -f
            $dateMatch.Groups[1].Value,
            $dateMatch.Groups[2].Value,
            $dateMatch.Groups[3].Value

        $seedDigits = @($draw.digits) -join ''
        $matches = $seedDigits -eq $independentDigits -and
            [string]$draw.drawDate -eq $independentDate

        $results.Add([pscustomobject][ordered]@{
            issue            = $issue
            seedDate         = [string]$draw.drawDate
            independentDate  = $independentDate
            seedDigits       = $seedDigits
            independentDigits = $independentDigits
            matches          = $matches
            auditUrl         = $auditUrl
        })
    }
}

$failed = @($results | Where-Object { -not $_.matches })
$summary = [pscustomobject][ordered]@{
    method          = 'Deterministic stratified sample: two issues per year'
    randomSeed      = $RandomSeed
    source          = '3D178 per-issue HTML result pages'
    sourceUsesOfficialJsonEndpoint = $false
    checked         = $results.Count
    matched         = $results.Count - $failed.Count
    mismatched      = $failed.Count
    results         = $results
}

$summary | ConvertTo-Json -Depth 5

if ($failed.Count -gt 0) {
    throw "Independent sample audit found $($failed.Count) mismatches"
}
