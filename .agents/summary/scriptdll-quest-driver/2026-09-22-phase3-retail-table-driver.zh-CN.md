# Phase 3：真端「表 + 类」驱动规格（本地已具备全部数据）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-22；性质：只读分析（未改生产代码）；上游：Phase 1（注册表）、Phase 2（驱动语义）
- 新增脚本：`report_retail_table_coverage.py`；结论文件：`retail-table-coverage.txt`、`datadriven-progress-schema.tsv`、`datadriven-loader-messages.txt`

## 1. 重大发现：真端服务端模板表就在本地

`/Users/mc/IdeaProjects/58Server/Map/XML/`（UTF-16，2024-02-04 版）就是 `QuestDB_Load` 读取的原始表：

```
Quest_SimpleHunt.xml  Quest_SimpleTalk.xml  Quest_SimpleCollectItem.xml  Quest_SimpleUseItem.xml
Quest_SimpleItemPlay.xml  Quest_SimpleSerialHunt.xml  Quest_SimpleGather.xml  Quest_CombineTask.xml
data_driven_quest.xml  quest.xml(22.7MB, 权威表)  quest_random_rewards.xml  item_quest.xml …
```

也就是说：**任务数据（目标/数量/奖励/前置/等级）不需要任何反编译或推导，本地已有全量真端表**；
需要恢复的只剩「7 个模板族类 + DataDriven 通用驱动」的行为。

## 2. 覆盖率（`report_retail_table_coverage.py`）

| 表 | 行数 | ∩本仓库(6224) |
|---|---:|---:|
| Quest_SimpleTalk | 3152 | 2223 |
| Quest_SimpleHunt | 1865 | 942 |
| data_driven_quest.xml | 2510 | 1508 |
| Quest_CombineTask | 574 | 574 |
| Quest_SimpleCollectItem | 262 | 178 |
| Quest_SimpleUseItem | 160 | 104 |
| Quest_SimpleItemPlay | 43 | 15 |
| Quest_SimpleSerialHunt | 16 | 10 |
| Quest_SimpleGather | 0 | 0 |
| quest.xml（权威表） | 10035 | **6217** |

- **模板表并集 ∩ 本仓库 = 5554 / 6224（89.2%）** —— 这 5,554 个任务原则上可以完全由表驱动，无需 XML。
- 剩 **670 个**无模板行：其中 **494 个**有 per-quest 脚本注册点（Phase 1 registry 已提取参数），
  其余是 115 个既无表也无注册点的（+ 少量注册点在别的 helper）。
- 本仓库有 7 个任务连真端 `quest.xml` 都不存在。

## 3. SimpleHunt 三方对齐实证（表 ↔ DLL ↔ 客户端）

`Quest_SimpleHunt.xml`：

```xml
<id id="1517">
  <acquired_npc_name>Erebos</acquired_npc_name>
  <count1>4</count1><monster1>IDLF3CI_WeA_NagaWi_46_Ae, …48_Ae</monster1>
  <count2>6</count2><monster2>IDLF3CI_KeA_NagaKn_46_Ae, … </monster2>
  <reward_npc_name>Erebos</reward_npc_name>
</id>
```

DLL 注册点：`(counterIndex=2, limit=6)` 与 `(counterIndex=1, limit=4)` —— 与 `count2/count1` **完全对应**：

| 概念 | 真端表 | DLL 注册参数 | 客户端 |
|---|---|---|---|
| 第 N 组击杀目标 | `monsterN` | `param_3 = N`（决定 SECTION_(N-1)） | `quest_monster.csv` 的怪物名列表 |
| 第 N 组需求数量 | `countN` | `param_4 = countN` | `SECTION_(N-1)<countN` 守卫 + `([%3N-1]/countN)` |
| 完成值 | — | `param_5 = Σ countN << 6(N-1)` | — |
| 接取/报告 NPC | `acquired_npc_name` / `reward_npc_name` | 描述符指针（`.data` 由表填充） | `QUEST_Q*.html` 的对话页 |
| 续接任务 | `con_quest` | — | 与 `quest.xml` 的 `finished_quest_condN` 互补 |

1102 (`count1=3, monster1=CherubimL_1_n/2_n`) 与 1365 (`count1=5/count2=5`) 同样一致。
→ Phase 2 的「行为来自类、参数来自表」得到**逐字段实证**。

## 4. DataDriven 驱动规格（覆盖 2,510 行 / 本仓库 1,508）

### 4.1 步骤定义 = `progress_info/data` 列表

```xml
<data>
  <category_progress_>Talk</category_progress_>
  <value0_progress_>LF5_Mosphera_E</value0_progress_>      <!-- 主目标 -->
  <value4_progress_>Movie 32, HACTION_SELECT1_1_1</value4_progress_>  <!-- 演出 -->
</data>
```

步骤顺序 = `<data>` 出现顺序（10501 有 6 步）。分类（含大小写变体，加载器按名匹配）：
`Hunt / Talk / CollectItem / PVP / EnterArea / ItemPlay / EnterWorld / TalkFOBJ`（+ `Collectitem / Itemplay / Pvp / Enterworld`）。
各槽位使用情况见 `datadriven-progress-schema.tsv`，槽位语义（据样例推断）：

| 槽位 | 样例 | 推断语义 |
|---|---|---|
| `value0` | `LF5_Mosphera_E` / `210040000` / `5` | 主目标：NPC/FOBJ 名、地图 ID、数量 |
| `value1` `value2` | `QUEST_1817B 3` | 任务条件（任务名 + 状态） |
| `value3` | `210050000 1440 408 553 77` | 传送：地图 + 坐标 |
| `value4` | `Cutscene 940` / `Movie 30, HACTION_SELECT6_1` | 播放演出/影片 + 页面 |
| `value5` | `Relative LF5_Q_B_Vri_Hide_As_57_An, 1, 60` | 生成 NPC（相对/绝对 + 数量 + 存活时间） |
| `value6` | `5` / `8` | 计数/选项 |
| `value7` | `STR_QUEST_SAY_LF4_04` | 台词/广播字符串 |
| `value9` `value10` | `12, 300190000, 7, QUEST_10032A, QUEST_10032B` / `120, 7, 1` | 多段复合参数（时间、地图、任务列表、限时） |

### 4.2 处理器词表（来自 DLL 内开发者错误字符串，`datadriven-loader-messages.txt`）

`LoadProgressInfo`（推进条件）：Hunt Progress / Talk Progress / Talk FOBJ / Collect Item / Item Play /
PvP Kill Num / PvP Target Level Gap / PvP Target Min Rank / PvP Target Max Rank /
Enter World / Enter Area / Enter Sensory Area / Enter Instance / Level Up(LogIn)

`LoadExtraAction`（附带动作）：Spawn Npcs / Teleport To / Play Cutscene / Give/Remove Items /
Message / Add Timer / Delay Time

各动作的关键参数也都由错误串暴露，例如
`Add Timer - Timer Time / Dest Progress`、`Enter Instance - Ins World ID / Ins Creation ID / Leave Progress`、
`Play CutScene - CutScene Type / ID / Hyper Link ID`、`Spawn NPC - NPC Name ID / Spawn Type`。

## 5. 落地路线（Phase 4 建议）

1. **离线生成器（风险最低、先做）**：把 8 张模板表 → 转成本仓库已有的 `quest-definition` XML 或直接生成 definition 对象，
   配「表 ↔ 现有 XML」等价门禁；用 1102/1517/1365 等做像素级对账。
   → 覆盖 5,554 个任务里的模板部分（SimpleTalk 2,223 + SimpleHunt 942 + Combine 574 + 其余）。
2. **DataDriven 运行时**：按 §4.2 的 20 个处理器实现通用驱动 → 再覆盖 1,508 个。
3. **per-quest 脚本**：494 个任务用 Phase 1 registry 的注册参数驱动（9 个 helper 家族）。
4. **权威表接入**：`quest.xml`(10,035) 作为奖励/前置/等级的唯一真值（本仓库已有 6,217 个），
   可直接生成 PREREQ 门禁的期望值，替代目前手工维护的 TSV 快照。

## 6. 未决问题

- DataDriven `valueN` 的精确语义需按分类逐个反查 loader 分支（本报告为样例推断）。
- 脚本型 494 个任务的注册参数语义（9 个 helper）尚未解码，属 Phase 1 的 `SCRIPTED(无模板)` 组。
- `Quest_SimpleGather.xml` 为空表，`Quest_SimpleSerialHunt` 仅 16 行，需确认是否为本版本废弃族。
