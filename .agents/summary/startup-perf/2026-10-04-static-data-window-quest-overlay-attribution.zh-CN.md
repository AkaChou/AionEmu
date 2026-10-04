# 静态数据窗口 18.4s 归因：quest overlay 构链中的物品名索引重复全量扫描（2026-10-04）

> status: **归因完成（JFR 实锤）+ 修复已落代码 + 聚焦单测与真机复测均已过**
> （2026-10-04 17:17 启动：静态窗口 18.4s→9.17s、总启动 24s→15s；新 JFR 证实索引只扫描一次）。
> evidence: `startup-54531-2026_10_04_16_51_08.jfr`（慢，16:51:08 起 45s；本机快照）、
> `startup-89609-2026_09_28_23_29_11.jfr`（快，对照）；`log/console.log` 全量启动序列（64 次）。
> 关联: [AR-001](../../memory-bank/patterns/architecture-runtime.md)、`2026-09-19-static-data-critical-path-attribution.zh-CN.md`、
> `2026-09-29-quest-conflict-validation-bucketing.zh-CN.md`
> 代码: `src/main/java/com/aionemu/gameserver/questEngine/retail/RetailItemNameIndex.java:72`（修复点）、
> `src/main/java/com/aionemu/gameserver/dataholders/loadingutils/XmlDataLoader.java:315`（等待被吸入静态窗口的接点）

## 1. 现象与判据

- 用户报：`静态数据解析完成，耗时 18137 毫秒`（10-04 16:51 启动），此前感知 ~6–9s 不正常。
- `log/console.log` 64 次启动序列：**10-03 09:37 起稳定 16.5–22.3s**；09-23–09-30 为 5.3–13.6s（常见 6–10s）。
- 单阶段耗时（items/npcs/skills/drops）只涨 ~20–50%（09-30 12:20：items 3666 / npcs 3875；10-04 16:51：5473 / 5094），
  而总耗时翻倍 ⇒ 不是单表变慢，是**并行窗口被串行等待拖长**。
- 数据量逐字相同：128629 物品 / 87967 NPC / 59058 NPC 技能 / 14518 紧凑技能 —— 排除数据增长。

## 2. JFR 归因（16:51 慢启动）

**线程面**：`quest-catalog-preload`（守护线程）16:51:12.411 → 16:51:30.778 **连续跑满 18.37s**，
占全体 ExecutionSample 第一（1008/2873）；同窗口 `static-data-loader` 834、`main` 253。
逐秒采样：16:51:13–18 解析线程满载；16:51:19–30 解析全部结束后 **main 近乎 0 CPU、preload 独占一核**。

**main 的阻塞链**（`jdk.JavaMonitorEnter` 实测时长）：

```
XmlDataLoader.loadStaticData → ProductionQuestDefinitions.catalog → RetailQuestDriver.overlayProduction
  → verifyProductionCoverage → SimpleCollectItemHandler.instance()      [5.93s]
  → SimpleTalkHandler.instance()        [0.92s]  → SimpleUseItemHandler.instance()      [0.85s]
  → SimpleItemPlayHandler.instance()    [0.84s]  → SimpleCombineTaskHandler.instance()  [1.01s]
  → DataDrivenNativeRuntime.instance()  [0.94s]                    合计 ≈ 10.5s（= 尾部空窗）
```

**热点栈**：preload 线程 875/1008 样本在 `RetailItemNameIndex.loadItemTemplates`（`Matcher.find` 正则叶帧），
即每次调用都重读 11 个物品分片（~87MB 文本）并全量正则扫描；单次约 0.8–1.0s CPU，共 6+ 段独立扫描。
调用者分布：`SimpleCollectItemHandler.parseSymbol` 437（构造期按行调用，一处即 ~6 次扫描）、
`SimpleCombineTaskHandler.retailItemIndex` 77、`RetailQuestDriver.load` 76、`SimpleTalkHandler` 75、
`SimpleItemPlayHandler` 71、`SimpleUseItemHandler` 70、`DataDrivenNativeRuntime.loadProduction` 69。

**对照（09-28 23:29 快启动，总 5.8s）**：item-index 相关帧仅 **53 个样本**（只有 `RetailQuestDriver.load` 的一次扫描）。

## 3. 根因与回归引入

1. **`RetailItemNameIndex.loadItemTemplates()` 无进程内缓存**——全库唯一例外
   （`NativeNpcNameResolver.instance()`、`ProductionQuestDefinitions.catalog()`、`RetailQuestDriver.currentOrLoad()`
   均为 DCL 缓存）。启动期调用点共 8 处，其中 6 处经 `verifyProductionCoverage` 逐家族构造 handler 触发。
2. **10-01–10-03 原生车道落地**（`578f5a6b4` P0b/P1–P3、`91eaef381` P9 等）给各 handler 新增
   `retailItemIndex()` / `parseSymbol` 调用 ⇒ 1 次扫描变 10+ 次，单线程关键路径 +~9s。
3. **症状显形在"静态数据阶段"的直接原因**：`6cfb707da`（09-30）把
   `QuestsData.fromCatalog(ProductionQuestDefinitions.catalog())` 放进 `XmlDataLoader`（line 314-317），
   静态窗口从此等待整个 overlay 构链；此前这段等待在 `enginesLifecycle`
   （09-28：staticData 6117ms + engines 6426ms 等待；10-04：staticData 18405ms + engines 325ms）。

## 4. 修复（已落代码）

- `RetailItemNameIndex.loadItemTemplates()` 改 DCL 缓存（`loaded` volatile + 私有 `scanItemTemplates()`），
  与 `NativeNpcNameResolver.instance()` 同构；`build()` 语义不变（测试仍走 `build()`）。
- 钉子：`src/test/java/com/aionemu/gameserver/questEngine/retail/RetailItemNameIndexCachingTest.java`
  （`assertSame` 钉"装载即单例" + `size() > 100_000` 防真空通过；实测 128163 条）。
  已跑（IDEA MCP，2026-10-04）：通过，耗时 1352ms——含一次全量扫描，第二次调用命中缓存。
- 预期：10+ 次扫描 → 1 次，关键路径省 ~8–9s；静态窗口预计 18.4s → ~8s，总启动 24s → ~16s。

**真机复测（2026-10-04 17:17，`startup-95129-2026_10_04_17_17_05.jfr`；启动参数 `-Daion.staticData.extraLoaders=3 -Daion.quest.catalogCompileThreads=4`）**：

| 指标 | 修复前 16:51 | 修复后 17:17 |
|---|---:|---:|
| 静态数据解析 | 18137ms | **8952ms** |
| staticDataLifecycle | 18405ms | **9166ms** |
| 启动完成（总初始化） | 24s | **15s** |
| 尾部空窗（最后数据消息→解析完成） | ~12s | **~2s** |
| `loadItemTemplates` 帧样本 | 875 | **63**（单次扫描，全部经 `scanItemTemplates`） |
| preload 线程样本 / 时长 | 1008 / 18.4s | **273 / 9.05s**（与解析重叠完成，不再拖窗口） |

复测后静态窗口重新由解析本身主导（最大单阶段 NpcDropData 6853ms），enginesLifecycle 365ms（overlay 早已就绪）。

## 5. 复现命令（JFR 归因）

```bash
JFR=$JAVA_HOME/bin/jfr
$JFR print --json --events jdk.ExecutionSample --stack-depth 24 startup-54531-2026_10_04_16_51_08.jfr > /tmp/exec.json
$JFR print --json --events jdk.JavaMonitorEnter --stack-depth 12 startup-54531-2026_10_04_16_51_08.jfr > /tmp/mon.json
# 过滤 sampledThread.javaName == quest-catalog-preload / main，按秒分桶 + 帧频统计（见 AR-001 的 jfr-attribute.py 同思路）
```

## 6. 未决

- 次级项（未做）：`SimpleCollectItemHandler` 构造内逐行 `parseSymbol` 缓存后为常量开销，可留；
  更彻底方案（索引构建自已解析 `ItemData`，省最后一次 87MB 重扫 + 正则）有顺序耦合，未立项。
- `verifyProductionCoverage` 双跑（preload 线程 + main 各一次）在缓存后余下成本（复测 JFR：preload 273 样本里
  `Matcher.*` 仍占 ~140，主要来自 NPC 名索引与物品索引各一次构建）可再评估。
- 静态窗口剩余时间已回到解析本身（items/npcs/skills/drops 四块并行，最大单阶段 6.8s），
  进一步压缩属既有候选清单（JIT/AOT、热点分片手写 StAX），不在本次范围。
