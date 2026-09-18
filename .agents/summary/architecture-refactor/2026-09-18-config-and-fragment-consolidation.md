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
