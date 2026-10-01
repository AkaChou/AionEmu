# P7 步 f 前置坐实：五项前置逐项裁定（真端宿主源逐函数取证）

> 日期：2026-10-02。分支：`quest`。性质：只读真端原码/本服包面考古（步 f 切换前置）。
> 证据根：`<真端根>/server58/MainServer_ScriptDLL64/ScriptDLL64.c`（下称 C:）、
> `<真端根>/server58-source/NPCServer_NPCSvr64/`（NP）、AionEmu 本服包面源码。
> 结论状态：五项全部裁定闭合；残留 EVIDENCE_MISSING 两处（param_6 坐标算式、Spawn 到期回收者）均按
> fail-closed 登记、不猜不实现。

---

## 1. F1 距离闸门 case-0 语义（§10.3-#24②）

共享前奏两份，均为**击杀类进度 handler 本体**（由宿主经函数指针表回调）：

| 变体 | 注册类别 | 槽 | 证据 |
|---|---|---|---|
| `FUN_180c46020` | 2（Hunt） | `DAT_184720a50`（C:2076036） | C:2069982，闸门段 C:2070033-2070074 |
| `FUN_180c46980` | 5（PvP） | `DAT_184720a08`（C:2076075） | C:2070487，闸门段 C:2070535-2070566 |

按行字段 +112（`piVarX[0x1c]`，DD 行恒 0）分派：

- 变体 A（Hunt）：**0/1 → 距离 ≤ 2500 且（同图 ∨ (+0x98==0 ∧ +0xa0==0 ∧ +0xa8==0)）**；2 → ≤10000 且
  （同图 ∨ (+0xa0==0 ∧ +0xa8==0)）；5 → ≤40000 且（同图 ∨ +0xa8==0）；6 → 仅 ≤40000；
  3/4/≥7/负值 → 无闸门。拒绝 = 推进前裸 `return`（静默）。
- 变体 B（PvP）：0/1 → ≤2500 且（同图 ∨ (+0x98==0 ∧ +0xa0==0)）；2 → ≤10000 且（同图 ∨ +0xa0==0）；
  5/6 → 仅 ≤40000；其余不设防。**B 不调 +0xa8**。
- 距离 = `2500.0 < param_6` 浮点**直比**（非平方、非角度）；param_6 由宿主算好传入，
  **坐标算式（2D/3D、锚点）宿主侧不可见 = EVIDENCE_MISSING**。

**裁定（步 f）**：case 表全部坐实，但 param_6 坐标算式缺证 ⇒ 按 §10.3-#24②「不猜、不近似」纪律
**不实现距离闸门**；本服击杀事件恒同图（变体 A/B 的同图短路恒命中，+0x98/+0xa0/+0xa8 分支不可达），
唯一未镜像差异 = 2500 距离界。§10.3-#24② 收窄为「param_6 坐标算式」单轴，保持开放。

## 2. F2 Talk 步报告通道动作面（e2 执行矩阵修正）

**结论：存在执行面，e2 矩阵对 Talk 的判定错误，步 f 修正。**

- 对话平面 `FUN_180c474b0` 的推进/完成分支（10000+N / 1009 / 10255）统一汇入
  `FUN_180c4d5b0(user, 旧步号, controller, mgr, -1)`；其 -1 路径在 **C:2075066 直调执行器
  `FUN_180c4c8d0(lVar4+0x28)`**，门 = **步表[param_2] 首 int == 4**（C:2074980 附近：完成步 kind 必须
  Talk）。
- **执行步号 = 完成步（旧步）**，非进入步——d5b0 收到的 param_2 是推进前的当前步（对话平面 1009 分支
  先 `+0x100(step+1)` 再以旧值调 d5b0；10000+N 分支同理）。
- **Talk 对话接取（1002/20000）同样执行步 0 动作**：接取收尾 = `FUN_180c4d5b0(user, -1, …, -1)`，其
  param_2<0 路径的门 = 行 def+0x08 == 4（接取 kind Talk），执行 `c8d0(步0动作)`。e1 曾误判
  「收尾 helper 不执行动作」。
- **CollectItem（kind 1）推进零动作**：拾取分支 `FUN_180c47790` 无执行器调用；-1 路径的 kind==4 门
  也把它排除。
- 五个推进边执行点全部传**完成步**动作表（步号镜像无 off-by-one）：
  Hunt `FUN_180c4d190(lVar1+0x20)`（C:2070138）、TalkFOBJ `c8d0(lVar2+0x50)`（C:2071395，lVar2 = 旧步
  载荷）、EnterArea `c8d0(lVar2+0x28)`（C:2071475，同前）+ 接取 `c8d0(步0)`（C:2071502）、
  Talk `c8d0(完成步)`（C:2075066）+ 接取 `c8d0(步0)`（param_2<0 路径）。
- **遗留偏差（登记）**：对话平面 ≥1000 回显分支（d5b0 的 param_5 ≥ 0 路径）会按「节点 type==4 且
  id==页动作」重放步内过场（控制器 +0x190/+0x1b8）——节点 id 绑定布局未逐字坐实 ⇒ 步 f 不镜像，
  登记为 Talk 步演出回显偏差。

## 3. F3 Spawn 可行走校验（NP `IUserImp_Spawn` @1401877b0 全文）

- **可行走化只在半径参数 ≥1 时发生**：`World_RandomWalkableSpawnLocation(世界, 出参, 玩家, 中心,
  param_7, 4, 1)`（NP classes/World/World.cpp:17533）——中心 ±半径**正方形均匀采样**，最多 0x20 次，
  逐次 = 可行走格子（`FUN_1402f2600` 位图查表）+ 世界 AABB + **高差容差 4** + 路径可达
  （`FUN_1402257e0`）+（可选）水面校验；**全败回退中心坐标**（World.cpp:17671-17674）。
- **Absolute（mode 2）且半径 0 = 坐标原样不校正**；count = 第 4 参 do-while 逐只生成（采样逐只独立）。
- time = 第 10 参透传 maker 与生成描述子；**函数内无定时器注册，到期回收者 EVIDENCE_MISSING**
  （本服 `ThreadPoolManager` 定时 `delete()` 维持登记偏差）。
- 非类型 2 世界走 `World__SimpleRandomWalkableSpawnLocation`、飞行玩家走
  `World__RandomFlyingSpawnLocation`（World.cpp:17684-17695）——本服镜像覆盖地面主路径，飞行/水面
  两支 = 登记偏差。
- **落面**：`NativeSpawnPort.Live.sampleWalkable` = `GeoMap.getZ`（中心 ±100 高度窗投射，NaN = 不可
  站立）+ 高差容差 4 + `canPassWalker` 路径检查 ×32 次采样，全败/无地理数据回退中心。

## 4. F4 Say 通道裁定

- 真端 `IUserImp::Say` = NC_SAY_CODE 包，文本 = **字符串表 id**（说话者 = 玩家）。
- 本服 `SM_MESSAGE` 只携带**原始文本**（String），无 id 通道；本服无服务端字符串文本表
  （仅 `retail-quest-string-ids.tsv` 的键→id 索引）⇒ 忠实 say 气泡不可实现。
- **裁定**：维持 `NativeSayPort.live() = SM_SYSTEM_MESSAGE(id)`（客户端按 id 渲染同文本）；
  通道差异（系统消息 vs say 气泡）= **永久登记偏差**，列入客户端验收清单。

## 5. F5 EnterArea 接取 QuestArea 区几何导入

- 探针（`p7/tools/enterarea-retail-zone-probe.py`）本就双轴裁定：acquire 轴 15 别名 = **14 OK**
  （R1 同名 9 + R3 任务脚本区绑定 5）+ **1 真端无区定义**（`DF6_QuestArea_Q25674`，R4 fail-closed）。
- **落面**：探针 `emit_zone_file` 扩为双轴 → `zones_retail_enterarea.xml` 91 区 → **105 区**
  （+14 接取侧）；运行时 `AcquirePlan` 增 kind 6（zoneAlias）+ `acquireZonesByName` 兴趣面 +
  `onEnterZone` 接取双角色（真端区 handler 尾段独立接取侧名字哈希树：接取 kind==6 且区名命中 →
  `(+0xd8)` 接取 + `c8d0(步0动作)`）。
- 真端无区定义的别名：真端注册名字哈希但该区不存在 ⇒ 永不命中（真端自身死边）⇒ 镜像 = 兴趣键登记
  原文、进区事件永不派发 = 同语义，不冻结（与 145 行空别名行一致）。
- 分桶影响：接取直方图 none 278 → **258 + enterarea 20**（镜像权威值，`dd-planrow-e2-mirror.py`
  步 f 版）；可路由 1444 / 冻结 23 不变。
