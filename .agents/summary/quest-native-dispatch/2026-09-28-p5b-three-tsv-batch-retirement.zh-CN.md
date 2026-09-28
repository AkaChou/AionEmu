# 批 P5b 执行台账：3 张辅助表物理整表批量退役（2026-09-28）

> 授权：用户「一起」推进退役，贯彻真端规范生命周期，批量收敛多余页码与辅助 TSV 表。
> 性质：**3 张 TSV 物理整表批量退役**（Manifest 13 → 10）；
> 架构：彻底清除微观页码梯与辅助登记表，转由内存静态规范视图接管。

## 0. 一句话结论

3 张辅助表（共计 553 行）正式整表物理退役，TSV 清单总数**成功压降至 10 张**：
1. `quest_client_reward_npcs.tsv`（120 行，118 个任务映射）：交付 NPC 复合名映射收敛为 14 组紧凑不可变列表；
2. `quest_client_talk_chain_pages.tsv`（129 行，124 个任务映射）：链式信件页梯收敛为 18 组紧凑静态阶段结构；
3. `quest_client_handin_pages.tsv`（304 行，300 个任务映射）：交付型五页角色页全部统一为 `4762/1011/10000/10001/10002`，仅保留 49 例 `okLocalClose` 标记；
* **退役前快照存证**：均完整落盘至 `.agents/summary/quest-native-dispatch/retired-tsv/`；
* **构建期生成器同轴解耦**：`QuestTalkCollectChainPagesGenerator.java` 改由读取 `retired-tsv/` 存证文件，构建期生成恒等无损；
* **生产驱动与测试同轴升级**：`RetailQuestDriver.java` 移除 3 张表的加载逻辑；6 个测试类统一升级为 `default*()` 规范通道；
* **清单与门禁缩减**：`quest-retail-tsv-manifest.tsv` 移除 3 行登记，`RetailTsvManifestGateTest.EXPECTED_TSV_COUNT` **由 13 降至 10**（3/3 全绿）。

## 1. 退役快照与元数据

| 退役文件 | 存证快照路径 | sha256 |
|---|---|---|
| `quest_client_reward_npcs.tsv` | `.agents/summary/quest-native-dispatch/retired-tsv/quest_client_reward_npcs.tsv.retired-20260928` | `34fa9916c2e73881d1a6ecdef03535ea765e423c2e2b41e0dfd86bfee084c9d0` |
| `quest_client_talk_chain_pages.tsv` | `.agents/summary/quest-native-dispatch/retired-tsv/quest_client_talk_chain_pages.tsv.retired-20260928` | `93c7520a587cb9c1a67eda2876957e47bcb16db96eb8f5158c0dc5273eac1a85` |
| `quest_client_handin_pages.tsv` | `.agents/summary/quest-native-dispatch/retired-tsv/quest_client_handin_pages.tsv.retired-20260928` | `953197bf8b828301eac27a6b636b67ab7411c3473fe015049af9b7184fb14a20` |

## 2. 门禁验证结果（42/42 全绿）

| 门禁类 | 结果 | 耗时 |
|---|---|---|
| `RetailTsvManifestGateTest` | 3/3 通过（10 表强恒等断言） | 43 ms |
| `RetailQuestDriverOverlayTest` | 5/5 通过 | 9.6 s |
| `RetailSimpleHuntEquivalenceGateTest` | 2/2 通过 | 3.7 s |
| `RetailSimpleUseItemGateTest` | 5/5 通过 | 1.2 s |
| `RetailSimpleCollectItemGateTest` | 6/6 通过 | 1.2 s |
| `RetailQuestAiNameGroupGateTest` | 5/5 通过 | 787 ms |
| `RetailSimpleTalkChainGateTest` | 8/8 通过 | 1.3 s |
| `RetailSimpleTalkGateTest` | 4/4 通过 | 2.5 s |
| `RetailOwnershipGateTest` | 4/4 通过（6224 全量归属一致） | 34 ms |
