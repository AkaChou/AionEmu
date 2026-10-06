# 子页动作丢失任务上下文：13403 推进后翻页动作 1694 落两参回显 ⇒ load fail（2026-10-06）

## 现象

用户报障（角色 Kk，任务 13403「大侵袭防备调查」，DataDriven 族；交互 NPC Kinesos=203096，
targetObj=24845）：步推进后停在页 select3(1693)，点击页面按钮（"继续听"/翻页，动作=1694）后
**对话窗口不关、页面 load fail**（"结束对话"按钮随之失效——本地关窗依赖正常页面状态）。

实机日志（`log/quests.log`，2026-10-06 18:38-18:39）：

```
18:38:38 203098（Spatalos）点任务行 31 → 页 4762 → 动作 20000 → 状态=3 步数=0 + 关窗（接取 ✓）
18:38:43 203096（Kinesos）点任务行 31 → 页 1011（questId=13403 ✓）
18:38:44 动作=1012 → 页 1012（questId=13403 ✓，DD 步匹配 → 三参回发）
18:38:45 动作=1013 → 页 1013（questId=13403 ✓）
18:38:46 动作=10000 → 状态=3 步数=1、2 + 页 1693（questId=13403 ✓，STAGE_PAGES[2]=select3）
18:39:13 动作=1694 上一页=1693 → SM_DIALOG_WINDOW targetObj=24845 questId=0 下发页=1694   ← 两参！
```

## 数据事实

- 13403：`data_driven_quest.xml:4370`（DD 族）；接取 Spatalos(203098)，交付 Alaus；步链
  `Talk(Kinesos,203096) → ItemPlay → Talk(Beris) → ItemPlay → Talk(Jenel) → Talk(FOBJ1) → Talk(FOBJ2)`
- 客户端契约（`client_dialog_contract.tsv:19561-19572`）：13403 声明 1011/1012/1013/**1693/1694/1695**/2375/2376/2716/3057/4762/10002
- DD `STAGE_PAGES = {1011,1352,1693,2034,2375,...}`（select1..15，`DataDrivenNativeRuntime.java:102`）；
  `DataDrivenProgress.step(vars) = vars & 0x3F`
- **基线对照**（log/quests.log）：9/28 起 1001/1002/1004/1005/1006/1007/14012 的动作 1694 全部
  **三参回发**（`SM_DIALOG_WINDOW ... questId=<任务> 下发页=1694`）

## 根因链

1. 动作 1694 = `SELECT3_1`（子页页 id 即翻页按钮动作 id；`QuestDialogPage.isSelectionSubPage(1694)=true`）。
2. 引擎链路：各族路由（13403 非 Simple 族）→ `DataDrivenNativeRuntime.onDialog` →
   `dispatchDialog` 的**步守卫**：`DataDrivenProgress.step(vars)=2 != hit.stepIndex()=0`（Kinesos 为
   第 0 步 Talk 目标）→ `continue` → **DD 不认领**（其 `dialogId >= 1000` 三参回发分支在守卫之后，
   够不到）→ `typed.owns(13403)=false` → 引擎 return false。
3. 上游 `DialogService.onDialogSelect`：`questId≠0 && !31 && !isSelectionSubPage` 不成立（1694 是
   子页）→ **不关窗** → 落 NPC 对话平面 switch → **default 两参回发**（`SM_DIALOG_WINDOW(oid, 1694)`，
   questId=0）。
4. 客户端按"无任务上下文"去 NPC 对话 html 找页 1694 → **找不到 ⇒ load fail**；页面状态损坏后
   "结束对话"（本地关窗）随之失效。

**缺环定位**：`isSelectionSubPage` 的消费面只有 SimpleTalk/SimpleItemPlay（族内 START 段分支，
`SM_DIALOG_WINDOW(objectId, dialogId, questId)` 三参）与 DialogService（两参回显）——
**DD 与其余族（Hunt/SerialHunt/CollectItem/UseItem/CombineTask）没有"子页原样回发"分支**，
未认领的子页动作一律落两参回显；当上一页是任务页（1693 ∈ 契约）时即 load fail。

## 修复

| 文件 | 变更 |
| --- | --- |
| `src/main/java/com/aionemu/gameserver/questEngine/QuestEngine.java` | **native 车道统一兜底**：`requestedOwner≠0 && npcId≠0 && !typed.owns && isSelectionSubPage(dialogId) && hasButtonPage(questId, dialogId)` 且任务 START/REWARD ⇒ 带 questId 原样回发该页（各族 handler 之后、typed 之前——movie 等副作用不受影响；与 Talk/ItemPlay 既有语义一致） |
| `src/test/java/.../tablelane/QuestEngineSelectionSubPageEchoTest.java` | 新回归门：实机行 13403（START，vars=2，NPC 203096）+ 动作 1694 ⇒ 唯一页 1694 且 questId=13403 |

行为边界：未声明的子页动作不受影响（fail-closed 保持）；questId=0 的 NPC 对话项照旧走
DialogService 两参回显（1115 验收形）；typed 车道保持原链路；各族已认领路径逐位不变。

## 验证状态

- IDEA 静态检查（QuestEngine + 新测试类）：**无错误**
- 单测：首轮未执行；后续已按 IDEA MCP 补齐（见文末续报，64/64 全绿）
- 实机复测：首轮复测已发生（翻页修复生效；暴露「结束对话循环一下才关闭」新症状，见续报）；
  第二轮修复后的实机复测**待用户**（重启服务端后重走到 13403 页 select3 点结束对话：
  应推进续链、不再循环）

## 续报：结束对话「循环一下才关闭」——DD 对话链推进被步守卫挡下（2026-10-06 18:54 实机 + 修复）

翻页修复生效后（1694/1695 均带 questId=13403 回发 ✓），新症状：点页 select3_1_1 的「结束对话」
（10002）→ 服务端**只发关窗包**（`SM_DIALOG_WINDOW(0,0)`）、**无 SM_QUEST_ACTION**（任务不推进）；
玩家侧「没有关闭窗口、循环了一下才关闭」。

根因：`dispatchDialog` 外层守卫 `step(vars) != hit.stepIndex() → continue`——10002 到达时当前步 = 2
（Beris 步），而该 NPC（Kinesos=203096）的 hit 步 = 0 ⇒ 被挡 ⇒ 落 DialogService 的「未处理任务动作」
兜底关窗。而推进后的新步页（1693）正是 `sendPostAdvancePage` 发给**同一对话窗**的（对话链模型：
Kinesos 开场 → 翻页 → 结束 → 下一步页续链），其按钮必须能在该窗被服务。

修复（`DataDrivenNativeRuntime.dispatchDialog`）：

1. 推进动作（10000+k）免 hit 步守卫（advanceAction 豁免）；其余动作守卫不变；
2. 推进以**当前步**为基准：`currentStep = step(vars)`、`currentPlan = plans.get(currentStep)`——
   advance/动作执行器/续链页全锚当前步；
3. advance 失败不发页（return false）。

验证：IDEA MCP `DataDrivenNativeRuntimeGateTest` **31/31**（含新用例
`dialogChainAdvanceIsServedAtANonCurrentStepNpc`：13403 实机行、乱序静默 + 非当前步 NPC 窗推进
+ 单页续链）；回归 SimpleTalk 16/16、SimpleItemPlay 15/15、QuestEngineSelectionSubPageEchoTest 1/1、
QuestEngineOpenDoorReplayOrderTest 1/1——**64/64 全绿**。实机复测待用户（重启后页 select3 点
结束对话应推进续链、不再循环）。
