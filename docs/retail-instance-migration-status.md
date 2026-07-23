# 真端副本机制迁移状态

## Nochsana Training Camp

- 地图：`300030000`
- 状态：`RETAIL_AI_QUEST`；真端静态世界、AI、技能、掉落、出口和任务拥有完整流程，无专用 Handler。
- 神器：公共交互消费者按 `NPC_AI_ProtectBuff` 读取唯一完整 NPC 技能槽，当前 `700437` 由 compact 数据解析为 `276/16`；缺失、多槽或无效槽明确拒绝接管。
- 验证：`AI2ActionsScriptNpcRoutingTest` 锁定消费者回退边界；`InstanceHandlerRecoveryMigrationTest#nochsanaUsesRetailWorldFlowWithoutPrivateDrops` 锁定 124 点、AI/技能、掉落、出口、无条件世界和无 Handler 所有权。

## Asteria / Roah Upper Storerooms

- 地图：`300050000`、`300070000`
- 状态：`HYBRID`；Retail 静态选择池、条件变量、Pattern、掉落、精确计时区域和钥匙归属负责已证明的数据链，Handler 只保留持久化 15 分钟 deadline 与当前无可靠消费者的直接清箱桥接。
- 钥匙边界：`185000033..185000038` 均有对应 `ownership_world`，真端均为 `remove_when_logout=FALSE`；断线保留，正常离图由通用 `InstanceService.onLeaveInstance` 清理，Handler 不再重复处理。
- 验证：`AbyssStoreroomRetailMigrationTest` 锁定真端选择池、条件/计时链、钥匙归属、通用离图消费者和清箱拒绝边界。

## Harmony

- 地图：`300450000`、`300570000`、`301100000`
- 所有权：真端 world spawn、NPC AI Pattern、waypoint 与条件变量驱动局内阶段；服务端仅保留匹配、计分和结算边界。
- 生成器提交：`f60ea05`、`8922606`
- 服务端提交：`9f18b89 refactor(instance): restore retail Harmony mechanics`
- 验证：三张地图的条件出生、路径、Pattern 支持判定和 PvP Arena 迁移测试已随提交覆盖。

## PvP Arena

- 地图：`300350000`、`300360000`、`300420000`、`300430000`、`300550000`
- 状态：`HYBRID`；共享 Handler 负责回合、个人积分、结算和恢复，Retail Pattern 只接管已证明可达且具有非零真端分值的对象。
- 计分所有权：原有 `207102`、`243675/243676`、`701173/701174/701187/701188` 加上 Glory 池内 `219502..219504/219540..219542/219653/219654/701216/701221/701226/701852`，均通过稳定出生键/对象 ID 幂等消费并持久化；仅实际装配 `RetailPatternAI2` 的对象退出旧死亡/交互路径，其余击杀、`701169..701172` 交互、`701212` 飞行环和采集继续保留旧所有者。
- 出生边界：Chaos 两图各有 5 个条件变量、10 条条件出生和 `405000/405001` 采集物；Discipline 两图没有采集出生；Glory 有 `S4_BOX/S6_BOX` 和 21 条条件，其中 17 条无条件池覆盖五个 spawn page、三点 Buff 与二选一双成员 Party，选择/死亡/重生均由现有条件出生状态持久化。未导入的非池基础演员继续由旧静态文件负责，池内 `243675/243676` 及 Tog Pattern 生成的 `218757` 静态重复已删除。
- 验证：`RetailPatternAI2Test` 锁定五图具体 NPC 的运行时接管；`PvPArenaMigrationTest` 锁定条件/静态/采集出生、Glory 页面权重/Party、计分幂等、遗物重生、飞行环持久化和 Discipline 无采集所有者；`RetailGiveScoreClosureTest` 锁定 13 个 world、50 个唯一 NPC、111 条可达计分绑定。

## Shugo Imperial Tomb

- 地图：`300560000`
- 状态：`HYBRID`；真端条件出生、waypoint 与 Pattern 负责波次和阶段推进，Handler 只保留离图清理与退出消息。
- 所有权：3 个变量、453 条条件、453 个槽和 509 个 NPC 条目覆盖三阶段波次；`831110/831111/831112` 因真端没有对应 Pattern，由 StageStarter AI 分别写入 `Condition_S2/S3/S4`。
- 变身：`831095` 的 `IDDF2Flying_event01_inviNPC04` Pattern 按玩家种族使用技能槽 `21094/21103`；删除旧 Handler 无来源的固定 `21096`，离图同时清理两种真端效果和旧效果。
- 物品边界：`182006989..182006991` 真端断线保留且归属本图，由通用流程在正常离图时清理；`182006999` Shugo Coin 无世界归属，是持久货币，Handler 不再在登出或离图时删除四项。
- 验证：`ShugoImperialTombMigrationTest` 锁定波次变量、代表性生产链、StageStarter 边界与 Handler 无重复交互；`RetailPatternAI2Test` 锁定 `831095` 的技能槽和具体 NPC 运行时接管。

## Empyrean Crucible / Crucible Challenge

- 地图：`300300000`、`300320000`
- 状态：`HYBRID`；Retail 条件出生、Pattern 与 Recordkeeper 桥接负责十阶段/五阶段遭遇，共享 Handler 负责幂等计分，两图 Handler 保留各自协议、复活和结算边界。
- 物品边界：`186000124/186000125` 归属团队图，`186000134` 归属单人图，真端均为 `remove_when_logout=FALSE`；普通离图由通用流程清理，Handler 仅在不经过通用流程的显式退出路径清理本图物品，断线保留。
- 验证：`CrucibleMigrationTest` 锁定本图物品归属、显式退出清理和登出保留语义。

## Ophidan Bridge

- 地图：`300590000`
- 状态：`HYBRID`；Retail 静态控制器/普通 Boss、条件出生、waypoint 与 Pattern 负责逃犯路线、中间 Boss、最终 Boss 变体、桥门和出口。
- 所有权：11 个变量和 56 条条件覆盖三种逃犯的五段路线、四组中间 Boss、三种最终 Boss 变体、普通 Boss 替换、桥门与出口；Handler 不再重复生成这些对象。
- 保留边界：当前运行时没有完整导入真端无条件静态出生池，四组防御怪随机池继续由 Handler 承担；`802180` 机会包及其四项私有掉落继续保留旧桥接。
- 验证：`OphidanBridgeRetailMigrationTest` 锁定变量、56 条条件、静态普通 Boss、条件门/出口与 Handler 回退边界；`RetailPatternAI2Test` 锁定控制器、18 个逃犯路线 NPC、8 个中间 Boss 和 4 个最终 Boss 的 Pattern 接管。

## Udas Temple

- 地图：`300150000`
- 状态：`HYBRID`；Retail 静态/条件出生、Pattern、掉落和静态门负责副本流程，Handler 只在正常离图时清理钥匙。
- 所有权：3 个变量和 14 条条件覆盖普通/特殊服 Boss、三名钥匙怪死亡计数、后续门控对象和传送器；`215787/215782/215791` 分别 100% 掉落 `185000083/185000084/185000085`，静态门 `97/102/121` 消费对应钥匙。
- 保留边界：三把钥匙没有 `ownership_world`，正常离图仍由 Handler 全量移除；真端 `remove_when_logout=FALSE`，已删除错误的登出清理并保留钥匙用于实例恢复。
- 验证：`InstanceHandlerRecoveryMigrationTest` 锁定条件/静态出生、钥匙掉落、静态门和最小 Handler 边界；`RetailPatternAI2Test` 锁定 Boss、三名计数钥匙怪和门控制器的 Pattern 接管。

## Lower Udas Temple

- 地图：`300160000`
- 状态：`HYBRID`；Retail 静态/条件出生、Pattern 和 npc_drops 负责遭遇与标准战利品，Handler 保留尚无完整真端生产者的 12 箱生命周期。
- 钥匙边界：`185000086/185000087` 没有 `ownership_world`，正常离图继续由 Handler 清理；真端均为 `remove_when_logout=FALSE`，断线恢复保留钥匙。
- 拒绝边界：真端 world 不生产现有 12 个动态宝箱，恢复脚本也未还原完整箱子生命周期，不能因宝箱模板和掉落存在而删除 Handler。
- 验证：`InstanceHandlerRecoveryMigrationTest#lowerUdasUsesCompactDropsWithoutPrivateInjection` 锁定掉落、钥匙生命周期与拒绝边界；恢复基线锁定 deadline/state。

## Abyssal Splinter

- 地图：`300220000`、`300600000`
- 状态：`HYBRID`；Retail 条件/静态出生和 Pattern 负责神器选择与 Dayshade 双 Boss 触发，6/4 个技能槽不足 Boss 继续明确回退。
- 碎片边界：`185000104` 没有 `ownership_world`，两图静态门 `19` 消费该碎片；真端为 `remove_when_logout=FALSE`，共享 Handler 只在正常离图时清理，断线恢复保留。
- 验证：`InstanceHandlerRecoveryMigrationTest#abyssalSplinterUsesRetailArtifactAndDayshadeTriggers` 锁定 Pattern 生产链、双图门、碎片生命周期和最小 Handler；`RetailPatternAI2Test` 锁定具体可接管 NPC。

## Aturam Sky Fortress

- 地图：`300240000`、`300241000`
- 状态：`HYBRID`；Retail 静态/条件出生、waypoint 与 Pattern 负责遭遇和门变量，Handler 只保留三段飞行环、过环效果清理与门 `177`。
- 物品边界：`164000163/164000202` 均为 `remove_when_logout=FALSE` 且归属正式图 `300240000`；正式图正常离开由通用流程清理，活动图因跨图归属由 Handler 仅在正常离开时清理，两图登出都保留物品。
- 验证：`AturamSkyFortressMigrationTest` 锁定正式/活动图不同的正常离图所有者与共同的登出保留语义。

## Esoterrace

- 地图：`300250000`
- 状态：`HYBRID`；Retail 条件出生和已支持 Pattern 负责已证明的 Boss 奖励、钥匙管理进度与门，Handler 保留不支持的 Pattern 回退、影片和旧风道桥接。
- 钥匙边界：`185000111` 没有 `ownership_world`，正常离图继续由 Handler 清理；真端为 `remove_when_logout=FALSE`，登出保留钥匙用于实例恢复。
- 验证：`EsoterraceRetailMigrationTest` 锁定最小 Handler、钥匙生命周期及 coverage 边界。

## Fallen Poeta

- 地图：`301660000`
- 状态：`HYBRID`；Retail 条件出生与 Pattern 负责屏障、波次和 Boss，Handler 仅保留离图/登出效果清理。
- 物品边界：`164002346` 为 `remove_when_logout=FALSE` 且具有 `ownership_world=301660000`，断线保留并由通用离图流程清理；Handler 只清理效果 `21805/21806`。
- 验证：`FallenPoetaRetailMigrationTest` 锁定条件/Pattern、物品归属、通用消费者和最小 Handler。

## Kamar And Idgel Dome Landmark

- 地图：`301120000`、`301680000`
- 状态：`HYBRID`；Retail 出生、Pattern、npc-scores/npc_drops 负责已证明的演员、交互/终局计分与掉落，Handler 保留比赛生命周期。
- 计分边界：Kamar 的 8 个交互对象仅在战斗期幂等计分，旧击杀/PvP 分继续由 Handler 处理；Landmark 的 `833914/833922` 分别固定归属两族 30,000 分，旧 `243965/243966` 普通怪与 PvP 分不重叠。
- Handler 所有权：准备/战斗/退出 deadline、门和补给、玩家注册、复活、积分持久化、封顶/超时结算、离线奖励队列与重启恢复。Landmark 的 `164000413/164000414` 均为 `remove_when_logout=FALSE` 且归属本图，普通离图由通用流程清理，显式退出仍由 Handler 清理，断线保留。
- 验证：`RetailGiveScoreClosureTest`、`RetailPatternAI2Test`、`BattlefieldInstanceMigrationTest`、`IdgelDomeMigrationTest` 和 `AI2EngineRetailSelectionTest`。

## Iron Wall Warfront

- 地图：`301220000`。
- 状态：`HYBRID`；Retail 静态出生、28 个条件变量、486 条条件出生和 Pattern 负责基础演员、巡逻、攻城对象和 Boss 生产。
- Handler 所有权：持久化调整/准备/战斗/退出 deadline、真端 `npc-scores` 的稳定出生键/对象 ID 幂等消费、PvP 分、多人结算、门 `2/17/26/35`、复活、离线奖励队列和重启恢复；没有私有出生桥接。静态弹药箱 `831329` 生产 `182006996/182006997`，三项临时物品均为 `remove_when_logout=FALSE` 但归属 Eternal Bastion，故仅由本图 Handler 在正常离图清理，断线保留。
- 验证：`RetailAiDefinitionLoaderTest` 锁定 486 条条件及关键巡逻/Boss 坐标；`BattlefieldInstanceMigrationTest#ironWallUsesRetailPopulationAndPersistentLifecycle` 锁定静态/条件/计分/结算边界。

## Engulfed Ophidan Bridge

- 地图：`301210000`。
- 状态：`HYBRID`；Retail 静态出生、37 个条件变量、90 条条件出生和 Pattern 负责场景演员与控制流。
- Handler 所有权：持久化准备/战斗/退出阶段、非 Pattern `give_score` 的 NPC 分值、PvP 分、多人结算、`164000277/164000278` 炮弹消耗、火炮效果、`855240` 私有炮击、门 `176/177`、离图清理和重启恢复。
- 验证：`BattlefieldInstanceMigrationTest#engulfedOphidanUsesRetailBattlefieldDataAndPersistentLifecycle` 锁定条件/静态演员、数据表、计分和炮击边界。

## Idgel Dome

- 地图：`301310000`。
- 状态：`HANDLER`；紧凑 `condition-spawns.xml` 没有该世界，不能以可加载 Pattern 代替缺失的阶段生产者。
- 数据与 Handler 边界：静态 `802192/802193` 开关和 `702581..702583` 掉落继续走数据；Handler 唯一负责补给、`234190/234751..234754` 终局、火焰陷阱的真端坐标出生，以及持久化战场、计分、结算、门和恢复。`164000314..164000316` 均为 `remove_when_logout=FALSE` 且归属本图，普通离图由通用流程清理，显式退出仍由 Handler 清理，断线保留。
- 验证：`IdgelDomeMigrationTest#keepsIdgelDomeHandlerSpawnsWithoutConditionProducer` 锁定没有条件生产者、静态开关/掉落和不可删除的 Handler 桥接。

## Lucky Ophidan Bridge

- 地图：`301320000`。
- 状态：`HANDLER`；紧凑条件出生仅导入 4 个变量和 7 个槽位，缺少真端逃亡者和中段 Boss 状态机需要的 `ra_*` 变量/生产者，不能将 Pattern 误判为完整接管。
- 数据与 Handler 边界：Retail 静态出生、已导入条件和 Pattern 保留可证明的演员；删除与 Handler 随机池同坐标的静态 `235768`。Handler 唯一负责 8 组随机演员、逃亡者完成后的 `702658/702659`、`730868`、`802180` 以及机会包私有掉落和 `235786` 清理。
- 验证：`OphidanBridgeRetailMigrationTest#luckyOphidanKeepsHandlerBridgeWithoutRunawayConditionClosure` 锁定条件缺口、无重复静态出生和保留的桥接。

## Danuar Sanctuary

- 地图：`301380000`。
- 状态：`HANDLER`；Retail 静态出生、5 个变量、9 条条件和 Pattern 负责已导入演员及终局遭遇。
- 数据所有权：真端 `235624/235625/235626` 的 `3333/3333/3334` 无条件随机组由现有条件出生引擎按 `(1056.595337, 693.456970, 287.991913)` 选择、持久化并恢复，Boss Pattern 写入 `cSetPortal = 3` 后生成出口。Handler 仅保留种族守卫、未建模钥匙交互/提示和恢复。`185000181..185000183` 唯一归属 `301140000`，本图不再错误清理。
- 验证：`DanuarSanctuaryRetailMigrationTest` 锁定权重、统一坐标、三个 Pattern 的出口变量和 Handler 回退边界。

## Stonespear Reach

- 地图：`301500000`。
- 状态：`HYBRID`；Retail 静态出生、7 个条件变量、Legion Pattern 和 `npc-scores` 负责演员、终局和战斗计分，`boss_on == 10` 生成 `855843`。
- Handler 所有权：不包含动态出生；仅消费条件出生死亡和有稳定键的零均衡 NPC 分数，并负责可持久化的准备/限时 deadline、排名结算、复活、退出和 JVM 恢复。
- 验证：`StonespearReachRetailMigrationTest` 锁定终局生产链、Pattern/分值数据以及 Handler 的生命周期边界。

## Contaminated Underpath

- 地图：`301630000`。
- 状态：`HYBRID`；Retail 条件出生以 `IDLUNA_DEF_PHASE_*` 生成带路径的 Luna 波次，Pattern 负责阶段推进、终局和奖励演员。
- Handler 所有权：不包含动态出生；门 `28` 只启动 `IDLUNA_DEF_PHASE_1_1`，并负责可持久化的准备/限时/结算 deadline、零重复的 `npc-scores` 计分、排名结算、效果 `21345/21346/22741` 清理和重启恢复；`182007405` 为 `remove_when_logout=FALSE` 且归属本图，断线保留并由通用离图流程清理。
- 验证：`ContaminatedUnderpathRetailMigrationTest` 锁定数据化阶段生产链、无 Handler 出生与生命周期边界。

## Event Contaminated Underpath

- 地图：`301631000`。
- 状态：`HYBRID`；Retail 条件出生在 `TIMEATTACK_PLAY_START == 1` 产生 `836060`，Pattern 负责逐波推进并写入 `Wave_4_Start`；该变量按真端 `SpecialServer_Cond` 分支产生普通终局 `248525` 或特殊终局 `248947`，二者均使用 `npcpath_wave_4-1`。
- Handler 所有权：门 `57` 启动计时；仅消费 `score_apply_type == 3` 的死亡计分并以对象 ID 幂等，两个终局均启动持久化结算；Handler 同时负责排名、奖励、效果清理及重启恢复，不生成演员。`186000470` 为持久副本货币，`186000495` 为现金钥匙，两者均为 `remove_when_logout=FALSE` 且无世界归属，离图和断线都不删除。
- 验证：`ContaminatedUnderpathMigrationTest` 锁定终局条件和 Handler 边界；`RetailAiDefinitionLoaderTest` 锁定完整 Retail 定义加载。

## Secret Munitions Factory

- 地图：`301640000`。
- 状态：`HYBRID`；Retail 条件出生有 19 个变量、63 条条件和对应路径，Pattern 负责已导入 Luna 演员与阶段变量。Handler 不产生 NPC。
- Handler 所有权：可恢复的准备/战斗/结算 deadline、`score_apply_type == 3` 的对象 ID 幂等计分、终局 `244147`、Luna 奖励、效果清理和重启恢复。`164002362` 为 `remove_when_logout=FALSE` 且归属本图，断线保留并由通用离图流程清理；`164000418` 归属 Kumuki Cave，永不由本图清理。
- 验证：`SecretMunitionsFactoryMigrationTest` 锁定 Retail 计分、持久化 deadline/结算和无手工出生边界。

## Treasure Island Of Courage

- 地图：`301700000`。
- 状态：`HANDLER`；真端 `IDRun/world_N.xml` 仍有 `idrun_treasure_despawn == 50` 的条件生产者，而紧凑 `condition-spawns.xml` 没有该世界，当前不能以静态出生或可加载 Pattern 宣称阶段生产链已接管。
- Handler 所有权：准备/战斗/退出 deadline、五个阶段传感器、阶段变量、宝箱/英雄物品、临时物品和效果清理、战场结算以及重启恢复；静态出生继续只负责可证明的场景对象。
- 验证：`TreasureIslandOwnershipTest` 锁定缺失条件生产者与不可删除的 Handler 阶段/宝箱边界。

## Opportunity Fissure Of Oblivion

- 地图：`302110000`。
- 状态：`EVENT`；该类没有独立流程，只继承 `302100000` Fissure Of Oblivion 的 Handler 生命周期。Retail 静态出生、Pattern 和 `npc-scores` 继续负责演员、战斗和数据化分数。
- Handler 所有权：继承的门变量、持久化计时赛/幂等计分、完成/奖励、清理和恢复；Event 子类不新增出生、计时或结算逻辑。
- 验证：`InstanceSettlementServiceTest#migratedHandlersCannotRestoreHardcodedFinalRewardsOrRankThresholds` 锁定继承关系与无旧线程实现。

## Crucible Spire

- 地图：`302400000`。
- 状态：`HYBRID`；Retail 条件出生覆盖 55 个变量、798 条条件和 798 个演员，负责种族/楼层条件、40 层演员与对应路径。首次进入只写入种族和楼层变量，不再手工重复产生 `247376/247386` 控制器。
- Handler 所有权：持久化楼层、奖励账本、飞行环/传送、失败后的控制器恢复、真端尚未建模的临时物品及显式退出清理；不把其余 40 层条件演员重新接管为手工出生。真端 `User::SaveInstanceData/LoadInstanceData` 对 `302400000` 有 Infinity 专用保存/加载路径，且 `164000530` 为 `remove_when_logout=FALSE`，因此登出保留位置、实例和物品。
- 验证：`CrucibleSpireMigrationTest` 锁定条件覆盖量、种族首层条件、无初始重复控制器及无登出强制退出。

## Ophidan Warpath

- 地图：`301670000`。
- 状态：`HYBRID`；Retail 静态出生、10 个条件变量和 31 条条件负责阵营演员、火炮与阶段对象。Handler 不生成战场演员。
- Handler 所有权：持久化准备/战斗/退出 deadline、PvP 与三类真端 `give_score` 对象消费、阵营积分阈值、复活、多人结算、离线奖励队列和重启恢复。
- 验证：`BattlefieldInstanceMigrationTest#ophidanWarpathUsesRetailAiAndPersistentLifecycle` 锁定 Retail 数据与生命周期边界。

## Evergale Canyon

- 地图：`302350000`。
- 状态：`HYBRID`；20 个变量、347 条条件、1,183 个槽和 1,197 个 NPC 条目负责战场演员、Boss 与页面差异，未保留私有出生。
- Handler 所有权：由 Retail 战场表驱动的准备/战斗/无人敌方/退出 deadline、人口档位变量、PvP 和终局对象稳定键计分、结算、复活、离线奖励和重启恢复。
- 验证：`BattlefieldInstanceMigrationTest#evergaleUsesRetailLifecyclePopulationAndSettlement` 锁定条件覆盖、数据表、计分和结算边界。

## Tiamat Stronghold

- 地图：`300510000`
- 状态：`HYBRID`；Retail 静态/条件出生、Pattern 和掉落负责阶段、波次、Boss、传送和宝藏。
- 所有权：18 个变量、159 条条件覆盖主要流程；旧 Handler 的动态出生、死亡、掉落和计时逻辑均已删除。
- 保留边界：静态控制器 `701523` 的 compact AI 为 `NoAction`，真端 Pattern 没有该交互消费者；Handler 仅在物品使用完成时开启门 `22`。
- 验证：`InstanceHandlerRecoveryMigrationTest#tiamatStrongholdUsesRetailConditionsAndKeepsOnlyUnsupportedSwitch`。

## Eternal Bastion

- 地图：`300540000`
- 状态：`HYBRID`；Retail 条件出生、91 个 NPC Party、Pattern、npc-scores 和掉落负责波次、演员、战斗和战利品。
- Pattern 所有权：`701625/701922` 均由 `IDF5_TD_AddWave_01` 完整消费 `on_talked_by_user`，依次施放真端技能槽 `21069`、写入 `Wave_Z2_S1=1`、广播消息并自销毁；旧 Handler 的 `21065/21066` 阵营变身桥接已删除。
- Handler 所有权：准备/限时/退出 deadline、基础分与 NPC 分持久化、成功/失败终点、排名、奖励幂等、直接退出的临时物品清理、效果清理和 JVM 恢复；三项临时物品均为 `remove_when_logout=FALSE` 且归属本图，断线保留，普通离图由通用流程清理。
- 结算：`world_timeattack` 提供基础分 `20000`、S 级最低分 `90000`、时限 `1800` 秒；`InstanceSettlementService` 唯一负责排名与奖励计划。
- 验证：`TheEternalBastionMigrationTest`、`RetailAiDefinitionLoaderTest`、`BattlefieldInstanceMigrationTest`、`InstanceSettlementServiceTest`。

## Occupied Rentus Base

- 地图：`300620000`
- 状态：`HYBRID`；Retail 静态/条件出生、waypoint、Pattern 和掉落负责可证明的数据化流程。
- 新接管：`855952` 按玩家种族写入 `weapon=1/2`，21 条条件中的 12 条生成对应攻城武器；攻城武器、`701151/701152` 补给、`282394` 油桶和 `283000/283001` 幻象由 Pattern 负责，Handler 不再重复出生、施法、治疗或死亡清理。
- 保留边界：`236300` 的 `IDYun_Nmd6_Hard` Pattern waypoint 起点为空，且引用未声明变量 `Ariana4`，继续拒绝运行时接管；Handler 保留初始门、三组 Boss 门、最终影片/士气/出口/Reian 奖励桥接，以及 `701097` NoAction 石墙和缺 Pattern 的 `701100` 香炉交互。
- 验证：`RetailPatternAI2Test` 锁定 19 个具体 NPC 的运行时接管和 `236300` 的明确拒绝；`InstanceHandlerRecoveryMigrationTest` 锁定 5 变量、21 条条件、攻城武器生产者、Handler 边界与掉落。

## Danuar Reliquary Family

- 地图：`301110000`、`301330000`、`301360000`。
- 状态：`HYBRID`；Retail 唯一静态出生和完整 clone/add Pattern 负责可证明的数据化演员。
- 出生边界：三图的 `284377/284378/284379` 各仅保留真端唯一坐标，删除旧手写重复点；每图均保留真端 8 个条件变量和 12 条条件。
- Handler 所有权：初始三 NPC、两阶段 Modor、持久化 15 分钟 deadline、完成、私有掉落、退出和重启恢复。实例级 Pattern 门防止可加载但不等价的阶段 Pattern 覆盖 custom AI，同时允许完整的 clone/add Pattern 接管。
- 拒绝边界：`284379` 缺技能槽；终局/困难链仍缺具名路径、区域或条件变量，不能接管完整流程。
- 验证：`RetailPatternAI2Test` 锁定三图具体 Pattern 支持矩阵；`AI2EngineRetailSelectionTest` 锁定 Handler-owned Boss 回退与 clone 接管；`InstanceHandlerRecoveryMigrationTest` 锁定唯一出生、条件数量、持久化状态和旧重复死亡清理已移除。

## Illuminary Obelisk Family

- 地图：`301230000`、`301370000`。
- 状态：`HYBRID`；Retail 条件出生、Pattern 和 npc_drops 负责波次、计时器、Boss、资源与活动箱。
- Handler 所有权：两件物品归属正式图且真端为 `remove_when_logout=FALSE`；正式图普通离开由通用流程清理，Infernal 图由 Handler 清理，两图主动退出仍由 Handler 清理并传送，登出均保留物品；无死亡、动态出生、掉落、门或阶段桥接。
- 数据边界：两图均保留 `h_wave_01_01..h_wave_04_01` 的关键波次变量；`IDF5_U3_StartNPC/GameTimer/BossTimer/DEF_CTRL/TimeOver` 及困难变体由 Pattern 消费，资源 `730884/730885` 和活动箱 `702658/702659` 由掉落数据负责。
- 验证：`IlluminaryObeliskMigrationTest` 锁定最小 Handler、波次变量、关键 Pattern 和掉落映射；`RetailInstanceOwnershipTest` 锁定 Handler 登记一致性。

## IDSweep Family

- 地图：`301400000`、`301590000`。
- 状态：`HYBRID`；Retail 条件/静态出生、Pattern 和 npc-scores 负责演员、波次、奖励箱条件与计分值。
- 数据边界：普通图 221 条、Safe 410 条条件；Safe 已移除与条件出生重叠的 `235xxx` 静态战斗 NPC，`SpecialServer_Cond` 的 Live/Master 分支均保留。
- Handler 所有权：共享 `ShugoVaultTimeAttackInstance` 负责持久化准备/战斗/结算 deadline、门 `430` 提前启动、幂等击杀计分、终 Boss 完成、排名与奖励变量、玩家奖励账本、变身清理和重启恢复；两图各七项临时物品均为 `remove_when_logout=FALSE` 且归属本图，断线保留并由通用离图流程清理；两个地图类仅绑定 `@InstanceID`。
- 验证：`InstanceHandlerRecoveryMigrationTest#idsweepUsesRetailConditionClosureAndRemovesSafeStaticCombatSpawns` 锁定条件闭包和静态去重；`InstanceSettlementServiceTest#migratedHandlersCannotRestoreHardcodedFinalRewardsOrRankThresholds` 锁定共享真端结算；`RetailInstanceOwnershipTest` 锁定 Handler 登记一致性。

## Theobomos Test Chamber

- 地图：`301610000`。
- 状态：`HYBRID`；Retail 静态/条件出生、Pattern 和 npc_drops 负责 Boss 流程、阶段变量、出口与战利品。
- 数据边界：6 条条件消费 `boss_summon/boss_summon_check/End_Boss_Die`；`220424/220425/220426` Pattern 完整，`End_Boss_Die == 1` 生成出口 `806206`，最终 Boss 与奖励箱掉落均由 npc_drops 负责。
- Handler 所有权：仅在 `220426` 死亡后幂等生成奖励箱 `806221`；生成状态持久化，并在 JVM 重启恢复时重建奖励箱。
- 验证：`InstanceHandlerRecoveryMigrationTest#theobomosTestChamberUsesRetailBossFlowAndDrops` 锁定最小桥接、条件/静态出生与掉落；`RetailAiDefinitionLoaderTest#loadsCompleteRetailDefinitions` 锁定变量和具体 Pattern 支持。

## Aetherogenetics Lab

- 地图：`310050000`。
- 状态：`HYBRID`；Retail 静态出生、Pattern、npc_drops 和静态门负责遭遇、钥匙生产与门消费。
- 数据边界：`212341/212175/212196/212193/212342` 分别 100% 掉落 `185000001..185000005`；静态门 `168/167/172/192/171` 分别消费对应钥匙。
- Handler 所有权：五把钥匙没有 `ownership_world`，仅在正常离图时清理；真端均为 `remove_when_logout=FALSE`，登出保留钥匙用于实例恢复。无死亡、掉落、阶段、门或奖励桥接。
- 验证：`InstanceHandlerRecoveryMigrationTest#aetherogeneticsLabUsesRetailKeyDropsAndKeepsOnlyKeyCleanup` 锁定最小 Handler、钥匙掉落来源和五扇静态门消费。

## Bastion of Souls

- 地图：`302340000`。
- 状态：`HYBRID`；Retail 静态/条件出生、Pattern 和 npc_drops 负责遭遇、最终 Boss、钥匙、宝箱与出口。
- 数据边界：条件变量覆盖 Boss/装备/阶段/UI、`statdown` 和终局；随机钥匙怪、三档最终 Boss/宝箱、双出口均由条件出生负责，钥匙与宝箱内容由 npc_drops 负责。
- Handler 所有权：创建 `BASTION_OF_SOULS` 飞行环；通过后写 `statdown=1`、传送并播放影片 `957`；离图/登出只清理效果 `17649/17672`。六项临时物品均为 `remove_when_logout=FALSE` 且具有 `ownership_world=302340000`，断线保留并由通用离图流程清理。
- 验证：`BastionOfSoulsMigrationTest` 锁定 Retail 掉落、条件链、静态去重和最小 Handler 边界；`RetailInstanceOwnershipTest` 锁定 Handler 登记一致性。

## Kumuki Cave

- 地图：`302330000`。
- 状态：`HYBRID`；Retail 条件/静态出生、waypoint 和 Pattern 负责出生页、波次、计时器、影片与 Boss 链。
- 数据边界：10 个变量、114 条条件、366 个条件槽覆盖两页流程；40 条完整路径支撑所有条件出生，且与遗留静态出生无重叠。
- Handler 所有权：仅处理 `703424` 消费钥匙 `185000295` 的交互及失败提示；`185000295/185000296/164002390` 均由 `ownership_world=302330000` 通用离图流程清理，`186000459` 无世界归属，仅在正常离图由 Handler 清理；四项物品真端均为 `remove_when_logout=FALSE`，登出只清理四个效果。
- 验证：`KumukiCaveInstanceTest` 锁定 Handler 最小边界、条件页/演员、路径闭包和关键 Pattern；`RetailInstanceOwnershipTest` 锁定 Handler 登记一致性。

## Theobomos Lab

- 地图：`310110000`。
- 状态：`HYBRID`；Retail 静态出生、Pattern、技能区域和 npc_drops 负责基础演员、战斗与标准战利品。
- 拒绝边界：当前 `condition-spawns.xml` 没有 `310110000` 世界，不能用 Pattern 可加载替代阶段生产者；随机宝箱、封印石和 Watcher 顺序链继续由 Handler 负责。
- Handler 所有权：`700422/237247` 私有掉落，随机宝箱，180 秒封印石、两名守卫计数、Watcher 消息/阶段、Triroan 到延迟 Ifrit、完成出口，以及所有 deadline/计数/完成状态的重启恢复。
- 验证：`InstanceHandlerRecoveryMigrationTest#theobomosLabUsesCompactDropsWhileKeepingUnmodeledQuestFlows` 锁定标准掉落迁移与保留桥接；恢复基线测试锁定 deadline/state；`RetailAiDefinitionLoaderTest` 锁定技能区域加载。

## Draupnir Cave

- 地图：`320080000`。
- 状态：`HYBRID`；Retail 条件/静态出生和 Pattern 负责模式选择、副官、Boss、中央控制室波次与种族入口。
- 数据边界：8 个变量、18 条条件和 24 个槽覆盖普通/特殊页副官、`213780/236929/237263/237275` Boss 链及三个 `702857` 控制点；`702858` 生成种族检查器 `702861`，再按玩家种族生成 `805736/805737`。
- Handler 所有权：仅发送入场提示，并以持久化 deadline 幂等生成缺少 Retail 数据生产者的 `237276`；无死亡、Boss 出生、物品交互或手写袭击链。
- 验证：`DraupnirCaveRetailMigrationTest` 锁定条件闭包、种族入口生产者和最小 Handler；`RetailPatternAI2Test` 锁定 14 个具体 NPC 的运行时接管。

## Adma Stronghold

- 地图：`320130000`。
- 状态：`HYBRID`；Retail 条件/静态出生、Pattern 和 npc_drops 负责阶段、Boss、出口与标准战利品。
- Handler 所有权：仅清理 `700396/700397` 交互效果，并在正常离图时清理无 `ownership_world` 的钥匙 `185000026..185000032`；真端钥匙均为 `remove_when_logout=FALSE`，断线恢复不清理。
- 验证：`AdmaStrongholdRetailMigrationTest` 锁定条件/静态出生、七把钥匙、最小 Handler 和 coverage 边界。

## Sealed Argent Manor

- 地图：`301510000`。
- 状态：`HYBRID`；Retail 条件/静态出生、Pattern、npc-scores 和 npc_drops 负责演员、变量门、变身效果、基础分值与战利品。
- 数据边界：3 个变量、28 条条件和 28 个槽覆盖 `key_monster` 六分支及两段传送门；`731648` 仅在 `teleport_01 == 3` 时生成，旧静态重复点已删除。`701001..701004` 由同一 Pattern 按各自技能槽施放 `19316..19319`，Handler 不再重复施法。
- Handler 所有权：持久化准备/战斗/结算 deadline、职业选择 `237193/237194`、铁牢与墙、`856547` 消耗 `185000242` 激活 Hetgolem、幂等击杀计分、排名、奖励账本、效果清理和 JVM 恢复。
- 验证：`InstanceHandlerRecoveryMigrationTest#sealedArgentManorUsesRetailDataAndKeepsTimeAttackHandler` 锁定条件闭包、静态去重、Pattern/掉落和 Handler 边界；`RetailPatternAI2Test` 锁定四个变身物件的具体运行时接管。

## Linkgate Foundry

- 地图：`301270000`。
- 状态：`HYBRID`；Retail 静态出生、完整 Pattern 和 npc_drops 负责房间控制、普通战斗与战利品。
- 拒绝边界：`condition-spawns.xml` 没有该世界；`233898/234990/234991` 三种 Boss Pattern 均缺 `Boss_Die` 条件变量，继续回退。静态出生包含房门/房间/Boss 控制器，但不包含 `233887..233897` 限时怪群或出口 `702338`，不能删除 Handler 的动态人口与完成链。
- 钥匙边界：`185000196` 具有 `ownership_world=301270000`，真端为 `remove_when_logout=FALSE`；通用离图流程清理，Handler 不再重复处理登出或离图。
- Handler 所有权：持久化 20 分钟 deadline、五次预警、限时怪群出生/到期清理、三种 Boss 完成后幂等出口和 JVM 恢复。
- 验证：`InstanceHandlerRecoveryMigrationTest#linkgateFoundryKeepsOnlyUnmodeledTimedPopulationAndCompletion` 锁定无条件世界、静态/Handler 演员边界、Pattern、掉落、钥匙归属和通用离图消费者；`RetailPatternAI2Test` 锁定普通怪/控制器接管及三种 Boss 的 `Boss_Die` 明确拒绝。

## Smoldering Fire Temple

- 地图：`302000000`。
- 状态：`HYBRID`；Retail 条件/静态出生、Pattern、npc-scores 和 npc_drops 负责普通/大师阶段、演员、战斗、分值和标准战利品。
- 数据边界：11 个变量、672 条条件和 672 个槽覆盖三段波次、Boss 房、普通/大师 Boss 与 `IDDF2_Dflame_Event_Reward` 的奖励前后页；旧 Handler 动态 Boss/守卫出生和静态条件演员均已去重。
- Handler 所有权：条件引擎初始化和完成时奖励变量、持久化准备/战斗/结算 deadline、门 `2/8`、幂等击杀计分、三种种族变身、`244435/834058/834212` 私有掉落、排名奖励、效果清理和 JVM 恢复。七件副本道具均为 `remove_when_logout=FALSE` 且具有 `ownership_world=302000000`，断线保留并由通用离图流程清理。
- 验证：`SmolderingFireTempleRetailMigrationTest` 锁定 672 条条件闭包、静态去重和 Handler 最小边界；`InstanceSettlementServiceTest` 锁定统一真端计时结算。

## Fissure of Oblivion

- 地图：`302100000`。
- 状态：`HYBRID`；Retail 静态出生、完整 Pattern 和 npc-scores 负责基础演员、门后演员与分值，Handler 负责计时赛生命周期。
- 数据边界：2 个变量、4 条条件和 4 个槽；`door_open == 1` 生成 `245827`，旧静态重复点已删除。三个 `worldraid_on == 1` 槽依赖的开启 Pattern 代表 `245422` 缺 `shadow_kill`，继续不可达并明确拒绝；`245415` 关闭控制器与 `245827` Pattern 可接管。
- Handler 所有权：门 `34` 与 `door_open` 桥接、持久化准备/战斗/结算 deadline、幂等 NPC 计分、`245411` 完成、统一排名/奖励账本、入场效果 `4831` 清理和 JVM 恢复。
- 验证：`InstanceHandlerRecoveryMigrationTest#fissureOfOblivionUsesRetailConditionActorsAndKeepsTimeAttackLedger` 锁定条件、静态去重和 Handler 边界；`RetailPatternAI2Test` 锁定 `245415/245827` 接管与 `245422` 的 `shadow_kill` 明确拒绝；`InstanceSettlementServiceTest` 锁定统一结算。

## Mirash Sanctuary

- 地图：`301720000`。
- 状态：`HYBRID`；Retail 条件/静态出生、Pattern 和 npc_drops 负责娃娃布局、石碑、Boss 冰雹、奔跑事件、标准战斗与战利品。
- 数据边界：12 个变量、138 条条件和 138 个槽；Pattern 明确写入但条件表原先未登记的 `boss_die/doll_time/resurrect_set/resurrect_statue` 已补齐，`248013/248427/835784` 等关键 Pattern 恢复可达。`248389` 恢复真端 `NPCPathZone_A_40`；`248533` 真端出生没有 Pattern 所需 waypoint，继续明确拒绝。删除 `248423/248424/248426/248472/835732/835784/835785` 七组无条件静态重复演员。
- Handler 所有权：随机二选一 `248533` 钥匙怪、`248389` 死亡后的持久化五秒突袭波次、`248444..248447` 石像复活链、`248382 -> 11333` 技能授予、`248013 -> 835733` 宝箱与技能清理、`835784..835789` 私有随机石头掉落。`164000531` 真端断线保留且归属本图，由通用流程在正常离图时清理。
- 验证：`InstanceHandlerRecoveryMigrationTest#mirashSanctuaryUsesCompactDropsWithoutPrivateBossRewards` 锁定条件闭包、静态去重、A40 路径、Handler 保留边界和掉落；`RetailPatternAI2Test` 锁定 7 个关键 Pattern 接管与 `248533` 的 waypoint 拒绝。

## Sauro Supply Base

- 地图：`301130000`。
- 状态：`HYBRID`；Retail 条件/静态出生和 Pattern 负责门区变体、Boss、传送器、战斗与真端系统消息。
- 数据边界：10 个变量、28 条条件、28 个槽和 30 个 NPC；`GATECONTROL_B4 <= 1` 生产 `230857`，旧静态重复点已删除；`230853` Pattern 写入 `OBJ_GATE_SELECTION` 后由条件生成 `730872`，Handler 不再直生。`BRIDGE_BOSS` 的生产者没有本地出生，`233255` 继续保留静态旧所有者。
- Handler 所有权：真端脚本门 ID `102/203/209/210/301/302/306/307` 与本地实体门 ID 不同，继续桥接并持久化 `59/372/375/376/378/382/383/387/388`；同时负责可恢复的五段警报、随机 `230846`、`230851` 侧门、`230857/230858` 完成出口与 `802181` 私有掉落。四把钥匙均为 `remove_when_logout=FALSE` 且具有 `ownership_world=301130000`，断线保留并由通用离图流程清理。静态数据已有两只 `230797`，删除 Handler 的重复伏兵。
- 验证：`InstanceHandlerRecoveryMigrationTest#sauroSupplyBaseUsesRetailActorsAndPersistentHandlerFallbacks` 锁定条件、静态去重、持久化 Handler 与保留边界；`RetailPatternAI2Test` 锁定 11 个关键 Pattern 接管。

## Seized Danuar Sanctuary

- 地图：`301140000`。
- 状态：`HYBRID`；Retail 静态/条件出生、Pattern 和 `npc_drops` 负责石棺、上升气流岩石、终局 Boss 战斗、出口和普通掉落。
- 数据边界：真端终局出生组以 `3333/3333/3334` 在 `235619/235620/235621` 中选择一只，本地删除三只静态重复并持久化随机选择；补入 Pattern 内部变量 `Pr_reset01` 后三名 Boss 均可执行，死亡写入 `cSetPortal = 3`，条件生成 `701876`。`233084` Pattern 已生成 `233085`，静态 `233187` Pattern 已写入 `cWindBoxP`，旧 Handler 重复桥接已删除。
- Handler 所有权：持久化七段警报、首位玩家种族和 14 个种族守卫、随机终局 Boss/死亡恢复；保留无 Pattern 的 `701859/701860/701863/701864` 交互与 `235574 -> 186000254` 私有掉落。`185000181..185000183` 均为 `remove_when_logout=FALSE` 且具有 `ownership_world=301140000`，断线保留并由通用离图流程清理。
- 验证：`InstanceHandlerRecoveryMigrationTest#seizedDanuarSanctuaryUsesRetailActorsAndPersistentHandlerFallbacks` 锁定条件、静态去重、持久化 Handler 和保留边界；`RetailPatternAI2Test` 锁定 5 个关键 Pattern 接管。

## Upper Abyss Storerooms

- 地图：`300120000`、`300130000`、`300140000`
- 状态：`RETAIL_AI_QUEST`；零手写玩法机制，无专用 Handler。
- 权威证据：三图 `world_N.xml`、`NpcAIPatterns_IDAbRe_Up3_SSH.xml`、真端出生与 `ownership_world` 钥匙定义。
- 所有权：条件出生、静态出生、门/宝箱/守财者/Boss Pattern 与掉落均由真端数据负责；15 把钥匙的 `ownership_world` 由通用离图流程消费。
- 数据验证：`12/11/11` 条件、`32/38/38` 条件 NPC、`212/219/221` 静态点；三图宝箱模板、守财者组和 Boss 钥匙均已覆盖。
- 删除的错误机制：手写阶段计时、宝箱生成、Boss/门处理、掉落注入及四个钥匙清理 Handler。真端钥匙明确为 `remove_when_logout=FALSE`，登出不再错误删除，正常离图仍按 `ownership_world` 清理。

## Drakenseer Lair

- 地图：`301620000`
- 权威证据：`58Server/Map/Worlds/IDF6_Dragon/world_N.xml`、`58Server/Map/XML/NpcAIPatterns_IDF6_Dragon_SSH.xml`。
- 所有权：`IDF6_Dragno_TimeCheck`、`IDF6_Dragon_Control_01/02`、`IDF6_Dragon_Wave_Start_A/B/C`、`IDF6_Dragon_Named` 负责计时、消息、波次、Boss、失败传送与出口；`npc_drops_part_009.xml` 负责 `220450` 的六组掉落。
- 生成器提交：`6f2b1de fix(converter): restore Drakenseer wave conditions`
- 服务端提交：本次 `refactor(instance): restore retail Drakenseer Lair`
- 数据验证：`12` 个变量、`133` 条条件、`133` 个出生槽、`100` 条 Wave 条件，拒绝条件为 `0`；核心 Pattern 全部可启用。
- 删除的错误机制：手写 FlyRing 计时、三塔死亡计数、强制技能 `21791`、手写 Boss 出口与自定义掉落。

## Beshmundir Temple

- 地图：`300170000`
- 状态：`HYBRID`；真端数据接管全部阶段出生，Handler 仅保留两项没有数据消费者的交互桥接。
- 权威证据：`idcatacombs/world_N.xml`、当前静态/条件出生、`30208/30308` 任务工作物品和 `30231/30331` 钥匙奖励。
- 所有权：祭坛 `730274` 与蓝焰门 `730290` 由真端数据出生；`799506` 不存在于该地图 world/静态/条件出生，故仍由祭坛完成回调生成。任务结算回收 `182209610/182209710`，门交互消耗 `185000091`。
- 钥匙边界：`185000091..185000096` 没有 `ownership_world`，正常离图继续由 Handler 清理；真端均为 `remove_when_logout=FALSE`，登出不再错误删除。
- 验证：`BeshmundirTempleInstanceTest` 锁定两项桥接的无替代依据与旧 Handler 机制已删除。

## Kromede's Trial

- 地图：`300230000`
- 状态：`HYBRID`；真端 world、Pattern、任务和静态出生接管主要流程，Handler 仅保留入口与仍无数据消费者的交互桥接。
- 权威证据：`idcromede/world_N.xml`、`Cromede_Relic1/2` 与 `Cromede_Relic1/2_Noshow` Pattern、`19220/19270` 变身及 `19273/19274` 净化技能槽。
- 所有权：`282093/282095` 遗物死亡后分别生成 `282085/282084`，后者施放 `19274/19273` 并自销毁；旧 Handler 的 `19248/19247` 玩家增益桥接已删除。石门即时删除、入口变身/影片说明以及效果清理仍由 Handler 承接。`164000140..164000143` 具有 `ownership_world=300230000`，由通用离图流程清理；`185000101/185000102/185000109` 没有世界归属，仅在正常离图由 Handler 清理；七项真端均为 `remove_when_logout=FALSE`。入口变身现按每名玩家的实际种族选择，魔族使用 `19270`。
- 验证：`KromedesTrialInstanceTest` 覆盖两族变身映射及既有的 Pattern/任务所有权边界。

## Taloc’s Hollow

- 地图：`300190000`
- 状态：`HYBRID`；真端静态/条件出生、Pattern、门、掉落、任务和 ScriptNpc 接管主流程，Handler 仅保留影片与离开清理桥接。
- 权威证据：`idelim/world_N.xml`、`NpcAIPatterns_IDElim_OSY.xml`、compact NPC AI/条件出生/掉落及两族 `10032/20032` 任务。
- 所有权：五株普通治疗植物、巨型植物条件链和无重生虫卵由数据负责；`Elim_ClodwormNm`/`Elim_NeutflyNm` 通过逻辑门号 `1/2` 控制门 `48/7`，门 `49/7/48` 初始开启，门 `180` 初始关闭可点击。`700940/700941` 由 ScriptNpc 使用真端 `19229/19230` 技能并在成功后删除自身；Handler 播放单人影片。`164000137..164000139` 无 `ownership_world` 且真端均为 `remove_when_logout=FALSE`，仅在正常离图清理，登出/离图继续清理效果与召唤物。
- 验证：`TalocsHollowQuestMigrationTest` 覆盖任务发放/回滚、条件出生、虫卵、Handler 边界及四扇门的状态和逻辑 ID。

## Haramel

- 地图：`300200000`
- 状态：`HYBRID`；真端静态出生、AI Pattern 与掉落已接管，升降机上行终点仍为未恢复的服务器桥接。
- 权威证据：`idnovice/world_N.xml`、`NpcAIPatterns_LDF4_PJW.xml`、compact NPC AI/技能/掉落数据。
- 所有权：116 个静态点保留真端位置；`799522/799523/799524` 为 1 秒刷新，`730320/730321` 为 60 秒刷新。`216922` 解析为 `IDNovice_Hameroon`，由 Pattern 生成宝箱与出口，二者不写入静态出生。
- 保留边界：`TowerLiftAI2` 的 `220/213/126.68472` 上行落点尚未从真端恢复，故不将该交互标记为完整接管。
- 验证：`RetailPatternAI2Test` 与 `InstanceHandlerRecoveryMigrationTest` 锁定首领 Pattern 支持、重生时间及静态/动态对象边界。

## The Hexway

- 地图：`300700000`
- 状态：`RETAIL_AI_QUEST`；无实例 Handler，真端静态出生、AI 和掉落负责副本流程。
- 所有权：宝箱为 6 个 Solo A、5 选 1 Solo B 和 1 个 Party 箱；`219617` 仅保留真端 entity `220/224/225/226/227/228` 的 6 个无重生路障，死亡后由通用 NPC 生命周期删除。
- 验证：`InstanceHandlerRecoveryMigrationTest` 锁定宝箱池、路障数量/坐标/朝向/entity、无重生语义及旧 Handler 已删除。

## Archives Of Eternity

- 地图：`301540000`
- 状态：`HYBRID`；真端静态/条件出生和 Pattern 接管主流程，Handler 仅保留当前条件引擎不能承载的移动区域种族书籍。
- 权威证据：`IDEternity_01/world.xml`、`IDEternity_01/world_N.xml`、`npcaipatterns_ideternity_kgw.xml`、`npcaipatterns_ideternity_ssh.xml`。
- 所有权：20 个变量、1063 条条件出生、Boss/密室/出口 `834053/834054`、Book4 四选一静态池和 BossRoom 控制器均由真端数据负责；书籍 1/2/3 与 5/6/7 的 12 个种族点由 Handler 桥接。
- 保留边界：真端 `world_N.xml#532` 的 `Road_Set` 表达式右括号损坏，无法导入；移动区域书籍条件尚无运行时消费者，不能删除该桥接。
- 验证：`ArchivesOfEternityMigrationTest` 锁定条件变量/数量、Boss 与出口生产链、Book4 随机池、静态生产者和 Handler 边界。

## Cradle Of Eternity

- 地图：`301550000`
- 状态：`HYBRID`；真端静态/条件出生、Pattern、技能槽和掉落接管主流程，Handler 仅保留太阳祭坛缺失 Pattern 的物品转变量桥接及离图效果清理。
- 权威证据：`IDEternity_02/world.xml`、`IDEternity_02/world_N.xml`、`NpcAIPatterns_IDEternity_02_SSH.xml`、`NpcAIPatterns_IDEternity_02_Named_SSH.xml`。
- 所有权：51 个变量、358 条条件、382 个槽覆盖种族初始状态、四区护盾/守卫、存档点、飞行阶段、三名 Boss、出口和任务标记；84 个真端无条件 NPC ID 均由静态或条件出生承载。
- 掉落：`220526/220534/220593` 分别使用 6/6/9 个真端掉落组；删除旧 Handler 对中间 Boss `220540` 的自定义奖励注入。
- 保留边界：`834007` 的 `IDEternity_02_D_button` 在真端 NPC 数据中有 AI 名称，但 Pattern 文件没有对应定义；Handler 消耗 `185000267` 后只写 `ideternity_02_d_button=2`，后续对象由条件出生负责。
- 验证：`CradleOfEternityMigrationTest` 锁定条件/静态出生、任务感知标记、Pattern、Boss 掉落和 Handler 边界；`RetailPatternAI2Test` 验证三名掉落 Boss 的 Pattern 与技能槽可运行。

## Trials Of Eternity

- 地图：`301560000`
- 状态：`HYBRID`；真端静态/条件出生、Pattern、技能槽、掉落和 `ownership_world` 物品清理接管主流程，Handler 仅保留受限图书馆钥匙传送。
- 权威证据：`IDEternity_03/world.xml`、`IDEternity_03/world_N.xml`、`NpcAIPatterns_IDEternity_03_Ctrl_SSH.xml`、`NpcAIPatterns_IDEternity_03_Monster_SSH.xml`、`NpcAIPatterns_IDEternity_03_Named_SSH.xml`。
- 所有权：42 个变量、574 条条件、576 个槽和五组随机 Party 覆盖种族初始化、四区波次、半首领、最终首领、掉落对象和出口；静态出生只保留 35 个无条件点。
- 掉落：`246410/731745/731746/731747/246408` 分别提供 `185000297..185000301`；Handler 不再注入私服掉落。
- 保留边界：`731736` 消耗 `185000297` 后执行真端受限图书馆传送；`185000297..185000301` 均为 `remove_when_logout=FALSE` 且具有 `ownership_world=301560000`，断线保留并由通用离图流程清理。`247035/247036` 在真端没有 Pattern 定义，按普通战斗 NPC 运行。
- 验证：`TrialsOfEternityMigrationTest` 锁定条件/静态出生、随机 Party、掉落和 Handler 边界；`RetailPatternAI2Test` 验证三个首领和五个流程控制 Pattern。

## Archives Of Eternity Q

- 地图：`301570000`
- 状态：`HYBRID`；55 条条件、59 个槽和真端门映射接管完整场景流，Handler 仅写核心 `SCENE=13`，任务脚本写入 `USER_RACE/USER_GENDER` 与场景阶段。
- 所有权：条件出生负责主要演员、控制器和两族出口；静态出生仅保留六个真端无条件 NPC。魔族任务仍生成两个 world 数据未表达的专属演员。
- 验证：`ArchivesOfEternityQMigrationTest` 锁定场景变量、条件/静态出生边界、10 个 retail door ID、13 扇初始开启门及最小 Handler/任务桥接。

## Sanctuary Dungeon

- 地图：`301580000`
- 状态：`RETAIL_AI_QUEST`；无实例 Handler，真端 Pattern 与条件出生负责副本内种族分支，两个任务 Handler 只负责入图。
- 权威证据：`IDF6_OP/world.xml`、`IDF6_OP/world_N.xml`、`NpcAIPatterns_IDF6_YDY.xml`、`_10520Covert_Communiques`、`_20520Lost_Destiny`。
- 所有权：`703092` 看到玩家后写 `IDF6_RACE_L/D`；4 条条件选择 `806076/806080` 和两族出口 `806189/806190`。静态出生仅保留 `703092/806118` 两个无条件对象。
- 保留边界：任务脚本只执行 `teleportToInstance(301580000, 431, 491, 99)`，不生成副本内 NPC，也不写种族条件变量。
- 验证：`InstanceHandlerRecoveryMigrationTest` 锁定无旧 Handler、条件/静态边界和 Pattern 生产者；`RetailPatternAI2Test` 验证 `703092` 可由运行时接管。

## Raksang Ruins

- 地图：`300610000`
- 状态：`RETAIL_AI_QUEST`；无实例 Handler，真端 Pattern + 条件出生接管全部流程，含六个传送 NPC。
- 权威证据：`IDRaksha_solo/world_N.xml` 的 `IDRaksha_Door_5F_Boss_Exit_SPG`，以及 `NpcAIPatterns_TamesSolo_KJS.xml` 的 `IDRaksha_Re_Boss_KJS` 与 `Tames_Solo_A/B/C_Teleporter`。
- 所有权：Boss `236306` 被击杀后由 Pattern 写入 `idraksha_clear=1`，条件出生在 `619.643005/685.139893/527.079773` 生成出口 `730445`。传送 NPC `206378-380`（Abiso）/`206395-397`（Proqura）由 `Tames_Solo_*_Teleporter` Pattern 的 `on_hyperlink_clicked` 传送至 `Alias_Start_A/B/C` 并启用 `QuestArea_Course_A/B/C`（绑定 quest `18739-18741`/`28739-28741`）。
- 数据验证：`22` 个变量、`109` 条条件；`RetailPatternAI2Test` 验证六个传送 NPC 的 Pattern 在完整真端数据下可由运行时接管。
- 删除的错误机制：手写 `RaksangRuinsInstance` 副本 Handler、`ProquraAI2`/`AbisoAI2` 逐 NPC 传送 AI2（硬编码三组坐标与 `switch(worldId)` 分支）；六个 NPC 模板 `ai` 字段由 `abiso`/`proqura` 改为 `general`，retail 失效时回退通用 AI。`coverage.xml` 标注为 `RETAIL_AI_QUEST`。
- 验证：`RaksangRuinsRetailMigrationTest` 锁定条件/静态出生、出口生产链、三个 Teleporter Pattern、`Alias_Start_A/B/C`、`QuestArea_Course_A/B/C`、旧 Handler/AI2 文件已删除及模板 `ai` 已迁移。

## Fire Temple（NPC 级 retail 接管，方案2 retail 语义优先）

- 地图：`320100000`
- 状态：NPC 级 retail 接管；副本 Handler（`FireTempleInstance` 宝箱流）保留。
- 移除：`KromedeTheCorruptAI2`（212846/214621）、`StickyTrapAI2`（281243）；三个 NPC 模板 `ai` 改 `general`，由 retail Pattern（`ND2_Sum_B` 等）接管。
- 玩法变化：Kromede 终 Boss 的私服 5 阶段 HP 战斗（95/75/55/25/15% 周期施放 `16674`/`17056` + spawn `281243` StickyTrap）让位 retail `ND2_Sum_B` 行为；StickyTrap 的 spawn 定时施放 `16675` + 自杀让位 retail Pattern。
- 验证：`FireTempleRetailMigrationTest` 仍锁定 static spawns + handler 宝箱流；`RetailPatternAI2Test` 确认三个 NPC 的 Pattern 在完整真端数据下可运行。

## 方案2 全量 retail 接管（supports=true 死代码清理）

按方案2（retail 语义优先），移除全部 `RetailPatternAI2.supports(pattern, npc)==true` 的 `ai/instance` Java AI2 死代码：共 131 个 Java 文件、199 个 NPC 模板 `ai` 改 `general`，由 retail Pattern 接管。覆盖约 30 个副本，主要含 `tiamatStronghold`、`dragonLordRefuge`、`beshmundirTemple`、`rentusBase`、`transidiumAnnex`、`aturamSkyFortress`、`elementisForest`、`cradleOfEternity`、`bastionOfSouls`、`anguishedDragonLordRefuge`、`sauroSupplyBase`、`pvpArenas`、`kromedesTrial`、`archivesOfEternity`、`danuarReliquary`/`infernalDanuarReliquary` 等。

- 判据：`supports==true` 时 NPC 装配 `retail_pattern`，Java AI2 不被实例化，属死代码。
- 玩法变化：各副本 Boss/怪/物件/陷阱的私服 Java 逻辑（HP 阶段、召唤、计时、治疗、屏障等）让位 retail Pattern 行为。
- 保留：`supports!=true`（Pattern 未迁入或运行时数据不全）的 Java 桥接不动；副本级 `InstanceHandler`（阶段/奖励/清理）不动。
- 验证：`Retail*Test` 全套 165 通过、编译通过；`ai.instance` 无任何 import 引用，删除零编译断。
