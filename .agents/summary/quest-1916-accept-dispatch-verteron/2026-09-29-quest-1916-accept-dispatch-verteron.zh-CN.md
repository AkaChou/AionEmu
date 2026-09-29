# 任务 1916（派往贝尔特伦）接取失败与转职后续族前置奖励分支对齐修复

## 1. 报障背景与现象

- **报障任务**：
  - 任务 ID：`1916`（派往贝尔特伦 / Dispatch to Verteron）
  - 阵营：`ELYOS`，职业：`CHANTER, CLERIC`，类别：`IMPORTANT`
  - 前置任务：`1007`（转职仪式 / A Ceremony of Sacred Purpose）
  - 接取 NPC：`203761`（祭司教师希凯雅 / Hygea）
  - 驱动归属：`RETAIL_TABLE`（`SimpleTalk`，已在 commit `4ede058c` 中退役 XML 并由真端表驱动）
- **日志切片**：
  ```text
  09-29 07:28:14 INFO  [multiThreadIoEventLoopGroup-3-4] quest - [QUEST-TRACE][S->C] SM_DIALOG_WINDOW 玩家=Zz targetObj=416 questId=0 下发页=10
  09-29 07:28:16 INFO  [PacketProcessor:1] quest - [QUEST-TRACE][C->S] CM_DIALOG_SELECT 玩家=Zz npcId=203761 targetObj=416 questId=1916 上一页=10 动作=31
  09-29 07:28:16 INFO  [multiThreadIoEventLoopGroup-3-4] quest - [QUEST-TRACE][S->C] SM_DIALOG_WINDOW 玩家=Zz targetObj=416 questId=1916 下发页=4
  09-29 07:28:18 INFO  [PacketProcessor:2] quest - [QUEST-TRACE][C->S] CM_DIALOG_SELECT 玩家=Zz npcId=203761 targetObj=416 questId=1916 上一页=4 动作=1002
  09-29 07:28:18 INFO  [multiThreadIoEventLoopGroup-3-4] quest - [QUEST-TRACE][S->C] SM_DIALOG_WINDOW 玩家=Zz targetObj=0 questId=0 下发页=0
  09-29 07:28:18 INFO  [multiThreadIoEventLoopGroup-3-4] quest - [QUEST-TRACE][S->C] SM_DIALOG_WINDOW 玩家=Zz targetObj=416 questId=0 下发页=10
  ```
- **核心异常**：
  玩家 Zz 与希凯雅对话首屏（页 10）可正常展示任务 1916；点击动作 31 进入接取询问窗（页 4，`ask_quest_accept`）；但在点击动作 1002（`QUEST_ACCEPT_1` 接受）后，服务端未成功接取任务，而是下发 `SM_DIALOG_WINDOW(0, 0)` 关闭窗口，随后弹回 NPC 初始页 10。

---

## 2. 根因分析 (Root Cause Analysis)

1. **接取转移边带条件门控**：
   任务 1916 由真端表驱动，其动作 1002 转移边带有前置条件 `QuestCondition.StartEligible()`。当条件不满足时，`QuestEngine.onDialog` 返回 `false`；`DialogService.onDialogSelect` 捕捉到返回 `false` 且动作号为非页面动作（1002）时，为防止非法动作导致客户端崩溃，下发 `SM_DIALOG_WINDOW(0, 0)` 关闭窗口。
2. **前置条件与运行时数据链断裂**：
   - 运行时 `PlayerQuestStartEligibilityPort.startConditionMet` 通过 `condition.rewardMode() == state.getReward()` 检查前置完成奖励分支。
   - 在 NCSoft 原生真端数据 `quest_retail/quest.xml` 中，任务 1916 声明：
     `<finished_quest_cond1>Q1007:4</finished_quest_cond1>`
     NCSoft 原生数据按 1-based 奖励槽 `reward_exp1..6` 区分职业分支：1=战士、2=斥候、3=法师、4=祭司（希凯雅）、5=枪炮、6=乐手。
   - 在 AionEmu 服务端实现与历史存档中：
     - 转职仪式（`1007.xml`、`2009.xml`）在角色完成转职时，完成奖励索引为 0-based（`complete-quest reward-index="0..5"`，祭司希凯雅执行 `complete-quest reward-index="3"`），数据库 `player_quests.reward` 存储 `0..5`。
     - 客户端 NPC 对话首屏过滤配置 `quest_data.xml` 中，1913..1916 分别配置为 `reward="0..3"`，因此 NPC 首屏能够正常显示任务。
   - 退役 XML 引入真端元数据编译器（commit `4ede058c`）时，`RetailQuestMetadataCompiler.java` 解析 `finished_quest_cond` 冒号后数字时，直接将字符串截取解析为 `rewardMode`（`Q1007:4` -> `4`）。
   - 最终导致运行时判定为 `4 == 3`（False），接取动作 1002 被拒，任务无法接取。

---

## 3. 完整受影响任务族（家族级审计）

遵循“严禁只修报障的单个任务”原则，全量审计真端 `finished_quest_cond` 中带 `:` 引用 1007/2009 的所有任务：

| 任务 ID | 任务名称 | 阵营 | 职业分支 | 真端声明 | 原编译器解析 | 实际引擎完成奖励索引 | 修复后 rewardMode |
|---|---|---|---|---|---|---|---|
| **1913** | Dispatch to Verteron | ELYOS | GLADIATOR, TEMPLAR | `Q1007:1` | 1 (错) | 0 (Macus) | **0** |
| **1914** | Dispatch to Verteron | ELYOS | ASSASSIN, RANGER | `Q1007:2` | 2 (错) | 1 (Eumelos) | **1** |
| **1915** | Dispatch to Verteron | ELYOS | SORCERER, SPIRIT_MASTER | `Q1007:3` | 3 (错) | 2 (Bellia) | **2** |
| **1916** | Dispatch to Verteron | ELYOS | CHANTER, CLERIC | `Q1007:4` | 4 (错) | 3 (Hygea) | **3** |
| **19070** | Dispatch to Verteron | ELYOS | GUNSLINGER, AETHERTECH | `Q1007:5` | 5 (错) | 4 (XML保留) | **4** |
| **19071** | Dispatch to Verteron | ELYOS | SONGWEAVER | `Q1007:6` | 6 (错) | 5 (XML保留) | **5** |
| **2901** | Dispatch to Altgard | ASMODIANS | GLADIATOR, TEMPLAR | `Q2009:1` | 1 (错) | 0 | **0** |
| **2902** | Dispatch to Altgard | ASMODIANS | ASSASSIN, RANGER | `Q2009:2` | 2 (错) | 1 | **1** |
| **2903** | Dispatch to Altgard | ASMODIANS | SORCERER, SPIRIT_MASTER | `Q2009:3` | 3 (错) | 2 | **2** |
| **2904** | Dispatch to Altgard | ASMODIANS | CHANTER, CLERIC | `Q2009:4` | 4 (错) | 3 | **3** |
| **29070** | Dispatch to Altgard | ASMODIANS | GUNSLINGER, AETHERTECH | `Q2009:5` | 5 (错) | 4 | **4** |
| **29071** | Dispatch to Altgard | ASMODIANS | SONGWEAVER | `Q2009:6` | 6 (错) | 5 | **5** |
| **2911** | Song of Blessing | ASMODIANS | 全职业 (2009全部后续) | `Q2009:1..6` | 1..6 (错) | 0..5 | **0..5** |

*(注：其他非转职任务如 80298 活动选择族、30155 等，其分支奖励在 AionEmu 体系中设计原生即为 1/2，不涉及 1-based 转 0-based 偏移，保持原值不变。)*

---

## 4. 修改清单

1. `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailQuestMetadataCompiler.java`:
   - 增加 `parseRewardMode(int questId, String token, int colon)` 方法。
   - 当引用的父任务为 `1007` 或 `2009` 时，将真端 1-based 奖励槽 `1..6` 规范化映射为 AionEmu 0-based 奖励模式 `0..5`（`Math.max(0, raw - 1)`）。
   - 在 `finished_quest_cond` 遍历和 `condFamily` 中统一使用该解析规则。
2. `src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestDispatchToVerteronFamilyProductionFlowTest.java`:
   - 将原单侧技术星分支断言 `preservesRetailMetadataForTechnistDispatchBranches` 扩展为全家族级断言 `preservesRetailMetadataForEveryVerteronBranch`，对 1913~1916 及 19070/19071 全量 6 个职业分支的元数据与 `rewardMode` 严格加锁。
3. `src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestDispatchToAltgardFamilyProductionFlowTest.java`:
   - 同步 commit `4ede058c` 退役 2901 等 XML 并迁移至真端 `SimpleTalk` 后的表驱动行为断言（去除 XML 时代的过时 `SetVariable("var0", 1)` 动作断言以及旧自定义传送动作），与 Verteron 家族统一遵循真端规范流。

---

## 5. 验证结果

### 5.1 静态与语法检查
- IntelliJ 诊断：所有修改文件 0 errors, 0 warnings。
- Git 检查：`git diff --check` 通过，无多余空白或格式冲突。

### 5.2 聚焦回归测试（用户授权执行）
```bash
mvn test -Dtest=PlayerQuestStartEligibilityPortTest,QuestDispatchToVerteronFamilyProductionFlowTest,QuestDispatchToAltgardFamilyProductionFlowTest
```
测试结果：
```text
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 361.7 s -- in com.aionemu.gameserver.questEngine.runtime.QuestDispatchToAltgardFamilyProductionFlowTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aionemu.gameserver.questEngine.runtime.QuestDispatchToVerteronFamilyProductionFlowTest
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.459 s -- in com.aionemu.gameserver.questEngine.runtime.PlayerQuestStartEligibilityPortTest
[INFO]
[INFO] Results:
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
