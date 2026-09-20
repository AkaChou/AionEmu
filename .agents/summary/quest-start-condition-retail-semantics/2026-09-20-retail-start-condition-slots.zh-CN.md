# 起始条件“槽位”语义修复（15321/15323 无法接取）

日期：2026-09-20
范围：`src/main/resources/aion/data/static_data/quest_definition/quests/` 起始条件块

## 玩家现象

- `Ww` 在 NPC `805330` 的任务列表中看到 `15321`、`15323`（`SM_DIALOG_WINDOW ... questId=0 下发页=10`）。
- 点击任务行后客户端发送 `CM_DIALOG_SELECT questId=15321 动作=31`，服务端返回 `下发页=4762`（`SELECT_NONE`）。
- 随后客户端发送 `动作=20000`（`QUEST_ACCEPT_SIMPLE`，接受按钮），服务端只回 `targetObj=0 questId=0 下发页=0` 关闭，任务未接取；重复多次同样结果。

## 证据链

1. **Aion 5.8 客户端 `quest.xml`（Map/XML，UTF-16）**
   - `15321`/`15322`/`15323`：`acquired_quest_cond1 = Q15301`、`acquired_quest_cond2 = Q15311`。
   - `15311`：`finished_quest_cond1 = Q15307`、`noacquired_quest_cond1..5 = Q15312..Q15316`。
   - `80613`：`finished_quest_cond1 = Q80611,Q80612`（同一字段内逗号列表）。
2. **零售 NPC 服务端（58Server 反编译源码）**
   - `Quest_CanAcquireQuest`（`NPCServer_NPCSvr64/classes/Quest/Quest.cpp`）按条件族分别检查：`finished`/`acquired`/`noacquired`/`unfinished` 各族独立求值，族与族之间必须同时成立。
   - `User_IsLeadingQuestComplete` / `User_IsLeadingQuestAcquired` / `User_IsRestrictionQuestComplete` / `User_IsRestrictionQuestAcquired`（`classes/Account/User.cpp`）：遍历最多 7 个槽位，**任一槽位满足即成立**（槽位之间 OR）。
   - `User_IsConditionSatisfy`：同一个槽位内逗号分隔的多个任务 ID 逐项 **AND** 累积。
   - `FUN_140daf5c0`（`XML_ParseQuestCondition`）：以 `,` 分词后写入同一个槽位的连续 8 字节元素，确认“逗号列表 = 同一槽位”。
3. **运行时数据（`al_server_gs.player_quests`，玩家 `Ww`）**
   - `15301 = START`，`15311` 无记录，`15321/15323` 无记录 → 旧判定 `acquired(15301) AND acquired(15311)` 为假，返回 `START_CONDITION_REJECTED`。
4. **迁移前后对照**
   - `quest_data.xml` 中 `15321`/`15323` 是**两个独立 `<start_conditions>` 块**（每块一个 `acquired`），旧接取路径 `QuestService.checkStartConditionsImpl` 对块取 OR。
   - typed XML 迁移把两块平铺进单个 `<start-conditions>`，而该 shorthand 被编译为**单个 AND 组**，OR 语义被收紧为 AND。

## 根因

客户端条件字段 `*_quest_condN` 是“槽位”：槽位之间 OR，槽位内逗号列表 AND。迁移把“多个槽位”平铺成 shorthand 后，编译器只产生一个 AND 组，导致：

- `15321/15323`（已接 `15301`、未接 `15311`）被错误拒绝；
- 反向地，`15311` 系列的 6 个单条件组被写成组间 OR，把“完成 15307 且未接 15312-15316”放宽为“任一成立即可”。

## 修复内容

- **多槽位任务改写为显式组（31 个）**：
  `13905`-`13908`、`15321`-`15326`、`15335`、`23905`-`23908`、`25321`-`25326`、`25334`、`25335`、`80060`-`80063`、`80066`-`80069`。
  改写后 `<start-condition-groups>` 与客户端槽位语义一一对应（组间 OR、组内 AND）。
- **15311 系列限位条件合并（12 个）**：
  `15311`-`15316`、`25311`-`25316` 由“6 个单条件组”改为“1 组：finished 前置 + 其余 noacquired”，恢复“完成 15307/25307 且其余部位任务均未接取”。
- **补齐 15322 缺失的槽位声明**：与客户端一致恢复 `acquired 15301 / acquired 15311`（at-distance 接取同样受该前置约束）。
- **测试**：`PlayerQuestStartEligibilityPortTest` 新增
  - `daevanionAuxiliarySlotsStayAlternativesInsteadOfOneConjunction`（15321 两组；已接 15301 可接、无前置被拒）；
  - `commaSeparatedSlotEntriesStayInsideOneConjunction`（80613 保持单组 AND）。

## 审计工具

- `.agents/summary/quest-start-condition-retail-semantics/audit_client_slots.py`
  从客户端 `quest.xml` 重建槽位模型，输出生产 XML 与零售语义不一致的任务清单（可重复运行）。

## 遗留发现（不在本次修复范围）

1. **24 个任务的槽位结构与客户端不一致**（缺条件或多条件）：
   `14124`、`1467`、`1510`、`17014`、`18911`-`18913`、`19047`、`19057`、`2217`、`2371`、`24113`、`2414`、`2433`、`2486`、`27014`、`28911`-`28913`、`29047`、`29057`、`3102`、`3975`、`4975`。
   其中一部分是“生产文件缺少客户端声明的前置”，需要单独的证据链确认是否为有意的放宽。
2. **大量生产任务完全没有 `start-conditions`，而客户端声明了前置**（约 700 个文件）：属于“前置未迁移”家族，需单独审计推进。
3. **单槽位多 ID 的限位语义**（如 `1877` 的 10 个 `noacquired`）：零售判定等价于“至少一个未接取”，与 `unfinished`/`noacquired` 的直觉“全部未接取”不同，建议后续用客户端实测或抓包确认。

## 验证状态

- 静态：XML 全部可解析（`xml.etree` 0 错误）；审计脚本确认 15321/15322/15323/15311 等与客户端槽位语义一致；IDE 检查无新增 error。
- 待授权执行（未运行）：
  `rtk mvn -q -Dtest=PlayerQuestStartEligibilityPortTest,QuestDaevanionThreeStageFlowTest,RetailSequentialQuestFamilyTest,QuestItemSourceContractGateTest,QuestClientContractGateTest,ProductionCatalogWhitelistVerificationTest test`
- 客户端复验：PENDING（需要真实客户端接取 15321/15323 与 15311 前置链）。
