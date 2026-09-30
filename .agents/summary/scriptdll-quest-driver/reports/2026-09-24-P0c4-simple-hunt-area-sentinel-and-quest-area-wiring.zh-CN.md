# P0c-4：SimpleHunt `_area_` 类别哨兵 → 区域发放接线（8 行落地 + 8 个 XML 退役）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-24
- 前置：P0c-3（`_faction_` 62 行）已完成；本切片处理它留下的 `RETAIL_ACQUIRE_NPC_SENTINEL_AREA_PENDING`
- 判据：**真端世界文件是区域发放的金标准**。真端 `Map/Worlds/<world>/world*.xml` 的
  `<questscript_area>` 用 `<quest>` 绑定任务 id，进区域即由区域引擎直接 `startQuest`；
  生产同义载体是 `definitions/compact/ai/ai-areas.xml` 的 `<quest_area ... quests="...">`。

## 1. 结论

1. `_area_` 不是"真端无法表达"，而是**生产区域表缺 6 条绑定**（真端世界文件全部有）。
   按真端补齐 4 条 `quest_area`（覆盖 6 个任务 id）后，8 行宇宙内 `_area_` 全部可迁移。
2. 新判据入代码：`RetailQuestAreaIndex`（生产 `ai-areas.xml` 的 quest_area 只读视图）+
   `RetailSimpleHuntDefinitionCompiler.requireAcquire`：**只有 quest id 已被 quest_area 绑定才放行 `_area_`**，
   未绑定仍保留稳定码 `RETAIL_ACQUIRE_NPC_SENTINEL_AREA_PENDING`（不再有本批实例）。
3. 8 行按 `_faction_` 同一口径登记**逐任务裁定**（真端优先，`basis=AREA_GRANT`），并退役 8 个 XML。

## 2. 真端普查（金标准）

生成：`.agents/summary/scriptdll-quest-driver/p0c4_world_questscript_area_scan.py`

| 项 | 值 |
|---|---|
| 扫描 world 文件 | 701（`world.xml` / `world_M.xml` / `world_N.xml`） |
| `<questscript_area>` 总数 | 657（去重后 285 个 world+区域键） |
| 绑定 quest 的条目 | 492 |
| 涉及 quest id | 331 |
| 生产 `<quest_area>` | 231 → **235**（本切片 +4） |

> 注：`world.xml` 里 `InvadePortalDest_*_questArea_01` 一类区域 `<quest>` 为空（传送落点区），不参与任务绑定。

## 3. 对账与落地

生成：`.agents/summary/scriptdll-quest-driver/p0c4_quest_area_delta.py`（`p0c4-quest-area-delta.tsv`）

| 状态 | 前 | 后 |
|---|---|---|
| MATCH（world+区域名对齐且 quest 集一致） | 220 | 224 |
| MISSING_IN_PROD（真端有、生产无） | 64 | 60 |
| EXTRA_IN_PROD | 9 | 9 |
| QUESTS_DIFFER | 1 | 1 |
| 真端 quest id 在生产缺失 | **16** | **9** |

本切片补齐的 4 条（生成器 `p0c4_emit_quest_area_snippets.py` → `p0c4-quest-area-snippets.xml`）：

| world_name | world_id | 区域名 | quests |
|---|---|---|---|
| df2a | 220050000 | `InvadePortalDest_41_questArea_02` | 39005 |
| df2a | 220050000 | `InvadePortalDest_41_questArea_03` | 49009,49007 |
| lf2a | 210060000 | `InvadePortalDest_42_questArea_02` | 49004,49005 |
| lf2a | 210060000 | `InvadePortalDest_42_questArea_03` | 39007,39009 |

world_id 取自生产 `ai-areas.xml` 既有映射（不新造），多边形/bottom/top 逐点照抄真端世界文件。

## 4. 代码改动

| 文件 | 变化 |
|---|---|
| `retail/RetailQuestAreaIndex.java`（新增） | `ai-areas.xml` 的 `<quest_area>` → quest id ⇒ world id 集；`isBound/areaCount/worlds/worldNames` |
| `retail/RetailSimpleHuntDefinitionCompiler.java` | `compile(...)` 增第 4 参 `questAreas`；`requireAcquire` 对 `AREA` 放行条件 = 已绑定 |
| `retail/RetailQuestDriver.java` | 装载 `/aion/definitions/compact/ai/ai-areas.xml` 并传入合成器 |
| `definitions/compact/ai/ai-areas.xml` | +4 条 `quest_area`（§3） |
| `test/.../RetailSimpleHuntFamilyGateTest.java` / `RetailSimpleHuntEquivalenceGateTest.java` | 装载区域表并传参 |
| `test/.../RetailSystemGrantDispatchTest.java` | 新不变量：已退役 `_area_` 行必须"quest_area 有绑定 + 带 SystemGrant 边" |
| `test/.../CounterChainTripletContractTest.java` | 改读生产视图；18033/28033 的旧 XML 链式阶梯合同退役，门禁只锁仍由 XML 承载的 28313（+2842/1841 饱和投影） |
| `test/resources/quest/retail-simplehunt-compiler-rejects.tsv` | 116 → **108** 行（`_area_` 8 行出表） |
| `test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv` | 62 → **70** 行（+8 `AREA_GRANT`） |
| `test/resources/quest/retail-simple-hunt-adjudicated-ir-fingerprints.tsv` | 62 → **70** 行（真端侧冻结 IR） |

## 5. 真端形状 vs 旧 XML（为什么必须裁定而不是等价对拍）

以 18033 为例（探针实测，生产视图）：

| 维度 | 历史 XML | 真端（合成结果） |
|---|---|---|
| 接取 | `NPC_START` 对话（801037） | `unaccepted --SystemGrant--> a0b0c0`（进区域发放，无接取 NPC） |
| 计数节点 | 链式 `started → k1 → k2 → k3`（每行一槽，乱序不计数） | 产性格子 `a0b0c0..a1b1c1`（每槽独立推进，乱序也计数） |
| 进度字段 | var0/var1/var2 @ 0/6/12（与真端一致） | var0/var1/var2 @ 0/6/12 |
| 报告/完成 | 801281（k3/reward） | 801281（reward/complete，来自真端 reward_npc_name） |

因此 8 行走"逐任务裁定 + 真端侧冻结指纹"，与 P0c-3 的 62 行、M5-b3x 的 CollectItem 裁定行同一口径。

## 6. 退役

```
python3 -B .agents/summary/scriptdll-quest-driver/p0c4_register_area_grants.py     # 裁定表 62 → 70
mvn -o -q "-Dtest=RetailSimpleHuntEquivalenceGateTest#frozenIrFingerprintsCoverExactlyTheMigratedQuests" \
    -Dretail.hunt.adjudicatedFingerprintOut=/tmp/hunt-adjudicated-ir.tsv test      # 冻结 70 行真端 IR
python3 -B .agents/summary/scriptdll-quest-driver/build_retention_list.py          # 8 行 → RETAIL_TABLE/OK
python3 -B .agents/summary/scriptdll-quest-driver/m2e_retire_migrated_xml.py       # migrated=356 moved=8
python3 -B .agents/summary/scriptdll-quest-driver/verify_retirement.py             # catalog=directory, 无悬空引用
```

- 退役清单（8）：18033 28033 39005 39007 39009 49005 49007 49009
- 本切片落地后 `_area_` 宇宙内拒绝数：**8 → 0**（`RETAIL_ACQUIRE_NPC_SENTINEL_AREA_PENDING` 清零）。

## 7. 验证

| 档 | 选择器 | 结果 | 日志 |
|---|---|---|---|
| 家族门禁 | `RetailSimpleHuntFamilyGateTest` | 1 例 / 0F（334 s） | 同批 surefire 输出 |
| 等价 + 指纹门禁 | `RetailSimpleHuntEquivalenceGateTest` | 2 例 / 0F | 同上 |
| 接线门禁 | `RetailSystemGrantDispatchTest` | 7 例 / 0F（含新 `_area_` 不变量） | 同上 |
| T1 | 8 个固定门禁 | 32 例 / 0F / SUCCESS | `gates/T1-110905.log` |
| T2 | T1 + 70 个裁定 id 命中类 | 59 例 / 0F / SUCCESS | `gates/T2-111251.log` |
| T3 | 整个 questEngine 测试树（forkCount=2） | 见 §7.1 | `gates/T3-*.log` |

### 7.1 T3 clean 全树实测与失败定性（2026-09-24）

命令：`mvn -o -B clean test -Dtest='com.aionemu.gameserver.questEngine.**' -DfailIfNoTests=false -DforkCount=2`
（日志 `gates/T3-p0c4-clean.log`，总耗时 7:39）。

结果：**1952 例 / 22F / 13E / 1 skipped / BUILD FAILURE**。

**必须 clean 的原因（方法论勘误）**：`mvn test` 只做增量拷贝，已从 `src/main/resources` 删除的
`quest_definition/quests/*.xml` 会**残留在 `target/classes`**，使"直读退役 XML"的旧测试**假通过**。
M2-e / M3 / M4 / M5 / P3 各批次引用的 T3「0F」证据都是增量构建取的，对"退役 XML 悬空引用"这一面
**不成立**；自本行起全树门禁以 `clean` 为准。

**本批判定**：P0c-4 退役的 8 个 id（18033 28033 39005 39007 39009 49005 49007 49009）在 35 个失败中
**零命中** → 本批零新增失败，P0c-4 收口成立；同时暴露此前各批次退役留下的 35 处悬空/缺口。

失败三分类（35）：

| 分类 | 数量 | id / 用例 | 归属 |
|---|---:|---|---|
| SimpleUseItem 退役批：测试仍直读 XML 或按旧 XML 断言 | 20 | 1309(+LegacyTemplateMirror)、1514×5、1197、3914、1644×2、1561×2、11036×2、2718/1718、80008×3、2578 | zcode 在飞切片（不动） |
| SimpleSerialHunt 退役批：生产合成器缺真端 `talk_npc1` 简报步骤 + 标签/元数据漂移 | 14 | 13918×7、30600×6、30610×1 | 本线程（P0c-5，见 §7.2） |
| SimpleCollectItem 退役批：测试直读已退役 XML | 1 | 3734 | 本线程（P0c-5） |

### 7.2 P0c-5：串行族 `talk_npc1` 简报步骤接线

真端 `Quest_SimpleSerialHunt.xml` 的 `talk_npc1`（30600=Linocus→800324、30610=Aluna→800326）在
AionEmu 侧**从未被解析**（`RetailSimpleSerialHuntTable` 只读 count/monster/acquired/reward），导致
退役后 30600/30610 丢失"接取→见简报 NPC→清标志位→开计数"这一步；客户端证据一致：
`quest_q30600.html` 同时存在 `select1`（接取）与 `select2`（简报页），且 `select2` 的唯一按钮是
`HACTION_SETPRO1`。落地：`Entry` 增列 `talkNpc`；串行合成器增 `SECTION_5` 简报标志位（接取置 1，
SETPRO1 清 0）与 `started/briefed/k1..kN` 规范节点名，`RETAIL_TALK_NPC_UNRESOLVED/AMBIGUOUS` 为保留 XML 的稳定码。

漂移裁定（真端优先，登记在案）：
1. 旧 XML 的 EnterWorld 旧档修复边（13918 step 档 var0=2..5、30600/30610 step-2 档、旧领奖投影补齐）
   **不再迁移**，存量进度按新阶梯重读；如需零进度损失，另做一次性 DB 归一化（不在本切片）。
2. `metadata.kills` 零售元数据不填（`RetailQuestMetadataCompiler` 传 `List.of()`），生产代码零消费；
   击杀面权威改为合成定义里的 `KillNpc` 边（真端客户端阶段契约登记）。

> 实施与验证明细见专项报告 `reports/2026-09-24-P0c5-serial-hunt-briefing-step.zh-CN.md`
> （T1 37/37、T2 81/81；串行冻结指纹重算；旧档迁移面反向锁定）。

## 8. 未落地 / 后续

| 项 | 数量 | 说明 |
|---|---|---|
| `_challengetask_`（宇宙内） | 84 | 本服挑战任务只有完成回调、无受理入口 → 保留 XML（`RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH`） |
| 真端有绑定、生产仍缺 | 9 个 quest id | 4 个 `InvadePortalDest_*_questArea_01`（落点区兄弟行 39006/39008/49006/49008）、`IDF5_TD_War_QuestArea_Q26960`（26960–26962）、`LDF4_Advance_QuestArea_PVP_ALL`（13745/23745）——都不是本片 `_area_` 行，按"有需要再补"登记 |
| `LF4_M` 世界归属 | 9 条 | 生产把 `LF4_M` 的 PVP 区挂在 world 210050000（lf4），真端在 `LF4_M`（world_name=LF4_M，id/worldid.xml = 210130000）；需先确认本服是否加载该 world 再改，登记待定 |
