# 1217 客户端验收记录（2026-10-07）

quest: 1217「해안을 더럽히는 상자 / Flotsam」（坎塔斯海岸的箱子，天族 Verteron）
user acceptance confirmation: 用户实机复测后明确回复「实机验证，可以完成，击杀 6 个显示 6/7，击杀 7 个显示 10/7，但是任务是正确完成了的」（2026-10-07）；整任务验收（杀满即推进到报告步），未限定分支或步骤
server launch mode: IDEA（常驻进程；用户自行重启后加载含修复的构建，本记录不涉及服务端生命周期操作）
repository commit: `8509228d9`（验收时工作区含该修复的未提交版本；repair 已按本记录落库为该提交）
working tree: dirty during acceptance（修复与 summary/memory-bank 待提交）；repair 落 `8509228d9`
Aion 5.8 client/data provenance: Aion 5.8 客户端（中文 L10N 覆盖包 `L10N/CHS/Data/data.pak`，SHA-256 未采集）；客户端页 `Dialogs/QUEST_Q1217.html`（摘要 `([%2]/7)`、接取台词「清除掉7个」）；客户端门控 `data/Quest/Quest.pak → quest_monster.csv` 的 1217 行 `Progress(SECTION_0<10; SECTION_5==0)`
npc template/object: 接取与交付 NPC 203184（Phorcys）；击杀目标 210197（FakeBox_19_n）/ 210085（Mimic_19_n）
map/instance: 普通世界（坎塔斯海岸，Verteron 区域）；无实例

steps:
1. 前置：在 Phorcys 203184 处接取 1217（`START`，打包计数 0）
2. 击杀箱子怪至 6 只 → 任务书计数显示 `6/7`（与修复前的 `x/7` 一致，只是推进点变更）
3. 击杀第 7 只 → 任务推进到「向普尔奇斯报告」步（任务正确完成）

source state/status/vars: 1217 `START`（var0/计数 0…7）→ 第 7 杀写完成态（`0x100`）并进入 `REWARD`
action/page/button: 普通攻击击杀 FakeBox_19_n / Mimic_19_n（`onKill` 计数路径）
expected response: 计数按 6 位字段逐杀 +1，第 7 杀达到完成值 7（`RetailHuntCounterLayout.goal`）并进入 `REWARD`，任务书走到报告行
actual response: 用户实机确认「任务是正确完成了的」——第 7 杀即推进到报告步；同一次击杀后计数条分子显示 `10`（`10/7`），为客户端侧观感残留（见 remaining risks）

startup health: 用户重启加载修复后正常运行；未报告 `Can't initialize typed quest engine`、`QuestCompilationException`、`AMBIGUOUS_TRANSITION` 或 catalog 编译失败
runtime logs: not captured
protocol trace: not captured
screenshots/recordings and SHA-256: not captured

acceptance status: ACCEPTED_NEW_PATTERN
matched Pattern: `KILL_COUNT_CONTRACT_NOT_PAGE_TEXT`（新）；representative commit `8509228d9`；`RetailSimpleHuntTableTest#quest1217KeepsThePlayerVisibleKillCountOfSeven`
remaining risks: ① 完成态计数条显示 `10/7`：服务端完成值已是 7（`cameraSpec.fullValue = Σ count<<shift`），分子 10 来自客户端自身 `quest_monster.csv` 门控 `SECTION_0<10` 未同步到 7 的渲染；玩家确认不影响完成，若日后要求显示一致需按 CPK-001 对客户端 `Quest.pak` 内 `quest_monster.csv` 的 1217 行做单条目补丁（`SECTION_0<10` → `SECTION_0<7`）。② 1217 是唯一按玩家可见页面口径改数的任务；同型的 1750（真端 15 vs 页面 5，页面点名旧模板怪）与 1840（真端 44 vs 页面 43）保持真端值，未跟随。③ 本次只覆盖单角色击杀路径；重登、任务分享、放弃后重接未复测。④ 真端表副本的偏差仅靠 `RetailSimpleHuntTableTest#quest1217KeepsThePlayerVisibleKillCountOfSeven` 与行内注释守卫，真端表整体重导入时仍需人工核对。
