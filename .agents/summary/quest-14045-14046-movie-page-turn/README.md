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

## Phase 3 `<movie-page-turn>` 编写块（2026-09-15，未提交）

- 新积木 `QuestXmlBlockExpander#expandMoviePageTurn`：`source`/`target`/`npc-id`/`action`/`movie-id` 必填，`page` 默认取与动作同名的客户端页面；`movie-type` 默认 `CUTSCENE`；可选 `conditions`、`actions` 子元素；可选 `next-action` 追加一条中继页路径
- 结构护栏：动作没有同名页面且未显式声明 `page` 时抛 `MOVIE_PAGE_TURN_PAGE_MISSING`，即积木无法表达「只播影片」这一缺陷形态；`movie-page-turn` 路由同时登记到 `explicitDialogRoutes`，派生积木不会再生成冲突路由
- `quest_definition.xsd` 新增 `movie-page-turn` 元素与 `movie-page-turn-type`；`docs/quest/WRITING_GUIDE.zh-CN.md` §3.3 增补作者合同
- 代表性迁移：14045（`SELECT1_1_1` + movie 272）、14046（`SELECT2_1` + movie 102）已改写成积木，`Quest14045And14046MoviePageTurnContractTest` 与 `MovieContinuationResponseFamilyTest` 断言 IR 完全不变
- 回归覆盖：`QuestXmlDomainBlocksTest` 新增 3 条（等价展开、条件/动作/movie-type 透传、缺页拒绝）

## Phase 4 运行时断路器（2026-09-15，未提交）

- `CM_DIALOG_SELECT` 在路由前调用 `breakDialogSelectLoop(...)`：同一目标、同一上一页、同一动作、同一任务按客户端重发节奏（相邻间隔 0.8–5 秒）连续出现 4 次仍未获得后续页时判定死循环；间隔过短（人类连点/重放）或过长都会重新计数，避免误伤
- 触发动作：写 `log.quest_dialog_select_loop` 告警（含玩家、npcId、targetObj、动作、上一页、questId、连续次数）、清除 NPC 任务行记忆、发送 `SM_DIALOG_WINDOW(0, 0)` 关闭窗口，并跳过本次任务路由
- 状态载体：`Player` 上的瞬时 `DialogSelectRepeat` 快照（不持久化，随玩家对象回收）；同一签名之外的选择、超过窗口的间隔都会重新计数
- 回归覆盖：`CM_DIALOG_SELECTRepeatGuardTest`（计数/签名变化/窗口过期/交替选择）+ `QuestDialogLoopBreakerProductionFlowTest`（真实协议：3 次无响应后第 4 次下发关闭窗口且任务状态不变）

Phase 3/4 在干净 HEAD `569ba10d8` 的临时 worktree（叠加本批改动）验证：

```bash
mvn -Dtest=QuestXmlDomainBlocksTest,CM_DIALOG_SELECTRepeatGuardTest,CMDialogSelectContextTest,QuestDialogLoopBreakerProductionFlowTest,Quest14045And14046MoviePageTurnContractTest,MovieContinuationResponseFamilyTest,QuestMovieContinuationGateTest,ProductionCatalogWhitelistVerificationTest,LocalizedLogCallsTest,QuestE2eInfrastructureTest,QuestEquippedStartProductionFlowTest,QuestPacketOrderRegressionTest test
# Tests run: 118, Failures: 0, Errors: 0
# PRODUCTION_COMPILE_OK=6193 / FAILURES=0 / WHITELIST_VIOLATIONS=0
```

未在真实 Aion 5.8 客户端上验证断路器表现（无法在客户端复现死循环）；`QuestDialogLoopBreakerProductionFlowTest` 走的是真实 `CM_DIALOG_SELECT` 包与生产 dispatcher，不等于真实客户端验收。

## 审查修复（2026-09-15，待验证）

代码审查（`/review`）后修复 4 项：

1. 断路器加入最小重发间隔 `MIN_DIALOG_SELECT_RESEND_GAP_MILLIS = 800`：只有间隔落在 0.8–5 秒的连续相同选择才计数，人类连点不再可能触发关窗；`QuestDialogLoopBreakerProductionFlowTest` 改为按 900ms 节奏驱动，`CM_DIALOG_SELECTRepeatGuardTest` 新增快速连点用例
2. `explicitDialogRoutes` 的 `npc-id` 预扫描改为容错解析：非法值不登记路由，由块展开阶段用 `XML_BLOCK_INVALID_INTEGER` 带任务上下文报错
3. 断路器阈值常量恢复包内可见（不再为跨包测试放宽），e2e 测试改为声明与生产一致的本地期望值
4. 新增 `MOVIE_PAGE_TURN_DUPLICATE_ACTION`：`next-action` 与 `action` 相同时显式失败，不再依赖编译器 `AMBIGUOUS_TRANSITION` 兜底；`QuestXmlDomainBlocksTest` 新增对应用例
5. 交互边界清零：`CM_SHOW_DIALOG` / `CM_CLOSE_DIALOG` 现在调用 `player.clearDialogSelectRepeat()`，重新打开或关闭对话不再延续之前的重发计数；`QuestDialogLoopBreakerProductionFlowTest#reopeningTheDialogRestartsTheResendCounter` 用真实 `CM_SHOW_DIALOG` 断言「边界后需重新累计 4 次才触发」
6. 新增同族编译期门禁 `PAGE_TURN_WITHOUT_ANY_RESPONSE`（`QuestPageTurnResponseGate`）：`TALK_TO_NPC` 动作在客户端合同里对应「有按钮的页面」但 `after-commit` 完全为空时编译失败，堵住「静默死按钮」整类；24053 的影片静默形态不受影响（仍由影片 ledger 豁免）。扫描确认现网该形态为 0 例

验证（2026-09-15，主工作区，用户授权后执行；未建 worktree）：

```bash
mvn -Dtest='QuestXmlDomainBlocksTest,CM_DIALOG_SELECTRepeatGuardTest,QuestDialogLoopBreakerProductionFlowTest,QuestMovieContinuationGateTest#compilerRejectsMovieOnlyPageTurnWithoutLedger+checkedInContractMatchesTheTrackedClientMappingCsv+externalDefinitionsDirectoryTakesPrecedenceOverTheClasspathCopy+classpathContractIsUsedWhenTheDefinitionsDirectoryHasNoCopy+invalidatingTheDefaultContractReloadsFromTheDefinitionsDirectory' -DfailIfNoTests=false test
# Tests run: 55, Failures: 0, Errors: 0
# QuestXmlDomainBlocksTest 44 / CM_DIALOG_SELECTRepeatGuardTest 5 / QuestDialogLoopBreakerProductionFlowTest 1 / QuestMovieContinuationGateTest 5
```

仍待验证（主工作区被并行 WIP 的 3939/3940 等改动阻塞，需干净目录）：

- `QuestMovieContinuationGateTest#sameStateMovieOnlyPageTurnsMatchTheProductionLedger`（遍历全生产目录）
- `ProductionCatalogWhitelistVerificationTest`（6193 个任务目录编译 + 白名单）

第 5 项（交互边界清零）验证（2026-09-15，主工作区，用户授权后执行）：

```bash
mvn -Dtest='CM_DIALOG_SELECTRepeatGuardTest,QuestDialogLoopBreakerProductionFlowTest,CMDialogSelectContextTest' -DfailIfNoTests=false test
# Tests run: 13, Failures: 0, Errors: 0
# 其中 QuestDialogLoopBreakerProductionFlowTest 2 条：阈值打断 + 重新打开对话后重新累计 4 次才触发
```

注意：e2e 用例驱动真实 `CM_SHOW_DIALOG` 时，测试替身 NPC 的 AI 工厂会打印一条 `AI 工厂出错` ERROR 日志（`AI2Engine` 预存在行为，测试环境噪声，不影响断言）。

第 6 项（静默死按钮门禁）验证（2026-09-15，主工作区 + 临时验证目录，用户授权后执行）：

```bash
# 主工作区（不需要生产目录）
mvn -Dtest='QuestPageTurnResponseGateTest,QuestXmlDomainBlocksTest,CM_DIALOG_SELECTRepeatGuardTest' test
# Tests run: 51, Failures: 0, Errors: 0

# 临时验证目录（含生产目录门禁，跑完立即删除）
mvn -Dtest='ProductionCatalogWhitelistVerificationTest,QuestMovieContinuationGateTest,QuestPageTurnResponseGateTest' test
# Tests run: 9, Failures: 0, Errors: 0
# PRODUCTION_COMPILE_OK=6193 / FAILURES=0 / WHITELIST_VIOLATIONS=0
```

结论：新增门禁对现网 6193 个任务零违规（与静态扫描的「0 例」一致），影片 ledger 例外不受影响。
