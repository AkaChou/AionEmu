# Quest Systemic Goal 进度台账（GOAL_PROGRESS）

> 目标：以 Aion 5.8 真端解包数据为权威，对 AionEmu 任务系统做长期系统性根因治理。
> 权威外部数据：`/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml`（只读）。
> 本台账记录每个审计轴的：audit count / real defects / false positives / evidence blocked / fixed / commit / tests / next action。

## 快照基线（2026-09-18 全库只读扫描）

- 真端任务 10,035 个；生产任务 6,222 个。
- 审计脚本：`.agents/summary/quest-systemic-goal/audit_start_metadata.py`
- 差异清单：`.agents/summary/quest-systemic-goal/start-metadata-diff.tsv`

### start-metadata 轴量化结论（sentinel 归一后）

| 轴 | audit 范围 | 差异数 | real defect | false positive / 等价表达 | 状态 |
|---|---|---|---|---|---|
| min-level | 真端可接任务（min≠999 占位）6,215 | 9 | 9（生产漂移） | 0 | P1 修复 |
| max-level | 全部 6,222 | 360 | 28（19 生产过宽无上限 + 4 生产 82→真端 65 + 5 族内不对称残留 cap） | 332（329 整族一致 cap=82 有意封顶 + 3 生产 998 等效无上限） | P2 修复+例外 |
| faction | 全部 6,222 | 1 | 0 | 1（15205 服务端按阵营拆分 15205/25205） | 例外记录 |
| gender | 全部 6,222 | 0 | 0 | 0 | 基线锁定 |
| max_repeat_count | 全部 6,222 | 0 | 0 | 0 | 基线锁定 |

### 真端 sentinel 语义（已取证）

- `minlevel_permitted=999`：真端占位任务（不可接取），生产侧自行激活，min 不纳入比对。
- `maxlevel_permitted=0`：字段默认，无上限。
- `maxlevel_permitted=998 / 999`：不可达大值，等效无上限（998 全部出现在 min=66 的 5.8 末期任务上；999 同时用于占位任务）。
- 生产 `max-level=2147483647 / 999 / 998`：同样等效无上限，与真端无上限等价。
- 真端 `race_permitted=pc_light pc_dark` 与生产 `races=PC_ALL` 等价。

## 阶段 P0：Goal 进度台账

- 状态：DONE（本文件）
- 产物：本台账 + 全库 start-metadata 审计脚本与差异清单

## 阶段 P1：min-level 真端精确对齐

- audit count：6,215（真端可接任务）
- real defects：9（1648、2231、2641、19000、19001、19002、19003、25407、25408）
- 修复明细（生产 → 真端）：
  - 1648：45 → 42（镜像 2648 生产/真端均 42，互证）
  - 2231：13 → 12
  - 2641：37 → 41（注意：镜像 1641 真端为 42，真端两族本就不同，以真端 2641=41 为准）
  - 19000/19001/19002/19003：40 → 50（镜像 29000~29003 生产/真端均 50，互证）
  - 25407/25408：70 → 68（镜像 15407/15408 生产/真端均 68，互证）
- 门禁：`QuestRetailStartMetadataGateTest` + 基线 `src/test/resources/quest/quest-start-metadata-retail-contract.tsv`
- 目录：`docs/QUEST_CATALOG.zh-CN.md` 同步刷新目标行（含 28649 文档漂移 66+→37+）
- 状态：DONE（见 commit）

## 阶段 P2：max-level 与 faction 全库收敛

- audit count：6,222
- real defects 28，分三类：
  - A. 真端有明确上限，生产无上限（19 条）：4711→50、4712→50、4722→55、15665→67、15666→67、18833→23、18835→26、25665→67、25666→67、29672→55、29673→19、29674→30、29675→40、29676→48、29684→19、29685→29、29686→39、29687→49、29688→65
  - B. 生产 82 过宽，真端 65（4 条）：80621、80622、80643、80644
  - C. 真端无上限，生产残留零散 cap（5 条）：27525（82，同族 17525 无上限）、50074（82，同族 Kumuki 50073=50/50074 无上限）、80945（82）、80946（82，同族 80947 无上限）、80878（75，同族 Luna 80876/80877=75 但真端 80878 明确 0）
- false positive / 等价表达 332 条：
  - 329 条整族一致 `max-level=82`（5000~5040 制作补给族、5100~5581、15300/25300、35052~35065 阵营每日、36542~36548、39022、45052~45065、46537~46548、49022、80602~80610 组队任务等）：服务端系统性版本封顶惯例（整族无一例外=82，与配置全局上限 83 相邻；5.8 玩家 66 级封顶，无玩家可见影响）→ 有意封顶例外清单 `quest-start-metadata-retail-cap-exceptions.tsv`
  - 25533、25566、25640：生产 `max-level=998` 等效无上限，等价表达
  - 15205：faction 服务端按阵营拆分（配对 25205），例外记录
- 状态：DONE（见 commit）

## 阶段 P3：职业权限语义收敛

- 状态：PENDING（raw class tokens 已在 start-metadata-diff.tsv 的 class-tokens-retail 行留档）

## 阶段 P4：奖励结算精确对齐

- 状态：PENDING

## 阶段 P5：下一类未根治维度

- 状态：PENDING

## 验证记录

### 2026-09-18 P1+P2 联合门禁（P1/P2 共享同一基线与门禁，合并交付）

- 命令 1：`mvn -q -Dtest=QuestRetailStartMetadataGateTest test`
  - Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
- 命令 2：`mvn -q -Dtest=QuestRetailStartMetadataGateTest,QuestItemSourceContractGateTest,QuestDropContractGateTest,QuestMovieAndDialogLoopRegressionTest,QuestDefinitionDirectoryLoaderTest,CompletedQuestPrerequisiteRegressionTest,QuestClientContractGateTest,ProductionCatalogWhitelistVerificationTest test`
  - QuestRetailStartMetadataGateTest: 6/0/0/0
  - QuestItemSourceContractGateTest: 3/0/0/0
  - QuestDropContractGateTest: 1/0/0/0
  - QuestMovieAndDialogLoopRegressionTest: 19/0/0/0
  - QuestDefinitionDirectoryLoaderTest: 2/0/0/0
  - CompletedQuestPrerequisiteRegressionTest: 7/0/0/0
  - QuestClientContractGateTest: 1/0/0/0
  - ProductionCatalogWhitelistVerificationTest: 1/0/0/0
- 生产编译：PRODUCTION_COMPILE_OK=6189，PRODUCTION_COMPILE_FAILURES=0，
  PRODUCTION_INTERACTION_OBJECT_FAILURES=0，PRODUCTION_WHITELIST_VIOLATIONS=0
- 未执行验证：Aion 5.8 客户端实机复测（等级门槛变化需客户端登录验证）——PENDING，
  由用户执行；本批改动只影响接取资格数值判定，无状态机/协议/奖励路径变化。
