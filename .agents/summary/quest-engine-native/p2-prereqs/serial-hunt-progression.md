# Quest_SimpleSerialHunt（串行狩猎）推进语义还原报告

> 考古批次：2026-10-01（P1 第二横切并行后台任务；只读，真端根 /Users/mc/IdeaProjects/58Server/）
> 定位：P2（SimpleSerialHunt 切换）前置证据；回答 P0a 遗留缺口「SerialHunt 16 任务在相机调用集中 0 次——推进机制是什么」。

核心结论先行：**串行狩猎没有专属 handler。它和 SimpleHunt 走同一条对象驱动管线（同一装载器形状、同一 vtable、同一 0x11 号击杀事件槽回调）；"串行"语义是数据形态（count_first…count_fifth 词序号阶段 + talk_npcN 中继 NPC）与 NPC 链设计，而不是独立代码路径。** 击杀推进是「vars dword 顶部 6 位（bits 26-31）串行步计数器 +1，门限 `< 本阶段阶段号`」，经 +0xf0 写回、+0x118 发包；完成/交付走 +0x100 通道（由 talk 类事件而非击杀路径触发）。

---

## 1. 表数据：16 行字段形状

文件：`/Users/mc/IdeaProjects/58Server/Map/XML/Quest_SimpleSerialHunt.xml`（UTF-16，已解码）

16 行 id：9622, 41189, 30600, 30610, 13025, 23025, 13619, 23619, 18911, 18912, 28911, 28912, 16991, 26991, 13918, 23918。

与 SimpleHunt 的字段差异（`Quest_SimpleHunt.xml` 用 `count1/monster1…count5/monster5`，Serial 用词序号）：

| 列名（Serial） | 行数 | 语义 |
|---|---|---|
| `dev_name` | 16 | 开发名（9622 竟叫 "[테스트] **병렬**사냥"=并行狩猎） |
| `acquired_npc_name` | 16 | 接取 NPC |
| `count_first/monster_first` | 16/16 | 阶段1 数量/怪物表（逗号分隔多怪物） |
| `count_second/monster_second` | 16/16 | 阶段2 |
| `count_third/monster_third` | 8/8 | 阶段3 |
| `count_fourth/monster_fourth` | 6/6 | 阶段4 |
| `count_fifth/monster_fifth` | 5/5 | 阶段5 |
| `talk_npc1` | **3** | 阶段中继 NPC（9622→Rebecca_2；30600→Linocus；30610→Aluna） |
| `talk_npc2` | **1** | 仅 9622→Rebecca_3 |
| `talk_npc3` | **1** | 仅 9622→Rebecca_4 |
| `reward_npc_name` | 16 | 交付 NPC |

**没有 serial/step/chain 类显式字段。** 阶段语义完全由「第 N 组列 + talk_npcN 的有无」承载。形状上 9622 是教科书式五阶段 NPC 链：Rebecca_5 接取 → 杀1 → Rebecca_2 → 杀2 → Rebecca_3 → 杀3 → Rebecca_4 → 杀4 → 杀5 → Rebecca_5 交付。

## 2. 运行时处理链（代码考古）

### 2.1 真实数据源与主装载器

- 主装载 `FUN_18106e570`（ScriptDLL64.c:2685427）：**根标签必须 `_wcsicmp(root, L"event_quest")==0`**（:2685459），逐行取 `<type>` 分派：
  - `hunt`→`FUN_18106c7e0`（:2685521）；`collect`→`FUN_18106c510`（:2685539）；**`serial_hunt`→`FUN_1810707d0` 解析 + `FUN_18106d540` 装载（:2685597-2685600）**。
  - 路径模板：`sprintf_s(&local_118,0x104,"%squest\\event_quest.xml",param_2)`（:2685420）。**event_quest.xml 不在本仓库数据快照中**（见 EVIDENCE_MISSING #2）；Map/XML 的 Quest_Simple*.xml 是同 schema 行集。
- 列→record 偏移映射（基础解析器 `FUN_18106eb70`，serial 复用它再叠加 count/monster 列）：
  - `id`→+0x0；`acquired_npc_name`→+0x28；`give_item`→+0x68/+0x88；**`talk_npc1`→+0xd8 且 rec+0x288+=1**（:2685713-2685714）；`give_item1`→+0xf8/+0x118；`remove_item1`→+0x120/+0x140；`emotion1`→+0x148；**`talk_npc2`→+0x168**（:2685814）；…；**`talk_npc3`→+0x1f8**（:2685915）；**`reward_npc_name`→+0x290**（:2686017）；`item_check`→+0x2b0；`con_quest`→+0x2b4；`cutsceneId1`→+0x2b8；`cs1_haction`→+0x2bc；`cs1_progress`→+0x2c0；`delete_item`→+0x2c4（此后 return，:2686086）。
  - serial 专属解析 `FUN_1810707d0`（:2686627）：`count_first`→rec+0x2d0（:2686661）、`monster_first`→按`,`切分入 vector@rec+0x2d8（:2686687-2686729）、second→0x2f0/0x2f8、third→0x310/0x318（fourth/fifth 同理）。
  - **rec+0x370/0x374/0x378/0x37c 没有任何列写入**；record 由 `FUN_18106ba90`（:2683723，memset 0x380）清零 ⇒ 恒为 0。

### 2.2 对象构造（每个「阶段×怪物」一个对象）

装载器 `FUN_18106d540`（:2684812）：对每阶段的怪物 vector 逐个分配 **0x9c8 字节** 对象（:2684857），设 `SimpleSerialHuntQuest::vftable`（:2684861），调 `FUN_181074590(obj, 怪物名, rec, countN, 阶段号N)`（:2684870，参数按 1..5 阶段），再 `FUN_181074670(obj)`（:2684871），插入注册表 `DAT_1847204c8`（:2684875，键含 `{questId=*rec, obj指针}` 12 字节 memcmp，`FUN_180c44720`@:2068630）。

- `FUN_181074590`（:2689525）：先虚调用 **vtable+0x90**，然后：
  ```c
  *(undefined4 *)(param_1 + 0x138) = param_4;            // obj+0x9C0 = 该阶段 count
  *(undefined4 *)((longlong)param_1 + 0x9c4) = param_5;  // obj+0x9C4 = 阶段号(1..5)
  ```
- vtable+0x90 = `FUN_1810745d0`（:2689539）：怪物名拷到 obj+8；`FUN_1810744a0(obj+0x640, rec)`（:2689500）把 rec+0x2d0/0x2f0/0x310/0x330/0x350（5 组 count）+ 5 组 monster vector + **rec+0x370/0x374/0x378/0x37c** 拷入子对象 obj+0x910..0x9bc。

**对象布局（0x9c8）**：+0x8 名字（大小写不敏感比较用）；+0xa8 事件槽表（`FUN_180cb2ac0`@fun_731.cpp:6214：`*(obj+0xa8+slot*8)=callback`）；+0x640 questId；+0x910-0x9a8 五组 count/monster 拷贝；+0x9b0/0x9b4/0x9b8/0x9bc ← rec+0x370/0x374/0x378/0x37c（**恒 0**）；+0x9c0 count；+0x9c4 阶段号。

### 2.3 vtable 实证（直接读 DLL）

从 `/Users/mc/IdeaProjects/58Server/MainServer/ScriptDLL64.dll` 反解 ctor 的 `lea` 得：SimpleSerialHuntQuest::vftable @ **0x181390a30**，SimpleHuntQuest::vftable @ 0x1813907a0。**前 19 槽（+0x00..+0x90）逐槽完全相同**（槽 3..17 是事件注册 thunk：+0x30/+0x38=0x181073160/0x181073130、+0x70/+0x78=0x1810746a0/0x181074640 等），唯一差异是槽 0 deleting-dtor：`0x18106c3c0`(Hunt) vs `0x18106c440`(Serial)。**+0x90 槽同为 0x1810745d0。** 即：两类运行时行为无差别。

事件槽绑定（`FUN_181074670`@:2689590）：**仅注册 0x11 号槽 → 回调 `FUN_1810749f0`**。

## 3. 击杀→「下一步」的完整路由与算术

### 3.1 路由

击杀事件载荷：`+0xc=int questId`、`+0x10=怪物名宽字符串指针`、`+0x18=player getter`。回调 `FUN_1810749f0`（:2689828）：

```c
iVar2 = *(int *)(param_2 + 0xc);            // questId
uVar3 = *(undefined8 *)(param_2 + 0x10);    // 被杀怪物名
// 遍历 DAT_1847204c8：
if (local_28 == iVar2) {                    // 键中 questId 匹配
  lVar5 = <node 中的 obj 指针>;
  iVar7 = FUN_181075dd0(lVar5 + 8, uVar3);  // 大小写不敏感宽字符串比较（:2691117，'A'-'Z'+0x20）
  if (iVar7 == 0) {                          // 名字精确命中 ⇒ 找到「该任务该怪物」的对象
    FUN_180caa960(*(undefined4 *)(lVar5+0x640), param_1, param_2,
                  *(undefined4 *)(lVar5+0x9bc),   // p4 = rec+0x37c = 0
                  *(undefined4 *)(lVar5+0x9c0),   // p5 = 该阶段 count
                  *(undefined4 *)(lVar5+0x9c4),   // p6 = 阶段号
                  *(undefined4 *)(lVar5+0x9b0),   // p7 = rec+0x370 = 0
                  *(undefined4 *)(lVar5+0x9b4),   // p8 = rec+0x374 = 0
                  *(undefined4 *)(lVar5+0x9b8),   // p9 = rec+0x378 = 0
                  1);                              // p10 = 1（固定）
```

因为键含 obj 指针，同 questId 的全部阶段/怪物对象共存于表中，名字匹配唯一定位。

### 3.2 相机算术（`FUN_180caa960`，ScriptDLL64.c:2138812；fun_731.cpp:1239 同文）

```c
plVar1 = payload->GetPlayer();                       // 玩家任务控制器
(+0xd0)(plVar1, &state, &vars, questId);             // 读 {state:char, vars:uint32}
bVar2 = (char)p7 * 6 - 6;                            // 当前槽移位
if (state=='\x03'                                    // 任务进行中(state==3)
 && ((vars >> ((char)p5*6-6 & 0x1f) & 0x3f) == p4)   // 前置槽==p4
 && ((int)(vars >> (bVar2 & 0x1f) & 0x3f) < p6)) {   // 当前槽计数 < 阶段号
  iVar3 = (1 << (bVar2 & 0x1f)) + vars;              // 当前槽 +1
  if (iVar3 == p8) {                                 // 达到阶段完成预设值
    if (p8 == p9) { (+0x100)(plVar1, questId, p9, 0); goto 发包; }  // 终值⇒+0x100通道
    if (p9 != 0) iVar3 = p9;                         // 否则跳到下一阶段预设值
  }
  if (p10 == 0) (+0xf8)(plVar1, questId, iVar3, 0);  // 写vars(变体)
  else (+0xf0)(plVar1, questId, iVar3, 0);           // 写vars（serial 固定走这里）
}
发包: local_68=questId; local_43=p4; local_47=p5; local_3b=p6; local_60=p8; local_5c=p9;
      local_54 = ((p9!=0)<<4 | p10&1)*2 | (p8==p9) | ...;
      (+0x118)(plVar1, &local_68);                   // 任务进度更新包
```

**代入实发数据（rec+0x370..0x37c 全 0）**：

- 当前槽移位 = (0×6−6)&31 = **26** ⇒ 计数器在 **vars bits 26-31（6 位）**。
- 前置门：`(vars >> ((count×6−6)&31)) & 0x3f == 0` —— count=1→移位0、2→6、4→18、7→36&31=4、9→48&31=16。对干净的 vars 这些位皆 0，恒真 ⇒ **count 列在此配置下不构成服务端击杀数门槛**（仅进 +0x118 展示字段）。
- 步进门：`(vars>>26)&0x3f < 阶段号`。
- 转移分支 `iVar3 == p8(=0)` 永假 ⇒ **永不跳预设、永不走 +0x100**。

**输入→旧状态→新状态→通道（示例 18911，5 阶段各 1 只 named Drakan）**：
杀第 1 阶段 Drakan_As → state=3、vars=0、(0<1) → vars=0+(1<<26)=0x04000000 → +0xf0 写回 → +0x118 包{step=1}。杀第 2 阶段 Drakan_Gi → (1<2) → vars=0x08000000 → …直至第 5 阶段后 bits26-31=5。**顶部 6 位计数器=串行步指针；每只当前阶段怪物死一次推进一格**。乱序击杀不被此计数器阻止（只受 `< 阶段号` 约束），真正的顺序性由怪物生成/出场设计（NPCServer 侧）与 talk NPC 阶段门承担。

对照家族相机（P0a 参照系）：`FUN_180cb13b0`（fun_731.cpp:5306，6 位槽 `slot*6-6`、mask 0x3f、达 target 且 flag≠0 走 +0x100 否则 +0xf0）与 `FUN_180cb14e0`（:5356，10 位槽 `slot*10-10`、mask 0x3ff）。`FUN_180caa960` 是同族的**串行变体**：多一道「前置槽==期望值」门与「阶段转移/跳预设」逻辑。16 个 serial id 的 16 进制形态在 `FUN_180cb13b0(`/`FUN_180cb14e0(` 常量调用点出现 **0 次**（14/16 抽验为 0，其余结构性不可能——id 只存在于 XML 数据流）——这正是 P0a「0 次」的成因：**serial 的 id 永远不进常量参数，而是运行时对象字段**。

## 4. 接取/交付 NPC 语义与状态存储

- NPC 对象由 `FUN_18106dfe0`（:2685226）按 record 建至多 5 个 quest-NPC：acquired(+0x28)、talk_npc1(+0xd8)、talk_npc2(+0x168)、talk_npc3(+0x1f8)、reward(+0x290)；各经 vtable+0x90 设名后用 +0x18/+0x20/+0x28(k)/+0x30(k)/+0x38/+0x40/+0x48/+0x50/+0x58(k) 配置（:2685249-2685343）。talk_npc1/2/3 的 phase 参数分别 0/1/2；reward 块对 hunt/serial 用 rec+0x37c（=0），对 talk 类型用 rec+0x288（talk 数），并 +0x28(4)（:2685328-2685342）。
- talk 类事件回调（槽 0x38→`FUN_1810748f0`→`FUN_180caa030`@:2138524）：玩家 phase == obj+0x9bc ⇒ 触发对话 0xa9c；phase 0xb/0xc/0xd/0xe（及 0x15-0x18）分别映射对话 0xbf1/0xd46/0xe9b/0xff0（`FUN_180caa0b0`@:2138543）。槽 0x37→`FUN_180caad20`@:2138974：**phase 达标 ⇒ player+0x100(questId,0,0) + npc+0x1b0 —— 这是交付/完成通道**（相机里的 +0x100 是「带完成语义的 vars 写」变体，serial 击杀路径不触发）。
- 状态存储：**不在 vars 之外的独立结构，就在玩家任务记录的 32 位 vars dword**（6 位槽打包；serial 用 bits 26-31 作步指针），由玩家侧接口 +0xd0 读 / +0xf0、+0xf8、+0x100 写、+0x118 发包；持久化在 MainServer 的 QuestDB。
- **MainServer 侧交叉验证**：`/Users/mc/IdeaProjects/58Server/server58-source/MainServer_Server64/classes/DB/QuestDB.cpp:440-560` —— 主服务器自己也加载 `quest_simpleserialhunt.xml`，且 **行内无 `talk_npc1` ⇒ 任务结构 +0x50 = 3；有 ⇒ = 10**（串行狩猎子类型码）。
- 与 DD（data-driven）体系的关系：`quest_data_drivens` 根标签（ScriptDLL64.c:2072267）的 DD 任务对象也注入 `DAT_1847204c8` 并直接写 0x130 槽=FUN_180c48b40（:2075211）；DD handler 用字面量 id 直调 6/10 位相机。serial 属 XML 对象驱动系，两系共享注册表但分派不同。

## 5. EVIDENCE_MISSING（2026-10-01 深夜更新；逐项状态见 §6.6，论证见 §6——**五洞全收口**）

1. ~~宿主侧事件入口~~ → **收口（§6.5）**：生成函数直接经 `FUN_180cb2ac0`（fun_731.cpp:6214 的对象槽表写入器）注册为槽 0x11（击杀）回调；mgr tag 容器 0x11 与 hash A 属 DD/事件任务分发系，与对象槽表是两套机制。
2. ~~event_quest.xml 原文件缺失~~ → **收口（§6.3）**：全域搜索确认不存在；且 §6.5 证明其属活动任务子系，主线任务不依赖它。
3. ~~count 列是否在别处强制~~ → **收口（§6.2，16/16）**：**codegen 生成脚本区间门相机强制 count（16/16 任务）**，原「不构成门槛」结论被推翻修正。
4. ~~玩家侧 vtable 偏移命名~~ → **收口（§6.4）**：IUserImp 133 槽两宿主全展开，全部定名。
5. ~~相机常量扫描覆盖~~ → **收口（§6.1）**：16/16 全量扫描完成（含勘误重扫，见 §6.8）。

---

## 6. 本轮补全（2026-10-01 深夜追补 + 同日次轮勘误；**五洞全收口**）

> 触发：对洞 #5 的补扫意外证伪了 §3.2 的推断（"id 只存在于 XML 数据流"），顺势打开了整层此前未知的
> **codegen 生成脚本车道**。以下全部为直接证据（反编译文本 + capstone 反汇编）。
> 勘误：首轮扫描 4 个 hex 为人工换算错误，"3/16 无 codegen"是伪影——同日次轮以程序化换算重扫修正为
> **16/16**（见 §6.8）。

### 6.1 洞 #5 收口：16 id 全量扫描（证伪原推断）

对 16 个 serial id 的十六进制与十进制形态在 ScriptDLL64 反编译源（fun/ 全目录）做全量扫描
（hex 程序化换算，勘误后终值）：

| 结论 | 任务 |
|---|---|
| **有 codegen 脚本条目（16/16，无一例外）** | 9622, 41189, 30600, 30610, 13025, 23025, 13619, 23619, 18911, 18912, 28911, 28912, 16991, 26991, 13918, 23918 |
| 6/10 位相机常量调用点（`FUN_180cb13b0(`/`FUN_180cb14e0(`） | **仍为 0/16**（原结论保持成立；serial 走 `FUN_180cb2450` 区间门变体） |

脚本条目引用的 helper 全部定义在 fun_731.cpp（相机同文件）：

| helper | 签名（还原） | 语义 |
|---|---|---|
| `FUN_180cb5920` | (名节点DAT, L"怪物名", questId) | 怪物名→任务注册（23025 六名=monster_first 六怪 ✓；16991/26991 各 22 名） |
| `FUN_180cb3070` | (节点DAT, 记录DAT, questId, kind u8, value u32, extra u32) | 记录上建步进描述数组（fun_731.cpp:6478：+0x570 计数、+0x574=questId、+0x578+i*6 {kind,value,pad}、+0x5f0+i*4=extra） |
| `FUN_180cb2450` | (questId, ctx, slot, low, high, display, target, flag) | **区间门 6 位相机变体（§6.2）** |
| `FUN_180caf7c0` | (questId, obj, ctx, int, u32, int) | 页动作分发（fun_731.cpp:4308：0x3ea/0x3eb/0x3ec/0x3ef/0x3f4/0x3f5/20000/0x4e21 = 1002/1003/1004/1007/1012/1013/20000/20001；0x3ea/20001 分支经 +0xd8=SetQuestAcquired 接取） |
| `FUN_180cafa40` | (questId, obj, ctx, phase, …, 物品指针组) | 交付/报告流（fun_731.cpp:4412：phase 匹配发页 0x548/0x947/0x69d/0x7f2；动作 0x3f1(1009)/10000/0x2711(10001) → +0xf0=SetQuestProgress / +0x1d0=RemoveItem） |
| `FUN_180caf9c0` | (questId, obj, ctx, phase) | phase 匹配 → obj+0x1b0（§4 已知的交付通道） |

**原 §3.2「id 只存在于 XML 数据流」推断被证伪**：正确表述是「serial id 不进 6/10 位常量相机调用点，
但 13/16 进 codegen 生成脚本」。

### 6.2 洞 #3 大半收口：count 列的真正强制点 = 生成脚本区间门相机

`FUN_180cb2450`（fun_731.cpp:5903）函数体语义：

```c
GetQuestState+vars（+0xd0）；
if (state==3 && param_4 <= vars && vars < param_5) {        // 区间门在整字 vars 上
  vars += 1 << (param_3*6-6);                                // 槽 param_3 计数 +1（6 位）
  if (flag && param_7==param_5 && vars==param_7) +0x100(questId, vars, 0);  // SetQuestSuccess
  else +0xf0(questId, vars, 0);                              // SetQuestProgress
}
+0x118 发包 {questId, prev=param_4, slot=param_3, target=param_5, display=param_6, cmd = 达标?0x1b:0x1a};
```

与表行对拍（23025，逐字段实证；表行 = `<id id="23025">`，count_first=9 六怪 / count_second=1 一怪）：

| 阶段 | 表行 | 生成脚本调用（fun_804.cpp:382/391） | 语义 |
|---|---|---|---|
| 1 | count_first=9 | `(0x59f1,ctx,1, 0, 9, 9, 0x49,1)` | vars∈[0,9) 每杀槽1+1；槽1满=9 |
| 2 | count_second=1 | `(0x59f1,ctx,2, 9, 0x49,1, 0x49,1)` | vars∈[9,0x49) 每杀槽2+1（+64）；0x49=73=9+64×1=**fullValue**；flag=1 ⇒ vars==fullValue 走 +0x100 |

⇒ 串行任务的 vars 语义是**跨阶段线性行进**（fullValue_k = Σ前k阶段 count×槽权），**count 列对 16/16 全部
任务是真实的服务端击杀门槛**。原 §3.2「count 列不构成服务端击杀门槛（仅展示）」**只对对象相机路径成立**
（该路径属活动任务子系，见 §6.5），据此整体推翻修正。更多实证：18911（四阶段各 1 只，target=0x1041041=四槽各 1 的
fullValue）、30610（`(0x7792,ctx,1, 0,1,1, 0x41,1)` / `(0x7792,ctx,2, 1,0x41,1, 0x41,1)`=两阶段各 1、
二阶段末走 +0x100）、13918（五阶段，同型）、23918（五阶段，同型）。§3.2 的 18911 示例（bits 26-31 步指针）
保留为**活动任务对象相机**的真实行为，不再是主线串行任务的语义。

**三任务勘误后补证**：30600/30610/23918 首轮"零命中"是 hex 伪影（§6.8）；正确 hex 重扫全部有完整
codegen（名注册 + 步进表 + 阶段相机 + af7c0/afa40 页/交付；30610 还见 afa40 带物品指针组
`&DAT_18572ce30/40/50` = give_item/remove_item 列落地）。**16/16 无一例外走线性行进形状，无 EVIDENCE_REQUIRED。**

### 6.3 洞 #2 收口：event_quest.xml 全域不存在

`find -iname 'event_quest*'` 覆盖整个真端根快照（含 server58 / server58-origin / server58-map 部署目录、
Cached、MainServer、NPCServer、Map/XML）：**零命中**。装载器路径模板 `%squest\event_quest.xml` 的目标文件
在快照内任何位置都不存在；Map/XML/Quest_SimpleSerialHunt.xml（16 行，行元素 `<id id="N">` 属性形）是可用
同 schema 行集。入仓路线：以 Map/XML 行集为源 + 来源 hash（同 P0b provenance 模式）；「运行时数值与原
event_quest.xml 一致」升级为不可实证假设，登记为已知限制。

### 6.4 洞 #4 收口（借 P7 宿主接口考古）：玩家侧 vtable 偏移定名

IUserImp 133 槽两宿主全展开（`../p7-prereqs/dd-host-interface-detail.md` §1），本报告用到的偏移全部定名：

| 偏移 | 槽 | 名 | 对本报告语义推断的影响 |
|---|---|---|---|
| +0xd0 | 26 | GetQuestState | 读 {state, vars} ✓ |
| +0xd8 | 27 | SetQuestAcquired | （页动作 0x3ea/20001 接取路径） |
| +0xf0 | 30 | SetQuestProgress | 普通写 ✓ |
| +0xf8 | 31 | SetQuestProgressMemoryOnly | §3.2 的 (+0xf8) 分支=内存写（不持久化；由槽名推知） |
| +0x100 | 32 | SetQuestSuccess | 「带完成语义的 vars 写」实为 SetQuestSuccess ✓ |
| +0x118 | 35 | ShareProgressMultiple | 进度/展示包 ✓ |

### 6.5 洞 #1 收口：实机分发 = 生成函数直接注册为对象槽表槽 0x11 回调

生成函数不是被间接索引的——每个「阶段×怪物」一条显式注册，由 **`FUN_180cb2ac0`（即计划 §2.3 的
fun_731.cpp:6214 对象槽表写入器）** 绑定到 event-node 的槽 0x11（击杀）。23025 注册块全貌
（monolith :1405995-1406130，地址 0x1808d6870..0x1808d6ab0）：

```c
FUN_180cb2ac0(&DAT_185704efc, &DAT_185706800, 0x11, FUN_180dbfc00, local_res8); // Alert_As   → 阶段1相机
FUN_180cb2ac0(&DAT_185704efd, &DAT_185706e40, 0x11, FUN_180dbfc00, local_res8); // Alert_Wi   → 阶段1相机
FUN_180cb2ac0(&DAT_185704efa, &DAT_185705b80, 0x11, FUN_180dbfc00, local_res8); // DDealer_Gu → 阶段1相机
FUN_180cb2ac0(&DAT_185704efb, &DAT_1857061c0, 0x11, FUN_180dbfc00, local_res8); // HPDrainDD  → 阶段1相机
FUN_180cb2ac0(&DAT_185704efe, &DAT_185707480, 0x11, FUN_180dbfc00, local_res8); // Heal_Pr    → 阶段1相机
FUN_180cb2ac0(&DAT_185704ef9, &DAT_185705540, 0x11, FUN_180dbfc00, local_res8); // Tanker_Fi  → 阶段1相机
FUN_180cb2ac0(&DAT_185704eff, &DAT_185707ac0, 0x11, FUN_180dbfc40, local_res8); // Weabind_Ri → 阶段2相机
```

第二参与 fun_341.cpp 的 cb5920 名注册用**同一批名节点 DAT**（六怪 + Weabind），第一参是该怪的 event-node
（与任务名节点 DAT_185704f00 同簇）。⇒ **codegen 系统自含**：怪物名以字面量进代码、名字节点与事件节点
静态建表、阶段相机与 af7c0（页动作）/afa40（交付报告，含物品指针组）/caf9c0（phase 交付）直接注册，
**不依赖 event_quest.xml**。18911/30610/13918/23918 抽查同构。

**event_quest.xml 重新归位**：它是**活动（event）任务子系**的表（type=hunt/collect/serial_hunt 的事件任务
行集，装载进 DAT_1847204c8 对象注册表，走对象相机 FUN_180caa960 的 bits26-31 步指针）——与主线任务
（codegen）是两条并行子系，P7 考古的 tag 容器/hash A 死代码面属 DD/事件分发系，不影响对象槽表分发。
主线 16 个串行任务的权威语义 = §6.2 的区间门线性行进，**对象相机不再构成竞争解释**。

对本车道的映射：真端「per-怪物×阶段 槽 0x11 注册」在 aionemu 由既有击杀链承担
（`NpcController` → `QuestEngine.onKill` → router → 家族 handler 按「怪物名 ∈ 当前阶段怪物名单」匹配），
`NativeQuestTableLoader` 已解析每阶段 monster 名单，无需新增注册表结构。

### 6.6 更新后 EVIDENCE_MISSING 状态（**五洞全收口**）

| # | 项 | 状态 |
|---|---|---|
| 1 | 宿主击杀分发路径 | **收口**（§6.5：生成函数 = 槽 0x11 注册回调；对象相机属活动任务子系） |
| 2 | event_quest.xml 原文件 | **收口**（全域不存在；属活动任务子系，主线以 codegen 驱动，Map/XML 行集+hash 入仓） |
| 3 | count 强制 | **收口**（§6.2：16/16 区间门强制，vars 线性行进） |
| 4 | 玩家侧 vtable 定名 | **收口**（§6.4） |
| 5 | 相机常量扫描覆盖 | **收口**（§6.1 + §6.8 勘误重扫） |

**P2 前置证据状态：齐。** 切换批待办只剩用户门（README §四）。

### 6.7 家族级 codegen 覆盖普查（P3-P6 前置证据）

对 7 张 repo 家族表各抽 3 id（id≥4096 优先，避免短 hex 常量碰撞），程序化换算后对 monolith 全文分类扫描：

| 家族（repo 行数） | 抽样 | 命中 helper 形 | 判定 |
|---|---|---|---|
| SimpleHunt (1865) | 14276, 24275, 4096 | b5920 名注册 + b3070 步进表 + **b13b0 6位相机** + af7c0/afa40 | codegen ✓（=P0a 相机矩阵已知） |
| SimpleSerialHunt (16) | 9622, 41189, 30600 | b5920 + b3070 + **b2450 区间门相机** + af7c0/afa40 | codegen ✓（16/16 全扫 §6.1） |
| SimpleTalk (3152) | 14275, 14270, 24270 | b5920 + b3070 + other-FUN（talk 专属注册函数） | codegen ✓（零相机，符合无击杀计数） |
| SimpleCollectItem (262) | 4099, 4202, 4207 | b5920 + b3070 | codegen ✓ |
| SimpleUseItem (160) | 11036, 11046, 11059 | b5920 + b3070 + other-FUN | codegen ✓ |
| SimpleItemPlay (43) | 9623, 19048, 29048 | b5920 + b3070 + other-FUN | codegen ✓ |
| CombineTask (574) | 5001, 5002（5000 全文零命中，疑休眠/他系行，P6 批内单查） | b5920 + b3070 | codegen ✓ |

⇒ **codegen 生成脚本是主线任务的全家族通用运行时机制**（计划 §2.3 模型全族成立）：每任务一条目 =
名节点注册 + 步进表 + 家族专属处理器（hunt/serial=相机，talk=页流，useitem/itemplay=用物流）。
「启动期由表构建注册表」的 native 等价物对全家族适用；P3-P6 前置考古可复用本报告的
「程序化 hex 抽样 → helper 分类」方法。

### 6.8 勘误记录

首轮 16-id 扫描的 4 个 hex 为人工换算错误：30600=**0x7788**（误 0x7768）、30610=**0x7792**（误 0x7772）、
23918=**0x5D6E**（误 0x5D4E）、13918=**0x365E**（误 0x364E）。由此得出的「3/16 无 codegen、三任务
EVIDENCE_REQUIRED」为伪影；家族普查（§6.7，程序化 hex）揭穿后重扫，四任务全部有完整 codegen。
教训入档：**id→hex 一律程序化换算**；定点扫描与抽样普查互为交叉验证。
