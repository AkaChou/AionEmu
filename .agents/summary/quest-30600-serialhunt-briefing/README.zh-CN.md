# 任务 30600 简报对话面修复（SerialHunt · 800324「点击任务没有下一步」）

日期：2026-10-08（实机玩家报障 → 修复 → 实机验收）
状态：**已验收**（用户「实机验证成功」2026-10-08；验收记录
[30600-2026-10-08-client-accepted.md](../quest-acceptance/30600-2026-10-08-client-accepted.md)）

## 现象（实机 trace）

与 NPC 800324（Linocus，30600 简报 NPC）对话，「点击任务没有下一步」——协议层死循环：

```
[S->C] SM_DIALOG_WINDOW targetObj=76362 questId=0  下发页=10
[C->S] CM_DIALOG_SELECT  npcId=800324 questId=30600 上一页=10 动作=31
[S->C] SM_DIALOG_WINDOW targetObj=76362 questId=0  下发页=10
…（循环）
```

## 根因（`SimpleSerialHuntHandler.onDialog` 简报分支两处偏差叠加）

1. **打开动作发错页**：`QUEST_SELECT(31)` 被 `dialogId == 26 || dialogId == 31 || dialogId == -1`
   分支拦截，一律发**通用选择页 10（questId=0）**——客户端在页 10 上点任务行（31）→ 服务端再发
   页 10 → 死循环。**真正的简报页 select2（1352，带 questId）从未下发过**。
2. **清位分支实为死代码**：`10000 || 1003 || 10001 || 31 || 20000` 分支里的 `31` 被上述分支拦截、
   永不可达；且该分支把**动作码 10000 当页号下发**（真端该动词为 close-dialog）。

## 修复（对齐真端退役 XML 行 0a/0b）

| 动作 | 真端定义（`git show 4ede058c0^:…/quests/30600.xml`） | 修复后行为 |
|---|---|---|
| `31` QUEST_SELECT | 行 0a：`TALK_TO_NPC QUEST_SELECT` → `SHOW_QUEST_PAGE SELECT2` | 发 **SELECT2(1352) 带 questId**（契约声明：30600/9622/30610 均含 1352） |
| `26` / `-1` 打开类 | —（保持既有口径） | 保持通用页 10（与引擎开门规则同口径） |
| `10000` SETPRO1 | 行 0b：清 var5 → `close-dialog` | 清位（`setVar(0)` = 真端 briefed 投影 var0/var1/var5 全 0）+ `SM_QUEST_ACTION(START,0)` + **关窗** |
| 1003 / 10001 / 20000 | 无真端依据 | 移除 |

## 证据链

1. **真端退役 XML 30600**：0a（QUEST_SELECT → SHOW_QUEST_PAGE SELECT2，状态不变）/ 0b
   （SETPRO1 → briefed 清位 + sync + close-dialog）。
2. **真端表在册行链终点**：`21×10000:0 + 2×10001:0`（SETPRO1/SETPRO2 家族，终点页 0）——
   `.agents/summary/quest-native-dispatch/2026-09-27-w5g3-w5g4-prep-and-rulings.zh-CN.md` L73；
   "SELECT2 页链不再由服务端驱动"仅指 IR 层建模被 native 接管（CounterChain 测试已 assumeFalse 跳过），
   非按钮不存在。
3. **3058 已验收同页先例**：页 1352（select2）上的「结束对话」按钮发 SETPRO1(10000)——
   实机已走通（`quest3058RelayWritesVar0AndRemovesItsItemAtStepTwo`）。

## 验证

- 测试：`SimpleSerialHuntNativeFamilyGateTest` **5/5**（强化用例
  `briefingGateEnforcesTalkBeforeKills`：31 → 唯一对话页 **1352 带 30600**；10000 → **关窗** + 清位；
  该用例换用 `NativeTalkFixture.player()` 以获得包捕获）。
- 实机验收（2026-10-08 用户确认）：与 800324 对话 → 点 30600 任务行 → **开出简报页**（不再死循环）
  → 「结束对话」→ 关窗 + 任务书刷新。逐步 trace/截图 not captured。

## 影响面与观察项

- 同车同型行：**9622（测试行，talk_npc1-3 多简报 NPC）/ 30610（魔族版）**——契约均声明 1352，
  同修复覆盖；9622 多简报 NPC 的逐人多段流程未实机复测（观察项）。
- 本次为纯服务端修复（客户端 pak 无需变更）。

## Pattern 归属与待办

- 归属 **QE-141**（`RELAY_DIALOG_OPEN_AND_SUBPAGE_ECHO`）家族面：**SerialHunt 简报面**
  （QUEST_SELECT → 该任务页；SETPRO → 推进 + 关窗，与 3058/14120 同形）。
- **卡片更新待办**：memory-bank（`quest-engine.md` 等）正被并行线占用（有未提交改动），
  为避免互相污染，QE-141 边界扩展暂缓并入。
