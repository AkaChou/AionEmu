# AionEmu

[English](README.md)

Aion 5.8 社区服务端。单 Maven 工程，JDK 25，Spring Boot 启动 login / game / chat。

## 目录

- [核心特性](#核心特性)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [客户端设置](#客户端设置)
- [配置说明](#配置说明)
- [运行脚本](#运行脚本)
- [环境变量](#环境变量)
- [网络端口](#网络端口)
- [项目结构](#项目结构)
- [文档](#文档)
- [开发](#开发)
- [常见问题排查](#常见问题排查)
- [致谢](#致谢)
- [许可证](#许可证)

## 核心特性

| 领域 | 能力 |
| --- | --- |
| 运行架构 | 内嵌 login、game、chat 服务，共享生命周期与 Netty 传输层。 |
| 游戏数据 | 任务、NPC、出生点、技能、物品、副本、活动、房屋和世界定义均由数据驱动。 |
| 掉落系统 | 深度优化 NPC、任务、活动和全局掉落计算；可缩放的基纳奖励与等级衰减；队伍分配、掷骰/竞价、自动拾取、宠物/小弟拾取、稀有掉落公告和尸体生命周期管理。 |
| AI 与移动 | 深度优化感知、仇恨、技能选择、巡逻、逃跑、护送、拥挤避让、地形感知移动和有界卡住恢复。 |
| Geo 与寻路 | 提供地形高度、材质、可见性和碰撞查询；使用原始 PATH `.path/.idx` 多层 A*；长距离 block/portal 分层搜索；保留 PATH Z 的路径平滑与最终 GEO 校验；支持飞行和游泳三维寻路。 |
| 运维能力 | `aion.home` 外置运行目录，配置与日志分离，支持打包 JAR/脚本以及管理和诊断命令。 |

## 技术栈

| 组件 | 选型 |
| --- | --- |
| 语言 | Java 25 |
| 框架 | Spring Boot 4.1（非 Web 应用，Netty 传输层） |
| 构建 | Maven，单模块 |
| 数据库 | MySQL |
| 网络 | Netty |
| 调度 | Quartz |
| XML | Jakarta XML Binding / JAXB |
| 测试 | JUnit Jupiter |

## 快速开始

### 环境要求

- JDK 25 或更高版本
- Maven 3.7 或更高版本
- 用于 login 和 game 持久化的 MySQL
- 用于连接服务端的 Aion 5.8 客户端

### 1. 初始化数据库

SQL 文件会创建所需的数据库和表：

```bash
mysql -u root -p < src/main/resources/db/mysql/al_server_ls.sql
mysql -u root -p < src/main/resources/db/mysql/al_server_gs.sql
```

### 2. 打包服务端

```bash
./package.sh
```

该命令生成 `target/AionEmu.jar`，并将 JAR、资源和生命周期脚本部署到 `aion/`（也可以通过 `AION_HOME` 指定其他目录）。默认构建会跳过测试；测试与重新打包选项请参考[开发](#开发)。

### 3. 配置运行环境

首次打包后编辑部署目录中的配置：

```text
aion/config/login/database.properties
aion/config/network/database.properties
aion/config/network/network.properties
```

启动前请设置数据库凭据，以及对客户端公布的 game/chat 地址。后续构建如需保留现有运行配置，请使用 `./re-package.sh`。

### 4. 启动与停止

```bash
./aion/start-silent.sh
tail -f aion/log/aionemu.log
./aion/shutdown.sh       # 优雅关闭
./aion/stop-silent.sh
```

使用 `AION_HOME=/path/to/runtime` 可将服务部署或运行在其他目录。可通过 `AION_HEAP_OPTS` 及其他 `AION_*_OPTS` 环境变量覆盖 JVM 参数。

```bash
AION_HOME=/path/to/runtime ./aion/start-silent.sh
```

如需在启动前清理运行数据（保留 JAR 和脚本）：

```bash
./aion/start-silent.sh -c
```

## 客户端设置

可选的 Aion 5.8 客户端补丁（任务本地化与 VIP `Game.dll`）请参阅[客户端补丁说明](patch/patch_documentation.md)。

## 配置说明

`aion/config/` 下的主要运行配置文件：

| 文件 | 用途 |
| --- | --- |
| `login/database.properties` | login 数据库（`al_server_ls`）连接配置 |
| `network/database.properties` | game 数据库（`al_server_gs`）连接配置 |
| `network/network.properties` | 客户端端口、对外地址、服务间内网地址和服务密码 |
| `main/*.properties` | 玩法、AI、掉落、倍率、副本及其他服务端行为 |
| `administration/*.properties` | 管理员/GM 命令、面板和限制 |
| `schedule/*.xml` | 定时活动、副本、攻城和世界事件 |

服务开关在 `src/main/resources/application.yml` 中配置（已打包进 JAR）：login、game、chat 三个服务是否启动，以及 Netty 传输模式。

## 运行脚本

| 脚本 | 说明 |
| --- | --- |
| `start-silent.sh` | 后台启动服务端，写入 PID 和日志文件 |
| `start-silent.sh -c` | 启动前清理运行数据（保留 JAR 和脚本） |
| `shutdown.sh` | 请求优雅关闭，最多等待 `AION_SHUTDOWN_TIMEOUT` 秒 |
| `stop-silent.sh` | 停止服务端，超过 `AION_STOP_TIMEOUT` 秒后可选强制结束 |

## 环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `AION_HOME` | `aion/` 或脚本所在目录 | JAR、配置、数据和日志的运行目录 |
| `AION_JAR_FILE` | `$AION_HOME/AionEmu.jar` | 服务端 JAR 路径 |
| `AION_LOG_DIR` | `$AION_HOME/log` | 日志目录 |
| `AION_LOG_FILE` | `$AION_LOG_DIR/aionemu.log` | 主日志文件 |
| `AION_PID_FILE` | `$AION_LOG_DIR/aionemu.pid` | PID 文件 |
| `AION_HEAP_OPTS` | `-Xms2g -Xmx8g` | JVM 堆内存设置 |
| `AION_GC_OPTS` | G1GC 和有界暂停目标 | JVM 垃圾回收设置 |
| `AION_SAFETY_OPTS` | 堆转储和 OOM 退出 | JVM 崩溃安全设置 |
| `AION_SYSTEM_OPTS` | UTF-8、IPv4、Asia/Shanghai 时区 | JVM 系统属性 |
| `AION_PATH_OPTS` | `-Daion.home=$AION_HOME -Daion.log.dir=$AION_LOG_DIR` | 指向运行目录和日志目录的路径系统属性 |
| `AION_JVM_OPTS` | 由上述 `AION_*_OPTS` 组合 | 完整 JVM 参数 |
| `AION_SHUTDOWN_TIMEOUT` | `120` | 优雅关闭超时（秒） |
| `AION_STOP_TIMEOUT` | `30` | 停止超时（秒），超时后可选强制结束 |
| `AION_FORCE_STOP` | `true` | 超过 `AION_STOP_TIMEOUT` 后是否强制结束 |
| `AION_PRESERVE_CONFIG` | `false` | 执行 `package.sh` 时，为 `true` 则保留现有运行配置 |
| `MAVEN_THREADS` | `1C` | `package.sh` 和 `re-package.sh` 使用的 Maven 并行线程数（设为 `1` 可关闭 Reactor 并行） |

## 网络端口

| 服务 | 默认端口 | 用途 |
| --- | ---: | --- |
| Login 客户端 | `2106` | 客户端登录 |
| Game 客户端 | `7777` | 世界/游戏连接 |
| Chat 客户端 | `10241` | 聊天连接 |
| Game -> Login | `9014` | 服务间连接 |
| Game -> Chat | `9021` | 服务间连接 |

端口和对外公布地址配置在 [`network.properties`](src/main/resources/aion/config/network/network.properties)。

## 项目结构

| 路径 | 用途 |
| --- | --- |
| `src/main/java/com/aionemu/` | Boot、公共基础设施、login、game 和 chat 源码 |
| `src/main/resources/application.yml` | Spring Boot 入口配置 |
| `src/main/resources/aion/data/` | 版本化静态游戏数据、任务、定义和数据包 |
| `src/main/resources/aion/definitions/` | 精简数据定义、Schema 和生成输入 |
| `src/main/resources/aion/geo/` | 地形、碰撞和原始 PATH 资源 |
| `src/main/resources/aion/config/` | 版本库中的默认配置 |
| `src/main/resources/db/mysql/` | login 和 game 数据库结构 |
| `aion/` | JAR、运行配置和日志的本地部署目录 |
| `scripts/` | 数据生成、审计、运行辅助和维护工具 |
| `docs/` | 任务、寻路、术语和维护文档 |
| `patch/` | Aion 5.8 客户端补丁和使用说明 |

## 文档

- [A* PATH 寻路方案](docs/PATH_ASTAR_REFACTOR_PLAN.md)
- [任务目录](docs/QUEST_CATALOG.zh-CN.md)
- [任务编写指南](docs/quest/WRITING_GUIDE.zh-CN.md) / [English](docs/quest/WRITING_GUIDE.md)
- [任务排查与修复 Playbook](docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md)
- [任务客户端对话框映射](docs/quest/client-dialog-mapping/README.zh-CN.md)
- [游戏术语中英对照](docs/aion-game-terms-en-zh.md)
- [客户端补丁说明](patch/patch_documentation.md)

## 开发

在仓库根目录运行测试：

```bash
mvn test
```

构建可运行 JAR：

```bash
mvn package
```

也可以使用打包脚本一步完成构建和部署：

```bash
./package.sh                              # 默认：clean + 跳过测试 + package + 部署，使用 1C Maven 线程
MAVEN_THREADS=2C ./package.sh             # 每个可用 CPU 核心使用两个 Maven 线程
./package.sh -DskipTests=false package    # 打包时同时运行测试
./re-package.sh                           # 部署时保留现有运行配置
```

应用入口为 `com.aionemu.AionBootApplication`。

## 常见问题排查

| 现象 | 常见处理 |
| --- | --- |
| `Missing target/AionEmu.jar` | 先运行 `./package.sh`。 |
| `AionEmu is already running` | 使用 `./aion/shutdown.sh` 或 `./aion/stop-silent.sh`；检查 `aion/log/aionemu.pid`。 |
| 数据库连接失败 | 确认 MySQL 已启动，检查 `aion/config/login/database.properties` 和 `aion/config/network/database.properties` 中的凭据，并确认已导入两个 SQL 文件。 |
| 客户端无法连接 | 检查 `aion/config/network/network.properties` 中的对外地址/端口以及防火墙规则。 |
| 构建后运行配置被覆盖 | 使用 `./re-package.sh`，或在执行 `./package.sh` 时设置 `AION_PRESERVE_CONFIG=true`。 |
| 需要干净运行环境 | 执行 `./aion/start-silent.sh -c`（保留 JAR 和脚本）。 |

## 致谢

基于 Aion 5.8 Community Emulator 及更早社区工作。

感谢 Aion-Lightning、Encom、[Beyond Aion](https://github.com/beyond-aion/aion-server)，以及历代 Aion 服务端社区的众多贡献者。

## 许可证

本项目使用 [GPL-3.0](LICENSE) 许可证。
