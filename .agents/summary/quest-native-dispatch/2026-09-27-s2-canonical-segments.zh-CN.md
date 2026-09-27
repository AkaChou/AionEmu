# SimpleTalk S2 · 链式规范段（G1 双块行）落地记录

> 车道：`quest-native-dispatch`；面：**SimpleTalk S2（链式面）**；角色：生产改造 + 门禁同步 + 证据。
> 上游：`2026-09-27-simpletalk-canonical-survey.zh-CN.md`（§2 落点、§3 策略三选一、§3.5 过场）；
> 侦察产物：`2026-09-27-s2-r-record-conflicts.zh-CN.md`、`2026-09-27-s2-t2-triage.zh-CN.md`、
> `2026-09-27-s2-movie-migration.zh-CN.md`（+ 各自 TSV）。
> 纪律：未 commit；未新增页码类 TSV；未启停服务；临时产物在 `/tmp`。

## 0. 一句话结论

链式 285 行中**同时具备 NPC_START 与 NPC_REPORT 块的 203 行（G1）**两个规范段整体接管：
接取换 `canonicalAcceptFlow`、交付换 `canonicalDelivery`，块 NPC 上构成旧页链的登记记录按**策略 A**
退场（过滤面 388 条）；**指纹变化面逐行等于 G1（203/0/0）**，G2/G3 的 82 行逐字未动。
纯 R 驱动行（G2 80 + G3 2 = 82 行）保持登记表逐字形状，留作 **γ 面**另行切片。

## 1. 切片边界（关键裁定）

| 组 | 定义 | 行数 | 本片 |
|---|---|---|---|
| **G1** | 有 NPC_START **且** 有 NPC_REPORT 块（非系统发放） | **203** | ✅ 规范段整体接管 |
| G2 | 无 NPC_REPORT 块（交付段纯 R 驱动） | 80 | ❌ 逐字（γ 面） |
| G3 | 有 REPORT 无 START（接取段纯 R 驱动） | 2（2611/35017） | ❌ 逐字（γ 面） |

**为什么 G2/G3 不进本片**：它们的接取/交付段由 R 记录构成（无块可换），canonical 化需为 R 驱动段
**合成**规范边（勘察 §3.4 的 γ 口径，A=201 条键冲突、含 `VAR_IS:var0=3` 阶段门等真实载荷），
且交付门形与 canonical 物品门不同构——属独立切片，不与 G1 混做。

**G1 判定同时是过滤器作用域开关**：非 G1 行 `canonicalSegments=false`，一字节行为不变
（指纹对拍证明：82 行逐字未动）。

## 2. 策略 A：过滤规则（落地口径）

作用域 = 规范段 NPC（接取 NPC = NPC_START 块 NPC；交付 NPC = NPC_REPORT 块 NPC ∪ 真端 reward_npc_name 解析集）：

- **接取 NPC 上**：`CHAIN_ACCEPT_RETIRED` = {ASK_QUEST_ACCEPT, SELECT1, SELECT1_1, SELECT1_1_1,
  QUEST_ACCEPT_1, QUEST_ACCEPT_SIMPLE, QUEST_REFUSE_1/2/SIMPLE} → 退场；**另加**判例
  1914/1915/1916 的 `SETPRO1 unaccepted→started` 接取提交（与 canonical 的 `QUEST_ACCEPT_SIMPLE(20000)`
  边逐字同形：`StartEligible` + `Sync(VISIBILITY_REFRESH)` + `Close`，且其按钮页 select1_1 随页梯退场；
  其余源的 SETPRO1 是阶段推进，保留）。该 3 条与 `s2-spec` §3.4 层 B1a 的裁定一致；
- **交付 NPC 上**：`CHAIN_DELIVERY_RETIRED` = {SELECT_QUEST_REWARD, CHECK_USER_HAS_QUEST_ITEM(_SIMPLE),
  SELECT5, SELECT6} **或** after-commit 下发退场页（{SELECT1, SELECT1_1, SELECT1_1_1, SELECT5, SELECT6}，
  `DIALOG:SHOW_QUEST_PAGE:` 通道）→ 退场；
- **中段 NPC**（简报页/SETPRO 阶梯/交互物）→ 一律逐字保留。

**实测过滤面（203 行）**：388 条 = 按页 {SELECT5:114, SELECT1_1:31, SELECT6:4} × 按动作
{ASK_QUEST_ACCEPT:114, QUEST_SELECT:114, SELECT_QUEST_REWARD:121, SELECT1_1:31, 39/SIMPLE:4+4}。
**过滤后 G1 行的 SELECT5/SELECT6 页下发 = 0**（零残留，编译期守卫断言）；selectionSources 越界 = 0。

### 2.1 fail-closed 守卫（编译期，违者拒绝编译）

1. **载荷守卫**：退场记录的 conditions 只允许 `HAS_ITEM:`、actions 只允许 `REMOVE_ITEM:`、
   after 只允许 DIALOG/SYNC/MOVIE（词表实测极窄：13/13/1）；其它一律抛。
   - 13 条 HAS_ITEM 载荷（11 行）由规范交付门承接：`gate = itemCheck ? (元数据门 / carried work-items,
     退场载荷兜底) : 空`，并逐条断言**载荷 ⊆ 规范门**。
2. **过场守卫**：退场记录带 `MOVIE:` token 时，要求触发轴 ∈ {1007, 1009} 且 movieId == 真端 cutsceneid1，
   否则抛——防"承载边退场但电影没重挂"的静默丢片。
3. **关窗出口守卫**：canonical 接取的 FINISH_DIALOG 只在 {unaccepted, 接取目标}；selectionSources 越界即抛
   （实测越界 0；28809 的 `s0 s1 s2` 属 G2，不在本片）。
4. **零残留守卫**：过滤后不得有 SELECT5/SELECT6 页下发、交付 NPC 上不得有 `started` 源 1009/39/20002
   （与家族门 `hasLegacyDeliveryResidue` 同口径）。

## 3. 过场（4 行，链式侧）

| 行 | 真端 | 触发轴 | 处置 | 落点/承载边 |
|---|---|---|---|---|
| 3020 | movie 363 | 1007 | **重挂** | `(unaccepted, 798143, 31)`（canonical 接取窗边；同 S1 判例 4056） |
| 1422 | movie 100 | 1353 | **不动** | 承载记录 `R4 (started,203731,1353)` 在中段 NPC，随简报页链保留 |
| 2421 | movie 132 | 1353 | **不动** | `R4 (started,204187,1353)` 同上 |
| 3006 | movie 361 | 1694 | **不动** | `R8 (s1,700339,1694)` 同上 |

- 分歧裁定：`s2-movie` 曾推定"1353/1694/1007 承载边全部退场"，`s2-rconf` 普查显示这些记录**不在键冲突面**；
  用原始记录复核后裁定：页触发行（1353/1694）的中段记录**不退场**（其页链仍在），只有 3020 的 1007 中转
  随 canonical 接取退场 ⇒ 仅 3020 需重挂。证据见 §5.2 的链门不变量。
- `SELECT2_CONTINUE` 闭包只改写**下发 SELECT2 页**的边；1422/2421 的承载边下发的是 SELECT2_1 ⇒ 无交互。

## 4. 改动面（生产）

`RetailSimpleTalkDefinitionCompiler`（单文件；S2 后 md5 `04e5b305717f`，该文件在本车道未被 git 跟踪，
变更不可用 `git diff` 呈现，以本台账 + 指纹对拍为证）：

- `buildChain` 增 `canonicalSegments` 分支（G1）+ 过滤器（`retiredChainRoute` / `chainPushedPages`）+
  守卫（`assertRetiredChainRouteQuiet` / `canonicalChainHandIn` / `retiredChainGate` /
  `assertCanonicalSelectionSources` / `assertCanonicalChainResidueFree`）；
- 接取块：`canonicalAcceptFlow(npc, target, acceptActions)` + （触发 1007 时）`attachMovieToRoute`；
- 报告块：`canonicalDelivery(reportNpc, hasItems, removeItems, deliveryWindowPage, report.source())`
  + （触发 1009 时）`attachMovieToRoute`；`carriedWorkItems` 改用**未过滤**回放集（门来源不被过滤窄化）；
- **未删任何 helper**：`acceptFlowChain`/`acceptContinuation`/`reportFlowChain`/`itemReportGate`
  仍服务 G2/G3 行（γ 面）；三处闭包（SELECT2_CONTINUE / SELECT5_CHECK / SELECT6）保留为安全网
  （G1 行过滤后恒不触发）。

## 5. 验收证据

### 5.1 变化面 == 缺陷面（指纹对拍）

重冻通道 `-Dretail.talkChain.fingerprintOut=/tmp/s2-chain-fp.new.tsv`（285 行全编译、problems=[]），
与替换前逐行对拍：**变化 203 / 新增 0 / 消失 0；变化集 == G1（双向差集空）**。
⇒ 已写入 `src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv`（该文件未纳入 git，
重冻前值可由回退本节生产 hunk 后重算）。

### 5.2 门禁

| 门 | 结果 |
|---|---|
| `RetailSimpleTalkChainGateTest` | **4/4 绿**：指纹回放 + 分区不变量 + 新增两条 S2 不变量 |
| ↳ `canonicalSegmentRowsCarryCanonicalShapesAndZeroPageChainResidue` | 203 行接取/交付形状 + 零残留；门源独立重算（元数据 / carried / 退场载荷三形） |
| ↳ `chainCutsceneRowsCarryExactlyOneRetailMovie` | 4 行 PlayMovie==1 + movieId/type + 触发轴落点；`CHAIN_CUTSCENE_ROW_FLOOR=4` |
| `RetailSimpleTalkGateTest`（家族门，单步面） | **4/4 绿**（含 `CUTSCENE_ROW_FLOOR=13` 不变量；链式过场注释改指链门） |
| `RetailSimpleTalkChainGateTest` 覆盖下限 | `CANONICAL_SEGMENT_ROW_FLOOR=200`（实测 203） |

### 5.3 诊断过程留痕（两条误判的裁定）

1. **1971 反向失配**：测试侧曾用"give_item 符号裸解析"近似工作物品门，得 [182215758:1]；生产侧 `carriedWorkItems`
   得空门。原始记录判生产侧正确——`remove_item1=ITEM_QUEST_1971C 1` 由链中段 `R2 SETPRO1` 的
   `HAS_ITEM+REMOVE_ITEM` 消耗（granted 1 − removed 1 = 0），legacy 同为空门（客户端无 SELECT5_CHECK 出口）。
   测试侧改为忠实重算 carried 语义（段内授予 − 段内移除，reward/complete 端记录不参与）。
2. **1909/3208/4208/11077 正向失配**：测试侧漏"carried 门"支（give_item 符号经 work_items 解析，
   如 `ITEM_DOC_QUEST_1909A 1` → 182206001:1），补第三形后对齐。
3. **过滤子句结构缺陷（由零残留守卫抓到，已修）**：追加 `SETPRO1` 判例时把接取子句误写成
   `if (npc ∈ 接取NPC) { …; return …; }` —— 该分支**总是 return**，使"同时是接取 NPC 又是交付 NPC"的
   行（判例 1422 的 203912）**跳过交付过滤**，`reward→reward` 的 SELECT5 报告页记录复活；
   编译期零残留守卫立刻拒绝编译 17 行（`canonical chain segment still pushes a retired page`）。
   修为两条**独立**子句（`(接取NPC ∧ 接取词表) ∨ (交付NPC ∧ 交付词表 ∪ 退场页)`）后全绿。
   **教训**：词表过滤的两个作用域子句必须独立成项；守卫消息带 `(source→target, event, after)` 是定位关键。

### 5.4 T2 基线（改前，链选择器）

`T2-134957.log`：**579 测试 / 35 failures + 16 errors = 59 条红身份**，落盘
`gates/T2-chain-baseline-reds.txt`。红因两类：**加载口径**（如 `missing quest definition 23809.xml`，
XML 退役波遗留）与**形状期望**（如 `quest 1514 的 START(var0=2) 状态必须唯一`）。
改后对拍判据：**ADDED 0 / REMOVED 0**。

### 5.5 T2 改后与分拣（收口记录）

- 首轮 T2（`T2-140528.log`，改后未分拣）：**ADDED 26 / REMOVED 0**，落 16 类，全部是预期形状冲突
  （1011→页 4、2375/1009→交付边 + 窗 5、1909/11008 中转退场、1913 点 31 即翻 REWARD）。
- 分拣波：3 个代理（`sortA` 8 类 / `sortB` 5 类 / `sortC` 3 类），**26 条全部重锚，零 HOLD**；
  产物 `2026-09-27-s2-sort-a/b/c.zh-CN.md`。两处代理自主裁定值得单独记：
  ①`QuestDispatchToVerteronFamilyProductionFlowTest` 的 6 行**不同形**——1913/1914/1915/1916 为 RETAIL_TABLE
  （canonical），**19070/19071 为 XML_RETENTION**（保留 legacy 形），代理按 retention owner 分流断言；
  ②`QuestRepeatLifecycleTest` 1963 的重复开局**未触设计边界**：重复别名由 `QuestDefinitionCompiler` 的
  `restoreRepeatStartDialogContract` 在编译期自动把 NONE→NONE 对话边复制到 `complete` 节点并附
  `StartEligible`（全局机制，非重复任务专属）。
- 终轮 T2（`T2-143321.log`）：**ADDED 1** —— 剩余 `Batch40ThreeNpcTalkLadderContractTest.rowOwnersDriveTheClientPageChain`；
  根因 = **21081 为 XML_RETENTION 行**：该测试类经 `ProductionQuestDefinitions.definitionInOverlay` 走生产目录，
  21081 由**保留 XML** 供货（legacy `31→SELECT5` + 1009 中转），而分拣代理按 SimpleTalk 编译器直编的 canonical
  形改写（探针实证：直编 21081 确为 canonical，因 `canonicalSegments` 只看块、不看 retention）。
  修正 = 该行恢复 legacy 断言（`Test.java` 内按既有 `retailOwned` 标志分流），并加类注释例外说明；
  临时探针 `Temp21081ProbeTest` 用完即删（源文件 + `.class` 均删）。
- 终版 T2 复跑（`T2-144601.log`）：581 测试 / 35F+16E —— **与基线计数逐项相同**；身份集对拍
  对 `T2-chain-baseline-reds.txt` = **ADDED 0 / REMOVED 0**（59 == 59），终版红集留痕
  `gates/T2-chain-final-reds.txt`。**T2 收口线达成。**

### 5.6 T3 身份集对拍（收口线）

对上一片基线 `gates/T3-132734.log` 做 `LC=C sort -u` + 集合运算，要求 **ADDED 0 / REMOVED 0**。

**首跑（共享树内，`T3-145705.log`）发现证据污染**：ADDED 2 = `RetailTalkChainProbeTest` +
`RetailTalkChainGateProbeTest` —— **兄弟车道（`scriptdll-quest-driver`）在 T3 运行窗口内于共享工作树创建的临时探针类**
（跑完即删：事后 `find src -newer` 无该文件、工作树全程哈希在跑前/跑后不一致也正是它们进出所致）。
归一化后 108 == 108（ADDED 0 / REMOVED 0），但按本车道 S1 先例，T3 必须**在仓库外隔离副本**跑以获得干净证据。

**隔离副本重跑**：`rsync` → `/private/tmp/aion-t3-s2`；顺序无关口径（`find | LC=C sort` + 逐文件 md5
清单）对拍副本与源**逐字节一致**；`QUEST_LOG_DIR` 回写真实车道目录。

- **试跑 1（弃用，`T3-150734.log`）**：瘦副本（仅 `pom.xml + src`，834M）→ **ADDED 45**，全部是
  **缺文件伪红**：`docs/quest/client-dialog-mapping/*.csv`（49 处 `NoSuchFileException`，漏拷 `docs/`）
  等；与基线条件（**4 处**缺文件）不同构 ⇒ **该日志不作为证据**，仅留痕。
  诊断中另确认两件事：①`QuestSoloriusEventDefinitionTest` 等读**相对路径** `src/main/resources/...`，
  已删 XML（80020/21065）在树内本就红（在基线 108 内）；②**树内跑与基线同构**（缺文件计数 4 == 4）。
- **试跑 2（弃用，`T3-151932.log`）**：`rsync pom.xml src docs`（947M，逐字节对拍通过）→ **ADDED 1**
  = `QuestDialogMigrationGateTest.dialogEnumGeneratorDoesNotScanLegacySyntax`（`IllegalStateException: cannot read
  .agents/summary/quest/generate_quest_dialog_enums.py`）——**又一个副本完整性伪红**（漏拷 `.agents/`；
  该类在树内跑 4/4 绿、不在基线内）。⇒ 副本必须**全树**（该测试读 `.agents/summary/quest/` 下的生成器脚本）。
- **试跑 3（正式，末次）**：全树副本（`rsync --exclude .git --exclude target`，≈1.3G，顺序无关逐文件哈希对拍），
  结果见 §7 收口记录。**教训**：隔离副本的排除清单必须**以"测试实际读取面"为界**——本片三次试跑依次暴露
  `docs/`（客户端 CSV）、`.agents/`（生成器脚本）两处遗漏；`src/ + pom.xml` 的瘦副本必假红。

## 6. 未决与移交

- **γ 面（G2/G3，82 行）**：R 驱动交付段需合成 canonical 边；前置 = 交付门形裁定（`VAR_IS:var0=k` 阶段门）、
  2953 的 `complete→complete START_ELIGIBLE` 重复接取（选中 1_1 页）承载迁移、28809 的 selectionSources
  跨节点关窗出口。勘察 §3.4 的 A=201 键冲突面即此。
- **T2 分拣**：链面 48 档 1 类中的"锁旧形"断言按 canonical 重锚（桶 P–V 台账已备，见 s2-t2-triage）。
  `RetailSimpleTalkMigrationReviewContractTest`（18 id，链面最重）与 `QuestMultistepChainContractTest`（11 id）
  优先；`RewardNpcOwnershipContractTest` 的 21455.xml 加载口径需重锚。

## 7. 收口记录（2026-09-27）

| 判据 | 结果 | 证据 |
|---|---|---|
| 变化面 == 缺陷面 | **203 / 0 / 0**，逐行 == G1；G2/G3 82 行逐字未动 | 指纹重冻对拍（三次重算均 203/0/0，末次含 1914/1915/1916 增量） |
| 链门 | 4/4 绿（203 行形状 + 零残留 + 4 行过场 + 指纹/分区） | `RetailSimpleTalkChainGateTest`（含新增 2 例不变量） |
| 家族门 | 4/4 绿（单步面不变量不动） | `RetailSimpleTalkGateTest` |
| **T2**（链选择器 285 id） | **ADDED 0 / REMOVED 0**（59 == 59；581 测试 / 35F+16E 与基线逐项相同） | `gates/T2-144601.log` + `gates/T2-chain-{baseline,final}-reds.txt` |
| **T3**（全 questEngine 树） | **ADDED 0 / REMOVED 0**（108 == 108；2018 测试 / 109F+23E；缺文件计数 4 == 基线） | `gates/T3-153035.log`（全树副本内跑）+ `gates/T3-final-reds.txt` |
| 记忆库 | QE-087 新增 + 路由；`MEMORY_BANK_SYNC_OK ENTRIES=132`；`MEMORY_BANK_VERIFY_OK STEPS=3` | `patterns/quest-engine.md`、`systemPatterns.md` |
| 改动面 | 19 个 Java 文件（1 生产 + 2 门禁 + 16 分拣类）+ 1 指纹重冻 | `find src -name '*.java' -newer <基线日志>` |

**纪律回执**：未 commit/push；未启停服务；未新增/退役任何 TSV（链登记表只读、一字节未改）；
隔离副本（`/private/tmp/aion-t3-s2`）跑完即 `rm -rf`，无残留、无 worktree；
临时探针 `Temp21081ProbeTest` 用完即删（源 + `.class`）；
Maven 仅用于聚焦测试与 T2/T3 门禁。

## 8. 代理复核对账（迟到回报，收口后）

三份代理报告在收口后到达，逐条对账结论：**无返工项**，两处设计分歧按本实现裁定。

1. **`s2-spec`（871 行修订版）的两条反例 → 支持本实现"承载边保留"**：规格证明 `1694` 退场会让
   `select3` 页(1693) 的 `{1694, 1779}` 双按钮无路由（且**无 SELECT3 闭包兜底**），`1353` 退场会让
   SELECT2 闭包把落点页降级 `DEFAULT_SUCCESS`（简报页消失）。本实现的过滤词表**本就不含 1353/1694**
   ⇒ 两条反例在落地形态下不成立。**唯一分歧**：规格主张"3 行页触发的电影也迁到 31 边并摘除承载 token"，
   本实现**保留原位**——理由：页触发（cs1_haction=1353/1694）的真端语义就是"按下 select2_1/select3_1 按钮时播片"，
   承载边可达 ⇒ 原位最忠实；迁到交付边会**推迟播片**（页精度后移）。链门不变量按触发轴分派
   （1007/1009 → `QUEST_SELECT` 落点；页触发 → 同号动作边）——规格的"迁移+摘 token"变体会在该分支上失败（落点变 `QUEST_SELECT` 而触发轴仍 1353/1694），**两种设计各有配套断言、不可混用**；本片取原位。
2. **`s2-rconf` 的判据修正成立且已被覆盖**：`attachMovieToRoute` 的 fail-closed 数的是**匹配路由条数**
   （`matches != 1`），不是电影数 ⇒「迁移 + 不过滤」不会报错而是**静默双播**。本实现对该风险的处置 =
   过滤承载边（3020 的 1007 记录退场）+ 链门**电影守恒断言**（每行 `PlayMovie` 恰 1 + movieId/type + 落点轴），
   已落地并通过（4/4 绿）——与 rconf 的"必须新增守恒断言"结论一致。
3. **作用域分歧（已在 §1 裁定并留痕）**：规格按"START ∪ REPORT = 268 行"预期漂移面，本实现取**双块交集 203 行**；
   差额 65 行（63 有 START 无 REPORT + 2 有 REPORT 无 START）保持 legacy，与 G2/G3 一并归入 γ 面。
   实测漂移面 203/0/0 与 G1 逐行相等 ⇒ 交集切片的"变化面==缺陷面"判据成立；268 口径无法提供同等粒度的验证。
4. **分拣三代理的 HOLD/例外处置**与本片证据一致：`sortA` 3 条 HOLD（EarlyElyos 的既有在册红 + XmlQuestFamily
   基线红）按纪律未动；`sortB` 报的 13 条（任务书写 9 条为枚举笔误）全部重锚；`sortC` 的
   `CANONICAL_DELIVERY_QUEST_IDS` 分派（19070/19071 = XML_RETENTION）独立复现了判据 = retention owner。
5. **遗留建议（未执行）**：`sortB` 建议把 `clientCheckButtonsConsumeEveryRetailRequirementAndKeepTheFailurePage`
   改名（FailurePage 已退场）——为保红身份对拍稳定本片未改，列入 γ 面清理候选。
