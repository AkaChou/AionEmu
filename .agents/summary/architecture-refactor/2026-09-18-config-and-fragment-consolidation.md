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
