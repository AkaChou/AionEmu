# 任务 14123 客户端验收记录

quest: 14123「The Shadow of Vengeance / 复仇之影」；ELYOS 29+ 任务；验收范围覆盖接取、进入目标区域、击杀行商锡普拉塔、返回 NPC 交付与领取奖励的完整客户端流程。

user acceptance confirmation: 用户原话“验证完成，提交”；2026-09-13；未限定单一分支或步骤，按验收规则视为整个任务客户端流程验收完成。

server launch mode: not captured（服务端由用户管理；本会话未启动、停止或重启）

repository commit: `d263468021b82eca00ee84c30832f7d6baf84b52`（`fix(quest): restore quest 14123 peddler spawn`）；本验收记录及 Playbook 更新在其后的文档提交中。

working tree: dirty；本次修复提交只包含任务 14123 XML 与 `Quest14123ZoneSpawnTest`。工作区中的会话恢复材料、`__pycache__` 和 JFR 文件与本任务无关，均保留未提交。

Aion 5.8 client/data provenance: Aion 5.8 客户端；仓库内客户端证据为 `docs/quest/client-dialog-mapping/quest-dialog-pages.csv`、`quest-dialog-action-details.csv`、`legacy-quest-dialog-contracts.csv`；`10000_19999/quest_q14123.html` 的来源 SHA-256 为 `18ebdd15c579eda4a1d21aa1fb2c230b1d01898daa4fe81011768ad5f60ca12c`；本次未重新采集客户端包或其附件哈希。

npc template/object: 接取与报告 NPC template 203933；击杀目标“行商锡普拉塔”template 206360；目标生成位置 `(1768.16, 924.47, 422.02, heading=0)`。runtime object ID、生成来源附件和客户端截图均 not captured。

map/instance: world 210020000（`ELTNEN_OBSERVATORY_210020000`）；目标按当前实例或默认实例生成，运行时 instance ID not captured。入口为重进区域，重连恢复为进入该 world。

steps:

1. 在满足 14123 接取条件且其他前置任务状态正确时，可由 NPC 203933 对话接取，或在进入 `ELTNEN_OBSERVATORY_210020000` 时自动接取。
2. 接取成功后，在 world 210020000 的结界塔后空地生成 template 206360；离开后重新进入任务区域，或在该 world 登录，仍能恢复目标。
3. 击杀 206360 后，任务变量由 `var0=0` 推进到 `var0=1`，目标不再需要在任务区域重复生成。
4. 返回报告 NPC 203933 或 203991，沿客户端对话链路进入领奖状态，选择奖励并完成任务。

source state/status/vars: `unaccepted / NONE / var0=0` -> `started / START / var0=0`；接取事务包含可见性刷新与目标生成。`started` 状态下，进入区域及在该 world 登录时仅在 `var0=0` 恢复生成；击杀后进入 `report / START / var0=1`，随后进入 `reward / REWARD` 并完成。

action/page/button: 对话接取沿用客户端 `QUEST_SELECT(31) -> SELECT1(1011) -> ... -> QUEST_ACCEPT_1(1002)` 链路；区域接取使用 `ENTER_ZONE`。本次修复不改变 14123 的原有报告、奖励窗口和奖励选择动作，只补齐目标生成 owner。

expected response: 两条接取路径都在状态提交后生成 template 206360；已接取且尚未击杀时，`ENTER_ZONE` 和该 world 的 `ENTER_WORLD` 可幂等恢复同一任务 NPC；击杀仍只推进一次计数并进入报告状态，不生成静态重复 NPC。

actual response: 用户确认“验证完成”；按规则记录为完整任务客户端流程通过。未提供修复后的独立 packet、运行日志或稳定截图附件。

startup health: not captured；服务端由用户管理，本会话未启动、停止或重启，也未捕获启动日志。会话中未收到 typed quest engine 初始化失败、`QuestCompilationException`、`AMBIGUOUS_TRANSITION` 或 production catalog 编译失败的报告。

runtime logs: not captured；无稳定时间窗口、角色、NPC runtime object、world/instance 或任务日志附件。

protocol trace: not captured；无稳定的 objectId、questId、page、action 或 spawn 包序列附件。

screenshots/recordings and SHA-256: not captured。

acceptance status: ACCEPTED_NEW_PATTERN

matched Pattern: `PRE_KILL_QUEST_NPC_ZONE_SPAWN_AND_REENTRY`；匹配字段为任务在击杀前必须生成可攻击目标、接取事务与区域重入都要恢复生成、击杀后停止恢复，以及完整客户端流程由用户确认通过；不同字段为该模式不是 14112 式“击杀后生成可交互 NPC”，因此不归入既有 `POST_KILL_QUEST_NPC_SPAWN_AND_REENTRY`。代表提交 `d263468021b82eca00ee84c30832f7d6baf84b52`；代表测试 `Quest14123ZoneSpawnTest#spawnsPeddlerOnBothAcceptanceRoutes`、`Quest14123ZoneSpawnTest#restoresPeddlerWhenReenteringTheQuestZone`、`Quest14123ZoneSpawnTest#restoresPeddlerAfterLoginAtTheQuestWorld`、`Quest14123ZoneSpawnTest#keepsThePeddlerKillCountAdvancingToReport`。

remaining risks: 修复后的 packet trace、启动日志、runtime object/world/instance、截图和稳定附件均为 not captured；未分别覆盖每个客户端接取分支、死亡后重试、重复登录和长时间挂机后的目标状态；本会话未运行 Maven，未单独执行代表测试；本会话未 push，`quest` 分支提交由用户推送。
