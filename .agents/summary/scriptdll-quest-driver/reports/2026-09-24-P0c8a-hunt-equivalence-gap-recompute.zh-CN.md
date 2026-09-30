# P0c-8a：SimpleHunt 等价缺口按当前编译器重算（155 行可证等价 + 2 行窄投影 → 退役 157）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-24
- 归属：真端任务驱动替换目标（P0c 线）
- 触发：P0c-6 收口时登记——`simplehunt-dialog-route-gaps.txt` / `phase5-3-rejections.txt` 是 M2-c
  时代导出的**旧口径**清单，此后合成器持续补齐接线（P0c-2/3/4/5/6），但清单从未按当前编译器重算，
  保留清单因此把"其实已经等价"的行继续登记为 `SEMANTIC_GAP:DIALOG_ROUTE`。

## 1. 结论

1. 用当前编译器 + 现役客户端契约 registrar 对**全部 565 个 SimpleHunt 保留行**实测 IR 差异轴
   （临时 JUnit 探针，源码留档 `p0c8_retention_diff_probe.java.txt`，用后删除）：
   **155 行已可证 IR 等价**（节点集合 + 转换多重集（含事件/条件/动作/after-commit/优先级）+ 进度布局三项全等）。
2. **157 行退役**（155 等价 + 2 行窄投影），`catalog 3448 → 3291`、`RETAIL_TABLE 2776 → 2933`：
   - 155 行 basis=`EQUIVALENCE_PROVEN`（冻结 IR 指纹承担等价证明，`retail-simple-hunt-ir-fingerprints.tsv`
     286 → **441** 行）；
   - 2 行 basis=`XML_LEGACY_PROJECTION`（11151 / 18313：XML 用历史窄位宽 width=1/3，真端用 6 位
     SECTION 网格；客户端 `SECTION_n` 门控在两种位宽下**不可区分**（count<8 时打包位相同）→
     按族形状权威取真端网格；裁定行 `retail-simple-hunt-adjudicated-ir-fingerprints.tsv` 91 → **93** 行）。
3. 旧口径证据面同步重算：`simplehunt-dialog-route-gaps.txt` **434 → 275 行**、
   `phase5-3-rejections.txt` **24 → 23 行**（删除实测已等价的 80690）；两者与保留清单
   `SEMANTIC_GAP:DIALOG_ROUTE` 行数**逐一对账为零差集**。
4. 退役暴露 4 个直读退役 XML 的测试类（1346/1347/1376 客户端对话对齐 + `QuestPrematureRewardRouteExclusionTest`
   的 `load(int)`），已按 P0c-6 先例改到生产视图（XML 目录 + 真端 overlay）；前三个另配新的语义定位工具
   `QuestProjectionNodes` 按 **(状态, 打包投影)** 找节点——真端网格标签（`a1..a9`）与旧 XML 标签
   （`k1..k9`）不同，锁合同不能再按标签断言。
5. **验收**：`verify_retirement.py` = `catalog=3291 directory=3291 retired=2933 sum=6224 — OK`；
   T1 **42/0F**；T2 **411 例 / 2F / 2E**（4 个失败方法全部为既有基线）；
   T3 clean **1961 例 / 8F / 13E** 且失败方法集与 `T3-p0c6-clean.log` 归一化 `comm -3` **零差异（自因新增失败 0）**。

## 2. 机器证据（可重跑）

| 证据 | 内容 |
|---|---|
| `p0c8-retention-diff-census.tsv` | 565 行逐行差异轴：`EQUAL 155` / `transition multisets differ 248` / `node sets differ 52` / `REJECTED:RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH 84` / `REJECTED:RETAIL_COUNTER_EXCEEDS_6BIT 13` / `REJECTED:RETAIL_MONSTER_UNRESOLVED 11` / `progress layouts differ 1`（11151）/ `RAW_node sets differ 1`（18313） |
| 探针口径 | 真端侧 = `RetailSimpleHuntDefinitionCompiler.compile(plan, metadata, 客户端登记表×5)`；XML 侧 = `RetailKillTargetXml.expand` + `QuestDefinitionXmlCompiler`；判定 = `RetailSimpleHuntEquivalenceGateTest.irEquivalenceProblem`（门禁同一函数，非另写一套） |
| 18313 的口径退化 | 展开口径下 XML 报 `AMBIGUOUS_TRANSITION: KILL_NPC`（每槽列 3 个种族变体 id，名族展开后同槽路由重叠）→ 退回未展开口径再比：node sets differ（XML 4 节点 width=1 历史窄投影 vs 真端 11 节点 6 位网格） |
| 客户端门控 | `Quest_unpacked/quest_monster.csv`：11151 = `SECTION_0<4; SECTION_5==0` / `SECTION_1<4; SECTION_5==0`；18313 = `SECTION_0<1 / SECTION_1<1 / SECTION_2<1`（均为 6 位 SECTION 字段） |
| `p0c8-equivalence-retire-decisions.tsv` | 157 行 owner 记录（verdict / basis / axes / 证据含节点·转换计数与客户端门控原文） |

## 3. 代码改动

| 文件 | 变化 |
|---|---|
| `questEngine/definition/QuestProjectionNodes.java`（新，测试工具） | 按 (状态, `ProgressLayout.pack` 后的投影) 定位节点/转移：`node` / `routes` / `talkRoutes`（3 参 / 4 参两版）/ `singleTalkRoute` / `assertTarget` / `targetNpcs`；口径与家族等价门禁一致 |
| `Quest1346ClientDialogAlignmentTest.java` | 改到 `ProductionQuestDefinitions.definition(1346)` + 语义定位（旧 `k9`→(START,var0=9)、`reward`→(REWARD,9)、`complete`→(COMPLETE,0)…），断言内容不变 |
| `Quest1347ClientDialogAlignmentTest.java` | 同上（`unaccepted`→(NONE,0,0)、`a7b3`→(START,7,3)、`reward`→(REWARD,7,3)） |
| `Quest1376ClientDialogAlignmentTest.java` | 同上（`k7`→(START,7)、`reward`→(REWARD,7)） |
| `QuestPrematureRewardRouteExclusionTest.java` | `load(int)` 改到生产视图（classpath 直读 XML → `ProductionQuestDefinitions.definition`），其余断言不变；删掉随之失去用途的 `InputStream`/`Objects` 导入 |
| 数据 | `retail-simple-hunt-ir-fingerprints.tsv` 286→441；`retail-simple-hunt-adjudicated-ir-fingerprints.tsv` 91→93（头注释加 P0c-8a 批）；`retail-simple-hunt-adjudicated-decisions.tsv` 93→95（+2 窄投影，前 91 行零搅动） |
| 生成器 | `p0c8_recompute_gap_list.py`（新，重算缺口表/拒绝表 + 写退役记录 + 登记裁定行；修掉旧 `gap_ids()` 把注释里的数字当 id 的解析缺陷） |

## 4. 逐行裁定

### 4.1 155 行 `EQUIVALENCE_PROVEN`（退役）

判据与 M2-e 的 286 行同一函数、同一口径：节点集合（按 状态+打包投影）、转换多重集（source/target
规范化键 + 事件文本 + 条件 + 动作 + after-commit + 优先级）、进度布局三项全等。旧 XML 与真端网格的
**节点标签可以不同**（等价判据本就不看标签），这正是标签型测试必须改造的原因（§1.4）。

### 4.2 2 行 `XML_LEGACY_PROJECTION`（退役，真端优先）

| 任务 | XML 侧 | 真端侧 | 裁定 |
|---|---|---|---|
| 11151 | `var0/var1 width=3`（max 4），节点与转换多重集与真端**全等** | 6 位 SECTION 网格 | 只有位宽不同；客户端门控 `SECTION_0<4`/`SECTION_1<4` 在 count<8 时两种位宽打包位相同 → 客户端不可区分，真端网格为族形状权威 |
| 18313 | 4 节点、每槽 3 个种族变体 id、width=1；展开口径下自冲突 | 11 节点 3 槽 6 位网格 | 展开口径不可用 + 未展开口径只差历史窄投影；客户端门控 `SECTION_0..2<1` 同样不可区分 → 真端网格为准 |

### 4.3 275 行留在缺口表（P0c-8b 工作面）

`SEMANTIC_GAP:DIALOG_ROUTE` 275 行按实测差异轴分桶（P0c-8b 逐行裁定）：
`transition multisets differ 248`（含「接取页 ShowQuestSelectionDialog vs 旧 XML CloseDialog」46 行、
「奖励对话 id 10..15 展开」25+7+5+4 行、「击杀目标多重集」4+4 行等）、`node sets differ 52`、
编译器拒绝 108（84 挑战哨兵 + 13 六位溢出 + 11 怪物名未解，均有稳定码登记）。

## 5. 验证

| 档 | 选择器 | 结果 | 日志 |
|---|---|---|---|
| 退役收口 | `verify_retirement.py` | **OK**（catalog 3291 = 目录 3291、retired 2933、sum 6224、无悬空引用） | — |
| 探针普查 | `P0c8RetentionDiffProbeTest`（临时，已删） | 565 行全量差异轴导出，见 §2 | `p0c8-retention-diff-census.tsv` |
| T1 | 固定门禁清单 | **42 例 / 0F / SUCCESS / 35s** | `gates/T1-151526.log` |
| T2 | T1 ∪ 157 行命中类 | **411 例 / 2F / 2E**（4 个失败方法全部既有基线，见 §5.2） | `gates/T2-152018.log` |
| T3 clean | 整个 questEngine 测试树（forkCount=2） | 见 §5.1 | `gates/T3-p0c8-clean.log` |

### 5.1 T3 clean 对账

命令：`mvn -o -B clean test -Dtest='com.aionemu.gameserver.questEngine.**' -DfailIfNoTests=false -DforkCount=2`
（仓库外隔离副本 `/tmp/aion-p0c8-t3`，用毕删除）。

**结果（`gates/T3-p0c8-clean.log`）：1961 例 / 8F / 13E / 1 skipped / BUILD FAILURE / 6:24**。

| 对账 | 结果 |
|---|---|
| 与 `gates/T3-p0c6-clean.log`（1958 例 / 8F / 13E / 1 skipped）比失败方法集 | **归一化 `comm -3` 零差异**（21 = 21 指纹） |
| **自因新增失败** | **0** |
| 例数 `+3` | 并发会话在飞门禁（非本切片） |
| 并发 build 污染标记（`NoClassDefFoundError` / `ClassNotFoundException`） | **0** |

首跑（`gates/T3-p0c8-clobbered.log` 同批）暴露的 2 个自因失败已定位并修掉：
`QuestPrematureRewardRouteExclusionTest` 的 `incompleteProgressCannotRewardAndCompletedProgressCanReport` /
`gateRejects1347ReportWithOnlyOneCounterComplete` —— 该类的 `load(int)` 用 classpath 读
`quest_definition/quests/<id>.xml`，1347 退役后 NPE；改为 `ProductionQuestDefinitions.definition(id)`
（生产视图对未退役任务返回同一 XML 定义，语义不变）后 1347 相关用例全绿。

### 5.2 非本切片的既有失败（T2）

| 失败 | 归属 | 依据 |
|---|---|---|
| `LegacyTemplateMirrorRouteRegressionTest… quest 1309` | SimpleUseItem（P3b） | 1309 报告页 SETPRO1 出口缺口（P0c-7 收敛项） |
| `ReportToManyDialogRouteRegressionTest… quest 3914` | 既有基线 | 与 `T3-p0c5-clean.log` 同指纹 |
| `EarlyElyosQuestRegressionTest.flowerDeliveryUnlocks…` | 既有基线 | 同上 |
| `QuestMutationPlannerTest.ringForLuckRemoves…` | 既有基线 | 同上 |

## 6. 未落地 / 后续

| 项 | 数量 | 说明 |
|---|---|---|
| 缺口表剩余行逐行裁定 | 275 | **P0c-8b**（已开桶，`p0c8b-dialog-route-buckets.tsv`）：`B_领奖对话 id 展开（真端多份同构 REWARD 路由 dialogId 10..15）` **107** / `D_两侧都有差异（缺失+多余）` **105** / `A_接取页（XML CloseDialog+可见性修复边 vs 真端 ShowQuestSelectionDialog）` **47** / `C_真端少路由（XML 多出）` **13** / `F_其它` **3**；逐桶按客户端契约裁定 |
| 其余差异行 | 251 | 565 保留行 − 157 退役 − 108 编译器拒绝 − ... = 上述 275 的一部分（同族不同登记源） |
| 9622（串行族 `talk_npc1`） | 1 | 仍留 XML（P0c-5 登记） |
| 旧档 DB 归一化 | 16 条边 / 12 任务 | **可选**（P0c-6 登记，未执行） |
