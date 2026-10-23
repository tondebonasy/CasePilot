param(
    [string]$EnvironmentFile = (Join-Path $PSScriptRoot '..\.env')
)

if (-not (Test-Path -LiteralPath $EnvironmentFile -PathType Leaf)) {
    throw 'Local configuration is missing. Copy .env.example to .env and fill in the required values.'
}

$casePilotSettings = @{}
$casePilotLineNumber = 0
foreach ($casePilotLine in Get-Content -LiteralPath $EnvironmentFile) {
    $casePilotLineNumber++
    $casePilotText = $casePilotLine.Trim()
    if (-not $casePilotText -or $casePilotText.StartsWith('#')) { continue }
    if ($casePilotText -notmatch '^([A-Z][A-Z0-9_]*)=(.*)$') {
        throw "Invalid environment entry at line $casePilotLineNumber. Use NAME=value."
    }
    $casePilotName = $Matches[1]
    $casePilotValue = $Matches[2].Trim()
    if ($casePilotValue.Length -ge 2 -and
        (($casePilotValue.StartsWith('"') -and $casePilotValue.EndsWith('"')) -or
         ($casePilotValue.StartsWith("'") -and $casePilotValue.EndsWith("'")))) {
        $casePilotValue = $casePilotValue.Substring(1, $casePilotValue.Length - 2)
    }
    $casePilotSettings[$casePilotName] = $casePilotValue
}

if (-not $casePilotSettings.ContainsKey('JWT_SECRET') -or
    [Text.Encoding]::UTF8.GetByteCount($casePilotSettings['JWT_SECRET']) -lt 32 -or
    $casePilotSettings['JWT_SECRET'] -match 'REPLACE|replace-with') {
    throw 'JWT_SECRET must be a unique random value of at least 32 bytes.'
}
foreach ($casePilotRequired in 'DB_USERNAME', 'DB_PASSWORD') {
    if ([string]::IsNullOrWhiteSpace($casePilotSettings[$casePilotRequired]) -or
        $casePilotSettings[$casePilotRequired] -match '^replace-with') {
        throw "$casePilotRequired must be configured before starting the services."
    }
}
if ($casePilotSettings['SEED_ADMIN_ENABLED'] -eq 'true' -and
    ([string]::IsNullOrWhiteSpace($casePilotSettings['SEED_ADMIN_PASSWORD']) -or
     $casePilotSettings['SEED_ADMIN_PASSWORD'] -match '^replace-with')) {
    throw 'Set SEED_ADMIN_PASSWORD before enabling administrator seeding.'
}

foreach ($casePilotEntry in $casePilotSettings.GetEnumerator()) {
    [Environment]::SetEnvironmentVariable($casePilotEntry.Key, $casePilotEntry.Value, 'Process')
}
Write-Host 'CasePilot configuration loaded for this terminal. Secret values were not printed.'
