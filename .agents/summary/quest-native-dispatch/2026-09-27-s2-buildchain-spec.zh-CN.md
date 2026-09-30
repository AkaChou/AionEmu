# SimpleTalk S2（链式面 `buildChain`）canonical 化：逐块实现规格


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 车道：`quest-native-dispatch`；面：**SimpleTalk 链式行 285 个 RETAIL_TABLE id**（`quest_client_talk_chain_steps.tsv` 登记）；
> 唯一落点：`src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkDefinitionCompiler.java` 的
> **`buildChain`（当前 `:412-633`）与其被调用 helper**。
> 本文面向"照做不改判"：每条改点给出**当前行号 + 原文片段 + 目标代码草案**。行号为 2026-09-27 本工作区实测（改动前）；
> **所有改动都是 `buildChain` 及其私有 helper 的原位改写，不改登记表、不改 TSV、不新增文件**。
> 前置证据：`2026-09-27-simpletalk-canonical-survey.zh-CN.md`（§2 落点、§3 策略 A、§4.1 家族门）；
> 规范形样板：`RetailSimpleTalkDefinitionCompiler.build`（S1 已收口）+ `RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow`
> （`:1085` 两参 / `:1096` 三参）+ `RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage`（`:455`）/
> `canonicalDelivery`（`:473`）。

---

## 0. 改前实测基线（本文所有数字的复算口径）

口径：`retail-xml-retention.tsv` 中 `owner=RETAIL_TABLE` 且落在 `quest_client_talk_chain_steps.tsv` 的行 = **285**；
"有块行"= `B NPC_START` / `B NPC_REPORT` 记录存在。

| 事实 | 数值 | 用途 |
|---|---|---|
| 链式行总数 | **285** | 改造面 |
| 有 `NPC_START` 块的行 / 块数 | **266 行 / 268 块**（`2266` 一行 3 块） | 改点 2 作用面 |
| 无 `NPC_START` 块的行 | **19**（2611/3001/35010/35017/35018/35024/35025/45010/45011/45017/45018/45024/45025/45026/1323/3023/21136/24202/80320） | 改点 2 的**例外面**（保持逐字回放） |
| 有 `NPC_REPORT` 块的行 / 块数 | **205 行 / 208 块**（`2611` 一行 4 块） | 改点 5 作用面 |
| 无 `NPC_REPORT` 块的行 | **80**（其中 **17** 行同时无 `NPC_START` 块；`2611`/`35017` 无 START 块**但有** REPORT 块） | 改点 5 的**例外面** |
| **有 START 或 REPORT 块（本片预期指纹漂移面）** | **268 行** | §10.4 重冻对拍 |
| **两者皆无（预期零漂移面）** | **17 行**（1323, 3001, 3023, 21136, 24202, 35010, 35018, 35024, 35025, 45010, 45011, 45017, 45018, 45024, 45025, 45026, 80320） | §10.4 防掩盖对拍 |
| `NPC_REPORT` 块 `target` 取值 | **恒 `reward`**（208/208） | 改点 5 的 `canonicalDelivery` 硬编码 `"reward"` 兼容性证明 |
| `NPC_REPORT` 块 `extra`（页）取值 | **恒 `SELECT5`**（208/208） | 改点 5：`page` 参数随 canonical 废弃 |
| `NPC_START` 块 target | `started` 265 / `s0` 2（28809/80752）/ `v0` 1（1118） | 改点 2 的 finish 源核对 |
| `NPC_COMPLETE` 块 `cri` | **恒 0**（277/277） | 改点 9：完成档位单档 |
| 链式行 quest.xml 奖励槽位 `maxSlot` | **恒 1**（285/285） | 改点 5：`deliveryWindowPage` 恒 = 档 0 = 窗 1，**与现状固定窗 1 逐字等价（零行为变化）** |
| `reward` 节点投影状态 | **恒 `REWARD`**（0 反例） | 改点 5：`canonicalDelivery` 的 `LEVEL_AND_VISIBILITY_REFRESH` 与现状 `reportFlowChain` 的 mode 计算逐字一致 |
| R 记录总数 / 不同动作 | 2235 / 35 | 改点 3 |
| R 记录键 **命中 legacy 块边键** | **50 条 —— 全部是 `(unaccepted, 块接取NPC, SELECT1_1)`** | 证明现行「显式路由覆盖块路由」**只在续页梯上生效过** |
| R 记录键 **命中 canonical 块边键** | **0 条** | 改点 3 的核心事实（见 §3.2） |
| 块间同键重复（含 START/REPORT/COMPLETE 全量模拟） | **0 条** | 改点 7：同键去重空转 |
| 下发 `ShowQuestDialog(SELECT1)`（真正的 select1 页）的 R 记录 | **7 条，全部在 19 个无块行** | 改点 2：删续页梯后**零死按钮**的证明 |
| `exits` 要求 `SELECT1_1` 的行 / 其中靠块合成的 | **98 / 46**（52 行有 R 记录） | 改点 2 丢边面 |
| `exits` 要求 `SELECT2_CONTINUE` | **257 行** | 改点 6：闭包块 1 **必须保留** |
| `exits` 要求 `SELECT5_CHECK`(+`_SIMPLE`) | **21 行**（全部 `item_check=1`） | 改点 5/6：门形状差异**空集** |
| 闭包块 2 实际触发行（`reward` 源 `QUEST_SELECT`→SELECT5 页 ∩ flag） | **18 行** | 改点 6：闭包块 2 **必须保留** |
| `exits` 要求 `SELECT6` | **12 行**（1152, 1932, 3092, 3961-3964, 4966-4969, 80320） | 改点 6：闭包块 3 **必须保留** |
| `MOVIE:` token | **4 条**：1422(`MOVIE:100`,R4 `1353 started→started`)、2421(`:132`,同形)、3006(`:361`,R8 `1694 s1→s1`)、3020(`:363`,R2 `ASK_QUEST_ACCEPT started→started`) | 改点 8：**全不在过滤面内** |
| `I` 记录 | **3 条**：1152(`203130 pepper→reward 169400112 1 - -`)、24202(`205150 s2→reward 182215465 1 - CLOSE`)、80320(`831427 s1→reward 182215303 12 - -`)，**全部在无 `NPC_REPORT` 块的行** | 改点 4 |
| `E` 记录（奖励行自愈） | 42 行 | 改点 7：不动 |
| 客户端页-按钮映射（`page-action-map.csv`，S2 过场裁定用） | `select2`(1352) = `{1353, 20002, 39}`；`select3`(1693) = `{1694, 1779}`；`select2_1`(1353) = `{1694}`；页 4（`SHOW_ASK_QUEST_ACCEPT_WINDOW`）按钮集 = `{1002,1003,1008,20000,20001}`（无 1007） | §8.2：承载边能否退场的死按钮判据 |
| `ASK_QUEST_ACCEPT(1007)` 边 | 135 条 = **114 条 `npc ∈ startNpcIds`（全 `started` 源）+ 21 条无块行（`unaccepted` 源）** | §8.2/§3.4 B1b：114 条退场、21 条保留 |
| 带 `MOVIE:` 的承载边 | `1353` × 2（1422/2421）、`1694` × 1（3006）、`1007` × 1（3020） | §8 过场迁移 |

**一句话结论**：S2 的 canonical 面 = **有块的接取段（266 行）+ 有块的交付段（205 行）**；
R 记录里"中间人对话页链"（`SETPRO1`/`SELECT2_1`/`1353`/`1694`/`SELECT3/4` 等）是**步进语义**，按 DD 混合链先例**一律保留**；
唯一被过滤的 R 记录 = **接取段旧页链残留 53 条**（`SELECT1_1` 50 + `SETPRO1` 3，皆落在"块接取 NPC 且 `unaccepted` 源"上）；
4 行链式过场**不需要迁移**（`MOVIE:` 挂在 R 记录的页链边上，不在任何过滤集合内）。

---

## 1. 改点总表

| # | 改点 | 位置（当前行） | 动作 | 实测影响面 |
|---|---|---|---|---|
| 1 | N 记录 → 节点 | `:414-418` | **不动** | 285 行 |
| 2 | SystemGrant 边 | `:425-429` | **不动** | 系统发放链行 |
| 3 | `NPC_START` 块接取段换 canonical；**续页梯删除** | `:430-449` | **必改 + 必删** | 266 行 / 268 块；续页梯丢边 46 行（零死按钮，§2.4） |
| 4 | R/C/Q/E 回放 + 显式路由覆盖 → **策略 A** | `:450-472` + `:505-515` | **必改** | 过滤 53 条 R 记录；覆盖机制实测 0 命中 |
| 5 | `I` 记录 → `itemReportGate` | `:477-479`（实现 `:866-916`） | **保留（不整类退场）** | 3 条 / 3 行（全在无 REPORT 块行） |
| 6 | `NPC_REPORT` 块交付段换 canonical | `:480-491`（实现 `:799-856`） | **必改** | 205 行 / 208 块 |
| 7 | 三处闭包块 | `:532-556` / `:559-579` / `:586-607` | **全部保留** | 257 / 18 / 12 行（触发条件均不恒假） |
| 8 | `NPC_COMPLETE` 块 / 同键去重 / 领奖行自愈 / 布局回放 | `:501-503` / `:519-528` / `:612-623` / `:624-630` | **不动**（§7 列两处可选清理） | 277 行 / 0 命中 / 42 行 / 285 行 |
| 9 | 过场轴 | `:529` 后新增 `reattachChainMovies` 调用（§8.3） | **定点迁移 4 行**（承载边按 §8.2 分派：1007 退场、`1353`/`1694` 保留边只摘电影）；门禁补电影守恒断言 | 4 行 MOVIE + 114 条 1007 |
| 10 | fail-closed 清单 + 零残差判据 | 新增 guard | **必做** | 生产侧 4 条 guard |

**改后必须删的死代码**：`acceptFlowChain`（`:778-796`）、`reportFlowChain`（`:799-856`）、`acceptContinuation`（`:1071-1080`）、
`acceptFlow` 三个重载（`:1027/1031/1036`）——**改动后全仓零调用者**（已核实：`acceptFlowChain` 仅 `:435`、`reportFlowChain` 仅 `:488`、
`acceptContinuation` 仅 `:445`、`acceptFlow(…3参)` 仅 `:782` 与已被 S1 迁走的 `build`）。

---

## 2. 改点 3：`NPC_START` 块的接取段换 canonical（`:430-449`）

### 2.1 当前行为（原文片段）

```java
430			} else if (!steps.blocks(entry.questId(), "NPC_START").isEmpty()) {
431				// 多变体任务可有多个 NPC_START（镜像/职业变体，判例 1484 五块）——每块各合成接取流。
432				for (RetailClientTalkChainSteps.BlockRecord start : steps.blocks(entry.questId(), "NPC_START")) {
433					// wave B：extra 第三段 = 接取发物（give_item → accept-actions 的 GiveItem，'-' = 无）。
434					String[] startExtra = start.extra().split("\\|");
435					blockTransitions.addAll(acceptFlowChain(start.npcId(),
436						List.of(startExtra[0].split(" ")),
437						startExtra[1], start.target(),
438						startExtra.length > 2 ? decodeActions(startExtra[2]) : List.of()));
439					// P0c-40：接取页梯（select1 → select1_1 → [select1_1_1] → 接取窗）与单步路径同形，
440					// 续页出口同样只认客户端登记（真端模板表没有页链列）。缺这一段则 select1 页的
441					// 「继续听」按钮在链式行上无路由（客户端死按钮；判例 21460/29070/29071）。
442					// P0c-40: the accept page ladder (select1 → select1_1 → [select1_1_1]) is shared with
443					// the single-step path and likewise driven by the client exit registry.
444					if (exits.requires(entry.questId(), RetailClientDialogExits.SELECT1_1)) {
445						blockTransitions.addAll(acceptContinuation(start.npcId(),
446							exits.requires(entry.questId(), RetailClientDialogExits.SELECT1_1_1)));
447					}
448				}
449			}
```

旧形 = `acceptFlowChain`（`:778`）→ `acceptFlow(acquiredNpc, target, acceptActions)`（`:1036`）叠加
`startPage` 覆盖的 `QUEST_SELECT` 边 + `selectionSources` 扩展的 `FINISH_DIALOG` 族 = **select1 页链接取**（1011 页 + 1007 中转 + 1012/1013 续页）。

### 2.2 目标 canonical 行为

- 接取段 = `RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow(npc, acceptTarget, acceptActions)`：
  `QUEST_SELECT(31)` 直发**接取询问窗（页 4）**；`QUEST_ACCEPT_1(1002)` / `QUEST_ACCEPT_SIMPLE(20000)` 两形提交带 `StartEligible` +
  `acceptActions`；拒绝族 `1003→1004 页 / 1004 / 20001 关窗`；`FINISH_DIALOG(1008)` 源 = **`{unaccepted, acceptTarget}`** → 任务选择页(10)。
- **`selectionSources`（`startExtra[0]`）与 `startPage`（`startExtra[1]`）两个参数整体废弃**（canonical 形的 finish 源与开窗页都是固定的）。
- 接取发物（`startExtra[2]` → `decodeActions`）**保留**，落到 1002/20000 两条提交边（与旧形一致）。

### 2.3 代码草案（原位替换 `:430-449`）

```java
		} else if (!steps.blocks(entry.questId(), "NPC_START").isEmpty()) {
			// S2 规范形（quest-native-dispatch）：quest-native-dispatch 链面接取段与单步面同构——
			// QUEST_SELECT 直发接取窗（页 4），1002/20000 两形提交（带块声明的接取发物）、拒绝族、
			// FINISH_DIALOG→任务列表页；select1 入口页、ASK_QUEST_ACCEPT(1007) 中转与 SELECT1_1 续页梯
			// 随页链整体退场（登记表的 selectionSources/startPage 两列在 canonical 形下不再被读）。
			// 多变体任务可有多个 NPC_START（镜像/职业变体，判例 1484 五块）——每块各合成接取流。
			// S2 canonical accept segment, isomorphic to the single-step path: the ask window is
			// emitted directly and the letter-page ladder retires with the page chain; the block's
			// selection sources and start page columns are no longer read.
			for (RetailClientTalkChainSteps.BlockRecord start : steps.blocks(entry.questId(), "NPC_START")) {
				// wave B：extra 第三段 = 接取发物（give_item → accept-actions 的 GiveItem，'-' = 无）。
				String[] startExtra = start.extra().split("\\|");
				canonicalStartEdges.addAll(RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow(start.npcId(),
					start.target(), startExtra.length > 2 ? decodeActions(startExtra[2]) : List.of()));
			}
			blockTransitions.addAll(canonicalStartEdges);
		}
```

其中 `canonicalStartEdges` 是**改点 4 的键集真源**（在 R 回放之前声明，见 §3.3）；`NPC_START` 块循环因此必须
**从 `:430` 上移到 R 回放循环（`:450`）之前**——这是本片唯一的结构性顺序调整，安全性见 §3.3。

### 2.4 续页梯删除的判定与"丢边"清单（任务书问题 2）

**判定依据**：canonical 形 `QUEST_SELECT` 下发的是**页 4**（接取询问窗），页 4 的按钮集 = `1002/1003/1008/20000/20001`
（`docs/quest/client-dialog-mapping/page-action-map.csv:30-34`），**不含 `HACTION_SELECT1_1`**；而 select1 页（1011）在
canonical 形下**不再被任何边下发**——实测 R 记录中 `ShowQuestDialog(SELECT1)` 的下发者共 7 条，
**全部落在 19 个无块行**（2611/3001/1323/3023/21136/24202/80320），266 个有块行的定义里 **0 条**。
⇒ 有块行删除 `acceptContinuation` 后**不可能产生客户端死端按钮 `BUTTON_WITHOUT_ROUTE`**（没有页就没有按钮）。

**丢边清单（逐个枚举，非空集）**：

| 类别 | 行数 | 明细 | 裁定 |
|---|---|---|---|
| `exits` 要求 `SELECT1_1` 且**有同键 R 记录** | **52** | 50 行 R 记录 npc == 块接取 NPC（列于 §3.4 层 B1）；2 行（2611/24202）npc ≠ 块接取 NPC | 50 条被层 B1 过滤（零残差）；2611/24202 是**无块行**，其 `SELECT1_1` 边必须保留（它们的 select1 页仍下发） |
| `exits` 要求 `SELECT1_1` 但**仅靠块合成** | **46** | 1131/1183/1363/1452/1527/1528/1691/1909/1918/1932/2135/2209/2247/2278/2515/2523/2569/2692/2767/2901-2904/2912/2917/2964/3006/3020/3035/3085/3092/3961-3964/3970/3973/4501/4905/11068/11072/11139/14122/21460/29070/29071 | 边消失 = 预期（页链退场）；**已证零死按钮** |
| `FINISH_DIALOG` finish 源收窄 | **1** | **28809**：`target=s0` 而 `selectionSources = {unaccepted, s0, s1, s2}` ⇒ 旧 finish 源 `{unaccepted,s0,s1,s2}` → canonical `{unaccepted,s0}`，**丢 `s1`/`s2` 两条 `FINISH_DIALOG`** | 逐条登记（s1/s2 阶段的关窗出口退场，关窗兜底交 DialogService，与 S1/采集族同口径）；其余 284 行的 finish 源集合**不变**（`{unaccepted,target} ∪ selectionSources == {unaccepted,target}`，逐行核验：`started` 265 行、`s0` 两行中 80752 的 selectionSources=`{unaccepted,s0}`、`v0` 一行 selectionSources=`{unaccepted}` 皆相等） |

⇒ **丢边面 = 46 条 SELECT1_1 合成边 + 28809 的 2 条 FINISH_DIALOG（+ 50 条 R 级 SELECT1_1 由层 B1 一并清掉）**，逐条已列。

---

## 3. 改点 4：R/C/Q/E 回放 + 显式路由覆盖 → 策略 A

### 3.1 当前行为（原文片段）

```java
450		for (RetailClientTalkChainSteps.RouteRecord route : steps.routes(entry.questId())) {
...（回放到 transitions，:450-472）
504		// 与 QuestXmlBlockExpander 同口径：块展开路由被显式原始路由覆盖（(source, npc, dialogId) 键）。
505		java.util.Set<String> explicitRoutes = new java.util.HashSet<>();
506		for (QuestTransition transition : transitions) {
507			if (transition.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null) {
508				explicitRoutes.add(transition.sourceNode() + ":" + talk.npcId() + ":" + talk.dialogId());
509			}
510		}
511		blockTransitions.removeIf(transition -> {
512			QuestEvent.TalkToNpc talk = (QuestEvent.TalkToNpc) transition.event();
513			return talk.dialogId() != null
514				&& explicitRoutes.contains(transition.sourceNode() + ":" + talk.npcId() + ":" + talk.dialogId());
515		});
```

**假绿机制（本片最重要的实测事实）**：该机制的方向是"**R 记录覆盖块边 ⇒ 删块边**"。canonical 化后若保留它，
任何与 canonical 边同键的 R 记录都会**把 canonical 边整条删掉**，形状回退旧形而门禁全绿。
**实测 R 键 ∩ canonical 块键 = 0 条**（285 行全量）⇒ 改后该机制既不再命中，也不再需要；
而它**历史上唯一的命中面 = 50 条 `(unaccepted, 块接取NPC, SELECT1_1)`**（`acceptContinuation` 合成边被 R 记录覆盖）。

### 3.2 目标：策略 A（加载期过滤，方向反转）

三层层递进，全部落在 `buildChain` 内：

- **层 A（覆盖：R 记录退场，而不是块边退场）**：`key(R) ∈ canonical 块边键集` ⇒ **该 R 记录不回放、不进 `explicitRoutes`**；
  canonical 块边保留。**实测 0 条**（但必须实现——这是防假绿的机制本身，数据漂移时它才生效）。
- **层 B（旧页链残留过滤）**：
  - **B1 接取段**：`eventType==TALK ∧ source=="unaccepted" ∧ npc ∈ {块接取 NPC} ∧ action ∈ ACCEPT_PAGECHAIN_ACTIONS` ⇒ 丢弃。
    **实测 53 条**（50 × `SELECT1_1` + 3 × `SETPRO1`；`SETPRO1` 三条逐字见 §3.5）。
  - **B2 交付段**：`eventType==TALK ∧ source ∈ {块 report source} ∧ npc ∈ {块 report NPC} ∧ action ∈ {SELECT_QUEST_REWARD, CHECK_USER_HAS_QUEST_ITEM, CHECK_USER_HAS_QUEST_ITEM_SIMPLE}` ⇒ 丢弃。
    **实测 0 条（空集证明）**。
- **层 C（fail-closed）**：作用域内、动作 ∈ canonical 段动作集、键 ∉ canonical 键集 ⇒ **throw**（§8 清单 G3/G4）。

**改后 `explicitRoutes` 处理**：`:505-515` 整块**删除**（其语义已由层 A 在回放侧实现；保留会构成方向相反的毒药）。

### 3.3 代码草案（顺序调整 + 过滤）

**(a) R 回放循环之前**（`:419-421` 之后插入）：

```java
		// S2 规范形（策略 A）：接取段块边先合成，作为 R 记录过滤的键集真源（单一真源，禁二次推导）。
		// 顺序调整的安全性：NPC_START 边在块边列表中的位置不影响任何后续步骤——carriedWorkItems
		// （:486）在旧形下也已经在扫描含 NPC_START 边的块边列表；R 回放只向 transitions append。
		// S2 strategy A: the accept-segment block edges are built first and serve as the one and only
		// key source for the replay filter; the ordering change is inert for every later step.
		List<QuestTransition> canonicalStartEdges = new ArrayList<>();
```

**(b) 新增过滤判据常量与 helper**（放在 `decodeAfters` 附近，`:771` 之后）：

```java
	/** 接取段旧页链动作（S2 退场集）：select1 页的续页梯、ask 中转与 SETPRO1 接取推进。 /
	 * Retired accept-page-chain actions: the letter-page continuations, the ask hop and SETPRO1. */
	private static final Set<QuestDialogAction> ACCEPT_PAGECHAIN_ACTIONS = Set.of(
		QuestDialogAction.SELECT1_1, QuestDialogAction.SELECT1_1_1, QuestDialogAction.SELECT1_1_1_1,
		QuestDialogAction.SELECT1_1_1_2, QuestDialogAction.SELECT1_1_2, QuestDialogAction.SELECT1_1_3,
		QuestDialogAction.SELECT1_1_4, QuestDialogAction.SELECT1_1_5, QuestDialogAction.ASK_QUEST_ACCEPT,
		QuestDialogAction.SETPRO1);

	/** 接取段 canonical 动作集（层 C 判据的"应被键集覆盖"白名单）。 /
	 * Canonical accept actions; every one of them must be covered by the canonical key set. */
	private static final Set<QuestDialogAction> CANONICAL_ACCEPT_ACTIONS = Set.of(
		QuestDialogAction.QUEST_SELECT, QuestDialogAction.QUEST_ACCEPT_1, QuestDialogAction.QUEST_ACCEPT_SIMPLE,
		QuestDialogAction.QUEST_REFUSE_1, QuestDialogAction.QUEST_REFUSE_2, QuestDialogAction.QUEST_REFUSE_SIMPLE,
		QuestDialogAction.FINISH_DIALOG);

	/** 交付段旧页链动作（S2 退场集）：报告页检查对与 1009 中转。 /
	 * Retired delivery-page-chain actions: the check pairs and the reward hop. */
	private static final Set<QuestDialogAction> DELIVERY_PAGECHAIN_ACTIONS = Set.of(
		QuestDialogAction.SELECT_QUEST_REWARD, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM,
		QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE);

	/** TalkToNpc 边的 (source:npc:action) 键（与旧 explicitRoutes 逐字同格式）。 /
	 * The (source:npc:action) key of a TalkToNpc edge, byte-identical to the old explicitRoutes key. */
	private static Set<String> talkKeys(List<QuestTransition> transitions) {
		Set<String> keys = new HashSet<>();
		for (QuestTransition transition : transitions) {
			if (transition.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null) {
				keys.add(transition.sourceNode() + ":" + talk.npcId() + ":" + talk.dialogId());
			}
		}
		return keys;
	}
```

**(c) R 回放循环内**（`:450` 循环体首部，`routeSource` 计算之前/之后均可，建议紧跟 `routeSource` 之后）：

```java
			// S2 策略 A：canonical 段已接管的动作不再逐字回放——R 记录退场而不是块边退场
			// （旧 explicitRoutes 机制方向相反，会把 canonical 边删掉；实测 R 键 ∩ canonical 键 = 0）。
			// Strategy A: registry routes the canonical segments have taken over are dropped here, so the
			// canonical block edges survive; the retired override mechanism did the exact opposite.
			Integer routeActionId = routeDialogId(route.action());        // null = 区间 / '-' / 多值 token
			QuestDialogAction routeAction = routeDialogAction(route.action());
			if ("TALK".equals(route.eventType()) && routeActionId != null) {
				String key = routeSource + ":" + route.npcId() + ":" + routeActionId;
				if (canonicalStartKeys.contains(key)) {
					continue;
				}
				if ("unaccepted".equals(routeSource) && startNpcIds.contains(route.npcId())
						&& ACCEPT_PAGECHAIN_ACTIONS.contains(routeAction)) {
					continue;
				}
				// 1007 ask 中转（S2 通用退场，过场迁移的前置）：canonical 接取段不再有 ASK_QUEST_ACCEPT
				// 边；实测 npc ∈ 块接取 NPC 的 1007 共 114 条（全为 started 源），取消 3020 的承载边。
				// 无块行的 21 条 1007 的 npc 不在集内 ⇒ 不受影响。
				if (startNpcIds.contains(route.npcId())
						&& routeAction == QuestDialogAction.ASK_QUEST_ACCEPT) {
					continue;
				}
				if (reportSources.contains(routeSource) && reportNpcIds.contains(route.npcId())
						&& DELIVERY_PAGECHAIN_ACTIONS.contains(routeAction)) {
					continue;
				}
				// 层 C：段作用域内、canonical 段动作却未被键集覆盖 ⇒ 数据漂移，当场炸。
				if ("unaccepted".equals(routeSource) && startNpcIds.contains(route.npcId())
						&& CANONICAL_ACCEPT_ACTIONS.contains(routeAction)) {
					throw new IllegalStateException("chain accept route not covered by the canonical block: "
						+ entry.questId() + " " + key);
				}
				if (reportSources.contains(routeSource) && reportNpcIds.contains(route.npcId())
						&& routeAction == QuestDialogAction.QUEST_SELECT) {
					throw new IllegalStateException("chain delivery route not covered by the canonical block: "
						+ entry.questId() + " " + key);
				}
			}
```

配套的四个集合在循环前预计算（R 回放之前，`canonicalStartEdges` 之后）：

```java
		Set<String> canonicalStartKeys = talkKeys(canonicalStartEdges);
		Set<Integer> startNpcIds = steps.blocks(entry.questId(), "NPC_START").stream()
			.map(RetailClientTalkChainSteps.BlockRecord::npcId).collect(java.util.stream.Collectors.toSet());
		Set<String> reportSources = steps.blocks(entry.questId(), "NPC_REPORT").stream()
			.map(RetailClientTalkChainSteps.BlockRecord::source).collect(java.util.stream.Collectors.toSet());
		Set<Integer> reportNpcIds = steps.blocks(entry.questId(), "NPC_REPORT").stream()
			.map(RetailClientTalkChainSteps.BlockRecord::npcId).collect(java.util.stream.Collectors.toSet());
```

> **实现注记（必须照做）**：登记表的 `action` 列是 **token 字符串**（`QUEST_SELECT` / `1353` / `-` / `A..B`），
> 解析发生在 `:464` 的 `expandDialogActionTokens`。**规格要求在编译器内新增两个私有 helper**（与 `parseDialogAction`
> 同处，`QuestDialogAction` 依赖不引入登记表类）：
> `private static Integer routeDialogId(String action)`（单值 token → id；`..` 区间 / `-` / 多值 → `null`）
> 与 `private static QuestDialogAction routeDialogAction(String action)`（枚举名可解析时返回该枚举动作者，数字 token → `null`）。
> 过滤判据统一使用这两个 helper；`routeActionId == null` 即"非本过滤面"（区间 token 实测 0 条落在退场集）。
> 现有的 `:464` 展开逻辑（区间 token → 逐 id 事件）**保持不动**；过滤只作用于单值 token。

### 3.4 层 B1 过滤集合的精确定义（任务书问题 3）

```
B1a = { R 记录 | eventType == "TALK"
              ∧ source == "unaccepted"
              ∧ npcId ∈ { 该任务全部 NPC_START 块的 npcId }
              ∧ action token 可解析为 ACCEPT_PAGECHAIN_ACTIONS 之一 }
B1b = { R 记录 | eventType == "TALK"
              ∧ npcId ∈ { 该任务全部 NPC_START 块的 npcId }
              ∧ action token 可解析为 ASK_QUEST_ACCEPT(1007) }      // 不限 source（实测全为 started）
```

实测命中：**B1a = 53 条 / 50 行**；**B1b = 114 条**（`asked` 源全为 `started`；无块行的 21 条 1007 因
`npcId ∉ startNpcIds` 不在集合内，保持逐字回放）。B1a 逐条可复算：

- `SELECT1_1` × 50，行：1152, 1156, 1158, 1537, 1560, 1628, 1913, 1914, 1915, 1916, 1928, 2383, 2414, 2433, 2486,
  2501, 2538, 2630, 2651, 2914, 2921, 2953, 3008, 3037, 3041, 3083, 3087, 3091, 3093, 3102, 3218, 3972, 4001, 4036,
  4218, 9550, 9558, 9559, 11010, 11103, 11106, 11109, 11228, 21004, 21068, 21071, 21106, 21110, 21111, 30154
- `SETPRO1` × 3，行：**1914 / 1915 / 1916**，逐字：
  `SETPRO1 203759 unaccepted→started cond=START_ELIGIBLE act=- after=SYNC:VISIBILITY_REFRESH;CLOSE`
  —— 与 `canonicalAcceptFlow` 的 `QUEST_ACCEPT_SIMPLE(20000)` 边**逐字同形**（`StartEligible` + `Sync(VISIBILITY_REFRESH)` +
  `CloseDialog`）⇒ 删除后语义由 canonical 边承担，**零语义丢失**。

> **反例警告**：B1 **不得**放宽为"`source=="unaccepted"` + 任意 npc + 动作等于 `QUEST_SELECT/SETPRO1`"——
> 1115（R1/R4/R6 挂在 `talk_npc1=Feyra/203072` 上）、35010/45010 系（无块行）的 `unaccepted` 源 `SETPRO1`/`QUEST_SELECT`
> 是**中间人/旧接取 NPC 的独立对话**，必须保留；放宽即把 `BUTTON_WITHOUT_ROUTE` 从接取段搬到中间人段。

**过滤集合 F 的完整定义（改后链式定义里不再出现的 R 记录）**：`F = 层 A ∪ 层 B1 ∪ 层 B2`，
其中层 A = `{R | key(R) ∈ talkKeys(canonicalStartEdges) ∪ talkKeys(canonical 交付边)}`（实测 0），层 B2（实测 0）。
⇒ **实测生效的只有层 B1 的 53 条**。

### 3.5 `blockTransitions` 去重逻辑是否仍需要（任务书问题 3 尾问）

`:519-528` 的块间同键去重（键 = `source:npc:dialogId:priority`）：全量模拟（`NPC_START` 268 块 + `NPC_REPORT` 208 块 +
`NPC_COMPLETE` 277 块的 preview 边）**实测 0 条重复**。canonical 化后块边**更少**（交付段由 3~5 条变 1 条、接取段删续页梯），
⇒ 仍为 0。
**结论：可删（空转），但本片建议保留**——理由：① 删除是纯清理，与 canonical 化无因果关系，混进本片会污染 diff 与 T3 身份集；
② 它是 `QuestXmlBlockExpander` 同口径的防御位，未来多变体块增长（判例 1486 类）时仍有意义。
**登记为后续清理项**（与 §7 的 `emptyIfNull` 死代码同批）。

---

## 4. 改点 5：`I` 记录 → `itemReportGate`（任务书问题 4）

**实测结论：`I` 记录不是"整类退场"，而是"整类保留"**——因为 3 条 `I` 记录**全部落在无 `NPC_REPORT` 块的行**，
它们是这些行**交付段的唯一来源**：

| quest | I 记录 | 该行交付来源 | 退场后果 |
|---|---|---|---|
| `1152` | `203130 pepper→reward item=169400112 req=1 remove=- failure=-`（SELECT6 失败页）。**该行有 `NPC_START` 块（203132）** ⇒ 接取段随本片 canonical 化（其 R6 `SELECT1_1 unaccepted→unaccepted` 被层 B1 过滤），交付段不受影响 | I 记录（无 REPORT 块、R 记录无交付边） | 删除 ⇒ 任务**无法交付**（无 `reward` 入边） |
| `24202` | `205150 s2→reward item=182215465 req=1 remove=- failure=CLOSE` | I 记录 + `E` 记录（自愈） | 同上 |
| `80320` | `831427 s1→reward item=182215303 req=12 remove=- failure=-` | I 记录 + R11 `SET_SUCCEED s1→reward` | 删除 ⇒ 丢失整组 HasItem 门（R11 无门） |

**退场判据（若将来要退）**：仅当"该行存在 `NPC_REPORT` 块 **或** R 记录中存在同 (source, npc, target) 的
`QUEST_SELECT→reward` 交付边"时，`I` -record 才可退场。**当前 285 行满足该判据的行数 = 0**。
⇒ **本片不动 `itemReportGate`（`:866-916`），`I` 记录路径保持 legacy 39/20002 形**，并计入 §9 的零残差**例外面**
（例外清单 = 3 行，逐条列名）。

**残留引用的处理**：`itemReportGate` 的三个调用相关面无变化，唯一影响是**闭包块 3（SELECT6）继续为 `1152`/`80320` 触发**
（它们仍下发 SELECT6 页，见 §6）。

> **风险登记**：`1152`/`80320` 的 `CHECK_USER_HAS_QUEST_ITEM(39)`/`(20002)` 边在家族门 S1 新增的
> `hasCanonicalDelivery` **链式分支不适用**（`inspectChain` 只查节点/布局/状态集，见 §10），所以不会变红；
> T2/T3 面则可能被"锁旧形"的对齐类测试命中（`RetailSimpleTalkMigrationReviewContractTest` 已覆盖 1152），
> 分拣口径 = **`I`-record 行按 legacy 保留，断言原样**（与 80482/80486 在 S1 的分拣先例同形）。

---

## 5. 改点 6：`NPC_REPORT` 块交付段换 canonical

### 5.1 当前行为（原文片段）

```java
480		for (RetailClientTalkChainSteps.BlockRecord report : steps.blocks(entry.questId(), "NPC_REPORT")) {
481			// 真端 reward_npc_name 指定交付 owner；旧登记块的 NPC 可停在前一位 talk_npc。 /
482			// Retail reward_npc_name owns turn-in; older block snapshots can point to the preceding talk NPC.
483			Set<Integer> rewardIds = index.resolveAll(List.of(
484				entry.rewardNpc() == null ? "" : entry.rewardNpc())).npcIds();
485			int reportNpc = rewardIds.size() == 1 ? rewardIds.iterator().next() : report.npcId();
486			List<QuestItemRequirement> reportItems = metadata.itemRequirements().isEmpty()
487				? carriedWorkItems(metadata, transitions, blockTransitions) : metadata.itemRequirements();
488			blockTransitions.addAll(reportFlowChain(reportNpc, report.source(), report.target(),
489				report.extra(), nodeByLabel, reportItems, entry.itemCheck(),
490				exits, entry.questId()));
491		}
```

`reportFlowChain`（`:799-856`）产出三条子形：
① 报告页 `QUEST_SELECT source→source` 显示 `SELECT5`；
② 无 `checkButton` 时：`SELECT_QUEST_REWARD(1009)` 边（带门）→ `target` + 窗页 `SHOW_SELECT_QUEST_REWARD_WINDOW1`，`requiresItems` 时另有 prio-1 **失败回页**；
③ `checkButton`（`exits` 的 `SELECT5_CHECK`/`_SIMPLE`）时：39/20002 各一对成功（门 + `RemoveItem` → target + 窗1）/失败（`SELECT6` 页或关窗），另有 `FINISH_DIALOG` 出口。

### 5.2 目标 canonical 行为

单条 `QUEST_SELECT(31)` 边：`report.source()` → **`reward`**（与块 target 逐字一致，实测 208/208），
条件 = 整组 `HasItem`、动作 = 整组 `RemoveItem`、after = `[Sync(LEVEL_AND_VISIBILITY_REFRESH), ShowQuestDialog(档位窗)]`；
未集齐 ⇒ **零路由**（关窗兜底交 DialogService）；报告页 `SELECT5`、39/20002 检查对、prio-1 回页、`SELECT6` 失败页**整类退场**。

### 5.3 参数形态（任务书问题 5 尾问：是否需要 metadata）

**需要，且形态与单步面逐字相同**——两个 helper 都是同包 package-private static，直接调用，**零新增函数**：

| helper | 签名 | 链面调用 |
|---|---|---|
| `RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage` | `(QuestMetadata metadata, int questId)` | `deliveryWindowPage(metadata, entry.questId())` —— `metadata` 与 `questId` 在 `buildChain` 形参中已有 |
| `RetailSimpleCollectItemDefinitionCompiler.canonicalDelivery` | `(int rewardNpc, List<QuestCondition> hasItems, List<QuestAction> removeItems, int rewardWindowPage, String collectSource)` | `canonicalDelivery(reportNpc, hasItems, removeItems, 窗页, report.source())` |

**窗页与现状逐字等价（零行为变化证明）**：`deliveryWindowPage` = `rewardWindowForTier(rewardGroups().size()-1)`；
实测全部 285 行的 `quest.xml` 奖励槽位 `maxSlot == 1` ⇒ `size() == 1` ⇒ 档 0 = `SHOW_SELECT_QUEST_REWARD_WINDOW1` = 现状
`reportFlowChain` 的固定窗 1。零奖励组行由 helper 自身的兜底（窗 1）覆盖。
**⇒ 本片不会出现 DD 四子面那次的"零奖励组越界"回归**（QE-028 兜底已在 helper 内）。

### 5.4 代码草案（原位替换 `:480-491`）

```java
		for (RetailClientTalkChainSteps.BlockRecord report : steps.blocks(entry.questId(), "NPC_REPORT")) {
			// S2 规范形（quest-native-dispatch）：交付段与单步面/采集族同构——QUEST_SELECT(31) 带整组
			// HasItem 门直翻 REWARD 并下发档位奖励窗（QE-028 单一真源；实测本族全部行的奖励槽位为 1，
			// 窗页恒等于旧形固定窗 1，零行为变化）。报告页 SELECT5、39/20002 检查对、prio-1 回页与
			// select6 失败页随页链退场——未集齐零路由，关窗兜底交 DialogService。
			// 过场轴不在本处重挂（MOVIE token 由登记表 after 列承载）：链面过场统一在
			// §8.3 的 reattachChainMovies 于块边并入后迁到落点 QUEST_SELECT 边。
			// S2 canonical delivery, isomorphic to the single-step path; the report page, the check
			// pairs, the priority-1 fallback and the failure page all retire with the page chain.
			if (!"reward".equals(report.target())) {
				throw new IllegalStateException("chain report block target must project REWARD: "
					+ entry.questId() + " target=" + report.target());
			}
			Set<Integer> rewardIds = index.resolveAll(List.of(
				entry.rewardNpc() == null ? "" : entry.rewardNpc())).npcIds();
			int reportNpc = rewardIds.size() == 1 ? rewardIds.iterator().next() : report.npcId();
			List<QuestItemRequirement> reportItems = metadata.itemRequirements().isEmpty()
				? carriedWorkItems(metadata, transitions, blockTransitions) : metadata.itemRequirements();
			if (entry.itemCheck() && reportItems.isEmpty()) {
				throw new IllegalArgumentException("retail report item gate missing: " + entry.questId());
			}
			List<QuestCondition> hasItems = reportItems.stream()
				.<QuestCondition>map(item -> new QuestCondition.HasItem(item.itemId(), item.count())).toList();
			List<QuestAction> removeItems = reportItems.stream()
				.<QuestAction>map(item -> new QuestAction.RemoveItem(item.itemId(), item.count())).toList();
			blockTransitions.add(RetailSimpleCollectItemDefinitionCompiler.canonicalDelivery(reportNpc,
				entry.itemCheck() ? hasItems : List.of(), entry.itemCheck() ? removeItems : List.of(),
				RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage(metadata, entry.questId()),
				report.source()));
		}
```

**门形状差异核验（必须留痕）**：旧形门 = `requiresItems = checkButton || itemCheck && !items.isEmpty()`；
新形门 = `itemCheck ? reportItems : []`。差异面 = `checkButton == true ∧ itemCheck == false` 的行——
**实测 21 个 flag 行全部 `item_check = 1` ⇒ 差异面为空集**（`§0` 表）。
**旧 guard `requiresItems && items.isEmpty() ⇒ throw` 收窄为 `itemCheck && reportItems.isEmpty()`**：触发面不扩大（实测 0 行）。

### 5.5 `carriedWorkItems` 的保留与不变性证明

- **保留**（`:929-955` 不动），调用点与实参不变（`metadata, transitions, blockTransitions`）；
- **结果不变的证明**：它扫描 `transitions ∪ blockTransitions` 并**跳过** `source=="reward" ∨ target ∈ {"reward","complete"}` 的边
  （`:934-937`）。canonical 交付边的 `target == "reward"` ⇒ **被跳过**；旧形的 `1009` 推进边同样 `target == target(块) == "reward"` ⇒ 同样被跳过。
  接取侧 `acceptActions` 落在 `target == start.target()`（`started`/`s0`/`v0`，均非 reward/complete）的边上，**两形一致**。
  ⇒ 唯一输入差异是"块边的条数"，而条数差异全在被跳过的集合内 ⇒ **`carriedWorkItems` 输出逐字不变**。
- **例外（必须 fail-closed 的原因）**：若某天出现 `projection != REWARD` 的 report target，交付边的 `target` 不再是 reward，
  扫描会把它计入 ⇒ 结果漂移。⇒ `§5.4` 的 target guard 就是为此。

---

## 6. 改点 7：三处闭包块（任务书问题 6）

**总结论：三块全部保留**，判据 = "触发条件在 canonical 形下**均不恒假**；删除会产生客户端死端按钮"。

### 6.1 `SELECT2_CONTINUE` 闭包（`:532-556`）——**保留**

- 作用：`QUEST_SELECT → ShowQuestDialog(SELECT2)` 且同 (source, npc) 无 `SELECT2_1(1353)` 续页路由时，改发 `DEFAULT_SUCCESS` 页（避免非当前步骤 NPC 的死按钮）。
- canonical 后**触发源仍在**：`SELECT2` 页的下发者共 **276 行**，全部来自**R 记录的中间人页链**
  （`QUEST_SELECT started/s1/... → SELECT2`），canonical 接取段（页 4）与 canonical 交付段（档位窗）**都不产生 SELECT2 页**
  ⇒ 该块的行为与改前**逐行相同**。
- **删后后果**：非当前步骤 NPC 上的 `SELECT2` 页缺 1353 续页路由 ⇒ **死端按钮 `BUTTON_WITHOUT_ROUTE`** ⇒ **禁止删**。

### 6.2 `SELECT5_CHECK` / `SELECT5_CHECK_SIMPLE` 闭包（`:559-579`）——**保留**

- 作用：`reward` 源 `QUEST_SELECT` 显示 `SELECT5` 页的边，改发固定窗 1（领奖态不再显示无法处理的 39/20002）。
- canonical 后**触发源仍在**：`reward` 源 `QUEST_SELECT`→`SELECT5` 的 R 记录共 **130 行**，其中 `exits` flag 命中的 **18 行**
  （1152 除外，其实为 I 记录行；实测触发集 = 1932, 3092, 3340, 3547, 3961-3964, 4966-4969, 11304, 14121, 14201, 24121, 24152, 24242）
  ⇒ 该块**不恒假**。
- **删后后果**：这 18 行在领奖态会重新下发 `SELECT5` 页，而其 39/20002 检查边在 canonical 交付后**已不存在**
  （canonical 交付不产生 CHECK 对；`SELECT6` 分支同去）⇒ **死端按钮** ⇒ **禁止删**。
- **注意**：本块改写的是 `afterCommit` 的窗页（`SELECT5 → 窗1`），对 `1152` 无影响（1152 无 `reward` 源 `QUEST_SELECT→SELECT5` 记录）。

### 6.3 `SELECT6` 失败页关窗闭包（`:586-607`）——**保留**

- 作用：任何下发 `SELECT6` 页的节点，必须同节点同 NPC 有 `FINISH_DIALOG` 出口（select6 页唯一按钮 1008）。
- canonical 后**触发源仍在（分三类）**：

| 行 | canonical 后的 `SELECT6` 下发者 | 闭包是否触发 |
|---|---|---|
| `1932`/`3092` | **R 记录**的 `CHECK_USER_HAS_QUEST_ITEM(_SIMPLE) reward→reward → SELECT6`（4 条，非块合成） | **触发**（原样保留，与判例 1932/3092 一致） |
| `1152`/`80320` | **`I` 记录**（`itemReportGate`，`failurePage='-'` ⇒ SELECT6） | **触发**（`itemReportGate` 未被本片改动） |
| `3961-3964`/`4966-4969`（8 行） | **无**（旧下发者 = `reportFlowChain` 的 CHECK 失败分支，随 canonical 退场；R 记录无 SELECT6） | 不触发（空转，零死按钮） |

- **删后后果**：`1932`/`3092`/`1152`/`80320` 的 select6 页 1008 按钮无路由 ⇒ **死端按钮** ⇒ **禁止删**。

---

## 7. 改点 8：`NPC_COMPLETE` 块 / 同键去重 / 领奖行自愈 / 布局回放（任务书问题 7）

| 面 | 位置 | 结论 |
|---|---|---|
| `NPC_COMPLETE` 块 → `completeFlowFromBlock` | `:501-503` | **不动**。已是规范形（8..23 + `cri=0` 预览窗 + class×slot 自动通道，`:1116-1208`）；与本片无交互（不产生 `QUEST_SELECT`）。**零同步** |
| 块间同键去重 | `:519-528` | **不动**（实测 0 命中，§3.5 已论证；登记为后续清理项） |
| 领奖行自愈（`E` 记录 / 无 E 时发射） | `:612-623` | **不动**。42 行 `E` 记录 + `rewardRow > 0` 的补发逻辑与接取/交付段无交互 |
| 布局回放（`P` 记录） | `:624-630` | **不动**。`chainLayout` 与单步同宽（6 位 SECTION 网格） |
| `systemGrant` 形状合同（剥离 `unaccepted` 源路由） | `:492-499` | **不动**，但**顺序敏感**：它作用于 `transitions`（R 回放结果）。策略 A 的层 B1 只过滤"npc ∈ 块接取 NPC"的记录；系统发放链行**无 `NPC_START` 块**（`precheck:182-185` 的 `grantKind().systemGrant() && block 存在` 是另一通道），⇒ 集合为空 ⇒ 两机制零交集。**必须在策略 A 之后执行**（保持现状次序） |
| `RetailQuestDriver` 驱动入口 | `RetailQuestDriver.java:827` | **不动**（形参表不变：`exits`/`chainSteps` 均仍被使用） |

**可选清理项（不在本片收口线内，登记备查）**：`:1347-1349` 的 `emptyIfNull` 全仓零调用者；
`RetailSimpleTalkDefinitionCompiler` 的 `acceptFlow` 三重重载与 `acceptContinuation` 在本片删除后
`RetailClientDialogExits.SELECT1_1`/`SELECT1_1_1` 的**生产消费点归零**（与 S1 裁定一致：只剩 `quest_client_dialog_exits.tsv` 的保留面）。

---

## 8. 改点 9：过场轴（任务书问题 8；已按 s2-movie 取证修订）

> **本节 2026-09-27 修订**：初版结论"4 部电影零迁移"已被 s2-movie 的逐行取证推翻——电影**必须**从"阶段续页/中转按钮"迁到
> "下发对应开页的那条 `QUEST_SELECT` 边"（与 S1 同口径）。**落点三元组已由本规格独立复算，4/4 匹配数 == 1**；
> 但"承载边退场"这一条**必须按行分派**（下 §8.2 给出两条实测反例）。

### 8.1 落点三元组（复核通过：改后边集合中匹配数 == 1）

| quest | movieId | 承载边（当前，`MOVIE:` token 在此） | **落点边（canonical 目标）** | 匹配数 |
|---|---|---|---|---|
| `1422` | 100 | R4 `1353 started→started`（npc 203731=Laokones，页 SELECT2_1） | `(started, 203731, QUEST_SELECT)` = R3（开 SELECT2 页） | **1** |
| `2421` | 132 | R4 `1353 started→started`（npc 204187，页 SELECT2_1） | `(started, 204187, QUEST_SELECT)` | **1** |
| `3006` | 361 | R8 `1694 s1→s1`（npc 700339，页 SELECT3_1） | `(s1, 700339, QUEST_SELECT)` = R7（开 SELECT3 页） | **1** |
| `3020` | 363 | R2 `ASK_QUEST_ACCEPT started→started`（npc 798143，ask 窗） | `(unaccepted, 798143, QUEST_SELECT)` = canonicalAcceptFlow 接取窗边 | **1** |

- 复核方法：在**改后**边集合（canonical 接取块边 + canonical 交付边 + 未被过滤的 R 记录）上重算 `source:npc:action` 多重集，
  四键计数均为 1（复算脚本见附录 B ⑤）。movieId 与真端 `cutsceneid1` 逐字相等（s2-movie 已验，本规格抽验一致）；
  四行**全为 cs1 轴**（`cs2_*` 全表 0 命中）。
- `3020` 与 S1 判例 4056（接取与交付同体 NPC）**逐字同形**：npc 798143 同时拥有接取窗边（`unaccepted` 源）与交付边（`s1` 源），
  故落点必须带 source（`unaccepted`），这正是 `attachMovieToRoute` 的作用域键语义。

### 8.2 承载边处置：**按行分派**（不可全表过滤——两条实测反例）

**反例一（`3006`，禁止退场承载边）**：客户端页映射实测 `select3` 页（id 1693）的按钮集 = `{1694 HACTION_SELECT3_1, 1779 HACTION_SELECT3_2}`
（`docs/quest/client-dialog-mapping/page-action-map.csv`）。退场 R8（`1694`）后，落点边 R7 仍下发 `select3` 页
⇒ `1694`/`1779` 两条按钮**无路由**（`3006` 无 `1779` 路由，且**不存在 SELECT3 页的闭包兜底**——闭包只有 SELECT2/SELECT5/SELECT6 三个）
⇒ **新增客户端契约门红 `BUTTON_WITHOUT_ROUTE`**。

**反例二（`1422`/`2421`，退场承载边会污染落点边）**：退场 R4（`1353`）后，闭包块 1（§6.1）的条件成立
（`exits` 含 `SELECT2_CONTINUE` ∧ 落点边 after 含 `ShowQuestDialog(SELECT2)` ∧ 同 `(source,npc)` 再无 `SELECT2_1` 路由）
⇒ 落点边 R3 的页被改写成 `DEFAULT_SUCCESS`（**简报页内容消失**），`PlayMovie` 随之挂到"完成页"上——
这正是 team-lead 第 3 条所指的**污染**。反之，**只要承载边不退场，闭包条件不成立，落点边页保持 SELECT2，零污染**。

**分派表（照做不改判）**：

| 承载动作 | 涉及行 | 处置 | 依据 |
|---|---|---|---|
| `ASK_QUEST_ACCEPT`（1007 中转） | `3020` **及同规则全部**：`npc ∈ startNpcIds` 的 1007 边共 **114 条**（`source == "started"`；无块行的 21 条 `unaccepted` 源 1007 的 npc 不在 `startNpcIds` ⇒ 不受影响） | **退场**（加入层 B1，判据见 §3.4 扩展条）。安全证明：canonical 接取窗边已承担页 4 下发；页 4 按钮集 `{1002,1003,1008,20000,20001}` 无 1007 ⇒ **零死按钮** | `npc∈startNpcIds` 时 1007 是旧接取链产物；S1/QE-086 已确立 canonical 形无 1007 边 |
| `1353`（SELECT2_1 阶段续页） | `1422`/`2421`（**仅这 2 条带 MOVIE**；`1353` 全表 102 条） | **保留承载边，只摘除 `MOVIE:`**（电影迁走即达成"电影不在旧页"；载体边残留归入"简报页链步进语义保留"，与 DD 混合链先例一致） | 退场触发闭包 ↦ 落点页降级（反例二）；且全表退场会打断 100 余行的简报页链（无对应闭包） |
| `1694`（SELECT3_1 阶段续页） | `3006`（**仅这 1 条带 MOVIE**；`1694` 全表 41 条） | **保留承载边，只摘除 `MOVIE:`** | 退场产生双死按钮（反例一）；无 SELECT3 闭包 |

⇒ **"承载边不再承载过场"通过迁移达成**（`MOVIE:` token 摘除 + `PlayMovie` 挂落点边），
**不依赖删除承载边**；唯一退场的承载边是 3020 的 1007（由 §3.4 的通用 1007 规则承担，非过场特例）。

### 8.3 迁移实现（代码草案，插在 `:529` `transitions.addAll(blockTransitions);` 之后）

落点边横跨"R 记录（1422/2421/3006）"与"块边（3020）"⇒ 迁移必须在**两者合并之后**对整份 `transitions` 执行一次：

```java
		transitions.addAll(blockTransitions);
		// S2 链式过场迁移（quest-native-dispatch）：登记表把 4 部过场的 MOVIE token 编码在"阶段续页按钮"
		// （1353/1694）与"ask 中转"（1007）边上；这些动作在 canonical 形下不再由服务端驱动，电影必须迁到
		// "下发对应开页的那条 QUEST_SELECT 边"（键带 source、match==1 fail-closed，与 S1 同口径）。
		// 落点为 4 行定点白名单（其余 281 行零副作用）；承载边保留（3020 的 1007 已由页链退场规则移除）。
		// The four chain cutscenes move onto the QUEST_SELECT edge that opens the corresponding page.
		transitions = reattachChainMovies(transitions, entry.questId());
```

常量表与 helper（放在 `attachMovieToRoute` 附近，`:1025` 之后）：

```java
	/** 链式过场定点迁移：承载边键 → 落点边键 + movieId（4 行逐行取证，落点匹配数 == 1 已复核）。 /
	 * Per-row chain cutscene moves: carrier key to landing key plus the movie id. */
	private record ChainMovieMove(String carrierSource, int carrierNpc, QuestDialogAction carrierAction,
			String landingSource, int landingNpc, QuestDialogAction landingAction, int movieId) {
	}

	private static final Map<Integer, ChainMovieMove> CHAIN_MOVIE_MOVES = Map.of(
		1422, new ChainMovieMove("started", 203731, QuestDialogAction.SELECT2_1, "started", 203731,
			QuestDialogAction.QUEST_SELECT, 100),
		2421, new ChainMovieMove("started", 204187, QuestDialogAction.SELECT2_1, "started", 204187,
			QuestDialogAction.QUEST_SELECT, 132),
		3006, new ChainMovieMove("s1", 700339, QuestDialogAction.SELECT3_1, "s1", 700339,
			QuestDialogAction.QUEST_SELECT, 361),
		3020, new ChainMovieMove("started", 798143, QuestDialogAction.ASK_QUEST_ACCEPT, "unaccepted", 798143,
			QuestDialogAction.QUEST_SELECT, 363));

	/**
	 * 承载边摘除 PlayMovie（作用域键，match 必须恰为 1；承载动作已按页链规则退场时允许 0——此时电影
	 * 尚未丢失，仅需挂落点），再经 {@link #attachMovieToRoute} 挂到落点边（match==1 fail-closed）。
	 * Strips the carrier movie (scoped key, match exactly one; zero is allowed only when the carrier action
	 * already retired by the page-chain rules) and re-attaches it to the landing edge.
	 */
	private static List<QuestTransition> reattachChainMovies(List<QuestTransition> transitions, int questId) {
		ChainMovieMove move = CHAIN_MOVIE_MOVES.get(questId);
		if (move == null) {
			return transitions;
		}
		List<QuestTransition> stripped = new ArrayList<>(transitions.size());
		int carriers = 0;
		for (QuestTransition transition : transitions) {
			if (move.carrierSource().equals(transition.sourceNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == move.carrierNpc() && talk.dialogId() != null
					&& talk.dialogId() == move.carrierAction().id()) {
				carriers++;
				stripped.add(new QuestTransition(transition.event(), transition.conditions(), transition.actions(),
					transition.targetNode(),
					transition.afterCommit().stream()
						.filter(action -> !(action instanceof AfterCommitAction.PlayMovie)).toList(),
					transition.priority(), transition.sourceNode()));
			} else {
				stripped.add(transition);
			}
		}
		// carriers == 0 仅允许"承载动作已在页链退场集内"（当前 = 3020 的 ASK_QUEST_ACCEPT）；
		// 其余为静默丢失（QE-086 的教训），当场炸。
		if (carriers > 1 || carriers == 0 && !ACCEPT_PAGECHAIN_ACTIONS.contains(move.carrierAction())) {
			throw new IllegalStateException("chain cutscene carrier must match exactly one transition: carriers="
				+ carriers + " quest=" + questId + " carrier=" + move.carrierSource() + ":" + move.carrierNpc()
				+ ":" + move.carrierAction().id());
		}
		return attachMovieToRoute(stripped, move.landingSource(), move.landingNpc(),
			move.landingAction().id(), move.movieId());
	}
```

> **顺序硬约束**：`reattachChainMovies` 必须在 §3.3 的层 B1/B2 过滤**之后**、闭包块（§6）**之前或之后皆可**
> （闭包只改页不改动作，与其无交互）。若放在过滤之前，3020 的承载边尚在 ⇒ `carriers == 1`（可接受）；
> 若放在过滤之后 ⇒ `carriers == 0`（由上面的允许分支接住）。**两种顺序都被 guard 覆盖**，但规格指定**过滤之后**（语义更清晰）。
> **`decodeAfters` 的 `MOVIE:` 解析（`:756-760`）保持不动**——迁移发生在图装配层，不在 token 解码层。

### 8.4 闭包块 1 的删留与污染避免（team-lead 第 3 条）

- **删留结论：保留**（§6.1：257 行触发源仍在，删则非当前步骤 NPC 的 select2 页死按钮）。
- **是否会污染落点边**：`1422`/`2421` 的落点边 R3 携带 `ShowQuestDialog(SELECT2)`，**正是闭包块 1 的候选**
  （`exits` 两行都含 `SELECT2_CONTINUE`）。**只要承载边 R4（1353）不退场**，闭包条件"同 `(source,npc)` 无 `SELECT2_1(1353)` 路由"便**不成立**
  ⇒ 落点边页保持 `SELECT2`，`PlayMovie` 插在真正的开页动作之前 ⇒ **零污染**。
- **若执行者按"全表过滤 1353"实施**：闭包条件成立 ⇒ 落点边页降级为 `DEFAULT_SUCCESS` ⇒ 电影挂在降级页上（**污染**）
  ⇒ **规格禁止全表过滤 1353/1694**（§8.2 分派表）。
- **加强守卫（可选，若团队希望"即使误删也当场炸"）**：在闭包块 1 的改写循环里跳过"afterCommit 含 `PlayMovie` 的边"，
  并把跳过计数与 `CHAIN_MOVIE_MOVES` 的存在性做一致性断言——但这是**冗余防线**，规格**不要求**（避免扩大改动面）。

### 8.5 链门断言草案（team-lead 尾注：放链门类内）

**位置：`RetailSimpleTalkChainGateTest` 类内**（不放家族门）。理由：4 行全是链行；链门已有 `chainSteps` fixture
（`:86-88`）可直接读 `MOVIE: token`；家族门的 S1 过场不变量（`RetailSimpleTalkGateTest:169-216`）已在 `:174` 显式排除链式行。

```java
	/** 家族规模下限：带 MOVIE: token 的链式行数（登记表实测 4；谓词失配 = 不变量空转）。 /
	 * Floor of chain rows carrying MOVIE tokens, so a predicate mismatch cannot silently void the check. */
	private static final int CHAIN_CUTSCENE_ROW_FLOOR = 4;

	/**
	 * 不变量 3（S2 电影守恒 + 落点形状）：登记表 after 列带 MOVIE: token 的行，编译后的定义必须
	 * ① 恰有 1 个 PlayMovie、movieId == token 值、类型 == CUTSCENE；
	 * ② 该 PlayMovie 挂在带 source 的 QUEST_SELECT 落点边上（键 == CHAIN_MOVIE_MOVES 的落点三元组）；
	 * ③ 原承载边（1353/1694/1007）上不再有任何 PlayMovie。
	 * 逐行断言集从 chainSteps 的 after 列动态解析（禁止硬编码 movieId —— 登记表增长时硬编码会静默漏检）。
	 */
	@Test
	void chainCutscenesMoveToTheirQuestSelectLandingEdges() throws Exception {
		// 期望集 = { questId -> movieId }（从 chainSteps.routes(questId) 的 afterCommits 解析 "MOVIE:<id>:<type>"）；
		// assertTrue(expected.size() >= CHAIN_CUTSCENE_ROW_FLOOR);
		// 对每行用与不变量 1 相同的生产口径 fixture 编译（复用 :132-134 的 compile 调用形态），然后：
		// ① PlayMovie 总数 == 1 且 movieId/type 匹配；
		// ② 承载者边的键 == 落点键（source 节点 + npcId + dialogId == QUEST_SELECT.id()）；
		// ③ 承载键上（source:npc:carrierAction）不存在任何 PlayMovie。
	}
```

**若链门编译成本过高**（285 行全量编译）：断言集收窄为"登记表带 MOVIE 的行"（实测 4 行）⇒ 单测耗时与现有
`adoptRowsReplayRegistryAndMatchFrozenFingerprints` 相比可忽略（后者已全量编译 285 行）。

---

## 9. 改点 10：fail-closed 清单与零残差判据（任务书问题 9）

### 9.1 生产侧 guard（6 条：**新增 4 条 + 内建/复用 2 条**，全部 `throw`）

| # | 位置 | guard 草案 | 期望触发 |
|---|---|---|---|
| **G1** | `NPC_REPORT` 循环首（§5.4） | `if (!"reward".equals(report.target())) throw new IllegalStateException("chain report block target must project REWARD: " + questId + " target=" + report.target());` | **0**（实测 208/208 = `reward`） |
| **G2** | 同上（`reportItems` 之后） | `if (entry.itemCheck() && reportItems.isEmpty()) throw new IllegalArgumentException("retail report item gate missing: " + questId);`（旧 guard `:810-812` 的收窄版，触发面不扩大） | **0**（实测 21 个 `item_check` 行皆有门来源） |
| **G3** | R 回放循环内（§3.3c） | 接取段作用域 + `CANONICAL_ACCEPT_ACTIONS` + 键 ∉ `canonicalStartKeys` ⇒ `throw new IllegalStateException("chain accept route not covered by the canonical block: …")` | **0**（canonical 键按 `source:npc:action` 生成 ⇒ 同域记录必被覆盖；guard 的作用是锁定"canonical 动作集 == 合成键集"的一致性，canonicalAcceptFlow 演变而白名单未同步时当即炸） |
| **G4** | 同上 | 交付段作用域（`source ∈ reportSources ∧ npc ∈ reportNpcIds`）+ `QUEST_SELECT` + 键 ∉ 交付 canonical 键 ⇒ `throw new IllegalStateException("chain delivery route not covered by the canonical block: …")` | **0**（实测交付段同域 `QUEST_SELECT` 记录 0 条） |
| **G5** | `reattachChainMovies`（§8.3） | 承载边摘除的作用域键 match 必须恰为 1；`== 0` **仅当**承载动作 ∈ `ACCEPT_PAGECHAIN_ACTIONS`（当前 = 3020 的 1007 已退场）时才允许 ⇒ 否则 `throw new IllegalStateException("chain cutscene carrier must match exactly one transition: …")` | 1422/2421/3006 = 1；3020 = 0（由退场规则合法产生） |
| **G6** | `attachMovieToRoute`（既有，`:1020-1023`） | 落点边 match 必须恰为 1（**S1 已实现，链面复用**） | 4/4 = 1（§8.1 已复核） |

**门禁侧守卫（2 条）**：§8.5 的链面电影守恒 + 落点形状断言（`CHAIN_CUTSCENE_ROW_FLOOR = 4`）；家族门 S1 过场不变量按原样排除链行。
**门禁侧同步（2 处）**：
① `RetailSimpleTalkChainGateTest` 指纹重冻（**漂移面 = 268 行**，`-Dretail.talkChain.fingerprintOut`；id 集不变、节点数逐 id 不变、
   transitions 每任务 -2~-5：删接取页链 1（`SELECT1` 开页）+ 续页 0~1（`SELECT1_1`）+ 报告页 1 + 1009/CHECK 1~5，
   另叠加 B1b 的 1007 退场（该行有则 -1）与过场迁移的 `PlayMovie` 位置变化（4 行），**逐条登记**）；
② `RetailSimpleTalkGateTest.inspectChain`（`:268-305`）：现状只断言节点/布局/状态集/SystemGrant ⇒ **canonical 化不触碰这些面，零同步**；
**建议**在同一类补 2 条链面形状断言（可选，非收口必需）："有 `NPC_START` 块的已受理行必须存在 `QUEST_ACCEPT_1`/`QUEST_ACCEPT_SIMPLE` 接取边"、
"有 `NPC_REPORT` 块的已受理行必须存在 `QUEST_SELECT(source→reward)` 交付边且定义内不再下发 `SELECT5`/`SELECT6`（I-record 行除外）"。

### 9.2 零残差判据（改后链式定义里**不得出现**的旧形 token 清单）

> 全部按"段作用域"限定，避免误伤中间人页链的合法保留面（§3.4 反例警告）。

| # | 判据（对每个已受理链式行，逐行可测） | 实测期望 |
|---|---|---|
| **R1a** | 有 `NPC_START` 块 ⇒ **不存在** `TalkToNpc(npc ∈ startNpcIds, dialogId ∈ {1012, 1013, 1014, 1019, 1034, 1055, 1076, 5103})` 且 `source == "unaccepted"` 的边 | 改后 = 0（改前 50 条 SELECT1_1） |
| **R1b** | 有 `NPC_START` 块 ⇒ **不存在** `TalkToNpc(npc ∈ startNpcIds, dialogId == 1007 /*ASK_QUEST_ACCEPT*/)` 的边（**不限 source**，B1b 规则） | 改后 = 0（改前 114 条，全 `started` 源） |
| **R2** | 有 `NPC_START` 块 ⇒ **不存在** `TalkToNpc(npc ∈ startNpcIds, dialogId == 10000 /*SETPRO1*/)` 且 `source == "unaccepted"` 的边 | 改后 = 0（改前 3 条，1914/1915/1916） |
| **R3** | 有 `NPC_REPORT` 块 ⇒ **不存在** `source ∈ reportSources ∧ npc ∈ reportNpcIds` 的 `dialogId ∈ {1009, 39, 20002}` 边 | 改后 = 0（改前 0） |
| **R4** | 有 `NPC_REPORT` 块 ⇒ 交付边恰 1 条：`QUEST_SELECT(31)`、`source == report.source()`、`target == "reward"`、`conditions == HasItem 整组`、`actions == RemoveItem 整组`、`after == [Sync(LEVEL_AND_VISIBILITY_REFRESH), ShowQuestDialog(deliveryWindowPage)]`、`priority == null` | 改后 = 205 行 / 208 条 |
| **R5** | 定义内 `ShowQuestDialog(SELECT1)` 的**下发者数 == 0**（有块行）；`ShowQuestDialog(SELECT5)` 的下发者**只能**是 `reward` 源（中间人/领奖态重开页，闭包块 2 的输入） | 有块行 = 0；`SELECT5` 只在 R 记录的 `reward` 源上 |
| **R6** | 定义内 `PlayMovie` **只允许**出现在 `CHAIN_MOVIE_MOVES` 的落点边（键 == 落点三元组 `source:npc:31`）上；**承载边**（键 == `source:npc:carrierAction`）零 `PlayMovie` | 改后：4 行各 1 个；其余 281 行 0 个 |
| **R7** | `1422`/`2421` 的落点边 `after` 仍含 `ShowQuestDialog(SELECT2)`（**未被 SELECT2 闭包降级为 `DEFAULT_SUCCESS`**）、`3006` 的落点边仍含 `ShowQuestDialog(SELECT3)` | §8.4 污染判据（改后 = 3 条保持原页） |

**例外面（必须逐条登记，不得写成"全族零残差"）**：
① **19 个无 `NPC_START` 块的行** —— 接取段逐字回放（含 `ASK_QUEST_ACCEPT`/`SELECT1_1`/`SETPRO1` 接取与 `QUEST_SELECT→SELECT1` 页）；
② **80 个无 `NPC_REPORT` 块的行** —— 交付段逐字回放；
③ **3 个 `I`-record 行**（`1152`/`24202`/`80320`）—— 39/20002 检查对保留；
④ **`28809`** —— 丢 `s1`/`s2` 两条 `FINISH_DIALOG`（有意，见 §2.4）。

---

## 10. 门禁 / 生成物 / 验证顺序（照做不改判）

1. **生产改动**：`RetailSimpleTalkDefinitionCompiler.java` 单文件（`buildChain` 顺序调整 + 4 个 helper 删除
   + 5 个常量/record（`ACCEPT_PAGECHAIN_ACTIONS`/`CANONICAL_ACCEPT_ACTIONS`/`DELIVERY_PAGECHAIN_ACTIONS`
   /`ChainMovieMove`/`CHAIN_MOVIE_MOVES`）+ 4 个私有 helper（`talkKeys`/`routeDialogId`/`routeDialogAction`/`reattachChainMovies`））。
   `RetailSimpleTalkTable` / `RetailClientTalkChainSteps` / `RetailClientDialogExits` / 全部 TSV **零改动**。
2. **测试同步**：`RetailSimpleTalkChainGateTest` 增第三不变量（§8.5，含落点形状与电影守恒）+ 指纹重冻；
   `RetailSimpleTalkGateTest` **零同步**（可选加 2 条形状断言）。
3. **契约门**：`RetailQuestContractTest` 的 `SimpleTalk#1118` 用例**必须仍绿**——`1118` 是**无 `NPC_REPORT` 块**行，
   其交付段由 R 记录（`R3` 报告页 + `R4` `1009` 带门交付）承担，**不在本片改造面**；接取段（`NPC_START 203059 unaccepted→v0`）改 canonical 后
   行走器按"当前页可见"挑边（QE-082 原语已覆盖两形态）⇒ 无需新协议原语。
4. **指纹重冻**（`-Dretail.talkChain.fingerprintOut=<path>`）：安装前做**防掩盖对拍**——漂移行集必须**恰等于**
   **有 `NPC_START` 块 ∪ 有 `NPC_REPORT` 块 = 268 行**（266 ∪ 205 的交并集；两块的 203 行 + 仅 START 的 63 行 + 仅 REPORT 的 2 行
   = `2611`/`35017`）；**非漂移行（17 行，§0 表尾）必须逐字不变**——这 17 行是本片"零副作用"的判据，
   任何漂移都说明改动越界（它们的接取/交付段全在 R 记录里）。节点数逐 id 不变（链门回放保真不变量）。
5. **T2/T3**：`affected_quest_tests.py` 选择器对 285 id 命中的类，**先跑后分拣**（分拣口径：① 无块行的断言原样保留；
   ② 有块行的接取/交付形状断言按 §5.4/§2.3 重锚；③ 断言"块合成续页梯""报告页 SELECT5""1009 直翻"的用例按 canonical 重写）。
   收口线同 README：T3 身份集 `ADDED 0 / REMOVED 0`（允许逐条登记的有意 REMOVED），T1 唯一红 = 20035 车道红。
6. **禁止事项**：不得改登记表（策略 B/C 需要跨车道协商）；不得删除任何 TSV（`RetailTsvManifestGateTest` 必须恒绿）；
   不得为链面过场新增页码类 TSV。

---

## 11. 最大风险点（执行者必须先读）

1. **顺序调整是本片唯一的非局部改动**：`NPC_START` 块合成必须上移到 R 回放之前（§3.3a）。
   若忘记上移，`canonicalStartKeys` 只能靠"二次推导"得到 ⇒ 与合成器漂移（`canonicalAcceptFlow` 演变）时
   **层 A 静默失效 ⇒ canonical 边被 R 记录覆盖 ⇒ 假绿复现**（正是 §3.1 的机制）。**必须用同一份 `canonicalStartEdges` 派生键集**。
2. **层 B1 的"npc ∈ 块接取 NPC"限定不可放宽**：放宽会把 1115/35010/45010 系（中间人 / 无块行的旧接取 NPC）的
   `unaccepted` 源对话路由一起删掉，把 `BUTTON_WITHOUT_ROUTE` 从接取段搬到中间人段（§3.4 反例警告）。
   同理 `source == "unaccepted"` 不可放宽为"任意源"（`3020` 的 `started` 源 `ASK_QUEST_ACCEPT` + `MOVIE:363` 会被误删）。
3. **"零残差"必须写成段作用域判据 + 例外清单**：19 无块行 / 80 无 REPORT 块行 / 3 个 `I`-record 行 / `28809` 的 4 类例外
   若被当成"改不彻底"而继续清理，会立即产生真实的功能回归（无交付边 / 无接取页 / 死按钮）。
4. **过场承载边只能按行分派，禁止"全表过滤 1353/1694"**（§8.2 两条实测反例）：`1353`/`1694` 各有 100 余条记录，
   全表退场会打断其余行的简报页链（`select2`/`select3` 页仍在、续页按钮无路由），且带 MOVIE 的 3 条退场分别触发
   "闭包降级落点页"（1422/2421）与"双死按钮"（3006）。**"电影不在旧页"由 `reattachChainMovies` 的 token 摘除达成，与承载边去留解耦**。

---

## 附录 A：例外行清单（改动前实测）

**19 个无 `NPC_START` 块的行**：
`1323, 2611, 3001, 3023, 35010, 35017, 35018, 35024, 35025, 45010, 45011, 45017, 45018, 45024, 45025, 45026, 21136, 24202, 80320`

**80 个无 `NPC_REPORT` 块的行**（升序，实测全量）：
`1118, 1152, 1163, 1323, 1394, 1484, 1851, 2266, 2271, 2480, 2486, 2488, 2538, 2553, 2646, 2663, 2914, 2953, 2954, 2963,
3001, 3023, 3037, 3041, 3087, 3093, 3100, 3218, 3966, 4052, 4209, 4218, 4970, 4971, 4972, 4973, 4974, 4976, 11010, 11070,
11103, 11105, 11106, 11117, 11460, 13809, 19004, 21004, 21033, 21036, 21065, 21068, 21071, 21106, 21111, 21135, 21136, 21217,
21455, 23809, 24202, 28809, 29004, 30711, 30761, 35010, 35011, 35018, 35024, 35025, 35026, 45010, 45011, 45017, 45018, 45024,
45025, 45026, 80320, 80752`
（复算口径：`retail-xml-retention.tsv(owner=RETAIL_TABLE) ∩ quest_client_talk_chain_steps.tsv` 且该行无
`B NPC_REPORT` 记录；与 §0 表格同源，附录 B 的 ① 号脚本可复算。）

**3 个 `I`-record 行**：`1152`（`203130 pepper→reward 169400112 ×1`，失败页 SELECT6；**有 `NPC_START` 块 ⇒ 接取段在本片改造面内**）、
`24202`（`205150 s2→reward 182215465 ×1`，失败页 CLOSE）、`80320`（`831427 s1→reward 182215303 ×12`，失败页 SELECT6）。

**17 行零漂移面（预期指纹逐字不变，任何漂移即越界）**：
`1323, 3001, 3023, 21136, 24202, 35010, 35018, 35024, 35025, 45010, 45011, 45017, 45018, 45024, 45025, 45026, 80320`。

**4 行链式过场（`MOVIE:` token）**：`1422`(100)、`2421`(132)、`3006`(361)、`3020`(363)；
落点三元组与承载边处置见 §8.1/§8.2（落点匹配数 4/4 == 1，附录 B ⑤ 可复算）。

## 附录 B：复算命令（只读，`python3 -B`）

```bash
cd <仓库根>
# ① 285 行分类：有/无 NPC_START、NPC_REPORT 块
python3 -B - <<'PY'
import collections
RET='src/main/resources/aion/data/static_data/quest_retail/'
own={l.split('\t')[0]:l.split('\t')[1] for l in open(RET+'retail-xml-retention.tsv',encoding='utf-8')
     if not l.startswith('#') and len(l.split('\t'))>2}
r=collections.defaultdict(list)
for l in open(RET+'quest_client_talk_chain_steps.tsv',encoding='utf-8'):
    if l.startswith('#') or not l.strip(): continue
    p=l.rstrip('\n').split('\t'); r[int(p[0])].append(p)
rt=[q for q in r if own.get(str(q))=='RETAIL_TABLE']
print('RT',len(rt),
      'no_start',sorted(q for q in rt if not any(p[1]=='B' and p[2]=='NPC_START' for p in r[q])),
      'no_report',len([q for q in rt if not any(p[1]=='B' and p[2]=='NPC_REPORT' for p in r[q])]))
PY
# ② 层 B1a 命中集（53 条）/ B1b（114 条）
#    B1a：R ∧ source==unaccepted ∧ npc ∈ 块 NPC ∧ action ∈ {SELECT1_1, SELECT1_1_1, ASK_QUEST_ACCEPT, SETPRO1}
#    B1b：R ∧ npc ∈ 块 NPC ∧ action == ASK_QUEST_ACCEPT（不限 source）
# ③ R 键 ∩ canonical 块键 == 0 / legacy 块键 == 50（全为 SELECT1_1）
# ④ select1 页下发者 == 7 条，全在无块行；SELECT6 下发者 == 1932/3092 的 4 条 R 记录 + I 记录路径
# ⑤ 过场落点键唯一性（4 行，期望全为 1）
python3 -B - <<'PY'
import collections
RET='src/main/resources/aion/data/static_data/quest_retail/'
recs=collections.defaultdict(list)
for l in open(RET+'quest_client_talk_chain_steps.tsv',encoding='utf-8'):
    if l.startswith('#') or not l.strip(): continue
    p=l.rstrip('\n').split('\t'); recs[int(p[0])].append(p)
A={'QUEST_SELECT':31,'QUEST_ACCEPT_1':1002,'QUEST_ACCEPT_SIMPLE':20000,'QUEST_REFUSE_1':1003,
   'QUEST_REFUSE_2':1004,'QUEST_REFUSE_SIMPLE':20001,'FINISH_DIALOG':1008,'ASK_QUEST_ACCEPT':1007}
for q,(s,n,a) in {1422:('started',203731,31),2421:('started',204187,31),
                  3006:('s1',700339,31),3020:('unaccepted',798143,31)}.items():
    keys=collections.Counter()
    for p in recs[q]:
        if p[1]=='B' and p[2]=='NPC_START':
            npc=int(p[3]); tgt=p[5]
            keys[('unaccepted',npc,31)]+=1; keys[('unaccepted',npc,1002)]+=1; keys[('unaccepted',npc,20000)]+=1
            keys[('unaccepted',npc,1003)]+=1; keys[('unaccepted',npc,1004)]+=1; keys[('unaccepted',npc,20001)]+=1
            keys[('unaccepted',npc,1008)]+=1; keys[(tgt,npc,1008)]+=1
        if p[1]=='B' and p[2]=='NPC_REPORT': keys[(p[4],int(p[3]),31)]+=1
        if p[1]=='R' and p[4] in A: keys[(p[5],int(p[3]),A[p[4]])]+=1
    print(q,(s,n,a),'matches =',keys[(s,n,a)])
PY
```

---

**规格自审（对照任务书 9 条 + s2-movie 三条）**：
① N 记录/SystemGrant 不动（§1、§7）
② NPC_START 接取段换 canonical + 续页梯删除判定与丢边清单（§2.4：46 条合成边 + `28809` 2 条 + 50 条 R 级，零死按钮已证）
③ 策略 A 代码级改法 + 过滤集合精确定义 + 去重结论（§3，B1a 53 条 + B1b 114 条生效 / 层 A、B2 空集 / 去重可删但保留）
④ `I` 记录**不整类退场**（退场判据 + 3 行例外，§4）
⑤ 交付段换 canonical + `deliveryWindowPage(metadata, questId)` 形态 + `carriedWorkItems` 保留与不变性证明（§5）
⑥ 三处闭包块**全保留** + 逐块判据与死按钮反证（§6）
⑦ `NPC_COMPLETE`/去重/自愈/布局回放不动（§7）
⑧ **过场迁到落点边**（§8：落点三元组 4/4 匹配数 == 1 复核；承载边按行分派——1007 退场 114 条、`1353`/`1694` 保留边只摘电影；
闭包块 1 零污染条件；链门断言草案 + `CHAIN_CUTSCENE_ROW_FLOOR=4`）
⑨ fail-closed **6 条 guard**（新增 4 + 内建/复用 2）+ **7 条零残差判据** + 4 类例外（§9）。
**对 s2-movie 三条的回应**：第 1 条 ✅ 采纳并独立复算；第 2 条 **部分采纳**——1007（含 3020）全表退场 ✅，
`1353`/`1694` **禁止全表退场**（§8.2 两条实测反例：`select3` 页双死按钮、落点页被闭包降级），
改为"迁移 MOVIE token + 承载边保留"以达成同一目标（"电影不在旧页上"）；第 3 条 ✅ 给出闭包块 1 的保留判据与"不退承载边即零污染"的规则。
