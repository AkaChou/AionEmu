# 竞技场两阶段任务领奖入口收口（18208/18209/28208/28209，2026-09-21）

## 报障（门禁回归，非玩家报障）

在 `7a7d27809`（领奖行全库审计批次 1-7）上跑聚焦门禁时出现两处失败：

- `QuestPrematureRewardRouteExclusionTest#incompleteProgressCannotRewardAndCompletedProgressCanReport[10]`
  —— 28208 在 `{var0=0}` 就能经 `TalkToNpc(205320, 1009)` 进入 REWARD。
- `QuestClientContractGateTest` 新指纹 2 条 `BUTTON_WITHOUT_ROUTE`：28208/28209 的
  `reward + NPC 205321 + 31 -> reward + page 10002` 页面上唯一按钮 `1009` 在 `reward` 态没有路线。

## 根因

1. **无门禁的“对话即领奖”捷径（四个任务都有）**：批次 6 保留了迁移期
   `<dialog type="NPC_REPORT" npc-id="205309|205320" source="started" target="reward" page="DEFAULT_SUCCESS"/>`，
   编译 IR 实测生成 `started + <npc> + 1009 -> reward + page 5`（`conditions=[]`、`actions=[]`），
   玩家在任务书计数行阶段与领奖 NPC 对话即可跳过两行击杀直接领奖；与行推进合同、retail 旧 handler
   （只在 REWARD 态响应领奖 NPC，批次 6 自述）均冲突。
2. **旧门禁案例失效**：`stage(28208, 7, -1, 7, "k7", 205320, 205321)` 引用批次 6 已删除的 `k7` 节点
   （旧“每杀一个阶段”模型），行模型下这条期望不再成立，掩盖了 18208/18209 的同型捷径。
3. **28208/28209 的第二个领奖人（205321）缺 reward 态领奖窗路线**：由并行任务补
   `<npc-complete npc-id="205321"><preview actions="USE_OBJECT SELECT_QUEST_REWARD"/></npc-complete>` 解决。

## 修复

- `18208.xml` / `18209.xml`（本提交）：删除 `NPC_REPORT` 捷径，领奖入口只保留在 REWARD 态
  （`reward + 31 -> page 10002` 报告页 + `reward + 1009 -> page 5` 奖励窗，均由既有路线/npc-complete 提供）；
  行推进仍由两条击杀路线收口，旧存档由既有四条 `enter-world` 收敛边修复。
- `28208.xml` / `28209.xml`：同型捷径删除已随 `bd6c9e224`（并行批次 8）落地，本提交不重复。
- `QuestPrematureRewardRouteExclusionTest`（本提交）：删除失效的 `k7` 案例，新增
  `arenaJournalRowsKeepTheRewardEntranceInTheRewardState`——对 18208/18209/28208/28209 的
  `var0 0..3 × var1 0..4 × var2 0..1` 全 START 变量域做 fail-closed 扫描（任何 START 侧对话路线
  不得进入 REWARD / 奖励窗口），防止“对话即领奖”重回；REWARD 态领奖链由 `ArenaPhaseRowContractTest`、
  `QuestClientContractGateTest` 与 npc-complete 完成路线覆盖。

## 验证（用户授权后，隔离 worktree）

```bash
rtk mvn -o -Dtest=QuestPrematureRewardRouteExclusionTest,QuestClientContractGateTest,ArenaPhaseRowContractTest,QuestReportedRewardCoverageTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,Quest16802ClientDialogAlignmentTest,Quest16803ClientDialogAlignmentTest,Quest16804ClientDialogAlignmentTest,QuestCounterSourceProjectionProductionFlowTest,QuestArchivesMissionCounterProductionFlowTest,QuestDataDrivenHuntProductionFlowTest,QuestProductionAcceptProtocolRegressionTest test
```

- 结果：`Tests run: 71, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`；含生产 catalog 白名单门禁
  （`PRODUCTION_COMPILE_OK=6189`）、客户端页面/按钮契约门禁、`ArenaPhaseRowContractTest` 行合同、
  16802/16803/16804 与档案馆家族回归。
- 隔离方式：工作区含多个并行任务的未提交改动，故在 `/tmp/aionemu-arena-verify`（`bd6c9e224` + 本提交 3 个文件）
  验证；验证后已 `git worktree remove --force` + `git worktree prune` 清理，未遗留构建产物。
- 早期一轮同样方式在 `/tmp/aionemu-28208-verify`（`7a7d27809` + 4 个 XML + 门禁测试）得到 71 用例全绿。

## 状态

- 实现 + 门禁：完成（本提交：`18208/18209` + 门禁维护；`28208/28209` 已在 `bd6c9e224`）。
- 客户端实机验收：pending（18208/18209/28208/28209 均未做客户端点验）。
- 待点验要点：计数两行未完成时与领奖 NPC 对话不得出现“领取奖励”按钮/奖励窗；精英击杀后
  `reward + 31 -> select_success(10002) -> 1009 -> 奖励窗(5)` 应可直接领取；旧存档进出副本/重登应按行号收敛。
