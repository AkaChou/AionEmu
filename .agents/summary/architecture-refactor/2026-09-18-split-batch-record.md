# 架构重构批次记录（大类拆分 / 碎片合并 / Lombok / Spring 边界）

> date: 2026-09-18
> branch: quest
> base: b2361c2cf

## 已完成提交

| Commit | 批次 | 内容 |
|---|---|---|
| `977a1c8fb` | Item 限制域 | `Item`（1086 行）→ 新增 package-private `ItemRestrictions` 静态策略（掩码叠加会员权限、仓库/交易/改造/出售、灵魂绑定、AP 提取、伊迪安、神石、注能、高阶守护者）；公开门面签名不变；零分配。测试 `ItemRestrictionsTest`（6 例，含权限跳过 soulbind 与掩码展开）。 |
| `3e63f82c6` | Player 冷却域 | `Player`（2565 行）itemCoolDowns → package-private `PlayerCooldowns`（对齐 `CreatureCooldowns` 先例）；门面 `isItemUseDisabled/getItemCoolDown/addItemCoolDown/removeItemCoolDown` 签名不变；`getItemCoolDowns()` 改手工门面；可空 map + live 视图语义固化（`PlayerCooldownsTest`，5 例）。 |
| `212e00ef4` | 死代码清理 | 删除全库零引用 `DropInfo`、`InstanceEngineManager`（无代码/反射/配置引用，非动态反射 AI 包树）。 |
| `8a417d734` | Player 每日限购域 | `Player` maxCountEvent → package-private `PlayerItemDailyLimits`；门面 5 方法签名不变；惰性可空 map + live 视图（EventItemsDAO 迭代删除依赖）固化（`PlayerItemDailyLimitsTest`，3 例）。 |
| `b0fb8fb51` | LegionService 徽章域 | 徽章上传/扣费生效/分包下发/成员广播（约 250 行）→ package-private `LegionEmblems`，与 `LegionMembers`/`LegionRestrictions` 同模式（惰性构造 + 宿主引用回调 addHistory/restrictions）；门面签名不变；徽章 debug 日志按 i18n 规则迁 `I18n.get` 并补双语 key ×3。测试：`LegionServiceTest`/`LegionContainerTest`/`LegionMemberContainerTest` + `LocalizedLogCallsTest` 通过。 |
| `98b429327` | Enchant 规则表 | `EnchantService`（1541→1266 行）静态物品规则查表（isEnhancedAncientFallusha、isGrayWolfAccessories、isArchdaeva×3、GloryShieldSkill，约 320 行）→ package-private `EnchantItemRules`；public static 门面保留，内部调用点直走规则类。测试 `EnchantServiceTest` 通过。 |
| `117c51ad3` | Teleport 变身面板 | `TeleportService2`（1640→1228 行）传送后变身面板同步（playerTransformation/archdaevaTransformation/instanceTransformation，约 450 行）→ package-private `TransformPanelSync`；public static 门面保留（CM_LEVEL_READY、PlayerController 调用方无感）。 |
| `11605b779` | 审计汇总 | 本文档初版。 |

## Spring Bean 边界整理（扫描结论：已达标）

全库扫描（src/main/java 全量）：**无 @Autowired 字段注入、无手写 Logger 声明（全部 @Slf4j）、无纯赋值构造器的 Spring Bean**（@Service/@Component/@Configuration/@Repository/@Controller 中未用 `@RequiredArgsConstructor` 且构造器为纯赋值的为 0）。早前 P0–P2 轮次已完成 Bean 边界与静态兜底退役（LegionService/Influence 的 ObjectProvider fail-fast 模式即产物），本轮无新增改动点。

## 明确保留（审计结论与理由）

- **ItemTemplate（733 行）**：JAXB 绑定类型（`@XmlAccessorType(NONE)`），字段即职责（静态物品属性定义），全部字段同一变化原因；`@Getter/@Setter` 已收尾。拆分只会引入间接层并威胁静态数据反序列化。红线：JAXB 实例字段严禁 final。
- **Battleground（1930 行）**：天梯/积分域已拆出 `BattlegroundLadder`；剩余主体是 4 个子类（TwoTeamBg/TwoTeamSmallBg/DeathmatchBg/SoloSurvivorBg）覆写的生命周期钩子（onDie/onLeave/onEndDefault/onSpectatorJoin/reconnectPlayer）与玩家准备/复位工具（freeze/heal/cdReset/teleport/preparePlayer，约 350 行）——后者深度耦合宿主状态（previousLocations/instance/mapId/计时），且被全部子类作为 protected 工具调用；外迁只产生搬运而非职责分离。scheduleAnnouncement 还存在对具体子类的 instanceof 判断，属模板方法族。
- **LegionService（徽章域拆分后约 1620 行）**：申请流/搜索/仓库历史/权限已全部一行门面委托 `LegionRestrictions`；成员域已有 `LegionMembers`；徽章域本轮已拆出。剩余成员管理/解散重建/公告历史与宿主缓存容器、在线成员广播深度交织，拆出需携带完整宿主引用，净收益为搬运。handleCharNameRequest/handleLegionRequest 是客户端协议分发器（switch exOpcode），属入口而非独立域。
- **QuestEngine（2333 行）**：任务事件总线/路由器，104 个方法几乎全为 onXxx 事件入口与 registerXxx 注册器，职责单一（任务事件路由），每入口被 QuestHandler/Controller 以既有签名调用；拆分只产生搬运。且 quest 域为并行任务活跃工作区（近期提交均为 quest 修复），本轮不动代码避免冲突。
- **QuestService（1561 行）**：计时器域（questTimer/invisibleTimer/cleanup*Timers/startManagedTimer/QuestTimerKey，约 250 行）为下一批最佳候选；同样因 quest 并行活跃域本轮保留。
- **实例脚本（`instance/handlers/scripts/` 92+ 类，单类 800–2300 行）**：按副本隔离的游戏脚本，每个类变化原因单一（对应副本流程），互相独立；拆分单个副本脚本属机械按行数拆类（禁止项）。
- **AI 包树（AggressiveNpcAI2 7543 行、RetailPatternAI2 3371 行等）**：`gameserver.ai` 动态反射包树，SDJ-001 红线，仅凭 IDE 无静态调用不可删/不可动。
- **geo 引擎（Matrix4f/PathData/Matrix3f/Vector3f/BoundingBox/Mesh/GeoMap/BufferUtils/BoundingSphere/FastMath/PathService）**：数学/几何库移植代码，库内聚（与 JDK 数学类同理）；另受 AR-010 寻路质量硬约束保护。
- **网络包（SM_SYSTEM_MESSAGE 24797 行、SM_INSTANCE_SCORE 1612 行）**：消息码/协议数据表，每方法对应一个消息码或序列化布局，职责单一。
- **碎片类保留**：`ABEntry/CPEntry/WardrobeEntry/EventWindowEntry/StigmaEntry/SkillSkinEntry`（DAO 持久化 + 网络包序列化契约边界）；`DisconnectionTask/GeneralUpdateTask/ItemUpdateTask/EventScheduleWrapper`（调度任务类型，独立生命周期）；boot/lifecycle 与 Logback filter（Spring/日志框架反射管理）。
- **Lombok 收尾边界**：手写访问器密度最高的类（GlobalRule 32 个、Influence 21 个、Player 74 个）均为语义化门面——访问器名与字段名脱钩（如 `getGlobalRuleItems()→gdItems`、`getAbyssElyosInfluence()→abyss_e`），类级 `@Getter/@Setter` 会生成重复访问器污染 API 面。按"不为了 Lombok 而 Lombok"保留。仓库单实现接口数量为 0，无 SPI 碎片可删；新类均用 package-private final class + 静态纯函数/惰性注册表模式，未新增接口/Helper/DTO。

## 未执行的全量验证

- 全量 `mvn test`：未授权，未执行。此前阻塞 testCompile 的 `QuestRetailStartMetadataGateTest`（Map.of 28 参数）已由并行会话在 `fbfbaba1c` 修复为 `Map.ofEntries`，当前 testCompile 无阻塞。
- 启动服务器/真实客户端验收：未执行（用户管理生命周期）。
- JFR 性能采样：本轮全部拆分为静态策略/查表/注册表迁移或逐字搬运，无热路径新增分配；未做采样对照。

## 下一批候选（优先级）

1. `QuestService` 计时器域（约 250 行，QuestTimerKey 已有私有类型基础）——待 quest 并行任务收尾后实施。
2. `Skill`（1950 行）：施法阶段/效果链/冷却审计。
3. `PlayerController`（1453 行）、`LadderService`（1473 行）、`AttackUtil`（1458 行）、`EffectController`（1366 行）、`Equipment`（1376 行）、`MinionService`（1317 行）、`BrokerService`（1255 行）：按"依赖聚类/变化原因聚类"同一标准逐个审计。
4. `LegionRestrictions`（809 行）：它本身是已拆出的伴生类，如继续膨胀可再分"申请流"与"权限校验"两域。
5. 全量 `mvn test`（需单独授权）作为最终回归。
