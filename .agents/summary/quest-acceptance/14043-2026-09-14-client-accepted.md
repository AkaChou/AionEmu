# 任务 14043 客户端验收记录

quest: 14043「Drawling Balaur / 学习龙族语」；ELYOS 45+ MISSION；验收范围＝任务班图斯寻路阶段及之后的领奖完成流程；用户未限定单一分支或步骤。

user acceptance confirmation: 用户原话“验证通过，提交”；2026-09-14；未限定分支或步骤，按验收规则视为整个任务客户端流程验收完成。

server launch mode: not captured（服务端由用户管理；本会话未启动、停止或重启）。本次验收对应的进程由 22:38:54 的启动日志标识。

repository commit: `e460582304f741163ce6b1d174a1456eee1600f8`（`fix(quest): resolve quest 14043 Bantus search alias`）；本验收记录与证据说明在其后的文档提交中追加。

working tree: dirty；本次修复提交只包含 `CM_OBJECT_SEARCH.java` 与 `CMObjectSearchTest.java`。工作区其余改动（quests/10032、14045、14046、20032、3934、11468、21468、hotspot teleport 等）与本任务无关，均保留未提交。

Aion 5.8 client/data provenance: Aion 5.8 客户端；客户端步骤与同名解析证据见 `../quest-14043-ventus-search-alias/2026-09-14-ventus-search-alias.zh-CN.md`（`data/Quest/Quest.pak` md5 `27bd5122412bfeb6ae14ab6b0385a2c7`、`data/Npcs/npcs.pak` md5 `2d296a9ba9a9aca5ba1d66f250cc17bd`）；本次未重新采集客户端包或其他附件哈希。

npc template/object: 任务目标 template 278532（班图斯，埃雷修兰塔情报官），运行时对象 122233；同名解析冲突模板 241198（诺斯珀德 DF6 B2_24 精英怪，显示名同为“班图斯”）与 241418（与 241198 共用同一刷新点的昼夜替换怪，即修复前的寻路落点）。

map/instance: 正确目标位于 world 400010000（埃雷修兰塔）；修复前落点为 world 220110000（诺斯珀德）；本次不涉及实例，运行时 instance ID not captured。

steps:

1. 前置：任务 14043 此前已完成（COMPLETE）；GM 目标选中自身。
2. 用 `//quest set 14043 REWARD 8` 将任务拨回班图斯交付阶段（等同现场 `状态=4 步数=8`），`//quest show 14043` 确认状态与变量。
3. 点击任务寻找/寻路，按修复后解析落到 NPC 278532（埃雷修兰塔），随后对话进入领奖。
4. 客户端在 278532 上显示页 10002，动作 1009 进入奖励页 5，动作 23 完成领奖。

source state/status/vars: `REWARD / var0=8`（日志 `状态=4 步数=8`）→ `COMPLETE`（`状态=5 步数=8`）。

action/page/button: 客户端“寻找”按钮（`CM_OBJECT_SEARCH`，0x00D6）→ GM 直接传送；随后对话链路页 10002 → 5，完成动作 23。

expected response: 寻找请求在 14043 的 `START var0=0` / `REWARD var0=6` / `REWARD var0=8` 阶段解析为 278532（埃雷修兰塔），不再命中诺斯珀德 DF6 刷新位的 241198/241418；随后可在 278532 处正常领奖完成任务。

actual response: 22:59:26 任务处于 `REWARD var0=8`；22:59:37–22:59:39 在 NPC 278532（运行时对象 122233）完成页 10002 → 5 → 动作 23，任务进入 `COMPLETE`。用户确认“验证通过”。

startup health: 本次验收窗口（22:38:59 启动完成后）typed 任务引擎正常：22:38:54“任务引擎开始加载”、22:38:55“已加载 typed 正式任务 owner：6193”、22:38:59“启动完成，耗时 21s”（`log/console.log:4101/4105/4304`）。注意：更早的 22:21:18 启动曾报 `Can't initialize typed quest engine` / `AMBIGUOUS_TRANSITION`（`log/console.log:3443-3452`），该构建未用于本次验收；若该报错在当前构建复现，本次验收即失效。

runtime logs: `log/console.log:4476-4482`（2026-09-14 22:59:26–22:59:39）；关键标识 questId=14043、npcId=278532、runtime object=122233、页 10002/5、动作 1009/23、状态 4→5；未单独导出附件，仓库内日志即证据载体。

protocol trace: not captured；`CM_OBJECT_SEARCH` 的请求 npcId 未被服务端日志记录，寻路阶段由用户确认与任务阶段日志共同佐证。

screenshots/recordings and SHA-256: not captured。

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `QUEST_SCOPED_NPC_SEARCH_ALIAS`；匹配字段：GM 寻找同名模板落到非任务对象、别名必须同时限定任务 ID/状态/阶段、修复层在 `CM_OBJECT_SEARCH`、不得影响普通玩家地图标记或做全局同名替换；差异字段：冲突模板是共用同一刷新点的昼夜替换对（241198/241418），因此限域别名覆盖该刷新位的两个模板。代表提交 `8b058d4b4de747d12df9e9af63617619d5eefcf5`，代表测试 `CMObjectSearchTest#resolvesTheQuestAcestesAliasAtTheFirstAndReportStages`。

remaining risks: 未抓到 `CM_OBJECT_SEARCH` 原始 npcId；`START var0=0` 与 `REWARD var0=6` 分支未单独复测（本次复测为 `REWARD var0=8`）；非 GM 玩家的地图标记按 Pattern 合同不作别名；跨地图、重登与放弃/重接路径未单独复测。
