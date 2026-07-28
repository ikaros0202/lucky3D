param(
    [string]$SeedPath = (Join-Path $PSScriptRoot '..\data\fc3d-seed.json'),
    [string]$GoldenCasesPath = (Join-Path $PSScriptRoot '..\testdata\fc3d-golden-cases.json')
)

$ErrorActionPreference = 'Stop'

function Assert-Equal {
    param(
        $Actual,
        $Expected,
        [string]$Message
    )

    $actualNumber = 0.0
    $expectedNumber = 0.0
    $bothNumeric =
        [double]::TryParse(
            [string]$Actual,
            [Globalization.NumberStyles]::Any,
            [Globalization.CultureInfo]::InvariantCulture,
            [ref]$actualNumber
        ) -and
        [double]::TryParse(
            [string]$Expected,
            [Globalization.NumberStyles]::Any,
            [Globalization.CultureInfo]::InvariantCulture,
            [ref]$expectedNumber
        )

    $equal = if ($bothNumeric) {
        $actualNumber -eq $expectedNumber
    } else {
        [string]$Actual -eq [string]$Expected
    }

    if (-not $equal) {
        throw "$Message. Expected '$Expected', got '$Actual'"
    }
}

function Assert-SequenceEqual {
    param(
        $Actual,
        $Expected,
        [string]$Message
    )

    Assert-Equal -Actual (@($Actual) -join ',') -Expected (@($Expected) -join ',') -Message $Message
}

function Get-Digits {
    param([string]$Number)

    if ($Number -notmatch '^\d{3}$') {
        throw "Invalid three-digit number: $Number"
    }

    return @($Number.ToCharArray() | ForEach-Object { [int][string]$_ })
}

function Get-GroupShape {
    param($Digits)

    $uniqueCount = @($Digits | Sort-Object -Unique).Count
    if ($uniqueCount -eq 1) {
        return 'LEOPARD'
    }
    if ($uniqueCount -eq 2) {
        return 'GROUP3'
    }
    return 'GROUP6'
}

function Get-UniquePermutations {
    param($Digits)

    $values = New-Object 'System.Collections.Generic.HashSet[string]'
    for ($a = 0; $a -lt 3; $a++) {
        for ($b = 0; $b -lt 3; $b++) {
            if ($b -eq $a) {
                continue
            }
            for ($c = 0; $c -lt 3; $c++) {
                if ($c -eq $a -or $c -eq $b) {
                    continue
                }
                $null = $values.Add(('{0}{1}{2}' -f $Digits[$a], $Digits[$b], $Digits[$c]))
            }
        }
    }
    return @($values | Sort-Object)
}

function Test-UniverseCondition {
    param(
        $Digits,
        $Condition
    )

    switch ([string]$Condition.type) {
        'GLOBAL_EXCLUDED_DIGITS' {
            foreach ($digit in @($Condition.digits)) {
                if ($digit -in $Digits) {
                    return $false
                }
            }
            return $true
        }
        'GLOBAL_REQUIRED_DIGITS' {
            foreach ($digit in @($Condition.digits)) {
                if ($digit -notin $Digits) {
                    return $false
                }
            }
            return $true
        }
        'SUM_RANGE' {
            $sum = ($Digits | Measure-Object -Sum).Sum
            return $sum -ge $Condition.minimum -and $sum -le $Condition.maximum
        }
        'GROUP_SHAPE' {
            return (Get-GroupShape -Digits $Digits) -in @($Condition.values)
        }
        default {
            throw "Unsupported golden universe condition: $($Condition.type)"
        }
    }
}

$resolvedSeedPath = [IO.Path]::GetFullPath($SeedPath)
$resolvedGoldenPath = [IO.Path]::GetFullPath($GoldenCasesPath)
$seed = Get-Content -Raw -Encoding UTF8 -LiteralPath $resolvedSeedPath | ConvertFrom-Json
$golden = Get-Content -Raw -Encoding UTF8 -LiteralPath $resolvedGoldenPath | ConvertFrom-Json

Assert-Equal -Actual $seed.schemaVersion -Expected 1 -Message 'Seed schema version'
Assert-Equal -Actual $seed.range.count -Expected $seed.draws.Count -Message 'Seed declared record count'
Assert-Equal -Actual $seed.draws[0].issue -Expected '2017001' -Message 'Seed first issue'
Assert-Equal -Actual (@($seed.draws.issue | Sort-Object -Unique).Count) -Expected $seed.draws.Count -Message 'Seed unique issue count'
Assert-SequenceEqual -Actual $seed.draws.issue -Expected @($seed.draws.issue | Sort-Object) -Message 'Seed issue ordering'

foreach ($draw in @($seed.draws)) {
    if ([string]$draw.issue -notmatch '^\d{7}$') {
        throw "Invalid seed issue: $($draw.issue)"
    }
    if ([string]$draw.drawDate -notmatch '^\d{4}-\d{2}-\d{2}$') {
        throw "Invalid seed date in issue $($draw.issue): $($draw.drawDate)"
    }
    Assert-Equal -Actual @($draw.digits).Count -Expected 3 -Message "Digit count for issue $($draw.issue)"
    foreach ($digit in @($draw.digits)) {
        if ($digit -lt 0 -or $digit -gt 9) {
            throw "Invalid digit in issue $($draw.issue): $digit"
        }
    }
}

foreach ($yearGroup in @($seed.draws | Group-Object { $_.issue.Substring(0, 4) })) {
    $numbers = @($yearGroup.Group | ForEach-Object { [int]$_.issue.Substring(4) } | Sort-Object)
    $expected = @($numbers[0]..$numbers[-1])
    Assert-SequenceEqual -Actual $numbers -Expected $expected -Message "Issue continuity for $($yearGroup.Name)"
}

$primeLike = @($golden.qualityConvention.primeLike)
foreach ($case in @($golden.attributeCases)) {
    $digits = Get-Digits -Number $case.number
    $sum = ($digits | Measure-Object -Sum).Sum
    $sortedUnique = @($digits | Sort-Object -Unique)
    $differences = @()
    for ($index = 1; $index -lt $sortedUnique.Count; $index++) {
        $differences += $sortedUnique[$index] - $sortedUnique[$index - 1]
    }

    Assert-Equal -Actual $sum -Expected $case.sum -Message "$($case.number) sum"
    Assert-Equal -Actual ($sum % 10) -Expected $case.sumTail -Message "$($case.number) sum tail"
    Assert-Equal -Actual (($digits | Measure-Object -Maximum).Maximum - ($digits | Measure-Object -Minimum).Minimum) -Expected $case.span -Message "$($case.number) span"
    Assert-Equal -Actual @($digits | Where-Object { $_ % 2 -ne 0 }).Count -Expected $case.oddCount -Message "$($case.number) odd count"
    Assert-Equal -Actual @($digits | Where-Object { $_ -ge 5 }).Count -Expected $case.bigCount -Message "$($case.number) big count"
    Assert-Equal -Actual @($digits | Where-Object { $_ -in $primeLike }).Count -Expected $case.primeLikeCount -Message "$($case.number) prime-like count"

    $routes = @($digits | ForEach-Object { $_ % 3 })
    $routeCounts = @(0, 1, 2 | ForEach-Object {
        $route = $_
        @($routes | Where-Object { $_ -eq $route }).Count
    })
    Assert-SequenceEqual -Actual $routes -Expected $case.routesByPosition -Message "$($case.number) routes by position"
    Assert-SequenceEqual -Actual $routeCounts -Expected $case.routeCounts -Message "$($case.number) route counts"
    Assert-Equal -Actual (Get-GroupShape -Digits $digits) -Expected $case.groupShape -Message "$($case.number) group shape"
    Assert-Equal -Actual ($differences -contains 1) -Expected $case.hasPairConsecutive -Message "$($case.number) pair consecutive"
    Assert-Equal -Actual ($sortedUnique.Count -eq 3 -and @($differences | Where-Object { $_ -eq 1 }).Count -eq 2) -Expected $case.hasTripleConsecutive -Message "$($case.number) triple consecutive"

    Assert-Equal -Actual ($digits[0] + $digits[1]) -Expected $case.pairSums.hundredsTens -Message "$($case.number) hundreds-tens sum"
    Assert-Equal -Actual ($digits[1] + $digits[2]) -Expected $case.pairSums.tensOnes -Message "$($case.number) tens-ones sum"
    Assert-Equal -Actual ($digits[0] + $digits[2]) -Expected $case.pairSums.hundredsOnes -Message "$($case.number) hundreds-ones sum"
    Assert-Equal -Actual ([Math]::Abs($digits[0] - $digits[1])) -Expected $case.pairDifferences.hundredsTens -Message "$($case.number) hundreds-tens difference"
    Assert-Equal -Actual ([Math]::Abs($digits[1] - $digits[2])) -Expected $case.pairDifferences.tensOnes -Message "$($case.number) tens-ones difference"
    Assert-Equal -Actual ([Math]::Abs($digits[0] - $digits[2])) -Expected $case.pairDifferences.hundredsOnes -Message "$($case.number) hundreds-ones difference"
}

foreach ($case in @($golden.crossDrawCases)) {
    $previous = @(Get-Digits -Number $case.previous | Sort-Object -Unique)
    $current = @(Get-Digits -Number $case.current | Sort-Object -Unique)
    $repeat = @($current | Where-Object { $_ -in $previous } | Sort-Object -Unique)
    $neighborPool = @($previous | ForEach-Object {
        if ($_ -gt 0) { $_ - 1 }
        if ($_ -lt 9) { $_ + 1 }
    } | Sort-Object -Unique)
    $neighbors = @($current | Where-Object { $_ -in $neighborPool } | Sort-Object -Unique)

    Assert-SequenceEqual -Actual $repeat -Expected $case.repeatDigits -Message "$($case.previous) to $($case.current) repeat digits"
    Assert-SequenceEqual -Actual $neighbors -Expected $case.neighborDigits -Message "$($case.previous) to $($case.current) neighbor digits"
}

foreach ($case in @($golden.permutationCases)) {
    $actual = Get-UniquePermutations -Digits (Get-Digits -Number $case.canonical)
    Assert-SequenceEqual -Actual $actual -Expected @($case.straightPermutations | Sort-Object) -Message "$($case.canonical) permutations"
}

foreach ($case in @($golden.universeCases)) {
    $count = 0
    for ($value = 0; $value -le 999; $value++) {
        $digits = Get-Digits -Number $value.ToString('D3')
        $matches = $true
        foreach ($condition in @($case.conditions)) {
            if (-not (Test-UniverseCondition -Digits $digits -Condition $condition)) {
                $matches = $false
                break
            }
        }
        if ($matches) {
            $count++
        }
    }
    Assert-Equal -Actual $count -Expected $case.expectedCount -Message "Universe case '$($case.description)'"
    if ($null -ne $case.expectedSingleMultiplierAmount) {
        Assert-Equal -Actual ($count * 2) -Expected $case.expectedSingleMultiplierAmount -Message "Amount for '$($case.description)'"
    }
}

foreach ($case in @($golden.playUniverseCases)) {
    Assert-Equal -Actual ($case.expectedBetCount * 2) -Expected $case.expectedSingleMultiplierAmount -Message "$($case.playType) single multiplier amount"
}

foreach ($case in @($golden.omissionCases)) {
    $omission = -1
    $byDraw = @()
    $completed = @()
    $missesSinceHit = $null
    foreach ($digit in @($case.hundredsDigits)) {
        if ($digit -eq $case.targetDigit) {
            if ($null -ne $missesSinceHit) {
                $completed += $missesSinceHit
            }
            $omission = 0
            $missesSinceHit = 0
        } else {
            if ($omission -lt 0) {
                $omission = 1
            } else {
                $omission++
            }
            if ($null -ne $missesSinceHit) {
                $missesSinceHit++
            }
        }
        $byDraw += $omission
    }

    Assert-SequenceEqual -Actual $byDraw -Expected $case.omissionByDraw -Message "Omission sequence '$($case.description)'"
    Assert-SequenceEqual -Actual $completed -Expected $case.completedOmissions -Message "Completed omissions '$($case.description)'"
    Assert-Equal -Actual $byDraw[-1] -Expected $case.currentOmission -Message "Current omission '$($case.description)'"
    Assert-Equal -Actual (($completed | Measure-Object -Average).Average) -Expected $case.averageOmission -Message "Average omission '$($case.description)'"
    Assert-Equal -Actual (($completed | Measure-Object -Maximum).Maximum) -Expected $case.maxOmission -Message "Maximum omission '$($case.description)'"
}

$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $resolvedSeedPath).Hash
Write-Output 'Predevelopment data validation passed.'
Write-Output "Seed records: $($seed.draws.Count)"
Write-Output "Seed range: $($seed.draws[0].issue) - $($seed.draws[-1].issue)"
Write-Output "Seed SHA256: $hash"
Write-Output "Golden attribute cases: $(@($golden.attributeCases).Count)"
