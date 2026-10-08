# quest 19640 在接取对象上点奖励窗 → `load fail (HtmlPageId 8)`：native 奖励窗动作按 questId 恢复面（2026-10-08）

状态：**已实机验收**（2026-10-08，用户确认「实机验证完成」；验收记录 `.agents/summary/quest-acceptance/19640-2026-10-08-client-accepted.md`）。

## 1. 玩家症状与实机证据

- 玩家 Kk，任务 19640（DD 族 `DD_TALK_HUNT_GRID`；接取 `Elim_DF4_01`=799022 洛塔斯，交付 `Barus`=798991）。
- 击杀满 10 只进 REWARD（`SM_QUEST_ACTION 任务=19640 状态=4`）后，与**接取对象** 799022 对话拿到奖励窗（页 5），点「选择奖励 1」（动作 8 = `SELECTED_QUEST_REWARD1`）→ 服务端下发页 8 → 客户端弹窗
  `load fail! Quest_Q19640.html (HtmlPageId 8) (QuestId 19640)`。
- 原始 trace：`evidence-19640-live-trace.txt`（`log/quests.log` 2026-10-08 09:52:56–09:54:14，共 5 轮可复现；本文件为当时日志摘录）。

```
09:53:14 [S->C] SM_DIALOG_WINDOW 玩家=Kk targetObj=79221 questId=19640 下发页=5
09:53:15 [C->S] CM_DIALOG_SELECT 玩家=Kk npcId=799022 targetObj=79221 questId=19640 上一页=5 动作=8
09:53:15 [S->C] SM_DIALOG_WINDOW 玩家=Kk targetObj=79221 questId=19640 下发页=8   ← load fail 的直接来源
```

## 2. 根因链（三层）

1. **页 5 由传送门类 AI 的旧页轴开出，不区分接取/交付**：`PortalDialogAI2.checkDialog`（与 `Specialize01PortalAI2#checkDialog` 同形）只读 QuestEngine 的 NPC 对话注册表（`getQuestNpc(npcId).getOnTalkEvent()`），任一相关任务处于 REWARD 即发 `rewardDialogId=5` + questId。19640 的**接取对象** 799022 也在注册表里（接取面 `installInterest` 注册，见 19638 修复批次），于是接取对象上被开出奖励窗。
2. **动作 8 全链无人认领**：native 各族的领奖腿（`dispatchReportDialog` / `rewardNpcIdsByQuestId`）只注册在**交付对象** 798991 上；799022 是接取对象，各族按 NPC 严格绑定均不认领，DD 进度面（Hunt 网格）也不处理奖励窗动作。
3. **AI 把动作 id 当页回显**：`PortalDialogAI2.onDialogSelect` 在引擎未认领且 `questId > 0` 时走 `else` 分支 `SM_DIALOG_WINDOW(getObjectId(), dialogId, questId)`——把动作 8 当页 8 下发，客户端按 `Quest_Q19640.html` 找 `HtmlPageId 8` 失败（客户端契约只声明 `4762/10002`）。

补充：19638/19639 同为 DD 行但交付对象就是 799022，所以它们在 799022 上「页 5 → 动作 8 → 完成」是正常的（已实机验收）；19640 是首个暴露「接取对象 ≠ 交付对象」缺口的实机案件。

## 3. 修复

### 3.1 引擎：native 奖励窗动作的按 questId 恢复面（主修）

- 新类 `NativeUnpinnedRewardWindow`：门 = 动作 ∈ `QuestDialogAction#isRewardWindowAction`（8..23/108/110..124）+ 状态 `REWARD`；结算经共用领奖口 `NativeReportRewardFlow`（真端元数据/档位/按钮声明全部门继续生效，任何一门不过零副作用）；收尾 = 选择对话页（页 10、questId=0，与各族交付面领奖收尾同形）。
- `QuestEngine.onDialog`：八族 + DD 全部未认领后调用（`requestedOwner != 0 && npc != null && isNativeOwner(...)`）。
- 裁定依据：
  - 真端 `QuestDialog` 无主键协议——typed 车道同款恢复面 `QuestProductionDispatcher#dispatchRewardWindowAction` 的既有注释：「奖励窗口由全局 UI 打开，客户端可能携带上一个交互对象，因此不能要求交互对象等于完成路由的 NPC」；
  - `QuestDialogAction#isRewardWindowAction` 契约注释：「the server must resolve them by questId + action instead of treating that object as the completion NPC binding」。
- 本质：**只结算客户端已经显示的按钮，不新增奖励窗开启入口**（开启仍由各族交付面/AI 页轴决定）。

### 3.2 AI 层：未认领任务动作不得回显为页

- `PortalDialogAI2.onDialogSelect` / `Specialize01PortalAI2.onDialogSelect` 的未认领分支改走共享分类 `PortalDialogAI2.unclaimedReply(questId, dialogId)`：
  - `DECLARED_SUB_PAGE`（客户端任务页声明过的子页动作）→ 原样回显 + questId（保留既有行为）；
  - `PLAIN_SUB_PAGE`（未声明的子页动作 = NPC 对话树页导航）→ 按 NPC 对话平面回显、无 questId（与 `DialogService`「未处理任务动作」守卫的既有裁定同形，实机 1115）；
  - `CLOSE`（其余任务动作是按钮不是页）→ 关窗，**绝不回显动作 id**（与 `DialogService` 同语义；本次 19640 的直接病灶）。

## 4. 影响面审计（同型机型）

脚本：`audit_acquire_reward_npc_gap.py`（输出 `evidence-acquire-reward-npc-gap.txt`）。
口径：扫描 8 张任务表的 `接取名 ≠ 交付名` 行（共 2106 行），按接取对象的 NPC 模板 `ai` 分组。

- **接取对象是传送门类（`portal_dialog`/`specialize_portal`）= 29 行**，即「会被 checkDialog 页轴在接取对象开出奖励窗」的暴露面：
  - `[DataDriven] 19640`（本案件，接取 799022 → 交付 Barus）；
  - `[SimpleTalk] 18711..18735 / 28711..28735`（27 行，接取 Silion 799531 → 交付 Lagia/Zaellen）；
  - `[SimpleTalk] 80279/80282`（接取 IDEvent01_In_NPC 831117 → 交付 IDEvent01_Treasure_Room）。
- 这些行的结算缺口由 3.1 的引擎恢复面统一覆盖（口径与族无关，`isNativeOwner` 八族全含）。
- 其余 AI（`general` 1148 行 / `aggressive` 303 行等）的自读注册表页轴仅 `PortalDialogAI2`/`Specialize01PortalAI2` 两类存在（`rewardDialogId` 全仓 grep 仅此二类），不在本缺口面内。
- 同型机型清单（未认领动作回显为页，**本批未改**，仅登记）：`Mighty_HeroAI2` / `Npc_SupportAI2` / `OublietteAI2` / `LatriAI2` / `Senior_AgentAI2` / `MatchMakerAI2` / `IDAb1_Heroes_TeleporterAI2` / `Investigative_BastielAI2` / `Investigative_BatiskanAI2` / `AbisoAI2` / `ProquraAI2` / `Fighting_Earth_JotunAI2` / `Sleeping_Sylfae_QueenAI2` / `ViolaAI2` / `PeregrineAI2` / `Imperial_ShrineAI2` / `Otherworldly_PucasAI2` / `Code_Red_NurserAI2` 等（`grep -rn "SM_DIALOG_WINDOW(getObjectId(), dialogId, questId)" src/main/java/com/aionemu/gameserver/ai/`，约 20+ 处）。这些 NPC 无 checkDialog 页轴，暴露前提是客户端在任务上下文点它们的页按钮且引擎未认领；按批次收敛模式登记待查。

## 5. 未做与理由

- **未收敛 `checkDialog` 的开启面**（即「REWARD → 页 5」是否应只属交付对象）：真端选择器证据不完整——反编译 `ScriptDLL64 fun_731 FUN_180caf460`（quest-731 shard）对状态 4（REWARD）返回 `0x2712`(10002 select_success)，但该函数的调用上下文（打开重放 vs 任务行选）未能确证，且收敛需要新增「该 NPC 是否该任务的交付对象」的引擎查询（现有注册表不区分接取/交付）。当前以 3.1 + 3.2 消除症状与死按钮，不开新既有行为改造面。
- 不新增奖励窗开启入口（守 QE-052「领奖 owner 唯一」与 QE-092「页与按钮同 owner」的既有裁定边界；本修复只补「客户端已显示的按钮必须有结算腿」）。

## 6. 文件清单

| 文件 | 改动 |
|---|---|
| `src/main/java/com/aionemu/gameserver/questEngine/tablelane/NativeUnpinnedRewardWindow.java` | 新增：恢复段（gate + 共用领奖口 + 页 10 收尾；含包内测试接缝） |
| `src/main/java/com/aionemu/gameserver/questEngine/QuestEngine.java` | `onDialog` 八族/DD 之后新增恢复面调用 |
| `src/main/java/com/aionemu/gameserver/ai/portals/PortalDialogAI2.java` | 未认领分支改走 `unclaimedReply` 分类（回显/关窗三分支） |
| `src/main/java/com/aionemu/gameserver/ai/portals/Specialize01PortalAI2.java` | 同上（委托同一分类） |
| `src/test/java/com/aionemu/gameserver/questEngine/tablelane/NativeUnpinnedRewardWindowTest.java` | 新增门禁：引擎编排正例（19640 + 799022 + 动作 8 → 结算 + 页 10）、动作门/状态门/越界按钮 fail-closed |
| `src/test/java/com/aionemu/gameserver/ai/portals/PortalDialogAI2Test.java` | 新增：`unclaimedReply` 分类断言（19640/1115 实测契约）+ 两个 portal AI 共用判据断言 |

## 7. 验证

### 已执行（2026-10-08，IDEA MCP runner，用户授权）

| 套件 | 结果 |
|---|---|
| `NativeUnpinnedRewardWindowTest`（新增门禁） | **4/4 通过**（引擎编排正例 19640+799022+动作 8 → 结算 + 页 10；动作门/状态门/越界按钮 fail-closed） |
| `PortalDialogAI2Test`（含新增 2 例） | **7/7 通过**（unclaimedReply 分类：19640/1115 实测契约 + 双 AI 共用判据） |
| `DataDrivenNativeRuntimeGateTest` | **36/36 通过** |
| `NativeQuestRewardClaimGateTest` | **16/16 通过** |
| `QuestEngineOpenDoorReplayOrderTest` | **1/1 通过** |
| `DialogServiceQuestDialogTest` | **8/8 通过** |
| `ProductionCatalogWhitelistVerificationTest` | 通过：`PRODUCTION_COMPILE_OK=707 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0` |
| `QuestProductionStartupGateTest` | **2/2 通过** |

合计 **75/75 全绿**。

### 已执行（实机复测，2026-10-08，用户确认）

服务端 10:28:19 冷启动（含修复构建）后，用户在实机完成 19640 全链复测（用户确认「实机验证完成」）：

1. 击杀推进：`log/adminaudit.log` 10:32:46 `spawn 215525*10`（215525 = `LF4_A3_Foam_Wisp_52_n`，19640 击杀目标）→ 击杀满进 REWARD；
2. 在洛塔斯 799022 处点奖励窗确认（动作 8）→ **领奖成功、无 load fail**（响应式验收口径：包序 not captured）；
3. 行为互证：10:34:15 `spawn 215658*10`（215658 = `LF4_B_Fethlot_Highland_53_n`，19641 目标）、10:34:32/46 `spawn 215653/215642*10`（19642 目标）——`con_quest` 链 19640→19641→19642 要求 19640 已完成，任务链连续推进。

验收记录：`.agents/summary/quest-acceptance/19640-2026-10-08-client-accepted.md`（acceptance status: ACCEPTED_NEW_PATTERN）。

### 提交

`git commit`（2026-10-08 用户「实机验证完成，提交」授权）：

- 第一提交（repair）：`NativeUnpinnedRewardWindow.java`、`QuestEngine.java`（仅本任务 hunk）、两个 portal AI、两个测试、本目录证据；
- 第二提交（docs）：Playbook 8.60（`UNPINNED_REWARD_WINDOW_QUEST_ID_RECOVERY`）+ 验收记录。两提交 hash 见验收记录。
