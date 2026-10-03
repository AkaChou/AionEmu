# P11 前置：真端 Quest-AI NPC 车道的语义边界（只读取证）

- 日期：2026-10-03；性质：**只读**（零生产代码/数据变更；本文件与 A 批的 SimpleItemPlay 拆闸门同批提交）。
- 起因：P10 §7 更正后，「176 中 142 个在真端有 `npcs.xml quest_ai_name ↔ ScriptDLL IOneQuestScriptNpc(name, questId)`
  注册」已坐实；本文件回答**这层注册到底承担什么**，以决定「路径 D（第三条 native 车道）」是否成立。

## 1. 一页结论

| 驱动面 | 真端实现 | 覆盖（本仓 6224 任务集） |
|---|---|---|
| 族表 row loader | ScriptDLL 逐族读行（列名按 `_wcsicmp` 匹配，见 §2 证据） | 5484（RETAIL_TABLE） |
| DD 表 | `data_driven_quest.xml` 行 + `category_acquire_` 选进度类 | 2510 行（与 176 **零交集**） |
| Quest-AI NPC 注册 | `FUN_180cb5920(out, L"name", questId)` → `IOneQuestScriptNpc` | 4682（含 176 中的 142） |
| quest.xml 设计数据 | `QuestDesignData::Load`（NPCServer）逐 quest 建对象 + 逐子元素消费 | 169/176 有行 |

**结论（判定）**：176 是**对话驱动 + quest.xml 设计数据**的件，不走族表也不走 DD；其「目标计数/完成」面在本轮**仍未坐实**
（候选见 §4），因此**路径 D 的完整实现暂不具备开工条件**；但同一份取证已足以支撑一个低风险、立刻可做的替代动作
（**D1：用真端注册面反向校验本仓 XML 车道的 NPC 绑定**，见 §5）。

## 2. 新增第一手证据（本轮）

1. **row loader 逐列读取**（`ScriptDLL64.c:2686058` 区间，同一段连续解析）：按名字逐个取
   `con_quest` → record+0x2b4、`cutsceneId1` → +0x2b8、`cs1_haction` → +0x2bc、`cs1_progress` → +0x2c0。
   ⇒ 族表列是**装载期**读入的结构字段（对应本仓 `NativeQuestTableLoader` 的读法），
   也解释了 SimpleItemPlay 表**没有** `cs1_haction` 时该槽恒空（= 无页动作触发点）。
2. **quest.xml 装载 = 通用设计数据**（`NPCServer_NPCSvr64/classes/Quest/QuestDesignData.cpp:1-201`）：
   root `<quests>` → 每 `<quest>` 建一个 `QuestDesignData`（`FUN_1402a98a0(obj,name,descIdx,id,...)`），
   再对每个子元素调用 `FUN_1402abf20(obj, child)` 逐个消费 ⇒ 设计数据层**不区分族**，列由数据自带。
3. **DD 的进度类分派**（`MainServer_ScriptDLL64/classes/Quest/QuestProgressExtraInfo_Talk.cpp:1-169`）：
   读 DD 行 `category_acquire_`，`"ItemPlay"` → `QuestProgressExtraInfo_ItemPlay`（record+8=3）、
   `"Talk"` → `QuestProgressExtraInfo_Talk`（record+8=4）。
4. **DD 不可覆盖 176**：DD 2510 行 ∩ 176 = **0**；DD 的 `category_acquire_` 取值分布 =
   `Talk 2044 / EnterArea 233 / none 136 / _faction_ 52 / LevelUpLogIn 24 / ItemPlay 21 / EnterWorld 15 / LevelUp 1`。

## 3. 本仓现状（对本批的意义）

- 本仓 XML 车道（740 份）**已经在功能上实现**了这 142 件的对话 + 目标 + 领奖（P3/P5 批次逐族验证过），
  即 P10 §7.5 的「路径 D」在**行为**上已存在，缺的是**真端溯源证据面**与**NPC 绑定的权威校验**。
- 因此本批不建议立刻新建第三条 runtime 车道（成本高、目标计数面未坐实），而建议先把注册面变成**门禁证据**。

## 4. 未坐实项（P11 待验清单）

1. 142 件的**击杀/收集计数**由谁产生：候选 (a) NPCServer 通用进度（`QuestDesignData` 列 + 击杀/掉落事件）、
   (b) 客户端上报型进度、(c) ScriptDLL 的通用类（`QuestProgressExtraInfo*` 之外的路径）。
   建议下一步：在 `NPCSvr64.c` 中定位「击杀事件 → quest design data 列匹配」的调用链
   （`classes/Account/IKilledByUserEventImp.cpp` 目前只有 17 行恢复体，需回原 .c 展开）。
2. SimpleItemPlay 两行过场的**播放点**：已知表列被 row loader 读入 record+0x2b8（§2.1），
   且该表无 `cs1_haction` ⇒ 无页动作触发；thunk `FUN_180cacb30(questId, rowValue, ...)` 的节点槽语义
   （param_2 与硬编码 action 数组比较、播放值取自 node 数组）**未坐实**，不做推测实现。
3. 34 个无注册件（清单与建议见 `../p10-xml-only-176/XML-ONLY-176-ADJUDICATION.zh-CN.md`）。

## 5. 建议落点（按性价比）

- **D1（推荐，立即可做）**：把 `npcs.xml quest_ai_name ↔ ScriptDLL (name, questId)` 注册面导出为本仓
  证据表（复用 `../p10-xml-only-176/scan_onequestscriptnpc.py` 与 `crosscheck_quest_ai.py` 的口径），
  加一个门测试：**XML 车道每份有 NPC 引用的任务，其引用的 NPC 必须在该任务的注册名集合内**
  （本批抽 1001/14010/11279 已 3/3 通过）。收益：把「本仓自研 XML 的 NPC 绑定」从人工核对升级为真端权威校验。
- **D2（中期）**：D1 落地且 §4.1 坐实后，再评估把 142 件纳入 native 车道的 ROI。
- **D3（保底）**：维持 XML 车道，仅在 P10/P11 文档标注「真端溯源自 Quest-AI 注册面」。

## 4.1 已坐实（2026-10-03 第二轮）：进度面是**服务端 item-driven**，不是客户端上报

问题：142 件的击杀/收集计数由谁产生？答案：**计数不由"逐杀事件"产生，而是由持物 + 掉落规则表达**，
整套逻辑在服务端（NPCServer 存储 + MainServer 读取），证据如下（全部第一手）：

1. **设计数据侧**（`NPCSvr64.c:465400-465560`，`Quest::Set` 逐列分派）：quest.xml 的列被逐条解析进 quest 实例结构——
   `Quest::Set, collect_ap`(+0x1c0) / `collect_ap_progress`(+0x1c4) / `collect_item1..4`(0x1e9..0x1ec) /
   **`collect_progress`(+0x1bc)** / `drop_item1..5`(0x308..0x30c) / `drop_each_member` / `drop_prob` /
   以及 reward_*、条件、`quest_work_item` 等（字面量族 `grep -o 'L"Quest::Set, …'` 40+ 列）。
2. **进度读取口**（`MainServer_Server64/classes/NPC/NpcScriptMgr.cpp:8`，恢复名 `NpcScriptMgr_GetItemCollectingProgress`）：
   按 questId 查玩家 quest 实例，返回其 **+0x1bc（= `collect_progress`）** 字段；查不到返回 0xffffffff。
   ⇒ 进度是"收集类进度槽位"，**由持有物驱动**，不是击杀计数器。
3. **进度存储/下发**（`NPCSvr64.c:485577/485665`）：写入口是 `User::SetQuestProgress` /
   `User::SetQuestProgressMemoryOnly`（来源文件 `..\..\Shared\Quest.cpp:0x9d8/0x9ea`），底层落地到
   `UserQuestData_SetQuestProgress`（`NPCSvr64.c:488391`）+ `GetQuestProgress/SetQuestSuccess/SetQuestBranch`；
   写入后**服务端向客户端下发**进度包（opcode 头 0x1e…）——方向是服务端 → 客户端。
4. **设计期校验**（`Server64.c:2295987/2295995`，`NpcScriptMgr::VerifyQuestData`）：
   "have collect_item, but collect_progress is invalid" / 反之 —— 收集物与进度槽位必须成对声明，佐证模型。
5. **反证**：`drop_monster` 在三个二进制中字面量 0 命中（NPCSvr64/Server64/ScriptDLL 全 0）；
   客户端 `quest_monster.csv` / `quest_script_monster.csv` 是**客户端展示表**，服务端对应物是
   `drop_item`/`drop_prob`/`collect_item`/`collect_progress`。

**对路径 D 的含义**：第三条 native 车道**不需要**实现逐杀计数子系统——只要
（a）NPC 对话 ingress（quest_ai_name 注册面，142 件已坐实）、（b）持物/掉落进度面
（`collect_item` + `collect_progress` + `drop_item`/`drop_prob`）。本仓 XML 车道已等价实现这两面，
故 **D1（注册面证据表 + NPC 绑定门）优先，D2（新建 runtime 车道）必要性下降**。

**残余未坐实（低优先）**：176 中 30 件在客户端 `quest_script_monster.csv` 有 `killedByUser` 行；
其服务端等价面是否为 `drop_item/drop_prob` 尚需逐件对拍（不阻塞 D1）。

## 6. D1 执行记录（2026-10-03）：注册面证据表 + NPC 绑定门

D1 已落地（§5 首选落点）。**性质**：新增证据表 + 常设门，零生产行为变更（新资源只被测试消费）。

| 交付物 | 路径 |
|---|---|
| 证据表生成器 | `.agents/summary/quest-engine-native/p11-quest-ai-lane/emit_quest_ai_registrations.py` |
| 度量 + 冻结常量导出 | `.agents/summary/quest-engine-native/p11-quest-ai-lane/measure_binding_gate.py --emit-constants` |
| 证据表 | `src/main/resources/aion/data/static_data/quest/retail/retail-quest-ai-registrations.xml`（+ 同名 `.xsd`） |
| 常设门 | `src/test/java/com/aionemu/gameserver/questEngine/retail/QuestAiDialogBindingGateTest.java` |
| schema 门登记 | `RetailTableSchemaGateTest`（十八 → 十九表） |

### 6.1 口径修正：名匹配按真端 `_wcsicmp` = **大小写不敏感**

D1 首轮按「注册名原文相等」展开，得 37 任务 / 41 引用跨界；复核时发现该口径**与真端语义不符**，证据三条：

1. `NPCDB::Load` 的 `quest_ai_name` 走名→id 表 `FUN_140d18530`，二分比较两侧都是 `_wcsicmp`
   —— `server58-source/MainServer_Server64/fun/fun_249.cpp:3018`（调用点 `fun_040.cpp:9923`，case 0x886）。
2. ScriptDLL 的对话名 map 遍历同样用 `_wcsicmp` —— `ScriptDLL64.c:2075978/2076005`。
3. 自洽性反证：`10110/10522/10525/10528/10529` 注册 `L"LF6_WEATHA_E"`，而真端 `npcs.xml` 只有
   `LF6_Weatha_E`（npc 806075）。大小写敏感语义下这些注册**悬空**（真端不可服务），与真端可运行矛盾。

改口径后：全局 Quest-AI NPC 集 9998 → **10136**；跨界 37→**21** 任务 / 41→**25** 引用；
未注册任务 18 → **18**（不受影响，最稳）；另 6 个「注册名解析不到 npc」的行归零。
**两个口径的读数都已留档**（首轮 37/41 清单可从此前度量重放：`measure_binding_gate.py` 由 `fold` 索引改回原文索引即可）。

### 6.2 实测读数（大小写折叠口径）

- 注册面：**18787** 个 `FUN_180cb5920` 调用点（= `grep` 出现数 18788 − 1 个函数定义；正则零漏抓）→
  **7043** 个唯一 quest id、**8190** 个注册名；注册名折叠后 554 个映射到 >1 个 npc（真端 `_wcsicmp`
  同键 ⇒ 同一 NPC 集合共享任务脚本，语义如此）。
- XML 车道（`quest/definitions/quests/*.xml`，733 件）：695 件带 `<dialog>`/`<npc-complete>` 引用，
  去重 (任务, NPC) 对 **1657**；命中全局 Quest-AI 集 **1402**；证据表 733 行中 **636** 行有注册展开。
- 绑定判定：跨界 **21 任务 / 25 引用**（冻结）；真端无注册 **18 任务**（冻结）。

### 6.3 冻结语义（为什么不是「全部必须自注册」）

注册面是 **(注册名, 任务) 对**，而同一 Quest-AI NPC 承接多条任务，故本任务的对话位未必出现在本任务的
注册展开里。21 件全部是这一形态，抽样（NPC ↔ 真端注册任务 ↔ 本仓引用任务）：

| NPC | 名字 | 真端注册任务 | 本仓引用任务 |
|---|---|---|---|
| 204700 | Thor | 2514/2611/2619/2641/2646/24053 | 2633 |
| 204837 | Hresvelgr | 4502/4503/4705/4913/4915/4916/… | 4914 |
| 799522 | Shugo_IDNovice_1 | 18500/18501/18502/18508/18511/2850x | 18510 |
| 205320 | Inggness | 28207/28208/28213 | 28209 |
| 799763 | event_Sonaran | 80016/80017 | 80298–80309（12 件事件链） |
| 700141/700142 | dragonportal/portaltrigger | 2022/24016/1020 | 14016 |

**门形态**：证据表覆盖 XML 车道**恰好**（733 行 = 目录文件名 id 全等，行序升序）＋ 行 id ⊆ 全局 id 列
＋ 两张清单**逐元素冻结**（`FROZEN_CROSS_QUEST` 21 任务 / `FROZEN_UNREGISTERED_TASKS` 18 件）
＋ 绑定面命中数pin（1402）。新增跨界引用/未注册任务 **即红**；收缩须同批改常量（不做静默放行）。
负例对照已做：给冻结表加一条假项 ⇒ 门红并打印实际集合 21 条。

### 6.4 门态与残余

- 本轮实测：`QuestAiDialogBindingGateTest` **2/2**、`RetailTableSchemaGateTest` **2/2** 绿
  （`mvn -Dtest='QuestAiDialogBindingGateTest,RetailTableSchemaGateTest' test`，EXIT=0）。
- 相关目录聚焦套件（`questEngine.retail` + `questEngine.tablelane` + 两张 definition 目录门）
  = **287 例 / 4 类 6 红**，全部为本批**之前既有**的门禁债；已做基线对照：把本批文件移开、`RetailTableSchemaGateTest`
  回退到 HEAD 后重跑同 4 类，**同样 6 红**（`RetailNonIrAxisGateTest` 2、`RetailQuestAiNameGroupGateTest` 1、
  `ItemPlayFamilyRowInventoryGateTest` 1、`SimpleCollectItemRowAlignmentGateTest` 2）。
- 残余（未冻结、登记为后续取证面）：1657 条对话引用里 **255** 条落在 Quest-AI 集外（其中 **67** 件任务的对话位
  **全部**不在集合内）；「注册名 → npc id」折叠歧义 554 名；18 件未注册任务仍待逐件裁决（P10 §34 件裁定的子集）。
- 证据表新鲜度：生成器**不在 CI**（依赖真端 `ScriptDLL64.c`/`npcs.xml`），门只保证「行集与 XML 车道全等 +
  冻结面不漂移」；重出表须手工跑 `emit_quest_ai_registrations.py --emit`，读数用 `measure_binding_gate.py --emit-constants` 复核。
