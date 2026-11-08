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

function ConvertTo-JavaLiteral {
    param([string]$Value)
    $escaped = $Value.Replace('\', '\\').Replace('"', '\"')
    return '"' + $escaped + '"'
}

function Invoke-JdbcSql {
    param(
        [string]$JshellPath,
        [string]$ConnectorJar,
        [string]$Url,
        [string]$User,
        [string]$Password,
        [string]$Sql,
        [string]$SuccessMarker,
        [string]$Label
    )

    $tempDir = Join-Path $repoRoot 'temp'
    New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
    $scriptFile = Join-Path $tempDir ("mysql-check-" + [guid]::NewGuid().ToString('N') + '.jsh')

    $template = @'
import java.sql.*;
Connection conn = DriverManager.getConnection(__URL__, __USER__, __PASSWORD__);
Statement st = conn.createStatement();
boolean ok = false;
try { st.execute(__SQL__); ok = true; } catch (Exception e) { e.printStackTrace(); }
st.close();
conn.close();
System.out.println(ok ? __MARKER__ : "__FAILED__");
/exit
'@

    $content = $template
    $content = $content.Replace('__URL__', (ConvertTo-JavaLiteral $Url))
    $content = $content.Replace('__USER__', (ConvertTo-JavaLiteral $User))
    $content = $content.Replace('__PASSWORD__', (ConvertTo-JavaLiteral $Password))
    $content = $content.Replace('__SQL__', (ConvertTo-JavaLiteral $Sql))
    $content = $content.Replace('__MARKER__', (ConvertTo-JavaLiteral $SuccessMarker))
    Set-Content -Path $scriptFile -Value $content -Encoding utf8NoBOM

    try {
        $output = & $JshellPath --class-path $ConnectorJar $scriptFile 2>&1 | Out-String
    }
    finally {
        Remove-Item $scriptFile -Force -ErrorAction SilentlyContinue
    }

    if ($output -notmatch [regex]::Escape($SuccessMarker)) {
        throw "$Label 失败：`n$output"
    }
}

$jdk = Resolve-Jdk
$env:JAVA_HOME = $jdk
$env:Path = "$(Join-Path $jdk 'bin');$env:Path"

$jshellPath = Join-Path $jdk 'bin\jshell.exe'
$connectorJar = Join-Path $env:USERPROFILE '.m2\repository\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar'

if (-not (Test-Path $jshellPath)) {
    Write-Host "[FAIL] 未找到 jshell：$jshellPath" -ForegroundColor Red
    exit 1
}
if (-not (Test-Path $connectorJar)) {
    Write-Host "[FAIL] 未找到 MySQL Connector/J：$connectorJar" -ForegroundColor Red
    exit 1
}

$pomPath = Join-Path $repoRoot 'server-api\pom.xml'
if (-not (Select-String -Path $pomPath -Pattern '<id>\s*mysql-check\s*</id>' -Quiet)) {
    Write-Host '[PENDING] 未找到 Maven profile mysql-check，待后端 agent 完成后验证。' -ForegroundColor Yellow
    exit 2
}

$testClassPath = Join-Path $repoRoot 'server-api\src\test\java\com\example\web\mysql\MysqlSchemaSmokeCheck.java'
if (-not (Test-Path $testClassPath)) {
    Write-Host '[PENDING] 未找到测试类 MysqlSchemaSmokeCheck，待后端 agent 完成后验证。' -ForegroundColor Yellow
    exit 2
}

if (-not $env:DB_HOST) { $env:DB_HOST = 'localhost' }
if (-not $env:DB_PORT) { $env:DB_PORT = '3306' }
if (-not $env:DB_USERNAME) { $env:DB_USERNAME = 'root' }
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = '123456' }

$dbName = 'forgestack_check_' + (Get-Date -Format 'yyyyMMddHHmmss')
$env:MYSQL_CHECK_DB = $dbName

$jdbcUrl = "jdbc:mysql://${env:DB_HOST}:${env:DB_PORT}/?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
$createSql = 'CREATE DATABASE `' + $dbName + '` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'
$dropSql = 'DROP DATABASE IF EXISTS `' + $dbName + '`'

$created = $false
$mavenExit = 1

try {
    Write-Host "创建测试库：$dbName"
    Invoke-JdbcSql -JshellPath $jshellPath -ConnectorJar $connectorJar -Url $jdbcUrl `
        -User $env:DB_USERNAME -Password $env:DB_PASSWORD -Sql $createSql -SuccessMarker 'DB_CREATED' -Label '创建测试库'
    $created = $true

    Write-Host '==> server-api mysql schema smoke check' -ForegroundColor Cyan
    Push-Location (Join-Path $repoRoot 'server-api')
    try {
        & .\mvnw.cmd -o -Pmysql-check test -Dtest=MysqlSchemaSmokeCheck
        $mavenExit = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }

    if ($mavenExit -ne 0) {
        Write-Host "[FAIL] MysqlSchemaSmokeCheck (exit $mavenExit)" -ForegroundColor Red
    }
}
finally {
    if ($created) {
        Write-Host "清理测试库：$dbName"
        try {
            Invoke-JdbcSql -JshellPath $jshellPath -ConnectorJar $connectorJar -Url $jdbcUrl `
                -User $env:DB_USERNAME -Password $env:DB_PASSWORD -Sql $dropSql -SuccessMarker 'DB_DROPPED' -Label '清理测试库'
        }
        catch {
            Write-Warning "清理测试库失败：$($_.Exception.Message)"
        }
    }
}

if ($mavenExit -eq 0 -and $created) {
    Write-Host ''
    Write-Host 'MYSQL SCHEMA CHECK PASSED' -ForegroundColor Green
    exit 0
}

Write-Host ''
Write-Host 'MYSQL SCHEMA CHECK FAILED' -ForegroundColor Red
exit 1
