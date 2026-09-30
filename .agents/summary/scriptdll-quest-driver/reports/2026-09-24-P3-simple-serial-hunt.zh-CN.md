# P3：SimpleSerialHunt 10 行 = 客户端链式阶梯落驱动（族全退役）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 口径：真端优先。客户端 `quest_monster.csv` 的链式 `Progress(SECTION_n<count; SECTION_(n-1)==count')`
> 门控是本族的形状权威——乱序击杀不计数，合成定义必须是**串行阶梯**而非并行计数网格。

## 1 交付

**生产代码**
- `RetailSimpleSerialHuntTable`（新）：真端 `Quest_SimpleSerialHunt.xml` 只读视图；每行至多五段
  `monster/count_first..fifth`（**单段可为逗号分隔多怪清单**，如 16991 的 8 职业变体），直接产出
  hunt 形 `RetailSimpleHuntTable.Entry`。
- `RetailClientHuntStages`（新）：客户端串行阶段契约登记 `quest_client_hunt_stages.tsv` 只读视图
  （段 / 计数 / 完整刷怪 id 集——含真端表未列的变体刷怪，如 16991 每段 18 只、18911 末段 5 只）。
- `RetailSimpleHuntDefinitionCompiler`：
  - 新增 `compileSerialChain(plan, stages, metadata)`：节点 = 链式前缀组合（前序段满、当前段计数
    0..count），击杀边只从首个未满段推进（`chainedCombos`/`chainedEdges`）；接取/报告/完成流与网格形
    完全同构；报告页 SELECT2 的 SETPRO1(10000) 按钮按客户端契约补路由（LEVEL 同步 + 关窗）。
  - 串行接取流采用"报告 NPC 安静"形状（未接态不发 SELECT1 页，与现行 XML 一致，避免触发客户端契约
    无按钮路由的接取页）。
  - `completeFlow` 对可选中任务改为只发"可选项数"条确认路由（P1b 口径对齐，命中 18912/28912）。
- `RetailQuestDriver`：装载串行表 + 阶段契约登记，新家族分支 `SimpleSerialHunt`。

**证据资产（机器生成，入仓）**
- `quest_client_hunt_stages.tsv`（34 段，刷怪名 id 解析 100%：`p3_client_hunt_stages.py`）。
- 漂移登记 `retail-simple-serial-hunt-drift.tsv`（10 行）；裁定 `p3-serial-hunt-decisions.tsv`
  （10 行 ADOPT_RETAIL，`p3_build_serial_hunt_decisions.py`）；冻结指纹
  `retail-simple-serial-hunt-ir-fingerprints.tsv`（10 行）；退役证据 `p3-retired-serial-hunt-evidence.tsv`。
- `build_retention_list.py`：`IMPLEMENTED_FAMILIES` + `SimpleSerialHunt`，三份裁定文件扩为四份合并。

## 2 证据

- 客户端契约：`quest_monster.csv` 10 任务全部为链式 SECTION 门控（`SECTION_n<count; SECTION_(n-1)==…`）；
  例如 18912 = `SECTION_0<2` 后 `SECTION_1<1; SECTION_0==2`——旧 XML 的并行网格与此相悖。
- 阶段解析：34 段 / 刷怪名单 id 解析 100%（`stages=34 unresolved=0`）。
- 漂移：10 行全 DIFF（轴类别 = 并行网格节点/路由被链式阶梯取代、EnterWorld 旧档修复路由（13918/23918）、
  wrap 物品完成奖励（18911/28911，真端 quest.xml 权威）、报告 NPC 的 NONE 态出口差异），全部裁定
  ADOPT_RETAIL。
- 退役：`p3_retire_serial_hunt_xml.py` → `moved=10`；`verify_retirement.py` →
  `catalog=3681 directory=3681 retired=2543 sum=6224 — OK`；doc links `replaced=10 dangling=0`；
  合同台账复跑（cap 38 行不变）。
- 门禁：T1 七类 **25/25 绿**（P0c 协同后扩展集 **30/30 绿**）；T2（10 id）**68/68 绿**
  （`gates/T2-085712.log`）；T3 全树首轮 **5F**（见 §5 根因）→ 修正后复跑
  **1952 例 / 0F / 0E / 1 skipped / BUILD SUCCESS**（`gates/T3-p3-run2.log`，新增 5 例 = 串行门禁）。

## 3 结论

- **可删 XML 数 = 10**（13918/16991/18911/18912/23918/26991/28911/28912/30600/30610）：五条充分条件
  逐条满足——元数据对拍一致；进度事件与 DLL 语义一致（0x11 击杀 + 客户端链式门控 = 串行阶梯）；
  合成 IR 与客户端契约逐按钮对齐（契约门禁驱动的两轮补齐：SELECT1 页按钮、SETPRO1 报告按钮）；
  家族门禁 5/5 绿；无未闭环口径冲突。
- SimpleSerialHunt **族收口：10/10 全部真端驱动**（生产宇宙内无保留）。
- 累计退役 2543/5554；RETAIL_TABLE/OK **2543**。

## 4 对拍结果

- **元数据**：全树元数据门禁绿；18911/28911 的 wrap 物品按真端 quest.xml 在完成时发放（生产 XML 的
  fixed "0 1" + extended 为历史口径，漂移登记承载）。
- **进度事件（DLL）**：0x11 击杀族 100% + 客户端链式门控 = 串行阶梯；阶段刷怪集以客户端 CSV 为权威
  （真端表子集 + 变体补全）。
- **IR**：10 行漂移全登记；冻结指纹 10 行守等价证据；门禁的链式推进校验（乱序边 = 0）逐任务断言。
- **客户端**：QuestClientContractGateTest 绿（两轮契约驱动补齐：SELECT1 接取页按钮、SETPRO1 报告页按钮）。

## 5 未验证 / 门禁

- T3 首轮 5F 根因 = **本切片补丁缺陷**：串行 SETPRO1 报告按钮路由经 python 全量替换被同时插入
  `buildSerialChain` 与网格路径 `build()`，每条 hunt 定义多 1 条路由（1102：35→36，286 行指纹全量位移、
  hunt 三门禁 5F）。修正为仅串行路径持有后 hunt 三门禁 + 串行门禁 **18/18 绿**、T3 复跑全绿。
- **并行会话协同（P0c）**：本切片期间并发会话落地 `RetailSystemGrantDispatcher` + `NpcFactions`
  发放接线 + 共享 `RetailGrantKind`（M5-b3x 挂起的 SystemGrant 分发接线阻塞项由其收口）；
  两侧改动共存验证 T1 30/30 绿。
- 实机验收：串行任务的"当前该打哪只"任务书高亮与乱序不计数行为，待用户执行。
- 16991/26991（防守向导双段各 4 + 2）的段间推进体感，待实机。

## 6 阻塞与决策项

| 项 | 处理（不停等） |
|---|---|
| SystemGrant 分发接线（M5-b3x 起挂起） | 等实机抓包；不阻塞 |
| 13918/23918 的 EnterWorld 旧档修复路由被移除 | 旧存档迁移面按真端口径移除，漂移登记留证；存量玩家进度按新阶梯重读 |

## 7 下一步

P3 继续：SimpleUseItem 104（全族 160/160 无 select1 = 系统发放形状，复用 P0 哨兵模型 + npcfactions
星期位）→ SimpleItemPlay 15。复现：`python3 -B p3_client_hunt_stages.py` 与
`mvn -o test -Dtest=RetailSimpleSerialHuntGateTest`。
