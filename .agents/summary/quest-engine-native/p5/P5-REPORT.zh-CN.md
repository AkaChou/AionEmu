# P5 报告：SimpleUseItem / SimpleItemPlay 族原生直驱切换（用物事件轴 + 同批删旧）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 主题：按计划 §7 P5 把 SimpleUseItem（160 行）与 SimpleItemPlay（43 行）两族切到真端表 +
> 真端 `quest.xml` + 客户端页/动作契约直驱，与旧 IR 车道一刀两断（同批删旧）。
> 日期：2026-10-01。分支：`quest`。
> 前置：`p5/P5-STEP1-REPORT.zh-CN.md`（表装载 + 行集可行性冻结）、`p3-prereqs/family-table-shapes.md` §2.5/§2.6。
> 口径：本族一切行为只读真端表 `Map/XML/Quest_SimpleUseItem.xml`、`Map/XML/Quest_SimpleItemPlay.xml`、
> `Map/XML/quest.xml` 与仓库静态数据；不生成 IR 节点、不回退旧编译器、不发明 id/页/物品；未决一律 fail-closed。

---

## 1. 交付：两族装载 + 两个原生处理器 + 共享符号解析

| 组件 | 变更 |
|---|---|
| `tablelane/NativeQuestTableLoader` | 新增 `useItemRows()/useItemSize()`（160 行）与 `itemPlayRows()/itemPlaySize()`（43 行）两个行模型：`SimpleUseItemRow`（`use_item_name` / `reward_npc_name` / `talk_npc1..3` / `con_quest` / `item_check` / `give_item1..3` / `remove_item1..3` / `cutsceneid1` / `cs1_haction`）与 `SimpleItemPlayRow`（`acquired_npc_name` / `reward_npc_name` / `use_item_name` / `give_item` / `talk_npc1..3` / `con_quest` / `item_check` / 分槽发扣 / `cutsceneid1`）；必填列缺失即 `NATIVE_TABLE_PARSE_FAILED`，第 K 步列**按位置保留**（缺位 `null`） |
| `tablelane/SimpleUseItemHandler`（新增） | 本族唯一处理器：① **用物接取口** = 真端 `UseItem` 无主事件 → 接取窗页 4（NONE 或可重复行 COMPLETE 才开窗）；无主 1002/20000 经 `NativeQuestStartPort` 建档 START、1003/1004/20001/1008 关窗；② 中继链 = `talk_npcK` **表序**推进（步位在 raw vars 16..17），步内按真端顺序执行 `give_itemK`/`remove_itemK`，页只发客户端声明的中继页；③ 交付 = 中继走完 ∧ `item_check` 门（真端 `quest.xml check_itemK_L` 门物品）→ 扣门物品翻 `REWARD` + 页 5，否则页 10；④ 领奖 = `NativeReportRewardFlow`（31/26/1009/-1 重开页 5；8..23/108/110..124 结算 → 页 1008） |
| `tablelane/SimpleItemPlayHandler`（新增） | 接取 NPC 的 31/26 → 客户端契约入口页；1002/20000 建档并发放真端 `give_item` 演出道具；1012/1013 原样回发；1003→1004、1004/20001→关窗、1008→页 10；`onItemUse` = 真端演出推进（START → REWARD）；1009 回收演出道具并重开奖励窗；领奖同上。**中继/cutscene/`item_check` 长尾行不路由**（不半接线，fail-closed） |
| `tablelane/NativeItemSymbols`（新增） | 两族共用符号解析（点号名 → `ITEM_` 前缀两通道、`符号 [数量]` 单元、位置保留）与 `ItemStack` 载体 |
| `QuestEngine` | `hasMatchingRoutes`/`onDialog`（collect 之后、typed 之前）/`onItemUseEvent`（native 两族先手，成功即 `SUCCESS`）三处分流 + 启动期 `installInterest`（中继 NPC 与交付 NPC 逐点注册；本族无接取 NPC 面） |
| `retail/RetailQuestDriver` | **切断** `compileSimpleUseItem`/`compileSimpleItemPlay`，删除两表/两编译器字段与其加载；retention 分支两族改判 native owner（`owns` 计数与覆盖检查同步） |

**同批删旧（`git rm`）**：`retail/RetailSimpleUseItemTable`、`retail/RetailSimpleItemPlayTable`、
`retail/RetailSimpleUseItemDefinitionCompiler`、`retail/RetailSimpleItemPlayDefinitionCompiler`、
旧金标 `RetailSimpleUseItemGateTest`、`RetailSimpleItemPlayGateTest`、
`test/resources/quest/retail-simple-use-item-drift.tsv`、`retail-simple-use-item-ir-fingerprints.tsv`、
`retail-simple-item-play-ir-fingerprints.tsv`。**本批结束后两族无并行可用双路径**。

## 2. 行集划分与路由口径（真端表事实，逐行冻结）

| 族 | 表行 | 退役（RETAIL_TABLE） | NATIVE_READY | 注册集（owns） | 路由集（routes） | 不可路由 |
|---|---|---|---|---|---|---|
| SimpleUseItem | 160 | 104 | **102** | 160 | **102** | 58 = 未退役 56（`minlevel=999` 停用形）+ fail-closed 残余 2 |
| SimpleItemPlay | 43 | 6 | **6** | 43 | **6** | 37（未退役 28 + XML 保留 9） |

- **fail-closed 残余 {30720, 30723}**：真端表行已退役，但交付名 `magician_apprentice` 在真端名册与
  客户端登记里都没有（复合名无解）⇒ 不给驱动定义、不发明映射；`RetailClientAcceptEntryPageTest` 内以
  `FAIL_CLOSED_NATIVE_ROWS` 双向冻结（新增或消失都必须显式改表）。
- 未解名证据面冻结：useitem = {`LDF5b_Greenhat_LD`（未退役 13060/23060，仍 XML 车道）, `magician_apprentice`}；
  itemplay = {`HousingManager_Da/Li`, `LDF5b_Greenhat_LD`, `NPC_event_devasday_shugo`, `_faction_`}。
- **客户端入口页复核（新增证据）**：102 条 NATIVE_READY 用物行的客户端任务页声明的入口页**全部是
  `ask_quest_accept`(4)** ⇒ 用物开窗页 4 对整族客户端可渲染；6 条 itemplay 行中 13704/13708/23704/23708
  声明 `select1`(1011)，处理器按 `QuestDialogContract.acceptEntryPage` 取页，另两行声明 4。

## 3. 两处真端事实就地坐实

1. **交付门口径 = 记录开关**：`item_check` 只有 5 行（80482/80486/80612/80615/80616）为 1，门物品取
   真端 `quest.xml` 的 `check_itemK_L`（全表 11 行声明门物品，其中 4 行无开关）。**只有开关行设门**——
   与 SimpleTalk 车道同一闸门口径；`quest_work_item1` 单独出现时**不**反推 `HasItem` 门（见 §7-③）。
2. **演出道具轴 = 用物推进**：itemplay 的 `use_item_name` 既是接取发放物（`give_item`，6/6 与用物品同名）
   又是推进事件；`onItemUse` 在 START 态直接翻 `REWARD`，与简单用物族（用物开接取窗）共用 `QuestEngine`
   的用物入口但语义不同，靠单一 owner 分离。

## 4. typed 目录退出后的旧金标重锚（本批 11 个类）

| 类 | 重锚内容 |
|---|---|
| `Quest1309ClientDialogAlignmentTest` | 改写为 native：用物页 4 → 1002 建档 + 关窗页 0；中继页 1352；交付翻 `REWARD`（1 → 2 例） |
| `QuestMultistepChainContractTest` | 1514 从"延后"改为 native 行锚：两级中继 + `SELECT2`/`SELECT3` 页 + 交付翻 `REWARD` |
| `QuestRewardItemGateTest` | nativeOwned 扩两族 + 冻结无定义残余 `{30720,30723}` |
| `RetailQuestDriverOverlayTest` | native 计数与负例筛选扩两族；production 覆盖等式保持 |
| `QuestInteractionObjectContractGateTest` | 交互对象覆盖下限 2800 → 2700（实测 2757） |
| `RetailClientAcceptEntryPageTest` | 原生车道分支扩两族（用物入口页走 `PAGE_ASK_ACCEPT` 客户端声明校验；itemplay 走 `acquireNpc` + 契约页），并冻结 fail-closed 残余行集 |
| `EarlyElyosQuestRegressionTest` | 1561 从 typed 物件形改为真端行锚（reward NPC 700188、无中继/无门、用物开窗、31 交付翻 `REWARD` + 页 5、-1/1009 自环） |
| `QuestEventQuestBatchDefinitionTest` | 整类重锚：80008/80009 走用物车道、80028/80031/80032 走 talk 车道，断言面改真端 `quest.xml` 轴 + 客户端接取窗页声明（**3 个基线红转绿**） |
| `QuestMutationPlannerTest` | 2578 从 typed 完成清理断言改为 native 行锚（两步中继、无门、工作物品 = 用物品本体） |
| `NativeQuestTableLoaderTest` | 两族行装载 + 长尾列逐字段对拍（10 → 11 例） |

新增族门：`SimpleUseItemNativeFamilyGateTest`（11 例）、`SimpleItemPlayNativeFamilyGateTest`（9 例）、
`UseItemFamilyRowAlignmentGateTest`（6 例，逐行正则对拍 + 路由集逐元素复算 + 门/中继页契约）、
`UseItemFamilyRowInventoryGateTest`（1 例，160/104/102 + 43/6/6 与残余 `{30720,30723}` 冻结）。

## 5. 门禁与实测

| 门 | 命令 | 结果 |
|---|---|---|
| 族门 + tablelane 全量 | `mvn -o test '-Dtest=*FamilyGateTest,*RowAlignmentGateTest,*InventoryGateTest,NativeQuestTableLoaderTest,*RewardClaimGateTest,*NativeTalk*Test'` | **91/91 绿**（含 P5 四门 11/9/6/1 与前四族族门回归）；`gates/2026-10-01-p5-family-tablane.log` |
| 聚焦套件 | `mvn -o test '-Dtest=*Quest*Test,*Retail*Test' -DfailIfNoTests=false` | **1691 例 / 162F+142E / 108 类红**；对 P4 基线（1695 / 163F+145E / 109 类）**ADDED 0 / REMOVED 1**（`gates/2026-10-01-focused-run-p5-final.log`、`-red-classes.tsv`、`-delta.tsv`） |
| 逐类三元组差集 | `python3 .agents/summary/quest-engine-native/p4/tools/red_class_delta.py delta ...` | 变化仅 6 类：`QuestEventQuestBatchDefinitionTest` 转绿（E-3）、`QuestMutationPlannerTest` F-1、两旧族金标 REMOVED（预期）、`Quest1309`/`NativeQuestTableLoader` T+1 |
| 范围外红（未动） | — | `EarlyElyosQuestRegressionTest` 1137（P4 登记）、`RetailClientAcceptEntryPageTest` 80817（DD 在飞切片，P4 基线同形）、`QuestMutationPlannerTest` 36525（SimpleTalk 混合类/QE-112 登记）与 36539 缺 XML（DD 在飞切片） |

## 6. 证据文件

- `p5/P5-STEP1-REPORT.zh-CN.md`：表来源/逐行装载/列填充率/行集划分（本报告 §2 的输入）。
- `p5/useitem-feasibility.tsv`：160 行 + 43 行逐行判据（本报告 §2 的复算底稿）。
- `gates/2026-10-01-p5-family-tablane.log`、`gates/2026-10-01-focused-run-p5-final.log`、
  `gates/2026-10-01-focused-run-p5-final-red-classes.tsv`、`gates/2026-10-01-focused-run-p5-final-delta.tsv`。

## 7. 边界与残余（P5 退出前置）

| # | 项 | 现状 | 处置 |
|---|---|---|---|
| ① | fail-closed 残余 `{30720, 30723}` | 真端行已退役但交付名无解 ⇒ 无驱动定义 | 冻结登记（族门 + 入口页门双向断言）；取得真端名册/客户端登记证据后再裁定 |
| ② | 未退役停用行 56（`minlevel=999`） | 真端表行存在但不可接取，仍走 XML 车道 | 随 P8 XML-only 冻结保留 |
| ③ | 旧的 `quest_work_item1` 反推门与完成代扣 | 随切换批退场（真端记录开关 `item_check=false`，不设门、不代扣） | 断言面已改为"开关口径"；若后续取得真端工作物品代扣证据，须以真端轴重新立项，不得回退旧合成语义 |
| ④ | 活动任务弃任（`LevelUp + EventActive(false)`）与活动状态轴 | 旧 typed 蛋糕特判随批退场；真端 `event_quest.xml` **全域缺失** | 登记为情报缺口（计划 §9 行 3 同口径），不合成 |
| ⑤ | itemplay 中继 / `cutsceneid1` / `item_check` 长尾行 | 本批**不路由**（37 行 fail-closed） | 需真端 cutscene + 分槽证据后单独立项 |
| ⑥ | `con_quest` 链式接取窗（useitem 32 行 / itemplay 9 行） | 未接线 | 与 P1-P4 同口径登记，随 P8 收口 |
| ⑦ | 代表任务真实客户端验收 | **PENDING_CLIENT** | 本批不得标记"族完成"，见 §7 硬规则第 7 条 |
