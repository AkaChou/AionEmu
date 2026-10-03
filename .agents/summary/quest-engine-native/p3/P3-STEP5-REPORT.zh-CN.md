# P3 步骤 5 报告：残余旧金标重锚 + 接取入口页/职业轴/前置档修正 + 跨族阻塞取证

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 主题：SimpleTalk（3152 行）原生车道的**最后一段门禁债收口**——把 P2 基线里仍在断言旧 IR 形状的
> talk 行类逐类改锚真端表行，并把重锚过程中暴露的三处实现缺口按真端修正；同时冻结本批实测门态与
> 两个跨族阻塞的证据。
> 日期：2026-10-01。分支：`quest`。
> 前置：`p3/P3-STEP1..4-REPORT.zh-CN.md`、`p3-prereqs/simple-talk-codegen.md`（真端 DLL 槽位与 thunk 立即数）、
> `gates/2026-10-01-red-attribution-p3.zh-CN.md`（P3 切换批新增 34 类红归因）。
> 口径：断言只来自真端表行（`Quest_SimpleTalk.xml` / `quest.xml`）+ 静态数据 id（`npc_template` / 物品
> `name_desc`）+ 客户端页契约；旧 IR 形状（节点名/条件/动作/页链）随本批退场，**不为兼容而保留**。

---

## 1. 交付 1：纯 talk 归属类按 §8.9 重锚（10 类）

| 类 | 例数 | 门态 | 重锚后的断言面 |
|---|---|---|---|
| `QuestMovieEventDefinitionTest`（80016/80018） | 4 | 4 绿 | 真端行单步 NPC 接取 + `item_check` ×15 交付门 + 客户端入口页；旧 IR 的 onLvlUp 自动接取 / MOVIE bonus 登记为**不可实证假设**（活动子系表缺失，计划 §10.3-#3） |
| `QuestCharmedEventDefinitionTest`（80030/80033） | 4 | 4 绿 | 同上（真端行 + `quest.xml` 轴）；旧 IR 的 EventQuestRefresh 排程登记为不可实证 |
| `QuestLunarEventDefinitionTest`（80034-80037） | 6 | 6 绿 | 四行真端行（`event_Harmonan` 799765 / `event_Druike` 799780）+ `check_item1_1 == collect_item1` 门通道 + 种族/前置（Q80029/Q80032）+ `max_repeat_count=255` 可重复窗 + 反向种族 fail-closed |
| `QuestMultistepChainContractTest` | 9 | 9 绿 | 13 个任务清册里的 10 个真端 talk 行（1183/1483/1721/1724/2646/2692/2767/3966/3968/4501）改锚 native 行 + 运行时逐行走链；其余 3 个（1319/1514/2449）保持 IR 断言，1514 属 P5 SimpleUseItem 批（见 §6） |
| `QuestRewardTitlePrerequisiteAuditTest` | 2 | 2 绿 | 真端 `reward_title*` / 前置轴直读 |
| `QuestRewardItemGateTest` | 3 | 3 绿 | 真端 `reward_item1_N` / `selectable_reward_item1_N` / `reward_item_ext_*` 对拍合同 TSV（native 行直读） |
| `QuestProductionAcceptProtocolRegressionTest`（1913 族） | 2 | 2 绿 | 真端接取协议（接取/拒绝页 + 接取 NPC owner） |
| `RetailClientAcceptEntryPageTest` | 4 | 3 绿 1 红 | 接取入口页 = 客户端任务页声明（唯一红：80817，DD 族，§10.3-#5） |
| `GrowthQuestDialogPageAlignmentTest` | 5 | 4 绿 1 红 | 成长/事件行页阶梯（唯一红：19673 kill route，DD 族金标债） |
| `EarlyElyosQuestRegressionTest` | 19 | 18 绿 1 红 | 8 个 talk 行（1117/1118/1131/1141/1156/1158/1414/1691）改锚 native 行锚 + 运行时走链；其余 11 个 IR 用例断言面不变（唯一红：1137，P4 SimpleCollectItem，§6） |

**本批移除的旧 IR 断言面（逐条有真端出处）**：

- 「onLvlUp 背包达标自动接取」「LUNAR bonus 随机档」「活动失效弃任」：只见于本地 XML 与旧 handler；
  真端 codegen 对这些行注册的是普通 SimpleTalk 槽位 ⇒ 登记为不可实证假设（不在 native 车道发明）。
- 「1118 交付门要求并扣除工作物品」：真端行**未声明 `item_check`** ⇒ 交付门不生效（`quest.xml` 的
  `collect_item`/`quest_work_item` 通道只对 `item_check` 行生效，`P3-STEP3-REPORT` §3 同口径）。
- 「1141/1156 的 `USE_OBJECT`/`CanAct` 物件门」：真端行只有接取/交付 NPC 两列 + 中继链，物件走对话开启。
- 「1131/1158 的手制 1009 中转」：真端报告段就是 `1009`（`cabb10` 的 1009 链接 → `mgr+0x1c8` 奖励窗）。

## 2. 交付 2：重锚过程中暴露的三处实现缺口（按真端修正）

| # | 缺口 | 真端事实 | 修正 | 证据 |
|---|---|---|---|---|
| 1 | 接取入口页写死页 4 | 真端表**没有页列**；能不能渲染首屏由客户端任务页（`quest_client_dialog_contract`）决定 | `QuestDialogContract.acceptEntryPage(int)`（偏好序 4 → 4762 → 1011，无登记回落 4）成为唯一取数面；`RetailClientAcceptEntryPage` 与三个 talk handler 全部委托它；`SELECT1_1`(1012)/`SELECT1_1_1`(1013) 翻页动作原样回发（客户端未声明即 fail-closed） | 80487 族入口页 4 → 1011；`Quest1141ClientDialogAlignmentTest` 等重锚类 |
| 2 | 职业轴逐字比较 | 真端 `class_permitted` token 是 `fighter/knight/wizard` 这类**基础职业名**，不是 `PlayerClass` 枚举名 | `RetailQuestMetadataCompiler.permittedClassNames(raw, minLevel)` 抽为共享映射（≥16 token 通配、基础职业 ≥10 展开进阶线），`NativeQuestStartPort` 与 `SimpleTalkHandler.factionRotationEligible` 同源消费 | 1913 等限职业行原判据**永远拒接**；`NativeQuestStartPortTest.classRestrictedRowsFollowTheRetailTokenMapping` 9/9 绿 |
| 3 | `que` 前置奖励档后缀 | 真端 `finished_quest_condN` 值可带 `:n` 奖励档后缀（如 `Q1007:1`） | `RetailQuestMetadataCompiler.prerequisiteRewardMode(questId, token)` 暴露；`NativeQuestStartPort` 解析剥后缀并比对前置的 `reward` 档 | 同上测试类；`NativeQuestStartPort.evaluateNpcAcquire` |

## 3. 交付 3：测试夹具与冻结证据

| 产物 | 内容 |
|---|---|
| `tablelane/NativeTalkFixture`（新增） | 真端行取数（`row/handler`）、假背包端口（`RecordingInventory`，记录 `give:`/`remove:` 调用序）、`Objenesis` 玩家/NPC 桩、对话包捕获（`dialogPages` / `assertOnlyDialogPage` / `clientEntryPage` / `clientDeclares`）；**夹具不合成语义** |
| `p3/tools/step5_anchor_evidence.py` | 证据导出器：真端行 + 静态 id + 客户端页集 → TSV（不复用 Java 侧实现取数） |
| `p3/step5-anchor-evidence.tsv` | 50 个待重锚任务的真端行冻结证据（接取/交付名与 id、中继、give/remove、`item_check` 门、`con_quest`、cutscene、客户端页集、类别/等级/种族/职业/重复上限/奖励/掉落/前置） |

## 4. 门态实测（工作区，含 QE-112 在飞切片）

| 门 | 命令 | 结果 |
|---|---|---|
| A 组（本批 10 类） | `mvn -o test -Dtest=QuestProductionAcceptProtocolRegressionTest,RetailClientAcceptEntryPageTest,GrowthQuestDialogPageAlignmentTest,QuestRewardTitlePrerequisiteAuditTest,QuestMovieEventDefinitionTest,QuestCharmedEventDefinitionTest,QuestLunarEventDefinitionTest,QuestMultistepChainContractTest,QuestRewardItemGateTest,EarlyElyosQuestRegressionTest` | **58 例 / 2F + 1E**；三红全部范围外：`RetailClientAcceptEntryPageTest`（80817，DD）、`GrowthQuestDialogPageAlignmentTest`（19673，DD）、`EarlyElyosQuestRegressionTest`（1137，P4）。日志 `gates/2026-10-01-p3step5-reanchor.log` |
| 聚焦套件 | `mvn -o test '-Dtest=*Quest*Test,*Retail*Test'` | **1688 例 / 163F + 151E / 2 skipped / 110 红类**；对 P3 步骤 4 基线（1678 / 162F + 185E / 117 类）**REMOVED 7 / ADDED 0**；`missing production quest definition` **196 → 128**。日志 `gates/2026-10-01-focused-run-p3step5.log`，逐类清单 `gates/2026-10-01-focused-run-p3step5-red-classes.tsv` |

被移除的 7 个红类（全部为本批重锚目标）：`QuestProductionAcceptProtocolRegressionTest`、
`QuestRewardTitlePrerequisiteAuditTest`、`QuestMovieEventDefinitionTest`、`QuestCharmedEventDefinitionTest`、
`QuestLunarEventDefinitionTest`、`QuestMultistepChainContractTest`、`QuestRewardItemGateTest`。

> 说明：`RetailClientAcceptEntryPageTest` / `GrowthQuestDialogPageAlignmentTest` / `EarlyElyosQuestRegressionTest`
> 三个类仍出现在 110 红类清单里，但剩余红例已不是 talk 行，而是 80817 / 19673（DD 行，§10.3-#5）与 1137
> （P4 SimpleCollectItem）。

## 5. 本批新发现的两个跨族阻塞（取证完成，需裁决后单批落地）

### 5.1 阻塞 A：native 车道的**领奖/完成段**对所有已切换族不可用（NPE）

**事实链**（逐条可复现）：

1. 旧族编译器随 P1/P2/P3 切换批删除后，已切换行从生产目录退场：
   `ProductionQuestDefinitions.definition(1141)` / `definition(2354)` ⇒ `missing production quest definition`；
2. 生产 `QuestsData` 由目录构建（`DataManager.java:305` → `QuestsData.fromCatalog`，`QuestsData.java:126-137`），
   故 `getQuestById(1141) == null`、`getQuestById(2354) == null`（XML-only 行如 1000 仍非 null）；
3. 三个 native handler 的领奖段仍调用旧 lane 的完成入口：
   `SimpleTalkHandler.java:894-896`、`SimpleHuntHandler.java:367-369`、`SimpleSerialHuntHandler.java:421-423`
   → `QuestService.finishQuest(env, rewardIndex)`；
4. `QuestService.finishQuest` 第一段即取 typed 模板（`QuestService.java:128-131`）⇒ 实测抛：
   `NullPointerException: Cannot invoke "com.aionemu.gameserver.model.templates.QuestTemplate.getCategory()" because "template" is null`
   （探针：临时类 `ZzP5Probe3Test`，注入 `QuestsData.fromCatalog(ProductionQuestDefinitions.catalog())` 后对
   1141/2354 的 REWARD 行调用 `finishQuest(player, 0)`；探针已删除，输出见本报告正文）。

**爆炸半径**：三个已切换族的**全部行**（SimpleHunt 939 + SimpleSerialHunt 16 + SimpleTalk 3134 路由行）——
玩家在奖励窗点「领取」即触发异常，任务无法完成。**当前门禁无一例覆盖领奖段**（族门与夹具均止步于
奖励窗页 5），故该缺口在 P1/P2/P3 三次切换批中都未被发现。

**真端对照**：真端完成 = `caad20` 完成门（`vars==param_4 → +0x100 SetQuestSuccess + 发页`，
`p3-prereqs/simple-talk-codegen.md` §3/§4）+ 奖励窗 `mgr+0x1c8`；奖励内容来自 `quest.xml` 的
`reward_exp*/reward_gold*/reward_item*/selectable_reward_item*/reward_title*` 列。本服旧 lane 的
`QuestService.finishQuest` 正是这套语义（经验/金币/道具/称号/重复上限），但它要求 typed 模板存在。

**候选处置（需裁决，二者都不得引入 IR 双事实来源）**：

1. **native 完成/领奖口**（计划 §6.2 `NativeReportRewardFlow` 的完成半边）：直读 `quest.xml` 奖励列 +
   状态端口写 `COMPLETE/completeCount/reward/completeTime` + 客户端同步；奖励档位、可选奖励、职业奖励、
   `reward_item_ext_*`、随机奖励的**逐列语义要单独坐实**（属 P6「奖励档位/窗口」范围）。
2. **metadata-only 模板保留**：已切换行不再产出可执行定义，但由真端 `quest.xml` 产出一条**无节点/无转换**的
   元数据条目（目录加载器已支持「无节点 = 仅元数据」语义），令旧完成口的奖励面继续可用；风险是奖励面仍走
   旧 lane，需确认它读的列与真端列一一对应。

**结论**：P3 的退出条件「报告/领奖语义闭环」**未达成**；本批不擅自实现奖励面（避免在未坐实档位语义前
造漂移），登记为 §10.3 阻塞并在下一批（建议 P3 步骤 6，跨族共用组件）落地。

### 5.2 阻塞 B：`bm_restrict_category` 未坐实 ⇒ 44% 的 talk 行不可接取

**事实**：真端 `quest.xml` 的 `bm_restrict_category` 是「128 位地图/类别位集」（写入 `NPCServer fun_052.cpp:3726`，
消费于 `Quest::CanAcquireQuest`），本服尚无该位集来源 ⇒ `NativeQuestStartPort.evaluateNpcAcquire`
在等级/种族/职业/性别轴之后、前置轴之前 **fail-closed**（`BM_RESTRICT_UNRESOLVED`，`NativeQuestStartPort.java:149-154`）。

**规模（按行复算）**：`quest.xml` 全表 3477/10035 行声明该轴；**SimpleTalk 表 1383/3152 行（43.9%）**声明该轴
⇒ 这些行当前**不可接取**（含本批测试用到的 1414、1691）。重锚类因此改为「表行/阶梯断言 + bm 轴 fail-closed 断言」，
机械面（中继、步内发扣、报告翻 REWARD、页阶梯）用直建 START 行验证（`EarlyElyosQuestRegressionTest`）。

**处置**：单独立项解 `fun_052.cpp` 的位集语义（写出侧与消费侧双向对拍），落地后把 fail-closed 断言换成
真端位集判定；在此之前不得放行兜底。

## 6. 残余与边界

- **QE-112 在飞（不动）**：7 个混合类（`QuestRefactorRepairRegressionTest`、`PlayerQuestStartEligibilityPortTest`、
  `QuestMovieAndDialogLoopRegressionTest`、`QuestItemPlayGrantGateTest`、`QuestDraupnirNpcVariantContractTest`、
  `QuestPrematureRewardRouteExclusionTest`、`QuestBatchReportNpcAlignmentTest`）与 `RetailSimpleHuntDefinitionCompiler`
  （+279）/`RetailDataDrivenDefinitionCompiler`/`quest_definition_catalog.xml`/3122·3123·4122·4123 等保持原状，
  未 stage、未改。
- **范围外红（登记不修）**：19673（DD）、80817（DD，§10.3-#5）、1137（P4 SimpleCollectItem）、1514（P5 SimpleUseItem
  的行阶梯断言，本批在 `QuestMultistepChainContractTest` 内显式登记为「随 P5 重锚」）。
- **未启服 / 客户端验收**：无服务端启动、无真机客户端验收 ⇒ 本族维持 `PENDING_CLIENT`（建议代表任务 1131：
  接取 Hyacinte(203097) → Shugo_LF1a_01(799093) 换手 → 交付 Nadaelo(203101)）。
- **本批未新增日志、未改 XML/数据文件**（除测试与文档）；三处实现修正均为真端口径收紧，无兼容分支。
