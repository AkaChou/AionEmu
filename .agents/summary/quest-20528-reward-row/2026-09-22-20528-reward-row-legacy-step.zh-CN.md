# 任务 20528/10528「构筑保护之实体 1」领奖投影回归 legacy 落盘 step（reward=11，不是任务书末行 12）

- 日期：2026-09-22
- 范围：`quests/20528.xml`（用户报障，魔族）、`quests/10528.xml`（天族镜像，必须同步以免两侧再分叉）、
  聚焦门禁 `src/test/java/com/aionemu/gameserver/questEngine/definition/ArchdaevaRewardRowContractTest.java`、
  审计例外登记 `.agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py`
- 状态：**实现完成 + 静态验证通过（XML 良构、全库审计脚本重跑）；聚焦 Maven 测试与生产目录/白名单门禁未执行
  （无构建授权，PENDING）；修复后客户端实机复测 PENDING_CLIENT**
- 结论：`reward` 节点投影 `var0 = 11`（= 最后一个 START 节点 s11 = legacy 落盘 step），
  交接 transition 不回写该字段，旧存档 `REWARD/var0=12` 由无 source 的 `enter-world` 自愈边改回 11。

## 一、用户口径（数值权威）

用户在本轮连续三条消息里收敛到同一口径（前两条被后一条显式推翻）：

1. 「任务要求发动了 2 次实体，start 10 阶段的应该是 11，reward 是 11」
2. 「不对，确实是发动 2 次，然后奖励是 reward 13 不是 11」← 已被下一条推翻
3. **「不对，确实是发动 2 次，start 最后是 11，然后奖励是 reward 11」**（最终口径）

据此：**发动实体 2 次（节点 s9 第一次、s10 第二次）→ 阶梯止于 s11 → 领奖态投影也是 11**。
本文所有静态证据都是对这条客户端口径的交叉验证，不替代用户口径。

> 证据分层：本条是用户给出的**期望值口径**；修复后的**实机复测尚未进行**，
> 因此不能把本任务标成 `CLIENT_ACCEPTED`（见 §七、§八）。

## 二、客户端证据（Aion 5.8）

`Dialogs/20000_29999/quest_q20528.html` 的 `quest_summary` 共 13 行（0..12，天族
`quest_q10528.html` 同形），末段如下：

| 行 | 文案 | 对应 XML 节点 |
| --- | --- | --- |
| 8 | 使用召唤道具、威扎波波醒来后和他对话 | `s8`（806297 `STEP_TO_9`） |
| 9 | 发动（实体） | `s9`（731715 第一次发动） |
| 10 | 发动（实体） | `s10`（731715 第二次发动，消耗 182216089 + 影片 877） |
| 11 | 使用召唤道具、威扎波波醒来后和他对话 | `s11`（806297 `SET_SUCCEED` 交付 182216088） |
| 12 | 和代理人佩莱格兰对话 | **无独立状态**（末行，只是第 0 行「去接任务」那句话的复述） |

客户端 step 声明的旁证（`Quest_unpacked/quest_script_monster.csv:708-711`、`quest_monster.csv:5108`、
客户端 `quest.xml` 的 `collect_progress=5`）只钉住 step 3（感应区 a）、4（击杀计数，`SECTION_0==4`）、
5（宝箱掉落）、7（感应区 b），**没有声明领奖 step**——所以领奖行只能由 legacy 落盘值 + 用户口径决定。

## 三、legacy 证据（落盘 step = 11）

`origin/history:.../archdaeva/_20528Building_A_Protection_Artifact_1.java`（天族镜像
`_10528Protection_Artifact_1.java` 同形）：

```java
case SET_REWARD: {
    removeQuestItem(env, 182216088, 1);   // 天族：182216076
    qs.setStatus(QuestStatus.REWARD);
    updateQuestStatus(env);
    changeQuestStep(env, 11, 12, true);   // ← reward=true
    return closeDialogWindow(env);
}
```

`QuestHandler#changeQuestStep(..., varNum)` 的语义（`origin/history` 原文）：

```java
if (qs.getQuestVarById(varNum) == step) {
    if (reward) {
        qs.setStatus(QuestStatus.REWARD);          // 只置状态，不写 nextStep
    } else if (nextStep != step) {
        qs.setQuestVarById(varNum, nextStep);
    }
    ...
}
```

即 `to=12` 在 reward 分支里**从不写盘**，进入 REWARD 时 packed step 仍是 11；两族的 `to` 都只是作者
书写的占位，真正的落盘值是 `from=11`。这与用户口径「start 最后是 11，奖励是 11」逐字一致，
也与本仓库既有先例 15300/25300（`changeQuestStep(env, 13, 14, true)` → reward=13，真机已验收）同形。

## 四、与「领奖行=任务书末行」口径的冲突（必须登记，不能静默）

批次 1（2026-09-21）用 QE-051 的行号口径把两侧都推到末行 12，其依据是：

- 客户端 `quest_summary` 13 行（0..12），末行 12 是「和代理人 X 对话」；
- 审计脚本判据 `last_row_npc_matches_quest = True`——末行点名的 NPC（佩莱格兰/维达）确实在任务的
  reward 路线（`npc-complete npc-id="806079"` / `"806075"`）里；
- 同族 10527 的实机验收值 15 恰恰是末行。

反向证据（本次采纳的原因）：

- 用户实机口径直接给出 11（两次，且明确推翻了 13 与 12）；
- 末行 12 与第 0 行同文（`normalize(rows[0]) == normalize(rows[-1])`），正是 QE-051
  `fix_or_guardrail` 第 1 条写明的例外情形：「最后一行是第 0 行重复行……按客户端验收值记例外」；
- legacy 落盘 step 就是 11，迁移出来的 10528 原本也是 11（批次 1 才改成 12）。

因此本任务登记为 **QE-054 的 legacy 落盘 step 例外**（`LEGACY_STEP_EXCEPTION`），
而不是继续套用行号口径。**这不是通用规则**：10527 的 15 同样是客户端验收值，两者并存，
见 `docs/quest/repair-playbook` 之外的族内清单 §六。

## 五、修复内容

`quests/20528.xml` 与 `quests/10528.xml`（两侧同形）：

1. `reward` 节点投影 `var0`：`12 -> 11`。
2. `s11 -> reward` 交接：删除 `set-variable var0=12`（目标投影权威，重写值本身就是错位来源）；
   保留 `remove-item 182216088`（天族 `182216076`）、条件 `variable-is var0=11`（20528）、
   `after-commit` 顺序 `LEVEL_AND_VISIBILITY_REFRESH` + `close-dialog` 不变。
3. 无 source `enter-world` 自愈边（两侧同形，10528 为方向反转）：
   `status=REWARD && var0==12 -> set var0=11`，`after-commit = LEVEL_AND_VISIBILITY_REFRESH`。
   没有这条边，批次 1 落盘的 REWARD/12 存档匹配不到 `reward` 节点的任何路由
   （`QuestMutationPlanner#matchesSourceNode` 要求源投影逐字段全等），玩家点不出领奖对话。
4. `progress/bit-field` 的 `var0 max` **必须保留 12**：`ProgressLayout#pack` 对存档值做范围校验
   （越界抛 `value out of range for progress field: var0`），下调到 11 会让旧存档在自愈边生效前先炸。
   两侧都加了双语注释说明这一点（QE-047 的 pack 约束）。
5. 文件头/合同注释同步改写（原文写的是「交付后应指向末行 12」，与本次口径相反）。

## 六、同族审计（末行 = 第 0 行重复行族）

只读扫描脚本 `.agents/summary/quest-20528-reward-row/scan_duplicate_row0_family.py`
（输出 `duplicate-row0-family.tsv`、运行日志 `duplicate-row0-family-run.txt`）：

- 家族规模 109 个任务（客户端 `quest_summary` 末行与第 0 行同文，去标签/去字典符/去空白后相等）；
- 其中 **36 个**处在「两种口径只差 1」的歧义带（`reward = 末行索引 = 最后 START + 1`），
  本任务的镜像族成员是 `10525/20525`、`10526/20526`、`10527/20527`、`10528/20528`、`10529/20529`；
- 已有 13 个任务用 legacy 落盘口径（`reward == 最后 START`，本次加入 10528/20528）：
  `10528, 11147, 11228, 13702, 14013, 14041, 15300, 20528, 24011, 24041, 24120, 24151, 25300`。

处置边界（**严禁批量按任一方向改**）：

- 本批只动有**用户口径 + legacy 逐字证据**的 10528/20528；
- `10527/20527` 保持 15（2026-09-22 用户实机确认 + `REWARD/var0=14 -> 15` 自愈边）；
- `10525/20525`（7）、`10526/20526`（12）、`10529/20529`（11）以及上表其它 26 个歧义带任务
  **仍未实机复核**：每个任务按 QE-051 例外条款用「legacy 落盘 step + 客户端末行是否第 0 行复述」
  两个探针单独定值，改前先跑本扫描脚本与 `audit_reward_row_vs_client_steps.py`。

## 七、验证记录（本次实际执行）

| 层 | 结果 |
| --- | --- |
| XML 良构 | `10528.xml`、`20528.xml` 解析通过；`reward` 投影=11、自愈边条件/动作与预期一致 |
| 全库审计脚本 | `python3 .agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py` 运行成功（exit 0，日志 `audit-full-run-2026-09-22.txt`）；两个任务由 `ROW_ALIGNED / ROW_STATE_ALIGNED` 变为 `ROW_BEHIND / ROW_WITHOUT_STATE(rows_without_state=12)`，并出现在「已登记 QE-054 legacy 落盘 step 例外」清单里；10528/20528 之外没有别的任务因本批改变判定（工作区还包含其它并行会话的任务改动，全库数字仅作参考：MISSING_LAST_ROW=80、ROW_BEHIND=134） |
| 家族扫描 | `scan_duplicate_row0_family.py` 运行成功（family size = 109） |
| 聚焦 Maven 测试 | **未执行（未获构建授权）**：`mvn -q -Dtest=ArchdaevaRewardRowContractTest,QuestCollectProgressAlignmentGateTest,Quest10520ClientDialogAlignmentTest test` |
| 生产目录/白名单门禁 | **未执行（未获构建授权）**：`QuestDefinitionCatalogManifestTest`、`ProductionCatalogWhitelistVerificationTest` |
| 客户端实机 | **PENDING_CLIENT**：需要在修复后的构建上复测交付与领奖两步 |

说明：仓库里已提交的 `audit-output.tsv` / `audit-missing-last-row.tsv` / `audit-qe051-candidates.tsv`
仍是 2026-09-21 的全库快照（本批不改这三份快照，避免把并行会话的其它任务改动混进它们）；
本批的运行结果保存在本目录 `audit-full-run-2026-09-22.txt`，两个任务已按 §七 进入已登记例外清单。

## 八、残余风险与实机复测要点

1. 复测点 A：第一次发动（731715）后任务书应停在行 10「发动」；第二次发动后应停到行 11
   「使用召唤道具、威扎波波醒来后和他对话」（`SECTION_0=11`，与 legacy 一致）。
2. 复测点 B：与 806297（天族 806292）交付后进入 REWARD，任务书**应保持在第 11 行**并显示领奖态，
   随后到 806079（天族 806075）领取奖励可正常弹出奖励窗。
3. 复测点 C（旧存档）：已经落盘过 `REWARD/var0=12` 的角色重新进入世界后应被自愈边改回 11，
   领奖对话仍可点出（当前先在线的角色需要一次重新进入世界）。
4. 若实机显示的是**空白行**或仍停在第 10 行，说明本任务实际属于行号口径（末行 12），
   则按 §四 的反证据把两侧改回 12 并反转自愈边即可（改动点集中在 §五 的 1/2/3 三条）。

## 九、本批文件清单

- `src/main/resources/aion/data/static_data/quest_definition/quests/20528.xml`
- `src/main/resources/aion/data/static_data/quest_definition/quests/10528.xml`
- `src/test/java/com/aionemu/gameserver/questEngine/definition/ArchdaevaRewardRowContractTest.java`
- `.agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py`（批次 53 例外登记）
- `.agents/summary/quest-10527-reward-row/2026-09-21-10527-reward-row-and-family-audit.zh-CN.md`（§五十八 勘误）
- 本目录：`scan_duplicate_row0_family.py`、`duplicate-row0-family.tsv`、`duplicate-row0-family-run.txt`、
  `audit-full-run-2026-09-22.txt`、本报告
