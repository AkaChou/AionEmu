# 无任务上下文 NPC 对话专项记录

状态：`CLIENT_ACCEPTED`（2026-09-13）

修复提交：`e518518ce fix(quest): treat contextless NPC dialogs as plain dialogs`

验收记录：[任务 1370 与无任务上下文 NPC 对话客户端验收记录](../../.agent/summary/quest-acceptance/1370-2026-09-13-client-accepted.md)

可检索 Pattern：`CONTEXTLESS_NPC_DIALOG_STAYS_PLAIN`

Playbook 案例：`8.33 无任务上下文 NPC 对话按钮被误绑定到未接取任务 owner`

## 1. 结论摘要

关闭“未满65级普通任务标记”后，5.8 客户端仍可能把 NPC 对话页上的任务按钮发回服务端，但此时客户端的 `questId` 为 0，并且没有同 NPC 的任务行记忆。这样的动作只代表普通对话，不能绑定 NPC 上未接取的普通任务 owner。

修复后，`CM_DIALOG_SELECT` 在没有任务上下文时直接走 `onSimpleDialogSelect`，完全不进入 QuestEngine owner 派发；它得到的 `page` 和 `questId=0` 响应与“该 NPC 任务全部完成”时一致。任务交互物 `USE_OBJECT(-1)` / `START_DIALOG(31)` 的 `AI2Actions.selectDialog` 路径不受影响。

## 2. 玩家可见症状

代表场景为 NPC 203949（任务 1370/1371/1373）：

1. 关闭“未满65级普通任务标记”。
2. 与 NPC 203949 对话，客户端从 `lastPage=1011` 发送动作 `1012`，携带 `questId=0`。
3. 服务端把动作绑定到未接取的普通任务 owner 1370，返回带 `questId=1370` 的 page 1012。
4. 客户端没有对应任务行，加载该任务页时报 `load fail`，继续对话无法推进。

同类 NPC 还包括 730019（1320/1321/1322/1478）、203965/203966（1347 报告链）等；表现可以是 load fail、点击无响应或卡在任务按钮页。

## 3. 协议证据与正常对照

预修复失败路径（用户日志字段，已去除角色信息）：

| 方向 | 包 | 关键字段 | 结果 |
|---|---|---|---|
| C→S | `CM_DIALOG_SELECT` | `targetObjectId=36770`、`dialogId=1012`、`lastPage=1011`、`questId=0` | 客户端没有任务上下文 |
| S→C | `SM_DIALOG_WINDOW` | `targetObjectId=36770`、`page=1012`、`questId=1370` | 错误绑定未接取 owner，客户端 load fail |

任务全部完成后的正常对照：

| 方向 | 包 | 关键字段 | 结果 |
|---|---|---|---|
| C→S | `CM_DIALOG_SELECT` | `targetObjectId=41644`、`dialogId=1012`、`lastPage=1011`、`questId=0` | 没有 owner 可接管 |
| S→C | `SM_DIALOG_WINDOW` | `targetObjectId=41644`、`page=1012`、`questId=0` | 普通页面导航正常 |

关键判据不是“页面上是否有任务按钮”，而是客户端是否给出了任务上下文：`questId>0`，或同一 NPC 存在由任务行选择建立的 remembered selection。两者都没有时，NPC 选项就是普通对话。

## 4. 根因链

移除标记后，任务行从客户端隐藏，但 NPC HTML 仍可能保留任务按钮。原入口把 `questId==0` 直接交给 QuestEngine 的通用 NPC 派发：

```text
CM_DIALOG_SELECT(questId=0, dialogId=1012)
  -> NpcController.onDialogSelect
  -> QuestEngine.onDialog(requestedOwner=0)
  -> npcDialogDispatchOwners 逐个尝试 1370/1371/1373
  -> 1370 handler 接管
  -> SM_DIALOG_WINDOW(page=1012, questId=1370)
  -> 客户端没有 1370 任务行
  -> load fail
```

任务全部完成后没有 owner 能接管，最终由 `DialogService` 回显 `page=1012`、`questId=0`，这才是正确语义。

## 5. 修复实现

修复位于 `src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_DIALOG_SELECT.java`：

```java
boolean npcTarget = obj instanceof Npc;
boolean genericQuestPage = npcTarget && isGenericQuestSelectionPage(dialogId, lastPage);
if (genericQuestPage) {
    player.clearNpcQuestDialogSelection();
}
int rememberedQuestId = obj instanceof Npc npc
    ? player.getNpcQuestDialogSelectionQuestId(npc.getObjectId()) : 0;
int routedQuestId = resolveRoutedQuestId(questId, rememberedQuestId, genericQuestPage);
if (hasQuestDialogContext(npcTarget, routedQuestId)) {
    creature.getController().onDialogSelect(dialogId, player, routedQuestId, extendedRewardIndex);
} else {
    creature.getController().onSimpleDialogSelect(dialogId, player, extendedRewardIndex);
}
```

三个语义点：

1. `hasQuestDialogContext`：NPC 目标只有在 `routedQuestId != 0` 时才进入任务引擎；没有上下文的 NPC 选项按普通对话处理。
2. `resolveRoutedQuestId`：第 10 页的非 31 动作永远返回 0，即使客户端为它附带了候选 `questId`，也不能借用任务上下文。
3. 交互物不受影响：`QuestItemNpcAI2` / `QuestStartItemNpcAi2` 通过 `AI2Actions.selectDialog` 直接调用 `QuestEngine.onDialog`，不经过 `CM_DIALOG_SELECT` 的普通对话分流；`QuestEngine.java` 本次只更新了说明性注释。

新增回归测试：

- `CMDialogSelectContextTest#treatsNpcSelectionsWithoutQuestContextAsPlainDialogs`
- `CMDialogSelectContextTest#genericPageNonQuestActionsCannotBorrowQuestContext`
- `DialogServiceQuestDialogTest#simpleNpcDialogUsesGenericPageWithoutQuestOwnerOrQuestDispatch`

## 6. 回归矩阵

| 场景 | 客户端 questId | lastPage / action | remembered selection | 路由结果 |
|---|---:|---|---:|---|
| 关闭标记，普通 NPC 对话 | 0 | 1011 / 1012 | 无 | 普通对话，页面 questId=0 |
| 开启标记，点击任务行 | 1370 | 10 / 31 | 无 | 任务引擎，正常接取链 |
| 开启标记，继续任务页 | 1370 | 1011 / 1012 | 1370 | 任务引擎 |
| 后续包省略 questId | 0 | 1011 / 1012 | 1370 | 任务引擎，使用 remembered selection |
| 第 10 页非 31 动作附带候选 questId | 1370 | 10 / 1012 | 无 | 普通对话，候选 questId 被忽略 |
| 任务交互物 | 0 | -1 / 31 | 不适用 | `AI2Actions.selectDialog -> QuestEngine` owner 路由 |

## 7. 验证记录

- 聚焦测试：`mvn -o -Dtest='QuestEngineNpcDialogDispatchTest,Quest1347ClientDialogAlignmentTest,Quest1346ClientDialogAlignmentTest,DialogServiceQuestDialogTest,CMDialogSelectContextTest,QuestProductionJourneyTest' -DfailIfNoSpecifiedTests=false test`，31/31 通过。
- 全量任务/客户端包扫描：`mvn -o -Dtest='com.aionemu.gameserver.questEngine.**,com.aionemu.gameserver.network.aion.clientpackets.**' -DfailIfNoSpecifiedTests=false test`，运行 1,317 条，仅既有 `QuestClientContractGateTest` 23 条指纹失败；规范化后与基线逐行一致。
- Playbook 校验：`python3 .agent/summary/quest/check_quest_repair_playbook.py`，输出 `PLAYBOOK_PATTERNS=56 REPRESENTATIVE_COMMITS=46 REPRESENTATIVE_TESTS=57 DETAILED_CASES=33`。
- 客户端验收：用户于 2026-09-13 回复“验证通过，请详细记录”；完整验收字段见文首验收记录。

## 8. 剩余风险与未捕获证据

- 修复后的稳定 packet trace、启动日志、截图、录屏未形成仓库附件，均标记为 `not captured`。
- 未独立覆盖所有职业、种族、重连、重复对话和所有 NPC 分支；本次用户验收按整体客户端行为确认。
- `quest_use_item` / `quest_start_use_item` 的交互物路径只有聚焦回归测试，未有本轮独立客户端证据。
- `QuestClientContractGateTest` 仍有既有 23 条指纹差异，本次修复未扩大该集合。
- 本记录与 Playbook 提交不包含 push；`quest` 分支的远端同步由用户执行。

## 9. 相关文件

- [任务排查与修复 Playbook](QUEST_REPAIR_PLAYBOOK.zh-CN.md)
- [Pattern 指纹与提交索引](repair-playbook/PATTERNS.zh-CN.md)
- [已验收代表案例 8.33](repair-playbook/CASES.zh-CN.md)
- [客户端任务对话映射说明](client-dialog-mapping/README.zh-CN.md)
- [验收记录模板](../../.agent/summary/quest-acceptance/README.zh-CN.md)
