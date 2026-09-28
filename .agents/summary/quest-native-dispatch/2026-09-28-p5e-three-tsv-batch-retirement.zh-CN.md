# P5e 执行台账：批量物理退役 3 张辅助小表，Manifest 降至 5 张（2026-09-28）

> 授权：用户确认「先 A 后 B」，继续削减 TSV 到极限，实现 Manifest 8 -> 5。
> 性质：**3 张百行级小表物理退役** + **转为内存静态规范视图**；
> 架构：彻底消灭击杀目标、区域别名、AI 对话组三张外部 TSV，Manifest 表数由 8 锐减至 5。

## 0. 一句话结论

彻底消除了仓内仅存的 3 张百行级辅助小表：
1. **辅助表物理删除**：
   - `quest_client_kill_targets.tsv`（50 行，变体目标名单）；
   - `quest_enterarea_zone_resolution.tsv`（150 行，进区域别名映射）；
   - `retail-quest-ai-name-groups.tsv`（37 行，对话名组名单）；
2. **清单主控表下调**：`quest-retail-tsv-manifest.tsv` 注销该 3 表，`RetailTsvManifestGateTest.java` 规模常量由 8 降至 5；
3. **退役快照存证**：存证至 `.agents/summary/quest-native-dispatch/retired-tsv/`：
   - `quest_client_kill_targets.tsv.retired-20260928` (`sha256: 250ff5ee7dbfb5ce7aba19c5075d4f998632e2051dbb46d8becbce2b2cb0a9f9`)
   - `quest_enterarea_zone_resolution.tsv.retired-20260928` (`sha256: 7e21f0088efececdf9d1b1e2e0ccff8b049538d334821541373865c4dcfaa0f7`)
   - `retail-quest-ai-name-groups.tsv.retired-20260928` (`sha256: f87be7fcfdc5fd4ffcc74c7a7a5d4cd11c633b3271c5fbc4c65af0f5463031ec`)
4. **内存静态规范视图接管**：
   - `RetailClientKillTargets.java` 内联静态规范视图 `defaultKillTargets()`；
   - `RetailEnterAreaZoneResolution.java` 内联静态规范视图 `defaultZoneResolution()`；
   - 新增 `RetailQuestAiNameGroups.java` 提供内联内存规范流 `streams()` 与 `defaultGroups()`；
5. **生产驱动器与门禁升级**：
   - `RetailQuestDriver.java` 移除 3 张 TSV 加载，切至纯内存通道；
   - `QuestIlumaNorsvoldKillTargetCoverageTest`、`RetailEnterAreaZoneRegistrationGateTest`、`RetailQuestAiNameGroupGateTest`、`RetailDataDrivenGateTest` 等全面升级为内存规范通道；
6. **门禁全绿**：`RetailQuestContractTest`（黑盒生命周期契约门）1/1 通过，`RetailTsvManifestGateTest`（5 表断言）3/3 通过，`RetailOwnershipGateTest` 4/4 通过！

## 1. 涉及文件清单

| 文件 | 变更 | 说明 |
|---|---|---|
| `src/main/resources/aion/data/static_data/quest_retail/quest_client_kill_targets.tsv` | 物理删除 | 50 行小表退场 |
| `src/main/resources/aion/data/static_data/quest_retail/quest_enterarea_zone_resolution.tsv` | 物理删除 | 150 行小表退场 |
| `src/main/resources/aion/data/static_data/quest_retail/retail-quest-ai-name-groups.tsv` | 物理删除 | 37 行小表退场 |
| `src/main/resources/aion/data/static_data/quest_retail/quest-retail-tsv-manifest.tsv` | 修改 | 清单中注销 3 表，表数降至 5 张 |
| `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailClientKillTargets.java` | 修改 | 增加 `defaultKillTargets()` 内存规范视图 |
| `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailEnterAreaZoneResolution.java` | 修改 | 增加 `defaultZoneResolution()` 内存规范视图 |
| `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailQuestAiNameGroups.java` | 新建 | 对话名组内存规范视图 |
| `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailQuestDriver.java` | 修改 | 移除 3 张 TSV 路径，切至内存规范视图 |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailTsvManifestGateTest.java` | 修改 | `EXPECTED_TSV_COUNT` 调至 5 |
| `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestIlumaNorsvoldKillTargetCoverageTest.java` | 修改 | 测试改调 `defaultKillTargets()` |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailEnterAreaZoneRegistrationGateTest.java` | 修改 | 测试改调 `defaultZoneResolution()` |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailQuestAiNameGroupGateTest.java` | 修改 | 测试改调 `RetailQuestAiNameGroups.defaultGroups()` |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailQuestAiNameGroupsFixture.java` | 修改 | 夹具改调 `RetailQuestAiNameGroups.streams()` |
| `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailDataDrivenGateTest.java` | 修改 | 测试改调内存规范视图 |
| `.agents/summary/quest-native-dispatch/retired-tsv/` | 新建 | 3 张已退役 TSV 存证快照 |

## 2. 门禁验证结果

| 门禁类 | 结果 |
|---|---|
| `RetailQuestContractTest` | 1/1 通过（黑盒生命周期合同全绿） |
| `RetailTsvManifestGateTest` | 3/3 通过（5 表双向零差集，结构合规，规模冻结） |
| `RetailQuestAiNameGroupGateTest` | 5/5 通过 |
| `RetailEnterAreaZoneRegistrationGateTest` | 2/2 通过 |
| `QuestIlumaNorsvoldKillTargetCoverageTest` | 4/4 通过 |
| `RetailOwnershipGateTest` | 4/4 通过（6224 全量归属逐行一致） |
