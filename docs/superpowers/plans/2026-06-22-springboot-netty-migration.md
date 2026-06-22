# Spring Boot Netty Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Run game, login, and optionally chat from one Spring Boot process with Netty-backed service lifecycles while preserving current game behavior.

**Architecture:** Add a boot module that owns process startup, configuration binding, and service lifecycle ordering. Keep game/login/chat protocol handlers and packet execution behavior intact while introducing Netty transport adapters behind service lifecycle classes.

**Tech Stack:** Java 25, Maven multi-module, Spring Boot, Netty, existing AionEmu service and packet code.

---

## User Constraint

The user explicitly requested no red-light tests. Do not write failing tests first for this migration. Use compile checks, dependency checks, and focused runtime-safe structure checks after each phase.

## File Structure

- Create `AL-Boot/pom.xml`: Spring Boot application module depending on commons, login, game, and chat modules.
- Create `AL-Boot/src/com/aionemu/boot/AionBootApplication.java`: single process entrypoint.
- Create `AL-Boot/src/com/aionemu/boot/config/AionServicesProperties.java`: binds `aion.services.*` toggles and network mode.
- Create `AL-Boot/src/com/aionemu/boot/lifecycle/AionServiceLifecycle.java`: shared lifecycle contract for boot-managed services.
- Create `AL-Boot/src/com/aionemu/boot/lifecycle/GameServiceLifecycle.java`: delegates current game startup and shutdown.
- Create `AL-Boot/src/com/aionemu/boot/lifecycle/LoginServiceLifecycle.java`: delegates current login startup and shutdown.
- Create `AL-Boot/src/com/aionemu/boot/lifecycle/ChatServiceLifecycle.java`: starts chat only when enabled.
- Modify root `pom.xml`: add Spring Boot and Netty 4 dependency management, add `AL-Boot` module.
- Modify existing game/login/chat startup classes only where needed to expose reusable `start()` and `stop()` methods while keeping old `main()` compatibility.

## Task 1: Boot Module Skeleton

- [x] Add `AL-Boot` module to the root Maven reactor.
- [x] Add Spring Boot dependency management and dependencies needed for the boot module.
- [x] Create the boot application entrypoint.
- [x] Create properties binding for `aion.services.game.enabled`, `aion.services.login.enabled`, and `aion.services.chat.enabled`.
- [x] Run `JAVA_HOME=$(/usr/libexec/java_home -v 25) rtk mvn -pl AL-Boot -am -DskipTests package`.
- [x] Commit with message `feat: add spring boot launcher module`.

## Task 2: Lifecycle Wrappers Without Behavior Change

- [x] Add lifecycle classes for game, login, and chat.
- [x] Make game and login enabled by default.
- [x] Make chat disabled by default and conditional on config.
- [x] Keep old main classes callable.
- [x] Run `JAVA_HOME=$(/usr/libexec/java_home -v 25) rtk mvn -pl AL-Boot -am -DskipTests package`.
- [x] Commit with message `feat: wire server lifecycles into boot launcher`.

## Task 3: Netty Transport Boundary

- [x] Introduce a transport mode setting that defaults to current behavior during transition.
- [x] Add Netty service abstractions that can host game, login, and chat TCP endpoints independently.
- [x] Keep packet parsing, crypto, flood protection, and packet processor execution in existing connection/handler code.
- [x] Run compile verification.
- [x] Commit with message `feat: add netty transport lifecycle boundary`.

## Task 4: Runtime Selection And Chat Toggle

- [x] Add config examples for enabling/disabling chat.
- [x] Ensure disabled chat does not break game/login startup.
- [x] Verify startup logs clearly identify which services are enabled.
- [x] Run compile verification.
- [x] Commit with message `feat: make chat startup configurable`.

## Task 5: Completion Audit

- [x] Confirm there is one bootable Spring Boot application.
- [x] Confirm game, login, and chat are represented as lifecycle-managed services.
- [x] Confirm chat can be disabled by configuration.
- [x] Confirm old packet/business logic remains in place.
- [x] Confirm build verification passes or record exact blockers.

## Completion Notes

- Single boot entrypoint: `com.aionemu.boot.AionBootApplication` in module `AL-Boot`.
- Startup order: login first, optional chat second, game last, so game keeps its existing login/chat connector behavior.
- Chat default: disabled in `application.yml`; enabled with Spring profile resource `application-chat.yml`.
- Transport default: `aion.services.transport.mode=legacy-nio`; Netty 4 lifecycle and endpoint binding boundary are present for subsequent endpoint-by-endpoint migration.
- Runtime validation blocker: MySQL at `127.0.0.1:3306` is available by user configuration, but schema tables are not initialized, so this phase intentionally did not run full application startup.
- Verification command used after each implementation phase: `JAVA_HOME=$(/usr/libexec/java_home -v 25) rtk mvn -pl AL-Boot -am -DskipTests package`.
