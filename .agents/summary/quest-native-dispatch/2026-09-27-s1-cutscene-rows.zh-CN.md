# S1 过场 13 行逐行核对（canonical 重挂落点台账）

> 车道 `quest-native-dispatch`；面：SimpleTalk **S1 单步面的过场轴**（`cutsceneid1` + `cs1_haction`）。
> 本文只读取证 + 台账产出：不改 `src/**`、不跑 Maven、不 commit。
> 取证时间 **2026-09-27 12:49**（12:51 补记了一处工作区新增，见 §7 第 1 条）。工作区处于在飞状态：
> `RetailSimpleTalkDefinitionCompiler.java` 是 **untracked 新文件**（`git status ??`），
> 取证期间 mtime `12:48:43`，实现段行号在数分钟内已漂移过（`328→325`、`996→993`）——
> **引用实现位置时必须带语义锚点**（见 §5.2），不要只抄行号。
> 配套台账：`s1-cutscene-rows.tsv`（13 行，Tab 分隔，本文件 §1 的机器可读形）。

## 0. 结论（一句话）

真端表 13 行与勘察表（`2026-09-27-simpletalk-canonical-survey.zh-CN.md` §2.3）**逐字段零差异**；
13 行里**只有 4056 是作用域键双命中行**（`(205203, QUEST_SELECT)` 在同一 questId 定义内有 2 条边）；
期望落点全部是"**下发对应窗页的那条 `QUEST_SELECT(31)` 边**"——12 行走交付边（`started`），
4056 走接取开窗边（`unaccepted`）；S2 链面另有 4 行（1422/2421/3006/3020），**四行全部 accept==reward 同体**。

## 1. S1 单步面 13 行全表（真端表逐行取证）

数据源：`src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml`
（`<quest_simpletalks>` 块内逐行，`xml_line` = 该行 `<cutsceneid1>` 的物理行号）。
npcId 解析通道 = `RetailNpcNameIndex.resolve(name)`（`name_desc` 精确命中 + `NPC_` 去前缀别名），
13 行 × 2 只 NPC **全部单值命中**（无 0 解、无多解 ⇒ 与 `requireNpc` 的判决一致）。

| # | quest_id | xml_line | movie_id | cs1_haction | 触发侧 | item_check | give_item | 接取名 → npcId | 交付名 → npcId | accept==reward | 双命中 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 3943 | 6027 | 93 | 1009 | 报告 | - | ITEM_DOC_QUEST_3943A | Anteros → 203788 | Fasimedes → 203700 | 否 | 否 |
| 2 | 3946 | 6042 | 94 | 1009 | 报告 | - | ITEM_DOC_QUEST_3946A | Vulcanus → 203790 | Fasimedes → 203700 | 否 | 否 |
| 3 | 3949 | 6057 | 95 | 1009 | 报告 | - | ITEM_DOC_QUEST_3949A | Utisda → 203792 | Fasimedes → 203700 | 否 | 否 |
| 4 | 3952 | 6072 | 96 | 1009 | 报告 | - | ITEM_DOC_QUEST_3952A | Hestia → 203784 | Fasimedes → 203700 | 否 | 否 |
| 5 | 3955 | 6087 | 97 | 1009 | 报告 | - | ITEM_DOC_QUEST_3955A | Diana → 203786 | Fasimedes → 203700 | 否 | 否 |
| 6 | 3958 | 6102 | 98 | 1009 | 报告 | - | ITEM_DOC_QUEST_3958A | Daphnis → 203793 | Fasimedes → 203700 | 否 | 否 |
| 7 | **4056** | 6354 | **403** | **1007** | **接取** | **1** | ITEM_QUEST_4056B | Jackspaner → **205203** | Jackspaner → **205203** | **是** | **是** |
| 8 | 4947 | 7012 | 125 | 1009 | 报告 | - | ITEM_DOC_QUEST_4947A | Logi → 204104 | Vidar → 204052 | 否 | 否 |
| 9 | 4950 | 7027 | 126 | 1009 | 报告 | - | ITEM_DOC_QUEST_4950A | Kinterun → 204106 | Vidar → 204052 | 否 | 否 |
| 10 | 4953 | 7042 | 127 | 1009 | 报告 | - | ITEM_DOC_QUEST_4953A | Lanse → 204108 | Vidar → 204052 | 否 | 否 |
| 11 | 4956 | 7057 | 128 | 1009 | 报告 | - | ITEM_DOC_QUEST_4956A | Lainita → 204100 | Vidar → 204052 | 否 | 否 |
| 12 | 4959 | 7072 | 129 | 1009 | 报告 | - | ITEM_DOC_QUEST_4959A | Honir → 204102 | Vidar → 204052 | 否 | 否 |
| 13 | 4962 | 7087 | 130 | 1009 | 报告 | - | ITEM_DOC_QUEST_4962A | Zyakia → 204110 | Vidar → 204052 | 否 | 否 |

形状佐证（`ALLKEYS` 实测）：13 行的叶子字段只有
`acquired_npc_name / cs1_haction / cutsceneid1 / dev_name / give_item / reward_npc_name`（+4056 的 `item_check`）——
**没有 `talk_npcN`、没有 `remove_itemN`** ⇒ 形状 = 单步（S1），且 `giveItemSymbol` 非空、
`removesItem()=false` ⇒ precheck 的 `grantable` 成立，13 行全部通过过场门
（1009 且 `!itemCheck` / 1007 分支）。

`dev_name` 佐证语义族：3943/3946/3949/3952/3955/3958 与 4947/4950/4953/4956/4959/4962 是
同一组"二转达人·制作名匠"任务（武器/金属防具/首饰/料理/炼金/裁缝六职业 × 两段），
4056 = `해적왕 모자 뺏아오기`（夺回海盗王帽子），13 行全为"任务起始信物 + 过场"型单步行。

### 1.1 npcId 解析的第二见证（独立来源）

`docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv`
（列 `start_npc_ids / end_npc_ids`，取自遗留 compact 任务脚本）逐行与上表一致：

| quest_id | 遗留 start→end | 本表解析 | 一致 |
|---|---|---|---|
| 3943 | 203788 → 203700 | Anteros → Fasimedes | ✓ |
| 3946 | 203790 → 203700 | Vulcanus → Fasimedes | ✓ |
| 3949 | 203792 → 203700 | Utisda → Fasimedes | ✓ |
| 3952 | 203784 → 203700 | Hestia → Fasimedes | ✓ |
| 3955 | 203786 → 203700 | Diana → Fasimedes | ✓ |
| 3958 | 203793 → 203700 | Daphnis → Fasimedes | ✓ |
| 4056 | 205203 → **（空，同体去重）** | Jackspaner → Jackspaner | ✓ |
| 4947 | 204104 → 204052 | Logi → Vidar | ✓ |
| 4950 | 204106 → 204052 | Kinterun → Vidar | ✓ |
| 4953 | 204108 → 204052 | Lanse → Vidar | ✓ |
| 4956 | 204100 → 204052 | Lainita → Vidar | ✓ |
| 4959 | 204102 → 204052 | Honir → Vidar | ✓ |
| 4962 | 204110 → 204052 | Zyakia → Vidar | ✓ |

4056 的 `end_npc_ids` 在登记表里为空、其余 12 行 on-site 单值——这是"同体去重"的旁证，
也说明"4056 同体"不是本表解析的偶然结果，而是遗留脚本与真端表共同的口径。

### 1.2 双命中面（作用域键 `(npcId, actionId)` 的唯一危险行：4056）

canonical 形下单步行的定义图（`canonicalAcceptFlow` + `canonicalDelivery` 常量展开，非运行期实测）：

| 边 | NPC | actionId | source → target | 证据 |
|---|---|---|---|---|
| 接取开窗 | **205203** | QUEST_SELECT(31) | `unaccepted → unaccepted` | `RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow` 第一条 `talk(...)` |
| 交付开窗 | **205203** | QUEST_SELECT(31) | `started → reward` | `RetailSimpleCollectItemDefinitionCompiler.canonicalDelivery`（本行 `collectSource="started"`） |

⇒ 4056 的定义里 `(205203, 31)` **恰有 2 条边**：只按 `(npcId, actionId)` 匹配会产生歧义；
按 `(sourceNode, npcId, actionId)` 匹配则两条各命中 1 条。两条边在 4056 上都真实可达
（接取：Jackspaner 开页 4；交付：Jackspaner 收帽子 → 窗），**不是死边**，所以不能靠"其中一条不可达"淡化。

其余 12 行**无双命中**：接取 NPC（203788/203790/203792/203784/203786/203793/204104/204106/204108/204100/204102/204110）
与交付 NPC（203700/204052）互不相同，且两条 NPC 在各自 questId 内各只承担一个角色。
补充排除项（对 13 行逐行成立，故不构成第二条 `(npc,31)` 命中）：

- `completeFlow` / `npcCompleteFlow` 在交付 NPC 上只登记 `USE_OBJECT` 与 `SELECT_QUEST_REWARD`(1009)（source=`reward`），**不含 QUEST_SELECT**；
- `reportNpcExit` 只登记 `FINISH_DIALOG`(1008)，且 `acquiredNpc == rewardNpc` 时**整条跳过**（4056 即此例）；
- 交互物门边是 `TalkToNpc(objectId)` 无 dialogId（`dialogId != null` 守卫已排除）。

## 2. S2 面：链式 4 行（单列，**不在本轮**）

真端表内链式（有 `talk_npcN`）声明 `cutsceneid1` 的行**恰为 4 行**，与勘察 §3.5 的 id 集一致：

| quest_id | xml_line | movie_id | cs1_haction | 触发页（客户端） | 接取名 → npcId | 交付名 → npcId | accept==reward |
|---|---|---|---|---|---|---|---|
| 1422 | 1042 | 100 | 1353 | SELECT2_1（页 1353） | Memnes → 203912 | Memnes → 203912 | **是** |
| 2421 | 3509 | 132 | 1353 | SELECT2_1 | Asgeirr → 204309 | Asgeirr → 204309 | **是** |
| 3006 | 5282 | 361 | 1694 | SELECT3_1（页 1694） | Shugo_LF2a_1 → 798132 | Shugo_LF2a_1 → 798132 | **是** |
| 3020 | 5329 | 363 | 1007 | ASK_QUEST_ACCEPT（页 4 前的中转） | Ankises → 798143 | Ankises → 798143 | **是** |

电影承载证据（`quest_client_talk_chain_steps.tsv` 的 `MOVIE:` token 全表共 4 处，全在 R 记录 after 列）：

```
3025:1422	R	4	203731	1353	started	started	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT2_1;MOVIE:100:CUTSCENE	NAME:Laokones	SELECT2_1=CLIENT
3501:2421	R	4	204187	1353	started	started	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT2_1;MOVIE:132:CUTSCENE	NAME:Kerupnise	SELECT2_1=CLIENT
3855:3006	R	8	700339	1694	s1	s1	-	-	DIALOG:SHOW_QUEST_PAGE:SELECT3_1;MOVIE:361:CUTSCENE	NAME:LF2A_FOBJ_Q3006	SELECT3_1=CLIENT
3869:3020	R	2	798143	ASK_QUEST_ACCEPT	started	started	-	-	DIALOG:SHOW_QUEST_PAGE:SHOW_ASK_QUEST_ACCEPT_WINDOW;MOVIE:363:CUTSCENE	NAME:Ankises	-
```

S2 待办提醒（本轮只登记，不裁定）：

1. **4 行全部 accept==reward 同体** ⇒ S2 重挂时**每一行都撞 S1 只有 4056 撞的双命中**，
	 作用域键 `(sourceNode, npcId, actionId)` 在 S2 是**必需项**而非保险项；A 策略的"canonical 键集合"必须带 source。
2. 3020 的承载边是 `ASK_QUEST_ACCEPT(1007) started→started` ⇒ canonical 形里对应"接取开窗 QUEST_SELECT"，
	 与 4056 同形（`source=unaccepted`）；1422/2421 的 1353、3006 的 1694 是**页链按钮**，
	 canonical 形下页链退场 ⇒ 落点要按 §3.5 的"对应 briefing/报告 canonical 边"逐行裁定（本台账不预设）。

## 3. 客户端按钮证据（`docs/quest/client-dialog-mapping/`）

### 3.1 页 4（`ask_quest_accept` / `HTML_PAGE_SHOW_ASK_QUEST_ACCEPT_WINDOW`）

- **能力集**（跨全部任务，`quest-dialog-action-details.csv` 全表页 4 共 7809 行）：
	- `HACTION_QUEST_ACCEPT_1`(1002) 3540、`HACTION_QUEST_REFUSE_1`(1003) 3524、
	  `HACTION_FINISH_DIALOG`(1008) 691、`HACTION_QUEST_ACCEPT_SIMPLE`(20000) 27、`HACTION_QUEST_REFUSE_SIMPLE`(20001) 27。
	- 与勘察 §2.3 的 `page-action-map.csv:30-34` 引用**逐字一致**（该行号区间已复核）；
	  与 `canonicalAcceptFlow` 登记的提交/拒绝/关窗族（1002/20000 + 1003/20001 + 1008）严格对应，**无死按钮**。
- **本轮 13 行的逐行登记集**：**只有 `1002 + 1003`**（页 4 上 1008/20000/20001 对这 13 行**没登记**）。
	⇒ 落点论证用的是"页 4 就是 canonical 接取窗页"这一点；13 行的页 4 按钮虽比能力集窄，
	 但 `canonicalAcceptFlow` 的 1008/20000/20001 边是**别的行**的合法能力（且这些边在 canonical 形里
	 本来就会随页下发，属于超集而非缺按钮），不改变落点结论。

### 3.2 12 行（`cs1_haction=1009` 族）的报告页

- 页 `select1`(1011) 登记 `HACTION_ASK_QUEST_ACCEPT`(1007)：12 行**全部命中**；
- 页 `select5`(2375) 登记 `HACTION_SELECT_QUEST_REWARD`(**1009**)：12 行**全部命中**（按钮文本如"拿出推荐信。"）。
- ⇒ 12 行的 1009 触发确系"报告页上交按钮"，其 canonical 同构落点 = **交付窗发放边**
  （页 2375 与页链一起退场，不再由服务端驱动）。

### 3.3 4056 的页链与交付页（唯一 1007 接取侧行）

`QUEST_Q4056.html` 全页（`source_file=QUEST_Q4056.html`，`source_variant=active`）：

| order | 页 | action | 文本 |
|---|---|---|---|
| 1 | select1(1011) | `HACTION_SELECT1_1`(1012) | 继续听。 |
| 2 | select1_1(1012) | `HACTION_ASK_QUEST_ACCEPT`(**1007**) | 点头。 |
| 3 | ask_quest_accept(4) | `HACTION_QUEST_ACCEPT_1`(1002) / `HACTION_QUEST_REFUSE_1`(1003) | 接受。/ 拒绝。 |
| 4 | quest_accept_1(1003) | `HACTION_FINISH_DIALOG`(1008) | 结束对话。 |
| 5 | quest_refuse_1(1004) | `HACTION_FINISH_DIALOG`(1008) | 结束对话。 |
| 6 | select5(2375) | `HACTION_CHECK_USER_HAS_QUEST_ITEM`(**39**) | 将船长帽交给他。 |
| 7 | select6(2716) | `HACTION_FINISH_DIALOG`(1008) | 结束对话。 |

两点关键：

1. 4056 的 1007 按钮挂在 **select1_1(1012)**（不是 1011），且 4056 的 select1 按钮是 `1012`(SELECT1_1)
	——比其余 12 行多一跳。这与 `quest_client_dialog_exits.tsv:1426` 的
	`4056 → SELECT1_1 SELECT6 SELECT5_CHECK` 三登记完全自洽。
	**canonical 形下这一跳随页链退场**，所以"电影跟着 1007 按钮走"在 canonical 形里没有落点，
	 只能跟"开窗边"走（与勘察 §2.3 规则 1 的论证同向）。
2. 4056 的交付侧**没有 1009 按钮**（select5 上是 39，失败页 select6 走 1008）⇒ 4056 的电影
	**不可能**是"报告侧过场"，与 `cs1_haction=1007` 一致；这从客户端侧独立证明了"4056 的电影属于接取侧"。

4056 的交付门（供落点精度参考，非过场落点）：`quest.xml:59914-59929` 同块声明
`quest_work_item1=quest_4056b`（= 接取发放的 `ITEM_QUEST_4056B`）**且** `collect_item1=quest_4056a 1`
（`drop_monster_1=DF2A_TesinonSeamanNMDQ_50_An` 掉落）⇒ `metadata.itemRequirements()` **非空**，
交付边走"整组门"子形（不是工作物品门），`exits` 侧的 SELECT5_CHECK/SELECT6 随页链退场。

### 3.4 S2 4 行的 `cs1_haction` 语义交叉验证（顺带取证）

| quest_id | cs1_haction | 客户端页/按钮 | 结论 |
|---|---|---|---|
| 1422 | 1353 | select2(1352) → `HACTION_SELECT2_1`(1353) | 是页链按钮，非 1007/1009 |
| 2421 | 1353 | select2(1352) → `HACTION_SELECT2_1`(1353) | 同上 |
| 3006 | 1694 | select3(1693) → `HACTION_SELECT3_1`(1694) | 同上 |
| 3020 | 1007 | select1_1(1012) → `HACTION_ASK_QUEST_ACCEPT`(1007) | 与 4056 同轴（接取侧） |

⇒ `cs1_haction` 列确系"客户端按钮 id"，13 行的 1009/1007 与 4 行的 1353/1694/1007 都在客户端
对话表里找得到对应按钮（**无孤值**）。

## 4. 每行期望的规范形落点（规格，与工作区当前实现对照）

`PlayMovie(movieId, CUTSCENE)` 的插入语序：**紧随 `SyncQuestState` 之后、`ShowQuestDialog(窗页)` 之前**
（= 老 craft 行编码序 `Sync → PlayMovie → 开窗`，由 `attachMovieToRoute` 的 after 遍历保证）。

| quest_id | source 节点 | npcId | actionId | 插入位置 | 当前实现锚点 |
|---|---|---|---|---|---|
| 3943/3946/3949/3952/3955/3958 | `started` | 203700 | QUEST_SELECT(31) | `canonicalDelivery` 边：`[Sync(LEVEL_AND_VISIBILITY_REFRESH), ★PlayMovie(93/94/95/96/97/98), ShowQuestDialog(交付窗)]` | `attachMovieToRoute(List.of(canonicalDelivery(...)), "started", rewardNpc, QUEST_SELECT.id(), movie)` |
| 4947/4950/4953/4956/4959/4962 | `started` | 204052 | QUEST_SELECT(31) | 同上（★PlayMovie(125..130)） | 同上 |
| **4056** | **`unaccepted`** | **205203** | QUEST_SELECT(31) | `canonicalAcceptFlow` 边：`[★PlayMovie(403), ShowQuestDialog(页 4)]` | `attachMovieToRoute(canonicalAcceptFlow(acquiredNpc,"started",acceptGiveItemActions(entry)), "unaccepted", acquiredNpc, QUEST_SELECT.id(), movie)` |

- 12 行交付侧的 `movie` 参数由 `entry.cutsceneTrigger() == SELECT_QUEST_REWARD(1009)` 给出；
	4056 接取侧由 `== ASK_QUEST_ACCEPT(1007)` 给出——**两个位点都带 `trigger==` 守卫**，
	 所以同一行不会被两条边各挂一次（这正是"双命中"的第二重保险）。
- fail-closed 要求：过场行的 match 数必须**恰为 1**；0（旧形"无匹配原样返回"）与 >1（键失效）
	都必须当场炸出来。当前实现以 `matches != 1 → IllegalStateException` 满足该要求
	（`attachMovieToRoute` 尾部断言，语义锚点见 §5.2）。
- 本轮 13 行按上表落点即"每行恰 1 挂点"：12 行走交付边（接取边无 1009 守卫 →
	`movieId=-1` 提前返回，不进匹配），4056 走接取边（交付边无 1007 守卫 → 同上）。

## 5. 勘察纠错与口径说明

### 5.1 13 行表：**零纠错**

逐字段比对 `2026-09-27-simpletalk-canonical-survey.zh-CN.md` §2.3 的 13 行表：
`quest_id`（13 个 id 集合完全相同）、`movie`、`cs1_haction`、`item_check`（12 行 `-` + 4056 `1`）、
`give_item`（13 个符号逐字相同）、接取 NPC 名、交付 NPC 名、4056 的"同 NPC"标记——
**无漏行、无多行、无 movie 差异、无触发动作差异**。
§1 的"单步 cutsceneid1 = 13 / cs1_haction = 13"与"链式 cutsceneid1 = 4（1422/2421/3006/3020）"亦实测吻合。

### 5.2 两处**时点/口径**说明（不是勘察错，但直接引用会踩坑）

1. **§2.3 的"现状"段（`:324-331` / `:357-372`）已被工作区超越**：勘察把
	`attachMovie(acceptFlow(...))` / `reportFlow(...)` 记为现状，但取证时工作区的
	`RetailSimpleTalkDefinitionCompiler.java`（untracked 新文件，mtime `09-27 12:48:43`）已经是
	canonical 形：接取段 `attachMovieToRoute(canonicalAcceptFlow(...), "unaccepted", ...)`、
	交付段 `attachMovieToRoute(List.of(canonicalDelivery(...)), "started", ...)`；
	legacy `attachMovie`（`(npc, action)` 无作用域版）与 `reportFlow` / `itemCheckReportFlow`
	已从文件中**删除**（grep 零命中），`acceptFlow` legacy 仍在（仅 `acceptFlowChain` 用，S2 面）。
	⇒ 勘察 §2.3 的"重挂方案"**已被采纳落地**；§7 未决项 2（helper 形态）也已被裁定为独立 helper
	`attachMovieToRoute`。**本文不再视为"待动工"**。语义锚点（行号会漂）：
	`attachMovieToRoute` 定义 / `matches != 1` 断言 / 接取调用点（`"unaccepted"`）/
	交付调用点（`"started"` + `canonicalDelivery`）。
	测试侧同步（12:51:35）：`RetailSimpleTalkGateTest` 补入过场不变量 `CUTSCENE_ROW_FLOOR = 13`
	（定义级 `PlayMovie` 计数 + movieId + 类型），即勘察 §2.3 第 5 点的"回归位"**也已落地**；
	仍缺的只有"4056 的边上落点断言"（§7 第 2 条）。
2. **页 4 的按钮口径**：勘察写"页 4 的按钮集恰为 1002/1003/1008/20000/20001"是**跨任务能力集**
	（`page-action-map.csv` 页级），而**本轮 13 行在页 4 上只登记 1002/1003**（见 §3.1）。
	两种口径都成立、互不冲突，但"13 行页 4 无死按钮"这句话必须按**逐行登记集**说，
	否则会被误读成"这 13 行页 4 上有 20000/20001"。

### 5.3 表外完整性（防止"13 行"被误当全表口径）

`Quest_SimpleTalk.xml` 全表 3152 行里声明 `cutsceneid1` 的共 **67 行**，按 owner × 形状分解：

| owner | 形状 | 行数 | 行 id（@xml_line, movie/haction） |
|---|---|---|---|
| RETAIL_TABLE | single | **13** | 本文件 §1 |
| RETAIL_TABLE | chain | **4** | 本文件 §2（S2 面） |
| XML_RETENTION | single | 40 | 1941-1946(`93-98`/1009)、2931-2936(`125-130`/1009)、9548/9549/9551/9552、11074/11075、18832、19009/19015/19021/19027/19033/19039/19050/19058、28832、29009/29015/29021/29027/29033/29039/29050/29058、30231/30331、16979/26979(`886/887`/**20000**) |
| XML_RETENTION | chain | 7 | 4015(`394`/1353)、18600(`189`/10001)、18806(`803`/1009)、28600(`189`/10002)、28806(`804`/1009)、13800(`827`/**无 cs1_haction**)、23800(`828`/**无 haction**) |
| 不在保留清单 | single | 3 | 9610(`103`/**20000**)、19056(`115`/1009)、29056(`146`/1009) —— **不在生产宇宙（6224），本轮与家族门都无关** |

两点值得留意（本轮不动，仅登记）：

- 全表 `cutsceneid1` 行里 **只有 13800/23800 两行没有 `cs1_haction`**（"无触发"型，勘察 §2.3 的
	precheck 注释也点名了这一形）；反向（有 `cs1_haction` 无 `cutsceneid1`）实测 **0 行**，两轴严格成对。
- `9610/16979/26979` 三行的触发是 `20000`(QUEST_ACCEPT_SIMPLE) —— **不在** precheck 支持的
	`{1007, 1009}` 触发集内；其中 16979/26979 还是 XML_RETENTION 行，不参与本族编译。
	 若后续有人把触发集放宽到 20000，须先补"简单接受"型落点的证据（本台账未取证）。

## 6. 复算命令（只读）

```bash
# ① 13 行 + 4 行 + 全表 67 行分解（owner × 形状 × xml_line）
python3 - <<'PY'
import re, collections
own={}; 
for ln in open('src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv',encoding='utf-8'):
    p=ln.rstrip('\n').split('\t')
    if len(p)>=5 and not ln.startswith('#'): own[int(p[0])]=p[1]
path='src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml'
s=open(path,encoding='utf-8').read()
blocks=re.split(r'<id\s+id="', s[s.index('<quest_simpletalks>'):])[1:]
rows={int(b.split('"')[0]):{k:v.strip() for k,v in re.findall(r'<(\w+)>([^<]*)</\1>', b)} for b in blocks}
cur=None; out=[]
for i,l in enumerate(s.split('\n'),1):
    m=re.match(r'\s*<id\s+id="(\d+)"',l)
    if m: cur=int(m.group(1))
    if '<cutsceneid1>' in l: out.append((i,cur,re.search(r'>(\d+)<',l).group(1)))
chain=lambda d: any(k.startswith('talk_npc') and v for k,v in d.items())
g=collections.defaultdict(list)
for i,q,mv in out: g[(own.get(q,'<none>'),'chain' if chain(rows[q]) else 'single')].append(q)
for k in sorted(g,key=str): print(k, len(g[k]), g[k])
PY

# ② 13 行 npcId 解析（name_desc 精确通道 + 去前缀别名）
#    见 §1 表；模板源 = src/main/resources/aion/data/static_data/npcs/npc_template_*.xml
#    交叉见证 = docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv 的 start/end_npc_ids

# ③ 客户端按钮（页 4 能力集 / 13 行逐行登记 / 4 行 haction 对应页）
python3 - <<'PY'
import csv, collections
c=collections.Counter()
for r in csv.DictReader(open('docs/quest/client-dialog-mapping/quest-dialog-action-details.csv',encoding='utf-8-sig',newline='')):
    if r['page_id']=='4': c[(r['action_constant'],r['action_id'])]+=1
print('page4 capability:', c.most_common())
PY
```

## 7. 缺口盘点（本台账不执行；状态按 2026-09-27 12:51 实测）

1. ~~家族门不变量~~ **已落地（取证期间 12:51:35 由主线补入）**：`RetailSimpleTalkGateTest`
	新增 `acceptedCutsceneRowsCarryExactlyOneRetailMovie`：谓词 = 已受理 ∧ `entry.cutscene()` ∧ 非链行 ∧
	`cs1_haction ∈ {1007, 1009}`，断言**定义内 `PlayMovie` 恰 1 个** + `movieId == 真端 cutsceneid1` +
	`type == CUTSCENE`，并加 `CUTSCENE_ROW_FLOOR = 13` 覆盖下限（谓词失配 = 不变量空转）；
	形状门另在 after 归一化中剔除 `PlayMovie`（"过场重挂会在开窗前插入"）。**13 这个数字在测试侧独立落定**
	（与本文 §1 的 13 行、裁定表 `p0c10n-cutscene-decisions.tsv` 的 13 个 `ADOPT_RETAIL` 三方一致）。
2. **4056 的"落点局部性"断言仍缺**（与第 1 条互补，不是重复）：新不变量是**定义级计数**
	（遍历该定义的全部 transitions 求 `PlayMovie` 总数），因此"电影挂错了边但总数仍为 1"**照样绿**——
	 4056 恰好有两条候选边（`unaccepted` 自环 / `started→reward`），是唯一能构造出这种假绿的行。
	 建议补一条**边上断言**：4056 的 `PlayMovie` 出现在 `sourceNode=unaccepted` 且
	 `event=TalkToNpc(205203, 31)` 的那条转换上，且 `started→reward` 交付边的 after 序列里没有 `PlayMovie`。
	 （这是"作用域键没退化成 `(npc, action)`"的唯一可执行反证。）
3. **S2 前置未动**：4 行链式过场**全部 accept==reward 同体**，S2 的过滤策略 A 的 "canonical 键集合"
	必须带 source 维度，否则 4 部电影会集体错挂（§2 待办 1）。
