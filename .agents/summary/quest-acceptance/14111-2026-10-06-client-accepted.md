# 14111 客户端验收记录（2026-10-06）

quest: 14111「Prey for the Lepharists / 处决农场上的雷帕尔革命团」
user acceptance confirmation: 用户明确回复「14111 已经实机验证成功」（2026-10-06）；整任务验收，未限定分支或步骤
server launch mode: IDEA（常驻进程；用户重启后加载含修复的构建）
repository commit: `cfaaf4230eb4ecea7992c3604dd1331a0d9c8c93`（验收时工作区含该修复的未提交版本；repair 已按本记录落库）
working tree: dirty during acceptance（修复与 summary/memory-bank 待提交）；repair 落 `cfaaf4230`
Aion 5.8 client/data provenance: Aion 5.8 客户端（用户实机）；客户端对话页契约 `src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv`（14111 声明 4/1003/1004/1011/1012/1352）
npc template/object: 203126（Abolos；`npc_template_200000_216188.xml:2779`）
map/instance: 普通世界（序章地图）；无实例

steps:
1. 前置：14111 已接取且击杀完成（LehparAs_11_An×9 + LehparWaNamed_12_An×1，状态=4 `REWARD`，步数 73）；同一 NPC 203126 上 1155（SimpleCollectItem `START`）进行中
2. 打开 NPC 203126 对话，重放应命中 14111 的奖励窗（修复前落两参通用页 10、14111 不可见）

source state/status/vars: 14111 `START` → `REWARD`（状态 4，步数 73）；1155 `START`
action/page/button: 打开对话（-1、questId=0）
expected response: `SM_DIALOG_WINDOW(targetObj, 5, 14111)`（页 5 奖励窗）→ 领奖后 14111 状态 5
actual response: 用户实机确认 14111 验证成功（奖励窗可达并可交付）

startup health: 用户重启加载修复构建后正常；无 typed quest engine 初始化异常报告
runtime logs: 修复前诊断窗口 2026-10-06 18:17-18:28（`log/quests.log`，仓库内）；
              验收时刻日志未采集（`not captured`）
protocol trace: 修复前 trace 见 `.agents/summary/quest-14111-open-door-replay-order/DIAGNOSIS.zh-CN.md`；验收时 `not captured`
screenshots/recordings and SHA-256: `not captured`

acceptance status: ACCEPTED_NEW_PATTERN
matched Pattern: `OPEN_DOOR_REPLAY_REWARD_FIRST`（新）；representative commit `cfaaf4230`；`QuestEngineOpenDoorReplayOrderTest#openDoorReplaysTheDeliverableQuestBeforeAnEarlierLiveOne`
remaining risks: 多 `REWARD` 任务按 questId 升序交付第一个（逐个交付语义未实机）；其它 NPC 的同型场景未逐一复测；13403 两组修复待实机复测（不在本记录范围）
