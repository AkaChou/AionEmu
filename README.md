# AionEmu

[中文](README.zh-CN.md)

Aion 5.8 community server. Single Maven project, JDK 25, Spring Boot starts login / game / chat.

## Highlights

| Area | What is included |
| --- | --- |
| Runtime | Embedded login, game, and chat services with shared lifecycle and Netty transport. |
| Game data | Data-driven quests, NPCs, spawns, skills, items, instances, events, housing, and world definitions. |
| Drops | Deeply optimized NPC, quest, event, and global drop calculation; scalable Kinah rewards and level-based reduction; group distribution, roll/bid, auto-loot, pet/minion pickup, unique-drop announcements, and corpse lifecycle handling. |
| AI & movement | Deeply optimized perception, aggro, skill selection, patrol, flee, escort, crowd avoidance, terrain-aware movement, and bounded stuck recovery. |
| Geo & pathfinding | Terrain height, materials, visibility, and collision queries; original PATH `.path/.idx` multi-layer A*; long-distance block/portal search; PATH-Z-preserving smoothing with final GEO validation; 3D flight and swimming paths. |
| Operations | External `aion.home` runtime directory, separated configuration and logs, packaged JAR/scripts, and admin/diagnostic commands. |

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

This builds `target/AionEmu.jar` and deploys the JAR, resources, and lifecycle scripts to `aion/` (or the directory in `AION_HOME`).

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
| `src/main/resources/aion/data/` | Versioned static game data, quests, definitions, and packets |
| `src/main/resources/aion/geo/` | Terrain, collision, and original PATH resources |
| `src/main/resources/aion/config/` | Versioned configuration defaults |
| `src/main/resources/db/mysql/` | Login and game database schemas |
| `aion/` | Local deployment directory for JAR, runtime config, and logs |
| `docs/` | Quest, pathfinding, terminology, and maintenance documentation |
| `patch/` | Aion 5.8 client patch files and usage notes |

## Documentation

- [A* PATH pathfinding plan](docs/PATH_ASTAR_REFACTOR_PLAN.md)
- [Quest catalog](docs/QUEST_CATALOG.zh-CN.md)
- [Quest writing guide](docs/quest/WRITING_GUIDE.md) / [中文](docs/quest/WRITING_GUIDE.zh-CN.md)
- [Quest repair playbook](docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md)
- [Client patch documentation](patch/patch_documentation.md)

## Development

Run the test suite from the repository root:

```bash
mvn test
```

The application entry point is `com.aionemu.AionBootApplication`.

## Credits

Based on Aion 5.8 Community Emulator and earlier community work.

Thanks to Aion-Lightning, Encom, [Beyond Aion](https://github.com/beyond-aion/aion-server), and the many community contributors behind earlier Aion server work.

## License

This project is licensed under [GPL-3.0](LICENSE).
