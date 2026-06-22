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

The `.java` files under `src/main/resources/aion/**/data/scripts/**` are Aion runtime script resources loaded by the server, not Maven main source files.

## Configuration Defaults

- `aion.services.login.enabled=true`
- `aion.services.game.enabled=true`
- `aion.services.chat.enabled=false`
- `aion.services.transport.mode=netty`

Chat can be enabled with `aion.services.chat.enabled=true` or the bundled `application-chat.yml` profile resource.

Database defaults are aligned to the local MySQL instance:

- Host: `127.0.0.1:3306`
- User: `root`
- Password: `123456`

Initialization SQL now lives under `docs/mysql/`.

## Completed Work

- [x] Flattened the Maven reactor into a single `aionemu` jar module.
- [x] Moved Java application and test code under Maven-standard `src/main/java` and `src/test/java`.
- [x] Moved service configuration and bundled resources under `src/main/resources`.
- [x] Added one Spring Boot launcher with `WebApplicationType.NONE`.
- [x] Added boot-managed lifecycle ordering: login phase 100, chat phase 200, game phase 300.
- [x] Made chat disabled by default and configurable.
- [x] Made Netty the default transport mode with explicit `legacy-nio` fallback.
- [x] Migrated the chat client acceptor to a Netty 4 adapter while preserving the existing chat packet handlers.
- [x] Moved MySQL initialization SQL into `docs/mysql/`.
- [x] Initialized and verified local database schemas.
- [x] Fixed the Java agent shaded jar so project callback classes are included.

## Verification Evidence

- `JAVA_HOME=$(/usr/libexec/java_home -v 25) rtk mvn -Dtest=AionServicesPropertiesTest,GameServerTest,ServiceContextTest test`
  - Result: 6 tests, 0 failures, 0 errors.
- `JAVA_HOME=$(/usr/libexec/java_home -v 25) rtk mvn -DskipTests package`
  - Result: `BUILD SUCCESS`.
- Fat jar smoke checks:
  - default Netty mode logs `Using Netty transport mode...`.
  - explicit `legacy-nio` fallback logs `Using legacy NIO transport...`.
  - chat enabled logs `Starting chat service...`.
  - chat enabled in Netty mode logs `Netty server listening ... for Chat Client Connections`.
  - chat disabled does not create an `AL-Chat` runtime directory.
- Login + game smoke with temporary `aion.home` reached `Server initialization COMPLETE`.
- Database schema verification:
  - `al_server_gs` has 98 tables.
  - `al_server_ls` has 10 tables.

## Remaining Technical Debt

- Game outbound login/chat connectors still use the legacy NIO dispatcher.
- Full protocol parity still needs client-side runtime validation after the structural migration.
