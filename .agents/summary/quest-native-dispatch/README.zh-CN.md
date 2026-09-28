# quest-native-dispatch：任务系统回归真端原生生命周期 —— 台账

> 目标（用户 Goal，2026-09-26 采纳）：停止为微观 HTML 页码扩建 TSV 补丁；RETAIL_TABLE 任务改为
> 真端原生生命周期分发（接取窗 4 → 1002 建档 → 计数/步骤推进 → 交付分档奖励窗 → 8..23 结算）；
> 微观页码审计收窄到存量 XML 任务；建立黑盒生命周期契约门。
> 车道唯一键 = `quest-native-dispatch`（本目录）；并发车道同号风险按"先查作者"纪律处理。

## 本轮已收口

### Phase 0：基线冻结（改码前）

- T1 全套（21 门禁类，75 例）：`gates/T1-001512.log`，**74 绿 / 1 红**。
- 唯一红 = `RetailDataDrivenGateTest.driftVersusShellsIsRegistered:430`：
  任务 **20035** 登记漂移码失同步（登记 `REJECTED:RETAIL_TALK_HUNT_CHAIN_DEFERRED`，
  实际 `REJECTED:RETAIL_ITEMPLAY_OUTPUT_UNRESOLVED`）。
  **归属**：共享工作区并行 DD 车道的在飞产物（记忆库 QA 台账中 20035 即并行车道任务），
  本车道不动它；本车道验收线 = 相对这条已知红**零新增失败**。
- 观察项：`RetailSimpleHuntFamilyGateTest` 单测 360s（全族 942 行编译循环）是 T1 长尾。

### Phase 1：`RetailQuestContractTest` 黑盒生命周期契约门（新建，已注册 T1）

- 文件：`src/test/java/com/aionemu/gameserver/questEngine/e2e/RetailQuestContractTest.java`
- 设计：**图引导的黑盒行走器**。从生产视图 IR（`CompiledQuestDefinition`）自发现路径，
  只经真实协议原语（`QuestJourneyRunner` 的 interact/clickVisibleAction/clickNativeAction/
  useObject/useItem/playItem/emitWorldEvent + `ClientResourceOracle` 可见性校验）驱动；
  **不锁任何具体页链**，因此页链形状改造（规范形）前后必须同绿——这是后续形状手术的安全网。
- 锁定的四相位合同（逐代表断言）：
  1. 接取：沿可见动作到达接取提交（询问窗形 `1007→页4→1002` 或直接接取形 `入口页 20000`），
     提交后必须 `START` 且进度为空；
  2. 进度：沿定义自身进度边驱动，节点或进度字段必须真实前进，进度期间不得 `COMPLETE`；
  3. 交付：沿真实交互到达**分档奖励窗**（页面 == `QuestDialogPage.rewardWindowForTier(档位)`
     查表值，QE-028），状态 `REWARD`；
  4. 结算：原生 `SELECTED_QUEST_REWARD1(8)` 一次性 `CompleteQuest`（恰一次）+ GrantReward →
     `COMPLETE`。
- 家族代表（8 例，全部走通全生命周期）：

| 家族 | 任务 | 形状要点 |
|---|---|---|
| SimpleHunt | 1112 | 单段网格，询问窗形接取，1352→1009 交付 |
| SimpleHunt | 30715 | 两段网格 a0→a1，**直接接取形**（入口页 1011 上 20000），2375 报告页 |
| SimpleSerialHunt | 13918 | 串行阶梯，直接接取形 |
| SimpleTalk | 1118 | 对话链 v0→v1（SETPRO1@203070），交付消耗任务物品（药膏 182200224） |
| SimpleItemPlay | 13704 | use-item 进度，REWARD 后 useObject(-1) 开交付对话 |
| DataDriven | 15042 | 三段道具演出（playItem 3000ms），kill-flip 不适用、交付时翻转 |
| DataDriven | 15546 | 四段顺序链（段饱和语义） |
| DataDriven | 18996 | **双阵营配对狩猎 + 进区域推进**（EnterZone→s4），s4 击杀即翻 REWARD |

- 明确范围外：`SimpleUseItem` 族（1107 等）为世界/系统发放形（无 NPC 接取边），
  契约门 v1 只覆盖 NPC 接取形；collect 族进度（1103）待接背包事实种子后纳入。
- 协议回环注意：`QuestProtocolLoop` 对 WORLD_EVENT 走运行时桥、其余请求走**真实 CM 包路径**，
  因此行走器请求序列必须协议忠实（见 QE-082）。

### 协议级逆向发现（QE-082 已沉淀，写门禁/行走器必读）

1. **接取合同有两形态**：询问窗形（`select1`(1011) 页按钮 1007 → 页 4 → 1002）与
   直接接取形（入口页可见按钮就是 `20000/20001`，没有 1007）。行走器提交边必须按
   "当前页可见"挑选，不能用 findFirst 取第一条（1002 恒在转移表前部）。
2. **`SELECT_QUEST_REWARD(1009)` 冷发会被真实 CM_DIALOG_SELECT 路径拒绝**（NO_MATCH）：
   必须先与交付 NPC 开对话（QUEST_SELECT 出报告页 1352/2375/10002），
   再 `clickVisibleAction(1009)`——与 `Quest1118ProductionFlowTest` 的顺序一致。
3. **`dialogId=-1`（USE_OBJECT）是 CM_SHOW_DIALOG 的"开对话"语义**，不是 dialog-select 动作；
   触发 `TalkToNpc(npc, -1)` 边（REWARD 节点预览路由）要用 `useObject(npc)`。
4. **页 10（SELECT_QUEST）残留上下文会污染后续动作**：上一步 afterCommit 落在页 10 后，
   冷发任何非 31 动作都会被包路径误读（18996 的 SETPRO2 冷发 UNKNOWN 即此因）。
   换 NPC 或页 10 残留时必须先重开对话拿页上下文。
5. **DD 链对话步骤是双协议注册**：同一推进既有 `TalkToNpc` 形也有 `QuestDialog`（无主）形，
   进度边过滤要覆盖两种事件空间（QE-017 共号空间的另一面）。
6. **配对狩猎（Cradle 形）节点上同时存在"条件门控推进边"与"计数边"**（如 s2 上
   `VariableAtLeast[var2,1]→s3` 与 `VariableBelow[var1,1]→自增`），驱动器必须
   **按序尝试全部进度边**（未命中请求无副作用），不能取第一条。
7. **kill-flip 与交付时翻转并存**：15546/18996 末段击杀直达 REWARD 节点；1118/15042 在
   1009 交付边翻转。契约合同允许两者，但进度期间不得 COMPLETE（已锁）。
8. 结论修正（对 Goal 文档的前提核对，已获批准的计划）：奖励窗必须分档查表
   （`rewardWindowForTier` → 5/6/7/8/45/46，QE-028），不能固定下发 5；
   "ask_quest_accept 动作"= 1007，页 4 是服务端下发的页面 id，两者不同空间角色。

## Phase 2 已收口：SimpleHunt 规范形竖切片（2026-09-26）

**落点（`RetailSimpleHuntDefinitionCompiler`）**：canonical 旗标只穿过 **7 参家族重载**
（`RetailQuestDriver.compileSimpleHunt` L861 唯一生产调用点）；8/9 参重载（DataDriven 路由）
与串行链保持 legacy 形——DD 车道 20035 红不被本片搅动。

- `canonicalAcceptFlow`：`QUEST_SELECT → ShowQuestDialog(4)` 自环；`1002 → 建档+页1003`、
  `20000 → 建档+关窗` 两形提交；`1003 → 页1004`、`1004/20001 → 关窗` 拒绝族；
  `FINISH_DIALOG → ShowQuestSelectionDialog(10)`。**无** 1007 中转、无 select1/select_none
  阶梯、无 `SELECT1_1` 续页（页 id 与按钮动作共号空间，QE-017）。
- 简报规范形：简报 NPC `QUEST_SELECT → 首个网格段 + LEVEL_AND_VISIBILITY_REFRESH + 关窗`
  单边直达（删 select2 页链）。
- 报告规范形：满段节点 `QUEST_SELECT → REWARD + ShowQuestDialog(rewardWindowForTier)`
  分档直达（删 1352/2375 报告页与 1009 中转；未满段无对话路由，关窗兜底交给 DialogService）。
  窗页按 `metadata.rewardGroups().size()-1` 查表，不固定 5（QE-028）。
- `completeFlow` 零改动（8..23 + 108/110+k 自动通道 + 职业路由本就规范）。

**级联收口**：
1. 审计收窄：`QuestClientContractGateTest` 对审计行双侧过滤 `RetiredQuestIds`
   （QE-066 同源对拍），`[quest-client-contract] retired-quest audit rows narrowed out=2431`；
   默认与严格模式（`-Dquest.client.contract.failOnStaleBaseline=true`）**双绿** ⇒
   introduced=0 / stale=0，retirement 语义正式移交 `RetailQuestContractTest`。
2. 契约门行走器：`walkToAcceptCommit` 增页 4 原生提交回退——翻到询问窗后若无可见提交按钮，
   直接回传接取提交边（客户端页树不含页 4 动作行，属服务端单侧下发）。
3. 测试对齐：`Quest1112ProductionFlowTest` test1 改 QUEST_SELECT 交付断言（REWARD 态
   1009/-1 = 预览语义，预览边无 `SyncQuestState` ⇒ 交互对象解析不重跑）；
   `QuestLegacyMonsterHuntProductionFlowTest` 21120/30715 两块改交付边断言（30715 方法更名
   `DeliversStraightToTheTieredRewardWindowAfterTheRetailKill`，保留 REWARD 无重开 +
   1009 预览 + 完成段锁定）；`MonsterHuntFamilyDefinitionTest` 补窗口自动确认位期望
   （110+k/108，归属早期 auto-reward 通道补账，与本片 canonical 无关）。
4. 指纹重冻（rebaseline 留痕语义，两列=真端指纹）：
   `retail-simple-hunt-ir-fingerprints.tsv` 441 行 **id 集不变**；裁定表
   `retail-simple-hunt-adjudicated-ir-fingerprints.tsv` 298→344 行（**+46 纯增量** =
   并行车道新迁移裁定任务补齐冻结证据，与 plain 表零交集）。
5. 家族门禁/等价门零形状锁：`RetailSimpleHuntFamilyGateTest` 只锁受理/拒绝登记（全绿），
   等价门 XML 对拍因 XML 已退役恒空转、判据由冻结指纹承担（`retail.hunt.rebaselineOut`
   注释即为此变更预留的留痕通道）。

**验证**：聚焦 7 类 57 例——第一轮 2 红（1112 预览断言过宽 + 家族测试自动位缺账）修复后
`gates/phase2-focused-3.log` **全绿**；`gates/phase2-contract-strict.log` 严格模式绿。

**T3 锁旧形测试分拣（收口轮）**：首轮 T3（`gates/T3-023514.log`，2014 例 129F/27E）对兄弟车道
基线 `scriptdll-quest-driver/gates/T3-232501.log`（2013 例 117F/22E）身份集对拍 = **ADDED 18 /
REMOVED 2**。REMOVED 两个正是本片修复（家族测试 auto 位、等价门 +46 缺证）；ADDED 18 逐条归属：
**13 个涉事任务全部 RETAIL_TABLE SimpleHunt**（1346/1347/1376/2485/3329/13702/11110/24153/24155/
23702/24112/1370/1320+1321），按"零售侧改、XML 侧留"分拣更新 **12 个测试类**到规范形——
报告页/1009 中转断言改满段 QUEST_SELECT 交付（1346/1347/1376/批量 2485·3329·13702/11110/
23702·23703·23705/24153 尾段/PacketOrder 24153/PrematureReward 1347 参数）；简报两步
（SETPRO 按钮 + SELECT2 页）改一步 QUEST_SELECT 清 SECTION_5（24112 按族分支、24153/24155/
leatherWings）；NONE 态入口 SELECT1(1011) 改页 4（1347）；派发动作 SELECT1_1(1012) 改
QUEST_SELECT（1370/1320·1321，实测规范形任务与旧梯任务在 QUEST_SELECT 上共号派发、1012 只剩
串行链/XML 保留行）。修复类复跑 `gates/phase2-t3fixes-1/2.log` 本片归属红全部转绿。

**收口判定（`gates/T3-030925.log`，2014 例 115F/22E/1S）**：对兄弟基线身份集对拍 =
**ADDED 0 / REMOVED 2**（移除两个 = 本片修复的兄弟遗留红：`MonsterHuntFamilyDefinitionTest.
completionRewardsCarrySelectableItemsAndFixedRewards`、`RetailSimpleHuntEquivalenceGateTest.
frozenIrFingerprintsCoverExactlyTheMigratedQuests`）；对本片首轮 T3 = FIXED 18 / NEW 0。
基线自带红（16823/50091/28743=DataDriven、2569/11139=SimpleTalk 等族）在 T3-232501 里
逐字在册，非本片新增，归兄弟车道清账。

## Phase 2 续片已收口：CombineTask 规范形（2026-09-27）

选族说明：滚动次序下一面。**CombineTask（574 个退役 id，全族同形）**，独立编译器
`RetailCombineTaskDefinitionCompiler`，生产驱动器与门禁夹具同调入口（原位翻转落点）。

**勘察裁定（本片关键）**：交付段**已经是规范形**——1009 双 prio（成功 = HasItem 产物 +
回收剩余分量 → REWARD + 关窗；缺产物 = SELECT3_2 合成回退页）没有页链自环（族形本来就不经
服务端报告页；SELECT3_2 是客户端本地合成对话回退页，非页链产物，保留）。完成流（8..23 +
108 双协议）/放弃流（忘配方）零改动。**唯一 canonical 化面 = 接取段**。

**落点**：接取段原位翻转——QUEST_SELECT 直发接取窗（页 4），select1 页与
ASK_QUEST_ACCEPT(1007) 中转随页链删除；复用上一片 `canonicalAcceptFlow` 三参重载承载
"接取发分量 + 学配方"（GiveItem×N + LearnRecipe）；私有 `acceptFlow` 与死代码 `talk`
helper 删除。

**形状冻结器反应与收口**：
1. 门禁 `RetailCombineTaskGateTest` 断言只锚提交边/交付边/完成边（不锚页链）——**零同步**；
   唯一红 = 指纹门（574 行全漂移 = 接取流变化波及每行，id 集一致）；
   `retail.combine.fpOut` 重冻回写后 3/3 绿。
2. 契约门不加 CombineTask 用例：其进度 = 游戏内制作（craft）产物，黑盒行走器无法驱动
   （`driveProgress` 会停在"没有任何进度边可驱动"），契约门家族覆盖按"黑盒可驱动"挑选。
3. T2（全家族 574 id）仅 3 红：`MigratedQuestRepair.groupOwners…` 基线在册、
   `driftVersusShells`=20035 车道红（不追）；`RetailPatternAI2Test`（ai 包）NPE 在
   `RetailPatternAI2.chooseAttackIntention`——与本片 questEngine 改动面零关联，兄弟 AI
   车道在飞/flaky，不越界不修。

**收口门禁（对 SimpleItemPlay 收口基线 T3-060336）**：T3 `gates/T3-062949.log`
（2016 例 115F/22E/1S）身份集 **ADDED 0 / REMOVED 0**（138→138 完全同态）；T1
`gates/T1-063617.log`（76 例 1F，唯一红 = `RetailDataDrivenGate.driftVersusShellsIsRegistered`，
20035 车道红，连续五轮同身份）。

## Phase 2 续片已收口：DataDriven 单段网格规范形（2026-09-26）

选族说明：滚动次序下一面 = **DD 系（最大族，686 行进本片）**。按形状分批：本片收口
**单段网格**（`compile` 直通路径）；多段顺序链（`compileSequentialStages`）与 talk/collect
链编译器留下一续片（同族对撞避免）。**HandinDialogFlow 已裁定不纳入 canonical 化**：
客户端五页词汇逐页镜像（每页每按钮都有路由是它的合同本意）、被 DD 链编译器共享消费、
`HandoverContinuationContract` 独立锁定——三票否决。

**落点**（QE-083 调用图判定）：`RetailSimpleHuntDefinitionCompiler` 新增公开规范形重载
`compileCanonical(plan, metadata, …)`（11 参私有 compile 尾参 canonical=true 的穿线封装）；
`RetailDataDrivenDefinitionCompiler` 单段网格调用点翻转 `compile → compileCanonical`。
零表结构改动、零登记表变化（686 行全为既有 DD 行的形状迁移，无新增/退役）。

**形状冻结器反应与收口**：
1. **DD 指纹门**：1217 行 id 集不变，686 行指纹漂移（恰为单段网格面）；`/tmp/dd-fp-new.tsv`
   防掩盖对拍（非漂移行零漂移、漂移集与单段网格集一致）后整文件安装重冻。
2. **DD 门禁 5/6 绿**：唯一红 = `driftVersusShellsIsRegistered` 的 20035 登记分歧
   （兄弟 SimpleTalk 链车道在飞，连续六轮同身份，不追）；契约门 `RetailQuestContractTest` 绿。
3. **T2 分拣**（T2-065114：74 红 = 31 基线在册 + 43 新增 + 车道红）：43 新增集中 4 类，
   全部按"零售侧改、XML 侧留"分拣修复：
   - `QuestMonsterProgressContractAuditTest`（2）：`assertGridSpecialMission` 改规范形
     （reportNpc 从满段 QUEST_SELECT 交付边提取、交付 afterCommit = [LEVEL 同步, 查表分档窗]、
     未满段零路由否定断言）。
   - `QuestPrematureRewardRouteExclusionTest`（15 新 + 2 基线一并收）：`grid()` 契约转
     canonical 变体；`assertCompletedReport` 窗口按形状二分（QUEST_SELECT=档位查表 /
     1009=固定窗 1）；28743 契约 `grid(1, 804732)`（探针证据：接取变体 206395/6/7 无交付边）；
     19631 负控改为"交付边原样搬家到未满段"（仿 1347 gate 的 re-source 构造）；顺手收两基线红
     （2569 备选段 s2 已被 XML 侧修复移除 → 改锁 REWARD 态 1009 重开预览；28743 canonical 伤亡）。
   - `QuestArchivesMissionCounterProductionFlowTest`（4）：满格 1009 → QUEST_SELECT 交付、
     未满段门控 1009 → 零对话路由断言（系统发放行无接取流，FINISH_DIALOG 出口不存在）。
   - `QuestLegacyMonsterHuntProductionFlowTest`（23→0）：challenge 入口页 4762→页4 +
     满段交付；50 行成功页测试改**形状二分 helper**（canonical=无 QUEST_SELECT/1009 报告通道、
     legacy=保留完成页自环）；`assertGridSimpleMonsterHunt`/`assertGridReportedMonsterHunt`
     交付边重锚；26988 单测同形；顺带去重 reported 尾部重复断言。
4. **T3 首跑暴露 T2 选择器盲区**（T3-073402：ADDED 19 / 10 类）：对齐/行契约类
   （Growth/19637/25512/25640/25698/28932/A03/EventShard/Section0/Cradle/DataDrivenHunt）
   锁单任务对话形状，同三模式分拣（入口页 4762→页4；1009→QUEST_SELECT 交付+档位窗；
   reportNpc 提取点改从交付边）。复跑 31/32 绿（唯一剩红 = 21065 基线在册）。

**判例级发现（QE-085）**：canonical 未满段**不是零对话路由**——`canonicalAcceptFlow` 的
FINISH_DIALOG(1008) 关窗出口挂在接取目标段（a0 或 started）；"零路由"断言必须收窄为
"无 QUEST_SELECT/1009 报告通道"。系统发放行（EnterWorld/SystemGrant）无接取流，才可断言全零。

**保留为基线在册红（不越界裁定）**：`quest16823And26823AutoGrant…` 的 939-movie 断言
（16823 = DD talk-hunt **chain** 行，兄弟轴）；`repairedMultiKillQuestsDeclareSeparateKillCounters`
的 50091 var1（P0c-53 组表采纳形 vs 旧修复契约）。归各自车道裁。

**收口门禁（对 CombineTask 收口基线 T3-062949，138 身份）**：T3 `gates/T3-075615.log`
（2016 例 112F/23E/1S）身份集 **ADDED 0 / REMOVED 2**（两例 = 本片有意收口的基线红
2569/28743，逐条登记如上，137→135）；T1 `gates/T1-080420.log`（76 例 1F，唯一红 =
20035 车道红，连续六轮同身份，零新增）。

## Phase 2 续片已收口：DataDriven 多段顺序链规范形（2026-09-27）

选族说明：DD 系第二片。多段 hunt 行（2..5 段，`!allPvp && huntStages>1`）由
`compileSequentialStages` 合成——链式前缀节点 + 首个未满段推进语义与网格片共享同一
`compile()` 主体，canonical 分支直接复用（无新通道）。

**落点**（QE-083 原位翻转）：`compileSequentialStages` 尾参 `canonical false→true`
（唯一生产调用点 = DD 路由，调用图核实无其他消费者）；`incompleteReportRoutes` 参数在
canonical 分支不再被读。零登记表变化。

**形状冻结器反应与收口**：
1. **DD 指纹门**：1217 行 id 集不变、**36 行漂移**（恰为顺序链行集：14252/15001/15020/
   15073/15100/15104/15203/15324/15406-8/15546/15580/16802/16806/17541/24252/25060/25324/
   25406-8/25546/25580/26802/26806/27541/30514/30564/80731-2/80799-80803）、1181 行零漂移；
   dump 防掩盖对拍后整文件安装。
2. **DD 门禁 5/6 绿**（唯一红 = 20035 车道红，连续七轮同身份）；契约门绿。
3. **T2（36 id，T2-081659）**：29 红 = 8 基线在册 + **21 新增（12 方法/8 类）**，全部按
   QE-085 三模式分拣：`assertSequentialSectionChain`（Legacy 类顺序链断言器，3 单测共用）
   未满链节点完成页自环+门控 1009 → 零报告通道、满链 1009 → QUEST_SELECT 交付；
   `QuestArchivesDualCounter`（16802/26802 双计数）满段/领奖态断言同形；`CounterSource`、
   `ResidualCounterLocks`（25324 三段链）、Audit 的 15001/14252+24252/stepZeroMultiCounter
   逐处交付边重锚；16802/26802 对齐类同形。
4. **基线红交互判例**：16802/26802 的 `areaGrantStarts...` 在基线在册但红因在更后面的
   completion 断言（dialogId=110 职业路由）——本片把失败点**提前**到形状段，修复形状断言后
   失败点**退回基线红位置且红因逐字相同**（completion 断言不动）。T3 对拍身份集不变。

**收口门禁（对 DD 单段网格基线 T3-075615，135 身份）**：T3 `gates/T3-083216.log`
（2016 例 112F/23E/1S）身份集 **ADDED 0 / REMOVED 0（135→135 完全同态）**；T1
`gates/T1-084004.log`（76 例 1F，唯一红 = 20035 车道红，零新增）。

## Phase 2 续片已收口：SimpleItemPlay 规范形（2026-09-27）

选族说明：滚动次序下一面。**SimpleItemPlay wave-1（6 个退役 id：13704/13708/19048/23704/
23708/29048）**，独立编译器 `RetailSimpleItemPlayDefinitionCompiler`，生产驱动器与门禁夹具
同调 4 参 `compile` 入口（原位翻转落点）。门禁无形状 inspect（形状由冻结指纹 + T2/T3 锁定），
同步零工作。

**落点（`build()` 接取段 + 交付段；演出推进边本就是规范形不动）**：
- 接取规范形：QUEST_SELECT 直发接取窗（页 4），select1 页与 ASK_QUEST_ACCEPT(1007) 中转删除；
  `RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow` 新增三参重载（接取提交边挂调用方
  动作）承载 ItemPlay 的"接取即发演出道具"（GiveItem）——两参重载委托三参（actions 空），
  SimpleHunt canonical 调用点不受影响。
- 交付/预览规范形：REWARD 态 QUEST_SELECT/USE_OBJECT 重开分档奖励窗（QE-028 查表，SELECT5
  报告页删除）；1009 预览通道保留并回收演出道具（回收时机不变），窗页同样查表。
- 演出推进边（UseItem→REWARD）、完成流（8..23 + auto 通道）零改动。

**形状冻结器反应与收口**：
1. 门禁唯一红 = 冻结指纹门（6 id 全漂移，本片有意变更；拒绝码 problems=[] 零漂移）；
   `retail.itemPlay.fingerprintOut` 重冻（id 集一致）回写后全绿；`QuestItemPlayGrantGateTest` 绿。
2. 契约门 `SimpleItemPlay#13704` 用例（既有）规范形下**一次通过**——道具接取分支（UseItem
   接取相位）与分档窗交付在黑盒行走下直接兼容。
3. T2（家族 6 id）7 红：`driftVersusShells`=20035 车道红、三个 `productionJourney…` 基线在册
   不追；本片伤亡 3 处分拣——`Quest13704/13708/19048…ClientDialogAlignmentTest` 的
   `returnsToXxx…` 方法改写为规范形断言（USE_OBJECT+QUEST_SELECT 双预览边开分档查表窗 +
   1009 回收保留），复跑三修复全绿。

**收口门禁（对 SimpleUseItem 收口基线 T3-053720）**：T3 `gates/T3-060336.log`
（2016 例 115F/22E/1S）身份集 **ADDED 0 / REMOVED 0**（138→138 完全同态）；T1
`gates/T1-061028.log`（76 例 1F，唯一红 = `RetailDataDrivenGate.driftVersusShellsIsRegistered`，
20035 车道红，连续四轮同身份）。

## Phase 2 续片已收口：SimpleUseItem 规范形（2026-09-27）

选族说明：滚动次序下一面（SimpleTalk 仍被兄弟车道占用）。**SimpleUseItem（102 个退役 id）**，
独立编译器 `RetailSimpleUseItemDefinitionCompiler`，生产驱动器与门禁夹具同调 7 参 `compile`
入口（原位翻转落点）。

**落点（`build()` 报告/交付段；接取段本就是规范形不动）**：
- 接取段既有形已合规：`UseItem → 页 4` + 无主 `QuestDialog 1002/1003/1008`（无 select1 阶梯）。
- 交付规范形：三种客户端交付模式统一到同一 QUEST_SELECT 入口直翻 REWARD +
  `rewardWindowForTier` 分档窗（QE-028）——CHECK 形（5 任务，80482/80486/80554/80558/80612）
  = QUEST_SELECT 带交付物 HasItem 门控；蛋糕事件形（80008/80009）= 带工作物品门控并保留
  `SetVariable(var0,1)` 领奖投影（QE-051）；默认形 = 无条件直翻。
- 随页链删除：SELECT5 报告页自环（`REPORT_PAGE` 常量一并删）、39/20002 检查对、SELECT6
  失败页关窗出口、1009 中转（含 cake 的双 prio 失败回页）；未集齐零路由（DialogService
  关窗兜底）。`exits` 形参保留稳定公开签名、build 不再消费。
- 掉落箱对象路由、previewFlow、completeFlow（8..23 + auto 通道）、journalRowRepair、
  cake 的 LevelUp 弃任边零改动。

**形状冻结器反应与收口**：
1. 家族门禁 `RetailSimpleUseItemGateTest` 唯一同步点 `hasReport`（SELECT5+1009 →
   QUEST_SELECT 分档窗+REWARD 判定）；语义/漂移/归属门当场绿，唯一红 = 冻结指纹门。
2. 指纹重冻（`retail.useItem.fpOut`）：102 行 id 集不变、节点数全部不变、transitions 每任务
   -1~-3（页链边删除）；回写后门禁 5/5 绿。
3. 契约门新增 `SimpleUseItem` 两条用例并加**道具接取分支**（`runItemUseLifecycle`：
   种子用物品本体 + 全部交付边 HasItem 门控物 → `useItem` 弹页 4 → 无主 1002 建档 →
   进度/交付/结算与 NPC 接取族同构）：`1107`（默认直交形）一次通过；`80482` 因接取前置
   （StartEligible 链任务）黑盒不可满足，CHECK 形代表换 `80554`——两形一次通过。
4. T2（全家族 102 id）28 红：25 个基线在册不追；本片伤亡 3 处全部分拣——
   `CollectTurnIn.simpleSelect5Quests…` 的 80482/80486 移出到新
   `canonicalUseItemQuestDeliversOnQuestSelect`（QUEST_SELECT+HasItem 交付 + 39 对/SELECT5
   自环否定断言）；`JavaHandlerFamily.lostAxe…`（1107）交付边断言 1009→QUEST_SELECT(31)；
   `QuestEventQuestBatch.cakeQuests…` 改写为 QUEST_SELECT 单边 + 1009 失败自环否定断言
   （方法名随之 `…OnQuestSelectDelivery`）。复跑三修复全绿（余红均为基线在册）。

**收口门禁（对 SimpleSerialHunt 收口基线 T3-045719）**：T3 `gates/T3-053720.log`
（2016 例 115F/22E/1S）身份集 **ADDED 0 / REMOVED 0**（138→138 完全同态——T2 三伤亡修复后
T3 全树无痕）；T1 `gates/T1-054340.log`（76 例 1F，唯一红 = `RetailDataDrivenGate.
driftVersusShellsIsRegistered`，20035 车道红，连续三轮同身份）。

## Phase 2 续片已收口：SimpleSerialHunt 规范形（2026-09-27）

选族说明：SimpleTalk 仍被兄弟车道占用，按滚动次序取 **SimpleSerialHunt（10 个退役 id：
13918/16991/18911/18912/23918/26991/28911/28912/30600/30610）**。该族没有独立编译器——
`RetailQuestDriver.compileSimpleSerialHunt` 把真端表行转成 hunt 形 plan 后调
`RetailSimpleHuntDefinitionCompiler.compileSerialChain → buildSerialChain`（串行阶梯合成）。

**落点（`buildSerialChain` 三处原位翻转——该私有 builder 的唯一调用链就是生产驱动器与
门禁夹具共享的 5 参 `compileSerialChain` 入口，P0c-5b 假绿教训不破）**：
- 接取：legacy `acceptFlow`（select1 阶梯）→ `canonicalAcceptFlow`（QUEST_SELECT 直发页 4，
  1002/20000 两形提交、拒绝族、FINISH_DIALOG→页10）；EnterWorld 接取边不变。
- 简报：两行（QUEST_SELECT 开 select2 页保持 var5=1 + SETPRO1 清标志）→ 一行（简报 NPC
  QUEST_SELECT `started→briefed` 一步清 SECTION_5 + LEVEL 同步 + 关窗）；"见中间人才开计数"
  语义由目标投影承担（与网格形 SimpleHunt canonical 完全同构）。
- 交付：SELECT2 报告页 + 其 SETPRO1 按钮路由 + 1009 中转 → 满段 QUEST_SELECT 直翻 REWARD +
  `rewardWindowForTier` 分档窗（QE-028 查表）。
- 击杀链边（`chainedEdges` 乱序不计数）、节点集、completeFlow（8..23 + auto 通道）零改动。

**形状冻结器反应与收口**：
1. 家族门禁 `RetailSimpleSerialHuntGateTest` 两处同步：简报清除路由断言 SETPRO1→QUEST_SELECT
   （+CloseDialog），`hasReport` SELECT2→分档窗+REWARD 判定；语义门/漂移门/归属门当场绿，
   唯一红 = 冻结指纹门（10 id 指纹全漂移——本片有意变更）。
2. 指纹重冻（`retail.serialHunt.fpOut` 留痕通道）：id 集不变、节点数逐 id 不变（9/14/9/7/9/14/
   9/7/7/7）、transitions 每任务 -3~-4（页链边删除）；回写
   `retail-simple-serial-hunt-ir-fingerprints.tsv`（10 数据行）后门禁 5/5 绿。家族全为退役行
   （漂移证据由冻结指纹承担），`equivOut` 无 XML 保留行需要对拍、未触发。
3. 契约门 `SimpleSerialHunt#13918` 用例（黑盒生命周期，QE-082 协议忠实行走器）规范形下
   **一次通过**——行走器按"当前页可见"挑边，两形态兼容。
4. T2（10 个家族 id）扫出 4 红，归因三步：`ChainEliteLadder.offerAndCompletion…` 与
   `RewardNpcOwnership.antidoteQuest…` 在 T3-042013 在册（兄弟基线既存）、
   `RetailDataDrivenGate.driftVersusShells…`=20035 车道红（兄弟在飞）——三红不追不修；
   本片唯一伤亡 `CounterChainBriefingStageContractTest.briefingPageKeepsTheFlag…`（30600/30610
   的 else 分支锁两步简报 legacy 形）→ 与 24112 分支合并为统一规范形断言
   （改名 `briefingTalkClearsTheFlagInOneStepAndKillsStayGated`），复跑 6/6 绿。

**收口门禁（对 SimpleCollectItem 收口基线 T3-042013）**：T3 `gates/T3-045719.log`
（2015 例 115F/22E/1S）身份集 **ADDED 0 / REMOVED 0**（138→138 完全同态——T2 伤亡修复后
T3 全树无痕）；T1 `gates/T1-050426.log`（76 例 1F，唯一红 = `RetailDataDrivenGate.
driftVersusShellsIsRegistered`，20035 车道红，兄弟 DD 车道在飞，与 CollectItem 收口轮同身份）。

## Phase 2 续片已收口：SimpleCollectItem 规范形（2026-09-26）

选族说明：滚动次序的下一族原是 SimpleTalk，但兄弟车道（scriptdll-quest-driver）此刻在飞改
SimpleTalk 链输入表（本会话内 `find -newer` 实证其表后写）——为避免同族对撞，本片取
**SimpleCollectItem（175 个 RETAIL_TABLE 任务）**，共享 flow 同构、方法论直接复用。

**落点（`RetailSimpleCollectItemDefinitionCompiler`，公开入口原位翻转——驱动器/门禁夹具同入口，
P0c-5b 假绿教训不破）**：
- 接取规范形 `canonicalAcceptFlow`：QUEST_SELECT 直发页 4，1002/20000 两形提交、拒绝族、
  FINISH_DIALOG→页10；删 SELECT1 入口页、1007 中转、SELECT1_1 续页梯；**保留**真端表声明的
  `setproRoute`（SETPRO1 备选接取，非页链产物，门禁要求项）。
- 简报规范形 `canonicalBriefingFlow`：中间 NPC QUEST_SELECT 一步直达采集行 `v{collect_progress}`
  （LEVEL_AND_VISIBILITY_REFRESH + 关窗）；select2 页链删除。掉落门 `var0 == collectingStep`
  与启动期 ACTION_ITEM_USE 合同由目标节点投影天然保持。
- 交付规范形：QUEST_SELECT 带整组 HasItem 门控直翻 REWARD + 档位奖励窗（QE-028 查表）；
  SELECT5 报告页与 39/20002 检查对随页链删除，未集齐零路由。
- **legacy statics 全部保留**（`acceptFlow`/`acceptContinuation`/`itemReport`/`reportNpcExit`/
  `completeFlow`/`journalRowRepair`）——5 个 DataDriven/Handin 编译器仍在共享调用
  （级联 #1 预言）；`briefingFlow`/`REPORT_PAGE` 无外部调用者，随翻转删除。

**级联收口**：
1. 门禁 `RetailSimpleCollectItemGateTest.inspect()` 三处形状检查随族更新：SELECT1_1 续页要求删除、
   `hasBriefingChain`（逐跳页链）→ `hasCanonicalBriefing`（一步直达）、`hasReport`（SELECT5+39 双
   分支）→ QUEST_SELECT+HasItem 交付。
2. 生成物重算（旗标即留痕通道）：`retail.collect.fpOut` 重冻
   `retail-simple-collect-item-ir-fingerprints.tsv`（177 行，退役行全量新指纹）；
   `retail.collect.equivOut` 重算 `retail-simple-collect-item-drift.tsv`（209 行——`classify()`
   对退役行抄冻结登记值，只更新 XML 保留行的 DIFF 轴：39/20002/1012 轴移到 XML_EXTRA 侧，
   属"XML 有而规范形不再下发"的真陈述）。
3. 契约门新增 `SimpleCollectItem#14120` 用例（带中间 NPC 简报步的富形状）：行走器按元数据
   `itemRequirements` 种子背包事实（`QuestJourneyRunner` 既有 `initialInventory` 通道），
   HasItem 交付门在真实包路径下放行——**一次通过**。
4. T2（16 个代表 id）扫出锁旧形测试类 `CollectTurnInClientActionAlignmentBatchTest`：
   15 族归属分拣——1103（SimpleCollectItem）分拣为规范形断言
   `canonicalCollectQuestDeliversOnQuestSelect`；其余 80482/80486（SimpleUseItem）、
   30312/30314/30315/1124（SimpleTalk）等族未受本片影响，SELECT5/39 断言原样保留；
   `quest18745KeepsTheRewardOwnerExclusive` 的红在兄弟基线 T3-232501 与本车道 T3-030925
   **逐字在册**（DataDriven 族，兄弟车道在飞），非本片新增。

**验证**：门禁 `gates/phase2b-collect-gate-4.log` 全绿；契约门
`gates/phase2b-contract-1.log` 绿（9 家族用例）；T2 `gates/T2-033952.log`。

**收口门禁（对 SimpleHunt 收口基线 T3-030925）**：
- T3 `gates/T3-042013.log`（2015 例 115F/22E/1S）：身份集 **ADDED 0 / REMOVED 0**
  （138→138 同态）；对上轮 T3-035909 为 ADDED 0 / REMOVED 3，消除的三个方法即本切片
  规范形改写（`QuestInteractionObjectContractGateTest.collectItemTalkStepsLandOnTheirCollectingStep`
  的 SETPRO1→QUEST_SELECT 一步推进、`QuestHaramelItemCollectingRegressionTest` 的
  canonicalTurnIn 分支（18501/28501/18503 规范形、18509/28509/28503 旧形）、
  `LegacyTemplateMirrorRouteRegressionTest` 把 2527/3096 从旧形镜像循环分拣为
  QUEST_SELECT(31)+HasItem 交付断言）。T3 全部红身份在兄弟基线 T3-232501（140 身份）
  内逐条在册——兄弟 SimpleTalk 车道基线既存，不追不修。
- T1 `gates/T1-042720.log`（76 例 1F）：唯一红
  `RetailDataDrivenGateTest.driftVersusShellsIsRegistered`（任务 20035 车道红，兄弟 DD
  车道在飞，上轮 T1-035200 同身份在册）。
- 操作纪律两条（本片实锤）：① `surefire:test` 不触发编译——改测试后必须 `mvn test`（或
  test-compile）再跑聚焦类，否则验的是旧类（本片三修复第一次复跑全被旧类打回）；
  ② T3 身份集对拍必须统一 `LC=C sort -u` 后用集合运算——默认 locale 的 comm 输出会
  造出 REMOVED 98/ADDED 36 的全假象（python 重算真相是 0/0）。

## Phase 2 尾片已收口：DD 四子面规范形（talk / collect / 混合链，D-a + D-b 合并收口，2026-09-27）

选片说明：DD 家族剩余 272 行（talk 111 + collect 5 + talk/collect 链 37 + talk/hunt 链 119）切两片连续施工、
**合并收口**（D-a = A+B 面 116 ids，D-b = C+D 面 112 ids）：两片共用同一张 1217 行 DD 指纹表与同一个 T1 门，
分两次收口只会把同一套重冻/对拍做两遍。

**落点（5 个生产文件 + 2 个测试基础设施文件）**：

- 接取段四族全切 `RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow`：talk（`RetailDataDrivenTalkCompiler:80/176`）、
  collect（`RetailDataDrivenCollectCompiler:94`，删续页分支、保留 `setproRoute`）、
  talk/collect 链（`:401`，三参带授予）、talk/hunt 链（`:736`，三参带授予）——上一片遗留的"事后授予挂载"循环
  与死方法 `isAcceptRoute` 一并删除；12 行物品接取新增共享 `canonicalItemAcceptFlow`（UseItem → 页 4 +
  无主 1002/20000 + 拒绝族 + 1008）。
- 报告/交付段：talk `assemble` 与 collect 交付段改 `QUEST_SELECT → REWARD + 分档奖励窗`（1009 降为预览通道，
  即真端"重开窗口"语义）；分档窗统一走新助手 `RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage(metadata, questId)`，
  交付边统一走新助手 `canonicalDelivery(...)`（after 序 = LEVEL_AND_VISIBILITY_REFRESH + 开窗）。
- **零奖励组兜底（本片新增不变量，已沉淀进 QE-028）**：80829 是零奖励组事件任务，
  `rewardGroups().size()-1 = -1` 直接进档位查表会抛越界 ⇒ `deliveryWindowPage` 在空集合时兜底固定窗 1
  （与引擎 `completeFlow` 预览同口径）。这是本片唯一一次"编译失败级"回归，逐条登记在案。
- **混合链的步进语义一律保留**：39 检查对是**进度推进**（不照搬网格片的"删检查对"）、talk 页面梯与 hunt
  计数段不动（canonical 只换接取/简报/报告界面段）；14 行 EnterArea 走 `SystemGrant`，接取段零工作。

**测试基础设施（首见：真端原生相位 A 的规划器原语）**：`QuestProductionJourneyPlanner`/`Executor` 新增
`NATIVE_ACCEPT_ACTION`——页 4 是客户端原生窗口，但**只在任务自己的 HTML 完全没有登记该页按钮时**才回退到
原生提交（1002/20000）；登记了按钮的任务（如 1103 的页 4 只声明接受 1002 / 拒绝 1003）必须走可见性判定，
否则规划器会发明客户端根本不会发的按钮（首轮 T3 正是被这条打红，见下"T3 首跑"一行）。

**级联收口**：

1. 门禁：`RetailDataDrivenGateTest` 的指纹门随重冻同步（形状门不变）；12 个锁旧形的测试类分拣
   （`GrowthQuestDialogPageAlignmentTest` 页梯族、`Quest80787To80794RetailAlignmentTest`、
   6 个 SimpleStart/Report 族的 `*ClientDialogAlignmentTest`、`Quest25670/28931/26800…`、
   `QuestBatchReportNpcAlignmentTest`）——断言按**真端形**重锚，XML 保留行不动。
2. 指纹重冻（`-Dretail.dataDriven.fpOut` → `retail-data-driven-ir-fingerprints.tsv`，1217 行）带**防掩盖对拍**：
   D-a 漂移行 = **116 == 预期影响面**、非漂移行 **0 变化**；D-a∪D-b 漂移行 = **228 == 预期面**、
   非漂移行 **0 变化**；重冻后的冻结表与战后 dump **逐字一致（0 行差异）**。
3. 契约门 `RetailQuestContractTest` 新增 `ContractCase("DataDriven", 1919)`（noProgress 用例），并放宽
   `driveProgress`：声明了推进边但当前节点无需计数步的图也算通过。

**验证**：

- 聚焦：DD 门 + 契约门 + 清单门 + 4 个分拣类 = 31 例，唯一红 = 20035 在册红；规划器原生原语修正后
  复跑 4 个分拣类 + `QuestProductionJourneyTest`（1103 计划形状锁）= 32 例全绿。
- T2（228 ids，两跑同形）：`gates/T2-103314.log` 与终版 `gates/T2-105158.log`（544 例 34F/6E/1S）——
  **新增失败 0**（58 个失败身份全部在基线 T3-083216 内逐条在册）；23 个 T1 门类全部跑到，唯一红 =
  20035 车道红，与基线 T1-095231 同身份。
- T3：`gates/T3-104123.log`（首跑 ADDED 1：1103 的计划形状锁被新原语改道 ⇒ 根因 = 未登记页才可回退原生，
  已修）→ 终版 `gates/T3-105219.log`（2015 例 109F/23E/1S）：**ADDED 0 / REMOVED 3**。
  REMOVED 3 逐条登记：① `QuestBatchReportNpcAlignmentTest.quest11139ReportsAndCompletesAtEndNpc`
  （本片有意收口，重锚后转绿）；②③ `RetailTalkChainGateProbeTest.probeSingleStepGrantRows` 与
  `RetailTalkChainProbeTest.probeChainEquivalence`（探针类已由兄弟车道从树上移除，身份随类消失）。
  类集差：**−4 个探针/临时类**（`ZzProbeDefinitionDumpTest`/两个 ChainProbe/`TempPvpDiagTest`，兄弟车道清理）、
  **+1 `RetailTsvManifestGateTest`（本片 WP4 常设门，3 例全绿）**；存活类用例数零变化
  （2016 − 4 + 3 = 2015 逐项对齐）。
- **防漂移门禁（WP4，本片新增）**：`quest-retail-tsv-manifest.tsv` + `RetailTsvManifestGateTest` 把
  `static_data/quest_retail/` 与 `definitions/quest_dialog/` 的 `*.tsv` 冻结为受控面（磁盘集 == 清单集双向零差集、
  4 列/词表、总数 == `EXPECTED_TSV_COUNT`），已注册进 `affected_quest_tests.py` 的 T1 清单。
  同片完成第一笔退役：`dd-fp-fresh.tsv`——删文件 + 删清单行 + 常量 27→26；退役理由与实测证据
  （全仓 0 消费者、id 集是正式表 966/1217 的真子集、966/966 行指纹逐字陈旧）见
  `2026-09-27-tsv-retirement-candidates.zh-CN.md` §2.4。
- **在册红（不追不修）**：20035（DD 车道登记 `REJECTED:RETAIL_TALK_HUNT_CHAIN_DEFERRED` vs 实际
  `REJECTED:RETAIL_ITEMPLAY_OUTPUT_UNRESOLVED`）与 `PlayerQuestStartEligibilityPortTest` 的 1007 分支，
  两者在基线 T1/T3 中逐字在册。

**WP3 剩余（未动，登记理由）**：`RetailSimpleHuntIrCompiler`（444 行，生产零引用，仅其自身测试消费）
待"shell 方案整体退场"时一并删；`retail-quest-ai-name-groups-rejected.tsv` 的写入方在兄弟车道
（先查作者纪律，跨车道协商后处置）。hunt 的 5 个 legacy 方法按既有裁定**保留**——唯一剩余消费者是
`RetailHandinDialogFlowCompiler:95`（HandinDialogFlow 已裁定不纳入 canonical 化）。

**内存库**：两处扩展——QE-028（"零奖励组不得进档位查表 + `deliveryWindowPage` 兜底"写入判定规则与边界）
与 QE-082（新增规划器侧同源条款："BFS 规划器与契约行走器同口径建模客户端原生窗口、可见性优先、
`no client-reachable path` 是兜底文案"）；`sync_memory_bank.py` → `MEMORY_BANK_SYNC_OK ENTRIES=130`、
`verify_memory_bank.py` → `MEMORY_BANK_VERIFY_OK STEPS=3`。

## Phase 2 尾片已收口之二：SimpleTalk S1 单步面规范形（1804 行，2026-09-27）

选片说明：SimpleTalk 是最大的单次改造面（2089 行 RETAIL_TABLE = **单步 1804（本片 S1）+ 链 285（余下 S2）**）。
S1 = 纯形状迁移 + **一个语义新增点**；链面 S2 需先裁 R 记录过滤策略（A/B/C）。

**落点（`RetailSimpleTalkDefinitionCompiler`，4 处）**：

- 接取段：`acceptFlow` → `canonicalAcceptFlow(npc, "started", acceptGiveItemActions(entry))`——`QUEST_SELECT(31)`
  直发页 4；`SELECT1/SELECT1_1` 页梯与 `ASK_QUEST_ACCEPT(1007)` 中转随页链删除；`build` 的 `exits`
  形参随之移除（`buildChain` 仍用，链面 S2 未动）。
- 交付段：三段旧报告流（空门 `reportFlow` / 工作物品门 `reportFlow` / 整组门 `itemCheckReportFlow`）
  → 单条 `canonicalDelivery(...)` + `deliveryWindowPage(...)`（P0c-10o 的三子形语义保持）；
  `REPORT_PAGE`、两个 `reportFlow`、`itemCheckReportFlow`、旧 `attachMovie`（唯一调用者已迁走）全部删除。
- **过场轴重挂（本片唯一语义新增点，13 行：12×`cs1_haction=1009` + 1×`1007`）**：canonical 形不再有
  1007/1009 路由，旧 `attachMovie` 按 `(npcId, actionId)` 匹配会**静默空转**（丢电影且无门禁能发现）。
  新增 `attachMovieToRoute(transitions, sourceNode, npcId, actionId, movieId)`：匹配键**带 source 节点**
  （判例 4056 接取/交付同体 NPC，`(npc, action)` 键双命中）+ **match 数恰为 1 的 fail-closed**
  （0 = 静默丢失、>1 = 键失效，两者都当场炸）；落点 = "下发对应窗页的那条 `QUEST_SELECT` 边"，
  PlayMovie 插在开窗之前（after 序仍是 Sync → PlayMovie → 开窗）。
- 保留：`acceptGiveItemActions`（接取发物）、交互物门（P0c-22）、`reportNpcExit`、legacy heal 边、
  `completeFlow`；`acceptFlow`/`acceptContinuation` 静态方法保留（`acceptFlowChain`/`buildChain` 在用，S2 收口后删）。

**门禁同步（`RetailSimpleTalkGateTest`）**：`hasSelect1Continuation` 删；`hasReport` → `hasCanonicalDelivery`
（三子形 + 档位窗 + 旧页链零残留）；**新增逐行过场不变量**（受理且声明过场轴的行：定义内 `PlayMovie`
恰 1 个且 `movieId == 真端 cutsceneid1`——**这是本片唯一能拦住过场静默丢失的门**，T2 面零引用过场 id）；
顺手修一处判据过宽：`priorityRoute` 只查交付段（完成流 `reward→complete` 的 class×slot 优先级是正常形状，
判例 1690/2657/80290 等 10 行误报）。家族门 3 → **4 例全绿（accepted=2149）**。

**级联收口**：本片**零生成物 churn**——链面未动 ⇒ 链指纹表不动；单步面无指纹冻结器 ⇒ 形状保护由
重写的家族门逐行断言 + T2/T3 承担（见"遗留"）。契约门（SimpleTalk #1118 全生命周期）绿；清单门绿。

**T2 分拣（1804 单步 id，`gates/T2-125926.log` → 终版 `gates/T2-132715.log`）**：首跑 823 例 55F/18E，
**新增 39 方法 / 31 类** ⇒ 三路 fanout 重锚（`sortS1a/b/c`，17+6+7 类）⇒ 终版 823 例 27F/10E，
**新增失败 0**。

**验证**：

- T3（仓库外隔离 clean 副本）：`gates/T3-132734.log`（2016 例 109F/23E/1S）——
  **ADDED 0 / REMOVED 0**（对 DD 尾片收口基线 `T3-105219` 完全同态；类集 482→482 不变，
  唯一计数差 = 家族门 +1 例过场不变量）。
- 23 个 T1 门类在终版 T2 中全部跑到，唯一红 = 20035 车道红（与基线逐字同身份）。

**选择器盲区（本片实锤，新增纪律条目）**：`QuestNpcFactionRetailGateTest`
（`model/gameobjects/player/npcFaction/`）3 例在 T2 中报"新红"，根因 = 它自建 catalog 读的是**已退役的
XML 目录**（`src/main/resources/aion/data/static_data/quest_definition`，35007/39601.xml 早已删除）而非
生产视图 ⇒ 与 S1 无关的陈债；它至今才暴露，是因为 T3 选择器 `-Dtest=com.aionemu.gameserver.questEngine.**`
**不覆盖 `model/**`**。⇒ 凡断言"任务定义"的测试类都必须在 T2/T3 口径内（该类修法：改用
`ProductionQuestDefinitions.catalog()`；归属 npcFaction 车道，本片只登记不代修）。

**收口裁定（agent 回报中的待裁项）**：

- **1478 的 `SELECT1_1` 出口登记（`quest_client_dialog_exits.tsv:287`）** 判为**既定代价，不补 1012 兜底**：
  canonical 形直接下发页 4，客户端永不显示带 `HACTION_SELECT1_1` 按钮的 select1 页 ⇒ 无实际死按钮；
  该行归入 WP6 盘点的"`quest_client_dialog_exits.tsv` 部分保留"面，S2 收口时一并核对
  （**单步面对 SELECT1_1/SELECT1_1_1 的消费点自此归零**，只剩链面 `acceptFlowChain`）。
- **方法名历史误称**（`simpleSelect5QuestsCheckItemsOnClientAction39` 现断言"无 39"、
  `quest18509/28509AcceptAndEmptyReportDialogs...` 现断言"无空报告页"）：**保留原名只改注释**——
  三个方法名被 3 份 `.agents/summary` 报告**现役引用**，改名会制造引用漂移。**反例（命名口径的另一半）**：
  `deliveryOnlyGrowthQuestsUseTheClientSelect5Page` → `...UseTheCanonicalDeliveryWindow` 的**更名保留**
  ——名字本身编码了已退场页面（S1 规格要求"并更名"），且旧名只出现在 `gates/*.log` 历史跑测日志
  （当时事实的快照，不随之漂移）、**0 份现役文档引用**。判据：**名字是否被现役文档引用**（引用→保留名、
  改注释；仅历史日志→可更名）。
- 其余上报项均为 **agent 自身迭代缺陷且已自修**，无生产侧问题：1351 是断言漏 `targetNode` 子句
  （canonical 交付边本身即"同 NPC + started 源 + `QUEST_SELECT`"，已抽共享负控消除重复）；
  2237 是 `actionId` 抄写错误（39 ↔ 20002，2237 为 XML_RETENTION，其 31 边是 `NPC_REPORT` 报告页路由，
  按 canonical 断言会假绿 ⇒ **不采纳**"改按 canonical 断言"的建议）。
- **已知缺口（登记不修）**：家族门未断言"PlayMovie 在开窗动作之前"的**顺序**——该顺序由
  `attachMovieToRoute` 的插入点构造保证（插在 `Show*`/`CloseDialog` 之前），但无可观测断言；
  如需补属后续小片（本片 T2/T3 证据已定稿，不再改动以保持证据与树一致）。

**遗留**：① 单步面 IR 指纹冻结器（勘察 §4.1 建议）本片未建——如需补，通道形态与链面同
（`-Dretail.talk.fpOut`）；② S2 链面（285 行）待 R 记录过滤策略裁定，且**链面另有 4 行过场**
（1422/2421/3006/3020 的 `MOVIE:` token 藏在 R 记录 after 列）必须一并迁移并加"电影数守恒"断言。

## Phase 2 尾片已收口之三：SimpleTalk S2 链式双块行规范段（203 行，2026-09-27）

- **切片边界**：链式 285 行中**同时具备 `NPC_START` 与 `NPC_REPORT` 块（非系统发放）的 203 行（G1）**
  两个规范段整体接管（接取 `canonicalAcceptFlow`、交付 `canonicalDelivery`）；**纯 R 驱动行（G2 80 + G3 2 = 82 行）
  保持登记表逐字形状**，留作 **γ 面**独立切片（需为 R 驱动段合成规范边；其交付门为 `VAR_IS:var0=k` 阶段门、
  含 2953 重复接取与 28809 跨节点关窗出口等前期裁定项）。**该切分与底稿 §2 的"落点清单"不同**：
  底稿按"有 START 块 266 行 / 有 REPORT 块 205 行"分别起算，本片按**双块交集**起算以保证
  "变化面 == 缺陷面"可逐行验证（实测成立，见下）。
- **策略 A（加载期过滤，唯一不动兄弟车道输入的选项）**：作用域 = 规范段 NPC；接取词表
  {ASK_QUEST_ACCEPT, SELECT1, SELECT1_1, SELECT1_1_1, QUEST_ACCEPT_1/SIMPLE, QUEST_REFUSE_*} 在接取 NPC 退场，
  **另加**判例 1914/1915/1916 的 `SETPRO1 unaccepted→started`（与 canonical `20000` 边逐字同形）；
  交付词表 {SELECT_QUEST_REWARD, CHECK_USER_HAS_QUEST_ITEM(_SIMPLE), SELECT5, SELECT6} 与**退场页下发**
  {SELECT1, SELECT1_1, SELECT1_1_1, SELECT5, SELECT6} 在交付 NPC 退场；中段记录逐字保留。
  **过滤面 388 条**；过滤后 G1 行 SELECT5/SELECT6 页下发 **0**、`started` 源交付中转 **0**、
  selectionSources 越界 **0**（三者均为编译期 fail-closed 守卫）。
- **fail-closed 守卫 4 条**（违者拒绝编译）：载荷白名单（conditions 只允许 `HAS_ITEM:`/`START_ELIGIBLE`、
  actions 只允许 `REMOVE_ITEM:`、after 只允许 DIALOG/SYNC/MOVIE；实测词表 13/13/1 极窄）+
  退场载荷必须被规范交付门覆盖 + 过场 token 必须可重挂（触发轴 ∈ {1007,1009} 且 movieId == 真端 `cutsceneid1`）+
  零残留。
- **过场 4 行（链式侧）**：3020（cs1_haction=1007）**重挂**到 `(unaccepted, 798143, 31)`（同 S1 判例 4056）；
  1422/2421（1353）/3006（1694）的承载记录在**中段 NPC**、其简报页链未退场 ⇒ **落点不动**。
  （`s2-movie` 曾推定四条承载边全退场、`s2-rconf` 普查显示均不在键冲突面；原始记录复核后裁定后者，
  仅 3020 需要迁移。）
- **变化面 == 缺陷面**：链门指纹重冻对拍 **变化 203 / 新增 0 / 消失 0，变化集逐行等于 G1**；
  G2/G3 的 82 行指纹**逐字未动**（本轮三次重算：初版 / 载荷守卫修订后 / `SETPRO1` 判例追加后，均为 203/0/0）。
- **门禁**：`RetailSimpleTalkChainGateTest` 新增两条 S2 不变量
  （`canonicalSegmentRowsCarryCanonicalShapesAndZeroPageChainResidue` 覆盖下限 200、独立重算三形门源；
  `chainCutsceneRowsCarryExactlyOneRetailMovie` 下限 4、触发轴决定落点）→ **4/4 绿**；
  家族门 **4/4 绿**（单步面不变量不变，链式过场注释改指链门）。
- **T2**（链选择器，285 id）：基线 59 红（加载口径 + 形状期望两类）→ 改后首轮 **ADDED 26 / REMOVED 0**
  （落 16 类，全部是预期形状冲突：1011→页 4、2375/1009→交付边 + 窗 5、1909/11008 中转退场、
  1913 点 31 即翻 REWARD）→ 分拣波（3 代理，26 条全部重锚、零 HOLD）→ 终版
  **ADDED 0 / REMOVED 0**（`T2-144601.log`，581 测试 / 35F+16E 与基线逐项相同，终版红集留痕）。
  分拣中两处代理自主裁定：`QuestDispatchToVerteronFamilyProductionFlowTest` 的 6 行不同形
  （19070/19071 是 **XML_RETENTION**，保留 legacy 形）；1963 重复开局由
  `QuestDefinitionCompiler.restoreRepeatStartDialogContract` 全局承接（未触设计边界）。
  收口时抓到一条**共因缺陷**并修正：`Batch40ThreeNpcTalkLadderContractTest` 对 **21081（XML_RETENTION）**
  断言了 canonical 形，但该行由保留 XML 供货（legacy 形）——判据 = **retention owner**，已按
  `retailOwned` 分流；临时探针 `Temp21081ProbeTest` 用完即删。
- **纪律**：未 commit/push；未启停服务；未新增/退役 TSV；`carriedWorkItems` 改用**未过滤**回放集
  （门来源不被过滤窄化，判例 1971：中段 `remove_item1` 消耗后 carried=0 ⇒ 空门是正确形状）。
- **产物**：`2026-09-27-s2-canonical-segments.zh-CN.md`（收口记录）+ 侦察三件套
  （`2026-09-27-s2-t2-triage.zh-CN.md`、`2026-09-27-s2-r-record-conflicts.zh-CN.md`、
  `2026-09-27-s2-movie-migration.zh-CN.md`、`2026-09-27-s2-buildchain-spec.zh-CN.md` + 各自 TSV）。

## Phase 2 尾片已收口之四：SimpleTalk S3a 仅接取段接管（60 行，2026-09-27）

- **切片边界**：γ 面 82 行中**有 `NPC_START` 块、无 `NPC_REPORT` 块、非系统发放、且接取块
  `selectionSources` 段内**的 **60 行**只接管接取段；交付段（纯 R 驱动的 SELECT5/1009 页链）**一字未动**。
  段旗标从 S2 的整行 `canonicalSegments` 拆成 `canonicalAccept` / `canonicalDelivery` 两个独立旗标。
- **判例 28809 派生式延期**：接取 NPC == 交付 NPC（830169），登记表把交付态泄进接取块的
  `selectionSources`（`unaccepted s0 s1 s2`）⇒ 现在接管会把**在服务页**（SELECT3/SELECT5）上的关窗按钮
  变成死端（致命 `BUTTON_WITHOUT_ROUTE`）⇒ 谓词 `acceptSourcesWithinSegment`（与守卫同源、非硬编码 id）
  判其延期、逐字保留；链门 `ACCEPT_DEFERRED_ROWS = {28809}` 逐行锁定，全 285 行 START 块普查越界者**恰此 1 行**。
- **守卫按段分治**：新增 `assertCanonicalAcceptResidueFree`（接取侧：块 NPC 上不得留 select1 族页下发与
  1007/1011/1012/1013 动作）；交付侧守卫仍只在 `canonicalDelivery` 时运行——**不把交付段页判据加进接取守卫**
  （S3a 行的交付段本就该保留那些页）。
- **变化面 == 缺陷面**：指纹重冻 **60/0/0**，逐行等于切片行集；S2 的 G1 203 行零漂移、28809 逐字未动。
- **门禁**：链门 **5/5 绿**（新增 `acceptOnlyRowsCarryCanonicalAcceptAndKeepTheirDeliverySegment`：
  60 行接取窗边 + 1002/20000 两形提交 + 接取侧零残留 + **交付段逐字保留对照断言** + 延期集锁定，
  下限 `CANONICAL_ACCEPT_ROW_FLOOR=55`）；家族门 **4/4 绿**。
- **T2**（链选择器，285 id）：首轮 **ADDED 2 / REMOVED 0**（两条恰为切片行的"接取入口页"硬断言：
  `Quest1152RetailAlignmentTest`、`Quest1118ProductionFlowTest`）→ 2 代理分拣重锚（只换锁旧形断言、
  按 retention owner 判 canonical、未删未放宽）→ 聚焦 2/2 绿 → 终版 **ADDED 0 / REMOVED 0**（51 == 51）。
- **T3**（仓库外全树副本）：分拣后终版对基线 **ADDED 0 / REMOVED 0**（见收口记录 §6）。
- **覆盖面提示**：60 行里 **10 行零测试类引用**（`1394 2553 2963 3218 4209 4218 4970 30711 30761 80752`）
  ⇒ T2 只证明 50 行；这 10 行由指纹表 + 链门两个硬数字背书，**不得表述为"T2 证明了全部 60 行"**。
- **产物**：`2026-09-27-s3a-accept-segment.zh-CN.md`（收口记录）+ 预分拣 `2026-09-27-s3a-t2-prep.zh-CN.md`
  + 分拣两件套 `2026-09-27-s3a-sort-1152/1118.zh-CN.md` + γ 面裁定简报
  `2026-09-27-s3-adjudication-brief.zh-CN.md`（S3b/S3c 用）。
- **记忆库**：新增 **QE-088**（按段接管：段旗标解耦 / 守卫分治 / 段外载荷派生式延期 / 对照断言 / 零测试引用行）。

## Phase 2 尾片已收口之五：SimpleTalk 守卫加固片 G-1..G-4（零形状变化，2026-09-27）

- **性质**：不是形状迁移，而是把裁定简报/s3-special 发现的**四条 fail-open 守卫缺口**补成编译期拒绝，
  为 S3b/S3c 的 R 驱动合成铺路（两代理一致建议"守卫先于形状"）。
- **四轴**：G-1 退场载荷**动作侧**（`REMOVE_ITEM`）与条件侧同轴收集（否则扣物静默消失）；
  G-2 交付中转判据从字面 `"started"` 改为**源节点投影 START**（REWARD 态同名路由是 QE-083 重开预览，必须留；
  判例 24202 的 I 记录腿在 `s2` ⇒ 旧判据整条空过）；G-3 canonicalDelivery 行必须有 **reward 态奖励窗重开载体**
  （否则领奖后关窗死档）；G-4 退场的 `VAR_IS`/`VAR_AT_LEAST` 阶段门必须被**规范边源节点投影蕴含**
  （否则领奖提前且 `QuestPrematureRewardRouteAudit` 因动作 id 不同源看不见）。
- **零形状变化证明**：指纹重冻 285 行 **changed 0 / added 0 / removed 0**、编译 **problems=[]**；
  改码前四轴现存命中普查各为 **0**（G-2 精确化后风险集 0；G-3 缺预览载体 0 行；
  G-1 "有扣物无条件门"退场记录 0 条；G-4 退场记录带 `VAR_*` 0 条）。
- **门禁**：链门 **6/6 绿**（新增 `hardenedGuardsHoldAcrossCanonicalRows`：四轴测试侧异路径重算 +
  双下限）、家族门 **4/4 绿**；**T2 ADDED 0 / REMOVED 0**（595 测试 / 35F+16E）、**T3 ADDED 0 / REMOVED 0**
  （2020 测试 / 109F+23E，隔离副本跑完即删）。
- **诚实边界**：这是**潜在**缺口加固而非现存缺陷修复——G-1/G-4 的落地判例由后续切片提供
  （1323 的 `REMOVE_ITEM`、35010 系列的 `VAR_IS:var0=1 @ started`）。
- **产物**：`2026-09-27-s3-guard-hardening.zh-CN.md`；记忆库 **QE-089**（守卫四轴守恒）。

## Phase 2 尾片已收口之六：SimpleTalk S3b R 驱动接取段合成（7 行，2026-09-27）

- **切片**：γ 面 8 行候选里**有 `QUEST_SELECT` 入口记录且非物件入口**的 7 行（`2611 3001 3023 21136
  24202 80320` + 裁定放行的 `28809`）接取段换规范形；**交付段未动**（`canonicalDelivery` 显式排除 R 驱动形）。
- **层 A（键覆盖过滤）**：合成的规范边会被编译器末尾的 `explicitRoutes` 用**同键**登记记录反向删除
  （R 驱动行带 `(unaccepted, npc, 31)` 入口页下发记录）⇒ 同键记录退场列为独立子句。实测同键冲突面
  **只在 R 驱动行**（S2 203 + S3a 60 共 0 条）。
- **R 驱动合成**：`rDrivenAccept` 派生谓词（非系统发放 ∧ 无 START 块 ∧ `acquired` 唯一解析 ∧ 非交互物
  ∧ 有 `QUEST_SELECT` 入口记录 ∧ 有 `QUEST_ACCEPT_*` 提交记录）⇒ `canonicalAcceptFlow(acquiredNpc,
  "started", 提交记录上的动作)`；新增**接取侧载荷覆盖守卫** `assertAcceptPayloadCovered`（动作侧
  `GIVE_ITEM`/`REMOVE_ITEM` 必须被规范接取边覆盖，判例 21136 的 `GIVE_ITEM:182207919:1`）。
- **28809 裁定放行**：段外 selectionSources 仅当**该源上登记表无 `FINISH_DIALOG` 记录**时放行
  （派生判据 `servesFinishDialog`，同时驱动延期谓词与守卫）；客户端实测其全页动作集无 `1008`
  ⇒ 旧形 4 条关窗出口本就无载体。链门 `ACCEPT_DEFERRED_ROWS` 清空为 `Set.of()`。
- **1323 延期**：入口是 `USE_OBJECT` 宝箱自环 ⇒ 由入口通道判据排除、逐字保留（留 `USE_OBJECT` 变体）。
- **开发期修正（留痕）**：接取侧载荷守卫首版**跨侧判定**，把交付侧检查对（`CHECK_USER_HAS_QUEST_ITEM`
  的 `REMOVE_ITEM`）算进接取侧 ⇒ 误伤 11 行；修为按退场侧分流（`retiredAcceptRoutes`）。
- **判据**：指纹 **7/0/0** 逐行 == 切片（S2 203 与 S3a 60 零漂移、1323 未动）；链门 6/6、家族门 4/4；
  T2/T3 见收口记录。
- **产物**：`2026-09-27-s3b-r-driven-accept.zh-CN.md`；记忆库 **QE-090**（层 A + R 驱动合成 + 载荷分侧）。

## Phase 2 尾片已收口之七：SimpleTalk S3c-A 交付段同构替换（31 行，2026-09-27）

- **切片**：交付段接管与接取段**解耦**。31 行 = 计划表 30 行（A1 12 / A2 9 / A3 2 / A4 2 + 未列 5 行
  `2553 2646 3218 3966 4218`）∪ `{80320}`（与 24202 逐轴同形：`SET_SUCCEED` 翻面 + `I` 记录门 + S3b
  接取形；独立事实工作流标注"不属本片"但未给结构差异 ⇒ 同形同办，防同形漏网）。
- **谓词（三类形，全派生）**：①块路径 `canonicalAccept ∧ 报告块`（S2 原谓词不变）；②块路径
  `systemGrant ∧ 报告块`（系统发放无接取段可接管；实测报告块行 205 行中无 START 块的仅 2611/35017
  ⇒ 新增面恰 1 行）；③**A 形翻面**（源投影 START ∧ 落 `reward` ∧ 交付作用域 ∧ 动作 ∈
  `SELECT_QUEST_REWARD`/`SET_SUCCEED`/`SETPRO1..3` ∧ **无竞争翻面**）。
- **交付作用域**：真端 `reward_npc_name` 解析集，**空集回落客户端交付 NPC 登记**（判例 35024/45024
  的真端名在 npc 注册表不存在；80752 为成员关系而非单元素集）。
- **载荷逐字承接（同构替换只换页面骨架）**：条件侧 `HAS_ITEM`/`VAR_IS` 与动作侧 `REMOVE_ITEM`/`SET_VAR`
  一并由规范交付边承接（`carriedDeliveryGates`/`carriedDeliveryActions`，G-4「被承担」分支 =
  token 逐字相等才放行）；`I` 记录与退场 `R` 记录同轴收集进门，接管行跳过 `itemReportGate` 展开
  （判例 24202：唯一门源 = I 记录，与 quest.xml `collect_item1` 同物）。
- **层 A 交付对偶**：规范交付边占 `(源, 交付 NPC, 31)` 键 ⇒ 同键登记记录必须退场，否则 `explicitRoutes`
  覆盖**反向删除**合成边（判例 80752 的 `SELECT2` 页记录，旧形静默回退而指纹零漂移）。
- **边界裁定**：D 类 5 行（4209/21033/21455/30711/30761 有竞争翻面）留 W2 (iii)；1152 结构排除
  （翻面落 `pepper`）；1323 交付段已接管、接取段留 W3 物件变体；35017 走新增的系统发放块路径。
- **判据**：指纹 **31/0/0** 逐行 == 切片（S2 203 / S3a 60 / S3b 7 零漂移）；链门 **7/7**（新增 S3c
  不变量：恰一条键边 + 档位奖励窗 + 载荷逐字承接 + 零报告页 + reward 态重开载体 + D 类对照组 +
  系统发放块路径成臂）、家族门 **4/4**；**T2 ADDED 0 / REMOVED 0**（596 测试 / 35F+16E 与基线逐项同，
  红身份集 sha256 `57bb0621…` 逐字节同）、**T3 ADDED 0 / REMOVED 0**（2021 测试 / 109F+23E 同，
  sha256 `ce4673c7…` 同）；首轮 2 条新增红（1118 生产流 + EarlyElyos 药膏行）按规范形重锚后归零。
- **开发期修正（留痕）**：谓词先窄后宽三次（单元素集 → 成员关系 → 客户端回落）；一次 stale 读数假象
  （冻结产物只在断言通过后写出，编译失败时停留旧值 ⇒ 必须核对 mtime/本次是否重写）。
- **产物**：`2026-09-27-s3c-a-delivery-segment.zh-CN.md`；记忆库 **QE-091**。

## Phase 2 尾片已收口之八：SimpleTalk S3c-D 报告页退场（39 行）与「就地加窗」否决（2026-09-27）

- **交付**：D 类 50 行（有 reward 入边翻面记录且无 `NPC_REPORT` 块的 RETAIL_TABLE 行）里，交付 NPC 侧
  下发报告页（`SELECT5`/`SELECT6`）的记录按 **R-REP 页下发判据**退场（实测 **39 条 = 39 行**、载荷全空）；
  指纹变化面 **39/0/0** 逐行 == 该行集（W1 31 行 / S2 203 / S3a 60 / S3b 7 零漂移）。
- **新增守卫**：`assertReportPageRetirementQuiet`（无规范承接边的行若退场对象带物品/阶段令牌即拒绝编译）
  + 测试侧 **G-3 对偶**（D 类必须保留 reward 态奖励窗重开载体）+ 中间人翻面 after **逐字保留**断言。
- **（iii）「中间人翻面就地加窗」被否决（证据）**：e2e 契约门实测 `3100`
  `native reward window has no completion route`——领奖选择动作按当前对话 owner 路由，而中间人 owner
  无完成腿 ⇒ 加窗会造**可见死按钮**（致命类 `BUTTON_WITHOUT_ROUTE`）⇒ 退回独立裁定（镜像领奖腿 /
  落交付 NPC / 维持现状），本片只做可证安全的 R-REP。
- **判定**：链门 **7/7**、家族门 **4/4**；**T2/T3 ADDED 0 / REMOVED 0**（595/2021 测试与基线逐项同，
  红身份集 sha256 `57bb0621…` / `ce4673c7…` 与 W1 终版逐字节相同）；首轮 5 条新增红经回滚 + 断言重锚归零
  （1163/3100/19004/Batch31 的奖励页形状断言按 R-REP 重锚）。
- **登记零变化**：`21455`（无报告页可退场）、`39003/49003`（块路径行）、其余无报告页的 D 行。
- **产物**：`2026-09-27-s3c-d-report-page-retirement.zh-CN.md`；记忆库 **QE-092**。

## Phase 2 尾片已收口之九：SimpleTalk S3c-obj 物件接取变体（1 行，2026-09-27）

- **交付**：物件哨兵接取者（判例 `1323`，宝箱 `730032`）的接取段按**物件梯**由编译器合成——入口
  `USE_OBJECT` 自环下发入口页 `select1`、中转 `ASK_QUEST_ACCEPT(1007)` 下发接取窗（页 4）、提交
  `QUEST_ACCEPT_1` 落 `started`、拒绝族下发拒绝页、关窗出口 `CLOSE`；真端 `give_item`
  （`ITEM_QUEST_1323A 1` → `182201309`，与 `quest_data.quest_work_items` 互证）从**可重复的入口自环**
  移到**提交边**（原形反复用物件即叠加任务物品、拒接后残留）。指纹变化面 **1/0/0** 逐行 == 该行集；
  S2 203 / S3a 60 / S3b 7 / S3c-A 31 / S3c-D 39 零漂移。
- **裁定（含被否方案）**：页梯**保留**——否决"塌缩成 `USE_OBJECT → 页 4`"：物件的对话窗口由服务端下发
  （`QuestStartItemNpcAi2` 无任务路由时回落 `SELECT1`），客户端为该物件 authored 的入口按钮就是
  `1007`，塌缩会把入口页 1011 重新打成 `CLIENT_PAGE_UNREACHED`，与 W3 判据"入口页/接取窗/拒绝页
  三页有路由"相悖（NPC 形的 select1 未达是可替代的取舍，物件没有可替代入口）。
- **新增守卫**：`assertObjectAcceptContract`（合成梯键集与该物件登记对话面**逐键相等** + 入口页/中转页
  常量合同；多一条 = 发明客户端不会发的按钮、少一条 = 静默丢出口 ⇒ 拒绝编译）+ 层 A 对偶（入口 `-1` 与
  中转 `1007` 键随段退场，否则合成边被 `explicitRoutes` 反向删除）+ 测试侧 S3c-obj 臂（三页各**恰一条**
  下发 / 入口零载荷 / 提交边载荷 == 真端 `give_item` / 提交边 `StartEligible` / 不得并立 NPC 形入口 31）。
- **判定**：链门 **8/8**、家族门 **4/4**；**T2/T3 ADDED 0 / REMOVED 0**（597 / 2022 测试；红身份集 sha256
  `57bb0621…` / `ce4673c7…` 与 W1/W2 基线逐字节相同）；守卫自检三负例（合同页变更 / 键漂移 / 谓词收缩）
  在仓库外副本内全部被拦（拒绝编译或覆盖面下限红）；聚焦测试里的三条红（1197/1131/4966）**均为基线在册红**。
- **产物**：`2026-09-27-s3c-obj-object-accept.zh-CN.md` + 普查脚本 `w3_object_entry_census.py`。

## W5-g1 已收口：页码类 TSV 退役第一张 `quest_client_report_pages.tsv`（2026-09-27）

- **死分支证明**：表的唯一值读取点在 `canonical=false` 死分支（`RetailSimpleHuntDefinitionCompiler:771`），
  而 `canonical=false` 的两个公有重载**零调用者**（全仓 3 处 `compile(` 调用全是 7 参 canonical 形）。
- **整体退场**：删读取者类 `RetailClientReportPages` + 2 个死重载 + 4 个死页链流（`reportFlow`/
  `incompleteReportRoutes`/`briefingFlow`/`acceptContinuation`；**`acceptFlow` 保留**——HandinDialogFlow
  三票否决排除迁移，是唯一剩余调用方）+ 全部参数穿线（3 生产文件 + 1 驱动 + 3 测试夹具）
  + 删表（5995 行）+ 删清单行 + `EXPECTED_TSV_COUNT` **26 → 25**。
- **判定**：清单门 3/3、SimpleHunt 家族门 1/1、等价门 2/2、SimpleTalk 链门 8/8 与家族门 4/4 绿；
  **T1/T2/T3 ADDED 0 / REMOVED 0** 且红身份集 sha256（`3b92439da8…` / `57bb0621…` / `ce4673c7…`）
  与基线逐字节相同 ⇒ **零 IR 变化**。
- **生成器移交**：`build_quest_client_report_pages.py` 属 `scriptdll-quest-driver` 车道（本车道只读）；
  "停写"登记为 **W6 移交项**；若被重跑，`RetailTsvManifestGateTest` 会立刻拦红（fail-closed 兜底）。
- **事故留痕**：批量删除脚本的启发式锚定误吃类头一段（未跟踪文件、无 git 基线），已从**会话历史里的
  原始文件读取记录**逐字重建；教训写入 **ENV-004 第 3 条**。
- **产物**：`2026-09-27-w5g1-report-pages-retirement.zh-CN.md`；记忆库 **QE-094** + ENV-004 扩充。

## W5-g2 已收口：页码类 TSV 退役第二张 `quest_client_entry_pages.tsv`（2026-09-27）

- **死分支证明**：三处读取**全部零效果**——两个 DD 链编译器把 `entryPage` 接成形参后**从不读**
  （`RetailDataDrivenTalkCollectChainCompiler.build` 形参在方法体内零出现），DD 定义编译器读出的局部
  在 W5-g1 删掉 `acceptEntryPage` 后**已无处可传**。
- **整体退场**：删读取者类 + 全部参数穿线（3 生产文件 + 驱动 + 1 测试夹具）+ 删表（2449 行）+ 删清单行
  + `EXPECTED_TSV_COUNT` **25 → 24**；与 g1 同型 ⇒ **无形状裁定**。
- **判定**：清单门 3/3、DD 门 5/6（唯一红 = 在册 20035 车道）、等价门 2/2、SimpleTalk 门 8/8+4/4 绿；
  **T1/T2/T3 ADDED 0 / REMOVED 0** 且红集 sha256（`3b92439da8…`/`57bb0621…`/`ce4673c7…`）逐字节恒等。
- **新增纪律**：`static_data/quest_retail/` **未纳入 git**（`git ls-files` = 0）⇒ 退役前先落快照
  （本片起落 `.agents/summary/quest-native-dispatch/retired-tsv/…retired-<日期>` 并记 sha256）。
- **产物**：`2026-09-27-w5g2-entry-pages-retirement.zh-CN.md`；QE-094 复核通过（两例可作纯机械退役模板）。

## W6-a 已收口：清理批（shell 编译器退役 + 命名反义 + 登记补注，2026-09-27）

- **W6-①**：迁移期 shell 编译器 `RetailSimpleHuntIrCompiler`（444 行）+ 其测试（556 行）**零生产引用** ⇒
  成对删除；`RetailSimpleHuntDefinitionCompiler` 两处 javadoc 提及改写；**3 个测试资源共享面须保留**
  （其中 `retail-simple-hunt-ir-fingerprints.tsv` 是等价门的 shell 基线）。
- **W6-③**：`RetailSimpleTalkMigrationReviewContractTest` 的**名实相反**方法改名
  （`...KeepTheFailurePage` → `...RetireTheFailurePage`；断言本就是"退场"）；**"一名指两页"
  （SELECT6=2716 vs CHECK_USER_ITEM_FAIL=10001）仍待裁定**，跨文件重命名不在本片。
- **W6-④**：`retail-quest-ai-name-groups-rejected.tsv` 表头与清单 note 补**生成器路径 + 车道 + 零消费者**；
  生成器停写仍是兄弟车道移交项。
- **W6-②/⑥（核实，未动作）**：EarlyElyos 3 条在册红**成立**（属形状工作，须单独裁定）；report_pages
  生成器**仍会写回**已退役路径（`build_quest_client_report_pages.py`，兄弟车道；重跑会被清单门拦红）。
- **判定**：聚焦 22/22 绿；**T1/T2/T3 ADDED 0 / REMOVED 0** 且红集 sha256（`3b92439da8…`/`57bb0621…`/
  `ce4673c7…`）逐字节恒等；**T3 测试数 2022 → 2016**（−6 = 被删类全绿）——零行为变化。
- **产物**：`2026-09-27-w6a-cleanup-batch.zh-CN.md` + `2026-09-27-w5c-w6-recon-pack.zh-CN.md`（W5 续片/W6 取证包）。

## W5-g3 已收口：页码类 TSV 退役第三张 `quest_client_talk_pages.tsv`（2026-09-27，形状片 A）

- **裁定（用户）**：**A 换判据 + 退役**（否决 B 保留陈旧守卫 / C 删门不补判据）。
- **副本实验（真解锁集实测）**：删门双跑 dump 对比 ⇒ **ADDED 0 / REMOVED 0**、分类变化**恰 6 行**
  全 `ADOPTED`（`25200 80814 80823 80824 80830 80833`）——取证代理"大概率拒绝码漂移"的推断被**实验否决**。
- **换判据**：无进度行必须有**客户端任务书行**（`clientSummaryRows.rows(id) > 0`，否则
  `RETAIL_TALK_JOURNAL_MISSING`）；非回归实测 `登记 ∩ noProgress = 181，缺任务书 = 0`；
  fail-open 点 = `lastRowIndex` 的 `Math.max(0, rows-1)` **缺行静默返 0**。
- **净效果**：**25200 受理**（客户端 HTML = DD 模板 + 页 4 authored + 任务书 1 行）+ **5 行换码仍拒**
  （客户端无 HTML、无任务书）；死值清除（`build×2`/`buildItemAcquire`/`assemble` 页参移除 + javadoc 重写）。
- **三件套**：受理 flip = retention owner + **遗留 XML 退役**（`quest_definition/quests/25200.xml`，git blob
  `7b1aebbe…`）+ catalog 条目（1265 → 1264）——缺 XML 一步会让 `verifyProductionCoverage` 级联 15 错。
- **判定**：清单门 3/3、黑盒契约门 1/1、DD 门唯一红 = foreign 在册红；**T1 ADDED 0 / REMOVED 0**
  （86 测试；红集 sha256 `3b92439da8…` 恒等）；T2/T3 合并收口跑。
- **纪律**：drift 重冻**只写本片 9 行**（foreign 44 行回置，保在册红）；retention 三份定向手工改
  （生成器重跑会回退 472 行 ⇒ 还原 + 定向改，见 ENV-004 规则 5）。
- **产物**：`2026-09-27-w5g3-talk-pages-retirement.zh-CN.md` + `w5g3-experiment/`（双跑 dump）+ **QE-095**。

## W5-g4 已收口：页码类 TSV 退役第四张 `quest_client_briefing_chains.tsv`（2026-09-27，零形状片）

- **裁定（用户）**：**a 退役 + 构建期校验**（否决 b 直接删门 / c 保持表与门）。
- **零拒绝实测**：47 行真端 `talk_npc1`（宇宙内 23 = 21 `ADOPT_RETAIL` + 2 spawn `KEEP_XML`；差集 24 行
  `minlevel_permitted=999` 砍内容/测试行**不进管线**）；**45×SETPRO1 + 2×SETPRO2** 终点；
  六族 389/389 交叉覆盖；retention 全表零 `RETAIL_BRIEFING_*` ⇒ 退役**零受理变化**。
- **不变量迁构建期**：冻结快照 `src/test/resources/quest/retail-client-briefing-chains.tsv`（sha256
  `35c38ad5…`）+ 新常设门 `RetailBriefingChainEvidenceGateTest`（登记 **3225** 冻结 + SimpleHunt **47** /
  CollectItem **5** 基数冻结 + 每行末跳 SETPRO 终点断言）+ 注册进 T1。
- **处置**：hunt `requireBriefing` 删两段表判据（保留 `TALK_NPC_UNRESOLVED`/`AMBIGUOUS`/`SLOT_CONFLICT`）+
  `isFlagClearingAction` 退场；collect 删 `RETAIL_BRIEFING_CHAIN_UNREGISTERED` + `BriefingStep.chain`；
  DD/driver 穿线移除；删表（3230 行）+ 删类 + 清单行 + `EXPECTED_TSV_COUNT` **23 → 22** + 4 测试夹具。
- **判定**：聚焦 18 测试仅 foreign 在册红（新门 1/1、清单门 3/3、等价门 2/2、采集族门 6/6）；
  T1 含新门（结果见 W5-g3 同批）；T2/T3 合并收口跑。
- **产物**：`2026-09-27-w5g4-briefing-chains-retirement.zh-CN.md` + **QE-096**。


## 批 0 已收口：R1–R4（2026-09-27，交接包 §1 批 0）

- **R1 `dialog_exits` 行级缩表**：`requires(` 12 处/3 文件静态可达性普查（singleStep 行 `build(` 不带
  exits、被拒行 precheck 先返、DD/采集只读 SELECT_NONE_1、链行体级四 token 禁删、256 行保守保留）⇒
  3938 数据行不变、token 13429→443（删 6493）；**DD 指纹 1217 行 + 链指纹 285 行前后逐字节相同**、
  7 家族门唯一红 = 在册 20035、红身份集与 T1 基线恒等。生成器停写移交（兄弟车道）。
- **R2 D 类加窗裁定 = ③维持现状**（①镜像腿/②窗落交付 NPC 均否决：QE-092 e2e + 客户端 authored
  翻面记录 + 1163/3100/Batch31/ReportToMany 逐行锁定；零形状变化）；余面 = 11 行无报告页的中间人翻面。
- **R3**：EarlyElyos 3 在册红裁定为"锁旧形测试被迁移取代、登记保留"（修复会破红集字节恒等 ⇒ 待裁定
  基线重冻后再重锚）；`FailurePage` 消歧注释落 `RetailSimpleCollectItemDefinitionCompiler.itemReport`
  （SELECT6=2716 ≠ CHECK_USER_ITEM_FAIL=10001）；生成器停写移交 ×2（report_pages + dialog_exits）。
- **R4**：本节 + 台账 `2026-09-27-b0-wrapup.zh-CN.md` + 记忆库 QE-097 + 总量复算（6224 =
  4959 RETAIL_TABLE + 670 计划保留 + **595 SEMANTIC_GAP**，批 0 零受理变化）。
- 门禁：`gates/r1-fp-pre.log` / `gates/r1-fp-post.log`（快照两轮）；无受理 flip ⇒ 无新增待实机复验项。

## 下一面（Phase 2 续片：其余家族规范形滚动）

> 已收口：SimpleHunt / SimpleCollectItem / SimpleSerialHunt / SimpleUseItem / SimpleItemPlay /
> CombineTask / DD 单段网格 / DD 多段顺序链 / DD 四子面（DD 家族清零）/
> **SimpleTalk S1 单步面（1804 行）** / **SimpleTalk S2 链式双块行（203 行，策略 A）** /
> **SimpleTalk S3a 仅接取段（60 行）** / **守卫加固片 G-1..G-4（零形状变化）** / **SimpleTalk S3b R 驱动接取段（7 行）** /
> **SimpleTalk S3c-A 交付段同构替换（31 行）** / **SimpleTalk S3c-D 报告页退场（39 行，含 (iii) 加窗否决）** /
> **SimpleTalk S3c-obj 物件接取变体（1 行，S3b-β 收口）**。
> 剩余 = **D 类加窗独立裁定**（镜像领奖腿 / 窗落交付 NPC / 维持现状——e2e 证据见 S3c-D 台账）
> + **W5 续片**（**已收口 g1/g2/g3/g4**；剩 `dialog_exits` 行级缩表）+ **W6 清理**（**已收口 W6-a**：
> shell 编译器退役 + 命名反义 + 登记补注；剩 EarlyElyos 3 条在册红、`FailurePage` 一名指两页、生成器停写移交）
> + **W7 收口文档**。
> **裁定已备**：`2026-09-27-s3-adjudication-brief.zh-CN.md`（五项裁定 + 切片表 + 风险清单），
> 其中本车道已补第三候选形 **(iii) 就地加窗**（保留中间人 `SETPROn` 键与简报页链、仅追加奖励窗 +
> 报告页/1009 对退场）——实测 50 行里 39 行退场面 = 纯 `SELECT5` 页、11 行交付面已净、14 行翻面带
> `VAR_IS:var0=0` 阶段门（(iii) 下**无需**第三种门形）。**前置 = 守卫加固片**（简报 G-1..G-4 四条 fail-open
> 缺口，零形状变化 + 独立 T2/T3，可先做）。
> HandinDialogFlow 已裁定不纳入（三票否决）。以下规格保留作续片的方法论底稿。

改造 = 编译层规范形（方案 A，保住事务/条件/持久化/审计语义），不在 onDialog 绕过图执行。

规范形定义（以 `RetailSimpleHuntDefinitionCompiler` 为基准，其余家族同构）：
- `acceptFlow` 规范形：`QUEST_SELECT(31) → ShowQuestDialog(4)` 直发；保留
  `1002→建档(+页1003)`、`20000→建档+关窗`、`1003/1004/20001` 拒绝族、`1008→页10`；
  **删** select1/select_none 阶梯、1007 中转、`SELECT1_1` 续页（`acceptContinuation` 整个删）。
- `briefingFlow` 规范形：简报 NPC `QUEST_SELECT → 清 SECTION_5 标志 + LEVEL 同步 + 关窗`
  一步到位（保留 talk_npc1 的"见中间人后才开计数"语义，删 select2 页链）；
  `requireBriefing` 的 `RETAIL_BRIEFING_*` 拒绝门随之删除。
- `reportFlow` 规范形：满段节点 `QUEST_SELECT → REWARD + ShowQuestDialog(rewardWindowForTier)`
  直达（删 1352/2375 报告页与 1009 中转）；`incompleteReportRoutes` 整组删（未满段对话交给
  DialogService 关窗兜底）。`QuestLegacyMonsterHuntProductionFlowTest` 的 DEFAULT_SUCCESS/SELECT2
  锁定断言按"零售侧改、XML 侧留"分拣更新。
- `completeFlow` 不动（已是规范形：8..23 + 108/110.. 自动通道 + 职业路由）。

已知级联（动手前先跑基线，逐项收口）：
1. **共享 flow 函数是全家族复用的 static**（Talk/CollectItem/ItemPlay/CombineTask 编译器同构调用）
   ⇒ 规范形做成新变体函数，SimpleHunt 先切，其余家族逐续片切换，旧函数在最后一个调用点切走后删除。
2. `requireBriefing` 删除会**解锁**一批登记为 `SEMANTIC_GAP:RETAIL_BRIEFING_*` 的 XML_RETENTION 行
   ⇒ `retail-xml-retention.tsv` 翻 owner=RETAIL_TABLE（带 evidence）→
   `RetailSimpleHuntFamilyGateTest` 的登记码断言同步重算（登记表 `retail-simplehunt-compiler-rejects.tsv`）。
3. 页链删除会让 `QuestClientContractGateTest` 生产视图审计产生批量 introduced
   ⇒ 按批准计划把该门对 RETAIL_TABLE 的口径切到"规范生命周期窗审计"（只审页 4 / 分档奖励窗 /
   8..23 段），XML_RETENTION 保留全页审计；基线 `quest-client-contract-baseline.tsv` 重算，
   按 QE-066 同源对拍并记录双侧致命计数。
4. 各 IR 指纹（SimpleHunt 链/等价/网格）重冻，沿用"同态对拍 ADDED 0/REMOVED 0 + 有意漂移逐条登记"。
5. 每步收口线：**`RetailQuestContractTest` 全绿 + T1 零新增失败 + T3 身份集对拍**。

## 缺口批 1+2 已收口：哨兵接取/击杀族（2026-09-28，提交 `06f4da5c8`）

- **SEMANTIC_GAP 595 → 304**（−291 = 24 受理 flip + 267 逐行裁定 ADJUDICATED + 9 行 drift 码对齐）。
- 批 1：挑战哨兵采纳边（`RetailChallengeAcquireAdoptions` 24 条 + `bind()` 管道归一化）+ flip 三件套；
  fail-closed 实录 = 10 行采纳后被拒（MONSTER_UNRESOLVED 击杀别名）门先红回退延期；裁定保留新前缀
  `ADJUDICATED:<码>`（归属门词表 + 家族门前缀不变式中立化）；裁定指纹 344→368。
- 批 2：108 行击杀/狩猎族零行为裁定改名（MONSTER_UNRESOLVED 69 / KILL_COVERAGE_LOSS 35 /
  MULTI_STAGE 4；35 行"可合成但契约弱于 XML"继承门禁必须合成断言）。
- 快筛：家族门 2/2、等价门 2/2（裁定集 +24）、归属门 4/4、ItemPlay 门 1/1 全绿；
  台账 `2026-09-27-b1-acquire-sentinel.zh-CN.md` + `2026-09-27-b2-kill-family.zh-CN.md`；
  实机复验清单 +24 行（`client-recheck-list.zh-CN.md`）。

## 命令与日志

- T1：`QUEST_LOG_DIR=<本目录>/gates .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1`
- 聚焦契约门：`mvn -o -B test -Dtest=RetailQuestContractTest -DfailIfNoTests=false -DforkCount=1`
- 本目录日志：`gates/T1-001512.log`（基线 74/75）、`gates/contract-first-run.log`~`contract-run13.log`
  （契约门 13 轮迭代：运行1=1绿7红 → 运行13=8绿，根因链见 git 历史与本 README"协议级发现"）、
  `gates/T1-012556.log`（收口复跑：**76 例 75 绿**，新增 RetailQuestContractTest 1 例绿；
  唯一红与基线同身份 = 20035 DD 漂移登记，并行车道在飞，**零新增失败**）。
- Phase 2 日志：`gates/phase2-focused-1/2/3.log`（聚焦 7 类 57 例：第一轮 2 红 → 修复 → 全绿）、
  `gates/phase2-contract-strict.log`（审计收窄严格模式：stale=0）、
  `gates/T1-022247.log`（Phase 2 T1：76 例唯一红=在册 20035 同身份，零新增）、
  `gates/T2-022851.log`（T2 1112 30715：128 例同唯一红）、
  `gates/T3-023514.log`（Phase 2 首轮 T3：ADDED 18 锁旧形清单）→ `gates/phase2-t3fixes-1/2.log`
  （12 类分拣修复复跑）→ `gates/T3-030925.log`（**收口 T3：ADDED 0 / REMOVED 2**）。
- DD 切片日志：`gates/T2-065114.log`（T2 74 红 = 31 基线 + 43 新增 + 车道红，逐类分拣）、
  `gates/T3-073402.log`（T3 首跑 ADDED 19 = T2 选择器盲区的对齐契约类，10 类三模式分拣）、
  `gates/T3-075615.log`（**收口 T3：ADDED 0 / REMOVED 2**，两例为本片有意收口并登记）、
  `gates/T1-080420.log`（收口 T1：76 例唯一红 = 20035 车道红，零新增）。
- DD 顺序链日志：`gates/T2-081659.log`（36 id T2：29 红 = 8 基线 + 21 新增，QE-085 三模式分拣）、
  `gates/T3-083216.log`（**收口 T3：ADDED 0 / REMOVED 0，135→135 完全同态**）、
  `gates/T1-084004.log`（收口 T1：76 例唯一红 = 20035 车道红，零新增）。
- SimpleTalk S1 日志：分拣预筛台账 `2026-09-27-s1-t2-triage.zh-CN.md`（1804 id → 128 类；档 1/2/3）、
  过场逐行取证 `2026-09-27-s1-cutscene-rows.zh-CN.md` + `s1-cutscene-rows.tsv`（13 行落点表）、
  `gates/T2-125926.log`（首跑 39 新红）→ **终版 `gates/T2-132715.log`（823 例，新增 0）**、
  **收口 T3 `gates/T3-132734.log`（ADDED 0 / REMOVED 0）**。
- DD 四子面日志（本片）：聚焦 `gates/T2-095921.log`（分拣前的 14 个本片新增失败，12 类）、
  `gates/T2-103314.log` + 终版 `gates/T2-105158.log`（228 id T2：**新增 0**，23 个 T1 门类唯一红 = 20035）、
  `gates/T3-104123.log`（T3 首跑：ADDED 1 = 1103 计划形状锁，根因已修）→
  `gates/T3-105219.log`（**收口 T3：ADDED 0 / REMOVED 3，逐条登记**）、
  指纹留痕 `dd-fp-da.tsv` / `dd-fp-da2.tsv` / `dd-fp-db.tsv`（1217 行全量 dump，非漂移行零漂移对拍）。
  T1 未单独复跑：T2 的 selector = T1 清单 ∪ 命中类，终版 T2 日志已内含全部 23 个 T1 门类的结果。
- 记忆库：QE-082/QE-083 已沉淀（`patterns/quest-engine.md` + systemPatterns 路由），
  `sync_memory_bank.py` + `verify_memory_bank.py` → `MEMORY_BANK_VERIFY_OK STEPS=3`。

## 纪律回执（SimpleTalk S1 片追加）

- 未 commit/push；未启停服务进程；Maven 仅聚焦测试与 T1/T2/T3 门禁；
- **未新增/未退役任何 TSV**（清单门 3 例全绿）；**零生成物 churn**（无指纹重冻、无 drift 重算——链面未动）；
- T3 按纪律在仓库外隔离 clean 副本跑（`/private/tmp/aion-t3-s1`），跑完即 `rm -rf`，无残留；
- 临时诊断探针 5 个（`TempJourneyProbe`/`Temp1103Probe`/`TempS1DeliveryProbe`/`TempS1ResidueProbe`/
  `TempS1AcceptanceProbe`/`Temp1351Probe`）用完即删源文件与 `.class`；
- 内存库：新增 **QE-086**（规范形过场重挂的作用域键 + match==1 fail-closed）并补 `systemPatterns.md`
  路由；`MEMORY_BANK_SYNC_OK ENTRIES=131`、`MEMORY_BANK_VERIFY_OK STEPS=3`；
- 新增产物：家族门 +1 例过场不变量、三份 S1 台账（`2026-09-27-s1-t2-triage.zh-CN.md` /
  `2026-09-27-s1-cutscene-rows.zh-CN.md` + `s1-cutscene-rows.tsv` / 本段）。

## 纪律回执（DD 四子面片追加）

- 未 commit/push；未启停服务进程；Maven 仅聚焦测试与 T1/T2/T3 门禁（计划授权范围）；
- **未新增任何页码类 TSV**（WP4 门禁落地即证明：终版 T2 中 `RetailTsvManifestGateTest` 3 例全绿）；
- TSV 仅发生**一笔退役**：`dd-fp-fresh.tsv`（删文件 + 删清单行 + `EXPECTED_TSV_COUNT` 27→26，三者同片）；
- T3 按纪律在**仓库外隔离 clean 副本**跑（`rsync --exclude target/.git/patch` → `/private/tmp/aion-t3-dab2`），
  跑完即 `rm -rf`，无残留副本、无 worktree；
- 临时诊断探针（`TempJourneyProbeTest`、`Temp1103ProbeTest`）用完即删源文件与 `.class`（不留在树上）；
- 新增产物：`quest-retail-tsv-manifest.tsv` + `RetailTsvManifestGateTest.java`（WP4）+
  `2016-09-27` 两份勘察文档（SimpleTalk 底稿、TSV 退役盘点）+ 本 README 段落。

## 纪律回执（SimpleTalk S2 链式片追加）

- 未 commit/push（沿用"先不提交"）；未启停服务进程；Maven 仅聚焦测试与 T2/T3 门禁；
- **未新增/未退役任何 TSV**；链登记表（兄弟车道产物 `quest_client_talk_chain_steps.tsv`）**只读**，一字节未改；
- 重冻产物仅 1 个：`retail-simple-talk-chain-ir-fingerprints.tsv`（203/0/0 == G1；G2/G3 的 82 行逐字未动）；
- T3 按纪律在**仓库外隔离副本**跑，共三次试跑（末次全树副本 `rsync --exclude .git --exclude target`，≈1.3G）：
  前两次瘦副本依次暴露 `docs/`（客户端 CSV）与 `.agents/`（生成器脚本）两处遗漏造成的**副本完整性伪红**，
  弃用日志留痕（`T3-150734.log` / `T3-151932.log`）；正式跑 `T3-153035.log`（缺文件计数 4 == 基线条件）；
  跑完即 `rm -rf`，无残留副本、无 worktree；
- 共享树并发取证：兄弟车道在首跑 T3 窗口内创建/删除了两个临时探针类（`RetailTalkChain*ProbeTest`），
  已按 QE-060 归属三步归一化并在 §5.6 记录，正式证据取隔离副本；
- 临时诊断探针 1 个（`Temp21081ProbeTest`）用完即删（源文件 + `.class`）；
- 内存库：新增 **QE-087**（登记表驱动链行的规范段改造：过滤按段作用域 + 零残留守卫 + 载荷 fail-closed）
  并补 `systemPatterns.md` 路由；`MEMORY_BANK_SYNC_OK ENTRIES=132`、`MEMORY_BANK_VERIFY_OK STEPS=3`；
- 新增产物：S2 收口记录 + 侦察四件套 + 分拣三件套（`2026-09-27-s2-*.zh-CN.md` + 各自 TSV）。

## 纪律回执（SimpleTalk S3a 片追加）

- 未 commit/push（沿用"先不提交"）；未启停服务进程；Maven 仅聚焦测试与 T2/T3 门禁；
- **未新增/未退役任何 TSV**；链登记表**只读**（一字节未改）；重冻产物仅 1 个
  （`retail-simple-talk-chain-ir-fingerprints.tsv`，60/0/0 == 切片行集，S2 的 203 行零漂移）；
- T3 在**仓库外全树副本**（`/private/tmp/aion-t3-s3a`，`rsync --exclude .git --exclude target`）跑；
  预跑（分拣前）已完成后被终版覆盖，终版在分拣后以增量重同步（`src/ docs/ .agents/`）重跑，跑完即删；
- 并行纪律：tree 内同一时刻只有一个 Maven（T2 在树内、T3 在副本，二者并行）；分拣代理**不跑 Maven**，
  由车道统一聚焦验证（`-Dtest=Quest1152RetailAlignmentTest,Quest1118ProductionFlowTest` = 2/2 绿）；
- 本片新增的 fail-open 自查（对照裁定简报 G-1 同族）：S3a 退场的接取记录**零条**带
  `REMOVE_ITEM` 而无 `HAS_ITEM`、零条带 `MOVIE`/`GRANT`/`COMPLETE_QUEST` ⇒ 无静默丢载荷；
  S2 的 203 行退场面同轴复算亦为 0（G-1 属**潜在**守卫缺口，非现存缺陷，已移交守卫加固片）；
- 内存库：新增 **QE-088**（按段接管：段旗标解耦 / 守卫分治 / 段外载荷派生式延期 / 对照断言 / 零测试引用行）
  并补 `systemPatterns.md` 路由；`MEMORY_BANK_SYNC_OK ENTRIES=133`、`MEMORY_BANK_VERIFY_OK STEPS=3`；
- 新增产物：S3a 收口记录 + T2 预分拣 + 分拣两件套 + γ 面裁定简报（`2026-09-27-s3*.zh-CN.md`）。

## 纪律回执（守卫加固片 + SimpleTalk S3b 片追加）

- 未 commit/push（沿用"先不提交"）；未启停服务进程；Maven 仅聚焦测试与 T2/T3 门禁；
- **未新增/未退役任何 TSV**；链登记表只读（一字节未改）；两片各重冻 1 次指纹（加固片 0/0/0、S3b 7/0/0）；
- T3 两次均在**仓库外全树副本**（`/private/tmp/aion-t3-hardening`、`/private/tmp/aion-t3-s3b`）跑，
  跑完即 `rm -rf` 并确认（无残留副本、无 worktree）；tree 内与副本并行时树内只有一个 Maven；
- 开发期修正全部留痕（加固片：守卫四轴从"潜在缺口"定性；S3b：接取侧载荷守卫跨侧判定误伤 11 行后分侧）；
- 记忆库：**QE-089**（守卫四轴守恒）+ **QE-090**（层 A 键覆盖 + R 驱动段合成）；
  `MEMORY_BANK_SYNC_OK ENTRIES=135`、`MEMORY_BANK_VERIFY_OK STEPS=3`；
- 新增产物：`2026-09-27-s3-guard-hardening.zh-CN.md`、`2026-09-27-s3b-r-driven-accept.zh-CN.md`。

## 纪律回执（SimpleTalk S3c-A 片追加）

未 commit/push；未启停服务；**未新增/退役任何 TSV**（链登记表只读，一字节未改；manifest 计数不变）；
未创建 worktree；T3 在仓库外全树副本（`/private/tmp/aion-t3-s3c`）跑、跑完即删；Maven 仅用于聚焦测试与
T2/T3 门禁；树内与副本并行时树内只有一个 Maven；中间产物全部落 `.agents/summary/quest-native-dispatch/`。

## 纪律回执（SimpleTalk S3c-D 片追加）

未 commit/push；未启停服务；**未新增/退役任何 TSV**；未创建 worktree；T3 在仓库外全树副本
（`/private/tmp/aion-t3-s3cd`）跑、跑完即删；Maven 仅用于聚焦测试与 T2/T3 门禁；树内与副本并行时树内只有一个 Maven。

## 纪律回执（SimpleTalk S3c-obj 片追加）

未 commit/push；未启停服务；**未新增/退役任何 TSV**（链登记表只读，一字节未改）；未创建 worktree；
T3 在仓库外全树副本（`/private/tmp/aion-t3-s3cobj`）跑、跑完即删；Maven 仅用于聚焦测试与 T2/T3 门禁；
树内与副本并行时树内只有一个 Maven；AI 中间产物（普查脚本）落 `.agents/summary/quest-native-dispatch/`。

## 纪律回执（W5-g1 片追加）

未 commit/push；未启停服务；**未新增任何 TSV**（本片只做退役：删表 + 删清单行 + 计数 −1）；未创建 worktree；
T3 在仓库外全树副本（`/private/tmp/aion-t3-w5g1`）跑、跑完即 `rm -rf`；Maven 仅用于聚焦测试与 T1/T2/T3 门禁；
树内与副本并行时树内只有一个 Maven；兄弟车道文件（生成器脚本、链登记表）**只读**。

## 纪律回执（W5-g2 片追加）

未 commit/push；未启停服务；**未新增任何 TSV**（第二张退役：删表 + 删清单行 + 计数 −1）；未创建 worktree；
T3 在仓库外全树副本（`/private/tmp/aion-t3-w5g2`）跑、跑完即 `rm -rf`；Maven 仅用于聚焦测试与 T1/T2/T3 门禁；
树内与副本并行时树内只有一个 Maven；取证子代理**只读、不跑 Maven**，结论由主执行体复核后入台账。

## 纪律回执（W6-a 片追加）

未 commit/push；未启停服务；未新增/退役任何 TSV；未创建 worktree；T3 在仓库外全树副本
（`/private/tmp/aion-t3-w6a`）跑、跑完即 `rm -rf`；Maven 仅用于聚焦测试与 T1/T2/T3 门禁；树内与副本并行时
树内只有一个 Maven；兄弟车道文件只读；**取证子代理只读、不跑 Maven**，结论由主执行体复核（一条"过期基数"
结论被复核否决并更正）。

## W6 尾 + W7 终局已收口（2026-09-28，缺口批后收尾阶段）

> 宪章：`2026-09-28-w6tail-w7-stage-charter.zh-CN.md`（提交 `82e7e3419`）。此前「下一面」一节
> 的剩余项就此全部关闭：D 类加窗已裁定维持现状（缺口批 5 台账）、W5 `dialog_exits` 缩表已收口
> （批 0 R1）、W6 尾两项见下；Phase 2 规范形全家族已由并行车道收口（见上各节）。

- **批 N1（EarlyElyos 3 在册红重锚）**：`ed45c2b35` + 台账 `7a83739b1`
  （`2026-09-28-n1-earlyelyos-reanchor.zh-CN.md`）。三红 = 锁迁移前旧 XML 形的
  `EarlyElyosQuestRegressionTest` 断言（1131 节点名 s1 / 1561 SimpleUseItem 规范形 /
  1691 s1·s2·s3 阶梯），探针 dump 留档 `w6tail-earlyelyos/`。聚焦 17/17 绿；
  **T2/T3 基线有意重冻**：T2 51→48（`fb70bc91…`）、T3 97→94（`5e3acdb9…`），
  diff 恰各 3 条 EarlyElyos 移除；T1 基线不变（`3b92439d…`）。
  新基线入册 `gates/T2-n1-baseline-reds.txt` / `gates/T3-n1-baseline-reds.txt`。
- **批 N2（FailurePage 命名拆分，零形状）**：`bfe9a0aa7` + 台账 `c45b5d266`
  （`2026-09-28-n2-failurepage-naming.zh-CN.md`）。删零调用死代码 `itemReport`
  （`hasFailurePage` 载体）；R3 消歧 javadoc（SELECT6=2716 ≠ CHECK_USER_ITEM_FAIL=10001）
  迁到活路径 `itemReportGate`；审查契约测试方法名带页号消歧。快筛 4 门 22/22 绿。
- **批 N3（W7 终局）**：终点全量对拍对**新基线**（结果见下）；README 终审（本节 +
  「下一面」节标记为已由本节关闭）；迁移总量终局复算：
  **6224 = 4983 真端驱动（RETAIL_TABLE）+ 1241 保留 XML（catalog 与生产 XML 一一对应；
  保留内部 = ADJUDICATED 571 + SCRIPTED 494 + NO_TABLE 176，精确闭合）；
  SEMANTIC_GAP = 0；`EXPECTED_TSV_COUNT` = 22**。
- **生成器停写移交（终版清单，scriptdll-quest-driver 车道 owner 消费）**：
  ① `build_quest_client_report_pages.py` 仍会写回已退役 report_pages 路径（W5-g1 起）；
  ② `build_quest_client_dialog_exits.py` 不感知 R1 缩表（批 0 起）；
  ③ `build_retention_list.py` 已滞后于全部手工裁定（重跑会回退 ADJUDICATED 行，
  是否补语义由 owner 裁定）；
  ④ `p0c52_quest_ai_name_groups.py` 仍会写回已退役的 `retail-quest-ai-name-groups-rejected.tsv`
  （P1 起；重跑即被清单门拦红）。四者本车道均只登记不改。
- **用户侧并行项**：24 行实机复验（`client-recheck-list.zh-CN.md`）；push 授权
  （缺口批 11 提交 + 本阶段提交均未 push）。
- 门禁证物：`gates/n1-earlyelyos-focus/fix.log`、`gates/T1-073737.log`、`gates/T2-072910.log`、
  `gates/n1-t3-1/2/3.log`、`gates/n2-quick.log`、`gates/T1-075148.log`、`gates/T2-080039.log`、
  `gates/n3-t3-1/2/3.log`。

## Phase 3 勘测已收口：TSV 全量退役可行性（只读，2026-09-28，提交待记）

- 授权 = 用户「phase 3」；范围 = 只读取证（零形状、零 Maven、红集不解冻）。
- 报告：`phase3-recon/2026-09-28-phase3-feasibility.zh-CN.md` + 引用矩阵
  `phase3-recon/census-readers.tsv`（脚本 `census_readers.py`）。
- 结论：22 张 → **可退役 1**（`ai-name-groups-rejected`，main/test 双零引用，删除需 owner
  认可审计账处置）+ **需缩表 1**（`dialog_exits` 普查 v2：读取点全活但 Phase 2 后行级死亡面
  扩大，批 0 R1 同法重算）+ **仍活 20**（多为语义登记非页码补丁；老 Goal「废 18 张页码 TSV」
  已到自然终点）。执行批立项：**P1 已执行（2026-09-28，用户决定「1」）**，
  P2（缩表）/P3（计数冻结）待决定。
- **批 P1 已执行（2026-09-28，用户决定「1」）**：`retail-quest-ai-name-groups-rejected.tsv`
  （23 行，sha256 `180627ee…`）整表退役——快照 `retired-tsv/…retired-20260928` + 删表 +
  删清单行 + `EXPECTED_TSV_COUNT` 22→21 + 生成器停写移交（终版清单第 ④ 项）。零代码变化；
  **门禁已过**：聚焦清单门 3/3、T1 红集 `3b92439da8…`、T3 基线口径红集 `5e3acdb9dcaf…`
  均与基线逐字节相同（T3 首轮 +2 为副本陈旧 test-classes 幽灵红，已清理并沉淀 QE-084）。
  台账：`2026-09-28-p1-ainame-rejected-retirement.zh-CN.md`。

## Phase 3 权威血缘审计：21 张在册 TSV 的"真端驱动"成色（只读，2026-09-28）

- 报告：`phase3-provenance/2026-09-28-authority-provenance-audit.zh-CN.md` + 矩阵
  `phase3-provenance/authority-provenance.tsv`（21 行 × 9 列：权威源/生成器/证明门/
  是否补真端表没有的/证据指针/裁定）。
- 结论：运行时主驱动 = 9 张真端表（4983 RETAIL_TABLE）+ 1241 保留 XML；TSV 为
  客户端合同 / 派生投影 / 审计账三类辅助，**未发现"真端表 ∪ 客户端 ∪ 旧存档"三源之外的行为注入**。
- 裁定：保留 17、保留+缩表 1（`dialog_exits` → P2）、再生成通道需补 3
  （G1 `use_item_report` 生成器缺失 / G2 `name_string_ids` 无脚本 / G3 `kill_targets`
  生产表与测试夹具逐字节相同=未记录拷贝）、门禁说明 1（G4 `legacy_heal` census 口径 test=0）。
- 无退役候选；不新增 TSV。

### 血缘缺口处置（2026-09-28，随审计落账）

- **G1/G2 已冻结**：`quest_client_use_item_report.tsv`（声明生成器不在树内）与
  `quest_name_string_ids.tsv`（无脚本）——内容 sha256（+ G2 主来源客户端
  `client_strings_quest.xml` sha256）钉入 `phase3-provenance/provenance-pins.tsv`，
  由 `check_provenance_pins.py` 校验（`PROVENANCE_PINS_OK`）。
- **G3 已补通道**：`quest_client_kill_targets.tsv` 与测试夹具整文件逐字节相同（`250ff5ee…`）；
  新增 fail-closed 提升脚本 `regenerate_kill_targets_production.py --check/--apply`（`G3_CHECK_OK`）。
- **G4 已解除**：`quest_legacy_heal_rows.tsv` 表头书面口径（armour 方向 vs 移除 XML 边互斥）+
  机检互斥不变量；测试侧链条由 `RetailNonIrAxisGateTest`（经 p0c11 统一登记表）常设守。
- **P2 已立项**：`2026-09-28-p2-dialog-exits-shrink-charter.zh-CN.md`
  （P2a 只读普查 v2 → P2b 缩表；判据新增「每 token 必须有客户端出口证据」）。
