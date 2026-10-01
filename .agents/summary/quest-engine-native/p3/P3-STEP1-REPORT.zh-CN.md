# P3 SimpleTalk 第一步：真端表行模型入原生装载器

> 批次：P3 SimpleTalk 原生切换批 / 步骤 1（表侧）。**零切换、零旧路径改动**：本步只把真端表
> `Quest_SimpleTalk.xml` 接进原生车道并加门，`RetailQuestDriver.compileSimpleTalk` 一字未动。
> 授权：用户已就该计划全程授权测试。

## 1. 表侧事实（先量后写，来自入仓表原文）

`src/main/resources/aion/data/static_data/quest/retail/Quest_SimpleTalk.xml`（root `quest_simpletalks`，3152 行）：

| 列 | 行数 | 比例 | 处置 |
|---|---:|---:|---|
| `acquired_npc_name` | 3152 | 100% | 必填，缺即 fail-closed |
| `reward_npc_name` | 3152 | 100% | 必填，缺即 fail-closed |
| `dev_name` | 3131 | 99% | 可选（仅诊断） |
| `item_check` | 1988 | 63% | 布尔（值恒为 `1`）：交付前须持有工作物品 |
| `con_quest` | 492 | 16% | 链式接取窗的下一条任务 id |
| `talk_npc1/2/3` | 468 / 193 / 68 | 15%/6%/2% | 中继链；实测**无 talk_npc2 无 talk_npc1 的行** |
| `give_item` | 407 | 13% | 接取发物（原文 `NAME COUNT`） |
| `give_item1/2/3` | 79 / 43 / 19 | | 链步发物 |
| `remove_item1/2/3` | 69 / 25 / 21 | | 链步扣物 |
| `cutsceneid1` + `cs1_haction` | 67 / 65 | 2% | 过场与对应动作 |

## 2. 交付

`NativeQuestTableLoader`（tablelane）新增：

- `SimpleTalkRow(questId, devName, acquiredNpcName, rewardNpcName, talkNpcNames, conQuest, itemCheck,
  giveItems, removeItems, cutsceneId, cutsceneAction)`；
- 装载链：`Talk` 表与 hunt/serial 同源（classpath 入仓副本），根标签 `quest_simpletalks` 校验，
  重复 id fail-closed，`give/remove` 按**原文**装载（不在装载层发明 count/物品 id 语义）；
- 访问面：`talkRows()` / `talkSize()` / `findTalk(id)` / `requireTalk(id)`（缺行 `NATIVE_TABLE_ROW_MISSING`）。

## 3. 门与读数

| 门 | 结果 |
|---|---|
| `NativeQuestTableLoaderTest.loadsFullSimpleTalkTable` | 3152 行、双 NPC 100%、468/1988/492 三列填充率对拍 |
| `NativeQuestTableLoaderTest.talkRowsKeepRetailTextForLongTailColumns` | 1131（give+remove+con_quest）/ 41536（三段中继+三发物）/ 1941（过场 93 + 动作 1009）逐字段对拍 |
| `NativeQuestTableLoaderTest.talkRowsFailClosedWhenAnNpcIsMissing` | 缺 `reward_npc_name`、非数字 `con_quest` 两类负例 |
| tablelane 套件 | **62/62 绿**（`mvn test -Dtest=com.aionemu.gameserver.questEngine.tablelane.*Test`） |
| 启动/派发回归 | **27/27 绿**（`QuestProductionStartupGateTest` 2、`QuestEngineRuntimeCompositionTest` 13、`QuestEngineNpcDialogDispatchTest` 6、`QuestEngineEscortAndProximityRegistrationTest` 1、`RetailQuestDriverOverlayTest` 5） |

## 4. 边界与下一步

- **未做**：`SimpleTalkHandler`（接取 cab520 / 中继 cabb10 / 报告 1009 / 领奖）、族级门禁、`compileSimpleTalk` 切断。
- **不阻塞**：本步不改 `RetailQuestDriver`，与在飞 QE-112 切片零交集。
- **下一步（P3 步骤 2）**：`SimpleTalkHandler` + 族级门禁 + 客户端页动作对拍（页 4/10/1011 + 链式 1352/1693/2034/2375/select5）。
