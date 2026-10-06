# quest 14110 交付检查按钮（20002 编码）静默关窗 + NPC 对话态未收尾 — 诊断与修复

日期：2026-10-06
状态：**实现完成；聚焦测试与生产门全绿；实机复测通过（2026-10-06，用户实测确认「正确」）**

## 1. 现象（用户报告）

任务 14110（Spiros / NPC 203111，Verteron 210030000）击杀集齐后对话交付：

```
16:10:08 [S->C] SM_DIALOG_WINDOW targetObj=25918 questId=0     下发页=10     # 开门列表
16:10:10 [C->S] CM_DIALOG_SELECT  npcId=203111 questId=14110 上一页=10  动作=31
16:10:10 [S->C] SM_DIALOG_WINDOW questId=14110                下发页=2375   # 报告页 select5
16:10:11 [C->S] CM_DIALOG_SELECT  questId=14110 上一页=2375 动作=20002
16:10:11 [S->C] SM_DIALOG_WINDOW targetObj=0 questId=0        下发页=0      # 关窗，零推进
```

玩家表述：「点击『拿出革命家的象征』后就关闭了弹窗，而且 NPC 203111 不巡逻了」。日志显示该玩家
16:08:38 起反复重试同一序列 ≥6 次（`log/quests.log` 3056-3110）。

## 2. 证据链

### 2.1 客户端页（权威声明）

`<客户端解包根>/Dialogs/10000_19999/quest_q14110.html`：

```xml
<HtmlPage name="select5">
  <Selects><Act href="HACTION_CHECK_USER_HAS_QUEST_ITEM_SIMPLE">拿出革命家的象征。</Act></Selects>
</HtmlPage>
```

该任务页**只声明**了 `select1`(1011)、`select5`(2375)，**没有 select6**（
`src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv:20549-20550` 同）。

### 2.2 运行时状态（DB 实证，2026-10-06 读取）

- `inventory`：玩家 Kk(151512) 持 **182215454 × 5**（= `quest_14110a`，任务要求 5）。
- `player_quests`：`(14110, START, quest_vars=0)` —— 检查从未执行；同日 1136（39 按钮）为 COMPLETE。

⇒ 点击时玩家**持满交付门**，真端语义应推进 REWARD + 奖励窗，实际零推进。

### 2.3 退役 XML（git 历史，`git show 4ede058c0~1:.../quest_definition/quests/14110.xml`）

```xml
<npc-item-report npc-id="203111" source="started" target="reward"
                 item-id="182215454" required="5" failure-page="CLOSE"/>
```

对应展开器 `QuestXmlBlockExpander.expandNpcItemReport`（`QuestXmlBlockExpander.java:712-718`）把每个
`npc-item-report` 块展开为**四**条边：`39 成功 / 39 失败(失败页) / 20002 成功(同 39 成功) /
20002 失败(关窗)`。即 **39 与 20002 是同一交付检查的两种客户端按钮编码**。

### 2.4 真端反编译（`<真端根>/server58/MainServer_ScriptDLL64/ScriptDLL64.c:2139580` 起）

`FUN_180cabb10`（对话/报告分派）动作分支：

```c
if (iVar5 == 0x4e22) {                                   // 20002
  iVar2 = check(questId);                                // 进度/完成门
  if (iVar1 == iVar2) {                                  // 门通过
    mgr+0x1c8(questId, 0, 1, 1);                         // = 奖励窗（同 1009 分支，尾参 0/1 之别）
    return; }
  goto joined;                                           // 未通过 → 落回步进/发页通道
}
```

0x4e22 = 20002；`mgr+0x1c8` 的项目既定映射 = 奖励窗（`p3/P3-STEP2-REPORT.zh-CN.md:16`）。

### 2.5 旧引擎的行为基准

`QuestEvent.matchesDialogId` 早已把 39/20002 定义为**同一交付检查的别名**
（`QuestEvent.java:791-812`「不同客户端修订用于同一交付检查」）。native 迁移后三个车道都退化成
只比较 `CHECK_USER_HAS_QUEST_ITEM.id()`（39），别名规则在 native 侧丢失。

## 3. 根因

native 三车道（SimpleTalk / SimpleCollectItem / DataDriven）的报告段与检查段只匹配 39：

| 车道 | 位置（修复前） | 后果 |
|---|---|---|
| SimpleTalk | `SimpleTalkHandler.java:992,1001` | 20002 走不到「推进」与「失败页」两支 ⇒ 返回 false ⇒ `DialogService.java:170-175` 兜底关窗 |
| SimpleCollectItem | `SimpleCollectItemHandler.java:921,929` | 同上 |
| DataDriven | `DataDrivenNativeRuntime.java:1837` | 落进「≥1000 原样回发」⇒ 把动作 id 当页下发 ⇒ 客户端 load fail |

影响面（客户端任务页全量普查，`.agents/summary/quest-14110-check-button/census_check_button.py`）：

| 家族 | 39（PLAIN） | 20002（SIMPLE） |
|---|---|---|
| Quest_SimpleTalk | 1534 | **604** |
| Quest_SimpleCollectItem | 164 | **93** |
| Quest_SimpleHunt | 0 | 8（本车道无报告页，按钮当前不可达） |
| Quest_SimpleUseItem | 5 | 0 |
| data_driven_quest | 378 | **65**（步页 select1/2/3） |

失败页声明：39 用户全部声明 `select6`；20002 用户 **683/693 不声明**、10 件声明
（如 80356 select6 文案「不够，一共需要5个…」——真端失败应答页）。

## 4. 修复

1. `QuestDialogAction.isItemCheckAction(int)`（新增）：把 39/20002 定义为一个**动作族**，作为唯一真源；
   `QuestEvent.isItemCheckDialogId` 改为委托（去重，别名口径不再两处分叉）。
2. `SimpleTalkHandler` / `SimpleCollectItemHandler`：报告段失败支与成功支改用族判定
   （成功 = 扣门物 + REWARD + `SM_QUEST_ACTION` + 奖励窗页 5；失败 = 客户端声明失败页，无声明则
   维持既有「关窗」兜底）。
3. `DataDrivenNativeRuntime.dispatchDialog`：`ACTION_CHECK_ITEM` 支改用族判定。

族判定是**客户端动作别名**，不是任务特例——与真端 1:1 数据驱动口径一致（无 questId/NPC/物品硬编码）。

## 5. 测试

新增（与既有 39 用例同形对照）：

- `SimpleTalkNativeFamilyGateTest#reportCheckButtonSimpleEncodingBehavesLikeThePlainForm`（1211：失败页 + 持满推进/扣物）
- `SimpleCollectItemNativeFamilyGateTest#reportPageCheckButtonSimpleEncodingBehavesLikeThePlainForm`（1144 族代表行）
- `DataDrivenNativeRuntimeGateTest#checkButtonSimpleEncodingBehavesLikeThePlainForm`（80875：10001 失败页 + 持满收尾）

## 6. 验收结果（2026-10-06，IDEA MCP runner）

| 套件 | 结果 |
|---|---|
| SimpleTalkNativeFamilyGateTest（含新增 20002 用例） | 16/16 |
| SimpleCollectItemNativeFamilyGateTest（含新增 20002 用例） | 16/16 |
| DataDrivenNativeRuntimeGateTest（含新增 20002 用例） | 30/30 |
| DialogServiceTest（新增，巡逻收尾接线 3 例） | 3/3 |
| QuestProductionStartupGateTest（生产目录编译门） | 2/2 |
| PlayerQuestDialogPortTest | 11/11 |
| QuestEngineNpcDialogDispatchTest | 6/6 |
| SimpleHunt / SimpleSerialHunt / SimpleCombineTask / SimpleItemPlay / SimpleUseItem 族门 | 5/5 · 4/4 · 11/11 · 15/15 · 11/11 |

**实机复测通过（2026-10-06，用户实测确认「正确」）**（记忆约定：实机服务端是 IDEA 常驻进程，改动需重启才生效）——验收项：
① 14110 报告页点「拿出革命家的象征」→ 扣 5×`quest_14110a` + 任务转 REWARD + 开奖励窗；
② NPC 203111 在关窗后恢复沿 `LF1A_8_NpcPath_N_Spiros` 巡逻。

## 7. 顺带修复：服务端关窗不通知 NPC（巡逻停在半路）

同一交互的并发症状，已按用户裁定「本批一并修」：

- **根因**：开门 `TalkEventHandler.onSimpleTalk`（`TalkEventHandler.java:93-98`）对 `is_dialog` 模板把行进中的
  NPC 置 `AISubState.TALK` 并设玩家为目标；移动 tick 的到达判定把 TALK 短路为「已到达」
  （`AbstractAI.java:753`）⇒ `WalkManager.targetReached` 命中 `case TALK: abortMove()`
  （`WalkManager.java:254-256`）停半路且不选下一 waypoint；恢复巡逻的唯一常规入口是
  `CM_CLOSE_DIALOG` → `DIALOG_FINISH`（`CM_CLOSE_DIALOG.java:52` → `TalkEventHandler.onFinishTalk:106-114`），
  而服务端单方面关窗的 22 个站点只发包、不碰 AI。
- **修复**：新增 `DialogService.closeDialog(npc, player)` / `closeDialog(player, targetObjectId)`
  （DIALOG_FINISH + 既有 `onCloseDialog` 清扫 + 关窗包；只通知已装配 AI 的生物，
  `Creature.getAi2IfPresent()` 不懒建 dummy）；11 个文件的 22 个服务端关窗站点全部改走该口
  （七个任务车道 + `DialogService` 兜底 + `CM_DIALOG_SELECT` 断路 + `PlayerQuestDialogPort`）。
  重复触发幂等（目标已清时 `onFinishTalk` 空操作），故客户端若也回发关窗包无副作用。
- 已落 memory-bank `AIM-008`。

## 8. 边界与遗留

1. **SimpleHunt/SerialHunt 8 行**：本车道不发 select5（杀满自动 REWARD），20002 按钮当前不可达，未接线。
2. **XML 车道展开器的 20002 失败支**仍硬编码 `CloseDialog`：XML 定义任务中仅 2237 一件用 20002 且未声明
   select6（关窗即正确），故本批不动，避免重冻 XML IR 指纹。
3. **客户端收到服务端关窗包是否回发 `CM_CLOSE_DIALOG`** 未实测（10-05 的 `PacketLoggerService` 有记录、
   10-06 已关闭，无法从日志判定）；本修复对两种情形都安全，不影响结论。
