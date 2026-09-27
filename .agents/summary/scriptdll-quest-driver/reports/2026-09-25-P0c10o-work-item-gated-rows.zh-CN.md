# P0c-10o：SimpleTalk 工作物品门报告流 5 行采纳退役（ITEM_CHECK_UNRESOLVED 桶归零）

> 日期：2026-09-25 ｜ 切片：P0c-10o ｜ lane：SimpleTalk ｜ 前序：P0c-10n（4586/5554）

## 交付

1. **生产代码（item_check 工作物品门形状落地）**：
   - precheck：`RETAIL_ITEM_CHECK_UNRESOLVED` 收窄——item_check=1 且无 collect_item 时，若
     `give_item` 符号经 quest_data work_items 通道可解析（与接取发物同通道同物）则放行；
     两通道皆不可解才拒绝；
   - `reportFlow(rewardNpc, gate)` 带门重载：门落在 `SELECT_QUEST_REWARD` 路由上——
     **priority 0** 成功（HasItem → REWARD，after = Sync + 开奖励窗）+ **priority 1** 失败
     （无条件 → started，after = 开选择页 SELECT_QUEST）——与老 XML 3204/80269 逐字对位；
   - `workItemRequirement(entry)`：门物品 = give_item 同物（数量取符号列缺省 1）；
   - 与判例 4056 形状互斥并存：有 collect_item → CHECK 按钮对 + select6 失败页；
     无 collect_item → 门路由 + 选择页回退。
2. **门禁对齐**：`RetailSimpleTalkGateTest.hasReport` 增 P0c-10o 通道——`hasWorkItemGatedRewardSelect`
   不变量（priority 0/1 结构 + 门物品 ∈ metadata.questWorkItems 断言）；删除原
   「collect_item 元数据为空」一刀切拒绝。
3. **裁定与退役**：5 行（3204/3337/4204/80269/80271）ADOPT 退役。

## 证据链（客户端形状三面印证）

- **真端表**：5 行全部 `give_item` + `item_check=1`，reward NPC 异于接取 NPC（事件日常为 event_*）；
- **真端 quest.xml**：`quest_work_item1` 与 give_item 同物（3204: quest_3204a↔182209085；
  80269: quest_80269↔182215198），**无 collect_item**——校验目标就是接取发的文档；
- **客户端页链**（quest-dialog-pages.csv）：5 行 select5 页后**直接** select_quest_reward1，
  **无 select6/检查按钮页**；对照 collect 行 4056 有 `select6`（2716）——两种形状的客户端证据分立；
- **老 XML**：3204 系已在接取路由发物（give-item），报告侧 SELECT_QUEST_REWARD 带同物
  HasItem/RemoveItem 门 + priority 0/1 对——退役前对拍逐字等价。

## 对拍（探针 `RetailTalkWorkItemProbeTest`，已删，源存 .java.txt）

- 3204/3337/4204：`OK:DIFF node sets differ ... REWARD/1→REWARD/0`——**纯投影差**（QE-051），
  门路由与老 XML 精确相等；
- 80269/80271：门路由相等 + 额外 `extra=FINISH_DIALOG(1008) CloseDialog`——10m A 类判例
  （acquired≠reward 的 canonical 关窗边，老 XML 没有）；
- 全部 5 行 = 已裁定类别组合，零新增差异轴。

## 排障记（两处自坑，已修）

1. **reportFlow 无限递归**：带门重载 gate 空时委托 1 参版本、1 参版本又调 2 参版本 →
   `RetailSimpleTalkGateTest` StackOverflow。修复：2 参版本自足（gate 空走原canonical 形状）。
   教训：重载委托链必须单向。
2. **门禁不变量滞后于编译器形状**：语义门禁的 `hasReport` 原把「item_check 必须 collect_item」
   写死，10o 形状落地即红（3204 系 10 条问题）。修复：门禁增 `hasWorkItemGatedRewardSelect`
   通道（结构不变量 + 门物品与 work-items 交叉断言）。教训：编译器新形状与门禁不变量同片交付。

## 结论（实测）

- SimpleTalk 族：**2223 = 1937 RETAIL_TABLE + 286 XML_RETENTION**（ITEM_CHECK_UNRESOLVED 桶
  归零：5 → 0）
- 全库 retention：**6224 = 4601 RETAIL_TABLE + 1623 XML_RETENTION**
- 门禁：`RetailSimpleTalkGateTest` 3/3 + `RetailSimpleTalkChainGateTest` 2/2 +
  `RetailOwnershipGateTest` 4/4 全绿
- **verify_retirement = 1625/1625/4599 sum=6224 — OK**（并行 DataDriven lane 的 8 行裁定已落，
  10m/10n 期间挂起的归账差归零——三道归账门禁 ownership/manifest/verify 全部复绿条件已满足）

## 未验证 / 下一步

- 运行时行为（接取发文档→报告门校验→完成清理）与客户端目检：需启动服务授权，未执行
- SimpleTalk 余量 286：TALK_CHAIN 52 + NO_START 7 + COMPOUND 5 + NO_ROUTES 1（链行战役续波）、
  CRAFT_AXIS_UNDECLARED 28（等真端 craft 轴来源）、SENTINEL_NO_GRANT_PATH 60、CLIENT_ROUTE 73、
  CLIENT_REPORT_VARIANT 17、名字未解 27、CUTSCENE 无触发 12、复合名 4
- 其他族：SimpleHunt gap 47 / reject 108 / spawn 2；DataDriven 随并行 lane 续波
