# Architecture & Runtime Patterns (核心架构与运行时模式)

本文档记录 AionEmu 服务端生命周期、Spring 容器集成、启动性能与网络架构规范。

> Pattern IDs: `AR-001`–`AR-008`
> card_status: ACTIVE; performance claims require the referenced JFR or test evidence
> scope: Spring lifecycle, runtime service lookup, DAO provider wiring, and packet registration
> last_reviewed: 2026-09-14

---

## [AR-001] 一、Spring 容器与启动性能准则 (JFR 实战定论)
<!-- pattern-metadata
status: CONFIRMED
scope: Spring startup, static-data loading pools and high-frequency service facades
first_seen: 2026-08-23
last_verified: 2026-09-15
symptom: 启动慢、Spring 单例锁竞争、重复解析、热路径动态查 Bean
root_cause: XML parsing and container singleton locks dominate startup while uncached lookups add hot-path contention
fix_or_guardrail: Size pools for nested waits, share parsed resources and cache injected facade references
evidence: .agents/summary/startup-perf/2026-08-23-jfr-lock-contention-and-pool-tuning.md:31; .agents/summary/architecture-performance-refactor/2026-09-15-runtime-jfr-after-refactor.md; startup JFR findings
validation: runtime JFR; 2026-09-15 复测（commit de4a66e20）未再出现 DefaultSingletonBeanRegistry 竞争，GC 708ms/33 次；性能结论仍需同工作量对比
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

---

## [AR-004] 四、传输层缓冲区与连接生命周期契约
<!-- pattern-metadata
status: CONFIRMED
scope: Netty 入站读取、ConnectionTransport 适配器与进程内直连通道
first_seen: 2026-09-15
last_verified: 2026-09-15
symptom: 连接建立后立即断开、5 秒无限重连、"连接已丢失"/"游戏服务器已断开"、IndexOutOfBoundsException、ping/pong NPE、内嵌模式登录服侧始终无 CM_GS_AUTH 认证成功、登录服日志刷 "未实现类 BannedMacDAO/SvStatsDAO 的 DAO"
root_cause: Netty ByteBuf#readBytes(ByteBuffer) 按目标 remaining 读取而非调用方算出的可写长度；进程内直连通道绕过 initialized()/onDisconnect() 生命周期回调；直连投递在 writeData 之后重复 flip 导致整帧被丢弃，slice() 载荷视图默认大端序；直连交付未切到接收方 ServiceContext 导致对端在错误上下文运行
fix_or_guardrail: 读入 NIO buffer 前必须把 limit 收敛到 position()+writableBytes 并恢复；ConnectionTransport 适配器必须复刻 initialized() 与 onDisconnect()；writeData 返回后缓冲已是读模式禁止再 flip，载荷视图必须显式 order(LITTLE_ENDIAN)；跨服务交付必须切到接收方 serviceContext（清理回调同理）
evidence: src/main/java/com/aionemu/commons/network/NettyConnectionHandler.java:123; src/main/java/com/aionemu/commons/network/NettyConnectionHandler.java:131; src/main/java/com/aionemu/commons/network/NettyConnectionHandler.java:290; src/main/java/com/aionemu/gameserver/network/loginserver/DirectMemoryLoginChannel.java:93; src/main/java/com/aionemu/gameserver/network/loginserver/DirectMemoryLoginChannel.java:104; src/main/java/com/aionemu/gameserver/network/loginserver/DirectMemoryLoginChannel.java:126; src/main/java/com/aionemu/commons/database/dao/DAOManager.java:45; src/test/java/com/aionemu/gameserver/network/loginserver/DirectMemoryLoginChannelTest.java:20; src/test/java/com/aionemu/commons/network/NettyConnectionHandlerTest.java:22; .agents/summary/architecture-performance-refactor/2026-09-15-p0-p2-implementation.md
validation: focused-test + runtime; NettyConnectionHandlerTest 缺陷写法 2 例失败/修复后 5 例全通；DirectMemoryLoginChannelTest 缺陷写法 2 例失败/修复后 3 例全通；另有 166 个聚焦用例通过；真实客户端端到端通过（18:48 客户端登录 → 账号认证 → 进入世界，全程 0 ERROR/WARN）
boundaries: 不改变包头/长度帧/opcode 契约；直连通道仅在 AionRuntimeMode.isBootEmbedded() 下启用，外部 socket 路径未随本次改动重新验证分布式多机部署
superseded_by: none
first_check: NettyConnectionHandler.read 的 buffer 传递方式、ConnectionTransport 实现是否触发 initialized/onDisconnect
-->

1. **禁止把整个读缓冲直接交给 Netty 读取**：
   - `AbstractByteBuf#readBytes(ByteBuffer dst)` 的长度取 `dst.remaining()`，随后执行 `checkReadableBytes(length)`。
   - 若把 `connection.readBuffer`（16KB/64KB 容量）整体传入，任何小于剩余容量的小包（握手、认证包）都会抛 `IndexOutOfBoundsException` → `exceptionCaught` → `close(true)`，表现为“连上就掉、无限重连”。
   - 正确写法：`int readLimit = readBuffer.limit(); readBuffer.limit(readBuffer.position() + writableBytes); try { byteBuf.readBytes(readBuffer); } finally { readBuffer.limit(readLimit); }`，既零堆分配又不改变帧语义。

2. **进程内直连必须复刻传输层生命周期**：
   - socket 模式的 `initialized()` 由 Netty `channelActive` 触发；自建 `ConnectionTransport` 时必须显式调用它，否则连接状态、ping/pong 线程等初始化副作用全部丢失（`loginserver.server.pingpong=true` 时会在 `setState(AUTHED)` 对 `null` 排程）。
   - 关闭路径同样要走 `onDisconnect()`（直连实现里由 `DirectTransport.close` 统一触发），保证账号解绑与门面重连逻辑与 socket 模式一致。

3. **内存传输必须复刻 socket 的缓冲约定**：
   - `AConnection.writeData` 返回时缓冲已是**读模式**（`position=0`、`limit=帧长`），投递方禁止再 `flip()`；重复 `flip()` 会把 `limit` 归零，整帧被静默丢弃（包已从队列取出，等于双向丢包），且不会抛异常、不会打日志。
   - `ByteBuffer.slice()` 产出的视图是 **BIG_ENDIAN**，必须显式 `order(ByteOrder.LITTLE_ENDIAN)` 才能与 `NettyConnectionHandler.parse` / `AConnection.readBuffer` 的端序一致，否则 `readH/readD/readS` 全部错位。
   - 因此判断“内嵌直连是否真的通”不能只看 `正在连接登录服务器：in-memory`，要看对端业务日志（如登录服 `CM_GS_AUTH - 游戏服务器 #N 现已在线`）。

4. **跨服务投递必须切换 ServiceContext**：
   - `ServiceContext` 是 `InheritableThreadLocal`（默认 `default`），`RunnableWrapper` 在**提交任务时**捕获上下文，`DAOManager` 又按上下文分表（`login` / `game` / `chat` 各一份 DAO 注册表）。
   - 进程内直连时，对端的 `processData` 是在**调用方线程**上执行的；若交付时不切到接收方上下文，接收方状态机及其 `schedule(...)` 延迟任务会带着发送方上下文运行 → `DAOManager.getDAO(...)` 抛 `未实现类 X 的 DAO`（典型：`login` 专属的 `BannedMacDAO`、`SvStatsDAO` 在 `game` 上下文里找不到）。
   - 排查要点：`DAONotFoundException` 说明上下文本身已初始化（否则是 `IllegalStateException`），可直接用启动日志“已加载 N 个 DAO … 服务上下文：X”缩小到错误的上下文；关闭/断线回调同样要在各端自己的上下文中执行。

---

## [AR-005] 五、集合遍历契约：先取快照再遍历
<!-- pattern-metadata
status: CONFIRMED
scope: KnownList / MapRegion 等可见对象集合的遍历与“去分配”类性能优化
first_seen: 2026-09-15
last_verified: 2026-09-15
symptom: 移除快照遍历后出现遍历期间新增对象被访问；KnownListIterationSafetyTest / KnownListTest 失败
root_cause: 仓库把“遍历前取快照”作为契约（静态闸门 + 快照语义测试），ConcurrentHashMap 弱一致迭代器会立即访问遍历期间新增的条目
fix_or_guardrail: 容器可从 synchronizedMap 换成 ConcurrentHashMap 提升并发安全，但遍历已知/可见集合必须保留 synchronized(map) 快照；改遍历前先跑 KnownListIterationSafetyTest 与 KnownListTest
evidence: src/main/java/com/aionemu/gameserver/world/knownlist/KnownList.java:416; src/test/java/com/aionemu/gameserver/world/knownlist/KnownListIterationSafetyTest.java:27; src/test/java/com/aionemu/gameserver/world/knownlist/KnownListTest.java:78; .agents/summary/architecture-performance-refactor/2026-09-15-p0-p2-implementation.md
validation: focused-test + full-suite; 全量 3247 例中该 7 例已修复并全通，剩余 13 例经 HEAD 基线对照确认为既有失败
boundaries: MapRegion 内部扫描（getDoors/activateObjects/deactivateObjects/findVisibleObjects）不在此闸门覆盖范围，可按需保留免拷贝遍历
superseded_by: none
first_check: KnownListIterationSafetyTest 的正则闸门、KnownListTest 的快照语义用例
-->

1. **性能优化不能取消快照语义**：
   - `knownObjects` / `knownPlayers` / `visualObjects` / `visualPlayers` 的遍历必须经 `knownObjectsSnapshot()` / `knownPlayersSnapshot()` / `getVisibleObjectsSnapshot()`；这些助手内部 `synchronized (map)` 复制一份。
   - 换成 `ConcurrentHashMap` 只解决“并发读写不损坏结构”，不解决“遍历期间新增对象是否应被访问”——后者是本仓库明确要求的快照语义。
   - `KnownListIterationSafetyTest` 是**源码级正则闸门**：任何 `for (... : knownObjects.values())` / `getKnownObjects().values()` 形式都会让它失败。

---

## [AR-006] 六、原始类型容器陷阱（热路径装箱）
<!-- pattern-metadata
status: CONFIRMED
scope: 所有运行期热路径上的 int/long 键容器选型
first_seen: 2026-09-15
last_verified: 2026-09-15
symptom: 游戏内 JFR 显示 Integer/Long 装箱占分配 74%，单站点 RealGeoData.getMap 占 41.6% 分配 + 11.35% CPU
root_cause: com.aionemu.commons.utils.collections.IntObjectHashMap 直接 extends LinkedHashMap<Integer,V>，每次 get(int)/put(int,V) 都自动装箱
fix_or_guardrail: 热路径禁用 IntObjectHashMap（名字像 primitive map，实为装箱 LinkedHashMap）；改用 LongObjectHashMap（原始 long 键，int 自动宽化）或升序 int 数组 + 二分
evidence: src/main/java/com/aionemu/commons/utils/collections/IntObjectHashMap.java:13; src/main/java/com/aionemu/commons/utils/collections/LongObjectHashMap.java; src/main/java/com/aionemu/gameserver/world/geo/RealGeoData.java:185; src/main/java/com/aionemu/gameserver/world/geo/path/PathData.java:1520; .agents/summary/architecture-performance-refactor/2026-09-15-gameplay-jfr-hotspots.md
validation: runtime JFR（300s 真实游戏内）+ focused-test（172 例含 PathDataTest/PathServiceConcurrencyTest/NpcMoveControllerPathTest/LongObjectHashMapTest/RealGeoDataLookupTest）；运行期收益待重启后复测
boundaries: 87 个使用点中多数为加载期数据表，本次只修了 RealGeoData 与 PathData 两处热点；采样权重仅用于相对排序
superseded_by: none
first_check: RealGeoData.getMap、PathData.visited、任何 Map<Long,...>/Map<Integer,...> 的逐节点 put/get
-->

1. **名字带 Int 不等于无装箱**：`IntObjectHashMap<V> extends LinkedHashMap<Integer, V>` 只提供 `contains(int)`/`keys()` 便利方法，键仍是 `Integer`；worldId 等大值每次查表都会新建 `Integer`。
2. **实测代价**：300s 游戏内窗口内，`RealGeoData.getMap(int)`（仅一行 `geoMaps.get(worldId)`）独占采样分配 1440.9MB / 41.6% 与 11.35% CPU；A* 的 `Map<Long, SearchNode> visited` 再占 34.9%（`Long.valueOf`）。
3. **正确姿势**：先在调用点做 last-lookup 缓存（worldId 在单线程内长期稳定），再考虑自写开放寻址 primitive 容器；引入 fastutil 等依赖需团队评估。

---

## [AR-007] 七、不可变 Map 迭代与实例级遍历的分配陷阱
<!-- pattern-metadata
status: CONFIRMED
scope: 热路径上的 Map.copyOf/Map.of 结果迭代、WorldMapInstance 级整表遍历
first_seen: 2026-09-15
last_verified: 2026-09-15
symptom: 游戏内 JFR（300s）：Object[] 占采样分配 39.6%（其中 WorldMapInstance.getNpcs 单站点 207MB）、KeyValueHolder 占 12.5% 且全部来自 QuestSnapshot 的 withXxx 校验链
root_cause: (1) ImmutableCollections.MapN 的 entrySet 迭代器每次 next() 都新建 KeyValueHolder；QuestSnapshot 紧凑构造器对 inventory/currencies/eventActivities 反复做 entrySet().stream().anyMatch 校验，而每个 withXxx 都会重走一遍；(2) WorldMapInstance.getNpcs() 先复制整表快照、再扩容收集结果，broadcast 路径只遍历一次却物化两份列表
fix_or_guardrail: (1) 减少遍历次数才是消除 KeyValueHolder 的正解——MapN 未覆写 keySet()/forEach，任一遍历都每条目分配；落地手法：迁移到不可变副本时用 `Set.copyOf(x) == x` / `Map.copyOf(x) == x` 识别“已校验过的不可变入参”并跳过逐条目校验（首建仍从可变集合完整校验一次）；(2) 实例级遍历提供 doOnAllNpcs(Visitor)（锁内取一次数组快照、锁外访问）；getNpcs()/getPlayersInside() 改为锁内单次预分配收集
evidence: src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestSnapshot.java:366; src/main/java/com/aionemu/gameserver/world/WorldMapInstance.java:390; src/main/java/com/aionemu/gameserver/ai/RetailPatternAI2.java:1641; src/main/java/com/aionemu/gameserver/questEngine/runtime/PlayerQuestEventPort.java:270; .agents/summary/architecture-performance-refactor/2026-09-15-allocation-and-null-gate.md
validation: focused-test（175 例）+ 两轮 300s 运行期 JFR：第二轮 getNpcs 207.2MB→0、worldMapObjectsSnapshot 19.9MB→0；第三轮（校验一次化后）KeyValueHolder 85.9MB→1.66MB 且 withXxx 重入路径归零、QuestSnapshot 相关 116.6MB→11.7MB
boundaries: 采样权重仅用于相对排序；WorldMapInstance 仍保留 LinkedHashMap 的插入序，未改 ConcurrentHashMap（会改变 getNpc 的“先到先得”语义）
superseded_by: none
first_check: Map.of/Map.copyOf 结果上的 entrySet()/forEach/stream；实例级 getNpcs()/getPlayersInside() 的新增调用点
-->

1. **不可变小 Map 的迭代是要花钱的**：`Map.of(k,v)`、`Map.copyOf(...)` 产生 `ImmutableCollections$MapN`。它只覆写 `entrySet()`，**没有覆写 `keySet()`、`forEach()` 或 `values()`**，因此 `entrySet()`、`keySet()`、`forEach()` 三条路径都会经由 `AbstractMap$KeyIterator`/`MapNIterator` 为每个条目 `new KeyValueHolder`（2026-09-15 复测 JFR 的调用链证实：`AbstractMap$KeyIterator.next() → MapN$MapNIterator.next()`）。改 `entrySet().stream()` 为 `keySet()` 循环**不能**消除该分配，只能省掉 stream 管道对象；真正的修法是减少遍历次数（例如把逐次校验合并为一次）。
2. **“复制 + 校验”每一步都做**：`QuestSnapshot` 的紧凑构造器对 `inventory`/`currencies`/`eventActivities` 逐个校验，而每个 `withXxx` 都要走一次构造器。`Map.copyOf` 对已是不可变的入参是**零成本**（返回同一实例），剩余成本是校验时的逐条目遍历。复测显示该站点从 84.9MB 降到 27.1MB（同一路径上的 `getAllFinishedQuests` 中间列表与结果集预分配也被移除），但**没有归零**——待做“校验一次”的设计改造。
3. **广播不要物化整表**：`WorldMapInstance.getNpcs()` 被 `broadcast_message` AI 动作按次调用，先 `new ArrayList<>(values)` 再收集 NPC，两次物化合计 227MB/300s。改为 `doOnAllNpcs(Visitor)`（锁内一次数组快照、锁外访问）后只剩单次快照，且保持原有“快照语义”（对应 AR-004）。
4. **判空也是分配问题**：`Set.copyOf(...).stream().anyMatch(...)` 与 `entrySet().stream().anyMatch(...)` 在每次快照构造时创建管道对象；`for` 循环同义且无分配。


---

## [AR-008] 八、模板字符串在热路径重复解析
<!-- pattern-metadata
status: CONFIRMED
scope: 由 XML/静态数据注入、随后在运行期被反复读取的字符串字段
first_seen: 2026-09-15
last_verified: 2026-09-15
symptom: 游戏内 JFR（300s）：TemporarySpawn.getTime 单站点占采样分配 52.4MB / 17.2%（另见同源 String[] 2.83%）
root_cause: TemporarySpawn 把 XML 注入的 "时.日.月" 字符串留到每次读取时用 String.split("\\.") 拆分；"." 不是 String.split 的单字符快路径，每次调用都会走正则匹配并分配 String[]，而 isInSpawnTime() 一次要调用 6 个取值器
fix_or_guardrail: 加载期或首次使用时解析一次并缓存（TemporarySpawn 用 volatile Integer[3] 缓存，模板加载后不再变化）；热路径禁止对同一常量字符串重复 split/正则/格式化
evidence: src/main/java/com/aionemu/gameserver/model/templates/spawns/TemporarySpawn.java:26; src/test/java/com/aionemu/gameserver/model/templates/spawns/TemporarySpawnTimeWindowTest.java; .agents/summary/architecture-performance-refactor/2026-09-15-gameplay-jfr-hotspots.md
validation: focused-test（TemporarySpawnTimeWindowTest 4 例 + TemporarySpawnEngineTest + RetailOpenWorldSpawnDataTest）+ 运行期 JFR 复测（2026-09-15 23:36 300s）：TemporarySpawn.getTime 站点 52.4MB→0（我方首帧不再出现）
boundaries: 缓存后模板字段不再可变（无生产代码写这些字段）；解析失败的语义与旧实现一致（段数不足仍抛数组越界）
superseded_by: none
first_check: 任何在每次事件里 String.split/String.format/Pattern.compile 的模板读取点
-->

1. **`String.split("\\.")` 不走快路径**：JDK 只对“单字符且非正则元字符”的分隔符做快路径；`.` 是元字符，因此每次调用都会做正则匹配并分配 `String[]`。
2. **调用次数被放大 6 倍**：`TemporarySpawn.isInSpawnTime()` 依次取时/日/月 × 刷新/消失共 6 次，每次都要重新拆分同一个字符串。
3. **正确姿势**：字段注入后只解析一次（首次使用懒解析 + `volatile` 缓存即可，无需改写 JAXB 绑定）；缓存的前提是模板加载后不再被修改——本仓库的 `TemporarySpawn` 只有 `@Getter`，无写入方。
