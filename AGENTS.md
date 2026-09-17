# AGENTS.md

This file provides project-level guidance for AI coding agents working in this repository.

> **Detailed rules are split into focused files under `.agents/rules/`.** Read the applicable files from the [Rules Index](#rules-index) for the task at hand.

## Global Rules

1. Do NOT run build commands unless explicitly requested by the user. When verification genuinely requires tests or a build, ask the user for authorization first and state the exact command and scope; without authorization keep the work `PENDING` and record the commands that were not executed.
2. Do NOT start, stop, or restart server processes. Their current state is unknown, and the user manages their lifecycle.
3. `.agents/summary/` is the canonical directory for all summary files.
4. Do NOT use, recreate, or reference `.agent/` or `.agent/summary/`. All agent rules, memory bank, and summary directories reside strictly under `.agents/`.
5. AI-generated intermediate artifacts, including temporary scripts, must be stored in `.agents/summary/<topic>/`; do not place them under `scripts/`.
6. Worktree discipline (worktree 使用纪律): do not create a git worktree unless the main working tree cannot be used for the required verification (for example, a parallel task makes the production catalog fail to build). Keep any worktree under a temporary path outside the repository, use it only for that verification, never commit from it, and remove it with `git worktree remove --force <path>` followed by `git worktree prune` as soon as the verification finishes. Never leave a worktree, its build output, or a temporary checkout behind.

## Memory Bank

Persistent architecture patterns and debugging insights are maintained in [.agents/memory-bank/](.agents/memory-bank/):
- **Read before action**: When diagnosing bugs or touching core systems, read [.agents/memory-bank/systemPatterns.md](.agents/memory-bank/systemPatterns.md) first to avoid known pitfalls.
- **Active focus**: Check [.agents/memory-bank/activeContext.md](.agents/memory-bank/activeContext.md) for current focus areas across sessions.
- **Automatic Wrap-up Protocol (自动沉淀协议)**:
  After resolving a non-trivial bug, runtime anomaly, or subtle architectural issue, first leave task-specific evidence in `.agents/summary/<topic>/`. Only promote a finding to `patterns/` when it is reusable; update [.agents/memory-bank/systemPatterns.md](.agents/memory-bank/systemPatterns.md) only for a new cross-domain invariant. After updating Pattern metadata, run `python3 .agents/memory-bank/sync_memory_bank.py` and `python3 .agents/memory-bank/check_memory_bank.py`; when this turn actually updates memory-bank content, append `[Memory Bank Auto-Updated]` as a receipt at the end of the response.
- **Document Co-Commit (沉淀文档随提交)**:
  A commit is still created only after explicit user authorization. Once authorized, automatically review and explicitly stage the retained summary, memory-bank, Playbook, and acceptance documents produced by the current task together with the related source changes. Do not use `git add -A`; do not stage raw intermediate artifacts, ignored runtime outputs, or unrelated dirty files. If a document contains unrelated hunks, stage only the task-owned hunks or leave that file uncommitted.

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

All detailed rules are in `.agents/rules/`:

| File                                             | Scope | Description |
|--------------------------------------------------|---|---|
| [i18n.md](.agents/rules/i18n.md)                 | Entire repository | Bilingual comments, localized logging, and terminology rules / 中英双语注释、日志国际化和术语规范 |
| [java_general.md](.agents/rules/java_general.md) | `**/*.java` | General Java conventions, error handling, dependency injection, and development workflow / Java 通用约定、错误处理、依赖注入和开发流程 |
| [backend.md](.agents/rules/backend.md)           | `src/main/java/**/*.java` | Backend implementation and JDK 25 language/API conventions / 后端实现与 JDK 25 语言/API 约定 |
| [formatting.md](.agents/rules/formatting.md)     | `**/*.java`, `**/*.xml` | Java and XML formatting, whitespace, wrapping, and generated-file boundaries / Java 与 XML 格式、空白、换行和生成文件边界 |
| [lombok.md](.agents/rules/lombok.md)             | `**/*.java` | Lombok boilerplate reduction and generated-behavior boundaries / Lombok 样板代码简化及生成行为边界 |
| [ai-artifacts.md](.agents/rules/ai-artifacts.md) | Entire repository | AI-generated intermediate artifacts, topic directories, and script placement / AI 生成中间产物、主题目录和脚本存放规则 |
| [quest-repair.md](.agents/rules/quest-repair.md) | Quest XML, quest engine, quest AI, quest tests, and `docs/quest/` | Quest evidence, repair, acceptance, and playbook-update rules / 任务证据、修复、验收和 Playbook 更新规则 |
| [memory-bank/](.agents/memory-bank/README.md) | Entire repository | Persistent architecture patterns, gotchas, and troubleshooting router / 持久化架构模式、避坑指南与排查路由器 |
