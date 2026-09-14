# 任务 14045/14046 影片翻页后目标页缺失导致 CM_DIALOG_SELECT 循环

- 状态: 客户端验收通过（ACCEPTED_EXISTING_PATTERN；2026-09-14 用户确认 14045/14046 验证通过）
- 日期: 2026-09-14
- 修复提交: `ae1015818bf2b530f4ba0ea7ec4d26d6e0cdbf43`
- 验收记录: `../quest-acceptance/14045-14046-2026-09-14-client-accepted.md`
- Pattern: `MOVIE_CONTINUATION_RESPONSE`（代表提交 `8b058d4b4`，代表测试 `Quest14047ClientDialogAlignmentTest#returnsFromMovie421ToTheStep11PageAndThenAdvancesToStep5`）
- 实时证据:
  - 14045: `SM_DIALOG_WINDOW(10)` -> `CM_DIALOG_SELECT(action=31, quest=14045)` -> `SM_DIALOG_WINDOW(1011)` -> `action=1012` -> `SM_DIALOG_WINDOW(1012)` -> `action=1013` 重复无响应
  - 14046: `SM_DIALOG_WINDOW(10)` -> `action=31, quest=14046` -> `SM_DIALOG_WINDOW(1352)` -> `action=1353` 重复无响应
- 客户端映射证据:
  - `docs/quest/client-dialog-mapping/quest-dialog-action-details.csv`: 14045 `select1_1(1012)` 的按钮是 `HACTION_SELECT1_1_1(1013)`；14046 `select2(1352)` 的按钮是 `HACTION_SELECT2_1(1353)`
  - 对应目标页 `select1_1_1(1013)` / `select2_1(1353)` 都是 `active` 且各有 1 个后续按钮

## 根因

这两个过渡已经注册了翻页动作，但 `after-commit` 只有 `<play-movie>`，没有下发目标页：

- `14045.xml`: `started + NPC 278506 + SELECT1_1_1` -> 只 `play-movie(272)`
- `14046.xml`: `s1 + NPC 203834 + SELECT2_1` -> 只 `play-movie(102)`

客户端播放影片后仍停留在上一页，继续重发同一 `CM_DIALOG_SELECT`；服务端没有 `SHOW_QUEST_PAGE`/`close-dialog` 响应，形成循环。

历史修复 `cfc2fa048`（register story page-turn routes for 156 quests）的规则只补“未注册的按钮动作”，这两个动作因为已经被影片过渡注册而被跳过；`docs/quest/client-dialog-mapping/quest-order-audit.csv` 已把 1013/1353 记录为 `CLIENT_PAGE_UNREACHED`。

## 修复

- `src/main/resources/aion/data/static_data/quest_definition/quests/14045.xml`: `play-movie(272)` 后补 `SHOW_QUEST_PAGE SELECT1_1_1`
- `src/main/resources/aion/data/static_data/quest_definition/quests/14046.xml`: `play-movie(102)` 后补 `SHOW_QUEST_PAGE SELECT2_1`
- 两任务目标页的下一跳 `SETPRO1(10000)` / `SETPRO2(10001)` 路由原本已存在，页面显示后即可继续。
- 回归覆盖: `Quest14045And14046MoviePageTurnContractTest`

## 同类扫描

规则: `TALK_TO_NPC` 的页面动作名对应该任务客户端 page id，但 `after-commit` 只有影片、没有目标页/关闭/传送。

家族扩展修复（2026-09-14，未提交）:

- `2002` NPC 203534：`SELECT2_1` 后补页 1353，并补 `SELECT2_1_1 -> 1354`；随后 SETPRO2 推进到 s2
- `2007` NPC 203539：`SELECT3_1` 后补页 1694，并补 `SELECT3_1_1 -> 1695`；随后 SETPRO3 推进到 v3
- `2008` NPC 203550：`SELECT5_1`（事务内移除三枚职业道具）后补页 2376，并补 `SELECT5_1_1 -> 2377`；随后 SETPRO5 进入 320020000
- `24045` NPC 279004：`SELECT2_1` 后补页 1353；SETPRO2 已存在
- `24052` NPC 204753：`SELECT1_1` 后补页 1012，并补 `SELECT1_2 -> 1097`；随后 SETPRO1 发放三枚道具并推进到 s1
- `24053` NPC 204787（started）：`SELECT1_1` 后补页 1012；SETPRO1 已存在。step1-step4 的旧 handler switch fallthrough 保持 movie-only，不在客户端 1011->1012->10000 可达链上，作为有意例外
- 回归覆盖：`MovieContinuationResponseFamilyTest`

## 验收验证

```bash
mvn -q -Dtest=Quest14045And14046MoviePageTurnContractTest test                     # 通过
mvn -q -Dtest=MovieContinuationResponseFamilyTest,Quest14045And14046MoviePageTurnContractTest test  # 通过
mvn -q -Dtest=QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test
# PRODUCTION_COMPILE_OK=6193 / FAILURES=0 / WHITELIST_VIOLATIONS=0
```

用户使用 Aion 5.8 客户端复测 14045/14046 的 1011->1012->1013 与 1352->1353->10001 路径并确认通过；启动日志、成功协议 trace、截图和重登/死亡/重复路径 not captured。
