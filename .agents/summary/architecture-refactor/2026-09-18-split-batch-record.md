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

## 明确保留（审计结论与理由）

- **ItemTemplate（733 行）**：JAXB 绑定类型（`@XmlAccessorType(NONE)`），字段即职责（静态物品属性定义），全部字段同一变化原因；`@Getter/@Setter` 已收尾。拆分只会引入间接层并威胁静态数据反序列化。红线：JAXB 实例字段严禁 final。
- **Battleground（1930 行）**：天梯/积分域已拆出 `BattlegroundLadder`；剩余主体是 4 个子类（TwoTeamBg/TwoTeamSmallBg/DeathmatchBg/SoloSurvivorBg）覆写的生命周期钩子（onDie/onLeave/onEndDefault/onSpectatorJoin/reconnectPlayer）与玩家准备/复位工具（freeze/heal/cdReset/teleport/preparePlayer，约 350 行）——后者深度耦合宿主状态（previousLocations/instance/mapId/计时），且被全部子类作为 protected 工具调用；外迁只产生搬运而非职责分离。scheduleAnnouncement 还存在对具体子类的 instanceof 判断，属模板方法族。
- **LegionService（1723 行）**：申请流/搜索/仓库历史/权限已全部一行门面委托 `LegionRestrictions`；成员缓存走 `LegionMemberContainer`/legionMembers()。剩余成员管理/解散重建/徽章/公告历史与宿主缓存容器、在线成员广播、扣费深度交织，拆出需携带完整宿主引用，净收益为搬运。handleCharNameRequest/handleLegionRequest 是客户端协议分发器（switch exOpcode），属入口而非独立域。
- **碎片类保留**：`ABEntry/CPEntry/WardrobeEntry/EventWindowEntry/StigmaEntry/SkillSkinEntry`（DAO 持久化 + 网络包序列化契约边界）；`DisconnectionTask/GeneralUpdateTask/ItemUpdateTask/EventScheduleWrapper`（调度任务类型，独立生命周期）；AI 包树全部类（SDJ-001 动态反射红线）；boot/lifecycle 与 Logback filter（Spring/日志框架反射管理）。
- **Lombok 收尾边界**：手写访问器密度最高的类（GlobalRule 32 个、Influence 21 个、Player 74 个）均为语义化门面——访问器名与字段名脱钩（如 `getGlobalRuleItems()→gdItems`、`getAbyssElyosInfluence()→abyss_e`），类级 `@Getter/@Setter` 会生成重复访问器污染 API 面。按"不为了 Lombok 而 Lombok"保留。仓库单实现接口数量为 0，无 SPI 碎片可删。

## 未执行的全量验证

- 全量 `mvn test`：未授权，未执行。已知并行任务欠账：untracked `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestRetailStartMetadataGateTest.java:281` 使用 `Map.of` 传 28 个参数（上限 10 对），阻塞任何 testCompile（本批次验证时临时改后缀挪开、跑完即恢复原状）。
- 启动服务器/真实客户端验收：未执行（用户管理生命周期）。
- JFR 性能采样：本轮拆分为静态策略/注册表迁移，无热路径新增分配；未做采样对照。

## 下一批候选（优先级）

1. `Player` 继续审计复活域（约 120 行，getSelfRezStone/haveSelfRezEffect/hasResurrectBase）——中收益，需保宿主引用。
2. `LegionService` 徽章域（约 250 行）——需携带宿主引用回调 addHistory/restrictions，中风险。
3. `EnchantService`（1540 行）、`TeleportService2`（1639 行）、`QuestService`（1560 行）未审计，按同一"依赖聚类/变化原因聚类"标准评估。
4. 等待并行任务修复 `QuestRetailStartMetadataGateTest` 的 `Map.of` 编译错误后，补跑全量 `mvn test`。
