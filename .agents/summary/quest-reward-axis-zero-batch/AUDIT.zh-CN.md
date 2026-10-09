# 领奖态轴归 0 批次——10522 同族全库排查审计

- 日期：2026-10-09
- 触发：10522/20522 收口（ffa0232f3）后用户指令「排查类似问题」
- 判据（10522 GM A/B 定谳的推广）：任务从接取到 REWARD 的路径上**没有任何 var0 写入面**
  （无 `<set-variable>`、无 `<increment-variable>`、无引擎外写入方、不在中继表车道）⇒ 轴恒为
  接取值 0 ⇒ reward 投影必须 = 0；批次「末行索引抬行」值让客户端任务书 0 基行匹配落空
  （REWARD 态步骤空白，静默缺陷——领奖路由按 status 匹配不挡流程）。
- 状态：**95 个 XML 收口（62 纯归 0 + 33 双路径投影归 0 + 自愈边反转）+ 1311 合同重锚；
  门禁 25/25 全绿；待实机抽验**

## 追加批次：双路径型 33 个（统一判据重扫发现）

初版 62 个修完后，按「全文任意 var0 写入动作（set-variable/increment-variable）+ 写入所在边」
统一判据重扫 C105，暴露**双路径型** 33 个（初版分诊把「setvar 在 ENTER 自愈边上」误并入了
started→reward 边判据的漏网）：

1322 1430 1464 15690 1647 19002 2232 2493 2513 25690 29000 29002 30210 30310 30503 30553
3057 4940 4941 80255 80256 80257 80258 80259 80260 80261 80262 80263 80264 80265 80266
80291 80295

- 结构：普通 `started→reward` 边（新流程，轴保持接取值 0）+ batch8 的 `ENTER→reward` 自愈边
  （REWARD+var0=0 → set 1——把正确的存量档改坏成 1，与 10522 原型完全同病同方向）。
- 修复（`apply_dualpath_axis_fix.py`）：reward 投影 1→0 + 自愈边反转（条件 var0=0→1、
  动作 set 1→0，捕获污染档 REWARD/var0=1 落盘归 0）。
- 自洽排除：started→started 推进 8 个（18302/18510/24114/28510/30011/30203/30237/30303/30350/
  30701 中除阶梯 2 个外）与阶梯 2 个（18302 五杀=5、30701=1）投影=末写值，不动。

## 分诊漏斗（733 个定义全量）

| 层 | 判据 | 剩余 |
|---|---|---|
| 形状 | 无玩法中间节点（仅 unaccepted/started/reward/complete）且 reward≠0 | 155 |
| 边面 | started→reward 边不带 set-variable（41 个带 = 两段对话推进，自洽排除） | 114 |
| 推进面 | 全文无 `<set-variable field="var0">` 且无已知引擎外写入方（8 个基线对齐排除） | 73 |
| 阶梯面 | 全文无 `<increment-variable field="var0">`（18302 五杀阶梯/30701 剔除——**初版扫描漏 increment 形态，18302 曾被误改后已回滚**） | 71 |
| 中继面 | 四张真端表（SimpleTalk/SimpleCollectItem/SimpleItemPlay/SimpleUseItem）无行（运行时 SETPRO 不认领） | 62 |
| enter-world 型 | 7 个（1472/1604/2443/2484/30308/4077/4712）唯一 reward 边为 enter-world 且带 setvar=1，自洽排除 | 62 |

**最终 62 个**（62 个投影 1、1311=3、18302/30701 已剔除）：
11226 11229 1311 1393 19079 19080 19081 21030 2106 2372 28511 28828 29079 29080 29081
30231 30235 30236 30238 30239 30240 30241 30242 30243 30247 30248 30249 30250 3032 30331
30335 30336 30337 30338 30339 30340 30341 30342 30343 30347 30348 30349 3712 3941 3944
3947 3950 3953 3956 45045 45046 45047 45048 4907 4945 4948 4951 4954 4957 4960 51010 51020

## 修复

- 62 个 XML：`<node label="reward">` 内 var0 → 0（脚本 `apply_reward_axis_zero.py`，幂等）。
  **无需自愈边**：这些任务从无写入方碰轴，存量存档恒 0，投影归 0 后自动对齐。
- `Quest1311ClientDialogAlignmentTest`：reward 断言 var0 3→0 重锚（批次值锁定，无真端依据）。
- 测试锁定面核对：64 名单全量扫测试引用，18302（Aturam 注释/阶梯）、LegacyKillFlow
  （仅断言边存在）、MissionItemConsumption（仅扣道具边）、ItemPlayFamilyRowInventoryGate
  （28828=ADJUDICATED 留 XML 无中继）、QuestAiDialogBindingGate（21030 冻结清单）均不锁
  reward 投影；EarlyElyosQuestRegression 1311 断言只物件门。

## 门禁（IDEA MCP，2026-10-09 授权）

- QuestProductionStartupGateTest 2/2
- Quest1311ClientDialogAlignmentTest 2/2（重锚后）
- ExternalRewardAdvanceReentryContractTest 2/2
- EarlyElyosQuestRegressionTest 19/19

## 方向修正（2026-10-09 实测推翻双路径批次）

80255 用户 A/B 实测（//reload quest 后）：
- `//quest set 80255 reward 0` → 步骤显示**行 0（使用烟花）**＝接取行，**错行**；
- `//quest set 80255 reward 1` → 显示**「和帕尔图对话」＝领奖行，正确**。

⇒ **33 个双路径任务的「投影 0」修复方向错误，已全部回滚到 batch8 形态**
（git checkout 07f69dc0e~1，投影 1 + 原 ENTER 自愈边），//reload quest 实机验收无误。

**模型修正**：客户端任务书行匹配存在**任务级差异**，不能从单一任务 A/B 推广全库——
- 10522（62 组口径）：REWARD/0 用户验收正确、REWARD/1 报空白；其行 2 带
  `[%dic:STR_DIC_N_LF6_Weatha_E]` 字典占位符；
- 80255（33 组口径）：REWARD/1 正确；其行 2 是纯文本（无 dic 键）。
两组唯一结构差异即行 2 是否带 `[%dic:]` 键，疑似渲染层差异，**语义未定谳**。
后续观察名单：62 组中 REWARD 态显示「进行行而非领奖行」的任务，按「行 2 是否带
字典键」细分后再动。

**遗留**：帕尔图（831163）全库无静态生成配置——真端 npc 表标注
`__spawn_zonename__=Housing_LF_Personal`（天族住宅区庆典活动 NPC），本库 spawns 无行；
测试用 `//spawn 831163` 临时生成。正式落活动生成配置另立任务。

## 方法论教训

1. **写轴动作形态全集**必须穷举：`set-variable`、`increment-variable`、引擎外写入方、
   中继表 SETPRO、enter-world 边——初版漏 increment（18302 五杀阶梯误判，幸被 Aturam
   合同注释「计数阶梯」反证纠正）。
2. 目录门禁（QuestProductionStartupGate）从 classpath 读 XML 且全目录并发编译：**并行任务
   的工作区中间态会污染门禁结果**（10529 AMBIGUOUS_TRANSITION 为并行任务
   quest-10529-counter-residue-marker 当时未完成的边，其后自愈）——红时先 `git stash`
   验基线再归因。
