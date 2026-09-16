# 任务 1220「A Secret Delivery / 秘密快递」客户端验收记录

quest: 1220「A Secret Delivery」（中文名「秘密快递」），天族，17 级可接，前置 1219 完成；NPC 乌内 203172 接取、努蒙 798004 中转、玛平恩恩 205240 交付领奖；覆盖整条任务（接取 → 换箱 → 交付 → 领奖完成），未限定单一分支或步骤。

user acceptance confirmation: 用户原话「1220 也过」（2026-09-16，回应本会话「是 1120 还是 1220」的确认请求）；未限定分支或步骤，按验收规则视为整条任务客户端验收完成。

server launch mode: not captured（服务端由用户管理；本会话未启动、停止或重启）

repository commit: `2169b6332`（`fix(dialog): stop echoing unhandled quest action ids as dialog pages`）——同一次提交同时包含共享 `DialogService` 回退分支修复与 1220 按钮链重建。验收时 HEAD 为 `15c79d31d`。

working tree: dirty；工作区含未提交的 3329 / 28301 / 80805 任务 XML、任务契约测试与 `quest-client-contract-baseline.tsv` 批次，以及 geoEngine/BIH、memory-bank、taloc 摘要等并行改动，均与本验收无关，保留未提交、未纳入本记录。

Aion 5.8 client/data provenance: Aion 5.8 客户端映射证据 `docs/quest/client-dialog-mapping/quest-dialog-pages.csv`（QUEST_Q1220.html，页 1011/4/1003/1004/1352/1353/2375/5）与 `quest-dialog-action-details.csv`（按钮 1007/1002/1003/1008/1353/10000/1009）；旧端模板证据 `legacy-quest-dialog-template-index.csv`（data_driven_quest / FULL / origin/history `zz_retail_simple_quests.xml`，sha256 `d8b94416e4f96778dec4b45641df097d693ba228`、`bad454f7f231ef391371a5250bd1644a5cd36a91a07a5e02e49b7c2986098dca`）；本次未重新采集客户端包哈希。

npc template/object:
- 接取 NPC：203172「Une」/ 乌内，Verteron `210030000` 静态刷点；runtime object ID not captured。
- 中转 NPC：798004「Shugo3」/ 努蒙，Verteron `210030000`。
- 交付与领奖 NPC：205240「shugo_Lender_LF2_01」/ 玛平恩恩，Eltnen `210020000`。
- 任务物品：革命团宝箱 182200568（接取发放、努蒙处交换）、182200569（送到玛平恩恩交付）；两者均声明为 `<work-items>`，完成或放弃都会回收。

map/instance: world `210030000`（Verteron）→ `210020000`（Eltnen），非副本；运行实例 ID not captured。

steps:
1. 与乌内 203172 对话：客户端页 select1(1011) → 1007 ASK_QUEST_ACCEPT 打开接取窗 page 4 → 1002 接受（拒绝走 1003 → page 1004）→ 接取时获得革命团宝箱 182200568。
2. 到努蒙 798004：select2(1352) → 按钮 1353 SELECT2_1 → 页 select2_1(1353) → 按钮 10000 SETPRO1 交出 182200568、换取 182200569，窗口关闭。
3. 到玛平恩恩 205240 交付：页 select5(2375) → 按钮 1009 SELECT_QUEST_REWARD → 奖励窗口 page 5。
4. 领取奖励，任务完成：GOLD 6620 + EXP 91950 + ITEM 188100335 ×14（固定奖励索引 0/1/2）。

source state/status/vars: `unaccepted`(var0=0) → `started`(var0=0) → `started1`(var0=1) → `reward`(var0=1) → `complete`(var0=0)。

action/page/button: 接取链 1011 → 1007 → page 4 → 1002；中转链 1352 → 1353 → 1353 → 10000；交付链 2375 → 1009 → page 5。action ID 与 page ID 分属独立命名空间（10000/1002 是按钮动作，不是页面 ID）。

expected response:
- 接取：NONE → START，接取事务内发放 182200568，显示接取窗口 page 4。
- 努蒙交换：仅在 `var0=0` 时提交，事务内移除 182200568、发放 182200569，提交后同步状态并关窗。
- 玛平恩恩交付：`started1 → REWARD`，显示奖励窗口 page 5。
- 领奖：发放固定奖励并置 COMPLETE；完成与放弃都按 `<work-items>` 回收两个宝箱。
- 客户端动作被拒绝时：`questId != 0` 且非通用任务列表动作 `QUEST_SELECT(31)` 时关闭窗口，不下发把动作 ID 当页面 ID 的对话页。

actual response: 用户确认整条任务可完成（客户端游戏内验收）；修复前在中转/接取点会出现的 `HtmlPageId 10000`、`HtmlPageId 1002` load fail 不再出现。未附截图、录屏、协议抓包或服务端日志。

startup health: not captured（服务端由用户管理；未收到 `Can't initialize typed quest engine`、`QuestCompilationException`、`AMBIGUOUS_TRANSITION` 或生产 catalog 编译失败的报告）

runtime logs: not captured

protocol trace: not captured

screenshots/recordings and SHA-256: not captured

acceptance status: ACCEPTED_NEW_PATTERN

matched Pattern: `UNHANDLED_QUEST_ACTION_ECHOED_AS_DIALOG_PAGE`；匹配字段为「任务引擎拒绝 `questId != 0` 的按钮动作后，`DialogService` 回退把动作 ID 当页面 ID 回显 → 客户端 load fail」，修复层为共享回退分支关窗 + 1220 按客户端按钮图重建；差异字段为同批 9550 由装备起始条件正常拒绝 `QUEST_ACCEPT_1(1002)` 触发同一回显路径。代表提交 `2169b6332`；代表测试 `DialogServiceQuestDialogTest#unhandledQuestAcceptActionClosesTheWindowInsteadOfEchoingTheActionId`、`DialogServiceQuestDialogTest#unhandledQuestStepActionClosesTheWindowInsteadOfEchoingTheActionId`、`Quest1220ClientDialogAlignmentTest#numonerkExchangesTheBoxAndMappinerkSettlesTheReward`。Playbook 已同步新增该 Pattern 与案例 8.36（同一提交）。

remaining risks: 本次只覆盖天族 1220；9550/9553 的装备条件拒绝路径只有静态与生产流测试，未在本次客户端复测；`questId == 0` 的普通 NPC 对话与通用任务列表动作 31 的第 10 页合同不属于本 Pattern；未捕获运行日志、协议抓包与截图；运行时 object ID 未记录。
