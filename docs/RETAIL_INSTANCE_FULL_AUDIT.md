# 真端副本完整审计

本文记录本轮按真端 world、AI Pattern、条件出生、静态出生和结算数据核对的结果。

## Smoldering Fire Temple（302000000）

### 真端证据

- `IDDF2_Dflame_Event/world_N.xml` 定义 11 个变量、672 条条件出生；普通/Master 分别使用 `spawn_page=1/2`。
- 真端 Pattern 推进三个阶段、Boss 房四波和最终 Boss；结算统一使用 `IDDF2_Dflame_Event_Reward`。

### 已完成

- 写入 672 条真端条件出生，保留页面、表达式、延迟、重生、walker 和反出生语义。
- Handler 初始化条件引擎并在完成时写入奖励变量；结算清理按真端 NPC score ownership 处理普通与 Master 模板。
- 七件副本道具均为 `remove_when_logout=FALSE` 且具有 `ownership_world=302000000`，断线保留并由通用离图流程清理；Handler 仅清理变身效果。
- 删除旧四 Boss 串行出生、错误解锁条件、手工阶段出生和重复静态出生。
- `SmolderingFireTempleRetailMigrationTest` 锁定变量、条件、页面映射、关键阶段与 Handler ownership。

## Nochsana Training Camp（300030000）

### 真端证据

- `/Users/mc/IdeaProjects/58Server/Map/Worlds/idab1_minicastle/world_N.xml` 包含 124 个 territory 与 124 个 NPC，且没有条件区或随机 spawn group；仓库静态出生原有 122 点，缺少 `256686` 的 `328.757874/285.468597/386.559998` 和 `256688` 的 `338.743591/284.947327/386.559998`。
- compact `npc-ai.xml`、`npcaipatterns.xml` 与 `npc-skills.xml` 分别绑定 `256686/256688/256693/256694` 的 `DrGuard_AeB/DrGuard_PeB/MiBGuard_ChiefC/MiDoor` Pattern 和真端技能组；城门死亡后两条 EventPath 由现有 Nochsana walker 数据提供。
- 真端 `npcs_npcs.xml` 将神器 `700437` 绑定 `NPC_ShieldofCompassion`（技能 `276`、等级 `16`）；`700438` 的静态出口及天魔两族返回路径由 `portal_template2.xml` 接管。
- compact 掉落覆盖 `256686/256688/256693`，任务 `3732/4732` 继续以城门 `256694` 和将军 `256693` 的击杀顺序推进；旧 Handler 的活动箱与支援包没有真端依据。

### 已完成

- 补入两个缺失的真端静态出生点，完整点数由 122 恢复为 124；坐标、朝向与 `random_walk=2` 均取自真端 world。
- 允许没有 walker 的真端 `on_wake_up -> goto_waypoint 0` 视为出生点唤醒，从而使 `256686` 不再退回通用攻击 AI；Nochsana 四个关键战斗 NPC 的 Pattern 接管已由专项测试锁定。
- 公共交互链按 `NPC_AI_ProtectBuff` 与唯一完整 NPC 技能槽施放真端技能；缺槽、多槽或无效槽拒绝接管。删除 Nochsana 专用 Handler，不再硬编码 `700437/276/16`。
- `coverage.xml` 改为 `RETAIL_AI_QUEST`，明确记录静态世界、AI、技能动作、掉落、出口与任务的完整 ownership。

### 验证范围

- `RetailPatternAI2Test` 验证 `256686/256688/256693/256694` 的 Pattern 在当前真端数据、技能与 walker 下均可接管；`AI2ActionsScriptNpcRoutingTest` 锁定神器消费者的唯一完整技能槽约束；`InstanceHandlerRecoveryMigrationTest` 锁定 124 点、关键坐标、AI/技能、掉落、出口、无条件世界和无 Handler 边界。
- `NochsanaFortressGateTemplateTest` 锁定 `256694 race=DRAGON_CASTLE_DOOR`，保证两族攻城兵器都能选择城门。自动化验证不替代 GM 实测。

## Upper Abyss Storerooms（300120000 / 300130000 / 300140000）

### 真端证据

- 三图真端 world、Pattern、条件/静态出生和掉落数据覆盖完整玩法；15 把地图钥匙均声明对应 `ownership_world`。
- 真端 `Items.xml` 对这些钥匙明确设置 `remove_when_logout=FALSE`；登出删除钥匙的旧 Handler 不是真端行为。

### 已完成

- 保留通用 `InstanceService.onLeaveInstance` 按 `ownership_world` 的离图清理，删除抽象钥匙清理 Handler 和三个地图壳类。
- 三图 `coverage.xml` 改为 `RETAIL_AI_QUEST`；`AbyssStoreroomRetailMigrationTest` 锁定全部 15 把钥匙的世界归属、无 Handler 和完整数据所有权。

## Asteria / Roah Upper Storerooms（300050000 / 300070000）

### 真端证据

- `idabre_up_asteria/world_N.xml` 与 `idabre_up_rhoo/world_N.xml` 分别定义 49/44 个无条件出生点，其中 40/39 个为概率选择池；坐标、朝向、walker 与候选概率均以真端 world 为准。
- `283080/Ab_RaceCheck` 在进入攻击状态时生产 `LIGHTIN/DARKIN`，`856595/IDAb_Race_Check` 在发现玩家时生产 `racecheck=1/2`；两图条件世界均声明并消费这三项变量。
- `206087/206088` 的真端 sensory polygon 进入时广播 `1400243` 并启动 900 秒计时，到期广播 `1400244`，分别生成 `281069/281074`。
- 钥匙 `185000033..185000038` 均声明对应 `ownership_world`，且真端 `Items.xml` 明确为 `remove_when_logout=FALSE`。

### 已完成

- 静态出生改为真端选择池，删除 Roah Handler 中三组错误手工随机出生；保留真端源 Z，不将其描述为 GEO 校正。
- 将两组真端 sensory polygon 写为高优先级子区域，Handler 由 `onEnterZone` 一次性启动持久化 deadline，恢复后继续调度，后来进入玩家同步剩余秒数；删除原飞行环近似触发。
- 到期继续由 Handler 直接清理宝箱：`281069/281074 -> 281070 -> 6631` 的 Pattern 已存在，但对应宝箱自定义 AI 尚无可靠消费者，当前不能只生成清箱 NPC 后删除桥接。
- 删除 Handler 的错误登出钥匙清理；正常离图由通用 `InstanceService.onLeaveInstance` 按 `ownership_world` 回收，无专用离图桥接。
- `coverage.xml` 明确记录静态池、条件变量、AI、精确计时区域与 Handler 桥接的实际 ownership。

### 验证范围

- `AbyssStoreroomRetailMigrationTest` 锁定点数、随机池、条件变量生产链、真端 polygon、一次性计时入口、deadline 恢复/同步、六把钥匙归属及通用离图消费者。
- XML 数据加载、专项测试和主源码编译用于自动化验证；GM 实测和线上副本压测不在本窗口范围内。

## Sulfur / Left Wing / Right Wing Lower Storerooms（300060000 / 300080000 / 300090000）

### 真端证据

- `idabre_low_divine/world_N.xml`、`idabre_low_wciel/world_N.xml`、`idabre_low_eciel/world_N.xml` 分别由 sensory NPC `206091/206089/206090` 首次发现玩家后启动 900000ms 计时，并在开始时广播 `1400243`。
- 三图计时到期均广播 `1400244`，随后分别关联清箱 NPC `281077/281075/281076`；真端没有强制退出、Boss 死亡后分阶段删箱或进图立即计时流程。
- 三组 sensory polygon 分别为 8/4/6 点，高度范围为 `160..180`、`350..380`、`100..130`；坐标逐点取自真端 world。
- 恢复源码 `MainServer_ScriptDLL64/fun/fun_725.cpp` 与 `fun_730.cpp` 的对应触发路径均调用精确 `900000` 毫秒调度。

### 已完成

- 写入三组高优先级真端计时区域，由 `onEnterZone` 一次性启动持久化 15 分钟 deadline；副本恢复后继续调度，后来进入的玩家同步剩余秒数。
- 到期由 Handler 直接清理三图全部真端宝箱，并发送开始/结束系统消息；当前数据链尚不能可靠消费真端清箱 NPC，因此保留与上层仓库一致的最小桥接。
- 删除 Sulfur 飞行环近似触发和强制销毁、Left Wing 的 Boss 死亡触发与每 5 分钟分阶段删箱、Right Wing 的进图即启动与强制传送退出。
- `coverage.xml` 明确记录条件/静态出生、AI、掉落、精确计时区域和持久 deadline 的实际 ownership。

### 验证范围

- `AbyssStoreroomRetailMigrationTest` 锁定三组完整 polygon、900 秒一次性触发、deadline 恢复/同步、开始/到期消息、直接清箱及旧错误路径删除。
- XML schema、条件出生/AI loader、Pattern AI、专项测试和主源码编译用于自动化验证；GM 实测和线上副本压测不在本窗口范围内。

## Udas Temple（300150000）

### 真端证据

- 真端静态/条件出生、Pattern、掉落和静态门覆盖 Boss、传送器、三名钥匙怪及三个钥匙门。
- 钥匙 `185000083..185000085` 没有 `ownership_world`，但真端 `Items.xml` 明确为 `remove_when_logout=FALSE`。

### 已完成

- 保留 Handler 的正常离图钥匙清理，避免无世界归属的钥匙被带出副本；删除错误的登出清理，使断线恢复保留真端钥匙状态。
- `coverage.xml` 明确记录离图清理的最小旧所有者；`InstanceHandlerRecoveryMigrationTest` 锁定三把钥匙无 `ownership_world`、无登出清理及完整数据流程。

## Lower Udas Temple（300160000）

### 真端证据

- `idtemple_low/world_N.xml` 与 compact NPC AI 覆盖副本静态/条件出生及 `215783/215795/215796/215797` 的真端 Pattern；`216149/216150` 均为 Udas 宝箱模板。
- compact `npc_drops` 已定义 `702658/702659` 的活动箱、`215786/215796` 的钥匙、`215797` 的 20% 贡献包，以及两种 Udas 宝箱自身的掉落组。
- `188053788` 在真端/客户端数据中仅作为物品模板存在，当前恢复源码和 compact 掉落中没有将它归属给 Lower Udas Boss 的证据。
- 钥匙 `185000086/185000087` 没有 `ownership_world`，真端 `Items.xml` 均声明 `remove_when_logout=FALSE`。

### 已完成

- 删除 Handler 对 Boss、宝箱和活动箱的手工掉落注册，恢复 compact 掉落的概率、掉落组与所有权，避免额外 100% 掉落和每位玩家强制获得 `188053788`。
- 保留现有 12 箱 deadline 和正常离图钥匙清理；删除错误的登出清理，使断线恢复保留钥匙。真端 world 不含对应静态宝箱，恢复脚本尚未还原其完整生命周期，当前不将该机制误标为已接管。

### 验证范围

- `InstanceHandlerRecoveryMigrationTest` 锁定 Handler 不再注入私服掉落、所有替代 compact 掉落、两把钥匙无世界归属和断线保留边界。
- 真端宝箱生命周期仍需继续从 ScriptDLL 恢复或实机验证；本阶段不以掉落修复替代该机制的完成声明。

## The Hexway（300700000）

### 真端证据

- `idunderpassre/world_N.xml` 定义 6 个固定 `IDUnderpassRe_Treasurebox_Solo_A`、1 个从 5 个等概率候选中选择的 `Solo_B`，以及 1 个固定 `Treasurebox_Party`；三组均为 `no_respawn=TRUE`。
- 5 个 `Solo_B` 候选由同一 `spawn_group` 的 `select_prob=2000` 选择，故静态生成使用 `pool="1"` 保留一选一语义。
- compact `npc_drops` 不给 `219609` 分配 `185000130..185000135`；`219610` 固定掉落 `185000135`。旧 Handler 向 `219609` 额外随机注入六把钥匙没有真端所有权。

### 已完成

- 删除首领死亡后生成 12 个 Party 箱、5 分钟逐箱销毁/倒计时，以及错误的随机钥匙掉落注入。
- 将静态宝箱改为真端无重生的 6 个 Solo A、5 选 1 Solo B 和 1 个 Party 箱，坐标、朝向和源高度均取自真端 world。
- 将 `219617` 从 21 个旧私服/混合点收敛为真端 6 个无重生路障，保留 entity `220/224/225/226/227/228` 及真端坐标、朝向和源高度。
- 删除重复的 `onDie -> onDelete` Handler；通用 NPC 死亡控制器已按无重生静态出生负责衰败和删除。

### 验证范围

- `InstanceHandlerRecoveryMigrationTest` 锁定无实例 Handler、`219609/219610` 的 compact 掉落所有权、三组静态宝箱，以及 6 个无重生路障的真端坐标、朝向和 entity ID。

## Dark Poeta（300040000）

### 真端证据

- `/Users/mc/IdeaProjects/58Server/Map/Worlds/idlf1/world_N.xml` 为 UTF-16LE 真端来源；普通创建 ID `39/66/122/179` 使用 `spawn_page=1`，SP/Master 创建 ID `1001/1002` 使用 `spawn_page=2`。
- 真端条件出生共有 73 条、20 个条件变量；现有生成数据漏掉来源编号 `#3/#4/#5/#18/#19/#28`，并遗漏 `vanq`、`aboss_die`、`sboss_die`、`SpecialServer_Cond`。
- `IDLF1_Temp_01_Sp`、`IDLF1_Temp_08_Sp`、`IDLF1_Temp_09_Sp`、`IDLF1_Vanq_A_Sp` Pattern 已存在并分别推进缺失变量；`SpecialServer_Cond` 按出生页面在 Handler 初始化。
- `206478` 的三条真端出生包含同一九点 sensory polygon；现有条件加载器和 Pattern AI 数据结构已支持该字段，不需要新增 dynamic area。

### 已完成

- 补入 6 条真端条件出生，使用 `10457..10462`，保留页面、位置、初始延迟、重生延迟、战斗状态反出生和 sensory polygon。
- 规范化真端 #5/#18 的多余右括号，避免表达式解析失败。
- Dark Poeta Handler 按 `spawn_page=2` 写入 `specialserver_cond=1`，普通页面写入 `0`，使新建和恢复路径一致。
- 保留现有 Handler 的 runtime 状态、条件引擎、Pattern、分数、掉落和结算 ownership；未删除无明确真端替代的逻辑。

### 验证范围

- `DarkPoetaRetailMigrationTest` 锁定 20 个变量、73 条条件、来源编号、页面、NPC 数量、sensory polygon、表达式规范化和页面初始化。
- 运行条件表达式解析、条件出生、Pattern AI、Handler 恢复和 XML schema 专项测试；GM 实测和线上副本压测不在本窗口范围内。

## Steel Rake（300100000）

### 真端证据

- `/Users/mc/IdeaProjects/58Server/Map/Worlds/idshulackship/world_N.xml` 为 UTF-16LE 真端来源；唯一条件为 `IDSHULACKSHIP_PH_KILL == 1`，生成 `215069` 并使用真端巡逻路径。
- 恢复源码 `server58-source/MainServer_Server64/fun/fun_055.cpp` 证明 `214968` 死亡时写入 `IDSHULACKSHIP_PH_KILL=1`。
- 真端无条件出生包含 13 个随机池和 2 个固定 5.8 Named；旧 Handler 的六等分 Special Delivery、Shugo 二选一和旧版 `215064/215065` 均不符合真端数据。
- compact `npc_drops` 已覆盖 Steel Rake 钥匙、Boss 和宝箱掉落；`215081` 的 `188051416` 为 50.06%，不存在旧 Handler 强制注入的 `188053787`。
- `IDSShip_KK` Pattern 写入 `Lever_ver30`，因此该变量继续由条件出生世界声明。

### 已完成

- 新增 `10531..10545` 共 15 条 Steel Rake 条件出生：1 条击杀条件、13 个真端随机池和 1 组固定 Named。
- 恢复 Brownie/Shadowstalker 的 16 点真端巡逻路径，并按真端概率、坐标和 Party 关系生成酒馆老板、Largimark、Special Delivery、宝箱、Calydon 与 Shugo 随机池。
- 删除静态 `215069` 及随机池重叠出生点，避免开场提前出生和重复出生。
- Handler 仅保留 `214968` 的条件变量写入与退出逻辑；移除旧随机出生、重复门/宝箱出生及自定义掉落注入。

### 验证范围

- `SteelRakeRetailMigrationTest` 锁定变量、15 条条件、真端概率/坐标、waypoint、固定 Named、静态去重、Pattern 和掉落 ownership。
- 条件出生与 waypoint XML 已通过 schema；Loader、Condition Engine 和 Steel Rake 专项测试通过。GM 实测和线上副本压测不在本窗口范围内。

## Baranath Dredgion（300110000）

### 真端证据

- `/Users/mc/IdeaProjects/58Server/Map/Worlds/idab1_dreadgion/world_N.xml` 定义 8 条条件出生和 9 个无条件选择池；条件变量覆盖三个传送发生器、两个护盾开关、Surkana 计数与 17 分钟 WorldTimer。
- 恢复源码 `server58-source/MainServer_Server64/fun/fun_055.cpp` 证明 `700505/700506` 分别写入 `TELEPORT_1_DESTROYED/TELEPORT_2_DESTROYED`，`700507/700508` 分别写入 `SWITCH_1_DESTROYED/SWITCH_2_DESTROYED`，`215085` 写入 `TELEPORT_3_DESTROYED`；旧 Handler 将第三传送器错误绑定到 `215427`。
- 真端无条件池包含 `215391` 的 87.5%/12.5% 两点选择、四组囚犯 50/50 选择、一组 50/50 Party，以及固定 `215093/215390/215427`；`215390` 使用 `Path_IDAb1_Drd_17` 的 24 点巡逻路径。
- compact NPC AI/Pattern 已覆盖舰长和关键 Named 的技能行为，`npc-scores.xml` 覆盖关键计分，`npc_drops` 覆盖五个可选 Named；`214823` 没有真端掉落行，走积分、任务和副本结算。真端 world 与恢复源码均不存在旧 Handler 的五 Named 击杀后生成 `701455` 路径。

### 已完成

- 将 `300110000` 条件世界扩为 7 个变量、17 条条件，完整保留真端概率、Party、坐标、朝向、初始延迟、120 秒刷新、额外刷新时间和战斗状态反出生。
- 补入 `retail:300110000:path_idab1_drd_17` 的 24 点路径，并删除静态 `215427` 与 `798323..798330` 重叠出生。
- 17 分钟 deadline 只写真端计时变量；发电机和 `215085` 死亡只写真端变量，传送器、护盾、舰长、Named 和囚犯全部由条件引擎创建与恢复；结算和销毁时清理条件引擎，阻止延迟刷新在结束后回生。
- 删除错误 10 分钟传送器、`215391` 五五开、`215086/215390` 二选一、`215082/215093` 舱壁二选一、`215427 -> 730197`、手工恢复分支及无真端依据的 `701455` 奖励箱。

### 验证范围

- `BaranathDredgionRetailMigrationTest` 锁定 8 条真端条件、9 个选择池、Party、Waypoint、静态去重、变量生产者，以及 AI/技能、分数、掉落、任务物品和舰长对话/击杀 ownership。
- 条件出生与 waypoint XML 已通过 schema；Loader、Condition Engine、Pattern AI、Handler 恢复和 Baranath 专项测试通过，主源码编译通过。GM 实测和线上 6v6 压测仍需按验收手册执行，不以自动化结果代替。

## Draupnir Cave（320080000）

### 真端证据

- `iddf3_dragon/world_N.xml`、`NpcAIPatterns_IDDF3_dragon_SP_YDY.xml` 和 compact `npc_drops` 共同接管阶段出生、AI 与掉落。
- 条件世界由 4 条扩为 18 条，包含 `master_mode`、`lastboss`、`lastboss_t`、`iddf3_dragon_t_waveend` 等 8 个变量，覆盖 16 个真端 `condition_info` 区域及两组页级副官出生。
- `702658/702659` 属于 Adma 机制，真端 Draupnir world 不引用它们。

### 已完成

- 条件槽覆盖普通/特殊 Boss、Akhal、三类效果对象、波次控制和四名副官。
- 静态出生移除 11 个与条件槽重复的 NPC 组，避免阶段出生重复。
- 删除 Handler 中错误的 Abbey 箱子与自定义掉落；其余 Boss 掉落由 compact `npc_drops` 提供。
- `702858` 的 Pattern 生成 `702861` 种族检查器，再按玩家种族生成 `805736/805737`；Handler 不再重复生成种族入口。
- 删除 Handler 的副官计数/Bakarma 出生、`236929 -> 237275` 延时、中央控制室交互和 `236900` 袭击链；仅保留无数据生产者的 `237276` 入场幻影、提示和持久化期限。
- 专项测试锁定变量、条件、14 个具体 NPC 的 Pattern 支持、静态去重和最小 Handler 边界。

## Aetherogenetics Lab（310050000）

### 真端证据

- 真端静态出生、Pattern、掉落和静态门覆盖五名钥匙怪、`185000001..185000005` 的 100% 掉落及五扇钥匙门。
- 五把钥匙没有 `ownership_world`，真端 `Items.xml` 全部声明 `remove_when_logout=FALSE`。

### 已完成

- Handler 只保留正常离图钥匙清理；删除错误的登出清理，使断线恢复保留钥匙。
- `InstanceHandlerRecoveryMigrationTest` 锁定无登出清理、五把钥匙无世界归属、掉落/门消费和覆盖矩阵边界。

## Adma Stronghold（320130000）

### 真端证据

- `iddf2a_adma/world_N.xml` 提供 `adma_t_boss`、`iddf3_dragon_fx3` 两个变量和 9 个条件区域。
- `npcaipatterns_master_4id_jsm.xml` 与 `npc-ai.xml` 共同绑定阶段、辅助出生和亡魂控制。
- compact `npc_drops` 已覆盖首领钥匙、装备包和常规掉落，Handler 注入属于重复或私服自定义逻辑。
- `185000026..185000032` 均没有 `ownership_world`，真端 `Items.xml` 全部声明 `remove_when_logout=FALSE`。

### 已完成

- 写入 9 条真端条件出生，并移除静态出生中重复的 `237242/237243`。
- 静态出生保留 Pot 的 25% 真端分支，并加入真端坐标的 `730176` 出口。
- 删除 Handler 的重复掉落、Pot 定时器、首领/亡魂/出口手工流程和错误 Abbey 箱子逻辑。
- Handler 保留交互效果和正常离图钥匙清理；删除错误的登出清理，使断线恢复保留钥匙。
- `AdmaStrongholdRetailMigrationTest` 锁定条件、关键 NPC、随机 Pot、出口、七把钥匙无世界归属和最小 Handler 边界。

## Fire Temple（320100000）

### 真端证据

- `iddf2_dflame/world_N.xml` 提供五个独立 named 组，以及 `214621` 的 10% 真端出生点；原 Handler 的 `212845` 分支没有真端依据。
- 真端掉落已覆盖稀有首领与宝箱公共掉落，Handler 注入属于重复私服逻辑。

### 已完成

- 用 `alternate_id/select_prob` 写入六个真端稀有出生，并补回纯 `214094` 静态点。
- 删除 Handler 的重复掉落、实例创建随机出生和旧自定义物品注入，合并 Kromede 重复公告。
- 保留真端 world 未接管的三档宝箱死亡后出生流程。
- `FireTempleRetailMigrationTest` 锁定稀有出生、纯 `214094` 点、Handler 删除项和保留的宝箱流程。

## Beshmundir Temple（300170000）

### 真端证据

- `/Users/mc/IdeaProjects/58Server/Map/Worlds/idcatacombs/world_N.xml` 将祭坛 `730274/IDCatacombs_Altar_Q30208` 作为普通与困难页的常驻对象出生；蓝焰门 `730290/IDCatacombs_door3` 则在 `SpecialServer_Cond == 0` 时出生。当前静态/条件数据分别保留祭坛和门的真端位置。
- `799506/IDCatacombs_NPC_DrakanUtra` 仅有 NPC 模板；它不在该地图的真端 world、当前静态出生或条件出生中。`30208/30308` 任务脚本只注册它的对话，因此没有可替代 Handler 的数据生产者。
- `30208/30308` 分别将 `182209610/182209710` 定义为任务工作物品；任务完成时由通用结算回收。`185000091` 是 `30231/30331` 的真端任务奖励，且物品模板为 Incinerator Key。

### 保留边界

- Handler 只保留祭坛完成交互后、持有进行中任务工作物品时生成一次 `799506` 的桥接，蓝焰门消耗 `185000091` 后删除门，以及六把无 `ownership_world` 钥匙的正常离图清理。
- 真端 `Items.xml` 将 `185000091..185000096` 全部设为 `remove_when_logout=FALSE`；删除旧 Handler 的登出清理，断线恢复时不再丢失钥匙。
- 所有其它 Boss、门、波次、掉落与阶段出生继续由真端条件出生、Pattern 和静态数据负责；未恢复出数据消费者的两条交互不删除或扩展。

### 验证范围

- `BeshmundirTempleInstanceTest` 锁定祭坛/门的数据所有权、`799506` 没有地图数据生产者、两族任务工作物品与钥匙任务奖励，以及 Handler 没有旧版私服机制残留。

## Kromede's Trial（300230000）

### 真端证据

- `/Users/mc/IdeaProjects/58Server/Map/Worlds/idcromede/world_N.xml` 包含红色遗物 `282095`、蓝色遗物 `282093` 与封印石门 `700835` 的真端出生；当前静态出生保留相同对象和位置。
- compact NPC AI 将两件遗物分别绑定到 `Cromede_Relic1/2`：`282095/282093` 死亡后生成 `282084/282085`，其 `Cromede_Relic1/2_Noshow` Pattern 在醒来时施放唯一技能槽 `19273/Cromede_CurePhysical_Nr` 或 `19274/Cromede_CureMental_Nr` 并自销毁。旧 Handler 的 `19247/19248` 玩家增益既不在真端调用链中，也不会由这些怪物型 NPC 的死亡事件触发，已删除。
- `19220/Polymorph_cromede` 与 `19270/Polymorph_cromede_dark` 是两套独立真端变身模板。原 Handler 以从未赋值的 `skillRace` 选择它们，导致所有玩家进入时都使用 `19220`。

### 已完成

- 删除无效实例级种族状态，按进入玩家的实际种族选择 `19220/19270`；魔族不再误用天族变身。
- Pattern 接管完整遗物死亡/净化链；Handler 仅保留石门死亡后立即删除、离开/登出清理副本钥匙与道具，以及入口影片/说明页，这些路径当前仍没有可替代的真端数据消费者。

### 验证范围

- `KromedesTrialInstanceTest` 锁定两族变身映射，并继续锁定 Pattern 已接管的首领选择、受伤 NPC、感应提示、任务影片和已删除的私服掉落/出生。

## Taloc’s Hollow（300190000）

### 真端证据

- `/Users/mc/IdeaProjects/58Server/Map/Worlds/idelim/world_N.xml` 为 UTF-16LE 真端来源，含五个固定 `Heal_Plant`、无重生的 `IDElim_FOBJ_BugEgg`，以及在 `IDElim_3F_Heal_Plant_Giant == 50` 时生成的巨型治疗植物 `700941`。当前静态出生与条件出生分别承接这些对象。
- 真端 `NpcAIPatterns_IDElim_OSY.xml` 的 `Elim_ComadFe2/Elim_ComadMe2` 在 3F Komad 死亡时推进巨型植物变量；`Elim_ClodwormNm` 和 `Elim_NeutflyNm` 分别控制逻辑门号 `1/2`。compact Pattern 和 NPC AI 已保留这条链。
- 真端对象表将门 `49/7/48` 标为初始开启、门 `180` 标为初始关闭且可点击；原静态门数据缺少开启状态，且门 `48/7` 缺少供 Pattern 查找的逻辑 ID。
- compact `npc_drops` 已负责 `215456/215478/215482` 的副本技能物品和 `215488` 的常规掉落；两族任务入口在传送前发放各自的果实/泪水并在传送失败时回滚。旧 Handler 的 minion 契约、纪念品和手工 Boss/对象流程没有数据所有权。

### 已完成

- 将门 `49/7/48` 的真端开启状态写入静态门数据，将门 `180` 标为可点击；为 Pattern 控制的门 `48/7` 分别补入逻辑 ID `1/2`，不恢复旧 Handler 的手工开门。
- 普通/巨型治疗植物由 ScriptNpc 复用真端 `Elim_HealtoPC01/02` 的 `19229/19230` 技能与成功后自清理语义；Handler 只保留单人副本影片和离开时的副本效果/随从/技能物品清理。`limits.xml` 将该图限制为单人，影片去重不会跨玩家抑制。
- 继续由静态/条件出生、Pattern、掉落和任务接管其余流程，不重新引入手工出生、掉落、奖励或 Boss 死亡回调。

### 验证范围

- `TalocsHollowQuestMigrationTest` 锁定任务物品发放与回滚、治疗植物条件出生、无重生虫卵、Handler 的最小边界，以及四扇门的真端状态和 Pattern 逻辑 ID。
- XML 数据加载、专项测试和主源码编译用于自动化验证；GM 实测仍应覆盖三扇初始开启门、钥匙门及巨型治疗植物出现后的交互。

## Raksang Ruins（300610000）

### 真端证据

- `/Users/mc/IdeaProjects/58Server/Map/Worlds/IDRaksha_solo/world_N.xml` 的 `IDRaksha_Door_5F_Boss_Exit_SPG` 在 `idraksha_clear == 1` 时生成出口 `730445`，坐标为 `619.643005/685.139893/527.079773`、朝向 `240`。
- `NpcAIPatterns_TamesSolo_KJS.xml` 的 `IDRaksha_Re_Boss_KJS` 在 Boss `236306` 被击杀时写入 `idraksha_clear=1`；compact Pattern 已保留这项动作。
- 原条件世界缺少该变量和出口出生，致使 Pattern 因变量未声明而不能接管；原 Handler 则在错误的 `648.5508/700.05725/522.0487` 手工生成出口。

### 已完成

- 条件世界新增 `idraksha_clear` 与真端出口条件出生（共 22 个变量、109 条条件），完整保留延迟、`idle_live_range=-1` 与战斗状态反出生语义。
- 删除 Raksang Handler 的 Boss 死亡回调；出口现在由真端 Pattern 和条件出生链生成。

### 验证范围

- `RaksangRuinsRetailMigrationTest` 锁定变量声明、Boss Pattern 生产链、出口条件出生的真端坐标与手工回调删除。
- 条件出生加载、Pattern AI、专项测试和主源码编译用于自动化验证；GM 实测仍应覆盖击杀 Boss 后的出口可见与可交互性。

## Haramel（300200000）

### 真端证据

- `idnovice/world_N.xml` 的 116 条无条件出生与当前静态点一一对应；`Shugo_IDNovice_1/2/3` 的刷新时间均为 1 秒，`IDNovice_Entrance_Out` 与 `IDNovice_Elevator_Lever_Up` 均为 60 秒。
- 真端 `216922` 的 AI 名称就是 `idnovice_Hameroon`；compact loader 会统一大小写，实际解析为 `IDNovice_Hameroon` Pattern。该 Pattern 负责随从、`700829` 宝箱和 `IDNovice_Out`，对应模板、字符串、区域与路径数据均已存在。
- 真端 world 未提供升降机上行落点；现有 `TowerLiftAI2` 仍将玩家送往 `220/213/126.68472`，不能据此宣称该交互已完整恢复。

### 已完成

- 将五个遗漏的可重生静态对象补为真端刷新时间：三名 Shugo 为 1 秒，入口出口与升降机上行拉杆为 60 秒。
- 保留真端首领 AI 映射，回归测试同时锁定其解析出的 Pattern、技能支持、动态宝箱/出口不属于静态出生，以及升降机桥接边界。

### 验证范围

- `RetailPatternAI2Test` 验证 `216922` 在当前 Pattern、技能和模板数据下可由通用 Pattern AI 接管；`InstanceHandlerRecoveryMigrationTest` 锁定五个重生时间与静态/动态 ownership。
- 自动化验证不替代升降机终点的 GM 实测；在恢复真端坐标前，该桥接保持原样。

## Archives Of Eternity（301540000）

### 真端证据

- `IDEternity_01/world_N.xml` 定义 20 个可生产变量。53 个多分支 condition list 中，52 个经真端缺失右括号规范化后可并列求值；`#294` 的四个传送变量由 `IDEternity_01_Teleport_Check_01..04` 在同一消息动作中将其余变量设为 `-1`，因此运行时只有一个出口分支成立。
- `IDEternity_Zone1_Setting` 初始化四个 Boss 房变量，并在发现玩家后写入 `Race`；Spawn/Boss/SecretRoom 控制 Pattern 继续生产 `Spawn_Set`、`Boss_Set`、`Boss_Class`、`Secret_Room_Choice`、`Sub_Boss_Die`、`End_Boss_Die`。
- `#301/#518/#870` 分别是 Book2/6、Book3/7、Book1/5 的两族移动区域出生。当前条件运行时不支持这些 `moveareaindex`，故生成器明确拒绝它们。Book4 为 `world.xml` 的 no-respawn 四选一 `spawn_group`，不是四个常驻对象。
- `#532` 第二分支含多余右括号，涉及 `Road_Set` 与 `2nd_Boss_Room`，源数据本身无法解析；本次不猜测或修补真端语义。

### 已完成

- 导入 20 个变量、1063 条条件、1063 个槽和 66 个 Party；Boss 组合、密室箱 `806139`、最终两族出口 `834053/834054` 与传送器均转由真端条件/Pattern 链产生。
- 静态出生按真端安全合并 65 点、移除 4 个旧点并新建 10 组；本地缺少该图 GEO，新增点保留真端 Z。补回 Book4 的原生 `pool="1"`，保留四个真端 entity ID 与随机移动范围。
- 删除 Handler 的手写随机 Golem、拟态/箱子、门/出口、死亡删除、延迟消息和重复 `onInstanceCreate` 调用。Handler 仅按首位玩家种族在真端 12 个 Book1/2/3、5/6/7 点位生成对应模板。

### 验证范围

- `ArchivesOfEternityMigrationTest` 锁定变量/条件/Party 数量、Boss 与出口生产链、Book4 池、静态 Pattern 生产者、未导入书籍条件和 Handler 的最小边界。
- XML loader、条件出生、Pattern AI、专项测试与主源码编译用于自动化验证；`Road_Set #532` 修复前不得将该图标记为完全闭环，GM 实测仍应覆盖全部道路组合与书籍交互。

## Cradle Of Eternity（301550000）

### 真端证据

- `IDEternity_02/world_N.xml` 有 51 个可生产变量、199 组条件列表。其中 117 组为多分支列表；`strong_a..d=2` 时对应 `da/li_01..04` 由守卫 Pattern 同时写为 8，和护盾范围条件互斥。四个 `save_01..04=2` 也由存档控制器单选推进，因此这些分支可并列导入，无需人为顺序保护。
- `IDEternity_02_Start` 初始化强度、飞行、存档、Boss 和两族变量，并在看到玩家后只把实际种族一侧的 `li/da` 写为 1。种族 NPC、四区护盾、门、存档点、Boss 和出口随后均由条件表达式选择。
- `IDEternity_02_Tower`、`IDEternity_02_Nepilim` 和 `IDEternity_02_SnakeM_Fly` 分别推进 `named_die_01/02/03`；`npc_drops_part_009.xml` 为 `220526/220534/220593` 提供 6/6/9 个掉落组。
- 五个条件感知 NPC `206548/206555/206590/206591/206592` 是任务区域标记，NPC AI 为 `NoAction`；运行时任务由 `zones_quest.xml` 和任务 Handler 消费，但其真端出生仍按原条件保留。四个可重生感知 NPC `206547/206549/206551/703374` 由静态出生承载。

### 已完成

- 导入 51 个变量、358 条条件和 382 个槽，覆盖全部 117 组合法多分支条件、五个任务感知条件、无条件感知出生，以及起始控制器、治疗控制器、双门和 20 个真端宝箱。
- 对照 `world_N.xml` 逐项核验无条件出生：84 个唯一 NPC ID 均可在静态或条件出生中找到，未覆盖数为 0。
- 删除 524 行 Handler 中的手写种族出生、死亡计数、Boss/出口/门生成、随机掉落、阶段 deadline、区域消息和重复效果逻辑。太阳祭坛 `834007` 因真端只提供 AI 名称、没有 Pattern 定义，保留最小桥接：消耗 `185000267` 后写 `ideternity_02_d_button=2`，其余效果和出生仍由条件数据处理。

### 验证范围

- `CradleOfEternityMigrationTest` 锁定变量/条件/槽数量、种族和强度分支、太阳祭坛、Boss 出口、无条件控制器、任务感知标记、三档掉落及 Handler 边界。
- `RetailPatternAI2Test` 使用实际 NPC 技能槽验证 `220526/220534/220593` 的 Pattern 可被运行时接管；共享 loader、条件引擎测试和主源码编译作为提交门禁。

## Trials Of Eternity（301560000）

### 真端证据

- `IDEternity_03/world_N.xml` 与 `NpcAIPatterns_IDEternity_03_Ctrl/Monster/Named_SSH.xml` 共同生产种族初始化、四区波次、半首领、最终首领、出口和任务区域；关键变量为 `race/wave/waveend/semibossend/bossrise/bossend`。
- `246418/246440/247075` 分别承载防御首领和两族终局首领 Pattern；`246443/246444/246747/246754/247022` 负责种族、房间、终局延迟、初始条件和任务感知控制。
- `247035/247036` 的 AI 名称在真端 ScriptDLL 注册，但 Map/XML 没有对应 Pattern；它们是条件出生的普通战斗波次 NPC，不应伪造 Pattern。`npc_drops_part_017/018.xml` 为 `246410/731745/731746/731747/246408` 提供副本钥匙和四本区域书。

### 已完成

- 现有单轨迁移导入 42 个变量、574 条条件和 576 个槽，包含五组随机 Party，接管开场、四区波次、半首领、最终首领、掉落对象与出口；静态出生仅保留 35 个真端无条件点。
- Handler 只保留 `731736` 消耗 `185000297` 后的受限图书馆传送；`185000297..185000301` 均为 `remove_when_logout=FALSE` 且具有 `ownership_world=301560000`，断线保留并由通用离图流程清理。Handler 不再持有波次、死亡、硬编码出生、私服掉落或线程任务。

### 验证范围

- `TrialsOfEternityMigrationTest` 锁定条件数量、随机 Party、静态/动态出生边界、五件真端掉落物及 Handler 最小职责。
- `RetailPatternAI2Test` 使用实际技能槽验证三个首领，并验证种族、房间、终局和感知控制 Pattern 的运行时能力；共享 loader、条件引擎测试和主源码编译作为提交门禁。

## Archives Of Eternity Q（301570000）

### 已完成

- 现有迁移导入 `SCENE/USER_GENDER/USER_RACE`、55 条条件和 59 个槽，条件出生负责主要演员、场景控制器和两族出口；静态文件只保留六个真端无条件 NPC。
- 十个 retail door ID 映射到本地门，13 扇门按真端初始开启。Handler 只写 `SCENE=13`，两族任务脚本写场景阶段及玩家种族/性别；魔族任务保留两个 world 数据未表达的专属演员。

### 验证范围

- `ArchivesOfEternityQMigrationTest` 锁定场景条件链、静态/条件出生边界、门映射、初始门状态以及 Handler/任务桥接；该图已纳入迁移状态和 coverage 所有权说明。

## Sanctuary Dungeon（301580000）

### 真端证据

- `IDF6_OP/world_N.xml` 以 `IDF6_RACE_L/D` 两个变量定义两组双分支条件：分别选择天/魔族剧情演员 `806076/806080` 和出口 `806189/806190`。
- `NpcAIPatterns_IDF6_YDY.xml` 的 `LF6_F2_Din_04_Enter_Attack_67` 绑定静态控制 NPC `703092`，看到天族或魔族玩家后写入对应种族变量。真端无条件出生还包含火焰效果 `806118`。
- `_10520Covert_Communiques` 与 `_20520Lost_Destiny` 只负责将两族玩家送入 `301580000`；副本内没有独立 Handler，也没有 Java 种族出生分支。

### 已完成

- 现有迁移导入两个变量、4 条条件和 4 个槽，静态出生只保留 `703092/806118` 两个真端无条件点；演员和出口不与静态出生重复。
- coverage 所有权改为真端 AI/条件出生与两个入图任务，移除只指向单个本地绝对路径的旧说明。

### 验证范围

- `InstanceHandlerRecoveryMigrationTest` 锁定无旧 Handler、种族变量、4 条条件、四个条件 NPC、两个静态控制对象及 Pattern 生产者。
- `RetailPatternAI2Test` 以实际 NPC 上下文验证 `703092` 的种族 Pattern 可被运行时接管；共享 loader、条件引擎测试和主源码编译作为提交门禁。

## 未闭环

- 其余生产副本仍需按同样 ownership 证据逐图处理；单图完成不代表全部区域完成。
- 客户端协议、基础倍率和 GEO/PATH 压测不在本批次范围内。
