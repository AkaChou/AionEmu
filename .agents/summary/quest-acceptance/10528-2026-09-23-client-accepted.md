# 10528 客户端验收记录

quest: 10528；本记录不覆盖同族 10526/20526 或魔族镜像 20528。
user acceptance confirmation: 用户于 2026-09-23 针对当前 10528 修复回复“客户端验证成功，提交”；未限定分支，按项目规则确认该任务完整客户端流程。没有要求用户重复游玩或重新提供截图。
server launch mode: not captured；本次未启动、停止或重启服务器。
repository commit: `263ef9c5eb60732b11829883925f392b79228d22`（XML、回归合同、审计例外与修复说明）。
working tree: dirty；并发任务文件和大量原有暂存内容不属于此修复；本次相关路径见 `.agents/summary/quest-10528-dialog-reward/2026-09-23-sage-handover-row-and-item-guard.zh-CN.md` 与上述提交。
Aion 5.8 client/data provenance: Aion 5.8 客户端，任务页面 `quest_q10528.html` 与仓库 `docs/quest/client-dialog-mapping/README.zh-CN.md` 的页面/动作区分规则；沿用本任务修复说明中记录的客户端页面与旧 handler 对照，本次未采集新的客户端包或 SHA-256。
npc template/object: 醒来的德扎波波 template `806292`；报障时日志中的 interaction object `166905`；天族代理人 template `806075`。修复后交互 object ID 未单独捕获。
map/instance: world ID、instance ID、进入/重登场景 not captured。

steps:
1. 玩家进展到 `START/var0=11`，与醒来的德扎波波对话；报障时曾观察到 `QUEST_SELECT(31) → 6841 → 6842 → SET_SUCCEED(10255)` 反复关闭窗口而不进入领取奖励。
2. 当前修复令贤者交接不再以召唤道具尚在背包为必要条件，并保留 legacy `REWARD/var0=11`；随后由代理人 `806075` 负责奖励选择/完成。这里描述修复合同，未取得修复后的逐包或背包快照。
3. 用户确认当前 10528 客户端验证成功；是否使用过 `REWARD/var0=12` 的老存档重登、具体奖励分支和每一步服务端变量未单独捕获。

source state/status/vars: 合同源 `START/var0=11`、目标 `REWARD/var0=11`；若旧错误存档为 `REWARD/var0=12`，enter-world 修复为 `REWARD/var0=11`；实际验收过程的状态与 packed vars not captured。
action/page/button: 报障阶段的 `31 → 6841 → 6842 → 10255` 及 `SM_DIALOG_WINDOW targetObj=0/page=0` 见用户原始日志；修复后的详细 action/page 序列 not captured。动作 ID 与页面 ID 不混同。
expected response: `SET_SUCCEED(10255)` 的事务执行 `RemoveItem(182216076, ALL)`（有则清理、没有也可交接），目标 `REWARD/11`，after-commit 依序 `LEVEL_AND_VISIBILITY_REFRESH → CloseDialog`；在代理人 `806075` 打开报告入口并完成奖励，不把贤者当作领奖 NPC。
actual response: 用户明确确认 10528 客户端验证成功，按完整任务验收；未提供修复后稳定抓包或日志，不把旧失败 trace 或早期“强写 12 后步骤列表消失”的截图误记为成功路径的逐包证明。

startup health: not captured；本次未运行聚焦 Maven、生产目录编译/白名单门禁或启动日志核验；未收到启动故障证据。
runtime logs: 用户此前提供 2026-09-23 16:00:51–16:00:55 的失败日志文字（806292 / 166905 / 10528）；修复后稳定日志文件与 SHA-256 not captured。
protocol trace: 用户此前提供报障时的包方向和 page/action 文字；修复后可长期访问的协议附件及 SHA-256 not captured。
screenshots/recordings and SHA-256: 早期失败截图曾说明强写步数 12 后任务步骤列表消失；未获得稳定可访问的截图文件、录屏或 SHA-256，均为 not captured。

acceptance status: ACCEPTED_EXISTING_PATTERN（10528 的用户客户端验收；自动化/目录门禁仍 PENDING，不扩展到另外 3 个任务）。
matched Pattern: Playbook 8.18 `LEGACY_REWARD_STEP_PROJECTION_MISMATCH`：迁移把 legacy 的 `REWARD/11` 错投影为 12，导致步骤列表空白，修复 XML 投影、交接动作及 enter-world 老档恢复；代表提交 `f6aff952a`，本次合同 `ArchdaevaRewardRowContractTest#quest10528KeepsTheLegacyRewardStepOnHandover`。Playbook 8.19 `REWARD_PREVIEW_OPTIONAL_WORK_ITEM`：旧 handler 清理可缺物品后继续，而严格正数扣除会使 planner 拒绝，改用 `ALL`；代表提交 `aea256a29`、`Quest18600ClientDialogAlignmentTest#intermediateNpcPagesAndFinalRewardPagesFollowTheLegacyChain`；本任务差异是扣除发生在贤者交接 `START → REWARD` 而非奖励预览，且缺物品只是由合同证实的可复现风险，用户报障当时的物品数未捕获。两类合同已有代表案例，不新增 Playbook；如果后续门禁证明阶段差异构成新的可复用问题，再重审资格。
remaining risks: 代码中的 10526/20526/20528 同形改动没有独立客户端验收；`REWARD/12` 老存档重登和缺召唤道具特定背包条件未获单独实机取证。未经另行构建授权，未运行 `mvn -q -Dtest=ArchdaevaRewardRowContractTest,QuestMonsterProgressContractAuditTest test` 或 production catalog/whitelist 检查；需用户授权后由开发侧执行、报告结果。10528 客户端验收与这些自动化门禁状态分开记录。
