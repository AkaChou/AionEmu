# 任务 16802 客户端验收记录

quest: 16802
user acceptance confirmation: 用户于 2026-09-22 明确回复“验证成功，提交”，确认 16802 实机击杀与实时奖励流程通过，为完整任务范围。
server launch mode: not captured
repository commit: `b077f1a80` (`fix(quest): 修复档案馆 16802/16803/16804 实时奖励不可用与 16803 击杀变体缺失`)
working tree: dirty；本次验收对应的 16802/16803/16804 XML 与测试已在 `b077f1a80` 提交入库，工作区内并发/未暂存的其他任务文件不属于本记录。
Aion 5.8 client/data provenance: Aion 5.8 客户端；任务定义与页面/按钮 action 来源于客户端 `quest.xml`、`quest_monster.csv` 及 `docs/quest/client-dialog-mapping/`，本次未重新采集客户端包哈希。
npc template/object: 接取/报告 NPC `806148`；击杀目标第 2 书库守护兵（`220306, 220309, 220312, 220315, 220318, 220324, 220327, 220330`，需求 30）；记录守护者/元素首领（`857450, 857452, 857454, 857456, 857458, 857459`，需求 2）。注意副本怪点机制：第 2 书库房间中央 1 只真正的记录守护者（`857452` 等），另一只位于第 1 或第 3 书库中央（四周装置激活出的麦格里昂召唤体 `857453/857457` 不计入）；领奖为原地「实时奖励」（`STR_QUEST_DIALOG__QUEST_GET_REWARD(912776)` / `STR_QUEST_PREPARE_REWARD(901163)`），报告 NPC `806148` 亦可保底。
map/instance: 知识书库 / 永恒档案馆 `301540000`；instance ID not captured。16801 完成为 16802 前置。

steps:
1. 完成前置使命 16801 后接取 16802，进入 `START` 状态（`var0=0, var1=0, var2=0`，满足客户端 `SECTION_0==0` 门控）。
2. 在知识书库中击杀 30 名第 2 书库守护兵（var1 累计到 30）和 2 名记录守护者（var2 累计到 2），两组击杀顺序任意。
3. 最后一击完成时（无论先满守护兵还是先满记录守护者），任务状态直接迁移至 `REWARD`，`var0=1`，触发 `LEVEL_AND_VISIBILITY_REFRESH`。
4. 客户端界面立即点亮任务名前的「[实时奖励]」按钮，点击实时奖励弹出奖励窗口，成功领取职业奖励并结束任务。

source state/status/vars: `START (var0=0, var1=0, var2=0)` -> 守护兵 `var1=30`、记录守护者 `var2=2` -> 最后一击触发 `REWARD (var0=1, var1=30, var2=2)`。
action/page/button: 计数阶段 `NPC_KILL` 触发 `CHECK_HUNT_COUNTER`；最后一击 `set var0=1` 迁移至 `REWARD`；客户端点击「[实时奖励]」（对应 `HACTION_SELECT_QUEST_REWARD(1009)`）直接弹出 `SHOW_SELECT_QUEST_REWARD_WINDOW1(5)` 并完成任务；保底支持与 NPC `806148` 对话 `QUEST_SELECT(31) -> DEFAULT_SUCCESS(10002) -> SHOW_SELECT_QUEST_REWARD_WINDOW1(5)`。
expected response: 双计数期 `var0` 保持 0（不投影实时字段，满足客户端 `SECTION_0==0` 门控）；两组计数可任意顺序推进并在饱和时各自封顶；最后一击满足全部条件时（priority 0）直接进入 `REWARD` 并将 `var0` 设为 1；任务窗点亮实时奖励按钮；点击实时奖励成功弹出奖励窗口并结算。
actual response: 用户实机验证通过：击杀 30 名守护兵与 2 名记录守护者后，实时奖励按钮正常点亮，成功点击实时奖励完成任务。测试期间曾排查排除：① `//quest set 16802 START 1` 会破坏 `SECTION_0==0` 导致怪头顶标记消失且客户端计数不匹配；② 第 2 书库中央只有 1 只真正的记录守护者，周围为召唤体，第 2 只需要前往第 1 或第 3 书库击杀。确认机制后实机游玩完整通过。

startup health: 提交 `b077f1a80` 包含专项测试 `Quest16802ClientDialogAlignmentTest`（280 行）及 production flow 回归测试（Tests run: 62, Failures: 0, Errors: 0, BUILD SUCCESS），未出现 `QuestCompilationException` 或 `AMBIGUOUS_TRANSITION`；用户实机测试无崩溃或任务引擎报错。
runtime logs: not captured
protocol trace: not captured
screenshots/recordings and SHA-256: not captured

acceptance status: ACCEPTED_EXISTING_PATTERN
matched Pattern: `QE-018`（`MULTI_COUNTER_FINAL_EVENT_ENTERS_REWARD`）与 `COUNTER_SOURCE_PROJECTION_NO_LOCK`；匹配字段：`START` 节点不投影实时计数字段、两组计数任意顺序推进、最后一击 priority 0 直接进入 REWARD/var0=1 点亮实时奖励、旧满计数存档恢复路线；代表提交：`b077f1a80`（16802/16803/16804）及 `4a3be57`（26802）；代表测试：`Quest16802ClientDialogAlignmentTest#finalKillInEitherCounterEntersRewardBeforeReporting`。
remaining risks: 未单独捕获协议包抓包文件与截图；同族 16803/16804 修复已包含在代码中，其实机游玩验证待用户后续按需测试。
