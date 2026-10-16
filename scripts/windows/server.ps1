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
  deploy.bat        一键部署并前台启动（Ctrl+C 同时停止游戏与 MySQL）
  start.bat         前台启动服务端，实时日志（Ctrl+C 停止游戏）
  stop.bat          停止后台残留的服务端（一般用 Ctrl+C 即可）
  start-mysql.bat   前台启动便携 MySQL（Ctrl+C 停止 MySQL）
  stop-mysql.bat    停止便携 MySQL
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('deploy', 'start', 'stop', 'start-mysql', 'stop-mysql', 'status')]
    [string]$Action = 'deploy'
)

$ErrorActionPreference = 'Stop'
try { [Console]::OutputEncoding = [Text.Encoding]::UTF8 } catch { }
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

# 脚本版本标记：便于确认 .bat 实际跑的是哪一版（排障用）。
# Script version marker: identifies which revision the .bat actually ran (for troubleshooting).
$ScriptVersion = '2026-10-16.2'

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
$MysqlIni     = Join-Path $MysqlHome 'my.ini'
$LogDir       = Join-Path $AionHome 'log'
$PidFile      = Join-Path $LogDir 'aionemu.pid'
$DbProps      = Join-Path $AionHome 'config\network\database.properties'
$NetworkProps = Join-Path $AionHome 'config\network\network.properties'
$GameProps    = Join-Path $AionHome 'config\main\gameserver.properties'

# 与 aion\config\network\database.properties 默认值一致 / matches the default database.properties.
$script:DbName        = 'aion_emu'
$script:MysqlPort     = 3306
$script:MysqlPassword = '123456'

# JRE：清华 TUNA 镜像优先（国内快），官方 Adoptium API 兜底（会 302 到 GitHub）。
# JRE: the TUNA mirror first (fast in China), the official Adoptium API as fallback (302s to GitHub).
$TunaListing   = 'https://mirrors.tuna.tsinghua.edu.cn/Adoptium/25/jre/x64/windows/'
$AdoptiumApi   = 'https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jre/hotspot/normal/eclipse'
# MySQL：华为云镜像优先（8.0.29，约 11MB/s），官方 CDN 兜底（当前 8.4.x）。
# MySQL: the Huawei Cloud mirror first (8.0.29, ~11MB/s), the official CDN as fallback (8.4.x).
# 国内镜像不提供 8.4 的 Windows zip；迁移脚本只用 utf8mb4_general_ci + InnoDB，8.0.29 完全兼容。
# China mirrors have no 8.4 Windows zip; the migration script only uses utf8mb4_general_ci + InnoDB, so 8.0.29 is fully compatible.
$MysqlMirror   = 'https://mirrors.huaweicloud.com/mysql/Downloads/MySQL-8.0/mysql-8.0.29-winx64.zip'
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

# 运行原生命令并取回权威退出码与标准输出/错误。用 Start-Process 的进程 .ExitCode（可靠），
# 不再依赖 PowerShell 里传播不稳定的 $LASTEXITCODE。退出码在 $global:LASTEXITCODE，
# 标准输出在 $script:NativeOutput，错误输出在 $script:NativeError。
# Runs a native command and returns its authoritative exit code, stdout and stderr. Uses Start-Process's
# process .ExitCode (reliable) instead of the flaky $LASTEXITCODE.
function Invoke-Native {
    param([string]$FilePath, [string[]]$Arguments)
    $outFile = Join-Path $env:TEMP ("aionemu-" + [guid]::NewGuid().ToString('N') + ".out")
    $errFile = "$outFile.err"
    $proc = Start-Process -FilePath $FilePath -NoNewWindow -Wait -PassThru `
        -RedirectStandardOutput $outFile -RedirectStandardError $errFile -ArgumentList $Arguments
    $script:NativeOutput = if (Test-Path $outFile) { Get-Content $outFile -Raw } else { '' }
    $script:NativeError  = if (Test-Path $errFile) { Get-Content $errFile -Raw } else { '' }
    Remove-Item $outFile, $errFile -Force -ErrorAction SilentlyContinue
    $global:LASTEXITCODE = $proc.ExitCode
}

function Get-File([string]$Url, [string]$Destination) {
    Write-Step "下载 / downloading: $Url"
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Destination) | Out-Null
    Remove-Item $Destination -Force -ErrorAction SilentlyContinue
    if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
        # 用 Start-Process 让 curl 直接写控制台（含进度表），避开 PS 5.1 把原生 stderr 当终止错误的坑。
        # Start-Process lets curl write to the console directly (progress meter), avoiding
        # PowerShell 5.1's terminating native-stderr error.
        $proc = Start-Process -FilePath 'curl.exe' -NoNewWindow -Wait -PassThru -ArgumentList @(
            '-fL', '--retry', '3', '--retry-delay', '2', '--connect-timeout', '20',
            '-o', "`"$Destination`"", "`"$Url`"")
        if ($proc.ExitCode -ne 0) { Write-Note "下载失败，curl 退出码 / download failed, exit code $($proc.ExitCode)"; return $false }
    } else {
        try {
            $client = New-Object System.Net.WebClient
            $client.Headers.Add('User-Agent', 'AionEmu-Deploy')
            $client.DownloadFile($Url, $Destination)
            $client.Dispose()
        } catch { Write-Note "下载失败 / download failed: $($_.Exception.Message)"; return $false }
    }
    return (Test-Path $Destination) -and ((Get-Item $Destination).Length -gt 0)
}

function Expand-Zip([string]$Zip, [string]$Destination) {
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    if (Get-Command tar.exe -ErrorAction SilentlyContinue) {
        Invoke-Native -FilePath 'tar.exe' -Arguments @('-xf', $Zip, '-C', $Destination)
        if ($global:LASTEXITCODE -eq 0) { return }
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

# mysql.exe 包装：强制 TCP 并统一 host/port；输出为 mysql 的文本，退出码见 $global:LASTEXITCODE。
# mysql.exe wrapper: forces TCP and a fixed host/port; output is mysql's text, exit code in $global:LASTEXITCODE.
function Invoke-MysqlCli {
    param([string[]]$Arguments)
    $pw = $script:MysqlPassword
    $oldPwd = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $pw
        $argv = @('--no-defaults', '--protocol=TCP', '--get-server-public-key',
            '-u', 'root', '-h', '127.0.0.1', '-P', "$script:MysqlPort", "--password=$pw")
        $argv += $Arguments
        Invoke-Native -FilePath $MysqlCli -Arguments $argv
    } finally {
        if ($null -ne $oldPwd) { $env:MYSQL_PWD = $oldPwd }
        else { Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue }
    }
}

# 从已存在的 database.properties 采纳端口/库名；密码固定用脚本默认（$MysqlPassword），
# 避免文件里残留的旧密码被沿用，导致应用与服务端密码不一致。
# Adopts the port/db name from an existing database.properties; the password stays the script
# default ($MysqlPassword) so a stale password in the file cannot make the app and server disagree.
function Read-DbSettings {
    if (-not (Test-Path $DbProps)) { return }
    foreach ($line in (Get-Content $DbProps)) {
        if ($line -match '^\s*database\.url\s*=\s*jdbc:mysql://[^:/]+:(\d+)/([^?]+)') {
            $script:MysqlPort = [int]$Matches[1]
            $script:DbName = $Matches[2]
        }
    }
}

# ---- 下载源发现 / Download source discovery --------------------------------
function Resolve-JreUrls {
    $urls = @()
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
    $urls += $AdoptiumApi
    return $urls
}

function Resolve-MysqlUrls {
    # 华为云镜像优先，官方 CDN（当前 8.4.x）兜底。
    # Huawei Cloud first, the official CDN (currently 8.4.x) as fallback.
    $urls = @($MysqlMirror)
    try {
        $page = Invoke-WebRequest -UseBasicParsing -Uri $MysqlPage -TimeoutSec 30
        $names = [regex]::Matches($page.Content, 'mysql-8\.4\.[0-9]+-winx64\.zip') |
            ForEach-Object { $_.Value } | Sort-Object -Unique
        if ($names.Count -gt 0) {
            $latest = $names |
                Sort-Object { [int]($_ -replace '^mysql-8\.4\.', '' -replace '-winx64\.zip$', '') } |
                Select-Object -Last 1
            $urls += "$MysqlCdn$latest"
        }
    } catch { Write-Note "MySQL 版本页不可达，使用内置版本 / version page unreachable, using the built-in one" }
    $urls += "$MysqlCdn$MysqlFallback"
    return $urls
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
    Write-Step '安装便携 MySQL / installing portable MySQL'
    $zip = Join-Path $env:TEMP 'aionemu-mysql.zip'
    $downloaded = $false
    foreach ($url in (Resolve-MysqlUrls)) {
        if (Get-File $url $zip) { $downloaded = $true; break }
        Write-Note '该源下载失败，尝试下一个 / source failed, trying the next one'
    }
    if (-not $downloaded) { Fail 'MySQL 下载失败 / MySQL download failed' }
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
# 生成 my.ini：便携 MySQL 监听 0.0.0.0 以便远程访问（用户可自行改端口/绑定）。
# Writes my.ini: the portable MySQL binds 0.0.0.0 so remote clients can reach it.
function Write-MysqlConfig {
    New-Item -ItemType Directory -Force -Path $MysqlHome | Out-Null
    $lines = @(
        '# 由 scripts\windows\deploy.bat 生成 / generated by scripts\windows\deploy.bat'
        '# bind-address=0.0.0.0 允许远程访问；需同时在防火墙放行 TCP 3306。'
        '# bind-address=0.0.0.0 enables remote access; also open TCP 3306 in the firewall.'
        '# 服务端自身仍走 127.0.0.1 连接（见 database.properties）。'
        '# The server itself still connects over 127.0.0.1 (see database.properties).'
        '[mysqld]'
        'bind-address=0.0.0.0'
        "port=$script:MysqlPort"
        "basedir=$($MysqlHome.Replace('\', '/'))"
        "datadir=$($MysqlData.Replace('\', '/'))"
    )
    # UTF-8 无 BOM：mysqld 会把 BOM 当成键名的一部分 / UTF-8 without a BOM.
    [System.IO.File]::WriteAllLines($MysqlIni, $lines, (New-Object System.Text.UTF8Encoding($false)))
    Write-Ok "MySQL 配置已写入 / MySQL config written: $MysqlIni (bind-address=0.0.0.0)"
}

function Initialize-MysqlData {
    if (Test-Path (Join-Path $MysqlData 'mysql')) { return }
    Write-Step "初始化 MySQL 数据目录 / initializing MySQL data dir: $MysqlData"
    if (Test-Path $MysqlData) { Remove-Item $MysqlData -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $MysqlData | Out-Null
    Write-MysqlConfig
    $defaultsParam = '--defaults-file=' + $MysqlIni.Replace('\', '/')
    Invoke-Native -FilePath $MysqlExe -Arguments @($defaultsParam, '--initialize-insecure')
    if ($global:LASTEXITCODE -ne 0) {
        Fail 'MySQL 初始化失败。常见原因：缺少 Microsoft Visual C++ 运行库 / MySQL init failed (often a missing VC++ runtime).'
    }
    Write-Ok 'MySQL 数据目录已初始化（root 初始为空密码）/ data dir initialized (root starts with an empty password)'
}

function Ensure-MysqlRunning {
    # 0. 刷新 my.ini（bind-address/port），保证与脚本配置一致
    # 0. Refresh my.ini (bind-address/port) so it matches the script configuration.
    Write-MysqlConfig

    # 1. 检查便携 MySQL 是否已经在运行，并验证凭据是否有效
    $runningPid = Get-RecordedPid $MysqlPidFile
    $isPortableRunning = (Test-Process $runningPid) -or (Get-MysqlProcesses)

    if ($isPortableRunning) {
        Invoke-MysqlCli @('-eSELECT(1)')
        if ($global:LASTEXITCODE -eq 0) {
            Write-Ok "便携 MySQL 运行中且凭据有效 / portable MySQL already running with valid credentials"
            return
        }
        Write-Note "便携 MySQL 运行中但凭据无法连接（状态异常），正在重启修复... / portable MySQL running but unreachable, restarting to repair"
        Stop-Mysql
    } elseif (Test-PortOpen $script:MysqlPort) {
        Invoke-MysqlCli @('-eSELECT(1)')
        if ($global:LASTEXITCODE -eq 0) {
            Write-Ok "复用 $script:MysqlPort 端口上已运行的 MySQL / reusing the MySQL already on port $script:MysqlPort"
            return
        }
        Fail "端口 $script:MysqlPort 已被占用且无法用配置凭据连接 / port $script:MysqlPort is in use but rejects the configured credentials."
    }

    # 2. 生成 init.sql，在 mysqld 启动阶段内部强制确保 root 账号密码与数据库就绪（彻底摆脱客户端网络认证问题）
    # Write init.sql so mysqld configures the root accounts and database internally on startup.
    $initSqlFile = Join-Path $RuntimeDir 'init.sql'
    $sqlLines = @(
        "ALTER USER 'root'@'localhost' IDENTIFIED BY '$script:MysqlPassword';"
        "CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY '$script:MysqlPassword';"
        "ALTER USER 'root'@'127.0.0.1' IDENTIFIED BY '$script:MysqlPassword';"
        "CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '$script:MysqlPassword';"
        "ALTER USER 'root'@'%' IDENTIFIED BY '$script:MysqlPassword';"
        "GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' WITH GRANT OPTION;"
        "GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION;"
        "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;"
        "CREATE DATABASE IF NOT EXISTS $script:DbName CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
        "FLUSH PRIVILEGES;"
    )
    [System.IO.File]::WriteAllLines($initSqlFile, $sqlLines, (New-Object System.Text.UTF8Encoding($false)))

    Write-Step "启动 MySQL (port $script:MysqlPort, bind 0.0.0.0) / starting MySQL"
    $initParam = "--init-file=" + $initSqlFile.Replace('\', '/')
    # --defaults-file 必须是第一个参数；my.ini 提供 basedir/datadir/port/bind-address。
    # --defaults-file must come first; my.ini supplies basedir/datadir/port/bind-address.
    $defaultsParam = '--defaults-file=' + $MysqlIni.Replace('\', '/')
    $process = Start-Process -FilePath $MysqlExe -PassThru -WindowStyle Hidden -ArgumentList @(
        $defaultsParam, $initParam
    )
    Set-Content -Path $MysqlPidFile -Value $process.Id -Encoding ASCII

    $started = $false
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 2
        if (Test-PortOpen $script:MysqlPort) { $started = $true; break }
    }
    Remove-Item $initSqlFile -Force -ErrorAction SilentlyContinue
    if (-not $started) {
        Fail 'MySQL 启动超时（缺少 VC++ 运行库时请先安装）/ MySQL not ready within 60s (install the VC++ runtime if missing).'
    }

    # 3. 校验连接（使用零空格语句 -eSELECT(1)，避免 Start-Process 参数解析将空格后的数字误判为数据库名）
    Invoke-MysqlCli @('-eSELECT(1)')
    if ($global:LASTEXITCODE -ne 0) {
        Fail "MySQL 已启动但凭据验证失败 / MySQL started but credential verification failed: $($script:NativeError)"
    }
    Write-Ok "MySQL 已启动，root 密码已配置，数据库 $script:DbName 已就绪 / MySQL ready, root password and database configured"
}

function Write-DbProperties {
    $url = "jdbc:mysql://127.0.0.1:$script:MysqlPort/$script:DbName" +
           '?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true' +
           '&useLegacyDatetimeCode=false&serverTimezone=UTC'
    # 已有文件：仅在凭据与脚本不一致时才改写，避免把配置里过期的旧密码留给应用。
    # Existing file: rewrite only when the credentials differ, so a stale password cannot survive.
    if (Test-Path $DbProps) {
        $current = Get-Content $DbProps -Raw
        $consistent = $current -match "(?m)^\s*database\.password\s*=\s*$([regex]::Escape($script:MysqlPassword))\s*$" -and
                      $current -match '(?m)^\s*database\.user\s*=\s*root\s*$'
        if ($consistent) {
            Write-Ok "database.properties 凭据一致，保留 / credentials consistent, keeping the file"
            return
        }
        Write-Note "database.properties 凭据与脚本不一致，改写为 root/$script:MysqlPassword / credentials differ, rewriting"
    } else {
        Write-Step "写入 database.properties（root/$script:MysqlPassword）/ writing $DbProps"
    }
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $DbProps) | Out-Null
    $lines = @(
        '# 由 scripts\windows\deploy.bat 生成的部署配置 / generated by scripts\windows\deploy.bat'
        '# 凭据唯一维护在此文件 / credentials are maintained only in this file'
        'database.driver = com.mysql.cj.jdbc.Driver'
        "database.url = $url"
        'database.user = root'
        "database.password = $script:MysqlPassword"
        'database.maxconnections = 20'
        'database.hikari.maxLifetime = 600000'
        'database.hikari.connectionTestQuery = SELECT 1'
    )
    # UTF-8 无 BOM：java.util.Properties 会把 BOM 当成首个键的一部分 / UTF-8 without a BOM.
    [System.IO.File]::WriteAllLines($DbProps, $lines, (New-Object System.Text.UTF8Encoding($false)))
    Write-Ok 'database.properties 已写入 / written'
}

# 直接回车时的默认 IP：单机部署即用 127.0.0.1。
# Default IP when ENTER is pressed: deploy for local play on 127.0.0.1.
$DefaultIp = '127.0.0.1'

# 判断字符串是否为合法 IPv4 地址（四段 0-255）。
# Returns whether the string is a valid IPv4 address (four octets, 0-255).
function Test-IPv4([string]$Value) {
    if ($Value -notmatch '^\d{1,3}(\.\d{1,3}){3}$') { return $false }
    foreach ($part in $Value.Split('.')) {
        if ([int]$part -gt 255) { return $false }
    }
    return $true
}

# 交互设置服务器连接 IP（IPv4；直接回车使用 127.0.0.1 本机部署），覆盖写入 network.properties。
# Prompts for the server IP (IPv4; ENTER uses 127.0.0.1 for local play) and overwrites network.properties.
# 每次部署只问一次：输入什么用什么，回车即用默认值。
# Asked exactly once per deployment: the typed value is used as-is, ENTER keeps the default.
function Configure-NetworkAddress {
    if (-not (Test-Path $NetworkProps)) { return }

    Write-Host ''
    Write-Host '================ 服务器连接 IP 设置 / Server Connection IP ================' -ForegroundColor Cyan
    Write-Host "请输入服务器 IP，局域网部署例如 192.168.1.x；本机单机部署直接回车使用 $DefaultIp。"
    Write-Host "Enter the server IP (LAN e.g. 192.168.1.x), or press ENTER for local play on $DefaultIp."
    Write-Host '提示 / hint: 在 Windows 上运行 ipconfig 可查看本机局域网 IP'
    Write-Host '-------------------------------------------------------------------------'

    $targetIp = ''
    while (-not $targetIp) {
        $entered = "$(Read-Host "请输入服务器 IP（直接回车默认 $DefaultIp）/ Enter the server IP [default: $DefaultIp]")".Trim()
        if (-not $entered) { $targetIp = $DefaultIp; break }
        if (-not (Test-IPv4 $entered)) {
            Write-Note '不是合法的 IPv4 地址，请重新输入 / not a valid IPv4 address, please retry'
            continue
        }
        if ($entered -eq '0.0.0.0') {
            Write-Note '0.0.0.0 不可用，请输入具体 IP / 0.0.0.0 is not usable, enter a concrete IP'
            continue
        }
        $targetIp = $entered
    }

    $lines = Get-Content $NetworkProps
    $newLines = @()
    foreach ($line in $lines) {
        if ($line -match '^\s*gameserver\.network\.address\s*=') {
            $newLines += "gameserver.network.address = $targetIp"
        } elseif ($line -match '^\s*chatserver\.network\.public\.address\s*=') {
            $newLines += "chatserver.network.public.address = $targetIp`:10241"
        } else {
            $newLines += $line
        }
    }
    [System.IO.File]::WriteAllLines($NetworkProps, $newLines, (New-Object System.Text.UTF8Encoding($false)))
    Write-Ok "服务器连接 IP 已设置为 / Server IP set: $targetIp (Chat: $targetIp`:10241)"
}

# 交互选择服务器国家代码（默认国服 CN: 5），并同步更新 gameserver.properties。
# Interactively selects the server country code (default CN: 5) and updates gameserver.properties.
function Configure-CountryCode {
    if (-not (Test-Path $GameProps)) { return }

    # 读取当前配置中的国家代码作为参考展示
    $currentCode = '5'
    foreach ($line in (Get-Content $GameProps)) {
        if ($line -match '^\s*gameserver\.country\.code\s*=\s*(\d+)\s*$') {
            $currentCode = $Matches[1]
            break
        }
    }

    Write-Host ''
    Write-Host '================ 服务器国家代码设置 / Server Country Code ================' -ForegroundColor Cyan
    Write-Host "当前配置代码 / Current Country Code: $currentCode"
    Write-Host '  [5] 盛大 / 国服 CN（默认，直接回车）/ SDO / China (default, press ENTER)'
    Write-Host '  [1] 北美 NA / North America'
    Write-Host '  [2] 欧洲 EU / Europe'
    Write-Host '  [4] 日本 JP / Japan'
    Write-Host '  [7] 俄罗斯 RU / Russia'
    Write-Host '  [0] 韩国 KR / Korea'
    Write-Host '  [99] 自动识别 AUTO / Auto-detect'
    Write-Host '-------------------------------------------------------------------------'
    $choice = Read-Host '请输入国家代码 (直接回车默认 [5] 国服 CN) / Enter country code [default: 5]'
    $choice = "$choice".Trim().ToLower()

    $targetCode = '5'
    if ($choice -eq '' -or $choice -eq '5' -or $choice -eq 'cn' -or $choice -eq 'sdo') {
        $targetCode = '5'
    } elseif ($choice -eq '1' -or $choice -eq 'na') {
        $targetCode = '1'
    } elseif ($choice -eq '2' -or $choice -eq 'eu') {
        $targetCode = '2'
    } elseif ($choice -eq '4' -or $choice -eq 'jp') {
        $targetCode = '4'
    } elseif ($choice -eq '7' -or $choice -eq 'ru') {
        $targetCode = '7'
    } elseif ($choice -eq '0' -or $choice -eq 'kr') {
        $targetCode = '0'
    } elseif ($choice -eq '99' -or $choice -eq 'auto') {
        $targetCode = '99'
    } elseif ($choice -match '^\d+$') {
        $targetCode = $choice
    }

    $lines = Get-Content $GameProps
    $newLines = @()
    foreach ($line in $lines) {
        if ($line -match '^\s*gameserver\.country\.code\s*=') {
            $newLines += "gameserver.country.code = $targetCode"
        } else {
            $newLines += $line
        }
    }
    [System.IO.File]::WriteAllLines($GameProps, $newLines, (New-Object System.Text.UTF8Encoding($false)))
    Write-Ok "服务器国家代码已配置为 / Server country code set: $targetCode"
}

# ---- 服务端 / Server --------------------------------------------------------
function Start-Server {
    $existingPid = Get-RecordedPid $PidFile
    if (Test-Process $existingPid) {
        Write-Note "AionEmu 游戏服务端已在运行 (pid $existingPid)，无需重复启动 / already running (pid $existingPid), not starting again"
        return
    }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

    $javaArgs = @(
        '-Xms2g', '-Xmx8g',
        '-XX:+UseG1GC', '-XX:MaxGCPauseMillis=200', '-XX:InitiatingHeapOccupancyPercent=30',
        '-XX:+ParallelRefProcEnabled', '-XX:+UseStringDeduplication',
        '-XX:+HeapDumpOnOutOfMemoryError', "-XX:HeapDumpPath=`"$LogDir`"", '-XX:+ExitOnOutOfMemoryError',
        '-Dfile.encoding=UTF-8', '-Djava.net.preferIPv4Stack=true', '-Duser.timezone=Asia/Shanghai',
        '-Duser.language=zh', '-Duser.country=CN',
        "-Daion.home=`"$AionHome`"", "-Daion.log.dir=`"$LogDir`"",
        '-jar', "`"$JarPath`""
    )
    Write-Step '启动 AionEmu（前台，实时日志）/ starting AionEmu in the foreground'
    # 前台运行：日志实时打印到当前窗口（root logger 的 out_console 走 stdout）。
    # 记录 pid 以便 Ctrl+C 后兜底强杀；等待 java 结束，Ctrl+C 或关窗即停止。
    # Foreground: logs stream to this window (the root logger's out_console writes to stdout).
    # The pid is recorded for a post-Ctrl+C fallback kill; waits for java, stopped by Ctrl+C or window close.
    $process = Start-Process -FilePath $JavaExe -ArgumentList $javaArgs -WorkingDirectory $AionHome `
        -NoNewWindow -PassThru
    Set-Content -Path $PidFile -Value $process.Id -Encoding ASCII
    Write-Host '        游戏端口 / game  : 7777'
    Write-Host '        登录端口 / login : 2106'
    Write-Host '        聊天端口 / chat  : 10241'
    Write-Host "        日志 / log       : $LogDir\aionemu.log"
    $process.WaitForExit()
    # Ctrl+C 可能只到了 powershell 而未传给子进程，这里兜底强杀残留的 java。
    # Ctrl+C may reach PowerShell but not the child; force-kill any leftover java here.
    if (-not $process.HasExited) { Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    if ($process.ExitCode -ne 0) {
        Fail "AionEmu 异常退出（退出码 $($process.ExitCode)），请检查日志 / AionEmu exited with error code $($process.ExitCode)"
    }
    Write-Note "AionEmu 已退出 / AionEmu exited"
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
    Invoke-Native -FilePath 'powershell.exe' -Arguments @(
        '-NoProfile', '-ExecutionPolicy', 'Bypass', '-EncodedCommand', $encoded)
    return ($global:LASTEXITCODE -eq 0)
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

function Get-MysqlProcesses {
    Get-Process -Name 'mysqld' -ErrorAction SilentlyContinue |
        Where-Object { $_.Path -and $_.Path.StartsWith($MysqlHome, [StringComparison]::OrdinalIgnoreCase) }
}

function Stop-Mysql {
    $id = Get-RecordedPid $MysqlPidFile
    $targets = @()
    if (Test-Process $id) { $targets += $id }
    # 记录 pid 失效时按 mysqld 路径兜底，避免漏停导致残留 / fall back to the mysqld path when the recorded pid is stale.
    foreach ($proc in (Get-MysqlProcesses)) { if ($targets -notcontains $proc.Id) { $targets += $proc.Id } }
    if ($targets.Count -eq 0) {
        Write-Ok '便携 MySQL 未在运行 / portable MySQL not running'
        Remove-Item $MysqlPidFile -Force -ErrorAction SilentlyContinue
        return
    }
    Write-Step "停止便携 MySQL (pid $($targets -join ', ')) / stopping portable MySQL"
    $oldPwd = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $script:MysqlPassword
        Invoke-Native -FilePath $MysqlAdmin -Arguments @(
            '--no-defaults', '--protocol=TCP', '--get-server-public-key',
            '-u', 'root', '-h', '127.0.0.1', '-P', "$script:MysqlPort", "--password=$script:MysqlPassword", 'shutdown')
    } finally {
        if ($null -ne $oldPwd) { $env:MYSQL_PWD = $oldPwd }
        else { Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue }
    }
    if ($global:LASTEXITCODE -ne 0) {
        Write-Note 'mysqladmin shutdown 失败（凭据不符？），改按 pid 终止 / mysqladmin shutdown failed, killing by pid'
    } else {
        for ($i = 0; $i -lt 30; $i++) {
            Start-Sleep -Seconds 1
            if (-not (Get-MysqlProcesses)) { break }
        }
    }
    # 温和 → 强杀，逐个处理 / graceful then force, one by one.
    foreach ($procId in $targets) {
        if (Test-Process $procId) { Stop-Process -Id $procId -ErrorAction SilentlyContinue }
    }
    Start-Sleep -Seconds 1
    foreach ($procId in $targets) {
        if (Test-Process $procId) { Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue }
    }
    Start-Sleep -Seconds 1
    if (Get-MysqlProcesses) {
        Fail '无法停止便携 MySQL / failed to stop the portable MySQL'
    }
    Remove-Item $MysqlPidFile -Force -ErrorAction SilentlyContinue
    Write-Ok '便携 MySQL 已停止 / portable MySQL stopped'
}

# ---- 动作分派 / Action dispatch --------------------------------------------
function Initialize-Runtime {
    # 先读配置（若有），使端口/库名/密码与运行时保持一致，再决定是否需要初始化数据目录。
    # Read the config first so port/db/password match the runtime before deciding on initialization.
    Read-DbSettings
    # 打印脚本实际使用的凭据，便于与应用侧日志对照（排障用）。
    # Print the credentials the script actually uses, to compare against the app-side log.
    Write-Host "[AionEmu] DB 凭据 / DB credentials: user=root password=$script:MysqlPassword db=$script:DbName port=$script:MysqlPort"
    Initialize-MysqlData
    Ensure-MysqlRunning
    Write-DbProperties
}

switch ($Action) {
    'deploy' {
        Write-Host '=== AionEmu Windows 一键部署 / one-click deployment ===' -ForegroundColor Cyan
        Write-Host "部署目录 / deploy dir: $AionHome"
        Write-Host "脚本版本 / script version: $ScriptVersion"
        if ((Test-Process (Get-RecordedPid $PidFile)) -and (Test-PortOpen $script:MysqlPort)) {
            Write-Note '游戏服务端与便携 MySQL 都已在运行，无需重复部署 / both the game server and MySQL are already running; nothing to do'
            exit 0
        }
        Configure-NetworkAddress
        Configure-CountryCode
        Install-Jre
        Install-Mysql
        Initialize-Runtime
        # 前台运行游戏服务端；游戏退出（或失败）后，finally 块必定自动停止便携 MySQL，绝不残留。
        # Run the server in the foreground; when the server exits (or fails), finally stops MySQL cleanly.
        try {
            Start-Server
        } finally {
            Read-DbSettings
            Stop-Mysql
        }
    }
    'start' {
        # 仅启动 jar；MySQL 由 start-mysql.bat 负责，与 start-silent.sh 的职责一致。
        # Jar only; MySQL is start-mysql.bat's job, matching start-silent.sh's responsibility.
        if (-not (Test-Path $JavaExe)) { Fail '未安装 JRE，请先运行 deploy.bat / run deploy.bat first.' }
        if (Test-Process (Get-RecordedPid $PidFile)) {
            Write-Note 'AionEmu 游戏服务端已在运行，无需重复启动 / the game server is already running; nothing to do'
            exit 0
        }
        if (-not (Test-PortOpen $script:MysqlPort)) {
            Write-Note "便携 MySQL 未在运行；请先运行 start-mysql.bat（或 deploy.bat）/ portable MySQL is not running; run start-mysql.bat (or deploy.bat) first"
            exit 0
        }
        Start-Server
    }
    'stop' {
        Stop-Server
    }
    'start-mysql' {
        if (-not (Test-Path $MysqlExe)) { Fail '未安装 MySQL，请先运行 deploy.bat / run deploy.bat first.' }
        if (Test-PortOpen $script:MysqlPort) {
            Write-Note '便携 MySQL 已在运行，无需重复启动 / the portable MySQL is already running; nothing to do'
            exit 0
        }
        Initialize-Runtime
        Write-Host ''
        Write-Host "便携 MySQL 运行中 (port $script:MysqlPort)" -ForegroundColor Green
        Write-Host "按 Ctrl+C 或回车键 (ENTER) 停止便携 MySQL / press Ctrl+C or ENTER to stop portable MySQL" -ForegroundColor Cyan
        $canReadKeys = $false
        try {
            # 将 Ctrl+C 作为普通控制台输入读取，避免被操作系统信号直接强杀而跳过 finally / treat Ctrl+C as input so finally runs.
            [Console]::TreatControlCAsInput = $true
            while ([Console]::KeyAvailable) { [void][Console]::ReadKey($true) }
            $canReadKeys = $true
        } catch { }

        try {
            if ($canReadKeys) {
                while ($true) {
                    Start-Sleep -Milliseconds 200
                    $mysqlPid = Get-RecordedPid $MysqlPidFile
                    if ($mysqlPid -and -not (Test-Process $mysqlPid)) { break }
                    $shouldStop = $false
                    while ([Console]::KeyAvailable) {
                        $key = [Console]::ReadKey($true)
                        $isCtrlC = ($key.Key -eq [ConsoleKey]::C -and ($key.Modifiers -band [ConsoleModifiers]::Control)) -or ($key.KeyChar -eq [char]3)
                        if ($isCtrlC -or $key.Key -eq [ConsoleKey]::Enter -or $key.Key -eq [ConsoleKey]::Escape -or $key.Key -eq [ConsoleKey]::Q) {
                            $shouldStop = $true
                            break
                        }
                    }
                    if ($shouldStop) { break }
                }
            } else {
                [void][Console]::ReadLine()
            }
        } finally {
            Read-DbSettings
            Stop-Mysql
            if ($canReadKeys) {
                try { [Console]::TreatControlCAsInput = $false } catch { }
            }
        }
    }
    'stop-mysql' {
        Read-DbSettings
        Stop-Mysql
    }
    'status' {
        $serverPid = Get-RecordedPid $PidFile
        if (Test-Process $serverPid) { Write-Host "AionEmu 运行中 / running (pid $serverPid)" -ForegroundColor Green }
        else { Write-Host 'AionEmu 未运行 / not running' -ForegroundColor Yellow }
        if (Test-Process (Get-RecordedPid $MysqlPidFile)) { Write-Host '便携 MySQL 运行中 / portable MySQL running' -ForegroundColor Green }
        else { Write-Host '便携 MySQL 未运行 / portable MySQL not running' -ForegroundColor Yellow }
    }
}
