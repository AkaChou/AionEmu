# SimpleHunt 家族切换批：运行时集成面事实报告

> 考古批次：2026-10-01（P1 切换批声明书并行后台任务；只读）。仓库根：`/Users/mc/IdeaProjects/AionEmu-test`。
> 除特别标注外，以下 `QE = src/main/java/com/aionemu/gameserver/questEngine`。
> 消费方：`P1-SWITCH-BATCH-DECLARATION.zh-CN.md` §2/§3/§7。

## 1. 击杀事件流（怪物死亡 → quest handler 拿到 (player, npcTemplateId)）

**完整调用链（PvE 单人击杀）：**

1. AI 死亡入口：`AI2Actions.killSilently/dieSilently` → `target.getController().onDie(...)`
   `src/main/java/com/aionemu/gameserver/ai2/AI2Actions.java:43-45,53-55`
   ```java
   public static void killSilently(AbstractAI2 ai2, Creature target) { target.getController().onDie(ai2.getOwner()); }
   ```
   AI 侧状态机为 `DiedEventHandler.onDie/onSimpleDie`（仅喊话+状态+清仇恨，不再分发任务）：
   `src/main/java/com/aionemu/gameserver/ai2/handler/DiedEventHandler.java:22-48`

2. `NpcController.onDie(Creature)` → `SM_EMOTION(DIE)` 广播 → `if (owner.getAi2().poll(AIQuestion.SHOULD_REWARD)) this.doReward();`
   `src/main/java/com/aionemu/gameserver/controllers/NpcController.java:200-244`（reward 判定在 219-221）

3. `NpcController.doReward()` 遍历 `AggroList.getFinalDamageList(true)`，**对每个造成伤害的玩家**：
   `NpcController.java:312`
   ```java
   GameEngineServices.questEngine().onKill(new QuestEnv(getOwner(), player, 0, 0));
   ```
   队伍/团本路径：`PlayerTeamDistributionService.doReward(...)` 逐成员同样调用：
   `src/main/java/com/aionemu/gameserver/model/team2/common/service/PlayerTeamDistributionService.java:199`

4. `QuestEngine.onKill(QuestEnv)` — `QE/QuestEngine.java:410-430`：
   ```java
   QuestEvent event = new QuestEvent.KillNpc(npc.getNpcId());              // :415
   List<Integer> questIds = getQuestNpc(npc.getNpcId()).getOnKillEvent();  // :417
   if (questIds.stream().anyMatch(typed::owns)) {
       typed.dispatch(event, env.getPlayer().getObjectId(), 0, QuestDispatchContract.BROADCAST); // :420
   }
   ```
   - **关键事实：`onKill` 里没有 legacy handler 回退，也没有 QuestEnv.getQuestId 语义**——XML 车道与 retail 车道都走同一个 `productionDispatcher`（`QuestRuntimeRouter`），questId=0 表示 BROADCAST。`QuestEnv` 只在引擎边界存在；进入 typed 运行时后 **npcTemplateId 以 `QuestEvent.KillNpc(npcId)` 事件载荷携带，player 由 dispatch 的 playerId 参数表达**。
   - `questNpcs` 索引由 `installProductionDefinitions` 从 catalog 全部 transitions 建：`KillNpc` 与 `KillNpcSet` 都注册 `registerQuestNpc(npcId).addOnKillEvent(definition.id())`：`QE/QuestEngine.java:2060-2065`；`getQuestNpc` 未注册返回空壳：`:1811-1816`。

5. 分派链：`QuestRuntimeRouter.dispatch`（owner 唯一性强制，双 owner fail-fast）：
   `QE/runtime/QuestRuntimeRouter.java:95-117,142-153` → `QuestProductionDispatcher.dispatch`（LazyConnection，连接= `DatabaseFactory::getConnection`）：
   `QE/runtime/QuestProductionDispatcher.java:290-306,101,119` → `QuestEventRouter.dispatch`（BROADCAST 合同按 owner 去重）：
   `QE/runtime/QuestEventRouter.java:23-84,69-81` → handler = `execute(connection, playerId, event, route, contract)`（`QuestProductionDispatcher.java:411-462`）→ `QuestExecutionCoordinator.executeValidated`（**按玩家串行** `serialExecutor.execute(playerId, …)`）：
   `QE/runtime/QuestExecutionCoordinator.java:92-119,116`

6. 「handler 拿到事实」的形态：`eventPort.snapshot(playerId, definition.id(), event, requirements)` 冻结事件前玩家事实快照（`QuestExecutionCoordinator.java:136-137`；实现 `QE/runtime/PlayerQuestEventPort.java:15-60`）。**没有 legacy `QuestHandler` 类**；"handler" 对应物 = transition 匹配：`QuestEvent.matches(definition, actual)`，其中 `KillNpcSet` 对 `KillNpc` 的匹配在 `QE/definition/QuestEvent.java:763-764`。

**其他击杀类事件（XML/IR 与 retail 同路）：**
- PvP 击杀：`QuestEngine.onKillRanked`（`:922-950`）、`onKillInWorld`（`:959-992`）。
- 玩家死亡：`PlayerController.java:607` → `questEngine().onDie` → `QuestEvent.Die` BROADCAST（`QuestEngine.java:546-566`）。
- 攻击事实：`NpcController.java:457` → `onAttack` → `QuestEvent.AttackNpc(npcId, QuestNpcAttackFacts)`（`QuestEngine.java:438-462`）。
- QuestEvent 为封闭 sealed 接口（`QE/definition/QuestEvent.java:10-23`），`KillNpc` 定义 `:75-84`，`KillNpcSet` `:90-105`。

## 2. 任务状态与持久化

**模型：**
- `QE/model/QuestState.java:23-64`：`questId / QuestVars / status / completeCount / completeTime / nextRepeatTime / reward / persistentState`。所有 setter 标记 `PersistentState.UPDATE_REQUIRED`（`:72-135`）。
- `QE/model/QuestVars.java:13-123`：**6 槽 × 6 bit 打包为一个 int**；`getQuestVars()` = `Σ value_i << 6i`（`:101-109`），`setVar(int)` 逐槽 `& 0x3F` 解包（`:116-122`）；`setVarById` 强制 0..63（`:40-42,90-94`）。
- `src/main/java/com/aionemu/gameserver/model/gameobjects/player/QuestStateList.java:28-42`：`TreeMap<Integer, QuestState>`。

**DB 落库点（legacy 脏标记路径）：**
- 接口：`src/main/java/com/aionemu/gameserver/dao/PlayerQuestListDAO.java:35-57`。
- MySQL 实现：`src/main/java/com/aionemu/gameserver/dao/impl/PlayerQuestListDAO.java`
  - 表 `player_quests`，字段 `quest_id, status, quest_vars, complete_count, next_repeat_time, reward, complete_time`（SQL 常量 `:27-33`；`quest_vars` 读写 `:65` / `:198` / `:257`，编码即 `qs.getQuestVars().getQuestVars()`）。
  - `store(Player)`（`:92-120`）：自管连接、`setAutoCommit(false)`、commit、成功后批量置 `UPDATED`；SQLException 时 `rollback()`（`:109-115`）。
  - 调用时机：**登录 load** `PlayerService.java:222`；**周期写** `GeneralUpdateTask.java:39`（`PlayerEnterWorldService.java:705` 挂调度）；**全量保存** `PlayerService.storePlayer` `:151`。即 legacy 车道=脏标记+周期/登出刷盘。
- typed 车道（XML 与 retail 同用）：`QE/runtime/PlayerQuestStatePort.java`
  - `apply(Connection, playerId, plan)`（`:50-75`）：写**暂存 QuestState 投影**（`questDao.store(connection, playerId, List.of(stagedProjection))` `:70`），不动 live 内存；`publish`（`:78-109`）仅在事务提交后把投影原子写入 live（`state.setQuestVar(committed.getQuestVars().getQuestVars())` `:99` 并置 `UPDATED` 防重复写 `:107`）；`rollback`（`:112-114`）丢弃 pending。装配：`QE/runtime/QuestRuntimeComposition.java:228`。
  - 事务编排：`QuestExecutionCoordinator.executeSerialized`（`:121-258`）：snapshot → plan → `QuestUnitOfWork.open(connection)`（`:168`）→ actionPort.preflight/apply + statePort.apply → `unit.commit()`（`:205`）→ publish → afterCommit；失败路径 `statePort.rollback` + `participant.afterRollback`（`:242-255`）。**typed 车道每事件即事务**。
  - packed vars 来源：`QE/runtime/QuestMutationPlanner.java:95`、Set/IncrementVariable（`:136-142`）、目标投影权威但让位于显式自环动作（`:187-194`）、`packed = layout.pack(variables)` `:195`。

**「绕开 QuestVars 语义直读写字段」的现有缝隙（已确认存在）：**
- 原始整字读写入口已存在：`QuestState.setQuestVar(int)`（`QE/model/QuestState.java:92-95`）+ `getQuestVars().getQuestVars()`（原始 int）。typed 车道的 `PlayerQuestStatePort.projection` 本来就用 `plan.nextPackedVariables()` 原始 int 构造（`:134-135`）。
- **QuestVars 的槽位访问器会钳制 10 位值**——`setVar` 解包每槽 `&0x3F`（`QuestVars.java:120`），`setVarById` 拒绝 >63。因此 native 10-bit 相机值只能走原始整字路径（`setQuestVar(raw)` / `getQuestVars()`），不能走槽位 API。
- native 侧已有的 raw 编解码：`QE/tablelane/RawQuestVarsCodec.java:15-96`。IR 车道的等价物是 `QE/definition/BitField.java:9-43` 与 `QE/definition/PersistenceMode.java`。

## 3. 客户端同步

- **唯一的 typed 状态同步出口**：`QE/runtime/PlayerQuestStateSyncPort.sync`（`:60-105`），注释 "仅在状态事务提交并发布后运行"：
  - 新出现在客户端任务列表 → `SM_QUEST_ACTION.addQuest(questId, status, packedVars)`，否则 `updateQuest(...)`（`:72-75`）；
  - COMPLETION 模式追加 `removeQuestFromClientList` + `SM_QUEST_COMPLETED_LIST`（`:76-80`）；
  - zone/nearby 刷新与 `onQuestStateChanged` 重评（`:84-96`）；`notifyFinishedNpc` AI 钩子（`:101-103`）。
- 包本体：`src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_QUEST_ACTION.java`：action 1=add（`:56-61`）、2=update（`:70-75`）、3=remove（`:82`）、4=timer（`:93`）、5=share（`:107`）、6（`:120`）；**`step` 字段即打包 quest_vars**（`writeD(step)` `:144,:151`）。本库无 `SM_QUEST_STATUS` 类，对应物就是 `SM_QUEST_ACTION`。
- **「一次击杀 +1」现在发什么**：SimpleHunt 网格推进边声明 `AfterCommitAction.SyncQuestState(PACKET_ONLY)`（`QE/retail/RetailSimpleHuntDefinitionCompiler.java:58-60`）；提交后由 `TypedQuestAfterCommitPort` 执行（`QE/runtime/TypedQuestAfterCommitPort.java:148-154` → `stateSyncPort.sync`）。
- **时机/去重**：`QuestExecutionCoordinator.withoutRedundantStateSync`（`:269-277`）——状态与副作用都没变时丢弃 SyncQuestState。
- SM_SYSTEM_MESSAGE：重复/冷却提示 `PlayerQuestStateSyncPort.sendCompletionAvailability`（`:115-133`，1400855/1402676/1400857）；每日提醒定时广播 `QuestEngine.addMessageSendingTask`（`QE/QuestEngine.java:2194-2224`）。legacy 侧放弃任务直接发 `SM_QUEST_ACTION` remove/timer（`src/main/java/com/aionemu/gameserver/services/QuestService.java:1350-1357`）。

## 4. 生产装配链

- **启动**：`GameEnginesLifecycle.start` → `engine.load(progressLatch)`（`src/main/java/com/aionemu/gameserver/lifecycle/GameEnginesLifecycle.java:61`）；预加载在静态数据前：`GameStartupSequenceLifecycle.java:261` → `GameEnginesGateway.java:126-127` → `QuestEngine.preloadProductionCatalog`（`QE/QuestEngine.java:1879-1903`）。
- **`QuestEngine.load`**（`QE/QuestEngine.java:2165-2188`）：`HtmlPagesRegistry.ensureLoaded()`（`:2176`）→ `installProductionDefinitions(...)`（`:2177-2178`）。热重载：`src/main/java/com/aionemu/gameserver/commands/admin/Reload.java:151,156`。
- **目录编译**：`loadProductionCatalog`（`:1868-1873`）：
  ```java
  QuestCatalog xmlCatalog = QuestDefinitionCatalogManifest.compile(Config.dataFile("./data/static_data/quest/definitions").toPath());
  return RetailQuestDriver.overlayProduction(xmlCatalog);
  ```
- **retail overlay 注入点**：`QE/retail/RetailQuestDriver.java`
  - `overlayProduction`（`:228-245`）fail-closed；`apply()`（`:458-485`）替换/补入 retail 定义；`verifyProductionCoverage`（`:251-312`）对 6224 行保留清单逐 id 核对（retail 行必须无 XML 且有可执行定义 `:291-294`；总数恰等 `PRODUCTION_QUEST_COUNT=6224` `:72,:279-282`）。
  - `load()`（`:348-456`）一次性装载保留清单 + 全部真端表。
  - 开关 `aion.quest.retailDriver`（`:71,:198-201`，默认 true）。
- **`OverlayView`**：`QE/definition/ProductionQuestDefinitions.java:21`（`record OverlayView(QuestCatalog xml, QuestCatalog overlay)`）；`catalog()`（`:28-45`）；`definitionInOverlay`（`:57-79`）。
- **typed owners**：`productionDispatcher.owners()`（`QE/runtime/QuestProductionDispatcher.java:177-179`）；查询面 `isHaveHandler/isProductionOwner`（`QE/QuestEngine.java:1837-1844`）。装配期拆分：`splitRuntimeCatalogs`（`QE/QuestEngine.java:2015-2036`）按 `RetailQuestDriver.retailOwnedIds` 分成互斥 xml/retail 子目录 → 各建 `QuestProductionDispatcher.production(...)`（共享同一 `QuestExecutionCoordinator(PlayerSerialExecutor)`，`:1975-1980`）→ `QuestRuntimeRouter` 组合（`:1981`）→ 交互物校验、广播口安装、逐定义事件接线校验 → `installProductionDefinitions`（`:2046-2107`，发布点 `:2106`）。
- **SimpleHunt 最终运行时形态**：**就是 IR（nodes + transitions）**——`RetailSimpleHuntDefinitionCompiler.compile` 产出 `QuestDefinitionCompiler.compile(definition)`（`QE/retail/RetailSimpleHuntDefinitionCompiler.java:156-158`），与 XML 编译产物同型，同入同一 dispatcher。
- **对话分派中 `QuestRuntimeRouter` 与 `RetailDialogIntentClassifier` 的角色**：
  - `QuestEngine.onDialog`（`:233-327`）：questId>0 → EXCLUSIVE 到唯一 owner（`:243-269`）；questId==0 → `npcDialogDispatchOwners` 排序逐 owner 尝试（`:272-291,343-359`）。
  - `QuestRuntimeRouter.isUnroutedLocalDialog`（`QE/runtime/QuestRuntimeRouter.java:173-188`）：用 `RetailDialogIntentClassifier.classify(questId, actionId)`（`QE/retail/RetailDialogIntentClassifier.java:30-43`；生命周期动作白名单 `:45-63`）判定客户端本地翻页/关窗，服务端直接确认（router `:100-103`）。

## 5. SimpleHunt 现行路径与消费面

**编译器输入：** `QE/retail/RetailSimpleHuntDefinitionCompiler.java`
- `compile(plan, metadata, clientRewardNpcs, questAreas, clientDialogExits)`（`:86-90`）；`compileSerialChain(plan, stageRegistry, metadata, briefingNpcIds, briefingNpcName)`（`:231+`）。
- `plan` = `RetailSimpleHuntPlan.bind(RetailSimpleHuntTable.Entry, RetailNpcNameIndex)`（`QE/retail/RetailQuestCatalog.java:61-63`；`RetailSimpleHuntPlan.java:51,73,83,99`）。
- 输入源清单：
  - 真端表 `Quest_SimpleHunt.xml`（`RetailQuestDriver.java:40,386-389`；`RetailSimpleHuntTable.load` `QE/retail/RetailSimpleHuntTable.java:55+`）。
  - 元数据 `quest.xml`（真端）：`RetailQuestDriver.java:63,434-437` → `RetailQuestMetadataCompiler.compile`（`:758-766`）+ npc 模板 8 文件（`:75-78`）+ `RetailQuestAiNameGroups`（/quest/retail-quest-ai-name-groups.tsv，`QE/retail/RetailQuestAiNameGroups.java:25-27`）+ `RetailNpcNameAliases` + item 模板 + `legacy/quest_random_rewards.xml`（`:65`）+ `quest_name_string_ids.tsv`（`:64`）+ `RetailSpawnedNpcIds`。
  - `clientRewardNpcs` ← `/quest/quest_client_reward_npcs.tsv`（`QE/retail/RetailClientRewardNpcs.java:56-58`）。
  - `questAreas` ← `/aion/definitions/compact/ai/ai-areas.xml`（`RetailQuestDriver.java:70,394-397`）。
  - `clientDialogExits` ← `RetailClientDialogExits.defaultExits()`（内建，无 TSV）。
  - 串行链 `stageRegistry` ← `/quest/quest_client_hunt_stages.tsv`（`QE/retail/RetailClientHuntStages.java:58-60`）。
- **产出**：`Outcome(CompiledQuestDefinition definition, String rejectionCode, String detail)`（`:68-73`），稳定拒绝码 RETAIL_METADATA_UNREDAILED/RETAIL_MULTI_TIER 等（`:44-50`）。

**重要：编译器被 9 个其他家族编译器复用其规范形 helper**（同批整体删除会破坏它们）：
- `RetailDataDrivenTalkCompiler.java:101,124,237`（canonicalAcceptFlow/canonicalItemAcceptFlow）
- `RetailDataDrivenTalkCollectChainCompiler.java:405,476,480`
- `RetailDataDrivenTalkHuntChainCompiler.java:730,844,1184`
- `RetailHandinDialogFlowCompiler.java:95`（acceptFlow）
- `RetailCombineTaskDefinitionCompiler.java:219`
- `RetailSimpleTalkDefinitionCompiler.java:297,411,699`（rewardWindowAutoFlow）
- `RetailSimpleItemPlayDefinitionCompiler.java:250,312`
- `RetailSimpleCollectItemDefinitionCompiler.java:394,531`
- `RetailSimpleUseItemDefinitionCompiler.java:310`
- `RetailDataDrivenDefinitionCompiler.java:440,477-479,855,902`（`RetailSimpleHuntPlan.bind` + `compileClientLadder`/`compileCanonical`）

**Quest_SimpleHunt.xml 的全部读取者：**
- main：`QE/retail/RetailQuestDriver.java:387`；`QE/tablelane/NativeQuestTableLoader.java:67`（native 新车道装载器，不在删除范围）；`QE/retail/RetailSimpleHuntTable.java`（loader 本体）。（`RetailNpcNameIndex.java:46` 仅注释提及。）
- test：`QuestSimpleHuntRetailContractTest`、`RetailOwnershipGateTest`、`RetailBriefingChainEvidenceGateTest`、`RetailQuestDriverOverlayTest`、`RetailSimpleHuntEquivalenceGateTest`、`RetailSimpleHuntFamilyGateTest`、`RetailSimpleHuntPlanEquivalenceTest`、`RetailSimpleHuntTableTest`、`RetailSystemGrantDispatchTest`。

**狩猎 TSV 消费面（main+test 全列）：**
- `/quest/quest_client_hunt_stages.tsv`：main — `RetailClientHuntStages`（`:58-60`）← `RetailQuestDriver.compileSimpleSerialHunt`（`:641`）与 `RetailSimpleHuntDefinitionCompiler.compileSerialChain`（`:240`）；test — `RetailSimpleSerialHuntGateTest`。
- `/quest/quest_client_kill_targets.tsv` + `quest_client_kill_targets_stages.tsv`：main — `RetailClientKillTargets`（`:48-62`）← `RetailQuestDriver.load`（`:431`）→ `RetailDataDrivenDefinitionCompiler`；test — `QuestIlumaNorsvoldKillTargetCoverageTest`、`RetailDataDrivenGateTest`、`QuestEventShardRetailAlignmentTest`、`ZzIrDumpProbeTest`。
- 相邻：`RetailClientHuntProgressRows` 不读 TSV，读 `/aion/definitions/quest_monster/quest_monster.csv`（`:51-55`）。
- 家族 test 夹具 TSV：`src/test/resources/quest/` 下 `retail-simplehunt-compiler-rejects.tsv`、`retail-simple-hunt-adjudicated-decisions.tsv`（main 侧 `RetailChallengeAcquireAdoptions` 仅注释引用该门禁，UNVERIFIED 运行时读表）、`quest-simple-hunt-retail-contract.tsv`、`quest-simple-hunt-server-target-exceptions.tsv`、`retail-simple-hunt-ir-fingerprints.tsv`、`retail-simple-hunt-adjudicated-ir-fingerprints.tsv`。

**守门测试测什么：**
- `RetailSimpleHuntFamilyGateTest`：全族 942 条保留行不变量——`RETAIL_TABLE` 行必须合成成功；`SEMANTIC_GAP:RETAIL_*` 必须仍按登记码被拒；驱动覆盖缺口/真端表缺口必须仍能合成；等价缺口必须有 plan（`:186-258`）；接受门槛 >250（`:258`）；要塞守军 plan 断言（`:130-143`）；挑战哨兵采纳集与裁定登记同步（`:292-323`）。
- `RetailSimpleHuntEquivalenceGateTest`：RETAIL_TABLE 且合成成功的任务，无 shell 真端定义与 quest-definition XML 在归一化 IR 层多重集合相等（节点按 (status,packed)，转换按 (source,event,target,conditions,actions,afterCommit)）。
- `RetailSimpleHuntPlanEquivalenceTest`：真端表→plan 必须复现每条 `COUNTER_GRID` 合同行。
- `RetailSimpleHuntTableTest`：表装载 + 计数器语义回放（表 1,865 行，6 行无计数器占位行跳过——与 tablelane 口径差 2（4 活跃零计数 + 2 注释行），切换批开工时对齐）。
- `QuestSimpleHuntRetailContractTest`：按快照 model 逐计数器断言。
- `RetailOwnershipGateTest`：保留清单覆盖生产全集、每任务唯一 owner、家族判定与真端表一致。

**删除清单（grep 可见，标注 main/test）：**
- main 删除候选：`QE/retail/RetailSimpleHuntDefinitionCompiler.java`（网格部分；串行与共享 helper 另判）、`RetailSimpleHuntPlan.java`（DD 仍用）、`RetailSimpleHuntTable.java`（DD 仍用）、`RetailQuestCatalog.java`（SimpleHunt 判源+onKill packed 推进 `:61-92`；亦持有 CombineTask 表）、`RetailHuntCounterLayout.java`（DataDriven 链也用，UNVERIFIED 是否随批删）。
- main 引用需改造：`RetailQuestDriver.java`（`compileSimpleHunt` `:768-790`、`compileSimpleSerialHunt` `:620-652`、字段 `:93,:113,:386-389`、`compileByFamily` `:544-567`）+ 上列 9 个复用 helper 的编译器 + `RetailDataDrivenDefinitionCompiler`。
- test 删除候选：`RetailSimpleHuntFamilyGateTest`、`RetailSimpleHuntEquivalenceGateTest`、`RetailSimpleHuntPlanEquivalenceTest`、`RetailSimpleHuntTableTest`、`QuestSimpleHuntRetailContractTest`；受牵连：`RetailOwnershipGateTest`、`RetailQuestDriverOverlayTest`、`RetailQuestCatalogTest`、`RetailSystemGrantDispatchTest`、`RetailHuntClientCountGateTest`、`RetailSimpleSerialHuntGateTest`、`RetailMetadataEquivalenceGateTest`、`RetailRewardWindowRouteTest`、`QuestEngineNpcDialogDispatchTest`、`RetailBriefingChainEvidenceGateTest`、`ZzIrDumpProbeTest`、`RetailQuestAiNameGroupGateTest`、`RetailEnterAreaZoneRegistrationGateTest`（视各自引用面）。
- 资源：`Quest_SimpleHunt.xml` 的编译器读取可删，但 `NativeQuestTableLoader` 仍以同一文件为 native 数据源——文件本身不能删。

## 6. XML-only id 枚举与 retail 交集

- **装载方式**：显式 manifest 白名单 `QE/definition/QuestDefinitionCatalogManifest.java:34-36`（`RESOURCE = "aion/data/static_data/quest/definitions/quest_definition_catalog.xml"`，version 必须=2 `:59-61`）。生产两路：`compile(Path)`（`:91-108`，QuestEngine 用）与 `compile(ClassLoader)`（`:73-85`）。
- **catalog 结构**：`<quest-definition-catalog version="2"><definition id="1000" resource=".../quests/1000.xml" mode="EXECUTABLE"/></...>`；742 条；**权威是 manifest 行的 id 属性**，装载后强制 编译id==manifest id（`CATALOG_ID_MISMATCH`，`:197-203`）。quests/ 目录 742 文件，文件名=id。
- **运行时枚举全部 XML 任务 id**：`productionDispatcher.owners()`（`QE/runtime/QuestRuntimeRouter.java:76-80`）；无独立枚举器——catalog 即枚举。⚠️ 切换批 go-live 时 `NativeQuestOwnerResolver` 的 XML id 源宜从目录文件名扫描改为编译后的 catalog id 集（现二者同值 742；manifest 是权威）。
- **XML-only 集合 vs retail 表 id 集合**：现运行时**不交叠且为闭环台账**：`retail-xml-retention.tsv` 6224 行 = XML_RETENTION 742 + RETAIL_TABLE 5482 = `PRODUCTION_QUEST_COUNT`；三重不交叠强制（overlay 校验 `RetailQuestDriver.java:291-297`、splitRuntimeCatalogs 分桶、`QuestRuntimeRouter.ownerOrNull` 双 owner 抛异常 `:148-151`）。retail 任务的 quest-definition XML 已物理删除（`RetailQuestDriver.java:30` "已删除的 XML 无法由开关恢复"）。
  - ⚠️ 与 tablelane 口径的关系：owner resolver 的"真端表 id 集"是**表全量行**（SimpleHunt 1863），与 retention 的 RETAIL_TABLE 行集（家族=SimpleHunt 939）不同——差集=跨族表行与他族 retention 名下 id。owner 裁决以 retention 家族为准，切换批开工时逐行重算落矩阵。
- **retention 台账的角色（只描述）**：production 侧是 `overlayProduction/verifyProductionCoverage` 与家族分派依据（`:358-385`；`compileByFamily` `:544-567` 兜底=SimpleHunt）；test 侧副本 `/quest/retail-xml-retention.tsv` 被各门禁读取。

**UNVERIFIED 项汇总**：① `RetailChallengeAcquireAdoptions` 是否运行时读 SimpleHunt 表（grep 只见注释引用门禁名）；② `RetailHuntCounterLayout` 的完整引用者清单（是否被 DataDriven 链共享到不能随批删）；③ test 门禁清单的完整牵连面（部分测试引用深度未逐行核对）。
