# 批 P4f 执行台账：quest_client_use_item_report.tsv 整表退役（2026-09-28）

> 授权：用户「开工·开始授权不要执著于 ir，以真端的驱动为主，推进“规范形生命周期”（Canonical Lifecycle）... 确立原则：页码类 TSV 只退役、绝不新增。授权下一步做什么」→「开始」。
> 性质：**TSV 物理整表退役**（Manifest 16 → 15）；
> 架构：彻底摆脱客户端 select5 微观页码报告模式 TSV，转由内存静态规范形视图接管。

## 0. 一句话结论

`quest_client_use_item_report.tsv`（106 行：2 行注释 + 104 行数据）正式整表物理退役：
* 退役前快照落盘：`.agents/summary/quest-native-dispatch/retired-tsv/quest_client_use_item_report.tsv.retired-20260928`（sha256: `72289bcaec2f3fd3c0af4e53012fb8256eeeef6960e5b61c3350b77640038efc`）；
* 生产驱动同轴退场：`RetailQuestDriver.java` 移除该表文件读取逻辑，改用 `RetailClientUseItemReport.defaultReport()`；
* 内存规范视图：5 个活动任务（80482, 80486, 80554, 80558, 80612）保持 CHECK 交付物 HasItem 门控（交付物等于接取道具本身），其余 99 个任务默认走规范形 REWARD 奖励窗；
* 测试同轴更新：`RetailDataDrivenGateTest` 移除无用死引用；`RetailSimpleUseItemGateTest` 改用 `defaultReport()`（5/5 全绿）；`RetailQuestDriverOverlayTest`（5/5 全绿）；
* 清单与计数门：`quest-retail-tsv-manifest.tsv` 移除该表，`RetailTsvManifestGateTest.EXPECTED_TSV_COUNT` 由 16 降至 15（3/3 全绿）。

## 1. 退役快照与元数据

| 证据 | 路径 | sha256 |
|---|---|---|
| 退役快照 | `.agents/summary/quest-native-dispatch/retired-tsv/quest_client_use_item_report.tsv.retired-20260928` | `72289bcaec2f3fd3c0af4e53012fb8256eeeef6960e5b61c3350b77640038efc` |
| 历史声明生成器 | `p3b_client_use_item_report.py`（树内无脚本，为历史烘焙生成） | - |

## 2. 受理门禁验证

| 门禁类 | 结果 | 耗时 |
|---|---|---|
| `RetailTsvManifestGateTest` | 3/3 通过 | 28 ms |
| `RetailSimpleUseItemGateTest` | 5/5 通过 | 215 ms |
| `RetailQuestDriverOverlayTest` | 5/5 通过 | 10.5 s |
| `RetailSimpleTalkGateTest` | 4/4 通过 | 6.4 s |
