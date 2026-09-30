# SimpleTalk S3a · 仅接取段接管（60 行）落地记录


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 车道：`quest-native-dispatch`；面：**SimpleTalk S3a（γ 面第一片）**；角色：生产改造 + 门禁同步 + 证据。
> 上游：`2026-09-27-s3-gamma-survey.zh-CN.md`（γ 面普查：接取可翻 68/82 = 61 块驱动 + 7 纯 R 驱动）、
> `2026-09-27-s3-special-rows.zh-CN.md`（逐行裁定证据）、`2026-09-27-s3-t2-triage.zh-CN.md`（分拣预判）。
> 纪律：未 commit；未新增/退役任何 TSV；未启停服务；临时产物在 `/tmp` 与仓库外副本。

## 0. 一句话结论

γ 面 82 行中**有 NPC_START 块、无 NPC_REPORT 块、非系统发放、且接取块 selectionSources 段内**的
**60 行**只接管接取段（`canonicalAcceptFlow` 原地换形 + 段内策略 A 过滤），**交付段一字未动**
（纯 R 驱动的 SELECT5/1009 页链逐字保留，留 S3c）；**指纹变化面逐行等于该 60 行（60/0/0）**，
S2 的 G1 203 行零漂移，判例 28809 派生式延期、逐字未动。

## 1. 切片边界（关键裁定）

| 组 | 定义 | 行数 | 本片 |
|---|---|---|---|
| **S3a** | 有 `NPC_START` 块 ∧ 无 `NPC_REPORT` 块 ∧ 非系统发放 ∧ selectionSources ⊆ {unaccepted, 块 target} | **60** | ✅ 仅接取段接管 |
| S2 独占 | 两块俱全（G1，非系统发放） | 203 | ❌ 已收口（逐字未动） |
| 延期 1 行 | selectionSources 越界（**28809**） | 1 | ❌ 派生式延期（逐字未动） |
| 纯 R 接取 7 行 | 无 START 块（1323/2611/3001/3023/21136/24202/80320） | 7 | ❌ S3b |
| 系统发放 14 行 | 真端类别哨兵发放（35010/35011/35017/35018/35024/35025/35026/45010/45011/45017/45018/45024/45025/45026） | 14 | ❌ S3b/S3c |
| 交付段面 | 全部 82 行的交付段（50 行被"`SETPROn` 翻面落中间人 NPC"卡住，待裁定） | 82 | ❌ S3c |

**为什么只切"接取段"**：交付段的 50 行阻断是**设计裁定**（交付落点形 (i) 搬到真端 reward NPC vs
(ii) 留原 NPC 换 `QUEST_SELECT(31)` + 追加奖励窗），未裁定前动工会造出可观测红；接取段与交付段
正交，可独立验收。本片把"段旗标"从 S2 的 `canonicalSegments`（两段耦合）拆成
`canonicalAccept` / `canonicalDelivery` 两个独立旗标，使"只接管一段"成为编译器的合法状态。

## 2. 判例 28809：为什么延期（派生式，非硬编码）

28809 的接取 NPC == 交付 NPC（830169），交付段仍是纯 R 驱动（本片不碰）⇒ R5（`QUEST_SELECT s2→s2`
下发 SELECT5）留在 830169 上 ⇒ 登记表把**交付态 s1/s2 泄进接取块的 `selectionSources`**
（实测 `unaccepted s0 s1 s2`）。canonical 接取只在 {`unaccepted`, 块 target} 登记 `FINISH_DIALOG(1008)`
⇒ 现在接管会把**在服务页**（SELECT3/SELECT5，仍被 R 记录下发）上的关窗按钮变成死端
（契约门 `BUTTON_WITHOUT_ROUTE` 是致命类）。

- 实装 = **派生谓词** `acceptSourcesWithinSegment(block)`（与守卫 `assertCanonicalSelectionSources` 同源），
  越界行 `canonicalAccept=false` ⇒ 该行**逐字保留**（漂移 0），页链出口随交付段（S3c）一并裁定。
- 全 285 行 START 块普查：**越界行恰 1 行（28809）**；链门新增 `ACCEPT_DEFERRED_ROWS = {28809}`
  逐行断言（登记表漂移即红）。
- 若未来要随片裁定"丢 s1/s2 关窗出口、由 DialogService 兜底"（勘察 §6-2 的候选），必须先证明
  该 1008 按钮不产生致命契约红——**本片未取该路线**（fail-closed 优先）。

## 3. 改动面（生产，单文件）

`RetailSimpleTalkDefinitionCompiler`（本车道未跟踪，改前 md5 `04e5b305717f` 系 S2 版）：

1. **段旗标拆分**：`canonicalAccept`（`!systemGrant ∧ 有 START 块 ∧ selectionSources 段内`）、
   `canonicalDelivery = canonicalAccept ∧ 有 REPORT 块`（S3c 将解耦以纳入 G3 的交付段）；
   `acceptNpcs` 随 `canonicalAccept` 填充、`reportNpcs` 随 `canonicalDelivery` 填充
   ⇒ 作用域集合为空时 `retiredChainRoute` 恒不匹配，未接管行一字节不变。
2. **接取分支**：`if (canonicalAccept)` 换 `canonicalAcceptFlow(npc, target, startActions)`
   + 1007 触发轴的过场重挂（S1 判例）。
3. **过滤器**：去掉 `canonicalSegments &&` 前置（由作用域集合自门控）——两个作用域子句仍**独立**
   （S2 教训：接取子句写成"总是 return"会让同体 NPC 行跳过交付过滤）。
4. **零残留守卫按段分治**：新增 `assertCanonicalAcceptResidueFree`（接取段：块 NPC 上不得再留
   select1 族页下发 / 1007/1011/1012/1013 动作路由），原 `assertCanonicalChainResidueFree` 只在
   `canonicalDelivery` 时跑（交付段口径不变）——**不把交付段页判据加进接取守卫**（S3a 行的交付段
   本就该保留 SELECT5/SELECT6）。
5. **未删任何 helper**：`acceptFlowChain` / `acceptContinuation` / `reportFlowChain` / `itemReportGate`
   仍服务未接管行；三处闭包保留。

## 4. 验收证据

### 4.1 变化面 == 缺陷面（指纹对拍）

重冻通道 `-Dretail.talkChain.fingerprintOut=/tmp/s3a-chain-fp.tsv`（285 行全编译、**problems=[]**），
与 S2 冻结值逐行对拍：

| 判据 | 结果 |
|---|---|
| changed | **60**，逐行 == S3a 行集（双向差集空） |
| added / removed | **0 / 0**（285 == 285） |
| G1 203 行漂移 | **0**（S2 收口面一字节未动） |
| 28809 | 未在变化集（派生式延期成立） |

新冻结值已写入 `src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv`（表头追加 S3a 注记）。

### 4.2 门禁

| 门 | 结果 |
|---|---|
| `RetailSimpleTalkChainGateTest` | **5/5 绿**（含新增 `acceptOnlyRowsCarryCanonicalAcceptAndKeepTheirDeliverySegment`） |
| ↳ S3a 不变量 | 60 行接取窗边 + 1002/20000 两形提交 + 接取侧零页梯残留；**交付段逐字保留**（登记记录的报告页下发仍在——与 S2 双块行"零残留"成对照）；延期行保持 legacy 页梯；`CANONICAL_ACCEPT_ROW_FLOOR=55`；`ACCEPT_DEFERRED_ROWS={28809}` |
| ↳ S2 不变量（203 行） | 仍绿（回归证明） |
| `RetailSimpleTalkGateTest`（家族门，单步面） | **4/4 绿** |
| 指纹回放/分区/登记形状 | 绿（285 行指纹、retention 二分、登记形状） |

### 4.3 T2（链选择器 285 id，与 S2 基线同口径）

- 首跑（`gates/T2-162854.log`，594 测试 / 37F+16E）：**ADDED 2 / REMOVED 0**，两条新增红恰为 S3a 行
  （`Quest1152RetailAlignmentTest.followsTheClientChefDialogAndLegacyTwoStepItemContract`、
  `Quest1118ProductionFlowTest.acceptanceGivesTheOintmentAndFinalDeliveryConsumesIt`），红因均为
  "接取入口页"硬断言（旧形页 1011 vs canonical 页 4）⇒ **变化面 ⊂ 切片**。
- 分拣波（2 个代理，只改测试、未跑 Maven）：`2026-09-27-s3a-sort-1152.zh-CN.md` /
  `2026-09-27-s3a-sort-1118.zh-CN.md`。两类均按 **retention owner 实测值**（`RETAIL_TABLE`）判 canonical，
  **只换"锁旧形"的断言**：1152 → 页 4 + `StartEligible` + 1002 after + **新增** 20000 边 + `FINISH_DIALOG`
  关窗出口 + 旧梯无路由双锁；1118 → 31 首屏改页 4 并**新增** 1002/20000 双提交边形状断言（交付段断言一字未动）。
  两处均未删断言、未放宽、未删测试方法。预分拣文档 `2026-09-27-s3a-t2-prep.zh-CN.md` 的"预判必红"桶 A
  **恰等于**实测 ADDED 2（独立交叉验证）。
- 聚焦验证：`mvn -o -B test -Dtest=Quest1152RetailAlignmentTest,Quest1118ProductionFlowTest` = **2/2 绿**。
- 终版 T2（`gates/T2-164459.log`，594 测试 / 35F+16E）：**ADDED 0 / REMOVED 0**（身份集 51 == 51，
  对拍基线 = S2 终版 `T2-144601.log`；终版红集留痕 `gates/T2-s3a-final-reds.txt`）。**T2 收口线达成。**

### 4.4 T3（全 questEngine 树，仓库外隔离副本）

- 预跑（`gates/T3-162856.log`，2019 测试 / 111F+23E）：ADDED 2 = 上节两个分拣类（**预跑不作最终证据**）。
- 终版（分拣后增量重同步 `src/ docs/ .agents/` 后重跑，`gates/T3-165559.log`）：**2019 测试 / 109F+23E**
  （与基线 109F+23E 逐项相同；+1 测试 = 本片新增的链门不变量方法）；**ADDED 0 / REMOVED 0**（身份集 97 == 97，
  两边用**同一**正则提取，基线 = S2 终版 `T3-153035.log`；S2 台账里的"108"系早期宽松提取的口径差，本片统一为 97）；
  `NoSuchFileException` 计数 **4 == 4**（与基线同构）；产物 `gates/T3-s3a-{reds,added,removed}.txt`；
  副本 `/private/tmp/aion-t3-s3a` 跑完即 `rm -rf`（已确认不存在）。
- **口径补强（T3 执行代理的独立提取）**：另用**完整方法级**口径（含参数化用例 `name()[1]` 形）复算，
  基线 132 / 终版 132 = **ADDED 0 / REMOVED 0**；且该口径计数恰等于 `Failures+Errors`（109+23 = 132）
  ⇒ 提取无损（strict 正则会漏 35 条参数化名）。两口径结论一致。两条前红
  （`Quest1118ProductionFlowTest` / `Quest1152RetailAlignmentTest`）在终版中各为 1 test / 0F / 0E。
  重同步后 `diff -r --brief` 对 `src/ docs/ .agents/` 三处 **0 差异行** ⇒ 副本 == 工作树当前态。

### 4.5 覆盖面提示（不得把"T2 全绿"读成"60 行全绿"）

`2026-09-27-s3a-t2-prep.zh-CN.md` §4 实测：60 行里 **10 行零测试类引用**
（`1394 2553 2963 3218 4209 4218 4970 30711 30761 80752`，其中 `4970` 是唯一的"有接取块、无完成块"极端形态）
⇒ T2 只证明 **50 行**不回归；这 10 行的接取形由链门的两个硬数字
（`CANONICAL_ACCEPT_ROW_FLOOR=55`、`ACCEPT_DEFERRED_ROWS={28809}`）+ 指纹表逐字背书，
不得表述为"T2 证明了全部 60 行"。

## 5. 未决与移交

- **S3b（纯 R 接取 7 行）**：需 `USE_OBJECT` 接取变体（1323 箱子哨兵 730032）、`GIVE_ITEM` 落点、
  3001/3023/21136 缺 20000/20001 形、3023 的 `SELECT3_1`/`SELECT3_1_1` unaccepted 源页梯、
  **层 A（键覆盖过滤）**——否则 `explicitRoutes` 会把合成的 canonical 边反过来删掉（假绿）。
- **S3c（交付段 82 行）**：D 类 50 行取 **(iii) 就地加窗**（保留中间人 `SETPROn` 键与简报页链、追加档位窗、
  退场报告页）；A 类 30 行走既有 `canonicalDelivery` 同构替换；
  28809 与 G3 的 `canonicalDelivery` 需与 `canonicalAccept` 解耦。
- **（iii）的两条实装红线（裁定代理复算后给出，取代我此前的候选口径）**：
  ①**terminal `CLOSE` 必须替换而非追加**——实测 57 条翻面记录中 37 条以 `CLOSE` 收尾，天真追加会得到
  `Sync(LVR) → CloseDialog → 窗`（窗被立即关闭 ⇒ 可观测"交任务后无奖励窗"）；
  ②**报告页退场判据用"页下发"（R-REP）而非动作词表**——退场对象 = `chainPushedPages ∋ {SELECT5,SELECT6}`
  的记录（实测 39 条/39 行，**不加** `NPC_COMPLETE` 条件），且**严禁**改用 `CHAIN_DELIVERY_RETIRED` 动作词表
  （含 `SELECT_QUEST_REWARD`，会连带退场 11 行 reward 态重开载体）。
  另：翻面自身带窗者 **5 行**（非 7 行；`4970/21217` 的窗挂在其他记录上）；带条件翻面 **17 行**
  （9×`VAR_IS:var0=0` + 2×`var0=1` + 2×`VAR_AT_LEAST` + 4×`HAS_ITEM`）；翻面动作共 57 条/50 行
  （`SETPRO1`×25 / `SETPRO3`×13 / `SETPRO2`×12 / `SELECT_QUEST_REWARD`×5 / `USE_OBJECT`×2）。
- 裁定摘要见同目录 `2026-09-27-s3-adjudication-brief.zh-CN.md`（代理产物；§1-① 推荐 (iii)、
  §2 切片表（含 4a 最薄子切片 5 行）、§3 含 R11 双窗排除证据与 terminal-CLOSE 裁定）。
- 待清理（S2 遗留）：`RetailSimpleTalkMigrationReviewContractTest` 的 FailurePage 命名、
  `RetailSimpleHuntIrCompiler`、`retail-quest-ai-name-groups-rejected.tsv`。

## 6. 收口记录（2026-09-27）

| 判据 | 结果 | 证据 |
|---|---|---|
| 变化面 == 缺陷面 | **60 / 0 / 0**，逐行 == 切片；S2 的 G1 203 行零漂移；28809 逐字未动 | 指纹重冻对拍（`-Dretail.talkChain.fingerprintOut`），problems=[] |
| 链门 | **5/5 绿**（S3a 不变量含"交付段逐字保留"对照断言；S2 两条不变量回归绿） | `RetailSimpleTalkChainGateTest` |
| 家族门 | **4/4 绿**（单步面不变量不动） | `RetailSimpleTalkGateTest` |
| **T2**（链选择器 285 id） | 首轮 ADDED 2（两条恰为切片行的入口页硬断言）→ 2 代理分拣重锚 → 聚焦 2/2 绿 → **ADDED 0 / REMOVED 0**（身份 51 == 51；594 测试 / 35F+16E 与基线逐项相同） | `gates/T2-162854.log`、`gates/T2-164459.log`、`gates/T2-s3a-final-reds.txt` |
| **T3**（全 questEngine 树，仓库外全树副本） | **ADDED 0 / REMOVED 0**（身份 97 == 97；2019 测试 / 109F+23E 与基线逐项相同；缺文件计数 4 == 4） | `gates/T3-165559.log` + `gates/T3-s3a-{reds,added,removed}.txt` |
| 记忆库 | 新增 QE-088 + 路由；`MEMORY_BANK_SYNC_OK ENTRIES=133`；`MEMORY_BANK_VERIFY_OK STEPS=3` | `patterns/quest-engine.md`、`systemPatterns.md` |
| 改动面 | 1 生产 + 1 链门测试 + 1 指纹重冻 + 2 分拣测试类 | `RetailSimpleTalkDefinitionCompiler`、`RetailSimpleTalkChainGateTest`、`retail-simple-talk-chain-ir-fingerprints.tsv`、`Quest1152RetailAlignmentTest`、`Quest1118ProductionFlowTest` |

## 7. 纪律回执

未 commit/push；未启停服务；未新增/退役任何 TSV（链登记表只读）；未创建 worktree；
T3 用仓库外副本（`/private/tmp/aion-t3-s3a`，跑完即删）；Maven 仅用于聚焦测试与 T1/T2/T3 门禁。
