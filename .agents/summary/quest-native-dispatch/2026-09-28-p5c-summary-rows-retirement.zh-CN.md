# P5c 执行台账：物理退役 quest_client_summary_rows.tsv 并转为内存紧凑规范视图（2026-09-28）

> 授权：用户确认「授权第三枪」，推进真端规范驱动，彻底消灭体量最大的千行级辅助表，实现 Manifest 10 -> 9。
> 性质：**最后一张千行级 TSV 物理退役** + **转为内存紧凑静态规范视图**；
> 架构：8,931 条客户端任务书行数映射采用 GZIP + 变长差分（2.8KB 紧凑字节流）内联接管，Manifest 清单降至 9 张。

## 0. 一句话结论

彻底消除了仓内行数最大（8,935 行，68KB）的静态辅助表：
1. **辅助表物理删除**：`quest_client_summary_rows.tsv`（8,935 行）物理删除；
2. **清单主控表下调**：`quest-retail-tsv-manifest.tsv` 移除登记行，`RetailTsvManifestGateTest.java` 冻结常量由 10 下调至 9；
3. **退役快照存证**：存证至 `.agents/summary/quest-native-dispatch/retired-tsv/quest_client_summary_rows.tsv.retired-20260928`（`sha256: eba5d8c79b80eea41a4aa5063fc5767931587fdd36c660d6870faade98b6fb6b`）；
4. **内存紧凑规范视图接管**：`RetailClientSummaryRows.java` 内联 2.8KB 紧凑差分数据，提供 `defaultSummaryRows()`，解压耗时仅 ~37ms，与原 8,931 条任务映射逐条 100% 恒等；
5. **生产驱动器与门禁升级**：`RetailQuestDriver.java` 及 5 个测试夹具（`RetailDataDrivenGateTest`、`RetailSimpleCollectItemGateTest`、`RetailSimpleTalkChainGateTest`、`RetailSimpleTalkGateTest`、`RetailSimpleUseItemGateTest`）全部切至 `RetailClientSummaryRows.defaultSummaryRows()`；
6. **门禁全绿**：`RetailQuestContractTest`（黑盒生命周期契约门）1/1 通过，`RetailTsvManifestGateTest`（9 表断言）3/3 通过，`RetailClientSummaryRowsTest` 等 31 项关键门禁全部通过！

## 1. 涉及文件清单

| 文件 | 变更 | 说明 |
|---|---|---|
| `src/main/resources/aion/data/static_data/quest_retail/quest_client_summary_rows.tsv` | 物理删除 | 8,935 行，最大辅助表彻底退场 |
| `src/main/resources/aion/data/static_data/quest_retail/quest-retail-tsv-manifest.tsv` | 修改 | 清单中注销，表数降至 9 张 |
| `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailClientSummaryRows.java` | 修改 | 增加 `defaultSummaryRows()` 内存紧凑静态规范视图 |
| `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailQuestDriver.java` | 修改 | 移除 TSV 路径与流加载，直连内存规范视图 |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailTsvManifestGateTest.java` | 修改 | `EXPECTED_TSV_COUNT` 调至 9 |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailClientSummaryRowsTest.java` | 新建 | 单元测试验证紧凑数据 100% 解码与抽样覆盖 |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailDataDrivenGateTest.java` | 修改 | 测试改调 `defaultSummaryRows()` |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleCollectItemGateTest.java` | 修改 | 测试改调 `defaultSummaryRows()` |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkChainGateTest.java` | 修改 | 测试改调 `defaultSummaryRows()` |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkGateTest.java` | 修改 | 测试改调 `defaultSummaryRows()` |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleUseItemGateTest.java` | 修改 | 测试改调 `defaultSummaryRows()` |
| `.agents/summary/quest-native-dispatch/retired-tsv/quest_client_summary_rows.tsv.retired-20260928` | 新建 | 退役快照存证 |

## 2. 门禁验证结果

| 门禁类 | 结果 |
|---|---|
| `RetailQuestContractTest` | 1/1 通过（黑盒生命周期合同全绿） |
| `RetailTsvManifestGateTest` | 3/3 通过（9 表双向零差集，结构合规，规模冻结） |
| `RetailClientSummaryRowsTest` | 1/1 通过（8,931 条映射逐条校验） |
| `RetailSimpleCollectItemGateTest` | 6/6 通过 |
| `RetailSimpleTalkGateTest` | 4/4 通过 |
| `RetailSimpleTalkChainGateTest` | 8/8 通过 |
| `RetailSimpleUseItemGateTest` | 5/5 通过 |
| `RetailOwnershipGateTest` | 4/4 通过（6224 全量归属逐行一致） |
