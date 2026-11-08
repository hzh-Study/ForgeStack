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

$tempDir = Join-Path $repoRoot 'temp'
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
$logPath = Join-Path $tempDir 'backend.log'

if (-not $env:DB_NAME) { $env:DB_NAME = 'forgestack' }

Write-Host "JDK: $jdk"
Write-Host "DB_NAME: $($env:DB_NAME)"
Write-Host "后端日志：$logPath"

Push-Location (Join-Path $repoRoot 'server-api')
try {
    & .\mvnw.cmd -o spring-boot:run 2>&1 | Tee-Object -FilePath $logPath
    $exitCode = $LASTEXITCODE
}
finally {
    Pop-Location
}

exit $exitCode
