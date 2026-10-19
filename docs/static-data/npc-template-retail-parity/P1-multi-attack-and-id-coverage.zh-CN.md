# P1：多段攻击与 NPC id 覆盖

> 适用提交：本次 P1/P2/P3 批次（未单独提交）
> 真端来源：`58Server/Map/XML/npcs.xml`（UTF-16，87,734 个 `<npc>`）
> 本地来源：`src/main/resources/aion/data/static_data/npcs/npc_template_*.xml`（8 个分片，87,982 条）

## 一、多段攻击（393 条）

### 真端形状

真端每个 NPC 的 `<attacks>` 可包含多个 `<data>`，每段只有四个字段：

```xml
<attacks>
  <data>
    <attack_mod>100</attack_mod>
    <attack_range>2.000000</attack_range>
    <attack_rate>1000</attack_rate>
    <attack_type>physical</attack_type>
  </data>
  <data>
    <attack_mod>100</attack_mod>
    <attack_range>26.000000</attack_range>
    <attack_rate>1000</attack_rate>
    <attack_type>magical_Fire</attack_type>
  </data>
</attacks>
```

全库 67,277 个 NPC 有 `<attacks>`，其中：

| 段数 | NPC 数 |
|---|---|
| 1 段 | 66,884 |
| 2 段 | 392 |
| 3 段 | 1 |
| **多段合计** | **393** |

多段 NPC 共 787 个攻击段。`attack_type` 分布以 `physical` + `physical`（128）、`physical` + `magical_Fire`（70）、`magical_Water` + `magical_Water`（69）为主；另有 1 条三段 NPC。

### 本地落地

| 层 | 变更 |
|---|---|
| 数据文件 | 新增 `src/main/resources/aion/definitions/compact/npc-attack-profiles.xml`：393 个 `<npc>` / 787 个 `<attack>`，按 npc id 升序 |
| 模型 | 新增 `NpcAttackProfile` record（`range` / `rate` / `type` / `attackMod`）；`NpcTemplate` 增加 `attackProfiles` 与 `getMaxAttackRange()`、`selectAttackProfile(distance)` |
| 加载 | 新增 `NpcAttackDefinitionLoader`，在 `XmlDataLoader` 中紧跟 `NpcCombatDefinitionLoader` 之后应用；属性名 `range` / `rate` / `type` / `attack_mod` |
| 运行时 | `Npc.selectAttackProfile(target)` 按目标距离选段；`Npc.getAttackType()` 使用选中段的类型；`CreatureController.attackTarget` 按 `attack_mod` 缩放伤害并在计算完成后清除选择，避免普攻选择泄漏到技能伤害 |
| 射程 | `NpcGameStats.getAttackRange()`、`SimpleAttackManager`、`SkillAttackManager`、`GeneralNpcAI2` / `AggressiveNpcAI2`、`RetailPatternAI2` 均改用 `getMaxAttackRange()`；单段 NPC 行为不变 |

选择规则：

1. 在所有覆盖当前距离的段中，取**射程最小**的一段（近距离优先近战段）；
2. 若没有任何段覆盖目标距离，取**射程最大**的一段（用于追击/脱战判定）；实际出手仍由射程检查拦截。

攻击类型映射（大小写不敏感，`magical_` 前缀可省略）：

| 真端 | 本地 `ItemAttackType` |
|---|---|
| `physical` | `PHYSICAL` |
| `magical_fire` | `MAGICAL_FIRE` |
| `magical_water` | `MAGICAL_WATER` |
| `magical_air` / `magical_wind` | `MAGICAL_WIND` |
| `magical_earth` | `MAGICAL_EARTH` |

未知类型在加载期直接抛异常，避免静默降级。

### 样本

| npc_id | 名称 | 段 |
|---|---|---|
| `200001` | Worm_test2 | 2m physical / 32m physical |
| `204260` | NPC_Lycan_Q2916 | 2m physical / 26m magical_Fire |
| `219995` | Desert Twister | 6m magical_Air（rate 800、mod 120） / 20m magical_Earth |
| `220716` | Dreadgion_M_Raid_Drakan_Boss_01_75_Al | 10m physical / 50m magical_Earth（mod 130） |

### 口径与限制

- 真端数据没有显式的段选择顺序；上述「最小覆盖射程」规则是本地推断。若实测发现三段 NPC（`231073` Hyperion）行为异常，需按 AI/技能另行裁定。
- `attack_rate` 已建模但**仍无运行时消费者**；本地出手间隔继续由顶层 `attack_delay` 决定。
- 真端多段 `<data>` 不含 `attack_delay`，因此没有“每段独立延迟”。
- 伤害区间 `min_damage` / `max_damage` 仍是 NPC 级单值；多段只影响射程、攻击类型、`attack_rate`（存储）与 `attack_mod`。
- 魔法自动攻击走既有 `calculateMagAttackResult` 路径，其基值仍取 `stats@power`（P4 溯源项）；在 `power` 来源查清前，魔法段伤害存在已知不确定性。
- 回归闸门：`src/test/java/com/aionemu/gameserver/dataholders/loadingutils/NpcAttackDefinitionLoaderTest.java`（解析 + 真实数据 393/787 计数）。

## 二、NPC id 覆盖

| 项 | 数量 |
|---|---|
| 真端 NPC | 87,734 |
| 本地模板 | 87,982 |
| 两侧都有 | 87,692 |
| 仅真端 | **42** |
| 仅本地 | **290** |

逐条清单见 `npc-id-coverage.tsv`（列：`npc_id` / `side` / `name` / `name_desc` / `level` / `type_or_ui` / `tribe` / `ai` / `local_refs`）。

### 仅真端（42）

- 全部 42 个 id 都能在本地 `definitions/compact/ai` 中命中；其中 **2 个在本地刷怪数据中真实刷出**：
  - `836075 IDRUN_Ridebox_01`（`spawns`）
  - `837158 TIAMAT_DOWN_D_abyss_seller_Europe`（`spawns`）
- 其余为 `IDRUN_*` 场景对象、`TIAMAT_DOWN_*_abyss_seller_*` 区域商人、`world_event_*` 测试/活动 NPC。
- **本期未补模板**：本地模板需要数字 `name_id`、`tribe` / `race` / `ai` 的本地枚举映射，而真端只提供字符串 `desc`（如 `STR_NPC_NO_NAME`），仓库内没有该字符串到客户端 `name_id` 的表；强行生成会显示错误名称。该缺口作为独立数据工程项保留。

### 仅本地（290）

- **275 个未被本地 spawns / ai / skills 引用**，主要是本地活动与自定义内容：`IDEvent` 67、`LC1`/`DC1` 68、`world` 25、`Dev` 22、`IDEVENT` 17、`IDInfinity` 14、`Halloween`/`halloween` 14 等。
- **15 个被引用**（6 个同时被 ai 与 spawns 引用、5 个仅 spawns、4 个仅 ai）。
- 结论：这些是本地独有内容，不是真端遗漏；不做删除或对齐。

## 三、相关决策

- P2 `maxXp`：`LDF5_Under` 副本 157 个怪**保留本地经验**，不对齐真端 0。
- P2 事件怪 `246844`/`246845`：**保留本地自定义**。
- P3 `npc_type`：只修实例 Boss/炮台子集；`race` 暂不对齐。见 `P3-behavior-contracts.zh-CN.md`。
