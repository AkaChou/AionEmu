# M5-b2：SimpleCollectItem 真端驱动漂移判定 + 首批 XML 退役


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-23
- 切片：M5-b2（SimpleCollectItem 家族，真端 `Quest_SimpleCollectItem.xml` 263 行 / 生产宇宙 178 行）
- 结论一句话：**口径已确定（判据是真端语义 + 客户端契约，不是"合成 IR 必须等于 XML IR"）；本族可驱动从 12 行提升到 89 行（全表 150 行），首批退役 29 个 XML，其余 60 行带差异轴登记留 XML 等下一轮。**

---

## 1. 目标与判据（口径确认，非漂移）

用户口径（2026-09-23）：**用真端任务信息 + ScriptDLL64 语义替换现有任务系统（替代 quest-definition XML）**；XML 只作对照物，不是金标准；真端表达不了的才保留 XML。

本切片据此把判据固化为三层证据：

| 层 | 来源 | 用途 |
|---|---|---|
| 真端模板表 | `quest_retail/Quest_SimpleCollectItem.xml`（`Map/XML` 入仓副本） | 接取 NPC / 采集对象 `objectN` / 报告 NPC / `con_quest` |
| 真端元数据 | `quest_retail/quest.xml` | 交付物与数量、奖励档、等级上下限 |
| 客户端契约 | `quest_client_dialog_exits.tsv`（SELECT1_1/SELECT6）、`quest_client_summary_rows.tsv`（任务书末行） | 真端表**没有**的对话页链与领奖行投影 |

> 与"XML 审计"路线的区别：XML 的 `REWARD/1`、`SET_SUCCEED(10255)`、`FINISH_DIALOG→关窗` 等写法在本切片被当作**待定性的差异轴**逐条登记，而不是必须复现的规范。

## 2. ScriptDLL64 反编译证据（本切片新增）

真端二进制：`${AION_RETAIL_ROOT}/MainServer/ScriptDLL64.dll`（77 MB，ImageBase `0x180000000`）；反编译源码：`58Server/server58/MainServer_ScriptDLL64/ScriptDLL64.c`。

1. **每任务一个 trampoline → 家族共享处理点**：真端为每个 SimpleCollectItem 任务注册一个跳板，例如 1103（`0x44f`）→ `FUN_180d46660`，函数体为
   `(**(code **)(lVar1 + 0x1b8))(param_1, uVar2, 0x44f, *puVar5, *puVar4, *puVar3);`
   三个数据指针分别取自上下文 `+0x18 / +0x38 / +0x48`，任务 ID 是编译期常量。同族其它任务（`0x52d0`、`0xa0cc`、`0x8c6` …）走**同一偏移** `+0x1b8`。
   → 家族语义是**共享实现**，逐任务的差异只能来自数据表（`Quest_SimpleCollectItem.xml` / `quest.xml`），这为"表驱动替换 XML"提供了直接依据。
2. **PE/RTTI 虚表普查**（工具 `m5b2_vtable_scan.py`）：整 DLL 只有 31 张 RTTI 虚表，`SimpleCollectQuest` 19 槽、`SimpleUseItemQuest` 20 槽、`SimpleItemPlayQuest` 20 槽、`SimpleTalkQuest`/`SimpleHuntQuest`/`SimpleSerialHuntQuest` 各 19 槽、`IScriptDLLImpl` 12 槽；`QuestProgressExtraInfo_*` 系列各 2 槽。
   → 跳板里的 `+0x1b8` 不落在这些类的虚表内，说明它是**上下文结构里的处理入口字段**（而非某任务的 C++ 虚函数）；因此**不存在"每任务一份对话页脚本"**，页链只能来自客户端与数据表。
3. **真端就是文件驱动**：DLL 内含 `DataDrivenQuestLoader` 整套报文（`quest_data_driven`、`progress_info`、`T_PROGRESSn_CATEGORY`、`con_quest_list`、`acquired_quest_condN`、`finished_quest_condN`…）与进度类别名（`CollectItem`/`Hunt`/`ItemPlay`/`Talk`/`PvP Kill`/`Enter Sensory Area`/`Enter World`/`LevelUp`/`Talk FOBJ`），并创建 `QuestProgressExtraInfo_CollectItem` 等类型对象；额外动作类别含 Give/Remove Item、Teleport To、Play Cutscene、Spawn Npcs、Delay Time、Message、Enter Instance、Add Timer。
   定位：`FUN_180c4b330`（LoadProgressInfo，引用 `T_PROGRESSn_CATEGORY`）、`FUN_180c48030` 区间（`quest_data_driven`）、`FUN_180c4b980`（LoadExtraAction）。
   → 与本仓库"真端表 + 客户端契约 → Java 定义"的路线同构；`data_driven_quest.xml` 族群（1508 行 `FAMILY_PENDING:DataDriven`）下一步可直接照这套类别语义落地。
4. **客户端动作面对齐**：DLL 不含任务页/动作字符串；动作面证据来自客户端解包（`docs/quest/client-dialog-mapping/`）。本族客户端动作实测只有 `ASK_QUEST_ACCEPT / QUEST_ACCEPT_1 / QUEST_REFUSE_1 / CHECK_USER_HAS_QUEST_ITEM / FINISH_DIALOG / SELECT1_1(SET_SUCCEED=10255) / SET_SUCCEED`。

工具与中间产物（可复算）：
`.agents/summary/scriptdll-quest-driver/{m5b2_pe_probe.py, m5b2_vtable_scan.py, m5b2_c_func.py, m5b2_xref_scan.py, m5b2_region_xref.py, m5b2_refscan.py, scriptdll64-quest-strings.txt, scriptdll64-quest-xrefs.txt}`

## 3. 本切片做了什么

### 3.1 合成器修复（`RetailSimpleCollectItemDefinitionCompiler`）

| 问题 | 症状 | 处理 |
|---|---|---|
| 同一（源节点, 事件）重复转换 | 132 行 `COMPILATION_FAILED / AMBIGUOUS_TRANSITION`（接取 NPC == 报告 NPC 时，接取流与报告出口都登记 `started + FINISH_DIALOG`） | 按 `RetailSimpleTalkDefinitionCompiler#reportNpcExit` 成熟口径加守卫：两 NPC 相同时不重复登记 |
| 交付失败页写死 SELECT6 | 客户端没有 `select6` 的任务会下发不存在的页 | 改为消费 `RetailClientDialogExits.SELECT6`：有则下发 select6，无则关窗 |
| 缺接取续页 | 客户端 `select1` 带 `HACTION_SELECT1_1` 的任务按钮无路由 | 新增 `acceptContinuation`：按 `SELECT1_1`/`SELECT1_1_1` 登记续页路由 |
| 领奖行投影写死 0 | 与客户端任务书末行（QE-051）不一致 | 改为 `RetailClientSummaryRows.lastRowIndex(questId)` |
| 家族未接线 | 新类未进生产 | `RetailQuestDriver` 增加 `SimpleCollectItem` 族加载/分派/缓存；保留清单开关不变 |
| 缺领奖行修复路由 | 多行任务书任务（2542 等）旧存档 var0 停在 0 无法自愈（`JournalRewardRowRepairContractTest`） | 新增 `journalRowRepair`：末行行号 > 0 时登记 `enter-world` + `status=REWARD` + `var0=0 → 末行`（与领奖投影同源；单行任务不登记） |

### 3.2 漂移登记（逐任务，含差异归轴）

- 生成器 = 门禁本体：`RetailSimpleCollectItemGateTest#driftVersusLegacyXmlIsRegistered`
  （重算：`-Dretail.collect.equivOut=<path>`；指纹冻结：`-Dretail.collect.fpOut=<path>`）
- 落盘：`src/test/resources/quest/retail-simple-collect-item-drift.tsv`（178 行；`EQUIVALENT` / `DIFF:<轴…>` / `REJECTED:<码>`）
- 明细证据：`retail-simple-collect-item-drift-detail.tsv`（全表 263 行）+ `retail-simple-collect-item-diff-lines.tsv`（逐行差异原文）

家族结果（178 行生产宇宙）：

| 分类 | 行数 | 说明 |
|---|---|---|
| `DRIVABLE` | 89 | 合成成功，语义不变量全过 |
| `REJECTED:RETAIL_ACQUIRE_NPC_SENTINEL` | 40 | 真端接取名是类别哨兵（`_faction_`），无 npc id，不可推导 |
| `REJECTED:RETAIL_COLLECT_SELECTABLE_REWARD` | 25 | 奖励档含可选项（需显式 choice 路由） |
| `REJECTED:RETAIL_COLLECT_ITEM_SHAPE` | 24 | `quest.xml` 交付物不是唯一一条（多物品收集） |

差异轴分布（89 个可驱动行，按任务数）：

| 轴 | 行数 | 定性 |
|---|---|---|
| `REWARD_ROW`（领奖行 0 vs 1） | 86 / 62（两侧） | **XML 陈旧**：客户端任务书只有 1 行 → QE-051 要求 var0=0，XML 写 1 |
| `VAR0_FIELD`（宽度 6 vs 1） | 59 / 59 | 值域等价（0..1 ⊂ 0..63），不影响客户端槽位投影 |
| `DIALOG_1008`（`FINISH_DIALOG` 回应） | 83 / 79 | 家族默认 = 下发任务列表页（page 10）；XML 覆盖为关窗（无客户端证据支撑，登记待复核） |
| `DIALOG_10` | 10 / 9 | 同上（选择页出手） |
| `DIALOG_39 / 20002` | 5 / 5 等 | 两个按钮互为别名，`select1_1(SET_SUCCEED)` 之外的客户端按钮均已登记 |
| `ROUTE` / `OTHER` | 59 / 9 | **未定性**：采集对象路由的额外 after-commit、`SET_SUCCEED(10255)` 等 → 保留 XML |

### 3.3 首批退役（29 个 XML）

放行规则：`DRIVABLE` 且差异轴全部落在"客户端证据已判定 XML 陈旧"的集合内
（`REWARD_ROW / VAR0_FIELD / DIALOG_10 / DIALOG_1008 / DIALOG_1009 / DIALOG_39 / DIALOG_20002 / DIALOG_1012 / DIALOG_1013`）。

- 集合：1103 1125 1144 1154 1155 1168 1172 1177 1178 1219 1411 1618 1623 1630 1641 2104 2109 2116 2119 2126 2203 2246 2257 2269 2503 2542 2554 2602 24111（29 个）
- 工具：`m5b2_retire_collect_item_xml.py`（删 XML + 移除 catalog 行，幂等；证据 `m5b2-retired-collect-item-evidence.tsv`）
- 保留清单生成器 `build_retention_list.py` 新增 SimpleCollectItem 分支：`IMPLEMENTED_FAMILIES` 加入该族；
  `REJECTED:*` → `SEMANTIC_GAP:<码>`；差异轴含 `ROUTE/OTHER` → `SEMANTIC_GAP:CLIENT_ROUTE_VARIANT`（记 `unreviewed-axes=`）；
  其余 → `RETAIL_TABLE/OK`。

## 4. 验证与证据

| 门禁 / 校验 | 结果 |
|---|---|
| `RetailSimpleCollectItemGateTest` | **5/5 绿**（家族规模 178、可驱动下限 89、语义不变量、漂移登记、冻结指纹 + 退役集合一致） |
| `RetailQuestDriverOverlayTest` | 2/2 绿（overlay 真端替换生产条目） |
| `RetailQuestCatalogTest` | 2/2 绿 |
| `RetailOwnershipGateTest` | 4/4 绿 |
| `QuestClientContractGateTest` | 1/1 绿（生产视图 = XML + overlay 的客户端页/按钮契约） |
| `QuestPageButtonAuditTest` | 2/2 绿 |
| `verify_retirement.py` | `catalog=3837 directory=3837 retired=2387 sum=6224 — OK`，无悬空引用 |
| 可驱动数量 | 12 → **89**（本族生产宇宙）；全表 263 行 → 150 |
| 全树对账（首次） | 1945 例 / 28F+11E：4 个新增 error 全部来自"测试直接读已退役 XML / 只认旧 XML 行为" → 已修（见下） |
| 全树对账（修后） | `mvn -o -Dtest='com.aionemu.gameserver.questEngine.**' test` → **1945 例 / 28F+7E / 35 个方法**，与干净 HEAD 基线**逐方法一致**（only-now=0、only-base=0；`gates/m5b2-questengine-failures.txt`） |

### 4.1 因退役而需要跟进的测试（本轮已修）

| 测试 | 现象 | 处理 |
|---|---|---|
| `JournalRewardRowRepairContractTest` | `missing quest definition 2542.xml`（直接读生产 XML 资源） | 改走 `ProductionQuestDefinitions.definition(...)`（生产视图 = XML + overlay） |
| `QuestProductionJourneyTest` | 其 catalog 只含 XML 目录 → 1103 找不到定义；改用生产视图后又断言旧 XML 的"FINISH_DIALOG 关窗" | catalog 改 `ProductionQuestDefinitions.catalog()`；FINISH_DIALOG 断言改为家族规范口径（任务列表页 10），并注明覆盖率为漂移轴 `XML_EXTRA:DIALOG_1008` |
| 冻结指纹 | 新增领奖行修复路由后 2542 指纹变化（1/29 行） | 复算冻结基线（变更原因即上述路由，登记在 §3.1） |

关键数字：catalog 3866 → **3837**；`RETAIL_TABLE` 2358 → **2387**；本族退役 29；冻结指纹 29 行。

## 5. 未验证 / 边界

- **未做真机/真客户端联机验收**：门禁只覆盖静态契约（页面存在性、按钮路由、IR 语义不变量），不覆盖运行时对话流。
- `FINISH_DIALOG→page 10` 与 XML 的"关窗"差异按家族默认放行，属**登记待复核**，不是已验证结论。
- `ROUTE/OTHER` 轴未定性：涉及对象路由 after-commit、`SET_SUCCEED(10255)` 可达性，需客户端页链/NPC 级证据。
- 多物品收集（24 行）与可选奖励（25 行）本轮按稳定码拒绝，仍走 XML。

## 6. 下一步

1. **M5-b3**：对 60 个带 `ROUTE/OTHER` 轴的行逐条定性（对象路由 after-commit 是否客户端必需、`10255` 是否该 NPC 可达），定性完即可放行第二批。
2. **M5-c**：`SimpleUseItem` / `SimpleItemPlay` / `SimpleSerialHunt` 三族照本切片流程推进（表已在仓）。
3. **M6-pre**：`DataDriven` 族（1508 行）——已从 ScriptDLL64 拿到类别语义（`QuestProgressExtraInfo_*` + ExtraAction 列表），可按同构方式落地。
