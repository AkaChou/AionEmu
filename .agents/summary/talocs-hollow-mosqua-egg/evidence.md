# Queen Mosqua 卵孵化链被回位取消 / Mosqua egg hatch chain canceled by return-home

## 症状 / Symptom

- Taloc's Hollow（map 300190000）中 NPC 215480（Queen Mosqua）持续召唤 `mosqua egg`
  （npc 282006）。
- 卵会进入战斗、放弃追踪并回位，但不会孵化或消失；Boss 继续按阶段召唤，卵持续变多。

## 真端数据链 / Retail data chain

- `npcaipatterns_idelim_osy.xml` `Elim_NeutflyNm`（npc 215480）在 HP 阈值战斗计时器中
  多次 `spawn_on_target` npc 282006：
  - HP < 50%：每 20 秒召唤 4 个 `SPAWN_ID_3`，`live_time=0`；
  - HP < 80%：首次召唤 3 个 `SPAWN_ID_5`，`live_time=300`。
- 卵自身 `Elim_NeutflyEgg` 的 `on_see_user` 动作序列是：
  1. `use_skill` `SKILLI_INDEX_0` = skill 19231 `NWI_Hatch_Nr`；
  2. `spawn_on_target` 282082 `BIDElim_NeutWorkmanflySummon_51_n`，`live_time=18`；
  3. `despawn_self`。
- Skill 19231 的模板 `duration=15000`，因此引擎把后续的 spawn/despawn 排在 15 秒后，
  这 15 秒就是卵的孵化窗口。

## 根因 / Root cause

- `RetailPatternAI2.executeActions` 把技能后的后续动作放进 `actionTasks` 延迟队列。
- 卵移动速度为 0，进入战斗后 `TargetEventHandler.returnToSpawn` →
  `ReturningEventHandler.onNotAtHome` 会直接 `teleportHome` 并触发 `handleBackHome`。
- `RetailPatternAI2.handleBackHome` 原先 `resetPatternState(Set.copyOf(actionTasks))`，
  会在卵孵化窗口内取消整个 spawn + `despawn_self` 链；同时清空 flag。
- 卵回到 idle 后再次看到玩家会重复上述循环，所以永远停留在“卵”状态并不断累积。

## 修复 / Fix

- 为包含 `despawn_self` 的事件动作链增加 `terminalActionTasks` 标记。
- `handleBackHome` 和 RETURNING 状态下重新受击的 `resetPatternState` 只取消普通队列，
  保留这类自消失事件链；死亡/真正 despawn 仍取消全部队列。
- 这样卵在 15 秒孵化窗口内即使放弃追踪并回位，仍会完成
  spawn 282082 + `despawn_self`。

## 验证边界 / Validation boundary

- IDE errors-only inspection passed；`git diff --check` passed。
- 未执行 Maven、服务端或真端客户端复验。
- 仍需真端确认孵化出的 282082 是否能按 `live_time=18` 正常存活；若它随卵的
  `despawn_self` 被 `spawned` 自动清理，需要在下一层单独处理召唤物生命周期。
