# 静态数据 XML 解析器探针结果（2026-09-19）

> status: 已按授权运行，结论 = **不采用解析器替换**（候选 2 否决）
> evidence: `xml-parser-probe/results-20260919-215023.txt`（warmup=2 / 5 轮，热态）
>          `xml-parser-probe/results-20260919-215104.txt`（warmup=0 / 3 轮，冷启动曲线）
> 探针: `xml-parser-probe/XmlParserProbe.java` + `run-probe.sh`
> 环境: Zulu JDK 26.0.2.1（aarch64，10 核），Woodstox 7.1.1 + stax2-api 4.2.2，无服务端进程参与

## 1. 结论

1. **收益不达标**：热态线程 CPU 中位数最优 −16%（NPC 分片），最差 **+8% 变慢**（`npc-ai.xml` StAX），
   item 分片仅 −5%，远低于"≥20–30% 才值得换"的门槛。
2. **必然改代码**：`SkillDefinitionLoader.createPartReader()` 在 Woodstox 下实测抛
   `SAXNotRecognizedException: Feature 'http://apache.org/xml/features/disallow-doctype-decl' not recognized`，
   而该 feature 是防 XXE 的安全设置，不能简单删掉。
3. **更重要的量级问题**：同一生产方法、同一文件，探针里 27MB 的 `loadMappings` 只要 168ms（热）/ 225ms（冷），
   而生产启动 JFR 中同一方法跨 4.32s。**约 20× 的差距不来自解析器吞吐**，解析器替换无法消除它。

## 2. 热态对比（中位数；`墙钟 / 线程 CPU`，单位 ms）

| 场景 | 数据量 | JDK 内置 | Woodstox 7.1.1 | 差异（CPU） |
|---|---:|---:|---:|---:|
| `jaxb-item`（`unmarshalShard` 等价路径） | 8.3MB | 101.1 / 99.1 | 141.9 / 94.2 | **−5%** |
| `jaxb-npc`（同上，`NpcData`） | 9.4MB | 101.8 / 101.1 | 92.4 / 85.0 | **−16%** |
| `stax-mappings`（生产 `loadMappings`） | 27.4MB | 168.2 / 165.5 | 191.8 / 178.6 | **+8%（变慢）** |
| `sax-skill-part`（生产 `createPartReader()` + SAX 扫描） | 1.2MB | 11.9 / 11.7 | 6.9 / 6.1（降级 reader） | 见 §4，不可比 |

- 属性切换均已生效：`impl.sax=com.ctc.wstx.sax.WstxSAXParserFactory`、
  `impl.stax=com.ctc.wstx.stax.WstxInputFactory`。
- 同进程交替对照（item 分片，显式构造 XMLReader，排除属性机制差异）：
  JDK 114.2 / 105.8 vs Woodstox 93.4 / 82.1 → 墙钟 −18%、CPU −22%。
  **这是唯一出现明显正收益的组合，但与属性路径（同一场景 −5%）不一致**，不足以支撑决策。
- 每次解析分配量：item 46MB、NPC 43MB（8–9MB XML），`npc-ai.xml` 107MB（27MB XML）；
  Woodstox 分配量普遍高 5–7MB。

## 3. 冷启动曲线（warmup=0，同一 JVM 连续 3 轮墙钟 ms）

| 场景 | JDK 第1/2/3 轮 | Woodstox 第1/2/3 轮 |
|---|---|---|
| `jaxb-item` | 336 / 114 / 96 | 248 / 101 / 75 |
| `jaxb-npc` | 352 / 110 / 100 | 247 / 85 / 73 |
| `stax-mappings` | 224 / 171 / 152 | 226 / 173 / 150 |

- 首轮比稳定态慢 2–3×（JIT + 类加载），Woodstox 在 JAXB 路径首轮快 25–30%，StAX 路径完全无差异。
- 注意：这不是"生产全冷"——`JAXBContext` 在测量前已创建（与生产 `sharedJaxbContext` 一致）；
  生产解析前的类加载/JIT 状态更差。

## 4. 兼容性实测（比性能更重要）

| 检查项 | 结果 |
|---|---|
| JAXB 直读路径被 `-Djavax.xml.parsers.SAXParserFactory` 切换 | ✅ 生效（JAXB 内部走 `SAXParserFactory.newInstance()`） |
| StAX 路径被 `-Djavax.xml.stream.XMLInputFactory` 切换 | ✅ 生效 |
| `SkillDefinitionLoader.createPartReader()` | ❌ `SAXNotRecognizedException`（`disallow-doctype-decl` 为 Xerces 专有 feature） |
| `setXIncludeAware(false)` | ✅ 无影响（JDK 基类仅 `true` 时抛异常） |
| `UnmarshallerImpl` 的 `string-interning` 探测 | ✅ 无影响（JAXB 已捕获 `SAXException`） |

结论：只有"JAXB 直读 + StAX"两条路径能纯靠依赖+启动参数替换；技能分卷 SAX 路径若替换必须
改成 feature 探测/条件设置，或该路径继续用 Xerces。

## 5. 为什么这否定了候选 2

| 观察 | 数值 |
|---|---|
| 探针内 `loadMappings`（27MB，真实生产方法） | 168ms 热 / 225ms 冷 |
| 生产启动 JFR 中同一方法跨度 | 4.32s |
| 探针内 item 分片 JAXB | 101ms 热 / 336ms 冷（首轮） |

同一文件、同一方法相差约 20×，说明启动期该线程的时间主要花在**冷执行/JIT/去优化、线程竞争与
JFR 采样开销**上，而不是解析器吞吐。换解析器既不能减少冷执行路径，也不能减少竞争。

### 5.1 追加对照：Reader 化（无收益，2026-09-19 追加）

JFR 归因显示静态数据采样中 `xerces XMLEntityScanner.load → UTF8Reader.read` 占 50.4%，
故追加实验：把字节流换成已解码的 `InputStreamReader` / `BufferedReader(64KB)` 再交给 JAXB。

| item 分片（8.3MB，热态中位数） | 墙钟 | 线程 CPU |
|---|---:|---:|
| 现路径 `unmarshal(InputStream)` | 103.6ms | 99.0ms |
| `unmarshal(InputStreamReader)` | 114.1ms | 102.6ms |
| `unmarshal(BufferedReader 64KB)` | 101.4ms | 98.3ms |

NPC 分片同为无收益（107.4ms → 112.8ms）。结论：**UTF-8 解码层不是瓶颈**，
50.4% 只是扫描器取数时的叶帧采样偏差；该方向否决。
探针已新增 `jaxb-item-reader` / `jaxb-item-buffered-reader` / `jaxb-npc-reader` 场景以便复现。

## 6. 对候选 1（构建期紧凑/二进制缓存）的影响

缓存方案的价值需要重新表述：省掉的不只是"解析吞吐"，而是

- JAXB 绑定与上下文构建（每类型一次，且是冷执行）；
- 每次解析 43–107MB 的临时对象（探针实测分配量），对应 GC 并行阶段 CPU
  （45s 录制内 `jdk.GCPhaseParallel` 合计约 5.9 核·秒；STW 暂停合计仅 622ms）；
- 大量 XML 字符串/字节数组的冷执行路径。

因此候选 1 仍是最优方向，但收益应以"启动墙钟 + 分配量"验证，而不是以解析器基准推断。

## 7. 未决 / 下一步

- 未做：Aalto 对照（本地仓库无 `aalto-xml`，需联网下载）；DOM（`WindstreamDefinitionLoader`）无对照价值。
- 建议下一步：先补一次 **静态数据阶段的定向 JFR 归因**（JAXB 上下文创建、对象图组装、GC、冷执行各占多少），
  再决定候选 1 的缓存形态（整对象图序列化 vs 紧凑中间格式）。
- 本轮未改生产代码、未引入依赖、未提交。
