# 启动 JFR：碰撞树预构建并行化（2026-09-19）

> status: 已实现，聚焦测试通过；待重启 JFR 复测
> evidence: `startup-20260824.jfr`（2026-09-19 19:32:53 录制，45s，profile 设置）
> related: `src/main/java/com/aionemu/gameserver/world/geo/RealGeoData.java`、`geoEngine/scene/Mesh.java`

## 1. 本次启动基线（19:33 运行）

- 总启动：14s；`staticDataLifecycle 8309ms`、`spawnLifecycle 1926ms`、`geoPathLifecycle 1760ms`、`housingLifecycle 22ms`。
- 静态数据解析：7688ms；最慢阶段 `SkillStreamJaxbWork 9278ms`（并行分片工作量之和，不是墙钟）、
  `NpcDropData 7900ms`、`SkillData 7742ms`、`RetailAiData 6662ms`、`ItemData 6545ms`。

## 2. JFR 热点（45s 录制，1977 个 Java 执行样本）

| 排名 | 方法 | 样本 | 占比 |
|---|---|---:|---:|
| 1 | `com.sun.org.apache.xerces.internal.impl.io.UTF8Reader.read` | 501 | 25.34% |
| 2 | `BIHTree.sortTriangles` | 70 | 3.54% |
| 3 | `BIHTree.createBox` | 47 | 2.38% |
| 4 | `XMLNSDocumentScannerImpl.scanStartElement` | 46 | 2.33% |
| 5 | `BIHTree.createNode` | 43 | 2.18% |
| 6 | `PNGImageReader.decodePass` | 40 | 2.02% |
| 7 | `RetailPatternAI2.supports` | 34 | 1.72% |

分配压力前排：`byte[] 19.62%`、`String 6.85%`、`Object[] 6.57%`、`short[] 3.51%`、`ArrayList 3.13%`。

## 3. 线程/调用方定位

- `static-data-loader` 线程组共 911 个样本，`UTF8Reader.read` 431 个；主要调用方为
  `XmlDataLoader.unmarshalShard`（187）、`QuestDefinitionXmlCompiler.parse`（61）、
  `RetailAiDefinitionLoader.loadMappings`（42）、`loadPatterns`（38）、
  `NpcDropData.loadCommonDropGroups`（37）、`NpcSkillDefinitionLoader.load`（34）、
  `WindstreamDefinitionLoader.parse`（29）、`XmlDataLoader.loadRetailAiWaypointData`（28）。
  静态数据加载已在多个 `static-data-loader` 线程并行，墙钟受最长任务约束。
- `LongRunningPool-1` 共 166 个样本，全部落在 `BIHTree`（`sortTriangles` 65、`createBox` 46、
  `createNode` 38），时间窗 19:33:07.526–19:33:10.541，来源为 `RealGeoData` 的碰撞树预构建。
- 代码中 `RealGeoData.prebuildCollisionDataAsync` 原实现只提交 **一个** 长任务，在循环里逐 Mesh
  串行 `createCollisionData()`；方法注释却写着「后台并行预构建」，实现与注释不一致。

## 4. 本次改动

1. `RealGeoData.prebuildCollisionDataAsync`：按 `max(2, availableProcessors())` 轮转切片，
   每个分片向 `GameThreadPoolServices.threadPoolManager().submitLongRunning` 提交一个任务；
   仍走生命周期线程池，不引入 `parallelStream()` 或嵌套 ForkJoin 池（保持既有并发门禁语义）。
2. 新增包内纯函数 `RealGeoData.partitionRoundRobin(List, int)`，保证每项只出现一次、分片顺序稳定；
   `RealGeoDataConcurrencyTest` 增加覆盖与参数校验用例。
3. `Mesh.collisionTree` 改为 `volatile`，`createCollisionData()` 使用双重检查锁定；
   `collideWith` 只在本地读取一次引用。这样后台并行预构建的树对游戏线程安全发布，
   也避免预构建与首次碰撞懒加载并发时重复建树。

## 5. 验证状态

- 已做：IDE 文件检查（三个文件 0 error）、`git diff --check` 通过。
- 已做（2026-09-19 20:01）：
  `mvn -B -Dstyle.color=never -Dtest=RealGeoDataConcurrencyTest,RealGeoDataLookupTest,GeoWorldLoaderAionServerFormatTest,GeoMapWalkerCollisionTest,DespawnableGeoHookTest,GeoServiceGroundSearchTest,GeoServiceSkillObstacleTest test`
  → 40 tests / 0 failures / 0 errors / 0 skipped，`BUILD SUCCESS`（16s；testCompile 1019 个源文件通过）。
- 未做：服务端重启与 JFR 复测（按项目规则由用户执行）。
- 重启后对比：`geoPathLifecycle`、`LongRunningPool-1` 的 BIHTree 样本窗口、启动完成耗时；
  预期碰撞树预构建从单线程 1.66s CPU 降到接近 10 路并行（实测值以重启 JFR 为准）。

## 6. 后续候选（未开工）

- 静态数据墙钟仍占启动主要部分；下一步优先审计 `NpcDropData`（common_drop_groups.xml 6.5MB）、
  `SkillData` 并行分片与 `RetailAiData` 的 XML 解析路径，评估更快解析器或紧凑缓存格式。
- `RetailPatternAI2.supports` 对每个 NPC 重复校验同一份 pattern 结构，可评估按 Pattern 记忆化；
  但 spawn 已并行，墙钟收益需先量化。
