# 任务 18600 客户端验收记录

quest: 18600「Scoring Some Bad Stigma / [副本]假烙印之石的流通」

user acceptance confirmation: 用户原话“客户端验证完成，提交，加入任务书案例”；2026-09-09；未限定分支或步骤，按规则视为整个任务客户端流程验收完成。

server launch mode: not captured（服务端由用户管理）

repository commit: `aea256a29521b4febb327fa99c4b1a7f2773d878`（`fix(quest): repair 18600 reward dialog flow`）

working tree: dirty；本次验收对应的 18600 XML 与专项测试已包含在上述提交；18602 XML/测试、Kromede 副本 NPC/spawn/实例测试、JFR 和脚本缓存属于工作区其他改动，均未纳入本次提交。

Aion 5.8 client/data provenance: Aion 5.8 客户端；仓库证据为 `../../../docs/quest/client-dialog-mapping/quest-dialog-pages.csv`、`quest-dialog-action-details.csv`、`client-html-pages.csv`、`client-hyperlinks.csv`；对应 `10000_19999/quest_q18600.html`，source SHA-256 `860501ea0aec73e7eeb57aafe32bf6de68b78a335a7275b7a6485c0370d884c7`。

npc template/object: 接取/最终领奖 NPC template 204500；中间 NPC template 804601、205228；runtime object ID not captured；交互对象来源为任务 XML、`origin/history` 旧 handler 与 Aion 5.8 客户端页面/action 合同。

map/instance: Kromede's Trial；world/instance ID not captured；entry/reentry context not captured。

steps:

1. 满足 ELYOS、等级 37+、任务 1527 未完成且未接取的条件，与 NPC 204500 对话，沿 `QUEST_SELECT(31) -> SELECT1(1011) -> ASK_QUEST_ACCEPT(1007) -> QUEST_ACCEPT_1(1002)` 接取并获得 182213000（Paper Bill）。
2. 与 NPC 804601 对话，沿 `SELECT2(1352) -> SELECT2_1(1353) -> SETPRO1(10000)` 交付 Paper Bill，进入 `s1 / START / var0=1`。
3. 与 NPC 205228 对话，沿 `SELECT3(1693) -> SELECT3_1(1694) -> SETPRO2(10001)`，进入 `reward / REWARD / var0=3`。
4. 返回 NPC 204500，直接对话 `USE_OBJECT(-1)` 或通用任务入口 `QUEST_SELECT(31)` 均显示 `SELECT5(2375)`；点击“拿出烙印之石并报告结果”发送 action 1009，打开奖励窗口 page 5，选择奖励完成任务。

source state/status/vars: `unaccepted / NONE / var0=0` -> `started / START / var0=0` -> `s1 / START / var0=1` -> `reward / REWARD / var0=3` -> `complete / COMPLETE / var0=0`；complete count 初始为 0。

action/page/button: `QUEST_SELECT(31) -> SELECT1(1011)`；`QUEST_SELECT(31) -> SELECT2(1352)`；`SELECT2_1(1353) -> SETPRO1(10000)`；`QUEST_SELECT(31) -> SELECT3(1693)`；`SELECT3_1(1694) -> SETPRO2(10001)`；`USE_OBJECT(-1)/QUEST_SELECT(31) -> SELECT5(2375)`；`SELECT_QUEST_REWARD(1009) -> SHOW_SELECT_QUEST_REWARD_WINDOW1(5)`；奖励选择 `SELECTED_QUEST_REWARD1..6(8..13)` -> COMPLETE。

expected response: 804601 交付后按 `LEVEL_AND_VISIBILITY_REFRESH -> SELECT_QUEST(10)`，205228 报告后按同一顺序进入 `REWARD var0=3`；204500 的 reward preview 必须在同一次交互中返回 page 5。182213001（Fake Stigma）属于旧流程中“有则清理”的工作物品，preview 使用 `RemoveItem.ALL` 不应因其缺失阻断页面；完成 route 依次执行固定奖励、可选奖励、`COMPLETE`，提交后按 `refresh-player-stats -> COMPLETION sync -> SELECT_QUEST(10)` 收尾。

actual response: 用户确认客户端验证完成，按规则记录为完整任务流程通过；未捕获独立 packet、运行日志或稳定截图附件。

startup health: not captured；未启动或重启服务端，用户未报告 typed quest engine 初始化失败或 `QuestCompilationException`。

runtime logs: not captured；无稳定时间窗口、角色、NPC/object、任务或 world/instance 日志附件。

protocol trace: not captured；无稳定的 objectId、questId、page 或 action 包序列附件。

screenshots/recordings and SHA-256: not captured；对话中的临时缓存截图未形成稳定仓库附件。

acceptance status: ACCEPTED_NEW_PATTERN

matched Pattern: `REWARD_PREVIEW_OPTIONAL_WORK_ITEM`；匹配 reward preview 缺少可选工作物品时仍应进入 page 5、`RemoveItem.ALL` 的 fail-open 清理语义，以及 `QUEST_SELECT/USE_OBJECT -> SELECT5 -> page 5` 的动作/页面合同。与已有 `QUEST_REWARD_PREVIEW_PAGE_CONTRACT` 的差异是本任务的目标奖励页本身是正确的 page 5，失败根因是严格正数扣除使 preview route 不可执行；与 `MULTI_NPC_HANDOFF_REWARD_OWNER` 的差异是本案例额外证明 reward preview 对缺失工作物品的可行性。代表提交 `aea256a29521b4febb327fa99c4b1a7f2773d878`；代表测试 `Quest18600ClientDialogAlignmentTest#intermediateNpcPagesAndFinalRewardPagesFollowTheLegacyChain`。

remaining risks: 未捕获运行日志、协议 trace、稳定截图、运行时 object ID、重连/重登/死亡/重复领取路径；本会话未运行 Maven focused/catalog/whitelist 门禁（遵循项目未授权构建规则）；服务端重新加载后的启动健康仍由用户环境负责。
