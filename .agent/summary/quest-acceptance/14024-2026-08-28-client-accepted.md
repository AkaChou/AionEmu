# 任务 14024 客户端验收记录

quest: 14024「A Krall-ing Suspicion / 偶然获得的情报」

user acceptance confirmation: 用户于 2026-08-28 回复“验收完成 提交”，结合当前任务上下文，确认任务 14024 的客户端验收完成，未限定单一分支或步骤。

server launch mode: not captured（服务端由用户管理）

repository commit: `f9567aa45`（`fix(quest): restore 14024 dialog entry after cancel`；前置页面/action 修复为 `af0ace627`）

working tree: dirty；本次修复提交仅包含 `src/main/resources/aion/data/static_data/quest_definition/quests/14024.xml` 和 `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest14024ClientDialogAlignmentTest.java`；其他已修改和未跟踪文件属于工作区其他任务，均保留未提交。

Aion 5.8 client/data provenance: Aion 5.8 客户端；仓库证据为 `docs/quest/client-dialog-mapping/quest-dialog-pages.csv`、`quest-dialog-action-details.csv` 和 `quest-sequence-audit.csv`，对应 `quest_q14024.html`，source SHA-256 `8916cf81b8c762dc3dc50669719cae3f2aba020a916b5e42dacaf25fcad951ee`；本次未重新采集客户端包哈希。

npc template/object: 中间交互 NPC template 204004；最终领奖 NPC template 204020；运行时 object ID not captured；交互对象来源为任务 XML 与 Aion 5.8 客户端页面/action 合同。

map/instance: 传送目标 world ID 210020000，坐标 `(1610, 1528, 318)`，heading `2`；当前 world/instance ID 与入口/重入上下文 not captured。

steps:

1. 前置：任务 14020 已完成，满足 ELYOS、等级 28 及任务 1043 的 `unfinished`/`noacquired` 条件；获得调查书 182215322 后处于 `START var0=2`。
2. 与 NPC 204004 对话，客户端 `QUEST_SELECT(31)` 显示 `SELECT4(2034)`；点击“拿出卡尔杜谷的调查书”发送 `CHECK_USER_HAS_QUEST_ITEM(39)`，有物品时进入 `s3 var0=3` 并显示 `SELECT4_2(2120)`。
3. 点击 `SELECT4_2` 页面的“继续听”，发送 `SELECT4_2_1(2121)` 并显示 `SELECT4_2_1(2121)`；在传送确认页取消对话后再次与 NPC 204004 交互，`QUEST_SELECT(31)` 恢复 `SELECT4_2(2120)`，随后可继续点击传送确认并前往 NPC 204020 领奖。
4. 登出/登录、重连、重启、死亡、重复领取：not captured。

source state/status/vars: 调查书检查前 `s2 / START / var0=2`；检查成功后 `s3 / START / var0=3`；传送确认后 `reward / REWARD / var0=3`；最终领奖后 `complete / COMPLETE / var0=3` 的节点投影。

action/page/button: `QUEST_SELECT(31) -> SELECT4(2034)`；`CHECK_USER_HAS_QUEST_ITEM(39) -> s3 + SELECT4_2(2120)`；`SELECT4_2_1(2121) -> SELECT4_2_1(2121)`；取消后 `QUEST_SELECT(31) -> SELECT4_2(2120)`；`SETPRO4(10003) -> REWARD`。

expected response: 调查书检查成功时提交 `var0=3`，随后按 `PACKET_ONLY sync -> SELECT4_2`；重新进入 NPC 时不改变状态并显示 `SELECT4_2`；继续听后显示 `SELECT4_2_1`；确认传送时进入 `REWARD`，提交后按 `LEVEL_AND_VISIBILITY_REFRESH sync -> teleport current-or-default -> close-dialog` 执行，最终由 NPC 204020 展开奖励页面并完成任务。

actual response: 用户明确确认“验收完成”；按该确认记录为完整客户端流程已通过。未捕获独立 packet、运行日志或稳定截图附件。

startup health: 未启动或重启服务；隔离验证中专项测试 1/1 通过，生产 catalog `6200` 条编译成功、失败 `0`，白名单违规 `0`。当前主工作区直接 Maven testCompile 另被无关未跟踪测试 `Quest3732InstanceObjectiveTest.java` 缺失 `QuestStatus` import 阻断，该文件未修改。

runtime logs: not captured；无稳定时间窗口、角色、NPC object、任务、世界或实例日志附件。

protocol trace: not captured；无稳定的 objectId、questId、page/action 包序列附件。

screenshots/recordings and SHA-256: not captured；初始问题图片为临时缓存路径，未作为稳定验收附件保存。

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `MULTI_NPC_HANDOFF_REWARD_OWNER`；匹配字段为多 NPC 显式状态链、客户端可见 action/page 链、中间 NPC 取消后需恢复交互、最终奖励 owner 独占；差异字段为 14024 使用调查书检查和 `SETPRO4` 传送确认，不包含代表任务 1163 的工作物品交接与 `SELECT2 -> SELECT2_1 -> SETPRO1` 链。代表提交 `598deb98f`；代表测试 `Quest1163ClientDialogAlignmentTest#followsTheRetailPotionHandoffAndRewardOwner`。

remaining risks: 未捕获运行日志、协议 trace、稳定截图、运行时 object ID、重连/重登/死亡/重复领取路径；本次不新增 Playbook/Pattern 条目。
