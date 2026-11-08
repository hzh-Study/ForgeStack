#Requires -Version 7.0
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot

function Resolve-Jdk {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    if ($env:USERPROFILE) { $candidates += (Join-Path $env:USERPROFILE '.jdks\corretto-17.0.21') }

    foreach ($candidate in $candidates) {
        if (-not $candidate -or -not (Test-Path $candidate)) { continue }
        $javaExe = Join-Path $candidate 'bin\java.exe'
        if (Test-Path $javaExe) {
            return (Resolve-Path $candidate).Path
        }
    }

    throw "未找到 JDK 17：请设置 JAVA_HOME，或将 JDK 放在 $env:USERPROFILE\.jdks\corretto-17.0.21"
}

$jdk = Resolve-Jdk
$env:JAVA_HOME = $jdk
$env:Path = "$(Join-Path $jdk 'bin');$env:Path"
Write-Host "JDK: $jdk"

$results = [System.Collections.Generic.List[object]]::new()

function Invoke-Step {
    param(
        [string]$Name,
        [string]$WorkDir,
        [string]$Command,
        [string[]]$Arguments
    )

    Write-Host ''
    Write-Host "==> $Name" -ForegroundColor Cyan
    Push-Location $WorkDir
    try {
        & $Command @Arguments 2>&1 | Out-Host
        $exitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }

    if ($exitCode -ne 0) {
        Write-Host "[FAIL] $Name (exit $exitCode)" -ForegroundColor Red
        [void]$results.Add([pscustomobject]@{ Name = $Name; Ok = $false })
        return $false
    }

    Write-Host "[PASS] $Name" -ForegroundColor Green
    [void]$results.Add([pscustomobject]@{ Name = $Name; Ok = $true })
    return $true
}

$steps = @(
    @{ Name = 'server-api test';          Dir = (Join-Path $repoRoot 'server-api');  Command = '.\mvnw.cmd'; Arguments = @('-o', 'test') },
    @{ Name = 'conventions';              Dir = $repoRoot;                            Command = 'node';       Arguments = @('scripts/check-conventions.mjs') },
    @{ Name = 'backend-ui build';         Dir = (Join-Path $repoRoot 'backend-ui');  Command = 'npm';        Arguments = @('run', 'build') },
    @{ Name = 'frontend-ui build';        Dir = (Join-Path $repoRoot 'frontend-ui'); Command = 'npm';        Arguments = @('run', 'build') },
    @{ Name = 'uniapp-ui build:h5';       Dir = (Join-Path $repoRoot 'uniapp-ui');   Command = 'npm';        Arguments = @('run', 'build:h5') },
    @{ Name = 'uniapp-ui build:mp-weixin'; Dir = (Join-Path $repoRoot 'uniapp-ui');  Command = 'npm';        Arguments = @('run', 'build:mp-weixin') }
)

$allOk = $true
foreach ($step in $steps) {
    if (-not (Invoke-Step -Name $step.Name -WorkDir $step.Dir -Command $step.Command -Arguments $step.Arguments)) {
        $allOk = $false
        break
    }
}

Write-Host ''
Write-Host '===== SUMMARY =====' -ForegroundColor Cyan
foreach ($result in $results) {
    $mark = if ($result.Ok) { '[PASS]' } else { '[FAIL]' }
    Write-Host "$mark $($result.Name)"
}

if ($allOk) {
    Write-Host ''
    Write-Host 'ALL CHECKS PASSED' -ForegroundColor Green
    exit 0
}

Write-Host ''
Write-Host 'CHECKS FAILED' -ForegroundColor Red
exit 1
