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

## 十二、第五/第六轮：BIH 遍历栈、水中寻路死区与 play-7 复测

> 本节的分配数字统一按「栈中第一个 `com.aionemu.` 帧」聚合，与第十一节口径略有差异（同一件事：`intersectWhere` 在 `play-5` 为 108.1 MB、`play-6` 为 17.2 MB）。

### 12.1 BIH 遍历栈 + 线程本地逆矩阵（第五轮，`/tmp/play-6-stack.jfr`）

服务 16:22 重启（`target/classes` 已含未提交的数组栈与 `INVERSE_MATRIX`），16:48–16:53 录 300s。

| 站点 | `play-5` | `play-6` | 变化 |
|---|---|---|---|
| `BIHNode.intersectWhere` | 108.1 MB | 17.2 MB | −84% |
| `Matrix4f.invert` | 26.4 MB | 0 | 归零 |
| `SearchWorkspace.node` | 147.2 MB | 5674.8 MB | 池上限实验值，已回退 |

**池上限实验教训（AR-010）**：把 `PathData.SearchWorkspace.nodes` 的每线程池截到 8192 后，`node()` 在每次搜索里重新分配，产生 5.7 GB churn；`jcmd GC.class_histogram` 显示存活 `PathData$MapData$Node` 从 **329 万 / 158 MB** 降到 2.8 万 / 1.35 MB —— 内存便宜、churn 昂贵。该实验已 `git checkout` 回退。

### 12.2 水中寻路死区（第六轮，`/tmp/play-7-water.jfr`）

症状：Taloc's Hollow（300190000）被拉进水池的怪停在水面不动；水边（浅）60s 回家，水里（深一点）永不回家。

定位（用 `water-volumes.bin` 与模板数据实算）：

- 卡住点 (486.12, 839.28, 1270.83) 在 volume 497 内，`surfaceZ = 1271.519`（没入 0.688m）；`bound_radius = 0.78/1.096`。
- `isSubmerged` 的水面余量写死 0.5m → 判为"游泳"；`waterEdge/allows` 用 `max(0.5, collision) = 0.78m` → 位置不合法。**0.5m~clearance 之间的死区**：被判为游泳却过不了空间寻路，地面 A* 又被跳过 → 必然 NO_PATH。
- `NpcMoveController` 的 HOME 30s/60s 瞬移兜底写在 `case HOME` 内，会被 `canPerformMove`、`prepareGroundPath() == false` 的提前 return 绕过，回家兜底不可达。

修复：

- `PathService.immersionMargin(owner) = max(max(0.5, collision), boundRadius.upper - 0.1)`，`isSubmerged` 使用该余量 —— 保证「判为游泳 ⇒ 一定过得了 waterEdge」，同时贴近客户端「没入水面才算游泳」的表现（与 `ZoneLevelService` 的 `noseHeight` 同源）。
- `NpcMoveController.abortTimedOutHomeReturn()` 提前到 `moveToDestination()` 入口，任何早退分支都不能绕过归家兜底。

验证：用户真机确认水池里的怪恢复正常移动；`play-7` 于 17:42:39–17:47:35 录 296s（该窗口开着 AI 日志，含水中场景）。

| 站点 | `play-5` | `play-7` | 说明 |
|---|---|---|---|
| 采样分配总量 | 590.3 MB | 449.9 MB | 本轮还额外开着 AI 日志 |
| `SearchWorkspace.node` | 147.2 MB | 159.9 MB | 池实验回退后回到常态 |
| `BIHNode.intersectWhere` | 108.1 MB | 18.2 MB | 数组栈改造生效 |
| `Matrix4f.invert` | 26.4 MB | 0 | 归零 |
| `Vector3f$1.create` | 15.0 MB | 28.6 MB | `newInstance <- intersectWhere`，池耗尽时新建 |

**`play-7` CPU 采样（1625 样本，A* 相关约 75%）**：

| 站点 | 样本 | 说明 |
|---|---|---|
| `searchLowLevel`（含子调用） | 1389 | A* 主体 |
| ├ 优先队列复合比较器（`comparingInt/Double` + `siftUp/siftDown`） | 434 | `OPEN_NODE_ORDER` 的 lambda 间接层 |
| ├ `Sector.simpleNeighbor/simpleNode/heightAt` | 337 | 邻居展开与地形采样 |
| ├ `workspace()` ThreadLocal（初始化 + 探测） | 182 | 每线程首次建工作区；`setInitialValue` 103 + `getEntryAfterMiss` 80 |
| ├ `getTerrainZ → RealGeoData.indexOfWorldId` | 174 | 每个采样点都做一次 worldId 二分查找 |
| └ `LongObjectHashMap.indexOf` | 83 | visited 表散列 |

GC：296s 内 4 次暂停（88.0ms + 44.2ms + 8.80ms + 0.07ms），合计约 141ms。

**下一轮候选（按 ROI）**：

1. A* 搜索期缓存 `GeoMap`：`PathService.terrain(worldId)` 的 lambda 每次采样都走 `getTerrainZ → WorldMapLookup.find`（174 样本 / 11%）。
2. 工作区池化 + 显式传递（`ConcurrentLinkedDeque` + 租约、用完归还）：省 `workspace()` 的 ThreadLocal 初始化/查找（182 样本 / 11%），并把 67 份工作区降到 5~10 份（约 −130 MB 堆）。
3. `BIHNode.intersectWhere` 的 5 个 scratch `Vector3f` 改线程本地 float 数组（28.6 MB 分配 + 池交互）。
4. 优先队列比较器内联（手写比较器或让节点实现 `Comparable`）。
5. Spring 门面引用固化：`GameServerNetworkServices.packetLoggerService()` 13.5 MB（网络写路径）、`GameEngineServices.questEngine()` 12.8 MB。
6. Quest 快照集合复制：`QuestSnapshot.withCompletedQuestIds` 14.2 MB + `PlayerQuestEventPort.completedQuestIdsOf` 7.8 MB。

**待补**：本轮 `jcmd GC.class_histogram` attach 失败（Azul 26 报 `state is not ready to participate in attach handshake`），下次重启后补抓存活对象（重点 `PathData$MapData$Node` 数量与工作区份数）。

### 12.3 工作区租约池、地形缓存与节点槽位回退（第七/第八轮，`play-8`/`play-9`）

按 ROI 落地了四项改动（`play-8`，`/tmp/play-8-alloc.jfr`）：

1. **A* 搜索期缓存世界地图**：`GeoService.getGeoMap(worldId)` + `PathService.terrain(worldId)` 把 `geoData.getMap` 提到 lambda 外，四个地形入口统一走它。
2. **工作区租约池**：`WORKSPACE_LEASE` + `WORKSPACE_POOL` + `MAX_POOLED_WORKSPACES=16`，四个顶层入口 acquire/`finally` release，`SearchWorkspace.resetState()` 归还前清状态但保留节点池实例。
3. **`BIHNode` 射线 scratch**：5 个 `Vector3f` 由对象池改每线程固定 `RayScratch`（删除 `recycleScratch` 与两个回收出口）。
5. **Spring 门面引用固化**：`questEngine()`/`packetLoggerService()` 只在解析到真实 bean 时缓存，`destroy()` 清空。

`play-8` 对 `play-7` 的结果：

| 指标 | `play-7` | `play-8` | 结论 |
|---|---|---|---|
| `workspace()` CPU 样本 | 182 | **15** | −92% |
| `ThreadLocal.setInitialValue` | 103 | **0** | 归零 |
| `getTerrainZ → indexOfWorldId` | 174 | **0** | 归零 |
| `Vector3f$1.create` | 28.64 MB | **0.74 MB** | −97% |
| `packetLoggerService()` / `questEngine()` | 13.53 / 12.77 MB | **0 / 0** | 归零 |
| 存活 `SearchWorkspace` | 67 | **8** | 池化生效 |
| **`PathData$MapData$Node` 分配** | 132.3 MB | **188.4 MB** | ❌ 副作用 |
| **存活 `Node`** | 329 万 / 158 MB | **464 万 / 222.7 MB** | ❌ 副作用 |

副作用根因：池化把 67 份工作区收敛到 8 份后，**这 8 份全部被重度 A* 膨胀**（以前只有 5~8 个 pathfinder 线程的工作区是大的，其余 59 个线程各只占几千槽位）。而 `searchLowLevel`/`searchToAnyPortal` 的邻居展开对**每个方向**都先 `step()` 取一个 Node 槽位，即使该邻居随即被 `continue` 或已在 `visited` 中——槽位数 ≈ 8 × 处理节点数。

**第八轮修复（节点槽位回退）**：`SearchWorkspace.nodeMark()` / `rollbackNodes(mark)`，只有真正写入 `visited` 的邻居（`known == null` 分支）保留槽位，其余展开用完即回退复用。`findBlockPath` 无需改动（它本就在需要时才取块节点）。

`play-9`（`/tmp/play-9-rollback.jfr`，300s，同 profile）：

| 指标 | `play-8` | `play-9` | 变化 |
|---|---|---|---|
| 采样分配总量 | 434.2 MB | **278.0 MB** | **−36%** |
| `SearchWorkspace.node` | 242.0 MB | **59.7 MB** | **−75%** |
| `PathData$MapData$Node`（class） | 188.4 MB | **48.9 MB** | −74% |
| `PathData$MapData$Node[]` | 53.6 MB | **10.8 MB** | −80% |
| 存活 `Node` | 464 万 / 222.7 MB | **95.2 万 / 45.7 MB** | **−79%** |
| 存活 `Node[]` | 29.4 MB | 5.6 MB | −81% |
| 存活 `SearchWorkspace` | 8 | 9 | 个位数 |
| CPU 样本（A* 占比） | 288 / 964 | 94 / 524 | 负载不同，结构不变 |

**验证**：`mvn -Dtest=PathDataTest,PathServiceCompressionTest,PathServiceConcurrencyTest,SpatialPathfinderTest,NpcMoveControllerPathTest test` → **128 例全绿**（`PathDataTest.reusesThreadLocalAStarWorkspace` 改为 `reusesPooledAStarWorkspace`：断言工作区归还共享池且两次搜索复用同一实例）；用户真机确认水中场景正常。

**待观察**：`OpenNode`/`SearchNode` 的绝对分配量在 `play-9` 上升（6.4→10.8 MB、6.9→9.2 MB），当前无法区分是负载差异还是槽位复用带来的搜索次数变化，需下一轮归一化确认。

**新的最大分配站点（quest 侧）**：`PlayerQuestEventPort.completedQuestIdsOf` 17.1 MB + `QuestSnapshot.validatedCompletedQuestIds` 14.0 MB + `QuestSnapshot.validatedInventory` 8.4 MB ≈ **40 MB**，属于任务引擎快照复制，需与 quest 侧的并行改动一并评估。

### 12.4 第九轮：quest 快照容器、路由 stream 与三个小项（`play-10`）

本轮同时修掉 quest 侧三个容器站点与三个小项（7 个文件，`mvn -Dtest=PlayerQuestEventPortTest,... test` 105 例全绿）：

| 改动 | 文件 |
|---|---|
| 任务 ID 集合单次构建（`questIdsOf`/`questIdSet`，取代 `completedQuestIdsOf`+`activeQuestIdsOf`+`Set.copyOf` 双哈希表） | `PlayerQuestEventPort` |
| 容器校验改为遍历入参（去掉 `MapN.keySet()` 视图/迭代器/`KeyValueHolder`） | `QuestSnapshot` |
| 路由 `routesFor(event, questId)`、`hasMatchingRoutes` 改普通循环（去掉 stream 管道与中间列表） | `QuestEventIndex` / `QuestProductionDispatcher` |
| `Monthes.values()` 缓存、实例广播数组快照按变更失效、`ForEach` 叶子分片 | `GameTime` / `WorldMapInstance` / `ForEach` |

#### 口径修正：采样重量不可跨轮直接比较

`play-9` 录到 587 个 `jdk.ObjectAllocationSample`，`play-10` 录到 2191 个，其中 **1772 个来自 `RMI TCP Connection`**（JMX 监视线程的小额样本）。样本数/权重的分布不同，导致同一站点在采样口径下的绝对字节不可直接对比；本轮的「目标站点归零」是**结构性事实**（方法已不存在），但「省了多少 MB」必须用非采样口径核对：

`jdk.ThreadAllocationStatistics`（每线程 `allocated` 累计值，取窗口首末差值）给出 300s 内精确分配量：

| 线程组 | `play-9` | `play-10` | 变化 |
|---|---|---|---|
| **`pool-4-thread-*`（游戏工作池：quest 串行执行器、调度任务、广播）** | 174.1 MB | **151.5 MB** | **−22.6 MB（−13%）** |
| `LongRunningPool-*`（AI/长时间任务） | 456.4 MB | 459.5 MB | +3.1 MB |
| `PacketProcessor:*`（客户端包处理，工作量对照） | 46.5 MB | 47.0 MB | +0.5 MB |
| `ForkJoinPool-*-worker-*` | 37.9 MB | 21.4 MB | −16.5 MB |
| `pathfinder` | 30.4 MB | 18.0 MB | −12.4 MB |
| `static-data-loader`（**录制窗口内仍在加载静态数据**） | 415.4 MB | 449.8 MB | +34.4 MB |
| 进程内全部线程合计 | 1180.7 MB | 1166.4 MB | −14.3 MB（−1.2%） |

结论：**quest 工作池的精确分配量下降 22.6 MB（−13%），而客户端包处理量与 AI 负载基本不变**（46.5→47.0、456.4→459.5），因此该下降不是负载变轻造成的。进程总量只降 1.2%，是因为录制窗口内 `static-data-loader` 还在加载静态数据（415~450 MB，占总量约 1/3，且两轮都如此）——**下一轮应在静态数据加载完成后再开始录制**，否则总量口径被启动负载污染。

#### 采样口径的目标站点（首帧+类，同 30 层栈深度）

| 站点 | `play-9` | `play-10` | 说明 |
|---|---|---|---|
| `GameTime$Monthes.values()` | 9.05 MB | **0** | 缓存枚举数组 |
| `completedQuestIdsOf` + `activeQuestIdsOf` + `validatedCompletedQuestIds` | 32.00 MB | **0** | 方法已移除 |
| 替换路径 `questIdsOf` + `questIdSet` | — | 12.51 MB | 单次遍历 + 一次 `Set.of` |
| `validatedInventory`（含 `MapN$1`/`KeyValueHolder`/`MapNIterator`） | 11.80 MB | 6.55 MB | 校验不再遍历副本 |
| `routesFor` + `hasMatchingRoutes`（stream 管道） | 14.51 MB | **0** | 改普通循环 |
| `WorldMapInstance.worldMapObjectsArray()` | 6.74 MB | **0.30 MB** | 按变更失效的缓存数组 |
| `ForEach` 任务实例 | 1.70 MB | **0** | 叶子 8 元素分片 |

#### 下一轮候选（`play-10` 暴露的 quest 侧新热点）

容器站点解决后，quest 侧剩下的是**每次（事件 × 路由）都全量捕获的事实**与**快照记录重建链**：

| 站点 | `play-10` | 性质 |
|---|---|---|
| `craftFactsOf`（含 `PlayerSkillList.getAllSkills` 9.96） | 16.23 MB | 每个快照遍历全部技能 |
| `equipmentFactsOf`（含 `ItemSetData.getItemSetTemplateByItemId` 4.18） | 9.66 MB | 每个快照遍历装备与套装 |
| `toInventoryMap`（含 `inventoryOf`） | 8.43 MB | 背包聚合 HashMap + `Map.copyOf` |
| `questIdSet` + `questIdsOf` | 12.51 MB | `Integer` 装箱 + `Set.of` 表 + 两个 `int` 缓冲 |
| `QuestSnapshot.withXxx` 链（`withEquipmentFacts` 2.90、`withMaxDp` 1.50、`withStartingClass`/`withGender`/`withTeamFacts` 各 0.82、`withWorldFacts` 0.55） | ≈7.7 MB | 每快照重建约 10 个 record |
| `QuestCraftSnapshot.<init>` | 4.07 MB | 技能/配方表投影 |

优先级建议：**A. 版本化缓存**（按玩家给这四类事实加版本号，未变化时复用上一次不可变快照，注意登出清理与线程安全）＞ **C. 按需捕获**（条件集合决定是否需要 craft/equipment/membership）＞ 记录重建链合并构造。

### 12.5 第十轮：按需捕获 + 单次构造 + 漏网门面缓存（`play-11`）

第十轮落地了三件事（代码 + 测试一次提交）：

1. **Spring 门面补缓存**：`GameEngineServices.instanceEngine()/ai2Engine()/chatProcessor()` 之前每次调用都走 `provider.getIfAvailable(...)`，按 `questEngine()` 的既有模式补 `resolvedX` 缓存（只在解析到真实 bean 时缓存，`destroy()` 清空）。
2. **事实按需捕获（新 `QuestFactRequirements`）**：由 transition 的 `conditions()`+`actions()` 静态推导本转换真正读取的事实族，`QuestExecutionCoordinator` 推导一次后交给 `QuestEventPort.snapshot(..., requirements)`；`QuestEventPort` 新增的默认方法回落旧门控，自定义端口/测试替身零改动。未采集的事实族保持未捕获，读取方 fail-closed。`QuestCondition`(33)/`QuestAction`(17) 都是 sealed，推导用**无 `default` 的穷尽 switch**，新增类型会直接编译失败——这是「不可能静默漏采」的编译期保证。
3. **快照单次构造**：删掉 `snapshotOf` + 8 连 `withXxx`，所有事实先算好再一次构造 record。

触发条件映射（全部读取点经 grep 审计）：`craftFacts` ← `RecipeKnown`/`CanGrantCraftSkill`；`equipmentFacts` ← `EquipmentSetEquipped`/`EquippedItem`/`UnequipItem`；`inventory` ← `HasItem`/`RemoveItem`/`GiveItem` **外加完成与放弃**（`QuestMutationPlanner` 会为 `questWorkItems` 追加 `RemoveItem(ALL)`，`PlayerQuestInventoryPort.preflight` 仍读 `itemCount`）；`questIdSets` ← `QuestsFinished`/`UnfinishedQuest`/`AcquiredQuest`/`NoAcquiredQuest`；货币、DP、队伍、会员、位置不门控（始终采集）。

#### 精确口径（`ThreadAllocationStatistics`，300s 窗口）

| 线程组 | `play-9` | `play-10` | `play-11` | Δ（11−10） |
|---|---|---|---|---|
| **`pool-*-thread-*`（含 quest 串行执行器 pool-4）** | 175.1 MB | 152.5 MB | **133.2 MB** | **−19.3 MB** |
| `LongRunningPool-*`（AI/长任务） | 456.4 MB | 459.5 MB | 408.5 MB | −51.0 MB |
| `static-data-loader`（**并行 XML 加载池，见下**） | 415.4 MB | 449.8 MB | 411.5 MB | −38.3 MB |
| `ForkJoinPool-*-worker-*` | 37.9 MB | 21.4 MB | 47.9 MB | +26.5 MB |
| `pathfinder` | 30.4 MB | 18.0 MB | 47.3 MB | +29.3 MB |
| `PacketProcessor:*` | 46.5 MB | 47.0 MB | 16.0 MB | −31.0 MB |
| 进程合计 | 1180.7 MB | 1166.4 MB | **1084.6 MB** | −81.8 MB |

工作量提示：`NativeMethodSample` 14,082 → 14,139（总体活动量相当），但**构成不同**（包处理 −31 MB、寻路 +56 MB），所以本轮以「站点归零」为主证据、`pool-*` 的 −19.3 MB 为次证据。

只看 quest 串行执行器所在的 `pool-4`：**174.1 → 151.5 → 132.3 MB**（play-9/10/11），两轮 quest 侧优化合计 **−41.8 MB（−24%）**。

#### 采样口径：目标站点（play-10 → play-11）

| 站点 | `play-10` | `play-11` | 说明 |
|---|---|---|---|
| `craftFactsOf` + `QuestCraftSnapshot.<init>` + `PlayerSkillList.getAllSkills` | 16.23 + 4.07 + 9.96 MB | **0 / 0 / 0** | 只有制作条件才采 |
| `equipmentFactsOf` + `ItemSetData.getItemSetTemplateByItemId` | 9.66 + 4.18 MB | 0.32 / 0.32 MB | 只有装备条件/卸下动作才采 |
| `toInventoryMap` + `QuestSnapshot.validatedInventory` | 8.16 + 4.78 MB | 0.63 / **0** | 按需采集 |
| `QuestSnapshot.withXxx` 链 + `QuestSnapshot.<init>` | 8.50 + 5.05 MB | **0 / 0** | 单次构造生效 |
| `questIdsOf` + `questIdSet` | 12.51 MB | **0**（替换为 `QuestFactRequirements` 6.60 + `freeze` 6.23） | 只被四个完成/进行中条件读时才采 |
| `GameEngineServices.ai2Engine()` | 6.57 MB | **0** | 门面缓存 |
| `QuestProductionDispatcher.dispatch`（栈深 30 截断，仅供参考） | 64.21 MB | **8.83 MB** | 同一路径采样分配 −86% |

**测试**：`mvn -Dtest=QuestFactRequirementsTest,PlayerQuestEventPortTest,QuestExecutionCoordinatorTest,QuestProductionDispatcherTest,QuestMutationPlannerTest,QuestRuntimeInfrastructureTest,QuestSnapshotValidationTest,QuestSnapshotCurrencyCaptureTest,QuestEventConditionTest,QuestPlayerFactsConditionTest,GameServiceProviderCompatibilityTest,GameRuntimeServiceBridgeTest test` → **132 例全绿**（含新增 `QuestFactRequirementsTest` 8 例）。

#### 新发现：`static-data-loader` 不是一次性启动负担

`XmlDataLoader` 的这个线程名属于**常驻并行 XML 加载池**（`CPU + 5` 线程，`DataManager` 的并行物品/技能路径共用），不是启动期的一次性线程：`play-10`（启动后 ~1.3 分钟开始录）与 `play-11`（**启动后 ~8.5 分钟**开始录）在该池上的分配几乎相同（449.8 / 411.5 MB），占进程总量约 38%。因此「等静态数据加载完再录」并不能消除它——它是当前**最大的单线程组分配源**，值得单独立项（确认它到底在反复加载什么、能否按需/缓存/复用解析缓冲）。

#### 下一轮（第 4 步）候选，按证据排序

1. `static-data-loader` 池：411 MB/300s，先查清它在加载什么（`XmlDataLoader` / `DataManager` 并行路径 / 按地图懒加载）。
2. `GameEventServices.eventService()` 3.56 MB：与 1a 完全同类的漏网门面缓存（全仓 `getIfAvailable(` 有 604 处，应按 JFR 热点逐个补，而不是全量重构）。
3. `RetailPatternAI2` 条件评估（`usesNpcParty` 4.67 / `supports` / `hasCompleteMasterData` / `supportsMoveType` / `hasWorldSceneConsumer`）与 `CreatureGameStats.getStatsByStatEnum`（2.85 + 每 stat 一个 `TreeMap$Entry`）、`KnownList.knownObjectsSnapshot`（6.05，长列表拷贝）。
4. 之后才是 quest 侧第 3 步（版本化缓存）：当前 quest 侧剩余成本已明显下降，需要重新测量后再决定是否值得引入高风险的失效点清单。

### 12.6 勘误：`ThreadAllocationStatistics` 必须按 `javaThreadId` 聚合（play-11 复算）

12.4/12.5 里「`static-data-loader` 415~450 MB」「`LongRunningPool` 456 MB」「进程合计 1.1 GB」的结论**作废**——那是聚合 bug，不是真实分配。

根因：`jdk.ThreadAllocationStatistics` 的 `thread` 字段同时带名字和 `javaThreadId`。若按**线程名**做 key，同名池线程会被合并成一条时间序列（`static-data-loader` 有 `CPU+5`=15 个线程、`LongRunningPool-*` 有 10 个以上），`max-min` 于是变成「两个不同线程各自累计值之差」，凭空造出几百 MB。事件本身带 `javaThreadId`，必须用它做 key 再按线程名归组。

修正后（300s 窗口，按 `javaThreadId` 聚合）：

| 线程组 | `play-9` | `play-10` | `play-11` |
|---|---|---|---|
| **`pool-*-thread-*`（含 pool-4）** | 175.1 MB | 152.5 MB | **133.2 MB** |
| `pathfinder`（A* 线程） | 97.0 MB | 39.8 MB | 138.5 MB |
| `ForkJoinPool-*-worker-*` | 37.9 MB | 21.4 MB | 47.9 MB |
| `PacketProcessor:*` | 25.7 MB | 32.0 MB | 12.5 MB |
| `multiThreadIoEventLoopGroup-*-*` | 10.1 MB | 9.4 MB | 11.6 MB |
| `RMI TCP Connection(*)` | 7.2 MB | 7.2 MB | 7.0 MB |
| **合计（有增量的线程）** | **354.8 MB** | **263.9 MB** | **352.3 MB** |
| `static-data-loader`（15 线程，空闲） | 0.0 MB | 0.0 MB | 0.0 MB |
| `LongRunningPool-*`（10+ 线程） | 0.0 MB | 0.0 MB | 0.0 MB |

结论修正：

- **仍然成立**：`pool-4` 精确分配 174.1 → 151.5 → **132.3 MB（两轮 quest 优化 −24%）**；12.5 的站点级归零是采样口径，与本次聚合 bug 无关，继续有效。
- **不成立**：进程总量「1.1 GB」「−1.2% / −7%」；「`static-data-loader` 是常驻加载负担」；`LongRunningPool` 456 MB。`static-data-loader` 与 `LongRunningPool` 在窗口内**没有可测量的分配**（线程空闲，管理线程名下的 0 MB）。
- `play-11` 总量高于 `play-10` 的原因是 `pathfinder` 39.8 → 138.5 MB（本轮寻路量大增），不是回归。

因此第 4 步的优先级修正为：

1. **`pathfinder`**（97.0/39.8/138.5 MB，随寻路量线性变化）与 **`pool-4`**（133.2 MB）两条真实大头；`static-data-loader` 立项取消。
2. 采样口径已定位、待处理：`RetailPatternAI2` 条件评估（`usesNpcParty`/`supports`/`hasCompleteMasterData`/`supportsMoveType`/`hasWorldSceneConsumer`）、`CreatureGameStats.getStatsByStatEnum`、`KnownList.knownObjectsSnapshot`、`GameEventServices.eventService()`（同类漏网门面缓存）。

**口径纪律**：此后所有 per-thread 统计一律按 `javaThreadId` 聚合，再按名字分组展示。

### 12.7 第十一轮：事件门面缓存与 AI 模式校验零分配化（待 `play-12` 复测）

第 4 步的前两项（`static-data-loader` 立项已按 12.6 取消）：

1. **`GameEventServices` 六个解析器补缓存**：`eventService()` / `playerEventService()` / `crazyDaevaService()` /
   `abyssRankUpdateService()` / `packetBroadcaster()` / `eventScheduler()` 原先是每次调用都走
   `provider.getIfAvailable(...)`；现在按 `GameEngineServices` 的既有模式补 `resolvedX`（只在解析到真实 Spring
   bean 后缓存，回退实例不钉住，`destroy()` 清空）。依据：`play-11` 采样里 `GameEventServices.eventService()`
   3.56 MB。
2. **`RetailPatternAI2` 模式校验零分配化**：这条路径在**每次 NPC 生成**时都会跑（
   `AI2Engine.selectNpcAi → RetailPatternAI2.supports(pattern, npc)`），`play-10` 采样子树 **21.97 MB**：
   `usesNpcParty` 6.38、`hasCompleteMasterData` 5.06、`hasWorldSceneConsumer` 3.24、`supportsMoveType` 0.27，
   分配物是 `ReferencePipeline$Head/$7`、`MatchOps$MatchOp`、lambda、迭代器与 `Object[]`。改动：
   - 常量提升：`SUPPORTED_RULE_CATEGORIES`（原来内联在**每个 rule** 的判断里）、`SUPPORTED_MOVE_TYPES`、
     `WORLD_SCENE_WORLDS`；
   - 四处 `values().stream().flatMap(List::stream).flatMap(rule -> rule.actions().stream()).anyMatch/noneMatch(...)`
     改成嵌套循环（新增零分配的 `hasActionType(pattern, type)`）：`usesNpcParty`、`hasWorldSceneConsumer`、
     `hasCompleteMasterData`、`on_arrived_at_point` 校验。语义等价（均为纯谓词，短路顺序变化不影响结果）。

**测试**：`mvn -Dtest=RetailPatternAI2Test,AI2EngineRetailSelectionTest,RetailAiDefinitionLoaderTest,RetailNpcPartyEngineTest,RetailConditionSpawnEngineTest,CreatureEventHandlerTest,GetMostPlayerDamageNullGateTest,GameServiceProviderCompatibilityTest,GameRuntimeServiceBridgeTest test` → **113 例全绿**（`RetailPatternAI2Test` 79 例覆盖模式校验正反例）。

**有意未做**：同文件还有约 40 处内联 `Set.of(...)`。按 `play-10` 的构成拆分，3+ 元素 `Set.of` 产生的 `Object[]` 只占该子树 21.97 MB 中的 1.92 MB，其余多是 2 元素 `Set.of("TRUE","FALSE")`（单个 `List12`，约 32 B/次）；机械替换 30+ 处字面量收益有限而 typo 风险偏高，等 `play-12` 复测确认剩余量再决定。

3. **`CreatureGameStats.getStatsByStatEnum` 去掉整表复制**：原实现每次调用都 `new TreeSet` 复制该 stat 的全部函数，
   而它被 `getStat(...)` 在**每个 SM_STATS_INFO 属性包**路径上调用（`play-11` 栈：
   `getStatsByStatEnum ← getStat ← PlayerGameStats.getKnowledge ← SM_STATS_INFO.writeImpl`）。现在没有 SET 类型函数
   （priority ≥ MAX-10，绝大多数情况）时直接返回底层 `TreeSet` 视图，有 SET 函数时行为不变。返回类型
   `TreeSet<IStatFunction>` → `Set<IStatFunction>`（4 个调用点只做遍历与 `isEmpty()`）。锁前提已核实：`stats` 的
   所有写入（构造、`addEffectOnly`、`endEffect`）都在写锁内，3 个内部调用点都在读锁内；唯一无锁调用方
   `commands/admin/Stat.java` 改为自行复制并在注释里声明视图的锁约束。

**撤销项：`KnownList.knownObjectsSnapshot`**。原本准备让 `clear()/forgetObjects()/doOnAllNpcs*()` 直接迭代
`knownObjects.values()`（ConcurrentHashMap 弱一致迭代）以省掉每次 despawn 的 `ArrayList`；但仓库存在架构守卫测试
`KnownListIterationSafetyTest`，它正则扫描 `src/main/java`，**禁止** `for (... : knownObjects|knownPlayers|visualObjects|
visualPlayers.values())` 这种就地迭代，只允许先 `new ArrayList<>(...)` 快照。这是刻意约定（循环体会 `del()` 改动已知
关系，弱一致迭代会漏访/重复），因此该项撤销、文件还原；该站点在 `play-11` 实际只有 0.91 MB/300s（此前 6.05 MB 是
「任意帧包含」的放大统计），不值得为它破坏既有不变量。

**测试**：`mvn -Dtest=CreatureGameStatsBytecodeTest,NpcGameStatsTest,StatFunctionsTest,AbsoluteStatEffectTest,PetrificationEffectTest,AbsoluteSnareEffectTest,WeaponDualEffectTest,KnownListTest,KnownListIterationSafetyTest,TargetRangePropertyTest test` → **46 例全绿**（含 `KnownListIterationSafetyTest` 5 例，确认还原干净）。

**状态**：本轮改动尚未复测，`play-12` 数据出来后回填本节与 12.6 的对照表。

### 12.8 第十/十一轮复测（`play-12`）与第十二轮改动（待授权测试）

**采样**：`/tmp/play-12.jfr`，2026-09-16 21:46:58–21:51:58（300 s），进程 PID 9346 启动于 21:39:52，
即包含 `5a15ee3ed`（AI 模式校验零分配化 + 事件门面缓存）与 `700b8e0ab`（`getStatsByStatEnum` 去整表复制）。
解析：`/tmp/play-12-alloc.txt`（336 个 `ObjectAllocationSample`，总权重 135.7 MB）、`/tmp/play-12-cpu.txt`。

#### 12.8.1 线程聚合（`ThreadAllocationStatistics`，按 `javaThreadId`，max−min，MB）

| 线程组 | play-10 | play-11 | **play-12** |
|---|---|---|---|
| `pool-*-thread-*` | 152.7 | 133.5 | **75.0** |
| `pathfinder` | 39.8 | 138.5 | **28.5** |
| `ForkJoinPool-*-worker-*` | 21.4 | 48.0 | **20.2** |
| `PacketProcessor:*` | 32.0 | 12.5 | **4.3** |
| `multiThreadIoEventLoopGroup-*` | 9.4 | 11.6 | **7.2** |
| `RMI TCP Connection(*)` | 7.2 | 7.0 | **7.1** |
| **合计（有增量的线程）** | **264.0** | **352.6** | **143.8** |
| 其中 `pool-4` 单组 | 151.7 | 132.6 | **74.0** |

`pool-4` 轨迹：`play-9` 174.1 → `play-10` 151.5 → `play-11` 132.3 → **`play-12` 74.0 MB（相对 play-11 −44%）**，
与 12.4/12.5 的 quest 快照改动方向一致。`pathfinder`/`pool-4` 的波动仍随现场寻路量变化，非受控 A/B，
只作趋势参考。

#### 12.8.2 站点级验证（`ObjectAllocationSample`，任意帧匹配，300 s）

| 站点 | play-10 | play-11 | **play-12** |
|---|---|---|---|
| `RetailPatternAI2`（整类） | 30.86 MB (90) | 47.52 MB (186) | **9.31 MB (24)** |
| ↳ `supports`/`usesNpcParty`/`hasCompleteMasterData`/`hasWorldSceneConsumer`/`supportsMoveType` | 21.97 MB | — | **0.000 MB（0 样本）** |
| `GameEventServices`（整类） | 2.17 MB (5) | 4.55 MB (14) | **0.000 MB（0 样本）** |
| `getStatsByStatEnum` | 3.27 MB (3) | 2.78 MB (4) | **0.000 MB（0 样本）** |
| `java.util.TreeSet` | 3.27 MB (3) | 2.17 MB (3) | **0.000 MB（0 样本）** |

三项 12.7 改动全部确认落地：校验子树、事件门面、stat 整表复制在 `play-12` 采不到任何分配。
`RetailPatternAI2` 残余 9.31 MB 已不含校验路径，主要是 `runEvent` 的规则列表迭代（见 12.8.3）。

#### 12.8.3 `play-12` 新的 top 分配站点（总采样 135.7 MB）

| 排名 | 站点 | 权重 | 说明 |
|---|---|---|---|
| 1 | `Vector3f$1.create`（`Ray.intersects` 的 4 个临时向量） | 33.54 MB | 37 样本，全部来自 `SimpleAttackManager.isTargetInAttackRange → GeoService.canSee → GeoMap.canSee → Terrain.collide → Ray.intersectWhere` |
| 2 | `Terrain.collideNearXY`（每次探测 `new Vector3f()` 交点） | 11.73 MB | 同一调用链，逐网格步 ×3 次探测 |
| 3 | `BIHNode.intersectWhere`（`float[] origins`/`invDirections`） | 9.75 MB | 每次射线遍历两个 `float[3]` |
| 4 | `PathData$MapData$SearchWorkspace.node` | 6.77 MB | 寻路 A\* 节点槽位 |
| 5 | `AQS$ConditionObject.newConditionNode` | 6.65 MB | JDK `ScheduledThreadPoolExecutor` 等待队列，框架侧 |
| 6 | `ImmutableCollections$AbstractImmutableList.iterator` | 6.39 MB | `ListN.iterator()` 的 `ListItr`，其中 6.39 MB 含 `RetailPatternAI2.runEvent:1115` 的规则列表 for-each |
| 7 | `CreatureGameStats.getStat` | 5.73 MB | `AdditionStat` 3.93 MB（正常业务对象）+ `CalculationType[]` 1.80 MB（`getStat(StatEnum,int)` 的 `new CalculationType[0]`） |
| 8 | `PathService.findGroundPath` / `waypoints` / `waypointSkipIndex` | 8.34 MB | 寻路结果数组（正常业务分配） |
| 9 | `ForEach.compute` | 3.90 MB | 叶子分片后的 ForkJoin 任务对象（12.4 已从 3.4 MB/轮降到批量分片） |

子站点归因：**`GeoService.canSee` 子树 55.77 MB（41%），其中 `SimpleAttackManager.isTargetInAttackRange` 54.19 MB**
（`play-11` 同链路仅 10.10/9.30 MB）。也就是说，寻路分配降下来之后，**AI 攻击距离判定里的地形射线检测成为最大头**：
每个 NPC 的移动任务轮询都会做最多 `距离/2 × 3` 次网格探测，每次都 `new Vector3f()`，再在 `Ray.intersects` 里
`Vector3f.newInstance()` 出 4 个向量（该「对象池」池空即 `new`，等于没有池）。

#### 12.8.4 第十二轮改动（本提交，待授权测试）

1. **`Ray` 求交临时向量改用每线程 scratch**：`intersects(...)` 的 `tempVa..tempVd` 与 `distanceSquared(...)` 的
   两个临时向量原先走 `Vector3f.newInstance()/recycle()`，而 `ObjectFactory.object()` 只是 `create()`，
   池空时照常 `new`。改为 `IntersectionScratch` + `ThreadLocal`（私有、纯数学、无递归、不逃逸）。依据 33.54 MB。
2. **`Terrain` 碰撞缓冲每线程复用**：`collide`/`collideAtOrigin` 的 `p1or4/p2/p3` 与 `collideNearXY` 的交点缓冲
   移入 `CollisionScratch`；**仅当 `results == null`（可见性判定）时复用交点**，`results != null` 时仍每次
   `new Vector3f()`——`CollisionResult` 会长期持有该引用，复用会写坏既有结果集。依据 11.73 MB。
3. **`BIHNode.intersectWhere(Ray...)` 复用轴数组**：`origins`/`invDirections` 两个 `float[3]` 移入既有
   `RayScratch`（赋值顺序与 `normalizeLocal()` 的相对次序保持不变）。依据 9.75 MB。
4. **`RetailPatternAI2.runEvent` 规则列表改下标遍历**：`pattern.event(name)` 返回 `ImmutableCollections` 的
   `ListN`，for-each 每次事件分配一个 `ListItr`。依据 6.39 MB。
5. **`CreatureGameStats.getStat(StatEnum,int)` 共享空 varargs 数组**：`new CalculationType[0]` → 静态
   `NO_CALCULATION_TYPES`（空数组只读，callee 不可能写入）。依据 1.80 MB。

合计覆盖 `play-12` 采样总量中约 46% 的分配；`GeoService.canSee` 链路预计仅剩 `GeoMap.canSee` 自身的
`pos/dir/Ray/CollisionResults`（约 4 MB）与 `BIHNode` 的命中对象。

**测试**：`mvn -Dtest=TerrainTest,BIHTreeRayTraversalTest,BoundingBoxRayIntersectionTest,GeoMapWalkerCollisionTest,GeoWorldLoaderAionServerFormatTest,GeoServiceGroundSearchTest,GeoServiceSkillObstacleTest,RealGeoDataConcurrencyTest,RealGeoDataLookupTest,RetailPatternAI2Test,CreatureGameStatsBytecodeTest,StatFunctionsTest test`
→ **BUILD SUCCESS，12 个测试类 138 例全绿**：`RetailPatternAI2Test` 79 例（规则匹配与模式校验正反例）、
`StatFunctionsTest` 18 例、`GeoWorldLoaderAionServerFormatTest` 18 例、`BIHTreeRayTraversalTest` 5 例
（BIH 射线遍历的距离/仅首命中/limit/未命中，覆盖第 3 项）、`BoundingBoxRayIntersectionTest` 3 例、
`GeoMapWalkerCollisionTest` 2 例、`GeoServiceGroundSearchTest` 2 例、`TerrainTest` / `GeoServiceSkillObstacleTest` /
`CreatureGameStatsBytecodeTest` 各 1 例。注意：`RealGeoDataConcurrencyTest` 是**源码闸门**（校验 geo 装载走生命周期池、
不在 worker 线程里改地图注册表），**不是**射线并发用例，不能当作线程本地 scratch 的并发证据。

**测试盲区与补测**：`Terrain.collide` 此前**没有任何测试覆盖**（`TerrainTest` 只测 `pathHeight`，
`GeoMapWalkerCollisionTest` 走的是 `collideWith` 而非 `terrain.collide`），而第 2 项正好改的就是它。因此新增
`TerrainRayCollisionScratchTest`（4 例）：单线程交替 null/results 路径 1000 次结果一致；`results != null` 收集到的
交点在地面探测 1000 次后仍未被改写（守住「收集结果时必须新建交点」这条语义）；未命中/超 limit 不写入结果集；
8 线程 × 500 轮并发探测与单线程基线一致。

**补测结果**：`mvn -Dtest=TerrainRayCollisionScratchTest,TerrainTest,BIHTreeRayTraversalTest,GeoMapWalkerCollisionTest,RealGeoDataLookupTest test`
→ **BUILD SUCCESS，5 个测试类 16 例全绿**（新增守护测试 4 例）。该次运行的 `default-compile` 输出
`Nothing to compile - all classes are up to date`，即 `target/classes` **未被写入**；服务器（PID 16290）的类路径首项正是
`AionEmu-test/target/classes`，故本次测试对运行中的进程零干扰。

**状态**：代码已改、静态检查与聚焦测试（138 + 16 例）均通过；`play-13` 复测待重启后进行。

### 12.9 第十三轮复测（`play-13`）：AI 视线射线链路分配归零

**采样**：`/tmp/play-13.jfr`，2026-09-16 22:18:54–22:23:54（300 s），进程 PID 20730（22:17:30 启动，
含提交 `0b480cb1b`）。解析：`/tmp/play-13-alloc.txt`（`ObjectAllocationSample` 45 个，总权重 21.5 MB）、
`/tmp/play-13-cpu.txt`（`ExecutionSample` 382 个）、`/tmp/play-13-histogram.txt`。

#### 12.9.1 线程聚合（按 `javaThreadId`，max−min，MB）

| 线程组 | play-10 | play-11 | play-12 | **play-13** |
|---|---|---|---|---|
| `pool-*-thread-*` | 152.7 | 133.5 | 75.0 | **43.7** |
| `pathfinder` | 39.8 | 138.5 | 28.5 | **20.5** |
| `ForkJoinPool-*-worker-*` | 21.4 | 48.0 | 20.2 | **25.3** |
| `PacketProcessor:*` | 32.0 | 12.5 | 4.3 | **2.5** |
| `multiThreadIoEventLoopGroup-*` | 9.4 | 11.6 | 7.2 | **7.9** |
| `RMI TCP Connection(*)` | 7.2 | 7.0 | 7.1 | **5.9** |
| **合计（有增量的线程）** | **264.0** | **352.6** | **143.8** | **107.7** |
| 其中 `pool-4` 单组 | 151.7 | 132.6 | 74.0 | **42.8** |

`pool-4` 轨迹：174.1（play-9）→ 151.5 → 132.3 → 74.0 → **42.8 MB**（相对 play-12 再 −42%）。
`ForkJoinPool-*-worker-*` 由 20.2 → 25.3 MB 是**未归因**项：本轮采样在该线程组没有任何样本落点
（见 12.9.3），需要下一轮专门看。

#### 12.9.2 站点验证：目标站点全归零（`ObjectAllocationSample`，任意帧匹配）

| 站点 | play-12 | **play-13** |
|---|---|---|
| `GeoService.canSee` 子树 | 55.767 MB (55) | **0.000 MB（0 样本）** |
| ↳ `SimpleAttackManager.isTargetInAttackRange` | 54.193 MB (52) | **0.000 MB（0 样本）** |
| ↳ `Ray.intersect*` / `Vector3f$1.create` | 33.542 MB (37) | **0.000 MB（0 样本）** |
| ↳ `Terrain.collide*` | 45.647 MB (45) | **0.000 MB（0 样本）** |
| ↳ `BIHNode.intersectWhere` | 9.747 MB (11) | **0.000 MB（0 样本）** |
| `AbstractImmutableList.iterator`（`ListItr`） | 6.393 MB (16) | **0.246 MB（1）** |
| `RetailPatternAI2`（整类） | 9.311 MB (24) | **3.464 MB（12）** |
| `PathData$MapData$SearchWorkspace` | 9.764 MB (14) | **0.000 MB（0）** |
| `questEngine` | 1.574 MB (3) | **1.473 MB（3）** |
| **采样合计** | **135.7 MB (336)** | **21.5 MB（45）** |

**功能未被绕过的证据（CPU 剖面，同一链路仍在跑）**：

| `ExecutionSample` 标记 | play-12 | **play-13** |
|---|---|---|
| `GeoService.canSee` | 88 | **78** |
| `GeoMap.canSee` | 41 | **39** |
| `Terrain.collide` | 6 | **5** |
| `Ray.intersects` | 1 | **4** |
| `SimpleAttackManager.isTargetInAttackRange` | 38 | **35** |
| `RetailPatternAI2.runEvent` | 19 | **34** |
| `PathService.findGroundPath` | 72 | **78** |

射线链路 CPU 命中数与 play-12 持平（78 vs 88），但分配从 55.77 MB 直接归零 —— 即「同一条路径、同样的调用量，
只是不再产生临时向量」，与 12.8.4 的预期一致，也说明 12.9 的低采样量不是「这段逻辑没跑」。

#### 12.9.3 `play-13` 剩余 top 站点（45 样本 / 21.5 MB）

| 权重 | 站点 | 说明 |
|---|---|---|
| 4.245 MB | `HashMap.newNode` | `HashMap$Node` |
| 3.500 MB | Netty `newPromise` | `DefaultChannelPromise`（网络层） |
| 1.473 MB | `ConcurrentHashMap.putVal` | |
| 1.228 MB | `CreatureGameStats.getStat` | `AdditionStat`，正常业务对象 |
| 1.227 MB | `Long.valueOf` | |
| 0.983 MB | `Object.clone`（`MemberName`） | 方法/反射元数据 |
| 0.736 MB | `RetailPatternAI2.<init>` | 每个 AI 实例一个 `ConcurrentHashMap` |
| 0.736 MB | `CHM$CollectionView.toArray` | |
| 0.736 MB | `Creature.<init>` / 0.491 `Npc.setupStatContainers` | NPC 生成固有成本 |
| 0.491 MB | `Integer.valueOf` / `CHM.newKeySet` / `ScheduledFutureTask` / `CHM.transfer` | |

已经没有任何「每次判定都 new 一个几何对象」的站点；剩下的都是容器扩容、网络 Promise、实体构造这类固有成本。

#### 12.9.4 结论与下一轮候选

1. **第十二轮 5 项改动全部确认生效**：目标站点 0 样本，采样总量 135.7 → 21.5 MB（−84%），线程总量
   143.8 → 107.7 MB（−25%），`pool-4` 74.0 → 42.8 MB。CPU 证据显示链路未被绕过。
2. 剩余真实大头：`pool-4`（42.8 MB）、`ForkJoinPool-*-worker-*`（25.3 MB，本轮**无采样落点**，需专门看）、
   `pathfinder`（20.5 MB）。
3. **口径更正（用户确认）**：`play-13` 的窗口本身**就是**「持续战斗 + 大量 NPC 移动」的目标场面，
   所以 12.9.2 的归零不是「这段逻辑没跑」，而是**同场景**下把 55.77 MB 的临时几何对象全部省掉了；
   12.9.1 的 143.8 → 107.7 MB 也是在同一级负载下取得的（`ExecutionSample` 382 个，FJP 组满载）。
4. 下一轮采样建议：改用细粒度分配事件定位剩余项（见 12.9.6），不再用 throttled 的
   `ObjectAllocationSample` 做单线程组归因。

#### 12.9.5 `play-13` 剩余分配逐条归因（45 个样本的栈判读）

| 权重 | 栈摘要（分配点 → 根因） | 归属 |
|---|---|---|
| 4.000 MB | `HashMap$Node ← putVal ← add ← getClassDataLayout0` | JMX/RMI 监控侧，**非游戏逻辑** |
| 3.500 MB | `DefaultChannelPromise ← write ← flushWrites` | Netty 出站包 |
| 1.227 MB | `Long.valueOf ← currenciesOf ← freeze ← snapshot ← executeSerialized`（2 处） | quest 快照货币装箱 |
| 0.983 MB | `MemberName.clone ← resolve ← findConstructor ← newInstanceWithCaller` | AI 实例化反射 |
| 0.981 MB | `Object[] ← toArray ← knownObjectsSnapshot ← doOnAllNpcsWithOwner` | KnownList 快照（既有不变量） |
| 0.736 MB | `TransformModel <init> ← spawnBaseNpc` | NPC 生成 |
| 0.736 MB | `AdditionStat ← getStat ← adjustDamages ← splitPhysicalDamage ← calculateAutoAttackPhysical` | 伤害计算（战斗固有） |
| 0.982 MB | `CHM$Node/transfer ← putVal ← addVisualObject ← findVisibleObjects ← doUpdate`（4 处） | 可见性/knownlist 更新 |
| 0.491 MB | `Integer.valueOf ← getNpcShouts ← hasAnyShout ← canHandleEvent ← onCreatureEvent` | NPC 喊话 |
| 1.227 MB | `CHM.newKeySet/<init> ← newInstance ← setupAI`（4 处） | 每个 AI 实例的 CHM 集合 |
| 0.491 MB | `ScheduledFutureTask ← schedule ← broadcastMessage ← executeActions` | AI 定时广播 |
| 0.273 MB | `VisibleObject[] ← prepareArray ← toArray ← worldMapObjectsArray` | 世界对象数组（12.4 已改失效缓存） |
| 0.736 MB | `MatchOps$MatchOp/$$Lambda ← allMatch ← supports ← selectNpcAi` | **12.7 有意留下的 stream 残留** |
| 0.492 MB | `MapN$1 entrySet / ListItr ← hasActionType/hasCompleteMasterData ← supports` | 同上（内联 `Set.of`/List 迭代） |
| 0.246 MB | `RandomAccessSpliterator ← stream ← hasQuestTemplates` | quest 模板判定 |
| 0.245 MB | `HashMap$KeyIterator ← spawn ← onHourChange ← checkDayTimeChange` | 昼夜刷新 |
| 0.245 MB | `Vector3f ← getZ ← getClosestCollision ← run` | 寻路结果取高度 |
| 0.245 MB | `ArrayList$Itr ← getMappedAbnormals ← clearEffect ← endEffect` | 异常状态清理 |
| 0.245 MB | `Object[]/setupStatContainers ← spawnNpc ← spawnObject ← spawn` | NPC 生成 |

已无「每次判定都 new 几何对象」的站点；剩下的要么是实体/战斗固有成本，要么是 12.7 刻意保留的那一处。

#### 12.9.6 FJP 25.3 MB 的定位与下一轮细粒度方案

- FJP 组 8 个 worker 各增 1.5–5.5 MB（合计 25.3 MB），但本轮 `ObjectAllocationSample` **没有一个样本落在该组**——
  throttled 采样在「低速率、小对象」线程上有空洞。方向只能由 CPU 给：99 个 FJP 样本落在
  `collideWith` 90 / `canSee` 66 / `searchAStar` 50 / `moveToDestination` 46 / `canReachWaypoint*` 56 /
  `prepareGroundPath` 27 / `poll` 27 / `isTargetInAttackRange` 23 / `getZ` 19，即**寻路准备 + GEO 碰撞 + AI 轮询**；
  对应分配应是路径结果数组、`CollisionResults`/`Ray`/`pos`/`dir` 与 `knownObjectsSnapshot` 的 `ArrayList`/`Object[]`。
- 细粒度方案（`play-14` 待授权）：自定义 JFR 配置开启 `jdk.ObjectAllocationInNewTLAB` +
  `jdk.ObjectAllocationOutsideTLAB`（带栈）。当前服务器总分配仅 ~0.36 MB/s（107.7 MB/300 s），TLAB 事件代价可忽略，
  但能精确给出每个分配栈，替代 throttled 采样的空洞。配置已备：`/tmp/alloc-detail.jfc`。

### 12.10 第十四轮复测（`play-14`，重负载）：细粒度分配归因与下一轮候选

**采样**：`/tmp/play-14.jfr`，2026-09-16 22:54–22:59（300 s），PID 26773（代码 = `0b480cb1b`），配置
`/tmp/alloc-detail.jfc`（`ObjectAllocationInNewTLAB` + `OutsideTLAB` 带栈）。用户确认窗口是**持续战斗 +
大量 NPC 移动**。产物：`play-14-tlab.txt`（875）、`play-14-outside.txt`（82）、`play-14-alloc.txt`（399 样本，
总权重 182.6 MB）、`play-14-cpu.txt`（343）、`play-14-histogram.txt`。

**本轮是重负载窗口**，用于验证上一轮结论在更大压力下依然成立：

| 线程组 | play-11 | play-12 | play-13 | **play-14** |
|---|---|---|---|---|
| `pathfinder` | 138.5 | 28.5 | 20.5 | **67.5** |
| `pool-*-thread-*` | 133.5 | 75.0 | 43.7 | **75.0** |
| `ForkJoinPool-*-worker-*` | 48.0 | 20.2 | 25.3 | **14.5** |
| `PacketProcessor:*` | 12.5 | 4.3 | 2.5 | **9.9** |
| `multiThreadIoEventLoopGroup-*` | 11.6 | 7.2 | 7.9 | **9.8** |
| `RMI TCP Connection(*)` | 7.0 | 7.1 | 5.9 | **7.0** |
| **合计** | 352.6 | 143.8 | 107.7 | **185.4** |
| 其中 `pool-4` | 132.6 | 74.0 | 42.8 | **74.1** |

`GeoService.canSee` 链路在重负载下依然 0 样本采样（12.9.2 结论保持），FJP 组由 25.3 → **14.5 MB**。

#### 12.10.1 按权重的前 15 个分配站点（`ObjectAllocationSample`，首次 `com.aionemu` 帧）

| 权重 | 样本 | 站点 | 性质 |
|---|---|---|---|
| 29.46 MB | 71 | `PathData$MapData$SearchWorkspace.node` | A\* 节点槽位 |
| 12.74 MB | 10 | `NpcShoutData.getNpcShouts` | **每次生物事件复制喊话列表 + npcId 装箱** |
| 9.39 MB | 13 | `PathData$MapData.reconstruct` | 路径重建 `PathPoint`/列表 |
| 7.55 MB | 16 | `SearchWorkspace.searchNode` | A\* 搜索节点 |
| 6.49 MB | 16 | （无 `com.aionemu` 帧） | JDK/Netty/监控内部 |
| 6.48 MB | 14 | `KnownList.knownObjectsSnapshot` | 快照（既有不变量，不动） |
| 6.09 MB | 4 | `MoveTaskManager$1.apply` | 移动步进里的 `Integer.valueOf`（`movingCreatures.get(objectId)`） |
| 5.49 MB | 10 | `PathService.waypoints` | 路径结果 `float[]` |
| 5.39 MB | 8 | `WaterVolumeStore.find` | 每次移动 tick 的 `worldId` 装箱 |
| 4.66 MB | 10 | `BIHNode.intersectWhere` | 命中点/结果对象 |
| 4.52 MB | 9 | `LongObjectHashMap.init` | A\* visited 表扩容 |
| 4.16 MB | 7 | `PathData$MapData.releaseWorkspace` | 工作区归还 |
| 4.14 MB | 6 | `NettyConnectionHandler.writeAndConsume` | `HeapByteBuffer.duplicate()` |
| 4.10 MB | 12 | `SearchWorkspace.openNode` | A\* 开放节点 |
| 3.63 MB | 6 | `PathData$MapData.point` | 网格点对象 |

**分配点（栈顶帧）前三**：`Integer.valueOf` **29.49 MB (32)**、`SearchWorkspace.node` 26.06 MB (66)、
`Arrays.copyOf` 11.21 MB (19)。

#### 12.10.2 装箱（`Integer.valueOf` 29.49 MB）逐条归因

| 权重 | 调用点 | 说明 |
|---|---|---|
| 12.74 MB | `NpcShoutData.getNpcShouts:101/102` ← `hasAnyShout(worldId,npcId,type)` ← `GeneralNpcAI2.canHandleEvent` ← `onCreatureEvent` ← `MovementNotifyTask` | 每次生物事件：内层 `Map<Integer,…>.get(npcId)` 装箱（`shoutsByWorldNpcs` 外层已是 `IntObjectHashMap`，但内层不是）+ **列表复制** |
| 6.09 MB | `MoveTaskManager$1.apply:75` | `movingCreatures` 是 `ConcurrentHashMap<Integer,MoveRegistration>`，每次步进 `get(objectId)` 装箱 |
| 2.95 MB | `PathData.getMap:84` | 每次 `groundWaypointStatus → canReachWaypoint` 的 `maps.get(worldId)`（还带 `synchronized`） |
| 2.80 MB | `WaterVolumeStore.find:81` | 每次移动 tick `volumesByWorld.get(worldId)` |
| 1.29 MB | `MoveTaskManager.addCreature` | 同上（`putIfAbsent/replace`） |
| 0.61 / 0.60 / 0.54 / 0.54 MB | `World.getWorldMap`、`QuestStateList.getQuestState`、`PlayerQuestEventPort.toInventoryMap/questIdSet` | 其余零星装箱 |

注：`com.aionemu.commons.utils.collections.IntObjectHashMap` 只是 `LinkedHashMap<Integer,V>` 的包装（AR-003 的「假原始类型容器」），
**不能**用来消除装箱；真要消掉应改用 `LongObjectHashMap`（真原始键）或「注册时缓存装箱键」的做法。

#### 12.10.3 下一轮候选（按「收益 ÷ 风险」排序）

1. **`NpcShoutData`（≈13 MB）**：① `hasAnyShout(worldId,npcId,type)` 不再调 `getNpcShouts` 复制列表，直接在底层列表上找类型
   （与 2 参版本一致）；② 内层 `Map<Integer,List<NpcShout>>` 换成排序 `int[] npcIds` + 二分，或注册期缓存装箱键。风险低、语义纯。
2. **`MoveTaskManager` 装箱（≈7.4 MB）**：把 `Integer` 键缓存在 `MoveRegistration` 里（每注册一次），`apply`/`addCreature`/`remove`
   全部复用同一个键实例，`ConcurrentHashMap` 的并发语义完全不变。风险低。
3. **`PathData.getMap` / `WaterVolumeStore.find` / `World.getWorldMap`（≈6.4 MB）**：per-tick 世界查表；可用
   `LongObjectHashMap` 或「实体级缓存 worldId→map/volume 列表」。风险中（缓存失效语义）。
4. **寻路 A\* 工作区（≈29.5 + 7.6 + 4.1 + 4.5 + 4.2 ≈ 50 MB，另加 `reconstruct`/`waypoints`/`point` ≈ 18 MB）**：
   本轮最大单项域，但 12.3 已经做过租约池与节点槽位回退，继续要动 `SearchWorkspace` 的节点生命周期，**单独一轮**做。
5. `NettyConnectionHandler.writeAndConsume` 的 `HeapByteBuffer.duplicate()`（4.1 MB）属网络层，收益中等。

**结论**：`play-14` 证明重负载下 12.8 的射线优化仍保持归零；剩余分配已收敛为**寻路 A\* 工作区**与
**若干 per-tick/per-event 装箱与列表复制**两类，前者是大头（≈68 MB），后者是低风险快赢（≈27 MB）。

### 12.11 第十五轮：per-event/per-tick 装箱与列表复制 + 寻路 A\* 工作区（A 阶段）

依据 12.10 的归因（以及 `play-14-histogram.txt`：`PathData$MapData$Node` 存活 **523,557** 个 / 25.1 MB，`OpenNode` 139,205 / 5.6 MB，`SearchNode` 138,340 / 4.4 MB）。

#### 12.11.1 低风险快赢三项（≈27 MB/300 s）

1. **`NpcShoutData`（12.74 MB）**：每次生物事件（`MovementNotifyTask → onCreatureEvent → GeneralNpcAI2.canHandleEvent → hasAnyShout(…, type)`）
   都会 `getNpcShouts` 复制列表并给内层 `Map<Integer,List<NpcShout>>` 的 `npcId` 装箱。现在加载期把结构**冻结成升序 `int[]` + 平行列表**（`WorldShouts` record，
   `Arrays.binarySearch` 查询），3 参 `hasAnyShout` 直接在底层列表上判定类型，不再复制。合并顺序、防御性复制契约、`size()` 计数全部保持。
2. **`MoveTaskManager`（7.4 MB）**：`movingCreatures` 是 `ConcurrentHashMap<Integer,MoveRegistration>`，每 100 ms 的移动步进都为
   `get/remove/putIfAbsent/replace` 的 int 键装箱。现在 `MoveRegistration` 自带一个装箱后的 `key`（每注册一次），此后所有查表复用同一实例，
   并发语义（身份比较、`replace(k,old,new)`、`remove(k,v)`）不变。
3. **per-tick 世界查表（6.4 MB）**：
   - `PathData.getMap`：新增 `volatile MapLookup(worldId,map,epoch)` + `mapsEpoch`，命中时不再进 `synchronized(maps)`、不再装箱；装载/淘汰时 `mapsEpoch++`，
     淘汰时清 memo（不钉住被淘汰的 `MapData`）；
   - `WaterVolumeStore`：`Map<Integer,List<Volume>>` → 冻结的升序 `int[]` + 平行列表（单次 volatile 发布）；
   - `World.getWorldMap`：`IntObjectHashMap<WorldMap>` → 构造期排序的 `int[]` + `WorldMap[]`，缺失 id 仍抛 `WorldMapNotExistException`。

#### 12.11.2 寻路 A\* 工作区：A2 + A3（A1 未做，理由见下）

- **A2（visited 预分配，≈4.5 MB）**：`LongObjectHashMap` 新增 `ensureCapacity(expectedEntries)`（前提：表为空，会重建底层数组）；
  `SearchWorkspace.beginLowLevelSearch(int budget)` / `beginPortalSearch(int budget)` 在搜索开始时一次性预分配到
  `min(budget, 8192)` 条目，原先 `512→…→16384` 的多次 `resize` 只剩一次分配。`clear()` 仍保留容量。
- **A3（`Node` 去派生字段，≈4 MB 分配 + 4.2 MB 常驻）**：`Node.x/y` 存的是 `gridX * 0.5f + 0.25f` 的推导结果（两处调用点公式完全相同），
  现在由 `x()`/`y()` 按需推导，字段删除；`reset(...)`/`workspace().node(...)` 签名相应收窄。
- **A1（节点数组扩容）未做**：`Arrays.copyOf` 实测仅 3.4 MB/300 s，且属工作区首次长到峰值的一次性搬移；唯一"低风险替代"
  是按 50k 预算提前预分配，等于把内存提前吃满、把分配挪到启动期，不符合"不干扰功能、不涨常驻内存"的原则。
- **未做（C 类）**：`reconstruct`/`waypoints`/`point`（≈18.5 MB/300 s）是交给 `NpcMoveController` 跨 tick 消费、
  还可能被 `canReachWaypointCached` 缓存的结果对象，复用 buffer 会串数据，保持不动。

#### 12.11.3 golden 差分护栏（本轮的验收核心）

新增 `PathGoldenDiffTest`：8×4 块合成路径图（第 3/4 列有墙、最后一行留缺口）+ 6 条固定用例
（`short`/`around-wall`/`diagonal`/`same-cell`/`node-limit`/`no-path`），快照包含
`status | mode | processedNodes | abstractNodes | 路径点逐点坐标`。两层断言：

1. **工作区复用护栏**：先跑用例集 → 再跑一次整图对角深搜（50k 预算）→ 重放用例集，两次输出必须逐字相同
   （专防节点池/`visited` 残留与容量策略改动带来的隐性状态）；
2. **golden 比对**：与 `src/test/resources/aion/geo/path-golden.txt` 逐字比对。

流程与证据：**基线在改 A 之前录制**（`-Dpath.golden.record=true`，2026-09-16 23:47；`around-wall` = FOUND/LOW_LEVEL/13081 节点/256 点，
`diagonal` = FOUND/11521/256，`node-limit` = NODE_LIMIT/5，`no-path` = NO_PATH/16384），**改 A 之后再比对通过**（23:59），
`path-golden.txt` 未被重写 → 13,081 节点那条绕墙路径的 256 个坐标一字不差。

**测试**：`mvn -Dtest=PathGoldenDiffTest,PathDataTest,PathServiceConcurrencyTest,PathServiceCompressionTest,NpcMoveControllerPathTest,PlayerMoveTaskManagerTest test`
→ **BUILD SUCCESS，6 个测试类 125 例全绿**；更早一轮 `NpcShoutDataTest`(新 4)+`ShoutEventHandlerTest`+`WaterVolumeStoreTest`+`PathDataTest`+
`PathServiceConcurrencyTest`+`PathServiceCompressionTest`+`NpcMoveControllerPathTest`+`MoveTaskManagerTest`+`PlayerMoveTaskManagerTest`+
`WorldTest`+`WorldMapTest`+`WorldMapInstanceTest` → **153 例全绿**。
