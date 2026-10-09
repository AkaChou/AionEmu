# AGENTS.md

本文档为在此代码库中工作的 AI 编码智能体（AI Coding Agents）提供项目级开发与协作指引。

> 💡 **详细规则已拆分至 `.agents/rules/` 目录下的专用文件中。** 请根据当前任务从 [规则索引](#rules-index) 中查阅适用的规则文件。

---

## 目录 (Table of Contents)

- [全局规则 (Global Rules)](#全局规则-global-rules)
- [规则索引 (Rules Index)](#rules-index)
- [记忆库机制 (Memory Bank)](#记忆库机制-memory-bank)
- [项目概览与技术栈 (Project Overview & Tech Stack)](#项目概览与技术栈-project-overview--tech-stack)
- [配置说明与外部数据 (Configuration & External Data)](#配置说明与外部数据-configuration--external-data)
- [源码结构 (Source Structure)](#源码结构-source-structure)
- [Maven 模块与打包部署 (Maven Module & Packaging)](#maven-模块与打包部署-maven-module--packaging)

---

## 全局规则 (Global Rules)

以下红线与纪律在所有任务中均具备最高优先级，必须严格遵守：

1. **⛔ 构建与测试授权红线**：
   - **严禁在未经用户明确要求的情况下运行构建命令（如 `mvn compile`、`mvn test`、`mvn package` 等）**。
   - 当验证流程确实需要运行测试或构建时，必须先向用户请求授权，并明确说明具体的命令、执行范围与原因；
   - 在未获授权前，保持当前工作状态为 `PENDING`，并在回复中明确记录未执行的命令。
2. **⛔ 服务端进程生命周期红线**：
   - **严禁自行启动、停止或重启服务端进程**。服务端实时状态未知，其生命周期完全由用户自行管理。
3. **📁 目录使用与隔离纪律**：
   - `.agents/summary/` 是存放所有任务总结、排查留痕与沉淀文档的**唯一权威目录**。
   - **严禁使用、创建或引用 `.agent/` 或 `.agent/summary/`**（注意区分单复数，全库仅认 `.agents/`）。
   - AI 生成的所有中间产物（包括临时脚本、分析工具、报告等）必须存放在 `.agents/summary/<topic>/` 下；**严禁将临时产物放置于 `scripts/` 或生产源码目录**。
4. **🌿 Worktree 使用纪律 (Worktree Discipline)**：
   - 除非主工作树无法用于必要验证（例如并行任务导致生产目录无法构建），否则**严禁创建 git worktree**。
   - 经允许创建时，必须置于仓库外部的临时路径下，且仅用于单次只读/验证；
   - **严禁在 worktree 中提交代码**；
   - 验证完成后必须立即使用 `git worktree remove --force <path>` 后接 `git worktree prune` 彻底清理，绝不可残留 worktree、构建产物（`target/`）或陈旧检出。
5. **📝 提交与暂存授权 (Commit Authorization)**：
   - 仅在获得用户明确授权后方可执行 `git commit`。
   - 获权后，显式暂存当前任务生成的总结、记忆库、Playbook 及验收文档，并与相关的源码变更一同提交；
   - **严禁使用 `git add -A` 或全量暂存**；严禁暂存中间临时产物、被忽略的运行时输出或无关改动。

---

<a id="rules-index"></a>
## 规则索引 (Rules Index)

针对特定代码领域和工作流的详细规约已模块化，位于 `.agents/rules/` 目录下：

| 规则文件 | 适用范围 | 核心契约与关注点 |
|---|---|---|
| [i18n.md](.agents/rules/i18n.md) | 全仓库 | **中英双语注释**（类与核心方法 Javadoc 必须双语同义）、**本地化日志**（必须走 `I18n.get(...)`，双语 properties 键对齐）、术语权威对齐 |
| [java_general.md](.agents/rules/java_general.md) | `**/*.java` | 串行执行 Java 命令、不可变设计、Java 25 实用特性、及早失败、**严禁吞异常/必须保留 cause**、**强制构造器注入/禁止字段注入** |
| [backend.md](.agents/rules/backend.md) | `src/main/java/**/*.java` | **JDK 25 语言基线**（优先 record/sealed/模式匹配）、`ScopedValue` 线程边界约束、**严禁随意开启预览特性**、保持既有架构兼容 |
| [formatting.md](.agents/rules/formatting.md) | `**/*.java`, `**/*.xml` | **Java**：Tab 制表符（宽 4 空格）、K&R 花括号、单行上限 120 字符；<br>**XML**：2 空格缩进、严格保持 Schema 元素与任务流转顺序 |
| [lombok.md](.agents/rules/lombok.md) | `**/*.java` | 优先类级注解、**严禁滥用 `@Data`（严格对照 5 项排查问卷）**、防范方法重载冲突与哈希/集合查找破坏 |
| [ai-artifacts.md](.agents/rules/ai-artifacts.md) | 全仓库 | 中间产物集中于 `.agents/summary/<topic>/`、任务完成后自动清理、临时 Worktree 规范、诊断转储（JFR/dump）不入仓 |
| [quest-repair.md](.agents/rules/quest-repair.md) | 任务 XML、引擎、AI 及测试 | **以全部类似任务角度根本解决**、**严禁任务引擎/编译器引入硬编码特例**、Playbook 模式指纹比对、待验收/双 Commit 工作流 |
| Hindsight MCP | 全仓库 | 持久化架构模式、避坑指南、排查路由器、结构化知识检索与维护（Bank ID: `AionEmu`） |

---

## 记忆库机制 (Memory Bank via Hindsight MCP)

代码库的持久化架构模式、调试排查经验与历史任务总结已全量迁移并统一托管在 **Hindsight MCP**（Bank ID: `AionEmu`）：

- **操作前检索 (Recall Before Action)**：
  - 诊断缺陷或修改核心系统前，优先通过 Hindsight MCP `recall` 检索已知架构陷阱、排查模式及历史任务证据；
  - 查阅重点领域或跨领域不变量时，可通过 query 检索 `mb_active_context` 或 `mb_system_patterns`。
- **模式检索与定位 (Pattern Retrieval)**：
  - 所有 222 个架构模式均已按 `pattern_<ID>`（如 `pattern_AIM-001`、`pattern_QE-045`）持久化至 Hindsight；
  - 支持直接通过症状、根因、修复规则及日志关键字进行语义召回与精确查看。
- **自动沉淀协议 (Automatic Wrap-up Protocol)**：
  1. 解决非平凡 Bug、运行时异常或细微架构问题后，将关键现象、根因链与防错规则提炼为事实；
  2. 使用 Hindsight MCP 的 `sync_retain` 或 `retain` 接口沉淀至知识库（附带相关标签如 `['pattern', <domain>, <ID>]` 或 `['summary', <topic>]`）；
  3. 当本轮会话实际更新了记忆库内容时，在回复末尾附上 `[Hindsight Memory Bank Updated]` 作为回执。

---

## 项目概览与技术栈 (Project Overview & Tech Stack)

### 项目概览
AionEmu 是一个 Aion 5.8 社区服务端项目。采用单体 Spring Boot 应用程序承载登录、游戏和聊天三大核心服务，并统一加载任务系统、NPC、地图、地形（geodata）、副本等静态游戏数据。

### 技术栈基线
- **核心环境**：Java 25, Spring Boot 4.1, Apache Maven
- **网络与并发**：Netty, Quartz 调度器
- **持久化与数据**：MySQL Connector/J, Jakarta XML Binding (JAXB)
- **代码增强与工具**：Lombok, SLF4J / Logback
- **测试框架**：JUnit Jupiter (JUnit 5)

---

## 配置说明与外部数据 (Configuration & External Data)

### 核心配置文件
- **Spring Boot 引导入口**：`src/main/resources/application.yml`（非 Web 模式运行，默认启用 login、game、chat 服务，基于 Netty 传输）。
- **运行时配置目录**：`src/main/resources/aion/config/`，划分为登录（login）、网络（network）、主服（main）、聊天（chat）、管理（administration）、调度（schedule）等领域。
- **数据库配置**：
  - 登录服数据库：`src/main/resources/aion/config/login/database.properties`
  - 游戏服数据库：`src/main/resources/aion/config/network/database.properties`

### 默认网络端口速查
定义于 `src/main/resources/aion/config/network/network.properties`：

| 服务与通道 | 默认端口 | 说明 |
|---|---|---|
| 登录客户端连接 (Login Client) | `2106` | 外部客户端登录入口 |
| 游戏客户端连接 (Game Client) | `7777` | 外部客户端游戏主入口 |
| 聊天客户端连接 (Chat Client) | `10241` | 外部客户端聊天连接 |
| 游戏服 -> 登录服内部通信 | `9014` | 内部跨进程协同通道 |
| 游戏服 -> 聊天服内部通信 | `9021` | 内部跨进程协同通道 |

### 外部数据根路径引用规范
外部数据根（真端服务端表、客户端安装目录、解包资源等）因开发者机器环境而异，在代码、脚本与文档中**一律仅通过逻辑名称指代，严禁硬编码绝对路径或强制环境变量**。详见 [ENVIRONMENT.md](ENVIRONMENT.md)：

| 逻辑名称 | 指向说明 |
|---|---|
| `<仓库根>` | 本项目代码仓库根目录 |
| `<真端根>` | 真端 5.8 服务端根：`Map/XML/Quest_*.xml`、`Map/Worlds/`、反编译源码等 |
| `<客户端目录>` | Aion 5.8 客户端安装根：`L10N/CHS/Data/data.pak`、`data/Quest/Quest.pak` 等 |
| `<客户端解包根>` | 客户端解包产物根：`Quest_unpacked/quest.xml`、`data_unpacked/Dialogs/**` 等 |

---

## 源码结构 (Source Structure)

项目采用清晰的领域模块化目录组织：

```text
src/main/java/com/aionemu/
├── boot/           # Spring Boot 生命周期、统一配置、国际化基建及传输层边界
├── commons/        # 数据库持久层、底层网络库、并发工具与共享基础设施
├── loginserver/    # 账号认证、服务器列表、登录业务服务
├── chatserver/     # 聊天通道、群组与社交消息服务
└── gameserver/     # 核心游戏世界、任务引擎、NPC/怪物 AI、网络协议与各子系统业务

src/main/resources/aion/
├── config/         # 运行时属性配置 (.properties)
├── data/           # 生产静态数据、NPC 模板、掉落表与任务 XML
├── definitions/    # 紧凑定义与代码生成模板输入
└── geo/            # 地形几何、寻路网格（Path）与 Geo 静态数据

src/test/java/      # 单元测试、任务编译器验证、生产目录与白名单审计测试
docs/               # 系统设计、任务修复 Playbook 与维护参考文档
scripts/            # 项目级运行时维护脚本、打包工具、数据生成与审计工具
aion/               # 本地部署/运行目标目录（非源码，不纳入版本控制）
```

---

## Maven 模块与打包部署 (Maven Module & Packaging)

- **单模块结构**：仓库根目录为唯一的 Maven 模块（`com.aionemu:aionemu`）。所有 Maven 命令均应在仓库根目录下执行。
- **打包产物**：Spring Boot 打包生成的独立可运行 JAR 为 `target/AionEmu.jar`。
- **部署脚本**：运行 `scripts/package.sh` 可将编译打包后的 JAR、配置文件及生命周期脚本统一部署至 `aion/` 目录（或由环境变量 `AION_HOME` 指定的目录）。
