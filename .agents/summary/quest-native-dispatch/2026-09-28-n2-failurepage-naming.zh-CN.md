# 批 N2 收口台账：FailurePage 命名拆分（零形状 · 2026-09-28）

> 阶段宪章批 N2（`2026-09-28-w6tail-w7-stage-charter.zh-CN.md`）；取证依据
> `2026-09-27-w5c-w6-recon-pack.zh-CN.md` §4-3。

## 处置（三处，零形状）

1. **死代码删除**：`RetailSimpleCollectItemDefinitionCompiler.itemReport(int, List, boolean, String)`
   —— W6-a 退役 shell 编译器后**零调用**（全仓 grep 唯一消费者不存在；活路径 =
   `RetailSimpleTalkDefinitionCompiler.itemReportGate`，QE-059 糖元素通道）。
   其 `hasFailurePage` 布尔（"FailurePage"歧义载体的 Java 侧最后一处）随之消失。
2. **消歧知识迁移**：批 0 R3 的命名消歧 javadoc（SELECT6=2716 ≠ CHECK_USER_ITEM_FAIL=10001）
   移到活路径 `itemReportGate`（`failurePage` TSV 列的消费点）。
3. **名实相反点改名**：`RetailSimpleTalkMigrationReviewContractTest:48` 方法名
   `…AndRetireTheFailurePage` → `…AndRetireTheSelect6TurnInFailurePage`
   （带页号消歧；该测试不在任何红集，改名零红集影响）。

## 边界（不动）

XML `failure-page` 属性与 TSV `failurePage` token 是数据格式（`QuestXmlBlockExpander` /
`RetailClientTalkChainSteps.ItemReportRecord` 消费），按宪章不动。

## 验证

快筛 4 门（MigrationReview / CollectItemGate / SimpleTalkGate / SimpleTalkChainGate）
一次 mvn：**22/22 绿、BUILD SUCCESS**；零 IR、零 retention、零指纹、零红集变化（预期）。
