# AGENTS.md

This file provides project-level guidance for AI coding agents working in this repository.

> **Detailed rules are split into focused files under `.agent/rules/`.** Read the applicable files from the [Rules Index](#rules-index) for the task at hand.

## Global Rules

1. Do NOT run build commands unless explicitly requested by the user.
2. Do NOT start, stop, or restart server processes. Their current state is unknown, and the user manages their lifecycle.
3. All summary files should be stored in `.agent/summary/` if that directory is available.

## Configuration

- Spring Boot entry configuration: `src/main/resources/application.yml`. The application runs in non-web mode, enables the login, game, and chat services by default, and uses the Netty transport.
- Runtime configuration: `src/main/resources/aion/config/`, split into login, network, main, chat, administration, schedule, and other domains.
- Default network ports are defined in `src/main/resources/aion/config/network/network.properties`: login client 2106, game client 7777, chat client 10241, game-to-login internal connection 9014, and game-to-chat internal connection 9021.
- Login and game database settings are stored in `src/main/resources/aion/config/login/database.properties` and `src/main/resources/aion/config/network/database.properties` respectively.

## Project Overview

AionEmu is an Aion 5.8 community server. A single Spring Boot application hosts the login, game, and chat services and loads quests, NPCs, maps, geodata, instances, and other static game data.

## Technology Stack

- Java 25, Spring Boot 4.1, Maven
- Netty, MySQL Connector/J, Quartz, Jakarta XML Binding
- Lombok, SLF4J/Logback, JUnit Jupiter

## Source Structure

- `src/main/java/com/aionemu/boot/` — Spring Boot lifecycle, configuration, internationalization, and transport boundaries
- `src/main/java/com/aionemu/commons/` — Database, networking, concurrency, and shared infrastructure
- `src/main/java/com/aionemu/loginserver/` — Login service
- `src/main/java/com/aionemu/chatserver/` — Chat service
- `src/main/java/com/aionemu/gameserver/` — Game world, quests, AI, protocol, and business services
- `src/main/resources/aion/config/` — Runtime configuration
- `src/main/resources/aion/data/` — Production static data and quest XML
- `src/main/resources/aion/definitions/` — Compact definitions and generation inputs
- `src/main/resources/aion/geo/` — Geo, Path, and terrain data
- `src/test/java/` — Unit tests, production catalog checks, and regression tests
- `docs/` — Design, quest-repair, and maintenance documentation
- `scripts/` — Data generation, auditing, runtime helpers, and maintenance tools
- `aion/` — Local deployment directory; it is not source code or a long-lived build input

## Maven Module

- The repository root is the only Maven module: `com.aionemu:aionemu`.
- The Spring Boot repackaged artifact is `target/AionEmu.jar`.
- `package.sh` deploys the JAR, resources, and lifecycle scripts to `aion/` or the directory specified by `AION_HOME`.
- Run all Maven commands from the repository root.

## Rules Index

All detailed rules are in `.agent/rules/`:

| File | Scope | Description |
|---|---|---|
| [i18n.md](.agent/rules/i18n.md) | Entire repository | Bilingual comments, localized logging, and terminology rules / 中英双语注释、日志国际化和术语规范 |
| [java_general.md](.agent/rules/java_general.md) | `**/*.java` | General Java conventions, error handling, dependency injection, and development workflow / Java 通用约定、错误处理、依赖注入和开发流程 |
| [formatting.md](.agent/rules/formatting.md) | `**/*.java`, `**/*.xml` | Java and XML formatting, whitespace, wrapping, and generated-file boundaries / Java 与 XML 格式、空白、换行和生成文件边界 |
| [lombok.md](.agent/rules/lombok.md) | `**/*.java` | Lombok boilerplate reduction and generated-behavior boundaries / Lombok 样板代码简化及生成行为边界 |
| [quest-repair.md](.agent/rules/quest-repair.md) | Quest XML, quest engine, quest AI, quest tests, and `docs/quest/` | Quest evidence, repair, acceptance, and playbook-update rules / 任务证据、修复、验收和 Playbook 更新规则 |
