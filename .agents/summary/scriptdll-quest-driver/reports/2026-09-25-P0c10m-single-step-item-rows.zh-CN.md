# P0c-10m：SimpleTalk 单步接取发物 116 行采纳退役（TALK_ITEM 桶收口）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 日期：2026-09-25 ｜ 切片：P0c-10m ｜ lane：SimpleTalk ｜ 前序：P0c-10l 族终收口（4421/5554）

## 交付

1. **生产代码（真端 give_item 轴落地）**：
   - `RetailSimpleTalkTable.Entry` 增加 `giveItemSymbol`（解析 `give_item`/`give_item1` 符号列）；
   - 新通道 `RetailQuestWorkItems`：quest_data.xml `quest_work_items` 只读视图（符号 → 物品 id，
     `first(symbol, questId)` 单符号行取首项；secure-processing + disallow-doctype-decl，任务缺席返回空集）；
   - `RetailSimpleTalkDefinitionCompiler`：precheck 增 `singleStepGrantOnly`（单步 && 非 remove && 有符号），
     绕过链路由要求；`build()` 接取流挂 `acceptGiveItemActions(entry)` → `GiveItem(itemId, count)`。
2. **裁定与退役**：116 行 ADOPT 退役（`p0c10m-item-decisions.tsv`，四类逐行 basis）+ 46 行 KEEP 维持
   （41 `cutsceneid1` 缺 movie 触发证据 + 5 `item_check` 真端 quest.xml 无 `collect_item` 声明）。
3. **登记翻转**：`retail-simple-talk-drift.tsv` 162 行翻转（116 → DIFF:NODE_PROJECTION 106 /
   DIFF:TRANSITION_SET 10；46 → 精确拒绝码 RETAIL_TALK_CUTSCENE 41 / RETAIL_ITEM_CHECK_UNRESOLVED 5）；
   `build_retention_list.py` 新增 p0c10m 裁定层 + 证据字段溯源到具体裁定表文件名（原硬编码 p0c10h 名）。

## 对拍与四类差异定性（全部 真端对、XML 错）

探针 `RetailTalkItemProbeTest`（已删，源存 `.java.txt`）跑 162 行：116 编译通过 + 老 XML IR 对拍，
差异逐行定性（`p0c10m-routediff-probe.tsv` 全文）：

| 类 | 行数 | 差异 | 裁定依据 |
|---|---|---|---|
| 投影差 | 106 | REWARD var0：老 XML 1 vs 真端 0 | QE-051：领奖投影 = 客户端任务书末行行号；单行任务为 0，老 XML 指向不存在的行 |
| A 报告侧关窗边 | 3（19001/19003/29001） | 真端多 `START→TalkToNpc[rewardNpc, FINISH_DIALOG(1008)]` 自环 CloseDialog | canonical 族形状（`reportNpcExit`，acquired≠reward 时生成），与 1803 已退役行共享同一调用点；老 `npc-report` 展开无关窗边（`expandNpcReport` 仅 QUEST_SELECT/SELECT_QUEST_REWARD） |
| B 老物品门冗余 | 2（2667/18601） | 老 XML 多 20002/10255 HasItem/RemoveItem 路由 | 真端行无 `item_check`（形状权威）；完成清理由规划器 `appendCompletionWorkItemCleanup` 依 retail quest.xml `quest_work_item1`（18601=quest_18601a、2667=doc_quest_2667a）在 COMPLETE 同事务无条件执行 |
| C 老 XML 从未发物品 | 4（3921/4921/28601/29051） | 老接取路由无 GiveItem（全 XML 无 GiveItem、无 handler） | 真端 `give_item` 为形状权威（3921=ITEM_QUEST_3921A 等），编译器经 RetailQuestWorkItems 通道发放——老定义物品从未发放，属老 XML 缺陷 |
| D 老 XML 丢奖励 | 1（29003） | 老 XML 无 `GrantReward[ITEM 125020014 ×1]` | retail quest.xml 29003 声明 `reward_item1_1=ac_head_d_n_u0_q_10c` + `reward_item1_2=shopmaterial_all_002a 30`，老 XML 只落了后者 |

## 证据

- `p0c10m-talkitem-ids.txt`（162 目标行）、`p0c10m-talkitem-probe.tsv`（116 OK + 46 REJECTED 分类）
- `p0c10m-routediff-probe.tsv`（10 路由差行 2400 字符全文 diff）、`p0c10m-drift-live.tsv`（活体分类导出）
- `p0c10m-item-decisions.tsv`（116 ADOPT，逐行 basis）、`p0c10m_update_drift_registry.py`（翻转脚本，四重前置断言）
- `p0c10m_retire_item_rows.py`（退役脚本：retention 两副本 RETAIL_TABLE/OK 断言 + 漂移已翻转断言 +
  驱动接线断言 + XML 在盘断言 → unlink 116 + catalog 移除 116）
- 零售源核对：`Quest_SimpleTalk.xml` 2667/18601/29003/29051/3921 行、retail `quest.xml` 18601/2667/3921
  work-item 声明、29003 奖励段、`QuestDialogAction` 枚举（1002/1003/1008/1009/10255/20000/20002 语义）
- 老引擎语义锚点：`QuestMutationPlanner.appendCompletionWorkItemCleanup`（完成同事务清 questWorkItems，
  对齐旧引擎 `QuestService.setFinishingState` 零售行为）

## 结论（实测）

- SimpleTalk 族：**2223 = 1919 RETAIL_TABLE + 304 XML_RETENTION**（本片 +116/−116）
- 全库 retention：**6224 = 4573 RETAIL_TABLE + 1651 XML_RETENTION**（向 5554 推进 116）
- 门禁：`RetailSimpleTalkGateTest` 3/3 绿（语义不变量 + 漂移登记同步）；`RetailSimpleTalkChainGateTest`
  2/2 绿（单步行不入 chainSteps 指纹目标集，255 行冻结指纹未受影响，校验模式通过）
- catalog：解析 OK，1643 条定义，116 退役 id 零残留、46 KEEP id 零误删，无重复 id

## 未验证 / 归账

- **verify_retirement = 1643/1643/4573，sum 6216 ≠ universe 6224（差 8）**：8 行
  （13945/15306/15316/18994/23945/25306/25316/28994）系**并行 DataDriven lane** 已删 XML + catalog
  但 p5 裁定表未含对应行（retention 仍标 FAMILY_PENDING:DataDriven）——并行 lane 在途状态，
  本 lane 未触碰其文件；本 lane 自身 delta 恒等（catalog −116 ↔ retired +116，6224−8=6216 精确闭合）
- `QuestDefinitionCatalogManifestTest` 6 errors：15548/25548 NO_NODES（磁盘 XML 零节点）——并行 lane
  在途修复文件（已知在途清单），非本片引入
- 运行时行为（接取发物→交付→完成清理全链）与客户端目检：需启动服务授权，未执行

## 下一步

- 并行 lane 收口后复跑 verify_retirement + RetailOwnershipGateTest + manifest 门禁（预期 8 差归零）
- SimpleTalk 余量：TALK_CUTSCENE 53（41 行缺 movie 触发证据，按 10k 判例补证）、ITEM_CHECK_UNRESOLVED 5、
  TALK_CHAIN 52 + 复合行
- 其他族：SimpleHunt gap 47 / reject 108 / spawn 2；DataDriven 余量随并行 lane
