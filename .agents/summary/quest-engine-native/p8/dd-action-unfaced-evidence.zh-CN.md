# DD 附加动作 case 9/10 取证与 Timer 落面（2026-10-02，偏差修复第三批；同日第四批补 case 9 立即面/注册表）

证据根：`<真端根>/server58/MainServer_ScriptDLL64/ScriptDLL64.c`（C:）、`MainServer_Server64/Server64.c`（S:）、
`NPCServer_NPCSvr64/NPCSvr64.c`（N:）、DD 表 `data_driven_quest.xml` 原始载荷探针。

## 1. case 10（Timer/AddTimer）——全链坐实 ⇒ 9 行解冻

| 面 | 证据 |
|---|---|
| 装载 | C:`FUN_180c49610` case 10：载荷 = `时间, 目标步, 旗标` 三整数（缺项日志 `"Add Timer - Timer Time / Dest Progress is Not Exists"`）；步号上下文入 def+0x68、目标步入 def+0x6c |
| 武装 | S:799910 `"IUserImp::AddQuestTimer"`（profiler 壳）→ `FUN_1400e4af0` = `ServerToNPCServer.inl` opcode 0x6c/0xad/0xff93 **转发 NPCServer 计时**（9-int 载荷）；N:275070 同名对端 |
| 到期分发 | C: 宿主按 quest id 回调注册表（`FUN_180c4dea0(&DAT_184720528, questId, FUN_180c46d80)`，注册条件 = def 轴存在 `!= -2`）→ **`FUN_180c46d80`** |
| 到期判定 | `FUN_180c46d80`：状态字 = 3（进行中）∧ `ctx < 当前步 < 目标步`（ctx 符号选支）⇒ `+0xf0`（SetQuestProgress 直写步号）或 `+0x160`（弃任） |
| 单位 | 载荷数值域 110~14100（`300,7,0`/`1800,7,0`/`2700,2,0`/`14100,7,0`）= 秒级语义；N: `AddNpcQuestTimer` 以 1 tick 形态注册（`local_34 = 1`） |
| 旗标 | 第三整数 ∈ {0,1}：0 = 到期推进（ctx=0 → `0 < cur < dest`）、1 = 到期弃任（`120,7,1` = 120 秒内未达步 7 则弃任）；与 `FUN_180c46d80` 的 ctx 符号选支互证 |

解冻 9 行：10506/13945/15613/15680/20504/20506/23945/25601/25605。
镜像落面：`NativeTimerPort`（线程池延时）+ `DataDrivenNativeRuntime.onQuestTimerExpired`（到期判定，可单测）
+ `DataDrivenProgress.jumpTo`（直写步号，组槽保持）+ 弃任走 `QuestService.abandonQuest`（QE-119 同口）。

## 2. case 9（EnterInstance）——三面全坐实，唯落点位置面数据不可达 ⇒ 维持冻结 3 行

- **装载面（推翻旧登记）**：C:`FUN_180c49610` case 9 载荷 = `creationId, worldId, leaveProgress, [成员名…]`
  （缺项日志三条："Ins Creation ID / Ins World ID / Leave Progress Not Exists"）。
  **worldId 是表内独立列**——计划原登记"creationId→世界映射在客户端表"不成立（当时只读了首 token）。
  实测载荷：`12, 300190000, 7, QUEST_9693A…`（9693/10032/10037/20037）、`2, 300190000, 7, QUEST_20032A, QUEST_20032B`（20032）、
  `13, 300160000, 7`（10034）、`3, 300150000, 5`（20034/20038）。
- **离场检查面**：`FUN_180c46d80` 第一块 = def+0x40（ctx）/ +0x44（leaveProgress）轴，同 Timer 形。
- **立即执行面（第四批推翻"无读者"登记）**：完成步应用器 S:`FUN_180c4c8d0` case 9 =
  `(**(code **)(*param_2 + 0x220))(param_2, first_int)`——**单参 creationId 虚调 `User::EnterInstance`**；
  同函数 case 10 = `+0x250(first_int * 1000)`（Timer 毫秒单位硬证据，§1"秒级语义推断"升级为坐实）；
  case 6/8 = `param_3`（任务对象 IOneQuestScriptNpc）+0x270/+0x3a0 虚槽（§3 同口径）。
- **注册表（第四批推翻"宿主内部不可达"假设）**：`User::EnterInstance`（S:926244，`FUN_1405a4880`）
  → `FUN_1406093b0` = `User::CheckAndAskPrivateInstance`（S:988239）→ `FUN_1406dae50` 按 insCreateId
  查实例记录 → `User::_EnterInstance`（S:926330，`FUN_1405a4aa0`：冷却/savetype ∈ {0x1e,0x3c,0x5b} 闸门
  /组队/复入/luna 计价通用实例引擎；"cannot find insCreateId(%d)" 三处日志）。
  **insCreateId 的静态注册表 = `<真端根>/Map/XML/instance_creation.xml`（377 行）**：
  引用的 creationId 全在——2 = `IDELIM_PRIVATE_D`/IDElim/INSTANCE_PRIVATE、3 = `IDTEMPLE_UP_INSTANT`/
  IDTemple_Up/INSTANCE_INSTANT、13 = `IDTEMPLE_LOW_INSTANT`/IDTemple_Low/INSTANCE_INSTANT；
  与载荷 worldId 列互证一致（3→300150000、13→300160000、2/12→IDElim ↔ 20032 的 EnterWorld 步 300190000）。
- **仍缺 = 落点位置面（数据级）**：注册表行自带 `start_point_alias_01/02`（2→`IDElim_Entrance_alias`、
  3→`IDTemple_SecretRoom_alias`、13→`IDTemple_Low_Ent01`）+ `resurrect_point_alias`；这些别名串在
  **真端全根（含反编译源）/ 本仓 / 客户端解包根** 四路检索均仅注册表自引 ⇒ 别名→坐标解析数据不可达
  （与 ZONE_ABSENT 的 LF6 world 资产同类）。fail-closed ⇒ **维持冻结 3 行（20032/10034/20034）**，
  冻结事由由"触发面 EVIDENCE_MISSING"升级为"落点位置面 EVIDENCE_MISSING（start_point_alias 解析资产）"，
  **禁止按"三面已坐实"半解冻**；用户侧资产到位后即可按本节全链落面。

## 3. c8d0 步 col6（case 6 Delay）——维持冻结 2 行（10035/25606）

case 6 = `FUN_180c49610` 早退分支（`FUN_181079cf0(param_5,…,L"DataDrivenQuest - Delay Time")`，单整数）；
对话平面 `FUN_180c474b0` 内 case 0..0xe 的分发表（步幅 0x155 的脚本字节码偏移 + `+0x188` 虚调）指向
**任务脚本字节码解释层**，def+0x270 槽（延迟态存哪、到期谁唤醒）仍未定名 ⇒ 维持 ACTION_UNFACED。

## 4. 原始载荷全集（60 命中 → 冻结 14）

探针：`data_driven_quest.xml` 的 `progress_info/data` 有序步，`value{6,8,9,10}_progress_` 非空列。
路由集 ∩ 执行矩阵后冻结 = col9 3（20032/10034/20034）+ col6 2（10035/25606）+ ~~col10 9~~（本批解冻）。
复算工具：`p7/tools/dd-planrow-e2-mirror.py`（本批同步 Timer 落面逻辑，离线输出 routed 1453 / frozen 14
与 Java 门逐值一致）。

## 5. 门态

DD 门 20/20（新增 `questTimerExpiryMirrorsTheRetailRangeCheck`：范围内直写 / 过目标步与 0 步零操作 /
REWARD 与未路由零操作）；族门 + tablelane 121/121；聚焦套件 1457 例 / 72 红类对基线 **ADDED 0 / REMOVED 0**
（`gates/2026-10-02-focused-run-timer-face.log`）。

第四批 = 纯证据批（零行为变更）：case 9 立即面/注册表两处登记推翻（§2），Java 镜像的
ENTER_INSTANCE 分支仅更新证据注释（仍 `ACTION_UNFACED`），分桶不变 1453/14，离线镜像逐值一致。
