# 启动加速：NPC/物品模板分片直读 + 自定义物品覆盖层 / Startup speedup: shard-direct loading for NPC/item templates + custom item overrides

日期 / Date: 2026-08-22 · 分支 / Branch: `quest` · 前置 / Builds on: `2026-08-22-parallel-compact-definitions-and-terrain.md`

## 启动卡住事故与修复 / Startup stall incident and fix (2026-08-22 15:49 → 16:30)

> **第三轮更新（16:33→17:05）见下节**——专用池修复后服务器仍卡住同一位置；已定位三个新嫌疑并全部消除，另加阶段看门狗自证卡点。
> **Round 3 below (16:33→17:05)**: the server still stalled at the same spot after the dedicated-pool fix; three new suspects eliminated plus a phase watchdog for self-diagnosis.

用户首启（15:48，重建缓存+分片）后第二次启动在主反序列化阶段无输出卡死，两次同位置复现。经活体 jstack + 复现 harness 定位为**两个叠加缺陷**：
After the first start (15:48, cache+shard rebuild), the second start stalled silently, reproduced twice at the same spot. Live jstack + a repro harness identified **two stacked defects**:

1. **commonPool 丢失唤醒（挂起主因）**：jstack 显示主线程反序列化早已完成、停在 `joinDefinition(motionDataFuture)` 永久等待，而 commonPool 全部 9 个 worker 空闲趴在 `awaitWork`——已提交任务无人执行、无人唤醒。机制：分片/紧凑定义加载是"池任务内部再提交子任务并 join"的嵌套结构，在 commonPool 上触发 managedBlock 补偿线程的丢失唤醒竞态（JDK 26 实测）。修复：`XmlDataLoader.STATIC_DATA_POOL` 专用定长守护池（大小=核数），全部静态数据 future（12 定义 + 19 分片 + 预加载上下文 + DataManager 物品/技能）改走它。定长池共享队列无窃取信号协议，结构性杜绝该问题；父任务≤2 且只等自己的子任务，无死锁。
   **commonPool lost wakeup (the hang)**: jstack showed main parked joining `motionDataFuture` forever with every commonPool worker idle in `awaitWork` — a submitted task never ran. Nested submit-and-join on commonPool triggers a managedBlock compensation lost-wakeup race (observed on JDK 26). Fix: dedicated fixed daemon pool `STATIC_DATA_POOL` (core-sized) for all static-data futures; a plain fixed pool structurally cannot strand tasks, and ≤2 parents joining only their own children cannot deadlock.
2. **CCL 裸 JAXB 调用 + 吞栈日志（诊断干扰项）**：`NpcDropData.loadCommonDropGroups` 与 `HotspotLocationData.load` 裸调 `JAXBContext.newInstance`（依赖线程上下文类加载器），池线程上部分环境抛 `ClassNotFoundException`；且 `loadStaticData` 的 catch 用 `I18n.get(key, e)` 把异常格式化进消息、**栈被吞**，使故障呈现为"无日志卡死"。修复：`createJaxbContext`（含 CCL 处理）改 public、两处改走它、catch 单独附加 throwable 打栈。
   **Raw JAXB CCL calls + swallowed stacks**: two loaders called raw `JAXBContext.newInstance` (thread-CCL dependent) on pool threads; the catch also swallowed stacks, masking failures as silent stalls. Both fixed.

验证 / Verification：
- 复现 harness `StaticDataLoadRepro`（src/test，非服务器、无端口/DB）专用池修复后连续 **5/5 通过，4.1-5.2s/次**（items=128629/npcs=87967/skills=14518）；目标测试 41/41 绿。
  Harness: 5/5 green runs at 4.1-5.2s after the dedicated-pool fix; targeted tests 41/41 green.
- 模板数与旧合并缓存差几条（128629 vs 128632 / 87967 vs 87975）：分片直读当前源文件，旧缓存构建较早——分片是更新的真实值。
  Template counts differ slightly from the old merged cache: shards read current sources.
- 若再次无日志卡住：对存活进程执行 `jstack <pid>` 抓栈。
  If a silent stall recurs: `jstack <pid>` the live process.

## 第三轮：专用池后仍卡住 → 排除法 + 看门狗 / Round 3: still stalls after the dedicated pool → elimination + watchdog (16:33→17:05)

用户重启后（16:33）**依然卡住**：全部 `[static-data-loader]` future 正常完成（物品 8.6s/NPC 8.7s/技能等全部打点），主线程自"正在从…解析静态数据"后无任何输出。进程已被杀，无法再 jstack。排查过程：

1. **主线程路径收窄**：最后一条主线程日志之后的路径 = `preloadedContext.get()` → 主缓存 unmarshal → 各 join（future 均已完成，不可能阻塞）→ 完成日志（未出现）。`StaticData` 无 `afterUnmarshal` 回调；`STATIC_DATA_SUMMARY_LOG` 默认 false，logSummary 本就不打印（harness 中看到的"摘要"行实为 `logSlowSectionTimings` 慢分段告警）——因此卡点可在 unmarshal 至构造器收尾之间任意处，**commonPool 上的静态字段赋值 `runAsync`+`get()` 重新成为头号嫌疑**（其失败模式恰为无错误无日志无限等待）。
   Main's path after its last log narrows to: preloaded-context get → main unmarshal → joins (futures all done) → completion logs (absent). StaticData has no afterUnmarshal; the summary log is off by default, so the wedge could sit anywhere up to the ctor tail — reinstating the commonPool assignment `runAsync`+`get()` as prime suspect (its failure mode is exactly a silent indefinite wait).
2. **服务器与 harness 的结构性差异**：harness 从未运行 (a) 构造器尾部的 commonPool 赋值、(b) 与静态阶段并行的 quest 目錄预加载（`GameStartupSequenceLifecycle:265` 在 staticData 之前提交，commonPool + 内部 8 线程池，全程抢核）。新增 `StaticDataLoadRepro server` 模式完整复刻该时序（JAXB 预热 → quest 编译并行 → 完整 `new DataManager()`），在 Homebrew 26 与 IDEA 实际使用的 Azul Zulu 26.0.2.1（含 `-ea`）下共 **13/13 全部通过（10.9-15.3s）**——CPU 争抢使阶段慢 ~3 倍（物品 8.6s vs 独跑 ~2s），但无法在本机复现挂起。环境残留差异仅剩 Spring 上下文/IDEA 启动器本身。
   Structural deltas vs harness: the ctor-tail commonPool assignment and the quest catalog preload racing the static phase were never harness-covered. A new `server` harness mode replays the exact sequence; 13/13 green across Homebrew 26 and Azul Zulu 26.0.2.1 with `-ea` (10.9-15.3s) — contention slows the phase ~3× but no repro; the residual delta is the Spring/IDEA launcher itself.
3. **三项修复（消除整类风险 + 自证卡点）/ Three fixes (eliminate the risk class + self-diagnosis)**：
   - `DataManager` 构造器：~130 个静态字段赋值由 commonPool `runAsync`+`get()` 改为**主线程内联**（赋值无并行收益，主线程本就立即等待）；`awaitStaticDataAssignment` 及其两个测试随之删除。
     The ~130-field assignment now runs inline on the loading thread (async bought nothing; main waited immediately); helper + its 2 tests removed.
   - `QuestEngine.preloadProductionCatalog`：commonPool `runAsync` 改为专用守护线程 `quest-catalog-preload`（不占静态数据池、不依赖 commonPool 调度）。
     Quest catalog preload now uses a dedicated daemon thread instead of commonPool.
   - **静态数据阶段看门狗**：`DataManager.startStallWatchdog`——加载超过 60s 未完成，每 30s（最多 5 次）以 ERROR 转储加载线程 + `static-data-loader`/`quest-catalog-preload`/commonPool 线程的栈与锁等待（`log.static_data.phase_stall_detected`）。下次重启无论是否仍卡，日志自证卡点，无需 jstack。
     Phase watchdog: if loading exceeds 60s, dump loader/worker stacks with lock info at ERROR every 30s (max 5) — the next restart identifies any residual stall by itself.
4. **验证**：目标测试 **65/65 绿**（新增 GameEnginesLifecycle/QuestEngineRuntimeComposition/SequenceLifecycle/ConsoleReporter）；harness server 模式修复后 3/3 通过（11-13s，看门狗静默）。
   Verification: targeted tests 65/65 green; harness server mode 3/3 at 11-13s with the watchdog silent.

## 决策背景 / Decision context

- 合并缓存（static_data.xml 150MB）与源体积（~136MB）基本相同——合并不减少解析量，只为"单文档 JAXB 根"服务。
  The merged cache (150MB) is no smaller than its sources (~136MB) — merging never reduced parse volume; it only served the single-document JAXB root.
- 因此 NPC 与物品模板改为**分片直读**：不再进入合并缓存，各自切成等大分片并行反序列化。
  NPC and item templates therefore moved to **shard-direct loading**: out of the merged cache, split into equal-size shards unmarshalled in parallel.
- 物品合并缓存路径整体废弃（不再生成 `cache/item_templates.xml`）；`static_data.xml` 移除 `<import file="npcs"/>`。
  The item merge path is retired (`cache/item_templates.xml` is no longer produced); `static_data.xml` drops `<import file="npcs"/>`.

## 分片命名与语义 / Shard naming and semantics

- 文件名：`{元素名}_{startId}_{endId}.xml`，如 `npc_template_250001_261250.xml`、`item_template_100000001_110000000.xml`；startId/endId 为分片内首尾模板的实际 ID。
  File names: `{element}_{startId}_{endId}.xml` with the shard's actual first/last template ids.
- 每片 ≤ 12,000 个模板（`XmlDataLoader.TEMPLATES_PER_SHARD`）；NPC 88k→8 片，物品 128.6k→11 片。
  ≤ 12,000 templates per shard; NPCs → 8 shards, items → 11.
- 分片目录：`cache/npc_shards/`、`cache/item_shards/`（运行时生成，不入库）。
  Shard dirs: `cache/npc_shards/`, `cache/item_shards/` (runtime-generated, not committed).
- 新鲜度：任一源文件新于任一分片（或分片缺失）→ 重建；重建先写 `.tmp` 再改名，全部成功后清理旧分片与残留临时文件。
  Freshness: rebuild when any source is newer than any shard (or shards missing); writes go through temp files + rename, stale files are cleaned only after all writes succeed.

## 自定义物品覆盖 / Custom item overrides

- 新增 `data/static_data/items/item_template_custom.xml`（已提交空模板含示例注释）：每次启动加载，条目按 ID **整体替换**或新增分片模板；分片器不会改动此文件。
  New `items/item_template_custom.xml` (committed starter with a commented example): applied on every start; entries fully replace or append shard templates by id; the shard writer never touches it.
- 合并入口 `ItemData.assembleFrom(shards, custom)`：分片按序 putAll → custom 覆盖 → 一次性 `setData` 重建 items/itemsByName/allItems 索引；日志输出覆盖/新增条数。
  Merge entry `ItemData.assembleFrom(shards, custom)`: sequential putAll → custom overrides → one `setData` pass rebuilds all indexes; replaced/added counts are logged.

## 变更文件 / Changed files

| 文件 / File | 变更 / Change |
|---|---|
| `loadingutils/TemplateShardWriter.java` | 新增：分片写出器（等分、ID 区间命名、原子写、陈旧清理、新鲜度检测）/ new shard writer |
| `loadingutils/XmlDataLoader.java` | NPC/物品分片并行加载；`loadStaticData` 接入 npcDataFuture；三处 FileReader→BufferedInputStream（主缓存/分片/校验）；删除 item 合并辅助方法 / sharded loads, InputStream fast path, item-merge helpers removed |
| `dataholders/DataManager.java` | 静态字段赋值 commonPool runAsync+get → 主线程内联；新增阶段看门狗 `startStallWatchdog`（60s/30s×5 转储栈）；删除 `awaitStaticDataAssignment` / inline assignment, phase stall watchdog, helper removed |
| `questEngine/QuestEngine.java` | 目錄预加载 commonPool → 专用守护线程 `quest-catalog-preload` / preload on dedicated daemon thread |
| `dataholders/ItemData.java` | `assembleFrom`、afterUnmarshal 空列表防护、`reload()` 改走分片路径 / merge entry, empty-list guard, reload via shards |
| `data/static_data/static_data.xml` | 移除 `<import file="npcs"/>`（附注释说明）/ npcs import removed with comment |
| `data/static_data/items/item_template_custom.xml` | 新增自定义覆盖模板 / new custom override starter |
| `messages(.zh_CN).properties` | 新增 4 个日志 key / 4 new log keys |
| 测试 / tests | `TemplateShardWriterTest`（等分/命名/清理/失败快速/多源）；`XmlDataLoaderTest` 改写 item 断言为分片+覆盖语义、新增 NPC 分片测试、`mainStaticDataLeavesNpcDropsOutOfSharedCache` 增加 npcs 不变量 |

## 预期收益 / Expected gains

- 主合并缓存 150MB→~74MB；NPC 段 4.4s→~1-1.5s（8 片并行）；物品 6s→~1-1.5s（11 片并行）；字节流直读另有 10-20%。
  Merged cache 150MB→~74MB; NPC 4.4s→~1-1.5s; items 6s→~1-1.5s; byte-stream input adds 10-20%.
- staticDataLifecycle 预期 10.3s → **~5-6s**；总启动 ~17s → **~12-14s**（首次启动额外付一次性成本：静态缓存重建 ~3-4s + 分片生成 ~3-4s）。
  staticDataLifecycle expected 10.3s → ~5-6s; total startup ~17s → ~12-14s (first start pays one-time rebuild + shard generation).

## 验证 / Verification

已由用户授权执行构建验证（2026-08-22 15:30-15:50）：
Build verification executed with user authorization:

- 目标测试 `TemplateShardWriterTest,XmlDataLoaderTest,DataManagerTest,GameStaticDataLifecycleTest`：**41/41 通过**。
  Targeted tests: 41/41 green.
- 全量 `mvn test`（含改动）：2878 tests，26F/64E/2S；stash 掉全部改动后在纯净树上重跑：2872 tests，**26F/64E/2S，失败类名单逐类一致**（31 个类，全部为 quest 修复域与 `AionConnection` 顺序敏感的类初始化问题 `Min Threads must be positive`）——**均为分支预存问题，本轮改动零回归**；差值 +6 为本轮新增测试且全部通过。
  Full suite with changes vs pristine stash: identical 26F/64E/2S and identical failing-class list (all quest-repair domain plus order-dependent `AionConnection` class-init); the +6 test delta is this batch's new tests, all passing. Zero regressions.
- 验证期间修复的三处小问题：`ItemData` 缺 `Unmarshaller` 导入；`Files.setLastModified` API 误用（改 `File.setLastModified`）；子元素序列化断言过严（`XMLEventWriter` 将自闭合标签展开为 `<x></x>`）。
  Three small fixes during verification: missing Unmarshaller import; setLastModified API misuse; over-strict serialization assertion (XMLEventWriter expands self-closing tags).
- 运行期性能验收仍待用户重启服务器确认（分片日志行与阶段耗时）。
  Runtime timing acceptance still pending a server restart by the user.

## 已知边界 / Known boundaries

- 分片 startId/endId 是标识而非排序保证（物品多源文件按文件名序切分，ID 不保证全局单调）。
  Shard id ranges are identifiers, not sort guarantees (multi-source items split in file-name order).
- 若源中存在重复模板 ID 跨分片边界导致同名分片文件，写出器会快速失败（IllegalStateException）。
  Duplicate ids producing a duplicate shard name fail fast.
- 自定义覆盖为**整体替换**语义：自定义条目需携带完整属性（从 item/item_*.xml 复制修改）。
  Custom overrides fully replace: entries must carry complete attributes.
