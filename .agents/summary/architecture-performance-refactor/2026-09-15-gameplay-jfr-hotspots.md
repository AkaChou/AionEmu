# 游戏内 JFR 热点分析 / Gameplay JFR hotspots

日期 / Date: 2026-09-15 · 提交 / Commit: `de4a66e20` · 采集 / Capture: `jcmd <pid> JFR.start name=Play settings=profile duration=300s filename=/tmp/play-1.jfr`
窗口 / Window: 19:33:13 – 19:38:13（300s，真实客户端在游戏内活动；单机、单客户端、以跑图/打怪为主）
原始文件 / Raw file: `/tmp/play-1.jfr`（**不入库**，`.gitignore` 已加 `*.jfr`）

## 一、改造目标的验证结果（正面）

| 检查项 | 结果 | 判据 |
|---|---|---|
| 热路径锁竞争 | **0 次** | `jfr print --events jdk.JavaMonitorEnter` 计数为 0；`contention-by-site` 视图 "No events found" |
| 本次改造的类是否仍在热点 | **0 次** | `KnownList` / `MapRegion` / `PacketProcessor` / `NettyConnectionHandler` / `AConnection` / `DirectMemoryLoginChannel` 均未出现在 `hot-methods` |
| GC | 5 分钟 3 次回收，暂停合计 **52.8ms**（最长 21.6ms） | `jfr view gc` + `jdk.GCPhasePause` 求和 |
| 快照分配的实际代价 | `KnownList.knownObjectsSnapshot()` 仅 **9.5 MB / 0.27%** | 见下表：P1「去掉快照以省 GC」的收益本就很小，保留契约语义的取舍正确 |

> 应用自身真实异常很少：`JavaExceptionThrow` 共 71,948 条中 **71,455 条来自 IDEA 调试代理**（`com.intellij.rt.debugger.agent.CaptureStorage$3.run()`，调试器附加所致，非业务代码）。业务侧只有 172 条 `PathService$IncompletePathSearchException: PATH search incomplete: NODE_LIMIT after 51276 nodes`，值得单独关注。

## 二、真正的热点：geo 寻路

CPU（`hot-methods`，样本 802）：

| 方法 | 占比 |
|---|---|
| `PathData$MapData.searchLowLevel(...)` | 20.70% |
| `RealGeoData.getMap(int)` | 11.35% |
| `PathData` 各 sector/node 助手（heightAt/simpleNode/simpleNeighbor/block/distance/node/reset/step…） | ≈20% |
| A* 开放集 `PriorityQueue` + `Comparator` 相关 | ≈12% |
| geo 碰撞其余部分（BIH/Terrain 等） | ≈5% |

**A\* 寻路及其查表合计占样本一半以上**，并伴随 172 次 `NODE_LIMIT after 51276 nodes`（每次失败要空转 5 万节点才放弃）。

分配（`jdk.ObjectAllocationSample`，采样权重合计 ≈3466 MB / 5 分钟）：

| 站点 | 采样权重 | 占比 | 根因 |
|---|---|---|---|
| `RealGeoData.getMap(int)` | **1440.9 MB** | **41.6%** | 见「假 IntObjectHashMap」 |
| `PathData$MapData.searchLowLevel(...)` | **1211.2 MB** | **34.9%** | `Map<Long, SearchNode> visited` 的 `Long` 装箱 |
| `WorldMapInstance.worldMapObjectsSnapshot()` | 94.9 MB | 2.7% | 快照拷贝（热点路径上的第二种快照） |
| `RetailPatternAI2.runEvent(...)` | 33.0 MB | 1.0% | AI |
| `PlayerQuestEventPort.completedQuestIdsOf(Player)` | 27.6 MB | 0.8% | quest 端口装箱 |
| `PathData.searchToAnyPortal(...)` | 16.0 MB | 0.5% | 同上 Long 装箱 |
| `KnownList.knownObjectsSnapshot()` | 9.5 MB | 0.27% | 本次保留的快照 |

按类：`Integer` 42.5% + `Long` 31.9% = **74% 的分配来自装箱**。

### 根因一：项目里的 `IntObjectHashMap` 是「假」原始类型 Map

`src/main/java/com/aionemu/commons/utils/collections/IntObjectHashMap.java:13`

```java
public class IntObjectHashMap<V> extends LinkedHashMap<Integer, V> { ... }
```

它没有原始 int 键，`get(int)` / `put(int, V)` 全部走 `Integer` 自动装箱；worldId 远超 `Integer` 缓存范围（-128..127），因此**每次查表都新分配一个 Integer**。`RealGeoData.getMap(int)` 内部只有一行 `geoMaps.get(worldId)`，却被 `GeoService.getTerrainZ` / `PathService.waterArea` 等按节点级频率调用 → 单站点 41.6% 分配、11.35% CPU。

该类在 **87 个文件**中使用（多数是加载期数据表，但 `RealGeoData` / `World` / `WorldMapInstance` / `ZoneService` / `ItemSetData` 属运行期热路径）。

### 根因二：A\* 的 visited 集合用 `Long` 装箱键

`PathData.java:456` 的 `Map<Long, SearchNode> visited`（以及 `portalGoals` / `blockVisited` 等）逐节点 `put/get` → `Long.valueOf` 占全部分配 31.9%，与 `searchLowLevel` 的 20.7% CPU 同源。

## 三、已实施的优化（本提交）

1. **`RealGeoData.getMap` 改为免装箱二分查表**：加载期把 `geoMaps` 展平为「升序 worldId 数组 + 对齐的 GeoMap 数组」的只读快照（`WorldMapLookup`，单 volatile 引用原子发布），查表走二分，彻底移除每次调用的 `Integer` 装箱与哈希查找。
   - 覆盖：`src/main/java/com/aionemu/gameserver/world/geo/RealGeoData.java`；测试 `RealGeoDataLookupTest`（命中/未命中/空表/单元素）与既有 `RealGeoDataConcurrencyTest`。
2. **`PathData` 搜索工作区换用原始 long 键容器**：新增 `com.aionemu.commons.utils.collections.LongObjectHashMap`（开放寻址，`get/put/putIfAbsent/clear/size`，无迭代），把 A* 的 `visited`、`portalGoals` 以及分块搜索的 `blockVisited`（int 键自动宽化，不装箱）从 `HashMap<Long,…>`/`HashMap<Integer,…>` 迁出。
   - 覆盖：`src/main/java/com/aionemu/gameserver/world/geo/path/PathData.java`；测试 `LongObjectHashMapTest`（覆盖扩容、线性探测、极值键、clear 复用）与既有 `PathDataTest`(27)/`PathServiceConcurrencyTest`(13)/`NpcMoveControllerPathTest`(59)。
   - 未引入新依赖（无 fastutil），保持零依赖与可测试性。

## 四、仍待处理（后续任务）

1. **`IntObjectHashMap` 系统性替换**：该「假原始类型 Map」在 87 个文件中使用，多数是加载期数据表；替换需保留插入序语义，属独立任务。
2. **`WorldMapInstance.worldMapObjectsSnapshot()`**：确认调用频率与必要性，考虑缩小范围或复用。
3. **`NODE_LIMIT` 治理**：诊断结论见下节，代码修复需要先拿到失败坐标。

## 五、`NODE_LIMIT`（172 次/5 分钟）诊断结论

- **发起者**：`NpcMoveController.collectPath()`（异步寻路结果回收）→ `requestTargetPath/requestLocationPath` → `PathService.navigateToLocationAsync`；JFR 线程名为 `pathfinder`。即 **NPC 移动寻路**，不是玩家或 GM 命令。
- **预算来源**：`gameserver.geo.path.max.nodes = 50000`（`GeoDataConfig.GEO_PATH_MAX_NODES`）；报文里的 51276 是 `processed` 计数，含层级搜索拆分后的计数。
- **节流现状**：每个 NPC 失败后 `pathRetryAt = now + 500ms`（`PATH_RETRY_DELAY_MS`），并有 `shouldReactToPathFailure` → `TargetEventHandler.onPathFindFailed` 让 AI 反应，因此单 NPC 最坏约 2 次/秒。172 次 ≈ 由少量 NPC 反复失败构成。
- **性质判定**：状态是 `NODE_LIMIT` 而**不是** `NO_PATH` —— `NO_PATH` 表示开放集被穷尽（确定不可达），而 `NODE_LIMIT` 表示预算耗尽时开放集仍非空，**不能断定目标不可达**，也可能是目标过远/启发式偏弱导致的过度展开。因此需要失败坐标才能定性。
- **下一步取证（游戏内即可，无需改代码）**：
  1. `//geo path` —— 打印 `PATH … nodeLimit=… noPath=… processedNodes=…` 与 `PATH recovery … replan=a/f/f nearest=a/s`，用于观察失败增长速率与「失败是否集中在 replan」。
  2. 对可疑 NPC 执行 `//ai2`（`Ai2Command` 切换 `AbstractAI.setLogging`）后，`NpcMoveController.logPathRequest/logPathResult` 会输出 `PATH request/result … from=(x,y,z) to=(x,y,z)`；配合 `AIConfig.MOVE_DEBUG` 记录到 `log/aidebug.log`。
  3. 拿到坐标后即可判定：同一目标反复失败 → 加带 `obstacleVersion` 的负缓存（短 TTL，障碍变化即失效）；跨岛/跨层不可达 → 前置连通性或高度差预检；预算型 → 对 NPC 局部移动降低重试预算（首次仍用 50000）。

## 六、口径与边界

- 采样权重来自 JFR `ObjectAllocationSample`（TLAB 采样加权），用于**相对排序**，不等于精确字节数；GC 暂停、锁竞争、热点占比均为采样统计。
- 单机、单客户端、以跑图打怪为主；组队/攻城/大规模并发未覆盖（当前无法测试）。
- 本窗口未观察到本次改造引入的锁竞争或热点，但这**不等于**「改造带来 X% 提升」；严格收益量化需要在 `/tmp` 干净副本上跑旧提交并复现同样操作。
- `RealGeoData` / `PathData` 优化已随本提交落地、通过 172 例聚焦测试，并已在重启后复测（见第七节）；复测同时暴露「窗口工作量不一致」这一口径问题，受控数字仍需按第七节末的归一化方案重测。

## 七、优化前后对比（重启后复测，非受控 A/B）

复测：服务重启（20:11:39，pid 69446，0 ERROR/WARN）后以完全相同参数再录 300s → `/tmp/play-2-after.jfr`（20:16:31–20:21:31）。

| 指标 | 基线 `/tmp/play-1.jfr` | 后置 `/tmp/play-2-after.jfr` |
|---|---|---|
| `java.lang.Integer` 分配 | 1472.5 MB（42.49%） | **23.4 MB（3.45%）** |
| `java.lang.Long` 分配 | 1105.9 MB（31.91%） | **0.7 MB（0.10%）** |
| `RealGeoData.getMap(int)` | 1440.9 MB | **0 MB（不再出现）** |
| `PathData.searchLowLevel(...)` | 1211.2 MB | **0 MB（不再出现）** |
| 采样分配合计 | 3465.8 MB | **676.2 MB（−80.5%）** |
| GC | 3 次 / 52.8 ms | 1 次 / 102 ms（单次 G1New 102ms） |
| `jdk.JavaMonitorEnter` | 0 | **0** |
| `searchLowLevel` CPU 占比 | 20.70% | 1.27% |
| `RealGeoData.getMap` CPU 占比 | 11.35% | 不再进入热点 |
| NODE_LIMIT 异常（原始计数） | 344 | 2 |

**两个目标站点归零是机制级结论**（源码 + 单测证明不可能再装箱），不受工作量影响。但**聚合数字不可当作受控 A/B**：

- CPU 采样总数 802 → 237（3.4 倍差距），NODE_LIMIT 344 → 2，说明后置窗口的 NPC 寻路工作量明显更小；
- 因此 −80.5% 的分配下降里，有一部分来自“活干得更少”，不能全部归因于本次优化。

若要拿到受控数字，建议按「同活动 + 同长度」再测一次，并用 `//geo path` 的 `submitted/completed/processedNodes` 作为归一化分母（分配 ÷ 处理节点数）。

**后置窗口的新分配 TOP（下一步候选）**：`Object[]` 269.4 MB（39.8%，疑似 `Map.of`/不可变集合与流式调用）、`KeyValueHolder` 84.0 MB（12.4%）、`PathData$MapData$Node` 40.8 MB（6.0%，A* 节点池增长）、`WorldMapInstance.worldMapObjectsSnapshot()` 19.4 MB、`PlayerQuestEventPort.toInventoryMap/completedQuestIdsOf` 44.1 MB。

## 八、第二轮复测：`AR-007` 改动前后（非受控 A/B）

复测：21:32:26 重启（修复字节码已确认加载：`javap` 可见 `doOnAllNpcs`）后，以同样参数录 300s → `/tmp/play-3-round2.jfr`（21:33:26–21:38:26）。

| 指标 | 修复前 `/tmp/play-2-after.jfr` | 修复后 `/tmp/play-3-round2.jfr` |
|---|---|---|
| 采样分配总量 | 687.4 MB | **304.1 MB（−55.8%）** |
| `jdk.ExecutionSample`（归一化口径） | 237 | 272（同量级） |
| `ObjectAllocationSample` 样本数 | 1463 | 603 |
| `WorldMapInstance.getNpcs()` | **207.2 MB（30.1%）** | **0（不再出现）** |
| `WorldMapInstance.worldMapObjectsSnapshot()` | 19.9 MB | **0** |
| `QuestStateList.getAllFinishedQuests()` | 5.2 MB | **0** |
| `QuestSnapshot.<init>` | 111.6 MB | 28.2 MB |
| └ `KeyValueHolder`（来源为 QuestSnapshot） | 84.9 MB | 27.1 MB（**未归零**） |
| `PlayerQuestEventPort.completedQuestIdsOf` | 36.8 MB | 8.3 MB |
| `PlayerQuestEventPort.toInventoryMap` | 23.1 MB | 6.1 MB |
| 新 TOP：`TemporarySpawn.getTime`（`String.split`） | — | **52.4 MB（17.2%）** |
| 新 TOP：`RewardServiceDAO.getAvailable` | — | 33.2 MB（10.9%） |
| `PathData$MapData$SearchWorkspace.node` | 45.8 MB | 35.0 MB |
| IDE 调试器 `CaptureStorage`（非游戏代码） | 18.1 MB | 31.9 MB（10.5%） |

**归零是机制级结论**（源码 + 单测证明该调用链已不存在），不依赖窗口工作量：`getNpcs` / `worldMapObjectsSnapshot` / `getAllFinishedQuests` 三处。

**必须更正的结论**：`KeyValueHolder` 没有归零。JFR 调用链是 `AbstractMap$KeyIterator.next() → MapN$MapNIterator.next()` —— 查 JDK 源码（`ImmutableCollections.MapN`）可知它只覆写了 `entrySet()`，**没有覆写 `keySet()`/`forEach()`/`values()`**，所以 `entrySet`、`keySet`、`forEach` 三条路径都会每条目分配一个 `KeyValueHolder`。把 `entrySet().stream()` 换成 `keySet()` 循环只能省掉 stream 管道对象，**不能**消除该分配；真正修法是把 `withXxx` 链上的逐次校验合并为一次。

**下一轮候选（按收益排序）**：

1. `TemporarySpawn.getTime`：每次 `isInSpawnTime()` 都 `String.split(":")` 解析时间窗（17.2%，且有 57 MB 级 `String[]` 同源）→ 改为加载期解析一次。
2. `QuestSnapshot` 校验一次化（27.1 MB `KeyValueHolder` + 相关遍历）。
3. `RewardServiceDAO.getAvailable`（10.9%）、`PathData$MapData$SearchWorkspace.node`（A* 节点池）。
4. `ArrayList.grow` 5.59% / `Arrays.copyOfRange(byte[])` 5.76%（传输层，需另查）。

## 九、第三轮改动（针对第八节暴露的新热点）

| 站点（第八节实测） | 改动 | 预期 |
|---|---|---|
| `TemporarySpawn.getTime` 52.4 MB（17.2%） | `TemporarySpawn` 首次使用时解析 `时.日.月` 并缓存（`volatile Integer[3]`），`isInSpawnTime()` 不再做 6 次 `String.split("\\.")`；解析失败语义与旧实现一致（段数不足仍抛数组越界） | 该站点与同源 `String[]` 归零 |
| `QuestSnapshot` 校验链 27.1 MB `KeyValueHolder` | 容器校验改为“入参还不是不可变副本才逐条目校验”（`Set.copyOf(x) == x` / `Map.copyOf(x) == x` 识别），首建路径由 `PlayerQuestEventPort` 返回可变集合以保证仍校验一次；新增 `QuestSnapshotValidationTest` 锁定该契约 | `KeyValueHolder` 归零 |
| `PlayerQuestEventPort` | 端口侧不再 `Map.copyOf`/`Set.copyOf`（改由快照构造器统一冻结），减少一次复制 | 小幅下降 |

新增/更新闸门与用例：`TemporarySpawnTimeWindowTest`（4 例，含“首次读取后改写字段不影响结果”的缓存契约）、`QuestSnapshotValidationTest`（3 例：可变入参仍被校验、结果被冻结）。

复测口径：重启后按同一方式再录 300s，对比 `TemporarySpawn.getTime`、`KeyValueHolder`、`Arrays.copyOfRange(byte[])` 与总权重；同时保留 `jdk.ExecutionSample` 计数作为工作量归一化分母。

## 十、第三轮复测：`AR-007`/`AR-008` 改动确认

复测：23:23:16 重启（字节码已确认含 `spawnTimeParts` 与 `validated*` 助手），23:36:15–23:41:15 录 300s → `/tmp/play-4-round3.jfr`。

| 指标 | play-2（改造前） | play-3（第二轮） | **play-4（第三轮）** |
|---|---|---|---|
| `TemporarySpawn.getTime`（我方首帧） | 7.9 MB | **52.4 MB** | **0（消失）** |
| `KeyValueHolder`（合计） | 85.9 MB | 27.5 MB | **1.66 MB** |
| └ 其中 `withXxx` 重入路径 | 84.9 MB | 27.1 MB | **0** |
| └ 剩余来源 | — | — | `QuestCraftSnapshot.<init>` 0.79MB、`validatedInventory` 0.67MB（首建校验一次，设计如此）、`RetailPatternAI2.hasCompleteMasterData` 0.10MB |
| `WorldMapInstance.getNpcs` | 207.2 MB | 0 | **0** |
| `QuestStateList.getAllFinishedQuests` | 5.2 MB | 0 | **0** |
| `QuestSnapshot` 相关合计 | 116.6 MB | 28.9 MB | **11.7 MB** |
| `PlayerQuestEventPort.toInventoryMap` | 23.1 MB | 6.1 MB | **2.3 MB** |
| 采样分配总量 | 687.5 MB | 304.1 MB | 551.6 MB（**本窗口 geo 碰撞工作量显著更大**） |
| `jdk.ExecutionSample`（归一化分母） | 237 | 272 | **362** |
| 分配样本数 | 1463 | 603 | 1791 |

**结论**：本轮三个目标站点全部达成（`getTime` 归零、`KeyValueHolder` 的 `withXxx` 重入归零、`getNpcs` 持续为 0）。**总权重上升不可解读为回归**：本窗口 CPU 采样 272→362，且分配结构被 geo 碰撞主导（`BIHNode.intersectWhere` 151.2 MB / 21.2%、`BoundingBox.collideWithRay` 82.9 MB、`Matrix4f.invert` 35.2 MB、`CollisionResults.addCollision` 30.9 MB、`Vector3f.clone` 24.5 MB；类占比 `Vector3f` 18.4%、`float[]` 9.0%、`Matrix4f` 6.4%）。

**下一轮候选（新 TOP，全部在 geoEngine 碰撞/射线路径）**：

1. `BIHNode.intersectWhere` + `BoundingBox.collideWithRay`（合计 234 MB / 33%）：射线查询的临时对象（`BIHStackData`、`CollisionResult`、`CollisionResults`）——可考虑查询对象复用或线程本地缓冲。
2. `Matrix4f.invert`（35.2 MB）与 `Vector3f.clone`（24.5 MB）：矩阵求逆/向量克隆在每次查询中重复计算，可缓存逆矩阵或改写为无克隆路径。
3. `ArrayList.grow` 12.08%：多与上面两类临时集合同源。
4. 仍未处理：`Throwable.fillInStackTrace` 4.04%（异常做控制流）。

## 十一、第四轮：geo 碰撞预剪枝无分配化（复测确认）

复测：服务 13:36:26 重启（字节码含 `clipRayRange`/`recycleScratch`/`BOUND_RANGE`），15:44:51–15:49:51 录 300s → `/tmp/play-5-geo.jfr`。

| 站点 | geo 改动前 `play-4` | geo 改动后 `play-5` | 变化 |
|---|---|---|---|
| `BoundingBox.collideWithRay` | 97.2 MB | **0.0 MB（消失）** | **−100%** |
| `CollisionResults.addCollision` | 30.9 MB | 2.1 MB | −93% |
| `Vector3f.clone` | 24.5 MB | **0.0 MB（消失）** | **−100%** |
| `BIHNode.intersectWhere` | 217.9 MB | 158.0 MB | −28% |
| `Matrix4f.invert` | 35.2 MB | 27.1 MB | −23% |
| `ArrayList.grow` | 68.7 MB | 28.4 MB | −59% |
| `jdk.ExecutionSample`（归一化分母） | 362 | 527 | 本窗口工作量更大 |
| 采样分配总量 | 551.6 MB | 602.0 MB | 原始值不可比 |
| **每 CPU 采样分配量** | 1.52 MB | **1.14 MB** | **−25%** |

改动内容（见提交）：`BoundingBox.clipRayRange` 抽取 t 区间计算；`BIHTree` 预剪枝对 `BoundingBox` 走零分配路径（线程本地 `float[2]`，其余包围体保持原路径）；`BIHNode.intersectWhere` 的 5 个临时向量改对象池并在两个出口回收。数值等价由 `BoundingBoxRayIntersectionTest#clipRayRangeMatchesCollisionResultsPath`（6 种射线形态对拍 closest/farthest）与 181 例 geo 套件守护。

**剩余与下一轮候选**：

1. `BIHNode.intersectWhere` 158 MB 的主体是 `ArrayList<BIHStackData>` + 每次分裂 `new BIHStackData`（见 `BIHNode.java:396`）——改为每线程原始数组栈。
2. `Matrix4f.invert` 27.1 MB：世界矩阵的逆每次查询重算，可缓存。
3. `PathData$MapData$SearchWorkspace.node/openNode` 175 MB：A* 节点池增长（本窗口寻路更多）。
4. `Throwable.fillInStackTrace` 37.1 MB（+67%）：疑似用异常做控制流，需定位抛出点。
