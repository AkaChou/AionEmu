# W6 · 清理批（shell 编译器退役 + 命名反义 + 登记补注）落地记录

> 车道：`quest-native-dispatch`；面：**W6 清理（①②③④⑥ 中的可立即执行项）**。
> 上游：`GOAL.zh-CN.md` §3 **W6**；`2026-09-27-w5c-w6-recon-pack.zh-CN.md`（四路只读取证）。
> 纪律：未 commit；未启停服务；未新增/退役任何 TSV（本片只改表头注释与清单 note）；T3 在仓库外副本、跑完即删。

## 0. 一句话结论

迁移期 shell 编译器 `RetailSimpleHuntIrCompiler`（444 行）+ 其测试（556 行）**零生产引用**，成对删除；
`RetailSimpleHuntDefinitionCompiler` 的 2 处 javadoc 提及同步改写；`RetailSimpleTalkMigrationReviewContractTest`
的**名实相反**测试方法改名；`retail-quest-ai-name-groups-rejected.tsv` 表头与清单 note 补**生成器路径与车道**。
**零行为变化**（T1/T2/T3 全 `ADDED 0 / REMOVED 0` 且红集 sha256 恒等；T3 测试数 2022 → 2016 = 删掉的 6 条全为绿灯）。

## 1. W6-① shell 编译器退役

| 判据 | 证据（实测） |
|---|---|
| 生产零引用 | `grep -rn RetailSimpleHuntIrCompiler src/main src/test` 仅命中：自身、其测试、`RetailSimpleHuntDefinitionCompiler.java:40/49`（javadoc） |
| 不在门禁注册面 | `.agents/summary/scriptdll-quest-driver/affected_quest_tests.py` 的 T1_GATE_CLASSES 零命中 |
| 测试资源有第二消费者（须保留） | `quest-simple-hunt-retail-contract.tsv`（另有 `RetailSimpleHuntPlanEquivalenceTest`、`QuestSimpleHuntRetailContractTest`）；`quest-simple-hunt-server-target-exceptions.tsv`（另有 `QuestSimpleHuntRetailContractTest`）；`retail-simple-hunt-ir-fingerprints.tsv`（**等价门 `RetailSimpleHuntEquivalenceGateTest` 的 shell 基线**，必须保留） |
| 动作 | 删 `src/main/java/.../retail/RetailSimpleHuntIrCompiler.java` + `src/test/java/.../retail/RetailSimpleHuntIrCompilerTest.java`；改写上述 2 行 javadoc（去掉指向已删类的 `{@link}`，语义保留："本类是家族唯一入口（迁移期 shell 方案已退役）"、"其余沿用原 shell 方案的网格校验语义"） |

## 2. W6-③ `FailurePage` 命名（部分收口，剩一条待裁定）

| 项 | 处置 |
|---|---|
| 名实相反（已修） | `RetailSimpleTalkMigrationReviewContractTest.clientCheckButtonsConsumeEveryRetailRequirementAndKeepTheFailurePage` → `…AndRetireTheFailurePage`（方法体断言检查按钮对与 SELECT6 失败页**退场**；改名后 4/4 绿，且该方法**不在任何红集**故无身份扰动） |
| **一名指两页（待裁定，登记）** | `FailurePage` 同时指 `SELECT6`(2716) 与 `CHECK_USER_ITEM_FAIL`(10001) 两个页——命名的歧义面，涉及测试/文档/登记表多处；**本片不改**（属跨文件重命名，需单独裁定范围） |
| 其余载体 | XML `failure-page` 属性、TSV `failurePage` token、布尔 `hasFailurePage`：语义明确，保留 |

## 3. W6-④ `rejected.tsv` 登记补注

- 表头追加：`# 生成：.agents/summary/scriptdll-quest-driver/p0c52_quest_ai_name_groups.py（if emit: 分支，车道=scriptdll-quest-driver）`；
- 清单 note 补同信息 + `本表零生产消费者`；
- **生成器停写**仍属兄弟车道（本车道只读），列为移交项。

## 4. W6-② / ⑥（核实结论，非本片动作）

| 项 | 结论 |
|---|---|
| EarlyElyos 3 条在册红 | **成立**（最新 T3 日志该类 3 Errors；红集与四份历史红集逐字恒等）。修复属**形状工作**（期望形状：1131 `started→shugo` 交付边 / 1561 `CanAct` 自环门 / 1691 `spoken-to-diana→returned-to-sneaker`），须单独切片 + 裁定，本片不动 |
| `report_pages` 生成器停写 | `build_quest_client_report_pages.py:34-35/:67` **仍会写回**已退役路径（无 emit 开关）⇒ 兄弟车道执行；重跑会被 `RetailTsvManifestGateTest` 立刻拦红（fail-closed 兜底） |

## 5. 门禁

| 门 | 结果 | 证据 |
|---|---|---|
| 聚焦（清单门 / 等价门 / SimpleHunt 家族门 319.7 s / SimpleTalk 链门 8/8 / 家族门 4/4 / 命名测试 4/4） | **22/22 绿** | 本片聚焦跑 |
| **T1** | **ADDED 0 / REMOVED 0**；sha256 `3b92439da8052988f0acd3a23a646786a78c3ac58e7c64a9c1dcabdd1d5cd137` 恒等 | `gates/T1-215229.log` + `gates/T1-w6a-*.txt` |
| **T2** | **ADDED 0 / REMOVED 0**；sha256 `57bb0621b2aa90f861e41358f63baa5450e73753440160ba276bb0255df721fd` 恒等 | `gates/T2-215908.log` + `gates/T2-w6a-*.txt` |
| **T3** | **ADDED 0 / REMOVED 0**（**2016** 测试 = 2022 − 6 条被删测试，全为绿灯）；sha256 `ce4673c74ed61fdcff494fedcc1b8e98c78b252d54b150a05c4bdaa937197870` 恒等 | `gates/T3-215222.log` + `gates/T3-w6a-*.txt` |

> 口径说明：**红集 sha256 恒等而测试数 −6**，正是"删掉的类全绿"的机器证明（若删到红测试，红集必减条目）。

## 6. 纪律回执

未 commit/push；未启停服务；未新增/退役任何 TSV；未创建 worktree；T3 在仓库外全树副本
（`/private/tmp/aion-t3-w6a`）跑、跑完即 `rm -rf`；Maven 仅用于聚焦测试与 T1/T2/T3 门禁；树内与副本并行时
树内只有一个 Maven；兄弟车道文件（生成器脚本）只读；取证子代理只读且不跑 Maven，其结论经主执行体复核
（其中一条"过期基数"结论被复核**否决**并更正在取证包 §5）。
