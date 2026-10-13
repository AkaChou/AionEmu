# R5 批次施工单：剩余 17 个静态定位器总清扫（14 小域 + Engine + WorldBootstrap + ThreadPool）

> 依据：`docs/architecture-optimization/2026-10-10-lifecycle-locator-retirement-plan.zh-CN.md`（R5 行口径 + R3/R4 遗留的「计划表外域盘点结论」）
> 前置：R1（CoreGameplay `4ad05b991`）、R2（LocationBootstrap `4562f39d7`）、R3（FeatureServices `049223053`）、R4（WorldServices `4d9e41a90`）已完成并提交。本批为用户裁定的「剩余全部一起」总清扫批，沿用四次验收的流程、fail-fast getInstance 模板与测试冻结审计套路。

---

## 一、范围与终态

**目标定位器**：`gameserver/lifecycle/` 下剩余全部 17 个静态定位器，合计 **4693 调用 / 1213 消费文件**（均为 src/main 口径）：

| # | 定位器 | 行数 | provider | 访问器 | 调用 | 热点访问器 |
|---|---|---:|---:|---:|---:|---|
| 1 | GameMaintenanceServices | 94 | 4 | 2 | 2 | — |
| 2 | GameCraftServices | 84 | 2 | 2 | 4 | — |
| 3 | GameEventServices | 264 | 6 | 6 | 16 | eventService 8 |
| 4 | GameMovementLoopServices | 174 | 4 | 4 | 17 | moveTaskManager 7 |
| 5 | GameStaticDataServices | 112 | 3 | 3 | 17 | htmlCache 12 |
| 6 | GameGameplayServices | 137 | 4 | 4 | 20 | duelService 12 |
| 7 | GameTaskManagerServices | 140 | 4 | 4 | 26 | expireTimerTask 20 |
| 8 | GameCreativityServices | 336 | 11 | 11 | 36 | creativityEssenceService 6 |
| 9 | GameBattlefieldServices | 280 | 9 | 9 | 52 | kamarBattlefieldService 7 |
| 10 | GameEventBootstrapServices | 164 | 5 | 5 | 56 | minionService 32 |
| 11 | GameServerNetworkServices | 263 | 8 | 8 | 61 | loginServer 41 |
| 12 | GameCronServices | 56 | 0 | 3 | 77 | cronService 76 |
| 13 | GameHousingServices | 209 | 5 | 5 | 98 | housingService 63 |
| 14 | GameRuntimeServices | 437 | 24 | 16 | 124 | inGameShopEn 26 |
| 15 | GameEngineServices | 245 | 5 | 5 | 821 | skillEngine 675 |
| 16 | GameWorldBootstrapServices | 181 | 5 | 4 | 1011 | world 918 |
| 17 | GameThreadPoolServices | 80 | 1 | 1 | 2255 | threadPoolManager 2254（+rememberThreadPoolManager 1） |
| | **合计** | | **98** | **90** | **4693** | |

**终态**：
1. 17 个定位器全部删除；4693 处消费改走各域服务自持 `XxxService.getInstance()`；Cron 域特殊（`CronService.requireCurrent()`，见 §3.2）；
2. 90 个目标服务通道统一：**87 个标准收口**（`resolvedInstance` 缓存 + fail-fast + `setInstanceProvider` 重置；其中 67 个含 Lombok `@Setter` 删除、5 个 FALLBACK 拆兜底、IDFactory 去 `final`）+ **BannedMacManager 补通道** + CronService 不改（commons 多上下文单例，消费侧改 `requireCurrent()`）+ ThreadPoolManager 已达标不动；
3. 绑定职责迁入 **16 个**新建 `XxxServiceBindings` @Component（Cron 域无绑定）；`GameThreadPoolLifecycle` / `GameUtilityServicesLifecycle` 两处编排器内联改造；
4. 测试：删除/改写 8 个语义反转审计、2 个专职测试保留行为断言、`GameServiceProviderCompatibilityTest`（16 域段落）与 `AionBootApplicationTest` 等编译级改写、新建总冻结审计 `RemainingServiceBindingsTest`。

**A 类判定 = 0（无构造注入子步）**：全库 `^\t(?!\t)...= Game\w+Services\.` 类级字段初始化器扫描仅 2 命中（`CalindiFlamelordAI2:57`、`IDTiamatKalrindyNamed65AlAI2:57`，`trapTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(...)`）——二者均为 **AI2 实例字段**（AI 引擎运行时反射构造，非 Spring 装配期），B 类直换 `ThreadPoolManager.getInstance()` 语义等价；无 `@Component` 类装配期字段初始化器命中。

---

## 二、现状核实（2026-10-13）

### 2.1 引用总量

- 外部访问器调用 **4693 处 / 1213 个 src/main 文件**（各域明细见 §一表；完整访问器→目标映射见附表 A）。
- 全部为 B 类直换（A 类 = 0）；无 `*Fallbacks` 类（计划 R5 行的 Fallbacks 已在前批消解）。

### 2.2 目标服务通道矩阵（90 个并集目标）

| 形态 | 数量 | 处置 |
|---|---:|---|
| 标准收口（加 `resolvedInstance` + 重置 + 保留 fail-fast） | **87** | §3.1 统一模板 |
| ├─ 内含 Lombok 字段级 `@Setter` | 67 | 删 `-- SETTER --` javadoc 段 + `@Setter` + 条件删 `import lombok.Setter`，改手写 setter |
| ├─ 内含 `getIfAvailable(() ->` FALLBACK | 5 | ChatProcessor、LifeStatsRestoreService、NetworkController、RiftManager、SkillEngine——删兜底分支改 fail-fast |
| └─ `public static final` getInstance | 1 | IDFactory——去 `final` + 加缓存 |
| `BannedMacManager` 补通道 | 1 | 现为 `private static final ... manager = new BannedMacManager()` 预构造静态单例、无 provider 字段、无 `setInstanceProvider`——按标准模板补全（类内 2 处全限定自调改 `LoginServer.getInstance()`） |
| `CronService` 不改造 | 1 | commons 层按 `ServiceContext` 键控多实例单例；`requireCurrent()`（null 检查 + `CronServiceException`）已存在，无需缓存/通道改造 |
| `ThreadPoolManager` 已达标 | 1 | 已有标准通道（全库 91 目标中唯一），不动 |

### 2.3 特例

- **Cron 域（GameCronServices，非 @Component 静态门面）**：
  - `cronService()` 76 处 / 45 文件 → `CronService.requireCurrent()`（与现 fallback 路径同一实现；`initSingleton` 时机不变）；
  - `initialize()` 唯一调用点 `GameUtilityServicesLifecycle:97 initializeCronService()` → 内联 `CronService.initSingleton(ThreadPoolManagerRunnableRunner.class)`；
  - `shutdownIfInitialized()` **0 调用死方法** → 随类删除；无新建 Bindings。
- **ThreadPool 域**：
  - `GameThreadPoolLifecycle`（98 行）：私有 `threadPoolManager()` 现为 `GameThreadPoolServices.rememberThreadPoolManager(provider.getObject())` —— 退役后改 `ThreadPoolManager.getInstance()`，删 `@Autowired(required=false) setThreadPoolManagerProvider` 与 provider 字段（`GameUtilityServicesLifecycle:62` 调 `threadPoolLifecycle.start()` 位于容器 refresh 之后，getInstance 通道已由 Bindings 喂入，时序安全）；
  - `rememberThreadPoolManager()` 仅此 1 调用点，随定位器删除。
- **GameRuntimeServicesLifecycle（694 行）辨析**：持 24×`ObjectProvider` 实例字段的启动编排器，**非静态定位器，本批不动**（其测试仅删语义反转断言）；同理 `GameThreadPoolLifecycle` 之外的既有 `*Lifecycle` 编排器均不动。
- **替换防误伤**：`GameRuntimeServices.` 带点模式与 `GameRuntimeServicesLifecycle.` 天然区分（`e` 后是 `.` 才匹配），沿用 R3/R4 脚本模式。
- **跨域调用**：`GameCraftServicesTest` 等旧审计的排除清单（如 `GameRuntimeServicesTest` 12 个自调排除文件）即各域服务自调场景，统一替换后自然归零。

### 2.4 装配与测试现状

测试引用全景（17 域）：

| 定位器 | 引用测试（次数） |
|---|---|
| GameMaintenanceServices | GameServiceProviderCompatibilityTest:3 |
| GameCraftServices | GameCraftServicesTest:2、Compatibility:4 |
| GameEventServices | PacketBroadcasterTest:6、Compatibility:8 |
| GameMovementLoopServices | GameStaticDataLifecycleTest:1、GameMovementLoopServicesTest:12、Compatibility:6、NpcMoveControllerPathTest:6、EventQuestNativeMaintenanceTest:3 |
| GameStaticDataServices | GameStaticDataLifecycleTest:5 |
| GameGameplayServices | GameGameplayServicesTest:3、Compatibility:6 |
| GameTaskManagerServices | GameTaskManagerServicesTest:3、Compatibility:6 |
| GameCreativityServices | GameCreativityServicesTest:2、Compatibility:15 |
| GameBattlefieldServices | Compatibility:11 |
| GameEventBootstrapServices | Compatibility:7 |
| GameServerNetworkServices | Compatibility:4 |
| GameCronServices | GameCronServicesTest:3、ScheduleHotReloadTest:7 |
| GameHousingServices | GameHousingServicesTest:3、Compatibility:7 |
| GameRuntimeServices | GameRuntimeServicesLifecycleTest:33*、GameStartupSequenceLifecycleTest:4*、GameRuntimeServicesTest:2、Compatibility:21、AionBootApplicationTest:3、GameServiceLifecycleTest:4 |
| GameEngineServices | ShutdownHookTest:1、RetailConditionSpawnEngineTest:3、QuestSkillDispatchBoundaryTest:1、QuestE2eWorldFixture:5、QuestProtocolLoop:2、DialogServiceQuestDialogTest:3、EventQuestNativeMaintenanceTest:5、AionBootApplicationTest:2 |
| GameWorldBootstrapServices | ShutdownHookTest:4、SpawnEngineTeardownSpawnGuardTest:3、SMPlayerSpawnTest:2、RetailConditionSpawnEngineTest:3、LeagueLeftEventTest:3、PlayerPvpRulesTest:3、PlayerQuestInventoryPortTest:3、NpcMoveControllerPathTest:2、SiegeServiceTest:3、OutpostTest:3、SystemMailServiceTest:5、BaseTest:3 |
| GameThreadPoolServices | GameThreadPoolLifecycleTest:9、RealGeoDataConcurrencyTest:2、AbstractCollisionObserverTest:3、NpcMoveControllerPathTest:4、PeriodicSaveServiceTest:3 |

（\* 计数含 `GameRuntimeServicesLifecycle` 前缀匹配；该类非本批对象，实际定位器引用以施工脚本精确统计为准。）

**处置分类**：
- **删除（语义反转审计，10 个）**：GameCraftServicesTest、GameCreativityServicesTest、GameCronServicesTest、GameGameplayServicesTest、GameHousingServicesTest、GameMovementLoopServicesTest、GameRuntimeServicesTest、GameTaskManagerServicesTest、GameStaticDataLifecycleTest（含 1 处 MovementLoop 引用；行为断言如有则先核对保留）；
- **保留行为 + 删反转断言（2 个）**：GameThreadPoolLifecycleTest（保留 startAndStopAreIdempotent 等行为断言；删 gameCronRunnerUsesThreadPoolBridgeInsteadOfDirectSingleton / threadPoolServicesFailFastOutsideSpringContainerInsteadOfLocalFallback / gameServicesUseLifecycleSchedulerBridges* 等反转断言；threadPoolLifecycleBridgesLegacyManagerThroughSpringProviders 随 provider 字段删除改写）、GameRuntimeServicesLifecycleTest（保留 fieldType 24 断言与 2 个 start 行为断言；删 2 个 assertNoDirectSingletonAccess 反转方法）；
- **编译级 + 语义改写**：GameServiceProviderCompatibilityTest（16 域段落：构造 `new GameXxxServices(...)` → `new XxxServiceBindings(...)`、访问器断言 → `X.getInstance()`、冗余双断言删行）、AionBootApplicationTest（Runtime/Engine 装配断言）、ScheduleHotReloadTest、PacketBroadcasterTest、NpcMoveControllerPathTest、ShutdownHookTest、BaseTest 及 WorldBootstrap/ThreadPool 面 12+5 个测试、Quest 面 4 个测试；
- **新建**：`RemainingServiceBindingsTest` 总冻结审计（17 域表驱动三断言：定位器不回归 / 生产代码零引用 / 16 个 Bindings 喂入 N 通道）。

---

## 三、处置规则

### 3.1 C 类收口（87 标准 + 2 特殊，统一 R1 `DropService` 模板）

```java
private static volatile XxxService resolvedInstance;

private static volatile ObjectProvider<XxxService> instanceProvider;   // 已有则保留

public static XxxService getInstance() {
    XxxService resolved = resolvedInstance;
    if (resolved != null) {
        return resolved;
    }
    ObjectProvider<XxxService> provider = instanceProvider;
    resolved = provider == null ? null : provider.getIfAvailable();
    if (resolved == null) {
        throw new IllegalStateException("XxxService 未由 Spring 提供：..."
            + "（静态兜底已退役）");
    }
    resolvedInstance = resolved;
    return resolved;
}

public static void setInstanceProvider(ObjectProvider<XxxService> provider) {
    instanceProvider = provider;
    resolvedInstance = null;
}
```

- 三类脚本分派：①手写 setter 族（20 个）直接在 `setInstanceProvider` 补 `resolvedInstance = null;` + 加字段 + getInstance 缓存版；②lombok 族（67 个）另删 javadoc 段/注解/条件 import 改手写；③FALLBACK 族（5 个）另删 `getIfAvailable(() ->` 分支；
- 特例：BannedMacManager 全通道补全（删预构造 `manager` 字段）；IDFactory 去 `final`；World 手写形态确认后补缓存。

### 3.2 B 类（消费方 → 90 映射）

`GameXxxServices.{name}()` → `{Svc}.getInstance()`（脚本批量 + 计数断言 + grep 归零 + build 兜底，同 R1-R4）；import 就地改写（含通配场景插精确 import、全限定内联改短名）。

**Cron 域例外**：`GameCronServices.cronService()` → `CronService.requireCurrent()`（46 文件，import `com.aionemu.commons.services.CronService`）；`GameCronServices.initialize()` 调用点内联 `CronService.initSingleton(ThreadPoolManagerRunnableRunner.class)`。

### 3.3 绑定迁移（16 个 Bindings + 2 处编排器改造）

| 新 Bindings（@Component + DisposableBean） | provider 数 | 对象 |
|---|---:|---|
| MaintenanceServiceBindings | 4 | SeasonRankingUpdateService、ShugoImperialTombSpawnManager、AbyssRankCleaningService*、DatabaseCleaningService* |
| CraftServiceBindings | 2 | CraftSkillUpdateService、RelinquishCraftStatus |
| EventServiceBindings | 6 | EventService、PlayerEventService、CrazyDaevaService、AbyssRankUpdateService、PacketBroadcaster、EventScheduler |
| MovementLoopServiceBindings | 4 | MovementNotifyTask、MoveTaskManager、PlayerMoveTaskManager、ZoneUpdateService |
| StaticDataServiceBindings | 3 | DataManager、HTMLCache、XmlDataLoader |
| GameplayServiceBindings | 4 | DuelService、LifeStatsRestoreService、SeasonRankingService、RiftManager |
| TaskManagerServiceBindings | 4 | ExpireTimerTask、TeamEffectUpdater、TeamMoveUpdater、TemporaryTradeTimeTask |
| CreativityServiceBindings | 11 | CreativityEssence/Skill/Stats/TransfoService + Accuracy/Agility/Health/Knowledge/Power/Precision/Will |
| BattlefieldServiceBindings | 9 | 9 个战场/副本服务 |
| EventBootstrapServiceBindings | 5 | LunaShopService、MinionService、ShugoSweepService、AtreianPassportService、EventWindowService |
| ServerNetworkServiceBindings | 8 | LoginServer、ChatServer、BannedMacManager、NetworkController、PacketLoggerService、AionPacketHandlerFactory、PacketFloodFilter、LsPacketHandlerFactory |
| HousingServiceBindings | 5 | HousingBidService、MaintenanceTask、TownService、HousingService、ChallengeTaskService |
| RuntimeServiceBindings | 24 | 16 个访问器服务 + 7 个绑定专用（CuringZoneService、DebugService、FlyRingService、GameTimeService、SpringZoneService、TaskManagerFromDB、WebshopService）+ Influence |
| EngineServiceBindings | 5 | SkillEngine、QuestEngine、InstanceEngine、AI2Engine、ChatProcessor |
| WorldBootstrapServiceBindings | 5 | World、IDFactory、ZoneService、HotspotTeleportService、RoadService* |
| ThreadPoolServiceBindings | 1 | ThreadPoolManager |

（\* 绑定专用服务，无访问器；16 域 provider 数合计 = 98。）

- 构造器/destroy 从旧定位器机械提取（R3 模式，仅剔除静态字段赋值段）；
- 删除 17 个定位器；`GameThreadPoolLifecycle` 改 `ThreadPoolManager.getInstance()` + 删 provider 字段；`GameUtilityServicesLifecycle.initializeCronService()` 内联 initSingleton。

### 3.4 测试处置

见 §2.4。新建 `RemainingServiceBindingsTest` 三断言（表驱动 17 域）。`LegacySingletonFallbackAuditTest` RETIRED 名单随 5 个 FALLBACK 拆除核对更新（全库 `getIfAvailable(() ->` 6 → 1，仅剩 ChatProcessRuntimeBridge）。

---

## 四、动作序列（4 子步，逐步验证）

1. **C 类收口**（87 标准 + BannedMacManager 补通道 + IDFactory 去 final + 67 lombok + 5 FALLBACK；CronService/ThreadPoolManager 不动）→ `build_project` + 断言复核（每类四断言：有缓存/重置/FALLBACK 归零/lombok 归零）；
2. **B 类批量替换**（90 映射 / 4693 调用 / 1213 文件 + Cron 特例）→ build + grep 残留归零 + 抽样；
3. **绑定迁移**（16 Bindings 生成 + 17 定位器删除 + 2 编排器改造 + 编译级测试改写）→ build；
4. **测试语义改写与分批复跑**（删 10 个反转审计、改写 2 个、建 RemainingServiceBindingsTest）→ 全绿 → 提交授权。

## 五、验收门禁

- **G1 反向 grep**：17 定位器名 + `rememberThreadPoolManager` 在 src/main src/test 仅剩新审计断言文本与文档；
- **G2 构建**：build 全绿；
- **G3 测试**：lifecycle + services + world + boot + quest 面分批复跑全绿（截断日志 `testFailed=0` 终检）；
- **G4 缓存语义**：87 服务 getInstance 含 `resolvedInstance` 且 setter 重置；world（918）/ skillEngine（675）/ threadPoolManager（2254）三大热点路径核对；`getIfAvailable(() ->` 全库 = 1；
- **G5 提交**：用户授权后提交（英文 message、显式路径暂存、单 commit 主体）。

## 六、风险与盘点

- **热点承接**：2254+918+675 = 3847 处集中于 3 个访问器；替换后入口唯一化，缓存语义由各服务自持；
- **时序**：`GameThreadPoolLifecycle.start()` / `GameUtilityServicesLifecycle` 序列位于容器 refresh 之后，getInstance 通道可用；AI2 字段初始化器 2 处为运行时构造；
- **BannedMacManager**：删除预构造静态单例后，容器外获取场景探针经 `TestServiceProviders.install` 覆盖；
- **编排器不动**：`GameRuntimeServicesLifecycle`（24 provider 字段）等既有 `*Lifecycle` 装配类非定位器，本批仅删其测试中的语义反转断言；
- **契约面**：`TestServiceProviders.install` 依赖 `setInstanceProvider` 签名（保留不动）；
- **规模**：1.9× R1-R4 合计（4693 vs 2432），diff 预计 ~1300 文件，单批单 commit。

---

## 八、施工记录（2026-10-13）

**子步 1（C 类收口）**：87 个标准服务 + 2 个特殊（BannedMacManager 补通道、CronService 不改）统一加 `private static volatile X resolvedInstance;` + fail-fast `getInstance()`（读缓存 → 穿 `instanceProvider` → null 抛 ISE 指向 LegacySingletonFallbackAuditTest → 写缓存），`setInstanceProvider` 内置 `resolvedInstance = null;` 重置。ThreadPoolManager 已达标不动。

**子步 2（B 类批量替换）**：4693 处 `GameXxxServices.acc()` → `XxxService.getInstance()` 脚本批量替换（带点模式正则防字母连续前缀误伤，如 `GameRuntimeServicesLifecycle` 不被 `GameRuntimeServices` 匹配）；Cron 域 76 处 → `CronService.requireCurrent()`。替换后 17 定位器名全库归零；build 绿。LoginServer.java 因 gameserver/loginserver 双 `ThreadPoolManager` 同简名冲突，改全限定名（全库同简名扫描确认独例）。

**子步 3（绑定迁移）**：生成 16 个 `XxxServiceBindings` @Component（implements DisposableBean，构造器 ObjectProvider 喂入、destroy 清 null；构造器参数与旧定位器逐一对应，含 Runtime 23 provider、ServerNetwork 8 含 BannedMacManager 追加）；删除 17 个定位器（16 + GameCronServices）；`GameThreadPoolLifecycle` 改造为 `ThreadPoolManager.getInstance()`（删 provider 字段与 setter）；测试编译级改写：访问器替换 99 处/11 文件、`new GameXxxServices(...)`→`new XxxServiceBindings(...)` 类名 swap 89 处、反射字段迁移 12 处（定位器缓存字段 → 服务自持 `resolvedInstance`/`instanceProvider`）、删除 8 个语义反转审计类（GameCraftServicesTest、GameCreativityServicesTest、GameCronServicesTest、GameGameplayServicesTest、GameHousingServicesTest、GameMovementLoopServicesTest、GameRuntimeServicesTest、GameTaskManagerServicesTest）；build 绿。

**子步 4（测试语义改写与分批复跑）**：
- 新建 `RemainingServiceBindingsTest`（三断言冻结审计：17 定位器文件不存在 / src/main 全扫词边界 regex 零引用 / 16 Bindings 含每服务 `setInstanceProvider(` 与 `(null)`）；定位器名子串误报（`GameRuntimeServicesLifecycle`）以 `Pattern.compile("\\b"+locator+"\\b")` 解决。
- 手工改写：ShutdownHookTest（删反转方法 finalShutdownUsesLifecycleBridgesInsteadOfDirectServiceSingletons）、GameThreadPoolLifecycleTest（重写 5 方法含 fail-fast 断言）、GameStaticDataLifecycleTest、GameRuntimeServicesLifecycleTest、AionBootApplicationTest（`engineServiceBindings` bean 断言）、ScheduleHotReloadTest（`CronService.initSingleton(ThreadPoolManagerRunnableRunner.class)`）。
- **工具缺陷修复**：RealGeoDataConcurrencyTest 被脚本在字符串字面量内误替换（`assertTrue(contains("GameThreadPoolServices.threadPoolManager().submitLongRunning"))` 与 `assertFalse(contains("ThreadPoolManager.getInstance().submitLongRunning"))` 撞成同串自相矛盾）→ 删除矛盾行 + 清理脚本误加 unused import；全量扫描确认无同类误伤（另 2 处同串冲突为不同 source 变量的合法断言）。
- 复跑全绿：RemainingServiceBindingsTest 3/3、LegacySingletonFallbackAuditTest 1/1、GameThreadPoolLifecycleTest 5/5、GameRuntimeServicesLifecycleTest 3/3、GameStaticDataLifecycleTest 5/5、GameServiceProviderCompatibilityTest 16/16、ScheduleHotReloadTest 1/1、PacketBroadcasterTest 2/2、PeriodicSaveServiceTest 3/3、ShutdownHookTest 4/4、AionBootApplicationTest、NpcMoveControllerPathTest、AbstractCollisionObserverTest、SMPlayerSpawnTest、RealGeoDataConcurrencyTest 6/6、SiegeServiceTest 2/2、BaseTest 2/2、OutpostTest 2/2、SystemMailServiceTest 2/2、RetailConditionSpawnEngineTest 3/3、SpawnEngineTeardownSpawnGuardTest 2/2、LeagueLeftEventTest 1/1、PlayerPvpRulesTest 4/4、PlayerQuestInventoryPortTest 14/14、DialogServiceQuestDialogTest 8/8、EventQuestNativeMaintenanceTest 6/6；build 兜底全绿（零 problems）。

**后续修复（2026-10-13 实机联调暴露）**：R5 提交后实机登录认证链路抛 `AccountService 未由 Spring 提供：instanceProvider 未注入`。根因：`CoreRuntimeServiceBeans` 装配的 7 个服务（AccountService、HTMLService、ArmsfusionService、PunishmentService、RenameService、SocialService、LimitedQuestService）在 933ad0601（Flex 迁移批次）引入 fail-fast 通道时遗漏接线——前 3 个为 provider 型但全库无喂入者；后 4 个为构造器自注册型（`instance = this`）但其 `@Lazy` bean 从未被任何组件请求，静态通道恒为 null。修复：4 个自注册型重构为标准 provider 模板（`resolvedInstance` 缓存 + fail-fast `getInstance()` + `setInstanceProvider` 重置）；新建 `CoreRuntimeServiceBindings` 统一喂入 7 个通道并在销毁时清空；`LegacySingletonFallbackAuditTest` 新增全库审计「持有 `instanceProvider` 通道的类必须有生产喂入者」（当前 167 持有者 0 断线，本次修复正是被该断言覆盖的类别）；新建 `CoreRuntimeServiceBindingsTest` 行为断言（喂入 assertSame + destroy 后 fail-fast）。影响面：登录选服认证（AccountService）、任务接取（LimitedQuestService.tryAcquire）、武器融合/打破、加好友/黑名单、角色与军团改名、管理员封号/监狱、AI 引导 HTML 与问卷奖励。

**待用户执行**：实机重启验证（R4/R5 均未实机验证，改动须重启服务端才生效）。

---

## 附表 A：90 个访问器 → 目标服务映射（调用数）

| 域 | 映射（访问器 → 目标 getInstance，调用数） |
|---|---|
| Maintenance | seasonRankingUpdateService→SeasonRankingUpdateService 1；shugoImperialTombSpawnManager→ShugoImperialTombSpawnManager 1 |
| Craft | craftSkillUpdateService→CraftSkillUpdateService 2；relinquishCraftStatus→RelinquishCraftStatus 2 |
| Event | eventService→EventService 8；crazyDaevaService→CrazyDaevaService 2；abyssRankUpdateService→AbyssRankUpdateService 2；eventScheduler→EventScheduler 2；playerEventService→PlayerEventService 1；packetBroadcaster→PacketBroadcaster 1 |
| MovementLoop | moveTaskManager→MoveTaskManager 7；playerMoveTaskManager→PlayerMoveTaskManager 6；movementNotifyTask→MovementNotifyTask 2；zoneUpdateService→ZoneUpdateService 2 |
| StaticData | htmlCache→HTMLCache 12；dataManager→DataManager 3；xmlDataLoader→XmlDataLoader 2 |
| Gameplay | duelService→DuelService 12；lifeStatsRestoreService→LifeStatsRestoreService 5；riftManager→RiftManager 2；seasonRankingService→SeasonRankingService 1 |
| TaskManager | expireTimerTask→ExpireTimerTask 20；teamEffectUpdater→TeamEffectUpdater 3；temporaryTradeTimeTask→TemporaryTradeTimeTask 2；teamMoveUpdater→TeamMoveUpdater 1 |
| Creativity | creativityEssenceService 6；agility 4；health 4；knowledge 4；power 4；precision 4；will 4；creativitySkillService 4；creativityStatsService 1；creativityTransfoService 1；accuracy 0（访问器随类删） |
| Battlefield | kamarBattlefieldService 7；hallOfTenacityService 7；engulfedOphidanBridgeService 6；ironWallWarfrontService 6；idgelDomeService 6；suspiciousOphidanBridgeService 5；idgelDomeLandmarkService 5；grandArenaTrainingCampService 5；idRunService 5 |
| EventBootstrap | minionService 32；lunaShopService 16；shugoSweepService 4；atreianPassportService 2；eventWindowService 2 |
| ServerNetwork | loginServer→LoginServer 41；bannedMacManager→BannedMacManager 5；chatServer→ChatServer 5；packetLoggerService→PacketLoggerService 4；networkController→NetworkController 2；packetFloodFilter→PacketFloodFilter 2；aionPacketHandlerFactory 1；lsPacketHandlerFactory 1 |
| Cron | cronService→CronService.**requireCurrent()** 76；initialize→内联 initSingleton 1 |
| Housing | housingService 63；housingBidService 15；townService 10；maintenanceTask 6；challengeTaskService 4 |
| Runtime | inGameShopEn 26；findGroupService 22；brokerService 14；influence→Influence 9；exchangeService 9；weatherService 7；petitionService 7；playerTransferService 6；territoryService 5；limitedItemTradeService 5；adminService 4；surveyService 3；gmService→GMService 3；periodicSaveService 2；announcementService 1；boostEventService 1 |
| Engine | skillEngine→SkillEngine 675；questEngine→QuestEngine 127；instanceEngine→InstanceEngine 9；ai2Engine→AI2Engine 8；chatProcessor→ChatProcessor 2 |
| WorldBootstrap | world→World 918；idFactory→IDFactory 87；zoneService 5；hotspotTeleportService 1 |
| ThreadPool | threadPoolManager→ThreadPoolManager 2254（+rememberThreadPoolManager 1 随类删） |

## 附表 B：C 类收口清单（87 标准 + 2 特殊）

**87 标准（加 `resolvedInstance` + `setInstanceProvider` 重置）**：

- Maintenance：SeasonRankingUpdateService、ShugoImperialTombSpawnManager
- Craft：CraftSkillUpdateService、RelinquishCraftStatus
- Event：EventService、PlayerEventService、CrazyDaevaService、AbyssRankUpdateService、PacketBroadcaster、EventScheduler
- MovementLoop：MovementNotifyTask、MoveTaskManager、PlayerMoveTaskManager、ZoneUpdateService
- StaticData：DataManager、HTMLCache、XmlDataLoader
- Gameplay：DuelService、LifeStatsRestoreService（FALLBACK）、SeasonRankingService、RiftManager（FALLBACK）
- TaskManager：ExpireTimerTask、TeamEffectUpdater、TeamMoveUpdater、TemporaryTradeTimeTask
- Creativity：CreativityEssenceService、CreativitySkillService、CreativityStatsService、CreativityTransfoService、Accuracy、Agility、Health、Knowledge、Power、Precision、Will
- Battlefield：KamarBattlefieldService、EngulfedOphidanBridgeService、SuspiciousOphidanBridgeService、IronWallWarfrontService、IdgelDomeService、IdgelDomeLandmarkService、HallOfTenacityService、GrandArenaTrainingCampService、IDRunService
- EventBootstrap：LunaShopService、MinionService、ShugoSweepService、AtreianPassportService、EventWindowService
- ServerNetwork：LoginServer、ChatServer、NetworkController（FALLBACK）、PacketLoggerService、AionPacketHandlerFactory、PacketFloodFilter、LsPacketHandlerFactory
- Housing：HousingBidService、MaintenanceTask、TownService、HousingService、ChallengeTaskService
- Runtime：AdminService、AnnouncementService、BoostEventService、BrokerService、ExchangeService、FindGroupService、GameTimeService*、GMService、Influence、LimitedItemTradeService、PeriodicSaveService、PetitionService、PlayerTransferService、SurveyService、TerritoryService、WeatherService、CuringZoneService*、DebugService*、FlyRingService*、SpringZoneService*、WebshopService*
- Engine：SkillEngine（FALLBACK）、QuestEngine、InstanceEngine、AI2Engine、ChatProcessor（FALLBACK）
- WorldBootstrap：World、IDFactory（去 `final`）、ZoneService、HotspotTeleportService、RoadService*

（\* 绑定专用服务：无访问器，仅存在于 provider 喂入与 C 类收口面。）

**特殊 2 个**：BannedMacManager（补全通道）；CronService（不改）。
**不动 1 个**：ThreadPoolManager（已达标）。
