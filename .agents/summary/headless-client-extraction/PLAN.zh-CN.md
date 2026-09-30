# 无头客户端迁出方案与执行记录

- 状态：**已执行搬运与移除；编译与全量测试均已跑（2026-09-30 用户授权）**。数据与 48 个 Java 文件已移到仓库外的本地项目 `AionEmu-headless`，本仓库不再包含它们；逐文件清单与 SHA-256 见同目录 `MIGRATION.zh-CN.md`。验证边界：`mvn -q test` 编译通过，全量 3965 个测试为 187 failures / 90 errors，全部落在并发车道的真端迁移上（本次未改动 `src/main/**` 与 `src/test/resources/**`）；保留的 8 个运行时夹具测试类共 24 个用例，21 绿 / 3 红，3 个红都是并发车道改形后未重钉的既有失败（27510、30603/30613、28504）。
- 日期：2026-09-30
- 目标：把「客户端页面/动作映射数据 + 无头客户端对拍能力」迁到独立项目，`AionEmu-test` 只保留运行时冻结表 + 最小完整性门禁。
- 一句话结论：**可行**。但现在的"无头客户端"是**进程内驱动 server 内部实现**的夹具，不是走 socket 的真客户端；必须按依赖方向切分：被**留下测试**用到的类留在 server（经 test-jar 导出），只有**迁出测试**用到的类才搬走。

## 1. 不可破坏的不变量

1. **运行时输入零改动**：`src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv` 与 `src/main/resources/quest/quest_client_*.tsv` 本次迁移不改内容（它们是生产资源，被 `RetailQuestDriver`、`RetailClientAcceptEntryPage`、`RetailClientTalkChainPages`、`RetailClientHandinPages`、`RetailClientSummaryRows` 读取）。
2. **server 默认 `test` 全绿**：迁移后仓库里不能留任何指向已迁出数据的类、路径或门禁。
3. **体积目标**：`docs/quest/client-dialog-mapping/`（15 文件 / 31 MB）不再由主仓库承载（迁出或转 submodule）。
4. **漂移仍可检测**：主仓库保留"派生表被静默改动"的摘要门禁（见 §5）。
5. **被测修订可追溯**：迁出项目必须能说清"这次跑的是哪个 server 修订"。

## 2. 归属规则（依赖方向）

| 规则 | 判定 | 结果 |
|---|---|---|
| R1 | 被**留下测试**引用的类 | 留在 server，进 `test-jar` 导出 |
| R2 | 只被**迁出测试/工具**引用的类 | 随数据迁出 |
| R3 | 两类都用到的共享类型 | 留在 server（R1 优先），迁出项目经 test-jar 依赖 |

## 3. 逐文件归属

### 3.1 数据（15 文件 / 31 MB）→ 全部迁出

| 文件 | 体积 | 迁出后主仓库替代 |
|---|---:|---|
| `quest-dialog-pages.csv` | 14.2 MB | 无（派生表已冻结） |
| `quest-dialog-action-details.csv` | 11.7 MB | 同上 |
| `legacy-quest-dialog-template-index.csv` | 2.2 MB | 同上 |
| `legacy-quest-dialog-contracts.csv` | 2.1 MB | 同上 |
| `same-symbol-map.csv` / `same-id-map.csv` | 1.05 MB | 同上 |
| `client-monster-progress-contracts.csv` | 494 KB | 同上 |
| `client-html-pages.csv` / `client-hyperlinks.csv` | 455 KB | 同上 |
| `page-action-map.csv` / `quest-action-summary.csv` | 140 KB | 同上 |
| `parse-recoveries.csv` / `parse-errors.csv` | 13 KB | 同上 |
| `README.zh-CN.md` / `mapping-summary.json` | 13 KB | 迁出项目文档 |

### 3.2 生成器（20 个脚本）→ 三分流

| 组 | 数量 | 处理 |
|---|---:|---|
| 数据生产：`generate_client_dialog_mapping.py`、`extract_legacy_quest_dialog_contracts.py`、`quest_dialog_symbols.py`、`test_generate_client_dialog_mapping.py` | 4 | 迁入新项目（数据源=客户端解包） |
| 生产表生成：`scriptdll-quest-driver/build_quest_client_*.py`（含 `_talk_chain_steps`） | 8 | 迁入新项目并**参数化输出目录**（见下） |
| 一次性分析：`p0c*`、`m5b2b*` 等 census/decisions 脚本 | 8+ | 保留在 `.agents/summary/` 作为历史证据，不迁入、不删除 |

**已核实的关键事实**：这 8 个生产表生成器写的是**旧路径** `src/main/resources/aion/data/static_data/quest_retail/`，而现役生产表在 `src/main/resources/quest/`；全库 **153 个脚本仍引用旧路径**，且**没有任何脚本写当前 `src/main/resources/quest/`**。即：生产表再生链**当前已经失效**，本次迁移正好把它交给新项目统一修好（`--out-dir` 参数），主仓库不再假装能再生。

### 3.3 Java 类归属

**留 server（14，进 test-jar）**：`e2e/QuestE2eRuntime`、`e2e/QuestE2eWorldFixture`、`e2e/client/QuestProtocolLoop`、`e2e/client/VirtualClientState`、`e2e/client/QuestTrace`、`e2e/client/ServerPacketObservation`、`e2e/client/ClientActionRequest`、`e2e/world/VirtualClock`、`e2e/QuestE2eTransitionMatch`、`e2e/QuestE2eAuditRow`、`e2e/QuestE2eStatus`、`e2e/QuestE2ePacketValidator`、`e2e/QuestWorldReachabilityOracle`、`e2e/QuestDialogLoopBreakerProductionFlowTest`。
依据：这些被**不依赖客户端数据**的测试使用（`Quest3732InstanceObjectiveTest`、`QuestPacketOrderRegressionTest`、`QuestCounterProjectionLockFollowUpTest`、`QuestCounterSourceProjectionProductionFlowTest`、`QuestDialogProjectionLockFollowUpTest`、`QuestResidualCounterLocksTest`、`QuestStepNpcSkipGuardTest`、`QuestDialogLoopBreakerProductionFlowTest` 等）——它们驱动 `QuestE2eRuntime` 但不读 CSV。

**随数据迁出（12）**：`e2e/client/ClientResourceOracle`、`e2e/client/QuestHeadlessClient`、`e2e/QuestE2eBatchAudit`、`e2e/QuestE2eReportWriter`、`e2e/LegacyQuestEvidenceOracle`、`e2e/HandoverContinuationContract`、`e2e/QuestE2eAudit`(CLI)、`e2e/QuestProductionJourneyAudit`(CLI)、`e2e/journey/QuestJourneyRunner`、`e2e/journey/QuestProductionJourneyPlanner`、`e2e/journey/QuestProductionJourneyExecutor`，以及 `definition/` 下的客户端对拍工具（`QuestDialogOrderAudit` 及其 CLI `QuestDialogSequenceAudit`、`QuestPrematureRewardRouteAudit`；**待确认是否被留下测试引用后再定**）。

**共享类型处理**：`QuestHeadlessClient.DispatchOutcome` 被留下的 `QuestProtocolLoop`/`QuestPacketOrderRegressionTest`/`QuestDialogLoopBreakerProductionFlowTest` 引用 → 必须先下沉为中性类型（如 `ClientActionOutcome`），否则 R1 会把整个 `QuestHeadlessClient` 拖住。

### 3.4 测试类归属（30 个测试类 / 161 个 `@Test`）→ 全部迁出

| 组 | 类数 | `@Test` | 说明 |
|---|---:|---:|---|
| `definition/*` 客户端对拍 | 20 | 90 | 13 个 `*ClientDialogAlignmentTest` 家族 + B 类路由 + 怪物进度 + legacy 对拍 + 契约门 |
| `e2e/*` 无头审计与 journey | 10 | 71 | 全量审计、页面按钮、世界可达性、交接续接、Golden Journey、无头基础设施 |
| 合计 | 30 | 161 | 迁出后主仓库这 161 条断言归零 |

**混合类**：`QuestDialogOrderAuditTest`（17 条）里只有 2 条读真实 CSV，其余用自造夹具 → 建议整类随迁，或拆成"夹具类留 server + 真实数据类迁出"。

## 4. server 侧需要的改动（小而明确）

| # | 改动 | 位置 |
|---|---|---|
| 1 | 产出 `test-jar`（`maven-jar-plugin` 的 `test-jar` goal） | `pom.xml` |
| 2 | 下沉 `DispatchOutcome` 为中性类型 | `questEngine/e2e/client/` → `questEngine/runtime/` |
| 3 | 加摘要门禁（§5） | `src/test/resources/quest/` + `src/test/java/.../definition/` |
| 4 | 改写文档/规则引用（§6） | `docs/`、`.agents/rules/`、`.agents/memory-bank/` |

## 5. 主仓库摘要门禁设计（保留什么）

新增 `src/test/resources/quest/client-contract-provenance.tsv`：

```text
# derived_table	source_snapshot_id	retained_quests	row_count	sha256
client_dialog_contract.tsv	aion-headless@2026-09-30/quest-dialog-pages.csv	5617	27816	…
quest_client_handin_pages.tsv	…	…	300	…
```

新增 `ClientContractProvenanceGateTest`：①派生表存在；②行数与登记一致；③sha256 与登记一致；④`source_snapshot_id` 非空。作用＝**任何对派生表的静默修改都会红**。

改造 `QuestMovieContinuationGateTest`：CSV 哈希钉子在数据不在仓库时改为 **SKIP + 明确打印**（"来源数据已迁出，新鲜度由 aion-headless 校验"），不再失败。

**保留**：`RetailTsvManifestGateTest`（冻结面结构）、`Retail*GateTest`（真端生命周期）。

**明确代价**：主仓库**不再能发现新的客户端契约缺陷**（`PAGE_NOT_IN_TASK_HTML`/`BUTTON_WITHOUT_ROUTE`、逐页对拍、怪物进度对拍、Golden Journey 都不在默认 `test` 里），只能在迁出项目跑。这是本次迁移最大的能力损失，必须写进 playbook。

## 6. 文档引用改写清单

| 文件 | 引用数 | 改法 |
|---|---:|---|
| `.agents/memory-bank/patterns/quest-engine.md` | 13 | 指向迁出项目；保留模式结论 |
| `docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md` | 9 | 客户端证据入口改为"迁出项目 + 报告回贴" |
| `docs/quest/client-dialog-mapping/README.zh-CN.md` | 8 | 随数据迁出 |
| `docs/quest/repair-playbook/CASES.zh-CN.md` | 2 | 同上 |
| `docs/README.zh-CN.md`、`docs/quest/NPC_DIALOG_CONTEXT.zh-CN.md` | 各 1 | 路径改为迁出项目名 |
| `.agents/rules/quest-repair.md` | 1 | 验收流程加"跑无头客户端"一步 |
| `.agents/memory-bank/activeContext.md` | 1 | 焦点改为迁移状态 |

`.agents/summary/` 下约 100 份历史记录会变成指向已迁出数据的引用：**保持原样**（历史证据），在 `CLEANUP-LEDGER` 追加一条迁移登记说明新位置。

## 7. 新项目骨架（建议）

```text
aion-headless/
├── data/client-dialog-mapping/        # 31 MB 原始映射（迁入）
├── data/generators/                   # 数据生产 + 生产表生成器（参数化 --out-dir）
├── src/test/java/.../headless/        # 30 个测试类（161 断言）+ 2 个 CLI main
├── src/test/resources/quest/          # 报告基线（可回贴主仓库）
├── reports/                           # 每次运行的无头客户端报告
└── pom.xml                            # depends: com.aionemu:aionemu (jar + test-jar)
```

运行方式（默认 server 修订 = 本地 `install` 的版本）：

```bash
# 主仓库：先安装当前修订（需授权）
./mvnw -q -DskipTests install
# 迁出项目：跑无头客户端全量对拍
./mvnw -q test -Dserver.jar="$HOME/.m2/repository/com/aionemu/aionemu/<version>/aionemu-<version>.jar"
```

报告回贴：`reports/<date>-<rev>.md` → 复制到主仓库 `.agents/summary/quest-headless/`，供 playbook 引用。

## 8. 分步执行计划

| 步 | 动作 | 产物 | 验证 | 回滚 |
|---|---|---|---|---|
| 0 | 冻结现状（记录 HEAD、`git status`、跑一次全量 `test` 基线） | 基线报告 | 全绿 | — |
| 1 | `pom.xml` 加 `test-jar` | 可被依赖的测试包 | `install` 后 jar 存在 | 还原 pom |
| 2 | 下沉 `DispatchOutcome` 中性类型 | 一处小重构 | `mvn -q test`（留下测试） | 还原该文件 |
| 3 | 新建 `aion-headless` 骨架 + pom（依赖 server jar/test-jar） | 空项目可编译 | `mvn -q test`（空跑） | 删目录 |
| 4 | 迁移数据 15 文件（新项目 git 仓库） | 主仓库少 31 MB | 主仓库 `test` 仍全绿（此时未删引用） | 还原目录 |
| 5 | 迁移生成器并按 `--out-dir` 参数化；主仓库删除/归档旧脚本 | 生成链在新项目可跑 | 新项目重生成 → 与现役派生表逐字节一致（**这就是"未破坏"的硬证据**） | 还原脚本 |
| 6 | 迁移 30 测试类 + 2 CLI + 12 个类 | 主仓库少 161 断言 | 新项目跑通 161 断言；主仓库 `test` 全绿（无残留引用） | 还原文件 |
| 7 | 加摘要门禁 + 改造哈希钉子 + SKIP 分支 | 主仓库有漂移检测 | `test` 全绿；手工改派生表 → 门禁红 | 删除门禁 |
| 8 | 改写文档/规则/memory-bank + `CLEANUP-LEDGER` 登记 | 引用不再悬空 | `verify_memory_bank.py` 绿 | 还原文档 |

步 0/1/2/3/5/6/7 涉及 Maven，需要逐次授权（我会在执行前给出确切命令与范围）。

## 9. 开放决策（需要你选）

- **D1 新项目位置与名字**：已定为独立 git 仓库、仓库外本地，项目名 `AionEmu-headless`。
- **D2 harness 归属**：`test-jar`（改动小，推荐起步）vs 把 harness 提升为 `src/main/java/.../questEngine/harness` 公开 API（更干净，改动大）。
- **D3 摘要门禁强度**：哈希+行数校验（推荐）vs 只登记不校验 vs 完全不设（主仓库将失去漂移检测）。
- **D4 生成器分流**：8 个生产表生成器迁入并修好输出路径（推荐）vs 先整体归档，只把数据生产 4 个迁出。
- **D5 是否保留 submodule 形态**：数据放独立仓库、主仓库以 submodule 引用（体积小、测试仍可自动跑），可与上面的"完全迁出"并存。

## 10. 风险登记

| 风险 | 影响 | 缓解 |
|---|---|---|
| 迁出后主仓库不再自动发现客户端契约缺陷 | 回归可能静默合入 | 摘要门禁 + playbook 强制"改任务前跑无头项目" |
| 两份 harness 实现漂移 | 测试结论不可信 | R1/R3 规则：共享类只留一份，经 test-jar 依赖 |
| 被测 server 修订不清 | 测了旧 jar | 运行脚本记录 jar 版本/提交号进报告 |
| 报告快照过期 | 重演 `client-lifecycle-alignment.csv` 的死法 | 报告只在 `.agents/summary/` 留档，不作门禁输入 |
