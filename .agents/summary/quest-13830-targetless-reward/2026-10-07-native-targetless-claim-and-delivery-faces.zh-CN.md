# 13830 native 无目标领奖面与全行交付面重建 + 领奖动作结算语义归一化（2026-10-07）

修复提交：`526afff8b`（Playbook 案例 8.56；记忆库 QE-159/QE-160）。
验收记录：`.agents/summary/quest-acceptance/13830-2026-10-07-client-accepted.md`。

## 1. 现象与时间线

| 时刻 | 现象 | 结论 |
|---|---|---|
| 2026-10-07 20:56 | 玩家 Kk 任务窗点「实时奖励」→ 确认，任务无法完成；日志 3 条 `[QUEST-TRACE][C->S] CM_DIALOG_SELECT 玩家=Kk npcId=0 targetObj=151512 questId=13830 上一页=5 动作=110` | ① 无目标领奖面无族认领（包被静默丢弃） |
| 入口修复后 | 「任务可以完成了，但是任务奖励物品没有入包」 | ② 110 原样进结算体 ⇒ `selRewIndex = 110-8 = 102` 越界 ⇒ 职业奖励静默未发（状态照写 COMPLETE） |
| 一并排查 | 客户端任务书第二条路径「和奥尔佩（203711）对话」全无响应 | ③ 非 Talk 行（LevelUpLogIn 接取）交付面未注册 |

用户确认操作界面 = 任务窗（J）；修复范围 = 两条领奖路径一并修。

## 2. 三源证据

1. **客户端权威（Aion 5.8 客户端）**：`data_unpacked/Dialogs/10000_19999/quest_q13830.html` 任务书第 2 步原文「在任务窗点击[领取奖励]% 或 和[奥尔佩]%对话」；动作 110 = `SELECTED_QUEST_AUTO_REWARD1`（实时奖励槽 1）；页 10002 `select_success`（台词页 + `HACTION_SELECT_QUEST_REWARD`=1009）由 `client_dialog_contract.tsv` 对 13830 声明（4762/10002 两页）。
2. **真端表**：`quest.xml` 13830 行 = `use_class_reward=1`、`max_repeat_count=1`、`reward_exp1=46544`、11 个职业各 1 项（GLADIATOR=`STIGMA_N_FI_cripplingcut_g1` → 物品 140001109）；`data_driven_quest.xml:4669` = `category_acquire_=LevelUpLogIn(30)` + 发 `Doc_quest_13830a` + `category_progress_=ItemPlay` + `reward_npc_name=Orphe`。
3. **真端原码证据（P7 系列）**：`P7-STEP2E1-REPORT.zh-CN.md` + `P7-STEP2E-PREREQ-ACQUIRE-ACTIONS.zh-CN.md` §3/§7——「**所有行**恒建对象 #2（`reward_npc_name`，槽 `+0x238` = `FUN_180c473e0`）」；该面语义：打开（状态 0/10）→ 页 10002（`0x2712`）；1008 → 关窗；1009 → 报告通道。
4. **退役前合同**：Playbook 案例 8.3 `TARGETLESS_REALTIME_REWARD_ACTION_SPACE`（commit `4a23cf0a`，退役前 XML 已实机验收）：无目标 8/110 → 发职业物品+经验、回收工作物品、完成 + **关窗**；XML 退役删除后该面未在 native 重建。

## 3. 根因（三层）

### ① 无目标领奖面缺失（任务窗/实时奖励槽）
- 确认包不带 NPC 上下文（`CM_DIALOG_SELECT` 的 `targetObjectId == 0` 或对象不可解析，实测 `targetObj=151512` 不可解析），引擎以 `npcId=0` 进入；
- native 车道只按 `npcId` 路由：`QuestEngine.java` DD 分发带 `npcId != 0` 守卫、各族 handler 领奖段前置 NPC 门 ⇒ 无族认领，包被静默丢弃。

### ② 领奖动作未翻译成结算体奖励窗语义（物品静默丢失）
- `QuestService.getRewardItems`（:192-330）：`dialogId != 23 && != 0` 时按 `selRewIndex = dialogId - 8` 取职业/可选奖励；
- 110 原样透传 ⇒ `selRewIndex = 102` 越界 ⇒ `getQuestItemsbyClass`（:449-454 越界返回 null）/可选奖励边界判定均静默跳过 ⇒ **任务状态照写 COMPLETE、奖励物品不发、零报错**；
- 23 通道（:260-）按 `env.getExtendedRewardIndex() - 8` 取选项——native 侧从未设置该字段 ⇒ 恒 -8 越界，同类丢物品。

### ③ 非 Talk 行交付面缺失
- 真端「所有行恒建对象 #2」，native 只在 `acquire.kind() == 4`（Talk 接取）时注册 `reportTalks` ⇒ LevelUpLogIn/ItemPlay 行（13830 为例）在交付 NPC 处无任何面。

## 4. 修复

| 面 | 变更 |
|---|---|
| 共享结算段 | 新增 `NativeTargetlessReward`：状态 `REWARD` + `QuestDialogAction.isRewardWindowAction` 动作段（8..23 ∪ 108 ∪ 110..124）→ `rewardFlow.claim` + `DialogService.closeDialog(player, 0)`；允许族级后置（CombineTask 的扣产物 + 忘配方）；任一門不满足零副作用返回 false |
| 三入口接入 | 七族 handler（`routes(questId)` 守卫后、NPC 门之前）；DD 运行时 `onDialog` 顶部（`npcId == 0 && requestedOwner ∈ routedQuestIds`）；`CM_DIALOG_SELECT`（`obj` 不可解析 + 动作 ∈ 奖励窗段 + `questId > 0` 时按 questId 进引擎；引擎内保持全部门） |
| 领奖口归一化 | `NativeReportRewardFlow.claimEnv`：`108/110..124 → 8 + rewardIndex`；`23 → 保留 id + extendedRewardIndex = 8 + rewardIndex`；`8..22` 原样透传（不做全局 remap；客户端面动作/页链不变） |
| 交付面放至所有行 | `reportTalks` 注册去掉 Talk 门；REWARD 面按接取类别分形：Talk 行维持现状（`31/1009/-1 → 页 5`）；非 Talk 行 = `打开(-1/31/26) → 页 10002`（`QuestDialogContract.hasButtonPage` 客户端声明校验，未声明 `continue`）、`1009 → 页 5`、领奖 → 结算 + 页 10；START 态 `!zeroStep || !talkAcquired` 不认领（防跳步） |
| 引擎放宽 | `QuestEngine` DD 分发去掉 `npcId != 0` 守卫（`npc != null ? npc.getObjectId() : 0`；门全在 DD 内部） |
| 附带修正 | `SimpleCombineTaskHandler`：选项段改 8..22 + 显式 23 分支（旧 `last = NOREWARD(23)` 把 23 映射成下标 15 ⇒ 按钮面 fail-closed；同型先例 1107 领奖循环 `99684c70d` 覆盖两族，本族为漏网族）；`SimpleSerialHuntHandler` 无目标分支前移到硬 NPC 门之前 |

## 5. 测试（IDEA MCP，10 类全绿）

- `NativeQuestRewardClaimGateTest`：16 用例，含新增 `claimActionsNormalizeIntoTheSettlementRewardWindowVocabulary`（110→8 / 8 透传 / 23→extendedRewardIndex=8）与 `targetlessClaimsSettleByQuestIdOnEveryLane`（Talk 1207 / Hunt 1102 / SerialHunt 16991 / CollectItem 1137 四族 + START/非确认段两门）。
- `DataDrivenNativeRuntimeGateTest`：新增 `targetlessRewardClaimSettlesByQuestIdAndClosesTheWindow`（13830 + GLADIATOR）、`nonTalkRowsServeTheDeliveryNpcWithTheRetailObjectTwoShape`（奥尔佩 203711：打开→10002、1009→页 5、领奖→结算+页 10、START 不认领）。
- `SimpleItemPlay/SimpleUseItem/SimpleCombineTaskNativeFamilyGateTest`：各补 targetless 用例；CombineTask 含完成流条件回收 + 23 归 0 两用例。
- `Quest23830To23834TargetlessRewardTest`：改锚为「非 Talk 行注册交付面」（原断言与真端 P7 证据冲突）。
- 全部 exit 0（2026-10-07）。

## 6. 实机验收

用户冷重启（IDEA 常驻服务端）后回复「实机验证成功，提交」——整任务验收、未限定分支或步骤；记录见 `.agents/summary/quest-acceptance/13830-2026-10-07-client-accepted.md`。

## 7. 残留风险

- 13831..13834 / 23830..23834 同族行仅结构同型核对（注册面/分形按同一条代码路径），未逐行实机。
- 多槽奖励（111..124 与奖励索引的逐一映射）与多 reward 组行未实机；`rewardTier` 对多槽行保持 fail-closed。
- 非 Talk 行交付面依赖客户端任务 HTML 声明页 10002：其余 482 个非 Talk 行中无声明者维持无面（fail-closed，不劣化）——全量声明面未逐一统计。
- 验收时刻的服务端日志/协议抓包未采集（`not captured`）；报障窗口的 `[QUEST-TRACE]` 三条见 DIAGNOSIS 引用（本目录 §1）。
