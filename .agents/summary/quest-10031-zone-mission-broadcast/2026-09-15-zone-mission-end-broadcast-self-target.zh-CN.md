# 任务 10031/20031 区域任务结束广播自目标导致 AFTER_COMMIT 失败

- 时间：2026-09-15
- 玩家：Ww（objectId 153807）
- 相关任务：10031「A Risk for the Obelisk」（天族）、20031「Go to Gelkmaros」（魔族镜像）
- 相关代码：`AfterCommitAction.BroadcastZoneMissionEnd`、`TypedQuestAfterCommitPort`、`PlayerQuestBroadcastPort`、`QuestProductionDispatcher.dispatchOwners`

## 一、实机证据（服务端日志）

```text
[QUEST-TRACE][C->S] CM_DIALOG_SELECT 玩家=Ww npcId=798927 targetObj=66896 questId=10031 上一页=10002 动作=1009
WARN [PacketProcessor:3] QUEST_RUNTIME - typed 任务 10031 已提交，但有 1 个提交后动作失败
WARN [PacketProcessor:3] QUEST_AUDIT - 任务 owner 10031 处理事件 TALK_TO_NPC ... 在阶段 AFTER_COMMIT 失败，已提交=true，动作=BroadcastZoneMissionEnd
QuestAfterCommitException: after-commit action BroadcastZoneMissionEnd failed for player 153807 quest 10031
```

关键事实：任务事务状态已提交，客户端页面/奖励窗口照常下发；失败只发生在 `after-commit` 广播。
`QuestUnitOfWork.runAfterCommit()` 是逐个 best-effort 执行，广播抛异常不会回滚，也不会阻断同一 transition 中后续的页面动作。

## 二、根因

1. `10031.xml` 的 4 条领奖路由（1 条 `SELECT_QUEST_REWARD` 预览 + 3 组完成分支）都声明了
   `<broadcast-zone-mission-end quest-ids="10031 10032 10033 10034 10035"/>`，把自身 10031 列为广播目标；`20031.xml` 同样自含 20031。全目录扫描仅 10031/20031 自含。
2. `10031/20031` 自身没有 `<zone-mission-end/>` 路由（只有 `level-up`/`enter-world` 自动接取边），而 `10032~10035`、`20032~20035` 都声明了该事件路由。
3. `PlayerQuestBroadcastPort.broadcastZoneMissionEnd()` 调用 `QuestProductionDispatcher.dispatchOwners(...)`；后者的合同是「路由条件不匹配算成功投递；缺路由或执行失败不算」，因此自身缺路由导致整体返回 `false`。
4. `TypedQuestAfterCommitPort.requireSuccess(false)` 抛 `QuestAfterCommitException`，被审计为 `AFTER_COMMIT` 失败，于是每次奖励预览/领奖都产生一条 `QUEST_RUNTIME` 警告。
5. 旧 Handler（`911440146:quest/inggison/_10031A_Risk_For_The_Obelisk.java`、`911440146:quest/gelkmaros/_20031Go_To_Gelkmaros.java`）确实把整族（含自身）交给 `QuestEngine.onEnterZoneMissionEnd`，但旧引擎对未注册该事件的 quest 是静默 no-op，不存在「缺路由失败」概念。迁移到 typed 引擎后，旧的宽松写法变成必现硬失败。

## 三、修复

只改任务 XML，不放松运行时合同：

- `quests/10031.xml`：4 处广播目标改为 `10032 10033 10034 10035`
- `quests/20031.xml`：4 处广播目标改为 `20032 20033 20034 20035`

未修改 `QuestProductionDispatcher.dispatchOwners` 的缺路由硬失败语义，也不改变广播位置（预览 1009 与所有完成分支），以保持与旧 Handler 相同的解锁时机。

## 四、回归

新增 `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest10031And20031ZoneMissionBroadcastTest.java`，锁定：

- 广播目标精确等于四个后续任务，绝不包含自身；
- `SELECT_QUEST_REWARD` 预览的 after-commit 顺序为 广播 → 奖励选择窗口；
- 每条完成分支的 after-commit 顺序为 刷新属性 → COMPLETION 同步 → 广播 → 最终选择页；
- `USE_OBJECT` 入口页不广播（与旧 Handler 一致）；
- 每个广播目标必须真实拥有 `<zone-mission-end/>` 路由，否则 `dispatchOwners` 会判定缺路由并失败。

## 五、验证结果

| 门禁 | 命令 | 结果 |
| --- | --- | --- |
| 任务专用回归 | `mvn -q -Dtest='Quest10031And20031ZoneMissionBroadcastTest' test` | PASS |
| 运行时相关 | `mvn -q -Dtest='QuestProductionDispatcherTest,BroadcastZoneMissionEndDefinitionTest,Quest10520ClientDialogAlignmentTest' test` | PASS |
| 生产目录/白名单 | `mvn -q -Dtest='ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest' test` | PASS：`PRODUCTION_COMPILE_OK=6193`、`FAILURES=0`、`WHITELIST_VIOLATIONS=0` |
| 全目录广播目标审计 | 内联静态扫描：6231 个任务定义、24 处 `broadcast-zone-mission-end` | PASS：`SELF_INCLUDES=0`、`UNROUTABLE_TARGETS=0` |

## 六、边界

- 未重启服务器、未做真机复测：真机需重走 `798927 USE_OBJECT → SELECT_QUEST_REWARD(1009)` 与领奖动作，确认不再出现 `AFTER_COMMIT BroadcastZoneMissionEnd failed`，并确认 `10032~10035` 可见性正常。
- 同类风险：任何 `broadcast-zone-mission-end` / `schedule-event-quest-refresh` 目标列表若包含未声明对应事件路由的 owner，都会触发同样的 AFTER_COMMIT 审计失败。
