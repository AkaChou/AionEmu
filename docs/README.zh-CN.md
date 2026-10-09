<div align="center">

# AionEmu

### 高性能、高度还原原版的 Aion 5.8 社区服务端
#### 单 Maven 工程 • Java 25 • Spring Boot 4.1

[![Java 25](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-Single--Module-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Protocol](https://img.shields.io/badge/Aion-5.8%20Emu-darkgreen)](https://github.com)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](../LICENSE)

[English](../README.md) | [中文](README.zh-CN.md)

</div>

---

## 📖 项目介绍

**AionEmu** 是专为 **Aion 5.8** 设计的现代化、模块化、高性能社区服务端，致力于实现高并发稳定性、优雅的代码架构以及与原始版本高度一致的真实游戏体验。

项目将 Login（登录）、Game（游戏）与 Chat（聊天）三大服务统一集成在单个 Spring Boot 容器中，全面拥抱 Java 25 现代化语言特性与原始机制高度对齐。

**核心亮点：**
- 📜 **数据驱动任务体系** — 全面废弃海量硬编码 Java Handler，统一迁移至原生数据驱动运行时（`tablelane`）。
- 🎯 **深度原版行为对齐** — 严格对齐原始 5.8 客户端与服务端对话阶梯、任务状态推进轴、奖励投影与掉落计算。
- 🧠 **智能 AI 与拟真移动** — 具备地形感知与障碍避让的高性能寻路；随从（Minion）全面接入客户端拟真跟随模型。
- ⚔️ **原版战斗与控制衰减** — 完整移植原始“连续异常状态衰减链”（PvP 控制递减），实现阶梯抗性与衰减时间窗口。
- 🛠️ **开发者与运维友好** — 彻底分离源码与运行时（`aion.home`）、增量更新自动保留配置、便捷实用的 GM 诊断指令集。

---

## ⭐ 核心特性

| 特性 | 说明 | 对齐度 / 状态 |
| --- | --- | :---: |
| 📜 [**数据驱动任务引擎**](quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md) | 原生表格驱动执行引擎，替代 1,500+ 个分散 Java 任务类；完整支持多阶段对话阶梯、收集、击杀与过场动画。 | ✅ 原始深度对齐 |
| 🧠 **AI 与三维寻路引擎** | 原始 `.path/.idx` 多层 A* 三维寻路；深度优化感知、仇恨管理、巡逻护送与复杂地形移动。 | ✅ 深度优化 |
| 🐾 **客户端拟真随从跟随** | 严格对齐原始客户端随从跟随模型，彻底根除服务端多余寻路开销与频繁收放/瞬移刷屏。 | ✅ 客户端仿真 |
| ⚔️ **PvP 控制效果递减链** | 完整移植原始麻痹、睡眠、恐惧等异常状态衰减链，具备动态抗性增益与分级衰减窗口。 | ✅ 原版机制对齐 |
| 🎁 **可缩放掉落与经济系统** | 动态掉落计算、等级衰减衰退、队伍掷骰竞价、自动拾取与尸体生命周期管理。 | ✅ 功能完整 |
| 🛠️ **运维与 GM 管理套件** | 支持批量刷怪（`//spawn <id>*<count>`）、持久化与非持久化刷怪分离（`//spawn` vs `//spawns`）及自动化脚本。 | ✅ 开箱即用 |

---

## 🗺️ 后续路线与规划

| 演进方向 | 目标与说明 | 状态 |
| --- | --- | :---: |
| 📜 **任务系统数据驱动重构** | 全面废除 1,500+ 硬编码 Java 任务脚本，实现统一原生数据驱动引擎，完整对齐 5.8 原始任务全链路。 | [x] |
| 🏰 **副本系统与机制深度对齐** | 深度对齐 5.8 原始副本机制、解耦 Boss 阶段行为脚本，构建通用数据驱动的副本流转引擎。 | [ ] |
| ⚡ **Go 语言重构与微服务化** | 核心组件逐步采用 Go 进行重构：实现高并发连接网关、登录与聊天独立服务化及超低延迟封包路由。 | [ ] |
| 📦 **静态数据流式与 JSONL 迁移** | 沉淀静态数据存储格式，推进大体积 XML 数据集向流式轻量 JSONL / 二进制格式迁移，降低内存常驻开销。 | [ ] |

---

## ⚡ 快速开始

### 环境依赖

- **JDK 25** 或更高版本
- **Maven 3.7** 或更高版本
- **MySQL 8.0+**
- **Aion 5.8 客户端**

### 1. 数据库初始化脚本

数据库表结构与初始化数据 SQL 脚本位于：
- **登录服 (Login)**：`src/main/resources/db/mysql/al_server_ls.sql`
- **游戏服 (Game)**：`src/main/resources/db/mysql/al_server_gs.sql`

### 2. 编译打包与部署

```bash
# 编译打包并自动部署至本地 aion/ 运行目录
./scripts/package.sh
```

> **提示**：后续构建可使用 `./scripts/re-package.sh`，部署时会自动保留 `aion/config/` 中的已有配置。

### 3. 启动与管理

```bash
# 后台静默启动服务端
./aion/start-silent.sh

# 查看实时运行日志
tail -f aion/log/aionemu.log

# 优雅关闭服务端
./aion/shutdown.sh
# 或快速停止
./aion/stop-silent.sh
```

---

## 🎮 客户端补丁与安装

`patch/` 目录提供了配套 Aion 5.8 客户端的增强与兼容补丁：

- **`bin64/`** — 核心运行库补丁（`Game.dll`），提供客户端运行与兼容支持。
- **`L10N/CHS/`** — 简体中文任务文本与界面汉化包（`data.pak`）。
- **`Levels/`** — 地图与地形数据修正（`Level.pak`）。
- **`Textures/ui/`** — 界面 UI 图标与纹理更新（`ui.pak`）。

### 如何使用

> ⚠️ **重要提示**：在覆盖任何补丁前，请务必先备份客户端中的原始同名文件与目录！

1. 找到您的 **Aion 5.8 客户端** 安装根目录。
2. 备份客户端中原有的 `bin64/`、`L10N/`、`Levels/`、`Textures/` 等目录。
3. 按照 `patch/` 下的目录结构（`bin64`、`L10N`、`Levels`、`Textures`），直接复制并覆盖到对应的客户端安装根目录下即可。

---

## 🛠️ 运维与开发指南

### 常用命令速查

| 命令 | 用途 |
| --- | --- |
| `./scripts/package.sh` | 清理、编译、打包并全量部署至 `aion/` 运行目录 |
| `./scripts/re-package.sh` | 增量重新部署，自动保留已自定义的 `aion/config/` 配置 |
| `AION_HOME=/path/to/dir ./aion/start-silent.sh` | 指定外置运行目录启动服务 |
| `./aion/start-silent.sh -c` | 清理临时运行时数据（保留 JAR 与配置文件） |
| `mvn test` | 执行单元测试与门禁验证套件 |

### 关键工程路径

- **程序启动入口**：`src/main/java/com/aionemu/AionBootApplication.java`
- **Spring Boot 配置**：`src/main/resources/application.yml`
- **静态游戏数据**：`src/main/resources/aion/data/`
- **本地运行目录**：`aion/`（对应系统属性 `aion.home`）

---

## 💡 如何贡献

我们非常欢迎社区的贡献、缺陷反馈与原版行为对齐修复！参与流程如下：

1. **Fork 本仓库**：创建您的特性分支（`git checkout -b feature/amazing-feature`）。
2. **遵循项目规约**：
   - 类与核心接口必须附带**中英双语**同义注释。
   - 坚持数据驱动设计，严禁在核心引擎中引入硬编码特例。
   - 保持代码质量与及早失败设计（不可变模型、Java 25 特性、严禁吞异常）。
3. **验证与测试**：在提交前运行单元测试与门禁验证套件（`mvn test`）。
4. **提交 Pull Request**：详细说明改动动机、原始对齐依据与测试验收证据。

---

## 🤝 致谢与参考

- 基于 **Aion 5.8 Community Emulator** 及早期开源社区贡献成果。
- 架构设计与原始协议逻辑部分参考自 [Beyond Aion](https://github.com/beyond-aion/aion-server)（Aion 4.8，GPL-3.0）。
- 特别鸣谢 **Aion-Lightning**、**Encom** 以及历代致力于 Aion 模拟器研发的社区开发者。

---

## 📄 许可证

本项目遵循 [GNU General Public License v3.0](../LICENSE) 开源协议。

<div align="center">

**AionEmu** • 打造极致纯净、真实的 Aion 5.8 模拟器体验。

</div>
