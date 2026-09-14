# 任务 28510 客户端验收记录

quest: 28510「Destroy The Haramel Facilities / 加工好的奥鲁卡」

user acceptance confirmation: 用户原话“验证通过，提交，排查类似问题”；2026-09-10；结合当前任务上下文，未限定分支或步骤，按规则视为整个任务客户端流程验收完成。

server launch mode: not captured（服务端由用户管理）

repository commit: `366d05508d78d8b0d9d49d4fd4f8e537afd85a15`（`fix(quest): align Haramel quest 28510 NPC ownership`）；验收记录提交前已有无关的并发本地提交 `ccc492dba4b0003c4db8ff4cc2c83a5a560c4009`，未改写。

working tree: dirty；28510 XML 与专项测试已包含在 `366d05508`；当前剩余修改和未跟踪文件属于数据库初始化、Kromede 副本、任务 1192/18602、JFR 及其他用户工作，均未纳入本次提交。

Aion 5.8 client/data provenance: Aion 5.8 客户端；仓库证据为 `../../../docs/quest/client-dialog-mapping/quest-dialog-pages.csv`、`quest-sequence-audit.csv` 和 `client-lifecycle-alignment.csv`；对应 `20000_29999/quest_q28510.html`；本次未重新采集客户端包哈希。

npc template/object: 接取 NPC template 804605；加工奥鲁卡交互 NPC template 700953；最终领奖 NPC template 203560；runtime object ID not captured；700953 的 `quest_use_item` 模板和 Haramel/Kumuki 静态出生数据为仓库证据。

map/instance: Haramel/Kumuki 静态出生证据；实际 world/instance ID、入口上下文 not captured。

steps:

1. 在任务 28510 未接取时使用 NPC 700953 的交互物入口，确认不再打开不存在的 `Quest_Q28510.html` 页面。
2. 按任务正常流程接取、击杀 700950、使用 700953，并在 203560 领奖；用户未提供逐步操作序列，按上述确认记录为完整客户端流程通过。
3. 登出/登录、重连、重启、死亡、重复领取：not captured。

source state/status/vars: 非任务使用时为 `NONE`；任务链为 `unaccepted/NONE/var0=0` -> `started/START`，700950 三次击杀后 700953 依次推进 `var0=3 -> 4 -> 5`，再进入 `reward/REWARD/var0=5`，最终 `complete/COMPLETE`。

action/page/button: 修复前 700953 被错误声明为 `NPC_START` 并触发 `SELECT1(1011)`；修复后仅允许已接任务的 `ACTION_ITEM_USE` 与 `USE_OBJECT(-1)`，804605 使用 `SELECT_NONE(4762)` 接取，203560 负责 `DEFAULT_SUCCESS(10002)`、奖励窗口 page 5 和奖励选择。

expected response: 非任务使用 700953 不应路由到 Q28510 接取页面或发送任务 HTML；任务内三次击杀后，700953 的 `USE_OBJECT` 分别执行变量推进并按 `PACKET_ONLY sync -> close` 响应，最后按 `LEVEL_AND_VISIBILITY_REFRESH -> close` 进入 `REWARD`；203560 独占奖励预览和完成路由。

actual response: 用户确认“验证通过”；未捕获独立 packet、运行日志或稳定截图附件。

startup health: not captured；未启动或重启服务端，用户未报告 typed quest engine 初始化失败或 `QuestCompilationException`。

runtime logs: not captured；无稳定时间窗口、角色、NPC object、任务或 world/instance 日志附件。

protocol trace: not captured；无稳定的 objectId、questId、page 或 action 包序列附件。

screenshots/recordings and SHA-256: not captured；对话中的临时缓存截图未形成稳定仓库附件。

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `MULTI_NPC_HANDOFF_REWARD_OWNER`；匹配中间交互物被错误扩展为 `NPC_START`、最终 reward owner 不独占以及客户端起始页/动作错位；不同点是 28510 使用 `quest_use_item` NPC 和三次击杀后的显式 `USE_OBJECT` 状态链，而代表任务 1163 使用多页中间 NPC 交接。代表提交 `598deb98f`；代表测试 `Quest1163ClientDialogAlignmentTest#followsTheRetailPotionHandoffAndRewardOwner`；本次测试为 `Quest28510ClientDialogAlignmentTest#preservesTheLegacyHaramelNpcRolesAndDialogFlow`。

similar issue audit: 只读扫描发现 804 个 `ai="quest_use_item"` NPC 模板仍有 85 条 `NPC_START` 路由，涉及 74 个任务；其中 10 条位于 8 个任务（18301: 730373/730374、2484: 700267、2664: 700324、28301: 730373/730374、28302: 730375、28303: 700980、3036: 700398、4004: 700340），这些任务的 Aion 5.8 客户端页面表没有 `SELECT1(1011)`，且 `origin/history` 旧 handler 将对应 object 只注册为对话入口、由其他 NPC 接取，属于优先复核的同类 load-fail 候选。另有 75 条虽然存在 1011 页面，但仍需核对 object owner；其中 25 条同时存在 `ACTION_ITEM_USE` 或 object/reward 路径。本次只提交已验收的 28510，未对候选任务批量修改。

remaining risks: 未捕获运行日志、协议 trace、稳定截图、运行时 object ID、重连/重登/死亡/重复领取路径；本会话未运行 Maven focused/catalog/whitelist 门禁（遵循项目未授权构建规则）；上述 8 个高优先级候选仍需分别对照 legacy、编译 IR 和客户端流程后再修复。
