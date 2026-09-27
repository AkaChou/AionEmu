# P0c-6：SimpleHunt 网格族 `talk_npc1` 简报步骤接线（23 行裁定：21 退役 / 2 保留 XML）

- 日期：2026-09-24
- 归属：真端任务驱动替换目标（P0c 线）
- 触发：P0c-5 收尾时登记的队列项——SimpleHunt 网格族 23 行 `talk_npc1` 的保留原因仍是
  `SEMANTIC_GAP:DIALOG_ROUTE`（`phase5-3-rejections.txt` / `simplehunt-dialog-route-gaps.txt` 旧登记），
  缺口正是**缺失的简报步骤**；串行族（P0c-5）已证明该形状可从真端表 + 客户端契约推出。

## 1. 结论

1. 真端 `Quest_SimpleHunt.xml` 里 23 行（宇宙内）带 `talk_npc1`，与 P0c-5 的串行族同构：
   客户端任务书 `select2` 页链 + `quest_monster.csv` 的 `SECTION_5==0` 击杀行门控。网格合成器
   （`RetailSimpleHuntDefinitionCompiler.build`）补齐接线：`SECTION_5` 标志位（bit 30，宽 1）、
   `started`（标志 1）→ 简报链（`QUEST_SELECT` 开 select2；末按钮 SETPRO1/SETPRO2 清位）→
   计数网格零段节点，另按客户端契约补**报告页**与**接取延续页**出口。
2. 23 行逐行裁定（`retail-simple-hunt-adjudicated-decisions.tsv` 70→**93** 行）：
   **21 行 ADOPT_RETAIL**（退役 XML，真端侧冻结 IR 91 行）；
   **2 行 KEEP_XML**——`SEMANTIC_GAP:QUEST_SPAWN_UNEXPRESSED`（新稳定码，见 §4.2）。
3. 被移除的 16 条"存档修复边"（12 个任务）按 P3 既有裁定不迁移，改登记**可选**一次性 DB 归一化清单
   （`p0c6-legacy-save-normalization.tsv`）：真端形状下 `var0` 是击杀计数、任务书行由客户端
   `SECTION_n` 门控推导，服务端不再有"行号"可漂移。
4. 客户端对话审计（23 行按真端独占装载）：**fatalRows=0**；`QuestClientContractGateTest` 绿。
5. 退役后暴露的悬空测试（含 T2 首次运行时被 `target/` 陈旧资源掩盖的 6 个类）全部收口：
   15 个测试类重指生产视图或改写到真端形状；临时探针用后删除。
6. **验收四件套全绿**：23 行逐行裁定 ✅；`verify_retirement.py` = `catalog=3448 directory=3448 retired=2776
   sum=6224 — OK` ✅；T1 **40/0F** + T2 **174/1F**（唯一失败 = 既有基线 1309）✅；T3 clean **1958 例 / 8F / 13E**，
   失败方法集与 `gates/T3-p0c5-clean.log` 归一化 `comm -3` **零差异 ⇒ 自因新增失败 0** ✅（§5.1）。

## 2. 真端证据

| 证据 | 内容 |
|---|---|
| 真端表 `Quest_SimpleHunt.xml` | 宇宙内 23 行 `talk_npc1` 非空且唯一解析 NPC；例：3329 `Zinas→203956`、13702 `LDF4_Advance_Leander→802352`、24112 `DF1A_Brodir_E→832821`、24155 `Gwendolin→204785` |
| 客户端 `quest_monster.csv` | 23 行击杀行门控均含 `Progress(SECTION_0<k; SECTION_5==0)`——标志位未清时击杀行不可见（与串行族同一门控） |
| 客户端任务书页链 | `quest_client_briefing_chains.tsv`（3225 行，生成器 `build_quest_client_briefing_chains.py`）：`select2` 首个同名按钮续页，末 hop 必为 SETPRO 家族（SETPRO1 10000 1912 行 / SETPRO2 10001 403 行…）；23 行的链**全部终结于 SETPRO1/SETPRO2** |
| 客户端报告页 | `quest_client_report_pages.tsv`（5995 行）：承载 1009 动作的页面；简报行全部推导为 **select5 = 2375**；对照：286 个等价冻结行与 70 个哨兵裁定行全部推导为 1352（零搅动） |
| 仓库内既有先例 | 14112/24155 的旧网格 XML 已在 bit 30 携带 `var5`（宽 1）；24155 的简报末按钮即 SETPRO2——与真端表 + 客户端契约互证 |
| 对照实验 | 286 等价冻结行 + 70 哨兵裁定行：无一行带 SETPRO1/SETPRO2 简报文，摘要槽位均为 "0,15" → 该特征可区分简报形状 |
| 14123 击杀目标 | `hippolyta_Q14123 → 206360`（`Peddler Hippola`，`name_desc=hippolyta_Q14123`）；ScriptDLL `fun/fun_196.cpp:933` 只登记 `hippolyta_Q14123 → 0x372b(14123)` 名字→任务绑定，无放置信息 |
| 14112 交付 NPC | `Soul_Kato → 203195`（name_desc `Soul_Kato`），旧 XML 用 `<spawn-npc-at-player slot="kato">` ×3 刷到玩家身边 |
| 刷怪可达性普查 | `p0c6-spawn-reachability-census.tsv`：全登记族 942 行 × 3 角色（击杀/接取/交付）；**"本任务刷怪边是该 NPC 进世界唯一途径"的行恰为 2 行（14112 交付、14123 击杀）**，两行均已按 `QUEST_SPAWN_UNEXPRESSED` 保留 XML（不变量校验 0 违例） |

## 3. 代码改动

| 文件 | 变化 |
|---|---|
| `retail/RetailClientBriefingChains.java`（新） | 客户端简报页链只读视图（`quest_client_briefing_chains.tsv`：`quest_id / entry_page / action:page…`；`Hop.terminal()` = pageId 0） |
| `retail/RetailClientReportPages.java`（新） | 客户端报告页登记（`quest_id / report_page`，1009 动作承载页） |
| `retail/RetailSimpleHuntDefinitionCompiler.java` | `compile(...)` 增 3 个客户端契约参数；`requireBriefing(...)` 稳定拒绝码 `RETAIL_TALK_NPC_UNRESOLVED` / `RETAIL_TALK_NPC_AMBIGUOUS` / `RETAIL_BRIEFING_CHAIN_MISSING` / `RETAIL_BRIEFING_TERMINAL_UNEXPECTED` / `RETAIL_BRIEFING_SLOT_CONFLICT`；`build(...)` 增 `var5`（bit 30 宽 1）、`started` 节点、`briefingFlow(...)`、报告页参数化、`acceptContinuation(...)`；接取流删除交付 NPC 在 NONE 态的 SELECT1/FINISH_DIALOG 分支（无真端形状来源，286 冻结行均满足 reward==acquired） |
| `retail/RetailSimpleHuntPlan.java` | record 增 `briefingNpcIds` / `briefingNpcName`；`bind()` 解析 `talk_npc1`（名字索引唯一解析） |
| `retail/RetailQuestDriver.java` | 加载并透传 `RetailClientBriefingChains` / `RetailClientReportPages` |
| 资源：`quest_client_briefing_chains.tsv` / `quest_client_report_pages.tsv`（新） | 3225 / 5995 行，生成器 `build_quest_client_briefing_chains.py` / `build_quest_client_report_pages.py` |
| 测试（15 类） | `CounterChainBriefingStageContractTest`（语义化节点查找 + 24112 无修复边）、`JournalRewardRowRepairContractTest`（16900–16903/24201 移入 RETAIL_DRIVEN）、`Batch37TalkKillReportRowLadderContractTest`、`MirrorPairRewardRowContractTest`（16960/26960）、`MirrorRewardProjectionLagContractTest`（24201）、`QuestMonsterProgressContractAuditTest`（24153）、`LegacyKillFlowRepairDefinitionTest`（23702）、`QuestMovieAndDialogLoopRegressionTest`（24155）、`QuestPacketOrderRegressionTest`（24153）、`QuestBatchReportNpcAlignmentTest`（13702 网格名）、`QuestRefactorRepairRegressionTest`（24155 同步模式）、`QuestEnterZoneStartOwnerRegressionTest`、`Quest14112LogoutPersistenceTest`、`Quest14123ZoneSpawnTest`（后两类 = KEEP_XML 证据锁 + 生产视图）、`RetailSimpleHuntFamilyGateTest`（新增 `DRIVER_COVERAGE_GAPS` 桶：编译成功但族表无该语义列的行） |
| 数据 | `retail-simple-hunt-adjudicated-decisions.tsv` 70→93；`retail-simple-hunt-adjudicated-ir-fingerprints.tsv` 70→**91**（其中 21 行本批；KEEP_XML 的 14112/14123 **不在**表内）；`retail-xml-retention.tsv` 重算（RETAIL_TABLE 2778→**2776**、SEMANTIC_GAP 1253→**1255**）；catalog **3448**（14112/14123 恢复） |
| 生成器 | `p0c6_build_hunt_briefing_decisions.py` 的 `xml_dialog_axes()` 改为**工作树优先、退役行回退 `git show HEAD:`** 并标注来源：23 行的"零售对 / XML 错"对比轴在 XML 删除后仍可复核（重生成仅动第 5 列 21 行，前 4 列与 93 行集合零搅动） |

## 4. 漂移裁定（真端优先）

### 4.1 21 行 ADOPT_RETAIL（退役 XML）

逐行证据（`retail-simple-hunt-adjudicated-decisions.tsv`，basis=BRIEFING_STEP，axes=RETAIL_BRIEFING_CHAIN，
evidence 机器推导）：真端 `talk_npc1` 名→id、acquired/reward、计数器、客户端链与终结符、报告页、
击杀门控 `SECTION_5==0=y`、旧 XML 对话轴。旧 XML 的对话轴缺陷（多出接取 NPC 的
SETPRO1/SELECT2_1 路由、报告页混用 select2）属 XML 历史内容，由客户端契约 + 真端侧冻结 IR 取代。

### 4.2 2 行 KEEP_XML：`SEMANTIC_GAP:QUEST_SPAWN_UNEXPRESSED`

| 任务 | 缺口 | 证据 |
|---|---|---|
| 14112 | **交付 NPC** `Soul_Kato`(203195) | 本服 `spawns/**` 无静态 spot；击杀目标 `SlimeQ_18_An`(210318)、接取/对话 NPC 均有静态 spot；旧 XML 的 `spawn-npc-at-player slot=kato template-id=203195` ×3（接取/登入/领奖态）是该 NPC 进世界的**唯一**途径 |
| 14123 | **击杀目标** `Peddler Hippola`(206360) | 本服 `spawns/**` 无静态 spot、Java 侧零引用；旧 XML 的 `spawn-npc-current-or-default slot=peddler-hippola template-id=206360 world-id=210020000 …` ×3 是唯一途径 |

裁定依据：真端家族表**没有刷怪列**；可用的真端源里，开放地图世界文件（`58Server/Map/Worlds` 只有实例/事件世界）
不含 Eltnen 的放置、客户端 `Quest.pak` 无 14112/14123 的 NPC 名字、ScriptDLL 只登记名字→任务绑定。
因此这是"**真端无法表达**"的降级行（提示词 §DoD 允许），必须保留 XML——删除会让任务不可完成。
反证（不是缺口的旁证）：`p0c6-spawn-reachability-census.tsv` 显示另有 53 行击杀目标无静态 spot，
但其退役 XML 也**没有**刷怪边（AionEmu 从未让其出场，多为实例 boss 由 Java 处理器刷）→ 退役不改变可达性。

### 4.3 报告页 / 接取延续页（客户端契约修正）

* 报告页由固定 select2 改为 `clientReportPages.reportPage(questId)`（简报行 = 2375/select5）——旧形状
  在 24112 这类任务上把**简报页**当报告页下发（审计探针暴露的缺陷之一）；
* 接取页延续按 `clientDialogExits.requires(questId, SELECT1_1[_1])` 补出（与既有出口登记同源）；
* 交付 NPC 在 NONE 态的 SELECT1/FINISH_DIALOG 出口删除：无真端/客户端来源，且 286 冻结行
  都是"接取者 = 交付者"。

### 4.4 存档修复边移除（P3 既有裁定）× 16 条 / 12 任务

`p0c6-legacy-save-normalization.tsv` 逐条登记（含 `quest_vars` 位段口径：`var0`=bit 0..5、
`var5`=bit 30）。**不要求零进度损失**；如需存量零损，按该表做一次性 DB 归一化（可选，未执行）。
受影响的旧存档形态：① 旧 XML 把 `var0` 当"任务书行号"的 REWARD 存档（如 16900 的 `var0=1` 应为计数 1 而非行 2）；
② 24112 的"未听简报先杀怪"（`var0=1,var5=1`）与旧领奖投影（`var0=0`）；③ 24153 的五段计数补齐。

## 5. 验证

| 档 | 选择器 | 结果 | 日志 |
|---|---|---|---|
| SimpleHunt 族门禁 | `RetailSimpleHuntFamilyGateTest`（942 行族不变量） | **1 例 / 0F / 0E / SUCCESS**（accepted=834、registered=108） | 见 §5.1 |
| 等价 + 冻结指纹 | `RetailSimpleHuntEquivalenceGateTest` | **2 例 / 0F / 0E / SUCCESS**（286 等价行零漂移；裁定冻结 91 行 = 登记集合） | 见 §5.1 |
| 退役收口 | `verify_retirement.py` | **OK**（catalog 3448 = 目录 3448、retired 2776、sum 6224、无悬空引用） | 见 §5.1 |
| 客户端对话审计 | 23 行真端独占装载 | **fatalRows=0**（对照：现行 XML 装载同为 0） | `p0c6-briefing-audit-probe.txt`（探针已删） |
| T1 | 固定门禁清单（含 `QuestClientContractGateTest`） | **40 例 / 0F / 0E / SUCCESS / 23s**（收口复跑） | `gates/T1-142956.log`（首跑 37 例 `gates/T1-134748.log`） |
| T2 | T1 ∪ 23 行命中类 | **174 例 / 1F / 0E / 37s**（唯一失败 = 既有基线 1309，见 §5.2） | `gates/T2-143024.log`（首跑 168 例 `gates/T2-134831.log`） |
| T3 clean | 整个 questEngine 测试树（forkCount=2） | **1958 例 / 8F / 13E / 1 skipped**（失败集与基线零差异 ⇒ 自因新增失败 **0**） | `gates/T3-p0c6-clean.log` |

### 5.1 T3 clean 对账

命令：`mvn -o -B clean test -Dtest='com.aionemu.gameserver.questEngine.**' -DfailIfNoTests=false -DforkCount=2`。

**结果（`gates/T3-p0c6-clean.log`）：1958 例 / 8F / 13E / 1 skipped / BUILD FAILURE / 6:36**。

| 对账 | 结果 |
|---|---|
| 与 `gates/T3-p0c5-clean.log`（1952 例 / 8F / 13E / 1 skipped）比失败方法集 | **归一化 `comm -3` 零差异**（21 = 21 指纹；唯一文本差异 = `QuestTitleRewardCoverageTest` 断言消息里 `HashMap` 键序） |
| **自因新增失败** | **0** |
| 例数 `+6` | 本切片新增/扩展的门禁（简报链、报告页、族覆盖缺口桶） |
| 并发 build 污染标记（`NoClassDefFoundError` / `ClassNotFoundException`） | **0** |

跑法与两处假失败的排除过程（机器可复核）：

1. **主工作树两次运行作废**：`gates/T3-p0c6-clobbered.log`（测试阶段 `target/test-classes` 被清空，
   出现 `ClassNotFoundException: …RetailSimpleHuntIrCompilerTest`、仅 86 类 / 507 例即收尾）与
   `gates/T3-p0c6-clobbered2.log`（主类 `target/classes` 被清空，32 处 `NoClassDefFoundError`）。
   两次都由**并发会话的构建**清空 `target/`（同时段 `java -agentlib:jdwp` 由 IDE 启动）→ 结论改为在
   **仓库外隔离副本** `/tmp/aion-p0c6-t3` 上验证（`rsync -a --exclude '/target' --exclude '/.git'
   --exclude '/aion' --exclude '/patch'`，用毕整目录删除，不留产物）。
2. **副本首跑的 3 个 only-right 全部定性并消除**：
   - `QuestDialogMigrationGateTest` 读 `.agents/summary/quest/generate_quest_dialog_enums.py`——该文件
     **已入库**，是首份副本漏拷 `.agents` 目录造成的假失败（第二份副本已含，最终运行不再出现）；
   - `QuestClientContractGateTest` 的 2 条 `BUTTON_WITHOUT_ROUTE|14120|14150`（started + NPC + 31 →
     page 1352）与 `QuestTitleRewardCoverageTest` 的 Map 顺序差异属**并发会话在飞的 SimpleCollectItem
     切片**：14120/14150 是 SimpleCollectItem `RETAIL_TABLE` 行，其 1352 页按钮只由
     `RetailSimpleCollectItemDefinitionCompiler`（该时段 314 行未提交改动）产出，本切片的改动面不含该族；
     对方切片收口后最终运行中同样消失（对方在同一台账登记为"14120/14150 已按新形状重算"）。

### 5.2 非本切片的既有失败

**21 个失败方法 21/21 与 `T3-p0c5-clean.log` 逐字相同**（归一化 `comm -3` 零差异），即全部为既有基线：

| 失败 | 归属 | 依据 |
|---|---|---|
| `LegacyTemplateMirrorRouteRegressionTest.legacyTemplateCloseControlsDoNotChangeQuestState` quest 1309 + `Quest1309ClientDialogAlignmentTest` | SimpleUseItem（P3b） | 1309 的保留清单行为 `RETAIL_TABLE/SimpleUseItem/OK basis=USE_ITEM_CANONICAL`，XML 已由该切片退役（`missing quest definition 1309.xml`）；1309 的报告页 SETPRO1 关窗出口属**该族**的客户端出口缺口，已在 §6 登记为 P0c-7 的收敛项 |
| 其余 19 行（`QuestMultistepChainContractTest` ×5、`QuestEventQuestBatchDefinitionTest` ×3、`VeryOldLetter1644DefinitionTest` ×2、`QuestE2eInfrastructureTest` ×2、`EarlyElyosQuestRegressionTest`、`QuestDraupnirNpcVariantContractTest`、`QuestMutationPlannerTest`、`QuestStartItemDefinitionRegressionTest`、`QuestTitleRewardCoverageTest`、`RemainingCapabilityDefinitionTest`、`ReportToManyDialogRouteRegressionTest`） | 既有 PM/测试漂移 + 并发会话在飞工作 | 与 `T3-p0c5-clean.log` 同指纹；本切片改动面（SimpleHunt 编译器 + 客户端登记视图 + 23 行相关测试类）不覆盖这些类 |

## 6. 未落地 / 后续

| 项 | 数量 | 说明 |
|---|---|---|
| 1309 报告页 SETPRO1 出口 | 1 | 属 SimpleUseItem 族出口缺口（不属本切片路径）；本切片同样暴露了"报告页按钮出口"这一类，建议 P0c-7 统一到 `quest_client_dialog_exits.tsv` 口径 |
| 14112/14123 的刷怪轴 | 2 | 若要回归真端驱动，需先把 NPC 放置迁到刷怪层（`spawns/**` 或实例/事件处理器），或由真端源补出放置证据；当前按 `QUEST_SPAWN_UNEXPRESSED` 保留 XML |
| 旧档 DB 归一化 | 16 条边 / 12 任务 | **可选**（`p0c6-legacy-save-normalization.tsv`）；不要求零进度损失 |
| 9622（串行族 `talk_npc1=Rebecca_2`） | 1 | 仍保留 XML（P0c-5 登记），可按本切片方法评估 |
| P0c-7 / P0c-8 | — | 按提示词队列继续（SimpleHunt 剩余 565 行 XML_RETENTION 的逐行裁定面） |
