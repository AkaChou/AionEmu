# 启动优化第二轮：1–5 号候选执行记录（2026-09-19）

> status: 代码改动已落地，聚焦测试通过（52 tests / 0 failures / 0 errors，2026-09-19 22:21）
> 约束：XML 仍是运行时唯一来源，**不引入二进制/紧凑缓存**，reload 语义不变
> 关联：`2026-09-19-static-data-critical-path-attribution.zh-CN.md`（第 5 节即 1–5 号候选）

## 1. 代码改动（本轮）

| # | 改动 | 文件 | 影响面 |
|---|---|---|---|
| 3 | `loadPatterns` 逐文件并行解析，再按文件名顺序合并 | `RetailAiDefinitionLoader.java` | 242 个 `npcaipatterns*.xml`（41MB）从单任务串行转为并行；输出确定性不变（按名排序合并） |
| 2 | 静态数据池可调：`-Daion.staticData.extraLoaders=N`（默认 5） | `XmlDataLoader.java` | 池 = CPU + N，供超订实验；默认行为与现状完全一致 |
| 5 | quest 目录编译池可调：`-Daion.quest.catalogCompileThreads=N`（默认 3） | `QuestDefinitionCatalogManifest.java` | 两处池（`compileEntries` / `compileResourceEntries`）统一取值；默认不变 |
| 4 | item 分片增量合并：新增 `ItemData.assembleFromMerged`，`loadItemData` 边完成边并入 | `ItemData.java`、`XmlDataLoader.java` | 不再同时持有全部分片的索引映射，降低峰值内存/GC 压力；`assembleFrom(List,..)` 保留并委托新方法 |

说明：
- #3 的每个任务自建 `XMLInputFactory`，`compile(Node)` 为无状态静态方法，无共享可变状态；
  异常仍按原路径上抛（`CompletionException` → `Future.join()` 行为与改动前一致）。
- #4 只消除了"分片索引在合并前全量驻留"这一项，**未**触及 JAXB 绑定本身的临时对象；
  手写 StAX 构建（items/NPCs 分片）仍是后续可评选项，属大改动，需单独排期与验收。
- 本轮新增日志：无（未新增 i18n key）。

## 2. #1 JVM 启动层：AOT 缓存实测结论（关键限制）

JDK 26（Zulu 26.0.2.1）实测：

| 场景 | 结果 |
|---|---|
| `-cp /tmp/aotprobe/probe.jar`（单个 jar） | ✅ `-XX:AOTMode=record` → `-XX:AOTMode=create` 生成 10.5MB 缓存，使用运行时 `Using AOT-linked classes: true` |
| `-cp /tmp/aotprobe/classes`（**非空目录**） | ❌ 退出时报 `Error: non-empty directory '/tmp/aotprobe/classes'` / `Cannot have non-empty directory in paths` |

**结论**：AOT/CDS 缓存无法用于本项目的 IDE 启动方式（classpath 含 `target/classes` 非空目录，
JDK 拒绝 dump）；**只对 jar 形态起效**（`target/AionEmu.jar` 或部署目录的 `aion/`）。

jar 形态的三步（待用户执行，需重启服务端）：

```bash
JAVA=/Users/mc/Library/Java/JavaVirtualMachines/azul-26.0.2.1/Contents/Home/bin/java
# 1) 训练运行（记录类加载/链接）
$JAVA -XX:AOTMode=record -XX:AOTConfiguration=/tmp/aion.aotconf -jar target/AionEmu.jar
# 2) 生成缓存
$JAVA -XX:AOTMode=create -XX:AOTConfiguration=/tmp/aion.aotconf -XX:AOTCache=/tmp/aion.aot -jar target/AionEmu.jar
# 3) 之后每次启动使用
$JAVA -XX:AOTCache=/tmp/aion.aot -jar target/AionEmu.jar
```

备选（更省事、收益略低）：`-XX:+AutoCreateSharedArchive -XX:SharedArchiveFile=/tmp/aion.jsa`，
首次启动退出时自动生成，之后自动使用；同样受"非空目录 classpath"限制。

另可供 JIT 侧实验：JIT 编译在静态数据窗口占 **16.2 核·秒**，可用
`-XX:CICompilerCount` / `-XX:TieredStopAtLevel=1` 做对照（后者牺牲稳态性能，需谨慎）。

## 3. 待执行的验证（需授权 / 需用户重启服务端）

1. ~~聚焦测试~~ **已执行（2026-09-19 22:21，用户授权）**：
   ```
   mvn -B -Dstyle.color=never -Dtest=RetailAiDefinitionLoaderTest,RetailConditionSpawnPartyLoaderTest,XmlDataLoaderTest,HotReloadDataTest,DataholderLookupIndexTest,QuestDefinitionCatalogManifestTest test
   ```
   结果：`Tests run: 52, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`（31.6s）。
   覆盖点：pattern 目录解析与优先级编译（`RetailAiDefinitionLoaderTest` 6）、item 分片装配与合并
   （`XmlDataLoaderTest` 24）、热重载（`HotReloadDataTest`）、索引重建（`DataholderLookupIndexTest`）、
   目录清单（`QuestDefinitionCatalogManifestTest` 10）、条件刷新点/队伍（`RetailConditionSpawnPartyLoaderTest`）。
2. A/B 启动矩阵（用户重启，每次记录总启动耗时 + JFR 静态数据窗口）：
   - 基线（无附加参数）
   - `-Daion.staticData.extraLoaders=3`（#2：15 → 13 线程）
   - `-Daion.quest.catalogCompileThreads=2` / `=4`（#5）
   - jar 形态 + `-XX:AOTCache=/tmp/aion.aot`（#1）
3. 预期与判据：静态数据窗口 8.05s、总启动 ~14s 为基线；
   任一组合把静态数据窗口压到 <7s 视为有效，需连续两次复现。

## 4. 回滚

- #2/#5 默认值未变（5 / 3），不传参数即等同改动前。
- #3/#4 为纯执行顺序与峰值内存调整，异常与输出契约不变；如出现回归可直接 revert 对应提交。

## 5. 未决

## 4.1 首次 A/B 结果（IDE 启动，2026-09-19 22:25）

实际生效参数（取自进程命令行）：`-Daion.staticData.extraLoaders=3`、`-Daion.quest.catalogCompileThreads=2/4`。

| 指标 | 基线（21:43） | 本次（22:25，extraLoaders=3） |
|---|---|---|
| 静态数据窗口墙钟 | 8.05s | **7.77s** |
| `static-data-loader` 线程数 | 15 | **13**（参数已生效） |
| static-data 采样数 | 983 | 1025 |
| 窗口内 GC 并行 | 3.0 核·秒 | 2.63 核·秒 |
| 窗口内 JIT | 62 次 / 16.2 核·秒 | 58 次 / 13.4 核·秒 |

结论与注意：

- 窗口 −3.5%，**未达 <7s 判据**，且**不是干净 A/B**：JIT 本身也降了 17%，
  说明两次运行的编译工作量/机器背景负载不同（IDE、其它进程），单次对比不足以定论。
- `-Daion.quest.catalogCompileThreads=2/4` 是无效值（`Integer.getInteger` 解析失败回落默认 3，
  实现按设计容错、未报错）；应写成单值 `=2` 或 `=4`。
- 两次启动日志：`staticDataLifecycle 7393ms`（22:25:26）与 `7843ms`（22:25:57），总启动 13s / 14s。
- 窗口内 JIT 仍有 13.4 核·秒 → **AOT/CDS（jar 形态）是本轮最值得继续的一项**。

### 4.2 第二次 A/B（IDE，22:28，`extraLoaders=3` + `catalogCompileThreads=4`）

参数均生效（`static-data-loader` 13 线程；窗口内 `pool-5-thread-1..4` 四个线程出现）。

| 指标 | 基线 21:43 | 22:25（extra=3，quest 无效→3） | 22:28（extra=3，quest=4） |
|---|---|---|---|
| JFR 静态数据窗口 | 8.05s | 7.77s | **7.29s** |
| 应用自报 `staticDataLifecycle` | 7664ms（20:55） | 7843ms | 7328ms |
| 窗口内 JIT | 62 次 / 16.2 核·秒 | 58 / 13.4 | 49 / **10.2** |
| 窗口内 GC 并行 | 3.0 核·秒 | 2.63 | 2.12 |

判读：JIT 同步下降 37%，说明三次运行之间还有**背景条件差异**（IDE/其它进程、page cache、采样丢失），
单次 7.29s 不足以认定 `<7s` 达标；应用自报值在 7.33–7.84s 间波动（±7%）。
先重复同配置 2–3 次确认可复现性，再进入 jar + AOT 对照。

### 4.3 复现性与当前判读（IDE 启动，截至 22:29）

应用自报 `staticDataLifecycle`（同一台机器、同一份 `target/classes`）：

| 启动时刻 | 配置 | staticDataLifecycle |
|---|---|---:|
| 21:44:00 | 无参数（15 线程） | 8093ms |
| 22:21:55 | 无参数（紧跟 Maven 测试，脏样本） | 9363ms |
| 22:25:32 | `extraLoaders=3` | 7393ms |
| 22:26:03 | `extraLoaders=3` | 7843ms |
| 22:28:05 | `extraLoaders=3` + `catalogCompileThreads=4` | 7328ms |
| 22:29:37 | 同上（重复） | 7730ms |

- 带参数四次中位数 **7.56s**，无参数两次中位数 **8.73s**（但无参数样本仅 2 次，其中一次为脏样本）。
- 同配置重复的极差 400ms（±5%），因此**单次结果不可用于定论**；
  若要确认约 −13% 的差异，需在同一时段补 2 次"无参数"对照。
- 参数已确认生效：`static-data-loader` 13 线程（基线 15）、窗口内 `pool-5-thread-1..4` 四个线程。

### 4.4 jar 构建（2026-09-19 22:40）

`mvn -B -DskipTests package` → `target/AionEmu.jar`（57MB，Spring Boot repackage，含本轮改动）。
AOT 训练/生成/使用三步见第 2 节；训练与使用需要正常启动一次服务端（由用户执行）。
`aion/` 部署目录当前只有 config/data/definitions/geo/log，还没有 jar，需要时用 `scripts/package.sh` 重新部署。

- 手写 StAX 分片构建（#4 的深水区）未排期。
- `npc-ai.xml` mappings→patterns 之外的其它长链（static_data 分区、npc_drops）未重新量测。
- 本轮未编译、未测试、未提交。

## 6. npc-ai.xml 关键链拆分（替换单体 + 保留脚本，2026-09-19）

按用户决定执行："替换单体文件，保留切分脚本"。

### 6.1 数据形态

- `npc-ai.xml`（27MB，87721 条 `<npc/>`）→ `definitions/compact/ai/npc-ai-parts/` 下 8 个按 **NPC ID 区间**命名的分片
  （`npc-ai_200000_216003.xml` … `npc-ai_834041_885645.xml`，每片约 3.2–3.6MB / 约 1.1 万条），
  **仍是 XML 源**：临时改 XML + reload 的工作流不变；文件名与 items/NPC 模板分片约定一致，可按 ID 定位。
- 单体文件已删除；脚本保留在 `scripts/split_npc_ai.py`
  （`--shards N --in FILE --out DIR`；重新切分时用
  `git show HEAD:src/main/resources/aion/definitions/compact/ai/npc-ai.xml > /tmp/npc-ai.xml` 取回单体）。
- 分片规则：保留 XML 声明 + 根元素，内容为连续 `<npc/>` 行，末尾补根闭合；
  加载侧按文件名里的**起始 NPC ID 数值升序**合并（不是字符串排序），与原始元素顺序一致。

### 6.2 加载侧改动

| 文件 | 改动 |
|---|---|
| `RetailAiDefinitionLoader` | `loadMappings(File)` 支持"目录或单体文件"：目录 → 各分片（`npc-ai_<起始ID>_<结束ID>.xml`）并行扫描后按起始 ID 升序合并；单体 → 原单遍扫描（`scanMappings`）。两个产出（NPC 映射、寻路行为）语义与单体一致 |
| `XmlDataLoader` | `NPC_PATH_BEHAVIOR_FILE` / `RETAIL_NPC_AI_MAPPINGS_FILE` 指向 `npc-ai-parts` 目录；两个消费方仍共用一次合并扫描（局部 Future） |
| `Quest14026GeranaiaSpawnTest` | 跨分片汇总目标 NPC 定义（原先 DOM 解析单体文件） |
| `RetailAiDefinitionLoaderTest` | 生产目录引用改为 `npc-ai-parts` |

### 6.3 验证（2026-09-19 23:0x）

```
mvn -B -Dstyle.color=never -Dtest=RetailAiDefinitionLoaderTest,NpcPathBehaviorDefinitionLoaderTest,\
RetailConditionSpawnPartyLoaderTest,Quest14026GeranaiaSpawnTest,XmlDataLoaderTest,\
RetailOpenWorldSpawnDataTest,NpcDropDataTest test
```
结果：`Tests run: 50, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`。
8 个分片均通过 XML well-formed 校验，npc 总数 87721（与单体一致）。

### 6.4 待复核与回滚

- 收益需下一次真实启动用 JFR 复核（预期压缩静态数据窗口内的 mappings 段；窗口为 7.3–7.8s 量级）。
- 回滚：把两个常量指回单体路径，并从 git 历史恢复 `npc-ai.xml` 即可（单体路径仍在代码中支持）。
- 未提交。

### 6.5 真实启动 + JFR 复核（2026-09-19 23:02，分片版）

启动命令（等价 IDE classpath，未加任何 `-Daion` 调参）：
`java -XX:StartFlightRecording=...,settings=profile -cp target/classes:<runtime cp> com.aionemu.AionBootApplication`

| 指标 | 基线 21:43（单体，15 线程） | 23:02（分片 + patterns 并行） |
|---|---|---|
| 静态数据窗口（JFR） | 8.05s | **7.44s（−7.6%）** |
| `staticDataLifecycle` | 8093ms | **7514ms** |
| 静态数据解析 | — | 7281ms |
| 启动错误数 | — | **0** |
| static-data 采样 | 983 | 773（≈−21%，采样有损，仅供参考） |

并行化证据（含该帧的采样线程数）：

| 路径 | 基线 | 23:02 |
|---|---|---|
| `RetailAiDefinitionLoader.scanMappings`（原单体扫描） | 1 线程 / 5.5% | **5 线程** / 3.8%（+0.3% 外层） |
| `RetailAiDefinitionLoader.loadPatterns` | 1 线程 / 7.8% | **11 线程** / 8.5% |
| `XmlDataLoader.unmarshalShard` | 15 线程 / 37.3% | 10 线程 / 23.3% |

结论：npc-ai 关键链已从"单体单线程扫描 + 单线程 patterns"变为**分片并行（5 线程）+ patterns 并行（11 线程）**，
真实启动无错误、窗口缩短约 0.6s；未加调参即达到此前带调参的水平。
