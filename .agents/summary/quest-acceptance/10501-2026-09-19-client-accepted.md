# 任务 10501「Research the Ruins / 被毁的遗迹」交付续接修复：客户端验收记录（ACCEPTED）

```text
quest: 10501（同批同形 41 个任务：2372、10504、13968、15689、15690、15691、16838、18742、18975–18978、19010/19016/19022/19028/19034、21027、23968、25689/25690/25691、26838、28742、28975–28978、29010/29016/29022/29028/29034、80723、80795、80849/80850/80851/80852、80886、80958）
user acceptance confirmation: 用户 2026-09-19 回复“客户端验证成功”，未限定分支或步骤，按项目规则视为该任务的完整游玩验收
server launch mode: 用户管理的服务端实例（本轮未启动、停止或重启服务端）
repository commit: 修复提交 75312dcdc（42 个 quest XML + 5 个测试类）；验收时基线 HEAD 为 c34458083、文档写入时已推进到 cf17c6607（均为并行任务提交，不含本任务改动）；本文档与 Playbook 案例 8.39 随本交付批次的第二个提交落地
working tree: dirty；本任务相关路径为 42 个 quest_definition XML（10501 + 41 个同形任务）、3 个新增测试类（Quest10501HandoverContinuationTest、QuestHandoverContinuationAuditTest、HandoverContinuationContract）、2 个更新的既有测试类（ItemCollectingDialogProtocolAlignmentTest、QuestRetailCollectionRoleAlignmentTest）；工作区其余改动属并行任务
Aion 5.8 client/data provenance: Aion 5.8 客户端解包数据 Dialogs/10000_19999/quest_q10501.html（SHA-256 448eb3983965f67c99f522261a542ce14d055d5d092579d3e08dda031eb8f280）；仓库映射证据 docs/quest/client-dialog-mapping/quest-dialog-pages.csv:852-869、docs/quest/client-dialog-mapping/quest-dialog-action-details.csv:630-647
npc template/object: 交付 NPC 804700；修前运行时 objectId=69442（用户提供的 SM_DIALOG_WINDOW/CM_DIALOG_SELECT 追踪）；前置交互物 85603（npcId 731536）、86799（npcId 731535）
map/instance: 野外任务；world/instance 本轮未采集（追踪未含 world 字段）

steps:
1. 前置：完成任务 10500 后接取 10501（ELYOS、MISSION、56+、不可共享、不可放弃），使用龙族证物放置器 182215598 三次，任务变量 var0 由 0 推进到 6。
2. 与 NPC 804700 对话 → 页 3057（select7）→ 动作 39（HACTION_CHECK_USER_HAS_QUEST_ITEM）上交龙族证物 182215599。
3. 修复前：服务端已进入 REWARD（状态=4 步数=7）并下发页 10000，客户端只本地关窗、没有后续；必须重新与 804700 对话才收到页 10002，再由 1009 打开奖励窗。
4. 修复后：同一次对话直接收到页 10002（不再出现 10000），1009 打开奖励窗 5，完成动作后 10501 结束（状态=5）并自动接取 10502。

source state/status/vars: s6（START，步数 6，var0=6）→ reward（REWARD，步数 7，var0=7，龙族证物 182215599 已扣除）
action/page/button: 页 3057 `HACTION_CHECK_USER_HAS_QUEST_ITEM(39)` → 成功分支 after-commit（STATE_SYNC_BEFORE_DEPENDENT_PAGE：LEVEL_AND_VISIBILITY_REFRESH → SHOW_QUEST_PAGE DEFAULT_SUCCESS(10002)）；页 10002 `HACTION_SELECT_QUEST_REWARD(1009)` → 奖励窗 5；页 10000 的 `HACTION_FINISH_DIALOG(1008)` 只本地关闭，服务端不可见
expected response: 事务内 remove-item 182215599 并写 var0=7，目标节点投影 REWARD；提交后先同步任务状态再下发 10002，玩家在同一次对话内即可领奖；物品不足分支仍下发 10001（关闭落点 SELECT_QUEST）
actual response: 用户确认“客户端验证成功”；字段级页面、状态变量与奖励包未采集附件

startup health: not captured；本轮未启动或重启服务端，用户未报告 typed quest engine 初始化失败、QuestCompilationException 或 AMBIGUOUS_TRANSITION
runtime logs: not captured（修前 2026-09-19 09:43–09:45 追踪摘要见 .agents/summary/quest-10501-handover-continuation/README.md；用户提供的临时缓存未落库为稳定附件）
protocol trace: not captured；修前用户提供的 CM_DIALOG_SELECT/SM_QUEST_ACTION/SM_DIALOG_WINDOW 追踪只保留摘要，无稳定附件与 SHA-256
screenshots/recordings and SHA-256: not captured

acceptance status: ACCEPTED_NEW_PATTERN
matched Pattern: `HANDOVER_CHECK_PAGE_LOCAL_CLOSE_CONTINUATION`（Playbook 新增案例 8.39，随本交付批次的第二个提交登记）；相关跨域模式 `QE-041`（1008 为客户端本地关闭，不能承载服务端续接）
matching fields: 客户端 check_user_item_ok 页只有 HACTION_FINISH_DIALOG(1008)；成功分支 after-commit 先状态同步再下发页；目标节点存在同 NPC 的 USE_OBJECT(-1) 续接页
differing fields: 与 8.25 `CHECK_CONFIRMATION_PAGE_CONTRACT` 取舍相反——8.25 的 ok 页按钮是 1009/故事翻页等会回传任务动作的可见按钮，因此保留确认页；本模式必须绕过确认页直接续接。10504 的续接页是 10002（DEFAULT_SUCCESS），其余 40 个任务的续接页是奖励窗 5
remaining risks: 仅代表任务 10501 实机验收，41 个同形任务按同合同静态锁定与门禁覆盖、未逐个实机复验；55 个无同 NPC 续接页的任务保持确认页终端形态（未改）；若后续观察到 1008 会被客户端回传任务动作，需重评本 Pattern 与 QE-041
```

## 证据引用

- 报告与复现、客户端按钮证据、根因、修复与同形批处理判定：`.agents/summary/quest-10501-handover-continuation/README.md`（第一、二、三、四、十节）。
- 验证日志：`.agents/summary/quest-10501-handover-continuation/mvn-handover-family-final-gates-2026-09-19.log`（聚焦 + 客户端契约门禁 47/47）、`mvn-production-catalog-family-2026-09-19.log`（PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0）。
- 代表测试：`Quest10501HandoverContinuationTest#handOverSuccessShowsTheReportPageInsteadOfTheClientClosedConfirmationPage`、`QuestHandoverContinuationAuditTest#clientLocalCloseConfirmationPagesContinueInTheSameDialogue`。
