# P0-P2 架构与性能改造实施记录

## Scope

- P0: `PlayerDAO` 公共数据缓存切换为 `ConcurrentHashMap`；核心玩法门面与 `ThreadPoolManager` 增加解析结果缓存，避免热路径穿透 Spring singleton registry。
- P1: `KnownList` / `MapRegion` 核心容器切换为 `ConcurrentHashMap`；**广播/遍历路径仍走快照**（容器变更一度改成弱一致迭代，被仓库既有契约测试 `KnownListIterationSafetyTest` 与 `KnownListTest` 判定为回归，已还原为 `synchronized(map)` 快照语义，仅保留线程安全容器与 `MapRegion` 内部扫描的免拷贝遍历）。Netty 入站改为直接读取到 NIO buffer，避免中间 `byte[]`；`PacketProcessor` 增加队首快速路径并在连接解锁后唤醒等待 worker。
- P2: 攻城伤害/BOSS 死亡、队伍/联盟变动、AP/GP 扩展点改为强类型监听器；删除 Javassist enhancer/weaver、编译期织入插件和启动织入调用；内嵌模式 GameServer/LoginServer 通信增加 `DirectMemoryLoginChannel`，外部 TCP 路径保留 `SocketLoginMessageDispatcher`。
- 补充：`WorldPosition.getInstanceId()` 对未绑定 `MapRegion` 的测试/未生成位置返回 `0`，修复既有跟随轨迹测试在无地图区域夹具时的 NPE。

## Regression fixes (2026-09-15 追加)

### R1. Netty 入站读取引入的连接闪断（P1 回归，已修复）

- 症状：服务端日志出现持续重连循环（`正在连接聊天服务器：/127.0.0.1:9021` → `已连接聊天服务器` → `与聊天服务器的连接已丢失` / `游戏服务器 #0 已断开`），间隔 5s。
- 根因：`NettyConnectionHandler.read()` 将中间 `byte[]` 去掉后改为 `byteBuf.readBytes(readBuffer)`。Netty 的 `AbstractByteBuf#readBytes(ByteBuffer dst)` 实现为 `int length = dst.remaining(); checkReadableBytes(length); ...`，即按**目标缓冲剩余容量**读取，而不是按调用方算出的 `writableBytes`。
  - `GameServer↔LoginServer` 读缓冲 64KB、`GameServer↔ChatServer` 读缓冲 16KB，而握手/认证包只有十几到几十字节，`checkReadableBytes` 必然抛 `IndexOutOfBoundsException`。
  - 异常进入 `exceptionCaught` → `close(true)` → 对端收到 `channelInactive` → `onDisconnect` → 双方日志“连接已丢失”并进入重连循环。
- 修复：保持零堆分配原地读取，但把 `readBuffer` 的 `limit` 临时收敛到 `position() + writableBytes`，读取后在 `finally` 中恢复。
- 对照证据：临时还原缺陷写法执行 `mvn test -Dtest=NettyConnectionHandlerTest` → `Tests run: 5, Failures: 1, Errors: 1`（`ClosedChannelException` / 读缓冲计数不符）；恢复修复后同命令 `Tests run: 5, Failures: 0, Errors: 0`。

### R2. 内嵌直连通道缺少登录侧传输初始化（P2 缺陷，已修复）

- `DirectMemoryLoginChannel.open()` 只调用游戏侧 `initialized()`（发送 `SM_GS_AUTH`），未调用登录侧 `GsConnection.initialized()`。
- 影响：登录侧 `state` 未置 `CONNECTED`、`pingThread` 未创建；而随仓库默认配置 `loginserver.server.pingpong = true`，`CM_GS_AUTH` 成功后会走 `setState(AUTHED)` → `schedule(pingThread, 5000)`，对 `null` 排程。
- 修复：`open()` 在 `login` 服务上下文中先执行登录侧 `initialized()`，再在 `game` 上下文触发认证，与 socket 模式“先 accept 初始化、再收 `SM_GS_AUTH`”的时序一致。

### R3. 队伍/联盟解散顺序回归（P2 重构缺陷，已修复）

- 原 `@GlobalCallback` 语义是 `beforeCall → 方法体（Preconditions → 容器 remove → onEvent）→ afterCall`。
- 重构后一度写成 `notify(before) → onEvent → remove`，使 `GroupDisbandEvent` / `AllianceDisbandEvent` 派发期间仍能从静态容器查到该队伍/联盟。
- 修复：恢复为 `notify(before) → remove → onEvent → notify(after)`（`Preconditions` 仍先于 before 通知，避免校验失败时误删寻找队伍条目）。

### R4. 进程内直连通道整帧丢失 + 载荷端序错误（P2 阻断性缺陷，已修复）

- 症状：内嵌模式下 `正在连接登录服务器：in-memory` 之后，登录侧**从未**出现 `c.a.l.n.g.clientpackets.CM_GS_AUTH - 游戏服务器 #1 现已在线`。`log/console.log` 中该日志最后一次出现是 09-15 13:15（socket 模式），此后 14:41/14:44/16:22/18:19 四次 in-memory 启动全部没有 → 游戏服实际未在登录服完成认证，`LoginServer.sendPacket(...)` 因 `isAuthed()==false` 全部失败。
- 根因一（整帧被丢弃）：`AConnection.writeData` 返回时写出缓冲**已经是读模式**（`LsServerPacket.write` 结尾为 `flip → putShort(帧长) → position(0)`，即 `position=0 / limit=帧长`）。
  - `DirectTransport.enableWriteInterest()` 又执行了一次 `buffer.flip()`，把 `limit` 归零 → `buffer.hasRemaining()` 为 false → 帧内容被静默跳过；而包已被 `writeData` 从队列 `pollFirst()` 取出，等于**双向全部丢包**。
- 根因二（载荷端序错误）：`ByteBuffer.slice()` 返回的视图是 **BIG_ENDIAN**，而 socket 路径的读缓冲是 `LITTLE_ENDIAN`（`AConnection.readBuffer.order(LITTLE_ENDIAN)`，且 `NettyConnectionHandler.parse` 显式 `packetBuffer.order(ByteOrder.LITTLE_ENDIAN)`）。即使补上发送，登录侧解析 `port`/`maxPlayers`/`password` 等多字节字段也会错位并认证失败。
- 修复：抽出 `DirectMemoryLoginChannel.pumpWrites(source, target)`，与 `NettyConnectionHandler.parse` 对齐——**不再 flip**，`slice()` 后显式 `order(ByteOrder.LITTLE_ENDIAN)`。
- 对照证据：新增 `DirectMemoryLoginChannelTest`；临时还原缺陷写法 `mvn test -Dtest=DirectMemoryLoginChannelTest` → `Tests run: 2, Failures: 2`（`expected: <1> but was: <0>` / `expected: <2> but was: <0>`）；恢复修复后 → `Tests run: 2, Failures: 0`。

### R5. 直连通道跨服务上下文串味（P2 阻断性缺陷，已修复）

- 症状：R4 修复后登录认证已成功（`c.a.l.n.g.clientpackets.CM_GS_AUTH - 游戏服务器 #1 现已在线`），但登录服侧随即刷错误：
  - 认证后 500ms：`未实现类 BannedMacDAO 的 DAO`（`SM_MACBAN_LIST` → `LoginProtectionServices.bannedMacManager()` → Spring 工厂方法抛 `BeanCreationException`）；
  - 每 3 秒：`未实现类 SvStatsDAO 的 DAO` + `心跳线程 #1 发生异常`（`PingPongThread`）。
- 根因：`ServiceContext` 是 `InheritableThreadLocal`（默认 `default`），且 `RunnableWrapper` 在**提交任务时**捕获上下文；`DAOManager` 按服务上下文分表（本轮日志：`login` 11 个 DAO、`game` 71 个 DAO）。
  - 直连模式下，登录服 `GsConnection.processData` 是由**游戏线程**（`DirectTransport`）调用的，而交付时又把上下文切到**发送方**（game），导致登录服的包处理与它 `schedule(...)` 出的延迟任务（`SM_MACBAN_LIST`、`PingPongThread`）全部带着 `game` 上下文运行 → 查不到只注册在 `login` 上下文的 DAO。
  - 佐证：日志只初始化了 `login`/`game` 两张表，且从未出现 `DAOManager is not initialized`（否则会是 `IllegalStateException` 而不是 `DAONotFoundException`）→ 失败调用必然落在 `game`；socket 模式历史日志（上下文=login）该错误出现 0 次。
- 修复：`pumpWrites` 交付封包时切到**接收方**上下文 `target.serviceContext()`；`DirectTransport.close()` 的清理改为 `cleanupEndpoint`，在**各端自身**上下文中执行 `onDisconnect()`（与 socket 模式「每端在自身上下文中处理入站与断开」一致）。
- 测试：`DirectMemoryLoginChannelTest` 新增断言——投递时上下文必须是接收方的 `game`（缺陷版会是发送方的 `login`）、清理时各端分别是 `login`/`game`。

## Behavior boundary

- 不修改客户端包体格式、opcode 注册、XML 模板规则或数据库 Schema。
- 队伍/联盟回调原 before/after 语义已映射到对应监听器方法；`FindGroupService` 仍通过运行时门面操作当前服务实例，并用 CAS 避免重复注册。
- 内嵌直连仍复用原封包写入、长度帧、opcode handler、认证状态机和各自线程池，只短路本地 socket 传输。

## Validation

- 已执行 Maven 聚焦测试（授权后）：
  - 批次一：`mvn test -Dtest=NettyConnectionHandlerTest,NettyConnectionPendingCloseTest,LoginServerConnectionTest,CommonsNetworkThreadPoolServicesTest,GameThreadPoolLifecycleTest,GameCoreServicesRuntimeBridgeTest,RealGeoDataConcurrencyTest,NpcMoveControllerPathTest,AionBootApplicationTest,NettyServerTest,Netty4ChatClientServerAdapterTest,ChatNettyServersTest,GameServerNetworkRuntimeBridgeTest,GameNetworkStartupLifecycleTest,LoginNetworkServicesTest`
    - 结果：`Tests run: 140, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- 批次二（回调/监听器重构覆盖）：`mvn test -Dtest=AggroListTest,AggroEventHandlerTest,FindGroupServiceTest,SiegeServiceTest,SiegeRaceCounterTest,PlayerAllianceGroupTest,LeagueLeftEventTest,TeamKinahDistributionEventTest,TeamTypeTest,GameSiegeScheduleLifecycleTest,AutoGroupServiceTest,PlayerAggroLevelTest,SiegeLocationTest`
    - 结果：`Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
  - 批次三（R4 与网络回归合并复跑）：`mvn test -Dtest=DirectMemoryLoginChannelTest,LoginServerConnectionTest,NettyConnectionHandlerTest,NettyConnectionPendingCloseTest,NettyServerTest,CommonsNetworkThreadPoolServicesTest,GameServerNetworkRuntimeBridgeTest,GameNetworkStartupLifecycleTest,LoginNetworkServicesTest,Netty4ChatClientServerAdapterTest,ChatNettyServersTest`
    - 结果：`Tests run: 47, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
  - 批次四（R5 上下文隔离）：`mvn test -Dtest=DirectMemoryLoginChannelTest` → `Tests run: 3, Failures: 0, Errors: 0`，`BUILD SUCCESS`。
  - 首轮曾暴露 `NpcMoveControllerPathTest` 两个既有夹具 NPE；加入 `WorldPosition` 空区域防护后重跑通过。
- 已执行 `mvn test -Dtest=NettyConnectionHandlerTest`（缺陷版/修复版对照，见 R1）。
- 已执行 `mvn test -Dtest=DirectMemoryLoginChannelTest`（缺陷版/修复版对照，见 R4）。
- 已执行 `mvn test-compile -DskipTests`：`BUILD SUCCESS`（主线与测试源均编译通过）。
- 已执行：`git diff --check`；旧回调/Javassist 引用全文检索；IntelliJ error-level inspection 覆盖核心修改文件（`PlayerDAO` 的 SQL datasource inspection 为 IDE 数据源未配置误报，非本次代码错误）。
- 运行态证据（用户启动的实例，非本 Agent 重启）：
  - 18:19:22 起聊天服握手双向完成（`CM_CS_AUTH - 游戏服务器 #1 现已在线` + `CM_CS_AUTH_RESPONSE - 游戏服务器认证成功`），不再出现 5 秒重连循环 → R1 运行态确认。
  - 18:37:35 / 18:39:48 起登录直连完成认证（`c.a.l.n.g.clientpackets.CM_GS_AUTH - 游戏服务器 #1 现已在线`），此前 in-memory 的 4 次启动均无此行 → R2/R4 运行态确认。
  - 同一实例暴露 R5（见上）；R5 修复后**尚未重启验证**。
- 未执行服务器启动/重启、真实客户端验收或全量 `mvn test`；登录直连的端到端（含客户端账号认证进入游戏）仍待重启后的真实日志确认。

## Runtime acceptance (2026-09-15 18:47 启动，端到端)

本轮为**真实验收**（用户启动实例、真实 5.8 客户端登录），窗口取 18:41:32 上次关闭之后至今：

- ERROR / WARN 总数：**0**。
- 全链路证据：
  - `18:48:22 c.a.l.network.aion.LoginConnection - 连接尝试来自：192.168.1.21`（客户端 → 登录服）
  - `18:48:23 c.a.g.network.aion.AionConnection - 连接来自：192.168.1.21` + `SM_VERSION_CHECK - 认证通过：客户端版本 5.8`
  - `18:48:23 c.a.g.n.loginserver.LoginServer - 账号认证状态：账号=1，状态=cc`（账号认证走内嵌直连 → 证明直连 `isAuthed()` 为真）
  - `18:48:27 GAMECONNECTION_LOG - 玩家 Ww（账号 cc）进入世界，MAC 地址=6C-0B-5E-A4-1D-57`
  - `18:48:27 GAMECONNECTION_LOG - 玩家登录：Ww，账号：cc`
- 稳定性：`游戏服务器 #1 的心跳检测已启动` 后再无 `心跳线程 #1 发生异常`；无 `未实现类 … 的 DAO`；无 `与登录/聊天服务器的连接已丢失`。
- 结论：R1–R5 全部获得运行态/端到端确认（此前 R1/R2/R4 已在 18:19、18:37/18:39 实例上分别确认）。

## Notes

- 计划中提到的 `FortressSiegeTest`、`PlayerGroupServiceTest`、`PlayerDAOTest` 当前仓库不存在；不得将其表述为已运行。
- `startup-20260824.jfr` 是任务前已有 untracked 文件，未纳入本次改动范围。

### 全量测试与基线对照 (2026-09-15)

- 全量 `mvn test`：`Tests run: 3247, Failures: 13, Errors: 0, Skipped: 2`。
- 其中 20 个失败（首轮）有 7 个由本次改造引入，已修复：
  - `KnownListIterationSafetyTest`（静态闸门：禁止直接遍历 known/visual 映射）+ `KnownListTest` 5 例快照语义 → 还原 `synchronized(map)` 快照遍历（容器仍为 `ConcurrentHashMap`）。
  - `SkillSpelledEventTest.broadcastsSuccessfulSkillToNearbyNpc` NPE → 测试用 Objenesis 构造对象 `objectId` 为 null，`ConcurrentHashMap` 拒绝 null 键；测试夹具补真实 ID（生产环境 objectId 永不为 null）。
  - `MapRegion.getObjectsSnapshot()` 删除、`PacketProcessor` 去掉 `LinkedList` 强转（收尾清理）。
- 剩余 13 个失败与本次改造无关：用 `git archive HEAD`(2f0752248) 在 `/tmp` 干净副本中运行同一批测试类，**13 个失败逐条原样复现**（Retail AI 定义计数 134/133、WorldScoped waypoint 3206/3207、windstream 兼容映射、quest 1722/1367/3935/80805/10032 路由与 SETPRO 领奖审计、Theobomos 编队分组）。属于分支既有数据/审计闸门欠账，未在本次范围内修改。
