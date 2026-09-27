# P0f：生产任务归属逐 ID 对账与缺失定义阻断

> 2026-09-24；状态：`FOCUSED_TEST_PASSED / T1_PASSED / T3_FAILED_NEEDS_TRIAGE / PENDING_CLIENT`。本切片聚焦门禁与 T1 已通过；T3 已运行但未通过。以下 T1/T3 数字来自各自的仓外工作树快照，不代表当前共享工作树，也不等于服务器启动或真实客户端验收。

## 根因与边界

- 改造前的 `RetailQuestDriver.overlay` 在开关关闭或真端资源加载失败时直接返回 XML 目录；退役后的 XML 已不存在，因此回退可能在启动成功的同时丢失任务。
- 本轮只读盘点：保留清单 6224 行（`RETAIL_TABLE` 3802、`XML_RETENTION` 2422），生产 XML 目录及 catalog 各 2422 项。该数字是审查时的工作区快照，不能当作并发工作的永久基线。
- 审查期间其他切片又改动了 owner 清单，曾读到 3872/2352 而 XML 目录仍为 2422；本轮没有触碰这些共享数据。严格护栏会拒绝这种未对齐的中间态，授权验证前须等待并发批次完成并重新对账。
- 聚焦门禁验证时主工作树及复制快照已对齐：清单 6224 行（`RETAIL_TABLE` 3872、`XML_RETENTION` 2352），生产 XML 目录及 catalog 各 2352 项；三者 ID 集合零差集。前述 3802/2422 是更早快照，不是该次 11 例聚焦验证输入。
- 不擅自恢复已经退役的 XML，也不将运行时开关宣称为“回到旧版本”。若必须做到旧版行为，需要另行决定旧版资源包或旧产物回滚策略。

## 已改动（聚焦门禁已验证）

1. `RetailQuestDriver.overlayProduction` 成为严格的生产入口：真端资源加载异常保留原异常作为 cause，禁止悄悄返回残缺 XML；读取同一份 owner 保留清单，对 6224 个任务逐 ID 核对最终目录。真端 owner 要求可执行且不与 XML 重叠，XML 保留 owner 要求来源未被替换；开关关闭只在旧 XML 目录完整时放行。真端表/索引经同步懒装载只构建一次，避免预加载线程和测试入口重复装载。
2. `QuestEngine.loadProductionCatalog` 与测试用的 `ProductionQuestDefinitions.catalog` 改用严格入口。宽松 `overlay` 仅给局部目录/兼容测试使用，不再由生产装载链调用。
3. `RetailQuestDriverOverlayTest` 新增开关关闭后缺少 XML 必须失败、生产目录精确覆盖、移除一条真端 owner 必须失败的断言；旧的 `>250` 宽松阈值改为逐 owner 精确计数。

## 当前证据与待办

- IDE 静态错误检查：四个修改的 Java 文件无 error。授权后，在仓库外、不复制主工作树 `target/` 的快照执行 `rtk mvn -o -B -Dtest=RetailQuestDriverOverlayTest,QuestProductionStartupGateTest,RetailOwnershipGateTest test`：**BUILD SUCCESS；11 例 / 0F / 0E / 0 skipped**（Overlay 5、Startup 2、Ownership 4；总耗时 46.551 s）。临时副本已删除，主仓 `target/` 未清理，未重启服务器。
- 运行时读取的 `src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv` 与门禁使用的 `src/test/resources/quest/retail-xml-retention.tsv` 在验证后字节相同（SHA-256 前 16 位均为 `f033c18dae59e507`）；不把仅存在于 test classpath 的清单误当生产输入。
- 复制预检首次使用未锚定的 `aion/` 排除模式，误排 `src/main/resources/aion/`，因生产 catalog 文件不存在而在 Maven 前中止；临时副本已删除。随后改为根锚定 `--exclude='/aion/'` 等模式，快照内重新对账通过，才执行上述测试。复制失败这次**不是测试失败**。
- 2026-09-24 首次 T1/T3 仓外副本预检的阻断记录（上段 18 个缺失 ID）是**历史中间态**：当时确实未进入 Maven；随后共享 owner/XML 又发生变化，不能再用它代表后续验证结果。
- 后续获准的 T1/T3 在另一份不带主工作树 `target/` 的仓外快照完成；该次 owner 快照为 6224 = `RETAIL_TABLE` 3890 + `XML_RETENTION` 2334，owner、catalog、磁盘 XML ID 集合对齐。T1 **50/50 通过**（0 失败、0 错误、0 跳过，forkCount=2）。T3 于 2026-09-24 23:59:02+08 结束：**1971 项 / 43 失败 / 72 错误 / 1 跳过**。对照既有 T3 基线 1961 / 30 失败 / 69 错误 / 1 跳过，规范化测试方法指纹新增 18、消失 2；这是含多个并发切片的整树快照，不能归因为 P0f。新增指纹涉及 `ClientQuestSectionAlignmentTest`（1）、`CounterChainTripletContractTest`（5）、`P0c9GapShapeProbeTest`（1）、`Quest18950/21320/28950/2929/30515ClientDialogAlignmentTest`（各 1）、`Quest80787To80794RetailAlignmentTest`（2）、`QuestE2eInfrastructureTest`（1）、`QuestNoHandlerShard3DefinitionTest`（1）、`QuestRetailStartMetadataGateTest`（1）、`RetailSimpleHuntEquivalenceGateTest`（1）；消失的 2 个指纹属于 `ItemCollectingDialogProtocolAlignmentTest`。
- T1/T3 之后的共享工作树静态快照又变为 6224 = `RETAIL_TABLE` 3910 + `XML_RETENTION` 2314；主/测试 owner 清单 SHA-256 前 16 位均为 `537d89cdbeb5adfb`，生产 catalog 与磁盘 XML 各 2314，`XML_RETENTION`、catalog、磁盘 XML 的 ID 集合一致。**这是后续快照，T1/T3 没有验证这组新文件状态**，下一轮不得把两个 owner 口径混作一轮运行输入。
- T3 的失败需结合并行测试改动重新归属：当时 `ClientQuestSectionAlignmentTest` 直接读取已退役的 `11102.xml`；当前共享工作树已改为 `ProductionQuestDefinitions.definition(questId)`，但尚未验证。`CounterChainTripletContractTest` 当前也改为生产视图与真端网格语义；T3 中该类的旧断言失败不应直接当作当前结果。`P0c9GapShapeProbeTest` 出现在 T3 快照，但当前源码树没有该探针。
- **28313 职业可选奖励是下一优先审计项**：T3 快照中该测试观察到 27 个职业奖励物品展开为 **432 条完成路由**（27 × dialog action 8..23）。当前并行版本的测试接受 8..23 全区间并只核对 11 个职业、27 个去重物品及每条路由带一个职业物品；它没有锁定每个 dialog action 应对应哪个物品。应先以真端/客户端动作语义确定 action→物品映射，再将 28313 做成黄金用例并审计整个 class-selectable 家族；不要仅凭去重物品完整就接受 432 条路由。
- 下一条最短验证路径：先完成 class-selectable 家族的 action→物品合同与路由级回归，再对**同一份**当前 owner/catalog/XML 快照做逐 ID 对账；其后依序 T1、干净仓外快照 T3。真实客户端验收仍为 `PENDING`。主工作树 `target/classes` 的已退役 XML 残留可能令非 clean 测试假绿。**不得只为让构建继续而盲改 owner，也不得把 T3 的整树差异记为 P0f 回归。**
- 若生产目录严格校验发现已有真端 owner 无法合成，应先按任务 ID 和拒绝码定位真实缺口，不得降回仅验证总量的弱门禁。
