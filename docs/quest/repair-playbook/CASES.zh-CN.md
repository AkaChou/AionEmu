# 已验收任务修复代表案例

[返回任务排查与修复 Playbook](../QUEST_REPAIR_PLAYBOOK.zh-CN.md) ·
[查看 Pattern 指纹与提交索引](PATTERNS.zh-CN.md)

本文件只保存已完成 Playbook 验收门禁、能够建立复用边界的代表案例。案例编号保持稳定，新增案例只能追加，
不得为了按任务 ID 排序而重排已有编号。

当本文件影响检索或评审时，将完整案例块按稳定编号范围移入 `cases/*.md`，并在本文件保留范围索引和链接。
自检器会自动聚合这些分片；分片不得复制已有案例、复用编号或脱离 Pattern 索引单独增加案例。

## 8.1 升级自动登记弹出不存在的任务页

- Pattern ID：`LEVEL_UP_AUTO_START_NO_DIALOG`。
- 代表任务：38001「Radiant Ops Recruitment」。
- 玩家症状：升级自动登记任务时客户端弹出任务 HTML 的 `HtmlPageId 4 / load fail`。
- 根因：升级入口错误发送 `SHOW_ASK_QUEST_ACCEPT_WINDOW(4)`；NPC `START_DIALOG(31)` 又错误发送 Aion 5.8 客户端不存在的 `SELECT2(1352)`。旧 handler 的升级入口只启动任务并刷新状态，NPC 对话页为 `DEFAULT_SUCCESS(10002)`。
- 修复层：仅修改任务 XML；升级入口提交 `NONE -> START` 并刷新可见性但不发送任务 HTML，NPC `START_DIALOG(31)` 独立显示客户端与旧 handler 共同证明的 `DEFAULT_SUCCESS(10002)`。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/38001.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest38001LevelUpDialogTest.java`。
- 验证命令和结果：`rtk mvn -q -Dtest=Quest38001LevelUpDialogTest,Quest38002LevelUpDialogTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestDialogOrderAuditTest test` 通过；生产 catalog 6200 条编译成功，失败 0，白名单违规 0；顺序审计显示 38001 的 31 -> 10002 为 `PAGE_ACTION_MATCHED`，奖励页 5 为 `TERMINAL_PAGE_REACHED`；玩家实测升级登记不再弹出加载失败页。
- 复用边界：仅适用于升级入口不应显示任务页，且 NPC `START_DIALOG(31)` 应显示 `DEFAULT_SUCCESS(10002)` 的同型任务；页面、状态或副作用合同不同的任务必须重新取证。
- commit：`d3b28d2af3a7a3085da461d96bb9dfe6118d4905`。

## 8.2 升级自动登记与双 NPC 阶段对话链错配

- Pattern ID：`LEVEL_UP_MULTI_NPC_PHASED_DIALOG`。
- 代表任务：1920「Testing Your Mettle」。
- 玩家症状：升级自动登记任务时客户端弹出任务 HTML 的 `HtmlPageId 4 / load fail`；修复升级提示后，还必须保证第一个 NPC 的 `1011 -> 1012 -> 1013 -> 10000`、第二个 NPC 的 `1352 -> 1353 -> 10255` 和最终领奖页链可达。
- 根因：升级入口错误发送 `SHOW_ASK_QUEST_ACCEPT_WINDOW(4)`；原 XML 将两个 NPC 的多阶段客户端动作压缩成通用 `FINISH_DIALOG`、`SELECT_QUEST` 和错误的奖励入口，丢失了 `var0=1` 中间状态以及客户端页面/动作顺序。旧 handler 与 Aion 5.8 客户端页面证据共同证明：第一个 NPC 完成第一段后关闭对话，第二个 NPC 才能进入成功状态，最终只由第一个 NPC 领奖。
- 修复层：仅修改任务 XML；升级入口不显示 page 4，两个 NPC 使用显式中间节点、客户端可见 action/page 和关闭响应推进，最终只保留第一个 NPC 的 reward preview 与 completion owner。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/1920.xml`、`src/main/resources/aion/data/static_data/quest_definition/quests/2945.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest1920And2945ClientDialogAlignmentTest.java`。
- 验证命令和结果：`rtk mvn -q -Dtest=Quest1920And2945ClientDialogAlignmentTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestDialogOrderAuditTest test` 通过；生产 catalog 6200 条编译成功，失败 0，白名单违规 0；两个 XML 均通过 XSD；客户端实测升级登记及双 NPC 对话流程通过。
- 复用边界：仅适用于升级入口不应显示 page 4，且任务存在由客户端页面动作驱动的双 NPC 或多阶段状态链的同型任务；单 NPC `DEFAULT_SUCCESS(10002)` 合同复用 8.1，页面、状态或奖励归属不同的任务必须重新取证。
- commit：`76b0894`。

## 8.3 实时奖励确认使用独立动作导致领取无响应

- Pattern ID：`TARGETLESS_REALTIME_REWARD_ACTION_SPACE`。
- 代表任务：13830「Stigma 101」。同批修复的 13831..13834 共享同一问题模式，不重复建案例。
- 玩家症状：任务进入实时奖励界面并可选择职业奖励，但点击“领取”没有反应，任务不完成、奖励不到背包、界面也不关闭。
- 根因：Aion 5.8 客户端对第一个普通奖励槽发送 `HACTION_SELECTED_QUEST_REWARD1(8)`，对第一个实时奖励槽发送 `HACTION_SELECTED_QUEST_AUTO_REWARD1(110)`。无目标 `CM_DIALOG_SELECT` 会把原始 action 交给 typed dispatcher，后者按 `QuestEvent.QuestDialog(110)` 查询生产索引；原 XML 只有普通奖励动作 8 的完成路由，因此实时奖励确认没有候选迁移。旧 `finishReportedQuest` 将 110..124 映射到普通奖励槽 8..22，且正式任务数据将这五个任务标记为 `can_report=true`，共同证明两个动作空间应落到等价的奖励完成合同。
- 修复层：任务 XML + 由客户端字典和活动 XML 引用生成的 typed dialog action 枚举。五个任务的 11 个互斥职业分支同时注册普通动作 8 和实际可见的实时动作 110；事务内发放职业物品与经验、回收工作物品并完成任务，提交后按 `refresh-player-stats -> COMPLETION sync -> close-dialog` 执行。不在共享 runtime 中全局重写动作。
- 修改文件：`src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDialogAction.java`、`src/main/resources/aion/data/static_data/quest_definition/quests/13830.xml`、`13831.xml`、`13832.xml`、`13833.xml`、`13834.xml`，以及 `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest13830To13834TargetlessRewardTest.java`。
- 验证命令和结果：`rtk mvn -Dtest=Quest13830To13834TargetlessRewardTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test` 通过，共 8 个测试，失败 0、错误 0、跳过 0；五个 XML 均通过 XSD；`rtk python3 .agents/summary/quest/generate_quest_dialog_enums.py --check` 返回 `changed=0`；Aion 5.8 客户端实测实时奖励可领取并正常完成任务。
- 复用边界：仅适用于权威数据允许实时报告、无目标奖励包确实发送 110..124，且普通与实时槽位应共享奖励完成语义的任务。必须按客户端实际可见槽位逐一映射：单一职业奖励通常只需 110；多槽奖励要分别证明 111..124 与奖励索引。动作 108、NPC 目标领奖、不同奖励索引、额外页面或副作用合同必须单独取证，不能套用本案例或做全局 remap。
- commit：`4a23cf0a0f531182e195bfa0f662513da50d170a`。

## 8.4 两段飞行传送、不可达副本恢复与血量阈值变身兜底

- Pattern ID：`MOVIE_CONTINUATION_RESPONSE`、`COMMIT_SYNC_BEFORE_FLIGHT_TELEPORT`、`UNREACHABLE_INSTANCE_REENTRY_RECOVERY`、`QUEST_SCOPED_NPC_SEARCH_ALIAS`、`THRESHOLD_TRANSFORM_DEATH_FALLBACK`、`QUEST_NPC_STATIC_SPAWN_DEDUPLICATION`、`QUEST_REWARD_PREVIEW_PAGE_CONTRACT`。
- 代表任务：14047「Chaining Memories」。
- 玩家症状：佩托 802052 的电影 421 会重复播放，任务页面不能继续；同一位置同时出现任务佩托 802052 和普通佩托 204653。任务先通过飞行 71001 到达第一处玩家无法自行返回的副本区域，再通过飞行 72001 进入下一处区域；如果客户端崩溃、断线，或服务端重启导致副本实例重建，持久化的 `s4/s5` 会把玩家留在无法重新执行任务动作的位置。GM 点击“寻找”还会因同名模板传送到普通阿凯斯泰斯 204652，而不是任务 NPC 802051。最后战斗中入口形态 233877 不会可靠生成任务监听的最终形态 214599，直接秒杀还会跳过 75% 血量检查；即使击杀推进，278500 奖励对话也会因服务端发送不存在的 `HtmlPageId 10002` 显示 load fail。
- 根因：电影 421 后缺少客户端实际存在的 `SELECT5_1(2376)` 页面，且把 `SETPRO10/SETPRO11(10009/10010)` action ID 当成页面 ID；副本静态数据重复生成普通佩托。原迁移先执行飞行副作用、后同步已提交状态，传送过程发生断线时客户端和任务进度可能不同步。`s4(var0=4)` 是飞行 71001 后的阶段，`s5(var0=5)` 是飞行 72001 后的阶段，两者都只能由前置飞行进入，却没有 `ENTER_WORLD` 恢复边；单纯回到上一个 `s4` 仍然无法从普通世界重新到达 802052。Aion 5.8 客户端寻找链接提交同名普通模板 204652，服务端没有结合任务阶段解析为 802051。副本静态出生 233877，而旧 handler 和任务合同只监听 214599；零售空 pattern 又绕过 `betrayer_icaronix` 脚本 AI，原阈值逻辑也没有死亡兜底。奖励预览最后把 `DEFAULT_SUCCESS(10002)` 当作 Q14047 页面发送，而旧 handler 的 `sendQuestEndDialog` 合同是奖励窗口 page 5。
- 修复层：任务 XML 将两段飞行都固定为 `commit -> PACKET_ONLY sync -> close-dialog -> flight-teleport`，分别使用 71001 和 72001；电影 421 后返回 2376 页面，错误阶段点击明确关闭。`s4/s5` 在 `ENTER_WORLD` 统一回退到 `s3(var0=3)`，让玩家重新与 802051 对话并再次触发飞行 71001；不在 `LOG_OUT` 回退，避免正常登出和重登各执行一次，也不回退已经可继续的 `s6`、`REWARD` 或 `COMPLETE`。GM 寻找只在 Q14047 `START + var0=3/6` 时把 204652 限域解析为 802051。副本删除重复 204653，保留静态入口形态 233877；AI 选择保护 `betrayer_icaronix`，并用共享 `AtomicBoolean` 让 75% 阈值和 `handleDied()` 最多生成一次 214599。最终击杀只监听 214599，提交后按 `PACKET_ONLY sync -> movie 422` 推进；278500 的 `USE_OBJECT` 显示 `SHOW_SELECT_QUEST_REWARD_WINDOW1`。
- 传送合同：`s3 + 802051 + SETPRO10 -> s4` 必须先同步 `var0=4` 再关闭窗口并执行 71001；`s4 + 802052 + SETPRO11 -> s5` 必须先同步 `var0=5` 再关闭窗口并执行 72001。传送是 commit 后副作用，事务失败时不得启动飞行，客户端也不能在旧任务状态下进入新区域。
- 副本崩溃回退合同：无论玩家在 71001 后尚未完成 802052 对话，还是在 72001 后尚未击杀 214599，只要重新进入世界时仍为 `s4/s5`，都回到 `s3`。恢复目标不是机械地减一阶段，而是回到普通世界中仍可交互、且能重建整段副本路径的最近节点；该合同同时覆盖客户端崩溃重连、网络断线重登、服务端重启和副本实例丢失后的重新进入。
- GM 寻找传送合同：客户端同名链接请求 204652 时，只有 GM 且 Q14047 为 `START + var0=3` 或 `START + var0=6` 才解析到任务 NPC 802051 并直接传送；其他任务、其他状态、其他阶段、空任务状态以及已经请求 802051 的情况均保持原 ID，不能全局改写同名 NPC。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/14047.xml`、`src/main/resources/aion/data/static_data/spawns/Instances/310100000_Azoturan_Fortress.xml`、`src/main/java/com/aionemu/gameserver/ai/instance/azoturanFortress/Betrayer_IcaronixAI2.java`、`src/main/java/com/aionemu/gameserver/ai2/AI2Engine.java`、`src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_OBJECT_SEARCH.java`，以及 `Betrayer_IcaronixAI2Test`、`AI2EngineRetailSelectionTest`、`CMObjectSearchTest`、`Quest14047ClientDialogAlignmentTest`。
- 验证命令和结果：索引快照运行 `rtk mvn -q -Dtest=Quest14047ClientDialogAlignmentTest,Betrayer_IcaronixAI2Test,AI2EngineRetailSelectionTest,CMObjectSearchTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test` 通过；生产 catalog 6200 条编译成功，失败 0，白名单违规 0。全量 E2E 生成 396,797 条 transition、390,082 条 PASS，Q14047 为 58/58 PASS，`PAGE_NOT_IN_CLIENT`、`INVALID_PACKET_ORDER`、`STATE_CHANGED_WITHOUT_RESPONSE`、`AFTER_COMMIT_FAILURE`、`RUNTIME_REQUIRED`、`TRANSACTION_FAILURE` 均为 0。用户使用重新打包的服务端完成真实客户端端到端验收，确认页面、两段飞行、断线恢复、唯一佩托、214599 击杀、电影 422 和 278500 奖励完成流程均可继续。
- 复用边界：传送前必须证明目标状态已经事务提交，状态同步、关闭窗口和飞行的先后顺序不能照搬旧 handler 中先改内存再发包的实现。只有后续阶段所在位置确实无法由玩家自行返回、实例重建也不能恢复交互对象，且旧 handler/客户端流程证明必须重新执行前置传送时，才能在 `ENTER_WORLD` 回退到最近的可重入阶段；普通持久进度不能借此清零，也不能把 `LOG_OUT` 和 `ENTER_WORLD` 同时作为回退入口。只有任务目标由入口形态的阈值变身生成时才添加死亡兜底，并必须用同一幂等门覆盖阈值和死亡竞争。GM 搜索别名必须同时限定任务 ID、状态和阶段，不得影响普通玩家地图标记或全局替换同名模板。页面 ID 与 action ID 仍是独立空间，每条电影后续页和奖励窗口都必须由 Aion 5.8 客户端与旧 handler 分别证明。
- commit：`8b058d4b4de747d12df9e9af63617619d5eefcf5`。

## 8.5 高阶守护者任务完成后未直接升到 66 级

- Pattern ID：`ARCHDAEVA_ATOMIC_COMPLETION_PROMOTION`。
- 代表任务：10520「遗失的记忆」。20520「Lost Destiny」为同一合同的魔族任务，不重复建立案例。
- 玩家症状：任务领奖后经验奖励和任务完成状态可以提交，但角色没有稳定地持久化高阶守护者身份，也不会在提交成功后立即升到 66 级；重试或重登还可能暴露数据库与在线角色状态不一致。
- 根因：标准 `npc-complete` 只覆盖普通奖励结算，任务 XML 没有声明高阶守护者晋升；经验奖励本身受当前经验和 65 级上限影响，不能替代 `is_archdaeva` 持久化及在线角色升级。
- 修复层：新增 `QuestAction.PromoteArchDaeva` 与 `promote-archdaeva` XML/XSD/DSL 合同；`PlayerQuestProgressionPort` 在奖励和任务状态相同的 JDBC 事务中执行 `GREATEST(exp, level-66-start-exp)` 与 `is_archdaeva=true`，提交后才调用在线角色 `setArchDaeva()`；事务快照同时恢复晋升标记。10520/20520 使用显式 `reward -> complete` 路由，顺序固定为经验奖励、晋升、完成任务，再刷新属性和完成状态。
- 修改文件：`src/main/java/com/aionemu/gameserver/dao/PlayerDAO.java`、`src/main/java/com/aionemu/gameserver/dao/impl/PlayerDAO.java`、`src/main/java/com/aionemu/gameserver/model/gameobjects/player/PlayerCommonData.java`、`src/main/java/com/aionemu/gameserver/questEngine/definition/QuestAction.java`、`QuestDefinitionCompiler.java`、`QuestDefinitionXmlCompiler.java`、`QuestDsl.java`、`src/main/java/com/aionemu/gameserver/questEngine/runtime/CompositeQuestActionPort.java`、`PlayerQuestProgressionPort.java`、`QuestProgressionPort.java`、`QuestMutationPlanner.java`、`QuestRuntimeComposition.java`、`quest_definition.xsd`、`quests/10520.xml`、`quests/20520.xml`，以及对应晋升、事务顺序和快照回归测试。
- 验证命令和结果：两个任务 XML 均通过 `xmllint --noout --schema .../quest_definition.xsd`；`git diff --check` 通过；生产 catalog/whitelist 报告为 6200 条任务编译成功、失败 0、白名单违规 0；用户确认真实客户端领奖后角色直接升到 66 级并完成验收。
- 复用边界：仅适用于任务完成本身代表高阶守护者晋升，且必须原子持久化身份标记、最低经验和任务完成状态的任务。普通经验奖励、普通等级奖励或仅更新客户端等级显示的任务不得复用该动作；晋升动作必须与恰好一个 `complete-quest` 和 `COMPLETE` 投影绑定。
- commit：`7cd670ffb22ddd080b550f6100b09932efe2c7d8`。

## 8.6 状态变化后先发页面、后同步任务状态

- Pattern ID：`STATE_SYNC_BEFORE_DEPENDENT_PAGE`。
- 代表任务：1573「Some Tasty Mushrooms」。1607、2392、2533、10032、24153 为同一协议顺序问题，不重复建立案例。
- 玩家可见症状：一次交互已经把任务推进到奖励或新进度，但服务端先发送新页面、后发送新任务状态；客户端可能用旧 `status/step` 解释新页面，表现为成功页、奖励页或物品确认页与任务进度不同步，或需要重复交互。协议回环可稳定观察到错误的 `SM_DIALOG_WINDOW -> SM_QUEST_ACTION` 顺序。
- 根因：这些 transition 的事务状态和物品动作本身正确，`after-commit` 却把 `SHOW_QUEST_PAGE` 声明在 `sync-quest-state` 之前。两者都在 commit 后执行不代表顺序可以交换；页面消费的是刚提交的状态，必须先让客户端收到对应的 `SM_QUEST_ACTION`。
- 修复层：仅调整六个任务 XML 的 `after-commit` 顺序为 `sync-quest-state -> SHOW_QUEST_PAGE`，不改变 source、target、条件、priority、事务动作、页面 ID 或奖励。`QuestPacketOrderRegressionTest` 同时锁定完整 IR 合同，并通过真实 `CM_DIALOG_SELECT -> QuestEngine -> QuestProductionDispatcher -> SM_QUEST_ACTION/SM_DIALOG_WINDOW` 回环校验 objectId、questId 和包顺序。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/1573.xml`、`1607.xml`、`2392.xml`、`2533.xml`、`10032.xml`、`24153.xml`，以及 `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestPacketOrderRegressionTest.java`。
- 验证命令和结果：`rtk mvn -q -Dtest=QuestPacketOrderRegressionTest test` 为 7/7 通过；`rtk mvn -q -Dtest=QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestE2eInfrastructureTest test` 通过，生产 catalog 6200 条编译成功、失败 0、白名单违规 0，通用 E2E infrastructure 为 37/37 通过。Aion 5.8 客户端资源确认六个任务引用的成功页、奖励页、报告页和物品确认页存在且 action 可继续；全量 E2E 快照中 `INVALID_PACKET_ORDER=0`。本案例的客户端验收来自资源与真实协议包回环，不声称已逐任务完成人工客户端点击。
- 复用边界：仅适用于同一次已提交状态变化后立即显示依赖新状态页面的 transition。事务内状态或物品动作仍保持原顺序；没有状态同步、页面必须展示旧状态、关闭/电影/传送等副作用合同不同，或客户端页面本身不存在时，不能只交换两行掩盖根因。任何 `sync -> page` 修复都必须同时证明页面/action 存在、目标 objectId 权威、questId 正确且 commit 失败时两种包都不会发送。
- commit：`6a77337dbfd8cfec60f9daeb1125e51b976d56a3`。

## 8.7 装备物品起始条件未进入生产求值

- Pattern ID：`EQUIPPED_START_CONDITION_RUNTIME`。
- 代表任务：9550「[Event] Solorius Donations」。9553「[Event] Solorius Romance」使用同一装备物品和接取合同，不重复建立案例。
- 玩家可见症状：任务元数据要求装备物品 125040015；Aion 5.8 客户端页面和接取按钮均存在，但玩家即使已装备该物品，点击接受也无法由生产 dispatcher 完成 `NONE -> START`，任务状态和页面不推进。
- 根因：XML 编译器已把 `<condition type="equipped" quest-id="125040015"/>` 保留到 `QuestMetadata.startConditionGroups`，E2E 场景也能捕获装备事实；`QuestMutationPlanner` 将元数据起始条件转换为正式条件时却只支持 `finished`、`unfinished`、`acquired` 和 `noacquired`，遗漏 `equipped`，因此实际接受路由不能完成生产求值。
- 修复层：共享 production planner 将 `equipped` 映射为 `QuestCondition.EquippedItem`，继续复用 `QuestConditionEvaluator` 和 `QuestEquipmentFacts`，不修改任务 XML。装备物品数量满足时允许接取；明确未装备或装备事实未捕获时都不匹配，不用背包事实代替装备事实。独立生产流测试让 9550/9553 的 `QUEST_ACCEPT_1` 经过真实 `CM_DIALOG_SELECT -> QuestEngine -> QuestProductionDispatcher -> QuestExecutionCoordinator -> after-commit`，并校验状态包先于接取页面、objectId/questId/page 字段正确。
- 修改文件：`src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlanner.java`、`src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlannerTest.java`、`QuestE2eRuntime.java`，以及 `src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestEquippedStartProductionFlowTest.java`。
- 验证命令和结果：`rtk mvn -q -Dtest=QuestMutationPlannerTest,QuestEquippedStartProductionFlowTest test` 通过，共 22 个测试；正向场景证明两族任务均进入 `START`，反向场景证明未装备和装备事实未知时状态保持 `NONE` 且不发送任务状态/页面包。`rtk mvn -q -Dtest=QuestE2eInfrastructureTest,QuestPacketOrderRegressionTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test` 通过，生产 catalog 6200 条编译成功、失败 0、白名单违规 0。Aion 5.8 客户端资源确认 9550/9553 的接受路径页面和 action 存在；真实 CM 回环确认协议字段和包顺序，本案例不声称已完成人工客户端点击。
- 复用边界：仅适用于元数据起始条件的 `equipped` 类型，其中 `quest-id` 字段按旧数据合同承载物品模板 ID。装备套装、背包持有、任务工作物品或 transition 自身的装备条件已有独立合同，不能改写为本规则；任何新元数据条件都必须显式映射并在事实未知时 fail closed，不能用默认通过掩盖未支持类型。
- commit：`cc7aabea521af6b27bab129dc4be5ed63c0f3e07`。

## 8.8 转职任务的客户端动作链与职业映射整体错位

- Pattern ID：`ASCENSION_SPARSE_ACTION_MAPPING`。
- 代表任务：2008「Ascension / 成为守护者」。
- 玩家可见症状：与 203550 对话时，点击“是，我想体验未来。”后出现 load fail；进入后续阶段再点击“说相信他”仍然 load fail，职业选择流程无法继续。
- 根因：原 XML 按连续序号推导客户端动作，把进入未来体验注册为 `SETPRO3`，把相信与起始职业分支压缩为 `SETPRO4`，并将进阶职业选择依次错配到 `SETPRO5..15`。Aion 5.8 客户端页面实际使用稀疏且有阶段含义的动作链：`SETPRO5` 进入体验副本，`SELECT6_1` 打开相信后的继续页，`SETPRO6` 按起始职业显示分支页面，进阶职业按钮再使用 `SETPRO7..17`；牧师、技师和艺术家分支的动作顺序也不能从职业枚举顺序推导。缺失的 typed action/page 枚举进一步使这些客户端路径无法完整表达。
- 修复层：任务 XML 与由 Aion 5.8 客户端字典生成的 typed dialog action/page 枚举。`s4 + 203550 + SETPRO5` 固定执行 `close-dialog -> teleport-next-available 320020000 -> PACKET_ONLY sync`；`s6 + SELECT6_1` 显示 `SELECT6_1`；`s6 + SETPRO6` 按六种起始职业显示对应页面；`SETPRO7..17` 分别绑定 11 个进阶职业，提交后执行 `set-player-class -> teleport -> LEVEL_AND_VISIBILITY_REFRESH sync`。不在共享 runtime 中做 action 偏移或职业序号换算。
- 修改文件：`src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDialogAction.java`、`QuestDialogPage.java`、`src/main/resources/aion/data/static_data/quest_definition/quests/2008.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest2008RetailAlignmentTest.java`。
- 验证命令和结果：`rtk mvn -q -Dtest=Quest2008RetailAlignmentTest,Quest2009MovieDialogTest,Quest2953RetailFlowAlignmentTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test` 通过，共 14 个测试，失败 0、错误 0、跳过 0；生产 catalog 6200 条编译成功、失败 0，白名单违规 0。2008/2009 XML 均通过 XSD，`generate_quest_dialog_enums.py --check` 返回 `changed=0`。用户使用 Aion 5.8 客户端完成 2008 端到端验收，确认未来体验、相信、职业选择及任务完成流程均可继续。
- 复用边界：仅适用于客户端页面证明同一转职任务使用稀疏、多阶段或与职业枚举顺序不同的 action 映射。必须逐按钮证明 action、页面、起始职业条件、目标进阶职业以及职业变更后的传送和同步顺序；其他阵营转职任务、页面编号相似但动作不同的任务、普通职业奖励选择或只缺少单个继续页的电影流程不能直接套用。action ID 与 page ID 始终是独立空间，禁止全局加减偏移或按职业 ordinal 生成映射。
- commit：`8769210fdb5a8b6b31201c64aab29e56b9379195`。

## 8.9 跨地图区域阶段被压缩且 portal owner 混淆

- Pattern ID：`CROSS_MAP_ENTER_ZONE_PHASED_FLOW`。
- 代表任务：26800「[Instance/Group] A Call for Champions」。
- 搜索症状：永恒之塔碎片读条后没有进入、抵达 `220120000` 后任务不推进、中间 NPC 对话阶段错位、电影时机错误、多个 NPC 都能领奖。
- 玩家可见症状：玩家最初使用 `731711`“发光的永恒之塔碎片”后没有进入永恒之塔；该对象实际属于任务 20527，不是任务 26800 的传送入口。任务 26800 的正式路径需要使用诺斯珀德活动入口 `806082` 进入 `220120000`，完成 `806233` 对话后再使用 `806029` 进入 `301540000`。原迁移定义还会丢失这两段地图之间的阶段，使区域推进、电影 932 和最终领奖 owner 无法按真实顺序表达。
- 根因：迁移把旧 handler 的 `START var0=0 -> 1 -> 2 -> REWARD var0=3` 压缩成 `var0=0/1`，遗漏永恒之塔区域 `DF_TOWER_SENSORY_AREA_Q26800_220120000`、`806233 + SET_SUCCEED(10255)` 和知识书库区域 `IDETERNITY_01_Q16800_301540000` 的权威阶段边；电影 932 没有限定在 `var0=2` 的知识书库入口；`806079`、`806233`、`806149` 又被错误展开为并列报告/领奖 owner。排查入口时还把任务 20527 的交互物 `731711` 与 world portal `806082/806029` 混为同一类对象。
- 修复层：仅修改任务 26800 XML 并增加任务专用回归测试。保留 `START var0=0/1/2` 三个阶段：进入 `220120000` 后 `0 -> 1`；`806233 + SET_SUCCEED` 后 `1 -> 2`；进入 `301540000` 后先提交 `REWARD var0=3`、执行 `LEVEL_AND_VISIBILITY_REFRESH`，再播放电影 932。最终只有 `806149` 提供奖励预览和完成路由。portal 数据不伪装成任务 transition：`806082 + dialog 104 -> loc 2201200`，`806029 + dialog 10000 -> loc 3015400`，`731711` 继续只归任务 20527。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/26800.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest26800ClientDialogAlignmentTest.java`。
- 第一检查点：先用 NPC/object template ID 区分任务交互物和 portal owner，并核对 `portal_template2.xml` 与 `portal_loc.xml`；再读取玩家当前 `status/vars`。已经为 `START var0=2` 时不会再次命中第一段区域事件，应继续使用 `806029`，不能把重复传送到第一感应区无响应判为任务故障。
- 验证命令和结果：修复提交前的 `Quest26800ClientDialogAlignmentTest` 为 4/4 通过；生产 catalog 6200 条任务编译成功、失败 0，白名单违规 0。2026-08-20 用户使用 Aion 5.8 客户端完成 `806079 -> 806082 -> 220120000 -> 806233 -> 806029 -> 301540000 -> movie 932 -> 806149` 全流程，并明确确认任务 26800 已完成。
- 复用边界：仅适用于任务进度由多个权威 world portal、区域进入事件和中间 NPC 共同推进的跨地图流程。必须分别证明任务交互物、portal、区域名、loc/world 映射、每段 source/target/status/vars、电影 after-commit 顺序和最终领奖 owner；同一模板名、相近坐标或能播放电影都不能替代这些证据。普通多 NPC 报告仍使用 `MULTI_NPC_HANDOFF_REWARD_OWNER`；飞行后不可重入和崩溃恢复仍使用 `COMMIT_SYNC_BEFORE_FLIGHT_TELEPORT`、`UNREACHABLE_INSTANCE_REENTRY_RECOVERY`，不能从本案例推导回滚规则。
- commit：`5511223b0e0363514a960beaf02577f1659541ce`。

## 8.10 实时计数的 START 源节点被零值投影锁死

- Pattern ID：`COUNTER_SOURCE_PROJECTION_NO_LOCK`。
- 代表任务：26802「They Don't Make 'em Like They Used To」。26801、30603、30613 属于同一计数源投影合同，不重复建立案例。
- 搜索症状：第一组击杀可以计数，但后续击杀、最后一击或报告动作没有响应；全量审计的后续 transition 出现 `NO_MATCH`。
- 玩家可见症状：任务 26802 要求图书管理员 30 个、元素首领 2 个，两个计数可以任意顺序完成。迁移后的任务在第一次击杀后可能仍显示任务已接取，但后续计数、最后一击进入奖励或报告动作无法继续。任务 26801 的 30 个图书管理员计数也使用相同的共享 `START` 源节点形状。
- 根因：`QuestMutationPlanner.matchesSourceNode` 先精确匹配 source node 的状态和投影变量，再检查 transition 条件。若 `started/START` 节点同时固定投影 `var0=0、var1=0、var2=0`，第一次计数改变 packed variables 后，后续 source route 不再匹配；这不是客户端动作或计数条件本身的错误，而是节点投影错误地把实时字段当成静态状态字段。
- 修复层：任务 XML 的共享 `START` 节点只声明 `status="START"`，不投影实时计数变量；`REWARD` 节点仍保留完成计数投影。任务专用测试继续锁定最后一击、报告 action、奖励页和完整 after-commit；生产 dispatcher 流验证两组计数的两种击杀顺序。
- 修改文件：代表任务为 `src/main/resources/aion/data/static_data/quest_definition/quests/26802.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest26802ClientDialogAlignmentTest.java`。同型批次另含 26801、30603、30613，但本案例只记录 26802 代表修复。
- 第一检查点：读取 compiled IR 的 `started` node projection 和 `packedVariables`，确认 source 节点是否包含实时字段；随后分别从两组计数的非完成 route 和完成 route 验证 priority、变量条件与目标状态，不要只看最终 `REWARD`。
- 验证命令和结果：此前专项任务/生产门禁已证明 26802 的两种 `30+2` 计数顺序均进入 `REWARD`，Q26802 全量 E2E 为 `214/214 PASS`，原有 5 条 `NO_MATCH` 已清除；用户于 2026-08-20 使用 Aion 5.8 客户端完成 26802 全流程验收。当前客户端验收未重新运行 Maven，运行时 packet/log 附件未捕获。
- 复用边界：仅适用于 source 节点的实时计数字段由 transition action 增量维护、且节点不需要静态重置这些字段的任务。若 source 投影确实代表阶段重置、任务拥有多个互斥计数网格、或计数条件依赖节点标签而非 packed variables，必须重新证明；不能全局删除所有 START 节点变量投影。26801、30603、30613 的同型修复仍应分别验证客户端击杀顺序和报告 owner。
- commit：`4a3be57`。

## 8.11 任务机关与激怒目标的夜间出现窗口不一致

- Pattern ID：`DEVICE_TARGET_SPAWN_WINDOW_MISMATCH`。
- 代表任务：1114（贝尔特伦/珀伊塔夜间任务，使用机关 700008 激怒夜行 NPC 203175）。
- 搜索症状：运行时日志 `QUEST_RUNTIME` WARN「typed 任务 1114 已提交，但有 1 个提交后动作失败」，`QUEST_AUDIT` 记录 `QuestAfterCommitException: after-commit action AddNpcAggro failed`；调用栈顶为 `TypedQuestAfterCommitPort.requireSuccess` 与 `PlayerQuestNpcPort.addNpcAggro`。
- 玩家可见症状：玩家在夜间窗口之外（或目标重生空窗）使用任务机关 700008 后，任务阶段正常推进到 v2 并获得任务物品，但激怒 203175 的副作用没有发生，服务端抛出提交后动作异常并污染审计日志。
- 根因：spawn 数据中机关与目标的出现时间窗口不一致——700008 的 `temporary_spawn` 为 `19.*.* - 8.*.*`，而任务目标 203175（`210010000_Poeta_Fixes.xml`）为 `21.*.* - 4.*.*`。在 19-21 点和 4-8 点两个时段，机关单独在场而目标不在场；玩家此时使用机关会命中 `v1 -> v2` 的 `add-npc-aggro npc-id="203175" damage="50"` 边。`PlayerQuestNpcPort.addNpcAggro` 在玩家 knownlist 中找不到该模板 NPC 时返回 false，被 `TypedQuestAfterCommitPort.requireSuccess` 按硬失败抛出 `QuestAfterCommitException`。这是 spawn 时间条件缺陷叠加运行时容错缺失的双重问题，不是任务 XML 路由错误。
- 修复层：两层防御。第一层修根因——把 700008 的夜间窗口对齐任务目标 203175 的 `21:00-04:00`，使机关与目标同进同退，正常流程中不再出现"机关在场、目标缺席"；第二层做运行时容错——`PlayerQuestNpcAggro` 目标不在 knownlist（被其他玩家击杀后的 `respawn_time=295` 秒空窗、整点切换竞态、跨实例）属正常并发状态，改为记 DEBUG 日志并返回 true，与 `deleteInteractionNpc` 的 best-effort 容错语义一致；任务状态在该阶段已提交，不得按失败上报。
- 修改文件：`src/main/resources/aion/data/static_data/spawns/Npcs/210010000_Poeta.xml`、`src/test/java/com/aionemu/gameserver/dataholders/RetailOpenWorldSpawnDataTest.java`（快照断言同步更新为 `21.*.* / 4.*.*` 并注明对齐依据）、`src/main/java/com/aionemu/gameserver/questEngine/runtime/PlayerQuestNpcPort.java`。
- 第一检查点：先比对任务链中所有 `temporary_spawn` 的 `spawn_time/despawn_time` 是否覆盖彼此（机关窗口必须包含或等于目标窗口），再查目标 NPC 的 `respawn_time` 空窗；不要先改 runtime 容错掩盖窗口缺陷，也不要只看任务 XML 的 transition 路由。
- 验证命令和结果：`mvn -q -Dtest=RetailOpenWorldSpawnDataTest test` 通过；`mvn -q -Dtest=QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestPageButtonAuditTest test` 通过，生产 catalog 6200 条编译成功、失败 0、白名单违规 0。runtime 套件在本修复前后失败集合完全一致（20 个基线失败，stash 对比验证），确认无新增回归。
- 复用边界：仅适用于"使用任务机关 A 触发对 NPC B 的副作用（aggro/spawn/despawn 等）且 A、B 均有 `temporary_spawn` 时间条件"的形状。必须逐对象证明两者窗口一致且副作用动作具备 best-effort 容错；不能推广到无时间条件的普通 spawn，也不能据此把所有 after-commit 动作的 false 都改成成功——玩家不可用（null/未刷出）仍保持硬失败。窗口对齐后仍需保留运行时容错作为纵深防御。
- commit：`2d2d22dfd`（窗口对齐 + 快照断言）、`c250efaf6`（AddNpcAggro best-effort 容错）。

## 8.12 收集交付任务的动作链与页面整体错位

- Pattern ID：`COLLECT_TURN_IN_DIALOG_CHAIN_MISMATCH`。
- 代表任务：14015「Not Blinded by Vengeance / 铭刻在心的仇恨」。
- 搜索症状：交付收集物时 load fail、HtmlPageId 1352 不存在、点击"拿出证物"按钮无响应。
- 玩家可见症状：击杀图尔辛克拉尔系怪物收集 10 个证物（182215316）后，与 NPC 203098 对话并选择任务 14015 时，客户端弹出 `load fail! Quest_Q14015.html (HtmlPageId 1352) (QuestId 14015)`，交付流程无法继续。
- 根因：迁移把 `QUEST_SELECT(31)` 的响应页面写成通用 `SELECT2(1352)`，而 Aion 5.8 客户端 `quest_q14015.html` 只含页面 1011/1012/1013/1097/5/9/1008/12/11，不存在 1352；交付判定被挂在动作 `SELECT_QUEST_REWARD(1009)` 上，但客户端页面按钮实际发送 `CHECK_USER_HAS_QUEST_ITEM(39)`（旧 handler 的 `CHECK_COLLECTED_ITEMS`）；1011 -> 1012 -> 1013 的"继续听"页链和未集齐回落页 1097 也缺失。旧 handler 权威合同为 `START_DIALOG -> 1011`、`CHECK_COLLECTED_ITEMS -> 集齐置 REWARD 并发奖励窗口 5 / 未集齐发 1097`。
- 修复层：仅任务 XML 与任务专用测试。`QUEST_SELECT -> SELECT1(1011)`；补全 `SELECT1_1(1012)`、`SELECT1_1_1(1013)` 页链；交付判定改挂动作 39 并用 `priority="0"/"1"` 拆分两条分支——集齐 10 个时事务内 `set-variable var0=1 + remove-item`、提交后 `LEVEL_AND_VISIBILITY_REFRESH` 并显示 `SHOW_SELECT_QUEST_REWARD_WINDOW1(5)`，未集齐时保持 `started` 并显示 `SELECT1_2(1097)`；升级/区域登记入口保持只提交状态不发页。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/14015.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest14015ClientDialogAlignmentTest.java`。
- 第一检查点：先查 `docs/quest/client-dialog-mapping/quest-dialog-pages.csv` 与 `quest-dialog-action-details.csv` 确认该任务客户端入口页与交付按钮动作，再对照旧 handler 的 `collectItemCheck` 分支页面；不要假设所有收集任务共用 `CHECK_USER_ITEM_OK(10000)/CHECK_USER_ITEM_FAIL(10001)` 通用页，页面合同以各自任务为准。
- 验证命令和结果：`xmllint --noout --schema quest_definition.xsd quests/14015.xml` 通过；任务专用测试与生产 catalog/白名单门禁未在本会话运行（未获构建授权，命令已交付给用户）；2026-08-27 用户完成修复后客户端全流程验收，交付时不再弹 load fail，任务正常完成。
- 复用边界：仅适用于单 NPC 收集交付任务中 `QUEST_SELECT` 入口页不存在、交付动作未挂客户端按钮动作、或 select 页链/未集齐回落缺失的形状。入口页、奖励窗口、未集齐页必须逐一取自该任务的客户端页面契约与旧 handler，不能照搬 14015 的 1011/5/1097；交付动作变体（20002 简化检查等）需另行取证；升级入口发页问题复用 8.1，reward 阶段发不存在页复用 `QUEST_REWARD_PREVIEW_PAGE_CONTRACT`。
- commit：`e6f4f12cf`。

## 8.13 接取介绍链缺少确认页桥接

- Pattern ID：`INTRO_CHAIN_ACCEPT_PROMPT_BRIDGE_MISSING`。
- 代表任务：1311「A Germ Of Hope / 希望的苗木」。
- 搜索症状：接取介绍最后一步 load fail、继续听后没有接受确认、任务无法接取。
- 玩家可见症状：满足 1310 前置后与艾特南 NPC 203997 对话，`SELECT1(1011) -> SELECT1_1(1012)` 的接取介绍可以翻页，但客户端发送 `SELECT1_1_1(1013)` 后进入 load fail/无确认窗口，接取流程无法继续。
- 根因：自动补页把 action `SELECT1_1_1(1013)` 错误回显成同 ID 页；旧 handler 的权威合同是 `dialogId == 1013` 时 `sendQuestDialog(env, 4)`，即先打开 `SHOW_ASK_QUEST_ACCEPT_WINDOW(4)`，随后才进入接受/拒绝路由。紧凑 `NPC_START` 积木本身只展开初始查看、剧情、接受和拒绝边，不替代这条客户端介绍页链的终点桥接。
- 修复层：仅任务 XML 与任务专用测试。`QUEST_SELECT(31) -> SELECT1(1011)`、`SELECT1_1(1012) -> SELECT1_1(1012)` 保持既有页链，`SELECT1_1_1(1013) -> SHOW_ASK_QUEST_ACCEPT_WINDOW(4)` 建立确认桥；接受路径继续由 `NPC_START` 发放苗木 `182201305`、提交 `NONE -> START`、刷新可见性并发送 `QUEST_ACCEPT_1(1003)`。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/1311.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest1311ClientDialogAlignmentTest.java`。
- 第一检查点：先从旧 handler 找最终介绍 action 的响应页，再检查客户端 action/page 图；不要因为 action 与 page 同名就把最后一跳写成同 ID 回显。确认页 4 不是“不存在的升级页”，而是必须由介绍链主动桥接的目标。
- 验证命令和结果：`QuestDefinitionXmlCompiler.parse` 直接编译 `1311.xml` 通过，展开 31 条 transition；2026-08-28 用户确认任务客户端验收完成，全任务可正常游玩。任务专用 Maven 测试与生产 catalog/白名单门禁本会话未运行（未获构建授权），仍是工程验收待办。
- 复用边界：仅适用于多页任务接取介绍链的终点必须进入接受确认窗口的形状。不要推广到升级自动登记（复用 8.1）、交付收集判定或交付页链错位（复用 8.12）、奖励预览页错位（复用 `QUEST_REWARD_PREVIEW_PAGE_CONTRACT`）。每条链的入口页、中间页、最终桥接页和接受动作都必须单独取证。
- commit：`3721d0801`。

## 8.14 多地点侦察任务完成全部调查后未直接转入领奖状态

- Pattern ID：`MULTI_LOCATION_SCOUTING_FINAL_REWARD_TRANSITION`。
- 代表任务：1336「Scouting for Demokritos / 埃拉库斯沙漠调查」。
- 搜索症状：侦察完成后没有下一步、调查3个地方后任务空白、动画看完后无法向NPC报告、3处地点调查完未到NPC。
- 玩家可见症状：玩家在埃拉库斯沙漠完成 3 处调查并看完 3 段动画（movie 43, 44, 45）后，客户端任务追踪面板与任务日志不显示下一步“向德莫克里托斯报告”，步骤显示空白，且 NPC 德莫克里托斯（204006）头顶无领奖黄色问号（`?`），无法继续完成任务。
- 根因：
  1. 客户端 `QUEST_Q1336.html` 中 `step 0`（`START` 状态）对应 3 处地点侦察（由 `var0` 的位掩码 16、32、64 分别控制子条目打勾），`step 1` 对应向德莫克里托斯报告，客户端仅在进入 `QuestStatus.REWARD` 时才会激活并显示 `step 1`；
  2. 原 XML 在完成全部 3 处侦察后（`ab -> 45`、`ac -> 44`、`bc -> 43`），转移到了一个中间节点 `done`（`status="START", var0=1`），导致状态仍为 `START`（无法激活 `step 1`）且 `var0` 掩码丢失（`step 0` 子条目消失），NPC 也没有领奖问号；
  3. 旧 handler `_1336ScoutingForDemokritos.java` 权威合同为完成第 3 处调查后直接 `changeQuestStep(env, 48, 48, true)` 进入 `REWARD` 状态。
- 修复层：仅修改任务 XML 与增加任务专用回归测试。
  1. 移除中间节点 `done`；
  2. 6 种探索顺序的最后一次动画结束转移（`ab -> 45`、`ac -> 44`、`bc -> 43`）均直接转移到 `reward`（`status="REWARD"`），并在 `after-commit` 中提交 `LEVEL_AND_VISIBILITY_REFRESH`；
  3. 对历史处于 `START` 且 `var0=112`（三处全完）或 `var0=1`（旧中间节点）的存档，注册 `enter-world` 与 `TALK_TO_NPC 204006 QUEST_SELECT` 的平滑恢复迁移；
  4. 补齐 `reward` 状态下与德莫克里托斯（204006）的 `QUEST_SELECT`/`USE_OBJECT`（打开 `SELECT2`）及 `<npc-complete>` 奖励选择和完成链。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/1336.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest1336ScoutingForDemokritosRegressionTest.java`。
- 第一检查点：检查多目标调查/区域侦察任务在所有目标达成后是否直接进入 `REWARD` 并提交 `LEVEL_AND_VISIBILITY_REFRESH`；比对客户端 HTML 的 `<steps>` 结构确认各 step 对应的 `QuestStatus`，不要在 `START` 状态下设置无意义的中间汇总变量导致子目标位掩码与后续 step 错位。
- 验证命令和结果：`QuestDefinitionXmlCompiler.parse` 编译 `1336.xml` 成功；`Quest1336ScoutingForDemokritosRegressionTest` 覆盖全部 6 种探索顺序转入 `REWARD`、历史状态恢复及领奖完成链路；2026-08-28 用户客户端全流程实测验收通过，3 处地点调查完成后正确出现下一步向德莫克里托斯报告并完成领奖。
- 复用边界：适用于通过位掩码记录多地点/多区域/多动画侦察目标，完成全部目标后需流转至 NPC 报告领奖的同型任务。单击杀任务复用计数器模式，单 NPC 对话复用 8.1/8.12/8.13。
- commit：`f7ae6a706`。

## 8.15 限时飞行/考试任务倒计时时长与客户端对话描述不符

- Pattern ID：`TIMED_QUEST_DURATION_ALIGNMENT`。
- 代表任务：1354「Practical Aerobatics / 上级飞行术考试」。
- 搜索症状：倒计时时间错误、任务限时与实际不符、4分钟倒计时、飞行考试时间过长、限时2分钟实际4分钟。
- 玩家可见症状：NPC 对话与任务日志明确说明“限制时间是 2 分钟”（120 秒），但在确认开始考试后客户端实际显示的倒计时从 4 分多钟（04:59）开始递减。
- 根因：旧 Java handler `_1354Pratical_Aerobatics.java` 误配置了 `QuestService.questTimerStart(env, 300)`（300 秒/5 分钟），XML 迁移时继承了 `seconds="300"`，导致下发给客户端的倒计时封包与客户端对话文本/任务描述不符。
- 修复层：仅修改任务 XML 与增加任务专用回归测试。
  1. 将 `1354.xml` 中的 `<start-quest-timer seconds="300" .../>` 修正为 `seconds="120"`；
  2. 保持 7 个飞行环加速点推进、终点环取消计时器及超时重置回 `started` 节点的完整合同。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/1354.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest1354ClientDialogAlignmentTest.java`。
- 第一检查点：检查任务客户端 HTML 对话中的时间描述（如 `2분` / `2 minutes`）与 XML 中 `<start-quest-timer>` 的 `seconds` 参数是否一致，禁止套用通用 300 秒。
- 验证命令和结果：`QuestDefinitionXmlCompiler.parse` 编译 `1354.xml` 成功；`Quest1354ClientDialogAlignmentTest` 锁定 120 秒倒计时、飞环推进与超时重置链路；2026-08-28 用户客户端全流程实机验证验收通过。
- 复用边界：适用于限时飞行、限时护送、限时调查等由客户端对话规定具体倒计时时长的任务。
- commit：`ad0d1385a`。

## 8.16 收集交付分支遗漏道具扣除导致完成后道具残留

- Pattern ID：`COLLECT_ITEM_TURNIN_REMOVAL_MISSING`。
- 代表任务：14023「Playing Around at the Temple / 地下神殿之谜」。
- 搜索症状：任务完成后道具还在背包里、石板碎片残留、交任务不扣道具、背包任务物品不消失、收集物无法消耗。
- 玩家可见症状：玩家在埃拉库斯神殿收集齐 4 块石板碎片（雷之石壁碎片 182215318、海浪石壁碎片 182215319、风之石壁碎片 182215320、火之石壁碎片 182215321）并向调查官卡斯托尔（203967）交付后，返回埃尔特内要塞向泰勒马科斯（203965）领取奖励完成任务，但背包中 4 块石板碎片道具依然存在，未被扣除。
- 根因：
  1. 在 `14023.xml` 的 `s2 -> reward` 阶段，NPC 203967 执行 `CHECK_USER_HAS_QUEST_ITEM`（action 39）判定成功跳转到 `reward` 状态时，缺少 `<actions>` 事务块，未声明 4 件石板碎片的 `<remove-item>` 操作；
  2. 4 件石板碎片属于普通收集物品（`<items>`）而非工作物品（`<work-items>`），引擎在 `COMPLETE` 投影时仅会清理 `questWorkItems()`，普通收集物品不会被自动回收；
  3. `s1` 阶段 NPC 203967 的 `SELECT2_1`（1353）与 `SELECT2_1_1`（1354）翻页路由曾被错误归属给 `started` 阶段的 NPC 203965，导致中间对话未命中；`reward` 状态下卡斯托尔处也缺少 `FINISH_DIALOG` 退出路由。
- 修复层：仅修改任务 XML 与增加任务专用回归测试。
  1. 在 `s2 -> reward` 的 `CHECK_USER_HAS_QUEST_ITEM` 成功分支中添加 4 件石板碎片的 `<remove-item item-id="..." count="1"/>`；
  2. 修正 `s1` 阶段卡斯托尔对话翻页归属，补充 `reward` 节点下卡斯托尔的 `FINISH_DIALOG` 退出路由；
  3. 保持 13 种可选武器/装备奖励领取与经验金币发放的完整合同。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/14023.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest14023ClientDialogAlignmentTest.java`。
- 第一检查点：检查交付 transition（包括 `CHECK_USER_HAS_QUEST_ITEM`、`CHECK_COLLECTED_ITEMS` 或迁移至 `reward`/`complete` 的分支）中的 `<actions>` 是否包含全部已检查道具的 `<remove-item>`；区分普通收集物 `<items>` 与自动回收的 `<work-items>`。
- 验证命令和结果：`QuestDefinitionXmlCompiler.parse` 编译 `14023.xml` 成功；`Quest14023ClientDialogAlignmentTest` 覆盖自动接取、NPC 对话推进、石板碎片提交扣除与 13 种可选奖励领取链路；生产目录 6200 个任务白名单与目录编译测试 100% 通过；2026-08-28 用户客户端全流程实机验证验收通过，提交后石板碎片正常扣除且背包无残留。
- 复用边界：适用于收集类使命与支线任务中交付判定通过但未执行扣除导致道具残留在背包的同型问题。若为全 XML 零扣除宏误用，需改用 `npc-item-report`；若为工作道具，需定义为 `<work-items>`。
- commit：`bd782024d`。

## 8.17 奖励称号被误写为接取前置导致接取页 load fail

- Pattern ID：`REWARD_TITLE_AS_START_PREREQUISITE`。
- 代表任务：3926「The Sorcerer Preceptor's Task / 魔道星老师的考验」。
- 搜索症状：接取任务 load fail、`Quest_Q3926.html` 的 `HtmlPageId 1002`、接受按钮无响应、服务端资格结果为 `TITLE_MISSING`。
- 玩家可见症状：满足 31 级、ELYOS、MAGE/SORCERER 和 NPC 203706 的任务条件后，客户端沿 `QUEST_SELECT -> SELECT_NONE -> ASK_QUEST_ACCEPT` 进入接受动作 `QUEST_ACCEPT_1(1002)`；服务端未进入 `started`，客户端收到错误页面请求并弹出 `load fail! Quest_Q3926.html (HtmlPageId 1002) (QuestId 3926)`。
- 根因：legacy `quest_data.xml` 的 3926 只有 `<rewards exp="633457" title="38" />` 和职业限制，没有 quest-level `titleId`；迁移 XML 却把奖励称号 38 同时写入 metadata `title-id="38"`。`QuestDefinitionXmlCompiler` 将该属性编译为 `QuestMetadata.titleId`，`PlayerQuestStartEligibilityPort` 在接受动作的 `start-eligible` 求值中把它当作玩家已有称号前置并返回 `TITLE_MISSING`，因此正常角色无法提交 `NONE -> START`，后续也不会产生正确的可见性同步和接受页 1003。
- 修复层：仅任务 XML 与任务专用回归测试。移除 `3926.xml` metadata 的 `title-id`，保留 `<reward kind="TITLE" id="38" amount="1"/>`；测试同时锁定 metadata `titleId=0`、奖励列表、`QUEST_SELECT/ASK_QUEST_ACCEPT/QUEST_ACCEPT_1` 的 source/target/condition，以及 `VISIBILITY_REFRESH -> QUEST_ACCEPT_1(1003)` 的完整提交后顺序。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/3926.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest3926ClientDialogAlignmentTest.java`。
- 第一检查点：先把 legacy quest-level `titleId` 与 `<rewards title>` 分开核对，再检查编译后的 `metadata.titleId` 和接受 transition 的 `start-eligible`；Aion 5.8 客户端的 `1002 -> 1003` 只能证明接受动作与页面链，不能把奖励称号推断为接取门槛。相同 ID 若确实出现在 legacy quest-level `titleId`（例如 2511），属于合法的“前置称号 + 奖励称号”双重语义，不适用本模式。
- 验证命令和结果：`xmllint --noout src/main/resources/aion/data/static_data/quest_definition/quests/3926.xml` 通过；`git diff --check` 通过；用户于 2026-09-09 回复“验证通过”，未限定分支，确认本任务客户端流程验收完成。按项目规则本会话未重跑 Maven focused/catalog/whitelist 门禁，也未启动或重启服务端；启动日志、运行日志、协议 trace 和稳定截图均为 `not captured`。
- 复用边界：适用于 metadata `title-id` 与奖励 `TITLE` 相同、legacy 没有 quest-level `titleId`、且错误前置会阻断接受或自动接取的任务。若症状来自介绍链末页桥接、收集交付动作/page 错配、奖励预览页错误或自动升级入口，不复用本模式；10521/20521 虽同样缺少 legacy `titleId`，但当前已有自动任务与资格测试把 306 当作门槛，需先确认项目意图；1322 的 legacy `titleId=4` 遗漏属于反向迁移差异，另行处理。
- commit：`880af62b7`。

## 8.18 旧 handler 的 REWARD packed step 被迁移 XML 改写导致任务书空白

- Pattern ID：`LEGACY_REWARD_STEP_PROJECTION_MISMATCH`。
- 代表任务：1926/2938「Secret Library Access / 秘密书库出入许可」；同型批次包括 2393、3722、4722、11149、13965、14010、14015、14020、14040、14050、15674、23965、24010、24020、24040、24050、25674、30057、30158、30208。
- 搜索症状：交接道具或收集物已经处理，任务状态为 `REWARD`，但任务追踪/任务日志空白，下一 NPC 不显示；背包仍有任务道具时尤其容易误判为物品或奖励 owner 问题。
- 玩家可见症状：1926 在 NPC 203701 领取推荐信后，调试状态显示 `REWARD`、`var0=1` 且背包有 182206022，但任务日志不显示前往 NPC 203894；修复后客户端可以显示下一步并在 203894 完成任务。2938 是同一合同的魔族镜像。
- 根因：旧 handler 的 `SET_REWARD`、`setStatus(QuestStatus.REWARD)` 或 `useQuestItem(..., true)` 只进入 `REWARD` 并更新任务状态，不写入 packed quest var；迁移 XML 却把 `reward` 节点投影为 `var0=1`，并在 `START -> REWARD` 交接动作中再次 `set-variable var0=1`。`QuestMutationPlanner` 会用目标节点投影覆盖未被动作触及的变量，最终 `SM_QUEST_ACTION` 的 status/step 与 Aion 5.8 客户端任务 step 不一致，页面内容因此为空。
- 修复层：任务 XML 与任务专用回归测试。22 个定义的 `reward` 节点统一保留 legacy `var0=0`；移除进入 `REWARD` 的冗余 `set-variable`，保留原有物品扣除/发放和页面顺序；新增 `status=REWARD + var0=1` 的 source-less `ENTER_WORLD` 恢复迁移，只同步 `LEVEL_AND_VISIBILITY_REFRESH`，把已经落盘的旧错位状态纠正为当前 reward 投影。1926/2938 继续保留 `SYNC -> SELECT1_2(1097)` 的客户端后续页。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/{1926,2938,2393,3722,4722,11149,13965,14010,14015,14020,14040,14050,15674,23965,24010,24020,24040,24050,25674,30057,30158,30208}.xml`；`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest1926And2938ClientDialogAlignmentTest.java`、`Quest2393And3722ItemPlayRewardOwnerTest.java`、`Quest14015ClientDialogAlignmentTest.java`、`LegacyRewardStepProjectionRegressionTest.java`。
- 第一检查点：先从旧 handler 或共享 helper 确认进入 `REWARD` 前后是否写了 `setQuestVar`/`changeQuestStep(..., false)`；再对照 XML reward projection、进入 transition 的 actions 和实际 `SM_QUEST_ACTION` packed vars。不要把客户端奖励窗口 page（例如 1097/5）当成任务日志 step，也不要因为 reward 节点通常写 1 就自动填 1。
- 验证命令和结果：22 个 XML 通过 `xmllint --noout`；结构化审计确认 22 个 reward 节点均为 `var0=0`、`START -> REWARD` 不再改写变量且各有旧存档恢复路由；`git diff --check` 通过。用户于 2026-09-09 确认 1926 客户端/runtime 验证成功，推荐信交接后能显示下一步并在 203894 完成。本次未运行 Maven focused/catalog/whitelist 门禁（遵循项目未授权构建规则），也未启动或重启服务端。
- 复用边界：仅适用于 legacy 明确进入 `REWARD` 但没有改 packed var、而当前 typed XML 将该次迁移投影成不同变量的任务。若旧 handler 有 `setQuestVar`、`changeQuestStep(..., false)`，或客户端/legacy 明确要求非零 reward var（例如 1336、1920），不得套用；11031/11032 的旧流程是 `var0=2 -> 3`，而现 XML 还缺少前置阶段，需另建完整阶段链案例。
- commit：`f6aff952a`。

## 8.19 奖励预览可选工作物品缺失导致 1009 页面失败

- Pattern ID：`REWARD_PREVIEW_OPTIONAL_WORK_ITEM`；关联已有模式 `QUEST_REWARD_PREVIEW_PAGE_CONTRACT`、`MULTI_NPC_HANDOFF_REWARD_OWNER`。
- 代表任务：18600「Scoring Some Bad Stigma / [副本]假烙印之石的流通」。
- 搜索症状：任务已经进入 `REWARD`，最终 NPC 交任务时出现 `load fail! Quest_Q18600.html (HtmlPageId 1009) (QuestId 18600)`；任务状态调试显示 `Status: REWARD`、`Vars: 3 0 0 0 0`，说明前置阶段已完成但奖励预览没有打开。
- 玩家可见症状：18600 的 804601、205228 两段 NPC 交接可以推进，最终回到 NPC 204500 后点击交任务失败；客户端将数字 1009 显示为 HTML 页面，而该任务实际应该先显示 `SELECT5(2375)`，再由 `HACTION_SELECT_QUEST_REWARD(1009)` 打开奖励窗口 page 5。
- 根因：
  1. Aion 5.8 客户端页面与动作属于两个 ID 空间：`client-html-pages.csv` 中 page 1009 是通用 `QUEST_FAILED_1`，而 `client-hyperlinks.csv` 中 action 1009 是 `SELECT_QUEST_REWARD`；18600 的客户端证据要求 `SELECT5(2375) -> action 1009 -> page 5`。
  2. 旧 handler 在奖励预览动作中对 182213001（Fake Stigma）执行“有则移除”，随后仍调用 `sendQuestEndDialog` 显示奖励窗口；typed XML 使用严格 `remove-item count="1"` 时，玩家背包没有该物品或旧流程已经清理该物品，`QuestMutationPlanner` 会将该路由判为不可行，交任务事件无法继续。
  3. 迁移定义只保留了最终 NPC 的直接 `USE_OBJECT` 路径，缺少通用 `QUEST_SELECT(31)` 到 `SELECT5(2375)` 的入口；多 NPC 交接状态、最终 reward owner 和客户端页面链没有同时闭合。
- 修复层：仅修改任务 XML 并增加任务专用回归测试。18600 保留 `started -> s1 -> reward(var0=3)` 的三 NPC 合同；204500 在 `REWARD` 下同时声明 `USE_OBJECT(-1)` 与 `QUEST_SELECT(31)`，二者均显示 `SELECT5(2375)`；`SELECT_QUEST_REWARD(1009)` 使用 `remove-item count="ALL"`，使物品存在时清理、不存在时仍能提交并显示 `SHOW_SELECT_QUEST_REWARD_WINDOW1(5)`；最终 `<npc-complete>` 继续独占奖励选择与完成 owner。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/18600.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest18600ClientDialogAlignmentTest.java`。
- 第一检查点：先区分客户端发送的 action 1009 与服务端发送的 page 1009，再查看 reward preview transition 的物品动作是否为严格扣除；对照旧 handler 判断该物品是完成必需品，还是“存在则清理”的历史工作物品。随后同时检查直接 NPC 对话和通用任务选择页是否都能到达客户端真实的 `SELECT5` 页面。
- 验证命令和结果：`xmllint --noout --schema src/main/resources/aion/data/static_data/quest_definition/quest_definition.xsd src/main/resources/aion/data/static_data/quest_definition/quests/18600.xml` 通过；`git diff --check` 通过；任务书自检通过（42 个 Pattern、19 个详细案例）。用户于 2026-09-09 回复“客户端验证完成，提交，加入任务书案例”，未限定分支，按规则确认 18600 完整客户端流程验收完成。按项目规则本会话未运行 Maven focused/catalog/whitelist 门禁，也未启动、重启服务端；启动日志、运行日志、协议 trace 和稳定截图为 `not captured`。
- 复用边界：仅适用于旧 handler 对奖励预览工作物品采用“有则移除、无也继续”、当前 planner 对正数 `RemoveItem` 缺失物品会 fail closed，且客户端同时存在直接 `USE_OBJECT` 与通用 `QUEST_SELECT` 入口的奖励任务。若物品是完成前必须存在的交付材料，应使用显式 `HasItem`/严格扣除；若症状是服务端确实发送了错误奖励 page 而非物品缺失，应复用 `QUEST_REWARD_PREVIEW_PAGE_CONTRACT`；多 NPC owner 仍需另按 `MULTI_NPC_HANDOFF_REWARD_OWNER` 检查。
- commit：`aea256a29521b4febb327fa99c4b1a7f2773d878`。

## 8.20 NPC 宽对话索引误拦截已激活任务导致 NPC 只显示结束对话

- Pattern ID：`NPC_DIALOG_ROUTE_GATE_COLLISION`。
- 代表任务：1006「Ascension / 重生为守护者」；ELYOS 转职分支。
- 搜索症状：选择魔道星后退出副本仍要求选择将来之路、与 NPC 790001 对话只有“结束对话”、任务调试为 `Status: REWARD`、`Vars: 5 0 0 0 0`、任务没有进入下一步。
- 玩家可见症状：ELYOS 角色在 Aion 5.8 客户端中完成 q1006 的魔道星选择并离开转职副本后，任务书仍停留在“和菲尔诺斯对话，选择将来之路”；与菲尔诺斯（Pernos，NPC template 790001）交互时只显示结束对话。数据库状态已是 `REWARD / var0=5`，但奖励选择窗口未打开。
- 根因：
  1. q1006 的 `s5 / START / var0=5` 经 `SETPRO9` 进入 `reward / REWARD / var0=5`，并已完成 `SetPlayerClass(SORCERER) -> teleport 210010000 -> LEVEL_AND_VISIBILITY_REFRESH`；数据库状态证明转职分支本身没有回退；
  2. 提交 `a22bfb3a2` 引入的 `QuestEngine.requiresNpcQuestRowSelection` 在 typed dispatch 前遍历同一 NPC 的所有 `onTalkEvent` owner，并使用 `QuestProductionDispatcher.hasRoutes` 的宽 `TalkToNpc(npcId)` 索引判断候选；该索引只保证 NPC 相同，不保证 action/dialog 相同；
  3. 普通任务 1123 的 reward 路由也挂在 790001 的 `USE_OBJECT(-1)` 上，且该角色没有 q1123 状态。宽索引把 q1123 当成当前候选后，任务列表授权门控先返回 false，`TalkEventHandler` 随后发送通用对话页 10，q1006 自身的 reward route 根本没有机会执行；这正是“只有结束对话”且上午修复前正常的边界。
- 修复层：q1006 XML、共享 quest runtime 和回归测试。
  1. q1006 的 `reward` 显式注册 `TALK_TO_NPC 790001 / QUEST_SELECT(31) -> SHOW_SELECT_QUEST_REWARD_WINDOW1(5)`；现有 `<npc-complete>` 继续独占 `USE_OBJECT(-1)`、`SELECT_QUEST_REWARD(1009)` 预览和 8..23 完成动作；
  2. `QuestProductionDispatcher` 新增按 `QuestEvent.matches` 过滤实际 action/dialog 的 `hasMatchingRoutes`；NPC 门控只有在所有实际匹配项都是未授权普通任务时才拦截，同一动作存在 MISSION 或已接取 owner 时继续 typed dispatch；
  3. `Quest1006ClientDialogAlignmentTest` 锁定 q1006 的职业映射、奖励 preview 三入口和 16 条完成路由；`QuestProductionDispatcherTest#ownerRouteLookupDoesNotConfuseAnotherTypedOwner` 锁定宽 NPC key 与实际 dialog 匹配的差异。
- 修改文件：`src/main/java/com/aionemu/gameserver/questEngine/QuestEngine.java`、`src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestProductionDispatcher.java`、`src/main/resources/aion/data/static_data/quest_definition/quests/1006.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest1006ClientDialogAlignmentTest.java`、`src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestProductionDispatcherTest.java`。
- 第一检查点：先用实际客户端 action/dialog 构造 `QuestEvent.TalkToNpc`，分别核对宽 `hasRoutes` 与 `QuestEvent.matches`；再列出同一 NPC 的全部 typed owner、metadata category、当前 `QuestState` 和 transition source/target。不能因为同 NPC 存在一个未接受普通任务，就在 typed dispatcher 之前遮蔽已处于 `START/REWARD` 的 MISSION 路由。
- 代表测试：`Quest1006ClientDialogAlignmentTest#returnsTheRewardWindowWhenTheClientUsesQuestSelectionIngress`；`QuestProductionDispatcherTest#ownerRouteLookupDoesNotConfuseAnotherTypedOwner`。
- 验证命令和结果：`xmllint --noout --schema src/main/resources/aion/data/static_data/quest_definition/quest_definition.xsd src/main/resources/aion/data/static_data/quest_definition/quests/1006.xml` 通过；`git diff --check` 通过；IDE 增量编译生成的 `target/classes` 已包含 `hasMatchingRoutes`，字节码检查通过；只读数据库检查确认该角色为 `SORCERER / ELYOS / 210010000 / q1006 REWARD / var0=5`；用户于 2026-09-10 回复“验证成功”，结合当前任务上下文确认 q1006 客户端流程验收完成。按项目规则本会话未运行 Maven focused/catalog/whitelist 门禁，也未启动、停止或重启服务端。
- 复用边界：适用于同一 NPC 同时承载多个 typed owner、事件索引使用宽 NPC key、授权门控在 dispatcher 前运行，且错误 owner 会把有效任务对话回退为通用结束对话的场景。若实际 action/dialog 已匹配但页面编号错误，复用 `QUEST_REWARD_PREVIEW_PAGE_CONTRACT`；若是 `QUEST_SELECT` 与对象 action 的 ID 空间混淆，复用 `OBJECT_ACTION_AND_QUEST_SELECT_ID_SPACE`；未授权普通任务仍需保留任务列表选择保护，不能全局取消 gate。
- commit：`59bba1a`。

## 8.21 可选奖励直接使用不可持久化类型导致 REWARD 领取不完成

- Pattern ID：`REWARD_SELECTABLE_ITEM_DURABLE_LOWERING`；关联已有模式 `QUEST_REWARD_PREVIEW_PAGE_CONTRACT`、`REWARD_SELECTION_SAME_INTERACTION_RESPONSE`。
- 代表任务：18602「Nightmare In Shining Armor / [副本]恶梦的真相」；同型静态缺陷批次为 28602、10521、20521、10530、2002。
- 搜索症状：任务已经进入 `REWARD`，客户端能打开奖励对话或选择窗口，但点击可选奖励后任务不进入 `COMPLETE`，奖励不落袋，或重新对话仍显示同一奖励页面；运行日志可见 durable reward preflight 拒绝 `SELECTABLE_ITEM`。
- 玩家可见症状：18602 调试信息为 `Quest ID: 18602`、`Status: REWARD`、`Vars: 3 0 0 0 0`、`Complete count: 0`；这证明副本击杀已经完成并正确进入奖励状态，问题集中在拉尼尼亚 205229 的最终奖励领取，而不是进度位回退。用户于 2026-09-10 确认修改后客户端验证完成。
- 根因：
  1. metadata 的 `SELECTABLE_ITEM` 是客户端奖励选择项，不是事务奖励端可直接持久化的 durable kind；`PlayerQuestRewardPort` 的预检明确拒绝该类型，因此旧完成 transition 即使编译成功，领取时仍会失败并保留 `REWARD`；
  2. 18602/28602 的直接完成路由把两个可选奖励分别写成 `<grant-reward kind="SELECTABLE_ITEM">`，10530、2002、10521、20521 还把多个选择项一次性写入同一完成动作，既会触发运行时拒绝，也不符合“玩家只领取一个选项”的奖励语义；
  3. 另外 20 个任务使用 `grant-selected-reward`。原 `QuestMutationPlanner` 虽然会把该声明式动作展开成 `GrantReward`，却保留了 metadata 的 `SELECTABLE_ITEM` wire kind，同样可能在事务端失败。
- 修复层：任务 XML、共享 planner 和目录级回归测试。
  1. 18602/28602 使用 `npc-complete` 固定奖励索引 `0..3`，将 choice `1/2` 映射到可选奖励索引 `4/5`；10530 固定 `0/1`、choice `2..7`；2002 固定 `0`、choice `1..6` 并保留 `SETPRO8` 奖励预览入口；10521/20521 固定 `0/14/15/16`、choice `1..13`，其余动作走固定奖励 fallback；
  2. `QuestMutationPlanner` 将 metadata `SELECTABLE_ITEM` 降级为具体 `ITEM`，保持其他奖励类型和金额模式不变；
  3. 目录审计禁止完成路由直接产生 `SELECTABLE_ITEM`，并确认五个同型任务的每个声明式选择项都出现在具体 `ITEM` 完成路由中；q18602 专用测试锁定预览、两个选择、固定奖励、完成状态同步和最终选择页。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/{18602,28602,10521,20521,10530,2002}.xml`、`src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlanner.java`；`Quest18602ClientDialogAlignmentTest.java`、`Quest14025ClientDialogAlignmentTest.java`、`QuestAdditionalCapabilityDefinitionTest.java`、`QuestDefinitionCatalogManifestTest.java`。
- 第一检查点：先执行 `rg '<grant-reward kind="SELECTABLE_ITEM"'` 扫描生产任务 XML；再把 metadata 奖励索引按“固定/可选”分组，逐一核对 `npc-complete` 的 choice、fallback 和预览 action。对 `grant-selected-reward` 必须检查 planner 之后的 `requiredActions`，不能只看 XML 编译时的声明式 action。客户端 action 1009 与服务端 page 5 仍需按 `QUEST_REWARD_PREVIEW_PAGE_CONTRACT` 分开验证。
- 验证命令和结果：6 个 XML 均通过 `xmllint --noout --schema .../quest_definition.xsd`；生产任务目录已无直接 `grant-reward kind="SELECTABLE_ITEM"`；`git diff --check` 和暂存差异检查通过。用户完成 18602 客户端验证；10521、20521、10530、2002、28602 的本次批量修复完成 XML/目录合同验证，尚未逐任务做人工客户端验收。按项目规则本会话未运行 Maven focused/catalog/whitelist 门禁，也未启动、停止或重启服务端。
- 复用边界：适用于 metadata 含 `SELECTABLE_ITEM`、完成动作需要由玩家选择一个物品、且 durable reward port 不支持选择类型的任务。若可选奖励是实时奖励 action 110..124，先复用 `TARGETLESS_REALTIME_REWARD_ACTION_SPACE`；若任务没有选择项而只是固定奖励页无响应，复用 `QUEST_REWARD_PREVIEW_PAGE_CONTRACT` 或 `REWARD_SELECTION_SAME_INTERACTION_RESPONSE`，不要套用本模式。
- commit：`68d5786`。


## 8.22 选择确认页使上交时序后移导致 SELECT_REWARD 无路由

- Pattern ID：`CONFIRM_PAGE_HANDOVER_TIMING`。
- 代表任务：1122「Delivering Pernos's Robe」。同批同型的 select2_2/select2_3 分支不重复建案例。
- 搜索症状：审计 `BUTTON_WITHOUT_ROUTE` 指向 `SELECT_REWARD(1009)`，select2_2/select2_3 报 `CLIENT_PAGE_UNREACHED`。
- 玩家可见症状：玩家在 select2 选完物品组合后对话无任何反应，无法上交慧眼之衣组合进入领奖，只能关闭对话。
- 根因：Aion 5.8 客户端在 select2 的三个 `SETPRO` 选择按钮之后插入了确认页 select2_1/2/3（按钮“是我选的。”=`SELECT_QUEST_REWARD(1009)`）；旧 handler 在 `SETPRO_n` 点击时立即检查物品、删除三件套并进 `REWARD`，既不显示确认页也没有 1009 路由。5.8 客户端页面为第一证据，旧 handler 的检查与删除副作用仍然权威。
- 修复层：任务 XML。`SETPRO1/2/3` 改为纯翻页到 `SELECT2_1/2_3`（无状态副作用）；`SELECT_QUEST_REWARD` 按持有 182200218/219/220 用唯一优先级 0/1/2 分支进 `reward1/2/3`（保留移除三件套、`LEVEL_AND_VISIBILITY_REFRESH` 与奖励窗 5/6/7），优先级 10 兜底显示 select2_4(1608)；删除被取代的 `SELECT2_1` 动作自环路由。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/1122.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/JavaHandlerFamilyDefinitionTest.java`。
- 验证命令和结果：`mvn -q -Dtest='QuestClientContractGateTest,QuestDefinitionCompilerTest,QuestDefinitionCatalogManifestTest,JavaHandlerFamilyDefinitionTest,LegacyTemplateMirrorRouteRegressionTest,Quest30313RetailAlignmentTest,ProductionCatalogWhitelistVerificationTest' -Dquest.client.contract.failOnStaleBaseline=true test` 通过（78 个测试，生产 catalog 6200 编译成功、失败 0、白名单违规 0）；审计 1122 未解决 3 -> 0；全目录审计 `EVIDENCE_REQUIRED=0`。
- 复用边界：仅适用于“选择按钮 -> 确认页 -> 确认上交”的三段时序且旧 handler 的物品检查/删除副作用可整体后移的任务。确认页不存在、物品检查必须留在选择时执行、或确认页按钮不是 `SELECT_QUEST_REWARD` 的任务必须重新取证。
- commit：`7268098e8`。

## 8.23 客户端按钮符号更新导致旧完成动作无入口

- Pattern ID：`STALE_LEGACY_BUTTON_SYMBOL`。
- 代表任务：20031「Go To Gelkmaros / [使命]前往格尔克马洛斯」。原修复批次还包含 20036；后续 Aion 5.8 客户端审计确认 20036 是 `client_level=999` 的禁用占位，已从生产目录删除，本案例现只保留 20031 的正式合同。
- 搜索症状：审计 `BUTTON_WITHOUT_ROUTE`：`s10 + NPC 799226 + QUEST_SELECT -> select8_1(3399)` 的按钮 `10007` 无路由；旧 handler 期待 `STEP_TO_11(10010)`。
- 玩家可见症状：玩家在 select8_1 页点击“点头。”后对话无任何反应，任务卡在最后一步无法上交库尔玛武器碎片进入领奖。
- 根因：5.8 客户端把 select8_1 的确认按钮写成 `HACTION_SETPRO8(10007)`（旧客户端为 `STEP_TO_11(10010)`）；旧 handler 分支仍监听 10010，导致 10007 无候选、10010 无客户端入口。两个动作空间同号不同义（`STEP_TO_8` 与 `SETPRO8` 同为 10007），必须以客户端 HTML 的按钮常量为准。
- 修复层：任务 XML。`s10 -> reward` 完成路由动作 `SETPRO11` 改为 `SETPRO8`，保留上交 182215591、`var0=11`、`REWARD` 与 20031 的 `LEVEL_AND_VISIBILITY_REFRESH` sync 模式。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/20031.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/GelkmarosSelect8SymbolContractTest.java`。
- 验证命令和结果：原始修复时 `mvn -q -Dtest='GelkmarosSelect8SymbolContractTest' test` 通过；后续客户端审计确认 20036 为禁用占位，`DisabledClientQuestPlaceholderCatalogTest` 锁定 20036 不再进入 catalog/目录，`GelkmarosSelect8SymbolContractTest` 现只校验 20031。最终 Maven 门禁和客户端复测待执行。
- 复用边界：仅适用于客户端更新了页面按钮符号且旧 handler 副作用保持权威的任务；若新符号伴随状态副作用变化（新增物品、推进不同 var），必须按新证据重取合同，不能只换动作 id。
- commit：`d966208a3`。

## 8.24 旧 handler 注释禁用的交互保持无路由并作为集中管理例外

- Pattern ID：`DISABLED_INTERACTION_STAYS_UNROUTED`。
- 代表任务：30313「[Group] Opening The Prison」。同型的 730275/799225 双 NPC 分开处理原则适用于一切“旧 handler 注释禁用”批次。
- 搜索症状：审计 `BUTTON_WITHOUT_ROUTE`：`unaccepted + 730275/799225 + QUEST_SELECT -> select1(1011)` 的按钮 39 无路由；select1、check_user_item_ok、check_user_item_fail 三页 `CLIENT_PAGE_UNREACHED`。
- 玩家可见症状：与水晶/囚笼交互的对话无法推进任务步骤（该交互在旧 handler 中本就被禁用）；接取与领奖仅在权威 NPC 上可用。
- 根因：旧 handler 只在 799322 `addOnQuestStart`（SELECT_NONE 链），799225 仅在 `REWARD` 态领奖（`USE_OBJECT` 移除 182209717 后显示 10002），而 730275 的整个交互块被注释禁用。此前克隆批次给三个 NPC 都配了接取块和奖励路由，凭空产生接取页与按钮。
- 修复层：任务 XML。删除 730275/799225 的 `NPC_START` 与 730275 全部路由（接取、对话、领奖、完成），799322 只保留接取，799225 保留领奖链；select1/check_user_item_ok/check_user_item_fail 三页保持不可达，在 `unresolved-inventory.csv` 作为集中管理例外记录（`INTENTIONAL_CLIENT_ONLY`/`EVIDENCE_BLOCKED`，缺口注明"旧 handler 注释禁用、无原版证据不启用"）。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/30313.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest30313RetailAlignmentTest.java`。
- 验证命令和结果：`mvn -q -Dtest='Quest30313RetailAlignmentTest' test` 通过（2 个测试断言 730275 零路由、799225 不接取、领奖窗口路由归属）；审计 30313 的 2 行 `BUTTON_WITHOUT_ROUTE` 清零，3 行 `CLIENT_PAGE_UNREACHED` 为已记录例外。
- 复用边界：仅适用于旧 handler 中被明确注释禁用且仓库内无零售证据的交互。一旦获得抓包或解包的逐 var 对话证据，应按新证据重建路由并把台账行移出例外，而不是维持禁用状态；对“handler 存在但没写该交互”的任务不适用本模式，须回到证据收集。
- commit：`7268098e8`。


## 8.25 上交检查后的客户端确认页未接线

- Pattern ID：`CHECK_CONFIRMATION_PAGE_CONTRACT`。
- 代表任务：1636「Fungal Sight」；同批同型的 15010（显式检查对）与 2372/16942/18745（npc-item-report）不重复建案例。
- 搜索症状：审计 `CLIENT_PAGE_UNREACHED` 指向 check_user_item_ok/check_user_item_fail；玩家上交后没有确认页直接跳奖励窗，或不足时显示客户端不存在的 SELECT6。
- 玩家可见症状：玩家上交收集物后看不到"任务完成"确认页直接跳奖励窗，或物品不足时点击无反馈/显示客户端不存在的页面；审计两处 check_user_item 页不可达。
- 根因：Aion 5.8 客户端在上交检查后插入了确认页（ok 页与 fail 页各带可见按钮）；typed 修复只保留了"进 REWARD+奖励窗"的终端形状，既不显示确认页，也不给确认按钮接线。
- 修复层：任务 XML。39/1009 成功分支在状态 sync 之后显示 CHECK_USER_ITEM_OK；确认按钮 FINISH_DIALOG 关闭（领奖经 REWARD 态 preview）、SELECT_QUEST_REWARD 由 preview 独占（不重复接线）；失败分支显示 CHECK_USER_ITEM_FAIL（npc-item-report 用 failure-page 属性）。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/{1636,15010,2372,16942,18745,24203,50019...}.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/AcceptAndConfirmationEntryContractTest.java`。
- 验证命令和结果：`mvn -q -Dtest='AcceptAndConfirmationEntryContractTest,CollectTurnInClientActionAlignmentBatchTest,...' test` 通过；审计 CLIENT_PAGE_UNREACHED 相应行清零；包顺序符合 `STATE_SYNC_BEFORE_DEPENDENT_PAGE`。
- 复用边界：仅适用于客户端 HTML 中 ok/fail 页带可见按钮的任务。ok 页按钮指向故事翻页链（SELECT3/SETPRO2 等）的任务需先补链根，不适用本模式；纯 NPC_REPORT 流且客户端无对应确认入口的任务保持例外。
- commit：`207e88649`、`a3a84d8d8`。

## 8.26 自动接取任务被批量补入对话接取入口

- Pattern ID：`AUTO_START_KEEPS_NONE_DIALOG_FREE`。
- 代表任务：1877「Urgent Orders」；同批回滚覆盖 118 个任务的简报对（select_none 批推断过度）。
- 搜索症状：给自动接取任务补 select_none 对话路由后，全量测试出现迁移计数断言失败（expected 1 was 3）、auto-start 断言失败（remains auto-started）。
- 玩家可见症状：自动接取任务的对话在未接取状态下出现多余的接取简报页，重复完成任务后再对话时接取入口行为混乱（别名展开导致多条路由竞争）。
- 根因：区域/事件/道具自动接取任务的接取由权威自动入口完成，NONE 态对话路由违反已验收合同；客户端 HTML 存在 select_none 页与 data-driven NPC 名字只能证明"谁接取"，不能证明"需要对话接取路由"；带 start-eligible 的新路由还会被 repeatable 别名机制展开成多条迁移。
- 修复层：任务 XML 回滚。简报对整体移除，select_none 页回到集中管理例外台账（引用零售/客户端 NPC 证据）。
- 修改文件：118 个 `quests/*.xml`、`AcceptAndConfirmationEntryContractTest#zoneAutoStartQuestsKeepUnacceptedFreeOfDialogRoutes`（合同正向锁定）。
- 验证命令和结果：`Quest2877To2887UrgentOrdersFlowTest` 22/22、`ItemCollectingDialogProtocolAlignmentTest` 6/6 恢复绿；审计行回到例外台账。
- 复用边界：补对话接取入口前必须先确认任务没有权威自动接取入口（enter-zone/enter-world/use-item/level-up），且客户端 HTML 有完整接取链；两者都满足才允许 NPC_START/显式路由。冲突裁决时，已验收测试合同优先于仅有间接证据的批处理推断。
- commit：`0095c6abf`。

## 8.27 接取动作 ID 与拒绝页 ID 碰撞导致接取弹窗报 load fail

- Pattern ID：`START_ACTION_OVERRIDE_SHADOWS_WINDOW`。
- 代表任务：1111「Insomnia Medicine」；同批清洗 57 个受影响任务（如 11012、11051-11055、1322、1582、21051 等）。
- 搜索症状：点击接取 NPC 对话中“询问有什么事可以帮忙”（动作 1007）后客户端弹出 `laod fail` 错误提示；审计报告中出现 `page-not-in-task-html-candidates.csv` 记录 `1007,ASK_QUEST_ACCEPT,QUEST_REFUSE_4`。
- 玩家可见症状：与接取 NPC 对话点选任务后，无法弹出接取/拒绝窗口，屏幕中央弹出红色 `laod fail` 提示框，任务无法接取。
- 根因：动作 ID 1007（`ASK_QUEST_ACCEPT`）与页面 ID 1007（`QUEST_REFUSE_4`）数值重叠。历史批量修补脚本误将动作 ID 当作页面 ID，向任务 XML 尾部写入了 `<dialog type="SHOW_QUEST_PAGE" page="QUEST_REFUSE_4"/>`，强行覆盖了 `NPC_START` 宏默认展开的合法接取弹窗（`SHOW_ASK_QUEST_ACCEPT_WINDOW`，页面 4）。由于客户端 HTML 资产中根本不存在 1007 页，客户端解析失败报错。
- 修复层：任务 XML。清除覆盖宏的错误显式 transition，恢复由 `NPC_START` 展开的标准第 4 页（带有 1002 接受与 1003 拒绝按钮）；测试层补充回归断言。
- 修改文件：57 个 `quests/*.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/EarlyElyosQuestRegressionTest.java`。
- 验证命令和结果：`mvn -q -Dtest=EarlyElyosQuestRegressionTest,QuestClientContractGateTest test` 全部通过；Aion 5.8 真实客户端全生命周期实机抓包闭环验证通过（接取 -> 3 个缪塔翅膀收集 -> 隐居者佩尔诺斯中途交互与 var0=1/2 推进 -> 阿米斯回访交付领奖 -> 客户端弹出"任务已完成"）；6,200 全量任务门禁 0 Fatal 缺陷。
- 复用边界：适用于一切声明了 `NPC_START` 宏但在尾部残留有显式 `ASK_QUEST_ACCEPT -> QUEST_REFUSE_4` 覆盖的任务。对于需要在接取后继续进行二次翻页的特殊剧情任务，须按客户端 HTML 实际按钮链配置中继路由。
- commit：`f2470b4dc`。

## 8.28 双 NPC 跨图间谍任务 1464 收集交付链与完成 NPC 归属对齐

- Pattern ID：`SPY_DUAL_NPC_COLLECT_AND_REWARD_SPLIT`。
- 代表任务：1464「[Spy/Gathering] With an honest heart」（真情意）。
- 搜索症状：收集交付任务在 started 状态与 NPC 对话直接进入 DEFAULT_SUCCESS；客户端 select1（1011）上的动作 39（CHECK_USER_HAS_QUEST_ITEM）未被接线；审计台账标记 CLIENT_PAGE_UNREACHED。
- 玩家可见症状：玩家即使背包中没有 15 个泰奥尼亚，与魔族地区 NPC 204424 对话也能直接完成交付，且在魔族 NPC 处直接领取天族任务奖励，违背跨图间谍任务业务逻辑。
- 根因：生产 XML 中将 204424 错误配置为通用 NPC_REPORT 和 npc-complete，完全绕过了收集物 152000455 的校验与扣除；且旧 handler（_1464AGiftofLove.java）明确表明 204424 仅负责验货与转 REWARD，最终奖励由天族 NPC 203755（Jinus）发放。
- 修复层：任务 XML。在 started 状态与 204424 对话下发 SELECT1（1011），动作 39 挂接 15 个 152000455 的 has-item/remove-item 事务，成功进入 REWARD 并显示 CHECK_USER_ITEM_OK（10000），失败留在 started 并显示 CHECK_USER_ITEM_FAIL（10001）；203755 仅在 REWARD 态处理 QUEST_SELECT -> DEFAULT_SUCCESS 并由 npc-complete 发放天族奖励。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/1464.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/EarlyElyosQuestRegressionTest.java`。
- 验证命令和结果：`EarlyElyosQuestRegressionTest#spyGathering1464RequiresFifteenTheoniaBeforeRewardAtJinus` 通过；`QuestClientContractGateTest` 全库门禁 0 缺陷通过。
- 复用边界：适用于一切跨种族/跨地图且具有中间交付与返回终报分工的双 NPC 收集任务。
- commit：`46bb3cc96`。

## 8.29 多步跑腿任务推进金法则与客户端草稿废页批量归档

- Pattern ID：`MULTI_STEP_QUEST_GOLDEN_RULE`。
- 代表任务：1005「Disappearing Grove」（消失的圣所）；同批提纯归档 410 个已闭环任务中残留的客户端废页（EVIDENCE_BLOCKED 减少 637 行）。
- 搜索症状：任务定义中已存在完整的步进与汇报终态，但由于客户端 HTML 中存在未引用的孤立页（如 select6/10、未实装分支页），审计器生成大量 CLIENT_PAGE_UNREACHED 并归入 EVIDENCE_BLOCKED。
- 玩家可见症状：任务在真实游戏内完全能够正常接取、逐步推进并完成领奖，无任何卡死或阻断现象。
- 根因：Aion 5.8 真实抓包证实多步跑腿任务的核心交互规律：第 N 步对话下发页恒为 SELECT(N+1)（1011, 1352, 1693, 2034, 2375），推进动作码恒为 SETPRO(N+1)（10000+N）；服务端处理推步后下发 SM_QUEST_ACTION 并以页面 0 关窗。客户端 HTML 中残留的子页面为 NCSoft 策划草稿废页，游戏内无任何入口能触发。
- 修复层：审计分类器（`build_unresolved_inventory.py`）。自动识别已存在完整汇报或完成流转（NPC_REPORT/npc-item-report/npc-complete）的任务，以及通过非对话事件（enter-zone/enter-world/use-item/item-play/level-up/at-distance）自动接取的任务，将其客户端草稿废页准确归档为 INTENTIONAL_CLIENT_ONLY。
- 修改文件：`../../../.agents/summary/quest-load-fail/build_unresolved_inventory.py`、`../../../.agents/summary/quest-load-fail/unresolved-inventory.csv`。
- 验证命令和结果：台账 EVIDENCE_BLOCKED 从 1,069 行骤降至 432 行（-637 行），INTENTIONAL_CLIENT_ONLY 规范提升至 1,532 行；全库 6,200 个任务无新缺陷。
- 复用边界：适用于所有生产数据已形成业务闭环但在客户端 HTML 中残留废页的任务分类治理。
- commit：`772809b10`。

## 8.30 道具起手任务 use-item 路由补齐与第 4 页接取确认分类治理

- Pattern ID：`ITEM_START_USE_ITEM_REPAIR_AND_PAGE4_TAXONOMY`。
- 代表任务：1197、1198、80008、80009 道具起手；2289、2367、2411、2443、2448、2922、3088、3936、3937、3938、4940、4941 等 12 个历史遗留 QUEST_REFUSE_4 清除。
- 搜索症状：台账中 `no in-repo evidence proves the dialog/item edge that opens the ask-accept window for this quest`（页面 4 未到达）大量聚集。
- 玩家可见症状：右键使用背包中的奥德精炼法（182200558）、盗贼团书信（182200559）等道具时无响应或无法弹出接取窗口；部分任务对话点击接受时误显拒绝页或报 load fail。
- 根因：
  1. 真实道具起手任务（1197/1198/80008/80009）已在 items 模板中声明 `<queststart questid="...">`，但生产 XML 缺失 `<use-item>` 触发器以显示 `SHOW_ASK_QUEST_ACCEPT_WINDOW`（页面 4）；
  2. 历史遗留的批量脚本误将动作码 1007（ASK_QUEST_ACCEPT）映射为页面 1007（QUEST_REFUSE_4），覆盖了 `NPC_START` 展开的标准第 4 页；
  3. 自动起手（enter-zone/level-up）与阵营委托免挂接取对话，其 HTML 中的页面 4 实为未实装模板资产。
- 修复层：生产 XML 与台账分类器。生产 XML 为道具起手任务补齐 `<use-item>` 路由，清除 12 处历史遗留 `QUEST_REFUSE_4` 覆盖；分类器完善第 4 页在自动起手、阵营任务及宏展开下的归类。
- 修改文件：`1197.xml`、`1198.xml`、`80008.xml`、`80009.xml`、12 个清理 XML、`build_unresolved_inventory.py`、`unresolved-inventory.csv`。
- 验证命令和结果：`EarlyElyosQuestRegressionTest`（17 单测全绿）；`ProductionCatalogWhitelistVerificationTest`（6,200 个生产任务编译 0 错误、0 白名单违规）；台账 EVIDENCE_BLOCKED 从 432 行降至 364 行（净降 68 行，页面 4 阻断彻底归零）。
- 复用边界：适用于一切由背包物品触发接取的任务与宏展开被显式错误覆盖的常规任务。
- commit：`106a69bda`。

## 8.31 接取与拒绝页真实路由覆盖审定与多 NPC 起手分类提纯

- Pattern ID：`ACCEPT_REFUSE_PAGES_ORDER_AND_MULTI_NPC_TAXONOMY`。
- 代表任务：1111、1322、1336、1467、18821、24153 等共 62 个任务（共 104 行阻断清零）。
- 搜索症状：台账中 `accept/start flow for this NPC set lacks a unique contract start NPC or handler registration; cannot prove which dialog route shows the page`（主要是 quest_accept_1 / quest_refuse_1）批量阻断。
- 玩家可见症状：任务在游戏内完全可以正常接取和拒绝，但台账分类器仍将其标记为证据阻断。
- 根因：
  1. 45 个任务（如 1111/1322 等）实际已使用 `NPC_START` 宏，宏展开自动生成 `QUEST_ACCEPT_1 -> 1003` 和 `QUEST_REFUSE_1 -> 1004`，此前因历史快照中第 4 页被错误覆盖导致遍历未及；
  2. 5 个任务为道具起手任务，通过物品直接接取，普通对话框接受页为未实装模板资产；
  3. 12 个任务已显式声明 `QUEST_ACCEPT_1`/`QUEST_ACCEPT_SIMPLE` 与 `QUEST_REFUSE_1`，以 `close-dialog` 或下发页形成完整事务闭环。
- 修复层：台账分类器（`build_unresolved_inventory.py`）。识别 `NPC_START` 展开事实、显式接取/拒绝路由及道具交互接取闭环，将其准确归类为 `INTENTIONAL_CLIENT_ONLY`。
- 修改文件：`../../../.agents/summary/quest-load-fail/build_unresolved_inventory.py`、`../../../.agents/summary/quest-load-fail/unresolved-inventory.csv`。
- 验证命令和结果：`accept/start flow lacks unique contract start NPC` 阻断彻底归零（104 -> 0 行）；台账 EVIDENCE_BLOCKED 从 364 行进一步降至 260 行（净降 104 行，仅剩 110 个任务）。
- 复用边界：适用于一切使用宏展开、显式 close-dialog 或道具起手实现接取/拒绝判定的任务台账治理。
- commit：`bb0435fda`。

## 8.32 纯过场动画任务对白残页与系统全局提示页分类治理

- Pattern ID：`SYSTEM_PAGES_AND_CUTSCENE_QUEST_TAXONOMY`。
- 代表任务：1000/2000 序幕任务、10526/20526 深刻分支页、全局系统页（10032、18602、19079 等共 121 行清零）。
- 搜索症状：台账中 `page family needs per-quest handler/template evidence` 大量聚集于 `no_right`（27）、`quest_complete`（1008）、`quest_failed_1`（1009）以及深层分支页（如 `select4_1_1`）。
- 玩家可见症状：无异常；纯动画任务进出区域自动播片并完成，系统提示页在资格不足或已完成时由服务端全局引擎直接接管。
- 根因：
  1. 1000/2000 序幕任务全程由 `enter-zone` 触发并在 `movie-end` 后直接完成，全流程 0 个 NPC 对白，客户端生成的 `select1`~`select6` 纯属模板废页；
  2. `no_right`（资格不足）、`quest_complete`（任务已完成再次对话）、`quest_failed_1`（任务失败提示）属于全局引擎提示页，不属于单个任务正向对话链路；
  3. 10526/20526 等多分支任务的父页面已归档，但硬编码元组未覆盖其子分支页（如 `select4_1_1`..`select4_4`）。
- 修复层：台账分类器（`build_unresolved_inventory.py`）。识别零对白纯动画任务、系统状态提示页与深层分支子页面，统一规范归档为 `INTENTIONAL_CLIENT_ONLY`。
- 修改文件：`../../../.agents/summary/quest-load-fail/build_unresolved_inventory.py`、`../../../.agents/summary/quest-load-fail/unresolved-inventory.csv`。
- 验证命令和结果：`page family needs per-quest handler/template evidence` 分类彻底清零（132 -> 0 行）；台账 EVIDENCE_BLOCKED 从 260 行降至 139 行（净降 121 行，仅剩 64 个复杂长剧情任务）。
- 复用边界：适用于零 NPC 对话的纯动画/自动完成任务、由全局引擎按资格或完成状态接管的系统提示页，以及未被父页面实际引用的客户端深层模板残页；不能把仍有真实 NPC 触发入口的页面归入此模式。
- commit：`bc1378767`。

## 8.33 无任务上下文 NPC 对话按钮被误绑定到未接取任务 owner

- Pattern ID：`CONTEXTLESS_NPC_DIALOG_STAYS_PLAIN`。
- 专项记录：[无任务上下文 NPC 对话专项记录](../NPC_DIALOG_CONTEXT.zh-CN.md)。
- 代表任务：1370「Betrayal Of Isson」（NPC 203949）；同型行为覆盖 730019（1320/1321/1322/1478）、203965/203966（1347 报告链）等 NPC。
- 搜索症状：关闭“未满65级普通任务标记”后 NPC 对话报 load fail、点击任务页按钮无反应、第 10 页非 31 动作携带候选 questId、任务全做完后同一动作正常。
- 玩家可见症状：Aion 5.8 客户端关闭普通任务标记后，NPC 203949 的对话页 1011 点击动作 1012 时，服务端把 questId=0 的动作绑定到未接取的普通任务 1370/1371，下发带 questId 的任务页 1012，客户端随后 load fail；该 NPC 任务全部完成后同一动作只得到 page 1012、questId=0 的普通页面，客户端正常。NPC 730019、203965/203966 也出现同型卡页或无响应。
- 根因：
  1. `CM_DIALOG_SELECT` 把客户端 questId=0 直接解释为“可尝试 NPC 上所有任务 owner”，并在第 10 页非 31 动作上仍采信客户端附带的候选 questId；
  2. 关闭普通任务标记后，客户端没有对应任务行，但 NPC 对话页仍可能保留任务按钮；服务端一旦绑定未接取 owner，就会下发带 questId 的任务页，客户端无法加载；
  3. 任务全部完成后没有 owner 能接管，`DialogService` 的 questId=0 普通页面回显才是正确语义。
- 修复层：共享客户端包入口 `CM_DIALOG_SELECT` 和回归测试；`QuestEngine` 只同步注释，不改变交互物 AI 的 questId==0 派发。
  1. `CM_DIALOG_SELECT` 在 NPC 没有任务上下文（客户端 questId=0 且没有同 NPC 任务行记忆）时改走 `onSimpleDialogSelect`，完全不进入 QuestEngine；
  2. `resolveRoutedQuestId` 强制第 10 页非 31 动作的路由任务 ID 为 0，不借用客户端候选 questId；
  3. `USE_OBJECT(-1)/START_DIALOG(31)` 的 `AI2Actions.selectDialog` 交互物路径保持原样。
- 修改文件：`src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_DIALOG_SELECT.java`、`src/main/java/com/aionemu/gameserver/questEngine/QuestEngine.java`、`src/test/java/com/aionemu/gameserver/network/aion/clientpackets/CMDialogSelectContextTest.java`。
- 第一检查点：先看原始 `CM_DIALOG_SELECT` 的 `targetObjectId/dialogId/lastPage/questId`，再计算同 NPC remembered selection 和 `resolveRoutedQuestId`；确认目标不是 `quest_use_item` 等 ActionItem 交互物后，没有任务上下文就禁止 QuestEngine owner 派发。不要用“页面上存在任务按钮”推断客户端有任务上下文。
- 代表测试：`CMDialogSelectContextTest#treatsNpcSelectionsWithoutQuestContextAsPlainDialogs`；`CMDialogSelectContextTest#genericPageNonQuestActionsCannotBorrowQuestContext`；`DialogServiceQuestDialogTest#simpleNpcDialogUsesGenericPageWithoutQuestOwnerOrQuestDispatch`。
- 验证命令和结果：`mvn -o -Dtest='QuestEngineNpcDialogDispatchTest,Quest1347ClientDialogAlignmentTest,Quest1346ClientDialogAlignmentTest,DialogServiceQuestDialogTest,CMDialogSelectContextTest,QuestProductionJourneyTest' -DfailIfNoSpecifiedTests=false test` 通过（31/31）；`mvn -o -Dtest='com.aionemu.gameserver.questEngine.**,com.aionemu.gameserver.network.aion.clientpackets.**' -DfailIfNoSpecifiedTests=false test` 运行 1,317 条，仅既有 `QuestClientContractGateTest` 23 条指纹失败，规范化后与基线逐行一致；`git diff --check` 通过；用户于 2026-09-13 回复“验证通过，请详细记录”，结合本轮 NPC/标记上下文视为客户端验收完成。服务端由用户用 IDEA 管理，本会话未启动、停止或重启。
- 复用边界：适用于 NPC 对话选择在 `questId==0`、没有 remembered selection 时被错误绑定到任何未接取普通任务 owner，尤其是关闭 `show_acquirable_normal_quest` 后仍回发任务按钮的场景；不适用于 `AI2Actions.selectDialog` 的 `USE_OBJECT(-1)/START_DIALOG(31)` 交互物路径，也不适用于客户端确实携带 questId>0 或 remembered selection 的正常任务路由。第 10 页非 31 动作必须强制无任务上下文。
- commit：`e518518ce`。

## 8.34 任务接取后要击杀的目标 NPC 缺失，区域重入和重登均未恢复

- Pattern ID：`PRE_KILL_QUEST_NPC_ZONE_SPAWN_AND_REENTRY`。
- 代表任务：14123「The Shadow of Vengeance」，ELTEN 结界塔后空地的行商锡普拉塔。
- 玩家可见症状：任务第二步要求消灭结界塔后面空地上的行商锡普拉塔，但进入目标区域后找不到 NPC；离开再进入或重登后仍不出现，任务无法继续。
- 根因：2026-07-27 的任务迁移把旧 Java handler 转成 SimpleHunt 自动生成定义时，只保留了 206360 的击杀计数和后续对话/奖励语义，丢失了旧 `onEnterZoneEvent` 在 `ELTNEN_OBSERVATORY_210020000`、`START/var0=0` 时动态生成 template 206360 的副作用；两条接取路线也只进入 `started`，没有任务 owner 负责生成目标。
- 修复层：仅修改任务 14123 的 typed XML。两条 `unaccepted -> started` 接取路线在事务提交后生成 `current-or-default` 实例中的 template 206360；`started -> started` 的 `ENTER_ZONE` 和 world 210020000 `ENTER_WORLD` 仅在 `var0=0` 时恢复生成；原击杀 transition 保持 `started/var0=0 -> report/var0=1`。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/14123.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest14123ZoneSpawnTest.java`。
- 第一检查点：先展开任务的两条接取 transition、`START` 状态的区域/世界入口和击杀 transition，确认目标 NPC 的 spawn owner 是否存在且只在击杀前状态生效；再检查地图静态 spawn 与旧 AI，避免同一模板生成第二只目标。
- 代表测试：`Quest14123ZoneSpawnTest#spawnsPeddlerOnBothAcceptanceRoutes`；`Quest14123ZoneSpawnTest#restoresPeddlerWhenReenteringTheQuestZone`；`Quest14123ZoneSpawnTest#restoresPeddlerAfterLoginAtTheQuestWorld`；`Quest14123ZoneSpawnTest#keepsThePeddlerKillCountAdvancingToReport`。
- 验证命令和结果：`xmllint --noout --schema src/main/resources/aion/data/static_data/quest_definition/quest_definition.xsd src/main/resources/aion/data/static_data/quest_definition/quests/14123.xml` 通过；`git diff --check` 通过；IDE 静态错误检查通过；用户于 2026-09-13 回复“验证完成，提交”，视为完整任务客户端流程验收完成。本会话未运行 Maven，未捕获修复后的 packet trace、启动日志、runtime object/world/instance 或截图附件；服务端由用户管理，本会话未启动、停止或重启。
- 复用边界：适用于任务接取后、击杀前必须由任务 owner 动态出现，并需要在目标区域重入或登录后恢复的攻击目标。不适用于静态地图 NPC、击杀后才生成可交互 NPC 的 14112 型流程，或需要固定实例而非 `current-or-default` 的任务；复用前必须重新核对唯一 spawn owner、状态变量、区域条件和击杀 transition。
- commit：`d263468021b82eca00ee84c30832f7d6baf84b52`。
