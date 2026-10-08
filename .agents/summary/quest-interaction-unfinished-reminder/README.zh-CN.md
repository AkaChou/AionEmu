# 任务未完成交互提醒（quest_use_item 失败应答面）

## 1. 需求与裁定
- 用户实机反馈：任务未完成时点击任务物件（如 702663 `LF4_FOBJ_Q10035A`「Corridor Access Control / 回廊入口控制器」，任务 10035 第 7 步 TalkFOBJ 目标）无任何反馈。
- 裁定：交互应提醒，形态同「未完成欧比斯入场」拦截（`CM_USE_ITEM` 的 `meetsAbyssEntryRequirement` 失败 → `STR_MSG_CANNOT_TELEPORT_TO_ABYSS`）。
- 消息选择（用户定）：`STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST`(1300690)「尚未完成所需的任务，无法移动。」。

## 2. 成因链路（服务端静默点）
点击 702663（10035 未接取）：
1. CM_SHOW_DIALOG → `NpcController.onDialogRequest` → `canInteract()` ✓（talk_info 非空）。
2. `QuestItemNpcAI2.handleDialogStart` → `canStartInteraction` 通过（`onTalkEvent` 注册 = 10035，native 装载后常驻）→ 播 3 秒使用进度条。
3. `handleUseItemFinish`：`selectDialog(USE_OBJECT)` 与 `selectDialog(QUEST_SELECT)` 均未被任务引擎认领（玩家无匹配任务状态）→ 非对话物件零发包（`QuestItemNpcAI2.java:78-83`，改动前）→ 表现「进度条走完、什么都没有」。

## 3. 实现
`src/main/java/com/aionemu/gameserver/ai/QuestItemNpcAI2.java`：
- 新增 `FailedInteractionReply` 枚举 + `failedInteractionReply(dialogNpc, collectObject)`（同 `PortalDialogAI2.unclaimedReply` 模式）：
  - 对话物件（talk_info `is_dialog`）→ 页 1011（原行为）；
  - 采集对象（`SimpleCollectItemHandler.instance().targetsForNpc(npcId)` 命中）→ 零发包（QE-137 真端超杀/条件不满足零副作用口径，不得触碰）；
  - 其余任务物件 → 发 1300690。
- `handleUseItemFinish` 的失败分支改走该分类。

## 4. 测试（IDEA MCP，2026-10-08）
- `QuestItemNpcAI2Test` 3/3（含新增 `failedInteractionsReplyByObjectKind`）。
- `QuestEngineNpcDialogDispatchTest` 6/6。
- `QuestInteractionObjectCatalogTest` 6/7 —— `productionQuestUseItemTalkRoutesDeclareActionEligibility` 失败为**既有红（非本批引入）**：自 2026-09-21 在 `.agents/summary/quest-10527-reward-row/2026-09-21-10527-reward-row-and-family-audit.zh-CN.md` 登记（当时 8 条资格缺口），现收敛为 6 条：2307:700247、2664:700324、3036:700398、4004:700340、21105:700812、28303:700980。本次改动未触碰任何任务定义/catalog 数据，非本次引入。

## 5. 边界与遗留
- 已知边界：「引擎不认领即提醒」——任务完成后点击残留物件同样会提醒（文案不精确但不再是静默）；如需区分「相关任务全部 COMPLETE 则静默」，需按 `questNpc.getOnTalkEvent()` ∩ 玩家任务状态再叠一层过滤（待用户裁定）。
- **实机验收（2026-10-08，用户确认）**：冷重启后未接 10035 状态点击 702663 → 3 秒进度条后出现 1300690「尚未完成所需的任务，无法移动。」——验收通过。
- 相关但未处理：730256 `LF4_UnderPass_In`（silentera westgate「锡兰泰拉西门」）「动作 104」无响应——`PortalDialogAI2` 无 `portal_template2.xml` 配置、且不在任何任务路由；真端由 ScriptDLL `LF4_UnderPass_In_simple` 脚本处理（`58Server/server58-source/MainServer_ScriptDLL64/classes/NPC/IAIScriptNpcImp.cpp`），待专门批次。

## 6. 证据坐标
- `log/quests.log` 2026-10-08（Kk；702663/10035 无痕 = CM_SHOW_DIALOG 与 AI 层静默均无打点；730256 动作 104 ×7 无响应）。
- `.agents/summary/quest-10506-beritra-corridor/README.md`（10506 同型「进不去回廊」案例）。
- 记忆库 QE-137（采集物零包口径：超杀/条件不满足零副作用）。
- 客户端 L10N 文案：`<客户端目录>/L10N/CHS/Data/data.pak` → `Strings/client_strings_msg.xml`（1300690 / 1390152 实文）。
