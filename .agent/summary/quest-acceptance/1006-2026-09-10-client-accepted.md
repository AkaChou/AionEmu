# 任务 1006 客户端验收记录

quest: 1006「Ascension / 重生为守护者」

user acceptance confirmation: 用户原话“验证成功，提交，加入任务书”；2026-09-10；未限定职业分支或步骤，结合上下文视为 q1006 当前流程客户端验收完成。

server launch mode: IDEA（服务端由用户管理；运行 classpath 使用 `target/classes`）

repository commit: `59bba1a`（NPC 对话门控修复；q1006 XML、dispatcher 和专项测试已在父提交 `7c849af8`）

working tree: dirty；本次 q1006 相关路径为上述两个提交中的 `QuestEngine.java`、`QuestProductionDispatcher.java`、`1006.xml`、`Quest1006ClientDialogAlignmentTest.java` 和 `QuestProductionDispatcherTest.java`；其他副本、数据库初始化、q18602 等改动保留未提交。

Aion 5.8 client/data provenance: Aion 5.8 客户端；仓库证据为 `docs/quest/client-dialog-mapping/quest-dialog-pages.csv`、`quest-dialog-action-details.csv`、`client-html-pages.csv`、`client-hyperlinks.csv`；本次新采集客户端资源 SHA-256: not captured

npc template/object: 菲尔诺斯（Pernos）NPC template 790001；runtime object ID: not captured；interaction object provenance: NPC 静态 spawn、q1006 XML 和实际 typed 对话路由，稳定 packet object ID not captured

map/instance: `210010000` Poeta；转职副本 entry/reentry instance ID: not captured；本次路径为选择魔道星后离开副本并回到普通地图向 NPC 790001 领奖。

steps:

1. 使用 ELYOS 角色，进入 q1006 转职流程，在 `s5 / START / var0=5` 的魔道星分支选择 `SETPRO9`。
2. 离开转职副本；q1006 进入 `reward / REWARD / var0=5`，职业变为 SORCERER，返回 `210010000`。
3. 与 NPC 790001 菲尔诺斯对话；验证直接 `USE_OBJECT(-1)` 或通用 `QUEST_SELECT(31)` 能打开奖励选择入口，而不是只返回结束对话。
4. 按客户端奖励选择动作继续领取并完成 q1006；本次用户确认未限定其他职业、重连或重复领取分支。

source state/status/vars: `s5 / START / var0=5` -> `reward / REWARD / var0=5` -> `complete / COMPLETE / var0=0`；验收前只读数据库观察到 `REWARD / var0=5`，complete count 为 0。

action/page/button: `SETPRO9(10008)` -> `SetPlayerClass(SORCERER) -> teleport 210010000 -> LEVEL_AND_VISIBILITY_REFRESH`；`USE_OBJECT(-1)`/`QUEST_SELECT(31)` -> `SHOW_SELECT_QUEST_REWARD_WINDOW1(5)`；`SELECT_QUEST_REWARD(1009)` -> page 5；`SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD(8..23)` -> COMPLETE。

expected response: 转职选择提交后状态保持 `REWARD / var0=5`，职业为 SORCERER；菲尔诺斯的直接或通用任务入口在同一次交互中返回奖励选择窗口 page 5；选择奖励后事务进入 `COMPLETE`，并按 q1006 的完成 route 执行固定奖励、奖励选择、玩家属性刷新、`COMPLETION` 任务同步和最终任务页响应。

actual response: 用户确认“验证成功”；任务已能继续，未捕获独立 packet trace、稳定运行日志或最终截图附件。

startup health: typed quest engine 初始化未报告失败；服务器由用户管理，本次未启动、停止或重启；稳定启动日志附件: not captured

runtime logs: not captured；已做只读数据库检查，确认角色职业/阵营/地图及 q1006 `REWARD / var0=5`，无稳定仓库日志附件。

protocol trace: not captured；无稳定的 objectId、questId、page、action packet 序列附件。

screenshots/recordings and SHA-256: not captured；对话中的临时缓存截图未形成稳定仓库附件。

acceptance status: ACCEPTED_NEW_PATTERN

matched Pattern: `NPC_DIALOG_ROUTE_GATE_COLLISION`；匹配同一 NPC 的宽 `TalkToNpc(npcId)` 索引、未授权普通任务 1123 与已处于 `REWARD` 的 q1006 MISSION owner 发生门控冲突；修复用实际 `QuestEvent.matches` 过滤，并保留普通任务列表授权保护。代表提交 `59bba1a`；代表测试 `Quest1006ClientDialogAlignmentTest#returnsTheRewardWindowWhenTheClientUsesQuestSelectionIngress`、`QuestProductionDispatcherTest#ownerRouteLookupDoesNotConfuseAnotherTypedOwner`。

remaining risks: 未捕获稳定运行日志、协议 trace、截图、runtime object ID、转职副本 instance ID；未独立覆盖其他职业/种族、重连、死亡和重复领取路径；本会话未运行 Maven focused/catalog/whitelist 门禁（遵循项目未授权构建规则）；服务端重新加载后的启动健康由用户环境负责。
