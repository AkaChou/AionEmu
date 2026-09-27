# P0c-22：Haramel 三行裁定 + 交互物门通道——chest 箱不在 quest_use_item 合同域

> 日期：2026-09-26 ｜ 切片：P0c-22 ｜ lane：SimpleTalk（另在飞 DataDriven）｜ 前序：P0c-21（归属桶词汇）

## 交付

1. **三行 ADOPT_RETAIL（18505/18509/28509，Haramel 副本采集任务）**：
   - **空报告失败应答**：exits 三行都声明 SELECT6 → 编译器 hasFailurePage 分支发 SELECT6(2716)，
     与判官原断言一致（非分歧，实跑确认）。
   - **报告 NPC 关窗应答（本轮裁定核心）**：客户端 1008 按钮全页字面 = **"结束对话"**
     （page-action-map.csv 跨任务一致）→ 族形 `reportNpcExit` 的 CloseDialog 是字面建模；
     遗留 XML 的 ShowQuestSelectionDialog(SELECT_QUEST) 是"关窗后回任务选择页"的**未声明
     行为推断**。判官 `emptyReportFinish` 改锁 CloseDialog（实跑确认）。
   - **chest 箱反转（本轮关键更正）**：18509/28509 掉落 IDNovice_WoodenBox=**700853，
     ai="chest"**——`QuestInteractionObjectValidator.validateCatalogDrops` 只对
     ai=**quest_use_item** 的掉落要求 ACTION_ITEM_USE 门；chest 开箱走 chest AI 流，
     **不需要任务路由门**。判官 CASES 的门断言（XML 时代手制门）改按 `needsGate` 条件化
     （questGate 工厂 = quest_use_item 对象；chest 箱 = false），并在专属测试里**反向锁死
     chest 无门**。
2. **交互物门编译器通道（prod 代码改动，当前零触发）**：`RetailSimpleTalkDefinitionCompiler`
   新增 8 参 `compile` 重载（旧 7 参签名委托 `RetailQuestUseItemNpcs.empty()` 保持 lane 文件
   兼容）——真端 drop_monster 解析为 quest_use_item 交互物时，与 CollectItem 规范形同构发射
   USE_OBJECT + CanAct(ACTION_ITEM_USE) started 自环。**与 validator 判据精确同构**（登记域
   = quest_use_item），影响面普查已采纳 SimpleTalk 行零命中；也是 1141 同构缺口的未来解锁件。
   驱动 `compileSimpleTalk` 切换新签名（`interactionObjects` 字段本就在手）。
3. 落地：裁定表 `p0c22-haramel-decisions.tsv` + b1 元组接线 + 外科手术清单补丁（3 行）+
   XML 退役×3 + catalog −3 + target 四件同步。

## 验证（实测）

- **判官绿**：`QuestHaramelItemCollectingRegressionTest` 4/4（六 CASES 含四个已采纳族形
  先例 + 18509/28509 专属 + 18505 专属）。
- **族门**：SimpleTalkGate 3/3 + ChainGate 2/2 + InteractionObjectContract 2/2 +
  PlayerQuestRewardPort 23/23 全绿（编译器改动后指纹无漂移——通道零触发实证）。
- **verify_retirement：catalog=1438 directory=1438 retired=4786 sum=6224 OK**（lane 同窗
  又回退 2 行属其震荡，宇宙一致）。
- **T2（gates/T2-052838.log）**：92 tests，唯一失败 = 既有 client-contract count=50 集
  （与基线**逐条 diff 相同**，三行不在清单）；ownership 门本轮已过（lane 已收口）。

## 结论（实测）

- SimpleTalk 族：**2223 = 2061 RETAIL_TABLE + 162 XML_RETENTION**（本片 3 行翻转）；
  CLIENT_ROUTE 22 → **19**；本车道采纳累计 51 → **54**
- 全库：catalog 1438 = 目录 1438；retired 4786（含 lane 窗口震荡）；verify 6224 OK
- 五类分歧的"掉落/交互物语义"面：SimpleTalk 族侧通道已建（chest/quest_use_item 判据澄清）；
  30312（GROUP 掉落 scope）仍待裁
- 本片净变更 = +3 行采纳；prod 改动 = 编译器重载 + 驱动一行（零行为变化实证）；判官 1 文件

## 判例（勿重推）

- **交互对象合同域 = ai 精确匹配 quest_use_item**（`validateCatalogDrops` 首行过滤）：
  chest/其它 ai 的掉落物**不要求** ACTION_ITEM_USE 门——"是交互物"与"在合同域内"是两回事，
  门断言前先查 npc 模板 ai 属性（700853 chest vs 700833 quest_use_item 同在 Haramel）。
- **客户端 1008 = "结束对话"是全页一致的字面**：关窗后应答以 CloseDialog 建模（族形）；
  ShowQuestSelectionDialog(SELECT_QUEST) 只在接取流 FINISH_DIALOG（acceptFlow 已锁）与
  客户端明示行为处使用，不作未声明推断。
- **编译器扩展用重载保兼容**：多车道共享的 compile() 签名变更会炸不可触碰的 lane 门禁文件——
  旧签名委托新签名（empty registry = 零行为变化），仅驱动切换。
- 判官先红后改的顺序教训：门断言写反（chest 期望有门）时，实跑红行号直接暴露——先跑再定
  裁定方向，比静态推演快。

## 下一步

- CLIENT_ROUTE 19 续裁（2150 完成路由聚合 / 30312 掉落语义 / 30315 认证计数 /
  journal-stale-row 80290+1351+1526 / 事件轴 80016 / 固守 1141+26930——1141 现在可经
  本通道复験）
- 五类分歧余三面（事件轴/selectable-reward/journal 轴）；TALK_CHAIN 21 + CRAFT 28
- 运行时行为与客户端目检：需启动服务授权，未执行
