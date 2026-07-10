# AionEmu

[Chinese README](README.zh-CN.md)

AionEmu is a community server project for Aion 5.8. This repository continues from the original Aion 5.8 Community Emulator and focuses on making the server codebase easier to start, debug, maintain, and evolve on a modern Java runtime.

The project is now organized as a single Maven build, targets Java 25 bytecode, and uses a non-web Spring Boot entry point to coordinate the game, login, and chat service lifecycles. It keeps the existing game logic, data loading, network protocol, DAO layer, callback system, and static data model while gradually improving startup observability, configuration bridging, Netty transport work, build-time callback weaving, tests, and local debugging.

## Project Focus

- Maintain, fix, and modernize an Aion 5.8 server emulator.
- Use a unified Maven build with Java 25 bytecode.
- Use `com.aionemu.AionBootApplication` as the production startup entry point.
- Manage login, game, and chat startup through Spring Boot lifecycle wiring.
- Preserve compatibility with legacy configuration, static data, database schemas, and client protocols.
- Run callback weaving during the build to reduce runtime `-javaagent` requirements.
- Keep local runtime state under `aion/` so generated logs, copied data, and local configuration stay separate from source defaults.
- Continue improving startup logs, static-data progress output, startup time, test coverage, and local runtime stability.

## Build

JDK 25 or newer is required.

```bash
mvn package
```

Common quick checks:

```bash
mvn test
mvn -DskipTests package
```

The repository also includes a packaging helper that sets Maven memory defaults and writes `target/AionEmu.jar`:

```bash
./package.sh
```

Run `./package.sh test` if you want the helper to execute a different Maven goal.

`package.sh` overwrites matching runtime resources and configuration. Use `./re-package.sh` to update an existing runtime tree while preserving current configuration; missing configuration files are still copied.

## Quick Start

```bash
./package.sh
./aion/start-silent.sh
tail -f aion/log/aionemu.log
```

Stop the background process with:

```bash
./aion/stop-silent.sh
```

`package.sh` deploys the JAR, Geo data, and start/stop scripts into `aion/`. `aion/start-silent.sh` starts `aion/AionEmu.jar` and writes logs and the pid file under `aion/log/`. `aion/shutdown.sh` waits for the in-game graceful shutdown flow; `aion/stop-silent.sh` force-stops after its timeout. Use `./aion/start-silent.sh -c` only when you intentionally want to clean runtime data; the deployed JAR and scripts are preserved.

Common runtime overrides:

```bash
AION_HOME=/path/to/runtime ./aion/start-silent.sh
AION_HEAP_OPTS="-Xms2g -Xmx10g" ./aion/start-silent.sh
AION_JVM_OPTS="..." ./aion/start-silent.sh
```

## Runtime Notes

The Spring Boot defaults live in `src/main/resources/application.yml`. Game, login, and chat services are enabled by default, and the transport mode defaults to Netty.

Legacy-style server configuration and data defaults live under `src/main/resources/aion`. At runtime the server resolves paths from `aion.home`, which defaults to the repository-local `aion/` directory in the helper scripts. This lets local config changes, logs, generated files, and copied static data stay outside the source resource tree.

NPC navigation still requires separate geodata/navmesh resources. The original resource notes are preserved below.

## Upstream Acknowledgements

This project builds on the long-running work of Aion 5.8 Community Emulator and earlier Aion server communities. The original README introduction, resource notes, and contributor list are preserved below.

Thank you to the Aion-Lightning team, the Encom team, and every individual contributor for the protocol, data, AI, quest, instance, networking, tooling, and operations work that made this codebase possible.

---

![final](https://github.com/user-attachments/assets/5143172d-41a8-4b68-add2-73ad635df78a)

## Original Aion 5.8 Community Project

This is a repository for Aion 5.8 Community Emulator

Community Discord : https://discord.gg/Nt7rBd8mnN

### NPC Navigation

Geodata lives in `src/main/resources/aion/game/geo` and uses `models.mesh` with per-map `.geo`, height `.png`, and optional `_materials.png` files.

Navigation meshes go into `src/main/resources/aion/game/geo/nav`. Set `gameserver.geo.nav.pathfinding.enable = true` in `geodata.properties` to enable pathfinding.

### Known Developers

#### Teams:

Aion-Lightning team

Encom team

#### Individual contributors:

AEJTester; ATracer; Aion Gates; AionChs Master; Alcapwnd; Antivirus; Antraxx; Aquanox; Avol;
Ben; Bio; CoolyT; Divinity; Dr.Nism; Falke_34; FrozenKiller; Ghostfur; GiGatR00n; Gregg Patton;
Hilgert; Ian Phillips; IceReaper; Imaginary; Joshua Slack; KID; KKnD; Kamui; Kirill Vainer;
KorLightning; Layane; Lightning; LokiReborn; Luno; Luzien; Lyahim; M@xx; Magenik; Mark Powell;
MATTY; Metos; Mr. Poke; Nemesiss; Nemiroff; NewLives; Rama; Ranastic; Rinzler; G-Robson26; Rolandas;
RotO; Sarynth; Simple; Sippolo; SoulKeeper; Source; Sweetkr; Taran; Undertrey; VladimirZ; Wakizashi;
Wnkrz; Xav; Xitanium; alexa026; antness; cura; dezalmado; kecimis; kosyachok; lhw; lhwKaipo; lord_rex;
nrg; orfeo087; orz; rhys2002; sphinx; srx47; vlog; xTz; xavier; yecgaaj; zdead; Angry Catster; cinus;
kortana; Magenik; sunbsn; podpol; DainAvenger; Belloxy; WIZARDMASTER; Novichok;
Beckupgaming/BeckUp-Media; BlueGalaxy; VNickXXL;
