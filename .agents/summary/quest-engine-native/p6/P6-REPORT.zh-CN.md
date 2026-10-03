# P6 报告：CombineTask 原生直驱切换（配方端口 + 同批删旧 + 放弃面收口）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 主题：按计划 §7 P6 把 CombineTask（**574 行**）切到真端表 + 真端 `quest.xml` + 生产配方表直驱，
> 与旧 IR 车道一刀两断（同批删旧）；并收口本批暴露出来的 native 车道**共享放弃面**缺陷（QE-119）。
> 日期：2026-10-01。分支：`quest`。前置：`p6/P6-STEP1-REPORT.zh-CN.md`、`p3-prereqs/family-table-shapes.md` §2.7。
> 口径：本族行为只读真端 `Map/XML/Quest_CombineTask.xml`、`Map/XML/quest.xml`、生产物品名索引
> （`RetailItemNameIndex`）与配方表（`recipe_templates.xml`）；不生成 IR 节点、不回退旧编译器、不发明
> id/页/物品；未决一律 fail-closed。

---

## 1. 交付：装载 + 原生处理器 + 配方端口 + 引擎分流

| 组件 | 变更 |
|---|---|
| `tablelane/SimpleCombineTaskHandler`（新增） | 本族唯一处理器：① 接取 = 接取 NPC（真端 `task_npc` 双名）的 31/26 问询 → 客户端契约入口页（本族 574 行零登记 ⇒ 回落真端接取窗页 4），1002/20000 经 `NativeQuestStartPort` 建档 START + 按表序发 `give_component1..8` + 学配方（1002 → 页 1003，20000 → 收窗），1003 → 页 1004、1004/20001 → 收窗；② 交付 = 交付动作 `1009`：持有产物（真端 `product` 符号 × 数量）→ 回收剩余分量（`RemoveItem(component, ALL)`）→ 翻 `REWARD` → 页 5；缺产物 → 真端回退页 `SELECT3_2`(1779)，状态与背包零变更；③ 领奖 = 奖励窗重开面（31/26/1009/-1）+ 确认区间（8..23 / 108 / 110..124）→ `NativeReportRewardFlow`（真端 reward 列 → 共用结算体）→ **完成后再走真端条件回收段**（扣产物 + 忘配方）→ 页 1008；④ `abandon` = 族级动作（忘配方），共用清理段留给 `QuestService` |
| `tablelane/NativeRecipePort`（新增） | 配方学习/忘记的唯一出口（生产实现 `RecipeListRecipePort`：`RecipeList` + `SM_LEARN_RECIPE`/`SM_RECIPE_DELETE`），静态数据里不存在的配方 id **fail-closed**（不落库、不假装学会）；与 P3 起的 `NativeInventoryPort` 同风格，处理器不直触配方表/DAO |
| `QuestEngine` | `hasMatchingRoutes`/`onDialog`（itemplay 之后、typed 之前）两处原生分流 + 启动期 `installInterest`；新增 **owner 面**：`isNativeOwner`（七族 routes 并集）/`nativeMetadata`（真端 `quest.xml` 行，缺行或不干净即 empty）/`hasNativeAbandonRoute`/`onNativeAbandon`（族级动作分发） |
| `QuestService` | `abandonQuest` 按 owner 分三支：native（真端 `cannot_giveup` + `quest_work_item*` 元数据 → 族级动作 → 共用清理段：状态复位 + 工作物品回收）/ typed production（IR abandon 边）/ legacy；修复「native 行无 typed 元数据 ⇒ `canAbandon` 恒 false」的跨族回归（QE-119） |
| `retail/RetailQuestDriver` | **切断** `compileCombineTask`，删除 `RetailCombineTaskTable`/`RetailRecipeIndex` 两字段与其装载；retention 分支 CombineTask 改判 native owner；`verifyProductionCoverage` 的原生覆盖面补 `SimpleCombineTaskHandler`（否则 574 行被判 `missing` ⇒ 生产目录覆盖门失败）；`RetailQuestCatalog` 去掉 CombineTask 判定面 |

**同批删旧（`git rm`）**：`retail/RetailCombineTaskTable`、`retail/RetailCombineTaskDefinitionCompiler`、
旧金标 `RetailCombineTaskGateTest`、`test/resources/quest/retail-combine-task-ir-fingerprints.tsv`。
**本批结束后本族无并行可用双路径。**

## 2. 真端语义落点（与退役 typed 形状的两处有意差异）

真端 helper（`f731:1355 FUN_180caac10` 一系 + 每任务 thunk）给出的**段落顺序**落进 native 车道后，与旧
typed 定义有两处结构性差异，均为「按真端」而非等价迁移：

| 段 | 退役 typed 形状 | native（本批，真端） |
|---|---|---|
| 接取 | `QUEST_SELECT` 直发接取窗（页 4）+ `GiveItem(分量)` + `LearnRecipe` | 接取 NPC 的 **31/26 问询** → 契约入口页 → 1002/20000 提交后建档 + 发分量 + 学配方（真端 `cab520` 页链：1003→页 1004、20001→收窗） |
| 完成 | `RemoveItem(产物, ALL)` → `ForgetRecipe` → `CompleteQuest(0)`（先回收再完成） | `NativeReportRewardFlow` 结算（= 真端 `+0x100 SetQuestSuccess`）**之后**再走真端条件回收段（扣产物 + 忘配方）——真端完成流是「先写任务成功、再跑 RemoveItem 循环」 |

其余段落与退役形状同源：交付门 = 持有产物（`HasItem` ↔ 真端产物列）、交付成功回收剩余分量、缺产物回退
`SELECT3_2`、放弃清配方。

## 3. 行集与路由口径（真端表事实，逐行冻结）

| 表行 | 退役（RETAIL_TABLE） | NATIVE_READY | 注册集（owns） | 路由集（routes） | 不可路由 |
|---|---|---|---|---|---|
| 574 | 574 | **574** | 574 | **574** | **0** |

- 逐行判据（`CombineTaskRowAlignmentGateTest` 独立正则重解析复算，不复用 DOM 装载器）：双 `task_npc` 名唯一
  ∧ `combineskill` 符号可解 ∧ 产物单槽可解 ∧ 分量非空且全可解 ∧ `(skill, product)` 在生产配方表**唯一**命中
  ∧ 真端元数据干净 ∧ 退役 ∧ 非 XML-only ⇒ 路由；该判据复算集与处理器路由集**逐元素相等**（ADDED 0 / REMOVED 0）。
- 接取入口页：574 行在客户端任务页索引里**完全无登记** ⇒ `QuestDialogContract.acceptEntryPage` 回落
  真端接取窗页 4（`CombineTaskRowAlignmentGateTest` 双向冻结该事实与行集）。

## 4. typed 目录退出后的旧金标重锚（6 处）

| 类 / 代码 | 重锚内容 |
|---|---|
| `RetailQuestDriver.verifyProductionCoverage`（生产门自身） | 原生覆盖面补 CombineTask；否则 574 行在「已退役且不在 typed 目录」时被判 `missing`，生产目录覆盖门失败（本批实测：未补时 `ProductionQuestDefinitions.catalog()` 每次调用都重新编译整个 XML 目录并抛错，复现方式见 §7-②） |
| `RetailQuestDriverOverlayTest` | native 计数与负例筛选扩 CombineTask；`6224 == production.entries + migratedNativeCount` 等式保持 |
| `QuestRewardItemGateTest` | `nativeOwned` 扩 CombineTask（奖励面改判真端 `quest.xml` 奖励列轴） |
| `RetailClientAcceptEntryPageTest` | 原生车道分支扩 CombineTask（lane/routes/接取 NPC）；新增**全局接取窗页**口径：CombineTask 574 行零登记 ⇒ 不算「任务页失同步」gap，且该行集必须逐元素等于本族路由集；fail-closed 原生行集合保持 `{30720,30723}` |
| `RetailRewardWindowRouteTest` | 原 typed 形状断言对象（CombineTask 5000，P5 时是唯一未切换家族）已退场 ⇒ 改为**生产目录全量扫描不变量**：任务域全局 `AUTO_REWARD` 路由每任务至多一条、奖励窗动作不产生同域重边（扫得 0 命中并显式打印，避免静默空断言） |
| `QuestInteractionObjectContractGateTest` | 可执行定义下限 2700 → **2100**（P6 实测 2183；下限仍能拦住目录塌成空壳） |

## 5. 门禁与实测

| 门 | 命令 | 结果 |
|---|---|---|
| 族门 + tablelane 全量 | `mvn -o test '-Dtest=*FamilyGateTest,*RowAlignmentGateTest,*InventoryGateTest,NativeQuestTableLoaderTest,*RewardClaimGateTest,*NativeTalk*Test' -DfailIfNoTests=false` | **108/108 绿**（新增 `SimpleCombineTaskNativeFamilyGateTest` 11/11、`CombineTaskRowAlignmentGateTest` 4/4；余为前六族与前批回归）；`gates/2026-10-01-p6-family-tablane.log` |
| 聚焦套件 | `mvn -o test '-Dtest=*Quest*Test,*Retail*Test' -DfailIfNoTests=false` | **1689 例 / 162F+142E / 108 类红**；对 P6 步骤 1 基线（1692 / 162F+142E / 108 类）**ADDED 0 / REMOVED 1**（仅旧金标 `RetailCombineTaskGateTest` 3 例随同批删旧退场）；`gates/2026-10-01-focused-run-p6.log`、`-red-classes.tsv`、`-delta.tsv` |
| 生产目录覆盖门 | `mvn -o test -Dtest=QuestProductionStartupGateTest,RetailQuestDriverOverlayTest` | **2/2 + 5/5 绿**（`6224` 等式与原生覆盖面成立） |

## 6. 本批发现并收口的两个跨族缺陷

### 6.1 native 行的放弃面恒不可达（QE-119，已闭环）

`QuestService.abandonQuest` 第一步取 `questCatalog().findMetadata(questId)`——那是 **typed IR 目录**；已切
native 的车道行不在目录里 ⇒ metadata 为 null ⇒ `canAbandon` 恒 false ⇒ 玩家点「放弃任务」无反应，且写在
handler 里的族级放弃动作（CombineTask 忘配方）**永不可达**。该缺陷对 P1-P6 全部已切换族成立（不是 P6 独有）。

落地：`QuestEngine.isNativeOwner/nativeMetadata/hasNativeAbandonRoute/onNativeAbandon` + `QuestService` 按
owner 分流（native / typed production / legacy）+ 真端元数据轴（`cannot_giveup`、`quest_work_item*`）。
断言面：引擎级 owner 与元数据双面、handler 族级动作逐元素（注入假配方端口）、`QuestService` 源码接线锁。
沉淀见 memory-bank `QE-119`。

### 6.2 生产目录覆盖门漏掉新切换族（本批实测出的门态缺陷）

`RetailQuestDriver.verifyProductionCoverage` 的原生覆盖面是**逐族硬编码**的 handler 清单；P6 切族后未补
CombineTask 时，574 行（已退役 ∧ 不在 typed 目录 ∧ 未被原生面认领）被判 `missing`。实测表现不是一次干净的
失败，而是 `ProductionQuestDefinitions.catalog()` 缓存永远写不进去 ⇒ 每个 `definition(id)` 调用都重新编译
整个 XML 目录，测试走进「每秒重建一个编译线程池」的活锁（`jstack` 逐帧定位：`main` 停在
`QuestDefinitionCatalogManifest.compileResourceEntries` 的 `future.get()`，线程池编号持续递增）。
补上 CombineTask 后 `QuestProductionStartupGateTest`/`RetailQuestDriverOverlayTest` 恢复绿。
**教训**：切族批必须把「生产目录覆盖门」与「原生覆盖面清单」列进同批清单，且覆盖门失败时的失败形态要看
`catalog()` 是否可缓存（一次干净异常 vs 活锁）。

## 7. 边界与残余

| # | 项 | 现状 | 处置 |
|---|---|---|---|
| ① | 代表任务真实客户端验收 | **PENDING_CLIENT** | 本批不得标记「族完成」；取 5000（或同形行）跑一次真实客户端接取→交付→领奖+放弃 |
| ② | 放弃段**全路径** | 引擎级 + handler 级 + 源码接线已实测；完整链路（在线玩家控制器 + 背包 + DB 配方表）未在单测栈执行 | 随 ① 一并验收；如失败先查 `RecipeListRecipePort` 的 DAO 面 |
| ③ | `con_quest` 链式接取窗 | 本族表**无**该列（574 行零声明），不适用 | 与 P1-P5 的 `con_quest` 残余（§10.3-#16-②）无交集 |
| ④ | 附近任务提示轴 | `QuestService.getLevelRequirement` / `checkStartConditions` 仍走 typed 模板 ⇒ native 行（P1-P6 全部）在 `SM_NEARBY_QUESTS` 面被判不可接取 | **本批未改**（跨族、属另一轴）；登记为 §10.3-#18 待立批 |
| ⑤ | 客户端任务页登记缺失（574 行） | 入口页靠契约回落页 4（全局接取窗）；本批以双向冻结固定 | 若后续取得客户端任务页登记，须同步重算 |

## 8. 证据文件

- `p6/P6-STEP1-REPORT.zh-CN.md`、`p6/combine-feasibility.tsv`：表来源 / 逐行装载 / 行集划分（本报告 §3 输入）。
- `gates/2026-10-01-p6-family-tablane.log`、`gates/2026-10-01-focused-run-p6.log`、
  `gates/2026-10-01-focused-run-p6-red-classes.tsv`、`gates/2026-10-01-focused-run-p6-delta.tsv`。
- 源码：`tablelane/SimpleCombineTaskHandler`、`tablelane/NativeRecipePort`、`QuestEngine`（owner 面）、
  `QuestService.abandonQuest`、`retail/RetailQuestDriver`；测试：`SimpleCombineTaskNativeFamilyGateTest`（11）、
  `CombineTaskRowAlignmentGateTest`（4）、`CombineTaskFamilyRowInventoryGateTest`（1）。
