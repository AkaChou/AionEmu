# quest-mixed-chain-native：混合链页梯退役 + 阶段页族派生（Phase 3）

> 用户 Goal（2026-09-30 采纳）：「摒弃当前的任务引擎，完全按真端来，当前任务引擎只负责现有的任务 XML 运行。」
> 本片把 Phase 2 的「客户端本地翻页」推进到**混合链**（talk/collectitem、talk/hunt/collectitem、talk/EA/EW/itemplay/FOBJ/PVP），
> 逐任务页梯 TSV 消费点清零并整表退役。Maven / git 已授权（不 push、不 `git add -A`）。

## 1. 引擎层（编译期）

| 组件 | 变化 |
|---|---|
| `RetailQuestDialogPages` | **新增** `COLLECT_CHECK_ACTION_ID(39)` / `COLLECT_OK_PAGE_ID(10000)` / `COLLECT_FAIL_PAGE_ID(10001)` 与 `advanceActions(stagePage, finalStage)`：中间段 = `SETPRO{K}`，末 talk 段 = {`SETPRO{K}`, `SET_SUCCEED`, `SELECT_QUEST_REWARD`} 三种客户端收尾按钮。`stage()/familyCount()/stageIndexForAction()` 的语义改为**客户端声明的 select 页族序**（页族号可跳号） |
| `RetailChainCutscenes`（新） | 段尾过场（客户端 HTML `<CutScene>`）常量账：11 行，每条标注客户端页名与旧壳 XML 见证；段下标 = 可见段序 |
| `RetailDataDrivenTalkCollectChainCompiler` | 不再消费页梯登记表：段首屏按声明页族取、段内导航边删除、collect 段用固定 39/10000/10001、末 talk 段收三种收尾按钮、过场改读常量账 |
| `RetailDataDrivenTalkHuntChainCompiler` | 同上（含 talk/EA/EW/itemplay/FOBJ/PVP 混合词汇）；hunt/骑行者仍占行不占段 |
| `RetailDataDrivenTalkCompiler#buildChain` | 常量改名为 `ACQUIRE_PAGE_FAMILIES`；首屏与推进按钮改由声明页族派生 |
| `RetailDataDrivenDefinitionCompiler` / `RetailQuestDriver` | 摘掉两个页梯登记参数与装配点 |

**退役文件**：`src/main/resources/quest/quest_client_talk_chain_pages.tsv`、`quest_client_talk_collect_chain_pages.tsv`
（含 `src/test/resources/quest/` 副本）与读取器 `RetailClientTalkChainPages` / `RetailClientTalkCollectChainPages`
（含其硬编码 `STEP_PAGE_HEADS = List.of(1011, 1352, …)`）。

## 2. 证据（客户端数据逐行核对，脚本只读）

| 断言 | 规模 | 结果 |
|---|---|---|
| 段首屏 = 客户端声明的第 i 个 select 页族首页 | 混合采集登记 86 行 × 全段 | **0 处不符** |
| 同上（链式信件登记） | 124 行 × 全段 | **0 处不符**（唯一 7 处不符全在 XML_RETENTION 行 3056/14220/24220） |
| 中间 talk 段推进 = `SETPRO{K}`（K = 该段页族号） | 194 段 | 100%（RETAIL_TABLE 行） |
| 末 talk 段推进 ∈ {`SETPRO{K}` ×38, `SET_SUCCEED` ×37, `SELECT_QUEST_REWARD` ×6} | 81 行 | 100% 落三按钮内 |
| collect 段 = (39, ok 10000, fail 10001) | 68 段 | 100% |
| 段尾过场 = 客户端 HTML 尾页 `<CutScene>`（15604/15605/15613/16942/25602/25604/26942 另有旧壳 `play-movie` 见证） | 11 行 | 逐页核对一致 |
| 新可编译 7 行（10112/10527/17540/20031/20112/20527/27540）派生段首与客户端页序 | 7 行 | 逐页一致（页首 1:1） |

**保留（语义边，不得按本片删除）**：collect 段 39 检查 + 好/坏结果页、`FINISH_DIALOG(1008)` 关窗出口、
落点换人补边、奖励窗与确认段、接取规范形、掉源自环与交互物合同。

## 3. 验证（本轮实跑）

| 轮次 | 命令 | 结果 |
|---|---|---|
| 编译 | `mvn -q -DskipTests compile` / `test-compile` | 通过 |
| 聚焦门 | `mvn -q -Dtest='QuestRuntimeRouterTest,QuestEngineRuntimeCompositionTest,RetailSimpleTalkGateTest,RetailDataDrivenGateTest,RetailQuestDriverOverlayTest,QuestProductionStartupGateTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest,QuestEngineNpcDialogDispatchTest' test` | 50 例，唯一红 = `80817` 既存漂移（非本片） |
| questEngine 包内全量 | `mvn -Dtest='com.aionemu.gameserver.questEngine.**.*Test' test` | 1855 例；红身份集对 HEAD 基线 `ADDED 0 / REMOVED 0`（203→203） |
| 全量 | `mvn test` | 3970 例（187F+71E）；红身份集 `ADDED 0 / REMOVED 0`（216→216） |
| 指纹重冻 | `-Dretail.dataDriven.fpOut=…` | id 集 **1448 行不变**，重冻 **130 行**（109 混合链 + 21 talk×EA/EW/itemplay；节点数 0 变化，仅变迁数变化）；`80817` 刻意不回写 |
| 漂移登记重算 | `-Dretail.dataDriven.equivOut=…` | 仅 7 行改判为 `REJECTED:CURATED_LEGACY_CONTRACT_LOCK`（其余 1441 行逐字相同），表头计数刷新 |

- 锁定测试重锚（已在册红的**红因**迁移，身份集不变）：`QuestDaevanionThreeStageFlowTest#nodesAndClientPagesKeepAllThreePhasesReachable`
  与 `#wrongOwnerOrOutOfOrderActionsCannotSkipTheMaterialPhase`、`QuestCradleReunionProductionFlowTest#preservesProductionProgressDialogAndRewardFlow`、
  `CollapsedSingleStepLadderContractTest#hemellosShardWalksTheClientActionLadder` / `#milliardShardWalksTheClientActionLadder`
  已改为「段首屏 + 推进按钮 + 本地翻页无路由」断言。
- 未执行：真实客户端验收、服务器启动/重启（按纪律由用户管理生命周期）。

## 4. 诚实边界

1. **7 行按 curated 暂缓（仍留 XML）**：页梯登记退役后 `10112/10527/17540/20031/20112/20527/27540` 不再被「页梯缺行」挡住，
   编译器可过；但翻转为真端 owner 属于生产行为变更（各自需合同/真机证据批）⇒ 本批在
   `RetailDataDrivenGateTest#CURATED_DEFERRED` 登记为暂缓，`retail-xml-retention.tsv` 保留 `XML_RETENTION` +
   `ADJUDICATED:CURATED_LEGACY_CONTRACT_LOCK`。**没有任何任务在本批静默换 owner。**
2. 末 talk 段的三按钮是**客户端末段页尾的收尾按钮全集**（逐行证据 100% 落在这三个 id 内）；服务端不再读页梯，
   因此无法逐行区分该行实际声明的是哪一个 ⇒ 三条同义边都登记（区间外按钮不会被发明）。
3. 仍有 3 个**本来就红**的锁定测试（`Batch40ThreeNpcTalkLadderContractTest` ×3 方法、`Quest1152RetailAlignmentTest`、
   `Quest1163ClientDialogAlignmentTest`），红因从「旧形不匹配」迁移为「新形无页梯路由 + 节点标签差异」，待单独重锚批次。
