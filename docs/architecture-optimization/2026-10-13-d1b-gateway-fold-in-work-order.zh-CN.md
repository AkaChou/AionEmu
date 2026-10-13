# D1b 折叠施工作业单（Gateway Fold-in Work Order）

> 状态：施工准备（工作流 2 验收中）
> 上位规划：`2026-10-09-architecture-optimization-plan.zh-CN.md` · `2026-10-10-lifecycle-locator-retirement-plan.zh-CN.md`
> 前置：D1a 已完成并提交（`69ff740eb`）。本单为架构评审候选 1「收拢 lifecycle 脚手架栈」的第二刀。

---

## 一、目标与边界

**目标**：
1. 37 个 `Game*Gateway` 折进 36 个 `Game*Lifecycle`（`GameStaticDataLifecycle` 吸收 `GameStaticDataGateway` 与 `GameMovementLoopGateway` 两个），删除全部 gateway 类；
2. 标准形 lifecycle 的重复包装样板（loaded / loadTimeMillis / lastFailure / synchronized start / 异常记录）抽入公共基类 `GameStartupStage`；
3. 清除折叠过程中暴露的死回退分支（`provider == null → getInstance()`、`getIfAvailable(X::getInstance)`、`new GameMovementLoopGateway()` 等）。

**用户已确认的取舍**：基类方案（三个候选中改动面最大者）——包装语义收敛为一次实现、一次测试；测试 `RecordingXxxGateway` 桩改为 `extends XxxLifecycle` 覆写 `runStage()`。

**本单不包含**：
- 定位器聚合 record 改形、`GameCronServices` 托管化、A/B/C 调用点分类（D2）；
- 启动顺序、阶段语义、日志文案、配置读取时机的任何行为性变化；
- 30 端口、协议、静态数据等其他改动。

**产物**：一次可编译的改动 + 测试全绿，随后请求提交授权。

---

## 二、基类契约

新建 `GameStartupStage`（lifecycle 包，抽象类）：

```java
public abstract class GameStartupStage {
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public final synchronized void start() {
        if (loaded) return;
        long begin = System.currentTimeMillis();
        try {
            runStage();
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = System.currentTimeMillis() - begin;
        }
    }

    /** 阶段业务体（原 gateway.start() 内联体）。 */
    protected abstract void runStage();

    public final synchronized boolean isLoaded() { ... }
    public final synchronized long getLoadTimeMillis() { ... }
    public final synchronized Throwable getLastFailure() { ... }
}
```

- 包装方法一律 `final`：包装语义唯一，子类只实现 `runStage()`；
- 特例签名（带参/返值）不进基类，见 §三.3。

---

## 三、折叠模板

### 3.1 标准形（28 个：无参 `void start()`）

以 `GameHtmlLifecycle` 为例：

```java
@Component
public class GameHtmlLifecycle extends GameStartupStage {

    private ObjectProvider<HTMLCache> htmlCacheProvider;   // 原 gateway 字段

    @Autowired(required = false)                            // 原 gateway setter 原样搬入
    void setHtmlCacheProvider(ObjectProvider<HTMLCache> htmlCacheProvider) { ... }

    @Override
    protected void runStage() {
        Util.printSection(I18n.get("console.section.html")); // 原 gateway.start() 内联
        htmlCacheProvider.getObject();                        // 死回退分支一并清除
    }

    // 原 gateway 的私有解析方法/辅助方法原样搬入
}
```

- lifecycle 的 `@RequiredArgsConstructor` + `private final GameXxxGateway` 字段删除；
- 三件套字段与四个包装方法删除（基类提供）；`isActivated`/`getActivationTimeMillis` 之类的**非标准命名**逐域确认后保留别名或统一（见特例）。
- 删除 `GameXxxGateway.java`；
- `GameStartupSequenceLifecycle` 与其余调用点**不改**（start 签名不变）。

**静态数据域（吸收 2 个）**：`GameStaticDataLifecycle` 直接持有 `dataManagerProvider`（原 StaticDataGateway）与 movement-loop 的 4 个 provider（movementNotifyTask / moveTaskManager / playerMoveTaskManager / zoneUpdateService）——「以 ObjectProvider 持有」即指这 4 个 provider 直接入类，不走 gateway 中转。业务体拆两个保护钩子供测试覆写：

```java
    @Override
    protected void runStage() {
        loadStaticData();
        initializeMovementLoops();
    }

    protected void loadStaticData() { dataManagerProvider.getObject().load(); }

    protected void initializeMovementLoops() {
        movementNotifyTaskProvider.getObject();
        moveTaskManagerProvider.getObject();
        playerMoveTaskManagerProvider.getObject();
        zoneUpdateServiceProvider.getObject();
    }
```

### 3.2 死分支清除（折叠时贯穿）

- `if (provider == null) { X.getInstance(); return; }` + `getIfAvailable(X::getInstance)`（现仅 `GameMovementLoopGateway` 4 处）；
- `getIfAvailable(...)` 伴生回退（现仅 `GameRuntimeServicesGateway` 1 处）；
- `movementLoopGatewayProvider == null → new GameMovementLoopGateway()`（现仅 `GameStaticDataLifecycle` 1 处）；
- 统一清零后一律裸 `provider.getObject()`（与 D1a 定位器 fail-fast 契约一致）。

### 3.3 特例表（8 个，签名保留，gateway 逻辑内联）

| 类 | 签名 | 处置 |
|---|---|---|
| `GameChatServerOverrideLifecycle` | `start(Boolean)` | 包装保留；计时基准原为 `gateway.currentTimeMillis()`，内联后若即 `System.currentTimeMillis()` 则同义 |
| `GameNetworkStartupLifecycle` | `start(Runnable)` | 包装保留；`printNetworkSection/printMiscSection/isBootEmbedded/shutdownHook/registerShutdownHook` 全部内联 |
| `GameServerNetworkLifecycle` | `start(GameServer)` + `start()` | 包装保留；嵌套类型 `GameServerNetworkLifecycle.NetworkPeer` 保留（R5）；`GameServer.java:154` 手工 new 更新为折叠后构造 |
| `GameStartupLogLifecycle` | `start() → long` | 包装保留；三元状态（startupTimeMillis）保留 |
| `GameSystemLifecycle` | `start(long) → long` | 包装保留 |
| `GameUtilityServicesLifecycle` | `start(GameThreadPoolLifecycle)` | 包装保留；参数不动（R1：保序第 5 步） |
| `GameWorldActivationLifecycle` | `start() → GameServer` | 包装保留；`activated` 命名与缓存返回语义保留 |
| `GameStartupCompletionLifecycle` | `start(long)` | 包装保留 |

特例的测试桩改为 `extends XxxLifecycle` + 覆写该类的业务钩子（施工时逐类指定钩子名；不引入无必要的钩子——能内联为 runStage 风格的加 `protected void runStage()`，签名冲突的按业务拆最小保护方法）。

### 3.4 附带更新

- `GameServer.java:154`：`new GameServerNetworkLifecycle(new GameServerNetworkGateway())` → 折叠后形态；
- `DataManager.java` Javadoc 中 `GameStaticDataGateway.load()` 引用改为 `GameStaticDataLifecycle`；
- 其余 4 处 "Gateway" 命中为业务词（Ganesh's Gateway / spawnStrongholdGatewaySecure），不碰。

---

## 四、测试处置

1. **基类测试收敛**：新建 `GameStartupStageTest` —— 用最小子类验证包装语义（成功、幂等、失败记录 lastFailure、重试后成功、loadTime ≥ 0）。39 个子类测试中重复的包装语义断言（幂等/计时/失败细节）**不再逐类重复**，保留"业务顺序 + 条件门控 + 计数"断言。
2. **39 个 `RecordingGameXxxGateway`** → `RecordingGameXxxLifecycle extends GameXxxLifecycle`，`@Override protected void runStage()`；构造从 `new GameXxxLifecycle(recordingGateway)` 改为直接 `new RecordingGameXxxLifecycle(events, ...)`。
3. `usesXxxGatewayCollaborator`（断言 lifecycle 持 gateway 字段）→ **删除**（协作关系退役）。
4. `xxxGatewayBridgesLegacyServicesThroughSpringProviders`（逐字段断言 gateway 的 provider 字段类型）→ 断言目标改 `GameXxxLifecycle.class`（字段名不变）。
5. 两个 gateway 专属测试（`GameEventBootstrapGatewayTest`、`GameWorldBootstrapGatewayTest`）→ 改写为对应 lifecycle 测试或删除（逐类评估其断言价值）。
6. 源码文本断言类（`GameRuntimeServicesLifecycleTest` 的 "不使用 X.getInstance()" 扫描、`GameStartupSequenceLifecycleTest` 的编排断言）→ 需要时更新路径/类名引用。
7. `GameStaticDataLifecycleTest`：`setMovementLoopGatewayProvider(provider(GameMovementLoopGateway.class, ...))` → 直接覆写 `loadStaticData()`/`initializeMovementLoops()` 两个钩子。
8. boot 侧（`GameServiceLifecycleTest`、`ChatServiceLifecycleTest`、`AionBootApplicationTest`）逐条核对其中的 Gateway 文本断言并更新。

---

## 五、验收

| 编号 | 门禁 | 口径 |
|---|---|---|
| G1 | 折叠完整 | 37 个 `Game*Gateway.java` 全部删除；`src/` 零引用（业务词除外） |
| G2 | 编译 | 全量构建零错误（IDEA MCP build_project） |
| G3 | 测试 | lifecycle 包全部测试类 + boot 测试 + D1a 期间受影响的 29 个类全绿 |
| G4 | 行为等价 | 启动顺序、阶段语义、日志、计时口径与折叠前逐域核对；`GameStartupSequenceLifecycle.start()` 调用序列不变 |
| G5 | 提交 | 显式路径暂存、英文 message、单 commit |

---

## 六、风险

| 编号 | 风险 | 处置 |
|---|---|---|
| R1 | 特例的内联改写引入行为差异（计时基准、幂等缓存语义）。 | 逐类对照原 gateway 与 lifecycle 包装 diff，编译前二次审读。 |
| R2 | `GameServerNetworkLifecycle` 有双 start 重载（`start()/start(GameServer)`），`GameServer.java` 手工 new 路径依赖其构造。 | 折叠时先读 `GameServer.java:140-160` 与类全文，保持两重载语义。 |
| R3 | 39 个测试同构改造量大，易出现残留引用（gateway 类型、旧构造器）。 | 全量 `grep -rn "Gateway" src/test` 复核归零后编译。 |
| R4 | `runStage()` 为 protected 后测试依赖子类覆写，误将业务断言空转。 | 每个 Recording 子类只覆写业务体；包装断言由基类测试承担，子类测试至少断言一次真实 `start()` 路径（部分域用 provider 桩保留真实 runStage 覆盖）。 |
| R5 | `GameStartupSequenceLifecycle` 的反射计时汇总按字段名打印耗时——折叠后字段仍是 lifecycle bean，不变。 | 不改；G3 中 `GameStartupSequenceLifecycleTest` 覆盖。 |

---

## 七、施工批次（建议顺序）

1. 基类 `GameStartupStage` + `GameStartupStageTest`；
2. 简单域（单 provider、逻辑 <50 行）：Html、Logging、AdminPanel、RatioLimit、Html、ChatServerOverride、System、SystemProperties、StartupHooks、StartupCompletion、ThreadPool、StartupLog……（按实际读入分批）；
3. 中等域（多 provider）：Battlefield、Housing、DisputeLand、Dredgion、SiegeSchedule、SeasonRanking、RewardServices、ProtectorConqueror、CustomEvents、ScheduledServices、OptionalServices……；
4. 大域：Engines、EventBootstrap、EventRuntime、WorldBootstrap、WorldActivation、LocationBootstrap、Spawn、Cleaning、NetworkStartup、ServerNetwork、RuntimeServices、UtilityServices、StaticData（吸收 2 个）；
5. 全量编译 → 复跑测试 → 提交。
