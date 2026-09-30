# 2026-09-29 DataDriven 单步 Talk 链收口（13 个）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

## 范围

- 本批从壳 XML / 生产目录转为 Canonical Lifecycle 真端驱动：`10500, 20500, 13961, 23961, 18649, 28649, 35055, 35056, 35057, 45055, 45056, 45057, 80989`。
- 不新增页码类 TSV；`retail-xml-retention.tsv`（主/测）同步翻为 `RETAIL_TABLE DataDriven OK basis=DD_TALK_CHAIN`。
- `retail-data-driven-drift.tsv` 本批 13 行翻为 `ADOPTED`；随后 strict sync 重算又纠正 21 条 retired stale 行，终值 `ADOPTED=1236`、当前生产宇宙剩余 `272`。
- 冻结 IR 指纹刷新到 1236 个已退役 DataDriven 行；接受下限 / 冻结退役规模维持 `1091`。

## 实现要点

- `RetailClientTalkChainPages` 新增共享形状 `p21`：`SELECT_NONE(4762)` 首页 + `SELECT2(1352)` 步骤页 + `SETPRO1(10000)` 推进；登记上述 13 个任务。
- `RetailDataDrivenTalkCompiler.buildChain()` 支持系统接取、每步 cutscene、单步链 `s1` 别名；仅当推进动作不是 `SET_SUCCEED` 时才发出 `SET_SUCCEED -> s1` 捷径边并声明 `s1`，避免同事件重复边与不可达节点。
- `RetailDataDrivenDefinitionCompiler` 通过 `talkChainAcquire` 放宽系统接取轴，并把接取类别、world 接取 id、cutscene 映射传入链合成器。
- 保留交付分档奖励窗与完成流走采集族既有 Canonical 生命周期。
- `RetailClientAcceptEntryPage` 补齐通用 `select1` 接取梯：1011 首屏无接受按钮时，按 `QuestDialogContract` 只补客户端声明的 1012/1013 翻页边；已有页梯家族不重复注入。门禁覆盖 1144 判例和全生产 `select1` 入口，覆盖面下限 40。

## 验证

- 临时探针验证 14 个历史拒绝/不可达样例全部 `ACCEPTED`；探针用后删除。
- 已清理 `target/classes` 中 13 个已删壳 XML 的陈旧副本（检查均为 absent）。
- 指纹重放：`mvn test -Dtest=RetailDataDrivenGateTest -Dretail.dataDriven.fpOut=src/test/resources/quest/retail-data-driven-ir-fingerprints.tsv`。
  - 首跑暴露测试资源刷新顺序：`fpOut` 已写源文件，但同一 JVM 仍读 `target/test-classes` 旧 1218 行；重跑资源复制后通过。
- 门禁全家桶：`mvn test -Dtest=RetailOwnershipGateTest,RetailQuestCatalogTest,RetailDataDrivenGateTest,RetailQuestContractTest,RetailTsvManifestGateTest,ReportToManySetSucceedAlignmentTest,Batch29RewardRowClosureContractTest,RetailClientAcceptEntryPageTest -Dretail.dataDriven.fpOut=src/test/resources/quest/retail-data-driven-ir-fingerprints.tsv`
  - 结果：`Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`，BUILD SUCCESS。
- drift strict sync 收口：`RetailDataDrivenGateTest.driftVersusShellsIsRegistered` 取消 retired 豁免；retired 只豁免壳 XML，不豁免登记同步。
  - `mvn test -Dtest=RetailDataDrivenGateTest -Dretail.dataDriven.equivOut=src/test/resources/quest/retail-data-driven-drift.tsv`
  - 首跑按预期红，暴露 21 条 retired stale 行并重写登记：`10011, 10501, 10502, 10503, 10504, 10505, 10507, 15000, 15680, 16820, 17525, 18996, 20011, 20502, 20503, 20506, 20507, 25023, 26820, 27525, 28996`。
  - 重跑 `RetailDataDrivenGateTest`：`6/6` 绿；`ADOPTED=1236` / `rows=1508`。
- ownership + manifest + DataDriven 聚焦门：`mvn test -Dtest=RetailOwnershipGateTest,RetailTsvManifestGateTest,RetailDataDrivenGateTest`
  - 结果：`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`，BUILD SUCCESS。
- `git diff --check` 无输出。
- 内存库：`sync_memory_bank.py` + `verify_memory_bank.py` 三步全绿（145 个 Pattern，fresh 145 / stale 0）。

## 未验证层

- 未启动游戏服务器，未做真实 Aion 5.8 客户端接取、推进、领奖验收。
- 未 push。

## 剩余

- 当前生产宇宙 DataDriven 剩余 `272`（`1508 - 1236`）。
- `RETAIL_TALK_CHAIN_DEFERRED=5`：`1888, 2888, 10033, 15550, 25550`。
- 另有既有非本批暂缓/拒绝族仍按 drift 登记逐码推进，不得混入本片。
