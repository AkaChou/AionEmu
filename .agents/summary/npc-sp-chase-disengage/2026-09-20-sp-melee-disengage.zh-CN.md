# max_chase_time=sp 怪物近战打一次就回出生点（857784）

- 日期：2026-09-20
- 地图：301570000（Archives Of Eternity / IDEternity_Q）
- 报障 NPC：857784（BIDEternity_Q_Sado_Pr_N_65_An_01，运行期 objectId 151173）
- 状态：静态修复完成；聚焦测试与客户端复验待执行。
- 影响范围：共享引擎规则，所有 `max_chase_time="sp"` 的 NPC，而不是单只 857784。

## 现象

- 攻击 857784 后，怪物会先追击/攻击，随后转身返回出生点，客户端表现为“打一下就转圈脱战”。
- 同一地图使用 sp 追击规则的怪物（如 857783、857903 等）共享同一状态机路径。

## 运行期证据

本次 22:53–22:54 的 AI 运行日志中，857784（objectId 151173）的关键序列如下：

| 时间 | 事件 |
| --- | --- |
| 22:53:31.308 | 受击进入 FIGHT，开始攻击 |
| 22:53:31.475 | 目标进入 1.45m 近战射程，普通攻击正常完成 |
| 22:53:33.622 | 目标跑远，进入追击；PATH 返回 FOUND |
| 22:53:36.196 | `scheduleNextAttack` 后立即 `ATTACK_FINISH -> NOT_AT_HOME -> RETURNING`，此时目标仍在 1.53m 内 |
| 22:53:38.347 | 重新受击进入 FIGHT 后再次 `ATTACK_FINISH -> NOT_AT_HOME -> RETURNING`，目标仍在 1.53m 内 |
| 22:53:51.540 | 再次在 1.44m 近战射程内结束攻击并返回出生点 |

这些回位事件之前没有 PATH 失败，日志连续出现 `PATH result status=FOUND`。因此“脱战”不是寻路失败或地形 NaN 导致，而是攻击调度提前执行了追击/回家判定。

## 静态数据

`src/main/resources/aion/definitions/compact/ai/npc-ai-parts/npc-ai_834041_885645.xml`：

```xml
<npc id="857784" name="BIDEternity_Q_Sado_Pr_N_65_An_01" ai="IDEternity_Q_Sado_Pr_02"
     max_chase_time="sp" react_to_pathfind_fail="return_to_sp"/>
```

当前 compact 扫描中，`max_chase_time="sp"` 的定义约 1,796 条（大写/小写/混合大小写合计），说明这是共享规则问题。

## 根因

1. `AttackManager#scheduleNextAttack` 在每次攻击调度前无条件调用 `stopRetailChase(npcAI)`。
2. `stopRetailChase -> shouldStopRetailChase` 对 `max_chase_time="sp"` 执行出生点判定：
   - 距离出生点超过 3m；
   - 每 2 秒检查一次；
   - 31% 概率停止追击并回出生点。
3. 857784 在近战站位时距出生点约 4.5m，已经满足距离条件；攻击链每轮都经过 `scheduleNextAttack`，因此会周期性触发回位。
4. 回位后玩家继续攻击又会重新进入 FIGHT，形成 `FIGHT -> RETURNING -> FIGHT` 抖动；客户端的转身/脱战表现来自这个状态循环，而不是路径绕圈。

## 修复

- 从 `AttackManager#scheduleNextAttack` 删除提前的 `stopRetailChase` 调用。
- 追击/回家判定仍保留在 `AttackManager#targetTooFar`：
  - `SimpleAttackManager#performAttack` 先判定目标是否真的不可攻击；
  - 目标超出射程或不可见时发出 `TARGET_TOOFAR`；
  - `targetTooFar` 再进入 `stopRetailChase`，继续支持数值 `max_chase_time`、`sp` 出生点规则和 `react_to_pathfind_fail`。

该修复保证目标处于近战射程内时不会因为 sp 随机判定而脱战，同时不删除真正的追击超时/回家语义。

## 验证

- 已新增源码闸门：`AttackManagerTest#attackSchedulingDoesNotStopRetailChase`。
- `git diff --check` 通过；IDE 检查仅保留 `AttackManager.java` 既有的 Javadoc warning。
- 聚焦测试待授权后执行：
  `mvn -B test -Dtest='AttackManagerTest,AttackManagerLeashTest'`
- 客户端复验待执行：部署后持续攻击 857784 及其周围 sp 怪物，近战射程内不应再出现打一次就回出生点。
