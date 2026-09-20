# 11323 奖励窗口 “load fail” 诊断（2026-09-20）

## 现象

1. 客户端收到 `SM_DIALOG_WINDOW page=5`（奖励窗口），玩家点击窗口里的“放弃奖励/关闭”，
   客户端上送 `CM_DIALOG_SELECT action=23 (SELECTED_QUEST_NOREWARD) questId=11323`。
2. 服务端在同一动作上回发 `SM_DIALOG_WINDOW page=23`，客户端 “load fail”；
   任务停在 `Status: REWARD`、`Vars: 4 0 0 0 0`、`Complete count: 0`。
3. `log.quest_trace.dialog_window` 直接打印 `SM_DIALOG_WINDOW` 构造参数，所以日志里的 23
   是服务端真实下发的 page id，不是动作 ID 的显示误差。

## 证据链

- 编译产物探针（`QuestDefinitionXmlCompiler.compile(11323.xml)`，用仓库 jar 内类 + 源码资源运行）：
  - `npc=798928 dialog=-1  -> ShowQuestDialog[5]`（奖励窗口预览）
  - `npc=798928 dialog=1009-> ShowQuestDialog[5]`
  - `npc=798928 dialog=8..23 -> reward => complete, after=[..., ShowQuestSelectionDialog[10]]`
  - 702725/702727/702743/702744/702745 **只有** `dialog=31 / 10255` 等推进页，没有任何 8..23 交付路由。
- `QuestEvent.matches` 对 `TalkToNpc` 要求 `expected.npcId() == observed.npcId()`（严格绑定）。
- 因此客户端带着 `npcId=702745` 上送 23 时，typed 引擎找不到路由 → `handled=false`；
  `PortalDialogAI2.onDialogSelect` 的兜底分支再执行
  `sendPacket(new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId))`，把**动作 ID 当页 ID 回显**，
  客户端找不到 page 23（`HtmlPages 23 = HTML_PAGE_SHOW_GATHER_SKILL_LEVELUP_WINDOW`，不是任务页）→ load fail。
- 受影响的“先看窗口、后按放弃”路径同样会命中 8..22（奖励按钮），不只是 23。

## 根因

奖励窗口的确认动作（`SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD`，8..23）由全局客户端窗口发出，
却按“完成 NPC”做严格绑定匹配；当客户端上送的交互对象不是该任务登记的报告 NPC 时，
路由无法匹配，兜底逻辑又把动作 ID 当页 ID 下发。

## 修复方向（需构建验证）

1. 引擎侧恢复路径：`questId + action ∈ [8,23]` 且严格绑定未命中时，在 owner 的定义内做“非绑定”候选查找，
   只接受**唯一的** `source=REWARD → target=COMPLETE` 且动作 ID 相同的转换，再由
   `QuestMutationPlanner` 按玩家真实快照校验后执行（状态不符自然不执行）。
2. 需要把 `QuestProductionDispatcher` / `QuestExecutionCoordinator` 的入口加上“已按上述规则校验”的
   内部标记（默认行为不变），供恢复路径复用同一事务/after-commit 管线。
3. 不在 `PortalDialogAI2` 加“吞掉”补丁：那只能消掉 red error，任务仍停在 REWARD。

## 待执行验证

```bash
mvn -q -Dtest=ReportToManyLegacyFlowRegressionTest,QuestXmlDomainBlocksTest test
mvn -q -Dtest=Quest11323RewardWindowRecoveryTest test   # 新增聚焦用例
mvn -q -Dtest='Quest*ProductionFlowTest' test
```

以上命令尚未执行（仓库规则要求构建前取得授权）。
