# Spring 迁移遗留清单与治理（2026-09-17）

> 主题：早期 Spring Boot 改造"未彻底完成"的遗留盘点、冻结护栏与退役路线。
> 结论先行：框架层（Bean/组件装配）已迁完；遗留集中在 ①双源静态兜底 ②配置层 ③DAO 层 ④过渡网关/桥接层。
> 静态门面调用点（2265/823）已有缓存解析、按 AR-001 达标，**不搬**。

## 一、已迁完（不要重复改造）

| 面 | 证据 | 结论 |
|---|---|---|
| 框架装配 | `@Bean` 181 / `@Component` 143 / `@Configuration` 10 / `@Autowired` 269 | 完成 |
| SPI | 已废除（memory-bank AR-002） | 完成，不再引入 |
| 启动 | 干净实例实测 15–25 s；刷怪按 10 核并行 | 非问题 |
| 静态门面 | `GameThreadPoolServices.` 2265 处、`GameEngineServices.` 823 处调用；解析结果已缓存 | AR-001 达标，**不搬**（零收益、超大 diff） |
| 运行时代价 | play-16 JFR：Spring 查找 ≈0；`DependencyObjectProvider$2` 仅 PacketPool 低频 | 非问题 |

## 二、遗留清单（按处理顺序）

### ① 双源静态兜底（本期在做）

- 形态：`provider.getIfAvailable(() -> SingletonHolder.instance)`——Spring 优先、静态兜底。
- 规模：源码中 `getIfAvailable(() ->` 共 268 处，其中 **0 处带 `SingletonHolder` 兜底**
  （原 131 处已全部退役：`InGameShopEn` 试点 + A 组 16 + B 组 9 + 第三批 105）。
- 另有 15 个文件含 `SingletonHolder` 字样，属于 chat/login/commons 侧纯静态持有，不是双源兜底，不在本期范围。
- 危害：兜底一旦被走到，就在容器之外静默创建第二套实例（两套状态），且不在启动期暴露。
- 现状：全部有 provider 注入点（手写 `setInstanceProvider` 或 Lombok `@Setter`），无孤儿；但每个类都至少被 1 个测试文件直接调用 `getInstance()`（`GameServiceProviderCompatibilityTest` 165 处调用、387 条断言），必须逐类迁移、同步改测试。
- 治理：`LegacySingletonFallbackAuditTest` 现冻结"零回落"：131 个退役文件清单 + 全库回落点必须为 0；
  新增兜底会同时触发数量断言与退役类断言。
- 批量计划：先按"除 `GameServiceProviderCompatibilityTest` 外还有多少测试文件直接调用 `getInstance()`"分批，
  再用"是否经 `*Fallbacks` 静态持有者回落"细化：
  - A 组（facade 直接 `getIfAvailable(provider, X::getInstance)`）：与试点同形，可机械退役；已完成 16 类。
  - B 组（经 `*Fallbacks` 的 eager 静态持有者回落）：缺少 provider 时首个回落会抛
    `ExceptionInInitializerError` 并污染该持有者类；已按"fallback 条目直接 `X.getInstance()`"收敛（9 类，见下）。
  - 每批统一执行：改 fail-fast、删兜底、同步改测试、下调审计常量、聚焦测试授权验证。
  - 工具：`.agents/summary/architecture-performance-refactor/retire_singleton_fallback.py`（dry-run 默认，`--apply` 落盘）。

### ② 配置层半迁

- 游戏/登录/聊天各保留一份静态 `Config.load()`（`ConfigurableProcessor.process` 扫描式装配）；`Config.load()` 有 4 个文件 5 处调用点。
- `SecurityConfig`(46 处引用) / `ThreadConfig`(21) / `IPConfig`(7) / `SvStatsConfig`(4) 仍是静态字段读取。
- `@ConfigurationProperties` 仅 5 个，且都在 boot 层。
- 计划（第二步）：先迁这四个配置类为 `@ConfigurationProperties`，静态读取点改为注入。

### ③ DAO 层零迁移

- `@Repository` = 0；`DAOManager` 是按 `ServiceContext` 分片的静态注册表 + 手写 `DatabaseFactory`(Hikari) + 手写 JDBC。
- 计划（第三步）：先做只读设计（分片语义 = login/game/chat 三上下文不串），再评估 Repository Bean 化。
- 边界：需要真实数据库/上下文测试，改动前必须明确验收口径。

### ④ 过渡层（网关/桥接）

- 20 个 `*RuntimeBridge*.java`、40 个 `*Gateway.java`；`gameserver/lifecycle` 目录共 125 个文件。
- 它们是兼容层：调用点已统一走 `GameRuntimeServices.*` 静态解析。
- 计划：不做"为删而删"；只在链路本要被改动时顺带收口。

### ⑤ 测试直接依赖静态单例

- 每个遗留类退役时，同步修改 `GameServiceProviderCompatibilityTest` 中该类的断言（provider 优先 → 退役类 fail-fast）。

## 三、治理机制（防扩散）

1. **冻结护栏**：`LegacySingletonFallbackAuditTest`——数量冻结、孤儿检测、退役类不得复发。
2. **契约护栏**：`GameServiceProviderCompatibilityTest`——provider 优先契约；退役类补 fail-fast 断言。
3. **源码审计护栏**：`GameRuntimeServicesLifecycleTest`（游戏服代码禁止 `X.getInstance()`）、`GameEventRuntimeBridgeTest`。
4. **登记流程**：新增兜底 → 先改本文档 → 再显式调整审计常量。

## 四、第一步执行记录（双源 fail-fast 试点：InGameShopEn）

- 选点依据：`InGameShopEn` 生产代码零直接 `getInstance()` 调用（全部经 `GameRuntimeServices.inGameShopEn()`），测试仅 1 处直接调用。
- 改动：
  - `getInstance()` 改为 fail-fast：provider 未注入或容器中无该 Bean → 抛 `IllegalStateException`（消息含类名与原因）；
  - 删除 `SingletonHolder` 静态兜底；
  - 新增 `inGameShopEnFailsFastWithoutSpringProvider()` 契约测试。
- 计数：131 → 130（审计常量同步下调）。
- 已知行为差异（边界）：`GameRuntimeServices.destroy()` 会清空 provider，关机窗口内迟到的 `inGameShopEn()` 调用将抛异常（以前是静默 new 一个真空实例、写入即丢）。若关机日志出现该异常 = 存在迟到调用点，应修调用点（关机前完成或可跳过），而不是恢复兜底。
- 验证边界：本轮改动尚未编译、未跑测试（AGENTS 规则：构建需用户授权）。

## 五、退役执行记录（2026-09-17）

### 第一批（A 组 16 类）

- 退役清单：`AbyssLandingSpecialService`、`AnnouncementService`、`BGService`、`CuringZoneService`、`DebugService`、
  `FindGroupService`、`FlyRingService`、`GameTimeService`、`LandingUpdateService`、`MailService`、`PeriodicSaveService`、
  `SpringZoneService`、`TaskManagerFromDB`、`ThievesGuildService`、`VeteranRewardsService`、`WebshopService`。
- 改动：`getInstance()` 统一 fail-fast + 删除 `SingletonHolder`；`MailService` 保留 Spring 实例缓存
  （`setInstanceProvider` 已清缓存），缺失 provider 时同样 fail-fast。
- 测试：`GameServiceProviderCompatibilityTest` 新增覆盖 16 类的反射 fail-fast 契约；
  `GameLocationBootstrapServices.abyssLandingSpecialService()` 的 destroy 后断言由"回退仍是另一实例"改为 `assertThrows`。
- 计数：131 → **114**（审计常量与 `RETIRED` 集合同步：17 类）。
### 第二批（B 组 9 类）

- 退役清单：`AionPacketHandlerFactory`、`ChatServer`、`DataManager`、`EventScheduler`、`IDFactory`、`LoginServer`、
  `LsPacketHandlerFactory`、`PacketFloodFilter`、`World`。
- 收敛方式：这 9 类的回落经 `*Fallbacks` 的 eager 静态持有者
  （`private static final class XxxFallback { INSTANCE = X.getInstance(); }`），缺 provider 时首触会把
  `IllegalStateException` 包成 `ExceptionInInitializerError` 并永久污染该持有者类。改为在 4 个 fallback 文件中
  直接 `X.getInstance()`（删除 9 个持有者），facade 与桥接层调用点不变，异常以 `IllegalStateException` 原样抛出。
- `DataManager` 特殊：移除 `getInstance()` 内的"补齐加载"副作用——静态数据加载由 `GameStaticDataGateway.load()`
  在 Spring 单例锁之外显式触发，`getInstance()` 只解析实例。
- 测试：`GameServiceProviderCompatibilityTest` 的反射 fail-fast 契约扩展到 9 类（共 25 类；`InGameShopEn` 保留单独用例）。
- 计数：114 → **105**（累计 26 类退役）。
- 工具：`.agents/summary/architecture-performance-refactor/thin_retired_fallbacks.py`（收敛 fallback 条目）。

### 第三批（其余 105 类，收口为零）

- 退役清单：其余 105 个双源类（含 `ThreadPoolManager`、`AI2Engine`、`QuestEngine`、`InstanceEngine`、`World`、
  `DataManager` 之后的全部剩余类）。
- 收敛方式：`retire_all_fallbacks_v2.py` 统一改 fail-fast（支持 `NewSingletonHolder`、全限定持有者、`resolvedInstance`
  缓存、空格缩进）；`thin_retired_fallbacks_v2.py` 收敛 9 个 `*Fallbacks` 文件里的 39 个 eager 持有者
  （第 40 个 `ThreadPoolManager` 早已走 lifecycle bridge）。
- 测试面：
  - 31 处 destroy 后回退断言（`assertNotSame`）翻转为 `assertThrows(IllegalStateException.class, …)`
    （compat 6 处、legacy-config 26 处，均按 FQN 过滤同名类）；
  - 新增测试工具 `TestServiceProviders`，为 12 个依赖旧兜底实例的测试类安装/清理 provider
    （`DropService`、`DropRegistrationService`、`AI2Engine`、`MoveTaskManager`、`MotionLoggingService`、
    `LsPacketHandlerFactory`、`QuestEngine`、`GMService`、`PacketBroadcaster`）。
  - `LegacySingletonFallbackAuditTest` 改为 131 文件清单 + 零回落冻结；compat 反射 fail-fast 契约覆盖全部 131 类。
- 结果：全量测试中 `未由 Spring 提供` 报错 **0**；剩余失败均为并行会话在途的 quest 侧（65 失败 + 20 错误）
  与 1 个既有 `SMPlayerSpawnTest` 字段类型不匹配，均与本迁移无关。

## 六、后续顺序

② 配置（`@ConfigurationProperties`）→ ③ DAO 归 Spring → ④⑤ 随链路收口；2265/823 静态门面调用点按 AR-001 结论不动。

> 回归注意：新增服务若走"Spring provider + 静态兜底"双源，会被 `LegacySingletonFallbackAuditTest` 拦截；
> 测试需要旧实例时用 `TestServiceProviders.install(...)` 安装 provider，而不是恢复兜底。

## 七、证据来源

- `.agents/summary/architecture-performance-refactor/2026-09-15-gameplay-jfr-hotspots.md`（12.13/12.25/12.26 性能口径）
- `.agents/summary/architecture-performance-refactor/2026-09-17-login-link-shutdown-robustness.md`（关机链路证据）
- memory-bank：`AR-001`（启动/热路径准则）、`AR-002`（SPI 废除与 legacy bridges 边界）、`AR-010`（懒物化）

## 八、结构优化轮（2026-09-17，大类分离）

- 目标类：`GameLegacyServiceBridgeConfiguration`（2053 行、162 个 `@Bean`，纯平铺、无跨 bean 调用、无辅助成员）。
- 拆分方案：按域拆成 6 个 `@Configuration(proxyBeanMethods = false)`：
  `CoreRuntimeServiceBeans`(40) / `EngineBeans`(21) / `NetworkBeans`(9) / `EventBeans`(22) /
  `SiegeBattlefieldBeans`(41) / `TaskAndStatBeans`(29)。
- 兼容性：原类保留为 `@Import` 聚合入口，Bean 名称与类型不变，现有 `AnnotationConfigApplicationContext(原类)`
  的注册方式继续有效；工具：`split_legacy_bridge_config.py`。
- 测试适配：`GameLegacyServiceBridgeConfigurationTest.assertConfigurationCreatesNew(...)` 改为扫描
  "聚合入口 + 6 个域配置"，保持"该类型必须由配置类 new 构造为 Spring Bean"的审计语义。
- 验证：`GameLegacyServiceBridgeConfigurationTest`(57) + `GameStaticDataLifecycleTest`(10) +
  `GameRuntimeServicesLifecycleTest`(6) + 两个 RuntimeBridge 测试 = **90 例全绿**；`mvn test-compile` 通过。

## 九、碎片类收拢（2026-09-17，无效类删除 / 强绑定小类合并）

判定口径：只被一个核心类使用、生命周期完全依附、无独立领域语义、不在 Spring Bean / AOP / API 契约上。

| 批次 | 原类（行数） | 处置 | 结果 |
|---|---|---|---|
| 一 | `ai2/scenario/WalkScenario`(11) | 删除（0 引用空子类） | 文件去除 |
| 一 | `utils/javaagent/JavaAgentUtils`(18) | 唯一方法恒真，内联进 `GameUtilityServicesRuntimeBridge` | 文件 + 测试去除 |
| 一 | `loginserver/utils/AccountUtils`(39) | 单静态方法内联进 `AccountController` | 文件去除 |
| 一 | `spawnengine/StaticObjectSpawnManager`(71) | 2 个私有静态方法内联进 `SpawnEngine` | 文件去除 |
| 二 | `world/zone/handler/GeneralZoneHandler`(27) | 空实现 → `ZoneService.NoOpZoneHandler`（`private static final`） | 文件去除 |
| 二 | `network/factories/CsPacketHandlerFactory`(48) | 内联进 `ChatServer`（`private static final class CsPacketFactory`） | 文件去除 |
| 二 | `utils/WorkStealThreadFactory`(104) | 内联进 `ThreadPoolManager`（`WorkStealThreadFactory` + `WorkStealThread`） | 文件去除 |
| 二 | `utils/xml/CompressUtil`(79) | `Compress`/`Decompress` → `PlayerScripts` 私有静态方法 | 文件去除 |

- 对外面影响：`ZoneService.DUMMY_ZONE_HANDLER`、`ChatServer`/`ThreadPoolManager`/`PlayerScripts` 的原有公开方法签名全部不变；
  被删类为内部实现类，仓库内无其它引用（含 XML / properties / 反射清单已核对）。
- 保留独立的同类候选（不合并）：注册表/AI/zone `*Handler`、JAXB `*Data` 模板、Spring Bean、`Effects` 注册表类。
- 机械扫描（引用计数）不足以判定死类：AdminCommand、AI2、实例脚本均走反射/注册表装配，必须逐类核对使用路径后再动。

## 十、大类分离（2026-09-17，LegionService 判权 / 入团申请域拆分）

- 原类：`LegionService` 2572 行 / 1 个外部入口（`GameCoreGameplayServices.legionService()`）+ `CM_LEGION` 等 6 处报文入口。
- 拆出：`services/LegionRestrictions`（811 行，包内可见，`@Slf4j`），承接两块高内聚实现：
  1. 判权集合（原 `private class LegionRestrictions`，22 个 `can*` / `is*` 校验）；
  2. 入团申请与军团仓库历史流程（原 `handleJoinRequest*`、`setJoin*`、`sendLegionJoinRequest*`、
     `handleLegionSearch`、`addWHItemHistory`）。
- 归属判定：这两块与 `LegionService` 的缓存/DAO 状态强耦合但成员间自成体系，判权与流程共用一个
  宿主引用即可，属于「对内实现细节适当收拢、对能力按域拆出」。
- 兼容性：`LegionService` 保留全部对外公开方法签名与语义，改为一行转调 `restrictions()`；
  `restrictions()` 惰性构造（构造期不暴露 `this`），`getLegionMemberEx(String)`、`MAX_LEGION_LEVEL` 提为包内可见；
  新增 `getAllCachedLegions()` 供搜索复用。外部调用点（`CM_LEGION`、`CM_LEGION_SEARCH`、`CM_LEGION_JOIN_*`、
  `ItemMoveService`、`ItemSplitService`、`PlayerEnterWorldService`）零改动。
- 测试适配：`LegionServiceTest` 反射路径由 `LegionService$LegionRestrictions` 改为顶层
  `LegionRestrictions`，判权注入改走 `service.restrictions()`。
- 验证：`LegionServiceTest`(3) + `LegionContainerTest`(3) + `LegionMemberContainerTest`(2) +
  `PlayerEnterWorldVipTest`(3) + `ModelCollectionImplementationTest`(18) +
  `ServiceInternalCollectionImplementationTest`(6) + `ShutdownHookTest`(5) = **40 例全绿**；`mvn test-compile` 通过。

## 十一、单实现接口清理 + 成员域/天梯域分离（2026-09-17）

### 1. 删除 6 个单实现接口（策略接口只是机械重复实现类方法名）

| 删除的接口 | 唯一实现 | 处理 |
|---|---|---|
| `utils/collections/ICache` | `LastUsedCache` | 去掉 `implements`，`@Override` 内联剥离 |
| `model/atreian_bestiary/ABList` | `PlayerABList` | 同上 |
| `model/event_window/EventWindowList` | `PlayerEventWindowList` | 同上 |
| `model/dorinerk_wardrobe/WardrobeList` | `PlayerWardrobeList` | 同上 |
| `model/cp/CPList` | `PlayerCPList` | 同上 |
| `model/skill/linked_skill/StigmaList` | `PlayerEquippedStigmaList` | 同上 |

- 判定依据：接口名在仓库内仅出现 2 次（自身 + 唯一 `implements`），**没有任何以接口类型声明的变量/参数/返回值**
  （DAO、Player 字段都用具体实现类），删除不影响 API 契约与多态语义。
- 结果：6 个文件去除，实现类方法名/签名/行为不变。

### 2. `LegionService` 成员域分离 → `LegionMembers`（387 行）

- 迁出内容：成员缓存与持久化（`storeLegionMember` ×2、`storeLegionMemberExInCache`、`addCachedLegionMember(Ex)`、
  `deleteLegionMemberFromDB`、`getLegionMemberEx` ×2、`loadLegionMemberExList`）、加入/踢出/离开
  （`addLegionMember` ×2、`removeLegionMember`、`removePlayerFromLegionAsItself`）、登录/下线同步（`onLogin`、`onLogout`）。
- 宿主协作：`LegionService` 暴露包内 `world()`、`allCachedLegionMembers()`、`allCachedLegions()`、`storeLegion()`、
  `storeLegionAnnouncements()`、`addHistory(...)`、`displayLegionMessage(...)`、`getLegionMemberEx(String)`；
  `LegionMembers` 惰性构造（`legionMembers()`）。
- 对外 API 不变：`loadLegionMemberExList`、`removePlayerFromLegionAsItself`、`onLogin`、`onLogout` 在 `LegionService`
  保留同名门面；`LegionRestrictions` 的 `directAddPlayer`/`getLegionMemberEx` 调用照旧。
- `LegionService` 行数：2572 → 1957（第十节后） → **1723**。

### 3. `Battleground` 天梯评分域 → `BattlegroundLadder`（107 行）

- 迁出内容：`playerWinMatch`、`playerLoseMatch`、`performLadderUpdate`、`calcRatingChange`、`getLadderDAO`。
- 兼容策略：`Battleground` 保留全部 `protected` 同名委托（`DeathmatchBg`/`SoloSurvivorBg`/`TwoTeamBg`/
  `TwoTeamSmallBg` 的 `super.xxx` 调用与 `super.K_VALUE` 均不变）。
- 未继续拆 `Battleground` 的原因：该类是 4 个战场子类的抽象基类，`preparePlayer`/`onDieDefault`/`onLeaveDefault`/
  `performTeleport` 等 2000 行主体是被子类 `super` 调用的继承契约，抽离会改变继承面且缺乏运行时验证手段，
  风险高于收益；只抽出唯一可作为纯域独立演进的天梯评分。
- 验证：`BattlegroundCollectionsTest`(1) + `LadderServiceTest`(3) + `ServiceMapImplementationTest`(6) +
  `LegionServiceTest`(3) + `LegionContainerTest`(3) + `LegionMemberContainerTest`(2) + `ModelCollectionImplementationTest`(18)
  + `ServiceInternalCollectionImplementationTest`(6) + `PlayerEnterWorldVipTest`(3) + `ShutdownHookTest`(5)
  + `GameLegacyServiceBridgeConfigurationTest`(57) = **140 例全绿**（含并行 quest 侧测试，一起跑无回归）；`mvn test-compile` 通过。

## 十二、大类 Lombok 批处理（2026-09-17）

处理口径（只做**零 Lombok 注解**且"字段名 ↔ 访问器名严格一致 + 平凡实现"的类；有字段级 `@Getter/@Setter` 的类不动）：

| 类 | 原行数 | 现行数 | 删除段/行 | 处理 |
|---|---|---|---|---|
| `PlayerCommonData` | 1284 | 1022 | 50 段 / 275 行 | 类级 `@Getter @Setter`，字段名与访问器一致的平凡 getter/setter 全部删除 |
| `Skill` | 2192 | 1952 | 8 段 / 253 行 | 同上 |
| `Battleground` | 2130 | 1930 | 8 段 / 210 行 | 同上 |
| `AbyssRank` | 397 | 288 | 6 段 / 112 行 | 同上 |

- 判定严格化：只删「方法名 = Lombok 会生成的名字」且「方法体是 `return field;` / `this.field = arg;`」的方法；
  名字不匹配（如 `is*` 前缀布尔字段）或体里有校验/发消息等副作用的访问器一律保留。
- 三个历史别名访问器显式保留并加注释（删掉会破坏现有调用点）：
  `PlayerCommonData#getNoExp()`（无同名字段可匹配）、`Battleground#setIsEvent(boolean)`（字段名 `isEvent`）、
  `Skill#setIsMultiCast(boolean)`（字段名 `isMultiCast`）。
- 字节码核验：`javap` 确认 Lombok 已生成被删访问器（`PlayerCommonData` 180 / `Battleground` 76 /
  `Skill` 90 / `AbyssRank` 43 个 public 方法），`getNoExp`、`setIsEvent`/`setIsMultiCast`、`setOnline`、
  `getSkillId` 等抽样全部存在。
- 未处理的高价值候选（留待下一批）：`Item`(1136)、`ItemTemplate`(775)、`Creature`(953)、`Effect`(1777)、
  `PetCommonData`/`MinionCommonData`（已混用字段级注解，需先统一策略）。
- 验证：`mvn test-compile` 通过；`LadderServiceTest`(3) + `GameRuntimeServicesLifecycleTest`(6) +
  `GameLegacyServiceBridgeConfigurationTest`(57) + `ShutdownHookTest`(5) + `ServiceInternalCollectionImplementationTest`(6)
  + `ServiceMapImplementationTest`(6) + `SkillEngineTest`(1) + `BattlegroundCollectionsTest`(1) + `LegionServiceTest`(3)
  + `ModelCollectionImplementationTest`(18) = **106 例全绿**。

## 十三、大类 Lombok 批处理（第二批：Effect / Creature / Item / ItemTemplate）

这一批先做了逐字段可行性分析，结论与第一批差异很大：**这四个大类里能机械替换的访问器比例很低**。

| 类 | 原行数 | 现行数 | 处理 |
|---|---|---|---|
| `Effect` | 1777 | 1705 | 类级 `@Getter @Setter`，删 9 个平凡 getter（其余 54 个访问器带逻辑或 `@Override` 语义，保留） |
| `Creature` | 953 | 936 | 16 个"平凡体"里 14 个是 `return false` / `return this` / `return 0` 的多态桩或覆盖点，**不能删**；只为 `aggroList`、`packetBroadcastMask` 两个真字段加字段级 `@Getter` |
| `Item` | 1136 | 1129 | 该文件本就有 28 个字段级 `@Getter`；4 个候选里只有 `expireTime` 可换，`getRandomCount`/`isAmplified`/`isEnhance` 是历史别名（字段 `rndCount`/`amplification`/`canEnhance` 与 Lombok 生成名不匹配），保留 |
| `ItemTemplate` | 775 | 775 | 本就有 41 个字段级 `@Getter`；`getFuncPetId` 换为字段级注解，`getTempExchangeTime` 因字段拼写为 `temExchangeTime` 而保留原名 |

- 过程中被编译期拦下的历史别名（均已加注释保留）：`Effect#getIsForcedEffect()`（字段 `isForcedEffect`，Lombok 会生成 `isIsForcedEffect`）、
  `Item#getRandomCount/isAmplified/isEnhance`、`ItemTemplate#getTempExchangeTime`（字段 `temExchangeTime`，少一个 p）。
- 结论：**大类的行数主要来自业务逻辑而不是访问器样板**——`Effect` 63 个访问器里 54 个带逻辑，`Creature` 61 个里 45 个带逻辑，
  `Item` 68 个里 61 个带逻辑，`ItemTemplate` 60 个里 43 个带逻辑。继续做 Lombok 批处理的边际收益已经很低；
  这些类的进一步瘦身应该走"按能力抽取实现类"（如 `Effect` 的效果结算、`Creature` 的仇恨/广播）而不是删除访问器。
- 验证：`mvn test-compile` 通过；`EffectTest`(6) + `BuffStunEffectTest`(1) + `SkillEngineTest`(1) + `ItemTest`(1) +
  `CreatureTest`(1) + `LadderServiceTest`(3) + `BattlegroundCollectionsTest`(1) + `GameLegacyServiceBridgeConfigurationTest`(57)
  + `ModelCollectionImplementationTest`(18) + `ServiceInternalCollectionImplementationTest`(6) + `ServiceMapImplementationTest`(6)
  = **101 例全绿**。

## 十四、Lombok 注解统一到类级（2026-09-17）

按「Lombok 优先加在类上」的口径，把四个类里散落的字段级注解全部收成类级：

| 类 | 字段级 @Getter | 字段级 @Setter | 收成 | 行数变化 |
|---|---|---|---|---|
| `Effect` | 61 | 46 | 类级 `@Getter @Setter` | 1705 → 1598 |
| `ItemTemplate` | 42 | 2 | 类级 `@Getter @Setter` | 775 → 733 |
| `Item` | 29 | 16 | 类级 `@Getter @Setter` | 1129 → 1086 |
| `Creature` | 15 | 8 | 类级 `@Getter @Setter` | 936 → 914 |

- 效果：**147 个字段级 `@Getter` + 72 个字段级 `@Setter` → 4 组类级注解**，净减 214 行，且类的"访问器策略"从"逐字段分散"变成一眼可见。
- 兼容性：Lombok 在已有同名方法时不生成，因此上一批保留的历史别名（`Effect#getIsForcedEffect`、
  `Item#getRandomCount/isAmplified/isEnhance`、`ItemTemplate#getTempExchangeTime`）依旧是手写实现，未被覆盖。
- 字段级注解残留检查：四个文件均为 0；字节码抽查 `getAggroList`/`getPacketBroadcastMask`/`getExpireTime`/
  `getFuncPetId`/`getSkill`/`getWorldId` 等全部存在。
- 验证：`mvn test-compile` 通过；`EffectTest`(6) + `BuffStunEffectTest`(1) + `SkillEngineTest`(1) + `ItemTest`(1)
  + `CreatureTest`(1) + `LadderServiceTest`(3) + `BattlegroundCollectionsTest`(1) + `GameLegacyServiceBridgeConfigurationTest`(57)
  + `ModelCollectionImplementationTest`(18) + `ServiceInternalCollectionImplementationTest`(6) + `ServiceMapImplementationTest`(6)
  = **101 例全绿**。
