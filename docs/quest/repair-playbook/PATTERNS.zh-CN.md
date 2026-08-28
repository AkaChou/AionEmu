# 任务修复 Pattern 指纹与提交索引

[返回任务排查与修复 Playbook](../QUEST_REPAIR_PLAYBOOK.zh-CN.md) ·
[查看已验收代表案例](CASES.zh-CN.md)

下面的提交不是互相独立的技巧，而是一条从客户端证据到状态、协议、AI 和性能的排查链。新 agent 遇到相似症状时，先找对应案例，再读取完整 diff 和测试。

本节只按可复用的“问题模式”记录一个代表任务，目的是为后续修复提供证据和实现参考，不维护任务清单、验收名单或覆盖数量。后续任务与已有案例的症状、根因、修复层和修复合同相同时，不追加任务 ID、不修改案例正文，也不新增重复案例；问题或修复模式实质不同时才建立新案例。

## 结构化检索指纹

案例正文仍以一个已验收代表任务为准；下面的指纹只负责让相似问题可被检索命中，不是任务验收清单。匹配后必须继续读取代表提交和测试，并按症状、根因、修复层和修复合同重新判断复用边界。

| Pattern ID | 症状关键词 | IR / owner 指纹 | 第一检查点 | 代表证明 |
|---|---|---|---|---|
| `LEVEL_UP_AUTO_START_NO_DIALOG` | 升级后自动接取弹 load fail、page 4 不存在、NPC 首次对话页错误 | `LEVEL_UP` 只提交 `NONE -> START` 并刷新可见性，不发送接取 HTML；NPC `QUEST_SELECT` 单独显示客户端存在的首个页面 | 自动接取 transition 与 NPC transition 是否被错误合并；旧 handler 是否只更新状态 | `d3b28d2af`、`Quest38001LevelUpDialogTest#matchesLegacyLevelUpAndNpcDialogContract` |
| `LEVEL_UP_MULTI_NPC_PHASED_DIALOG` | 升级接取 load fail、第一 NPC 可点但第二 NPC 卡住、最终在错误 NPC 领奖 | 自动接取不发页面；多个 NPC 按显式中间状态和各自客户端 action 链推进；最终 reward owner 独占完成路由 | 每个 NPC 的 source/target/vars、关闭点、下一阶段入口和最终 owner | `76b0894`、`Quest1920And2945ClientDialogAlignmentTest#quest1920MatchesLegacyAndClientDialogContract` |
| `TARGETLESS_REALTIME_REWARD_ACTION_SPACE` | 实时奖励界面能选但领取无响应、任务不完成 | targetless reward action 使用独立动作空间并映射到对应奖励槽；普通和实时动作共享完成语义但不做全局 remap | 原始 `CM_DIALOG_SELECT` action、客户端可见槽位、生产 targetless 索引和奖励索引 | `4a23cf0a0`、`Quest13830To13834TargetlessRewardTest#compilesNormalAndRealtimeTargetlessRewardsWithClassRewardsAndCloseOrder` |
| `REWARD_SELECTION_SAME_INTERACTION_RESPONSE` | 第一次点击奖励无响应、第二次才出现奖励页 | 进入 `REWARD` 的同一次 transition 在状态提交后必须返回奖励窗口、任务页或关闭响应 | 所有 `SELECT_QUEST_REWARD -> REWARD` route 的 target 与完整 after-commit | `906c08e92`、`QuestDefinitionCatalogManifestTest#rewardSelectionTransitionsRespondInTheSameInteraction` |
| `QUEST_SIDE_EFFECT_AI_RETAIL_FALLBACK` | 击杀或生成后目标 NPC 不出现、自定义任务副作用消失 | retail pattern 没有等价任务副作用时保留证据化 scripted AI；不得全局禁用 retail pattern | AI 生命周期/战斗阶段副作用、零售 pattern 动作和明确 fallback 集合 | `906c08e92`、`AI2EngineRetailSelectionTest#selectsCompleteRetailPatternsButPreservesScriptedActionItemProtocols` |
| `MOVIE_CONTINUATION_RESPONSE` | 继续听、电影重复播放、动画结束仍是原按钮、点击后无下一页 | 同状态电影 self-loop 必须在 `play-movie` 后按权威合同显示后续页、关闭窗口或推进状态，不能只有 movie 副作用 | 客户端当前页 action、电影 transition 的完整 after-commit、后续页是否存在 | `8b058d4b4`、`Quest14047ClientDialogAlignmentTest#returnsFromMovie421ToTheStep11PageAndThenAdvancesToStep5` |
| `COMMIT_SYNC_BEFORE_FLIGHT_TELEPORT` | 飞行后状态落后、断线后客户端位置与任务阶段不一致 | 状态事务提交后按 `sync -> close -> flight` 执行；事务失败不得启动传送 | 传送 transition 的 target 投影、after-commit 顺序和失败路径 | `8b058d4b4`、`Quest14047ClientDialogAlignmentTest#synchronizesCommittedProgressBeforeFlightsAndTheFinalMovie` |
| `UNREACHABLE_INSTANCE_REENTRY_RECOVERY` | 崩溃、断线或实例重建后留在不可达阶段，重登无法继续 | `ENTER_WORLD` 回到普通世界中最近的可重入阶段并重建整段路径；不在 `LOG_OUT` 重复回退 | 当前持久阶段是否依赖已消失实例对象、恢复目标是否可重新触发前置传送 | `8b058d4b4`、`Quest14047ClientDialogAlignmentTest#rollsBackUnreachableFlightStepsOnRelogin` |
| `QUEST_SCOPED_NPC_SEARCH_ALIAS` | GM 寻找同名 NPC 传送到普通模板而非任务模板 | 模板别名解析必须同时限定任务、状态和阶段，其他调用保持原模板 ID | 客户端请求模板、同名候选、任务状态谓词和非目标场景 | `8b058d4b4`、`CMObjectSearchTest#resolvesTheQuestAcestesAliasAtTheFirstAndReportStages` |
| `THRESHOLD_TRANSFORM_DEATH_FALLBACK` | Boss 被秒杀后不变身、最终击杀事件永远不出现 | 血量阈值和死亡路径共享同一幂等门，最多生成一次最终形态；任务只监听权威最终形态 | 静态入口形态、阈值回调、死亡回调、AI 选择和任务 kill route | `8b058d4b4`、`Betrayer_IcaronixAI2Test#spawnsFinalFormOnceWhenThresholdAndDeathFallbackBothRequestIt` |
| `QUEST_NPC_STATIC_SPAWN_DEDUPLICATION` | 同一位置出现两只同名任务 NPC、对话或寻找命中错误对象 | 静态 spawn、任务 spawn 和普通模板中只保留权威 owner；同模板名不代表同 object/template | 地图静态 spawn、任务 after-commit spawn、客户端引用和实际 object ID | `8b058d4b4`、`Quest14047ClientDialogAlignmentTest#keepsOnlyTheQuestPeithoAndSpawnsTheIcaronixEntryForm` |
| `QUEST_REWARD_PREVIEW_PAGE_CONTRACT` | 最终 NPC 点击奖励出现不存在页面的 load fail | reward preview 必须发送客户端存在且旧 handler 证明的奖励窗口，不能把通用成功页或 action ID 当 page ID | reward 状态的 preview action、页面存在性、完成 owner 和旧 `sendQuestEndDialog` | `8b058d4b4`、`Quest14047ClientDialogAlignmentTest#opensTheRewardWindowAtJuditioInsteadOfTheMissingStep3Page` |
| `POST_KILL_QUEST_NPC_SPAWN_AND_REENTRY` | 击杀后任务 NPC 不出现、重登后目标消失、首次奖励点击无响应 | 击杀提交后生成任务 NPC；所有需要继续交互的持久状态在 `ENTER_WORLD` 恢复生成；首次奖励交互同次响应 | kill target、spawn owner、恢复状态集合和奖励 preview | `7a6ad8eca`、`Quest14112LogoutPersistenceTest#keepsPoisonousBubblegutKillProgressAcrossLogout` |
| `DUPLICATE_QUEST_SIDE_EFFECT_OWNER` | 同一任务 NPC 重复生成、XML 与旧 AI 同时推进或清理 | 生产 XML/typed owner 与遗留 AI 只能有一个副作用 owner；移除不再权威的旧入口 | XML after-commit、AI 生命周期方法、catalog owner、相同事件的 runtime 对象/副作用次数 | `7a6ad8eca` 的完整 diff 删除旧 AI `handleDied` 生成 owner；`Quest14112LogoutPersistenceTest#keepsPoisonousBubblegutKillProgressAcrossLogout` 锁定 XML 击杀生成 owner |
| `PERSISTENT_PROGRESS_NO_LOGOUT_ROLLBACK` | 下线后击杀进度回退、重登回到前一步 | 持久 progress 不得在 `LOG_OUT` 或恢复流程写回较早 `START` 投影 | progress persistence、logout/enter-world 边和所有 reset/set-variable 动作 | `c25db02d5`、`Quest14112LogoutPersistenceTest#keepsPoisonousBubblegutKillProgressAcrossLogout` |
| `MULTI_NPC_HANDOFF_REWARD_OWNER` | 中间 NPC 卡住、结束对话 load fail、交付后不能继续、错误 NPC 领奖 | `START + handoff owner + visible action chain -> REWARD`；最终 reward owner 独占 preview/completion；其他 NPC 在对应状态无 start/complete route。1163 的具体链为 `SELECT2 -> SELECT2_1 -> SETPRO1` | 展开后的每个 NPC route、work item give/has/remove、`REWARD` 投影和最终 `npc-complete` owner | `598deb98f`、`Quest1163ClientDialogAlignmentTest#followsTheRetailPotionHandoffAndRewardOwner` |
| `FOLLOW_CURRENT_INTERACTION_OBJECT` | 护送跟错同模板 NPC、对话后又生成一只 NPC | follow target 使用本次权威 interaction object，不按模板 ID 重新查找或生成 | interaction object、after-commit follow action、是否存在重复 spawn | `56009f7f5`、`Quest1149ClientDialogAlignmentTest#startsPoppyFollowingWhenTheEscortStepBegins` |
| `FOLLOW_STARTS_IMMEDIATELY` | 护送开始后 NPC 原地不动，玩家先走远才追 | 进入 FOLLOWING 时立即启动移动，不等待目标先越过距离阈值 | follow 状态入口、`FollowManager.startMoving` 和首次 tick | `de7e6ebe1`、`FollowManagerTest#startsMovingAsSoonAsFollowingBegins` |
| `ESCORT_FOLLOW_DISTANCE` | 护送 NPC 超过很远才追、追到远处就停 | 跟随距离属于实际 AI 状态判定；按普通/实例/残血上下文使用并测试真实 3D 边界 | 生效 AI、HP、实例上下文、开始追击和停止追击阈值 | `138e5c57e`、`FollowManagerTest#followingStopsAtTheCloseFollowDistance` |
| `STATE_GATED_ACTION_ITEM_USE_FLOW` | 未接任务也能点物品、接取后点击无页面、完成 NPC 不响应 | object route 只在权威状态开放，并按客户端动作链分段推进；work item 生命周期与完成 owner 同时验证 | `can-act`、`ACTION_ITEM_USE`、source 状态、变量和 give/has/remove | `c02c8722e`、`EarlyElyosQuestRegressionTest#stolenVillageSealUsesTheItemStackOnlyAfterAcceptance` |
| `OBJECT_ACTION_AND_QUEST_SELECT_ID_SPACE` | 点击任务物品无响应、把对象动作当成 NPC 选择动作 | object action 与 `QUEST_SELECT` 属于独立 ID 空间；XML 必须注册客户端实际发送的事件 | 客户端 object action、AI fallback 和编译后的 event 类型 | `0786d4126`、`EarlyElyosQuestRegressionTest#recoveredVillageSealUsesTheItemStackOnlyAfterAcceptance` |
| `QUEST_ITEM_DIALOG_ACTION_FALLBACK` | action gate 拒绝后直接关闭，但任务实际存在 talk/start route | 任务交互物依次尝试 `USE_OBJECT -> START_DIALOG`，两者都未命中才发通用响应 | NPC action gate、任务生产索引和两个 fallback 事件的命中结果 | `9abdf9433`、`QuestItemNpcAI2Test#triesUseObjectBeforeFallingBackToTheStartDialog` |
| `ARRIVAL_GATED_ESCORT_COMPLETION` | 攻击时过早推进、NPC 到达后任务反而不继续 | 启动周期到达检查并由权威 reach 事件推进；攻击事件不替代位置到达 | 调度生命周期、距离判定、取消条件和 `NpcReachTarget` route | `5c7a2eb68`、`Quest1157EscortRegressionTest#movieStartsOnlyAfterMimitiReachesGaphyrk` |
| `DEPENDENCY_INDEXED_DIALOG_REFRESH` | 对话明显卡顿、一次状态变化重复刷新大量任务 | 状态变化只重评估显式依赖 owner；保留必要 visibility refresh 和页面响应 | 单次交互的依赖遍历数、状态包数、页面包数和索引反向边 | `a5e7fba5a`、`QuestDependencyIndexTest#indexesOnlyLevelUpOwnersByExplicitQuestDependency` |
| `INTERACTION_OBJECT_TASK_SPECIFIC_CONTRACT` | 多个早期交互物任务表现相似但页面、物品或次数各不相同 | 共享 dispatcher/runtime 只修共同协议缺陷；每个任务仍保留独立 action、状态、work item、计数和 owner 合同 | 客户端对象动作、旧 handler、当前 IR 和任务专用断言的差异 | `1f4139b56`、`EarlyElyosQuestRegressionTest#ointmentAcceptanceKeepsTheWorkItemOnBothClientAcceptRoutes` |
| `ORDERED_MULTI_NPC_REPORT_FLOW` | 多 NPC 报告顺序错乱、任意 NPC 都能完成、报告 self-loop 冲突 | 每个报告阶段使用显式 state/var 和唯一 owner；中间 route 与最终 completion 不合并 | 完整 NPC 顺序、每阶段 source/target、终止响应和展开后冲突 | `15a20225c`、`ReportToManyLegacyFlowRegressionTest#legacyHandlersDefineOrderedNpcStepsAndTerminalResponses` |
| `ACCEPTED_ONLY_OBJECT_ROUTE` | 未接任务即可触发交互物副作用、接取后正常 | object route 必须以已接取状态和权威变量/物品为门槛，未接取状态无可执行 route | unaccepted/started 两种 compiled route、client action 和副作用 | `e5a25fd9b`、`EarlyElyosQuestRegressionTest#belbuasWineBarrelUsesTheObjectRouteOnlyAfterAcceptance` |
| `EQUIPPED_START_CONDITION_RUNTIME` | 已装备指定物品仍不能接任务、装备事实未知时误放行 | metadata `equipped` 必须下沉为正式运行时条件；未知事实 fail closed，不用背包事实代替 | metadata 转换、装备事实捕获、正向/反向生产 dispatcher 路径 | `cc7aabea5`、`QuestEquippedStartProductionFlowTest#soloriusQuestsRequireTheClientVisibleEquippedItemBeforeAcceptance` |
| `ASCENSION_SPARSE_ACTION_MAPPING` | 体验未来 load fail、相信后 load fail、职业按钮错位 | 稀疏 `SETPRO`/`SELECT` action、起始职业条件和进阶职业映射逐按钮绑定；action ID 与 page ID 独立 | Aion 5.8 客户端 action/page、职业条件、转职后的传送和 sync 顺序 | `8769210fd`、`Quest2008RetailAlignmentTest#followsClientBeliefAndAdvancedClassActions` |
| `ARCHDAEVA_ATOMIC_COMPLETION_PROMOTION` | 任务完成但未升 66、身份标记与在线等级不一致 | 身份、最低经验和任务完成在同一事务持久化；在线角色更新只在 commit 后执行 | promotion action、JDBC 事务、快照恢复、after-commit 和唯一 complete route | `7cd670ffb`、`QuestArchDaevaPromotionDefinitionTest#asmodianCompletionPromotesArchDaevaAfterTheExpReward` |
| `STATE_SYNC_BEFORE_DEPENDENT_PAGE` | 页面与任务状态不同步、需要重复交互、包顺序反了 | 同一次已提交状态变化后先发送 quest state，再显示依赖新状态的页面 | 完整 after-commit、objectId/questId/page 和真实包顺序 | `6a77337db`、`QuestPacketOrderRegressionTest#protocolLoopSendsCommittedStateBeforeEveryRepairedPage` |
| `CROSS_MAP_ENTER_ZONE_PHASED_FLOW` | 任务物品读条后没有传送、进入目标地图后不推进、中间 NPC 或最终领奖 owner 错位 | 任务对象与 world portal owner 分离；每次权威 `ENTER_ZONE` 和 NPC handoff 都推进独立持久阶段；电影只在最终地图阶段播放；最终 reward owner 独占完成路由 | 交互对象的 portal/quest owner、portal loc/world、区域名、进入前后 status/vars、电影与领奖 owner | `5511223b0`、`Quest26800ClientDialogAlignmentTest#advancesThroughTheTowerAndEnfitentaOnlyAtTheAuthoritativeStages` |
| `COUNTER_SOURCE_PROJECTION_NO_LOCK` | 第一次击杀能推进，后续计数、最后一击或报告突然无响应；审计出现 `NO_MATCH` | `START` 源节点只投影任务 status，不把实时计数字段固定写成零；计数 transition 自己按当前变量条件匹配，最后一击再进入 `REWARD` | source node projection、packed variables、计数条件/priority、不同击杀顺序下的生产 dispatcher 结果 | `4a3be57`、`Quest26802ClientDialogAlignmentTest#finalKillInEitherCounterEntersRewardBeforeReporting` |
| `COLLECT_TURN_IN_DIALOG_CHAIN_MISMATCH` | 交付收集物时 load fail、HtmlPageId 不存在、交付按钮无响应 | `QUEST_SELECT` 显示客户端存在的任务入口页；交付判定挂在客户端实际按钮动作 `CHECK_USER_HAS_QUEST_ITEM(39)` 上，用 priority 0/1 拆分集齐进 `REWARD`+奖励窗口与未集齐回落提示页 | 客户端页面契约中该任务的入口页与交付按钮动作；旧 handler 的 collectItemCheck 分支与页面 | `e6f4f12cf`、`Quest14015ClientDialogAlignmentTest#collectDialogChainUsesOnlyClientOwnedPages` |
| `INTRO_CHAIN_ACCEPT_PROMPT_BRIDGE_MISSING` | 接取介绍最后一步 load fail、无法打开接受窗口、继续听后没有确认 | 多页介绍链不能把每个 action 简单回显成同 ID 页；最后一跳必须按旧脚本桥接到 `SHOW_ASK_QUEST_ACCEPT_WINDOW(4)`，再由 `NPC_START` 的接受/拒绝路由接手 | 旧 handler 的最终介绍 action 响应页；当前 action->page 链和确认页入口 | `3721d0801`、`Quest1311ClientDialogAlignmentTest#acceptDialogChainReachesTheLegacyAcceptWindowAndStartsTheQuest` |
| `DEVICE_TARGET_SPAWN_WINDOW_MISMATCH` | 使用任务机关后任务推进但副作用失败、运行时 WARN `AddNpcAggro failed`、激怒目标不出现 | 机关与副作用目标的 `temporary_spawn` 窗口必须覆盖彼此；knownlist 缺席目标的 best-effort 副作用不按硬失败上报，玩家不可用仍保持硬失败 | 比对任务链所有 spawn 窗口与目标 `respawn_time` 空窗；先修窗口对齐再保留运行时容错 | `2d2d22dfd`、`RetailOpenWorldSpawnDataTest#keepsNymphGownRetailReferenceHeightAndNightWindow` |
| `MULTI_LOCATION_SCOUTING_FINAL_REWARD_TRANSITION` | 侦察完成后没有下一步、调查3个地方后任务空白、动画看完后无法向NPC报告 | 多目标区域/动画侦察通过位掩码记录进度，最后一次侦察直接迁移至 REWARD 并在 after-commit 提交 LEVEL_AND_VISIBILITY_REFRESH；提供 enter-world/talk 平滑恢复，不使用 START 汇总中间节点 | 最终侦察完成 transition 的目标状态与 sync mode；是否误用 START 中间节点导致客户端 step 0/1 与位掩码错位 | `f7ae6a706`、`Quest1336ScoutingForDemokritosRegressionTest#completesTheReportStateForEveryInvestigationOrder` |

指纹表至少维护以上五列。一个代表提交可以支撑多个独立 Pattern；这属于对复合修复的检索拆分，不是新增重复案例。新增代表模式时，案例正文记录完整验收证据，指纹表只提炼可搜索的症状别名和抽象 IR/owner 特征，具体 action/page 仅作为代表实例；后续同型任务不把任务 ID 追加进表中。

`DUPLICATE_QUEST_SIDE_EFFECT_OWNER` 的代表证明是刻意拆开的复合证据：提交 diff 证明旧 Java AI owner 被移除，具体测试方法证明 typed XML owner 接管同一击杀生成合同。只运行该测试不能单独证明没有第二 owner。复用本模式时还必须检查当前 catalog/XML/AI 源 owner，并在实际路径可用时记录同一事件产生的 NPC object ID、生成次数和清理次数；这些 runtime 证据使用验收记录模板留存。

| 提交 | 案例 | 可复用结论 |
|---|---|---|
| `8b058d4b4` | 14047 两段飞行传送、不可达副本恢复、伊卡罗尼斯变身和完整客户端链 | 状态必须先提交并同步再传送；飞行专属地点必须覆盖崩溃、断线和实例重建后的可重入恢复；血量阈值变身必须以幂等死亡路径覆盖直接秒杀；同名 NPC 的 GM 寻找传送只能按任务阶段限域 |
| `4a23cf0a0` | 13830 实时奖励选择后点击领取无响应 | 无目标实时奖励使用 110..124 独立动作空间；任务 XML 必须注册实际可见槽位并保留完整完成合同 |
| `906c08e92` | 24 个奖励选择路由首次点击无响应；37 个任务副作用 AI 被 retail pattern 覆盖 | 用生产目录审计捕获 `SELECT_QUEST_REWARD -> REWARD` 无响应；有生命周期任务副作用的 AI 必须有证据化 fallback 集合 |
| `7a6ad8eca` | 14112 击杀剧毒斯拉希后生成 Kato、重登恢复、首次奖励对话 | 任务 NPC 生成、登录恢复、页面响应和旧 AI 清理必须作为同一任务合同验证 |
| `c25db02d5` | 14112 下线后击杀进度回退 | 持久位域不能被登出/恢复流程写回 START；用专用测试锁定 logout/enter-world |
| `598deb98f` | 1163 对话页面和状态时序对齐 | 页面 ID、动作顺序和状态迁移要结合客户端/旧 handler 验证，不能套通用 page |
| `56009f7f5` | 1149 跟随与玩家对话的 Poppy，而非另一只同模板 NPC | 保留 interaction object 身份，使用 `start-follow-current-target-npc`，不要重复 spawn |
| `de7e6ebe1` | 护送开始后 follower 不立即移动 | 进入 FOLLOWING 状态时立即启动移动，并用 FollowManager 测试证明 |
| `138e5c57e` | 护送 NPC 超过 15 米才追、追到 15 米停 | 跟随容差属于 AI 状态判定；当前普通满血贴身距离为 3 米，必须测试边界而不是只改一个常量 |
| `c02c8722e` | 1156 消失的村落印章 object flow | `USE_OBJECT`、`can-act`、中间变量和完成 NPC 必须按客户端动作链分段验证 |
| `0786d4126` | 1158 村落印章对象交互 | object action 与 `QUEST_SELECT` 是两个 ID 空间，接取前不能开放 object route |
| `9abdf9433` | 交互物 dialog fallback | action gate 失败不代表没有任务 talk/start 路由；按 `USE_OBJECT -> START_DIALOG` 顺序尝试 |
| `5c7a2eb68` | 1157 Mimiti 到达目标后才继续 | 护送/诱导要用周期检查任务和 `NpcReachTarget`，不能在攻击事件里提前播放下一段电影 |
| `a5e7fba5a` | 对话刷新放大导致卡顿 | 用依赖反向索引限制重评估；不要删除必要的状态可见性刷新 |
| `1f4139b56` | 早期 Elyos 多个交互物任务 | 多任务共享协议缺陷应在 runtime/回归测试层修复，并保留各任务的页面/动作差异 |
| `15a20225c` | 多 NPC 顺序报告流 | 先画完整 state/var 时序，再实现显式 route；不要将 report self-loop 和完成路由合并 |
| `e5a25fd9b` | Belbua 酒桶交互 | 从客户端 object action 和接取状态证明 route，避免未接取时误触发任务副作用 |
| `cc7aabea5` | 9550 装备事件物品后仍无法接取任务 | 元数据 `equipped` 起始条件必须下沉为正式 `EquippedItem` 条件并使用已捕获装备事实求值；装备事实未知时必须 fail closed |
| `8769210fd` | 2008 魔族转职任务的客户端动作链整体错位 | 转职页面的稀疏 action ID、起始职业分支和进阶职业映射必须逐项取自客户端证据，不能按页面序号或职业顺序推导 |
| `5511223b0` | 26800 永恒之塔到知识书库的跨地图阶段流 | 先区分任务交互物与实际 portal owner，再用每段 `ENTER_ZONE`、NPC handoff、电影和唯一 reward owner 锁定完整状态链；不能把跨地图阶段压缩为通用接取/领奖模板 |
| `4a3be57` | 26802 Archives mission live counters | `START` 节点不固定投影 `var0/var1/var2=0`；图书管理员与元素首领两组计数可按任意顺序推进，最后一次击杀进入 `REWARD`；代表测试为 `Quest26802ClientDialogAlignmentTest#finalKillInEitherCounterEntersRewardBeforeReporting` |
| `2d2d22dfd` | 1114 机关与激怒目标的夜间出现窗口不一致 | 任务机关 `temporary_spawn` 窗口必须覆盖或等于副作用目标 NPC 的窗口（700008 对齐 203175 的 21:00-04:00）；`addNpcAggro` 对 knownlist 缺席目标按 best-effort 跳过，玩家不可用仍为硬失败；代表测试为 `RetailOpenWorldSpawnDataTest#keepsNymphGownRetailReferenceHeightAndNightWindow` |
| `f7ae6a706` | 1336 埃拉库斯沙漠三地点调查完成转入领奖 | 多目标区域侦察任务必须在最后一段动画/地点完成后直接转入 REWARD 并提交 LEVEL_AND_VISIBILITY_REFRESH，禁止插入 START 汇总中间节点导致位掩码丢失与客户端 step 错位；同时为历史遗留存档提供登录与对话平滑恢复 |
