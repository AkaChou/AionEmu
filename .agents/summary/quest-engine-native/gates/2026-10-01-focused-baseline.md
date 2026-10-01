# 2026-10-01 聚焦 Maven 基线

- 工作区：含未提交的 QE-112（DataDriven 行阶梯 + 4 行翻转 + 指纹重冻未完成）
- 说明：本文件是迁移计划 §4.6 的聚焦基线证据；不是长期 gate 输入。

## 命令与结果

| 命令 | 结果 |
|---|---|
| `mvn -q -Dtest=QuestVarsTest -DfailIfNoTests=false test` | PASS |
| `mvn -q -Dtest=RetailSimpleHuntFamilyGateTest -DfailIfNoTests=false test` | PASS |
| `mvn -q -Dtest=RetailDataDrivenGateTest -DfailIfNoTests=false test` | FAIL：8 例 1 失败 |

## 失败详情

- 测试：`RetailDataDrivenGateTest.frozenFingerprintsCoverExactlyTheRetiredQuests`
- 现象：20 个 quest 的冻结指纹与当前 IR 不一致（指纹漂移）
- quest：2869, 13955, 15001, 15020, 15073, 15100, 15104, 15203, 15406, 15407, 15408, 15546, 15580, 15671, 16802, 16806, 17541, 18952, 23955, 25060
- 归因：QE-112 在飞未提交状态的指纹重冻未完成，不是 P0/P1 新回归。

## 处置

- 标记 `KNOWN_RED / IN_FLIGHT`。
- 不追单个 quest id，不在本计划步骤里修。
- QE-112 落地前，涉及这些行的迁移排除或等待。
