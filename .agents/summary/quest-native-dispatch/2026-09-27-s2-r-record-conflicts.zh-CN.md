# S2「R 记录 vs canonical 块边」冲突普查（策略 A 实施数据）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 车道：`quest-native-dispatch`；面：**SimpleTalk S2（链式面）**。
> 只读普查，**不落码、不改登记表、不改任何生产/测试文件**；本文与同名 TSV 是本轮唯一产物。
> 参照：勘察底稿 `2026-09-27-simpletalk-canonical-survey.zh-CN.md`（§3 策略 A/B/C）、
> S1 已落地的 `RetailSimpleTalkDefinitionCompiler`（单步面规范形 + `attachMovieToRoute` fail-closed）。

## 0. 输入指纹（复验用）

| 输入 | 行数 | md5 前 12 | mtime |
|---|---|---|---|
| `src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv` | 5000（4990 数据行 + 10 注释） | `0ae2d9243e96` | 2026-09-26 22:48:34 |
| `src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv` | 6226 | `24f8ca5d7950` | 2026-09-26 22:40:41 |
| `src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml` | 3152 行模板 | — | 2026-09-23 13:14 |
| `src/main/resources/aion/data/static_data/quest_retail/quest_client_dialog_exits.tsv` | 3942 | — | — |
| `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkDefinitionCompiler.java` | 1350 | `847e3b81c87c2` | **2026-09-27 12:48:43（S1 已落地）** |
| `src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDialogAction.java` | — | — | — |

产物自身指纹（本文自指纹以 `md5 -q <file>` 现算为准，写入动作本身会改变它）：

| 产物 | md5 前 12 | mtime |
|---|---|---|
| `2026-09-27-s2-r-record-conflicts.zh-CN.md`（本文） | （见下方说明） | （见下方说明） |
| `s2-r-record-conflicts.tsv` | `ac531acdcca0` | 2026-09-27 13:52:54 |

复算（只读，无中间产物）：

```bash
# 行集对账
python3 -B - <<'PY'
import re,collections
R='src/main/resources/aion/data/static_data/quest_retail/'
own={};fam={}
for ln in open(R+'retail-xml-retention.tsv',encoding='utf-8'):
    p=ln.rstrip('\n').split('\t')
    if len(p)>=5 and not ln.startswith('#'): own[int(p[0])]=p[1];fam[int(p[0])]=p[2]
s=open(R+'Quest_SimpleTalk.xml',encoding='utf-8').read()
x={int(b.split('"')[0]):{k:v.strip() for k,v in re.findall(r'<(\w+)>([^<]*)</\1>',b)}
   for b in re.split(r'<id\s+id="',s[s.index('<quest_simpletalks>'):])[1:]}
S2={q for q,d in x.items() if fam.get(q)=='SimpleTalk' and own.get(q)=='RETAIL_TABLE'
    and any(k.startswith('talk_npc') and v for k,v in d.items())}
kind=collections.Counter()
for ln in open(R+'quest_client_talk_chain_steps.tsv',encoding='utf-8'):
    if not ln.startswith('#') and ln.strip(): kind[ln.split('\t')[1]]+=1
print(len(S2),dict(kind))     # 285 {'P':295,'N':1562,'B':779,'R':2299,'C':6,'E':46,'I':3}
PY
```

## 1. S2 行集口径对账（285 vs 295）

| 口径 | 数量 | 说明 |
|---|---|---|
| `RETAIL_TABLE` ∩ `family=SimpleTalk` | 2089 | 交付面全集 |
| ├ 有 `talk_npcN` = **S2 行集** | **285** | `chain_steps` 表内**全部**出现（差集 ∅，无遗漏） |
| └ 无 `talk_npcN` = S1 单步面 | 1804 | 本轮不动 |
| `chain_steps` 表内出现的 id | **295** | |
| 表内 − S2 = **多出 10 个** | 10 | 全部 `owner=XML_RETENTION, family=SimpleTalk`（1938/2231/2641/2653/2724/18805/19070/19071/21081/28805）；**少掉 0 个** |
| S2 内 R 记录 | **2235** 条 / 280 个 id | 5 个 S2 id 无任何 R/C/Q/E/B 记录 = 纯规范块残组行（与勘察 §3.3「5 行」吻合） |
| 表内非 S2 的 10 个 id 携带 R 记录 | 64 条 | 属 XML_RETENTION 面，**不在本片范围** |

⇒ 本片分析面 = **285 个 id / 2235 条 R 记录**。非 `R` 记录（`C` 6 条 / `E` 46 条 / `Q` 0 条）**不产生 `TalkToNpc` 边**，
既不进 `explicitRoutes` 也不进 `blockTransitions`，**策略 A 不涉及**（详见 §2.1）。

## 2. 块边键生成规则（逐 helper 实读 `buildChain`，S1 后当前形态）

覆盖判定键 = `sourceNode + ":" + npcId + ":" + dialogId`（`:505-515`）；**块间同键去重**（`:519-528`）键多带
`priority`，只作用于 `blockTransitions` **内部**，与 R 记录无关——任务书里"触发同键去重"的表述需按此澄清：
**R 记录只通过 `explicitRoutes` 覆盖块边，不参与去重**。

| helper | 位置 | 键生成规则 | S2 后 |
|---|---|---|---|
| `acceptFlowChain` → `acceptFlow` | `:778` / `:1036` | `(unaccepted,npc,{1007,1002,20000,1003,1004,20001})` ∪ `(unaccepted,npc,31)` ∪ `({unaccepted}∪target,npc,1008)`；`acceptFlowChain` 再删 1008 全部后按 `finishSources={unaccepted,target}∪selection-sources` 重加 | **翻 canonicalAcceptFlow**：删 `1007`、删 selection-sources 分量；`31/1002/20000/1003/1004/20001/1008(仅{unaccepted,target})` 保留 |
| `acceptContinuation` | `:1071` | `(unaccepted,npc,1012[,1013])`（受 `exits.SELECT1_1/SELECT1_1_1` 驱动） | **退场**（S1 单步面已删，链面同步） |
| `reportFlowChain` | `:799` | 恒有 `(source,npc,31)`；`checkButton`(SELECT5_CHECK/SIMPLE) ? `{(source,npc,39)×2,(source,npc,20002)×2[, (source,npc,1008)]}` : `{(source,npc,1009)(×2，requiresItems 时 prio 0/1)}` | **翻 canonicalDelivery**：仅保留 `(source,npc,31)`（带整组门 + 档位窗）；`39/20002/1008/1009` 退场 |
| `itemReportGate` | `:866` | `(source,npc,{39,20002})` × 成功/失败对 | **退场**（I 记录整类，39/20002 不再下发） |
| `completeFlowFromBlock` | `:1116` | `(source,npc,d)`，`d ∈ preview ∪ actions ∪ choice ∪ fallback` 展开（`preview` 含 `USE_OBJECT → -1`、`SELECT_QUEST_REWARD → 1009`；`choice=RETAIL` 时为 8..23） | **不动**（勘察 §3.1，已是规范形 8..23） |

**实读更正**：任务书示例里的"1002/20000/1008/1011/1012"中，`1011/1012` 是 `SELECT1`/`SELECT1_1`
（接取页链动作），只在**退场**的 `acceptContinuation` 出现（1012/1013），**不在** canonical 接取键集内；
canonical 接取键集 = `{31,1002,20000,1003,1004,20001,1008}`。

### 2.1 非 R 记录不产生键（已核）

`RetailClientTalkChainSteps.load` 把 `C→CAN_ACT`、`Q→QUEST_ACTION`、`E→ENTER_WORLD`，只有 `R→Talk`；
`buildChain` 只在 `event instanceof QuestEvent.TalkToNpc` 时收集 `explicitRoutes` ⇒ **C/Q/E 永不参与覆盖、也不被过滤**。

## 3. R 记录全量分类（双口径）

策略 A 的过滤集合大小**完全取决于 S2 的覆盖范围决策**（见 §7-1），故给出两个口径：

- **口径 α（保守）**：只改造**已有 B 块**（`NPC_START/NPC_REPORT/NPC_COMPLETE`），不为"无块、纯 R 驱动"的行合成 canonical 边。
- **口径 γ（全量，主口径）**：α + 为 R 驱动段合成 canonical 边（接取段取 `unaccepted` 源 `ACC1/ACCS` 边的 npc；
  交付段取 `target=reward` 的 `31/1009` 边的 `(source,npc)`）。

| 分类 | 定义 | γ | α |
|---|---|---|---|
| `A_filter_keep` | 键 ∈ **S2 canonical 块边**（同键会覆盖新块边 ⇒ 必过滤） | **201** | **0** |
| `B_filter_retire` | 键 ∈ **S2 退场块边**（页链块边退场后这些 R 边成无主残留 ⇒ 必过滤） | **50** | **50** |
| `C_shadow` | 键命中**保留块边**（NPC_COMPLETE preview）但 action 非策略 A 目标 | **121** | 121 |
| `D_canon_no_hit` | canonical 键动作、键不命中任何块边 | 681 | 882 |
| `D_keep` | 非 canonical 键动作（页链/推进/物品轴） | 1182 | 1182 |
| 合计 | | 2235 | 2235 |

**A 类不存在于口径 α** —— 即：**若 S2 只改造已有块，则"R 记录覆盖 canonical 块边"这一假绿源在当前数据里为 0 条**；
A 类 201 条全部来自"无块、纯 R 驱动"的行（19 个 synth 接取 id + 48 个 synth 交付 id，14 个重叠，合计 53 个 id）。

### 3.1 A 类 201 条构成（口径 γ）

| 命中来源 | 条数 | action 分布 | 涉及 id |
|---|---|---|---|
| `ACCEPT:canon(synth)`（R 驱动接取段） | 145 | `QUEST_SELECT` 20 / `FINISH_DIALOG` 28 / `QUEST_ACCEPT_1` 21 / `QUEST_ACCEPT_SIMPLE` 17 / `QUEST_REFUSE_1` 21 / `QUEST_REFUSE_2` 20 / `QUEST_REFUSE_SIMPLE` 18 | 19（1323/2611/3001/3023/21136/24202/35010/35017/35018/35024/35025/45010/45011/45017/45018/45024/45025/45026/80320） |
| `DELIVER:canon(synth)`（R 驱动交付段） | 56 | 全 `QUEST_SELECT` | 48（1118/1163/1323/1394/1484/1851/2480/2538/2553/2646/2953/3001/3023/3093/3100/3218/3966/4052/4209/4218/11010/11103/13809/21004/21033/21036/21065/21071/21217/21455/23809/28809/30711/30761/35010/35011/35018/35024/35025/35026/45010/45011/45017/45018/45024/45025/45026/80752） |

### 3.2 B 类 50 条（唯一"确定性必过滤"面）

50 条全部是 `unaccepted→unaccepted` 的 `SELECT1_1`（客户端 select1 续页按钮），键 `(unaccepted,npc,1012)`
命中 `acceptContinuation` 块边；**载荷全零**（无 conditions/actions/priority/after 之外内容）。
涉及 **50 个 id**：1152 / 1156 / 1158 / 1537 / 1560 / 1628 / 1913 / 1914 / 1915 / 1916 / 1928 / 2383 / 2414 / 2433 /
2486 / 2501 / 2538 / 2630 / 2651 / 2914 / 2921 / 2953 / 3008 / 3037 / 3041 / 3083 / 3087 / 3091 / 3093 / 3102 /
3218 / 3972 / 4001 / 4036 / 4218 / 9550 / 9558 / 9559 / 11010 / 11103 / 11106 / 11109 / 11228 / 21004 / 21068 /
21071 / 21106 / 21110 / 21111 / 30154。

### 3.3 C 类 121 条（真冲突但非策略 A 目标）

全部是 `reward→reward` 的 `SELECT_QUEST_REWARD`（after = `SHOW_SELECTION_PAGE:SHOW_SELECT_QUEST_REWARD_WINDOW1`），
键 `(reward,npc,1009)` 命中 **`NPC_COMPLETE` preview** 块边（`ShowQuestDialog(previewPage)`）。
**载荷 9 cond / 9 acts / 18 prio** —— 即现状下这些 R 记录已把 preview 块边覆盖掉（形状差：Selection 页 vs 普通页）。
涉及 112 个 id。S2 不改 `NPC_COMPLETE` ⇒ 该覆盖**维持现状**；策略 A **不应**顺手过滤（会改变奖励窗重开语义）。

## 4. 策略 A 的精确输入

### 4.1 必须过滤（`explicitRoutes` 排除 + 不回放）

| 档 | 定义 | 条数 | 键数 | 涉及 id |
|---|---|---|---|---|
| **F1（确定性）** | 键 ∈ 退场块边（`1012/1013`） | **50** | `(unaccepted,npc,1012)` × 50 | 50（§3.2 清单） |
| **F2（条件性）** | 键 ∈ R 驱动段合成的 canonical 块边 | **201** | 接取 `(unaccepted,npc,{31,1002,20000,1003,1004,20001,1008})` + 交付 `(source,npc,31)` | 53（§3.1 清单） |

- 口径 α 下过滤面 = **F1（50 条 / 50 id）**；口径 γ 下 = **F1+F2（251 条 / 94 id）**。
- **键数**：F1 = 50 个键（每个 id 1 个）；F2 = 145+56 = 201 个键（无重复键）。

### 4.2 必须保留

| 档 | 定义 | 条数 | 说明 |
|---|---|---|---|
| **R1** | 非 canonical 键动作 | **1182** | `SETPRO1..3` 300/111/47、`USE_OBJECT` 50、页链按钮 `SELECT2_1` 154 / `SELECT3_1` 62 / `SELECT4_1` 29 / `1353` 102 / `1694` 41 / `2035` 18、`SELECT5` 页 14、`SELECT2` 10、`ASK_QUEST_ACCEPT` 135、`SELECT_QUEST_REWARD` 192、区间确认 13 等 —— 链中间步骤驱动边，canonical 形无替代 |
| **R2** | canonical 动作锚在**链中间节点**（`chain_mid`） | **174** | `QUEST_SELECT` 170（`s1` 60 / `k1` 31 / `s2` 21 / `k2` 12 / `stage1` 7 …）+ `FINISH_DIALOG` 4 |
| **R3** | `C/E` 记录（S2 内 `C` 6 条 / `E` 42 条；`Q` 0） | 48 | 不产生键，策略 A 天然不涉及（另 4 条 `E` 在非 S2 的 XML_RETENTION 行上） |

### 4.3 必须显式裁定（fail-closed 候选）

**判据**：`action ∈ {31,1008,1002,20000,1003,1004,20001}` 且键不命中任何块边 且锚在**段端点**
（`accept_source` / `accept_target` = `NPC_START.target` / `report_source` = `NPC_REPORT.source` /
`complete_source` = `NPC_COMPLETE.source`）—— 这些动作在 canonical 形里只能由块边承接，
**不得静默保留**（要么在 S2 规格里显式登记为"链式段端点语义"，要么报错）。

| action | accept_source | accept_target | report_source | complete_source | 合计 |
|---|---|---|---|---|---|
| `QUEST_SELECT`(31) | 1 | 380 | 3 | 114 | 498 |
| `FINISH_DIALOG`(1008) | 0 | 5 | 4 | 0 | 9 |
| **合计** | 1 | 385 | 7 | 114 | **507**（涉及 247 个 id） |

其中 **134 条 / 132 id** 是"接取 NPC 自身的进行态开页"（`(started, 接取NPC, 31)`，npc == `NPC_START.npc`）——
与勘察 §3.5 的电影迁移、以及 canonical 形接取段无 `started` 态 31 边这一事实直接相关，**必须逐条定性**。

**另有 F1 残余**：`SELECT1_1` 53 条中 3 条不命中退场块边（纯 R 驱动的续页），须与 F1 同批显式处理。

### 4.4 过滤的载荷损失面（已逐条核）

| 载荷 | 条数 | 是否真损失 | 处置 |
|---|---|---|---|
| `START_ELIGIBLE`（ACC1/ACCS） | 38 | **否** —— canonicalAcceptFlow 的 `1002/20000` 边自带 `StartEligible` | 无 |
| `VAR_IS:var0=3`（交付页门） | 2（q=3001 seq12 / q=3023 seq12） | **是** —— 过滤后丢"仅 var0=3 才开交付页"的门 | 迁到 canonical 交付边条件或保留该条 |
| `GIVE_ITEM:182207919:1`（接取发物） | 1（q=21136 seq3） | **是**（可迁） | 走 `canonicalAcceptFlow(..., acceptActions)` 第三参（P0c-10m 通道，判例 1131） |
| `priority` | 0 | — | — |

### 4.5 `MOVIE_CARRIER` 单列（4 条，链式面全部过场承载边）

TSV 第 18 列 `movie_carrier` 对这 4 条给出显式标记（标记单列而不并入 `class_gamma`，是为了让分类列保持可 awk 聚合；
检索用 `grep MOVIE_CARRIER`）。登记表**全表仅 4 处 `MOVIE:` token**，逐条取证如下：

| quest_id | seq | 动作（按钮语义） | 键 | 覆盖键（α/γ） | 分类 | 电影 | 页下发行 | canonical 后承载边可达性 |
|---|---|---|---|---|---|---|---|---|
| 1422 | 4 | `SELECT2_1`(1353) select2 页"继续"按钮 | `(started,203731,1353)` | 无 | 残差 `D_keep` | 100 | R3 `QUEST_SELECT→SELECT2`（**中间链**） | **可达**（中间链页链保留） |
| 2421 | 4 | `SELECT2_1`(1353) 同上 | `(started,204187,1353)` | 无 | 残差 `D_keep` | 132 | R3 `QUEST_SELECT→SELECT2`（中间链） | **可达** |
| 3006 | 8 | `SELECT3_1`(1694) select3 页"继续"按钮 | `(s1,700339,1694)` | 无 | 残差 `D_keep` | 361 | R7 `QUEST_SELECT→SELECT3`（中间链） | **可达** |
| 3020 | 2 | `ASK_QUEST_ACCEPT`(1007) —— **S2 退场动作**（`select1`/`select1_1` 页按钮，勘察 §2.3） | `(started,798143,1007)` | 无 | 残差 `D_keep` | 363 | R1/R3（`DEFAULT_SUCCESS`/`SELECT2`，**非** select1） | **存疑（fail-closed）** |

**回答两个问题**：

1. **全部 4 条都属残差类**（键不在 canonical 块边集内，口径 α/γ 一致，均 `D_keep`）——`1353/1694/1007` 都不是
   canonical 键动作（`1007` 是 S2 **明确删除**的合成动作，`1353/1694` 是页链中转按钮），故命中键集为 ∅。
2. **原先不在任何"必须过滤/必须 fail-closed"清单里 —— 这是本普查的判据缺口，现予修补**：
   我原来的 fail-closed 判据只覆盖「action ∈ canonical 键动作 且 锚在段端点」，**没有覆盖"承载事件被 S2 废弃 + 带 `MOVIE:`"**这一面。
   修补后处置分档：

| 档 | 记录 | 判据 | 处置 |
|---|---|---|---|
| **M1 必须成对处理（迁移 + 过滤）** | **3020 seq2** | action `1007` 是 S2 退场动作（canonical 接取形不再合成、且不再驱动 select1 页）⇒ 边**可达性存疑**；勘察 §3.5 已定其迁移落点 = 接取开窗 `QUEST_SELECT` 边 | 迁移电影 363 到 canonical 接取开窗边，**并同时过滤本条** |
| **M2 条件性成对处理** | **1422 seq4 / 2421 seq4 / 3006 seq8** | 承载边由**中间链页链**驱动，中间链保留时可达；仅当 S2 决定清理中间链页链（勘察 §3.5 假定的方案）时失效 | 若清理中间链 ⇒ 迁移到对应 briefing/报告 canonical 边 + 过滤；若不清理 ⇒ **保持现状即可**（电影照播） |

**关于"保留 = 双份 `PlayMovie`"的精确化（与 team-lead 表述有一处出入，如实报）**：

- `attachMovieToRoute` 的 fail-closed 断言数是**匹配的路由条数**（`:1020` `matches != 1`），不是电影数。
  因此「迁移 + 不过滤」**不会**触发它：canonical 边仍匹配 1 条 ⇒ 断言通过 ⇒ **静默双播**（两条边各带一个 `PlayMovie`），比报错更危险。
- 反之「过滤 + 不迁移」⇒ 电影静默丢失，`attachMovieToRoute` 同样抓不到（它只保证 canonical 边 match==1，不检查 R 边是否被删）。
- ⇒ **迁移与过滤必须成对**；且链门必须补一条**电影数守恒断言**（每 id 的 `PlayMovie` 总数 == 该 id 真端 `cutsceneid1` 声明的电影数），
  否则上述两种错误**在当前机制下都无红灯**。这是本片第二个必须新增的防假绿机制（第一个见勘察 §2.3 的作用域匹配器）。

> 口径 γ 的 F2 过滤面与 §4.5 的 M1/M2 是**并列增量**（M1 的 3020 在全量清理中间链时才计入 M2 的 3 条）。

## 5. 量化：过滤前后 action 记录数（口径 γ，F1+F2 为过滤面）

| action | 过滤前 | 过滤后 | Δ |
|---|---|---|---|
| `QUEST_SELECT` | 744 | 668 | **−76** |
| `SELECT1_1` | 53 | 3 | **−50** |
| `FINISH_DIALOG` | 41 | 13 | **−28** |
| `QUEST_REFUSE_1` | 21 | 0 | **−21** |
| `QUEST_ACCEPT_1` | 21 | 0 | **−21** |
| `QUEST_REFUSE_2` | 20 | 0 | **−20** |
| `QUEST_REFUSE_SIMPLE` | 18 | 0 | **−18** |
| `QUEST_ACCEPT_SIMPLE` | 17 | 0 | **−17** |
| 其余 20 类（`SETPRO*`/`USE_OBJECT`/`1353`/`1694`/`2035`/`SELECT2_1`…/`SELECT_QUEST_REWARD` 192/`ASK_QUEST_ACCEPT` 135/区间 13 等） | 1300 | 1300 | 0 |
| **合计** | **2235** | **1984** | **−251** |

口径 α（仅 F1）：2235 → 2185（`SELECT1_1` 53→3，其余全不变）。

## 6. 对账与风险

1. **与勘察 §3.3 的代理指标一致**：本轮独立复算得"带 ≥1 个 `QUEST_SELECT` R 记录的行 = **268**"、
   "带 ≥1 个 canonical 键动作的行 = **269**"，与勘察表格逐字相同 ⇒ 口径无误。
2. **但"96% 假绿"不是键冲突面**：269 是"含 canonical 键动作"的**代理指标**；精确键冲突面是
   **口径 γ 251 条 / 94 id（33% 的 id）**、**口径 α 50 条 / 50 id（18% 的 id）**。
   关键事实：canonical 键动作的 R 记录共 882 条，其中 **681 条（=D_canon_no_hit）键不命中任何块边**
   ——它们锚在链中间步骤或段端点上，**不会被 `explicitRoutes` 覆盖，反而是"保留的旧形状"**。
   因此"假绿"的准确量化口径应为：**S2 后仍以 R 记录原形存在的边数 = 2185（α）/ 1984（γ）**。
3. **§3.5 的 4 行链式电影不受策略 A 影响**（逐条核）：1422/2421/3006/3020 的承载边
   `1353`(R4/R7)、`1694`(R8)、`ASK_QUEST_ACCEPT`(R2) 全部落在 **`D_keep`**（非 canonical 动作、键不命中）
   ⇒ F1/F2 都不会删到它们；C 类的 `SELECT_QUEST_REWARD`(R7) 也不在过滤面。迁移方案可独立实施。
4. **`reportNpc` 名解析覆盖风险**：`buildChain:483-485` 会用 `reward_npc_name` 解析值替换 B 记录的 `report.npcId()`。
   抽查 205 个含 `NPC_REPORT` 块的行：交付边 `(target=reward)` 与 B 记录 npc 一致 118、不一致 2（**39003**：块 npc=800504 vs R 交付边 npc=800512；**49003**：800505 vs 800511）、无交付边 88。
   ⇒ 若 F2 的交付段键在 39003/49003 上因名解析而改 npc，需以解析值为准重判（数量级 2 行，不影响总量）。
5. **`SELECT1_1` 的 3 条残余**与 **507 条段端点 canonical 动作**是本片**唯一需要人工裁定**的面（§4.3）。

## 7. 未决项（需 lane owner 裁定）

1. **S2 覆盖范围**：只改造已有 B 块（口径 α，过滤面 50 条）还是同时覆盖"无块、纯 R 驱动"的段（口径 γ，过滤面 251 条 / 94 id）？
   后者会让 A 类 201 条从"保留"变为"必须过滤"，并需要新增合成逻辑（19 个接取 id + 48 个交付 id）。
2. **507 条段端点 canonical 动作**的定性（保留为链式语义 / 报错）——建议按 §4.3 表格逐类在 S2 规格中登记。
3. **C 类 121 条**（`(reward,npc,1009)` 覆盖 NPC_COMPLETE preview）是否在本片顺带处理，或维持现状。
4. §4.4 的 **2 条 `VAR_IS` 门**与 **1 条 `GIVE_ITEM`** 的迁移落点。
5. F1 的 50 条在链门上如何留痕（`retail-simple-talk-chain-ir-fingerprints.tsv` 285 行会整体漂移，需重冻通道留痕）。
6. **`MOVIE_CARRIER` 4 条（§4.5）**：M1（3020，`1007` 退场动作承载边可达性存疑）必须先裁定；M2（1422/2421/3006）
   取决于"S2 是否清理中间链页链"这一覆盖范围决策——**若清理则必须迁移 + 过滤成对执行**，否则静默双播 / 静默丢失。
   配套：链门"电影数守恒"断言（本片第二个必增的防假绿机制）。
7. `FAILCLOSED` 判据已按 §4.5 修补：除「canonical 动作锚在段端点」外，须再覆盖「**承载事件被 S2 废弃 + 带 `MOVIE:`/其它 after 载荷**」的记录。
