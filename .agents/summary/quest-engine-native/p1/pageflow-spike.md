# PAGE-FLOW SPIKE 报告：SimpleHunt 网格形页流约定与独立出处矩阵（声明书 G3）

日期：2026-10-01　状态：**已完成**。消费方：`P1-SWITCH-BATCH-DECLARATION.zh-CN.md` §2.3/§8-G3。
范围 = 网格形编译路径（`compile`/`compileCanonical`/`compileSequentialStages`/`compileClientLadder` →
`assembleRoutes`）；串行链 `compileSerialChain` 不在范围（P2）。`RetailSimpleHuntDefinitionCompiler`
（下称 HUNT）为 QE-112 在飞脏文件，本 SPIKE 只读。

## 0. 范围与路径图

- 网格路径：`compile` HUNT:86-90 → 私有 `compile` HUNT:127-162 → `precheck` HUNT:480-515 →
  `build` HUNT:580-669 → `assembleRoutes` HUNT:680-739；`compileCanonical` HUNT:100-104、
  `compileSequentialStages` HUNT:121-125、宽计数 `buildCanonicalCounterQuest` HUNT:950-1082、
  DD 行阶梯 `compileClientLadder` HUNT:756-819 同汇。
- 调度方：`RetailQuestDriver.java:775-779`（grid）；`RetailDataDrivenDefinitionCompiler.java:476-482`
  （allHunt→ladder，否则 canonical）。
- 证据源：PAGE=`QuestDialogPage.java`；ACT=`QuestDialogAction.java`；HP=入仓
  `retail/HtmlPages.xml`（UTF-16 已核对）；CLIENT=`/Users/mc/PycharmProjects/unpak/dialog_unpacked/QUEST_Q*.html`（存在，UTF-8）；SRV58=`58Server/server58-source/MainServer_ScriptDLL64/`（存在）。
- verdict：EVIDENCED / CONVENTION_ONLY / UNEVIDENCED。

## 1. 接取流（canonicalAcceptFlow，HUNT:1305-1331；acquired_npc，源态 unaccepted）

| # | 元素 | 值 | 来源 | 独立出处 | verdict |
|---|------|-----|------|----------|---------|
| A1 | 入口页 | `QUEST_SELECT`(31) → `ShowQuestDialog(4)` | A（PAGE:12） | HP: 4=`ask_quest_accept`；CLIENT Q1000 有该页；**SRV58 派发器 QUEST_SELECT 态发 4762/阶段页表，页 4 只经 1007 打开（fun_731.cpp:4146 起）** | 页 4=EVIDENCED；直跳=CONVENTION_ONLY（家族裁定） |
| A2 | 接受（窗内） | `QUEST_ACCEPT_1`(1002)+`StartEligible`；→`ShowQuestDialog(1003)` | A+B | HP: 1003=`quest_accept_1`；SRV58 `FUN_180caf150`（fun_731.cpp:4038-4107）0x3ea→0x3eb | EVIDENCED |
| A3 | 接受（简单） | `QUEST_ACCEPT_SIMPLE`(20000)+`StartEligible`；`CloseDialog` | A | SRV58 fun_731.cpp:1513-1523：20000→资格检查→关窗；CLIENT Q1346 按钮 `HACTION_QUEST_ACCEPT_SIMPLE` | EVIDENCED |
| A4 | 拒绝（窗内） | `QUEST_REFUSE_1`(1003)→`ShowQuestDialog(1004)` | A | HP: 1004=`quest_refuse_1`；SRV58 fun_731.cpp:1497-1499 | EVIDENCED |
| A5 | 拒绝（页 1004 按钮） | `QUEST_REFUSE_2`(1004)→`CloseDialog` | A | 同类关窗路径 | CONVENTION_ONLY |
| A6 | 拒绝（简单） | `QUEST_REFUSE_SIMPLE`(20001)→`CloseDialog` | A | SRV58 fun_731.cpp:1515-1518（音效+关窗）；CLIENT Q1346 | EVIDENCED |
| A7 | 关窗退场 | `FINISH_DIALOG`(1008)（双态）→`ShowQuestSelectionDialog(10)` | A | HP: 10=`select_quest`；SRV58 fun_731.cpp:4076-4078：1008=纯关窗 | 动作=EVIDENCED；回页 10=CONVENTION_ONLY |
| A8 | （对照）旧信页阶梯 | 1011/4763/1007→页 4 | A/E | HP+CLIENT 双侧一致（旧形） | 旧形=EVIDENCED；规范形删除该链=家族裁定 |

## 2. 简报流（仅 talk_npc1 非空）

| # | 元素 | 值 | 来源 | 独立出处 | verdict |
|---|------|-----|------|----------|---------|
| B1 | 简报位 | var5 位偏移 30（槽 6），1 bit | E（HUNT:597-604） | SRV58 `FUN_180cb13b0`（fun_731.cpp:5304）6 位组 `6*(N-1)` → 槽 6=30；`RETAIL_BRIEFING_SLOT_CONFLICT` 门 HUNT:193-198 | 偏移=EVIDENCED；slot6=简报语义=CONVENTION_ONLY |
| B2 | 简报边 | 简报 NPC `QUEST_SELECT`(31)，started→零段/briefed；`LEVEL_AND_VISIBILITY_REFRESH`+`CloseDialog`（一步清位） | E（HUNT:659-666/1022-1031） | CLIENT Q1346：简报=客户端本地翻页链，服务端只需清位点 | CONVENTION_ONLY（规范形最小合同） |
| B3 | 简报门 | `RETAIL_TALK_NPC_UNRESOLVED/_AMBIGUOUS` 等 | A | — | 工程门 |

## 3. 击杀进度中对话

| # | 元素 | 值 | 来源 | 独立出处 | verdict |
|---|------|-----|------|----------|---------|
| P1 | 进度中再对话 | 编译器不 emit（无非满态 QUEST_SELECT 路由）；可见行为属运行时兜底 | A（结构性缺席） | SRV58 未匹配动作=原样回显（fun_731.cpp:4092-4094） | UNEVIDENCED（勿猜，native 按同构缺席处理） |
| P2 | 击杀边 | KillNpc 逐怪 + `PACKET_ONLY_SYNC`；满段杀附 `LEVEL_AND_VISIBILITY_REFRESH` 进 reward | A+B | SRV58 相机逐字（FUN_180cb13b0/14e0） | 计数=EVIDENCED；满段刷新=CONVENTION_ONLY |

## 4. 报告流（满段交付，assembleRoutes HUNT:723-736）

| # | 元素 | 值 | 来源 | 独立出处 | verdict |
|---|------|-----|------|----------|---------|
| R1 | 交付入口 | 交付 NPC `QUEST_SELECT`(31)（fullLabel）→`ShowQuestDialog(奖励窗页)`；旧报告页 1352/2375 与 1009 中转已删除 | A+E | CLIENT Q1346：select2→`HACTION_SELECT_QUEST_REWARD`(1009) 旧链有出处 | 规范形直跳=CONVENTION_ONLY |
| R2 | 奖励窗页 | 档 0..5 → 页 **5/6/7/8/45/46**；>6 档显式抛错（HUNT:1364-1369） | A+B（档数=quest.xml 奖励组） | HP: 5-8/45/46=`select_quest_reward1..6` | 页 id=EVIDENCED；映射与 >6 拒绝=CONVENTION_ONLY（QE-028） |
| R3 | 交付 NPC 集 | `plan.rewardNpcIds()` 优先，否则 `clientRewardNpcs`（TSV 120 行，dic 链生成） | C+B | RNPC.java:14-23 dic 链规则；网格形仅复合势力行兜底（HUNT:495-500） | EVIDENCED |
| R4 | 未接态交付 NPC 无出口 | 无 NONE 态路由（286 冻结行全 acquired==reward） | E | — | CONVENTION_ONLY |

`can_report`：quest.xml 217 行有值，**main 零消费**——对网格形报告流无门控效果（登记，不改）。

## 5. 奖励/完成流（completeFlow HUNT:1377-1401 → npcCompleteFlow HUNT:1403-1498 + rewardWindowAutoFlow HUNT:1514-1582）

| # | 元素 | 值 | 来源 | 独立出处 | verdict |
|---|------|-----|------|----------|---------|
| C1 | 奖励态重开 | `USE_OBJECT`(-1)/`SELECT_QUEST_REWARD`(1009) → `ShowQuestDialog(5)` | A | HP: 5=`select_quest_reward1`；SRV58 fun_731.cpp:4068-4070：1009→开奖励窗 | EVIDENCED |
| C2 | 确认区间 | `SELECTED_QUEST_REWARD1..15`(8..22)+`NOREWARD`(23) → complete；afterCommit=RefreshPlayerStats+COMPLETION+回页 10 | A | SRV58 fun_880.cpp:5535-5558（页 5+动作 23 域=奖励窗确认）弱佐证；CLIENT HTML 无此字样（全局窗口原生 UI） | 弱佐证；形状=CONVENTION_ONLY |
| C3 | 自动确认通道 | `SELECTED_QUEST_AUTO_REWARD`(108)/(110..115)+槽上限 15；双协议注册 | A | — | CONVENTION_ONLY |
| C4 | 职业可选奖励 | `CLASS_ROUTE_ORDER` 11 序；priority=`itemIndex*11+classIndex`；兜底 176；classRewardKey | A+B | — | CONVENTION_ONLY |
| C5 | 多档门 | `rewardGroups().size()!=1` → 抛错落 `COMPILATION_FAILED`；**javadoc 宣称 `RETAIL_MULTI_TIER`（HUNT:47）但全仓无 emit** | A | — | **文档漂移，登记** |

## 6. 退场约定（`RetailClientDialogExits.defaultExits()`）

- select_none 续页 14 任务集 `{1888,2888,15478,15479,15606,16800,25050,25073,25094,25478,25479,25606,26800,80989}`（EXITS:15-17）——页 4763 有 HP 出处，集合=CONVENTION_ONLY 冻结。
- **结构性事实：网格形路径从不消费 clientDialogExits**（仅 requireNonNull）；消费方全是 DD talk/collect 链与被排除迁移的 HandinDialogFlow ⇒ **native 网格形无需该输入**。

## 7. 全部字面值清单

- 页 id（Show 方向）：4/1003/1004/10/5/奖励窗 5-8/45/46——HP 全部核对通过，无缺口。
- 动作 id：31/1002/20000/1003/1004/20001/1008/-1/1009/8..23/108/110..115（槽上限 15→110..124）。
  真端旁证：31/1002/1003/1004/1007/1008/1009/20000/20001 见于 fun_731.cpp 派发器；**108/110..115 未见**。
- 其他：`REWARD_FALLBACK_PRIORITY=176`、`itemIndex*11+classIndex`、`DEFAULT_PVP_LEVEL_GAP=10`、6/10 位掩码（SRV58 逐字）。

## 8. native 重建分区（G3 结论）

**可直接从真端数据推导**：全部页 id 存在性/页名（HP）；交付 NPC 集（dic 链 TSV，复合势力行）；计数
位布局与简报位偏移（SRV58 逐字）；动作词汇 31/1002/1003/1004/20000/20001/1008/1009（CLIENT+SRV58 双侧）。

**家族级冻结约定（8 项，全族共形、不产生行级冻结；native 按现形状复刻并把本文件当出处登记）**：
① QUEST_SELECT→页 4 直跳；② FINISH_DIALOG→任务选择页 10；③ 确认区间 8..23 逐条形状 + 108/110+k
自动确认 + 双协议注册 + 槽上限 15；④ 档位→奖励窗映射（5/6/7/8/45/46）与 >6 档拒绝；⑤ class 路由序
与 priority 公式与兜底 176 与 classRewardKey；⑥ select_none 14 任务集（网格形不消费，登记存档）；
⑦ 简报位 slot6 语义与一步清位；⑧ 满段杀附带可见性刷新。

**UNEVIDENCED（不要猜）**：进度中再对话的运行时兜底页（native 按同构缺席处理，由既有运行时兜底行为
对拍覆盖）；`can_report` 未接线（零观察效果）。

**顺带发现**：C5 文档漂移（`RETAIL_MULTI_TIER` 无 emit）——登记入切换批矩阵，随批消除。
