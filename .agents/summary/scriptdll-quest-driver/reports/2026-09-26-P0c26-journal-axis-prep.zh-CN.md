# P0c-26：journal 轴备料（风暴期静态切片）——1526 登记表数据修正 + 自愈边通道发现


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 日期：2026-09-26 ｜ 切片：P0c-26 ｜ lane：SimpleTalk（DataDriven 风暴持续，无窗口）｜ 前序：P0c-25

> **性质**：lane 全量风暴窗口不可用（missing=664 持稳），本片为纯静态备料 + 数据修正 +
> 既有失败口径修正；1526/80290/1351 的翻转与判官实跑全部待窗（与 P0c-24/25 合并执行）。

## 发现

1. **1526 登记表数据错误（已修正）**：客户端 HTML（`data_unpacked/Dialogs/QUEST_Q1526.html`）
   实证 **2 个 `<step>`**——`quest_client_summary_rows.tsv` 记 1 = 生成器误计。已改 1526→2
   （main + target 两副本）。族机制（summaryRows 驱动 reward 投影 + 链路径内置
   [REWARD,var0=0]→rewardRow 修复边）修正后自动全对；批量 10 判官的
   `alignedSiblingsOfTheSameFamilyShareTheRewardRow`（1526 ∈ ALIGNED_SIBLINGS，钉 var0=1）
   随投影修正转绿，**零代码零判官改动**。
2. **80294 缺自愈边 = 既有债（judge-abort 掩蔽实证）**：lenient 探针直证 80294 真端编译
   **零 EnterWorld 边**——单步 `build()` 无 journalRowRepair（该边只在链路径 531-540 行、
   且只发 [REWARD,var0=0]→rewardRow 方向）。`staleRewardRowsAreHealedOnEnterWorld` 的
   契约循环顺序 80291→80295→**80290**→80294：P0c-15 复験时方法在 80290 处中止，
   **80294 的边从未被验证**；80294 采纳后该方法已成为**既有失败**（T3-035139 已含 1 失败）。
   DurableDaevanion 判官走全量视图，风暴中 7 方法全 ERROR（装载错非断言错）。
3. **T3 口径修正（重要）**：T3-035139 = **1999 测试，68F+26E = 94 既有失败**（~55 类，多车道
   共享债池：CradleReunion/DispatchToAltgard/StartEligibility/MutationPlanner/RepeatLifecycle/
   Daevanion 等）。本车道此前"零新增失败"口径只 diff 了 client-contract 单测试的失败集——
   修正为：**切片红线 = 不往债池新增失败类/失败行**；既有债池归属各车道另行清偿
   （QuestRepeatLifecycleTest 的 1963 ERROR、DurableDaevanion 的 80294 边、
   EarlyElyosQuestRegressionTest 也在池中——本轮 P0c-24 对其 barrel 方法的编辑待窗后
   需核对该类的债行是否含 1141 相关）。
4. **通道设计（待窗实现）**：单步 `build()` 补 journal 修复边通道（对齐 CollectItem 的
   `journalRowRepair` 模式）——weapon 方向（rewardRow>0：[REWARD,var0=0]→rewardRow）+
   armour 方向（stale 行来自 `retail-legacy-save-normalization.tsv` 登记：80290/80294
   staleRow=1→0）。落地后清偿 DurableDaevanion 既有红并解锁 80290。
5. **1351 备料完成**：唯一判官 = 批量方法 `dualNpcQuest1351KeepsRewardOwnershipOnTheTurnInNpc`，
   唯一红点 = 首断言钉接取 NPC 203965 started 的 QUEST_SELECT→SELECT1（XML 手工任务描述页；
   P0c-19/20 词汇：族形 started 态只有 FINISH_DIALOG 出口）。改一处断言即可。
6. **80290 备料完成**：80294 同形已采纳（armour 投影/owner/familySiblings 方法对真端编译
   全绿实证）= 直接先例；其 heal 边债与 80294 共享（通道落地一并清偿）。

## 已落地（本片净变更）

- `quest_client_summary_rows.tsv` 1526: 1→2（main + target；HTML 2-step 实证）
- 探针存档 `Retail80294HealEdgeProbeTest.java.txt`（临时源已删）
- 本报告；prod 代码零改动；零行翻转（1526/80290/1351 待窗）

## 待窗合并执行清单（P0c-24/25/26）

1. P0c-24：`Quest1141ClientDialogAlignmentTest` + `EarlyElyosQuestRegressionTest` +
   `QuestInteractionObjectContractGateTest` + T2 1141
2. P0c-25：`Quest30312RetailAlignmentTest` + `Quest30315RetailAlignmentTest` + 批量判官 + T2
3. P0c-26：1526 翻转（数据修正已就位）+ 批量 10 alignedSiblings 绿；80290/1351 翻转 +
   1351 批量断言改 + 单步修复边通道（清偿 Daevanion 既有红）+ T2
4. 全窗口收口后重跑 verify + T3 口径核对（债池快照 diff，红线 = 零新增）

## 判例（勿重推）

- **契约循环的 judge-abort 会掩蔽后续行的真端缺口**：P0c-15"Daevanion 仅 80290 红"时
  80294 的边从未验证——多契约循环判官的采纳确认必须逐契约行核验（或按行隔离重跑），
  不可同形推断。
- **单步/链路径的通道不对称**：journalRowRepair 只在链路径（buildChain 531-540），单步
  build() 无修复边——单步行族的陈旧存档修复是结构性缺口（既有债）。
- **"零新增失败"的口径 = 债池快照 diff**：T3 债池 94 失败 ~55 类是多车道共享现状，
  单测试失败集 diff 不足以支撑全树结论；切片红线按"不新增债行"执行。
- summary_rows 生成器对 `<steps>` 的计数与客户端 HTML 实际 step 数可出现偏差——采纳前对
  journal 轴行用外部 HTML 直证 step 数（1526 实证 2）。
