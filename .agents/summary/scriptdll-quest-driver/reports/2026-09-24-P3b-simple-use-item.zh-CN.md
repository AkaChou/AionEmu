# P3b：SimpleUseItem 104 行 = 用物品接取规范形落驱动（102 退役 + 2 真端缺口）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 口径：真端优先。用物品接取族的规范形状 = `UseItem` → 接取确认窗 → 无目标对话接受/拒绝/关窗
> → 报告 NPC SELECT5 → npc-complete 确认 8..23。

## 1 交付

**生产代码**
- `RetailSimpleUseItemTable`（新）：真端表只读视图（`use_item_name` + `reward_npc_name`）。
- **道具符号映射规则（全族 104/104 唯一解析）**：`use_item_name`（如 `ITEM_QUEST_1107A`）去掉
  `ITEM_` 前缀转小写 = 物品 `name_desc`（`quest_1107a` → 182200501）。
- `RetailSimpleUseItemDefinitionCompiler`（新）：UseItem 接取（弹接取确认窗）+ 无目标对话
  （QUEST_ACCEPT_1=1002 → started + StartEligible；QUEST_REFUSE_1=1003 / FINISH_DIALOG=1008 → 关窗）
  + 报告（QUEST_SELECT → SELECT5 + 交付）+ npc-complete（预览窗 + 确认 8..23，可选项绑 8..n）
  + 领奖投影（QE-051）与领奖行修复路由。
- **交付双模式**（客户端 select5 页按钮为权威，新登记表 `quest_client_use_item_report.tsv`）：
  `REWARD`（99 行）= 1009 直接交付；`CHECK`（5 行：80482/80486/80554/80558/80612）= 39/20002 交付
  检查对（携带用物品本体 → 领奖；缺物 → 关窗；SELECT6 失败页的 FINISH_DIALOG 出口补齐）。
  CHECK 行的交付物 id 与退役前 XML 的 `npc-item-report` 逐一相符（182215419/182215444/182215579）。
- `RetailClientUseItemReport`（新）+ `RetailQuestDriver` 接线（新家族分支）。

**清单与登记（机器生成）**
- 漂移登记 `retail-simple-use-item-drift.tsv`（104 行 = 102 ADOPT + 2 NPC 缺口）；
  裁定 `p3b-use-item-decisions.tsv`（102 ADOPT_RETAIL，`p3b_build_use_item_decisions.py`）；
  冻结指纹 `retail-simple-use-item-ir-fingerprints.tsv`（102 行）；
  退役证据 `p3b-retired-use-item-evidence.tsv`；`build_retention_list.py` 消费（新增 UseItem 漂移分支：
  REJECTED 行 → XML_RETENTION，修正此前 generic 分支把它们误标 RETAIL_TABLE 的问题）。
- **退役 102 个 XML**（catalog 3681 → **3477**）；RETAIL_TABLE/OK **2747**。

## 2 证据

- 形状勘察：13 种 XML 签名（use-item 主导 81 行；NPC 接取/道具交付/choice 等变体 23 行）；
  `use_item_name` 解析 104/104；客户端 select5 按钮普查 = 99 REWARD + 5 CHECK。
- 漂移：104 行全登记（102 DIFF + 2 REJECTED）；等价 0（差异轴 = NPC_START/SELECT2_1/SETPRO1 等
  XML 过渡步骤被规范形状取代、领奖投影 QE-051、并行会话（P0c）同期的指纹基线位移）。
- 退役：`moved=102`；`verify_retirement.py` → `catalog=3477 directory=3477 retired=2747 sum=6224 — OK`；
  doc links `replaced=102 dangling=0`；合同台账 cap 38 行不变。
- 门禁：T1 扩展集 **50/50 绿**（含 collect/serial/useitem 三家族门禁 + 契约 + 归属 + overlay +
  Manifest + JavaHandler/CollectTurnIn 对齐批）；T2（8 id 代表）**105/105 绿**
  （`gates/T2-105625.log`）；T3 全树 **1957 例 / 0F / 0E / 1 skipped / BUILD SUCCESS**
  （`gates/T3-p3b.log`，唯一 skip = 既有 `QuestDialogMigrationEquivalenceTest`）。

## 3 结论

- **可删 XML 数 = 102**（全族除 30720/30723）。五条充分条件逐条满足：元数据对拍一致（work-item 轴
  除登记分歧外一致）；进度事件与 DLL 语义一致（用物品接取 + 1009/39 交付为客户端契约）；合成 IR 与
  客户端契约逐按钮对齐（契约门禁两轮驱动补齐：39 检查按钮、SELECT6 的 FINISH 出口）；家族门禁 5/5 绿；
  无未闭环口径冲突。
- **保留 2 行**：30720/30723（报告 NPC 名真端/本服均解析不到 → `RETAIL_REWARD_NPC_NPC_UNRESOLVED`，
  XML_RETENTION/SEMANTIC_GAP）。
- **P1 收口后的家族状态**：SimpleUseItem 104 = 102 驱动 + 2 缺口。

## 4 对拍结果

- **元数据**：全树元数据门禁绿；work-item 轴按登记分歧承载（真端 quest_work_item1 为符号、本服为 id）。
- **进度事件**：UseItem 事件 = 客户端"右键道具"手势；交付 = 客户端 select5 按钮（39/1009）。
- **IR**：漂移 104 行全登记；102 行 DIFF 轴全部落在已知类别；冻结指纹 102 行守等价证据。
- **客户端**：QuestClientContractGateTest 绿（两轮契约驱动补齐：39 检查按钮 + SELECT6 FINISH 出口）。

## 5 未验证 / 门禁

- **T3 全树暂不稳定（并行协调项）**：并行会话（P0c 扩展）于 10:47-10:53 在途重构 retail 包
  （新增 `RetailQuestAreaIndex`、改 hunt 编译器/driver/本切片 UseItem 编译器），两轮 T3 分别跑在
  不同中间快照上（45F → 71F，含其 driver 在途改动使生产视图丢失串行定义 13918）。
  本切片的家族门禁在最新代码上 **10/10 绿**（串行 5 + UseItem 5，直连编译器路径），
  扩展 T1 在其上一波快照 **50/50 绿**；全树复跑待并行会话收口后执行并对账。
- 实机验收：右键道具接取的确认窗与 CHECK 交付按钮的客户端表现，待用户执行。

## 5b 运行时修复（用户实机启动失败反馈）

用户实机启动报 `QuestInteractionObjectValidator`：quest 2119 的 catalog drop npc 700188 无 START
ACTION_ITEM_USE 路由。根因二重：
1. metadata 的 drop 映射做了**显示名族扩展**，把 1561 号光族箱（LF3_JewelBox_Q1561=700188）并进了
   2119 暗族箱（DF1_JewelBox=700127）的 drop 集——全库 2723 个 drop_monster 名单 100% 精确可解析，
   族扩展纯属过度合并 → 改为精确解析；
2. UseItem 编译器对有 drop 的任务（2435/1561 等）未发掉落箱对象路由 → 补 TALK + CAN_ACT
   （ACTION_ITEM_USE，START 态）路由。
修复后 `QuestInteractionObjectCatalogTest` **7/7 绿** + 扩展 T1 **57/57 绿**。
**实机验证（用户确认）**：服务端重启后引擎装载不再报错——修复经真实引擎启动闭环。

## 6 阻塞与决策项

| 项 | 处理（不停等） |
|---|---|
| 并行会话（P0c 扩展）同期退役 hunt 62 + talk 40 并扩 RETAIL 基线 | 已在 verify/门禁中共存验证（sum=6224 OK、T1 50/50）；两侧基线文件以最后一次门禁运行为准 |
| SystemGrant 分发（P0c 已收口） | 挂起项解除 |

## 7 并行会话冲突记录（16:15 更新）

`retail-simple-use-item-drift.tsv` 正被并行会话（P0c 扩展）循环覆写为其 2 行视图（仅 30720/30723 两行
REJECTED，丢失已退役 102 行的冻结证据）。本会话的完整 104 行版本已存于 `/tmp/ui-drift5.tsv`。
恢复命令：`cp /tmp/ui-drift5.tsv src/test/resources/quest/retail-simple-use-item-drift.tsv`。
两个会话需对漂移登记文件建立单写者纪律后再复跑 T1/T3。

## 8 下一步

P3c：SimpleItemPlay 15（43 行：41 主形状 + 2 `_faction_`）——同一流水线。复现：
`mvn -o test -Dtest=RetailSimpleUseItemGateTest`。
