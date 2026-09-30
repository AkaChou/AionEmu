# P0c-8b：SimpleHunt 缺口表两个大桶按客户端契约逐行裁定（154 行 ADOPT_RETAIL → 退役）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-24
- 归属：真端任务驱动替换目标（P0c 线）
- 触发：P0c-8a 重算后的缺口表 275 行已按实测差异形状归 5 桶
  （`p0c8b-dialog-route-buckets.tsv`：A 47 / B 107 / C 13 / D 105 / F 3）；本切片裁定其中两个大桶
  ——**A 接取流偏差 47 行 + B 领奖确认段 107 行 = 154 行**——判据逐行机器可核，不靠人工目测。

## 1. 结论

1. **A_接取页（47 行，basis=`XML_ACCEPT_FLOW_DEVIATION`）**：旧 XML 的 `started` 态 `FINISH_DIALOG(1008)`
   用 `close-dialog`（**同文件的 `unaccepted` 态却是 `SHOW_SELECTION_PAGE page=SELECT_QUEST`，自相矛盾**），
   且多出一条 `SETPRO1(10000)` 接取捷径（`unaccepted -> started`）。真端两个源态都下发
   `SELECT_QUEST(10)` = 客户端全局任务簿页（`HtmlPages.xml: page_id 10 = HTML_PAGE_SELECT_QUEST`）。
   逐行断言该任务客户端 HTML 的按钮集**不含** `HACTION_SETPRO1/SETPRO2` ⇒ 那条捷径在客户端不可达、
   删除不可观测；语料 534 个已证等价行中 **499 行**用 `finish="SELECTION_DIALOG"`（= 完成后下发第 10 页）
   ⇒ 真端形状是语料常态、XML 是少数派。
2. **B_领奖确认段（107 行，basis=`RETAIL_REWARD_CONFIRM_RANGE`）**：旧 XML 的 `<npc-complete>` 只声明
   实际带可选奖励的那几条 `<choice>`（K 条）；真端下发完整确认段
   `SELECTED_QUEST_REWARD1(8)..SELECTED_QUEST_NOREWARD(23)`（16 条）。
   逐行断言 `xmlOnlyTransitions="-"`（XML 的每条转换在真端侧都有同签名对应）⇒
   **真端是 XML 的严格超集**，采纳不丢任何 XML 语义；语料 530/534 已证等价行用全段写法，窄写法是本批的少数派。
3. **154 行退役**：`catalog 2863 → 2709`、`RETAIL_TABLE 3361 → 3515`（本批 +154）；缺口表 **275 → 121**；
   裁定表 `retail-simple-hunt-adjudicated-decisions.tsv` **95 → 249**（本批 +154）；
   裁定行真端侧冻结 IR `retail-simple-hunt-adjudicated-ir-fingerprints.tsv` **93 → 247**（本批 +154）；
   已证等价冻结指纹 441 行不变（A/B 两桶不是"IR 等价"，是"真端超集/客户端不可观测"，故走裁定面而非等价面）。
4. 退役暴露的自因测试面 4 类、共 6 个失败方法，全部收口（§3）：`MonsterHuntFamilyDefinitionTest`
   （3 个：`started`/`k8`/`k9` 标签在真端网格里不存在，且领奖确认段从 2 条变 16 条）、
   `QuestBatchReportNpcAlignmentTest.quest2485ReportsAndCompletesAtLegacyEndNpc`（1 个：`k1` → 网格满段名 `a1`）——
   两者改按**按 (状态, 打包投影) 定位**（与等价门禁同口径），语义断言不变；
   `RewardOwnerTrimContractTest.henirOwnerKeyResolvesThroughTheDredgionFamily`（1 个：直读 4714.xml）改成生产视图 +
   退役登记断言；`RetailMetadataEquivalenceGateTest`（1 个：目录规模魔法数下限 `> 3000`，在并发批时已红）改为由 6224 恒等式锁定。
5. **验收**：`verify_retirement.py` = `catalog=2709 directory=2709 retired=3515 sum=6224 — OK`；
   T1 **42 例 / 1F**（唯一失败为并发会话 DataDriven 批的 `QuestClientContractGateTest` count=786）；
   T2 **260 例 / 3F / 0E**（3 个失败全部非本切片，见 §5.2）；T3 clean 见 §5.1（**本切片自因新增失败已归零**）。
6. **过程教训（写进 §3 的警示框）**：非 clean 的 `mvn test` 看不见退役断链——被删 XML 残留在
   `target/classes/...quest_definition/quests/`，直读 classpath XML 的测试会继续假绿；
   本切片 T2 全绿而 clean 副本 T3 立刻抓到 `RewardOwnerTrimContractTest`。
   **退役类切片只能以 clean 副本 T3 为准。**

## 2. 机器证据（可重跑）

| 证据 | 内容 |
|---|---|
| `p0c8b-bucket-decisions.tsv` | 154 行 owner 记录：`quest_id / verdict / basis / axes / evidence`（evidence 含 XML 的 `<choice>` 清单、客户端按钮集、确认段区间与语料统计） |
| `p0c8b_bucket_decisions.py` | 生成器（同一脚本内逐行断言，断言不过即中止并打印原因）：桶 A 断言"客户端无 SETPRO 按钮 + XML 有空集捷径 + 两条 FINISH_DIALOG 形态"，桶 B 断言"`retailOnlyTransitions=REWARD` + `xmlOnlyTransitions=-` + 有 `<choice>` 且声明数 < 16" |
| `p0c8-retention-diff-census.tsv` | 565 行差异轴普查（P0c-8a 产出，本切片的分桶输入） |
| `p0c8b-dialog-route-buckets.tsv` | 275 行缺口的分桶普查（A 47 / B 107 / C 13 / D 105 / F 3） |
| `simplehunt-dialog-route-gaps.txt` | 缺口表 275 → **121**（D 105 + C 13 + F 3，P0c-8c 工作面） |
| 客户端契约源 | `docs/quest/client-dialog-mapping/quest-dialog-action-details.csv`（逐任务按钮常量集）、`quest_monster.csv`（`SECTION_n` 门控） |

## 3. 代码 / 数据改动

| 文件 | 变化 |
|---|---|
| `src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv` | 95 → **249** 行（追加 154 行 ADOPT_RETAIL；既有 95 行零搅动） |
| `src/test/resources/quest/retail-simple-hunt-adjudicated-ir-fingerprints.tsv` | 93 → **247** 行（真端侧冻结 IR，本批 154 行） |
| `.agents/summary/scriptdll-quest-driver/simplehunt-dialog-route-gaps.txt` | 275 → **121** 行（移除两个大桶） |
| `src/test/java/.../definition/MonsterHuntFamilyDefinitionTest.java` | 绝对定位改成语义口径：新增 `rewardVars`（按 REWARD 态）、`gridMinVar0`/`gridMaxVar0`（START 态 var0 取值域）、`completionActions` 改为 **dialogId → 动作序列** 并断言确认段 = `REWARD1(8)..NOREWARD(23)` 全段、第 k 个可选项绑 `REWARD1+k`；奖励池顺序改按真端 quest.xml 声明序（内容不变） |
| `src/test/java/.../definition/QuestBatchReportNpcAlignmentTest.java` | 2485 的满段节点名 `k1` → 网格名 `a1`（沿用同文件 13702 先例的注释口径） |
| `src/test/java/.../definition/RewardOwnerTrimContractTest.java` | `definition(int)` 改到生产视图；Henir 同族旁证（4713/4714/4716）对**退役**同族行改断言"无 XML ⇒ 必须在保留清单登记为已退役"（`RetiredQuestIds`），仍留 XML 的行继续按 XML 文本断言 |
| `src/test/java/.../definition/RetailMetadataEquivalenceGateTest.java` | 删掉目录规模魔法数下限（`> 3000`，M4-b 时代的 3866 早已失效、且与同方法上一行的 6224 恒等式重复）：精确覆盖由恒等式锁定 |
| 生产 XML | 删除 154 个（`quests/1113.xml` … `quests/80455.xml` 等，见裁定表） |

> **方法论发现（本切片最重要的过程教训）**：**非 clean 的 `mvn test`（主工作树）看不见退役断链**——
> 被删的 XML 仍残留在 `target/classes/aion/data/static_data/quest_definition/quests/`，
> 于是直读 classpath XML 的测试继续"绿"。本切片 T2（`gates/T2-182352.log`，260 例 / 3F）
> 全绿，而 clean 隔离副本的 T3 立刻暴露 `RewardOwnerTrimContractTest`（读 4714.xml）。
> 结论：**退役类切片必须以 clean 副本的 T3 为准**，T2 只能当"选择器是否命中"的粗筛。

## 4. 逐桶裁定

### 4.1 桶 A：接取流偏差（47 行，`XML_ACCEPT_FLOW_DEVIATION`）

| 侧 | 形状 |
|---|---|
| 旧 XML | `unaccepted` 态 `FINISH_DIALOG` 下发 `SHOW_SELECTION_PAGE SELECT_QUEST`，`started` 态却用 `close-dialog`（自相矛盾）；另有 `SETPRO1(10000)` 捷径 `unaccepted -> started` |
| 真端 | 两个源态 `FINISH_DIALOG` 都下发 `SELECT_QUEST(10)`（客户端全局任务簿页） |
| 客户端 | 逐行断言任务 HTML 按钮集无 `HACTION_SETPRO1/SETPRO2` ⇒ XML 的捷径在客户端**不可达** |
| 语料 | 534 个已证等价行中 499 行用 `finish="SELECTION_DIALOG"` ⇒ 真端形状为常态 |

裁定：`ADOPT_RETAIL`（删除的捷径不可观测；真端补回的接取页是客户端真实页面）。

### 4.2 桶 B：领奖确认段（107 行，`RETAIL_REWARD_CONFIRM_RANGE`）

| 侧 | 形状 |
|---|---|
| 旧 XML | `<npc-complete>` 只声明 K 条 `<choice>`（本批逐行断言 K < 16） |
| 真端 | 确认段 = `SELECTED_QUEST_REWARD1(8)..SELECTED_QUEST_NOREWARD(23)` 全段 16 条，每条以 `CompleteQuest(0)` 收尾，第 k 个可选项绑 `REWARD1+k` |
| 关键判据 | 逐行断言 `xmlOnlyTransitions="-"` ⇒ **真端 ⊋ XML**，采纳不丢任何 XML 语义（XML 的每条转换都在真端侧有同签名对应） |
| 语料 | 530/534 已证等价行用全段写法；本批 132 行的窄写法是族内少数派 |

裁定：`ADOPT_RETAIL`（超集采纳；可选奖励→确认 id 的绑定关系不变，只是多发 14 条"无选项"确认位
——客户端只在任务书声明了选项按钮时才显示选项位，多出的确认 id 在客户端不可达）。

### 4.3 剩余 121 行（P0c-8c 工作面）

D 105（两侧都有差异：缺失 + 多余）+ C 13（真端少路由，XML 多出转换）+ F 3（其它）；
按 census 差异轴看是 `transition multisets differ 74` + `node sets differ 47`。

## 5. 验证

| 档 | 选择器 | 结果 | 日志 |
|---|---|---|---|
| 退役收口 | `verify_retirement.py` | **OK**（本切片检查点：catalog 2709 = 目录 2709、retired 3515、sum 6224、无悬空生产引用）；**随后并发会话在飞批把该脚本跑红**（`retired=3771 sum=6480` + 5 个已登记未删 XML：13968/15002/15010/15011/15012），属其批次中间态，见 §5.4 | — |
| 自因测试面（clean 副本复跑） | `MonsterHuntFamilyDefinitionTest` + `QuestBatchReportNpcAlignmentTest` + `RewardOwnerTrimContractTest` | **26 例 / 0F** | — |
| T1 | 固定门禁清单 | **42 例 / 1F**（唯一失败 = 并发会话 DataDriven 批，§5.2） | `gates/T1-182302.log` |
| T2 | T1 ∪ 本批 154 行命中类 | **260 例 / 3F / 0E**（3 个失败全部非本切片，§5.2；注意 §3 的方法论警示） | `gates/T2-182352.log` |
| T3 clean（首跑） | 整个 questEngine 测试树（forkCount=2，仓库外隔离副本） | **1960 例 / 31F / 64E / 1 skipped**；暴露 2 个自因失败（已修） | `gates/T3-p0c8b-clean.log` |
| T3 clean（收口复跑） | 同上（含 3 个自因测试收口后的源码） | **1960 例 / 29F / 64E / 1 skipped**；基线 21 个失败方法全部仍在，**新增 36 个全部归并发批**，**自因新增失败 0** | `gates/T3-p0c8b-clean2.log` |

（被中止/作废的两跑留档：`gates/T3-p0c8b-clobbered-probe.log`（首跑撞上并发会话在飞探针 `ZZProbeDDFixtureTest`）、
`gates/T3-p0c8b-clobbered-ddrung.log`（次跑卡在并发会话门禁 `RetailDataDrivenGateTest` 的单次校验 >13 分钟）。）

### 5.1 T3 clean 对账

命令（仓库外隔离副本 `/tmp/aion-p0c8b-t3`，用毕删除）：
`mvn -o -B clean test -Dtest='com.aionemu.gameserver.questEngine.**,!com.aionemu.gameserver.questEngine.retail.RetailDataDrivenGateTest' -DfailIfNoTests=false -DforkCount=2`

副本纪律（copy-only，逐项登记）：rsync 排除 `/target`、`/.git`、`/aion`、`/patch`（保留 `.agents`，因为
`QuestDialogMigrationGateTest` 读其中 git 跟踪的脚本）；副本内另行删除并发会话的两个在飞临时探针
（`ZZProbeDDCollectTest.java` / `ZZProbeDDFixtureTest.java`，基线跑里也没有它们、属临时产物），
并用选择器排除并发会话在飞门禁 `RetailDataDrivenGateTest`（基线里尚无该类；其单次
`QuestDefinitionCompiler.sourceNodesAreMutuallyExclusive` 校验实测 >13 分钟 100% CPU，已作为协调项登记）。

| 对账 | 结果 |
|---|---|
| 与 `gates/T3-p0c8-clean.log`（1961 例 / 8F / 13E / 1 skipped）比失败方法集（归一化 `comm -3`） | 基线 21 个指纹**全部仍在**（无掩盖、无消失），本次 57 个 = 21 + 36 新增 |
| **自因新增失败（收口后）** | **0**（首跑的 2 个自因失败已修并复跑验证：clean 副本内 26/0F） |
| 例数 −1 | 选择器排除了并发会话的 `RetailDataDrivenGateTest`（基线无、本次首次出现） |
| 并发 build 污染标记（`NoClassDefFoundError` / `ClassNotFoundException`） | **0** |

### 5.2 非本切片的失败（归属与证据）

| 失败 | 归属 | 依据 |
|---|---|---|
| `QuestClientContractGateTest… count=786` | **并发会话 DataDriven P5-1 批** | 在副本内把 `MAX_REPORTED_FINGERPRINTS` 抬到 1000 复跑：786 条指纹**全部**属于 424 个 DataDriven 任务（`basis=DD_TALK_HUNT_GRID`），与本切片 id 集合**零交集** |
| `QuestDraupnirNpcVariantContractTest.liveNpcVariantsPreserveQuestDropContracts` | 既有基线（并发会话禁改区） | 与 `T3-p0c6-clean.log` / `T3-p0c8-clean.log` 同指纹 |
| `FissureOfOblivionInstanceTest.hiddenRoomControllersCannotInterceptBossAttacks:102` | **HEAD 既有，非本切片亦非 questEngine 树** | 断言的是**AI 源码文本**：`spawn(244490, 301.1525f, 512.97736f, 350.8281f, (byte) 0);` 在 HEAD 的实现里位于 `threadPoolManager().schedule(() -> spawn(...), 2000)` 闭包内 ⇒ 字面量不匹配（`git show HEAD:<file>` 命中数 0）；`src/test/java/.../FissureOfOblivionInstanceTest.java` 与该 AI 源文件**均未修改**（`git status` 干净，AI 文件 mtime 2026-09-22）⇒ 与任务退役无关，登记为外部项（修法是断言 `schedule(` 包裹形态或改实现为直接 spawn） |

### 5.3 T3 clean 相对基线的 38 个新增失败：逐个归属（首跑 2 本切片已修 → 收口复跑新增 36 全部归并发批）

**归属方法**（不靠人工目测）：失败方法消息里的 `missing quest definition <id>.xml` / `missing quest <id>` /
`missing resource …<id>.xml` / 断言里的 quest id → 查 `retail-xml-retention.tsv` 的 reason 列 → 看是哪一批退役的；
无 id 线索的（人口/目录抽查类）另算。

| 归属 | 数量 | 代表与证据 |
|---|---:|---|
| **本切片（已修）** | **2** | ① `RewardOwnerTrimContractTest.henirOwnerKeyResolvesThroughTheDredgionFamily`：读 4714.xml（本批退役）→ 改生产视图 + 退役登记断言，clean 副本复跑 **7/7 绿**；② `RetailMetadataEquivalenceGateTest`：目录规模魔法数下限 `> 3000`（并发会话 P5-1 后目录已 2863、本批 2709，**该门禁在并发批时已红**）→ 改为由同方法 6224 恒等式锁定，clean 副本复跑 **1/1 绿** |
| 并发会话 DataDriven P5-1 批 | 34 | 逐类点名 id 全为 `p5-datadriven-decisions.tsv basis=DD_TALK_HUNT_GRID`：`19637`（2 例）、`28932`（3 例）、`1514/15041/19636/19640/19673/25002/25090/25093/25512/25640/25698/29634/50126/19631/19638/25500/16988`（各 1+ 例）等；涉及类 `Quest19636/19637/19640*`、`Quest25512/25640/25698*`、`Quest28932RewardRowContractTest`、`GrowthQuestDialogPageAlignmentTest`、`AlignedMirrorRewardRowContractTest`、`MirrorRewardProjectionLagContractTest`、`JournalRewardRowRepairContractTest`、`QuestLegacyMonsterHuntProductionFlowTest`、`QuestNoHandlerShard3DefinitionTest`、`QuestMonsterProgressContractAuditTest`、`QuestFactRequirementsTest`、`QuestSection0ReportRowContractTest`、`QuestReportedRewardCoverageTest`、`QuestIlumaNorsvoldKillTargetCoverageTest`、`QuestPrematureRewardRouteExclusionTest.fiveKillsNotOneUnlock16988ReportStage` 等 |
| 并发会话 DataDriven 人口/门禁 | 2 | `QuestKillCounterRetailGateTest`（断言"声明 `<kills>` 的任务 ≥90"）：**实测**当前目录 50 行声明、本批只删掉其中 **1** 行、并发批删掉 **44** 行（50+1=51 < 90 ⇒ 与本批无关）；`RetailSimpleUseItemGateTest.driftVersusLegacyXmlIsRegistered`（并发会话自述"漂移文件被并行会话循环覆写、暂不验证"） |
| 既有基线（与本切片无关） | 0 新增 | 基线 21 个失败方法**全部仍在**（无一个消失/被掩盖） |

**收口复跑（`gates/T3-p0c8b-clean2.log`）后的新增失败 = 36 个，全部为并发会话 DataDriven 批；
本切片自因新增失败 = 0。** 修后的两个类在同一个 clean 副本内单独复跑：26 例 / 0F（§5）。

### 5.4 收尾时的共享文件中间态（并发会话在飞，非本切片）

本切片自己的检查点上 `verify_retirement.py` = OK。收尾复查时（同一天更晚）该脚本变红：

```
catalog=2709 directory=2709 retired=3771 sum=6480
FAIL: retired quests still in catalog: ['13968', '15002', '15010', '15011', '15012']
FAIL: catalog+retired=6480 != universe 6224
```

含义：保留清单已登记 3771 行退役，但其中 256 行（含上面 5 个 id）的 XML 仍在目录里 —— 这是**并发会话正在进行的下一批退役的中间态**
（本切片的 154 行在其检查点上已逐一对账：`catalog 2709 + retired 3515 = 6224`）。
**不要**据此回滚本切片；等并发会话批次落地后再复跑该脚本即可。

## 6. 未落地 / 后续

| 项 | 数量 | 说明 |
|---|---|---|
| 缺口表剩余行逐行裁定 | **121** | D 105 + C 13 + F 3（**P0c-8c**） |
| 9622（串行族 `talk_npc1`） | 1 | 仍留 XML（P0c-5 登记） |
| 旧档 DB 归一化 | 16 条边 / 12 任务 | **可选**（P0c-6 登记，未执行） |
| `FissureOfOblivionInstanceTest` 文本断言 | 1 方法 | 外部项，见 §5.2（不属本族，未动） |
