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
| `dbb2725d7` | Ladder 身份外观域 | `LadderService`（1474→1235 行）战场身份/外观展示域（getBgCloak 披风查表、getName 名称脱敏、getNameByIndex 队伍名、getCapeEmblemByIndex 徽章查表，约 280 行）→ package-private `BattlegroundIdentity` 静态类；门面签名不变。测试 `Ladder*` 通过。 |
| `152b99522` | Attack 控制效果域 | `AttackUtil`（1459→1156 行）暴击控制效果域（isSkillEffect 控制系技能查表约 260 行 + applyEffectOnCritical 武器暴击踉跄/摔倒规则）→ package-private `AttackControlEffects`；门面保留（当前全库零外部调用方，API 兼容优先）。测试 `AttackUtilTest` 通过。 |
| `69ac7b2ba` | 审计汇总扩充 | rounds 2–4 + Spring Bean 边界扫描结论。 |
| `9832bddd9` | Legion 申请流域 | `LegionRestrictions`（810→703 行）入团申请流域（搜索、招募设置、申请提交/取消/批复、仓库历史，11 方法约 250 行）→ package-private `LegionJoinRequests`（持 service + permissions 双引用）；原方法一行委托，`LegionService` 调用链不变。测试 `LegionServiceTest` 等三件套通过。 |
| `a399dff9d` | 击杀奖励公式域 | `StatFunctions`（1224→1095 行）奖励公式（solo/group XP/DP、PVP AP/GP/XP/DP 共 11 方法约 265 行）→ package-private `KillRewardFormulas`：奖励数值调整与战斗公式变化原因不同；跨域调用（ApNpcRating/calculateRatingMultipler）显式宿主前缀。测试 `StatFunctionsTest` 通过。 |
| `e83b9287e` | 审计汇总 round 5 | BattlegroundIdentity/AttackControlEffects + Equipment 保留审计。 |
| `9832bddd9` 前基线 | 全量测试基线对照 | 3401 例 70F+21E；对 b2361c2cf 基线 diff 本轮拆分域零新增失败（详见下文全量测试回归结论）。 |
| `0727e56f5` | Skill 冷却查表域 | `Skill`（1951→1305 行）烙印强化冷却查表（StigmaEnchantCoolDown，技能 ID 巨型 switch 约 650 行）→ package-private `SkillCooldownTables`；public 门面保留（当前零外部调用方），内部调用点直走查表类。`SkillEngineTest` 通过。 |
| `b2b0a8d30` | Player 储物注册表 | `Player`（2542→2413 行）CUBE/宠物背包/房屋仓库/普通/账号/军团仓库注册与查询、脏物品收集、已存储标记、全物品收集 → package-private 静态 `PlayerStorageRegistry`；字段和 Lombok 访问器保留，公开门面签名不变，零分配。`PlayerStorageRegistryTest` 固化 CUBE/普通/账号/未知类型路由；`PlayerQuestCurrencyPortTest` 覆盖宠物背包/房屋仓库与持久化链路。 |
| `a2b5717d6` | Player 房屋域 | `Player`（2413→2393 行）房屋列表惰性加载、活动房屋、houseOwnerId、建筑 owner 状态位 → package-private 静态 `PlayerHouses`；新增同包 `getHousesOrNull()` 仅供房屋域读取原始缓存，公开门面签名不变。`PlayerHousesTest` 固化状态位增删、空缓存活动房屋与 reset 语义。 |
| `b7e2a284b` | Player 展示标签域 | `Player`（2393→2355 行）账号会员标签、特殊账号名标签、管理员访问等级标签与聊天命令前缀 → package-private 静态 `PlayerTags`；公开 `getCustomTag(boolean)` 签名不变。`PlayerTagsTest` 固化默认占位、管理员标签与会员标签语义。 |

## 追加审计结论（round 8，Player 储物域）

- **Player 储物注册表已拆出**：`setStorage/getStorage/getDirtyItemsToUpdate/markDirtyItemContainersStored/getAllItems` 现由无状态 `PlayerStorageRegistry` 承担；存储字段、Lombok 访问器与公开签名保持不变。
- **Player 房屋域已拆出**：`getHouses/resetHouses/getActiveHouse/getHouseOwnerId/isBuildingInState/setBuildingOwnerState/unsetBuildingOwnerState` 现由 `PlayerHouses` 承担；`getHousesOrNull()` 为同包只读原始缓存入口。
- **Player 展示标签域已拆出**：`getCustomTag/getAccessTag/getAcountTag` 现由 `PlayerTags` 承担；公开门面签名不变。
- **Player 仍约 2355 行**：PvP/关系判定域（canPvP/zone/siege relations）暂缺聚焦测试面，暂不盲拆；`Battleground`、`ItemTemplate`、`LegionService` 的既有保留结论不变。

## 追加审计结论（round 5）

- **Equipment（1377 行）**：装备容器 + 校验规则深度耦合私有 `equipment` TreeMap 与 `owner` 字段（validateEquippedWeapon/validateEquippedArmor 直读私有容器）；同包拆出需扩字段可见性，风险大于收益。soulBindItem（75 行）独立过小。**保留**。

## 追加审计结论（round 7，backlog 清空）

- **EffectController（1367 行）**：每生物一个的控制器，是 AR-010 性能线成果载体（EMPTY_EFFECTS 占位符 + 三表首次写入惰性分配，实测省 38 万包装对象/12.2 MB）；protected volatile 字段是子类 `PlayerEffectController` 契约，ReentrantLock 锁语义精细。任何实例域拆分 = 每实体新增辅助对象分配，直接触碰性能红线。**保留，勿拆**。
- **MinionService（1318 行）/ BrokerService（1256 行）**：编排密集（DAO + 包发送 + 模型联动）；BrokerService 价格统计域深耦合宿主 `raceBrokerItems` 容器与排序器（唯一纯函数 getAvgMaxMinPrice 仅 25 行）。拆出均需持 service 引用，净收益为搬运。**保留**。
- **PlayerController（1453 行）**：生命周期事件控制器（onDie/onLogin/对话/移动回调），多态回调密集，与 `CreatureController` 同构。**保留**。
- 至此 >1200 行的非 quest、非 AI、非 geo、非脚本大类已全部审计完毕：要么完成域拆分（9 个宿主类），要么给出明确保留理由。

## 追加审计结论（round 6）

- **LegionRestrictions（拆分后 703 行）**：剩余为纯权限校验（can* 判定 + is/isValid 辅助），职责已单一，**保留**。
- **StatFunctions（拆分后 1095 行）**：剩余为战斗命中/伤害/防御公式（单一变化原因：战斗平衡），**保留**。
- **全量测试事故记录**：首次全量 `mvn test`（后台）运行期间并发执行了聚焦 `mvn compile`，两个 Maven 进程写同一 `target/` 导致产物混乱（NoClassDefFound 等 297 Errors 无效）；已整改——重跑期间不执行任何并发 Maven 操作，以重跑结果为准。教训：后台全量测试与任何 mvn 命令严格串行。

## 全量测试回归结论（已授权执行，基线对照）

- **当前 HEAD 全量**：3401 例，70 Failures + 21 Errors（80 失败用例 / 41 类），其中 39 类在 questEngine 包（并行 quest 任务 P1–P5 契约对齐活跃域）。
- **基线对照（临时 worktree @ b2361c2cf，用后已 remove --force + prune）**：42 个失败类。用例级 diff：HEAD 新增 2 类失败（SilenteraSpawnedDialogFamilyTest 断言 NPC ID 143620≠71810、QuestDispatchToVerteronFamilyProductionFlowTest 断言 quest 11237≠14046），均为 **quest 静态数据断言**；基线失败消失 3 类（并行任务期间已修复）。
- **归因**：时间窗内并行 quest 任务持续提交 XML 修复/回滚（如 `928e01067`、`32b59d6d6`），这两个数据断言失败随其 XML 演进波动；本轮 8 个拆分域（Item/Player/Legion/Enchant/Teleport/Ladder/Attack/Stats）**零新增失败**，全部聚焦测试通过。
-quest 域失败为并行任务中间状态欠账，应随其 P5 收敛，不在本 goal 修复范围。

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
2. `LegionRestrictions`（703 行）与 `StatFunctions`（1095 行）拆分后已职责单一，无需进一步拆分。
3. quest 域 80 例测试失败随并行任务 P5 收敛后，复跑全量 `mvn test` 确认（本轮基线对照已证实与本轮拆分无关）。
