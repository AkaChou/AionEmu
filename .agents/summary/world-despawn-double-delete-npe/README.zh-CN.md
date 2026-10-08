# World.despawn 并发双删除 NPE 收口（BT 300170000 实例）

> date: 2026-10-08
> status: 修复已落地，待重启服务端实机复测（PENDING_CLIENT）
> scope: `src/main/java/com/aionemu/gameserver/world/World.java` — `despawn(VisibleObject, boolean)`

## 现象（原始日志）

```
10-08 16:41:55 ERROR [pool-5-thread-24] c.a.g.world.WorldMapInstance - 对地图实例内的所有 NPC 运行访问器时异常：地图 ID=300170000，实例 ID=2
java.lang.NullPointerException: Cannot invoke "MapRegion.remove(VisibleObject)" because the return value of "VisibleObject.getActiveRegion()" is null
    at World.despawn(World.java:566)
    at World.despawn(World.java:551)
    at VisibleObjectController.delete(VisibleObjectController.java:44)
    at CreatureController.delete(CreatureController.java:561)
    at VisibleObjectController.onDelete(VisibleObjectController.java:85)
    at AI2Actions.deleteOwner(AI2Actions.java:34)
    at RetailPatternAI2.executeActions(RetailPatternAI2.java:1500)   // despawn_self 动作
    at RetailPatternAI2.runEvent(...)                                 // on_message
    at RetailPatternAI2.lambda$broadcastMessage$1(RetailPatternAI2.java:2268)
    at WorldMapInstance.doOnAllNpcs(WorldMapInstance.java:614)
```

背景：用户在 BT（300170000）实机测试条件刷链（Ahbana / 纪念碑 / 摆渡人）时使用了 GM 隐身命令；
错误后 2 秒同实例出现 `[cond-var] spawn world=300170000 cond=2094 npc=799520`，说明该链当时正在活跃触发。

## 根因

1. `VisibleObject.getActiveRegion()` → `WorldPosition.getMapRegion()` **以 `isSpawned` 为门**：
   `return isSpawned ? mapRegion : null;`（WorldPosition.java:59-61）。
2. `World.despawn` 561 行已把区域捕获进局部变量 `oldMapRegion`，但 562-566 行**重新三次调用 `getActiveRegion()`**。
3. 并发双删除同一 NPC（典型：`broadcastMessage` 的 1ms 调度波把 `on_message` 送达某 NPC → 其规则链走到
   `despawn_self`；同一时刻该 NPC 自己更早一条延迟续接任务/到期清理也在 `despawn` 它）：
   - 线程 A：`delete()` 的 `isSpawned()` 判过（`VisibleObjectController.java:43`）→ `despawn` 562 行判空通过；
   - 线程 B：先执行到 568 行 `setIsSpawned(false)`；
   - 线程 A：恢复执行 566 行 → `getActiveRegion()` 因 `isSpawned==false` 返回 **null** → NPE。
4. 典型时序波及：`despawn_self` 前置 `use_skill` 时动作链被拆成延迟任务（RetailPatternAI2.java:1491-1496），
   与后续广播波的同步 `despawn_self` 天然构成两条并发删除链。

结论：不需要「另一线程显式置空 region」——`isSpawned` 翻转本身就是置空。这是判空后二次读取（TOCTOU），
不是规则数据缺陷；零售引擎对重复删除本就容忍，属于我们世界层缺幂等护栏。

## 修复

`World.despawn`（World.java:560 起）：区域**只读一次**进 `oldMapRegion`，判空与 remove 全程复用局部变量：

```java
MapRegion oldMapRegion = object.getActiveRegion();
if (oldMapRegion != null) {
    if (oldMapRegion.getParent() != null) {
        oldMapRegion.getParent().removeObject(object);
    }
    oldMapRegion.remove(object);
}
object.getPosition().setIsSpawned(false);
```

修复后并发双删除退化为幂等无害操作：
- `MapRegion.remove` 自带 `objects.remove(id) != null` 守卫（MapRegion.java:174-180）；
- `setIsSpawned(false)` 重复置位无副作用；
- `revalidateZones` 内部对未生成生物走 `zone.onLeave` 分支（MapRegion.java:309）。

与既有模式一致：`0deee46f9` 已给 spawn 侧加「bringIntoWorld 统一入口护栏」，本修复是 despawn 侧的镜像收口
（世界层统一入口护栏，不动 AI 规则语义）。

## 不做的事与理由

- **不给 `RetailPatternAI2` 的 `despawn_self` 加 `isSpawned` 前置护栏**：`delete()` 对「未生成但在世界」的对象
  仍需走到 `world.removeObject` 注销；在 AI 层短路会漏掉合法清理，且修不掉竞态窗口本身。
- **不动 `World.preSpawn` 的同类双读**（523-524 行两次 `getActiveRegion()`）：同属一类 TOCTOU，但无实机案例、
  且玩家 preSpawn 与并发 despawn 相撞的窗口远窄于 AI 链。登记为后续观察项，暂不扩 diff。

## 验证

- IDE 静态检查（errorsOnly）：`World.java` 无错误。
- Maven / 单测：未授权未执行（AGENTS.md 全局规则 1）。
- 实机复测（待用户重启服务端后）：BT 内再次触发纪念碑 → Ahbana/摆渡人链（含隐身观察），
  确认 300170000 不再出现本 NPE；广播波与到期清理并发时 NPC 正常消失、无残留。
