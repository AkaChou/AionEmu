# P0a §4.2 语义矩阵：相机 0xf0/0x100 双通道副作用（真端全链路）

> P0a 只读审计产物 · 2026-10-01。结论与 §4.4.2 相机参数矩阵（camera-params.tsv，2463 调用点、flag 全 1）互证。
> 快照缺失项见文末 §5（vftable 数据本体、ctx 类、DB 侧 SQL）——已按"只认证据"口径标注推断与置信度。

都已追踪完毕。这是结构化报告。

---

# 家族任务进度相机（FUN_180cb13b0 / FUN_180cb14e0）全链路审计报告

## 0. 架构前提（有证据）

- ScriptDLL64.dll 不是独立进程逻辑：**NPCSvr64 加载它** —— `/Users/mc/IdeaProjects/58Server/server58-source/NPCServer_NPCSvr64/fun/fun_052.cpp:2750` `_snprintf(...,"%s%s",&DAT_1417818f0,"ScriptDLL64.dll"); LoadLibraryA(...)`，失败报 `"can not load script dll %s"`；`:2775` `"Incorrect Script.Dll, can not get data-driven quest data"`。
- 接口获取：`NPCServer_NPCSvr64/classes/NPC/NpcScriptMgr.cpp:1000-1012` `GetProcAddress(DAT_1503bfd48,"GetInterface")`，接口版本校验 `0x2a`。
- 数据驱动任务注册：`fun_052.cpp:3133` 加载 `%s\xml\data_driven_quest.xml`，经 DLL 接口 vtable `+0x38` 注册（`fun_052.cpp:3140`）。DLL 侧把每个 handler 写入自身全局注册表 `*(ctx + 0xa8 + idx*8) = fn`（`MainServer_ScriptDLL64/fun/fun_731.cpp:6214`，函数 `FUN_180cb2ac0`）。
- 快照目录名 "MainServer_ScriptDLL64" 只是归档命名；任务脚本实际宿主是 NPCSvr64（MainServer_Server64 自己的 `IUserImp` 没有任务接口方法，见 `/Users/mc/IdeaProjects/58Server/server58-source/MainServer_Server64/include/Account/IUserImp.h`，全文无 GetQuestState/SetQuestProgress）。

## 1. fun_731.cpp:5250-5450 精读：调用对象与虚表槽位

### 1.1 代码事实（fun_731.cpp:5306-5352 与 5356-5402，两函数仅 3 处不同）

```c
plVar1 = (int64_t *)(*(code *)**(uint64_t **)(param_2 + 0x18))(param_2 + 0x18);   // ctx+0x18 对象的 vtable[0] → 返回 plVar1
(**(code **)(*plVar1 + 0xd0))(plVar1,&local_res10,param_1);                        // GetQuestState(out{status,vars,branch}, questId)
if (((local_res11 < 0x40000000) && (local_res10 == '\x03')) &&
    (bVar2 = (char)param_3*'\x06'-6, (int)(local_res11>>(bVar2&0x1f)&0x3f) < param_4)) {   // 10位版: *'\n'-10, &0x3ff
  iVar3 = (1 << (bVar2 & 0x1f)) + local_res11;
  if ((param_6 == 0) || (iVar3 != param_5))
      (**(code **)(*plVar1 + 0xf0))(plVar1,param_1,iVar3,0);     // 普通写入
  else (**(code **)(*plVar1 + 0x100))(plVar1,param_1,iVar3,0);   // 推进写入
}
// 无条件：组 0x32 字节共享记录并调 +0x118
local_58=param_1; local_50=param_5; local_44=(local_44&0xffffff9a|0x1a)^param_6&1;
local_2f=param_3; local_2b=param_4; local_27 = 0(6位版) / 1(10位版);
(**(code **)(*plVar1 + 0x118))(plVar1,&local_58);
```

- **param_1** = 硬编码 questId（包装函数写死，如 `fun_759.cpp:93` `FUN_180cb13b0(0xcef,param_2,1,6,0x106,1)`）。
- **param_2** = 脚本事件上下文（宿主分发器传入的第二实参）；`ctx+0x18` 是一个"用户接口持有者"对象，其 vtable[0] 返回 **IUserImp\***。
- **param_3/4/5** = slot（1 起）、required、fullValue；**param_6** = flag。
- `local_res10/local_res11` 是 `&local_res10` 一次调用写出的 6 字节结构 `{u8 status; u32 vars; u8 branch}`。

### 1.2 plVar1 的类型：NPCSvr64 的 IUserImp（有证据，强）

vtable 槽位与 NPCSvr64 `IUserImp`（`/Users/mc/IdeaProjects/58Server/server58-source/NPCServer_NPCSvr64/classes/Account/IUserImp.cpp`）方法一一吻合。判定方法：用 DLL 侧各槽位的**调用形状**与 NPCSvr64 实现的**签名**互证（IUserImp::vftable 数据段不在快照中，映射系推断，但 12 个连续槽全部吻合，置信度高）：

| vtable 槽 | DLL 侧实测调用形状（证据） | IUserImp 方法（NPCSvr64 地址） |
|---|---|---|
| +0xd0 (slot26) | `(this, &out6, questId)` | GetQuestState @140185760（:1686，填 `{status,vars,branch}`） |
| +0xd8 (27) | `(this,0x7f8,0,1)` | SetQuestAcquired @1401858f0（:1753，status=3 AddQuest） |
| +0xe0 (28) | `(this,0x44a)` | SetQuestWaiting @140185a90（:1823，status=6） |
| +0xf0 (30) | `(this,questId,vars,0)` | SetQuestProgress @140185d90（:1957） |
| +0xf8 (31) | `(this,questId,vars,0)`（第4参忽略） | SetQuestProgressMemoryOnly @140185f30（:2021） |
| +0x100 (32) | `(this,questId,vars,0)` | SetQuestSuccess @1401860c0（:2085） |
| +0x108 (33) | `(this, &record)` | ShareProgress(指针版) @140186480（:2237） |
| +0x110 (34) | `(this,q,p,p,0,1,0,0,0,0)` 10参 | ShareProgress(展开版) @140186250（:2149） |
| **+0x118 (35)** | `(this, &record0x32)` | **ShareProgressMultiple(指针版) @140186b90（:2491）** |
| +0x120 (36) | `(this,q,p,p,x,y,0,0,0,0)` 10参 | ShareProgressMultiple(展开版) @140186730（:2299） |

基础偏移：vtable 前 3 槽（slot0-2）为基类 `IObject`（dtor 末尾写 `IObject::vftable`，`IUserImp.cpp:144`）。IUserImp 构造：`IUserImp_ctor(this, User*)`，`this+8 = User*`（`IUserImp.cpp:75` `param_1[1] = param_2`），所有方法做 `if (*(int64_t*)(this+8)!=0) User_Xxx(user,...)`。

**结论**：+0xf0 = `IUserImp::SetQuestProgress`，+0x100 = `IUserImp::SetQuestSuccess`。不是 this 也不是 ctx，而是 ctx+0x18 getter 返回的独立接口对象（围绕 NPCSvr64 `User` 的包装）。"ctx+0x18 是什么类、其 vtable[0] getter 实现" —— 快照中未定位到（见 §5）。

## 2. 写入通道实现体（+0xf0 / +0x100）

两槽实现体**都在快照内**（NPCSvr64），链条完整：

### 普通写入 +0xf0
1. `IUserImp_SetQuestProgress`（IUserImp.cpp:2013-2015）：`User_SetQuestProgress(user, questId, vars, 3, 0)` —— 常量 `3` 为状态过滤器。
2. `User_SetQuestProgress`（`NPCServer_NPCSvr64/classes/Account/User.cpp:7361`，源码路径串 `"..\..\Shared\Quest.cpp"`，:7406）：
   - :7424 `UserQuestData_SetQuestProgress(user+0x208, questId, vars, 3, &out)` —— 更新 NPCSvr 本地镜像；
   - 成功后 :7426-7441 发**服务器间包**：`pkt+10=0x44, +0xc=0xac, +0xd=0xffbb`，载荷 `+0x11=userId, +0x15=questId, +0x19=status(u16), +0x1b=vars(u32), +0x1f=branch, +0x21=memoryOnly(0), +0x22=reason(0)`。
3. 镜像数据层 `UserQuestData_SetQuestProgress`（`NPCServer_NPCSvr64/classes/Account/UserQuestData.cpp:1029`）：条目为 6 字节 `{status u8; vars u32; branch u8}` 线性表（:1104-1131 按 questId 查找）；过滤器 `3` 时要求当前 status∈{3,4}（:1118 `1 < (unsigned char)(status-3)` 为假才通过），然后**只写 vars，保持 status**（:1119-1125）。全程 `EnterCriticalSection`（:1098）。

### 推进写入 +0x100
1. `IUserImp_SetQuestSuccess`（IUserImp.cpp:2142-2143）：`User_SetQuestSuccess(user, questId, vars, 0)`。
2. `User_SetQuestSuccess`（User.cpp:7754）：:7816 `UserQuestData_SetQuestSuccess(user+0x208, questId, vars, &out)`；随后发**同一个 0x44 包**（:7818-7833）。
3. 数据层 `UserQuestData_SetQuestSuccess`（UserQuestData.cpp:1152）：**仅当 status==3**（:1228）才成功：`status 3→4`，vars = 传入值（传入 0 则保留旧值，:1231-1233），branch 保留（:1236-1238）。

### 下游汇聚（MainServer 权威侧，有证据）
- `MainServer_Server64/fun/fun_049.cpp:12264`（字符串 `"UpdateQuestPacket"`、`"d:\...\mainserver\NpcSocket.cpp"` :12234/12235）：解析 0x44 包 → `User_OnUpdateQuestFromNpcServer(user, questId, {status,vars,branch}, memoryOnly, reason)`。
- `MainServer_Server64/classes/Account/User.cpp:197462` `User_OnUpdateQuestFromNpcServer`：
  - :197575 `UserQuestData_UpdateQuest(user+0x8b0, questId, {...})` 写权威内存态；失败则 :197578 `FileLog_Add(... L"User::OnUpdateQuestFromNpcServer, internal state mismatch, user(%x, %s), questId=%d, state=%d, progress = %d")` —— **只记日志，无回滚**；
  - :197583-197595 status!=0 且有会话 → 发**客户端包**：`pkt+10=0x184, +0xc=0x56, +0xd=0xfe7b, +0xf=2, +0x10=questId, +0x14=status(u16), +0x16=vars(u32), +0x1a=branch`，经 `FUN_140c9d4e0(session, pkt)`；
  - :197598-197624 `memoryOnly != 1` 时再发 **DB 包**：`pkt+10=0x56, +0xc=0xae, +0xd=0xffa9, +0xf=2, +0x10=questId, +0x14=status, +0x16=vars, +0x1a=branch, +0x21=<用户字段>`，经 DB 连接池对象（`FUN_1400e9380`，`fun/fun_031.cpp:2040`，源 `"d:\_build\src\server\mainserver\DB.h"`，池上限 200）——**即时（非延迟）DB 写，fire-and-forget**；
  - status==3 → :197627 `User_InvokeQuestUpdatedEventScript(user, questId, vars)` + 审计日志 `FUN_140055700(0x1f6,...)`（:197707）；
  - status==4 → :197647-197649 `User_InvokeQuestSucceedEventScript` + `User_UpdateQuestAcquireCondition` + `User_UpdateQuestSucceedMentorMark` + 日志 `0x1f7`（:197665）；
  - 区域校验：:197569-197572/197714-197771 对任务区域列表（`+0x7cd0/+0x7cb8` 等）做范围判断，不在区域内时走另一分支（:197760 日志 `"User is not in Quest Area -- QuestId : %d, Quest Perform Area : %d"`）。

## 3. 两条通道副作用矩阵

| 维度 | 普通写入（+0xf0 → SetQuestProgress） | 推进写入（+0x100 → SetQuestSuccess） |
|---|---|---|
| 触发条件（DLL） | `flag==0` 或 `newVars != fullValue` | `flag!=0` 且 `newVars == fullValue`（**实测全部 2463 个生成调用点 flag=1**，见 §4d） |
| 数据层守卫（镜像） | 要求 status∈{3,4}；只写 vars，status 不变 | 要求 status==3（否则整体失败、**不发任何包**）；status 3→4，写 vars |
| 写入字段 | vars；status/branch 保持 | vars + status(3→4)；branch 保持 |
| NPCSvr→Main 同步 | 0x44/0xac/0xffbb，memoryOnly=0，reason=0（User.cpp:7426-7441） | **同一包、同一构造代码**（User.cpp:7818-7833），仅 status 字节=4 |
| 持久化 | MainServer 收 0x44 后即时发 DB 包 0x56（User.cpp:197598-197624）；memoryOnly=1 才跳过（MemoryOnly 是另一槽 +0xf8，相机不用） | 同左；status=4 一并落库。**无事务/无回滚**：镜像先写、包后发；Main 侧失败仅 `internal state mismatch` 日志 |
| 客户端同步 | 0x184/0x56/0xfe7b 包（status=3+新 vars → 客户端刷计数 n/required） | 同一包类型，status=4+满 vars → 客户端进入"已完成可交付" |
| 页面/演出/奖励 | 无页面；仅 MainServer 触发 `User_InvokeQuestUpdatedEventScript`（可再驱动脚本，如计数提示） | `User_InvokeQuestSucceedEventScript`；交任务页/奖励窗不在本链路 —— 由玩家与 NPC 对话的 GiveQuestReward 流程（NPCSvr64 `IUserImp_GiveQuestReward`/`IGiveQuestRewardOKEventImp`）另行触发；`IUserImp_PlayQuestCutScene/PlayQuestMovie` 存在于同接口，供其它脚本事件调用 |
| 两通道是否同底层写函数 | 数据层**不同**：`UserQuestData_SetQuestProgress`(:1029) vs `UserQuestData_SetQuestSuccess`(:1152)（同一把 CriticalSection、同一 6 字节条目格式）；传输与 MainServer 处理**完全同路**（同一 0x44 包 → 同一 handler → 同一 `UserQuestData_UpdateQuest` → 同一客户端包/DB 包） | 同左 |

### +0x118（两函数末尾无条件调用）
- `IUserImp::ShareProgressMultiple(this, record*)` → `User::ShareProgressMultiple`（NPCSvr64 User.cpp:7642）：不在队伍/团（user+0x1f8/0x1fc/0x200 全 0 且世界类型≠7）时**按 record+0x11 字节**决定是否发系统消息（=1→`NPCServerToServer_SystemMessage(...,0x1560a2)`，=2→`0x1560a1`；相机填 0 → 静默丢弃，:7702-7719）；在组内则发包 `0x77/0xac/0xff88`（:7723-7748），把 record 的 12×u32 + u16 原样转发 MainServer（MainServer 侧有 `User_ShareQuestProgressWithOthers`、`Party_ShareQuestProgress`、`AllianceBattleGroup_ShareQuestProgress` 等，Server64 symbols.tsv）。
- record 布局（相机）：`+0x00 questId, +0x08 fullValue, +0x14 位标志(bit0=flag), +0x29 slot, +0x2d required, +0x30..31 u16(required高位+channel)`，channel：6 位版=0、10 位版=1（fun_731.cpp:5344/5394）。第三兄弟 `FUN_180cb1610`（:5406）不写 vars，只做纯共享（调用样例 `FUN_180cb1610(0xb802,param_2,1,5,1,0x1e,0x27,5,1)`）。

## 4. 专项确认

**(a) status 枚举**（行为证据，非符号枚举）：
- 0 = 无/未接（`UserQuestData_GetQuestState` 查无此人填 0/0/0，UserQuestData.cpp:811-813；SetQuestProgress 过滤器 0 要求 status==0，:1109-1115）
- 3 = 已接取/进行中（`IUserImp_SetQuestAcquired`→`User_AddQuest(status=3)`，IUserImp.cpp:1814-1817；**相机守卫 `status==3` 即"进行中"**；SetQuestSuccess 只认 3）
- 4 = 已完成待交付（SetQuestSuccess 写入；Main 收到 4 触发 QuestSucceed 事件）
- 5 = 已领奖/终结（`User_OnGetQuestRewardFromMainServer` 写 5，User.cpp:7032；镜像 UpdateQuest 对 4→5 做条目删除，UserQuestData.cpp:321-360）
- 6 = 待接取（`IUserImp_SetQuestWaiting`→AddQuest(status=6)，IUserImp.cpp:1884-1887）
- 过滤器 `{3,4}` 组合：`SetQuestProgress(filter 3)` 接受 3 或 4（:1118）。

**(b) `vars < 0x40000000` 守卫**：两种编码都是 30 位打包（6 位×最多 5 槽 = bit0..29；10 位×3 槽 = bit0..29）；`0x40000000 = 1<<30`。即"vars 必须是合法的 30 位计数器字"——最高 2 位（bit30/31）不在任何编码范围内，作为保留/损坏哨兵位（**推断**，无直接注释证据；但位宽算术完全吻合）。若 vars 被污染成 ≥1<<30（如 0xFFFFFFFF 哨兵），相机直接放弃写入，防止把垃圾位移继续传染。

**(c) 一次事件加多少**：每次调用只做 `vars + (1 << (6*(slot-1)))` 或 `+ (1 << (10*(slot-1)))`，即**该槽位 +1，且只写这一个槽**。不会一次加超过 1；但分发器对同一事件 id 的 handler 是**链表遍历多次调用**（NPCSvr64 `fun/fun_041.cpp:1471-1475`、`fun_040.cpp:1388-1393`），一次事件可依次触发多个任务/多个槽位各自的相机，各自 +1。计数封顶：`(cur & mask) < required` 不满足则整个写入（含 +0x100）都不发生。

**(d) `flag`（param_6）来源**：生成代码的**硬编码立即数**，来自 data_driven_quest.xml 驱动的表行（每行一个包装函数）。实测统计：`MainServer_ScriptDLL64/fun/*.cpp` 中 `FUN_180cb13b0/FUN_180cb14e0` 共 **2463 个调用点全部传 1**、0 个传 0（awk 统计尾参）。因此实际语义退化为：`newVars == fullValue` 即走推进通道。flag 同时被塞进共享记录 +0x14 位标志 bit0 上送 MainServer（语义未见消费点，存疑）。

**(e) 注册点复核**：`fun_731.cpp:6214` `FUN_180cb2ac0` 写 `*(reg+0xa8+idx*8)=fn`，调用点 `fun_229.cpp:1056+`（`FUN_1807c13d0` 等，每任务一条）；`FUN_180cb2ad0`（:6238）为变体（param_3==3 时找空闲槽写 +0xa8 与 +0x3d8）。宿主经 `GetInterface` 拿到该注册表（`NpcScriptMgr.cpp:1042-1043` → `DAT_1503bfd38/40`），分发时 FNV-1a(4 字节键) 哈希查 handler 链（`fun_041.cpp:1440-1447`）。

## 5. 快照中缺失/未能定位的部分（如实说明）

1. **`IUserImp::vftable` 数据本体**：只有符号引用（`IUserImp.cpp:14/29/93` `*param_1 = IUserImp::vftable`），数据段未反编译。§1.2 的槽位映射是从"12 个连续槽的调用形状 vs 实现签名"互证得出的**高置信推断**，非直接读表。
2. **ctx（param_2）的类与 ctx+0x18 的 getter 实现**：分发器入口存在（`fun_041.cpp:1423` 读注册表 +0x4e8 哈希表、+0x528 函数指针 invoker；`fun_040.cpp:1446` 六参包装），但 invoker 与 ctx 构造代码在快照中无具名函数、无可见调用者（经函数指针调用），故"ctx+0x18 是哪个类、其 vtable[0] 如何返回 IUserImp"未定位。
3. **DB 侧最终 SQL**：MainServer 发出的 0x56 DB 包的接收端/SQL 文本不在本快照（DB.h 连接池对端；CacheD/DB 服务器二进制未含此协议处理的可读副本）。
4. **`FUN_180cb1610`/`+0x118` 共享记录在 MainServer 侧的消费逻辑**（`Party_ShareQuestProgress` 等的包分派点）未逐行展开，仅确认存在同名类方法。
5. `+0xe8 = IUserImp::GetQuestProgress` 推断自槽位顺序（DLL 中无调用样本），置信度略低于其它槽。

## 6. 关键文件路径索引

- 相机函数：`/Users/mc/IdeaProjects/58Server/server58-source/MainServer_ScriptDLL64/fun/fun_731.cpp:5306,5356,5406`；包装/调用点：`fun/fun_759.cpp:93`；注册写入：`fun/fun_731.cpp:6214,6238`；注册调用点：`fun/fun_229.cpp:1056+`
- 接口实现（+0xd0/+0xf0/+0x100/+0x118 等）：`/Users/mc/IdeaProjects/58Server/server58-source/NPCServer_NPCSvr64/classes/Account/IUserImp.cpp:1686,1753,1823,1893,1957,2021,2085,2149,2237,2299,2395,2491`
- 数据层镜像：`/Users/mc/IdeaProjects/58Server/server58-source/NPCServer_NPCSvr64/classes/Account/UserQuestData.cpp:219,748,1029,1152,1265`
- NPCSvr→Main 包构造：`/Users/mc/IdeaProjects/58Server/server58-source/NPCServer_NPCSvr64/classes/Account/User.cpp:6693,7361,7447,7642,7754`
- 宿主加载/注册：`/Users/mc/IdeaProjects/58Server/server58-source/NPCServer_NPCSvr64/classes/NPC/NpcScriptMgr.cpp:1000-1047`、`fun/fun_052.cpp:2750,3133-3140`；分发器：`fun/fun_041.cpp:1419,1532`、`fun/fun_040.cpp:1380,1446`
- MainServer 权威侧：`/Users/mc/IdeaProjects/58Server/server58-source/MainServer_Server64/fun/fun_049.cpp:12264`；`classes/Account/User.cpp:197462`（客户端包 :197584-197595，DB 包 :197598-197624，状态分派 :197625-197711）；`classes/Account/UserQuestData.cpp:412
