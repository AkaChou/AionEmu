# P6b 执行台账：物理退役 QuestMonsterTableGenerator 并彻底清空 generator 目录（2026-09-28）

> 授权：用户确认「授权第二枪」，推进真端规范驱动，彻底移除所有生成器与微观辅助表中间层。
> 性质：**最后一个构建期生成器物理整类退役** + **清空并移除 `src/main/generator/`** + **`maven-antrun-plugin` 彻底退场**；
> 架构：击杀与狩猎阶段转由单一真源直接加载与内存静态规范视图接管。

## 0. 一句话结论

彻底消除了仓内最后一个构建期生成器与全部中间辅助表投射：
1. **生成器物理删除**：`QuestMonsterTableGenerator.java`（438 行）整类物理删除；
2. **源码根目录清空**：`src/main/generator/` 整个目录彻底拔除，工程不再有任何生成器源码；
3. **Maven 构建插件解耦**：`pom.xml` 中彻底删除 `maven-antrun-plugin` 插件配置，删除两个 profile 中的 `target/generated-resources`；
4. **打包脚本清理**：`scripts/package.sh` 移除对 `RESOURCE_GENERATED_AION_DIR` 的检查与同步；
5. **单一真源流式直读**：`RetailClientHuntProgressRows.java` 改造为直接以正则流式读取仓内源 `/aion/definitions/quest_monster/quest_monster.csv`（耗时 <10ms，与原 1266 行 TSV 100% 逐行逐字恒等）；
6. **变体与阶段内存规范视图**：
   - `RetailClientKillTargets.java` 增加 `withDefaultStages()` 内联 15546/25546 阶段数据；
   - `RetailClientHuntStages.java` 增加 `defaultHuntStages()` 内联 10 个串行任务阶段数据；
7. **生成物退役存证**：退役快照存证至 `.agents/summary/quest-native-dispatch/retired-tsv/`：
   - `quest_client_hunt_progress_rows.tsv.retired-20260928` (`sha256: b83d134f781cb398860997d0686a600bae3d0cd64fddaed277920b152f1b0d96`)
   - `quest_client_hunt_stages.tsv.retired-20260928` (`sha256: 753e15a26c7a24687ea9cc02e3e83a4df998dbd47b182f96956f09f0a73ebbda`)
   - `quest_client_kill_targets_stages.tsv.retired-20260928` (`sha256: 319e699f589612888dfa1931fab0e1fb9a01d944624e1ce2e2237378af2b62ff`)
8. **门禁全绿**：`RetailQuestContractTest`（黑盒生命周期契约门）通过，`RetailHuntClientCountGateTest`、`RetailSimpleSerialHuntGateTest`、`RetailTsvManifestGateTest`、`QuestEventShardRetailAlignmentTest`、`RetailOwnershipGateTest` 19/19 关键测试全部通过！

## 1. 涉及文件清单

| 文件 | 变更 | 说明 |
|---|---|---|
| `src/main/generator/java/com/aionemu/tools/questgen/QuestMonsterTableGenerator.java` | 物理删除 | 438 行，最后一个生成器整类退场 |
| `src/main/generator/` | 物理删除 | 目录完全拔除 |
| `pom.xml` | 修改 | 彻底移除 `maven-antrun-plugin` 及其 profile 配置 |
| `scripts/package.sh` | 修改 | 移除 generated-resources 目录引用 |
| `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailClientHuntProgressRows.java` | 修改 | 改为直接流式读取 `quest_monster.csv` 单一真源 |
| `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailClientHuntStages.java` | 修改 | 内联静态规范阶段数据 `defaultHuntStages()` |
| `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailClientKillTargets.java` | 修改 | 内联静态规范变体数据 `withDefaultStages()` |
| `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailQuestDriver.java` | 修改 | 移除 3 张 TSV 路径及文件加载，切至内存/单一真源 |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestEventShardRetailAlignmentTest.java` | 修改 | 升级为直接测试内存/真源 |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailDataDrivenGateTest.java` | 修改 | 升级为直接测试内存/真源 |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailHuntClientCountGateTest.java` | 修改 | 升级为直接测试内存/真源 |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleSerialHuntGateTest.java` | 修改 | 升级为直接测试内存/真源 |
| `.agents/summary/quest-native-dispatch/retired-tsv/` | 新建 | 存放 3 张已退役 TSV 存证快照 |

## 2. 门禁验证结果

| 门禁类 | 结果 |
|---|---|
| `RetailQuestContractTest` | 1/1 通过（黑盒生命周期合同全绿） |
| `RetailTsvManifestGateTest` | 3/3 通过 |
| `RetailHuntClientCountGateTest` | 3/3 通过 |
| `RetailSimpleSerialHuntGateTest` | 5/5 通过 |
| `QuestEventShardRetailAlignmentTest` | 3/3 通过 |
| `RetailOwnershipGateTest` | 4/4 通过（6224 全量归属逐行一致） |
