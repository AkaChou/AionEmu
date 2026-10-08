# 任务 10034 客户端验收记录（副本进入 / 离场面）

quest: 10034「锯齿形状的刀 / Jagged Sword」——副本进入与离场恢复面（DD 车道，ELYOS Lv53，英吉斯温链；副本 300160000 Lower Udas Temple「地下神殿的秘密空间」，目标区 `HIDDEN_LIBRARY_300160000`）。与本文件同日的掉落/采集面记录相互独立：`.agents/summary/quest-acceptance/10034-15011-2026-10-08-client-accepted.md`（击杀 216494 掉落 + 15011 物件使用）。

user acceptance confirmation: 用户 2026-10-08 原话「实机验证成功，提交」（对本轮请求的副本三步走查的整体确认；逐步未逐字回报）。范围 = 整个副本进入/离场面（含掉线重登与 10 分钟实例销毁窗内重登两条支路），未限定单步。

server launch mode: IDEA（常驻 Spring Boot 进程，classpath=target/classes）；服务端重启由用户管理；本面修复在用户重启后生效（复测前核对启动时间晚于编译时间）。

repository commit: 修复随四主题合并提交 `cc9dbaf5a`（2026-10-08；本面的 `NativeTeleportPort.enterInstance` + `DataDrivenNativeRuntime` 离场回退 + 门禁同批入库）；本验收记录与 Playbook/memory-bank 收口在后续提交；装配面直测 `NativeTeleportPortTest` 补录于该收口提交。

working tree: dirty；并行会话在飞主题（walker/geo 等）保留未暂存，未纳入本批；本批只显式暂存本任务文件。

Aion 5.8 client/data provenance: 真端 DD 表 `data_driven_quest.xml` 10034 行（case 9 载荷 `value9_progress_ = 13, 300160000, 7` = creationId, worldId, leaveProgress；timer 列 `1800, 7, 0` 目标步同值）；真端落点别名 creation 13 = `IDTemple_Low_Ent01`（794.989990, 918.979980, 154.000000, dir 213）；退役 typed XML（`git 9321e7663^`）s4/s5/s6 → s3 + 补发 182215627。本轮未新采集客户端包 SHA-256。

npc template/object: 入口 NPC 730295（`drakan stone statue`，英吉斯温 210050000 344.93/1374.55/336.43，对话进副本）；副本内任务物件 hidden switch 700604（`LF4_FOBJ_Q10023E`，`ai=quest_use_item`，spawn 806.21/886.73/152）与图书管理员 216531（Zhanim）；补发物品 182215627（`quest_10034a`）。运行时 object ID not captured。

map/instance: world 300160000（`HIDDEN_LIBRARY_300160000` zone = 659–828 × 800–966）；进入前 210050000 Inggison；instance ID = 运行时分配（`getNextAvailableInstance`，创建期完成 spawn；空置销毁窗 = `instance.solo.destroy_delay_seconds` 600s）。

steps:
1. ELYOS 做 10034 至步 3（与 730295 对话的进入步），与 730295 对话进入副本 300160000。
2. 副本内应可见 hidden switch（700604）与 HIDDEN_LIBRARY 区 NPC（不再空图）。
3. 副本内掉线/被弹出后重登 ⇒ 步回到 3 并补回 `Jagged Sword`（182215627），可再次进入。
4. 副本内正常下线重登（实例 10 分钟销毁窗内）⇒ 不回滚（仍在 4/5/6，注册复用回副本）。

source state/status/vars: 进入步 = 3（Talk `LF4_FOBJ_Q10023D` 所在步）；副本内步 = 4/5/6；回写 = `jumpTo(vars, enterStep)`（保留组槽）+ `UPDATE_REQUIRED` + `SM_QUEST_ACTION(questId, status, vars)`。运行时逐字记录 not captured。

action/page/button: 730295 对话（Talk 步 3）挂 case 9 `ENTER_INSTANCE` 进副本 → 带 instanceId 传送（复用已注册 → 否则分配 + 注册）；掉线重登由 `InstanceService.onPlayerLogin` 先决出世界（副本丢失 → 回退面接管）。逐字包序 not captured。

expected response: 进副本落进「有 spawn 的实例」（复用已注册 / 分配下一可用 + 注册 + 带 instanceId 传送），可见任务 NPC 与开关；副本丢失后重登 ⇒ 步回 3 + 补发 182215627（仅 count==0 时）+ 状态包；副本世界内 / 步 3 / 步 7 / REWARD 零动作；10 分钟窗内重登由注册复用回到副本、不回滚。

actual response: 用户按整体确认「实机验证成功」（进副本可见 NPC/开关；掉线重登回步 3 并补回 Jagged Sword、可再进；副本内 10 分钟窗内重登不回滚）。逐步/逐包 not captured。

startup health: 实机验收以用户确认为准；本轮未采集启动日志（not captured）；服务端为 IDEA 常驻进程。

runtime logs: not captured。

protocol trace: not captured。

screenshots/recordings and SHA-256: not captured。

acceptance status: ACCEPTED_NEW_PATTERN
matched Pattern: `INSTANCE_ENTRY_MUST_ALLOCATE_AND_REGISTER`（新建；代表提交 `cc9dbaf5a`、`DataDrivenNativeRuntimeGateTest#instanceEntryAdvancesThroughTheNextAvailableInstancePort`）。匹配字段：进入腿（副本入口必须带实例分配/注册、裸传送落默认空实例）与离场窗口（`enterStep < 步 < leaveProgress` 且世界 ≠ 载荷世界）为本面新合同。对照 `UNREACHABLE_INSTANCE_REENTRY_RECOVERY`（`8b058d4b4`、`Quest14047ClientDialogAlignmentTest#rollsBackUnreachableFlightStepsOnRelogin`）差异字段：症状（进入空图 vs 重登不可继续）、根因（入口丢分配语义 vs 阶段依赖已消失实例对象）、修复层（传送端口分配/注册 vs ENTER_WORLD 回退）；离场回退腿完全复用该既有合同（ENTER_WORLD 回退、不在 LOG_OUT 回退），不另建案例。
remaining risks: ① 组队沿用 typed 车道同款单人注册口径（不做 `registerGroupWithInstance`/teamId 查找）；② `NativeTeleportPort.Live` 的世界判定/委派不在单测内（装配逻辑已由 `NativeTeleportPortTest` 直测）；③ 20034（`3, 300150000, 5`）未实机，同型面只验收 10034；④ 超过 10 分钟窗/服务端重启后的重登由离场回退面接管（本轮窗内支路已复测）。
