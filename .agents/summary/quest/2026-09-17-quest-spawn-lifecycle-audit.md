# 2026-09-17 任务临时 NPC 生成/清理闭环专项审计

## 1. 排查维度与判定口径

对全部含 `spawn-npc-at-player` / `spawn-npc` 的任务做“生成 → 使用 → 回收”闭环核对：

1. 该 slot 是否有显式 `despawn-npc` / `delete-interaction-npc` / `delete-world-npcs`；
2. 若没有，生成的 NPC 是否被某个任务事件消费（`kill-npc` / `talk-to-npc` / `drop.npc-id` / `can-act`）；
3. 若既无显式回收也无需消费（场景/特效 NPC），其生命周期是否由引擎终态清理兜底。

## 2. 全库扫描结果

39 个任务使用 `spawn-npc-at-player`，其中 22 个没有同 slot 的显式回收动作：

| 类别 | 任务 | 生成物 | 消费方式 |
|---|---|---|---|
| 击杀消费 | `10506`(ego 236263)、`13954`/`23954`(soul 247093)、`2252`(210634)、`24022`(spy 204417)、`36510`(twister 216614)、`36512`(216616)、`46511`(216629)、`25680`(tauric-an 247074)、`20506`(muscle 219957) | 任务 NPC | 同 XML 存在 `kill-npc` 路由，死亡即从世界移除 |
| 掉落消费 | `10527`(244108)、`20527`(244124) | 携带任务掉落的 NPC | `<drops npc-id=...>` 声明其掉落，击杀后掉落任务道具 |
| 对话消费 | `1693`(harmone 798388)、`14112`(kato 203195)、`25601`/`25605`/`25606`、`30217`(799506) | 交付/接力 NPC | 必须持续存在直到玩家交互，终态才回收 |
| 场景/特效 | `15680`(guard 282465)、`25680`(cellatu 806695、cellatu-treasure 282465)、`25023`(sprout 805161)、`30710`(gabelline 800457)、`30760`(hakelan 800458) | 场景 NPC/特效（如 282465 = `IDElemental_Smoke`，NON_ATTACKABLE） | 无需消费，随任务终态回收 |

对照旧版 handler：`QuestService.addNewSpawn(worldId, instanceId, templateId, x, y, z, heading)` 生成的是**无存活时间的单次 spawn**，旧引擎没有任何回收；迁移后的 typed 引擎在以下四类路径按 `(playerId, questId)` / `playerId` / `instanceId` 清理，**严格优于旧基线**：

- `QuestExecutionCoordinator`：`nextStatus == COMPLETE || NONE` 时注册 `terminalCleanup`（完成/失败/放弃）；
- `PlayerQuestRecoveryEventPort.recover` → `QuestRuntimeResources.cleanupPlayer`（登出/重登）；
- `InstanceService` → `QuestRuntimeResources.cleanupInstance`（副本销毁）；
- `QuestSpawnRegistry`：按 slot 幂等（自环重复触发生成不会刷出重复 NPC），`remove` 只删本 slot 的权威 handle。

结论：**本维度未发现需要修复的生产缺陷**，不需要任何 XML 变更。

## 3. 既有门禁覆盖

- `QuestSpawnRegistryTest`：`cleanupRemovesEverySlotOfTheQuest`、`registryIsIsolatedAcrossPlayersAndQuests`、`spawnIsIdempotentPerSlotAndDoesNotRespawn`、`spawnsUnderSlotAndDespawnsOnlyTheAuthoritativeHandle`；
- `PlayerQuestSpawnPortTest` / `PlayerQuestSpawnPortRaceTest`：`aRegistrationRaceDoesNotLeaveAnUntrackedNpc`、`spawnIsBestEffortWhenPlayerLoggedOut`、`playerPositionSpawnUsesTheEventSnapshot`；
- `QuestExecutionCoordinatorTest#terminalCleanupRunsLastAndItsFailureIsReported`：终态清理排在 after-commit 末尾且失败被上报。

即“生成后由终态兜底回收”这一根因机制本身已被锁定，无需新增空转门禁。

## 4. 残余隐患与根治实施（QE-029）

`QuestSpawnRegistry.contains()` 原先只判断“slot 是否登记了 handle”，不判断该 handle 是否仍存活。
若生成的 NPC 被**注册表之外**的路径销毁（例如 `1922` 的 `<delete-world-npcs/>` 会清空当前世界地图
实例内的全部 NPC、其他玩家的击杀、实例场景清理），而任务之后又需要重新生成同一 slot
（`enter-world` 型恢复路由或 self-loop 再触发），`spawnNpc` 会因为 `contains == true` 直接返回成功，
**新 NPC 不会被创建，玩家的任务 NPC 永久消失**。

按“以全部类似任务角度根本解决”的原则，修复落在引擎层而不是单个任务 XML：

1. **`PlayerQuestSpawnPort.isUsableAuthoritativeHandle(Npc)`**：真实 NPC 一旦已死亡（`lifeStats.isAlreadyDead()`）
   或已离开世界（`!isSpawned()`）即判定陈旧；无法判定生命属性的替身/半初始化产物保持幂等，
   避免把正常重复的 after-commit 误判为重建请求。
2. **`QuestSpawnRegistry.replaceStale(snapshot, slot, stale, replacement)`**：按调用方判定的**具体**陈旧
   handle 做 CAS 替换——slot 在判定与替换之间被清空时由本次生成接管；并发下若他人已换入 handle，
   则保留对方的权威并取消旧跟随任务，绝不制造无人登记的孤儿实体（仍禁止按 templateId 删同类）。
3. **`spawnNpc` / `spawnNpcRandom`（非替换路径）** 统一改用该语义登记：先判存活，陈旧时重建并原子替换，
   否则保持原有幂等不刷怪行为。

## 5. 验证结果（已通过）

```bash
mvn test -Dtest=PlayerQuestSpawnPortTest,PlayerQuestSpawnPortRaceTest,QuestSpawnRegistryTest,QuestExecutionCoordinatorTest
```

`Tests run: 34, Failures: 0, Errors: 0, Skipped: 0` —— `QuestSpawnRegistryTest` 8 项（新增
`replaceStaleSwapsOnlyTheJudgedHandle`）、`PlayerQuestSpawnPortTest` 9 项（新增
`questNpcDestroyedOutsideTheRegistryIsRebuiltForItsOwner`）、`PlayerQuestSpawnPortRaceTest` 1 项、
`QuestExecutionCoordinatorTest` 16 项。

同时复跑任务门禁套件（跨影响确认，未改任何任务 XML）：

```bash
mvn test -Dtest=QuestMovieAndDialogLoopRegressionTest,QuestDefinitionDirectoryLoaderTest,CompletedQuestPrerequisiteRegressionTest,QuestClientContractGateTest,ProductionCatalogWhitelistVerificationTest
```

`Tests run: 30, Failures: 0, Errors: 0, Skipped: 0`；`PRODUCTION_COMPILE_OK=6186, FAILURES=0, WHITELIST_VIOLATIONS=0`。

## 6. 架构沉淀与边界

- 新增 `[QE-029] 二十七、任务生成 NPC 的权威 handle 存活语义 (QUEST_SPAWN_HANDLE_LIVENESS)`，
  同步 `patterns/quest-engine.md`、`systemPatterns.md`、`symptom-index.md`，`sync/check` 校验 61 条通过；
- 边界：终态清理（完成/放弃/登出/副本销毁）仍是 slot 回收的主路径，本次只补齐“被外部销毁后的重建”；
  替换只作用于调用方判定的那个 handle，保持 slot 隔离与 race 安全；
- 未启动/停止/重启服务器进程；Aion 5.8 客户端实机验证仍待用户执行（在实例内触发世界级 NPC 清理后
  重新进入并确认任务 NPC 重建）。
