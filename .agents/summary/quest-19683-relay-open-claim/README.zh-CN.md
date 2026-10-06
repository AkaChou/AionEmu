# 19683 中继收口后教官处「打开无交付对话」（2026-10-06）

## 症状（用户实机，2026-10-06 08:29 quests.log）

任务 19683「专属守护者的欢迎辞」（DD_TALK_CHAIN：教官蕾娜 806698 接取/交付，商人 Prina 806708
中继）实机 trace：

```
08:29:54 [S->C] targetObj=9733  questId=0     页=10    打开教官（未接，可接行可见）
08:29:58 [C->S] npcId=806698 questId=19683 上一页=10  动作=31   → 页 4762（接取入口）
08:30:00 [C->S] …动作=20000（QUEST_ACCEPT_SIMPLE）             → SM_QUEST_ACTION 状态=3 步数=0 + 关窗
08:30:01 [S->C] targetObj=1999  questId=0     页=10    打开 Prina
08:30:02 [C->S] npcId=806708 questId=19683 上一页=10  动作=31   → 页 1011（中继步页）
08:30:03 [C->S] …动作=10255（SET_SUCCEED）                     → 状态=4 步数=1 + 关窗（19671 批收口 ✓）
08:30:05 [S->C] targetObj=9733  questId=0     页=10    回教官打开 → 页 10，之后零 C->S   ← 缺陷
```

玩家描述：任务面板停在「向成长支援教官报告」，点教官 806698 后对话窗口没有 19683 的任何行
（「没有这个任务的对话」），无法领奖。

## 证据链（口径：真端权威；族内仲裁参照 = SimpleTalk 交付面）

1. 中继收口本身正常（19671 批已修）：10255 → REWARD + 关窗（页 0），无效页 1008 未再出现。
2. 缺陷在**打开动作（-1 = USE_OBJECT）的交付认领**：
   - `DataDrivenNativeRuntime.dispatchReportDialog` REWARD 分支原只认 `31/1009` → 打开（-1）
     不认领 → 落引擎开门平面逐任务重放（QuestEngine.java:402-410）全部 miss → 通用页 10 列表。
   - 通用页 10 列表只含**可接取**行（`NativeQuestStartPort.zoneVerdict` 对 START/REWARD 恒
     `OMITTED`，真端 opcode 127 语义），REWARD 态无交付入口 ⇒ 客户端无行可点。
   - 接取面已在 REWARD 态守门（`dispatchAcquireDialog` 1413-1416，19671 批加的），确认 -1 亦
     不被接取面认领——两侧都不认领，故落空。
3. 族内仲裁参照（SimpleTalk 交付 NPC 面，`SimpleTalkHandler.java:1019-1027`）：REWARD 态在交付
   NPC 处 `31/26/1009/-1` **任一动作都直接发奖励窗页 5**——真端在交付对象 #2 上打开即弹奖励窗，
   不依赖页 10 列表。
4. memory-bank QE-145 边界①（2026-10-05）已登记该缺口：「DD REWARD 态开门（-1）未随 SimpleTalk
   族回奖励窗页 5（现落默认页 10）——无实机样本，未动」。本次实机即该悬案的样本。
5. 19683 静态面：retention 台账 owner=RETAIL_TABLE / family=DataDriven / basis=DD_TALK_CHAIN；
   `data_driven_quest.xml` reward_npc_name=`LC1_L_grow_npc_Rena_01` → 806698
   （npc_template_800031_834289.xml，接取/交付同主，中继 Prina 806708）。
   reportTalks 注册（19671 批扩展后）已覆盖该行——缺的只是 REWARD 分支的 -1 认领。

## 修复（`DataDrivenNativeRuntime.dispatchReportDialog`，表驱动通用层，无任务 ID 特例）

REWARD 分支认领动作集 `31/1009` → `31/1009/-1`：打开（USE_OBJECT）即发奖励窗页 5，与 SimpleTalk
交付面同形。START 态行为不变（中继步行仍不认领 -1，落页 10 列表；由进度面 owns）。

## 测试（`DataDrivenNativeRuntimeGateTest.relayTalkChainRowsDeliverOnTheRewardInstructor`）

- 注册断言：806698 必须注册 19671 **与 19683** 的交付对象；
- START 段新增：打开（-1）不得被交付面认领（零发页）；
- REWARD 段新增：打开（-1）必须开奖励窗页 5（继 31/1009 之后第三动作断言）。

## 验证状态

- IDE 编译面：两文件零错误（IDEA get_file_problems，errorsOnly）。
- 聚焦门（2026-10-06，用户授权，IDEA MCP runner，复用 IDE 运行配置）：
  `DataDrivenNativeRuntimeGateTest` **28/28 全绿**（28 started / 0 failed / 0 ignored，exitCode 0；
  方法数不变——本次为既有方法 `relayTalkChainRowsDeliverOnTheRewardInstructor` 扩展断言）。
- 实机复测：**2026-10-06 用户客户端验证通过**（重启服务端加载新字节码后，走查 19683
  接取 → Prina 中继收口 → 回蕾娜打开交付链；19671 同通用面随动受益）。
