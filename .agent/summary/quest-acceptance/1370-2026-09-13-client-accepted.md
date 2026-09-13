# 任务 1370 与无任务上下文 NPC 对话客户端验收记录

quest: 1370「Betrayal Of Isson」为代表任务；本轮修复覆盖共享 NPC 对话入口的无任务上下文行为，同型 NPC 包括 730019（1320/1321/1322/1478）、203965/203966（1347 报告链）等。

user acceptance confirmation: 用户原话“验证通过，请详细记录”；2026-09-13；结合上一轮要求“未开启‘未满65级普通任务标记’时应与做完该 NPC 任务后的对话一致”，视为本轮无任务上下文 NPC 对话修复的客户端验收完成，未限定单一分支或步骤。

server launch mode: IDEA（服务端由用户管理；本会话未启动、停止或重启）

repository commit: `e518518ce`（修复提交：treat contextless NPC dialogs as plain dialogs）；本验收记录及 Playbook 更新在其后的文档提交中。

working tree: dirty；本次修复路径为 `CM_DIALOG_SELECT.java`、`QuestEngine.java`、`CMDialogSelectContextTest.java`；`QuestProductionDispatcher.java`、`.agent/summary/quest-load-fail/*` 等既有改动保留未提交，与本验收无关。

Aion 5.8 client/data provenance: Aion 5.8 客户端；仓库内客户端证据来源为 `docs/quest/client-dialog-mapping/quest-dialog-pages.csv`、`quest-dialog-action-details.csv`、`client-html-pages.csv`、`client-hyperlinks.csv`；本次新采集客户端资源 SHA-256: not captured。

npc template/object: 代表 NPC template 203949，用户日志中的 runtime object ID 为 36770；同型观察对象包括 template 730019 / object ID 45485、template 203965 / object ID 41644、template 203966 / object ID 49550；稳定附件与对象生成来源 not captured。

map/instance: 用户日志未记录 world ID / instance ID；not captured。

steps:

以下为本轮修复覆盖的验收范围；用户确认整体通过，未逐项提供独立日志、截图或包序列。

1. 使用 ELYOS 测试角色关闭“未满65级普通任务标记”，与 NPC 203949 对话；客户端下发 `questId=0`、`lastPage=1011`、动作 `1012`。
2. 服务端不得把该动作绑定到未接取的普通任务 1370/1371；应直接按普通对话处理，返回 `targetObjectId=36770`、`page=1012`、`questId=0` 的页面，与任务全部做完后的行为一致。
3. 开启“未满65级普通任务标记”后重复同一 NPC 对话，确认 q1370/1371 仍能按 `questId>0` 正常进入任务路由和接取链。
4. 对 NPC 730019、203965/203966 等同型无上下文对话重复验证，确认不再 load fail 或卡在任务按钮页。
5. 回归任务交互物 `quest_use_item` / `quest_start_use_item` 的 `USE_OBJECT(-1)` / `START_DIALOG(31)` 路径，确认仍由 `AI2Actions.selectDialog -> QuestEngine` 处理；该路径不在本次“普通对话”分流内。

source state/status/vars: 代表场景为客户端 `questId=0` 且无同 NPC remembered quest-row selection；q1370 处于 `unaccepted`。错误路径会绑定 q1370 owner 并下发带 `questId=1370` 的任务页；正确路径没有任务 owner 接管，返回 `questId=0` 的普通页面。

action/page/button: `CM_DIALOG_SELECT(targetObjectId=36770, dialogId=1012, lastPage=1011, questId=0)`；第 10 页非 31 动作也必须强制无任务上下文。期望响应 `SM_DIALOG_WINDOW(targetObjectId=36770, page=1012, questId=0)`。

expected response: 无任务上下文时不进入 QuestEngine owner 派发；不再向客户端下发带 `questId` 的任务页；页面、关闭和普通对话行为与“该 NPC 任务全部完成”一致。客户端携带 `questId>0` 或同 NPC remembered selection 时继续保持原任务路由；交互物 `USE_OBJECT(-1)` / `START_DIALOG(31)` 路径不受影响。

actual response: 用户确认“验证通过”；未提供逐项截图、稳定协议包序列或独立运行日志附件。结合上一轮用户提供的 NPC 203949 预修复日志（`questId=0`、动作 `1012` 被错误绑定并触发 load fail）和 NPC 203965 任务完成后的正常 `page=1012 / questId=0` 对照，视为同型客户端行为验收完成。

startup health: 服务端由用户使用 IDEA 管理；本会话未启动、停止或重启，未捕获启动日志。会话中未观察到 `Can't initialize typed quest engine`、`QuestCompilationException`、`AMBIGUOUS_TRANSITION` 或 production catalog compile failure；稳定附件 not captured。

runtime logs: 用户提供的预修复日志覆盖 2026-09-13 17:58（NPC 203949）及同日的 730019、203965、203966 场景；修复后的稳定运行日志附件 not captured。日志可复现字段为 NPC template/object、questId、lastPage、action 和 SM_DIALOG_WINDOW 的 targetObjectId/page/questId。

protocol trace: 预修复协议序列来自会话日志；修复后稳定 packet trace 附件 not captured。当前可见的关键字段为 `questId=0` 的 CM_DIALOG_SELECT 不得生成带 `questId` 的任务页。

screenshots/recordings and SHA-256: not captured。

acceptance status: ACCEPTED_NEW_PATTERN

matched Pattern: `CONTEXTLESS_NPC_DIALOG_STAYS_PLAIN`；匹配字段为 `CM_DIALOG_SELECT` 的 `questId==0`、无同 NPC remembered selection、NPC 对话按钮被错误绑定到未接取普通任务 owner，以及任务全做完后同类页面的正常对照；不同字段为交互物 `USE_OBJECT(-1)/START_DIALOG(31)` 与 `questId>0` 正常任务路由，它们必须继续走 owner 派发。代表提交 `e518518ce`；代表测试 `CMDialogSelectContextTest#treatsNpcSelectionsWithoutQuestContextAsPlainDialogs`、`CMDialogSelectContextTest#genericPageNonQuestActionsCannotBorrowQuestContext`、`DialogServiceQuestDialogTest#simpleNpcDialogUsesGenericPageWithoutQuestOwnerOrQuestDispatch`。

remaining risks: 修复后稳定 packet trace、启动日志、截图和 runtime object/world/instance 附件为 not captured；未独立覆盖每个职业、种族、重连、重复对话和所有 NPC 分支；交互物 `USE_OBJECT/START_DIALOG` 仅有聚焦回归测试，未有本轮独立客户端证据；`QuestClientContractGateTest` 仍有既有 23 条指纹差异，未因本次修复扩大；本会话未执行 push，`quest` 分支的提交由用户推送。
