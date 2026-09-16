# 任务 10032：击杀 Celestius 后卡斯帕的幻影（799503）不出现

- 时间：2026-09-16
- 玩家：Ww（objectId 153807）
- 分支：`quest`
- 相关：`quests/10032.xml`、`RetailPatternAI2`、`npcaipatterns_idelim_osy.xml`、`TalocsHollowInstance`、commit `5ccb10261`

## 一、症状 / Symptom

玩家在塔洛克空洞（300190000）击杀 Celestius（215488）并拿到心脏
`182215620`（`10032.xml:24`，Celestius 掉落）之后，任务要求与卡斯帕的幻影
`799503`（`CaspaGhost_01`，`10032.xml:279/290/306`）对话交付，但该 NPC 在副本里
**完全不出现**，无法继续也无法完成。

## 二、真端数据 / Retail data

- 真端 pattern `Elim_ComadAe`（215488/246242 的 pattern，`npc-ai.xml:10453`、
  `npc-ai.xml:32187`）的 `on_killed_by_user` 只有一条 DIRECT 动作链
  （`npcaipatterns_idelim_osy.xml:11`）：

  ```text
  despawn SPAWN_ID_1 / SPAWN_ID_2 / SPAWN_ID_3
  spawn  SPAWN_ID_4 = CaspaGhost_01 (SPAWN_LOCATION_ABSOLUTE, 548/811/1375, dir=0)
         live_time=0, despawn_at_attack_state=FALSE
  set_condition_spawn_variable IDElim_3F_Boss = 2   → 条件 3083 刷 700741（condition-spawns.xml:31042）
  play_cutscene_by_user_indicator USERI_KILLER 437
  ```

- `CaspaGhost_01` 就是 `799503`（`npc-ai.xml:65623`，模板 `portal_dialog`，
  `npc_template_286321_800030.xml:75263`）；`5ccb10261` 之后全仓库**没有**任何静态
  spawn 或条件刷怪入口（`condition-spawns.xml` 中 799503 计数为 0），唯一来源就是上面这条 pattern 动作。

## 三、旧实现退出历史 / Legacy

`5ccb10261`（feat(instance): align Taloc's Hollow retail flow）同时做了两件事：

1. 删除 `TalocsHollowInstance.onDie` 里 Celestius 分支的
   `spawn(799503, 539.94135f, 813.3849f, 1377.4283f, (byte) 27)` 兜底；
2. 把 `CelestiusAI2` 精简为 `extends RetailPatternAI2`（行为完全交给真端 pattern）。

于是 799503 的唯一生成路径变成 pattern 动作。当前 `TalocsHollowInstance.java:211`
的 Celestius 分支只发奖励与通关广播，不再刷该 NPC。

## 四、根因 / Root cause

死亡事件链的调用顺序（`NpcController.java:244-245`）：

```text
onCreatureEvent(DIED, lastAttacker) → RetailPatternAI2#handleKilled → runEvent("on_killed_by_user")
                                     ↑ 这里生成 799503（登记进 spawned[SPAWN_ID_4]）
onGeneralEvent(DIED)               → handleDied → runDeathEvent() → resetPatternState()
                                     ↑ releaseTrackedSpawns() 立刻把刚生成的 799503 删掉
```

`resetPatternState()` → `releaseTrackedSpawns()` 只保护 `selfManagedSpawns`
（现仅 `live_time>0` 的对象）；799503 是 `live_time=0`，于是走
`despawnForLifecycle()`：`despawnAtAttackState=false`、自身不在战斗 →
`shouldDelayLifecycleDespawn(false, false) == false` → **同一调用栈内 `onDelete()`**。
客户端即使收到刷新也看不到实体，表现就是“NPC 没有出现”。

同类内容还有大批（死亡时刷奖励 NPC、传送门、控制物的 pattern 共 127 条
`tracked + live_time=0` 记录），本质是 IR-009 只覆盖了 `live_time>0` 的一半：
真端 data 里 `<spawn_id>` 只用于显式 `<despawn>`，**没有**“生成者死亡即删除子对象”的语义。

## 五、修复 / Fix

| 位置 | 改动 |
|---|---|
| `RetailPatternAI2` | 新增 `SPAWNER_END_EVENTS = {on_die, on_killed_by_user, on_killed_by_npc, on_despawn}` 与 `spawnerEndEventInProgress` 标记；`runEvent` 在执行这些事件链时置位并 try/finally 还原 |
| `RetailPatternAI2#spawnAt` | 生成判定改为 `hasIndependentLifetime(liveTime, spawnerEndEventInProgress)`：死亡/消失事件链里生成的子对象与 `live_time` 对象一样，只保留登记、不随生成者状态重置删除 |
| `RetailPatternAI2Test` | 新增 `spawnerEndEventSpawnsKeepIndependentLifetime`：锁定事件集合（含 `on_die`/`on_killed_by_user`/`on_killed_by_npc`/`on_despawn`，不含可逆的 `on_leave_attack_state`）与 `hasIndependentLifetime` 三条分支 |

边界：显式 `<despawn spawn_id>` 仍然有效；`live_time` 到期任务不变；
可逆的 `on_leave_attack_state`（90 处 spawn 动作）仍随回位重置释放；
子对象不再由生成者重生/回位自动回收，清理交给真端显式动作或副本销毁。

安全性论证：这四类事件之后紧跟着 `resetPatternState()`，所以被标记的子对象
**此前一定是“生成后立即删除”的无效 spawn**；本次改动的效果只会让真端本来要求出现的
对象变得可见，不会改变已经在生效的交互对象（`live_time>0` 与 `on_leave_attack_state` 路径完全未动）。

## 六、验证 / Validation

- **静态取证**：上述调用顺序、登记与释放判定全部来自源码；799503 无其它生成入口。
- **聚焦测试**：`mvn -B test -Dtest='RetailPatternAI2Test'` → **PASSED**
  （2026-09-16 16:22:57，`Tests run: 79, Failures: 0, Errors: 0, Skipped: 0`，BUILD SUCCESS；
  工作区含并行任务改动，非 A/B 隔离）。
- **真机验收**：**PENDING**，验收项见下节。

## 七、真端对照验收项 / Retail acceptance

1. 击杀 Celestius（215488）后：应播放影片 437、出现净土艾昂之塔碎片 700741
   （条件 3083），并在 `548/811/1375` 出现**卡斯帕的幻影 799503**。
2. 与 799503 对话可交付心脏 `182215620`，任务 10032 进入领奖/完成。
3. 反例护栏：普通召唤物（如 mosqua egg 孵化的 282082）仍按 `live_time` 存活后消失；
   脱战（`on_leave_attack_state`）产生的标记物仍会随回位被清理。
