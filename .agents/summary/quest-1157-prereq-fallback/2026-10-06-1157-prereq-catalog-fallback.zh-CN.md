# 1157 接取死亡与前置元数据回退修复（2026-10-06）

> 任务：1156「消失的村落印章」完成后，1157「加皮尔的爱情」在 NPC 798003（加皮尔）处不显示可接。
> 结论：`XMLStartCondition.checkFinishedQuests` 在「前置行已迁入原生车道（不在生产目录）」时 fail-closed；
> 10/2 P7 步 f「retail 编译产物清零」后引爆，180 条同型链静默死亡。已按真端语义修复并回归。

## 1. 实机证据（`log/quests.log`）

| 时间 | 玩家 | 事件 |
|---|---|---|
| 10-06 18:31:08 / 18:32:48 | Kk | 两轮 1156 均走完 `SM_QUEST_ACTION 任务=1156 状态=5`（COMPLETE，领奖正常） |
| 10-06 18:34:49 | Kk | 点加皮尔(228400 → targetObj=28400) 仅得 `页 10 questId=0`，无任何 1157 接取面 |
| 09-29 11:05:39 | Tt | **同一 NPC 成功接取 1157**：`31→1011→1012→1007→页4→1002→状态=3`，随后完成并接到 1158 |

即：链路 9/29 是通的，10/6 死了 —— 中间存在分水岭提交。

## 2. 根因链（代码，逐环可点）

1. `definitions/quests/1157.xml:11-13` 的 `<condition type="finished" quest-id="1156"/>`
   → catalog metadata.startConditions（`QuestDefinitionXmlCompiler.java:320-336`）
   → `QuestTemplate.fromMetadata` 生成 `XMLStartCondition.finished=[1156]`（`QuestTemplate.java:745-763`）。
2. 任何「可接」判定都走 `QuestService.checkStartConditionsImpl`（`QuestService.java:582-591`）
   → `XMLStartCondition.checkFinishedQuests`（`XMLStartCondition.java:46-69`）：
   - `qs(1156).getStatus() == COMPLETE` ✓ 通过；
   - `questCatalog().findMetadata(1156)` → **empty** → `if (metadata == null) return false;` ✗ 卡死。
3. 为什么 empty：1156 属 `RETAIL_TABLE`（retention 清单），而 catalog 由
   `definitions/quest_definition_catalog.xml` 的 **733** 个 XML 保留行构成（已核：RETAIL_TABLE ∩ catalog = 0）。
4. 引入史：
   - `edf65aaff`（8/9）重构把旧逻辑「模板缺失 → 跳过」改为「元数据缺失 → 拒绝」
     （当时全部任务都在 catalog，属防御式写法）；
   - `715a00136`（10/2，P7 步 2 步 f）「retail 编译产物为零」→ 1156 从 catalog 消失 → 立即引爆；
   - 9/29 时尚有 retail 编译产物兜底，故 Tt 可接。

## 3. 真端对照（反汇编坐实，`<真端根>/server58-source/`）

- `Quest::CanAcquireQuest` 的 leadingquest 轴（`MainServer_Server64/classes/Quest/Quest.cpp:319-336`）：
  遍历本行 7 个 `finished_quest_condN` 槽（槽间 OR）→ `User_IsLeadingQuestComplete`。
- `User_IsLeadingQuestComplete`（`classes/Account/User.cpp:200830-200903`）：槽间 OR；
  单槽 `User_IsConditionSatisfy`（同文件 200669-200748）：槽内 AND → `UserQuestData_IsFinishedQuestWithBranch`。
- `UserQuestData_IsFinishedQuestWithBranch`（`classes/Account/UserQuestData.cpp:4075-4190`）：
  存档节点存在 + branch（`:n`）匹配 + **完成计数 >= 前置行 `max_repeat_count`**；
  `max_repeat_count` 读自 QuestDB **全量静态表**（`FUN_140d1df50`，`fun/fun_249.cpp:5402-5429`；表树见 `classes/DB/QuestDB.cpp:867-903`）；
  完成计数 = `UserQuestData_GetQuestFinishCount` 返回的存档节点 +0x20 字节（`UserQuestData.cpp:4449-4555`）。
- **真端不存在「查不到前置行」的场景** —— 前置行永远从全量静态表直接查。

仓库与真端的两点偏差（本次修复的目标）：

| 维度 | 真端 | 修复前仓库 |
|---|---|---|
| 数据源 | QuestDB 全量表，永远可查 | 只查 catalog（733 XML 行）→ 缺失即拒绝 |
| 比较符 | `max_repeat_count <= finishCount`（**≥**） | `completeCount != maxRepeatCount → false`（**==**，溢出计数反而误拒） |

## 4. 修复

仅改 `src/main/java/com/aionemu/gameserver/model/templates/quest/XMLStartCondition.java`（`checkFinishedQuests`）：

1. `metadata == null` 时回退 `QuestEngine.nativeMetadata(questId)`（封装 `isNativeOwner` + `clean` + 异常兜底，
   与 catalog 同源 `RetailQuestMetadataCompiler`）；两处都无行才 fail-closed（引用破损，QE-008 语义保留）。
2. 计数判负 `!=` → `<`（对齐真端 `max_repeat_count <= finishCount` 的 ≥ 语义）；
   `maxRepeat ∈ {1, 255}` 不附加计数要求（真端 1 天然满足、255 走特判分支）。

## 5. 影响面

扫描 733 个 XML 保留行：**180 条链**的 finished 前置为 RETAIL_TABLE 行（10/2 起全部静默死亡），
含 1157 / 1194 / 1311 / 1371 / 1466 / 1634 / 2114 / 2443 / 2920 / 3013 / 3036 / 3939 / 11012 …；
修复后全部恢复正确判定。

## 6. 验证状态

- 编译：`target/classes/.../XMLStartCondition.class` 含 `nativeMetadata`（javap 核）+ 时间戳同步（IDEA VFS 滞后坑已排除，口径见 `quest-stale-red-reanchor-20261005/README.zh-CN.md`）。
- 回归（IDEA MCP 跑测）：
  - `NativeNearbyQuestAxisGateTest` **8/8 绿**（判定/展示面活基准）；
  - `QuestPrerequisiteRetailContractTest` **2/2 绿**（XML 行前置契约门）。
- 存量红（在册，与修复无因果）：
  - `QuestHaramelSubsequentQuestsProductionFlowTest`：`missing retail overlay quest 18504`（18504 于 9/30 退役，catalog/definitions 均已无该行）；
  - `QuestMetadataFieldMappingTest`：740 期望 vs 733 实际（P7-STEPF-REPORT 登记 both-red，P8-CUT1 重锚后计数再漂移）。
- **实机验收：通过（2026-10-06，用户实测确认）**：1156 完成后在加皮尔处接取 1157 全链路恢复。

## 7. 实机验收清单（已于 2026-10-06 通过）

1. 重启服务端；先核对进程启动时间晚于 class 编译时间（本树已核）。
2. 已完成 1156 的玩家（Kk）到 Verteron 加皮尔（约 892, 2024, 166）：NPC 出现可接标志、对话页 10 列表含 1157 行。
3. 接取形状：`31 → 1011 → 1012 → 1007 → 页 4 → 1002` → `SM_QUEST_ACTION 1157 状态=3`。
4. 任务流：对 NPC 210319 使用/攻击 → 引至加皮尔旁（radius 13）→ 过场电影 17 → 回加皮尔交付（SELECT5）→ 领奖 `状态=5`。
5. 随后 1158「找回的村落印章」在加皮尔处可接（RETAIL_TABLE 车道，独立面）。

## 8. 待办

1. ~~实机验收 1157~~（2026-10-06 通过）。
2. ~~同类 fail-closed 面排查~~（本批完成，见 §9）。
3. pattern 化（候选不变量：「迁移后任何引用他行的元数据查询必须有真端全量表回退」）——**已写**：
   归入 **QE-151**（`METADATA_DUAL_SOURCE_NATIVE_FALLBACK`，1157+A/B/C+D 四面合一，2026-10-06）；
   `patterns/quest-engine.md` + `systemPatterns.md` 路由均已落笔、sync+verify 绿，**提交待**并行会话
   （DD/13403）提交后补（两份文件当前含其未提交改动，无法分离 hunk；处置记录见
   `.agents/summary/quest-event-maintenance-native/…§8.4`）。

## 9. 同类面排查（findMetadata 全量 20 文件，2026-10-06）

方法：对每个 `questCatalog().findMetadata(...)` 调用点，判定「查询对象是否可能为 native 行（不在目录）」
与「null 时行为是否静默丢失/误判」。

### 9.1 已正确分流 / 无缺口

| 调用点 | 机制 |
|---|---|
| `QuestService:572`（前置检查） | native 行在 `checkStartConditionsImpl:513-515` 提前分流到 `nativeAcquireAllowed` |
| `QuestService:1212`（getLevelRequirement） | 仅 typed 行路径；native 行走 `nativeZoneVerdict` 分流（nearbyQuestFlag:1257-1263） |
| `QuestService:1405`（abandonQuest） | **已有 `nativeMetadata` 回退**（本仓同款先例：cannot_giveup / quest_work_item 取真端行） |
| `QuestService:1200`（checkLevelRequirement） | 无调用方（死代码） |
| `NpcFactions:294-307` | 显式按 metadata 存在性分流（native → `NativeSystemGrantLanes`） |
| `NpcFactions:348` | 存在性做 typed/native 归属去重（正确语义） |
| `RetailAreaEngine:145` | 显式按行分流（native AREA → start port；其余按目录） |
| `PlayerQuestStartEligibilityPort` | native 行不走它（车道设计）；其 `repeatCompletionMatches:191-196` 对前置 null 宽容（跳过计数检查） |
| `PlayerQuestStatePort` / `StateSyncPort` / `SystemMessagePort` / `EffectPort` | typed 执行面（plan.questId 均为 XML 行）或带 null 保护（StateSyncPort:116、StatePort:128） |
| `QuestEngine:487`（challenge 提示）、`QuestEngine:2560`（每日提醒广播） | 表现面：native 行少提示消息（低危，不静默丢失功能） |
| `admin/Quest:143/299` | GM 工具面：native 行少诊断/清理输出（低危） |
| `QuestStartAction:39/44` | 数据面实例 questid=450/500 均不在生产集（无 native 行实例；非有效缺口） |

### 9.2 真实缺口（同型）

| # | 位置 | 现象 | 处置 |
|---|---|---|---|
| A | `QuestState.canRepeat()`（无参，`QuestState.java:161-164`）→ catalog null → `canRepeat(metadata):167-169` false | **根子**：已 COMPLETE 的可重复 native 行被判"不可重复" | **已修**（2026-10-06）：无参版 metadata 获取加 `nativeMetadata` 回退（fail-closed 保留） |
| B | `PortalDialogAI2:135` / `Specialize01PortalAI2:85`（`qs.canRepeat()`） | 传送门/专业 NPC 处，已完成的可重复 native 行不再列为可接候选（对话入口缺失） | **随 A 修复**（调用无参版） |
| C | `CM_QUEST_SHARE`（`runImpl:43` 取数处） | native 行**不可分享**（静默无反应）；`CM_DIALOG_SELECT:179` 的共享接受分支为同缺口下游（被上游挡住，不可达） | **已修**（2026-10-06）：取数处加 `nativeMetadata` 回退；三 helper 的 null 契约（`canShare(null)→false`）保持不变 |
| D | `EventService.matchesEventQuestMetadata:193-198`（metadata==null → false） | native 事件行（80029/80032/80034-80037 等 SimpleTalk 行）**登录时不参与事件开启/维护/循环重置**；tablelane 无 `EventQuestRefresh` 替代面 | **已修**（2026-10-06 专项：EventService 双源回退 + `QuestService.startEventQuest` native 分支 + eligibility loader 回退 + 计数 ≥ 语义；318 任务中 315 行恢复；见 `.agents/summary/quest-event-maintenance-native/`） |

低危/未深查：`QuestCatalogDrop` 的 `Optional<QuestMetadata>` 消费面（类型本身 null 安全，未发现 `.get()` 风险）。

### 9.3 A/B/C 修复验证（2026-10-06，IDEA MCP）

- `CMQuestShareCanonicalMetadataTest` 1/1 绿（helper 语义未变：`canShare(null) → false` 契约保留）；
- `RetailQuestStateTest` 2/2 绿（含 `repeatEligibilityUsesCanonicalRepeatPolicyAndCooldown`）；
- `NativeNearbyQuestAxisGateTest` 8/8 绿（回归）；
- `PlayerQuestStartEligibilityPortTest` 16/17 绿，1 红 = `daevanionAuxiliarySlotsStayAlternativesInsteadOfOneConjunction`
  （**存量红**：断言 15321 的**真端编译产物**组数 2→1；15321/15301/15311 全为 RETAIL_TABLE DataDriven 行，
  测试经 `retailMetadataOf` 读编译面 —— 与 A/B/C 三个运行面改动零交集；另记为 DataDriven 编译面待归因项）。

