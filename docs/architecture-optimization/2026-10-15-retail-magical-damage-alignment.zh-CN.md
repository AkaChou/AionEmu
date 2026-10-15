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

### 2.3 对齐变更（`StatFunctions.java`）

| 真端列 | 本地实现 | 状态 |
|---|---|---|
| col0 伤害% | `applyMovementDamageBonus(sector, v)`：sector==0 → ×1.1，其余合法档 → ×0.8，<0（NPC/未动）不变；接在 `adjustDamages` 末尾 | 本会话修正（原前进窗 90° 过宽） |
| col2 抗性固定 −50 | `getMovementResistanceOffset(0) == −50`，接在 `applyMagicalDefenseModifiers` 元素抗性上 | 本会话修正（原为 ×0.5） |
| col1/col3~col5 | `applyMovementStatModifier` 保留：PHYSICAL_DEFENSE/MAGICAL_DEFEND h==0 ×0.8、PARRY/BLOCK h 3..5 +500；删除元素抗性 ×0.5 case | 修正 |
| vtable 槽 189 仅 Player | `movementSector(Creature)` 非玩家返回 −1（不参与） | 对齐 |

## 3. 遗留偏差与未实施项（③ 范围外/表外）

1. **MoveState 滞后**：真端 666ms/1332ms 平滑；本地即时读取。
2. **MOUSE 移动**：真端鼠标移动恒判前进；本地纯按角度算。
3. **施加顺序**：真端在防御计算**之前**施加 MoveBonus；本地 `adjustDamages` 在其后（乘法可交换，但防御是除法时结果有别）。
4. **表外保留**：本地 EVASION h≠0&&h≠4 +300、SPEED ×0.8/×0.6 不在真端 MoveBonus 表内（真端 col6 全 0），暂保留未删。
5. **物理侧 col3/col4/col5**（招架/格挡）未接入物理伤害路径。
6. **③ 结构项未实施**：魔法防御 `(1+t4/100)` 耦合列、自目标跳过增幅、NPC 施法者分支（`(int)boost/1000` 与 `roll(min,max)*(knowledge/100+1)`）、param_5 四阶段门控含 vtable[0xe10]、PvP 硬编码 0.42、武器来源增幅/抗性覆盖、最终伤害下限 1。
7. **魔增上限**：维持 `32767` 默认（此前误改 100000 已回退），配置可调。

## 4. 验收（PENDING，未经授权未执行）

```bash
mvn -q -Dtest=AttackUtilTest,StatFunctionsTest test
```

新增/修改测试：`AttackUtilTest.parsesRetailRandomDamageApplyTypesCaseInsensitively`、
`AttackUtilTest.acceptsOnlyRetailRandomDamageTypes`、
`StatFunctionsTest.appliesRetailMovementCombatStatTable`（FIRE_RESISTANCE 期望回 1000）、
`StatFunctionsTest.appliesRetailMoveBonusDamageAndResistanceColumns`。
