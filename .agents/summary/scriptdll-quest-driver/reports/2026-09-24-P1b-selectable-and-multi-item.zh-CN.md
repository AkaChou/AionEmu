# P1b：可选奖励 25 行 + 多交付物 24 行落驱动（SimpleCollectItem 全族收口）

> 口径：真端优先。接续 P1a，本切片把最后两批稳定码拒绝（`RETAIL_COLLECT_SELECTABLE_REWARD` /
> `RETAIL_COLLECT_ITEM_SHAPE`）落进编译器并退役。**SimpleCollectItem 178 行自此 100% 由真端驱动。**

## 1 交付

**生产代码（`RetailSimpleCollectItemDefinitionCompiler`）**
- **可选奖励**：不再按稳定码拒绝，改为按 `npc-complete` 的 choice 展开口径合成——第 k 个可选项绑确认动作
  `8+k`，动作为固定奖励 + 该项 + `CompleteQuest(0)`，与 XML `<choice action="SELECTED_QUEST_REWARDk"
  reward-index="N">` 逐一对齐（1579：2 选项→8/9；2329：5 选项→8..12）。XML `choice=0` 的行判为 XML 漏配，
  按真端元数据补齐（真端 quest.xml 的奖励 kind 是权威）。选择数 >16（确认区间容量）或多奖励档仍拒绝。
- **多交付物**：交付检查对（动作 39 / 20002）按真端 `itemRequirements` 整组检查/扣除
  （`has-item`×N + `remove-item`×N），与 XML 的多条同构（11004：7×+3× 双交付物一次交）。
- `precheck` 码义更新：`RETAIL_COLLECT_ITEM_SHAPE` = 真端交付物为空；`RETAIL_COLLECT_SELECTABLE_REWARD`
  = 可选项超 16 或多奖励档。两码在本族 178 行上**双双归零**。

**门禁（`RetailSimpleCollectItemGateTest`）**
- `hasReport`/`itemCheckPair` 改为整组交付物断言；`hasComplete` 改为"确认 id 集合精确等于期望集"
  （无可选 = 8..23 全覆盖；有可选 = 8..8+n-1）；下限 129/126 → **178/175**。
- `LegacyTemplateMirrorRouteRegressionTest`：3096 的交付镜像按客户端模板索引从采集对象 700423..426
  改为报告 NPC 798225（Pyrrha，start/end 同体）——旧 XML 把检查挂采集对象属历史错误，
  与 M5-b3 的 2527（700328→204811）同判。

**清单与登记（机器生成）**
- `retail-simple-collect-item-drift.tsv` 重算：accepted 129 → **178**（REJECTED 双码 0）。
- 新裁定 `p1b-collect-choice-decisions.tsv`：**49 行 ADOPT_RETAIL**
  （25 `SELECTABLE_CHOICE_EXPANSION` + 24 `MULTI_ITEM_HANDIN`；`p1b_build_choice_decisions.py`）；
  `build_retention_list.py` 三份裁定文件合并消费。
- `retail-xml-retention.tsv`：RETAIL_TABLE/OK 2484 → **2533**；退役 **49** 个 XML（catalog 3740 → **3691**）；
  冻结指纹 126 → **175**。

## 2 证据

- 漂移重算：`mvn -o test -Dtest=RetailSimpleCollectItemGateTest -Dretail.collect.equivOut=...`
  → 49 行 `REJECTED:*` 清零、全部转 `DIFF:*`；`acceptedDefinitionsCarryRetailSemantics`
  （178 行全族语义断言，含新 choice/多交付物不变量）绿。
- 裁定：`python3 -B p1b_build_choice_decisions.py` → `decisions=49`。
- 退役：`m5b3_retire_collect_item_xml.py` → `moved=49`；`verify_retirement.py` →
  `catalog=3691 directory=3691 retired=2533 sum=6224 — OK`；doc links `replaced=49 dangling=0`；
  合同台账复跑（cap 例外 38 行不变）。
- 门禁：T1 七类 **25/25 绿**；T2（49 id）首轮 **1F**（`LegacyTemplateMirrorRouteRegressionTest` 3096
  镜像=旧 XML 形状）→ 按模板索引修正镜像后 **69/69 绿**；T3 全树见 §5。

## 3 结论

- **可删 XML 数 = 49**。五条充分条件逐条满足：元数据对拍一致；进度事件与 DLL 语义一致
  （家族动作码 open 31 + check 39 全族 178 行零例外——交付轴由客户端任务书 select5 实证）；合成 IR 与
  npc-item-report/npc-complete 展开口径同构（差异全部落在 XML 历史路由变体，如 14120 多驱动真端表
  未声明的对象 730020 + 1353/1352 续页，M5-b3 批次 2 同判）；家族门禁绿；无未闭环口径冲突。
- **SimpleCollectItem 全族收口**：178 行 = 178 可驱动（RETAIL_TABLE）+ 0 稳定码拒绝。
  族内累计退役 = 29 + 57 + 20 + 20 + 49 = **175**，另 3 行（1137/2237/28503）为 M5-b3 判定的真端缺口，
  已在保留清单（SEMANTIC_GAP），保留合计 178。

## 4 对拍结果

- **元数据**：全树元数据门禁绿（`RetailMetadataEquivalenceGateTest` 于 T3 内）。
- **进度事件（DLL）**：交付检查 39/20002 与家族签名一致；choice 确认 8..23 与客户端奖励窗按钮对应。
- **IR**：49 行 DIFF 轴全部落在既有词表（ROUTE / REWARD_ROW / VAR0_FIELD / DIALOG_10/1008/1009/1012/39/20002/OTHER），
  逐行裁定入档；冻结指纹 175 行守等价证据。
- **客户端**：LegacyTemplateMirrorRouteRegressionTest 按模板索引复绿（3096 镜像归位报告 NPC）；
  QuestClientContractGateTest 绿。

## 5 未验证 / 门禁

- T3 全树（forkCount=2）：首轮 1947 例 / **1F**（`QuestRewardItemGateTest.selectableItemsMatch...`
  27 行 production selectable=[]：生产视图解析器只认 kind=="ITEM"，未计 choice 路由上的
  `SELECTABLE_ITEM` 发放）→ 修 `parseRetailProduction`（choice 确认路由的 SELECTABLE_ITEM 发放计入
  可选集合）+ 重跑 `build_item_selectable_contract_tsv.py` 基线后 3/3 绿；
  T3 复跑 **1947 例 / 0F / 0E / 1 skipped / BUILD SUCCESS / 357 s**（`gates/T3-072852.log`，
  唯一 skip = 既有 `QuestDialogMigrationEquivalenceTest`）。
- 实机验收：可选奖励选择窗（8..12 按钮位）与多交付物一次交付的客户端表现，待用户执行。

## 6 阻塞与决策项

| 项 | 处理（不停等） |
|---|---|
| SystemGrant 分发接线（M5-b3x 起挂起） | 等实机抓包；不阻塞 |
| 1137/2237/28503 真端缺口 | 已在保留清单（M5-b3 判定），随终局报告交付 |

## 7 下一步

SimpleCollectItem 族收口完成。按 §4 队列推进 P2（`_area_` 数据缺口：SimpleHunt 21 行按真端世界文件重算
`ai-areas.xml`，10 GB 扫描单独切片）或 P3（SimpleUseItem 104 → SimpleItemPlay 15 → SimpleSerialHunt 10，
同一流水线）。复现：`mvn -o test -Dtest=RetailSimpleCollectItemGateTest`。
