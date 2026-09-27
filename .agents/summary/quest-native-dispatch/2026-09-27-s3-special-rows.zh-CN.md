# SimpleTalk S3（γ 面：82 行纯 R 驱动链行）特例行逐行取证与处置提案

> 车道：`quest-native-dispatch`；面：**SimpleTalk S3（γ 面）**；角色：**只读取证**（不落码、不改任何生产/测试/TSV）。
> 上游：S2 收口 `2026-09-27-s2-canonical-segments.zh-CN.md`（§1 切片边界、§6 未决 4 条）、
> 规格 `2026-09-27-s2-buildchain-spec.zh-CN.md`（§4 `I` 记录、§3.4 层 B1）、
> 台账 `2026-09-27-s2-r-record-conflicts.zh-CN.md`（§3.1 A 类 201 条、§4.4 载荷损失面）。
> 纪律回执：未 commit/push；未启停服务；未跑 Maven/编译/测试；python3 全部 `-B`；临时产物仅 `/tmp`；
> 本文件是本轮**唯一**产物。

## 0. 输入指纹（复验基线）

| 输入 | md5(12) | mtime |
|---|---|---|
| `src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv` | `0ae2d9243e96` | 2026-09-26 22:48:34 |
| `src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv` | `24f8ca5d7950` | 2026-09-26 22:40:41 |
| `src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml` | `85d84d329fd2` | 2026-09-23 13:14:18 |
| `src/main/resources/aion/data/static_data/quest_retail/quest.xml` | `f3c901c7cb0b` | 2026-09-23 00:38:02 |
| `src/main/resources/aion/data/static_data/quest_retail/quest_client_dialog_exits.tsv` | `32a4d5f9cf08` | 2026-09-26 10:31:31 |
| `RetailSimpleTalkDefinitionCompiler.java` | `afa06e48fe8d` | 2026-09-27 14:32:25 |
| `RetailSimpleHuntDefinitionCompiler.java` | `b2153dda42eb` | 2026-09-27 09:44:24 |
| `RetailSimpleCollectItemDefinitionCompiler.java` | `fcfb508bb077` | 2026-09-27 09:50:55 |
| `RetailClientTalkChainSteps.java` | `8c0b827c9a6c` | 2026-09-26 10:44:35 |
| `QuestDefinitionCompiler.java` | `9807124fc28c` | 2026-09-25 12:50:25 |
| `docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv` | `3d72065956db` | 2026-09-11 22:59:04 |
| `docs/quest/client-dialog-mapping/quest-dialog-pages.csv` | `f96aee615f5b` | 2026-08-13 13:48:27 |
| `docs/quest/client-dialog-mapping/quest-dialog-action-details.csv` | `f77359b3c284` | 2026-08-13 13:48:27 |
| `2026-09-27-s2-canonical-segments.zh-CN.md` | `d9f158a973c8` | 2026-09-27 15:40:49 |
| `2026-09-27-s2-buildchain-spec.zh-CN.md` | `13863bc6732f` | 2026-09-27 13:58:25 |
| `2026-09-27-s2-r-record-conflicts.zh-CN.md` | `5216428e5c17` | 2026-09-27 13:53:23 |

## 0.1 任务书三处前提偏差（先纠，后续各节按修正后口径）

| # | 任务书表述 | 实测 | 影响 |
|---|---|---|---|
| **A** | 「17 行**无任何 B 块**的行（含 3001/3023/**21136/24202**/35010/35018/35024/35025/…/1323 一类）」 | **17 行是"无 NPC_START **且** 无 NPC_REPORT 块"**（= 任务书 §2/§5 里那 19 行的子集去掉 `2611`/`35017`）；其中**只有 7 行无任何 B 块**：`3001 3023 35025 45010 45017 45024 80320`。另 **10 行有 `NPC_COMPLETE`**：`1323 21136 24202 35010 35018 35024 45011 45018 45025 45026` | §7 的"能否合成 canonical 接取"判据必须按 7 行 vs 17 行两档给；把 10 个有 COMPLETE 的行当"无块行"会漏掉它们的 `NPC_COMPLETE` 预览边（reward 态重开窗载体） |
| **B** | 「`24202` 无 START」 | ✅ 成立（`24202` 只有 `B NPC_COMPLETE`）；「`80320` 无任何块」✅ 成立 | — |
| **C** | S2 §1 的 `G2=80 / G3=2` 与 `G2=63 / G3=2 / NEITHER=17` 两组口径 | 均正确但**分桶不同**：S2 的 `G2` 是"无 REPORT 块"（63+17=80），我按"有 START 无 REPORT"得 63。**82 = 63 + 2 + 17**（互斥完备） | 切片设计时必须固定一个互斥口径，建议用「有 START? × 有 REPORT?」四分桶 |

**另有两条与 S2 台账不符、需更正的实测**：

1. **`1152` 的 `item_check` = 1（不是 0）**。`Quest_SimpleTalk.xml` 的 1152 行内确有 `<item_check>1</item_check>`（与 24202/80320 同）。任务书与台账都没有把它当门丢失风险；实测 `entry.itemCheck()==true`（`P0c-34` 的三方 cross-check 也证明三点都走同一通道）。**⇒ §2 的三形判定与我最初从登记表推断的"1152 走空门形"相反。**
2. **`80320` 的 `quest.xml` 存在 `check_item1_1 = quest_80320a 24`（= 2×collect_item1 的 12）**。`itemRequirements` 只认 `collect_item*`（`RetailQuestMetadataCompiler:239`），故不影响门值；但该双写是"门值与校验值不一致"的既有事实，迁移时必须写明以 `I` 记录/`collect_item` 为准，否则后人照 `check_item` 抄会翻倍。

---

## 1. γ 面（82 行）结构分区（本次取证的骨架）

判据：`RETAIL_TABLE ∩ family=SimpleTalk ∩ talk_npcN 非空` = 285 行；`G1 = 有 START ∧ 有 REPORT` = 203；γ = 82。

| 分区 | 定义 | 行数 | ids |
|---|---|---|---|
| **P1** | 非系统 ∧ **有 NPC_START**（接取段 = 现成规范块） | **61** | 60 行带 COMPLETE；1 行带 START 无 COMPLETE |
| **P2** | **有 NPC_REPORT**（交付段 = 现成规范块） | **2** | `2611`（非系统，无 START）/ `35017`（系统发放） |
| **P3** | 非系统 ∧ 无 START ∧ 无 REPORT | **6** | `1323 3001 3023 21136 24202 80320` |
| ├ 其中**无任何 B 块** | | **3** | `3001 3023 80320` |
| └ 其中只有 `NPC_COMPLETE` | | **3** | `1323 21136 24202` |
| **P4** | **系统发放**（`grantKind().systemGrant()`） | **14** | `35010 35011 35017 35018 35024 35025 35026 45010 45011 45017 45018 45024 45025 45026`（**全为 `_faction_`**） |
| **合计** | | **82** | |

块签名全量普查（`系统? × START? × REPORT? × COMPLETE?`）：`npc,-,-,-`3 / `npc,-,-,C`3 / `npc,-,R,-`1 / `npc,S,-,-`1 / `npc,S,-,C`60 / `sys,-,-,-`4 / `sys,-,-,C`7 / `sys,-,R,C`1 / `sys,S,-,-`2 = **82**。

**决定性的三条结构性事实（本片最重的发现）**：

1. **γ 的 14 个系统发放行全在 `_faction_` 一类**（无 `_area_` / `_challenge` / 未知哨兵）⇒ 系统发放面在 γ 里是**同质**的；且 `35011/35026` 虽带 `NPC_START` 块，`buildChain:629-633` 的 `if (systemGrant)` 分支**先于** START 分支 ⇒ 它们的 START 块**永不展开**（不是"展开后被过滤"，是从未展开）。
2. **`61/82` 行的接取段本来就是规范块**（P1）⇒ γ 里"接取段 canonical 化"对它们**零合成成本**：只需把 `canonicalSegments`（`:616-618`）的判据从"双块"放宽到"有 START ∧ 非系统"，`:641-654` 的 `canonicalAcceptFlow` 分支即刻可用。
3. **`203/203` 的 G1 行都带 `NPC_COMPLETE` 块**（实测 0 例外）——这是 S2 敢把 `reportNpcs` 上 `SELECT_QUEST_REWARD` 整词表退场的**隐含前提**：`completeFlowFromBlock`（`:1379+`）承接了 reward 态领奖握手。γ 里 **`11/82` 行没有 `NPC_COMPLETE`**：`2611 3001 3023 4970 35011 35025 35026 45010 45017 45024 80320` ⇒ 对这 11 行，**reward 态重开窗的载体只有 R 记录**（见 §5/§6 的逐行裁定）。

---

## 2. 特例 1：`I` 记录独占交付（`1152` / `24202` / `80320`）

### 2.1 原始记录（全量）

**`1152`**（15 行；`B NPC_START 203132` + `B NPC_COMPLETE 203130`，**无 NPC_REPORT**）：
```
1152	I	203130	pepper	reward	169400112	1	-	-
1152	B	NPC_START	203132	unaccepted	started	unaccepted started|SELECT1|GIVE_ITEM:182200526:1
1152	B	NPC_COMPLETE	203130	reward	complete	cri=0|fixed=0 1|actions=SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD|finish=SELECTION_DIALOG|preview=USE_OBJECT SELECT_QUEST_REWARD|choice=-|fallback=-
1152	R	1	203130	QUEST_SELECT	started	started	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT2
1152	R	2	203130	SELECT2_1	started	started	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT2_1
1152	R	3	203130	SETPRO1	started	pepper	-	REMOVE_ITEM:182200526:1	SYNC:PACKET_ONLY;CLOSE
1152	R	4	203130	QUEST_SELECT	pepper	pepper	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT5
1152	R	5	203130	FINISH_DIALOG	pepper	pepper	-	-	DIALOG:SHOW_SELECTION_PAGE:SELECT_QUEST
1152	R	6	203132	SELECT1_1	unaccepted	unaccepted	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT1_1
```
**`24202`**（30 行；仅 `B NPC_COMPLETE 205150`）：
```
24202	I	205150	s2	reward	182215465	1	-	CLOSE
24202	E	reward	STATUS_IS:REWARD;VAR_IS:var0=1	SET_VAR:var0=2	SYNC:LEVEL_AND_VISIBILITY_REFRESH
24202	R	10	205150	QUEST_SELECT	s2	s2	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT5
24202	R	11	205150	SET_SUCCEED	s2	reward	-	-	SYNC:LEVEL_AND_VISIBILITY_REFRESH;CLOSE
```
**`80320`**（25 行；**零 B 块**）：
```
80320	I	831427	s1	reward	182215303	12	-	-
80320	R	10	831427	QUEST_SELECT	s1	s1	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT5
80320	R	11	831427	SET_SUCCEED	s1	reward	-	-	SYNC:LEVEL_AND_VISIBILITY_REFRESH;CLOSE
80320	R	12	831427	USE_OBJECT	reward	reward	-	-	DIALOG:SHOW_QUEST_PAGE:SHOW_SELECT_QUEST_REWARD_WINDOW1
80320	R	13	831427	SELECT_QUEST_REWARD	reward	reward	-	-	DIALOG:SHOW_QUEST_PAGE:SHOW_SELECT_QUEST_REWARD_WINDOW1
```

### 2.2 `I` 记录的门语义（形状真源：`itemReportGate:1129-1179`）

| 列 | 语义 | 落地形状 |
|---|---|---|
| `npc_id` | 门宿主 NPC | `TalkToNpc(npc, 39)` 与 `(npc, 20002)` 两组 |
| `source`/`target` | 必须投影 START / REWARD（`:1136-1141` 抛错守卫） | 成功边 `source → target`，失败边 `source → source` |
| `item_id`/`required` | 门物品与数量 | `cond = [HasItem(item, required)]` |
| `remove_count` | `-` → = `required`；`ALL` → `ALL`；数字必须 == required（`:1142-1153`） | `act = [RemoveItem(item, removeCount)]` |
| `failure_page` | `-` → `ShowQuestDialog(SELECT6)`；`CLOSE` → `CloseDialog()` | 失败边 after |

三条记录实测：

| quest | 行 | 门 | 失败页 | 通报页承载者 |
|---|---|---|---|---|
| `1152` | `203130 pepper→reward` | `HasItem(169400112,1)` / `RemoveItem(169400112,1)` | `SELECT6`（`-`） | `R4 QUEST_SELECT pepper→pepper → SELECT5`；exits=`SELECT5_CHECK` ⇒ 客户端 select5 按钮 = 39 ✅ 自洽 |
| `24202` | `205150 s2→reward` | `HasItem(182215465,1)` / `RemoveItem(182215465,1)` | `CLOSE`（exits **无** `SELECT6` ✅ 自洽） | `R10 … s2→s2 → SELECT5`；exits=`SELECT5_CHECK_SIMPLE` ⇒ 按钮 = 20002 ✅ |
| `80320` | `831427 s1→reward` | `HasItem(182215303,12)` / `RemoveItem(182215303,12)` | `SELECT6` | `R10 … s1→s1 → SELECT5`；exits=`SELECT6 SELECT5_CHECK` ⇒ 39 + 失败页 ✅ |

**"独占交付"核实结论（精确化）**：三条的 `I` 门都是**唯一的客户端可达交付路径**，但"唯一"的强度不同：
- `1152`：`pepper` 节点上除 `R4` 外**没有任何页下发行**；`R3 SETPRO1 started→pepper` 只 `SYNC+CLOSE`。⇒ 去掉 `R4` 后 `I` 门的两条边仍在（键 `(pepper,203130,39/20002)` 不被显式路由覆盖），**但没有任何页承载其按钮** = 交付不可达。
- `24202`：同形（`R10` 是 `s2` 上唯一页下发）。另有 `R11 SET_SUCCEED s2→reward`（**无门**）——但 24202 的客户端页表**没有任何页发出 `HACTION_SET_SUCCEED`**，且 `SET_SUCCEED` 不在 exits 登记 ⇒ `R11` **非客户端可达**，不构成第二条交付路径。
- `80320`：同形（`R10` 唯一）。`R11 SET_SUCCEED s1→reward` 同上（80320 页表亦无 `SET_SUCCEED`）。

### 2.3 若走 canonical 交付：`canonicalChainHandIn` 三形能否承接

`canonicalChainHandIn(entry, reportItems, retiredRoutes)`（`:526-541`）三形：

| 形 | 条件 | `gate` | 对三条记录的实测 |
|---|---|---|---|
| ① | `!entry.itemCheck()` | `[]` | **不适用**（三点 `item_check` 全 = 1，见 §0.1 更正 1） |
| ② | `itemCheck ∧ reportItems.isEmpty()` | `payload = retiredChainGate(retiredRoutes)` | 三点 `reportItems = metadata.itemRequirements()` 均非空（`collect_item1` 解析 = `169400112×1 / 182215465×1 / 182215303×12`，与 `I` 记录 required **逐值相等**）⇒ 不走本形 |
| ③ | `itemCheck ∧ !reportItems.isEmpty()` | `reportItems` | **✅ 三点全走本形，且门值与 `I` 记录逐值相等** |

⇒ **结论：`canonicalChainHandIn` 的三形机制在门值上完全能承接这三条 `I` 记录**（语义等价、零丢门），但**签名与调用点都不承接**：

1. **无调用点**：`canonicalChainHandIn` 只在 `NPC_REPORT` 块循环（`:713-749`）里被调用。三条记录**都没有 `NPC_REPORT` 块** ⇒ 走 canonical 交付必须先为"I 记录"新增一个交付段调用点。
2. **`payload` 通道读不到 `I` 记录**：`retiredChainGate`（`:505-518`）只扫 `retiredRoutes` 的 `HAS_ITEM` 条件，而 `I` 记录是 `ItemReportRecord`，**不进 `retiredRoutes`**。⇒ 若走形②（`reportItems` 为空的行），门会静默变空。三点当前不触形②，但这是**结构性的**：任何"`item_check=1` ∧ 无 `collect_item`"的行（`precheck:222-234` 允许的 P0c-10o 形）合成到链面时会踩中。
3. **触发轴不同构**：`canonicalDelivery`（`RetailSimpleCollectItemDefinitionCompiler:473-479`）的事件是 `QUEST_SELECT(31)`，而 `itemReportGate` 是 `CHECK_USER_HAS_QUEST_ITEM(39)` / `_SIMPLE(20002)`。⇒ 迁移同时要**退掉通报页下发行**（`R4`/`R10`），否则客户端不会发出 31，canonical 边成死路（G1 已接受的同一取舍）。

### 2.4 处置提案

**提案：本片对三条 `I` 记录行维持"整类保留"（不迁移），理由是"可翻但收益小、风险不对称"。** 依据：

- **"不迁移"的最小改动面 = 0 行代码**。三条行当前已在生产（`retail-xml-retention.tsv` owner=`RETAIL_TABLE`，basis 分别为 `ITEM_CHECK_GATE_CHANNEL` / `PROGRESS_ROW_PROJECTION`），`P0c-34`/`P0c-35` 的契约审计结论是"三行贡献 **0** 条致命指纹"（24202/80320 只留非致命 `CLIENT_PAGE_UNREACHED`）。**保留 = 现状即正确**，只需在 γ 规格里登记为**例外面**（3 行逐条列名）并把 82 行的过滤面按"排除这 3 行"收窄。
- 若**必须迁移**（为消灭 family 内最后一处 legacy 交付形），最小改动面 = **4 处**：①新增 `I`-record 交付调用点（1 个循环 + 1 个 helper 重载）；②`retiredChainGate` 增 `I` 记录 payload 入参（否则形②空门）；③退掉 `R4`/`R10` 的页下发行（走现有 `chainPushedPages` 判据即可，三条的 after 全为 `DIALOG:` ⇒ 现有载荷守卫放行）；④`reportNpcs` 需要从 `itemReports()` 的 npc 并入（现只来自 `NPC_REPORT` 块 ∪ `reward_npc_name`）。**新增 guard 必须含**：`assertRetiredChainRouteQuiet` 目前**不检 `I` 记录**，需补"`I` 记录的门 ⊆ canonical 门"的逐条断言（现只有 `canonicalChainHandIn` 内部的 `payload ⊆ gate`，且 payload 只含 `HAS_ITEM`）。

**风险（按严重度）**：

1. **🔴 `24202` 是唯一"静默不可交付"陷阱**：若把 `canonicalSegments` 放宽到覆盖 `I` 行但**忘记**给 `I` 记录补 canonical 交付边，则 `R10` 被 `chainPushedPages={SELECT5} ⊂ CHAIN_RETIRED_PAGES` 退场、且其 conditions/actions 全为 `-` ⇒ **载荷守卫放行**；`I` 门边存活但无页承载。此时 `assertCanonicalChainResidueFree`（`:566-586`）**不报错**（它的 39/20002 残留判据只在 `"started"` 源上生效，而 `I` 门的 source 是 `s2`）⇒ **无红灯、任务不可交付**。
   - 对照：`1152` / `80320` 的 `failure_page='-'` ⇒ 失败边下发 `SELECT6` ⇒ **零残留守卫会抛**（`canonical chain segment still pushes a retired page`）⇒ 这两条是**响亮失败**，不构成静默风险。**24202 的 `failure_page='CLOSE'` 恰好绕开了唯一的守卫**。
2. **🟡 reward 态重开窗载体**：`1152`（`B NPC_COMPLETE 203130`）与 `24202`（`B NPC_COMPLETE 205150`）有预览边兜底；**`80320` 零 B 块** ⇒ 迁移后它的 reward 态重开窗只剩 `R12/R13`（`USE_OBJECT` + `SELECT_QUEST_REWARD reward→reward → window1`），其中 `R13` 属 `CHAIN_DELIVERY_RETIRED` 词表 ⇒ **会被退场**，需显式保留 `R13` 或补 reward 态重开边（同 §9 G-3）。
3. **🟡 门宿主与付款人不一致**：`1152` 的 `I` npc = `203130`（= `NPC_COMPLETE` 的 npc，**非**接取 NPC `203132`），`24202`/`80320` 的 `I` npc = 交付 NPC。⇒ 新增调用点时不能复用 `reportNpc` 解析（`reward_npc_name` 解析值），必须直接取 `ItemReportRecord.npcId()`。
4. **🟢 客户端的 39/20002 按钮归属**已由 `exits` 独立背书（三条分别 `SELECT5_CHECK` / `SELECT5_CHECK_SIMPLE` / `SELECT5_CHECK`），迁移后 `exits` 需同步退役（否则 `reportFlowChain` 的 `checkButton` 分支仍会为它们生成死边——但三条都不进 `reportFlowChain`，无实际影响）。

---

## 3. 特例 2：`2953`（重复接取：`complete→complete START_ELIGIBLE` + `SELECT1_1`）

### 3.1 原始记录（全量）

```
2953	P	0	6	0	63	PERSISTENT	LOCAL
2953	N	unaccepted	NONE	0
2953	N	started	START	0
2953	N	reward	REWARD	1
2953	N	complete	COMPLETE	0
2953	B	NPC_START	204191	unaccepted	started	unaccepted started|SELECT1|GIVE_ITEM:182207039:1
2953	B	NPC_COMPLETE	204191	reward	complete	cri=0|fixed=0 1|actions=SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD|finish=SELECTION_DIALOG|preview=USE_OBJECT SELECT_QUEST_REWARD|choice=-|fallback=-
2953	R	1	204071	QUEST_SELECT	started	started	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT2
2953	R	2	204071	SELECT2_1	started	started	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT2_1
2953	R	3	204071	SETPRO1	started	reward	HAS_ITEM:182207039:1	REMOVE_ITEM:182207039:1	SYNC:LEVEL_AND_VISIBILITY_REFRESH;CLOSE
2953	R	4	204191	QUEST_SELECT	reward	reward	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT5
2953	R	5	204191	SELECT1_1	complete	complete	START_ELIGIBLE	-	DIALOG:SHOW_QUEST_PAGE:SELECT1_1
2953	R	6	204191	SELECT1_1	unaccepted	unaccepted	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT1_1
```
`quest.xml`：`max_repeat_count = 100`；`give_item = ITEM_QUEST_2953A 1`；`quest_work_item1 = quest_2953a`；`item_check` 无。
`exits`：`2953  SELECT1_1 SELECT2_CONTINUE`。

### 3.2 机制核实（生产代码逐行）

`restoreRepeatStartDialogContract`（`QuestDefinitionCompiler.java:278-335`，由 `:31` 在 `compile()` 入口无条件调用）对 `2953` **成立**，四步判据逐条过：

| 步 | 代码 | 对 2953 的实测 |
|---|---|---|
| 重入门 | `:279` `metadata().repeatPolicy().maxRepeatCount() <= 1 → return` | `max_repeat_count=100` ⇒ **过** |
| 完成节点 | `:286-291` 收集 `status=COMPLETE` 节点，空则 return | `complete` 存在 ⇒ **过** |
| `startNpcs` | `:292-302` 取"`source` 投影 NONE ∧ `target` 投影 START ∧ 含 `StartEligible` ∧ `TalkToNpc.dialogId ∈ {1002, 20000}`"的 NPC | `canonicalAcceptFlow` 的 `QUEST_ACCEPT_1(1002)`/`QUEST_ACCEPT_SIMPLE(20000)` 两条边 `unaccepted→started` + `StartEligible` ⇒ `startNpcs = {204191}` ⇒ **过** |
| 复制条件 | `:307-329` 对 `source` 与 `target` **双 NONE** ∧ `TalkToNpc` ∧ `npc ∈ startNpcs` ∧ 无 actions ∧ `isDialogOnlyResponse(after)` 的边，复制到每个 complete 节点并补 `StartEligible`（`:319` `hasDialogRoute` 去重） | `canonicalAcceptFlow` 的 `31`（`ShowQuestDialog(4)`）、`1003/1004/20001`（`ShowQuestDialog`/`CloseDialog`）、`1008`（`ShowQuestSelectionDialog`）**全部合格** ⇒ 复制到 `complete` |

**跨状态接取提交**由运行时承接，不需要复制：`QuestMutationPlanner.matchesSourceStatus`（`:402-416`）在 `:413-415` 显式允许
`snapshot.status() == COMPLETE ∧ source.projection().status() == NONE ∧ 含 StartEligible` 的转换跨越。这正是
`QuestRepeatLifecycleTest.repeatable1963ReopensItsStartPageThenAcceptsFromCompletedState`（`:41-77`）所锁的形：
`complete` 态上 `(npc, 31)` 只开接取窗（`nextStatus` 仍 `COMPLETE`，`:51-55`），`(npc, 1002)` 从 `complete` 快照出发 `nextStatus → START`（`:72-76`）。

**⇒ 对 `2953` 与对 `1963` 完全同构**：机制**不区分 G1/G2**（它只看 `maxRepeatCount` 与定义图的 NONE→START 接取边），因此 **`2953` 的接取段换 `canonicalAcceptFlow` 后重复开局机制同样成立**。

### 3.3 差异（必须登记的一处）

`canonicalAcceptFlow` 的 `31` 边 after = `ShowQuestDialog(SHOW_ASK_QUEST_ACCEPT_WINDOW)`（页 4）；
`acceptFlowChain` 的 `31` 边 after = `ShowQuestDialog(SELECT1)`（页 1011，`startPage`）。
⇒ 复制到 `complete` 的重开页从 **select1（1011）** 变为 **ask 窗口（4）**。这与 `1963` 的 S2 期望**逐字一致**（该测试断言的正是页 4），属**预期改进**而非回归。

另一处：`R5 SELECT1_1 complete→complete START_ELIGIBLE` 在 canonical 下**会被退场**（`acceptNpcs={204191}` ∧ `SELECT1_1 ∈ CHAIN_ACCEPT_RETIRED`），且 **能安静退场**（`assertRetiredChainRouteQuiet` 的 conditions 白名单含 `START_ELIGIBLE`，`:469`）。退场后 `complete` 态仍有 `31/1003/1004/20001/1008` 的别名边 ⇒ 无丢页。⇒ 登记表里的 `R5` 本就是"重复别名"的手写替身（`:319` `hasDialogRoute` 命中它时会跳过自动复制），canonical 后自动复制顶上。

### 3.4 处置提案

**`2953` 接取段可翻（照抄 P1 的 `canonicalAcceptFlow`），不需要为重复接取做任何特殊处理**；但**必须新增一条回归断言**：`(2953, complete, 204191, 31)` 的 after 必须是页 4 且 conditions 恰为 `[StartEligible]`（模板取 `QuestRepeatLifecycleTest:49-55`）。
**风险**：`R5` 与自动别名边**同键不同源**（`(complete,204191,1012)`），canonical 后 `R5` 退场 + `R6` 退场 ⇒ `(complete,204191,1012)` 由自动复制提供（`R6` 是 NONE→NONE 的合格源）⇒ **不可丢**；若 S3 的过滤面误把 `unaccepted` 侧的 `R6` 排除在"复制源"之外（例如通过提前 `removeIf`），`complete` 态将失去 select1_1 续页——**这是本行唯一的隐性依赖**，需在链门加"每个 complete 节点至少有一条 NONE→NONE 复制边"的存在性断言。

---

## 4. 特例 3：`28809`（`selectionSources = "unaccepted s0 s1 s2"`，接取目标 `s0`）

### 4.1 原始记录（全量）

```
28809	P	0	6	0	63	PERSISTENT	LOCAL
28809	N	unaccepted	NONE	0
28809	N	s0	START	0
28809	N	s1	START	1
28809	N	s2	START	2
28809	N	reward	REWARD	3
28809	N	complete	COMPLETE	0
28809	B	NPC_START	830169	unaccepted	s0	unaccepted s0 s1 s2|SELECT1|GIVE_ITEM:190100013:1
28809	B	NPC_COMPLETE	830169	reward	complete	cri=0|fixed=0|actions=SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD|finish=SELECTION_DIALOG|preview=USE_OBJECT SELECT_QUEST_REWARD|choice=-|fallback=-
28809	R	1	830408	QUEST_SELECT	s0	s0	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT2	RETAIL_MATCH	SELECT2=CLIENT
28809	R	2	830408	SETPRO1	s0	s1	-	SET_VAR:var0=1	SYNC:PACKET_ONLY;CLOSE
28809	R	3	830417	QUEST_SELECT	s1	s1	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT3	RETAIL_MATCH	SELECT3=CLIENT
28809	R	4	830417	SETPRO2	s1	s2	-	SET_VAR:var0=2	SYNC:PACKET_ONLY;CLOSE
28809	R	5	830169	QUEST_SELECT	s2	s2	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT5	RETAIL_MATCH	SELECT5=CLIENT
28809	R	6	830169	SELECT_QUEST_REWARD	s2	reward	-	SET_VAR:var0=3	SYNC:LEVEL_AND_VISIBILITY_REFRESH;CLOSE
28809	R	7	830169	QUEST_SELECT	reward	reward	-	-	DIALOG:SHOW_QUEST_PAGE:SHOW_SELECT_QUEST_REWARD_WINDOW1
```
`exits`：**无 `28809` 行**（这就是为什么 `s0 s1 s2` 只能来自块 extra 第一段）。
已删 XML（`git show 818889c7c:.../quests/28809.xml:37`）逐字：`<dialog type="NPC_START" npc-id="830169" source="unaccepted" target="s0" selection-sources="unaccepted s0 s1 s2" start-page="SELECT1">`
⇒ **`selectionSources` 是 XML 作者声明，不是客户端页驱动**。`acceptFlowChain:1050-1057` 据此为 `{unaccepted, target} ∪ selectionSources` 各登记一条
`talk(830169, FINISH_DIALOG, src, src, after = ShowQuestSelectionDialog(SELECT_QUEST))`（把 `src` 取遍 `unaccepted/s0/s1/s2`）。

### 4.2 这些出口服务哪些页/按钮（客户端证据）

`docs/quest/client-dialog-mapping/quest-dialog-action-details.csv` 的 `28809` 全量按钮：

| 页(page_id) | 客户端可见动作 |
|---|---|
| `select1`(1011) | `HACTION_QUEST_ACCEPT_SIMPLE`(20000)、`HACTION_QUEST_REFUSE_SIMPLE`(20001) |
| `select2`(1352) | `HACTION_SETPRO1`(10000) |
| `select3`(1693) | `HACTION_SETPRO2`(10001) |
| `select5`(2375) | `HACTION_SELECT_QUEST_REWARD`(1009) |
| `select_quest_reward1`(5) / `quest_summary`(9) / `quest_complete`(1008) / `select_acqusitive_quest_desc`(12) / `select_progressive_quest_desc`(11) | 0 动作 |

**⇒ `28809` 的客户端 HTML 没有任何页承载 `HACTION_FINISH_DIALOG`(1008)**。因此：

- `s1` 节点上服务端下发的 `SELECT3` 页（`R3`）唯一按钮是 `SETPRO2`（由 `R4` 承接 ✅）；`s2` 节点下发的 `SELECT5` 页（`R5`）唯一按钮是 `1009`（由 `R6` 承接 ✅）。
- `s1`/`s2` 上那两条 `FINISH_DIALOG` 出口**没有任何客户端按钮载体** ⇒ 丢它们**不产生 `BUTTON_WITHOUT_ROUTE`**。
- 反向也成立：`unaccepted`/`s0` 上的同类出口同样无载体（`select1` 上是 20000/20001，`select2` 上是 10000），canonical 保留它们只是形状一致，不是功能性必需。

**审计口径补充（重要）**：客户端契约门 `QuestClientContractGateTest`（`src/test/java/.../definition/QuestClientContractGateTest.java:56-70`）**显式把 `RetiredQuestIds`（= 全部 `RETAIL_TABLE` 行）从审计行里剔除**，把生命周期合同让给黑盒门；页面-按钮审计 `QuestE2eBatchAudit.auditPageButtons` 只被 `QuestPageButtonAuditTest` 用于**临时 XML**。⇒ **零售行不存在"死按钮"的自动门禁**，只能靠"客户端页表 + 服务端路由"的人工逐行对拍（S2 §2.4/§6 用的正是这个方法）。本节的结论就是按该口径给出的。

### 4.3 阻断点

`assertCanonicalSelectionSources`（`:548-559`）**会直接抛**：`28809` 的 extra 第一段含 `s1`/`s2`，两者都 ∉ `{unaccepted, start.target()=s0}` ⇒
`canonical accept finish sources must stay within the segment: quest 28809 source s1 target s0`。
⇒ **不是静默丢，是 fail-closed 拒绝编译**（S2 §2.1 第 3 条的守卫按预期工作）。这正是它被移交给 S3 的原因。

### 4.4 处置提案（三选一，附推荐）

| 方案 | 改动 | 代价 | 风险 |
|---|---|---|---|
| **A 保留原记录（= 不进 canonical 接取）** | 0 | `28809` 的接取段永远留在 legacy 词表外，γ 的"接取段全覆盖"目标留一个洞 | 🟢 零新风险 |
| **B 扩 canonical 关窗源**（**推荐**） | 在 `canonicalSegments` 的接取分支后，**逐字复用 `acceptFlowChain:1050-1057` 的形状**补 `selectionSources \ {unaccepted, target}` 的 `FINISH_DIALOG` 出口（1 个 helper 内联，无需改共享 helper）；同时把 `assertCanonicalSelectionSources` 的语义从"越界即抛"改为"越界 ⇒ 必须由补充出口覆盖" | ~10 行，零新函数 | 🟢 2 条 inert 路由（`s1/s2` 上），客户端不可见；与 G1 行的"中段记录逐字保留"哲学一致（`s1/s2` 的 `R3/R5` 本就保留） |
| **C 宣告退场** | 把 `assertCanonicalSelectionSources` 的越界改为放行，并在 S3 记录里留客户端页表证据（§4.2 表格） | 改动最小 | 🟡 依赖"编译器无法自证客户端页无 1008"的人工证据；且与 `s0` 出口形状不一致（同为无载体，一个留一个丢） |

**推荐 B**：它把"块 extra 声明的关窗源"当作**登记表权威**（与 `start-page`/`selection-sources` 的语义一致），避免在编译器里引入"客户端页知识"这个它本来没有的依赖；A 次之（代价是行数目标不闭合）；C 不推荐（同一形状在同一行内一半留一半丢，未来做等价性对拍时无法解释）。

**`28809` 的交付段**（与本节并列）：`R5`+`R6` → 一条 `canonicalDelivery(830169, [], [], window1, "s2")`；`R7`（`QUEST_SELECT reward→reward → window1`）**保留**（`QUEST_SELECT` ∉ 交付词表，after 页 5 ∉ 退场页）⇒ reward 态重开窗有载体（且 `NPC_COMPLETE` 也在）。⇒ **本行接取 + 交付两段都可翻**，唯一需要裁定的是 §4.4 的出口三选一。

---

## 5. 特例 4：`3001` / `3023`（`VAR_IS:var0=k` 阶段门交付）

### 5.1 原始记录（全量）

`3001`（29 行；**零 B 块**，`N: started v1 v2 v3`）：
```
3001	R	6	798133	QUEST_SELECT	started	started	VAR_IS:var0=0	-	DIALOG:SHOW_QUEST_PAGE:SELECT2
3001	R	7	798133	SETPRO1	started	v1	-	-	SYNC:PACKET_ONLY;CLOSE
3001	R	8	798136	QUEST_SELECT	v1	v1	VAR_IS:var0=1	-	DIALOG:SHOW_QUEST_PAGE:SELECT3
3001	R	9	798136	SETPRO2	v1	v2	-	-	SYNC:PACKET_ONLY;CLOSE
3001	R	10	798139	QUEST_SELECT	v2	v2	VAR_IS:var0=2	-	DIALOG:SHOW_QUEST_PAGE:SELECT4
3001	R	11	798139	SETPRO3	v2	v3	-	-	SYNC:PACKET_ONLY;CLOSE
3001	R	12	798132	QUEST_SELECT	v3	v3	VAR_IS:var0=3	-	DIALOG:SHOW_QUEST_PAGE:SELECT5      ← 交付页门
3001	R	13	798132	SELECT_QUEST_REWARD	v3	reward	VAR_IS:var0=3	-	SYNC:LEVEL_AND_VISIBILITY_REFRESH;DIALOG:SHOW_SELECTION_PAGE:SELECT_QUEST
3001	R	14	798132	USE_OBJECT	reward	reward	-	-	DIALOG:SHOW_QUEST_PAGE:SHOW_SELECT_QUEST_REWARD_WINDOW1
3001	R	15	798132	SELECT_QUEST_REWARD	reward	reward	-	-	DIALOG:SHOW_QUEST_PAGE:SHOW_SELECT_QUEST_REWARD_WINDOW1
3001	R	16	798132	SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD	reward	complete	-	GRANT:GOLD:0:320:EXACT;GRANT:EXP:0:50008:EXACT;GRANT:ITEM:188051196:1:EXACT;GRANT:ITEM:186000002:1:EXACT;COMPLETE_QUEST:0	REFRESH_PLAYER_STATS;SYNC:COMPLETION;DIALOG:SHOW_SELECTION_PAGE:SELECT_QUEST
```
`3023` 同形（`N: started v1 v2 v3`，`R12/R13` 逐字同形，`R14/R15/R16` 同形；另有 `R17/R18 SELECT3_1/SELECT3_1_1 unaccepted` 与 `R21-R24` 页梯）。
`quest.xml`：`3001 max_repeat_count=1`，无 `collect_item` ⇒ `itemRequirements=[]`；`3023` 有 `quest_work_item1/2`（`quest_3023a`/`quest_3023b`）但无 `collect_item`。`exits`：两行都只有 `SELECT2_CONTINUE`（**无 `SELECT5_CHECK`**）⇒ 客户端 `select5` 按钮 = `1009`（已由 client action details 证实）。

### 5.2 `canonicalDelivery` 能否以 `QuestVariableIs` 作为条件承接

| 维度 | 判定 | 证据 |
|---|---|---|
| **签名** | **✅ 能** | `canonicalDelivery(int rewardNpc, List<QuestCondition> hasItems, List<QuestAction> removeItems, int rewardWindowPage, String collectSource)`（`RetailSimpleCollectItemDefinitionCompiler:473-474`）——第一参数类型是 `List<QuestCondition>`（**形参名 `hasItems` 不构成类型约束**），而 `VAR_IS:var0=3` 解码为 `QuestCondition.QuestVariableIs("var0", 3)`（`:952-954`），可直传。`buildChain:733-736` 的 `handIn → HasItem/RemoveItem` 映射只是**调用方的构造方式**，不是 helper 的限制 |
| **语义** | **✅ 能，但冗余** | 条件列表就是转换的 `conditions`，多条件为合取；`QuestVariableIs` 是标准 `QuestCondition`。**更强的一点**：`QuestMutationPlanner.matchesSourceNode`（`:340-351`）在 `:348-350` 要求"快照解包变量 == **源节点投影**"⇒ 把边锚在 `v3`（`N v3 START 3` ⇒ 投影 `var0=3`）时，`VAR_IS:var0=3` **已被锚点节点投影完全蕴含**，无需在边上再写条件 |
| **判例** | **✅ 同形已落地** | `Quest1913ProductionFlowTest.rewardDialogIsAvailableOnlyAfterTheVerteronTransfer`（`:100-140`）："传送前（`started`，var0=0）：交付边锚在 `started1`，`QUEST_SELECT(31)` **零路由**；传送后（`started1`，var0=1）：点 31 即翻 REWARD 并下发本档奖励窗"。⇒ 家族的规范手法是**节点锚定**（`started1`），不是变量条件 |

### 5.3 翻形后的边形状草案（含 after 序）

```
# 3001 / 3023 交付段（替换 R12 + R13 两条）
QuestTransition(
    event      = TalkToNpc(798132 /* 3001 */ | 798138 /* 3023 */, QuestDialogAction.QUEST_SELECT.id()=31),
    conditions = List.of(),                       // ← VAR_IS:var0=3 由锚点节点 v3 的投影蕴含（:348-350）
    actions    = List.of(),                       // ← 原 R12/R13 无动作，门由 metadata.itemRequirements() 决定（本两行为空）
    targetNode = "reward",
    after      = [ SyncQuestState(LEVEL_AND_VISIBILITY_REFRESH),      // 序 1
                   ShowQuestDialog(SHOW_SELECT_QUEST_REWARD_WINDOW1) ],// 序 2（档位窗；两行奖励组 = 1 ⇒ 窗 1）
    priority   = null,
    sourceNode = "v3" )
```
`after` 两形的差分：**旧形** `[SYNC:LEVEL_AND_VISIBILITY_REFRESH, DIALOG:SHOW_SELECTION_PAGE:SELECT_QUEST]`（先回任务列表页二次点击）
→ **新形** `[Sync(LEVEL_AND_VISIBILITY_REFRESH), ShowQuestDialog(窗1)]`（一步到位）。
这**正是 S2 已在 203 行 G1 上接受的同一取舍**（`2026-09-27-s2-buildchain-spec` §5.2/§7），本片只是把承载者从 `NPC_REPORT` 块换成 R 记录合成的边。

配套三条（缺一即错）：
1. **`R12` 必须退场**（否则显式路由覆盖同键 `(v3, 798132, 31)` ⇒ 新边静默消失）。判据已现成：`chainPushedPages(R12.after) = {SELECT5} ⊂ CHAIN_RETIRED_PAGES`（`:441-443`）。
2. **载荷守卫必须扩**：`assertRetiredChainRouteQuiet`（`:466-474`）的 conditions 白名单是 `{空, -, HAS_ITEM:, START_ELIGIBLE}` ⇒ 命中 `VAR_IS:var0=3` **直接抛**（`chain canonical segment retires a gated route`）。**这就是 §7-4 台账里那条"2 条 VAR_IS 交付门"的具体阻断形态：不是静默丢，是响亮拒绝**。
3. **`R15`（`SELECT_QUEST_REWARD reward→reward → window1`）会被交付词表退场**，而两行**都没有 `NPC_COMPLETE`** ⇒ 退场后 reward 态只剩 `R14`（`USE_OBJECT reward→reward → window1`）。若认定 `USE_OBJECT` 不是该行客户端在 reward 态的可见动作，则必须显式保留 `R15` 或补一条 reward 态重开边（`reportNpcExit:1350-1356` 是现成的同族形状，但它是 `started` 源，**不适用**本例，需新写）。

### 5.4 处置提案

**可翻；推荐"节点锚定 + 载荷蕴含断言"，不引入 `VAR_IS` 载荷通道。** 具体：

- **不**把 `VAR_IS` 条件搬到 canonical 边（避免"条件与锚点双写"的冗余），而是**在守卫里加一条蕴含断言**：`retiredChainRoute` 命中且 conditions 含 `VAR_IS:<var>=<k>` 时，要求
  `nodeByLabel.get(report.source()).projection().variables().get(var) == k`，否则抛
  （`chain delivery anchor does not subsume the retired var gate`）。这是**可静态求值**的 fail-closed 断言（`QuestNode`/`NodeProjection` 在 `buildChain` 形参里就有），既保证零丢门，又不改动 `canonicalDelivery` 的签名。
- 若某行的 `VAR_IS` 值**不被锚点蕴含**（本片实测 0 行），再用备用方案：把 `canonicalDelivery` 的第一参当作**通用条件通道**直接传 `QuestVariableIs`（签名已支持，`§5.2` 已证）。
- **`R15` 的 reward 态载体**需 lane owner 裁定：建议**保留 `R15`**（把交付词表的退场作用域限制为 `started`-源，与 `assertCanonicalChainResidueFree:579` 的现有口径一致——该守卫本来就只查 `"started"` 源），这样两行的 reward 态握手与 G1 行（有 `NPC_COMPLETE`）等价。

**风险**：🟡 若照抄 G1 的"整词表退场"而不看 `NPC_COMPLETE` 存在性，`3001/3023/80320/2611` 等 11 行会**静默失去 reward 态重开窗**（无守卫覆盖：`assertCanonicalChainResidueFree` 只查 `started` 源）。建议 S3 增一条不变量：**`NPC_COMPLETE` 块缺失的行，reward 节点必须至少有一条非退场记录作为重开载体**。

---

## 6. 特例 5：`21136`（`GIVE_ITEM` 载荷）+ `2611` / `35017`（G3：有 REPORT 无 START）

### 6.1 `21136`（`P3`：只有 `NPC_COMPLETE`，无 START/REPORT）

```
21136	B	NPC_COMPLETE	730355	reward	complete	cri=0|fixed=0 1 2|actions=SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD|finish=SELECTION_DIALOG|preview=SELECT_QUEST_REWARD|choice=-|fallback=-
21136	R	1	799271	QUEST_SELECT	unaccepted	unaccepted	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT1
21136	R	2	799271	ASK_QUEST_ACCEPT	unaccepted	unaccepted	-	-	DIALOG:SHOW_QUEST_PAGE:SHOW_ASK_QUEST_ACCEPT_WINDOW
21136	R	3	799271	QUEST_ACCEPT_1	unaccepted	started	START_ELIGIBLE	GIVE_ITEM:182207919:1	SYNC:VISIBILITY_REFRESH;DIALOG:SHOW_QUEST_PAGE:QUEST_ACCEPT_1
21136	R	5	799271	QUEST_REFUSE_2	unaccepted	unaccepted	-	-	CLOSE
21136	R	8	799413	SETPRO1	started	s1	VAR_IS:var0=0	REMOVE_ITEM:182207919:1;SET_VAR:var0=1	SYNC:PACKET_ONLY;CLOSE
21136	R	10	799414	SETPRO2	s1	reward	VAR_IS:var0=1	SET_VAR:var0=2	SYNC:LEVEL_AND_VISIBILITY_REFRESH;CLOSE
21136	R	11	730355	USE_OBJECT	reward	reward	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT5
```
`Quest_SimpleTalk.xml`：`give_item = ITEM_QUEST_21136A 1`（⇒ `entry.giveItemSymbol()` 非空，`entry.itemCheck()` 无）。`exits`：`SELECT2_CONTINUE`。

**载荷判定**：`R3` 的 `GIVE_ITEM:182207919:1` 是**接取发物**（与 `quest.xml quest_work_item1=quest_21136a` 同轴），不是交付门；
`R8` 在下一步 `REMOVE_ITEM` 同一物品（`granted 1 − removed 1 = 0`）⇒ `carriedWorkItems` 为空，与 `P0c-10m`/`1131` 判例同形。
**承载通道现成**：`canonicalAcceptFlow(npc, target, acceptActions)` 第三参（`RetailSimpleHuntDefinitionCompiler:1096-1105`）会把 `acceptActions`
挂到 `1002/20000` 两条提交边上；`buildChain:637-640` 也已从块 extra 第三段解析 `startActions`（wave B 通道）。
**阻断点（唯一）**：`assertRetiredChainRouteQuiet`（`:475-481`）的 actions 白名单只有 `{空, -, REMOVE_ITEM:}` ⇒ 退 `R3` 时**抛** `chain canonical segment retires an acted route: quest 21136 action GIVE_ITEM:182207919:1`。
**处置提案**：**接取段可翻**（把 `GIVE_ITEM` 载荷放进 `acceptActions`，并在守卫 actions 白名单里为接取段增加 `GIVE_ITEM:`，同时断言"`GIVE_ITEM` 载荷 ⊆ `acceptActions`"——与 `HAS_ITEM ⊆ gate` 同构的对称断言）。
**交付段不可翻**：`21136` 的交付是**阶段链驱动的 `SETPRO2 s1→reward`**（服务端 `SET_VAR:var0=1` 门 + `SYNC:LEVEL_AND_VISIBILITY_REFRESH`），没有任何 `31/1009` 交付边、没有 `NPC_REPORT` 块 ⇒ **无 canonical 对应形**（`canonicalDelivery` 只能表达"从某源节点一键翻 REWARD"，表达不了"先 `SET_VAR` 再翻"的组合）。
**风险**：🟡 部分翻形会造成"同一行接取段 canonical、交付段 legacy"的**混合形态**，家族指纹与 `S3` 的"逐字未动"判据都要改成"分流"。若不做接取段翻，则 `21136` 保持逐字（推荐：与 §2 同理，**收益小、混合形态成本高**）。

### 6.2 `2611`（`P2`：4 个 `NPC_REPORT` 块，**无 START**）

```
2611	B	NPC_REPORT	204763	started	reward	SELECT5
2611	B	NPC_REPORT	204763	step1	reward	SELECT5
2611	B	NPC_REPORT	204763	step2	reward	SELECT5
2611	B	NPC_REPORT	204763	step3	reward	SELECT5
2611	R	1	204763	QUEST_SELECT	unaccepted	unaccepted	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT1
2611	R	2	204763	ASK_QUEST_ACCEPT	unaccepted	unaccepted	-	-	DIALOG:SHOW_QUEST_PAGE:SHOW_ASK_QUEST_ACCEPT_WINDOW
2611	R	3	204763	QUEST_ACCEPT_1	unaccepted	started	START_ELIGIBLE	-	SYNC:VISIBILITY_REFRESH;DIALOG:SHOW_QUEST_PAGE:QUEST_ACCEPT_1
2611	R	4	204763	QUEST_ACCEPT_SIMPLE	unaccepted	started	START_ELIGIBLE	-	SYNC:VISIBILITY_REFRESH;CLOSE
2611	R	5	204763	QUEST_REFUSE_1	unaccepted	unaccepted	-	-	DIALOG:SHOW_QUEST_PAGE:QUEST_REFUSE_1
2611	R	6	204763	QUEST_REFUSE_2	unaccepted	unaccepted	-	-	CLOSE
2611	R	7	204763	QUEST_REFUSE_SIMPLE	unaccepted	unaccepted	-	-	CLOSE
2611	R	8	204783	QUEST_SELECT	started	started	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT2
2611	R	10	204784	QUEST_SELECT	step1	step1	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT3
2611	R	12	204700	QUEST_SELECT	step2	step2	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT4
2611	R	14	204763	USE_OBJECT	reward	reward	-	-	DIALOG:SHOW_QUEST_PAGE:SHOW_SELECT_QUEST_REWARD_WINDOW1
2611	R	15	204763	SELECT_QUEST_REWARD	reward	reward	-	-	DIALOG:SHOW_QUEST_PAGE:SHOW_SELECT_QUEST_REWARD_WINDOW1
2611	R	16	204763	SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD	reward	complete	-	GRANT:EXP:0:5220034:QUEST_BASE;GRANT:ITEM:186000010:20:EXACT;COMPLETE_QUEST:0	REFRESH_PLAYER_STATS;SYNC:COMPLETION;DIALOG:SHOW_SELECTION_PAGE:SELECT_QUEST
2611	R	17	204763	SELECT1_1	unaccepted	unaccepted	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT1_1
2611	R	18	204763	FINISH_DIALOG	unaccepted	unaccepted	-	-	DIALOG:SHOW_SELECTION_PAGE:SELECT_QUEST
2611	R	19	204763	FINISH_DIALOG	started	started	-	-	DIALOG:SHOW_SELECTION_PAGE:SELECT_QUEST
```
`exits`：`2611 SELECT1_1 SELECT2_CONTINUE`（**无 `SELECT5_CHECK`**）⇒ `reportFlowChain:1070-1072` 的 `checkButton=false` ⇒ 现形**本来就没有物品门**（`requiresItems=false`）。
**判定**：**交付段可翻，且门中性**——`canonicalChainHandIn` 走形①/②（`entry.itemCheck()=false` ⇒ `gate=[]`；`retiredRoutes` 无 `HAS_ITEM` ⇒ `payload=[]`）⇒ 与现形 `conditions=[]` **逐字等价**，**零门丢失**。
**接取段可翻**（6 个非系统无 START 行之一，见 §8）。四条 `NPC_REPORT` 块 → 四条 `canonicalDelivery` 边（源 `started/step1/step2/step3`，逐块一条），与 G1 的"多变体块各出一段"同口径（`:758` `NPC_COMPLETE` 十块的先例）。
**阻断点**：无（`assertRetiredChainRouteQuiet` 对退场的 `R1-R7/R17` 载荷全空 ⇒ 放行；`assertCanonicalSelectionSources` 无 START 块 ⇒ 不触发）。
**风险**：🟡 `R15`（reward 态 1009）会被交付词表退场，而 `2611` **无 `NPC_COMPLETE`**（在 §1 的 11 行清单里）⇒ 与 §5.3 第 3 点同源风险；`R14`（`USE_OBJECT`）是唯一残留载体。

### 6.3 `35017`（`P4` 系统发放 + `NPC_REPORT`）

```
35017	B	NPC_REPORT	799806	s1	reward	SELECT5
35017	B	NPC_COMPLETE	799806	reward	complete	cri=0|fixed=0 1 2|actions=SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD|finish=SELECTION_DIALOG|preview=USE_OBJECT SELECT_QUEST_REWARD|choice=-|fallback=-
35017	R	1..9	799806	（全部 unaccepted 源：SELECT2_1/ASK/ACCEPT_1/ACCEPT_SIMPLE/SETPRO1/REFUSE_1/2/SIMPLE/FINISH_DIALOG）
35017	R	10	798155	QUEST_SELECT	started	started	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT2
35017	R	11	798155	SETPRO1	started	s1	-	SET_VAR:var0=1	SYNC:PACKET_ONLY;CLOSE
35017	R	12	798155	SELECT2_1	started	started	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT2_1
```
`acquired = _faction_` ⇒ `systemGrant=true`。**实测**：`buildChain:750-757` 的 `removeIf` 把 `R1-R9`（`unaccepted` 源的 `TalkToNpc`/`QuestDialog`）**全部删除**（R2/R9 也在内）。⇒ 编译时该行只剩 `SystemGrant` 边 + `R10/R11/R12` + 块边。
**客户端背书（关键）**：`35017` 的客户端 `ask_quest_accept`(4) 页**唯一动作是 `HACTION_FINISH_DIALOG`**（无 20000/20001/1002/1003）⇒ **客户端根本不提供"接受/拒绝"按钮** ⇒ 系统发放（无 NPC 接取）是**客户端事实**，不是服务端便利。
**处置提案**：**交付段可翻**（`B NPC_REPORT 799806 s1→reward` → `canonicalDelivery(799806, [], [], window1, "s1")`，门中性同 §6.2）；**接取段不翻**（无接取段）。⇒ `35017` 是唯一一个"系统发放 + 可翻交付段"的行，属 §7 的例外，需单独登记。
**风险**：🟢 低。`R12`（`SELECT2_1 started→started → SELECT2_1`）不在任何退场词表 ⇒ 保留；`R10`（`→ SELECT2`）保留。

---

## 7. 特例 6：系统发放行（14 行，**全为 `_faction_`**）

清单：`35010 35011 35017 35018 35024 35025 35026 45010 45011 45017 45018 45024 45025 45026`。
判据（`RetailGrantKind:54-69` + `RetailGrantKind.java:6-13` 注释）：`acquired_npc_name` 形如 `_<token>_` 且 `token ∈ {faction, challengetask, area}` → 对应类别；其余 `_..._` → `UNKNOWN_SENTINEL`；**`systemGrant() = (this != NPC)`**。
实测：14 行**全部 `_faction_`**（`NPC 接取` 面为 0；`_area_`/`_challengetask_`/未知哨兵在 γ 中**不存在**）。`grantable()` 对 `_faction_` 为 true ⇒ `precheck:258` 放行。

**canonical 化对它们意味着什么**（逐条）：

1. **接取段：恒不可翻，且不可翻性是"设计内含"**。`canonicalSegments`（`:616`）第一项就是 `!entry.grantKind().systemGrant()`；`buildChain:629-633` 用 `if (systemGrant)` 抢在 START 分支之前。⇒ 对 `35011/35026`（**带 `NPC_START` 块**）也**不会**展开接取段——不是被过滤器退场，而是从未合成。若强行合成 `canonicalAcceptFlow`，会与 `:750-757` 的 `removeIf`（删 `unaccepted` 源路由）**直接冲突**，且与客户端页表冲突（`35017` 的 ask 页只有 1008；其余 `_faction_` 行同族）。
2. **`unaccepted` 源记录已在编译期被删** ⇒ γ 的 `F2`（"接取段 R 记录键 ∈ 合成 canonical 块边 ⇒ 过滤"）对**这 14 行的接取部分无对象**（`sel1_R:` 全为 `-` 已证：14 行的 `QUEST_SELECT unaccepted → SELECT1` 记录均不存在或不匹配）。⇒ **把系统发放行纳入 γ 接取过滤面是空操作**。
3. **交付段**：仅 `35017` 有 `NPC_REPORT`（可翻，§6.3）；其余 13 行的交付是 **R 驱动的 `31/1009` 边**（实测 12 行有 `REP31:79xxxx@…` 或 `@v1/@started`，`35011/35026` 的源是 `v1`），其中 `1009` 非 reward 源→reward 的有 11 行（`35010 35011 35018 35024 35025 35026 45010 45011 45017 45018 45024 45025 45026` 里的 11 个）——**形态与 P1/P3 的行完全同构**，其可翻性**只取决于交付段合成，与系统发放无关**。
4. **`35024/45024/45026` 是双接取/双报告 NPC 行**（`Accept_R` 含两个 npc；`35024/35026` 的 `REP31` 也含两个 npc）⇒ 合成时必须按"多变体块各出一段"处理，不能假设单例。

**处置提案**：**S3 应把系统发放行显式排除在"接取段 canonical 化"之外（与 S2 §1 一致，14 行逐条列名）**，但**允许其交付段按 §5 的同一规则参与**（`35017` 走 `NPC_REPORT` 路径，其余 13 行走 R 合成路径）。**风险**：🟢 低——若整类排除，γ 的"交付段全覆盖"目标留 13 个洞；若整类纳入接取段，则与 `removeIf`/客户端页表双重冲突（应视为**不可行**，不是"有风险"）。

---

## 8. 特例 7：无 `NPC_START`/`NPC_REPORT` 块的行（17 行）与 canonical 接取合成可行性

### 8.1 清单核实（**任务书 §7 的前提需更正**）

| 档 | 行数 | ids |
|---|---|---|
| 无 START **且** 无 REPORT | **17** | `1323 3001 3023 21136 24202 35010 35018 35024 35025 45010 45011 45017 45018 45024 45025 45026 80320` |
| ↳ 其中**有 `NPC_COMPLETE`**（有 reward 态载体） | **10** | `1323 21136 24202 35010 35018 35024 45011 45018 45025 45026` |
| ↳ 其中**无任何 B 块** | **7** | `3001 3023 35025 45010 45017 45024 80320` |
| ↳ ↳ 非系统（可合成接取段） | **3** | `3001 3023 80320` |
| ↳ ↳ 系统发放（§7 排除） | **4** | `35025 45010 45017 45024` |
| （另：有 REPORT 无 START = `2611` `35017`，属 §6） | 2 | — |

### 8.2 "接取段由记录驱动时能否合成 canonical 接取"判定：**✅ 能，19 行全部可合成**

判据与证据（逐行可复算）：

1. **接取提交的 R 记录与 `canonicalAcceptFlow` 的两条提交边逐字同构**。以 `3001` 为例：
   - `R3 QUEST_ACCEPT_1 unaccepted→started START_ELIGIBLE after=SYNC:VISIBILITY_REFRESH;DIALOG:SHOW_QUEST_PAGE:QUEST_ACCEPT_1`
     ↔ `canonicalAcceptFlow` 的 `1002` 边：`TalkToNpc(npc,1002)` + `[StartEligible]` + `acceptActions` + `[Sync(VISIBILITY_REFRESH), ShowQuestDialog(QUEST_ACCEPT_1=1003)]` ✅ **逐字同形**
   - `R` 中缺 `QUEST_ACCEPT_SIMPLE`（`3001/3023/21136/1323` 只有 `1002`）不构成缺口：客户端的 `ask_quest_accept` 页对这两类行**只有 `1002`/`1003`**（`3001/3023/21136/1323` 的 action details 实测），canonical 多出的 `20000` 边是**客户端不会发出的 inert 边**，不是死按钮。
   - 拒绝族（`1003/1004/20001`）**并不齐备**（实测逐行）：只有 `21136` / `2611` 三条全有；`1323` 只有 `QUEST_REFUSE_1`；`3001/3023` 有 `REFUSE_1/2` 无 `REFUSE_SIMPLE`；`24202/80320` 三条全有但带额外的 `FINISH_DIALOG` 源（`24202` 多 `s2`、`80320` 多 `s1`）。**但这不构成缺口**：canonical 的 `1004/20001` 边是**客户端不会发出的 inert 边**（`1323/3001/3023` 的 `ask_quest_accept` 页只有 `1002`/`1003`；`quest_refuse_1` 页只有 `1008`）⇒ canonical 是**超集**；反过来，多出来的 `FINISH_DIALOG` 源（`24202@s2`、`80320@s1`）是**中段关窗出口，必须保留**（两条都是 `CLOSE`，after 无页下发 ⇒ 不触发任何退场判据）。
   - **`FINISH_DIALOG` 源集**同样不是逐行统一的：非系统 7 行为 `{unaccepted, started}`（`1323/3001/3023/21136`）、`{unaccepted, started, s2}`（`24202`）、`{unaccepted, started, s1}`（`80320`）、`{unaccepted, started}`（`2611`，另有 `R19 started`）——**canonical 的 `{unaccepted, target}` 恒为其子集** ⇒ 合成**不会丢出口**；11 个系统行的 `FINISH_DIALOG` 只有 `unaccepted`（其 `unaccepted` 源记录在 `buildChain:750-757` 被删 ⇒ 编译期即不存在），与"无接取段"一致。
2. **接取 NPC 可从 `acquired_npc_name` 解析**，且**三方一致**（本片最强的可行性证据）：

| quest | `acquired_npc_name` | R 记录接取 NPC | 客户端 `start_npc_ids`（`legacy-quest-dialog-contracts.csv`） | 一致 |
|---|---|---|---|---|
| `3001` | `Shugo_LF2a_1` | `798132` | `798132` | ✅ |
| `3023` | `Shugo_LF2a_7` | `798138` | `798138` | ✅ |
| `80320` | `event_Ziva` | `831426` | `831426` | ✅ |
| `21136` | `Gehlen` | `799271` | `799271` | ✅ |
| `24202` | `Surt` | `205150` | `205150` | ✅ |
| `1323` | `LF2_Lost_JewelBox` | `730032` | `730032` | ✅ |
| `2611` | `Freyja` | `204763` | `204763` | ✅ |
| （11 行 `_faction_`） | 哨兵 | 无（`unaccepted` 源已删） | 无（`35017` 为 `PARTIAL`） | 不适用 |

   `precheck:250-264` 的 `requireAcquire` 在这 8 行上**已经通过**（否则它们不会在产）；⇒ `index.resolveAll(acquiredNpc)` 唯一，且与 R 记录/客户端登记三重一致 ⇒ **合成不再需要新的名字解析通道**。
3. **`selectionSources` 的来源**：`acceptFlowChain` 的关窗源来自**块 extra**；对无 START 块的 19 行，canonical 合成的关窗源只能从 R 记录推出。实测可推出且与 canonical 的 `{unaccepted, target}` 相容（见 1 的末两条），唯一例外是 `28809`（有 START 块，§4）。
4. **⚠️ 反例 `1323`：入口动作是 `USE_OBJECT`，不是 `QUEST_SELECT`**——这是 19 行里唯一的例外，必须单独裁定。
   ```
   1323	R	5	730032	USE_OBJECT	unaccepted	unaccepted	-	GIVE_ITEM:182201309:1	DIALOG:SHOW_QUEST_PAGE:SELECT1
   1323	R	6	730032	ASK_QUEST_ACCEPT	unaccepted	unaccepted	-	-	DIALOG:SHOW_QUEST_PAGE:SHOW_ASK_QUEST_ACCEPT_WINDOW
   1323	R	2	730032	QUEST_ACCEPT_1	unaccepted	started	START_ELIGIBLE	-	SYNC:VISIBILITY_REFRESH;DIALOG:SHOW_QUEST_PAGE:QUEST_ACCEPT_1
   1323	R	3/4	730032	FINISH_DIALOG	unaccepted|started	同	-	-	CLOSE
   ```
   `1323` **没有** `QUEST_SELECT unaccepted → …` 记录（其余 6 个非系统行都有），其入口是 `TalkToNpc(730032, USE_OBJECT=-1)`（`QuestDialogAction.USE_OBJECT(-1)`；`npc` 名 `LF2_Lost_JewelBox` = 宝箱对象）**且该边自带接取发物 `GIVE_ITEM:182201309:1`**。
   `canonicalAcceptFlow` 的入口是 `QUEST_SELECT(31) → ShowQuestDialog(4)`；套用会同时**换掉入口动作**（对象交互 → NPC 对话框）与**丢掉发物载荷**。`RetailSimpleHuntDefinitionCompiler` 的物品接取形（`canonicalItemAcceptFlow`，事件为 `UseItem(itemId, 0)`）是最近的类比，但事件类型仍不同（`UseItem` vs `USE_OBJECT`）。
   ⇒ **`1323` 的接取段「可翻性未定」，需要 lane owner 在三条中选一**：①保留 `R5` 原记录 + 只 canonical 化其后的 ladder（`R2/R3/R4` 与 `R6`）；②给 `canonicalAcceptFlow` 增入口事件参数（`USE_OBJECT` 变体），把 `GIVE_ITEM` 走 `acceptActions`；③`1323` 整行留 legacy。**注意 `retail-xml-retention.tsv` 记 `1323` 的 basis = `p0c32-accept-entrance-decisions.tsv` 的 `M3D_DOWNGRADE_REVERSED`，且该文件第 19 行写的理由是"accept entrance = verbatim `QUEST_ACCEPT_1` route"——即 P0c-32 当时只核了提交边，**未核入口动作**，本条是新的取证增量。**

### 8.3 处置提案

- **可合成**，且**推荐 S3 以"接取段优先"分档**：把 P1（61 行，现成块）+ `3001/3023/24202/2611/80320`（纯 `select1` 梯入口）+ 计 **66 行**的接取段一次性纳入；`21136` 需先扩 `GIVE_ITEM` 白名单（+1 行 = 67）；`1323` 待裁定（§8.2-4）；14 个系统行**无接取段可纳入**（§7）。交付段按 §5/§6 判据另分一档：P2 的 2 行（`2611/35017`）+ 非系统 `31/1009` 交付边行 **18** 行（`1118 1323 1394 2553 2646 3001 3023 3218 3966 4209 4218 21004 21033 21065 21455 28809 30711 30761`）= **20 行**可合成；其余 γ 行的交付段留 legacy 或单独切片。
- **7 个零 B 块行**（`3001 3023 35025 45010 45017 45024 80320`）的**接取段**：3 个非系统行可合成（§8.2），4 个系统行**无接取段**（§7）。
- **风险**：🟡 合成接取段会**同时**退掉 `R1`（`QUEST_SELECT unaccepted → SELECT1`）与 `R2`（`ASK_QUEST_ACCEPT`）。对 `80320`，`R1` 的 after 是 `DIALOG:SHOW_QUEST_PAGE:SELECT1`、`R2` 是 `SHOW_ASK_QUEST_ACCEPT_WINDOW`；客户端页表 `80320` 的 `select1` 页**唯一动作是 `1007`(ASK_QUEST_ACCEPT)**、`ask_quest_accept` 页是 `1002/1003` ⇒ canonical 后 `select1` 页不再下发、点 NPC 直发页 4 ⇒ **与 G1 同形取舍**，且 `select1` 页会变成 `CLIENT_PAGE_UNREACHED`（非致命，与 `P0c-35` 对 24202 的 1352/1353/1693/1694 同类）。

---

## 9. 本轮新发现的守卫缺口（S3 必须先裁定的三条 fail-open）

| # | 缺口 | 触发条件 | 后果 | 建议 |
|---|---|---|---|---|
| **G-1** | `retiredChainGate:505-518` **只收集 `HAS_ITEM` 条件**，而 `assertRetiredChainRouteQuiet:475-481` **放行 `REMOVE_ITEM` 动作**；且 `canonicalChainHandIn:530` 的 `!itemCheck ⇒ gate=[]` 会把已算出的 `carriedWorkItems` 也丢掉 | 退场记录带 `REMOVE_ITEM` 但**无** `HAS_ITEM`（γ 特有：物品在接取发放、交付时无条件扣除）。单步面无 R 记录 ⇒ **本缺口只在链面暴露** | **静默丢扣物**：`1323 R11 SELECT_QUEST_REWARD v1→reward actions=REMOVE_ITEM:182201309:1`（conditions=`-`）被退场后，`reportItems = carriedWorkItems = [{182201309,1}]` **算得出来但被 `!itemCheck` 短路丢弃**（`:365-369` 的三子形注释："无 item_check = 空门"），`gate=[]`、`payload=[]` ⇒ 覆盖断言空过，扣物动作**凭空消失**，玩家永久留下任务物品 | 把 payload 断言从"条件 ⊆ 门"扩为"**动作 ⊆ 门**"（`REMOVE_ITEM:id:n` 必须能在 gate 找到同 id 且 `count ≥ n` 的项）；或让 `!itemCheck ∧ carried 非空` 时门取 carried（会改动族语义，需单独裁定） |
| **G-2** | 零残留守卫 `assertCanonicalChainResidueFree:578-583` 的交付中转判据**只在 `"started"` 源上生效** | 退场记录把交付页下发行删掉、但交付边（`39/20002`）锚在 `s1/s2/pepper/v3` 等非 `started` 源 | **交付按钮不可达但无红灯**：`24202`（`failure_page='CLOSE'` 使其同时躲过 SELECT6 检查）⇒ 任务不可交付而全部门禁绿 | 判据去掉 `"started"` 限定（改为"该源节点不得有任何交付中转残留"），或补"页下发行与按钮路由成对"的不变量 |
| **G-3** | reward 态重开窗**无守卫**（11 行无 `NPC_COMPLETE`，`3001/3023/80320/2611` 尤甚） | 交付词表把 `SELECT_QUEST_REWARD reward→reward`（`R15`）一并退场 | **静默丢领奖握手**：玩家领奖后关窗，无法再打开奖励窗 | 增不变量：`NPC_COMPLETE` 缺失的行，`reward` 节点必须至少有一条**非退场**的记录作为重开载体（或保留 `R15`） |

另记两条**非缺口但需登记**：
- **`assertCanonicalSelectionSources:548-559` 是 S3 唯一的"响亮"阻断面**（`28809`），处置见 §4.4。
- **`assertRetiredChainRouteQuiet` 对 `VAR_IS`/`GIVE_ITEM` 的拒绝是本片最好的性质**：`3001/3023/21136` 的载荷在朴素翻形下会**直接抛**（不是静默丢）⇒ §5/§6 的提案只需"扩白名单 + 补蕴含/覆盖断言"，不需引入新的检测机制。

---

## 10. γ 阻断面判断（team-lead 问题的直接回答）

**这 7 类特例不构成 S3 的阻断面**，但它们**把 S3 的切片口径从"一刀切"改成了"三分档"**：

- **可无痛翻（现成机制、零新断言）**：P1 的 61 行接取段；P2 的 2 行交付段（`2611`/`35017`）；`2953` 的重复接取（`§3`，机制已核实成立）；`3001/3023/24202/2611/80320` 的接取段（纯 `select1` 梯，§8.2）。
- **需小改动（1–2 处守卫/调用点）**：`3001/3023` 交付段（节点锚定 + 蕴含断言）；`21136` 接取段（`GIVE_ITEM` 通道 + 白名单）；`28809`（关窗源三选一）；`1323` 类 `REMOVE_ITEM` 载荷（G-1）。
- **待裁定（无现成通道）**：`1323` 接取段入口动作 `USE_OBJECT(-1)`（§8.2-4）；`21136` 交付段（阶段链 `SETPRO2→reward`，无 canonical 对应形）；`80320` reward 态重开窗（零 B 块）。
- **应当排除（设计内含）**：14 个 `_faction_` 系统发放行的**接取段**（`§7`，不可翻）；3 个 `I` 记录行的**交付段**（`§2.4`，可翻但收益小、风险不对称 ⇒ 建议整类保留）。

**真正的阻断面是 3 条 fail-open 守卫缺口（§9）**，其中 **G-2 是唯一能造成"门禁全绿 + 任务不可交付"的组合**（`24202`）。⇒ 建议 S3 的**第一片**只做"守卫加固 + 分档判据"，**第二片**才动生产形状；否则 γ 的"指纹变化面 == 缺陷面"判据无法像 G1 那样成立（因为 γ 没有"双块交集"这样的天然切片边界）。
