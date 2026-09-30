# M5-c 以真端为基准复核基线失败（旧 XML 期望 → 真端/客户端期望）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-23
- 前置：M5-b2（SimpleCollectItem 批次 1）后，`questEngine` 全树仍有 **28F + 7E = 35 个失败方法**（`gates/head-baseline-failures.txt`，2026-09-23 16:00 基线）。
- 本轮用户口径（最高优先）：**"部分测试是以旧的 xml 为主，可能是错误的，请以真端为基准"**
  → 断言旧 XML 行为的测试期望，若与真端表 / 客户端任务书契约冲突，**改测试（或修生产 XML 的缺项）以真端为准**，不得为了过测试回退真端驱动。

## 1. 交付物

| 类型 | 路径 | 说明 |
|---|---|---|
| 报告 | 本文件 | 35 个基线失败的逐类处置 + 全树复跑结果 |
| 基线名单 | `.agents/summary/scriptdll-quest-driver/gates/head-baseline-failures.txt` | 35 行，逐方法（含参数化 [n]） |
| 基线明细 | `.agents/summary/scriptdll-quest-driver/gates/m5b2-baseline-failure-details.tsv` | 失败消息逐条 |
| 复跑日志（中间态） | `.agents/summary/scriptdll-quest-driver/gates/m5c-retail-baseline-questengine.log` | 改到一半时的 2F |
| 复跑日志（终态） | `.agents/summary/scriptdll-quest-driver/gates/m5c-final-questengine.log` | **1947 例 / 0F / 0E / 1 skipped / BUILD SUCCESS** |

## 2. 判定口径（三类证据的优先级）

1. **真端表**（`/Users/mc/IdeaProjects/58Server/Map/XML/Quest_*.xml`）：任务是否可驱动、进度类别、收集/击杀物、奖励档位。
2. **客户端契约**（`/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs/**/quest_q<id>.html` → `quest_client_summary_rows.tsv`）：任务书行数 = 进度行号上限（QE-051 领奖行投影）。
3. **现有 XML**：**只作对照物**，不是金标准；历史错误直接按 1+2 放行。

## 3. 处置分类（35/35 全部分类）

### A 类：测试断言旧 XML 行为 → 改测试以真端/客户端为准（11 个方法）

| 测试方法 | 旧期望（XML） | 新期望（真端/客户端） | 客户端证据 |
|---|---|---|---|
| `Quest1926And2938ClientDialogAlignmentTest.quest1926…` | 领奖行 `var0=0` | `var0=1` | `quest_q1926.html` 2 行（槽位 %0/%3） |
| `Quest1926And2938ClientDialogAlignmentTest.quest2938…` | 同上 | `var0=1` + 自愈边条件 0→1 + 转移数 25→26 | `quest_q2938.html` 2 行 |
| `Quest21114PoisonedFungiRetailFlowTest.preserves…` | `var0=4` | `var0=5` | `quest_q21114.html` 6 行（末行=向 Martinez 报告） |
| `Quest28808ClientDialogAlignmentTest.keeps…` | 旧 XML 角色/行号 | 真端 SimpleTalk 单一接取-报告-领奖所有权 | 客户端页出口 + 真端表 |
| `Quest3057RetailFlowAlignmentTest.completes…` | `var0=0` | `var0=1` | `quest_q3057.html` 2 行 |
| `Quest50019RetailAlignmentTest.preserves…` | 领奖行 1 行口径 | `var0=2`（3 行任务书） | `quest_q50019.html` 3 行 |
| `Quest51019RetailAlignmentTest.preserves…` | 同上 | `var0=2` | `quest_q51019.html` 3 行 |
| `QuestArchDaevaPromotionDefinitionTest.elyos…` | `var0=6` | `var0=5` | `quest_q10520.html` 6 行（槽位 %0/3/6/9/12/15） |
| `QuestArchDaevaPromotionDefinitionTest.asmodian…` | 同上 | `var0=5` | `quest_q20520.html` 6 行 |
| `ReportToManyMirrorQuestFamilyTest.mirrored…` | reward `var0=1`、交接写 `var0=1` | reward `var0=2`、交接不写 `var0` | `quest_q18805/28805.html` 3 行 |
| `QuestCounterProjectionLockFollowUpTest.quest4711…` | reward `var0=0` | reward `var0=3`；finalKill 源 `s2`；首次交接 `LEVEL_AND_VISIBILITY_REFRESH` | `quest_q4711.html` 4 行 + `4711.xml` 行阶梯 |

### B 类：XML 旧结构 vs 真端/客户端派生的新行阶梯 → 改测试目标节点（9 个方法）

| 测试方法 | 处置 |
|---|---|
| `MissionItemConsumptionBatchRegressionTest.campaignMissionsConsumeRequiredCollectionItems` | 29064 的扣物边 `started→reward` 改为 `s1→reward`（客户端 2 行） |
| `MissionItemConsumptionBatchRegressionTest.turnInItemConsumptionContractsAreLocked` | 20529 的扣物边 `s9→reward` 改为 `s9→s10`（12 行，折叠链在批次后拆出 s10） |
| `QuestRetailCollectionRoleAlignmentTest.preserves…` ×7（CollectionCase[2/5/6/7/8/9/10]） | 新增 `rewardRow` / `turnInTarget` 声明：25094 走中间行 `s1`（并在中间行显式 `SetVariable("var0",1)`），其余 `goTo reward`；25062/25526/25532/25535/25538/25690 = `var0=1`，25080/25081/25013 = `var0=0` |

### C 类：真端/客户端证明 XML 缺项 → 修生产 XML（2 个方法，9 个 XML）

| XML | 修复 | 依据 |
|---|---|---|
| `quests/13809.xml`、`quests/23809.xml` | 730969/730970/730971 各加 `can-act ACTION_ITEM_USE` | 三个 NPC `ai="quest_use_item"`；客户端页按钮 `HACTION_SETPRO` |
| `quests/18301.xml`、`quests/28301.xml` | 730374（H-Core）加 `can-act ACTION_ITEM_USE` | NPC `ai=quest_use_item` |
| `quests/30504.xml`、`quests/30554.xml` | 701098（柱子物件）加 `can-act ACTION_ITEM_USE` | NPC `ai=quest_use_item`；真端 `action_ids` 只登记 701098 |
| `quests/10526.xml`、`quests/20526.xml` | work-items 补 `164002347` / `164002348` | 真端 `quest_work_item1`（`Item_F6_Mission_Summon_Leibo_L/D`）；legacy `2c4a0de78^` 尾步 `removeQuestItem` |
| `quests/15101.xml` | 收口击杀保留 `set-variable var0=2` | `QuestSection0ReportRowContractTest`：报告行显式写值是该族合同 |

→ 加完 `can-act` 后 `QuestInteractionObjectCatalogTest` 7/7 绿；`QuestWorkItemMigrationCoverageTest` 的 `EVIDENCE_CONFLICT_QUESTS` 收敛为只剩 4942。

### D 类：测试自身缺陷 / 口径工具（13 个方法，含 1 处生产类修正）

| 测试方法 | 处置 |
|---|---|
| `MiragentQuestFamilyProgressionTest.family…` | 3938 期望节点更新为 `…s9,s10`；末节点允许非 `reward` 命名但必须投影 REWARD；`FINAL_PROGRESS_NODES[3938]` → `s9` |
| `Quest25512ClientDialogAlignmentTest.follows…` / `killProgress…` | 按 `sourceNode` 过滤改用 `Objects.equals`（生产定义带无 source 的 enter-world 自愈边，`null` 被 `equals` 误判） |
| `QuestHandoverContinuationAuditTest.clientLocal…` | `REPAIRED_BRANCHES` 里 21027 从 `started→reward by 799254 page 5` 改为 `stage1→reward by 799255 page 10000`（QE-051 三行阶梯后交钥匙移到 Kantele）；解析器支持 `#` 注释行；失败消息带 violations 明细 |
| `QuestVarsTest.packsAndUnpacksEverySixBitSection` | **真端口径修正（生产类）**：打包值是 32 位（wire `writeD`、DB INT），槽 0..4 占 bit0..29，槽 5 只剩 bit30..31 → 测试改为 `s0=63,s5=3` + `fifthSlotValueBeyondTwoBitsIsDroppedByTheWireFormat` + `unsetSlotsReadAsZero` |
| `QuestVars`（生产，`src/main/java/.../model/QuestVars.java`） | `getQuestVars()`/`setVar()` 改无符号移位（修 `setVarById(5,6)` 回读 62 的符号扩展 bug）；`getVarById()`/`getQuestVars()` 容忍 `null` 槽位 |
| `QuestKillCounterSimulator`（测试工具） | `requiredKills` 先试全新 START 快照，未命中退回第一条击杀转移的 source 投影；`killCounterFields` 增加"写入值 == 目标节点投影 ⇒ 阶段/行号钉住，不算计数器" |
| `QuestKillCounterRetailGateTest.singleCounter…` | 改为聚合 violations 一次性报告（原来首个不符即中断，掩盖其它任务） |
| `QuestItemSourceContractGateTest.repaired…` | 退役任务回退 `ProductionQuestDefinitions.definition(id)`（28836/28838 XML 已退役）；28836/28838 断言从 `collect-item` 改 `assertTurnInItems(item,50)`（真端 SimpleTalk `item_check=1` + `quest.xml collect_item1`）；2239 期望改 `Map.of(182203228,3, 182203227,1)` |
| `QuestCounterProjectionLockFollowUpTest.quest25608…` | 模拟器口径修正后 25608（7 行任务书，击杀行 step3 → 推进 step4）通过 |
| `QuestLegacyMonsterHuntProductionFlowTest.quest25060…` / `quests25090And25093…` / `simpleMonsterHunts…` | 同样 `Objects.equals` 过滤修正 + 真端口径 |
| `QuestStepNpcSkipGuardTest.quest3711…` / `quest4711…` | 重写 `assertSkipGuarded`：断言"步骤 NPC 无任何直达 REWARD 的 `SELECT_QUEST_REWARD` 边"（原断言硬编码 `started→reward`，行阶梯后失效）；流程重排为 SETPRO1 → s1 收到 SELECT2 页 → 提前索奖被拒 → SETPRO2 → 最后一击进 REWARD |

## 4. 结果

```
mvn -o -q test-compile
mvn -o test -Dtest='com.aionemu.gameserver.questEngine.**' -DfailIfNoTests=false
→ Tests run: 1947, Failures: 0, Errors: 0, Skipped: 1 ; BUILD SUCCESS
```

- 唯一 skipped = `QuestDialogMigrationEquivalenceTest`（既有长期 skip，与本次无关）。
- 基线 35 个失败方法 **35/35 收敛**，且其中 15 个是"改测试期望对齐真端/客户端"，2 个是"按真端/客户端补生产 XML 缺项"，1 个是生产 `QuestVars` 位宽 bug。

## 5. 未验证 / 边界

- 仅跑了 `questEngine` 全树（1947 例），**未跑全仓 `mvn -o test`**。
- 客户端证据为静态解包 HTML + 任务书行数 TSV，**无真机 5.8 客户端在线验收**。
- 生产 `QuestVars` 改动影响所有任务的 var 打包（`QuestVars` 是全局模型类），建议后续在真实存档/DB 往返场景补一次冒烟。

## 6. 下一步

1. M5-b3：60 个带 `ROUTE/OTHER` 轴的 SimpleCollectItem 行逐条定性 → 放行第二批 XML 退役。
2. M5-c（族）：`SimpleUseItem`(104) / `SimpleItemPlay`(15) / `SimpleSerialHunt`(10)。
3. M6-pre：`DataDriven`(1508 行) 按 ScriptDLL64 已还原的类别语义落地。
4. 全仓回归 `mvn -o test`（待授权后执行）。
