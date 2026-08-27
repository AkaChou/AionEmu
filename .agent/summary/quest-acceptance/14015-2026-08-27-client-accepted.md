# 任务 14015 客户端验收记录

quest: 14015

user acceptance confirmation: 用户于 2026-08-27 在对话中明确回复"验收确认"，确认范围为完整任务交付流程（收集 10 个图尔辛克拉尔的证物并交付 NPC 203098），未限定单一分支或步骤。

server launch mode: not captured

repository commit: `e6f4f12cf` (`fix(quest): align quest 14015 collect turn-in dialogs`)

working tree: dirty；任务 14015 XML 与专项测试已包含在修复提交中，工作树中其余未提交文件（18501、DialogService、CM_DIALOG_SELECT 等）属于其他进行中工作，不属于本验收记录。

Aion 5.8 client/data provenance: Aion 5.8 客户端；页面/action 证据取自仓库内 `docs/quest/client-dialog-mapping/quest-dialog-pages.csv` 与 `quest-dialog-action-details.csv`（quest_q14015.html，source_sha256 `d05639bc5c549aca2707c6c21fb0cdeefa4704afdf19cd808508d14e02aa9fa9`），本次未重新采集客户端包哈希。

npc template/object: 交付 NPC 203098（斯帕塔洛斯）；运行时 object ID not captured。

map/instance: 铸币会驻地（开放世界），world/instance ID not captured。

steps:

1. 前置：任务 14010 已完成，任务经升级/区域登记进入 `START var0=0`；击杀图尔辛克拉尔系怪物收集 10 个证物（182215316）。
2. 与 NPC 203098 对话，从通用任务选择页选择任务 14015。
3. 依次点击 select1 -> select1_1 -> select1_1_1 的"继续听"按钮，最后点击"拿出图尔辛克拉尔的证物"完成交付并领取奖励。

source state/status/vars: `START var0=0`；交付后 `REWARD var0=1`，完成 `COMPLETE var0=0`。

action/page/button: `QUEST_SELECT(31) -> SELECT1(1011)`；动作 `SELECT1_1(1012) -> 页 1012`；动作 `SELECT1_1_1(1013) -> 页 1013`；`CHECK_USER_HAS_QUEST_ITEM(39)` 集齐 `priority=0` 进 REWARD 并打开 `SHOW_SELECT_QUEST_REWARD_WINDOW1(5)`，未集齐 `priority=1` 回落 `SELECT1_2(1097)`。

expected response: 选择任务后显示客户端实际存在的入口页 1011（修复前发送不存在的 1352 导致 load fail）；交付动作按客户端按钮动作 39 判定；集齐时同一次交互移除物品、进入 REWARD 并弹出奖励窗口。

actual response: 用户完成修复后客户端全流程，交付时不再弹出 load fail，任务可正常完成验收。

startup health: 修复 XML 通过 `xmllint --noout --schema quest_definition.xsd`；任务专用测试与生产 catalog/白名单门禁未运行（本会话未获构建授权，命令已列于交付说明）；用户能在真实客户端推进至对话与交付流程，未报告任务引擎初始化或编译错误。

runtime logs: not captured

protocol trace: not captured

screenshots/recordings and SHA-256: not captured；用户在对话中提供了修复前的 load fail 截图（临时缓存路径，未保留为稳定附件）。

acceptance status: ACCEPTED_NEW_PATTERN

matched Pattern: `COLLECT_TURN_IN_DIALOG_CHAIN_MISMATCH`；匹配字段为 `QUEST_SELECT` 发送客户端不存在页、交付动作未对齐客户端按钮动作、select 页链与未集齐回落缺失。与 `LEVEL_UP_AUTO_START_NO_DIALOG` 的差异：该模式核心是升级入口发页且 NPC 页合同为 `DEFAULT_SUCCESS(10002)`，本任务升级入口本已正确，NPC 页为专用收集链入口页 1011，并新增交付动作空间错配（1009 -> 39）与 priority 0/1 双分支合同。代表提交 `e6f4f12cf`；代表测试 `Quest14015ClientDialogAlignmentTest#collectDialogChainUsesOnlyClientOwnedPages`。

remaining risks: 任务专用测试与生产 catalog/白名单门禁未在本会话运行；packet 顺序、运行时 object ID、启动日志未捕获；未集齐分支（动作 39 未满 10 个）未单独实测，由 IR 合同与客户端页面证据覆盖。
