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
