# 启动速度优化

> 目标:把游戏服务器启动时间从 **36s** 降到 **~16s**。
> 方法:对比同源项目 `aion-server`(4.8 版本,启动 10s),借鉴其零依赖优化。

## 现状基线

实测启动总耗时 **36 秒**(`GameStartupSequenceLifecycle` 末尾汇总日志),三大头占 95%:

| 阶段 | 耗时 | 占比 | 根因 |
|---|---|---|---|
| `staticDataLifecycle` | 21.8s | 60% | JAXB 单线程解析 **239MB** `static_data.xml`(`cache/static_data.xml`) |
| `geoNavLifecycle` | 8.9s | 25% | 单线程读 **192MB** `meshs.geo` + 同步构建每个 mesh 的 BIH 碰撞树 |
| `spawnLifecycle` | 3.7s | 10% | 单线程生成 92810 Npc + 19354 Gatherable |
| 其余 30 项 | ~1.5s | 5% | 噪音,不处理 |

更细:`staticData` 21.8s = merge 0.3s + unmarshal 17.1s + 后处理/索引 ~4.4s。其中 unmarshal 17.1s **包含了** unmarshal 前的一次 239MB StAX 预扫描(只为数进度条)。

## 对比 aion-server(10s)的根因

aion-server 是 AionEmu 同源的 4.8 版本,启动 10s。unmarshal 本体两边完全一致(都是标准 `u.unmarshal(reader)`,无黑科技)。差距来自 aion-server 把若干开销**挪出了关键路径**,而这些在 AionEmu 里是同步阻塞的:

| 维度 | AionEmu 5.8 | aion-server 4.8 | 可借鉴 |
|---|---|---|---|
| 数据量 | 239MB | 150MB | ❌ 版本差异,不可控 |
| JAXBContext 初始化 | unmarshal 前**同步**建(`XmlDataLoader:137`) | 启动伊始 `preLoadContextAsync` **异步预热**(`GameServer:93`) | ✅ 零依赖 |
| 进度条预扫描 | unmarshal 前 StAX 扫一遍 239MB(`XmlDataLoader:106`) | **不做** | ✅ 零依赖 |
| geo 碰撞树构建 | loadMeshs 里**同步**每 mesh 构建(`GeoWorldLoader:113`) | 加载完**后台 parallelStream** 构建(`GeoWorldLoader:46`) | ✅ 零依赖 |
| geo mesh 读取 | **逐 float 循环**读 192MB(`GeoWorldLoader:94`) | **slice 零拷贝**视图(`GeoWorldLoader:130`) | ✅ 零依赖 |
| 脚本编译 | 每次启动重编 | `cache/classes/` 2043 个预编译 .class | ⚠️ AionEmu engines 已并行仅 748ms,收益小,暂不做 |

关键安全前提:`Mesh.collideWith`(`scene/Mesh.java:337-338`)有**懒加载兜底**——`collisionTree==null` 时首次碰撞自动构建。所以 geo 的同步构建可安全挪后台,功能不依赖预构建,预构建只为消除运行时首次碰撞的卡顿。

## 优化清单(按性价比排序)

全部零依赖,#1~#4 已被 aion-server 同源验证。

### [ ] #1 geo `createCollisionData` 异步后台化
- **借鉴**:aion-server `GeoWorldLoader.java:46`
- **落点**:
  - `geoEngine/GeoWorldLoader.java:113` 删除 `m.createCollisionData()`
  - `world/geo/RealGeoData.java` `loadGeoMaps()` 在 `loadWorldMaps` 完成后、`models.clear()` 前,fire-and-forget 后台任务,从 `models` 递归收集所有 `Mesh`,`parallelStream().forEach(Mesh::createCollisionData)`
- **预期**:省 4~5s(geo 8.9s → ~3~4s)
- **风险**:后台构建完成前(启动后几秒)若有碰撞检测,`collideWith` 懒加载兜底,仅首次微慢;启动阶段无玩家,实测无影响
- **权衡标注**:`// ponytail: 碰撞树后台预构建,启动不阻塞;collideWith 懒加载兜底`

### [ ] #2 去掉 staticData `sectionEntryCounts` 预扫描
- **借鉴**:aion-server 根本不做进度条计数
- **落点**:`dataholders/loadingutils/XmlDataLoader.java:106`。改为不确定进度,或把 counts 缓存到 sidecar(`static_data.counts`),仅 cache 重建时重算
- **预期**:省 4~6s
- **风险**:进度条不再显示每段条目数/百分比,只显示段落名(可接受)

### [ ] #3 异步预热 JAXBContext
- **借鉴**:aion-server `GameServer.java:93` + `utils/xml/JAXBUtil.java:29` `preLoadContextAsync`
- **落点**:
  - `lifecycle/GameStartupSequenceLifecycle.java:49` `start()` 最早期(systemProperties 之后)异步触发 `JAXBContext.newInstance(StaticData.class)`,存入 holder
  - `XmlDataLoader.createStaticDataUnmarshaller:137` 改用预热的 context(`future.get()`)
- **预期**:省 2~4s(context 初始化与 logging/config/utilityServices 并行)
- **风险**:低。context 是线程安全的,可异步构建后供 unmarshal 线程使用

### [~] #4 geo loadMeshes 批量读取 (实测后回退，详见进度区)
- **借鉴**:aion-server `GeoWorldLoader.java:130` 的 slice 思路
- **落点**:`geoEngine/GeoWorldLoader.java` 逐 `geo.getFloat()`/`getShort()` 循环,改为 `geo.slice().asFloatBuffer().get(float[])` 批量读 + `vertices.put(float[])` 批量写
- **为何不用零拷贝**:AionEmu 用 `Arena.ofConfined()`/MemorySegment(try 关闭即失效),而 aion-server 用 `MappedByteBuffer`(channel 关闭后仍有效)。mesh 不能持有 mmap 视图,故用中间 `float[]`/`short[]` 批量拷贝,值层面传递保持原 BIG_ENDIAN 编码不变
- **预期**:省 1~3s(取决于 JIT 对 getFloat 循环的原有优化程度)
- **风险**:中。顶点/索引字节序正确性必须实测碰撞验证

### [~] #5 spawn 并行化 (经核查放弃)
- **借鉴**:原创(aion-server 也没做)
- **落点**:`spawnengine/SpawnEngine.java:240` `spawnAll`
- **放弃原因(2026-06-30 核查)**:spawn 路径多处非线程安全,并行化需改造核心代码,风险 ≫ 2~3s 收益:
  - `RiftManager.riftGroups` 是 `HashMap`(`:53`),`addRiftSpawnTemplate` 多线程 put 不安全
  - `World.storeObject/setPosition/spawn` 操作核心 World 集合,未为并发设计
  - `VisibleObjectSpawner.RatedTemplate.ORIGINAL_STATS` check-then-act(`:164`),多线程重复 clone
  - `WalkerFormationsCache` + 多个 static manager 均非并发
  - aion-server 同源未并行 spawn,印证该代码家族不为并发 spawn 设计
- **结论**:数据竞争会导致 NPC 丢失/世界损坏/偶发崩溃,不值得为最小头(3.7s)冒险

## 预期总账

| | staticData | geo | spawn | 总启动 |
|---|---|---|---|---|
| 基线 | 21.8s | 8.9s | 3.7s | **36s** |
| 实测(#1+#2+#3,#4 回退) | 18.6s | 2.2s | 3.5s | **26s** |
| +Fast Infoset(#6) | 回退 | — | — | — |

> 实测累计省 10s(-28%):#1 geo -6.7s、#2 staticData 预扫描 -3.4s。
> #6 Fast Infoset 实测失败回退:FI 编码器对 >1MB 文本节点硬限制(`Integer > 1,048,576`),static_data 含超大 quest 脚本/HTML 触顶,格式与数据根本不兼容。
> spawn(3.5s)经核查线程安全风险过高,保留串行。

## 验证方法

每步改完:
1. `mvn -q -o compile` 编译
2. IDE 跑 `AionBootApplication`(或 `./maven-package.sh` + `./start-silent.sh`)
3. 看日志末尾 `Startup phase timings` 汇总,对比各阶段耗时变化
4. 不要加 `-c`(`start-silent.sh -c` 清缓存变冷启动,不代表日常)

## 进度

- [x] #1 geo createCollisionData 异步后台化 (实测：geo 8880→2188ms，省 6.7s ✅)
- [x] #2 去掉 staticData 预扫描 (实测：staticData 22089→18589ms，省 3.2s ✅)
- [x] #3 异步预热 JAXBContext (编译+测试通过；实测 context 创建非大头，收益微弱但无害)
- [~] #4 geo loadMeshes 批量读取 (实测后回退：slice order 陷阱 + 异常类型破坏 AbyssCore fallback)
- [~] #5 spawn 并行化 (经核查放弃：线程安全风险≫收益)
- [~] #6 Fast Infoset 二进制缓存 (实测回退：FI 对 >1MB 文本节点硬限制，与数据不兼容)
