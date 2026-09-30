# SimpleTalk S3b/S3c 裁定简报（决策就绪）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 车道：`quest-native-dispatch`；面：SimpleTalk γ（82 行）的 S3b/S3c。角色：**只读研究 + 本文件**。
> 上游证据：`2026-09-27-s3-special-rows.zh-CN.md`（逐行取证）、`2026-09-27-s3-gamma-survey.zh-CN.md`（§5/§6）、
> `s3-gamma-rows.tsv`（82×11）、`2026-09-27-s3-t2-triage.zh-CN.md`。
> 本文件对上游的**两处更正**在 §1-⑸ 与 §1-⑶ 内以「更正」标出（含复算命令口径）。
> 纪律：未跑 Maven / 未编译 / 未跑测试；未启停服务；未改任何生产/测试/TSV；无 commit；无 worktree。

## 0. 一句话结论

γ 的**接取段**只剩 8 行未接管（`1323 2611 3001 3023 21136 24202 80320` + 延期的 `28809`），**交付段**剩 82 行；
两者的第一道闸门不是形状，而是**四条 fail-open 守卫缺口**（上游 §9 的 G-1/G-2/G-3 + 本简报新增的 G-4 阶段门）。
⇒ 切片顺序必须是「守卫加固 → 接取段 → A 类交付段 → **D 类 5 行已带窗的最薄片** → D 类其余 45 行」，
且 D 类整片**阻塞在裁定项 ①**（D 类推荐第 **三** 形 **(iii) 就地加窗**，见 §1-①）。

## 1. 五项必须裁定项

> 引用缩写：`RSTDC` = `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkDefinitionCompiler.java`；
> `RSCIDC` = 同目录 `RetailSimpleCollectItemDefinitionCompiler.java`；`RSHDC` = `RetailSimpleHuntDefinitionCompiler.java`；
> `ChainGate` = `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkChainGateTest.java`；
> `REG` = `src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv`；
> `ACT` = `docs/quest/client-dialog-mapping/quest-dialog-action-details.csv`。

### ① 50 行交付落点形：中间人 `SETPROn` 翻面 ⇒ 搬到 reward NPC（i）/ 留驻换 `31`（ii）/ 就地加窗（iii）

| 项 | 内容 |
|---|---|
| 问题 | D1–D4 的 50 行，`target==reward` 的翻面记录挂在**链中间 NPC** 且动作是 `SETPROn`（无一条 `1009`）：交付边落在哪个 NPC、动作换成什么 |
| 行集 | **50 行**（`s3-gamma-rows.tsv` 的 `delivery_canonicalizable=no` 全域）：1163 1484 1851 2266 2271 2480 2486 2488 2538 2663 2914 2953 2954 2963 3037 3041 3087 3093 3100 4052 4209 4970 4971 4972 4973 4974 4976 11010 11070 11103 11105 11106 11117 11460 13809 19004 21033 21036 21068 21071 21106 21111 21135 21136 21217 21455 23809 29004 30711 30761 |
| 候选 (i) 搬到 reward NPC | 改动面 **4 处**：①`canonicalDelivery(rewardNpc, …)` 调用点沿用（`RSTDC:800`）但要为 R 驱动行**新增调用点**（现仅 `NPC_REPORT` 块循环 `RSTDC:775-811` 内）；②退场作用域必须从 `reportNpcs` 扩到"翻转 NPC"（`RSTDC:441-443` 判据 / `:679-687` 作用域构造）；③中间人页链的 `SELECT_n`/`SELECT_n_1` 是否一并退场需逐行判；④`retiredRoutes` 载荷断言。**风险**：③若保留页链，`SELECT_n_1` 页的**唯一按钮 `SETPROn`（客户端实测：1163 的 `select2_1`(1353) 只有 `HACTION_SETPRO1`，`ACT:32724`）**成为死按钮，且**无自动门禁**（见 §3-R1）；④语义漂移：零售"最后一个中间人收官"变成"跑到交付 NPC 再点" |
| 候选 (ii) 留驻 + 换 `QUEST_SELECT(31)` | 改动面 **3 处**：①同 (i)①②；②**必须同键退场中间人的简报记录**——实测 **47/50** 行的翻转 `(npc, source)` 上已存在 `QUEST_SELECT` 记录（例：`REG:1163 R1 npc=203151 QUEST_SELECT started→started → SELECT2`）⇒ 规范边 `(started, 203151, 31)` 与它**同键**，不退场则被 `explicitRoutes` 反删（`RSTDC:825-835`）⇒ 假绿；余 3 行 `13809/21111/23809` 不同键（其中间入口是 `USE_OBJECT` 而非 `QUEST_SELECT`，见 `REG:13809 R1`/`21111 R3`/`23809 R1`），须逐行核；③须扩 `assertRetiredChainRouteQuiet` 的 conditions 白名单（`RSTDC:466-474`）。**风险**：②把中间人从"两跳页链"降为"点 NPC→31"，`SELECT2/SELECT2_1` 页变 `CLIENT_PAGE_UNREACHED`（非致命，与 `P0c-35` 对 24202 的同类接受口径一致） |
| 候选 (iii) 就地加窗形 | **保留**中间人 NPC 上既有的 `SETPROn(k→reward)` 键/简报页链（`SELECT_n`/`SELECT_n_1` 与其 `SETPROn` 按钮**逐字保留**——客户端真机路径本就是"按 `SETPROn` 按钮交任务"），只把翻面记录的 `after` 追加档位奖励窗，并按 S2 口径退场 reward NPC 侧的报告页（`SELECT5` 下发）。改动面 **2 处**：①新增"R 驱动交付段加窗"后处理（沿用 `RSTDC:759-761` 的 R 回放通道，改 `afterCommit`，2 处 helper）；②报告页退场面 = 实测 **39 条 / 39 行**（每行恰 1 条；动作 `QUEST_SELECT`(31)×20 + `USE_OBJECT`(-1)×19；源节点 `reward`×35 + `started`×4），判据复用现有 `chainPushedPages ⊂ CHAIN_RETIRED_PAGES`（`RSTDC:441-443`）**零新代码**。**与 (i)/(ii) 的差异**：不动落点、不动动作、不动页链 ⇒ 零死按钮、零 `CLIENT_PAGE_UNREACHED`、不需"简报记录同键退场"。**风险**：该行交付段**永不收敛到 S1/S2 规范形**（家族内并存第二种交付形，`canonicalDelivery`/零残留守卫均覆盖不到 ⇒ 50 行成为守卫盲区，只能靠新增禁令式不变量兜底）；且 37 条翻面 after 以 `CLOSE` 收尾（见下方实测）⇒ 追加窗必须同时裁定 terminal `CLOSE` 的处置，否则窗被立即关闭 |
| **推荐** | **①(iii) 就地加窗**（对 5 行"翻转边已带窗"先行；lane owner 若以"家族收敛"为最高优先则改 ②(ii)）——一句话理由：(iii) 是唯一在"客户端真机路径 / 死按钮 / 页可达性 / 门与载荷守恒"四维度上**零新增风险**的候选。逐条：**(a) 客户端忠实度**——`SETPROn` 是**真实页按钮**（`ACT:1163` 第 6 条：`select2_1`(1353) 唯一按钮 `HACTION_SETPRO1`），登记表的 `RETAIL_MATCH`/`SELECT2=CLIENT` 列即客户端背书，真端声明的翻面就是"按 `SETPROn` 按钮"；(ii) 把它换成另一条事件通道（`31` = NPC 任务列表行选择，`CM_DIALOG_SELECT.java:99-103` 要求 `lastPage==SELECT_QUEST(10)`）并让简报页变 `CLIENT_PAGE_UNREACHED`（简报正文对玩家消失 = 玩法回归）。**(b) 改动面**——50 行的翻面记录**全部已 `target==reward`**（构造性成立），(iii) 只改该记录的 `after`（追加档位窗）+ 报告页退场（39 条，见下）；(ii) 还要额外退场简报页链并承受同键反删风险（§3-R7）。**(c) ④/R8 消失**——D 类 **13 行**带阶段门翻面（11 `VAR_IS` + 2 `VAR_AT_LEAST`），(iii) **原样保留记录自身 conditions** ⇒ 不需要"第三种门形"、不存在阶段门未被锚点蕴含的问题（§1-⑤(a) 的 i/ii 两难只在"合成新交付边"的前提下成立）。**(d) R5/G-1 消失**——D 类 **11 行**的翻面带 `REMOVE_ITEM`/`GIVE_ITEM`（如 `REG:2953 204071 SETPRO1 act=REMOVE_ITEM:182207039:1`、`REG:21455 799240 SETPRO1 act=GIVE_ITEM:182209515:1;REMOVE_ITEM:182209514:1`），(iii) 下这些翻面**不退场**，扣物/发物原样保留（注意：团队举例的 `1323 R11` **属 A 类不属 D 类 50 行**，结论对但例证错） |
| 判定所需证据 | ①**必须补**：50 行中"翻转 NPC 是否在 `talk_npcN` 集内"的逐行分类（D3 的 `30711/30761` 翻转 NPC `730701` **不在** `talk_npcN`，见 `s3-gamma-survey` §5 表）——`REG` 与 `Quest_SimpleTalk.xml` 的 `talk_npcN` 对拍即可；②客户端页表逐行核 `SELECT_n_1` 页按钮集（`ACT`，1163 已核）；③(iii) 额外须核：每行"`after` 追加窗后恰有 1 条窗口下发边"与 terminal `CLOSE` 的次序裁定（见 §3-R11） |

**50 行实测分布**（本简报独立复算 `REG`；与团队口径的差异已逐条标注，**未采用未复现的数字**）：

| 项 | 本简报复算值 | 与团队口径 |
|---|---|---|
| 阻断性 `SETPRO*` 翻面记录数 | **57 条 / 50 行** | — |
| 翻面动作记录分布 | `SETPRO1×25 / SETPRO3×13 / SETPRO2×12 / SELECT_QUEST_REWARD×5 / USE_OBJECT×2` | **不一致**：团队记 `SETPRO1×21/SETPRO3×12/SETPRO2×7/SELECT_QUEST_REWARD×2`（差 4/1/5/3，且漏 `USE_OBJECT×2`） |
| 翻面 `after` 尾串分布 | `SYNC:LEVEL_AND_VISIBILITY_REFRESH;CLOSE`×**35** / `…;DIALOG:SHOW_SELECTION_PAGE:SELECT_QUEST`×**13** / `…;SHOW_SELECT_QUEST_REWARD_WINDOW1`×**7** / `SYNC:PACKET_ONLY;CLOSE`×**2** | 团队记"主干 = `…;CLOSE`（含 SELECT_QUEST 变体）" ✅ 定性一致 |
| 翻转边自身已带奖励窗 | **5 行**：4209 / 21033 / 21455 / 30711 / 30761 | **不一致**：团队列 7 行（含 4970 / 21217） |
| 任意 R 记录带奖励窗 | **7 行**：4209 / **4970** / 21033 / **21217** / 21455 / 30711 / 30761 | ✅ 与团队 7 行**逐行相同**（差异仅在口径：`4970`/`21217` 的窗挂在 `reward→reward` 的 `SELECT_QUEST_REWARD` 记录上（`REG:4970 798393 SELECT_QUEST_REWARD reward→reward → window1`），其**翻面边仍以 `CLOSE` 收尾** ⇒ 交任务当次不开窗，须再对话一次） |
| 带条件的翻面 | **17 行**（不是 16 行）：`VAR_IS:var0=0`×**9 行** / `VAR_IS:var0=1`×**2 行** / `VAR_AT_LEAST:var0=1`×**2 行** / `HAS_ITEM`×**4 行**（2953 / 4209 / 21033 / 21455） | **不一致**：团队记"16 行，其中 14 行 `VAR_IS:var0=0`、2 行 `HAS_ITEM`" |
| 无任何 `NPC_COMPLETE` 块 | **1 行：4970** | ✅ 一致 |
| `NPC_COMPLETE` 块的 `source` 取值 | 恒为 **`reward`**（49 个块，去重后仅此一值） | 新增（团队未记，但为 §3-R11 的关键前提） |
| 带 `SELECT5_CHECK`/`_SIMPLE` 退出的行 | **0** | 新增 ⇒ `RSTDC:888-908` 的 SELECT5_CHECK 闭包对 D 类恒不触发 |
| 有 `SELECT5` 页下发的行 | **39** | ✅ **与团队"39 行"一致** |
| 有 `1009` 对（报告页 + 翻面同源）的行 | **4 行**：4209 / 21033 / 30711 / 30761 | **不一致**：团队记"39 行 = 纯 `SELECT5` 页下发（无 `1009` 对、无 `I` 记录）" ⇒ "无 `I` 记录" ✅ 一致（D 类 0 行有 `I` 记录），"无 `1009` 对" ❌ 有 4 行例外 |
| 翻面带 `REMOVE_ITEM`/`GIVE_ITEM` 的行 | **11 行**（`2538 2953 3093 3100 4052 4209 11070 11105 11106 21033 21455` 带 `REMOVE_ITEM`；`GIVE_ITEM` 另有多行） | 新增（团队仅举 `1323 R11`，**该行属 A 类不属 D 类 50 行**） |

**报告页退场的可导出规则（(iii) 下的核验结论）**

候选规则（团队提出）："**当且仅当**该行存在 `NPC_COMPLETE` 块（= reward 态重开载体）时退场 `SELECT5` 报告页；**无完成块的 11 行**保留之" ⇒ **核验结论：不成立 / 无对象**：
- 实测 D 类**无 `SELECT5` 页下发 = 11 行**，逐行与团队列名**完全一致**（2266 2271 2663 2914 2954 3037 3041 3087 11460 19004 21455）；但这 11 行**全部有** `NPC_COMPLETE` 块；
- D 类**无** `NPC_COMPLETE` 的只有 **4970**，而 `4970` **有** `SELECT5` 页下发（`REG:4970 R3 USE_OBJECT reward→reward → SELECT5`）⇒ "无完成块的 11 行"这个集合在 D 类**不存在**，候选规则的前提集与退场对象集**互不重合**。

**本简报给出的可导出规则 R-REP（已逐行核验）**：退场对象 = **`after` 下发 `SELECT5`/`SELECT6` 页的记录**
（判据 = `chainPushedPages(route.afterCommits()) ∩ {SELECT5, SELECT6} ≠ ∅`，与 `RSTDC:441-443` 的页判据同源），
**不附加 `NPC_COMPLETE` 条件**：
- 实测 **39 条 / 39 行**（每行恰 1 条），退场后 **39/39 行仍有 REWARD 态重开载体**：38 行经 `NPC_COMPLETE` 预览边（`RSTDC:1472-1483`），`4970` 经在册 `REG:4970 R4 SELECT_QUEST_REWARD reward→reward → window1` ⇒ **无洞**（同时回答 §3-R4/G-3）；
- **实现红线**：判据**不得**改用 `CHAIN_DELIVERY_RETIRED` 动作词表——该词表含 `SELECT_QUEST_REWARD`（`RSTDC:416-418`），而 11 个无 `NPC_COMPLETE` 行的 reward 态重开载体**正是** `SELECT_QUEST_REWARD reward→reward`（见 §3-R4），用动作词表会连带退场 ⇒ 死档奖励窗。
- 该规则对**第 3 片（A 类）同样适用**（A 类的报告页退场同理只认页下发，不认动作词表）。

### ② `28809` 的越界 `selectionSources`（`unaccepted s0 s1 s2`；接取 NPC == 交付 NPC `830169`）

| 项 | 内容 |
|---|---|
| 问题 | `selectionSources` 声明了段外的 `s1`/`s2` 关窗出口：接受"丢 `s1`/`s2`、由 `DialogService` 兜底"，还是补合成出口 |
| 行集 | **1 行**（`28809`）。当前状态 = **已延期**：`RSTDC:552-563` 的 `acceptSourcesWithinSegment` 返回 false ⇒ `canonicalAccept=false` ⇒ 整行 legacy 逐字；`ChainGate:65` 已登记 `ACCEPT_DEFERRED_ROWS = Set.of(28809)`，`:439` 断言该集合逐行命中 |
| 候选 A 维持延期 | 0 改动。代价：γ 接取段"全覆盖"目标留 1 个洞 |
| 候选 B 补合成出口 | 逐字复用 `acceptFlowChain:1050-1057` 形状补 `selectionSources \ {unaccepted, target}` 的 `FINISH_DIALOG`（≈10 行），并把 `assertCanonicalSelectionSources`（`RSTDC:570-581`）从"越界即抛"改为"越界 ⇒ 必须由补充出口覆盖"。风险：新增 2 条与段内出口**同形但一个留一个丢**的不对称 |
| 候选 C 放行 + 行级例外 | 改 `assertCanonicalSelectionSources` 为放行并在 S3 记录留客户端页表证据。**风险**：依赖客户端页表这一编译器本不持有的知识 |
| **推荐** | **C（=`28809` 接取段可翻，丢 `s1`/`s2`）**——一句话理由：客户端实测 `28809` 的全页动作集 = `{20000,20001}(select1) / {10000}(select2) / {10001}(select3) / {1009}(select5)`，**无 `1008`**（`ACT:28809` 全 5 条记录）⇒ 四条 `FINISH_DIALOG` 出口**全部无按钮载体**，丢 `s1`/`s2` 不产生死按钮；B 的 +10 行只买到形状对称 |
| 判定所需证据 | `ACT:28809` 的 5 条记录（已核）；`REG:28809 B NPC_START 830169 … unaccepted s0 s1 s2`（已核）；`ChainGate:65/439`（已核） |

### ③ `1323` 的 `USE_OBJECT` 接取变体 + `GIVE_ITEM` 落点；`3001`/`3023`/`21136` 缺 `20000`/`20001` 形

| 项 | 内容 |
|---|---|
| 问题 | (a) `1323` 入口是对象交互 `USE_OBJECT(-1)`（`CONT` = `REG:1323 R5 npc=730032 USE_OBJECT unaccepted→unaccepted act=GIVE_ITEM:182201309:1 → SELECT1`），`canonicalAcceptFlow` 入口固定 `31`；(b) 该 `GIVE_ITEM` 现挂**入口边**（每次点宝箱重发）；(c) `3001/3023/21136` 的拒绝族只有 `1002`/`1003`，canonical 会新增 `20000`/`20001` |
| 行集 | **4 行**：`1323`（a+b）、`3001` `3023` `21136`（c） |
| 候选 (a)-① 保留 R5 + 只翻其后 | 保留对象入口边与 `GIVE_ITEM`，只另合成 `1002/1003/1008` 提交族。改动面最小；风险：入口动作与 canonical 的 `31` 入口**并存**，`1323` 的接取段成为"半规范" |
| 候选 (a)-② 增入口事件参数 | `canonicalAcceptFlow` 增 `USE_OBJECT` 变体（参照 `RSHDC:1127+` 的 `UseItem` 无主形）。改动面：1 个重载；风险：家族内出现第三个接取入口形 |
| 候选 (a)-③ 整行 legacy | 0 改动；γ 接取段再留 1 洞 |
| 候选 (b) 移 `GIVE_ITEM` 到提交边 | `RSHDC:1096-1105` 的 `canonicalAcceptFlow(…, acceptActions)` 三参重载**已现成**，`RSTDC:1296+` 的 `acceptGiveItemActions` 亦可复用。**语义严格更安全**：入口边是 `unaccepted` 自环 ⇒ 每次点宝箱都发；移到 `1002/20000` 后提交即入 `started`，入口边不可达 ⇒ 净发一次 |
| 候选 (c) 接受 inert 增量 | `ACT:3001`/`21136` 的 `ask_quest_accept`(4) 页动作 = `{1002,1003}`（无 `20000/20001`），`1323` 同 ⇒ canonical 的 `20000/20001` 是**客户端不会发出的 inert 边**，不是死按钮 |
| **推荐** | **(a)-② + (b) 移到提交边 + (c) 接受 inert 增量**——一句话理由：`(b)` 有现成通道且修掉"重复发物"；`(c)` 的 3 行增量经客户端页表证伪为 inert；(a) 二选一取决于是否愿意为 1 行新增入口形，若不愿则取 (a)-③（**勿取 (a)-①**：半规范形态会让后续等价性对拍无法归因） |
| 判定所需证据 | `REG:1323 R5/R6/R2/R3/R4`（已核）；`ACT:1323`（`select1`=1007、`ask`=1002/1003，**无 20000/20001**）（已核）；`RSHDC:1085-1105`（已核）；**须补**：`1323` 的对象 `730032` 是否在 `GameServer` 侧被登记为可交互对象（`interactionObjects`，`RSTDC:336-354` 同源通道） |

### ④ `3023` 的 `SELECT3_1`/`SELECT3_1_1` 页梯（`unaccepted` 源）

| 项 | 内容 |
|---|---|
| 问题 | `3023` 有 4 条 `SELECT3_1`/`SELECT3_1_1` 记录：`unaccepted` 源 2 条（`REG:3023 R17/R18`）+ `v1` 源 2 条（`R22/R24`）。退场还是保留 |
| 行集 | **1 行**（`3023`）；但同形记录在 `3001` 亦有（`REG:3001 R20 SELECT3_1 v1→v1`），是**同一条改动的作用面** |
| 候选 退场（词表扩容） | **必须源限**：`retiredChainRoute`（`RSTDC:429-444`）按 `npc ∈ acceptNpcs ∧ action ∈ 词表` 匹配，**不看 source** ⇒ 把 `SELECT3_1` 裸加进 `CHAIN_ACCEPT_RETIRED` 会**连带退掉 `v1` 源的 `R22/R24`**，使 `select3`(1693) 页唯一按钮 `1694` 变死按钮（`ACT:3023` 第 7 条：`select3` 只有 `HACTION_SELECT3_1`）。正确形 = 复用 `RSTDC:438` 的 `SETPRO1 && source=="unaccepted"` 先例，加 `SELECT3_1/SELECT3_1_1 && source=="unaccepted"` |
| 候选 保留 | 0 改动；风险：留下 2 条**客户端不可达**的页下发行（`unaccepted` 态服务端只下发 `select1`(1011)，其唯一按钮是 `1007`（`ACT:3023` 第 1 条）⇒ `1694/1695` 无入路），零残差守卫需登记例外 |
| **推荐** | **退场 + 源限**——一句话理由：这 2 条在 `unaccepted` 态**无客户端入路**（页图证据：`select1→1007→页4`，`select3` 族只在 `v1` 可达），退场零行为变化；源限是**必须**的，否则误伤 `3001/3023` 的中段页梯 |
| 判定所需证据 | `ACT:3023` 第 1/7/8/9 条（已核）；`REG:3023 R17/R18/R22/R24`、`REG:3001 R20`（已核）；`RSTDC:429-444`（已核） |

### ⑤ 交付门形 + 报告页/`1009` 配对退场 + 层 A

| 项 | 内容 |
|---|---|
| 问题 | (a) 真端阶段门（`VAR_IS`/`VAR_AT_LEAST`）与 `canonicalDelivery` 的物品门不同构 ⇒ 是否有"第三种门形"；(b) 报告页与 `1009` 的配对退场口径；(c) 层 A（键覆盖过滤，`R 记录退场`）的实装范围 |
| 行集 | (a) **25 行**有阶段门翻面：2266 2271 2663 2914 2954 3001 3023 3037 3041 3087 11460 21136 21217 30711 30761 35010 35018 35024 45010 45011 45017 45018 45024 45025 45026；(b) **31 行**有 `pre-reward` 的 `31/1009` 翻面、其中 **30 行**存在同 `(npc, source)` 的 `QUEST_SELECT` 报告页记录（唯一例外 `21455`）；(c) 键冲突面 = 接取侧 **138 条/19 行**、交付侧 **19 条/19 行**（`s3-gamma-survey` §3.4/§4.3） |
| (a) 候选 i 引入条件通道 | 把 `QuestVariableIs` 塞进 `canonicalDelivery` 第一参（签名类型是 `List<QuestCondition>`，`RSCIDC:473-474`，形参名 `hasItems` 不构成约束）。风险：调用点写成"把 `var0` 门当物品门"的误导代码 |
| (a) 候选 ii 节点锚定 | 靠 `QuestMutationPlanner.matchesSourceNode`（`:338-350`，要求快照解包变量 **== 源节点投影**）让锚点节点蕴含门值，边上不写条件。**更正上游**：上游称"锚点不蕴含的行 = 0"，**实测 9 条边不被蕴含**——`30711/30761`（`VAR_AT_LEAST:var0=1 @ started`）与 `35010/35018/35024(×2)/45011/45025(×2)`（`VAR_IS:var0=1 @ started`），其 `N started START var0=-`（`REG:35010 N started START var0=-`）⇒ 锚点**不**蕴含门值。**这 6 行必须走条件通道**（余 20 条锚定边可走锚定） |
| **推荐** | **(a) ii 为主 + 6 行例外走 i**——一句话理由：锚定对 20/29 条边零冗余；9 条例外里 6 条（`35010/35018/35024/45011/45025`）是 `SET_VAR` 阶段链驱动的真端门，**丢门会变成"简报前即可领奖"的 premature 且审计看不见**（`QuestPrematureRewardRouteAudit` 的候选要求"客户端页含该 dialogId"，而 canonical 边动作是 `31`，`31` 在客户端页动作集 0 命中）⇒ 必须靠链门新增"退场阶段门 ⊆ 规范边条件"的蕴含断言兜底 |
| **(a) 的作用域限定** | **若 ① 取 (iii)，则 (a) 对 D 类 50 行不适用**：D 类 50 行里带阶段门翻面的 **13 行**（11 `VAR_IS` + 2 `VAR_AT_LEAST`）在 (iii) 下记录自身 conditions **原样保留** ⇒ 既不需要"第三种门形"，也不存在 §3-R8（阶段门未被锚点蕴含）。**(a) 的 i/ii 两难只对"合成新交付边的行"成立**——即第 3 片（A 类 30 行，其中 6 行是 `started` 锚点不蕴含的 `VAR_IS:var0=1`）与第 2 片（接取段合成） |
| **推荐** | **(b) 按"页下发即退场"配对退**——机制已现成：`RSTDC:441-443` 的 `chainPushedPages(route.afterCommits()) ⊂ CHAIN_RETIRED_PAGES` 会连带退掉报告页记录（`CHAIN_RETIRED_PAGES` 含 `SELECT5/SELECT6`），30/31 行的配对**无需新代码**；`21455` 的例外须逐行核 |
| **推荐** | **(c) 必须实装层 A**——理由：不实装则 `explicitRoutes`（`RSTDC:825-835`）用同键 R 记录**反向删除** canonical 块边 ⇒ 形状回退旧形而门禁全绿（假绿）。作用域 = `RSTDC:825-835` 的键集构造改为"R 记录键 ∈ canonical 边键 ⇒ 该 R 不回放、不进 `explicitRoutes`" |
| 判定所需证据 | `RSCIDC:473-479`（签名）、`QuestMutationPlanner:338-350`（锚定语义）、`REG:35010/35018/35024/45011/45025/30711/30761` 的 `N started START var0=-` 与 `R16/R3` 门（均已核，本简报独立复算）；`QuestPrematureRewardRouteAudit` 候选判据（已核）；`ACT` 全表 `HACTION_QUEST_SELECT` **0 命中**（已核） |

## 2. 切片建议（顺序即推荐执行序）

> 每片「指纹漂移面」= 该片必须经 `-Dretail.talkChain.fingerprintOut` 重冻的行集（`ChainGate:143-189`），
> 也是"变化面 == 缺陷面"判据的分母。回滚面 = 该片撤回后逐行退回 legacy 逐字形状的行集。

| # | 切片 | 行集 | 依赖裁定 | 指纹漂移面 | 最小门禁集合 | 回滚面 |
|---|---|---|---|---|---|---|
| **1** | **守卫加固 + 判据**（零形状变化） | **0 行** | ⑤(a)(c)、① 的 (iii) 作用域判据与 (iii) 的 R-REP 红线 | **∅**（必须为空） | `RetailSimpleTalkChainGateTest` 新增 4 条不变量（G-1 动作侧载荷断言 / G-2 去 `"started"` 限定 / G-3 `reward` 态重开载体 / G-4 阶段门蕴含）+ 家族门 `RetailSimpleTalkGateTest` + T2(82) + T3 | ∅（无形状变更） |
| **2** | **接取段 R 驱动合成** | **8 行**：`1323 2611 3001 3023 21136 24202 80320 28809` | ②、③、④ | 8 行 | 链门（`CANONICAL_ACCEPT_ROW_FLOOR` 由 55 提到 **68**、`ACCEPT_DEFERRED_ROWS` 清空为 `Set.of()`、`ChainGate:327` 形状不变量）+ 家族门 + T2 + T3 + 契约门（`QuestPrematureRewardRouteAudit` 须仍空） | 8 行接取段退回 legacy 逐字 |
| **3** | **交付段 A 类同构替换**（非 D 类） | **30 行**：`1118 1323 1394 2553 2646 3001 3023 3218 3966 4218 21004 21065 28809 80752`（14 非系统 A 类）+ `35010 35011 35017 35018 35024 35025 35026 45010 45011 45017 45018 45024 45025 45026`（14 系统发放）+ `2611`（`NPC_REPORT` 块交付，与第 2 片只重叠接取段） | ⑤(a)(b)(c) | 30 行 | 链门指纹重冻 + 零残留 `RSTDC:620-640` + 家族门 + T2 + T3 + 契约门 | 30 行交付段退回 legacy |
| **4a** | **交付段 D 类·最薄子切片（翻转边已带窗者）** | **5 行**：`4209 21033 21455 30711 30761`（翻转边 `after` 已含 `SHOW_SELECT_QUEST_REWARD_WINDOW1` ⇒ **已是 canonical 形**） | ①、(iii) + ⑤(b) | 5 行 | 链门指纹重冻 + 零残留 + 家族门 + T2 + T3 + 契约门 | 5 行（只回滚报告页退场） |
| **4** | **交付段 D 类**（其余中间人翻面） | **50 行**（§1-① 行集；本片按 (iii) 处理，与 4a 的 5 行**重叠**——4a 先落，4 片只处理余下 45 行 + 复验 5 行） | **①**（推荐 (iii)）；**(iii) 下 ⑤(a) 不适用**、⑤(c) 不适用（不合成新边 ⇒ 无层 A 面）；须先落 §3-R11 的窗口/`CLOSE` 次序裁定 | 50 行 | 链门 + 家族门 + T2 + T3 + 契约门；**额外**：`SELECT_n_1` 页按钮集的人工对拍（§3-R1 无门禁）+ "每行恰 1 条窗口下发边且窗口在 terminal `CLOSE` 之后"断言（§3-R11）+ R-REP 断言（39 条退场对象、判据用页下发而**非**动作词表） | 50 行交付段退回 legacy |

> **第 3 片与第 4 片的判据分界**：**第 3 片** = 翻面**已在 reward NPC** 且动作是 `1009`/`SELECT_QUEST_REWARD` 形 ⇒ 与既有 `canonicalDelivery`（`RSCIDC:473-479`）**同构替换**，`source` 取翻面记录的源节点；
> **第 4 片** = 翻面**在链中间 NPC** 且动作是 `SETPROn` ⇒ 按 ①(iii) **就地加窗**（不合成 reward NPC 侧新边）。
> 两片的行集互斥（`s3-gamma-rows.tsv` 的 `delivery_canonicalizable` 列即此分界），重叠只发生在"第 2 片的 `2611` 接取段"与"mixed 行 `4209/21033/21455/30711/30761` 的 reward NPC 侧翻面"两处，均已在行集列注明。

**顺序理由**：第 1 片不改形状 ⇒ 可用"285 行全绿"证明新守卫不误伤，把守卫风险与形状风险解耦；
第 2 片规模最小（8 行）且已有 `ChainGate:327` 的形状不变量骨架；
第 3 片 30 行全部命中"翻转已在 reward NPC"，与 S2 已落地的机制同构（替换成本最低）；
第 4 片规模最大且**唯一依赖一个未裁定项**，必须最后且单独回滚。

**第 4 片的前置增量**（不在上表门禁内，须单列）：`s3-gamma-rows.tsv` 的 D 类里 **5 行是 mixed**
（`4209 21033 21455 30711 30761`，reward NPC 侧另有翻面）⇒ 第 4 片开工前必须对这 5 行单独出"保留哪一条/是否合并"的裁定。
其中 `30711/30761` 的中间人 `R5 SETPRO1(730701) started→reward cond=- act=-` 是**无门直翻**，而客户端 `select2_1`(1353)
确实带 `HACTION_SETPRO1(10000)`（`ACT:30711` 第 4 条）⇒ 该边是**客户端可见的 premature 领奖边**（今日未被
`QuestPrematureRewardRouteAudit` 捕获，因该审计只报"同一 `(source,npc,target)` 下的多选"，单条不报）。

**(iii) 下这条边的处置（必须显式裁定，不是缺陷修复）**：(iii) **保留** `SETPROn` 键 ⇒ 该 premature 边在 (iii) 下
**原样存活**。因此第 4 片不是一个隐含的缺陷修复——它**显式接受**"服务端保留一条客户端可见的直接领奖边"这一现状
（今日已如此，非新增）。若 lane owner 认为该边必须修（加门或退场），那是**与 ① 并列的独立缺陷修复裁定**：
`30711/30761` 需按 `N reward REWARD var0=2`（`REG:30711`）为 `R5` 补 `VAR_AT_LEAST:var0=1` 门（与同行的
`R3 USE_OBJECT(804870) cond=VAR_AT_LEAST:var0=1` 同轴），该改动**只影响这 2 行**，可独立成片。

## 3. 风险清单（若裁定错误 ⇒ 可观测红 + 捕获者）

| # | 风险 | 可观测形态 | 捕获者 |
|---|---|---|---|
| **R1** | 中间人页链保留而 `SETPROn` 退场（候选 (i) 的 ③ 做错） | `BUTTON_WITHOUT_ROUTE`：`select2_1` 页按钮 `10000` 无路由 | **无自动门禁**。契约门显式剔除 `RetiredQuestIds`（= 全部 `RETAIL_TABLE` 行，`QuestClientContractGateTest:58-62`）；`auditPageButtons` 仅被 `QuestPageButtonAuditTest` 用于临时 XML；零残留守卫只查 `SELECT5/SELECT6` 页与 `started` 源 `39/1009/20002`（`RSTDC:620-640`）⇒ **只能人工对拍 `ACT`** |
| **R2** | canonical 后部分页不再下发 | `CLIENT_PAGE_UNREACHED`（`80320` 的 `select2`(1352) 唯一动作 `1008`；`3023` 的 `select3_1` 族） | **非致命，无门禁**（`P0c-35` 已对 24202 的 1352/1353/1693/1694 接受同类） |
| **R3** | 退场承载边带 `MOVIE` 但未重挂 | 过场静默丢弃 | `ChainGate:462-463`（每行恰 1 条 `PlayMovie` + `movieId`/触发轴，`CHAIN_CUTSCENE_ROW_FLOOR=4`）+ `RSTDC:488-498` 的 MOVIE fail-closed 分支 |
| **R4** | `reward` 态重开窗丢失（G-3） | 领奖后关窗无法重开（**死档奖励窗**）；`NPC_COMPLETE` 缺失的 **11 行**（跨全 82 行）：`2611 3001 3023 4970 35011 35025 35026 45010 45017 45024 80320` | **当前无**（`RSTDC:632-638` 的交付中转判据只查 `"started"` 源）⇒ 须由第 1 片新增不变量。**逐行核验结论（本简报新增）**：这 11 行的 REWARD 态重开载体**全部是在册 R 记录、不是块预览边**，且形状统一 = `USE_OBJECT(-1) reward→reward` + `SELECT_QUEST_REWARD(1009) reward→reward`，两条 `after` 均为 `DIALOG:SHOW_QUEST_PAGE:SHOW_SELECT_QUEST_REWARD_WINDOW1`（唯一例外 `4970` 的 `USE_OBJECT` 腿下发 `SELECT5`，其重开载体是 `REG:4970 R4 SELECT_QUEST_REWARD → window1`）；`35025/45024` 各有两个 reward NPC ⇒ 载体成对出现。**触发点**：`SELECT_QUEST_REWARD ∈ CHAIN_DELIVERY_RETIRED`（`RSTDC:416-418`）⇒ 任何"用动作词表 + 作用域含 reward NPC"的退场都会**同时打掉全部 11 行的重开载体** ⇒ 交付段退场必须改用页下发判据（R-REP） |
| **R5** | 静默丢扣物（G-1） | `1323 R11 SELECT_QUEST_REWARD v1→reward actions=REMOVE_ITEM:182201309:1 cond=-` 退场后动作凭空消失，玩家永久持有任务物品 | **当前无**（`retiredChainGate:505-518` 只收 `HAS_ITEM` **条件**；`canonicalChainHandIn:530` 在 `!itemCheck` 时把 `gate` 置空 ⇒ 覆盖断言空过） |
| **R6** | 交付不可达但全绿（G-2，`24202` 型） | 任务无法交付而全部门禁绿（`failure_page='CLOSE'` 恰好绕开 `SELECT6` 检查） | **当前无**（`RSTDC:579` 的 `"started"` 源限定；`I` 门 source 是 `s2`） |
| **R7** | 层 A 未实装 ⇒ 合成边被同键 R 反删（假绿） | 形状回退旧形、门禁全绿 | 链门形状不变量（`ChainGate:197` / `:327`）+ 指纹对拍（`ChainGate:143-189`） |
| **R8** | 阶段门未被锚点蕴含而未承接（§1-⑤ 的 6 行） | 交付提前可用（`premature`）；**`QuestPrematureRewardRouteAudit` 看不见**（其候选要求客户端页含该 `dialogId`，而 canonical 边动作是 `31`，`31` 在客户端页动作集 0 命中） | **当前无** ⇒ 须由第 1 片的 G-4 蕴含断言兜底 |
| **R9** | 指纹表未随形状重冻 | 链门 `assertEquals(frozenFingerprints, computed)` 红（`ChainGate:187`） | `ChainGate:143-189`（重冻通道 `-Dretail.talkChain.fingerprintOut`） |
| **R10** | 档 3 机械筛盲区：`JournalRewardRowRepairContractTest`（γ 面 id 覆盖第一，21/57，无旧形 token） | T2 红但不在预筛档 1 名单内 | T2 选择器（82 口径 `combined`）+ 实跑红集合分拣；**链门 `RetailSimpleTalkChainGateTest` 必须手工从档 1 名单剔除**（它是门禁不是待改类，见 `s3-t2-triage` §5.3） |
| **R11** | **(iii) 下"奖励窗挂在中间人翻面边"与 `NPC_COMPLETE` 预览边产生双窗**（团队提出的问题） | 三子形态：①同一次客户端动作双推窗；②窗被紧随的 terminal `CLOSE` 立即关掉（**可观测"交任务后没有奖励窗"**，而告警面只有"窗没出现"）；③两扇窗档位不一致（多档行） | **①与③已由本简报实测排除、②是本片的真实缺口**，捕获者须由第 1 片新增"每行恰 1 条窗口下发边且窗口在 terminal `CLOSE` 之前"的断言承担（`ChainGate` 形状不变量） |

**§3-R11 的排除证据（逐条已核）**：

1. **①同动作双推窗 = 可排除**：`NPC_COMPLETE` 块的 `source` **恒为 `reward`**（49 个块去重后仅此一值）；(iii) 加窗的翻面边 `source ∈ {started, s1, k2, stage2, stage1, v1, v2}` ≠ `reward` ⇒ 两者**键不同** ⇒ 不可能在同一次客户端动作里同时命中。进一步的直接核验：把每行 `preview` 段解析出的 dialogId 与块 `source` 组成块边键，与 R 回放键集（`(source, npc, dialogId)`）求交 ⇒ **50 行全部为空集（0 条冲突）**。
2. **`SELECT5_CHECK` 闭包不会额外造第二扇窗**：D 类 **0 行**带 `SELECT5_CHECK`/`SELECT5_CHECK_SIMPLE` 退出 ⇒ `RSTDC:888-908` 对 D 类恒不触发。
3. **③两窗档位不一致 = 本片 0 行、但须锁**：预览边用 `rewardWindowForTier(completeRewardIndex)`（`RSTDC:1474-1482`），(iii) 加窗须用 `deliveryWindowPage(metadata, questId)`（`RSCIDC` 同源），γ 82 行奖励组 `maxSlot` 全为 1（`s3-gamma-survey` §2.1）⇒ 两者同值；该等价性必须**行级断言锁住**，否则多档行会双窗不同档（非本片触发，属回归防护）。
4. **②terminal `CLOSE` 是本片唯一真实缺口**：57 条翻面记录里 **37 条以 `CLOSE` 收尾**（35 条 `…;CLOSE` + 2 条 `SYNC:PACKET_ONLY;CLOSE`）⇒ 若"追加窗"写成 `after + window`，则序列为 `Sync(LVR) → CloseDialog → ShowQuestDialog(窗)`；若写成 `after + window` 在 `CLOSE` 之前插入或替换 terminal `CLOSE`，才与 canonical 形 `[Sync(LVR), ShowQuestDialog(窗)]`（`RSCIDC:473-479`）等价。**裁定项**：本片取"**替换 terminal `CLOSE`**"，并要求逐行断言 after 尾串 ∈ {`…;DIALOG:SHOW_QUEST_PAGE:SHOW_SELECT_QUEST_REWARD_WINDOW1`}（即退场后所有 D 行的翻面 after 尾串唯一）。

## 4. 证据索引

| 结论 | 出处 |
|---|---|
| γ = 82 行；A 类 31 行；D 类 50 行；系统发放 14 行 | `s3-gamma-rows.tsv`（11 列 × 82 行，`delivery_canonicalizable` 列）+ `s3-gamma-survey` §1/§4.1 |
| 47/50 行翻转 `(npc,source)` 已有同键 `QUEST_SELECT` 记录 | 本简报独立复算 `REG`（口径：`target==reward ∧ source!=reward` 的 R 记录 vs 同 `(npc, source)` 的 `QUEST_SELECT` 记录） |
| 50 行翻面分布：57 条记录 / 动作 `SETPRO1×25 SETPRO3×13 SETPRO2×12 SELECT_QUEST_REWARD×5 USE_OBJECT×2` | 本简报独立复算 `REG`（`target==reward ∧ source!=reward`，含 mixed 行的 reward NPC 侧翻面） |
| 翻面 `after` 尾串：`…CLOSE`×35 / `…SELECT_QUEST`×13 / `…WINDOW1`×7 / `SYNC:PACKET_ONLY;CLOSE`×2 | 本简报独立复算 `REG` 的 after 列 |
| 翻转边已带窗 = 5 行；任意 R 记录带窗 = 7 行 | 本简报独立复算；`4970`/`21217` 的窗在 `reward→reward` 的 `SELECT_QUEST_REWARD` 记录上（`REG:4970 798393` / `REG:21217 799226`） |
| 带条件翻面 17 行（`VAR_IS:var0=0`×9 / `VAR_IS:var0=1`×2 / `VAR_AT_LEAST`×2 / `HAS_ITEM`×4） | 本简报独立复算 `REG` 的 conditions 列 |
| `NPC_COMPLETE` 块 `source` 恒为 `reward` | 本简报独立复算 `REG` 的 B 记录（49 个 `NPC_COMPLETE` 块） |
| D 类 0 行带 `SELECT5_CHECK`/`_SIMPLE` 退出 | 本简报独立复算 `quest_client_dialog_exits.tsv` |
| 预览块边键 ∩ R 回放键集 = ∅（50 行） | 本简报独立复算（`REG` 的 `preview` 段 × `(source, npc, dialogId)`） |
| 预览边/档位窗的合成规则 | `RSTDC:1472-1483`（`rewardWindowForTier(completeRewardIndex)`） |
| D 类报告页退场对象 = **39 条 / 39 行**（`QUEST_SELECT`×20 + `USE_OBJECT`×19；`reward`×35 + `started`×4） | 本简报独立复算 `REG`（判据 `chainPushedPages ∩ {SELECT5,SELECT6}`） |
| D 类有 `1009` 对 = **4 行**（4209/21033/30711/30761）；D 类有 `I` 记录 = **0 行** | 本简报独立复算 `REG` |
| D 类翻面带 `REMOVE_ITEM` = **11 行** | 本简报独立复算 `REG` 的 actions 列 |
| 无 `NPC_COMPLETE` 的 11 行，其 reward 态重开载体 = 在册 `USE_OBJECT`+`SELECT_QUEST_REWARD reward→reward → window1` | 本简报独立复算 `REG` 的 `reward→reward` 记录（`2611 3001 3023 4970 35011 35025 35026 45010 45017 45024 80320`） |
| `CHAIN_DELIVERY_RETIRED` 动作词表 | `RSTDC:416-418`（含 `SELECT_QUEST_REWARD`/`CHECK_*`/`SELECT5`/`SELECT6`） |
| `31` = NPC 任务列表行选择，非页按钮 | `CM_DIALOG_SELECT.java:90-103`（`isNpcQuestRowSelection` 要求 `lastPage==SELECT_QUEST(10)`）；`page-action-map.csv` 中 `HACTION_QUEST_SELECT` 命中 **0**；`docs/quest/WRITING_GUIDE.md:330`（`QUEST_SELECT(31)`=查看任务信息） |
| `1163` 的 `select2_1`(1353) 唯一按钮 = `SETPRO1` | `ACT:1163` 第 6 条（csv 行 32724） |
| `28809` 全页动作集无 `1008` | `ACT:28809` 全 5 条记录 |
| `3001/21136/1323` 的 `ask_quest_accept`(4) 只有 `1002/1003` | `ACT:3001`/`ACT:21136`/`ACT:1323` 对应行 |
| `3023` 的 `select3→1694→select3_1→1695→select3_1_1(10001)` | `ACT:3023` 第 7/8/9 条 |
| `80320` 的 `select2`(1352) 唯一动作 `1008` | `ACT:80320` 第 5 条 |
| 25 行阶段门；其中 **9 条边不被锚点蕴含**（更正上游"0 行"） | 本简报复算 `REG` 的 `R` 记录 `VAR_IS`/`VAR_AT_LEAST` × `N <node> <status> var0=`（例：`REG:35010 N started START var0=-` 与 `R16 … cond=VAR_IS:var0=1`） |
| `canonicalDelivery` 第一参类型是通用条件 | `RSCIDC:473-479` |
| 锚定语义（源节点投影 ⊆ 快照） | `QuestMutationPlanner.java:338-350` |
| `canonicalAcceptFlow` 三参重载承接接取动作 | `RSHDC:1096-1105`；调用点 `RSTDC:325-329`、`RSTDC:709-714` |
| 退场判据/作用域/守卫行号 | `RSTDC:429-444`（判据）、`:464-502`（载荷守卫）、`:505-518`（载荷收集）、`:526-541`（链式交付门）、`:552-563`（段内判据）、`:570-581`（selectionSources 守卫）、`:620-640`（零残留）、`:673-687`（段旗标与 `reportNpcs`）、`:816-818`（系统发放 `removeIf`）、`:825-835`（`explicitRoutes` 键覆盖） |
| 层 A 的定义与"未实装 ⇒ 假绿" | `2026-09-27-s2-buildchain-spec.zh-CN.md:193`、`:344-346`、`:773` |
| 契约门剔除 `RETAIL_TABLE` 行 | `QuestClientContractGateTest.java:58-66` |
| premature 审计的候选判据 | `QuestPrematureRewardRouteAudit.java:79-96`（`:93` 的客户端页动作集判据） |
| 链门下限/延期行/指纹通道 | `ChainGate:58`、`:62`、`:65`、`:143-189`、`:197`、`:327`、`:462-463` |
| S3a 已完成 60 行（`CANONICAL_ACCEPT_ROW_FLOOR=55`） | `ChainGate:62`、`:437-439` |
| T2 选择器口径（82 id）与命令 | `s3-t2-triage` §2（`affected_quest_tests.py --json <82 ids>`） |

**证据不足、需补动作的两处**（不做任何推断）：①§1-① 的"翻转 NPC ∉ `talk_npcN`"逐行分类（`30711/30761` 已确认；
其余 48 行未核）；②§1-③ 的 `1323` 对象 `730032` 的交互物登记状态（须核 `RetailInteractionObjects` 同源判据）。
