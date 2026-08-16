# AionEmu

[English](README.md)

Aion 5.8 社区服务端。单 Maven 工程，JDK 25，Spring Boot 启动 login / game / chat。

## 核心特性

| 领域 | 能力 |
| --- | --- |
| 运行架构 | 内嵌 login、game、chat 服务，共享生命周期与 Netty 传输层。 |
| 游戏数据 | 任务、NPC、出生点、技能、物品、副本、活动、房屋和世界定义均由数据驱动。 |
| 掉落系统 | 深度优化 NPC、任务、活动和全局掉落计算；可缩放的基纳奖励与等级衰减；队伍分配、掷骰/竞价、自动拾取、宠物/小弟拾取、稀有掉落公告和尸体生命周期管理。 |
| AI 与移动 | 深度优化感知、仇恨、技能选择、巡逻、逃跑、护送、拥挤避让、地形感知移动和有界卡住恢复。 |
| Geo 与寻路 | 提供地形高度、材质、可见性和碰撞查询；使用原始 PATH `.path/.idx` 多层 A*；长距离 block/portal 分层搜索；保留 PATH Z 的路径平滑与最终 GEO 校验；支持飞行和游泳三维寻路。 |
| 运维能力 | `aion.home` 外置运行目录，配置与日志分离，支持打包 JAR/脚本以及管理和诊断命令。 |

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

该命令生成 `target/AionEmu.jar`，并将 JAR、资源和生命周期脚本部署到 `aion/`（也可以通过 `AION_HOME` 指定其他目录）。

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
| `src/main/resources/aion/data/` | 版本化静态游戏数据、任务、定义和数据包 |
| `src/main/resources/aion/geo/` | 地形、碰撞和原始 PATH 资源 |
| `src/main/resources/aion/config/` | 版本库中的默认配置 |
| `src/main/resources/db/mysql/` | login 和 game 数据库结构 |
| `aion/` | JAR、运行配置和日志的本地部署目录 |
| `docs/` | 任务、寻路、术语和维护文档 |
| `patch/` | Aion 5.8 客户端补丁和使用说明 |

## 文档

- [A* PATH 寻路方案](docs/PATH_ASTAR_REFACTOR_PLAN.md)
- [任务目录](docs/QUEST_CATALOG.zh-CN.md)
- [任务编写指南](docs/quest/WRITING_GUIDE.zh-CN.md) / [English](docs/quest/WRITING_GUIDE.md)
- [任务排查与修复 Playbook](docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md)
- [客户端补丁说明](patch/patch_documentation.md)

## 开发

在仓库根目录运行测试：

```bash
mvn test
```

应用入口为 `com.aionemu.AionBootApplication`。

## 致谢

基于 Aion 5.8 Community Emulator 及更早社区工作。

感谢 Aion-Lightning、Encom、[Beyond Aion](https://github.com/beyond-aion/aion-server)，以及历代 Aion 服务端社区的众多贡献者。

## 许可证

本项目使用 [GPL-3.0](LICENSE) 许可证。
