# P0a §4.1 语义矩阵：DD Hunt 计数算术（真端还原）

> P0a 只读审计产物 · 2026-10-01。方法：快照 Hunt 类体缺失 ⇒ 只读反汇编原始 `ScriptDLL64.dll`（capstone）定位 vtable 0x18123d3e0 与计数函数，回映射快照 `fun_718.cpp` 与全量反编译 `server58/MainServer_ScriptDLL64/ScriptDLL64.c`。
> **本报告修正计划 §3-12/§4.1 的前提**：「8 行 >63 宽计数」不成立，见 §3 对照表与核心修正结论。
> 与 §4.2 互证：DD 处理器的 +0xE8/+0xF0/+0x100 槽位号与家族相机路径的 IUserImp（GetQuestProgress/SetQuestProgress/SetQuestSuccess）一致——DD 与家族走同一用户接口写入通道（跨证据推定，置信度中高）。

Hunt 计数考古报告

**方法说明**：快照 `server58-source` 中 Hunt 类体缺失（只有 loader 引用 vftable 符号）。本次考古通过只读解析原始二进制 `/Users/mc/IdeaProjects/58Server/MainServer/ScriptDLL64.dll`（PE 基址 0x180000000，用 python3/capstone 反汇编，未写任何文件）定位到 `QuestProgressExtraInfo_Hunt::vftable = 0x18123d3e0` 及其真正的运行时计数函数，再回映射到快照与全量反编译文件 `/Users/mc/IdeaProjects/58Server/server58/MainServer_ScriptDLL64/ScriptDLL64.c`（下称 ScriptDLL64.c）。

---

## 1. QuestProgressExtraInfo_Hunt 类体：存在，但是纯数据类（无计数逻辑）

**此前审计的假设（计数逻辑在 Hunt 类内部）不成立。**

- vtable 地址（二进制 0x180c4b55c 处 `lea rcx,[rip+...]` → 0x18123d3e0，对应 ItemPlay.cpp:143 的赋值）：仅 2 个槽，都是析构：
  - slot0 = `FUN_180c45e00`（/Users/mc/IdeaProjects/58Server/server58-source/MainServer_ScriptDLL64/fun/fun_718.cpp:5776）——销毁 this+8 的 groups vector 与 this+0x20 的 actions vector；
  - slot1 = `FUN_180c48df0`（fun_718.cpp:8033）——清理 this+0x20。
- 兄弟类 vtable 地址（同样只有析构对）：CollectItem=0x18123d440、Hunt=0x18123d3e0、PvP=0x18123d3f8、Talk=0x18123d410、ItemPlay=0x18123d428、EnterArea=0x18123d458、EnterWorld=0x18123d470、TalkFOBJ=0x18123d4a0。slot 实现一一对应 fun_718.cpp:5738/5757/5776/5799/5812/5824 与 fun_718.cpp:8033/8042/8051/8060。

**Hunt handler 对象（0x38 字节）布局**（由 loader 建立证据）：
- +0x00 vptr；+0x08 `vector<HuntGroup> groups`（元素 0x38 字节）：
  - HuntGroup = `{ vector<wstring> names @+0x00, vector<uint32> name_hashes @+0x18, uint32 target_count @+0x30 }`
- +0x20 `vector<action>`（value2..value10 属性解析出的完成动作）
- +8 处清零可见于 loader（classes/Item/QuestProgressExtraInfo_ItemPlay.cpp:140-150，`FUN_18107ac38(0x38)` 后 6 个 qword 清零）。

**数据填充**：属性加载器 `FUN_180c4b980` case 2（"DataDrivenQuest - Hunt Progress"，fun_718.cpp:9363 起）：value0 字符串以 `L" ,;"` 分词（fun_718.cpp:9391），名字 token 依次入 names 向量并用 `FUN_1810798f0` 做 Java 风格哈希 `h*0x1003f+c`（fun_913.cpp:2553-2563）入 hashes 向量；遇到非 0 数字 token 结束一组；随后 `FUN_180c518d0(handler, &names, &hashes, count)`（fun_718.cpp:9611 → fun_719.cpp:2706-2735，经 `FUN_180c51670` 按 0x38 步长 push，fun_719.cpp:2586-2613）。`分号;`分隔多组，组内格式 `名字[,名字...] 数量`。

---

## 2. 真正的计数逻辑：FUN_180c46020（DD Hunt 击杀处理器）

**文件**：fun_718.cpp:5896-6054（=ScriptDLL64.c:2069982）。注册：DD 注册函数 case 2 中 `DAT_184720a50 = FUN_180c46020;`（ScriptDLL64.c:2076037），同时把每个目标名插入 name→quest 映射（ScriptDLL64.c:2075950-2076036，`param_1+0xa0` 的 map）。快照内另有同一函数族的 Talk 推进处理器 `FUN_180c466a0`/`FUN_180c467b0`（fun_718.cpp:6217/6277）佐证同一 vars 布局。

### 2a. 每次击杀的算术（输入 → 旧 vars → 新 vars → 通道）

```
输入: param_1=服务器侧 quest 对象(经 vtable), param_2=被击杀 NPC 名哈希(u32),
      param_3=击杀者/共享者 id, param_4=quest var 索引, param_5=期望当前 step, param_6=距离平方

var_old = vtable[+0xE8](this, param_4)                    // 读 32 位 var (fun_718.cpp:5908)
step    = var_old & 0x3f                                  // 低 6 位 = 步骤号 (bits 0-5)
if (step != param_5) return                               // 步骤不匹配 → 不计数
// 共享模式检查 def+0x70: 1→dist²≤2500; 2→≤10000; 5/6→≤40000 + 队友判定 (fun_718.cpp:5913-5980)
row = def进度向量[step]  // 步长0x10 {cat_id@+0, handler@+8}  (fun_718.cpp:5985-6001)
if (row.cat_id != 2 /*Hunt*/) return                      // (fun_718.cpp:6002)
groups = row.handler->groups  // handler+8, 0x38 步长

// 组循环, 最多 4 组 (`if (5 < iVar15) break`), 位偏移 shift-6 = 6,12,18,24:
  slot = (var_old >> (shift-6)) & 0x3f                    // 6位槽 (fun_718.cpp:6017)
  if (param_2 ∈ group->hashes):                           // 名哈希命中 (fun_718.cpp:6019-6020)
      if (slot < group->target /*@+0x30*/):               // (fun_718.cpp:6021)
          var_new = var_old + (1 << (shift-6));  matched=true   // (fun_718.cpp:6023)
      break
  if (slot < group->target) allFull=false                 // (fun_718.cpp:6029)

if (!allFull):
      if (!matched) return                                // 超杀/不匹配 → 不写 var（无副作用）
      vtable[+0xF0](this, param_4, var_new, 0); return    // 普通更新通道 (fun_718.cpp:6043)
else: // 该 step 所有组满
      var_next = (var_old & 0x3f) + 1                     // step++ 且组槽位清零 (fun_718.cpp:6046)
      if (var_next < 行数): vtable[+0xF0](this,param_4,var_next,0)      // 还有后续 <data>
      else:                 vtable[+0x100](this,param_4,var_next,1)     // 最终完成通道 (fun_718.cpp:6047-6052)
      FUN_180c4d190(handler->actions@+0x20, this, param_4)  // 执行该步动作(swap/过场/计时) (fun_718.cpp:6053)
```

### 2b. 32 位 quest vars 槽布局（DD 宽计数如何编码）

**既不是 10 位槽，也不是整槽 32 位，而是「6 位步号 + 4×6 位组计数」打包在单个 var 中**：
- bits 0-5：当前步骤号（0-63，对应 XML 第 step 个 `<data>` 行）；
- bits 6-11 / 12-17 / 18-23 / 24-29：组 1-4 的击杀计数，各 6 位，**单组上限 63**；
- bits 30-31：不使用（循环在第 5 组前 break，位 30 从不写入）。

存储位置：服务器侧 quest var 数组（经 vtable +0xE8/+0xF0/+0x100 访问），与家族任务相机（fun_731.cpp:5306 `FUN_180cb13b0` 6位×6槽、:5356 `FUN_180cb14e0` 10位×3槽，均有 `var<0x40000000` 与 `status=='\x03'` 守卫）走同一通道但打包方式不同。**DD Hunt 路径没有 0x40000000 守卫，也没有 status==3 检查**（status 检查存在于 Talk 处理器 FUN_180c466a0，fun_718.cpp:6221-6223）。

### 2c. 前置守卫与超杀
- 守卫 = 步骤匹配（`step != param_5 → return`）+ 共享距离/队友检查；无 status 检查、无 var 上限守卫。
- 超杀：命中但该组已满（或全部已满但非最后步）→ `matched=false → return`，**不写 var**，安全 no-op（fun_718.cpp:6037-6042）。
- **唯一漏洞**：target > 63 时 6 位槽溢出回绕。target=100 时第 64 杀使 bits6-11 从 63→0 并污染 bit 12（下一组槽）；`(slot<target)` 永远无法稳定满足到 100，**任务无法通过击杀完成**。这是算术的直接后果（fun_718.cpp:6017-6046），非推测。

### 2d. 完成判据与通知
- 判据：该 step 的**所有组**槽位均达各自 target（`allFull==true`）。
- 推进：非最终步走 vtable+0xF0（普通 var 更新）；最终步走 **vtable+0x100(…, flag=1)「final」通道**，同时 `FUN_180c4d190` 应用 handler+0x20 动作表（case 1-10：刷 NPC/过场/计时器等，同族实现见 fun_718.cpp:9835 `FUN_180c4c8d0`、9986 `FUN_180c4cd50`、10131 `FUN_180c4d190`）。
- 客户端通知：DLL 内只见 +0xF0/+0x100 区分；具体封包在服务器侧类（vtable 实现不在 ScriptDLL64）。**EVIDENCE_MISSING：封包 opcode**（需在 MainServer_Server64 侧找 +0xF0/+0x100 实现）。

### 2e. 多组分槽
- 每个 `<data>` 行（step）一个 handler；组按解析顺序占 bits 6/12/18/24；**最多 4 组**（第 5 组起被 `5 < iVar15` 截断——若前 4 组满而第 5+ 组未满，`allFull` 仍为 true，会错误推进步骤）。组数与组内名单、target 全部来自 value0 的 `名 名,数;名,数` 语法。

---

## 3. 8 个 quest id 对照（/Users/mc/IdeaProjects/58Server/Map/XML/data_driven_quest.xml，UTF-16LE，2526 任务，1392 条 Hunt 行）

全文件 Hunt 行中 **count>63 的只有 1 条**（80817）。此前审计的「8 行超 63」与该文件不符——90/120/300 全部是 value5/value10 里的**刷怪描述符**（`Relative/Absolute <npc>, <数量>, <存活300秒>, <坐标>`）与计时器（`<value10>300, N, 0`）被误读为 Hunt 计数。

| quest | XML 实况（行号） | 类别 | count | vars 编码公式（推断自 FUN_180c46020，证据充分） |
|---|---|---|---|---|
| 10034 | line 3003 块，Hunt 行 `LF4_Temple_Nepilim_NamedQ_53_An 1` | Hunt | 1 | 杀该 NPC：var += 1<<6（slot0 0→1=满）→ allFull → var=(var&0x3f)+1 步进。审计"90" **不在本文件** |
| 20503 | line 15451 块 | **无 Hunt 行**（Talk/Talk/CollectItem/EnterArea） | — | "120×4"实为 EnterArea value5 四条 `Relative …, 1, 120` 刷怪描述符。EVIDENCE_MISSING（审计数字无出处） |
| 20506 | line 15550 块 | Hunt ×2 步，各 1 组：`DF5_H_MissionVritra_Low_As_63_An 1;`、`DF5_H_MissionDeva_Fi_63_An 1;` | 1,1 | 每步 var += 1<<6 后步进；`300`=value5 刷怪存活参数/`value10 600,6,0` 计时器，非计数。审计"[90,1,1,300]"系混入刷怪描述 |
| 20507 | line 15592 块 | **无 Hunt 行**；value5 两条 `Relative …, 1, 300` 刷怪 | — | "[300,300]"=刷怪描述符。EVIDENCE_MISSING |
| 25050 | line 18495 块 | **无 Hunt 行**；value5 `Absolute DF5_Zagmus_E, 1, 300, 坐标`；value10 `300, 4, 0` | — | "90"不在文件 |
| 25051 | line 18525 块 | **无 Hunt 行**（同上结构，value10 `300, 2, 0`） | — | "90"不在文件 |
| 25082 | line 18661 块 | Hunt：`DF5_H2_QuestShellizard_63_An 1;` | 1 | 同 10034 公式。审计"[90,1]"中仅"1"有据 |
| 80817 | line 31735 块 | Hunt：`world_event_camel 100;` | **100** | **唯一真实 >63**：bits6-11 上限 63 < 100 → 第 64 杀起槽回绕并污染 bit12，任务无法完成（见 2c）。公式同上但永不满足 allFull |

---

## 4. 缺失证据清单与补全路径

1. **击杀事件分发器**（谁调用已注册的 `DAT_184720a50`/表 `DAT_184720a10`）：反编译 C 中无读者（ScriptDLL64.c 仅 2076037 一处写）。GetInterface 导出（0x180cb5910，接口对象 0x1846e1cc0，vtable 0x18133a218 共 12 项，实现散布 fun_731.cpp:7077-7420 与 fun_732.cpp:408-460）均为注册类辅助。需补：对 0x184720a10/0x184720a50 的交叉引用重反编译，或 Server64 侧调用 DLL 的路径。由此也缺失：param_4（var 索引）与 param_5（期望步号）的确切来源（推断为服务器按任务 id 从 def/玩家态取得，无直接证据）。
2. **+0xE8/+0xF0/+0x100 的实现与封包**：服务器侧 quest 对象类（不在 ScriptDLL64），需在 MainServer_Server64 反编译中定位 quest var 数组与 CM/SM 封包。
3. **参数 5/6 的语义边界**：共享模式值 1/2/5/6 的 XML 属性来源（推测为 Hunt 行的 valueN，本 8 行均未出现）。
4. 相关注件（全部只读已核）：
   - /Users/mc/IdeaProjects/58Server/server58-source/MainServer_ScriptDLL64/classes/Item/QuestProgressExtraInfo_ItemPlay.cpp:90-283（分派）
   - /Users/mc/IdeaProjects/58Server/server58-source/MainServer_ScriptDLL64/classes/Quest/QuestProgressExtraInfo_Talk.cpp:59-114（接取分派，Hunt 不在接取侧）
   - fun_718.cpp:5896（计数）、9363（Hunt 数据加载）、6217/6277（Talk 推进同布局佐证）、10499（0x640 运行时对象注册）
   - fun_731.cpp:5306/5356（家族任务 6/10 位相机，对照组）
   - /Users/mc/IdeaProjects/58Server/server58/MainServer_ScriptDLL64/ScriptDLL64.c:2069982、2075950-2076079（全量版计数/注册）
   - /Users/mc/IdeaProjects/58Server/MainServer/ScriptDLL64.dll（vtable 0x18123d3e0、导出 GetInterface）

**核心修正结论**：DD 任务不存在「超 63 宽计数」的 8 行问题——实际编码为 6 位步号+4×6 位组槽（上限 63/组），全 XML 仅 80817（count=100）真实超限且因 6 位槽回绕而**不可能完成**；其余 7 行的 90/120/300 均为刷怪/计时描述符误读
