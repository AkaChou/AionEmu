# 任务 1120「Thinning out Worgs / 扫荡野狼」客户端验收记录

quest: 1120「Thinning out Worgs」（中文名「扫荡野狼」），天族，6 级可接，NPC 203082 起始与报告；覆盖整条任务（接取 → 9 杀 → 报告 → 二选一奖励 → 完成），未限定单一分支或步骤。

user acceptance confirmation: 用户原话「1120 已经验证通过」（2026-09-16）与「1120 也验收通过」（2026-09-16）；未限定分支或步骤，按验收规则视为整条任务客户端验收完成。

server launch mode: not captured（服务端由用户管理；本会话未启动、停止或重启）

repository commit: `4574adf03`（`feat(quest): migrate client dialog protocol`，`quests/1120.xml` 当前内容所属提交；`counter-grid` 形状由 `85044415f` 的 domain-block 迁移引入）。验收时 HEAD 为 `6d847cc7c`。

working tree: dirty；工作区含未提交的 3329 / 28301 / 80805 任务 XML、任务契约测试与 `quest-client-contract-baseline.tsv` 批次，以及 geoEngine 并行改动，均与本验收无关，保留未提交、未纳入本记录。

Aion 5.8 client/data provenance: Aion 5.8 客户端映射证据 `docs/quest/client-dialog-mapping/quest-dialog-pages.csv`（QUEST_Q1120.html，sha256 `bb388c214675b17b745c9dec4ff0621668279b3d623fdb060094962124d3877a`）与 `quest-dialog-action-details.csv`；旧端模板证据 `legacy-quest-dialog-template-index.csv`（monster_hunt / FULL / origin/history `zz_retail_simple_quests.xml`，sha256 `d8b94416e4f96778dec4b45641df097d693ba228`、`bad454f7f231ef391371a5250bd1644a5cd36a91a07a5e02e49b7c2986098dca`）；本次未重新采集客户端包哈希。

npc template/object:
- 接取与报告 NPC：203082「Tula」（`npcs/npc_template_200000_216188.xml`），Poeta 静态刷点；runtime object ID not captured。
- 击杀目标：210142、210143（同在 Poeta 刷点）；两个 template 任一均计一次击杀。

map/instance: world `210010000`（Poeta），非副本；运行实例 ID not captured。

steps:
1. 与 203082 对话：客户端页 SELECT1(1011) → ASK_QUEST_ACCEPT(1007) 打开接取窗口 page 4 → QUEST_ACCEPT_1(1002) 接取（拒绝走 1003 → QUEST_REFUSE_1/1004）。
2. 在 Poeta 击杀 210142/210143 共 9 只，var0 由 0 逐级推进到 9。
3. 第 9 杀后回到 203082 报告：SELECT2(1352) → SELECT_QUEST_REWARD(1009) → 奖励窗口 page 5。
4. 选择奖励完成：固定 GOLD 3040 + EXP 4455，可选 ITEM 162000048 ×1 或 ITEM 169000003 ×250。

source state/status/vars: `unaccepted`(var0=0) → `started`(var0=0) → `k1..k9`(var0=1..9) → `reward`(var0=9) → `complete`(var0=0)。

action/page/button: 接取链 1007 → page 4 → 1002；报告链 1009 → page 5。action ID 与 page ID 分属独立命名空间（1011/1007/4/1002/1352/1009/5）。

expected response:
- 接取：NONE → START，先显示接取窗口 page 4，确认后进入 `started`。
- 每次击杀目标 NPC：var0 精确 +1，状态保持 START，并按 PACKET_ONLY 同步客户端计数。
- 第 9 杀后向 203082 报告：进入 REWARD 并打开奖励窗口 page 5。
- 领取后发放固定与可选奖励，任务置 COMPLETE 并清理工作状态。

actual response: 用户确认整条任务可完成（客户端游戏内验收）；未附截图、录屏、协议抓包或服务端日志。

startup health: not captured（服务端由用户管理；未收到 `Can't initialize typed quest engine`、`QuestCompilationException`、`AMBIGUOUS_TRANSITION` 或生产 catalog 编译失败的报告）

runtime logs: not captured

protocol trace: not captured

screenshots/recordings and SHA-256: not captured

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: 无新增 Pattern。本次是对既有 monster_hunt `counter-grid` 生产合同的客户端确认，没有对应修复提交，也没有产生新的问题模式；`COUNTER_SOURCE_PROJECTION_NO_LOCK`（26802，`START` 节点投影锁）与 `INTERACTION_OBJECT_TASK_SPECIFIC_CONTRACT` 的复用边界都不覆盖本任务，因此不新增 Playbook 案例，也不修改 `PATTERNS.zh-CN.md` / `CASES.zh-CN.md`。代表回归：`MonsterHuntFamilyDefinitionTest#thinningWorgsKillChainAdvancesVar0OneStepPerKill`、`MonsterHuntFamilyDefinitionTest#completionRewardsCarrySelectableItemsAndFixedRewards`、`MonsterHuntFamilyDefinitionTest#packagedProductionDirectoryCompilesTheThreeHuntOwners`。

remaining risks: 本次只覆盖天族 1120（NPC 203082 与 Poeta 的 210142/210143）；不自动覆盖同族 1112/1113，也不覆盖其余使用 `counter-grid` 的任务；未捕获运行日志、协议抓包与截图；运行时 object ID 未记录。
