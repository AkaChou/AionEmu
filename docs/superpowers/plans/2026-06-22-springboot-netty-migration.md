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
- `src/main/java/com/aionemu/AionBootApplication.java`: only Spring Boot process entrypoint.
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

Initialization SQL now lives under `src/main/resources/db/mysql/`.

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
- [x] Guarded chat runtime and Spring configuration beans so chat internals only load when `aion.services.chat.enabled=true`.
- [x] Made Netty the boot-managed transport mode and retired the explicit `legacy-nio` fallback.
- [x] Removed the unused commons NIO server acceptor layer after retiring the boot fallback.
- [x] Removed legacy socket-channel connection factories and constructors from migrated packet connections.
- [x] Migrated the chat client acceptor to a Netty 4 adapter while preserving the existing chat packet handlers.
- [x] Shared boot-managed Netty 4 event loops across migrated service endpoints.
- [x] Made the chat Netty server lazy so loading the class does not bind ports when chat is disabled.
- [x] Made login partial-startup cleanup avoid initializing NetConnector or CronService during shutdown.
- [x] Added DAOManager initialization-state checks and moved login server stats cleanup before DAO/database shutdown.
- [x] Made login shutdown cleanup use a Spring-instantiable `Shutdown` bean instead of the legacy singleton fallback in the boot-managed bridge.
- [x] Routed login shutdown requests and process runtime shutdown hooks through a shared shutdown service wrapper instead of duplicate singleton fallbacks.
- [x] Made the shared login shutdown service wrapper own its lazy fallback instead of calling the legacy shutdown singleton directly.
- [x] Made login player-transfer startup use a Spring-instantiable service bean while retaining the legacy singleton only as a lazy fallback.
- [x] Routed login player-transfer packet access through a Spring-provided service before the legacy singleton fallback.
- [x] Routed the login startup runtime bridge through the shared player-transfer service wrapper instead of keeping its own singleton fallback.
- [x] Made the login player-transfer service wrapper own its lazy fallback instead of calling the legacy transfer singleton directly.
- [x] Routed login game-server packet execution and delayed MAC-ban-list dispatch through a Spring-provided thread-pool manager before the legacy singleton fallback.
- [x] Routed login startup thread-pool initialization and player-transfer scheduling through the Spring-provided thread-pool manager before the legacy singleton fallback.
- [x] Routed login cron runner and task trigger scheduling through the Spring-provided thread-pool manager before the legacy singleton fallback.
- [x] Routed login shutdown thread-pool cleanup through the Spring-provided thread-pool manager before the legacy singleton fallback.
- [x] Routed commons Netty packet executor and common-network shutdown cleanup through a Spring-provided thread-pool manager before the legacy singleton fallback.
- [x] Made the login thread-pool manager Spring-instantiable and moved its non-Spring fallback into the shared service wrapper.
- [x] Made login premium-controller startup use a Spring-instantiable controller bean while retaining the legacy singleton only as a lazy fallback.
- [x] Routed the login startup runtime bridge through the shared premium-controller wrapper instead of keeping its own singleton fallback.
- [x] Routed login premium packet access through the shared premium-controller wrapper.
- [x] Made the login premium-controller wrapper own its lazy fallback instead of calling the legacy premium singleton directly.
- [x] Made login task-from-db startup use a Spring-instantiable manager bean while retaining the legacy singleton only as a lazy fallback.
- [x] Routed the login startup runtime bridge through the shared task-from-db manager wrapper instead of keeping its own singleton fallback.
- [x] Made the login task-from-db wrapper own its lazy fallback instead of calling the legacy task-manager singleton directly.
- [x] Made login banned-MAC management Spring-instantiable while replacing its eager singleton with a lazy fallback.
- [x] Routed login banned-MAC packet access through a Spring-provided manager before the legacy singleton fallback.
- [x] Routed login flood/brute-force protection access through Spring-provided services before legacy singleton fallbacks.
- [x] Routed login banned-IP startup and packet access through a Spring-provided service wrapper.
- [x] Made login `NetConnector` discard its transport on shutdown so later embedded lifecycles create a fresh transport.
- [x] Routed login startup network connect through a Spring-provided server transport before the legacy `NetConnector` fallback.
- [x] Moved login server transport bridges from the legacy `NetConnector.getInstance()` accessor to the named transport lifecycle accessor.
- [x] Routed login cron initialization and shutdown cleanup through a dedicated cron service bridge.
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
- [x] Made chat restart scheduling Spring-instantiable and wired shutdown cleanup through the boot-provided restart service before falling back to the legacy singleton.
- [x] Made chat core services Spring-instantiable in the chat Spring configuration while keeping legacy singleton accessors as deprecated compatibility fallbacks.
- [x] Routed chat core service access from legacy dependencies, channel IDs, client packet handlers, and game-server packets through a Spring-provider bridge.
- [x] Routed chat shutdown game-server offline cleanup through the Spring-provided core service bridge.
- [x] Routed chat restart-service fallback access through a Spring-provider bridge.
- [x] Routed chat Netty server fallback access through a Spring-provider bridge.
- [x] Routed chat shutdown-hook fallback access through a local bridge while Spring creates the configured hook directly.
- [x] Routed chat shutdown Netty and commons thread-pool cleanup through bridge helpers.
- [x] Routed chat game-server packet execution through the commons thread-pool bridge.
- [x] Routed game core service fallback access through a local helper instead of direct singleton calls in the runtime bridge.
- [x] Routed game engine fallback access through a local helper instead of direct singleton calls in the runtime bridge.
- [x] Routed game world-service fallback access through a local helper instead of direct singleton calls in the runtime bridge.
- [x] Routed game world-bootstrap fallback access through a local helper instead of direct singleton calls in the runtime bridge.
- [x] Routed game event-bootstrap fallback access through a local helper instead of direct singleton calls in the runtime bridge.
- [x] Routed game event-runtime fallback access through a local helper instead of direct singleton calls in the runtime bridge.
- [x] Routed game maintenance-service fallback access through a local helper instead of direct singleton calls in the runtime bridge.
- [x] Routed game server-network fallback access through a local helper instead of direct singleton calls in the runtime bridge.
- [x] Routed game shutdown-hook fallback access through a local helper instead of direct singleton calls in network startup and shutdown request paths.
- [x] Routed game housing fallback access through a local helper instead of direct singleton calls in the runtime bridge.
- [x] Routed game battlefield fallback access through a local helper instead of direct singleton calls in the runtime bridge.
- [x] Made the game shutdown hook Spring-instantiable while keeping the legacy singleton only as a fallback.
- [x] Made the game banned-MAC manager Spring-instantiable while keeping the legacy singleton only as a fallback.
- [x] Made the game admin service Spring-instantiable while keeping the legacy singleton only as a fallback.
- [x] Made the game player-transfer service Spring-instantiable while keeping the legacy singleton only as a fallback.
- [x] Made 10 lightweight game reward, Dredgion, and location-bootstrap services Spring-instantiable while keeping legacy singletons only as fallbacks.
- [x] Made 10 lightweight game rift, location-bootstrap, and abyss landing services Spring-instantiable while keeping legacy singletons only as fallbacks.
- [x] Made 10 lightweight default-constructor game service beans Spring-instantiable while keeping legacy singletons only as fallbacks.
- [x] Made 10 lightweight engine, geo/nav, challenge, and trade service beans Spring-instantiable while keeping legacy singletons only as fallbacks.
- [x] Made 10 lightweight FFA and battlefield entrance services Spring-instantiable while keeping legacy singletons only as fallbacks.
- [x] Made 10 runtime event, packet-broadcast, drop-registration, and landing-special service beans Spring-instantiable while keeping legacy singletons only as fallbacks.
- [x] Made 10 scheduled player-event, announcement, debug, house-maintenance, and veteran-reward service beans Spring-instantiable while keeping legacy singletons only as fallbacks.
- [x] Made 10 business data, broker, petition, weather, and world-object service beans Spring-instantiable while keeping legacy singletons only as fallbacks.
- [x] Made 10 housing, town, cleaning, html-cache, zone, hotspot, road, and landing-update service beans Spring-instantiable while keeping legacy singletons only as fallbacks.
- [x] Made gameserver `LoginServer` and `ChatServer` Spring-instantiable while routing their static compatibility accessors through Spring providers before legacy fallbacks.
- [x] Made gameserver `ThreadPoolManager` Spring-instantiable while routing its static compatibility accessor through a Spring provider before the legacy fallback.
- [x] Made gameserver `IDFactory` Spring-instantiable while routing its static compatibility accessor through a Spring provider before the legacy fallback.
- [x] Made gameserver `World` Spring-instantiable while routing its static compatibility accessor through a Spring provider before the legacy fallback.
- [x] Made gameserver `DataManager` Spring-instantiable while routing its static compatibility accessor through a Spring provider before the legacy fallback.
- [x] Preserved embedded shutdown mode so login/chat/game restart requests reach the boot launcher as restart requests instead of plain shutdown.
- [x] Tightened the embedded game shutdown fallback so it also closes the active game transport when the boot shutdown handler is unavailable.
- [x] Made chat lifecycle cleanup run when chat startup fails before returning successfully.
- [x] Made chat shutdown skip legacy Netty server creation when the chat Netty singleton was never initialized.
- [x] Made CronService removable on shutdown so the same service context can initialize it again in one JVM.
- [x] Moved MySQL initialization SQL into `src/main/resources/db/mysql/`.
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
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=NetConnectorTest,DAOManagerTest,AionServiceLauncherTest test`
  - Result: 11 tests, 0 failures, 0 errors.
- `rtk rg -n "public static void main\\(|static void main\\(" src/main/java src/test/java`
  - Result: only `src/main/java/com/aionemu/AionBootApplication.java` remains in production source.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=AionBootApplicationTest,AionServicesPropertiesTest,AionServiceLauncherTest,AionTransportBoundaryTest test`
  - Result: 13 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=AionBootApplicationTest,AionServicesPropertiesTest test`
  - Result: 11 tests, 0 failures, 0 errors.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -Dtest=ChatServiceLifecycleTest,AionBootApplicationTest,AionServicesPropertiesTest test`
  - Result: 14 tests, 0 failures, 0 errors.
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
  - The earlier explicit `legacy-nio` fallback smoke is historical; the fallback has since been retired and `LEGACY_NIO` is rejected as an unsupported transport mode.
- Login + game smoke with temporary `aion.home` reached `Server initialization COMPLETE`.
- Earlier fat jar smoke on Java 25 required `--add-opens java.base/java.lang=ALL-UNNAMED` for legacy reflective proxy access; current code no longer depends on that workaround.
- Fat jar smoke with temporary `aion.home`, temporary ports, geodata disabled, svstats disabled, Netty transport, and chat disabled reached `=== Server initialization COMPLETE ===`; it logged Netty listeners on `127.0.0.1:19014`, `127.0.0.1:12106`, and `127.0.0.1:17777`, plus `Chat Server is disabled by configuration`.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q -Dtest=GameLegacyServiceBridgeConfigurationTest,GameNetworkStartupLifecycleTest,GameShutdownRequestTest,ShutdownHookTest,AionBootApplicationTest test`
  - Result: exit code 0.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q -Dtest=GameServerNetworkRuntimeBridgeTest test`
  - Result: exit code 0.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q -Dtest=GameThreadPoolLifecycleTest test`
  - Result: exit code 0.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q -Dtest=GameWorldBootstrapRuntimeBridgeTest test`
  - Result: exit code 0.
- `rtk env JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q -Dtest=GameCoreServicesRuntimeBridgeTest test`
  - Result: exit code 0.
- Database schema verification:
  - `al_server_gs` has 98 tables.
  - `al_server_ls` has 10 tables.

## Remaining Tasks

- [ ] Confirm `GameLegacyServiceBridgeConfiguration` stays free of `return Xxx.getInstance();` bridge beans during subsequent migration slices.
- [ ] Use 10-service batches for similar simple `GameLegacyServiceBridgeConfiguration` bean reductions. Do not split similar simple conversions into three-service commits; only use a smaller commit when fewer than 10 same-kind simple candidates remain.
- [ ] Replace each reduced legacy bean with Spring-instantiable construction only when the constructor and initialization behavior are safe under lazy Spring ownership.
- [ ] Keep legacy singleton accessors as fallback compatibility paths until the corresponding startup/runtime path has Spring-provider coverage and focused tests.
- [ ] Break up the large login service startup static initialization path into finer-grained Spring-managed components with partial-startup cleanup coverage.
- [ ] Defer tests for simple mechanical Spring-migration slices; run one final unified test pass before declaring the migration complete.
- [ ] Run focused boot/runtime smoke checks after larger startup or behavior-changing slices, including login + game with temporary `aion.home`, Netty transport, and chat disabled by default.
- [ ] Run chat-enabled smoke after chat-affecting slices to preserve optional chat startup and game-side chat connector behavior.
- [ ] Validate full client protocol parity after the structural migration, covering login, character entry, game networking, chat-enabled mode, shutdown/restart requests, and representative gameplay flows.
- [ ] Keep the migration status evidence current by appending focused test commands, smoke commands, and observed results for each committed slice.
