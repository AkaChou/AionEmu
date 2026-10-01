# DD 分发器宿主接口细节考古（IUserImp 133 槽 / hash 节点 +0x14 / 事件 tag 全链路）

> 考古批次：2026-10-01（后台 Explore agent，只读）。定位：补全 `dd-dispatcher-and-handlers.md` §5 的三个
> EVIDENCE_MISSING（IUserImp 槽位命名 / FUN_180c42ba0 hash 节点 expectedStep / 事件 tag 语义名）。
> 方法：python + capstone 直接反汇编 `MainServer/ScriptDLL64.dll`、`NPCServer/NPCSvr64.exe`、
> `MainServer/Server64.exe`（image base 按 PE 头 0x180000000 / 0x140000000 实读）。
> 证据缩写：`C` = server58/MainServer_ScriptDLL64/ScriptDLL64.c；`NP` = server58-source/NPCServer_NPCSvr64/；
> `SV` = server58-source/MainServer_Server64/。
> 结论状态：**结构层面本轮已无缺口**；语义残缺项见 §4（tag 9/0x13 本 build 零证据面，不可还原 = 合法冻结态）。

---

## 第 1 章：IUserImp vftable 槽位命名（前报 §5.5 → 已还原）

### 1.1 vftable 定位（构造点反汇编实测）

| 宿主 | 构造函数 | 存入的 vftable | 槽数 | 证据 |
|---|---|---|---|---|
| NPCSvr64 | `IUserImp::IUserImp` @0x140183140 | **0x140475040**（.rdata） | 133（+0x000~+0x420） | `lea rax,[rip+0x2f1ee7]`→0x140475040；`mov [rcx],rax`（0x140183152-0x14018315e）；[-1]=0x140509a58（RTTI COL，未解析名） |
| Server64 | `IUserImp::IUserImp` @0x1404d9bb0 | **0x1411768d8**（.rdata） | 133 | `lea rax,[rip+0xc9cd14]`→0x1411768d8（0x1404d9bbd）；`mov [rcx],rax`；[-1]=0x141394a40 |

两表 **133 槽逐槽 ABI 对齐**（同名方法落同槽）。wrapper 对象 = `{vftable, user*}` 16 字节（NP ctor
`mov [rcx+8],rdx`；SV 同）。方法名来源 = NP/SV renames.tsv（NP renames.tsv IUserImp 块），函数体内线程守卫串
（如 `"IUserImp::GetCharacterName"`、源文件标注 `NpcAIOrderFunc.cpp:0xfdb`）为原始名铁证。

### 1.2 DD handler 已用槽位表（NPCSvr64 视角；SV 侧同槽同名实现不同址）

| 偏移 | 槽 | NP 实现 | Server64 实现 | 方法名 / 语义 | 证据 |
|---|---|---|---|---|---|
| +0x030 | 6 | 0x140183940 | 0x1404d9cd0(未名) | **GetId** —— 返回 `dword[user+0x8C]`（0x140183a56 实测） | vtable dump；⚠️ 前报"+0x30=mapId"标注与槽名冲突，见 §4 遗留④ |
| +0x050 | 10 | 0x140183dd0 | 0x1404d9e90(未名) | **GetCharacterName** —— 单参，返回 `user->creature->vtbl+0x80()` 的返回值（0x140183ed7 尾部）；DD EnterArea 用它与 `FUN_1810798b0`（**0x1003F 逐字符字符串哈希**，C:2694308）比较 ⇒ 实际返回 **u32 名字哈希（name-id）** | NP/classes/Account/IUserImp.cpp @140183dd0；C:2071464-2071469 |
| +0x070 | 14 | 0x140184390 | 0x1404d9f10(未名) | **GetLevel**（前报锚定 ✓） | vtable dump |
| +0x098 | 19 | 0x140184c60 | 0x1404da8a0 | **IsAllianceUser**（Pvp 距离闸门 `+0x98==0` = 非同盟） | vtable dump |
| +0x0A0 | 20 | 0x140184df0 | 0x1404daa30 | **IsUnionUser** | vtable dump |
| +0x0A8 | 21 | 0x140184f80 | 0x1404dabc0 | **IsBattleGroupUser**（Pvp `+0x30==param_3 || +0xA8` = 同图或战场组） | vtable dump |
| +0x0D0 | 26 | 0x140185760 | 0x1404db2a0(未名) | **GetQuestState**（锚 ✓；读 `{state:char, vars:u32}`） | vtable dump |
| +0x0D8 | 27 | 0x1401858f0 | 0x1404db2e0(未名) | **SetQuestAcquired**＝接取（锚 ✓） | vtable dump |
| +0x0E0 | 28 | 0x140185a90 | — | SetQuestWaiting | vtable dump |
| +0x0E8 | 29 | 0x140185c10 | 0x1404db360(未名) | **GetQuestProgress**（锚 ✓） | vtable dump |
| +0x0F0 | 30 | 0x140185d90 | 0x1404db3a0(未名) | **SetQuestProgress**（锚 ✓） | vtable dump |
| +0x0F8 | 31 | 0x140185f30 | — | SetQuestProgressMemoryOnly | vtable dump |
| +0x100 | 32 | 0x1401860c0 | 0x1404db450(未名) | **SetQuestSuccess**（锚 ✓） | vtable dump |
| +0x118 | 35 | 0x140186b90 | 0x1404dd380(未名) | **ShareProgressMultiple**（进度/展示包） | vtable dump |
| +0x160 | 44 | 0x140187180 | 0x1404db570(未名) | **DeleteWorkingQuest**（Talk handler `def+0x40<0` 分支＝删任务，语义自洽） | vtable dump |
| +0x1B0 | 54 | 0x140188620 | 0x1404dbba0(未名) | **PlayCutSceneByQuestShare**（ItemPlay 0x3F1 成功后播动画，自洽） | vtable dump |
| +0x1B8 | 55 | 0x1401887d0 | 0x1404dbc10(未名) | **PlayMovie**（动作表 case 4） | vtable dump |
| +0x1D0 | 58 | 0x140188b30 | 0x1404dbd90(未名) | **RemoveItem**（动作 case 2 移除） | vtable dump |
| +0x1D8 | 59 | 0x140188e70 | 0x1404dbe40(未名) | **RemoveItemMax**（Talk 逐项扣任务物品） | vtable dump |
| +0x220 | 68 | 0x140189ec0 | 0x1404dc6c0 | **EnterInstance**（动作 case 9） | vtable dump |
| +0x250 | 74 | 0x14018a440 | 0x1404dcb80 | **AddQuestTimer**（动作 case 10 定时器） | vtable dump |
| +0x2B8 | 87 | 0x14018bbe0 | 0x1404de2c0 | **Say**（动作 case 7） | vtable dump |
| +0x2D0 | 90 | 0x14018c260 | 0x1404dea50 | **Teleport**（动作 case 3 坐标4元组） | vtable dump |
| +0x2F8 | 95 | 0x14018cad0 | 0x1404df430 | **AddItem**（动作 case 1 给物） | vtable dump |
| +0x358 | 107 | 0x14018dd60 | 0x1404e0ae0 | **CreateMonster**（动作 case 5 带坐标掉落/刷怪） | vtable dump |

**两处前报勘误**（本次实测）：

1. 前报动作表 case 10 的 `(+0x250)` = AddQuestTimer ✓ 不变；但前报 FUN_180c45f10 的 "`(+600)`" 是
   **Ghidra 十进制 600 = 0x258 = 槽 75 = DeleteQuestTimer**（任务成功删定时器，语义自洽），并非十六进制
   0x600。0x258 槽两宿主均命名 DeleteQuestTimer。
2. 前报 ItemPlay 0x3F0 的 "`(+0x5D8)`"：FUN_180c45fe0（C:2069980）里 `lVar1 = *param_1` 后
   `call [lVar1+0x5d8]`，第三参 `&DAT_18123caa8` 在 DLL 自己的 .rdata —— **接收者是 DLL 侧对象
   （IOneQuestScriptNpc 体系），不是宿主 IUserImp**；+0x5D8 不是 IUserImp 槽（133 槽止于 +0x420）。

## 第 2 章：FUN_180c42ba0 与 hash 节点 +0x14（前报 §5.2 → 已还原）

### 2.1 真实参数表（3 实参，r9 未用）

```
FUN_180c42ba0(rcx = hash_map 头（mgr+0x4E8 hashB 或 mgr+0x530 hashC）,
              rdx = &out {节点指针@+0, bool inserted@+8},      // 返回值写回
              r8  = &pair {int key@+0, int val@+4})            // 8 字节按值对
```

反汇编：`mov rdi,r8; mov r15,rdx; mov rsi,rcx`（0x180c42bc2-bc8）→ `call 0x180cb52f0`（对 r8 哈希）→
桶数组 `[rsi+0x10]`、桶槽数 16 字节（`shl r14,4`）。Ghidra 的"3+1 参"中第 4 参是未使用的 r9 残留。

### 2.2 节点内存布局（0x18 字节，分配器 FUN_180c4f4e0）

```
+0x00  prev 链指针
+0x08  next 链指针
+0x10  key   (dword)   ← pair[0]
+0x14  value (dword)   ← pair[4]   ★ expectedStep 落点
```

写入口 = FUN_180c42650（0x180c42650-0x180c42684）：`[rax+0x10] = dword[rbx]`、`[rax+0x14] = dword[rbx+4]`
（rbx = r9 = pair 指针）。查找比较同样用 `cmp dword[rdi],[node+0x10]`（0x180c42c0f）。宿主读者
NP/fun/fun_041.cpp:1449/1472 取同一节点传给 fp 的第 4/5 参（questid, expectedStep）。

### 2.3 两个调用点的步号来源（全 DLL 仅此 2 个 E8 调用点）

| 调用点 | 容器 | pair 栈位 | key | step | 处理函数登记 |
|---|---|---|---|---|---|
| **0x180c4e7c3**（加载器 case 5 / Pvp） | `lea rcx,[rip+0x3ad2218]`→**0x1847209C8**=mgr+0x4E8（hash B） | `key@[rbp-0x80]`（`mov [rbp-0x80],ecx`，ecx=`*r12`）、`step@[rbp-0x7C]`（`mov [rbp-0x7C],r15d`） | `*r12` | `r15d = dword[rbp+0xE8]`（步循环计数） | `DAT_184720a08 = FUN_180c46980`（cmove 选择，0x180c4e7a5/e7b8） |
| **0x180c4ebe8**（加载器 case 2 / Hunt） | `lea rcx,[rip+0x3ad1e34]`→**0x184720A10**=mgr+0x530（hash C） | `key@[rsp+0x70]`、`step@[rsp+0x74]`（0x180c4ebc8/ebdc） | `*r12` | `r15d = dword[rbp+0xE8]` | `DAT_184720a50 = FUN_180c46020`（0x180c4ebc4/ebe1） |

结论：**步号不经过独立参数**——加载器把 `{key, step}` 组成 8 字节栈上 pair（step 恰在 key+4），
FUN_180c42ba0 经 FUN_180c42650 将 pair 原样拷入节点 +0x10/+0x14。前报 local_154/local_144 即这对栈槽在
Ghidra 帧里的别名。

## 第 3 章：事件 tag 语义（前报 §5.1 → 机制改写 + 部分定名）

### 3.1 前报假设证伪 + 真实机制

- **IAIScriptNpcImp::vftable 实测 @0x18123C0A0**（.rdata；[-1]=0x1813c5910，RTTI 解析出
  `.?AVIAIScriptNpcImp@@`），**只有 4 槽**，槽 4 起即邻接字符串数据。注册 thunk **不在其中**。
  - slot[0] 0x180cb4d40：挂接/替换子脚本（写 +0xA8，重算 +0x404 起 102 项标志 → FUN_180cb4c50 聚合
    +0x8CC..0x8DC）
  - slot[1] 0x180cb4e50：换入 0x330 字节脚本状态块并转调 slot[0]
  - slot[2] 0x180cb3f70：执行第 idx 号"AI 模式组"（FUN_180cb3f00/FUN_180cb3d00 求值，日志串
    `"---> Pattern(%s, p:%d) is satisfied with %d conditions"`）
  - slot[3] 0x180cb3cb0：slot[2] 失败则直呼成员函数 +0x404+idx*8
  - 该类 = 20,999 个数据驱动 AI NPC 类的基类（DLL/classes/NPC/IAIScriptNpcImp.cpp 每函数一个类初始化器，
    类名如 "1011_Mercenary_Chief"、"ADrGuard_PeB_WarpA"）。
- **真实注册机制**：tag 薄包装（0x180cb2b40~0x180cb30d0，寄存器透传）→ tag 守卫注册器 → 容器。包装的
  调用者是**每 handler 一个的 0x20 字节小存根**（如 0x1805d5be0：`xor edx,edx; lea r8d,[rdx+0x15];
  mov r9d,0x11e52570; call 0x180cb2f80`），存根地址出现在 **.rdata 静态初始化表**（实测 qword：
  0x1805d5ff0→0x1811325e0、0x1805d5b80→0x181132aa8、0x1805d5be0→0x181131670）⇒ **DLL 加载时自注册**。
  "无静态 xref"之谜解开：xref 是存根 E8 调用 + 初始化表 qword，非 vtable。

### 3.2 注册面 × 分发面 全景（本次实测 xref 数）

| tag | 容器 | 注册器（守卫函数） | 包装 | 包装调用数 | 宿主活分发点 | 判定 |
|---|---|---|---|---|---|---|
| 5/6/7 | mgr+0x268+tag*0x10 | FUN_180c4dfd0 | 0x180cb2eb0 | **384** | SV User.cpp 等（前报已列） | 活 |
| 8 | mgr+0x258 | FUN_180c4dce0 | 0x180cb2bf0 | 3（0x181073c0c 等内联） | SV direct vec 走法 | 活 |
| 9 | mgr+0x3F8 | FUN_180cb4610 | 0x180cb2e90 | **0** | 仅死代码 SV 0x140092680 | **未用** |
| 10 | mgr+0x408 | FUN_180cb42d0（日志 "RegisterCutSceneEventHandler"） | 0x180cb2b40 | **43** | 仅死代码 SV 0x140092700 | 注册活/分发死 |
| 0xB | hash A mgr+0x4A8 | FUN_180cb47d0 | 0x180cb2ef0 | **0**（且插入器 FUN_180cb2770 唯一调用者就在 47d0 内部 0x180cb4866） | NP PacketDie 走 hash A（fun_040.cpp:4838） | **表恒空**：走空表 |
| 0xD | mgr+0x448 | FUN_180cb49c0（日志 "RegisterMovieEventHandler"） | 0x180cb2f40 | 6 | 仅死代码 SV 0x1400927f0 | 注册活/分发死 |
| 0xF | mgr+0x458 | FUN_180cb4ae0 | 0x180cb3030 | **54** | 仅死代码 SV 0x140092870 | 注册活/分发死 |
| 0x10 | mgr+0x428 | FUN_180c4e1b0 | 0x180cb2ff0 | 83 + 加载器直调 | **SV User.cpp:103908（User::InvokeQuestUpdatedEventScript）** | 活 |
| 0x11 | mgr+0x438 | FUN_180c4e100 | 0x180cb2fc0 | **0** | — | **未用** |
| 0x12 | mgr+0x468 | FUN_180c4d890 | 0x180cb2bc0（0 调用；加载器直调 0x180c4e619/0x180c4ec3b） | — | SV（EnterWorld，前报） | 活 |
| 0x13 | mgr+0x478 | FUN_180cb48b0 | 0x180cb2f20 | **0** | 仅死代码 SV 0x140092980 | **未用** |
| 0x14 | mgr+0x498 | FUN_180cb44f0 | 0x180cb2b80 | 6 | 仅死代码 SV 0x140092a00 | 注册活/分发死 |
| 0x15 | mgr+0x488 | FUN_180cb43f0 | 0x180cb2f80 | 1 | **NP FUN_1401f5020 内联（PacketResurrect）fun_040.cpp:4973** | 活 |
| hashB/C | mgr+0x4E8/0x530 | FUN_180c42ba0 | — | 2（0x180c4e7c3/0x180c4ebe8） | NP fun_041.cpp:1354/1472 | 活 |

死代码判定方法：对 0x140092680/700/7f0/870/980/a00 六函数做了 E8 call、E9 jmp（含 ILT 跳板二跳）、
RIP 相对 LEA、imm32、imm64、全文件 qword 六种扫描，全部 0 命中；并用已知有调用者的 FUN_140203960
（3 命中）/FUN_1401e6750（1 命中）做阳性对照验证扫描器。函数体确在二进制内（0x140092680 处实测
`mov r10,[r11+0x3f8]`）——链接器保留的不可达分发器。

### 3.3 语义名与置信度

| tag | 语义 | 置信度 | 证据 |
|---|---|---|---|
| 10 | **CutScene 播放事件**（key=cutscene id：0x114/0x128/0x38e/0x38f/0x390/0x391/0x393/0x39b/0x39c/0x39f/0x3a0…43 个） | 确定 | 守卫函数日志串 `L"RegisterCutSceneEventHandler, %d already registered"`（C:2145230 区） |
| 0xD | **Movie 播放事件**（key=movie id：0x1b/0x21/0x22…） | 确定 | 日志串 `L"RegisterMovieEventHandler, %d already registered"`（C:2145589 附近） |
| 0x15 | **玩家复活事件**（key=世界 id；ctx 事件号=0x15） | 确定 | ①NP PacketResurrect 处理函数 FUN_1401f5020 内联分发（fun_040.cpp:4973，`local_res20=0x15`、key=`*(u32)(user世界对象+0x24)`、IUserImp_ctor 后逐 entry `fp(&wrapper,&ctx)`）；②handler FUN_180c85170（C:2111692）：`(+0x340=GetCurBaseWorldNum)()==0x11e52570` 后按 `(+0x60=GetRace)` 给 `(+0x270=AddSkillEffect)(0x4b14或0x4b46,1)` |
| 0x14 | **世界维度任务推进事件**（key=世界 id，与 0x15 同一 key 空间 0x11E-----：0x11e350b0/0x11e4d750/0x11e859c0；id=任务 id 0xe86/0x126e/0xe8d/0x1275/0x778c/0x7796；handler FUN_180ef4910 等 → `FUN_180cacbf0(questid,user,0,1,3或6,2,0)` 推进到指定状态） | key 维度=世界：中（handler 用 GetCurBaseWorldNum 同维度比较）；**具体宿主触发场景：还原不了**（分发器死代码） | 注册调用 1940844-1940899；handler C:2452357 |
| 0xF | **通用计数进度事件**（handler 模板：`(+0xD0 GetQuestState)==3` 且 counter<10 → `(+0xF0 SetQuestProgress)(counter+1)`，满 10 `(+0x100 SetQuestSuccess)`；id=任务 id 0x4a82/0x4a83/0x7192/0x7193…；key=事件维度 0x7d7/0x1175/0xa0c，维度本体不明） | 计数语义=确定（handler 代码）；宿主触发场景=**EVIDENCE_MISSING**（分发器死代码） | C:2458995-2459060 |
| 0xB | 注册 API 名义 = 按 NPC/怪物 tplid 的 hash A 事件（前报结论的结构成立），但本 build **零注册调用 ⇒ hash A 恒空**，PacketDie 的 hash A 遍历永不命中 | 结构=确定；触发场景=未用 | xref 全扫（§3.2 表） |
| 9 / 0x13 | 注册包装 0 调用、分发器死代码 —— 本 build 完全未接线 | — | xref 全扫 |

SV 六个死分发器（0x140092680/700/7f0/870/980/a00）同型：`map<key,vector<Entry>>` 二分查 key → 取节点
+0x28 vector → `FUN_140a1c4f0(vec, user)` 遍历调 `fp(user, ctx{evt, entry.id})`——即它们就是 tags
9/10/0xD/0xF/0x13/0x14 的**宿主触发入口**，但在本二进制中无人调用。

## 第 4 章：仍 EVIDENCE_MISSING 及原因

1. **tag 9、0x13 的语义名**：无注册（包装 0 调用）、无日志串、分发器死代码。可用证据面为零，不可还原。
2. **tag 0xF 的 key（0x7d7=2007/0x1175=4469/0xa0c=2572）对应的宿主触发场景**：唯一分发器 SV 0x140092870
   死代码，无任何调用上下文可判定是击杀 tplid、物品使用还是其他 id 空间。handler 逻辑（计数 10 次成功）
   确定，触发维度不确定。
3. **tag 0x14 的宿主触发场景**：同上（分发器 0x140092a00 死代码）；仅能确定 key=世界 id 维度、效果=任务推进。
4. **`+0x30 GetId` 返回的 `user+0x8C` 到底是 objid 还是世界号**：函数体只做 `mov edi,[rax+0x8c]`
   （0x140183a56），无字符串/日志佐证字段语义；它影响对 Hunt/Pvp handler 中 `+0x30(user)==param_3` 闸门
   的解释（前报把 param_3 称 mapId）。
5. **tag 0x15 的 key 0x11E52570 对应的世界名明文**：需要该 build 的世界名→hash（0x1003F 系）清单或
   运行时数据才能反推明文。
6. 前报遗留 ③（0x22 字节击杀上下文逐字段）与 ④（FUN_180c4dfd0 event5 key 解释）不在本轮范围，仍缺。

**结构层面本轮已无缺口**：IUserImp 133 槽两宿主全展开、FUN_180c42ba0 参数/节点布局/双调用点全还原、
tag 注册-分发全链路（含死代码区）xref 全扫描闭合。
