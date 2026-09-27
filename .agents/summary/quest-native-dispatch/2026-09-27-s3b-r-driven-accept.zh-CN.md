# SimpleTalk S3b · R 驱动接取段规范形合成（7 行）落地记录

> 车道：`quest-native-dispatch`；面：**SimpleTalk S3b（γ 面第二片：接取段 R 驱动合成）**；角色：生产改造 + 门禁同步 + 证据。
> 上游：`2026-09-27-s3-adjudication-brief.zh-CN.md` §2 第 2 片、`2026-09-27-s3-special-rows.zh-CN.md`（逐行取证 §2/§6/§11/§12）。
> 纪律：未 commit；未新增/退役任何 TSV；未启停服务；T3 在仓库外副本、跑完即删。

## 0. 一句话结论

γ 面 8 行候选里，**有 `QUEST_SELECT` 入口记录且非物件入口的 7 行**（`2611 3001 3023 21136 24202 80320`
+ 裁定放行的 `28809`）接取段换规范形；**指纹变化面逐行等于这 7 行（7/0/0）**，S2 的 203 行与 S3a 的 60 行
零漂移，物件入口行 `1323` 按设计**逐字未动**（留 `USE_OBJECT` 变体）。

## 1. 两个新机制（本片的核心）

### 1.1 层 A：键覆盖过滤（假绿防线）

**问题**：`buildChain` 末尾的 `explicitRoutes` 覆盖（与 `QuestXmlBlockExpander` 同口径）会用**同键**
（`source:npc:dialogId`）的登记记录**反向删除**合成的规范边。R 驱动行恰好带
`(unaccepted, 接取NPC, 31) → SELECT1 入口页下发` 记录 ⇒ 不处理的话 canonical 接取窗边被删、
形状回退旧形，而**门禁全绿**（判例 2611/3001/3023/21136/24202/80320 各 1 条）。

**实装**：`canonicalKeys` = 接取段规范边键（`31`/`1002`/`20000`/拒绝族/关窗出口 × 接取 NPC ×
源 `unaccepted` ∪ 块 target）；回放循环里同键记录**退场**（与策略 A 词表并列为独立子句），
并经 `assertRetiredChainRouteQuiet` 载荷守卫。**实测**：现存已接管行（S2 203 + S3a 60）同键冲突 **0 条**
⇒ 层 A 对已收口面零影响（该面只是 R 驱动行的问题）。

### 1.2 R 驱动接取段合成 + 接取侧载荷覆盖守卫

**实装**：`rDrivenAccept` 谓词 = 非系统发放 ∧ 无 `NPC_START` 块 ∧ `acquired` 唯一解析 ∧ 非交互物 ∧
**登记表有 `(unaccepted, npc, QUEST_SELECT)` 入口记录** ∧ 有 `unaccepted→started` 的
`QUEST_ACCEPT_*` 提交记录 ⇒ 以 `canonicalAcceptFlow(acquiredNpc, "started", acceptActions)` 合成，
`acceptActions` 逐字取**提交记录**上的动作（判例 21136 的 `GIVE_ITEM:182207919:1`）。

**新增守卫**：`assertAcceptPayloadCovered`——退场记录的**动作侧**载荷（`GIVE_ITEM`/`REMOVE_ITEM`）
必须被规范接取边（source=`unaccepted`）的动作覆盖，否则拒绝编译（G-1 的接取侧对偶）。
为此 `assertRetiredChainRouteQuiet` 的动作白名单放行 `GIVE_ITEM:`。

**开发期修正（留痕）**：该守卫首版把所有退场记录都当接取侧 ⇒ 误伤 11 行（1932/3092/3209/3340/3547/
11304/14121/14201/24121/24152/24242）——它们的 `REMOVE_ITEM` 是**交付侧检查对**
（`CHECK_USER_HAS_QUEST_ITEM reward→reward c=HAS_ITEM a=REMOVE_ITEM`，由交付边承接，S2 的
`canonicalChainHandIn` 覆盖断言已兜底）⇒ 修为**按退场侧分流**（`retiredAcceptRoutes` 单独收集）。
教训：同一文件里"接取侧"与"交付侧"的载荷各有承接边，守卫不得跨侧判定。

## 2. 判例 28809：从"派生式延期"到"裁定放行"

S3a 片因 `selectionSources = unaccepted s0 s1 s2` 越界而延期的 28809，本片放行：

- **派生判据**（取代硬编码例外）：段外源仅当**该源上登记表没有 `FINISH_DIALOG` 记录**时放行
  （`servesFinishDialog`）——28809 的段外源 `s1`/`s2` 上登记表无任何关窗记录 ⇒ 无载体 ⇒ 丢出口
  不改变可达行为；有记录的行仍拒绝编译（不得静默丢出口）。
- **客户端证据（裁定依据之二）**：`s3-special` 实测 28809 全页动作集 = `{20000,20001}(select1) /
  {10000}(select2) / {10001}(select3) / {1009}(select5)`，**无 `1008`** ⇒ 旧形里 4 条 FINISH_DIALOG
  出口**本就无按钮载体**（死代码）⇒ 丢 s1/s2 零行为变化。
- 该判据同时驱动**延期谓词**与**守卫**（同源），链门 `ACCEPT_DEFERRED_ROWS` 由 `{28809}` 清空为 `Set.of()`。

## 3. 1323 的延期（派生判据，非硬编码）

1323 的接取入口是 `USE_OBJECT`（宝箱 730032 的自环，`GIVE_ITEM` 挂在自环上）而**无 `QUEST_SELECT`
入口记录** ⇒ 被 `rDrivenAccept` 的入口通道判据排除、逐字保留（留 `USE_OBJECT` 变体，S3b-β）。
注：`RetailQuestUseItemNpcs.isInteractionObject(730032)` 实测为 false（该表不含此物件）⇒ **operative
的判据是"入口记录通道"**，物件表检查作为第二道保险保留。

## 4. 变化面 == 缺陷面

| 判据 | 结果 |
|---|---|
| 指纹重冻（`-Dretail.talkChain.fingerprintOut=/tmp/s3b-fp.tsv`） | **7 / 0 / 0**，逐行 == `{2611 3001 3023 21136 24202 80320 28809}`；编译 problems=[] |
| S2 203 行 / S3a 60 行 | **零漂移** |
| 1323 | 未在变化集（延期成立） |

## 5. 门禁

| 门 | 结果 |
|---|---|
| `RetailSimpleTalkChainGateTest` | **6/6 绿**（S3a 不变量取数面已扩到 R 驱动行、延期集清空；四轴守卫含新行） |
| `RetailSimpleTalkGateTest`（家族门） | **4/4 绿** |
| **T2**（285 选择器，基线 = 守卫加固片终版 `T2-171838.log`） | **ADDED 0 / REMOVED 0**（身份 51 == 51；595 测试 / 35F+16E 与基线逐项同） |
| **T3**（隔离副本，基线 = `T3-171907.log`） | **ADDED 0 / REMOVED 0**（身份 97 == 97；2020 测试 / 109F+23E 逐项同；副本跑完即删） |

## 6. 未决与移交（S3b-β / S3c）

- **S3b-β**：1323 的 `USE_OBJECT` 接取入口变体（`canonicalAcceptFlow` 增入口事件参数，或按 QE-… 的
  "物件哨兵接取者"全绑物件），外加 `GIVE_ITEM` 落点裁定（勘察推荐移到提交边，现成通道）。
- **S3c**：交付段（A 类 30 行同构替换 + D 类 50 行 (iii) 就地加窗；两红线：terminal `CLOSE` 必须
  **替换**而非追加、报告页退场判据用"页下发"R-REP 而非动作词表）。
- **系统发放 14 行**：接取段恒不可翻（设计内含，`buildChain` 已删 `unaccepted` 源路由）；交付段可按同规则参与。

## 7. 收口记录（2026-09-27）

| 判据 | 结果 | 证据 |
|---|---|---|
| 变化面 == 缺陷面 | **7 / 0 / 0** 逐行 == 切片 | `/tmp/s3b-fp.tsv` 对拍 + 指纹表重冻 |
| 链门 / 家族门 | **6/6 / 4/4 绿** | `RetailSimpleTalkChainGateTest`、`RetailSimpleTalkGateTest` |
| **T2**（285 选择器） | **ADDED 0 / REMOVED 0**（595 测试 / 35F+16E） | `gates/T2-173652.log` |
| **T3**（隔离副本） | **ADDED 0 / REMOVED 0**（2020 测试 / 109F+23E；完整方法级口径亦 0/0） | `gates/T3-173720.log` + `gates/T3-s3b-{reds,added,removed}.txt` |
| ↳ **哈希级加强证据** | 132 条红身份内容的 sha256 在 **S3a 终版 / 守卫加固片 / S3b 三次独立运行间逐字节相同**（`034f614da553a86e91cf47fce3eddcb1fa284d3c7ec434577e39b7eeb0cac421`）⇒ T3 口径下是**集合恒等**，强于"差集为空"；且红集中无任何条目涉及本片 7 个 id | 三份 `T3-*.log` + `T3-*-reds.txt` |
| 记忆库 | 追加 **QE-090**（层 A 键覆盖 + R 驱动段合成）+ 路由 | `patterns/quest-engine.md`、`systemPatterns.md` |

## 8. 纪律回执

未 commit/push；未启停服务；未新增/退役任何 TSV（链登记表只读）；未创建 worktree；
T3 在仓库外全树副本跑、跑完即删；Maven 仅用于聚焦测试与 T2/T3 门禁；tree 内与副本并行时树内只有一个 Maven。
