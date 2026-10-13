# R3 批次施工单：GameFeatureServices 静态定位器退役

> 依据：`docs/architecture-optimization/2026-10-10-lifecycle-locator-retirement-plan.zh-CN.md`（R3 行：计划口径 692 引用）
> 前置：R1（CoreGameplay，`4ad05b991`）与 R2（LocationBootstrap，`4562f39d7`）已完成并提交。本批沿用 R1/R2 已验证的流程、fail-fast getInstance 模板与测试注入套路。

---

## 一、范围与终态

**目标定位器**：`lifecycle/GameFeatureServices.java`（637 行，@Component 纯绑定定位器）：
- 构造器持 30×`ObjectProvider` 参数（PlayerLimitService、VeteranRewardsService、BGService 仅绑定、无访问器），逐一 `setInstanceProvider` 并写入静态字段；`destroy()` 反向清空；
- 27 个静态访问器：26 个有调用（共 658 处），rewardService 为 **0 调用**；
- 访问器三种形态：4 个自带 `resolved*` 缓存（npcShouts/siege/base/staticDoor）、18 个走 `requireService`（无缓存）、5 个内联 fail-fast（ffa/ladder/kisk/systemMail/pet）——**23 个访问器无缓存**，每次调用穿 Spring 单例注册表全局锁（`npcShoutsService()` 365 处为最热点）。

**终态**：
1. 27 个静态访问器全部退役；消费方改走域服务自持 `XxxService.getInstance()`（统一模板、全带缓存）；rewardService（0 调用）随定位器一并删除，f2pService 2 处消费改走 `F2pService.getInstance()`；
2. 30 个域服务 `getInstance()` 统一为 R1 `DropService` 模板（28 个加 `resolvedInstance` 缓存；DisputeLandService 另删 Holder 残留）；
3. 绑定职责迁入新建 `FeatureServiceBindings` @Component（30×ObjectProvider 构造喂入 + destroy 清空）；定位器类删除；
4. 测试改写：`GameServiceProviderCompatibilityTest` 该域方法适配；`AionBootApplicationTest` 装配断言指向新组件。

**A 类判定 = 0（本批无构造注入子步）**：消费方扫描（`@Component/@Service/@Configuration` 类级注解）仅命中定位器自身；服务间消费仅 4 处且全部位于方法体内（见 §2.3）→ 统一 B 类直换；无 R2 的「字段初始化器强制构造注入」场景（缩进启发扫描不可靠，由 build/测试兜底，参照 R2 LandingUpdateService 教训）。

---

## 二、现状核实（2026-10-13）

### 2.1 引用总量与分布

- `GameFeatureServices.` 引用 **684 行** = import **189 文件** + 外部访问器调用 **658 处** + 定位器类内 provider 自赋值。
- 访问器调用明细（26 个）：

| 访问器 | 调用数 | 访问器 | 调用数 |
|---|---:|---|---:|
| `npcShoutsService` | 365 | `repurchaseService` | 5 |
| `siegeService` | 87 | `atreianBestiaryService` | 5 |
| `baseService` | 59 | `asyunatarService` | 5 |
| `systemMailService` | 25 | `shieldService` | 4 |
| `petService` | 15 | `dropDistributionService` | 4 |
| `kiskService` | 11 | `disputeLandService` | 3 |
| `ladderService` | 10 | `banditService` | 3 |
| `ffaService` | 9 | `staticDoorService` | 2 |
| `protectorConquerorService` | 8 | `growthEnergy` | 2 |
| `motionLoggingService` | 8 | `f2pService` | 2 |
| `dredgionService` | 8 | `windyGorgeService` | 1 |
| `arcadeUpgradeService` | 8 | `coalescenceService` | 1 |
| `aStationService` | 7 | `bonusService` | 1 |

- 消费方目录分布（二级目录，完整清单由替换脚本运行时输出核对）：ai/instance 69、network/aion 21、ai/worlds 15、services 9、instance/handlers 8、ai/rvr 8、commands/admin 6、services/siegeservice 5、services/events 3、model/siege 3、controllers 3、ai/siege 3、其余 services/*、model/*、ai/*、skillengine、spawnengine、geoEngine、eventEngine、world/zone、configs、commands/player 等零散项。

### 2.2 30 个域服务通道形态

- **28 个无缓存 fail-fast**（每次调用穿 provider）——统一模板将加 `resolvedInstance`；其中 **DisputeLandService 为唯一 Holder 残留**（`DisputeLandServiceHolder.INSTANCE` + `getIfAvailable(() ->`，即全库 9 处 `getIfAvailable(() ->` 残留之一，且在 `LegacySingletonFallbackAuditTest` RETIRED 清单内）；
- Siege/Base 已带缓存（R2 批次完成）；
- 30 个全部已有手写 `setInstanceProvider`；
- 既有自调：30 个服务文件内各 1 处 `XxxService.getInstance()`（R0/D1 遗留，非本批产生；本批替换后 SystemMailService 自调 +1）。

### 2.3 特例

- **服务间消费 4 处**（全方法体内）：`RewardService:87`（→systemMail）、`VeteranRewardsService:283`（→systemMail）、`LadderService:422/733`（→ffa）、`SystemMailService:387`（**自调**）→ 统一直换 `XxxService.getInstance()`。
- 定位器访问器形态差异（4 缓存 / 18 requireService / 5 内联 fail-fast）随类删除，无需逐一保留。

### 2.4 装配与测试现状

- 定位器为 @Component 由容器构造；本域无独立 Fallbacks 类（`requireService` 内联 fail-fast 即兜底载体，与 R2 结论一致）；无对应 `*Lifecycle` 类。
- 测试引用 2 处：
  - `GameServiceProviderCompatibilityTest#gameFeatureServicesRegistersAndClearsPlayerActionProviders`（934-1055 行）：30 通道装配 + 混合断言（部分已是 `X.getInstance()` 形态）；
  - `AionBootApplicationTest:194`：`assertEquals(GameFeatureServices.class, context.getType("gameFeatureServices"))`。
- 计划文档所称 `GameFeatureServicesRuntimeBridgeTest` 已不存在（全库无此文件）。

---

## 三、处置规则

### 3.1 C 类收口（30 服务统一模板）

统一为 R1 `DropService` 形态（同 R2 §3.3）：

- 28 个无缓存服务：加 `private static volatile XxxService resolvedInstance;` + 缓存版 getInstance + `setInstanceProvider` 重置；
- DisputeLandService：删除 `DisputeLandServiceHolder` 与 `getIfAvailable(() ->` 分支，改 fail-fast + 缓存；
- Siege/Base：已达标，不动。

### 3.2 B 类（消费方 → 26 个映射）

`GameFeatureServices.{name}()` → `{Svc}.getInstance()`（脚本批量 + 计数断言 + grep 归零 + build 兜底，同 R1/R2）；import 就地改写；rewardService（0 调用）随定位器删除。映射见附表 A。

**注意**：`systemMailService()` 在 SystemMailService 内的自调点同样替换为 `SystemMailService.getInstance()`（与该服务既有自调形态共存）。

### 3.3 绑定迁移

- 新建 `lifecycle/FeatureServiceBindings.java` @Component implements DisposableBean：构造器 30×ObjectProvider 逐一 `setInstanceProvider`；destroy() 全部置 null（重置缓存由 setInstanceProvider 承担）；
- 删除 `GameFeatureServices.java`；测试注入点同步改造（§3.4）。

### 3.4 测试处置

| 测试 | 处置 |
|---|---|
| `GameServiceProviderCompatibilityTest#gameFeatureServicesRegistersAndClearsPlayerActionProviders` | 改名 `featureServiceBindingsRegistersAndClearsProviders`；`new GameFeatureServices` → `new FeatureServiceBindings`；残留 `GameFeatureServices.xxx()` 断言改 `XxxService.getInstance()` |
| `AionBootApplicationTest:194` | `gameFeatureServices` bean 断言 → `featureServiceBindings`（含 import 调整） |
| 其余受影响测试 | 跑测试发现，build 兜底 |
| 复跑口径 | lifecycle + services 相关分批（沿用 R1/R2 流程），全绿 |

---

## 四、动作序列（4 子步，逐步验证）

1. **C 类收口**（28 服务统一模板 + DisputeLand 删 Holder）→ `build_project`；
2. **B 类批量替换**（25 映射 / 656 调用 / 189 import）→ build + grep 残留归零 + 抽样；
3. **绑定迁移 + 删定位器**（FeatureServiceBindings 新建 + 原类删除 + 测试 2 处改写）→ build；
4. **测试改写与分批复跑** → 全绿 → 提交授权。

（A 类 = 0，无构造注入子步。）

## 五、验收门禁

- **G1 反向 grep**：`grep -rn 'GameFeatureServices' src/main src/test` 仅剩新审计断言文本与文档；
- **G2 构建**：build 全绿；
- **G3 测试**：lifecycle + services 相关分批全绿（截断日志 `testFailed=0` 终检）；
- **G4 缓存语义**：30 服务 getInstance 含 `resolvedInstance`；npcShouts（365 处热点）替换后路径核对；
- **G5 提交**：用户授权后提交（英文 message、显式路径暂存、单 commit 主体）。

## 六、风险与盘点

- **热点承接**：23 个无缓存访问器（npcShouts 365 处最热）→ 服务自持 `resolvedInstance` 缓存；语义等价、穿锁消除；
- **门禁**：DisputeLandService 删 Holder 后 `LegacySingletonFallbackAuditTest` 的 RETIRED 断言（不得含 `getIfAvailable(() ->`）满足；
- **自调观察**：30 服务既有 30 处 + 本批 SystemMailService 自调 +1；不在 `GameServiceSelfSingletonCleanupTest` 3 服务门禁名单内，记录留后续清理批次；
- **计划表外大域**：`GameEngineServices`（1232 行/415 文件）与 `GameWorldBootstrapServices`（1070 行/375 文件）不在 2026-10-10 计划的 R3/R4/R5 表中——本批完成后盘点其与 R4/R5 的关系，建议作为独立勘察批次；
- **契约面**：`TestServiceProviders.install` 依赖 `setInstanceProvider` 签名（保留不动）。

---

## 八、施工记录（2026-10-13）

四子步全部完成；G1/G2/G4 达成，G3 实跑 **35 个测试类全绿**：

- **子步 1（C 类收口）**：28 服务统一 DropService 模板（`resolvedInstance` 缓存 + `setInstanceProvider` 重置）；其中 **15 个 Lombok 字段级 `@Setter` 族**（AStation、F2p、WindyGorge、MotionLogging、StaticDoor、Kisk、Repurchase、DropDistribution、SystemMail、Bonus、Pet、ArcadeUpgrade、AtreianBestiary、Coalescence、GrowthEnergy）删 `-- SETTER --` javadoc 段 + `@Setter` 注解 + `import lombok.Setter`（MotionLoggingService 保留 import——另有 4 处其它字段使用），改手写 setter；13 个手写 setter 族直接补 `resolvedInstance = null;`；2 个 `public static final` getInstance（VeteranRewards、FFA）去 final 统一签名；DisputeLandService 删 `DisputeLandServiceHolder` + `getIfAvailable(() ->` 分支 + javadoc 换标准 fail-fast 文本（全库 `ServiceHolder` 归零）；BonusService 删本地兜底实例 `private static final BonusService instance = new BonusService();`（`@NoArgsConstructor` 保留——`TaskAndStatBeans.bonusService()` @Bean 仍用无参构造）+ 三处 javadoc 换标准文本。累计 57 个断言全过。
- **子步 2（B 类替换）**：**190 文件 / 658 调用**（26 映射）→ `XxxService.getInstance()`；import 就地改写（189 精确 import + `PlayerController` 通配 `lifecycle.*` 场景插 2 行精确 import）；`GameFeatureServices.` 在 src/main 归零（仅剩定位器自身）。
- **子步 3（绑定迁移）**：新建 `lifecycle/FeatureServiceBindings.java`（215 行，30×ObjectProvider 构造 + destroy 30 绑定；构造器/destroy 从旧定位器机械提取，仅剔除静态字段赋值段）；删除 `GameFeatureServices.java`。首版生成两处笔误已修复：构造器签名首参被整行替换丢失（改为子串替换）、destroy 残留 31 行旧静态字段清空（补过滤）。
- **子步 4（测试处置）**：`GameServiceProviderCompatibilityTest` 19 处访问器断言改 `X.getInstance()`、5+3 行冗余双断言删除（`assertSame(x, X.getInstance())` 已与前一行重复）、2 构造改 `new FeatureServiceBindings(...)`、方法改名 `featureServiceBindingsRegistersAndClearsProviders`；destroy 后 `assertNotSame(bonusService, BonusService.getInstance())`（依赖旧本地兜底实例语义）改 `assertThrows(IllegalStateException.class, BonusService::getInstance)`；`AionBootApplicationTest` import + 装配断言改 `featureServiceBindings`；新建 `FeatureServiceBindingsTest`（R1/R2 同款三断言：定位器不回归 / 生产代码零引用 / 30 通道喂入）。

**测试矩阵（35 类，全 0 失败）**：

| 域 | 类 / 用例数 |
|---|---|
| lifecycle | GameServiceProviderCompatibility 16、FeatureServiceBindings 3、CoreGameplayServiceBindings 3、LocationBootstrapServiceBindings 3、GameServiceSelfSingletonCleanup 1、LegacySingletonFallbackAudit 1、GameDisputeLandLifecycle 3、GameDredgionLifecycle 5、GameRewardServicesLifecycle 4、GameProtectorConquerorLifecycle 3、GameEventRuntimeLifecycle 4 |
| services | GameLegacyServiceBridgeConfiguration 56、ServiceConstructorInjection 48、ServiceInternalCollectionImplementation 6、ServiceMapImplementation 6、ServiceCollectionImplementation 3、TemporaryVisibleObjectMap 6、AStationService 3、BaseService 1、KiskService 1、ProtectorConquerorService 2、SiegeService 2、DropDistributionService 1、LadderService 3、SystemMailService 2、PlayerLimitService 2、WebshopService 1、ScheduleHotReload 1 |
| model / persistence / questEngine / dataholders | ModelCollectionImplementation 18、TransactionSafety 5、ExternalRewardAdvanceReentryContract 2、XmlDataLoader 25 |
| boot | AionBootApplication 13、LocalizedLogCalls 1、LocalizedLogArguments 2 |

**观察项（记录给后续批次）**：

- 全库 `getIfAvailable(() ->` 残留由 9 处降至 **7 处**（NetworkController、SkillEngine、PathService、ChatProcessor、LifeStatsRestoreService、RiftManager、ChatProcessRuntimeBridge），均属后续批次域。
- `BonusService.getInstance(ItemGroupsData)` 全库无调用（死方法候选，重构前即存在），留独立清理批次评估（涉及 itemGroups 覆盖语义，谨慎处理）。
- 调研口径修正：`f2pService()` 实测 2 处调用（原误记 0）：`F2pAction:63`、`PlayerEnterWorldService:612`；总替换数 656 → **658**。

---

## 附表 A：26 个访问器 → 目标服务映射

| # | 访问器 | 目标服务（getInstance） | 调用数 |
|---|---|---|---:|
| 1 | `npcShoutsService()` | `com.aionemu.gameserver.services.NpcShoutsService` | 365 |
| 2 | `siegeService()` | `com.aionemu.gameserver.services.SiegeService` | 87 |
| 3 | `baseService()` | `com.aionemu.gameserver.services.BaseService` | 59 |
| 4 | `systemMailService()` | `com.aionemu.gameserver.services.mail.SystemMailService` | 25 |
| 5 | `petService()` | `com.aionemu.gameserver.services.toypet.PetService` | 15 |
| 6 | `kiskService()` | `com.aionemu.gameserver.services.KiskService` | 11 |
| 7 | `ladderService()` | `com.aionemu.gameserver.services.events.LadderService` | 10 |
| 8 | `ffaService()` | `com.aionemu.gameserver.services.events.FFAService` | 9 |
| 9 | `protectorConquerorService()` | `com.aionemu.gameserver.services.ProtectorConquerorService` | 8 |
| 10 | `motionLoggingService()` | `com.aionemu.gameserver.services.MotionLoggingService` | 8 |
| 11 | `dredgionService()` | `com.aionemu.gameserver.services.instance.DredgionService2` | 8 |
| 12 | `arcadeUpgradeService()` | `com.aionemu.gameserver.services.events.ArcadeUpgradeService` | 8 |
| 13 | `aStationService()` | `com.aionemu.gameserver.services.AStationService` | 7 |
| 14 | `repurchaseService()` | `com.aionemu.gameserver.services.RepurchaseService` | 5 |
| 15 | `atreianBestiaryService()` | `com.aionemu.gameserver.services.player.AtreianBestiaryService` | 5 |
| 16 | `asyunatarService()` | `com.aionemu.gameserver.services.instance.AsyunatarService` | 5 |
| 17 | `shieldService()` | `com.aionemu.gameserver.services.ShieldService` | 4 |
| 18 | `dropDistributionService()` | `com.aionemu.gameserver.services.drop.DropDistributionService` | 4 |
| 19 | `disputeLandService()` | `com.aionemu.gameserver.services.DisputeLandService` | 3 |
| 20 | `banditService()` | `com.aionemu.gameserver.services.events.BanditService` | 3 |
| 21 | `staticDoorService()` | `com.aionemu.gameserver.services.StaticDoorService` | 2 |
| 22 | `growthEnergy()` | `com.aionemu.gameserver.services.player.GrowthEnergy` | 2 |
| 23 | `f2pService()` | `com.aionemu.gameserver.services.F2pService` | 2 |
| 24 | `windyGorgeService()` | `com.aionemu.gameserver.services.WindyGorgeService` | 1 |
| 25 | `coalescenceService()` | `com.aionemu.gameserver.services.item.CoalescenceService` | 1 |
| 26 | `bonusService()` | `com.aionemu.gameserver.services.reward.BonusService` | 1 |

（0 调用随类删除：`rewardService()` → `services.reward.RewardService`。）

## 附表 B：30 个域服务清单（C 类收口对象）

**28 个加缓存**：DisputeLandService、DredgionService2、AsyunatarService、PlayerLimitService、NpcShoutsService、ShieldService、RewardService、VeteranRewardsService、ProtectorConquerorService、FFAService、LadderService、BGService、BanditService、AStationService、F2pService、WindyGorgeService、MotionLoggingService、StaticDoorService、KiskService、RepurchaseService、DropDistributionService、SystemMailService、BonusService、PetService、ArcadeUpgradeService、AtreianBestiaryService、CoalescenceService、GrowthEnergy。
**已达标（不动）**：SiegeService、BaseService。
**Holder 删除**：DisputeLandService（唯一）。
