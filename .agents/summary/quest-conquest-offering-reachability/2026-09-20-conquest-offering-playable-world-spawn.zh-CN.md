# 2026-09-20 15321/25321「消灭征服之祭物」：大师服事件刷怪落到可玩世界

## 现象（用户报障与追问）

玩家做到 15321「凯西内尔之翼能做的事」第 3 段时，客户端任务书显示“消灭征服之祭物”，
但**在普通英吉斯温野外找不到任何该目标怪**，任务无法推进。随后用户追问：
“15321 是不是大师服专属任务？”“5.8 本来的英吉斯温就是大师服地图吗？”

## 证据链（真端 + 客户端 + 服务端）

| 证据 | 内容 |
|---|---|
| 客户端任务合同 | `Quest_unpacked/data_driven_quest.xml` 15321 第 3 个 Hunt 段 = `LF4_Rotation_65_Deva_Q15321_01..07 10;`（10 只）；`quest_monster.csv:4099` 同步 `SECTION_0==5; SECTION_1<10` |
| 客户端命名 | `client_strings_monster.xml` 的 `STR_NPCTitle_LF4_Rotation_Normal_01_01…` 标题 = `<征服之祭物>`；`client_strings_dic_monster.xml` 词典说明“在英吉斯温和格尔克马洛斯中出现” |
| 服务端 NPC | `npc_template_235749_247606.xml`：236307–236334（`…_An`）、236530–236557（`…_An_D`）都是 `LF4_Rotation_Normal_*` 65 级怪 |
| 真端关卡 | `58Server/Map/Worlds/**/world*.xml` 共 844 个文件，`Rotation` 与 group 97–100/109–112 **只出现在 `LF4_M/world.xml`**（8 个 `DirectPortal_StartLF4_Design_4.8_Rotation*`），普通 `lf4/df4` 为 0 |
| 真端可玩世界判定 | `58Server/Map/XML/zonemap_hotspot.xml`、`Subzones/client_zonemap_hotspot.xml`：`<id>72</id><name>HOTSPOT_LF4_01</name><world>LF4</world>`（坐标 1335/275/590 与本仓库 hotspot 完全相同）⇒ 5.8 玩家侧英吉斯温 = **LF4 = 210050000**，镜像图 LF4_M/210130000 是大师服那一张 |
| 服务端刷怪（修复前） | 全目录扫描：236530–236553 只登记在 `spawns/Conquest/210130000_Inggison [Master Server].xml`；236586–236609 只在 `220140000_Gelkmaros [Master Server].xml`；`236307–236334`、`236554–236557` 全服零刷点 |
| 入口 | 210130000/220140000 除 `world_maps.xml`/zones/镜像刷怪/id-mappings 外无任何玩家入口；`a7da0ad67` 又把英吉斯温传送与 Secret Portal 归一化到 210050000（该提交与真端 hotspot 一致，无需回退） |

结论：**任务本体不是大师服专属**（接取 NPC 在希哥尼亚 210070000、其余对话 NPC 在普通世界，5 段击杀也在普通世界），
但第 3 段的击杀目标属于真端“4.8 Rotation / 征服之祭物”事件，刷怪只登记在大师服镜像地图；
单机 emulator 没有大师服入口，因此这一段在修复前不可完成。

## 修复（用户选择：不引入大师服，把该段做成在可玩世界可完成）

新增两个生产刷怪文件，把同一征服活动（conquest id 1/2）的 **CONQUEST 段击杀目标**登记到可玩世界：

| 新文件 | 目标世界 | 内容 | 刷怪点 |
|---|---|---|---|
| `spawns/Conquest/210050000_Inggison.xml` | 210050000 英吉斯温 | conquest id=1，npc 236530–236553（24 种） | 60 |
| `spawns/Conquest/220070000_Gelkmaros.xml` | 220070000 格尔克马洛斯 | conquest id=2，npc 236586–236609（24 种） | 85 |

- 每个 `<spawn>`/`<spot>` 块**逐字复制**自对应镜像文件（坐标系一致，已验证 `verbatim_parity=True`）。
- 未复制 7 个非任务 NPC（`7027xx` portal_dialog、`8330xx` general），文件头注释已列明。
- **未改动**事件排程 `conquest_schedule.xml`、`ConquestService`/`Offering` 状态机、魔法器与 Master 地图原刷怪。

因此英吉斯温/格尔克马洛斯会在原有活动时段（每天 13/18/23/4/9 点，每次 `gameserver.conquest.duration=1` 小时）
刷出征服之祭物；`//conquest start 1|2` 可即时开启活动用于验证。

## 回归测试

新增 `src/test/java/com/aionemu/gameserver/dataholders/QuestConquestOfferingSpawnDataTest.java`（3 例）：

- `inggisonEventSpawnsQuest15321TargetsInPlayableWorld`：断言刷怪世界 = 210050000、刷出 NPC 全部属于 15321 的 `kill-npc` 集合、
  是镜像事件集合的子集、≥10 种且 ≥10 个点位、conquest id 1 仍在排程表内。
- `gelkmarosEventSpawnsQuest25321TargetsInPlayableWorld`：对 25321 / 220070000 / conquest id 2 做同样断言（同族，避免只修报障任务）。
- `productionSpawnLoaderIndexesPlayableWorldConquestGroups`：走真实 `SpawnsData2.load`（JAXB + afterUnmarshal 索引）加载 Conquest 目录，
  断言 conquest id 1/2 同时索引到可玩世界与镜像世界，且可玩世界下确实索引到 236530 / 236586。

## 同类审计（系统性，未修）

审计脚本：`audit_master_server_spawn_coverage.py`（本目录）。当前各刷怪类别“可玩世界/镜像世界”文件数：

| 类别 | Inggison live/master | Gelkmaros live/master |
|---|---|---|
| Beritra | 1 / 1 | 1 / 1 |
| Conquest（本次修复） | 1 / 1 | 1 / 1 |
| Gather | 1 / 1 | 1 / 1 |
| Live_Party_Concert_Hall | **0 / 1** | **0 / 1** |
| Nightmare_Circus | **0 / 1** | **0 / 1** |
| Npcs | 1 / 2 | 1 / 1 |
| Outpost | 1 / 1 | 1 / 1 |
| Rifts | 1 / 1 | 1 / 1 |
| Sieges | **0 / 1** | **0 / 1** |
| Zorshiv_Dredgion | **0 / 1** | 0 / 0 |

即 `Live_Party_Concert_Hall`、`Nightmare_Circus`、`Sieges`、`Zorshiv_Dredgion` 同样是“只在镜像世界声明刷怪”，
在单机 emulator 下不可达；是否按本次模式迁移需要单独决策（不在本次范围）。

## 验证状态

已执行（静态）：

- `xmllint --noout --schema src/main/resources/aion/data/static_data/spawns/spawns.xsd <两个新文件>` → validates；
- 逐字一致性校验（新文件 24 个 `<spawn>` 块与镜像源逐字节相同）；
- `python3 .agents/summary/quest-conquest-offering-reachability/audit_master_server_spawn_coverage.py` → exit 0；
- `git diff --check` → 无空白问题。

已执行（测试，2026-09-20 用户授权后）：

- `rtk mvn -q -Dtest=QuestConquestOfferingSpawnDataTest,QuestCounterProjectionLockFollowUpTest,IncrementVariableDefinitionTest,QuestMutationPlannerTest,RetailSequentialQuestFamilyTest,QuestMonsterProgressContractAuditTest,QuestCounterSourceProjectionProductionFlowTest,QuestResidualCounterLocksTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest -DfailIfNoSpecifiedTests=false test` → EXIT=0；
  其中 `QuestConquestOfferingSpawnDataTest` 3 例 0 失败 0 错误；生产目录/白名单门禁 `PRODUCTION_COMPILE_OK=6189`、
  `PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。

**仍待执行**：

- 实机/客户端验收：需要在活动时段（或 GM `//conquest start 1`）进入英吉斯温确认怪出现并可完成 10 杀。

## 未决风险

1. 击杀事件“供奉 BOSS”判定取 id 区间内已刷出的第一个实体（`ConquestOffering#initConquestBoss`），
   现在该实体位于可玩世界；击杀它会按原逻辑提前结束活动（与镜像图行为一致，未新增机制）。
2. 236307–236334（`…_An`）与 236554–236557 仍无任何刷点：它们是真端动态生成、单机 emulator 未实现的“普通版”轮换怪；
   本次未给它们补点，任务靠 `…_An_D` 一支即可完成（客户端任务合同两支都接受）。
3. 本修复属“内容可达性适配”，不是真端结构复刻（真端该段需进大师服）；若要真端对齐需另立大师服入口方案。
