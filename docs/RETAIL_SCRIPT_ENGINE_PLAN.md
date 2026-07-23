# 真端脚本/事件执行层迁移规划

本文记录对应真端 `ScriptDLL64.dll` 的统一脚本/事件执行层奠基，以及后续阶段（点 2 完整数据驱动 Quest 运行时、点 4 逐副本移除 Java 桥接）的演进路线。

## 真端契约来源

真端恢复源码位于 `58Server/server58-source/MainServer_ScriptDLL64`：158,519 个函数、14 个类，核心契约类：

| 真端类 | 子系统 | 用途 |
| --- | --- | --- |
| `IAIScriptNpc` / `IAIScriptNpcImp` | NPC | 脚本 NPC 接口与实现，由数据实例化、引擎按事件回调 |
| `IOneQuestScriptNpc` | NPC | 单任务脚本 NPC |
| `SimpleHuntQuest` | Quest | 数据驱动狩猎任务 |
| `SimpleCollectQuest` | Quest | 数据驱动收集任务 |
| `SimpleTalkQuest` | Quest | 数据驱动对话任务 |
| `SimpleSerialHuntQuest` | Quest | 数据驱动连串狩猎任务 |
| `SimpleItemPlayQuest` | Item | 数据驱动物品游玩任务 |
| `SimpleUseItemQuest` | Item | 数据驱动使用物品任务 |

语义模型：**注册表（id → 工厂）+ 数据绑定 + 事件回调**。脚本 NPC 与任务均由数据实例化，引擎按 `onDialog` / `onKill` / `onItemUse` / `onEnterZone` / `onMovieEnd` 等事件回调，逻辑由数据驱动而非手写。

## 技术路线（已确认）

**纯 Java 数据驱动注册表，不引入 JNI / 不加载真端 `ScriptDLL64.dll`。**

理由：
- 真端 `IAIScriptNpcImp` 的 20,999 个方法在恢复源码中均为 `FUN_*` 未命名桩，无法直接抽取可执行语义。
- 真端可执行语义已由本项目的 `RetailPatternAI2`（AI 侧）与 `XMLQuest`（Quest 侧）用 Java 重新实现并数据驱动，且 AI Pattern 结构覆盖已达 98.88%。
- 引入 JNI 会带来本地依赖与跨平台风险，与当前纯 Java 架构不符。

因此点 1 的落地形态是：建立与真端同构的 Java 注册-回调抽象，把现有 AI Pattern 执行器与 XML Quest 执行器收拢到统一契约下，为逐副本、逐任务移除手写 Java 桥接提供落地层。

## 已奠基（本会话）

新增包 `com.aionemu.gameserver.scriptEngine`：

- `ScriptNpc.java`：脚本 NPC 回调契约接口，对应 `IAIScriptNpc`。声明 `onDialogStart` / `onDialogSelect` / `onSeePlayer` / `onLeavePlayer` / `onKilledByPlayer` / `onDied` / `onTimerEnd` / `onEnterZone` / `onLeaveZone` / `onMovieEnd` / `onItemUse`，均为 `default` 空实现，便于渐进迁移。
- `ScriptQuest.java`：数据驱动任务脚本契约接口，对应 `Simple*Quest` 族，持有完整任务处理器并暴露任务 ID。
- `ScriptRegistry.java`：按 NPC ID / 任务 ID 绑定 `ScriptNpc` / `ScriptQuest` 的注册表，对应真端脚本实例化表。
- `ScriptEngine.java`：`GameEngine` 实现，单例 + Spring `ObjectProvider` 模式，与 `AI2Engine` / `QuestEngine` / `InstanceEngine` 同构。`load()` 奠基阶段仅初始化空注册表并记录日志，零行为变化。

引擎接入（`lifecycle` 包）：
- `GameEngineServices`、`GameEnginesGateway`、`GameEnginesRuntimeBridge`、`GameEngineServiceFallbacks` 四处统一加入 `ScriptEngine` 的 provider 注入、解析方法与回退工厂。
- `GameEnginesGateway.engines()` 将 `ScriptEngine` 纳入启动期并行加载列表。

测试：
- `ScriptRegistryTest`：注册/查询/重复覆盖/null 忽略/clear 五项。
- `GameEnginesRuntimeBridgeTest`、`AI2EngineRetailSelectionTest`、`RetailConditionSpawnEngineTest` 同步更新构造参数并绿。

消息：`log.scriptEngine.loading` / `log.scriptEngine.loaded` / `log.scriptEngine.shutdown`（中英双语）。

## Quest 接管第一阶段（已实施）

- `QuestEngine` 继续加载现有 Java Handler 与 XML Quest；XML Quest 完成事件索引注册后，其处理器所有权移交给 `ScriptRegistry`。
- 所有任务事件通过统一的处理器解析入口优先查询 `ScriptQuest`，未迁移的任务回退原 Java `QuestHandler`。
- `ScriptQuest` 直接复用成熟的 XML `QuestHandler` 事件语义，不复制对话、击杀、物品、区域、影片等执行逻辑。
- 任务热重载和关闭会清空 Quest 脚本绑定，保留未来独立加载的 NPC 脚本绑定。
- `scripts/generate_retail_simple_quests.py` 以 `58Server/Map/XML/Quest_Simple*.xml`、`data_driven_quest.xml`、`quest.xml`、`npcs.xml`、`Items.xml` 及证据化恢复所需的 `58Server/server58-source/MainServer_ScriptDLL64/fun` 为唯一行为与 ID 来源，生成最后优先的 `zz_retail_simple_quests.xml`。
- 只生成当前基础任务中能被现有模板等价表达且 NPC/物品名称全部解析成功的任务；额外语义和未解析名称写入同名报告，继续保留旧 XML/Java 回退。
- 当前生成 5,028 个真端任务；49 个旧 XML 文件保留 726 条仍有独立语义的定义，统一由 `ScriptRegistry` 接管。生成器按单一所有权运行：现存 Java Handler 的任务不生成 XML；删除已证明可替代的 Handler 后，真端 XML 才接管。
- 当前仓库保留 841 个 Java Handler，其中 835 个属于当前 6,476 条基础任务；生成 XML、保留 XML 与 Java 所有权交集为 0，避免特殊 Java 逻辑被最后加载的生成 XML 静默覆盖。
- 真端 `quest.xml` 共 10,035 条，本项目基础任务 6,476 条，其中 6,462 条与真端相交。真端独有的 3,573 条中，3,406 条等级为 999；剩余 167 条已归类为 86 条字面测试任务、42 条已有隔离结论、38 条 DataDriven 内部测试和使用测试 NPC 的任务 9801，没有尚待直接发布的基础任务候选。
- 项目基础任务已达到 `6,476 / 6,476` 管理闭包：6,453 条有 XML/Java 可执行处理器，23 条正式隔离。隔离清单由生成报告维护：13 条仅有真端编译脚本但恢复源码尚无完整可执行语义，8 条不在 58Server 的任何 `quest.xml`，2 条为等级 999 停用任务。
- `DataDrivenQuest` 通用处理器严格支持真端 `Talk`（含步骤物品变更、指定推进对话和单影片）、`CollectItem`、`Hunt`、`ItemPlay`、`EnterArea`（感知 NPC）和 `EnterWorld` 步骤；任一字段、NPC、物品或数量无法完整解析时整条拒绝，不猜任务语义。
- 生成器报告强制给出 `base`、`executable`、`isolated`、`managed`，其中 `managed` 必须等于基础任务总数；23 条隔离任务不注册空处理器，避免把“有占位”误报成“可执行”。
- 报告同时强制给出 `ownership.generated_xml`、`ownership.java_handlers` 和 `ownership.overlap`；`overlap` 非 0 时生成直接失败。

### ScriptDLL 证据恢复首批（2026-07-24）

恢复 `19079/19080/19081` 与 `29079/29080/29081` 六条突破石买入任务。该批不依据社区实现猜测，而由以下真端证据共同闭合：

- `quest.xml` 明确六条任务均检查并扣除 1 个 `exceed_enchant_key_01`，奖励金币分别为 `1,140,000,000 / 1,600,000,000 / 2,020,000,000`，并区分天/魔阵营。
- `npcs.xml` 唯一解析六个专用 buyer NPC：`805716/805720/805722` 与 `805717/805721/805723`；`Items.xml` 唯一解析物品 `166500002`。
- ScriptDLL 恢复源码把六个任务 ID 分别绑定到 `LC1_L_exceed_key_buyer[_02/_03]` 和 `DC1_D_exceed_key_buyer[_02/_03]`，并包含任务状态建立、库存数量检查、扣除、结算和状态重置调用。
- 生成器对 `quest.xml` 字段与 `fun_611/620/687/688/690/691/695/697/868/869/880/882.cpp` 的关键 token 设置硬门；任一证据缺失即拒绝生成并继续隔离。
- 运行时复用 `ItemCollecting`，且不设置 `item_id`（该字段仅用于接取时发放的工作物品）；实际检查与扣除严格读取 `quest_data.xml` 的 `collect_items`。不新增 Java Quest Handler。

### ScriptDLL 证据恢复第二批（2026-07-24）

恢复 `80761/80766` 两条限时烟花支援任务。真端证据闭合如下：

- `quest.xml` 明确两条均为每日重复任务、分属天魔阵营并奖励 10 GP；同步修正 `quest_data.xml` 中 `80766` 漂移的 `3 GP`。
- `npcs.xml` 唯一解析天族 `833648 → 702947 → 702948` 与魔族 `833650 → 702950 → 702951` 的征服者、烟花箱 01、烟花箱 02 顺序。
- ScriptDLL 恢复源码在 `fun_630/694/701/705/708/869/875/885/894/897.cpp` 中闭合 NPC 绑定、阶段 `0 → 1 → 2`、对话 `1011 → 1352 → 1693`、120 秒计时、超时回退到阶段 0 和征服者奖励入口。
- 运行时复用 `ReportToMany`，仅增加可选计时配置；未配置计时的既有任务行为不变，不新增 Java Quest Handler。任一字段或函数块 token 漂移时生成器重新隔离整条任务。

### ScriptDLL 证据恢复第三批（2026-07-24）

恢复 `30503/30553` 两条提亚马兰塔之眼碎石救援任务。真端证据闭合如下：

- `quest.xml` 与 `quest_data.xml` 明确两条任务的天魔阵营、经验、不同金币以及各自 6 组可选食物奖励；`Items.xml` 唯一解析全部 12 个奖励物品 ID。
- `npcs.xml` 唯一解析 `Lition(205438)`、`IDYun_Debris_Q30503(701097)` 与 `Rodelion(799541)`；ScriptDLL 在 `fun_619/624/626.cpp` 将三者绑定到两条任务。
- ScriptDLL 的阶段注册和回调绑定进一步证明：Lition 负责阶段 0 接取和阶段 4 交付；碎石仅在任务阶段 0 可交互并执行删除，不推进阶段；Rodelion 对话才进入任务完成状态。生成器对 `fun_656/661/667/668/692/698/703/704/877/878/882/888/893.cpp` 设置函数块硬门，任一证据漂移即重新隔离。
- 运行时继续复用 `DataDrivenQuest`，仅为已有 `action_ids` 增加默认关闭的 `delete_action_target` 属性；未配置的既有任务行为不变，不新增 Java Quest Handler。
- 镜像后续任务 `30504/30554` 仍隔离：其石柱交互会推进到阶段 1，并在进入世界 `300280000` 时回退阶段 0，当前通用模板尚不能无损表达这组复合语义。

### ScriptDLL 证据恢复第四批（2026-07-24）

恢复天族 `3219/3220` 与魔族 `4219/4220` 四条钢铁钩爪号隐藏钥匙任务。真端证据闭合如下：

- `quest.xml` 与 `quest_data.xml` 明确四条任务的阵营、十个钥匙物品、五个掉落 NPC、每项数量 1、100% 概率及队员独立掉落；`Items.xml` 和 `npcs.xml` 唯一解析对应名称与 ID。
- ScriptDLL `fun_873.cpp` 证明四条任务在进入世界 `300100000` 时创建，离开时清理任务状态。生成器按包含任务 ID 的单个函数块验证世界进入和离开调用链，任一证据漂移即重新隔离。
- 运行时复用 `DataDrivenQuest`，仅增加 `WORLD_ACTIVE` 启动方式：任务在钢铁钩爪号内保持启动，使 `quest_data.xml` 的任务掉落生效，离开时通过正式放弃流程清理；无步骤、交付 NPC 或奖励状态，不新增 Java Quest Handler。

### ScriptDLL 证据恢复第五批（2026-07-24）

恢复天族 `19678/19679` 与魔族 `29678/29679` 四条装备进化成长任务。真端证据闭合如下：

- `quest.xml` 与 `quest_data.xml` 明确四条任务的阵营、66 级门槛、30 天冷却、接取库存条件、6 亿经验和进化材料奖励；`Items.xml` 唯一解析阵营硬币与两种奖励材料。
- `npcs.xml` 唯一解析天族 Rena `806698` 与魔族 Melrania `806700`；ScriptDLL 闭合 NPC 绑定、阶段 0 接取、获得奖励材料时阶段 `0 → 1`、阶段 1 回原 NPC 结算。生成器逐函数块验证 NPC、任务 ID、物品 ID、回调和状态转换，任一证据漂移即重新隔离。
- 运行时复用 QuestEngine 已有的获得物品事件，仅为 `DataDrivenQuest` 增加 `GET_ITEM` 步骤注册与推进；不新增 Java Quest Handler。

### ScriptDLL 证据恢复第六批（2026-07-24）

恢复天族 `3959` 与魔族 `4963` 两条 Epilogue 感知自动结算任务。真端证据闭合如下：

- `quest.xml` 明确两条任务分别要求完成 `1099/2099`，均为 50 级、单次、无经验和金币奖励；`quest_data.xml` 同步补齐对应前置任务条件。
- `npcs.xml` 唯一解析 `LC1_SensoryArea_Q3959(206101)` 与 `DC1_SensoryArea_Q4963(206102)`，两者均已有正式出生点。ScriptDLL 闭合 NPC 绑定、感知回调、任务创建、状态推进和无对话自动结算。
- 生成器逐函数块验证 `fun_611/620/871/872/873/875/906.cpp` 中的任务、NPC、前置任务、回调和结算 token；任一证据漂移即重新隔离。
- 运行时仅为 `DataDrivenQuest` 增加 `SENSORY_COMPLETE` 启动方式，复用 QuestEngine 的感知事件和 `QuestService` 正式接取、奖励结算流程；无步骤、交付 NPC 或逐任务 Java Handler。

### ScriptDLL 证据恢复第七批（2026-07-24）

恢复天族 `30504` 与魔族 `30554` 两条 Paios 救援任务。真端证据闭合如下：

- `quest.xml` 与 `quest_data.xml` 明确两条任务分别要求完成 `30503/30553`，等级 60、单次，奖励经验、金币及三种可选卷轴；同步修正基础 XML 漂移的 57 级门槛。`Items.xml` 唯一解析三个奖励物品。
- `npcs.xml` 唯一解析 Lition `205438`、石柱 `701098` 与 Paios `799536`，且正式 `300280000/300620000` 出生表均包含三者。
- ScriptDLL 闭合 Lition 阶段 0 接取、石柱阶段 0 交互推进、Paios 阶段 1 结算，以及已推进任务重新进入世界 `300280000` 时回退阶段 0。生成器逐函数块验证 `fun_619/624/625/667/671/698/703/707/873/877/882/892/896.cpp`，任一 token 漂移即重新隔离。
- 运行时为 `DataDrivenQuest` 增加可推进的 `ACTION` 步骤和默认关闭的 `reset_world_id`；未配置任务行为不变，不新增逐任务 Java Handler。

### Java Handler 所有权收口（2026-07-25）

- 删除 1,476 个已由严格真端生成器完整承接的手写 Handler；生产生成 XML 与剩余 Java Handler 的任务 ID 交集由 1,464 降为 0。
- 剩余 889 个 Java Handler 均不在严格生成集合中（883 个属于当前基础任务）。其中 792 条包含非线性事件或影片、传送、实例、定时器、技能、动态物品、对象交互、继承逻辑、动态奖励、出生等副作用；91 条表面仅含对话/击杀且无已识别副作用，但真端 ScriptDLL 的启动、目标、阶段、完成与奖励调用链尚未闭合，因此继续保留 Java，不能按表面形状猜测迁移。
- 生成器以文件存在性作为所有权门：Java Handler 存在则保留 Java；经证据审计删除后才生成真端 XML。无需维护第二份任务白名单。
- `1868/2868` 与 `13962/23962` 的迁移测试改为直接审计生产 XML，分别锁定固定区域顺序和真端唯一感知目标，避免测试继续依赖已删除类。
- `1888/2888` 的真端阶段会重复回到同一接取 NPC；生成器仅对这种显式重复顺序改用现有 `DataDrivenQuest` 线性步骤，保持五段对话顺序，不扩展运行时。
- `17160/17161/27160/27161` 的 `_challengetask_` 是无 NPC 接取哨兵；统一生成 `start_npc_ids="0"` 的 `MonsterHunt`，目标 ID、合计 10 次击杀和交付 NPC 均由真端 NPC 数据唯一解析。
- `15690/25690` 继续保留 Java：真端任务使用逻辑 NPC 名 `ld_rw_npc_gd5001`，但 `npcs.xml` 没有它到 `806696/806697` 的唯一绑定。`17505/27505` 仍包含影片副作用，`18738/28738` 仍包含世界限制、十个炸弹发放及十次使用计数，均不得按表面阶段强行迁移。

### ScriptDLL 证据恢复第八批（2026-07-25）

- 恢复住宅引导任务 `18806/28806`。`quest.xml` 与 `quest_data.xml` 闭合 21 级、单次、阵营、前置 `18831/28831` 和 12,951 经验奖励，且两条任务均无物品、影片、传送或副本副作用。
- `npcs.xml` 将 `HousingManager_Li/Da` 分别唯一解析为五个住宅管家 `810017..810021` 与 `810022..810026`，并唯一解析中间 NPC `Pesarius(830528)/Prakon(830530)` 和结算 NPC `Kaionen(830194)/Katenon(830211)`。
- ScriptDLL 闭合三个逻辑 NPC 绑定、阶段 `0` 接取、阶段 `0` 中间对话、阶段 `1` 结算以及两个标准 Talk 回调；生成器逐函数块验证 `fun_351/353/361/362/543/554/564/586/805/813/839/849/857.cpp`，任一证据漂移即拒绝生成。
- 运行时直接复用 `DataDrivenQuest` 的单段 Talk，不新增 DSL、运行时原语或逐任务 Java Handler。

### ScriptDLL 证据恢复第九批（2026-07-25）

- 恢复卡多尔枯树任务 `13809/23809`。`quest.xml` 与 `quest_data.xml` 闭合 65 级、单次、阵营、三件任务工作物品、检查物品、3,446,553 经验、150,660 金币及两种二选一奖励。
- `npcs.xml` 将天族/魔族接取与结算 NPC 唯一解析为 `802427/802429`，三个交互目标唯一解析为 `730969/730970/730971`；`Items.xml` 同时锁定六件阵营工作物品和两个可选奖励模板。
- 每条任务恰有 15 个 ScriptDLL 函数块，只包含四个 NPC 绑定、阶段 `0→1→2→3→奖励`、接取回调和四个对象交互回调。生成器验证全部函数块及调用数量，任一目标、物品、阶段或回调漂移即拒绝生成。
- 运行时复用三个有序 `ACTION` 步骤，删除两条 `useQuestObject` Java Handler；基础 `quest_data.xml` 继续负责工作物品与奖励，不新增运行时原语。

### ScriptDLL 证据恢复第十批（2026-07-25）

- 恢复卡多尔抵达引导 `13800/23800`。`quest.xml` 与 `quest_data.xml` 闭合 65 级、单次、阵营、一件任务工作物品、3,446,553 经验、150,660 金币及两种二选一奖励。
- `npcs.xml` 将天族链路唯一解析为 `804699 → 804782 → 802431`，魔族链路唯一解析为 `804719 → 804753 → 802433`；`Items.xml` 同时锁定阵营工作物品和奖励模板。
- 每条任务恰有 13 个 ScriptDLL 函数块，只包含三项 NPC 绑定、阶段 `0→1→2→奖励`、接取、三段对话和完成回调。生成器验证函数块数量、调用分布与所有数据字段，任一漂移即拒绝生成。
- 运行时复用现有单段 `TALK` 步骤，删除两条 Java Handler；任务工作物品和奖励继续由基础 `quest_data.xml` 驱动。

### ScriptDLL 证据恢复第十一批（2026-07-25）

- 恢复天族 `15542` 与魔族 `25542` 两条 Coalescence 引导任务。`quest.xml` 与 `quest_data.xml` 闭合 66 级、单次、阵营、前置 `15550/25550`、26,524,800 经验及物品 `165060002 × 1` 奖励。
- `npcs.xml` 将 `LF6_Felen_E/DF6_Edorin_E` 唯一解析为 `806074/806078`；ScriptDLL 闭合 NPC 绑定、阶段 0 接取、阶段 4 奖励、接取与奖励对话、完成清理及两端触发。生成器逐函数块验证 `fun_614/623/690/692/696/698/874/880/882/886/887/904/905/907/908.cpp`，任一 token 漂移即拒绝生成。
- 运行时为 `DataDrivenQuest` 增加默认关闭的 `complete_on_start`，仅在 Talk 接取成功后进入奖励状态；删除两条 Java Handler，奖励继续由基础 `quest_data.xml` 驱动。

### ScriptDLL 证据恢复第十二批（2026-07-25）

- 恢复天族 `18036` 与魔族 `28036` 两条永恒堡垒影片引导任务。`quest.xml` 与 `quest_data.xml` 闭合 65 级、单次、阵营、前置 `13305/23305` 和 1,723,277 经验奖励，且无物品奖励。
- `npcs.xml` 唯一解析天族 `LDF5b_Demades_E(801281) → LDF5b_IDLDF5b_TD_Drakan_Fighter(802008)` 与魔族 `LDF5b_Latkel_E(801280) → LDF5b_IDLDF5b_TD_Drakan_Fighter_Da(802015)`。ScriptDLL 闭合阶段 `0 → 3 → 4`、影片 `0x1c`、起始/中间/奖励对话、事件触发和奖励入口；生成器逐函数块验证 `fun_442/620/691/697/703/806/882/887/892.cpp`，任一 token 漂移即拒绝生成。
- 运行时复用 XMLQuest 已有的 `movie` 属性：Talk 步骤确认时播放影片并推进奖励状态，不新增影片 DSL；删除两条 Java Handler。

### ScriptDLL 证据恢复第十三批（2026-07-25）

- 恢复天族 `1422` 与 `1423` 两条纯对话任务。`quest.xml` 与 `quest_data.xml` 分别闭合奖励、工作物品、等级、阵营和单次属性；`npcs.xml` 唯一解析 `Memnes(203912) → Laokones(203731) → Memnes(203912)` 与 `Marana(203983)`。
- ScriptDLL 对 `0x58e/0x58f` 逐函数块闭合 NPC 注册、阶段 `0 → 3 → 4`、标准对话回调、奖励回调和任务触发；生成器验证 `fun_358/359/541/555/569/589/817/850/855.cpp` 及 `fun_624/692/703/716/806/878/883/893/905/909.cpp`，任一字段或证据漂移即重新隔离。
- 两条任务统一生成现有 `report_to/report_to_many`，不新增运行时原语；删除两条 Java Handler。

### ScriptDLL 证据恢复第十四批（2026-07-25）

- 恢复天族 `1131` 物品交接任务。`quest.xml`、`Quest_SimpleTalk.xml` 与 `quest_data.xml` 闭合等级、阵营、奖励、检查物品和两个工作物品；`npcs.xml` 唯一解析 `Hyacinte(203097) → Shugo_LF1a_01(799093) → Nadaelo(203101)`。
- ScriptDLL `0x46b` 函数块闭合阶段 `0 → 3 → 4`、三个 NPC 注册、标准对话回调以及接取/中间物品发放和扣除路径；生成器验证 `fun_351/360/364/545/555/564/590/813/841/850/854.cpp`，任一字段、物品或证据漂移即重新隔离。
- 生成现有 `DataDrivenQuest` 的单段 Talk，保留接取发 `quest_1131a`、中间扣除并发 `doc_quest_1131b`；不新增运行时原语，删除一条 Java Handler。

### ScriptDLL 证据恢复第十五批（2026-07-25）

- 恢复天族 `1987` 仓库扩充任务。`quest.xml` 与 `quest_data.xml` 闭合 29 级、400 技能点、2 格仓库扩充、经验奖励、阵营和单次属性；`npcs.xml` 唯一解析 `Fasimedes(203700) → Bustant(203749)`。
- ScriptDLL `0x7c3` 函数块闭合阶段 `0 → 4`、NPC 注册、接取/交付对话和奖励入口；生成器验证 `fun_611/615/690/695/881/890/904.cpp`，任一字段或证据漂移即重新隔离。
- 任务统一生成现有 `report_to`，不新增运行时原语；删除一条 Java Handler。

### ScriptDLL 证据恢复第十六批（2026-07-25）

- 恢复天族 `1469` 寻找 Denlavis 任务。`quest.xml`、`Quest_SimpleTalk.xml` 与 `quest_data.xml` 闭合等级、阵营、单次属性、任务工作物品和全部奖励；`npcs.xml` 唯一解析 `Hagne(790004) → TreasureGuardianQ_36_Ae(212878) → Hagne(790004)`。
- ScriptDLL `0x5bd` 函数块闭合两个 NPC 注册、阶段 `0 → 3 → 4`、接取/中间对话和奖励回调；生成器验证 `fun_350/368/547/564/584/812/843/849/857.cpp`，任一字段、NPC、物品或函数证据漂移即重新隔离。
- 任务生成现有 `DataDrivenQuest` 的单段 Talk，接取发放 `quest_1469a`，中间不伪造物品扣除；不新增运行时原语，删除一条 Java Handler。

### ScriptDLL 证据恢复第十七批（2026-07-25）

- 恢复镜像任务 `16979/26979`。`quest.xml`、`Quest_SimpleTalk.xml` 与 `quest_data.xml` 闭合 61 级、单次、不可共享、阵营、影片 `886/887`、简易接取动作 `20000`、2,807,767 经验和 133,740 金币；`npcs.xml` 唯一解析两组专用接取与交付 NPC。
- ScriptDLL `0x4253/0x6963` 函数块闭合起始/交付 NPC 注册、阶段 `0 → 3 → 4`、简易接取和奖励回调；生成器验证 `fun_352/354/539/565/587/813/835.cpp`，任一字段、NPC、阶段或回调证据漂移即重新隔离。
- 两条任务生成无 `item_id` 的现有 `ItemCollecting`。共享模板补齐 `ACCEPT_QUEST_SIMPLE` 成功接取后的影片播放，保留原有 1012 对话影片路径；删除两条 Java Handler，不新增模板类型或逐任务桥接。

### ScriptDLL 证据约束索引（2026-07-26）

- 生成器对 82 条 `retained_authoritative_gap` 的十六进制任务 ID 建立单遍 ScriptDLL 索引：每个 `fun_*.cpp` 只读取一次，并在逐任务报告中记录 `quest_hex`、命中文件、出现次数、相同文件集合分组和约束处置。
- 80 条存在任务 ID 命中，标记为 `evidence_audit`；`13945/23945` 在全部恢复源码中零命中，自动标记为 `blocked_no_script_hit`，不得仅凭 Java 表面同构迁移。
- 相同命中文件集合当前形成 `30708/30758`、`18213/28213`、`30702/30752` 三组高信号候选。该分组只决定审计顺序；启动触发、NPC/目标、阶段/数量、完成条件和奖励入口仍须在单个函数块中逐项闭合。
- 本阶段不改变任务执行所有权：生成 XML 4,970、保留旧 XML 745、基础 Java Handler 874、所有权交集 0，管理闭包保持 6,476/6,476。

### ScriptDLL 证据恢复第十八批（2026-07-26）

- 恢复提亚马特要塞镜像任务 `30708/30758`。`quest.xml`、`quest_data.xml` 与 `Items.xml` 闭合等级、阵营、单次属性、7,086,913 经验和 `tiamat_coin_01(186000201) × 1` 奖励；`npcs.xml` 唯一解析接取 NPC `800369`、交付 NPC `800438` 和三个击杀目标 `800425/800426/800427`。
- ScriptDLL `0x77f4/0x7826` 函数块闭合五个 NPC 注册、阶段 `0 → 4`、简易接取、三个目标任意合计 5 次击杀、计数完成和奖励回调；生成器验证 `fun_618/691/697/866/878/881/887.cpp`，任一目标、数量、阶段或回调漂移即重新隔离。
- 两条任务生成现有 `MonsterHuntData`。旧 Java Handler 错绑单一目标 `219356`，现已按真端三目标语义删除；不新增运行时类型或逐任务桥接。

### ScriptDLL 证据恢复第十九批（2026-07-26）

- 恢复第三训练所镜像任务 `18213/28213`。`quest.xml`、`Quest_SimpleItemPlay.xml`、`quest_data.xml` 与 `Items.xml` 闭合前置任务、阵营、两件任务物品、接取发放 A、第二次对话以 A 换 B、使用 B、2,010,386 经验、92,940 金币和竞技场徽章 `186000137 × 100`。
- ScriptDLL `0x4725/0x6e35` 函数块闭合三 NPC 注册、阶段 `0 → 1 → 2 → 3 → 4`、物品使用触发/完成回调、接取与奖励入口；生成器验证 `fun_336/337/338/339/340/802/803/872.cpp`，任一 NPC、物品、阶段、对话数据或回调漂移即重新隔离。
- 两条任务复用现有 `DataDrivenQuest` 的 `TALK → TALK → ITEM_PLAY` 链；删除旧 Java Handler，其中魔族 Handler 的注册 NPC 还与实际交付 NPC 不一致。不新增运行时类型或逐任务桥接。

### ScriptDLL 证据恢复第二十批（2026-07-26）

- 恢复提亚马特要塞镜像任务 `30702/30752`。`quest.xml`、`Quest_SimpleHunt.xml`、`quest_data.xml` 与 `Items.xml` 闭合专用接取/对象/目标/交付 NPC、对象对话后击杀 `219354 × 1`、7,086,913 经验和 `potion_hp_mp_50a(162000050) × 34` 奖励。
- ScriptDLL `0x77ee/0x7820` 函数块闭合四 NPC 注册、高位起始阶段、对象交互后阶段清零、单次击杀计数完成和奖励入口；生成器验证 `fun_163/301/305/317/328/766/780/787/792/800.cpp`，任一目标、数量、阶段或回调漂移即重新隔离。
- 两条任务复用现有 `DataDrivenQuest` 的 `TALK → HUNT` 链；共享 TALK 步骤只补充接受 `USE_OBJECT` 对话入口，以承载真端功能对象交互，不新增模板类型或逐任务桥接，删除两条旧 Java Handler。

### ScriptDLL 证据恢复第二十一批（2026-07-26）

- 恢复特拉斯德雷德奇安镜像任务 `30600/30610`。`quest.xml`、`Quest_SimpleSerialHunt.xml`、`quest_data.xml` 与 `Items.xml` 闭合 56–60 级、阵营、单次属性、中间对话、两名导航官任意合计击杀 1 次、Boss 击杀 1 次、6,775,529 经验、577,800 金币、2,000 AP 和奖励包 `188051598 × 1`。
- `npcs.xml` 唯一解析两组接取/交付 NPC `Hejitor(800325)/Astella(800327)`、中间 NPC `Linocus(800324)/Aluna(800326)` 与三个共享目标 `219256/219257/219264`。ScriptDLL `0x7788/0x7792` 函数块闭合 NPC 绑定、阶段 `0 → TALK → HUNT → HUNT → 4`、两段单次计数推进和接取/对话/进度/奖励入口；生成器验证 `fun_340/341/342/804.cpp`，任一字段或 token 漂移即重新隔离。
- 两条任务复用现有 `DataDrivenQuest` 的 `TALK → HUNT → HUNT`，不新增运行时原语；删除仅处理 Boss 且错误绑定接取/中间 NPC 的两个旧 Java Handler。

### ScriptDLL 证据恢复第二十二批（2026-07-26）

- 恢复冬季活动镜像任务 `50008/51008`。`quest.xml`、`quest_data.xml` 与 `Items.xml` 闭合 9 级、每日重复、不可共享、阵营、奖励糖果 `160010203 × 1` 和活动币 `186000177 × 4`；`npcs.xml` 唯一解析主城 Santa `831032/831033`、配送员 `219290/219291` 和住宅感知区 `206234/206235`。
- ScriptDLL `0xc358/0xc740` 的全部命中闭合主城 Santa 阶段 0 接取与阶段 4 交付、阵营配送员两次击杀计数和完成调用、接取/奖励对话；住宅感知区只有任务关联绑定，没有阶段或回调，因此不伪造运行时触发。生成器验证 `fun_611/616/620/690/691/695/697/878/880/882/885/887.cpp`，任一字段、NPC、数量或函数块 token 漂移即重新隔离。
- 两条任务生成现有 `MonsterHuntData`，同一 Santa 接取和交付；删除错误使用住宅 Santa/Sugo NPC 的两个旧 Java Handler，不新增运行时原语。

### ScriptDLL 证据恢复第二十三批（2026-07-26）

- 恢复魔族专家奖励任务 `2985`。真端 `quest.xml` 与 `quest_data.xml` 闭合 29 级、不可共享、魔族、任意制作技能 400 点、291,412 经验和扩展背包 2 格；同时为已迁移的天族镜像任务 `1987` 补回相同的制作技能门槛。
- `npcs.xml` 唯一解析 Vidar `204052` 与 Roskva `204072`；ScriptDLL `0xba9` 函数块闭合阶段 `0/3/4`、七条专家任务状态分支、接取/奖励/完成入口。恢复源码中的字符串指针 `DAT_18124e758` 经原始 `ScriptDLL64.dll` 偏移 `0x124cf58` 验证为 UTF-16 `Roskva`，因此交付 NPC 不依赖旧 Java 推断。
- 任务复用现有 `ReportToData`，制作技能条件继续由 `QuestService` 读取 `QuestTemplate` 执行；删除旧 Java Handler，不新增运行时原语。生成器验证 `fun_626/630/694/699/717/884/890/906.cpp`，任一任务字段、NPC、阶段、前置分支或结算入口漂移即重新隔离。

### ScriptDLL 证据恢复第二十四批（2026-07-26）

- 恢复因吉森任务 `11010`。真端 `Quest_SimpleTalk.xml`、`quest.xml`、`quest_data.xml`、`npcs.xml` 与 `Items.xml` 闭合 `Pucio(798931) → Naiting(799071) → Lionel(798906) → LF4_FOBJ_Q11010A(730323) → Naiting`、对象步骤发放 `quest_11010a(182206713) × 1`，以及经验、金币和 `coin_06 × 6` 奖励。
- ScriptDLL `0x2b02` 的全部 15 个命中闭合四个对象绑定、阶段 `0 → 1 → 2 → 3 → 4`、接取与四段对话入口；生成器验证 `fun_357/358/360/362/543/555/557/558/570/590/819/838/850/852/853.cpp`，任一 NPC、物品、阶段或入口漂移即重新隔离。
- 任务复用现有 `DataDrivenQuest` 三段 `TALK`，功能对象继续使用已支持的 `USE_OBJECT` 对话入口；只让现有 compiled simple-talk 配置接受显式步骤列表，删除旧 Java Handler，不新增运行时原语。

### ScriptDLL 证据恢复第二十五批（2026-07-26）

- 恢复贝勒斯兰任务 `2512`。真端 `Quest_SimpleTalk.xml`、`quest.xml`、`quest_data.xml`、`npcs.xml` 与 `Items.xml` 闭合 `Loki(204703) → Gigrite(204801) → Kistenian(204753)`、接取发放 `quest_2512a(182204411) × 2`、中间步骤扣除 `× 1`，以及经验、金币、恢复药水和深渊铸币奖励。
- ScriptDLL `0x9d0` 的全部 11 个命中闭合三个 NPC 绑定、阶段 `0 → 1 → 4`、接取、中间对话和奖励入口；生成器验证 `fun_350/353/358/536/554/569/586/817/832/849/855.cpp`，任一 NPC、物品数量、阶段或入口漂移即重新隔离。
- 任务复用现有 `DataDrivenQuest` 单段 `TALK` 的接取发放与步骤扣除能力；删除旧 Java Handler，不新增运行时原语。成功接管的 compiled simple-talk 同时从通用 `unsupported/invalid` 报告中回扣，避免把已生成任务误报为阻塞。

### ScriptDLL 证据恢复第二十六批（2026-07-26）

- 恢复沃特伦任务 `1218`。真端 `Quest_SimpleTalk.xml`、`quest.xml`、`quest_data.xml`、`npcs.xml` 与 `Items.xml` 闭合 `Taiotus(203121) → Shugo3(798004) → Une(203172)`、Shugo3 步骤发放 `doc_quest_1218a(182200566) × 1`、60,300 经验奖励及后续任务 `1219` 依赖。
- ScriptDLL `0x4c2` 闭合三个 NPC 绑定、阶段 `0 → 1 → 4`、任务 ID 分派、接取、中间对话与完成入口；恢复源码中的 `DAT_181249090/DAT_18133a410` 经原始 `MainServer/ScriptDLL64.dll` 文件偏移 `0x1247890/0x1338c10` 验证为 UTF-16 `Shugo3/Une`。生成器验证 `fun_363/366/368/545/556/573/595/719/821/840/851/859.cpp`，任一证据漂移即重新隔离。
- 任务复用现有 `DataDrivenQuest` 单段 `TALK` 的步骤发放能力；任务工作物品由 `quest_data.xml` 在结算时统一检查和扣除，删除旧 Java Handler，不新增运行时原语。

### ScriptDLL 证据恢复第二十七批（2026-07-26）

- 恢复沃特伦任务 `1220`。真端 `Quest_SimpleTalk.xml`、`quest.xml`、`quest_data.xml`、`npcs.xml` 与 `Items.xml` 闭合完成 `1219` 后由 `Une(203172) → Shugo3(798004) → shugo_Lender_LF2_01(205240)`，接取发放 `quest_1220a(182200568)`，中间步骤扣除 A 并发放 `quest_1220b(182200569)`，以及经验、金币和强化粉末奖励。
- ScriptDLL `0x4c4` 闭合三个 NPC 绑定、阶段 `0 → 1 → 4`、任务 ID 分派、启动触发、接取、中间对话与奖励入口；复用已由原始 DLL 验证的 `Shugo3/Une` 字符串指针。生成器验证 `fun_363/368/374/545/556/574/600/719/755/822/840/851/856.cpp`，任一字段、前置任务、物品数量、阶段或入口漂移即重新隔离。
- 任务复用现有 `DataDrivenQuest` 单段 `TALK` 的接取发放、步骤扣除和步骤发放能力；compiled simple-talk 证据门新增可选 `finished_quests` 精确校验，删除旧 Java Handler，不新增运行时原语。

### ScriptDLL 证据恢复第二十八批（2026-07-26）

- 恢复艾特南任务 `1483`。真端 `Quest_SimpleTalk.xml`、`quest.xml`、`quest_data.xml`、`npcs.xml` 与 `Items.xml` 闭合 `Shugo_LF2_13(798126) → Herodes(203940) → Ernia(203944) → Shugo_LF2_14(798127)`，前两段分别发放 `quest_1483a(182201401)` 和 `quest_1483b(182201402)`，以及经验和铸币奖励。
- ScriptDLL `0x5cb` 闭合四个 NPC 绑定、阶段 `0 → 1 → 2 → 4`、任务 ID 分派、接取、三段对话与完成入口；生成器验证 `fun_348/351/364/537/554/558/572/593/719/820/833/849/852/858.cpp`，任一字段、NPC、物品、阶段或入口漂移即重新隔离。
- 任务复用现有 `DataDrivenQuest` 两段 `TALK` 的步骤发放能力；结算继续由 `quest_data.xml` 检查和扣除两件工作物品，删除旧 Java Handler，不新增运行时原语。候选 `2767` 因真端等级 99 与当前基础数据等级 45 冲突而保留 Java，未用已闭合的调用链掩盖权威元数据缺口。

### ScriptDLL 证据恢复第二十九批（2026-07-26）

- 恢复艾特南后续任务 `1484`。真端 `Quest_SimpleTalk.xml`、`quest.xml`、`quest_data.xml`、`npcs.xml` 与 `Items.xml` 闭合完成 `1483` 后由 `Shugo_LF2_14(798127) → Anasya(204045) → Telamone(204048) → Sandinas(204011) → Shugo_LF2_13(798126)`，三段分别发放 `quest_1484a/b/c(182201403/4/5)`，以及经验、金币和铸币奖励。
- ScriptDLL `0x5cc` 的全部 19 个命中闭合五个 NPC 绑定、阶段 `0 → 1 → 2 → 3 → 4`、任务 ID 分派、启动、接取和四段对话入口；生成器验证 `fun_343/363/364/367/532/556/557/558/572/593/719/807/820/828/851/852/853.cpp`，任一证据漂移即重新隔离。
- 任务复用现有 `DataDrivenQuest` 三段 `TALK` 的步骤发放能力；结算继续由 `quest_data.xml` 检查和扣除三件工作物品，删除旧 Java Handler，不新增运行时原语。

### ScriptDLL 证据恢复第三十批（2026-07-26）

- 恢复同构任务 `2914/3037`。真端 `Quest_SimpleTalk.xml`、`quest.xml`、`quest_data.xml`、`npcs.xml` 与 `Items.xml` 分别闭合 `Air(204147) → Frana(204236) → Air` 和 `Grynos(798166) → Ixion(798199) → Grynos`，中间步骤发放 `quest_2914a(182207014)` / `quest_3037a(182208027)`，并闭合阵营、前置和全部奖励。
- ScriptDLL `0xb62/0xbdd` 各 9 个命中闭合两个 NPC 绑定、阶段 `0 → 1 → 4`、接取和两段对话入口；`DAT_18124f0e0` 经原始 `MainServer/ScriptDLL64.dll` 文件偏移 `0x124d8e0` 验证为 UTF-16 `Air`。生成器验证 `fun_343/349/536/553/560/580/808/831/848.cpp` 与 `fun_350/352/538/554/564/584/812/834/849.cpp`，任一证据漂移即重新隔离。
- 两条任务复用现有 `DataDrivenQuest` 单段 `TALK` 的步骤发放能力；结算继续由 `quest_data.xml` 检查和扣除工作物品，删除两个旧 Java Handler，不新增运行时原语。

### ScriptDLL 证据恢复第三十一批（2026-07-26）

- 恢复魔族任务 `2321`。真端 `Quest_SimpleUseItem.xml`、`quest.xml`、`quest_data.xml`、`npcs.xml` 与 `Items.xml` 闭合使用 `quest_2321b(182204242)` 接取、`Gunter(204225)` 扣除该物品并发放 `doc_quest_2321a(182204119)`、`Hellione(790018)` 结算，以及八种可选武器奖励。
- ScriptDLL `0x911` 的 11 个命中闭合两个 NPC 绑定、物品触发注册、阶段 `0 → 1 → 4`、接取和两段对话入口；生成器验证 `fun_601/603/606/607/608/860/861/862.cpp`，并新增基础 XML 可选奖励数字 ID 的严格校验，任一证据漂移即重新隔离。
- 任务复用现有 `DataDrivenQuest` 的 `ITEM_PLAY → TALK`，只让 compiled 证据门按任务选择 `Quest_SimpleTalk.xml` 或 `Quest_SimpleUseItem.xml`；删除旧 Java Handler，不新增运行时原语。

### ScriptDLL 证据恢复第三十二批（2026-07-26）

- 恢复魔族任务 `2428/2458`。真端数据闭合 `Kistig → Honir → Moreinen → Kistig` 并在 Moreinen 发放 `quest_2428a(182204216)`；同时闭合 `Lif → DF2_NPC_Sprigg → Lif` 的接取发 `quest_2458a(182204194)`、中间扣 A 发 `doc_quest_2458b(182204195)`。`2458` 基础等级按真端从 23 修正为 22。
- ScriptDLL `0x97c/0x99a` 各 12 个命中闭合 NPC、阶段、接取、对话、物品发放与结算入口；`DAT_18124edf0/DAT_18133aba8` 经原始 DLL 文件偏移 `0x124d5f0/0x13393a8` 验证为 UTF-16 `Honir/Lif`。生成器验证两组全部命中文件，任一证据漂移即重新隔离。
- 两条任务复用现有 `DataDrivenQuest` 的线性 `TALK`、接取发放和步骤物品交换能力；删除两个旧 Java Handler，不新增运行时原语。

### ScriptDLL 证据恢复第三十三批（2026-07-26）

- 恢复天族任务 `3076`。真端 `Quest_SimpleTalk.xml`、`quest.xml`、`quest_data.xml`、`npcs.xml` 与 `Items.xml` 闭合 `Atropos(798155) → Ascalon(278503) → Cymaon(278556) → Atropos`、Cymaon 步骤发放 `quest_3076a(182208047) × 1`，以及经验、金币和铸币奖励；旧 Java 的 `Calon` 仅为错误注释，数字 NPC 实际对应 Ascalon。
- ScriptDLL `0xc04` 的全部 12 个命中闭合三个 NPC 绑定、阶段 `0 → 1 → 2 → 4`、接取和三段对话入口；生成器验证 `fun_344/346/533/553/557/560/580/809/828/849/852.cpp`，任一证据漂移即重新隔离。
- 任务复用现有 `DataDrivenQuest` 两段 `TALK` 的步骤发放能力；结算继续由 `quest_data.xml` 检查和扣除工作物品，删除旧 Java Handler，不新增运行时原语。

### ScriptDLL 证据恢复第三十四批（2026-07-26）

- 恢复魔族任务 `2421/2480`。真端数据分别闭合 `Asgeirr(204309) → Kerupnise(204187) → Asgeirr` 的接取发 A、中间扣 A 发 B 与影片 `132`，以及 `Tree_Move_Nabalu(730038) → Tree_Move_virdi(730021) → Tree_NoMove_Lodas(730019) → Tree_Move_Nabalu` 的两段工作物品发放、前置任务和全部奖励；`2421` 基础等级按真端从 20 修正为 30。
- ScriptDLL `0x975` 的 13 个命中闭合两个 NPC、阶段 `0 → 1 → 4`、物品交换、影片对话和结算入口；`0x9b0` 的 13 个有效任务命中闭合三个 NPC、阶段 `0 → 1 → 2 → 4`、接取和三段对话入口，另一个 `fun_912.cpp` 的 `+0x9b0` 只是结构偏移，不作为任务证据。生成器逐函数块验证两条调用链，任一证据漂移即重新隔离。
- 两条任务复用现有 `DataDrivenQuest` 的线性 `TALK`、物品交换和单影片能力；生成器只补齐已存在 `movie` 字段的透传，删除两个旧 Java Handler，不新增运行时原语。

### ScriptDLL 证据恢复第三十五批（2026-07-26）

- 恢复天族 `3020` 与魔族 `4015` 两条单影片对话任务。真端数据分别闭合 `Ankises(798143) → NPC_Agrint_Tartagan(798149) → Ankises` 的接取发放并在中间扣除 `quest_3020a(182208011)`、影片 `363`，以及 `Vinduer(205130) → DF2A_FOBJ_Q4015(730107) → Vinduer`、影片 `394` 和全部奖励。
- ScriptDLL `0xbcc/0xfaf` 各 13 个命中，分别闭合两个 NPC 绑定、阶段 `0 → 1 → 4`、接取、影片对话、物品操作与结算入口；生成器验证 `fun_344/360/543/553/560/580/808/838/848/853.cpp` 和 `fun_346/369/534/556/575/596/823/829/851/859.cpp`，任一证据漂移即重新隔离。
- 两条任务复用现有 `DataDrivenQuest` 的单段 `TALK`、物品交换和影片能力；功能对象继续走现有 `USE_OBJECT` 对话入口，删除两个旧 Java Handler，不新增运行时原语。

### ScriptDLL 证据恢复第三十六批（2026-07-26）

- 恢复希隆 `1553/1574`。真端数据分别闭合 `Diana(203786) → DF2_NPC_TalkingMirror(730051) → Perento(204500) → Piera(204584)` 的接取发 `quest_1553a(182201794)`、镜前交换成 `quest_1553b(182201795)`，以及 `Tree_Move_Terba(730025) → Trou(204560) → Arkos(204561) → Sirilis(204562) → Tree_Move_Terba` 的接取发 `quest_1574a(182201736) × 6`、三段各扣除 2；前置条件和全部奖励继续由 `quest_data.xml` 驱动。
- ScriptDLL `0x611/0x626` 的 19/20 个命中闭合全部 NPC、阶段 `0 → 1 → 2 → 4` 与 `0 → 1 → 2 → 3 → 4`、启动、物品数量、对话和结算入口；生成器逐函数块验证真端调用链，任一证据漂移即重新隔离。
- 两条任务复用现有 `DataDrivenQuest` 的线性 `TALK` 与步骤物品交换能力；删除两个旧 Java Handler，不新增运行时原语。`1691` 的首个 NPC 绑定在恢复源码中仍为未命名数据常量，继续保留 Java，不猜测。

### ScriptDLL 证据恢复第三十七批（2026-07-26）

- 恢复奥特加德 `2207/2278/2279`。真端数据闭合莱卡恩使者、翻译官、Sueron、Mimir、Balder 与 Soul_Zenkaka 的顺序对话，以及 `doc_quest_2207a(182203257)` 的接取发放和第二段扣除、`doc_quest_2278a(182203254)` 的首段发放和第三段扣除、`quest_2279a(182203261)` 的第二段发放。
- ScriptDLL `0x89f/0x8e6/0x8e7` 的 14/16/14 个命中闭合 NPC 绑定、阶段 `0 → 1 → 2/3 → 4`、启动、物品调用、对话和结算入口；生成器逐函数块验证调用链，任一证据漂移即重新隔离。
- `2278/2279` 的基础等级按真端从 16 修正为 17；三条任务复用现有 `DataDrivenQuest` 线性步骤，删除三个旧 Java Handler，不新增运行时原语。

### ScriptDLL 证据恢复第三十八批（2026-07-26）

- 恢复贝鲁斯兰 `2515/2523`。真端数据闭合 `Elli(790015) → Araison(204192) → Mareke(204205) → Shugo_DF2_5(798081) → Elli` 的接取发 `doc_quest_2515a(182204412)`、首段扣除、后两段分别发 `quest_2515c(182204414)` 与 `quest_2515e(182204416)`；同时闭合 `Svera(204802) → Shugo_DF3_10/11/12(798117/8/9) → Horu(204734)` 的接取发 `quest_2523a(182204417) × 3`、三段各扣除 1，以及前置和全部奖励。
- ScriptDLL `0x9d3/0x9db` 的 16/18 个命中闭合全部 NPC、阶段 `0 → 1 → 2 → 3 → 4`、启动、工作物品数量、四段对话和结算入口；生成器逐函数块验证 `fun_344/348/351/359/364/366/533/545/555/556/558/563/573/583/585/807/811/821/828/841/850/851/852/853/854/855.cpp`，任一字段或调用链漂移即重新隔离。
- 两条任务复用现有 `DataDrivenQuest` 的线性 `TALK`、接取发放和步骤物品操作；删除两个遗漏物品语义的旧 Java Handler，不新增运行时原语。

### ScriptDLL 证据恢复第三十九批（2026-07-26）

- 恢复贝鲁斯兰 `2692/4501`。真端数据闭合 `LabB_BeholderNamedQ_43_Ae(212164) → Lanse(204108) → Shugo_AB1_D3(279027) → Ab1_NPC_LugBug(279029) → 原 NPC` 的接取发 `quest_2692a(182204510)`、末段扣 A 发 `quest_2692b(182204511)`；同时闭合 `Lapion(204728) → Tekor(204340) → Virashak(204348) → Lapion`、Virashak 发 `quest_4501a(182204533)` 及全部奖励。
- ScriptDLL `0xa84/0x1195` 的全部 16/12 个命中闭合 NPC、阶段 `0 → 1 → 2/3 → 4`、启动、物品数量、对话和结算入口；生成器对 `fun_343/357/358/363/367/369/541/547/556/557/558/568/588/589/816/837/842/851/852/853/854.cpp` 设置逐函数块硬门，并用不同数据常量区分 `fun_851.cpp` 中 `2692` 的两个独立回调。
- 两条任务复用现有 `DataDrivenQuest` 的线性 `TALK` 与步骤物品交换；删除两个遗漏真端物品动作的旧 Java Handler，不新增运行时原语。

### ScriptDLL 证据恢复第四十批（2026-07-26）

- 恢复 `3035/3973/4052`。真端数据分别闭合 Atropos 的两段钥匙充能交换、`Tersites → Utisda → Daphnis → Andu → Mesalina` 的三件信物发放，以及 Moai 功能对象接取后依次对话三名 Bumbum NPC、在中间发放并最终扣除 `quest_4052a(182209030)`；`3035` 基础最低等级按真端从 47 修正为 46。
- ScriptDLL `0xbdb/0xf85/0xfd4` 的全部 15/18/15 个命中闭合 NPC、阶段、启动、物品和对话入口；`fun_369.cpp` 的 `DAT_18124cc30` 经原始 DLL 文件偏移 `0x124b430` 验证为 UTF-16 `Utisda`，生成器据此锁定未命名常量，不按旧 Handler 猜测。
- 运行时只让现有 `TALK` 起点同时响应 `USE_OBJECT` 展示接取页，复用同一接取、步骤和奖励状态机；测试锁定 `4052` 对象交互只展示任务而不提前改变状态。删除三个旧 Java Handler，不新增 DSL 字段或模板类型。

### 通用 XML 任务运行时收敛（2026-07-26）

- 唯一旧 `<xml_quest>` 任务 `1127` 已等价改写为 `data_driven_quest`：`798008` 接取、`700001` 对象交互发放 `182200215 × 1`、进入步骤 1、回到 `798008` 显示 `2375` 并检查任务物品后结算。
- `DataDrivenQuest` 仅补充接取/步骤对话页覆盖，并让现有 `ACTION` 步骤复用物品发放/扣除；删除 `XmlQuestData`、`XmlQuest` 和旧 `models/xmlQuest` 条件/事件/操作层，同时从 JAXB 与 XSD 移除 `<xml_quest>`。
- 本阶段只收敛运行时，不将旧定义冒充真端生成产物：`1127` 继续由生成报告标记为 `blocked_authoritative_source`，待 ScriptDLL 启动、对象回调、阶段和结算证据闭合后再迁入 `retail="true"` 生成 XML。

### SimpleTalk 字段模型批量迁移首批（2026-07-26）

- 生成器直接编译只读真端 `Quest_SimpleTalk.xml` 的 `give_item1..3`、`remove_item1..3`、`cutsceneid1` 和 `cs1_haction` 字段；名称到数值 ID 的解析仍只发生在构建期，生产运行时不依赖开发机的 `58Server` 路径。
- `DataDrivenQuest` 步骤新增默认关闭的 `advance_dialog_id` 与 `movie`，并复用已有步骤物品发放/扣除能力；`cs1_haction=1009` 的无中间对话任务可在指定选择事件播放影片并进入正式奖励对话，不新增逐任务 Java Handler。
- 首批移除并迁入 19 条旧 `report_to`：`2964/3209/3943/3946/3949/3952/3955/3958/4209/4947/4950/4953/4956/4959/4962/11074/11075/19056/29056`。真端原始 XML 未改动。
- 同一字段规则同步修正既有生成任务 `1192/2913/21136`：`remove_item1` 现在绑定 `talk_npc1`，不再错误延迟到最后一段对话。
- 通用候选必须让位于现存旧 XML 和已通过专用证据编译的任务，防止字段扩展覆盖制作奖励、动作链等额外语义。28 条 `crafting_rewards` 继续保留：`Quest_SimpleTalk.xml` 没有表达完成后提升制作技能和自动学习配方的副作用。
- 当前生成 XML 5,028 条、保留旧 XML 726 条、选中 XML 共 5,754 条，所有权交集为 0；可执行任务 6,453 条、隔离任务 23 条，管理闭包保持 6,476/6,476。

### 旧 XML 所有权审计（2026-07-25）

- 已从旧 XML 精确删除 2,523 条定义：2,467 条原本已被 `retail="true"` 生成 XML 覆盖，另有 56 条 `category_acquire_=none` 的 Talk/Hunt 经真端字段与唯一 NPC/目标解析证明后迁入生成 XML；32 个空文件一并删除。
- 574 条 `work_order` 已由真端 `Quest_CombineTask.xml` 统一生成：逐条验证任务字段、NPC 唯一解析、配方技能/技能点/产物/任务类型、`quest.xml` 产品与组件、`quest_data.xml` 数字物品 ID 和数量；任一漂移即拒绝生成。运行时继续复用现有 `WorkOrdersData/WorkOrders`，不新增 Java Handler，旧 `work_order.xml` 已删除。
- 该阶段 49 个旧文件保留 745 条定义：65 条使用生成器尚未覆盖的专用 XML 模板、324 条名称或目标无法唯一解析、136 条不在当前基础任务、115 条缺少支持的真端源、64 条包含额外语义字段、41 条真端结构尚不能无损表达。
- 生成报告逐条记录文件、模板、保留原因和处置结论。65 条专用模板已经与生成 XML 一样经 `static_data.xml` 的目录导入进入 `XMLQuests`，再由 `QuestEngine.registerScriptQuest` 移交统一 `ScriptRegistry`；无需仅为合并物理文件而改写。
- 41 条结构候选继续保留：14 条 Talk+PVP 与 14 条无 NPC 接取 PVP 带 `value3_progress_`，但缺少其到世界 ID 的权威映射；另有 4 条无 NPC PVP 即使不带 `value3`，旧定义仍分别使用 `worlds="0"` 或指定世界，不能反推。2 条 EnterArea+PVP、2 条 Talk+PVP+CollectItem 和 1 条大小写不同的 PvP 需要复合阶段语义；4 条 SimpleItemPlay 缺少物品发放、使用、回收的对称证据。其余保留项分别受基础任务、权威源、引用解析或额外语义阻塞，不能猜测转换。
- 生成器同时校验生成 XML、保留旧 XML 与 Java Handler 的完整所有权；任一实际生效 XML 与 Java 任务 ID 相交时直接失败。
- 该阶段生成 XML 5,009 条、保留旧 XML 745 条、选中 XML 共 5,754 条，所有权交集为 0；可执行任务 6,453 条、隔离任务 23 条，管理闭包保持 6,476/6,476。

### Java Handler 最终所有权审计（2026-07-25）

- 生成器逐文件审计全部 841 个 Java Handler，并在报告中记录任务 ID、文件、事件集合、已识别机制、保留原因和处置结论；报告条目数与磁盘 Handler 数不一致时，所有权统计无法闭合。
- 835 个基础任务 Handler 已完整分类：792 条为 `retained_complex_semantics`，43 条为 `retained_authoritative_gap`；另 6 条不属于当前基础任务，标记为 `not_executable_without_base`。
- 机制 census：物品变更 379、物品条件 255、影片 130、出生 75、传送 70、定时器 51、动态奖励 34、对象交互 31、实例状态 26、技能 7、继承型 Handler 6。`defaultCloseDialog` 的物品重载按物品变更计入，避免把发放或扣除任务物品的链路误报为单纯对话。该统计是“每个 Handler 是否使用该机制”，同一任务可计入多个机制。
- `retained_authoritative_gap` 不等于可直接转换：它只证明当前 Java 表面形状简单；在 ScriptDLL 的启动触发、NPC/目标、阶段/数量、完成条件和奖励入口全部闭合前，仍不得删除 Java 或生成空 XML。
- 当前 5,028 条生成 XML、726 条保留 XML、835 条基础 Java Handler 与 23 条正式隔离均有唯一所有者和机器可审计处置；不能用审计分类替代尚未完成的迁移。

### 剩余 ScriptDLL 隔离边界（最终审计）

| 任务 | 保留隔离原因 |
| --- | --- |
| `16984/26984/16989` | 已恢复任务创建、影片播放和影片结束结算函数，但启动触发注册链不完整，不能证明任务如何开始。 |
| `9554–9557` | `quest.xml` 只有阵营/性别奖励分支，ScriptDLL 无对应任务函数或触发链，不能仅凭奖励表注册。 |
| `1489` | 只有通用任务 ID 分派命中，无目标、触发和完成链。 |
| `2590` | ScriptDLL 无对应可执行任务链。 |
| `1908` | 多阶段、多 NPC、多个数据数组和特殊对话选择，无法证明全部分支语义。 |
| `3326` | 与任务 `1037` 联动，含隐藏计数 20、多个怪物和跨任务结算条件，属于跨任务状态机。 |
| `4338` | Urakon/Gundalpun/Kimci/五个朋友、多阶段、物品回调、任务 `2033` 状态和动态交互，不能由单任务通用模板无损表达。 |
| `50102` | NPC 动态选择五种物品之一并各检查/扣除 10 个，当前模型不支持 OR 型动态库存分支。 |

以上 13 条继续隔离，不注册空处理器；只有启动事件、目标、阶段/数量、完成条件和奖励入口全部由真端证据闭合时才重新评估。

## 真实服务端任务冒烟（2026-07-24）

数据边界：真端原始证据固定为 `/Users/mc/IdeaProjects/58Server`；`/Users/mc/PycharmProjects/aion_drop` 是真端 XML 转换与迁移审计区域；AionEmu 仓库承载生成产物与运行时。

真实进程启动验证：

- 使用 `./package.sh` 部署后，以 `-Xms1g -Xmx4g` 启动 Login / Game / Chat；游戏与登录数据库均通过 HikariCP 连接。
- `2106`、`7777`、`9014`、`9021`、`10241` 五个端口全部监听，GameServer 向 LoginServer / ChatServer 认证成功。
- QuestEngine 加载 8,037 个处理器，服务进入“可接受连接”，连续运行 7 分钟无 `ERROR`、`FATAL` 或异常日志。
- 生成任务与 Java Handler 已保持单一所有权，不再依赖最后加载覆盖；`QuestEngineScriptRoutingTest` 继续证明 `ScriptRegistry` 路由与未迁移 Java 回退均可用。

事件级代表矩阵由 `RetailQuestRuntimeSmokeTest` 直接读取生成 XML，避免复制或猜测任务语义：

| 类型 | 代表任务 | 验证事件 |
| --- | ---: | --- |
| Talk | 1118 | NPC 对话推进到奖励状态 |
| CollectItem | 1152 | 收集检查分派并完成最终步骤 |
| Hunt | 14252 | 三段目标击杀按顺序推进 |
| PVP | 11324 | `KillInWorld` 两次击杀进入奖励状态 |
| ItemPlay | 13951 | 真端物品 182216201 使用后推进 |
| EnterArea | 15322 | 感知 NPC 206465 后推进 |
| EnterWorld | 13950 | 进入世界 302340000 后推进 |
| 混合状态机 | 17540 | EnterWorld → Talk → Hunt → Hunt → CollectItem 严格顺序推进 |
| 真端 SimpleTalk 字段模型 | 2964、3209、11074 | 中间步骤物品发放、最终收集检查，以及指定对话选择播放步骤影片后进入奖励状态 |
| ScriptDLL 物品买入 | 19079–19081、29079–29081 | 六条均解析为同 NPC 接取/交付的 `ItemCollectingData`，且不伪造接取物品 |
| ScriptDLL 限时汇报 | 80761、80766 | 两段烟花箱 NPC 顺序、120 秒计时配置和超时阶段回退 |
| ScriptDLL 碎石救援 | 30503、30553 | 碎石交互删除目标但不推进，Rodelion 对话后才进入奖励状态 |
| ScriptDLL 隐藏钥匙 | 3219、3220、4219、4220 | 进入钢铁钩爪号启动、重复进入不重置、离开后清理任务状态 |
| ScriptDLL 装备进化 | 19678、19679、29678、29679 | 同 NPC 接取/交付，获得指定进化材料后进入奖励状态 |
| ScriptDLL 感知结算 | 3959、4963 | 感知专用区域 NPC 后接取并通过正式奖励流程自动完成 |
| ScriptDLL 交互推进 | 30504、30554 | 石柱交互推进，重进伦图斯基地回退阶段 0，Paios 负责结算 |
| ScriptDLL 住宅引导 | 18806、28806 | 五个住宅管家均可接取，中间 NPC 单段推进后由专用 NPC 结算 |
| ScriptDLL 枯树交互 | 13809、23809 | 三个枯树对象必须按真端顺序交互，完成后回同一阵营 NPC 结算 |
| ScriptDLL 卡多尔抵达 | 13800、23800 | 专用起点接取，经传送门 NPC 对话后由阵营指挥官结算 |
| ScriptDLL 串行狩猎 | 30600、30610 | 中间对话后两名导航官任意合计击杀 1 次，再击杀 Boss，严格按三步推进 |
| ScriptDLL 活动配送员 | 50008、51008 | 主城 Santa 接取，阵营配送员击杀 2 次后回同一 NPC 交付 |
| ScriptDLL 专家奖励 | 1987、2985 | 任意制作技能 400 点后，由阵营领袖接取并向仓库管理员领取经验与背包扩展 |
| ScriptDLL 伤员援助 | 11010 | 依次对话两名 NPC 和功能对象，对象步骤发放任务物品后回 Naiting 结算 |
| ScriptDLL 营地物资 | 2512 | Loki 发放两份补给，Gigrite 收取一份后由 Kistenian 结算 |
| ScriptDLL 催款单 | 1218 | Taiotus 接取，Shugo3 发放催款单后由 Une 结算并解锁后续任务 |
| ScriptDLL 秘密配送 | 1220 | 完成 1219 后由 Une 发货，Shugo3 交换包裹，再交给沃特伦目标 NPC |
| ScriptDLL 行商请求 | 1483 | 两名中间 NPC 依次发放工作物品，最终交给下一名行商 |
| ScriptDLL 行商后续 | 1484 | 完成 1483 后由三名中间 NPC 依次发放工作物品，再返回首名行商 |
| ScriptDLL 失物转交 | 2914、3037 | 中间 NPC 发放唯一工作物品，再返回各自接取 NPC 结算 |
| ScriptDLL 物品信件 | 2321 | 使用灵魂信件接取，在 Gunter 交换成书信后由 Hellione 结算 |
| ScriptDLL 莫尔海姆配送 | 2428、2458 | 严格保留 NPC 顺序、书籍发放和接取物品交换 |
| ScriptDLL 结界强化 | 3076 | 依次对话 Ascalon、Cymaon，在第二段发放工作物品后回 Atropos 结算 |
| ScriptDLL 魔族物品配送 | 2421、2480 | 接取/步骤物品发放与交换、影片 132、前置任务和两段 NPC 顺序 |
| ScriptDLL 单影片对话 | 3020、4015 | 工作物品交换、功能对象入口及影片 363/394 后按阶段结算 |
| ScriptDLL 希隆物品配送 | 1553、1574 | 两段物品交换与三段定量分发，保留前置条件和原 NPC 顺序 |
| ScriptDLL 奥特加德递送链 | 2207、2278、2279 | 三条相邻任务保留文书发放、交换、NPC 顺序和真端等级 |
| ScriptDLL 贝鲁斯兰递送 | 2515、2523 | 三段工作物品发放/扣除、NPC 顺序、前置条件与奖励均按真端保留 |
| ScriptDLL 贝鲁斯兰物件交接 | 2692、4501 | 接取/末段工作物品交换、NPC 顺序和全部奖励均按真端保留 |
| ScriptDLL 跨区域物件交接 | 3035、3973、4052 | 两段交换、三件信物发放及 Moai 对象接取均按真端执行 |

验证结果：48 个运行时冒烟用例，以及任务路由和注册表共 58 个 Java 测试全部通过；正式生成一致性检查与 Python 生成器 25 个测试通过。该自动化覆盖生产 XML 反序列化、处理器事件语义和状态迁移；仍需使用 5.8 客户端进行 NPC 可达性、对话页展示、物品动画、区域触发距离、计时条显示和最终奖励包的人工联机确认。

## 后续迁移边界

### 点 2：完整数据驱动 Quest 运行时

现状：统一加载路径已完成；所有真端 XML 可证明任务由 `ScriptEngine + XMLQuest/DataDrivenQuest` 加载，剩余 Java Handler 已进入机器审计并继续按 ScriptDLL 证据逐条迁移。

维护规则：新增真端来源后，只迁移证据链完整且能由现有强类型模型无损表达的具体任务；否则维持 Java 所有权，不新增空处理器或推测性 DSL。

### 点 4：逐副本移除 Java 桥接

现状：副本迁移按独立所有权矩阵维护。真端 Pattern、条件/静态出生、掉落和任务数据负责可证明机制；匹配、计分、奖励、持久化、离线恢复及缺少真端消费者的交互继续保留最小 Java 边界。

演进顺序（每个副本一次提交 + 更新 `docs/retail-instance-migration-status.md`）：
1. 优先由 AI 引擎（`RetailPatternAI2`）接管：副本内 NPC 的对话、传送、出生、门控凡有真端 Pattern/条件出生/Alias 数据覆盖的，移除对应 `ai/instance/*` AI2。
2. 无 Pattern 覆盖但属通用交互（如 `TowerLiftAI2` 式传送）的，改由 `ScriptNpc` + 数据绑定承接，替代 `switch(worldId)` 硬编码。
3. 副本级阶段/奖励/清理：判据为"变量、事件、阶段、奖励、清理全部由真端数据覆盖"时，移除 `instance/handlers/scripts/*` Handler；否则保留最小桥接并在迁移状态文档记录边界。
4. 严禁 Java 逐副本单独补逻辑 Handler；缺口优先补真端数据（`staticdata_converter` 生成器），数据确实缺失才保留桥接并记录。

逐副本工作清单起点：`ai/instance/*` 目录与 `instance/handlers/scripts/*` 文件交叉 `retail-instance-migration-status.md` 已记录的 HYBRID/RETAIL 状态，按"已 RETAIL_AI_QUEST → 移除残留 AI2"和"HYBRID → 评估剩余桥接能否数据化"两档推进。

## 不做

- 不引入 JNI / 不加载真端 `ScriptDLL64.dll`。
- 不重写 `RetailPatternAI2` / `XMLQuest` 运行时逻辑（点 1 奠基仅提供统一抽象与注册表）。
- 不为去头而改无关文件。
