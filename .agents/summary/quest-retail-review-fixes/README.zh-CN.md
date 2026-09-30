# 零售任务迁移审查修复


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

## 判据

- 13956/23956、13965/23965、16830/26830、16831/26831 等 8 个 DataDriven 任务：真端定义为 EnterWorld/EnterArea 接取，生产 XML 壳已退役删除，漂移登记已为 ADOPTED 且已具备 IR 指纹。此前在 retention 清单中残留为 XML_RETENTION，导致 RetailQuestDriver.verifyProductionCoverage 报错 missing=[...] (8) 阻断启动。现已通过 p5_build_datadriven_decisions.py 补齐纯 EnterArea 链形状支持，将全量裁定扩展至 1036 行，并同步三份 retention 清单至 RETAIL_TABLE DataDriven，消除启动阻断。
- 任务形状、物品与 NPC 取真端 `Quest_SimpleTalk.xml`、`Quest_SimpleUseItem.xml`、`data_driven_quest.xml` 和 `quest.xml`；对话页、可见按钮与任务书末行取 Aion 5.8 客户端提取的登记表。退役 XML 和 handler 只用于识别旧存档兼容状态，不作为新路由的主判据。
- SimpleTalk 的 SELECT5 检查家族生成动作 39/20002 的条件成功与低优先级失败边；去重键包括 priority。报告所需道具按真端 collect_item/工作物品及此前扣除记录取值。已经在上一阶段扣完工作物品的 1971 不生成空的失败边。
- 领奖行非零的链式任务补无源 EnterWorld 旧存档修复；11008 的非当前步骤 NPC 改显示无后续按钮的页，当前步骤 NPC 保留完整 1353/SETPRO1 页链。
- 80008/80009 的真端工作物品分别为 `quest_80008a`/`quest_80009a`（182214006/182214007）；客户端报告动作为 1009。报告时先检查并扣除物品；活动失效后的 LevelUp 执行弃任。
- 21217、1553、11072 所在回归测试读取生产 overlay；XML 保留清单按任务核对。静态扫描另发现 25608 退役 XML 的直读测试，现已换用生产 overlay；其真端七段与客户端七行继续由原测试锁定。DataDriven 混合链非最终击杀的旧领奖行恢复已扩展至小于客户端末行的存档，EnterWorld 与领奖 NPC 点击均可恢复。
- 并行迁移重生成 owner 后，26 个 DataDriven 行被误置为 `XML_RETENTION/FAMILY_PENDING`，但对应 XML 已退役、漂移登记均为 `ADOPTED`。补齐 `p5_build_datadriven_decisions.py` 的 EnterArea/Hunt 客户端行形状，重生成 1028 行裁定，并按生成结果只同步这 26 行的三份 owner 清单；保留并行 SimpleTalk 的 20 行原有证据文字。
- 退役 DataDriven 的 EnterArea 步将真端别名解析为生产 zone 登记名后才能收到运行时 `EnterZone`；16 行共 47 个别名均能在本服 zone XML 中找到登记。15605 的两个真端掉落源 NPC 703137/703138 均属 `quest_use_item` 交互物，编译器给两者各补 Talk 与 ACTION_ITEM_USE 自环，转换总数因而增加 4。18996/28996 未登记区域名，仍归 `XML_RETENTION`，门禁首次拒绝码调整为 `RETAIL_ENTERAREA_ZONE_UNRESOLVED`。
- 15596（阿斯特拉 210100000）与 25596（诺斯佩拉 220110000）真端接取类别为 `EnterWorld`，参数为地图/世界 ID，严禁将其当做 NPC 解析或生成伪造的 `TalkToNpc` 接取流。接取边生成为标准的 `QuestEvent.EnterWorld + StartEligible + WorldIs(worldId)`，领奖与完成依然由各自阵营司令官真实模板（806114 LF6_Ilisia_E / 806116 DF6_Reinhard_E）承载。同时整族普查修正 `isHuntEaMix` 判定（必须同时含有 hunt 和 enterarea），避免纯 Hunt 任务被误拦截进混合链编译器。

## 验证

- 针对启动中断 `retail production catalog incomplete: entries=6216/6224, missing=[13956, 13965, 16830, 16831, 23956, 23965, 26830, 26831] (8)`：
  1. 执行 `mvn -Dtest=QuestEnterWorldPvpRetailContractTest,RetailDataDrivenGateTest,QuestProductionStartupGateTest test`，10 项通过，确认 1036 个 DataDriven 任务驱动装载与启动期安装无异常；
  2. 执行 `mvn -Dtest=RetailQuestDriverOverlayTest test`，5 项通过，断言 `productionOverlayContainsExactlyTheManifestQuestUniverse`（6224 项满额）成功；
  3. `verify_retirement.py` 静态检查通过：catalog 1490 + retired 4734 = 6224，目录数与清单数完全一致，0 悬空生产引用。
- 已获授权的指纹重算命令：`mvn -Dtest=RetailSimpleTalkChainGateTest,RetailSimpleUseItemGateTest -Dretail.talkChain.fingerprintOut=.agents/summary/quest-retail-review-fixes/talk-fingerprints.tsv -Dretail.useItem.fpOut=.agents/summary/quest-retail-review-fixes/use-item-fingerprints.tsv test`：7 项通过。当前链式冻结集与重算结果仅 1971 一行指纹不同，已核对并更新此行。早期在并行快照中观察到 265 项中的 222 项指纹变化，不能单独归因于本轮审查修复。
- 已获授权的聚焦回归命令：`mvn -Dtest=RetailSimpleTalkChainGateTest,RetailSimpleUseItemGateTest,RetailSimpleTalkMigrationReviewContractTest,QuestEventQuestBatchDefinitionTest,Batch31GelkmarosRowLadderContractTest,Batch40ThreeNpcTalkLadderContractTest,Quest1553ClientDialogAlignmentTest,Quest3961To3964RetailAlignmentTest test`：8 类 31 项通过。
- 已获授权的 DataDriven 指纹命令首次执行：6 项中 1 项因 15 行恢复边的 IR 指纹演进失败；逐行核对后更新 15 行（节点数不变、转换数各加 1）。之后同命令在区域名解析接入后重算，16 行区域任务指纹变化，除 15605 新增 4 条有证据的交互物自环外，其余节点与转换数均不变；已按重算结果更新冻结集。
- 已获授权的 `mvn -Dtest=Quest25608RetailSevenStepAlignmentTest,RetailDataDrivenGateTest test` 在并行 owner 误回退期间因 25608 生产视图缺失、冻结集 1028/1002 不符而失败；26 行 owner 修复后为 12 项 2F（区域别名与 15605 指纹）；区域映射接入后 DataDriven 门禁为 6 项 2F（16 行新指纹及 18996/28996 拒绝码），上述冻结登记已同步。待并行 Maven writer 退出后，须复跑这条聚焦命令确认全绿。
- `verify_retirement.py` 静态检查通过：catalog 1517 + retired 4707 = 6224，目录数一致，生产资源引用无悬空；三份 owner 清单一致、1028 个 DataDriven 裁定/指纹对齐。
- 新增 `QuestEnterWorldPvpRetailContractTest` 专用契约门禁；运行 `mvn -Dtest=QuestEnterWorldPvpRetailContractTest,RetailDataDrivenGateTest,QuestProductionStartupGateTest test`，10 项测试全部通过（BUILD SUCCESS），启动期 `不存在 NPC 模板 210100000/220110000` 警告彻底消除。
- 未执行服务器启停、生产全目录门禁或 Aion 5.8 客户端实机验收；这些证据层仍为 PENDING。当前并行工作树的全量生产目录和 XML 保留清单还在变化，不把聚焦通过当作全量验收。

## 2026-09-26 审查三项：多 NPC 领奖窗与退役 XML 测试

- CombineTask 574 行每行两个 task_npc。完成流现在先生成每个 NPC 的 8..23 及 108 路由，再只生成一次全局 `QuestDialog(108)`；门禁逐行检查全局事件恰一条、动作及完整完成后的响应顺序。原冻结 IR 尚无自动确认通道，574 行的冻结变更已按每行 +3 条转换审阅。
- SimpleCollectItem、SimpleHunt、SimpleTalk 三家族每任务调用一次完成流，NPC 路由仍按完成 NPC 集展开；`RetailRewardWindowRouteTest` 覆盖四家族双 NPC 与可选奖励全局槽位，5 项通过。
- 15042、16823/26823、1963 的生产流测试改用 `ProductionQuestDefinitions.definitionInOverlay`，15476 保持 XML owner。15042 的 DataDriven ItemPlay 多播接取按 work-item 数量授 1 件笛子，最后一次演出按真端 `value1_progress_` 发蝴蝶并收回笛子；16823/26823 将真端 `value4_progress_` 的 EnterArea/Hunt 过场加入完成后的响应。链完成节点清零所用计数段，保留进度投影的终态语义。
- 已授权七类命令 `rtk mvn -Dtest=RetailCombineTaskGateTest,RetailSimpleCollectItemGateTest,RetailSimpleHuntFamilyGateTest,RetailSimpleTalkGateTest,Quest15042ProductionFlowTest,QuestCradleReunionProductionFlowTest,QuestRepeatLifecycleTest test` 最后一次于 2026-09-26 11:14 +08:00 结束：18 项 2F/0E；15042、Cradle 双阵营、Repeat、Hunt、Talk 和两个家族的非指纹门禁通过。两项失败分别为 CombineTask 574 行有意指纹漂移，以及 SimpleCollectItem 冻结 155 行而当前退役 175 行。后者新增 20 行均为并行阵营发放迁移（35021/35022/36015/36016/39601/39605/39608/39701/39708/39709 及镜像）。新增多 NPC 用例、DataDriven 门禁和生产目录/白名单的后续结果见下。服务端与 Aion 5.8 客户端验收仍未执行，不得声明完成验收。

### 冻结差集与验证收口（2026-09-26）

- 三份测试导出的 `*-fingerprints.tsv` 保存在本目录；同步到 `src/test/resources/quest/` 前逐行对比集合、哈希、节点数和转换数：CombineTask 574/574 行哈希变化、每行节点数不变且转换数 +3（一个全局 108、两个 NPC 108）；SimpleCollectItem 原 155 行零漂移、追加 20 个已退役阵营镜像行，合计 175 行；DataDriven 1174 行中仅 15042、16821、16823、26821、26823 五行哈希改变，节点数和转换数不变。三份冻结文件已按导出结果同步。
- 17:25 +08:00 初次执行授权的 `rtk mvn -Dtest=RetailRewardWindowRouteTest,RetailCombineTaskGateTest,RetailSimpleCollectItemGateTest,RetailDataDrivenGateTest -Dretail.combine.fpOut=.agents/summary/quest-retail-review-fixes/combine-task-fingerprints.tsv -Dretail.collect.fpOut=.agents/summary/quest-retail-review-fixes/collect-item-fingerprints.tsv -Dretail.dataDriven.fpOut=.agents/summary/quest-retail-review-fixes/data-driven-fingerprints.tsv test`：20 项 4F/0E，其中三项冻结集或指纹未同步。
- 17:29:27 +08:00 冻结同步后执行 `rtk mvn -Dtest=RetailRewardWindowRouteTest,RetailCombineTaskGateTest,RetailSimpleCollectItemGateTest,RetailDataDrivenGateTest test`：20 项 1F/0E。唯余 `RetailDataDrivenGateTest#driftVersusShellsIsRegistered`：20035 并行车道登记 `RETAIL_TALK_HUNT_CHAIN_DEFERRED`，当前编译首拒绝为 `RETAIL_ITEMPLAY_OUTPUT_UNRESOLVED`；本任务没有更改此行的裁定或登记。新增奖励路由 5 项、CombineTask 3 项、CollectItem 6 项、DataDriven 其余 5 项均通过。
- 17:29:58 +08:00 执行 `rtk mvn -Dtest=QuestProductionStartupGateTest,RetailQuestDriverOverlayTest,ProductionCatalogWhitelistVerificationTest test`：8 项 0F/0E，BUILD SUCCESS；生产目录装载、overlay 范围与白名单通过。
- 17:37:54 +08:00 复跑七类 `rtk mvn -Dtest=RetailCombineTaskGateTest,RetailSimpleCollectItemGateTest,RetailSimpleHuntFamilyGateTest,RetailSimpleTalkGateTest,Quest15042ProductionFlowTest,QuestCradleReunionProductionFlowTest,QuestRepeatLifecycleTest test`：18 项 0F/0E，BUILD SUCCESS。指定的 15042、16823/26823、1963 测试已无退役 XML 直读；15476 仍是 XML owner。
- 本次源代码及冻结文件已通过限定路径的 `git diff --check`；新建文件另做尾空白检查。未进行服务端启停或 Aion 5.8 客户端实机验证。实现与静态/聚焦门禁已收口，整类 DataDriven 漂移门禁与实机验收保持 PENDING，归属分别为并行 20035 车道与客户端验收。
