# Phase 5-1：SimpleHunt 表驱动切片（去 XML 路线第一刀）

- 日期：2026-09-22；性质：新增**真端表驱动**数据/语义层（未接入运行时、未提交）
- 目标：让任务由真端文件驱动；XML 只在真端表解决不了时降级

## 1. 交付物

| 产物 | 说明 |
|---|---|
| `src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleHunt.xml` | 真端模板表（UTF-16→UTF-8，656KB，1,865 行）入仓 |
| `questEngine.retail.RetailSimpleHuntTable` | 表加载器：`countN/monsterN` + `acquired/reward_npc_name` + `con_quest`；跳过 6 行无计数器行（1859 行可驱动） |
| `questEngine.retail.RetailHuntCounterLayout` | ScriptDLL64 `FUN_180cb13b0` 语义：6 位/槽位打包、`counterValue`、`canIncrement`（`< countN`）、`increment`（`+= 1<<6(N-1)`）、`goal`（`Σ countN<<shift`）、`isComplete` |
| `questEngine.retail.RetailNpcNameIndex` | `name_desc` → npc_id 索引（大小写不敏感，87,719 名），把真端的 spawn 名桥接到运行时 id |
| `questEngine.retail.RetailSimpleHuntPlan` | 表行 + 名索引绑定成执行计划：`slotForNpc` / `canCount` / `count` / `isComplete` / `npcIdsBySlot` |

## 2. 验证（Maven 已授权）

```
RetailSimpleHuntTableTest                3/3 通过
RetailSimpleHuntPlanEquivalenceTest      1/1 通过   ← 813 条 counter-grid 合同全部由真端表复现
QuestSimpleHuntRetailContractTest        1/1 通过   ← XML 侧合同门禁
ProductionCatalogWhitelistVerificationTest           PRODUCTION_COMPILE_OK=6191 FAILURES=0 VIOLATIONS=0
QuestDefinitionCatalogManifestTest / QuestClientContractGateTest   通过
```

语义回放（与 Phase 2 反编译校验一致）：

| 任务 | 真端表 | 表驱动算出的完成值 | DLL 注册点 |
|---|---|---|---|
| 1102 | `count1=3` | `3` | `(slot1,3)` param_5=3 |
| 1517 | `count1=4, count2=6` | `0x184 = 4 \| 6<<6` | `(slot2,6)` `(slot1,4)` param_5=0x184 |
| 1365 | `count1=5, count2=5` | `0x145 = 5 \| 5<<6` | `(slot1,5)` `(slot2,5)` param_5=0x145 |

**等价结论**：本仓库现有 SimpleHunt XML 的 813 条击杀计数合同，真端表 + 名索引可 1:1 复现 —— 即这 813 条已具备"删 XML 走真端表"的数据前提。

## 3. 边界（诚实说明）

- **尚未接入运行时**：击杀事件 → 进度写入 → 客户端摘要 `([%n]/N)` → 报告/奖励 仍走现有 XML 定义编译路径。
- 名索引目前从 NPC 模板文件构建（测试内）；运行时接线的正确做法是从 `NpcData` 构建同样索引。
- 未纳入本次等价断言的 25 条：`PENDING_MISMATCH`(34)/`CHAIN_COUNT_MISMATCH`(32) 等已知偏差与链式建模任务。

## 4. Phase 5-2 草案（运行时接线）

1. `RetailQuestCatalog`：按 quest_id 汇总多张真端表（先 SimpleHunt，再 SimpleTalk/CombineTask/DataDriven + `quest.xml` 元数据）。
2. 运行时接口：`RetailQuestDriver`（`onKill/onTalk/onEvent` → 进度/状态码），SimpleHunt 实现用本切片的 Plan/CounterLayout。
3. 降级链（必须显式）：`真端表命中 → driver`；未命中 → 现有 XML（默认行为不变，避免回归）。
4. 客户端契约：SECTION 值 → `[%3n]/[%3n+1]/[%3n+2]`（Phase 2 §2.4 已确认），SimpleHunt 只需推 SECTION_0..N 与报告位 SECTION_5。
5. 验收：以 1102/1517/1365 做"driver 结果 == XML 定义"逐状态对拍，通过后再按族删 XML。
