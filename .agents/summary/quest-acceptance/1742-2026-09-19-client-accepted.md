# 1742 重开局验收（2677 的天族同形验证）

```text
quest: 1742「Immortal Soul」（ELYOS，min-level 45，报告/开局 NPC 278591 Deidamia）；
  作为 2677「[Instance/Group] Kindling The Flames」（ASMODIANS，NPC 204817）COMPLETE 重开局修复的
  天族同形验证对象——按用户 2026-09-19 规则「如果是魔族任务，则用天族对等任务来测」
user acceptance confirmation: 用户原话「1742 验证成功」；2026-09-19；未限定分支或步骤
server launch mode: not captured（服务端由用户管理；本会话未启动、停止或重启）
repository commit: `464df58bb`（`fix(quest): align kill counters, repeat start dialogs and reported rewards`，
  补齐 2677 的 `complete -> complete SELECT1_1(1012)` 路由 `<start-eligible/>`）；本验收记录在其后的文档提交中
working tree: 记录时任务相关路径无未提交改动；工作区内并行的 collect-progress 批次与 memory-bank 文档不纳入本记录
Aion 5.8 client/data provenance: 客户端页面/动作来自仓库既有映射
  （`docs/quest/client-dialog-mapping/`，`SELECT1_1` = action/page 1012）与 legacy handler 合同；
  本次未重新采集客户端包哈希
npc template/object: 278591「Deidamia」（ELYOS、ELITE、NON_ATTACKABLE、lv40）；runtime object ID not captured
map/instance: not captured（验证点是 NPC 任务的 COMPLETE 重开局路由，不依赖特定世界或副本）

steps:
1. 让 1742 处于 COMPLETE（可重复任务；GM 可用 `//quest set 1742 COMPLETE 0`，该命令会自增 completeCount）。
2. 与 278591 对话，点任务列表中的该任务行（客户端动作 `SELECT1_1` = 1012）。
3. 观察开局页是否在同一次对话内打开、且任务可重新接取。

source state/status/vars: COMPLETE（completeCount >= 1），var0 = 0
action/page/button: `CM_DIALOG_SELECT` action 1012（SELECT1_1）→ 服务端 `SHOW_QUEST_PAGE SELECT1_1(1012)`
expected response: 带 `start-eligible` 的 `complete -> complete` 路由命中，同一次对话回发开局页
  （无 load fail、窗口不闪退），任务可重新接取
actual response: 用户确认「1742 验证成功」；未提供抓包、截图或日志，技术产物记为 not captured

startup health: not captured（服务端由用户管理；未收到 typed quest engine 初始化失败、
  `QuestCompilationException`、`AMBIGUOUS_TRANSITION` 或生产 catalog 编译失败的报告）
runtime logs: not captured
protocol trace: not captured
screenshots/recordings and SHA-256: not captured

acceptance status: ACCEPTED_NEW_PATTERN
matched Pattern: `REPEATABLE_REOPEN_START_ELIGIBLE`（新 Pattern，指纹与案例随门禁测试一并落 Playbook）；
  matched fields：`complete -> complete` 重开局路由 + `start-eligible` + 同 NPC 回发开局页；
  differing fields：representative commit `464df58bb` 的修复对象是魔族 2677，本次实机验证对象是天族同形 1742；
  representative test `QuestRepeatableReopenStartEligibleTest#completeReopenRoutesCarryStartEligibleAndReshowTheStartPage`
  （**PENDING**：测试文件与 Playbook 行随下一次提交落地，聚焦 Maven 命令尚未执行）
remaining risks:
- 2677 本体为魔族任务，本次按用户规则以天族同形 1742 验证，未在魔族角色上直验。
- 天族 11053（NPC 799017）、11202（NPC 799009）为同形静态旁证，未逐一实机点验。
- 本轮 5 组待验项中的 13758 族、25640/25698、13841、26930 仍未验收。
```
