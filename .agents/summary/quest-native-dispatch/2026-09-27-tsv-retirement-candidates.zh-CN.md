# TSV / Loader 退役候选盘点（Phase 3 输入）

> 车道：`quest-native-dispatch`；用途：**Phase 3（真端表退役 / 页码类 TSV 退场）的输入清单**。
> 本文只盘账、不动任何表与代码；所有"消费者"均为 2026-09-27 在本工作区 **grep 实测的 file:line**。
> 口径两条（很重要）：
> 1. **"值读取点"才算消费者**——只把表对象当参数往下传（`clientReportPages`、`acceptEntryPage` 之类）
>    而函数体从不读其值的，记为"死参数穿线"，不算消费者；
> 2. 判定基准是**全部族 canonical 化完成后**的状态；未收口的族（DD 剩余子面、SimpleTalk 链面）
>    仍然构成"前置未满足"，其表**不能**提前退休。

> **【2026-09-27 22:40 执行现状勘误——先读本块】**
> 本盘点 5 张候选中**已退役 4 张**：① `report_pages`(g1) ② `entry_pages`(g2) ③ `talk_pages`(g3) ④ `briefing_chains`(g4)；
> `EXPECTED_TSV_COUNT` **26 → 22**（每片 −1）。其中两张的**退役范式与本盘点预判不同**（W5 判据升级，见 GOAL §0.5）：
> - ③ 是**活门**：判据换成**任务书行存在性**（`RETAIL_TALK_JOURNAL_MISSING`）；实测**真解锁 6 行**（其中 5 行
>   客户端无 HTML/无任务书 ⇒ 换判据后仍拒；**25200 受理**）。**它才是唯一解锁面**（本盘点把它排在 briefing 之后是错的）。
> - ④ **今日零拒绝**（47 行内宇宙 23 行全有 SETPRO 终点链；六族 389/389；retention 零 `RETAIL_BRIEFING_*`）⇒
>   退役**零受理变化**；不变量迁**构建期常设门** `RetailBriefingChainEvidenceGateTest`（快照 + 基数冻结 + 注册 T1）。
> - 本盘点 §1 表头"25 张 / 清单 27 行"与 §4-0 的 **27** 为**退役前口径**；现值为 **22**。
> - ⑤ `dialog_exits` **仍未动**，且经取证确认**无整旗标死亡** ⇒ 只能**行级 × 旗标可达性缩表**
>   （判据 = 家族门定义快照逐键相同），不是删除候选项。
> - 台账：`2026-09-27-w5g1-…` / `w5g2-…` / `w5g3-…` / `w5g4-…` 四份；范式沉淀 QE-094/095/096。

## 1. 结论总表

> **范围说明（与冻结清单对账，实测双向零差集）**：`static_data/quest_retail/` 磁盘上共 **25 张 TSV**
> （不含清单自身），本盘点全覆盖——§1 表覆盖其中 23 张（第 10、15 行各含 2 个文件），
> §2.4 覆盖剩余 2 张（`dd-fp-fresh.tsv`、`retail-quest-ai-name-groups-rejected.tsv`）。
> 冻结面的另外 2 张在 `src/main/resources/aion/definitions/quest_dialog/`
> （`client_dialog_contract.tsv`、`movie_continuation_exceptions.tsv`，role=`contract`，消费者 `QuestDialogContract`），
> 属客户端对话契约面，**不在本工程四类裁决内，本盘点不动也不建议动**；登记其存在只为对齐
> "清单 27 行 = 25 + 2"，避免退役执行时误判范围。（**退役前口径；现值 22**，见首块勘误。）


| # | 表（`src/main/resources/aion/data/static_data/quest_retail/`） | 数据行 | Loader（`.../questEngine/retail/`，行数） | 当前值读取点（file:line） | canonical 化后失去消费者？ | 退役前置 / 风险 |
|---|---|---|---|---|---|---|
| 1 | `quest_client_report_pages.tsv` | 5995 | `RetailClientReportPages.java`（75） | **唯一**：`RetailSimpleHuntDefinitionCompiler.java:771`（值只在 `canonical=false` 分支 `:791` 消费，而该分支已无调用者） | **是（改为"死读"）** | ① legacy 报告分支（`reportFlow`/`incompleteReportRoutes`）与两个**已无调用者**的公有重载（`RetailSimpleHuntDefinitionCompiler.java:101-106`、`:115-120`，canonical=false）一起退场；② 删除时同步 `RetailQuestDriver.java:80-81/505-506` 与 5 处死参数穿线 |
| 2 | `quest_client_entry_pages.tsv` | 2449 | `RetailClientEntryPages.java`（70） | 3 处**全为死值/死参**：`RetailDataDrivenDefinitionCompiler.java:434`（→ `compileCanonical` 的 `acceptEntryPage` 在 canonical 分支不读）、`RetailDataDrivenTalkHuntChainCompiler.java:427`（形参 `:571` 从不读）、`RetailDataDrivenTalkCollectChainCompiler.java:284`（形参 `:303` 从不读） | **是（现已零效果）** | 需先确认无第三条（未 grep 到的）读取路径；删调用点参数即可，无形状风险 |
| 3 | `quest_client_talk_pages.tsv` | 1340 | `RetailClientTalkPages.java`（73） | `RetailDataDrivenDefinitionCompiler.java:464`（`entryPage()`/`completionPage()` 在 D-a 片后**已不读**＝死值）；**但 `:174` 是受理门**（no-progress 行必须有极简信页登记，否则 `RETAIL_TALK_VOCABULARY_UNSUPPORTED`） | **值已死；门仍活** | 需先改写受理门（否则退役会改变受理集合 → 保留清单/XML 拥有权翻转）；建议与 `RetailClientTalkChainPages` 一起按"DD 剩余子面收口"统一裁定 |
| 4 | `quest_client_briefing_chains.tsv` | 3225 | `RetailClientBriefingChains.java`（115） | 3 处：`RetailSimpleHuntDefinitionCompiler.java:231`（**受理门** `requireBriefing`，canonical 行也会跑：`:178` 在旗标之前）、`:822`（legacy `briefingFlow`，canonical 不读）、`RetailSimpleCollectItemDefinitionCompiler.java:94`（legacy `BriefingStep`，canonical 不读） | **否**（受理门仍在用） | 需先把 `requireBriefing` 的两个形状门（`RETAIL_BRIEFING_CHAIN_MISSING` / `RETAIL_BRIEFING_TERMINAL_UNEXPECTED`）换成 canonical 受理判据；换门会**解锁**原本被拒的行 ⇒ 必须同步 `retail-xml-retention.tsv` owner 与 SimpleHunt 家族门登记（README"级联 #2"同源）。注：retention 表里当前**没有**任何 reason 含 `BRIEFING` 的行（实测 grep=0），解锁面需在换门时现算 |
| 5 | `quest_client_dialog_exits.tsv` | 3938 | `RetailClientDialogExits.java`（91） | **部分**：S1 收口后 `RetailSimpleTalkDefinitionCompiler.java:328-330 / :365-367`（SELECT1_1、SELECT6）归零；`RetailSimpleHuntDefinitionCompiler.java:742-744` 已在 `!canonical` 分支（死）；**仍活**：S2 链面 `RetailSimpleTalkDefinitionCompiler.java:435/523/550/577/798-830`、DD 混合链 `RetailDataDrivenDefinitionCompiler.java:164`（`SELECT_NONE_1`）、`RetailDataDrivenCollectCompiler.java:64` | **否（部分保留）** | 保留；待 S2 与 DD 剩余子面收口后再盘点（届时 `SELECT1_1/SELECT1_1_1/SELECT2_CONTINUE/SELECT5_CHECK/SELECT6` 五个旗标可能整体退场，只留 `SELECT_NONE_1`） |
| 6 | `quest_client_summary_rows.tsv` | 8931 | `RetailClientSummaryRows.java`（73） | 8 处：`RetailSimpleTalkDefinitionCompiler.java:312`、`RetailSimpleCollectItemDefinitionCompiler.java:289`、`RetailSimpleUseItemDefinitionCompiler.java:167`、`RetailDataDrivenCollectCompiler.java:63/80`、`RetailDataDrivenDefinitionCompiler.java:468/471/489`、`RetailDataDrivenTalkHuntChainCompiler.java:408`、`RetailDataDrivenTalkCollectChainCompiler.java:275`（+ `RetailHandinDialogFlowCompiler.java:57` 参数） | **否** | **必须保留**：真端模板表无"任务书行数"列，QE-051 领奖行投影的唯一来源（`RetailQuestDriver.java:65-66` 注释即此口径） |
| 7 | `quest_client_reward_npcs.tsv` | 118 | `RetailClientRewardNpcs.java`（71） | 6 处：`RetailSimpleTalkDefinitionCompiler.java:149/304`、`RetailSimpleHuntDefinitionCompiler.java:560/975`、`RetailSimpleCollectItemDefinitionCompiler.java:194/266` | **否** | **必须保留**：`reward_npc_name` 为 `<地图>_<势力名>` 复合引用时的 dic 链唯一解析通道（`RETAIL_REWARD_NPC_FACTION_COMPOSITE` 门同源） |
| 8 | `quest_client_use_item_report.tsv` | 104 | `RetailClientUseItemReport.java`（86） | `RetailSimpleUseItemDefinitionCompiler.java:153/154/155` | **否** | **必须保留**：真端 `Quest_SimpleUseItem.xml` 只有 `{id, use_item_name, reward_npc_name}` 三列，无报告模式列 |
| 9 | `quest_client_hunt_stages.tsv` | 34 | `RetailClientHuntStages.java`（83） | `RetailSimpleHuntDefinitionCompiler.java:302/349`（`compileSerialChain`/`serialSlots`） | **否** | **必须保留**：串行阶梯的客户端阶段契约（真端表只有 `monster/count` 逐段列，无客户端阶段门控） |
| 10 | `quest_client_kill_targets.tsv` + `quest_client_kill_targets_stages.tsv` | 46 + 8 | `RetailClientKillTargets.java`（133） | `RetailDataDrivenDefinitionCompiler.java:424/426`（`withClientKillTargets`/`withClientStageKillTargets`） | **否** | **必须保留**：真端表只给基础模板名，客户端 SECTION 行的同族 `T_` 实刷变体唯一来源 |
| 11 | `quest_client_hunt_progress_rows.tsv` | 1266 | `RetailClientHuntProgressRows.java`（117） | `RetailDataDrivenDefinitionCompiler.java:433`（计数轴裁定）、`RetailDataDrivenTalkHuntChainCompiler.java:352` | **否** | **必须保留**：单段计数以客户端为准的裁定通道（常设门登记分歧集） |
| 12 | `quest_use_item_npcs.tsv` | 804 | `RetailQuestUseItemNpcs.java`（65） | `RetailSimpleTalkDefinitionCompiler.java:350`（交互物门）、`RetailDataDrivenTalkHuntChainCompiler.java:1135`、`RetailHandinDialogFlowCompiler.java:149` | **否** | **必须保留**：P0c-22 启动期交互对象合同（判例 18509/28509） |
| 13 | `quest_enterarea_zone_resolution.tsv` | 146 | `RetailEnterAreaZoneResolution.java`（73） | `RetailDataDrivenTalkHuntChainCompiler.java:272`、`RetailDataDrivenTalkCollectChainCompiler.java:183` | **否** | **必须保留**：EA 步别名→登记区名解析（EA 步死边防线） |
| 14 | `quest_legacy_heal_rows.tsv` | 2 | `RetailLegacySaveHealRows.java`（89） | `RetailSimpleTalkDefinitionCompiler.java:380` | **否** | **必须保留**：旧存档行号自愈（P0c-28，判例 80290/80294）；真端表无陈旧存档列 |
| 15 | `quest_client_handin_pages.tsv` + `quest_client_handin_exceptions.tsv` | 300 + 7606 | `RetailClientHandinPages.java`（116） | `RetailDataDrivenDefinitionCompiler.java:495`（例外门）、`RetailDataDrivenCollectCompiler.java:57`（→ `RetailHandinDialogFlowCompiler`） | **否** | **必须保留**：HandinDialogFlow 已裁定**不纳入 canonical 化**（客户端五页词汇逐页镜像 + 被 DD 链共享 + 独立合同测试，三票否决） |
| 16 | `quest_client_talk_chain_pages.tsv` | 124 | `RetailClientTalkChainPages.java`（96） | `RetailDataDrivenDefinitionCompiler.java:165/486`、`RetailDataDrivenTalkHuntChainCompiler.java:381`、`RetailDataDrivenTalkCompiler.java:185` | **否（暂）** | 部分保留：DD talk 链（wave B）的页梯仍是形状源；D-a 只改了接取/交付/预览段 |
| 17 | `quest_client_talk_collect_chain_pages.tsv` | 86 | `RetailClientTalkCollectChainPages.java`（120） | `RetailDataDrivenTalkHuntChainCompiler.java:386`、`RetailDataDrivenTalkCollectChainCompiler.java:247` | **否（暂）** | 部分保留：DD 混合链（talk×collect）页梯仍是形状源，属 DD 剩余子面 |
| 18 | `quest_client_talk_chain_steps.tsv` | 4990 | `RetailClientTalkChainSteps.java`（179） | `RetailSimpleTalkDefinitionCompiler.java:169/170/182/183/186/192`（受理）、`:441-463`（R 回放）、`:468`（I 记录）、`:471/492`（B 记录）、`:615`（P 布局） | **否（暂）** | 部分保留：**SimpleTalk 链面（S2）的形状源兼唯一证据**，随 S2 裁定（见 `2026-09-27-simpletalk-canonical-survey.zh-CN.md` §3.4 的 A/B/C 三策略） |
| 19 | `quest_name_string_ids.tsv` | 10161 | （无独立 loader，`RetailQuestDriver.java:115` 常量 + `:891-893` 解析） | `RetailQuestDriver.java:847`/`:860`（`nameIds`）→ `RetailQuestMetadataCompiler`（任务名 string id） | **否** | **必须保留**：任务名国际化映射 |
| 20 | `retail-quest-ai-name-groups.tsv` | 31 | （`RetailNpcNameIndex.build`，`RetailQuestDriver.java:117-118/556-558`） | 同上：`npcIndex` 构建的唯一读入点 | **否** | **必须保留**：守备队同组共用 ScriptDLL 对话名的唯一展开通道（常设门 `RetailQuestAiNameGroupGateTest`） |
| 21 | `retail-xml-retention.tsv` | 6224 | （无 loader，`RetailQuestDriver.java:37-38`） | 驱动归属判定 + **14 个测试类**（`RetailOwnershipGateTest`、`RetiredQuestIds`、9 个 `Retail*GateTest`、`RetailQuestDriverOverlayTest`、`QuestItemPlayGrantGateTest`、`QuestRetailClassGateTest`） | **否** | **必须保留**：owner/family/reason/evidence 的唯一事实源 |

## 2. 四类明细

### 2.1 可退役候选（4 张）

**① `quest_client_report_pages.tsv`（5995 行）** — 最干净的一个

- 全仓实测：`reportPage(` 的**唯一**调用点是 `RetailSimpleHuntDefinitionCompiler.java:771`；
  该行无条件执行，但值只在 `:791`（`canonical=false` 的 `reportFlow`）被消费。
- 生产侧所有 hunt 编译路径均已 canonical：`RetailQuestDriver.java:861`（7 参家族重载 → `canonical=true`，
  见 `RetailSimpleHuntDefinitionCompiler.java:91-92`）、`compileCanonical`（DD 单段，`:131-137`，canonical=true）、
  `compileSequentialStages`（DD 顺序链，`:154-161`，canonical=true）、`compileSerialChain`（串行链）。
- **`canonical=false` 的两个公有重载已无任何调用者**（`:101-106` 八参、`:115-120` 九参；全仓
  `RetailSimpleHuntDefinitionCompiler.compile(` 只有 3 处命中，且都是 7 参 canonical 形：
  `RetailQuestDriver.java:861`、`RetailSimpleHuntEquivalenceGateTest.java:347`、
  `RetailSimpleHuntFamilyGateTest.java:160`）⇒ 旧报告路径**现在**就是生产死代码，本表**今天**已无有效读者。
- 其余线程全是死参数：`RetailDataDrivenDefinitionCompiler.java:42/49/63/287/448/451`、
  `RetailDataDrivenTalkHuntChainCompiler.java:248/426/570`。
- 退役前置：① 删两个死重载 + legacy `reportFlow`/`incompleteReportRoutes`（`RetailSimpleHuntDefinitionCompiler.java:1207-1217`/`:1179-1205`）；
  ② 删 `RetailQuestDriver.java:80-81`（常量）与 `:505-506`（加载）及 5 处死参数穿线。

**② `quest_client_entry_pages.tsv`（2449 行）** — 已经零效果

- 三个读取点全部不产生行为：DD hunt 的 `entryPage`（`RetailDataDrivenDefinitionCompiler.java:434`）流向
  `compileCanonical`/`compileSequentialStages` 的 `acceptEntryPage`，而 canonical 分支走
  `canonicalAcceptFlow(acquiredNpc, acceptTarget)`（`RetailSimpleHuntDefinitionCompiler.java:739-741`）
  **不用该参数**；两个 DD 链编译器把它当形参接住后**从不读**（`RetailDataDrivenTalkHuntChainCompiler.java:571`、
  `RetailDataDrivenTalkCollectChainCompiler.java:303`）。
- 退役前置：仅需删参数与加载点（`:92-93/516-517`），**无形状/受理风险**；但仍建议与 DD 剩余子面收口同步做，
  避免"删参数"的机械改动与形状改动混在同一片里。

**③ `quest_client_talk_pages.tsv`（1340 行）** — 值已死、门还活

- 值：`RetailDataDrivenDefinitionCompiler.java:464` 的 `pages.entryPage()/pages.completionPage()`
  在 D-a 片后已不被 `RetailDataDrivenTalkCompiler.build/buildItemAcquire/assemble` 读取（该方法签名仍接两个
  `int` 参数但函数体只用 `metadata/completionPage` 之外的入参；`buildChain` 用 `stageLadders` 而非 `entryPage`）。
- 门：`RetailDataDrivenDefinitionCompiler.java:174`（no-progress 行必须登记极简信页，否则
  `RETAIL_TALK_VOCABULARY_UNSUPPORTED`）。**退役 = 改受理门**，会改变保留清单的 owner 分布。**【2026-09-27 结果】已退役（W5-g3）**：门换**任务书行存在性**判据（`RETAIL_TALK_JOURNAL_MISSING`）；实测真解锁 6 行 ⇒ 1 行受理（25200）+ 5 行换码仍拒。见 `2026-09-27-w5g3-talk-pages-retirement.zh-CN.md`。

**④ `quest_client_briefing_chains.tsv`（3225 行）** — 同上，且门更硬

- `RetailSimpleHuntDefinitionCompiler.java:178` 的 `requireBriefing` 在 canonical 旗标生效**之前**执行 ⇒
  canonical 行虽不用页链，**仍被页链登记门卡着**（`RETAIL_BRIEFING_CHAIN_MISSING` /
  `RETAIL_BRIEFING_TERMINAL_UNEXPECTED`，`:231-238`）。
- 另有 `RETAIL_BRIEFING_SLOT_CONFLICT`（`:240+`）与 slot 6 冲突判定，与表无关，是布局不变量（保留）。
- 退役前置：把"页链登记齐备"门换成 canonical 判据（简报 NPC 唯一 + 目标投影可表达"见中间人才开计数"），
  并同步 retention/家族门。**这是四张候选里唯一会解锁新受理行的**。**【2026-09-27 勘误】该判断反了**：briefing 门**今日零拒绝** ⇒ 换门/退役**零受理变化**；真正解锁面在 ③ `talk_pages`（6 行）。**已退役（W5-g4）**：两处门子句删除 + 不变量迁构建期常设门；见 `2026-09-27-w5g4-briefing-chains-retirement.zh-CN.md`。

### 2.2 必须保留（真端表无对应列，退役即语义缺口）

`quest_client_summary_rows.tsv`（QE-051 领奖行投影）、`quest_client_reward_npcs.tsv`（复合势力交付集）、
`quest_client_use_item_report.tsv`（SimpleUseItem 报告模式）、`quest_client_hunt_stages.tsv`（串行阶段契约）、
`quest_client_kill_targets*.tsv`（客户端实刷变体）、`quest_client_hunt_progress_rows.tsv`（计数裁定）、
`quest_use_item_npcs.tsv`（交互对象合同）、`quest_enterarea_zone_resolution.tsv`（EA 别名）、
`quest_legacy_heal_rows.tsv`（旧存档自愈）、`quest_client_handin_pages.tsv` + `..._exceptions.tsv`
（HandinDialogFlow 三票否决）、`quest_name_string_ids.tsv`、`retail-quest-ai-name-groups.tsv`、
`retail-xml-retention.tsv`。

> 一句话判据：**只要某个值来自"客户端侧事实"（任务书行数、页链按钮、SECTION 门控、变体名单、
> 交付对象 dic 链）而真端模板表没有该列，就必须保留**；反过来，凡是被 canonical 形状替代的
> "服务端页链驱动"产物（入口页、报告页、页链续页），才是退役对象。

### 2.3 部分保留（2 张 + 1 张）

- `quest_client_dialog_exits.tsv`：S1 收口后仍活着的旗标 = `SELECT_NONE_1`（DD 混合链）、
  `SELECT1_1/SELECT1_1_1`（S2 链面）、`SELECT2_CONTINUE`（S2）、`SELECT5_CHECK/SELECT5_CHECK_SIMPLE`（S2）、
  `SELECT6`（S2）。**S2 + DD 剩余子面收口后再盘点**。
- `quest_client_talk_chain_pages.tsv` / `quest_client_talk_collect_chain_pages.tsv`：DD 链字面页梯仍是形状源。
- `quest_client_talk_chain_steps.tsv`：SimpleTalk 链面形状源，随 S2 三策略裁定（推荐"加载期过滤"，
  该表保持不动）。

### 2.4 数据残骸与死代码（4 项）

| 项 | 规模 | 实测证据 | 处置建议 |
|---|---|---|---|
| `dd-fp-fresh.tsv`（**生产资源目录**） | 966 行 | **全仓 0 消费者**（`grep -rn dd-fp-fresh` 在 `*.java/*.py/*.sh/*.md/*.jsonl` 零命中）；id 集是正式表 `src/test/resources/quest/retail-data-driven-ir-fingerprints.tsv`（1217 行）的**真子集**（966 全部在内，251 行缺失），且 **966/966 行指纹与正式表逐字不同**（陈旧快照） | **已在清单登记为 `RETIREMENT_CANDIDATE`（退役前置 = 无）** ⇒ 正规动作 = 删文件 + 删清单行 + `EXPECTED_TSV_COUNT` 27→26，同提交登记理由（**该表已在早前片退场；磁盘与清单现均无此表**） |
| `retail-quest-ai-name-groups-rejected.tsv` | 18 行 | 唯一引用 = 兄弟车道生成器 `.agents/summary/scriptdll-quest-driver/p0c52_quest_ai_name_groups.py:37-40`（`OUT_REJECTED_TSV` 输出路径）；生产 java 0 引用 | **跨车道协商**（脚本属 `scriptdll-quest-driver` 车道）；由该车道决定"停写 / 迁到 `.agents/summary/`" |
| `RetailSimpleHuntIrCompiler`（`src/main/java/.../retail/RetailSimpleHuntIrCompiler.java`） | 444 行 | 生产 0 引用（`grep -rn` 仅命中自身 + 两处 javadoc 提及）；唯一消费者 `RetailSimpleHuntIrCompilerTest`（556 行，15 处调用） | 死代码候选：**迁移期 shell 方案**，被 `RetailSimpleHuntDefinitionCompiler` 取代——后者 Javadoc（`RetailSimpleHuntDefinitionCompiler.java:40-42`）明写"与 `RetailSimpleHuntIrCompiler`（迁移期 shell 方案）不同，本类不再消费 quest-definition XML"。删则连带删测试；建议 Phase 3 与"shell 方案整体退场"一起做，不要单独删 |
| `RetailSimpleTalkGateTest` 类注释 | — | 非代码，但同类"过期文案"还有 `RetailSimpleTalkChainGateTest` 注释里的"83 行 ADOPT"（实际指纹 285 行） | 顺手修注释，避免下一轮按过期数字做判断 |

## 3. 退役顺序（依赖图）

```
第 0 层（不依赖任何形状改造，只需删死代码 + 授权）
   ├─ report_pages 退役：删两个无调用者的 canonical=false 重载 + legacy reportFlow/incompleteReportRoutes
   └─ entry_pages  退役：删 3 处死参（DD definition / DD talk-hunt 链 / DD talk-collect 链）

第 1 层（依赖 SimpleTalk 两片）
   S1(单步 canonical) ──► dialogue_exits 的 S1 两处读取归零；单步面无指纹冻结器（见 survey §4.1）
   S2(链 canonical，需先定过滤策略 A/B/C) ──► talk_chain_steps 与链门指纹重冻；dialogue_exits 的链面旗标退场

第 2 层（依赖 DD 剩余子面 + 受理门改写）
   DD 剩余子面(talk-hunt 链 / talk-collect 链 / 混合链) canonical 化
        ├─ talk_pages 受理门改写（:174） ────► talk_pages 退役  ✅ 2026-09-27 完成（判据=任务书行）
        └─ talk_chain_pages / talk_collect_chain_pages 页梯退场 ──► 退役
   SimpleHunt 简报门改写（:178 requireBriefing）──► briefing_chains 退役（唯一会解锁新受理行）  ✅ 2026-09-27 完成（**零拒绝，非解锁面**；不变量迁构建期）

第 3 层
   dialogue_exits 五旗标退场（仅留 SELECT_NONE_1）► dialogue_exits 缩表（非删除）
```

## 4. 风险与纪律

0. **执行机制已就位（本轮新增，2026-09-27 09:58 落地）**：`quest-retail-tsv-manifest.tsv`
   （生产资源目录内，退役前 27 行登记表；现值 **22**）+ `RetailTsvManifestGateTest`（常设门）把
   `static_data/quest_retail/` 与 `definitions/quest_dialog/` 的 `*.tsv` 冻结为**受控面**：
   ①磁盘集合 == 清单集合（双向零差集）；②每行 4 列、file 唯一、role/status 取自词表；
   ③登记总数 == `EXPECTED_TSV_COUNT`（`RetailTsvManifestGateTest.java:62`，写于本盘点时 **27**；**当前值见 §5 执行回执**：26→25→24→23→**22**）。
   ⇒ **退役的正规动作 = 删文件 + 删清单行 + 常量 −1**（三者必须同一提交、并在任务报告登记理由），
   本文 §1/§2 的每一条"可退役"都必须按这个三步走；清单里已把 `dd-fp-fresh.tsv` 标为
   `RETIREMENT_CANDIDATE`（note 与本盘点独立同结论）。
1. **不删表、不改表**（本轮纪律）：本文是 Phase 3 输入，任何删除都在 Phase 3 单独授权后执行。
2. **门禁同步面**：每张表的退役都会牵动至少一个门禁——
   `RetailOwnershipGateTest`（保留清单全集）、`RetailQuestDriverOverlayTest`（加载器覆盖）、
   `RetailSimpleTalkGateTest`（`SELECT1_1`/`SELECT6` 两处断言）、`RetailSimpleTalkChainGateTest`（登记行分区）、
   `RetailDataDrivenGateTest`（DD 家族门）。退役前逐门确认"该表不再被读"的证据链。
3. **跨车道协作**：`quest_client_talk_chain_steps.tsv`、`retail-simple-talk-chain-ir-fingerprints.tsv`、
   `retail-quest-ai-name-groups-rejected.tsv` 的写入方是 `scriptdll-quest-driver` 车道；
   动它们之前必须按"先查作者"纪律协商，且用 `find -newer` 复验写入窗口。
4. **退役两步走**：`RetailClient*Loader` 的删除与 `RetailQuestDriver` 的构造参数收窄是**两件事**，
   后者会牵动 12 个测试类的夹具构造（`RetailSimpleCollectItemGateTest`、`RetailSimpleHuntFamilyGateTest`、
   `RetailSimpleSerialHuntGateTest`、`RetailNonIrAxisGateTest`、`RetailDataDrivenGateTest`、
   `RetailQuestDriverOverlayTest`、`RetailSimpleItemPlayGateTest`、`RetailSimpleTalkChainGateTest`、
   `RetailSimpleUseItemGateTest`、`RetailSimpleHuntEquivalenceGateTest`、`QuestItemPlayGrantGateTest`、
   `RetailOwnershipGateTest`）。建议顺序：**先"读点归零（删调用/参数）"→ 绿 → 再"删文件 + 删清单行 +
   常量 −1"**，两段各自独立提交，避免形状改动与表集合改动混在同一个评分面里。
5. **假绿防线**：任何一次"表被退役"的提交，都必须在收口报告里给出**该表的读点 grep 证据**
   （`grep -rn "<表名>\|<Loader类名>" src/main src/test` 的零/预期命中），否则无法区分
   "真的没人读了"与"读点被参数穿线藏起来了"（本次盘点已实测出自 2 处隐藏死参数）。

---

## 5. 执行回执（2026-09-27）

| 候选 | 状态 | 证据 |
|---|---|---|
| ① `quest_client_report_pages.tsv` | **已退役**（W5-g1，5995 行；类 + 2 死重载 + 4 死页链流 + 全参数穿线；计数 26→25） | `2026-09-27-w5g1-report-pages-retirement.zh-CN.md` |
| ② `quest_client_entry_pages.tsv` | **已退役**（W5-g2，2449 行；三处读取零效果；计数 25→24） | `2026-09-27-w5g2-entry-pages-retirement.zh-CN.md` |
| ③ `quest_client_talk_pages.tsv` | **已退役**（W5-g3，1340 行；门换任务书行判据；1 行受理 + 5 行换码；计数 23→22） | `2026-09-27-w5g3-talk-pages-retirement.zh-CN.md` |
| ④ `quest_client_briefing_chains.tsv` | **已退役**（W5-g4，3230 行；零拒绝实测 ⇒ 零受理变化；不变量迁构建期常设门 + 注册 T1） | `2026-09-27-w5g4-briefing-chains-retirement.zh-CN.md` |
| `dd-fp-fresh.tsv` | 已在早前片退场（当前磁盘与清单均无此表） | 清单/磁盘双向复核 |
| `quest_use_item_npcs.tsv` | **更正：不退**（实测 808 行有数据 + 6 个生产消费者；§2.2 的"必须保留"为准，GOAL §3 W5 行的"空表"表述过期） | `src/main/java/.../RetailQuestUseItemNpcs.java` 及消费者 |
| 退役前快照纪律（新增） | 自 W5-g2 起每张退役表先落 `.agents/summary/quest-native-dispatch/retired-tsv/` 并记 sha256（数据目录未纳入 git） | 本目录 `retired-tsv/` |
