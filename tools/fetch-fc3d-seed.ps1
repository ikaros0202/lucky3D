param(
    [string]$OutputPath = (Join-Path $PSScriptRoot '..\data\fc3d-seed.json'),
    [string]$IssueStart = '2017001',
    [string]$IssueEnd = '9999999',
    [int]$PageSize = 100
)

$ErrorActionPreference = 'Stop'
$endpoint = 'https://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice'
$headers = @{
    'User-Agent' = 'Lucky3D seed generator/1.0'
    'Referer'    = 'https://www.cwl.gov.cn/'
}

function Invoke-DrawPage {
    param(
        [int]$PageNumber
    )

    $parameters = [ordered]@{
        name        = '3d'
        issueCount  = ''
        issueStart  = $IssueStart
        issueEnd    = $IssueEnd
        dayStart    = ''
        dayEnd      = ''
        pageNo      = $PageNumber
        pageSize    = $PageSize
        week        = ''
        systemType  = 'PC'
    }

    $query = ($parameters.GetEnumerator() | ForEach-Object {
        '{0}={1}' -f
            [Uri]::EscapeDataString([string]$_.Key),
            [Uri]::EscapeDataString([string]$_.Value)
    }) -join '&'

    $response = Invoke-WebRequest `
        -UseBasicParsing `
        -Uri "$endpoint`?$query" `
        -Headers $headers `
        -TimeoutSec 30

    $payload = $response.Content | ConvertFrom-Json
    if ($payload.state -ne 0) {
        throw "Official API returned state=$($payload.state): $($payload.message)"
    }

    return $payload
}

function Convert-OfficialDraw {
    param(
        [Parameter(Mandatory = $true)]
        $Draw
    )

    if ([string]$Draw.code -notmatch '^\d{7}$') {
        throw "Invalid issue code: $($Draw.code)"
    }

    $digits = @([string]$Draw.red -split ',' | ForEach-Object {
        if ($_ -notmatch '^\d$') {
            throw "Invalid digit '$_' in issue $($Draw.code)"
        }
        [int]$_
    })

    if ($digits.Count -ne 3) {
        throw "Issue $($Draw.code) does not contain exactly three digits"
    }

    $dateText = [string]$Draw.date
    if ($dateText -notmatch '^(\d{4}-\d{2}-\d{2})') {
        throw "Invalid draw date '$dateText' in issue $($Draw.code)"
    }

    [pscustomobject][ordered]@{
        issue             = [string]$Draw.code
        drawDate          = $Matches[1]
        digits            = $digits
        officialDetailUrl = if ($Draw.detailsLink) {
            "https://www.cwl.gov.cn$($Draw.detailsLink)"
        } else {
            ''
        }
    }
}

$firstPage = Invoke-DrawPage -PageNumber 1
$total = [int]$firstPage.total
if ($total -le 0) {
    throw 'Official API returned no 福彩3D draw records'
}

$pageCount = [Math]::Ceiling($total / $PageSize)
$rawDraws = New-Object System.Collections.Generic.List[object]

for ($page = 1; $page -le $pageCount; $page++) {
    $payload = if ($page -eq 1) {
        $firstPage
    } else {
        Invoke-DrawPage -PageNumber $page
    }

    foreach ($draw in @($payload.result)) {
        $rawDraws.Add((Convert-OfficialDraw -Draw $draw))
    }
}

$draws = @($rawDraws | Sort-Object issue)
$uniqueIssueCount = @($draws.issue | Sort-Object -Unique).Count

if ($draws.Count -ne $total) {
    throw "Expected $total records but downloaded $($draws.Count)"
}

if ($uniqueIssueCount -ne $draws.Count) {
    throw "Downloaded data contains duplicate issue codes"
}

$issuesByYear = $draws | Group-Object { $_.issue.Substring(0, 4) }
foreach ($yearGroup in $issuesByYear) {
    $annualNumbers = @($yearGroup.Group | ForEach-Object {
        [int]$_.issue.Substring(4)
    } | Sort-Object)

    $expectedNumbers = @($annualNumbers[0]..$annualNumbers[-1])
    $missingNumbers = @($expectedNumbers | Where-Object {
        $_ -notin $annualNumbers
    })

    if ($missingNumbers.Count -gt 0) {
        throw "Official API data for $($yearGroup.Name) is incomplete. Missing issue numbers: $($missingNumbers -join ', ')"
    }
}

$document = [ordered]@{
    schemaVersion = 1
    generatedAt   = [DateTimeOffset]::UtcNow.ToString('o')
    sourceName    = 'China Welfare Lottery official website'
    sourceUrl     = 'https://www.cwl.gov.cn/'
    sourceEndpoint = $endpoint
    range         = [ordered]@{
        firstIssue = $draws[0].issue
        lastIssue  = $draws[-1].issue
        count      = $draws.Count
    }
    draws         = $draws
}

$resolvedOutput = [IO.Path]::GetFullPath($OutputPath)
$outputDirectory = Split-Path -Parent $resolvedOutput
[IO.Directory]::CreateDirectory($outputDirectory) | Out-Null

$json = $document | ConvertTo-Json -Depth 6
$utf8NoBom = New-Object Text.UTF8Encoding($false)
[IO.File]::WriteAllText($resolvedOutput, $json, $utf8NoBom)

$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $resolvedOutput).Hash
Write-Output "Wrote $($draws.Count) records: $resolvedOutput"
Write-Output "Range: $($draws[0].issue) - $($draws[-1].issue)"
Write-Output "SHA256: $hash"
