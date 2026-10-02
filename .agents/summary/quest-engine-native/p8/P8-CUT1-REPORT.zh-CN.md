# P8 第一刀报告：台账/编译残类清扫 + 门禁读者重锚 + both-red 门重锚批

- 批次：P8 最终收口第一刀（计划 §7「P8 最终收口」/ §10.2；承接步 f 提交 715a00136）
- 日期：2026-10-02
- 性质：**纯删旧 + 门禁重锚批（零生产行为变更）**——被删类均已无 live-main 消费者（步 f 后逐类核实）。

## 1. 17 个 retail 残类的逐类裁定（全部删除）

**向上闭包核实**：`RetailDataDrivenCollectCompiler` / `RetailClientAcceptNpcSets` 零读者；
`RetailHandinDialogFlowCompiler` 唯一读者为前者；`SimpleCollectItemHandler` 对
`RetailSimpleCollectItemDefinitionCompiler` 的引用仅存于 javadoc（代码零依赖）——整个
SimpleHunt/Collect 编译残片簇（12 主类，2026 行编译器 + 310 行 plan + 81 行 catalog 等）
在步 f 后已无 live-main 路径。

| 簇 | 删除主类 |
|---|---|
| 编译残片 | `RetailSimpleHuntDefinitionCompiler`（1635 行，quest_client_hunt_stages/dialog_exits/reward_npcs/quest_area 读取面随之退场）、`RetailSimpleHuntPlan`、`RetailQuestCatalog`、`RetailSimpleCollectItemDefinitionCompiler` |
| 孤儿编译器 | `RetailDataDrivenCollectCompiler`、`RetailHandinDialogFlowCompiler` |
| client 台账读者 | `RetailClientDialogExits`、`RetailClientSummaryRows`、`RetailClientRewardNpcs`、`RetailClientKillTargets`、`RetailClientHuntProgressRows`、`RetailClientHuntStages`、`RetailClientHandinPages`、`RetailQuestUseItemNpcs`、`RetailQuestAreaIndex`、`RetailEnterAreaZoneResolution`、`RetailClientAcceptNpcSets` |

**retail/ 包 41 → 24 类**；存活类全部有 live-main 消费者
（`RetailClientHandinNpcSets`＝三采集/用物/演出 handler 的多交付展开、`RetailQuestAiNameGroups`＝
组表、`RetailClientUseItemReport`＝用物 report 常量、其余＝名字/标题/配方/表索引面）。

## 2. 台账资源退场（8 个）

`quest_client_accept_npc_sets` / `handin_pages` / `summary_rows` / `hunt_stages` /
`kill_targets` / `kill_targets_stages` / `reward_npcs`（7 个 quest_client_*）+
`quest_enterarea_zone_resolution`（别名解析表随其读者退役——解析现只活在
`NativeEnterAreaPort.create`，登记名实参 fail-closed）。**存活**：
`quest_client_handin_npc_sets.tsv`（live）+ `retail-quest-ai-name-groups.tsv`（真端组台账，live）。
被删台账的全部残余引用仅存 javadoc（逐个 grep 核实）。

## 3. 测试面裁定（20 个消费测试 → 删 6 类 + 移 13 法 + 重锚 10 类）

- **整删 6**（主语 = 已删编译产物）：`RetailSimpleHuntFamilyGateTest`（挑战接取采纳法迁入
  `RetailQuestAiNameGroupGateTest` 同包——其断言全部落在 live 类上；`RetailSimpleHuntEquivalenceGateTest`
  的 IR 指纹法在 native 车道下本就自禁用）、`RetailSimpleHuntEquivalenceGateTest`、
  `RetailHuntClientCountGateTest`、`RetailSimpleHuntPlanEquivalenceTest`、`RetailQuestCatalogTest`、
  `RetailClientSummaryRowsTest`。
- **移除 13 个死主语法**（跨 8 类）：`RetailRewardWindowRouteTest.selectableWindowSlots…`、
  `RetailClientContractConstantsGateTest.dialogExitsKeepCanonicalSelectNoneRows`、
  `RetailQuestDriverOverlayTest` 两个死 helper、`ClientQuestSectionAlignmentTest` 三法（1843/11102/17106）、
  `MigratedQuestRepairDefinitionTest.groupOwners…`（15602）、`QuestA03Shard.pureTalk21065`（文件直读）、
  `QuestDraupnirNpcVariantContractTest` 两法（3532/2631）、`QuestMovieAndDialogLoop` 两法（24155/24202）、
  `RetailSequentialQuestFamilyTest.quest15321…`（文件直读）、`QuestPrerequisite.unported…`（1870/2869/2870 全 DD）。
- **重锚 10 类**（预言机 → live 面）：
  1. `RetailQuestAiNameGroupGateTest`——bindPlan 组展开 = 解析器通道直通调用，绑定面由
     `resolveAllOrQuestAiNameGroup` 断言承担（dispatch 登记面归 `QuestEngineNpcDialogDispatchTest`）；
  2. `RetailSystemGrantDispatchTest`——SimpleHunt 哨兵行重锚为**已登记缺口 fail-closed**
     （§10.3-#25：不得混入 Talk/Collect 聚合面 + owner 交叠恒空 + ai-areas 绑定改测试侧直读
     `ai-areas.xml` quest_area），7/7 全绿（原 2 红）；
  3. `RetailRewardWindowRouteTest`——交付登记预言机 → live `RetailClientHandinNpcSets.defaultSets()`
     （三行探测 35021/35023/35045 与旧台账同值）；
  4. `RetailEnterAreaZoneRegistrationGateTest`——重写为生产同源端口重建
     （retention 切换集 + `DataDrivenNativeRuntime.TABLE_RESOURCE` + zones 登记名）：
     解析面非空 ∧ 每个解析目标 ∈ 登记名 ∧ 冻结缺席集恒等 `RETAIL_ABSENT_ALIASES`；
  5. `QuestPrerequisiteRetailContractTest`——分支覆盖下限 1000→100（XML 域实测 106）、
     PREREQ_BATCH 收窄为 XML 保留行 3 键（2533/3050/21080）、2641 漂移锚随 native 行退役；
  6. `QuestRetailClassGateTest`——native 行（无 typed 载体）在三分册中豁免，非 native 行缺载体仍红；
     **原生行职业轴执行位保持登记**（真端证据批，未见证据不实现）；
  7. `QuestMetadataFieldMappingTest`——目录人口 6224→740（XML 保留行），五轴下限按 740 人口重冻
     （rewards 722/drops 169/collects/workItems/startConditions 实测冻结）；
  8. `QuestReportedRewardCoverageTest`——182 名单逐条「缺失 ∧ 非退役 ⇒ 红」防名单陈旧，
     XML 保留行仍逐条断言；
  9. `ClientQuestSectionAlignmentTest`——残余三法随 IR 主语退场（SECTION_0 闭包由
     `QuestSection0ReportRowContractTest` TSV 形状门 + native 家族门承担）；
  10. `QuestEngineNpcDialogDispatchTest`——核实零代码依赖（仅 javadoc 提及），不动。

## 4. 门态（本批显式命令）

```
# DD 门对
mvn -o test -Dtest='DataDrivenNativeRuntimeGateTest,DataDrivenProgressTest'   # → 18 + 9 全绿
# 家族 + tablelane（RetailSimpleHuntFamilyGateTest 3 例随车道删除，189 → 186）
mvn -o test '-Dtest=*FamilyGateTest,*RowAlignmentGateTest,*InventoryGateTest,NativeQuestTableLoaderTest,
  *RewardClaimGateTest,*NativeTalk*Test,NativeNearbyQuestAxisGateTest,SMNearbyQuestsPacketTest,
  NativeAcceptEntryAskFlowGateTest,DataDrivenNativeContractGateTest,DataDrivenProgressTest,
  DataDrivenQuestTableGateTest,DataDrivenEnterAreaPortGateTest,DataDrivenNativeRuntimeGateTest,
  MultiCellSensoryZoneRegistrationTest,RetailEnterAreaZoneRegistrationGateTest'   # → 186/186 全绿
# 迁移门（presence 4 + overlay 5 + ownership 4 + owner-resolver 6）            # → 19 全绿
# 聚焦套件
mvn -o test '-Dtest=*Quest*Test,*Retail*Test'
# → 1453 例 / 28F+164E / 73 红类；对步 f 基线（v2：1478 / 84 红类 / 161 红例）：
#   类级 ADDED 0 / 测试级 ADDED 0；REMOVED 11 类 = both-red 重锚转绿
#   （prerequisite/class/metadata/reported-reward/ClientQuestSection/Migrated/A03/Draupnir/
#    MovieAndDialogLoop/RetailSequential/SystemGrantDispatch）
```

日志：`gates/2026-10-02-p8-family-tablane.log`、`gates/2026-10-02-focused-run-p8.log`（临时，不入库）。

## 5. 反漂移规则（本批新增）

1. **client* 台账 = 测试侧预言机可用、main 侧禁止**：步 f 后任何 main 类不得引用
   `quest_client_*` 资源；`quest_client_handin_npc_sets.tsv` 是唯一例外（三 handler 的多交付
   展开，P4 裁定）。
2. **删除编译残片前必须做向上闭包核实**：`grep -rl` 反向消费者逐个分类（live-main / test /
   javadoc-only），javadoc 引用不构成保留理由。
3. **预言机随车道退役**：测试引用被删类作 oracle 时，替换物必须是 live 面
   （handler 访问器 / 生产同源端口重建 / 测试侧直读数据文件），禁止复刻旧车道逻辑。

## 6. 第二刀：§10.3-#25 取证 + 接线 + 阵营门禁收口（2026-10-02，已闭环）

详见 `p8/simplehunt-grant-face-adjudication.zh-CN.md`（宿主面逐函数取证 + 轮换表镜像发现）。要点：

- **Faction 轴**（家族无关）：`NpcFactionDB` 7 星期向量载池 → `CheckNewFactionQuest` →
  `GetTodayFactionQuest`（tm_wday）→ 随机选 → `InitFactionQuest`（state=2 + 0xfe7b）→ cab520
  state 分发 → `AddQuest` type 3；`Quest::CanAcquireQuest` 零家族分支、DLL 零哨兵字符串。
- **Area 轴**（家族无关）：`world.xml <questscript_area>` → case 0x894 → `NpcScriptMgr_AddQuestArea`
  → `MoveNew` 入队 → 60 tick 排水 → `User_AddAreaQuest(type 3)`。
- **接线**：`SimpleHuntHandler implements NativeSystemGrantLane`（第三车道六法），
  `NativeSystemGrantLanes` = Talk+Collect+Hunt；`RetailSystemGrantDispatchTest` 翻转 7/7 绿。
- **连带收口（取证发现）**：生产轮换表镜像陈旧 → `npc_factions_quest.xml` 425→436 行对齐真端
  （补 11 全 0 掩码行 + 39713/49713 本地全 1 回正）；契约快照 v2 三列 287 行（+34 已实发未评审行、
  休眠行 until-ported）；`QuestNpcFactionRetailGateTest` 三法重锚绿 + `NpcFactionQuestDataTest`
  425→436。行为影响 = 35027-35030/45027-45030/36514/36517 从「无轮换行即每日可发」回正为真端
  本征「永不发放」。
- **触发器残留登记 §10.3-#26**：`_area_` 行发放面就绪但进区不分发（真端 MoveNew→tick→AddAreaQuest
  链无对应物；Talk 8 + Hunt 17 行），独立批取证接线。

## 7. 第三批：DD ItemPlay 道具可得门重建（2026-10-02，已闭环）

详见 `p8/datadriven-itemplay-grant-gate.zh-CN.md`。`DataDrivenItemPlayGrantGateTest`：路由集 ∩ DD ItemPlay 步 = 42 步 / 37 任务全人口三源对拍（模板索引 / 行内 GIVE ∪ 任务掉落 / 工作物品采集面），18738/28738 = 真端宝箱 NPC 外部登记（登记必须仍被使用）；本门 1/1、DD 门全组 45/45。

## 8. P8 剩余

- **原生行职业轴执行位**（`QuestRetailClassGateTest` 豁免面：真端 class 限制是否为服务端闸门）；
- memory-bank 同步；客户端验收 `PENDING_CLIENT`（Say 气泡 / Spawn 回收 / DD 接取页词汇 /
  进区接取 `*_QuestArea_*` 走查）。
