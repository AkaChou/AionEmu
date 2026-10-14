# Windows 一键部署 | Windows Deployment

[中文](#中文) | [English](#english)

---

## 中文

面向 Windows 的部署与运行脚本，**只依赖系统自带组件**：Windows PowerShell 5.1、`curl.exe` 与 `tar.exe`
（Windows 10 1803+ / Server 2019+ 自带），无需预装 JDK、MySQL 或 Git。

### 一键部署

在仓库根目录执行 `bash scripts/package.sh` 打包（产物落在 `aion/`，本目录脚本会一并复制到 `aion\windows\`），
然后在 Windows 上双击 `aion\windows\deploy.bat`。脚本会自动完成：

1. 下载并解压 **JRE 25**（Adoptium 官方源优先，失败回退清华 TUNA 镜像）到 `aion\runtime\jre`；
2. 下载并解压**便携 MySQL 8.4**（官方 CDN）到 `aion\runtime\mysql`；
3. 初始化数据目录 `aion\runtime\mysql-data`、以 `--initialize-insecure` 起步，启动 MySQL（127.0.0.1:3306）；
4. 配置 `root` 密码并创建 `aion_emu` 库（utf8mb4）；
5. 写入 `aion\config\network\database.properties`（已存在则保留不动）；
6. 启动服务端，等待 Flyway 首次自动建表。

启动后端口：游戏 `7777`、登录 `2106`、聊天 `10241`。运行日志：`aion\log\aionemu.log`。

### 常用命令

| 命令 | 作用 |
|---|---|
| `deploy.bat` | 一键部署并启动（可重复执行，已装组件会跳过） |
| `start.bat` | 启动便携 MySQL + 服务端 |
| `stop.bat` | 优雅停止服务端（先发 Ctrl+C，超时 120s 后强制终止） |
| `stop.bat -All` | 同时停止便携 MySQL |
| `powershell -File server.ps1 status` | 查看服务端 / MySQL 运行状态 |

### 目录与配置

- `aion\runtime\jre`、`aion\runtime\mysql`：自动下载的运行时，可删除后重新部署；
- `aion\runtime\mysql-data`：**数据库数据目录，请勿删除**；
- `aion\config\network\database.properties`：数据库连接与凭据的唯一维护点，默认 `root` / `123456`，
  首次部署后建议修改（同时删除 `aion\runtime\mysql-data` 会重置数据库）。

### 注意

- 便携 MySQL 依赖 **Microsoft Visual C++ 2015-2022 运行库**，缺失时 `mysqld` 初始化/启动会失败，请先安装；
- MySQL 的 `winx64.zip` 仅官方 CDN 可达，国内网络下载可能较慢（脚本对官方源做了重试）；
- 服务端标准输出进入其自身的隐藏控制台窗口，请以 `aion\log\aionemu.log` 为准。

---

## English

Windows deployment and run scripts that depend **only on bundled components**: Windows PowerShell 5.1,
`curl.exe` and `tar.exe` (shipped with Windows 10 1803+ / Server 2019+). No preinstalled JDK, MySQL or Git.

### One-click deploy

Run `bash scripts/package.sh` at the repo root (the output lands in `aion/`, and this directory's scripts are
copied to `aion\windows\`), then double-click `aion\windows\deploy.bat` on Windows. It will:

1. download and extract **JRE 25** (Adoptium official first, falling back to the TUNA mirror) into `aion\runtime\jre`;
2. download and extract a **portable MySQL 8.4** (official CDN) into `aion\runtime\mysql`;
3. initialize the data dir `aion\runtime\mysql-data` with `--initialize-insecure` and start MySQL on 127.0.0.1:3306;
4. configure the `root` password and create the `aion_emu` database (utf8mb4);
5. write `aion\config\network\database.properties` (kept untouched if it already exists);
6. start the server; Flyway builds the tables on first start.

Ports after startup: game `7777`, login `2106`, chat `10241`. Log: `aion\log\aionemu.log`.

### Commands

| Command | Purpose |
|---|---|
| `deploy.bat` | Deploy and start (re-runnable; already installed parts are skipped) |
| `start.bat` | Start the portable MySQL and the server |
| `stop.bat` | Graceful stop (Ctrl+C first, force-killed after a 120s timeout) |
| `stop.bat -All` | Also stop the portable MySQL |
| `powershell -File server.ps1 status` | Show server / MySQL status |

### Notes

- The portable MySQL needs the **Microsoft Visual C++ 2015-2022 runtime**; without it `mysqld` fails to
  initialize/start.
- The MySQL `winx64.zip` is only reachable on the official CDN, so downloads may be slow in some regions.
- The server's stdout goes to its own hidden console window; rely on `aion\log\aionemu.log`.
