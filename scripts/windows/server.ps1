<#
.SYNOPSIS
  AionEmu Windows 部署与运行脚本 / Windows deployment and run script.

.DESCRIPTION
  仅依赖 Windows 自带的 Windows PowerShell 5.1、curl.exe 与 tar.exe（Windows 10 1803+）。
  一键部署会：下载并解压 JRE 25 与便携 MySQL 8.4 → 初始化数据目录 → 启动 MySQL →
  配置 root 密码与 aion_emu 库 → 写入 database.properties → 启动服务端。
  Depends only on the bundled Windows PowerShell 5.1, curl.exe and tar.exe (Windows 10 1803+).
  One-click deploy downloads and extracts JRE 25 and a portable MySQL 8.4, initializes the data
  directory, starts MySQL, configures the root password and the aion_emu database, writes
  database.properties, then starts the server.

  注意：服务端标准输出进入其自带的（隐藏）控制台窗口，运行日志由 logback 写入 log\aionemu.log。
  NOTE: the server's stdout goes to its own (hidden) console window; logback writes log\aionemu.log.

.EXAMPLE
  deploy.bat        一键部署并启动 / deploy and start
  start.bat         启动服务端 / start the server
  stop.bat          优雅停止服务端 / stop the server gracefully
  stop.bat -All     同时停止便携 MySQL / also stop the portable MySQL
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('deploy', 'start', 'stop', 'status')]
    [string]$Action = 'deploy',

    # 仅 stop 使用：同时停止便携 MySQL / stop action only: also stop the portable MySQL
    [switch]$All
)

$ErrorActionPreference = 'Stop'
try { [Console]::OutputEncoding = [Text.Encoding]::UTF8 } catch { }
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

# ---- 路径 / Paths -----------------------------------------------------------
$ScriptDir = $PSScriptRoot
$AionHome = $null
# 打包布局（脚本与 jar 同目录）或仓库布局（scripts\windows\ → ..\..\aion）都能定位。
# Resolves both the packaged layout (script next to the jar) and the repo layout (scripts\windows).
foreach ($candidate in @($ScriptDir, (Join-Path $ScriptDir '..\..\aion'))) {
    if (Test-Path (Join-Path $candidate 'AionEmu.jar')) { $AionHome = (Resolve-Path $candidate).Path; break }
}
if (-not $AionHome) {
    throw '找不到 AionEmu.jar / AionEmu.jar not found. 先执行 bash scripts/package.sh 打包 / run it first.'
}

$JarPath      = Join-Path $AionHome 'AionEmu.jar'
$RuntimeDir   = Join-Path $AionHome 'runtime'
$JavaExe      = Join-Path $RuntimeDir 'jre\bin\java.exe'
$MysqlHome    = Join-Path $RuntimeDir 'mysql'
$MysqlData    = Join-Path $RuntimeDir 'mysql-data'
$MysqlExe     = Join-Path $MysqlHome 'bin\mysqld.exe'
$MysqlCli     = Join-Path $MysqlHome 'bin\mysql.exe'
$MysqlAdmin   = Join-Path $MysqlHome 'bin\mysqladmin.exe'
$MysqlPidFile = Join-Path $RuntimeDir 'mysql.pid'
$LogDir       = Join-Path $AionHome 'log'
$PidFile      = Join-Path $LogDir 'aionemu.pid'
$DbProps      = Join-Path $AionHome 'config\network\database.properties'

# 与 aion\config\network\database.properties 默认值一致 / matches the default database.properties.
$DbName        = 'aion_emu'
$MysqlPort     = 3306
$MysqlPassword = '123456'

$AdoptiumApi   = 'https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jre/hotspot/normal/eclipse'
$TunaListing   = 'https://mirrors.tuna.tsinghua.edu.cn/Adoptium/25/jre/x64/windows/'
$MysqlPage     = 'https://dev.mysql.com/downloads/mysql/8.4.html'
$MysqlCdn      = 'https://cdn.mysql.com/Downloads/MySQL-8.4/'
$MysqlFallback = 'mysql-8.4.11-winx64.zip'

# ---- 输出 / Output ----------------------------------------------------------
function Write-Step([string]$Message) { Write-Host "[AionEmu] $Message" -ForegroundColor Cyan }
function Write-Ok([string]$Message)   { Write-Host "[  OK  ] $Message" -ForegroundColor Green }
function Write-Note([string]$Message) { Write-Host "[ WARN ] $Message" -ForegroundColor Yellow }
function Fail([string]$Message) {
    Write-Host "[ FAIL ] $Message" -ForegroundColor Red
    exit 1
}

# ---- 通用工具 / Shared helpers ---------------------------------------------
function Get-File([string]$Url, [string]$Destination) {
    Write-Step "下载 / downloading: $Url"
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Destination) | Out-Null
    Remove-Item $Destination -Force -ErrorAction SilentlyContinue
    if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
        & curl.exe -fL --retry 3 --retry-delay 2 --connect-timeout 20 -o $Destination $Url 2>$null
        if ($LASTEXITCODE -ne 0) { return $false }
    } else {
        try { Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $Destination } catch { return $false }
    }
    return (Test-Path $Destination) -and ((Get-Item $Destination).Length -gt 0)
}

function Expand-Zip([string]$Zip, [string]$Destination) {
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    if (Get-Command tar.exe -ErrorAction SilentlyContinue) {
        & tar.exe -xf $Zip -C $Destination 2>$null
        if ($LASTEXITCODE -eq 0) { return }
    }
    Expand-Archive -Path $Zip -DestinationPath $Destination -Force
}

function Get-RecordedPid([string]$File) {
    if (-not (Test-Path $File)) { return $null }
    $text = (Get-Content $File -First 1).Trim()
    $value = 0
    if ([int]::TryParse($text, [ref]$value)) { return $value }
    return $null
}

function Test-Process([Nullable[int]]$ProcessId) {
    if (-not $ProcessId) { return $false }
    return [bool](Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)
}

function Test-PortOpen([int]$Port) {
    try {
        $client = New-Object Net.Sockets.TcpClient
        $client.Connect('127.0.0.1', $Port)
        $client.Close()
        return $true
    } catch { return $false }
}

# mysql.exe 包装：强制 TCP 并统一 host/port，退出码留在 $LASTEXITCODE 供调用方判断。
# mysql.exe wrapper: forces TCP and a fixed host/port; the exit code stays in $LASTEXITCODE.
function Invoke-MysqlCli {
    param([string[]]$Arguments, [string]$Password = $null)
    $pw = if ($null -ne $Password) { $Password } else { $MysqlPassword }
    $argv = @('--no-defaults', '--protocol=TCP', '-u', 'root', '-h', '127.0.0.1', '-P', "$MysqlPort")
    if ($pw) { $argv += "--password=$pw" }
    $argv += $Arguments
    & $MysqlCli @argv 2>$null
}

# database.properties 是凭据唯一维护点：存在时以它为准，避免脚本与应用各说各话。
# database.properties is the single source of truth for credentials: honor it when present.
function Read-DbSettings {
    if (-not (Test-Path $DbProps)) { return }
    foreach ($line in (Get-Content $DbProps)) {
        if ($line -match '^\s*database\.password\s*=\s*(\S.*?)\s*$') { $script:MysqlPassword = $Matches[1] }
        if ($line -match '^\s*database\.url\s*=\s*jdbc:mysql://[^:/]+:(\d+)/([^?]+)') {
            $script:MysqlPort = [int]$Matches[1]
            $script:DbName = $Matches[2]
        }
    }
}

# ---- 下载源发现 / Download source discovery --------------------------------
function Resolve-JreUrls {
    $urls = @($AdoptiumApi)
    try {
        $listing = Invoke-WebRequest -UseBasicParsing -Uri $TunaListing -TimeoutSec 30
        $names = [regex]::Matches($listing.Content, 'OpenJDK25U-jre_x64_windows_hotspot_[0-9._]+\.zip') |
            ForEach-Object { $_.Value } | Sort-Object -Unique
        if ($names.Count -gt 0) {
            # 版本号是 25.0.4.1_1 这类 5 段式，[version] 只支持 4 段，故按纯数字比较。
            # The version is 5-part (25.0.4.1_1) and [version] supports only 4 parts, so compare digits.
            $latest = $names | Sort-Object { [long]($_ -replace '\D', '') } | Select-Object -Last 1
            $urls += "$TunaListing$latest"
        }
    } catch { Write-Note "TUNA 镜像列表不可达，仅用官方源 / TUNA listing unreachable, official source only" }
    return $urls
}

function Resolve-MysqlUrl {
    try {
        $page = Invoke-WebRequest -UseBasicParsing -Uri $MysqlPage -TimeoutSec 30
        $names = [regex]::Matches($page.Content, 'mysql-8\.4\.[0-9]+-winx64\.zip') |
            ForEach-Object { $_.Value } | Sort-Object -Unique
        if ($names.Count -gt 0) {
            $latest = $names |
                Sort-Object { [int]($_ -replace '^mysql-8\.4\.', '' -replace '-winx64\.zip$', '') } |
                Select-Object -Last 1
            return "$MysqlCdn$latest"
        }
    } catch { Write-Note "MySQL 版本页不可达，使用内置版本 / version page unreachable, using the built-in one" }
    return "$MysqlCdn$MysqlFallback"
}

# ---- 安装 / Installation ----------------------------------------------------
function Install-Jre {
    if (Test-Path $JavaExe) { Write-Ok "JRE 已安装 / JRE present: $JavaExe"; return }
    Write-Step '安装 JRE 25 / installing JRE 25'
    $zip = Join-Path $env:TEMP 'aionemu-jre.zip'
    $downloaded = $false
    foreach ($url in (Resolve-JreUrls)) {
        if (Get-File $url $zip) { $downloaded = $true; break }
        Write-Note '该源下载失败，尝试下一个 / source failed, trying the next one'
    }
    if (-not $downloaded) { Fail 'JRE 下载失败 / JRE download failed' }
    Expand-Zip $zip $RuntimeDir
    Remove-Item $zip -Force -ErrorAction SilentlyContinue
    # 按目录内容（bin\java.exe）定位解压根，不依赖顶层目录名 / locate the extracted root by content.
    $extracted = Get-ChildItem $RuntimeDir -Directory |
        Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } | Select-Object -First 1
    if (-not $extracted) { Fail 'JRE 解压结果异常 / unexpected JRE extraction result' }
    if (Test-Path (Join-Path $RuntimeDir 'jre')) { Remove-Item (Join-Path $RuntimeDir 'jre') -Recurse -Force }
    Move-Item $extracted.FullName (Join-Path $RuntimeDir 'jre')
    if (-not (Test-Path $JavaExe)) { Fail '未找到 java.exe / java.exe not found after extraction' }
    Write-Ok "JRE 安装完成 / JRE installed: $JavaExe"
}

function Install-Mysql {
    if (Test-Path $MysqlExe) { Write-Ok "MySQL 已安装 / MySQL present: $MysqlHome"; return }
    Write-Step '安装便携 MySQL 8.4 / installing portable MySQL 8.4'
    $zip = Join-Path $env:TEMP 'aionemu-mysql.zip'
    if (-not (Get-File (Resolve-MysqlUrl) $zip)) { Fail 'MySQL 下载失败 / MySQL download failed' }
    Expand-Zip $zip $RuntimeDir
    Remove-Item $zip -Force -ErrorAction SilentlyContinue
    $extracted = Get-ChildItem $RuntimeDir -Directory |
        Where-Object { Test-Path (Join-Path $_.FullName 'bin\mysqld.exe') } | Select-Object -First 1
    if (-not $extracted) { Fail 'MySQL 解压结果异常 / unexpected MySQL extraction result' }
    if (Test-Path $MysqlHome) { Remove-Item $MysqlHome -Recurse -Force }
    Move-Item $extracted.FullName $MysqlHome
    if (-not (Test-Path $MysqlExe)) { Fail '未找到 mysqld.exe / mysqld.exe not found after extraction' }
    Write-Ok "MySQL 安装完成 / MySQL installed: $MysqlHome"
}

# ---- MySQL 运行 / MySQL runtime --------------------------------------------
function Initialize-MysqlData {
    if (Test-Path (Join-Path $MysqlData 'mysql')) { return }
    Write-Step "初始化 MySQL 数据目录 / initializing MySQL data dir: $MysqlData"
    if (Test-Path $MysqlData) { Remove-Item $MysqlData -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $MysqlData | Out-Null
    & $MysqlExe --no-defaults --initialize-insecure "--basedir=$MysqlHome" "--datadir=$MysqlData" 2>$null
    if ($LASTEXITCODE -ne 0) {
        Fail 'MySQL 初始化失败。常见原因：缺少 Microsoft Visual C++ 运行库 / MySQL init failed (often a missing VC++ runtime).'
    }
    Write-Ok 'MySQL 数据目录已初始化（root 初始为空密码）/ data dir initialized (root starts with an empty password)'
}

function Ensure-MysqlRunning {
    if (Test-Process (Get-RecordedPid $MysqlPidFile)) {
        Write-Ok '便携 MySQL 已在运行 / portable MySQL already running'
        return
    }
    if (Test-PortOpen $MysqlPort) {
        # 端口已监听：能用配置凭据连上就直接复用 / reuse a reachable server already listening on the port
        Invoke-MysqlCli @('-e', 'SELECT 1')
        if ($LASTEXITCODE -eq 0) {
            Write-Ok "复用 $MysqlPort 端口上已运行的 MySQL / reusing the MySQL already on port $MysqlPort"
            return
        }
        Fail "端口 $MysqlPort 已被占用且无法用配置凭据连接 / port $MysqlPort is in use but rejects the configured credentials."
    }
    Write-Step "启动 MySQL (port $MysqlPort) / starting MySQL"
    $process = Start-Process -FilePath $MysqlExe -PassThru -WindowStyle Hidden -ArgumentList @(
        '--no-defaults', "--basedir=$MysqlHome", "--datadir=$MysqlData",
        "--port=$MysqlPort", '--bind-address=127.0.0.1'
    )
    Set-Content -Path $MysqlPidFile -Value $process.Id -Encoding ASCII
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 2
        if (Test-PortOpen $MysqlPort) { Write-Ok 'MySQL 已启动 / MySQL started'; return }
    }
    Fail 'MySQL 启动超时（缺少 VC++ 运行库时请先安装）/ MySQL not ready within 60s (install the VC++ runtime if missing).'
}

function Initialize-MysqlAccount {
    # 已能用目标密码登录 → 无需处理 / already usable with the target password
    Invoke-MysqlCli @('-e', 'SELECT 1')
    if ($LASTEXITCODE -eq 0) { return }
    # 空密码可用 → 设置密码并补齐 127.0.0.1 账号 / empty password works → set it and add the 127.0.0.1 account
    Invoke-MysqlCli @('-e', 'SELECT 1') -Password ''
    if ($LASTEXITCODE -ne 0) { Fail '无法以 root 连接 MySQL / cannot authenticate to MySQL as root' }
    Write-Step '配置 root 密码 / configuring the root password'
    $sql = "ALTER USER 'root'@'localhost' IDENTIFIED BY '$MysqlPassword'; " +
           "CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY '$MysqlPassword'; " +
           "GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION; FLUSH PRIVILEGES;"
    Invoke-MysqlCli @('-e', $sql) -Password ''
    if ($LASTEXITCODE -ne 0) { Fail 'root 密码配置失败 / failed to configure the root password' }
    Write-Ok 'root 密码已配置 / root password configured'
}

function Initialize-Database {
    $existing = Invoke-MysqlCli @('-N', '-B', '-e', "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='$DbName'")
    if ($LASTEXITCODE -eq 0 -and "$existing".Trim() -eq $DbName) {
        Write-Ok "数据库 $DbName 已存在 / database present"
        return
    }
    Write-Step "创建数据库 $DbName (utf8mb4) / creating database"
    Invoke-MysqlCli @('-e', "CREATE DATABASE IF NOT EXISTS $DbName CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    if ($LASTEXITCODE -ne 0) { Fail '数据库创建失败 / failed to create the database' }
    Write-Ok "数据库 $DbName 已创建（Flyway 首次启动自动建表）/ created (Flyway builds the tables on first start)"
}

function Write-DbProperties {
    if (Test-Path $DbProps) {
        Write-Ok "保留现有 database.properties / keeping the existing database.properties"
        return
    }
    Write-Step "写入 database.properties（root/$MysqlPassword）/ writing $DbProps"
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $DbProps) | Out-Null
    $url = "jdbc:mysql://127.0.0.1:$MysqlPort/$DbName" +
           '?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true' +
           '&useLegacyDatetimeCode=false&serverTimezone=UTC'
    $lines = @(
        '# 由 scripts\windows\deploy.bat 生成的部署配置 / generated by scripts\windows\deploy.bat'
        '# 凭据唯一维护在此文件 / credentials are maintained only in this file'
        'database.driver = com.mysql.cj.jdbc.Driver'
        "database.url = $url"
        'database.user = root'
        "database.password = $MysqlPassword"
        'database.maxconnections = 20'
        'database.hikari.maxLifetime = 600000'
        'database.hikari.connectionTestQuery = SELECT 1'
    )
    # UTF-8 无 BOM：java.util.Properties 会把 BOM 当成首个键的一部分 / UTF-8 without a BOM.
    [System.IO.File]::WriteAllLines($DbProps, $lines, (New-Object System.Text.UTF8Encoding($false)))
    Write-Ok 'database.properties 已写入 / written'
}

# ---- 服务端 / Server --------------------------------------------------------
function Start-Server {
    $existingPid = Get-RecordedPid $PidFile
    if (Test-Process $existingPid) { Write-Ok "AionEmu 已在运行 / already running (pid $existingPid)"; return }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

    $javaArgs = @(
        '-Xms2g', '-Xmx8g',
        '-XX:+UseG1GC', '-XX:MaxGCPauseMillis=200', '-XX:InitiatingHeapOccupancyPercent=30',
        '-XX:+ParallelRefProcEnabled', '-XX:+UseStringDeduplication',
        '-XX:+HeapDumpOnOutOfMemoryError', "-XX:HeapDumpPath=`"$LogDir`"", '-XX:+ExitOnOutOfMemoryError',
        '-Dfile.encoding=UTF-8', '-Djava.net.preferIPv4Stack=true', '-Duser.timezone=Asia/Shanghai',
        "-Daion.home=`"$AionHome`"", "-Daion.log.dir=`"$LogDir`"",
        '-jar', "`"$JarPath`""
    )
    Write-Step '启动 AionEmu / starting AionEmu'
    $process = Start-Process -FilePath $JavaExe -ArgumentList $javaArgs -WorkingDirectory $AionHome `
        -WindowStyle Hidden -PassThru
    Set-Content -Path $PidFile -Value $process.Id -Encoding ASCII
    Start-Sleep -Seconds 3
    if (-not (Get-Process -Id $process.Id -ErrorAction SilentlyContinue)) {
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
        Fail "AionEmu 启动后立即退出 / exited right after launch. 查看日志 / see $LogDir"
    }
    Write-Ok "AionEmu 已启动 / started (pid $($process.Id))"
    Write-Host '        游戏端口 / game  : 7777'
    Write-Host '        登录端口 / login : 2106'
    Write-Host '        聊天端口 / chat  : 10241'
    Write-Host "        日志 / log       : $LogDir\aionemu.log"
    Write-Host '        停止 / stop      : stop.bat'
}

function Send-CtrlC([int]$TargetPid) {
    # Windows 无 SIGTERM：附加到目标控制台并发 CTRL_C_EVENT，触发 JVM 关闭钩子（优雅停机）。
    # Windows has no SIGTERM: attach to the target console and raise CTRL_C_EVENT so the JVM runs
    # its shutdown hooks (graceful stop). Runs in a child process so our own console stays attached.
    $code = @"
Add-Type -Namespace AionEmu -Name NativeMethods -MemberDefinition @'
[System.Runtime.InteropServices.DllImport("kernel32.dll", SetLastError=true)] public static extern bool FreeConsole();
[System.Runtime.InteropServices.DllImport("kernel32.dll", SetLastError=true)] public static extern bool AttachConsole(uint dwProcessId);
[System.Runtime.InteropServices.DllImport("kernel32.dll", SetLastError=true)] public static extern bool SetConsoleCtrlHandler(System.IntPtr handler, bool add);
[System.Runtime.InteropServices.DllImport("kernel32.dll", SetLastError=true)] public static extern bool GenerateConsoleCtrlEvent(uint dwCtrlEvent, uint dwProcessGroupId);
'@
[void][AionEmu.NativeMethods]::FreeConsole()
if (-not [AionEmu.NativeMethods]::AttachConsole($TargetPid)) { exit 1 }
[void][AionEmu.NativeMethods]::SetConsoleCtrlHandler([IntPtr]::Zero, `$true)
[void][AionEmu.NativeMethods]::GenerateConsoleCtrlEvent(0, 0)
Start-Sleep -Milliseconds 400
exit 0
"@
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($code))
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -EncodedCommand $encoded 2>$null | Out-Null
    return ($LASTEXITCODE -eq 0)
}

function Stop-Server {
    $id = Get-RecordedPid $PidFile
    if (-not (Test-Process $id)) {
        Write-Ok 'AionEmu 未在运行 / not running'
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
        return
    }
    Write-Step "优雅停止 AionEmu (pid $id) / stopping gracefully, up to 120s"
    if (-not (Send-CtrlC $id)) { Write-Note '无法发送 Ctrl+C，将直接终止 / cannot send Ctrl+C, terminating directly' }
    for ($i = 0; $i -lt 120; $i++) {
        Start-Sleep -Seconds 1
        if (-not (Test-Process $id)) {
            Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
            Write-Ok 'AionEmu 已停止 / stopped'
            return
        }
    }
    Write-Note '优雅停止超时，强制终止 / graceful stop timed out, killing'
    Stop-Process -Id $id -Force -ErrorAction SilentlyContinue
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    Write-Ok 'AionEmu 已强制停止 / force-stopped'
}

function Stop-Mysql {
    $id = Get-RecordedPid $MysqlPidFile
    if (-not (Test-Process $id)) {
        Write-Ok '便携 MySQL 未在运行 / portable MySQL not running'
        Remove-Item $MysqlPidFile -Force -ErrorAction SilentlyContinue
        return
    }
    Write-Step "停止便携 MySQL (pid $id) / stopping portable MySQL"
    & $MysqlAdmin --no-defaults --protocol=TCP -u root -h 127.0.0.1 -P $MysqlPort "--password=$MysqlPassword" shutdown 2>$null
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 1
        if (-not (Test-Process $id)) { break }
    }
    if (Test-Process $id) { Stop-Process -Id $id -Force -ErrorAction SilentlyContinue }
    Remove-Item $MysqlPidFile -Force -ErrorAction SilentlyContinue
    Write-Ok '便携 MySQL 已停止 / portable MySQL stopped'
}

# ---- 动作分派 / Action dispatch --------------------------------------------
function Initialize-Runtime {
    # 先读配置（若有），使端口/库名/密码与运行时保持一致，再决定是否需要初始化数据目录。
    # Read the config first so port/db/password match the runtime before deciding on initialization.
    Read-DbSettings
    if (-not (Test-PortOpen $MysqlPort)) { Initialize-MysqlData }
    Ensure-MysqlRunning
    Initialize-MysqlAccount
    Initialize-Database
    Write-DbProperties
}

switch ($Action) {
    'deploy' {
        Write-Host '=== AionEmu Windows 一键部署 / one-click deployment ===' -ForegroundColor Cyan
        Write-Host "部署目录 / deploy dir: $AionHome"
        Install-Jre
        Install-Mysql
        Initialize-Runtime
        Start-Server
    }
    'start' {
        if (-not (Test-Path $JavaExe)) { Fail '未安装 JRE，请先运行 deploy.bat / run deploy.bat first.' }
        if (-not (Test-Path $MysqlExe)) { Fail '未安装 MySQL，请先运行 deploy.bat / run deploy.bat first.' }
        Initialize-Runtime
        Start-Server
    }
    'stop' {
        Stop-Server
        if ($All) { Read-DbSettings; Stop-Mysql }
    }
    'status' {
        $serverPid = Get-RecordedPid $PidFile
        if (Test-Process $serverPid) { Write-Host "AionEmu 运行中 / running (pid $serverPid)" -ForegroundColor Green }
        else { Write-Host 'AionEmu 未运行 / not running' -ForegroundColor Yellow }
        if (Test-Process (Get-RecordedPid $MysqlPidFile)) { Write-Host '便携 MySQL 运行中 / portable MySQL running' -ForegroundColor Green }
        else { Write-Host '便携 MySQL 未运行 / portable MySQL not running' -ForegroundColor Yellow }
    }
}
