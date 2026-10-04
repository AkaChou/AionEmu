# 诊断：任务接取静默失败（80787 事件族）+ 页面错乱待定位

日期：2026-10-03 ｜ 分支：quest ｜ HEAD：913d3a9bc

## 症状

1. 玩家在事件任务 80789/80790 的 4762 页（select_none）点 SIMPLE 接受（动作 20000），
   服务端零回包、零日志（"全都无法接取"的实际观测面）。
2. "页面错乱"：任务日志(J)页乱 + 对话框内容乱（具体表现待用户复测描述）。

## 已实证的因果链（接取失败）

- `log/quests.log:1285-1293`（10-03 16:11，玩家 Uu）：
  31 → 下发 4762 → 连点 20000 ×7 → 无任何 S->C 响应。
- 代码位置：`DataDrivenNativeRuntime.dispatchAcquireDialog` 的 `ACTION_BOOK` 分支
  （tablelane/DataDrivenNativeRuntime.java，"20000 → 接取 + 完成通道"）：
  `NativeQuestStartPort.start()` 判定失败即 `return false`——静默、无日志、无回包。
- 失败轴（高置信）：`PREREQUISITE_MISSING`。retail quest.xml 实测：
  - 80789 → `finished_quest_cond1 = Q80787`
  - 80790 → `finished_quest_cond1 = Q80788`
  - 其余轴全开：minlevel=1 / maxlevel=75 / pc_light+pc_dark / 全职业 / gender=all / max_repeat=255
- 测试角色 Uu 为 16:05 首次进世界的新角色，quests.log 全文 0 条 80787/80788 记录。
- 9/28 23:58 基线（quests.log:871-881）：玩家 Zz 同任务同车道 31→4762→20000 成功
  （SM_QUEST_ACTION 状态=3→4→5 完整走通）。Zz 为完成过前置的老角色。
- git 考古：前置列 Q80787/Q80788 早已存在（仅 0997233c2 目录搬运移动）；
  ACTION_BOOK 判定链 P7（715a00136/fd574e347，9/28 前）引入。
  今日 5 笔提交均未改动 80789/80790 的 quest.xml 行。
  ⇒ 非今日回归，是"前置链拒绝 + 静默无提示"的组合暴露。

## 交互缺陷（真问题，独立于数据）

- 打开 4762 页（动作 31）只查"没接过"（state==null），不做资格判定；
  玩家能看到任务和接受按钮，点 20000 才全判定且拒绝时无任何提示/日志。
- 真端应在清单层隐藏不可接任务：opcode 127 三值判定 `zoneVerdict` 已实现
  （NativeQuestStartPort，6171aa2c2 接线），但 4762 接取面未与之对齐。

## 已排除 / 已撤销的假设

- ~~静态数据加载竞态~~：console.log 中 `XmlDataLoader - NpcData  7113` 等行
  是 `logStaticDataPhaseTimings`（XmlDataLoader.java:1116）输出的**各阶段耗时毫秒**，
  非条目计数。两次启动差异（7113 vs 4036）= 冷/热缓存耗时差，非数据丢失。
- ~~quest_name_string_ids 迁移破坏~~：XML 10161 行 = 旧 TSV 10161 行，等价。
- ~~80790 前置列今日新加~~：git -S 考古仅目录搬运。
- 退役 7 件任务 DB 残留 → J 页乱：概率低（自造任务，客户端无定义；QuestStateList
  对缺失 metadata null-safe），未最终排除。

## 缺陷 A（已实证）：10/1 回归——SimpleTalk 族领奖全灭（2ab3cc59b）

- 10-03 16:37 玩家 Uu 跑通 1101 接取（31→1011→1007→页4→1002→状态3），
  走到 REWARD（状态4、页5），点完成确认动作 23 → 零响应（quests.log:1303-1314）。
- 路径：`SimpleTalkHandler` 领奖段把 `8..23` 段动作一律映射为 `dialogId - 8`
  ⇒ 动作 23 → 选项下标 15；`NativeReportRewardFlow.windowButtonProblem`
  判 "index 15 beyond 3 options"（1101 声明 reward_item1_1/1_2/1_3 共 3 个选项）
  ⇒ `NATIVE_REWARD_BUTTON_UNDECLARED` fail-closed：不发奖、不完成、无回包、无日志。
- 旧实现（2ab3cc59b~1）领奖不查按钮面 ⇒ 9/28 前能领。2ab3cc59b（10-01 19:58）
  把 SimpleTalk/SimpleHunt/SimpleSerialHunt 三族领奖改走 NativeReportRewardFlow。
- quests.log 佐证：9/28 23:58（80790，DataDriven 车道）为最后一条领奖成功记录，
  其后至 10/03 无任何领奖成功——三族领奖自 10/1 起真机全灭、从未被测过。
- 门禁假绿：`NativeQuestRewardClaimGateTest` 只构造 `8 + buttonIndex` 选项动作
  （test:405），未覆盖客户端完成确认动作 23。
- 修复方向（需真端证据坐实 23 语义后再动）：23 为完成确认而非选项 15；
  108/110..124 已映射 rewardIndex=0，8..23 区间上界把完成动作吞进了选项段。
  真端 cab520 对 23 的语义待从 p3 材料/反演坐实。

## 待验证（用户复测矩阵）

| # | 角色 | 操作 | 预期 |
|---|---|---|---|
| 1 | Zz | 接 80790 | ✅ 成功（前置已满足）|
| 2 | Zz | 接 18504（203166 处，无前置）| ✅ 成功 |
| 3 | Uu | 接 18504 | ✅ 成功 |
| 4 | Uu | 接 80789/80790 | ❌ 静默（前置未满足）|
| 5 | 任意 | 重启后对比 J 页/对话框错乱是否变化 | 固定=确定性数据问题 |

#1/#2/#3 任一失败 ⇒ 存在全局回归，重新开线。

## 修复落地（2026-10-03，缺陷 A）

- 真端语义坐实：`QuestDialogAction.SELECTED_QUEST_REWARD1..15 = 8..22`（选项段），
  `SELECTED_QUEST_NOREWARD = 23`（无选择确认）；结算体 `QuestService.getRewardItems`
  本就按 `dialogId==23 + extendedRewardIndex` 正确处理（23+!extended=固定奖励、
  23+extended=按扩展下标发可选项）。缺陷仅在 handler 层把 8..23 一段一刀切当选项段。
- 四处修复（选项段 8..23 → 8..22，23 归入无选择确认映射 rewardIndex=0）：
  `SimpleTalkHandler`、`SimpleHuntHandler`、`SimpleSerialHuntHandler`、`SimpleCollectItemHandler`
  （SimpleCollectItem 不在 P3 报告三族清单内但映射同构，一并修复）。
- 回归测试：`NativeQuestRewardClaimGateTest` 新增 2 用例（Talk 1207 + CollectItem 1137，
  从 handler 层驱动动作 23）。红绿验证：git stash 撤销修复 ⇒ 2 用例红
  （expected true but was false，真机症状等价复现）；恢复修复 ⇒ 14/14 绿。
- 族门回归：SimpleTalk/Hunt/SerialHunt/CollectItem 四族门 32/32 绿。
- 旧门禁漏网原因：claim 门禁全部以 `8 + buttonIndex` 手工构造动作直调 flow，
  绕过了 handler 的 dialogId→rewardIndex 映射层——映射缺陷所在层无覆盖。

## 修复落地（2026-10-03，缺陷 B：接取静默拒绝）

真端证据（P7-REPORT §「同一判定函数」）：清单（opcode 127）与 NPC 接取面同用
`Quest::CanAcquireQuest`（等级/种族/职业/性别/前置 finishedcount/限制位），「不会出现能接
但不在清单」。当前实现偏离契约：接取面 31 入口只查「没接过」，资格判定推迟到 20000/1002
的 start() 且拒绝时静默（无回包、无日志）。

修复（两半）：
1. **可观测性**：`NativeQuestStartPort.startTraced(player, questId, dialogId)`（新增，
   判定与 start() 完全一致）拒绝时向 `quest` logger 打 QUEST-TRACE（含结论轴 detail，
   如 `PREREQUISITE_MISSING: unfinished prerequisites [80788]`）。六个接取面全部改走：
   DataDrivenNativeRuntime（ACTION_ACCEPT/ACTION_BOOK）、SimpleTalk、SimpleHunt、
   SimpleSerialHunt、SimpleCollectItem、SimpleUseItem。I18n key
   `log.quest_trace.acquire_refused`（messages/messages_zh_CN）。
2. **31 入口预检**（对齐清单同源判定）：DataDrivenNativeRuntime.dispatchAcquireDialog
   与 SimpleTalkHandler 的 31/26 打开接取面前先过 `evaluateNpcAcquire`——资格不满足
   不进接取面（不发 4762），玩家不会再「进了接取页点接受却被无声吞掉」。

测试与门禁：
- 新用例 `acquireEntryPageIsEligibilityGatedLikeTheNearbyList`（80789 前置 Q80787：
  未完成 → 31 拒绝且零页下发；completePrerequisites 后 → 31 发 4762）。
- 既有用例假设修正 ×2：DataDrivenNativeRuntimeGateTest:616「无状态玩家打开必发入口页」
  改为「资格放行的行才发」（真端契约本就如此）；SimpleTalk 族门
  acceptEntryFollowsTheRetailCab520Routing 原用 CHAINED_QUEST 41536——该行
  minlevel_permitted=999（真端不可达行）+ 前置 Q41535，本就不该对 20 级玩家开接取面；
  改用可接取行 1131 并补不可达行拒绝断言。
- 门禁：12 类 109/109 绿 + 外围（AcceptProtocol/GrowthDialogPage/NpcDialogDispatch）9/9 绿。

## 修复落地（2026-10-03，缺陷 C：DD_TALK_SIMPLE 零步行交付报告面缺失）

- 根因：715a00136（10-02 P7 步 f「1444 行原子切换 + 同批删旧」）把 171 个零步 Talk 行
  （DD_TALK_SIMPLE，retention owner=RETAIL_TABLE/family=DataDriven，含 80787-80794 事件族）
  切给 DataDrivenNativeRuntime，但新 runtime 只建了接取面；旧引擎的 31 报告推进语义随删旧消失。
- 真机实证（17:56 quests.log）：80788/80787 已接（START）后在交付 NPC 上点 31 ×5 全零响应
  （「报告结果，结果 NPC 无法对话」）；SimpleTalk 侧 1101 全链正常（缺陷 A/B 修复生效）。
- 修复：`DataDrivenNativeRuntime` 新增交付报告面——
  - 构建：`reportTalksByNpcId`（零步行解析 `reward_npc_name` 注册；66 行接取=交付、105 行异 NPC；
    名字解析失败只缺席交付面，不冻结接取面）。
  - 分发：`dispatchReportDialog`（onDialog 在接取面之后）：START 31/26/-1 → 推进 REWARD +
    SM_QUEST_ACTION(状态4) + 页5；REWARD 31/26/-1/1009 → 页5；奖励窗动作（8..22 选项下标 /
    23 = NOREWARD 不占下标 / 108 / 110..124）→ `rewardFlow.claim` → 页1008。
  - 注意：不能用 runtime 的 START-only `state()` helper（REWARD 态返回 null）。
  - rewardFlow 注入（构造 +1 参数；生产 = NativeReportRewardFlow.instance()）。
- 语义基线：quests.log 2026-09-28 80790 真机流（31→REWARD+页5→23→COMPLETE）+ P7-STEP2E1
  交付对象 #2（reward_npc_name 恒注册，槽 +0x238）。
- 测试：`zeroStepTalkRowsReportAndClaimOnTheRewardNpc`（80787 三步全链）；GateTest 夹具补
  RetailQuestDriver 初始化（领奖口元数据来源，与领奖门禁同型）。
- 门禁：15 类 129/129 绿。

## 第四轮（2026-10-03 晚，复测反馈跟进）

- **80788/80790 全链真机打通**（18:39 quests.log：31→REWARD+页5→23→COMPLETE+页1008）✅
- **修复 D（跳步报告）**：dispatchReportDialog 的报告/发窗触发收窄为 31/1009——开门动作
  （-1/26，右键 NPC）不再自动推进报告（9/28 基线是点任务行才报告；18:39 真机右键即被推进
  = 跳过对话步骤）。DataDrivenGateTest 25/25 绿。
- **验证（非缺陷）**：SimpleHunt REWARD 态 31 → 页5 在 handler 层正常（claim gate 新断言绿）。
  真机 1102 卡壳（REWARD 后任务行从 NPC 对话页消失、无 C->S）在客户端渲染依赖的通道，
  服务端 handler 无缺口——需真端客户端行为参考定位（页 10 任务行的数据源/进行中任务标记）。
- **833672 任务链图**（quest.xml 实测）：两条独立链——80788(一次性,无前置)→80790(每日255,
  前置Q80788) + 80792(一次性,无前置)→80794(每日255,前置Q80792)。「应该只能接一个」的判定
  需真端依据（事件条件轴 or 清单行为）；服务端 31 预检 + opcode 127 清单过滤（OMITTED 不入）
  均已按 CanAcquireQuest 同源拦截，客户端页 10 行的显示残留待客户端行为参考。

## 修复落地（2026-10-03 晚，缺陷 E：右键开门不进任务对话）

- 用户复测三答定位：1102 REWARD 后任务日志显示「向米雷斯报告」、NPC 头顶有标记（客户端状态
  全对），但右键 NPC「只有结束对话可点」——`TalkEventHandler.onTalk` 先问引擎
  `onDialog(questId=0, -1)`，而引擎的 tablelane 分发全部要求 `requestedOwner != 0`，questId=0
  的兜底只走 typed 车道与采集物先手 ⇒ 引擎 false ⇒ 普通页 10（questId=0），任务行入口被堵死。
- 修复：`QuestEngine.onDialog` 的 questId==0 兜底段（采集物先手之后、typed 兜底之前）遍历玩家
  进行中/可交（START/REWARD）任务逐个重放 `onDialog(npc, player, questId, -1)`——真端对话平面
  「打开（-1/26）→ 阶段页」的对齐；SimpleHunt REWARD -1 → 页 5（handler 断言已加）。
- 与修复 D 的组合语义：DataDriven 零步行（80788 族）右键仍页 10（-1 不触发报告），玩家点行 31
  报告——对话步骤完整；SimpleHunt REWARD 右键直接开奖励窗。
- 门禁：10 类 91/91 绿。

## 修复落地（2026-10-03 晚，缺陷 F/G：第五轮复测反馈）

- **缺陷 F（采集物交互被吞）**：QuestEngine 的 questId=0 兜底里采集物先手段（targetsForNpc）
  原样 `setQuestId + return true` 不发任何包——真机 1103（SimpleCollectItem，物件
  LF1_Cherubim_pouch）右键谷物袋子零响应。改为重放 `onDialog(npc, player, questId, -1)`，
  由 START 段 onObjectUse 推进收集并回页。
- **缺陷 G（完成不移除 → 客户端列表累积）**：真机 80790（每日 255）循环接/交 6 轮，
  对话列表累积 6 个相同行。根因：接取发 SM_QUEST_ACTION action1（添加），完成只发
  action2（更新）不移除——客户端每轮再添加一条。`QuestService.setFinishingState` 完成
  后补发 action3（removeQuestFromClientList；进世界同步同型，PlayerEnterWorldService:358
  对全部 COMPLETE 逐个移除）。可重复任务下一轮接取由 addQuest 重新插入。
- 1103 日志中被连续接取两次：中间客户端发过 CM_DELETE_QUEST（放弃包，QUEST-TRACE 不记录）
  ——玩家放弃重接，非缺陷。
- 门禁：70/70 绿（引擎/四族/领奖/采集）；Quest1112ProductionFlow 3 Error + EarlyElyos
  1 Error 为**既存红**（git stash 对照验证：`missing production quest definition 1112`，
  P 系列批次遗留，与本批无关）。

## 修复落地（2026-10-04，缺陷 H：报告确认页两步语义，裁定 a）

- 普查（6224 件退役 XML）：NPC_REPORT 分型三页 SELECT2(1352)/SELECT5(2375)/DEFAULT_SUCCESS(10002)，
  共 1640 件；旧编译器两步语义 = 任务行（31）只发确认页不推进，报告确认（1009）才推进 REWARD+
  奖励窗；10002 型由客户端自动回发 1009（9/28 80790 基线 11ms 闭环由此解释）。
- 契约与退役 XML 交叉印证一致（80787=10002、1101/1137/80482/1107=2375、1102=1352），
  裁定口径 = 客户端任务页契约声明优先（`QuestDialogContract.reportConfirmPage`，新增）。
- 五处改造（31 → 确认页或一步直达[无声明降级]；1009 → 推进；-1/26 不推进不跳步）：
  SimpleTalkHandler、DataDrivenNativeRuntime（零步交付面）、SimpleCollectItemHandler、
  SimpleUseItemHandler；顺带拆分两处有副作用的就绪检查（handInComplete/handIn 内含扣物品，
  两步第一步会误扣——拆出无副作用 handInReady）。
  不动的：SimpleHunt/SimpleSerialHunt（杀满自动 REWARD，无报告步）、SimpleCombineTask（已 1009
  驱动）、SimpleItemPlay（物品使用事件驱动）。
- 门禁：全族 105/105 绿（两处旧断言更新为两步流：SimpleCollectItem 1137/18501、
  SimpleUseItem 80482/1107，全部 2375 型）。
- 普查落盘：retired-xml-behavior-census.md。

## 修复落地（2026-10-04，缺陷 I/J：第六轮复测反馈）

- **缺陷 I（接取误发完成页）**：真机 80789/80790 接取（20000）→ 状态3 + **页 1008（完成页）**，
  玩家看到「获得了礼物」完成文案（实际只是接取）。旧 XML（80789/80790）：
  `QUEST_ACCEPT_SIMPLE → started + close-dialog`、`QUEST_REFUSE_SIMPLE → close-dialog`。
  修复 `DataDrivenNativeRuntime` 收尾分支：20000/20001 → close-dialog（SM_DIALOG_WINDOW(0,0)）；
  1008 动作（ACTION_COMPLETE 完成通道）保持回完成页。
- **缺陷 J（通用页带 questId 触发 load fail）**：真机 1101（SimpleTalk 零步行，START 态）
  右键米雷斯 → 下发「页10 带 questId=1101」×3 → load fail。根因：1101 契约页集合
  {4,1003,1004,1011,2375} **无页 10**，带 questId 时客户端按任务 html 找 page 10 失败；
  9/30 正常日志页10 全部 questId=0（通用选择页）。
  修复：7 处 `PAGE_IN_PROGRESS(10) 带 questId` 全部改为不带（SimpleTalk/SimpleCollectItem×2/
  SimpleUseItem/CombineTask/ItemPlay×2）。
  1101 退役 XML 佐证：`NPC_START(203049) + NPC_REPORT SELECT5(203057)`——两步报告语义与
  缺陷 H 改造一致（31 → 2375 → 1009 → 奖励窗）。
- **缺陷 J 补充**：SimpleHunt/SerialHunt 另有 3 处字面量「页10 带 questId」（565/405/422 行）同型修复。
- 验证（2026-10-04，IDEA MCP runner 逐类启动）：8 类全绿——DataDrivenNativeRuntimeGateTest
  （1 处旧断言更新：20001 期望 1008 → 关窗 0）、SimpleTalk/SimpleCollectItem/SimpleUseItem/
  SimpleHunt/SimpleSerialHunt 族门、NativeQuestRewardClaimGateTest、QuestEngineNpcDialogDispatchTest。

## 修复落地（2026-10-04，缺陷 K：采集物件交互开对话窗 → load fail）

- 真机（15:37:16 quests.log，玩家 Kk）1103 接取成功后与谷物袋子交互：
  `SM_QUEST_ACTION 1103 状态=3 步数=1`（advance 状态同步 ✓）同毫秒
  `SM_DIALOG_WINDOW targetObj=11967 questId=0 下发页=10`（两参通用页发到采集物）→ 客户端 load fail。
- 根因：`SimpleCollectItemHandler.handleDialog` START 段对采集物目标在 `onObjectUse` 成功后再补发
  对话窗（页10）——真端采集交互无 after-commit 页（1103 退役 XML：`TALK_TO_NPC npc-id=700105
  started→started` 无页；QE-044 口径 = PACKET_ONLY 状态同步，advance 内已发 SM_QUEST_ACTION）。
  对非对话物件开对话窗，客户端无该对象对话资源 → load fail。
- 修复（两处）：
  1. `SimpleCollectItemHandler` 887 段：删除补发页，`return onObjectUse(...)`（推进成功即终）。
  2. `QuestEngine` questId=0 兜底先手段：加 `collectObjectClaimed` 认领信号——物件属 native 采集族
     （routes 通过）即认领；loop 全 false（超杀/条件不满足）时也不再落 `TalkEventHandler` default
     的通用页10（否则超杀再点仍对物件开窗 load fail；真端超杀=零副作用零包）。
- 门禁：SimpleCollectItem 族 14/14 绿；QuestEngineNpcDialogDispatch 6/6 绿。

## 修复落地（2026-10-04，缺陷 L：采集族物品驱动修正——推翻 P4 相机设计）

- 用户复测："采集不再 load fail，但需要采集 3 个才能下一步，现在采集一个就下一步了"（1103）。
- **根因（全族级）**：P4 批把"真端 quest.xml 的 collect_itemN 计数"误当作采集相机的 required
  （`NativeCollectSpecs`），交互/击杀按相机 +1 写 var0。**真端采集族根本没有相机**：
  - 相机调用集（`camera-params.tsv`，2463 调用点/1812 任务）：**采集族 262 行 0 命中**
    （对照 SimpleHunt 1812/1863 ✓）；真端源码复核 `FUN_180cb13b0/14e0(0x44f/0x470/0x465,)` 0 命中
    （对照 1102 `fun_760.cpp:517 FUN_180cb13b0(0x44e,param_2,1,3,3,1)` 有）。
  - 旧 XML（1103/1137/2346）：交互/击杀转换 `started→started` **零 var 写**；全节点 var0=0；
    `collect_progress=0` 250/262 行（客户端在 var0==0 维持采集步与报告对白条件）。
  - 真语义 = **物品驱动**：交互掉物品（`drop_monster_K→drop_item_K, prob`，每次 1 个）→ 收集；
    `isQuestDrop` 按 `metadata.itemRequirements()`（=collect_item 列）判"未持满才掉"——
    "采 3 个"即此上限；报告门 = 持有 collect_item 整组（`check_item`）。
  - 玩家可见症状链：交互 1 次 → var0=1 → 客户端按 var0 显示**进入下一步**（应采 3 个）。
- **次生缺口（同批修复）**：1103 退役 XML 后 catalog 无其 `<drops>`（overlay 直通），native 侧
  未接手 ⇒ `getQuestDrop(700105)` 空 ⇒ `QuestItemNpcAI2` 早退、**任务物品永不发放**。
- 修复（4 处主源码）：
  1. `SimpleCollectItemHandler`：删相机参数/advance/cameraFull；`onObjectUse`/`onKill` 改**认领零写**；
     `handInReady` 去相机门（只留中继链+持有物）；新增 `questDropsFor(npcId)`（构建期从
     `metadata.drops()` 建掉落索引，**无条件注册**）；routable 条件删相机项（拦截改由本行
     对象/元数据承接——原恒真的 `!objects.isEmpty()` 语义修正为本行 `objectIds`）；
  2. `QuestEngine.questDrops`：聚合目录快照 ∪ native 采集掉落（同 questId 目录优先——单一 owner）；
  3. `CameraRegistry`：撤销采集族相机注册（unresolved 机制一并退役）；
  4. `NativeQuestTableLoader.cameraSpec(int,Map)` 重载与 `NativeCollectSpecs` 退役删除。
- 测试对齐（既有门禁同步，非新增）：族门 14/14（改认领语义 4 用例）、RowAlignment 7/7
  （相机冻结改"无相机"+39611/49611 组表解析更新：路由 273→275 列）、Claim 14/14、
  18501/3734/Haramel/契约门/分发/DataDriven 25/Talk 9/UseItem 11/Hunt 4/SerialHunt 4 全绿。
- **悬案记录**：SimpleSerialHunt 16 行同为 0 相机（其 stage 相机写系 P2 批设计）——本轮
  **未动**（超范围，需独立取证）。

## 修复落地（2026-10-04，缺陷 M：报告页检查按钮 39 零响应 → 页面重复）

- 用户复测："和 npc 203057 对话，重复'拿出找到的谷物袋子'"（1103 报告页）。
- 实机日志实锤（log/quests.log 16:27，玩家 Kk）：31 → 下发 2375（select5）→ 玩家点
  「拿出找到的谷物袋子」→ C->S `动作=39` ×3 全部**零 S->C 回包** → 重开再点、页面原地重复。
- 根因：报告确认动作随任务页而分——直翻型按钮 = SELECT_QUEST_REWARD(**1009**)；检查型按钮 =
  HACTION_CHECK_USER_HAS_QUEST_ITEM(**39**，报告页整组检查，服务端判定）。两族报告段的推进
  动作集只含 {31, 1009} → 39 落空（return false → 零响应）。
- 客户端页全量普查（9127 件 html，含 Dialogs 子目录；修正先前只扫顶层的口径）：39 用户全库
  **2234**；两族 **Talk 675 + CollectItem 85 全部声明失败页 select6=2716**、无一声明 10000
  （39 成功 = 直接奖励窗，无中间 ok 页）；SimpleUseItem 0 件；DataDriven 表 10 件（1870/2870
  归 XML 保留；其余 8 件 = Shape B：select1 挂 39 + check_user_item_ok/fail(10000/10001) 结果页
  + select_success(10002) 1009，属 DD 步进面而非零步交付面，**未接线，悬案**）。
- 页型证据（客户端 html 逐页）：1103/1137 select5 按钮 39（「拿出找到的谷物袋子/化石。」）+
  select6 失败文案；1101 对照 = select5 按钮 1009；1105/1000 同型（select6=「您别跟我开玩笑」
  「你难道不会数数吗」）；契约 tsv 全量：select6 恒 = 2716（1874 件声明）。
- 修复（主源码两处 + 契约一处 + 既有门禁对齐两处）：
  1. `QuestDialogContract.checkFailPage(questId)`（新增）：select6=2716 声明即返回，未声明 -1
     （fail-closed，不发明页）；
  2. `SimpleTalkHandler`/`SimpleCollectItemHandler` 报告段：39 与 1009 同义推进（持满）；
     `39 且未持满` → 下发 checkFailPage（真端失败应答页）；
  3. 既有门禁对齐（非新增门禁类）：Collect 族门 +`reportPageCheckButton39AdvancesOrShowsTheDeclaredFailPage`
     （1137：39 缺物→2716 零写零扣 / 39 持满→REWARD+页5 扣物）；Talk 族门同型（1211 = routed 检查行）。
- 验证（2026-10-04 IDEA MCP runner）：SimpleCollectItem 族 15/15、SimpleTalk 族 11/11、
  NativeQuestRewardClaimGateTest 14/14、QuestEngineNpcDialogDispatchTest 6/6 全绿。
- 不动面（证据边界）：SimpleUseItem/DataDriven 零步交付面（普查 0 件 39 用户）；DD 8 件 Shape B
  （10000/10001 页型，悬案）；canonical 定义层的 39 语义（30217「1693 页确认按钮 39 在 REWARD 态
  开奖励窗」等）不在表车道两族范围。

## 修复落地（2026-10-04，缺陷 N：Talk 族击杀掉落断供——1105 击杀 210079 无任务道具）

- 用户复测："任务 1105，击杀 210079，没有任务道具"。
- 根因（缺陷 L 同类的族级翻版）：1105 = RETAIL_TABLE/SimpleTalk（表车道路由），进度为击杀采集
  （quest.xml：`drop_monster_1=MerdionQ_2_n` / `drop_item_1=quest_1105a` / `drop_prob_1=100`；
  退役 XML：`drop npc-id=210079 item-id=182200202 chance=100 each-member`）。P3 迁移只接手了
  SimpleTalk 的对话面；退役 XML 连同其 `<drops>` 退出 catalog 后，表车道行不再有任何掉落供源
  （`QuestEngine.questDrops` 当时只聚合 catalog ∪ 采集族 native）⇒ 击杀零掉落、交付门永不可达。
- 缺口普查（quest.xml drop 列 ∩ 各族表）：**Talk 1031/3152 行**（其中 6 行 XML 保留）、
  CollectItem 253/262（缺陷 L 已接）、UseItem **1 行（2435）**、Hunt/SerialHunt/CombineTask/
  ItemPlay 0 行；**DD 表 221 行**有掉落列且 DD 运行时无掉落面（全文件 grep drop 零命中）——DD 221
  与 UseItem 2435 记为悬案；XML_RETENTION 733 行中 163 行有掉落列（XML 车道持有，catalog 供源 ✓）；
  其余 130 行（1012/1017/1032 等）不在 6217 生产全集内（quest.xml 全表 10035 行 ⊃ 生产 6217，
  惰性数据）。
- 修复（主源码 2 处）：
  1. `SimpleTalkHandler`：构建期对每个路由行（跳过 xmlOwnedIds）先按 quest.xml
     `drop_item_1`/`drop_monster_1` 列预筛，再取 `retailMetadataOf`，把 `metadata.drops()` 经
     `QuestCatalogDrop.catalog` 注册进 dropsByNpcId；新增 `questDropsFor(npcId)`。概率/上限语义由
     现有 `QuestService.isQuestDrop` 原样承担（START 态 + neededAmount 上限 + work-item 1 上限 +
     itemRequirements 上限）——即复原退役前 catalog 掉落的行为。
  2. `QuestEngine.questDrops`：聚合扩为 catalog ∪ 采集族 ∪ Talk 族（同 questId 仍只信 catalog 条目，
     单一 owner 不变量保持）。
- 测试对齐：SimpleTalk 族门 +`talkFamilyServesTheRetailKillDrops`（1105→210079 掉落 quest_1105a；
  XML 保留行 9548 不得由 native 重复供源）。
- 验证（2026-10-04 IDEA MCP runner）：SimpleTalk 族 12/12、SimpleCollectItem 族 15/15（回归）、
  QuestEngineNpcDialogDispatchTest 6/6 全绿。
- 叠加面：1105 报告/交付面（select5 按钮 39 + item_check 门）已由缺陷 M 修复——本修复落地后
  1105 全链（接取→击杀掉落×3→报告 39→奖励窗）应可通。

## 修复落地（2026-10-04，缺陷 O：领奖收尾页错误——发 QUEST_COMPLETE(1008) 而非回选择对话页 10）

- 用户复测："任务领奖结束后的页面不正确，将任务列表完成的任务信息展示在奖励对话之后了"。
- 根因：P 系列表车道迁移把领奖收尾写成 `SM_DIALOG_WINDOW(objectId, QUEST_COMPLETE=1008, questId)`
  （带 questId 的完成页）；真端语义 = **npc-complete finish=SELECTION_DIALOG**（普查 4801/4805，
  CLOSE_DIALOG 仅 4）——领奖结算后**回选择对话页（页 10，questId=0）**。
- 证据三链：
  1. 旧引擎实机日志（log/quests.log 9/28 12:12，1103，pre-P7 正确行为）：动作=23 →
     `SM_QUEST_ACTION 状态=5` → `SM_DIALOG_WINDOW questId=0 下发页=10`；
  2. 退役 XML 普查：npc-complete finish 型 SELECTION_DIALOG 4801 / CLOSE_DIALOG 4；
  3. canonical 展开层（QuestXmlBlockExpander → AfterCommitAction.ShowQuestSelectionDialog(
     SELECT_QUEST=10)，PlayerQuestDialogPort.showSelectionDialog 以 2 参 SM_DIALOG_WINDOW(objectId, 10)
     发送）——定义层本就正确。
- 修复（八处投递点）：SimpleTalk / SimpleCollectItem / SimpleUseItem / SimpleHunt /
  SimpleSerialHunt / SimpleCombineTask / SimpleItemPlay 的领奖分支 + DataDrivenNativeRuntime
  dispatchReportDialog 的 23/选项确认分支 → 一律 `new SM_DIALOG_WINDOW(objectId,
  QuestDialogPage.SELECT_QUEST.id())`（2 参 = questId 0，QE-137 通用页规则）；单点使用的
  PAGE_COMPLETE(1008) 常量随改删除防漂移（DataDriven 保留——其步进/接取面 1008 完成通道
  e1 裁定保留，不混改）。
- 测试对齐（既有门禁同步，非新增门禁类）：NativeQuestRewardClaimGateTest（4 断言）、
  SimpleCollectItem/SimpleUseItem/SimpleCombineTask/SimpleItemPlay 族门、DataDriven 门
  （zeroStepTalkRowsReportAndClaimOnTheRewardNpc）、QuestDialog31RegressionTest 阶梯段。
- 验证（2026-10-04 IDEA MCP runner）：ClaimGate 14/14、Talk 族 12/12、Collect 族 15/15、
  UseItem 族 11/11、Combine 族 11/11、ItemPlay 族 14/14、DataDriven 25/25 全绿。
- 既存红记录：QuestDialog31RegressionTest 的 26823 行（`missing production quest definition 26823`）
  ——26823 归 RETAIL_TABLE/DataDriven（P5 批已切 DD 表车道，catalog 无定义），定义层测试期望过时，
  与既存红 1112 同型；本轮仅其后的阶梯断言段被验证通过。
- 悬案（未分离）：4 件 CLOSE_DIALOG 型（普查未列 id）仍按页 10 收尾；DD 步进/接取面 1008 通道
  （e1 裁定）不动。

## 后续（未实施）

1. "页面错乱"待复现样本再定位（领奖修复后优先复测 J 页/对话框是否仍乱）。
2. **验证口径（用户裁定 2026-10-03）**：quest 车道行为验证不建新门禁/回放测试；
   用 git 历史退役的 `quest_definition/quests/<id>.xml`（逐任务行为规范：状态机 +
   NPC_REPORT/npc-complete 转换 + sync-quest-state）作印证基准。本轮七缺陷中 A/C/G/B
   的行为语义在 80790 旧 XML 里均有明确记录（`git show 4ede058c0~1:.../quests/80790.xml`）。
3. 833672 列表显示（客户端页 10 行渲染）待真端参考行为裁定。
4. 既存红：Quest1112ProductionFlowTest（missing production quest definition 1112）+
   EarlyElyos 1 Error——P 系列批次遗留，与本批无关，另行处理。
