# SECTION_0 报告行闭环系统性审计（同类问题全量排查）

- 日期：2026-09-19
- 触发：15001 修复并通过客户端验收后，按 Playbook「系统性优先于孤立修复」规则排查同类问题。
- 结论一句话：**同类问题仍大量存在**——production catalog 中至少 250 个可执行击杀任务在旧 handler 中有 `setQuestVarById(0, …)`（报告行索引）写入，但当前 XML 让 `var0` 停在计数阶段；另有 33 行需要逐个复核。

## 1. 判定模型（来自当前引擎语义，非猜测）

`QuestMutationPlanner.build` 的求值顺序（`src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlanner.java`）：

1. 先应用 transition 的 actions（`set-variable` / `increment-variable`），被触及字段记入 `actionTouchedFields`；
2. 再用目标节点投影补足**未被动作触及**的字段（`projection.variables()` 去掉 touched 后 `putAll`）；
3. 因此进入 `reward` 后的 `var0` = 显式 set/increment 值，否则 reward 投影值，否则保持进入前的值。

客户端合同（Aion 5.8 客户端解包数据）：

- `Quest_unpacked/quest_monster.csv`：`Progress(SECTION_0==S; SECTION_n<N)` 表示任务说明行 S 上并行门控 `SECTION_n` 计数；
- `data_unpacked/Dialogs/.../quest_q*.html` 的 `quest_summary`：每行一个 `<step>`，行可见条件由 `[%k]` 占位表达；
- 报告行索引由 `SECTION_0` 承载（实机证据：10032 `//quest set 10032 START 65` 行切换；15001 修复后客户端确认）。

因此「最后一击杀满 → 进入 REWARD」时，`SECTION_0` 必须等于报告行 `S+1`，否则服务端状态是 REWARD、客户端任务说明仍停在击杀行。

## 2. 复现命令与产物

```bash
python3 .agents/summary/quest-15001-multicounter-step/audit_section0_report_row_closure.py \
  --client-quest-csv <Aion 5.8 客户端>/Quest_unpacked/quest_monster.csv \
  --client-dialog-root <Aion 5.8 客户端>/data_unpacked/Dialogs \
  --legacy-handler-root <origin/history 的 quest/handlers 解包目录> \
  --out-csv .agents/summary/quest-15001-multicounter-step/section0-report-row-closure-candidates.csv
```

本次执行证据（客户端数据为机器本地解包目录，路径不入库）：

- production catalog `EXECUTABLE` = **6189**；
- 旧 handler 基线：`git archive 7e9f0316c^ src/main/java/com/aionemu/gameserver/quest/handlers`（2339 个 Java handler，未创建 worktree）；
- 候选 283 行（quest+stage），分类见下表；
- 逐行结果：`.agents/summary/quest-15001-multicounter-step/section0-report-row-closure-candidates.csv`。

## 3. 分类结果

| 判定 | 行数 | 含义 | 处置 |
|---|---|---|---|
| `SAME_CLASS_CONFIRMED` | 250 | 旧 handler 完成任务时显式写 `setQuestVarById(0, …)`（报告行索引），当前 XML 把 `var0` 留在计数阶段 | 同型 sweep 修复候选 |
| `REVIEW_LEGACY_NO_VAR0` | 26 | 有旧 handler 但未见 `var0` 写入（分段/计时/副本阶段任务居多） | 逐个比对客户端页面与旧 handler 分支 |
| `REVIEW_NO_LEGACY` | 7 | 无旧 handler 可对照（14252/18911/23918/24252/28911 等） | 需要单独客户端证据 |

`SAME_CLASS_CONFIRMED` 代表（完整清单见 CSV）：

- 15041、15050、15062、15074 系（绿雾湿地/龙帝外庭院单计数器）；
- 15101、15201、15202、15326、15409–15445、15471–15475；
- 15500–15537、15560–15579、15620–15640、15693–15709；
- 23759–23770、23841/23844/23845、24153、25002/25010、25201–25203；
- 25409–25445、25471–25475、25500–25579、25620–25640、25693–25709；
- 16986、17526、18994、26986、27526、28994（副本/要塞阶段计数）。

代表证据（旧 handler 权威写入）：

```java
// origin/history: src/main/java/com/aionemu/gameserver/quest/handlers/redemption_landing/_15471Setting_Up_The_Outposts.java
if (qs.getQuestVarById(1) < 1) { qs.setQuestVarById(1, qs.getQuestVarById(1) + 1); updateQuestStatus(env); }
if (qs.getQuestVarById(1) >= 1) { qs.setQuestVarById(0, 1); qs.setStatus(QuestStatus.REWARD); updateQuestStatus(env); }
```

当前 `15471.xml` 的最终击杀路线只有 `<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>`，`reward` 投影 `var0=0` → 终态 `var0=0`，与旧 handler 行为不一致。

## 4. 为什么之前的击杀任务批量排查没有发现

1. **审计口径只覆盖计数，不覆盖阶段索引**：
   - `QuestKillCounterRetailGateTest`（168 任务批次）比对的是 `SECTION_1/2< N` 计数门控与阈值；
   - `QuestMonsterProgressContractAuditTest` 校验字段存在与 `offset/width`；
   - `ClientQuestSectionAlignmentTest` 校验击杀计数必须落在 `6*n` 段；
   - 三者都不检查「最后一击杀满时 `SECTION_0` 是否写成报告行 S+1」。文档结论因此写成“第 N 击设置 `var0=1`”，但真实 XML 只写了计数字段（15041/15471 等），文档与实现存在偏差且无人复核。
2. **批次覆盖面**：94/168/172 批次处理的是单计数器阈值与字段错位；complex-16 批次处理的是 `SECTION_0 != 0` 的多段击杀（如 17510 `SECTION_0==3`）；QE-012 处理的是「阶段/计数交换」；QE-018 只要求“最终事件进入 REWARD”，都没有把“REWARD 时的说明行索引”列为合同字段。
3. **服务端侧自洽**：`SM_QUEST_ACTION` 的 status=REWARD 与计数饱和都正确，packed step（如 20800）只在客户端侧表现为“停在击杀行/空分子”，在没有客户端截图或 HTML/SECTION 对照时无法从服务端日志判定。
4. **排查入口偏差**：历史排查以「计数是否累积/是否进入 REWARD」为第一检查点，未把 `quest_monster.csv` 的 `SECTION_0==S` 与 `reward` 投影 `var0` 联合比对。

## 5. 影响与边界

- 契约层：250 个 `SAME_CLASS_CONFIRMED` 任务在“最后一击 → REWARD”这一条路径上与 15001 完全同型。
- 玩家可见症状的严重度可能不同：取决于任务是否有 `started -> reward` 的对话恢复路线、reward 源节点投影是否与状态一致；但任务说明行都会停在击杀行。
- 本次未修改这 250 个任务：批量改 XML 属于生产行为变更，需要单独授权 + 回归测试 + 生产门禁 + 代表任务客户端验收（与本项目既有批次流程一致）。

## 6. 建议的后续 sweep（待授权）

1. 扩展 `.agents/summary/quest-15001-multicounter-step/apply_multi_counter_step_closure.py`（或新脚本）以 CSV 为输入，对 `SAME_CLASS_CONFIRMED` 逐任务应用 15001 模式：
   reward 投影 `var0=报告行`；`started->started` 击杀自环 `set var0=所在阶段`；`started->reward` priority 0 终击 `set var0=报告行` 且保留 `LEVEL_AND_VISIBILITY_REFRESH`；补 `enter-world`（`status-is REWARD` + `var0==所在阶段`）迁移修复。
2. 新增/扩展回归测试：以 CSV 为契约源，断言每行候选的 reward 投影与终击写入 ≥ 报告行；生产门禁命令：
   `mvn -q -Dtest='QuestMonsterProgressContractAuditTest,ClientQuestSectionAlignmentTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest' test`
3. 先在代表性任务（15041 单计数、15471 周常、15500 守护、24153 多计数、18994 两段）做客户端验收，再全量合闸。
4. `REVIEW_*` 两类先逐个判定，不与 sweep 混提。

## 7. 2026-09-19 sweep 执行结果（本轮）

- 审计脚本修正：`analyze()` 早期只解析 `unaccepted/started/ready/reward/complete` 五个固定节点名，导致 `k1`/`step2` 等自定义节点的投影 `var0` 被漏读，16986/17526 被误报。修正为解析全部 `<node>` 后可执行任务候选从 250 行降为 **248 行**。
- 批量修复：`apply_section0_report_row_sweep.py` 对结构完全同型的 **244** 个任务应用四项合同；18994/28994（多阶段）单独修复为 `step2 -> reward` 与 reward 投影 = 报告行 3。
- 修复后复跑审计：`SAME_CLASS_CONFIRMED` 仅剩 **15101、24153**；`REVIEW_LEGACY_NO_VAR0` 24 行、`REVIEW_NO_LEGACY` 7 行保持不变。
- 残余清单：`section0-report-row-closure-residual.csv`；完整报告见 `2026-09-19-section0-report-row-sweep.zh-CN.md`。
- 注意：旧 handler 证据的正则只匹配 `setQuestVarById(0, <literal>)` 与 `setQuestVar(0, <literal>)`，会漏掉 `setQuestVar(1)`（单参数即 var0）与 `setQuestVarById(0, var + 1)` 形态；`REVIEW_LEGACY_NO_VAR0` 中至少 15060、16974 属此类漏判，需二次分类。
