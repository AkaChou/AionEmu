# 19671 中继收口「完成页」误显与教官处交付面缺失（2026-10-05）

## 症状（用户实机，2026-10-05 18:18 quests.log）

任务 19671「蕾娜的欢迎问候」（DD_TALK_CHAIN 单步链：教官蕾娜 806698 接取/交付，商人梅利娜 806699
中继）实机 trace：

```
18:18:54 [C->S] 动作=31  npcId=806699 上一页=10   questId=19671
18:18:55 [S->C] 下发页=1011
18:18:56 [C->S] 动作=10255（SET_SUCCEED）
18:18:56 [S->C] SM_QUEST_ACTION 状态=4 步数=1
18:18:56 [S->C] SM_DIALOG_WINDOW targetObj=1173 questId=19671 下发页=1008   ← 缺陷 1
```

玩家描述：与梅利娜对话完显示「任务已完成」，但任务进度仍停在「向成长支援教官报告」，梅利娜头顶
仍有任务标记，再对话无任务信息（页 10）。回教官处目标为交付/领奖，但当前实现无交付路由（缺陷 2）。

## 证据链（口径：真端权威；退役 XML 仅参考）

1. 退役 19671 尾（`git show 4ede058c0~1:.../quests/19671.xml`）：`started + 806699 + SET_SUCCEED →
   reward`，after-commit = `sync-quest-state(LEVEL_AND_VISIBILITY_REFRESH) + close-dialog`（零发页）；
   同批 10500/13961 同形。
2. 真端 `FUN_180c474b0` 的 `0x280f` → SetProgress + `mgr+0x5d8`；`+0x5D8` 的「完成通道即发页」解读
   已在 p7-prereqs/dd-host-interface-detail.md 勘误（DLL 侧对象调用、非宿主发页槽）。
3. 质证（子会话 1131 案例，2026-10-05 实机验收）：中继推进 after-commit = 关窗
   （`SM_DIALOG_WINDOW(0,0)`）；SETPRO 全量普查 3923 条=3479 close-dialog+sync / 142 SELECT_QUEST
   ——「回页 10」为少数派翻译形态；实机「仅状态包不关窗、关窗包才关」。
4. 旧视图断言（GrowthQuestDialogPageAlignmentTest @715a00136^ 被删）：`started + 806699 + 10255 →
   reward + [LEVEL_AND_VISIBILITY_REFRESH, 页 10]` 为待修正形态；`reward + 806698 + 31 → 奖励窗档位页
   （SHOW_SELECT_QUEST_REWARD_WINDOW1=5）`、`npc-complete finish=SELECTION_DIALOG → 页 10` 为本轮
   教官交付面依据。ReportToManySetSucceedAlignmentTest（活跃）同断言 `reward + 1009 → 页 5`。
5. 用户症状三连：（a）页 1008=QUEST_COMPLETE 误显；（b）Mellina 标记不刷新（缺 visibility 刷新轴）；
   （c）教官处交付面缺失（reportTalks 只注册零步行——真端「所有行恒建交付对象 #2」）。

## 修复（`DataDrivenNativeRuntime`，表驱动通用层，无任务 ID 特例）

| # | 位置 | 改动 |
|---|---|---|
| 1 | `dispatchDialog` `ACTION_ADVANCE_COMPLETE(10255)` 分支 | `advance`（检查返回值）+ `refreshLevelAndVisibility`（updateZone+updateNearbyQuests，LEVEL_AND_VISIBILITY_REFRESH 可见性轴）+ `SM_DIALOG_WINDOW(0,0)` 关窗；删除页 1008+questId 下发 |
| 2 | 构建期 `reportTalks` 注册 | 条件 `acquire.kind()==4 && steps().isEmpty()` → `acquire.kind()==4`（全部 Talk 行注册交付对象 #2）；`dispatchReportDialog` 加零步守卫：START 态只服务零步行（中继步行 START 归进度面） |
| 3 | `dispatchAcquireDialog` | 加 REWARD 守卫：待交付行不属接取面（缺守卫时 REWARD 态 ≥1000 动作被 default 原样回发，劫走交付面 `1009 → 奖励窗` 路由） |

测试（`DataDrivenNativeRuntimeGateTest`）：`relaySetSucceedClosesTheWindowAfterTheAdvance`（10255 →
REWARD + 关窗）、`relayTalkChainRowsDeliverOnTheRewardInstructor`（START 不认领 / REWARD 31、1009 →
页 5 / 23 → 结算 + 页 10）；新增 `reportTalkInterests()` 视图。

未镜像登记：`onQuestStateChanged`（等级任务重评估轴）在 tablelane 无先例未落面。

## 验证（2026-10-05，用户授权，IDEA MCP runner，复用 IDE 运行配置）

- `DataDrivenNativeRuntimeGateTest`：**28/28 全绿**（0 failed / 0 ignored；含新增
  `relaySetSucceedClosesTheWindowAfterTheAdvance`、`relayTalkChainRowsDeliverOnTheRewardInstructor`），
  exitCode 0。
- `DataDrivenProgressTest`：**9/9 全绿**，exitCode 0。
- 客户端实机复测（待用户重启服务端；IDEA 常驻进程需加载新字节码）：
  1. 蕾娜(806698) 接取 → 梅利娜(806699)：`31 → 1011`；`10255 → SM_QUEST_ACTION 状态=4 步数=1` +
     `SM_DIALOG_WINDOW targetObj=0 questId=0 下发页=0`（关窗；不再见页 1008「任务已完成」）；
  2. 梅利娜头顶任务标记应随可见性刷新消失；
  3. 回蕾娜(806698)：打开见任务列表（页 10）→ 行选 31 → 页 5 奖励窗 → 领奖 → 结算回页 10、任务完成。
