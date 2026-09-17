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
