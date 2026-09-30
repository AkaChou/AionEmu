# S2 链式 4 行过场迁移：逐行落点表（canonical 重挂取证）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 车道 `quest-native-dispatch`；面：SimpleTalk **S2 链式面的过场轴**（真端 `cutsceneid1` + `cs1_haction`）。
> 本文**只读取证 + 台账产出**：不改 `src/**`、不改任何 TSV/测试、不跑 Maven、不 commit。
> 取证时间 **2026-09-27 13:5x**；工作区在飞（`RetailSimpleTalkDefinitionCompiler.java` 为 untracked 新文件，
> 取证时刻 mtime `12:48:43`，md5(12)=`847e3b81c87c`）——**引用实现位置一律用语义锚点，不抄行号**。
> 配套台账：`s2-movie-migration.tsv`（4 行，Tab 分隔，本文件 §4/§5 的机器可读形；前 9 列沿用
> `s1-cutscene-rows.tsv` 表头，后接 S2 专列）。

## 0. 结论（一句话）

4 行的真端过场轴**全部自洽**（`cutsceneid1` 非空、与 `MOVIE:` token 的 movie id 逐字相等、
`cs1_haction` 与 token 所在 R 记录的 action 逐字相等，**零 HOLD**）；期望落点全部是
**"下发对应窗页的那条 `QUEST_SELECT(31)` 边"**（S1 口径），四元组 = 行内三元组
`(source 节点, npcId, 31)` 在改后定义中**恰命中 1 条 transition**（不过滤 / 过滤两情形皆 1）：

| quest_id | 期望落点三元组 | 承载页（真端语义） |
|---|---|---|
| 1422 | `(started, 203731, 31)` | 阶段页 `SELECT2_1`(1353) |
| 2421 | `(started, 204187, 31)` | 阶段页 `SELECT2_1`(1353) |
| 3006 | `(s1, 700339, 31)` | 阶段页 `SELECT3_1`(1694) |
| 3020 | `(unaccepted, 798143, 31)` | 接取窗 `SHOW_ASK_QUEST_ACCEPT_WINDOW`(4)（与 4056 同形） |

两处**必须写进 S2 规格的险情**：① 4 行**全部 accept==reward 同体** ⇒ 不带 source 的
`(npcId, 31)` 键在受理 NPC 上**命中 4 条**（1422/2421/3006/3020 各自的受理 NPC），作用域键是必需项；
② 现挂点三元组在"页链/中转记录退场"后**命中 0**（= 电影静默丢失的那个 0），而
`RetailSimpleTalkChainGateTest` **零过场断言**、家族门 `acceptedCutsceneRowsCarryExactlyOneRetailMovie`
显式跳过链行（`chainSteps.has(questId) → continue`）⇒ 今天丢电影**全门禁绿**（§6 补断言草案）。

## 1. 只读声明与输入指纹（md5(12) / mtime）

| 输入 | md5(12) | mtime | 用途 |
|---|---|---|---|
| `src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml` | `85d84d329fd2` | 2026-09-23T13:14:18 | 真端 `cutsceneid1`/`cs1_haction` |
| `src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv` | `0ae2d9243e96` | 2026-09-26T22:48:34 | 登记表（**兄弟车道生成物，只读**）：4 处 `MOVIE:` token |
| `src/main/resources/aion/data/static_data/quest_retail/quest_client_dialog_exits.tsv` | `32a4d5f9cf08` | 2026-09-26T10:31:31 | 页链出口登记（SELECT2_CONTINUE / SELECT1_1） |
| `src/main/java/.../questEngine/retail/RetailSimpleTalkDefinitionCompiler.java` | `847e3b81c87c` | 2026-09-27T12:48:43 | 编译器（`buildChain` / `decodeAfters` / `attachMovieToRoute`） |
| `src/main/java/.../questEngine/definition/QuestDialogAction.java` | `6fbd65e6c921` | 2026-09-26T11:29:08 | `QUEST_SELECT(31)`、`ASK_QUEST_ACCEPT(1007)`、`SELECT2_1(1353)`、`SELECT3_1(1694)` |
| `src/main/java/.../questEngine/definition/QuestDialogPage.java` | — | — | `SELECT2(1352)/SELECT2_1(1353)/SELECT3(1693)/SELECT3_1(1694)/SHOW_ASK_QUEST_ACCEPT_WINDOW(4)` |
| `src/main/java/.../questEngine/retail/RetailSimpleTalkTable.java` | `212d36de0a95` | 2026-09-25T21:43:00 | `cutscene()` = `cutsceneMovieId >= 0`；`cutsceneTrigger` |
| `src/test/java/.../retail/RetailSimpleTalkChainGateTest.java` | `0f8184b7fd5c` | 2026-09-26T16:42:53 | 链门（**PlayMovie/cutscene 零命中**） |
| `src/test/java/.../retail/RetailSimpleTalkGateTest.java` | `dd418abe6bb8` | 2026-09-27T12:58:50 | S1 家族门过场不变量（样板 + 链行跳过） |
| `src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv` | `13a5c40e361c` | 2026-09-26T22:49:57 | 链行冻结 IR 指纹（重挂会演进 → 需走重冻通道） |
| `.agents/summary/quest-native-dispatch/2026-09-27-simpletalk-canonical-survey.zh-CN.md` | `95ed411d7de5` | 2026-09-27T10:07:24 | §3.1/§3.4/§3.5（过滤策略 A、4 行电影迁移口径） |
| `.agents/summary/scriptdll-quest-driver/build_quest_client_talk_chain_steps.py` | `18e8ef1731e6` | 2026-09-26T22:44:27 | token 生成规则（**放在 `haction` 命中的那条续页 R 记录 after 列**） |
| `docs/quest/client-dialog-mapping/quest-dialog-action-details.csv` | `f77359b3c284` | 2026-08-13T13:48:27 | 客户端页链/按钮逐字证据 |

口径更正一处（不影响结论）：`MOVIE:` token 位于 R 记录的 **after-commit 列（第 10 列）**，
不是 conditions/actions 列——列序 = `qid R seq npc_id action src tgt conds acts afters npc_name page_check prio`
（生成器 `R()` 的形参序），编译器以 `decodeAfters(route.afterCommits())` 回放。

## 2. 四行真端事实（逐字）

真端表 `Quest_SimpleTalk.xml`：全表 `cutsceneid1` 67 行、`cs1_haction` 65 行
（差 2 = 13800/23800 传送门行，S1 台账 §5.3 已登记）；**`cutsceneid2`/`cs2_haction` 全表 0 命中**
⇒ 本族只有 cs1 轴，无第二部电影的落点问题。

| quest_id | xml_line | dev_name | 接取名→npcId | 交付名→npcId | 阶段 talk | movie | cs1_haction | 客户端页链（`quest-dialog-action-details.csv`） |
|---|---|---|---|---|---|---|---|---|
| 1422 | 1042 | 외관개조퀘스트 | Memnes→203912 | Memnes→203912 | Laokones→203731 | 100 | 1353 | select2(1352) —[1353「把冰之宝剑交给他…」]→ **select2_1(1353)** —[SETPRO1(10000)「结束对话」] |
| 2421 | 3509 | 외관개조퀘스트 | Asgeirr→204309 | Asgeirr→204309 | Kerupnise→204187 | 132 | 1353 | select2(1352) —[1353]→ **select2_1(1353)** —[SETPRO1] |
| 3006 | 5282 | 사라진 슈고의 행방 | Shugo_LF2a_1→798132 | Shugo_LF2a_1→798132 | Hecuba→798146 / LF2A_FOBJ_Q3006→700339 | 361 | 1694 | select3(1693) —[1694「详细展开。」]→ **select3_1(1694)** —[SETPRO2(10001)「停止展开。」] |
| 3020 | 5329 | 불타는 어그린트 만나기 | Ankises→798143 | Ankises→798143 | NPC_Agrint_Tartagan→798149 | 363 | 1007 | select1(1011) —[1012]→ select1_1(1012) —[**1007**「询问是否有解决的办法。」]→ **ask_quest_accept(4)** |

读法（决定落点的关键）：`cs1_haction` = **客户端按钮 id**，电影在**该按钮下发的那一页**上播放
（客户端先播片后显页 ⇒ 服务端 after 序必须"PlayMovie 在开窗之前"，QE-086 第 3 条）。
- 1422/2421：按钮 1353 是阶段页的**续页按钮**，它下发的 `select2_1(1353)` 是该阶段的**行动页**
  （其按钮 SETPRO1 = 阶段推进动作，服务端由 R 记录 `SETPRO1 started→s1` 承担）。
- 3006：同理，按钮 1694 下发 `select3_1(1694)`，其按钮 SETPRO2 = 阶段推进（`s1→s2`）。
- 3020：按钮 1007 位于接取页梯 `select1_1(1012)`，下发**接取窗页 4**（与 S1 判例 4056 逐字同形）。

### 2.1 登记表 R 记录（`MOVIE:` token 全表恰 4 处，全在 after 列）

```
3025:1422	R	4	203731	1353	started	started	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT2_1;MOVIE:100:CUTSCENE	NAME:Laokones	SELECT2_1=CLIENT	-
3501:2421	R	4	204187	1353	started	started	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT2_1;MOVIE:132:CUTSCENE	NAME:Kerupnise	SELECT2_1=CLIENT	-
3855:3006	R	8	700339	1694	s1	s1	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT3_1;MOVIE:361:CUTSCENE	NAME:LF2A_FOBJ_Q3006	SELECT3_1=CLIENT	-
3869:3020	R	2	798143	ASK_QUEST_ACCEPT	started	started	-	-	DIALOG:SHOW_QUEST_PAGE:SHOW_ASK_QUEST_ACCEPT_WINDOW;MOVIE:363:CUTSCENE	NAME:Ankises	-	-
```

生成规则（`build_quest_client_talk_chain_steps.py`，页链 while 循环内）：
`movie_bit = ';MOVIE:%s:CUTSCENE' if int(haction_id) == next_action else ''`
⇒ token 挂在 **haction 命中的那条续页/中转 R 记录的 after 列尾部**，即"**承载边**"而非"canonical 边"。
逐行核对：movie id 与 `cutsceneid1` 相等（100/132/361/363 ✓）、action 与 `cs1_haction` 相等（1353/1353/1694/1007 ✓）。

### 2.2 块结构（`B` 记录）与节点

4 行**全部是标准三块**（`NPC_START` / `NPC_REPORT` / `NPC_COMPLETE`），无多变体、无 `I`/`Q`/`E` 记录：

| quest_id | 节点 | NPC_START | NPC_REPORT | NPC_COMPLETE |
|---|---|---|---|---|
| 1422 | unaccepted(started,s1,reward,complete) | 203912 `unaccepted→started` 页 `SELECT1` 发 `ITEM_QUEST_1422A` | 203912 `s1→reward` 页 `SELECT5` | 203912 `reward→complete` |
| 2421 | 同上 | 204309 `unaccepted→started` 页 `SELECT1` 发 `ITEM_QUEST_2421A` | 204309 `s1→reward` 页 `SELECT5` | 204309 |
| 3006 | unaccepted(started,s1,s2,reward,complete) | 798132 `unaccepted→started` 页 `SELECT1`（无发物） | 798132 `s2→reward` 页 `SELECT5` | 798132 |
| 3020 | unaccepted(started,s1,reward,complete) | 798143 `unaccepted→started` 页 `SELECT1` 发 `ITEM_QUEST_3020A` | 798143 `s1→reward` 页 `SELECT5` | 798143 |

⇒ MOVIE token **不在任何块路由上**，只在显式 R 路由上；块只负责接取/报告的 canonical 合成。
⇒ 接取段的 `QUEST_SELECT` 边由 `acceptFlowChain` 合成（`(unaccepted, 受理NPC, 31)`，
页取块 extra 第 2 段 = `SELECT1`）；报告段的 `QUEST_SELECT` 边由 `reportFlowChain` 合成
（`(块 source, 交付NPC, 31)`，页 = `SELECT5`）。

## 3. 现挂点：编译器代码事实（含两处缺陷）

1. **回放**：`buildChain` 对 R 记录逐字回放——`decodeConditions` / `decodeActions` /
   **`decodeAfters`**（`MOVIE:` → `AfterCommitAction.PlayMovie(id, CUTSCENE)`）。
   所以 4 行今天**已经会下发 PlayMovie**，位置在该 R 记录 after 列表内。
2. **语序缺陷（违反 QE-086 第 3 条）**：登记表 token 序是 `DIALOG:SHOW_QUEST_PAGE:X;MOVIE:...`
   ⇒ 解码后 after = `[ShowQuestDialog(X), PlayMovie]`，**电影排在开窗之后**；
   canonical 形要求 `Sync → PlayMovie → 开窗`（`attachMovieToRoute` 的插入点构造）。
3. **落点缺陷**：承载边是"页链续页按钮/1007 中转"，不是 `QUEST_SELECT(31)` 边；
   S1 家族门的落点守卫（`acceptedCutsceneRowsCarryExactlyOneRetailMovie` 内，
   "过场未挂在 QUEST_SELECT 边上"分支）把它判为**不合格**——但该门对链行 `continue`，
   **链门又零过场断言** ⇒ 4 行今天处于"无门禁 + 语序错 + 落点不合口径"的三无状态。
4. **旧挂载器**：`attachMovie(transitions, npcId, triggerActionId, movieId)`（`(npc, action)` 无作用域版）
   已从文件中删除（S1 台账 §5.2 实测 grep 零命中）；现行唯一挂载器 = `attachMovieToRoute`
   （语义锚点：定义处 / `matches != 1` 断言 / 接取调用点 `"unaccepted"` / 交付调用点 `"started"`）。

## 4. canonical 落点逐行裁定

裁定口径（S1 口径 + QE-086）：`PlayMovie` 落在**下发对应窗页的那条 `QUEST_SELECT(31)` 边**上，
匹配键 = `(source 节点, npcId, 31)`，`PlayMovie` 插在该边开窗动作之前；
"接取段 vs 交付段" 由 `cs1_haction` 定——本片 4 行的 `cs1_haction` ∈ {1353, 1353, 1694, **1007**}，
**只有 3020 落在 S1 的"接取段"分支上**；1422/2421/3006 是**阶段页按钮**，S1 的 1007/1009 二分支
覆盖不到，须按"阶段页 canonical 边"逐行裁定（S1 台账 §2 待办 2 已挂账，本文件落地）。

### 4.1 3020：接取段（**无歧义**，与 4056 同形）

- 真端语义：电影 363 播于**接取窗页 4** 的显示（按钮 1007 的第二跳）。
- canonical 形：接取段翻 canonical（`canonicalAcceptFlow`）后，**页 4 由 `QUEST_SELECT(31)` 直发**
  （页链 `select1 → select1_1 → 1007` 与 `SELECT1_1` 续页梯整段退场）。
- 落点 = **`(unaccepted, 798143, 31)`**，改后该边的 after 序 = `[PlayMovie(363), ShowQuestDialog(4)]`
  ——与真端播片页**逐页相同**（无页漂移），与 4056 的 `(unaccepted, 205203, 31)` 逐字同形。
- 前提：R2（`(started, 798143, 1007)` → 页 4）这条**中转记录必须同时退场**，否则页 4 有两名下发行
  （见 §5.3 险情 3）。

### 4.2 1422 / 2421 / 3006：阶段页（**落点已定，前提待裁**）

- 真端语义：电影播于**阶段行动页**的显示（select2_1 / select2_1 / select3_1）。
- 阶段页链在 canonical 形下的两种处理，落点**同为 `QUEST_SELECT(31)` 边**，但播片页不同：

| 方案 | 做法 | 播片页 | 与真端一致 | 落点满足 QE-086 |
|---|---|---|---|---|
| **P1（推荐）** | 阶段续页记录（1353/1694）退场，`QUEST_SELECT(31)` **直发阶段行动页**（`SELECT2_1`/`SELECT3_1`） | `select2_1`/`select3_1` | ✅ 逐页相同 | ✅（且按钮闭环：行动页按钮 = `SETPRO1`/`SETPRO2`，服务端已有该路由） |
| P2 | 保留阶段续页记录，只把电影挪到 `QUEST_SELECT` 边 | `select2`/`select3`（早一跳） | ⚠️ 早一页 | ✅ |
| Q | 电影留在续页边，只修语序 | `select2_1`/`select3_1` | ✅ | ❌ 承载边不是 `QUEST_SELECT` |

  P1 与 S1 的接取/交付同构（"`QUEST_SELECT` 直发窗页、页梯退场"），且**不需要新造页**
  （`SELECT2_1`/`SELECT3_1` 已在 `QuestDialogPage` 中，登记表也已用 `=CLIENT` 背书）；
  Q 被 S1 家族门落点守卫明确排除（"页链按钮不再承载过场"），故取 P1。
- 落点：1422 → `(started, 203731, 31)`；2421 → `(started, 204187, 31)`；3006 → `(s1, 700339, 31)`
  （source 节点取**该阶段 R 记录的 source**，与承载边同源：1422/2421 = `started`，3006 阶段 2 = `s1`）。
- 共同前提：① 续页记录退场（P1）；② 承载记录上的 verbatim `MOVIE:` token **不得再回放**
  （否则定义内 2 部电影，见 §6 断言）；③ `SELECT2_CONTINUE` 关窗闭包不得回头改写这条边（§5.3 险情 4）。

## 5. 唯一性验证（逐行两情形）

匹配口径：`transition.event() instanceof QuestEvent.TalkToNpc` ∧ `sourceNode 相等` ∧ `npcId 相等`
∧ `dialogId == actionId`（`nil` 守卫：`dialogId != null`）。
"不过滤" = 现状逐字回放；"过滤后" = A 策略（canonical 键记录退场：1007 中转 / 页链续页）生效后。

| quest_id | 落点三元组 | 不过滤 | 过滤后 | 现承载三元组 | 不过滤 | 过滤后 |
|---|---|---|---|---|---|---|
| 1422 | `(started, 203731, 31)` | **1** | **1** | `(started, 203731, 1353)` | 1 | **0** |
| 2421 | `(started, 204187, 31)` | **1** | **1** | `(started, 204187, 1353)` | 1 | **0** |
| 3006 | `(s1, 700339, 31)` | **1** | **1** | `(s1, 700339, 1694)` | 1 | **0** |
| 3020 | `(unaccepted, 798143, 31)` | **1** | **1** | `(started, 798143, 1007)` | 1 | **0** |

匹配数来源（静态读码枚举，非运行期实测；`QuestTransition` 发射点见 §4/§2.2 的语义锚点）：

- 落点边 = 唯一一条 `(source, 受理/阶段 NPC, 31)`：1422/2421/3006 来自 R 记录
  `QUEST_SELECT`（`started,R3` / `started,R3` / `s1,R7`），**不属于 canonical 键记录，过滤后仍在**；
  3020 来自 `acceptFlowChain`/`canonicalAcceptFlow` 合成边（`unaccepted`），两情形皆 1。
- 承载边 = 该行唯一一条 haction R 记录（1353/1353/1694/1007），**过滤后归零**。
- 每行受理 NPC 侧的 `(npc, 31)` **无条件命中 4 条**（见下）——落点全部**不落**在受理 NPC 上，
  所以"无作用域键"的危险只在**误用/放宽键**时爆发。

### 5.1 逐行 `(npcId, 31)` 无作用域命中分布（>1 险情的量化）

| quest_id | 受理/交付 NPC（同体）| 该 NPC 的 `(npc,31)` 边（按 source） | 计数 | 阶段 NPC 的 `(npc,31)` |
|---|---|---|---|---|
| 1422 | 203912 | `unaccepted`(块接取) / `started`(R1) / `s1`(块报告) / `reward`(R6) | **4** | 203731 → 1（R3） |
| 2421 | 204309 | `unaccepted` / `started` / `s1` / `reward` | **4** | 204187 → 1（R3） |
| 3006 | 798132 | `unaccepted` / `started` / `s2` / `reward` | **4** | 700339 → 1（R7）；798146 → 1（R3） |
| 3020 | 798143 | `unaccepted` / `started` / `s1` / `reward` | **4** | 798149 → 1（R3） |

⇒ 若实现退化成 `(npcId, actionId)` 键：3020 的落点在受理 NPC 上会**命中 4 条**（电影被复制 4 次）；
1422/2421/3006 若误挂在受理 NPC 上同样是 4 条。**作用域键是必需项**（S1 台账 §2 待办 1 的同结论，
本片从"1 行 2 命中"升级为"4 行 4 命中"）。

### 5.2 =0 险情（静默丢失）

现承载三元组在过滤后**全部归零**（上表右列）⇒ 若迁移片只做"过滤"而**不重挂**，
4 部电影**整体消失**，而今天**没有任何门禁会红**（§3 第 3 点）。
`=0` 的第二重来源：旧 `attachMovie` 的"零匹配原样返回"（QE-086 root_cause）已删除，
但迁移片若改用 `attachMovieToRoute` 而未同步移除 verbatim token，则是"**2 部电影**"的假绿
（`PlayMovie` 数 != 1）——两种方向都必须由 §6 的断言当场炸出。

### 5.3 三条交互险情（落点稳定性）

1. **`SELECT2_CONTINUE` 关窗闭包**（`buildChain` 内，"给非当前步骤 NPC 显示无按钮页"段）：
   对每条 `(source, npc, QUEST_SELECT 31 → 下发页 SELECT2)` 的边，若**同 source 同 npc 无
   `SELECT2_1(1353)` 路由**，则把下发页改写成 `DEFAULT_SUCCESS(10002)`。
   1422/2421 的**落点边 R3 正是这条被检查的边** ⇒ 一旦续页记录 1353 退场而闭包未同步删除，
   落点边的下发页会从 `SELECT2` 翻成 `DEFAULT_SUCCESS`（电影会播在"无按钮页"之前）。
   3006 的落点边（`s1, 700339`）不受影响（闭包只查 `SELECT2` 页），但 `(started, 798146, 31)`
   （Hecuba 阶段）会受影响。⇒ P1 落地时必须**同片处置该闭包**（勘察 §3.1 已标"页链退场后可删（需逐行核）"）。
2. **显式路由覆盖块路由**（`explicitRoutes` 过滤）：落点三元组中 1422/2421/3006 的边是 R 记录本身，
   3020 的是块合成边——**无同键 R 记录覆盖 3020 的落点边**（该行 R 记录的 source 集为
   `started`/`reward`，不含 `unaccepted`）⇒ 两情形下都存活，不会被块过滤误删。
3. **页 4 双名下发行（3020）**：若 1007 中转记录（`(started, 798143, 1007)`）**保留**，
   则页 4 在 `unaccepted`（canonical 接取边）与 `started`（R 记录）两侧都能下发；
   此时电影若挂 `unaccepted` 边 = 与 4056 同形（真端首见即播），而 `started` 侧"进行态再谈"重开页 4 时**无电影**
   （真端也无：1007 只在接取流程出现）⇒ 可接受，但**规格必须显式声明**这条取舍，
   否则会被当成"电影偶尔不播"的偶发 bug。

## 6. 电影数守恒断言草案（链门 `RetailSimpleTalkChainGateTest`）

现状：链门 **0 处** `PlayMovie`/`cutscene` 断言；家族门的过场不变量**跳过链行**。
建议在链门补一例（同族断言放同门），形状照 S1 家族门（`acceptedCutsceneRowsCarryExactlyOneRetailMovie`）
但**加 source 维度**与**语序/守恒**三条：

```java
/** 链式过场行下限：真端表链面声明 cutsceneid1 的行数（1422/2421/3006/3020）。 */
private static final int CHAIN_CUTSCENE_ROW_FLOOR = 4;

/** 落点台账（s2-movie-migration.tsv 的机器可读形）：questId → {source, npcId, movieId, haction}。 */
private static final Map<Integer, String[]> CHAIN_CUTSCENE_LANDINGS = Map.of(
	1422, new String[] {"started", "203731", "100", "1353"},
	2421, new String[] {"started", "204187", "132", "1353"},
	3006, new String[] {"s1", "700339", "361", "1694"},
	3020, new String[] {"unaccepted", "798143", "363", "1007"});

@Test
void chainCutsceneRowsCarryExactlyOneRetailMovieOnTheCanonicalEdge() {
	List<String> problems = new ArrayList<>();
	int checked = 0;
	int moviesTotal = 0;
	for (var landing : CHAIN_CUTSCENE_LANDINGS.entrySet()) {
		int questId = landing.getKey();
		var entry = table.find(questId);
		var meta = retailTable.find(questId);
		// 谓词失配守卫：真端轴必须与台账逐字相符，否则本行不进断言（防"空转绿"）。
		if (entry.isEmpty() || meta.isEmpty() || !chainSteps.has(questId) || !entry.get().cutscene()
				|| entry.get().cutsceneMovieId() != Integer.parseInt(landing.getValue()[2])
				|| entry.get().cutsceneTrigger() != Integer.parseInt(landing.getValue()[3])) {
			problems.add(questId + ": 真端过场轴与台账不符/输入缺失");
			continue;
		}
		var outcome = RetailSimpleTalkDefinitionCompiler.compile(entry.get(), npcIndex,
			RetailQuestMetadataCompiler.compile(meta.get(), npcIndex, itemIndex, randomRewards, nameIds),
			exits, summaryRows, clientRewardNpcs, chainSteps);
		if (!outcome.accepted()) {
			problems.add(questId + ": 被拒绝 " + outcome.rejectionCode() + " " + outcome.detail());
			continue;
		}
		checked++;
		var definition = outcome.definition().definition();
		List<QuestTransition> carriers = definition.transitions().stream()
			.filter(t -> t.afterCommit().stream().anyMatch(a -> a instanceof AfterCommitAction.PlayMovie))
			.toList();
		List<AfterCommitAction.PlayMovie> movies = carriers.stream()
			.flatMap(t -> t.afterCommit().stream())
			.filter(a -> a instanceof AfterCommitAction.PlayMovie)
			.map(a -> (AfterCommitAction.PlayMovie) a)
			.toList();
		moviesTotal += movies.size();
		// ① 每行 PlayMovie 数 == 声明数（1）——静默丢失（=0）与重复（>1）都炸。
		if (movies.size() != 1) {
			problems.add(questId + ": PlayMovie 数 " + movies.size() + "（期望 1）");
			continue;
		}
		// ② 落点守卫：必须是 (台账 source, TalkToNpc(台账 npc, QUEST_SELECT(31)))
		//    —— 无作用域键在受理 NPC 上会命中 4 条，故 source 与 npc 必须同时断言。
		QuestTransition carrier = carriers.get(0);
		if (!(carrier.event() instanceof QuestEvent.TalkToNpc talk)
				|| !landing.getValue()[0].equals(carrier.sourceNode())
				|| talk.npcId() != Integer.parseInt(landing.getValue()[1])
				|| !isAction(talk, QuestDialogAction.QUEST_SELECT)) {
			problems.add(questId + ": 过场未挂在台账 (source,npc,QUEST_SELECT) 边上：" + carrier.event()
				+ " source=" + carrier.sourceNode());
			continue;
		}
		// ③ movieId 与类型 == 真端 cutsceneid1 / CUTSCENE。
		if (movies.get(0).movieId() != entry.get().cutsceneMovieId()
				|| movies.get(0).type() != QuestMovieType.CUTSCENE) {
			problems.add(questId + ": movieId/type 与真端不符：" + movies.get(0));
		}
		// ④ 语序：PlayMovie 必须在同一条边的开窗/关窗动作之前（QE-086 第 3 条，当前无任何断言）。
		List<AfterCommitAction> afters = carrier.afterCommit();
		int movieAt = -1;
		int windowAt = Integer.MAX_VALUE;
		for (int i = 0; i < afters.size(); i++) {
			if (afters.get(i) instanceof AfterCommitAction.PlayMovie) {
				movieAt = i;
			} else if (windowAt == Integer.MAX_VALUE
					&& (afters.get(i) instanceof AfterCommitAction.ShowQuestDialog
						|| afters.get(i) instanceof AfterCommitAction.ShowQuestSelectionDialog
						|| afters.get(i) instanceof AfterCommitAction.CloseDialog)) {
				windowAt = i;
			}
		}
		if (movieAt < 0 || windowAt == Integer.MAX_VALUE || movieAt > windowAt) {
			problems.add(questId + ": PlayMovie 不在开窗之前（after=" + afters + "）");
		}
	}
	assertTrue(problems.isEmpty(), () -> "链式过场不变量缺口：" + problems.stream().limit(10).toList());
	// ⑤ 覆盖下限 + 电影总数守恒：谓词失配（不变量空转）与"过滤顺手删片"都不会静默通过。
	assertTrue(checked >= CHAIN_CUTSCENE_ROW_FLOOR,
		() -> "链式过场行覆盖回退：" + checked + " < " + CHAIN_CUTSCENE_ROW_FLOOR);
	assertEquals(CHAIN_CUTSCENE_ROW_FLOOR, moviesTotal,
		() -> "链式过场电影总数漂移：" + moviesTotal + " != " + CHAIN_CUTSCENE_ROW_FLOOR);
}
```

补充两条配套要求（否则本断言可被"合法"绕过）：

- **守恒的第二半**：`moviesTotal == 4` 与"每行恰 1"合起来才排除了"某行 2 部 + 某行 0 部"的置换；
  若后续再有链行声明 `cutsceneid1`，`CHAIN_CUTSCENE_ROW_FLOOR` 与 `LANDINGS` 必须同片更新
  （下限断言会把"新行没进表"炸出来）。
- **指纹随动**：重挂只改 after ⇒ 节点断言不受影响，但 4 行的链 IR 指纹 (`retail-simple-talk-chain-ir-fingerprints.tsv`)
  必然演进，须走 `-Dretail.talkChain.fingerprintOut` 重冻并**人工核对 4 行差异仅为 `PlayMovie` 位移**后入仓。

## 7. 复算命令（只读）

```bash
# ① 真端轴逐行（movie/haction/形状），确认 cutsceneid2 全表 0 命中
python3 -B - <<'PY'
import re
txt = open('src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml', encoding='utf-8').read()
print('cutsceneid1=', txt.count('<cutsceneid1>'), 'cs1_haction=', txt.count('<cs1_haction>'),
      'cutsceneid2=', txt.count('cutsceneid2'), 'cs2_haction=', txt.count('cs2_haction'))
for qid in (1422, 2421, 3006, 3020):
    seg = re.search(r'<id id="%d">(.*?)</id>' % qid, txt, re.S).group(1)
    print(qid, re.findall(r'<(\w+)>([^<]*)</\1>', seg))
PY

# ② 登记表 MOVIE token（全表应恰 4 处，全在 after 列）
grep -n 'MOVIE:' src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv

# ③ 客户端页链（4 行的 page_order / page_id / action_id / button_text）
python3 -B - <<'PY'
import csv
for r in csv.DictReader(open('docs/quest/client-dialog-mapping/quest-dialog-action-details.csv',
        encoding='utf-8-sig', newline='')):
    if r['quest_id'] in ('1422', '2421', '3006', '3020'):
        print(r['quest_id'], r['page_order'], r['html_page_name'], r['page_id'], r['action_id'],
              r['action_constant'], r['button_text_zh'])
PY
```

## 8. 未决 / 依赖（交 S2 规格线裁定，本文件不预设）

1. **P1 的前提**：阶段续页记录（`1353`×2、`1694`）是否进 A 策略的退役集，
   以及退役后 `QUEST_SELECT(31)` 边的下发页是否改指 `SELECT2_1`/`SELECT3_1`（本文推荐 P1）。
   若裁定为 P2（保留续页页、只挪电影），落点三元组不变、只损失"早一页"的播片精度。
2. **`SELECT2_CONTINUE` 闭包的处置**（§5.3 险情 1）：P1 落地必须同片删除或改写，
   否则 1422/2421 落点边的下发页被改写成 `DEFAULT_SUCCESS`。
3. **3020 的 1007 中转记录**（§5.3 险情 3）：退役则与 4056 完全同形；保留则须显式声明取舍。
4. **verbatim token 的移除方式**：登记表只读（兄弟车道生成物）⇒ 只能在编译器侧
   （链路径过滤 `MOVIE:` token 后由 `attachMovieToRoute` 重挂），**不得改 TSV**（勘察 §3.4 策略 A）。
5. 本文**未**运行编译器（只读纪律：不跑 Maven/不编译），§5 的匹配数为静态读码枚举；
   首个可执行校验 = §6 断言在链门跑绿（`checked >= 4` 且 `moviesTotal == 4`）。
