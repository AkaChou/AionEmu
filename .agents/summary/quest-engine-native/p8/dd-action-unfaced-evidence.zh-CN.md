# DD 附加动作 case 9/10 取证与 Timer 落面（2026-10-02，偏差修复第三批）

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

## 2. case 9（EnterInstance）——装载/离场两坐实，立即面 EVIDENCE_MISSING ⇒ 维持冻结 3 行

- **装载面（推翻旧登记）**：C:`FUN_180c49610` case 9 载荷 = `creationId, worldId, leaveProgress, [成员名…]`
  （缺项日志三条："Ins Creation ID / Ins World ID / Leave Progress Not Exists"）。
  **worldId 是表内独立列**——计划原登记"creationId→世界映射在客户端表"不成立（当时只读了首 token）。
  实测载荷：`12, 300190000, 7, QUEST_9693A…`（9693/10032/10037/20032/20037）、`13, 300160000, 7`（10034）、
  `3, 300150000, 5`（20034/20038）。
- **离场检查面**：`FUN_180c46d80` 第一块 = def+0x40（ctx）/ +0x44（leaveProgress）轴，同 Timer 形。
- **立即执行面**：+0x40/+0x44 全文件**仅装载器与离场检查两个读者**——进实例的触发面（谁在何时把玩家
  送进 worldId）在 ScriptDLL64 无读者 ⇒ EVIDENCE_MISSING，fail-closed 维持冻结（10032 非路由、
  20032/10034/20034 冻结），**禁止按"离场轴已坐实"半解冻**。

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
