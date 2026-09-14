# 任务 14025 客户端验收记录

quest: 14025「Cooking up Disasters / 埃尔特内遭遇的危机」

user acceptance confirmation: 用户原话“验证通过，排查类似问题修复”；后续补充“添加任务书”；2026-09-10；结合当前任务上下文，未限定分支或步骤，按规则视为整个任务客户端流程验收完成。

server launch mode: not captured（服务端由用户管理）

repository commit: `5a65c99bcf42964249338804973c6a97c8cfd936`（`fix(quest): prevent 14025 auto-start accept dialog`）；相似问题修复提交 `bde6d1163edb58c1ff0c16b59a0ccd030c057771`（`fix(quest): remove accept pages from automatic starts`）。

working tree: dirty；14025 修复与专项测试已包含在 `5a65c99bcf42964249338804973c6a97c8cfd936`；相似问题批量修复及目录级回归测试已包含在 `bde6d1163edb58c1ff0c16b59a0ccd030c057771`；数据库、Kromede 副本、客户端对话入口、1192/18602 及临时文件属于工作区其他改动，均保留。

Aion 5.8 client/data provenance: Aion 5.8 客户端；仓库证据为 `../../../docs/quest/client-dialog-mapping/quest-dialog-pages.csv`、`quest-dialog-action-details.csv` 和 `quest-order-audit.csv`，对应 `10000_19999/quest_q14025.html`；本次未重新采集客户端包哈希。

npc template/object: 初始/阶段 NPC template 203989；中间阶段 NPC template 204020；最终领奖 NPC template 203901；runtime object ID not captured；交互对象来源为任务 XML、`origin/history` 旧 handler 与 Aion 5.8 客户端页面/action 合同。

map/instance: world 210020000（Eltnen / 埃尔特内）；instance ID not captured；入口与重入上下文 not captured。

steps:

1. 满足 ELYOS、等级 35+、任务 1043 未完成且未接取的条件；区域任务结束或升级时自动进入 `started / START / var0=0`。
2. 自动接取只提交任务状态并发送 `VISIBILITY_REFRESH`，不发送 `SHOW_ASK_QUEST_ACCEPT_WINDOW(4)`；与 NPC 203989 沿 `QUEST_SELECT(31) -> SELECT1(1011)` 进入任务阶段。
3. 完成 203989 的收集交付并进入 204020 阶段，经过 `SELECT2/SELECT2_1/SELECT2_2`、`SETPRO2` 和地图内传送；继续经过 `SELECT3`、`SETPRO3`、`SELECT4`、`SETPRO4` 与任务目标击杀。
4. 在 203989 的 `SELECT6(2716)` 后发送 `SETPRO6` 进入 `REWARD`；返回 203901，沿 `SELECT7(3057)` 打开奖励预览并选择奖励完成任务。登出/登录、重连、重启、死亡和重复领取：not captured。

source state/status/vars: `unaccepted / NONE / var0=0` -> `started / START / var0=0` -> `s1..s5 / START / var0=1..5` -> `reward / REWARD / var0=6` -> `complete / COMPLETE`。

action/page: 区域任务结束或升级 -> `started`，仅 `VISIBILITY_REFRESH`；`QUEST_SELECT(31) -> SELECT1(1011)`；后续阶段使用 `SELECT2`、`SELECT2_1`、`SELECT2_2`、`SELECT3`、`SELECT4`、`SELECT6`；最终 `SELECT7(3057)` -> `SELECT_QUEST_REWARD(1009)`。

expected response: 自动接取完成后任务进入 START 且可见性刷新，不弹出任务接受 HTML；NPC 页面和阶段状态按 Aion 5.8 客户端 action/page 链推进，最终由 203901 独占奖励预览与完成。

actual response: 用户确认“验证通过”；按该确认记录为完整客户端流程已通过。未捕获独立 packet、运行日志或稳定截图附件。

startup health: not captured；未启动或重启服务端，用户未报告 typed quest engine 初始化失败或 `QuestCompilationException`。

runtime logs: not captured；无稳定时间窗口、角色、NPC object、任务、世界或实例日志附件。

protocol trace: not captured；无稳定的 objectId、questId、page 或 action 包序列附件。

screenshots/recordings and SHA-256: not captured；对话中的临时缓存图片路径不可作为长期验收附件。

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `LEVEL_UP_AUTO_START_NO_DIALOG`；匹配自动入口从 `NONE` 进入 `START`、旧 handler 只启动任务并刷新状态、客户端接受页不得由自动入口发送。14025 的差异是同时存在 `ZONE_MISSION_END` 入口和多阶段 NPC/传送/奖励链，已由任务专项测试覆盖；代表案例为 38001，代表提交 `d3b28d2af3a7a3085da461d96bb9dfe6118d4905`，代表测试 `Quest38001LevelUpDialogTest#matchesLegacyLevelUpAndNpcDialogContract`。本任务不新增重复 Playbook/Pattern 条目。

similar issue audit: 全量生产 XML 静态扫描发现 35 个任务、59 条 `level-up`/`zone-mission-end` 的 `unaccepted -> START` 路由错误发送 `SHOW_ASK_QUEST_ACCEPT_WINDOW(4)`；已修复任务为 `10110, 10111, 10112, 10113, 10521, 10525, 10526, 10527, 10528, 10529, 10530, 14041, 14042, 14043, 14044, 14045, 14046, 19070, 19071, 20111, 20521, 24015, 24042, 24052, 24053, 24054, 2901, 2902, 2903, 2904, 29070, 29071, 80000, 80001, 80230`；新增 `QuestAutoStartDialogAuditTest` 锁定生产目录规则。该批次尚未逐任务完成客户端验证，不能将其验收状态等同于 14025。

remaining risks: 相似批次尚未获得逐任务客户端/runtime 验证；本会话未运行 Maven focused/catalog/whitelist 门禁（遵循项目未授权构建规则）；未捕获运行日志、协议 trace、稳定截图、运行时 object ID、重连/重登/死亡/重复领取路径；服务端重新加载后的启动健康仍由用户环境负责。
