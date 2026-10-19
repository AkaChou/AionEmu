# P2：数值小尾巴对齐

> 真端来源：`58Server/Map/XML/npcs.xml`
> 本地来源：`src/main/resources/aion/data/static_data/npcs/npc_template_*.xml` 与 `definitions/compact/ai/npc-ai-parts/*.xml`
> 审计快照：`field-diff-details.tsv`（对齐前；本文记录实际落地与保留项）

## 一、已对齐

### 模板属性（89 个模板 / 59 个属性）

| 字段 | 落地条数 | 典型 |
|---|---|---|
| `level` | 9 | `213576`–`213579` 40→35；`235892`/`235893` 61/62→62/63；`836196` 29→15；`836785`/`836786` 5→40 |
| `maxHp` | 2 | `836785`/`836786` 172→9426 |
| `hp_regen` | 2 | `836785`/`836786` 86→4713 |
| `pdef` | 2 | `836785`/`836786` 57→662 |
| `mresist` | 2 | `836785`/`836786` 28→782 |
| `main_hand_accuracy` | 3 | `836196` 474→504；`836785`/`836786` 207→1034 |
| `evasion` | 3 | `836196` 1234→352；`836785`/`836786` 144→723 |
| `maxXp` | 2 | `836785`/`836786` 0→6166 |
| `sensory_range` | 34 | `IDInfinity_*` 8/10/18 → 真端值；`836785` 20→10 |

## 二、按决策保留

| 项 | 数量 | 原因 |
|---|---|---|
| `LDF5_Under` 副本怪 `maxXp` | 157 | 用户决定保留本地经验（真端 `exp=0`，本地 5816/26011），避免副本升级断层 |
| `213576`–`213579` `max_chase_time` | 4 | 任务 14026 防守怪需要无限追击（`Quest14026GeranaiaSpawnTest` 明确断言 `max_chase_time=0`）；真端值 8 会破坏该任务 |
| `246844`/`246845` 世界事件五周年怪 | 完整属性块 | 用户决定保留本地自定义（本地 55 级/35000 HP，真端 75 级/7 或 30000 HP） |
| `700100`–`700103` 训练假人 `maxHp` | 4 | 本地 3,730,286 是有意的高血量测试假人，真端 4,190 |
| `maxXp` 浮点噪声 | 36 | 真端为 `34410517.5` / `19240125.33` 等浮点值，本地整型截断；差异 < 1，非真实缺口；已确认未写入小数 |
| `836025` | 1 | 重复条目已删除；保留的正主 `IDEVENT_Def_SWHOWITZER_01A` 各项已与真端一致，审计 TSV 中的旧行不再适用 |

## 三、未处理（非本轮目标或需独立决策）

- **`main_hand_attack` 派生偏差 16 条**：本地值非零而真端 `min/max` 中点为 0（攻城器械等）。`main_hand_attack` 是本地派生列，改动会影响技能/展示，未动。
- **8 条 `0/0` 区间覆盖非零 `main_hand_attack`**：`201054`–`201059`（攻城器械）、`246844`/`246845`（事件怪）。这些 NPC 对齐后普攻伤害为 0；均为 `NON_ATTACKABLE` + `siege_weapon` 或 `dummy` AI，预计无普攻行为，待实机抽查。
- **真端未声明 `attack_*` 的本地值**：`attack_range` 2,354、`attack_rate` 2,374、`attack_delay` 55 保留本地值；这是口径，不是缺陷。
- **`attack_rate`**：仍无运行时消费者。

## 四、验证

- 8 个 `npc_template_*.xml` 分片 `ElementTree` 解析通过。
- 59 个已对齐属性逐条与真端值一致；保留项与本地原值一致；无小数写入 `maxXp`。
- `NpcTemplateRetailAlignmentTest`、`NpcCombatDefinitionLoaderTest`、`NpcAttackDefinitionLoaderTest` 均通过。
