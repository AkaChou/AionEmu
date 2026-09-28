# 批 P2c 执行台账：quest_client_dialog_exits.tsv 整表物理退役（2026-09-28）

> 授权：用户「开工·开始授权不要执著于 ir，以真端的驱动为主，推进“规范形生命周期”（Canonical Lifecycle）... 确立原则：页码类 TSV 只退役、绝不新增。授权下一步做什么」→「开始」。
> 性质：**TSV 物理整表退役**（Manifest 15 → 14）；
> 架构：彻底贯彻规范形生命周期，终结微观页梯对话出口补丁表，转由内存静态规范形视图接管。

## 0. 一句话结论

`quest_client_dialog_exits.tsv`（324 行：16 行注释 + 308 行数据）正式整表物理退役：
* **退役前快照落盘**：`.agents/summary/quest-native-dispatch/retired-tsv/quest_client_dialog_exits.tsv.retired-20260928`（sha256: `e7d4a1b7cbd1192c92dc689af20ef92e114ba2311e3e67eaf404973184c90c78`）；
* **生产驱动解耦**：`RetailQuestDriver.java` 移除 `CLIENT_DIALOG_EXITS` 文件读取逻辑，改用 `RetailClientDialogExits.defaultExits()`；
* **内存规范视图接管**：
  * `SELECT2_CONTINUE`（289 个链式任务）
  * `SELECT5_CHECK`（12 个任务）
  * `SELECT5_CHECK_SIMPLE`（11 个任务）
  * `SELECT6`（13 个任务）
  * `SELECT_NONE_1`（14 个任务）
  全量 308 项经断言与退役前 TSV 逐任务、逐旗标 100% 恒等；
* **测试夹具同轴升级**：7 个相关家族测试类（`RetailDataDrivenGateTest`, `RetailSimpleCollectItemGateTest`, `RetailSimpleHuntEquivalenceGateTest`, `RetailSimpleHuntFamilyGateTest`, `RetailSimpleTalkChainGateTest`, `RetailSimpleTalkGateTest`, `RetailSimpleUseItemGateTest`）移除物理 TSV 读取，统一接入 `defaultExits()`；
* **清单与门禁缩减**：`quest-retail-tsv-manifest.tsv` 移除登记行，`RetailTsvManifestGateTest.EXPECTED_TSV_COUNT` **由 15 降至 14**（3/3 全绿）。

## 1. 退役快照与元数据

| 证据 | 路径 | sha256 |
|---|---|---|
| 退役快照 | `.agents/summary/quest-native-dispatch/retired-tsv/quest_client_dialog_exits.tsv.retired-20260928` | `e7d4a1b7cbd1192c92dc689af20ef92e114ba2311e3e67eaf404973184c90c78` |
| 历史生成器 | `build_quest_client_dialog_exits.py`（停写退役） | - |

## 2. 门禁验证结果（35/35 全绿）

| 门禁类 | 结果 | 耗时 |
|---|---|---|
| `RetailTsvManifestGateTest` | 3/3 通过 | 46 ms |
| `RetailQuestDriverOverlayTest` | 5/5 通过 | 9.5 s |
| `RetailSimpleHuntEquivalenceGateTest` | 2/2 通过 | 3.6 s |
| `RetailSimpleUseItemGateTest` | 5/5 通过 | 1.1 s |
| `RetailSimpleCollectItemGateTest` | 6/6 通过 | 1.2 s |
| `RetailSimpleTalkChainGateTest` | 8/8 通过 | 1.3 s |
| `RetailSimpleHuntFamilyGateTest` | 2/2 通过 | 347 s |
| `RetailSimpleTalkGateTest` | 4/4 通过 | 2.5 s |
