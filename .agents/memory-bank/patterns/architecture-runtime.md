# Architecture & Runtime Patterns (核心架构与运行时模式)

本文档记录 AionEmu 服务端生命周期、Spring 容器集成、启动性能与网络架构规范。

> Pattern IDs: `AR-001`–`AR-003`
> card_status: ACTIVE; performance claims require the referenced JFR or test evidence
> scope: Spring lifecycle, runtime service lookup, DAO provider wiring, and packet registration
> last_reviewed: 2026-09-14

---

## [AR-001] 一、Spring 容器与启动性能准则 (JFR 实战定论)
<!-- pattern-metadata
status: CONFIRMED
scope: Spring startup, static-data loading pools and high-frequency service facades
first_seen: 2026-08-23
last_verified: 2026-09-14
symptom: 启动慢、Spring 单例锁竞争、重复解析、热路径动态查 Bean
root_cause: XML parsing and container singleton locks dominate startup while uncached lookups add hot-path contention
fix_or_guardrail: Size pools for nested waits, share parsed resources and cache injected facade references
evidence: .agents/summary/startup-perf/2026-08-23-jfr-lock-contention-and-pool-tuning.md:31; startup JFR findings
validation: runtime JFR; performance claim requires the same workload and environment
boundaries: JFR conclusions are workload-specific; do not infer a universal optimal pool size
superseded_by: none
first_check: startup JFR, static-data pool, resource parse count and facade lookup sites
-->

1. **JFR 启动瓶颈实测定论**：
   - 使用 `-XX:StartFlightRecording` + `jcmd` 深入分析确认：生产启动耗时瓶颈在 **XML 静态数据解析的 CPU 密集工作量** 以及 **Spring 容器单例解析锁**。
   - **非瓶颈项**：类加载、GC 暂停与线程调度并非核心瓶颈。实测工作线程无脑翻倍会导致线程上下文切换增加，总耗时反而不降反升。

2. **多线程调优核心经验公式**：
   - **嵌套父任务池公式**：`STATIC_DATA_POOL` 核心大小必须为：`CPUs + 嵌套父任务数(5)`。若未考虑等待子任务的父任务，会导致“等待中的父线程挤占工作线程”，引发严重的线程饥饿与启动性能倒退。
   - **重型资源共享单次解析**：`npc-ai.xml`（27MB）曾存在双重重复解析，优化后由 `loadMappings` 返回 `NpcMappings(npcs, pathBehaviors)`，实现多消费者共享单次解析产物。
   - **并行刷怪与服务门面缓存**：刷怪加载改为按地图并行（`world-spawner` 池），并为高频 Spring 门面提供解析态缓存（`GameHousingServices`、`GameFeatureServices`、`GameLocationBootstrapServices`）。
   - **任务编译让路与事件分桶**：Quest 编译池从 8 调降至 3 为静态数据让出 CPU；`validateTransitionConflicts` 按事件类型分桶校验（利用 Sealed Interface 特性，跨事件类型的冲突直接判定为 false），使 typed 编译完成时间大幅提前。

3. **高频热路径严禁裸调 Spring 容器**：
   - 在封包分发、移动同步、AI 循环等高频服务门面热路径上：
   - **严禁裸调用 `applicationContext.getIfAvailable(...)` 或动态 Bean Lookup**。
   - 必须在服务启动引导或初始化注入阶段完成 Bean 引用缓存，坚决消除热路径锁竞争。

---

## [AR-002] 二、DAO 持久化与 Provider 架构（SPI 废除事实）
<!-- pattern-metadata
status: CONFIRMED
scope: DAO provider wiring and startup service registration
first_seen: 2026-09-09
last_verified: 2026-09-14
symptom: ServiceLoader 找不到 Provider、启动注册失败、静态扫描漏掉无扩展名服务文件
root_cause: Runtime SPI discovery hid provider dependencies and service registration files were easy to miss
fix_or_guardrail: Use explicit DAOClassProvider injection and compile-time provider wiring
evidence: src/main/java/com/aionemu/commons/database/dao/DAOManager.java:45; src/main/java/com/aionemu/loginserver/lifecycle/LoginStartupRuntimeBridge.java; src/main/java/com/aionemu/gameserver/lifecycle/GameUtilityServicesRuntimeBridge.java; commit eac35a48c
validation: static; focused startup or provider wiring test required per change
boundaries: Legacy bridges must remain lifecycle-safe until all static call sites are migrated
superseded_by: none
first_check: DAOManager provider parameter and startup bridge construction
-->

1. **彻底移除 Java SPI 机制**：
   - 项目已完全废除基于 `META-INF/services/*` 的 `ServiceLoader` SPI 动态发现机制。
   - 曾因服务注册文件无扩展名导致静态分析漏检误删，引发启动时崩溃。
2. **显式工厂与强类型注入**：
   - `DAOManager.init(DAOClassProvider)` 现采用显式传参注入。
   - 两个核心启动桥类（`LoginStartupRuntimeBridge` 与 `GameUtilityServicesRuntimeBridge`）直接 `new` 实例化对应的 Provider，其完整性完全交由 Java 编译器静态保障。

---

## [AR-003] 三、网络与封包调度
<!-- pattern-metadata
status: CONFIRMED
scope: Server and client packet opcode registration
first_seen: unknown
last_verified: 2026-09-14
symptom: 封包无响应、opcode 已实现但 handler 未触发、收发链路失败
root_cause: Packet classes without explicit opcode registration are not reachable by the dispatcher
fix_or_guardrail: Register packet class literals in ServerPacketsOpcodes or AionPacketHandler
evidence: src/main/java/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java; src/main/java/com/aionemu/gameserver/network/aion/AionPacketHandler.java
validation: static; focused packet test or runtime packet trace required per handler
boundaries: Registration proof does not validate packet payload compatibility or client opcode version
superseded_by: none
first_check: opcode map, handler registration and client protocol version
-->

1. **封包类字面量注册守则**：
   - 网络封包（Server Packet / Client Packet）必须在 `ServerPacketsOpcodes` 或 `AionPacketHandler` 中以类字面量显式注册映射关系。未完成注册的封包处理器即便实现了接口亦无法收发处理。
