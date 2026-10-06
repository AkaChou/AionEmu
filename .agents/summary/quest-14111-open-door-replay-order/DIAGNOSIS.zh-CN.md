# 开门重放遮挡可交付任务：NPC 203126 上 1155 短路 14111（2026-10-06）

## 现象

用户报障：角色 Kk 从 NPC 203126（Abolos）接取 **1155** 与 **14111**；14111 已完成盗贼（LehparAs_11_An×9）
与头目（LehparWaNamed_12_An×1）击杀，但打开对话只见 1155 的未完成面（2375 → 2716），
**任务列表看不到 14111、无法交付**。

实机日志（`log/quests.log`，2026-10-06）：

```
18:17:35 CM_DIALOG_SELECT questId=14111 上一页=10 动作=31   → 页 1011（接取入口）
18:17:36 动作=1012 → 页 1012；18:17:38 动作=1007 → 页 4；动作=1002 → 状态=3 步数=0 + 页 1003
18:23:33..18:24:23 击杀推进 步数=1..9
18:25:31 SM_QUEST_ACTION 任务=14111 状态=4 步数=73           ← REWARD（可交付）
18:26:13 SM_DIALOG_WINDOW targetObj=27152 questId=0 下发页=10   ← 打开对话：通用页 10，无页 5
18:26:14 动作=31(1155) → 页 2375 → 动作=39 → 页 2716（1155 未持满失败页）
18:28:42/44 再次打开 → 同样页 10（questId=0）
```

## 数据事实

- NPC 203126 = Abolos（`npc_template_200000_216188.xml:2779`，`name="abolos"`）
- **1155**：`Quest_SimpleCollectItem.xml:41`，`acquired_npc_name`/`reward_npc_name=Abolos` →
  SimpleCollectItemHandler 车道，START（采集 LF1_brownie_Box 未满）
- **14111**：`Quest_SimpleHunt.xml:13525`，`acquired_npc_name`/`reward_npc_name=Abolos` →
  SimpleHuntHandler 车道，REWARD
- 客户端契约：1155 声明 2375/2716（与日志吻合）；14111 声明 4/1003/1004/1011/1012/1352

## 根因链

1. 打开对话（`-1`）→ `QuestEngine.onDialog` 的 `questId==0` 块，对玩家 START/REWARD 任务逐个重放
   （questId 上下文重入）。
2. 重放按 `QuestStateList`（TreeMap）**questId 升序单遍遍历**，首个认领者 `return true` **短路**。
3. `1155 < 14111` → 1155 先被遍历。`SimpleCollectItemHandler.handleDialog` 的 START 分支
   （`SimpleCollectItemHandler.java:942`）对 `dialogId == -1` 认领：
   发两参 `SM_DIALOG_WINDOW(objectId, PAGE_IN_PROGRESS=10)`（questId=0）→ `return true`。
4. 循环终止 → 14111 的奖励窗重放（`SimpleHuntHandler` REWARD 分支 `-1 → 页 5`）**永不执行**。
5. 客户端任务列表**不渲染交付（REWARD）行**（19683 / QE-145 边界① 已确立）⇒ 14111 既不在列表、
   也无法交付——违反「可交付任务必须可经打开（-1）到达」不变式。

排除项（认领条件成立的判据）：acquire 与 reward 走同一 `NativeNpcNameResolver.resolveMembers` 通道，
接取成功已证 `resolveMembers("Abolos")` 含 203126 ⇒ `rewardNpcIdsByQuestId.get(14111)` 必含 203126；
页形（两参 → trace questId=0）与 `PAGE_IN_PROGRESS=10` 同日志逐字吻合。

## 修复

| 文件 | 变更 |
| --- | --- |
| `src/main/java/com/aionemu/gameserver/questEngine/QuestEngine.java` | 开门重放改两遍：**REWARD 先行、START 其后**（可交付任务不再被编号更小的进行中任务短路遮挡） |
| `src/test/java/com/aionemu/gameserver/questEngine/tablelane/NativeTalkFixture.java` | 新增 `assertOnlyDialogPageWithQuest`（断言唯一页 + questId 上下文） |
| `src/test/java/com/aionemu/gameserver/questEngine/tablelane/QuestEngineOpenDoorReplayOrderTest.java` | 新回归门：实机行 1155（START）+ 14111（REWARD）同挂 203126，打开必须命中页 5 + questId=14111 |

行为边界：无 REWARD 任务时第二遍 START 遍历与原行为一致（各族现有「START && -1 → 页 10」语义不变）；
多个 REWARD 任务时按 questId 升序交付第一个（交完再开下一个）。

## 验证状态

- IDEA 静态检查（3 文件）：**无错误**
- 单测：**未执行**（未授权 build）——待授权后用 IDEA MCP 跑 `QuestEngineOpenDoorReplayOrderTest`
- 实机复测：**待用户**（重启服务端后打开 NPC 203126 应直接进 14111 奖励窗页 5，领奖后 14111 → COMPLETE）
