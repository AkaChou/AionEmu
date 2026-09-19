# 静态数据加载只读审计：CPU 边界与候选优化（2026-09-19）

> status: 审计完成；候选 2 探针已运行并**否决**解析器替换（未改代码、未引入依赖）
> evidence: `startup-20260824.jfr`（2026-09-19 20:55:41 录制，45s，profile）
> baseline: `log/console.log` 20:55 启动：总 14s，`staticDataLifecycle 7664ms`，
> `geoPathLifecycle 1861ms`，`spawnLifecycle 1720ms`；静态数据解析 7381ms。

## 1. 结论先行

静态数据加载是 **CPU 边界**，不是磁盘 I/O 或线程饥饿边界：

- 20:55:45–52.8（静态数据主窗口）JVM User 64–73.5%，`machineTotal` 100%；
  启动完成后 JVM User 降到 ~1%（`jdk.CPULoad` 时间序列）。
- 静态数据源体积大：items 87MB/11 分片、npcs 72MB/8 分片、skills 34MB/30 分片、
  ai 75MB（`npc-ai.xml` 单文件 26MB）、npc_drops 35MB/27 分片；总量约 300MB XML。
- 磁盘阻塞证据很弱：`jdk.FileRead` 慢读（>10ms）总计仅 0.303s，
  `static-data-loader` 的原生样本仅 7 个；不是 I/O 瓶颈。
- 因此 `NESTED_PARENT_TASKS`/池大小调参不是首选：机器已满负载，再加线程只会增加竞争。

## 2. CPU 分布（静态数据窗口内的执行样本，10ms 周期）

| 解析路径 | 样本 | 占 static-data 样本 |
|---|---:|---:|
| `XmlDataLoader.unmarshalShard`（items/NPCs JAXB） | 381 | ~33% |
| `XmlDataLoader.unmarshalSection`（static_data 分区 JAXB） | 181 | ~16% |
| `SkillDefinitionLoader.loadPart`（skills SAX→JAXB） | 133 | ~12% |
| `RetailAiDefinitionLoader.loadMappings` + `loadPatterns` | 135 | ~12% |
| `NpcDropData.loadCommonDropGroups` + `loadDropsFromFile` | 79 | ~7% |
| `WindstreamDefinitionLoader.parse`（DOM） | 38 | ~3% |
| 其它（waypoints/condition-spawns/npc-skills/…） | ~155 | ~14% |

绝对样本数不代表绝对 CPU 秒数（safepoint 采样有偏差），但相对占比稳定：
**item/NPC 分片 JAXB 是最大的单块 CPU**，其次是 static_data 分区、skills、Retail AI。

## 3. 关键路径时间线（20:55）

| 阶段 | 线程 | 时间窗 | 墙钟 |
|---|---|---|---:|
| `RetailAiDefinitionLoader.loadMappings`（26MB npc-ai.xml） | static-data-loader#244 | 45.106–49.424 | 4.32s |
| `RetailAiDefinitionLoader.loadPatterns` | static-data-loader#246 | 48.196–52.460 | 4.26s |
| `RetailAiData` 组装（等 mappings/patterns/areas/…） | static-data-loader#246 | 48.196–52.460 | 4.26s |
| `XmlDataLoader.unmarshalShard`（item/NPC 分片） | 多线程 | 45.31–50.24 | ~4.9s |
| `SkillDefinitionLoader.loadPart` | 多线程 | 50.41–52.0 | ~1.5s |
| 静态数据总窗口 | — | 45.1–52.46 | 7.38s |

最长依赖链是 `npc-ai.xml mappings → RetailAiData(patterns/areas/…)`；
items/NPCs 分片约 50.2s 完成，skills 在 50.4–52.0 补尾，最终都收在 52.46 附近。

## 4. 嵌套 join 的证据与限制

`jdk.ThreadPark`/`JavaMonitorWait`/`JavaMonitorEnter` 聚合（static-data-loader）：

- `XmlDataLoader.joinDefinition` 8.84s
- `SkillDefinitionLoader.load` 5.96s
- `RetailAiDefinitionLoader.load` 4.87s
- `NpcDropData.loadEager` 3.87s
- `RetailAiDefinitionLoader.joinOrNull` 2.48s
- 合计约 26.3s 停车等待（平均约 3.4 个线程在等）

这些等待说明嵌套“提交子任务再 join”的结构确实在占线程；但当时机器已 100%、
JVM 已占 ~7 核，**减少 join 只能改善尾延迟/重叠，不能消除主要 CPU 成本**。
先做 CPU 成本削减，再评估是否顺带扁平化 join。

## 5. 候选优化（按性价比）

1. **构建期紧凑/二进制缓存（最高收益，工程量大）**
   - 覆盖 items 87MB、npcs 72MB、skills 34MB、`npc-ai.xml` 26MB、npc_drops 35MB。
   - 启动时只做校验 + 反序列化，预计可去掉大部分 XML 解析 CPU（7.4s → 1–2s 量级，需实测）。
   - 风险：缓存失效/版本校验、对象图兼容、生成链路与提交体积。
   - **2026-09-19 用户否决**：XML 必须保持运行时唯一来源——临时修改 XML 后 reload 要立即生效，
     且不能要求"改一次 XML 就重新生成二进制"。后续请在"XML 仍是唯一来源"的前提下选方案。
2. **更快 XML 解析器探针（中等收益，中等风险）**
   - 对 StAX 路径（`loadMappings`、`loadPatterns`、`NpcDrop`、`NpcSkill`、waypoints）评估
     Woodstox/Aalto；对 JAXB/SAX 路径（`unmarshalShard`、`unmarshalSection`、
     `SkillDefinitionLoader`）评估 Woodstox SAX/Aalto 适配。
   - 优点是改动局部、可用单分片基准量化；缺点是引入依赖并需验证 JAXB 行为。
   - **2026-09-19 实测：已否决**（收益 −16%~+8%，且技能分卷 SAX 路径不兼容），
     见 `2026-09-19-xml-parser-probe-results.zh-CN.md`。
3. **拆分/缓存 `npc-ai.xml`（26MB）映射链（定向收益）**
   - 当前 mappings 4.3s + patterns 4.3s 占据关键路径；可把互不依赖的段落拆分并行，
     或对 mappings 结果做紧凑缓存。
4. **Windstream DOM→StAX（低收益，低风险）**
   - 约 3% 样本；可做但不足以改变启动量级。
5. **池大小/嵌套 join 调参（当前不推荐）**
   - CPU 已饱和，扩容线程会争抢；仅在 CPU 成本下降后再复测。

## 6. 候选 2 的静态可行性（2026-09-19 追加，javap 证据）

在不改代码的前提下，先判断"换解析器"能否通过 JAXP 系统属性生效，以及生产 feature 集是否兼容。

| 解析路径 | 能否被系统属性切换 | 依据 |
|---|---|---|
| JAXB 直读分片（items/NPCs/static_data 分区/drops） | ✅ | `JAXBContextImpl.getSAXParserFactory()` → `jaxb-core XmlFactory.createParserFactory()` → `SAXParserFactory.newInstance()`；只设置 `namespaceAware` 与 `secure-processing`，Woodstox 均支持 |
| StAX 扫描（`loadMappings`/`loadPatterns`/waypoints/…） | ✅ | `XMLInputFactory.newFactory()` 受 `javax.xml.stream.XMLInputFactory` 控制；`SUPPORT_DTD`、`javax.xml.stream.isSupportingExternalEntities` 见 Woodstox `ReaderConfig`/`StreamScanner` |
| `SkillDefinitionLoader.createPartReader()` | ❌ 需改代码 | 设置 Xerces 专有 `http://apache.org/xml/features/disallow-doctype-decl`；Woodstox 7.1.1 jar 内不存在该字符串，`SAXFeature.findByUri` 不识别 → `SAXNotRecognizedException` |
| `setXIncludeAware(false)` | ✅ | JDK `SAXParserFactory.setXIncludeAware` 仅在 `true` 时抛 `UnsupportedOperationException`，`false` 直接返回 |
| `UnmarshallerImpl.needsInterning()` | ✅ | 只用 `http://xml.org/sax/features/string-interning`（Woodstox `SAXFeature.STRING_INTERNING` 存在），且已捕获 `SAXException` |

结论：**JAXB/StAX 路径具备"仅加依赖 + 启动参数"的可行性；技能分卷 SAX 路径必须同时改代码**
（做特性探测或按解析器条件化设置 feature）。Woodstox 7.1.1 与 `stax2-api 4.2.2` 已在本地仓库；
`aalto-xml` 不在本地仓库，需要联网下载。

## 7. 推荐的第一步与探针状态

先做 **候选 2 的只读基准探针**：一个 item 分片、一个 NPC 分片、`npc-ai.xml`、一个 skill part，
分别用当前 JDK 内置解析器与 Woodstox 解析，测 CPU/墙钟/分配。
若解析器替换能带来 ≥20–30% 的 CPU 中位数下降，就先做解析器替换；否则直接进入候选 1 的二进制缓存设计。

探针已于 2026-09-19 21:50 授权运行完毕，**候选 2 否决**：

- 结果文档：`.agents/summary/startup-perf/2026-09-19-xml-parser-probe-results.zh-CN.md`
  （热态 + 冷启动曲线 + 兼容性实测）。
- 关键数据：热态 CPU item −5%、NPC −16%、`npc-ai.xml` StAX **+8%**；
  `SkillDefinitionLoader.createPartReader()` 在 Woodstox 下抛 `SAXNotRecognizedException`；
  同一方法（`loadMappings`，27MB）探针 168ms（热）/225ms（冷） vs 生产启动 JFR 跨度 4.32s。
- 推论修正：第 3 节的 4.32s/4.26s 方法跨度**不能**当作解析 CPU；启动成本大头是冷执行/JIT/竞争与
  解析之外的阶段（GC 并行阶段 45s 内约 5.9 核·秒，STW 暂停仅 622ms）。

**下一步建议**：静态数据阶段的定向 JFR 归因已完成，见
`2026-09-19-static-data-critical-path-attribution.zh-CN.md`
（并行度只有 4 线程；同窗口 JIT 16.2 核·秒、GC 3.0 核·秒；Reader 化对照实验无收益）。
候选 1 已按用户约束否决；新的候选清单见该文档第 5 节。

本轮审计未改生产代码、未引入依赖、未提交。
