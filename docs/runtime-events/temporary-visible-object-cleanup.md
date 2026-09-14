# 定时世界活动临时对象生命周期修复记录

日期：2026-09-14

## 现象

17:00 贝里特拉回廊活动在重复周期后可能向客户端发送重复的传送门、黑天、激光和特效 NPC，存在客户端崩溃风险。排查时同时覆盖了其他使用相同活动启动流程的世界活动。

当前工作区没有真实客户端崩溃转储或在线复现证据，因此本文记录的是基于服务端对象生命周期的高可信根因和修复；最终仍需真实客户端运行验证。

## 根因

这些活动通过 `SpawnEngine.spawnObject` 创建并立即刷出临时对象，但旧实现使用固定 NPC ID 作为 `Map` key：

```java
adventEffect.put(702549, SpawnEngine.spawnObject(...));
```

同一活动阶段在多个地图坐标刷出相同 NPC ID 时，后一次 `put` 只覆盖服务中的 Java 引用，不会删除已经进入世界的前一个对象。下一轮活动继续覆盖引用，导致旧对象无法从服务侧统一回收。单次刷怪模板只限制重生行为，并不等同于活动结束时自动销毁对象。

## 排查范围

确认存在同类跨周期引用覆盖的活动服务如下：

| 服务 | 临时对象活动 ID | 旧问题 | 修复 |
| --- | --- | --- | --- |
| `BeritraService` | 1、35 | 4 组贝里特拉对象和 4 组艾雷什基伽尔对象使用固定 key 覆盖引用 | 已修复 |
| `AnohaService` | 1 | 剑光特效使用固定 key 覆盖引用 | 已修复 |
| `SvsService` | 5 | 进阶走廊对象只保留最后一份引用 | 已修复 |
| `RvrService` | 5 | 每个特效阶段的两个地图对象使用同一 key | 已修复 |
| `ZorshivDredgionService` | 3 | 四类入侵对象使用固定 key 覆盖引用 | 已修复 |

当前 17:00 的 Nightmare Circus 和 Conquest 活动，其状态 NPC 已通过地点的 `spawned` 集合进入既有 `despawn` 路径，未发现相同的临时特效 Map 覆盖缺陷。Windstream、Guard Post Generator 及副本对象也有各自的定时或副本销毁路径，本次未扩大修改范围。

## 修复方案

1. 将上述临时对象容器从固定 key 的 `Map` 改为 `Collections.synchronizedList`，每次刷出使用 `add`，保留本轮所有对象引用。
2. 每个活动的启动定时任务开始时先清理上一轮遗留对象，避免上一次异常中断后继续叠加。
3. 活动正常结束时调用 `onDelete()` 回收已刷出的对象，并清空列表释放引用。
4. 保留地点状态 NPC 原有的 `spawned`/`despawn` 生命周期，不改变活动地图、坐标、NPC ID 或客户端协议包。

## 验证

目标回归测试通过：

```text
mvn '-DtestExcludes=**/NpcMoveControllerPathTest.java' -Dtest=TemporaryVisibleObjectMapTest test
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

完整测试套件也已执行，但工作区中其他未提交改动导致基线失败：共 3192 个测试，6 个 Failure、3 个 Error、2 个 Skipped。失败涉及 Walker 分组、Retail AI 数量、任务契约、静态生成 XML、AI 选择，以及已有的 `NpcMoveControllerPathTest`；没有将这些无关改动纳入本次修复。

未启动服务或真实客户端，因此活动运行时和客户端崩溃是否完全消失仍需部署后验证。
