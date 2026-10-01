# P4 报告：SimpleCollectItem 族原生直驱切换（槽序纠偏 + 多交付 NPC 展开 + 同批删旧）

> 主题：按计划 §7 P4 把 SimpleCollectItem 族（262 行）整体切到真端表/真端 `quest.xml` 直驱，
> 与旧 IR 车道一刀两断（同批删旧），并就地坐实两处真端事实：**相机槽序**与**复合交付名**。
> 日期：2026-10-01。分支：`quest`。
> 前置：`p1/P1-TRANCH1..3`、`p2/P2`、`p3/P3-STEP1..6-REPORT.zh-CN.md`。
> 口径：本族一切行为只读真端表 `Map/XML/Quest_SimpleCollectItem.xml` + `Map/XML/quest.xml` +
> 仓库静态数据；不生成 IR 节点、不回退旧编译器、不发明 id/页/槽位；未决一律 fail-closed。

---

## 1. 交付：262 行装载 + `SimpleCollectItemHandler` 原生直驱

| 组件 | 变更 |
|---|---|
| `tablelane/NativeQuestTableLoader` | 新增 `collectRows()`：真端 `Quest_SimpleCollectItem.xml` **262 行**逐行装载（`dev_name` / `acquired_npc_name` / `reward_npc_name` / `give_item` / `give_item1` / `remove_item2` / `con_quest` / `cutsceneid1` / `cs1_haction` / `party_drop` / `object1..4` / `talk_npc1..3`），字段**逐字**保留原文 |
| `tablelane/SimpleCollectItemHandler`（新增） | 全族唯一处理器：① 接取 = 接取 NPC 的 31 问询 → 客户端契约入口页，1002/20000 经 `NativeQuestStartPort` 建档 START 并按 `give_item` 原样发放；② 中继链 = `talk_npc1..3` **严格按表序**推进（乱序/重复零推进，步位在 raw vars 16..17）；③ 采集 = 点击对象/击杀 `drop_monster_N` 推进**同一个**相机槽；④ 交付 = 交付 NPC 集合内按持有量扣物 → `REWARD` + 页 5，未满足 → 页 10；⑤ 领奖 = `NativeReportRewardFlow`（NPC 域 108 / 8..23 / 110..124）→ 页 1008 |
| `tablelane/NativeCollectSpecs`（新增） | 相机 `required` = 真端 `quest.xml collect_itemN` 的计数（`item 名 数量` 单元），逐行派生 |
| `tablelane/CameraRegistry` | 本族 250 行相机行注册（3 行真端休眠行不派生） |
| `tablelane/NativeQuestOwnerResolver` | collect 行 owner 面接入（注册集 ≠ 路由集） |
| `QuestEngine` | `onDialog` / `onKill` / `onCanAct` / `hasMatchingRoutes` 原生分流 + 启动期 `installInterest`（采集对象、掉落怪、中继 NPC、交付 NPC 逐点注册） |
| `retail/RetailQuestDriver` | **切断** collect 旧编译入口（删 `compileSimpleCollectItem` / `simpleCollectItemTable`），262 行 100% 移出旧 IR |

**同批删旧**：`git rm` 旧金标 `RetailSimpleCollectItemGateTest`（760 行）与 `retail/RetailSimpleCollectItemTable`；
`RetailSimpleCollectItemDefinitionCompiler` 家族编译入口（`compile` / `precheck` / `build` / BriefingStep /
`canonicalBriefingFlow`）删除，仅保留仍被 DataDriven / Handin 复用的共享片段
（`setproRoute` / `journalRowRepair` / `canonicalAcceptFlow` / `deliveryWindowPage` / `canonicalDelivery` /
`reportNpcExit` / `completeFlow` / `talk`），类注释已标注**随 P7 退场**。

## 2. 槽序纠偏（本轮最关键的真端事实修正）

真端相机槽**不是** `objectN` 的列序，而是：

```
drop_monster_K  （来源；对象或真怪，可空格并列多来源）
      ↓ 产出
drop_item_K     （掉落物）
      ↓ 在交付列里的位置
collect_itemN   （N = 槽位；required = 该单元计数）
```

反例（真端数据，任一都足以否证「按对象列序取槽」）：

| 行 | 事实 |
|---|---|
| 4046 | `object1` 而非 `collect_item1`（交付列第 4 项） |
| 2487 | `object1` 但掉落列是第 2 列；槽 1 的来源是真怪 `Pretor_38_An` |
| 2346 / 41216 | 槽 1 来源是真怪 / 另一个 FOBJ（对象列只是线索） |
| 1154 / 41510 | 一个掉落列并列多个来源，全部灌同一槽 |

全表复算（真端表 × `quest.xml`）：`objectK` 与 `drop_monster_K` 同名 **275** 列、仅大小写差异 **8** 列、
一个掉落列列多个来源 **3** 列、掉落源是真怪 **3** 列。`SimpleCollectItemHandler.dropSourceSlots(...)`
统一取槽，**对象与击杀同源**；对象不在任何掉落列（41216）或掉落物不在交付列 ⇒ `unroutable`
fail-closed，不按列序猜槽。

门：`SimpleCollectItemRowAlignmentGateTest` **5/5**（独立正则重解析真端表 + `quest.xml`，不复用装载器
DOM 路径；逐行字段/相机 required/槽对齐 + 全表计数冻结 262 · 300 对象列 · talk 5/2/1 · party_drop 80 ·
con_quest 37 · cutscene 2 · give_item 6 · give_item1 1 · remove_item2 1 · reward_check 5 ·
collect_item 253/27/13 · 相机 250 · 休眠 3 · 路由集 246）。

## 3. 多交付 NPC 展开 + 复合名 fail-closed

- 248 个可路由行里 **22 行** `reward_npc_name` 是复合逻辑名（`LDF5b_Silverlin_LD`、`LF4_GuardianOfDivine`
  等），静态数据无此全名 ⇒ 按**客户端交付登记**（`quest_client_handin_npc_sets.tsv`）逐元素展开，
  其中 **20 行**为多交付 NPC（最多 3 个：39709）；
- **2 行**（39611 / 49611，`LDF5b_Silverlin_LD`）既无静态名命中、也无客户端登记 ⇒ **交付面不存在**，
  fail-closed **不路由**；真端证据：两行 `minlevel/maxlevel/client_level` 全 `999`（停用行），
  退役编译器同判 `RETAIL_REWARD_NPC_FACTION_COMPOSITE`，P3 SimpleTalk 车道对同名亦按 `DATA_GAP` 冻结；
- 注册/路由分解（门禁冻结）：**owns 262** = **routed 246** + 不可路由 16
  （XML-only 1 = 2237；真端休眠 3 = 36017/46017/47112；无采集计数 9 = 9649/9650/9654/9655/9656/
  50017/50018/51017/51018；对象无槽 1 = 41216；复合无登记 2 = 39611/49611）。

## 4. native 系统发放聚合车道（QE-116）

| 组件 | 变更 |
|---|---|
| `tablelane/NativeSystemGrantLane`（新增） | 发放车道接口：`grantKind` / `factionId` / `isSystemGranted` / `factionRotationCandidates` / `factionRotationEligible` / `grantSystemStart` |
| `tablelane/NativeSystemGrantLanes`（新增） | 聚合入口（`lanes() = Talk + CollectItem`、`laneOf`、`ownershipConflicts()` fail-fast） |
| `tablelane/NativeFactionRotation`（新增） | 阵营日常轮换资格的真端 `quest.xml` 轴判定（Talk/Collect 共用，删掉两份副本） |
| `NpcFactions` | 阵营日常分配**只认聚合入口**：切一族不再孤立该族的哨兵行 |

本族 43 行 `_faction_` 系统发放行经该口接入（接受/建档/轮换资格与 SimpleTalk 同形）。

## 5. typed 目录退出后的跨族旧金标重锚

collect 行退出 typed 目录后，三个仍按 typed 形状断言的跨族门按真端面重锚：

| 类 | 处置 | 门态 |
|---|---|---|
| `RetailClientAcceptEntryPageTest` | 原生车道分支纳入 SimpleCollectItem（入口页 = 客户端契约页；1012/1013 翻页由处理器原样回发） | 4 例 1F（80817 旧债，与基线一致，0E） |
| `RetailRewardWindowRouteTest` | typed 形状断言只留未切换家族（CombineTask 5000）；native 三族改断**客户端登记交付集 + 车道归属 + 采集族 native 交付面逐元素相等**（native 不产生 quest 域全局 AUTO_REWARD ⇒ 无 `AMBIGUOUS_TRANSITION` 风险） | 5/5 **由红转绿** |
| `PlayerQuestStartEligibilityPortTest` | `metadata()` 对 native 行直取真端 `quest.xml` 规范元数据（与 native 领奖口同一条编译器），不再读退场 IR | 16 例 1F + **0E（E-4）** |

## 6. 门禁与实测

| 门 | 命令 | 结果 |
|---|---|---|
| 族门（全部已切换家族 + tablelane 基础设施） | `mvn -o test -Dtest=SimpleCollectItemNativeFamilyGateTest,SimpleCollectItemRowAlignmentGateTest,SimpleTalkNativeFamilyGateTest,SimpleTalkRowAlignmentGateTest,SimpleHuntNativeFamilyGateTest,SimpleHuntHandlerTest,SimpleSerialHuntNativeFamilyGateTest,TableSourceProvenanceGateTest,NativeQuestTableLoaderTest,NativeQuestXmlTableTest,NativeQuestOwnerResolverTest,NativeQuestRewardClaimGateTest,NativeQuestStartPortTest,NativeNpcNameResolverTest,CameraRegistryTest,HtmlPagesRegistryTest,ProgressCameraTest,RawQuestVarsCodecTest -DfailIfNoTests=false` | **114/114 绿**（日志 `gates/2026-10-01-p4-family-tablane.log`） |
| 本族族门 | `-Dtest=SimpleCollectItemNativeFamilyGateTest,SimpleCollectItemRowAlignmentGateTest` | **12/12 + 5/5** |
| 重锚类 | `Quest18501InteractionObjectTest` / `Quest3734DragonArmsChestTest` / `QuestHaramelItemCollectingRegressionTest` / `QuestRewardItemGateTest` / `RetailQuestDriverOverlayTest` / `QuestInteractionObjectContractGateTest` | 2/2 · 2/2 · 2/2 · 3/3 · 5/5 · 2/2 |
| 聚焦套件 | `mvn -o test '-Dtest=*Quest*Test,*Retail*Test' -DfailIfNoTests=false` | **1695 例 / 163F + 145E / 2 skipped / 109 红类**；对 P3 步骤 6 基线（1688 / 163F + 151E / 110 类）**ADDED 0 / REMOVED 1**（`RetailRewardWindowRouteTest` 转绿），逐类差集见 `p4/p4-final-red-class-delta.tsv` |

**覆盖盲点（已登记）**：聚焦模式的 `*Quest*Test,*Retail*Test` **不匹配** `Simple*FamilyGateTest` /
`Simple*HandlerTest` 这类类名 ⇒ 每批必须额外跑上面的显式族门命令，不能用聚焦套件绿替代族门绿。

## 7. 边界与残余（P4 退出前置）

1. **cutscene 未接线**：真端表 2 行声明 `cutsceneid1`（18501 / 28501 = 456，`cs1_haction` 1012），
   collect 处理器尚未播片；按真端 `0x1e` thunk 接线属后续步骤。
2. **`con_quest` 未接线**：真端表 37 行声明链式接取窗（1103/1219/1411/… 含 18506/28506 家族），
   与 P3 同类（阻塞 §10.3-#10），collect 族同样未消费。
3. **旧编译器共享片段**：`RetailSimpleCollectItemDefinitionCompiler` 仅剩 8 个共享片段，已被
   DataDriven / Handin 引用 ⇒ 必须随 **P7 DataDriven** 原子删除，本批不删（否则破共享编译单元）。
4. **客户端验收 `PENDING_CLIENT`**：本批无服务端启动、无真机客户端跑图 ⇒ 18501/3734/2487 等代表行
   的真实客户端验收未做（§7 批门第 7 条未满足，P4 只算「实现完成、验收待跑」）。
5. **不在本批**：QE-112 在飞切片（`RetailSimpleHuntDefinitionCompiler` 等 7 个混合类）与 80817 等
   DataDriven 旧债；均未触碰。

## 8. 证据文件

- 计划：`../2026-10-01-真端引擎迁移计划.zh-CN.md`（§7 P4 行、§10.1/§10.2/§10.3）
- 门禁日志：`../gates/2026-10-01-p4-family-tablane.log`、`../gates/2026-10-01-focused-run-p4-final.log`
- 红类清单/差集：`../gates/2026-10-01-focused-run-p4-final-red-classes.tsv`、`p4-final-red-class-delta.tsv`
- 工具：`p4/tools/collect_feasibility.py`（真端表可达性/槽序复算）、`p4/tools/red_class_delta.py`（红类逐类差集）
- 真端源：`<真端根>/Map/XML/Quest_SimpleCollectItem.xml`、`<真端根>/Map/XML/quest.xml`
