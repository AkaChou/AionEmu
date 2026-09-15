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

家族扩展修复（2026-09-14，提交 `d7e0f4cb0`）:

- `2002` NPC 203534：`SELECT2_1` 后补页 1353，并补 `SELECT2_1_1 -> 1354`；随后 SETPRO2 推进到 s2
- `2007` NPC 203539：`SELECT3_1` 后补页 1694，并补 `SELECT3_1_1 -> 1695`；随后 SETPRO3 推进到 v3
- `2008` NPC 203550：`SELECT5_1`（事务内移除三枚职业道具）后补页 2376，并补 `SELECT5_1_1 -> 2377`；随后 SETPRO5 进入 320020000
- `24045` NPC 279004：`SELECT2_1` 后补页 1353；SETPRO2 已存在
- `24052` NPC 204753：`SELECT1_1` 后补页 1012，并补 `SELECT1_2 -> 1097`；随后 SETPRO1 发放三枚道具并推进到 s1
- `24053` NPC 204787（started）：`SELECT1_1` 后补页 1012；SETPRO1 已存在。step1-step4 的旧 handler switch fallthrough 保持 movie-only，不在客户端 1011->1012->10000 可达链上，作为有意例外
- 回归覆盖：`MovieContinuationResponseFamilyTest`

## Phase 1 硬门禁

`QuestMovieContinuationGateTest` 已加入默认测试集：它从 tracked 客户端 CSV 和编译后 IR 枚举“同状态 movie-only page-turn”，只允许 24053 step1-step4 四条显式 ledger；新增违规或遗留已修复 ledger 都会让测试失败。

- 干净 HEAD `d7e0f4cb0` 的临时 worktree 验证：`QuestMovieContinuationGateTest` 通过。
- 当前共享工作区另有并行任务的 `3940.xml` `DUPLICATE_NODE_PROJECTION`，不属于本次 Phase 1；在该并行改动修复前，主工作区的全生产目录测试会被它阻塞。

## Phase 2 编译期 fail-closed（2026-09-15，未提交）

Phase 1 只在测试期发现问题；Phase 2 把同一条规则下沉到**生产编译路径**，未登记账的同状态 movie-only 翻页会直接让任务目录编译失败（启动期报 `Can't initialize typed quest engine` + `MOVIE_WITHOUT_CONTINUATION`）。

- 契约资源（生成物，不手改）：
  - `src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv`：由 `.agents/summary/quest/generate_quest_dialog_contract.py` 从 tracked 客户端 CSV 生成（过滤 `source_variant=active`、`page_mapping=exact`、`action_count>0`），文件头记录两个源 CSV 的 SHA-256；`--check` 提供无写入校验
  - `src/main/resources/aion/definitions/quest_dialog/movie_continuation_exceptions.tsv`：显式例外 ledger，当前仅 24053 `step1`-`step4` 的 `dialog 1012`
- 共享规则：`QuestMovieContinuation.violations(definition, contract)` —— `TALK_TO_NPC` 自环 + 客户端该动作有按钮页 + `after-commit` 含 `play-movie`/`play-movie-random` + 无 `SHOW_QUEST_PAGE`/`show-selection-dialog`/`show-dialog-window`/`close-dialog`/传送 + 同节点无 `MOVIE_END` 路由 ⇒ 违规
- 加载路径：`QuestDialogContract.loadDefault()` **优先读外部 definitions 目录**（`Config.definitionFile("quest_dialog/...")`，与任务 XML、schema 同一部署布局），目录内没有副本时才回退 classpath；资源缺失或格式非法分别抛 `QUEST_DIALOG_CONTRACT_MISSING` / `QUEST_DIALOG_CONTRACT_INVALID`
- 热重载：`QuestEngine.loadProductionCatalog()` 每次编译前调用 `QuestDialogContract.invalidateDefault()`，因此 `//reload quest` 会重新读取契约与 ledger（改 ledger 后无需重启）；打包 jar 的 `maven-jar-plugin` 始终排除 `aion/**`，所以生产必须走外部文件路径，classpath 只服务开发与测试
- 强制点：`QuestDefinitionXmlCompiler.compile(InputStream)` / `compile(InputStream, Schema)` → `QuestDefinitionCompiler.compile(definition, contract)` → `validateMovieContinuationContract(...)`；违规且不在 ledger 时抛 `MOVIE_WITHOUT_CONTINUATION`（消息含任务、节点、dialog、页面名和 movie ID）
- 兼容路径：`QuestDefinitionCompiler.compile(QuestDefinition)` 仍使用 `QuestDialogContract.empty()`，手工构造 IR 的单元测试不受影响
- 契约新鲜度门禁：`QuestMovieContinuationGateTest#checkedInContractMatchesTheTrackedClientMappingCsv` 比对契约头部源哈希与 tracked CSV；CSV 变更后必须重新生成契约（已用篡改 CSV 的负向验证确认会失败）
- 部署要求：外部资源模式下 `aion/**` 不进 jar，`package.sh`/`start-silent.sh` 会把 `definitions/quest_dialog/*.tsv` 同步到 `AION_HOME/definitions/quest_dialog/`；只要沿用现有打包脚本即满足，手动部署需要把这些文件一起放到运行目录

## 验收验证

```bash
mvn -q -Dtest=Quest14045And14046MoviePageTurnContractTest test                     # 通过
mvn -q -Dtest=MovieContinuationResponseFamilyTest,Quest14045And14046MoviePageTurnContractTest test  # 通过
mvn -q -Dtest=QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test
# PRODUCTION_COMPILE_OK=6193 / FAILURES=0 / WHITELIST_VIOLATIONS=0
```

Phase 1/2 在干净 HEAD `0a121ca50` 的临时 worktree（叠加本批改动）验证：

```bash
python3 .agents/summary/quest/generate_quest_dialog_contract.py --check   # QUEST_DIALOG_CONTRACT_OK
mvn -Dtest=QuestMovieContinuationGateTest,MovieContinuationResponseFamilyTest,Quest14045And14046MoviePageTurnContractTest,ProductionCatalogWhitelistVerificationTest test
# Tests run: 15, Failures: 0, Errors: 0
# PRODUCTION_COMPILE_OK=6193 / FAILURES=0 / WHITELIST_VIOLATIONS=0
mvn -Dtest=QuestDefinitionCatalogManifestTest,QuestEngineNpcDialogDispatchTest,QuestEngineRuntimeCompositionTest,ProductionCatalogWhitelistVerificationTest test
# Tests run: 30, Failures: 0, Errors: 0
```

负向验证（不属于默认测试）：删除 14045 的 `SHOW_QUEST_PAGE SELECT1_1_1` 后编译报 `MOVIE_WITHOUT_CONTINUATION`；篡改 `quest-dialog-pages.csv` 后新鲜度门禁失败。

用户使用 Aion 5.8 客户端复测 14045/14046 的 1011->1012->1013 与 1352->1353->10001 路径并确认通过；启动日志、成功协议 trace、截图和重登/死亡/重复路径 not captured。
