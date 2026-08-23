# AionEmu

[中文](README.zh-CN.md)

Aion 5.8 community server. Single Maven project, JDK 25, Spring Boot starts login / game / chat.

## Table of Contents

- [Highlights](#highlights)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Client Setup](#client-setup)
- [Configuration](#configuration)
- [Runtime Scripts](#runtime-scripts)
- [Environment Variables](#environment-variables)
- [Network Endpoints](#network-endpoints)
- [Project Layout](#project-layout)
- [Documentation](#documentation)
- [Development](#development)
- [Troubleshooting](#troubleshooting)
- [Credits](#credits)
- [License](#license)

## Highlights

| Area | What is included |
| --- | --- |
| Runtime | Embedded login, game, and chat services with shared lifecycle and Netty transport. |
| Game data | Data-driven quests, NPCs, spawns, skills, items, instances, events, housing, and world definitions. |
| Drops | Deeply optimized NPC, quest, event, and global drop calculation; scalable Kinah rewards and level-based reduction; group distribution, roll/bid, auto-loot, pet/minion pickup, unique-drop announcements, and corpse lifecycle handling. |
| AI & movement | Deeply optimized perception, aggro, skill selection, patrol, flee, escort, crowd avoidance, terrain-aware movement, and bounded stuck recovery. |
| Geo & pathfinding | Terrain height, materials, visibility, and collision queries; original PATH `.path/.idx` multi-layer A*; long-distance block/portal search; PATH-Z-preserving smoothing with final GEO validation; 3D flight and swimming paths. |
| Operations | External `aion.home` runtime directory, separated configuration and logs, packaged JAR/scripts, and admin/diagnostic commands. |

## Tech Stack

| Component | Choice |
| --- | --- |
| Language | Java 25 |
| Framework | Spring Boot 4.1 (non-web application, Netty transport) |
| Build | Maven, single module |
| Database | MySQL |
| Networking | Netty |
| Scheduling | Quartz |
| XML | Jakarta XML Binding / JAXB |
| Testing | JUnit Jupiter |

## Quick Start

### Requirements

- JDK 25 or newer
- Maven 3.7 or newer
- MySQL for login and game persistence
- An Aion 5.8 client for connecting to the server

### 1. Initialize the databases

The SQL dumps create the required databases and tables:

```bash
mysql -u root -p < src/main/resources/db/mysql/al_server_ls.sql
mysql -u root -p < src/main/resources/db/mysql/al_server_gs.sql
```

### 2. Package the server

```bash
./package.sh
```

This builds `target/AionEmu.jar` and deploys the JAR, resources, and lifecycle scripts to `aion/` (or the directory in `AION_HOME`). The default build skips tests; see [Development](#development) for test and repackaging options.

### 3. Configure the runtime

Edit the deployed configuration after the first package:

```text
aion/config/login/database.properties
aion/config/network/database.properties
aion/config/network/network.properties
```

Set the database credentials and the public game/chat addresses before allowing clients to connect. Use `./re-package.sh` for later builds when existing runtime configuration must be preserved.

### 4. Start and stop

```bash
./aion/start-silent.sh
tail -f aion/log/aionemu.log
./aion/shutdown.sh       # graceful shutdown
./aion/stop-silent.sh
```

Use `AION_HOME=/path/to/runtime` to deploy or run from another runtime directory. `AION_HEAP_OPTS` and the other `AION_*_OPTS` variables can override JVM settings.

```bash
AION_HOME=/path/to/runtime ./aion/start-silent.sh
```

To clean runtime data before starting (keeps the JAR and scripts):

```bash
./aion/start-silent.sh -c
```

## Client Setup

See [Client patch documentation](patch/patch_documentation.md) for optional Aion 5.8 client patches (quest localization and VIP `Game.dll`).

## Configuration

The main runtime configuration files under `aion/config/` are:

| File | Purpose |
| --- | --- |
| `login/database.properties` | Login database (`al_server_ls`) connection settings |
| `network/database.properties` | Game database (`al_server_gs`) connection settings |
| `network/network.properties` | Client ports, public addresses, internal service addresses, and service passwords |
| `main/*.properties` | Gameplay, AI, drops, rates, instances, and other server behavior |
| `administration/*.properties` | Admin/GM commands, panels, and restrictions |
| `schedule/*.xml` | Scheduled events, instances, sieges, and world activities |

Service enablement is configured in `src/main/resources/application.yml` (bundled inside the JAR): which of the login, game, and chat services start, and the Netty transport mode.

## Runtime Scripts

| Script | Description |
| --- | --- |
| `start-silent.sh` | Start the server in the background, write PID and log files |
| `start-silent.sh -c` | Clean runtime data before starting (keeps JAR and scripts) |
| `shutdown.sh` | Request graceful shutdown and wait up to `AION_SHUTDOWN_TIMEOUT` seconds |
| `stop-silent.sh` | Stop the server, with optional force-kill after `AION_STOP_TIMEOUT` seconds |

## Environment Variables

| Variable | Default | Description |
| --- | --- | --- |
| `AION_HOME` | `aion/` or the script directory | Runtime directory for JAR, config, data, and logs |
| `AION_JAR_FILE` | `$AION_HOME/AionEmu.jar` | Path to the server JAR |
| `AION_LOG_DIR` | `$AION_HOME/log` | Log directory |
| `AION_LOG_FILE` | `$AION_LOG_DIR/aionemu.log` | Main log file |
| `AION_PID_FILE` | `$AION_LOG_DIR/aionemu.pid` | PID file |
| `AION_HEAP_OPTS` | `-Xms2g -Xmx8g` | JVM heap settings |
| `AION_GC_OPTS` | G1GC with bounded pause targets | JVM garbage-collection settings |
| `AION_SAFETY_OPTS` | Heap dump and exit-on-OOM | JVM crash-safety settings |
| `AION_SYSTEM_OPTS` | UTF-8, IPv4, Asia/Shanghai timezone | JVM system properties |
| `AION_PATH_OPTS` | `-Daion.home=$AION_HOME -Daion.log.dir=$AION_LOG_DIR` | Path system properties pointing to the runtime and log directories |
| `AION_JVM_OPTS` | Composed from the `AION_*_OPTS` above | Full JVM option line |
| `AION_SHUTDOWN_TIMEOUT` | `120` | Graceful shutdown timeout in seconds |
| `AION_STOP_TIMEOUT` | `30` | Stop timeout before force-kill in seconds |
| `AION_FORCE_STOP` | `true` | Whether to force-kill after `AION_STOP_TIMEOUT` |
| `AION_PRESERVE_CONFIG` | `false` | Preserve existing runtime config during `package.sh` when `true` |
| `MAVEN_THREADS` | `1C` | Maven parallel build threads used by `package.sh` and `re-package.sh` (`1` disables reactor parallelism) |

## Network Endpoints

| Service | Default port | Purpose |
| --- | ---: | --- |
| Login client | `2106` | Client login |
| Game client | `7777` | World/game connection |
| Chat client | `10241` | Chat connection |
| Game -> login | `9014` | Internal service connection |
| Game -> chat | `9021` | Internal service connection |

Ports and advertised addresses are configured in [`network.properties`](src/main/resources/aion/config/network/network.properties).

## Project Layout

| Path | Purpose |
| --- | --- |
| `src/main/java/com/aionemu/` | Boot, shared infrastructure, login, game, and chat source |
| `src/main/resources/application.yml` | Spring Boot entry configuration |
| `src/main/resources/aion/data/` | Versioned static game data, quests, definitions, and packets |
| `src/main/resources/aion/definitions/` | Compact data definitions, schemas, and generation inputs |
| `src/main/resources/aion/geo/` | Terrain, collision, and original PATH resources |
| `src/main/resources/aion/config/` | Versioned configuration defaults |
| `src/main/resources/db/mysql/` | Login and game database schemas |
| `aion/` | Local deployment directory for JAR, runtime config, and logs |
| `scripts/` | Data generation, auditing, runtime helpers, and maintenance tools |
| `docs/` | Quest, pathfinding, terminology, and maintenance documentation |
| `patch/` | Aion 5.8 client patch files and usage notes |

## Documentation

- [A* PATH pathfinding plan](docs/PATH_ASTAR_REFACTOR_PLAN.md)
- [Quest catalog](docs/QUEST_CATALOG.zh-CN.md)
- [Quest writing guide](docs/quest/WRITING_GUIDE.md) / [中文](docs/quest/WRITING_GUIDE.zh-CN.md)
- [Quest repair playbook](docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md)
- [Quest client-dialog mapping](docs/quest/client-dialog-mapping/README.zh-CN.md)
- [Game terminology EN/中文](docs/aion-game-terms-en-zh.md)
- [Client patch documentation](patch/patch_documentation.md)

## Development

Run the test suite from the repository root:

```bash
mvn test
```

Build a runnable JAR:

```bash
mvn package
```

Or use the packaging wrapper to build and deploy in one step:

```bash
./package.sh                              # default: clean + skip tests + package + deploy, using 1C Maven threads
MAVEN_THREADS=2C ./package.sh             # use two Maven threads per available CPU core
./package.sh -DskipTests=false package    # run tests during packaging
./re-package.sh                           # deploy while preserving existing runtime config
```

The application entry point is `com.aionemu.AionBootApplication`.

## Troubleshooting

| Symptom | Likely fix |
| --- | --- |
| `Missing target/AionEmu.jar` | Run `./package.sh` first. |
| `AionEmu is already running` | Use `./aion/shutdown.sh` or `./aion/stop-silent.sh`; check `aion/log/aionemu.pid`. |
| Database connection failure | Verify MySQL is running, credentials in `aion/config/login/database.properties` and `aion/config/network/database.properties`, and that both SQL dumps were imported. |
| Client cannot connect | Check `aion/config/network/network.properties` public addresses/ports and firewall rules. |
| Runtime config overwritten by build | Use `./re-package.sh` or set `AION_PRESERVE_CONFIG=true` when running `./package.sh`. |
| Need a clean runtime | Run `./aion/start-silent.sh -c` (keeps JAR and scripts). |

## Credits

Based on Aion 5.8 Community Emulator and earlier community work.

Thanks to Aion-Lightning, Encom, [Beyond Aion](https://github.com/beyond-aion/aion-server), and the many community contributors behind earlier Aion server work.

## License

This project is licensed under [GPL-3.0](LICENSE).
