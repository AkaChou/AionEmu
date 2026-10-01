# P0a §2.6/§4.4.4：owner 重冻与 owner-identity 三源身份矩阵

> ⚠️ **2026-10-01 修正**：本报告的"名字缺失 4623 行"结论源于审计工具只索引 npcTemplates 的 `name` 属性、漏掉 `name_desc`（真端式全名）。修正后 READY=7542/8562（88%），真实残差=518 缺失+165 歧义行。修正归因与数字见 **[owner-identity-correction.md](owner-identity-correction.md)**；`owner-identity.tsv` 已重跑更新。下文初版数字保留作历史记录，不再作为决策依据。

> P0a 只读审计产物 · 2026-10-01。逐行矩阵：`owner-identity.tsv`（9232 行）；工具：`tools/owner_identity.py`（幂等只读）。
> ⚠️ 基线口径：retention 侧取自 **QE-112 在飞未提交**版本（6224 行）；QE-112 落地后重跑本脚本即为正式冻结。

## 1. owner 重冻（过渡期台账 vs 真端表独立重算）

| 集合 | 行数 | 来源 |
|---|---:|---|
| 真端家族表行（8 家族 + DD） | **8562** | `<真端根>/Map/XML/` 独立解析 |
| 本服台账全量 owner | 6224 | `retail-xml-retention.tsv`（QE-112 版） |
| XML definitions | 742 | `definitions/quests/*.xml` |
| 全集（并集） | 9232 | — |

关键差值：**真端表 8562 行 ≠ 台账 retail 5482 行** —— 3080 个真端表行从未进入本服运行时 owner（身份/可达性/适配原因被台账排除）。这 3080 行是 native 车道"表存在即可驱动"幻觉的反证：**表行存在不足以 native-ready**（计划 §2.6 口径的正确性被数据证实）。
一致性正证据：台账 `RETAIL_TABLE` 行 100% 在真端表中（LEDGER_ONLY_NO_TABLE = 0），台账无幽灵行。

## 2. 三源身份判定（verdict 分布）

| verdict | 行数 | 含义 |
|---|---:|---|
| `NATIVE_READY_RAW_VARS_PENDING` | 2872 | 名字唯一解析 + 客户端证据齐；只欠 raw vars 存档结论 |
| `NATIVE_READY_SENTINEL_ACQUIRE` | 655 | 同上，但接取名为引擎哨兵（`_faction_`/`_challengetask_`/`world_quest_*` 等），哨兵语义由引擎显式实现 |
| `NATIVE_NAME_MISSING` | **4623** | 真名（NPC/monster dev 名）在本服静态数据无模板 |
| `NATIVE_NAME_AMBIGUOUS` | 75 | 名字命中多个 npc id |
| `CLIENT_EVIDENCE_MISSING` | 337 | id 不在客户端 quest.xml（全为 DD 行） |
| `NO_TABLE_XML_ONLY` | 670 | 无真端表行（= XML_RETENTION NO_TABLE 670，两口径吻合）|

### 按家族

| 家族 | READY(含哨兵) | NAME_MISSING | AMBIGUOUS | CLIENT_MISSING |
|---|---:|---:|---:|---:|
| SimpleTalk | 1550 / 3152 | 1533 | 69 | — |
| SimpleHunt | 17 / **1863** | **1846** | — | — |
| DataDriven | 1294 / 2492 | 858 | 3 | 337 |
| CombineTask | 451 / 574 | 123 | — | — |
| SimpleCollectItem | 108 / 262 | 154 | — | — |
| SimpleUseItem | 92 / 160 | 65 | 3 | — |
| SimpleItemPlay | 14 / 43 | 29 | — | — |
| SimpleSerialHunt | 1 / 16 | 15 | — | — |

## 3. 名字缺失的根因分层（4623 行不是单一问题）

缺失名 Top（按出现行次）：`_faction_`(270)→哨兵已分桶；真名大头：`world_quest_q_q_*`(212)、`test_simpletalk_17`(63)、`event_edandos/nebrith`(114)、`utisda`/`uquini`(110)、`*_bountyhunter_*`(98)、`hynus`/`gard` 等。

1. **哨兵名**（已分桶 655 行 READY_SENTINEL）：引擎显式语义，非缺口。
2. **命名惯例漂移**：真端表用短 dev 名（如 `NeutL1_25_Ae`、`Kimci`），本服模板名带地图/变体前后缀（如 `ab1_new_neutl1_65_an`）。**这正是本服过去用客户端 `quest_monster.csv` + `RetailNpcNameIndex` 硬编码绕过的坑**；native 名字解析器（`NativeNpcNameResolver`）必须给出显式解析策略（规范化规则 / 刷怪点 join / 数据表映射 + 出处），否则 SimpleHunt 整族无法满足"家族内未闭环行整族不切换"（不变式 2）。
3. **真缺失**：模板集确实没有对应刷怪（测试名 `test_simpletalk_17`、部分活动 NPC）→ 这类行 native-ready 判据只能走"客户端证据 + 引擎哨兵/映射表"或登记 `IDENTITY_EVIDENCE_REQUIRED`。

## 4. 337 个 CLIENT_EVIDENCE_MISSING（全部 DD）

客户端 `quest.xml`（10,035 id）不含这 337 个 DD 表 id：多为 `_challengetask_`/活动/内部行。处置：与 §4.1 DD 轴合并裁决——真端表有行但客户端无正文 = 玩家不可达或客户端另有来源（本解包根无任务书/页文件，正文证据链只有 quest.xml/quest_monster.csv 两路）。**默认冻结为 `IDENTITY_EVIDENCE_REQUIRED`，不阻塞 DD 表解析，但阻塞 DD 家族切换判定。**

## 5. 结论（对切换批次的影响）

1. **名字解析器是 P1 的第一阻塞件**，先于 SimpleHunt 切换：4623 行的解析策略未定 ⇒ SimpleHunt 不满足整族切换前置。
2. owner 推导器可按本矩阵自动化：`NATIVE_OWNER = 真端表行 ∩ 三源齐`；`CONFLICT_CANDIDATE = 0`（台账仲裁下现无表/XML 双主冲突）。
3. AMBIGUOUS 75 行需逐行消歧（多模板同名），进 P1 名字解析器负例集。
4. QE-112 落地后重跑本脚本刷新全部数字；本文件数字不作冻结验收。
