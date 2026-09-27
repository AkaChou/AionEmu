# P0c-19：第六类"缺口"证伪——入口 NPC 镜像裁定，11003/80356/80365 ADOPT_RETAIL

> 日期：2026-09-26 ｜ 切片：P0c-19 ｜ lane：SimpleTalk ｜ 前序：P0c-18（三行 KEEP + 第六类定性）

## 交付

1. **第六类"单步 report 流发射缺失"证伪（P0c-18 结论的更正之更正）**：
   - P0c-18 翻转态直证时，探针打印与断言**全部过滤在单一 npcId**（镜像抄自遗留 XML 首块）
     上——11003 的 798933、80356 的 831815、80365 的 831827 **全部是接取 NPC**
     （798933=Phailos、831815=event_Scrooge_l_1、831827=event_Scrooge_d_1，npc 模板实证）。
   - SimpleTalk 单步编译的 `itemCheckReportFlow` 按真端表 `reward_npc_name` 发在**交付 NPC**上
     （798942=Strabon、831819=event_guarantee_1）——探针从未观察交付 NPC 路由，
     "报告流整体缺失"是**探针单 NPC 过滤伪影**。
   - 编译器代码路径（`build()` 315-331 行）从来就有单步 report 分支；真端 quest.xml 对三行
     都声明 `collect_item1/2` → itemRequirements 非空 → precheck 通过 → itemCheckReportFlow 必然发射。
2. **客户端强二证据（模板索引 start/end NPC 列）**：`legacy-quest-dialog-template-index.csv`
   对三行声明**接取/交付分离**：11003=798933/798942、80356=831815/831819、80365=831827/831819；
   真端表 acquired/reward 同对。**客户端契约自身就是非对称形**——遗留 XML 的对称双 NPC
   全形状（两个 NPC 都挂 NPC_START+NPC_REPORT+CHECK 对+npc-complete）= 手工漂移。
3. **三行 ADOPT_RETAIL + 判官镜像修正（与翻转同进同退）**：
   - 裁定表 `p0c19-entry-npc-mirror-decisions.tsv`（3 行，basis=M3D_DOWNGRADE_REVERSED，
     头注完整记录证伪链）；接线 `build_retention_list.py` b1 元组末位。
   - 判官 `LegacyTemplateMirrorRouteRegressionTest` 镜像改指交付 NPC（798942/831819/831819），
     action 统一 39（模板索引列；编译器 39/20002 双变体都登记，断言 39 即客户端契约形），
     双语裁定注释入码。
   - 退役落地 `p0c19_retire_entry_mirror_rows.py`（前置断言含判官镜像已改；删 3 XML +
     catalog 移 3 行 1462→1459）。
4. **清单落地纪律（风暴规避）**：`build_retention_list.py` 全量重生成会带入 DataDriven lane
   的震荡中间态（含把已采纳 13951/13967 打回 XML_RETENTION 的悬挂风险）——改用**外科手术
   补丁**：lane 一致基线 + 仅叠 3 行，三副本 md5 归一（b251349962586dbe15843d4cb05f9153）。

## 验证（实测）

- **判官绿**：`itemCollectingMirrorsUseTheClientOwnedReportAndTurnInProtocol` 对三行
  （改指交付 NPC 后）在生产视图实跑通过——真端编译在交付 NPC 上有完整
  QUEST_SELECT→SELECT5(2375) / 39 对（success→reward+窗5 / failure→SELECT6(2716)）形状。
- **verify_retirement：catalog=1459 directory=1459 retired=4765 sum=6224 OK**（无悬挂）。
- **T2 全量（gates/T2-043837.log）**：71 tests，2 失败**全部既有**——
  ① `legacyTemplateCloseControlsDoNotChangeQuestState` quest 1131（T3-003550/024858/032134/
  035139 等多份更早日志同在，行号 348→361 仅为注释行漂移）；② `QuestClientContractGateTest`
  count=50 BUTTON_WITHOUT_ROUTE（与 T3-035139 前置日志**失败集逐条 diff 相同**，三行不在清单）。
- **族门**：`RetailSimpleTalkGateTest` 3/3 + `RetailSimpleTalkChainGateTest` 2/2 +
  `RetailOwnershipGateTest` 4/4 + `QuestProductionStartupGateTest` 2/2 全绿。

## 结论（实测）

- SimpleTalk 族：**2223 = 2052 RETAIL_TABLE + 171 XML_RETENTION**（本片 3 行翻转）；
  CLIENT_ROUTE 31 → **28**；本车道采纳累计 42 → **45**
- 全库：catalog 1459 = 目录 1459；retired 4765（含 lane 本窗口退役）；verify 6224 OK
- **第六类分歧除名**：六大分歧类收敛为五类（前置派生/事件轴/selectable-reward/掉落语义/
  journal 轴）；prod 代码零改动——"缺口"从未存在，错在判官镜像转写
- 本片净变更 = +3 行采纳；判官镜像 3 项修正；裁定表 + 退役脚本 + 本报告

## 判例（勿重推）

- **探针/判官的 NPC 过滤即断言范围**：断言"某流缺失"前必须核对被过滤的 npcId 在该定义里的
  角色（接取 or 交付）——SimpleTalk 形状中接取流挂 acquired、报告/检查/完成流挂 reward，
  单 NPC 视图对另一侧永远"缺失"。
- **遗留 XML 对称双 NPC 形是转写惯性的产物**：模板索引 start/end 列是逐任务 NPC 分工的
  客户端权威；start≠end 时 XML 把全形状铺满两 NPC = 手工近似，按真端对、XML 错裁。
- **lane 风暴期禁全量重生成**：builder 全量跑会采纳 lane 输入源的震荡中间态（已采纳行被打回
  = 悬挂）；风暴期对清单做外科手术补丁（基线 + 本片行），双稳定后再全量对账。

## 下一步

- CLIENT_ROUTE 28 行续裁（同词汇逐任务归零）
- 五类既有分歧通道（前置派生/事件轴/selectable-reward/掉落语义/journal 轴）
- TALK_CHAIN 21 通道缺口 + CRAFT 28 续档
- 运行时行为与客户端目检：需启动服务授权，未执行
