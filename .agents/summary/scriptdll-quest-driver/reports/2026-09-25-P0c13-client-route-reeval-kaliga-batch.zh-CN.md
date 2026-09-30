# P0c-13：CLIENT_ROUTE 73 行复験·第一批——Kaliga 系 20 行采纳退役（M3-d 降级反转）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 日期：2026-09-25/26 ｜ 切片：P0c-13 ｜ lane：SimpleTalk ｜ 前序：P0c-12（哨兵桶归零）

## 交付

1. **复験方法（flip-and-test，生产视图为被试）**：M3-d 时代按「逐任务客户端契约」降级的
   73 行 CLIENT_ROUTE，在编译器多轮进化后（10h~12 的 B 系/单步/工作物品/collect 门/挑战哨兵
   通道）重新对拍。73 行探针全编译 OK:DIFF（10 行纯 QE-051 投影、63 行过渡集差）；
   契约测试装载**生产视图**（`ProductionQuestDefinitions.definition()` = 目录 + 真端 overlay），
   随归属自动切换——因此可按批「翻转 → 退役 XML → 跑契约测试」，红即回退 KEEP。
2. **Kaliga 批 20 行采纳**（18618-18627 ELYOS + 28618-28627 ASMO，
   `p0c13-kaliga-decisions.tsv`，basis=`M3D_DOWNGRADE_REVERSED`）：
   - **探针直证**：ELYOS 10 行差异 = 恰一对过渡（QUEST_SELECT 接取路由有无
     `PlayerRaceIs` 条件）；ASMO 10 行另差两条 `SyncQuestState[PACKET_ONLY]` 进度刷新边
     （CollectItem[185000102] / KillNpc[217006]）。
   - **裁定：真端对、XML 错（两类）**：
     ① 种族隔离在真端形状里是**任务级**轴（每阵营独立任务 ID + `race_permitted` 元数据），
     运行时在接取入口强制（`QuestService` 485/696/755 三处 `isRacePermitted`）——对立方
     永远无法进入 START，路由级 `PlayerRaceIs` 是老 XML 的冗余设防；真端 SimpleTalk 表
     无路由级种族轴。
     ② PACKET_ONLY 进度刷新边是老 handler 模拟（不改状态），真端 SimpleTalk 形状无此轴；
     交付语义由对话框 CHECK 对（39/20002）完整承载，契约测试亦不主张这两条边。
   - **契约测试转真端形状**：`QuestKaligaCollectionClientDialogAlignmentTest` 接取路由条件
     断言改 `List.of()`（双语注释记录裁定），删除路由级种族求值断言与 `opposite()` 辅助
     （`permittedRaces` 元数据断言保留在更前行）；其余契约（SELECT5 页 / CHECK 对
     priority 0/1 / select6 失败页 / boss 处无 key 检查）全保留。
3. **构建器语义升级**：`build_retention_list.py` 降级登记分支加优先级反转——
   `downgrades` 行若已有 `ADOPT_RETAIL` 复験裁定（b1 层）则降级让位（与 wave B 翻案同先例）。
4. **退役落地**：`p0c13_retire_kaliga_rows.py`（自 p0c12 派生，三处改动核对 diff）；
   20 XML 删除、catalog 1537→1517；漂移 20 行本就冻结在探针分类
   `DIFF:TRANSITION_SET`（无需翻转）；裁定表证据措辞按最终裁定修正后重建三副本。

## 结论（实测）

- SimpleTalk 族：**2223 = 2027 RETAIL_TABLE + 196 XML_RETENTION**（CLIENT_ROUTE 73→53）
- 全库 retention：**6224 = 4707 RETAIL_TABLE + 1517 XML_RETENTION**
- **verify_retirement = 1517/1517/4707 sum=6224 — OK**（两次：退役后 + 重建后）
- 门禁：`QuestKaligaCollectionClientDialogAlignmentTest` **绿**（生产视图装载 + 全 20 案例通过）；
  `RetailSimpleTalkGateTest` 3/3 + `RetailSimpleTalkChainGateTest` 2/2 绿
- 并行 lane 同期落地（DataDriven 26 行翻转 + 15548/25548 零节点修复登记）已被共享构建器
  复现（`p5-datadriven-decisions.tsv` 层），重建不回退其行——10010 复核 RETAIL_TABLE ✓

## 碰撞分诊（T2 全量 93 id 扫描，非本片失败全为在先状态）

- `QuestClientContractGateTest`（catalog 级基线门）：29 任务 BUTTON_WITHOUT_ROUTE 漂移
  （接取 START 路由缺失）——**T3-211202（21:12，本片首写之前）即已红**，与本片无关；
  全部 29 行为在先采纳行，属并行 DataDriven lane 在飞改动范畴。
- `QuestDialog31`/`ReportToMany`/`MissionItemConsumption`/`QuestBatchReportNpc`/
  `LegacyTemplateMirror`/`EarlyElyos`/`PlayerQuestStartEligibility` 各测试的失败主体
  （1131/3914/3092/11139/25602 等）均非本片 93 id，且在 T3-211202/T3-003550（00:45，
  本片首写之前）日志中已失败——在先状态。
- `ProductionCatalogWhitelistVerificationTest` + `QuestDefinitionCatalogManifestTest` ×6
  = 15548/25548 零节点（台账已登记、lane 已按 METADATA_ONLY 修复——T2 快照在修复落地前）。
- **本片 20 行对上述失败零贡献**：门禁失败集合与本片 id 无交集（kaliga-20 ∩ 29 漂移 = ∅）。
- **新教训（target/classes 陈旧资源）**：`QuestDefinitionDirectoryLoader` 按 classpath 目录
  扫描 `quests/*.xml`——源树删除 XML 后 `target/classes` 陈旧副本仍参与编译，表现为
  `wrongOwner`（清单 RETAIL_TABLE + 目录仍见 XML）。处置：删除 target 陈旧副本（本片 20 个）
  后门禁恢复；退役脚本今后可直接清理 target 对应物。

## 下一步（P0c-14 候选）

- CLIENT_ROUTE 余 53 行按契约测试分批复験：ReportToManyDialogRoute 系 16、
  CharmedEvent 7、StartEligibility 7、Lunar 5、CollectTurnIn 4、LegacyTemplateMirror 4 等
  （注：前述「MissionItemConsumption 20」旧计划与实际映射不符，以本片映射表为准）
- SimpleTalk 其他桶：TALK_CHAIN 21 通道缺口、CRAFT 28、名字未解 27、CLIENT_REPORT_VARIANT 17
- 其他族：SimpleHunt gap 47 / reject 108 / spawn 2；DataDriven 随并行 lane
