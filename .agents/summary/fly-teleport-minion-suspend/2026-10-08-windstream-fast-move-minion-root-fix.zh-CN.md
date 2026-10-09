# 风之路/高移速下守护灵「召唤了/取消召唤」刷屏：MinionKnownList 主仆距离免疫（CL-001 修订）

```text
report: 上轮修复（2b483f0db，飞行传送入口收起/落地恢复）后，用户实测进风道（windstream）或移速特别快时仍不停「取消召唤/召唤」
status: IMPLEMENTATION_COMPLETE + 门禁全绿（2026-10-09 IDEA MCP：MinionKnownListTest 2/2 + FlyTeleportMinionSuspendGateTest 3/3）/ RUNTIME_PENDING（风之路/高移速实机复验待用户回报；提交按用户 2026-10-09 授权先行）
changed: 新增 world/knownlist/MinionKnownList、VisibleObjectSpawner 装配点、新增 MinionKnownListTest
working tree: dirty（18602 等并行改动严格保留未动）
```

## 1. 为什么上轮方案盖不住

上轮按用户裁定采用「飞行传送入口收起、落地恢复」，修复范围 = 全部 `.setState(CreatureState.FLIGHT_TELEPORT)` 入口（9 处，门禁守护）。
但风之路（windstream）不设置 `FLIGHT_TELEPORT` 状态（`PlayerActions.setPlayerMode(WINDSTREAM)` 只写 `windstreamPath`），
普通高速移动（加速坐骑/GM 飞行速度等）更是没有独立入口可挂钩——**高速场景无法逐入口枚举**，这正是上轮 summary 边界里预留的假设（"若复现按同法扩展"）。

## 2. 根因不变，修复位点上移

机制链与上轮诊断一致（CL-001）：`CM_MOVE_IN_AIR`/`CM_MOVE` 刷新主人 KnownList → 95m 可见距离遗忘 minion → `SM_MINIONS(6)`；
跟随 tick（1s/2s，25m 瞬移）拉回 → 双向重建 → `SM_MINIONS(5)`。

本轮改为**根因修复**：主仆对在 KnownList 上互相视为永久在范围内，让可见性机制不再参与跟随物生命周期。
这是上轮诊断时提出的原方案，本次因边界暴露而落地。

## 3. 实现

| 文件 | 变更 |
| --- | --- |
| `world/knownlist/MinionKnownList.java`（新增） | 继承 `PlayerAwareKnownList`，覆写 `checkObjectInRange` 与 `checkReversedObjectInRange`：目标 == master 时恒为 true，其余对象走默认 95m 规则。两个覆写分别覆盖 minion 侧与主人侧的 `forgetObjects`/`findVisibleObjects` |
| `spawnengine/VisibleObjectSpawner.java` | `spawnMinion` 装配点 `new PlayerAwareKnownList(minion)` → `new MinionKnownList(minion)`（Pet 保持不变） |
| `world/knownlist/MinionKnownListTest.java`（新增） | ① 主仆两个方向恒在范围内（Objenesis 实例 + 反射注入 master）；② spawner 装配契约 + Pet 不受影响 |

方向覆盖推演（`KnownList.forgetObjects`/`findVisibleObjects`）：

- **主人侧**（X=玩家，object=minion）：距离判定失败后走 `minion.knownList.checkReversedObjectInRange(master)` → 豁免 → 不遗忘、可重发现；
- **minion 侧**（X=minion，object=master）：`checkObjectInRange(master)` → 豁免 → 不遗忘、可发现。
  缺一不可——minion 侧遗忘 master 时会经 `master.knownList.del(minion)` 再次触发 `notSee → SM_MINIONS(6)`。

## 4. 效果与既有行为

- 风之路、高移速、加速Buff 等一切高速场景：不再刷「取消召唤/召唤了」；minion 超过 25m 由跟随 tick 瞬移拉回（原有行为），位置以 `SM_MOVE` 同步；
- 上轮的飞行传送收起/恢复**保留不动**（已实机验收）：传送开始一条「取消召唤」、落地一条「召唤了」，传送期间 minion 不在世界；
- 旁观玩家对 minion 的可见性仍按各自 95m 规则（只有主仆对豁免）；死亡收回、离线、换宠、跨图 despawn 全部不变。

## 5. 验证状态

- IDE inspections：3 个文件 0 error；新增行 ≤120 字符，双语注释；
- **门禁全绿（2026-10-09，IDEA MCP）**：`MinionKnownListTest` 2/2（主仆双向豁免 + spawner 装配契约与 Pet 不受影响）+ `FlyTeleportMinionSuspendGateTest` 3/3（既有门禁回归）；`MinionKnownListTest` 反射写 `Minion.master` final 字段有 JVM 警告（与仓库既有 Objenesis 测试同款，不阻断）；
- **实机 PENDING**：冷重启后复验 ①进风道全程无循环消息、minion 沿途跟随；②高移速（坐骑/GM 速度）无循环消息；③普通速度行为无变化；④飞行传送仍是「开始一条收回、落地一条召唤」。
