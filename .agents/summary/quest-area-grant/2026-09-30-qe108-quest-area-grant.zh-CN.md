# QE-108：真端 quest_area 区域接取轴（进区域发放 + 采集交付行）与 6 行 owner 翻转

> 用户 Goal（2026-09-30）：「摒弃当前的任务引擎，完全按真端来，当前任务引擎只负责现有的任务 XML 运行。」
> QE-107 收尾时把 `RETAIL_ACQUIRE_GRANT_UNSUPPORTED`（8 行，全部 `category_acquire_=EnterArea`）记为「另轴」。
> 本片落地该轴：**真端世界文件的 `quest_area` 绑定就是发放源**，绑定即放行；无绑定保持 fail-closed。

## 1. 判据（绑定即发放，禁止按形状猜）

1. **发放权威 = 真端世界文件的 `quest_area` 绑定**：`ai-areas.xml` 的
   `<quest_area world_id=... name=... quests="a,b,c">` 里逐 id 包含该任务 ⇒ 玩家走进该区域时由
   `RetailAreaEngine` 直接 `QuestService.startQuest(...)` 发放，**不需要也不存在接取 NPC**。
2. **采集交付行**（本片 6 行的第二步）：`progress_info` 全为 `collectitem`，交付物由真端元数据承载
   （`15674`=182216193×6、`18739`=182215692×5、`18740`=182215693×8、`25674` 同形镜像）。
3. **未绑定的同形行继续拒绝**：`15548/25548` 的真端 `dev_name` 是「75레벨 달성 주화지급 퀘스트」（75 级达成硬币
   发放）、无 `progress_info`，`category_acquire_=EnterArea` 属误标；`ai-areas.xml` 中没有任何 `quest_area`
   的 `quests` 含这两个 id ⇒ 发放源（等级里程碑事件）未定位 ⇒ **不猜**，继续留 XML。

## 2. 引擎改动（本片生产代码）

| 文件 | 改动 |
|---|---|
| `RetailDataDrivenDefinitionCompiler` | 接取轴门新增一条白名单判据 `areaCollectAcquire = isEnterArea && entry.allCollect() && questAreas.isBound(questId)`（与既有 `areaNoProgressAcquire` 同门同源），未绑定的同形行仍在门外拒绝 |
| `RetailDataDrivenCollectCompiler` | `buildSimple(...)` 识别**发放哨兵**（接取集恰为 `{-1}`）：接取集置空、不合成接取段（避免把哨兵当 NPC id 建死路由），并给定义**前置一条 `SystemGrant` 边**（`NONE → started`，条件 `StartEligible`，`after-commit = SyncQuestState(VISIBILITY_REFRESH)`，`priority=null`）；合成走单一 `withSystemGrant(...)`，客户端词汇表路径与家族规范形路径共用同一段接取语义 |

哨兵口径与既有真端族一致：talk 族的 `acquiredNpc < 0`、hunt 族的系统发放都只保留一条 `SystemGrant` 边
（`RetailSystemGrantDispatchTest` 正是按「`quest_area` 绑定 + 定义带 `SystemGrant` 边」两条判据锁定的）。

## 3. 效果（6 行翻转，逐行可复核）

`15674 18739 18740 25674 28739 28740`：`REJECTED:RETAIL_ACQUIRE_GRANT_UNSUPPORTED` → `ADOPTED` →
`RETAIL_TABLE`（retention 双副本 + XML 删除 + 目录登记行删除）。DD 分类桶：`ADOPTED` 1453 → 1459，
`REJECTED:RETAIL_ACQUIRE_GRANT_UNSUPPORTED` 8 → 2（余下 15548/25548，见第 1.3 节）。

| quest | 真端绑定（world / area） | shared / onlyXml / onlyRetail |
|---|---|---|
| 15674 | 220110000 df6 / `DF6_QuestArea_Q15674` | 19 / 15 / 6 |
| 25674 | 210100000 lf6 / `LF6_QuestArea_Q25674` | 19 / 15 / 6 |
| 18739 | 300610000 idraksha_solo / `QuestArea_Course_A` | 18 / 13 / 6 |
| 28739 | 300610000 idraksha_solo / `QuestArea_Course_A` | 18 / 14 / 6 |
| 18740 | 300610000 idraksha_solo / `QuestArea_Course_B` | 18 / 13 / 6 |
| 28740 | 300610000 idraksha_solo / `QuestArea_Course_B` | 18 / 14 / 6 |

**6/6 行共享边非零**（无零共享红线）。

## 4. 翻转证据（QE-104 生产路径对拍）

方法（`probe/`，与 QE-106/107 同规格）：A = 现行 owner（XML 在场，
`ProductionQuestDefinitions.definitionInOverlay(id)`）；B = 打开接取轴 + 把这 6 行翻成 `RETAIL_TABLE` **并**
移走 XML + 删目录登记行后，同一入口再 dump（overlay fail-fast 要求同时改）。

差异面（以 18739 为例，其余同族）完全落在已定轴上，**没有语义边被静默删除**：

1. `unaccepted --EnterZone[ERIVALE_TERRITORY_VILLAGE_210070000]`（旧壳的区域边，且世界 id 与真端不符）
   → `unaccepted --SystemGrant[]`：真端由 `quest_area`（300610000 idraksha_solo）在进区域时直接发放，
   IR 里是发放边而不是客户端区域手势。
2. `started --TalkToNpc(804707, 39)` 检查对（`HasItem` 成功分支 + `10001` 失败分支）→
   `started --TalkToNpc(804707, QUEST_SELECT=31)` 带**整组** `HasItem` 门控 + `RemoveItem` 直翻 `reward`：
   真端采集交付形（未集齐零路由，关窗兜底交 `DialogService`）。
3. 接取段页梯退场：`unaccepted` 态的 `31→4762`（询问窗）、`20000/20001`（接取/拒绝）、`20002` 等
   旧壳边随「无接取 NPC」整体消失——发放不是客户端手势，不需要接取窗。
4. `reward --TalkToNpc(804707, 1009)` + `SELECT_QUEST_REWARD` → `reward` 态 `108` 领奖窗 + 全局
   `QuestDialog(108)`（自动领取入口只发一次），并补一条登录自愈边
   （`EnterWorld` + `StatusIs(REWARD)` + `var0 < 末行` ⇒ 置末行 + `LEVEL_AND_VISIBILITY_REFRESH`）。
5. `reward` 节点投影随客户端任务书行：18739/18740/28739/28740 与 XML 同为 `var0=1`（节点集相同），
   15674/25674 的 reward 投影由旧壳的 `var0=0` 改为真端行值 `var0=1`（节点集因此不同）。

临时翻转已还原（`git status` 复核）；A/B dump、比值与探针脚本留在 `probe/`。

## 5. 连带测试修订（同片，属「遗留壳合同」类）

| 测试 | 原断言 | 修订 |
|---|---|---|
| `CollectTurnInClientActionAlignmentBatchTest#raksangQuestsShowTheClientOwnedEntryPageAndFailPage` | 18739/18740 的 SELECT1 入口页 + 39 检查对 + `CHECK_USER_ITEM_FAIL` 失败页 | 改名 `raksangQuestsGrantOnAreaEntryAndDeliverOnQuestSelect`，断言真端规范形：`SystemGrant` 发放边（`StartEligible` + `VISIBILITY_REFRESH`）、`QUEST_SELECT(31)` 带整组 `HasItem` 门控 + `RemoveItem`、单档奖励窗，并用既有 `assertNoLegacyDeliveryPages` 锁旧页链零残留 |
| `LegacyRewardStepProjectionRegressionTest` | `QUEST_IDS` 含 15674/25674，锁旧打包步（reward `var0=0` + `QuestVariableIs(var0,1)` 自愈边 + 无 `SetVariable`） | 两个 id 移出 `QUEST_IDS` 并写明退役口径（与 13965/23965 同注释体例），DD 合同由门禁/指纹锁定 |

## 6. 真机验收清单（用户执行）

1. **15674 / 25674**（df6/lf6 区域引导）：走进 `DF6_QuestArea_Q15674`（220110000）/`LF6_QuestArea_Q25674`
   （210100000）区域应**立刻接到任务**（无 NPC 可点）；收集 6 件交付物后与 `806114` 对话，任务书应直接
   进领奖态并弹出奖励窗，未集齐时应**没有任何提示页**（关窗）。
2. **18739 / 28739 / 18740 / 28740**（idraksha_solo 课程区）：走进 `QuestArea_Course_A` / `QuestArea_Course_B`
   应立刻接到对应任务；收集 5 件（18739/28739）/ 8 件（18740/28740）后与 `804707` 对话直翻领奖并弹窗。
3. **旧存档自愈**：已停在领奖态（`var0` 小于末行）的上述任务，登录后应自动补到末行并能完成领奖。
4. 通用回归：奖励窗 `108` 的自动领取入口每条任务只出现一次；未进区域时不应能接到任务。

## 7. 门禁与验证

- `RetailDataDrivenGateTest`：漂移登记与冻结指纹与实现同源（新翻转 6 行指纹入库；**80817 的既存漂移刻意不重冻**）。
- `RetailSimpleTalkGateTest`、`RetailSimpleItemPlayGateTest`、`RetailSimpleUseItemGateTest`、`RetailOwnershipGateTest`、
  `RetailTsvManifestGateTest`、`RetailEnterAreaZoneRegistrationGateTest`、`RetailSystemGrantDispatchTest`、
  `ProductionCatalogWhitelistVerificationTest` —— 绿（`RetailDataDrivenGateTest` 仅剩 80817 既存红）。
- `run_quest_gates.sh T3`：1856 例，红身份集 **198 个 = 与上一片基线逐行相等**，对 `target/agent-logs/qe107b/t3.ids`
  **ADDED 0 / REMOVED 0**（本片日志 `target/agent-logs/qe108b/T3-215137.log`）。本片开始时该套件曾新增 2 个红
  （`CollectTurnInClientActionAlignmentBatchTest#raksangQuestsShowTheClientOwnedEntryPageAndFailPage`、
  `LegacyRewardStepProjectionRegressionTest#keepsLegacyRewardEntryAtTheExistingPackedStep`），已按第 5 节随片修订，修订后归零。
- `verify_retirement.py`：`catalog=750 directory=750 retired=5474 sum=6224 — OK`。

## 8. 既存红（本片未引入，也未顺手回写）

1. `RetailDataDrivenGateTest.frozenFingerprintsCoverExactlyTheRetiredQuests`：`80817` 冻结值与实际 IR 不一致
   （`f5245215…` → `00a65835…`），QE-102 起刻意保留，作为「指纹只增不改」政策的活体证明。
2. `RetailNonIrAxisGateTest.cappedQuestsAreNeverRetailDriven`：`quest-start-metadata-retail-cap-exceptions.tsv`
   尚有 26 条封顶行在 HEAD 上已是 `RETAIL_TABLE`，登记行必须按 P0c-8c/9 流程随采纳切片移除（属行为变更，
   需单独成片 + 用户裁定）。
3. `CollectTurnInClientActionAlignmentBatchTest#quest1137ChecksOnlyTheCollectedFossilNotTheWorkItem` /
   `#quest18745KeepsTheRewardOwnerExclusive`：HEAD 既存红（旧壳 39 检查对与入口页合同），本片未触碰其任务。

## 9. 边界与未做

- 未做真机验收（第 6 节清单待用户执行）。
- 15548/25548（75 级里程碑硬币发放）无 `quest_area` 绑定、真端发放源未定位：**不猜、不硬编码**，继续留 XML。
- 本片只覆盖「`EnterArea` 发放 + 采集交付」这一形；其余 `EnterArea` 形（无进度里程碑、活动类）仍按各自判据。
