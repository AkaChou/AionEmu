# 静态数据并发模型：虚拟线程编排 + 固定平台解析池

## 结论

静态数据加载采用混合并发模型，而不是把所有加载任务直接放到虚拟线程：

- 虚拟线程负责父任务、fan-out、join、轻量合并和依赖编排。
- 固定平台线程池负责 XML 令牌化、绑定、数字转换等 CPU 密集解析。
- 不使用仍属于预览特性的 `StructuredTaskScope`。

虚拟线程解决的是父任务等待子任务时占用平台线程的问题，不会增加 CPU 解析吞吐。若把全部
CPU 密集任务直接放进虚拟线程，它们仍会竞争同一组 carrier 线程；用信号量限制并发又容易在
父任务持有许可并等待子任务时形成死锁。因此保留固定解析池更稳妥，也能继续复用
`StreamingXmlBinder` 的 `ThreadLocal<XMLInputFactory>`。

## 执行器

### 虚拟线程编排器

线程名前缀：`static-data-orchestrator-`。

负责：

- `DataManager` 的物品/技能父任务。
- 物品、技能、NPC 模板、NPC 掉落、零售 AI 的扇出与等待。
- 单文件解析任务的提交和结果合并。

### 固定平台解析池

线程名前缀：`static-data-parse-`。

负责：

- 静态数据入口分区的流式绑定。
- 物品/NPC/技能/掉落/AI 分片解析。
- NPC 覆盖层和单文件紧凑定义解析。
- 所有嵌套分片父任务在虚拟线程中执行，解析线程只运行叶子任务。

默认解析线程数为 `min(8, max(2, ceil(3 * CPU / 4)))`：

| CPU | 默认解析线程 |
|---:|---:|
| 4 | 3 |
| 8 | 6 |
| 10 | 8 |

可通过 `-Daion.staticData.parseWorkers=N` 覆盖。分片调度窗口为
`max(4, parseWorkers * 2)`，仍可通过 `-Daion.staticData.shardWindow=N` 覆盖。

## 死锁护栏

`submitLargestFirst` 会在固定解析线程上立即抛出 `IllegalStateException`。这样后续若误把
扇出父任务提交到解析池，不会因为父任务占满全部解析线程并等待自己的子任务而死锁。

## 验证

IDEA 构建通过，以下测试通过：

- `XmlDataLoaderTest`
- `DataManagerTest`
- `SkillDefinitionLoaderTest`
- `RetailAiDefinitionLoaderTest`
- `NpcDropDataTest`
- `NpcTemplateDropLoadingTest`
- `DropModifiersTest`

生产数据 harness `StaticDataLoadRepro server` 在 10 核开发机连续运行四次：

| 次数 | 加载耗时 | 进程 CPU 时间 |
|---:|---:|---:|
| 1 | 10,161 ms | 40,568 ms |
| 2 | 8,588 ms | 39,044 ms |
| 3 | 7,527 ms | 40,259 ms |
| 4（最终复测） | 7,295 ms | 42,255 ms |

三次结果均为 `items=128629`、`npcs=88002`、`skills=14518`，quest 预加载完成且无失败。
开发机为 10 核，4/8 核部署仍需在目标机器上用相同 harness 复测并调整
`aion.staticData.parseWorkers`。
