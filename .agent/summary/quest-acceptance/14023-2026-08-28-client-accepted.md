# 任务 14023 客户端验收记录

quest: 14023「Playing Around at the Temple / 地下神殿之谜」

user acceptance confirmation: 用户于 2026-08-28 回复“验收提交，然后排查类似问题”，结合当前任务上下文，确认任务 14023 的客户端验收完成，未限定单一分支或步骤。

server launch mode: not captured（服务端由用户管理）

repository commit: `bd782024d`（`fix(quest): consume 14023 temple fragments on turn in and align castor dialogs`）

working tree: dirty；本次修复提交仅包含 `src/main/resources/aion/data/static_data/quest_definition/quests/14023.xml` 和 `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest14023ClientDialogAlignmentTest.java`；其他已修改和未跟踪文件属于工作区其他任务，均保留未提交。

Aion 5.8 client/data provenance: Aion 5.8 客户端；仓库证据为 `docs/quest/client-dialog-mapping/quest-dialog-pages.csv`、`quest-dialog-action-details.csv` 和 `quest-sequence-audit.csv`，对应 `quest_q14023.html`，source SHA-256 `083e5222441019c450aa1f96f6577251ea3e7513bdbfb6070a812578d139e8fa`；本次未重新采集客户端包哈希。

npc template/object: 接取及最终领奖 NPC template 203965（泰勒马科斯）；中间调查官 NPC template 203967（卡斯托尔）；运行时 object ID not captured；交互对象来源为任务 XML 与 Aion 5.8 客户端页面/action 合同。

map/instance: 埃尔特内要塞 / 埃拉库斯神殿；当前 world/instance ID 与入口/重入上下文 not captured。

steps:

1. 前置：任务 14020 已完成，满足 ELYOS、等级 29 及任务 1043 的 `unfinished`/`noacquired` 条件；升级或区域使命结束自动接取进入 `START var0=0`。
2. 与 NPC 203965 对话，客户端 `QUEST_SELECT(31)` 显示 `SELECT1(1011)`；点击“继续听”发送 `SELECT1_1(1012)` 并显示 `SELECT1_1`；点击“结束对话”发送 `SETPRO1(10000)` 进入 `s1 var0=1`。
3. 前往埃拉库斯神殿与 NPC 203967 对话，`QUEST_SELECT(31)` 显示 `SELECT2(1352)`；点击“询问具体的任务”发送 `SELECT2_1(1353)` 并显示 `SELECT2_1`；点击“继续听”发送 `SELECT2_1_1(1354)` 并显示 `SELECT2_1_1`；点击“结束对话”发送 `SETPRO2(10001)` 进入 `s2 var0=2`。
4. 击杀神殿怪物收集 4 块石板碎片（182215318, 182215319, 182215320, 182215321）；返回 NPC 203967 处对话，`QUEST_SELECT(31)` 显示 `SELECT3(1693)`；点击“拿出石壁的碎片”发送 `CHECK_USER_HAS_QUEST_ITEM(39)`，成功时同次事务扣除 4 个石板碎片道具，进入 `reward var0=3` 并显示 `CHECK_USER_ITEM_OK(10000)`；点击“结束对话”发送 `FINISH_DIALOG(1008)` 退出。
5. 返回要塞与 NPC 203965 交互，`USE_OBJECT` 显示 `SELECT4(2034)`，点击“报告结果”打开奖励窗口，选择可选武器/防具奖励完成任务。
6. 登出/登录、重连、重启、死亡、重复领取：not captured。

source state/status/vars: 接取前 `unaccepted / NONE / var0=0`；接取后 `started / START / var0=0`；泰勒马科斯对话后 `s1 / START / var0=1`；卡斯托尔对话后 `s2 / START / var0=2`；交齐碎片后 `reward / REWARD / var0=3`；最终领奖后 `complete / COMPLETE / var0=3` 的节点投影。

action/page/button: `QUEST_SELECT(31) -> SELECT1(1011)`；`SELECT1_1(1012) -> SELECT1_1(1012)`；`SETPRO1(10000) -> s1`；`QUEST_SELECT(31) -> SELECT2(1352)`；`SELECT2_1(1353) -> SELECT2_1(1353)`；`SELECT2_1_1(1354) -> SELECT2_1_1(1354)`；`SETPRO2(10001) -> s2`；`QUEST_SELECT(31) -> SELECT3(1693)`；`CHECK_USER_HAS_QUEST_ITEM(39) -> reward + remove 4 items + CHECK_USER_ITEM_OK(10000)`；`FINISH_DIALOG(1008) -> SELECT_QUEST(10)`；`USE_OBJECT(-1) -> SELECT4(2034)`；`SELECT_QUEST_REWARD(1009) -> SELECT_QUEST_REWARD_WINDOW(5)`。

expected response: 收集 4 个石板碎片交付成功时，同次事务提交 `var0=3` 并扣除 4 个道具，随后按 `LEVEL_AND_VISIBILITY_REFRESH sync -> CHECK_USER_ITEM_OK` 执行；`FINISH_DIALOG` 正常退出；最终由要塞司令官 NPC 203965 展开奖励选择并完成任务，背包中不再残留碎片道具。

actual response: 用户明确确认“验收提交”；按该确认记录为完整客户端流程已通过。未捕获独立 packet、运行日志或稳定截图附件。

startup health: 未启动或重启服务；隔离验证中专项测试 1/1 通过，生产 catalog `6200` 条编译成功、失败 `0`，白名单违规 `0`。

runtime logs: not captured；无稳定时间窗口、角色、NPC object、任务、世界或实例日志附件。

protocol trace: not captured；无稳定的 objectId、questId、page/action 包序列附件。

screenshots/recordings and SHA-256: not captured；初始问题未提供附件截图。

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `MULTI_NPC_HANDOFF_REWARD_OWNER`；匹配字段为多 NPC 显式状态链、客户端可见 action/page 链、中间 NPC 收集物扣除与状态交接、最终奖励 owner 独占；差异字段为 14023 使用 4 个石板碎片检查扣除与 `SELECT2_1 -> SELECT2_1_1 -> SETPRO2` 链。代表提交 `598deb98f`；代表测试 `Quest1163ClientDialogAlignmentTest#followsTheRetailPotionHandoffAndRewardOwner`。

remaining risks: 未捕获运行日志、协议 trace、稳定截图、运行时 object ID、重连/重登/死亡/重复领取路径；本次不新增 Playbook/Pattern 条目。
