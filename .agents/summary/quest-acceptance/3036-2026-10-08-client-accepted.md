# 任务 3036 客户端验收记录

quest: 3036「Let's See What It Does / 实体的本来面目」（天族，Theobomos 链 3035→3036）

user acceptance confirmation: 用户 2026-10-08 原话“实机验证成功，提交”；2026-10-08；按提交前给出的复测口径（无 load fail / 任务步骤恢复 / Atropos 领奖）视为整个任务客户端流程验收完成。未提供逐步操作序列与截图附件。

server launch mode: 服务端由用户管理（仓库规则：AI 不启停）；两轮复测分别发生在 2026-10-07 23:23 与 23:49 的会话内（log/quests.log）。

repository commit: 随本次修复提交一并落地（`fix(quest): 3036 圣物领奖收尾 load fail + 任务步骤空白——物件 owner 收敛/零回页 + 领奖态步号轴回归 legacy 值`）；验收记录与本提交同批；提交时基线 HEAD 为 `d10993395`（并行任务 14047/QE-161 的提交，不含本任务改动）。

working tree: dirty；本次提交 = 3036.xml、`Quest3036ClientDialogAlignmentTest`（新）、`RewardRowProjectionRegressionTest`（追加 `Row(3036, 1, 0, false)`）、`JournalRewardRowRepairContractTest`（移出 3036）、审计脚本 `LEGACY_STEP_EXCEPTION` 登记、本文档与本任务证据文档、记忆库 QE-052/QE-056（仅本任务 hunks）。

Aion 5.8 client/data provenance: Aion 5.8 客户端解包；`Dialogs` 下 `QUEST_Q3036.html`（`<name>LF2a_Artifact_Q3036</name>` 由客户端 NPC 数据推导）；真端入仓副本 `src/main/resources/aion/data/static_data/quest/retail/HtmlPages.xml`（`HTML_PAGE_SELECT_QUEST=10`/`htmlpagename=select_quest`、`SHOW_SELECT_QUEST_REWARD_WINDOW1=5`）；客户端 NPC 数据 `npcs_unpacked/client_npcs_npc.xml` 的物件 700398 只有 `quest_ai_name`（无对话 html）；本次未重新采集客户端包哈希。

npc template/object: 接取/领奖 NPC template 798155（Atropos，obj 68055）；圣物物件 template 700398（obj 27668，`ai=quest_use_item`、`tribe=FIELD_OBJECT_LIGHT`）；充能发动石 item 182208026。

map/instance: Theobomos（Ragdoth 海岸一带）；实际 world/instance ID、object 出生点 not captured。

steps:

1. 第一轮（23:23，修复前）：Atropos 接取（`页10 → 31 → 4762 → 1007 → 4 → 1002 → 状态=3 + 1003`）→ 在圣物上领奖；客户端弹 `load fail! LF2a_Artifact_Q3036.html (HtmlPageId 10) (QuestId 0)`。
2. 第二轮（23:49，第一层修复后）：接取（`状态=3 步数=0`）→ 在圣物上使用充能发动石 → `状态=4 步数=1`，无任何对话窗、无 load fail；但任务说明步骤整块空白（描述与奖励仍在）。
3. 第三轮（2026-10-08，两层修复后）：用户实机验证成功（复测口径：旧档进世界自愈、物件推进零回页、Atropos 行选报告页/奖励窗/领取收尾一致）。
4. 登出/登录、重连、死亡、重复领取：not captured。

source state/status/vars: `unaccepted/NONE/var0=0` → 接取 `started/START/var0=0` → 圣物使用 `reward/REWARD/var0=0`（旧档 `var0=1` 由 `REWARD/1 -> 0` 自愈边在 enter-world 回滚）→ 领取 `complete/COMPLETE`。

action/page/button: 物件侧：`USE_OBJECT(-1)` 推进（零发页，after-commit 仅 `sync-quest-state LEVEL_AND_VISIBILITY_REFRESH`）；Atropos 侧：行选 `QUEST_SELECT(31)` → `DEFAULT_SUCCESS(10002)`（客户端自回 1009）→ `SHOW_SELECT_QUEST_REWARD_WINDOW1(5)` → 领取 `SELECTED_QUEST_REWARD1..NOREWARD(8..23)` → 完成 + `SELECT_QUEST(10)` 收尾（questId=0，Atropos 自身 html 声明 `select_quest` 可加载）。

expected response: 物件交互只推进状态、不发任何页（物件在客户端无对话 html，任何 `questId=0` 页都会 load fail）；领奖态 `var0` 停在 legacy 落盘值 0（2 行任务书的行槽位为 `3×行号`，`REWARD/1` 越界会让两条 `<p visible>` 全不亮）；领奖 owner 唯一 = 任务书末行点名的 Atropos。

actual response: 用户确认“实机验证成功”；两轮缺陷的 trace 与截图见本任务证据文档（`.agents/summary/quest-3036-artifact-report-owner/README.zh-CN.md`），第三轮未捕获独立 packet/日志附件。

startup health: not captured（服务端由用户重启管理）；未报告 typed quest engine 初始化失败或 `QuestCompilationException`；离线门禁 `PRODUCTION_COMPILE_OK=707 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。2026-10-08 在并发 14047 提交推进 HEAD 后全部门禁复跑通过（3036 5/5、领奖行合同 4/4、投影回归 171/171、目录 10/10、交互物件 2/2、对话迁移 4/4；`RewardOwnerTrimContractTest` 6/7 唯一红为既存项）。

runtime logs: log/quests.log 2026-10-07 23:23:34-52（第一轮，含 load fail 前最后一帧）与 23:49:49-23:50:09（第二轮）已摘录入证据文档。

protocol trace: 第一/二轮 C→S/S→C 逐帧（见证据文档 §2）；第三轮 not captured。

screenshots/recordings and SHA-256: 用户提供的两张客户端截图（load fail 弹窗、任务说明步骤空白）为对话内临时缓存，未形成稳定仓库附件。

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `REWARD_OWNER_MUST_BE_JOURNAL_REWARD_ROW_NPC`（QE-052，物件兼任领奖 owner → 收尾页打到无对话物件）＋ `CLIENT_SCRIPTED_JOURNAL_ROW`（QE-056，领奖态 var0 越出行槽位 → 任务书步骤整块空白，与 1123/1361/11006 同型）。代表提交 `7a7d27809`（误抬投影的批次）与 QE-054 收口批次 `cec4ef885`/`dc8e59e34`/`f59d5ca42`；代表测试 `RewardOwnerTrimContractTest`（owner 唯一）、`Quest11006ClientDialogAlignmentTest`/`Quest1361ClientDialogAlignmentTest`（领奖态步号轴）；本次测试 `Quest3036ClientDialogAlignmentTest`（5 例）。

similar issue audit: 物件 owner 残留候选 11 个（`ai=quest_use_item` 且带 `npc-complete`，2026-10-07 全库扫描）：18808(730534)、21105(700812)、2232(700061)、2237(700145)、2307(700247)、2664(700324)、28302(730375)、28303(700980)、28808(730534)、4004(700340)、4012(700342) —— 逐件取证，**不得批量改**。交叉线索：`.agents/summary/quest-acceptance/28510-2026-09-10-client-accepted.md` 的同类扫描已把 `3036: 700398` 列为 load-fail 优先复核候选（物件被错误登记 `NPC_START` 且客户端无 `SELECT1(1011)`），本轮一并收口。审计工具盲区：本机 `audit_reward_row_vs_client_steps.py` 的 `UNPACK_ROOT` 解析到空目录，全库会误报 `NO_CLIENT_HTML`（3036 因此被漏审），建议本地修正解析后全量复跑。
