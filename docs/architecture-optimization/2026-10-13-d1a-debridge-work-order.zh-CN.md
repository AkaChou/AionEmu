# D1a 去桥施工作业单（De-bridge Work Order）

> 状态：施工准备（工作流 2 验收中）
> 上位规划：`2026-10-09-architecture-optimization-plan.zh-CN.md` · `2026-10-10-lifecycle-locator-retirement-plan.zh-CN.md`
> 本单为架构评审候选 1「收拢 lifecycle 脚手架栈」的第一刀。D1b（折叠 gateway）与本单分离。

---

## 一、目标与边界

**目标**：删除 lifecycle 过渡层的中间层（运行时桥与降级兜底），把桥的非服务方法体搬到其唯一调用方（gateway），使 gateway 自足；定位器的兜底调用改为 fail-fast。

**本单不包含**：
- gateway 折进 lifecycle（D1b）
- 定位器聚合 record 改形、`GameCronServices` 托管化、A/B/C 调用点分类（D2）
- 30 端口、协议、静态数据等任何其他改动

**产物**：一次可编译的改动，随后请求构建/测试授权。

---

## 二、精确边界

### 2.1 删除（26 文件，`src/main/java/com/aionemu/gameserver/lifecycle/`）

**运行时桥（15）**
`GameBattlefieldRuntimeBridge` · `GameCoreServicesRuntimeBridge` · `GameEnginesRuntimeBridge` · `GameEventBootstrapRuntimeBridge` · `GameEventRuntimeBridge` · `GameFeatureServicesRuntimeBridge` · `GameHousingRuntimeBridge` · `GameLocationBootstrapRuntimeBridge` · `GameMaintenanceServicesRuntimeBridge` · `GameNetworkStartupRuntimeBridge` · `GameRuntimeServiceBridge` · `GameServerNetworkRuntimeBridge` · `GameUtilityServicesRuntimeBridge` · `GameWorldBootstrapRuntimeBridge` · `GameWorldServicesRuntimeBridge`

**降级兜底（11）**
`GameBattlefieldFallbacks` · `GameCoreServiceFallbacks` · `GameEngineServiceFallbacks` · `GameEventBootstrapFallbacks` · `GameEventRuntimeFallbacks` · `GameHousingFallbacks` · `GameMaintenanceServiceFallbacks` · `GameServerNetworkFallbacks` · `GameShutdownHookFallbacks` · `GameWorldBootstrapFallbacks` · `GameWorldServiceFallbacks`

**引用者已核**：上述 26 类的引用全部落在 `lifecycle` 包内及其测试，包外零引用（已排除 chatserver/commons 的同名无关符号）。

### 2.2 修改（主源码）

| 类别 | 数量 | 说明 |
|---|---|---|
| gateway（接收搬迁） | 10 | 见第三节 |
| gateway（仅删桥解析兜底） | 24 | 全部 24 个 Group A gateway 都要把 `runtimeBridge()` 兜底替换为 fail-fast |
| 定位器（去兜底调用） | 10 | 45 处 `Fallbacks.x()` 调用点 |
| 定位器（去内联兜底） | 11 | `provider.getIfAvailable(X::getInstance)` 内联形态 |
| 其他 | 1 | `GameShutdownRequest.java` 引用 `GameShutdownHookFallbacks` |

`GameThreadPoolServices` 的嵌套 `Fallbacks` holder（`static final ThreadPoolManager`）是**类内自持**，不属于待删的 11 个兜底类，单独处置。

### 2.3 不修改

`GameCronServices`（无 `@Component`、无 provider）本单不碰，归 D2。
`GameStartupSequenceLifecycle` 编排器不碰——它只调 `lifecycle.start()`，本单不改该签名。

---

## 三、搬迁映射（33 个成员 → 10 个 gateway）

| 源 bridge | 成员 | 类别 | 去向 | 配置门 |
|---|---|---|---|---|
| `GameUtilityServicesRuntimeBridge` | `initializeExceptionHandler` | 非服务 | `GameUtilityServicesGateway` | — |
| | `reportCallbackSupport` | 非服务 | 同上 | — |
| | `initializeCronService` | **服务调用** | 同上 | 见 §6 风险 R1 |
| | `loadConfig` | 非服务 | 同上 | `GSConfig.SERVER_COUNTRY_CODE` |
| | `initializeDateTime` | 非服务 | 同上 | `GSConfig.TIME_ZONE_ID` |
| | `loadThreadConfig` | 非服务 | 同上 | `ThreadConfig.BASE_THREAD_POOL_SIZE` 等 3 项 |
| `GameEventRuntimeBridge` | `printEventsSection` · `spawnTemporarySpawns` | 非服务 | `GameEventRuntimeGateway` | — |
| | `isEventServiceEnabled` | 非服务 | 同上 | `EventsConfig.ENABLE_EVENT_SERVICE` |
| | `isPlayerEventEnabled` | 非服务 | 同上 | `EventsConfig.EVENT_ENABLED` |
| | `isCrazyDaevaEnabled` | 非服务 | 同上 | `EventsConfig.ENABLE_CRAZY` |
| | `isTopRankingUpdateEnabled` | 非服务 | 同上 | `RankingConfig.TOP_RANKING_UPDATE_SETTING` |
| `GameLocationBootstrapRuntimeBridge` | `printSiegeAndBattlefieldLocationsSection` · `printVortexAndWorldBossLocationsSection` · `printPvpAndRvrLocationsSection` · `printInstanceAndDungeonLocationsSection` · `printAbyssAndLandingLocationsSection` | 非服务 | `GameLocationBootstrapGateway` | — |
| `GameMaintenanceServicesRuntimeBridge` | `isPigPoppyEventEnabled` · `isAbyssEventEnabled` · `isImperialTombEnabled` | 非服务 | `GameScheduledServicesGateway` | `EventsConfig.*` 3 项 |
| | `schedulePigPoppyEvent` · `scheduleAbyssEvent` | 服务调用 | 同上 | — |
| `GameRuntimeServiceBridge` | `printServicesSection` | 非服务 | `GameRuntimeServicesGateway` | — |
| | `initializeGameTime` · `startGameTimeClock` | 非服务 | 同上 | 绑 `ServerVariableDao` → `GameTimeManager` |
| `GameWorldServicesRuntimeBridge` | `createGameServer` · `activateGameServer` · `markPlayersOffline` | 非服务 | `GameWorldActivationGateway` | `markPlayersOffline` 走持久层 |
| `GameNetworkStartupRuntimeBridge` | `isBootEmbedded` · `registerShutdownHook` · `currentTimeMillis` | 非服务 | `GameNetworkStartupGateway` | — |
| `GameBattlefieldRuntimeBridge` | `printBattlefieldSection` | 非服务 | `GameBattlefieldGateway` | — |
| | `isAutoGroupEnabled` | 非服务 | 同上 | `AutoGroupConfig.AUTO_GROUP_ENABLED` |
| `GameHousingRuntimeBridge` | `printHousingSection` | 非服务 | `GameHousingGateway` | — |
| `GameServerNetworkRuntimeBridge` | `gameConnectionFactory` | 非服务 | `GameServerNetworkGateway` | — |

**其余 109 个成员无任何调用方**（全部是 `setXxxProvider` 注入 setter），随类删除。
**117 个 `resolve` 成员**只被 gateway 当兜底 supplier 使用，删除桥后由 gateway 自身的解析器承担。

---

## 四、改造后的解析器形态

现状（gateway 与 bridge 双层兜底）：

```java
private KamarBattlefieldService kamarBattlefieldService() {
    if (kamarBattlefieldServiceProvider == null) {
        return runtimeBridge().kamarBattlefieldService();
    }
    return kamarBattlefieldServiceProvider.getIfAvailable(() -> runtimeBridge().kamarBattlefieldService());
}
```

改造后（单层、fail-fast）：

```java
private KamarBattlefieldService kamarBattlefieldService() {
    return kamarBattlefieldServiceProvider.getObject();
}
```

`ObjectProvider.getObject()` 在 bean 缺席时抛 `NoSuchBeanDefinitionException`，即 fail-fast 语义，无需自造辅助方法。

**同形改造适用于**：
- 10 个 gateway 的 `runtimeBridge()` 兜底（`runtimeBridgeProvider` 字段与 setter 一并删除）；
- 10 个定位器的 45 处 `Fallbacks.x()`；
- 11 个定位器的内联 `X::getInstance`。

---

## 五、测试处置（18 个引用 bridge 的文件）

> ⚠️ 本节已修订。初稿按「文件是否出现 bridge 字样」一刀切判为删除，**已证伪**：
> `GameLegacyServiceBridgeConfigurationTest` 是 1,016 行 / 57 个 `@Test` 的**服务图装配审计**，
> 仅 2 处提及 `GameRuntimeServiceBridge`（import + 1 个 eager 断言）；其余 56 个
> `exposesXxxAsLazySpringBeans` / `createsSpringManagedXxxInsteadOfLegacySingleton`
> 恰是唯一能捕捉 fail-fast 改错的测试，必须保留。
> 同理 `GameWorldServicesRuntimeBridgeTest` 8 个测试仅 3 行 bridge 引用，多数在测 gateway 解析。
> **判定必须逐测试方法做，不能按文件做。**

**已确读并定案（2 文件）**

| 文件 | 处置 | 依据 |
|---|---|---|
| `GameUtilityServicesLifecycleTest`（5 测试） | **修剪**：删 3 个 | 保留 `startInitializesUtilityServicesOnce`、`failedStartRecordsFailureAndAllowsRetry`（测 lifecycle 幂等与失败重试，行为不随本单改变）；删 `usesUtilityServicesGatewayCollaborator`、`utilityGatewayBridgesRuntimeStaticsThroughSpringProvider`（调 `gateway.setRuntimeBridgeProvider(...)`，该 setter 本单删除）、`utilityGatewayUsesOptionalRuntimeBridgeProvider`（断言待删字段）；同时删内部类 `RecordingGameUtilityServicesRuntimeBridge`（继承 bridge，编译阻断） |
| `GameLegacyServiceBridgeConfigurationTest`（57 测试） | **修剪**：删 1 个 | 删 `exposesRuntimeServicesAsSpringBeansWithEagerBridgeOnly`（`:372`，断言 bridge 的 eager 装配）；保留其余 56 个装配审计 |

**待逐方法判定（工作流 test-disposition-audit 产出，共 17 文件）**

其余 16 个 `Game*RuntimeBridgeTest` / `GameRuntimeBridgeEagernessTest` / `GameNetworkStartupLifecycleTest` / `AionBootApplicationTest`，连同上述 2 个文件的最终清单，按「每个 `@Test` 方法」判定为 DELETE / KEEP / REWRITE，判定结果落回本表后施工。

**附带（不阻塞）**：`GameHousingServicesTest`（`:20`、`:39`）与 `GameWorldServicesTest`（`:21`）只是在 `Files.walk` 里**过滤掉**兜底文件路径，兜底类删除后该过滤成为死代码，**不影响编译与通过**，顺手清理即可。
`GameShutdownRequestTest` 需修剪：`:65` 读 `GameShutdownHookFallbacks.java` 源文本（删文件后 `NoSuchFileException`）；`:58` 读 `GameShutdownRequest.java` 源文本（该文件本单要改，断言需同步）。

---

## 六、执行序（每步保持可编译）

0. **补空缺 bean**（G2 前置）：`SkillEngine` 是 149 个被兜底类型中唯一无 bean 声明者。在 `EngineBeans` 补
   `@Bean @Lazy public SkillEngine skillEngine() { return SkillEngine.skillEngine; }`
   ——**直接返回静态字段，不得调用 `SkillEngine.getInstance()`**（后者会回头向同一容器索取自身，构成创建期循环引用）。
   此步必须在第 2 步之前，否则 `skillEngine()` 的 fail-fast 会在启动期抛异常。
1. **搬迁**（10 个 gateway）：把 §3 的成员体写入目标 gateway 为私有方法；调用点改为直接调用；删除 gateway 的 `runtimeBridgeProvider` 字段、setter 与 `runtimeBridge()` 方法。此步后 gateway 不再引用任何 bridge 类。
2. **定位器 fail-fast**：10 个定位器的 45 处 `Fallbacks.x()`、11 个定位器的内联 `X::getInstance`，改为直接调用（`provider.getObject()`）。
3. **附属修改**：`GameShutdownRequest.java`、`GameThreadPoolServices` 的类内 `Fallbacks` holder。
4. **删除 26 文件**。
5. **测试处置**：按 §5 删/修剪。
6. **请求授权**：全量构建 + `gameserver/lifecycle` 与 `boot` 测试套件。

> 顺序 0→5 之间任一步骤完成后，源码树仍引用尚未删除的桥类，因此**中间态可编译**；只有第 4 步之后才依赖第 0、1、2 步全部完成。

---

## 七、验收门禁

| 门禁 | 判据 | 状态 |
|---|---|---|
| G1 桥体搬迁完整 | §3 的 33 个成员在 gateway 侧均有对应实现，且无遗漏 | 待施工 |
| G2 fail-fast 安全 | 被兜底服务在 gameserver 上下文均为**无条件 bean** | ⚠️ **过，附 1 项前置**（详见下方） |

### G2 验收结果（2026-10-13 终态）

**两批并集：149 个被兜底类型**（第一批 45 个走 `Game*Fallbacks` 类，第二批 95 个走内联 `X::getInstance`，另有 9 个两批共有）。

| 结果 | 数量 | 说明 |
|---|---|---|
| ✅ 有无条件 `@Bean` | **148** | 全部经机械扫描逐方法核验：`@Bean` 注解确实落在返回该类型的方法上，且无 `@Conditional`/`@Profile` |
| ⛔ **无任何 bean 声明** | **1** | **`SkillEngine`** |

**`SkillEngine` 问题定性**：手工静态单例——`@NoArgsConstructor(access = PRIVATE)`（`SkillEngine.java:22`）、`public static final SkillEngine skillEngine = new SkillEngine()`（`:29`）、静态 `getInstance()`（`:139-145`）。**全库无任何 bean 声明**：其四个兄弟 `QuestEngine`/`InstanceEngine`/`AI2Engine`/`ChatProcessor` 在 `EngineBeans.java:56/62/68/74` 都有 `@Bean`，唯独它没有（构造器私有，外部 `new` 不出来）。因此 `ObjectProvider<SkillEngine>` 恒为 null，生产实际走兜底 `GameEngineServiceFallbacks.skillEngine()` → `SkillEngine.getInstance()`。**按"一律 fail-fast"照做会在启动期抛异常。**

**补 bean 的陷阱**：不可写 `@Bean SkillEngine skillEngine() { return SkillEngine.getInstance(); }` —— `getInstance()`（`:139-145`）会回头向同一容器索取 `SkillEngine`，构成 bean 创建期循环引用。必须直接返回静态字段 `SkillEngine.skillEngine`；这同时是行为等价的（加 bean 后 `getInstance()` 走的仍是同一字段）。

> **方法学教训**：按 `grep` 派生的类型清单会**漏掉走不同兜底路径的类型**——`SkillEngine` 经 `GameEngineServiceFallbacks` 类解析，其名字**不出现**在内联 `X::getInstance` 模式中，因此第二批的 95 个类型清单里没有它。兜底路径有三种（`Fallbacks` 类、内联 `::getInstance`、类内自持 holder），**清单必须按路径分别枚举，不能只 grep 一种模式**。
> 另一条反向教训：凭代码形态**推测**"属性类/任务类是静态单例"被证伪——7 个属性类与 8 个任务类在 `TaskAndStatBeans.java` 全部有 `@Bean`。形态推断不可替代逐一核验。
| G3 引用归零 | 26 个待删类在 `src/` 全域零引用 | ✅ 已核 |
| G4 编译 | 全量构建零错误 | 待授权 |
| G5 测试 | `lifecycle` + `boot` 套件全绿 | 待授权 |

**G2 未过时的回退**：凡被判定为「可能缺席」或「非 Spring 管理的单例」的服务，其解析器保留原 `getInstance()` 兜底，不纳入 fail-fast；该清单将单独列出并交你复核，不擅自决定。

---

## 八、风险与未决项

| 编号 | 风险 | 处置 |
|---|---|---|
| **R1** | `GameUtilityServicesRuntimeBridge.initializeCronService` 被初审误判为非服务，实为服务调用——它调 `GameCronServices.initialize()` → `CronService.initSingleton()`，会构造单例并启动 Quartz `StdSchedulerFactory` 调度器（自带关停生命周期）。 | 搬迁时必须保序（`GameStartupSequenceLifecycle` 第 5 步 `utilityServicesLifecycle.start()`），且不得与 D2 的 `GameCronServices` 托管化冲突。 |
| **R6** | `SkillEngine` 补 bean 后，`GameEnginesRuntimeBridgeTest.java:157-172` 那条「`GameEngineServiceFallbacks` 是 `SkillEngine.java` 之外唯一允许调 `getInstance()` 之处」的断言会失效（兜底类将被删除）。 | 该测试已列入 §5 待删/改写清单；补 bean 与删测试需同批完成。 |
| **R2** | `markPlayersOffline()` 写持久层（`playerDAO().setPlayersOffline(false)`），必须在任何加载玩家的阶段之前执行。 | 搬迁后在 `GameWorldActivationLifecycle`（第 13 步）内位置不变。 |
| **R3** | 游戏时钟初始化（第 4 步 `initializeGameTime`）必须先于事件运行期（第 17 步，`TemporarySpawnEngine.spawnAll()` 读游戏时间）。 | 搬迁后二者位置不变；施工时逐个确认。 |
| **R4** | 本次改动无法自行编译验证。 | 第 6 步一次性请求构建/测试授权；G4/G5 未过不进入 D1b。 |
| **R5** | `GameServerNetworkGateway` 引用嵌套类型 `GameServerNetworkLifecycle.NetworkPeer`。 | 本单不碰 lifecycle，该类型不受影响；D1b 折叠时需保留。 |

---

## 九、本单不覆盖（交 D1b / D2）

- **D1b**：37 个 gateway 折进 36 个 lifecycle（`GameStaticDataLifecycle` 吸收 2 个，其中 `GameMovementLoopGateway` 以 `ObjectProvider` 持有）；删除 25/37 个 gateway 的 `P==null` 死分支。
- **D2**：定位器聚合 record 改形（21 个定位器、148 个访问器、14 个 ctor-only 注册、8 个实例缓存、1 个 memo map）；`GameCronServices` 托管化；按 R1→R5 逐域做 A/B/C 调用点分类。
