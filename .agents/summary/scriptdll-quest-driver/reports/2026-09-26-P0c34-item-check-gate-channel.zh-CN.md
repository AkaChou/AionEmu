# P0c-34：item_check 门通道落地（npc-item-report 转写 + 链编译展开）+ 1152 入台

- 日期：2026-09-26
- 切片：P0c-34（承 P0c-33 回退后的第一片；通道落地 + 首行入台）
- 触发：P0c-33 §7 工作项「item_check 门通道（npc-item-report 转写 + 链编译展开）——落地后 24202/80320 可重新入台」
- 结论：**通道落地并前向验证通过（24202 真端定义与已删 XML 完全等价），1152 入台退役；24202/80320 维持 XML_RETENTION，被"进度行投影"轴阻塞（新证据见 §5）**

## 1. 结论摘要

1. 生成器新增 **I 记录**（`npc-item-report` 逐字转写）并做真端双背书 fail-closed：真端 `Quest_SimpleTalk.xml` 行内 `<item_check>1</item_check>` 存在 + 真端 `quest.xml` 的 `collect_item` 符号经 `item_name_index.tsv` 解析到同一 `(item_id, count)`。三条在册行 cross-check 全 MATCH（`p0c34-item-report-crosscheck.tsv`）。
2. 链编译器新增门展开：39 / 20002 各成功/失败一对（成功 = HAS_ITEM 扣物 → target，after = `SYNC:LEVEL_AND_VISIBILITY_REFRESH` + 领奖窗 5；失败 = 留 source，缺省 `SELECT6(2716)`，`failure-page="CLOSE"` → 关窗），形状与 `QuestXmlBlockExpander.expandNpcItemReport` 逐字同口径，并入 `blockTransitions` 以复用「显式路由覆盖块路由」与同键去重。
3. **前向验证**（临时探针，源码已存档）：三行经真端编译器合成后跑 `QuestDialogOrderAudit` → **0 条致命指纹**（P0c-33 的 `BUTTON_WITHOUT_ROUTE` 是 39/20002 无路由，现已全部命中）；与已删 XML 的 IR 对比：**24202 完全等价（onlyRetail=0/onlyXml=0）**，80320/1152 只多 1 条零售侧自愈边（`EnterWorld REWARD var0=0`），无 XML 独有边。漂移门独立确认：`24202: DIFF:TRANSITION_SET → EQUIVALENT`。
4. **1152 入台**（唯一无阻塞行）：manifest ×4 + 目录 ×2 + XML ×2 + 指纹 ×2 + 漂移登记 ×2；`verify_retirement.py` OK（目录 1381 + 退役 4843 = 6224，当时快照）；专用判官 `Quest1152RetailAlignmentTest` 改为生产视图后 1/1 绿（它断言的正是门形状：成功 cond/act/after、失败 → pepper + SELECT6）。
5. 顺带偿还 P0c-33 判据 6 的潜伏债：链路径通用「var0=0 修复边」守卫在**已有 REWARD 修复边**时不再发射（24202 双修复边、19004 同形）；19004 的冻结指纹随之外科更新（唯一变化行）。
6. 生成器新增 **REWARD 投影漂移护栏**：16 行「XML 转写值 ≠ 客户端任务书末行行号」写入 `p0c34-chain-reward-row-divergence.tsv`（含 1479），并对 1479 用显式裁定表固定客户端值——避免重新生成时静默把 1479 的 `reward var0` 从 2 翻回 1（客户端 3 行 → 末行 2；XML 退役前写的是 1）。

## 2. 落地物

| 层 | 文件 | 说明 |
|---|---|---|
| 生成器 | `.agents/summary/scriptdll-quest-driver/build_quest_client_talk_chain_steps.py` | I 记录产出 + 真端 cross-check fail-closed + REWARD 投影裁定/分歧导出 |
| 登记表 | `src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv`（+ target/classes） | 新增 1 行表头注释 + 3 行 I 记录（1152/24202/80320） |
| 加载器 | `RetailClientTalkChainSteps.java` | `ItemReportRecord` + `case "I"` + `itemReports(int)`（`has()`/`questIds()` 维持 routes 口径不变） |
| 编译器 | `RetailSimpleTalkDefinitionCompiler.java` | `itemReportGate(...)`（4 条门路由）+ `syncQuestState(QuestNode)` + 通用修复边守卫收窄 |
| 裁定 | `p0c34-item-report-crosscheck.tsv`、`p0c34-chain-reward-row-overrides.tsv`、`p0c34-chain-reward-row-divergence.tsv`、`p0c34-item-check-gate-channel-decisions.tsv`、`p0c34_flip_item_check_gate_row.py` | 逐行证据与手术式入台脚本 |
| 探针 | `RetailItemCheckGateChannelProbeTest.java.txt`（源码已删） | 三行真端编译 + 契约审计 + 与 XML 的 IR 差集 + 生产视图致命指纹扫描 |

## 3. 关键证据（可复核）

1. **真端 cross-check**：1152 `collect_item1 shopmaterial_co_17a 1 → 169400112`（XML item-id 169400112 required 1 ✓）；24202 `quest_24202b 1 → 182215465` ✓；80320 `quest_80320a 12 → 182215303` ✓（`p0c34-item-report-crosscheck.tsv`）。
2. **门路由形状**（探针输出逐字）：24202 `started + npc 205150 + 39 → reward` cond=`HasItem(182215465,1)` act=`RemoveItem(182215465,1)` after=`[SyncQuestState(LEVEL_AND_VISIBILITY_REFRESH), ShowQuestDialog(5)]`；失败对 `→ started` after=`[CloseDialog]`（该行 `failure-page="CLOSE"`）；80320 失败页 = `ShowQuestDialog(2716)`（缺省 SELECT6）。
3. **IR 等价**：24202 onlyRetail=0/onlyXml=0；80320/1152 onlyRetail=1（通用自愈边）/onlyXml=0。
4. **漂移门**：`RetailSimpleTalkGateTest.driftVersusLegacyXmlIsRegistered` 由红转绿——它原本报 `1152: 登记=REJECTED:COMPILATION_FAILED 实际=DIFF:TRANSITION_SET, 24202: 登记=DIFF:TRANSITION_SET 实际=EQUIVALENT`，两条正是本片通道的直接效果（登记表随之更新）。
5. **契约审计**：三行审计 0 条致命指纹；生产视图致命指纹总数 46，其中 **1152/24202/80320 贡献 0**（探针 `PRODUCTION-FATAL total=46` + 逐行过滤）。
6. **1152 旧 KEEP 依据消失**：`p0c10h-talk-chain-b1-decisions.tsv` 的 `REJECTED:COMPILATION_FAILED:UNREACHABLE_NODE: complete` 在当前编译器下不再复现（编译接受，38 transitions）。

## 4. 门禁结果

| 门禁 | 结果 |
|---|---|
| `RetailSimpleTalkChainGateTest` | 2/2 绿（冻结指纹 272 行 = RETAIL_TABLE 集合，含新增 1152；分区/形状不变量不变） |
| `RetailSimpleTalkGateTest` | 3/3 绿（家族规模 2223、accepted 下限、漂移登记同步） |
| `Quest1152RetailAlignmentTest` | 1/1 绿（**改为生产视图后**，断言口径未改） |
| `RetailOwnershipGateTest` | 4/4 绿（catalog + 退役 = 6224） |
| `QuestDefinitionCatalogManifestTest` | 10/10 绿 |
| `RetailQuestDriverOverlayTest` / `RetailQuestCatalogTest` / `RetailNonIrAxisGateTest` / `RetailSystemGrantDispatchTest` / `RetailSimpleSerialHuntGateTest` / `RetailSimpleItemPlayGateTest` / `RetailSimpleHuntFamilyGateTest` | 5/5、2/2、4/4、7/7、5/5、1/1、1/1 全绿 |
| `QuestProductionStartupGateTest` / `QuestInteractionObjectContractGateTest` / `ProductionCatalogWhitelistVerificationTest` | 2/2、2/2、1/1 绿 |
| `RetailMetadataEquivalenceGateTest` / `QuestMovieAndDialogLoopRegressionTest` | 1/1、19/19 绿 |
| `verify_retirement.py` | `catalog=1381 directory=1381 retired=4843 sum=6224` OK（无悬空引用） |
| T1 档（62 tests） | 4 红**全为并发车道在飞**：`QuestClientContractGateTest`(46)、`RetailDataDrivenGateTest`×2（16800/26800 等 5 行指纹 + 20035 漂移，运行中从 11 行收敛到 1 行）、`RetailSimpleCollectItemGateTest`（frozen=155 retired=175，P0c-33 已登记既有债） |

## 5. 24202/80320 未入台：阻塞于「进度行投影」轴（本片新证据）

- 真端声明：24202 `collect_progress=2`、80320 `collect_progress=1`；元数据轴**早已登记**为零售优先（`retail-metadata-divergences.tsv`：24202 `drops RETAIL_PRIORITY`、80320 `drops/items RETAIL_PRIORITY`）。
- 客户端任务书逐行：24202 3 行，第 0/1 行是中间对话（Neligor、Cairon），第 2 行带 `[%collectitem]`（"去纳赫尔古城从…身上找到钥匙并向 Surt 报告"）；80320 2 行，第 1 行带 `[%collectitem]`。即 `collect_progress` = **任务书上掉落生效行**，与 `var0` 行号一致。
- 当前链形状把 0..2 行压成单个 `started(var0=0)`（XML 期简化），而运行时掉落门 `QuestService.isQuestDrop` 要求 `status=START && var0==collectingStep`；24202 无 `START & var0=2` 节点、80320 无 `START & var0=1` 节点 → **入台会让门物品（182215465 / 182215303，唯一来源为该掉落）不可获得 = 任务不可完成**。
- 因此 24202/80320 的入台必须以「进度行投影」切片先行（v{step} 行 + 中间对话推进；同族先例：`RetailSimpleCollectItemDefinitionCompiler` 的 `talksFirst → v{step}` 约定）。就绪度已满：门路由就位、24202 与已删 XML 完全等价、`JournalRewardRowRepairContractTest` 的 `(24202, 2, 1)` 契约由 E 记录边满足（通用守卫停发重复边后恰 1 条）。
- 附带待裁：该判官的 `quests2372And4907And24202And24203DropsStepCorrected` 断言 24202 `collectingStep==0`，与已登记的零售优先轴冲突——进度行切片须一并裁定（判官更新需带证据）。

## 6. 未验证 / 阻塞（PENDING）

1. **运行时与手动客户端抽检未执行**：服务器进程由用户管理，AGENTS.md 规则 1/2 禁止自行启停；本次未执行的命令：任务书行高亮/检查按钮运行时验证（需授权启动）。
2. **契约门基线**：当前 classpath 基线（`src/` 与 `target/test-classes` 两份同 md5）为 **0 缺陷**；门实测 46 条既有指纹 → 红。证据链：本片两条链门在飞证明 271+1152 行 IR 未变（只有 19004 因修复边守卫变化），且 1152 贡献 0 → 46 条属并发轴（44 条 SELECT1_1 家族 + 16800/26800 两行），**建议由客户端契约轴 owner 刷新基线或修复**（P0c-33 观察到该基线在 classpath 副本曾为 50 行；本片不动）。
3. `JournalRewardRowRepairContractTest.persistedRewardRowsAreRepairedOnEnterWorld` 的 80020 契约（期望 stale=2）在登记表无 E 记录 → 该判官文件昨日由其他 lane 改为 `TEMP-VERIFY(view)` 并新增契约，属其批在飞；本片 diff 证明未增删任何 E 记录。
4. `docs/QUEST_CATALOG.zh-CN.md` 定义文件列的退役行滞后 1481 行（脚本 `refresh_catalog_doc_links.py --apply` 可批刷新）；本片不插单行补丁以免与并发 docs 改动混合。

## 7. 命令索引

| 命令 | 结果 |
|---|---|
| `python3 -B .agents/summary/scriptdll-quest-driver/build_quest_client_talk_chain_steps.py` | I 记录 3 行 + cross-check MATCH；REWARD 投影分歧 16 行导出 |
| `diff <registry 快照> <registry>` | 仅 1 表头 + 3 行 I（1479 由裁定表固定值） |
| `mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest,RetailSimpleTalkGateTest` | 5/5 绿 |
| `mvn -o -B test -Dtest=RetailItemCheckGateChannelProbeTest`（探针，已删） | 三行门路由 + 0 致命指纹 + IR 差集 + 生产致命指纹 46 |
| `python3 -B .agents/summary/scriptdll-quest-driver/p0c34_flip_item_check_gate_row.py` | 1152 入台（manifest ×4 / 目录 ×2 / XML ×2） |
| `python3 -B .agents/summary/scriptdll-quest-driver/verify_retirement.py` | OK 6224 闭合 |
| `mvn -o -B test -Dtest='RetailSimpleTalkChainGateTest' -Dretail.talkChain.fingerprintOut=/tmp/...` | 重冻结 → 仅新增 1152 行（其余为排序位移） |
| `bash .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1` | 62 tests / 4 红（全并发轴）；T2 1152 同 |
