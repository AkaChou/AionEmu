# R2 批次施工单：GameLocationBootstrapServices 静态定位器退役

> 依据：`docs/architecture-optimization/2026-10-10-lifecycle-locator-retirement-plan.zh-CN.md`（R2 行：计划口径 433 引用）
> 前置：R1（GameCoreGameplayServices）已完成并提交（`4ad05b991`）。本批沿用 R1 已验证的五子步流程、fail-fast getInstance 模板与测试注入套路。

---

## 一、范围与终态

**目标定位器**：`lifecycle/GameLocationBootstrapServices.java`（@Component 纯绑定定位器）：
- 构造器持 22×`ObjectProvider` 参数，逐服务 `setInstanceProvider` 并写入同名静态字段；`destroy()` 反向清空；
- 19 个静态访问器（`vortexService()` … `abyssLandingSpecialService()`）；
- `resolveCached(provider)`：provider==null → fail-fast ISE；否则按 `ObjectProvider` 为 key 走 `RESOLVED_SERVICES`（ConcurrentHashMap）缓存解析结果——注释明示为并行刷怪的 JFR 热点优化（每位 spawn 都要穿 Spring 单例注册表全局锁，缓存后仅首次解析）。

**终态**：
1. 19 个静态访问器全部退役；79 个消费方改走域服务自持 `XxxService.getInstance()`（B 类 77 个）或构造注入（A 类 2 个）；
2. 22 个域服务 `getInstance()` 统一为 R1 `DropService` 模板（fail-fast + `resolvedInstance` 对象字段缓存；`setInstanceProvider` 同时重置缓存）——以对象字段缓存承接原 `RESOLVED_SERVICES` 的热点语义；
3. 绑定职责迁入新建 `LocationBootstrapServiceBindings` @Component（22×ObjectProvider 构造喂入 + destroy 清空）；定位器类删除；
4. 门禁测试改写为 R2 冻结审计（定位器不回归 / 生产代码零引用 / 绑定组件喂全 22 通道）。

---

## 二、现状核实（2026-10-13）

### 2.1 引用总量与分布

- 施工实测引用面：import 79 行 + 外部访问器调用 **412** 行（77 个文件）+ 定位器类内 provider 自赋值 19 行（随类删除）。
- 访问器调用明细（19 个）：

| 访问器 | 调用数 | 访问器 | 调用数 |
|---|---:|---|---:|
| `abyssLandingService` | 163 | `agentService` | 11 |
| `rvrService` | 40 | `svsService` | 9 |
| `beritraService` | 27 | `nightmareCircusService` | 9 |
| `abyssLandingSpecialService` | 21 | `conquestService` | 9 |
| `vortexService` | 19 | `riftService` | 8 |
| `anohaService` | 18 | `iuService` | 8 |
| `moltenusService` | 15 | `idianDepthsService` | 8 |
| `zorshivDredgionService` | 12 | `dynamicRiftService` | 8 |
| `outpostService` | 12 | `towerOfEternityService` | 4 |
| `instanceRiftService` | 11 | | |

- 消费方 **79 文件** 按目录：ai 26（abyssLanding 8 / beritraInvasion 5 / rvr 6 / worldBoss 3 / agentFight 2 / levinshor 1 / inggison 1）、commands/admin 16、services 31、clientpackets 2、configs 1、controllers 1、model/team2 1、spawnengine 1。

### 2.2 22 个域服务通道形态

- **16 个仍带 legacy 兜底**（`XxxServiceHolder.INSTANCE` 内部类 + `getIfAvailable(() -> Holder.INSTANCE)`）：Vortex、Beritra、Agent、Anoha、Svs、Rvr、Iu、NightmareCircus、DynamicRift、InstanceRift、ZorshivDredgion、Moltenus、Rift、Conquest、IdianDepths、TowerOfEternity。即「全库 25 处 `getIfAvailable(() ->` 残留」的主体。
- **6 个已 fail-fast 收口**（`AbyssLandingService` 形态，但无 `resolvedInstance` 缓存，每次调用穿 provider）：Outpost、AbyssLanding、AbyssLandingSpecial、Base、LandingUpdate、Siege。
- 22 个全部已有 `setInstanceProvider`；其中 Siege/Base 由字段级 Lombok `@Setter` 生成（无方法体可改，本批改为手写以承载缓存重置），其余 20 个为手写单行体。

### 2.3 热点与特例

- **`spawnengine/VisibleObjectSpawner.java`（19 处）**：每个 spawn 按 spawnId 查全部 19 类 location——正是 `resolveCached` 注释所指的 JFR 热点路径。替换后 getInstance 必须保有等价缓存（本次统一模板含 `resolvedInstance` 字段缓存）。
- `configs/Config.java`（12 处，`reloadSchedule()` 配置重载路径）；`controllers/RVController.java`（1）；`model/team2/alliance/PlayerAllianceService.java`（1，R1 已改造过其首批引用）。
- **服务本体自引用**：`InstanceRiftService`（自调 `instanceRiftService()` 2 处，266/269 行）、`RiftService`（自调 2 处，278/281 行）→ 直接替换为自身 `getInstance()`。
- **服务间互调（唯二跨对象）**：`AbyssLandingService` → `AbyssLandingSpecialService`（8 处，502-558 行，全在实例方法内）；`LandingUpdateService` → `AbyssLandingService`（13 处；其中 22-23 行为**字段初始化器**调用，构造注入后须移入构造器体）。
- `AbyssLandingSpecialService` 对定位器引用为 0（叶子，无出边）。

### 2.4 装配与测试现状

- 定位器为 @Component，由容器直接构造（无显式 new 宿主）；`GameLocationBootstrapLifecycle` 已自持 22 个 provider（`@Autowired(required=false)` setter），**不引用定位器**，本批不动。
- 测试引用 2 个文件：`GameLocationBootstrapServicesTest`（bridge 审计：断言源码**不得**出现 `XxxService.getInstance()`——R2 后语义反转，必须改写）；`GameServiceProviderCompatibilityTest`（`gameLocationBootstrapServicesRegistersAndClearsAbyssLandingProvider` 方法待改测新绑定组件）。
- 计划文档数字修正：plan 记 R2=433 引用、fallback 载体 `GameWorldBootstrapFallbacks`；实测 512 行/431 调用，当前代码**无独立 Fallbacks 类**，fallback 载体 = 定位器内部 `resolveCached` + 16 服务 `Holder`。

---

## 三、处置规则

### 3.1 A 类（2 个 Spring bean → 构造注入）

| 注入目标 | 消费方（@Bean：SiegeBattlefieldBeans） | 调用点 | 备注 |
|---|---|---|---|
| `AbyssLandingSpecialService` | `services/AbyssLandingService.java` | 8（全实例方法） | 依赖链叶子，无环 |
| `AbyssLandingService` | `services/abysslandingservice/LandingUpdateService.java` | 13 | 22-23 行字段初始化器移入构造器体（赋值后执行）；其余 11 处在实例方法 |

- @Bean 方法（SiegeBattlefieldBeans）加参数；依赖链 `AbyssLandingSpecialService ← AbyssLandingService ← LandingUpdateService` 单向无环，直接构造注入，不需要 R1 的构造环规避。
- 替换后消费处直接引用注入字段（不再每次解析）。

### 3.2 B 类（77 个静态宿主 → `XxxService.getInstance()`）

19 个替换映射（sed 批量 + 计数断言 + grep 归零 + build 兜底，同 R1）：

`vortexService()`→`VortexService.getInstance()`、`beritraService()`→`BeritraService`、`agentService()`→`AgentService`、`anohaService()`→`AnohaService`、`svsService()`→`SvsService`、`rvrService()`→`RvrService`、`iuService()`→`IuService`、`nightmareCircusService()`→`NightmareCircusService`、`dynamicRiftService()`→`DynamicRiftService`、`instanceRiftService()`→`InstanceRiftService`、`outpostService()`→`OutpostService`、`zorshivDredgionService()`→`ZorshivDredgionService`、`moltenusService()`→`MoltenusService`、`riftService()`→`RiftService`、`conquestService()`→`ConquestService`、`idianDepthsService()`→`IdianDepthsService`、`towerOfEternityService()`→`TowerOfEternityService`、`abyssLandingService()`→`AbyssLandingService`、`abyssLandingSpecialService()`→`AbyssLandingSpecialService`。

- 同时删除 61 处 `GameLocationBootstrapServices` 废弃 import，补目标 import。

### 3.3 C 类收口（22 个域服务统一模板）

统一为 R1 `DropService.getInstance()` 现状形态：

```java
public static XxxService getInstance() {
    XxxService resolved = resolvedInstance;
    if (resolved != null) {
        return resolved;
    }
    ObjectProvider<XxxService> provider = instanceProvider;
    resolved = provider == null ? null : provider.getIfAvailable();
    if (resolved == null) {
        throw new IllegalStateException("XxxService 未由 Spring 提供："
            + (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
            + "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
    }
    resolvedInstance = resolved;
    return resolved;
}
```

- 16 个 Holder 服务：删 `XxxServiceHolder` 内部类 + 重写 getInstance；
- 6 个已 fail-fast 服务：在既有 fail-fast 基础上加 `resolvedInstance` 字段缓存（热点承接）；
- 22 个统一：`setInstanceProvider` 内 **同时重置 `resolvedInstance = null`**（对齐 DropService:98-101）。

### 3.4 绑定迁移

- 新建 `lifecycle/LocationBootstrapServiceBindings.java` @Component implements DisposableBean：构造器 22×ObjectProvider 逐一 `XxxService.setInstanceProvider(...)`；destroy() 全部置 null（重置缓存由 setInstanceProvider 承担）。
- 删除 `GameLocationBootstrapServices.java`；`GameLocationBootstrapLifecycle` 不动。

---

## 四、动作序列（5 子步，逐步验证）

1. **C 类收口**（22 服务统一模板）→ `build_project`；
2. **B 类批量替换**（19 模式，431 调用）→ build + grep 残留归零 + 抽样测试；
3. **A 类构造注入**（2 服务 + SiegeBattlefieldBeans @Bean 参数 + 字段初始化器特例）→ build；
4. **绑定迁移 + 删定位器**（新组件 + 原类删除 + 测试注入点改造）→ build；
5. **测试改写与分批复跑**（§五清单）→ 全绿。

## 五、测试处置

| 测试 | 处置 |
|---|---|
| `GameLocationBootstrapServicesTest`（bridge 审计，语义反转） | 删除，并新建 `LocationBootstrapServiceBindingsTest`：定位器不回归 / 生产代码零引用 / 绑定组件喂全 22 通道（R1 `CoreGameplayServiceBindingsTest` 同款三断言） |
| `GameServiceProviderCompatibilityTest#gameLocationBootstrapServicesRegistersAndClearsAbyssLandingProvider` | 改名改测 `LocationBootstrapServiceBindings`（去 8 行 assertSame 式旧断言） |
| 其余受影响测试 | 跑测试发现，build 兜底；Holder 删除后的测试引用 grep 先查 |
| 复跑口径 | lifecycle 包 + boot 包 + services 域相关（沿用 R1 分批流程） |

## 六、验收门禁

- **G1 反向 grep**：`grep -rn 'GameLocationBootstrapServices' src/main src/test` 归零（仅剩新审计断言文本与文档）；
- **G2 构建**：build 全绿；
- **G3 测试**：lifecycle + boot + services 相关分批全绿（截断日志 `testFailed=0` 终检）；
- **G4 缓存语义**：22 服务 getInstance 含 `resolvedInstance`；`setInstanceProvider` 重置缓存；20 处热点（VisibleObjectSpawner 等）替换后路径核对；
- **G5 提交**：用户授权后提交（英文 message、显式路径暂存、单 commit 主体）。

## 七、风险与盘点

- **缓存语义承接**：`RESOLVED_SERVICES`（按 provider）→ 各服务 `resolvedInstance`（单字段）；provider 皆为容器内单例，语义等价；`setInstanceProvider` 重置缓存保证重绑定安全。
- **跨域引用**：`VortexService:419` 引用 `GameWorldBootstrapServices.world()`（R3/R4 范围），本批不动，记录待后续批次。
- **Holder 残留检查**：C 类后 `grep -rn 'ServiceHolder' src/main` 仅剩 DisputeLandService（GameFeatureServices 域，R3 范围）；src/test 中 Holder 直接引用为零。
- **测试注入面**：`TestServiceProviders.install(X.class, instance)` 依赖 `setInstanceProvider`（保留不动），仅依赖反射签名不变。
- **本批不动的跨域调用**：services/player 等对 `GameWorldServices`/`GameThreadPoolServices` 等其它定位器的引用（R3-R5 范围）。

## 八、施工记录（2026-10-13）

五子步全部完成；G1/G2/G4 达成，G3 实跑 **23 个测试类全绿**：

- **子步 1（C 类收口）**：22 服务统一为 DropService 模板（`resolvedInstance` 缓存 + `setInstanceProvider` 重置）；16 个 Holder 服务删 `XxxServiceHolder` 改 fail-fast；Siege/Base 去 Lombok `@Setter` 改手写 setter。
- **子步 2（B 类替换）**：77 文件 / 390 调用 → `XxxService.getInstance()`（import 就地改写）。
- **子步 3（A 类构造注入）**：AbyssLandingService 注入 AbyssLandingSpecialService（8 调用）；LandingUpdateService 注入 AbyssLandingService（14 调用，2 个字段初始化器移入构造器体）；SiegeBattlefieldBeans 两个 @Bean 方法加依赖参数。
- **子步 4（绑定迁移）**：新建 `LocationBootstrapServiceBindings`（22 通道 + destroy 清空）；删除定位器；`GameLocationBootstrapLifecycle` 未动。
- **子步 5（测试处置）**：删 `GameLocationBootstrapServicesTest`、建 `LocationBootstrapServiceBindingsTest`；`GameServiceProviderCompatibilityTest` 中该测试改名 `locationBootstrapServiceBindingsRegistersAndClearsProviders`；`GameLegacyServiceBridgeConfigurationTest` 两个测试旧断言（`assertNotSame(getInstance(), bean)`，依赖 Holder 兜底）改为「bean 定义存在 + getInstance fail-fast」；`GameServiceSelfSingletonCleanupTest` 红出的 RiftService 自调 `getInstance()` 以「record 持实例引用」修复（InstanceRiftService 同法）。

**测试矩阵（23 类）**：

| 域 | 类 / 用例数（全 0 失败） |
|---|---|
| lifecycle | GameServiceProviderCompatibility 16、LocationBootstrapServiceBindings 3、GameLocationBootstrapLifecycle 3、GameServiceSelfSingletonCleanup 1、LegacySingletonFallbackAudit 1、CoreGameplayServiceBindings 3、GameWorldBootstrapLifecycle 5、GameSpawnLifecycle 2、GameThreadPoolLifecycle 11 |
| services | ServiceConstructorInjection 48、GameLegacyServiceBridgeConfiguration 56、ServiceInternalCollectionImplementation 6、ServiceMapImplementation 6、TemporaryVisibleObjectMap 6、BaseService 1、RiftService 2、OutpostService 1、SiegeService 2、TowerOfEternityService 2、ScheduleHotReload 1 |
| boot | LocalizedLogCalls 1、LocalizedLogArguments 2、GameServiceLifecycle 3 |

**观察项（记录给后续批次）**：

- B 类替换后 8 个服务类内仍有自调 `XxxService.getInstance()`（Vortex 2、Agent 6、Svs 5、NightmareCircus 2、ZorshivDredgion 9、Moltenus 5、Conquest 2、Siege 1，共 32 处）；Rift/InstanceRift 已按自调门禁整改为实例引用，其余可在后续批次扩展 `GameServiceSelfSingletonCleanupTest` 清单时一并清理。
- 全库 `getIfAvailable(() ->` 残留 9 处（NetworkController、SkillEngine、PathService、ChatProcessor、LifeStatsRestoreService、RiftManager、DisputeLandService、BonusService、ChatProcessRuntimeBridge），均属后续批次域。

---

## 附表 A：A 类 2 个（注入目标 → 消费方）

| # | 注入目标 | 消费方 | 调用点 | 行号 |
|---|---|---|---:|---|
| 1 | `AbyssLandingSpecialService` | `services/AbyssLandingService.java` | 8 | 502-558 |
| 2 | `AbyssLandingService` | `services/abysslandingservice/LandingUpdateService.java` | 14 | 23-24、83-146 |

## 附表 B：B 类 77 个（按目录）

**ai（26）**：`agentFight/Empowered_MastariusAI2`、`Empowered_VeilleAI2`；`beritraInvasion/BenoidAI2`、`MagnorionAI2`、`TerracrusherAI2`、`TumonAI2`、`Wurg_The_GlacierAI2`；`rvr/asmodianWarshipInvasion/Suminid_ComanderAI2`、`Taina_ComanderAI2`、`Vasad_ComanderAI2`；`rvr/elyosWarshipInvasion/Luluran_ComanderAI2`、`Mandos_ComanderAI2`、`Nanabel_ComanderAI2`；`worlds/inggison/Dredgion_CommanderAI2`；`worlds/levinshor/ZorshivCommanderAI2`；`worlds/reshanta/abyssLanding/Annihilation_MonumentAI2`、`Defeat_MonumentAI2`、`Harbinger_EquilibriumAI2`、`Redemption_LightAI2`、`Siege_CommanderAI2`、`Spirit_MonumentAI2`、`Stolen_Harbinger_CallAI2`、`Stolen_Light_Of_RedemptionAI2`；`worlds/reshanta/worldBoss/krotan/Spirit_Of_KrotanAI2`、`kysis/Spirit_Of_KysisAI2`、`miren/Spirit_Of_MirenAI2`。

**commands/admin（16）**：Agent、Anoha、Beritra、Conquest、DynamicRift、IdianDepths、InstanceRift、Invasion、Iu、Landing、Moltenus、NightmareCircus、Rift、Rvr、Svs、ZorshivDredgion。

**services（29）**：`InstanceRiftService`（自调）、`QuestService`、`RiftService`（自调）、`agentservice/AgentFight`、`anohaservice/AnohaStartRunnable`、`BerserkAnoha`、`base/Base`、`BaseBossDeathListener`、`beritraservice/BeritraInvasion`、`BeritraStartRunnable`、`conquestservice/ConquestOffering`、`dynamicriftservice/DynamicRift`、`idiandepthsservice/IdianDepths`、`instanceriftservice/RiftInstance`、`iuservice/Iu`、`moltenusservice/MoltenusFight`、`nightmarecircusservice/CircusInstance`、`outpost/Outpost`、`OutpostBossDeathListener`、`player/PlayerEnterWorldService`、`PlayerReviveService`、`rvrservice/RvrStartRunnable`、`Rvrlf3df3`、`siegeservice/ArtifactSiege`、`FortressSiege`、`svsservice/Panesterra`、`towerofeternityservice/TowerOfEternity`、`vortexservice/DimensionalVortex`、`zorshivdredgionservice/ZorshivDredgion`。

**其它（6）**：`configs/Config`、`controllers/RVController`、`model/team2/alliance/PlayerAllianceService`、`network/aion/clientpackets/CM_ABYSS_LANDING`、`CM_LEVEL_READY`、`spawnengine/VisibleObjectSpawner`。
