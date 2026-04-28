# 智慧医疗压测一键执行脚本
# 用法: .\run-pressure-test.ps1 [-Concurrency 50|100|200] [-Duration 120] [-Target localhost]
#
# 依赖: JMeter 已安装且 jmeter 在 PATH 中

param(
    [ValidateSet(50, 100, 200, 400)]
    [int]$Concurrency = 100,

    [int]$Duration = 120,

    [string]$Target = "localhost",

    [string]$OutputDir = "results"
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$JmxFile   = Join-Path $ScriptDir "hospital-pressure-test.jmx"
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$ResultDir = Join-Path $ScriptDir "$OutputDir\$Timestamp"

if (!(Test-Path $JmxFile)) {
    Write-Host "[ERROR] 找不到 $JmxFile" -ForegroundColor Red
    exit 1
}

Write-Host "================================" -ForegroundColor Cyan
Write-Host " 智慧医疗压测" -ForegroundColor Cyan
Write-Host " 目标: $Target" -ForegroundColor Cyan
Write-Host " 并发: $Concurrency" -ForegroundColor Cyan
Write-Host " 持续: ${Duration}s" -ForegroundColor Cyan
Write-Host " 结果: $ResultDir" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan

# 覆盖 JMX 中的变量
$JtlFile = Join-Path $ResultDir "result.jtl"

jmeter -n -t $JmxFile `
    -J TARGET_HOST=$Target `
    -J READ_CONCURRENCY=$Concurrency `
    -J DURATION=$Duration `
    -l $JtlFile `
    -e -o $ResultDir

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "[√] 压测完成，HTML 报告: $ResultDir/index.html" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "[!] 压测异常退出，code=$LASTEXITCODE" -ForegroundColor Red
}
