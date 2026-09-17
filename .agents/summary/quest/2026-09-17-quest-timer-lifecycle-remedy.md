# 2026-09-17 任务计时器生命周期闭环专项排查

## 1. 排查维度

在奖励结算批次之后，继续按“时序资源收尾”维度排查：任务计时器（可见倒计时 / 隐形计时器）的启动与收尾是否闭环。判定口径：

- 任务通过 `start-quest-timer` / `start-invisible-timer` 启动计时器；
- 收尾必须二选一：
  1. 同 `timer-id` 的 `cancel-quest-timer`（使命结束主动取消）；或
  2. 与计时器类型匹配的到期事件路由（可见 → `quest-timer-end`，不可见 → `invisible-timer-end`）。

## 2. 全库扫描结果

全库 28 个含计时器的任务逐一核对（可见 / 不可见、到期路由 / 取消动作）：

- 27 个已闭环（19 个可见倒计时任务既有 `quest-timer-end` 路由又在终局路径 `cancel-quest-timer`；6 个 43 秒 `<npc>-return` / `-flyback` / `-flight` 隐形计时器按真端语义只靠 `invisible-timer-end` 路由闭环）；
- **1 个孤儿计时器：任务 `2230`**（魔族 L12 “A Friendly Wager”）。

## 3. 命中缺陷与真端证据

任务 `2230` 的题面明确为“在 30 分钟以内搜集 10 颗棕熊尖牙交给沙尼亚”，XML 在 `SETPRO1` 时启动 `start-quest-timer seconds="1800" timer-id="visible"`，但**任何路径都没有取消该计时器**。

历史 handler 证据（`/Users/mc/IdeaProjects/AionEmu`，QuestHandler `_2230AFriendlyWager`）：

```java
case CHECK_COLLECTED_ITEMS:
    if (var == 0) {
        if (QuestService.collectItemCheck(env, true)) {
            qs.setStatus(QuestStatus.REWARD);
            updateQuestStatus(env);
            QuestService.questTimerEnd(env);   // 交付成功即结束倒计时
            return sendQuestDialog(env, 5);
        }
```

即真端语义是：**赌注成功交付时结束计时器**；迁移后的 XML 漏掉了这次 `questTimerEnd`，导致玩家交完尖牙进入领奖后，客户端倒计时仍在跑，归零时会按“超时”渲染一个已经完成的赌注。

同时确认：`2230` 没有 `quest-timer-end` 路由属于**真端一致**行为——旧 handler 同样没有 `onQuestTimerEndEvent`，超时本就只由客户端展示、不做服务端判负，因此本次只补“成功交付取消计时器”，不擅自新增超时判负。

## 4. 修复内容

`src/main/resources/aion/data/static_data/quest_definition/quests/2230.xml`：在“交付 10 颗棕熊尖牙 → REWARD”的转换 after-commit 首位加入

```xml
<cancel-quest-timer timer-id="visible" scope="PLAYER_QUEST"/>
```

与真端 `questTimerEnd` 语义对齐，且排在 `sync-quest-state` 之前（先收尾计时器再刷新任务状态）。

## 5. 新增全库回归门禁

`QuestMovieAndDialogLoopRegressionTest`：

1. **`startedQuestTimersHaveCancelOrExpiryRoute()`（全库孤儿计时器门禁）**
   遍历全服可执行任务的编译 IR：收集 `StartQuestTimer` / `StartInvisibleTimer` 的 `timer-id`、`CancelQuestTimer` 的 `timer-id`、以及 `QuestTimerEnd` / `InvisibleTimerEnd` 路由；可见与不可见分别匹配类型，逐个 timer-id 断言闭环。修复前会命中 `2230`，修复后全库 0 违规。
2. **`quest2230StopsWagerCountdownOnHandIn()`（专项）**
   断言 `2230` 的 `started -> reward`（`CHECK_USER_HAS_QUEST_ITEM`）交付路径包含 `CancelQuestTimer` 且 `timer-id == visible`。

## 6. 验证结果

```bash
mvn test -Dtest=QuestMovieAndDialogLoopRegressionTest,QuestDefinitionDirectoryLoaderTest,CompletedQuestPrerequisiteRegressionTest,QuestClientContractGateTest,ProductionCatalogWhitelistVerificationTest
```

`QuestMovieAndDialogLoopRegressionTest` 15 项全绿；其余门禁（全服状态图 0 死胡同、前置 0 环、客户端契约 0 致命缺陷、生产目录 `PRODUCTION_COMPILE_OK=6186/FAILURES=0/WHITELIST_VIOLATIONS=0`）全部通过。

## 7. 架构沉淀

新增架构模式 `[QE-027]` 二十五、任务计时器生命周期闭环 (QUEST_TIMER_LIFECYCLE_CLOSURE)，同步 `patterns/quest-engine.md`、`systemPatterns.md`、`symptom-index.md`，并通过 `sync_memory_bank.py` / `check_memory_bank.py` 校验。
