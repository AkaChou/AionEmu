# P0–P2 改造后 JFR 量化记录 / JFR after the P0–P2 refactor

日期 / Date: 2026-09-15 · 提交 / Commit: `de4a66e20` · 分支 / Branch: `quest`
采集来源 / Source: IDEA 运行配置 `-XX:StartFlightRecording=name=AionStartup,settings=profile,duration=45s,filename=<临时文件>.jfr`
（JFR 为一次性采样产物：文件名由本地运行配置指定，每次启动覆盖，不长期保留、不入库，因此本文不记录具体文件名与路径。）
窗口 / Window: 19:13:35 – 19:14:20（45s，覆盖启动 + 静态数据 + 刷怪；客户端 19:14:22 登录，**不在窗口内**）

## 启动阶段耗时 / Startup phases

| 阶段 | 08-23 P7 基线 | **09-15 de4a66e20** |
|---|---|---|
| staticDataLifecycle | 8257 | 11857 |
| spawnLifecycle | 2803 | 2790 |
| geoPathLifecycle | — | 2786 |
| enginesLifecycle | 962 | 469 |

说明：`staticDataLifecycle` 高于 08-23 记录，但本次改造未触碰静态数据解析路径；该阶段由 Xerces/JAXB 解析 + quest typed 编译构成，且 08-23 之后 quest 目录与 AI 定义数据持续增长（全量测试里 Retail AI/quest 计数断言也随之漂移）。**不能据此判定性能回归**，需要同一份数据的 A/B 才能定论。

## GC / 分配 / 竞争 / CPU

- GC：45s 窗口内 **33 次暂停、合计 708ms**（G1New 为主，最长单次 9.88ms）。与 08-23 记录（静态数据窗口 726ms）同量级 → 无 GC 回归。
- CPU 热点：`Xerces UTF8Reader.read` 37.2%、PNG 解码 3.5%、geo `BoundingBox/BIHTree` ~5%、JAXB unmarshaller ~1%。**热点全部在解析与 geo，与 AR-001 结论一致**。
- 锁竞争（contention-by-site）：**未出现 `DefaultSingletonBeanRegistry.getSingleton`**（08-23 JFR 的成片停车点，P0/P5 的整治目标）。剩余站点均为一次性启动开销：`ZipFile.getEntry`、`QuestDefinitionXmlCompiler.parseAction`、`JAXBContextImpl.build`、`BuiltinClassLoader.loadClassOrNull`，以及并行刷怪的 `World.addVisibleObject`（5 次、均 19.6ms）。
- 分配压力（allocation-by-class）：`byte[]` 15.9%、`Object[]` 6.7%、`String` 6.6%、`ThreadLocalMap$Entry` 3.6%、`Point2D` 3.1%、`ArrayList` 2.4%。站点前三为 `HeapByteBuffer.<init>` 4.2%（读文件/解压）、`XMLString.toString` 4.0%、`StringUTF16.compress` 3.6%；游戏侧唯一位列前五的是 `MathUtil.getClosestPointOnSegment` 3.0%。

## 能证明什么 / 不能证明什么

- ✅ 证明：高频 Spring 门面查容器不再是竞争源（P0/P5 目标达成）；GC 无回归；启动期热点仍由解析/geo 主导。
- ⚠️ 未能证明：P1 的运行期收益（视野广播、封包处理、移动同步）——**该窗口不覆盖游戏内**。
- ⚠️ 口径修正：`KnownList` 的“广播零垃圾”**没有落地**。容器换成 `ConcurrentHashMap` 后，遍历仍必须走 `synchronized(map)` 快照（`KnownListIterationSafetyTest` 静态闸门 + `KnownListTest` 快照语义），因此每次广播仍分配 `ArrayList`。要再降分配只能换其它手段（对象池/复用缓冲/按需视图），且必须先满足闸门测试。
- ⚠️ 新增开销（已知且可接受）：直连通道每帧一次 `ServiceContext.use(target)`（`pumpWrites`），即一次 ThreadLocal set/restore；该链路只有认证/ping/账号级流量，`ThreadLocalMap$Entry` 3.6% 主要来自启动期线程池包装，不是它。

## 复现命令 / Reproduce

```bash
JFR=/Users/mc/Library/Java/JavaVirtualMachines/azul-26.0.2.1/Contents/Home/bin/jfr
JFR_FILE=/tmp/startup.jfr   # 本地临时采样文件：按运行配置的 filename 指定，每次启动覆盖，不入库
$JFR summary "$JFR_FILE"
$JFR view --width 130 hot-methods "$JFR_FILE"
$JFR view contention-by-site "$JFR_FILE"
$JFR view --width 120 allocation-by-class "$JFR_FILE"
$JFR view gc "$JFR_FILE"
grep "GameStartupSequenceLifecycle" log/console.log | tail -30
```

## 下一步 / Next

在游戏内另录一段 JFR（建议 5 分钟，`jcmd <pid> JFR.start duration=300s filename=play.jfr`），再按同样四个视图分析 `KnownList.doOnAll*`、`MapRegion`、`PacketProcessor`、`AConnection.writeData` 与 Spring 门面在**真实玩法**下的热点/分配/竞争。注意启动期 JFR 是临时产物，每次启动都会被覆盖，需要留档时应另存文件名。

> 后续进展 / Follow-up：游戏内窗口已完成，见 [2026-09-15-gameplay-jfr-hotspots.md](2026-09-15-gameplay-jfr-hotspots.md) —— 结论是热路径锁竞争为 0、本次改造的类全部不在热点，真正的瓶颈是 geo 寻路的装箱分配；该文档同时记录了已实施的 `RealGeoData` / `PathData` 优化与 `NODE_LIMIT` 诊断。
