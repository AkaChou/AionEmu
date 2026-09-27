# M4-b：CombineTask 真端驱动落地 + 574 个 XML 退役（catalog 4440 → 3866）

- 日期：2026-09-23
- 切片：M4 第二批（CombineTask 从"真端表可复现"到"真端驱动生产 + XML 退役"）
- 前置：`2026-09-23-M4a-combine-task-table-and-shapes.zh-CN.md`、`2026-09-23-M3c-retired-xml-in-git-history.zh-CN.md`
- 一句话：CombineTask 全族 574 行由**真端模板表 + 本服配方索引 + 真端 quest.xml 元数据**合成，
  合成结果与退役前 XML 编译结果 **574/574 逐行 IR 等价**；删除 574 个 quest-definition XML，
  保留清单 `RETAIL_TABLE` 1784 → **2358**，生产白名单 4440 → **3866**。

## 1. 交付

| 交付物 | 位置 | 说明 |
|---|---|---|
| 真端表只读视图 | `RetailCombineTaskTable.java` | 行 = task_npc / combineskill / combine_skillpoint / recipe_name / product / give_component1..8（DOM 解析，允许内部 DTD） |
| 配方索引 | `RetailRecipeIndex.java` | `(skillid, productid)` → recipe id（真端表只有配方符号名，服务端按 recipe id 工作） |
| 合成器 | `RetailCombineTaskDefinitionCompiler.java` | 族常量形状 + 六个稳定拒绝码；技能/技能点/产物/分量与 quest.xml 元数据交叉校验 |
| 生产接线 | `RetailQuestDriver` / `RetailQuestCatalog` | 保留清单 `family=CombineTask` 分派到新编译器；目录判定同时认 CombineTask 行 |
| 家族门禁 | `RetailCombineTaskGateTest`（3 例） | 家族规模冻结 574 + 逐行真端语义不变量 + 冻结 IR 指纹 |
| 冻结指纹 | `src/test/resources/quest/retail-combine-task-ir-fingerprints.tsv`（574 行） | XML 删除后的漂移守卫（`-Dretail.combine.fpOut=<path>` 重算） |
| 退役脚本 | `m4b_retire_combine_task_xml.py` | 删除生产 XML + 移除 catalog 行 + 证据 TSV（`m4b-retired-combine-task-evidence.tsv`，574 行 sha256） |

## 2. 形状与语义（真端为源）

```
unaccepted --QUEST_SELECT/ASK/ACCEPT_1(accept-actions)/ACCEPT_SIMPLE--> started
started    --SELECT_QUEST_REWARD + has-item(product)--> reward        (priority 0, 回收 give_component)
started    --SELECT_QUEST_REWARD 缺产物--> started                   (priority 10, SELECT3_2 回退页)
reward     --SELECTED_QUEST_REWARD1..NOREWARD--> complete            (扣产物 + 忘配方 + complete-quest)
started    --abandon--> unaccepted                                   (忘配方)
```

- 四节点 `var0=0`，进度域与 SimpleHunt/SimpleTalk 同构（6 位 SECTION，本族不做行号投影）。
- **每个 `task_npc` 一份完整路由**（真端表给多名，一般是天/魔各一），接取动作 =
  `give_componentN` 逐条 `give-item` + `learn-recipe`（`QUEST_OWNED`）。
- 配方 id 由 `recipe_name` 所在行的 `(combineskill, product)` 反查本服 `recipe_templates.xml`：574/574 唯一。
- 两侧一致性（不过即拒绝，退回 XML）：表 vs 真端 quest.xml 的 `combineskill` / `combine_skillpoint` /
  `collect_item*`（产物）/ `quest_work_item*`（分量）。

## 3. 一次性等价证据（退役前）

| 证据 | 命令 | 结果 |
|---|---|---|
| 全族 IR 对拍 | 临时探针（合成 vs 退役前 XML，规范化指纹） | `family=574 accepted=574 identical=574`，拒绝码/差异形状均为空 |
| 家族门禁（退役前） | `mvn -o -Dtest=RetailCombineTaskGateTest test` | 3/3 绿 |
| 退役前聚焦门禁 | `mvn -o -Dtest=<17 类选择器> test` | 见 §5（`gates/m4b-focus-preretire-run1.log`；当时的 3 条失败为"清单已翻转、XML 未删"的中间态） |

判据说明：本族**没有**逐任务客户端 HTML（`quest_client_dialog_exits.tsv` / `quest_client_summary_rows.tsv`
对 574 个 id 命中 0），客户端契约落在合成窗口本身，因此本族的"客户端契约"就是族形状常量；
这解释了为什么可以整族复现，而不是逐任务取证。

## 4. 退役执行

| 步骤 | 命令 | 结果 |
|---|---|---|
| dry-run | `python3 -B m4b_retire_combine_task_xml.py --dry-run` | `migrated=574 moved=574 missing=0`；`catalog removed=574 remaining=3866` |
| 正式退役 | `python3 -B m4b_retire_combine_task_xml.py` | 同上落盘；证据 TSV 574 行（含退役前 sha256 + `git-history:<path>`） |
| 保留清单 | `python3 -B build_retention_list.py` | `RETAIL_TABLE=2358`（+574）、`FAMILY_PENDING=1815`（−574）；三份清单同步 |
| 退役收口 | `python3 -B verify_retirement.py` | `catalog=3866 directory=3866 retired=2358 sum=6224` + `OK: 目录一致，无悬空生产引用` |
| 目录文档 | `python3 -B refresh_catalog_doc_links.py --apply` | `retired=2358 replaced=574 dangling=0`（再跑为 `replaced=0`） |

**副作用（已登记）**：`quest-start-metadata-retail-cap-exceptions.tsv` 329 → **42**。
原 287 条本族任务在 XML 时代带服务端"整族 82 级封顶"约定，真端 `maxlevel_permitted=0`（无上限）；
改由真端元数据驱动后该封顶消失。封顶值 82 高于 5.8 玩家等级上限（66），**无可观察影响**。
生成器 `build_retail_contract_tsv.py` 已收紧口径：封顶例外只统计**仍在生产白名单里**的任务。

## 5. 验证证据

| 证据 | 命令 | 结果 |
|---|---|---|
| 聚焦门禁（17 类，退役后） | `mvn -o -Dtest='com.aionemu.gameserver.questEngine.retail.*Test,ProductionCatalogWhitelistVerificationTest,RetailOwnershipGateTest,QuestRetailClassGateTest,QuestRewardItemGateTest,RetailMetadataEquivalenceGateTest,QuestRetailStartMetadataGateTest,QuestSimpleHuntRetailContractTest' test` | `gates/m4b-focus-postretire-run1.log`：43 例 1F（`RetailMetadataEquivalenceGateTest` 的 `catalog>4000` 下限过期） |
| 上限修正后复跑 | 同上 4 类 | **14/14 绿 BUILD SUCCESS** |
| questEngine 全树 | `mvn -o -Dtest='com.aionemu.gameserver.questEngine.**' test` | 见 §6（`gates/m4b-questengine-run1.log`） |

中间态踩坑（都已修，留证避免重犯）：

1. 清单翻转但 XML 未删时，`RetailOwnershipGateTest` / `RetailMetadataEquivalenceGateTest` 会报
   "catalog 仍留退役任务 / 覆盖 6798 ≠ 6224" —— 预期中间态，删除 XML 后自愈。
2. `QuestRetailStartMetadataGateTest` 的封顶例外表必须**在删除 XML 之后**重算（生成器已改为只统计生产白名单内任务）。
3. `RetailMetadataEquivalenceGateTest` 的 `catalog.size() > 4000` 是迁移进度下限，M4-b 后按 3866 下移到 3000
   （精确覆盖仍由 `catalog + RetiredQuestIds == 6224` 恒等式保证）。

## 6. 全树对账

- 命令：`mvn -o -Dtest='com.aionemu.gameserver.questEngine.**' test`（`gates/m4b-questengine-run1.log`）
- 结果：**1939 例 / 28F+7E = 35**，与干净 HEAD 基线（`gates/head-baseline-failures.txt`，35 条）**逐方法一致**：
  `only-now = 0`、`only-baseline = 0`（含参数化用例 `QuestRetailCollectionRoleAlignmentTest(CollectionCase)[2,5..10]` 同集合）。
- 新增可驱动家族后用例数 1933 → 1939（+`RetailCombineTaskGateTest` 3 例等），失败集合**未增长**。

## 7. 未验证 / 风险

- **未验证**：服务端重启、实机客户端（需用户执行）——本族的可见行为是合成窗口与任务书，改动只有"定义来源"。
- 风险：真端表的 `product` 多产物 / `give_component` 缺项未出现（574/574 单产物且至少 1 分量），
  编译器已用稳定码 `RETAIL_COMBINE_PRODUCT_SHAPE` / `RETAIL_COMBINE_COMPONENT_UNRESOLVED` 兜住。

## 8. 下一步

SimpleCollectItem 178 → SimpleUseItem 104 → SimpleItemPlay 15 → SimpleSerialHunt 10 → DataDriven 1508
（按同一流程：表入仓 → 合成器 → 家族门禁 + 冻结指纹 → 退役 XML → 聚焦 + 全树对账）。
