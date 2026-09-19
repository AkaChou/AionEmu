# 待客户端点验顺序清单（按风险排序）

source: `.agents/summary/quest-acceptance/2026-09-18-kill-counter-and-repeat-dialog-pending-client.md`
generated: 2026-09-19（周六）
scope: 13758/13761/13764/13767（5 杀族）、25640/25698（满计数恢复）、2677（重开局）、
13841（无目标领奖）、26930（扣量）
test character assumption: 天族角色（用户当前 59 级）；**魔族任务按用户规则改用天族对等任务点验**
status: 计划文档；本文件不构成验收，逐项验收仍落独立验收记录

## 0. 通用手法

- **起手**：`//quest set <id> START 0`。`//quest start <id>` 仍按 legacy 前置与 `maxlevel_permitted` 判定，
  失败时只给通用提示，不用于点验。
- **直接设变量**：`//quest set <id> <status> <值> [varNum]`，`varNum` 省略写 `var0`，`varNum=1` 写 `var1`
  （击杀计数/交付计数都在 `var1`）。`set ... COMPLETE` 会自增 `completeCount`。
- **给物品**：`//additem <itemId> <数量>`。
- **观察**：选中目标玩家后 `//quest log on` 打印状态与变量；背包数量、任务窗与提示作为 actual 证据。
- **重复周期**：GM 账号（`accessLevel != 0`）不排程 daily/weekly 重复，可立即重接；
  证据 `PlayerQuestStatePort.shouldScheduleRepeat`。13758/15698 的 `cycles="WED"` 只影响下次重复时间，不影响接取。
- **等级**：59 级低于多数用例的 `min-level`（65/68/70），下表已标注哪些必须走 `//quest set` 起手。

## 1. 对等映射（魔族 → 天族）

| 待验任务（魔族） | 天族点验对象 | 依据 |
|---|---|---|
| 25640「Mysterious Organisms in Norsvold」 | 15640「Mysterious Organisms in Iluma」 | 奖励（EXP 25060275 + 186000237×5）、daily、30 杀门控 `var1>=30` 完全一致；地区名互为镜像 |
| 25698「Shadows after the Territory of Spiritus Base」 | 15698「Shadows after the Coast of the Light-Deprived Base」 | 奖励（GOLD 1500000 + EXP 53023500 + 186000500×3）、weekly WED、5 杀门控 `var1>=5` 一致 |
| 26930「[Alliance] Doomsday Weapon」 | 16930「[Alliance] Greater Than the Whole」 | 同为 [Alliance]、min 65、需求 `186000257`×10、奖励 EXP 4463286 一致 |
| 2677「[Instance/Group] Kindling The Flames」 | **无天族镜像** → 用 1742「Immortal Soul」（备选 11053/11202）验证同形重开局路由 | Elyos 侧没有 Beluslan 对等；1687-1690 只有 `selection-sources="unaccepted started"`，没有 COMPLETE 重开局路由。库内 31 条 `complete -> complete SELECT1_1` 同形路由现在都带 `start-eligible`，2677 是最后补齐的一条 |

## 2. 点验顺序（风险高 → 低）

| 顺序 | 用例 | 风险类型 | 天族可直接测？ | 成本 |
|---|---|---|---|---|
| 1 | 26930 扣量（→ 16930） | 道具被多扣/清空，不可逆 | 对等任务 + GM 起手 + 加道具 | 低 |
| 2 | 13841/13845/13849 无目标自动领奖 | 领取路径缺失 → 卡在 REWARD | 是（min 45，59 级可正常接） | 低 |
| 3 | 25640/25698 满计数恢复（→ 15640/15698） | 未满可越级领奖（经济）/ 满计数卡死 | 对等任务（min 68/70 → GM 起手） | 低 |
| 4 | 13758/13761/13764/13767 五杀族 | 击杀口径错误（修复前要 15/12 只） | 是，但 min 65 → GM 起手 | 中 |
| 5 | 2677 重开局（→ 1742 同形验证） | 单一对话路由 → 无法重新开局 | 同形替代（min 45） | 低 | **✅ 2026-09-19 通过** |

### 顺序 1（P1｜道具丢失风险）26930 → 天族 16930

- 目标：[Alliance] Greater Than the Whole，16930（ELYOS，min 65），报告 NPC 804626（Lv65 NON_ATTACKABLE）。
- GM 准备：`//quest set 16930 START 0`；`//additem 186000257 15`（**故意多带 5 个**）。
- 步骤：记录背包 `186000257` 数量 → 与 804626 对话进入交付页（SELECT5）→ 领奖窗口 → 领取奖励 → 再看背包。
- expected：只扣 10，背包剩 5；任务进入 COMPLETE；EXP 4463286 与职业奖励到账。
- 失败判据：剩下 0（按 `ALL` 清空）；或 10 个一个没扣；或进不了领奖窗口。
- 记录字段：背包前后数量（截图/文字）、任务状态、奖励到账。
- 复用边界：天族 16930 还有 `CHECK_USER_HAS_QUEST_ITEM_SIMPLE` 分支，与 26930 的 `npc-item-report`
  不完全同形；本项验证的是「只扣 10、不清空同名多余道具」这条数据合同。26930 本体如需直验，用魔族角色做同样步骤。

### 顺序 2（P1｜领取路径缺失）13841 / 13845 / 13849

- 目标：13841「Occupy Siel's Western Fortress」NPC 263295；13845「Occupy Siel's Eastern Fortress」NPC 263597；
  13849「Secure Sulfur Tree」NPC 264797（均 ELYOS，min 45，daily）。
- 背景：`3b4e7fc4c` 把这三个任务的 `reported-reward-mode="FIXED"` 擦掉，客户端「无目标自动领奖」
  `SELECTED_QUEST_AUTO_REWARD(108)` 落地路由消失；`464df58bb` 已恢复。
- GM 准备：`//quest set 13841 REWARD 0`（三个任一即可，同批修复）。
- 步骤：进入 REWARD 后**不点 NPC**，在客户端任务窗使用该任务的领取奖励按钮（action 108）→ 观察奖励与状态。
- expected：EXP 1407270 + AP 400 到账，任务 COMPLETE。
- 失败判据：点击无反应/报错；任务停在 REWARD；奖励不到账。
- 备注：13845/13849 为同批同型，可顺带点验；正常接取路径（对话 → 击杀 → 报告）为附加项，不是本轮重点。

### 顺序 3（P2｜门禁双向）25640 → 15640、25698 → 15698

- 目标：15640「Mysterious Organisms in Iluma」NPC 806089（min 68，daily，30 杀）；
  15698「Shadows after the Coast of the Light-Deprived Base」NPC 806796（min 70，weekly WED，5 杀）。
- GM 准备（以 15640 为例，15698 把 30 换成 5）：`//quest set 15640 START 1 1` → 未满；
  `//quest set 15640 START 30 1` → 满计数。
- 步骤（正反双向）：
  1. `var1=1` 时与 NPC 对话、选择报告 → **不得**进入领奖窗口（回退关窗/无奖励页）。
  2. `var1=30` 时再对话报告 → **必须**进入 `SHOW_SELECT_QUEST_REWARD_WINDOW1`，领奖后任务 COMPLETE。
- 失败判据：`var1=1` 就进领奖窗口（门禁缺失，可越级领奖）；`var1=30` 仍不给领（满计数恢复路线缺失）。
- 依据：两个 XML 各只有一条 `SELECT_QUEST_REWARD` 路线，条件为 `var1>=30`（15698 为 `>=5`）。
- 备注：真机杀 30 只成本高，用 GM 设变量验证门禁即可；若要顺带确认计数累加，杀 1 只看 `var1` 是否 +1。

### 顺序 4（P2｜击杀口径）13758 / 13761 / 13764 / 13767

- 目标（均 ELYOS，min 65，weekly WED，门控 5 杀）：

| 任务 | 名称 | 报告 NPC | 击杀目标 |
|---|---|---|---|
| 13758 | [Weekly] Cut Throat Tactics | 805269/805270/805271 | 235346/235347 |
| 13761 | [Weekly] Tomuls in the West | 805275/805276/805277 | 235370/235371 |
| 13764 | [Weekly] Beasts in the East | 805272/805273/805274 | 235354/235355 |
| 13767 | [Weekly] Southern Brawlers | 805278/805279/805280 | 235362/235363 |

- GM 准备：`//quest set 13758 START 0`（min 65 > 59 级，必须 GM 起手）；想省时间可先
  `//quest set 13758 START 4 1` 再杀第 5 只。
- 步骤：杀满 5 只 → 观察任务条目与计数器 → 与报告 NPC 对话领奖。
- expected：第 5 只即进入 REWARD（`var1` 0..4 累加、第 5 杀 `set var1=5`），客户端狩猎步骤显示完成，可报告领奖。
- 失败判据：需要第 6 只及以上；或第 5 只后客户端仍显示未完成。
- 对照项：13765（同族、未改计数，击杀 235357）一并确认同为 5 杀，作为 negative control。
- 备注：修复前该族需要 15/12 只（`464df58bb` 修正）。

### 顺序 5（P3｜重开局路由｜✅ 2026-09-19 已通过）2677 → 天族同形 1742

> 结果：用户 2026-09-19 回复「1742 验证成功」。验收记录：
> `.agents/summary/quest-acceptance/1742-2026-09-19-client-accepted.md`。
> 修复 commit `464df58bb`；门禁测试与 Playbook 案例随后续提交落地。

- 直接对象：2677「[Instance/Group] Kindling The Flames」（ASMODIANS，min 45，NPC 204817 Svafnir）。
  修复 `464df58bb` 补齐 `complete -> complete SELECT1_1(1012)` 的 `<start-eligible/>`。
- 天族同形对象：1742「Immortal Soul」（ELYOS，min 45，NPC 278591 Deidamia）；
  备选 11053（NPC 799017）、11202（NPC 799009）。
- 步骤：`//quest set 1742 COMPLETE 0` → 与 278591 对话 → 点任务列表里的该任务行（`SELECT1_1`）→ 观察是否打开开局页。
- expected：开局页正常打开（同一次对话、无 load fail、窗口不闪退），可重新接取。
- 失败判据：点行后客户端回列表/无响应/报 `HtmlPageId` load fail。
- 复用边界：本项证明「带 `start-eligible` 的 COMPLETE 重开局路由」在客户端可用；
  2677 本体仍以静态证据（31/31 条同形路由均带条件）为准，如有魔族角色可用同样步骤直验（NPC 204817）。

## 3. 收口规则

- 每完成一项点验：按 `.agents/rules/quest-repair.md` 落 `<quest>-<date>-client-accepted.md` 验收记录
  （user acceptance confirmation、commit、working tree、steps、expected/actual、未捕获产物、剩余风险）。
- Playbook：本轮 5 组对应 Pattern 已存在（`SINGLE_COUNTER_KILL_GATE_OVERSHOOT` /
  `KILL_COUNTER_COMPLETION_ADVANCES_JOURNAL_ROW` / `LEGACY_START_OWNER_LOSS` 等）时不新增案例，
  只更新对应验收证据；确有新模式时才建案例并跑 `python3 .agents/summary/quest/check_quest_repair_playbook.py`。
- 五组全部点验完成后，本文件标记 CLOSED 并归档到对应验收记录。
