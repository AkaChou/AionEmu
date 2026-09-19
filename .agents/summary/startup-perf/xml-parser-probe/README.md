# 静态数据 XML 解析器探针 / Static-data XML parser probe

> status: **已运行**（2026-09-19 21:50，用户授权）。
> 结果与结论：`../2026-09-19-xml-parser-probe-results.zh-CN.md` —— **候选 2（解析器替换）否决**。
> 无 `pom.xml` 改动、无依赖引入、不启动服务端、不写生产代码。
> 关联审计：`../2026-09-19-static-data-cpu-bound-audit.zh-CN.md`（候选 2）。

## 0. 运行结果速览（2026-09-19）

- 热态线程 CPU：item −5%、NPC −16%、`npc-ai.xml` StAX **+8%（变慢）** → 未达 ≥20–30% 门槛。
- `SkillDefinitionLoader.createPartReader()` 在 Woodstox 下实测抛
  `SAXNotRecognizedException`（`disallow-doctype-decl` 为 Xerces 专有）→ 换解析器必须改代码。
- 同方法同文件：探针 168ms（热）/ 225ms（冷） vs 生产启动 JFR 跨度 4.32s → 启动成本大头不是解析器吞吐。
- 报告：`results-20260919-215023.txt`（热态）、`results-20260919-215104.txt`（冷启动曲线）。

## 1. 目的

用真实数据文件量化"换 XML 解析器"能否削减静态数据加载 CPU：同一文件、同一目标类型，
在 JDK 内置解析器与 Woodstox 7.1.1 之间比较墙钟、CPU 与分配。

关键设计：多数场景**通过 JVM 系统属性切换解析器**，因为生产代码走的正是 JAXP 查找路径
（`SAXParserFactory.newInstance()` / `XMLInputFactory.newFactory()`）。这样测出的结论可以直接回答
"只加依赖 + 加启动参数是否可行"，而不只是"换解析器理论上更快"。

## 2. 前置：生成运行时 classpath（需授权）

```bash
mvn -B -q dependency:build-classpath -Dmdep.outputFile=/tmp/aion-runtime-cp.txt -DincludeScope=runtime
```

- `maven-dependency-plugin` 3.10.0 已在本地仓库，可加 `-o` 离线执行。
- 该命令只解析依赖、不编译、不跑测试；但按 AGENTS.md 仍先请求授权。

## 3. 运行

```bash
bash .agents/summary/startup-perf/xml-parser-probe/run-probe.sh
# 可选：WARMUP=2 ITERATIONS=5 / CP_FILE=... / JAVA=... 环境变量覆盖
```

探针用源码模式启动（`java -cp <cp> XmlParserProbe.java ...`），不产出仓库内 class 文件。
报告写入本目录 `results-YYYYMMDD-HHMMSS.txt`（运行产物，未纳入版本控制）。

## 4. 场景

| 场景 | 数据 | 被测代码路径 | 切换方式 |
|---|---|---|---|
| `jaxb-item` | item 分片 8.3MB | 镜像 `XmlDataLoader.unmarshalShard`（共享 JAXBContext + 字节流直读） | `javax.xml.parsers.SAXParserFactory` |
| `jaxb-npc` | NPC 分片 9.4MB | 同上（`NpcData`） | 同上 |
| `stax-mappings` | `npc-ai.xml` 26MB | 反射调用生产方法 `RetailAiDefinitionLoader.loadMappings(File)` | `javax.xml.stream.XMLInputFactory` |
| `sax-skill-part` | skill part 1.1MB | 反射调用生产方法 `SkillDefinitionLoader.createPartReader()` + SAX 扫描 | `javax.xml.parsers.SAXParserFactory` |
| `jaxb-item-explicit` | item 分片 8.3MB | JAXB + 显式构造的 XMLReader，**同进程交替 A/B** | 显式 `WstxSAXParserFactory` |

未覆盖：`WindstreamDefinitionLoader` 的 DOM 路径（Woodstox 不提供 `DocumentBuilderFactory`，无对照价值）；
npc_drops 走与 `jaxb-item` 相同的 JAXB 直读路径，未单列。

## 5. 输出与判读

每轮输出一行 `RESULT`（中位数/最小值/所有轮次墙钟）与解析器实现类名，用于确认属性切换是否生效：

- 判读门槛：CPU 中位数下降 **≥20–30%** 才值得推进解析器替换；否则转候选 1（构建期二进制缓存）。
- `sax-skill-part` 会额外打印 `feature_compat=`：
  - `ok`：生产 feature 集（含 `disallow-doctype-decl`）被该解析器接受；
  - `fail`：抛异常，表示**必须改代码**（做特性探测或条件化设置），不能只换依赖。
  - `fail` 时会退回"仅命名空间感知"的 reader 继续测吞吐，仍可与 JDK 对照。
- 单轮解析会丢弃结果对象，迭代间调用 `System.gc()`；两个变体各用独立 JVM、同样的预热轮次。

## 6. 静态预判（javap 证据，2026-09-19）

| 路径 | 结论 | 证据 |
|---|---|---|
| JAXB 直读分片 | ✅ 可被系统属性切换 | `JAXBContextImpl.getSAXParserFactory()` → `jaxb-core XmlFactory.createParserFactory` → `SAXParserFactory.newInstance()`，只设置 `namespaceAware` + `secure-processing` |
| StAX 扫描（`npc-ai.xml` 等） | ✅ 可被系统属性切换 | `XMLInputFactory.newFactory()` 受 `javax.xml.stream.XMLInputFactory` 控制；`SUPPORT_DTD`、`isSupportingExternalEntities` 在 Woodstox `ReaderConfig`/`StreamScanner` 中存在 |
| `SkillDefinitionLoader.createPartReader()` | ❌ 需改代码 | 设置 Xerces 专有 `http://apache.org/xml/features/disallow-doctype-decl`；Woodstox 7.1.1 jar 内无该字符串，`SAXFeature.findByUri` 会不识别 |
| `setXIncludeAware(false)` | ✅ 不阻塞 | JDK `SAXParserFactory.setXIncludeAware` 仅在 `true` 时抛 `UnsupportedOperationException` |
| `UnmarshallerImpl` 的 `string-interning` 探测 | ✅ 不阻塞 | `needsInterning()` 用 `http://xml.org/sax/features/string-interning`（Woodstox `SAXFeature.STRING_INTERNING` 存在）且已捕获 `SAXException` |

结论：**JAXB/StAX 路径具备"只加依赖 + 启动参数"的可行性；SAX 技能分卷路径需要同时改代码。**

## 7. 注意

- 探针只读数据文件；不要用它启动服务端，也不要让它写回 `src/main/resources`。
- 生成的 `results-*.txt` 是中间产物，保留在 `.agents/summary/` 下，不提交。
