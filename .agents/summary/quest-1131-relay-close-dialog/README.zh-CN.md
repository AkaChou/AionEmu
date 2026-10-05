# 1131 中继推进「结束对话」后多余弹页：after-commit 回调为真端关窗（2026-10-05）

## 症状（用户实机，2026-10-05 15:17 quests.log）

任务 1131（天族「未送达的防具」，接取 203097 → 中继 799093 拿出交易凭证 → 报告 203101）。
在 799093 交付凭证（`action=10000`＝SETPRO1＝select2_1 页的「结束对话」按钮）后，
服务端下发 `SM_DIALOG_WINDOW targetObj=21355 questId=0 下发页=10`——窗口未关、任务列表页重开，
玩家需再点一次「结束对话」才关窗。期望：**第一次点击即关窗**。

实机 trace（节选）：

```
15:17:58 [C->S] CM_DIALOG_SELECT npcId=799093 targetObj=21355 questId=1131 上一页=10   动作=31
15:17:58 [S->C] SM_DIALOG_WINDOW 玩家=Kk targetObj=21355 questId=1131 下发页=1352
15:17:59 [C->S] CM_DIALOG_SELECT 上一页=1352 动作=1353
15:17:59 [S->C] SM_DIALOG_WINDOW 下发页=1353
15:18:01 [C->S] CM_DIALOG_SELECT 上一页=1353 动作=10000
15:18:01 [S->C] SM_QUEST_ACTION 任务=1131 状态=3 步数=1
15:18:01 [S->C] SM_DIALOG_WINDOW targetObj=21355 questId=0 下发页=10     ← 多余弹页（缺陷）
```

## 证据链（口径：真端权威；退役 XML 仅参考；先对 XML 定位）

1. **先对退役 XML（参考面）**：1131 退役定义（`git show 4ede058c0~1:.../quest_definition/quests/1131.xml`）在
   799093 的 `SETPRO1` 转换 = `give 182200507 doc_quest_1131b + remove 182200506 + sync(PACKET_ONLY) +
   close-dialog`。实现与 XML 不一致（发页 10 ≠ close-dialog）⇒ 已定位到偏差。
2. **真端权威取证**（ScriptDLL）——Talk 族对话/报告侧分派器 `FUN_180cabb10`：
   `10000/10001/10002 → SetQuestProgress(+0xf0)(quest, 1/2/3) + 0x5d8 + GiveItem(+0x410) +
   RemoveItem(+0x1d0)`，**零发页**（同函数 10020-10023 分支才显式「跳下一条 select 页」，
   10000-10002 无页）。证据面：`.agents/summary/quest-engine-native/p3-prereqs/simple-talk-codegen.md` §函数表。
3. **0x5d8＝关窗**（2026-10-05 已实证）：仅状态包客户端不关窗、二次点击才关（14:53 实机）；
   补 close-dialog（`SM_DIALOG_WINDOW(0,0)`）后「结束对话。」一次点击即关窗（XML 车道 1002 教程链复测通过）。
   即 `PlayerQuestDialogPort.closeDialog` 与 `RetailDialogIntentClassifier` 的本地关窗形态。
4. **退役语料旁证（非权威）**：全量 SETPRO 转换 after-commit 普查
   （`census_setpro_tails.py`，3923 条）：3479 条 `close-dialog+sync`、142 条 `SELECT_QUEST+sync`、
   若干业务页——「回页 10」是少数派翻译形态，与真端「推进零页」结论一致。

## 根因

表车道两个中继处理器把「推进 after-commit」写成真端不存在的「回选择对话页 10」：

- `SimpleTalkHandler`（START 段中继步动作分支）——旧注释引「退役 XML SETPRO1 明文」，实为翻译夸大；
- `SimpleItemPlayHandler`（同轴分支，注释同源）。

`SM_DIALOG_WINDOW(objectId, 10)` 让客户端把 NPC 任务列表页重开（questId=0），玩家看到第二个「结束对话」。

## 修复（2 源文件 + 1 夹具 + 2 测试）

| 文件 | 改动 |
|---|---|
| `SimpleTalkHandler.java` | 中继推进 after-commit：`SM_DIALOG_WINDOW(objectId, SELECT_QUEST)` → `SM_DIALOG_WINDOW(0, 0)`（关窗，零发页） |
| `SimpleItemPlayHandler.java` | 同上（cabb10 同轴分支） |
| `NativeTalkFixture.java` | 新增 `assertCloseDialog(player)`：唯一关窗包 + 页 0 + 目标 0 + questId 0 |
| `SimpleTalkNativeFamilyGateTest.java` | 推进/重复推进断言改为 `assertCloseDialog` |
| `SimpleItemPlayNativeFamilyGateTest.java` | e2e 两步推进断言改为 `assertCloseDialog`（领奖收尾页 10 保持） |

保持不动（另一机制，真端 `npc-complete finish=SELECTION_DIALOG` 完成收尾族）：领奖收尾回页 10、
报告进行中页 10、奖励窗页 5。

## 验证

- 2026-10-05，用户授权，IDEA MCP runner（复用 IDE 运行配置）：
  - `SimpleTalkNativeFamilyGateTest` 15/15 ✅（含推进关窗断言 `assertCloseDialog`）；
  - `SimpleItemPlayNativeFamilyGateTest` 15/15 ✅（e2e 两步推进关窗断言）；
  - `QuestProductionStartupGateTest` 2/2 ✅（生产目录 707 行编译无契约违规）。
- **客户端验收通过（2026-10-05 17:19，用户确认「客户端验证成功 提交」）**：重启后的服务端
  （进程 17:16:25 启动）trace——`17:19:28,958 动作=10000 上一页=1353 → SM_QUEST_ACTION 状态=3
  步数=1 → 17:19:28,959 SM_DIALOG_WINDOW targetObj=0 questId=0 下发页=0`——第一次点「结束对话」
  即关窗，不再重开任务列表页。验收记录：`.agents/summary/quest-acceptance/1131-2026-10-05-client-accepted.md`。
- **2026-10-05 17:13 复测排除记录**：该次实机仍见 `下发页=10`——原因是服务端为 IDEA 于
  **14:59:28** 启动的常驻进程（`-classpath target/classes`，日志在仓库根 `log/`），而本修复
  15:28 写入、15:29 编译；JVM 不热加载已加载类 ⇒ 该进程执行的是旧字节码。**复测前须重启服务端**
  （重启由用户执行；重启后期望签名：`动作=10000 → SM_QUEST_ACTION 步数=1 → SM_DIALOG_WINDOW
  targetObj=0 questId=0 下发页=0`）。
- 备注：工作区另叠有并行未提交改动（接取收尾 20000/20001 关窗、开门清单修复等），本主题仅
  覆盖中继推进 after-commit；上述测试在叠加态下全绿。

## 沉淀

- 记忆库：QE-141 的「推进 after-commit = 回选择页 10」结论回调为「关窗（真端 0x5d8）」；
  QE-142 的「表车道 cabb10 推进后段待复核」跟进项收口。
- 口径（用户裁定，2026-10-05）：**真端是权威证据；退役 XML 只是参考**；诊断顺序 = 先对退役 XML
  （实现与 XML 一致仍出问题 ⇒ 偏差在「与真端不一致」，再用真端权威数据修复）。
