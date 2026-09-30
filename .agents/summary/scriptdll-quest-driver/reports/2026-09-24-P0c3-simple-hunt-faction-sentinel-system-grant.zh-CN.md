# P0c-3：SimpleHunt `_faction_` 类别哨兵行 → 系统发放形状（62 行落地 + 62 个 XML 退役）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 前置：P0c（`RetailSystemGrantDispatcher` + `NpcFactions` 接线）与 P0c-2（共享 `RetailGrantKind`、
> 客户端交付 NPC 登记表扩展、系统发放形状在 SimpleTalk 落地）。本切片把同一形状推广到 **SimpleHunt**。

## 1. 结论

1. 真端 `Quest_SimpleHunt.xml` 的类别哨兵行共 **235**：`_faction_` **127**（宇宙内 **62**）、
   `_challengetask_` 87（宇宙内 84）、`_area_` 21（宇宙内 8）。
2. 宇宙内 62 行 `_faction_` **全部可承接**：16 行报告名是真 NPC 名、46 行是复合势力引用
   （`LF4_GuardianOfDivine` / `LDF5b_Silverlin_LD` / `DF4_GuardianOfTower` …），后者 **46/46**
   都能由客户端任务书 dic 链解出交付 NPC 集；62 行击杀槽位全部可解析。
3. 真端 `npcfactions_quest.xml` 覆盖 125/127，其中 **89 行星期位全 0**（永不发放）；生产表把其中的
   **26 行宇宙内任务写成了全 1** → 本切片按真端修正（与 M5-b3x 的 CollectItem 判例同口径）。
4. 合并复核还抓出 **P0c-2 的 4 行遗漏**（SimpleTalk 39603/39610/49603/49610 同样"真端全 0 / 生产全 1"），
   本切片一并修正 → 已退役哨兵行 **102 行（40 + 62）** 的阵营接线四层一致、星期位与真端一致。
5. 落地 62 行并退役 62 个 XML：**catalog 3641 → 3579**、**RETAIL_TABLE 2583 → 2645**、
   SimpleHunt 可合成 764 → **826**（942 行族内）；`verify_retirement.py` OK。
6. 未落地：`_challengetask_` 84 行（本服无受理入口）、`_area_` 8 行（区域发放数据待接线，P0c-4）。

## 2. 普查（`p0c3_simple_hunt_sentinel_census.py` → `p0c3-simple-hunt-sentinel-census.tsv`）

| 轴 | `_faction_`（127） | `_challengetask_`（87） | `_area_`（21） |
|---|---|---|---|
| 宇宙内 | **62** | 84 | 8 |
| 报告名唯一可解 | 66 | 87 | 21 |
| 报告名复合引用（未解） | 61（宇宙内 46） | 0 | 0 |
| 击杀槽位全可解 | 123 | 77 | 19 |
| 真端星期位 | 全0 **89** / 非全0 36 / 未登记 2 | 未登记 | 未登记 |
| 生产星期位（修正前） | 全0 61 / 非全0 62 / 未登记 4 | 未登记 | 未登记 |
| 生产 × 真端 不一致 | **28**（宇宙内 26 = 真端全0但生产全1） | — | — |

## 3. 客户端 dic 链（`p0c3_simple_hunt_reward_npc_probe.py`）

宇宙内 46 行复合报告引用 **46/46** 解出交付 NPC 集（2–3 个支部 NPC；共享 dic 如 `35021`/`36500`/
`LDF5a_Silverlin_BL`/`LDF5b_Greenhat_BA` 等）。登记表 `quest_client_reward_npcs.tsv`
（生成器族 = CollectItem 全族 ∪ SimpleTalk 复合行 ∪ SimpleHunt 复合行）：family 231 → **294**，
registered 69 → **118**（SimpleHunt 部分 +49；宇宙内 46 行全覆盖）。

## 4. 变化

### 4.1 代码

| 文件 | 变化 |
|---|---|
| `retail/RetailSimpleHuntTable.java` | `Entry` 增 `grantKind`（接取名分类，复用 `RetailGrantKind`） |
| `retail/RetailSimpleHuntPlan.java` | 计划携带 `grantKind`；6 参/8 参兼容构造器分别按 `NPC` 与名字形态推导 |
| `retail/RetailSimpleHuntDefinitionCompiler.java` | ①`compile(plan, metadata, clientRewardNpcs)`；②接取门按类别分流（`_faction_` 放行 / `_area_` → `..._AREA_PENDING` / `_challengetask_` → `..._NO_GRANT_PATH` / 未知哨兵 → 原码）；③报告门支持复合势力引用（客户端登记缺失 → `RETAIL_REWARD_NPC_FACTION_COMPOSITE`）；④`build()` 生成系统发放形状：**零接取路由** + 单条 `SystemGrant` 边（NONE → 零段网格节点）+ 逐交付 NPC 展开报告/完成；⑤`compileSerialChain` 显式关闭系统发放形状（P3 家族判据不变） |
| `retail/RetailSimpleSerialHuntTable.java` | Entry 构造点补 `grantKind` |
| `retail/RetailQuestDriver.java` | 传入 `clientRewardNpcs` |
| `data/static_data/npc_factions/npc_factions_quest.xml` | **26 + 4 行**星期位按真端修正（26 SimpleHunt + 4 SimpleTalk，全 1 → 全 0） |

### 4.2 裁定与门禁

| 资源/门禁 | 内容 |
|---|---|
| `retail-simple-hunt-adjudicated-decisions.tsv`（新） | 62 行逐任务裁定：**ADOPT_RETAIL**（36 = `FACTION_GRANT_DAILY`，26 = `FACTION_GRANT_DISABLED`），含真端/生产星期位、阵营、交付 NPC 来源等证据 |
| `retail-simple-hunt-adjudicated-ir-fingerprints.tsv`（新） | 62 行真端侧冻结 IR（裁定行不主张 XML 等价，改用真端侧防漂移） |
| `RetailSimpleHuntEquivalenceGateTest` | ①XML 等价对拍跳过裁定行，并断言"裁定 ⇒ 已退役 + 真端可合成"；②指纹门禁接受裁定指纹，断言"冻结集合 = 裁定集合"、两登记表不相交、退役 ⇔ 冻结 |
| `RetailSystemGrantDispatchTest` | 新增 2 例：已退役 SimpleHunt `_faction_` 行（≥62）必须带 `SystemGrant` 边；`_area_` 行本轮不得被系统发放 |
| `RetailSimpleHuntFamilyGateTest` | 拒绝登记表重算（178 → **116** 行），族内不变量仍绿（见 §6） |
| `m2e_retire_migrated_xml.py` | 集合改为"XML 等价 286 ∪ 裁定 62 = 348" |

### 4.3 退役

- 保留清单重算：`RETAIL_TABLE/OK` 2583 → **2645**；`SEMANTIC_GAP` 1344 → **1282**；合计仍 6224。
- `m2e_retire_migrated_xml.py`：`migrated=348 moved=62 already_retired=286 missing=0`，
  catalog 条目 3641 → **3579**（62 个 `quests/<id>.xml` 删除，内容由 git 历史承担）。
- `verify_retirement.py`：`catalog=3579 directory=3579 retired=2645 sum=6224 — OK`。

退役清单（62）：35009 35016 35023 35035–35038 35043 35044 35049–35051、36502 36508 36521 36523 36526、
39604 39607 39609 39612 39613 39615、39702 39706 39707 39710–39712 39715、45023 45049–45051、
46502 46508 46514 46520 46522 46523 46527、49604 49607 49609 49612 49613 49615、49702 49706 49707 49710–49712 49715。

## 5. 未落地（带精确原因）

| 行 | 数量 | 稳定码 | 缺口 |
|---|---|---|---|
| SimpleHunt `_challengetask_`（宇宙内） | 84 | `RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH` | 本服挑战任务系统无受理入口（只有完成回调） |
| SimpleHunt `_area_`（宇宙内） | 8 | `RETAIL_ACQUIRE_NPC_SENTINEL_AREA_PENDING` | 生产 `ai-areas.xml` 的 quest_area 覆盖不足（P0c-4） |
| `_area_` 宇宙外 | 13 | — | 真端有行、本服无任务 |
| 计数超 6 位 / 怪名未解 | 13 + 11 | `RETAIL_COUNTER_EXCEEDS_6BIT` / `RETAIL_MONSTER_UNRESOLVED` | 真端数据本身缺口（沿用旧登记） |

## 6. 验证

| 档 | 选择器 | 结果 |
|---|---|---|
| 家族门禁 | `RetailSimpleHuntFamilyGateTest`（942 行族不变量） | 见 §6.1 |
| 等价门禁 | `RetailSimpleHuntEquivalenceGateTest`（等价对拍 + 冻结指纹） | 见 §6.1 |
| 接线门禁 | `RetailSystemGrantDispatchTest` | 见 §6.1 |
| 接线复核 | `p0c3_faction_wiring_consistency.py` | 已退役 `_faction_` 行 = **102**，问题 = **0** |
| T1 | 8 个固定门禁 | 见 §6.1 |
| T2 | T1 + 受影响 ID | 见 §6.1 |
| T3 | `com.aionemu.gameserver.questEngine.**`（forkCount=2） | 见 §6.1 |

### 6.1 结果

| 档 | 命令 | 结果 | 日志 |
|---|---|---|---|
| T1 | `run_quest_gates.sh T1` | **32 例 / 0F / 0E / BUILD SUCCESS**（28 s） | `gates/T1-104311.log` |
| T2 | `run_quest_gates.sh T2 <62 ids>` | **48 例 / 0F / 0E / BUILD SUCCESS**（27 s） | `gates/T2-104353.log` |
| T3 | `QUEST_FORK_COUNT=2 run_quest_gates.sh T3` | 见 §6.3（本文档随 T3 收口更新） | `gates/T3-*.log` |

### 6.2 退役后必须一起改的"旧 XML 测试"（本轮新增发现）

T3 首轮（`gates/T3-101946.log`，10:19–10:26，1962 例 / 11F / 6E）里除 zcode 在飞的 SimpleUseItem
切片外，本轮自己的失败是 **2 例**，另有 **2 例是"靠脏 `target/classes` 假通过"**：

| 类 | 症状 | 处置 |
|---|---|---|
| `QuestCounterProjectionLockFollowUpTest#quest49702…` | 49702 退役后取生产视图 → 没有 `started` 节点（真端形状是 `unaccepted`→`a0..a6` 网格） | 删除该用例，留注释指向真端形状与两条门禁 |
| `QuestProductionJourneyTest#plansAndExecutesTargetlessNpcFactionAcquisitionFromProductionXml` | 49715 退役后首步由 `TARGETLESS_ACTION` 变为 `WORLD_EVENT`（真端 SystemGrant） | 旧 XML 目标less 形状改由 49713 承担；**新增** 49715 的真端旅程用例（首步 `WORLD_EVENT`、10 次击杀、交付 NPC 上报、可执行到底） |
| `Quest39707RetailFlowAlignmentTest` / `Quest49715RetailFlowAlignmentTest` | 直读 `/aion/…/quests/<id>.xml` 资源；XML 已删除，仅因 `target/classes` 残留旧副本而"通过" | 删除（真端 IR 已由家族冻结指纹 + 系统发放门禁锁定） |
| `Quest14120/14150ClientDialogAlignmentTest` | 同上（M5-b3x 退役的 CollectItem 行）；改取生产视图后断言的真端形状没有多段交接链 | 删除（漂移已登记在 `m5b3-collect-route-decisions.tsv`，形状由 CollectItem 冻结指纹锁定） |
| `Quest18501InteractionObjectTest` | 同上；改取生产视图后断言仍成立 | 保留，改读生产视图 |
| `QuestMutationPlannerTest#dailyRotatingNpcFactionQuestStartsTheFactionLifecycleOnAccept` | 36525 退役后接取边是 `SystemGrant` | 改断言系统发放边仍提交 `StartNpcFactionQuest` |

**根因（值得记住）**：`mvn test` 只做增量拷贝，**删除的 XML 仍留在 `target/classes`**（实测 3837 个
quest XML vs 源码树 3579 个），因此"直读资源路径"的测试会在脏构建下假通过、clean 构建下 NPE。
本轮新增审计脚本 `p0c3_stale_xml_test_audit.py` → 全树仅 6 处直读退役 XML（4 处本轮处置、
2 处属 SimpleUseItem 切片：`Quest1309ClientDialogAlignmentTest`、`QuestMutationPlannerTest` 的 2578 用例）。
**T3 判据补充**：零新增失败必须在"退役 XML 不出现在 classpath"的前提下成立。

## 7. 复现命令

```bash
python3 -B .agents/summary/scriptdll-quest-driver/p0c3_simple_hunt_sentinel_census.py
python3 -B .agents/summary/scriptdll-quest-driver/p0c3_simple_hunt_reward_npc_probe.py
python3 -B .agents/summary/scriptdll-quest-driver/m5b3x_client_reward_npcs.py           # 登记表再生（含 SimpleHunt）
mvn -o -q -Dtest=RetailSimpleHuntFamilyGateTest -Dretail.hunt.rejectsOut=/tmp/hunt.txt test
python3 -B .agents/summary/scriptdll-quest-driver/m2c2_register_rejects.py /tmp/hunt.txt  # 拒绝登记表
python3 -B .agents/summary/scriptdll-quest-driver/p0c3_build_hunt_sentinel_decisions.py    # 裁定表
mvn -o -q "-Dtest=RetailSimpleHuntEquivalenceGateTest#frozenIrFingerprintsCoverExactlyTheMigratedQuests" \
    -Dretail.hunt.adjudicatedFingerprintOut=/tmp/hunt-adjudicated-ir.tsv test               # 冻结真端侧 IR
python3 -B .agents/summary/scriptdll-quest-driver/p0c3_fix_faction_masks.py                # 星期位按真端修正
python3 -B .agents/summary/scriptdll-quest-driver/build_retention_list.py
python3 -B .agents/summary/scriptdll-quest-driver/m2e_retire_migrated_xml.py               # 退役 62 个 XML
python3 -B .agents/summary/scriptdll-quest-driver/verify_retirement.py
python3 -B .agents/summary/scriptdll-quest-driver/p0c3_faction_wiring_consistency.py
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 <ids...>
QUEST_FORK_COUNT=2 .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T3
```

## 8. 口径提醒

- SimpleHunt 家族此前的退役判据是"与历史 XML 的 IR 等价（286 行上限）"；本切片对**类别哨兵**这一子集
  引入**逐任务裁定**（真端优先），XML 在该子集里是历史内容（有接取路由而真端没有），因此不参与等价对拍，
  改由"家族门禁语义不变量 + 真端侧冻结 IR"承担护栏。判据与 CollectItem（M5-b3 `ROUTE/OTHER` 裁定）、
  SimpleTalk（漂移登记）三条线保持同一方向：**真端是金标准，XML 只在真端无法表达时保留**。
- 真端星期位全 0 的行（SimpleHunt 26 + SimpleTalk 4）退役后**不再由阵营轮换发放**——这正是真端语义
  （内容下线），与 M5-b3x 的 CollectItem 判例一致；保留 XML 反而会让这些任务继续可接（真端不会）。
