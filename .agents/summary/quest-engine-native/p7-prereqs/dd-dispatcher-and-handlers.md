# DD（data_driven_quest）Quest 子系统考古报告 — P0a 缺口补充

> 考古批次：2026-10-01（P1 第二横切并行后台任务；只读）
> 证据缩写：`C` = /Users/mc/IdeaProjects/58Server/server58/MainServer_ScriptDLL64/ScriptDLL64.c（全量反编译）；`NP` = server58-source/NPCServer_NPCSvr64/；`SV` = server58-source/MainServer_Server64/；`DLL` = MainServer_ScriptDLL64 快照（server58-source/）。所有行号均已核对。
> 定位：P7（DataDriven 切换）前置证据；回答 P0a 遗留缺口①击杀/事件分发器、②其余 7 类 progress handler 布局、③接取侧 5 类触发。

## 0. 核心结论（一张图）

```
ScriptDLL64.dll 唯一导出 = GetInterface (RVA 0xcb5910, 主 EXE 调 GetProcAddress 取用)
 ├─ 宿主(NPCSvr64 / Server64)把自己的 IUserImp 对象传入 → 存 DLL 侧 DAT_184720398
 └─ 返回 12 槽接口对象 {vptr=0x18133a218}（C:2146355 GetInterface）
     slot2 → &DAT_1847204e0  「quest 事件管理器 mgr」
     slot3 → &DAT_184720a58  「questid→{fp,flag} 命名回调表」

DAT_184720a10 / a50 / 9c8 / a08 不是独立全局 —— 它们是 mgr 的字段：
   DAT_1847209c8 = mgr+0x4E8 (hash_map B)   DAT_184720a08 = mgr+0x528 (B 的处理函数指针 = Pvp)
   DAT_184720a10 = mgr+0x530 (hash_map C)   DAT_184720a50 = mgr+0x570 (C 的处理函数指针 = Hunt)
   DAT_184720a18/9d0 = 各自"已初始化"标志
读者 = 宿主侧内联汇编（直接按 mgr 指针 + 偏移访问），不在 DLL 内 —— 所以在 ScriptDLL64.c 里"只见写入不见读者"。
```

mgr（DAT_1847204e0）结构（构造 C:2144002 FUN_180cb2c50）：
- `+0x000 ~ +0x217`：24 个 `vector<Entry>`（步长 0x18），Entry = `{int id@+0; u32 pad@+4; void(*fp)()@+8}`（0x10 字节，由 FUN_180c4dea0 C:2075449 push）
- `+0x258` 起 + `+0x268 + tag*0x10`（tag=5/6/7，FUN_180c4dfd0 C:2075526）、`+0x3F8`(tag 9)、`+0x408`(tag 10 CutScene)、`+0x428`(tag 0x10)、`+0x438`(tag 0x11)、`+0x448`(tag 0xD Movie)、`+0x458`(tag 0xF)、`+0x468`(tag 0x12)、`+0x478`(tag 0x13)、`+0x488`(tag 0x15)、`+0x498`(tag 0x14)：`map<int key, vector<Entry>>`
- `+0x4A8` hash_map A（key=npc/monster tplid，FUN_180cb47d0 tag 0xB），节点 `+0x18` 为 vector
- `+0x4E8` hash_map B、`+0x530` hash_map C（FUN_180cb2a10 构造）

## 1. P0a 缺口①：击杀/事件分发器 —— 谁调用 DAT_184720a50/表 a10

### 1.1 为什么 DLL 反编译里没有读者
用自写 RIP-相对位移全扫描（含 image base 0x180000000）扫 MainServer/ScriptDLL64.dll .text：
- `DAT_184720a50`：仅 2 处引用，均位于加载器 FUN_180c4e2d0 内部 —— `0x180c4eb9f`（`mov rax,[a50]`）与 `0x180c4ebe1`（store FUN_180c46020），伴随 `cmp qword [0x184720a17],0` 守卫（即反编译 C:2076036-2076037 的 `if (DAT_184720a18==0) DAT_184720a50=FUN_180c46020`）。
- `DAT_184720a10`：仅 1 处（`0x180c4ebd5` `lea rcx,&a10` → 作为 FUN_180c42ba0 的实参，C:2076040）。
- 其余 handler（FUN_180c46020/466a0/467b0/46980/46bb0/46c90/46d80/46e90/45f10…）的全部 xref 也都在加载器区间 0x180c4e2d0~0x180c4f010 内（即只在注册时取址）。

结论：DLL 侧确实无分发代码；**读者在宿主，且按 `mgr指针+0x528/0x570` 这种字段偏移访问**（NPCSvr64.exe / Server64 二进制中无 `0x184720a10/a50` 的 imm64 绝对引用，已扫描验证为 0 命中）。

### 1.2 真正的分发点（宿主侧）

**(a) NPCSvr64 —— PacketDie（击杀包，ServerSocket.cpp:0x4f3）** — NP/fun/fun_040.cpp:4706 起的 FUN_1401f4b70，守护字符串 `"PacketDie"`（fun_040.cpp:4805）：
1. 找到受害生物 plVar7 与攻击者 plVar8（类型==1、双方 map 不同、死法非 0xC/0x11 才算玩家击杀），组装 0x22 字节击杀上下文 `local_1a60`：`+0 = 0xB(事件类型 DIE)`、`+8 = 受害者 objid`、`+0xC..0x1B = 受害者 8 个 u16 字段（tplid/等级等，取自 creature +0x18c/+0x3c/+0x1EC/+0x1E4/+0x1DC/+0x1A4）`（fun_040.cpp:4825-4840）。
2. `FUN_14020fc30(plVar8,&idarray,1)`（fun_041.cpp:2918）= `GetCurQuestIds(user,&arr,flag)` —— 返回该玩家**当前进行中任务的 questid 数组**（队长且 `+0x204==2` 时取全队）。
3. 先内联遍历 **hash A（mgr+0x4A8）**：命中 key 后走节点 `+0x18` vector，`ctx 高32位 = entry.id; fp(IUserImp,&ctx)`（fun_040.cpp:4856-4866）。
4. 再 `FUN_140203960(mgr, user, &ctx, arg+6, count, ids, arg)`（fun_040.cpp:4871）→ **读 hash B（mgr+0x4E8，即 DAT_1847209c8）并以 mgr+0x528（即 DAT_184720a08 = FUN_180c46980）为处理函数**（fun_041.cpp:1423-1530：`(**(code**)(param_1+0x528))(param_2,param_3,param_4, entry.key@+8, entry.val@+0x14, param_7)`）。
5. 若攻击者 `+0x1B0()!=0`（组队/延迟状态），把 0x22 字节记录写入环形缓冲 `DAT_15052b130`（stride 0x22）并 `FUN_14020fd10` 排队（fun_040.cpp:4876-4896）。

**(b) NPCSvr64 —— PacketValidMemberList（ServerSocket.cpp:0xd4a）** — NP/fun/fun_040.cpp:11144 起 FUN_1401fe590（守护字符串 `"PacketValidMemberList"` fun_040.cpp:11215）：对每个成员 user：
- `FUN_14020fc30(user,&arr,0)` 取 questid 列表；若包字段 `*param_2==0`（无排队记录）→ `FUN_1402033a0(mgr,user,uVar3,uVar4,count,ids,uVar2)`（fun_040.cpp:11236）→ **读 hash C（mgr+0x530 = DAT_184720a10），fp = mgr+0x570 = DAT_184720a50 = FUN_180c46020（Hunt 计数）**（fun_041.cpp:1305-1397）；否则用环形缓冲记录重放 `FUN_140203960`（Pvp 通道，fun_040.cpp:11240）。

**(c) Server64（MainServer/Server64.exe，用户所在进程）**
- `User.cpp`（SV/classes/Account/User.cpp:57946-57958，源标注 `"..\\mainserver\\PCScriptInterface.cpp",0x20`）：**玩家死亡**时 `evt=1`，遍历 **直接向量 vec[1]（mgr+0x18）**：`ctx = {u64: lo=1, hi=entry.id}; fp(&IUserImp{vftable,user}, &ctx)` —— 即与 NP 的 FUN_1401ef560 同型逻辑（NP/fun/fun_040.cpp:1378-1396 的 FUN_1401ef560 在 NPCSvr64 内无调用者，是 Server64 侧同源代码的对应物）。
- `IUserImp.cpp:5055-5057`：`evt=6`，按 NPC tplid 查 **map mgr+0x268+6*0x10**（事件 6 = 进入 NPC 感知区）。
- `GatherSource.cpp:2499-2500`：`evt=7`，同型 map walk（采集/区域事件）。
- 其余 `+0x268+evt*0x10` walk 共 10+ 处（User.cpp:58921/59477/59842/60047/60321/61442/61511/79662…）。

**(d) DLL 接口 slot3 对象 DAT_184720a58（questid→{fp,flag}，fp=0x180c47090）**
- 注册：C:2076153-2076154（`param_2[0x1a] != -2` 时 `FUN_180c44a20(&DAT_184720a58,…,{0x80c47090,1})`）。
- 宿主读者：NP/fun/fun_039.cpp:2540-2562 FUN_1401e6750 —— 按 `ctx[0]`（questid）查 map，tail-call `(*(code*)node[5])(ctx)`；被 1401e7818 处调用。Server64 侧同构（`_DAT_14f4395e0`）。

### 1.3 调用约定（事件→DD handler）
- hash B/C 的 fp 统一 6 参：`fp(IUserImp* user, ctx/杀敌信息, u32 mapId, u32 questid=entry.key@+8, u32 expectedStep=entry.val@+0x14, float distance=arg6 位模式)`。
- `questid` 来源 = `FUN_14020fc30` = `UserQuestData_GetCurQuestIds`（NP/fun/fun_041.cpp:2918-2938；更名见 NP/renames.tsv:1394-1395）。
- `expectedStep` 来源 = 注册时步号：加载器 case 2/5 在插入 hash 前把 `local_res20/uVar22`（当前步循环计数）写入紧邻 key 的值字段（C:2076039 `local_154 = local_res20`；C:2076078 `local_144 = uVar22`）。
- 直接向量 fp 为 2 参：`fp(IUserImp* user, int* ctx)`，`ctx[0]=事件类型`（=向量下标），`ctx[1]=entry.id`（宿主每次覆写，见 NP fun_040.cpp:1390）。

## 2. P0a 缺口②：8 类 progress handler 的 vars 布局与完成算术

共用前奏（所有 6 参 handler，Hunt/Pvp）：C:2069982 FUN_180c46020 / C:2070487 FUN_180c46980
- `prog = (+0xE8)(user, questid)`（GetQuestProgress，u32 打包：bit0-5=当前步，之后每 6 位一个子计数器）；`if ((prog&0x3F) != expectedStep) return`。
- `def = map DAT_184720b00[questid] 的节点+0x28`（找不到 → 空def `DAT_184720a70`，`def+0==0` 直接 return）。
- 距离/等级闸门按 `def+0xE0`（piVar[0x1C]）取值：0→dist≤2500 且 `+0x30(mapId)==param_3` 放行、需 `+0x98==0`；1→dist≤10000；2→dist≤10000；5/6→dist≤40000（5 还要求 `+0x30==param_3 || +0xA8`）；再过 `+0xA0` 检查。
- 完成写回：中间步 `(+0xF0)(user,questid,val,0)` = SetQuestProgress；最后一步 `(+0x100)(user,questid,val,1)` = SetQuestSuccess；随后调动作表解释器。

各 handler（步条目 = `def+0xF0` 向量元素 0x10 字节 `{int kind@0; void* data@8}`；data 字段按各类）：

| 类别(kind) | 处理函数 | 触发事件 | data 布局 | 完成算术 | 副作用 |
|---|---|---|---|---|---|
| **Hunt(2)** | FUN_180c46020（C:2069982，fp 存 mgr+0x570/DAT_184720a50） | NP PacketValidMemberList→FUN_1402033a0 | data+8：子目标数组，步长0x38，每个 `{vector<int> monsterIds@+0x18; u32 count@+0x30}`；动作表 data+0x20 | 5 个 6 位子计数（bit6..35，移位 6/12/18/24/30）；击杀 tplid（param_2，来自包 `param_2[1]`）命中子目标列表且未满→该字段+1；任一+1但不全满→SetQuestProgress；全满→`prog=(prog&0x3F)+1` 步进，最后一步 SetQuestSuccess | FUN_180c4d190(data+0x20,user,questid)（C:2074785） |
| **Pvp(5)** | FUN_180c46980（C:2070487，fp 存 mgr+0x528/DAT_184720a08） | NP PacketDie→FUN_140203960 | data+8=需击杀数；data+0xC/0x10=受害者tplid下/上界(0=不限)；data+0x14=等级差容许 | 检查 `ctx+0xC`(u16)+def+0x14 ≥ user `+0x70`(GetLevel)，`ctx+0x1E`∈[+0xC,+0x10]；counter=(prog>>6)&0x3F，+1<count→`prog+=0x40`；否则步进/成功（无子目标列表） | 无 |
| **CollectItem(3)** | FUN_180c46e90（C:2070772，注册 event 5，C:2075855/2076044） | Server64 map(+0x268+5*0x10)（IUserImp.cpp/GatherSource.cpp walk） | data+8=npc id（=ctx+8 比较）；data+0xC=需求数 | 状态==3(进行中)：counter=(prog>>6)&0x3F，+1<count→`prog+=0x40`；否则步进/成功。状态!=3：def+4==3 且 npc 匹配 → `(+0xD8)(user,questid,0,0)` 接取后 FUN_180c4cd50(data+0x10,…) | FUN_180c4cd50（C:2074638）动作表 |
| **Talk(4)** | FUN_180c466a0（C:2070313，注册 vec2=DAT_184720510，C:2076149） | Server64 直接向量 evt=2 | def+0x40=负数则 `(+0x160)` 否则 SetQuestProgress 的值；def+0x44=需要次数；def+0x48=目标 npc tplid（=ctx+0xC 比较）；def+0xA0 起动作向量 | `(+0xD0)` 取 state(必须==3)与 prog；`(prog&0x3F) < def+0x44` → SetQuestProgress(questid, def+0x40)；然后对 def+0xA0 向量逐项 `(+0x1D8)(user,item)`（对话奖励/给物） | 逐项 +0x1D8 |
| **PvpAccept/Event0x15 & 0x13** | FUN_180cb4720 值对象（C:2145397） | map +0x488 / +0x478 | —（宿主 FUN_1401ef5d0→FUN_140204430 走 +0x488） | 同 vector-Entry 约定 | — |
| **EnterArea(6)** | FUN_180c47bf0（C:2071405，挂对象槽 +0x278，case 6 注册 C:2076101） | 对象虚槽调用（IOneQuestScriptNpc 体系） | data+8=areaId（`FUN_1810798b0(data+8)`），与 `(+0x50)(user)` 比较；动作表 data+0x28 | 进入匹配区域→步进/成功 | FUN_180c4c8d0(data+0x28,…)（C:2074485） |
| **EnterWorld(7)** | FUN_180c467b0（C:2070375，注册 event 0x12，C:2075859/2076090+2076104） | map +0x468 walk | data+8=world/zone id（=ctx+8） | 状态==3：`(prog&0x3F)==ctx+0xC` 校验后直接步进/成功；状态!=3：def+4==7 且 zone 匹配→接取+FUN_180c4cd50 | FUN_180c4cd50 |
| **TalkFOBJ(9)** | FUN_180c478e0（C:2071274，挂槽 +0x230，case 9 注册 C:2076135） | 对象虚槽调用 | data+0x20=vector<int> fobjId（与 `(+0x50)(user)` 比较）；data+0x38=动作类型表(0/1/2：+0x518/+0x4D8/+0x4C0 或 +0x500 加 10000)；动作表 data+0x50 | 与 Hunt 同款 5×6bit 计数：首次命中从 0→1 计数并按动作类型给效果，全满→步进/成功 | FUN_180c4c8d0(data+0x50,…) |
| **ItemPlay(3接取/进行)** | FUN_180c474b0（C:2071069，挂槽 +0x1E0）——**确认：无独立 vars 计数** | 对象虚槽调用（对话/物品玩法流） | 走对话页码表 0x3F3/0x548/0x69D/0x7F2/0x947/0xA9C/0xBF1/0xD46/0xE9B/0xFF0/0x1964/0x1AB9/0x1C0E/0x1D63/0x1EB8（=0x3F3+0x14A*idx）；动作码 ≥10000：`SetQuestProgress(questid, code-9999)`；0x3F1：`SetQuestSuccess(questid, step+1)` + `(+0x1B0)`；0x3F0/0x280F：选奖励 `(+0x5D8)` | 完成由动作码驱动（服务器只做转发），无 6bit 计数 | FUN_180c4d5b0 收尾（C:2071073 尾部） |

动作表解释器 FUN_180c4cd50（C:2074638）/ FUN_180c4d190（C:2074785）case 1-10：1=批量 `(+0x2F8)`（生成/给物）、2=批量 `(+0x1D0)`（移除）、3=`(+0x2D0)`（影像/坐标4元组）、4=8字节 `{0/1/2/3, id}` → `+400`/`+0x1B8`（cutscene 开关）、5=奖励物列表 `(+0x358)`/`(+0x180)`（0=带坐标掉落，非0=直接给）、7=`(+0x2B8)`、9=`(+0x220)`、10=定时器 `(+0x250)(ms*1000)`。

**quest 成功处理（event 0x10/0x11）**
- FUN_180c4e1b0（C:2075626）：event 0x10 → map+0x428，条目 `{questid, expected(=param_2[0x1b]), fp}`（FUN_180c4dde0 C:2075325 三字段 0x10 字节）。
- FUN_180c4e100（C:2075580）：event 0x11 → map+0x438，`map<questid,fp>`，日志字符串 `"RegisterQuestSucceedEventHandler, %d already registered"`。
- FUN_180c45f10（C:2069914，event 0x10 的 fp）：取 `ctx+4`，`local_30=+0x30(mapId)`，构造 `{ctx+4,ctx+4,0,0,mapId,…}` 后 `(+600)(user,&local_38,0)` —— 按 XML `param_2[0x1b]`（=步数）在最后一击时触发成功/领奖。
- 加载器尾部（C:2076151-2076186）：`param_2[0x1a]!=-2` 时注册 a58；若 `steps.size()==param_2[0x1b]` 则直接把 `{FUN_180c45f10,1}` 放进 DAT_184720918（set），否则注册 event 0x10。

## 3. 接取侧 5 类 kind 的注册与触发

**XML 解析**（DLL/classes/Quest/QuestProgressExtraInfo_Talk.cpp，函数 FUN_180c492d0 @180c492d0，行 9-116）：读属性 `id`、`category_acquire_`（`"Data Driven - Category_Acquire"`）：
- ItemPlay→`param_2+8 = 3`；Talk→4（0x40 字节对象）；LevelUp→8；EnterWorld→7；LevelUpLogIn→10（8/7/10 共用 QuestProgressExtraInfo_LevelUp/EnterWorld vftable 对象，存 `param_2+0x10`）。随后 `con_quest`(param_2+0x38)、`con_quest_list`（→ZoneQuestList, param_2+0x3C）、`reward_npc_name`（param_2+0x18）。

**progress 侧类别**（DLL/classes/Item/QuestProgressExtraInfo_ItemPlay.cpp FUN_180c4b330，属性哈希 `0xFE9="category"`，`0xFEC..0xFF6=子属性0..10`）：CollectItem→1、Hunt→2、ItemPlay→3、Talk→4、Pvp→5（默认 `+0x14=10`）、EnterArea→6、EnterWorld→7、TalkFOBJ→9 —— 与加载器步循环 switch(case 1/2/3/4/5/6/7/9) 一一对应（C:2075886-2076135）。

**加载器**（FUN_180c4e2d0 C:2075682，调用点 C:2072341，位于 `DataDrivenQuestLoader::LoadBasicInfo/LoadProgressInfo` 内，param_1 = loader this，`param_1+0x90` = questdef map、`param_1+0xA0` = 名称映射）：
- 序言：acquire==4(Talk) → 建 0x640 对象 + 接取表项 kind=0/questid，槽 `+0x1D8=FUN_180c47220`；恒建第二个对象 + 接取表项 `kind=4(Talk), value=-1`，槽 `+0x238=FUN_180c473e0`；若步向量空 → 追加 `kind=3,value=0` 表项 + `+0x1E0=FUN_180c474b0`（C:2075690-2075790）。
- 接取触发分派（C:2075792-2075813）：acquire==3(ItemPlay) → `FUN_180c4dfd0(mgr,5, npc标识, questid, FUN_180c46e90)`；==7(EnterWorld) → `FUN_180c4d890(mgr,0x12,…,FUN_180c467b0)`；==8(LevelUp) → `FUN_180c4dea0(vec0, questid, FUN_180c46bb0)`；==10(LevelUpLogIn) → 再加 `FUN_180c4dea0(vec3, questid, FUN_180c46c90)`。
- **LevelUp / LevelUpLogIn 的执行点**：FUN_180c46bb0（C:2070709）/ FUN_180c46c90（C:2070772）——检查 `def+4 ∈ {8,10}`、`ctx+8 == def+8`(所需等级)，然后 `(+0xD8)(user,questid,0,0)`（接取）+ `FUN_180c4cd50(def+0x10,…)`。宿主触发在 **Server64**：等级提升/登入路径遍历 vec0（mgr+0x000）（NP 版 FUN_1401ef560 无调用者；Server64 版见 SV User.cpp:57950-57958 同型内联 walk）。FUN_180c47090（@180c47090）为 a58 回调：`(+0x28)(DAT_184720398, ctx[4], out)` 找 user → `(+0xD0)` 校验 state==3 → `prog&0x3F < def+0x6C` 时 `SetQuestProgress(def+0x68)`，最后 `(+0x30)` ReleaseUser。
- 0x640 对象的接取条件表：`+0x570`=数量，`+0x578+i*6 = {u8 类型, u32 value, u8}`，`+0x5F0+i*4` 辅助值（FUN_180c4df60 C:2075398 写入；FUN_180cb3070 C:2144075 同款）。
- 对话→接取链路：宿主 `NpcScriptMgr::RegisterOneQuestClass`（NP/classes/NPC/IQuestScriptNpc.cpp:14-88，"NpcScriptMgr::RegisterOneQuestClass"）按名称注册 DLL 类（DLL 侧工厂 FUN_180cb5920 `IOneQuestScriptNpc::vftable`，C:2146396），宿主调其虚方法（+0x1D8/+0x1E0/+0x238…）→ FUN_180c474b0 走对话页码/动作码完成接取与交任务（0x3F0 选奖励→`(+0x5D8)`，0x3F1→SetQuestSuccess）。

## 4. 接口与注册面（补遗）

- PE 导出：MainServer/ScriptDLL64.dll 与 NPCServer/ScriptDLL64.dll 均**仅 1 个导出 GetInterface**（导出目录 NumFuncs=1；地址 rva 0x13ef17c→0xcb5910；NPCServer 副本导出名区已损坏但地址一致）。版本号 0x2A 校验：NP/classes/NPC/NpcScriptMgr.cpp:1000-1047（`GetProcAddress(...,"GetInterface")`、`(**(code**)(*DAT_1503bfd50))() != 0x2a` 报 "Incorrect Script.Dll, interface version mismtach"）；SV/classes/NPC/NpcScriptMgr.cpp:464-513 同构（指针 DAT_14f4395d8=slot2 mgr、DAT_14f4395e0=slot3 a58）。
- 12 槽 vtable 0x18133a218（二进制 .rdata 实测）：0=版本 0x2A(FUN_180cb3c80)；1=保存双表(FUN_180cb4170)；2=返回&mgr(FUN_180cb3c90)；3=返回&a58(FUN_180cb3ca0)；4=类工厂初始化(FUN_180cb4160→FUN_18106e500(&DAT_184720b20))；5/6=工厂枚举(FUN_180cb4c40/c00)；7/8/10=a70 迭代(FUN_180cb4150/42c0/4c30)；9=FUN_180cb4140(&DAT_184720b40)；11=拷贝日志路径(FUN_180cb4c10)。
- 事件 tag→容器总表（DLL 注册函数，全部带 `if (param_2 != TAG) return 0` 守卫与 Register* 日志串）：9→+0x3F8(C:2145290 FUN_180cb4610)；10 "CutScene"→+0x408(C:2145174)；0xB→hashA+0x4A8(C:2145432 FUN_180cb47d0)；0xD "Movie"→+0x448(C:2145551)；0xF→+0x458(C:2145637)；0x12→+0x468(C:2075105 FUN_180c4d890)；0x13→+0x478(C:2145501)；0x14→+0x498(C:2145322)；0x15→+0x488(C:2145237)；5/6/7→+0x268+tag*0x10(C:2075526)；8→+0x258(FUN_180c4dce0 C:2075262)；0x10→+0x428；0x11→+0x438。

## 5. EVIDENCE_MISSING / 后续路径

1. **事件 tag 的权威名称表**：tag 9/0xB/0xD/0xF/0x13/0x14/0x15 的语义名只从日志串拿到 CutScene/Movie 两个；注册 thunk 族（FUN_180cb2b40~FUN_180cb30d0）在整个 NPCSvr64.exe/ScriptDLL64.dll 中均无静态 xref（qword+RIP 双扫描 0 命中），推断为 IAIScriptNpcImp 虚方法/间接跳转目标。下一步：对 FUN_180cb2990/2a10（hash 构造）与 IAIScriptNpcImp::vftable（C 中 20999 处引用）做槽位展开，定位注册 API 的脚本侧调用者。
2. **FUN_180c42ba0 hash 节点 +0x14（expectedStep）的确切写入口**：加载器在调用前写 `local_154/local_144 = 步号`，但 Ghidra 只还原出 3+1 参签名（C:2066979）；步号如何进入节点需反汇编 0x180c42ba0 调用点栈布局确认。
3. **击杀上下文 0x22 字节的逐字段语义**：ctx+0xC/0x1E 等只确认了"受害者 u16 字段之一/一个 u32"（fun_040.cpp:4825-4840 的源偏移 +0x18C/+0x3C/+0x1EC/+0x1E4/+0x1DC/+0x1A4 与 creature 结构对照尚未完成）。
4. **`FUN_180c4dfd0` event5 注册 key** `*(int*)(wstring.data+8)`（C:2075855/2076044）的解释（NPC id 字符串？还是已转换对象）未定论；下一步追 XML 行结构 `param_2+4` 的填充函数（LoadBasicInfo 内，C:2072200-2072300 区段）。
5. **FUN_180c46020 中 `+0x98/+0xA0/+0xA8/+0x70/+0x50` 等宿主 IUserImp 槽的命名**：只有 +0xE8/+0xF0/+0x100/+0xD0/+0xD8 已由 P0a 前置+renames.tsv 锚定；其余槽号→IUserImp 方法名需展开 NPCSvr64.exe 中 IUserImp::vftable（140183140 构造所在类的 vftable）。
6. NPCServer/ScriptDLL64.dll 导出目录的名称串损坏（本报告用地址比对绕过）——若需导出名原始证据，需从 MainServer 副本或安装包另取。
