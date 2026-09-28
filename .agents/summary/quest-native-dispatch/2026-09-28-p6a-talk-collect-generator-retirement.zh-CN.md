# P6a 执行台账：首个构建期生成器与混合链表物理退役（2026-09-28）

> 授权：用户确认「开始第一枪」，推进真端规范驱动，彻底移除生成器与微观页码梯中间层。
> 性质：**首个构建期生成器物理整类退役** + **依赖表与事实源退役**；
> 架构：斩断构建期 BFS 页梯算法，转由内存静态规范视图接管。

## 0. 一句话结论

彻底消除了用于在构建期生成客户端 talk+collect 微观页码梯的生成器：
1. **生成器物理删除**：`QuestTalkCollectChainPagesGenerator.java`（414 行）整类物理删除；
2. **Maven 构建解耦**：`pom.xml` 中移除 `generate-retail-talk-collect-chain-pages` 执行块；
3. **事实源物理删除**：`talk_collect_frozen_facts.csv`（20 行）物理删除；
4. **生成物退役存证**：`quest_client_talk_collect_chain_pages.tsv`（86 行）退役前快照存证至 `retired-tsv/`（`sha256: 2b379d238889b9e2932fc9ca631775f8a3b4dbf8c28132ae94fbc5ced522767a`）；
5. **生产驱动器与门禁升级**：`RetailClientTalkCollectChainPages.java` 转为内存静态规范视图，`RetailQuestDriver.java` 与 `RetailDataDrivenGateTest.java` 升级为 `defaultTalkCollectChainPages()` 内存通道；
6. **门禁全绿**：`RetailQuestContractTest`（黑盒生命周期契约门）1/1 通过，`RetailSimpleCollectItemGateTest` 等 27 例关键门禁全绿。

## 1. 涉及文件清单

| 文件 | 变更 | 说明 |
|---|---|---|
| `src/main/generator/java/com/aionemu/tools/questgen/QuestTalkCollectChainPagesGenerator.java` | 物理删除 | 414 行，BFS 页梯生成器彻底退场 |
| `src/main/resources/aion/definitions/quest_dialog/talk_collect_frozen_facts.csv` | 物理删除 | 20 行，伴生事实源彻底退场 |
| `pom.xml` | 修改 | 移除 antrun 中的生成器编译与执行块 |
| `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailClientTalkCollectChainPages.java` | 修改 | 转为内存静态规范视图，提供 `defaultTalkCollectChainPages()` |
| `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailQuestDriver.java` | 修改 | 移除 TSV 文件加载逻辑，改读内存静态规范视图 |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailDataDrivenGateTest.java` | 修改 | 测试夹具改读内存静态规范视图 |
| `.agents/summary/quest-native-dispatch/retired-tsv/quest_client_talk_collect_chain_pages.tsv.retired-20260928` | 新建 | 退役快照存证 |

## 2. 门禁验证结果

| 门禁类 | 结果 |
|---|---|
| `RetailQuestContractTest` | 1/1 通过（10.33 s，黑盒生命周期合同全绿） |
| `RetailTsvManifestGateTest` | 3/3 通过 |
| `RetailSimpleCollectItemGateTest` | 6/6 通过 |
| `RetailQuestAiNameGroupGateTest` | 5/5 通过 |
| `RetailSimpleTalkChainGateTest` | 8/8 通过 |
| `RetailSimpleTalkGateTest` | 4/4 通过 |
| `RetailOwnershipGateTest` | 4/4 通过（6224 全量归属逐行一致） |
