# 任务 26800 客户端验收记录

quest: 26800

user acceptance confirmation: 用户于 2026-08-20 明确回复“26800 已完成”，确认范围为完整任务，不限于单一步骤。

server launch mode: not captured

repository commit: `5511223b0e0363514a960beaf02577f1659541ce` (`fix(quest): restore quest 26800 tower flow`)

working tree: dirty；任务 26800 XML 与专项测试相对修复提交无后续差异，其他未提交文件不属于本验收记录。

Aion 5.8 client/data provenance: Aion 5.8 客户端；页面/action 来源见 `docs/quest/client-dialog-mapping/README.zh-CN.md`，本次未重新采集客户端包哈希。

npc template/object: 接取与领奖 NPC `806079/806149`，中间交付 NPC `806233`，永恒之塔入口 `806082`，知识书库入口 `806029`；运行时 object ID not captured。

map/instance: 诺斯珀德 `220110000` -> 永恒之塔 `220120000` -> 知识书库 `301540000`；instance ID not captured。用户曾在永恒之塔内通过 `//quest show 26800` 确认 `START var0=2`，随后沿正式 `806029` portal 继续。

steps:

1. 与 `806079` 完成简易接取，得到 `START var0=0`。
2. 使用活动生成的 `806082` 进入 `220120000`，区域事件将进度推进到 `START var0=1`。
3. 与 `806233` 对话并执行 `SET_SUCCEED(10255)`，推进到 `START var0=2`。
4. 使用 `806029` 进入 `301540000`，区域事件提交 `REWARD var0=3` 后播放电影 932。
5. 与 `806149` 领取奖励并完成任务。

source state/status/vars: `NONE var0=0`；依次经过 `START var0=0/1/2` 和 `REWARD var0=3`。

action/page/button: `806079` 简易接取页链；`806233` 的 `SELECT2 -> SELECT2_1 -> SET_SUCCEED`；`806149` 的 `DEFAULT_SUCCESS -> SHOW_SELECT_QUEST_REWARD_WINDOW1`。portal `806082` 使用 dialog 104，portal `806029` 使用 dialog 10000。

expected response: 每个权威区域和 NPC 只在对应阶段推进；知识书库入口按 `commit REWARD var0=3 -> LEVEL_AND_VISIBILITY_REFRESH -> movie 932` 执行；`806149` 是唯一奖励完成 owner。

actual response: 用户完成上述完整路径并成功完成任务 26800。中途截图曾证明状态已到 `START var0=2`；临时缓存附件未保留，因此不作为稳定附件引用。

startup health: 修复提交验收前专项测试 4/4、生产 catalog 6200/6200、失败 0、白名单违规 0；本次运行的启动日志 not captured，用户未报告 typed quest engine 初始化或 catalog 编译错误。

runtime logs: not captured

protocol trace: not captured

screenshots/recordings and SHA-256: not captured；聊天中的临时缓存截图已不可访问。

acceptance status: ACCEPTED_NEW_PATTERN

matched Pattern: `CROSS_MAP_ENTER_ZONE_PHASED_FLOW`；匹配字段为 task object/portal owner 分离、两段 `ENTER_ZONE` 持久阶段、NPC handoff、电影时机和唯一 reward owner。与 `MULTI_NPC_HANDOFF_REWARD_OWNER` 的差异是本任务还要求两个 portal 的 loc/world 合同和两次跨地图区域事件。代表提交 `5511223b0`；代表测试 `Quest26800ClientDialogAlignmentTest#advancesThroughTheTowerAndEnfitentaOnlyAtTheAuthoritativeStages`。

remaining risks: 未单独抓取 packet 字段与顺序，未记录运行时 object ID、instance ID、服务器启动方式或重登/实例重建分支；用户的完整任务完成确认仍构成本任务客户端验收。
