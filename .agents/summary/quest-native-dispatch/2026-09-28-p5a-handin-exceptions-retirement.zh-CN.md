# 批 P5a 执行台账：quest_client_handin_exceptions.tsv 巨型表整表退役（2026-09-28）

> 授权：用户「确认」推进 417 KB 巨型例外表退役，按真端规范生命周期收缩 TSV 资产面。
> 性质：**TSV 物理整表退役**（Manifest 14 → 13）；
> 架构：彻底清除 417 KB、7,608 行的巨型机械比对例外表，将 48 例稳定拒绝特征收敛为内存静态规范集合。

## 0. 一句话结论

`quest_client_handin_exceptions.tsv`（7,608 行，417 KB）正式整表物理退役：
* **退役前快照存证**：`.agents/summary/quest-native-dispatch/retired-tsv/quest_client_handin_exceptions.tsv.retired-20260928`（sha256: `3d5ec6f2df99669cce2df7451c6328a7241dd7dd4256ca8de15cf9d4ecd461ff`）；
* **生产驱动同轴解耦**：`RetailQuestDriver.java` 移除 `CLIENT_HANDIN_EXCEPTIONS` 常量与文件加载逻辑；
* **内存静态规范集合**：48 例在册稳定拒绝任务（稳定码 `RETAIL_HANDIN_VOCABULARY_UNSUPPORTED`）收敛入 `RetailClientHandinPages.DEFAULT_EXCLUDED`，其余 7,500+ 行死数据彻底退场；
* **测试夹具同步简化**：`RetailDataDrivenGateTest.java` 移除物理 TSV 读取；
* **清单与门禁缩减**：`quest-retail-tsv-manifest.tsv` 移除登记行，`RetailTsvManifestGateTest.EXPECTED_TSV_COUNT` **由 14 降至 13**（3/3 全绿）。

## 1. 退役快照与元数据

| 证据 | 路径 | sha256 |
|---|---|---|
| 退役快照 | `.agents/summary/quest-native-dispatch/retired-tsv/quest_client_handin_exceptions.tsv.retired-20260928` | `3d5ec6f2df99669cce2df7451c6328a7241dd7dd4256ca8de15cf9d4ecd461ff` |
| 历史生成器 | `build_quest_client_handin_pages.py`（停写退役） | - |

## 2. 门禁验证结果

| 门禁类 | 结果 | 耗时 |
|---|---|---|
| `RetailTsvManifestGateTest` | 3/3 通过（13 表强恒等断言） | 44 ms |
| `RetailQuestDriverOverlayTest` | 5/5 通过 | 9.8 s |
| `RetailSimpleUseItemGateTest` | 5/5 通过 | 1.2 s |
| `RetailSimpleCollectItemGateTest` | 6/6 通过 | 1.2 s |
| `RetailSimpleTalkChainGateTest` | 8/8 通过 | 1.3 s |
| `RetailOwnershipGateTest` | 4/4 通过（6224 全量归属一致） | 171 ms |
| `RetailQuestAiNameGroupGateTest` | 5/5 通过 | 1.0 s |
