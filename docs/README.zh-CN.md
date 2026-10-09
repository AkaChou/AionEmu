<div align="center">

# AionEmu

### 高还原的 Aion 5.8 社区服务端
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

**AionEmu** 是专为 **Aion 5.8** 设计的现代化、单体架构高性能社区服务端。通过单个 Spring Boot 容器统一整合登录（Login）、游戏（Game）与聊天（Chat）核心服务，拥抱 Java 25 语言特性与原始游戏机制深度对齐，具备生产级高并发稳定性与整洁的工程架构。

---

## ⭐ 核心特性

| 特性 | 说明 | 状态 |
| --- | --- | :---: |
| 📜 **数据驱动任务引擎** | 原生表格驱动执行引擎（`tablelane`），替代 1,500+ 个分散 Java 类，全量支持多阶段对话阶梯、收集、击杀与过场动画。 | ✅ 原始深度对齐 |
| 🧠 **AI 与三维寻路引擎** | 原始 `.path/.idx` 多层 A* 三维寻路；深度优化感知、仇恨管理、巡逻护送与复杂地形移动。 | ✅ 深度优化 |
| 🐾 **客户端拟真随从跟随** | 严格对齐原始客户端随从跟随模型，彻底根除服务端多余寻路开销与频繁收放/瞬移刷屏。 | ✅ 客户端仿真 |
| ⚔️ **PvP 控制效果递减链** | 完整移植原始麻痹、睡眠、恐惧等异常状态衰减链，具备动态抗性增益与分级衰减窗口。 | ✅ 机制对齐 |
| 🎁 **可缩放掉落与经济系统** | 动态掉落计算、等级衰减、队伍掷骰竞价、自动拾取与尸体生命周期管理。 | ✅ 功能完整 |
| 🛠️ **运维与 GM 管理套件** | 支持批量刷怪（`//spawn <id>*<count>`）、持久化与非持久化刷怪分离（`//spawn` vs `//spawns`）及自动化脚本。 | ✅ 开箱即用 |

---

## 🗺️ 后续路线与规划

| 演进方向 | 目标与说明 | 状态 |
| --- | --- | :---: |
| 📜 **任务系统数据驱动重构** | 全面废除 1,500+ 硬编码 Java 任务脚本，实现统一原生数据驱动引擎，完整对齐 5.8 原始任务全链路。 | ✅ 已完成 |
| 🏰 **副本系统与机制深度对齐** | 深度对齐 5.8 原始副本机制、解耦 Boss 阶段行为脚本，构建通用数据驱动的副本流转引擎。 | 🚧 进行中 |
| ⚡ **Go 语言重构与微服务化** | 核心组件逐步采用 Go 进行重构：实现高并发连接网关、登录与聊天独立服务化及超低延迟封包路由。 | 📝 Todo |
| 📦 **静态数据流式与 JSONL 迁移** | 沉淀静态数据存储格式，推进大体积 XML 数据集向流式轻量 JSONL / 二进制格式迁移，降低内存常驻开销。 | 📝 Todo |

---

## ⚡ 快速开始

### 环境依赖
- **JDK 25+** • **Maven 3.7+** • **MySQL 8.0+** • **Aion 5.8 客户端**

### 1. 数据库初始化
数据库脚本位于：
- **登录服 (Login)**：`src/main/resources/db/mysql/al_server_ls.sql`
- **游戏服 (Game)**：`src/main/resources/db/mysql/al_server_gs.sql`

### 2. 构建与运行
```bash
./scripts/package.sh         # 打包并部署到 aion/（后续更新配置可用 ./scripts/re-package.sh）
./aion/start-silent.sh       # 后台静默启动（日志：tail -f aion/log/aionemu.log）
./aion/shutdown.sh           # 优雅关闭（或 ./aion/stop-silent.sh 快速停止）
```

---

## 🎮 客户端补丁

`patch/` 目录提供配套 Aion 5.8 客户端的兼容与体验补丁（含 `bin64/` 运行库、`L10N/` 任务汉化、`Levels/` 地形与 `Textures/` UI 图标）。
- **使用方法**：请先**备份原客户端同名文件**，然后将 `patch/` 下的子目录对应覆盖至客户端根目录即可。

---

## 💡 如何贡献

欢迎提交 PR 与 Issue！
1. **代码规范**：核心接口附中英双语注释，坚持数据驱动设计，不引入硬编码特例。
2. **测试验证**：提交前确保本地测试通过（`mvn test`）。
3. **提交 PR**：简要说明修改内容、原版对齐依据及测试证据。

---

## 🤝 致谢与许可

- 基于 **Aion 5.8 Community Emulator** 与早期开源社区贡献；架构与协议参考 [Beyond Aion](https://github.com/beyond-aion/aion-server)（GPL-3.0）。
- 本项目遵循 [GNU General Public License v3.0](../LICENSE) 开源协议。

<div align="center">

**AionEmu** • 打造极致纯净、真实的 Aion 5.8 模拟器体验。

</div>
