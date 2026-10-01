# QE-112 落地批：DD 纯 hunt 行的真端行阶梯（4 行出仓 + 旧 IR 车道删旧）

日期：2026-10-01　分支：`quest`　基线：P7 前置裁定批 `cca544bbb`　性质：**实现 + 裁定 + 死码删除**

## 1. 问题与根因

DD 表里 4 行（3122/3123/4122/4123）长期停在 `XML_RETENTION /
ADJUDICATED:RETAIL_HUNT_MULTI_STAGE_DEFERRED`，理由写作「stages=6 exceeds the 5-slot layout cap」。

根因是**形状误判**：旧模型把真端 progress 块的「段（`;` 分隔的并行目标）」当成 `SECTION` 槽位，
于是「5 槽」成了硬上限。真端的 DD hunt 形不是这样：

- **行阶梯**：`var0` = 进度行号（客户端 `SECTION_0`），`var1..var4` = **当前行**的段计数
  （客户端 `SECTION_1..4`）；行数受 6 位行号限制（≤63），每行 1..4 段、每段计数 1..63；
- 3122/3123/4122/4123 = **6 行 × 每行 1 杀**，只需行号 + 一个计数两个字段，旧「5 槽上限」根本不存在；
- 客户端任务书 `quest_monster.csv` 对 DD hunt 行一律登记
  `Progress(SECTION_0==k; SECTION_m<count)`（段号恒为 1..n），与真端 `FUN_180c46020`
  的「步号 + 4×6 位组槽」逐指令一致。

## 2. 证据（双源复算，工具化）

工具：`.agents/summary/quest-dd-hunt6-ladder/tools/dd-hunt-ladder-probe.py`（只读）；
台账：`.agents/summary/quest-dd-hunt6-ladder/qe-112-quest-ddhunt-ladder-decisions.tsv`。

| 项 | 值 |
|---|---|
| 真端纯 hunt 行的进度块 | 842 行（含 4 个翻转行） |
| 客户端声明行阶梯的行 | 842 行 |
| 真端块段数 ≠ 客户端行阶梯的行 | **55**（家族口径「客户端计数为权威」⇒ 按客户端行编译，逐行登记 `SHAPE_DIFF`） |
| 翻转 4 行 | 3122/3123/4122/4123：真端 6 个进度块（首块 1 段、其余由客户端行补足）+ 客户端 6 行 × 1 杀 |

## 3. 落地内容

| 面 | 改动 |
|---|---|
| 合成器 | `RetailSimpleHuntDefinitionCompiler.compileClientLadder(...)`（新）：行阶梯布局、行节点只钉行号、每段「未满自环 +1（优先级 1）+ 本行收口（优先级 0，推进行号；非末行清零本行计数，末行满计数留在领奖投影 QE-051）」；同批把接取/报告/完成段抽成 `assembleRoutes(...)` 供网格/串行/阶梯三形共用（既有形逐字节不变） |
| DD 路由 | `RetailDataDrivenDefinitionCompiler`：纯 hunt 行（`allHunt()`）改走 `compileClientLadder`，**删除「>5 段即拒」的硬上限**；PVP 行仍走既有单计数网格；宽计数（>63，如 80817）仍走既有分支（见 §5） |
| 4 行出仓 | 台账 `XML_RETENTION/ADJUDICATED:RETAIL_HUNT_MULTI_STAGE_DEFERRED` → `RETAIL_TABLE/OK`（main+test 双副本，note 记 `qe-112-quest-ddhunt-ladder-decisions.tsv basis=DD_HUNT_CLIENT_LADDER`）、删 `quests/{3122,3123,4122,4123}.xml` 与目录四条 `<definition>`；漂移登记 `ADOPTED 1463 → 1467`（`REJECTED:RETAIL_HUNT_MULTI_STAGE_DEFERRED` 直方图清零） |
| 客户端行阶梯门 | `RetailDataDrivenGateTest#pureHuntRowsFollowTheClientRowLadder`（新，**客户端行驱动**）：对每个「已退役 ∧ 客户端声明进度行 ∧ 纯 hunt ∧ 非 PVP」的行，从 `quest_monster.csv` 反推行阶梯并用 `RetailHuntLadderShape.assertLadder` 逐行复算（布局/节点/击杀边配对/饱和行走/领奖投影），冻结覆盖 **595 行** |
| 夹具 | `RetailHuntLadderShape`（新，测试夹具）：行阶梯形状 + 饱和行走合同，供 DD/hunt 族回归共用 |
| 指纹重冻 | `retail-data-driven-ir-fingerprints.tsv` 40 行随新形重冻（20 个 光明侧 + 19 个 黑暗侧多段单行行 + 80817），节点/转移数不变，**语义由上面的客户端门守** |
| 旧金标重锚 | `QuestDataDrivenRetailFlowAlignmentTest`（25321 改由生产驱动取定义 + 按客户端行阶梯逐行断言）、`QuestDataDrivenHuntProductionFlowTest`（16805/26805/16807/26807 改由客户端行阶梯驱动 + 行阶梯走一遍 N 杀收口进领奖态） |
| **同批删旧**（计划 §10.3-#1） | 删 `RetailQuestDriver.compileSimpleHunt` / `compileSimpleSerialHunt` / `RetailSimpleSerialHuntTable` 及其装载与字段/参数、`SIMPLE_SERIAL_HUNT_TABLE` 常量、`RetailSimpleSerialHuntGateTest`（旧串行族金标）；家族分派只剩 DataDriven |

## 4. 门态与回归

| 门 | 结果 |
|---|---|
| `RetailDataDrivenGateTest` | **9/9 绿**（含新增客户端行阶梯门；指纹门随本批重冻转绿） |
| `QuestDataDrivenRetailFlowAlignmentTest` / `QuestDataDrivenHuntProductionFlowTest` | **2/2 + 4/4 绿**（旧形断言按客户端行阶梯重锚） |
| 族门 + tablelane（显式 18 类命令） | **138/138 绿**（删旧后无表/无金标残留） |
| 聚焦套件 | **1699 例 / 161F+137E / 105 红类**；对上一批基线 **ADDED 0 / REMOVED 3**（`QuestDataDrivenRetailFlowAlignmentTest`、`QuestDataDrivenHuntProductionFlowTest`、`RetailDataDrivenGateTest` 转绿）、changed 4（含 `RetailSimpleSerialHuntGateTest` REMOVED：死码金标退场） |

日志：`gates/2026-10-01-qe112-family-tablane.log`、`gates/2026-10-01-focused-run-qe112{,-red-classes,-delta}.tsv`。

## 5. 边界与未做

- **80817（100 杀）不在本批语义裁定内**：它仍走既有宽计数分支（历史形，非本批引入）。§10.3-#5 已裁定
  P7 原生车道必须**原样复刻真端 6 位组槽算术（含第 64 杀回绕、真端自身不可完成）**，禁止 10 位相机与显式禁用；
  本批只把它的冻结指纹随形重冻（P7 前的中转形态）。
- **中性化的旧族金标**（`RetailSimpleHuntEquivalenceGateTest`、`QuestSimpleHuntRetailContractTest`、
  `CounterChainBriefingStageContractTest`）仍以 native 守卫短路；其车道已无生产入口，随 P8 typed 车道删除退场。
- `compileSimpleHunt` 的删除使 `RetailSimpleHuntTable`/`RetailQuestCatalog.simpleHuntPlan` 只剩 DD 车道消费者
  （DD 混合链与阶梯都用它），因此保留。
- 客户端三表/`quest_monster.csv` 证据在仓库外或客户端包内复算；仓内只落工具 + 台账 + 门。

## 6. 下一步

- **§10.3-#2 P0a 重冻**：本批落地后重跑 `p0a/tools/owner_identity.py` + `p0a/tools/raw_vars_probe.py`，
  重出 `owner-identity.tsv`（P0a 报告已注明「正式冻结须等 QE-112 落地」）。
- **P7 DataDriven 切换批**：按 `CLIENT_PRESENT ∧ owner RETAIL_TABLE` 1467 行切原生车道（含 80817 的 6 位溢出算术复刻），
  同批删 DD 编译器与 zone/AI 台账读取（§10.3-#14）。
