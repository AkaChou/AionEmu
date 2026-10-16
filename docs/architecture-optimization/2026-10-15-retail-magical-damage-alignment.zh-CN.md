# 真端法系伤害公式对齐报告（① 随机伤害 + ② MoveBonus）

> 依据：零售参考二进制 `58Server/MainServer/Server64.exe`（42,184,704 B，ImageBase 0x140000000）与 Ghidra 源码 `server58-source/MainServer_Server64/`。
> 核心函数：`Creature_CalcMagicalSkillAttackDamage`（0x14036f850，28 参数，`include/Misc/Creature.h:338`）。
> 本文日期：2026-10-15。状态：① ② 已实施，未编译验证（PENDING）。

## 0. 真端调用链（已确认的顺序）

```
boost 因子（魔增/知识）→ 取整 → basic 段随机 → add 段随机 → 求和
→ 攻击/技能加成（param_5 mask 门控）→ MoveBonus（攻击者伤害%）→ 防御/抗性 → PvP 比例
```

本地顺序 `boost → BOOST_SPELL_ATTACK → basic 随机 → add 随机 → 求和` 与真端等价。

## 1. ① SkillRandomDamageTable 随机伤害（已实施）

真端读取 `skill_randomdamages.xml`（DAT_1428b7410），20 档，掷点 `Rnd(0,99)/5`；
`SkillRandomDamageApplyType` 位掩码：1=BasicDmg、2=AddDmg、3=All。

| 变更点 | 文件 | 内容 |
|---|---|---|
| 解析入口 | `AttackUtil.java:890` | 仅 `SpellAttackInstantEffect` 读 `effectN_reserved3/reserved4`（经 `RetailSkillFields`），其余效果按 0（不随机） |
| 解析器 | `AttackUtil.java` | `parseRandomDamageType`（仅单字符 1-9）、`parseRandomDamageApplyType`（basicdmg/adddmg/all，大小写不敏感） |
| 取值 | `Effect.java:617` | 新增 `getCurrentEffectTemplate()` |
| 应用 | `StatFunctions.java` | 15 参 `calculateMagicalSkillDamage` 双随机插入点：basic 段掷一次、add 段独立掷一次，各受 ApplyType 位门控 |

## 2. ② MoveBonus 表 DAT_141069ed0（已实施）

### 2.1 真端表（8 行 × 7 int，行 = MoveState）

| 行(MoveState) | col0 伤害% | col1 防御% | col2 抗性固定 | col3 招架+500 | col4 格挡+500 | col5 格挡+300 | col6 闪避 |
|---|---|---|---|---|---|---|---|
| 0 静止 | 0 | 0 | 0 | — | — | — | 0 |
| 1 前进 | +10 | −20 | −50 | — | — | — | 0 |
| 2 后退 | −20 | 0 | 0 | ✓ | ✓ | — | 0 |
| 3 | −20 | 0 | 0 | ✓ | ✓ | — | 0 |
| 4 侧移 | −20 | 0 | 0 | — | — | ✓ | 0 |
| 5 | +10 | 0 | 0 | — | — | ✓ | 0 |
| 6 | −20 | 0 | 0 | ✓ | ✓ | ✓ | 0 |
| 7 | 0 | 0 | 0 | — | — | — | 0 |

col5（+300）语义沿用反汇编判读，对应 DEBUG 串族中的格挡变体。

### 2.2 MoveState 枚举与档位

真端 `& 7`：0=静止、1=前进(≤45°)、2=后退(>135°)、4=侧移(45°~135°)，3/5/6 为位或组合。
提供者：`Creature` 虚函数 vtable[0x5e8]（槽 189），**仅 Player 覆写**（`FUN_140578eb0`，3° 步长 + 666ms/1332ms 滞后）；NPC 恒 0。

本地映射：`PlayableMoveController.setNewDirection` 的 `movementHeading`（0..7，−1=未动），
公式 `(int)(((heading*3) − h)/45)` 因向零截断使 **档 0 == |角度差| < 45°**，与真端前进带一致。

### 2.3 对齐变更（`StatFunctions.java` / `PlayableMoveController.java`）

真端 MoveState 枚举（0=静止、1=前进、2=后退、4=侧移，滞后位或组合 3/5/6）已完整落地，
本地 8 向档位在 `setNewDirection` 内映射为真端枚举后写入滞后状态机。

| 真端列/机制 | 本地实现 | 状态 |
|---|---|---|
| col0 伤害% | `applyMovementDamageBonus(state)`：行 1/5 → ×1.1，行 2/3/4/6 → ×0.8；法系在 `calculateMagicalSkillDamage` 防御阶段前施加（param_5 & 8 顺序），物理在 `adjustDamages`（useMovement） | 已修正并接入法系 |
| col1 防御% | `applyMovementStatModifier`：仅行 1 ×0.8（PHYSICAL_DEFENSE/MAGICAL_DEFEND） | 对齐 |
| col2 抗性固定 −50 | `getMovementResistanceOffset(1) == −50`，接在元素抗性上 | 对齐 |
| col3 招架 +500 | 行 2/3/6（`(state & 2) != 0`），经 `getMovementModifier` 接入 dodge/parry/block 判定 | 对齐 |
| col4 格挡 +500 | 行 2/3/6；col5 格挡 +300 行 4/5/6，两列可叠加（行 6 = +800） | 本轮补齐 |
| col6 闪避 | 表全 0，本地 EVASION +300 case 已删除 | 本轮删除表外项 |
| 双状态滞后 | `recordRetailMoveState`/`getRetailMoveState`（FUN_140578e10/eb0 语义）：新方向 666ms 后生效、旧方向存活 1332ms，位或合并 | 本轮实现 |
| MOUSE 恒前进 | `setNewDirection` 检查 `movementMask & MovementMask.MOUSE`，鼠标移动跳过角度计算直接判前进（User_CalcMoveState 语义） | 本轮实现 |
| vtable 槽 189 仅 Player | `movementSector(Creature)` 非玩家返回 -1（不参与） | 对齐 |

### 2.4 其他对齐（本轮）

- **法系最终伤害下限 1**：`AttackUtil` 法系路径在 PvP/limitAttr 之后钳制 `Math.max(1, damage)`
  （真端 `Creature_CalcMagicalSkillAttackDamage` 尾段）。
- 真端防御段确证：col1 乘在受击者魔法抵抗上再 /10 从抵抗差中扣除、col2 进 `(col2+param_12)/(attackerScale/1000)`
  与等级宽限表比较下限——本地既有 `applyMagicalDefenseModifiers` + `applyElementalDefenseLowerCap` 数值等价，未改。

## 3. 遗留偏差（真端有、本地未做或有意保留）

1. **param_5 & 4 回调**（技能对象 vtable[0xe10]，槽 450）：技能级伤害修改虚函数，覆写者需逐技能考古；
   默认类无操作，仅个别技能受影响，未实施。
2. **目标侧最终回调**（vtable[0x5e0]，槽 188）：已核实 Creature 与 Player 的槽 188 均为 `return 0`
   （FUN_1400b47b0），调用条件 `!= 0` 永假——真端默认关闭，本地无需实现。
3. **DOT 不走此管线**：真端调用点仅 SpellAttack/Drain/DelayedSpellAttack/SignetBurst/Enslave/ProcAttack，
   DoT 使用独立公式，本地 `calculateMagicalOverTimeSkillDamage` 独立成函数与此一致，无需接 col0。
4. **NPC 施法分支**：`roll * (pct/100 + 1) * (命中穿透乘子 + 1)` 已实现（`calculateMagicalAccuracyPenetration`，
   与"每 100 点 MR 差 = 10%"同源）；触发条件真端为 effect 字段（偏移 0x2ec）==1，本地为
   `Func.PERCENT && effector instanceof Npc`，字段加载器未考古到，触发源保留本地判定。
5. **PvP 疲劳系数**：真端 `0.42 * fatigue`，本地固定 0.42（无疲劳系统数据源）。
6. **SPEED 移动减速**：×0.8/×0.6 为本地表外手感项，真端 MoveBonus 表无此列（保留）。
7. **魔增上限**：维持 `32767` 默认（此前误改 100000 已回退），配置可调。
8. **随机门控与掩码**：真端 noReduce（技能 0xd72 标志）清零调用掩码（ProcAttack 恒 0、SpellAttack=0x1f、
   SignetBurst=0x1e）；本地已将两段随机纳入 noReduce 门控；非 SpellATK_Instant 效果的随机字段
   布局不同，仍按 0 处理。
9. **元素抗性分母已修正**（2026-10-16 第二轮核实）：真端 `FUN_14036bbb0` 条件为双方等级均 >50 时
   `1300 + (min(攻,守等级) − 50) × 10`，与目标类型无关；本地原误用 `target instanceof Player` 门控，
   已改为统一公式（`elementalDefenseDenominator`）。影响：PC 攻击 >50 级 NPC 时法术伤害提高约 4~8%。

## 3.1 公式形状存疑项（需运行时数值比对裁决，未改动）

真端 PC 分支反编译：`base = skillDamage × (0x760值/100 + 命中穿透)`，BOOST_SPELL_ATTACK 段为
`(0x11c4 + 100)/100 × dmg + 0x11C8`；本地为 `×(knowledge/100 + magicBoost/1000)` 加法 +
BOOST_SPELL_ATTACK stat 直加。关键未知：

- `0x760`（ItemClass/Npc.cpp 模板写入）与 `0x11c4`（`SkillEffectBoostSpellAttackEffect` 累加）的
  存储单位——若存 10 倍百分数，真端 `(x+100)/100` 与本地公式在 K=100 时等价，K≠100 时发散；
- 两公式形状（真端疑似乘法分配 vs 本地加法）无法静态裁决。

`FUN_14036b890`（防御段 mresCalc）与 `FUN_14036beb0`（PvE 比例）的 Ghidra 反编译返回值丢失，
无法逐位核实，本地公式数值自洽予以保留。`FUN_14036f220`（法术抵抗 gate）本地已有 MR 判定等价机制。

## 4. 验收（已通过）

第一轮（① 随机伤害 + ② MoveBonus + 枚举化/滞后/MOUSE/col0/col5/下限 1）：
IDEA 原生 JUnit 运行（2026-10-16 授权执行）：

- `StatFunctionsTest`：19/19 通过（真端 MoveState 枚举语义 ×2 重写通过）
- `AttackUtilTest`：10/10 通过

新增/修改测试：`AttackUtilTest.parsesRetailRandomDamageApplyTypesCaseInsensitively`、
`AttackUtilTest.acceptsOnlyRetailRandomDamageTypes`、
`StatFunctionsTest.appliesRetailMovementCombatStatTable`（真端 MoveState 枚举语义）、
`StatFunctionsTest.appliesRetailMoveBonusDamageAndResistanceColumns`（行号语义 + col5 叠加 + EVASION 无修正）。

第二轮（遗留项：NPC 命中穿透、noReduce 随机门控、滞后单测）：
IDEA 原生 JUnit 运行（2026-10-16 授权执行），全部通过：

- `PlayableMoveControllerTest`：4/4 通过（666/1332ms 边界 ×4）
- `AttackUtilTest`：10/10 通过（`usesRetailNpcPercentageSpellBaseAndOneTimeBoostOrder` 补穿透断言）
- `StatFunctionsTest`：19/19 通过（随机门控回归）

第三轮（外围函数核实：分母条件修复）：
IDEA 原生 JUnit 运行（2026-10-16 授权执行），全部通过：

- `StatFunctionsTest`：20/20 通过（新增 `scalesElementalDefenseDenominatorBySharedLevelsAboveFifty`）
