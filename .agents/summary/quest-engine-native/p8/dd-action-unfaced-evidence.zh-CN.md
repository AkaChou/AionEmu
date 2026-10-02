# DD 附加动作 case 9/10 取证与 Timer 落面（2026-10-02，偏差修复第三批；同日第四批补 case 9 立即面/注册表；10-03 第五批 col6 类定名/param_6 单位佐证；10-03 第七批 col6 空桩落面解冻 2 行）

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

## 3. c8d0 步 col6（case 6 Delay）/ col8（case 8 Message8）——col6 = 宿主空桩零效果（第七批落面解冻 2 行）；col8 = `Npc::Die`（零 routed 人口，维持冻结）

- **装载面（第五批误判更正）**：装载器 `FUN_180c49610` case 6 **确实入列**——读
  "DataDrivenQuest - Delay Time" 整数成功即 `FUN_180c51560` 存入动作向量（缺列才静默跳过）；
  case 7/8 = 字符串索引 "Message"（错索引有日志）。
- **接收者类（第七批钉死）**：应用器 `FUN_180c4c8d0` 的 param_3 = **宿主侧（EXE）任务脚本对象**
  ——DLL 每类处理函数存于自身 0x640 记录的字段（+0x1e0/+0x230/+0x258/+0x260/+0x268），被宿主
  以 **EXE 对象为第一参**回调（dlg 处理器 `FUN_180c478e0` 的 +0x50/+0x518/+0x4d8/+0x4c0/+0x500/
  +0x5d8 大偏移虚调 ⇒ 槽位 ≥0x5e0 = ≥188，远超 DLL 自身 `IOneQuestScriptNpc::vftable`
  （@0x123d3b8，RTTI 真空表：析构 + 每步类 {COL, 双函数} 分派结构，Ghidra 命名有误导性））。
  宿主类 = **EXE `Npc`**（`Npc::vftable` @EXE 0x12b8470，199 槽，构造器 `FUN_140b16fe0` lea 提取）：
  **槽 +0x3a0（116）= `FUN_1400b21350` = `Npc::Die`（剖析串交叉验证）** —— 映射可信。
- **col6 Delay = 空桩零效果 ⇒ 落面为忽略**：`Npc::vftable` 槽 +0x270（78）= `FUN_140094480` =
  **`return;` 空桩**（Npc 预留接口槽区）⇒ 真端 Delay = 装载存储、运行时调用空桩、**零效果**。
  镜像 = 忽略该动作（真端一致）⇒ **10035/25606 解冻**（载荷 = Delay 8 / 3 / 2，仅此两行携带），
  分桶 1453/14 → **1455/12**。
- **col8 Message8 = `Npc::Die` ⇒ 维持冻结**：case 8 调宿主槽 +0x3a0 = 宿主 NPC 死亡（真端语义
  存在但未落面）；全表唯一 value8-on-c8d0 载荷行 = 9696（`STR_MSG_LIMIT_SALE_TEST_DESC01`，
  `_TEST_` 串且该行不路由）⇒ routed 人口零行，`defSideAction` 对 col8 维持 fail-closed。
- **方法论更正**：第六批"盘上 DLL 与反编译不同源"系**指纹法错误**（假设槽 0 = 工厂邻区析构）——
  DLL 与反编译**同源**（ghidra.log 实证 IMPORTING 同一文件）；col6 真正的解锁 = 接收者在
  **EXE 侧**，而盘上 `Server64.exe`（2020-06-17）与 Server64.c 反编译**同源**（宽/窄剖析串全中），
  其代码早已在可读的反编译里 ⇒ **无需任何用户资产**。

## 4. 原始载荷全集（60 命中 → 冻结 12）

探针：`data_driven_quest.xml` 的 `progress_info/data` 有序步，`value{6,8,9,10}_progress_` 非空列。
路由集 ∩ 执行矩阵后冻结 = col9 3（20032/10034/20034）+ ~~col6 2~~（第七批解冻：10035/25606，
载荷 Delay 8/3/2）+ ~~col10 9~~（本批解冻）。
复算工具：`p7/tools/dd-planrow-e2-mirror.py`（第七批同步 Delay 空桩忽略逻辑，离线输出
routed 1455 / frozen 12 与 Java 门逐值一致）。

## 5. 门态

DD 门 20/20（新增 `questTimerExpiryMirrorsTheRetailRangeCheck`：范围内直写 / 过目标步与 0 步零操作 /
REWARD 与未路由零操作）；族门 + tablelane 121/121；聚焦套件 1457 例 / 25F+164E / 72 红类对基线
**ADDED 0 / REMOVED 0**（`gates/2026-10-02-focused-run-timer-face.log`；第七批重跑同值）。

第四批 = 纯证据批（零行为变更）：case 9 立即面/注册表两处登记推翻（§2），Java 镜像的
ENTER_INSTANCE 分支仅更新证据注释（仍 `ACTION_UNFACED`），分桶不变 1453/14，离线镜像逐值一致。

第七批（2026-10-03）= 行为变更批：col6 Delay 空桩落面（镜像忽略），10035/25606 解冻，
分桶 **1455/12**（逐类步数重冻：hunt 818/collectitem 345/pvp 207/talk 387/enterarea 137/
itemplay 39/enterworld 31/talkfobj 15；接取 talk 1133）；DD 门 20/20 + presence 门 4/4 绿；
聚焦套件重跑 1457 例 25F+164E 与基线同值。

## 6. param_6 距离闸门——单位佐证升级，生产函数仍未定位 ⇒ 维持不实现

- **case 表坐实（本批通读 `FUN_180c46020` 全体）**：def+0x38 值取 0/1 → `2500.0 < param_6` 拒；
  2 → `10000.0`；5/6 → `40000.0`；其余值直落计数体。case 0/1 另带同图判定
  （`+0x30(user)==param_3` 直过，否则 `+0x98/+0xa0/+0xa8` 旗标链）。
- **单位佐证（升级"未坐实"）**：EXE 侧 `World::KillNpcInRange`（S:926xxx 前奏/1095865 派生）
  与击杀归属代码全程**平方距离**口径（`dpps(x²+y²+z²)` 点积平方和、`<= r*r`、
  `0x2711`=100²+1 门）；线性读法（2.5km/10km/40km 击杀记数界）荒谬 ⇒
  **2500/10000/40000 = 50m/100m/200m 平方距离**为唯一自洽读法。
- **仍缺 = param_6 生产函数（第六批定性收口）**：**ScriptDLL64.c 全库带 float 参数的函数
  仅 `FUN_180c46020` 一个**（签名枚举核过全文件），且其在 DLL 内**零调用点**（处理指针
  `DAT_184720a50` 只写不读）⇒ 调用方在 **EXE 侧**，经 DLL 数据段函数指针调入，且按 x64 ABI
  浮点经 XMM 寄存器传递、**不出现在反编译调用文本**中 ⇒ 纯文本检索不可达（EXE 侧
  `Npc::Die`/`World::KillNpcInRange` 等击杀链已通读，均无该调用形态；NPCSvr64 无死亡任务
  派发串）。定位需**同源二进制（Ghidra 工程/带符号）或调试会话**——与 col6 槽体合并为
  同一件用户侧资产（同源 ScriptDLL64.dll + Server64.exe）。fail-closed ⇒ **维持不实现**；
  开放轴收窄为"同源二进制到位后从 XMM 传参调用点直读算式"。
