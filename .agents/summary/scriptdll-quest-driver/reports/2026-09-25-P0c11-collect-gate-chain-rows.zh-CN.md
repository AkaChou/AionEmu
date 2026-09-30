# P0c-11：SimpleTalk 阶段门链行 10 行采纳退役 + collect_item 通道（TALK_CHAIN 残组续波）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 日期：2026-09-25 ｜ 切片：P0c-11 ｜ lane：SimpleTalk ｜ 前序：P0c-10o（4601/5554 快照）

## 交付

1. **collect_item 门物品通道（builder 侧）**：
   - 新索引 `item_name_index.tsv`（128163 条，`p0c11_build_item_name_index.py` 生成，与生产
     `RetailItemNameIndex` 同口径：name_desc 小写、首个命中）；
   - `synthesize_canonical` 扩展 `collect_gates`：门物品解析通道二选一——阶段发物符号
     （give_itemN ↔ quest_data work_items，10i 通道）或真端 quest.xml `collect_itemN` ↔
     item_name_index（P0c-11 通道，判例 1932：quest_1932a↔182206008 与老 XML SETPRO1 门同物）；
   - **门形状按客户端 select6 在册分派**（页链证据）：有 select6 → CHECK 按钮对（判例 4056，
     39/20002 双按钮 + priority 0/1 + select6 失败页）；无 select6 → SELECT_QUEST_REWARD 路由门
     priority 0/1（判例 3204/3340，10o 形状，priority 1 回选择页）。
2. **裁定与退役**：10 行（1932/3092/3340/3547/11304/24152/14201/14121/24121/24242）ADOPT 退役；
   逐行差异定性全部 真端对、XML 错（裁定表 `p0c11-collect-gate-adopt-decisions.tsv` 带 per-row
   basis）：24121/24242 老 XML 缺 talk_npc2 阶段段；3092/3340 老 XML reward var0 混用任务书行数轴
   （canonical K 轴 = 10i 判例 1183 先例）；14201 老 XML 多两路 NPC_START（talk NPC 也可接，
   真端表 acquired 唯一）；3547/14121 老 XML 阶梯缺段。
3. **登记/指纹**：drift 10 行翻转（退役时点冻结 DIFF）；链行冻结指纹 255 → **265**（+10 新行；
   3209 一行受控演进：门路由统一 priority 0/1 对，消除死按钮，冻结头注记录）；catalog −10。

## TALK_CHAIN 52 残组终分桶（本片普查结论）

52 行 = **10 本片采纳** + 10 物件哨兵（NO_START_OBJECT，判例 1323 保留）+ 11 既有机制归档
（SHAPE_UNSUPPORTED 1 / REPORT_VIEW_SYNC_DIFF 2 / NPC_COMPLETE_META_MISMATCH 4 /
COMPLETE_PARAM_DIFF 2 / NO_START_OBJECT 1 + 轴分歧 1）+ **21 canonical 通道缺口**
（work_items 通道缺失：18035/18807/18809/21070/21460/24120/28035/28807/29070/29071 + 本片
未入选 10 行——keep 维持，等 collect/give 符号通道补全后续波）。TALK_CHAIN 桶 52 → 42 KEEP。

## 结论（实测，快照时点）

- 全库 retention：**6224 = 4611 RETAIL_TABLE + 1613 XML_RETENTION**（lane 口径，并行 DD wave
  同窗口又落 10 行；本片净贡献 +10）；
- **verify_retirement = 1603/1603/4611，sum 差 10** = 并行 DD lane 的 10 行
  FAMILY_PENDING:DataDriven（p5 裁定未翻转，已归账，同 10m/10n 先例）；
- 产物一致性：10 退役 id catalog 零残留、XML 零在盘；登记表 1366 路由再生成；指纹 265 冻结。

## ⚠️ 并发碰撞记录（本轮重要事件）

本片收口阶段，**并行 lane 在共享文件 `RetailSimpleTalkDefinitionCompiler` /
`RetailClientDialogExits` 落了新改动**（23:22/23:29，SELECT5_CHECK/SELECT2_CONTINUE 词汇 +
reportFlowChain 自合成 CHECK 按钮对 + 链路径三段后处理），与本片同窗口冲突：

- **回归面**：16 条链行（含本片 10 行 + 1909/1971/3208/3209/4208/11077/21081 等）在新代码下
  `COMPILATION_FAILED`（`Range [0, -1)`，先于 23:29 编辑同一登记表可编译；单步行不受影响）；
  `RetailSimpleTalkGateTest` drift 同步与 `ChainGate` 回放保真随之红；
- **判定**：碰撞源在 lane 的在途改动（对同一 `reportFlowChain` 区域的重构），非本片引入——
  本片对拍在 23:0x 全绿后才出现，且单步路径复验正常；
- **处置**（按并行不触碰纪律）：未回滚/未修补对方文件；本片产物（登记表/指纹/裁定表/
  retention/catalog）落盘自洽；门禁红状态如实登记，待对方 wave 收口后复绿复验；
- **教训**：共享编译器文件的多 lane 并行需要声明在途文件清单（台账 §阻塞已有先例），本轮
  双方都在 `reportFlowChain` 动刀——后续切片开工前应先查文件 mtime 与对方在途标记。

## 未验证 / 下一步

- 门禁复绿：等并行 lane 的 reportFlowChain 重构收口后复跑 SimpleTalk 3 + ChainGate 2 +
  Ownership 4（预期本片 10 行回绿；6 条 3209 族回归行需 lane 侧修 guard 或按其新设计
  重转写登记表）
- 运行时行为 / 客户端目检：需启动服务授权，未执行
- SimpleTalk 余量：TALK_CHAIN 42 KEEP（21 canonical 通道缺口续波）、SENTINEL 60、CLIENT_ROUTE 73、
  名字未解 27、CUTSCENE 无触发 12、craft 28
- 其他族：SimpleHunt gap 47 / reject 108；DataDriven 随并行 lane

---

## 【同日收口更新】碰撞已解，门禁全绿

并行 lane 23:53 落下 reportFlowChain 收口改动后，16 条链行全部恢复编译；其重构将 24 行 IR
规范化（stage-give 族 + 本片 p0c11 批）。复验流程：12 行探针（本片 10 + 1909/3209）编译通过、
本片 10 行 DIFF 轴与退役裁定完全一致、单步路径不受影响 → 二次重冻结指纹 265（头注记录两次演进）。

- 门禁：**SimpleTalk 3/3 + ChainGate 2/2 + Ownership 4/4 全绿**
- **verify_retirement = 1603/1603/4621 sum=6224 — OK**（并行 DD lane 的 10 行 FAMILY_PENDING 已
  裁定归账，全库精确闭合）
- 全库：**6224 = 4621 RETAIL_TABLE + 1603 XML_RETENTION**
