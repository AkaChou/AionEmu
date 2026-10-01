# P3 步骤 6 报告：native 完成/领奖口 + `bm_restrict_category` 语义坐实（跨族两阻塞闭环）

> 主题：解决 §10.3-#11（native 领奖/完成段 NPE，QE-113）与 §10.3-#12（`bm_restrict_category`
> 未坐实导致 44% talk 行不可接取，QE-114）——两个 P3 退出前置的跨族阻塞。
> 日期：2026-10-01。分支：`quest`。
> 前置：`p3/P3-STEP1..5-REPORT.zh-CN.md`、`p3/p3-prereqs/simple-talk-codegen.md`。
> 口径：奖励面与接取轴都只读真端 `quest.xml`（与生产目录同一条编译器），不引入 IR 双事实来源。

---

## 1. 交付 1：native 完成/领奖口（`NativeReportRewardFlow`）

已切换到 native 车道的行不再有 typed XML/IR 模板，旧完成口必然取到 null 模板。本批按计划 §6.2 的
`NativeReportRewardFlow` 完成半边落地：

| 组件 | 变更 |
|---|---|
| `tablelane/NativeReportRewardFlow`（新增） | 领奖口：① 仅 `REWARD` 态可结算；② 奖励面 = `RetailQuestDriver.retailMetadataOf(id)`（**与生产目录同一条** `RetailQuestMetadataCompiler` + 同一批 npc/物品/随机奖励/name id 索引，含 `reward_exp*/gold*/abyss_point/title/item/selectable_reward_item/{class}_selectable_reward/*_ext` 列）；③ 结算体 = `QuestService.finishQuest(env, tier, template)`；④ fail-closed：缺行、元数据不可用、奖励符号名未解析（`reward:*`）、按钮未声明、多档未坐实 |
| `services/QuestService` | 抽出 `finishQuest(env, reward, template)` 共用结算体（旧入口 `finishQuest(env, reward)` 保留，缺模板时 **fail-closed 返回 false**，不再 NPE）；`setFinishingState` 改用传入模板的 `questWorkItems`；无控制器环境不再 NPE（单测/无控制器路径） |
| `retail/RetailQuestDriver` | 新增 `retailMetadataOf(questId)`：真端 `quest.xml` → 规范元数据（按任务缓存，与目录构建同一编译器） |
| `tablelane/SimpleTalkHandler`、`SimpleHuntHandler`、`SimpleSerialHuntHandler` | 领奖段改走 `NativeReportRewardFlow.claim(env, rewardIndex)`；完成口经构造参数注入（生产 = `instance()`），族门可换结算体 |

**奖励窗按钮语义**：真端 `SELECTED_QUEST_REWARD1..15`(8..22) 是奖励窗内被选中的选项。行的
**单槽**结构（`reward_*1` 族）⇒ 档位固定首档，选项下标由对话动作 id 传给结算段；行声明多槽时档位
语义属计划 P6，本口 **fail-closed**（`NATIVE_REWARD_TIER_UNRESOLVED`）。已切换行里只有
**18706/28706** 声明多档，且两行等级轴为 `999`（真端不可达）；其余全部单档。

**按钮声明面校验**：可选项数 = `selectable_reward_item1_*` 条数，或职业奖励行的本职业
`{class}_selectable_reward` 条数（与 `QuestService` 的职业分支同映射）；下标越界即
`NATIVE_REWARD_BUTTON_UNDECLARED`，不发放不完成。

## 2. 交付 2：`bm_restrict_category` 语义坐实（阻塞 #12 / QE-114）

双向对拍（`p3/p3-prereqs/bm-restrict-category-semantics.md` 全文取证）：

1. **写出侧**：NPCServer `fun_052.cpp:3723`（`+0x7c8c`）与 MainServer `fun_249.cpp:4117`（`+0x1f23`）
   都只存 **1 字节类别下标**，并 clamp `[0,8]`——不是 128 位地图位集；
2. **消费侧**：`Quest::CanAcquireQuest`（`Quest.cpp:142-217`）+ `FUN_1402d10a0`（`fun_055.cpp:3247`）=
   「玩家限制位图第 `类别 + 19` 位为 1 ⇒ 拒绝接取」；
3. **位名表**（`Server64.exe` VA `0x141122090` 实读 68 项）：下标 20..23 = `quest_acquire1..4`；
4. **数据分布**：`quest.xml` 3477 行声明该列，**取值全为 1**（SimpleTalk 982 / SimpleHunt 507 /
   SimpleSerialHunt 2）；区域 `bm_restrict.xml` 的账号类型 restrict 名单里**没有** `quest_acquire*`；
5. **本服落地**：`NativeQuestStartPort.restrictCategory(row)`（与真端同形 clamp）+
   `RestrictionBitmap` 端口（本服无计费来源 ⇒ 空位集，等价真端全订阅账号）⇒ 类别 1 的行按真端**可接取**；
   位命中时 `Outcome.BM_RESTRICT_BLOCKED` 拒绝建档。原 `BM_RESTRICT_UNRESOLVED` fail-closed 删除。

## 3. 交付 3：门禁

| 门 | 命令 | 结果 |
|---|---|---|
| 新领奖段门（12 例） | `mvn -o test -Dtest=NativeQuestRewardClaimGateTest` | **12/12 绿**：奖励面逐列对拍（经验/金币/道具/可选/称号/`_ext`/职业奖励）、三族 `REWARD` → 领奖 → `COMPLETE` 页、按钮越界/未解析奖励/缺元数据/非 REWARD 态/结算拒绝全部 fail-closed、多档行（18706）拒绝接单 |
| 接取轴门（9 例） | `mvn -o test -Dtest=NativeQuestStartPortTest` | **9/9 绿**：注入置位位图 ⇒ `BM_RESTRICT_BLOCKED` + 位下标 = `QUEST_ACQUIRE_FIRST_BIT(20)`；生产空位集 ⇒ 建档 START |
| A 组（12 类） | `mvn -o test -Dtest=NativeQuestRewardClaimGateTest,QuestProductionAcceptProtocolRegressionTest,RetailClientAcceptEntryPageTest,GrowthQuestDialogPageAlignmentTest,QuestRewardTitlePrerequisiteAuditTest,QuestMovieEventDefinitionTest,QuestCharmedEventDefinitionTest,QuestLunarEventDefinitionTest,QuestMultistepChainContractTest,QuestRewardItemGateTest,EarlyElyosQuestRegressionTest,NativeQuestStartPortTest` | **79 例 / 2F + 1E**；三红全部范围外：80817（DD）、19673（DD）、1137（P4 SimpleCollectItem）。日志 `gates/2026-10-01-p3step6-groupa.log` |
| 族门（Talk/Hunt/Serial + 装载/来源） | `mvn -o test -Dtest=SimpleTalkNativeFamilyGateTest,SimpleHuntNativeFamilyGateTest,SimpleSerialHuntNativeFamilyGateTest,SimpleHuntHandlerTest,SimpleTalkRowAlignmentGateTest,TableSourceProvenanceGateTest,NativeQuestTableLoaderTest,NativeQuestXmlTableTest` | **40/40 绿** |
| 聚焦套件 | `mvn -o test '-Dtest=*Quest*Test,*Retail*Test'` | **1688 例 / 163F + 151E / 2 skipped / 110 红类**；对 P3 步骤 5 基线（1688 / 163F+151E / 110 类）逐类清单 **完全一致（ADDED 0 / REMOVED 0）**。日志 `gates/2026-10-01-focused-run-p3step6.log`，逐类清单 `gates/2026-10-01-focused-run-p3step6-red-classes.tsv` |

**本批重锚的两个门态用例**（原「bm 轴 fail-closed」断言换真端判定）：

- `EarlyElyosQuestRegressionTest` 1414/1691：真端类别 1 ⇒ 限制位 20；本服位集为空 ⇒ 经真实接取
  动作（1002）建档 START；**前置轴**（Q1413 / Q1932）未完成时仍拒接并不落库——fail-closed 责任
  从「未坐实的 bm 轴」移交给真端确实声明的轴；
- `NativeQuestStartPortTest`：位集注入（位命中）⇒ 拒绝；生产空位集 ⇒ 放行。

## 4. 边界与残余

- **多档奖励（P6）**：已切换行 18706/28706 声明 4 档且等级轴 999（不可达）⇒ 本口 fail-closed；
  可达行出现第二档时必须先坐实档位语义，`NativeQuestRewardClaimGateTest` 的冻结断言会先破。
- **随机奖励组（`%name`）**：`RetailQuestMetadataCompiler` 把 `%组名` 映射为 `RANDOM`，而
  `QuestTemplate.toRewards` 不承载该 kind ⇒ 这类列当前**不发放**（3007/2658 等重复任务行）。
  属 P6「奖励档位/窗口」范围，未坐实前不发明发放；本批不改变该行为（旧 lane 同样不发）。
- **计费/账号类型**：本服无来源 ⇒ 限制位集恒空；将来引入时只替换 `RestrictionBitmap` 生产实现。
- **未启服 / 客户端验收**：无服务端启动、无真机客户端验收 ⇒ 三族维持 `PENDING_CLIENT`
  （`NativeReportRewardFlow` 的发放细节由共用结算体承担，仅在单测层验证门态与数据面）。
- **不在本批**：QE-112 在飞切片（7 个混合类 + `RetailSimpleHuntDefinitionCompiler` 等）与
  80817/19673/1137 三个范围外红；均未触碰。
