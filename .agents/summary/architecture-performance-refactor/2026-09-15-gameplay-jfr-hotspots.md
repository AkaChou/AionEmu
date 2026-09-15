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
- 本节的 `RealGeoData` / `PathData` 优化代码已随本提交落地并通过 172 例聚焦测试，但**尚未重启验证运行期收益**（需要在重启后再录一段同长度 JFR，对比 `Integer`/`Long` 装箱占比与 `searchLowLevel`/`getMap` 的样本占比）。
