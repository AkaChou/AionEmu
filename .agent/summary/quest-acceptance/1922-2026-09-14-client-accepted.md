# 任务 1922 客户端验收记录

quest: 1922「Deliver On Your Promises / 履行承诺」；ELYOS 45+ 使命；验收范围覆盖进入圣殿地下竞技场、完成击杀计数、向最终 NPC 报告并领取奖励的完整客户端流程。

user acceptance confirmation: 用户原话“验证完成，提交，排查类似问题修复”；2026-09-14；未限定单一分支或步骤，按验收规则视为整个任务客户端流程验收完成。

server launch mode: not captured（服务端由用户管理；本会话未启动、停止或重启）

repository commit: `ce469009178a523ef9a58670d7ce35d4ef7240fc`（`fix(quest): allow optional 1922 turn-in item cleanup`）；本验收记录在其后的文档提交中。

working tree: dirty；本次修复提交只包含任务 1922 XML 与 `Quest1922RewardTurnInTest`。工作区中的传送、物品、Portal、JFR 和脚本缓存改动与本任务无关，均保留未提交。

Aion 5.8 client/data provenance: Aion 5.8 客户端；仓库内客户端证据为 `docs/quest/client-dialog-mapping/quest-dialog-pages.csv`、`quest-dialog-action-details.csv`、`client-html-pages.csv`、`client-hyperlinks.csv`；`QUEST_Q1922.html` 的来源 SHA-256 为 `8bfdaf8bb1303f128bf23fce584d82ba160830efa3c0d2b3c6b8a7e034129950`；旧行为对照为 `origin/history:src/main/java/com/aionemu/gameserver/quest/handlers/abyss_entry/_1922Deliveron_Your_Promises.java`，其奖励完成不检查或扣除 182206030。

npc template/object: 接取与前置对话 NPC template 203830；竞技场交接 NPC template 203764；最终报告与领奖 NPC template 203901。用户会话 trace 中的 runtime object ID 分别为 11473、13952、46222；稳定对象生成附件 not captured。

map/instance: world 310080000（Sanctum Underground Arena）；会话 trace 显示运行时 instance ID 2；离开实例后 final turn-in 在普通世界完成，入口/重入附件 not captured。

steps:

1. 与 NPC 203830 对话，沿 `QUEST_SELECT(31)` 打开 select1 页面；继续到 select1_1 页面后点击接受动作 `SETPRO12(10011)`，任务变量进入 `var0=4`。
2. 与 NPC 203764 对话，沿 select3(1693) -> select3_1(1694) 点击 `SETPRO3(10002)` 进入圣殿地下竞技场；完成计数后沿 select4(2034) -> select4_1(2035) 点击 `SETPRO4(10003)` 返回最终报告阶段。
3. 与 NPC 203901 对话，页面 3739 上的 `SELECT_QUEST_REWARD(1009)` 进入 `REWARD` 并打开奖励窗口 page 6；点击奖励选择动作 23 完成最终交任务。

source state/status/vars: `s7 / START / var0=7` -> `REWARD / var0=7` -> `COMPLETE / var0=0`；XML/planner 合同在完成时保留 `var4=10`，因此 packed vars 为 167772160。修复前的会话 trace 中任务 step 为 167772167，说明客户端任务追踪已经进入最终奖励阶段。

action/page/button: `QUEST_SELECT(31)` -> action `SETPRO3(10002)` / page 1694；`QUEST_SELECT(31)` -> action `SETPRO4(10003)` / page 2035；最终 `SELECT_QUEST_REWARD(1009)` -> page 6；奖励动作 `SELECTED_QUEST_NOREWARD(23)` -> COMPLETE。

expected response: 最终领奖事务依次发放 GOLD 4000、EXP 8325278、奖励物品 167000497，以 `RemoveItem.ALL` 可选清理 182206030，随后 `CompleteQuest(1)`；提交后按 `refresh-player-stats -> COMPLETION sync -> SELECT_QUEST(10)` 收尾。182206030（Fortress Flag）可能不在背包，缺失时不得阻断完成。

actual response: 用户确认客户端验证完成，按规则记录为完整任务流程通过；会话内预修复 trace 中动作 23 后没有完成响应，修复后的稳定 packet、运行日志和截图附件均未捕获。

startup health: not captured；服务端由用户管理，本会话未启动、停止或重启；未收到 typed quest engine 初始化失败、`QuestCompilationException`、`AMBIGUOUS_TRANSITION` 或 production catalog 编译失败的报告。当前会话使用现有 `target/classes` 通过 jshell 完成 XML 编译和 planner 检查，未运行 Maven。

runtime logs: not captured；会话内预修复 trace 覆盖 2026-09-14 09:33-09:36，包含任务 1922、NPC 203830/203764/203901、object 11473/13952/46222、page 3739/6 和 action 1009/23；稳定附件及 SHA-256 not captured。

protocol trace: not captured；会话内预修复包顺序显示 `SM_DIALOG_WINDOW(page=3739) -> CM_DIALOG_SELECT(action=1009) -> SM_QUEST_ACTION(status=REWARD) -> SM_DIALOG_WINDOW(page=6)`，最终动作 23 在修复前没有完成响应；稳定 trace 附件和 SHA-256 not captured。

screenshots/recordings and SHA-256: not captured。

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `REWARD_PREVIEW_OPTIONAL_WORK_ITEM`；匹配字段为旧任务工作物品允许“有则清理、无则继续”、严格 `RemoveItem` 会使 planner 将奖励/完成路由判为不可行、改用 `RemoveItem.ALL` 后缺失物品仍能完成。差异字段为 18600 的阻断点位于 reward preview，而 1922 的阻断点位于 `REWARD -> COMPLETE` 最终领奖事务；修复合同相同，因此不新增或改写 Playbook 案例。代表提交 `aea256a29521b4febb327fa99c4b1a7f2773d878`；代表测试 `Quest18600ClientDialogAlignmentTest#intermediateNpcPagesAndFinalRewardPagesFollowTheLegacyChain`；本次专项测试 `Quest1922RewardTurnInTest#finalTurnInCompletesWhenTheFlagIsMissing`。

remaining risks: 未捕获修复后的稳定日志、协议 trace、截图、运行时 object/world/instance 和重连/重登/重复领奖附件；未覆盖其他种族、职业或奖励分支；本会话未运行 Maven focused/catalog/whitelist 门禁，遵循项目未授权构建规则；未执行 push。
