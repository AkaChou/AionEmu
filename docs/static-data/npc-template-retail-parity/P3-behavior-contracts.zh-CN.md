# P3：行为合同对齐

> 真端来源：`58Server/Map/XML/npcs.xml`
> 本地来源：`src/main/resources/aion/data/static_data/npcs/npc_template_*.xml`
> 关联审计：`../npc-type-retail-unattackable-audit.zh-CN.md` / `.tsv`；本目录 `field-diff-details.tsv`

## 决策

| 项 | 决策 |
|---|---|
| `npc_type` | 只修实例 Boss / 炮台等明确敌对项（54 条） |
| `race` | 暂不对齐，仅保留映射分析 |

## 一、`npc_type`：已落地 54 条

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

### 未改动的部分（全库 9,278 条中）

- **已刷出的其余 1,275 条**：绝大多数是 PvP 守卫家族（`GUARD` / `GUARD_DARK` / `PROTECTGUARD_*` / `F4GUARD_*` / `LDF5_*GUARD` 等）。真端按阵营相对可攻击，本地 `npc_type` 是绝对量；改成 `ATTACKABLE` 会改变 PvP 守卫行为，须按 SDJ-007 逐域裁定。
- **高价值清单中排除的 52 条**：
  - `IDLUNA_SIEGE_Soldier_*` 5 条：已知友军，保持 `NON_ATTACKABLE`。
  - `IDYun_Soldier_*` / `IDYUN_Hard_Soldier_*` 22 条：`IDYUN_ANCIENT` 阵营，可能是友军；需实例内实测。
  - `IDF6_LF1_NPC_Ra_*` 6 条：`IDF6_LF1_PROTECT`，保护者阵营，需确认敌对关系。
  - `NPC_*` 静态 NPC 7 条、`DF2_NPC_*` / `DGuard_LycanFighter_*` 5 条、`IDLF1_*_Brownie_*` 2 条、`IDF5_TD_*` / `IDLDF5_*` PEACE 4 条：均需逐条确认是否友军/脚本对象。
- **未刷出的其余条目**：没有 spawn 证据，暂不处理。

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
2. `IDYun_*` / `IDF6_*` / `NPC_*`：实例内实测其实际敌对关系，再决定是否改 `ATTACKABLE`。
3. `race`：以副本/世界为域，产出「真端 `race_type` → 本地 `Race`」映射表；不要全库照抄。
