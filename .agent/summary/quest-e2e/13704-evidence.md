# Q13704 Fona's Quick Fix 取证记录

- Pattern：`NONE`；与 `MULTI_NPC_HANDOFF_REWARD_OWNER` 共享“先报告页、后原生奖励窗”的协议形状，但本任务由同一 NPC `802331` 同时承担报告和领奖，不是多 NPC handoff，不能直接当作同型批量修复。
- 当前 IR：`started + use-item 182215527 -> reward` 在同一次提交后直接显示 page `5`；`reward + 802331 + USE_OBJECT/1009` 也直接显示 page `5`，使客户端 page `2375` 不可达，原生奖励窗没有客户端可达的完成入口。
- Aion 5.8 客户端：`quest_q13704.html` 的 active 页链为 `SELECT1(1011) -> SELECT5(2375) -> 1009 -> SHOW_SELECT_QUEST_REWARD_WINDOW1(5)`；`2375` 的唯一可见按钮是 `1009`。
- 旧 handler：`_13704Fona_Quick_Fix` 在物品使用后以 `var0=1` 进入 `REWARD` 并同步状态；`REWARD + 802331 + USE_OBJECT` 显示 `2375`；随后 `1009` 先扣 `182215527`，再调用 `sendQuestEndDialog` 显示 page `5`；原生奖励动作再完成任务。
- 修复合同：保留物品使用后的状态同步，移除其直接显示 page `5`；将 `USE_OBJECT` 和 `SELECT_QUEST_REWARD` 分拆为 `2375` 与 page `5` 两条 route；把任务物品扣除移到 `1009`，不在原生奖励完成时重复扣除。
