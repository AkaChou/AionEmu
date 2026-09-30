# P0c-2：SimpleTalk `_faction_` 类别哨兵行 → 系统发放形状（40 行落地 + 40 个 XML 退役）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 前置：P0c（`RetailSystemGrantDispatcher` + `NpcFactions` 接线）已让"含 `SystemGrant` 边的定义可被发放"。
> 本切片把 **SimpleTalk 家族里接取名是 `_faction_` 哨兵的行**真正编译出来——它们此前一律被
> `REJECTED:RETAIL_ACQUIRE_NPC_SENTINEL`（118 行）挡住，只能留在 XML 降级。

## 1. 结论

1. 真端 `Quest_SimpleTalk.xml` 的哨兵行共 **181**：`_faction_` 98（宇宙内 **58**）、
   `_challengetask_` 75（宇宙内 60）、`_area_` 8（**全部在本服宇宙之外**）。
2. 宇宙内 58 行 `_faction_` **并非全部"干净单步"**（交接摘要的预估过乐观）：
   - 报告名 = 真 NPC 名（Palas / Priamos / Columba / Alaum）**24 行**；
   - 报告名 = 复合势力引用（`LF4_GuardianOfDivine` / `LF4_BountyHunter_Li` / `DF4_GuardianOfTower` /
     `LDF5a_Silverlin_L` …）**34 行**；
   - 其中带 `talk_npcN` 对话链 **14 行**；
   - 复合引用在客户端任务书 dic 链里**解不出交付 NPC 集 4 行**（45045–45048）。
3. 本切片补齐两个能力后落地 **40 行**：①系统发放形状（无接取路由 + `SystemGrant` 边）；
   ②复合报告名走**客户端 dic 链登记表**（与 CollectItem 的 P1a 同一条证据链）。
4. 落地 40 行全部退役：**catalog 3681 → 3641**、**RETAIL_TABLE 2543 → 2583**、
   SimpleTalk 可驱动 **1598 → 1638**；`verify_retirement.py` 通过（catalog=目录=3641、退役=2583、和=6224）。
5. 仍未落地 18 行（宇宙内）+ 60 行 `_challengetask_`，全部带**精确稳定码**（§4）。

## 2. 普查（脚本 `p0c2_simple_talk_sentinel_census.py` → `p0c2-simple-talk-sentinel-census.tsv`）

| 轴 | `_faction_`（98 行） | `_challengetask_`（75 行） | `_area_`（8 行） |
|---|---|---|---|
| 宇宙内 | **58** | 60 | 0 |
| 报告 NPC 名唯一可解 | 41 | 75 | 8 |
| 报告 NPC 名不可解（复合引用） | 57（宇宙内 34） | 0 | 0 |
| 带 `talk_npcN` 链 | 34 | 0 | 2 |
| `item_check=1` | 64 | 60 | 0 |
| 真端 `quest.xml` 有 `npcfaction_name` | 98/98 | 0 | 0 |
| 生产 `npc_factions_quest.xml` 星期位 | 全部 `1111111`（每天可发放） | 未登记（不适用） | 未登记 |

- 阵营键落地：58 行覆盖 6 个势力（`GuardianOfDivine` / `GuardianOfTower` / `BountyHunter_Li` / `BountyHunter_Da` /
  `Silverlin_L` / `Silverlin_D`），与 `RetailQuestMetadataCompiler.NPC_FACTIONS` 一一对应 → `NpcFactions` 的
  候选过滤（`metadata.npcFactionId()` + 星期位）天然满足。
- `_area_` 8 行全部在宇宙之外（catalog ∪ 退休清单都没有），不参与任何接线契约。
- `_challengetask_`：本服 `ChallengeTaskService` 只有 `showTaskList` / `onChallengeQuestFinish`
  （`QuestService.java:148` 在完成时回调），**没有"受理挑战子任务"的入口** → 无发放入口，保持 XML 降级。

## 3. 客户端 dic 链探测（`p0c2_simple_talk_reward_npc_probe.py`）

34 行复合报告引用里 **30 行**能从 `QUEST_Q<id>.html → STR_DIC_E_<token> → STR_DIC_N_<name> → name_desc`
解出交付 NPC 集（多为 2–3 个支部 NPC，共享 dic 如 `35021`、`36500`、`LDF5b_Silverlin_BA`）；
**4 行（45045–45048）客户端页无 dic 令牌** → 保留 XML。

## 4. 变化与未落地原因

### 4.1 代码

| 文件 | 变化 |
|---|---|
| `retail/RetailGrantKind.java`（新） | 类别哨兵分类共享化：`of()` / `systemGrant()` / `knownGrant()` / **`grantable()`**（NPC ∪ FACTION ∪ AREA 有发放入口） |
| `retail/RetailSimpleCollectItemTable.java` | 内嵌 `GrantKind` 枚举迁出到 `RetailGrantKind`（行为不变） |
| `retail/RetailQuestMetadataCompiler.java` | 新增共享判定 `isFactionComposite(...)`（原 CollectItem 私有副本删除，避免两份实现漂移） |
| `retail/RetailSimpleTalkTable.java` | `Entry` 增 `grantKind`（接取名分类） |
| `retail/RetailSimpleTalkDefinitionCompiler.java` | ①接取门按类别分流：`_faction_`/`_area_` 放行、`_challengetask_` → 新稳定码、未知哨兵 → 原码；②报告门支持复合势力引用（客户端登记缺失 → 新稳定码）；③`build()` 生成系统发放形状（`acquiredNpc=-1` + 单条 `SystemGrant` 边 + 逐交付 NPC 展开报告/完成/关窗出口） |
| `retail/RetailQuestDriver.java` | 传入 `clientRewardNpcs`（生产 overlay 同口径） |

### 4.2 客户端交付 NPC 登记表扩展

`m5b3x_client_reward_npcs.py` 的族从"SimpleCollectItem 全族"扩为
"SimpleCollectItem 全族 ∪ SimpleTalk 复合势力奖励引用行"（判定复用 Java 的 `NPC_FACTIONS`，不复制映射）：
`family=231 registered=69`（原 178/28；SimpleTalk 部分新增 41 行登记、12 行无解 → 后者含 45045–45048）。

### 4.3 门禁

| 门禁 | 新增/变更 | 结果 |
|---|---|---|
| `RetailSimpleTalkGateTest` | 系统发放分支：**禁接取/续页路由** + **必须有 `SystemGrant` 边** + 报告 NPC 关窗出口；多交付 NPC 循环；`ACCEPTED_FLOOR` 1598 → **1638** | 3/3 绿 |
| `RetailSystemGrantDispatchTest` | 新增 2 例：已退役 SimpleTalk `_faction_` 行**必须可发放**（≥40）；`_challengetask_` 行**不得**被系统发放 | 5/5 绿 |
| `RetailSimpleCollectItemGateTest` | 共享枚举/共享复合判定的回归 | 5/5 绿 |

### 4.4 漂移登记重算（`retail-simple-talk-drift.tsv`，2223 行）

| 分类 | 前 | 后 |
|---|---|---|
| `DIFF:NODE_PROJECTION` | 1002 | **1042**（+40 = 新接受） |
| `DIFF:TRANSITION_SET` | 553 | 553 |
| `EQUIVALENT` | 43 | 43 |
| `REJECTED:RETAIL_TALK_CHAIN` | 308 | **322**（+14：原来的接取门拒绝，现在推到对话链门） |
| `REJECTED:RETAIL_TALK_ITEM` | 162 | 162 |
| `REJECTED:RETAIL_ACQUIRE_NPC_SENTINEL` | 118 | **0** |
| `REJECTED:RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH`（新） | — | **60**（`_challengetask_`：已知哨兵、本服无受理入口） |
| `REJECTED:RETAIL_REWARD_NPC_FACTION_COMPOSITE`（新） | — | **4**（45045–45048：客户端无 dic 交付登记） |
| 其余（`ACQUIRE_NPC_UNRESOLVED` 19 / `TALK_CUTSCENE` 12 / `REWARD_NPC_UNRESOLVED` 6） | 37 | 37 |

新接受 40 行全是 `DIFF:NODE_PROJECTION`（历史 XML 把无进度域的任务写成 `REWARD/var0=1` 之类），
与 2026-09-23 口径一致：**XML 不是金标准，真端语义 + 客户端契约才是**。

### 4.5 退役

- `build_retention_list.py` 重算：`RETAIL_TABLE/OK` 2543 → **2583**；
- `m3b_retire_simple_talk_xml.py`：`migrated=1538 moved=40 already_retired=1498 missing=0`，
  catalog 条目 `3681 → 3641`（40 个 `quests/<id>.xml` 删除，内容由 git 历史承担）；
- `verify_retirement.py`：`catalog=3641 directory=3641 retired=2583 sum=6224 → OK`。

退役清单（40）：35031–35034、35039–35042、35045–35048、36503/09/15/20/22/24/25/27/28、39603、39610、
45031–45034、45039–45042、46503/09/21/24/25/26/28、49603、49610。
其中 24 行报告名是复合势力引用（走客户端登记表）、16 行是真 NPC 名；**40/40 都带 `item_check=1`**
（交付检查：`collect_item*` 条件 + 移除动作 + 缺物品回落失败页/关窗）。

### 4.6 运行期接线四层一致（退役 40 行）

`NpcFactions.sendDailyQuest()` 的候选过滤要同时满足 `metadata.npcFactionId() == faction.getId()` 与
`NPC_FACTIONS_QUEST_DATA.isActiveOn(id, today)`，因此退役行必须四层对齐：

| 层 | 来源 | 结果 |
|---|---|---|
| ①真端名 | `quest.xml <npcfaction_name>` | 40/40 有值（GuardianOfDivine / GuardianOfTower / BountyHunter_Li / BountyHunter_Da / Silverlin_L / Silverlin_D） |
| ②编译器映射 | `RetailQuestMetadataCompiler.NPC_FACTIONS` | 40/40 落到非 0 阵营 id（2/4/5/7/15/16） |
| ③星期位表 | `npc_factions_quest.xml` 的 `faction_id` + 星期位 | 40/40 与②**逐行相等**，星期位全 `1111111`（每天可发放） |
| ④阵营对象 | `npc_factions.xml` 的 `npc_faction id` | 含 2/4/5/7/15/16 → `NpcFactions` 会遍历到这些阵营 |

复核方式：按 §7 的普查脚本口径解析 `NPC_FACTIONS`（正则抽 `Map.entry("名", id)`，不复制映射）、
真端 `quest.xml` 的 `<npcfaction_name>`、两张生产表，判定 `①→② == ③` 且 `② ∈ ④`。

## 5. 未落地（带精确原因，供后续批次直接消费）

| 行 | 数量 | 稳定码 | 缺口 |
|---|---|---|---|
| 35010/11/17/18、35024–35026、45010/11/17/18、45024–45026 | 14 | `RETAIL_TALK_CHAIN` | 需实现 ScriptDLL64 状态链（`FUN_180cabb10`）在定义里的等价形状 |
| 45045–45048 | 4 | `RETAIL_REWARD_NPC_FACTION_COMPOSITE` | 客户端任务书无 dic 交付线索，需另找权威（XML/服务端阵营表） |
| SimpleTalk `_challengetask_` 60 行 | 60 | `RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH` | 本服挑战任务系统无受理入口（只有完成回调） |
| `_area_` 8 行（宇宙外） | 8 | —（不在宇宙，无契约行） | 真端有行、本服无任务 |

## 6. 验证

| 档 | 选择器 | 结果 |
|---|---|---|
| 定向 | `RetailSimpleTalkGateTest,RetailSystemGrantDispatchTest,RetailSimpleCollectItemGateTest` | **13 例 / 0F / 0E**（`gates/P0c2-talk-gate-093712.log`） |
| T1 | 8 个固定门禁（选择器唯一来源 `affected_quest_tests.py#T1_GATE_CLASSES`） | **30 例 / 0F / 0E / 31 s**（`gates/T1-093742.log`；+2 = 本切片新增例） |
| T2 | T1 + 101 个受影响 ID（58 SimpleTalk `_faction_` + 43 CollectItem `_faction_`）命中类 | **121 例 / 0F / 0E / 33 s**（`gates/T2-093827.log`） |
| T3 | `com.aionemu.gameserver.questEngine.**`（`QUEST_FORK_COUNT=2`） | **1957 例 / 0F / 0E / 1 skipped / 437 s / BUILD SUCCESS**（`gates/T3-093908.log`） |

### 6.1 T3 对比

| 轮次 | 例数 | 失败 | 说明 |
|---|---:|---:|---|
| 09:12（zcode 并发改 Hunt 中） | 1955 | 5 | SimpleHunt 冻结 IR 指纹漂移（与本切片无关，失败方法集合与更早基线逐字相同） |
| **09:39（本切片）** | **1957** | **0** | +2 = 本切片新增门禁；zcode 的 Hunt 修复落地后指纹失败清零 |

### 6.2 附带复核

`p0c2_faction_wiring_consistency.py`：已退役 `_faction_` 行 = 40，**问题 = 0**（四层阵营接线一致，见 §4.6）。

## 7. 复现命令

```bash
# 普查（哨兵行可编译性 + 阵营键 + 星期位）
python3 -B .agents/summary/scriptdll-quest-driver/p0c2_simple_talk_sentinel_census.py
# 客户端 dic 链探测（复合报告名 → 交付 NPC 集）
python3 -B .agents/summary/scriptdll-quest-driver/p0c2_simple_talk_reward_npc_probe.py
# 登记表再生（CollectItem + SimpleTalk 复合行）
python3 -B .agents/summary/scriptdll-quest-driver/m5b3x_client_reward_npcs.py
# 漂移登记再生（先导出门禁分类，再落登记）
mvn -o -q -Dtest=RetailSimpleTalkGateTest -Dretail.talk.equivOut=/tmp/talk-drift.txt test
python3 -B .agents/summary/scriptdll-quest-driver/m3b_simple_talk_drift.py --classification /tmp/talk-drift.txt --write-registry
# 保留清单 + 退役 + 收口验证
python3 -B .agents/summary/scriptdll-quest-driver/build_retention_list.py
python3 -B .agents/summary/scriptdll-quest-driver/m3b_retire_simple_talk_xml.py
python3 -B .agents/summary/scriptdll-quest-driver/verify_retirement.py
# 回归
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 <ids...>
QUEST_FORK_COUNT=2 .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T3
```

## 8. 口径提醒

- `_faction_` 行**不是**"解析缺口"：接取名不是 NPC，真端由阵营日常轮换发放（星期位表见
  `npc_factions_quest.xml`）；本切片证明"可编译 + 可发放"两件事必须**同时**成立才退役。
- 复合报告名（`LF4_GuardianOfDivine` 等）的真端语义是"交给该势力在本图的支部 NPC"，
  权威是**客户端任务书 dic 链**，不是历史 XML。
- 本轮**未**改动 `retail-xml-retention.tsv` 的口径（仍是"真端语义 + 客户端契约通过即可退役"），
  只是把新落地的 40 行按该口径重算进去。
