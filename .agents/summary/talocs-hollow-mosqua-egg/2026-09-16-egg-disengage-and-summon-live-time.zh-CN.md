# Queen Mosqua 卵的脱战抖动与孵化物生命周期 / Mosqua egg disengage churn and hatchling lifetime

日期 / Date: 2026-09-16 · 分支 / Branch: `quest` · 状态 / Status: **已修复；聚焦测试与客户端实机验证均通过**

关联 / Related: `AIM-004`（`patterns/ai-movement.md`）、`IR-009`（`patterns/instance-runtime.md`）、
`.agents/summary/talocs-hollow-mosqua-egg/evidence.md`（09-14 孵化链被回位取消的修复）

## 一、症状 / Symptom

1. Queen Mosqua（215480）召唤的 mosqua egg（282006）在卵形态下不能移动，被攻击时反复“脱离战斗”，
   一波多只时客户端连续播放脱战表现/音效。
2. 卵孵化出的召唤物 282082（`BIDElim_NeutWorkmanflySummon_51_n`）只短暂出现就消失，真端 pattern 给的是 `live_time=18`。

## 二、真端数据与原文 / Retail data

- 真端 NPC 数据 `/Users/mc/IdeaProjects/58Server/Map/XML/npcs_std_monsters.xml`：
  - 282006：`move_speed_normal_* = 0`、`attack_range=3.0`、`min/max_damage=291/437`、`attack_delay=2081`、
    `sensory_range=20`、`max_chase_time=0`、`react_to_pathfind_fail=return_to_sp`。
  - 282082：`move_speed_normal_run=4.5`、`attack_range=2.0`、`min/max_damage=738/1107`、`attack_delay=1938`、
    `max_hp=5611`、4 个技能、`max_chase_time=40`、`sensory_range=10` —— 完整的可移动战斗召唤物。
- 真端 pattern `/Users/mc/IdeaProjects/58Server/Map/XML/NpcAIPatterns_IDElim_OSY.xml`（UTF-16）：
  - `Elim_NeutflyEgg`：`on_see_user` = `use_skill(SKILLI_INDEX_0)` → `spawn_on_target(SPAWN_ID_1,
    BIDElim_NeutWorkmanflySummon_51_n, live_time=18, despawn_at_attack_state=TRUE)` → `despawn_self`；
    `on_enter_attack_state` / `on_enter_idle_state` 都是 `do_nothing`。**没有**任何回位/放弃目标/清仇恨动作，
    也没有对 SPAWN_ID_1 下发 `<despawn>`。
  - `Elim_NeutflyNm.on_die` 则用显式 `<despawn><spawn_id>SPAWN_ID_1/2/3</spawn_id></despawn>` 清理它要清理的子对象
    —— 真端 `spawn_id` 的用途就是“显式 despawn 动作”。
- 本地 compact 副本（`definitions/compact/ai/npc-ai.xml`、`npcaipatterns_idelim_osy.xml`）与真端逐字段一致，差异在引擎。

## 三、根因 / Root causes

### A. 不可移动 NPC 的“够不着 → 放弃目标 → 立刻重新仇恨”抖动

0 移速 ⇒ `NpcAI2#isMoveSupported()` false（`NpcGameStats#getMovementSpeed` 由模板 walk/run speed 得出）⇒
`AttackManager#targetTooFar` 走到当时的 `shouldKeepTargetWhenImmobile(maxDamage=437, …) == false` 分支 ⇒
`TARGET_GIVEUP` ⇒ `TargetEventHandler#onTargetGiveup` 清仇恨并 `think()` ⇒
`ThinkEventHandler#thinkAttack` 无最高仇恨 ⇒ `BACK_HOME` ⇒ `ReturningEventHandler#onBackHome` 广播
`SM_EMOTION(NEUTRALMODE)` ⇒ 卵因 aggressive + 受击重新加仇恨立刻回到 FIGHT ⇒ 循环。
该分支是 2026-07-27 `3978ddbab` 引入的本仓库自创逻辑（`AttackManagerTest` 曾锁定），真端数据无对应驱动。

### B. 孵化物被生成者 teardown 级联删除

`spawn_on_target` 生成 282082 时登记进 `RetailPatternAI2#spawned`（`spawn_id=SPAWN_ID_1`）并排了自己的 18 秒到期任务；
同一动作链末尾的 `despawn_self` → `NpcController#onDespawn` → `AIEventType.DESPAWNED` → `handleDespawned` →
`resetPatternState` 时把该对象一并 `despawnForLifecycle`（`despawn_at_attack_state=TRUE` ⇒ 不延迟）⇒ 同一调用栈内被删除，
`live_time=18` 形同虚设。凡“带 live_time 的临时召唤物”都受影响，故症状具有普遍性。

## 四、修复 / Fix

| 位置 | 改动 |
|---|---|
| `AttackManager#targetTooFar` | 删除不可移动分支的 `TARGET_GIVEUP`，并移除随之失效的 `shouldKeepTargetWhenImmobile` / `hasOffensiveSkill` / `isOffensiveSkill` 与 5 个 import。不可移动 NPC 的战斗只由“目标离开已知列表”或真端 `max_chase_time` 规则结束 |
| `RetailPatternAI2` | 新增 `selfManagedSpawns`；`spawnAt` 对 `live_time>0` 的对象打标；`resetPatternState` 改走 `releaseTrackedSpawns()`：自带 `live_time` 的对象只保留登记（显式 `<despawn>` 仍有效），`live_time=0` 的标记物/门继续随重置删除；各删除路径同步清理标记 |
| `AttackManagerTest` | 改为源码契约闸门：`targetTooFar` 必须保留 `isMoveSupported()` 判断且不得出现 `TARGET_GIVEUP` |
| `RetailPatternAI2Test` | 新增 `patternResetKeepsSelfManagedLiveTimeSpawns`：`live_time` 对象不随重置删除且保留登记，`live_time=0` 对象仍被删除 |

## 五、验证 / Validation

- `mvn test -Dtest='AttackManagerTest,AttackManagerLeashTest,RetailPatternAI2Test,TargetEventHandlerTest,SimpleAttackManagerTargetSafetyTest,SkillAttackManagerBytecodeTest,FollowManagerTest,GetMostPlayerDamageNullGateTest,InstanceMovieNullGuardTest'`
  → **95 例 / 0 失败 / 0 错误，BUILD SUCCESS**（主源码 4718 文件编译通过）。
- 沉淀校验（本修复提交 `6d847cc7c` 时）：`sync_memory_bank.py` + `check_memory_bank.py` →
  `MEMORY_BANK_OK ROUTER_IDS=44 PATTERNS=44 SYMPTOM_INDEX=44`。此后并行任务新增的 `IR-010` 卡片尚未挂路由，
  当前 `check_memory_bank.py` 会报 `unrouted IR-010`——与本修复无关。
- **客户端实机验证（2026-09-16）**：用户报告通过，覆盖本次两个症状（卵反复脱战、孵化物过早消失）。
  用户未逐项复述细节，如需可将具体现象/时间补记在本节。

## 六、真端对照验收项 / Retail acceptance

1. 300190000 远程攻击卵：不应出现反复脱战表现/音效；卵保持仇恨直到被杀。→ **客户端验证通过（2026-09-16，用户报告）**
2. 打破卵后跟踪 282082：应存活约 18 秒、主动追击并施放技能（`NWA_Satk_Nr` 等）。→ **客户端验证通过（2026-09-16，用户报告）**
3. 若真端实测出现“卵也会脱战”，则以真端 `max_chase_time` 等驱动字段重新定位，不要在代码里按伤害大小判断。
