# Q13708 Proximity Protect 取证记录

- Pattern：与 Q13704 逐项同构；两者都不是多 NPC handoff，而是“同一 NPC 的报告页前置 + 原生奖励窗”合同。
- 当前 IR：物品使用后直接显示 page `5`，`reward + 802332 + USE_OBJECT/1009` 同样直接显示 page `5`，遗漏客户端可见的 `SELECT5(2375)`。
- Aion 5.8 客户端：`quest_q13708.html` 的 active 页链为 `SELECT1(1011) -> SELECT5(2375) -> 1009 -> SHOW_SELECT_QUEST_REWARD_WINDOW1(5)`；`2375` 的唯一可见按钮是 `1009`。
- 旧 handler：`_13708Proximity_Protect` 在 `START + var0=0` 使用 `182215529` 后进入 `REWARD`；`REWARD + 802332 + USE_OBJECT` 显示 `2375`；`1009` 扣除 `182215529` 后通过 `sendQuestEndDialog` 显示 page `5`。
- 修复合同：保留物品使用后的状态同步，分离 `USE_OBJECT -> SELECT5` 与 `1009 -> remove-item -> page 5`，原生奖励动作只结算奖励和完成任务。
