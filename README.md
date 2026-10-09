<div align="center">

# AionEmu

### Authentic Aion 5.8 Community Server
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

**AionEmu** is a modern, modular, and high-performance server emulator for **Aion 5.8**. By unifying Login, Game, and Chat services into a single Spring Boot application, AionEmu combines cutting-edge Java 25 capabilities with deep original server/client behavioral parity, designed for production stability and clean architecture.

---

## ⭐ Core Features

| Feature | Description | Status |
| --- | --- | :---: |
| 📜 **Data-Driven Quest Engine** | Native table-driven execution runtime (`tablelane`) replacing 1,500+ legacy Java files; full coverage of multi-stage dialogue, hunt, collect, and cutscene sequences. | ✅ Authentic Parity |
| 🧠 **AI & Pathfinding Engine** | Original `.path/.idx` multi-layer A* 3D navigation; optimized aggro, patrol, escort, and obstacle handling. | ✅ Enhanced |
| 🐾 **Client-Simulated Minions** | Fully authentic minion follow model simulated on the client side, eliminating server pathfinding overhead and spammy teleport broadcasts. | ✅ Client Simulation |
| ⚔️ **PvP CC Decay Chains** | Port of original abnormal status diminishing returns with dynamic resistance increase and tiered time windows. | ✅ Authentic Parity |
| 🎁 **Scalable Drops & Economy** | Dynamic drop calculations, level-based reduction, group roll/bid, auto-loot, and corpse lifecycle management. | ✅ Complete |
| 🛠️ **Operations & GM Tooling** | Batch spawning (`//spawn <id>*<count>`), persistent/non-persistent spawn separation (`//spawn` vs `//spawns`), and automated packaging scripts. | ✅ Ready |

---

## 🗺️ Roadmap

| Milestone & Direction | Scope & Objectives | Status |
| --- | --- | :---: |
| 📜 **Data-Driven Quest Migration** | Full migration of 1,500+ hardcoded Java quest handlers to a unified native table-driven runtime aligned with authentic 5.8. | ✅ Done |
| 🏰 **Instance Engine & Original Mechanics** | Deep alignment of 5.8 original instance workflows, boss phase AI decoupling, and data-driven instance script orchestration. | 🚧 In Progress |
| ⚡ **Go Transformation & Microservices** | Incremental re-architecture with Go: high-concurrency connection gateway, decoupled login/chat standalone services, and low-latency packet routing. | 📝 Todo |
| 📦 **Static Data Modernization** | Migrating voluminous XML static datasets into streaming, memory-mapped compact JSONL / binary formats to minimize startup footprint. | 📝 Todo |

---

## ⚡ Quick Start

### Prerequisites
- **JDK 25+** • **Maven 3.7+** • **MySQL 8.0+** • **Aion 5.8 Client**

### 1. Database Initialization
Database schemas and initial data scripts are located at:
- **Login Server**: `src/main/resources/db/mysql/al_server_ls.sql`
- **Game Server**: `src/main/resources/db/mysql/al_server_gs.sql`

### 2. Build & Run
```bash
./scripts/package.sh         # Build and deploy to aion/ (or ./scripts/re-package.sh to preserve config)
./aion/start-silent.sh       # Start in background (logs: tail -f aion/log/aionemu.log)
./aion/shutdown.sh           # Graceful shutdown (or ./aion/stop-silent.sh)
```

---

## 🎮 Client Patch

The `patch/` directory contains companion enhancements for the Aion 5.8 client (`bin64/` runtime, `L10N/` quest texts, `Levels/` terrain, and `Textures/` UI).
- **Usage**: Please **back up your client's original files first**, then copy and overwrite corresponding folders from `patch/` into your client root installation directory.

---

## 💡 Contributing

Contributions are warmly welcomed!
1. **Guidelines**: Maintain bilingual (EN/ZH) comments for key interfaces, adhere to data-driven design, and avoid hardcoded exceptions.
2. **Testing**: Ensure all tests pass before submitting (`mvn test`).
3. **Pull Request**: Clearly describe your changes, alignment rationale, and test results.

---

## 🤝 Acknowledgments & License

- Based on **Aion 5.8 Community Emulator** and earlier community work; architecture and protocol insights inspired by [Beyond Aion](https://github.com/beyond-aion/aion-server) (GPL-3.0).
- This project is licensed under the [GNU General Public License v3.0](LICENSE).

<div align="center">

**AionEmu** • Crafting the ultimate Aion 5.8 server experience.

</div>
