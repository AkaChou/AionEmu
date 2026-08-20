# 任务 26801 客户端验收记录

quest: 26801

user acceptance confirmation: 用户于 2026-08-20 明确回复“26801，26802 均客户端验收通过”，确认 26801 为完整任务范围，不限于单一步骤。

server launch mode: not captured

repository commit: `96eeeb4bc3b3bca293485a66b451eae0aae15d96` (`fix(quest): restore Archives mission counters`)

working tree: dirty；26801 相关修复提交已在当前分支历史中，其他未提交文件不属于本验收记录。

Aion 5.8 client/data provenance: Aion 5.8 客户端；页面/action 来源见 `docs/quest/client-dialog-mapping/README.zh-CN.md`，本次未重新采集客户端包哈希。

npc template/object: 接取、报告和奖励 owner `806149`；运行时 object ID not captured。击杀目标为档案馆图书管理员 `220305、220308、220311、220314、220317、220323、220326、220329`。

map/instance: 知识书库 `301540000`；instance ID not captured。任务可从 26800 完成后的同一副本流程进入。

steps:

1. 以 26800 完成为前置，沿知识书库任务链进入/接取 26801 并得到 `START`；本次未单独捕获是 `enter-world` 自动登记还是 `806149` 页面接受。
2. 在 `301540000` 击杀 30 个任务指定图书管理员。
3. 第 30 个击杀后进入 `REWARD`，返回 `806149`。
4. 打开奖励窗口，选择职业奖励并完成任务。

source state/status/vars: `NONE -> START`；`var1` 从 0 累计到 30，完成后进入 `REWARD`，`var0=1`。

action/page/button: 客户端接取或自动登记细节未单独捕获；报告页 `DEFAULT_SUCCESS(10002)`；奖励窗口 `SHOW_SELECT_QUEST_REWARD_WINDOW1(5)`；最终由 `806149` 完成。

expected response: 每次击杀同步 `PACKET_ONLY`；第 30 次击杀进入 `REWARD` 并刷新可见性；奖励事务按职业发放 EXP、金币和职业物品，再发送完成同步和任务选择页。

actual response: 用户确认 26801 客户端完整流程通过。运行时 object ID、packet trace、日志和截图未捕获。

startup health: not captured；用户未报告 typed quest engine 初始化、catalog 编译或运行时错误。

runtime logs: not captured

protocol trace: not captured

screenshots/recordings and SHA-256: not captured

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `COUNTER_SOURCE_PROJECTION_NO_LOCK`；匹配字段为实时计数、共享 `START` 源节点和最后一次击杀进入奖励的合同。代表提交 `4a3be57`；代表测试 `Quest26802ClientDialogAlignmentTest#finalKillInEitherCounterEntersRewardBeforeReporting`。26801 的具体计数阈值修复来自 `96eeeb4bc`。

remaining risks: 未单独捕获 30 次击杀的 packet 顺序、运行时 object ID、instance ID、服务器启动方式及断线/重登分支；用户的完整任务完成确认构成本任务客户端验收。
