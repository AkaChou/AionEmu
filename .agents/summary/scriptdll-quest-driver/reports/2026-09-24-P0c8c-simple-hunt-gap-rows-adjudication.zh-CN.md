# P0c-8c：SimpleHunt 缺口表剩余 121 行逐行裁定（79 ADOPT_RETAIL → 退役 / 42 KEEP_XML 登记）

- 日期：2026-09-24
- 归属：真端任务驱动替换目标（P0c 线）
- 触发：P0c-8b 裁定 A/B 两个大桶后，缺口表剩 **121 行**（D 105 两侧都有差异 + C 13 真端少路由 + F 3 其它）。
  本切片把 121 行**全部裁定到零 UNRESOLVED**，落地（退役 79 个 XML + 登记 42 行稳定保留码），跑门禁收口。
- 口径（不变）：**真端文件是形状权威；客户端解包数据是强二证据；遗留 XML 只是对照（含历史错误）**。
  每个差异必须归为"真端对、XML 错"或"真端缺口 → 保留 XML"，不要求零进度损失。

## 1. 结论

1. **121 行全部裁定，UNRESOLVED = 0**：
   - **ADOPT_RETAIL 79 行**：差异全部可用"同一语义的另一种表示"或"真端为 XML 严格超集/客户端不可观测"解释，
     XML 删除（`Path.unlink()`），真端侧冻结 IR 重算。
   - **KEEP_XML 42 行**：4 类真端缺口（每条都有独立机制证据），XML 保留并在缺口表登记稳定码
     —— `CLIENT_BUTTON_UNWIRED` 11 / `KILL_COVERAGE_LOSS` 28 / `CLASS_SELECTABLE_REWARD` 2 / `XML_EXTRA_REWARD` 1。
     码名不带 `RETAIL_` 前缀：该前缀在本仓的门禁/拒绝表语义里**专指族编译器拒绝码**（如 `RETAIL_MONSTER_UNRESOLVED`），
     而本批 42 行的族编译器**全部合成成功**（`accepted=834`），缺口在真端表覆盖而非编译。
2. **配对判据是一套分级放宽的多重集配对**（`exact → order → blind → npc → dialog → shape → open`），
   每级只放宽一个轴，并同时要求两侧"配对后剩余集合"两侧同时为空的**该级**解释：
   `PACKED_ONLY`（节点打包值完全一致）/ `REWARD_ORDER_LEGACY`（奖励发放顺序归一）/ `STAGE_FLIP_PAIRED`（同包、状态名互换）/
   `ACCEPT_NPC_DRIFT`（接取 NPC 漂移）/ `REPORT_PAGE_DIALOG_LEGACY`（报告页占位 id 或客户端报告页登记命中）/
   `PACK_REBASE_LEGACY`（状态级节点 + 计数记账剥离 + 同步模式归一 + 奖励排序）/ `ROUTE_SET_IDENTICAL`（实际未触发）。
   残余解释器另给出逐行"谁多谁少"的机制归类（`KILL_VARIANT` / `EXTRA_KILL_COUNTER_LEGACY` / `PREMATURE` /
   `BRIEFING_PAGE_LEGACY` / `CLOSE_DIALOG_LEGACY` / `ACCEPT_FLOW_LEGACY` / `RETAIL_EXTRA_CLIENT_ROUTE` / `GRID` / `LEGACY_NODE` …）。
3. **判据可重跑且确定**：同一输入重跑 `p0c8c_retire_gap_rows.py --dry-run` 再得
   `裁定 121 行 = ADOPT 79 + KEEP 42（UNRESOLVED 0）`（与落地那次逐字一致）；落地脚本断言 0 UNRESOLVED，
   有未归类差异即中止、不写盘 —— 不存在"漏判后被静默采纳"的路径。
4. **分类器里最值钱的四条判例（都可复用到后续族）**：
   - **接取链曾挂错 NPC**：旧 XML 把接取对话挂在"交付/目标 NPC"上（2668 / 23705 / 23917 / 28031 / 28037 / 80388 / 80389），
     真端 `acquired_npc_name` 指向交任务的 NPC 之外的接取 NPC ⇒ 按真端采纳。
   - **客户端按钮在真端侧确实无路由 ⇒ KEEP**：客户端 select5 的检查按钮 `20002`（80751 家族 8 行）与报告按钮 `1009`
     （1470/1548/30222）在真端路由集里找不到对应动作 ⇒ 属于"真端缺口"，留 XML（`CLIENT_BUTTON_UNWIRED`）。
   - **真端职业可选奖励尚未被编译器落地 ⇒ KEEP**：真端表用 `<class>_selectable_reward` 表达职业可选奖励
     （11102/28313 两侧都有差异），而现有编译器只认单一奖励块 ⇒ 留 XML（`CLASS_SELECTABLE_REWARD`），
     记为零售编译器缺口而不是"XML 错"。
   - **旧 XML 自相矛盾的击杀网格按真端采纳**：3118 家族"首杀进 REWARD、再杀退回 START"的网格、
     以及 21120 的窄投影（`PACK_REBASE_LEGACY`：4 位 max10 → 真端 6 位 max63）全部按真端形状落地。
5. **落地量**：`quest_definition_catalog.xml` 定义数 -79（与本批删除的 79 个 XML 一一对应，目录＝磁盘，无悬空）；生产裁定表
   `retail-simple-hunt-adjudicated-decisions.tsv` **249 → 328**；裁定行真端侧冻结 IR 重算 **247 → 326**
   （328 − 14112/14123 两行 KEEP）；缺口表 `simplehunt-dialog-route-gaps.txt` **121 → 42**，每行带稳定码。
6. **自因测试面 17 个失败方法 / 10 个测试文件，全部收口**（§3、§6）：
   - **批次早期（8 个方法 / 4 个文件）**：`MirrorRewardProjectionLagContractTest`（3）、`Quest30318To30321RetailAlignmentTest`（1）、
     `Quest11110And1548PostKillReportDialogTest`（2）、`LegacyKillFlowRepairDefinitionTest`（2）。
   - **T3 clean 副本对账后新暴露（9 个方法 / 6 个文件）**：`QuestLegacyMonsterHuntProductionFlowTest`（2：21120/30715）、
     `QuestE2eInfrastructureTest`（1：3118 的续接边夹具）、`PlayerQuestStartEligibilityPortTest`（1：80613 的合取表达）、
     `RetailSimpleHuntFamilyGateTest`（1：新稳定码未登记）、`RewardRowEventTwoRowContractTest`（5：80601/80606 直读退役 XML
     + legacy `var0` 步进/自愈边不复存在）、`QuestRetailStartMetadataGateTest`（1：封顶清单快照过期）。
   - 修复判例统一：**"按 (状态, 打包投影) 定位 + 语义断言不变"**取代写死旧标签/旧计数记账；其中 4 个改动同时构成对采纳判定的
     **独立验证**（真端形状下这些任务的击杀/报告/确认路由必须成立）。
7. **两处元数据轴/门禁面的采纳差异被显式登记**（T3 抓出，不是静默通过）：
   - **封顶例外清单**：80604/80609 旧 XML 带服务器侧 82 级封顶（`quest-start-metadata-retail-cap-exceptions.tsv`），
     真端行 `maxlevel_permitted=UNLIMITED` ⇒ 采纳后 `production==retail`，清单按"只登记仍存活封顶行"的规则移除这两行
     （清单内留注释说明；同级 80602/80603/80605/80607/80608/80610 仍保留 XML、仍在清单内）。
     影响面：只改变 83 级角色（全局配置上限 83，5.8 客户端玩家上限 66）的可接取性。
   - **旧存档行号自愈边**：80601/80606 旧 XML 的 `REWARD/var0=0 → var0:=1` 自愈边在真端网格下不再表达
     （击杀只推进 START 段、`REWARD` 投影恒为领奖行 var0=1，任务书行号改由客户端 `SECTION` 门控推导）⇒
     按 P0c-6 先例登记进 `p0c6-legacy-save-normalization.tsv`（**可选**一次性 DB 归一化），不要求零进度损失。
8. **验收**：`verify_retirement.py` = `catalog=2400 directory=2400 retired=3824 sum=6224 — OK`（目录一致、无悬空生产引用）；
   T1 的 12 个门禁类中 11 个已在 T3 clean 中通过（含 400 s 的族门禁），唯一未跑的是并发会话在飞的 `RetailDataDrivenGateTest`（属并行批）；
   T3 clean 见 §5（**本切片自因新增失败 0**）。

## 2. 机器证据（可重跑）

| 证据 | 内容 |
|---|---|
| `.agents/summary/scriptdll-quest-driver/p0c8c_gap_decisions.py` | 唯一判据来源：头注释列全部类别与判据；逐行输出 `(verdict, basis, evidence, axes)`；含可达性、客户端动作 id、报告页登记、真端表双源解析 |
| `.agents/summary/scriptdll-quest-driver/p0c8c_retire_gap_rows.py` | 落地执行器（**复用分类器，禁止二套判据**）：断言 0 UNRESOLVED → 写裁定表 → 追加生产裁定表 → `Path.unlink()` 删 XML → 重写缺口表为 `id<TAB>稳定码` |
| `.agents/summary/scriptdll-quest-driver/p0c8c-gap-decisions.tsv` | 121 行 owner 记录：`quest_id / verdict / basis / axes / evidence`（evidence 含"客户端可达怪数量、真端缺哪些可达怪、逐类机制计数"） |
| `.agents/summary/scriptdll-quest-driver/p0c8c-gap-shape-census-full.tsv` | 15 列形状普查（6.7 MB）：节点键与转换签名的两侧差异、计数失配、两侧布局字段、两侧击杀形状、**节点全量清单**（用于证明"只是改名"的同构） |
| `.agents/summary/scriptdll-quest-driver/p0c8c-retired-xml-evidence.tsv` | 79 行退役存证：`quest_id / sha256(HEAD blob) / git-history:<path>`（删除前内容可回溯） |
| `.agents/summary/scriptdll-quest-driver/p0c8c-production-shape*.tsv` | 生产视图（XML + 真端 overlay）逐任务形状读数，用于把测试断言改到真端口径（21120/30715/26988/3118/80613/80751/1548/11110/23703） |
| 客户端证据链 | `quest_monster.csv`（`SECTION` 门控 + `name_desc → npc_id`）、`docs/quest/client-dialog-mapping/quest-dialog-action-details.csv`（逐任务客户端动作 id：20000 接取 / 20001 拒绝 / 20002 检查 / 1009 领奖 / 1007 询问）、`quest-dialog-pages.csv`（页面链）、`quest_client_report_pages.tsv`、`quest_client_briefing_chains.tsv` |
| `src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv` | 生产裁定表 249 → **328** 行 |
| `src/test/resources/quest/retail-simple-hunt-adjudicated-ir-fingerprints.tsv` | 裁定行真端侧冻结 IR 247 → **326** 行（冻结模式 `-Dretail.hunt.adjudicatedFingerprintOut=<path>` 重算） |

## 3. 代码 / 数据改动

| 文件 | 变化 |
|---|---|
| `src/main/resources/.../quest_definition/quests/*.xml` | **删除 79 个**（`Path.unlink()`；不迁移内容，存证在 `p0c8c-retired-xml-evidence.tsv`） |
| `src/main/resources/.../quest_definition/quest_definition_catalog.xml` | 同步移除 79 条 `<definition id="N" …/>`；目录＝磁盘（2400 = 2400），无悬空引用 |
| `src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv` | +79 行 ADOPT_RETAIL（按 id 排序去重，既有 249 行零搅动） |
| `src/test/resources/quest/retail-simple-hunt-adjudicated-ir-fingerprints.tsv` | 冻结模式重算 → 326 行 |
| `.agents/summary/scriptdll-quest-driver/simplehunt-dialog-route-gaps.txt` | 121 → **42** 行（每行 `id<TAB>稳定码`，供保留清单登记 `SEMANTIC_GAP:<码>`） |
| `.agents/summary/scriptdll-quest-driver/build_retention_list.py` | 最小改动：缺口表每行可带稳定码（缺省仍 `DIALOG_ROUTE`），来源列区分缺口表与历史拒绝表 |
| `src/test/java/.../definition/MirrorRewardProjectionLagContractTest.java` | 新增 `RETAIL_DRIVEN_RETAIL = {24201, 11110}` 并跳过其镜像奖励行断言：11110 真端驱动（var0 = 饱和计数 10），其镜像 21110 仍是 SimpleTalk 侧 XML（`SEMANTIC_GAP:RETAIL_TALK_CHAIN`）——"镜像必须同形"的前提在混合驱动下不成立 |
| `src/test/java/.../definition/Quest30318To30321RetailAlignmentTest.java` | 改生产视图；前置断言改为"两种表达等价"（`start-conditions(finished)` 或 `prerequisites` 任一成立） |
| `src/test/java/.../definition/Quest11110And1548PostKillReportDialogTest.java` | 改生产视图 + 新增 `startNodesByPack`（按打包值定位网格饱和段），断言口径不变（11110 `var0=10`、1548 `var0=5`、报告页 1352 → 1009 → reward） |
| `src/test/java/.../definition/LegacyKillFlowRepairDefinitionTest.java` | 23703/23705 不再写死 `"started"`，改 `rewardReady(...)` 语义定位；并断言"网格满段节点自身即报告门控，路由不应再带条件"（旧 XML 的 `var1` 门控是历史错误） |
| `src/test/java/.../runtime/QuestLegacyMonsterHuntProductionFlowTest.java` | 21120 / 30715 两个方法改到真端网格口径：新增 `startStepsByPack`；21120 断言"网格步进条件/动作全空 + `PACKET_ONLY` + 十次击杀推进 var0 1..10 + 饱和段报 799291 → reward"；30715 断言"步进空条件 + 报告页 2375 + 1009 → reward"，并**正向锁定**"真端 REWARD 状态不含 `reward --31--> reward` 重开路由"（本批采纳的遗留多余路由） |
| `src/test/java/.../e2e/QuestE2eInfrastructureTest.java` | `unreachableCounterContinuationIsAttributedToItsExecutableSibling` 的夹具从 3118（已真端驱动，网格无"饱和后仍存在的续接边"）改挂 **80751**——保留 XML 的 SimpleHunt 行（`SEMANTIC_GAP:CLIENT_BUTTON_UNWIRED`），其 `required=1` 的 compact counter 仍编出 `VariableBelow(var0, 0)` 不可执行续接边，判定口径与断言完全不变 |
| `src/test/java/.../runtime/PlayerQuestStartEligibilityPortTest.java` | `commaSeparatedSlotEntriesStayInsideOneConjunction` 改为在**两种等价表达**下锁死"槽位内两条目同属一个合取、未被拆成备选"（真端把无后缀 finished 条件编成 `prerequisites` 集合，旧 XML 编成单个 start-condition 组） |
| `src/test/java/.../definition/RewardRowEventTwoRowContractTest.java` | 改生产视图 + `preKillStart`（按打包值定位击杀前 START 段）；80601/80606 的击杀断言改为网格口径（击杀只推进段、无条件无动作；报告路由进 `reward`），自愈边断言改为"真端驱动行不保留行号自愈边，REWARD 状态恒为领奖行"；80255/80256 的 legacy 口径原地保留 |
| `src/test/java/.../definition/QuestRetailStartMetadataGateTest.java` 的封顶清单资源 `src/test/resources/quest/quest-start-metadata-retail-cap-exceptions.tsv` | 移除 80604/80609（真端行 `UNLIMITED`，采纳后 `production==retail`），清单内留注释说明；其余 36 行不动 |
| `src/test/java/.../retail/RetailSimpleHuntFamilyGateTest.java` | 新增 `RETAIL_TABLE_GAPS` 类别（4 个稳定码）与不变量"登记为真端表缺口的行**必须仍能族表合成**"；码名不含 `RETAIL_` 前缀（该前缀在本门禁专指族编译器拒绝码） |
| `.agents/summary/scriptdll-quest-driver/p0c6-legacy-save-normalization.tsv` | 追加 80601/80606 两行：被移除的 `REWARD/var0=0 → var0:=1` 自愈边 → **可选**一次性 DB 归一化 |

**警示（沿用 P0c-8b）**：非 clean 的 `mvn test` 看不见退役断链——被删 XML 会残留在
`target/classes/...quest_definition/quests/`，直读 classpath XML 的测试会继续假绿。**退役类切片只能以 clean 副本 T3 为准。**

## 4. 对拍结果（P0c-8b → P0c-8c）

| 指标 | P0c-8b | P0c-8c | 说明 |
|---|---:|---:|---|
| 缺口表行数 | 121 | **42** | 121 行全部裁定；42 行带稳定码保留 |
| 裁定表行数 | 249 | **328** | +79 ADOPT_RETAIL |
| 裁定行真端侧冻结 IR | 247 | **326** | 328 − 14112/14123（两行 KEEP，无真端侧 IR） |
| catalog 定义数 | — | **2400** | 本批 −79（`p0c8c-retired-xml-evidence.tsv` = 79 行）；并发 DataDriven 批同期也在收敛，**绝对值以实时对账为准**（当前 磁盘 2400 = catalog 2400 = 清单 XML_RETENTION 2400，零差集） |
| 本批删除 XML | — | **79** | 存证 `p0c8c-retired-xml-evidence.tsv`（含退役前 sha256 与 git 路径） |
| 未归类差异（UNRESOLVED） | 0 | **0** | 落地脚本硬断言 |

## 5. 门禁

### 5.1 T3 clean（唯一"零新增失败"证据）

命令（**仓库外隔离副本** `/private/tmp/aion-p0c8c`，rsync 排除 `/target`、`/.git`、`/aion`、`/patch`、`/log`，含 `.agents`）：

```
cd /private/tmp/aion-p0c8c
mvn -o -B test -Dtest='com.aionemu.gameserver.questEngine.**,!com.aionemu.gameserver.questEngine.retail.RetailDataDrivenGateTest' \
    -DfailIfNoTests=false -DforkCount=2
```

（排除项 `RetailDataDrivenGateTest` 是并发会话（P5-1 DataDriven 批）的在飞门禁，单次校验 >13 分钟；非本线文件。）

_§5.1 结果：_

| 运行 | 结果 | 日志 | 说明 |
|---|---|---|---|
| 基线（P0c-8b 收口） | 1960 例 / **29F** / **64E** / 1 skipped | `gates/T3-p0c8b-clean2.log` | 本切片的对账基准（失败方法集 93 行） |
| P0c-8c 首跑（本批修复前） | 1961 例 / **36F** / **76E** / 1 skipped | `gates/T3-p0c8c-clean.log` | 暴露本切片自因面：族门禁码未登记、封顶清单快照、直读退役 XML |
| P0c-8c 收口复跑 | 1961 例 / **30F** / **69E** / 1 skipped | `gates/T3-p0c8c-clean2.log` | **自因新增失败 0** |

失败方法集（`comm` 逐行对账，按类名前缀归一化）：

- **新增 8 个，全部为并发会话 DataDriven 批（逐类点名其任务 id 全落 `p5-datadriven-decisions.tsv`）**：
  `ItemCollectingDialogProtocolAlignmentTest` 2（直读 `quests/80745.xml`、`80945.xml`）、
  `Quest80875RetailAlignmentTest` 1（80875）、`ReportRowRewardProjectionContractTest` 4（19672）、
  `QuestRefactorRepairRegressionTest` 1（80798）。
- **消失 2 个**：`GrowthQuestDialogPageAlignmentTest.abbeyGrowthQuestsUseTheClientSelectNoneStartPage`、
  `QuestClientContractGateTest.productionQuestDialogsDoNotIntroduceFatalClientContractRegressions`（并发批与本线修复共同收敛）。
- **本切片自因新增失败 = 0**；收口复跑中 `RetailSimpleHuntFamilyGateTest` **1 例 / 0F / 391.1 s 绿**（含新增的真端表缺口类别）。

排除项说明：`RetailDataDrivenGateTest` 属并发会话（P5-1 DataDriven 批）的在飞门禁，单次校验 >13 分钟，
按 P0c-8b 先例排除；T1 的其余 11 个门禁类均已包含在本次 T3 内并通过。

### 5.2 T2（命中任务 ID 的全部测试类）

见 §6 的收口记录：本批暴露 8 个自因失败方法，逐类修完后复跑 3 个受影响测试类**零新增失败**。

### 5.3 T1（固定全局门禁）

（详见 §6 收口记录。）

## 6. 收口记录（自因失败 8 个方法的修复口径）

| 失败方法 | 原因 | 修复口径 |
|---|---|---|
| `MirrorRewardProjectionLagContractTest` ×3 | 11110 真端驱动后，镜像 21110 仍 XML 侧（`RETAIL_TALK_CHAIN`），"镜像同形"前提失效 | 显式跳过 `RETAIL_DRIVEN_RETAIL` 配对并注释缺口码 |
| `Quest30318To30321RetailAlignmentTest` | 前置条件表达从 `start-conditions` 变为 `prerequisites` | 断言改为"两种表达等价"（任一成立） |
| `Quest11110And1548PostKillReportDialogTest` ×2 | 旧标签 `k1`/`started` 不存在 | 新增 `startNodesByPack`，按打包值定位饱和段 |
| `LegacyKillFlowRepairDefinitionTest` ×2 | 23703/23705 旧标签 + `var1` 门控 | `rewardReady(...)` 语义定位；断言"路由不应再带条件" |
| `QuestLegacyMonsterHuntProductionFlowTest.quest21120ReachesTurnInOnTheTenthKill` | 旧 XML 计数自环 + 4 位 max10 布局 | 网格口径（步进空条件/动作 + 十次击杀 + 饱和段报告） |
| `QuestLegacyMonsterHuntProductionFlowTest.quest30715UsesTheClientSelect5ReportAfterTheRetailKill` | 旧标签 + `VariableBelow` 计数条件 + 旧 `reward --31--> reward` 路由 | 网格口径 + 正向锁定采纳后的 REWARD 状态路由集 |
| `QuestE2eInfrastructureTest.unreachableCounterContinuationIsAttributedToItsExecutableSibling` | 3118 真端网格没有"饱和后仍存在的续接边" | 夹具改挂保留 XML 的 80751（`required=1` compact counter 仍编出 `var0<0` 不可执行续接边），断言不变 |
| `PlayerQuestStartEligibilityPortTest.commaSeparatedSlotEntriesStayInsideOneConjunction` | 真端把无后缀 finished 条件编成 `prerequisites`（集合）而非 start-condition 组 | 两种等价表达下都锁死"同属一个合取、未拆备选" |
| `RetailSimpleHuntFamilyGateTest.familyCompilesWithoutShell` | 42 行 KEEP 的稳定码未登记 ⇒ 门禁把它们判为"既非编译器缺口也非等价缺口" | 新增 `RETAIL_TABLE_GAPS` 类别（4 码）+ 断言"缺口在真端表不在编译"；`KILL_COVERAGE_LOSS` 去掉 `RETAIL_` 前缀以免与族编译器拒绝码混淆 |
| `RewardRowEventTwoRowContractTest` ×5 | 80601/80606 退役后该测试直读 `quests/8060*.xml`；且其断言的 legacy `var0` 步进/自愈边不再由真端表达 | 改生产视图 + `preKillStart` 语义定位；击杀断言改网格口径；自愈边改登记式断言（真端驱动行不保留行号自愈边）+ `p0c6-legacy-save-normalization.tsv` 登记 |
| `QuestRetailStartMetadataGateTest.maxLevelsMatchTheRetailContractWithSentinelsAndCapLedger` | 80604/80609 采纳后不再是"存活封顶行"，封顶清单仍是旧快照 | 清单移除这两行（附注释；语义差异见结论 7） |

## 7. 未验证 / 阻塞 / 下一步

- **未验证**：
  - 数据库持久化路径（真端驱动后的存档读写往返）与真实客户端抽检不在本轮范围；仅以 §5 的门禁与形状断言为准。
  - KEEP 42 行的"真端缺口是否可在真端侧补齐"未评估（如职业可选奖励的编译落地、客户端按钮在真端表的等价路由）。
  - 80604/80609 的 82 级封顶与 80601/80606 的旧存档归一化属**可选**服务器侧策略，本轮只登记不执行。
- **并发观察（不触碰）**：本批期间并发 DataDriven 批同期在收敛——其 10 行（`19010…29034`）一度出现
  "清单标 `RETAIL_TABLE` 但 XML 仍在磁盘/catalog"的不一致，随后（22:04）由该批改登记为
  `XML_RETENTION/SEMANTIC_GAP:RETAIL_HANDIN_VOCABULARY_UNSUPPORTED`，本线重算保留清单后
  **磁盘 2400 = catalog 2400 = 清单 XML_RETENTION 2400 零差集**；其未收口的 5 个测试类
  （80745/80945/80798/19672/80875 的直读退役 XML）已在 §5.1 点名为并行批自因面。
- **下一步**：SimpleHunt 族收口（缺口表 42 行 + 稳定拒绝码 108 行 + 驱动覆盖 2 行的族级对账），
  以及 SimpleItemPlay（15 行真端表已入仓）的形状普查。
- **待 bank 所有者提升的可复用结论**（`.agents/memory-bank/*` 是并发会话禁改区，本线只登记不写）：
  ① 批量退役落地时**新增的保留码必须同时登记进对应家族门禁的类别白名单**（否则门禁报"保留原因既非编译器缺口也非等价缺口"），
  且码名**不得带 `RETAIL_` 前缀**——该前缀在族门禁/拒绝表语义里专指"族编译器拒绝码"；
  ② 采纳真端形状会**连带改变非 IR 轴**（服务器侧封顶清单、旧存档自愈边、聚合/元数据表达），
  这些轴不在"节点/转换"普查里，必须靠 **T3 clean 副本对账**才暴露 ⇒ 退役类切片的验收只认 clean 副本。
