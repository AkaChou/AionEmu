# P0a §4.4.8：QE-112 在飞状态与基线纪律记录

> P0a 只读审计产物 · 2026-10-01。

## 工作区快照（P0a 开工时）

- 分支 `quest`，HEAD = `71d0b031d`（fix(quest): realign DataDriven adjudication codes with the frozen drift registry）。
- **QE-112（DataDriven 行阶梯 + 4 行翻转 + 指纹重冻）仍未提交**：15 个文件脏（+985/−1296），关键面：
  - `RetailDataDrivenDefinitionCompiler.java`、`RetailSimpleHuntDefinitionCompiler.java`（行阶梯形）
  - `quest_definition_catalog.xml` + 删除 `definitions/quests/{3122,3123,4122,4123}.xml`（4 行翻转）
  - `retail-data-driven-ir-fingerprints.tsv`（1282 行重冻）、drift.tsv、两份 retention TSV
  - 新增探针：`ZzIrDumpProbeTest.java`、`RetailHuntLadderShape.java`；切片台账 `.agents/summary/quest-dd-hunt6-ladder/`
- 基线快照（计划 §4.7）：`RetailDataDrivenGateTest` 红 = QE-112 指纹漂移在飞，非本计划回归。

## 纪律执行

1. 本 P0a 全程**未触碰**上述在飞文件（工具只读；产物全部写 `.agents/summary/quest-engine-native/p0a/`）。
2. P0a 所有数量均为 2026-10-01 脏快照：owner-identity.tsv 基于 `retail-xml-retention.tsv` 的 QE-112 未提交版本（6224 行）；**正式冻结须等 QE-112 落地或用户裁决后重跑 `tools/owner_identity.py`**（幂等、只读）。
3. 相机矩阵/表对拍/can_report/缺失表决策来自 `<真端根>` 侧，与本仓工作区状态无关，可作为稳定证据。
