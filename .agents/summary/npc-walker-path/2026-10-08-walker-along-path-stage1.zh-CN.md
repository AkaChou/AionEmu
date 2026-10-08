# 行走 NPC「沿 Path 推进」切片 1+2：装载校验 + Path 接入 + 每步碰撞解算（阶段 0+1+2）

日期：2026-10-08
状态：**实机验收通过（2026-10-08）**——12:48 轮：spiros 6/6 被挡段沿 Path、抖动点减半且剩余均为
真实地形、无停-走-停/穿墙；13:02 轮：队长 205294 沿 Path 生效、四只动物保持直线零请求；
13:14 轮：重复预取根因（`moveToNextPoint` 丢预取）修复后每段恰好一次 `PATH request` ✓。
聚焦测试 7 类 112 例全绿 + 复跑 PathTest/WalkStreamTest 全绿；静态 0 error。
第三轮（长等待「原地空踏步」）修复：编队暂停停包改为**移动包截止驱动**（真端 `_CommonUpdate` 口径，§6.7）；
Ermona 休息点数据错位一顶点已按真端修正（§6.8）。两项均**待实机复验**。
（回滚：两开关改回 `off` / `false` 即完全恢复旧行为。）
关联：`npc-walker-stairs/RETAIL-WALK-SEMANTICS.zh-CN.md`（真端四层机制逐函数坐实）；AIM-009/010/011（贴地/编队/短段契约，本切片全部保留）。

## 1. 背景与目标

真端行走四层机制中，第 4 层（短段下发）与航点 Z/编队恢复已由 AIM-009/010/011 收口；本切片对齐剩余部分：

1. 装载期路线校验（≤100m / 可行走面 / 可寻路 / 闭环去重）；
2. **航点之间沿 Path 推进**（不再是直线）——真端 `NPC::GetCurWayPointGoalPos` 返回「当前航点 + 该段 Path」；
3. **每步碰撞/步高抬升解算**（切片 2，对齐真端 `fun_043`）：被挡按步高抬升 ≤9 次、向下打地面装回面上，仍不通则截断在墙前。

## 2. 设计决策（含关键兼容点）

### 2.1 Path 来源与时机：「换段时异步预取 + LoS 快捷 + 按段降级」

- **不做**装载期全量预计算（PATH 地图惰性加载）；**不做**同步 A*（红线：worker 内禁同步等 A*）。
- `setRouteStep`（每段仅一次）末尾 `refreshWalkerLegPath()`：
  - 编队成员 / 飞行 / 游泳 → 保持直线（保守，与现状一致）；
  - `blocked` 模式：对「当前位置 → 本段航点」做一次廉价 PATH-LoS（`PathService.canWalkStraightLine`，0.5m 格 Bresenham + 端点投影，≤10cm 高度偏差）；**通过 → 零变更**；失败 → `resetPath()` + `requestLocationPath()` 异步预取；
  - `always` 模式：全段预取（实机 A/B 用）；`off` 模式：完全等价旧行为（回滚保证）。
- 性能量化：spawn 数据 walker_id 引用 7717；换段频率数百段/秒 ⇒ 上述「LoS 快捷 + 仅被挡段 A*」是硬要求；LoS 每段一次（非每 tick）。

### 2.2 推进与短段流式（★ 最关键的兼容点）

- Path 就绪 → 复用 `moveAlongPath` 的节点推进骨架（`skipWaypoints` 已含 lookahead=3 节点跳跃）；**无 Path/走完/失败 → 回退直线走完本段，绝不进入追击的停车阶梯**（`stopForPath`/`finishFailedPointMove` 会让巡逻停走）。
- ★ **短段剩余量改用「沿折线到航点」的弧长口径**（`walkPathRemaining`），短段目标沿折线取点（`walkPathStreamTarget`，末端=航点本身）。
  理由：Path 节点间距 0.5m 级，若沿用「到最近节点」口径，0.75m 停发门槛会吞掉整段短段补发，客户端收到 <1m 的到点包 → 触发「接近目标即判定到达」→ 停-走-停（AIM-011 实机已否决该形态）。
  只要整段剩余 > 0.75m，客户端收到的都是 ≥1m 前视点的流式包，节点密度不影响客户端。
- 到航点判定不变（`isReachedPoint` 基于 pointX/Y/Z；服务端插值钳制保证精确到达）；换段由 `WalkManager.targetReached` 收口。
- 走 Path 的 walker **仍然逐 tick 贴地**（放宽 `shouldApplyGeoHeightCorrection` 的 `path != null` 排除：只对非行走者保留）；朝向平滑/单 tick Z 限幅原样。

### 2.3 prepareGroundPath 对行走者免复检免停车

`prepareGroundPath` 的「path 头可达复检」（有界 A*）与重规划停车分支对行走者短路——行走者的 Path 是「本段折线」（固定端点），不是动态追击目标；动态障碍由 `obstacleVersion` 检查兜底。短段推进保持 10Hz 的 O(1) 预算，且绝不发停包。

### 2.4 切片 2：每步碰撞/步高抬升解算（`walk.collision.enable`，默认关）

- 启用条件：`GEO_NPC_WALK_COLLISION_ENABLE && walkerWALK 子状态 && walkerLegNeedsPath`——平地/直线段零额外成本，行为逐字节不变。
- **耦合提示**：`walkerLegNeedsPath` 只在 `mode≠off` 的换段判定里置位，因此**单独开 collision 不开 mode 是 no-op**；实机 A/B 需两者同开。碰撞解算当前只对非编队行走者生效（`walkerLegNeedsPath` 与编队互斥）——编队成员保持 AIM-009/010 既有逐 tick 贴地语义；成员是否需要同款阶梯由实机 A/B 决定。
- 对齐真端 `fun_043` 阶梯（`resolveWalkerCollisionStep`，每 tick 一次）：
  1. `GeoMap.canPassWalker` 线段可通行检查（两端 +0.5m、skipFirstHit：地形微起伏放行、墙体阻挡）；通过 → 返回 null（不干预，交给逐 tick 贴地）；
  2. 被挡 → 抬升循环 i=1..9：`walkerLiftOffset(i) = min(1.5×i, 2.0)`（真端步高 1.5m、上限 2.0），对抬升后的目标点复检可通行；
  3. 抬升后通过 → 向下打地面 `GeoMap.getZ(x, y, 抬升Z, 抬升Z−3)` 装回面上，返回该点；
  4. 9 次仍不通 → `GeoMap.getClosestCollision` 取墙前最近点（截断在障碍前，**绝不穿墙**）。
- 接入位置：贴地段在 GEO 高度校正前先跑解算，解算点取代线性采样点；单 tick Z 限幅与「位移 >1m 重置方向」逻辑原样保留（不双重处理）。

### 2.5 装载期校验（`WalkerRouteValidator`，默认 log 只记录）

- 惰性 per-(worldId, routeId) 一次（首次在该世界启动该路线时，`WalkManager.startRouteWalking` 挂载）；
- 校验项：相邻 ≤100m（超限 warn）、每点可行走面（`projectGroundZ` 不兜底窄带，NaN=无地面 warn）、相邻点 PATH-LoS（不通 = **正常**的「需要 Path」段，计入统计）、闭环末点 <1m（info）；
- 汇总行（有需 Path 段或问题时）：`log.walker.route.summary`；`enforce`（按世界净化副本）留待后续切片。

### 2.6 开关与回退（`geodata.properties` / `GeoDataConfig`）

```properties
gameserver.geo.npc.walk.path.mode = off          # off（回滚态，当前）| blocked | always
gameserver.geo.npc.walk.collision.enable = false # 每步碰撞/步高解算（需 mode≠off 方生效）
gameserver.geo.npc.walk.route.validate = log     # off | log | enforce
```

`mode=off` 时：`usesPath()` 与旧分支逐分支等价、`refreshWalkerLegPath` 在 resetPath 前提前返回——运行路径与改造前一致。

## 3. 改动文件

| 文件 | 内容 |
|---|---|
| `configs/main/GeoDataConfig.java` + `aion/config/main/geodata.properties` | 3 个开关 |
| `world/geo/path/PathService.java` | 新增 `canWalkStraightLine`（廉价网格 LoS；刻意避开 `canReachWaypoint` 的有界 A*） |
| `spawnengine/WalkerRouteValidator.java`（新） | 装载校验（log 模式） |
| `ai2/manager/WalkManager.java` | `startRouteWalking` 挂载校验 |
| `controllers/movement/NpcMoveController.java` | 字段 `walkerLegNeedsPath`；`shouldUseWalkerPath` 纯谓词；`usesPath()` walker 分支；`moveAlongPath` 回退；`refreshWalkerLegPath`（LoS+预取）；贴地放宽；`walkPathStreamTarget`/`walkPathRemaining`；流式分支；`prepareGroundPath` walker 短路；`walkerLiftOffset`/`resolveWalkerCollisionStep`（切片 2 碰撞阶梯）+ 贴地段接入 |
| `model/templates/walker/WalkerTemplate.java` | `getRouteId()` |
| `messages.properties` / `messages_zh_CN.properties` | 4 个 key（中英对等） |
| `NpcMoveControllerWalkStreamTest.java` | +5 测试方法（模式谓词 / 折线取点与直线版对拍 / 弧长剩余量 / 步高封顶 2.0m / 挡墙截断不穿墙） |

## 4. 验证状态

- IDE 静态检查：改动文件全部 0 error。
- 聚焦测试（IDEA MCP，2026-10-08，用户授权）：**7 类全绿**（切片 1 集）—
  `NpcMoveControllerWalkStreamTest`(7) / `NpcMoveControllerPathTest`(62) / `LocalizedLogCallsTest`(1) /
  `WalkManagerTest`(2) / `InstanceWalkerFormationsPositionGroupingTest`(7) / `GeoMapWalkerCollisionTest`(2) /
  `PathDataTest`(28)。
- 切片 2 复核（同日）：`NpcMoveControllerWalkStreamTest`(9，+2 碰撞用例) 全绿；
  `NpcMoveControllerPathTest`(62) 复跑 exitCode=0 无回归。
- 第二轮改动复核（2026-10-08 第三批，IDEA MCP，用户授权）：**7 类全绿 112 例、0 失败** —
  `NpcMoveControllerWalkStreamTest`(11，含编队队长资格 / 同航点去重两个新用例) / `NpcMoveControllerPathTest`(62) /
  `LocalizedLogCallsTest`(1) / `WalkManagerTest`(2) / `InstanceWalkerFormationsPositionGroupingTest`(7) /
  `GeoMapWalkerCollisionTest`(2) / `PathDataTest`(27)。
- **实机 A/B（待用户执行）**：
  1. `aion/config/main/geodata.properties` 的 `gameserver.geo.npc.walk.path.mode` 改为 `blocked`、`gameserver.geo.npc.walk.collision.enable` 改为 `true`（碰撞解算依赖 mode≠off 的段判定，单独开 collision 不生效），重启服务端（部署目录配置文件由用户维护）；
  2. 观察点：
     - 台阶段（Verteron Ermona 东 3→6、西 13→17、南 9→11）：上台阶逐级爬、无「入地→拉起」、无停-走-停；矮障碍沿步高抬升贴面（1.5m/次、封顶 2.0m），不穿墙、不卡死；
     - 拐角：轨迹沿折线切内角、无穿墙、无原地转身（朝向 12°/tick 平滑仍生效）；
     - 平地/长直线段：`ai2 log` 中**不应出现** `PATH request kind=location`（blocked 零 A* 路径），行为与改造前一致；
     - `walkGroundStream from=… to=…` 节奏与改造前同构；`WALKER route validate` 日志给出「需要 Path」的段清单；
     - 若有异常：`mode` 改回 `off` 即完全回滚。
  3. 进阶对照（可选）：`always` 模式复核全段沿 Path 的表现差异。

## 5. 后续切片

- 切片 3：`enforce` 路线净化（per-world 副本）+ 编队成员沿领队折线弧长偏移（可选）+ `//geo walkercheck` 命令。
- 实机通过后：按项目惯例落 AIM 卡片（「行走沿 Path 推进」契约）并更新 memory-bank。

## 6. 2026-10-08 第二轮：日志判读与三项修正

### 6.1 判读结论：当天实机跑的是回滚态

- `geodata.properties` 在 11:42/11:55 两次服务端重启时均为 `off`/`false`（mtime 10:55 未再改，
  启动日志证实配置读自 `src/main/resources/aion/config/...`），当天「跳帧/拐角/穿墙」观察全部属旧基线。
- 校验器日志本身有效（29 条路线给出「需要沿 Path」段）。

### 6.2 三项修正

1. **配置开启（A/B 用）**：`walk.path.mode = blocked`、`walk.collision.enable = true`。
2. **编队队长参与沿 Path**（`NpcMoveController`）：新增 `isFormationFollower`（非零偏移即跟随者，
   `FORMATION_SHIFT_EPSILON = 0.01`；编队内缺 shift 按异常态保守排除）与 `walkerPathEligible`
   （PATH 开 + 非跟随者 + 非飞行 + 非空间寻路）。原因：`WalkerGroup.form()` 给**队长也设置
   walkerGroup**（OFFSET 型 shift (0,0)），原 `walkerGroup != null` 排除把整队（含队长）挡在门外。
   非零偏移成员保持直线成员段（AIM-009/010 语义），是否参与留待本轮 A/B 决定。
3. **校验器假阴性修复**（`WalkerRouteValidator`）：LoS 端点 Z 改用**地面解算值**（`projectGroundZ`，
   无地面回退模板 Z），与运行时 `refreshWalkerLegPath` 同口径。原因：模板 Z 与真实地面差 3–5m
   （LF1A 路线 124.1 vs 实际 ~121.3），`Sector.find` 垂直容差仅 0.7m ⇒ `projectPoint` 全部 null ⇒
   `canWalkStraightLine` 一律「视为可达」⇒ Verteron 地面路线全部假报 0 段需要沿 Path。

### 6.3 离线探针：Z 反向点是真实地形（`probe_bump_profile.py`）

- 复刻 `resolveGroundZ`（带 [z−101, z+1] 取最高面）对 spiros 轨迹 8 个采样点**逐点 ≤1mm 吻合**，
  含 12.7cm 反向点两侧。
- y≈1478（13→14 腿）剖面：7cm 矮棱（x≈1672.9）+ 11cm 圆包（峰值 121.43 @ x≈1676）+
  12cm 台阶边（(1688.5,1497.8)）；弦线最大偏差 **+0.204m**（远超 LoS 0.10m 容差，GeoMap 口径该段应判
  「需要沿 Path」）。同 x 两次经过的样本差异由航向 y 漂移 2cm 横跨地形坡度解释，
  **无棱线/双面/起始 Z 敏感**（sweep 平滑、无跳变、无次面）。
- 结论：这类 ±4–12cm 的「轻微上下跳帧」是**地形忠实跟随**（矮棱/圆包/台边），沿 Path 不改变走线；
  需要时只能另做客户端可见 Z 的平滑策略（会偏离贴地，需 A/B）。

### 6.4 下一轮实机观察点（重启后）

1. 启动日志：Verteron 路线应首次出现「N 段需要沿 Path」摘要（校验器修复生效；此前全 0）。
2. `ai2 log` 开在 spiros（203111）或队长 205294 上：被挡段应出现 `PATH request kind=location`，
   随后沿折线推进；四只小动物（非零偏移成员）仍为直线。
3. 台阶段/矮障碍：逐级爬、无穿墙卡死（collision 开）；客户端无停-走-停（弧长前视契约）。
4. 劣化即回滚：`mode`/`collision` 改回 `off`/`false`。

### 6.5 第二轮实机 A/B（2026-10-08 12:48 重启后）：通过

- **校验器（修复版）**：Spiros 16 步 **6 段**、Ermona 17 步 **7 段**需要沿 Path（修复前同为 0）；
  Verteron 其他地面路线同样出数（Potcrab 84 步 78 段、LehparAs 88 步 53 段等）。
- **运行时**：spiros 6 个被挡航段全部发起 `PATH request kind=location`（含 y≈1478 的矮棱+圆包段
  13→14），`PATH request failed` 0 条；轨迹 y≈1477.88（偏离直线弦 y≈1478.00 约 12cm）证明
  **沿折线推进生效**（非直线回退）。
- **抖动对拍**（同脚本口径，`analyze_aidebug_trace.py`）：
  旧 run（11:52，167.5m）行走中 Z 反向 ≥2cm **10** 处、≥5cm **4** 处、最大 12.7cm；
  新 run（12:56，90m）≥2cm **2** 处、≥5cm **2** 处。剩余两处均为**真实地形**（探针已证）：
  (1688.5,1497.85) 的 12cm 台阶边 12.9cm、x≈1672.9 的 7cm 矮棱 7.4cm；圆包段 ±4–10cm 来回消失。
- **用户判读**：台阶段/矮障碍不穿墙不卡死、无停-走-停、效果不错。
- **重复预取的真实根因（13:02 运行日志定位并修复）**：每个被挡航段成对 `PATH request`（相隔 ~100ms）——
  第二个请求**不是**重复 setRouteStep，而是 `moveToNextPoint()`（`NpcMoveController:836`）在
  `startRouteWalking` 的 `setRouteStep` 之后无条件 `resetPath()`，丢弃刚预取（或在途）的路径；
  首个移动 tick 的 `moveAlongPath` 见 `!cachedPathValid` 再发一次 A*。日志序列为铁证：
  `Setting step to 4` → request#1(FOUND) → `MC: moveToNextPoint started` → 首 tick `moveToDestination`
  → request#2（requestId 连续，两请求之间只有 reset 与 tick，无第二次 setRouteStep）。
  修复：`moveToNextPoint` 的 resetPath 改为条件执行（`walkerPathTargetsCurrentLeg()`：本段需要沿 Path 且
  在途/缓存路径目标仍是当前航点时保留）；`sameWalkerLegTarget` 同航点去重（平面 5cm/垂直 0.5m）保留为
  重复刷新场景的第二道防线。**13:14 实机复验（队长 205294，obj 22793）：3 个被挡航段 = 3 次请求（requestId 1/2/3
连续、全 FOUND、无成对）✓ 每段一次预取达标**；队长轨迹 32.6m 连续、最大单 tick 反向 4.4cm（平滑）。

### 6.6 队长 / 四只动物 / spiros 的 13:02 运行复核

- **队长 205294（obj 27009）**：6 个被挡航段全部发起 `PATH request`（全 FOUND），from/to 精确等于
  Ermona 航点对（如 step 2→3：1665.55,1465.86 → 1674.19,1463.23）⇒ **编队队长沿 Path 生效**；
  81m 轨迹连续、无停走。
- **四只动物（obj 27010–27013，非零偏移成员）**：**零** PATH 请求 ⇒ 保持直线成员段（按约定设计）；
  抖动指标与历史同构（27012：895 tick/104m，≥5cm 反向 3 处，最大 8.9cm —— 均为探针证实的真实地形）。
- **spiros（obj 27325）**：6 个被挡航段全部沿 Path；≥2cm 反向 3 处、最大 12.9cm 仍是
  (1688.5,1497.85) 的真实台阶边。
- 队长一次 17.3cm 单 tick 反向位于同一台阶边（(1687.8,1497.2)，换角度接近棱线）——真实地形，非回归。

### 6.7 长等待「原地空踏步」：编队暂停停包按移动包截止（真端口径，2026-10-08）

- 现象（用户报告）：队长 205294 与跟随者在**等队伍**时「原地空踏步、前进极小距离」。
- 日志判读（13:14/13:18 窗口，obj 22793）：到点 0.000m 干净；随后 `WALK_WAIT_GROUP` **5.1 秒**
  （等 −8m 成员 799737 归位：13:19:28,278 → 13:19:33,378 恢复，恢复时一次 `PATH request` ✓）；
  等待期间服务端**零移动、零包**；五个对象全窗口 `小位移tick` = 0 ⇒ 现象在客户端侧。
- 机制：`WalkerGroup.targetReached` 等队伍分支对已到位成员调 `pauseAtRoutePoint()` → `resetMove()`，
  按既定契约（PathTest `formationWaypointPauseDoesNotSendStopOrResetTheMoveMask`）**不发停、保留行走
  掩码**（历史上为避免暂停瞬间客户端尚在走最后 ~1m 时被冻住半途、恢复「回吸」）。短停顿没问题；
  但等 −8m 成员 ~5s（休息点更久），客户端全程保持行走态 → 原地踏步。
- 真端做法（本次坐实）：`NpcMotionController::_CommonUpdate`（NpcMotionController.cpp:193-195）
  每 tick 在「移动包截止已到（`motion[1] <= now`）且没有排队的下一段移动（`motion[0xd] == 0`）」
  时广播停包 `SendStopMovePacket`（duration=0、起=终）——**停包锚定在客户端走完最后一条移动包的
  时刻**，等待期间客户端被明确停住，从不原地踏步；放行后下一段移动包自然续走。
- 修复（取代 1s 固定宽限；`PAUSE_STOP_GRACE_MS` 已移除）：
  1. 每次移动包广播记录「客户端走完本包的估计时刻」`movePacketDeadlineMs = now + dist/speed`
     （`NpcMoveController` 广播点 + `movePacketDeadline` 纯函数）；
  2. `pauseAtRoutePoint` 把停包排到 `截止 + WALKER_STOP_DEADLINE_MARGIN_MS(100ms)`（`pauseStopDelayMs`；
     100ms 覆盖包延迟/一个 AI tick 调度抖动）；**在该时刻前恢复 → 代数作废、不发停**（短暂停行为
     逐字保留）；等待超过该时刻才 `setAndSendStopMove`，并发 AI2 log
     `pauseStop at route point (move packet deadline passed)`；
  3. 从未下发过移动包（截止为 0）不发停——真端同款护栏（其运动截止为 0 时不发停）。
- 验证：静态 0 error（仅存量警告）；`NpcMoveControllerPathTest` +2 用例
  （`pauseStopIsAnchoredToTheMovePacketDeadline`、`pauseWhileClientStillWalksTheLastSegmentSendsNoStop`）
  待跑。**待实机复验**：等队伍期间客户端在走完最后一段后即站定、不空踏步；恢复时无「回吸」。

### 6.8 数据修复：Ermona 休息点错位一个顶点（2026-10-08）

- 真端 world.xml `LF1A_NPCPath_Ermona`（`/tmp` 转换 UTF-16LE 后逐点核对）：`stay_duration=30000` +
  `stay_motion(Default idle motion)` 位于点 **1/6/12**（1659.969971,1470.829956 / 1683.656738,1481.128418 /
  1658.484131,1504.036743）；我们 XML 的 `rest_time="30000"` 原在 step **2/7/13**——step 1/6/12 的坐标与
  真端点 1/6/12 **逐位一致** ⇒ 同一路线、停留标记晚一个顶点。
- 引擎语义核对（确认可直接对齐真端索引）：`setRouteStep` 取**当前目标步**的 `getRestTime()`
  （`NpcMoveController:2589`），`WalkManager.chooseNextRouteStep` 在**到达该步**时消费（`abortMove()` +
  延时 `moveToNextPoint`，`WalkManager:301-317`）——与真端「停留读自刚到达的航点」
  （`NPC::GotoWayPoint` 读当前索引航点，NPC.cpp:5330-5343）同语义。
- 修复：`aion/data/static_data/npc_walker/210030000_Verteron_Walkers.xml` Ermona 的 `rest_time` 由
  2/7/13 移至 1/6/12（坐标/Z 未动）。对照 LehparAs_9 无此问题（我们 step 1/43/86 的 rest 坐标与真端
  点 1/44/88 物理吻合，仅编号差）。**待实机复验**：休息位置应在真端三点（较修复前沿路线前移一个顶点）。
