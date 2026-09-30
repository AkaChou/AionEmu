# P0c-36：中间对话行阶梯（真端 `talk_npc1..3` + 客户端 steps 文本 → `started(0)/s1(1)..sK(K)`）+ 24202/80320 重建


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 切片窗口：2026-09-26（承接 P0c-35 的"进度行投影只能落末行"残留）。两行 P0c-35 遗留的
> `CLIENT_PAGE_UNREACHED`（24202 页 1352/1353/1693/1694、80320 页 1352）在本片全部转为
> `PAGE_ACTION_MATCHED`。**本片未新增任何致命契约指纹**（总数 44，与改动前逐行相同）。

## 1. 结论摘要

- **缺口形状**：真端 `Quest_SimpleTalk.xml` 行有 `talk_npc1`/`talk_npc2`/`talk_npc3`（多段对话行），
  客户端任务书 `quest_summary` 的 `<steps>` 第 k 行文本点名 `STR_DIC_N_<talk_npcK>`；XML 期把多行压成单个
  `started(var0=0)`，使中间对话页成为不可达页（非致命 `CLIENT_PAGE_UNREACHED`）。
- **阶梯形状（本片裁定）**：`accept → started(START,var0=0) → s1(var0=1) → … → sK(var0=K)`，
  `K = 真端 collect_progress`（QE-061 仍成立：sK 承载掉落生效行），收集/交付段（I 门、B 报告流、
  R 的 `QUEST_SELECT`/`SET_SUCCEED`）的 source 从 `started` 迁到 `sK`。
- **推进方式由客户端页链证据决定**（不是猜的）：
  - `SETPRO`：阶段末页有 `HACTION_SETPRO{k}` 按钮 → 该按钮是唯一推进点（24202：`select2`(1352) →
    `select2_1`(1353) → `SETPRO1`(10000)@205159；`select3`(1693) → `select3_1`(1694) → `SETPRO2`(10001)@205198）。
  - `TALK`：阶段页链**没有**推进按钮 → 该次对话的 `QUEST_SELECT` 事件本身推进 var0
    （80320：`select2`(1352) 只有 `HACTION_FINISH_DIALOG`）。
- **落地**：生成器 `build_quest_client_talk_chain_steps.py` 新增阶梯通道（fail-closed 三轴校验 + 阶段页链遍历 +
  段迁移 + 阶段入口路由），登记表 1366 条路由重建；P0c-35 的覆盖表清空（注明被本片取代）。
- **验收**：本片两行在生产视图的中间页全部 `PAGE_ACTION_MATCHED`（24202 五页、80320 三页），
  致命契约总数 **44（改动前后逐行字节相同）**；链指纹恰 2 行演进（本片两行）；族门 3/3、链门 2/2 绿；
  `verify_retirement.py` = `catalog=1351 directory=1351 retired=4873 sum=6224 — OK`。

## 1b. 顺带偿还：记忆库结构门禁复绿（并发 lane 的退役尾巴）+ QE-063 登记（可复核）

本片收口时 `verify_memory_bank.py` 结构门禁红，两类原因、都已修：

1. **本片自身**：新卡 QE-063 未在 `systemPatterns.md` 登记路由 → 补路由行（任务系统域名 ID 列表 + 关键词句）。
2. **并发 lane 的退役尾巴**：`QE-041`(10501) 与 `QE-044`(10503/10504) 的 evidence 指向已被并发批退役删除的 quest XML
   （P0c-45..46 批：10011/10501..10507/20011/20502..20507）——正是 QE-062 描述的症状。
   按 QE-062 配方用既有脚本修：`mb_dangling_evidence_fix.py`（先 dry-run：`TOTAL 3 replacements over 2 cards; mode=DRY`，
   再 `--apply`：3 处 REPLACE），逐条核对 retention owner 后改指 `retail-xml-retention.tsv` + git 历史。

结果：`sync_memory_bank.py` → `MEMORY_BANK_SYNC_OK ENTRIES=109`（PATTERNS 108 → **109**，+QE-063）→
`verify_memory_bank.py` → **`MEMORY_BANK_VERIFY_OK STEPS=3`**（derived-index / structure / freshness 全绿；
`FRESHNESS fresh=109 aging=0 stale=0`）。取代关系：QE-063 记录本片形状，QE-061 的 `boundaries` 已补“中间行通道已建”的指向。

## 2. 落地物

| 产物 | 路径 | 说明 |
|---|---|---|
| 缺阶梯普查（可重放） | `.agents/summary/scriptdll-quest-driver/p0c36_ladder_gap_census.py` → `p0c36-talk-ladder-census.tsv` | 族内 talk 行 324：步名对齐 273（已建模 232 / **缺阶梯 41** = 29 `XML_RETENTION` + 12 `RETAIL_TABLE`）、步名不对齐 51 |
| 逐行裁定 | `.agents/summary/scriptdll-quest-driver/p0c36-talk-ladder-decisions.tsv` | 2 行：`24202 K=2 SETPRO`、`80320 K=1 TALK`，每行附三轴依据（steps 文本 / 页按钮 / collect_progress / gate source） |
| 阶梯通道 | `.agents/summary/scriptdll-quest-driver/build_quest_client_talk_chain_steps.py` | ①载入裁定表 ②`apply_talk_ladder()`（fail-closed + 阶段页链 + 段迁移 + 入口路由）③写盘前 post-pass（表头行与数据行分离，避免分组解析撞注释行） |
| 登记表 | `src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv`（+ `target/classes` 同 md5 `07a3adf954981f01a0373c663d0d73f5`） | 24202 = 20 条 R + P/N×6/I/B/E；80320 = 18 条 R + P/N×5/I |
| P0c-35 覆盖表清空 | `.agents/summary/scriptdll-quest-driver/p0c35-progress-row-overrides.tsv` | 数据行删除 + 顶部「⚠️ 由 P0c-36 取代 / Superseded by P0c-36」说明（阶梯直接给出 var0） |
| 探针（源码归档，树内已删） | `P0c36TalkLadderProbeTest.java.txt` + `p0c36-probe-out.txt` | 阶梯/路由/掉落逐行 dump |
| 探针（源码归档，树内已删） | `P0c36ContractScanProbeTest.java.txt` + `p0c36-scan-out.txt` | 生产视图致命契约扫描 + 阶梯页可达性逐页判定 |
| 指纹 | `src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv`（+ target 同 md5 `d476be26aa1ead3d95e1940b66a47b1b`） | 24202/80320 两行外科替换 |

## 3. 关键证据（可复核）

**三轴形状背书（逐行）**

| 轴 | 24202 | 80320 |
|---|---|---|
| 客户端 `quest_summary` 步数 | 3（`STR_DIC_N_Tree_Moving_Neligor` / `STR_DIC_N_Cairon` / `[%collectitem]`+`STR_DIC_N_Surt`） | 2（`STR_DIC_N_event_Soraya` / `[%collectitem]`×12+`STR_DIC_N_event_Soraya`） |
| 真端 talk 字段 | `talk_npc1=Tree_Moving_Neligor`→205159、`talk_npc2=Cairon`→205198 | `talk_npc1=event_Soraya`→831427 |
| 真端 `collect_progress` | 2 | 1 |
| 阶段页链按钮 | `select2`(1352)→`SELECT2_1`(1353)→`SETPRO1`(10000)；`select3`(1693)→`SELECT3_1`(1694)→`SETPRO2`(10001) | `select2`(1352) 仅 `HACTION_FINISH_DIALOG`（无推进按钮） |
| 裁定 | K=2，mode=`SETPRO` | K=1，mode=`TALK` |

**普查口径**：真端族内 3152 行中 468 行有 `talk_npc1`、193 行有 `talk_npc2`、68 行有 `talk_npc3`；
本片只处理"步名三轴对齐且缺阶梯"的 41 行中的两行（其余 39 行按族分批，见 §6）。

**生成器 fail-closed（任一不满足即报错退出，不产出半截阶梯）**：talk 数 == K；每个 talk 名唯一解析为 NPC id；
`summary_rows == K+1`；`collect_progress == K`；mode ∈ {SETPRO,TALK}；reward NPC 唯一解析；
`started` 节点存在且 `var0 ∈ {0,K}`；阶段页链只跟随"目标页名以 `SELECT` 开头"的动作（页 id 与按钮动作共号空间，
1008 既是 `HACTION_FINISH_DIALOG` 也是 `quest_complete` 页 id）；TALK 模式下若发现推进按钮或续页即失败。

**段迁移**：`I`（item_check 门）、`B`（`NPC_REPORT` 报告流）、`R`（`QUEST_SELECT`/`SET_SUCCEED`/
`SELECT_QUEST_REWARD`/`USE_OBJECT`，以及 `reward_id` 匹配的 `FINISH_DIALOG` 副本）的 source 与 target 一并
从 `started` 重写到 `sK`（只改 source 会造出 `sK → started` 回退边）；`R seq` 重编号。

**形状实测（探针，最终树态）**

```
=== 24202 transitions=43 drops=[QuestDrop[npcId=214437, itemId=182215465, chance=100, eachMember=true, collectingStep=2, scope=GROUP], QuestDrop[npcId=214438, ... collectingStep=2 ...]]
  NODE unaccepted NONE {var0=0} / started START {var0=0} / s1 START {var0=1} / s2 START {var0=2} / reward REWARD {var0=2} / complete COMPLETE {var0=0}
=== 80320 transitions=38 drops=[QuestDrop[npcId=219646, itemId=182215303, chance=100, eachMember=true, collectingStep=1, scope=GROUP]]
  NODE unaccepted NONE {var0=0} / started START {var0=0} / s1 START {var0=1} / reward REWARD {var0=1} / complete COMPLETE {var0=0}
```

**可达性（契约扫描探针）**：`PRODUCTION-FATAL total=44`，本片两行致命 **0**；
`INFO 24202 page=1352 client=1353 / 1353 client=10000 / 1693 client=1694 / 1694 client=10001 / 2375 client=20002`
与 `INFO 80320 page=1352 client=1008 / 2375 client=39 / 2716 client=1008` **全部 `PAGE_ACTION_MATCHED`**
（P0c-35 时它们是非致命 `CLIENT_PAGE_UNREACHED`）。

## 4. 门禁结果

| 门 | 命令 | 结果 |
|---|---|---|
| 链指纹门 | `mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest` | **2/2 绿**；`274` 行中恰 2 行演进：24202 `46eb6812…` → `bac4158d65129f13628f2cfd38f82941e5baa5f3dd8a28d90a1bd99010cab324`、80320 `3681565d…` → `d825451fde37c8a7a42f583164a3a1406200fdd6c31eb4253b169a0c4d6c882f`（装入 src+target 双副本） |
| 家族语义门 | `mvn -o -B test -Dtest=RetailSimpleTalkGateTest` | **3/3 绿**（含 `retail-summary-rows.tsv` 冻结证据校验）。漂移登记**无需改动**：该分类是"真端合成定义 vs 历史 XML"，阶梯是**新增节点**，仍属 `DIFF:NODE_PROJECTION` |
| 契约审计 | `mvn -o -B test -Dtest=QuestDialogOrderAuditTest` | **17/17 绿**（审计行集由探针单独导出，见 §3） |
| T2（24202 80320） | `run_quest_gates.sh T2 24202 80320`（`gates/T2-123039.log`） | 83 例 7F+2E；**红集全部非本片**，逐条归属见 §5 |
| 退役恒等式 | `python3 -B verify_retirement.py` | `catalog=1351 directory=1351 retired=4873 sum=6224 — OK` |
| 副本一致性 | `md5 -q` ×4 组 | 登记表 `07a3adf9…`、指纹 `d476be26…`、漂移 `3c18e1b5…`、保留清单 `75d89ccd…` 均 src↔target 相等 |

## 5. T2 红集归属（本片贡献 0）

改动前基线（`gates/T2-101946.log`，12:19）已有 3 红，**与本片无关且逐字未变**：

1. `JournalRewardRowRepairContractTest.persistedRewardRowsAreRepairedOnEnterWorld:265` — quest **15613**（非本片行）；
2. `QuestClientContractGateTest` — 致命集 **count=44，所列 36 行**改动前后**字节相同**（本片两行 0 命中）；
3. `RetailSimpleCollectItemGateTest` — `frozen=155 retired=175`（采集族退役集增长，他车道）。

本轮新增红全部落在**并发车道的 XML→目录迁移在飞窗口**（`quest_definition_catalog.xml` 于 12:35 仍在写，
`verify_retirement` 快照从 1366/4858/6224 → 1351/4873/6224）：

- `RetailOwnershipGateTest`（`6224 vs 6221`）：**复跑 4/4 绿** → 判定 mid-flip 瞬态，非缺陷；
- `QuestDefinitionCatalogManifestTest.externalProductionCatalogCompiles:44`：`catalog.findExecutable(14153).orElseThrow()`
  无值——迁移后 14153 已不在可执行集，属迁移车道待同步的测试期望；
- `RetailDataDrivenGateTest` 4 红 → 迁移落定后 2 红（指纹 15042/16821/16823/26821/26823 + 登记 20035
  `RETAIL_TALK_HUNT_CHAIN_DEFERRED` vs `RETAIL_ITEMPLAY_OUTPUT_UNRESOLVED`）。**已排除本片污染**：
  该族编译路径 `RetailQuestDriver:790` **不接收** `RetailClientTalkChainSteps`（只有 `compileSimpleTalk` 在 `:819` 接收），
  故本片对链登记表的编辑在结构上不可能移动 DataDriven 指纹。

本片自身门类全绿：链门 2/2、族门 3/3、`QuestMovieAndDialogLoopRegressionTest` 19/19、
启动门 2/2、白名单 1/1、目录门 2/2、overlay 5/5、序列 hunt 5/5、系统发放 7/7、交互物 2/2、非 IR 轴 4/4。

## 6. 未验证 / 阻塞（PENDING）

- **运行时可授权项（PENDING）**：阶梯的实际对话推进（`SETPRO1/SETPRO2` 按钮点击 → `var0` 1→2、`QUEST_SELECT`
  推进 TALK 模式）与掉落步生效，需要启动服务器 + 客户端抽检；按用户约束**不启动服务器进程**，未执行。
  未执行的命令形态：启动 `AionEmu.jar` + 客户端登录 24202/80320 流程。
- **同形行余量（已登记，下一批）**：缺阶梯 41 行中余 **39 行**（29 `XML_RETENTION` + 12 `RETAIL_TABLE`），
  代表行 1479 / 2963 / 24123 / 30711 / 35010 / 45025 等；步名不对齐的 51 行需先补步名证据再判。
- **`retail-summary-rows.tsv`**：家族门 3/3 绿即冻结证据校验通过，本片未改该文件（它是客户端派生物，与 IR 无关）。

## 7. 命令索引

```bash
# 证据/裁定
python3 -B .agents/summary/scriptdll-quest-driver/p0c36_ladder_gap_census.py
cat .agents/summary/scriptdll-quest-driver/p0c36-talk-ladder-decisions.tsv

# 落地（重建登记表）
python3 -B .agents/summary/scriptdll-quest-driver/build_quest_client_talk_chain_steps.py
cp src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv \
   target/classes/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv

# 形状探针（临时，用毕即删；源码见归档 .java.txt）
mvn -o -B test -Dtest=P0c36TalkLadderProbeTest
mvn -o -B test -Dtest=P0c36ContractScanProbeTest

# 门禁
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest
mvn -o -B test -Dtest=RetailSimpleTalkGateTest
mvn -o -B test -Dtest=QuestDialogOrderAuditTest
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 24202 80320
python3 -B .agents/summary/scriptdll-quest-driver/verify_retirement.py
```
