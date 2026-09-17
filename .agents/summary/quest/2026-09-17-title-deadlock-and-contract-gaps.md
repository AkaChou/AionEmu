# 任务排查与系统性修复记录（2026-09-17）

## 1. 称号前置自指死锁（Bootstrap Deadlock）彻底排查与解除
- **排查范围**：全服 6,222 个任务 XML 与真端客户端 。
- **排查发现**：
  - 真端客户端中实际仅 29 个任务具有 title 前置要求（均为军衔或特殊资格）。
  - 服务端仅 3 个任务配置了 title-id：10521（天族阿斯特拉 65 级主线“回忆的永恒”）、20521（魔族阿斯特拉 65 级主线“找回的命运”）、2511（魔族“出人头地的野心”）。
  - 这 3 个任务全部将自身通关发放的称号（306“记忆的继承者”、75“暗夜之印”）错误配为了接取前置，导致新升 65 级玩家无法接取主线。
- **修复与守卫**：
  - 移除 10521、20521、2511 中的 title-id 约束。
  - 在 QuestRewardTitlePrerequisiteAuditTest 中增加 noQuestRequiresItsOwnRewardTitle() 全量门禁测试，严防未来任何任务误将自身奖励称号配为前置。

## 2. 道具使用任务拒绝动作补齐（BUTTON_WITHOUT_ROUTE 销项）
- **涉及任务**：1197, 1198, 80008, 80009。
- **病灶**：使用道具弹出 page 4 接受窗口时，点击拒绝按钮客户端发送 action 1008（HACTION_FINISH_DIALOG）。原 XML 仅声明了 QUEST_REFUSE_1 (1003)，缺失 FINISH_DIALOG。
- **修复**：将拒绝动作扩充为 actions="QUEST_REFUSE_1 FINISH_DIALOG"，支持客户端全部关闭/拒绝行为。

## 3. 接取后 page 1003 按钮动作闭环路由补齐
- **涉及任务**：2289, 2367, 2411, 2443, 2448, 2922, 3088, 3936, 3937, 4940, 4941（共 11 个任务、18 条缺失路由）。
- **病灶**：在 unaccepted -> started 接取后下发 page 1003 (QUEST_ACCEPT_1)，客户端展示的后续按钮在 started 状态下无对应 NPC 路由，导致对话卡死或客户端契约违规。
- **修复**：
  - 2289：NPC 203616 点击 SETPRO1 (10000) 结束对话 (close-dialog)。
  - 2367、2411、2448、2922：发任务的主 NPC 补齐 SELECT1_1、SELECT1_2 分支展示页以及 SETPRO10、SETPRO20 分支记录与关闭路由。
  - 2443：NPC 204403 补齐 SETPRO1 / QUEST_ACCEPT_1（准备好了）以及 SELECT1_1_1（还没准备好）闭环。
  - 3088：NPC 798202 补齐 SELECT1_1 / SELECT1_2 / SELECT1_3 果汁选项及 SELECT1 重新考虑、SETPRO1/3/7 提交果汁路由。
  - 3936、3937：NPC 203710 / 203708 扩展 actions="QUEST_SELECT SELECT1" 响应 1003 上的“继续听”。
  - 4940、4941：NPC 204050 / 204060 补齐 SELECT1（继续听/点头）与 SETPRO1（结束对话）路由。

## 4. 客户端契约基线文件完全清零
- src/test/resources/quest/quest-client-contract-baseline.tsv 中历史残留的 22 条违规全部清零，客户端契约门禁达到 0 缺陷纯净状态。
