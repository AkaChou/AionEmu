<div align="center">

<img src="logo.webp" alt="AionEmu Logo" width="640" style="max-width: 100%; height: auto;" />

# AionEmu

### 高度还原的 Aion 5.8 社区服务端模拟器
#### 单体 Maven 工程 • Java 25 • Spring Boot 4.1 • Netty

[![Java 25](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-Single--Module-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Protocol](https://img.shields.io/badge/Aion-5.8%20Protocol-darkgreen)](https://github.com)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](../LICENSE)

[English](../README.md) • [简体中文](README.zh-CN.md)

<br/>

[快速开始](#快速开始) • [核心特性](#核心特性) • [客户端补丁](#客户端补丁) • [参与贡献](#参与贡献) • [团队](#团队)

</div>

---

## 项目介绍

**AionEmu** 是专为 **Aion 5.8** 设计的现代化、单体架构高性能社区服务端模拟器。

---

## 核心特性

| 特性 | 说明 |
| :--- | :--- |
| **单体统一架构** | 单个 Spring Boot 4.1 进程管理登录、游戏与聊天服务，服务间零网络延迟与开销，支持按需独立开关。 |
| **数据驱动任务引擎 (`tablelane`)** | 原生表格驱动执行架构，替代 1,500+ 个硬编码任务类；100% 还原原版对话阶梯、收集、计数、过场动画与奖励。 |
| **真实 3D 网格寻路与 Geo 引擎** | 原始 `.path/.idx` 多层 3D A* 网格寻路，支持视线遮挡（LOS）、复杂地形高程物理与障碍物规避。 |
| **战斗与攻击计算系统** | 精确还原物理/魔法攻击力算法、双持独立结算、命中/暴击/闪避/格挡/盾牌吸收及伤害浮动判定。 |
| **技能与异常状态递减链** | 完整覆盖 5.8 原版技能体系、连续技/烙印链条、Buff/Debuff 优先级冲突互斥，以及 PvP 控制状态分级衰减窗口。 |
| **层级化 AI 状态机** | 基于 AI2 体系的怪物与 NPC 行为树、群体仇恨协作、巡逻脱战重置，以及阶段性 Boss 行为逻辑（持续深化完善中）。 |
| **世界对象与场景交互** | 深度支持多页面 NPC 对话树、采集物/机关物件触发交互、传送门网络，以及高精度飞行/滑翔状态同步。 |
| **动态掉落与经济体系** | 动态掉落几率计算、等级衰减曲线、组队掷骰竞价、自动拾取与尸体生命周期管理。 |
| **露娜系统 (Luna System)** | 完整支持 5.8 露娜商店、露娜制作（工坊）、露娜骰子乐、每日/每周露娜副本活动，以及消费里程碑奖励。 |
| **客户端拟真随从跟随** | 严格还原 5.8 原版随从跟随模型，由客户端平滑演算位移，彻底消除服务端寻路负载与广播刷屏。 |

---

## 快速开始

### 环境依赖
- **JDK 25+**
- **MySQL 8.0+**
- **Aion 5.8 客户端**（推荐 64 位）

### 1. 配置数据库
数据库连接在 `/config/network/network.properties` 中进行配置：

```properties
# 数据库连接配置位于 /config/network/network.properties
database.url = jdbc:mysql://127.0.0.1:3306/aion_emu?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC
database.user = root
database.password = 你的密码
```

只需在 MySQL 中创建空白数据库 `aion_emu`，服务端首次启动时将自动完成表结构初始化。

> **从旧双库（`al_server_gs` / `al_server_ls`）升级**：先启动一次服务端完成建表，停服后运行 `scripts/sql/migrate-to-aion-emu.sql`。详见 [数据库迁移指引](../scripts/sql/README.md)。

### 2. 启动与管理服务端
直接使用脚本管理服务端运行：

- **Linux / macOS**（使用 `scripts/`）：
  ```bash
  ./scripts/start-silent.sh     # 后台静默启动服务端
  tail -f aion/log/aionemu.log  # 查看实时运行日志
  ./scripts/shutdown.sh         # 优雅关服保存数据（或 ./scripts/stop-silent.sh 快速停止）
  ```

- **Windows**（使用 `scripts/windows/`）：
  ```cmd
  scripts\windows\deploy.bat    # Windows 部署并启动服务端
  ```

---

## 参与贡献

开发者或需要从源码二次开发与构建的用户请按以下步骤操作：

### 1. 克隆代码与配置
```bash
git clone https://github.com/AionEmu/AionEmu.git
cd AionEmu
```
在 `src/main/resources/aion/config/network/network.properties` 中配置开发数据库信息。

### 2. 编译与打包
```bash
./scripts/package.sh          # 全量构建并输出部署产物至 aion/
# 日常调试若需保留 aion/config/ 中已修改配置，可使用 ./scripts/re-package.sh
```

### 3. 测试与开发准则
- 提交 PR 前请确保全量测试通过：`mvn test`
- 核心领域逻辑与对外接口请保留中英双语注释。
- 坚持数据驱动设计，不引入硬编码特例。

---

## 团队

主要成员：

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/AkaChou">
        <img src="https://github.com/AkaChou.png?size=160" width="120" height="120" alt="AkaChou" />
      </a>
      <br />
      <a href="https://github.com/AkaChou">AkaChou</a>
    </td>
    <td align="center">
      <a href="https://github.com/sunbsn">
        <img src="https://github.com/sunbsn.png?size=160" width="120" height="120" alt="sunbsn" />
      </a>
      <br />
      <a href="https://github.com/sunbsn">Sunbsn</a>
    </td>
  </tr>
</table>

---

## 客户端补丁

`patch/` 目录提供配套 Aion 5.8 客户端的兼容与体验补丁：

- **`bin64/`** — 核心运行库（`Game.dll`），提供协议支持与兼容运行。
- **`bin64.7z`** — 客户端运行库压缩包，需先解压再覆盖 `bin64/Game.dll`（详见 [`patch/README.md`](../patch/README.md)）。
- **`L10N/CHS/`** — 简体中文任务文本与界面汉化包（`data.pak`）。
- **`Levels/`** — 地图与地形数据修正（`Level.pak`）。
- **`Textures/ui/`** — 界面 UI 图标与纹理更新（`ui.pak`）。

> ⚠️ **提示**：覆盖补丁前请先备份客户端原始同名文件。详细说明见 [`patch/README.md`](../patch/README.md)。

---

## 致谢与开源许可

- 特别感谢 **Aion 5.8 社区**、早期开源贡献者、**Encom 团队**（Encom team），以及 [Beyond Aion](https://github.com/beyond-aion/aion-server) 项目。
- 本项目遵循 **[GNU General Public License v3.0](../LICENSE)** 开源协议。
