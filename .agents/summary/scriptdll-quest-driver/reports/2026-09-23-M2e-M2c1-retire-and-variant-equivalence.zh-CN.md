# M2-e + M2-c(批次 1)：退役 286 个 SimpleHunt XML + 同名族等价集

> **口径修正（M3-c，2026-09-23 后续切片）**：本报告的退役动作其后改为**删除**——旧 XML 不再迁移到
> `src/test/resources/quest/retired/`（该目录与 1867 个冻结副本已删除），退役事实只留保留清单
> `owner=RETAIL_TABLE`，历史内容由 git 承担；相关门禁/测试统一改走生产视图与冻结指纹。
> 见 `2026-09-23-M3c-retired-xml-in-git-history.zh-CN.md`。

- 日期：2026-09-23；状态：**实现完成、门禁绿**（未提交；未做服务端重启/实机验收）
- 承上：M2-b（286 个 SimpleHunt 可证 IR 等价）、M2-d（生产接线 + 开关）已在此前切片完成
- 范围：删除已迁移任务的 quest-definition XML 并从 catalog 收敛；修正 Phase 4-2「对齐批次」对可击杀目标的误删；
  建立"同一客户端显示名（`name_id`）家族"的击杀目标等价集，使真端表口径与本服刷怪现实同时成立

## 1. 交付

### 1.1 XML 退役（M2-e）

| 项 | 结果 |
|---|---|
| `git mv` 退役 | 286 个：`quest_definition/quests/<id>.xml` →（测试作用域）`src/test/resources/quest/retired/<id>.xml` |
| `quest_definition_catalog.xml` | 6224 → **5938** 条（删除同 286 条） |
| 生产 quests 目录 | **5938** 个文件（与 catalog 逐一对应） |
| 生产任务全集 | 5938（XML 驱动）+ 286（真端驱动）= **6224** |
| 冻结 IR 指纹 | `src/test/resources/quest/retail-simple-hunt-ir-fingerprints.tsv` **286 行** |
| 退役证据 | `.agents/summary/scriptdll-quest-driver/m2e-retired-xml-evidence.tsv`（id + 原 XML SHA-256） |
| 执行脚本 | `.agents/summary/scriptdll-quest-driver/m2e_retire_migrated_xml.py`（支持 `--dry-run`） |

新增门禁语义：`RetailSimpleHuntEquivalenceGateTest.frozenIrFingerprintsCoverExactlyTheMigratedQuests`
- 冻结集合 ⇔ 退役集合（既不许悬空冻结行，也不许未冻结的退役任务）；
- XML 仍在 → 现算两侧指纹并与冻结行比对；XML 已删 → 用 `retired/` 冻结副本重算；
- 生产 XML 是否存在**看源树**（`src/main/resources/...`），不看 classpath——`target/classes` 会残留已删除资源，
  用 classpath 会把"已退役"误判成"仍在生产"。
- 冻结模式（`-Dretail.hunt.fingerprintOut=<path>`）现在也支持"已退役后用 fixture 重算"，避免只能删前冻结。

新增/复用测试基础设施：`QuestXmlFixtures`（生产 XML 优先 → `retired/` 冻结副本回落）、`RetiredQuestDefinitions`。

### 1.2 同名族等价集（M2-c 批次 1）

问题（证据）：同一只怪在本服常有多个 npc_id，且**客户端显示同一个名字**（同 `name_id`），
而 `QuestEventIndex` 按精确 npc_id 建路由、不做变体展开。例如 Draupnir 副官 `213802`（真端表 id）与本服实刷
`237267` 同 `name_id=315838`；已提交契约 `c8e22de55 fix(quest): align Draupnir NPC variants with live spawns`
正是为此把两个 id 都写进任务 XML。Phase 4-2 的机械对齐（"删除既不在真端表、也不在客户端 CSV 的 id"）
把这类别名删掉，导致击杀无法计数（`QuestDraupnirNpcVariantContractTest` 直接判失败）。

交付：
- `RetailNpcNameIndex` 增加**同名族闭包** `withDisplayNameVariants(...)`：`name_id` 家族大小 2..16 才展开
  （`DISPLAY_NAME_FAMILY_LIMIT = 16`；`name_id=350000` 这类 4324 成员的占位名不展开），家族成员来自仓库 NPC 模板；
- `RetailSimpleHuntPlan.bind` 把真端表解析出的击杀目标按同名族闭包展开 → **真端驱动的任务在实机上可完成**；
- 对拍侧（等价门禁 / 表→IR 编译器门禁）用同一闭包归一化 XML，两侧落在同一等价集上再比较，
  避免把"表达差异"（`KillNpcSet` vs 逐 id `KillNpc`）当成不等价（新增 `RetailKillRoutes`、`RetailKillTargetXml`）；
- 误删回滚：**46 个同族别名 id（26 个文件）**，见 `.agents/summary/scriptdll-quest-driver/restore_equivalence_ids.py`。

### 1.3 服务端可达目标例外台账

Phase 4-2 还删掉了 3 类"本服必须接受但真端表未列"的目标，本切片逐条取证并回滚（11 个 id / 4 个任务）：

| 任务 | 回滚 id | 原因 | 证据 |
|---|---|---|---|
| 1179 | 211099 211135 211153 211960 211961 | `RETAIL_TARGET_UNREACHABLE`：收窄后保留的真端目标 210343 在本服无任何 spawn/handler | 保留集合可达性扫描 |
| 1470 / 24275 | 214621 | `SERVER_SPAWN_VARIANT`：本服火之神殿实刷/使用的是 214621（Kromede 变体） | `FireTempleInstance.java`、`quest_data.xml` |
| 1497 | 211830 211831 212070 212071 | `KILL_DECLARATION`：任务自身 `<kills>` 声明了这些 npc，收窄后声明与击杀转移不一致 | quest XML `<kills>` sequence 1/2 |

- 台账：`src/test/resources/quest/quest-simple-hunt-server-target-exceptions.tsv`（**新增，11 行**，reason + evidence）；
- 生成/复核脚本：`.agents/summary/scriptdll-quest-driver/restore_server_targets.py`；
- `QuestSimpleHuntRetailContractTest` 读台账放行这些 id，并校验 reason 合法、evidence 非空；
- `RetailSimpleHuntIrCompilerTest` 把台账任务单列为"已登记例外"，覆盖率门禁口径改为
  `acceptedCounterGrid + registeredExceptions ≥ 520`（回退仍会失败，登记不会被静默吞掉）。

### 1.4 契约基线复算

- `quest-start-metadata-retail-contract.tsv` 重新生成：**+2 行**（18744 / 28744，此前仅存在于生产目录、漏在基线中）；
  `quest-start-metadata-retail-cap-exceptions.tsv` 无变化；
- 生成脚本 `.agents/summary/quest-systemic-goal/build_retail_contract_tsv.py` 的生产全集改为
  "生产 XML 目录 ∪ 退役冻结副本"，避免重算时丢掉已退役任务。

### 1.5 门禁口径修正（生产全集 = catalog ∪ 退役 fixture）

| 门禁 | 修正 |
|---|---|
| `RetailOwnershipGateTest` | catalog + fixture = 6224；catalog 不得含退役任务 |
| `RetailMetadataEquivalenceGateTest` | 目录条目 + 退役 fixture 条目（`resource=/quest/retired/<id>.xml`） |
| `RetailSimpleHuntEquivalenceGateTest` | 见 §1.1 |
| `QuestSimpleHuntRetailContractTest` | fixture 感知装载 + 同名族 + 例外台账 |
| `QuestRetailStartMetadataGateTest` / `QuestRewardValueGateTest` / `QuestTitleRewardCoverageTest` | 生产视图改为 **catalog + `RetailQuestDriver.overlay(...)`**（退役任务的生产元数据由真端表提供），metadata-only 与 fixture 兜底 |
| `QuestDraupnirNpcVariantContractTest` | 装载回落 fixture；扫描范围扩到 `retired/`（生产全集） |
| 其它历史家族测试（1102 / 1112 / 1400 / 21294 / 1101 等） | 装载统一改为 `QuestXmlFixtures`（生产优先 → fixture 回落） |

### 1.6 文档悬空引用

- `docs/QUEST_CATALOG.zh-CN.md`：**286 行**链接与显示文本改指 `src/test/resources/quest/retired/<id>.xml`，
  并加"迁移说明"；复核后 markdown 悬空引用 = 0（脚本 `.agents/summary/scriptdll-quest-driver/refresh_catalog_doc_links.py`）；
- `docs/quest/WRITING_GUIDE.md` / `.zh-CN.md`：`1112` 示例改指冻结副本路径；
- `docs/quest/client-dialog-mapping/*.csv`：历史审计快照，`quest_xml` 列是"当时的路径 + SHA-256"，
  仓库内消费者（`ReportToManyLegacyFlowRegressionTest`、`LegacyQuestEvidenceOracle`）**不打开该列**（全树跑绿可证），
  按快照保留原值，不当作生产引用。

## 2. 证据与命令

| 层 | 结果 | 证据 |
|---|---|---|
| 退役一致性 | OK | `python3 .agents/summary/scriptdll-quest-driver/verify_retirement.py` → `catalog=5938 directory=5938 retired=286 sum=6224`，无悬空生产引用 |
| 冻结指纹 | 286 行；51 行重算（两侧同时变化） | `-Dretail.hunt.fingerprintOut=/tmp/fp-new.tsv`；对比显示 51 行 xml/retail 指纹**成对变化**，无单侧漂移 |
| 聚焦门禁（本切片 + 生产装载/契约） | **119 用例全绿** | `.agents/summary/scriptdll-quest-driver/gates/m2e-gate-run8.log`（`mvn -o test -Dtest=...`） |
| questEngine 全树 | 1930 用例；35 个失败方法 / 21 个类 | `.agents/summary/scriptdll-quest-driver/gates/questengine-full2.log` |
| 基线归因 | **零新增失败** | HEAD 归档基线（`git archive HEAD` → `/tmp/aionemu-head-1`）跑同一组类：同样 28F+7E；逐方法比对 `only-now = 0` |
| 客户端/运行期 | **未验证** | 未启动服务端、未做客户端实机抽检（本切片不含） |

退役前的专项失败（已全部修掉）：`RetailSimpleHuntEquivalenceGateTest`、`RetailOwnershipGateTest`、
`RetailMetadataEquivalenceGateTest`、`QuestRetailStartMetadataGateTest`、`QuestDraupnirNpcVariantContractTest`、
`Quest1470ClientDialogAlignmentTest`、`QuestKillCounterRetailGateTest.killDeclarationsStayInsideKillTransitions`、
`QuestRewardValueGateTest`、`QuestTitleRewardCoverageTest`、以及 12 个"仍然读生产 XML"的历史家族测试。

## 3. 结论

1. **286 个 SimpleHunt 任务的生产 XML 已退役**：catalog 收敛到 5938，生产目录与 catalog 一一对应，
   等价性由 286 行冻结 IR 指纹在"没有 XML"的前提下继续证明（删除纪律由门禁强制）。
2. **真端驱动在实机上可完成**：击杀目标按客户端显示名（`name_id` 家族）展开，覆盖 Draupnir 这类
   "真端表 id ≠ 本服实刷 id" 的情形；这正是 M2-c「扩大等价集」的第一批（SimpleHunt 族）。
3. **Phase 4-2 机械对齐被纠正**：46 个同族别名 + 11 个服务端可达目标回滚；
   剩余 66 个"不同名且真端/客户端都未列"的删除保留（收窄符合真端权威，且收窄后保留集合在本服均可达）。
4. **本切片对 questEngine 全树零新增失败**（与 HEAD 基线逐方法一致）；历史 21 个失败类在 HEAD 即失败，
   属既有债务，另列清单跟踪（见 §5）。

## 4. 未验证 / 边界

- 未跑服务端启动与客户端实机验收（任务可接取/计数/报告/奖励未做真机确认）；
- 同名族闭包是**生产行为变化**：真端驱动的击杀计数现在接受同显示名的兄弟 id（家族 ≤16）。
  依据是"客户端显示同一名字"这一玩家可感知口径；未做客户端 UI 侧逐任务比对；
- `mvn` 全量 `test`/`package` 未执行（仅跑 questEngine 选择器）；未提交、未推送；
- 记忆库沉淀（`.agents/memory-bank/*`）本轮**不写入**：该目录正被并发工作占用（`index.jsonl`、`patterns/*` 有未提交改动），
  待并发提交后再把"同名族等价集"作为跨域不变量登记，避免覆盖他人编辑。

## 5. 阻塞与既有债务（非本切片引入）

`questEngine` 全树在 HEAD 即失败的 21 个类（35 个方法，基线可复现）：Miragent、MissionItemConsumption、
Quest1926And2938、Quest21114、Quest25512、Quest28808、Quest3057、Quest50019/51019、QuestArchDaevaPromotion、
QuestItemSourceContractGate、QuestKillCounterRetailGate(15101)、QuestRetailCollectionRoleAlignment、
QuestWorkItemMigrationCoverage、ReportToManyMirror、QuestHandoverContinuationAudit、QuestVars、
QuestCounterProjectionLockFollowUp、QuestInteractionObjectCatalog、QuestLegacyMonsterHuntProductionFlow、
QuestStepNpcSkipGuard。这些必须在后续切片单独定性（是否已被并发工作修掉 / 是否属真缺陷）。

## 6. 下一步

1. **M2-c 收口**：145 个 spawn 名多 id 消歧（把同名族闭包从 SimpleHunt 推广到其它族与 `RetailNpcNameIndex` 的
   解析失败场景）、21 个名解析失败、13 个 6 位溢出登记保留；
2. **M3 SimpleTalk（2223）**：先做 20 个代表任务的语义闭环（`FUN_180cab520` 单步 / `FUN_180cabb10` 状态链 /
   `FUN_180caca90` 报告），再全族；
3. 每族迁移前先跑 `RetailOwnershipGateTest` + 家族等价门禁 + 退役脚本 `--dry-run`，再执行删除。
