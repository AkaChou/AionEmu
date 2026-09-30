# P0c-35：进度行投影（真端 collect_progress → START 行 var0）+ 24202/80320 入台


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-26
- 切片：P0c-35（承 P0c-34 §5 的阻塞轴；通道落地后的第二片）
- 触发：P0c-34 §5「24202/80320 入台阻塞于『进度行投影』轴——定义把多行压成单个 `started(var0=0)`，掉落门永不满足」
- 结论：**轴已建模并落地，24202/80320 入台退役；两行在生产视图贡献 0 条致命契约指纹；遗留「中间对话行合成」登记为下一片**

## 1. 结论摘要

1. **语义确认（三源合一）**：真端 `quest.xml` 的 `collect_progress` = 客户端任务书带 `[%collectitem]` 的行号 = **掉落生效行**。
   - 客户端侧：24202 任务书 3 行，第 2 行带 `[%collectitem]`；80320 2 行，第 1 行带 `[%collectitem]`；真端声明 `collect_progress` 分别为 2 / 1，逐行对齐。
   - 运行时侧：`QuestService.isQuestDrop` 要求 `status=START && 该任务 var0 == collectingStep`（`0` = 任意 START 行均可掉）。
   - 后果侧：P0c-34 前两行被压成单个 `started(var0=0)`，而元数据轴已登记为零售优先（`drops RETAIL_PRIORITY`，`collectingStep=2/1`）→ 无 `START & var0=2`（24202）/`START & var0=1`（80320）节点 → **门物品唯一来源掉落永不触发 = 任务不可完成**。这就是 P0c-34 拒绝入台的判据。
2. **生成器新增进度行投影裁定（N 记录 var0）+ 四轴 fail-closed 背书**（`PROGRESS_ROW_OVERRIDE`）：① 该行必须存在真端 `collect_progress > 0`；② 裁定值必须等于 `collect_progress`；③ 该值必须等于客户端任务书**末行**行号（中间行合成未建模前只接受末行，其余一律 fail 提示「需中间行合成，另行裁定」）；④ 该行必须有 `drop_monster_*` / `drop_item_*` 或真端 `<item_check>` 轴背书。
3. **两行裁定并落地**：`24202 started: 0 → 2`、`80320 started: 0 → 1`（`p0c35-progress-row-overrides.tsv` 显式裁定 + `p0c35-progress-row-projection-decisions.tsv` 逐行 6 条判据）。
4. **入台**：`p0c35_flip_progress_row_rows.py`（清单 ×4 / 目录 ×2 / XML ×2），`verify_retirement.py` 闭合（目录 1372→后续并发批继续下降，退役含本片 2 行，sum 恒 6224）。
5. **判官按真端口径更新**：`QuestMovieAndDialogLoopRegressionTest#quests2372And4907And24202And24203DropsStepCorrected` 拆分口径——2372/4907/24203（仍 XML 拥有）断言 `collectingStep==0`；24202（P0c-35 起真端驱动）断言 `collectingStep==2` **且**存在投影为 `START & var0=2` 的节点（可满足性断言，直接锁 QE-061）。19/19 绿。
6. **契约审计**：生产视图致命指纹 45 条，**24202/80320/1152 贡献 0**；两行只出现非致命的 `CLIENT_PAGE_UNREACHED`（24202：1352/1353/1693/1694；80320：1352）——即未合成的中间对话页，登记为下一片（§5）。
7. **漂移登记**：24202/80320 → `DIFF:NODE_PROJECTION`（投影形状变化，路由集不变），与 1152 的 `DIFF:TRANSITION_SET`（P0c-34 门通道）区分开。
8. **顺带偿还记忆库潜伏债**：`verify_memory_bank.py` 的**唯一**结构红 = 52 处悬空证据引用（18 张历史卡片的 `evidence` 仍写着已退役删除的 quest XML，其中 1 处正是本片退役的 `24202.xml`）。修法：逐条核对 `retail-xml-retention.tsv` owner（全部为 `RETAIL_TABLE`，UNEXPECTED 0），把路径换成持久指针 `retail-xml-retention.tsv 的 quest <id> 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）`。修后结构门禁由 `FAILED` 转 **`MEMORY_BANK_VERIFY_OK STEPS=3`**（PATTERNS=107 不变，证明未覆盖并发车道内容）；并沉淀 **QE-062**（含"整串精确替换"教训：先替换短形式 `quests/NNNN.xml` 会吃掉同文件长路径的尾段，留下半截假路径，本片踩中 2 处已二次修正）。PATTERNS 现 108。

## 1b. 记忆库修复的前后对照（可复核）

| 项 | 修前 | 修后 |
|---|---|---|
| `verify_memory_bank.py` | `MEMORY_BANK_VERIFY_FAILED STEPS=structure`，52 行 `references missing path`，涉及 QE-014/020/022/023/024/025/030/031/032/036/037/040/048/050/051/052/053/054 | `MEMORY_BANK_VERIFY_OK STEPS=3`（derived-index / structure / freshness 全绿） |
| 卡片数 | 107 | 108（+QE-062，注册进 `systemPatterns.md` 路由 + 症状索引自动重生成） |
| 悬空引用 | 52（18 卡） | 0（全部改指 retention 清单 + git 历史） |
| 半截假路径 | 2 处（`.../quest_definition/retail-xml-retention.tsv`，替换副作用） | 0（已修，并写进 QE-062 的 fix 条目防复发） |

## 2. 落地物

| 层 | 文件 | 说明 |
|---|---|---|
| 生成器 | `.agents/summary/scriptdll-quest-driver/build_quest_client_talk_chain_steps.py` | 进度行投影裁定载入（fail-closed 四轴）+ N 记录 var0 覆盖 + `PROGRESS-ROW-OVERRIDE` 回显 |
| 登记表 | `src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv`（+ target/classes，md5 `fdb7cffc…`） | 24202 `N started START 2`、80320 `N started START 1`（其余记录不变） |
| 裁定 | `p0c35-progress-row-overrides.tsv`（2 行）、`p0c35-progress-row-projection-decisions.tsv`（逐行 6 判据 + 缺口登记） | 显式裁定与证据 |
| 手术 | `p0c35_flip_progress_row_rows.py` | 两行入台：retention 清单 ×4 / catalog ×2 / XML ×2（带 whipsaw 守卫） |
| 冻结 | `src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv`（+ target，md5 `0dc8f1e8…`） | 新增 `24202 46eb6812…`、`80320 3681565d…` |
| 漂移 | `src/test/resources/quest/retail-simple-talk-drift.tsv`（+ target） | 24202/80320 → `DIFF:NODE_PROJECTION` |
| 判官 | `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMovieAndDialogLoopRegressionTest.java` | 真端口径拆分（见 §1.5） |
| 探针 | `P0c35RewardRecoveryEdgeProbeTest.java.txt`、`P0c35ContractScanProbeTest.java.txt`（源码均已删） | 修复边形状 + 生产视图致命指纹扫描 |
| 记忆库 | `.agents/memory-bank/patterns/quest-engine.md`（QE-061 补落地/验收/残留 + 新增 **QE-062**）、`systemPatterns.md`（路由 QE-062）、`mb_dangling_evidence_fix.py`（可重放修复脚本） | 悬空证据 52 → 0；结构门禁转绿 |

## 3. 关键证据（可复核）

1. **投影落地后的形状**（探针逐字输出）：24202 `started` 投影 `var0=2`、掉落 `step=2`（214437/214438 → 182215465）、恰 1 条修复边 `cond=[StatusIs[REWARD], QuestVariableIs[var0, 1]] act=[SetVariable[var0, 2]]`；80320 `started var0=1`、1 条修复边 `REWARD var0=0 → 1`；1152 `REWARD var0=0 → 2`（P0c-34 遗留形状）。
2. **修复边契约**：`JournalRewardRowRepairContractTest` 期望的 `(24202, stale=1, reward=2)` 由该唯一修复边满足（P0c-34 的「已有 REWARD 修复边则不再补通用边」收窄生效，无双边）。
3. **IR 指纹**：`24202 46eb6812f3116b411a30a7002e73d50586939548e3991d34ec2fa397be8e8de8`、`80320 3681565deb032e24c7b9c1333af49845a3ce96550e845480d219fed58e7980e8`（链门全量重算与冻结值精确相等）。
4. **生产视图致命指纹扫描**（`P0c35ContractScanProbeTest`，源码已删）：`PRODUCTION-FATAL total=45`，逐行过滤 24202/80320/1152 → **无致命行**；两行仅 `CLIENT_PAGE_UNREACHED`（非致命，见 §5）。
5. **真端背书**：24202 `collect_progress=2` + `quest_24202b 1 → 182215465`（I 记录 cross-check MATCH）；80320 `collect_progress=1` + `quest_80320a 12 → 182215303` MATCH。
6. **四副本纪律**：登记表 src↔target md5 相等（`fdb7cffc…`）、清单 ×4 md5 相等（本片落地时为 `e9ce1692…`；其后并发车道继续批量退役使清单增长为 `6d503e71…`，四副本始终同步）、指纹 src↔target 相等（`0dc8f1e8…`）、漂移 src↔target 相等（`3c18e1b5…`）；目录/清单/XML 三处一致（`1152.xml`/`24202.xml`/`80320.xml` 均已不在树内，catalog 中 0 次命中）。

## 4. 门禁结果

| 门禁 | 结果 |
|---|---|
| `RetailSimpleTalkChainGateTest` | 2/2 绿（冻结指纹含本片 2 行；分区/形状不变量不变） |
| `RetailSimpleTalkGateTest` | 3/3 绿（漂移登记同步：24202/80320 更新为 `DIFF:NODE_PROJECTION`） |
| `QuestMovieAndDialogLoopRegressionTest` | **19/19 绿**（含按真端口径改写的掉落步断言） |
| `RetailOwnershipGateTest` | 4/4 绿（重试后；首次红为并发车道 mid-flip 的 6220 瞬态，见 §6.2） |
| `QuestDefinitionCatalogManifestTest` / `RetailQuestDriverOverlayTest` / `RetailQuestCatalogTest` / `RetailNonIrAxisGateTest` | 10/10、5/5、2/2、4/4 绿 |
| `RetailSimpleSerialHuntGateTest` / `RetailSimpleItemPlayGateTest` / `RetailSimpleHuntFamilyGateTest` / `RetailSystemGrantDispatchTest` | 5/5、1/1、1/1、7/7 绿 |
| `QuestProductionStartupGateTest` / `QuestInteractionObjectContractGateTest` / `ProductionCatalogWhitelistVerificationTest` | 2/2、2/2、1/1 绿 |
| `verify_retirement.py` | `catalog=… directory=… retired=… sum=6224` **OK**（多次快照：1375/1372/1368 目录随并发批下降，退役同步上升，恒等式不破） |
| T2（24202 80320） | 85 tests / 5 红 0 错，**跨 2 次重跑集合完全相同**；5 红逐条归因为他车道或既有债（§6.1），本片行贡献 0 |
| T1 | 59 tests / 4 红 1 错；4 红与他车道 T2 集合一致，1 错为并发 mid-flip 瞬态（复跑 `RetailOwnershipGateTest` 4/4 绿） |

## 5. 遗留：中间对话行未合成（下一片，已在裁定表登记）

- **24202**：客户端任务书第 0/1 行是中间对话（Neligor → Cairon，客户端页 1352/1353 与 1693/1694），当前定义没有对应节点 → 审计报 `CLIENT_PAGE_UNREACHED`（非致命，但玩家在客户端上会看到无路由的中间页 **且掉落生效行从第 2 行才开始**，与真端「先跑腿对话、后进入收集」的节奏不同）。
- **80320**：客户端页 1352 未达（接取 NPC = 信件行 0，无中间对话行）。
- **判据积累（下一片可直接用）**：客户端页 1352/1353、1693/1694 的页文本 + 真端 `talk_npc1`/`talk_npc2` 字段 + `Dialogs/QUEST_Q<id>.html` 的 dic 名 → 可派生「中间对话行 → var0 推进」通道；同形行（8xxx 家族）一并解锁。
- 生成器中已用 fail-closed 拦住「把末行之外的投影写进裁定表」的捷径：任何非末行投影都会报 `客户端末行 X != collect_progress Y（需中间行合成，另行裁定）`。

## 6. 未验证 / 阻塞（PENDING）

1. **运行时与手动客户端抽检未执行**：服务器进程由用户管理，AGENTS.md 规则 1/2 禁止自行启停。未执行的命令/动作：24202/80320 收集期掉落实测（击杀 214437/214438 是否掉 182215465）、任务书行高亮与检查按钮运行时验证、中间对话页的人工目检。
2. **并发车道瞬态**：T1 首次跑 `RetailOwnershipGateTest` 报 `expected 6224 but was 6220`——同一分钟内 `verify_retirement.py` 两次实测均为 6224 OK、且随后该门 4/4 绿；原因是 DataDriven lane 正在批量退役（目录 1375→1372→1368），其「删 catalog 行」与「写退役清单」之间存在毫秒级窗口。**本片未改动任何退役清单/目录的既有行**（仅追加 2 行），不代他车道修补。
3. **他车道红（本片不代修）**：`RetailDataDrivenGateTest`（15042/16821/16823/26821/26823 指纹漂移 + 20035 漂移码）、`RetailSimpleCollectItemGateTest`（frozen=155 retired=175 既有债）、`JournalRewardRowRepairContractTest`（15613，归属 DataDriven，**不在链登记表**，其契约 stale=5 vs 实际 var0=0）、`QuestClientContractGateTest`（45→44 条既有指纹，属客户端契约轴）。
4. **`docs/QUEST_CATALOG.zh-CN.md` 退役定义列滞后**（脚本 `refresh_catalog_doc_links.py --apply` 可批刷新）；本片不插单行补丁以免与并发 docs 改动混合。

## 7. 命令索引

| 命令 | 结果 |
|---|---|
| `python3 -B build_quest_client_talk_chain_steps.py` | `PROGRESS-ROW-OVERRIDE 24202 started: 0 -> 2`、`80320 started: 0 -> 1`（四轴背书通过） |
| `python3 -B p0c35_flip_progress_row_rows.py` | 两行入台（清单 ×4 / 目录 ×2 / XML ×2） |
| `python3 -B verify_retirement.py` | `sum=6224` OK（1375/1372/1368 三次快照均闭合） |
| `mvn -o -B test -Dtest='RetailSimpleTalkChainGateTest' -Dretail.talkChain.fingerprintOut=/tmp/p0c35-fp.tsv` | 重冻结 → 仅本片 2 行新增 + 19004 更新（其余排序位移） |
| `mvn -o -B test -Dtest=P0c35RewardRecoveryEdgeProbeTest`（已删） | 修复边形状 + 投影值逐字输出 |
| `mvn -o -B test -Dtest=P0c35ContractScanProbeTest`（已删） | `PRODUCTION-FATAL total=45`；24202/80320/1152 致命 0 |
| `mvn -o -B test -Dtest='QuestMovieAndDialogLoopRegressionTest'` | 19/19 绿（真端口径） |
| `bash run_quest_gates.sh T2 24202 80320` | 85 tests / 5 红 0 错，跨次集合相同；本片行 0 贡献 |
| `bash run_quest_gates.sh T1` | 59 tests / 4 红 1 错（1 错=并发瞬态，复跑绿） |
| `python3 -B mb_dangling_evidence_fix.py --dry-run` / `--apply` | 悬空证据 52 处 / 18 卡（UNEXPECTED 0）→ 全部改指 retention 清单 |
| `python3 .agents/memory-bank/sync_memory_bank.py` + `verify_memory_bank.py` | `ENTRIES=108` → **`MEMORY_BANK_VERIFY_OK STEPS=3`**（修前为 `FAILED STEPS=structure`） |
