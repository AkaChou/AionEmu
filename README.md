<div align="center">

<img src="docs/logo.webp" alt="AionEmu Logo" width="640" style="max-width: 100%; height: auto;" />

# AionEmu

### Authentic Aion 5.8 Community Server Emulator
#### Unified Maven Architecture • Java 25 • Spring Boot 4.1 • Netty

[![Java 25](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-Single--Module-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Protocol](https://img.shields.io/badge/Aion-5.8%20Protocol-darkgreen)](https://github.com)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)

[English](README.md) • [简体中文](docs/README.zh-CN.md)

<br/>

[Quick Start](#quick-start) • [Features](#features) • [Client Patch](#client-companion-patch) • [Contributing](#contributing)

</div>

---

## Overview

**AionEmu** is a modern, high-performance community server emulator for **Aion 5.8**.

---

## Features

| Feature | Description |
| :--- | :--- |
| **Unified Runtime** | Single Spring Boot 4.1 process managing Login, Game, and Chat services; zero inter-process network overhead with flexible per-service toggles. |
| **Data-Driven Quests (`tablelane`)** | Native table-driven execution engine replacing 1,500+ legacy hardcoded quest classes; 100% authentic dialogue ladders, movies, hunt/collect counters, and rewards. |
| **True 3D Navigation & Geo Engine** | Original `.path/.idx` multi-layer A* 3D pathfinding with line-of-sight (LOS) raycasting, true height physics, and obstacle avoidance. |
| **Combat & Attack Engine** | Faithful physical and magical damage formulas, dual-wield calculations, critical/hit/evasion/parry/block resolutions, and shield absorbs. |
| **Skill Engine & CC Decay** | Complete 5.8 original skill mechanics, combo/stigma chains, strict buff/debuff priority conflict trees, and PvP CC diminishing returns. |
| **Hierarchical AI System** | Modular AI2 state machines for monster and NPC behaviors, aggro coordination, patrol routines, and multi-phase Boss AI (continuously being refined). |
| **Interactive World & Dialogs** | Authentic multi-page NPC dialog trees, gatherable/mechanism object interactions, portal networks, and flight/gliding state synchronization. |
| **Drops & Economy Engine** | Dynamic drop calculations, level-based decay curves, party roll/bid distribution, auto-loot, and corpse lifecycle management. |
| **Luna System & Features** | Full support for 5.8 Luna Shop, Luna Crafting, Luna Dice, daily/weekly event instances, and consumption reward tiers. |
| **Client-Simulated Minions** | Fully authentic 5.8 minion follow model simulated on the client side, eliminating server tick overhead and broadcast spam. |

---

## Quick Start

### Prerequisites
- **JDK 25+**
- **MySQL 8.0+**
- **Aion 5.8 Client** (64-bit recommended)

### 1. Database Configuration
Database connection settings are configured in `/config/network/network.properties`:

```properties
# Database connection settings in /config/network/network.properties
database.url = jdbc:mysql://127.0.0.1:3306/aion_emu?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC
database.user = root
database.password = your_password
```

Create an empty database named `aion_emu`. The server automatically initializes all required tables on first launch.

> **Upgrading from legacy dual DBs (`al_server_gs` / `al_server_ls`)**: Start the server once to initialize tables, stop it, and execute `scripts/sql/migrate-to-aion-emu.sql`. See [Database Migration Guide](scripts/sql/README.md).

### 2. Start & Manage Server
Use the provided scripts to run and manage the server:

- **Linux / macOS** (via `scripts/`):
  ```bash
  ./scripts/start-silent.sh     # Start server in background
  tail -f aion/log/aionemu.log  # Monitor live startup logs
  ./scripts/shutdown.sh         # Graceful shutdown (or ./scripts/stop-silent.sh)
  ```

- **Windows** (via `scripts/windows/`):
  ```cmd
  scripts\windows\deploy.bat    # Deploy and start server on Windows
  ```

---

## Contributing

Developers and contributors building from source can follow these steps:

### 1. Clone & Configure
```bash
git clone https://github.com/AionEmu/AionEmu.git
cd AionEmu
```
Configure your database credentials in `src/main/resources/aion/config/network/network.properties`.

### 2. Build & Package
```bash
./scripts/package.sh          # Full build and deploy to aion/
# or ./scripts/re-package.sh to preserve existing runtime configuration in aion/config/
```

### 3. Test & Guidelines
- Run automated tests before submitting: `mvn test`
- Public APIs and core domain logic should include bilingual (EN/ZH) comments.
- Adhere to data-driven design; avoid hardcoding specific IDs or exceptions in Java code.

---

## Client Companion Patch

The `patch/` directory contains companion enhancements for the Aion 5.8 64-bit client:

- **`bin64/`** — Core runtime library (`Game.dll`) for protocol compatibility.
- **`bin64.7z`** — Client runtime archive; extract it first, then overwrite `bin64/Game.dll` (see [`patch/README.md`](patch/README.md)).
- **`L10N/CHS/`** — Simplified Chinese quest dialogue and UI localization (`data.pak`).
- **`Levels/`** — Map terrain and coordinate corrections (`Level.pak`).
- **`Textures/ui/`** — UI icons and interface assets (`ui.pak`).

> ⚠️ **Notice**: Always back up original client files before overwriting. See [`patch/README.md`](patch/README.md) for details.

---

## Acknowledgments & License

- Special thanks to the **Aion 5.8 Community**, early open-source contributors, the **Encom team**, and the [Beyond Aion](https://github.com/beyond-aion/aion-server) project.
- This project is licensed under the **[GNU General Public License v3.0](LICENSE)**.
