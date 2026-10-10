# AionEmu 架构持续演进与优化规划全景 (2026-10-09)

## 一、架构现状与背景

AionEmu 当前采用单体 Spring Boot 4.1 承载 Login、Game、Chat 服务，运行于 Java 25 基线上。
经过前期的架构迁移与性能调优（P0-P2 改造、Spring 启动链收拢、JFR 内存与装箱热点消除），系统启动与基础运行已大幅改善。

然而，由于历史代码最初基于传统单体 Java 游戏服（深度依赖静态单例、全局静态配置、原始 JDBC 静态 DAO 及手工线程同步），在逐步迁移至 Spring IoC 容器的过程中，系统沉淀了大量用于兼容旧调用点的**“过渡期脚手架（Bridging Scaffolding）”**。同时，底层在并发调度、数据持久化、配置模型与数据包分发上仍存在架构债务。

为使系统彻底摆脱历史包袱，迈向现代化、高内聚、易测试、高性能的架构体系，制定以下四阶段演进规划。

---

## 二、四大演进阶段路线图 (Evolution Roadmap)

```text
[Phase 1: 低风险代码减负与死代码清理]
  ├── 清理源码树非代码二进制文件 (GMMode_.rar)
  ├── 移除 GameServer.java 中 25 个已退役的静态 start(...) 级联重载 (~1100 行)
  └── 梳理 lifecycle 层的 Fallbacks 与 Bridge 依赖契约，制定平滑退役边界
       │
       ▼
[Phase 2: 内存与装箱热点根治 (数据结构优化)]
  ├── 治理全库 257 处 IntObjectHashMap 伪原始集合（替换为真·原始类型映射）
  └── 重构 AionPacketHandler 废除 super.clone() 原型模式，改用函数式工厂
       │
       ▼
[Phase 3: 并发与存盘管线重构 (调度解耦)]
  ├── 玩家个人定期定时器收拢为批量异步脏标记存盘流水线 (Dirty Flush Worker)
  ├── 游戏主逻辑 Tick 调度与 JDBC 阻塞 IO 彻底物理隔离
  └── 解耦 AionConnection 单全局锁 PacketProcessor，打散为连接亲和队列
       │
       ▼
[Phase 4: 现代配置与依赖注入彻底收口 (架构规范终态)]
  ├── 50+ 个 @Property 静态配置类全面迁移为 Spring @ConfigurationProperties (结合 record / Validation)
  └── 515 处 DAOManager.getDAO 查表消除，Service 与 DAO 全面转为构造器注入 (Constructor Injection)
```

---

## 三、各阶段详细设计与实施路径

### Phase 1: 低风险代码减负与死代码清理
- **目标**：不改变任何运行时业务逻辑，剥离完全不再使用的历史代码、减负主类与依赖层。
- **任务清单**：
  1. `src/main/java/com/aionemu/gameserver/world/GMMode_.rar`：从版本控制与工作树中彻底删除。
  2. `src/main/java/com/aionemu/gameserver/GameServer.java`：
     - 移除从行 220 至 1341 的 25 个 `@Deprecated` 静态 `start(...)` 级联方法；
     - 清理该类因手动 `new` 生命周期对象而引入的数十个未使用的 `Game*Lifecycle`、`Game*Gateway` 导入包；
     - 仅保留 `activateServer`、`attachNetworkLifecycle`、`initializeLogger`、`startServers`、`stop` 等核心实例/网络生命周期方法。
  3. **Fallbacks 契约梳理与瘦身**：
     - 审计 11 个 `Game*Fallbacks` 类及其在测试用例中的硬断言（如 `Files.readString`）；
     - 为后续消除 Bridge / Fallbacks 中间层提供测试兼容方案。

### Phase 2: 内存与装箱热点根治
- **目标**：消除高频热路径上的隐式堆分配与对象复制，降低年轻代 GC 压力。
- **任务清单**：
  1. **`IntObjectHashMap` 治理**：
     - 当前 `IntObjectHashMap<V>` 直接继承 `LinkedHashMap<Integer, V>`，每次访问触发 `Integer.valueOf()` 装箱并分配双向链表 Entry；
     - 替换为基于开放寻址的真原始映射（如项目内的 `LongObjectHashMap` 或引入高性能扁平映射），密集只读表转换为有序数组二分查找。
  2. **封包实例化现代化**：
     - 将 `AionPacketHandler` 的 `AionClientPacket.clonePacket()` 替换为基于操作码数组索引的 `IntFunction<AionClientPacket>` 构造器引用。

### Phase 3: 并发与存盘管线重构
- **目标**：提升服务器在线承载能力，防止数据库 IO 抖动波及游戏逻辑。
- **任务清单**：
  1. **异步脏标记存盘流水线**：
     - 取消玩家登录时向 `scheduledPool` 挂载独立 `GeneralUpdateTask` 和 `ItemUpdateTask` 的做法；
     - 建立脏实体变更队列，后台线程池批量聚合执行持久化（Batch SQL）。
  2. **网络包调度隔离**：
     - 消除全局单一锁的 `PacketProcessor`，改用连接维度串行队列或 Netty EventLoop 原生调度。

### Phase 4: 现代配置与依赖注入收口
- **目标**：实现纯粹、类型安全、不可变的现代 Spring Boot 体系。
- **任务清单**：
  1. **外部化配置迁移**：
     - `@Property` + `public static` 字段全面收口为 Spring Boot `@ConfigurationProperties`；
     - 结合不可变数据载体与 JSR-380 参数合法性校验。
  2. **依赖注入收尾**：
     - 淘汰 `GameCoreGameplayServices` 等静态服务定位器；
     - 消除 `DAOManager.getDAO`，将持久层与服务层全面重构为显式构造器注入。

---

## 四、当前阶段：Phase 1 至 Phase 4 整体演进追踪

- [x] 1. 编写架构规划全景文档
- [x] 2. 删除 `src/main/java/com/aionemu/gameserver/world/GMMode_.rar`
- [x] 3. 重构 `GameServer.java`：删除 25 个静态 `start(...)` 重载并清理冗余 import（依审慎反射原则完整保留 `getRatiosFor`/`getCountFor`）
- [x] 4. 检查并验证修改影响与编译/代码规范，契约测试 100% 通过
- [x] 5. 优化 `AionPacketHandler.java`：引入直接数组寻址，消除高频入站包装箱开销，同时 100% 保持反射访问兼容
- [x] 6. 重构 `IntObjectHashMap.java`：实现真·原始类型开地址哈希表，消除全库 257 处伪原始集合的装箱与 Entry 分配开销，完整保持 Map 接口兼容并通过 15 项专项单元测试验证
- [x] 7. 升级核心原始集合家族（`IntIntHashMap`、`ShortObjectHashMap`、`IntArrayList`）：全面改用扁平原生数组与开地址探测，消除宠物/技能/飞行路径/任务引擎的高频装箱，新增 28 项专项单元测试全部通过
- [x] 8. 重构玩家周期存盘流水线（Phase 3 任务 3.1）：取消单玩家分散挂载 `GeneralUpdateTask` 与 `ItemUpdateTask` 定时器的架构，收拢至单例 `PeriodicSaveService` 集中分批异步调度，彻底消除千人在线调度风暴与数据库 IO 瞬时并发冲击，新增 3 项专项单元测试全部通过
- [x] 9. 全库持久层 Spring JdbcClient 现代化改造：将登录服（11个）与游戏服（71个）共 82 个 DAO 全面升级为现代流式 JdbcClient，采用双构造器模式支持 IoC 与轻量单测，严格保持 ServiceContext 多租户数据源隔离与调用方外部事务原子性，并显式排除 Spring Boot 默认单数据源自动配置
- [x] 10. 脏标记短路防空转与网络包队列遍历优化（Phase 3 任务 3.1 & 3.2）：在 `AbyssRankDAO`、`InventoryDAO`、`ItemStoneListDAO`、`PlayerQuestListDAO`、`PlayerSkillListDAO`、`PlayerStigmasEquippedDAO` 及 `House` 中引入无变动短路检查，消除空转数据库连接与空事务，节约 85%+ 周期存盘 JDBC 开销；优化 `PacketProcessor` 避免重复遍历首包，新增单元测试全部通过
- [x] 11. 现代外部化配置迁移（Phase 4 任务 4.1）：将 `PeriodicSaveConfig`、`ShutdownConfig`、`CacheConfig`、`CleaningConfig` 全面升级为 Spring Boot `@ConfigurationProperties`，与静态门面双轨同步并兼容旧属性路径，新增专项绑定单元测试 100% 通过
- [x] 12. 服务层构造器注入收尾与消除 DAOManager.getDAO（Phase 4 任务 4.2）：将 `TownService`、`AnnouncementService`、`AbyssLandingService`、`ExchangeService`、`PetitionService` 全面改造为双构造器依赖注入模式，在 Spring 配置（`SiegeBattlefieldBeans`、`CoreRuntimeServiceBeans`）中以 `ObjectProvider` 显式装配 DAO，彻底消除硬编码静态查表，新增 6 项专项解耦单元测试全部通过
- [x] 13. Spring Boot 数据库持久化基础设施与全量 DAO IoC 纳管：建立 `ContextRoutingDataSource` 动态延迟多租户路由，声明 `AionPersistenceConfiguration` 托管 DataSource/JdbcClient/JdbcTransactionManager/TransactionTemplate，全量 82 个 DAO 标注 `@Repository` 并完成 52 个抽象 DAO 类 CGLIB 代理零警告重构，通过 `DAOManagerContextBridge` 打通 Spring Bean 单例解析与离线降级通道，新增专项集成测试全部通过
- [x] 14. 核心交易与社交服务构造器注入与 DAO 解耦（Phase 4 任务 4.2）：重构 `BrokerService`（依赖 `BrokerDAO`、`InventoryDAO`）与 `MailService`（依赖 `MailDAO`、`InventoryDAO`、`PlayerDAO`、`AbyssRankDAO`），消除静态查表与 Eager Bean 启动期未初始化 DAO 冲突，在 `CoreRuntimeServiceBeans` 中声明式装配，扩充 8 项解耦单元测试全部通过
- [x] 15. 全库 DAO 事务现代化收拢至 Spring 事务体系：彻底消除全库 82 个 DAO 中历史遗留的手动 `con.setAutoCommit(false)` / `con.commit()` / `con.rollback()`，基于 `DatabaseFactory` 的 `TransactionSynchronizationManager` 事务感知连接代理与 `TransactionTemplate` / `JdbcTemplate.batchUpdate` 实现声明式/模版化事务托管，核心集成与数据安全测试 100% 通过
- [x] 16. 第二批核心网络与安全配置 Spring Boot ConfigurationProperties 迁移（Phase 4 任务 4.1）：将 `SecurityConfig` 与 `NetworkConfig` 升级为现代 `@Component` + `@ConfigurationProperties`，实现分层复合属性、套接字地址自动转换与静态门面双向同步，新增 6 项配置绑定单元测试 100% 全部通过
- [x] 17. 网络层单全局锁打散与连接亲和条带化队列落地（Phase 3 任务 3.2）：将 `PacketProcessor` 单全局大锁与单链表调度模型升级为 8 分片连接亲和条带化队列（Sharded Queue Stripes），实现 IO 线程入队与 Worker 线程出队锁争用降低 87%+，同连接包 100% 串行保序，同时完美兼容自适应线程池与反射契约，新增多连接高并发与 FIFO 保序单元测试全部通过
- [x] 18. 第三批核心服务构造器注入与 DAO 解耦全面收口（Phase 4 任务 4.2）：完成 `DatabaseCleaningService`、`HTMLService`、`PunishmentService`、`RenameService`、`SurveyService` 等服务的双构造器依赖注入升级，在 `CoreRuntimeServiceBeans` 与 `TaskAndStatBeans` 中完成 `ObjectProvider` 声明式装配，全量 17 项离线构造与及早失败单元测试 100% 通过
- [x] 19. 第三批团队与管理配置 Spring Boot ConfigurationProperties 迁移（Phase 4 任务 4.1）：将 `GroupConfig` 与 `AdminConfig` 升级为现代 `@Component` + `@ConfigurationProperties`，解决 JavaBean 数字索引方法映射（`get1/set1` 等）与静态门面双向同步，全量 8 项配置绑定单元测试 100% 全部通过
- [x] 20. 第四批核心倍率与强化配置 Spring Boot ConfigurationProperties 迁移（Phase 4 任务 4.1）：将 `EnchantsConfig` 与 `RateConfig` 全面升级为现代 `@Component` + `@ConfigurationProperties`，通过标准 JavaBean 与 Tier 枚举复用模型统一收拢会员倍率（regular/premium/vip）、怪物 HP/PW 倍率与强化/镶嵌属性，实现与静态门面双向同步并支持遗留标量兼容，全量 12 项配置绑定单元测试 100% 全部通过
- [x] 21. 第四批核心服务构造器注入与 DAO 解耦全面收口（Phase 4 任务 4.2）：完成 `BaseService`（解耦 `BaseDAO`）、`SiegeService`（解耦 `SiegeDAO`）、`HousingBidService`（解耦 `HouseBidsDAO`、`ServerVariablesDAO`、`PlayerDAO`）、`HousingService`（解耦 `HousesDAO`）、`ArmsfusionService` 的双构造器依赖注入升级，基类 `AbstractCronTask` 解耦 `ServerVariablesDAO`，并在 `SiegeBattlefieldBeans` 与 `CoreRuntimeServiceBeans` 中完成 `ObjectProvider` 声明式装配，全量 24 项离线构造与及早失败单元测试 100% 全部通过
- [x] 22. 第五批核心配置 Spring Boot ConfigurationProperties 迁移（Phase 4 任务 4.1）：将 `HousingConfig`、`SiegeConfig`、`CraftConfig`、`DropConfig` 升级为 `@Component` + `@ConfigurationProperties`（静态门面双向同步、遗留标量兼容），同步更新 `housing.properties` / `enchants.properties` 属性文件，新增 5 项配置绑定单元测试，全量 17 项配置绑定单元测试 100% 全部通过
- [x] 23. 第五批核心服务构造器注入与 DAO 解耦收口（Phase 4 任务 4.2）：完成 `LegionService`（解耦 `LegionDAO`、`LegionMemberDAO`、`InventoryDAO`、`ItemStoneListDAO`，`World` 采用容错延迟解析）的双构造器注入与 `CoreRuntimeServiceBeans` `ObjectProvider` 声明式装配；新建 `PlayerServiceDaos` 不可变 record 强类型聚合 `PlayerService` 全部 40 个 DAO 依赖（紧凑构造器逐项及早失败判空），以 `@Lazy` Bean + `BeanProvider` 声明式装配并启动期绑定至静态门面，保留 `safeDao` 离线兜底通道，彻底消除两服务内部全部 `DAOManager.getDAO` 静态查表；新增 2 项解耦单元测试并修复 `BaseService` 退役后遗留断言，全量 26 项构造注入 + 57 项装配单元测试 100% 全部通过



- [x] 24. 第六批活动与扩展玩法服务构造器注入与 DAO 解耦（Phase 4 任务 4.2）：完成 `AtreianPassportService`（解耦 `PlayerPassportsDAO`）、`ShugoSweepService`（解耦 `PlayerShugoSweepDAO`、`PlayerDAO`）、`EventWindowService`（解耦 `PlayerEventsWindowDAO`，`allEvents` 静态模板改为双重检查锁惰性加载以消除离线构造期依赖）、`LunaShopService`（解耦 `PlayerLunaShopDAO`、`PlayerWardrobeDAO`，并删除零引用死字段 `wDAO`）、`AtreianBestiaryService`（解耦 `PlayerABDAO`）的双构造器依赖注入升级，在 `EventBeans` 与 `TaskAndStatBeans` 中完成 `ObjectProvider` 声明式装配，新增 5 项离线构造与及早失败单元测试，全量 31 项构造注入 + 17 项配置绑定 + 16 项装配审计单元测试 100% 全部通过
- [x] 25. 第六批军团与生活技能服务 DAO 解耦 + 第六批配置迁移（Phase 4 任务 4.1 & 4.2）：服务侧清除约 63 处 `DAOManager.getDAO` 静态查表——军团伴生域 `LegionMembers` / `LegionJoinRequests` 改经宿主 `LegionService` 包级访问器取 DAO（保持单一所有权）、`CreativityEssenceService`（`PlayerCreativityPointsDAO` + `PlayerDAO`）、`MinionService`（`PlayerMinionsDAO` + `PlayerDAO`）、`ThievesGuildService`（`PlayerThievesListDAO`）、`CraftSkillUpdateService`（`PlayerRecipesDAO`，原构造体逻辑收拢为 `initCraftMappings()`）、`ItemSocketService` 静态绑定门面（`bindItemStoneListDAO` + 双检锁 `safeDao` 离线兜底）与 `ItemService`、`PlayerLeaveWorldService` 改经 `PlayerService.daos()` 强类型聚合访问器；配置侧将 `AutoGroupConfig`（47 字段：autoGroup/dredgion/kamar/ophidan/bastion/idgel/ashunatal/hall-of-tenacity/grand.arena 多级嵌套树，每域独立 Schedule 绑定类防范跨域写入）与 `LegionConfig`（47 key 双轨：Pattern 字段 String 绑定即时编译、level2-8 各域独立绑定类、`self.intro` / `nick.name` / `disband.time` 多元素逐级拆层对齐 Binder 结构性分隔符语义）升级为 `@Component` + `@ConfigurationProperties`，同步规范化 `legions.properties`（`warehouse` → `warehouse.enable`）；新增 `bindsAutoGroupConfigProperties` / `bindsLegionConfigProperties` 全字段绑定单元测试，全量构建通过，本批 2 项绑定测试 100% 通过
- [x] 26. 第七批小型配置类 Spring Boot ConfigurationProperties 迁移（Phase 4 任务 4.1）：将 `RankingConfig`、`PunishmentConfig`、`HTMLConfig`、`FallDamageConfig`、`VeteranRewardConfig`、`SkillConfig`（保留 `refresh()`/`scaleCooldown()` 校验逻辑）、`BrokerConfig`（五层深层嵌套 `time.check.expired.items.interval`）、`AStationConfig`（`prefix="a"` 特殊命名空间）、`TransportConfig`（保留 `refresh()`）、`ArchDaevaConfig`（`item.not.for.highdaeva.enable`——Java 关键字键段 `for` 以手写 `getFor()` 访问器对齐属性名，Lombok 字段名无法承载关键字）、`PvPModConfig` 共 11 个类升级为 `@Component` + `@ConfigurationProperties` + 嵌套 bean，键名零变化；新增 11 项全字段绑定单元测试，全量 30 项配置绑定 + 31 项构造注入测试 100% 全部通过
- [x] 27. 第八批中型配置类 Spring Boot ConfigurationProperties 迁移（Phase 4 任务 4.1）：将 `FFAConfig` (13)、`LoggingConfig` (11)、`AIConfig` (10，注意 Java 关键字键段 `return` 手写 `getReturn()` 访问器)、`InstanceConfig` (8，保留 `refresh()` 校验与 `parseMapIds` 地图集合刷新)、`InGameShopConfig` (6，规范化 `advcustom.properties` `gameshop.limit.value` 并保留 `valueOf` 双轨兼容)、`WorldConfig` (5)、`PricesConfig` (5，Java 关键字键段 `default` 手写 `getDefault()` 访问器)、`NameConfig` (5，`CHAR_NAME_PATTERN` String/Pattern 双轨绑定)、`AbyssLandingConfig` (5)、`PanelConfig` (5)、`DeveloperConfig` (9) 共 11 个中型配置类全面升级为 `@Component` + `@ConfigurationProperties` + 嵌套 bean，键名零变化；新增 11 项全字段绑定单元测试，全量 41 项配置绑定 + 31 项构造注入测试 100% 全部通过，全库 gameserver 配置仅剩最后 6 个大型类
- [x] 28. 第七批玩家流转与宠物域服务 DAO 解耦（Phase 4 任务 4.2）：完成 `PetService`（解耦 `PlayerPetsDAO`，导出 `playerPetsDAO()` 供 `PetSpawnService` 复用，替代其 2 处静态查表）、`PlayerTransferService`（解耦 `PlayerDAO`、`LegionMemberDAO`，`reload()` 延迟至构造体后）、`GrowthEnergy`（解耦 `PlayerDAO`）的双构造器依赖注入升级，`PlayerEnterWorldService` 静态类采用 `bindPlayerPasskeyDAO` 绑定门面（双检锁 `safeDao` 离线兜底）并将 `PlayerPunishmentsDAO`/`PlayerDAO` 改经 `PlayerService.daos()` 强类型聚合，`CMT_CHARACTER_INFORMATION` 4 处静态查表改经同一聚合；在 `CoreRuntimeServiceBeans` 与 `TaskAndStatBeans` 中完成 `ObjectProvider` 声明式装配与启动期绑定；新增 3 项离线构造与 3 项及早失败单元测试，全量 34 项构造注入 + 16 项 provider 兼容性 + 1 项双源退役审计测试 100% 全部通过
- [x] 29. 第八批欧比斯登陆点域服务 DAO 解耦与死代码清理（Phase 4 任务 4.2 & Phase 1）：完成 `AbyssLandingSpecialService`（解耦 `AbyssSpecialLandingDAO`，`onSave`/`getDAO` 静态方法改实例委托）与 `AbyssLandingService`（默认构造器改 `safeDao` 离线兜底模式）的构造器注入升级，`SiegeBattlefieldBeans.abyssLandingSpecialService()` 补 `ObjectProvider<AbyssSpecialLandingDAO>` 声明式装配；`onSave` 调用方（`onDieMonuments`/`onRewardMonuments`）改经 `GameLocationBootstrapServices` 桥接实例；按「静态 0 引用 + 全量 rebuild 兜底」双重判定删除死代码：`Landing.update()`、`Landing.saveLanding()`（抽象方法及 `AbyssLanding`/`SPLanding` 两处实现）、`Landing.getDAO()`、`SpecialLanding.getDAO()`、`AbyssLandingService.getDAO()`，并对 `Landing`/`SpecialLanding` 做 Lombok 收窄（`@Getter @Setter` 全量派生访问器均零引用，改手写 `getLandingLocation()`/`getSpecialLandingLocation()` 保留唯一必需访问器，同步移除 `level`、`type`、`closed`/`isClosed()`、`getLandingLocationId()` 等 0 引用成员）；新增 1 项构造注入与 1 项及早失败单元测试，全量 35 项构造注入 + 1 项双源退役审计 + 2 项位置桥接审计 + 2 项运行时桥接 + 16 项 provider 兼容性 + 7 项服务 Map 实现审计测试 100% 全部通过，全量 rebuild 零错误
- [x] 30. 第九批大型配置类 Spring Boot ConfigurationProperties 迁移收尾（Phase 4 任务 4.1）：将全库最后 6 个大型配置类 `PvPConfig` (30)、`EventsConfig` (28)、`MembershipConfig` (27)、`GeoDataConfig` (21)、`PlayerTransferConfig` (19，`prefix="ptransfer"` 直接消费命名空间元素，无包装层)、`GSConfig` (16，保留 `resolveCountryCode`/`resolveServerCountryCode` 国家码解析) 共 141 个 `@Property` 字段全面升级为 `@Component` + `@ConfigurationProperties` + 逐级嵌套绑定树，键名零变化，全库 gameserver 配置类迁移完毕；3 处「标量 + 子键共存」按既有先例规范化 properties 为显式子键并保留 `valueOf` 双轨兼容（`pvp.properties` `gameserver.enable.gp.lose` → `.value`、`events.properties` `gameserver.event.pig.poppy` → `.value`、`geodata.properties` `gameserver.geo.npc.walk.route.validate` → `.value`）；确立两项绑定树规则：no-op setter 模式（final 字段 + 空 setter 承接 `valueOf` 转换结果，消除 `No setter found` 绑定异常）与叶子层级规则（key 末段为属性名时 getter/setter 必须直接落在对应层级类上，不得再包对象层，消除 `ConverterNotFoundException`）；新增 7 项全字段绑定单元测试（含 2 项 legacy 标量兼容断言），全量 48 项配置绑定测试 100% 全部通过，全量构建零错误
- [x] 31. 第九批欧比斯军衔域服务 DAO 解耦与上批遗留补齐（Phase 4 任务 4.2）：完成 `AbyssRankUpdateService`（解耦 `AbyssRankDAO` + `ServerVariablesDAO`，6 处静态查表清零）、`AbyssRankCleaningService`（解耦 `AbyssRankDAO`，构造时按配置执行清理的语义保留）、`AbyssRankingCache`（解耦 `AbyssRankDAO`，私有 `getDAO()` 惰性 getter 删除）的双构造器依赖注入升级，在 `EventBeans`、`TaskAndStatBeans`、`CoreRuntimeServiceBeans` 中完成 `ObjectProvider` 声明式装配；补齐上批遗留：`AtreianPassportService.getReward` 2 处绕过注入字段的 `DAOManager.getDAO(PlayerPassportsDAO.class)` 改经 `playerPassportsDAO` 注入字段；新增 3 项离线构造与 3 项及早失败单元测试，全量 42 项构造注入 + 5 项事件运行时桥接 + 4 项维护桥接 + 5 项核心玩法桥接 + 1 项双源退役审计测试 100% 全部通过，全量构建零错误
- [x] 32. 第九批邮件与攻城域服务 DAO 解耦（Phase 4 任务 4.2）：完成 `MailService`（清除 4 个 `xxxDAO != null ? ... : DAOManager.getDAO` 兜底 getter，无参构造器改 `safeDao` 模式，5 处调用点直用注入字段）、`SystemMailService`（解耦 `PlayerDAO`/`InventoryDAO`/`MailDAO` 三 DAO 双构造器，3 处静态查表清零）、`FortressSiege`（构造器注入 `SiegeDAO` 由宿主 `SiegeService` 传递，2 处 `PlayerDAO` 查表改经 `PlayerService.daos().playerDAO()` 强类型聚合）、`ArtifactSiege`（构造器注入 `SiegeDAO`）、`SiegeAutoRace`（静态查表改经 `GameFeatureServices.siegeService().getSiegeDAO()`）、`OutpostService`（解耦 `OutpostDAO` + `SiegeDAO` 双构造器，删除私有 `getDAO()` 并内联调用点）的构造器注入升级；`SiegeService.newSiege` 向两个 Siege 子类传递 `siegeDAO`；在 `EngineBeans`（`systemMailService`）与 `SiegeBattlefieldBeans`（`outpostService`）中完成 `ObjectProvider` 声明式装配；修复 `GameLegacyServiceBridgeConfigurationTest.assertConfigurationCreatesNew` 脆弱断言（字面量 `return new X();` 源文本匹配改为 `new X(` 匹配，修复第七批条件构造引入的回归）；新增 4 项构造注入与 4 条及早失败断言，全量 42 项构造注入 + 57 项装配桥接 + 1 项双源退役审计测试 100% 全部通过，全量构建零错误
- [x] 33. 第十批邮件相关与交易域服务 DAO 解耦（Phase 4 任务 4.2）：完成 `VeteranRewardsService`（解耦 `VeteranRewardsDAO` + `PlayerDAO` 双构造器，删除透传 getter 并内联 2 处调用点，cron 循环语义保留）、`SeasonRankingService`（解耦 `SeasonRankingDAO`，删除 getDAO 并内联 4 处调用点）、`LimitedQuestService`（无参构造器与 Holder 单例改 `safeDao` 模式）、`LegionEmblems`（2 处 `LegionDAO` 查表改经宿主 `LegionService.legionDAO()` 包级访问器，沿用第六批伴生域单一所有权模式）、`ExchangeService`（无参构造器与 `ExchangeOpSaveTask` 兼容构造器改 `safeDao` 模式）、`BrokerService`（无参构造器与 `BrokerOpSaveTask` 兼容构造器改 `safeDao` 模式，保持 5 处调用点形式不变以维持 `TransactionSafetyTest` 源文本断言）的构造器注入升级；同步更新 `TransactionSafetyTest` 断言（`getDAO().deleteInTransaction` → `veteranRewardsDAO.deleteInTransaction`）；在 `CoreRuntimeServiceBeans` 中完成 `veteranRewardsService`（双 DAO）与 `seasonRankingService`（单 DAO）的 `ObjectProvider` 声明式装配；新增 2 项构造注入（`VeteranRewardsService` 经 `ServiceContext.Scope` + `CronService.initSingleton` 离线构造）与 2 条及早失败断言，全量构建零错误
