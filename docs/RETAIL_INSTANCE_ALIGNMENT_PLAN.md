# 真端副本整体对齐实施计划

## 目标

以 5.8 真端数据、恢复源码和客户端协议为权威，逐步移除能够被真端数据完整替代的副本 Java 逻辑；对缺少数据、运行时消费者或协议证据的部分保留最小桥接并明确记录，不猜测行为。

本计划完成的判据不是“没有 Handler”，而是每张地图的入口、出生、AI、路径、门、阶段、计分、奖励、退出和恢复都有唯一且经过验证的所有者。

## 当前基线

- `coverage.xml` 登记 140 条所有权记录；生产/验收范围排除已禁用且需客户端补丁的 `300260000` Elementis Forest 后为 139 张：90 `HANDLER`、24 `RETAIL_AI_QUEST`，其余 25 张由匹配、锦标赛、活动、住宅、纯数据或测试系统负责。
- `instance/handlers/scripts` 有 101 个 Java 文件，其中 96 个直接绑定 `@InstanceID`。
- `ai/instance` 有 305 个 Java AI；已删除 134 个经 `RetailPatternAI2.supports(...) == true` 证明不可达的旧实现。
- 12,798 个已加载 Pattern 中，15 个因当前执行器不支持的事件/条件/动作而结构拒绝。
- 2,448 条 NPC 映射缺完整技能槽：828 条无技能分配、1,620 条 Pattern 索引越界、0 条已分配槽位孤儿。
- 副本范围内共有 142 个受技能槽缺口影响的地图/NPC 绑定，涉及 35 张地图、137 个唯一 NPC。
- 副本范围另有 77 条 NPC 运行时数据缺口：74 条具名出生路径缺失、2 条出生/删除 NPC 名称不存在、1 条 waypoint 起点为空；启动回退日志会输出具体 `world + npc + pattern + reason`。
- 139 张生产/验收范围地图均在 `coverage.xml` 记录了细粒度所有权，且 `behavior_source` 不再只指向整份 Handler；`RetailInstanceOwnershipTest` 锁定范围和该约束。
- 动态奖励恢复已覆盖全部 23 个“全局结束时发奖”的 Handler；Dredgion、战场、Arena、Luna、Shugo Vault 与 Time Attack 共用奖励账本的稳定键、载荷校验和离线重放，主动停止/逐层即时结算的三个 Crucible 路径保持原所有者。
- GEO/PATH 自动验收锁定 137/137 个生产地图资源、78 个 waypoint 世界、59 个无 waypoint 世界、空间高度层、动态障碍版本、有界请求队列和实例销毁清理；两个 Tournament 测试图不属于生产 GEO 范围。
- 阶段/Boss 自动机制族已覆盖技能槽、召唤/Party、条件死亡、区域/风道、动态状态、deadline、稳定对象键和重启恢复；5.8 客户端实战验收仍单列在阶段 5，不以单元测试替代。

基线数字随提交更新，不把历史文档中的百分比当作当前事实。

## 执行规则

1. 每张地图或同构地图族一个实现提交；公共运行时能力单独提交。
2. 修改前必须同时核对 Handler、NPC AI/Pattern、条件出生、静态出生、路径、计分、掉落、奖励、任务和入口/出口。
3. 删除 Java 所有权前，必须证明真端数据生产者和当前运行时消费者都存在。
4. `RetailPatternAI2.supports(...)` 是 NPC AI 删除的硬门槛；不能用结构支持替代具体 NPC/地图运行依赖校验。
5. 缺失 ID、坐标、技能槽、条件变量或结算语义时停止，不从相似副本或旧私服逻辑猜值。
6. 每一步至少留下一个会在回归时失败的自动检查；涉及协议、重启恢复或多人结算时还必须完成实机验收。
7. 每个提交只包含该步骤文件，保持可独立回滚；不使用宽泛暂存。

## 阶段 0：基线与防回归

目标：让后续进度可以由程序重算，而不是依赖手写百分比。

- [x] 建立副本 Retail AI 闭包测试，按地图/NPC 输出 Pattern、技能槽和运行时依赖的拒绝原因。
- [x] 建立 `coverage.xml` 与 `@InstanceID`、任务入口、匹配入口的所有权一致性检查。
- [x] 将运行时回退日志按 `world + npc + reason` 去重并输出已格式化参数。
- [x] 生成可复现的基线摘要：地图分类、Handler、专用 AI、Pattern 拒绝原因和缺失依赖数量。

提交边界：日志修复、闭包测试、覆盖一致性测试、基线文档分别提交。

## 阶段 1：副本 NPC AI 闭包

目标：优先消除会改变 Boss 阶段和技能序列的回退。

- [x] 从同版本真端 NPC/技能数据重新核对 2,448 条技能槽缺口；副本缺口中没有 `HERO`/`LEGENDARY`，并已删除 `215284` 从相似 NPC 推断出的无来源技能分配。
- [x] 对 15 个通用语义拒绝 Pattern 逐项分类：9 个真端明确失败、1 个缺路径数据、5 个仅测试对象，没有可据此新增的公共消费者。
- [x] 对缺失出生/删除 NPC 名称、跨世界变量和路径起点逐项证明数据来源；DF6 两个具有完整条件出生消费者的跨世界变量已迁入，其余缺口继续回退。
- [x] 每批重新运行完整 NPC 映射闭包，禁止增加任何既有拒绝类型或数量；本批通过 `RetailAiSkillSlotClosureTest`、`RetailPatternAI2Test`、`RetailGiveScoreClosureTest`、`RetailInstanceOwnershipTest`、`RetailConditionSpawnEngineTest`、`RetailPatternRuntimeStateTest`、`InstanceConfigTest`、`RetailAiDefinitionLoaderTest`、`AI2EngineRetailSelectionTest`（128 项）。
- [x] 对新接管的阶段 Boss 完成技能 ID、等级、召唤和清理链自动机制族验收；5.8 客户端实战验收继续由阶段 5 跟踪。

提交边界：同一数据来源的一组 NPC/技能修复一个提交；对应运行时语义另一个提交；旧 AI 删除再单独提交。

当前技能槽核对使用 `/Users/mc/IdeaProjects/58Server/Map/XML/China/npcs.xml`（SHA-256 `c1bfe5470fa369a026e80f1c515dcb1c1f6ea2a86a115e7f0b97383e09fccbe7`）；V3 临时重建产物与仓库 `npc-skills.xml` 的 SHA-256 均为 `7533e8d2b4489700712951281724f8e1c4d0bb4caf1f46a12df55cc9a61fa3be`。

高优先级 `EXPERT/ELITE` 批次共 17 个绑定，已逐项核对完成：`300040000:215284`、`300220000:216948/216949/216950/216951/216952/216960`、`300260000:217234/217235/217277`、`300600000:219553/219554/219555/219563` 的真端 NPC 均没有 `<skills>`；`310100000:214801`、`320150000:218672`、`300620000:236302` 分别只有 7、7、3 个技能槽，但对应 Pattern 分别访问索引 7、7、3。当前没有可证明的数据修复，禁止从同构 NPC 复制技能，17 个绑定全部继续回退。

结构拒绝分类：

- 仅测试对象：`TEST_AI_GIVE_AbyssPoint`、`TEST_AI_GIVE_Money`、`TEST_AI_GIVE_WorldScore`、`Test_Basic_Monster_AI_JSM_1`、`Test_Basic_Monster_AI_KMD_2`。
- 真端明确失败：`IDArena_S1_D_Monster_4`、`IDArena_S1_Monster_4`、`IDArena_pvp02_S1_Drakan_02`、`IDDF3_T_Monster_04`、`IDEternity_02_Tower_Area_Ctrl_06`、`IDEternity_02_Tower_Area_Ctrl_07`、`IDEternity_Q_Sado_As_02`、`Raksha_Dragon_HNmd`、`Raksha_Dragon_NNmd`。它们的拒绝动作都传入 `SKILLI_NONE`；恢复源码 `FUN_140327140` 将该枚举解析为负技能索引，`AP_UseSkill` 和 `AP_UseSkillByAttackerIndicator` 随即返回失败，不能当作空操作放行。
- 缺路径数据：`IDSeal_Guardian_Chief_02` 使用 `SPAWN_LOCATION_WAY_POINT_START`，但 `pathname` 为空，无法证明出生起点。
- 当前副本出生中仅有 `300300000:217478/217487`、`301550000:220563/220564` 和 `301390000:855461` 命中以上生产 Pattern，均继续回退；其余生产 Pattern 当前没有静态或条件出生。

运行时数据缺口分类：

- 全部 Pattern 中有 24 个不存在于当前真端 NPC 数据的出生/删除名称、29 个未在目标 world 声明的跨世界变量绑定和 1 个空 waypoint 起点；对应精确集合由闭包测试锁定。
- 副本范围共有 77 条：`301110000/301330000/301360000/301640000/320080000` 的 74 条具名出生路径在真端目录中只有 Pattern 引用，没有路径实体；`400030000:277224` 引用不存在的 `Hnikar_a1/Pucio_al`；`301390000:855461` 的 waypoint 起点没有 `pathname`。
- 跨世界动作共有 24 个唯一目标 world/变量对；DF6 的 `DF6_QuestNPC_Spawn`、`QuestNpc_Sun_Dark_To_Light` 已按真端 3 个条件出生闭合，剩余 17 对缺完整消费者或完整生成数据，继续由运行时闭包拒绝。当前副本出生仍未命中这些拒绝绑定。

## 阶段 2：公共消费者与 ScriptDLL 事件层

目标：补齐 Pattern 已表达但服务器没有执行入口的公共能力。

- [x] 将竞技场 `300450000:207101` 的 `give_score` 接入 Harmony 队伍积分、稳定出生键/对象 ID 幂等、持久化和封顶结算链。
- [x] 完成其余可达 `give_score` 副本计分消费者矩阵：Kamar 8 个交互对象、Landmark 2 个终局对象和 Harmony 其余 24 个对象已接入；普通 Arena 重复击杀/既有交互桥接、Tournament 无 Handler 消费者及 Mysticarium 零分对象继续有证据地回退。完整明细见 `docs/RETAIL_GIVE_SCORE_CONSUMER_MATRIX.md`。
- [x] 按真端类型实现直达传送门传送表现、开放通知、次数耗尽关闭及 RvR 区域生命周期；缺端点坐标的 `97..128` 共 32 个定义继续拒绝，额外 AP 次数等字段不完整的定义也不提前接管。
- [x] 闭合 `set_condition_spawn_variable_to_world` 目标世界变量：迁入 DF6 两个完整变量消费者，17 个缺消费者或完整生成数据的目标对继续明确拒绝。
- [x] 为技能/控制区域、风道、移动碰撞、世界场景和 NPC 刷新动作建立明确消费者矩阵；完整明细见 `docs/RETAIL_PATTERN_ACTION_CONSUMER_MATRIX.md`。
- [x] 将可数据化的 NPC 对话、物品、影片、区域和传送桥接迁出副本 Handler；`RetailScriptNpcConsumerClosureTest` 锁定 22 个保留文件、`5/16/1` 分类和零 `SCRIPT_NPC_CANDIDATE`，其余桥接均有缺失生产者或状态事务证据。
- [ ] 将可数据化任务迁入 XML/`ScriptQuest`；跨系统特殊任务保留最小 Handler。

提交边界：每项公共消费者一个提交；每个 NPC/任务迁移批次一个提交。

## 阶段 3：已审计混合副本收口

目标：先处理边界已知、改动最小的 16 张 `HANDLER` 地图和一个已知残留的 `RETAIL_AI_QUEST` 地图。

执行顺序：

1. `300200000` Haramel：恢复 Tower Lift 真端终点并删除 `TowerLiftAI2`。
2. `300160000` Lower Udas Temple：钥匙登出/离图语义已闭环；仍需恢复 12 箱完整真端生命周期。
3. `300050000/300060000/300070000/300080000/300090000`：恢复清箱 NPC 消费链，删除直接清箱桥接。
4. `300700000` The Hexway：恢复路障生命周期消费者。
5. `300170000` Beshmundir Temple：补齐祭坛演员和蓝焰门交互消费者。
6. `300190000` Taloc's Hollow、`300230000` Kromede's Trial：迁移交互、影片和效果清理桥接。
7. `301540000/301550000/301560000/301570000`：补齐移动区域、太阳祭坛、钥匙传送和场景变量消费者。
8. [x] `300030000` Nochsana Training Camp：公共交互链按 `NPC_AI_ProtectBuff` 与唯一技能槽接管神器效果，专用 Handler 已删除。
9. [x] `300120000/300130000/300140000`：真端钥匙为 `ownership_world` 且 `remove_when_logout=FALSE`；通用离图清理接管，四个错误登出清理 Handler 已删除。
10. [x] `300050000/300070000`：六把钥匙同样由 `ownership_world` 通用离图流程接管；删除 Handler 的错误登出清理，清箱桥接仍按第 3 项保留。
11. [x] `301560000` Trials Of Eternity：五件副本道具由 `ownership_world` 通用离图流程接管；Handler 仅保留受限图书馆钥匙传送。

每张图完成后更新 `coverage.xml` 与 `retail-instance-migration-status.md`；只有所有运行所有者均数据化时才改为 `RETAIL_AI_QUEST` 或删除 Handler。

## 阶段 4：75 张粗粒度 Handler 逐图审计

目标：把仅指向 Java 文件的 `behavior_source` 改为具体所有权说明，并移除已被真端数据覆盖的逻辑。

已完成 `301220000` Iron Wall Warfront 所有权审计：Retail 静态出生与 28 个变量、486 条条件出生负责场景演员、巡逻和 Boss 生产，Handler 没有私有出生桥接；静态弹药箱生产的三项物品真端断线保留，但归属 Eternal Bastion，故仅由本图 Handler 在正常离图清理；Handler 保留可恢复的调整/准备/战斗/退出阶段、真端 NPC 分值消费的稳定键幂等、多人结算和四扇初始门。

已完成 `301210000` Engulfed Ophidan Bridge 所有权审计：Retail 静态/条件出生和 Pattern 负责场景演员及控制流；Handler 保留可恢复战场阶段、非 Pattern `give_score` 的 NPC 计分、炮弹消耗/火炮效果/私有炮击、两扇门、结算和清理。

已完成 `301310000` Idgel Dome 所有权审计：紧凑条件出生没有该世界生产者，补给、Kunax/护卫和火焰陷阱继续由 Handler 的真端坐标定时/交互桥接；三项本图物品真端断线保留，普通离图由通用流程清理，显式退出仍由 Handler 清理；Handler 继续唯一负责战场生命周期、计分、结算、门和恢复。

已完成 `301320000` Lucky Ophidan Bridge 所有权审计：真端 `IDLDF5_Under_01_PC` 的逃亡者/中段 Boss 状态机依赖未导入的 `ra_*` 变量和条件出生，不能用可加载 Pattern 代替该生产者；Handler 继续负责八组随机池、终局箱子/出口/机会包及私有掉落桥接，删除与其同坐标的静态 `235768` 重复出生。

已完成 `301380000` Danuar Sanctuary 所有权审计：Retail 静态出生、5 个条件变量/9 条条件和 Pattern 负责已导入演员；真端 `235624/235625/235626` 的 `3333/3333/3334` 无条件随机组复用现有条件出生选择、稳定持久化和单次死亡恢复，删除 Handler 的重复随机 Boss 与错误坐标出口；种族守卫、钥匙交互、定时提示和恢复继续由 Handler 所有。`185000181..185000183` 唯一归属 `301140000`，删除本图陈旧清理。

已完成 `301500000` Stonespear Reach 所有权审计：Retail 静态出生、7 个条件变量、Legion Pattern 和 `npc-scores` 负责演员、`boss_on == 10` 终局 `855843` 与战斗计分；Handler 不生成演员，仅以稳定键幂等消费死亡/分数，并保留可恢复的限时、结算、复活、退出和恢复。

已完成 `301630000` Contaminated Underpath 所有权审计：Retail 条件出生与 Pattern 负责 `IDLUNA_DEF_PHASE_*` 波次、路径、终局和奖励演员；`182007405` 真端断线保留且归属本图，由通用离图流程清理；Handler 不生成演员，仅由门 `28` 启动第一阶段，并保留可恢复的限时/计分/结算与效果清理。

已完成 `301631000` [Event] Contaminated Underpath 所有权审计：`TIMEATTACK_PLAY_START` 条件出生启动真端 Pattern 波次，`Wave_4_Start` 按 `SpecialServer_Cond` 产生普通 `248525` 或特殊 `248947` 终局并使用同一真端路径；持久副本货币 `186000470` 与现金钥匙 `186000495` 真端断线保留且无世界归属，离图也不删除；Handler 不生成演员，仅负责门 `57` 启动、对象 ID 幂等计分、两个终局的可恢复结算、奖励和效果清理。

已完成 `301640000` Secret Munitions Factory 临时物品边界：本图 `164002362` 真端断线保留并由 `ownership_world` 通用离图流程清理；删除 Handler 对 Kumuki Cave 物品 `164000418` 的跨图清理；Handler 仅保留可恢复的限时/计分/结算、Luna 奖励与效果清理。

已完成 `301700000` Treasure Island Of Courage 所有权审计：真端仍有 `idrun_treasure_despawn == 50` 条件生产者，而紧凑条件表没有该世界；阶段、宝箱和结算不能因静态出生或 Pattern 存在而误判为数据接管，继续由 Handler 负责并由回归测试锁定拒绝边界。

已完成 `302400000` Crucible Spire 所有权审计：55 个变量、798 条 Retail 条件负责种族/楼层演员；删除首次进入对 `247376/247386` 的重复手工出生。真端对 Infinity 实例数据有专用保存/加载路径，临时物品 `164000530` 断线保留，故删除登出强制退出；Handler 保留持久化楼层、飞行环传送、失败恢复、显式退出清理及未建模临时物品生产。

已完成 `301670000` Ophidan Warpath 与 `302350000` Evergale Canyon 所有权审计：两图的 Retail 静态/条件演员、路径和页面变体保持数据化，Handler 只拥有持久化战场阶段、真端计分消费者、结算、复活、退出和恢复；Evergale 的 20 个变量/347 条条件与 Ophidan Warpath 的 10 个变量/31 条条件均有回归锁定。

已完成 `300220000/300600000` Abyssal Splinter 同构族：神器普通/困难选择与 Dayshade 双 Boss 触发改由 Pattern 和条件出生接管，删除 3 个提前生成 Boss 的旧 AI；10 个真端技能槽不完整的 Boss 继续回退。碎片 `185000104` 无 `ownership_world` 且真端为 `remove_when_logout=FALSE`，共享 Handler 仅保留正常离图清理，断线恢复不删除。

已完成 `300520000/300630000` Dragon Lord's Refuge 同构族：Pattern、条件/静态出生负责阶段、Boss、传送和宝箱，Kahrun/计时专用 AI 保留已验证桥接，Handler 仅保留离图效果清理。

已完成 `301660000` Fallen Poeta：Pattern 和条件出生负责屏障、波次与 Boss；`164002346` 由 `ownership_world` 通用离图流程清理并在断线时保留，Handler 仅保留离图/登出效果清理。

已完成 `301200000` Nightmare Circus：条件/静态出生负责完整马戏团流程，Handler 仅保留离图效果清理。

已完成 `320100000` Fire Temple：Retail 静态出生负责命名变体，Handler 仅保留 Kromede 随机宝箱和提示。

已完成 `320130000` Adma Stronghold：Retail 条件/静态出生与 Pattern 负责阶段、Boss 和出口；七把钥匙无 `ownership_world` 且真端声明 `remove_when_logout=FALSE`，Handler 仅保留道具交互效果与正常离图清理，断线恢复不删除钥匙。

已完成 `320150000` Padmarashka's Cave 所有权审计：Retail 静态出生和 4 名守护者 Pattern 可达；`218672` 缺第 8 技能槽，Boss/蛋 Pattern 缺 `Phage`/`egg_die` 条件变量，继续明确回退专用 AI；Handler 保留持久化期限、解盾、蛋计数、Boss 完成和影片桥接。

已完成 `400030000` Transidium Annex 所有权审计：八个 `297331..297334/297472..297475` 载具由 Pattern 按阵营 tribe 校验、使用唯一技能槽并自销毁；`277225..277228` 是仅有 `20378/20381/20383` 战斗技能的炮台，不存在旧 `21652` 交互，故删除整个旧交互消费者。58 条 Retail 条件槽仍因缺初始化 NPC `297304` 不可达，且初始化 Pattern 缺 `NPC_TANK_A` 等变量、Boss Pattern 引用不存在的 `Pucio_al/Hnikar_a1`；Handler 继续负责其余 Panesterra 附楼流程，不猜测补造出生或变量。

已完成 `300040000` Dark Poeta 所有权审计：Retail 条件/静态出生、Pattern、分数和掉落负责数据化机制；Handler 保留出生页/阵营变量、计时与门、幂等击杀/采集计分、阶段变量、Marabata 控制器生命周期、影片、排名结算和退出恢复。

已完成 `300100000` Steel Rake 所有权审计：Retail 条件/静态出生、随机池、waypoint、Pattern 与掉落负责遭遇；Handler 仅保留 `214968` 死亡写 `IDSHULACKSHIP_PH_KILL` 的真端桥接和退出传送。

已完成 `300110000` Baranath Dredgion 所有权审计：Retail 条件/静态池、waypoint、Pattern、分数、掉落和任务数据负责 PvE 内容；Handler 保留准备/计时、变量桥接、Surkana 房间、阵营与 PvP/NPC 计分、复活、舰长结算、奖励和退出恢复。

已完成 `300170000` Beshmundir Temple 所有权审计：Retail 条件/静态出生与 Pattern 负责 Boss、门、波次和掉落；Handler 仅保留祭坛任务应答者、蓝焰门钥匙和离图副本道具清理三类无数据替代桥接。

已完成 `300210000` Chantra Dredgion 所有权审计：Retail 静态/条件出生、waypoint、Pattern、分数和掉落负责 PvE 内容；Handler 保留准备/计时、真端延迟传送器与 Named 出生、变量桥接、Surkana 房间、阵营与 PvP/NPC 计分、复活、舰长结算、奖励和退出恢复。

已完成 `300440000` Terath Dredgion 所有权审计：Retail 静态/条件出生、waypoint、Pattern、分数和掉落负责 PvE 内容；Handler 保留准备/计时、真端延迟传送器与 Named 出生、变量桥接、Surkana 房间、阵营与 PvP/NPC 计分、复活、舰长结算、奖励和退出恢复。

已完成 `301650000` Ashunatal Dredgion 所有权审计：Retail 静态/条件出生、waypoint、Pattern、分数和掉落负责 PvE 内容；Handler 保留准备/计时、真端延迟传送器与 Named 出生、变量桥接、Surkana 房间、阵营与 PvP/NPC 计分、复活、舰长结算、奖励和退出恢复。

已完成 `302200000/302300000` Dredgion Defense 同构族所有权审计：Retail 静态/条件出生与 Pattern 负责入侵计时、波次、目标和 S–F 排名状态；真端 `INSTANCE_UNIONMATCH (0x5c)` 进入路径读取可恢复实例数据，删除登出强制退出；Handler 仅保留玩家死亡协议、效果 `18290/18300` 清理和显式退出。

已完成 `300230000` Kromede's Trial 所有权审计：Retail 静态出生、Pattern、感知区和任务负责战斗、伤员、Boss 选择、宝库提示及终局影片/完成；`282093/282095` 遗物死亡后生成 `282085/282084`，由后者施放真端净化技能 `19274/19273` 后自销毁，旧 `19248/19247` Handler 桥接已删除；四件 `ownership_world` 道具由通用离图流程清理，三把无归属钥匙仅在正常离图由 Handler 清理，登出保留全部七项；Handler 另保留入场变身/影片/UI、石门删除和庄园区域影片/提示。

已完成 `300190000` Taloc's Hollow 临时状态边界：`164000137..164000139` 无 `ownership_world` 且真端为 `remove_when_logout=FALSE`，Handler 仅在正常离图清理三件道具，登出/离图继续清理效果与召唤物。

已完成 `301390000/301520000` Drakenspire Depths 地图族所有权审计：Retail 静态/条件出生与 Pattern 负责普通/任务波次、双子和 Beritra 流程；普通图 `855461` 因 waypoint 起点缺 pathname 继续明确回退，Handler 仅保留退出；任务图钥匙 `185000219` 真端为 `remove_when_logout=FALSE`，但本地归属为普通图 `301390000`，因此 Handler 仅在正常离图清理钥匙，并在登出/离图清理效果 `22778/22779`。

已完成 `300240000/300241000` Aturam Sky Fortress 正式/活动同构族所有权审计：Retail 静态/条件出生、waypoint 与 Pattern 负责遭遇和门变量；`164000163/164000202` 真端为断线保留且归属正式图，正式图由通用离图流程清理，活动图由 Handler 仅在正常离图清理，两图登出只清效果。

已完成 `300350000/300360000/300420000/300430000/300550000` PvP Arena 地图族所有权审计：普通 Chaos/Discipline 的 Retail 静态/条件/采集出生、Pattern 与共享 Handler 分别负责数据化演员、7 个非零 Pattern 计分对象和旧交互/飞行环桥接；Glory 复用条件出生引擎导入 17 个真端无条件随机池，按 `1/11/21/31/41` spawn page 持久化选择 Brax/Tog 与 Buff 对象，并保留两组全页面 Buff/Party 选择；Pattern 与共享 Handler 唯一消费 15 个池内非零计分 NPC，静态重复 `243675/243676/218757` 已删除。未导入的非池基础演员继续由旧静态文件负责，不猜测替换。

已完成 `300560000` Shugo Imperial Tomb 所有权审计：Retail 条件出生、waypoint 与 Pattern 负责 453 条波次条件和阶段计数，三个缺 Pattern 的开场对话由最小 StageStarter AI 写入 `Condition_S2/S3/S4`；`831095` 由 Pattern 按种族施放 `21094/21103`，删除旧 Handler 硬编码 `21096`。三枚本图 Tag 由通用流程在正常离图时清理、断线保留；无世界归属的 Shugo Coin 永久保留；Handler 只清效果和发送退出消息。

已完成 `300590000` Ophidan Bridge 所有权审计：Retail 静态控制器/普通 Boss、56 条条件出生、waypoint 与 Pattern 负责逃犯路线、中间 Boss、最终 Boss 变体、桥门和出口；Handler 删除重复的逃犯、中 Boss、最终 Boss和出口出生，仅保留四组尚无完整静态导入消费者的防御怪随机池及机会包/掉落桥接。

已完成 `300150000` Udas Temple 所有权审计：Retail 静态/条件出生、Pattern、掉落和静态门负责遭遇、Boss/传送进度与三把钥匙门；Handler 仅保留离图/登出清理 `185000083..185000085`。

已将 `300450000/300570000/301100000` Harmony、`301120000` Kamar 和 `301680000` Idgel Dome Landmark 的既有计分迁移结果写入细粒度覆盖表：Retail 数据负责已证明的演员/计分动作；Landmark 两项本图物品真端断线保留，普通离图由通用流程清理，显式退出仍由 Handler 清理；Handler 继续唯一负责各自的比赛生命周期、持久化、结算和恢复。

已完成 `300510000` Tiamat Stronghold 所有权审计：Retail 静态/条件出生、Pattern 与掉落负责阶段、波次、Boss、传送和宝藏；`701523` 为无 Pattern 的 `NoAction` 控制器，Handler 仅保留物品使用完成后开启门 `22`。

已完成 `300540000` Eternal Bastion 所有权审计：Retail 条件出生、91 个 NPC Party、Pattern、分值和掉落负责波次/演员/战斗/战利品；`701625/701922` 的 `IDF5_TD_AddWave_01` Pattern 已闭合技能 `21069`、变量 `Wave_Z2_S1`、消息和自销毁消费者，旧 Handler 的 `21065/21066` 变身桥接已删除；三项本图临时物品真端断线保留，普通离图由通用流程清理，Handler 的直接退出路径继续清理；Handler 另负责持久化计时攻防、分数与排名、完成/失败、结算、效果清理和退出恢复。

已完成 `300620000` Occupied Rentus Base 所有权审计：删除 Handler 重复的种族攻城武器出生、武器/补给交互和油桶/幻象死亡处理；Retail 条件出生与 Pattern 接管这些链。最终 Boss Pattern 的 waypoint 起点为空且缺 `Ariana4` 条件变量，门、完成影片/出口/Reian 和两个无 Pattern 任务物件继续保留旧桥接。

已完成 `301110000/301330000/301360000` Danuar Reliquary 同构族所有权审计：三图删除初始三名 Idean 的旧重复出生，仅保留真端唯一坐标；完整 clone/add Pattern 接管数据化演员。初始三 NPC、两阶段 Modor、15 分钟 deadline、完成、私有掉落、退出和恢复继续由 Handler/custom AI 唯一负责；缺技能槽、具名路径、区域或变量的条件链继续明确拒绝，不因 Pattern 可加载而覆盖阶段控制。

已完成 `301230000/301370000` Illuminary Obelisk 同构族所有权登记：Retail 条件出生、Pattern 和掉落负责波次、计时器、Boss、资源与活动箱；`164000289/164000290` 真端断线保留且归属正式图，正式图普通离开由通用流程清理，Infernal 图由 Handler 清理，两图 Handler 只保留显式退出清理/传送，登出不删除。

已完成 `301400000/301590000` IDSweep 同构族所有权登记：Retail 条件/静态出生、Pattern 和分值负责演员、波次与计分值；两图各七项临时物品真端断线保留且归属本图，由通用离图流程清理；共享 Handler 负责持久化准备/战斗/结算 deadline、门启动、幂等击杀计分、排名/奖励条件、玩家奖励、变身清理和重启恢复。

已完成 `301610000` Theobomos Test Chamber 所有权审计：Retail 静态/条件出生、Pattern 和掉落负责 Boss、变量、出口与战利品；Handler 仅保留 `220426` 死亡后生成 `806221` 奖励箱的持久化幂等桥接，并补齐 JVM 重启恢复。

已完成 `310050000` Aetherogenetics Lab 所有权登记：Retail 静态出生、Pattern、掉落和静态门负责遭遇、五把钥匙的 100% 掉落与对应门消费；Handler 仅保留离图/登出清理 `185000001..185000005`。

已完成 `302340000` Bastion of Souls 所有权登记：Retail 静态/条件出生、Pattern 和掉落负责遭遇、最终 Boss、钥匙、宝箱与出口；六项 `ownership_world` 临时物品由通用离图流程清理并在断线时保留，Handler 仅保留飞行环写 `statdown` 后传送/播放影片及效果清理。

已完成 `302330000` Kumuki Cave 所有权登记：Retail 条件/静态出生、路径和 Pattern 负责出生页、波次、计时器、影片与 Boss 链；修正 `164002390` 的错误世界归属，三项 `ownership_world` 物品由通用离图流程清理，无归属 `186000459` 仅在正常离图由 Handler 清理，登出保留四项物品；Handler 另保留 `703424` 消费钥匙 `185000295` 和效果清理。

已完成 `310110000` Theobomos Lab 所有权审计：Retail 静态出生、Pattern、技能区域和掉落负责基础演员、战斗与标准战利品；当前没有该世界的 Retail 条件出生，Handler 继续唯一负责随机宝箱、`700422/237247` 私有掉落、封印石限时、Watcher/Ifrit 顺序链、出口和持久化恢复。

已完成 `320080000` Draupnir Cave 所有权审计：Retail 条件/静态出生和 Pattern 负责模式、副官、普通/特殊 Boss、Akhal、中央控制室波次及种族入口；删除 Handler 的重复计数、出生、交互与袭击链，仅保留没有数据生产者的 `237276` 入场幻影、提示和持久化期限。

已完成 `301510000` Sealed Argent Manor 所有权审计：Retail 条件/静态出生、Pattern、分值和掉落负责演员、变量门、四种变身效果与战利品；删除 Handler 对 `701001..701004` 的重复施法和绕过 `teleport_01 == 3` 的静态 `731648`，保留持久化计时赛、职业 Boss、铁牢/Hetgolem、计分结算、奖励和恢复。

已完成 `301270000` Linkgate Foundry 所有权登记：Retail 静态出生、完整 Pattern 和掉落负责房间控制、普通战斗与战利品；`185000196` 的 `ownership_world` 与通用离图流程负责钥匙清理，真端登出保留。三种 Boss Pattern 均缺 `Boss_Die` 条件变量，继续明确回退。当前没有该世界的 Retail 条件出生，静态数据也不生产限时怪群和出口，因此 Handler 继续唯一负责 20 分钟怪群、预警/到期、Boss 完成出口和恢复。

已完成 `302000000` Smoldering Fire Temple 所有权登记：Retail 的 11 个变量、672 条条件出生、Pattern、分值和掉落负责普通/大师两页阶段、演员、战斗、分值与标准战利品；七件 `ownership_world` 副本道具由通用离图流程清理并在断线时保留，Handler 负责条件初始化/奖励变量桥接、持久化计时赛、幂等计分、门、三种族变身、私有掉落、结算奖励、效果清理和恢复。

已完成 `302100000` Fissure of Oblivion 所有权审计：Retail 静态出生、完整 Pattern 和分值负责基础演员、`door_open` 门后演员与分值；删除绕过门变量的静态 `245827`。`worldraid_on` 的实际开启 Pattern 缺 `shadow_kill`，三个条件槽继续明确拒绝；Handler 保留门变量桥接、持久化计时赛、幂等计分、完成、奖励、清理和恢复。

已完成 `301720000` Mirash Sanctuary 所有权审计：补齐 Pattern 源码明确写入的 `boss_die/doll_time/resurrect_set/resurrect_statue`，使娃娃布局、石碑、Boss 冰雹和奔跑事件的 138 条条件出生可达；恢复 `248389` 的真端 A40 路径并删除 7 组无条件静态重复演员。`248533` 真端出生没有 Pattern 所需 waypoint，继续明确拒绝；Handler 保留随机钥匙怪、延迟突袭波次、石像复活、`11333` 技能桥接、私有石头掉落和恢复。`164000531` 由通用所有权流程在正常离图时清理，断线保留。

已完成 `301130000` Sauro Supply Base 所有权审计：Retail 的 10 个变量、28 条条件和完整 Pattern 接管门区变体、`230857`、`230853 -> OBJ_GATE_SELECTION -> 730872`、战斗与重复系统消息；删除静态 `230857` 和 Handler 直生传送器/重复伏兵。真端门脚本 ID 与本地实体门 ID 不同，Handler 继续桥接并持久化九扇门、五段警报、随机钥匙怪/侧门、终局出口/私有奖励及恢复；四把 `ownership_world` 钥匙由通用离图流程清理，断线保留。

已完成 `301140000` Seized Danuar Sanctuary 所有权审计：真端终局 Boss 为三选一，本地删除三只静态重复并由 Handler 持久化选择；补齐 Pattern 内部变量 `Pr_reset01` 后，石棺、上升气流岩石和三名终局 Boss 均由 Pattern 接管，`cSetPortal == 3` 条件负责出口。三把 `ownership_world` 钥匙由通用离图流程清理并在断线时保留；Handler 保留可恢复的警报、种族守卫、四个无 Pattern 钥石交互和私有炮弹掉落。

已完成 `300300000/300320000` Crucible 团队/单人图临时物品边界：`186000124/186000125` 与 `186000134` 分别归属本图且真端断线保留；删除 Handler 的登出/普通离图重复清理，只保留不经过通用离图流程的显式退出清理，并移除跨图物品清理。

按风险和复用能力分批：

1. 同构仓库/单人 PvE：机制少，优先提取重复桥接。
2. 传统 PvE：逐 Boss、门、阶段、掉落和出口核对。
3. 计时/波次/Luna：统一使用 `InstanceRuntimeState`、`InstanceDeadlineScheduler` 和 `InstanceSettlementService`。
4. Dredgion/战场：核对准备期、目标、阵营计分、复活、超时和结算。
5. Arena/Tournament：核对回合、排名、协议与奖励账本。

每张图的最小验收清单：

- 入口与准入规则；
- 创建页和静态/条件出生无重叠；
- 关键 NPC 的具体 `supports(pattern, npc)`；
- 门、路径、区域和传送；
- 阶段变量、计时和恢复；
- 计分、掉落、奖励幂等；
- 离图、登出、销毁清理；
- `coverage.xml` 所有权说明和专项测试。

## 阶段 5：恢复、协议与多人验收

目标：证明“代码可恢复”在真实生命周期中行为正确。

- [ ] 对准备期、战斗期、结算期分别执行 JVM 强制终止和恢复。
- [x] 通过自动机制族验证 NPC Pattern 本地状态、条件变量、门、动态出生、积分和 deadline；真实 JVM 强杀仍由上一项单列。
- [ ] 对 5.8 客户端的进入、次数、匹配、阶段、积分和结算包建立完整黄金文件；当前已锁定匹配、阶段、冷却和阵营积分载荷。
- [ ] 按机制族完成 139 图 GM 验收，记录阻塞原因，不用 `//kill` 跳过关键状态链。
- [x] 对 Dredgion、战场、Arena 和 Luna 完成自动并发、重复结算、离线排队和重启重放测试；真实多客户端并发仍并入 139 图 GM 验收。

提交边界：恢复缺陷按共享根因提交；协议按包族提交；验收记录按地图族提交。

## 阶段 6：真端基线与运行环境

目标：避免正确副本机制被私服倍率和地图基础设施改变结果。

- [x] 将真端基础奖励与服务器倍率分层；`InstanceSettlementService` 保持真端表原值，默认与仓库运行配置中的副本相关倍率统一为 1，非副本商城、采集、制作、宠物等倍率继续由各自配置键隔离。
- [x] 核对副本掉落、任务 XP/基纳/AP/GP、NPC AP、Dredgion 和四类 Arena 奖励倍率；`RateConfigTest` 同时锁定缺省绑定和仓库配置均为 1，防止真端基础值再次被隐式放大。
- [x] 接入副本相关 ScalingDrop 数据；22 个 NPC 由紧凑数据单轨替代普通掉落，外层千分制、内层万分制且每组最多选择一项。真端 `dropMultiple` 使用 BoostTime 的普通/高等级值并以 `applyBoostTimeTable_MaxNpcLevel` 分流，玩家或队伍的 event drop multiplier 与等级修正随后作用于外层概率；原始 `rate=1000` 最终恢复为必掉，不能被等级差削减。MainServer `OverseasEventSystem` 另通过 `NS_VERSION` 下发 event drop percent，本仓库没有该生产者，明确不猜测或模拟该海外活动倍率。
- [x] 注入明确的 `SpecialServer_Cond` 环境，不依赖隐式默认值；`gameserver.instance.special_server_cond` 在实例初始化时写入已声明该变量的条件世界，并由配置/运行时测试锁定 `0/1` 取值。
- [x] 建立 GEO/PATH 覆盖基线：`RetailGeoPathCoverageTest` 锁定 137 个生产世界、137 个 `.geo.gz` 地图资源和 78 个含 `retail:<world>:` 路线的世界；当前 59 个世界没有路线定义，不能以资源存在宣告 PATH 实测完成。
- [x] 完成自动高密度 PATH 请求有界队列、飞行/水下三维绕障、动态门版本和实例销毁清理验收。
- [ ] 在生产 JVM 与 5.8 客户端完成高密度怪物追击、飞行和动态碰撞实机压测。

## 完成标准

- 139 张地图均有具体且可验证的行为所有者，`behavior_source` 不再只指向未经拆解的整份 Handler。
- 所有可达副本关键 NPC 不因可修复的数据/消费者缺口回退；保留回退均有真端失败语义或不可恢复证据。
- 可数据化的副本专用 AI 和 Handler 已删除；剩余 Java 桥接均记录输入、输出和删除条件。
- 准入、阶段、计分、奖励、退出和 JVM 恢复通过自动测试与 5.8 客户端验收。
- 真端基础值可在倍率 1 环境复现；非真端倍率被明确隔离。

## 每步提交模板

1. `test(...): lock <map/family> retail ownership`
2. `feat(data): restore <missing retail data>` 或 `fix(instance): align <mechanism>`
3. `refactor(instance): remove <proven legacy bridge>`
4. `docs(instance): record <map/family> ownership and acceptance`

不强制四个提交全部存在；没有独立价值的文档更新与该地图实现同提交，公共能力和跨地图数据必须单独提交。
