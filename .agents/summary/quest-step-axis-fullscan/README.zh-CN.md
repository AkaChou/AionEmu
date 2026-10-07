# QE-054 全库步号轴批次：40 任务领奖投影回归真端/legacy 权威值（2026-10-07）

- 状态：修复完成；**14046 客户端实机验收通过**（用户 2026-10-07 确认「修复成功」），其余 39 任务实机 `PENDING_CLIENT`。
- 关联记忆库：QE-054 `LEGACY_REWARD_STEP_IS_AUTHORITATIVE`；Playbook `STEP_AXIS_RETAIL_SETPROGRESS_AUTHORITY`（8.54 建立口径、8.55 全族审计方法，本批次为全库推广）。
- 门禁：`RewardRowProjectionRegressionTest`（40 任务 × 3 断言 = 120 例）。
- 修复提交：本批次 `fix(quest)` 提交（`git log -- .agents/summary/quest-step-axis-fullscan/`）；验收记录见 `.agents/summary/quest-acceptance/14046-2026-10-07-client-accepted.md`。

## 1. 触发与现象

14046（Piecing the Memory / 记忆的碎片2，ELYOS 45+ MISSION）与 203754（Aithra）交接（`HACTION_SET_SUCCEED`）进入 REWARD 后，客户端任务书（J）「任务说明」的 8 行步骤**整块空白**，只剩「指令」行与奖励区（2026-10-07 实机报障，截图与 trace 见验收记录）。

```
22:04:47 CM_DIALOG_SELECT npcId=203754 动作=10255        (HACTION_SET_SUCCEED)
22:04:47 SM_QUEST_ACTION 任务=14046 状态=4 步数=7
```

根因链：

1. `步数=7` 是领奖行批次（QE-051 的 `7a7d27809` 等）按「客户端 quest_summary 末行索引」抬升的**推断值**；
2. 客户端在 **REWARD 态**渲染任务书时按自身 `[%N]` 行门槛把显示位再进一位（同型先例：1926/2938，`.agents/summary/quest-1926-reward-dialog/`；本批次 1319 为正向自洽实证），8 行任务下发 7 → 第 9 行不存在 → 全部行门槛不成立 → 步骤区全灭；
3. 真端 14046 的 SET_SUCCEED 处理器带 `step==6` 守卫（`FUN_180caf3c0(0x36de, …, 6)`）且**不推进进度**（保持 6）；全脚本 `SetProgress` 常量集合只有 {3,5}（另有数据驱动 4），**从不存在 7**；legacy `defaultCloseDialog(6,6,true)` 同样落盘 6。

## 2. 判据（三源）

- 真端 `SetProgress(questId, 值)` / `0x110` 显式轴推进 / 通用对话口（动作 `100NN` → 进度 `NN-9999`，要求严格 +1，见 `FUN_180caf150`）；
- 真端 `0x100` 状态推进**不写轴**（进 REWARD 保持玩法末值）；
- legacy handler 落盘值（`changeQuestStep(old,new,true)` / `useQuestItem(old,new,true)` / `defaultCloseDialog(...)` / `defaultOnKillEvent(...)` 的 reward 分支都不写 nextStep，落盘 old）；
- 全量扫描 `audit_all_step_axis.py`：733 个 XML 的全部带事件 transition 目标节点 var0 vs 真端常量集合；`triage_mismatch.py` 按玩法事件/分型编码分诊；`reward-contracts-crosscheck.tsv` 做 reward 契约交叉核对。

## 3. 修复清单（40，`批次值 → 权威值`）

`1626 (7→6) 1636 (4→3) 2122 (2→1) 2208 (2→1) 2284 (3→2) 2333 (3→2) 2436 (2→1) 2620 (2→1)
3056 (2→1) 1319 (8→7) 1900 (4→3) 3082 (3→2) 3200 (4→3) 3721 (3→2) 4038 (3→2) 4502 (3→2)
4721 (3→2) 4939 (5→4) 4943 (4→3) 11116 (3→2) 11076 (4→3) 14046 (7→6) 14051 (4→3) 14153 (6→5)
10530 (9→8) 20530 (9→8) 21114 (5→4) 24022 (8→7) 24023 (4→3) 24024 (5→4) 24025 (4→3) 24030 (9→8)
24046 (7→6) 24051 (6→5) 30111 (2→1) 30227 (3→2) 30327 (3→2) 30217 (3→2) 30317 (3→2) 1921 (4→3)`

逐任务证据（legacy handler 代码摘录 / 真端函数摘录 / 零售集合）见 `risky-legacy-evidence.txt`、`risky-retail-evidence.txt`、`reward-contracts-crosscheck.tsv`（RISKY 25 行 + 逐任务复核追加 15 行）。

## 4. 修复形态与自愈边

- 34 个任务：单条批次回滚边（无 source `enter-world`：`REWARD && var0==批次值 → set 权威值`，after-commit `LEVEL_AND_VISIBILITY_REFRESH`）；
- 6 个任务（1319/1900/4038/4939/4943/11116，`legacyZeroProjection` 组）：两条无 source 边并存——迁移期旧档直修边（`var0==0 → 权威值`）+ 批次坏档回滚边（`var0==批次值 → 权威值`）；
- `var0` 位段 `max` 保留批次坏值上界（坏档需可 pack，QE-047 教训）；
- 门禁断言：投影=权威值、回滚边条件/动作/after-commit、planner 回滚计划（`QuestMutationPlanner`），legacyZero 组额外断言 0 值直修边存在/不存在。

## 5. 验证记录

- IDEA MCP 定向 8 类全绿（2026-10-07）：
  `RewardRowProjectionRegressionTest` 120 例、`JournalRewardRowRepairContractTest` 4、`ShadowCourtRowLadderContractTest` 6、`Quest14051ClientDialogAlignmentTest` 1、`Quest21114PoisonedFungiRetailFlowTest` 4、`SequentialItemCheckQuestFamilyTest` 2、`QuestDefinitionCatalogManifestTest` 10、`ProductionCatalogWhitelistVerificationTest` 1（`PRODUCTION_COMPILE_OK=707 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`）。
- 14046 客户端实机（用户确认）；其余 39 任务实机 `PENDING_CLIENT`。
- 全量扫描对照：`fullscan-step-axis.tsv`（修复后，本目录）vs `fullscan-step-axis.pre-fix.tsv`（修复前快照）；40 任务的被抬值（批次值）从事件目标明细中消失（例：14046 `dialog:7x2 + enter-world:7` → `enter-world:6`；21114/11076 转全 MATCH）。

## 6. 已知边界与残留

- 扫描的「真端集合」只含硬编码 `SetProgress`/`0x110` 常量，**不含通用对话口推进值与数据驱动值**，因此修复后仍标 MISMATCH 的多为合法中间行值（例：14046 的 6、1319 的 8——1319 的 s8 是 9 行任务书中的领奖行 START 态，`reward=7` 在 REWARD 态显示行 8，与「+1 模型」完全自洽），需按第 2 节口径人工判读；
- 已登记例外族不属本批：QE-045 锁（15300/25300）、计数族（var0 承载击杀/收集/分支标记）、`NO_RETAIL_SETPROGRESS` 406 个（无 SetProgress 数据）、itemUseArea 已收口族（`a279fe6da` / `dc8e59e34`）；
- 剩余 MISMATCH 的逐族排查（triage `TRUE_CANDIDATE` 按 legacy/真端复核）为后续批次；
- 其余 39 任务未实机复验；死亡/重登/放弃等旁路未逐条验证。

## 7. 复现命令

```bash
python3 .agents/summary/quest-step-axis-fullscan/audit_all_step_axis.py    # 全量扫描（当前工作区 XML × 真端源码）
python3 .agents/summary/quest-step-axis-fullscan/triage_mismatch.py       # MISMATCH 分诊（玩法事件 vs 分型编码）
python3 .agents/summary/quest-step-axis-fullscan/apply_reward_axis_fix.py --dry  # 批次修复脚本幂等检查（已修复后全 SKIP）
```
