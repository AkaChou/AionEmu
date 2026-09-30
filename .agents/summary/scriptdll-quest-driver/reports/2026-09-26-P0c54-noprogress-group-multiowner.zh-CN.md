# 2026-09-26 续片 28 / P0c-54：无进度交付行的声明组多 owner（18737/28737 采纳）+ 两个 NPC 桶的形状普查


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

## 0. 一句话

`RETAIL_REWARD_NPC_UNRESOLVED` 桶里 18737/28737 两行的交付字段写的是**已声明的真端对话名组**
（`IDRaksha_Solo_StageStart[_Dark]`），却被"非 hunt 族只精确解析 + 交付必须单 owner"两道门挡住；
本片给**无进度交付行**开一条受守卫的多 owner 通道（报告流逐 owner、完成流按集合形态一次），
采纳退役 2 行；并对**两个 NPC 桶的余量做形状普查**：REWARD 余 7 行全为数据级/登记级/单成员形，
ACQUIRE 余 21 行全为「接取字段写的是任务物品符号」形（属物品轴，不是 NPC 轴）。

## 1. 缺口：reward 名是**已声明组**的两行

| quest | DD `value0_acquire_` | DD `reward_npc_name` | progress | 现状 |
|---|---|---|---|---|
| 18737 | `LF5_Averse_E`（精确 → 804707） | `IDRaksha_Solo_StageStart` | 无（交付即完成） | `REJECTED:RETAIL_REWARD_NPC_UNRESOLVED` |
| 28737 | `DF5_Cnut_E`（精确 → 804732） | `IDRaksha_Solo_StageStart_Dark` | 无 | 同上 |

四源一致（决策表 `p0c54-noprogress-group-decisions.tsv`）：

| 源 | 提供什么 |
|---|---|
| 组表 `retail-quest-ai-name-groups.tsv` | 两个组名**已声明**（第 18/19 行，各 3 员，成员共一个 `title_id`） |
| 客户端 npc 块 `<quest_ai_name>` | 把组名声明在 206378/206379/206380（暗面 206395/206396/206397）上 |
| 遗留 XML（见证） | **3 × `NPC_REPORT` + 3 × `npc-complete`**（逐字同形，每个成员一份）——多 owner 交付是原形 |
| DD 表 | 接取名是精确的单 NPC（`LF5_Averse_E`/`DF5_Cnut_E`），与交付集不交 |

⇒ 判据：**交付集 = 组员全展开**（多 owner），接取 NPC 与之不交（满足 1919 合同的「各 NPC 路由不越界」）。

## 2. 代码改动（两处，均为**受守卫**的通道扩域）

1. `RetailDataDrivenDefinitionCompiler`
   - 新增 `noProgressGroup = entry.noProgress() && npcIndex.isQuestAiNameGroup(rewardName)`：
     只有**无进度交付行**且 reward 名**在组表内声明**时，reward 才走统一名字通道 `resolvePartyName`
     （精确 → 声明组 → 变体）；其余非 hunt 族仍 `resolveAll`（精确）——组名在那里不展开，
     未声明的同名多刷点照旧按歧义拒绝。
   - 交付集门从「size != 1 ⇒ 拒绝」放宽为「size != 1 ∧ ¬collectDestined ∧ ¬(hunt 组) ∧ ¬(无进度声明组)
     ⇒ 拒绝」；无进度分支改传**交付集**（集合重载），单 owner 行行为逐字节不变。
2. `RetailDataDrivenTalkCompiler`
   - 新增集合重载 `build(questId, acquiredNpc, Collection<Integer> rewardNpcs, …)`：报告流
     （`QUEST_SELECT` + `SELECT_QUEST_REWARD(1009)`）**逐 owner 各一条**；完成流调用既有的
     `completeFlow(questId, Collection, metadata)` 集合重载——领奖窗的对话页通道无 NPC 域**整族一份**、
     报告 NPC 通道逐实例（与 P0c-45/46 记录的 50052 家族形口径一致，**不会撞 `AMBIGUOUS_TRANSITION`**）。
   - int 单 owner 签名委托集合重载（`List.of(rewardNpc)`），既有行为零变化。

## 3. 爆炸半径（改动前先量）

`-Dretail.dataDriven.equivOut` 实测 1508 行分类 dump（`p0c54-classification-preflip.tsv`）与当前登记表
逐行对拍：**本片改动只翻 2 行**（18737/28737 → `ADOPTED`）；其余 22 行为并行车道的既有面
（21 行**已退役行**的冻结登记值 + 车道 `20035` 的码位前移），本片不触碰。

## 4. 落地与收口

| 步 | 内容 |
|---|---|
| 清单 | 5 副本（src/main + src/test + target/classes + target/test-classes + `.agents` 快照）翻 `RETAIL_TABLE`，reason `basis=DD_NO_PROGRESS_GROUP_REWARD`；whipsaw 守卫（四副本先一致） |
| 生产面 | `quests/18737.xml`、`quests/28737.xml` 各删 2 副本；`quest_definition_catalog.xml` 各删 2 行 |
| 登记 | 漂移表 2 行 → `ADOPTED`（双副本；头注直方图按数据行重算） |
| 指纹 | `retail-data-driven-ir-fingerprints.tsv` 新增 2 行（`18737 a0cf3575… 4/73`、`28737 5864f9df… 4/73`，双副本，升序插入） |
| 核验 | `verify_retirement.py` = `catalog=1284 / directory=1284 / retired=4940 / sum=6224` + `OK 目录一致，无悬空生产引用` |

**既有 pin 独立确认形状**：`QuestBatchReportNpcAlignmentTest.quest18737SupportsMultipleEndNpcChoices`
（老 pin，断言接取 804707 单条 + 三个交付 NPC 各 1 条 `QUEST_SELECT`／`SELECT_QUEST_REWARD` +
`completionNpcs == [206378,206379,206380]` 顺序敏感）在翻转后**通过**——真端合成与遗留多块形逐项一致。

## 5. 门禁

| 档 | 结果 | 归因 |
|---|---|---|
| DD 门 + 组门 | 11 例 **1F** | 唯一红 = 车道 `20035` 码位（本片不改） |
| T1（`gates/T1-210731.log`） | 75 例 **1F** | 同上；**客户端契约门 1/1 绿**（新采纳 2 行零 fatal）、归属/白名单/目录清单门全绿 |
| T2（`gates/T2-211401.log`，18737/28737） | 88 例 **3F** | ①车道 `20035`；②`RetailSimpleTalkChainGateTest` 链式指纹偏离——**车道在飞**：其 `retail-simple-talk-chain-ir-fingerprints.tsv` mtime **21:14:57**、`quest_client_talk_chain_steps.tsv` **21:18:06**（都落在本片 T2（21:14:01 起）窗口内，属其重冻中途态）；③`QuestBatchReportNpcAlignmentTest.quest11139ReportsAndCompletesAtEndNpc`——**车道既有**（车道 T3 `T3-204340` 与本片 T3 在册） |
| T3（`gates/T3-212101.log`） | 2013 例 **117F/22E/1S** | 对拍车道同态 `T3-204340.log`（20:43，同为 2013/117/22/1）**ADDED 0 / REMOVED 0**（107 ↔ 107 身份集相同）= **零新增**；T2 窗口内的链式指纹红在 21:21 前已由车道重冻自愈（两日志中 `RetailSimpleTalkChainGateTest` 均无失败行） |

## 6. 两个 NPC 桶的形状普查（余量口径，防再次从错误轴进攻）

**`REWARD_NPC_UNRESOLVED` 9 → 7**，逐行形状：

| 行 | reward 名 | 形状 | 处置 |
|---|---|---|---|
| 15690 / 25690 | `ld_rw_npc_gd5001` | **真端表同 id 双行**（后行覆盖，幸存行 reward 名生产表零命中） | 数据级，留拒（勿从代码轴攻） |
| 30722 / 30772 | `magician_apprentice` | 组表**已登记的矛盾排除**（`LEGACY_ACCEPT_CONTRADICTS_CLIENT`） | 设计上不解 |
| 50064 / 51064 | `Event_NPC_guardrung_l` / `_d` | 客户端块仅**单成员**（835132 / 835133）+ `CollectItem` 形（同 50089/50090 的活动商人采集形） | 需客户端契约先裁（单成员声明 + 采集行） |
| 50108 | `event_npc_idsweep_eyeloong` | 客户端块 2 员（836258/836259）但**模板名撞** 470174/470175（`MULTI_TITLE_ID`，两组不同 title） | 名字与模板不同源，留拒 |

**`ACQUIRE_NPC_UNRESOLVED` 21 行**：接取字段全是**任务物品符号**（`doc_quest_*` / `quest_*` / `QUEST_*`）
或 `_challengetask_` 哨兵 ⇒ 它们不属于"NPC 名字"轴，而属于**物品轴**（ItemPlay 接取授予词汇）；
下一次要动这一桶必须先把物品符号→物品 id 的解析与"接取即授予"形状补齐，**不要**再按 NPC 名展开。

## 7. T3 全量对拍（归因）

本片 T3 = `gates/T3-212101.log`（21:21:01 启动，452s，2013 例 117F/22E/1S）。对拍工具
`p0c52_t3_attribution.py`（失败方法集合算术）：

| 对拍对象 | 结果 | 说明 |
|---|---|---|
| 车道同态 `gates/T3-204340.log`（20:43 启动；同为 2013/117/22/1） | **ADDED 0 / REMOVED 0**（107 ↔ 107） | 零新增失败的**主证据**：失败身份集逐条相同 |
| 本片 T2 的链式指纹红 | 已自愈 | 车道 `retail-simple-talk-chain-ir-fingerprints.tsv`(21:14:57) 与其步骤表(21:18:06) 的重冻中途态；21:21 的 T3 里该门已绿 ⇒ 确认为其窗口内瞬态 |
| `QuestBatchReportNpcAlignmentTest.quest11139ReportsAndCompletesAtEndNpc` | **车道既有，已核**（本片不修） | 三份日志逐字同签名：`T3-194812`（**翻转前**基准，行 2298-2306）、`T3-204340`（行 2223-2231）、`T3-212101`（行 2221-2229）——同为 `expected: <1> but was: <0>` @ `QuestBatchReportNpcAlignmentTest.java:178`。结构侧独立核对：11139 是 SimpleTalk **链式行**（`RETAIL_TABLE`，`retail-simple-talk-chain-ir-fingerprints.tsv` 在册），不在本片两行翻转集，也不在 DD 指纹新增集（当前 DD 仓 1199 行 = P0c-52 基线 1174 + P0c-53 净 23 + 本片 2；`added` 恰为 25 且含 18737/28737、`removed` 0）⇒ 与本片改动无交集 |

**翻转前基线的取得方式（方法学）**：`T3-194812.log` 是本片动工前的车道全量档，用于"失败是否**既存**"的判据；
同态对拍（`T3-204340` ↔ `T3-212101`）用于"本片是否**新增**失败"。两者互补：前者判既存、后者判增量。
链式指纹仓 `retail-simple-talk-chain-ir-fingerprints.tsv` 属**链车道工件**（其重冻工具按追加合并写、非升序），
本片只读比对、不改：链车道自己的两个快照 `p0c54-fingerprints-{pre,dump}.tsv`（`770064ea…` → `fae74c98…`）
之间 13 行值变更即其本片重冻量，而 `-dump` 与当前仓（`c9657153…`）**逐行同值**（同 285 行集合，仅 6 行落位差异）
⇒ 链式门在三份 T3（`T3-194812`/`T3-204340`/`T3-212101`）中均 2/2 绿。

## 8. 编号消歧（同轮并发车道）

本片与同轮并发车道各自独立编号，**唯一键 = lane + 续片号**（沿用 `P0c-44`/`P0c-45` 判例）：

| lane | 编号 | 续片 | 主题 |
|---|---|---|---|
| **DataDriven / 门禁车道（本片）** | P0c-54 | 续片 28 | 无进度交付行的声明组多 owner（18737/28737）+ 两个 NPC 桶形状普查 |
| SimpleTalk 链车道（并发） | P0c-54 | 续片 31 | 阶段页轴：入口页补行（39003/49003）+ 阶段页跨 NPC 扩散剪除 |

**前缀共用提示（防误归）**：`p0c54*` 前缀为两车道共用。属**链车道**的产物：
`p0c54_stage_page_{census,decisions}.py`、`p0c54-stage-page-{census,decisions}.tsv`、`p0c54_blast_radius.py`、
`p0c54-blast-radius.txt`、`p0c54_refreeze_fingerprints.py`、`p0c54-fingerprints-{pre,dump}.tsv`、
`p0c54-registry-pre.tsv`、`p0c54-ir-{pre,post}.txt`、`p0c54-audit-{pre,post}.txt`、
`p0c54-generator-stage-page-axis.out`、`P0c54{StagePage,Audit}ProbeTest.java.txt`。
属**本片**的产物：`p0c54_retire_noprogress_group_rows.py`、`p0c54_update_drift_rows.py`、
`p0c54_insert_fp_rows.py`、`p0c54-noprogress-group-decisions.tsv`、`p0c54-classification-preflip.tsv`、
`p0c54-fp-dump.tsv`、报告 `/reports/2026-09-26-P0c54-noprogress-group-multiowner.zh-CN.md`。

## 9. 结论

18737/28737 的采纳证明：**组声明通道 + 多 owner 交付**在无进度形上可用，且与既有 pin（顺序敏感）逐项一致；
两个 NPC 桶的余量已按形状分类，其中 17 行有明确"不可采纳"证据（数据冲突/登记排除/名字撞模板），
真正的下一站是**物品轴**而非 NPC 轴。
