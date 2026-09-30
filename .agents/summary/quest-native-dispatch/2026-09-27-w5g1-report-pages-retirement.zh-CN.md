# W5-g1 · 页码类 TSV 退役第一张：`quest_client_report_pages.tsv`（含读取者与死代码整体退场）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 车道：`quest-native-dispatch`；面：**W5 页码类 TSV 退役（g1）**。
> 上游：`GOAL.zh-CN.md` §3 的 **W5** + §1 的 **F3**；`2026-09-27-tsv-retirement-candidates.zh-CN.md` §2.1-①、
> §3 第 0 层、§4-0（退役机制）。
> 纪律：未 commit；未启停服务；**未新增任何 TSV**；T3 在仓库外副本、跑完即删。

## 0. 一句话结论

`quest_client_report_pages.tsv`（5995 行）的**唯一读者**在 `canonical=false` 死分支里——该分支的两个公有重载
**零调用者**（全仓仅 3 处 `compile(` 调用，全是 7 参 canonical 形）⇒ 本表今天已无有效读者。按正规动作整体
退场：**删读取者类 + 删死重载与死页链流 + 删参数穿线 + 删表 + 删清单行 + `EXPECTED_TSV_COUNT` 26 → 25**；
**零 IR 变化**（全部家族门与指纹不变）。

## 1. 死分支证明（逐调用点，实测当日树）

| 事实 | 证据（file:line，实测） |
|---|---|
| 表的唯一值读取点 | `RetailSimpleHuntDefinitionCompiler.java:771`（`clientReportPages.reportPage(questId)`），值只在 `canonical=false` 的交付分支消费 |
| 两个 `canonical=false` 公有重载 **零调用者** | `RetailSimpleHuntDefinitionCompiler.compile` 全仓 3 处调用：`RetailQuestDriver.java:861`、`RetailSimpleHuntEquivalenceGateTest.java:347`、`RetailSimpleHuntFamilyGateTest.java:160`——**全是 7 参 canonical 形**（`…, clientDialogExits, clientReportPages)`，内部 `canonical=true`） |
| 其余入口恒 canonical | `compileCanonical`（`:131`，true）、`compileSequentialStages`（`:154`，true）、`compileSerialChain`（独立实现，不引用该表） |
| 单元级复核 | `reportPage(` 全仓 1 处（定义 1 + 调用 1）；`RetailClientReportPages` 引用面 = 1 类 + 4 生产文件 + 3 测试 |
| 读取者类是否另有消费者 | 无（类内只有 `load`/`reportPage`/`empty`） |

⇒ 退役前置（审计 §2.1-① 的 ①②）已全部满足，**且不需要任何形状裁定**。

## 2. 处置清单（同一片、同一提交范围）

| # | 动作 | 对象 |
|---|---|---|
| 1 | 删死重载 ×2（含 javadoc） | `compile(…, acceptEntryPage)`、`compile(…, acceptEntryPage, incompleteReportRoutes)` |
| 2 | 删 `canonical=false` 分支与随之无用的四个私有/包内方法 | `reportFlow`、`incompleteReportRoutes`、`briefingFlow`、`acceptContinuation`（**`acceptFlow` 保留**：HandinDialogFlow 的唯一剩余调用方，三票否决、明确排除迁移） |
| 3 | 简化签名与穿线 | 私有 `compile`/`build` 去 `clientReportPages`/`acceptEntryPage`/`incompleteReportRoutes`/`canonical`；`compileCanonical`/`compileSequentialStages` 去参；DD 定义编译器（2 签名 + 2 转发）、DD talk-hunt 链编译器（2 签名 + 1 转发）、`RetailQuestDriver`（常量 + 字段 + 构造参数 + 加载 + 2 调用点） |
| 4 | 删读取者类 | `src/main/java/.../retail/RetailClientReportPages.java` |
| 5 | 删表 | `src/main/resources/aion/data/static_data/quest_retail/quest_client_report_pages.tsv`（5995 行）；快照**补录**：`retired-tsv/quest_client_report_pages.tsv.retired-20260927`（sha256 `a458ec9300600505551f4ab832672a17fb52f5d1d71386056df95de0c3db529e`，5999 行含头）——本片退役时快照纪律（交接包 §2.6）尚未成文，内容自 `target/classes/…/quest_client_report_pages.tsv` 孤副本恢复；其余三张退役表在退役时已按纪律落快照 |
| 6 | 删清单行 | `quest-retail-tsv-manifest.tsv` 的 `quest_client_report_pages.tsv` 行 |
| 7 | 计数 | `RetailTsvManifestGateTest.EXPECTED_TSV_COUNT` **26 → 25** |
| 8 | 测试夹具 | 3 个门禁测试各删 1 处 `RetailClientReportPages.load` + 1 个字段 + 1 个实参 |

**生成器移交（兄弟车道）**：表由 `.agents/summary/scriptdll-quest-driver/build_quest_client_report_pages.py`
生成；该脚本属 `scriptdll-quest-driver` 车道（本车道**只读**，一字节未改）。若将来有人重跑该生成器，
`RetailTsvManifestGateTest`（磁盘集合 == 清单集合）会**立刻变红** ⇒ 这是 fail-closed 兜底；停写动作登记为
**W6 移交项**（姊妹车道执行）。

## 3. 开发期事故与修复（诚实留痕）

- **事故**：用脚本按方法名批量删除四个死方法时，删除器把**类头一段**（`DEFAULT_PVP_LEVEL_GAP` 常量 +
  `PACKET_ONLY_SYNC` 常量 + 私有构造器 + `Outcome` 记录 + 家族入口 javadoc 首行）一并吃掉——该类是
  **未跟踪文件**（无 git 基线可回滚），编译立刻报 5 处 `找不到符号`。
- **修复**：从**本会话历史里的原始文件读取记录**（工具输出逐字保留）重建被删文本（常量、记录与 javadoc
  逐字还原：`DEFAULT_PVP_LEVEL_GAP = 10`、`PACKET_ONLY_SYNC`、`public record Outcome(CompiledQuestDefinition
  definition, String rejectionCode, String detail)` + `accepted()`、`网格形合成（家族入口）。` 首行）；
  结构体检（孤立注释行 0 / 花括号平衡 0）+ 全家族门禁复核通过。
- **复核发现的第二处偏差**：首轮删除的启发式**只吃到了 javadoc 与类头**，四个"死方法"本身**仍在文件里**
  （编译能过、门禁也绿——是**静默未完成**）。第二遍改用**签名行 + 花括号配对**（先在 `/tmp` 落备份）才真正
  删净三个方法（`reportFlow` 已在首轮随 javadoc 一起消失；`incompleteReportRoutes`/`briefingFlow`/
  `acceptContinuation` 本次删除，共 −91 行）；复跑全部门禁确认零行为变化。
- **教训（写入记忆库）**：**按名删除成员必须用"签名行 + 花括号配对"锚定，禁止用"往上找最近的 `/**`"之类
  的启发式回退**；对**未跟踪文件**做批量删除前应先落副本（本次靠会话记录救回，属侥幸）；**删完必须复核
  "方法真的不在了"**（编译通过与门禁绿都不能证明删除发生——死代码删不删都绿）。

## 4. 门禁

**终版复跑（第二轮删除之后，判据同轴）**：T1 `ADDED 0 / REMOVED 0`（sha256 `3b92439da8…` 与基线逐字节相同）、
T2（597 测试）与 T3（2022 测试）同为 `0/0` 且红集 sha256 与上一片逐字节相同；清单门 3/3、等价门 2/2、
SimpleHunt 家族门 1/1（328.9 s）、SimpleTalk 链门 8/8、家族门 4/4 —— 即"删净死代码"对 IR 无任何影响。

| 门 | 结果 |
|---|---|
| `RetailTsvManifestGateTest`（清单门，磁盘集合 vs 登记集合 vs 计数） | **3/3 绿**（退役三步同一片完成） |
| `RetailSimpleHuntFamilyGateTest`（家族门：全部 SimpleHunt 行仍被受理/形状不变） | **1/1 绿**（355.7 s） |
| `RetailSimpleHuntEquivalenceGateTest`（等价门：规范形 vs 旧 XML 基线） | **2/2 绿** |
| `RetailSimpleTalkChainGateTest` / `RetailSimpleTalkGateTest`（本片不触碰 SimpleTalk） | **8/8 / 4/4 绿** |
| **T1**（固定套件，含清单门） | **ADDED 0 / REMOVED 0**，红身份集 sha256 `3b92439da8…` 与基线**逐字节相同**（唯一红 = 在册 `RetailDataDrivenGateTest.driftVersusShellsIsRegistered`） |
| **T2**（树内 285 选择器） | **ADDED 0 / REMOVED 0**（597 测试 / 35F+16E；红集恒等） |
| **T3**（仓库外全树副本，跑完即删） | **ADDED 0 / REMOVED 0**（2022 测试 / 109F+23E；红集恒等） |

**零 IR 变化**：本片只删死代码与死数据，**任何已受理行都不应改变形状**——判据 = 全部家族门绿 + T2/T3 零新增，
且 SimpleTalk 指纹表（285 行）在 T3 内由链门直接校验通过。

## 5. 门禁证据

| 门 | 结果 | 证据 |
|---|---|---|
| **T1**（固定套件，含清单门） | **ADDED 0 / REMOVED 0**（86 测试 / 1F = 在册红） | `gates/T1-203317.log`（首轮树）/ `gates/T1-210105.log`（**终版树**）+ `gates/T1-w5g1{,-b}-{reds,added,removed}.txt` |
| ↳ 哈希级加强证据 | 红身份集 sha256 `3b92439da8052988f0acd3a23a646786a78c3ac58e7c64a9c1dcabdd1d5cd137`（1 条）与基线**逐字节相同** | `gates/T1-baseline-reds.txt` |
| **T3**（仓库外全树副本，跑完即删） | **ADDED 0 / REMOVED 0**（2022 测试 / 109F+23E；两次独立副本跑同结果） | `gates/T3-203310.log` + **`gates/T3-205507.log`（终版副本）** + `gates/T3-w5g1{,-b}-{reds,added,removed}.txt` |
| ↳ 哈希级加强证据 | 红身份集 sha256 `ce4673c74ed61fdcff494fedcc1b8e98c78b252d54b150a05c4bdaa937197870`（97 条）与上一片**逐字节相同**；副本内清单门 3/3 绿 | `gates/T3-s3cobj-reds.txt` |
| **T2**（树内 285 选择器） | **ADDED 0 / REMOVED 0**（597 测试 / 35F+16E） | `gates/T2-204450.log` + **`gates/T2-210737.log`（终版树）** + `gates/T2-w5g1{,-b}-{reds,added,removed}.txt` |
| ↳ 哈希级加强证据 | 红身份集 sha256 `57bb0621b2aa90f861e41358f63baa5450e73753440160ba276bb0255df721fd`（51 条）与上一片**逐字节相同** | `gates/T2-s3cobj-reds.txt` |

## 6. 沉淀

- **记忆库**：新增 **QE-094**（页码类 TSV 退役的三步与死分支证明：生成器 / 表 / 读取者 / 消费点四环节同轴）
  并补 `systemPatterns.md` 路由；**ENV-004（注释与批量文本安全）追加第 3 条"成员级删除的锚定与快照纪律"**
  （§3 事故的教训 + 会话历史救回通道）；`MEMORY_BANK_SYNC_OK ENTRIES=139`、`MEMORY_BANK_VERIFY_OK STEPS=3`。

## 7. 纪律回执

未 commit/push；未启停服务；**未新增任何 TSV**（本片只做退役）；未创建 worktree；T3 在仓库外全树副本
（`/private/tmp/aion-t3-w5g1`）跑、跑完即 `rm -rf`；Maven 仅用于聚焦测试与 T1/T2/T3 门禁；树内与副本并行时
树内只有一个 Maven；兄弟车道文件（含 `build_quest_client_report_pages.py` 与链登记表）**只读**；
AI 中间产物落 `.agents/summary/quest-native-dispatch/`。
