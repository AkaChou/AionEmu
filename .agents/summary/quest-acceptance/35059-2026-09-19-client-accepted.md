# 35059 势力日常接取与击杀计数客户端验收记录

quest: 35059「[Daily] Silence the Shardjaws」（天族 56–57 级，Alabaster Order 势力日常；报告 NPC Laysean 804942，Cygnea world 210070000）。

user acceptance confirmation: 用户原话“35059 验证成功”；2026-09-19；未限定单一分支或步骤，按验收规则（rule 4）视为整任务可玩，即
接取 → 击杀 10 只 Shardjaw Kirrus（235817/235818）→ 回报 Laysean 领奖的完整链路通过。

server launch mode: not captured（服务端由用户管理；本会话未启动、停止或重启）

repository commit: `0f2f3145d`（`fix(quest): declare faction ownership for 218 faction dailies`）与 `30d2daff9`
（`fix(quest): require exactly the client kill gate in 34 single-counter quests`，35059 在两者交集中）；
本验收记录在其后的文档提交中。

working tree: clean（提交本记录时，任务相关路径无未提交改动；工作区内其它协作者的文件未纳入本任务）

Aion 5.8 client/data provenance: Aion 5.8 客户端解包表 `quest.xml`（35059：`client_level=56`、
`minlevel_permitted=56`、`maxlevel_permitted=57`、`npcfaction_name=GuardianOfDivine`）与
`quest_monster.csv`（`Progress(SECTION_0==0; SECTION_1<10)`）；服务端侧 `quest_data.xml`
（`npcfaction_id="2"`、`maxlevel_permitted="57"`）与 `npc_factions_quest.xml`（faction 2，7 天全开）。
本次未重新采集客户端包哈希。

npc template/object:
- 报告/接取 NPC：804942「Laysean」（ELYOS、NON_ATTACKABLE、lv65），Cygnea 单点刷新 (1396.07, 616.18, 583.50)，属 Alabaster Order 日常发放者之一。
- 阵营出生 NPC（加入势力）：Cygnea「Mirtis」805145 (2897.01, 916.05, 574.41)；Heiron「typhon」799803。
- 击杀目标：235817「Wandering Shardjaw Kirrus」(lv57)、235818「Hunting Shardjaw Kirrus」(lv58)，均在 Cygnea 刷新。
- runtime object ID not captured。

map/instance: world 210070000（Cygnea，天族）；instance ID not applicable（非副本任务）。

steps:
1. 以 56–57 级天族角色加入 Alabaster Order（与 Mirtis 805145 对话），等待/触发当日势力日常派发。
2. 到 Cygnea 与 Laysean 804942 对话，在任务列表中接受 35059（接取页 1011 / `QUEST_ACCEPT_1`、接受窗口 4）。
3. 击杀 10 只 Shardjaw Kirrus（235817/235818）观察计数器与任务条目推进。
4. 回报 Laysean，领取 EXP 1477634 与 `coin_protection_100`/`186000100` ×6。

source state/status/vars:
- 接取前：35059 为 NONE（或可重复的 COMPLETE），`var1` 位段 0。
- 击杀中：`var0=0`，`var1` 随每次击杀 +1，至 `var1=10` 时任务转入 REWARD（`var0=1`）。
- 修复前：需要第 11 次击杀才进入 REWARD（`below 10` 累加、`at-least 10` 收口）。

action/page/button: 客户端接取动作 `ASK_QUEST_ACCEPT(1007)` → `QUEST_ACCEPT_1(1002)`/`QUEST_ACCEPT_SIMPLE(20000)`；
报告动作 `QUEST_SELECT(31)`、`SELECT_QUEST_REWARD(1009)`；领奖页 `DEFAULT_SUCCESS` / `SHOW_SELECT_QUEST_REWARD_WINDOW1`。

expected response:
- 接取：35059 出现在 Laysean 任务列表并可接受后进入 START（修复前因缺 `npc-faction-id` 永不入每日候选池、客户端无入口）。
- 击杀：正好 10 只即推进到 REWARD（修复前需 11 只）。
- 回报：进入领奖窗口并正常发放奖励。

actual response: 用户确认“35059 验证成功”；未提供抓包、截图或日志，技术产物记为 not captured。

startup health: not captured；服务端由用户管理，本会话未启动、停止或重启，未收到 typed quest engine 初始化失败、
`QuestCompilationException`、`AMBIGUOUS_TRANSITION` 或生产 catalog 编译失败的报告。

remaining risks:
- 阵营轮换入口细节（当日轮换随机命中 vs GM 辅助）未单独留证；本记录按 rule 4 以“整任务可玩”为验收范围。
- 同批其它未验收任务（13758/13761/13764/13767 五杀族、25640/25698 满计数恢复、2677 重开局、13841 无目标领奖、26930 扣量）
  仍按 `.agents/summary/quest-acceptance/2026-09-18-kill-counter-and-repeat-dialog-pending-client.md` 待客户端验收。
