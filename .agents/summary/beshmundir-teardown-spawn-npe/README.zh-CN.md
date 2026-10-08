# 帕休曼迪尔寺院副本销毁期 AI spawn 链 NPE（DespawnLich 281697）

## 1. 现象（2026-10-08 16:10:42 实机日志）

`/reset` → `<副本销毁>300170000 2` 之后立即刷 ERROR：

```
生成 NPC 281697 时出错，世界 300170000，x-y 979.0-130.0
java.lang.NullPointerException: Cannot invoke "MapRegion.getParent()" because
  "WorldPosition._jr$ig$mapRegion(...)" is null
  WorldPosition.getWorldMapInstance(113) ← InstanceScaler.onBeforeSpawn(82) ← NpcController.onBeforeSpawn(142)
  ← World.spawn(537) ← SpawnEngine.bringIntoWorld(418/304) ← VisibleObjectSpawner.spawnNpc(276)
  ← SpawnEngine.getSpawnedObject(171)/spawnObject(108) ← RetailPatternAI2.spawnAt(2931)/spawn(2788)
  ← RetailPatternAI2.execute(1756)/executeActions(1502)/runEvent(…) ← handleDespawned(1391)
  ← AbstractAI.handleGeneralEvent(559)/onGeneralEvent(311) ← NpcController.onDespawn(173)
  ← VisibleObjectController.onDelete(84) ← InstanceService.destroyInstance(272)
  ← resetPlayerInstances(153) ← Reset(36)
```

NPC 281697 = DespawnLich，来自真端 pattern 链：Temadaro（216586）despawn → spawn 281697 →
其 `on_wake_up` 显示 1400469 + 广播 6981 + despawn_self（见
`.agents/summary/beshmundir-macunbello-conditional-spawn/`，该链此前已实机验证）。

## 2. 根因链（源码逐级证实）

1. `InstanceService.destroyInstance`（InstanceService.java:258）**先** `map.removeWorldMapInstance(instanceId)`，
   **后**才逐个 `onDelete`（:265-274）——注销与删对象之间是敞口窗口。
2. 删对象同步触发 AI despawn 链：`NpcController.onDespawn` → `RetailPatternAI2.handleDespawned`
   → `runEvent("on_despawn")` → pattern 的 spawn 动作。
3. `RetailPatternAI2.spawnAt`（:2931）以 `getOwner().getInstanceId()=2` 调 `SpawnEngine.spawnObject`。
4. `SpawnEngine.bringIntoWorld(VisibleObject,int,int,float,float,float,byte)`（SpawnEngine.java:413）先
   `storeObject` 再 `World.setPosition`；`World.setPosition`（World.java:486-489）查到
   `getWorldMapInstanceById(2)==null`（步骤 1 已注销）时**静默 return**，`mapRegion` 保持 null。
5. `World.spawn`（World.java:537）不校验位置是否已解析，继续 `onBeforeSpawn` →
   `InstanceScaler.onBeforeSpawn`（InstanceScaler.java:82）读 `mapRegion.getParent()` → NPE。
   （即便没有 InstanceScaler，下一行 `getActiveRegion().getParent()` 也会同样 NPE。）
6. NPE 被 `VisibleObjectSpawner.spawnNpc` 的 catch（:277-280）吞掉记 ERROR；但步骤 4 的 `storeObject`
   已完成 → World 注册表（allObjects / allNpcs / local objects）残留一个未生成的“幽灵”对象，无路径回收。

## 3. 与既有护栏（IR-012）的关系

IR-012「副本销毁后残留的延迟任务必须自行收口」只覆盖**实例处理器自身排定**的 spawn，
其边界明示“AI 定时器、任务线程与引擎内部延时不在本护栏内”。本次即 **AI 路径**首次撞上同一 NPE 点，
按“守护写在统一入口”的同一口径外扩到 `SpawnEngine.bringIntoWorld`。

## 4. 修复（本次改动）

- `src/main/java/com/aionemu/gameserver/spawnengine/SpawnEngine.java`：
  `bringIntoWorld(VisibleObject,int,int,float,float,float,byte)` 在 `storeObject` **之前**校验目标实例
  是否仍在 `WorldMap`；不存在则记 WARN 并放弃（不登记、不 spawn、无幽灵对象）。覆盖所有走该入口的
  路径（AI / 任务 / 实例处理器 / 启动），不改变实例存在时的任何行为。
- `src/main/resources/messages.properties` / `messages_zh_CN.properties`：新增
  `log.3e552d08a3b9=Skipped spawn of npc {0}, world {1}, instance {2}: the instance is already unregistered`
  / `跳过生成 NPC {0}，世界 {1}，实例 {2}：副本实例已注销`。
- `src/test/java/com/aionemu/gameserver/spawnengine/SpawnEngineTeardownSpawnGuardTest.java`（新增）：
  复用 `TestWorld extends World` + `GameWorldBootstrapServices` 注入范式（同
  `RetailConditionSpawnEngineTest`）。实例已注销 ⇒ 不 store、不 spawn；实例仍在 ⇒ 照常放行。

## 5. 验证状态

### 聚焦测试（2026-10-08，IDEA MCP，用户授权；不做命令行构建、不启停服务端）

- `SpawnEngineTeardownSpawnGuardTest`：**红→绿闭环**。
  - 红：临时把护栏条件改为 `false && …` 复跑 ⇒ `skipsSpawnWhenTheTargetInstanceWasAlreadyUnregistered`
    失败 `expected: <0> but was: <1>`（store 仍被调用，即修复前行为）；
  - 绿：恢复护栏后 2/2 通过（含实例仍在时照常放行的对照用例）。
  - 注：外部编辑后**首跑吃到旧字节码**（旧编译产物=护栏生效态），重跑才进入红态——与既有经验一致。
- `LocalizedLogArgumentsTest` 2/2、`LocalizedLogCallsTest` 1/1 通过（键双包一致、3 参数 = {0}{1}{2}）。

### 待办（实机复测，用户）

进 BT 新实例 → 触发 Temadaro despawn（或在含 Temadaro 的实例上 `/reset`）→ 日志应只出现 WARN
「跳过生成 NPC 281697，世界 300170000，实例 2：副本实例已注销」，不再有「生成 NPC 281697 时出错」
ERROR + NPE。（若 WARN 显示原始键名 `log.3e552d08a3b9`，属语言包未重载，重启/热重载语言包后即正常；
护栏逻辑本身不依赖消息包。）

## 6. 边界与遗留（本次未修）

- `RetailPatternAI2.spawnAt` 对 `spawned == null` 无判空：若该 spawn 动作同时带
  `spawn_id != SPAWN_ID_NONE` 或 `live_time > 0`，`writableDespawnStates().put(null, …)`
  （ConcurrentHashMap 拒绝 null 键）会二次 NPE。本次未触发，属独立隐患。
- `WorldPosition.getWorldMapInstance()` 对未解析位置仍直接 NPE（既有设计，未改）。
- `World.setPosition` 的静默 return 语义未动（它同时服务传送等非刷怪路径，改动面更大）。
