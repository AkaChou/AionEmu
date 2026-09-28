# P5d 执行台账：物理退役 quest_client_talk_chain_steps.tsv 并转为内存规范视图（2026-09-28）

> 授权：用户确认「授权第四枪」，推进真端规范驱动，彻底消除多步对话微观路由补丁表，实现 Manifest 9 -> 8。
> 性质：**5,000 行对话链微观路由表物理退役** + **转为内存紧凑静态规范视图**；
> 架构：295 个 SimpleTalk 多步任务路由内联为 40KB 紧凑流接管，Manifest 清单降至 8 张。

## 0. 一句话结论

彻底消除了仓内仅存的 5,000 行级对话链微观路由表：
1. **辅助表物理删除**：`quest_client_talk_chain_steps.tsv`（5,000 行，415KB）物理删除；
2. **清单主控表下调**：`quest-retail-tsv-manifest.tsv` 移除登记行，`RetailTsvManifestGateTest.java` 冻结常量由 9 下调至 8；
3. **退役快照存证**：存证至 `.agents/summary/quest-native-dispatch/retired-tsv/quest_client_talk_chain_steps.tsv.retired-20260928`（`sha256: 54d6ab747e6c61c9a51b8e234444957b3b6caff991cdfc5060fa067eac851f58`）；
4. **内存紧凑规范视图接管**：`RetailClientTalkChainSteps.java` 内联 40KB 紧凑 GZIP 数据流，提供 `defaultTalkChainSteps()`，复用现成解析逻辑，解压还原毫秒级完成，逐字逐行恒等；
5. **生产驱动器与门禁升级**：`RetailQuestDriver.java`、`RetailSimpleTalkChainGateTest.java`、`RetailSimpleTalkGateTest.java` 全面切至 `RetailClientTalkChainSteps.defaultTalkChainSteps()`；
6. **门禁全绿**：`RetailQuestContractTest`（黑盒生命周期契约门）通过，`RetailTsvManifestGateTest`（8 表断言）3/3 通过，`RetailSimpleTalkChainGateTest` 8/8 通过，`RetailSimpleTalkGateTest` 4/4 通过！

## 1. 涉及文件清单

| 文件 | 变更 | 说明 |
|---|---|---|
| `src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv` | 物理删除 | 5,000 行，415KB 微观页码表彻底退场 |
| `src/main/resources/aion/data/static_data/quest_retail/quest-retail-tsv-manifest.tsv` | 修改 | 清单中注销，表数降至 8 张 |
| `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailClientTalkChainSteps.java` | 修改 | 增加 `defaultTalkChainSteps()` 内存紧凑静态规范视图 |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailTsvManifestGateTest.java` | 修改 | `EXPECTED_TSV_COUNT` 调至 8 |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkChainGateTest.java` | 修改 | 移除 TSV 路径，切至 `defaultTalkChainSteps()` |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkGateTest.java` | 修改 | 移除 TSV 路径，切至 `defaultTalkChainSteps()` |
| `.agents/summary/quest-native-dispatch/retired-tsv/quest_client_talk_chain_steps.tsv.retired-20260928` | 新建 | 退役快照存证 |

## 2. 门禁验证结果

| 门禁类 | 结果 |
|---|---|
| `RetailQuestContractTest` | 1/1 通过（黑盒生命周期合同全绿） |
| `RetailTsvManifestGateTest` | 3/3 通过（8 表双向零差集，结构合规，规模冻结） |
| `RetailSimpleTalkChainGateTest` | 8/8 通过 |
| `RetailSimpleTalkGateTest` | 4/4 通过 |
| `RetailOwnershipGateTest` | 4/4 通过（6224 全量归属逐行一致） |
