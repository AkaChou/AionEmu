# 任务 3934 奖励预览路由重复导致 AMBIGUOUS_TRANSITION

- 状态: 实现完成，待重新拷贝资源/复测（PENDING）
- 日期: 2026-09-14
- 症状:
  - 启动日志 `Can't initialize typed quest engine.`，原因 `QuestCompilationException: AMBIGUOUS_TRANSITION: same event has overlapping transitions without unique priorities: TALK_TO_NPC`
  - `//reload quest` 失败
  - `target/surefire-reports/com.aionemu.gameserver.questEngine.ProductionCatalogWhitelistVerificationTest.txt`: `3934:AMBIGUOUS_TRANSITION...`，`PRODUCTION_COMPILE_OK=6192`，失败 1

## 根因

`3934.xml` 同时存在两套 NPC 203701 的 REWARD 预览路由，source 都是 `reward`，都没有 priority/conditions：

1. 显式客户端路由:
   - `USE_OBJECT`（dialogId -1）-> `SHOW_QUEST_PAGE DEFAULT_SUCCESS(10002)`
   - `SELECT_QUEST_REWARD`（1009）-> `SHOW_QUEST_PAGE SHOW_SELECT_QUEST_REWARD_WINDOW1(5)`
2. `<npc-complete><preview actions="USE_OBJECT SELECT_QUEST_REWARD"/></npc-complete>` 宏展开:
   - `USE_OBJECT`（-1）-> `SHOW_QUEST_DIALOG 5`
   - `SELECT_QUEST_REWARD`（1009）-> `SHOW_QUEST_DIALOG 5`

同一个 `TALK_TO_NPC` 事件重叠且没有唯一 priority，编译器 fail-closed。

## 修复

删除 `npc-complete` 里的通用 `<preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>`，保留与客户端 HTML 对齐的显式两步路由。
`QuestDefinitionCompiler.restoreRewardPreviewContract` 检测到显式 -1/1009 路由后不会再次补预览。

## 验证

- 使用现有 `target/classes` 的 `QuestDefinitionXmlCompiler.compile` 逐个编译当前修改的 7 个任务: 10032、11468、14045、14046、20032、21468、3934 全部 `COMPILE_OK`
- 3934 编译后 reward 预览恰好两条: `-1 -> 10002`、`1009 -> 5`，与 `Quest3934ClientDialogAlignmentTest` 的断言一致
- 未执行 Maven 测试/打包；未重启服务

## 生效前提

`target/classes/aion/data/static_data/quest_definition/quests/3934.xml` 仍是旧资源（含 `<preview>`）。重新 `mvn -q process-resources`（或完整测试/打包）拷贝资源后，`//reload quest`/重启才会读到修复。
