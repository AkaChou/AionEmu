# P0c-10g SimpleTalk wave A 链式行门禁转正（探针 → 永久三不变量门禁）

- 日期：2026-09-25
- 切片：P0c-10g（P0c-10f wave A 收尾：临时对拍探针转正为永久门禁）
- 前置：`2026-09-25-P0c10f-talk-chain-wave-a.zh-CN.md`（83 ADOPT 退役 + 40 KEEP + 冻结指纹入仓）

## 交付

1. **`RetailSimpleTalkChainGateTest`**（`src/test/java/com/aionemu/gameserver/questEngine/retail/`，
   新增，替代已删除并归档的 `RetailTalkChainProbeTest`，源码存
   `p0c10f-talk-chain-probe.java.txt`）。三不变量：
   - **回放保真**：83 行 ADOPT 逐行用生产口径 fixture（`quest_client_dialog_exits.tsv` 全量装载，
     非 probe 的 `empty()`）编译；合成定义节点投影逐一等于登记 N 记录（label/status/var0）；
     IR 指纹等于冻结值 `retail-simple-talk-chain-ir-fingerprints.tsv`（83 行）。
     重冻结开关：`-Dretail.talkChain.fingerprintOut=<path>`。
   - **retention 分区**：登记行集（137 行）对保留清单零孤儿，恰好二分 ADOPT
     （`RETAIL_TABLE` + `RetiredQuestIds.contains`）与 KEEP（`XML_RETENTION` + 未退役）。
   - **登记形状**：每行有 N 节点 + P 布局记录；ADOPT 行必须有 R 路由。行集允许 wave B 增长，
     已冻结的 83 行指纹与分区不许静默漂移。
2. **T1 清单登记**：`affected_quest_tests.py` `T1_GATE_CLASSES` 增补
   `RetailSimpleTalkChainGateTest`（第 18 类）。

## 证据

- `mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest`：**Tests run: 2, Failures: 0, Errors: 0**。
  指纹在生产口径 fixture 下与冻结值精确相等（等价探针已证链式路径不消费 `exits`
  ——`RetailSimpleTalkDefinitionCompiler.compile` 只有 `singleStep()` 行走 `build(...exits...)`，
  链式行走 `buildChain(entry, chainSteps, metadata)`，本次门禁以实测闭环确认）。
- `run_quest_gates.sh T1`（62 例）：新门禁 2/2 绿；`RetailSimpleTalkGateTest` 1 Error 为
  共享 target 竞态（`NoClassDefFoundError: RetailSimpleHuntEquivalenceGateTest`，第 5 次出现），
  `test-compile` 恢复后单跑 **3/3 绿**；其余 2 类失败
  （`ProductionCatalogWhitelistVerificationTest` 15548/25548 NO_NODES、
  `QuestDefinitionCatalogManifestTest` 6E）经 `git status` 核对：**两个门禁测试文件本身处于
  并行会话修改中（M），15548/25548 在 HEAD 即无节点**——归属并发车道，与本切片无关。
- 登记表现在态（137 行 / 922 路由）：与 wave A 报告所记 837/123 相比，builder 收尾轮次把
  残组 14 行（B-only 无路由：1479/11069/13700/13701/18210/18940/21073/23700/23701/26990/
  28210/28940/39003/49003）并入 wave A 行集并收敛路由转写至 922。**83 行 ADOPT 集合与冻结
  指纹文件双向精确相等（实测）**；14 行残组全部落 `XML_RETENTION` 侧，由 KEEP 不变量覆盖。
- `verify_retirement.py` = **catalog=2011 directory=2011 retired=4213 sum=6224 — OK**（较
  wave A 实测 4209 再 +4，为并行会话在飞退役；恒等式成立、零悬空引用）。

## 结论

- wave A 的退役态现在有独立于对拍探针的常驻护栏：指纹漂移、retention 翻转、登记孤儿、
  退役态回退四类风险各有一条失败信息直接定位。
- 无生产代码改动；本切片只新增测试类 + T1 登记。

## 未验证

- 83 行退役任务的**运行时行为**与**客户端抽检**仍属 P3 终局项（沿用 wave A 报告口径）。
- T1 整档绿依赖并行会话收口其在飞的 whitelist/manifest 门禁改动，本切片不代修。

## 下一步

- wave B：177 行链+物品复合（census `p0c10e-talk-chain-census.tsv` 的 wave B 集）。
- 残组/变体 54 行（40 decisions + 14 残组）逐机制归零评估。
