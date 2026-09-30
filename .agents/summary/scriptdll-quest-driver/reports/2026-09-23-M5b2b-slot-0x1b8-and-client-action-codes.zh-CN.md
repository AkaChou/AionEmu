# M5-b2b 槽 0x1b8 反查 + 采集族客户端动作码（进行中）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-23
- 目标（活动 goal 第 1/3 项）：反查真端任务脚本 DLL 里每个任务的跳板槽 `0x1b8`
  —— 它**接受哪些客户端动作码**、**怎么写进度**（`0xd0/0xf0/0x100`）；并把 XML 里的
  `1003/1004/20001/10000/31` 续页、`REWARD/1` 投影、`dialogId=10255` 逐条定性为"真端确有 / XML 历史错误"。

## 1. 槽归属（已确证）

跳板形态（720 个任务共用一个槽）：

```c
/* ScriptDLL64.c:2226723  FUN_180d46660  ← 任务 1103(0x44f) 的跳板 */
uVar2 = (*(code *)**(undefined8 **)(param_2 + 0x40))(param_2 + 0x40);
lVar1 = *param_1;                                     /* 宿主侧对象虚表 */
puVar3 = (*(code *)**(undefined8 **)(param_2 + 0x18))(param_2 + 0x18);
puVar4 = (*(code *)**(undefined8 **)(param_2 + 0x38))(param_2 + 0x38);
puVar5 = (*(code *)**(undefined8 **)(param_2 + 0x48))(param_2 + 0x48);
(**(code **)(lVar1 + 0x1b8))(param_1, uVar2, 0x44f, *puVar5, *puVar4, *puVar3);
```

- DLL 内 RTTI 虚表普查（31 张）最大 20 槽（`SimpleUseItemQuest`/`SimpleItemPlayQuest`），**没有 56 槽虚表**
  → `param_1` 不是任务类实例，而是**宿主侧对象**（工具：`m5b2_vtable_scan.py`）。
- 新增通用扫描器 `re_vtable_scan.py`（DLL/EXE 通用）扫宿主 `Server64.exe`（1192 张 RTTI 虚表）：
  - `.?AVIUserImp@@` 133 槽，**slot 55 (+0x1b8) = 0x1404dbc10**
  - `.?AVUser@@` 201 槽，slot 55 = `User_SendAttackToNpcServer`（说明必须按类定槽，不能只看偏移）
  - `.?AVNpc@@` 199 槽，slot 55 = `0x140b35120`
- 同地址邻域（`Server64`，`fun/fun_050.cpp`）已有强相关命名：
  `NpcScriptMgr_GetItemCollectingProgress`(0x1404d98b0)、`IUserImp_RemoveAllCollectItems`(0x1404dc2f0)、
  `IUserImp_RemoveQuestRewardCheckItems`(0x1404dbe90)、`IUserImp_RemoveAllQuestRewardCheckItems`(0x1404dc090)。

→ 结论：跳板确实是对**玩家对象**调用虚槽 `+0x1b8`（见 §3 的独立证明链），但
`0x1404dbc10` **不是"采集任务进度"接口**（§3.3 纠偏），`+0x1b8` 只是 IUserImp 虚表里的一个
客户端通知槽，采集/击杀进度写入在别处。

## 2. 客户端动作码（已确证，客户端口径）

数据源：`docs/quest/client-dialog-mapping/legacy-quest-dialog-template-index.csv`
（由真端 compact 任务脚本 + 5.8 客户端 HTML 派生的客户端出口索引），工具 `m5b2b_collect_action_codes.py`。

| 范围 | 行数 | open action | check action | reward page |
|---|---:|---|---|---|
| 全表 `template_type=item_collecting` | 1950 | **31 `QUEST_SELECT`** ×1950 | **39 `CHECK_COLLECTED_ITEMS`** ×1950 | 5 `SHOW_SELECT_QUEST_REWARD_WINDOW1` ×1745；2375 `SELECT5` ×1743；4762 `SELECT_NONE` ×207 |
| 本仓库 SimpleCollectItem 族（178 任务） | 218 | 31 ×218 | 39 ×218 | 5 ×218（无例外） |
| 其中已退役 86 | 86 | 31 ×86 | 39 ×86 | 5 ×86 |

→ **采集族的客户端动作码只有两个：`31`（打开/报告）与 `39`（交付物检查）**，整族零例外。
→ 与引擎枚举对齐（`QuestDialogAction`）：31=`QUEST_SELECT`、39=`CHECK_USER_HAS_QUEST_ITEM`、
  20002=`CHECK_USER_HAS_QUEST_ITEM_SIMPLE`、1009=`SELECT_QUEST_REWARD`、1008=`FINISH_DIALOG`、
  10255=`SET_SUCCEED`、10000=`SETPRO1`、2716=`SELECT6`、2375=`SELECT5`、1011=`SELECT1`。
  即 XML 里出现的 `1009/20002/1008/10` 属**引擎侧别名/族默认**，不是客户端多出来的动作码。

## 3. 跳板是谁在调谁（已确证，含对 §1 的纠偏）

### 3.1 事件处理器注册表：`对象 + 0xa8 + 事件码 × 8`

真端每个任务脚本在 DLL 里对应一个**静态任务对象**（BSS 全局），构造时写入
`(NPC 名, 任务 ID)` 与一张 **102 槽的处理器表**：

```c
/* ScriptDLL64.c:2146378  FUN_180cb5920 —— 任务对象构造 */
*param_1 = IOneQuestScriptNpc::vftable;
puVar4 = param_1 + 0x15;                 /* 字节偏移 0xa8：处理器表起点 */
lVar5 = 0x66;                            /* 102 个槽 */
do { *puVar4 = 0; *(int *)(puVar1) = -1; ... } while (lVar5 != 0);
```

```c
/* ScriptDLL64.c:1003528  —— 任务 1103 的对象 + 事件码注册 */
FUN_180cb5920(&DAT_1848e4d90, L"Mires", 0x44f);                        /* 构造：NPC=Mires，quest=1103 */
FUN_180cb2ac0(&DAT_1848e4d8d, &DAT_1848e4d90, 0x35, FUN_180d46660, 0); /* 事件码 0x35 → 跳板 */
FUN_180cb2ac0(&DAT_1848e4d86, &DAT_1848e4d90, 0x1d, &LAB_180cb5a70, 0); /* 事件码 0x1d */
/* …同对象共注册 13 个事件码：0x1c 0x1d 0x1e 0x26 0x32 0x35 0x36 0x37 0x38 3 … */
```

```c
/* ScriptDLL64.c:2143904  FUN_180cb2ac0 —— 注册函数本体（表写在"第 2 个参数"上） */
/* 反汇编：movsxd rax,r8d ; mov [rdx + rax*8 + 0xa8], r9 ; mov rax,rcx ; ret */
*(undefined8 *)(param_2 + 0xa8 + (longlong)param_3 * 8) = param_4;
```

- 事件码 `0x35` 全 DLL 注册 **736** 次；其中 **702** 次的处理器体是同一形态的跳板
  （`grep -c "(code \*\*)(lVar1 + 0x1b8))(param_1,"` = 702），与 §1 的"720 个任务共用"量级一致。
- 跳板形态确认（示例 `FUN_180d46660`，任务 1103）：

```c
uVar2  = (*(code *)**(undefined8 **)(param_2 + 0x40))(param_2 + 0x40);
lVar1  = *param_1;                                     /* param_1 的虚表 */
puVar3 = (*(code *)**(undefined8 **)(param_2 + 0x18))(param_2 + 0x18);
puVar4 = (*(code *)**(undefined8 **)(param_2 + 0x38))(param_2 + 0x38);
puVar5 = (*(code *)**(undefined8 **)(param_2 + 0x48))(param_2 + 0x48);
(**(code **)(lVar1 + 0x1b8))(param_1, uVar2, 0x44f, *puVar5, *puVar4, *puVar3);
```

### 3.2 `param_1` 是玩家对象（IUserImp），不是任务对象、也不是 NpcScriptMgr

三个候选逐一排除，并给出正向证明：

| 候选 | 判据 | 结论 |
|---|---|---|
| DLL 侧任务对象（`SimpleCollectQuest` 等，vtable 为多子对象拼接，首段仅 19 槽） | 任务对象的首字段是"类描述符"（`IOneQuestScriptNpc::vftable` = `0x18123d3b8`，其后紧跟 `L"list<T> too long"` 等字符串），`+0x1b8` 落在字符串上 | ✗ |
| 宿主接口对象 `NpcScriptMgr` 单例（`Server64` `0x14f4395a0`，`GetInterface` 时传入并保存在 DLL `DAT_184720398`） | `NpcScriptMgr::vftable` = `0x141315238`，**整表只有 10 槽**，`+0x1b8` 越界 | ✗ |
| **玩家对象 `IUserImp`（133 槽）** | 同一事件族的其它处理器把 `param_1` 当玩家用：见下 | ✓ |

正向证明（`Server64` `FUN_180f045d0`，一段按任务 ID（`0x5df4`）驱动的任务脚本处理器）：

```c
pcVar3 = (**(code **)(*param_1 + 0xd0))(param_1, local_res8, 0x5df4); /* +0xd0 = GetQuestState   */
iVar2  = (**(code **)(*param_1 + 0xe8))(param_1, 0x5df4);             /* +0xe8 = GetQuestProgress*/
(**(code **)(*param_1 + 0xf8))(param_1, 0x5df4, 4);                   /* +0xf8 = SetQuestSuccess */
(**(code **)(*param_1 + 0x358))(param_1, 0x5df4, 0x39188, 1, 0, …);   /* 发奖/物品接口           */
```

`+0xd0 / +0xe8 / +0xf0 / +0xf8 / +0x100` = `GetQuestState / GetQuestProgress / SetQuestProgress /
SetQuestProgressMemoryOnly / SetQuestSuccess` —— 这正是 `IUserImp` 的槽 26/29/30/31/32
（§1 的虚表普查同一份数据）。**任务脚本处理器拿到的是玩家对象**，任务进度就是在这几个槽上读写的。

### 3.3 纠偏：`+0x1b8` 不是"采集进度接口"

- `IUserImp` 槽 55（`+0x1b8`）= `0x1404dbc10`，实现体只做一件事：构造 0x16 字节的
  `0x1eb / 0x56 / 0xfe14` 报文并交给 `User_PlayCutScene`（`renames.tsv:3810` 为人工命名的
  `User::PlayCutScene`）。
- 同族处理器（如 `FUN_180feaa70`）先把 `uVar2` 交给 `+0x5d8`，随后调用 **`+0x1c0`**（槽 56，
  `0x1404dbc70`）——`+0x1b8` 与 `+0x1c0` 是相邻的两个同族槽，属于同一组客户端通知接口。
- `FUN_180c4cd50`（脚本动作分发器）的 `case 4` 也以 `(*(*param_2 + 0x1b8))(param_2, id)` 单参形式
  调用同一槽，说明该槽在脚本动作语义里是**"播片/客户端演出"类动作**，而不是进度写入。

→ 所以 §1 里"采集族跳板 = 采集任务进度接口"的推断**作废**：`0x1b8` 是玩家对象上的客户端演出通知槽。
**采集/击杀的进度写在 `+0xd0/+0xe8/+0xf0/+0xf8/+0x100` 上**（§3.2），由**其它事件码**
（`0x1c/0x1d/0x1e/0x26/0x32/0x36/0x37/0x38`）的处理器完成；事件码 `0x35` 只是同批注册中的
一个演出类事件。事件码到语义的完整映射见 §4。

### 3.4 对判定面的影响

- 原先想用"槽 `0x1b8` 接受哪些客户端动作码"来给 XML 续页（`1003/1004/20001/10000/31`）定性，
  **这条路作废**：该槽不参与进度判定。
- 正确的判定路径改为：**按事件码族看每个任务的处理器体**（哪些事件码存在、处理器里读了哪些
  任务状态、写了哪些进度、发了哪些客户端通知），再与客户端 `QUEST_Q<id>.html` 实际出口三方对齐。

## 4. 事件码 → 语义（家族差异法 + 组合结构，附工具）

### 4.1 方法：单任务看不出来，家族差异与组合结构能看出来

新增工具 `.agents/summary/scriptdll-quest-driver/m5b2b_handler_slot_scan.py`：把
"任务 → (NPC/物件, 事件码, 处理器, 处理器调用的玩家对象虚槽)"逐条摊平
（全表 **80102 行 / 7043 个任务对象**，`m5b2b-quest-event-census.tsv`，2.0 s 跑完）。

再用 `.agents/summary/scriptdll-quest-driver/m5b2b_event_family_crosstab.py` 把真端 7 个家族表
与事件码做交叉表（`m5b2b-event-family-crosstab.tsv`）。

> **工具缺陷与修正（保留记录）**：首版正则只匹配 `FUN_xxxx` 形态的处理器，漏掉 `&LAB_xxxxx`，
> 使普查只有 52117 行并把 `0x1c/0x1d/0x32/0x36/0x37/0x38` 整批漏掉；由子代理 Boyle 独立列出的
> "1103 共 11 条注册"与首版结果对不上而暴露。修正正则后重算（80102 行）。
> **本节数字全部为修正后口径**，首版"0x37 是合成专用"的结论作废。

### 4.2 家族覆盖率矩阵（修正后）

```text
event   SimpleHunt SimpleCollect SimpleTalk CombineTask SimpleUseItem SimpleItemPlay SimpleSerialHunt
0x3           0.66        1.00     0.99        1.00        1.00          1.00          0.00   ← 交互
0x11          1.00        0.00     0.00        0.00        0.00          0.00          1.00   ← 击杀
0x1c          0.86        0.81     0.92        1.00        0.00          0.95          1.00   ┐
0x1d          0.86        0.81     0.92        1.00        0.00          0.95          1.00   ├ 三元组
0x26          0.86        0.81     0.92        1.00        0.00          0.95          1.00   ┘
0x32          0.98        0.97     0.98        1.00        0.98          1.00          1.00   ← 近乎全员
0x33          0.03        0.02     0.12        1.00        0.00          0.79          0.19   ← 合成
0x35          0.06        0.13     0.15        0.00        0.21          0.21          0.00   ┐ 成对
0x1e          0.06        0.13     0.15        0.00        0.21          0.21          0.00   ┘
0x36          0.00        0.97     0.63        1.00        0.03          0.00          0.00   ┐
0x37          0.00        0.97     0.63        1.00        0.03          0.00          0.00   ├ 三元组
0x38          0.00        0.97     0.63        1.00        0.03          0.00          0.00   ┘
```

### 4.3 组合结构（7043 个任务对象上的同现一致性）

| 组合 | 同现一致性 | 解读 |
|---|---|---|
| `0x36` / `0x37` / `0x38` | **7034/7043 = 99.9%** | 同一次注册动作写入的三个槽，**采集族签名（97%）** |
| `0x1c` / `0x1d` | **6975/7043 = 99.0%**（含 `0x26` 95.8%） | 任务生命周期三元组（多数家族 ≥81%） |
| `0x1e` / `0x35` | **6792/7043 = 96.4%** | 成对注册，**与采集无关**（合成族 0%） |

全 DLL 最常见的注册形状（前 3）：

```text
1664  {0x3, 0x1c, 0x1d, 0x26, 0x32, 0x36, 0x37, 0x38}       ← 采集/交互主形状
 888  {0x3, 0x11, 0x1c, 0x1d, 0x26, 0x32}                  ← 击杀形状
 638  {0x3, 0x1c, 0x1d, 0x26, 0x32, 0x33, 0x36, 0x37, 0x38} ← 合成+采集形状
```

### 4.4 逐条结论（含对子代理结论的取舍）

| 事件码 | 裁定 | 证据 |
|---|---|---|
| **`0x11` = 击杀** | **确证** | 击杀两族 100%（286/286、16/16）；采集 0/261、Talk 0/3088、合成 0/574、用物品 0/154。零反例 |
| **`0x33` = 合成** | **确证** | CombineTask 574/574 = 100%；SimpleItemPlay 79%（同属物品操作）；其余 ≤19% |
| **`0x36/0x37/0x38` = 采集族签名槽组**（细分未定） | **确证为采集签名** | 采集族 **97%**、Talk 63%、合成 100%；击杀族 0%、用物品 3%。Boyle 另证 `0x37` 处理器 `FUN_180ee5e70` 调玩家槽 `+0x100`（注册行 `ScriptDLL64.c:1934280`） |
| **`0x1c/0x1d/0x26` = 生命周期三元组**（细分未定） | **确证为三元组** | 同现 99.0% / 95.8%；只有 SimpleUseItem 一族缺 `0x26` |
| **`0x32`** | 近乎全员槽（97–100%） | 采集 97%、击杀 98%；Boyle 证 `FUN_180f579a0` 用它读**玩家 `+0xe8`（任务进度）**（`ScriptDLL64.c:2510726`） |
| **`0x1e` + `0x35` = 成对槽** | **成对事实确证；语义定为"客户端通知/演出"** | 同现 96.4%；`0x35` 处理器调玩家 `+0x1b8`（§3.3 客户端通知槽）。**子代理"0x35 = 采集进度"的判定不成立**：采集族只有 13% 注册 `0x35`，而 Talk 15%、用物品/物品操作 21% —— 它在非采集任务里更常见 |
| `0x3` = NPC/物件交互 | 高置信 | 采集/对话/用物品/物品操作 ≈100%，击杀 66%，SerialHunt 0% |

**本轮最有价值的结论**：`0x11` = 击杀（家族级零反例）；采集族"必有集合"是
`{0x3, 0x32, 0x36, 0x37, 0x38}`（97–100%），**不是 `0x35`**。

### 4.5 宿主侧消费者（子代理 Boyle 证据，已并入）

`NPCSvr64.exe` 的 `NpcScriptMgr::RegisterQuest`（`NPCSvr64.c:466532–466556`）遍历对象 `+0xa8` 表的
**0…0x65 共 102 槽**，把每个非空槽连同 `+0x3d8` 的 meta 注册成"**该 NPC 关心的事件码**"，
并置 `npc+0xb0+idx` 的兴趣字节；同一函数前段用 `+0x570`（步骤数）/`+0x578`（步骤表）/
`+0x574`（questId）迭代任务步骤。

→ **`+0xa8+idx*8` 的 idx 就是宿主事件码**；这张表不是 DLL 内部结构，而是**给 NPC 服务器订阅用的接口**。

### 4.6 具体任务的注册形状（工具输出，非人工整理）

```text
quest 1103 (Mires)              obj=1848e4d90 事件码 0x3 / 0x1c / 0x1d / 0x1e / 0x26 / 0x32 / 0x35 / 0x36 / 0x37 / 0x38
quest 1103 (LF1_Cherubim_pouch) obj=1848e53d0 事件码 0x3
quest 1125 (Kalio)              obj=…         事件码 0x1c / 0x1d / 0x26 / 0x32 / 0x36 / 0x37 / 0x38（**无 0x35**）
```

要点：

1. **任务对象按 (NPC/物件, 任务 ID) 建，不是按任务建**——同一任务可能有多个对象
   （1103 = Mires + 采集物 LF1_Cherubim_pouch；14150 = Sarantus/Ibelia/LF3_FOBJ_Q14150）。
2. 采集物上的处理器 `FUN_180d40180` **跨任务共用**（1103/1136/14150 的物件都用它），
   任务差异靠对象里存的 `(NPC 名, 任务 ID)`。
3. 事件码 `0x26` 的处理器多为"每任务一个 stub + 任务 ID 常量"，例如
   `FUN_180d3bf90(){ FUN_180cab520(0x44f, param_1, param_2, 0, 0); }`（`0x44f` = 1103）。
4. `FUN_180cb2ad0` 的第 3 参 **`3` 不是事件码**，是 "kind=3" 分支：从槽 `0x27` 起找第一个空槽写入，
   并同时写 `+0x3d8+idx*4` 的 meta（`ScriptDLL64.c:2143909`）。

### 4.7 与 §3 的口径衔接

- 采集族的进度读写**不在 `0x35`**；公共路径是 `0x3`（物件交互）、`0x32`（读 `+0xe8` 进度）、
  `0x36/0x37/0x38`（采集签名）。
- 下一步定性 XML 续页（`1003/1004/20001/10000/31`）应沿 **`0x32` / `0x36` / `0x37` / `0x38`**
  的处理器链读 **`+0xe8`（读进度）/`+0xf0`（写进度）/`+0xf8`（写成功）/`+0x100`（成功）** 的调用点。

### 4.8 三维交叉：哨兵接取 ⟺ 缺生命周期三元组 ⟺ 客户端无 `select1`（零反例）

工具：`.agents/summary/scriptdll-quest-driver/m5b2b_triplet_start_correlation.py`
（真端契约索引 `start_npc_ids` × DLL 事件签名）与
`.agents/summary/scriptdll-quest-driver/m5b2b_client_accept_page_probe.py`
（客户端 `QUEST_Q<id>.html` 的 `HtmlPage name`）。
对 SimpleCollectItem **全 178 行**（含 40 行 `REJECTED:RETAIL_ACQUIRE_NPC_SENTINEL`）实测：

```text
二维表 A（triplet, start_is_sentinel）       二维表 B（sentinel_start, has_select1）
  triplet  & sentinel start :   0            sentinel & select1    :   0
  triplet  & real start     : 130            sentinel & !select1   :  40  ← ask_quest_accept
  no-trip  & sentinel start :  40            real     & select1    : 138
  no-trip  & real start     :   8            real     & !select1   :   0
```

三条独立证据同向（全部零反例）：

1. **40 个哨兵行的 DLL 事件签名完全同形**：40/40 = `{0x3, 0x32, 0x36, 0x37, 0x38}`，
   **无** `0x1c/0x1d/0x26` 三元组；而 138 个真实接取行里 130 行（94%）带三元组。
2. **客户端接取页完全不同**：40 行全部只有 `ask_quest_accept`（**没有** `select1`/ACCEPT/REFUSE 对），
   138 个真实接取行 100% 有 `select1`。样本：`QUEST_Q35007.html`（哨兵）
   = `ask_quest_accept, quest_complete, quest_summary, select5, select6, select_acqusitive_…`；
   `QUEST_Q1103.html`（真实）= `ask_quest_accept, quest_accept_1, quest_refuse_1, quest_summary, select1, …`。
3. **真端契约索引** `start_npc_ids` 对同 40 行为哨兵 `0`。

→ 裁定：`REJECTED:RETAIL_ACQUIRE_NPC_SENTINEL` **不是解析缺口，而是另一类任务形状**
（"无 NPC 对话接取页 + 无任务生命周期三元组"）。合成器用主形状拒绝这 40 行是正确的，
它们若要被驱动，需要新增一种"直接接取"形状（或维持 `KEEP_XML`，见 §5.2）。

**反向留痕（不掩饰）**：三维交叉并不互推——`no-trip & real start = 8`
（1125 / 1136 / 1154 / 1219 / 2116 / 2119 / 2203 / 2497），其中 2 行（1125/1154）与哨兵行同签名
`{0x3,0x32,0x36,0x37,0x38}`、5 行只有 `{0x3}`、1 行（2119）**无任何事件注册**；
同理 `sentinel ⇒ no-triplet` 是 100% 蕴含，但 `no-triplet ⇒ sentinel` 只有 40/48 = 83%。
所以"缺三元组"只能作为**形状证据**，不能单独当成"接取 NPC 解析失败"的判据。

**已退役 86 行的签名分布（同口径复核，供后续对拍）**：

```text
64  {0x3, 0x1c, 0x1d, 0x26, 0x32, 0x36, 0x37, 0x38}            ← 主形状
11  {0x3, 0x1c, 0x1d, 0x1e, 0x26, 0x32, 0x35, 0x36, 0x37, 0x38} ← + 成对通知槽
 5  {0x3}                                                       ← 1136/1219/2116/2203/2497
 2  {0x3, 0x32, 0x36, 0x37, 0x38}                               ← 哨兵签名
 1  -（无注册）
 1  {0x3, 0x1c, 0x1d, 0x26, 0x32, 0x33, 0x36, 0x37, 0x38}       ← + 合成槽
 1  {0x3, 0x1c, 0x1d, 0x1e, 0x26, 0x32, 0x33, 0x35, 0x36, 0x37, 0x38}
 1  {0x3, 0x1c, 0x1d, 0x26}
```

→ 即：**退役集合里也有 8 行（5 行 `{0x3}` + 2 行哨兵签名 + 1 行无注册）不属于主形状**。
它们被退役的依据是"逐行 IR 与真端表等价"（M5-b2/M5-b3 的冻结指纹），不是"签名同形"，
两条判据互不替代——签名只用来**分类任务形状**，等价性仍由 IR 对拍证明。

### 4.9 推广验证：`ask_quest_accept` 是**家族无关**的"自动接取"形状（跨 7 家族）

新工具：

- `.agents/summary/scriptdll-quest-driver/m5b2b_family_ids.py`：从真端家族表（`<id id="NNNN">`）导出
  "首列 = quest_id"的 ID 清单，供探针复用（7 个家族：CombineTask 574 / SimpleCollectItem 262 /
  SimpleHunt 1865 / SimpleItemPlay 43 / SimpleSerialHunt 16 / SimpleTalk 3152 / SimpleUseItem 160）；
- `.agents/summary/scriptdll-quest-driver/m5b2b_family_accept_crosstab.py`：把各家族
  `m5b2b-client-accept-page-<Family>.tsv` 汇总成跨家族交叉表（→ `m5b2b-family-accept-crosstab.tsv`）。

**先修一个计数缺陷**：首版探针把"无契约行"（`start_npc_ids = ?`）当成"真实 NPC"计入，导致
SimpleSerialHunt 出现 16 vs 10 的口径矛盾。已改为三态（`sentinel / real / no-contract`）并单列计数；
下表为修正后口径。

```text
family              rows   三元组×客户端接取页（无 HTML 已单列）
CombineTask          574   triplet & NO_HTML 574                       ← 全族无客户端任务书
SimpleCollectItem    262   triplet & select1 211 | no-trip & ask_accept 43 | no-trip & select1 7 | no-reg & select1 1
SimpleHunt          1865   triplet & select1 1586 | no-trip & ask_accept 144 | no-trip & select1 123 | triplet & ask_accept 4 | NO_HTML 8
SimpleItemPlay        43   triplet & select1 41 | no-trip & ask_accept 2
SimpleSerialHunt      16   triplet & select1 16
SimpleTalk          3152   triplet & select1 2835 | no-trip & select1 149 | no-trip & ask_accept 100 | no-reg & select1 63 | NO_HTML 5
SimpleUseItem        160   no-trip & ask_accept 154 | no-reg & ask_accept 6     ← 全族 0 个 select1
------------------------------------------------------------------------------------------------------
跨家族合计（不含 NO_HTML）
  triplet          & select1          4689
  triplet          & ask_quest_accept    4   ← 唯一反例：13912/13913/23912/23913（均无契约行）
  no-triplet       & ask_quest_accept  443
  no-triplet       & select1           279
  no-registration  & ask_quest_accept    6
  no-registration  & select1            64
```

**语义锚点（客户端原文）**：`ask_quest_accept` 页的 `<Selects>` 只有
`<Act href="HACTION_FINISH_DIALOG">把委托书收起来</Act>`（对比有 `select1` 的任务多出
`quest_accept_1`/`quest_refuse_1`）。即 `ask_quest_accept` = **委托书/公告正文页 + "收起来"按钮**，
没有 ACCEPT/REFUSE 选择——这正是"任务不是通过 NPC 对话接取"的客户端表现。

**方向性不变量（零反例方向 + 明确反例方向）**：

| 方向 | 计数 | 结论 |
|---|---|---|
| `triplet ⇒ select1` | 4689 / 4693 = **99.9%** | 有生命周期三元组 ⇒ 客户端有 ACCEPT/REFUSE 页 |
| `ask_quest_accept ⇒ 无 triplet` | 443 / 453 = **97.8%** | 自动接取 ⇒ 不注册生命周期三元组（4 个反例全在 SimpleHunt 且无契约行） |

**家族级事实**：

1. SimpleCollectItem 的 §4.8 不变量（哨兵 ⟺ 无三元组 ⟺ 无 `select1`）在**本族 178 行上是 100%**，
   但**不是全局规律**：SimpleTalk 有 61 行、SimpleHunt 有 85 行是"哨兵接取却仍带 `select1`"。
   → 哨兵（`start_npc_ids = 0`）本身不是形状判据，**必须与事件签名/客户端页联合判**。
2. SimpleUseItem **全族 160/160 无 `select1`**（用物品接取）→ 该族天然是"自动接取"形状，
   这解释了 §4.2 里 SimpleUseItem 的 `0x1c/0x1d/0x26` 覆盖率为 0。
3. CombineTask **全族 574/574 无客户端任务书 HTML** → 合成任务不产生委托书页（与 M4-a
   "无领奖行投影需求"一致）；`triplet` 却 574/574 齐全。
4. SimpleSerialHunt 16/16 为 `triplet & select1`（唯一"全员主形状"的小家族）。

**对 §5.2 待裁项的直接影响**：40 行哨兵**不是采集族专有产物**——同形状在 SimpleHunt 144、
SimpleTalk 100、SimpleUseItem 154+6、SimpleItemPlay 2 行上同样出现（合计 ≥ 443 行）。
因此口径 A（新增"自动接取"形状）的**收益面是数百行**，不是 40 行；口径 B（`KEEP_XML`）
只是把问题推给 XML，且这些 XML 本身在历史上就无法表达"自动接取"（客户端页由任务书驱动）。

> 口径提醒：`start_npc_ids` 来自**真端对话契约索引**，`?`（无契约行）**不得**计入"真实 NPC"。
> 本表所有数字均为三态修正后口径，首版两态数字（SimpleSerialHunt 16 / SimpleUseItem 56 等）作废。

## 5. 对 AionEmu 的影响 / 裁定（部分结论已可下）

### 5.1 已可下的裁定

| 项 | 结论 | 依据 |
|---|---|---|
| 任务脚本能否直接驱动"AionEmu 的进度模型" | **能**。玩家对象上的 `GetQuestState/GetQuestProgress/SetQuestProgress/SetQuestSuccess` 是任务脚本的一等 API，直接对应本仓库 `RetailQuestState`（`state/progress/branch`）三维 | §3.2 |
| 真端是否也有"客户端演出"语义 | **有**，且是独立事件族（`0x35` 等），与进度读写解耦 | §3.1/§3.3 |
| `+0x1b8` 能否作为 XML 续页的判据 | **不能**。该槽不参与进度判定，§1 推断作废 | §3.3 |
| 采集族客户端动作码 | 仍成立：`open 31 (QUEST_SELECT)` + `check 39 (CHECK_COLLECTED_ITEMS)`，本族 178 任务零例外 | §2 |
| "自动接取"形状 | **确证为家族无关形状**：`ask_quest_accept`（委托书正文 + `HACTION_FINISH_DIALOG`）⇒ 无生命周期三元组，443/453 = 97.8%；反向 `triplet ⇒ select1` 4689/4693 = 99.9% | §4.9 |
| 哨兵 `start_npc_ids = 0` 能否单独判形状 | **不能**。SimpleTalk 61 行 / SimpleHunt 85 行是"哨兵却带 `select1`" → 必须与事件签名/客户端页联合判 | §4.9 |

### 5.2 仍待补（不阻塞其它切片）

- 事件码的**细分语义**（`0x1c/0x1d/0x26` 组内谁是"接取/进行/交付"；`0x36/0x37/0x38` 组内谁是
  "采集/上交/完成"）：§4 已把**组**定死（同现 99%+、签名 97%），组内细分仍需逐处理器读。
- 每个采集任务在交付事件里**到底写了哪个 var**（`1003/1004/20001/10000/31` 续页是否真端确有）：
  沿 `0x32`/`0x36`/`0x37`/`0x38` 处理器链读 `+0xe8/+0xf0/+0xf8/+0x100` 调用点即可，属可执行的下一步。
- **40 行哨兵拒绝的处置口径待裁**（§4.8/§4.9）：已确证它们是"自动接取"形状，而非解析缺口；
  且该形状家族无关（跨族 ≥ 443 行）。可选口径 A = 新增"自动接取"驱动形状（不注册生命周期三元组）；
  口径 B = 维持 `KEEP_XML`。**倾向 A**（收益面是数百行而非 40 行），但需先裁定驱动模型如何表达
  "无 NPC 接取 + 客户端任务书页"；裁定前**不动**这 40 行。
- 因此本报告已把"跳板归属 + 事件码分组 + 哨兵形状三维交叉"闭环；**XML 续页的三方定性顺延到上一项完成后**，
  在此之前 SimpleCollectItem 家族剩余 92 行（89 稳定码拒绝 + 3 真端缺口）**保持不动**。

## 6. 回归成本降低（已落地：受影响测试反查 + T1/T2/T3 分档）

### 6.1 成本问题

切片收口目前唯一可接受的"零新增失败"证据是 `questEngine` 全树（1947 例，
`forkCount=1` 实测 **8–10 min**）。若每次改完都跑全树，一个切片 30 min 里有 20+ min 花在重复回归上；
而实际改动通常只影响极少数任务（例如 M5-b3 一批 57 个 XML，只命中 2 个任务测试类 + 7 个门禁类）。

### 6.2 三个杠杆（全部已落地）

| 杠杆 | 做法 | 实测 / 预期 |
|---|---|---|
| ① 受影响测试反查 | `affected_quest_tests.py <quest ids...>` 扫 1092 个测试类，按数字边界匹配任务 ID，直接吐出可复制的 `-Dtest=` 选择器 | **0.23 s**（性能上限 5 s，PASS） |
| ② T1/T2/T3 分档 | `run_quest_gates.sh {T1\|T2\|T3} [ids...]`：T1 = 固定 7 个全局门禁；T2 = T1 ∪ 命中类；T3 = 全树 | T1 实测 **25 例 / 25 s**（`gates/T1-223925.log`） |
| ③ 命令优化 | 单次 `mvn -o test -Dtest=...` 自带测试编译（去掉 `test-compile` 前置，省 ~25 s）；`-B` 批处理；全树用 `-DforkCount=2` 分片 | 每次省 ~25 s；全树预期 1.5–2× |

分档口径（写死在脚本头部，避免每轮重新解释）：

- **T1**：`RetailSimpleCollectItemGateTest, RetailOwnershipGateTest, RetailQuestCatalogTest,
  RetailQuestDriverOverlayTest, QuestClientContractGateTest, ProductionCatalogWhitelistVerificationTest,
  QuestDefinitionCatalogManifestTest` —— 每次改完立刻跑，25 s。
- **T2**：T1 ∪ `affected_quest_tests.py` 命中的类 —— 切片内每完成一个小步骤跑。
- **T3**：`-Dtest='com.aionemu.gameserver.questEngine.**'` —— **只在切片收口跑一次**。

T1 清单的唯一来源是 Python 工具里的 `T1_GATE_CLASSES`；`run_quest_gates.sh` 通过导入该模块读取，
不复制清单，避免两处漂移。若某个 T1 类在测试根目录解析不到，`affected_quest_tests.py` 会输出
`WARN unresolved T1 gates` 并以退出码 3 结束（防止"门禁名写错导致静默少跑"）。

### 6.3 本轮修掉的反查脚本缺陷

初版 `GATE_CLASS_KEYWORDS`（`Gate/Contract/Catalog/Manifest/Whitelist/Ownership/Overlay`）用**子串**匹配
全测试树，误捕 106 个与任务系统无关的类，例如：

- `lifecycle/GameWorldBootstrapGatewayTest`、`GameEventBootstrapGatewayTest`（`Gate` 命中 `Gateway`）
- `ai/ThresholdTransformDeathFallbackGateTest`、`controllers/attack/GetMostPlayerDamageNullGateTest`、
  `dataholders/SiegeWeaponGateAttackTest`、`model/templates/npc/NochsanaFortressGateTemplateTest`、
  `dataholders/DimensionalGateRetailSpawnTest`

修法（两层）：

1. 默认 `gates` = 上述固定 T1 七类（显式白名单），不再做关键词猜测；
2. 需要宽口径时加 `--wide-gates`，关键词扫描**限定在 `com.aionemu.gameserver.questEngine.` 包前缀内**
   （`GATE_SCAN_PACKAGE_PREFIX`），包外一律不入选。

修复前后（同一组 ID 2585/1136/2237/14150）：

| 指标 | 修复前 | 修复后（默认 T1） | 修复后（`--wide-gates`） |
|---|---:|---:|---:|
| 命中任务测试类 | 2 | 2 | 2 |
| 门禁类 | 106 | **7** | 95（全部在 `questEngine` 包内） |
| `combined` 选择器长度 | 约 8.5 KB | 687 字符 | 约 7.6 KB |

### 6.4 明确的边界（不降级、不打折）

- **T3 仍是"零新增失败"的唯一证据来源**：本方案降低的是 T3 的**执行频次**，不是取消 T3。
  每个切片收口必须留下一次 T3 日志（`gates/T3-<HHMMSS>.log`）。
- **并行只用 `-DforkCount=N`**（多 JVM 分片，进程隔离）。**不用** `-Dparallel=classes`：
  同一 JVM 内共享静态状态（生产目录、缓存），questEngine 测试树有跨类静态依赖，风险不可接受。
- 受影响反查是**包含式**（宁可多跑几个类），不是精确可达分析；它只用于把 T2 从"全树"缩到"相关类"，
  不作为"改动无影响"的证明。

### 6.5 复现命令

```bash
cd <仓库根>
# T1：改完立刻跑（~25 s）/ run right after an edit
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1
# T2：T1 + 命中类 / T1 plus affected classes
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 2585 1136 2237 14150
# T3：切片收口，2 个 JVM 分片 / slice close-out with two JVMs
QUEST_FORK_COUNT=2 .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T3
# 只要选择器、不跑测试 / selectors only
python3 -B .agents/summary/scriptdll-quest-driver/affected_quest_tests.py --json 2585 1136
```
