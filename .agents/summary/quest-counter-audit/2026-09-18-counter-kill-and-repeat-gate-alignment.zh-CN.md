# 2026-09-18 剩余 10 项 questEngine 失败收口：计数器真端合同与门禁口径

## 范围

`com.aionemu.gameserver.questEngine.**.*Test` 在 `e57e63c5c` 之后仍有 10 项失败，按性质分三类：

| 类 | 数量 | 任务 | 性质 |
|---|---|---|---|
| A | 5 | 13765、19636、19640、23920、50073 | 陈旧断言形状（旧「每次击杀一条 KillNpc 转换」链式结构） |
| B | 2 | 25640、25698 | 门禁口径与「满计数恢复路线」既有决策冲突 |
| C | 3 | 2677、26930、13841（连带 13845/13849） | 需真端/客户端证据裁决，其中 2 项是代码回归 |

## A 类：断言形状对齐「KillNpcSet + var1 计数器」

`3b4e7fc4c` 起，多杀任务统一为 **单条 npc-set 击杀转换累加 `var1`，第二条满计数后进入 reward**。
`metadata.kills()` 只被测试消费（`QuestMetadata.kills()` 在生产代码中无调用点），真正的运行时合同是
转换里的 `var1` 阈值。

- **19636 / 19640（10 杀）**、**23920（10 杀）**、**50073 / 50074（15 杀）**、**13765（5 杀）**：
  测试改为断言 `KillNpcSet` 覆盖的怪物集合 + 计数器上限/收口动作（`set-variable var1 = N`），
  并保留 reward 分支数、报告 NPC、领奖页等原有保护点；不再以「每个怪物各 10 条 KillNpc」这种
  已不存在的链式形状作为门禁。

## A 类附带的真端偏差修正：13758–13769 家族应为 **5 杀**

三份互相独立的证据一致指向「一条狩猎步骤、5 次击杀」：

1. 客户端 `quest_monster.csv`：**12 个任务全部** `Progress(SECTION_0==0; SECTION_1<5)`；
2. 客户端 `data_driven_quest.xml`：`value0_progress_ = LDF4_Advance_*_65_* 5;`；
3. `911440146:src/main/java/quest/levinshor/_1375x_*.java`：全部为 `var1 < 5` → `var1 >= 5` 转 REWARD。

`b771eef59`（"13758-13769 击杀链延长至客户端数量(15/8/20/12/6/20)"）按错误读数把
13758/13761/13764/13767 的 `var1` 目标延长到 15/12/15/12，`3b4e7fc4c` 把结构改成计数器时沿用了这些错值，
于是玩家需要多杀 7–10 只。本次：

- 13758/13761/13764/13767：`var1` 位域上限、`below/at-least` 门槛、收口 `set-variable`、reward 节点值、
  三条满计数恢复路线全部回落到 **5**；
- 12 个任务的 `<kills>` 声明统一为 **单条狩猎步骤**（列出怪物集合），与计数器、客户端表一致
  （原 8/15/20/12/6/20 条与任何计数证据都不符）。

## B 类：门禁口径对齐「满计数恢复路线」

25640/25698 的 `QuestPrematureRewardRouteExclusionTest` 既有决策是：**未满计数不得报告/领奖，满计数必须能从 START 恢复报告**。
两个客户端对齐门禁却断言「START 态不得存在 `QUEST_SELECT` 路由」，与恢复路线直接冲突
（且引用的 `h30`/`h5` 中间节点已随计数器改造消失）。改为正反双向：

- 正向：START 态进入 REWARD 的对话路由**只有** `QUEST_SELECT(31)` 与 `SELECT_QUEST_REWARD(1009)`，
  且两条都必须带 `variable-at-least var1 = N`、仅置 `var0=1`，并分别下发 `DEFAULT_SUCCESS` /
  `SHOW_SELECT_QUEST_REWARD_WINDOW1`；
- 反向：除这两条满计数路线外，任何 START 态路由都不得打开领奖窗口；
- 逐计数早领检测并入 `QuestPrematureRewardRouteExclusionTest` 的 `counter(25640, 30, 806101)` /
  `counter(25698, 5, 806804)` 用例（0..N-1 全部不得进入 REWARD）。

> 注：NPC_START 块会额外生成 START 态的 `FINISH_DIALOG(1008)` 选择窗口路由，其目标是 START 且只显示选择页，
> 不属于报告/领奖路径，门禁按「进入 REWARD」而不是「存在任意对话路由」判定。

## C 类：三项证据裁决

### 1) 2677：`complete → complete` 重复开局缺 `start-eligible`（代码回归）

`f00d6e538` 用生成的 `<dialog type="NPC_START">` 替换手写开局路由时，新增的
`complete → complete SELECT1_1(1012)` 漏掉了 `<start-eligible/>`。运行期
`QuestMutationPlanner.matchesSourceStatus` 明确要求：COMPLETE 状态下只有带 `StartEligible` 的
转换才能跨过「未接取」边界；生产惯例（1742/2317/11202 等 44 个任务）也是
`unaccepted` 镜像路由无条件下发页面、`complete` 路由带 `start-eligible`。

- 全库 2735 条「可重复任务开局对话」门禁检查中，**仅此 1 条**缺失（临时审计用例实测），故按惯例补条件。

### 2) 13841 / 13845 / 13849：丢失 `reported-reward-mode="FIXED"`（代码回归）

`3b4e7fc4c` 重写 `<transitions>` 开标签时把这三个任务的 `reported-reward-mode="FIXED"` 擦掉
（同批的 13947 保留可见对照），导致客户端「无目标自动领奖」`SELECTED_QUEST_AUTO_REWARD(108)`
落地路由消失，`QuestReportedRewardCoverageTest` 断言的 182 个可实时报告任务缺 1 组。已恢复该属性。

### 3) 26930：断言过期（数据侧已按真端修正）

`f00d6e538` 已把 `<npc-item-report>` 从 `remove-count="ALL"` 改为 `remove-count="10"`，证据为
客户端 `quest.xml` 的 `<collect_item1> item_idruneweapon_quest_01 10`，以及旧 handler
`checkQuestItems(env, 0, 0, true, 5, 2716)` → `QuestService.collectItemCheck(env, true)` 精确
`decreaseByItemId(itemId, collectItem.getCount())`（并不清空全部同名道具）。测试断言仍停留在旧值，改为
断言扣除 **10** 并显式否决 `ALL`。

## 验证证据（本机 2026-09-18）

| 范围 | 命令 | 结果 |
|---|---|---|
| 聚焦 10 项 + 相邻门禁 | `mvn -o test -Dtest='Quest13765RetailAlignmentTest,Quest19636RetailAlignmentTest,Quest19640RetailAlignmentTest,Quest25640ClientDialogAlignmentTest,Quest25698ClientDialogAlignmentTest,QuestA03ShardRetailAlignmentTest,QuestDefinitionCatalogManifestTest,QuestEventShardRetailAlignmentTest,QuestReportedRewardCoverageTest,QuestPrematureRewardRouteExclusionTest'` | 通过 |
| questEngine 全包 | `mvn -o test -Dtest='com.aionemu.gameserver.questEngine.**.*Test'` | **1440 run / 0 failures / 0 errors / 1 skipped** |

计数普查脚本 `audit_counters.py`、`census_counters.py`（本目录）为只读审计工具，
用于对比 `quest_monster.csv` 的 `SECTION_1<N` 与 XML `set-variable var1` 目标值。

## 未验证边界

- 未做真机/真客户端验收；本次 XML 修改仅经静态编译（生产目录编译门禁）与结构门禁验证。
- 13758–13769 家族的客户端 `SECTION_1<5` 与旧 handler 一致，但真端 UI 的击杀计数显示仍需实机确认。
- 本次未运行服务端进程，也未做全仓库（非 questEngine）测试。

---

## 追加（第 1/2/4 项优化）：击杀数门禁、声明收口与批量改写守卫

### 1) 击杀数真端门禁：`QuestKillCounterRetailGateTest` + planner 模拟器

不再从 XML 形状反推击杀数，而是用 `QuestKillCounterSimulator` 走**真实 `QuestMutationPlanner`** 连续模拟击杀，
得到"引擎口径下完成所需击杀数"，再与客户端 `SECTION_1<N` 门控比对：

- `src/test/resources/quest/quest-kill-counter-retail-contract.tsv`：414 个单计数器任务的客户端门控快照
  （source：`Quest_unpacked/quest_monster.csv`）。
- `src/test/resources/quest/quest-kill-counter-overkill-pending.tsv`：**34 个"引擎要求 N+1 只"的待裁任务**
  （13955/23955、35052、35058-35065、36532-36536、45052/45058/45060-45064、46531-46548）。
  证据：同形态 514 个任务均为"正好 N 杀完成"（`below N-1` 累加 + `at-least N-1` 收口），
  且客户端只给 N。门禁强制该账本**精确等于**模拟结果，修复后必须同步清空。
- 反向对照 `simulatorReproducesTheFixedOverkillDrift`：把 13765 还原为漂移形态后模拟器报 6 杀（原 5），证明门禁非空转。

### 2) `<kills>` 声明收口

`QuestKillCounterRetailGateTest.killDeclarationsStayInsideKillTransitions`：声明中的 npc 必须真实出现在击杀转换里，
声明仅作展示、计数一律以 `var1` 计数器为准（`QuestMetadata.kills()` 在生产代码无消费点，100/1302 的任务声明它）。

### 4) 批量改写守卫：`check_quest_xml_block_attributes.py`

`.agents/summary/quest-counter-audit/check_quest_xml_block_attributes.py`：对比两个 revision 上被改动任务 XML 的
**块级属性集合**，任何"基线有、新版本丢"的属性都会失败退出（可用 `--allow path:tag:attr` 显式留证）。
自检：`--base 3b4e7fc4c^ --head 3b4e7fc4c` 精确报出 13841/13845/13849 的 `transitions:reported-reward-mode` 丢失。

### 验证

| 范围 | 命令 | 结果 |
|---|---|---|
| 新门禁（隔离 worktree，含工作区新文件） | `mvn -o test -Dtest='QuestKillCounterRetailGateTest'` | 5/5 通过 |
| 守卫脚本历史自检 | `check_quest_xml_block_attributes.py --base 3b4e7fc4c^ --head 3b4e7fc4c` | 精确报出 3 处属性丢失 |

---

## 追加（b→a）：34 个“引擎多要一只”任务的处理

### b) 真端判定（静态证据链已成立）

1. **同族 canonical 对照**（最直接）：45058/45060-45064（待裁）与 **45059/45065（canonical）同族、同报告 NPC 804931、同客户端门控 10**：
   - 45059：`below 9` → `at-least 9` → `set var1=10`，XML 注释“第 **10** 次击杀达成”；
   - 45058：`below 10` → `at-least 10` → `set var1=11`，XML 注释“第 **11** 次击杀达成”。
2. **客户端数据**：`quest_monster.csv` 只给 `SECTION_1<10`；`data_driven_quest.xml` 的 `value0_progress_` 同为 10。
3. **计数口径**：var1 位于 quest_vars 的 6..11 位（= 客户端 SECTION_1），无偏移；据此在 10 杀时客户端门控 `<10` 已为假（客户端判定该狩猎步骤完成），而服务端仍要求第 11 次击杀 → 玩家“多杀一只”。
4. 门禁模拟器（真实 planner）与上述一致：全库仅这 34 个 `gate+1`，其余 514 个同形态任务都是正好 N 杀。

结论：这 34 个是**服务端计数漂移**，不是客户端计数偏移。仍保留 `.agents/summary/quest-acceptance/2026-09-18-kill-counter-and-repeat-dialog-pending-client.md` 的实机点验步骤作为最终确认。

### a) 批量修正（34 个任务）

按同族 canonical 形态逐条对齐（脚本带严格形态断言，遇到未预期 var1 引用即失败）：

| 项 | 修正前 | 修正后 |
|---|---|---|
| bit-field `var1` max | N+1 | N |
| 累加门槛 `variable-below` | N | N-1 |
| 收口门槛 `variable-at-least` | N | N-1 |
| 收口 `set-variable var1` | N+1 | N |
| reward 节点投影 `var1` | N+1 | N |
| 满计数恢复路线 `variable-at-least`（2 条/NPC） | N+1 | N |
| 注释 | “击杀 0..N-1”“第 N+1 次击杀达成” | “击杀 0..N-2”“第 N 次击杀达成” |

任务清单：13955、23955、35052、35058-35065、36532-36536、45052、45058、45060-45064、46531、46535、46536、46539-46548。

### 验证（隔离 worktree，工作区改动叠加在 `f3d80479d` 之上）

| 范围 | 结果 |
|---|---|
| `QuestKillCounterRetailGateTest` | 4/4 通过 → 414 个单计数器任务**全部**正好等于客户端门控 |
| `com.aionemu.gameserver.questEngine.**.*Test` | 1444 run / 0 failures / 0 errors / 1 skipped |

门禁的待裁账本 `quest-kill-counter-overkill-pending.tsv` 已清空删除，门禁改为无条件断言“完成所需击杀数 == 客户端门控”。
