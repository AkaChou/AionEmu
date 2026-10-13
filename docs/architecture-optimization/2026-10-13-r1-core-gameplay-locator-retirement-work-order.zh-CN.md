# R1 批次施工单：GameCoreGameplayServices 静态定位器退役

> 状态：待审阅；审阅通过后开工
> 依据：`2026-10-10-lifecycle-locator-retirement-plan.zh-CN.md` §三 R1（引用量升序第一域，兼作处置规则验证批）
> 前置：D1a `69ff740eb`、D1b `4026415fa` 已完成

---

## 一、范围与终态

唯一对象：`com.aionemu.gameserver.lifecycle.GameCoreGameplayServices`（@Component 静态定位器：8 个成员方法、152 个 main 调用点 / 85 个文件）。

终态（可机验）：

1. `grep -rn 'GameCoreGameplayServices' src/main src/test` 归零（`docs/` 历史文档除外）；
2. B 类静态宿主改经域服务自持 fail-fast 通道 `XxxService.getInstance()`（单一所有权）；
3. A 类 Spring bean 消费方构造注入目标域服务，静态调用删除；
4. 定位器降格为绑定组件 `CoreGameplayServiceBindings`（仅承接 8 次 `setInstanceProvider` 喂入 + 销毁清理，不暴露任何静态访问方法），原类删除；
5. **C 类补收口**：`BalaurAssaultService` / `BattlefieldUnionService` 的 `getInstance()` 删除 `instance` 静态字段兜底，收口为 fail-fast（与其余 6 个域服务对齐）。

不在本批范围：8 域服务内部对其余定位器（GameFeatureServices / GameWorldServices / GameWorldBootstrapServices / GameThreadPoolServices / GameRuntimeServices / GameHousingServices / GameEngineServices / GameBattlefieldServices / GameTaskManagerServices）的调用——留待 R2–R5（盘点见 §七 R4）。

## 二、现状核实（2026-10-13）

```
消费方 152 处 / 85 文件
  ├─ A 类 20：@Configuration @Bean 创建的 Spring 域服务
  └─ B 类 65：packet 25 / instance-script 13 / 纯静态服务与工具类 15 / 命令 4 / model·ai·controller 8
        │  调用
        ▼
GameCoreGameplayServices（@Component：构造器 8×ObjectProvider 写静态字段 + 转发 setInstanceProvider）
        │  转发 provider.getObject()
        ▼
8 域服务：Drop / Mail / Pvp / AutoGroup / AbyssRankingCache / Legion / BalaurAssault / BattlefieldUnion
  └─ 各自 fail-fast getInstance()（6/8 已收口；2/8 残留 instance 兜底，见 §一.5）
```

关键事实：

- `GameCoreServiceFallbacks` 已不存在（D1a 已清，本批无 fallbacks 层）；
- 8 个域服务**仅**在 GameCoreGameplayServices 一个定位器暴露（范围闭环，无跨定位器同域成员）；
- 域服务 `getInstance()` 在 main 中现存调用为 0——本批首次建立该通道用法；
- 绑定喂入现由定位器构造器承担；`CoreRuntimeServiceBeans` / `EngineBeans` / `SiegeBattlefieldBeans` 是 8 域服务与 A 类消费方的 @Bean 宿主。

调用分布与 A/B 拆分：

| 方法 | 调用数 | A 类文件 | B 类文件 |
|---|---|---|---|
| legionService | 61 | 5 | 29 |
| autoGroupService | 43 | 11 | 22 |
| dropService | 16 | 2 | 6 |
| battlefieldUnionService | 11 | 0 | 3 |
| mailService | 8 | 0 | 7 |
| abyssRankingCache | 7 | 2 | 2 |
| balaurAssaultService | 4 | 0 | 2 |
| pvpService | 2 | 0 | 1 |
| 合计 | 152 | 20 | 65 |

## 三、处置规则

### 3.1 A 类（20 个 bean 消费方 → 构造注入）

- 在各 `*Beans` @Configuration 的 @Bean 方法签名追加目标域服务参数并传入构造器；消费方改字段构造器注入，静态调用删除；清理未用 import（build 兜底）。
- 清单见附表 A；涉及装配类：`CoreRuntimeServiceBeans` / `EngineBeans` / `SiegeBattlefieldBeans`。
- 特例：
  1. **AbyssRankingCache**（@Bean，需 legionService）：装配层单向依赖（LegionService 不依赖本类；`legionRanking` 由运行时 `performRankingUpdate(Map)` 注入），@Bean 方法加参数即可，**不成环**；
  2. **LegionMemberDaoImpl**（@Service 组件，3 处）：与 LegionService 存在构造器环（LegionService 构造注入 LegionMemberDao）。本批这 3 处改走 `LegionService.getInstance()`（归 B 类处理），不做构造注入；
  3. **SiegeService 内部 `new FortressSiege(...)` / `new ArtifactSiege(...)`**（`SiegeService.java:417/420`）：两子类非 Spring bean；推荐由 SiegeService（已注入 legionService）经构造参数传递，或子类内走 `getInstance()`——施工时据构造器签名为准；
  4. **Siege 基类 / SiegeAutoRace**：grep 未见直接 `new`，施工时先核实创建点再归位（预期同为构造传参或 getInstance()）。

### 3.2 B 类（65 个静态宿主 → 域服务 getInstance() 通道）

替换模板（逐文件机械替换）：

```
GameCoreGameplayServices.legionService()      →  LegionService.getInstance()
GameCoreGameplayServices.autoGroupService()   →  AutoGroupService.getInstance()
GameCoreGameplayServices.dropService()        →  DropService.getInstance()
GameCoreGameplayServices.mailService()        →  MailService.getInstance()
GameCoreGameplayServices.pvpService()         →  PvpService.getInstance()
GameCoreGameplayServices.abyssRankingCache()  →  AbyssRankingCache.getInstance()
GameCoreGameplayServices.balaurAssaultService()      →  BalaurAssaultService.getInstance()
GameCoreGameplayServices.battlefieldUnionService()   →  BattlefieldUnionService.getInstance()
```

- 逐文件补 import（`services.*` / `services.siegeservice.*` / `services.abyss.*`）；
- 批量纪律（见「批量替换三大陷阱」）：模板规则化、before/after 计数复验、替换后 grep 残留归零、build 全量兜底；
- 行为等价：定位器转发 `getObject()`（fail-fast），`getInstance()` 为 `getIfAvailable()` + null 校验抛 ISE——缺失场景均 fail-fast，等价。

### 3.3 绑定迁移（定位器降格）

- 新建 `lifecycle/CoreGameplayServiceBindings`（@Component）：构造器签名与原定位器一致（8×ObjectProvider），构造内完成 8 次 `setInstanceProvider` 与缓存清理；销毁（`@PreDestroy`）完成清空与置 null；
- 删除 `GameCoreGameplayServices` 类；
- 测试注入点同步改造（见 §五）。

### 3.4 C 类收口（2 个域服务）

- `BalaurAssaultService` / `BattlefieldUnionService`：`getInstance()` 删除 `provider.getIfAvailable(() -> instance)` 与 `return instance` 分支，对齐其余 6 个域服务的 fail-fast 形态（`instanceProvider == null → ISE`；`getIfAvailable()` 为 null → ISE）；确认零引用后删除 `private static final ... instance` 字段。

## 四、动作序列（5 子步，逐步验证）

1. **C 类收口**（2 类）→ build + 相关测试；
2. **B 类批量替换**（65 文件）→ build + grep 残留归零 + 抽样测试；
3. **A 类构造注入**（20 类 + 3 个 Beans 类，含 4 个特例）→ build；
4. **绑定迁移 + 删除定位器**（新组件 + 原类删除 + 测试注入点改造）→ build；
5. **测试改写与全量复跑**（§五清单）→ 全绿。

## 五、测试处置

| 测试 | 处置 |
|---|---|
| `GameCoreGameplayServicesTest`（5 个 bridge 格式断言） | 断言从"源码不得出现 `XxxService.getInstance()`"改为"不得出现 `GameCoreGameplayServices.`"+ 相应 `getInstance()` 通道断言；或并入新绑定组件测试 |
| `GameServiceProviderCompatibilityTest`（9 处） | `gameCoreGameplayServicesRegistersAndClearsCoreOnlineProviders` 改测 `CoreGameplayServiceBindings`；其余 accessor 断言复核 |
| `RetailConditionSpawnEngineTest`（3 处） | `new GameCoreGameplayServices(...)` → `new CoreGameplayServiceBindings(...)` |
| `AionBootApplicationTest`（2 处） | bean 类型/名称断言改为 `CoreGameplayServiceBindings` |
| `GameLegacyServiceBridgeConfigurationTest` | 已核实无引用（0 处），无需改 |
| `retiredSingletonAccessorsFailFastWithoutSpringProvider` | 扩展断言覆盖收口后的 BalaurAssault / BattlefieldUnion（C 类验收） |

## 六、验收门禁

- **G1 归零**：`grep -rn 'GameCoreGameplayServices' src/main src/test` 归零；
- **G2 编译**：全量零错误；
- **G3 测试**：lifecycle 包 + boot 包 + `services` 域相关测试全绿（授权后按 D1b 同款 IDEA MCP 分批流程）；
- **G4 行为等价**：8 域服务 fail-fast 语义保持；绑定组件构造/销毁行为与原定位器一致（provider 喂入 + 缓存清理）；
- **G5 提交**：英文 message、显式路径暂存、单 commit（用户授权后）。

## 七、风险与盘点

- **R1 循环装配**：LegionMemberDaoImpl 走 `getInstance()` 规避；AbyssRankingCache 已核单向不成环；
- **R2 批量替换陷阱**：import 遗漏（替换后必须 build 兜底）；模式限 `GameCoreGameplayServices\.` 前缀，机械规则化；多模式 grep 用 search_regex；
- **R3 测试源码文本断言**：`GameCoreGameplayServicesTest` 是读源码字符串的 bridge 审计测试，替换后必须同步改写（否则假绿）；
- **R4 跨域盘点（本批不动）**：8 域服务内部对 GameFeatureServices / GameWorldBootstrapServices / GameWorldServices / GameThreadPoolServices / GameRuntimeServices / GameHousingServices / GameEngineServices / GameBattlefieldServices / GameTaskManagerServices 的调用留待 R2–R5；
- **R5 实机验证**：由用户重启服务端确认（不在 AI 权限内）。

## 附表 A：A 类 20 个（注入目标 → 消费方）

| 注入目标 | 消费方 |
|---|---|
| legionService | RenameService、PeriodicSaveService、SiegeService、TerritoryService、AbyssRankingCache |
| autoGroupService | SuspiciousOphidanBridgeService、IdgelDomeLandmarkService、HallOfTenacityService、DredgionService2、GrandArenaTrainingCampService、IDRunService、IdgelDomeService、KamarBattlefieldService、IronWallWarfrontService、AsyunatarService、EngulfedOphidanBridgeService |
| dropService | DropDistributionService、DropRegistrationService |
| abyssRankingCache | AbyssRankCleaningService、AbyssRankUpdateService |

（合计 20）

## 附表 B：B 类 65 个（按目录）

| 目录 | 数量 | 代表 |
|---|---|---|
| network/aion（clientpackets + serverpackets） | 25 | CM_LEGION（11 处）、CM_BATTLEFIELD_UNION_REGISTER（8 处） |
| instance/handlers/scripts | 13 | Dredgion 系列、IdgelDome 系列、PvPArena 系列 |
| services（siegeservice/player/item/instance + DialogService） | 15 | Siege、FortressSiege、SiegeAutoRace、ArtifactSiege、PlayerEnterWorldService、PlayerLeaveWorldService 等纯静态类 |
| commands/admin | 4 | SiegeCommand、PlayerInfo、Petitions、LegionCommand |
| model（team2 / autogroup / gameobjects） | 6 | PlayerGroupService、PlayerAllianceService、LeagueService、Mailbox |
| controllers / ai | 4 | PlayerController、NpcController、QuestItemNpcAI2、ChestAI2 |
| persistence/dao | 1 | LegionMemberDaoImpl（3 处，规避构造环） |
