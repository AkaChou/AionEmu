# Queen Mosqua 死亡链 NPE：player 为 null / Null-player NPE in the Queen Mosqua death chain

日期 / Date: 2026-09-15 · 分支 / Branch: `quest` · HEAD: `aea22f3cb` ·
状态 / Status: **源码已修并通过聚焦测试（本处 + 同类 6 处 + 闸门盲区 8 处 + 未使用形参清理 3 处），真端复测未执行**

关联 / Related: `IR-008`（`.agents/memory-bank/patterns/instance-runtime.md`）、
`.agents/summary/architecture-performance-refactor/2026-09-15-allocation-and-null-gate.md`

## 一、症状 / Symptom

2026-09-15 23:38:54 运行态日志（`pool-4-thread-1`，技能收招线程）：

```text
java.lang.NullPointerException: Cannot invoke "com.aionemu.gameserver.model.gameobjects.player.Player.getSummon()" because "player" is null
	at com.aionemu.gameserver.instance.handlers.scripts.TalocsHollowInstance.onDie(TalocsHollowInstance.java:200)
	at com.aionemu.gameserver.controllers.NpcController.onDie(NpcController.java:243)
	at com.aionemu.gameserver.model.stats.container.CreatureLifeStats.reduceHp(CreatureLifeStats.java:92)
	at com.aionemu.gameserver.controllers.CreatureController.onAttack(CreatureController.java:255)
	...
	at com.aionemu.gameserver.skillengine.effect.DamageEffect.applyEffect(DamageEffect.java:51)
```

- 命中位置是 `case 215480, 246240`（Queen Mosqua）分支的“解除召唤”判断。
- 该分支在 NPE 之后还有 `sp(700739 … Cracked Huge Insect Egg)`，
  异常会让 `onDie` 整体中断：**蛋黄不生成，2F 气流链条的起点直接丢失**。
- 同一 `onDie` 的 `case 215488, 246242`（Celestius）也用同一个 `player` 发奖励，属于同一类风险。

## 二、证据链 / Evidence

- `TalocsHollowInstance.onDie` 以 `npc.getAggroList().getMostPlayerDamage()` 取玩家（第 185 行）。
- `AggroList.getMostPlayerDamage()`（`AggroList.java:264`）在 `aggroList` 为空、或
  `getFinalDamageList(false)` 中没有任何 `Player` 条目时返回 `null`；`getFinalDamageList`（`AggroList.java:477`）
  会把召唤物伤害归并到主人，但**丢弃主人为 null、或主人不在 owner 已知列表内的伤害条目**。
  因此“没有可归属玩家”至少有三种成因：

  1. 致死伤害来自无主或非玩家生物（NPC 技能、环境 DoT 等）；
  2. 致死来源是召唤物，但主人已不在 owner 的已知列表（离开 / 登出 / 被移出已知列表）；
  3. 死亡前 aggro 列表已被清空（脚本或 GM 路径）。

- 本次日志**未记录击杀者身份与 aggro 内容**，因此上述三种成因无法区分，**真因而今只到“无玩家归属”这一层**。
- 既有契约与本修复的关系：`IR-008 NPC_DEATH_PLAYER_MAY_BE_NULL` 早已要求“消费 `getMostPlayerDamage()` 必须先判空”；
  源码闸门 `GetMostPlayerDamageNullGateTest` 在落地时就把 `TalocsHollowInstance` 登记为已存在未判空基线（1 处，高危、待单独决策）。
  本次运行态 NPE 正是该基线的兑现。

## 三、修复 / Fix

`src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TalocsHollowInstance.java`：保留世界推进，只对玩家专属效果判空。

| 分支 | 玩家专属效果（判空后跳过） | 无条件保留的世界推进 |
|---|---|---|
| `215480, 246240` Queen Mosqua | `SummonsService.release(player.getSummon(), …)` | `deleteNpc(700738)`、`sendMovie`（本身已容忍 null）、`sp(700739 …)` |
| `215488, 246242` Celestius | `ItemService.addItem(player, 188900011/170170044, 1)` | `sendMsg("[Congratulation] …")` |

`src/test/java/com/aionemu/gameserver/controllers/attack/GetMostPlayerDamageNullGateTest.java`：
把 `TalocsHollowInstance` 从 `KNOWN_UNGUARDED_SITES` 移除（基线 10 处 → 9 处：3 处形参未使用 + 6 处真实风险），
该文件再出现未判空使用会被闸门直接点名。

## 四、验证边界 / Validation boundary

- 已做（2026-09-15 23:54）：
  - `mvn test -Dtest='GetMostPlayerDamageNullGateTest,InstanceMovieNullGuardTest,ModelCollectionImplementationTest'`
    → **20 例 / 0 失败 / 0 错误，BUILD SUCCESS**；同时完成 4718 个主源码文件编译，覆盖 `stop*(player)` 签名清理；
  - 按闸门同一套规则全量复算 `src/main/java`：**90 个消费点、0 违规**。
- **未做**：真端/客户端复测（重启后重打 Queen Mosqua，确认 435 播片、`700739` 生成、2F 气流与召唤释放；
  以及无玩家归属场景下副本推进不中断）。
- **未证明**：本次致死伤害的具体来源（第二节三种成因之一）。若要定论，需要在 `onDie` 处留一条击杀者/aggro 快照证据。

## 五、第二轮：同类 6 处 + 闸门盲区内的 8 处（2026-09-15）

统一语义：**无玩家归属时，玩家奖励一律跳过；需要落点的刷怪回退到 NPC 自身坐标**；世界推进与提示不受影响。

| 文件 | 原未判空使用 | 处理 |
|---|---|---|
| `MirashSanctuaryInstance` | `player.getSkillList().addSkill(…)`、`SkillLearnService.removeSkill(player, …)`、4 处 `spawn(…, player.getX/Y/Z())` | 技能增删判空跳过；雕像改用 `player != null ? … : npc` 计算的 `spawnX/Y/Z` 回退 |
| `TrialsOfEternityInstance` | `AbyssPointsService.addGp(player, 1200)` | 判空跳过 |
| `AturamSkyFortressInstance` | 5 处 `sp(craftsman, player.getX/Y/Z(), …)` | 回退到发生器自身坐标（`spawnX/Y/Z`） |
| `event/Event_AturamSkyFortressInstance` | 同上 5 处 | 同上 |
| `event/IDEvent_Def_HInstance` | `ItemService.addItem(player, …)`、`player.getCommonData().addExp(…)` | 判空跳过（波次推进与实例计分不变） |
| `event/Event_ContaminatedUnderpathInstance` | 同上 | 同上 |

**闸门盲区**：`GetMostPlayerDamageNullGateTest` 只校验“**首次**真正使用前有判空”，
首次使用之后的解引用不会被拦——一个早期守卫会“放行”整个方法。按该盲区逐点复核 `onDie` 后发现并一并修复
`AturamSkyFortressInstance` / `event/Event_AturamSkyFortressInstance` 中被放行的 8 处 AP/GP 发放
（`Commander Barus` 的 `addGp/addAp`、`Ashunatal Shadowslip` 的 `addAp/addGp`）。
同一启发式扫出的其它候选（`sendMovie`/`sendPacket`、形参未使用的 `stop*(player)`、
`PvpService.doReward` 的 `winner == null` 早退）经逐条确认均为误报。
`KNOWN_UNGUARDED_SITES` 基线相应缩减为 3 条形参未被使用的 `stop*(player)` 调用。

**第三轮（同日，独立清理）**：三条形参未被使用的 `stop*(player)` 调用改为直接去掉未使用实参——
`FallenPoetaInstance.stopInstance(Player)`、`DrakenseerLairInstance.stopDrakenseerLairTimer(Player)`、
`KumukiCaveInstance.stopInstance2(Player)`；调用点与随之失效的 `getMostPlayerDamage()` 局部变量一并移除
（三个类都没有子类、没有 `@Override`，基类也未声明同名方法，已逐一确认）。
`KNOWN_UNGUARDED_SITES` 因此变为**空表**，闸门的 `MIN_CALL_SITES` 由 90 调整为 85（清理后实际为 90 处消费点，保留余量）。

## 六、后续待决策 / Follow-ups

- 播片语义：`sendMovie(player, 435)` 在无玩家归属时对任何人都不会播放。若希望此时副本内玩家仍能看到 435，
  需要改成“优先给归并玩家，否则广播副本内全部玩家”，属于玩法语义变更，本次未做。
- 闸门增强：要真正消除“首次使用之后”的盲区，需要能识别多行实参、复合条件与对外 null 约定的分析方式
  （纯源码正则误报率过高），本次未改闸门规则。
