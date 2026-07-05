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
./maven-package.sh
```

Run `./maven-package.sh test` if you want the helper to execute a different Maven goal.

## Quick Start

```bash
./maven-package.sh
./start-silent.sh
tail -f aion/log/aionemu.log
```

Stop the background process with:

```bash
./stop-silent.sh
```

`start-silent.sh` starts `target/AionEmu.jar`, writes logs and the pid file under `aion/log/`, and copies missing runtime resources from `src/main/resources/aion` into `aion/`. Existing files are kept by default. Use `./start-silent.sh -c` only when you intentionally want to delete and recreate the local `aion/` runtime directory before startup.

Common runtime overrides:

```bash
AION_HOME=/path/to/runtime ./start-silent.sh
AION_HEAP_OPTS="-Xms2g -Xmx10g" ./start-silent.sh
AION_JVM_OPTS="..." ./start-silent.sh
```

## Runtime Notes

The Spring Boot defaults live in `src/main/resources/application.yml`. Game, login, and chat services are enabled by default, and the transport mode defaults to Netty.

Legacy-style server configuration and data defaults live under `src/main/resources/aion`. At runtime the server resolves paths from `aion.home`, which defaults to the repository-local `aion/` directory in the helper scripts. This lets local config changes, logs, generated files, and copied static data stay outside the source resource tree.

Use this command when source runtime resources changed and you want to refresh the local `aion/` copy without overwriting local config:

```bash
./refresh-aion.sh
```

`refresh-aion.sh` overwrites non-config runtime files, preserves existing `*/config/**` files, and keeps an existing `aion/log/logback-spring.xml`.

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

[Monono2 Geodata](https://drive.google.com/file/d/1jjLjPDoU5NQr7u7jfg1xqkhKfMdEX1RY/view?usp=sharing) (requires setting **gameserver.geodata.monon2.in.use = true** in geodata.properties)

[Navmeshes](https://drive.google.com/file/d/1ulkx0TwdDZnFZL5ildkVFtD1WQ3jGA7p/view?usp=sharing)

The **nav** folder from the archive goes into your AL-Game\data folder. Make sure that you have **gameserver.geo.nav.pathfinding.enable = true** in geodata.properties

The use of navmeshes is optional. However, they drastically improve npc navigation at the cost of additional 2GB RAM usage.

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
