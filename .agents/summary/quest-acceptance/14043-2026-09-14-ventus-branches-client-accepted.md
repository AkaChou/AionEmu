# 任务 14043 班图斯分支补充验收记录

quest: 14043「Drawling Balaur / 学习龙族语」；本次补记的对象是寻找班图斯（278532）的三个阶段：`START var0=0` 起始对话、`REWARD var0=6` 与 `REWARD var0=8` 两种交付/领奖分支。

user acceptance confirmation: 用户 2026-09-14 原话“验证通过，提交”，未限定分支或步骤，按验收规则构成整个任务客户端游玩验收的权威证据；本次用户在追问后指示“补一次验收记”，要求把同一次验收的其余班图斯分支一并记录。分支本身未另行声明受限范围。用户于同日再次确认“14043 已经正常完成了”（整任务、未限定分支），按验收规则不要求重复游玩或补截图材料。

server launch mode: not captured（服务端由用户管理；本会话未启动、停止或重启）。本次验收窗口由 22:38:54 的启动日志标识。

repository commit: `e460582304f741163ce6b1d174a1456eee1600f8`（`fix(quest): resolve quest 14043 Bantus search alias`）；首份记录 `1791c9811 docs(quest): record quest 14043 client acceptance`；本补充记录单独提交。

working tree: dirty；本源修复提交只包含 `CM_OBJECT_SEARCH.java` 与 `CMObjectSearchTest.java`。工作区其余改动（quests/10032、11468、20032、21468、3934、hotspot teleport、memory-bank 等）属于其他进行中工作，均保留未提交。

Aion 5.8 client/data provenance: Aion 5.8 客户端；仓库内证据为 `../../docs/quest/client-dialog-mapping/quest-dialog-pages.csv`、`quest-dialog-action-details.csv`、`quest-sequence-audit.csv:11803-11805`、`quest-order-audit.csv:53069-53087`，客户端页面为 `10000_19999/quest_q14043.html`；客户端包 md5 与同名解析链见 `../quest-14043-ventus-search-alias/2026-09-14-ventus-search-alias.zh-CN.md`（`data/Quest/Quest.pak` md5 `27bd5122412bfeb6ae14ab6b0385a2c7`、`data/Npcs/npcs.pak` md5 `2d296a9ba9a9aca5ba1d66f250cc17bd`）。本次未重新采集客户端包哈希。

npc template/object: 三个阶段的权威目标均为 template 278532（班图斯，埃雷修兰塔情报官），静态刷点 `spawns/Npcs/400010000_Reshanta.xml:643`；同名解析冲突模板 241198（诺斯珀德 DF6 B2_24 精英怪）与 241418（同一刷新点的昼夜替换怪）。本次唯一捕获到的运行时对象是 `REWARD var0=8` 阶段的 122233；`START var0=0`、`REWARD var0=6` 的 runtime object ID not captured。

map/instance: 正确目标位于 world 400010000（埃雷修兰塔）；修复前寻路落点为 world 220110000（诺斯珀德）。本次不涉及实例，instance ID not captured。

steps:

1. 前置：任务 14043 已达到 `COMPLETE`；GM 目标选中自身。
2. 起始分支：`//quest set 14043 START 0` 回到第一步（客户端第 1 步文本为“和埃雷修兰塔的班图斯对话”），点击寻找应解析到 278532。
3. 短分支交付：`//quest set 14043 REWARD 6` 进入 `reward6`（任务 XML `quests/14043.xml:347-355` 的 `npc-complete npc-id=278532`）。
4. 长分支交付：`//quest set 14043 REWARD 8` 进入 `reward8`（`quests/14043.xml:358-366`），本次捕获的即此阶段：客户端在 278532 上显示页 10002，动作 1009 进入奖励页 5，动作 23 完成任务。
5. 每个阶段都可先用 `//quest show 14043` 确认 `Status/Vars` 后再点击寻找。

source state/status/vars: `START / var0=0`（起始对话）；`REWARD / var0=6`；`REWARD / var0=8`（本次实测由 `状态=4 步数=8` 推进到 `COMPLETE`）。

action/page/button: 客户端“寻找”按钮（`CM_OBJECT_SEARCH`，0x00D6）→ GM 直接传送；交付链路页 `10002 -> 5`，完成动作 23。三个阶段使用同一寻找入口与同一交付页链。

expected response: 在 `START var0=0`、`REWARD var0=6`、`REWARD var0=8` 三个阶段，客户端寻找请求都解析为 278532（埃雷修兰塔），不再命中诺斯珀德 DF6 刷新位的 241198/241418；随后在 278532 处可正常交付/领奖。

actual response: 用户确认“验证通过”（未限定分支，视为整个任务可用）；本次运行时日志捕获到 `REWARD var0=8` 的完整交付：22:59:26 进入该阶段，22:59:37–22:59:39 在 278532（对象 122233）完成页 10002 → 5 → 动作 23，任务进入 `COMPLETE`。`START var0=0` 与 `REWARD var0=6` 的独立运行时痕迹 not captured；用户已确认“14043 已经正常完成了”，按整体确认记录为通过，不再要求补材料。

startup health: 本次验收窗口的进程于 22:38:54–22:38:59 启动：“任务引擎开始加载”→“已加载 typed 正式任务 owner：6193”→“启动完成，耗时 21s”（`log/console.log:4101/4105/4304`），窗口内未出现 typed 引擎或生产 catalog 错误。注意更早 22:21:18 的另一次启动曾报 `Can't initialize typed quest engine` / `AMBIGUOUS_TRANSITION`（`log/console.log:3443-3452`），该构建未用于验收；若该报错在当前构建复现，本次验收即失效。

runtime logs: `log/console.log:4476-4482`（22:59:26–22:59:39，`REWARD var0=8` 交付完成）；`log/console.log:2142-2160`（21:42:13–21:42:27，同任务中间阶段 `s7 -> s8 -> REWARD` 的客户端页链 1352/1353/1354/3057，用于核对交付前的对话链）。未单独导出附件，仓库内日志即证据载体。

protocol trace: not captured；`CM_OBJECT_SEARCH` 的请求 npcId 未被服务端日志记录；三个阶段的寻找请求均由用户整体确认佐证。

screenshots/recordings and SHA-256: not captured。

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `QUEST_SCOPED_NPC_SEARCH_ALIAS`；匹配字段：GM 寻找同名模板落到非任务对象、别名必须同时限定任务 ID/状态/阶段、修复层在 `CM_OBJECT_SEARCH`、不得影响普通玩家地图标记或做全局替换；差异字段：冲突模板是共用同一刷新点的昼夜替换对（241198/241418），因此限域别名覆盖该刷新位的两个模板，且本次覆盖三个阶段（`START 0`、`REWARD 6`、`REWARD 8`）。代表提交 `8b058d4b4de747d12df9e9af63617619d5eefcf5`，代表测试 `CMObjectSearchTest#resolvesTheQuestAcestesAliasAtTheFirstAndReportStages`。

remaining risks: 未抓到 `CM_OBJECT_SEARCH` 原始 npcId；`START var0=0` 与 `REWARD var0=6` 无独立运行时痕迹与截图，但已由用户“14043 已经正常完成了”的整体确认覆盖；非 GM 玩家的地图标记按 Pattern 合同不作别名；跨地图、重登、放弃/重接路径未单独复测。另注：`docs/quest/client-dialog-mapping/quest-order-audit.csv` 的 14043 行已于 2026-09-14 用当前 IR 重新生成（全表生成后只替换 14043 的 27 行，单一 diff hunk）：中间页 1352/1353/1354/1355/1375/1438/1693/1694/2034/2035/2375/2376/3057 由 `CLIENT_PAGE_UNREACHED` / `EVIDENCE_REQUIRED` 转为 `PAGE_ACTION_MATCHED`，与 21:42 的实机日志一致；仅剩客户端重复页 `select9`(3739) 为 `CLIENT_PAGE_UNREACHED`（与 `select7`/3057 同文案同动作、IR 从不下发，属未使用页，无玩家影响）。
