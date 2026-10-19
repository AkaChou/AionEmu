# 真端 npcs.xml × 本地 npc 模板：剩余字段全量审计（2026-10-19）

> 本文件是**只读审计**（除修正本期新增模板的 `name_desc` 笔误外，未改动任何运行时数据）。
> 真端来源：`58Server/Map/XML/npcs.xml`（UTF-16，87,734 个 `<npc>`）。
> 本地来源：`src/main/resources/aion/data/static_data/npcs/npc_template_*.xml`（8 个分片，88,002 条）。
> 交叉规模：**87,712** 个 id 两侧都有（含本期补入的 20 条同族模板）。
> 逐字段结果：[`full-field-audit.tsv`](full-field-audit.tsv)。
> 已审计字段（属性数值、攻击、感知、`race`、`bound_radius`、`equipment`）仍以旧的
> [`field-diff-summary.tsv`](field-diff-summary.tsv) 与 [`P4-appearance-power.zh-CN.md`](P4-appearance-power.zh-CN.md) 为准，本文件不重复造表。

---

## 一、口径

- 缺省与不一致分开计数：本地缺属性记为 `local_missing`，真端无对应元素记为 `retail_missing`。
- 文本精确比较（`name_desc`）；枚举大小写不敏感（`type`、`tribe`）；数值整数精确比较；
  `height`/`talk_info@distance` 为浮点，按 0.006 / 0.5 容差比较。
- `abnormal_immunity` 按逗号拆分、大小写不敏感、`perification`≡`petrification` 后做集合比较。
- `height` 有两个对照：本地 `bound_radius@upper`（本地投影）与真端 `upper × scale / 100`（真端全长高度）。
- `name_id`/`title_id` 是本地数字字符串 id，真端是 `STR_...` 字符串键，二者**不能直接数值比较**；
  只统计两侧存在量与样例，归为 `X_MAPPING_REQUIRED`。

---

## 二、结果总表

| 字段 | 结论 | 可比 | 一致 | 不一致 | 本地缺 | 真端缺 |
|---|---|---:|---:|---:|---:|---:|
| `name_desc` ↔ 真端 `<name>` | **完全一致** | 87,712 | 87,712 | **0** | 0 | 0 |
| `type` ↔ 真端 `<npc_type>` | 需裁定 | 74,741 | 68,136 | **6,605** | 6,820 | 6,151 |
| `tribe` ↔ 真端 `<tribe>` | 基本一致 | 87,697 | 87,235 | **462** | 0 | 15 |
| `hpgauge_level` ↔ 真端 `<hpgauge_level>` | 有偏差 | 87,334 | 77,869 | **9,465** | 284 | 94 |
| `height` ↔ 本地 `bound_radius@upper` | 本地投影 | 87,671 | 81,399 | **6,272** | 31 | 10 |
| `height` ↔ 真端 `upper × scale / 100` | 真端无 `height` 元素 | 87,675 | 52,703 | **34,972** | 31 | 6 |
| `ammo_speed` ↔ 真端 `<ammo_speed>` | 本地大量缺失 | 2,344 | 2,336 | **8** | **85,329** | 39 |
| `talk_info@distance` ↔ 真端 `<talking_distance>` | 语义不同 | 17,499 | 13,243 | **4,256** | 69,929 | 284 |
| `talk_info@delay` ↔ 真端 `<talk_delay_time>` | 有偏差 | 3,124 | 1,596 | **1,528** | 82,488 | 2,100 |
| `abnormal_immunity` ↔ 真端 `<abnormal_immunity_list>` | **完全一致** | 21,932 | 21,932 | **0** | 65,779 | 1 |
| `name_id` ↔ 真端 `<desc>` | 映射待建 | 87,712 | — | — | 0 | 0 |
| `title_id` ↔ 真端 `<npc_title>` | 映射待建 | 33,217 | — | — | 53,973 | 52,107 |
| `name`（客户端显示名） | 真端无对应 | — | — | — | 266 | 87,712 |
| `rank` | 真端无对应 | — | — | — | 0 | 87,712 |
| `rating` | 真端无对应 | — | — | — | 1 | 87,712 |
| `abyss_type` | 真端无对应 | — | — | — | 65,100 | 87,712 |
| `state` | 真端无对应 | — | — | — | 84,906 | 87,712 |
| `floatcorpse` | 真端无对应 | — | — | — | 86,437 | 87,712 |
| `on_mist` | 真端无对应 | — | — | — | 87,647 | 87,712 |

> 参考行（不重复计算）见 TSV：`npc_type ↔ unattackable`、`equipment ↔ visible_equipments`、
> 属性数值/攻击/感知/`race`（旧报告）、`bound_radius` 与战斗元素（本期已建模）、
> `massive_looting`（93 条）与 `kisk_stats`（88 条，已由其他子系统承载）。

---

## 三、关键发现

### 3.1 `name_desc`：修正本期 20 条新模板后 100% 一致

`name_desc` 与真端 `<name>` 完全同源。本期补入的 20 条同族模板最初把 `name_desc` 写成了
`STR_NPC_NO_NAME` 或族模板的 `STR_*` 键，已统一修正为真端 `<name>`（与本地既有 87,692 条约定一致）。
修正后 **87,712 / 87,712 = 100%**。

### 3.2 `type` ↔ 真端 `<npc_type>`：6,605 条差异，且参与运行时行为

本地 `type` 枚举与真端 `<npc_type>` 词表一一对应（大小写不敏感）：

| 本地 → 真端 | 条数 |
|---|---:|
| `ABYSS_GUARD` → `MONSTER` | 2,126 |
| `GENERAL` → `ABYSS_GUARD` | 1,867 |
| `GENERAL` → `MONSTER` | 1,544 |
| `MONSTER` → `ABYSS_GUARD` | 248 |
| `MONSTER` → `GENERAL` | 226 |
| `ABYSS_GUARD` → `GENERAL` | 200 |
| `GENERAL` → `GUARD` | 125 |
| `GENERAL` → `RAID_MONSTER` | 76 |
| `MONSTER` → `RAID_MONSTER` | 73 |
| `GUARD` → `ABYSS_GUARD` | 64 |

`NpcTemplateType` 被 `Npc`（守卫判定）、`RetailPatternAI2`（怪物分支）、`VisibleObjectSpawner`
（FLAG/HOUSING/RAID_MONSTER 可见性）、`Outpost`/`Base`（旗帜）等消费，因此这不是纯展示字段。
按本期「审计只读」边界，**未改数据**；是否对齐需与 P3 守卫家族（SDJ-007）一并裁定。

### 3.3 `tribe`：99.47% 一致，462 条差异集中在要塞/传送/攻城

| 本地 → 真端 | 条数 |
|---|---:|
| `FIELD_OBJECT_DARK` → `Teleportor_Da` | 64 |
| `F4GUARD_LIGHT` → `ProtectGuard_Light` | 40 |
| `F4GUARD_DARK` → `ProtectGuard_Dark` | 40 |
| `F4GUARD_DRAGON` → `Guard_Dragon` | 40 |
| `PROTECTGUARD_LIGHT` → `SiegeWeapon_PC` | 31 |
| `PROTECTGUARD_DARK` → `SiegeWeapon_PC_Dark` | 30 |
| `AGGRESSIVEMONSTER` → `AggressiveSupportMonster` | 18 |
| `AGGRESSIVESINGLEMONSTER` → `monster` | 18 |

`tribe` 参与仇恨/阵营判定，差异会直接影响要塞与攻城行为；建议单独立项逐域裁定，不在本审计中修改。

### 3.4 `hpgauge_level`：9,465 条差异，服务端无消费者

`hpgauge_level` 在本地仅由 JAXB 读写，`grep` 未发现服务端消费者（客户端血条档位字段）。
差异不影响服务端战斗；若做客户端对齐，可直接取真端值。

### 3.5 `height`：不是真端字段，而是本地/客户端投影

- 与本地 `bound_radius@upper` 一致 **81,399 / 87,671 = 92.8%**；
- 与真端 `upper × scale / 100` 一致 **52,703 / 87,675 = 60.1%**；
- 本地 `height` 通过 `SM_NPC_INFO` 下发给客户端（`writeF(npcTemplate.getHeight())`）。

结论：`height` 是独立于 `bound_radius` 的本地投影，存在两套混合约定（例如 `200000` 取 raw upper=3，
而 `203536` 取 scaled upper=0.464）。本期 `bound_radius` 覆盖层已持有正确的
`upper × scale / 100`；若客户端高度需要与真端一致，应改为从覆盖层 `upper` 派生，而不是继续沿用
本地 `height`。本期未改（审计只读）。

### 3.6 `ammo_speed`：本地仅 2,459 条，真端 87,673 条

两侧都有时 **2,336 / 2,344 = 99.7%** 一致；8 条差异（`50↔55`、`60↔55`、`25↔50`）。
本地缺口 85,329 条。该字段已入 `NpcTemplate.ammoSpeed`，但**无运行时消费者**，
本期按 field-only 处理；若未来接入投射物速度，再按真端批量补齐。

### 3.7 `talk_info`：语义不同，不能照抄

- `distance`：可比 17,499，一致 13,243（75.7%）；本地大量默认 `5`，真端 `talking_distance=0`
  表示“无对话”，本地 `getTalkDistance()` 缺省为 `2`。
- `delay`：可比 3,124，一致 1,596（51.1%）；本地默认 `3`，真端多为 `5`/`10`/`2`。

结论：本地 `talk_info` 是对话距离/延迟的运行时参数，真端同名元素语义更接近客户端交互半径；
建议只对“确有对话”的 NPC 逐条对齐，不要全量复制。

### 3.8 `abnormal_immunity`：21,932 / 21,932 = 100%

本地 `abnormal_immunity` 位图与真端 `abnormal_immunity_list` 完全一致，无需改动。

### 3.9 `name_id` / `title_id`：数字 id ↔ `STR_*` 键，需客户端字符串表

- `name_id`：两侧都存在 87,712 条；本地是数字 id（如 `350051`），真端是 `<desc>STR_SUMMONHOMING_TEST</desc>`。
- `title_id`：两侧都有 33,217 条；本地数字 id（如 `466226`），真端 `STR_NPCTITLE_...`。
- 映射需要客户端字符串表（`client-data-workspace`）或已有的 `name_id → STR_*` 反查表；
  本期不建立映射，只记录缺口。

### 3.10 真端无对应元素的本地字段

`name`（客户端显示名，266 条缺失）、`rank`、`rating`、`abyss_type`、`state`、`floatcorpse`、`on_mist`
在真端 `npcs.xml` 中无同名元素：

- `rating` 参与 `VisibleObjectSpawner` 的 `CreatureSeeState`（感知等级），是运行时字段；
- `name` 是客户端本地化显示文本；
- `rank`/`abyss_type`/`state`/`floatcorpse`/`on_mist` 为 Encom/本地运行时语义。

这些字段无法从真端对齐，保留本地值。

---

## 四、建议的后续动作

| 优先级 | 动作 | 理由 |
|---|---|---|
| P1 | `type` ↔ 真端 `<npc_type>` 对齐评估 | 6,605 条差异且参与守卫/可见性/AI 行为；需与 SDJ-007 一起裁定 |
| P1 | `tribe` 462 条逐域裁定 | 影响仇恨/阵营/要塞行为 |
| P2 | `height` 改为从 `npc-bound-radius.xml` 的 `upper` 派生 | 客户端高度当前只有 60.1% 与真端一致 |
| P2 | `name_id`/`title_id` 建立客户端字符串表映射 | 目前无法数值比较，影响客户端显示与 GM 命令 |
| P3 | `hpgauge_level` 批量取真端值 | 服务端无消费者，纯客户端字段 |
| P3 | `talk_info` 只对确有对话的 NPC 对齐 | 语义不同，全量复制会改变交互半径 |
| P4 | `ammo_speed` 待有消费者时再补 | 当前 field-only |

---

## 五、复现方法

1. 用 `open(RETAIL, encoding="utf-16")` 逐行流式读取真端 XML，按 `<npc>` 块抽取
   `name`、`desc`、`npc_type`、`tribe`、`hpgauge_level`、`ammo_speed`、`bound_radius/upper`、
   `scale`、`abnormal_immunity_list`、`talking_distance`、`talk_delay_time`、`npc_title`
   （DTD 实体表由 Python 解析器处理，无需展开）。
2. 逐行扫描 8 个 `npc_template_*.xml`，按 `<npc_template ...>` 开标签取属性，随后取
   `<talk_info>` 与 `<bound_radius>` 子元素。
3. 按 id 关联后逐字段比较，输出 `full-field-audit.tsv`。
4. 审计脚本为一次性工具，未入库；仅保留 TSV 与本文件。
