# NPC 直接改血的血量同步收敛（2026-09-23）

## 1. 任务背景

用户提问：「如何让客户端的怪物实时显示血量？」经澄清，目标是**修复血量不同步**——
AI 脚本/系统绕过攻击流程直接改血后，客户端血条停在旧值，直到下一次真实攻击才跳变。

## 2. 机制调查结论

### 2.1 客户端血条的数据通道

客户端血条**完全由服务器下发的百分比驱动**，包内不含绝对 HP：

| 环节 | 位置 | 说明 |
|---|---|---|
| 血量字段 | `network/aion/serverpackets/SM_ATTACK_STATUS.java:147` | `writeC(creature.getLifeStats().getHpPercentage())`，0–100 整数 |
| 百分比算法 | `model/stats/container/CreatureLifeStats.java:288` | 整数截断，存活时保底 1% |
| 伤害广播 | `controllers/NpcController.java:460` | 发给 NPC KnownList 内在线玩家 |
| 治疗广播 | `model/stats/container/NpcLifeStats.java:27` | 经 `sendAttackStatusPacketUpdate` |
| 出生快照 | `SM_NPC_INFO:128` | 客户端首次可见时的正确百分比来源 |

**精度上限是 1%**：想让血条每击都动或显示数值，服务器端无解，必须改客户端。

### 2.2 两个盲区

1. **协议粒度**：单次伤害不足以推动 1% 时血条不动（高血量怪尤其明显）。属客户端 UI 与协议的硬限制。
2. **绕过路径**：`NpcLifeStats.onReduceHp()` 为空实现，`setCurrentHp` / `setCurrentHpPercent`
   自身也不广播 → 所有"不经 `Controller.onAttack`"的改血都不发包。

### 2.3 已核实的受害者

| 站点 | 行为 |
|---|---|
| `ai/instance/steelRakeCabin/AnikikiAI2.java:82`、`steelRake/TamerAnikikiAI2.java:83` | 回满 |
| `ai/instance/empyreanCrucible/PriestElyosPreceptorAI2.java:89`、`PriestAsmodiansPreceptorAI2.java:89` | 设 50% |
| `ai/instance/bastionOfSouls/Detachment_Captain_BastikanAI2.java:64`、`BastielAI2.java:64` | 阶段转换设 5%（**经核实非可见 bug**，见 2.4） |
| `model/stats/container/CreatureGameStats.java:565` | maxHp 变化时按比例重算 |
| `spawnengine/VisibleObjectSpawner.java:869` | Servant 出生设血（**修复**，修前客户端恒显示 100%） |

正确范式（说明缺陷被逐个打补丁过）：`ai2/handler/ReturningEventHandler.java:79-87`。

### 2.4 已排除的路径

- `World.spawn` 时序为 `onBeforeSpawn(537)` → `setIsSpawned(true)(538)` → `onAfterSpawn(541)` →
  `updateKnownlist(542)`，因此 `Bastikan/Bastiel` 在 `handleSpawned` 改血时 KnownList 为空、
  发包是空操作；客户端首次可见即正确值。
- `synchronizeWithMaxStats` / `updateCurrentStats` 的调用点全部在玩家/召唤物侧。
- 无 NPC 版 LifeStats DAO、无反射写 `currentHp`、无 `NpcLifeStats` 子类（仅测试桩）。

## 3. 修复方案

### 3.1 收敛出口（`NpcLifeStats`）

覆写 `setCurrentHp(int)` 与 `setCurrentHpPercent(int)`：前置守卫（`owner != null && owner.isSpawned()`）
→ 用 `restoreLock`（与 `hpLock` 同一实例）取前后值 → 锁外经 `sendAttackStatusPacketUpdate(TYPE.HP, delta, 0, LOG.REGULAR)` 补发。

- **未 spawn 短路**让「每只 NPC 出生都会执行」的初始化改血保持零额外开销。
- `onReduceHp()` 保持空实现并加注释：攻击路径已由 `NpcController#onAttack` 广播，补发会双包双飘字。
- 判定抽成包级静态纯函数，便于无 mock 测试。

### 3.2 等比重算的静默出口（**测试才暴露的修正**）

初版设计用「调用前后的百分比是否变化」作为判据，**行为测试直接证伪了它**：

`CreatureGameStats.checkHPStats` 是**先让新 maxHp 生效、再调 `setCurrentHp`**，所以在覆写内部读到的
「变化前百分比」已经按**新**上限计算：maxHp 从 100 翻到 200、currentHp 40 → 80 时，读到的是
40/200 = 20% → 80/200 = 40%，看起来变了，于是误发包；而客户端可见的百分比（相对旧上限的 40%）
**从未改变**。

**修正**：为等比重算引入显式静默路径，不再靠百分比推断。

- `CreatureLifeStats` 新增 `rescaleCurrentHp(int)`，默认委托 `setCurrentHp`（玩家/召唤物行为不变）。
- `NpcLifeStats` 覆写为 `super.setCurrentHp(hp)`（绕过自身覆写 → 静默）。
- `CreatureGameStats.checkHPStats:565` 改调 `rescaleCurrentHp`。

若不做这一步，实例人数变化（`InstanceScaler`）与战斗中 maxHp 变化（buff）会对全实例生物触发同步包，
产生整片假飘字。

### 3.3 清理

`ai2/handler/ReturningEventHandler.java`：删除手工 `SM_ATTACK_STATUS` 补发与随之无用的 import，
保留 `notifyLifeChangedObservers`（该通知不在收敛范围，且是此处唯一的观察者通知）。

## 4. 验证

| 项 | 命令 / 结果 |
|---|---|
| 聚焦测试 | `mvn -B -Dtest=NpcLifeStatsTest,NpcDirectHpSyncTest,NpcDirectHpSyncGateTest,SMAttackStatusTest test` → **18/18 通过** |
| 定向回归 | `mvn -B test -Dtest=CreatureGameStatsTest,CreatureGameStatsBytecodeTest,PlayerQuestPvpEventPortTest,PlayerServiceTitleRestoreTest,MpConditionTest,SwitchHpMpEffectTest,TargetRangePropertyTest,FollowManagerTest,RetailPatternAI2Test,CMObjectSearchTest,NpcControllerTest` → **127/128 通过** |
| 唯一失败 | `RetailPatternAI2Test.patternDrivenNpcsDoNotRandomlyCastUnscriptedSkills`（`RetailPatternAI2:893` NPE）——**已归档的既有失败**，`.agents/summary/startup-perf/` 与 `scriptdll-quest-driver/` 均明确记录「与本改造无关，不要去修它」 |
| IDE inspections | 0 error（剩余 warning 为守卫写法与结构门禁的固有提示） |
| 客户端实机 | **PENDING**（需部署后确认血条跳变时机与无双重广播） |

测试清单：
- `NpcLifeStatsTest`：百分比判定表（3 例）
- `NpcDirectHpSyncTest`（新）：未 spawn 静默、掉血/回血 delta 符号、幂等静默、等比重算静默、
  低于 1 个百分点静默、赋同值静默（7 例）
- `NpcDirectHpSyncGateTest`（新）：三个覆写存在性、爆炸半径收口、`onReduceHp` 保持静默、
  `checkHPStats` 必须走静默出口、`ReturningEventHandler` 不再自行拼包（5 例）
- `SMAttackStatusTest`：`TYPE.HP` 与 `TYPE.DAMAGE` 同线值 7 但符号约定相反（2 例）

## 5. 已知残留（明确不在本次范围）

- `skillengine/effect/AbstractHealEffect.java:130`：负治疗量走 `reduceHp` → NPC 静默加血。
- `skillengine/action/HpUseAction.java:53`：NPC 施法消耗 HP → 静默扣血。
  两者都受制于「`onReduceHp()` 不能补发」的双包约束，要修需先重构攻击路径的广播归属。
- NPC 的 MP 同步不做（客户端无 NPC 魔法条）。

## 6. 涉及文件

**生产**：
- `src/main/java/com/aionemu/gameserver/model/stats/container/NpcLifeStats.java`
- `src/main/java/com/aionemu/gameserver/model/stats/container/CreatureLifeStats.java`
- `src/main/java/com/aionemu/gameserver/model/stats/container/CreatureGameStats.java`
- `src/main/java/com/aionemu/gameserver/ai2/handler/ReturningEventHandler.java`

**测试**：
- `src/test/java/com/aionemu/gameserver/model/stats/container/NpcDirectHpSyncTest.java`（新增）
- `src/test/java/com/aionemu/gameserver/model/stats/container/NpcDirectHpSyncGateTest.java`（新增）
- `src/test/java/com/aionemu/gameserver/model/stats/container/NpcLifeStatsTest.java`
- `src/test/java/com/aionemu/gameserver/network/aion/serverpackets/SMAttackStatusTest.java`
