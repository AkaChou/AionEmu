# KnownList 移动广播异常：触发链与复现计划（2026-09-19）

> status: 待运行时复现（runtime repro pending）
> related: `ba96881cc`、`AR-013`、`.agents/summary/i18n-log-args/2026-09-19-i18n-log-argument-contract.zh-CN.md`

## 1. 结论

`KnownList` 的「对所有 NPC 运行访问器时异常」不是某个 NPC 或某个任务的专属缺陷，而是
`MovementNotifyTask` 的 500ms 已知列表遍历与 NPC 生命周期变更（despawn/传送/区域切换）之间的竞态。
触发一次异常需要同时满足三个条件：

1. 有生物进入移动通知队列；
2. 该生物的 KnownList 中至少有一个 NPC；
3. 在快照生成后、访问该 NPC 前，它变成未生成状态（或 AI/lifeStats 缺失）。

## 2. 已证实的调用链

1. 移动入队：`CreatureController.onStartMove()/onMove()/onStopMove()` →
   `notifyAIOnMove()` → `GameMovementLoopServices.movementNotifyTask().add(getOwner())`
   （`controllers/CreatureController.java:100-138`）。入口包括 `CM_MOVE`（`:116/:128`）、
   `CM_SUMMON_MOVE`，以及 NPC AI 的 `MoveEventHandler`/`RetailPatternAI2` 移动回调。
2. 500ms tick：`MovementNotifyTask` 构造参数 `500`（`taskmanager/tasks/MovementNotifyTask.java:91-94`），
   由 `AbstractPeriodicTaskManager.onStartup()` → `ThreadPoolManager.scheduleAtFixedRate`
   调度到 `scheduledPool`（`pool-4-thread-*`；`AbstractPeriodicTaskManager.java:44-47`、
   `ThreadPoolManager.java:78-83/:160-165`）。
3. 遍历：`callTask` → `creature.getKnownList().doOnAllNpcsWithOwner(MOVE_NOTIFIER, limit)`
   （`MovementNotifyTask.java:103-113`）。6 张 Abyss 图 limit=200，其余 `Integer.MAX_VALUE`。
4. 快照：`knownObjectsSnapshot()` 先复制 `ArrayList`（`KnownList.java:484-492`），
   再逐个 `visitor.visit(npc, owner)`（`:361-377`）。
5. 访问：`MoveNotifier.visit` 读取 `object.getAi2().getState()`、`object.getLifeStats().isAlreadyDead()`，
   存活则 `onCreatureEvent(CREATURE_MOVED, (Creature) owner)`（`MovementNotifyTask.java:201-208`）。
6. AI 分发：`AbstractAI.onCreatureEvent`（`ai2/AbstractAI.java:705-706`）→
   `GeneralNpcAI2.handleCreatureMoved`（`ai/GeneralNpcAI2.java:203`）或 `AggressiveNpcAI2`（`:73`）
   → `CreatureEventHandler.onCreatureMoved`（`ai2/handler/CreatureEventHandler.java:31-37`）。
7. 仇恨/距离：`checkAggro` 依序检查 FIGHT、死亡、可见性、
   `owner.getActiveRegion().isMapRegionActive()`、感知范围、喊话与敌对视线
   （`CreatureEventHandler.java:72-119`）；移动者为玩家时再调
   `QuestEngine.onAtDistance(new QuestEnv(npc, player, 0, 0))`。

## 3. 最可疑的异常窗口

- `World.despawn` 先 `object.getPosition().setIsSpawned(false)`，再执行区域重算与
  `clearKnownlist()`（`world/World.java:587-595`）。
- `WorldPosition.getMapRegion()` 在 `isSpawned == false` 时直接返回 `null`
  （`world/WorldPosition.java:63-65`）；`VisibleObject.getActiveRegion()` 只是转发该方法。
- KnownList 遍历使用的是快照副本；快照生成后发生的 despawn 不会把对象从本轮副本中移除。
  因此 `checkAggro` 的 `owner.getActiveRegion().isMapRegionActive()` 可在
  `CreatureEventHandler.java:84` 抛出 NPE，随后被 `KnownList` 的 `catch` 记录。
- 第二候选是 `MoveNotifier.visit` 的 `object.getAi2()`/`getLifeStats()` 为空；
  运行日志中没有「AI 工厂出错」行，因此该分支可能性较低，但新堆栈可直接区分。

## 4. 运行证据

- `log/console.log:66251`（09-19 09:32:58，`pool-4-thread-36`）与 `:66360`
  （09:43:27，`pool-4-thread-9`）的旧报错；对应 `log/error.log:11262-11263`。
- 两条报错都在 scheduledPool 上，且旧模板由 `doOnAllNpcs` 与 `doOnAllNpcsWithOwner`
  共用，无法区分路径；`ba96881cc` 已拆分模板并恢复堆栈。
- 09:32:31 与 09:42:10 附近的 15001 `状态=4`、09:43:26 的 86597 对话，说明窗口内存在
  任务阶段切换/对象清理造成的生命周期抖动；该时间线只能说明场景，不能单独证明根因。

## 5. 复现步骤（用户侧）

1. 重启服务端并加载包含新日志代码的构建。
2. 进入 NPC 密集且会 despawn/spawn 的场景（任务阶段切换、副本入口/传送、区域重算附近）。
3. 持续移动/停步（`CM_MOVE` 的 start/move/stop 都会入队），每次移动都会进入下一次 500ms tick；
   同时在移动窗口内让目标 NPC 被移除/despawn。
4. 这是竞态，可能需要来回移动数秒到数十秒；KnownList 越大、despawn churn 越多越容易命中。

## 6. 复现后的日志判读

新日志会打印所有者、失败 NPC、已遍历数，并带完整堆栈：

- `log.113eb26bcfad`（`doOnAllNpcsWithOwner`）：`所有者={0}，NPC={1}，已遍历={2}`；
- `log.70e363d7c042`（`doOnAllNpcs`）：同样带三个参数。

判读方式：

- 带「所有者」的是 `MovementNotifyTask` 路径；不带的是其它 `doOnAllNpcs` 调用方。
- 堆栈首帧为 `CreatureEventHandler.checkAggro(CreatureEventHandler.java:84)` → 确认 despawn 竞态。
- 堆栈首帧为 `MovementNotifyTask$MoveNotifier.visit(MovementNotifyTask.java:202)` → AI/lifeStats 缺失。
- 堆栈落在 `QuestEngine.onAtDistance` → 转查具体任务的距离处理器。
- 记录 NPC objectId 与该对象的 despawn/传送时间，确认是否落在快照与访问之间的窗口内。

## 7. 修复预案（待复现确认，不在本次记录中实施）

- 最小防护：`MoveNotifier.visit` 在读取 AI/lifeStats 前跳过 `!object.isSpawned()`；
  `checkAggro` 把 `owner.getActiveRegion()` 提为局部变量并做 null 检查。
- 若确认是 despawn 竞态，补一条确定性回归测试：快照后置 `isSpawned=false`，
  验证访问器不再抛异常且该 NPC 不再参与仇恨/距离处理。
- 复现拿到堆栈前不先改语义，避免掩盖真实失败点。
