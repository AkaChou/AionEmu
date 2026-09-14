# 任务 3926 客户端验收记录

quest: 3926「The Sorcerer Preceptor's Task / 魔道星老师的考验」
user acceptance confirmation: 用户原话“验证通过”；2026-09-09；结合当前任务上下文，未限定职业分支或步骤，按规则视为整个任务客户端流程验收完成。
server launch mode: not captured（服务端由用户管理）
repository commit: 880af62b7
working tree: dirty；3926 修复 XML 与专项测试已在上述提交中；当前无暂存改动，工作区仅保留未跟踪的 `.agents/summary/quest/__pycache__/` 与 `../../../startup-20260824.jfr`；其间已有并发提交 `adc5cbc0b`，未改写。
Aion 5.8 client/data provenance: Aion 5.8 客户端；仓库证据见 `../../../docs/quest/client-dialog-mapping/quest-dialog-pages.csv`、`quest-dialog-action-details.csv`、`quest-sequence-audit.csv`；newly collected SHA-256: not captured
npc template/object: NPC template 203706；runtime object ID: not captured；interaction object provenance: not captured
map/instance: world/instance not captured；entry/reentry context: not captured

steps:
1. 起点为满足 31 级、ELYOS、MAGE/SORCERER 条件的角色，与 NPC 203706 对话查看任务。
2. 沿客户端接取链查看任务、打开接受窗口，并提交 `QUEST_ACCEPT_1(1002)`。
3. 按正常任务流程继续任务并完成；本次用户确认未限定额外的重连、死亡或重复领取分支。

source state/status/vars: `unaccepted/NONE` -> `started/START` -> `reward/REWARD` -> `complete/COMPLETE`；3926 无需要记录的任务变量。
action/page/button: NPC 203706 `QUEST_SELECT(31)` -> `SELECT_NONE(4762)`；`ASK_QUEST_ACCEPT(1007)` -> 接受窗口 4；`QUEST_ACCEPT_1(1002)` -> `QUEST_ACCEPT_1(1003)`；拒绝 `1003` -> `1004`。
expected response: 接受动作满足 `start-eligible` 后事务内提交 `unaccepted -> started`，保留奖励称号 38；提交后按 `VISIBILITY_REFRESH` 同步任务状态，再显示客户端页面 1003，不请求不存在/错误的页面 1002。
actual response: 用户确认“验证通过”；未捕获具体页面、状态变量、奖励包或协议附件。

startup health: not captured；未启动或重启服务端，用户未报告 typed quest engine 初始化失败或 `QuestCompilationException`。
runtime logs: not captured；无稳定时间窗口、角色、NPC、任务、世界或实例日志附件。
protocol trace: not captured；无稳定的 packet order、objectId、questId、page 或 action trace 附件。
screenshots/recordings and SHA-256: not captured；对话中的原始截图未形成稳定仓库附件。

acceptance status: ACCEPTED_NEW_PATTERN
matched Pattern: `REWARD_TITLE_AS_START_PREREQUISITE`；匹配项为接受动作的 `start-eligible`、metadata `title-id` 与奖励 `TITLE` 的同 ID 冲突、legacy 仅有奖励称号，以及客户端 `1002 -> 1003` 接取合同。与最接近的 `INTRO_CHAIN_ACCEPT_PROMPT_BRIDGE_MISSING` 不同：3926 不是介绍链末页缺少 page 4 桥接，而是资格条件先把接受 transition 拒绝；代表提交 `880af62b7`，代表测试 `Quest3926ClientDialogAlignmentTest#keepsTheRetailAcceptanceChainAndTreatsTitleAsAReward`。
remaining risks: 当前未重跑 Maven focused/catalog/whitelist 门禁；未采集启动、运行日志、协议和稳定截图。只读扫描发现同类明确候选 19075、11033、2434、29074、3922–3925、3927–3929、4923–4925、4928；10521/20521 需结合现有自动接取测试确认是否为有意项目语义；2511 有 legacy `titleId=75`，不属于本模式；1322 为相反方向的 legacy `titleId=4` 遗漏。
