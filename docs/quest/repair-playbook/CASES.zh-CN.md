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
- 验证命令和结果：`rtk mvn -Dtest=Quest13830To13834TargetlessRewardTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test` 通过，共 8 个测试，失败 0、错误 0、跳过 0；五个 XML 均通过 XSD；`rtk python3 scripts/quest/generate_quest_dialog_enums.py --check` 返回 `changed=0`；Aion 5.8 客户端实测实时奖励可领取并正常完成任务。
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
