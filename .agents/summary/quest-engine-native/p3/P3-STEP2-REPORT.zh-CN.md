# P3 步骤 2 报告：SimpleTalk 原生直驱切换（物品面 + 接线 + 旧编译入口切断）

> **批次**：P3 SimpleTalk（计划 §7）。**范围**：`Quest_SimpleTalk.xml` 3152 行。
> **时点**：2026-10-01。**状态**：🟡 代码已落地（原生直驱 + 物品面 + 旧 IR 入口切断）；
> 残余 = 旧金标重锚 34 类（`gates/2026-10-01-red-attribution-p3.zh-CN.md`）+ 客户端验收 `PENDING_CLIENT`。
> **授权**：用户 2026-10-01「完全按真端方式来，理论上不需要再询问我，授权 maven 和提交」。

## 1. 本步交付（真端事实 → Java 面）

| 面 | 真端证据 | 实现 |
|---|---|---|
| 接取（cab520） | 1002→`SetQuestAcquired`+页 1003；20000→acquired+状态包+`GiveItem(0x410)`；1003→页 1004（`f731:1692-1737`） | `SimpleTalkHandler.onDialog` 接取段：31/26→页 4；1002/20000→`QuestService.startQuest`（20000 追加 `give_item`）；1003/1004/20001→页 1004 |
| 中继（cabb10） | `10000/10001/10002 → SetQuestProgress(+0xf0)(quest,1/2/3) + 状态包 + GiveItem + RemoveItem`；页 1352/1693/2034（`f731:1915-1945`） | 中继段：仅当 `vars==step-1` 才写 `+0xf0`（乱序/重复零副作用），随后按同样顺序发放 `give_itemK`、扣除 `remove_itemK` |
| 报告与领奖 | `1009`：`phase==finalStep` → `mgr+0x1c8` 奖励窗；`caad20` 完成门 `vars==param_4`（`f731:1394/1954`） | 报告段：`vars>=relayCount` 且交付门通过 → 扣交付物 + `REWARD` + 页 5；否则页 10。领奖段：8..23/108/110+k → `QuestService.finishQuest` + 页 1008 |
| 物品（give/remove） | 1131 逐字节对拍：cab520 发 `182200506`（1131A）；cabb10 发 `182200507`（1131B）+ 扣 `182200506`（`SC:2321854`、`SC:2374739/2388429`） | `NativeInventoryPort`（唯一出口：`count/give/remove`）；`ItemServiceInventoryPort` 生产实现 |
| 交付门（item_check） | 1988 行 `item_check=1`；真端 quest.xml `collect_item1..N` / `quest_work_item1..N` 为工作物品通道 | 门物品解析：`collect_item1..N` → `quest_work_item1..N`（首项）→ 表内发放符号；解析失败 fail-closed（报告门不放行） |
| 物品符号 → id | 表用 `ITEM_...` 符号，物品模板 `name_desc` 去前缀形（`ITEM_QUEST_1131A` ↔ `quest_1131a` = 182200506） | `RetailItemNameIndex.loadItemTemplates()`（与旧车道**同一份**装载路径，旧车道 `listXmlNames` 去重迁移） |
| 启动注册 | codegen 静态注册表（b5920 + 槽表） | `SimpleTalkHandler.installInterest(QuestEngine)`：接取 NPC（start+talk）/交付 NPC（talk）/中继 NPC（talk） |
| 引擎分流 | 表驱动车道 | `QuestEngine.hasMatchingRoutes`/`onDialog`/`load` 三入口 + `RetailQuestDriver` 切断 `retailOwnedTalk` 注册（3152 行移出旧 IR） |

## 2. 表行模型修正（真端分槽）

`SimpleTalkRow` 由「give/remove 扁平列表」修正为**真端分槽**（否则接取发放与第 K 步发放不可区分）：

| 列 | 语义 | 实测 |
|---|---|---|
| `give_item` | 接取侧 20000 发放 | 65 行（item_check 行）/ 全表 548 个 give 单元 |
| `give_item1..3` | 第 K 中继步发放（下标对齐 `talk_npc1..3`） | 79/43/19 行 |
| `remove_item1..3` | 第 K 中继步扣除 | 69/25/21 行 |

单元格式实测：663 个 give/remove 单元 **全部**为 `NAME COUNT`（count 恒为数字），非该形 fail-closed。

## 3. 未解析面冻结（证据快照）

| 快照 | 口径 | 数量 |
|---|---|---|
| `p3/simple-talk-unresolved-npcs.tsv` | NPC 名唯一解析失败集合（哨兵 7 / 复合单元 6 / 测试行 3 / 数据缺口 26） | **39 名**；接取列 207 行、交付列 86 行；接取可解而交付缺口的行 9 |
| `p3/simple-talk-unresolved-items.tsv` | 物品面：交付门 `GATE_GAP` 11 行 + 部分缺口 `GATE_PARTIAL` 3 符号 | **14 项**（1988 个 item_check 行中 1977 行门成立） |

归因：两组均为**本服静态数据缺口**（NPC/物品模板缺该 `name_desc`），非表语义或分派缺陷；
老链路对这些行同样是拒绝/静默跳过（`RETAIL_ITEM_CHECK_UNRESOLVED`、`acceptGiveItemActions` 空表），
故原生车道与老链路行为等价（fail-closed）。补齐静态数据后该白名单须逐条归零。

## 4. 门态（可复跑）

| 门 | 结果 |
|---|---|
| `SimpleTalkNativeFamilyGateTest` | **7/7**（3152 行归属 + 39 未解析名 + 207/86 行冻结 + 哨兵行数 98/75/8 + 中继索引与页阶梯 + cab520 接取 + cabb10 步进幂等/乱序零推进 + 报告门 + 物品面 14 项冻结 + 步进发放/扣除非真端顺序 + 交付门持有门） |
| `NativeQuestTableLoaderTest` | 10/10（含 SimpleTalk 分槽断言） |
| tablelane 套件 | **69/69** |
| 启动与派发门禁 | 27/27（`QuestProductionStartupGateTest` 2/2、`QuestEngineRuntimeCompositionTest` 13/13、`QuestEngineNpcDialogDispatchTest` 6/6、`QuestEngineEscortAndProximityRegistrationTest`、`RetailQuestDriverOverlayTest` 5/5） |
| 聚焦套件 `*Quest*Test,*Retail*Test` | 1678 例 / 164F+226E / 152 类红；其中 **34 类为本次新增**（旧金标重锚），基线 118 类债 `REMOVED 0`（`gates/2026-10-01-red-attribution-p3.zh-CN.md`） |

## 5. 残余与下一步

1. **旧金标重锚（34 类，P3 批次内）**：按 §8.9 以真端表行 + 客户端页动作重写；禁止为兼容而保留旧 IR 形状。
2. **同批删旧（死码）**：`RetailQuestDriver.compileSimpleTalk`、`RetailSimpleTalkTable`、`RetailSimpleTalkDefinitionCompiler`
   与 `src/main/resources/quest/quest_client_accept_npc_sets.tsv` / `quest_client_handin_npc_sets.tsv` 的消费面，
   须与第 1 项**原子删除**（这些金标仍在读旧编译器输出，先删代码会让测试无法编译）。
3. **静态数据缺口 14 项**：补齐物品模板 / NPC 模板后重跑冻结断言并归零白名单。
4. **客户端验收**：至少一条代表任务（建议 1131：接取发放 + 步内发/扣 + 交付门）真实客户端跑通；未跑则 P3 `PENDING_CLIENT`。
5. `con_quest`（492 行）与 cutscene 只装载未接线，属 P3 后续步骤（真端语义：链式接取窗 + `PlayMovie`）。
