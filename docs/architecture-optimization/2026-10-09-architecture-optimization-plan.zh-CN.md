# AionEmu 架构持续演进与优化规划全景 (2026-10-09)

## 一、架构现状与背景

AionEmu 当前采用单体 Spring Boot 4.1 承载 Login、Game、Chat 服务，运行于 Java 25 基线上。
经过前期的架构迁移与性能调优（P0-P2 改造、Spring 启动链收拢、JFR 内存与装箱热点消除），系统启动与基础运行已大幅改善。

然而，由于历史代码最初基于传统单体 Java 游戏服（深度依赖静态单例、全局静态配置、原始 JDBC 静态 DAO 及手工线程同步），在逐步迁移至 Spring IoC 容器的过程中，系统沉淀了大量用于兼容旧调用点的**“过渡期脚手架（Bridging Scaffolding）”**。同时，底层在并发调度、数据持久化、配置模型与数据包分发上仍存在架构债务。

为使系统彻底摆脱历史包袱，迈向现代化、高内聚、易测试、高性能的架构体系，制定以下四阶段演进规划。

---

## 二、四大演进阶段路线图 (Evolution Roadmap)

```text
[Phase 1: 低风险代码减负与死代码清理]
  ├── 清理源码树非代码二进制文件 (GMMode_.rar)
  ├── 移除 GameServer.java 中 25 个已退役的静态 start(...) 级联重载 (~1100 行)
  └── 梳理 lifecycle 层的 Fallbacks 与 Bridge 依赖契约，制定平滑退役边界
       │
       ▼
[Phase 2: 内存与装箱热点根治 (数据结构优化)]
  ├── 治理全库 257 处 IntObjectHashMap 伪原始集合（替换为真·原始类型映射）
  └── 重构 AionPacketHandler 废除 super.clone() 原型模式，改用函数式工厂
       │
       ▼
[Phase 3: 并发与存盘管线重构 (调度解耦)]
  ├── 玩家个人定期定时器收拢为批量异步脏标记存盘流水线 (Dirty Flush Worker)
  ├── 游戏主逻辑 Tick 调度与 JDBC 阻塞 IO 彻底物理隔离
  └── 解耦 AionConnection 单全局锁 PacketProcessor，打散为连接亲和队列
       │
       ▼
[Phase 4: 现代配置与依赖注入彻底收口 (架构规范终态)]
  ├── 50+ 个 @Property 静态配置类全面迁移为 Spring @ConfigurationProperties (结合 record / Validation)
  └── 515 处 DAOManager.getDAO 查表消除，Service 与 DAO 全面转为构造器注入 (Constructor Injection)
```

---

## 三、各阶段详细设计与实施路径

### Phase 1: 低风险代码减负与死代码清理
- **目标**：不改变任何运行时业务逻辑，剥离完全不再使用的历史代码、减负主类与依赖层。
- **任务清单**：
  1. `src/main/java/com/aionemu/gameserver/world/GMMode_.rar`：从版本控制与工作树中彻底删除。
  2. `src/main/java/com/aionemu/gameserver/GameServer.java`：
     - 移除从行 220 至 1341 的 25 个 `@Deprecated` 静态 `start(...)` 级联方法；
     - 清理该类因手动 `new` 生命周期对象而引入的数十个未使用的 `Game*Lifecycle`、`Game*Gateway` 导入包；
     - 仅保留 `activateServer`、`attachNetworkLifecycle`、`initializeLogger`、`startServers`、`stop` 等核心实例/网络生命周期方法。
  3. **Fallbacks 契约梳理与瘦身**：
     - 审计 11 个 `Game*Fallbacks` 类及其在测试用例中的硬断言（如 `Files.readString`）；
     - 为后续消除 Bridge / Fallbacks 中间层提供测试兼容方案。

### Phase 2: 内存与装箱热点根治
- **目标**：消除高频热路径上的隐式堆分配与对象复制，降低年轻代 GC 压力。
- **任务清单**：
  1. **`IntObjectHashMap` 治理**：
     - 当前 `IntObjectHashMap<V>` 直接继承 `LinkedHashMap<Integer, V>`，每次访问触发 `Integer.valueOf()` 装箱并分配双向链表 Entry；
     - 替换为基于开放寻址的真原始映射（如项目内的 `LongObjectHashMap` 或引入高性能扁平映射），密集只读表转换为有序数组二分查找。
  2. **封包实例化现代化**：
     - 将 `AionPacketHandler` 的 `AionClientPacket.clonePacket()` 替换为基于操作码数组索引的 `IntFunction<AionClientPacket>` 构造器引用。

### Phase 3: 并发与存盘管线重构
- **目标**：提升服务器在线承载能力，防止数据库 IO 抖动波及游戏逻辑。
- **任务清单**：
  1. **异步脏标记存盘流水线**：
     - 取消玩家登录时向 `scheduledPool` 挂载独立 `GeneralUpdateTask` 和 `ItemUpdateTask` 的做法；
     - 建立脏实体变更队列，后台线程池批量聚合执行持久化（Batch SQL）。
  2. **网络包调度隔离**：
     - 消除全局单一锁的 `PacketProcessor`，改用连接维度串行队列或 Netty EventLoop 原生调度。

### Phase 4: 现代配置与依赖注入收口
- **目标**：实现纯粹、类型安全、不可变的现代 Spring Boot 体系。
- **任务清单**：
  1. **外部化配置迁移**：
     - `@Property` + `public static` 字段全面收口为 Spring Boot `@ConfigurationProperties`；
     - 结合不可变数据载体与 JSR-380 参数合法性校验。
  2. **依赖注入收尾**：
     - 淘汰 `GameCoreGameplayServices` 等静态服务定位器；
     - 消除 `DAOManager.getDAO`，将持久层与服务层全面重构为显式构造器注入。

---

## 四、当前阶段：Phase 1 执行追踪与 Phase 2 演进

- [x] 1. 编写架构规划全景文档
- [x] 2. 删除 `src/main/java/com/aionemu/gameserver/world/GMMode_.rar`
- [x] 3. 重构 `GameServer.java`：删除 25 个静态 `start(...)` 重载并清理冗余 import（依审慎反射原则完整保留 `getRatiosFor`/`getCountFor`）
- [x] 4. 检查并验证修改影响与编译/代码规范，契约测试 100% 通过
- [x] 5. 优化 `AionPacketHandler.java`：引入直接数组寻址，消除高频入站包装箱开销，同时 100% 保持反射访问兼容
- [x] 6. 重构 `IntObjectHashMap.java`：实现真·原始类型开地址哈希表，消除全库 257 处伪原始集合的装箱与 Entry 分配开销，完整保持 Map 接口兼容并通过 15 项专项单元测试验证
