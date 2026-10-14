# 80790（事件族 80787-80790）完成三件套 / 报告收口步轴 / REWARD 翻转可见性刷新——客户端验收记录

- 日期：2026-10-14
- 结论：**CLIENT_ACCEPTED** —— 用户实机确认「实机验证成功，提交」
- 记录落位：`docs/quest/80790-2026-10-14-client-accepted.md`。原 `.agents/summary/quest-acceptance/` 树已于 2026-10-09 整体迁移至 Hindsight MCP（tags: `summary`/`quest-acceptance`）；本记录按仓库持久化文档纪律落 `docs/`，并与 Playbook 记录（PATTERNS 3 个新指纹行 + 案例 8.65）合并为同一个 `docs(quest)` 提交，摘要同步寄存 Hindsight。

## 待验收批次（修复提交，均在本次验收前已落库）

| 提交 | 时间 | 内容 |
|---|---|---|
| `3325bf15e` | 2026-10-13 21:20 | 完成路径三件套配对：`PlayerQuestStateSyncPort` COMPLETION 模式与 `QuestService.setFinishingState` 在 update 包后补 `removeQuestFromClientList` + `SM_QUEST_COMPLETED_LIST`；新增 `PlayerQuestStateSyncPortCompletionPacketTest` |
| `05dbecbe8` | 2026-10-13 21:27 | 同型面收口：`commands/admin/Quest`、`GiveStigma`、`CmdDeleteQuest`（非 MISSION 分支）、`RelinquishCraftStatus`（master+expert 两循环）补齐 action3 + 已完成列表配对 |
| `9c921397d` | 2026-10-13 23:51 | `NativeQuestStartPort.refreshVisibility(Player)` 提升为共享入口（javadoc 锁「状态包之后调用」合同）；tablelane 七族 REWARD 翻转点尾部补刷新；两个锁顺序合同的测试 |
| `994d864cc` | 2026-10-13 23:51 | 测试对齐：`SimpleTalkNativeFamilyGateTest` 中继已完步断言改为「不回放」（锁 relay 永不回放合同，测试层） |
| `d4fe78001` | 2026-10-14 00:07 | `DataDrivenNativeRuntime` 零步 Talk 报告收口（update + `DataDrivenProgress.advance` 派生步）；非 tablelane 翻转尾巴 5 处（`RiftOrbAI2`、`GiveStigma.completeQuest`、`cmd_questauto`、`cmd_questrestart`、`ClassChangeService.completeQuest`）补可见性刷新 |

## 仓库状态

- 记录时 HEAD = `d4fe78001f9d6fa878e229d314cd8639b94c052d`，工作树干净（`git status --porcelain` 为空）。

## 客户端与数据出处

- 客户端：Aion 5.8 客户端 `patch/bin64/Game.dll`，SHA-256 `7b635cd3d5a747fe89ca79442480e318a4aceeb4c235be64834e8283d8d9322d`。`SM_QUEST_ACTION` 三动作语义（action 1 无条件插入不查重 / action 2 二分未命中忽略整包、新 status==COMPLETE 移入已完成表 / action 3 未命中静默、命中移除并弹移除消息）与头顶标记重算时机（仅 0x7f `SM_NEARBY_QUESTS` 刷新时）由 REA + Ghidra 对 opcode 0x7c → `FUN_106d7b30` 全链解出（分析产物 `~/IdeaProjects/aion-ref/Game.dll/`，只读）。
- 静态数据：原版表 `data_driven_quest.xml` 与客户端任务表解包（`~/PycharmProjects/unpak/Quest_unpacked/`，只读）。

## 实机上下文（源状态 / 动作 / 页面）

- 任务：80790（对偶 80789），事件族 80787-80790「Talk 零步任务」；源状态 START 步 0 ／ 交付动作 = Talk 报告确认（31/1009）／ 页面 = 奖励窗（5）；完成路径 → COMPLETE。
- GM 走轴校准：`//quest set 80790 start 0` → `//quest set 80790 reward 1`（`log/adminaudit.log` 2026-10-13 23:29）。
- 具体地图/NPC 编号本轮未单独截取（**not captured**；同族 80788 验收记录已寄存 Hindsight `summary`/`quest-acceptance`）。

## 预期 vs 实际

| 面 | 修复前（症状） | 修复后（实机） |
|---|---|---|
| 报告收口轴（80790） | 任务书领奖态停在进行中步 `reward/0`（应 `reward/1`）；80789/80790 表现不一致 | `start/0 → reward/1` 正确走轴；80789/80790 一致（GM 走轴 2026-10-13 校准 + 本轮整体验收确认） |
| 完成链路（80788-80790） | 完成任务弹「任务失败」（action3 缺 action2 配对）；可重复任务完成行滞留、下轮累积重复行（缺 action3 + 已完成列表） | 完成无「失败」弹窗，行正常进入已完成列表（用户实机确认） |
| REWARD 翻转可见性（1102/1103） | 杀满/报告确认进 REWARD 后交付 NPC 头顶无任务标记，重登才出现 | 切换即现，不重登（2026-10-13 用户确认） |

## 实机复核证据（日志）

- 服务端由用户于 2026-10-14 09:09 前后重启（`log/adminaudit.log` 09:09:34 聊天服务加载）；重启后 09:18-09:21 复核 1107/1110/1113/1114 完成链路全部正常，其中 1113 报告轴 `状态=4 步数=8` → `状态=5` 完成、无「任务失败」弹窗（`log/quests.log`）。
- 80789/80790 完成链路 trace：2026-10-13 18:58-23:33 多条 `SM_QUEST_ACTION 状态=3/4/5` 序列（`log/quests.log`）。
- 已知盲区：`quests.log` 不记录 action 码，action 3 在日志中不可见（跨动作核对只能按代码路径）。
- 截图/协议抓包：**not captured**（其缺失不影响用户实机游戏验收的有效性）。
- 进程生命周期：由用户自行管理，AI 未启停。

## 门禁（记录时）

- IDEA MCP（2026-10-14 会话内，用户授权）：`DataDrivenNativeRuntimeGateTest` 与 6 个 tablelane 聚焦类共 134 例全部通过、0 失败。
- 本轮提交（docs）未再运行任何构建/测试命令。

## 验收状态与残留风险

- 验收状态：CLIENT_ACCEPTED（用户实机确认「实机验证成功」）。
- 残留风险：① `NativeQuestStartPort.refreshVisibility` 为 best-effort（静默捕获 `RuntimeException`）——未来「标记不亮」首查此处与调用点；② `RiftOrbAI2` 等非 tablelane 修复点为结构性同型收口，未逐点实机；③ DataDriven apply 0x280f 收口后可能重复下发一次幂等 0x7f 区域清单（无功能影响）；④ `ClassChangeService`/`startEventQuest` 相关路径位于默认 false 配置门后，未采样。
