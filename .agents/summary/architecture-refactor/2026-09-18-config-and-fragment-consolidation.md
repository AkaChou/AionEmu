# Spring 配置层解耦与碎片调度任务收拢记录（2026-09-18）

## 一、改造范围与背景

根据 Spring 迁移遗留清单（`2026-09-17-spring-migration-leftovers.md`）与项目高内聚重构标准，本次完成：
1. **配置层 Spring 解耦**：将遗留的四大静态配置类纳入 Spring IoC 容器与 ConfigurationProperties 规范，消除硬编码静态耦合；
2. **过度拆分的碎片调度任务收拢**：消除 6 个行数极小（30~40 行）且仅服务单一 Service 调度周期的孤立 `*StartRunnable`，合并为宿主类内部私有任务，消除 5 个碎片空包。

## 二、配置层解耦交付（4 类）

| 配置类 | 属性前缀 / 模式 | 改造内容 | 兼容性保证 |
|---|---|---|---|
| `SvStatsConfig` | `svstats` | `@Component` + `@ConfigurationProperties(prefix = "svstats")`，提供实例属性与 setter 同步 | 静态字段 `SVSTATS_ENABLE` 双向同步，兼容遗留 `PingPongThread` / `Shutdown` 与测试 |
| `IPConfig` | - | `@Component`，`@PostConstruct` 自动探测与加载网络地址 | 提供 `getPublicAddress()` 与 `ipRanges()` 实例接口，保留静态 `getDefaultAddress()` / `getRanges()` 门面 |
| `ThreadConfig` | `gameserver.thread` | `@Component` + `@ConfigurationProperties(prefix = "gameserver.thread")`，`@PostConstruct` 计算线程池大小 | 提供实例属性与 getter/setter，保留 `THREAD_POOL_SIZE` 等静态字段与 `ThreadPoolManager` 兼容 |
| `SecurityConfig` | - | 标记为 `@Component`，正式纳入 Spring 容器管理 | 30 项安全与反外挂配置支持 Bean 注入与静态读取 |

## 三、碎片类收拢清单（6 类删除，合并至宿主）

| 宿主核心类 | 收拢并删除的碎片类 | 内部化形式 | 架构收益 |
|---|---|---|---|
| `RiftService` | `services/rift/RiftOpenRunnable.java` | `private static class RiftOpenRunnable` | 消除 35 行独立文件，调度任务与服务编排内聚 |
| `VortexService` | `services/vortexservice/VortexStartRunnable.java` | `private static class VortexStartRunnable` | 消除 33 行独立文件及单一使用碎片包 |
| `NightmareCircusService` | `services/nightmarecircusservice/CircusStartRunnable.java` | `private static class CircusStartRunnable` | 消除 37 行独立文件及单一使用碎片包 |
| `ConquestService` | `services/conquestservice/ConquestStartRunnable.java` | `private static class ConquestStartRunnable` | 消除 36 行独立文件及单一使用碎片包 |
| `MoltenusService` | `services/moltenusservice/MoltenusStartRunnable.java` | `private static class MoltenusStartRunnable` | 消除 48 行独立文件及单一使用碎片包 |
| `InstanceRiftService` | `services/instanceriftservice/InstanceStartRunnable.java` | `private static class InstanceStartRunnable` | 消除 33 行独立文件及单一使用碎片包 |

## 四、测试验证（已授权）

- **命令 1**：`mvn -q -Dtest=IPConfigTest,PingPongThreadTest,GameUtilityServicesLifecycleTest,RiftServiceTest test`
  - 测试全部通过（0 失败，0 错误），覆盖 IPConfig 默认地址逻辑、PingPongThread 统计启闭、ThreadConfig 加载生命周期与 RiftService 调度流程。
- **命令 2**：`mvn -q -Dtest=ConquestLocationTest,VortexLocationTest,MoltenusLocationTest,InstanceRiftLocationTest,NightmareCircusLocationTest test`
  - 测试全部通过（0 失败，0 错误），覆盖各世界事件地点与调度相关测试。


## 五、第二批碎片类收拢交付（8 类删除，合并至宿主）

| 宿主核心类 | 收拢并删除的碎片类 | 内部化形式 | 架构收益 |
|---|---|---|---|
| `SiegeService` | `services/siegeservice/SiegeStartRunnable.java` | `public static record SiegeStartRunnable` | 消除 30 行独立 record 文件，攻城启动调度与 Service 紧密内聚 |
| `SvsService` | `services/svsservice/SvsStartRunnable.java` | `private static class SvsStartRunnable` | 消除 48 行独立文件与 `svsservice` 碎片空包 |
| `AgentService` | `services/agentservice/AgentStartRunnable.java` | `private static class AgentStartRunnable` | 消除 67 行独立文件与 `agentservice` 碎片空包 |
| `ZorshivDredgionService` | `services/zorshivdredgionservice/DredgionStartRunnable.java` | `private static class DredgionStartRunnable` | 消除 67 行独立文件与 `zorshivdredgionservice` 碎片空包 |
| `Siege` | `services/siegeservice/SiegeBossDoAddDamageListener.java` | `public static class SiegeBossDoAddDamageListener` | 消除 27 行独立伤害监听器 |
| `Siege` | `services/siegeservice/SiegeBossDeathListener.java` | `public static class SiegeBossDeathListener` | 消除 42 行独立首领死亡监听器 |
| `DimensionalVortex` | `services/vortexservice/GeneratorDestroyListener.java` | `public static class GeneratorDestroyListener` | 消除 48 行独立生成器监听器 |
| `ConquestOffering` | `services/conquestservice/ConquestBossDestroyListener.java` | `public static class ConquestBossDestroyListener` | 消除 45 行独立桩监听器 |

### 测试验证（已授权）
- **命令**：`mvn -q -Dtest=SiegeServiceTest,SiegeRaceCounterTest,GameSiegeScheduleLifecycleTest,GameDredgionLifecycleTest,SvsLocationTest,AgentLocationTest,ZorshivDredgionLocationTest test`
- 测试全部通过（0 失败，0 错误），攻城流程、SVS、代理战、挖掘舰等调度与监听功能保持 100% 行为一致。

## 配置消费点注入化可行性分析（2026-09-18，优先级 2）

### 一、现状量化

`SecurityConfig` / `ThreadConfig` / `IPConfig` / `SvStatsConfig` 已在 `661d983f3` 注册为 Spring Bean
（其中两个为 `@ConfigurationProperties`）。四者的**真实消费点**（排除 `Configure.java` 的类字面量与
各 `Config.java` 自身的加载器）如下：

| 配置类 | 真实静态读取点 | 消费方文件 | 消费方是否 Spring Bean |
|---|---:|---|---|
| `SecurityConfig` | 44 | 22（`Player`/`Skill`/`AionConnection`/网络包/控制器/服务/`KnownList` 等） | **全部不是** |
| `ThreadConfig` | 5 | 2（`ThreadPoolManager`、`AGameProcessor`） | **全部不是** |
| `IPConfig` | 6 | 2（`SM_GS_AUTH`、`SM_CS_AUTH`，均为网络包构造路径） | **全部不是** |
| `SvStatsConfig` | 3 | 2（`PingPongThread`、`Shutdown`） | **全部不是** |

### 二、结论：不做「78 处调用点改注入」

- 78 个消费点分布在 **30 个类**，其中 **0 个是 Spring Bean**：它们是实体（`Player`/`Skill`/`Item`）、
  按引用创建的对象（`AionConnection`、网络包）、以及静态工厂/构造器创建的工具与线程（`ThreadPoolManager`、`PingPongThread`）。
- 把这些类变成 Bean 或携带配置引用属于**对象图重设计**，会触碰玩家、技能、网络包与线程池的创建路径，
  风险远大于收益，且与"不为动而动"纪律冲突。
- 因此 `public static` 字段在此处不是临时兜底，而是**面向非 Bean 对象图的访问器层**：设置值必须同时
  反映到该层的语义是当前架构的正确状态。

### 三、本轮实际修掉的两个缺陷（`@PostConstruct` 静态方法无效）

审计发现 `661d983f3` 把 `@PostConstruct` 加在了 **static 方法**上，而 Jakarta Annotations 规范中
`@PostConstruct` 只适用于实例方法，Spring 不会回调。当前能工作只是因为遗留 `Config.load()` 又显式调用了一次。

| 文件 | 原问题 | 处理 |
|---|---|---|
| `ThreadConfig` | `@PostConstruct public static void load()` 不会被回调；且 Spring 经 setter 绑定后派生值 `THREAD_POOL_SIZE` 不会重算，可能出现「实例属性=8、派生池大小仍按旧值」的分裂状态 | 移除静态注解；新增实例 `@PostConstruct void recomputeAfterBinding()`，绑定完成后统一重算一次派生池大小 |
| `IPConfig` | 同类无效静态注解；而对外地址必须在遗留 `Config.load()` 写入 `NetworkConfig.PUBLIC_ADDRESS` **之后**解析，Spring 初始化更早，若在 Bean 生命周期里解析会把回环兜底地址固化 | 移除静态注解，并在类注释中明确"刻意不注册 Spring 生命周期回调"的原因；解析入口仍由遗留 `Config.load()` 调用，实例访问器 `getPublicAddress()`/`ipRanges()` 供 Bean 注入方使用 |

### 四、新增回归测试

`src/test/java/com/aionemu/gameserver/configs/main/ThreadConfigTest.java`（2 例）：

1. `loadDerivesPoolSizeFromCurrentProperties`：池大小 = `(base + extra) × CPU 核心数`；
2. `bindingSettersWriteFieldsWithoutRecomputingPoolSize`：setter 只写字段不重算，`recomputeAfterBinding()` 才重算，
   固化"绑定期间逐字段写、绑定结束一次重算"的约定。

### 五、验证

- `mvn -q -Dtest=ThreadConfigTest,IPConfigTest,GameUtilityServicesLifecycleTest,GameThreadPoolManagerBoundsTest,PingPongThreadTest test`：全部通过。
- 全库扫描确认无其它 static `@PostConstruct` 残留。

## 配置路径冻结与绑定验证（2026-09-18，用户约束：配置文件保持当前路径）

### 结论先行

**不引入 `application.yml` 配置项、不移动任何 properties 文件、不改动任何配置键名。**
四个配置目录（`aion/config/administration|main|network|login|chat`）与 `mygs/mycs/myls.properties`
仍是唯一配置来源，Spring 侧通过既有 `AionLegacyPropertySourceEnvironmentPostProcessor`
（`EnvironmentPostProcessor`，已注册于 `META-INF/spring.factories`）读取同一批文件。

### 关键验证：点号长键可直接绑定到 `@ConfigurationProperties`

此前无法确定「遗留 properties 的点号长键（`gameserver.thread.basepoolsize`）能否被 Spring
`@ConfigurationProperties` 直接绑定」。本轮用真实配置类做了绑定探针，结论是 **可以**：

- 绑定经过 setter，`ThreadConfig.getBasepoolsize()` 返回 7；
- 同一次绑定把静态门面 `ThreadConfig.BASE_THREAD_POOL_SIZE` 也写成 7（非 Bean 调用方仍可读）。

因此**不需要**新建 `application.yml` 键，也**不需要**改键名；Bean 与静态门面从同一份文件收敛到同一值。

### 新增回归测试

`AionLegacyPropertySourceEnvironmentPostProcessorTest#legacyDottedKeysBindOntoConfigurationPropertiesFromUnchangedPaths`：

1. 在临时目录按**原路径结构**写入 `config/main/main.properties` 与 `config/login/loginserver.properties`；
2. 经真实 `EnvironmentPostProcessor` 注入 Spring `Environment`；
3. 断言 `gameserver.thread.basepoolsize=7` 同时写入 `ThreadConfig` 实例属性与静态门面；
4. 断言 `svstats.enable_svstats=true` 同时写入 `SvStatsConfig` 实例属性与静态门面；
5. `finally` 还原静态字段，避免污染其它测试。

### 时序确认（双写路径已收敛，不再有分裂风险）

| 阶段 | 动作 | 结果 |
|---|---|---|
| Spring 上下文刷新 | `@ConfigurationProperties` 从 Environment（含遗留文件映射）绑定 | setter 写静态字段 |
| Bean 初始化 | `ThreadConfig @PostConstruct recomputeAfterBinding()` | 派生 `THREAD_POOL_SIZE` 按绑定值重算 |
| 服务生命周期 | `GameUtilityServicesRuntimeBridge.loadConfig()` → `Config.load()` | `ConfigurableProcessor` 用同一文件同一键写同一静态字段；随后 `ThreadConfig.load()` 重算 |

三条路径读写同一组静态字段、同一批文件、同一键名，值一致；不存在"Bean 与静态字段各自读不同来源"的分裂。

### 仍未完成的配置工作（如实登记，未做）

1. 其余 **51 个** `@Property` 配置类仍是纯静态形态（**这是当前唯一权威来源，功能正常**）；是否逐个迁移到
   可绑定形态属可选项，需按业务域分批评估。
2. `SecurityConfig` 注册为 `@Component` 但**无实例字段、无 `@ConfigurationProperties`**，Spring 不绑定任何值；
   其 30 个静态字段仍由 `Config.load()` 写入。当前是无副作用的空壳注册。
3. `IPConfig` 同样只有静态权威；实例访问器 `getPublicAddress()`/`ipRanges()` 目前无调用方。

## 方案 A 落地：配置权威唯一化（2026-09-18）

### 问题

遗留 `ConfigurableProcessor` 以静态字段为唯一权威，而 Spring `@ConfigurationProperties` 绑定读 `Environment`
（含命令行参数、环境变量、application.yml）。两者不打通时，操作者的命令行/环境变量覆盖只会写到 Bean 属性，
随后被遗留加载器用文件值覆盖 → **Bean 看一套、非 Bean 调用方看另一套**。

### 实现（不改配置路径、不改键名）

| 件 | 作用 |
|---|---|
| `commons.configuration.ConfigSourceResolver` | 函数式接口：`String resolve(String key)` |
| `commons.configuration.ConfigSourceResolverHolder` | 全局持有者，启动层发布一次；`publish(null)` 可清除 |
| `boot.config.BootConfigSourceResolver`（新增单例） | 在 Spring 上下文装配阶段 `publish(environment::getProperty)`；**不**放在 `EnvironmentPostProcessor` 里，因为后者在任何 `SpringApplication` 实例化时都会触发，测试反复调用会污染全局持有者（实测会让 `VipConfigPathTest` 在整包运行时读到别的测试环境） |
| `ConfigurableProcessor.getFieldValue` | 查值顺序变为：**已发布解析器 → `Properties[]` → 默认值** |

- 解析器未发布时（单测、非 Boot 启动）行为与原来**完全一致**，即 `bootOverrides → 文件 → 默认值`。
- 已发布时，命令行/环境变量/application.yml 对**所有**调用方（Bean 与静态字段）同时生效，权威唯一。
- 配置文件目录与键名保持不变，仅复用既有 `EnvironmentPostProcessor` 的既有映射。

### 新增回归测试

1. `commons/configuration/ConfigurableProcessorSourceResolverTest`（2 例，不依赖 Spring 上下文）：
   - 解析器优先于 `Properties` 与默认值；
   - 解析器返回 `null` 时正确回退到 `Properties`/默认值，不吞值。
2. `boot/config/LegacyConfigOverridePrecedenceTest`（2 例，真实 `EnvironmentPostProcessor`）：
   - 文件 `basepoolsize=7` + 命令行 `=9` → 静态字段必须为 **9**（不得回退到文件值），且与 Bean 绑定值一致；
   - 未发布解析器时 `Properties` 顺序行为不变。

### 验证

`mvn -q -Dtest=ConfigurableProcessorSourceResolverTest,LegacyConfigOverridePrecedenceTest,AionLegacyPropertySourceEnvironmentPostProcessorTest,LegacyConfigOverridesTest,LegacyServerConfigOverridesTest,ThreadConfigTest,IPConfigTest,GameUtilityServicesLifecycleTest,GameServiceLifecycleTest,LoginServiceLifecycleTest,ChatServiceLifecycleTest,VipConfigPathTest test` → 全部通过。

### 全量回归（2026-09-18）

`mvn -B test`：**3444 例，非 quest 失败 0**。
- 首轮全量暴露一处**测试隔离缺陷**：把发布逻辑放在 `EnvironmentPostProcessor` 里，导致整包运行时
  `VipConfigPathTest` 读到前一个测试实例化的 `StandardEnvironment`（单测单独跑通过）。改为
  `BootConfigSourceResolver` 单例发布后，`VipConfigPathTest` 在全量运行中恢复通过。
- 剩余失败/错误全部集中在 `questEngine`（并行会话在途：部分测试引用了正在改动的 audit 类，
  报 `NoClassDefFound`），与本轮配置改造无关。

## 配置层正式收口（2026-09-18）

### 收口结论

配置层治理按用户约束（**配置文件保持当前路径、键名不变**）正式收口。收口范围与边界如下。

### 已完成并验证

| # | 事项 | 提交 |
|---|---|---|
| 1 | `SvStatsConfig`/`IPConfig`/`ThreadConfig`/`SecurityConfig` 纳入 Spring 容器，配置目录与键名不变 | `661d983f3` |
| 2 | 修复无效的**静态** `@PostConstruct`：`ThreadConfig` 改为实例钩子并在绑定后重算派生池大小；`IPConfig` 明确不做生命周期回调的原因 | `803a27868` |
| 3 | 实测并固化"遗留点号长键可直接绑定到 `@ConfigurationProperties`"，证明无需新增 yml 键、无需移动文件 | `817e4f46a` |
| 4 | 权威唯一化：命令行参数 / 环境变量 / application.yml 覆盖对 Bean 绑定与静态字段同时生效（`ConfigSourceResolver` + `ConfigSourceResolverHolder` + `ConfigurableProcessor` 取值顺序） | `e2580742d` |
| 5 | 解析器改由 `BootConfigSourceResolver` 单例在装配阶段发布一次，消除测试隔离污染（`VipConfigPathTest` 整包失败事故） | `63bdffa97` |
| 6 | 全量回归：`mvn -B test` → 3444 例，**非 quest 失败 0**；剩余失败全部属并行 quest 工作区 | — |

### 明确不在本次收口范围（登记备用）

1. **其余 51 个 `@Property` 配置类**：保持纯静态形态，`Config.load()` 仍是其唯一权威。功能正常、有测试覆盖，
   是否逐个迁移到可绑定形态属后续可选增量，不在本次范围。
2. **`SecurityConfig` / `IPConfig` 的 Bean 注册**：两者保留 `@Component`，但当前不承载实例绑定
   （`SecurityConfig` 无实例字段，`IPConfig` 的实例访问器暂无调用方）。属无副作用注册；若后续要清理空壳注册
   或补齐实例绑定形态，需单独立项评估。
3. **消费点注入化**：78 个静态读取点分布于 30 个**非 Bean** 类，注入化等价于对象图重设计，已判定不做
   （详见前文"配置消费点注入化可行性分析"）。

### 使用说明（给运维）

- 覆盖优先级：**命令行参数 / 环境变量 / application.yml > 遗留 properties 文件 > `@Property` 默认值**。
- 覆盖对**所有**调用方生效：Spring Bean 属性与遗留静态字段读取同一份有效值。
- 例：`--gameserver.thread.basepoolsize=9` 会同时改变 `ThreadConfig` 的 Bean 属性与静态字段，
  并被后续 `Config.load()` 保留，不会被文件里的旧值覆盖。

## 配置消费方代码优化（2026-09-18，用户要求"配置文件保持当前，但代码要继续优化"）

### 1. `ThreadPoolManager` 改为注入线程配置（首个真正注入化的消费方）

之前判定"消费点无法注入"是因为它们都不是 Bean。`ThreadPoolManager` 是**例外**：它由
`EngineBeans.threadPoolManager()` 这个 `@Bean` 工厂方法创建，因此可以合法注入配置。

| 变更 | 说明 |
|---|---|
| 新增 `ThreadPoolManager(ThreadConfig)` | 池大小、线程优先级、超时告警阈值全部来自注入的配置对象，不再读静态门面 |
| 保留无参构造器 | 仅服务非 Bean 回退路径（`GameThreadPoolServices` 的 lazy holder）与既有测试，行为不变 |
| `ThreadPoolRunnableWrapper` 由 `static` 改为内部类 | 包装器从实例字段读取告警阈值，不再静态读取 `ThreadConfig` |
| 删除死常量 | `MAXIMUM_RUNTIME_IN_MILLISEC_WITHOUT_WARNING = 5000`（全库零引用） |
| `EngineBeans.threadPoolManager(ThreadConfig)` | Bean 工厂注入配置；消费点从 4 处静态读取降为 0 |

### 2. `ThreadConfig` setter 立即收敛派生值

原实现只在 `@PostConstruct` 重算 `THREAD_POOL_SIZE`，导致 setter 写入组件值后**派生值短暂不一致**。
现改为每个 setter 写入后立即 `load()`，`@PostConstruct` 保留为"无 setter 调用时"的兜底。

### 3. 测试更新

- `GameThreadPoolManagerBoundsTest` 新增 `injectedThreadConfigDecidesPoolSizes`：断言 instant/scheduled
  两个池的核心线程数由**注入配置**决定，并用 `try/finally` 还原静态字段；
- `ThreadConfigTest` 的两条契约更新为：`bindingSettersRecomputeTheDerivedPoolSize`（setter 立即收敛）
  与 `postConstructRecomputesTheDerivedPoolSize`（兜底重算）。

### 4. 验证

- 聚焦：`ThreadConfigTest`(3) / `GameThreadPoolManagerBoundsTest`(4) / `GameThreadPoolLifecycleTest` 全绿；
- 全量 `mvn -B clean test`：**3445 例，Failures 0，Errors 6，Skipped 2**；
  6 个错误全部是并行会话正在删除/改写的 quest audit 类造成的 `NoClassDefFound`，与本轮无关
  （clean 构建后 `EffectControllerTest`、`NpcMoveControllerPathTest`、`AbstractCollisionObserverTest`
  等匿名内部类加载错误随 `target` 清理一并消失，确认此前是脏增量构建的假失败）。

## 测试侧配置污染治理：ConfigSnapshot（2026-09-18，优先级 2）

### 背景与量化

全库测试里有 **215 处**对 `XxxConfig` 静态字段的直接赋值（分布在 **49** 个测试类）。其中真正属于
"字段备份 + `@AfterEach` 还原"样板的是 **14 个类**，典型形态：

```java
private final int originalA = XxxConfig.A;
@AfterEach void restore() { XxxConfig.A = originalA; }
```

这类样板有三个问题：① 漏还原会把状态泄漏给后续测试；② 新增字段时容易忘记同步；③ 个别测试干脆
**硬编码"基线值"**（如 `SkillConfig.CONSUME_DP = true`），一旦生产默认值变化就会静默失真。

### 新增工具

`src/test/java/com/aionemu/testutil/ConfigSnapshot`：

```java
private final ConfigSnapshot snapshot = ConfigSnapshot.of(MembershipConfig.class,
    "STORE_WH_ALL", "TRADE_ALL");

@AfterEach
void restore() {
    snapshot.restore();
}
```

- `of(Class, String...)` 要求字段**存在且为 static**，拼写错误立刻抛 `IllegalArgumentException`（含可用字段清单），
  不会静默跳过；
- 快照在**测试实例构造时**捕获（JUnit 每方法一个新实例），比"节流为类级常量"更准确；
- `restore()` 逐字段还原并复位可访问性。

### 迁移范围（本轮 17 个测试类）

| 测试类 | 原样板 | 迁移收益 |
|---|---|---|
| `VipServiceTest` | 2 字段 + 手写还原 | 快照 |
| `ItemRestrictionsTest` | 6 字段 + `@BeforeEach` 备份 + `@AfterEach` 还原 | 快照（去掉 12 行样板） |
| `PlayerTagsTest` | 2 字段备份 | 快照 |
| `EnchantServiceTest` | 2 字段备份 | 快照 |
| `ThreadConfigTest` | 5 个类级常量 + 手写还原 | 快照（并去掉"类加载时捕获"的陈旧基线隐患） |
| `GameThreadPoolManagerBoundsTest` | 方法内 3 字段 try/finally | 快照 |
| `VipConfigPathTest` | 2 字段备份 | 快照（字段扩到 5 个 VIP 键） |
| `IPConfigTest` | 手工置空 `PUBLIC_ADDRESS` | 快照 |
| `StatFunctionsTest` | **硬编码基线** `1f / 3400` | 快照（还原真实原值） |
| `StaticDataTest` | 硬编码 `false` | 快照 |
| `DpUseActionTest` / `DpUsePeriodicActionTest` | 硬编码 `true` | 快照 |
| `GeoServiceGroundSearchTest` / `GeoServiceSkillObstacleTest` | 字段备份 | 快照 |
| `ShutdownHookTest` | 字段备份 + `@BeforeEach` | 快照（去掉一个 `@BeforeEach`） |
| `ConsoleStaticDataProgressReporterTest` | 硬编码 `true / false` | 快照 |
| `PingPongThreadTest` | 方法内 `restore()`（**顺序相关**） | 快照 + 测试内直接置 false（顺序无关） |

### 验证

- `mvn -B -Dstyle.color=never -Dtest='com.aionemu.gameserver.**,com.aionemu.loginserver.**,com.aionemu.testutil.**' test`
  → **3245 例，Failures 0，Errors 0，Skipped 2**。

### 尚未迁移（登记）

仍在使用"方法内 try/finally 保存单个配置字段"的测试（如 `PathDataTest`、`PathServiceConcurrencyTest`、
`NpcMoveControllerPathTest`、`TargetRangePropertyTest` 等，约 150 处）：这些是**用例级**局部开关，
语义清晰、无跨用例泄漏风险，暂不强制迁移；新增或修改此类用例时可顺手改用 `ConfigSnapshot`。

## 优先级 1：`AGameProcessor` 告警阈值改为显式传入（2026-09-18）

### 变更

| 文件 | 内容 |
|---|---|
| `AGameProcessor` | 新增 `protected AGameProcessor(int threadsCount, long maxRuntimeInMillisWithoutWarning)`，阈值存入实例字段；无参重载退化为 `this(threadsCount, ThreadConfig.MAXIMUM_RUNTIME…)`（仅供非 Bean 路径与既有调用方）；`RunnableTaskWrapper` 由 `static` 改为内部类并读取实例字段 |
| `MovementProcessor` | 新增 `MovementProcessor(long maxRuntimeInMillisWithoutWarning)`；无参构造器保留给 `Global.MovementProcessor` 这一静态持有者 |

### 效果

`AGameProcessor` 的**调度热路径不再静态读取配置**——包装器使用实例阈值，显式构造器让调用方（未来可注入
`ThreadConfig`）完全掌握该值。非 Bean 路径（`Global.MovementProcessor`）行为不变。

### 新增测试

`AGameProcessorTest`（2 例）：

1. `explicitRuntimeThresholdIsUsed`：显式阈值 4321 必须被实例采用；
2. `legacyConstructorUsesTheStaticFacadeThreshold`：无参构造器仍等于静态门面值（回退路径兼容）。

验证：`AGameProcessorTest`、`NpcMoveControllerPathTest`、`PathDataTest` 全绿。

## 优先级 4：清理空壳 Bean 注册（2026-09-18）

### 判定

| 配置类 | 是否保留 `@Component` | 依据 |
|---|---|---|
| `ThreadConfig` | **保留** | 有真实实例绑定（setter 写字段并重算派生值）+ 实例 `@PostConstruct` 兜底 |
| `SvStatsConfig` | **保留** | 有实例字段，setter 同步静态门面，Spring 绑定真实生效 |
| `SecurityConfig` | **移除** | 只有 30 个静态字段，无实例字段、无实例 setter、无生命周期行为；注册成 Bean 不会带来任何绑定能力，却让人误以为它走 Spring 配置 |
| `IPConfig` | **移除** | 实例访问器已无调用方（`getPublicAddress()` / `ipRanges()` 全库零引用，已删除）；对外地址必须由遗留 `Config.load()` 在上下文刷新后解析，Bean 生命周期只会把回环兜底地址固化 |

### 变更

- `SecurityConfig`：移除 `@Component` 与相关 import，类注释写明"刻意不注册为 Spring Bean"的原因与取值路径；
- `IPConfig`：移除 `@Component`、`import org.springframework.stereotype.Component`，删除两个零调用方的实例访问器；
- 两者的取值路径不变：仍由遗留 `Config.load()` 写入静态字段，命令行/环境变量覆盖经
  `BootConfigSourceResolver` + `ConfigSourceResolver` 生效（与其它 50 个静态配置类一致）。

### 验证

`mvn -q -Dtest='IPConfigTest,PacketFloodFilterTest,AionBootApplicationTest,ConfigurableProcessorSourceResolverTest,ThreadConfigTest' test`
→ 全部通过；`AionBootApplicationTest` 实际启动 Spring 上下文并关闭，说明移除两个 Bean 注册不影响容器装配。

## 验证提交：环境处理器泄漏未真正移除（2026-09-18 晚）

### 发现

`63bdffa97` 的提交信息写着"revert the post-processor to its previous responsibility (property sources only)"，
但 `AionLegacyPropertySourceEnvironmentPostProcessor` **仍然在每次 `SpringApplication` 实例化时**执行：

```java
ConfigSourceResolverHolder.publish(environment::getProperty);
```

该提交只新增了 `BootConfigSourceResolver` 单例，没有删掉旧发布点——也就是说当初导致 `VipConfigPathTest`
读错环境的全局泄漏点在最后一个 commit 之后依然存在，提交信息与代码不一致。

### 处理

- 删除 post-processor 里的 `ConfigSourceResolverHolder.publish(...)` 及其 import，并在代码里写明"刻意不在此发布"
  的原因；解析器只由 `BootConfigSourceResolver` 在上下文装配时发布一次。
- 顺带补注释说明 `addLast(MapPropertySource)` 的语义：文件值以**最低优先级**注册，命令行/系统属性/环境变量/
  `application.yml` 仍然覆盖它。

### 新增回归测试

`src/test/java/com/aionemu/boot/config/LegacyPropertySourcePrecedenceTest.java`（3 例）：

1. `postProcessorLeavesTheGlobalResolverUntouched` —— 调用 post-processor 后 `ConfigSourceResolverHolder.resolve(...)`
   必须仍为 `null`（守住隔离性，回归 `VipConfigPathTest` 事故）；
2. `operatingSystemEnvironmentVariablesOutrankLegacyFileValues` —— 用 `SystemEnvironmentPropertySource`
   模拟 `GAMESERVER_THREAD_BASEPOOLSIZE`，断言原始键与 `aion.legacy.game.property.*` 前缀键都取环境变量值
   （即"文件不得反向覆盖外部环境变量"这一契约被测试固定下来，而不是只写在提交信息里）；
3. `fileValueAppliesWhenNoHigherPrecedenceSourceDefinesTheKey` —— 无更高优先级来源时文件值仍然生效。

验证：`mvn -B -Dstyle.color=never -Dtest='com.aionemu.boot.**' test` → **85 例，0 失败 0 错误**。

## 静态配置类：审计与决策（2026-09-18 晚）

详见 `.agents/summary/architecture-refactor/static-config-classes-decision.md`。要点：

- 52 个 `@Property` 类 / 759 字段 / 756 键；Bean 内静态配置读取 30 处，**构造期读取 0 处**（全部在
  `Config.load()` 之后由 `AionServiceLauncher` 按相位调用的方法体内）；
- 3 个重复键是游戏服/聊天服各自加载同一份 `network/network.properties` 的镜像声明，取值必然一致；
- 52 个配置类全部被 `ConfigurableProcessor.process(...)` 覆盖，不存在漏挂加载器的类；
- 14 个零引用字段按"客户端是否有对应物"逐个定性（2026-09-18 晚）：8 个保留、6 个删除，详见 static-config-classes-decision.md 的追加裁决；
- 结论：**其余 50 个静态配置类保持静态**，不迁移为 Spring Bean。
