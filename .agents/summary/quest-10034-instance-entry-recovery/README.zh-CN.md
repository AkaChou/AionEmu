# 10034 进副本空实例 + 掉线不可重进：col9 实例语义与离场恢复（2026-10-08）

> 主题：任务 10034（DD 车道，ELYOS，英吉斯温链；副本 300160000 Lower Udas Temple『地下神殿的秘密空间』，
> 目标区 `HIDDEN_LIBRARY_300160000`）实机两报障：①进副本"什么东西也没有"（看不到 hidden switch 700604
> 与图书馆 NPC）；②副本内掉线后无法再次进入、任务步卡在 4/5/6。根因 = DD 车道 col9 迁移时丢了**实例分配/
> 注册语义**（裸传送落到默认空实例）+ **未实现离场恢复**（case 9 载荷 `leaveProgress` 装载即弃）。

## 1. 现象与根因

### 根因 A：进入 300160000 被送进"默认空实例"（副本无任何 spawn）

- 链路：`DataDrivenNativeRuntime.runActionList` 的 `case ENTER_INSTANCE` 只调
  `NativeTeleportPort.Live.teleport` → `TeleportService2.teleportTo(player, worldId, x, y, z, h)`
  （5 参重载，`TeleportService2.java:586-591`）——**跨世界时固定 `instanceId = 1`**。
- `WorldMap` 构造时为每个世界（含副本 300160000）创建默认实例 `instanceId=1`，而
  `WorldMapInstanceFactory.createWorldMapInstance` **不含 spawn**；`SpawnEngine.spawnAll()` 显式跳过
  实例世界（`if (!worldMapTemplate.isInstance())`），副本 spawn 只在
  `InstanceService.getNextAvailableInstance` → `SpawnEngine.spawnInstance(...)` 生成。
- ⇒ 玩家落在 **300160000 的默认空实例**（只有地形，没有 NPC/物件）。GM 的 `//findnpc` 传送走
  `TeleportService2.teleportToNpc(player, SpawnSearchResult)`（`TeleportService2.java:905-925`）：
  对实例世界会 `getNextAvailableInstance`（新建 + 完整 spawn）+ `registerPlayerWithInstance` +
  带 instanceId 传送 ⇒ 实机现象"GM 传过去才有 NPC 和开关"；两侧地图 ID 都是 300160000（**实例 ID 不同**）。
- 对照撤回：退役 typed XML（`git 9321e7663^:.../quest_definition/quests/10034.xml`）与旧 handler
  （`_10034Found_Underground`）都用 `teleport-player-next-available-instance` /
  `getNextAvailableInstance + registerPlayerWithInstance + teleportTo(instanceId)`——DD 迁移时丢失。

### 根因 B：掉线后步卡 4/5/6、不可重进（未实现离场恢复）

- 玩家进的是默认实例且**从未注册**；掉线后 `InstanceService.onPlayerLogin`（`:412-441`）的
  `getRegisteredInstance` 命中不到 → `moveToExitPoint` 送回主世界。
- 任务步停在 4/5/6；DD 车道**未实现 case 9 的离场检查**（解析 `leaveProgress` 后丢弃，
  `DataDrivenNativeRuntime.java` ENTER_INSTANCE 分支原注释"不实现"）→ 与 730295（英吉斯温
  344.93 1374.55 的 drakan stone statue）的 Talk 步只服务步 3 ⇒ 任务卡死。
- 语义依据（三源一致）：
  - 真端 DD 载荷 `value9_progress_ = 13, 300160000, 7`（creationId, worldId, **leaveProgress**）；
    20034 = `3, 300150000, 5`。
  - 真端反汇编（第九批）：`锚 < 步 < leaveProgress ⇒ +0xf0 写回锚`，锚 = 动作所在步（本服取进入步）。
  - 退役 XML：s4/s5/s6 → s3 + 补发 182215627（`Jagged Sword`）、s7 不回滚 —— 与 `3 < 步 < 7` 完全一致。
  - timer 列佐证：10034 `1800, 7, 0` / 20034 `1800, 5, 0`，目标步与 `leaveProgress` 同值（= 副本内流程结束点）。
- 第十批曾终裁"离场检查面真端拓扑不可达 ⇒ 不实现"——本次为**行为修复**口径主动落面（Playbook 模式
  `UNREACHABLE_INSTANCE_REENTRY_RECOVERY`，代表 14047 / commit `8b058d4b4`：`ENTER_WORLD` 回到最近可重入
  阶段、不在 `LOG_OUT` 回退、回退目标是"普通世界中可重建整段路径的节点"）。

## 2. 实现面

| 文件 | 变更 |
|---|---|
| `tablelane/NativeTeleportPort.java` | 新增 `enterInstance(...)`（case 9 唯一出口）：共享装配 `assembleInstanceEntry`（注入面 = `InstanceSource`（复用已注册/分配/注册）+ `InstanceTeleport`（带 instanceId 传送）；复用命中零分配零注册、未命中分配+注册）——`Live` 实现：非副本世界/模板缺失退回裸传送（照 `TeleportService2.teleportToNpc` 判定口径 + null 防御），否则走共享装配；z+1、度→6位与 case 3 同形 |
| `tablelane/DataDrivenNativeRuntime.java` | ①`scanFacedActions` 的 ENTER_INSTANCE 分支把 `leaveProgress` 存 `count` 槽（槽位复用先例 = SPAWN movieId / TIMER itemId+count+movieId）；②`create()` 经 `collectInstanceLeaveRollbacks` 收集 `List<LeaveRollback>`（questId, enterStep=动作步, worldId, leaveProgress, 进入步 REMOVE_ITEMS 物品；只收 routed 行）+ 视图 `instanceLeaveRollbacks()`；③`runActionList` ENTER_INSTANCE → `enterInstance`；④`onEnterWorld` 前置 `recoverUnreachableInstanceSteps`：`worldId != 载荷世界` ∧ START ∧ `guardClear` ∧ `enterStep < 步 < leaveProgress` ⇒ `DataDrivenProgress.jumpTo(vars, enterStep)` + `UPDATE_REQUIRED` + `SM_QUEST_ACTION` + 补发物品（仅 `inventoryPort.count==0` 时 give；判空）——写法与 `onQuestTimerExpired` 同款（纯进度写、不跑步执行器）；⑤返回值不短路（先算恢复、再 acquire/dispatch 合并）；⑥注释加固：世界条件是载重判断（进副本的 CM_LEVEL_READY 也触发，无条件回滚 = 硬性软锁）、补发是 count-zero 去重栅栏、单人注册口径、同行多 case9 幂等、jumpTo 保组槽的等价论证 |
| `DataDrivenNativeRuntimeGateTest.java` | `RecordingTeleports` 加 `enterInstance` 记录；新增 ⑧c `instanceEntryAdvancesThroughTheNextAvailableInstancePort`（10034 进入步推进 ⇒ `enterInstance:300160000`、consumes 182215627、不得 `teleport:300160000`）、⑧d `unreachableInstanceStepsRecoverOnEnterWorld`（收集面 2 行逐项对拍；步 4/5 非副本世界 ⇒ 写回 3 + 补发 + update 包 + 零渲染；幂等；副本世界内/步 3/步 7/REWARD 零动作；20034 步 3/4 → 2 且步 5 越窗零动作——typed s5 漂移不镜像）、⑧e `recoveryDoesNotShortCircuitTheEnterWorldAdvance`（同事件：回滚与 ENTER_WORLD 直接步进并存）、⑧f `emptyRoutingViewHasNoRecoveryFace`（空路由早退分支零 NPE） |
| `NativeTalkFixture.java` | `QuestActionView` 扩为四元（+ step），进度写回断言可读包内步号（既有唯一调用点同步） |
| `NativeTeleportPortTest.java`（新） | 装配面直测：复用命中（零分配零注册 + 传送带注册实例 id）、未命中（allocate + register + 传送带新实例 id）；z 抬 1.0、heading 度→6 位逐值对拍 |
| `p7/tools/dd-planrow-e2-mirror.py` | 注释同步：离场检查面第十批"拓扑不可达"终裁 + 本批"行为修复"主动落面说明（离线镜像未镜像该面 = 预期差异） |

- 通用性（非硬编码）：机制挂在 **case 9 动作**上，生产覆盖 10034/20034 两行（20032 冻结不路由，
  无任何面）；测试钉 `rollbacks.size() == 2` 冻结当前影响面。
- 影响面：其余 1455 条 DD 行、typed 车道、`NativeInstanceEntryPort` 落点表（别名坐标不变）均不动；
  routed/frozen 计数与逐类步数冻结不变。

## 3. 门禁与验证状态

- **静态**：IDE 检查（`DataDrivenNativeRuntime` / `NativeTeleportPort` / `NativeTeleportPortTest` /
  门禁测试 / `NativeTalkFixture`）0 error。
- **单测（2026-10-08 已授权运行，IDEA MCP，全绿）**：`DataDrivenNativeRuntimeGateTest` 50/50（含新增
  ⑧c/⑧d/⑧e/⑧f 与既有 ⑧b/冻结面/routed 1457 回归）、`NativeTeleportPortTest` 2/2、
  `QuestProductionStartupGateTest` 2/2、`RetailOwnershipGateTest` 5/5、`RetailTableSchemaGateTest` 2/2、
  `SimpleTalkNativeFamilyGateTest` 17/17、`SimpleHuntNativeFamilyGateTest` 6/6 —— **合计 84 项，0 失败 0 错误**。
- **实机 PENDING（服务端需重启）**：
  1. ELYOS 做 10034 至步 3，与 730295 对话进副本 ⇒ 应能看到 hidden switch（700604）与 HIDDEN_LIBRARY 区 NPC
     （不再空图）；
  2. 副本内掉线/被弹出后重登 ⇒ 步回到 3 并补回 `Jagged Sword`（182215627），可再次进入；
  3. 副本内正常下线重登（实例 10 分钟销毁窗内）⇒ 不应回滚（仍在 4/5/6，注册复用回副本）。

## 4. 残留 / 风险

1. `NativeTeleportPort.Live` 的世界判定/装配委派不在单测内（装配逻辑本身已由 `NativeTeleportPortTest`
   直测）；非副本回退与实例分配端到端由实机走查验收。
2. 组队场景沿用 typed 车道同款单人注册口径（不做 `registerGroupWithInstance`/teamId 查找；注释已登记）。
3. 与第十批"拓扑不可达 ⇒ 不实现"的考据为**主动偏离**（行为修复），解析处注释与离线镜像工具注释均已记录。
4. 20034 退役 typed XML 的 `s5 → s2` / `QUEST_FAILED` / `var1=0` 三条属 typed 漂移，不镜像
   （DD 载荷窗口 = {3,4}；⑧d 已以"步 5 越窗零动作"钉死）。

## 5. memory-bank / Playbook 收口（待实机验收后）

- 验收通过后：评估更新 `QE-163`（DD 车道接取/执行面）或新建卡记录"col9 实例语义 + 离场恢复"通用结论；
  按 Automatic Wrap-up Protocol 跑 `sync_memory_bank.py` + `verify_memory_bank.py`。
- Playbook：本修复属 `UNREACHABLE_INSTANCE_REENTRY_RECOVERY` 已覆盖模式（14047 代表），如无新契约差异
  则不新增案例（按规则 13 在验收后复核）。
