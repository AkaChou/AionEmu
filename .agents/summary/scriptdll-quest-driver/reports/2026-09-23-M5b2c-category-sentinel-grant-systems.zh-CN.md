# M5-b2c 类别接取名哨兵 = 三种真端发放系统（`_faction_` / `_challengetask_` / `_area_`）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 口径：真端优先。本切片**只做证据与裁定**，不改生产代码；结论用于把 M2-c 批次 2 的
> "真端无法推导" 改判为 "真端可表达，但形态是**系统发放**而非 NPC 对话框"。

## 1. 起点：M5-b2b §4.8/§4.9 留下的 40 行

SimpleCollectItem 的 40 行 `REJECTED:RETAIL_ACQUIRE_NPC_SENTINEL` 在 §4.8/§4.9 已证为
"自动接取"形状（无生命周期三元组 + 客户端只有 `ask_quest_accept`）。本切片回答**它到底靠什么发放**。

真端家族表给的是**类别哨兵名**，不是 NPC：

```xml
<!-- Quest_SimpleCollectItem.xml（真端） -->
<id id="35007">
	<dev_name>신성 수호회 FOBJ 01</dev_name>
	<acquired_npc_name>_faction_</acquired_npc_name>   ← 类别哨兵
	<party_drop>1</party_drop>
	<object1>FOBJ_Q35007</object1>
	<reward_npc_name>Palas</reward_npc_name>
</id>
```

对照正常行（1103）是 `<acquired_npc_name>Mires</acquired_npc_name>`。

**三元集合完全相等（零差集）**：

| 口径 | 计数 | 集合 |
|---|---:|---|
| 真端家族表 `acquired_npc_name = _faction_` | 43 | A |
| 客户端任务书只有 `ask_quest_accept`（无 `select1`） | 43 | B |
| A ≡ B | **差集 = ∅** | — |
| 旧 XML 无接取 NPC（契约索引 `start_npc_ids = 0`） | 40 | C ⊂ A |
| DLL 无生命周期三元组 | 43/43 | A 全中 |

即：`_faction_` ⟺ 客户端无 ACCEPT/REFUSE 页 ⟺ 无生命周期三元组。

> **勘误（2026-09-24，P0c 门禁暴露）**：本节初版曾断言"多出的 3 行（`39611`/`47112`/`49611`）
> 旧 XML 写了接取 NPC"。**该断言错误**：这 3 行**不在本服宇宙内**——`quest_definition_catalog.xml`、
> `quests/` 目录、`git HEAD`、`retail-xml-retention.tsv`、漂移登记表里都没有它们，因此在契约索引里
> 表现为 `no-contract-row`（不是"有接取 NPC"）。正确口径：真端表 `_faction_` **43 行 = 本服宇宙内
> 40 行 + 宇宙外 3 行**；宇宙内 40 行与"`start_npc_ids = 0` 的 40 行"完全重合（P0c 门禁
> `RetailSystemGrantDispatchTest` 已断言这 40 行全部带 `SystemGrant` 边）。

## 2. 决定性证据：真端有 `npcfactions_quest.xml`（阵营 × 星期）

`<真端根>/Map/XML/npcfactions_quest.xml`（UTF-16）**436 条**：

```xml
<quest_id quest_id="35007">
	<dev_name>신성 수호회 FOBJ 01</dev_name>
	<npcfaction_name>GuardianOfDivine</npcfaction_name>
	<mon>1</mon>          ← 只有周一
</quest_id>
```

→ `_faction_` 的语义 = **由"NPC 阵营日常轮换"系统发放**，`npcfaction_name` 给阵营、`mon..sun` 给星期位。

`Quest_SimpleCollectItem.xml` 的 43 行分布（`m5b2b_faction_grant_coverage.py`）：

```text
  8  GuardianOfDivine   1111111   ← 每天
  8  GuardianOfTower    1111111
  6  BountyHunter_Li    1111111
  6  BountyHunter_Da    1111111
  4  Silverlin_L        0000000   ┐
  4  Silverlin_D        0000000   │
  3  Greenhat_L         0000000   ├ 全 0 = 真端不发放（15 行）
  3  Greenhat_D         0000000   │
  1  Mentor_Da          0000000   ┘
```

**40 行哨兵因此再分两半**：

- **28 行**：真端**每天**由阵营发放 → 应驱动（走既有阵营系统，无需新增"接取路由"）；
- **12 行**（39601/39605/39608/39701/39708/39709 + 49601/49605/49608/49701/49708/49709）：
  真端**全星期 0** → 真端永不发放。AionEmu 现有 XML 却给了接取 NPC 路由，属"跑了真端不该跑的任务"。

## 3. AionEmu 已经有这三套子系统（不需要新造形状）

| 类别哨兵 | 真端文件 | AionEmu 现有实现 | 本族覆盖 |
|---|---|---|---|
| `_faction_` | `Map/XML/npcfactions_quest.xml`（436 条） | `NpcFactionQuestData#isActiveOn` + `QuestsData#sortedByFactionId`（`QuestTemplate.npcFactionId`）+ `PlayerQuestStartEligibilityPort`（`NPC_FACTION_QUEST_NOT_ACTIVE` / `NPC_FACTION_QUEST_COOLDOWN`）；生产数据 `npc_factions/npc_factions_quest.xml` | 各族合计 **270 行中 268 覆盖（99.3%）**；其中 **150 行真端全 0**（collect 15 / hunt 89 / talk 44 / itemplay 2） |
| `_challengetask_` | `Map/XML/challenge_task.xml`（123 挑战任务 → 159 quest_id） | `ChallengeTaskService` / `ChallengeData`；生产数据 `quest_data/challenge_tasks.xml`（159 quest_id） | SimpleHunt **87 行 87/87 覆盖** |
| `_area_` | `Map/Worlds/*/world_*.xml` 的 `quest_area` | `RetailAiDefinitionLoader`（解析 `quest_area` → `QuestArea.questIds`）+ `RetailAreaEngine`（进区域即启动）；生产数据 `definitions/compact/ai/ai-areas.xml`（231 条） | SimpleHunt 21 行**仅 3/21 命中生产数据** → **真缺口**，需按真端世界文件重算 `ai-areas.xml` |

`_area_` 的 21 行里包含 §4.9 的 4 个反例 `13912/13913/23912/23913`（"triplet 却 ask_quest_accept"）：
它们是**区域触发**任务，接取页自然不是 NPC 对话 —— 反例由此得到解释，不再是异常。

### 3.1 阵营 id 对齐核验（真端 `npcfactions.xml` ↔ AionEmu `npc_factions.xml`）

| 项 | 真端 `Map/XML/npcfactions.xml` | AionEmu `static_data/npc_factions/npc_factions.xml` |
|---|---|---|
| id=2 | `GuardianOfDivine`，`category=dailyquest`，`minlevel_permitted=30`，`race=pc_light`，`desc=STR_FACTION_GuardianOfDivine` | `Alabaster Order`，`category=DAILY`，`minlevel=30`，`race=ELYOS`，`nameId=2258001`，`npcid=799803` |

- **id / 类别 / 最低等级 / 种族四元对齐**，差异只在"名称"：真端给 **dev name**，AionEmu 给**客户端显示名**。
- 交叉印证：真端 `quest.xml` 的 35007 行带 `category2 = STR_FACTION_GuardianOfDivine`，与真端
  `npcfactions.xml` 的 `desc` 同串；而 AionEmu 侧 35007.xml 的 `npc-faction-id="2"` 与上表 id 一致。
- **未验证**：AionEmu `nameId=2258001` 的文本未在客户端字符串表核对；本表只主张"id 对齐"，
  不主张"名称逐字一致"。
- 真端 `quest.xml` **自带 `<npcfaction_name>`** → 驱动层无需依赖 `npcfactions_quest.xml` 取阵营名，
  只需用它取**星期位**。

## 4. 对 M2-c 批次 2 结论的修正（留痕）

M2-c 批次 2 曾判"144 个接取名是真端类别哨兵（`_challengetask_` 74 / `_faction_` 62 / `_area_` 8），
真端 `challenge_task.xml`、`npcfactions*.xml` 与 NPC 注册表都不含其 npc id → **不可由真端文件推导**"。

**修正**：该结论只在"**当成 NPC 推导**"这个前提下成立。三类哨兵**都有真端发放系统**：

- `_challengetask_` → `challenge_task.xml`（本次实测 SimpleHunt **87/87 覆盖**）；
- `_faction_` → `npcfactions_quest.xml`（各族 **268/270 覆盖**）；
- `_area_` → 世界文件 `quest_area`（生产数据覆盖不足，属**数据缺口**而非机制缺口）。

因此 `RETAIL_ACQUIRE_NPC_SENTINEL` 的正确裁定是：**真端可表达，但形态是"系统发放 + 无接取路由"**，
而不是"真端无法表达 → 保留 XML"。

## 5. 施工路径（M5-b3x，待执行，不在本切片）

1. `RetailSimpleCollectItemTable` 已读 `acquired_npc_name`；新增类别哨兵识别
   （`_faction_` / `_challengetask_` / `_area_` → `GrantKind`）。
2. `RetailSimpleCollectItemDefinitionCompiler`：哨兵行**不生成 `acceptFlow` / `setproRoute` / `reportNpcExit` 的接取半边**，
   只保留 `report`（`reward_npc_name`）+ 采集对象 + 领奖；即 §4.9 的"自动接取"形状。
3. metadata 携带 `npc-faction-id`：由 `npcfactions_quest.xml` 的 `npcfaction_name` → `npc_factions.xml` 的 id 解析。
4. 全 0（禁用）行按真端**不生成发放**；对应 XML 退役前先确认 AionEmu 不会因此把它们变成"永不可接取"以外的行为差异。
5. 之后才有资格动 §4.8 的 40 行（28 可驱动 + 12 真端禁用）。

## 6. 复现命令

```bash
# 类别哨兵 × 真端阵营发放覆盖 / category sentinel vs retail faction grants
python3 -B .agents/summary/scriptdll-quest-driver/m5b2b_faction_grant_coverage.py
# → m5b2b-faction-grant-coverage.tsv

# 客户端接取页 × 三元组（7 家族）/ client accept page vs triplet (7 families)
python3 -B .agents/summary/scriptdll-quest-driver/m5b2b_family_ids.py
python3 -B .agents/summary/scriptdll-quest-driver/m5b2b_client_accept_page_probe.py \
    --family-src .agents/summary/scriptdll-quest-driver/m5b2b-family-ids-SimpleCollectItem.tsv \
    --out .agents/summary/scriptdll-quest-driver/m5b2b-client-accept-page-SimpleCollectItem.tsv
python3 -B .agents/summary/scriptdll-quest-driver/m5b2b_family_accept_crosstab.py
```

## 7. 边界（未验证项）

- **未**在实机客户端验证"阵营日常任务的接取界面"；本节只证真端数据与 AionEmu 代码路径存在。
- `npcfactions_quest.xml` 全 0 的语义按 AionEmu 既有注释取"已禁用"；其中 `Mentor_Da` 等
  可能是**另一套系统**（导师）发放，未逐条证明 —— 已登记为待查，不阻塞 28 行可驱动部分。
- `_area_` 的 18 行缺失需要重算 `ai-areas.xml`（真端世界文件 10 GB，未在本轮执行）。
