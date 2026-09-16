# 任务 3329「Dinner's On Me」客户端验收记录

quest: 3329「Dinner's On Me」（天族，31 级可接，Eltnen `210020000`）；Dalanius 203909 接取、Zinas 203956 交付领奖；计数怪 grassland rynoce 210887/210912 ×4（var0）与 mottled tipolid 210914/210932 ×6（var1）；奖励 EXP 404557 + ITEM 186000003 ×1。

user acceptance confirmation: 用户原话「3329 客户端验证成功」（2026-09-16），并附服务端 `[QUEST-TRACE]` 日志 17:50:59–17:52:07。粘贴日志覆盖接取 → 4+6 计数 → 203956 报告 → reward → complete 的正常链路；未包含“未满计数时在 203956 `SELECT2_2`(page 1438) 点击 SETPRO1(10000) 结束对话”的片段，该子场景仅为用户口述确认，日志证据未覆盖。

server launch mode: not captured（服务端由用户管理；本会话未启动、停止或重启）

repository commit: not committed；`src/main/resources/aion/data/static_data/quest_definition/quests/3329.xml` 仍为工作区未暂存改动，同批 28301/80805 XML、契约测试与 baseline 亦未提交。

working tree: dirty；含 3329/28301/80805 任务 XML、任务契约测试、`quest-client-contract-baseline.tsv`、geoEngine/BIH、taloc 摘要等并行改动。

Aion 5.8 client/data provenance: `docs/quest/client-dialog-mapping/quest-dialog-action-details.csv` 中 quest 3329 行——page 1011 action 1007、page 4 action 1002/1003、page 1003/1004 action 1008、page 1352 action 1353、page 1353 action 1438、page 1438 action 10000(SETPRO1)、page 2375 action 1009；`legacy-quest-dialog-contracts.csv` 记录 `monster_hunt / FULL / 203909 -> 203956`。本次未重新采集客户端包哈希。

npc template/object:
- 接取 NPC 203909「Dalanius」，runtime targetObj=49937。
- 交付 NPC 203956「Zinas」，runtime targetObj=48331。
- 计数怪 210887/210912「grassland rynoce」、210914/210932「mottled tipolid」。

map/instance: world `210020000`（Eltnen），非副本；运行实例 ID not captured。

steps:
1. 17:51:00 与 203909 对话 action 31 `QUEST_SELECT`（page 10 → 1011）→ 17:51:01 action 1007 `ASK_QUEST_ACCEPT`（1011 → 4）→ 17:51:02 action 1002 `QUEST_ACCEPT_1`（4 → 接取：`SM_QUEST_ACTION 状态=3 步数=0`，page 1003）。
2. 17:51:12–17:51:56 击杀计数：步数 1/2/3/4，随后 68/132/196/260/324/388；388 = var0(4) + var1(6) × 64，满足 counter-grid `var0=4`、`var1=6`。
3. 17:52:06 与 203956 对话 action 31（page 10 → 2375 `SELECT5`）→ 17:52:07 action 1009 `SELECT_QUEST_REWARD`（2375 → page 5；`SM_QUEST_ACTION 状态=4 步数=388`，即 REWARD 节点 var0=4/var1=6）→ 17:52:07 action 23 领奖（page 5 → `SM_QUEST_ACTION 状态=5 步数=0`，任务窗关闭 questId=0 page 10）。
4. 未在粘贴日志中出现：a0b0 阶段 203956 page 1438(`SELECT2_2`) action 10000(SETPRO1) 的提前结束对话点击。

source state/status/vars: `a0b0`(var0=0, var1=0) → 计数后 `a4b6`(var0=4, var1=6) → `reward`(4, 6) → `complete`(0, 0)；日志状态 3/4/5 与 START/REWARD/COMPLETE 对齐。

action/page/button: 接取链 10 → 31 → 1011 → 1007 → 4 → 1002 → 1003；完成链 10 → 31 → 2375 → 1009 → 5 → 23 → 10；SETPRO1 按钮为 page 1438 的 action 10000。

expected response:
- 计数未满时，203956 `SELECT2_2` 的 SETPRO1 只关闭对话，不进入 reward、不发奖——本次 XML 修复目标。
- 计数满 4+6 后，`a4b6 -> reward -> complete` 发放 EXP 404557 + ITEM 186000003 ×1。

actual response: 正常链路与预期一致：状态 3 步数 0 接取；计数 4+6（步数 388）；203956 报告后状态 4 步数 388；领奖后状态 5 步数 0，questId=0 page 10。粘贴 trace 未见提前发奖，亦未见提前 SETPRO1 点击片段。

acceptance status: ACCEPTED（用户确认 + 正常完成链路服务端 trace）；提前 SETPRO1 子场景按用户口述确认通过，日志证据未覆盖。

remaining risks: 28301、80805 客户端验收仍 PENDING；3329 提前 SETPRO1 场景未留日志；baseline 中 23 条 `BUTTON_WITHOUT_ROUTE` 既有缺口未修。
