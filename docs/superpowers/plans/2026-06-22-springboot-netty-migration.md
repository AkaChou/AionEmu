# Spring Boot Netty Migration Status

**Goal:** run login, optional chat, and game from one Spring Boot process with Netty as the default transport.

**Current architecture:** single Maven module. The Spring Boot entrypoint, lifecycle classes, and migrated server code live under `src/main/java`. Default service configuration and bundled Aion config/data resources live under `src/main/resources`.

**Tech stack:** Java 25, Maven, Spring Boot, Netty 4, existing AionEmu protocol and service code.

---

## User Constraints

- Do not keep the Maven multi-module layout.
- Java source code must be under `src/main/java`; tests must be under `src/test/java`.
- Configuration and runtime resources must be under `src/main/resources`.
- `chat` must be configurable and disabled by default.
- The user explicitly requested no red-light tests for this migration.

## Current File Structure

- `pom.xml`: single Maven module for the combined application.
- `src/main/java/com/aionemu/boot/AionBootApplication.java`: only Spring Boot process entrypoint.
- `src/main/java/com/aionemu/boot/config/AionServicesProperties.java`: binds `aion.services.*`.
- `src/main/java/com/aionemu/boot/lifecycle/*ServiceLifecycle.java`: login, chat, and game lifecycle wrappers.
- `src/main/java/com/aionemu/boot/transport/*`: boot-managed transport boundary.
- `src/main/resources/application.yml`: default Spring Boot configuration.
- `src/main/resources/application-chat.yml`: optional chat profile configuration.
- `src/main/resources/aion/login/**`: login service config, data, and runtime resources.
- `src/main/resources/aion/game/**`: game service config, data, and runtime resources.
- `src/main/resources/aion/chat/**`: chat service config.
- `src/test/java/**`: focused migration tests.

No Java source files are kept under `src/main/resources`; application code lives under `src/main/java` and tests under `src/test/java`.

## Configuration Defaults

- `aion.services.login.enabled=true`
- `aion.services.game.enabled=true`
- `aion.services.chat.enabled=false`
- `aion.services.transport.mode=netty`

Chat can be enabled with `aion.services.chat.enabled=true` or the bundled `application-chat.yml` profile resource.

When no explicit runtime directory system properties are provided, bundled resources are materialized under `aion.home` using `login/`, `chat/`, and `game/` directories instead of the old `AL-*` module names.

Database defaults are aligned to the local MySQL instance:

- Host: `127.0.0.1:3306`
- User: `root`
- Password: `123456`

Initialization SQL now lives under `docs/mysql/`.

## Completed Work

- [x] Flattened the Maven reactor into a single `aionemu` jar module.
- [x] Moved Java application and test code under Maven-standard `src/main/java` and `src/test/java`.
- [x] Moved service configuration and bundled resources under `src/main/resources`.
- [x] Removed Java source files from `src/main/resources`.
- [x] Changed default runtime materialization paths from old `AL-*` module names to `login/`, `chat/`, and `game/` under `aion.home`.
- [x] Prepared the `game/cache` runtime directory during boot path setup instead of relying on callers to create it implicitly.
- [x] Routed geodata world file lookup through `aion.game.data.dir` so runtime data overrides work under the new boot home layout.
- [x] Added one Spring Boot launcher with `WebApplicationType.NONE`.
- [x] Removed standalone production `main` entrypoints outside `AionBootApplication`.
- [x] Added boot-managed lifecycle ordering: login phase 100, chat phase 200, game phase 300.
- [x] Made chat disabled by default and configurable.
- [x] Made Netty the default transport mode with explicit `legacy-nio` fallback.
- [x] Migrated the chat client acceptor to a Netty 4 adapter while preserving the existing chat packet handlers.
- [x] Shared boot-managed Netty 4 event loops across migrated service endpoints.
- [x] Made the chat Netty server lazy so loading the class does not bind ports when chat is disabled.
- [x] Made login partial-startup cleanup avoid initializing NetConnector or CronService during shutdown.
- [x] Added DAOManager initialization-state checks and moved login server stats cleanup before DAO/database shutdown.
- [x] Added a reusable Netty 4 client connector and moved game-to-chat outbound connections to Netty when Netty transport mode is enabled.
- [x] Moved game-to-login outbound connections to Netty when Netty transport mode is enabled.
- [x] Removed the extra legacy NIO dispatcher from GameServer startup when Netty transport mode is enabled.
- [x] Added direct Netty connection handler tests for length-frame parsing, write flushing, and single disconnect notification.
- [x] Added Netty pending-close characterization coverage for close-frame flushing and single disconnect notification.
- [x] Added chat Netty4/JBoss buffer adapter tests for inbound and outbound packet bridging.
- [x] Isolated chat Aion packet internals behind `PacketReader` and `PacketWriter`, keeping JBoss `ChannelBuffer` at the legacy Netty 3 boundary.
- [x] Made the boot launcher stop the active transport boundary after stopping service lifecycles or transport preparation failure.
- [x] Made launcher and transport shutdown idempotent across repeated Spring destroy callbacks.
- [x] Routed embedded login task shutdown/restart, chat scheduled restart, and game scheduled/admin shutdown requests through the boot-managed shutdown handler.
- [x] Preserved embedded shutdown mode so login/chat/game restart requests reach the boot launcher as restart requests instead of plain shutdown.
- [x] Tightened the embedded game shutdown fallback so it also closes the active game transport when the boot shutdown handler is unavailable.
- [x] Made chat lifecycle cleanup run when chat startup fails before returning successfully.
- [x] Made chat shutdown skip legacy Netty server creation when the chat Netty singleton was never initialized.
- [x] Made CronService removable on shutdown so the same service context can initialize it again in one JVM.
- [x] Removed the solo-play `.vs` command entrypoint, permission entry, help text, and internal solo queue scheduler.
- [x] Moved MySQL initialization SQL into `docs/mysql/`.
- [x] Initialized and verified local database schemas.
- [x] Fixed the Java agent shaded jar so project callback classes are included.

## Verification Evidence

- `JAVA_HOME=$(/usr/libexec/java_home -v 25) rtk mvn -Dtest=AionServicesPropertiesTest,GameServerTest,ServiceContextTest test`
  - Result: 6 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=NettyServerTest,NioServerTest,AionServiceLauncherTest test`
  - Result: 8 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=com.aionemu.chatserver.network.netty.NettyServerTest,com.aionemu.commons.network.NettyServerTest test`
  - Result: 3 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=LoginServerConnectionTest,NettyServerTest,AionServiceLauncherTest test`
  - Result: 9 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=NettyConnectionHandlerTest test`
  - Result: 3 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=NettyConnectionPendingCloseTest test`
  - Result: 1 test, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=Netty4ChatClientServerAdapterTest test`
  - Result: 3 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=ChatPacketBufferAdapterTest,Netty4ChatClientServerAdapterTest,com.aionemu.chatserver.network.netty.NettyServerTest,com.aionemu.commons.network.NettyServerTest test`
  - Result: 8 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=AionTransportBoundaryTest,AionServiceLauncherTest,AionServicesPropertiesTest test`
  - Result: 9 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=AionTransportBoundaryTest,AionServiceLauncherTest,AionServicesPropertiesTest test`
  - Result: 11 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=CronServiceTest,NetConnectorTest,AionServiceLauncherTest test`
  - Result: 7 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=DAOManagerTest,NetConnectorTest,AionServiceLauncherTest test`
  - Result: 9 tests, 0 failures, 0 errors.
- `rtk rg -n "public static void main\\(|static void main\\(" src/main/java src/test/java`
  - Result: only `src/main/java/com/aionemu/boot/AionBootApplication.java` remains in production source.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=AionBootApplicationTest,AionServicesPropertiesTest,AionServiceLauncherTest,AionTransportBoundaryTest test`
  - Result: 13 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=LoginTaskShutdownHandlerTest,ChatRestartRequestTest,ShutdownHookTest,AionServiceLauncherTest,GameServerAuthFailureTest test`
  - Result: 13 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=AionEmbeddedShutdownHandlerTest,AionServiceLauncherTest,ShutdownHookTest,LoginTaskShutdownHandlerTest,ChatRestartRequestTest test`
  - Result: 15 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=ShutdownHookTest,AionServiceLauncherTest,AionTransportBoundaryTest test`
  - Result: 12 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=ChatServiceLifecycleTest,AionServiceLauncherTest test`
  - Result: 9 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=com.aionemu.chatserver.network.netty.NettyServerTest,ChatServiceLifecycleTest test`
  - Result: 4 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=CronServiceTest,ServiceContextTest test`
  - Result: 4 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=AionServicePathsTest,GameServerTest test`
  - Result: 5 tests, 0 failures, 0 errors.
- `rtk rg -n 'cmd_solo|class cmd_solo|super\("vs"\)|^vs\s*=|\.vs :|registerForSolo|unregisterForSolo|soloQueueList|HandleSoloQueue|No opponents found' src/main/java src/main/resources/aion/game/config/administration/commands.properties`
  - Result: no solo-play command or solo queue entrypoint remains; only normal event queue messages remain.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn clean -DskipTests compile`
  - Result: `BUILD SUCCESS`.
- `JAVA_HOME=$(/usr/libexec/java_home -v 25) rtk mvn -DskipTests package`
  - Result: `BUILD SUCCESS`.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -DskipTests package`
  - Result: `BUILD SUCCESS`.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -DskipTests package`
  - Result: `BUILD SUCCESS`.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -DskipTests package`
  - Result: `BUILD SUCCESS`.
- Fat jar smoke checks:
  - Existing IntelliJ process still owned `2106/9014`, so smoke used temporary config ports.
  - Default Netty + chat disabled logged `Aion service startup: login=true, chat=false, game=true`, `Using Netty transport mode`, `Chat service is disabled by boot configuration`, and Netty listeners on `127.0.0.1:19014` and `127.0.0.1:12106`.
  - Chat profile + Netty logged `Aion service startup: login=true, chat=true, game=true`, `AL Chat Server started`, Netty chat listeners on `127.0.0.1:12041` and `127.0.0.1:19021`, and game-side chat connector enabled.
  - Explicit `legacy-nio` fallback logged `Using legacy NIO transport managed by existing game/login/chat startup code` and legacy listeners on `127.0.0.1:19014` and `127.0.0.1:12106`, without boot-managed Netty event loop startup.
- Login + game smoke with temporary `aion.home` reached `Server initialization COMPLETE`.
- Fat jar smoke on Java 25 required `--add-opens java.base/java.lang=ALL-UNNAMED` for legacy cglib/lambdaj access.
- Fat jar smoke with temporary `aion.home`, temporary ports, geodata disabled, svstats disabled, Netty transport, and chat disabled reached `=== Server initialization COMPLETE ===`; it logged Netty listeners on `127.0.0.1:19014`, `127.0.0.1:12106`, and `127.0.0.1:17777`, plus `Chat Server is disabled by configuration`.
- Database schema verification:
  - `al_server_gs` has 98 tables.
  - `al_server_ls` has 10 tables.

## Remaining Technical Debt

- Legacy NIO transport remains available as an explicit fallback mode.
- Login service startup still has large static initialization blocks; shutdown is guarded for partially initialized DAO/transport/service state, but finer-grained startup components would make failure cleanup easier to test.
- Full protocol parity still needs client-side runtime validation after the structural migration.
