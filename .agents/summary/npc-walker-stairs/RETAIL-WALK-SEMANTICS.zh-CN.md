# 真端 NPC 行走语义（反编译实证）— 台阶段为什么不抖

日期：2026-10-06
来源：`<真端根>/server58-source/NPCServer_NPCSvr64/`（Ghidra 反编译；未命名函数按调用语义判断，已标注）
关联：`DIAGNOSIS.zh-CN.md`（本次修复的残留项：上台阶轻微上下跳帧）

## 0. 结论

真端行走**不是「航点之间直线插值」**，而是四层机制叠加；它没有台阶抖动，也不靠更密的作者数据：

1. **装载期**：每个航点吸附到可行走面，路线整体校验（相邻 ≤100m、可寻路、闭环 1m 去重）。
2. **航点之间**：用 Path（`PathFind.exe` 生成的 `<world>-path.dat` 连通图 + 运行时 A*）连接，而非直连。
3. **行走推进（地面 NPC）**：每一步做碰撞/地表解算——线段碰撞查询、按步高抬升探测（最多 9 次）、向下打地面查询把移动点放回面上；飞行 NPC 才走纯直线。
4. **下发**：移动包带起点/终点/时长（=距离/速度，下限 100ms），控制器到期即再发 → 客户端插值的是推进中的**短段贴地**序列。

## 1. 证据

### 1.1 装载期航点吸附 + 路线校验（日志串原文可读）

`classes/Misc/WayPointInfo.cpp:307` `WayPointInfo::CalcWayPointZPos`（两个调用点：
`WayPointInfo.cpp:799` 创建时、`classes/World/World.cpp:19507` 世界装载时）：

- 逐点调用世界碰撞投影并**写回航点坐标与 Z**；失败即截断路线：
  `"Invalid way point data : world(%s), %s (not on walkable surface) x=%.1f, y=%1.f, z = %1.f"`。
- 相邻点必须 <100m：`"too long way point data : world(%s), %s ... %5.2f m, should be less than %d m"`（阈值 100）。
- 每个点必须落在有寻路数据的扇区里：`"… no path data for (%.2f %.2f %.2f)"`；
  相邻点必须可寻路（预计算）：`"… (can't find path)"`。
- 参数 `param_3 != 0` 的分支还会做闭环收口：末点距首点 <1.0m 则去掉末点。

对照：真端 `Map/Worlds/lf1a/world.xml` 的 `LF1A_NPCPath_Ermona` 与仓库
`src/main/resources/aion/data/static_data/npc_walker/210030000_Verteron_Walkers.xml:2843`
逐点相同、z 同为 `124.100006` —— **真端同样不信数据里的 Z，是引擎算的**。
本仓库 AIM-009 的修复（航点 Z 按航点自身坐标采样）即该语义的运行时等价物。

### 1.2 航点之间是 Path，不是直线

- `classes/Misc/PathSector.cpp:113`（绑定到 PathSector 类、实为 `WorldPathFindData::Load`）：
  逐世界读取 `%s\Worlds\%s\%s-path.dat`（由 `PathFind.exe` 生成，头部版本 `0x60005`），
  扇区类型：Flat(0) / FlatWithLink(1) / **Terrain(2)** / TerrainWithLink(3) / Simple(4,6,8,…) /
  SimpleWithLink(5,7,9,…) / Complex(16)（`classes/World/TerrainPathSector.cpp` 等类即由此而来）。
- `classes/NPC/NPC.cpp:17254/17324` `NPC::GetCurWayPointGoalPos`：返回「当前航点 + 该段 Path」
  （`WayPointInfo::GetWayPointPath`）。
- `classes/NPC/NPC.cpp:19160` `NPC::MoveToGoal`：沿 Path 推进；路径来源优先用传入的预计算 Path
  （`Path_Clone`），否则 `NPC::FindPath`（A*）现算。

### 1.3 每步推进做碰撞/地表解算（核心）

`fun/fun_043.cpp:3116`（`NPC::MoveToGoal` 的推进函数，Ghidra 未命名）：

- 先按直线算推进点；`if (local_150 == 0)`（Ghidra 推断：地面 NPC，非飞行）进入解算循环：
  1. 线段碰撞查询（`FUN_1402f1e20`，按调用语义）从当前位置到推进点；
  2. 被挡则按步高向上抬（`fVar26`：默认 1.5，NPC 自带值上限 2.0），最多 9 次；
  3. 每次抬升后向下打地面查询（`FUN_1402f2480`，按调用语义）取脚下可行走面，
     把移动点放到面上（`local_1d0 = fStack_1b0 + fVar26` 等）。
- 9 次仍冲突：记 `"[DEBUG] Too many collisions …"` 并把移动点直接置为路径节点。
- 飞行 NPC（`*param_3 + 0x90` 非零）跳过整个解算，直线推进。

推论：地面 NPC 的**每个模拟位置都贴面**，台阶逐级爬；不存在长距离 3D 弦线。

### 1.4 位置持续下发

`classes/NPC/NpcMotionController.cpp:410` `NpcMotionController::SendStartMovePacket`：

- 组包 `FUN_14019c2c0(…, 位置A, 朝向, 位置B, 时长ms, flags, …)`；
  时长 `= 距离/速度×1000`，下限 100ms（同函数内另有固定 667ms 的转身/特殊分支）。
- 控制器维护 `下次应发时间`（`param_1[1]`/`*param_1`），到期且位置有更新即再发
  （`NpcMotionController__CommonUpdate`，`NpcMotionController.cpp:133`；
  `fun/fun_035.cpp:721` `FUN_1401b2510` 写位置并置脏位）。
- 即：客户端拿到的是**推进中的短段位置序列**（每段 ≥100ms），不是一整段 A→B。

## 2. 与 AionEmu 现状对照（残留根因）

| 环节 | 真端 | 本仓库现状 |
|---|---|---|
| 航点 Z | 装载期吸附可行走面 | AIM-009 已修：运行期按航点自身采样（语义等价） |
| 段内轨迹 | 沿 Path 节点 + 每步碰撞/地表解算 | 直线 3D 插值（`NpcMoveController.java:1001-1003`） |
| 逐 tick 贴地 | 每步解算 | 有实现（`:1016-1034`，含平滑与单 tick 限幅），但被 `path != null` 与 `subState == WALK_PATH` 排除（`:1016-1019`）→ 行走者完全不贴地 |
| 客户端下发 | 每段 ≥100ms 的推进位置 | 每段（航点）仅一次 SM_MOVE（`:1049-1060`）→ 客户端把 3–20m 的段画成直线 |

补充事实：`NpcMoveController.usesPath()`（`:1277-1280`）为
`GEO_PATH_ENABLE && !(subState == WALK_PATH)` —— **行走者被显式排除在寻路服务之外**，
与实机 ai2 日志一致（每段目标 = 编队偏移后的路线点，无中间寻路节点）。

## 3. 对齐尝试与结论（客户端包逐字段对账后修正）

### 3.1 关键对账：真端客户端包也没有时长字段

- 真端主服 `classes/NPC/Npc.cpp:6268`（`Npc::MoveNpc`）构造的**客户端移动包**逐字段为：
  `[len][头][objectId][x,y,z][heading][mask][终点 x,y,z（mask&0xC0==0xC0）][glide（mask&4）]`，
  25/37 字节，**没有 duration**；与我们的 `SM_MOVE` 同构。
- 时长（`duration = 距离/速度`，下限 100ms，日志串 `[NpcMoveTrace] moveDuration:%u`）只用于
  **服务端自己的调度**：NPC 服务端 `NpcMotionController::SendStartMovePacket` 到期即发下一个包。
- 即：官方客户端同样按「掩码速度走直线、到达即停」，真端的平滑来自
  **「包总在客户端走到之前送达」的节奏**（每步碰撞/地表解算 + 到期即发）。

### 3.2 第一次尝试（v1）为什么失败

2026-10-06 第一批实现 ① 行走态逐 tick 贴地 + ② 贴地短段下发（前视 0.8m、**200ms 节流 + 逐 tick
偏差门控**）→ 实机「卡在原地，然后往前瞬移」：门控/节流让实际包间隔抖动到 300–500ms，客户端
先走完 0.8m 停下，服务端的下一个包起点已在前方 ⇒ 停-跳。**错在节奏，不在短段本身。**

### 3.3 第二、三次尝试（v2 实机残留 → v3 待实机复测）

v2（按对账修正节奏）：前视 = `max(1m, 速度×0.3s)`、每移动 tick 补发、按段粘滞（地表-弦线偏差
>5cm 的段才流式）、与 ① 成对保留、收尾段回落正常到点广播。**实机残留**：「脚部入地然后拉起来」——
见 `LOG-ANALYSIS-799737.zh-CN.md`：前视 1m 而客户端每 tick 只走 0.1–0.2m ⇒ 每 tick 只爬弦线行程的
1/5–1/10，台阶段竖向滞后于地面，下一包吸附回地面真值即「拉起」；换腿后门控延迟 6–7 tick 还要
先走 8–9m 长弦再被一次性拉起。**结论：v2 的「≥3 个下发周期」方向错了——真端节奏等于「包 = 刚走完
的一步」（时长=距离/速度 ⇒ 段长 = 一个周期的行程），不是「远大于一个周期」。**

v3（对齐真端节奏）：前视 = **本 tick 实测位移 × 1.3**（下限 0.15m）——略大于一个周期行程，
客户端不会先到点（防 v1 停-跳），弦线又足够短（≈0.25m）贴住台阶；**取消地形门控**，行走态每个
移动 tick 补发、换腿首 tick 即发短段（不再下整段长弦）；系数 1.3 为旋钮（停→1.5，入地→1.15）。
契约已并入 **AIM-011**；`ai2 log` 可见 `walkGroundStream from=… to=…`。

### 3.4 其余留档（未采用）

`-path.dat` 全套寻路机器与分段预计算 Path；远目标重锚（同目标、周期刷新起点）；
台阶段路线点子航点加密——风险/收益均未实机确认。
