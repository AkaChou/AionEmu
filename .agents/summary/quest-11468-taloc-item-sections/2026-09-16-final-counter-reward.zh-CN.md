# 任务 11468/21468 三维计数最后一击进入 REWARD

## 触发与症状

- 实机 GM 信息：任务 `11468` 为 `Status: START`，`Vars: 0 10 5 3 0 0`，`Complete count: 0`。
- 三种任务物品计数已经全部达到 `10/5/3`，但状态仍为 `START`，客户端没有自动进入领奖阶段。

## 权威证据

1. `origin/history` 的 `_11468WithFriendsLikeThese` / `_21468TheStruggleWithin`：
   `onUseSkillEvent` 在每次计数后调用 `reward(qs, env)`；当 `var1==10 && var2==5 && var3==3`
   时立即 `qs.setStatus(QuestStatus.REWARD)`，再 `updateQuestStatus(env)`。
2. 当前 XML 只有三条 `started -> started` 的 `use-skill` 自环，没有“最后一个计数事件进入 REWARD”的优先级 0 路线。
3. 现有 Playbook Pattern：`COUNTER_SOURCE_PROJECTION_NO_LOCK`，代表提交 `4a3be57`，
   测试 `Quest26802ClientDialogAlignmentTest#finalKillInEitherCounterEntersRewardBeforeReporting`。
   - 匹配：最终计数事件必须优先进入 `REWARD`，并提交 `LEVEL_AND_VISIBILITY_REFRESH`；
     已持久化满计数的存档保留带门禁的 `SELECT_QUEST_REWARD` 恢复路线。
   - 差异：本任务是 `UseSkill(9832/9833/9834)` 三维计数，不是 `KillNpcSet`；`started`
     节点已经正确省略实时计数投影，不需要再修 source projection；没有 `var0` 完成标志。

## 修改

- `11468.xml`、`21468.xml`：每个技能增加两条互斥优先级的 `use-skill` 路线：
  - `priority=0`：当前字段等于 `required-1` 且另外两字段已经满足阈值时，执行最后一次
    `increment-variable`，目标 `reward`，after-commit 为 `LEVEL_AND_VISIBILITY_REFRESH`；
  - `priority=1`：当前字段仍低于 `required` 时留在 `started`，after-commit 为 `PACKET_ONLY`。
- 将 `SELECT_QUEST_REWARD` 的 `started -> reward` 恢复路线加上 `var1>=10 && var2>=5 && var3>=3`
  门禁，保留已经持久化满计数存档的对话恢复路径，同时阻止提前跳奖励。
- 新增 `Quest11468And21468SkillCompletionTest`：锁定六条计数路线的事务动作、条件、priority、
  after-commit，以及最后使用三种技能分别进入 `REWARD` 的 runtime planner 结果。

## 验证与验收

- 2026-09-16 用户确认 11468 与 21468 均正常完成，客户端验证成功；最终计数、
  `REWARD`、报告领奖与 `COMPLETE` 已闭环。
- 已执行：`xmllint --noout --schema .../quest_definition.xsd` 对两个 XML 均通过；IDE 对 XML
  与新增测试无 error；静态 transition dump 与合同一致；`git diff --check` 通过。
- 未执行：Maven 专项测试、生产 catalog/白名单门禁。
- 待授权命令：
  `mvn -q -Dtest='Quest11468And21468SkillCompletionTest,ClientQuestSectionAlignmentTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest' test`
- 现有满计数存档可在新代码加载后与 `799503` 对话并点击报告，由带门禁的
  `SELECT_QUEST_REWARD` 恢复路线进入 `REWARD`；新产生的最后一件物品使用会自动进入 `REWARD`。
