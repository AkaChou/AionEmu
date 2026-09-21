# 永恒之塔入口可用性：重启后立即开放 + 整点随机刷新 / Tower Of Eternity entrance uptime

- 日期：2026-09-21
- 范围：`TowerOfEternityService`（世界入口刷出时机与开关归属）
- 状态：`IMPLEMENTATION_COMPLETE / FOCUSED_TESTS_PASSED / CLIENT_ACCEPTED`

## 需求

入口原先只在整点 cron 触发后出现。中途重启服务器后，到下一个整点前 Iluma（阿斯泰拉 `210100000`）与
Norsvold（诺斯珀德 `220110000`）的 5 个刷新点全部没有入口。要求：重启后立刻有入口；整点仍按原规则随机
刷新，保证任何时刻进游戏都能找到入口。

## 修改前的代码事实

- 初始化为全 CLOSED：`TowerOfEternityService.initTowerOfEternityLocation()` 对 10 个地点 `spawn(loc, CLOSED)`，
  而 `spawns/Tower_Of_Eternity/*.xml` 的 CLOSED 块为空 → 关闭态没有任何入口对象。
- 只有 cron（`gameserver.tower.of.eternity.schedule = 0 0 */1 ? * *`，`duration = 1`）触发
  `startTowerOfEternity(Rnd.get(1, 5))` / `Rnd.get(6, 10)` 才刷出 OPEN 入口（`VisibleObjectSpawner.spawnTowerOfEternityNpc`
  只在 `loc.isActive()` 且 OPEN 时创建 NPC）。
- 关闭计时器按 **地点 ID** 回调 `stopTowerOfEternity(id)`：同一 ID 在计时器到期前被重新开放时，旧计时器会误停
  新实例；`putIfAbsent` 也会吞掉同 ID 的一轮开启。
- 运行时证据：当前进程 14:14:02 初始化（`log/console.log:135522-135523` “初始化完成 / 已加载 10 个位置”），
  下一次整点 15:00 前全图无入口。

## 变更

- `TowerOfEternityService`
  - 新增 `openRandomTowerOfWorld(worldId)`：随机取该世界的地点 ID，**先关闭该世界已开放的地点再开启新地点**，
    保证每个世界同一时刻只有一个入口。
  - `initTowerOfEternityLocation()` 在注册 cron 之前先对 `210100000` / `220110000` 各开一个随机入口 → 重启后立即有入口。
  - 整点 cron 改调同一方法 → 保留“整点随机刷新”，并顺带消除同 ID 的一轮吞开启。
  - 关闭计时器改为 `stopTowerOfEternity(id, tower)`（按实例用 `ConcurrentMap.remove(key, value)` 校验归属），
    新实例不会被旧计时器误停；新增 `towerTransitionLock` 串行化启动 / 刷新 / 计时器回调。
- 新增本地化日志键 `log.tower.entrance_opened`（`messages.properties` 与 `messages_zh_CN.properties`，占位符一致）。
- 新增 `TowerOfEternityServiceTest`：旧计时器不得移除同 ID 新实例；归属计时器正常停止。

## 验证结果

- 聚焦门禁（2026-09-21，用户授权后执行）：`mvn -Dtest=TowerOfEternityServiceTest,LocalizedLogCallsTest,LocalizedLogArgumentsTest
  -DfailIfNoTests=false test` → `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`（19.5 s，测试源
  1045 个文件全量编译通过）。原始日志见同目录 `mvn-focused-2026-09-21.log`。
- 真实客户端验收（2026-09-21，用户回复「验证成功」）：重启后入口立即可用，整点继续随机刷新。
- 运行时观测点：`log/console.log` 出现 `log.tower.entrance_opened`（“永恒之塔入口已开启：世界 …, 地点 …”），
  可用于确认每轮开放的世界与地点 ID。

## 边界说明

- `gameserver.tower.of.eternity.duration = 1` 与每小时刷新匹配时不出现空窗；若把 `schedule` 改成间隔大于
  `duration`，仍会出现空窗（持续时间配置语义未改）。
- 本地图配置只在 `gameserver.tower.of.eternity.enable = true` 时生效；关闭配置时启动日志仍为
  “永恒之塔活动已在配置中禁用”，不会有入口。
