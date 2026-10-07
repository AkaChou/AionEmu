# 遗留问题 A-D 复核：门禁实测记录（2026-10-07，用户授权后 IDEA MCP）

## 1. 结果总表

| 门禁 | 结果 |
|---|---|
| `FactionDailyRewardRowInRangeContractTest`（新增） | 3/3 绿（17 行界内 + 8 对镜像等值 + 36512 结构锁） |
| `ArchdaevaRewardRowContractTest`（D 重锚） | 7/7 绿（10525/20525 reward 7→6、handover 6→6、自愈边 7→6） |
| `JournalRewardRowRepairContractTest` | 4/4 绿 |
| `RewardRowProjectionRegressionTest` | 168 例全绿（56 行 × 3 断言） |
| `Quest10525TestimonyCounterContractTest` | 2/2 绿（10525/20525 var1 布局不受 reward 修改影响） |
| `MissionItemConsumptionBatchRegressionTest` | 4/4 绿（含 20525 s4→s5 扣材 + 15606/1367 等活体断言） |
| `QuestDefinitionCatalogManifestTest`（目录级） | 10/10 绿（生产目录编译含 10525/20525/36512 改动） |
| `ProductionCatalogWhitelistVerificationTest`（目录级） | `PRODUCTION_COMPILE_OK=707 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0` |

静态：`xmllint + quest_definition.xsd` validates 3/3（10525/20525/36512）。

## 2. 顺手收口的两处存量红（退役陈旧引用，与 A-D 改动无关但阻挡了本次实测）

| 测试 | 存量红 | 处置 |
|---|---|---|
| `ArchdaevaRewardRowContractTest#awakenedSageHandoverDoesNotRequireTheSummoningItemToRemainInInventory` | 加载 `10526.xml`——该 XML 已由 `9321e7663`（2026-09-29，批量物理退役 29 个遗留 XML）删除（RETAIL_TABLE/DataDriven），测试未同步 → `missing quest definition 10526.xml` | 从 sages 映射移除已退役的 10526/20526（保留 10528/20528 对，删去只服务于退役对的 expectedStep=12 分支） |
| `MissionItemConsumptionBatchRegressionTest`（3/4 用例红） | 38 个引用 id 已 RETAIL_TABLE 退役（无 XML、生产视图无 overlay 定义）：kaliga 族 28 个 + 3092/29064/15608/15613/25601/25605/10010/20010/15400/25400/1362 | 按 Batch37 先例改**退役守卫**（`assertTrue(RetiredQuestIds.contains(id))`，注释留退役前口径与"表车道门承担"指向）；活体断言（11216/15606/1573/1636/2333/19000/17540/18511/27540/28511/14025/14046/14051/20110/20112/20525/20529/24030/2001-2006/1922/2947/1367 等）原样保留 |

两处均属“退役提交未同步清理引用”的存量债（前例：QE-054 收口 Batch37 的 26905/26906/26908 退役守卫）。

## 3. 命令与工具链

- 全部经 IDEA MCP（用户授权）：`build_project` 增量编译 → `get_run_configurations`（文件运行点行号）→ `execute_run_configuration`；外改后已先行重编译（无旧字节码假红）。
- 静态校验：`xmllint --noout --schema <quest_definition.xsd> <xml>`（仓库根运行）。

## 4. 待办（PENDING_CLIENT）

- 10525/20525：领奖时任务书显示「使用 quest_10525d」行（axis=6）、领奖对话与旧档自愈（REWARD/7 → 6）。
- 36512：领奖时任务书显示「向 E_36500 报告」行（axis=1）、旧档（REWARD/2）重登自愈。
