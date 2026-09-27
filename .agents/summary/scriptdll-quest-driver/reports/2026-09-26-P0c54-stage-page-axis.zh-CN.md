# P0c-54 阶段页轴：入口页补行 + 跨 NPC 扩散剪除（SimpleTalk 链车道 / 续片 31）

日期：2026-09-26　车道：SimpleTalk 链登记表（`quest_client_talk_chain_steps.tsv`）　编码：`P0c-54`（编号取全局最大值以免与并发 DataDriven 车道的 `P0c-48..P0c-53` 撞号；唯一键 = lane + 续片号）

## 1. 缺口形状

P0c-47 收口 `DELIVER_UNCOVERED` 时登记了 `STAGE_PAGE_OWNER_SPREAD_PENDING`：39003/49003 缺客户端声明的阶段入口页、接取 NPC 却带了阶段续页；11106 的阶段续页在两个阶段 NPC 上双份。本片把它做成**阶段页轴**，并先做全量普查定规模（而不是只修登记的 3 个任务）。

普查 `p0c54_stage_page_census.py`（一行一个「任务 × 阶段 × 页链上的页」事实，`OK/缺失/越界` 三态）：**404 项中 48 项不合形** ⇒ 归两形：

| 形 | 计数 | 例 |
|---|---|---|
| **A 入口页缺失** | 2 | 39003 / 49003 的 `SELECT2`（客户端页链入口）从未被任何行下发 |
| **B 阶段页跨 NPC 扩散** | 44 | 阶段页链上的页被**非本阶段 NPC** 也下发：接取角色（`unaccepted` 相位）、**别的阶段** NPC、交付 owner |
| C 入口页不在客户端页册 | 1 | 21033 阶段 2（真端数据缺口，另轴） |
| D 页链断裂 | 1 | 80320@831426（另轴） |

普查期间修掉**两个自伤假绿**（记入判例素材）：①页名有前缀关系（`SELECT2 ⊂ SELECT2_1`），子串匹配会把 `SELECT2_1` 行当成 `SELECT2` 的 owner ⇒ 必须**精确令牌**（`;` 切分后整 token 比对）；②阶段集合若取 `s{k}` **节点**会漏掉**末阶段**（末阶段 dst 是 `reward`、无 `s{k}` 节点）与**压平形**（单阶段任务没有 `s1` 节点）⇒ 阶段必须取 **`SETPRO<k>` 推进行**。

## 2. 裁决口径（fail-closed 轴）

裁定表 `p0c54-stage-page-decisions.tsv`（生成器逐行校验；任一轴不成立即中止）：

1. **code 域**：`STAGE_ENTRY_MISSING` / `STAGE_PAGE_OWNER_SPREAD`；
2. **与块级通道互斥**：阶梯 / 接取入口 / 改道 / 多余 owner / 角色收窄六表同任务即失败。与 P0c-47 **行内**阶段窗外溢**允许同任务**——两表靶行不同（本表靶 = 阶段页链的页行；P0c-47 靶 = `SETPRO<k>` 推进行的末位窗外溢令牌），且两侧都有「条目未被消费即 fail-closed」收尾守卫，重叠是自证的；
3. **owner 必须 `RETAIL_TABLE`**（XML 保留行的 IR 属 XML，不进本表）；
4. **真端轴**：`owner_npc` 必须 == 真端 `talk_npc<k>` 的**唯一解析**（k = 该页所属阶段）；
5. **客户端页链轴**：页必须在客户端该阶段页链上——入口 `SELECT{k+1}` →（按钮动作落在页号共号空间）→ 续页 … → 带 `HACTION_SETPRO{k}` 的末页；ENTRY 页还必须是**页链入口**；
6. **角色轴（越界者）**：extra NPC 必须可判角色——客户端任务书 `start_npc_ids`（接取）/ `end_npc_ids`（交付 owner）/ `progress_npc_ids` 序位 ≠ k（别的阶段位），或真端别的 `talk_npc<j>`（j≠k）；不可判 ⇒ 剔除并登记；
7. **只剪页动作行**：`QUEST_SELECT` 行是被剪 NPC 自己的**对话入口**（接取/报告窗），删了该 NPC 对话整条消失、任务不可接 ⇒ 本表不裁（转登记 `ACCEPT_ENTRY_PAGE_WRONG_PENDING`）。

收尾两道守卫：**①裁定表每条都必须命中/插入**（未消费 = 表腐化 ⇒ fail-closed）；**②无页丢失**：剪除后每一页仍由 owner 名下记录下发。

## 3. 五源取证（以 39003 为例，其余同形）

| 源 | 证据 |
|---|---|
| 真端表 | `acquired=Arena_Geniki_E_LHM`、`talk_npc1=DF2a_Ionia_E_LHW`（=**800512**）、`reward=DF2a_Nevma_G_LHM`（=800504） |
| 客户端任务书 | `start=800500 / progress=800512 / end=800504`、`progress_page_ids=1352` |
| 客户端页册 | 任务级并集含 `1352`（select2）与 `1353`（select2_1）——**页册不判归属**，归属靠动作链 |
| 客户端页 + 按钮 | `select2` 页文「…我来告诉您和战斗有关的几个注意事项。」按钮 `HACTION_SELECT2_1`（**「点头。」**，动作号 1353）→ `select2_1` 页文「…军团长大人就在[%dic:…]北部。」按钮 `HACTION_SETPRO1`（**「结束对话。」**，动作号 10000） |
| 现状登记表 | 只有 `R2 800512 SELECT2_1`；`SELECT2` 从未下发 ⇒ 审计 `CLIENT_PAGE_UNREACHED 1352`（页链入口断了，续页也就不可达） |

⇒ 结构结论：**阶段 NPC 需要「入口页 → 续页 → 推进」三步**，而接取 NPC 上的 `SELECT2_1` 是越界页（接取角色客户端页只有 `SELECT1=1011`）。插入口行 + 剪越界行。

## 4. 落地

- 生成器新增：模块级 `stage_client_chain()`（与 `apply_talk_ladder` 同口径的页链推导）、装载段 7 轴校验、发射段「剪页动作行 + 补入口行」、收尾两守卫；补行后 R 序号随既有重编过程收敛 1..N。
- 登记表 5076 → **5052** 行（`395f4016…` → **`43e37883…`**，422519 → **419727** 字节，10 行头不变）；`ADDED [] / REMOVED []`（块集合不变）。
- **爆炸半径**（`p0c54-blast-radius.txt`，pre = `p0c54-registry-pre.tsv`）：`BLOCKS 295 -> 295`、`CHANGED` **18 任务**、`ADDED/REMOVED []`；净 **-24** 行（26 剪 + 2 插）。
- **IR 逐行对拍**（临时探针 `P0c54StagePageProbeTest`，`canonicalText` 同口径；pre/post 各一次，pre 用 `surefire:test` 直跑避免资源阶段覆写）：**总变更 8 行** = 7 条越界页路由消失（2611/3001/3023/11070/11105/11106/11117/19004/21004/21036/21136 中的对应项）+ 39003/49003 各「-1 越界 +1 入口」；**owner 侧该页保留恰好 1 条**（2611@204783 / 11106@798979 / 21036@798713 / 11070@798949 …）⇒ 无页丢失。
- **IR 惰性说明（诚实记录）**：35017/45010/45017/45024/45026 的剪行**不改变 IR**——它们是 **SystemGrant 起手**任务，`unaccepted` 相位在 IR 里只有 1 条 `SystemGrant → START/0` 边，这些 `unaccepted→unaccepted` 行本就不编译（pre/post IR 逐字相同）；剪除属**登记表形态清理**，无行为效应。
- **审计对拍**（`P0c54AuditProbeTest`，生产视图 overlay 口径；pre/post）：**未达/EVIDENCE 行 10 → 6**，闭合的恰是 **39003/49003 各 2 行**（`CLIENT_PAGE_UNREACHED 1352/1353`），**零新增**；残留 6 行为既有（19004 的 `SELECT5(2375)`、35017/45010/45017/45024/45026 的 `ask_quest_accept(4)`，前后逐字相同）。
- **指纹重冻**（`p0c54_refreeze_fingerprints.py`）：`added=[] removed=[] changed=13`（11070/11105/11106/11117/19004/21004/21036/21136/2611/3001/3023/39003/49003），288 行布局不变；`770064ea…` → **`c9657153…`**（`src/test/resources` + `target/test-classes` 同值）。
- 生成器重跑逐字节相同（`FIDELITY_OK`）+ **22 张**输入表零漂移。

## 5. 门禁

| 门 | 命令 | 结果 |
|---|---|---|
| 净树（链门 + 审计 + 契约门） | `mvn -o -B test -Dtest='RetailSimpleTalkChainGateTest,QuestDialogOrderAuditTest,QuestClientContractGateTest'` | **20/20 绿**（2 + 17 + 1） |
| T1 | `run_quest_gates.sh T1` | 75 例 **1F** = 在册 lane 项 `20035`（`gates/T1-211816.log`，402s）；本片 18 id **零命中** |
| T2（18 id） | `run_quest_gates.sh T2 2611 … 49003` | 145 例 **7F**（`gates/T2-212512.log`）：4 条在册（`15613`/`1131`/`13965`/`20035`）+ 3 条**全量基线既有**（`AlignedMirrorRewardRowContractTest`×2 = 11294/26820、`Quest19004RetailAlignmentTest` = 19004 名占位 `Q19004`）——三条在 `T3-204340`/`T3-194812`/`T3-175816` **三份全量基线逐字在册** ⇒ 非本片 |
| T3（切片收口跑一次） | `QUEST_FORK_COUNT=2 run_quest_gates.sh T3` | **2013 例 / 117F / 22E / 1S**（`gates/T3-213222.log`，21:32:22–21:38:50，6m25s）——**与 P0c-47 收口全量基线 `gates/T3-204340.log` 身份集完全相同：ADDED ×0 / REMOVED ×0**（两侧 107 个失败方法）；本片 18 id 与关键 NPC/页里仅 `19004`/`203752` 命中，且都落在**全量基线既有的两个红点**里（`Quest19004RetailAlignmentTest` 的名占位 `Q19004`；`ReportToManyDialogRouteRegressionTest` 的 quest 3914 —— 3914 根本不在链登记表、属别族既有债） |

## 6. T3 归因

`gates/T3-213222.log`（2013 例 / 117F / 22E / 1S） vs **P0c-47 收口全量基线** `gates/T3-204340.log`（2013 例 / 117F / 22E / 1S）：
`ADDED ×0 / REMOVED ×0`（失败方法集合两侧同为 **107**）——**零新增失败**，且计数也逐位相同。

本片 18 个任务 id 与 34 个相关 NPC/页 id 在整份 T3 日志的命中只有两处，且均为**全量基线既有红点**：
1. `Quest19004RetailAlignmentTest.preservesTalkChainStepsAndRewards`（`expected: <Perikles's Insight> but was: <Q19004>`）——19004 的**元数据名占位**轴，在 `T3-204340`/`T3-194812`/`T3-175816` 三份全量基线逐字在册（每份 3 次命中）；
2. `ReportToManyDialogRouteRegressionTest.migratedReportNpcsOpenTheirPageFromStartDialog`（`missing START_DIALOG route for quest **3914**, npc 203752, page 1352`）——**3914 根本不在链登记表**（`awk` 零行），属 ReportToMany 别族的既有债，三份基线同样逐字在册。

⇒ T3 结论：**零新增失败归本片**；本片 13 个行为可见任务的 IR 变更 8 行、审计未达页 4 行闭合、指纹 13 行变化，均未在 T3 引入任何新红点。

## 7. 残留与登记

1. **阶段推进行 NPC 漂移（新登记 `STAGE_PAGE_ADVANCE_NPC_DRIFT_PENDING`）**：9 任务（1323/1394/1484/2480/2538/3093/4052/11010/11103，各 2 行）的 `SETPRO1` 推进行**不在真端 `talk_npc1` 上**（例：3093 `SETPRO1@203784` vs 真端 `talk_npc1=LF2A_NPC_Gastak`）⇒ 与阶梯轴（K 段/var0 投影 + 阶段归属）同域，须与 `apply_talk_ladder` 的声明一致性一批定判据；本片按「owner == `talk_npc<k>`」硬轴 fail-closed 剔除，未裁。
2. **接取入口页轴（新登记 `ACCEPT_ENTRY_PAGE_WRONG_PENDING`）**：7 条（35017@799806 / 45010@799848 / 45017@799849 / 45024@799842·799843 / 45026@799842·799843）——被剪 NPC 的**对话入口行**（`QUEST_SELECT`）下发的是阶段页（`SELECT2_1`）而非接取入口页（客户端 `select1` / `ASK_QUEST_ACCEPT` 系）；删了该 NPC 对话整条消失 ⇒ 本片保留、逐条登记（属 P0c-42 接取入口轴）。
3. 既有余量不变：`GATE_VS_ITEM_CHECK_PENDING`(21033/21455)、`XML_ONLY_ACCEPT/STAGE_ROLE_SPREAD_PENDING`(34/28 项)、`SELECT6_CLOSE_EXIT_OTHER_FAMILIES`(62)、`XML_RETENTION` 1011 未达(44)、21033 阶段 2 客户端页缺失、80320 页链断裂、真端数据缺口桶。
4. **运行时/客户端目检 PENDING**（未启服，AGENTS.md 规则 1）：18 任务中行为可见的 13 个（阶段页不再出现在错误 NPC 上）+ 39003/49003 的阶段入口页可点开。

## 8. 复现命令

```bash
D=.agents/summary/scriptdll-quest-driver
python3 -B $D/p0c54_stage_page_census.py                  # 普查（只读）
python3 -B $D/p0c54_stage_page_decisions.py               # 裁定表派生（只读）
python3 -B $D/p0c43_dryrun_diff.py                        # 干跑 == 安装（逐字节）
python3 -B $D/p0c42_builder_fidelity_check.py             # FIDELITY_OK + 22 张输入表零漂移
python3 -B $D/p0c54_blast_radius.py                       # 爆炸半径（pre=p0c54-registry-pre.tsv）
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest -Dretail.talkChain.fingerprintOut=/tmp/p0c54-fp.tsv
python3 -B $D/p0c54_refreeze_fingerprints.py /tmp/p0c54-fp.tsv --apply
mvn -o -B test -Dtest='RetailSimpleTalkChainGateTest,QuestDialogOrderAuditTest,QuestClientContractGateTest'
bash $D/run_quest_gates.sh T1 && bash $D/run_quest_gates.sh T2 2611 3001 3023 11070 11105 11106 11117 19004 21004 21036 21136 35017 39003 45010 45017 45024 45026 49003
QUEST_FORK_COUNT=2 bash $D/run_quest_gates.sh T3
python3 -B $D/p0c52_t3_attribution.py $D/gates/T3-204340.log <本次 T3 日志>
```

IR/审计前后对拍（临时探针，源码归档为 `.java.txt`，用后已从测试树删除）：`P0c54StagePageProbeTest.java.txt`（`-Dp0c54.irOut` / `-Dp0c54.questIds`）、`P0c54AuditProbeTest.java.txt`（`-Dp0c54.auditOut`）；pre 态用 `mvn -o -B surefire:test` 直跑（跳资源阶段，避免 `target/classes` 被覆写）。
