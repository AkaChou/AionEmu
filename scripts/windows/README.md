# Windows 一键部署 | Windows Deployment

[中文](#中文) | [English](#english)

---

## 中文

面向 Windows 的部署与运行脚本，**只依赖系统自带组件**：Windows PowerShell 5.1、`curl.exe` 与 `tar.exe`
（Windows 10 1803+ / Server 2019+ 自带），无需预装 JDK、MySQL 或 Git。

### 一键部署

在仓库根目录执行 `bash scripts/package.sh` 打包（产物落在 `aion/`，本目录脚本会一并复制到 `aion\windows\`），
然后在 Windows 上双击 `aion\windows\deploy.bat`。脚本会自动完成：

1. 提示交互输入**服务器对外 IP**（必须为局域网或公网 IPv4，本机 `localhost` / `127.0.0.1` 会被拒绝，
   自动写回 `aion\config\network\network.properties`）；
2. 下载并解压 **JRE 25**（清华 TUNA 镜像优先，失败回退 Adoptium 官方源）到 `aion\runtime\jre`；
3. 下载并解压**便携 MySQL**（华为云镜像 8.0.29 优先，失败回退官方 CDN 8.4.x）到 `aion\runtime\mysql`；
4. 初始化数据目录 `aion\runtime\mysql-data`、以 `--initialize-insecure` 起步，启动 MySQL（127.0.0.1:3306）；
5. 配置 `root` 密码并创建 `aion_emu` 库（utf8mb4）；
6. 写入 `aion\config\network\database.properties`（已存在则保留不动）；
7. 启动服务端，等待 Flyway 首次自动建表。

启动后端口：游戏 `7777`、登录 `2106`、聊天 `10241`。运行日志：`aion\log\aionemu.log`。

### 常用命令

| 命令 | 作用 |
|---|---|
| `deploy.bat` | 一键部署并**前台**启动（实时日志；Ctrl+C 或关窗**同时停止游戏与 MySQL**） |
| `start.bat` | **前台**启动服务端并**实时打印日志**（Ctrl+C 或关窗停止**游戏**） |
| `stop.bat` | 停止后台残留的服务端（一般用 Ctrl+C 即可） |
| `start-mysql.bat` | 仅**前台**启动便携 MySQL（Ctrl+C 或关窗停止 **MySQL**） |
| `stop-mysql.bat` | 仅停止便携 MySQL |
| `powershell -File server.ps1 status` | 查看服务端 / MySQL 运行状态 |

> 三个启动脚本都在**前台**运行：Ctrl+C 停止脚本并停止其负责的服务；`.bat` 结束后 `pause`，窗口不自动关闭。
> 若游戏或 MySQL 已在运行，脚本会**说明原因并退出**（避免重复启动）。

### 目录与配置

- `aion\runtime\jre`、`aion\runtime\mysql`：自动下载的运行时，可删除后重新部署；
- `aion\runtime\mysql-data`：**数据库数据目录，请勿删除**；
- `aion\runtime\mysql\my.ini`：便携 MySQL 启动配置（每次部署自动刷新），默认
  `bind-address=0.0.0.0` + `port=3306`，**允许远程客户端直接连接**；服务端自身仍走 `127.0.0.1`；
- `aion\config\network\database.properties`：数据库连接与凭据的唯一维护点，默认 `root` / `123456`，
  首次部署后建议修改（同时删除 `aion\runtime\mysql-data` 会重置数据库）。

### 远程访问数据库

便携 MySQL 已建好 `root@%` 账号并监听所有网卡，远程连接只需两步：

1. 在服务器放行防火墙 TCP `3306`（管理员 PowerShell）：
   ```powershell
   netsh advfirewall firewall add rule name="AionEmu MySQL" dir=in action=allow protocol=TCP localport=3306
   ```
2. 客户端用主机 IP 连接，例如：`mysql -h 192.168.1.18 -P 3306 -u root -p`

> 想改回仅本机可访问，编辑 `aion\runtime\mysql\my.ini` 把 `bind-address=0.0.0.0` 改成 `127.0.0.1`
> 后重启 MySQL（`stop-mysql.bat` 再 `start-mysql.bat`）。

### 注意

- 便携 MySQL 依赖 **Microsoft Visual C++ 2015-2022 运行库**，缺失时 `mysqld` 初始化/启动会失败，请先安装；
- MySQL 优先走华为云镜像（8.0.29，约 11MB/s），失败回退官方 CDN（8.4.x）。国内镜像不提供 8.4 的
  Windows zip；迁移脚本只用 `utf8mb4_general_ci` + InnoDB，8.0.29 与 8.4 均兼容；
- 服务端标准输出进入其自身的隐藏控制台窗口，请以 `aion\log\aionemu.log` 为准。

---

## English

Windows deployment and run scripts that depend **only on bundled components**: Windows PowerShell 5.1,
`curl.exe` and `tar.exe` (shipped with Windows 10 1803+ / Server 2019+). No preinstalled JDK, MySQL or Git.

### One-click deploy

Run `bash scripts/package.sh` at the repo root (the output lands in `aion/`, and this directory's scripts are
copied to `aion\windows\`), then double-click `aion\windows\deploy.bat` on Windows. It will:

1. interactively prompt for the **advertised server IP** (must be a LAN or public IPv4; `localhost` /
   `127.0.0.1` is rejected, automatically written to `aion\config\network\network.properties`);
2. download and extract **JRE 25** (TUNA mirror first, falling back to the official Adoptium source) into `aion\runtime\jre`;
3. download and extract a **portable MySQL** (Huawei Cloud mirror 8.0.29 first, falling back to the official CDN 8.4.x) into `aion\runtime\mysql`;
4. initialize the data dir `aion\runtime\mysql-data` with `--initialize-insecure` and start MySQL on 127.0.0.1:3306;
5. configure the `root` password and create the `aion_emu` database (utf8mb4);
6. write `aion\config\network\database.properties` (kept untouched if it already exists);
7. start the server; Flyway builds the tables on first start.

Ports after startup: game `7777`, login `2106`, chat `10241`. Log: `aion\log\aionemu.log`.

### Commands

| Command | Purpose |
|---|---|
| `deploy.bat` | Deploy and start in the **foreground** (live logs; Ctrl+C or close **stops both game and MySQL**) |
| `start.bat` | Start the server in the **foreground** with **live logs** (Ctrl+C or close stops the **game**) |
| `stop.bat` | Stop a leftover background server (usually Ctrl+C is enough) |
| `start-mysql.bat` | Start the portable MySQL in the **foreground** (Ctrl+C or close stops **MySQL**) |
| `stop-mysql.bat` | Stop the portable MySQL only |
| `powershell -File server.ps1 status` | Show server / MySQL status |

> The three launchers run in the **foreground**: Ctrl+C stops the script and its service; every `.bat`
> ends with `pause`, so the window never closes on its own. If the game or MySQL is already running, the
> script explains why and exits instead of starting a duplicate.

### Remote database access

The portable MySQL already creates a `root@%` account and listens on every interface, so remote access
needs two steps:

1. open TCP `3306` in the server firewall (admin PowerShell):
   ```powershell
   netsh advfirewall firewall add rule name="AionEmu MySQL" dir=in action=allow protocol=TCP localport=3306
   ```
2. connect from a client with the host IP, e.g. `mysql -h 192.168.1.18 -P 3306 -u root -p`

> To restrict it back to localhost, set `bind-address=127.0.0.1` in `aion\runtime\mysql\my.ini` and restart
> MySQL (`stop-mysql.bat` then `start-mysql.bat`).

### Notes

- The portable MySQL needs the **Microsoft Visual C++ 2015-2022 runtime**; without it `mysqld` fails to
  initialize/start.
- MySQL prefers the Huawei Cloud mirror (8.0.29, ~11MB/s) and falls back to the official CDN (8.4.x).
  China mirrors carry no 8.4 Windows zip; the migration script only uses `utf8mb4_general_ci` + InnoDB,
  so both 8.0.29 and 8.4 are compatible.
- The server's stdout goes to its own hidden console window; rely on `aion\log\aionemu.log`.
