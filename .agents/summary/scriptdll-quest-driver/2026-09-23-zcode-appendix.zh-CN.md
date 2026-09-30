# ZCODE 交接辅助内容：状态、数据地图、家族覆盖、架构锚点、坑位

> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> ⚠️ **状态已过期（2026-09-23 23:5x）**：本附录 §1「工作区状态」与 §3「运行时架构锚点」里的 HEAD/文件数按 v1 口径写成，
> 已被真端改造推进超越。**最新状态、回归三档、作废结论清单以 `2026-09-23-zcode-goal-prompt-v2.zh-CN.md` §3/§6/§7 为准**；
> 本附录其余章节（数据地图、字段样例、DLL helper→族归属、坑位）仍然有效。


配套提示词：`2026-09-23-zcode-prompt.zh-CN.md`。本文件按需查阅，不要求整篇读入上下文。

---

## 1. 工作区状态（2026-09-23 实测）

- `HEAD = 943419462`，分支 `quest`（`ahead origin/quest 9`，**未 push**）。
- `git tag wip/retail-quest-phase5` → `452a2a404`（被撤回的真端改造提交，可整份恢复；用户要求先不提交）。
- `git status`：`47 M` + `8 ??`，暂存区 0。
  - 未跟踪：`src/main/java/.../questEngine/retail/`、`src/test/java/.../questEngine/retail/`、`src/main/resources/aion/data/static_data/quest_retail/`、`src/test/java/.../definition/QuestSimpleHuntRetailContractTest.java`、`src/test/resources/quest/quest-simple-hunt-retail-contract.tsv`、`.agents/summary/scriptdll-quest-driver/`、`.agents/summary/idea-unused-false-positive/`、`.agents/summary/quest-20528-reward-row/`。
  - 40 个任务 XML 被修改：**38 个属本改造**（仅 `<dimension>` npc-id 对齐）`11027 11029 11038 11150 11202 11293 1179 14276 1470 1497 17001 18205 18309 18310 18617 18915 24275 27001 30218 3052 3097 3098 3113 3213 3527 3532 3713 4525 80215 80224 80399 80400 80415 80416 80434 80435 80450 80451`；另 2 个（`10528`、`20528`）属**他人并发工作**，不要动。
- 生产白名单 `quest_definition_catalog.xml` 条目数：**6224**。

---

## 2. 已有代码与文档（Phase 1–5-3）

```
src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleHunt.xml     640KB（真端表 UTF-8 化）
src/main/java/com/aionemu/gameserver/questEngine/retail/
  RetailSimpleHuntTable.java      140 行  countN/monsterN/acquired_npc_name/reward_npc_name/con_quest
  RetailHuntCounterLayout.java     61 行  6 位槽位语义（DLL FUN_180cb13b0）
  RetailNpcNameIndex.java          92 行  name_desc → npc_id（87719 名）
  RetailSimpleHuntPlan.java        98 行  slotForNpc/canCount/count/isComplete/npcIdsBySlot
  RetailQuestCatalog.java          81 行  RETAIL_TABLE / XML_FALLBACK / UNKNOWN
  RetailSimpleHuntIrCompiler.java 444 行  表行 → 定义 IR；tableFirst(...)；11 个降级码
src/test/java/com/aionemu/gameserver/questEngine/retail/{RetailSimpleHuntTableTest,RetailSimpleHuntPlanEquivalenceTest,RetailQuestCatalogTest,RetailSimpleHuntIrCompilerTest}.java
src/test/java/com/aionemu/gameserver/questEngine/definition/QuestSimpleHuntRetailContractTest.java
src/test/resources/quest/quest-simple-hunt-retail-contract.tsv  1366 行快照
.agents/summary/scriptdll-quest-driver/  各阶段文档 + 报告 + 脚本 + phase5-3-rejections.txt
```

阶段文档：`2026-09-22-phase1-quest-registry`、`phase2-driver-semantics`（DLL 语义）、`phase3-retail-table-driver`、`phase4-simple-hunt-reconciliation`、`phase4b-gate-and-align`、`phase5-1-simplehunt-table-driver`、`phase5-2-retail-catalog`、`phase5-3-retail-ir-compiler`。

---

## 3. 运行时架构锚点（不要重写，只能复用）

管线：`QuestDefinitionXmlCompiler.parse/compile` → `QuestDefinitionCompiler.compile`（结构校验 + 契约还原）→ `CompiledQuestDefinition` → `QuestEventIndex` → `QuestProductionDispatcher.dispatch`；进度存 `QuestState` / `QuestVars`。

| 锚点 | 位置 |
|---|---|
| 定义记录 | `questEngine/definition/QuestDefinition.java`（id, version, metadata, progressLayout, nodes, transitions） |
| 结构编译器（全部失败码） | `questEngine/definition/QuestDefinitionCompiler.java`（760 行；`fail("CODE", ...)` 31 处，如 `AMBIGUOUS_TRANSITION`、`UNREACHABLE_NODE`、`COMPLETE_QUEST_SYNC_REQUIRED`、`CRAFT_LIFECYCLE_INCOMPLETE`） |
| counter-grid XML 展开语义（对拍靶子） | `questEngine/definition/QuestXmlBlockExpander.java:749 expandCounterGrid` |
| Java DSL | `questEngine/definition/QuestDsl.java`（`bitField`:27、`killNpc`:54、`QuestBuilder`:745、`TransitionBuilder`:807） |
| 进度布局 | `questEngine/definition/ProgressLayout.java` + `BitField.java`（`pack/unpack`，字段不得重叠） |
| 事件索引 / 调度 | `questEngine/runtime/{QuestEventIndex,QuestProductionDispatcher,QuestMutationPlanner}.java` |
| 生产目录装载 | `questEngine/QuestEngine.java:1853` → `QuestDefinitionCatalogManifest.compile(...)`；`QuestDefinitionDirectoryLoader.java:47` 走 `compileResourceEntries`（Phase 7 overlay 应挂在这条链上） |
| 状态模型 | `questEngine/model/{QuestState,QuestVars,QuestStatus,RetailQuestState}.java`；`QuestStatus`：START=3 / REWARD=4 / COMPLETE=5 / LOCKED=6 |
| 建议门禁测试 | `questEngine.ProductionCatalogWhitelistVerificationTest`、`questEngine.definition.QuestDefinitionCatalogManifestTest`、`questEngine.definition.QuestClientContractGateTest`、`questEngine.definition.QuestRetailStartMetadataGateTest` |

`QuestMetadata` 字段：name, displayNameId, minLevel, maxLevel, permittedRaces, category, repeatPolicy, prerequisites, itemRequirements, rewards, drops, permittedClasses, permittedGender, rank, maxCountLimitedQuest, countRecoverLimitedQuest, cannotShare, cannotGiveup, bountyReward, useClassReward, combineSkill, combineSkillPoint, timer, repeatCycles, npcFactionId, mentorType, targetType, titleId, inventoryItems, questWorkItems, extendedRewards, bonuses, kills, startConditions, classRewards, rewardGroups, extendedRewardGroups, startConditionGroups。

---

## 4. 真端 / 客户端数据结构（实测样例）

### 4.1 `Quest_SimpleHunt.xml`（1865 行）

```xml
<id id="1102"><count1>3</count1><monster1>CherubimL_1_n, CherubimL_2_n</monster1>
  <acquired_npc_name>Mires</acquired_npc_name><reward_npc_name>Mires</reward_npc_name><con_quest>…</con_quest></id>
```
`countN/monsterN` = ScriptDLL64 注册点的 `param_4/param_3`；槽位 N ↔ `SECTION_(N-1)`；1517 = `count1=4`(3 只 NagaWi) + `count2=6`(6 只 NagaKn)。

### 4.2 真端 `quest.xml`（22MB，UTF-16 + 内部 DTD，258,493 行）

```xml
<quest><id>1102</id><name>Q1102</name><dev_name/><desc>STR_QUEST_NAME_Q1102</desc>
  <category1>important</category1><category2>STR_QUEST_ZONE01</category2><f_mission>0</f_mission>
  <max_repeat_count>1</max_repeat_count><client_level>1</client_level>
  <minlevel_permitted>1</minlevel_permitted><maxlevel_permitted>0</maxlevel_permitted>
  <finished_quest_cond1>Q1101</finished_quest_cond1><cannot_share>1</cannot_share>
  <gender_permitted>all</gender_permitted><reward_exp1>180</reward_exp1><reward_gold1>400</reward_gold1>
  <class_permitted>warrior scout … rider</class_permitted><race_permitted>pc_light</race_permitted></quest>
```
对照仓库 `quests/1102.xml`：`display-name-id="1102202"`、`IMPORTANT`、min 1 / max 2147483647、前序 1101、`cannot-share="true"`、`GOLD 400 + EXP 180`、`ELYOS`。
客户端 UTF-8 副本（11MB）：`/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml`。
收集类字段（1001）：`collect_progress`、`collect_item1 "quest_1001a 3"`、`drop_monster_1`、`drop_item_1`、`drop_prob_1`、`drop_each_member_1`、`selectable_reward_item1_N`、`check_item1_N`。

### 4.3 其余各族表结构（首行实测）

| 表 | 首行样例（截断） |
|---|---|
| `Quest_SimpleTalk.xml`(3152) | `<id id="85"><acquired_npc_name>Rebecca</acquired_npc_name><reward_npc_name>Rebecca</reward_npc_name></id>`（多步链另有 `talk_npcN`；2641 = 4 步） |
| `Quest_CombineTask.xml`(574) | `<id id="5000"><dev_name>…</dev_name><task_npc>Anteros,Auminus</task_npc><combineskill>weaponsmith</combineskill><combine_skillpoint>1</combine_skillpoint><recipe_name>r_ws_q5000</recipe_name><product>item_ws_q5000 3</product><give_component1>item_part_ws_q5000_a 4</give_component1></id>` |
| `Quest_SimpleCollectItem.xml`(262) | `<id id="1103"><dev_name>…</dev_name><acquired_npc_name>Mires</acquired_npc_name><object1>LF1_Cherubim_pouch</object1><reward_npc_name>Mires</reward_npc_name><con_quest>1104</con_quest></id>` |
| `Quest_SimpleUseItem.xml`(160) | `<id id="1107"><dev_name>…</dev_name><use_item_name>ITEM_QUEST_1107A</use_item_name><reward_npc_name>Namus</reward_npc_name></id>` |
| `Quest_SimpleItemPlay.xml`(43) | `<id id="9623"><acquired_npc_name>Rebecca_5</acquired_npc_name><talk_npc1>Rebecca_2</talk_npc1><talk_npc2>Rebecca_3</talk_npc2><use_item_name>ITEM_QUEST_9623A 1</use_item_name><reward_npc_name>Rebecca_5</reward_npc_name></id>` |
| `Quest_SimpleSerialHunt.xml`(16) | `<id id="9622">…<count_first>1</count_first><monster_first>…</monster_first><count_second>1</count_second><monster_second>…</monster_second>…<count_fifth>1</count_fifth><monster_fifth>…</monster_fifth><reward_npc_name>Rebecca_5</reward_npc_name></id>` |
| `Quest_SimpleGather.xml` | **空容器**（`<quest_simplegathers></quest_simplegathers>`，无任务行）→ 本仓库 0 个任务，Phase 10 只记录不迁移 |
| `data_driven_quest.xml`(2510) | `<quest_data_driven><id>1740</id><name>Q1740</name><category_acquire_>Talk</category_acquire_><value0_acquire_>Taranis</value0_acquire_><reward_npc_name>Taranis</reward_npc_name><progress_info><data><category_progress_>Hunt</category_progress_><value0_progress_>Ab1_1131_guard_Da_c_1, …</value0_progress_></data></progress_info></quest_data_driven>` |

### 4.4 客户端表

- `quest_monster.csv`（2.2MB）：客户端"任务→怪"，含 `Progress(SECTION_n<limit; SECTION_5==0)` 计数守卫。
- `quest_script_monster.csv`（256KB）：**必须有独立步骤**的脚本怪 —— 未登记其中的击杀可整体用一个计数模型表达（这就是"1001 的 5 只怪各占一个状态、多数击杀任务一个状态就够"的判据）。
- `combine_task.xml` / `challenge_task.xml` / `data_driven_quest.xml`：客户端侧 CombineTask / ChallengeTask / DataDriven 表。

### 4.5 ScriptDLL64（53MB，latin-1）

`extract_helper_body.py FUN_xxxx` 抽函数体。核心语义（SimpleHunt）：`(packed >> 6*(N-1)) & 0x3F < countN` 则 `packed += 1 << 6*(N-1)`；达 `Σ countN<<6(N-1)` 写完成态（0x100），否则普通写（0xf0）。

---

## 5. DLL helper → 族归属（`helper_families.tsv` 摘要）

| helper | quests / stubs | 族 | 族内重叠 |
|---|---|---|---|
| `FUN_180cabb10` | 3671 / 4516 | SimpleTalk | 3151 |
| `FUN_180cab520` | 3284 / 3284 | SimpleTalk | 2970 |
| `FUN_180cafa40` | 1877 / 1935 | SimpleHunt | 1861 |
| `FUN_180cb13b0` | 1786 / 2429 | SimpleHunt | 1786 |
| `FUN_180caf7c0` | 1648 / 1648 | SimpleHunt | 1632 |
| `FUN_180caac10` | 574 / 1148 | CombineTask | 574 |
| `FUN_180caaf00` | 574 / 574 | CombineTask | 574 |
| `FUN_180caca90` | 472 / 472 | SimpleTalk | 407 |
| `FUN_180cacb30` | 102 / 205 | SimpleTalk | 67 |
| `FUN_180cafe40`/`FUN_180cb1610`/`FUN_180cb14e0` | 50/47/26 | SimpleHunt | 47/47/26 |
| `FUN_180caf350`(645)、`FUN_180caf6c0`(493)、`FUN_180caf640`(416)、`FUN_180caf3c0`(416)、`FUN_180caf740`(337)、`FUN_180caf5f0`(103)、`FUN_180caa850`(38)、`FUN_180cab150`(31)、`FUN_180caa1b0`(27)、`FUN_180cacbf0`(25) | — | `SCRIPTED(无模板)` | 0（这就是"保留 XML"的 494 个仓库任务的注册点来源） |

（完整表见 `helper_families.tsv` / `registry_helpers.tsv`；注册点原文见 `quest_registry.tsv`，20383 行。）

---

## 6. SimpleTalk / CombineTask 语义要点（Phase 2 反编译结论）

公共结构：`param_2` = 脚本接口对象（虚表），`param_3` = 记录/上下文；槽位（推断）：`+0xd0/+0xd8` 读进度、`+0xf0` 普通写、`+0x100` 完成写、`+0x118` 推送计数进度、`+0x188` 状态码、`+0x410` 对话页、`+0x5d8` 刷新、`+0x1c0/0x1d0/0x1f0/0x1f8` CombineTask 分量/产物/检查。

- **SimpleTalk**：`FUN_180cab520` 单步（状态 ∈ {0,10} 才推进，写 `0x3eb/0x3ec/0x3f4/0x3f5` 后发对话页）；`FUN_180cabb10` 多步链（状态变量 `0→1→2`，完成态 `0x15..0x18`；状态码 `0x548/0x69d/0x7f2` 步长 341，`0x948/0x99d/0x9f2/0xa47`，`state == param_4 → 0x947`，与客户端摘要行的映射尚未闭环，Phase 8 必须先闭环）；`FUN_180caca90` 报告步。
- **CombineTask**：`FUN_180caac10`（`0x100` 结束进度 → `0x5d8` 刷新 → 写 8 个分量 → `0x1f8`/`0x1d0` 逐个发放产物 → `0x1f0` 收尾）；`FUN_180caaf00` 分支收尾。
- **客户端摘要契约**：`quest_summary` 每行 = 一个 SECTION 状态，模板变量 `[%3n]`=第 n 个 SECTION 的 visible、`[%3n+1]`=颜色、`[%3n+2]`=计数显示。实测：1102 两行(SECTION 0,5)、1517 两行(0,1,5)、1365 两行(0,1,5)、1001 五行(0..4)、1002 十行、2641 四行。**报告行固定 SECTION_5**，`SECTION_5==0` 守卫 = 尚未报告；击杀进度不是独立行，而是在行内显示 `(已杀/上限)`。

---

## 7. 家族覆盖实测（`retail_family_coverage.py`，可重跑）

```
repo 生产任务定义 = 6224
Quest_SimpleHunt.xml         rows=1865  ∩repo= 942 (15.1%)
Quest_SimpleTalk.xml         rows=3152  ∩repo=2223 (35.7%)
Quest_SimpleCollectItem.xml  rows= 262  ∩repo= 178 ( 2.9%)
Quest_SimpleUseItem.xml      rows= 160  ∩repo= 104 ( 1.7%)
Quest_SimpleItemPlay.xml     rows=  43  ∩repo=  15 ( 0.2%)
Quest_SimpleSerialHunt.xml   rows=  16  ∩repo=  10 ( 0.2%)
Quest_SimpleGather.xml       rows=   0  ∩repo=   0   # 表本身是空容器
Quest_CombineTask.xml        rows= 574  ∩repo= 574 ( 9.2%)
data_driven_quest.xml        rows=2510  ∩repo=1508 (24.2%)
模板并集 ∩repo = 5554 (89.2%)      无模板行的仓库任务 = 670
```

`coverage-report.txt`（注册表视角）：registry quests=6814，`registry ∩ repo = 4540`；各族注册命中率 100%（SimpleHunt 1863/1865、SimpleTalk 3151/3152、CollectItem 262/262、UseItem 160/160、ItemPlay 43/43、SerialHunt 16/16、CombineTask 574/574），DataDriven 注册命中 0（它不是注册点驱动，是表驱动）。
`datadriven-coverage-report.txt`（无注册点的 1684 个仓库任务）：DataDriven 覆盖 1508、quest_monster.csv 863、quest_script_monster.csv 32，合计 1569；**仍无任何表 115**（其中 7 个客户端已不存在）。

→ **保留 XML 的 670 = 494（真端有注册点 = 脚本驱动）+ 176（真端无覆盖）**。Phase 12 要把这份清单落成 `retail-xml-retention.tsv`。

---

## 8. 坑位清单（踩过或已识别）

1. counter-grid 必须是**完整笛卡尔积**，START 节点投影字段集合必须完全等于网格字段；否则 `COUNTER_GRID_NODE_FIELDS_MISMATCH` / `INCOMPLETE_PRODUCT`。
2. **定义首个节点必须是接取态（NONE）**：`QuestDefinitionCompiler` 从 `nodes.get(0)` 做可达性遍历，把网格节点排前面会 `UNREACHABLE_NODE`（Phase 5-3 踩过）。
3. 同事件并行边必须给唯一 `priority`，否则 `AMBIGUOUS_TRANSITION`；不同显式 `source` 的边互不冲突。
4. **`Set.copyOf` 迭代顺序不稳定**：生成边/节点前必须 `Collections.sort`，否则同输入产出不同 IR（对拍随机失败）。
5. 自环计数不得自增被 source 投影钉死的字段（`COUNTER_SELF_LOOP_PINS_INCREMENTED_FIELD`）。
6. 真端文件 UTF-16 + 内部 DTD（实体是纯文本替换）；`ScriptDLL64.c` latin-1；客户端 UTF-8。
7. 入仓的 `Quest_SimpleHunt.xml` 已转 UTF-8（640KB），不要从 UTF-16 覆盖。
8. 家族扫描约 4 秒（695 任务），不要为提速缩样本。
9. **击杀步语义顺序由 `param_3` 的 SECTION 序号决定，不是注册点顺序**（1517 反例：注册序 SECTION_1→SECTION_0）。
10. **服务端状态数 ≠ 客户端行数**：1001 客户端 5 行 / 服务端 5 个 script 步；SimpleTalk 2641 客户端 4 行；报告行固定 SECTION_5。
11. `displayNameId` 目前**没有运行时消费方**（`grep displayNameId src/main/java` 只命中 `QuestMetadata.java` / `QuestMetadataFieldMapping.java`），只有测试断言具体数值；真端 `quest.xml` 只给 `STR_QUEST_NAME_Qxxxx` 符号名，客户端解包数据里搜不到数字 id（如 `1102202`）。→ 先确认客户端任务名的实际驱动来源再动它，不要伪造。
12. `RetailPatternAI2Test` 是既有失败（`RetailPatternAI2:893` NPE），与本改造无关。
13. 删 XML 是不可逆动作：必须同时改 `quest_definition_catalog.xml`，并全仓 `grep` 确认没有被测试/脚本引用；分族、单独提交候选。

---

## 9. 常用命令

```bash
# 聚焦门禁（已授权）
mvn -q -Dtest='com.aionemu.gameserver.questEngine.retail.*Test,QuestSimpleHuntRetailContractTest' test
# 生产目录门禁（改接线/删 XML 时）
mvn -q -Dtest='ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest,QuestClientContractGateTest' test

# 家族覆盖（本文件 §7 的数字来源）
python3 -B .agents/summary/scriptdll-quest-driver/retail_family_coverage.py

# 抽取 DLL 函数体
python3 .agents/summary/scriptdll-quest-driver/extract_helper_body.py FUN_180cabb10

# 真端表速查（UTF-16）
python3 -c "import re;print(re.search(r'<id id=\"1102\">(.*?)</id>', open('/Users/mc/IdeaProjects/58Server/Map/XML/Quest_SimpleHunt.xml', encoding='utf-16').read(), re.S).group(1))"
```

产物落位：`.agents/summary/scriptdll-quest-driver/`（脚本加执行位、报告 `.txt`/`.tsv`、结论 `.zh-CN.md`）。

---

## 10. 参考里程碑（可选，不是验收要求）

提示词按"终局状态"写，本节只是降低风险的工作切分参考；zcode 可自行调整顺序，但"每族必须先证等价再删 XML"的纪律不能变。

| 里程碑 | 目标 | 覆盖（仓库口径） | 关键数据/语义 |
|---|---|---|---|
| M1 | 真端元数据层（替换 shell 的 metadata/奖励/前序/种族） | 全局基础设施 | 真端 `quest.xml` 字段映射见 §4.2 |
| M2 | SimpleHunt 收口 + 生产接线（表优先/XML 降级开关）+ 删该族 XML | 942（已证 520） | `FUN_180cb13b0`/`FUN_180caf7c0`/`FUN_180cafa40` |
| M3 | SimpleTalk | 2223 | §6 SimpleTalk 三条 helper 语义 + 2641 四行摘要闭环 |
| M4 | CombineTask | 574 | `FUN_180caac10`/`FUN_180caaf00` + `CRAFT_LIFECYCLE_INCOMPLETE` 门禁 |
| M5 | 小族合集 | 307 = CollectItem 178 + UseItem 104 + ItemPlay 15 + SerialHunt 10 | §4.3 各表字段 |
| M6 | DataDriven | 1508 | `category_acquire_`/`category_progress_`/`value0_*` → 事件类型枚举表 |
| M7 | 收尾 | 保留 670 | `retail-xml-retention.tsv` + catalog 收敛 + 终局报告 |

每个里程碑收口时都跑提示词 §6 的两组 Maven 门禁，并把"真端驱动 N / 保留 XML K + 原因"写进报告。
