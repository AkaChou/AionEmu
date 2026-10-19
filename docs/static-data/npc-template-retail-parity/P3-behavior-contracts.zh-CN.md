# P3：行为合同对齐

> 真端来源：`58Server/Map/XML/npcs.xml`
> 本地来源：`src/main/resources/aion/data/static_data/npcs/npc_template_*.xml`
> 关联审计：`../npc-type-retail-unattackable-audit.zh-CN.md` / `.tsv`；本目录 `field-diff-details.tsv`

## 决策

| 项 | 决策 |
|---|---|
| `npc_type` | 第一批修实例 Boss / 炮台 54 条；第二批把非守卫 848 条全部改为 `ATTACKABLE`（含 `PEACE` / `INVULNERABLE` 与已知友军）；守卫家族待 SDJ-007 |
| `race` | 暂不对齐，仅保留映射分析 |

## 一、`npc_type`：非守卫 848 条已全部改为 ATTACKABLE

### 第一批：实例 Boss / 炮台 54 条

筛选规则：`../npc-type-retail-unattackable-audit.zh-CN.md` 的 106 条高价值清单中，名称含 `Boss` / `Howitzer`，或为 `LDF4_Advance_GodElite*` / `IDRUN_Spider_01` / `IDEternity_02_Event_Nepilim_03` 的条目。全部由 `NON_ATTACKABLE` 改为 `ATTACKABLE`。

| 分组 | npc_id | 条数 |
|---|---|---|
| DF4 3111 Boss（暗） | `258220`–`258224` | 5 |
| DF4 3111 Boss（光） | `258215`–`258219` | 5 |
| DF4_M_9011–9017 Boss | `884355`–`884450` 区间内 28 个 | 28 |
| LF4_M_8011–8017 Boss | `884250`–`884345` 区间内 14 个 | 14 |
| GAb1 炮台 | `277225`–`277228` | 4 |
| LDF4 精英 | `235064`、`235065` | 2 |
| 事件 Nepilim | `220588` | 1 |
| IDRUN 蜘蛛 | `835812` | 1 |

验证：54 个模板当前均为 `ATTACKABLE`；8 个分片 XML 解析通过。

### 第二批：非守卫 848 条

按决策，把审计 TSV 中 `retail_unattackable=0` 且 `local_tribe` 不含 `GUARD` 的 848 条全部改为 `ATTACKABLE`：

| 原 `npc_type` | 条数 | 处置 |
|---|---:|---|
| `NON_ATTACKABLE` | 724 | 改为 `ATTACKABLE` |
| `PEACE` | 114 | 改为 `ATTACKABLE` |
| `INVULNERABLE` | 2 | 改为 `ATTACKABLE` |
| `ATTACKABLE`（第一批已修） | 8 | 保持 |

实际写回 **840 条**；8 个分片 XML 解析通过，模板总数 88,002。其中 56 条为 `IDYUN` / `IDF6` / `IDLUNA` 已知友军（已刷出 33 条），按决策一并改为 `ATTACKABLE`；这会改变和平、无敌与友军语义，后续需按任务/守卫脚本实测。

### 未改动：守卫家族 8,430 条

- `local_tribe` 含 `GUARD` 的 8,430 条（其中 1,131 条已刷出）保持原 `npc_type`。
- 真端按阵营相对可攻击，本地 `npc_type` 是绝对量；批量改成 `ATTACKABLE` 会改变 PvP 守卫行为，仍须按 SDJ-007 建立判定层。
- `race` 仍未对齐，见第二节。

## 二、`race`：未对齐

### 现状

- 与真端 `race_type` 的可比条目 63,786，不一致 **7,186** 条。
- 把两侧都做「小写 + 去下划线 + `PC_Light→ELYOS`、`PC_Dark→ASMODIANS`」归一化后，**规范化等价项为 0**：所有差异都是分类体系不同，不是拼写/大小写问题。

### 差异分类

| 类别 | 条数 | 典型 |
|---|---|---|
| 宽泛类型 vs `Type_A`–`Type_E` | 3,632 | `BEAST → Type_A` 448、`BEAST → Type_B` 422、`MAGICALMONSTER → Type_E` 228 |
| 阵营别名 | 1,671 | `BROWNIE → PC_Light` 672、`BROWNIE → pc_light` 350、`ASMODIANS → pc_light` 185 |
| 跨阵营 | 514 | `ELYOS → GChief_Light` 101、`ASMODIANS → brownie` 85、`ELYOS → brownie` 81 |
| 龙族首领命名 | 433 | `GCHIEF_DRAGON → Drakan` 418、`→ drakan` 15 |
| 其他 | 936 | `BROWNIE → pc_light_Castle_Door` 48、`DRAKAN → Construct` 47、`ELYOS → Drakan` 47 |

### 为什么不批量改

`race` 参与阵营判定、仇恨、技能目标与部分掉落/任务过滤。上述差异中，`BROWNIE ↔ PC_Light`、`BEAST ↔ Type_A-E` 等不是「值写错了」，而是 Encom 与真端对同一 NPC 使用了不同的分类轴；直接写真端值会把一大类 NPC 的阵营语义改掉。当前保留本地值，后续按域（副本 / 世界 / PvP 守卫 / 活动）逐域出映射表再裁定。

## 三、后续可执行项

1. PvP 守卫家族：按 SDJ-007 建立「阵营相对可攻击」判定层，而不是逐条改 `npc_type`。
2. `IDYun_*` / `IDF6_*` / `IDLUNA_*`：已按第二批决策改为 `ATTACKABLE`，需在实例内实测友军/任务脚本是否受影响。
3. `race`：以副本/世界为域，产出「真端 `race_type` → 本地 `Race`」映射表；不要全库照抄。
