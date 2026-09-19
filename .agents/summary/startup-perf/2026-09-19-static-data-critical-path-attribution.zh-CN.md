# 静态数据阶段：并行度、JIT/GC 与竞争归因（2026-09-19）

> status: 只读归因完成；**记录用户约束：静态数据必须保持 XML 为唯一来源，不做构建期二进制/紧凑缓存**
> evidence: `startup-20260824.jfr`（2026-09-19 21:43:42 起，45s，profile）
> 工具: `.agents/summary/startup-perf/jfr-attribute.py`
> 关联: `2026-09-19-static-data-cpu-bound-audit.zh-CN.md`、`2026-09-19-xml-parser-probe-results.zh-CN.md`

## 0. 前置约束（来自用户，2026-09-19）

**XML 加载不使用二进制缓存**：既要支持临时修改 XML 后 reload 立即生效，也不接受"改一次 XML 就要重新生成二进制"。
因此候选 1（构建期紧凑/二进制缓存）**否决**，后续不得再作为推荐项；优化必须在"XML 仍是运行时唯一来源"的前提下进行。

## 1. 静态数据窗口的规模

- 窗口：21:43:46.26 – 21:43:54.31，**8.05s**；`static-data-loader` 采样 983 个（10ms 周期）。
- 窗口内 `jvmUser` 56–71%（≈6–7 核），`machineTotal` 100%（注意包含 IDE 等其它进程）。
- 采样到的 Java 执行约 1206 个（≈12 核·秒），**但同期 JVM 真实 CPU ≈ 6.5 核 × 8s**，差额主要来自下面两项。

| 同窗口消耗 | 量 | 说明 |
|---|---:|---|
| JIT 编译（`jdk.Compilation`） | **62 次 / 16.2 核·秒** | 编译器线程，占 JVM CPU 的最大单项 |
| GC 并行阶段（`jdk.GCPhaseParallel`） | **3.0 核·秒** | `ScanHR 1.59s`、`ObjCopy 0.48s`（STW 暂停全录制仅 622ms） |
| 应用线程（ExecutionSample） | ≈12 核·秒（含 pool-5） | 采样有丢失，绝对值偏低 |

## 2. 并行度与超订（修正后的读数）

- 线程池规模：`XmlDataLoader.STATIC_DATA_POOL = availableProcessors() + NESTED_PARENT_TASKS(5)`
  = **本机 15 线程**（溢出部分用于吸收嵌套 join 里 parked 的父任务）；窗口内实测正好 15 个
  `static-data-loader` 线程 ID，另有 `pool-5-thread-1/2/3`（quest 目录预编译池，上限 3）。
- 真正在跑的规模：窗口 8.05s、JVM 约 6.5 核 CPU ⇒ **平均约 6 个线程同时运行**，
  其余线程在 park（等待子任务）。10 核机器已被吃满（`machineTotal` 100%，含 IDE 等其它进程）。
- ⚠️ **采样丢失严重**：窗口内 `static-data-loader` 仅 983 个 ExecutionSample，
  若 15 线程满负荷应约 12000 个（≈8% 捕获率）。因此**只能用相对占比，不能把样本数当 CPU 秒数**；
  可靠绝对值只有事件计时类指标（JIT 16.2 核·秒、GC 3.0 核·秒）。
- 结论：静态数据阶段是"**超订 + 被 JIT/GC/其它启动工作分走 CPU**"，
  单纯加解析线程不会更快（与旧审计"CPU 已饱和、扩容无益"一致）；
  可压缩的头号单项是 **JIT 编译**，其次是 GC 与线程超订本身的调度开销。

## 3. 解析负载归属（9.83 核·秒的 static-data 采样）

| 路径 | 含该帧的样本占比 |
|---|---:|
| `XmlDataLoader.unmarshalShard`（items/NPCs JAXB） | 37.3% |
| `XmlDataLoader.loadStaticDataSection` / `unmarshalSection` | 13.8% / 13.6% |
| `SkillDefinitionLoader.loadPart`（SAX→JAXB） | 10.2% |
| `RetailAiDefinitionLoader.loadPatterns` / `loadMappings` | 7.8% / 5.5% |
| `NpcSkillDefinitionLoader.load` / `NpcDropData.loadEager` | 3.7% / 3.7% |
| `WindstreamDefinitionLoader.parse`（DOM） | 2.6% |

叶子帧里 `xerces XMLEntityScanner.load → UTF8Reader.read` 占 **50.4%**——但下述对照实验证明
**这不是可行动的"解码热点"**。

## 4. 对照实验：把字节流换成已解码 Reader（无收益）

动机：若 50% 采样真在 UTF-8 解码上，交给 `InputStreamReader/BufferedReader` 应显著变快。

| 场景（item 分片 8.3MB，热态中位数） | 墙钟 | 线程 CPU |
|---|---:|---:|
| 现路径 `unmarshal(InputStream)` | 103.6ms | 99.0ms |
| `unmarshal(InputStreamReader)` | 114.1ms | 102.6ms |
| `unmarshal(BufferedReader(64KB))` | 101.4ms | 98.3ms |
| NPC 分片：现路径 / Reader | 107.4ms / 112.8ms | 103.3ms / 104.4ms |

结论：**解码层不是瓶颈**，`UTF8Reader.read` 只是扫描器取数时的采样落点（叶帧偏差）。
该方向否决，不再作为候选。

## 5. 仍然可做、且不改变 XML 实时语义的候选

1. **JVM 启动层（零语义风险，优先）**：JIT 16.2 核·秒是已测出的头号单项成本，
   可用 JDK 26 的 AOT 缓存 / AppCDS（`-XX:AOTCache` / `-XX:SharedArchiveFile`）或编译策略实验；
   这些产物与 XML 无关，不影响 reload。需真实启动对照（未授权、未执行）。
2. **线程池超订收敛（配置级，最便宜）**：`STATIC_DATA_POOL = CPU + NESTED_PARENT_TASKS(5)` = 15 线程，
   与 3 个 quest 编译线程、JIT/GC 一起压在 10 核上。可实验 `NESTED_PARENT_TASKS` 3/4/5 与
   扁平化嵌套 join，观察 parked 父任务与上下文切换的权衡。
3. **缩短关键链**：`npc-ai.xml` mappings→patterns→RetailAiData 组装仍是长链（旧审计：跨度 4.3s+4.3s）。
   可拆分互不依赖段落或提前并行启动，不改变 XML 来源。
4. **减少解析期临时对象**：每次解析分配 43–107MB/文件，对应 GC 3.0 核·秒/8.5s。
   方向：热点大文件（items/NPCs 分片）改手写 StAX 构建（已有 `SkillDefinitionLoader` 先例），
   或减少字符串驻留/中间集合。
5. **同窗口让路（已有先例，可复测）**：quest 目录编译池已从 8 线程降到 3 线程（源码注释记录了
   早前 JFR 结论）；可复测 2/3/4 线程或延后到 geo 窗口，确认当前取值仍最优。

## 6. 复现命令

```bash
JFR=/Users/mc/Library/Java/JavaVirtualMachines/azul-26.0.2.1/Contents/Home/bin/jfr
$JFR print --json --events jdk.ExecutionSample --stack-depth 32 startup-20260824.jfr > /tmp/aion-exec-2143-d32.json
$JFR print --json --events jdk.GCPhaseParallel,jdk.ClassLoad,jdk.Compilation,jdk.ObjectAllocationSample \
	startup-20260824.jfr > /tmp/aion-other-2143.json
$JFR print --json --events jdk.CPULoad startup-20260824.jfr > /tmp/aion-cpuload-2143.json
python3 .agents/summary/startup-perf/jfr-attribute.py /tmp/aion-exec-2143-d32.json /tmp/aion-other-2143.json
```

## 7. 未决

- 未做：AOT/AppCDS 对照实验（需要真实启动，未授权）；`pool-5` 线程数复测；手写 StAX 分片构建的收益评估。
- 本轮未改生产代码、未引入依赖、未提交。
