# Ermona 巡逻队：小动物上下台阶悬空/入地 + 205294 对话后领队抢跑 — 诊断与修复

日期：2026-10-06
状态：**两处根因已定位并修复（实机验收通过）；残留对齐 v3 已实现并通过聚焦测试、待实机复测** —
v1（前视 0.8m + 节流/门控）实机停-跳已回退；v2（前视 1m）实机残留「脚部入地然后拉起来」，
经 ai2 日志定量归因为**前视过长**（见 `LOG-ANALYSIS-799737.zh-CN.md`）；
v3 = 前视改为「本 tick 实测位移 × 1.3（下限 0.15m）」+ 取消地形门控（换腿首 tick 起每 tick 补发）。

## 0. 现象（用户报告）

1. NPC 799731 / 799746 / 799736 / 799737（Verteron 210030000 `LF1A_NPCPath_Ermona` 队的四只小动物）
   巡逻时：上台阶「上上下下地跳」或「升空又下来」，下台阶「入地」。
2. NPC 205294（领队 Tenos）对话完后「提前往前走」，后面的小动物没及时跟上。
3. 用户开启了 799737 的 `ai2 log`，要求判读 `log/aidebug.log`。

## 1. AI2 日志判读（799737 = objectId 26056）

- 日志只有 799737 一个对象（objectId 26056），仅四个窗口有记录：
  16:55:59–16:56:02、16:57:32–16:58:21、16:58:30–16:59:04、16:59:45–17:00:28。
- **窗口之间的「位置瞬跳」（最大 31.8m）是用户自己反复开关日志造成的**：`log/adminaudit.log`
  16:55:53–17:00:28 有 9 条 `ai2 log`（目标=white dagg）切换记录，与窗口边界逐一对应。
  窗口内连续记录段**没有任何位置跳变**。
- 窗口内服务端轨迹平滑（逐 tick |Δz| 中位数 0.003m，仅 2 次方向翻转）；到达时 ownerZ 与目标 Z
  差 ≤0.02m；`MOVE_VALIDATE → onTargetTooFar → default` 是行走态的每 tick 常规噪音（default 分支空操作）。
- 日志覆盖东/南/北侧；**西侧爬升段（路线步 13→17，Z 119.0→121.0）恰好在两个关日志窗口里没被记录**。

### 1.1 从日志反解出的编队语义（已用脚本逐点验证）

- 成员航点 = `getLinePoint(前一路线点, 当前路线点, shift)`，shift=(offsetsx_i, offsetsy_i)；
  799737（walker_index=4，offset (0,-8)）的 13 次目标切换全部命中（≤0.3m），
  见 `decode_walker_offsets.py`（含修正后的 `getLinePoint` 复刻：coronal 分支 sagittal=0 时取
  `abs(coronal)`）。
- 四只动物的编队偏移：799731 −2m / 799746 −3m / 799736 −6m / 799737 −8m（沿路径后退）；领队 205294 = 0。

## 2. 根因①：编队成员的航点 Z 取「上一步坐标」，斜坡/台阶段误差随偏移放大

`NpcMoveController.setRouteStep`（修复前）对编队成员：

```java
localPoint2D = WalkerGroup.getLinePoint(上一步, 当前步, shift);   // 航点 X/Y：后退 offsetsy 米
this.pointZ = resolveRouteStepZ(paramRouteStep2);                  // ← Z 却取「上一步」坐标处地面
```

- 航点与 Z 采样点相距最多 8m；台阶段上每米落差直接变成航点 Z 误差。
- **且行走态（substate=WALK_PATH）被排除在逐 tick 贴地修正之外**
  （`NpcMoveController` 的 `shouldApplyGeoHeightCorrection && ... && getSubState() != AISubState.WALK_PATH`），
  所以错误的航点 Z 就是 NPC 的真实 Z，也是下发给客户端的目标 Z（客户端即按此渲染）。
- 误差表（离线复刻 `GeoMap.getZ`＝网格面 ∪ 地形图取带内最高面，`probe_route_ground.py`；
  **17/17 路线点与实机日志的 `resolveRouteStepZ` 采样值逐位一致**，例如 step10=119.125、
  step11=118.875，模型可信）：

| 编队成员 | offset | max\|误差\| | 主要位置（旧值−真值，>0 悬空 / <0 入地） |
|---|---|---|---|
| 799731 | −2m | 0.26m | step→5 −0.26 |
| 799746 | −3m | 0.27m | step→5 −0.27 |
| 799736 | −6m | 0.84m | step→10 −0.84；step→1 +0.63；step→2 +0.27 |
| 799737 | −8m | **1.17m** | step→10 −1.17；step→1 +1.08；step→2 +0.85；step→11 −0.79；step→5 −0.52；step→4 +0.48 |
| 205294（领队） | 0 | 0 | 不受影响（用户只报四只动物，吻合） |

- 符号即表现：旧值偏高（+）→ 客户端把 NPC 摆在地面之上＝悬空/升空；偏低（−）→ 插入地面＝入地；
  相邻航点符号/幅度交替 → 上上下下的跳。平台段误差≈0（所以是「可能」发生、间歇性）。

## 3. 根因②：对话结束后领队单独抢跑一步，编队永久错开

链路：`CM_CLOSE_DIALOG → DIALOG_FINISH → GeneralNpcAI2.handleDialogFinish →
TalkEventHandler.onFinishTalk → think() → ThinkEventHandler.thinkWalking → WalkManager.startWalking
→ startRouteWalking → findNextRoutStep`。

- `findNextRouteStepAfterPause`：若 NPC 站在「当前路线点」1m 内，直接推进到**下一步**。
- 领队偏移为 0（站位于路线点本身）→ 对话结束恢复时命中该分支，**单独走向下一步**；
  四只动物偏移 2–8m，距路线点 >1m，不抢步，仍在 `WALK_WAIT_GROUP` 等领队到齐。
  → 领队领跑一整段后动物才动；且 `WalkerGroup.setStep` 把 groupStep 抬到领队的新步 →
  **领队此后永久比编队超前一步**（「没有及时跟上」不会自愈）。

## 4. 修复

1. `src/main/java/com/aionemu/gameserver/controllers/movement/NpcMoveController.java`
   - 新增 `resolveGroundZ(x, y, fallbackZ)`（`resolveRouteStepZ` 改为其薄封装）；
   - `setRouteStep`：编队成员航点 Z 改为**按航点自身坐标**采样
     （`resolveGroundZ(pointX, pointY, 上一步模板 Z)`）。回退语义与旧实现一致；
     领队偏移 0 时采样坐标与旧值相同（无行为变化），非编队行走者完全不变。
2. `src/main/java/com/aionemu/gameserver/ai2/manager/WalkManager.java`
   - `findNextRoutStep`：编队成员一律走 `findClosestRouteStep` 的编队分支
     （groupStep<2 → 第 1 步，否则 groupStep 对应点），不再走个体「推进下一步」；
   - 抽出纯判定 `shouldResumeIndividually(inWalkerGroup, currentPoint)` 供测试锁定规则。
3. `src/test/java/com/aionemu/gameserver/ai2/manager/WalkManagerTest.java`
   - 新增 `onlyUngroupedWalkersResumeIndividually`（无编队个体续走保留；编队成员不得抢步）。

## 5. 验收状态

- IDE 静态检查：三个改动文件 0 error。
- 聚焦测试（2026-10-06，IDEA MCP runner，**79 例 0 失败**）：

| 套件 | 结果 |
|---|---|
| WalkManagerTest（含新增 onlyUngroupedWalkersResumeIndividually） | 2/2 |
| NpcMoveControllerPathTest（移动控制器全量回归） | 62/62 |
| InstanceWalkerFormationsPositionGroupingTest（编队/偏移） | 7/7 |
| ThinkEventHandlerTest | 1/1 |
| FollowManagerTest | 5/5 |
| TargetEventHandlerTest | 2/2 |

- 实机复测（2026-10-06，用户判读，已重启服务端生效）：**通过** —
  「编队恢复、不再掉队、不再悬空、不再入地」。
  残留：上台阶时仍偶有轻微上下跳帧（不严重，未复现悬空/入地）。
  残留机理推断：航点间为直线 3D 插值（每个航点只发一次 SM_MOVE），台阶段弦线相对台阶面
  存在固有几何下垂，与航点 Z 采样误差无关（根因①已消除，量级从米级降至台阶半高以内）。
- 如需再抓日志：`ai2 log` 开着跑满一圈（≥4 分钟，特别是西侧 13→17 与南侧 9→11），
  新代码下这些点的 ownerZ 应贴地（旧代码在 step→10 处比地面高约 1.2m）。
- **残留对齐 v1（已回退）→ v2（实机残留，已定量归因）→ v3（待实机复测，2026-10-06 第二批）**：
  真端客户端移动包经逐字段对账也无时长字段（时长只用于服务端「到期即发」的调度，
  见 `RETAIL-WALK-SEMANTICS.zh-CN.md` §3.1）。
  v1（前视 0.8m + 200ms 节流 + 逐 tick 偏差门控）实机「卡在原地，然后往前瞬移」——门控/节流
  让包晚于客户端到达，属**节奏错配**，已回退并入库 **AIM-011**。
  v2（前视 `max(1m, 速度×0.3s)`、每 tick 补发、按段粘滞）实机残留「脚部入地然后拉起来」；
  用 ai2 日志 + 客户端离线模拟定量归因：**前视过长**（1m vs 每 tick 行程 0.1–0.2m，客户端每 tick
  只爬弦线 1/5–1/10，台阶段竖向滞后 → 入地 → 下一包拉回）+ 换腿后门控延迟 6–7 tick（客户端先走
  8–9m 长弦再被一次性拉起）。前视扫描（1.15/1.3/1.5/2/3 × 实测位移）：1.3 时 >4cm 修正从 9 次
  降到 3 次（且都在台阶棱线），1.15 时 0 次。详见 `LOG-ANALYSIS-799737.zh-CN.md`。
  v3 = 前视 = 本 tick 实测位移 × 1.3（下限 0.15m，自动适配 100/200/500ms 档）+ 取消门控
  （换腿首 tick 起每 tick 补发，不再下整段长弦）；`ai2 log` 可见 `walkGroundStream from=… to=…`。
  聚焦测试（2026-10-06 IDEA MCP）：`NpcMoveControllerWalkStreamTest` 3/3（更新）、
  `NpcMoveControllerPathTest` 62/62（0 失败）。
  实机复测点：① 台阶段是否还有入地/拉起；② 是否出现停顿/前跳（有 → 前视系数 1.3→1.5；
  仍有入地 → 1.3→1.15）；③ 平地/长直线段不应出现新行为差异。
- 未验证替代（留档、未采用）：远目标重锚（同目标、周期刷新起点）；台阶段路线点子航点加密。

## 6. 本目录脚本

| 脚本 | 用途 |
|---|---|
| `decode_walker_offsets.py` | 用实机日志反解编队偏移语义（修正版 getLinePoint 复刻，13/13 命中） |
| `probe_route_ground.py` | 离线复刻 `GeoMap.getZ`（PHYSICAL 网格面 ∪ 地形 PNG，取带内最高面），输出路线/航点地面与误差表；17/17 与实机日志吻合 |
| `trace-23942-latest.txt` | 2026-10-06 18:47:52–18:48:49 的 799737（23942）移动日志切片 |
| `analyze_walk_stream_sag.py` | 逐 tick 贴地误差 + 弦线-地面偏差（入地/浮空深度）分析 |
| `sim_client.py` | 忠实客户端模拟（吸附 from → 沿弦线走 → 到点停），输出位置偏差与吸附瞬间的竖向修正 |
| `sweep_lookahead.py` | 前视距离参数扫描（固定 L / 自适应 K×实测位移） |
