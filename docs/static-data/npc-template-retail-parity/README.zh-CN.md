# 真端 npcs.xml × 本地 npc 模板：全字段差异审计

**本目录为只读审计产物，本期不修改任何运行时数据。**

- 真端来源：`58Server/Map/XML/npcs.xml`（UTF-16，含 `<!DOCTYPE strings [...]>` 实体表，87,734 个 `<npc>` / 360 种元素）
- 本地来源：`src/main/resources/aion/data/static_data/npcs/npc_template_*.xml`（8 个分片，87,982 条）
- 交叉规模：**87,692** 个 id 两侧都有
- 产出：`field-diff-summary.tsv`（逐字段）、`field-diff-details.tsv`（逐条不一致，26,675 行）、`retail-only-elements.tsv`（真端元素与本地对应关系）、`skills-parity.tsv`（技能缺口，本期为空）

## 一、先看清形状：本地模板不是真端投影

| 事实 | 证据 |
|---|---|
| `npc_template_*.xml` 是 Encom 静态数据包 | 仓库第 2 个提交 `911440146`（2026-06-23）即已入库且已含 IDLuna 模板；`a52b525f4` 只把单体文件拆成 8 个分片 |
| 部分字段另有投影来源 | `ai` / `sensory_range` / `sensory_angle` / `max_chase_time` / `react_to_pathfind_fail` / `move_type_return` 的真源是 `definitions/compact/ai/npc-ai-parts/npc-ai_*.xml`（真端 AI 映射投影）；`min_damage`/`max_damage`/`stat_ratio` 的真源是覆盖层 `definitions/compact/npc-combat.xml`（`NpcCombatDefinitionLoader.apply`，加载在模板分片之后、无条件覆盖） |
| 技能关联已完全对齐 | 真端 59,058 个带 `<skills>` 的 NPC 与本地 `npc-skills.xml` 的 `<assign npc_ids>` 集合**完全相同**（缺口 0、多余 0） |
| 同名 id 静默后者覆盖前者 | `NpcData.afterUnmarshal` 用 `IntObjectHashMap.put`，不检查重复；分片间重复按分片顺序取胜 |

因此「对齐」必须逐字段判定**真源在哪一层**，不能把模板整体改成真端值——那会与 ai 分片、覆盖层打架。

## 二、逐字段差异汇总

完整表见 `field-diff-summary.tsv`。按结论分组：

### A. 数值字段：绝大多数本来已一致

| 字段 | 不一致 / 可比 | 说明 |
|---|---|---|
| `mdef` `main_hand_crit_rate` `parry` `block` | **0** | 与真端 `magical_defend` / `critical` / `parry` / `block` 逐条相同 |
| `hp_regen` | 2 / 87,340 | |
| `pdef` `mresist` | 5 / 87,341 | |
| `main_hand_accuracy` `evasion` | 6 / 87,341 | |
| `maxHp` | 9 / 87,341 | |
| `level` | 12 / 87,341 | 不一致集中在 `213576`/`213577`/`246844`/`246845`/`836785` 等极少数 |
| `sensory_range`（模板列） | 34 / 80,313 | ai 分片层的 `sensory_range` 与真端 **100%/80,312** 一致，模板列另有 34 条偏差 |

### B. 需要对齐的数值缺口（本期实际发现的问题）

| 字段 | 不一致 / 可比 | 典型 |
|---|---|---|
| `attack_rate` | **11,028 / 66,297** | `201083:15 ↔ 1000`、`206356:2 ↔ 1000` |
| `attack_delay` | **5,271 / 85,317** | `201042:1449 ↔ 6000` |
| `attack_range` | **1,097 / 66,067** | `201072:2 ↔ 15.0`（远程系怪本地被拉成近战 2） |
| `maxXp` | 198 / 87,205 | `230859:5816 ↔ 0`（真端 `exp=0` 而本地给了经验） |
| 真端有伤害范围但本地没有 | **46,070 条** | 真端 87,161 个 NPC 有 `min_damage`/`max_damage`；本地只有覆盖层 `npc-combat.xml` 的 41,271 条（与真端 100% 相同），其余走 `main_hand_attack × Rnd(80,120)%` |

### C. 不是「不一致」，而是本地另有真源（不要照抄真端）

| 本地字段 | 真端字段 | 结论 |
|---|---|---|
| `stats@main_hand_attack` | `physical_attack` | **派生列**：`main_hand_attack = (min_damage + max_damage) / 2`，87,325 / 87,341（99.98%）成立；与 `physical_attack` 的 83,194 条差异属正常 |
| `stats@power` | 真端**没有** `<power>` 元素 | 本地 `power` 只喂 `getMainHandMAttack()`（= MAGICAL_ATTACK 基数），**不参与物理普攻**（物理基数走 `main_hand_attack`）；与真端 `magical_attack` 仅 36 条相同，与任何真端数值列都不匹配（相关性覆盖 `magical_attack`/`physical_attack`/`physical_defend`/`max_hp`/`exp`），比值 p5=0.04、p50=1.47、p95=4.03 无稳定关系 → **来源未知，本期未对齐** |
| `min_damage`/`max_damage`/`stat_ratio` | 同名 | 以 `npc-combat.xml` 覆盖层为准，覆盖行与真端 100% 一致 |

### D. 行为字段（引用既有结论，不在本目录重复造表）

- `npc_type` × 真端 `unattackable` **不构成映射**：`unattackable=1` 的 17,518 条里仅 1,840 条为本地 `INVULNERABLE`；`unattackable=0` 的 29,975 条里 16,974 条为 `NON_ATTACKABLE`。逐条清单见 `../npc-type-retail-unattackable-audit.zh-CN.md` 与同目录 TSV。
- `race` × 真端 `race_type`：加别名归一化（`PC_Light→ELYOS`、`PC_Dark→ASMODIANS`，大小写不敏感）后 **7,186 / 63,786** 条不一致；真端 85 种取值中 75 种可落到本地 `Race` 枚举，另有 `Type_A..E`、`LF5_Q_Item` 等无对应。典型：本地 `BROWNIE` ↔ 真端 `PC_Light`（672 条）、本地 `BEAST` ↔ 真端 `Type_A/B/C`（1,210 条）。
- `ai` 列与真端 `ai_name` 词表不同源：本地 789 种模板 `ai` 值中 788 种能在 `@AIName` 注册表命中（仅 `quest_start_use_item` 的 5 条因注解列表被正则截断而未命中，实际已注册）；真端 `ai_name` 有 14,690 种取值。两者不能直接互写。

### E. 外观与体积

| 字段 | 不一致 / 可比 | 说明 |
|---|---|---|
| `bound_radius@side` | 36,775 / 87,655 | 同值 50,079（57%）；本地恰为真端一半 16,617（19%，如 `201003:0.25 ↔ 0.5`）；其余为 0.42/0.47/0.37… 等自有比例 |
| `bound_radius@front` | 36,666 / 87,655 | 同上（同值 50,989，本地为真端一半 16,6xx 量级） |
| `bound_radius@upper` | 22,868 / 87,654 | 同值 64,786（74%）；不一致多为本地 `2.0` ↔ 真端 `2.4` 等 |
| `equipment` | 不可比 | 本地 `<item>` 是本地物品模板 id，真端 `visible_equipments/*` 是网格名（`npc_sword_yun_02`）→ 属不同体系，需经网格/物品映射表才能比 |

真端还有 285 个元素（`appearance/*`、`scale`、`weapon_scale`、`max_enemy_count`、`elemental_defend_*`、`hit_count`、`dp_reduce`、`idle_name`、`abyss_point`、`concentration`、`can_see_polymorph`、全部商店/仓库/制造/掉落组/传送等）本地**无对应**，清单见 `retail-only-elements.tsv`。

## 三、已执行的对齐（2026-10-19）

> 本目录的 TSV 是**对齐前**的审计快照；本节记录实际落地内容。

| 步骤 | 落地 | 规模 |
|---|---|---|
| 1. 补伤害区间 | `definitions/compact/npc-combat.xml` 在既有 41,455 行后追加新行（原有行逐字节保留） | **+46,070 行**（41,455 → 87,525）；其中 1,265 行 `min=max=0`，这些 NPC 本地 `main_hand_attack` 也为 0，行为不变 |
| 2. 攻击档位 | 8 个 `npc_template_*.xml` 分片写回 `attack_range` / `attack_rate` / `attack_delay` | **19,218 条模板**（attack_rate 11,817、attack_delay 7,295、attack_range 2,115）；属性缺失的 3,831 条按真端值插入 |
| 3. `power` | 仅溯源，未改动 | 见第二节 C 组 |
| 4. `race` / `npc_type` | 未改动（行为合同，逐域裁定） | 见 `../npc-type-retail-unattackable-audit.zh-CN.md` |
| 5. 外观 | 未改动（单独立项） | — |

口径：

- 伤害区间与 `stat_ratio` 取真端 `<min_damage>` / `<max_damage>` / `<stat_ratio>`；真端未声明 `stat_ratio` 时写 `1000`（与本地默认一致，不改变该轴）。
- `attack_range`/`attack_rate`/`attack_delay` 取真端 `<attacks>` 的**首个** `<data>`；393 条多段攻击的 NPC 只取首段（这些 NPC 的后续段仍未被建模）。
- `attack_range` 的浮点值四舍五入为整数（本地 XSD 为 `xs:int`）。
- `attack_rate` 目前**无运行时消费者**（`NpcTemplate.getAttackRate()` 无调用点），本次对齐属数据一致性，不影响战斗节奏；真正影响出手间隔的是 `attack_delay`。
- 回归闸门：`NpcTemplateRetailAlignmentTest`（样例外加行数/单调性校验）与 `NpcCombatDefinitionLoaderTest`（行数与模板存在性）。

## 四、建议的对齐顺序（原计划，供后续步骤参考）

1. **补 `npc-combat.xml` 的 46,070 条伤害范围**：真端有范围、本地没有的先补齐——这是唯一「照抄真端即正确」且影响战斗数值的批量缺口。
2. **对齐 `attack_range` / `attack_rate` / `attack_delay`（约 17,000 条）**：注意真端 393 条 NPC 的 `<attacks>` 有多个 `<data>` 段（本审计只比对第一段，已在口径中说明）；落地前需确认本地 AI 的 `getAttackDistanceToTarget()` 是否另有覆盖。
3. **溯源 `stats@power`**：在查清它由什么生成之前不要动（它当前参与物理普攻基值，改动等于改全服怪物伤害）。
4. **`race` 与 `npc_type` 属行为合同**：先按 `../npc-type-retail-unattackable-audit.zh-CN.md` 的做法逐域裁定，不要批量照抄。
5. **外观（bound_radius / scale / appearance）**：影响客户端体型与命中盒，建议单独立项，不与数值同批。

## 五、口径与边界

- 缺省视为「缺失」单列计数，不计入「不一致」。
- 数值比较：整数精确比较；真端浮点（`15.000000`）四舍五入到 2 位后比较。
- 大小写：文本类（`race_type`、`tribe`）归一化为小写比较；`race` 另加 `PC_Light→ELYOS`、`PC_Dark→ASMODIANS` 别名。
- 多 `<attacks><data>`：只比第一段（真端 393 条多段）。
- `equipment` 与 `visible_equipments` 不可比，未计入统计。
- `X_NO_RETAIL` / `E_APPEARANCE` 两类**不写入** `field-diff-details.tsv`（前者不是缺陷，后者只出汇总率）。
- 真端文件为 UTF-16 且带巨大 DTD 实体表：解析前先 `iconv -f UTF-16 -t UTF-8`，并注意 DTD 展开后正文约 518MB。

## 六、复现方法

1. `iconv -f UTF-16 -t UTF-8 58Server/Map/XML/npcs.xml` 流式读入；
2. 逐 `<npc>` 块抽取字段，按本文件第二节的映射与本地分片/ai 分片/`npc-combat.xml`/`npc-skills.xml` 对比；
3. 输出四张 TSV；审计脚本为一次性工具，未入库（`docs/` 仅保留结果）。
