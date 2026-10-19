# P4：外观（bound_radius / appearance / scale / weapon_scale）与 stats@power 溯源

> 本文件包含审计结论与已落地的运行时改动：`bound_radius`（第二节）与战斗元素/主人物品继承（第九节）。
> The document records both the audit and the implemented runtime changes: `bound_radius` and the combat-element/master-item inheritance work.
> 上游：工作区同级真端目录 `58Server/Map/XML/npcs.xml`（UTF-16，87,734 个 `<npc>`）；
> 本地：`src/main/resources/aion/data/static_data/npcs/npc_template_*.xml`（8 个分片，审计快照 87,982 条；补入 20 条后 88,002 条）。
> 交叉规模：审计快照 87,692 个 id 两侧都有（补入 20 条后 87,712）；真端独有 42 个（补入后 22 个），本地独有 290 个。
> 配套分类表：[`P4-retail-only-classification.tsv`](P4-retail-only-classification.tsv)（287 行，逐元素分类）。

---

## 结论摘要

1. **`bound_radius` 已按「全长」约定落地。** 本地分片实际混合了两套约定：
   - **原始值约定**：`local = raw`，48,653 个 NPC；
   - **半值约定**：`local.front = raw.front × scale / 200`、`local.side = raw.side × scale / 200`、`local.upper = raw.upper × scale / 100`，30,904 个 NPC；
   - 其余 8,097 个为第三套历史快照，无法由当前真端值解析得到。

   真端服务端在加载时会把 `bound_radius` 三轴乘以 `scale / 100`，并用 `max(front, side) * 0.5` 作为内部碰撞半径（`58Server/server58-source/NPCServer_NPCSvr64/classes/Misc/ObjectClass.cpp:741-761`）。本地已新增 `definitions/compact/npc-bound-radius.xml` 覆盖层（覆盖 87,705 个模板），按 `raw × scale / 100` 生成三轴全长值；`BoundRadius` 增加 `fullLength` 标记，NPC 覆盖层的 `collision = max(front, side) * 0.5`。玩家/坐骑/机甲仍使用半径约定。消费者已统一到 `getHalfFront()` / `getHalfSide()` / `getHalfMaxHorizontal()`，`bound_radius` 不再 BLOCKED。

2. **`equipment` 可以建立映射，映射键是 `ItemTemplate.name_desc`。** 真端 `visible_equipments/<slot>` 的网格名与本地物品模板的 `name_desc` 同源：main/sub/torso/glove/foot/leg/shoulder 的网格名可解析率 99.17%–99.96%，head 96.4%，wing_mesh 0%（`.cgf` 路径，不是物品）。按 NPC 逐槽位比较，同名率 75.5%–84.8%；剔除箭矢与翅膀后，装备网格集合完全相等的 NPC 为 **40,509 / 49,644 = 81.6%**。差异主要来自版本变体（`_01`/`_02`、`guard`/`lodge`、vritra/gabsub 等）和投射物，不是映射体系不可行。**不建议用网格名替换本地 `<item>`**，因为物品 id 参与 NPC 攻击类型与装备属性计算。

3. **`stats@power` 不能从真端 5.8 溯源，它是 Encom 本地的等级曲线。** 真端 `npcs.xml` 没有 `<power>` 元素，客户端 `npc_client` 表也不含 `power`/`magical_attack`/`physical_attack`/`level`/`max_hp` 等战斗数值；与真端 `magical_attack` 仅 36/87,234 条完全相同。本地 87,982 条模板只有 287 个不同 `power` 值，且 1–60 级呈现严格的按十级分段线性曲线（每段步长 3,4,5,6,7,8）。**不要用 `magical_attack` 覆盖 `power`**：`power` 只喂 `NpcGameStats.getMainHandMAttack()`（MAGICAL_ATTACK 基数），影响 NPC 魔法普攻/魔法技能基数，不参与物理普攻；直接替换会让尾部比例达到 4 倍以上。

4. **287 个真端独有元素**（含 3 条审计伪元素，实际字段 284 个）分类为：
   - 已由其他子系统承载 **129** 个（掉落 19、商店/仓库/服务 68、传送/副本 11、任务/对话 15、技能/战斗覆盖层 13、AI 投影 3）；
   - 纯外观/客户端 **40** 个（模型/外观 36、客户端 UI/本地化 4）；
   - **战斗数值 87 个已落地数据层**（第九节）：87 = 56 个 `master_item_*`（由 `npc-master-item-bonus.xml` 承载）+ 31 个其他元素（其中 30 个由 `npc-combat-elements.xml` 承载；`disk_type` 复核为客户端分类字段，不属战斗数值）；5 个（`dp_reduce`、`cancel_level`、两种 damage trim、`noreducespell_immune`）为数据-only，其余均接入现有战斗/抗性公式；
   - 仍未建模 **31** 个（AI 感知/威胁 20；PvP/深渊奖励 4；服务开关 3；其他 4）。

---

## 一、口径

- 数值比较：整数精确比较；真端浮点四舍五入到 4 位后比较，容差 `0.0011`。
- `bound_radius` 分组时对 `scale` 缺失按真端默认 `100` 处理；真端 `scale=0` 不存在，最小 2、最大 1500。
- `equipment` 映射按 `name_desc.strip().lower()` 做大小写不敏感匹配；本地槽位由物品模板 `slot` 位掩码解析（1=main、2=sub、4=head、8=torso、16=glove、32=foot、2048=shoulder、4096=leg）。
- 30 个本地缺失 `bound_radius`、7 个真端缺失/为零的条目不参与比例统计。
- 真端 `.cgf` 翅膀、`arrow`/`drakanarrow01` 投射物在“剔除后集合比较”中排除。

---

## 二、bound_radius：运行时语义与消费者

### 2.1 本地字段与派生

`src/main/java/com/aionemu/gameserver/model/templates/BoundRadius.java`

```java
collision = (float) Math.sqrt(side * front);
```

`src/main/java/com/aionemu/gameserver/model/templates/npc/NpcTemplate.java:107-108,337-338` 直接把 XML 的 `front/side/upper` 反序列化为 `BoundRadius`；本地模板没有 `scale`、`appearance`、`visible_equipments` 字段。

### 2.2 真端服务端语义

- `ObjectClass::Set()` 读取 `bound_radius` 后，再读 `scale`（默认 100）；若 `scale != 100`，把 front/side/upper 三轴都乘以 `scale / 100`（`ObjectClass.cpp:741-761`）。
- 同一段代码计算 `max(front, side) * 0.5` 存入内部半径字段（0x148），即真端内部碰撞半径 = 运行时全长的一半。
- 本地 AI 投影已携带真端 `scale`：`npc-ai-parts/*.xml` 的 `model_scale` 与真端 `<scale>` 在 48,532/48,532 条上完全一致（`RetailAiDefinitionLoader.java:741`）。

### 2.3 本地消费者

| 消费者 | 使用的轴 | 语义 |
|---|---|---|
| `BoundRadius.collision` | `sqrt(front*side)` | 局部碰撞半径 |
| `GeoService.java:447-459,515,540-543` | `max(front,side)`、`collision`、`upper` | 视线检查、可见检查、垂直偏移 |
| `SM_NPC_INFO.java:149-166` | `front`（+0.125，持械 +0.1） | 下发给客户端的边界 |
| `PositionUtil.java:111-117`、`MathUtil.java:401-402` | `front/side`、`collision` | 位置偏移、单位间距 |
| `WalkerGroup.java:105,195` | `side` | 编队间距 |
| `PathService.java:875`、`PathfindSectorSampler.java:41-62`、`FollowMotor.java:183` | `upper` | Z 偏移、寻路采样 |
| `TargetRangeProperty.java:71-113,211` | `collision` | 技能目标距离 |
| `AbstractCollisionObserver.java:72` | `upper` | 碰撞观察 |
| `CreatureEventHandler.java:111-151` | `max(front,side) * model_scale / 200` | 感知范围偏移 |

### 2.4 抽样分组

以 `raw` 表示真端原值，`scale` 缺失按 100。按三轴联合判定：

| 组 | 判定 | 数量 | 说明 |
|---|---|---|---|
| 原始值约定 | `local ≈ raw`（三轴） | **48,653** | 新内容 id 段（834–885k、235–247k）几乎全部属于此组 |
| 半值约定 | `local ≈ (raw×scale/200, raw×scale/200, raw×scale/100)` | **30,904** | 旧内容 id 段（200–216k、270–286k）为主；与本地 `sqrt(front*side)` 碰撞公式自洽 |
| 第三套快照 | 两者都不满足 | **8,097** | 本地值与 5.8 真端无解析关系 |

逐轴排他分类（分母为三轴齐全的 87,633 条）：

| 轴 | `local=raw` | `local=raw×scale/200` | 其他 | 备注 |
|---|---|---|---|---|
| front | 50,374（57.5%） | 24,364（27.8%） | 12,658（14.4%） | 另有 186 条 `raw/2` |
| side | 49,387（56.4%） | 22,601（25.8%） | 15,364（17.5%） | 另有 208 条 `raw/2` |
| upper | 63,300（72.2%） | — | 11,003（12.6%） | `raw×scale/100` 13,264（15.1%） |

“其他”比例的 front/side 并非单一系数：front 最常见的是 0.60（5,636 条）、0.50（2,296）、0.53（1,646）、0.68（1,600）、0.45（1,517）、0.40（1,125），说明第三套快照与 5.8 真端不是简单的缩放关系。真端缺失/为零 7 个、本地缺失 30 个，均不参与比例统计。

与既有审计 `field-diff-summary.tsv` 的差异说明：该表以 `local*2 ≈ raw`（容差 0.01）统计“本地为真端一半”，得到 front 16,613 / side 16,587，与 README 的 16,617/16,6xx 一致；本表采用三轴联合与 `scale` 修正后的公式，因此半值组更大。

### 2.5 抽样样本

| NPC id | scale | 本地当前 (f/s/u) | 真端原始 (f/s/u) | 半值目标 (f/s/u) | 组 |
|---|---|---|---|---|---|
| 201003 | 100 | 0.25 / 0.25 / 1.3 | 0.5 / 0.5 / 1.3 | 0.25 / 0.25 / 1.3 | 半值（已对齐） |
| 202514 | 120 | 0.9 / 0.54 / 1.32 | 1.5 / 0.9 / 1.1 | 0.9 / 0.54 / 1.32 | 半值（已对齐） |
| 800031 | 100 | 0.25 / 0.35 / 2.0 | 0.5 / 0.7 / 2.0 | 0.25 / 0.35 / 2.0 | 半值（已对齐） |
| 833817 | 100 | 0.5 / 0.7 / 2.0 | 0.5 / 0.7 / 2.0 | 0.25 / 0.35 / 2.0 | 原始值（会改动） |
| 201070 | 100 | 0.3 / 0.2958 / 2.4 | 0.5 / 0.5 / 2.0 | 0.25 / 0.25 / 2.0 | 第三套快照 |
| 201072 | 100 | 0.3 / 0.2958 / 2.4 | 1.2 / 1.2 / 5.7 | 0.6 / 0.6 / 5.7 | 第三套快照 |
| 200000 | 10 | 1.2 / 3.46 / 3.0 | 1.2 / 3.456 / 3.0 | 0.06 / 0.1728 / 0.3 | 原始值（SkillZone，scale=10） |

> 200000 是 `skillarea` AI 的 SkillZone，`scale=10`；按半值公式目标会缩到 0.06/0.17/0.3。该类 NPC 是否应参与 bound_radius 对齐需单独确认。

### 2.6 统一对齐规则

真端值 + `scale` 足以生成目标值，**不需要逐条手写**。若采用“本地 front/side 为半径（半值）”约定：

```
target.front = raw.front * scale / 200
target.side  = raw.side  * scale / 200
target.upper = raw.upper * scale / 100
```

若采用“本地 front/side 为全长”约定：

```
target.front = raw.front * scale / 100
target.side  = raw.side  * scale / 100
target.upper = raw.upper * scale / 100
```

两种方案的影响面（相对当前本地值）：

| 轴 | 需要改动的行数 | 升高 | 降低 | 目标/当前中位数 |
|---|---|---|---|---|
| front | 52,224 | 2,166 | 50,056 | 0.50 |
| side | 52,292 | 2,024 | 50,266 | 0.50 |
| upper | 35,522 | 25,302 | 10,217 | 1.20 |

按 id 段看，235–247k 与 834–885k 几乎全部会改动（分别 11,315/11,386、10,631/10,668），而 200–216k 大部分保持不变（2,727/11,142）。这正是“混合数据源”的指纹。

> 实际落地（2026-10-19）：新增 `definitions/compact/npc-bound-radius.xml` 覆盖 **87,705** 个模板；相对本地分片，front/side/upper 分别改动 **66,361 / 66,586 / 35,570** 条。差异高于上表估算，因为覆盖层按每个 NPC 的 `scale` 逐条计算，且包含 20 条新补模板。

### 2.7 风险与处置（已落地）

| 消费者 | 隐含假设 | 若数据为半值组 | 若数据为原始值组 |
|---|---|---|---|
| `BoundRadius.collision` + `GeoService`/`PositionUtil` | front/side 是**半径（半值）** | 自洽 | 碰撞/偏移约为真端的 2 倍 |
| `CreatureEventHandler.sensoryBoundOffset` | front/side 是**全长（原始值）** | 偏移被重复缩放：`sensoryOffset / 真端碰撞` 中位数 **0.50**、p95 0.75 | 自洽 |

量化：原始值组的 `localCollision / 真端碰撞` 中位数 **1.667**、p95 **2.236**；半值组的感知偏移仅为真端碰撞的约一半。也就是说，**当前代码无论选哪一组数据，都有一个消费者是错的**。因此建议：

1. 先由战斗/移动域裁定本地 `bound_radius` 的语义（半径还是全长），并在 `BoundRadius` 类注释中固化；
2. 同时修正另一个消费者：
   - 选“半径”约定：把 `sensoryBoundOffset` 改为 `max(front,side)`（不再乘 `model_scale/200`），或改用 `collision`；
   - 选“全长”约定：把 `BoundRadius.collision` 改为 `max(front,side) * 0.5`，并复核 `GeoService` 的偏移；
3. 再按对应公式批量生成 `bound_radius`，并做刷怪、碰撞、视线、感知、寻路回归。

**落地结果（2026-10-19）**：已选「全长」约定。`BoundRadius` 增加 `fullLength` 标记与 `getHalfFront()` / `getHalfSide()` / `getHalfMaxHorizontal()`；`NpcTemplate` 增加 `setBoundRadius()`；新增 `NpcBoundRadiusDefinitionLoader` 与 `definitions/compact/npc-bound-radius.xml`。`GeoService`、`PositionUtil`、`WalkerGroup`、`SM_NPC_INFO` 与 `CreatureEventHandler` 已改用半值/`getHalfMaxHorizontal()`；玩家、坐骑、机甲保持半径约定。`bound_radius` 不再 BLOCKED，剩余项是实机回归（见第六节 #4）。

---

## 三、equipment / visible_equipments 映射

### 3.1 映射键

真端 `visible_equipments/<slot>` 存的是网格名（如 `npc_sword_yun_02`）；本地物品模板 `ItemTemplate.name_desc` 恰好就是同一个网格名。全库 128,170 个带 `name_desc` 的物品模板中，只有 6 个网格名对应多个物品 id（最多 3 个），因此“网格名 → 物品”基本唯一。

### 3.2 覆盖率

| 槽位 | 真端网格名可解析到物品模板 | NPC 双侧逐槽位同名率 |
|---|---|---|
| main | 44,122 / 44,143 = **99.95%** | 36,173 / 43,883 = **82.43%** |
| sub | 16,990 / 16,996 = **99.96%** | 5,451 / 6,426 = **84.83%** |
| torso | 27,048 / 27,084 = **99.87%** | 20,945 / 26,863 = **77.97%** |
| head | 14,136 / 14,664 = **96.40%** | 10,558 / 12,972 = **81.39%** |
| glove | 25,075 / 25,286 = **99.17%** | 19,084 / 24,955 = **76.47%** |
| foot | 25,321 / 25,364 = **99.83%** | 19,304 / 25,203 = **76.59%** |
| leg | 24,406 / 24,476 = **99.71%** | 18,419 / 24,244 = **75.97%** |
| shoulder | 23,503 / 23,550 = **99.80%** | 17,566 / 23,283 = **75.45%** |
| wing_mesh | 0 / 731 = **0%** | 0 / 0（本地无翅膀物品槽） |

集合级比较（49,644 个 NPC 双侧都有装备，剔除 `arrow`/`drakanarrow01` 与 `.cgf` 翅膀）：

- 网格集合完全相等：**40,509（81.6%）**
- 本地是真端子集：384
- 部分重叠：2,032
- 完全不相交：6,719

### 3.3 差异原因

- **版本变体**：`npc_lt_torso_lodge_l_ra_01` ↔ `..._02`、`npc_pl_*_guard_*_01` ↔ `..._lodge_*_02`、`npc_*_gabsub01a_01` ↔ `npc_*_t01_01`、`npc_*_idvritra01c_01` ↔ `npc_*_d_t01_01` 等；同一 NPC 在两个数据版本使用了不同网格。
- **投射物**：真端 `sub` 常放 `arrow`/`drakanarrow01`（约 4,000 条），本地没有对应物品；这类不应算装备差异。
- **本地独有盾牌/武器**：如 `npc_shield_gabsub01a_01`、`drakanshield01`，是本地物品化装备，真端用另一种网格。
- **翅膀**：真端 `wing_mesh` 是 `.cgf` 路径，本地没有 NPC 翅膀装备字段；`wing_always` 是布尔开关。

### 3.4 结论

- **能映射，且覆盖率足够做审计与可视化对齐**；不能直接拿网格名替换本地 `<equipment><item>`：本地 `NpcEquippedGear.init()` 会用物品 id 解析槽位、武器类型与属性，替换会改变 NPC 攻击类型和装备属性。
- 若要做“视觉逐 NPC 对齐”，建议新增一张 `mesh_alias` 表处理约 18% 的版本变体，并把 `arrow`/`wing_mesh` 排除在装备集合之外；翅膀需要单独的 `wing_mesh` 字段，而不是物品槽位。
- `appearance/*`（pc_type/hair_type/face_type/face_color/hair_color）在真端与已解包客户端 `npc_client` 表之间 100% 一致（有值部分），属于纯客户端外观，本地服务端无需建模。

---

## 四、stats@power 溯源

### 4.1 运行时消费者

- `NpcStatsTemplate.power`（`src/main/java/com/aionemu/gameserver/model/templates/stats/NpcStatsTemplate.java:44-45`）
- → `NpcGameStats.getMainHandMAttack()` = `getStat(StatEnum.MAGICAL_ATTACK, power)`（`NpcGameStats.java:277-279`）
- → `StatFunctions.calculateAttackDamage()`：当 `element != SkillElement.NONE` 时取 MAttack（`StatFunctions.java:222-232`）
- → `AttackUtil.calculateMagAttackResult()`（魔法自动攻击，`AttackUtil.java:222-232`）与 `AttackUtil.calculateSkillResult()` 中非物理攻击分支（`AttackUtil.java:570-590`）
- 物理自动攻击走 `getMainHandPAttack()`（本地 `main_hand_attack`），不使用 `power`。
- 另一条 NPC 法术路径 `AttackUtil.calculateMagicalSkillResult()` 用 `getMainHandPAttack().getBase()` 缩放（`AttackUtil.java:864`），因此并非所有 NPC 法术都吃 `power`。
- 实例缩放会乘 `RatePW`（`VisibleObjectSpawner.java:203,223,246`）。

### 4.2 与真端字段的关系

| 指标 | 结果 |
|---|---|
| 真端 `npcs.xml` 是否有 `<power>` | 否（360 个元素中无 power；客户端 `npc_client` 表也不含 `power`/`magical_attack`/`physical_attack`/`level`/`max_hp` 等战斗数值） |
| 与 `magical_attack` 完全相同 | 36 / 87,234 |
| 与 `magical_attack` 比值 | p5=0.037，p50=1.470，p95=4.026 |
| Pearson（原始 / log1p） | 0.223 / 0.261 |
| 与 `physical_attack` 完全相同 | 93 / 87,234 |
| 与 `exp` 完全相同 | 0 / 87,205 |

### 4.3 本地等级曲线证据

本地 87,982 条模板只有 **287 个不同 `power` 值**（min 1、max 1925、中位数 590），呈明显离散档位。1–60 级存在严格的分段线性序列：

| 等级段 | 段首值 | 每级增量 | 段末值 |
|---|---|---|---|
| 1–9 | 23 | +3 | 47 |
| 10–19 | 60 | +4 | 96 |
| 20–29 | 120 | +5 | 165 |
| 30–39 | 200 | +6 | 254 |
| 40–49 | 300 | +7 | 363 |
| 50–59 | 420 | +8 | 492 |
| 60 | 560 | — | — |
| 61–80 | 932 / 1232 等离散值 | — | — |

验证：1–60 级的每一个期望值都真实存在（出现次数 17–978），且在多数等级是最高频值（例如 L10=60 出现 422 次、L30=200 出现 249 次、L40=300 出现 749 次、L50=420 出现 978 次、L55=460 出现 910 次）。61 级以后曲线消失，`932`（10,096 条）与 `1232`（25,742 条）成为主导，属于终局内容的离散档位；`23`（18,338 条）是大量非战斗/占位 NPC 的默认值。

作为对照，真端同等级 `magical_attack` 的最高频值与本地曲线接近但不同：L10 57 vs 60、L20 118 vs 120、L30 202 vs 200、L40 289 vs 300、L55 462 vs 460、L60 564 vs 560。这支持“本地 `power` 是同一类等级曲线经取整/分档后的 Encom 版本”的推断，但**不是逐条投影**，尾部差异可达 4 倍。

### 4.4 结论与边界

- **可验证结论**：`power` 不是真端 5.8 `npcs.xml` 的投影；它是 Encom/AionEmu 血统的本地生成属性，1–60 级遵循上述分段线性曲线，61 级以上使用离散终局档位。
- **无法溯源的部分**：仓库内没有生成该曲线的脚本或表；已解包的客户端 `npc_client` 表不含 `power`/`magical_attack`/`physical_attack`/`level`/`max_hp` 等战斗数值；真端服务端反编译源码只读取 `magical_attack` 等字段，没有 `<power>`。因此“具体由哪个 Encom 版本/工具生成”无法在当前材料内闭合。
- **不建议对齐**：把 `power` 改成真端 `magical_attack` 会改变 NPC 魔法伤害基数，且两者中位数比 1.47、p95 4.03，属于战斗行为变更，不是数据一致性修复。

---

## 五、287 个真端独有元素分类

### 5.1 计数口径

`retail-only-elements.tsv` 共 329 个真端元素：

- 42 个有本地对应（`bound_radius`、`race`、`npc_type`、技能、掉落覆盖层等）；
- **287 个标为“无对应”**；
- 其中 3 个是审计伪元素：`__spawn_zonename__`、`__design_reference__`、`__ai_name_ref__`，实际字段 **284 个**。
- README 原记“285 个”，与本次复核的 287（含伪元素）存在口径差；本报告以 TSV 复核值为准。

### 5.2 分类统计

逐元素明细见 [`P4-retail-only-classification.tsv`](P4-retail-only-classification.tsv)。

| 顶层分类 | 子类 | 元素数 | 代表元素 | 本地承载/现状 |
|---|---|---|---|---|
| 已由其他子系统承载（129） | 掉落/战利品 | 19 | `drop_group_idx`、`common_drop`、`massive_looting_*`、`item`、`prob` | `npc_drops` / `NpcDrop` / `DropGroup` / `MassiveLooting`；现金掉落未实现 |
| | 商店/仓库/交易/服务 | 68 | `buy_price_rate`、`tab/atab/etab`、`*warehouse*`、`bindstone_*` | `TradeListTemplate`、仓库/物品服务 Java 实现；NPC 功能入口多由 AI 硬编码 |
| | 传送/副本/区域 | 11 | `instance_entry`、`ins_creation_id_*`、`direct_portal_npc_type`、`pvpzone*` | `TeleporterData`、`portals/`、instance handlers |
| | 任务/对话/社交/图鉴 | 15 | `npc_title`、`quest_ai_name`、`talk_delay_time`、`subdialog_type`、`monsterbook_*` | 任务引擎/DSL、`TalkInfo`、`npc_factions`、`monster_book`；本地已有 `title_id` |
| | 技能/战斗覆盖层 | 13 | `stat_ratio`、`limit_attribute_reduce_value`、`magical_skill_boost_resist`、`skill_*` | `npc-skills.xml`、`compact/npc-combat.xml`、`NpcStatsTemplate` |
| | AI 投影 | 3 | `sensory_range_short`、`move_speed_return`、`decrease_sensory_range_return` | `compact/ai/npc-ai-parts`（`RetailAiData`） |
| 纯外观/客户端（40） | 模型/外观 | 36 | `scale`、`weapon_scale`、`mesh`、`appearance/*`、`visible_equipments/*` | 客户端 `npc_client` 数据/物品网格；服务端无需复制 |
| | 客户端 UI/本地化 | 4 | `deadbody_name_id`、`display_msg_attacked`、`display_msg_die`、`omit_system_msg` | 纯客户端 |
| 已落地（本期，87） | 战斗数值 | 87 | `elemental_defend_*`、`hit_count`、`dp_reduce`、`concentration`、`master_item_*`（56 个） | `npc-combat-elements.xml` 87,169 行 + `npc-master-item-bonus.xml` 342 行；详见第九节 |
| 尚未建模（31） | AI 感知/威胁 | 20 | `max_enemy_count`、`can_see_invisible`、`can_see_polymorph`、`hate_decrease_*`、`chase_flying` | 无；可与真端 AI 感知项目合并 |
| | PvP/深渊/奖励 | 4 | `abyss_point`、`abyss_rank`、`glory_point`、`warpoint` | 玩家侧系统存在，NPC 模板字段未接 |
| | 服务开关/功能入口 | 3 | `no_penalty`、`siege_involve`、`ignore_contribution` | 无数据化入口 |
| | 其他/元数据 | 4 | `mobile_event`、3 个审计伪元素 | 无/忽略 |

> 注：`scale` 归入“纯外观/客户端”是就 NPC 模板字段而言；它同时被真端服务端用于缩放 bound_radius，本地已通过 `npc-ai-parts@model_scale` 获得同一值。若做 bound_radius 对齐，必须把 `scale` 纳入公式（见第二节）。

### 5.3 值得单独立项

1. **战斗数值（87 个）— 本期已落地**：`elemental_defend_*`（6）、伤害 trim/暴击减免（6）、`hit_count`/`dp_reduce`/`concentration`/`dp`、`master_item_*`（56，召唤/宠物继承主人属性）。数据层、属性注入与主要消费者见第九节；`disk_type` 复核为客户端分类字段（`D1`–`D7`/`guard`/`merchant`/`e_earth` 等），不计入战斗数值。
2. **AI 感知/威胁（20 个）**：`can_see_invisible`、`can_see_polymorph`、`undetectable`、`max_enemy_count`、`hate_decrease_*`、`chase_flying`、`chase_range_fromsp`。与已完成的 retail AI 感知迁移同一域，建议合并。
3. **PvP/深渊奖励（4 个）**：`abyss_point`、`abyss_rank`、`glory_point`、`warpoint`。需要奖励设计，不建议从模板直接照抄。
4. **外观补充（scale/weapon_scale/wing_mesh）**：`scale` 已可从 AI 投影获得；`weapon_scale` 与 `wing_mesh` 本地无字段，若做客户端外观逐 NPC 对齐需补数据通道。
5. **低优先**：UI/本地化、服务开关、伪元素。

---

## 六、下一步可执行清单

| # | 任务 | 前置 | 验收 |
|---|---|---|---|
| 1 | **已完成**：裁定本地 `bound_radius` 为「全长」语义，写入 `BoundRadius` 类注释 | 战斗/移动域决策 | 文档 + 单测 |
| 2 | **已完成**：修正消费者（`sensoryBoundOffset`、`GeoService`、`PositionUtil`、`WalkerGroup`、`SM_NPC_INFO`） | #1 | 单测：感知偏移/碰撞与真端公式一致 |
| 3 | **已完成**：新增 `npc-bound-radius.xml` 覆盖层，按 `raw × scale / 100` 生成三轴 | #1、#2 | 87,705 行；`NpcBoundRadiusDefinitionLoaderTest` |
| 4 | 实机回归：刷怪、贴墙、视线、感知、寻路、副本 | #3 | 抽样 + 全库冒烟；无卡怪/抽搐/超远感知 |
| 5 | 建立 `mesh_alias` 表，处理 `visible_equipments` ↔ `name_desc` 的 18% 变体；排除 `arrow`/`wing_mesh` | 无 | 逐槽位同名率提升到 >95%（目标） |
| 6 | 若需要视觉翅膀，新增 `wing_mesh` 字段/数据通道 | #5 | 客户端显示与真端一致 |
| 7 | `power` 保持现状并记录为 Encom 本地曲线；若要战斗对齐，单独立项映射 `magical_attack` | 战斗设计 | 伤害回归通过 |
| 8 | 战斗数值与 AI 感知/威胁两个专项 | 各自设计 | 见第五节 |
| 9 | **已完成**：战斗元素覆盖层 `npc-combat-elements.xml`（87,169 行）+ `NpcCombatElementsDefinitionLoader` | 战斗公式评估 | `NpcCombatElementsDefinitionLoaderTest` |
| 10 | **已完成**：属性注入 `NpcGameStats.getStat` / `getMAccuracy`、`hit_count`、`limit_attribute`、元素免疫 | #9 | 单测 + 公式路径核对 |
| 11 | **已完成**：主人物品继承 `npc-master-item-bonus.xml`（342 行）+ `MasterItemBonusApplier` | #9 | `NpcMasterItemBonusDefinitionLoaderTest`、`MasterItemBonusApplierTest` |
| 12 | 实机回归：战斗元素对伤害/抗性/召唤继承的影响 | #9–#11 | 见 `ACCEPTANCE.zh-CN.md` 第八节 |

**当前阻塞项**：

- `power`：无真端上游，且替换会改变战斗行为，BLOCKED（建议保持）。
- 翅膀：`wing_mesh` 无本地物品通道，视觉逐 NPC 对齐 BLOCKED。
- `visible_equipments` 的 18% 变体需要 alias 表，非阻塞但需额外工作。

---

## 七、复现方法

1. 以 `xml.etree.ElementTree.iterparse` 直接流式读取 UTF-16 的 `58Server/Map/XML/npcs.xml`（expat 处理 DTD 内部实体），按 `<npc>` 抽字段；也可先 `iconv -f UTF-16 -t UTF-8` 再解析。
2. 解析 8 个 `npc_template_*.xml` 分片，按 `npc_id` 关联；解析 `item_template_*.xml` 的 `id`/`name_desc`/`slot`。
3. `bound_radius` 按 `raw`、`raw*scale/100`、`raw*scale/200` 三组公式逐轴分类；同时输出三轴联合分组。
4. `equipment` 按 `name_desc` 与 `visible_equipments/<slot>` 做逐槽位和集合比较；集合比较剔除 `arrow`/`drakanarrow01`/`.cgf`。
5. `power` 按等级分组统计众数、频次与分段线性序列，并与真端 `magical_attack` 做精确匹配、比值分位与 Pearson/log-Pearson。
6. 所有脚本为一次性工具，未入库；结果仅保留本目录的 Markdown 与 TSV。

---

## 八、关键文件索引

- 本地模型/运行时：
  - `src/main/java/com/aionemu/gameserver/model/templates/BoundRadius.java`
  - `src/main/java/com/aionemu/gameserver/model/templates/npc/NpcTemplate.java`
  - `src/main/java/com/aionemu/gameserver/model/templates/npc/NpcCombatElements.java`
  - `src/main/java/com/aionemu/gameserver/model/templates/npc/NpcMasterItemBonus.java`
  - `src/main/java/com/aionemu/gameserver/model/templates/stats/NpcStatsTemplate.java`
  - `src/main/java/com/aionemu/gameserver/model/stats/container/NpcGameStats.java`
  - `src/main/java/com/aionemu/gameserver/model/stats/container/MasterItemBonusApplier.java`
  - `src/main/java/com/aionemu/gameserver/model/stats/container/SummonGameStats.java`
  - `src/main/java/com/aionemu/gameserver/model/stats/container/SummonedObjectGameStats.java`
  - `src/main/java/com/aionemu/gameserver/model/stats/container/HomingGameStats.java`
  - `src/main/java/com/aionemu/gameserver/model/stats/container/TrapGameStats.java`
  - `src/main/java/com/aionemu/gameserver/world/geo/GeoService.java`
  - `src/main/java/com/aionemu/gameserver/ai2/handler/CreatureEventHandler.java`
  - `src/main/java/com/aionemu/gameserver/controllers/attack/AttackUtil.java`
  - `src/main/java/com/aionemu/gameserver/utils/stats/StatFunctions.java`
  - `src/main/java/com/aionemu/gameserver/model/items/NpcEquippedGear.java`
  - `src/main/java/com/aionemu/gameserver/dataholders/loadingutils/NpcCombatDefinitionLoader.java`
  - `src/main/java/com/aionemu/gameserver/dataholders/loadingutils/NpcBoundRadiusDefinitionLoader.java`
  - `src/main/java/com/aionemu/gameserver/dataholders/loadingutils/NpcCombatElementsDefinitionLoader.java`
  - `src/main/java/com/aionemu/gameserver/dataholders/loadingutils/NpcMasterItemBonusDefinitionLoader.java`
  - `src/main/java/com/aionemu/gameserver/spawnengine/VisibleObjectSpawner.java`
- 本地数据：
  - `src/main/resources/aion/data/static_data/npcs/npc_template_*.xml`
  - `src/main/resources/aion/data/static_data/items/item/item_template_*.xml`
  - `src/main/resources/aion/definitions/compact/ai/npc-ai-parts/*.xml`
  - `src/main/resources/aion/definitions/compact/npc-combat.xml`
  - `src/main/resources/aion/definitions/compact/npc-bound-radius.xml`
  - `src/main/resources/aion/definitions/compact/npc-combat-elements.xml`
  - `src/main/resources/aion/definitions/compact/npc-master-item-bonus.xml`
- 真端（工作区同级，不纳入仓库）：
  - `58Server/Map/XML/npcs.xml`
  - `58Server/server58-source/NPCServer_NPCSvr64/classes/Misc/ObjectClass.cpp`
- 审计产物：
  - `docs/static-data/npc-template-retail-parity/README.zh-CN.md`
  - `docs/static-data/npc-template-retail-parity/field-diff-summary.tsv`
  - `docs/static-data/npc-template-retail-parity/retail-only-elements.tsv`
  - `docs/static-data/npc-template-retail-parity/P4-retail-only-classification.tsv`

---

## 九、P4 战斗元素与主人物品继承（2026-10-19 落地）

### 9.1 数据层

| 覆盖层 | 行数 | 内容 |
|---|---:|---|
| `definitions/compact/npc-combat-elements.xml` | 87,169 | 30 个标量/列表战斗元素：`hit_count`、`dp`、`dp_reduce`、`concentration`、`magical_hit_accuracy`、`cancel_level`、物理/魔法暴击减免与暴击伤害减免、`proc_reduce_rate`、`noreducespell_immune`、4 个 `npc_pve_*`、6 个 `elemental_defend_*`、`limit_attribute`、`elemental_immunity`、`abnormal_resist` / `abnormal_penetration` |
| `definitions/compact/npc-master-item-bonus.xml` | 342 | 28 组 `master_item_*_ratio/limit`，仅写 ratio/limit 非零项 |

口径：

- 只对本地与真端共有的 87,692 个 id 建行；真端独有 42 个不写入。
- 标量元素为 0（`hit_count` 为 1）时省略；子元素仅在非零时写入。
- `disk_type` 复核为客户端分类字段（`D1`–`D7`、`guard`、`merchant`、`e_earth` 等），不属于战斗数值，未进入覆盖层。
- `abnormal_immunity_list` 已由 `NpcTemplate.abnormal_immunity` 位图承载，不重复写入。
- `elemental_immunity` 归一化为 `SkillElement` 集合：`erfire/erwater/erair/erearth/erlight/erdark` 分别映射到火/水/风/地/光/暗。
- `abnormal_penetration_name` 中的 `Stat_ArAll` / `Stat_ArStunLike` 是组标记，本地没有组穿透属性，加载时展开为具体 `*_RESISTANCE_PENETRATION` 集合并去重。

### 9.2 属性映射与消费者

| 真端元素 | 本地属性 | 消费者 |
|---|---|---|
| `elemental_defend_fire/water/air/earth` | `FIRE/WATER/WIND/EARTH_RESISTANCE` | `CreatureGameStats.getMagicalDefenseFor` → `StatFunctions.applyMagicalDefenseModifiers` |
| `elemental_defend_light/dark` | `ELEMENTAL_RESISTANCE_LIGHT/DARK` | 同上（与 `SkillElement.getResistanceForElement` 对齐） |
| `hit_count` | `NpcTemplate.getHitCount()` | `AttackUtil.getPhysicalHitCount`（NPC 普攻分段） |
| `dp` | `MAXDP` | NPC 侧无战斗消费者（`MAXDP` 仅在玩家 `PlayerGameStats` 中读取）；保留数据 |
| `concentration` | `CONCENTRATION` | 施法专注/打断判定（既有） |
| `magical_hit_accuracy` | `MAGICAL_ACCURACY` | `NpcGameStats.getMAccuracy` → 法术命中/穿透 |
| `physical/magical_critical_reduce_rate` | `PHYSICAL/MAGICAL_CRITICAL_REDUCE_RATE` | `StatFunctions.calculateEffectiveCritical` |
| `physical/magical_critical_damage_reduce` | `PHYSICAL/MAGICAL_CRITICAL_DAMAGE_REDUCE` | `AttackUtil.calculateWeaponCritical` |
| `proc_reduce_rate` | `PROC_REDUCE_RATE` | 神石触发减伤（`GodStone`） |
| `npc_pve_attack_ratio_*` / `npc_pve_defend_ratio_*` | `PVE_ATTACK/DEFEND_RATIO_PHYSICAL/MAGICAL` | `StatFunctions.adjustDamages` |
| `limit_attribute` | `PVE_ATTACK_RATIO_TYPE_A..E` | `StatFunctions.applyLimitAttributeBonus`（显式优先，`race` 回退） |
| `elemental_immunity` | `Set<SkillElement>` | `applyMagicalDefenseModifiers` / `calculateMagicalOverTimeSkillDamage` 直接归零 |
| `abnormal_resist_name/value` | `*_RESISTANCE`；`Stat_ArAll`→`ABNORMAL_RESISTANCE_ALL`，`Stat_ArStunLike`→`STUNLIKE_RESISTANCE` | `EffectTemplate.calculateEffectResistRate` |
| `abnormal_penetration_name/value` | `*_RESISTANCE_PENETRATION` | 同上 |
| `dp_reduce` / `cancel_level` / `physical_damage_trim` / `magical_damage_trim` / `noreducespell_immune` | 数据字段 | 暂无消费者；保留供后续机制 |

注入方式：`NpcGameStats.getStat(StatEnum, 0)` 在调用方未提供基础值时替换为覆盖层值；这样 `getStrikeResist`、`getSpellResist`、`getConcentration`、`getMagicalDefenseFor`、PvE 比率与异常抗性等既有公式无需改动即可消费。`SummonGameStats` 单独做同样的注入。

### 9.3 主人物品继承

真端链路：`Npc_SetAttrBonus_ByMasterItemBonus` → `FUN_140b14730` 读取主人 `BonusAttrSum_MasterItem` → `Npc__SetAttrBonus_ByMasterItemBonus` → `FUN_140b1ae30` 按模板 `master_item_*_ratio/limit` 逐属性计算 → 结果以 limit 封顶后并入召唤物属性。

本地实现：

- `npc-master-item-bonus.xml` 只为 342 个召唤物模板建行；
- `MasterItemBonusApplier` 在 `SummonGameStats`、`SummonedObjectGameStats`、`HomingGameStats`、`TrapGameStats` 的 `getStat` 中生效；
- 主人侧映射：命中/暴击/物攻读主手属性（`MAIN_HAND_ACCURACY` / `MAIN_HAND_CRITICAL` / `MAIN_HAND_POWER`），其余属性同名读取；
- 计算公式：`min(masterItemBonus × ratio / 100, limit)`；`masterItemBonus` 由 `getItemStatBoost` 汇总主人装备与魔石加成；
- 模板无配置时保留原有固定比例回退，未被真端覆盖的召唤物行为不变。

近似与边界：

- 真端算式还包含 `base × ratio% × masterPercent%` 与 `first_level_attr` 项；本地以主人装备加成总额近似，`limit` 在绝大多数模板上起主导作用。
- `master_item_*` 的 342 个模板均为召唤/陷阱/追踪弹类；未对普通 NPC 生效。
- `npc-master-item-bonus.xml` 的 ratio/limit 与真端逐值一致；运行时只做封顶，不写回模板。

### 9.4 回归闸门

- `NpcCombatElementsDefinitionLoaderTest`：临时覆盖层断言 + 真实数据 87,169 行与样本 200000/201000。
- `NpcMasterItemBonusDefinitionLoaderTest`：临时覆盖层断言 + 真实数据 342 行与样本 201000/201001。
- `MasterItemBonusApplierTest`：主人侧主手属性映射。
- 既有 `NpcTemplateRetailAlignmentTest` / `NpcAttackDefinitionLoaderTest` / `NpcBoundRadiusDefinitionLoaderTest` 继续通过。

### 9.5 覆盖计数（逐元素）

`npc-combat-elements.xml` 共 87,169 行，逐元素非默认值计数如下（0 值省略）：

| 元素 | 行数 | 元素 | 行数 |
|---|---:|---|---:|
| `abnormal_resist`（子元素） | 171,460 | `abnormal_penetration`（子元素） | 23,747 |
| `magical_hit_accuracy` | 85,831 | `dp` | 80,265 |
| `cancel_level` | 72,721 | `physical_critical_reduce_rate` | 60,167 |
| `magical_critical_reduce_rate` | 52,460 | `elemental_defend_earth` | 40,174 |
| `elemental_defend_fire` | 40,041 | `elemental_defend_air` | 40,006 |
| `elemental_defend_water` | 40,026 | `elemental_defend_light` | 38,471 |
| `elemental_defend_dark` | 38,469 | `dp_reduce` | 32,726 |
| `proc_reduce_rate` | 29,765 | `noreducespell_immune` | 6,239 |
| `physical_damage_trim` | 1,229 | `magical_damage_trim` | 1,227 |
| `limit_attribute` | 7,600 | `npc_pve_attack_ratio_physical` | 115 |
| `npc_pve_attack_ratio_magical` | 62 | `npc_pve_defend_ratio_physical` | 62 |
| `npc_pve_defend_ratio_magical` | 62 | `elemental_immunity` | 167 |
| `physical_critical_damage_reduce` | 21 | `magical_critical_damage_reduce` | 9 |
| `concentration` | 6 | `hit_count` | 1（`283021`，值 2） |

> `elemental_defend_*` 存在 2,675 行负值（真端允许负元素防御，表现为元素弱点）；加载器按有符号整数保留，伤害公式直接按负值放大元素伤害。
