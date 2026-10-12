# Lifecycle 过渡层退役战役：静态服务定位器与 Fallbacks 收口方案

> 状态：规划（Phase 1 任务 3 收尾 + Phase 4 任务 4.2 终态）
> 前置：Phase 4.2 DAO 解耦 34 批已完成（2026-10-10），全库真实静态查表仅剩 `QuestRuntimeComposition` 8 处（quest 线域）。
> 本方案对应总规划 `2026-10-09-architecture-optimization-plan.zh-CN.md` Phase 4 任务 2「淘汰 GameCoreGameplayServices 等静态服务定位器」与 Phase 1 任务 3「Fallbacks 契约梳理与瘦身」。

---

## 一、现状分层（2026-10-10 核实）

```text
消费方（数千个静态调用点，约 4,824 行引用）
   │  GameThreadPoolServices.threadPoolManager()  2,260 行
   │  GameWorldServices.xxx()                    1,287 行
   │  GameFeatureServices.xxx()                    692 行
   │  GameLocationBootstrapServices.xxx()          433 行
   │  GameCoreGameplayServices.xxx()               152 行
   ▼
L1 定位器（@Component + ObjectProvider 静态缓存 + fallback）
   ▼
L2 RuntimeBridge（lifecycle 包内装配层）
   ▼
L3 Fallbacks（11 个 GameXxxFallbacks，约 800 行：离线/降级懒加载单例或 fail-fast）
```

- 定位器为 Spring 迁移期的**刻意过渡态**：构造注入 `ObjectProvider` 写静态字段，静态方法解析（Spring 优先 → Fallbacks 兜底 → 缓存），`DisposableBean` 销毁清空。
- Fallbacks 已出现**两种终态先例**：`GameCoreServiceFallbacks.dataManager()` 直接 fail-fast（双源兜底已退役组件）；`threadPoolManager()`/`htmlCache()` 仍保留懒加载单例兜底。
- 已确立的**单一所有权 / bind 桥先例**：`TransformPanelSync`（`bindPlayerTransformDAO` + 双检锁 safeDao 兜底）、`ItemSocketService`、`PlayerService.daos()` 强类型聚合、`LegionService` 包级访问器伴生域模式。

## 二、消费方三分类判定规则

| 类别 | 判定 | 终态 |
|---|---|---|
| **A 可注入化** | 类本身是 Spring bean（`@Component`/`@Configuration` 装配类/bean 方法宿主） | 构造注入目标 bean，删除静态调用；在对应 `XxxBeans` 配置类 `ObjectProvider` 装配 |
| **B 永久门面** | 无 bean 生命周期的静态宿主（静态工具类、枚举、JAXB 模型、静态嵌套任务类、客户端 packet 类、命令类） | 宿主自持 `bind` 入口 + 启动期由 `CoreRuntimeServiceBeans` 等显式绑定（单一所有权），调用点改经宿主门面；禁止散布定位器调用 |
| **C fail-fast 退役** | 生产环境必有 bean、fallback 无真实场景（参照 `dataManager()` 先例） | Fallbacks 方法改为 `Objects.requireNonNull` 或 `getInstance()` fail-fast，删除懒加载兜底 |

## 三、分域批次路线（引用量升序，先小后大验证规则）

| 批 | 域 | 引用量 | 关联 Fallbacks |
|---|---|---|---|
| R1 | `GameCoreGameplayServices`（Drop/Mail/Pvp/AutoGroup/AbyssRankingCache/Legion/Balaur/Union） | 152 | GameCoreServiceFallbacks |
| R2 | `GameLocationBootstrapServices` | 433 | GameWorldBootstrapFallbacks |
| R3 | `GameFeatureServices` | 692 | GameEventBootstrap/RuntimeFallbacks |
| R4 | `GameWorldServices` | 1,287 | GameWorldServiceFallbacks |
| R5 | `GameThreadPoolServices` + `GameShutdownHookFallbacks` | 2,260 | ShutdownHook/Maintenance/EngineService/Battlefield/ServerNetwork 系列 |

每批固定动作序列：
1. 该域全部消费方按 A/B/C 分类（`jbcontext search` 语义定位 + `grep` 清点）；
2. A 类构造注入化；B 类建 bind 桥（单一所有权）；C 类 fallback fail-fast 化；
3. 删除定位器中被清空的方法与 Fallbacks 中对应兜底；
4. 更新桥接门禁测试（`GameFeatureServicesRuntimeBridgeTest` 等 9 项）与新增域门禁；
5. 全量构建 + 反向 grep 该域定位器引用归零 + 已授权测试套件全绿。

## 四、验收门禁（战役级）

- 全库 `GameXxxServices.` 静态调用仅剩 B 类宿主门面内部实现与测试断言；
- 11 个 Fallbacks 中 C 类全部 fail-fast，B 类兜底仅存在于有 bind 桥的宿主内（不再经独立 Fallbacks 类）；
- 桥接门禁测试全绿 + 全量构建零错误；
- `GameThreadPoolServices`（R5 最后一批）退役后，`ThreadPoolManager.setInstanceProvider` 静态注入通道同步拆除。

## 五、关联遗留项处置

- **JSR-380 配置校验（Phase 4.1 深化）**：基础设施已落地（`spring-boot-starter-validation` 依赖 + `VipConfig` 试点 `@Validated`/`@Valid`/`@Min`/`@Max`，`LoginConfigBindingTest.validatorRejectsOutOfRangeVipLevel` 验证；遗留直写路径仍由手写 `validate()` 守护）。后续按批次为数值范围型配置字段补约束注解，逐类验证，严禁批量套模板。
- **Tick 调度与 JDBC 阻塞 IO 物理隔离（Phase 3 遗留）**：#8（`PeriodicSaveService` 收拢）与 #10（脏标记短路）已消除周期存盘主通道；#17（8 分片条带化）已解包调度锁。剩余风险为散点任务（`scheduledPool` 上的非周期任务、cron 任务内的同步 JDBC）在逻辑线程直连数据库。**处置：先以 JFR 运行期采样取证（注意与 JRebel 冲突，需二选一），拿到阻塞热点证据后再定改造清单，禁止无证据重构并发架构。**
- **密集只读表二分（Phase 2 补充）**：已回退（2026-10-12）——冻结前提「运行期外层键集不变」不成立（`//spawn` 保存写 `customs`、事件注入刷怪地图写世界键），冻结后 `//reload_spawn` 之后的保存与事件会抛 `IllegalStateException`；同时冻结容器破坏多源分区反射合并、导致启动丢失地点刷怪索引。现 `SpawnsData2` 25 张索引表回退 `IntObjectHashMap`，`FrozenIntObjectTable` 及其专项测试一并删除；多源合并改由显式 `merge(SpawnsData2)` 承载。
- **`QuestRuntimeComposition` 8 处**：quest 线活跃域，待其收敛后按 bind 桥模式（参照 `TransformPanelSync`）收口。
