# 任务 15300/25300 领奖阶段任务书空白修复

- 日期：2026-09-19
- 范围：`15300.xml`、`25300.xml`，以及龙脊深渊两个副本处理器的门延迟任务收口。
- 状态：**CLIENT_ACCEPTED**（2026-09-19/20 用户实机确认 15300 全程顺利完成、奖励可领取）；Maven 聚焦与生产目录门禁已于 2026-09-20 授权执行并全绿。

## 现象与运行期证据

- `2026-09-19 23:44:21` 与 805362 对话后：
  - `CM_DIALOG_SELECT ... 上一页=7523 动作=10013`
  - `SM_QUEST_ACTION 任务=15300 状态=4 步数=14`
  - `SM_DIALOG_WINDOW 玩家=Ww targetObj=0 questId=0 下发页=0`
- 玩家反馈此时任务信息消失，背包已有 `182215903`，无法继续找 805327 领奖。
- `2026-09-19 23:38:52` 的延迟任务 NPE 为同类收口缺口：
  `doors.get(267).setOpen(true)` 在副本销毁后执行，门映射已清空或缺失。

## 根因

- 旧 handler `_15300Taking_Arms` 的领奖交接为：
  `changeQuestStep(env, 13, 14, true)`。
- 旧 `QuestHandler.changeQuestStep(..., reward=true)` 只设置 `QuestStatus.REWARD`，不会写入 `nextStep`；
  因此旧存档的 REWARD packed step 仍是 `var0=13`。
- 迁移 XML 却把 `reward` 节点投影和交接动作都写成 `var0=14`，客户端按错位 step 解释 REWARD 任务，
  表现为任务书空白、下一 NPC 不显示。这是既有模式 `LEGACY_REWARD_STEP_PROJECTION_MISMATCH` 的同型实例。
- 魔族镜像 `25300` 的旧 handler 同样调用 `changeQuestStep(env, 13, 14, true)`，当前 XML 存在相同错位。

## 修复

- `15300.xml`、`25300.xml`：
  - `reward` 节点投影改为 `var0=13`；
  - 删除 `s13 -> reward` 交接中的冗余 `set-variable var0=14`，保留任务物品发放；
  - 增加无 source 的 `enter-world` 恢复迁移：
    `status=REWARD && var0=14 -> reward`，只执行 `LEVEL_AND_VISIBILITY_REFRESH`，
    用于纠正已经落盘的错误状态。
- `DrakenspireDepthsQInstance`、`DrakenspireDepthsInstance`：
  - 所有延迟开门统一走 `openDoor(int)`；
  - 副本销毁或门映射缺失时直接返回，单个门不存在时跳过；
  - `onInstanceDestroy` 对门映射判空后再 `clear()`。
- 新增/扩展测试：
  - `Quest15300And25300RewardProjectionTest`：锁定 reward 投影、交接动作、恢复迁移和客户端任务状态规划；
  - `DrakenspireDepthsInstanceTeardownGuardTest`：锁定两个处理器的延迟开门不再直接解引用门映射。

## 已执行验证

- `xmllint --noout`：`15300.xml`、`25300.xml` 通过。
- `git diff --check`：通过。
- Python 结构审计：两个任务均为 `reward var0=13`、交接不再写 `var0=14`、存在 `REWARD var0=14 -> reward` 恢复边。
- IDEA inspections：本次新增/修改文件无编译错误；其余警告为两个大型历史处理器既有告警。

## 已执行验证（2026-09-20 授权）

```bash
mvn -B test -Dtest='Quest15300And25300RewardProjectionTest,QuestInstanceExitRecoveryTest,DrakenspireDepthsInstanceTeardownGuardTest,DrakenspireDepthsQOrissanSceneTest,DrakenspireDepthsQTwinSceneTest,DrakenspireDepthsQTwinSpawnSurfaceTest,ImmortalOrissanAI2Test,ThresholdTransformDeathFallbackGateTest,Betrayer_IcaronixAI2Test'
mvn -B test -Dtest='ProductionCatalogWhitelistVerificationTest,QuestDefinitionDirectoryLoaderTest,QuestDefinitionCatalogManifestTest'
```

- 第一批：`Tests run: 22, Failures: 0, Errors: 0` → BUILD SUCCESS（本机日志，被 .gitignore 排除、未入库：`2026-09-20-mvn-focused-tests.log`）。
- 第二批：`Tests run: 13, Failures: 0, Errors: 0` → BUILD SUCCESS（本机日志，被 .gitignore 排除、未入库：`2026-09-20-mvn-catalog-tests.log`）。
- 首轮执行时 `QuestInstanceExitRecoveryTest` 因新增的无 source 恢复边触发测试端非空假设（`transition.sourceNode().equals(...)` NPE），已改为 `Objects.equals(...)`；这是测试端假设问题，不是任务数据回退。

## 客户端复测路径

1. 停服、重新构建并重启，确保新 XML 和目标类目录同批次。
2. 登录 Ww，`REWARD var0=14` 的旧存档会由 `ENTER_WORLD` 恢复边纠正为 `var0=13` 并下发状态包。
3. 与赛格尼亚的 805327 对话：
   - 应先下发 `DEFAULT_SUCCESS(10002)`；
   - 点击报告按钮发送 `SELECT_QUEST_REWARD(1009)`；
   - 应打开奖励窗口 page 5；
   - 选择奖励后完成并进入 `COMPLETE`。
4. 魔族角色按 25300/805339 的镜像路径复测。
5. 副本销毁后确认不再出现 `doors.get(...)` NPE。

## 客户端验收记录（2026-09-19/20）

- 用户原话：「验证通过，15300 顺利完成」。
- 验收覆盖：领奖阶段任务书不再空白、`REWARD` 后可以继续找 805327 领奖并进入 `COMPLETE`；副本销毁后的延迟开门 NPE 不再复现。
- 修复前运行期证据（保留）：`2026-09-19 23:44:21` `SM_QUEST_ACTION 任务=15300 状态=4 步数=14` → 任务信息消失；`23:38:52` `DrakenspireDepthsQInstance.java:727` `doors.get(267)` NPE。

## 同型全库审计（2026-09-20）

用户追问"最后领奖路线缺失修过多次为何仍未发现"，已做全库审计并把清单、反思与决策规则落在
`.agents/summary/quest-reward-projection-audit/2026-09-20-legacy-reward-projection-audit.zh-CN.md`；
结论：本轮只修客户端已验收的 15300/25300，其余 106 个 `MISMATCH_PROJECTION` / 32 个 `MISSING_RECOVERY_EDGE` 进入清单，禁止机械批量套用。
