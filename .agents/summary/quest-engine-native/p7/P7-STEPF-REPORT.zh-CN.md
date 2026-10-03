# P7 步 f 报告：五项前置坐实 + 1444 行原子切换 + 同批删旧（生产行为切换批）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 批次：P7 DataDriven 步 2 步 f（计划 §7「P7 DataDriven」/ §10.2 / §10.3-#22#23#24；前置裁定
  `p7/P7-STEPF-PREREQ-ADJUDICATIONS.zh-CN.md`）
- 日期：2026-10-02（承接步 e2 的 2026-10-02 收口，提交 972a70488）
- 性质：**本批 = 计划内的生产行为切换批**——生产路由集从空切换为切换集（此前各步均为零行为变更）。
- 结论：五项前置（距离闸门 case-0 / Talk 报告通道动作面 / Spawn 可行走校验 / Say 通道 /
  EnterArea 接取区几何）逐项按真端宿主源坐实并落面（两项 EVIDENCE_MISSING 残留按 fail-closed 登记）；
  生产 `DataDrivenNativeRuntime.loadProduction` 接管切换集 1467 行（**可路由 1444 / 显式冻结 23**，
  冻结带 `FreezeReason` 登记 §10.3 = 非静默丢弃）；同批删除旧 DD 编译车道
  （`RetailDataDrivenDefinitionCompiler` + Talk/TalkHuntChain/TalkCollectChain 三链编译器 +
  `RetailDataDrivenTable` 旧视图 + `RetailClientAcceptEntryPage` 修复面 + typed 形
  `QuestDialogContract.acceptEntryPage`，§10.3-#22 闭环）；TSV 清单 +1 行（接取侧 14 区随
  `zones_retail_enterarea.xml` 落盘，91 → 105 区）。

## 1. 五项前置裁定（全文见前置裁定笔记）

| 前置 | 裁定 | 落面 |
|---|---|---|
| F1 距离闸门 §10.3-#24② | case 表全坐实（0/1→≤2500+同图/阵营组判定；两变体 Hunt/PvP 逐字引用）；**param_6 坐标算式 EVIDENCE_MISSING ⇒ 不实现**（本服击杀恒同图，未镜像差异仅 2500 距离界） | 不落代码；#24② 收窄为坐标算式单轴保持开放 |
| F2 Talk 报告通道动作面 | **e2 矩阵修正**：对话平面推进/完成汇入 `FUN_180c4d5b0(…,-1)`，C:2075066 直调 `c8d0(完成步动作)`，门 = 完成步 kind==4；Talk 对话接取 1002/20000 收尾同走 -1 路径 ⇒ **步 0 动作**；CollectItem 拾取分支无执行器 ⇒ 排除；五个推进边全部传完成步表（步号镜像无 off-by-one） | `apply()` 执行集合 = {Hunt, EnterArea, TalkFOBJ, **Talk**}；`dispatchAcquireDialog` 1002/20000 接取成功后 `runActions(step 0)`；≥1000 演出回显面（节点 id 绑定未坐实）= 登记偏差 |
| F3 Spawn 可行走校验 | `World_RandomWalkableSpawnLocation` 逐字坐实（±半径正方形均匀采样 ×0x20 + 格子可行走 + AABB + 高差容差 4 + 路径可达，全败回退中心；Absolute 原样） | `NativeSpawnPort.Live.sampleWalkable` 镜像（`GeoMap.getZ` + `canPassWalker`）；到期回收者/飞行/水面支线 = 登记偏差 |
| F4 Say 通道 | 真端 Say 文本 = 字符串 id；本服 `SM_MESSAGE` 无 id 通道、无服务端文本表 ⇒ 忠实气泡不可实现 | 维持 `SM_SYSTEM_MESSAGE(id)`，**永久登记偏差**（客户端渲染同文本） |
| F5 EnterArea 接取区几何 | acquire 轴 15 别名 = 14 OK + 1 真端无区定义（R4） | 探针双轴 emit → `zones_retail_enterarea.xml` 91 → **105 区**；`AcquirePlan` kind 6 + `acquireZonesByName` 兴趣面 + `onEnterZone` 双角色；缺席别名登记原文 = 死边镜像（不冻结） |

## 2. 切换与删旧

- **生产切换**：`loadProduction()` = retention 台账（owner RETAIL_TABLE ∧ family DataDriven = 1467 候选）
  + `NativeEnterAreaPort.create`（注册区面 = `DataManager.ZONE_DATA`，单测回退扫同一 zones 目录）+
  `NativeNpcNameResolver.instance()` + `RetailItemNameIndex.loadItemTemplates()` + 五端口 live()；
  行级裁定在 `create` 内逐行完成（1444/23）。启动次序依赖静态数据先行
  （`GameStartupSequenceLifecycle`：`preloadProductionCatalog → staticDataLifecycle.start →
  enginesLifecycle.start`）。
- **owner 面**：`QuestEngine.isNativeOwner` 增 `DataDrivenNativeRuntime.routes(questId)`；
  `verifyProductionCoverage` 覆盖三分册 = 目录条目 ∨ 七族 handler owns ∨ **DD 运行时 owned**
  （routed 或显式冻结——冻结带 `FreezeReason`，非静默丢弃）；`splitRuntimeCatalogs` retail 子目录恒空
  （编译产物为零）。
- **同批删旧**（§10.3-#22 闭环）：删 `RetailDataDrivenDefinitionCompiler`、`RetailDataDrivenTalkCompiler`、
  `RetailDataDrivenTalkHuntChainCompiler`、`RetailDataDrivenTalkCollectChainCompiler`、
  `RetailDataDrivenTable`、`RetailClientAcceptEntryPage`；`QuestDialogContract` 删 typed 形
  `acceptEntryPage`（页 4 优先）+ `ACCEPT_ENTRY_PAGE_PREFERENCE`（native 口径 `retailEntryPage` 保留）；
  `RetailQuestDriver` 重写为三职责（quest.xml 元数据底座 / 生产覆盖校验 / 直通 overlay），
  其 AI 台账（`ai-areas.xml`）与 enterarea 解析表读取随编译车道退场；孤儿 client* 台账类
  （`RetailClientHandinPages`/`RetailClientKillTargets`/`RetailClientHuntProgressRows`/
  `RetailClientHuntStages`/`RetailEnterAreaZoneResolution`〔门禁仍读〕/`RetailQuestAreaIndex`
  〔SimpleHunt 残片仍引用〕等）= **P8 清扫面**（本批不动）。
- **冻结 23 行的步 f 后形态**：9 行 ZONE_ABSENT（真端 LF6 侧无区定义，镜像真端死步）+ 14 行
  ACTION_UNFACED（col9 Instance 3 / col10 Timer 9 / c8d0 步 col6 2）——进度/接取面缺席，
  解冻随其证据闭合（§10.3-#23/#24③ 保持登记）。

## 3. 测试面（随车道删旧同步重锚）

- **删**（门的载体 = 已删车道的编译产物）：`RetailDataDrivenGateTest`、`RetailClientAcceptEntryPageTest`、
  `Quest80787To80794RetailAlignmentTest`、`QuestEnterWorldPvpRetailContractTest`（15596 DD 行）、
  `Quest16803/16804/26803/26804ClientDialogAlignmentTest`、`Quest2877To2887UrgentOrdersFlowTest`、
  `QuestArchivesMissionCounterProductionFlowTest`、`QuestDataDrivenHuntProductionFlowTest`。
- **迁**：`RetailDataDrivenClientPresenceGateTest` 载体 RetailDataDrivenTable → `DataDrivenQuestTable`
  （80817 载荷尾 `;` 按前导整数解析）；`DataDrivenQuestTableGateTest` ①旧视图逐行对拍删除（旧视图已删，
  LIVE_ROWS 冻结保留为独立测试）；`DataDrivenNativeContractGateTest` ⑥装载器对拍半删除（冻结摘要保留）；
  `DataDrivenEnterAreaPortGateTest` 接取轴断言翻转（OK 行必须已注册 / R4 行必须未注册）；
  `ClientAcceptEntryPageAssertions` 改测试侧预言机（同偏好序页 4→4762→1011，不回主代码）；
  `RetailQuestDriverOverlayTest` 重写（全集恒等式 6224 = 目录 + 原生覆盖〔七族 ∨ DD owned〕+
  DD 1467 全量 owned 断言 + 负例改 XML_RETENTION 行 + overlay 直通）；
  `CombineTaskRowAlignmentGateTest` 对齐 `retailEntryPage`；`SimpleCollectItemNativeFamilyGateTest`
  旧 owner 断言随面退场。
- **扩**：`DataDrivenNativeRuntimeGateTest` 16 → 18 例——①生产单例翻转（1467 owned / 1444 routed /
  23 frozen / 兴趣面非空）；接取直方图冻结（talk 1126 / itemplay 13 / enterworld 12 / leveluplogin 15 /
  **enterarea 20** / none 258）；⑫执行矩阵翻转（Talk 推进边**执行**、CollectItem 推进零动作）；
  新 ⑪-b kind-6 进区接取面（20 行 / 15 别名、同名区接取 + 步 0 动作、缺席别名死边断言）；
  新 ⑩-b Talk 对话接取步 0 动作。

### 3.1 第二波：DD 定义消费测试随删旧同步裁定（判定规则 + 清单）

**判定规则**：凡解析 native-owned 行（retention owner RETAIL_TABLE = 七族 ∨ DD 1467 行）的
生产视图定义（`ProductionQuestDefinitions.definition/definitionInOverlay/findExecutable`）的测试，
其主语已随删旧消失——行级流程主语 = 删（原生语义由家族门 + DD 运行时门承担）；
跨目录扫描门 = 按原生归属豁免后保留（invariant 仍覆盖 XML 保留行）；
逐 e2 基线核对：e2 绿 ⇒ 其加载的定义只能来自 DD 合成 ⇒ 全量 DD 主语（e2-green 论证）。

- **整类删 25**（全部主语 = DD 行）：`Quest10501HandoverContinuationTest`、
  `Quest10503/10504ClientDialogAlignmentTest`、`Quest13765RetailAlignmentTest`、
  `Quest15042ProductionFlowTest`、`Quest15546KillCounterSaturationFlowTest`、
  `Quest18950/1919/21320/26800/28950/2929/30515ClientDialogAlignmentTest`、
  `Quest80875RetailAlignmentTest`、`QuestCollectProgressAlignmentGateTest`（10503/10530/20530/10504）、
  `QuestCradleReunionProductionFlowTest`（16823/26823）、`QuestDaevanionLeggingsProductionFlowTest`
  （15314/25314）、`QuestDataDrivenRetailFlowAlignmentTest`（25321/25306）、
  `QuestEnterZoneStartOwnerRegressionTest`（14123/15322/25322——进区不自动接取合同由 DD 门 ⑪-b 与
  SimpleHunt 门承担）、`Quest13830To13834TargetlessRewardTest`、`QuestIlumaNorsvoldKillTargetCoverageTest`
  （25504/15546）、`QuestEventShardRetailAlignmentTest`（50073/50074）、
  `QuestDaevanionThreeStageFlowTest`（15315/25315）、`QuestRetailCollectionRoleAlignmentTest`
  （25022/25073/25013）、`QuestItemPlayGrantGateTest`（DD ItemPlay 道具可得轴转 P8 以 DD 运行时步载荷
  重建；SimpleItemPlay 家族门继续覆盖其家族行）。
- **手术删 22 类中的 DD 方法**（30 方法，余下方法保留原红/绿态）：`ClientQuestSectionAlignmentTest`
  （18931）、`GrowthQuestDialogPageAlignmentTest`（19671-19673 四法）、
  `MigratedQuestRepairDefinitionTest`（11319/13955/15602 五法）、`Quest16802/26802ClientDialogAlignmentTest`、
  `Quest19637ClientDialogAlignmentTest`、`Quest25512ClientDialogAlignmentTest`、
  `Quest25608RetailSevenStepAlignmentTest`（五法）、`Quest28932RewardRowContractTest`、
  `QuestCounterProjectionLockFollowUpTest`（15321/25608/27510）、
  `QuestMovieAndDialogLoopRegressionTest`（16942/26942、15301/25301）、
  `QuestLegacyMonsterHuntProductionFlowTest`（25090/25093、25xxx 组、29691）、
  `QuestPrematureRewardRouteExclusionTest`（16988/19631）、`QuestRefactorRepairRegressionTest`
  （13945 组、26930）、`QuestResidualCounterLocksTest`（17541、15322/25322）、
  `RetailSequentialQuestFamilyTest`（15590/25590）、`QuestDraupnirNpcVariantContractTest`（14252/24252）、
  `QuestA03ShardRetailAlignmentTest`（23920/25533/25640/25698）、
  `QuestNoHandlerShard3DefinitionTest`（29634/30565）、`QuestBatchReportNpcAlignmentTest`（18737）、
  `QuestReportedRewardCoverageTest.classRewardMetadata…`、`QuestRepeatLifecycleTest`（15476；1963 原生法保留）、
  `QuestFactRequirementsTest`（19638/19637）、`QuestDependencyIndexTest`（10011/10032）。
- **门重锚 6**（扫描域豁免 native 行，invariant 保留给 XML 行）：
  `QuestRewardItemGateTest`（`nativeOwned` + DD `owns()`；fail-closed 残余回到 {30720,30723}）、
  `QuestInteractionObjectContractGateTest`（可执行定义下限 2100→700，实测 714 = XML 保留行）、
  `QuestProductionStartupGateTest`（SystemGrant 目录边锚 → 双原生通道锚 = `NativeSystemGrantLanes`
  聚合面 + DD 接取型发放兴趣面 levelup/enterworld）、`QuestKillCounterRetailGateTest`（native 行跳过 +
  13765 反向对照三件套删除——行阶梯语义由 DD 门承担；合同快照 414 不变）、
  `QuestSection0ReportRowContractTest`（合同 269 行全数 native ⇒ 两扫描法退役，仅存 TSV 形状门）、
  `RetailSingleStepRewardRowContractTest`（XML 轴只剩 29002；A/B 组随 DD 行由 DD 门承担）。

### 3.2 both-red 类登记（e2 已红、本批不改，P8 重锚）

`QuestPrerequisiteRetailContractTest`（498→106 分支覆盖）、`QuestRetailClassGateTest`
（native 行 class 轴缺目录载体——**原生行职业轴执行位 = 新登记开放轴**）、
`QuestMetadataFieldMappingTest`（6224→740 期望）、`QuestReportedRewardCoverageTest.method1`、
`QuestRetailCollectionRoleAlignmentTest`（已删）、`ClientQuestSectionAlignmentTest` 残余、
`QuestMovieAndDialogLoopRegressionTest` 目录扫描法、`QuestDraupnirNpcVariantContractTest` 残余法、
`RetailSequentialQuestFamilyTest.quest15321…`、`QuestA03ShardRetailAlignmentTest.pureTalk21065…`
（21065.xml 已退役文件直读）、`QuestPrerequisiteRetailContractTest.unported…`——同因（native 行定义
退场）在 e2 已红，红法不变或等价漂移；一律归入 P8「重锚门禁」批，不在步 f 扩批。

## 4. 门态（本批显式命令）

```
# DD 门对
mvn -o test -Dtest='DataDrivenNativeRuntimeGateTest,DataDrivenProgressTest'
# → 18 + 9 全绿
# 家族 + tablelane
mvn -o test '-Dtest=*FamilyGateTest,*RowAlignmentGateTest,*InventoryGateTest,NativeQuestTableLoaderTest,
  *RewardClaimGateTest,*NativeTalk*Test,NativeNearbyQuestAxisGateTest,SMNearbyQuestsPacketTest,
  NativeAcceptEntryAskFlowGateTest,DataDrivenNativeContractGateTest,DataDrivenProgressTest,
  DataDrivenQuestTableGateTest,DataDrivenEnterAreaPortGateTest,DataDrivenNativeRuntimeGateTest,
  MultiCellSensoryZoneRegistrationTest,RetailEnterAreaZoneRegistrationGateTest'
# → 189/189 绿
# 迁移门
mvn -o test '-Dtest=RetailDataDrivenClientPresenceGateTest,RetailQuestDriverOverlayTest,
  RetailOwnershipGateTest,NativeQuestOwnerResolverTest'
# → 4 + 5 + 4 + 6 全绿
# 聚焦套件
mvn -o test '-Dtest=*Quest*Test,*Retail*Test'
# → 1478 例 / 34F+178E / 84 红类；对步 e2 基线（105 红类 / 207 红例）：
#   类级 ADDED 0 / 测试级 ADDED 0；REMOVED 21 类全部为已删车道测试（21 类清单见 §3.1）；
#   残余 161 红例 = e2 既有基线红（归因：native 行生产视图定义消费 / 并行车道登记红）
```

日志：`gates/2026-10-02-p7-stepf-*.log`、`gates/2026-10-02-focused-run-p7-stepf-v2.log`（临时，不入库）。

## 5. 反漂移规则（本批新增）

1. **执行矩阵以 d5b0 汇入点为准**：对话平面推进/完成/接取收尾统一经 `FUN_180c4d5b0`，-1 路径直调
   c8d0 且门在**步 kind==4**；执行步号 = **完成步**（接取路径 = 步 0）。禁止「对话平面零执行」与
   「进入步执行」两种旧误判。
2. **切换集 = retention 台账权威**（owner RETAIL_TABLE ∧ family DataDriven = 1467）；行级
   routed/frozen 由运行时逐行裁定，冻结必须带 `FreezeReason`（覆盖校验只认「目录 ∨ 原生 owner ∨
   显式冻结」三册，禁止第四种静默丢弃）。
3. **EVIDENCE_MISSING 不落代码**：param_6 坐标算式（距离闸门）、Timer 到期分发、EnterInstance
   creationId 映射、d5b0 演出回显节点绑定、Spawn 到期回收者——均保持登记态，禁止按直觉补齐。
4. **测试侧预言机不回主代码**：`ClientAcceptEntryPageAssertions` 的页 4 优先序只为 XML-only 对齐金标
   服务；主代码唯一口径 = `retailEntryPage`（信页优先）。

## 6. 剩余（P8 + 验收）

- **P8 收口**：孤儿 client* 台账类与 `RetailQuestAreaIndex`/`RetailEnterAreaZoneResolution` 主类
  （门禁/SimpleHunt 残片仍在读）清扫；五个台账退役裁定；剩余客户端派生资源；overlay/driver 进一步
  坍缩；重锚门禁；memory-bank 同步。
- **P8 新登记轴（步 f 产生）**：
  1. **DD ItemPlay 道具可得门重建**——`QuestItemPlayGrantGateTest` 随 IR 人口退场，等价门改为
     「DD 运行时步载荷 itemplay itemId → 物品模板/掉落/授予三源对拍」（§3.1 门退役条）；
  2. **原生行职业轴执行位**——`QuestRetailClassGateTest` 的 class 合同在 native 行上无目录载体，
     需裁定（a）真端 class 限制是否为服务端闸门（若是，接入 native 接取面；若否，登记为显示元数据）；
  3. **both-red 门重锚**——§3.2 清单（prerequisite 覆盖下限、class 合同、metadata 全集 6224→740、
     reported-reward 人口等）随 P8「重锚门禁」统一处理；
  4. **冻结 23 行解冻**（§10.3-#23/#24③ 证据闭合后）。
- **客户端验收 `PENDING_CLIENT`**：Say 通道表现（系统消息 vs say 气泡）；Spawn 回收时序；DD 接取
  入口页/页动作全词汇；进区接取（`*_QuestArea_*` 族）实机走查。
