# 1006「飞升」贝尔帕坦步骤点不动（进行中步骤挂在「结束对话」动作上）——客户端验收记录

- 日期：2026-10-22
- 结论：**CLIENT_ACCEPTED** —— 用户实机确认「修复成功，提交、沉淀」
- 记录落位：`docs/quest/1006-2026-10-22-client-accepted.md`。`.agents/summary/quest-acceptance/` 树已于 2026-10-09 整体迁移至 Hindsight MCP（tags: `summary`/`quest-acceptance`）；本记录按仓库持久化文档纪律落 `docs/`，并与 Playbook 记录（PATTERNS 新增指纹行 `LIVE_STEP_ENTRY_MUST_BE_TASK_ROW_ACTION` + 案例 8.67）合并为同一个 `docs(quest)` 提交，摘要同步寄存 Hindsight。

## 待验收批次（修复提交，落库于本次验收之前）

| 提交 | 内容 |
|---|---|
| `6458e9783` | 33 个任务 XML 的 57 条「进行中步骤 + `USE_OBJECT(-1)`」改挂到客户端真会发的任务行动作（25 条 `QUEST_SELECT(31)`、32 条 `EXCHANGE_COIN(59)`）；新增生产目录回归审计 `QuestLiveRowEntryAuditTest`；`Quest1006ClientDialogAlignmentTest` 补贝尔帕坦路由断言；`QuestInteractionObjectTestData` 补 `npcAiNames` |

## 仓库状态

- 记录时 HEAD = `6458e978331a88823d4e81f0e066a0f78c73933f`（修复提交），工作树仅剩本记录与 Playbook 两个文档改动（`git status --porcelain` 为该三项）。
- 提交前工作树无其它未暂存/无关改动（修复提交按显式路径暂存，未使用 `git add -A`）。

## 客户端与数据出处

- 客户端：Aion 5.8 客户端。症状证据取自用户粘贴的服务端日志片段（`CM_DIALOG_SELECT npcId=205000 questId=1006 上一页=10 动作=31` ×3，每次被 `SM_DIALOG_WINDOW questId=0 下发页=10` 应答）。
- 客户端静态数据：Aion 5.8 客户端解包的 quest 表（`quest.xml`，含 `extra_category` 列）与 `data_driven_quest.xml`（客户端任务行表），用于判定 coin_quest 行固定发 `EXCHANGE_COIN(59)`、以及哪些 NPC 根本没有客户端任务行。解包产物位于用户本机客户端解包工作区（具体路径 **not captured**：不在仓库文档中固化仓库外绝对路径）。
- 原版/退役证据：退役 handler `_1006Ascension`（`git show a46c9f35f^:src/main/java/com/aionemu/gameserver/quest/handlers/ascension/_1006Ascension.java`），其 `case START_DIALOG:`（=31）在 205000 触发飞行传送 + 43 秒计时 + `var0 99 -> 50`。

## 实机上下文（NPC/对象、源状态/动作/页面）

- 任务：1006「飞升」；NPC 205000 贝尔帕坦（Belpartan）；源状态 START、source 节点 `s99`（`var0=99`）；入口动作 = 任务列表行点击 31（修复前误绑 `USE_OBJECT(-1)`）；目标节点 `s50`。
- 副作用合同：`flight-teleport 1001`（飞行动画 id 1001）→ `start-invisible-timer 43`（timer-id `1006-return`，scope PLAYER_QUEST、persistence SESSION、overwrite REPLACE、delivery AT_MOST_ONCE）→ `sync-quest-state PACKET_ONLY`；后续 `s50 -> s51` 在地图 310010000 生成 211042 并挂 1000 仇恨。
- 页面：该步骤不经过任务页（服务端直接飞行传送），因此「页面对不上」不是本症状的一部分。

## 预期 vs 实际

| 面 | 修复前（症状） | 修复后（实机） |
|---|---|---|
| 贝尔帕坦步骤入口 | 点 NPC 对话与点任务行都无反应：客户端发 `action=31`，服务端每次回 `questId=0` 通用第 10 页；任务停在 `状态=3 步数=99`，飞行传送与 43 秒无敌计时都不触发 | 任务行点击进入 `s99 -> s50`，飞行传送与无敌计时正常触发，任务继续推进（用户 2026-10-22 确认「修复成功」） |
| 同族 32 个任务 | 同形步骤同样不可达（未逐个实机） | 按同形合同改挂 `QUEST_SELECT(31)` / `EXCHANGE_COIN(59)`；随 1006 一并验收 |

## 启动健康度与日志

- 服务端进程由用户自行管理，AI 未启停、未重启。用户重启后复测通过（**not captured**：启动日志片段与修复后的服务端日志均未采集；用户仅反馈修复成功）。
- 症状期日志（用户提供）：`CM_DIALOG_SELECT npcId=205000 questId=1006 上一页=10 动作=31` ×3 → `SM_DIALOG_WINDOW questId=0 下发页=10` ×3；`SM_QUEST_ACTION 任务=1006 状态=3 步数=99`。
- 截图/协议抓包：**not captured**（其缺失不影响用户实机游戏验收的有效性）。

## 门禁（记录时）

- IDEA MCP（2026-10-22 会话内，用户授权「聚焦测试 + 任务目录编译门禁」）：`QuestLiveRowEntryAuditTest`、`Quest1006ClientDialogAlignmentTest`、`QuestProductionStartupGateTest`、`QuestInteractionObjectContractGateTest`、`ProductionCatalogWhitelistVerificationTest` 全部通过（生产 catalog 编译 OK=707、失败 0、白名单违规 0、交互对象合同失败 0）。
- 审计有效性反向验证：把 1006 临时改回 `USE_OBJECT(-1)` 后 `QuestLiveRowEntryAuditTest` 确实报 `1006/205000/s99`，随即还原。
- 本次 docs 提交未再运行构建/测试命令。

## 验收状态与残留风险

- 验收状态：CLIENT_ACCEPTED（用户实机确认「修复成功」）。
- 残留风险：
  ① 回归审计里逐条列名的 15 条同形例外未改（传送门 24026/24051/24154；交互物件面 3200/18602/36506/36510/46511/50019/50020/51019；未接线的巧克力塔 80300/80301/80306/80307）——它们缺退役 handler / 客户端数据佐证，改前必须各自取证；其中 3200/50019/50020/51019 一族（live 步骤靠 `-1` 推进）仍可能是真缺陷。2620/204787/s1 与 2919/204206/bookInserted 已按 1929 同形补 `QUEST_SELECT(31)` 行入口（尚未实机验收）。
  ② 8.49 `ACTIVE_NPC_DIALOG_MISSING_DIRECT_ENTRY` 那种「客户端没有任务行」的 NPC，其「首次点击 -1 直接入口」在开门不再认领进行中任务后同样不再由开门投递，本次未取证、未改。
  ③ 同族 33 个任务中仅 1006 做了实机验收，其余 32 个任务按同形合同改挂，未逐个实机。
  ④ 改挂动作的判据是客户端任务表的 `extra_category` 与退役 handler 的 `START_DIALOG` 证据；若某任务的客户端实际行动作不是 31/59，需按该任务自身证据重判。
  ⑤ 2026-10-24 复核更新：①中的「污名对话面 1929」不是合法例外——装备后 `equipped96` 只有 `USE_OBJECT(-1)`，任务行点击（31）复现 `page=10 → action=31 → page=10` 死循环；已补 `QUEST_SELECT(31) -> SELECT6` 并从审计例外移除（Playbook 8.67 后续复核），用户实机复测确认修复成功。
