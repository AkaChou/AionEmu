# AGENTS.md

本文件为在本仓库中工作的 AI coding agent 提供项目入口说明。

> **详细规则拆分在 `.agent/rules/` 下。** 根据任务范围读取下方 [Rules Index](#rules-index) 中的适用规则。

## 全局规则

1. 未经用户明确要求，不运行 lint 或构建命令。
2. 不要启动或重启开发服务器；不假设服务器当前已启动，服务器由用户负责启动和管理。
3. 仓库存在 `.agent/summary/` 时，所有总结文件统一存放在该目录。

## 配置

- Spring Boot 入口配置：`src/main/resources/application.yml`，应用以非 Web 模式启动 login、game 和 chat 服务，默认使用 Netty transport。
- 服务端运行配置：`src/main/resources/aion/config/`，按 login、network、main、chat、administration 和 schedule 等领域拆分。
- 默认网络端口定义在 `src/main/resources/aion/config/network/network.properties`：登录客户端 2106、游戏客户端 7777、登录服与游戏服内部通信 9014。
- 数据库配置位于 `src/main/resources/aion/config/login/database.properties` 和 `src/main/resources/aion/config/network/database.properties`。

## 项目概览

AionEmu 是 Aion 5.8 社区服务端。单个 Spring Boot 应用统一承载 login、game 和 chat 服务，并加载任务、NPC、地图、地形、实例及其他静态游戏数据。

## 技术栈

- Java 25、Spring Boot 4.1、Maven。
- Netty、MySQL Connector/J、Quartz、Jakarta XML Binding。
- Lombok、SLF4J/Logback、JUnit Jupiter。

## 源码结构

- `src/main/java/com/aionemu/boot/`：Spring Boot 生命周期、配置、国际化和传输边界。
- `src/main/java/com/aionemu/commons/`：数据库、网络、并发和通用基础设施。
- `src/main/java/com/aionemu/loginserver/`：登录服务。
- `src/main/java/com/aionemu/chatserver/`：聊天服务。
- `src/main/java/com/aionemu/gameserver/`：游戏世界、任务、AI、协议和业务服务。
- `src/main/resources/aion/config/`：运行配置。
- `src/main/resources/aion/data/`：正式静态数据与任务 XML。
- `src/main/resources/aion/definitions/`：紧凑定义及生成输入。
- `src/main/resources/aion/geo/`：Geo、Path 和地形数据。
- `src/test/java/`：单元测试、生产 catalog 和回归测试。
- `docs/`：设计、任务修复和维护文档。
- `scripts/`：数据生成、审计、运行辅助和维护工具。
- `aion/`：本地部署目录，不作为源码或长期构建输入。

## Maven 模块

- 仓库根目录是唯一 Maven 模块：`com.aionemu:aionemu`。
- Spring Boot 重打包产物为 `target/AionEmu.jar`。
- `package.sh` 将 JAR、资源和启停脚本部署到 `aion/` 或 `AION_HOME` 指定目录。
- 所有 Maven 命令都应从仓库根目录执行。

## Rules Index

所有详细规则位于 `.agent/rules/`：

| 文件 | 适用范围 | 说明 |
|---|---|---|
| [i18n.md](.agent/rules/i18n.md) | 全仓库 | 中英双语注释、日志国际化和术语规范 |
| [java_general.md](.agent/rules/java_general.md) | `**/*.java` | Java 通用约定、错误处理、依赖注入和开发命令约束 |
| [lombok.md](.agent/rules/lombok.md) | `**/*.java` | Lombok 样板代码简化及生成行为边界 |
| [quest-repair.md](.agent/rules/quest-repair.md) | 任务 XML、任务引擎、任务 AI、任务测试和 `docs/quest/` | 任务证据、修复、验收和 playbook 更新规则 |
