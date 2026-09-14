# Architecture & Runtime Patterns (核心架构与运行时模式)

本文档记录 AionEmu 服务端生命周期、Spring 容器集成、启动性能与网络架构规范。

> Pattern IDs: `AR-001`–`AR-003`
> card_status: ACTIVE; performance claims require the referenced JFR or test evidence
> scope: Spring lifecycle, runtime service lookup, DAO provider wiring, and packet registration
> last_reviewed: 2026-09-14

---

## 一、Spring 容器与启动性能准则 (JFR 实战定论) (`AR-001`)

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

## 二、DAO 持久化与 Provider 架构（SPI 废除事实） (`AR-002`)

1. **彻底移除 Java SPI 机制**：
   - 项目已完全废除基于 `META-INF/services/*` 的 `ServiceLoader` SPI 动态发现机制。
   - 曾因服务注册文件无扩展名导致静态分析漏检误删，引发启动时崩溃。
2. **显式工厂与强类型注入**：
   - `DAOManager.init(DAOClassProvider)` 现采用显式传参注入。
   - 两个核心启动桥类（`LoginStartupRuntimeBridge` 与 `GameUtilityServicesRuntimeBridge`）直接 `new` 实例化对应的 Provider，其完整性完全交由 Java 编译器静态保障。

---

## 三、网络与封包调度 (`AR-003`)

1. **封包类字面量注册守则**：
   - 网络封包（Server Packet / Client Packet）必须在 `ServerPacketsOpcodes` 或 `AionPacketHandler` 中以类字面量显式注册映射关系。未完成注册的封包处理器即便实现了接口亦无法收发处理。
