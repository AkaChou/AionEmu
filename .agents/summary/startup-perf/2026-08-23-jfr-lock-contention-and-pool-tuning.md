# 启动加速：并行加载、刷怪并行与 Spring 锁消除 / Startup speedup: parallel loads, parallel spawning, Spring lock removal

日期 / Date: 2026-08-23 · 分支 / Branch: `quest`

## 结果总账 / Results ledger（六次启动对比）

| 阶段 | 基线 08-22 | P0-P2 | P5验证 | P6验证 | **P7验证 20:13** |
|---|---|---|---|---|---|
| staticDataLifecycle | 10853 | 12275 | 12183 | 9893 | **8257** ✅ |
| spawnLifecycle | **4463** | 2499 | 1866 | 2239 | 2803 ✅（低位） |
| enginesLifecycle | 410 | 553 | 2162 | 2833 | **962** ✅ |
| **总启动** | **18s** | 19s | 19s | 20s | **17s** ✅ |

P7 后 quest typed 编译完成时刻从 +18s 提前到 +14s，engines 等待从 2.8s 回落到 <1s。

## 变更清单 / Changes

1. **P0-2 NpcDropData 分片并行**（`NpcDropData.loadEager`）：串行 flatMap → STATIC_DATA_POOL 并行；merge/index 保持 join 后串行。
2. **P1-1 SkillData part 并行**（`SkillDefinitionLoader`）：30 个 part 并行 parse+expand+unmarshal，按序拼接保确定性；每任务自建 Unmarshaller。
3. **P1-2 RetailAiData 文件级并行**（`RetailAiDefinitionLoader.load`）：12 个源文件并行；party 校验移到 join 后。
4. **P3-① 池大小公式**（`XmlDataLoader.STATIC_DATA_POOL`）：`max(4,cpus)` → `cpus + NESTED_PARENT_TASKS(5)`。教训：嵌套父任务停车线程挤占干活线程曾致 staticData 10.9→12.3s 回归，公式化后恢复。
5. **P3-② npc-ai.xml 单次扫描双产出**：27MB 文件原被 NpcPathBehavior 与 RetailAiData.loadMappings 各解析一次；现 `loadMappings` 返回 `NpcMappings(npcs, pathBehaviors)` 记录，经 `XmlDataLoader.npcMappingsFuture` 共享，省一次全量解析。`NpcPathBehaviorDefinitionLoader` 仅测试仍用。
6. **P2 刷怪按地图并行**（`SpawnEngine.spawnAll`）：非副本地图并行生成于专用 world-spawner 池（min(地图数, cpus)）；审计确认 IDFactory/World 容器/TemporarySpawnEngine/WalkerFormationsCache/门状态全部有并发保护；失败等全部收尾再抛。
7. **P5 Spring 单例锁消除**（JFR 抓到的成片停车点）：
   - `GameHousingServices` 5 个方法加 resolved 缓存
   - `GameFeatureServices` baseService/staticDoorService/npcShoutsService 加缓存
   - `GameLocationBootstrapServices.getIfAvailable` 统一按 fallback 供应器做 ConcurrentHashMap 缓存（覆盖 19 个地点服务）
8. **P6 quest 编译池让路**（`QuestDefinitionCatalogManifest` 两处池 8→3）：预编译与 staticData 同窗抢核，quest 编译无截止时间，让路后剩余工作在 geo/world 窗口继续。实测 staticData 12183→9893ms（首次跌破 10s）。
9. **P7 quest 冲突校验分桶**（`QuestDefinitionCompiler.validateTransitionConflicts`）：JFR 热点第一名。`overlaps()` 仅同型事件可真（record equals 跨型恒 false，唯一例外 KillNpc/KillNpcSet），按 `event().getClass()` 分桶后仅桶内 + Kill 跨桶两两校验，消除 O(T²) 全对扫描的无效比较。改动前后 741 个 Quest 测试失败清单逐条 diff 完全一致（12 个均为预先存在），语义等价确认。

## JFR 分析结论（startup.jfr，静态数据窗口 21s）

- CPU 构成：46% Xerces/JAXB 解析、27% quest 编译、11% 刷怪、8% BIH 碰撞树、6% geo+BigInteger。
- **GC 仅 726ms、无类加载热点**——瓶颈是真实解析工作量，CPU 利用率 ~49%，墙钟由最大单线程块决定。
- 抓到 world-spawner 成片停在 `DefaultSingletonBeanRegistry.getSingleton` 全局锁 → P5 的修复依据。

## 明确不做 / Deliberately not done

- 继续调线程数：三次实测证明 staticData 对并行度不敏感（工作线程翻倍墙钟不变）。
- geo 懒加载：侵入面大收益有限。
- 二进制快照缓存 / Leyden AOT：staticData 突破 12s 的唯二路径，工程量大，待评估。

## 已知遗留 / Known leftovers

- `RetailAiDefinitionLoaderTest.keepsStructurallySupportedRetailCoverageForLegacyGenericBossAi` 在改动前即失败（expected 132 was 0），与本轮无关，待另查。
- engines 阶段计时包含 quest 编译的等待显形，属记账位置变化而非性能回归。

## 验证状态 / Verification status

编译通过；NpcDropDataTest/RetailPatternAI2Test/GameStaticDataLifecycleTest/GameServiceProviderCompatibilityTest/QuestDefinitionCompilerTest 等共 ~180 测试全绿（除上述遗留）。建议重启后对比 spawnLifecycle（预期 <1.5s）与 staticDataLifecycle（预期 ≤11s）。
