# P5 步骤 1 报告：SimpleUseItem / SimpleItemPlay 表装载 + 行集可行性冻结

> 主题：计划 §7 P5 批门第 1 条（本族表行清单、来源 hash、NATIVE_READY 行集与未证行集）+ 第 2 条输入。
> 日期：2026-10-01。分支：`quest`。前置：`p3-prereqs/family-table-shapes.md` §2.5/§2.6。
> 口径：只读真端表 + 仓库静态数据，不发明语义；**本步零切换**（旧编译器与旧金标仍在，行为不变）。

---

## 1. 表来源与逐行装载（本步交付）

| 表 | 根元素 | 行数 | 入仓副本 | 真端源 | 内容一致性 |
|---|---|---|---|---|---|
| `Quest_SimpleUseItem.xml` | `quest_simpleuseitems` | **160** | `src/main/resources/aion/data/static_data/quest/retail/`（UTF-8） | `<真端根>/Map/XML/`（UTF-16） | 逐行逐列对拍一致，唯 4042 的 `dev_name` 内换行 `\r\n`→`\n`（文本规范化，非语义差异） |
| `Quest_SimpleItemPlay.xml` | `quest_simpleitemplays` | **43** | 同上（UTF-8） | 同上（UTF-16） | 逐行逐列**完全一致** |

`NativeQuestTableLoader` 新增两族行模型与装载（逐行原文，缺必填列即 fail-closed）：

```java
record SimpleUseItemRow(int questId, String devName, String useItemName, String rewardNpcName,
        List<String> talkNpcNames, Integer conQuest, boolean itemCheck, List<String> stepGiveItems,
        List<String> stepRemoveItems, Integer cutsceneId, Integer cutsceneAction)
record SimpleItemPlayRow(int questId, String devName, String acquiredNpcName, String rewardNpcName,
        String useItemName, List<String> talkNpcNames, Integer conQuest, boolean itemCheck,
        String acceptGiveItem, List<String> stepGiveItems, List<String> stepRemoveItems,
        Integer cutsceneId, Integer cutsceneAction)
```

必填面（缺失即 `NATIVE_TABLE_PARSE_FAILED`）：useitem = `use_item_name` + `reward_npc_name`（真端 160/160）；
itemplay = `acquired_npc_name` + `reward_npc_name`（真端 43/43）。两族均**没有** `device`/`emotion` 类新轴。

## 2. 列填充率（复算，与 §2.5/§2.6 一致）

| 列 | SimpleUseItem(160) | SimpleItemPlay(43) |
|---|---|---|
| `use_item_name` | 160 | 41 |
| `acquired_npc_name` | **0（本族无此列）** | 43 |
| `reward_npc_name` | 160 | 43 |
| `talk_npc1 / 2 / 3` | 54 / 26 / 10 | 11 / 6 / 0 |
| `con_quest` | 32 | 9 |
| `give_item` | **0（本族无此列）** | 34 |
| `give_item1 / 2 / 3` | 8 / 4 / 3 | 7 / 5 / — |
| `remove_item1 / 2 / 3` | 10 / 6 / 3 | 1 / 4 / — |
| `item_check` | 5（80482/80486/80612/80615/80616） | 0 |
| `cutsceneid1` | 0 | 2 |

**第 K 步列对齐（关键事实）**：两族凡声明 `give_itemK`/`remove_itemK` 的行**都**声明了第 K 个中继 NPC
（useitem 8/4/3 + 10/6/3 行全部如此；itemplay 7/5 + 1/4 行全部如此）⇒ 与 SimpleTalk 同源 codegen 的
「`talk_npcK` 第 K 步 + 该步发/扣物品」槽语义；装载器按位置保留（缺位 = `null`），不在装载层发明计数。

## 3. 行集划分（owner × 可行性）

| 族 | 表行 | 退役（owner=RETAIL_TABLE） | XML-only（登记保留） | 未登记（既不退役、无 XML） | **NATIVE_READY** |
|---|---|---|---|---|---|
| SimpleUseItem | 160 | **104** | 0 | 56（`minlevel=999` 停用形） | **102** |
| SimpleItemPlay | 43 | **6** | 9 | 28 | **6** |

- NATIVE_READY 判据（探针内独立复算，非读 handler）：退役 ∧ 非 XML-only ∧ 接取 NPC 唯一解析 ∧
  交付 NPC 唯一解析或客户端交付集合非空 ∧ `use_item_name` 走**两通道**（先原名、未命中再去 `ITEM_` 前缀；
  单元先剥 `符号 [数量]` 的计数位）可解 ∧ 中继 NPC 可解 ∧ 第 K 步发/扣物品可解 ∧ 真端 `quest.xml`
  元数据可编译（`clean()`）。
- fail-closed 残余（冻结）：SimpleUseItem **{30720, 30723}**（`magician_apprentice` 复合交付名、客户端无登记，
  与 P4 39611/49611 同类）；两族其余行均因「未退役 / XML-only」不进本批路由。
- 冻结门：`UseItemFamilyRowInventoryGateTest` **1/1 绿**（160/104/102 + 43/6/6 + 残余集），
  证据 TSV `p5/useitem-feasibility.tsv`（重生成：`-Dp5.out=<path>`）。

## 4. 语义输入（步骤 2 的实现依据，均为真端声明面）

| 面 | SimpleUseItem | SimpleItemPlay |
|---|---|---|
| 接取 | **使用道具** `use_item_name`（本族无接取 NPC） | `acquired_npc_name` 对话 31 → 入口页 → 1002/20000 接取 |
| 接取发放 | 无 `give_item` 列 ⇒ 不发 | `give_item`（接取即发**演出道具**，34 行；6 个 NATIVE_READY 行全部声明） |
| 推进 | 中继链 `talk_npcK` 严格表序 + 第 K 步 `give_itemK`/`remove_itemK`（1559 第 3 步换物、1718 三步换物为实测样本） | 使用 `use_item_name` 道具一步推进（本批 6 行无中继链、无 `item_check`） |
| 交付 | `reward_npc_name`；`item_check` 5 行 = 交付门（80482/80486/80612…） | `reward_npc_name`（6 行中 5 行接取 NPC = 交付 NPC） |
| 领奖 | `NativeReportRewardFlow`（领奖段与 P1-P4 同口） | 同左 |

## 5. 门态（本步）

| 门 | 命令 | 结果 |
|---|---|---|
| 装载器/表族门（含 P5 新行模型） | `mvn -o test -Dtest=NativeQuestTableLoaderTest,NativeQuestXmlTableTest,TableSourceProvenanceGateTest` | **17/17 绿**（Hunt 1863 行、Serial 16、Talk 3152、Collect 262、UseItem 160、ItemPlay 43 同批装载） |
| P5 行集冻结门 | `mvn -o test -Dtest=UseItemFamilyRowInventoryGateTest` | **1/1 绿** |

## 6. 残余与下一步

1. **步骤 2**：`SimpleUseItemHandler` / `SimpleItemPlayHandler` + `QuestEngine` 入口分流 + `RetailQuestDriver`
   切断 `compileSimpleUseItem`/`compileSimpleItemPlay`，同批删 `RetailSimpleUseItemTable`/`RetailSimpleItemPlayTable`
   与两族旧金标（`RetailSimpleUseItemGateTest` / `RetailSimpleItemPlayGateTest`），并逐行重锚到 native 面。
2. **未证面（步骤 2 须先坐实再接线）**：`con_quest`（useitem 16 行 / itemplay 0 行退役面）与 `cutsceneid1`
   （itemplay 2 行，均**未退役**，本批不接）；两族 `give_itemK/remove_itemK` 的**页/对话落点**仍以表列语义为准，
   客户端验收 `PENDING_CLIENT`。
3. **不在本批**：QE-112 在飞切片（`RetailSimpleHuntDefinitionCompiler` 等）与 P6/P7 范围。
