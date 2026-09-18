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

### 死字段：14 个（登记；见文末裁决，已按客户端证据处理）

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

## 追加裁决（2026-09-18 晚）：按客户端证据逐个定性

判定方法：对一个零引用旋钮，先在客户端与随包数据里找它的对应物——
客户端目录 `/Users/mc/IdeaProjects/5.8客户端`（`Levels/` 地图、`data/` 表、`docs/data/*ID和名字.txt`
抽取表）以及服务端 `aion/data/static_data/**`。**找得到对应内容⇒保留（属预留旋钮）；找不到⇒删除。**

### 保留：8 个（客户端确认存在）

| 字段 | 客户端/数据证据 |
|---|---|
| `EventsConfig.EVENT_GIVE_JUICE` | 物品 `160009017` = 苹果汁（`STR_EVENT_FOOD_DRINK_01`）；NPC 雷琳(799702)、箩雅(799703) |
| `EventsConfig.EVENT_GIVE_CAKE` | 物品 `160010073` = [扎库隆]蛋糕块（`STR_EVENT_FOOD_CAKE_01`）；NPC 布里奥斯(798414)、宝丹(798416) |
| `AutoGroupConfig.IDTM_LOBBY_E01_*`（3） | 客户端地图 `Levels/IDTM_LobbyE_01`；服务端 `auto_group.xml`(id=129/302370000)、`AutoGroupType.IDTM_LOBBY_E_01`、`WorldMapType`、`instance_cooltimes.xml` |
| `AutoGroupConfig.IDTM_LOBBY_P02_*`（3） | 客户端地图 `Levels/IDTM_LobbyP_02`（另有 `data/Dialogs/idtm_arena`）；服务端 `auto_group.xml`(id=128/302420000)、`AutoGroupType.IDTM_LOBBY_P_02` |

> IDTM 家族里 `IDTM_LOBBY_P_01`(127/302390000) 已由 `GrandArenaTrainingCampService` 用
> `GRAND_ARENA_TRAINING_CAMP_*` 三件套接线（`isGrandArenaTrainingCamp()` 只对 P_01 返回 true）。
> 即 E_01 / P_02 的大厅调度尚未实现，配置旋钮保留待实现；接线时注意数据侧写法是
> `IDTM_LobbyE_01` / `IDTM_LobbyP_02`，与当前键名 `IDTM_Lobby_E01` / `IDTM_Lobby_P02` 不一致。

### 删除：6 个（客户端、数据、历史实现三处都无对应物）

| 字段 | 判定依据 |
|---|---|
| `BrokerConfig.SAVE_MANAGER_INTERVAL` | 旧"交易行存盘管理器"间隔；`BrokerService` 现用 `CHECK_EXPIRED_ITEMS_INTERVAL`，属被替代的残留；服务端持久化细节，客户端无对应物。同时删除 `main/broker.properties` 的键与注释 |
| `AdvCustomConfig.CRAFT_DELAYTIME_RATE` | 键 `gameserver.craft.delaytime,rate` 自带逗号（疑似两个键拼错），无配置行、无实现；制作延迟由 `recipeTemplate.getCraftDelayTime()` 数据驱动 |
| `loginserver Config.ACCOUNT_CHARSET` | `accounts.charset`，账号库字符集；现代代码按 Java String/UTF-8 处理，无配置行、无实现 |
| `GroupConfig.TEAM2_ENABLE` | "启用 Team2 系统"——`model.team2.*` 已是唯一实现（无旧/新双轨可切），开关没有语义，客户端也没有这个开关。无配置行 |
| `SecurityConfig.CAPTCHA_EXTRACTION_BAN_ADD_TIME` | 历史实现见 `9639ce717`：`+ CAPTCHA_EXTRACTION_BAN_ADD_TIME * count` 已被该"清理死代码"提交删除，只剩字段。同时删除 `main/security.properties` 的键与注释 |
| `SecurityConfig.CAPTCHA_BONUS_FP_TIME` | 同上：`9639ce717` 删除了 `increaseFp(TYPE.FP, CAPTCHA_BONUS_FP_TIME)`。同时删除 `main/security.properties` 的键与注释 |

注：验证码功能本身仍在（客户端字符串表有 `STR_MSG_CAPTCHA_*`，服务端有 `SM_CAPTCHA` /
`CAPTCHAUtil`），被删的只是"通过验证码奖励 FP""失败追加封禁"这两条已移除行为的旋钮。

### 验证

- `python3 .agents/summary/architecture-refactor/audit_dead_config_fields.py`：死字段从 14 降到 8，
  即剩余的正是上表"保留"的 IDTM(6) + 活动赠品(2)；
- `mvn -B clean test` → **3456 例，0 失败 0 错误，2 跳过，BUILD SUCCESS**。
