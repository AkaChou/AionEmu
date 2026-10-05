# 开门直发接取页抢列表（2026-10-05）

## 症状（实机报障）

与 NPC 834166 对话（表车道 DataDriven 任务 80868/80875）：「看不到任务列表，打开直接是任务接收的页面（4762）」。

## 因果链

1. 右键 NPC → `TalkEventHandler.onTalk`（TalkEventHandler.java:32）→ `questEngine.onDialog(QuestEnv(npc, player, 0, -1))`。
2. `QuestEngine.onDialog`：六个类型化车道全部 `requestedOwner != 0` 门控（QuestEngine.java:286-328）→ 开门（questId=0）不经过它们；
   唯一无条件收到开门的是 `DataDrivenNativeRuntime.onDialog`（QuestEngine.java:332）。
3. `DataDrivenNativeRuntime.dispatchAcquireDialog` 的 `case 31, 26, -1`（原 DataDrivenNativeRuntime.java:1367）：
   对 fresh（无状态）且 `evaluateNpcAcquire(...).started()`（资格判定通过）的第一个任务**直发 4762 并 return true** →
   `QuestEngine` 返回 true → `TalkEventHandler` 不发页 10 → 列表被抢。
4. 期望链路（修复后）：DD 不认领 -1 → 引擎开门块（QuestEngine.java:395-410）无进行中/可交重放 →
   落 `TalkEventHandler` 默认分支 `page 10`（TalkEventHandler.java:81，任务列表）→ 点行（31, lastPage=10）→
   DD 接取面 `evaluateNpcAcquire` 预检 → 发 4762 带 questId。

## 证据

- **引擎既有裁定**（QuestEngine.java:395-401 注释）：「右键开门…有进行中/可交任务时先进任务对话（真端对话平面
  `FUN_180c474b0`），否则才落普通页 10」。
- **家族共识**：SimpleTalk（833）/SimpleHunt（519/562）/SimpleCollectItem（993）/SimpleItemPlay/SimpleCombineTask
  接取面均 31/26-only；这些面均经实机验证（list → 31 → 入口页）。仅 DD 与 SimpleSerialHunt 认领 -1。
- **真端 `FUN_180c47220` 勘误**：其 state 0/10 分支「任何动作发 4762」发生在**宿主已按任务上下文路由到该处理器**
  的信封内；宿主对开门（-1、无 questId 上下文）走通用平面（进行中重放/页 10），不经 fresh 任务处理器。
  `FUN_180cabb10`（serial hunt）同构——非 -1 路由证据。
- **历史基线**：09-29 OLD 引擎 quests.log：打开 → 下发页 10 → 动作 31 → 下发页 4762。
- **引入史**：DD 的 -1 来自 e1 移植（fd574e347，自述「零行为变更」口径），无实机验证背书。

## 修复（4 文件，+30/−11）

| 文件 | 改动 |
|---|---|
| `DataDrivenNativeRuntime.java` | `dispatchAcquireDialog`：`case 31, 26, -1` → `case 31, 26` + javadoc 修正（打开不认领） |
| `SimpleSerialHuntHandler.java` | 接取面 `26/31/-1` → `26/31`（**死边**：该处理器仅 `requestedOwner != 0` 可达，各重放路径要么带 START/REWARD 态、要么非接取 NPC——纯词汇对齐，零行为风险） |
| `DataDrivenNativeRuntimeGateTest.java` | javadoc + 新增断言：fresh 玩家 -1 → `onDialog` false 且零发页 |
| `SimpleSerialHuntNativeFamilyGateTest.java` | `nativeDialogFlowEndToEnd` 新增 `envOpen(-1)` → false 断言 |

保留项：DD `dispatchDialog` 的 -1 认领（有 `state != null` + hit/步守卫）＝ 进行中开门的真端阶段页语义，正确。

## 验证

- IDEA `build_project`：✅ 零 problem。
- 门测试（IDEA MCP，exitCode 0）：`DataDrivenNativeRuntimeGateTest` 全类 ✅；
  `SimpleSerialHuntNativeFamilyGateTest` 4/4 ✅；相邻门 `DataDrivenNativeContractGateTest` ✅、
  `CMDialogSelectContextTest` ✅。
- 实机回归：**待用户客户端验证**（NPC 834166：开门应见任务列表 → 点行 → 4762）。

---

# 续报：接取收尾 20000 回页 load fail（2026-10-05 15:16 实机）

## 症状

开门修复生效（页 10 列表 → 行选 31 → 1011 → 动作 20000 接取成功），**接取后下发页 1003 → 客户端 load fail**（quest 14110 / NPC 203111 / Spiros）。

## 因果链

1. 14110 ∈ `Quest_SimpleTalk.xml`（SimpleTalk 车道）；客户端契约仅声明 1011/2375，**无 1003/1004**
   （全库 3676/3665 个任务声明 1003/1004，客户端据此分型：无确认页的任务接受按钮发 20000）。
2. `SimpleTalkHandler` 接取面把 `1002 || 20000` 折叠同支、都回 `PAGE_ACCEPTED (1003)`。
3. 真端 `FUN_180cab520`（cab520 = 七族接取面同一引用函数）**逐支确证**：
   - `0x3ea (1002)` → check → 页 `0x3eb (1003)` + 发物（param_4/param_5 = 行 give_item）
   - `0x4e20 (20000)` → check → **`0x5d8` 关窗** + 发物（simple accept 无确认页）
   - `0x3eb (1003)` → 回页 `0x3ec (1004)`；`0x4e21 (20001)` → `0x2a8` 取消 + **关窗**
4. 同形滞后族：SimpleSerialHunt / SimpleHunt / SimpleCollectItem 同分支同错。
   正确形已在 SimpleItemPlay / SimpleCombineTask / DataDriven 落地（10-04 DD 修复含实机背书）。

## 修复（6 文件）

| 文件 | 改动 |
|---|---|
| `SimpleTalkHandler` | 1002 → 1003（+补发物，真端两支均发）；20000 → 关窗(+发物)；20001 → 关窗；头注释同步 |
| `SimpleSerialHuntHandler` | 1002 → 1003；20000 → 关窗（简报位保留）；20001 → 关窗 |
| `SimpleHuntHandler` | 同上（无发物/简报） |
| `SimpleCollectItemHandler` | 同上（grant 保留在支内） |
| `SimpleTalkNativeFamilyGateTest` | 新 `acceptTailFollowsTheRetailCab520CloseSemantics`：1002→1003+give、20000→页0+give、20001→页0；`acceptCommitCreatesTheRetailRow` 换 fake 端口实例（1002 现在发物，单测无 ItemData） |
| `QuestRepeatLifecycleTest` | 1963 的 1002 接取换 `NativeTalkFixture.handler(RecordingInventory)`（同上原因） |

## 验证

- IDEA 编译 ✅；门测试（exitCode 0）：`SimpleTalkNativeFamilyGateTest` ✅、`SimpleSerialHuntNativeFamilyGateTest` ✅、
  `SimpleHuntNativeFamilyGateTest` ✅、`SimpleCollectItemNativeFamilyGateTest` ✅、`SimpleHuntHandlerTest` ✅、
  `SimpleTalkRowAlignmentGateTest` ✅、`QuestRepeatLifecycleTest` ✅。
- 实机回归：**待用户客户端验证**（14110：接受 → 应直接关窗入任务书；拒绝 → 同样关窗）。
