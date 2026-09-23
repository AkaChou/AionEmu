# NPC 与召唤物的非攻击扣血同步（2026-09-23）

承接上一轮 `2026-09-23-npc-direct-hp-sync.zh-CN.md` 的第 5 节「已知残留」：
`AbstractHealEffect` 的负治疗量与 `HpUseAction` 的施法耗血仍从 `reduceHp` 直调扣血，全链路静默。

## 1. 语义决策（用户确认：保留扣血，只补同步）

调查中用户质疑「治疗量小于 0，则不加血，但不应该扣血吧」。核实数据后确认扣血是**有意行为**，不改：

| 证据 | 内容 |
|---|---|
| 技能数据 | `<healinstant>` 380 处中 7 处负值，全部是 `EL_Unsummon`（精灵星解除召唤）3732–3738，`value="-10" percent="true"` |
| 客户端数据 | retail 技能表同值 `<f i="36" v="-10" />` |
| 作用对象 | 技能组 `<properties first_target="MYPET" …/>`（`groups.xml:3287`）→ 命中**主人的召唤物** |
| 代码侧 | `AbstractHealEffect.calculate:89-91` 已有专门的"不扣死"钳位 `finalHeal = max(finalHeal, -currentValue)` |

结论：负治疗量是"解除召唤时消耗召唤物生命"的零售语义，**保留扣血，只补客户端同步**。

## 2. 缺口清单（改造前的四路对照）

| 路径 | 玩家 | NPC | 召唤物 |
|---|---|---|---|
| 攻击 | ✅ | ✅ `NpcController.onAttack:460` | ✅ `SummonController.onAttack:170` |
| 治疗 | ✅ | ✅ `onIncreaseHp` | ✅ `onIncreaseHp` |
| 直接设血 | ✅ | ✅ 上轮已修 | 无调用点 |
| **非攻击扣血（本轮）** | ✅ `PlayerLifeStats.onReduceHp:38` 已完整同步 | ❌ `onReduceHp()` 空 | ❌ `onReduceHp()` 空；**面板与血条都不更新** |

### 2.1 需改造的三个调用点

全仓 `reduceHp(` 调用点已枚举并二次核查：

- `skillengine/effect/AbstractHealEffect.java:130` —— 负治疗量扣血
- `skillengine/action/HpUseAction.java:53` —— 施法消耗 HP（63 处 `<hpuse>`，其中 24 个 skill_id 命中 `npc-skills.xml`）
- `skillengine/periodicaction/HpUsePeriodicAction.java:41` —— 周期 HP 消耗（当前数据不可达，API 一并收口）

### 2.2 明确不动

- `CreatureController:249`（攻击路径）、`:569`（`die()`，死亡包自处理）
- `ZoneLevelService:92`、`StatFunctions:958,964`、`Battleground:988,1006,1022`（全玩家侧，通道已完整）

## 3. 实现

### 3.1 基类只留一个诚实的直通入口

`CreatureLifeStats` 在 `reduceHp` 之后新增：

```java
public int reduceHpFromEffect(int value, Creature attacker) {
    return reduceHp(value, attacker);
}
```

默认不广播——玩家侧同步仍由 `onReduceHp` 独占，避免双包。javadoc 写明这是非攻击流程入口，子类可覆写补同步。

采用**子类覆写整个入口**而非"基类模板方法 + 钩子"：与 `NpcLifeStats` 既有的三个出口
（`setCurrentHp` / `setCurrentHpPercent` / `rescaleCurrentHp`）同形，让门禁的"出口声明在子类"
不变式自然扩展。

### 3.2 NPC 侧（`NpcLifeStats`）

复用已有守卫 `canSyncDirectHpSet()` 与判据 `hasVisibleHpChange()`：
未 spawn 短路 → 读前后百分比 → `super.reduceHpFromEffect(...)` → 百分比变化则
`sendAttackStatusPacketUpdate(TYPE.HP, hpAfter - hpBefore, 0, LOG.REGULAR)`。

- delta 用**实际差值**而非请求值：钳位（`newHp<=0 → 0`）与已死短路会让两者不等。
- `onReduceHp()` 保持空：攻击路径已由 `NpcController.onAttack` 广播。

### 3.3 召唤物侧（`SummonLifeStats`）——双通道

召唤物有**两条**客户端通道，只发一条不够：

- 血条：`SM_ATTACK_STATUS` 广播给能看到它的玩家；
- 主人面板：消费**绝对 HP**，必须单独下发 `SM_SUMMON_UPDATE`。

抽出可测 seam `sendSummonPanelUpdate()`（`master == null` 判空返回），并让既有 `onIncreaseHp`
复用它（单一来源）。**不加 `isSpawned()` 守卫**（与 `onIncreaseHp` 一致）：despawn 时 KnownList 已清空、
广播自然退化；传送挂起期间主人面板仍存在，同步反而有益。master 会被 `ReleaseSummonTask` 置空，
必须判空。

### 3.4 锁序约束（**新增硬性禁令**）

**禁止在 `reduceHp` 外套锁**。`CreatureLifeStats` 的类 javadoc 明确要求 `onDie` 在锁外回调，
而 `onDie` 会跨对象再取别的生物的 `lifeLock`（如 `SummonsService.ReleaseSummonTask`）——
外套锁会引入跨生物锁序，形成经典倒序死锁。因此前后值读取不加锁：竞态窗口有界，
血条百分比在序列化时读活值永远正确，只影响"是否发包"在 1% 边界上的翻转。

## 4. 验证

| 项 | 命令 / 结果 |
|---|---|
| 聚焦测试 | `mvn -B test -Dtest=NpcLifeStatsTest,NpcDirectHpSyncTest,NpcDirectHpSyncGateTest,SummonLifeStatsTest,SMAttackStatusTest test` → **28/28 通过** |
| 定向回归 | 同上一次命令追加 `SwitchHpMpEffectTest,TargetRangePropertyTest,FollowManagerTest,CMObjectSearchTest,RetailPatternAI2Test` → **113/114 通过** |
| 合计 | 142 run，1 error |
| 唯一失败 | `RetailPatternAI2Test.patternDrivenNpcsDoNotRandomlyCastUnscriptedSkills`（`RetailPatternAI2:893` NPE）——**已归档的既有失败**，与本次改动无关 |
| IDE inspections | 三个测试文件 0 error 0 warning；生产文件 0 error |
| 客户端实机 | **PENDING**（需部署后确认召唤物血条与主人面板同步下降） |

测试清单：

- `NpcDirectHpSyncTest`：新增 5 例——已 spawn 同步、未 spawn 静默、不足 1% 静默、
  **致死仍发 1 包**（锁住"致死不特判"，与攻击路径先例一致）、已死静默（共 12 例）
- `SummonLifeStatsTest`（新）：条 + 面板双通道、不足 1% 双通道静默、master 缺席不 NPE（3 例）
- `NpcDirectHpSyncGateTest`：新增 2 例——三个调用点走同步入口且 `CreatureController` 不得出现该入口、
  召唤物双通道出口存在（共 7 例）；爆炸半径检查改为**整条继承链**断言

### 4.1 夹具要点

Objenesis 桩绕过构造器 → 必须补桩：`getObserveController()`（`reduceHp` 会调
`notifyLifeChangedObservers`）、`getController()`（`onDie`）。`TestSummon` 需要一个**永不执行**的
构造器来满足超类契约（`Summon` 真实构造器解引用模板与 `DataManager`）。

## 5. 明确不做（登记为边界）

- 召唤物未覆写 `setCurrentHp*`：今天无调用点，不顺手扩爆炸半径。
- MP 侧同构缺口不补：`SM_ATTACK_STATUS` / `SM_SUMMON_UPDATE` 都不带 MP，NPC 无 MP 显示通道。
- 玩家 `<hpuse>` 的飘字：`TYPE.USED_HP(4)` 的 `writeImpl` 走另一条布局分支，不能直接替换。
- 负治疗量语义：保留扣血（见第 1 节）。

## 6. 涉及文件

**生产**：

- `src/main/java/com/aionemu/gameserver/model/stats/container/CreatureLifeStats.java`
- `src/main/java/com/aionemu/gameserver/model/stats/container/NpcLifeStats.java`
- `src/main/java/com/aionemu/gameserver/model/stats/container/SummonLifeStats.java`
- `src/main/java/com/aionemu/gameserver/skillengine/effect/AbstractHealEffect.java`
- `src/main/java/com/aionemu/gameserver/skillengine/action/HpUseAction.java`
- `src/main/java/com/aionemu/gameserver/skillengine/periodicaction/HpUsePeriodicAction.java`

**测试**：

- `src/test/java/com/aionemu/gameserver/model/stats/container/SummonLifeStatsTest.java`（新增）
- `src/test/java/com/aionemu/gameserver/model/stats/container/NpcDirectHpSyncTest.java`
- `src/test/java/com/aionemu/gameserver/model/stats/container/NpcDirectHpSyncGateTest.java`
