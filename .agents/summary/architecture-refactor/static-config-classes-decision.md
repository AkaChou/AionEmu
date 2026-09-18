# 静态配置类：迁移判定与审计结论（2026-09-18）

## 问题

`@Property` 配置类共 **52** 个（**759** 个字段 / **756** 个键）。此前只有 `ThreadConfig`、`SvStatsConfig`
被实例绑定（Spring Bean），其余 **50** 个仍是"纯静态字段 + `Config.load()` 写入"。

本阶段要回答的是：**其余 50 个是否也该迁成实例绑定？** 判定标准不是行数或字段数，而是
"该类是否被 Spring Bean 在**构造期**消费"。

## 关键机制

启动顺序（`AionServiceLauncher implements ApplicationRunner`）：

```text
容器刷新（所有 Bean 构造 + @PostConstruct）
  → AionServiceLauncher.run()（ApplicationRunner）
  → 按相位执行 AionServiceLifecycle
  → GameUtilityServicesLifecycle.start() → utilityServicesGateway.loadConfig() → Config.load()
```

也就是说：**任何 Bean 在构造器 / 字段初始化器 / `@PostConstruct` 里读静态配置字段，读到的都是
加载前的占位值（0 / null / 字段初始值），而不是配置文件里的值。** 只有"方法体内、且在 `Config.load()`
之后的调用点"读静态字段才是安全的。

## 审计结果

审计脚本（均为只读，放在本目录）：

| 脚本 | 结论 |
|---|---|
| `audit_static_config_classes.py` | 每个配置类的消费方构成（引用类数 / 其中 Spring Bean 数） |
| `audit_early_config_reads.py` | Bean 内静态配置读取点 **30 处，全部在生命周期/网关方法体内**；构造期读取 **0 处** |
| `audit_config_keys.py` | 键总数 756；重复键 3；无"既不在配置文件又无 `defaultValue`"的键 |
| `audit_dead_config_fields.py` | 类外无引用且类内也无引用 **14** 个；仅类内使用 3 个 |

### 构造期读取：0 处（关键证据）

30 个 Bean 内读取点逐一核对后，全部位于 `start()` / `isXxxEnabled()` / `createNettyTransport()` 这类
**运行期方法**，由 `AionServiceLauncher` 在 `Config.load()` 之后按相位调用。唯一命中的 Bean 工厂方法是
`SiegeBattlefieldBeans.maintenanceTask()`（`@Bean @Lazy`，读 `HousingConfig.HOUSE_MAINTENANCE_TIME`），
它由 `ObjectProvider` 在运行期解析，仍在 `Config.load()` 之后。

### 重复键：3 处，均可解释

| 键 | 绑定位置 | 说明 |
|---|---|---|
| `chatserver.network.public.address` | `gameserver...NetworkConfig.PUBLIC_CHAT_ADDRESS` / `chatserver.configs.Config.PUBLIC_CHAT_ADDRESS` | 两个模块各自持有的镜像声明 |
| `chatserver.network.gameserver.address` | `NetworkConfig.CHAT_ADDRESS` / 聊天服 `Config.GAME_ADDRESS` | 同上 |
| `chatserver.network.gameserver.password` | `NetworkConfig.CHAT_PASSWORD` / 聊天服 `Config.GAME_SERVER_PASSWORD` | 同上 |

三个键都定义在**同一个** `aion/config/network/network.properties`，游戏服与聊天服各自加载该文件
（聊天服 `Config.loadProperties()` 的第 0 个文件就是它），因此两个字段的取值必然一致，属于刻意镜像，
不需要合并。

### 未被任何加载器处理的配置类：0

52 个含 `@Property` 的类全部出现在 `ConfigurableProcessor.process(Xxx.class, ...)` 调用里，不存在
"新增了配置类但忘了挂进加载器"的情况。

### 死字段：14 个（登记，暂不删除）

`AdvCustomConfig.CRAFT_DELAYTIME_RATE`、`AutoGroupConfig.IDTM_LOBBY_E01_*` / `_P02_*`（6 个）、
`BrokerConfig.SAVE_MANAGER_INTERVAL`、`loginserver Config.ACCOUNT_CHARSET`、`EventsConfig.EVENT_GIVE_JUICE`、
`EventsConfig.EVENT_GIVE_CAKE`、`GroupConfig.TEAM2_ENABLE`、`SecurityConfig.CAPTCHA_EXTRACTION_BAN_ADD_TIME`、
`SecurityConfig.CAPTCHA_BONUS_FP_TIME`。

判定为**暂不删除**的理由：

- 其中 5 个（`broker.save.manager.interval`、`events.give.cake`、`events.give.juice`、
  `captcha.bonus.fp.time`、`captcha.extraction.ban.add.time`）在随包 `aion/config/**` 中**确有对应配置行**，
  删字段会留下无主键，反而更难解释；
- 其余 9 个虽然没有任何配置行，但都对应上游/待实现特性（IDTM 大厅调度、team2 开关、账号字符集），
  属于"预留但未接线"的旋钮，删除是产品决定而不是重构决定；
- 无论删除与否，运行期行为完全一致（当前没有任何代码读它们）。

## 决策

**其余 50 个静态配置类保持静态，不迁移为 Spring Bean。** 依据：

1. 构造期读取为 0 —— 没有"Bean 拿到过期值"的问题，实例绑定没有要解决的痛点；
2. 消费方绝大多数是非 Bean 的遗留类（如 `CustomConfig` 被 67 个类引用，其中 Bean 仅 2 个），
   迁移需要重写整张对象图，收益与风险不匹配；
3. 覆盖能力已经具备：命令行 / 环境变量 / `application.yml` 经 `ConfigSourceResolverHolder` 对静态字段
   同样生效（只有 `ThreadConfig`、`SvStatsConfig` 因为有真实的实例绑定需求才成为 Bean）。

出现下列情况时才重新评估：某个配置类开始被 Bean **在构造期**读取（本次审计脚本可直接复用），或需要
`@ConfigurationProperties` 的校验 / 刷新语义。
