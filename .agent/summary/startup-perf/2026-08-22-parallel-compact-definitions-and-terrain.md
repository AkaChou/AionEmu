# 启动加速：紧凑定义并行加载 + 地形 PNG 并行解码 / Startup speedup: parallel compact definitions + parallel terrain decoding

日期 / Date: 2026-08-22 · 分支 / Branch: `quest`

## 背景 / Background

以 2026-08-22 14:02 启动日志为基线（总耗时 20s），瓶颈分布：
Based on the 2026-08-22 14:02 startup log (20s total), the bottleneck split was:

| 阶段 / Phase | 耗时 / ms |
|---|---|
| staticDataLifecycle | 10278 |
| spawnLifecycle | 3907 |
| geoPathLifecycle | 3665 |
| enginesLifecycle | 520 |
| worldBootstrapLifecycle | 471 |

## 变更 1：紧凑定义与主 XML 并行 / Change 1: compact definitions parallel to main XML

`XmlDataLoader.loadStaticData`（`src/main/java/com/aionemu/gameserver/dataholders/loadingutils/XmlDataLoader.java`）

- 之前：`static_data.xml` 反序列化完成后，11 个紧凑定义加载（npc drops、hotspot、motion、charge-skills、npc-skills、AI、npc-path-behavior、retailAi、AI waypoints、windstream、pet rides）在主线程**串行**执行，约 2s。
- 之后：这些 loader 在反序列化**开始前**通过 `CompletableFuture.supplyAsync`（commonPool）启动，反序列化结束后按原顺序 `joinDefinition` 回填。与既有的物品/技能并行路径同一思路。
- Before: after the main unmarshalling, 11 compact-definition loads ran serially on the main thread (~2s). After: they start before unmarshalling via the common pool and are joined afterwards in the original order — same pattern as the existing parallel item/skill paths.
- 安全性：已核对全部 loader 均为自包含文件解析器，无 `DataManager` 静态依赖、无共享可变状态；`NpcCombatDefinitionLoader.apply` 依赖 `data.npcData`，保留在反序列化之后。
- Safety: all loaders verified as self-contained file parsers with no DataManager/static or shared mutable state; `NpcCombatDefinitionLoader.apply` (needs `data.npcData`) stays post-unmarshal.
- 失败语义：`joinDefinition` 解包 `CompletionException` 并按原始 `RuntimeException`/`Error` 重抛；主流程失败时取消未完成的 future（与 `DataManager.loadStaticData` 一致）。

## 变更 2：地形 PNG 并行解码 / Change 2: parallel terrain PNG decoding

`GeoWorldLoader.loadTerrains`（`src/main/java/com/aionemu/gameserver/geoEngine/GeoWorldLoader.java`）

- 之前：161 张 geo 地形/材质 PNG 串行 `ImageIO.read` + 光栅解码（geo 阶段的主要耗时之一）。
- 之后：`IntStream.parallel()` 并行解码到 `DecodedTerrain[]`，随后**按原排序串行回填** `GeoMap`。direct PNG 覆盖合并 PNG 的语义（依赖排序后"后应用者胜出"）保持不变。
- Before: 161 terrain/material PNGs were decoded serially. After: decode in parallel via the common pool, then apply sequentially in the original sort order — the direct-over-combined override semantics (later application wins) are preserved exactly.
- 测试约束已核对：`GeoWorldLoaderAionServerFormatTest` 的 `loadTerrainsPrefersDirectPngOverCombinedPng` / `loadTerrainsSharesCombinedTerrainPngAcrossMaps` 语义不变；源码断言（`loadTerrains(`、`missingMeshes`）仍满足。

## 明确不做 / Deliberately not done

- GEO 整体与静态数据并行：`RealGeoData.loadWorldMaps` 依赖 `DataManager.WORLD_MAPS_DATA`，且 `loadMeshs` 读取 `MATERIAL_DATA`，无法提前。
- Whole-geo-vs-static-data overlap: `RealGeoData` depends on `WORLD_MAPS_DATA`/`MATERIAL_DATA`.
- 编排器阶段重叠（如 engines ∥ worldBootstrap）：`GameStartupSequenceLifecycleTest` 钉死了事件顺序，收益仅 ~0.5s。
- Orchestrator-level overlap (e.g. engines ∥ worldBootstrap): the order test pins the event sequence and the gain is only ~0.5s.

## 验证状态 / Verification status

未运行构建与服务器（遵守 AGENTS.md 规则）。建议验收：
Not built or run (per AGENTS.md). Suggested acceptance:

1. `mvn test -Dtest=GeoWorldLoaderAionServerFormatTest,DataManagerTest,GameStaticDataLifecycleTest`
2. 重启后对比日志：`静态数据解析完成` 与 `staticDataLifecycle`/`geoPathLifecycle` 的阶段耗时。
3. 预期：staticData 10.3s → ~8s；geoPath 3.7s → ~2.5s；总启动 20s → ~17s。

## 后续候选 / Follow-up candidates

- 按世界并行刷怪（spawn 3.9s → ~1s）：需要运行时验证 IDFactory/AI2/注册表的线程安全。
- 将 NpcData（反序列化 4.4s）从 static_data.xml 缓存拆分为独立并行缓存文件（同 items 模式）：收益最大但工程量大。
- AppCDS / JIT 分层预热等 JVM 级手段。
