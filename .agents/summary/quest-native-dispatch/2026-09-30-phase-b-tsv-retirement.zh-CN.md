# 批 P-B 执行台账：推进规范形生命周期与多余辅助 TSV 大幅缩表退役（2026-09-30）

> 授权：用户指示「C、A、B，授权 maven」，全面推进选项 B。
> 性质：**TSV 物理整表退役 + 严格缩表 + 死代码清理**
> 核心目标：遵循“真端驱动”与“规范形生命周期”（Canonical Lifecycle）原则，废除微观页码阶梯补丁，大幅削减和退役 TSV。

## 0. 一句话结论

累计削减 **16,121 行** 冗余表数据与死代码，核心门禁与全家族门禁 100% 全绿通过：
1. **彻底物理退役 `quest_client_handin_exceptions.tsv`**（删除 7,608 行）：该表为早期机械比对遗留产物，`RetailClientHandinPages.excluded(int)` 在全仓无任何有效调用，整表物理删除并解耦代码；
2. **P2 战役严格缩表 `quest_client_dialog_exits.tsv`**（由 324 行缩减至 14 行）：剔除已随规范形生命周期废除的 310 行微观翻页死标记（`SELECT2_CONTINUE`、`SELECT6`、`SELECT5_CHECK` 等），仅保留 14 行真正在生效的 `SELECT_NONE_1` 续页路由；
3. **严格缩表 `quest_client_use_item_report.tsv`**（由 106 行缩减至 5 行）：剔除 101 行与系统默认行为（`Mode.REWARD`）一致的冗余默认行，仅保留 5 行实质 `CHECK` 规则；
4. **代码级死代码清除**：
   - 清除 `RetailClientHandinPages.java` 中的 `withExceptions` 与 `excluded`；
   - 清除 `RetailClientDialogExits.java` 中的未引用常量（`SELECT1_1`、`SELECT1_1_1`、`SELECT2_CONTINUE`、`SELECT6`、`SELECT5_CHECK`、`SELECT5_CHECK_SIMPLE`）；
   - 清除 `RetailClientKillTargets.java` 中无引用的 `questsTargeting`、`stageTargets(int, int)`、`contains`；
   - 清除 `RetailSimpleCollectItemDefinitionCompiler.java` 中无调用的 `acceptContinuation`。

## 1. 变更清单与数据比对

| 文件 | 变更性质 | 影响行数 |
| :--- | :--- | :--- |
| `quest_client_handin_exceptions.tsv` (main & test) | 物理整表退役删除 | -15,216 行 (-7,608 × 2) |
| `quest_client_dialog_exits.tsv` (main & test) | 严格缩减 310 行冗余行 | -620 行 (-310 × 2) |
| `quest_client_use_item_report.tsv` (main & test) | 严格缩减 101 行默认行 | -202 行 (-101 × 2) |
| `RetailClientHandinPages.java` | 解耦死表与删除死方法 | -43 行 |
| `RetailClientDialogExits.java` | 清除死常量 | -13 行 |
| `RetailClientKillTargets.java` | 清除死代码方法 | -18 行 |
| `RetailSimpleCollectItemDefinitionCompiler.java` | 清除死方法 `acceptContinuation` | -15 行 |
| **总计** | | **-16,121 行** |

## 2. 门禁验证结果

| 门禁组 | 测试类/方法 | 结果 |
| :--- | :--- | :--- |
| **全家族门禁** | `RunAllFamilyGates` (10 个家族门禁类，37 项测试) | 37/37 全 PASS (100%) |
| **核心门禁** | `ProductionCatalogWhitelistVerificationTest` | 782 OK, 0 Fail, 0 Violation PASS |
| **生产覆盖门禁** | `RetailQuestDriverOverlayTest` | 5/5 全 PASS |
| **真端归属门禁** | `RetailOwnershipGateTest` | 4/4 全 PASS |
| **元数据等价门禁** | `RetailMetadataEquivalenceGateTest` | 1/1 全 PASS |
| **启动覆盖门禁** | `QuestProductionStartupGateTest` | 2/2 全 PASS |
| **静态数据测试** | `XmlDataLoaderTest` | 24/24 全 PASS |
| **TSV 清单门禁** | `RetailTsvManifestGateTest` | 3/3 全 PASS |
| **记忆库三层校验** | `verify_memory_bank.py` | STEPS=3 全部 PASS |
