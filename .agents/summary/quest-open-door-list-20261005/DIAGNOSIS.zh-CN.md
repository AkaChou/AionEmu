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

---

# 续报 2：进行中阶段页抢占开门 + 39 交付检查按钮无处理（2026-10-05 17:21 实机）

## 症状

NPC 834166（Elly）有 3 个可接任务。80868、80875（Luna 事件，DD 表行）先后接取成功后：
「继续和 npc834166 对话，应该能够继续接取任务才对，但是现在看到的是任务 80875 的
『交出持有物品』对话」；且点该按钮零响应（日志：`页1011 questId=80875 → 动作=39 上一页=1011 → 关窗`）。

完整日志链：`页10→31(80868)→4762→20000→状态3+关窗→页10→31(80875)→4762→20000→状态3+关窗→
页1011 questId=80875→动作=39→关窗`。

## 因果链（两处独立缺陷）

1. **打开（-1）被进行中阶段页抢占**：第三次开门时 80875 已进行中（步 0，CollectItem），
   引擎开门块（QuestEngine.java:402-410）逐一重放进行中任务 → DD `dispatchDialog` 的
   `case 31, 26, -1` 阶段页分支命中（state+命中步守卫通过）→ 发阶段页 1011 并 return true →
   引擎认定已认领 → TalkEventHandler 默认页 10 列表不可达。
   第二次开门（仅 80868 进行中）之所以正常，是零步 Talk 行无步命中（无 StepHit）未触发该分支。
2. **39 无处理**：80875 客户端 1011 页（select1，收集步）的按钮 = 「交出持有物品」
   （HACTION_CHECK_USER_HAS_QUEST_ITEM = 39）；`dispatchDialog` 无 39 分支（39 < 1000，
   落到末尾 `return false`）→ 零响应，客户端停页后关窗。

## 证据

- **真端 SimpleTalk 族既有约定（同形仲裁）**：`SimpleTalkHandler` 交付 NPC 面
  - 进行中（START）：`dialogId == 1009 || 31 || 26 || -1` → `PAGE_IN_PROGRESS`（页 10 列表，
    SimpleTalkHandler.java:1010）；
  - 待领奖（REWARD）：`31 || 26 || 1009 || -1` → 页 5 奖励窗（SimpleTalkHandler.java:1023）。
  即**阶段页只随行选 31/26 下发，开门（-1）回列表**。QD-144 中「保留 DD 的 -1（进行中阶段页）」
  系静态推断、实测推翻。
- **39 语义**：AL 参考 `_80875FightAgainstMechanerk.java`（39 → `checkQuestItems(0,1,false,10000,10001)`）；
  真端 `FUN_180c474b0` 的顺序动作 10000..10013 步进 + `0x3f1(1009)` → `0x100`+`0x1b0` 结算；
  真端 +0x268 槽（FUN_180c45f70）发 0x2711 = 10001（检查失败应答）。
- **客户端契约**：80875 声明 1011/4762/10000/10001/10002（`check_user_item_fail`=10001）；
  talk 族检查型惯例 `select6`=2716。
- **数据**：quest.xml 80875 `collect_item1 = quest_80875a 7`（item 182216117，
  `item_template_182005539_190200002.xml`）；data_driven_quest.xml 80875 progress=CollectItem 单步。

## 修复（3 文件）

| 文件 | 改动 |
|---|---|
| `DataDrivenNativeRuntime.java` | ① `dispatchDialog` 阶段页分支 `case 31, 26, -1` → `case 31, 26`（打开不认领，落引擎开门平面 → 列表）；② 新增 `ACTION_CHECK_ITEM(39)` 交付门分支：门 = quest.xml `collect_item`（回退 `quest_work_item`）经 `RetailItemNameIndex` 解析；未持满 → 契约 `check_user_item_fail` 页（80875=10001）；持满 → 按门扣除 + 步进 + （末步）奖励窗页 5；门符号未全解析 ⇒ fail-closed。构造器新增 `RetailItemNameIndex` 参数（2 个调用点同步） |
| `QuestDialogContract.java` | `checkFailPage` 名称优先：先查声明 `check_user_item_fail` 的页（事件/DD 行），回退 select6=2716（talk 族惯例），未声明 -1 |
| `DataDrivenNativeRuntimeGateTest.java` | `talkStepsFollowTheSharedRetailDialogPlane` 补「打开(-1) 零发页不认领」断言；新增 `checkButton39FollowsTheHandOverGate`（80875：无物 → 10001 + 零写；持 7 个 → remove:182216117:7 + REWARD + 页 5） |

## 验证

- IDEA 编译 ✅；门测试（exitCode 0）：`DataDrivenNativeRuntimeGateTest` ✅（含新 39 用例）、
  `DataDrivenNativeContractGateTest` ✅、`DataDrivenItemPlayGrantGateTest` ✅、
  `SimpleTalkNativeFamilyGateTest` ✅、`GelkmarosKanteleRowLadderContractTest` ✅、
  `NativeAcceptEntryAskFlowGateTest` ✅、`QuestKillCounterRetailGateTest` ✅、
  `RetailQuestDriverOverlayTest` ✅。
- **存量红（与本次无关，需上游批次处理）**：`AcceptAndConfirmationEntryContractTest` 读取的
  `definitions/quests/15010.xml` 已随 `fde615094`（DataDriven 归一，物理退役 XML）删除，
  HEAD 即红。
- 实机回归：**待用户客户端验证**（834166：接取 80868/80875 后再对话应见列表（可继续接第 3 个）；
  点 80875 行 → 1011 收集页；无 quest_80875a 点「交出持有物品」→ 10001 失败页；集齐 7 个 → 扣除并弹领奖窗）。
