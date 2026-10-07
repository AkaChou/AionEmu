# P7 步 2 步 e 前置：接取 6 类 kind 执行面 + 通用附加动作执行器（真端原码坐实）

> 日期：2026-10-02。分支：`quest`。性质：只读真端原码考古（步 e 实现前置）。
> 证据根：`<真端根>/server58/MainServer_ScriptDLL64/ScriptDLL64.c`。

---

## 1. 接取侧注册面（`LoadBasicInfo` 尾段，C:2075690-2075813）

| acquire kind | 注册 | 执行函数 | 触发 |
|---|---|---|---|
| 4 = Talk | **恒**建第二个对象 + 接取表项 `kind=4, value=-1`，槽 `+0x238 = FUN_180c473e0` | 「已接取/进行中」对话面（见 §3） | NPC 对话 |
| 4 = Talk | 另建 0x640 对象 + 接取表项 `kind=0/questid`，槽 `+0x1D8 = FUN_180c47220` | 「可接取」对话面（见 §3） | NPC 对话 |
| （任意，步向量空时） | 追加接取表项 `kind=3, value=0` + 槽 `+0x1E0 = FUN_180c474b0` | 共享对话平面（d2 已接） | NPC 对话 |
| 3 = ItemPlay | `FUN_180c4dfd0(mgr, 5, npc标识, questid, FUN_180c46e90)` | **双角色**（见 §2） | 物品获得事件 5 |
| 7 = EnterWorld | `FUN_180c4d890(mgr, 0x12, …, FUN_180c467b0)` | **双角色**（见 §2） | 进世界事件 0x12 |
| 8 = LevelUp | `FUN_180c4dea0(vec0, questid, FUN_180c46bb0)` | 等级遍历（见 §2） | 升级（Server64 走 vec0） |
| 10 = LevelUpLogIn | `FUN_180c4dea0(vec3, questid, FUN_180c46c90)` | 等级遍历（见 §2） | 登入/升级（Server64 走 vec3） |
| 6 = EnterArea | 进区 handler `FUN_180c47bf0` 注册于区事件 | **双角色**（见 §2） | 进区 |
| 无（none 120 行） | 无接取注册 | — | 系统发放/不可接取 |

## 2. 双角色接取（进度 handler 的 state!=3 分支）

同一 progress handler 兼任接取：`状态 != 3` 且 `def+4 == 事件 kind` 且载荷匹配（npc/区/世界/等级）→
`(+0xd8)(user, questid, 0, 0)` = **SetQuestAcquired（接取）** → 随后 `FUN_180c4cd50(def+0x10, …)`
（**接取时也执行附加动作**）。逐 handler：

- CollectItem `FUN_180c46e90`：`def+4==3` 且 data+8（npc id）匹配。
- EnterWorld `FUN_180c467b0`：`def+4==7` 且 zone 匹配。
- EnterArea `FUN_180c47bf0`：`def+4==6` 且同名区匹配。
- LevelUp `FUN_180c46bb0`（C:2070709）/ LevelUpLogIn `FUN_180c46c90`：`def+4 ∈ {8,10}` 且
  `ctx+8 == def+8`（**所需等级 = acquire 载荷**）。
- ItemPlay 接取（acquire==3）：走对话平面（0x1E0 槽）？——**不是**：注册 event 5 用同一
  `FUN_180c46e90`（与上同行）。

> **勘误（2026-10-06，13403 实机 + 反编译复读）**：本节「接取时执行的附加动作 = 进度 handler 的
> `def+0x10`」的容器归属有误——接取分支执行的是 `*(entry+0x10)`（`category_acquire_` 装入的
> `QuestProgressExtraInfo` 对象，即 **value1..10_acquire_ 接取列**），进度步 0 列只在完成步执行。
> 详见 P7-STEPF-PREREQ-ADJUDICATIONS 勘误与 QE-153。

## 3. Talk 接取对话面（词汇与家族车道同源）

**FUN_180c47220（+0x1D8，「可接取」面）**：
- 对话打开（dialog 状态 0 或 10）→ 发页 **0x129a = 4762**（接取入口问询页；= 家族车道
  `acceptEntryPage` 4762，P5C 同源）。
- 动作码：
  - **1002（0x3ea）**：`(+0xd8)(user, questid, 0, 0)` 接取；成功 → 发页 **1003（0x3eb）**；
    随后 `FUN_180c4d5b0(user, -1, user, def, code=-1)` 收尾。
  - **1003（0x3eb）**：发页 **1004（0x3ec）**。
  - **1007（0x3ef）**：`(+0x1a0)(user, questid)`（问询流入口；P5C 同款 mgr+0x1a0）。
  - **20000**：接取；成功 → 发 `+0x5d8` 选择窗页（DAT_18123caa8）；code=-1 收尾。
  - **20001（0x4e21）**：`(+0x2a8)(user, 0x1560e9, 0x1e)`（演出/链接面）→ 发 `+0x5d8` 页。
  - **1008（0x3f0）**：发 `+0x5d8` 页。
  - 其余 ≥1000：回显动作页（+0x188）+ 收尾。
- 0x640 对象接取条件表：`+0x570`=数量，`+0x578+i*6 = {u8 类型, u32 value, u8}`，
  `+0x5F0+i*4` 辅助值（`FUN_180c4df60` C:2075398 写入）——即 con_quest/等级/职业类接取门。

**FUN_180c473e0（+0x238，「进行中」面，kind=4 value=-1）**：
- 对话打开（0/10）→ 发页 **0x2712 = 10002**（进行中页；与共享对话平面空步集页同值）。
- **1008** → 发 `+0x5d8` 页；**1009（0x3f1）** → `(+0x1b0)(user, questid, 0)`（报告通道）。

## 4. 附加动作执行器（三个变体，按 kind 家族分派）

入参 `(动作表, user 上下文, questid[, def/区上下文])`；**虚槽定名见
`p7-prereqs/dd-host-interface-detail.md` §1**（AddItem +0x2f8 / RemoveItem +0x1d0 / Teleport +0x2d0 /
PlayMovie +0x1b8 / CreateMonster +0x358 / Say +0x2b8 / EnterInstance +0x220 / AddQuestTimer +0x250 /
DeleteQuestTimer +0x258）。

| 执行器 | 用者 | case 覆盖 | 差异点 |
|---|---|---|---|
| `FUN_180c4cd50`（C:2074638，共享） | CollectItem / EnterWorld / Talk 双角色 + acquire | 1,2,3,4,5,7,9,10 | **无 case 6/8**；case 4 type 0/2 → 槽 +400(dec)=PlayCutScene 系、type 1/3 → PlayMovie |
| `FUN_180c4c8d0`（C:2074485） | EnterArea / TalkFOBJ | **1..10 全** | **case 6 → def 侧对象 +0x270、case 8 → def 侧 +0x3a0**（DLL 侧对象，非 IUserImp） |
| `FUN_180c4d190`（C:2074785） | Hunt | 1,2,3,4,5,7(…) | **case 5 刷怪走 user +0x180**（非 +0x358）——槽语义未定名 |

**⇒ 动作面证据缺口**：def 侧 +0x270/+0x3a0（delay-6/message-8 的 EnterArea/TalkFOBJ 面）与 Hunt 的
+0x180 刷怪槽未定名；宿主 Say/EnterInstance/AddQuestTimer 的**本服渲染面**（Say=哪种包、EnterInstance
走哪个服务、定时器到点推进哪条边）需逐一坐实后才能实现对应 case。

## 4.1 切换集动作直方图（2026-10-02 复算）

**93 行**携带附加动作，171 动作实例：talk GIVE 78 / REMOVE 20 / SPAWN 11 / DELAY 10 / TIMER 6 /
CUTSCENE 8 / MESSAGE 4 / INSTANCE 3 / TELEPORT 3；talkfobj GIVE 7 / SPAWN 5 / REMOVE 3 / CUTSCENE 2 /
DELAY 2 / TIMER 2 / TELEPORT 1；itemplay GIVE 4 / REMOVE 3 / SPAWN 5 / CUTSCENE 2 / TIMER 2 / MESSAGE 1；
enterarea CUTSCENE 12 / SPAWN 6 / MESSAGE 3 / TIMER 2；hunt CUTSCENE 5 / SPAWN 4；enterworld
GIVE/REMOVE/CUTSCENE 各 1。

## 5. 本服切换集接取直方图（步 1 冻结）与执行面映射

| acquire | 行数 | 本服执行面（步 e 实现） |
|---|---:|---|
| talk | 1142 | 对话接取：开页 4762（`QuestDialogContract.acceptEntryPage` 同口径）→ 1002/20000 接取（接取门走 `NativeQuestStartPort.checkStartConditions` 同轴）→ 1003/确认窗 |
| enterarea | 165 | `onEnterZone` state!=3 分支（同名区已由 NativeEnterAreaPort 解析）+ 接取时附加动作 |
| itemplay | 13 | `onItemAcquired` state!=3 分支 + 接取时附加动作 |
| enterworld | 12 | `onEnterWorld` state!=3 分支 + 接取时附加动作 |
| leveluplogin | 15 | 登入/升级事件（Server64 vec3 对应本服登入路径； LevelUp 纯升级 0 行） |
| none | 120 | 无接取注册（真端同样无面）⇒ 不接接取入口，登记冻结理由 |

## 7. 注册块逐字复核（2026-10-02 深夜补充，C:2075690-2075920）

1. `acquire==4`（Talk）：0x640 对象 #1（name = value0_acquire_）+ 条目 `{type=0, value=-1}` +
   槽 `+0x1d8 = FUN_180c47220`。
2. **所有行恒建** 0x640 对象 #2（name = `reward_npc_name`）+ 条目 `{type=4, value=-1}` +
   槽 `+0x238 = FUN_180c473e0`；步向量空时追加条目 `{type=3, value=0}` + 槽 `+0x1e0 = FUN_180c474b0`
   + `+0x474 = 0`；槽 `+0x198 = FUN_180c46590 | FUN_180c464e0`（按 con_quest/con_quest_list 有无选择，
   语义未坐实）。
3. 事件注册（**仅此四类**）：`acquire==3` → event 5（参数 = value0_acquire_ 的 name-id）；
   `==7` → event 0x12（world id）；`==8` → vec0 遍历；`==10` → vec0 **加** vec3 双遍历。
   **`acquire==6`（EnterArea）在 DLL 注册块中无注册**。
4. **EnterArea 接取走区 handler 双角色**（FUN_180c47bf0 尾段，C:2071405 区）：步进分支之后走
   **独立接取侧区注册树**（`DAT_184720a70`/`DAT_184720b00`，名字哈希 0x1003F 比较），
   `acquire kind==6` → `(+0xd8)` 接取 → `FUN_180c4c8d0(步0动作)`。注册树填充点未逐字定位
   （LoadBasicInfo 的 acquire==6 分支）。

## 8. 切换集接取参数事实（2026-10-02 复算）

- 直方图：Talk 1142 / EnterArea 165 / none 120（**字面 `none` 类别值**）/ LevelUpLogIn 15 /
  ItemPlay 13 / EnterWorld 12。
- **EnterArea 165 行中 145 行 `value0_acquire_` 为空** ⇒ 接取树注册空名哈希 ⇒ 永不命中（镜像 =
  永不接取，同 `none`）；**非空 20 行 / 15 个唯一别名**（`*_QuestArea_*` 族）**全部不在本仓
  zones_retail_enterarea.xml**（该文件只有步 c 的 91 个 progress 侧感官区）——步 c 判定的
  「acquire 15 = 14 解析 + 1 缺席」的解析发生在真端世界文件，**几何未入仓** ⇒ e1 需先补
  QuestArea 区几何导入（步 c 同款流程），否则 20 行无进区面。
- ItemPlay 接取参数 = 物品名符号（`QUEST_16975a`/`doc_quest_13952a` 等 13 个）✓ 可经
  `RetailItemNameIndex` 解析；LevelUpLogIn = 等级整数（30/40/45/50/66）✓；
  EnterWorld = world id ✓；Talk = NPC 名（含 quest_ai_name 组键与模板名）✓。
- con_quest 轴：35 行（多为 `con_quest_list = STR_QUEST_ZONE*` 区串 + 少量数字 con_quest）——
  **语义未坐实**（0x640 条目 type 表未还原）⇒ e1 fail-closed 冻结
  （新桶 `ACQUIRE_CONDITION_UNFACED`）。

## 9. e1 分桶复算（2026-10-02，纯接取/动作面轴，未含 EnterArea 区导入）

按「步 e1 动作面 = GIVE/REMOVE/CUTSCENE(+Movie)，其余 TELEPORT/SPAWN/DELAY/MESSAGE/INSTANCE/TIMER
= `ACTION_UNFACED`；con_quest 非空 = `ACQUIRE_CONDITION_UNFACED`」复算：

- 可路由 **1390** / `ACTION_UNFACED` **33** / `ACQUIRE_CONDITION_UNFACED` **35** / `ZONE_ABSENT` 9
  （合计 1467 ✓）。冻结行样例见复算脚本输出（10033/10034/10035…、10010/10031/10500…）。
- ⚠️ EnterArea 接取的 20 行非空别名行需区几何导入后方可接取（不影响 progress 面，只影响接取）；
  导入前这 20 行接取面缺席（镜像真端 = 玩家不可接取），是否整行冻结待 e1 实现时与
  「接取面缺席 ≠ 进度面缺席」的整行原子语义合议裁定。

## 10. 切片决策（2026-10-02，按本仓分批惯例 P3 步骤 1-6 / P5D 步 1-2 同型）

- **步 e1（先行切片）= 接取面**：talk 对话接取（4762/1002/1003/1004/1007/20000/20001/1008/1009 全词汇）
  + 双角色接取（enterarea/enterworld/itemplay 的 state!=3 分支）+ leveluplogin（登入/升级钩）+ none 不接面。
  接取时的步 0 附加动作：仅实现**已坐实且端口齐备**的 GIVE/REMOVE（NativeInventoryPort）与
  CUTSCENE（NativeMoviePort）；其余类型 → 新冻结桶 `ACTION_UNFACED`（整行冻结，fail-closed，
  不得静默丢动作）。
- **步 e2（后续切片）= 动作面收全**：TELEPORT / MESSAGE(Say) / SPAWN(CreateMonster 双形) / DELAY /
  INSTANCE / TIMER(+成功 DeleteQuestTimer) 逐 case 坐实宿主渲染面后实现，解除 ACTION_UNFACED 冻结。
  证据缺口：def 侧 +0x270/+0x3a0、Hunt +0x180、Say/EnterInstance/AddQuestTimer 本服对应面。
- 门（e1）：`DataDrivenNativeRuntimeGateTest` 扩接取用例 + 新冻结桶冻结断言；分桶数字随
  ACTION_UNFACED 重算并冻结。
