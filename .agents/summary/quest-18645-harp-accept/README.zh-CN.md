# 卡利加的竖琴架（730777）点击无法接取 18645《卡利加的收藏品（竖琴）》

> 状态：**实现完成，门禁已过（SimpleTalk 门禁 17/17、NPC 对话分发回归 6/6、探针全项符合预期），待实机验收（需重启加载）**
> 日期：2026-10-07
> 关联：quest 18645（Elyos）/ 28645（Asmodian 镜像）、物件 730777 `IDCromede_harp`、副本 300230000、SimpleTalk 原生车道

## 1. 报障与现象

用户报障：点副本内 NPC **730777（卡利加的竖琴架）**"应该可以领取任务"，实际"点击没有任务"（角色 Kk，**吟游星 48 级**）。

实机日志（`log/quests.log`，用户点击时）：

```
22:35:10 [S->C] SM_DIALOG_WINDOW 玩家=Kk targetObj=162092 questId=0 下发页=1011
22:35:12 [S->C] SM_DIALOG_WINDOW 玩家=Kk targetObj=162092 questId=0 下发页=1011
```

服务端**有响应**（`162092` = 730777 的实例对象），但下发的是**两参页 1011（questId=0）**——客户端的 AI 兜底页，客户端拿不到任务上下文，渲染不出接取对话 ⇒ 玩家看到"点物件没反应/没有任务"。

## 2. 任务与一族数据（真端 quest.xml 133919-133940）

| 物件 (npc id) | 任务 | 限定职业（`class_permitted` 有效项） | 兑换 |
|---|---|---|---|
| 730326 `IDCromede_sword` | 18618 长剑柜 | 战士系通用 | sword_n_u0_38a |
| 730327 `IDCromede_2hsword` | 18619 | 守护星 | 2hsword |
| 730328 `IDCromede_dagger` | 18620 | 杀星/弓星 | dagger |
| 730329 `IDCromede_polearm` | 18621 | **剑星** | polearm |
| 730330 `IDCromede_bow` | 18622 | **弓星** | bow |
| 730331 `IDCromede_mace` | 18623 | 守护/护法/治癒 | mace |
| 730332 `IDCromede_staff` | 18624 | 护法/治癒 | staff |
| 730333 `IDCromede_book` | 18625 | **魔道/精灵** | book |
| 730334 `IDCromede_orb` | 18626 | **魔道/精灵** | orb |
| 730335 `IDCromede_shield` | 18627 | 守护/护法/治癒 | shield |
| 730775 `IDCromede_gun` | 18643 | **枪炮星** | gun |
| 730776 `IDCromede_cannon` | 18644 | **枪炮星** | cannon |
| **730777 `IDCromede_harp`** | **18645** | **吟游星（bard）** | **harp_n_u0_38a** |

- 全部为 SimpleTalk 行（`retail-xml-retention.xml`：owner=RETAIL_TABLE、family=SimpleTalk）、`acquired_npc_name = reward_npc_name = 物件`（自接自交）、**无前置**、min-level 37、`collect_item1 = key_idcromede_05`（交付扣 185000102，卡利加掉落）。

## 3. 根因

**物件"打开/使用"（dialogId=-1=USE_OBJECT）在 SimpleTalk 接取面没有入口**：

1. 物件使用完成 → `QuestItemNpcAI2.handleUseItemFinish` → `selectDialog(USE_OBJECT / QUEST_SELECT, questId=0)` → 引擎 `onDialog`。
2. 引擎 `questId=0` 段的既有通道（2026-10-07 修复前）：采集族先手 / openDoorReplay（**仅玩家已有任务**）/ typed 候选 / DD 面——**SimpleTalk 是 `requestedOwner != 0` 才路由**，且 18645 不在 DD 的 `routedQuestIds`（DD 只服务"其余族 1467 行"）⇒ **无人认领**。
3. 落 `QuestItemNpcAI2` 兜底：`isDialogNpc()`（730777 模板 `<talk_info is_dialog="true"/>`）→ 发**两参** `SM_DIALOG_WINDOW(objectId, SELECT1)`——**无 questId**，客户端无法把它关联到 18645 的任务 html ⇒ 渲染不出接取对话（22:35 日志两行即此）。

真端语义（QE-070/QE-093 登记表裁定）：**物件的接取入口 = `USE_OBJECT` 自环 → 携带 questId 的入口页**（入口页/中转页属客户端合同，但**必须是任务页形态**）。

## 4. 修复（4 处）

| 文件 | 改动 |
|---|---|
| `questEngine/tablelane/SimpleTalkHandler.java` | ① 接取入口动作 `31/26` **扩展为 `31/26/-1`**（USE_OBJECT 物件打开走同一"资格判定 + `retailEntryPage + questId`"分支）；② 新增反向索引 `acquireQuestIdsByNpcId`（init 由 `acquireNpcIdsByQuestId` 反转）与 API `acquireQuestIdsForNpc(npcId)` |
| `questEngine/QuestEngine.java` | `onDialog` 的 `questId=0` 段新增：**物件族**（`QuestItemNpcAI2`/`QuestStartItemNpcAi2`）的 `-1/1001` 打开按 NPC 重放 SimpleTalk 接取行（`routes` 过滤；认领即 `env.setQuestId` 并短路）——**普通 NPC 不进入**（其打开由客户端本地对话框驱动：页 10 列表 → 31，行为不变） |
| `tablelane/SimpleTalkNativeFamilyGateTest.java` | 新用例 `objectOpenEntersTheAcquireFaceWithQuestContext`：物件打开 → **入口页 + questId**、不落库、非接取 NPC 零响应 |

**修复后链路**：点竖琴 → 引擎（-1）→ 重放到 18645 → `evaluateNpcAcquire` 通过 → **`select1(1011) + questId=18645`** → 客户端按 18645 渲染"（陈列着卡里加收藏的武器的陈列柜。）＋仔细查看" → 1007 → 接取窗（页 4）→ 接受 → 18645 落库。

## 5. 运行时探针（一次性，已删除；文本留 `.agents/summary/quest-18645-probe/`）

```
PROBE1 row18645=true
PROBE2 class=warrior scout mage cleric engineer artist bard | race=pc_light | min=37 | max=0 | reward=harp_n_u0_38a 1
PROBE3 routes(18645)=true
PROBE4 owns(18645)=true
PROBE5 acquireNpcs(18645)=[730777]
PROBE6 permitted=[AETHERTECH, ASSASSIN, CHANTER, CLERIC, GLADIATOR, GUNSLINGER, RANGER, SONGWEAVER, SORCERER, SPIRIT_MASTER, TEMPLAR]
PROBE7 zoneVerdict(bard48)=ACQUIRABLE
PROBE8 evaluateNpcAcquire(bard48)=STARTED
```

## 6. 验证

- **门禁**：`SimpleTalkNativeFamilyGateTest` **17/17**（含新用例）、`QuestEngineNpcDialogDispatchTest` **6/6**（questId=0 分发回归）。
- **实机验收（需重启加载）**：
  1. Kk（吟游星）站到竖琴 730777 旁 → 点击/使用 → **应出"（陈列着卡里加收藏的武器的陈列柜。）＋「仔细查看。」**（不再是空白）；
  2. 点「仔细查看」→ 接取窗"（陈列柜中放着一把弦乐器。）…" →「接受」→ **18645 进任务**；
  3. 拿到 `key_idcromede_05`（185000102）后点物件交任务 → 奖励 `harp_n_u0_38a`；
  4. 对照：枪炮星玩家点 730775/730776、剑星点 730329 等应同形可用（同族机制）。

## 7. 边界

- 仅"物件族"（`quest_use_item` / `quest_start_use_item`）的 `-1/1001` 打开进入重放；NPC 行为不变。
- 同族其余 11 件（18618-18627、18643/18644）共用本修复，无需逐件处理。
- `key_idcromede_05` 拾取来源（卡利加掉落，物品 185000102）不在本次范围。
