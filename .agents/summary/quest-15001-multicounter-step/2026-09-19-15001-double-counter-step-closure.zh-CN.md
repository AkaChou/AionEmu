# 任务 15001「Lending Both Hands」双计数未推进 SECTION_0 修复

## 触发与症状

- 玩家报告：任务 15001 完成两组各 5 只击杀后没有进入下一步「和努贝斯对话」；任务窗口的击杀计数出现 `(/5)` 类空分子显示。
- 服务端 `SM_QUEST_ACTION` 追踪：
  - 第一组计数：步数 `64 → 128 → 192 → 256 → 320`（`var1=1..5`，`SECTION_1`）。
  - 第二组计数：步数 `4416 → 8512 → 12608 → 16704 → 20800`（`var2=1..5`，`SECTION_2`，同时保持 `var1=5`）。
  - 最终：`状态=4 步数=20800`。
- `20800` 按 6-bit 位段解码为 `var0=0, var1=5, var2=5`；服务端已进入 `REWARD`，但客户端任务说明行索引读取的 `SECTION_0` 仍为 0，因此客户端仍停在击杀行，没有切到报告行。

## 权威客户端证据

1. `Quest.pak` / `Quest_unpacked/quest_monster.csv`：
   ```csv
   15001,Progress(SECTION_0==0; SECTION_1<5),,simpleQuest,,2,lf5_a_varanus_55_n,lf5_a_varanus_56_n
   15001,Progress(SECTION_0==0; SECTION_2<5),,simpleQuest,,2,lf5_a_shellizard_55_n,lf5_a_shellizard_56_n
   ```
   - `SECTION_0` 是阶段/任务说明行索引：击杀阶段要求 `SECTION_0==0`。
   - `SECTION_1`、`SECTION_2` 分别是两组怪物的独立计数。
2. `data_unpacked/Dialogs/10000_19999/quest_q15001.html` 的 `quest_summary`：
   - 第一组计数占位 `[%5]`、第二组 `[%8]`，报告步骤可见条件为 `[%9]`。
3. 已有实机证据（任务 10032/20032，commit `0823653a7`）：`//quest set 10032 START 65`（`SECTION_0=1`）后客户端任务说明立即切到下一行，证明 5.8 客户端的说明行索引读取 `SECTION_0`。
4. 已有实机证据（任务 19637，19637 修复记录）：击杀计数在 `SECTION_1`，`SECTION_0` 是阶段控制变量；击杀满后必须写 `var0=1` 才会激活第二步。

## 根因

- 服务端 XML 的 6-bit 位段布局本身正确：`var0@0`（阶段）、`var1@6`（第一组计数，max 5）、`var2@12`（第二组计数，max 5）。
- 但 `reward` 节点投影 `var0=0`，两条 `started -> reward` 的最终击杀路线只写 `var1=5` / `var2=5`，从未写 `var0=1`。
- 结果：`REWARD` 状态携带的 packed 值是 `20800`（`SECTION_0=0`），不是客户端报告步骤所需的 `20801`（`SECTION_0=1`）。
- 因此客户端仍按第一阶段渲染任务说明和击杀行；在第二阶段尚未完成时，已完成计数行的空分子显示也是同一陈旧阶段布局的可见表现。

## 同族 sweep

按同一客户端契约（`SECTION_0==0` + `SECTION_1/SECTION_2...` 计数 + 第二步报告行）在 production catalog 中扫描，共 14 个可执行任务缺同一闭环：

- 15001、15020、15073、15100、15104、15203、15406、15407、15408
- 15580、15671、25671、25060、18952

其中 15203 为三段计数，18952 为四段计数，25060 为三段计数。2869 虽然客户端也有相同双计数契约，但生产 XML 只有 metadata、没有 progress/nodes/transitions，属于非可执行占位，不在本次生产修复范围。

## 修改

对上述 14 个任务统一执行：

1. `reward` 节点投影 `var0=1`；计数达到上限的字段保持显式上限投影。
2. 每个 `started -> started` 击杀自环显式 `set var0=0`，自愈旧的脏阶段值。
3. 每个 `started -> reward` 的 priority 0 最终击杀路线显式 `set var0=1`，保留原有最终计数写入，并确保 `after-commit` 为 `LEVEL_AND_VISIBILITY_REFRESH`。
4. 每个任务新增一条无 source 的 `enter-world` 迁移修复路线：
   - 条件：`status-is REWARD` + `variable-is var0==0`；
   - 动作：`set var0=1`；
   - `after-commit`：`LEVEL_AND_VISIBILITY_REFRESH`。
   - 作用：玩家已有 `REWARD/var0=0` 存档在重登/进入世界时自动修复为 `REWARD/var0=1`，避免 reward 源节点投影变化导致无法交任务。
5. 25671、25060 的 reward 节点补齐计数上限投影，与同族任务保持一致。

批处理脚本保留在 `.agents/summary/quest-15001-multicounter-step/apply_multi_counter_step_closure.py`。

## 验证与边界

- 已做静态检查（非构建）：
  - 14 个 XML 均可被 XML parser 解析。
  - `var0@0`、`var1@6`、`var2@12` 等位段布局与客户端 SECTION 对齐。
  - reward 节点 `var0=1`、最终击杀路线 `set var0=1` + `LEVEL_AND_VISIBILITY_REFRESH`、迁移修复路线合同均通过脚本断言。
  - 15001 静态模拟：两组 5 杀后的预期 wire step 为 `1 + 5*64 + 5*4096 = 20801`。
  - IDE 对 14 个 XML 与修改后的 `QuestMonsterProgressContractAuditTest` 报 0 error。
- 已执行（用户授权）：
  - `mvn -q -Dtest='QuestMonsterProgressContractAuditTest,ClientQuestSectionAlignmentTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest' test` → PASS。
  - 输出：`PRODUCTION_COMPILE_OK=6189`、`PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。
  - 测试首次运行暴露了新审计方法对无 source 迁移路由的空指针（`sourceNode().equals`），已改为 `"started".equals(...)` 等 null-safe 写法后通过；该结果也说明 source-less repair 路由必须被审计代码显式排除。
- 客户端验收：**ACCEPTED**。用户于 2026-09-19 回复“客户端验证完成，已修复”，确认 15001 最终击杀后进入报告步骤；完整记录见 `.agents/summary/quest-acceptance/15001-2026-09-19-section0-report-row-client-accepted.md`。
- 提交：`c34458083 fix(quest): advance SECTION_0 to the report row when step-zero kill counters saturate`（本地 commit，未 push）。

## 同类问题排查（2026-09-19）

- 使用 `.agents/summary/quest-15001-multicounter-step/audit_section0_report_row_closure.py` 对 production catalog（6189 个 EXECUTABLE）、Aion 5.8 客户端 SECTION 契约与 `origin/history` 旧 handler 做交叉审计：
  - `SAME_CLASS_CONFIRMED` 250 行：旧 handler 完成任务时写 `setQuestVarById(0, …)`，当前 XML 把 `var0` 停在计数阶段；
  - `REVIEW_LEGACY_NO_VAR0` 26 行、`REVIEW_NO_LEGACY` 7 行：需要逐个复核。
- 结论与建议 sweep 见 `.agents/summary/quest-15001-multicounter-step/2026-09-19-section0-report-row-closure-audit.zh-CN.md`；逐行清单见同目录 `section0-report-row-closure-candidates.csv`。

## 剩余风险

- 14 个同族任务中只有 15001 做了实机验收；其余按同合同静态与门禁覆盖。
- 真端 UI 在「一组计数已满、另一组未满」时如何显示已完成计数行（`5/5` vs `/5`）尚未用修后版本单独实机确认。
- 迁移修复依赖 `enter-world` 事件；已在线且不重登/不切图的旧存档需要一次重登或换图才会触发。
- 250 个同型候选任务尚未修复（等待授权后按批次 sweep + 客户端代表验收）。
