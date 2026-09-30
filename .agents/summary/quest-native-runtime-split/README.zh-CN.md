# quest-native-runtime-split：XML 与真端运行时隔离 + 客户端本地翻页退场（Phase 2）

> 用户 Goal（2026-09-30 采纳）：「摒弃当前的任务引擎，完全按真端来，当前任务引擎只负责现有的任务 XML 运行。」
> 本轮把生产目录按 owner 拆成两条**互斥**运行时，并把「阶段内翻页」从服务端逐页路由中摘除：
> 真端 owner 的页内导航交由客户端本地完成，服务端只确认收到。Maven / git 已授权（不 push、不 `git add -A`）。

## 1. 运行时隔离（引擎层）

| 组件 | 位置 | 作用 |
|---|---|---|
| `QuestRuntimeDispatcher` | `questEngine/runtime/` | 统一入口接口（catalog/owns/hasRoutes/dispatch/dispatchOwners/奖励窗/共享接取/状态变更） |
| `QuestRuntimeRouter` | `questEngine/runtime/` | 路由到唯一 owner 子运行时；重复 owner fail-fast；`questId=0` 按 `QuestDispatchContract` 组合两子运行时结果 |
| `QuestProductionDispatcher` | `questEngine/runtime/` | 新增共享 `QuestExecutionCoordinator` 的 `production(...)` 重载（XML 与真端仍按玩家串行） |
| `QuestEngine#splitRuntimeCatalogs` | `questEngine/QuestEngine.java` | 用 `RetailQuestDriver.current().retailOwnedIds()` 把生产目录拆成 XML / 真端两个互斥子目录，分别构建 dispatcher 并做交互对象校验 |
| `RetailQuestDriver#retailOwnedIds` | `questEngine/retail/` | 真端模板表 owner 只读快照 |

- 合成目录（单测/工具）无真端 owner 时保持单目录兼容（不影响既有 XML 工具）。
- `QuestInteractionObjectValidator` 对两个子运行时分别校验；`QuestProductionEventWiring` 仍遍历合并目录。

## 2. 真端本地对话页（客户端驱动）

- 新增 `RetailDialogIntent`（`LOCAL_PAGE_NAVIGATION` / `LOCAL_CLOSE`）与 `RetailDialogIntentClassifier`：
  `FINISH_DIALOG(1008)` → 本地关闭；生命周期动作（`QUEST_SELECT`/`SELECT*`/`SETPRO*`/`SET_SUCCEED`/
  `CHECK_USER_HAS_QUEST_ITEM*`/接受拒绝/奖励确认/`SELECT_QUEST_REWARD`）不判为本地翻页；其余动作只有在
  该任务 `client_dialog_contract.tsv` 里登记为页面时才算本地翻页。
- `QuestRuntimeRouter` 对真端 owner 且**显式无匹配路由**的本地动作返回已处理 no-op（不改任务状态），
  其余 miss 仍如实未处理；XML owner 绝不进入该分支。
  - 判据必须用 `hasMatchingRoutes`（事件索引按 NPC 宽键登记对话路由，`hasRoutes` 会把「同 NPC 其它对话」
    误判为已登记路由）。

## 3. 页梯退场（编译层）

| 家族 | 变更 |
|---|---|
| SimpleTalk（`RetailSimpleTalkDefinitionCompiler`） | 阶段首屏由客户端契约推导（`select2..`，`RetailQuestDialogPages`）；阶段内 `SELECTn_m` 逐页路由全部删除；`SETPRO{i}` / `SET_SUCCEED` 语义边保留；过场只挂在真端 `cs1_haction` 所属阶段 |
| DataDriven Talk 链（`RetailDataDrivenTalkCompiler#buildChain`） | 同上（首屏 `select1..`）；推进按钮 = `SETPRO{i+1}`；末阶段 `SET_SUCCEED` 与 `SETPRO{N}` 同义收口领奖态；旧「单步 s1 别名边」删除；不再消费 `quest_client_talk_chain_pages.tsv` 页梯 |
| DataDriven 门（`RetailDataDrivenDefinitionCompiler`） | `RETAIL_TALK_CHAIN_DEFERRED` 判据改为「客户端契约缺阶段首屏」；不再按页梯登记表判阶段数 |

`RetailQuestDialogPages`（新增）：按客户端契约页名 `select{N}` 推导阶段首屏；同名页缺失时退回标准
`selectN` 页 id（`1011 + 341*(N-1)`）；两者都没有则返回空（**不发明页**，由调用方如实拒绝）。

### 证据（真端表 + 客户端页面合同）

- `data_driven_quest.xml` 的 81 条 allTalk 行：44 条在 `quest_client_talk_chain_pages.tsv` 有登记，
  其阶段首屏页名 100% == `select{i+1}`（0 处不符）；中间阶段推进动作 100% == `SETPRO{i+1}`；
  末阶段推进动作 ∈ {`SET_SUCCEED(10255)` ×38, `SETPRO{N}` ×6}（6 条均为单步）。
- 按页名推导与旧「标准页 id 列表」推导对全部 81 行的首屏 id 与接受/拒绝判定**逐行一致**（0 差异）。

## 4. 验证（本轮实跑）

| 轮次 | 命令 | 结果 |
|---|---|---|
| 编译 | `mvn -q -DskipTests compile` | 通过 |
| 聚焦门 | `mvn -q -Dtest='QuestRuntimeRouterTest,QuestEngineRuntimeCompositionTest,RetailSimpleTalkGateTest,RetailDataDrivenGateTest,RetailQuestDriverOverlayTest,QuestProductionStartupGateTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest,QuestEngineNpcDialogDispatchTest' test` | 50 例，唯一红 = `80817` 既存漂移（非本片） |
| questEngine 包内全量 | `mvn -Dtest='com.aionemu.gameserver.questEngine.**.*Test' test` | HEAD 基线 1853 例 / 红身份 245 条；本片（提交后复跑）1855 例 / 红身份 245 条 → **ADDED 0 / REMOVED 0**（`diff` 逐行相同） |
| 全量 | `mvn test` | HEAD 基线 3968 例（188F+70E）；本片 3970 例（187F+71E）→ 红身份集 **ADDED 0 / REMOVED 0**（258→258，含错误与失败） |

- 窗口内冻结指纹：`src/test/resources/quest/retail-data-driven-ir-fingerprints.tsv` 逐行核对 id 集
  **不变**（1448 行），仅按本轮形状变更重冻 **36 行**（全部为 DD allTalk 行）；`80817` 是 HEAD 既存漂移，
  **刻意不回写**，保持与基线红同身份。
- 未执行：真实客户端验收、服务器启动/重启（按纪律由用户管理生命周期）。

## 5. 与基线的差异（诚实边界）

- 有 3 个**本来就红**的锁定测试，本轮红因从「旧形不匹配」变为「新形缺页梯路由」：
  `Batch40ThreeNpcTalkLadderContractTest#rowOwnersDriveTheClientPageChain`、
  `Quest1152RetailAlignmentTest#followsTheClientChefDialogAndLegacyTwoStepItemContract`、
  `Quest1163ClientDialogAlignmentTest#followsTheRetailPotionHandoffAndRewardOwner`
  （身份集不变，仍需单独一轮"锁定测试重锚"把它们改成新合同）。
- 4 条 SimpleTalk 表行（`9570/9572/13816/23816`）不在 6224 owner 清单内，未被生产编译；其客户端契约
  缺少 `select2` 阶段首屏，新判据会 fail-closed（旧行为是硬编码下发 1352）。

## 6. 下一批（未做）

1. 混合链 `RetailDataDrivenTalkHuntChainCompiler` / `RetailDataDrivenTalkCollectChainCompiler` 仍消费
   `quest_client_talk_chain_pages.tsv` / `quest_client_talk_collect_chain_pages.tsv` 的 talk 段页梯；
   按本轮同法摘除（collect 段的 39/20002 检查与好/坏结果页是语义边，必须保留）。
2. 页梯 TSV 消费点清零后，`RetailClientTalkChainPages.STEP_PAGE_HEADS` 与两个 `quest_client_*_chain_pages.tsv`
   才能整体退役（含测试资源副本）。
3. 锁定测试重锚批次（页梯断言 → 本地翻页 no-op + 阶段首屏断言）。
