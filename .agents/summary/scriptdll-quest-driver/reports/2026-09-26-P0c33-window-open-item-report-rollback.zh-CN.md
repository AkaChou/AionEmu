# P0c-33：风暴窗口开启 → 正式判官复跑抓到两行真回归（24202/80320 回退 XML）+ 27 行 CHAIN_COMPOUND 定性

- 切片：P0c-33（真端任务驱动，launcher lane）
- 日期：2026-09-26
- 状态：完成（窗口内判官复跑 + 回退 + 复验全绿；运行时/客户端目检待授权）
- 关联：`p0c33_rollback_item_report_rows.py`、`p0c33-item-report-rollback-decisions.tsv`、
  `p0c32-accept-entrance-decisions.tsv`、`reports/2026-09-26-P0c32-accept-entrance-nine-rows.zh-CN.md`

## 1. 结论摘要

1. **风暴消退、窗口开启**：`ProductionQuestDefinitions.catalog()` 全视图构建成功
   （`STORM_HEALED entries=6224`），lane 的 CombineTask/CollectItem 编译器落地，664 行缺口清零。
2. **窗口第一价值 = 正式判官复跑**：T2（16 个风暴期采纳任务 + 受影响类）**184 例 / 6F / 4E**，
   逐条归因后**本车道真回归 = 2 行**：`24202` 与 `80320`（P0c-32 采纳行）。
3. **回归性质**：两行真端定义**缺 item_check 门路由**——客户端 select5(页 2375) 的检查按钮
   （24202 = `CHECK_USER_HAS_QUEST_ITEM_SIMPLE(20002)`、80320 = `CHECK_USER_HAS_QUEST_ITEM(39)`）
   在服务端无对应边 → 产生 2 条 `BUTTON_WITHOUT_ROUTE` 新指纹（客户端可见死按钮）。
4. **归因方法（可复用判例）**：对 **HEAD 版遗留 XML 定义**跑同一审计 = **0 条**，
   对真端定义 = 1 条 → **指纹由本车道翻转引入**，不是既有债（探针 `RetailXmlEraContractProbeTest`，
   用 `/tmp` 里的 HEAD 导出 XML 编译后进 `ImmutableQuestCatalog`，不触碰生产树）。
5. **根因**：遗留 XML 用 `<npc-item-report source="started" target="reward" item-id required failure-page>`
   表达该门（XML 编译器展开为 39/20002 成功/失败对）；**链登记表未转写该元素**，链编译因此无门路由。
6. **落地**：两行**回退 XML_RETENTION**（`SEMANTIC_GAP:RETAIL_TALK_CHAIN` +
   `basis=ITEM_CHECK_GATE_NOT_TRANSCRIBED`），XML ×2 恢复、目录行回插、链指纹删两行；
   `verify_retirement.py` = `catalog=1392 directory=1392 retired=4832 sum=6224 — OK`。
7. **回退后复验**：族门/链门/所有权/非 IR 轴/系统发放/影片门/启动门/契约白名单全绿；
   契约新指纹 0（两行从 52→44 的计数中消失）；`JournalRewardRowRepairContractTest` 回到既有
   `80020` 基线失败；`QuestMovieAndDialogLoopRegressionTest` 19/19 绿（24202 掉落步失败消失）。
8. **27 行 CHAIN_COMPOUND 定性完成**（本条为 P0c-33 原定目标）：三组，
   5 行名字证据 / 10 行 canonical 通道缺口 / 12 行已裁定 KEEP；**10 行缺口被证明可由现有数据面打通**
   （零售行符号 + 物品名索引 10/10 解析，无需陈旧的 `quest_data.xml`）。

## 2. 窗口开启与判官复跑

| 项 | 值 |
|---|---|
| 轮询命令 | `mvn -o -B test -Dtest=RetailStormPollProbeTest`（临时探针，已存档） |
| 结果 | `STORM_HEALED entries=6224`（此前持续 `entries=5560/6224 missing=664`） |
| T2 输入 | 1141 30312 30315 1526 1351 80290 80294 1323 1479 2611 3001 3023 21136 24123 24202 80320 |
| T2 结果 | 184 例 / 6F / 4E（`gates/T2-095648.log`） |

10 个失败方法的归因（对 `T3-035139` 基线做归一化集合差）：

| 失败方法 | 归因 |
|---|---|
| `EarlyElyosQuestRegressionTest` ×3 | 既有（基线同一方法名，NoSuchElement 于 `route()`） |
| `QuestStartItemDefinitionRegressionTest…quest 1197` | 既有 |
| `JournalRewardRowRepairContractTest.persistedRewardRowsAreRepairedOnEnterWorld` | 既有方法被掩蔽：基线在 `80020` 处中止（lane 修好 80020 后）暴露 `24202` → **本车道** |
| `LegacyTemplateMirrorRouteRegressionTest…quest 1131` | 既有（基线同一行） |
| `QuestClientContractGateTest…count=52` | 既有方法 + **本车道新增 2 条指纹**（24202/80320） |
| `CollectTurnInClientActionAlignmentBatchTest…quest 18745` | lane（采集族在飞） |
| `RetailSimpleCollectItemGateTest…frozen=155 retired=175` | lane（采集族 20 行退役未装指纹） |
| `QuestMovieAndDialogLoopRegressionTest…24202 drops` | **本车道**（真端 `collect_progress` → `collectingStep` 映射） |

## 3. 缺陷通道定性：`npc-item-report` 未转写

真端 item_check 轴的门在遗留 XML 里由 `<npc-item-report>` 表达，XML 编译器展开为：

```
started + <npc> + 39    -> reward（HasItem + 领奖窗）/ started（失败，CloseDialog）
started + <npc> + 20002 -> reward（同上）/ started（失败，CloseDialog）
```

链登记表（`build_quest_client_talk_chain_steps.py` 的产物）只转写 `TALK_TO_NPC` 路由与
`NPC_START/NPC_COMPLETE` 块，**没有 `npc-item-report` 的记录类型**；链编译 `buildChain` 因此
只回放登记表里的 13 条 R 记录，得到的 started 态只有 `QUEST_SELECT→SELECT5`、
`SET_SUCCEED→reward`、`FINISH_DIALOG` 自环——客户端 select5 的检查按钮成为死按钮。

真端数据面（两行齐备，非缺口）：
`Quest_SimpleTalk.xml` 声明 `item_check`；`quest.xml` 给出 `collect_item`（24202 = `quest_24202b ×1`、
80320 = `quest_80320a ×12`，符号经 `item_name_index` 解析）；客户端 HTML 声明检查按钮。

同类波及审计（临时探针 `RetailItemCheckCoverageProbeTest`）：真端 1988 个声明 `item_check` 的行中，
已驱动行 **16 个缺 CHECK 门**，其中 14 个以 `SELECT_QUEST_REWARD(1009)+HasItem` 表达（正常族形）、
**2 个为本次回退行**（`24202/80320`，`hasItemEdges=0`）、另 2 个潜伏（`13809/23809`：
客户端无检查按钮、XML 无 `npc-item-report`、契约审计不命中 → 维持 RETAIL_TABLE 观察）。

## 4. 回退落地与复验

`p0c33_rollback_item_report_rows.py`（手术式，whipsaw 守卫四副本一致基线 + 只改两行）：

1. 清单四副本：`24202/80320` → `XML_RETENTION` + 新证据列；
2. 生产 XML ×2 由 `git show HEAD:…` 回写（main + `target/classes`，遵守 P0c-17 判例）；
3. 目录行回插 ×2（保持 id 升序，main + target）；
4. 链 IR 指纹删两行（main + target 副本 md5 归一）。

复验：

| 验证 | 结果 |
|---|---|
| `verify_retirement.py` | `catalog=1392 directory=1392 retired=4832 sum=6224 — OK` |
| `RetailSimpleTalkGateTest` | 3/3 绿（漂移登记同步：24202/80320 活体分类 = `DIFF:TRANSITION_SET` 与登记一致） |
| `RetailSimpleTalkChainGateTest` | 2/2 绿（冻结指纹 = RETAIL_TABLE 集合，两行退回 KEEP 侧） |
| `RetailOwnershipGateTest` / `RetailNonIrAxisGateTest` / `RetailSystemGrantDispatchTest` | 4/4、4/4、7/7 绿 |
| `QuestClientContractGateTest` | 44 条指纹，**两行 0 条**（回退前 52 条含两行） |
| `JournalRewardRowRepairContractTest` | 回到既有基线失败（`80020`），24202 双修复边消失 |
| `QuestMovieAndDialogLoopRegressionTest` | 19/19 绿（24202 掉落步失败消失） |
| `QuestDefinitionCatalogManifestTest` / `QuestProductionStartupGateTest` / 交互物合同 / 白名单 | 10/10、2/2、2/2、1/1 绿（`PRODUCTION_COMPILE_OK=1363`、0 违规） |
| T3 全树 | 见 §5 |

## 5. T3 全树（窗口态）

`QUEST_FORK_COUNT=2 .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T3`
→ **2004 例 / 123F / 21E / 1 skipped**（`gates/T3-102720.log`，窗口态=生产视图可读，判官跑到真实断言）。

对基线 `T3-035139`（1999 例 / 68F / 26E，风暴态）做失败方法归一化 diff：
**新增 38 / 消解 9**。逐条归因结论：

- **本车道新增 0**：38 条新增中无一条方法涉及 24202/80320，且逐条核对失败消息里的任务 id
  （26905/13918/13951/15042/16802/18742/25022/28313/13704/13708…）**全部属他族**
  （SimpleHunt / SimpleSerialHunt / DataDriven / SimpleItemPlay / CombineTask 指纹重算），
  为并行车道在飞产物的指纹/合同未同步（如 `RetailCombineTaskGateTest` 574 行指纹漂移、
  `RetailSimpleHuntEquivalenceGateTest` 35023/35049… 缺冻结证据、`RetailSimpleCollectItemGateTest` 155 vs 175）。
- **消解 9** 含窗口恢复带来的绿（`ProductionCatalogWhitelistVerificationTest`、`QuestDefinitionCatalogManifestTest`、
  `DurableDaevanionWeaponRewardRowContractTest.staleRewardRowsAreHealedOnEnterWorld` 等）。
- 本片两行回退的唯一生产行为变化 = 该两任务的 owner 回到 XML；与之相关的族门/链门/影片门/判官全绿（§4）。

**说明（诚实口径）**：T3 是跨窗口比较（并行车道在 03:57→现在之间落地了大量编译器），
38 条新增不能全归因于"现在"，也不全归因于任何单一车道；本片自身的 delta 只有两行（入台→回退），
已按上表逐面复验。

**memory-bank 结构门既有红（非本片引入）**：`python3 -B .agents/memory-bank/verify_memory_bank.py`
= freshness OK、structure FAILED，47 条既有卡片（QE-014/020/022/023/024/030/031/032/053/054… 的 evidence）
指向已退役的任务 XML 路径。HEAD 版本即如此，本片新增卡片 QE-059/QE-060 零违规（已核）。
批量改写他车道卡片属独立清理切片，且与并发窗口冲突，本片只登记不修。

## 6. 27 行 `RETAIL_TALK_CHAIN_COMPOUND` 缺口定性（P0c-33 原定目标）

27 行（`retail-simple-talk-drift.tsv` 中 owner=XML_RETENTION 且活体分类为 COMPOUND）分三组：

### A 组：名字证据（5 行，census verdict `NAME_EVIDENCE_NEEDED`）

| qid | 形状 | 现状判据 |
|---|---|---|
| 1324 | give_item；talk=Novan | **名字通道已通**（Novan=204031、Maximianus/Herodes 可解析；census 陈旧）→ 可进物品轴转录批 |
| 1414 | give_item；talk=`LF2_Gear_Q1414` | talk 目标是**物件**（id 700175，use-item 通道）；acquire 名 Aeolus 二义 → 对象交互通道 |
| 1463 | give+remove；talk=Valerius/Medea | `Valerius` 在 NPC 模板名索引**无解析**（Medea=204424）→ 模板改名证据待补 |
| 11107 | give+remove；talk=`LF4_gateway_Li_Guard_Boss_Q03/04/05` | 三个 talk 目标是**传送门物件**（无 id）→ 对象/网关通道 |
| 80021 | give+remove；talk=`event_Shugo_Peddler_Df4_01`/Skanin/Grak | 活动 NPC 无解析（Skanin/Grak 可解析）→ 活动模板名证据待补 |

### B 组：canonical 输入缺口（10 行，`p0c10i-canonical-gaps.tsv` 记 KEEP）

`18035 18807 18809 21070 21460 24120 28035 28807 29070 29071`——生成器 canonical 合成要求
`quest_data.xml` 的 `quest_work_items`（符号→id 通道），而该文件**陈旧不可用**，故当时记 KEEP。

本切片实测**通道可行性 = 10/10**：真端行的 `give_item/remove_item` 符号在物品名索引里全部可解析
（符号 = `ITEM_` + `name_desc`，大小写不敏感）：

```
18035 ITEM_DOC_QUEST_18035A -> doc_quest_18035a -> 182213483
18807 ITEM_QUEST_18807A     -> quest_18807a     -> 182213220   （give+remove 同物=自消费）
18809 ITEM_RIDE_WHALE_002_ET60 -> ride_whale_002_et60 -> 190100013
21070 ITEM_QUEST_21070A -> quest_21070a -> 182207938
21460 ITEM_QUEST_21460A -> quest_21460a -> 182209520
24120 ITEM_QUEST_24120A -> quest_24120a -> 182215469
28035 ITEM_DOC_QUEST_28035A -> doc_quest_28035a -> 182213484
28807 ITEM_QUEST_28807A -> quest_28807a -> 182213221
29070 ITEM_DOC_QUEST_29070A -> doc_quest_29070a -> 182213297
29071 ITEM_DOC_QUEST_29071A -> doc_quest_29071a -> 182213298
```

结论：**生成器覆盖可行**——把 canonical 通道从 `quest_data.work_items` 改为
「真端行符号（`ITEM_` 前缀归一）+ 物品名索引 + 客户端页」即可；这同时解锁 C 组中 9 行
`CANONICAL_INPUT_GAP` 的重新裁定（它们另有 con_quest 轴需单独证据）。

### C 组：已裁定 KEEP（12 行，不动）

`4015 11001 13800 18600 19064 21138 23800 28600 29064 30055 30202 30302`
（`p0c10j-deferred-decisions.tsv`：CANONICAL_INPUT_GAP ×9、CUTSCENE_EVIDENCE_PENDING ×2 = 13800/23800、
CON_QUEST_SUCCESSOR_COND_MISSING ×1 = 11001）。已决不入台。

## 7. 遗留 / 下一片工作项

1. **item_check 门通道（npc-item-report 转写）**——生成器增记录类型（npc/source/target/item-id/
   required/failure-page）+ 链编译展开 39/20002 对；落地后 24202/80320 可重新入台，
   `13809/23809` 一并对裁。
2. **canonical 通道换源**（B 组 10 行 + C 组 9 行复裁）。
3. **A 组**：1324 可直接进批；1414/11107 走对象/网关通道；1463/80021 补模板改名证据。
4. **潜伏债登记**：
   * 链路径通用 `var0=0` 修复边应在**已有 REWARD 修复边**时不发射（`19004` 同形双修复边潜伏）；
   * 真端 `collect_progress → collectingStep` 映射（24202 = 2）与「收集期可掉落 / 冻结 0」判据冲突；
   * `JournalRewardRowRepairContractTest` 的 `80020` 既有失败（lane 侧 heal 边）。
5. **窗口剩余项**：T3 债池 diff（本片已跑，见 §5）、Ownership/verify 复跑（本片已跑）、
   lane 在飞的采集族指纹同步（`frozen=155 retired=175`，不属本车道）。

## 8. 证据与命令索引

| 命令 | 结果 |
|---|---|
| `mvn -o -B test -Dtest=RetailStormPollProbeTest` | `STORM_HEALED entries=6224` |
| `.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 …` | `T2-095648.log`（184/6F/4E）、`T2-101946.log`（87/3F，回退后） |
| `mvn -o -B test -Dtest=RetailXmlEraContractProbeTest` | XML 期 0 条 / 真端期 1 条 UNROUTED（24202、80320） |
| `mvn -o -B test -Dtest=RetailItemCheckCoverageProbeTest` | 已驱动行缺 CHECK 门 16 行（14 走 1009 门、2 本次回退、2 潜伏） |
| `mvn -o -B test -Dtest=RetailSimpleTalkGateTest -Dretail.talk.equivOut=…` | 族门 3/3 绿 + 活体分类 dump（24202/80320 = `DIFF:TRANSITION_SET`） |
| `python3 -B …/p0c33_rollback_item_report_rows.py` | 清单×4 + XML×2 + 目录×2 + 指纹×2 落地 |
| `python3 -B …/verify_retirement.py` | `catalog=1392 directory=1392 retired=4832 sum=6224 — OK` |
| `QUEST_FORK_COUNT=2 …/run_quest_gates.sh T3` | `T3-102720.log` |
| 探针源码存档 | `RetailStormPollProbeTest / RetailContractDeltaProbeTest / RetailWindowRowShapeProbeTest / RetailXmlEraContractProbeTest / RetailItemCheckCoverageProbeTest`（`.java.txt`） |

## 9. PENDING（不启动服务器，需授权）

运行时行为与客户端目检（24202/80320 退回 XML 后行为与 v0.9 一致；门通道落地后需再验一次
「点击 select5 检查按钮 → 持物进领奖窗 / 缺物留页面」）。未执行的命令：无（本片全部验证已跑）。
