# quest 3733 无法重复接取 — 诊断与修复记录（2026-10-07）

## 报障

实机玩家 Kk 与 NPC 800518（LF2_Brando_E_LHM / Brando）交互，完成 quest 3733 后再次点击任务行（动作 31），服务端反复只回下发页 10（questId=0），无法重复接取。

## 证据链

### 1. 实机日志时间线（log/quests.log，本地时间）

| 时间 | 事件 |
|---|---|
| 19:36:35 | CM_DIALOG_SELECT 31 (questId=3733) → S->C 页 **1011**（入口页，正常） |
| 19:36:36 | CM_DIALOG_SELECT **20000** → SM_QUEST_ACTION 任务=3733 状态=3（START）→ **首次接取成功** |
| 19:36:54 ~ 19:38:37 | 步数 1→65→4161→状态 4（REWARD），击杀推进正常 |
| 19:39:08 | S->C 页 5（奖励窗） |
| 19:39:09 | 动作 23（无选择确认）→ 状态 5（**COMPLETE**），complete_count=1 |
| 19:39:10 起 | 31 动作全部 → 回页 10（questId=0），循环，无任何接取页 |

### 2. 玩家存档（player_quests, player_id=151512）

- 查询时 DB 无 3733 行 → 非状态数据问题：`PeriodicSaveConfig.PLAYER_GENERAL=900`（15 分钟周期保存，`GeneralUpdateTask`），完成时未到期属正常延迟。
- 内存状态经日志确认为 COMPLETE、complete_count=1 < max_repeat_count=5（重复预算充足）。

### 3. 对照实验（同一玩家/时段，log/quests.log）

Kk 于 19:41 在 NPC 203935/203932 上的 31 动作（1421/1427/14120/1310）全部正常进接取面 → 非全局机制故障，仅 3733 所在族受影响。

### 4. 代码定位

- `SimpleHuntHandler.handleDialog`（`src/main/java/com/aionemu/gameserver/questEngine/tablelane/SimpleHuntHandler.java:517`）接取段仅 `status == NONE || qs == null`；COMPLETE 全部失配 → `return false`。
- `QuestEngine.onDialog:292` 的 `routes(3733)` 为 true、handler 返回 false → 全链无认领 → `DialogService.onDialogSelect` 回退页 10。
- 排除项：3733 无 `definitions/quests/3733.xml`（不在 xmlOnlyIds，native 路由成立）；`resolveMembers("LF2_Brando_E_LHM")` 唯一解析 800518；`SimpleHuntHandler.routes(3733)=true`。

## 根因（家族性缺口）

真端 `finishedcount < max_repeat_count` 的可重复任务在 COMPLETE 态应重开接取面（记忆库 QE-036：REPEAT_COMPLETE_START_DIALOG_GATE）。native 表驱动迁移中，Talk/ItemPlay/UseItem 三族已实现 `fresh || (status == COMPLETE && repeatable(questId))`（`SimpleTalkHandler:830`、`SimpleItemPlayHandler:599`、`SimpleUseItemHandler:466`）；**SimpleHunt / SimpleSerialHunt / SimpleCollectItem 三族遗漏**。

### 影响面（audit_repeat_families.py，按 quest.xml max_repeat_count>1 统计）

| 族 | 总行数 | 受影响可重复行 |
|---|---|---|
| SimpleHunt | 1863 | **786**（含 3733） |
| SimpleCollectItem | 262 | 79 |
| SimpleSerialHunt | 16 | 1（9622） |

## 修复（2026-10-07）

三族 handler 同一处对称修改（模式照抄 ItemPlay）：

1. 接取段条件：`boolean fresh = state == null || status == QuestStatus.NONE;` + `if (fresh || (status == QuestStatus.COMPLETE && repeatable(questId)))`。
2. 各族新增私有 `repeatable(int questId)`（读 `NativeQuestXmlTable` 的 `max_repeat_count > 1`，与 Talk 同形；不查已用次数——收尾由 `NativeQuestStartPort.repeatVerdict` 按 `complete_count < max_repeat_count` 结算）。
3. 未加 `evaluateNpcAcquire` 预检（与 ItemPlay/UseItem 口径一致；资格过滤已在清单层 `zoneVerdict`）。

改动文件：
- `src/main/java/com/aionemu/gameserver/questEngine/tablelane/SimpleHuntHandler.java`
- `src/main/java/com/aionemu/gameserver/questEngine/tablelane/SimpleSerialHuntHandler.java`
- `src/main/java/com/aionemu/gameserver/questEngine/tablelane/SimpleCollectItemHandler.java`

## 验证

### 单测（IDEA MCP 运行，全部 exitCode=0）

| 测试类 | 结果 |
|---|---|
| SimpleHuntNativeFamilyGateTest（7，含新增 `repeatableCompletedRowReopensTheAcceptFace`，锚点 3733 + 非可重复 2354） | 通过 |
| SimpleSerialHuntNativeFamilyGateTest（5，含新增，锚点 9622 + 非可重复 30600） | 通过 |
| SimpleCollectItemNativeFamilyGateTest（18，含新增，锚点 9620 + 非可重复 1137） | 通过 |
| SimpleHuntHandlerTest（2） | 通过 |
| NativeQuestStartPortTest（9） | 通过 |
| QuestEngineNpcDialogDispatchTest（5） | 通过 |

新增用例锁定：COMPLETE+completeCount<max → 31 下发入口页（带 questId）；1002/20000 收尾复位 START、vars 清零/置位（SerialHunt 简报位 0x40000000）、complete_count 保留；max_repeat_count=1 的行 COMPLETE 态仍不进接取面。

### 实机验收

**待用户执行**：重启服务端（改动须重启生效）后，Kk 对 800518 点 3733 → 应下发页 1011 → 20000 → 状态 3，重复接取闭环。

## 关联

- 记忆库 QE-036（同一不变量的 native 车道形态，已补充）。
- 门禁/Playbook：待实机验收后按 quest-repair 规则评估（规则 11：静态推断不得登记代表案例）。
