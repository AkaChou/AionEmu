# P7 步 2 步 c 报告：DD `EnterArea` 轴——真端同名区解析端口 + 逐行裁定（零行为变更）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 批次：P7 DataDriven 步 2 步 c（计划 §7「P7 DataDriven」/ §10.2 / §10.3-#22）
- 日期：2026-10-01
- 结论：**DD 进区绑定 = 真端同名区**（名哈希逐值比对，无任何名字换算规则）；切换集 120 个 `EnterArea` 别名全部逐行裁定，
  其中 105 个解析到真端区几何（91 进程 + 14 接取），15 个真端副本无定义 ⇒ fail-closed 冻结并登记（新增 §10.3-#23）。
  本批不发运行期分流（随 DD 原生 handler 同批接线），**零行为变更**。

## 1. 真端事实（原码 + 世界文件）

| 事实 | 证据 |
|---|---|
| 进区 handler `FUN_180c47bf0` 用 `FUN_1810798b0`（0x1003F 名哈希）把**当前步别名哈希**与**进区区名哈希**比对 ⇒ 绑定 = **同名**，无换算规则 | `ScriptDLL64.c:2071405`（`iVar3 = FUN_1810798b0(lVar2 + 8)` 对 `vtbl[0x50]()`）；步 kind 6 = EnterArea 的注册在 `:2076100` 一带 |
| 感官区 = 世界文件里的**同名 NPC**（`<npc><name>X</name><sensory_area><points>…<top>/<bottom>`），多胞区 = 多个同名 `<npc>` | `<真端根>/Map/Worlds/IDAb1_Ere/world.xml`（Q10011a 四点多边形 + top 446.87/bottom 416.87） |
| 任务脚本区 = `<questscript_area><name>X</name><quest>id,…</quest><points_info>…` ⇒ **真端自带 quest 绑定**，与世界文件里的区名无需逐字相同 | `<真端根>/Map/Worlds/IDEternity_War/world_N.xml`（`QuestArea_Q13965a` 绑 13965/23965，`QuestArea_Q13965` 绑 13975/23975） |
| 世界目录名 → mapid 的仓内既存映射（客户端派生） | `src/main/resources/aion/definitions/compact/id-mappings.xml`（`id/worldid.xml`，233 条） |
| 客户端侧同一别名以 SP NPC 出现（只有位点 + `sensory_range`，无多边形） | `Levels/IDAb1_Ere/Level.pak` → `mission_mission0.xml`（`SPQuest_NPC32 npc="IDAb1_Ere_SensoryArea_Q10011a" Pos="238.5,748.1,426.5"`） |

## 2. 逐行裁定（切换集 = 1467 行）

工具：`p7/tools/enterarea-retail-zone-probe.py`（复算 + `--emit-zones`），台账：`p7/tools/qe-enterarea-retail-zone-resolution.tsv`（120 行 + 表头）。

| 轴 | 别名数 | 真端可解析 | 真端无定义 |
|---|---|---|---|
| `progress`（进程步，本批落地） | 105 | **91** | 14 |
| `acquire`（接取 kind，随接取批接线） | 15 | 14 | 1 |
| 合计 | 120 | 105 | 15 |

解析级联（命中即止，全部在探针里可复算）：

- **R1 真端同名**：101 条（感官区 91 + 任务脚本区 10）。
- **R3 任务脚本区按 `<quest>` 绑定**：4 条（`IDEternity_War_QuestArea_Q1396x/Q23965` —— 别名只带世界前缀，
  真端区名是 `QuestArea_Q13965a…`，且 `QuestArea_Q13965` 实际绑 13975/23975 ⇒ **只按名字形会错绑**，绑定优先）。
- **R4 fail-closed**：15 条（见 §4）。

其它口径数字：

- 注册胞数：progress 轴 **120 胞**（91 区，其中 15 个多胞区共 44 胞）；acquire 轴 14 胞。
- 覆盖世界目录：19 个（`df5`/`df6`/`IDEternity_02`/`IDEternity_03`/`LF5`/`IDAb1_Ere` …）。
- 与仓内遗留壳的**几何交叉核对**（仅核对，不作解析依据）：56 个别名在同名归一规则下能找到遗留区，
  其中 35 条胞数一致、**30 条遗留壳是「出生点 + r=10 球体」→ 0 胞**（真端多边形取代之），0 条其它差异。

## 3. 落地物

| 交付 | 说明 |
|---|---|
| `src/main/java/.../questEngine/tablelane/NativeEnterAreaPort.java` | 进区轴原生端口：恒等解析（别名 → 同名注册区）+ fail-closed（未登记别名抛 `DATA_DRIVEN_ENTER_AREA_ZONE_UNRESOLVED`）+ 冻结常量 `RETAIL_ABSENT_ALIASES`（14 条） |
| `src/main/resources/aion/data/static_data/zones/zones_retail_enterarea.xml` | 91 条 `POLYGON`/`SUB` 区（**区名 = DD 别名**），坐标与 `top/bottom` 逐字取自真端世界文件；由探针 `--emit-zones` 生成，禁止手改 |
| `src/test/resources/quest/retail-enterarea-zone-resolution.tsv` | 逐行冻结台账（生产工具产物副本） |
| `src/test/java/.../tablelane/DataDrivenEnterAreaPortGateTest.java` | 新门 6 例：schema / 逐行裁定闭合 / 真端几何逐字（胞数+mapid+摘要）/ 禁近似补与禁死数据 / 恒等解析（不是遗留壳名）/ 未登记 fail-closed |

**同区双名（过渡形态）**：本批新增的区名 = DD 别名（真端名）；旧 Encom 壳名（如 `IDAB1_ERE_Q10011_A_302340000`、
`LF6_SENSORY_AREA_Q15551_A_TO_B_210100000`）与 `quest_enterarea_zone_resolution.tsv` 仍在，供**遗留 IR 车道**
消费，随 P7 步 2 末批（车道删除）一并退场。两套名字并存期间互不影响（不同 `ZoneName`，事件各发一次）。

## 4. 真端副本缺口（fail-closed 冻结，新登记 §10.3-#23）

14 条 `progress` 别名全部落在 **LF6（Elyos）侧**，且**镜像的 DF6（Asmodian）侧在真端有完整多边形**：

| 形态 | 别名 | 真端镜像证据 |
|---|---|---|
| 场地引导 A↔B（8） | `LF6_SensoryArea_Q1555{1..4}_{AtoB,BtoA,AtoD,DtoA,AtoF,FtoA,AtoH,HtoA}` | `df6/world_N.xml` 有 `DF6_SensoryArea_Q2555x_*`（含多边形与 top/bottom） |
| 动态环境（5） | `LF6_SensoryArea_Q1560{1a,1b,2a,4a,5a}_…` | 同上（`DF6_SensoryArea_Q2560x_*`） |
| 具名（1） | `LF6_SensoryArea_Q15608a_Dynamic_Env` | 同上 |

- 真端扫描口径：256 个世界目录 × `world.xml` / `world_M.xml` / `world_N.xml` 全量 `<sensory_area>`（407 名字 / 464 名字-世界对）
  与 `<questscript_area>` / `<item_use_area>`；LF6 侧只有 `LF6_SensoryArea_Q25673` 与绑定 quest 25674 的 questscript 区。
- 客户端侧只有 SP NPC 位点 + `sensory_range = 10`（`npcs_unpacked/client_npcs_npc.xml` id 206516 等），**无多边形**。
- 处置：**不注册、不猜几何**（禁"出生点 + 半径"近似复活），冻结在端口常量与门里；待取到真端 LF6 完整世界文件
  或客户端区域几何后单独立批接线。同一批同时冻结 `acquire` 轴的 `DF6_QuestArea_Q25674`（真端同名区只在 LF6 世界
  以 `LF6_QuestArea_Q25674` 出现，与别名世界前缀冲突 ⇒ 不得跨世界搬几何）。

## 5. 门态（本批显式命令）

```
# 新门
mvn -o test -Dtest=DataDrivenEnterAreaPortGateTest -DfailIfNoTests=false          # 6/6 绿
# 族门 + tablelane（含 zone 注册两门）
mvn -o test '-Dtest=*FamilyGateTest,*RowAlignmentGateTest,*InventoryGateTest,NativeQuestTableLoaderTest,*RewardClaimGateTest,*NativeTalk*Test,NativeNearbyQuestAxisGateTest,SMNearbyQuestsPacketTest,NativeAcceptEntryAskFlowGateTest,DataDrivenNativeContractGateTest,DataDrivenProgressTest,DataDrivenQuestTableGateTest,DataDrivenEnterAreaPortGateTest,MultiCellSensoryZoneRegistrationTest,RetailEnterAreaZoneRegistrationGateTest' -DfailIfNoTests=false
# → 170/170 绿（上一批 157/157 + 新门 6 + zone 两门 7）
# 聚焦套件
mvn -o test '-Dtest=*Quest*Test,*Retail*Test' -DfailIfNoTests=false
# → 1703 / 161F+137E / 105 红类，与 P7 步 2 步 b 基线逐类相同（ADDED 0 / REMOVED 0 / changed 0）
```

日志：`gates/2026-10-01-p7-step2c-family-tablane.log`、`gates/2026-10-01-focused-run-p7-step2c{,-red-classes,-delta}.tsv`。

## 6. 反漂移规则（本批新增）

1. **进区绑定只许同名**：任何"别名 → 区名"的换算规则（大小写归一、去前缀、`AtoB`→`A_TO_B`、`SENSORYAREA`→`SENSORY_AREA`）
   都不得进入运行期解析；旧壳名只许存在于即将删除的遗留车道台账里。
2. **几何逐字来自真端**：生成器只做"誊抄 + 归并同名多胞"，禁止插值、禁止取整、禁止用球体/位点近似；
   门内以**原文摘要**（`bottom|top|点列` 的 sha256 前 16 位）+ 胞数 + mapid 三重冻结。
3. **两轴分开**：`progress` 与 `acquire` 别名集合互斥（本批实测 105/15，无交集）；接取轴的区数据与接线随接取批落盘，
   本批不得预置死数据（门内有断言）。
4. **真端缺席就是缺席**：真端世界文件没有定义 ⇒ 冻结 + 登记，不得用客户端 SP NPC 位点补几何。

## 7. 后续（P7 步 2 剩余）

- 步 d：DD 原生 handler 的 8 类 progress 运行时（Hunt/CollectItem/PvP/Talk/EnterArea/ItemPlay/EnterWorld/TalkFOBJ），
  其中本批的进区轴只需把 `NativeEnterAreaPort` 的区名注册进 `QuestEngine` 进区兴趣 + 原生分流；
- 步 e：接取轴 6 类（含 `acquire` 15 别名）+ 通用附加动作执行面；
- 步 f：1467 行原子切换 + 同批删 `RetailDataDriven*Compiler` / `RetailEnterAreaZoneResolution` /
  `RetailQuestAiNameGroups` / typed `RetailClientAcceptEntryPage` / 旧壳区名 + `quest_enterarea_zone_resolution.tsv`。
