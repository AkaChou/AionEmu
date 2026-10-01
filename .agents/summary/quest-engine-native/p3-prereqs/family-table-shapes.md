# 家族表形状审计（P3-P6 数据层前置 + P1 范围增补依据）

> 批次：2026-10-01 深夜（只读；对象 = repo 表 `src/main/resources/aion/data/static_data/quest/retail/`，UTF-8，
> 行元素 `<id id="N">` 属性形）。定位：①全家族列清单/填充率（NativeQuestTableLoader 扩展与各族切换矩阵输入）；
> ②跨表 id 相交门禁预检（owner 门禁查过表↔XML，表↔表此前从未查过）；③P1 范围内特殊行组（声明书 §1.1 增补）。
> 同目录 SimpleTalk codegen 流水线考古（DLL 侧）另行产出，两份互不重叠。

---

## 1. 跨表 id 相交：全部不相交（PASS）

7 张家族表（SimpleTalk 3152 / SimpleHunt 1865 / SimpleSerialHunt 16 / SimpleCollectItem 262 /
SimpleUseItem 160 / SimpleItemPlay 43 / CombineTask 574）两两相交 = **零**。
CameraRegistry「同任务只注册一次」与 owner-disjoint 的**表侧**前提在数据层成立；每表 distinct_ids = rows
（表内无重复 id）。这是 go-live 门禁预检的第一次全绿读数（QE-112 落地后随 P0a 重冻复跑）。

## 2. 各族形状速览（列 → 填充数(率)）

### 2.1 SimpleTalk（P3 主对象，3152 行，root=quest_simpletalks）

- 全 100%：`acquired_npc_name` + `reward_npc_name`（双 NPC 结构无条件成立）。
- **链分布：2684 行（85%）零 talk_npc** = 直交形（接取 NPC → 交付 NPC，无中继）；talk_npc1 468（14%）、
  talk_npc2 193（6%）、talk_npc3 68（2%）。
- **`item_check` 63%（1988 行）**——高频物品前置列（record +0x2b0），P3 handler 必须建模，不许按小众列搁置。
- `con_quest` 492（15%）前置任务门；`give_item` 407（12%）接取发物；give_item1/2/3 ≤3%；remove_item1/2/3 ≤3%；
  `cutsceneid1`+`cs1_haction` 67/65（2%）。

### 2.2 SimpleHunt（P1 对象，1865 行，root=quest_simplehunts）

- count1/monster1 99%（缺的 4 行 = 休眠零计数行）；多段尾：stage2 23%、stage3 8%、stage4 2%、stage5 0.9%。
- talk_npc1 全表 47 行，其中 **retail-owned（P1 的 939 行内）21 行 + talk_npc2 1 行（14152，双中继）** →
  **P1 范围增补**（声明书 §1.1）。
- mentor_type/mentor_minlevel/mentor_maxlevel 全表 47/36/36——**retail-owned = 0**，不进 P1。
- retail-owned 特殊列：`con_quest` **85 行**、`cutsceneid1` 3 行（3016/4007/4014）、`give_item` 1 行（24115）。
- **缺 `reward_npc_name` 恰 2 行：80281/80283**——两行**不在 retention 任何家族**（无 owner）、不在休眠清单
  （有计数，非零计数休眠形）。P1 不路由（§2.4 注册≠路由）；但 **P8 go-live 按 resolver 表集优先会成为
  native-owned ⇒ 缺交付 NPC 将 fail-closed**。须提前处置（80xxx=活动段位，疑活动任务遗留行；处置建议：
  P8 前按活动任务子系裁定排除，或补真端交付声明）。

### 2.3 SimpleSerialHunt（P2，16 行）——与 P2 报告 §1 一致，不赘述。

### 2.4 SimpleCollectItem（P4，262 行）

- `object1..object4` = 采集目标（98%/8%/4%/3%）；**`party_drop` 30%（80 行）**——组队掉落列，P4 handler 必查；
  `reward_check` 5 行；give_item 6 行；talk_npc1 5 行；cutsceneid1 2 行。

### 2.5 SimpleUseItem（P5，160 行）

- **无 `acquired_npc_name` 列**——接取 = 使用物品（非 NPC 对话），接取路由按族分叉的设计输入。
- `use_item_name` 100%；talk_npc1 33%/talk_npc2 16%/talk_npc3 6%（用物后多段对话形占比高）；
  `remove_item1..3`（用后扣除）6%/3%/1%；give_item1..3 5%/2%/1%；`item_check` 3%。

### 2.6 SimpleItemPlay（P5，43 行）

- `use_item_name` 95% **且** `acquired_npc_name` 100%——用物接取与 NPC 交付并存形；`give_item` 79%
  （接取即发物）；talk_npc1 25%；give_item1/2 16%/11%；remove_item1/2 2%/9%。

### 2.7 CombineTask（P6，574 行）

- 制作形，与其它族全然不同：`combineskill`/`combine_skillpoint`/`recipe_name`/`product`/`task_npc` 全 100%，
  `give_component1` 100% / `give_component2` 26%。P6 需要独立行模型（配方/产物/材料），不复用 hunt 形状。

## 3. 结构结论（loader 扩展设计输入）

1. 公共列（acquired/reward_npc_name、talk_npc1-3、give_item*、remove_item*、emotion、cutscene*、con_quest、
   item_check）跨 5+ 族复用 ⇒ `NativeQuestTableLoader` 扩展 = **公共 Record 基模型 + 各族专属列扩展**，
   对齐真端「base parser + family parser」结构（P2 报告 §2.1 的列→偏移表即公共模型字段清单）。
2. useitem 族无接取 NPC ⇒ 接取路由按族分叉（P3 的 NativeAcquireRouter 设计输入：Talk/Item 两类入口）。
3. CombineTask 独立形状 ⇒ P6 行模型独立立案，不强行并入公共基模型。

## 4. 对切换批的输出

- **P1**：四组特殊行增补进声明书 §1.1（21+1 行 talk 中继 / 85 行 con_quest / 3 行 cutscene / 1 行 give_item）；
  22 行中继语义在族级对拍时以旧 IR 行为为过渡 oracle，真端口径可用其 codegen 条目抽查（全家族 codegen 已证，
  见 `../p2-prereqs/serial-hunt-progression.md` §7）。
- **P8**：80281/80283 处置（本文件 §2.2）。
- **P0a 重冻**：本审计的相交门与填充率随 QE-112 落地后复跑一遍（幂等脚本口径同本文件）。
