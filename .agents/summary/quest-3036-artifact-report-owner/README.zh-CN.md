# 任务 3036 领奖收尾 load fail + 任务步骤空白：物件 owner 收敛、物件零回页、领奖态步号轴回归 legacy 值

状态：`CLIENT_ACCEPTED`（2026-10-08 用户实机验证成功；验收记录见 [3036-2026-10-08-client-accepted.md](../quest-acceptance/3036-2026-10-08-client-accepted.md)）
日期：2026-10-07（第三轮实机验收 2026-10-08）
相关 Pattern：[QE-052](../../memory-bank/patterns/quest-engine.md#qe-052-五十领奖-owner-必须等于客户端任务书领奖行-npc-reward_owner_must_be_journal_reward_row_npc)、[QE-056](../../memory-bank/patterns/quest-engine.md)、QE-054、QE-051、QE-137、QE-140

## 1. 玩家可见症状（两轮实机）

| 轮次 | 症状 | 触发动作 |
|---|---|---|
| 第一轮（23:23） | 领奖后弹 `load fail! LF2a_Artifact_Q3036.html (HtmlPageId 10) (QuestId 0)` | 在圣物上领奖 |
| 第二轮（23:49-23:50，修复①后） | 任务说明里**步骤整块空白**（描述「用充了能的发动石启动拉格托斯海岸的实体，并把结果告诉阿特洛浦斯」与基本奖励仍在，`<steps>` 两条都不亮） | 在圣物上使用充能发动石推进到 REWARD 后 |

第二轮 trace：`23:49:49 SM_QUEST_ACTION 任务=3036 状态=3 步数=0`（接取）→ `23:50:09 状态=4 步数=1`（物件推进到 REWARD；**无任何对话窗**，第一轮的 load fail 已消失）。

## 2. 第一层：领奖收尾页打到无对话物件

证据链：

1. **页 10 = NPC 通用选择页**：真端 `HtmlPages.xml`：`HTML_PAGE_SELECT_QUEST id=10 htmlpagename=select_quest`；`questId=0` 时客户端按**目标对象**的对话 html 解析页号。
2. **物件没有对话 html**：客户端解包 `Dialogs/**` 无 `LF2a_Artifact_Q3036.html`；物件 700398 的客户端数据只有 `<quest_ai_name>LF2a_Artifact_Q3036</quest_ai_name>`（`Field_Object_Light`、`cursor_type=action`、`talk_delay_time=3`）。同目录 LF2a 下 `Atropos.html` 声明 `select_quest`（→ 页 10 在 Atropos 上可加载）。
3. **A/B 对照**：同日志 23:23:33，任务 3035 在 Atropos（68055）上同型收尾（`状态=5` → `questId=0 页=10`）无报错。
4. **客户端任务书领奖行 = Atropos**：`QUEST_Q3036.html` 的 `quest_summary` 两行——行 0「用充了能的发动石启动[圣物]」、行 1「向 [STR_DIC_N_Atropos] 报告」；同文件 `select_none/ask_quest_accept/quest_accept_1/quest_refuse_1/select_success` 全篇文案都是 Atropos 的台词。
5. **legacy 基线**（`git show 7e9f0316c^` 的 `_3036LetSeeWhatItDoes`）：物件只在 START 调 `useQuestObject(env, 0, 1, true, false)`；领奖 `sendQuestEndDialog` 只在 798155；接取 `addOnQuestStart(798155)` 只在 Atropos。
6. **真端脚本**（ScriptDLL64）：`FUN_180f7ed30`（`+0x100(0xbdc)` 状态推进）与 `FUN_180f587a0`（登记 + 处理充能发动石 0xadc461a）零发页；发页的只有 Atropos 侧 `FUN_180fd0620`（页 0x3eb=1003）/`FUN_180fe48c0`（页 0x3ec=1004）。
7. **迁移缺陷**：typed 定义把 `useQuestObject(..., reward=true)` 的「置 REWARD」读成「物件是领奖 NPC」→ 物件带 `npc-complete`，`finish=SELECTION_DIALOG` 的收尾页 10 打在物件上（违反 QE-052 owner 唯一性）。
8. 同型先例：quest 11006（QE-054 交接）——用物步的页 10 尾随属迁移期「翻译夸大」，已删（用物没有对话对象 ⇒ 发页即 load fail）。

## 3. 第二层：领奖态步号轴被批次误抬（QE-054/QE-056）

- 客户端 `quest_summary` 行槽位是 `3×行号`（行 0 = `[%0]/[%1]`，行 1 = `[%3]/[%4]`）。领奖行批次（`7a7d27809`）按「末行索引 = 领奖行」把 3036 的 reward 投影从 0 抬到 **1**，并加了 `REWARD/0 -> 1` 自愈边。
- **legacy 权威值 = 0**：`changeQuestStep(env, step, nextStep, reward, varNum)`（AionEmu 同源 `QuestHandler.java:99`）的 reward 分支只 `qs.setStatus(REWARD)`，**不写 var**；真端 `0x100` 状态推进同样不写轴。客户端在 REWARD 态按状态自行显示报告行。
- 2026-10-07 实机（第二轮）：`状态=4 步数=1` 下 2 行任务书的两个 `<p visible>` **全不亮**（步骤整块空白）——与 1123（QE-056）/1361/11006（QE-054）同型：var0 越出 `quest_summary` 声明的行槽位。
- 处置（照 11006/1361 的批次收口先例）：reward 投影回 **0**、自愈边反转 `REWARD/1 -> 0`（回滚被批次写坏的存档）、从 `JournalRewardRowRepairContractTest` 移出并登记 `RewardRowProjectionRegressionTest` 的 `Row(3036, 1, 0, false)`、审计脚本登记 `LEGACY_STEP_EXCEPTION`。

## 4. 修复面（本轮全部改动）

| 文件 | 内容 |
|---|---|
| `.../quests/3036.xml` | 物件 700398：删 `NPC_START`/`NPC_REPORT`/`npc-complete`，只留零回页行进边（`started→reward`，`TALK_TO_NPC(700398, USE_OBJECT)`，after-commit 仅 `sync-quest-state LEVEL_AND_VISIBILITY_REFRESH`）；删 Atropos 侧可跳步的 `NPC_REPORT started→reward`；新增 `reward + 31 → DEFAULT_SUCCESS(10002)`；`npc-complete(798155)` 保持 8..23 领取 + 收尾页 10；reward 节点投影 `var0=1 → 0`；自愈边 `REWARD/0→1` → `REWARD/1→0` |
| `Quest3036ClientDialogAlignmentTest.java`（新，5 例） | 物件零回页 / 物件永不作页目标 / Atropos 报告页 / 唯一 owner + 收尾页 10 / 投影 0 + 回滚边 |
| `RewardRowProjectionRegressionTest.java` | 追加 `Row(3036, 1, 0, false)`（QE-054 收口口径） |
| `JournalRewardRowRepairContractTest.java` | 移出 `Contract(3036, 1, 0)`，留 11006/1361 同款取证注释 |
| `audit_reward_row_vs_client_steps.py` | `LEGACY_STEP_EXCEPTION` 登记 3036 |
| 记忆库 QE-052/QE-056 + 派生索引 | 物件 owner 机型变体、机制扩展（纯服务端多行任务同受行槽位约束）、3036 证据 |

> 说明：本轮**未**改 `RewardOwnerTrimContractTest`（其契约按「reward = 末行索引」断言，与 QE-054 口径冲突；3036 的 owner 断言由本任务专属测试承担）。

## 5. 门禁（IDEA MCP，2026-10-07 首跑 / 2026-10-08 复跑）

| 门禁 | 结果 |
|---|---|
| `Quest3036ClientDialogAlignmentTest`（新） | **5/5 绿** |
| `JournalRewardRowRepairContractTest` | **4/4 绿** |
| `RewardRowProjectionRegressionTest` | **171 例全绿**（含新 3036 行） |
| `QuestDefinitionCatalogManifestTest` | **10/10 绿** |
| `ProductionCatalogWhitelistVerificationTest` | `PRODUCTION_COMPILE_OK=707 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0` |
| `QuestInteractionObjectContractGateTest` | **2/2 绿** |
| `QuestDialogMigrationGateTest` | **4/4 绿** |
| `RewardOwnerTrimContractTest` | 6/7（唯一红 = **既存红** `henirOwnerKeyResolvesThroughTheDredgionFamily`：先调 `definition(4713)`，而 4713 已在 `2415b2f9c` 退役，与本改动无关） |
| static / 记忆库 | `xmllint + quest_definition.xsd` 通过；`git diff --check` 干净；`MEMORY_BANK_VERIFY_OK` |

复跑（2026-10-08，并发任务 14047 的提交 `463bf2891`/`d10993395` 推进 HEAD 后）：上述各门禁在提交前全部重跑，结果一致——3036 类 **5/5**、领奖行合同 **4/4**、投影回归 **171/171**（含 `Row(3036, 1, 0, false)`）、目录清单 **10/10**、`ProductionCatalogWhitelistVerificationTest` `PRODUCTION_COMPILE_OK=707 / 0`、交互物件门 **2/2**、对话迁移门 **4/4**；`RewardOwnerTrimContractTest` 仍为 6/7（唯一红 = 既存 `henirOwnerKeyResolvesThroughTheDredgionFamily`）。

## 6. 实机复测口径（待用户执行；改 XML 需先重启服务端）

1. **旧档自愈**：现存停在 `REWARD/var0=1` 的角色进世界时应被 `REWARD/1 -> 0` 回滚 → 任务说明恢复显示两行步骤（行 0 发动、行 1 向 Atropos 报告）。
2. 接取 → 在圣物上用充能发动石：只有使用进度条 + `状态=4 步数=0`，**无对话窗、无 load fail**，任务步骤不再空白。
3. 回 Atropos 行选（31）→ `questId=3036 页=10002` →（自回 1009）奖励窗页 5 → 领取（8..23）→ `状态=5` + `targetObj=Atropos questId=0 页=10`（可加载），无 load fail。

## 7. 遗留与候选

- **同族残留候选（物件 owner）11 个**：18808、21105、2232、2237、2307、2664、28302、28303、28808、4004、4012 —— 逐件取证，**不得批量改**（QE-052 boundaries）。
- **既存红**：`RewardOwnerTrimContractTest.henirOwnerKeyResolvesThroughTheDredgionFamily` 建议先取 `resourceTextOrNull` 再走退役断言（本轮未代做）。
- **本机审计盲区**：`audit_reward_row_vs_client_steps.py` 的 `UNPACK_ROOT` 解析为 `<仓库父目录>/PycharmProjects/unpak`（本机上该目录为空），故本机跑审计会全库报 `NO_CLIENT_HTML` —— 这正是 3036 这类行被漏审的原因（QE-051 症状索引亦警示大小写/索引前提）。本机可用符号链接或本地改动指向 `<客户端解包根>` 后再全量复核。
- **道具生命周期**：充能发动石（182208026）现由 planner 在完成时清理（work-items 口径）；legacy/真端疑在「使用」时即消耗（`+0x1d8(0xadc461a)`），本轮未改。
