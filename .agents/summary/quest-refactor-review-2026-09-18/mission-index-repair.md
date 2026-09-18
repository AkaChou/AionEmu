# 14026 / 24026 奖励索引修复

状态：代码已修改，验证 PENDING。未运行 Maven、IDE build、Java 测试或服务进程；由主线程串行统一验证。

## 改动范围

- `src/main/resources/aion/data/static_data/quest_definition/quests/14026.xml`
- `src/main/resources/aion/data/static_data/quest_definition/quests/24026.xml`
- `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMissionRewardIndexRegressionTest.java`
- 本证据文档。本线程未执行 git add/commit 或撤销其他人的改动。最终 status 显示新测试为 `A `（已暂存），两 XML 为工作区修改，本文档未跟踪；保留共享索引状态，不自行反向调整。

两个 XML 各六条 `reward -> complete` 路由的 `complete-quest reward-index="2..7"` 全部改为 `0`。其余内容不变，包括两档 metadata、称号、六选一装备、固定奖励、NPC、事件、节点与 after-commit 顺序。

## 证据与结论

1. 当前 metadata 每项只有两个有序奖励组，有效零基组索引是 `0/1`。`2..7` 实为第一组中六件 `SELECTABLE_ITEM` 的位置，不能作为完成奖励组索引。
2. `911440146:src/main/java/quest/mission/_14026A_Lone_Defense.java` 和 `_24026A_Hand_From_Each_Side.java` 的 REWARD 分支分别由 NPC `203901/204301` 负责：START_DIALOG 展示 `2375`，SELECT_REWARD 展示 `5`，其他动作调用默认 `sendQuestEndDialog(env)`。
3. 同版本 `src/main/java/com/aionemu/gameserver/questEngine/handlers/QuestHandler.java:191-220`：默认重载传 `reward=0`；动作 `8..23` 调用 `QuestService.finishQuest(env, reward)`，`reward` 明确是 `List<Reward>` 的组索引。
4. 同版本 `src/main/resources/aion/game/data/static_data/quest_data/quest_data.xml:28709`、`:39045` 各只有一个奖励组，与当前第一组一致：EXP `3504765`，称号 `12/60`，各六件可选装备；14026 固定物品 `188053407*3,186000003*40,190200000*10`，24026 固定物品 `186000008*20,190200000*10`。
5. Aion 5.8 客户端仓库映射：`docs/quest/client-dialog-mapping/quest-dialog-action-details.csv:4917`、`:14819` 都是 `select5(2375) -> HACTION_SELECT_QUEST_REWARD(1009)`；`client-html-pages.csv:7` 确认 page `5` 为第一档奖励窗口；`client-hyperlinks.csv:10-15` 确认六个选择动作是 `8..13`，不是奖励组编号。
6. 当前每条显式奖励都包含 `190200000*10`，仅当前第一组有该物品，第二组没有。结合旧 handler 与 page 5，实际组必须为 `0`，不能因为 metadata 有两档就将部分装备改成组 `1`。
7. 历史审计 `.agents/summary/quest-refactor-review-2026-09-18/head-results.json` 已记录这十二条路由的 `configured=[] granted=[TitleGrant[...]]`。这是组索引越界引发的称号合同错配，不是应删除称号的证据。

## Pattern 对照与回归

已读规则、CodeGraph、Playbook、QE-026/QE-028、代表提交 `68d5786` 的 diff 及 `Quest18602ClientDialogAlignmentTest`。匹配点：REWARD owner、组内可选装备、固定奖励及完成顺序；区别：本任务已用 durable `ITEM` 显式发奖，无需改 planner 或改成 `npc-complete`，只修正 `CompleteQuest` 组索引。复用奖励组/选择项分离及预览档位一致性，不增加新 Pattern。

新增参数化测试覆盖两个任务：

- 精确比对两档 metadata 的全部奖励、数量及顺序，保留称号和第二档声明。
- 唯一 REWARD 节点及 `reward/complete` 的状态和 `var0=4`。
- 同 NPC 的 `QUEST_SELECT -> SELECT5`、`USE_OBJECT/SELECT_QUEST_REWARD -> WINDOW1`，无事务动作或条件。
- 恰好六条完成路由，六个选择动作与六件装备逐项对应；每条完整 actions 为第一档公共奖励、一个装备、`CompleteQuest(0)`。
- 完整 after-commit 顺序：刷新属性、COMPLETION 同步、选择页 `10`。
- 每项只有九条 reward 源路由，不增加未经证实的第二档剧情或额外领奖入口。

## 检查与未决

- 已执行两 XML 的 scoped `git diff --check`，通过；diff 仅十二处数字替换。
- 新测试执行 `git diff --no-index --check /dev/null <test-path>` 无空白诊断；新增文件差异的退出码为 1，不代表运行了 Java 测试。
- 未执行编译、测试、完整 catalog 或客户端验收，不能宣称已通过 IR/运行时验收。
- 第二档仍没有有证据支持的交付路径。客户端 14026 另有 `select9`，但同样使用动作 `1009`；仅页面存在不能证明第二档的 owner/状态条件。保持 metadata，不造剧情，也不删除称号来绕过门禁。
- 静态检查 `QuestMovieAndDialogLoopRegressionTest#multiTierQuestsNeverDeclareDeadRewardGroups`：修复后 covered 组索引为 `{0}`，组 `1` 仍会被该门禁报告；这是保留的未决项，不改共享门禁。待主线程结合第二档的来源和真实触发条件另行裁决。

主线程待执行命令（本线程未执行，按顺序运行）：

```bash
rtk mvn -q -Dtest=QuestMissionRewardIndexRegressionTest test
rtk mvn -q -Dtest=QuestTitleRewardCoverageTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test
rtk mvn -q -Dtest='QuestMovieAndDialogLoopRegressionTest#multiTierQuestsNeverDeclareDeadRewardGroups' test
```

最后一项预期仍暴露上述第二档问题；全库门禁也可能包含并行任务尚未收口的独立失败，不应归因于本补丁或借机修改其他文件。
