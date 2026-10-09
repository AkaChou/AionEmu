<div align="center">

# AionEmu

### High-performance, Authentic Aion 5.8 Community Server
#### Single Maven project • Java 25 • Spring Boot 4.1

[![Java 25](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-Single--Module-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Protocol](https://img.shields.io/badge/Aion-5.8%20Emu-darkgreen)](https://github.com)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)

[English](README.md) | [中文](docs/README.zh-CN.md)

</div>

---

## 📖 Introduction

**AionEmu** is a modern, modular, and high-performance server emulator for **Aion 5.8**, designed for production stability, maintainability, and authentic game mechanics.

By unifying Login, Game, and Chat services into a single Spring Boot application, AionEmu combines cutting-edge Java 25 capabilities with deep original server/client behavioral parity.

**Core Highlights:**
- 📜 **Data-Driven Quests** — Replaces fragmented Java handlers with a unified native data-driven runtime (`tablelane`).
- 🎯 **Authentic Behavior Parity** — Rigorously aligned dialogue ladders, state advancement axes, reward projections, and drop mechanics.
- 🧠 **Modern AI & Movement** — Terrain-aware navigation, crowd avoidance, and client-side simulated minion following without server lag.
- ⚔️ **Authentic Combat & CC** — Complete original PvP crowd-control decay chains (paralyze, sleep, fear resistance scaling).
- 🛠️ **Developer & Ops Friendly** — Externalized runtime (`aion.home`), zero-touch config preservation, and streamlined GM diagnostic toolsets.

---

## ⭐ Core Features

| Feature | Description | Parity & Status |
| --- | --- | :---: |
| 📜 [**Data-Driven Quest Engine**](docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md) | Native table-driven execution engine replacing 1,500+ hardcoded Java files; full coverage of multi-stage dialogue, hunt, collect, and movie sequences. | ✅ Authentic Parity |
| 🧠 **AI & Pathfinding Engine** | Original `.path/.idx` multi-layer A* 3D navigation; optimized aggro, patrol, escort, and obstacle handling. | ✅ Enhanced |
| 🐾 **Client-Simulated Minions** | Fully authentic minion follow model simulated on the client side, eliminating server pathfinding overhead and spammy teleport broadcasts. | ✅ Client Simulation |
| ⚔️ **PvP CC Decay Chains** | Port of original abnormal status diminishing returns with dynamic resistance increase and tiered time windows. | ✅ Authentic Parity |
| 🎁 **Scalable Drops & Economy** | Dynamic drop calculations, level-based reduction, group roll/bid, auto-loot, and corpse lifecycle management. | ✅ Complete |
| 🛠️ **Operations & GM Tooling** | Batch spawning (`//spawn <id>*<count>`), persistent/non-persistent spawn separation (`//spawn` vs `//spawns`), and automated packaging. | ✅ Ready |

---

## 🗺️ Roadmap

| Milestone & Direction | Scope & Objectives | Status |
| --- | --- | :---: |
| 📜 **Data-Driven Quest Migration** | Full migration of 1,500+ hardcoded Java quest handlers to a unified native table-driven runtime aligned with authentic 5.8. | [x] |
| 🏰 **Instance Engine & Original Mechanics** | Deep alignment of 5.8 original instance workflows, boss phase AI decoupling, and data-driven instance script orchestration. | [ ] |
| ⚡ **Go Transformation & Microservices** | Incremental re-architecture with Go: high-concurrency connection gateway, decoupled login/chat standalone services, and low-latency packet routing. | [ ] |
| 📦 **Static Data Modernization** | Migrating voluminous XML static datasets into streaming, memory-mapped compact JSONL / binary formats to minimize startup footprint. | [ ] |

---

## ⚡ Quick Start

### Prerequisites

- **JDK 25** or newer
- **Maven 3.7** or newer
- **MySQL 8.0+**
- **Aion 5.8 Client**

### 1. Database Initialization

Database schemas and initial data scripts are located at:
- **Login Server**: `src/main/resources/db/mysql/al_server_ls.sql`
- **Game Server**: `src/main/resources/db/mysql/al_server_gs.sql`

### 2. Build & Deploy

```bash
# Package server and deploy to local aion/ runtime directory
./scripts/package.sh
```

> **Tip**: Use `./scripts/re-package.sh` for subsequent builds to automatically preserve existing configurations under `aion/config/`.

### 3. Start & Monitor

```bash
# Start background server
./aion/start-silent.sh

# Follow live log
tail -f aion/log/aionemu.log

# Graceful shutdown
./aion/shutdown.sh
# or immediate stop
./aion/stop-silent.sh
```

---

## 🎮 Client Patch Setup

The `patch/` directory contains client-side patches and enhancements for the Aion 5.8 client:

- **`bin64/`** — Modified core runtime library (`Game.dll`) for client compatibility.
- **`L10N/CHS/`** — Localized quest and interface text data (`data.pak`).
- **`Levels/`** — World and level terrain fixes (`Level.pak`).
- **`Textures/ui/`** — UI icons and visual texture updates (`ui.pak`).

### How to Install

> ⚠️ **Warning**: Always back up the corresponding files and directories in your Aion 5.8 client folder before applying any patches!

1. Locate your **Aion 5.8 Client** root installation directory.
2. Back up the original `bin64/`, `L10N/`, `Levels/`, and `Textures/` folders.
3. Copy all folders inside `patch/` (`bin64`, `L10N`, `Levels`, `Textures`) and paste them directly into your client root directory, overwriting existing files when prompted.

---

## 🛠️ Operations & Development

### Common Commands

| Command | Purpose |
| --- | --- |
| `./scripts/package.sh` | Clean, compile, package, and deploy fresh runtime to `aion/` |
| `./scripts/re-package.sh` | Rebuild and deploy while preserving customized `aion/config/` |
| `AION_HOME=/path/to/dir ./aion/start-silent.sh` | Run using an externalized runtime directory |
| `./aion/start-silent.sh -c` | Clean transient runtime data while preserving JARs and configs |
| `mvn test` | Run project test suites |

### Key Project Paths

- **Entry Point**: `src/main/java/com/aionemu/AionBootApplication.java`
- **Application Config**: `src/main/resources/application.yml`
- **Game Static Data**: `src/main/resources/aion/data/`
- **Runtime Deployment**: `aion/` (`aion.home`)

---

## 💡 Contributing

We welcome community contributions, bug reports, and authentic parity fixes! To contribute:

1. **Fork the Repository**: Create your feature branch (`git checkout -b feature/amazing-feature`).
2. **Follow Project Guidelines**:
   - Class and core method documentation must include **bilingual (EN/ZH)** Javadoc.
   - Adhere to data-driven design — avoid hardcoded exceptions in the engine.
   - Ensure clean code standards (immutability, Java 25 features, zero swallowing of exceptions).
3. **Verify & Test**: Run project unit and regression tests (`mvn test`) before submitting.
4. **Open a Pull Request**: Provide a clear description of your changes, reference any relevant issue, and include test verification evidence.

---

## 🤝 Acknowledgments & Credits

- Based on **Aion 5.8 Community Emulator** and earlier community work.
- Architecture and original protocol insights inspired by [Beyond Aion](https://github.com/beyond-aion/aion-server) (Aion 4.8, GPL-3.0).
- Special thanks to **Aion-Lightning**, **Encom**, and the open-source Aion emulation community.

---

## 📄 License

This project is licensed under the [GNU General Public License v3.0](LICENSE).

<div align="center">

**AionEmu** • Crafting the ultimate Aion 5.8 server experience.

</div>
