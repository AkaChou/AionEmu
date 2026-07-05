# AionEmu

[English README](README.md)

AionEmu 是一个面向 Aion 5.8 的社区服务端项目。本仓库在原 Aion 5.8 Community Emulator 的基础上继续维护，重点放在可启动、可调试、可持续演进的现代 Java 服务端代码库上。

当前项目已经整理为单 Maven 工程，使用 JDK 25 构建，并通过 Spring Boot 的非 Web 启动入口统一编排 game、login、chat 等服务生命周期。项目仍保留原有游戏逻辑、数据加载、网络协议、DAO、回调系统和静态数据结构，同时逐步改进启动可观测性、配置桥接、Netty 传输、构建期 callback weaving、测试覆盖和本地调试体验。

## 项目重点

- Aion 5.8 服务端模拟器的维护、修复和现代化。
- 统一 Maven 构建，目标字节码为 Java 25。
- 使用 `com.aionemu.AionBootApplication` 作为生产启动入口。
- 通过 Spring Boot lifecycle 管理 login、game、chat 服务启动顺序。
- 保留并兼容 legacy 配置、静态数据、数据库 schema 和客户端协议。
- 在构建阶段执行 callback weaving，减少运行时 `-javaagent` 依赖。
- 将本地运行状态集中在 `aion/` 下，避免日志、复制后的数据和本地配置混入源码默认资源。
- 持续改进启动日志、静态数据加载进度、启动速度、测试覆盖和本地运行稳定性。

## 构建

需要 JDK 25 或更新版本。

```bash
mvn package
```

常用的快速校验命令：

```bash
mvn test
mvn -DskipTests package
```

仓库也提供了打包脚本，会设置 Maven 默认内存参数并生成 `target/AionEmu.jar`：

```bash
./maven-package.sh
```

如果需要执行其它 Maven 目标，可以把目标传给脚本，例如：

```bash
./maven-package.sh test
```

## 快速启动

```bash
./maven-package.sh
./start-silent.sh
tail -f aion/log/aionemu.log
```

停止后台进程：

```bash
./stop-silent.sh
```

`start-silent.sh` 会启动 `target/AionEmu.jar`，把日志和 pid 文件写到 `aion/log/`，并把 `src/main/resources/aion` 中缺失的运行资源复制到 `aion/`。默认不会覆盖已有文件。只有在明确要删除并重建本地 `aion/` 运行目录时，才使用 `./start-silent.sh -c`。

常见运行参数覆盖：

```bash
AION_HOME=/path/to/runtime ./start-silent.sh
AION_HEAP_OPTS="-Xms2g -Xmx10g" ./start-silent.sh
AION_JVM_OPTS="..." ./start-silent.sh
```

## 运行说明

Spring Boot 默认配置位于 `src/main/resources/application.yml`。game、login、chat 默认都启用，transport mode 默认使用 Netty。

legacy 风格的服务端配置和数据默认资源位于 `src/main/resources/aion`。运行时路径由 `aion.home` 解析；在仓库自带脚本中，默认指向仓库内的 `aion/` 目录。这样本地配置改动、日志、生成文件和复制后的静态数据都会留在源码资源目录之外。

当源码中的运行资源发生变化，并且需要刷新本地 `aion/` 副本但保留本地配置时，使用：

```bash
./refresh-aion.sh
```

`refresh-aion.sh` 会覆盖非配置运行文件，保留已有的 `*/config/**` 文件，并保留已有的 `aion/log/logback-spring.xml`。

NPC 导航仍需要单独准备 geodata/navmesh 资源。相关说明保留在下方原项目介绍中。

## 上游致谢

本项目继承自 Aion 5.8 Community Emulator 及更早期 Aion 服务端社区的长期成果。下面保留原 README 中的项目介绍、资源说明和贡献者名单。

感谢 Aion-Lightning team、Encom team，以及所有个人贡献者长期在协议、数据、AI、任务、实例、副本、网络、工具和运行维护上的投入。没有这些组织和贡献者的持续工作，本仓库无法站在现在的基础上继续演进。

---

![final](https://github.com/user-attachments/assets/5143172d-41a8-4b68-add2-73ad635df78a)

## 原 Aion 5.8 社区项目

This is a repository for Aion 5.8 Community Emulator

Community Discord : https://discord.gg/Nt7rBd8mnN

### NPC Navigation

[Monono2 Geodata](https://drive.google.com/file/d/1jjLjPDoU5NQr7u7jfg1xqkhKfMdEX1RY/view?usp=sharing) (requires setting **gameserver.geodata.monon2.in.use = true** in geodata.properties)

[Navmeshes](https://drive.google.com/file/d/1ulkx0TwdDZnFZL5ildkVFtD1WQ3jGA7p/view?usp=sharing)

The **nav** folder from the archive goes into your AL-Game\data folder. Make sure that you have **gameserver.geo.nav.pathfinding.enable = true** in geodata.properties

The use of navmeshes is optional. However, they drastically improve npc navigation at the cost of additional 2GB RAM usage.

### 已知开发者

#### 团队：

Aion-Lightning team

Encom team

#### 个人贡献者：

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
