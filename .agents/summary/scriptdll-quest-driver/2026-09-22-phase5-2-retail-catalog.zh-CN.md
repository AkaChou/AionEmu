# Phase 5-2：真端优先 / XML 降级 判定层

- 日期：2026-09-22；性质：运行时接入前置（未接入 dispatcher，未提交）
- 前一提交：`452a2a404`（Phase 5-1 切片 + 合同门禁 + 38 个 XML 对齐）

## 1. 架构判断（重要）

现有运行时是**编译后定义 IR + 事件索引**驱动：
`QuestDefinitionXmlCompiler → CompiledQuestDefinition → QuestEventIndex(Route) → QuestProductionDispatcher.dispatch`，
进度存放于 `QuestState`/`QuestVars`。

因此"去掉 XML"的正确含义是**换定义来源**，而不是写第二套运行时：

```
定义来源优先级（迁移期）：
  1) 真端模板表 → 表→IR 编译器 → CompiledQuestDefinition ┐
  2) 真端表无法表达 → 现有 quest-definition XML ──────────┴→ 同一个 QuestEventIndex / Dispatcher
```

好处：编译器校验、事件索引确定性、进度存储、客户端契约全部复用，风险最低。

## 2. 本切片交付

| 产物 | 说明 |
|---|---|
| `questEngine.retail.RetailQuestCatalog` | 三态来源判定 `RETAIL_TABLE / XML_FALLBACK / UNKNOWN`，并对外提供 `simpleHuntPlan(questId)` 与表驱动击杀推进 `onKill(questId, npcId, packed)` |

判定语义：
- `hasSimpleHunt(questId)` → 真端模板表可驱动（运行时不需要 XML）
- 否则若本仓库有 XML → `XML_FALLBACK`（DataDriven / 脚本 / 未覆盖任务）
- 两边都没有 → `UNKNOWN`

## 3. 验证

```
RetailQuestCatalogTest   2/2 通过
  ├ decidesRetailTableVersusXmlFallback：1517→RETAIL_TABLE、10501→XML_FALLBACK、999999→UNKNOWN
  └ advancesKillCountersStraightFromTheRetailTable：
      1102 连续 3 杀推进 Section_0 到 countN 后停止；
      1517 槽位 2 步长 1<<6、槽位 1 步长 1，混打互不干扰
```

覆盖口径：真端 SimpleHunt 表对本仓库 **942** 个任务可用（其余降级 XML），
其中 **813** 条计数器合同已在 Phase 5-1 证明与现有 XML 等价。

## 4. Phase 5-3 计划（接入 dispatcher）

1. `RetailSimpleHuntIrCompiler`：表行 + 名索引 → `QuestDefinition`（progress 6 位字段 + 击杀计数转移 + 报告/奖励钩子）。
2. 接线：`QuestDefinitionCatalogManifest` 增加"表优先"分支，命中即用编译结果替换同 id 的 XML 定义；
   XML 定义保留为 fallback（默认行为不变，先只对白名单任务开启）。
3. 对拍验收：1102/1517/1365 在 dispatcher 级跑「击杀序列 → 进度 → 状态 → 客户端 SECTION」，
   与 XML 定义逐事件一致后，再扩大白名单并逐族删除 XML。
4. 元数据（等级/奖励/前置）下一步接入真端 `quest.xml`，与前置门禁合流。

## 5. 边界

- 未修改 `QuestProductionDispatcher` / `QuestEventIndex`，未接入运行时；未重启服务。
- 本切片代码（`RetailQuestCatalog` + 测试）尚未提交，待 Phase 5-3 一起成批提交。
