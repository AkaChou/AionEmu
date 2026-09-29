# 任务引擎启动期冲突校验二级分桶优化（解决多段高计数任务启动假死）

## 1. 现象与证据
- **报障现象**：服务端启动时，输出 `AI2 引擎开始加载 / 已加载 819 个 AI2 脚本` 后长时间停滞不动。
- **线程分析（jstack）**：
  - 主线程 `main` 阻塞在 `GameEnginesLifecycle.await()`；
  - `InstantPool-1` 阻塞在 `QuestEngine.awaitProductionCatalogPreload()`；
  - 守护线程 `quest-catalog-preload` 占满单核 100% CPU，死循环式运行 `BitSet.intersects` -> `QuestDefinitionCompiler.sourceNodesAreMutuallyExclusive` -> `validateTransitionConflicts`。
- **JFR 证据**：`startup-19632-2026_09_29_10_16_25.jfr` 中采样到 3014 次 `BitSet.intersects`，持续消耗 CPU 数百秒。

## 2. 根因剖析
1. **多段高计数任务网格展开状态爆炸**：
   - commit `2415b2f9c` 退役了 35 个 SimpleHunt 存量 XML。
   - 其中 Q30244 / Q30344（帕休曼迪尔寺院神殿净化，20/10/40）展开生成 9,471 个状态节点、125,740 条转移边；Q2434 展开生成 6,125 个节点、50,400 条边。
2. **冲突检测算法 $O(N^2)$ 雪崩**：
   - `QuestDefinitionCompiler.validateTransitionConflicts` 原先仅按 `EventConflictKey` 一级分桶。
   - 同一怪物的约 9,000 条转移边分布在 9,000 个不同节点上，却被全部放入同一个列表进行两两比较（$9000 \times 8999 / 2 \approx 40,500,000$ 次比较）。
   - 单任务 14 种怪物累计执行 5.6 亿次 `BitSet.intersects`，多任务累加超过 12 亿次比较，导致生产目录加载耗时高达 361.7 秒（6 分钟以上）。

## 3. 架构优化方案（二级状态节点分桶）
在 `QuestDefinitionCompiler.java` 中引入 `EventConflictBucket`：
- **单节点候选（`singleNodeCandidates`）**：按 `nodeIndex` 分桶存储；
- **多节点通配候选（`multiNodeCandidates`）**：存储跨节点的通用边；
- **比较逻辑**：
  - 单节点边仅与同节点边和包含该节点的多节点通配边比较；
  - 多节点边遍历其 set bit 对应的单节点边和多节点通配边比较；
- **数学等价**：不同来源节点的单节点边交集必为空集，原算法在 `sourceNodesAreMutuallyExclusive` 处亦必然返回互斥。

## 4. 验证结果
- **语法与静态检查**：IDEA Lint 0 errors, 0 warnings，`git diff --check` 通过。
- **聚焦测试**：
  ```bash
  mvn test -Dtest=QuestDefinitionCompilerTest,QuestDispatchToAltgardFamilyProductionFlowTest
  ```
  - `QuestDefinitionCompilerTest`：45 个用例全部通过（耗时 0.103 s）；
  - `QuestDispatchToAltgardFamilyProductionFlowTest`：**从 361.7 秒降至 10.76 秒**（提速 33.6 倍，包含全量生产目录 6224 任务编译）；
  - Tests run: 47, Failures: 0, Errors: 0, BUILD SUCCESS。
