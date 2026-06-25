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
- Continue improving startup logs, static-data progress output, test coverage, and local runtime stability.

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

## Runtime Notes

The default configuration lives in `src/main/resources/application.yml`. Game and login services are enabled by default, while chat is disabled by default. Legacy configuration and data are still organized through the project `config`, `data`, and `sql` directories, then bridged into the Spring Boot environment during startup.

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
