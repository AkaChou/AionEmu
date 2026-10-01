# P0a 修正记录：owner-identity 名字索引缺陷（带归因重冻）

> 2026-10-01。P0a 工具修正（非数据变更）；按计划 D.2「无归因的重冻视为漂移」，本文件即归因记录。
> `owner-identity.tsv` 已用修正后工具重跑（工具 `tools/owner_identity.py`，幂等只读）。

## 1. 缺陷

初版 `parse_npc_names()` 只索引 npcTemplates 的 **`name` 属性**；模板同时携带 **`name_desc` 属性 = 真端式全名**，而真端任务表引用的正是 `name_desc` 形态的名字：

```xml
<npc_template name_desc="DF5_Soglo_E" ... name="Soglo" npc_id="804915" ...>
```

真端表 `Quest_SimpleHunt.xml` 25050 行的 `value0_acquire_ = DF5_Soglo_E` ⇒ 初版误判缺失。

## 2. 修正后数字（覆盖初版报告的对应数字）

| 指标 | 初版（name-only 索引） | 修正（name ∪ name_desc） |
|---|---:|---:|
| 名字索引规模 | 57,832 | 116,225 |
| 缺失名字引用（行加权） | 13,174 | **877**（234 个唯一名）|
| NATIVE_READY（含哨兵） | 3,527 | **7,542 / 8,562（88.0%）** |
| NATIVE_NAME_MISSING 行 | 4,623 | **518** |
| NATIVE_NAME_AMBIGUOUS 行 | 75 | **165** |
| CLIENT_EVIDENCE_MISSING（DD） | 337 | 337（不变） |
| NO_TABLE_XML_ONLY | 670 | 670（不变） |

按家族 READY（含哨兵）/行数：SimpleTalk 2939/3152、SimpleHunt **1691/1863**、SimpleSerialHunt **16/16**、CombineTask 574/574、SimpleCollectItem 238/262、SimpleUseItem 156/160、SimpleItemPlay 38/43、DataDriven 1895/2492。

## 3. 残差与去向

- **518 缺失行 + 165 歧义行**：唯一名 234 个，其中 146 个（623 引用）在本服静态数据**完全不存在**（集中于 LF4/DF4/DF6/ldf5a/ldf5b 高地区域与赏金 NPC——本服静态数据构建范围之外）；其余为跨模板同名（歧义）。
- 规范化类桥接（子串/词元超集）在残差上覆盖率 ≤3% 且多为 `test_` 变体 ⇒ **规范化路线正式否决**（详见 `../p1-prereqs/name-resolution-decision.md`）。
- P1 决策输入与残差处置方案见 `../p1-prereqs/name-resolution-decision.md`；本修正不改变 P0b 任何产物。
