# 真端副本完整审计

本文记录本轮按真端 world、AI Pattern、条件出生、静态出生和结算数据核对的结果。

## Smoldering Fire Temple（302000000）

### 真端证据

- `IDDF2_Dflame_Event/world_N.xml` 定义 11 个变量、672 条条件出生；普通/Master 分别使用 `spawn_page=1/2`。
- 真端 Pattern 推进三个阶段、Boss 房四波和最终 Boss；结算统一使用 `IDDF2_Dflame_Event_Reward`。

### 已完成

- 写入 672 条真端条件出生，保留页面、表达式、延迟、重生、walker 和反出生语义。
- Handler 初始化条件引擎并在完成时写入奖励变量；结算清理按真端 NPC score ownership 处理普通与 Master 模板。
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
- 将神器桥接从错误的技能等级 10 修正为真端等级 16；Handler 仍只保留该数据运行时暂未分发的交互，不再生成 Boss、出口或私服掉落。
- `coverage.xml` 明确记录静态世界、AI、掉落、出口、任务与神器桥接的实际 ownership。

### 验证范围

- `RetailPatternAI2Test` 验证 `256686/256688/256693/256694` 的 Pattern 在当前真端数据、技能与 walker 下均可接管；`InstanceHandlerRecoveryMigrationTest` 锁定 124 点、关键坐标、AI/技能、掉落、出口、无条件世界和 Handler 边界。
- `NochsanaFortressGateTemplateTest` 锁定 `256694 race=DRAGON_CASTLE_DOOR`，保证两族攻城兵器都能选择城门。自动化验证不替代 GM 实测。

## Asteria / Roah Upper Storerooms（300050000 / 300070000）

### 真端证据

- `idabre_up_asteria/world_N.xml` 与 `idabre_up_rhoo/world_N.xml` 分别定义 49/44 个无条件出生点，其中 40/39 个为概率选择池；坐标、朝向、walker 与候选概率均以真端 world 为准。
- `283080/Ab_RaceCheck` 在进入攻击状态时生产 `LIGHTIN/DARKIN`，`856595/IDAb_Race_Check` 在发现玩家时生产 `racecheck=1/2`；两图条件世界均声明并消费这三项变量。
- `206087/206088` 的真端 sensory polygon 进入时广播 `1400243` 并启动 900 秒计时，到期广播 `1400244`，分别生成 `281069/281074`。

### 已完成

- 静态出生改为真端选择池，删除 Roah Handler 中三组错误手工随机出生；保留真端源 Z，不将其描述为 GEO 校正。
- 将两组真端 sensory polygon 写为高优先级子区域，Handler 由 `onEnterZone` 一次性启动持久化 deadline，恢复后继续调度，后来进入玩家同步剩余秒数；删除原飞行环近似触发。
- 到期继续由 Handler 直接清理宝箱：`281069/281074 -> 281070 -> 6631` 的 Pattern 已存在，但对应宝箱自定义 AI 尚无可靠消费者，当前不能只生成清箱 NPC 后删除桥接。
- `coverage.xml` 明确记录静态池、条件变量、AI、精确计时区域与 Handler 桥接的实际 ownership。

### 验证范围

- `AbyssStoreroomRetailMigrationTest` 锁定点数、随机池、条件变量生产链、真端 polygon、一次性计时入口、deadline 恢复/同步和已删除的飞行环及错误出生。
- XML 数据加载、专项测试和主源码编译用于自动化验证；GM 实测和线上副本压测不在本窗口范围内。

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
- 专项测试锁定变量、条件、关键 NPC、静态去重和错误 Handler 残留。

## Adma Stronghold（320130000）

### 真端证据

- `iddf2a_adma/world_N.xml` 提供 `adma_t_boss`、`iddf3_dragon_fx3` 两个变量和 9 个条件区域。
- `npcaipatterns_master_4id_jsm.xml` 与 `npc-ai.xml` 共同绑定阶段、辅助出生和亡魂控制。
- compact `npc_drops` 已覆盖首领钥匙、装备包和常规掉落，Handler 注入属于重复或私服自定义逻辑。

### 已完成

- 写入 9 条真端条件出生，并移除静态出生中重复的 `237242/237243`。
- 静态出生保留 Pot 的 25% 真端分支，并加入真端坐标的 `730176` 出口。
- 删除 Handler 的重复掉落、Pot 定时器、首领/亡魂/出口手工流程和错误 Abbey 箱子逻辑。
- `AdmaStrongholdRetailMigrationTest` 锁定条件、关键 NPC、随机 Pot、出口与 Handler 残留。

## Fire Temple（320100000）

### 真端证据

- `iddf2_dflame/world_N.xml` 提供五个独立 named 组，以及 `214621` 的 10% 真端出生点；原 Handler 的 `212845` 分支没有真端依据。
- 真端掉落已覆盖稀有首领与宝箱公共掉落，Handler 注入属于重复私服逻辑。

### 已完成

- 用 `alternate_id/select_prob` 写入六个真端稀有出生，并补回纯 `214094` 静态点。
- 删除 Handler 的重复掉落、实例创建随机出生和旧自定义物品注入，合并 Kromede 重复公告。
- 保留真端 world 未接管的三档宝箱死亡后出生流程。
- `FireTempleRetailMigrationTest` 锁定稀有出生、纯 `214094` 点、Handler 删除项和保留的宝箱流程。

## 未闭环

- 其余生产副本仍需按同样 ownership 证据逐图处理；单图完成不代表全部区域完成。
- 客户端协议、基础倍率和 GEO/PATH 压测不在本批次范围内。
