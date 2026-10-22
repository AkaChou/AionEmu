# 1007「A Ceremony In Sanctum」Jucleas 对话无弹窗无影片、直接跳到 Reward 30——客户端验收记录

- 日期：2026-10-22
- 结论：**CLIENT_ACCEPTED** —— 用户实机确认「实机验证成功，提交、沉淀」
- 记录落位：`docs/quest/1007-2026-10-22-client-accepted.md`。`.agents/summary/quest-acceptance/` 树已于 2026-10-09 整体迁移至 Hindsight MCP（tags: `summary`/`quest-acceptance`）；本记录按仓库持久化文档纪律落 `docs/`，并与 Playbook 记录（PATTERNS 新增指纹行 `OPEN_DOOR_MUST_NOT_PRESS_PAGE_CHAIN_DELIVERY` + 案例 8.68）合并为同一个 `docs(quest)` 提交，摘要同步寄存 Hindsight。

## 待验收批次（修复提交，落库于本次验收之前）

| 提交 | 内容 |
|---|---|
| `e58a44a79` | `QuestDialogRouter` 开门兜底新增「客户端页链」门：交付按钮在子页（`SELECT⟨n⟩_1`）上的步不再被开门替按；新增 2 例回归测试（合成页链步 + 生产 1007/2009 步） |

## 仓库状态

- 记录时 HEAD = `e58a44a79031f6d58fb10959a5dfa1226b17e778`（修复提交），工作树仅剩本记录与 Playbook 两个文档改动。
- 提交前工作树无其它未暂存/无关改动（修复提交按显式路径暂存，未使用 `git add -A`）。

## 客户端与数据出处

- 客户端：Aion 5.8 客户端。症状证据取自用户提供的任务状态截图（对话前 `(Quest Id: 1007) Status: START / Vars: 2 0 0 0 0 0 / Complete count: 0`，对话后 `Reward 30`）。
- 客户端静态数据：Aion 5.8 客户端解包的 `Dialogs/QUEST_Q1007.html`（select3 页按钮 = `HACTION_SELECT3_1`(1694)、select3_1 页按钮 = `HACTION_SETPRO3`(10002)）与 `HyperLinks.xml`（动作 id 映射：31=QUEST_SELECT、1694=SELECT3_1、10002=SETPRO3、10255=SET_SUCCEED、1009=SELECT_QUEST_REWARD）；对照页 `Dialogs/10000_19999/quest_q19679.html`（select1 页按钮 = `HACTION_SET_SUCCEED`，单按钮交付步）；客户端任务行表 `data_driven_quest.xml`（2155 行，不含 1007/1006/19679）。解包产物位于用户本机客户端解包工作区（具体路径 **not captured**：不在仓库文档中固化仓库外绝对路径）。
- 原版/退役证据：退役 handler `_1007A_Ceremony_In_Sanctum`（`git show 56c28337f^:src/main/java/com/aionemu/gameserver/quest/handlers/ascension/_1007A_Ceremony_In_Sanctum.java`）：`case START_DIALOG:`（=31，任务行点击动作）在 var==2 时 `sendQuestDialog(env, 1693)`；`case SELECT_ACTION_1694:` → `playQuestMovie(env, 91)`；`case STEP_TO_3:`（=10002）→ 按起始职业写 var 10/20/30/40/50/60 + `QuestStatus.REWARD` + `sendQuestSelectionDialog(env)`。

## 实机上下文（NPC/对象、源状态/动作/页面）

- 任务：1007「A Ceremony In Sanctum」（天族）；魔族镜像 2009 同形（Kalsten 204075）。
- NPC：203752 Jucleas（登记面：`retail-quest-ai-registrations.xml` 的 quest 1007 = 203752/203758/203759/203760/790001/801212/801213）。
- 源状态：START、source 节点 `s2`（`var0=2`）；目标：职业分档 REWARD 节点（法师 = `reward30` / `var0=30`）。
- 客户端对话链（与 `QUEST_Q1007.html` 一致）：任务行点击 31 → 页 1693（select3）→ 按钮 1694 → 影片 91 + 页 1694（select3_1）→ 按钮 10002（SETPRO3）→ 进 REWARD。
- 服务端侧效应合同：`s2 -> rewardNN` 的 after-commit = `sync-quest-state LEVEL_AND_VISIBILITY_REFRESH` → `close-dialog`；`1007.xml`/`2009.xml` 未改动（与客户端页契约、退役 handler 一致）。

## 预期 vs 实际

| 面 | 修复前（症状） | 修复后（实机） |
|---|---|---|
| 与 Jucleas 对话 | 没有对话弹窗、没有影片，任务从 `s2`（START/var0=2）直接落到 `Reward 30` | 先出任务清单 → 点任务行出 select3(1693) → 按「继续听」出影片 91 + select3_1(1694) → 按「结束对话」才进 `Reward 30`（用户 2026-10-22 实机确认通过） |
| 根因面 | 开门兜底（`QuestDialogRouter.unlistedDeliveryTransition`）把交付转换自己的动作 id（SETPRO3=10002）当成客户端动作重放，跳过整条页链 | 兜底新增页链门：同 source/NPC 另有子页续页路由时，交付按钮在更后一页，兜底不认领 |
| 同族步 | 全库 138 个「页链步」同样会被开门替按（1006 s5、2008 s6、3933/3934/3935/3939/3940@203752、2009@204075 等） | 一并受门约束；138 个任务均带客户端 `quest_q<id>.html`，页链仍可渲染 |
| 单按钮交付步 | 19678/19679 在 806698 由兜底推进到 REWARD（2026-10-20 修复） | 兜底保留（该步无子页续页路由），不回退 |

## 启动健康度与日志

- 服务端进程由用户自行管理，AI 未启停、未重启。用户重启后复测通过（**not captured**：启动日志片段与修复后的服务端日志均未采集；用户仅反馈实机验证成功）。
- 症状期日志：**not captured**（仓库 `log/quests.log` 止于 2026-10-22 17:21:33 的 1006 会话，`log/console.log` 中没有 NPC 203752 记录；用户以任务状态截图报告症状）。
- 截图/协议抓包：仅任务状态截图（对话前 START/var0=2、对话后 Reward 30），无协议抓包。

## 门禁（记录时）

- IDEA MCP（2026-10-22 会话内，用户授权「聚焦测试 + 目录门禁」）：`QuestDialogUnlistedRowEntryTest` 8 例全绿（新增 `aStepWithAClientPageChainIsNotAdvanced`、`theProductionCeremonyStepsAreNotAdvancedByTheOpen`）、`Quest1007MovieDialogTest`、`Quest2009MovieDialogTest`、`ProductionCatalogWhitelistVerificationTest`（生产 catalog 编译 OK=707、失败 0、白名单违规 0、交互对象合同失败 0）、`QuestProductionStartupGateTest`、`QuestLiveRowEntryAuditTest` 全部通过。
- 本次 docs 提交未再运行构建/测试命令。

## 验收状态与残留风险

- 验收状态：CLIENT_ACCEPTED（用户实机确认「实机验证成功」）。
- 残留风险：
  ① 兜底仍会替玩家按「该步第一页上的交付按钮」（415 个单按钮交付步，含 19678/19679）——这是 2026-10-20 为未列表行保留的救援路径，语义上仍等于替玩家点一次；若将来要收紧，需另案取证客户端任务行渲染口径。
  ② 兜底门依赖「子页续页路由」的形状（动作 id 命中 `QuestDialogPage.isSelectionSubPage` 且 after-commit 回发该子页）；若某任务把页链写成非子页动作，兜底仍会替按，改前必须按该任务自身客户端证据重判。
  ③ 全库 138 个失去兜底的页链步只有 1007/2009 一族做了实机验收，其余按同形合同改判，页链可渲染性依据是客户端 `quest_q<id>.html` 存在性普查（141 个候选任务全部命中）。
  ④ 「客户端任务行由服务端任务同步驱动、不依赖客户端 DD 表」这一取证结论来自 2026-10-19/2026-10-22 两段实机 trace（19679@806698、1006@790001），未写成客户端行为契约文档。
