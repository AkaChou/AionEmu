# Creature Vitals & HP Sync Patterns (生物属性与血量同步模式)

本文档记录生物生命值在服务端与客户端血条之间的同步契约，以及绕过攻击流程直接改血时的实战避坑经验。

> Pattern IDs: `CV-001`
> card_status: ACTIVE; conclusions are tied to the 5.8 client packet layout and the NPC life-stats implementation
> scope: NPC and summon HP synchronization to the client bar and the master summon panel (direct assignment, proportional rescale, attack, heal and non-attack reduction paths) and the SM_ATTACK_STATUS percentage contract
> last_reviewed: 2026-09-23

---

## [CV-001] 一、直接改血必须走同步出口，等比重算必须走静默出口 (DIRECT_HP_SET_NEEDS_SYNC_OUTLET)
<!-- pattern-metadata
status: CONFIRMED
scope: NPC 与召唤物生命值向客户端血条和主人召唤面板的同步通道（直接改血 / 等比重算 / 攻击 / 治疗 / 非攻击扣血）与 SM_ATTACK_STATUS 的百分比契约；玩家侧另有独立通道，不在范围内
first_seen: 2026-09-23
last_verified: 2026-09-23
symptom: 怪物血条停在旧值、要等下一次真实攻击才跳变（AI 脚本阶段转换设 5%/50%、脚本回满、Servant 出生设血后客户端毫无反应）；反向症状是实例人数变化或战斗中最大生命变化后，周围玩家收到成片与血条无关的飘字而血条纹丝不动；第三类是技能侧扣血（精灵星「精灵吸收」的负治疗量、NPC 施法耗血）后 NPC 血条与召唤物的主人面板同时停在旧值
root_cause: 客户端血条只认 SM_ATTACK_STATUS 里 0–100 的整数百分比（包内不含绝对 HP）；而 NpcLifeStats.onReduceHp() 为空实现、setCurrentHp/setCurrentHpPercent 自身也不广播，因此所有绕过 Controller.onAttack 的改血都不发包；reduceHp 的直调（技能侧扣血）同样不经过任何出口，NPC 与召唤物两侧皆静默，召唤物还额外需要 SM_SUMMON_UPDATE 才能刷新主人面板（该面板消费的是绝对 HP 而非百分比）；同时 CreatureGameStats.checkHPStats 先让新的最大生命生效、再重算当前生命，使「覆写入口读到的前后百分比」无法代表客户端可见的变化，等比重算会被误判成一次真实增减
fix_or_guardrail: 直接改血收敛到 NpcLifeStats 覆写的 setCurrentHp/setCurrentHpPercent（未 spawn 短路 → 锁内取前后值 → 锁外补发 TYPE.HP + 有符号 delta）；等比重算走 CreatureLifeStats.rescaleCurrentHp 并由 NPC 覆写为静默；非攻击扣血走 CreatureLifeStats.reduceHpFromEffect，由 NPC（只广播）与召唤物（广播 + SM_SUMMON_UPDATE 面板）各自覆写，delta 用实际差值而非请求值；onReduceHp() 有意保持空实现（攻击路径已由 NpcController#onAttack 广播，补发会双包双飘字）；同步判据用百分比变化而非绝对值，且禁止用覆写内部的前后百分比识别等比重算；禁止在 reduceHp 外套锁（onDie 在锁外回调并跨对象取锁，外套锁会形成跨生物倒序死锁）
evidence: src/main/java/com/aionemu/gameserver/model/stats/container/NpcLifeStats.java; src/main/java/com/aionemu/gameserver/model/stats/container/CreatureLifeStats.java; src/main/java/com/aionemu/gameserver/model/stats/container/SummonLifeStats.java; src/main/java/com/aionemu/gameserver/model/stats/container/CreatureGameStats.java:565; src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_ATTACK_STATUS.java:147; src/main/java/com/aionemu/gameserver/controllers/NpcController.java:460; src/main/java/com/aionemu/gameserver/ai2/handler/ReturningEventHandler.java; src/main/java/com/aionemu/gameserver/skillengine/effect/AbstractHealEffect.java:130; src/main/java/com/aionemu/gameserver/skillengine/action/HpUseAction.java:53; src/main/java/com/aionemu/gameserver/skillengine/periodicaction/HpUsePeriodicAction.java:41; src/test/java/com/aionemu/gameserver/model/stats/container/NpcDirectHpSyncTest.java; src/test/java/com/aionemu/gameserver/model/stats/container/NpcDirectHpSyncGateTest.java; src/test/java/com/aionemu/gameserver/model/stats/container/SummonLifeStatsTest.java; .agents/summary/npc-hp-sync/2026-09-23-npc-direct-hp-sync.zh-CN.md; .agents/summary/npc-hp-sync/2026-09-23-npc-effect-hp-reduction-sync.zh-CN.md
validation: static（全仓 setCurrentHp*/reduceHp 调用点与 SM_ATTACK_STATUS 发送点枚举，确认 NPC 侧只有 NpcController#onAttack 与 NpcLifeStats#onIncreaseHp 两个广播源）; focused-test（NpcLifeStatsTest,NpcDirectHpSyncTest,NpcDirectHpSyncGateTest,SummonLifeStatsTest,SMAttackStatusTest：28/28 通过）; 定向回归（SwitchHpMpEffectTest,TargetRangePropertyTest,FollowManagerTest,CMObjectSearchTest,RetailPatternAI2Test：113/114 通过，唯一失败为已归档的既有基线失败 ai.RetailPatternAI2Test 的 RetailPatternAI2:893 NPE）; IDE inspections 0 error; client/production 复验 PENDING（需部署后确认血条跳变时机、召唤物面板刷新与无双重广播）
boundaries: 覆盖 NPC 与召唤物；玩家（PlayerLifeStats）直接继承 CreatureLifeStats，有自己的同步通道且不受影响；召唤物未覆写 setCurrentHp*（今天无调用点，不扩爆炸半径）；召唤物的面板更新不加 isSpawned() 守卫（despawn 后 KnownList 已空、广播自然退化），但必须判空 master（ReleaseSummonTask 会置空）；不解决协议本身的 1% 粒度上限（更细的血条或数值显示必须改客户端）；MP 侧同构缺口不补（SM_ATTACK_STATUS 与 SM_SUMMON_UPDATE 都不带 MP，NPC 无 MP 显示通道）；玩家 <hpuse> 的飘字走 TYPE.USED_HP(4) 的另一条布局分支，不能直接替换；负治疗量（EL_Unsummon 3732-3738，first_target=MYPET）的扣血语义经用户确认为零售行为，保留；Bastikan/Bastiel 的 handleSpawned 改血发生在 KnownList 建立之前，发包是空操作
superseded_by: none
first_check: 血条不同步时，先确认该改血是否经过 Controller.onAttack（绕过者必不发包），再确认它走的是 setCurrentHp*/rescaleCurrentHp/reduceHpFromEffect 中哪条出口；排查成片假飘字时，检查最大生命变化是否仍在走 setCurrentHp 而不是 rescaleCurrentHp；核实出生/重生期改血是否被 isSpawned() 守卫正确短路；召唤物主人面板不刷新时，检查是否漏发 SM_SUMMON_UPDATE（血条正常但面板停住是该漏发的特征）
keywords: 血条, 血量实时显示, 血条不动, 血条跳变, 血条不实时, 假飘字, 双飘字, hpPercentage, SM_ATTACK_STATUS, setCurrentHp, setCurrentHpPercent, rescaleCurrentHp, reduceHpFromEffect, checkHPStats, 等比重算, 阶段转换, 脚本回满, 出生设血, onReduceHp, 精灵吸收, 负治疗量, EL_Unsummon, 施法耗血, hpuse, reduceHp, 召唤面板, 主人面板, SM_SUMMON_UPDATE, 召唤物
-->

- **现象**：AI 脚本改血后客户端血条停在旧值，直到玩家下一次真实攻击才跳变；典型触发是副本 Boss 阶段转换设 5%/50%、脚本 NPC 回满血、带 `hpCondition` 的 Servant 出生设血。
- **根因链**：
  1. 客户端血条的唯一数据源是 `SM_ATTACK_STATUS` 的百分比字段（`SM_ATTACK_STATUS.java:147` 写入 `getHpPercentage()`，0–100 整数），包内**没有绝对 HP**；出生快照由 `SM_NPC_INFO:128` 提供；
  2. NPC 侧的广播源只有两处——伤害走 `NpcController.onAttack:460`，治疗走 `NpcLifeStats.onIncreaseHp`；
  3. `NpcLifeStats.onReduceHp()` 为空实现，`CreatureLifeStats.setCurrentHp` / `setCurrentHpPercent` 自身也不通知，于是 `setCurrentHp`/`setCurrentHpPercent` 的所有调用点（AI 脚本、属性重算、出生设血）一律静默；
  4. 既有代码里 `ai2/handler/ReturningEventHandler` 的手工补发正是这个缺陷的产物，说明它长期被逐处打补丁。
- **修复契约（四个出口分工明确）**：
  - **攻击路径**：`NpcController#onAttack` 独占广播，`onReduceHp()` 必须保持空——否则每次普通攻击都会双包双飘字；
  - **直接改血**：`NpcLifeStats` 覆写 `setCurrentHp`/`setCurrentHpPercent`，未 spawn 时短路（出生包自带正确值，且出生初始化对每只 NPC 都执行，短路保证零额外开销），锁内取前后值、锁外补发 `TYPE.HP` + 有符号 delta；
  - **等比重算**：`CreatureGameStats.checkHPStats` 改走 `CreatureLifeStats.rescaleCurrentHp`，NPC 覆写为 `super.setCurrentHp`（无同步）；
  - **非攻击扣血**：`CreatureLifeStats.reduceHpFromEffect(int, Creature)` 是唯一入口（技能侧三个调用点：负治疗量、`hpuse` 施法耗血、周期耗血），默认直通 `reduceHp` 不广播；`NpcLifeStats` 覆写为「守卫 → 读前后百分比 → super → 变化则广播」，`SummonLifeStats` 覆写为「广播 **+** `SM_SUMMON_UPDATE` 面板」。delta 必须用**实际差值**（`hpAfter - hpBefore`）而非请求值：钳位与已死短路会让两者不等。
- **召唤物的两条通道**：血条走 `SM_ATTACK_STATUS` 广播给可见玩家，主人面板消费的是**绝对 HP**、必须单独下发 `SM_SUMMON_UPDATE`。只发一条会出现「血条动了面板不动」或反之。面板更新不加 `isSpawned()` 守卫（despawn 后 KnownList 已空、广播自然退化；传送挂起期间面板仍存在，同步反而有益），但**必须判空 master**（`ReleaseSummonTask` 会置空）。
- **锁序禁令**：**禁止在 `reduceHp` 外套锁**。类 javadoc 要求 `onDie` 在锁外回调，而 `onDie` 会跨对象再取别的生物的 `lifeLock`（如 `SummonsService.ReleaseSummonTask`），外套锁会引入跨生物锁序、形成经典倒序死锁。前后值读取因此不加锁：竞态窗口有界，百分比在序列化时读活值永远正确，只影响「是否发包」在 1% 边界上的翻转。
- **判据选择**：同步判据用「客户端可见百分比是否变化」而非「绝对增减量」——最大生命不变时的细粒度变化（如 1000 血里 40→45）客户端看不出，补包只是噪音。
- **最容易踩的陷阱**：不能用覆写入口读到的「前后百分比」识别等比重算。`checkHPStats` 先让新上限生效再重算当前生命，40/100 → 80/200 在覆写内读作 20% → 40%，看起来变了而客户端毫无感知；若据此发包，实例人数变化（`InstanceScaler` 对全实例生物重算）与战斗中 maxHp 变化（buff）会给周围玩家整片假飘字。等比重算必须靠**显式出口**区分，不能靠推断。
- **验证边界**：focused-test 28/28 与定向回归 113/114 证明了发送时机、静默语义与调用点收口；**客户端实机尚未复验**，血条跳变观感、召唤物主人面板刷新与"是否仍有双重广播"需部署后确认。
- **教训**：凡是"脚本直接改数值、客户端另有显示副本"的字段，都要问一句"这个改动有没有走同步出口"；而"按比例重算"这类**刻意保持可见值不变**的操作，必须给它一条显式静默路径，别指望下游用数值反推；同一份状态有多个客户端消费面时（血条 + 主人面板），要逐个列出消费面并各自下发，别假设一个包能喂饱所有 UI。反之，**会在锁外回调并跨对象取锁的路径**（如 `onDie`）绝不能在外层补锁——省下的那点竞态窗口，代价是跨生物倒序死锁。
