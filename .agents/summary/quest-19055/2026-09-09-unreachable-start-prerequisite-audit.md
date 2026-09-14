# 任务 19055 不可达接取前置审计

日期：2026-09-09

## 结论

任务 19055 的 `finished quest-id="19054"` 前置已在现有修复提交 `adc5cbc0b` 中删除。当前 19055 的 metadata 不再有任务前置，NPC 798450 的 `QUEST_ACCEPT_1(1002)` 仍然按 `unaccepted -> started` 执行，提交后依次刷新可见性并显示客户端页面 `QUEST_ACCEPT_1(1003)`。

19054 不存在于当前任务定义目录、生产任务目录或旧版 `quest_data.xml` 中；用户确认其为 999 级不可达任务。因此本次删除有明确的任务数据与用户侧证据，不改变任务 19055 的页面动作映射。

## 相似前置扫描

扫描范围为 `src/main/resources/aion/data/static_data/quest_definition/quests/*.xml` 的 metadata：

- 6238 个任务定义全部解析成功，XML 解析错误 0。
- metadata 中共 1925 条 `prerequisites/quest` 或 `type="finished"` 前置引用。
- 发现 27 条引用指向缺失目标，涉及 17 个目标任务；当前目标任务均不在生产 catalog，也不在旧版 `quest_data.xml`。
- 19054 已不再是命中项；它在修复前是 19055 唯一的缺失前置。
- 当前扫描没有发现“目标 XML 存在且 `min-level >= 999`”的引用；本次 19054 属于“缺失定义 + 用户确认 999 级不可达”。

| 缺失目标 | 引用任务与位置 |
|---:|---|
| 1036 | 1365 `prerequisites`；1467 `start-conditions` |
| 1062 | 1510、1517、1518 `prerequisites` |
| 1094 | 3102 `start-conditions` |
| 1099 | 19047 `start-conditions` |
| 2013 | 2217 `start-conditions` |
| 2035 | 2371 `start-conditions` |
| 2038 | 2486 `start-conditions` |
| 2039 | 2433 `start-conditions` |
| 2053 | 2697 `prerequisites` |
| 2055 | 2585、2586、2587、2588 `prerequisites` |
| 2092 | 4079 `prerequisites` |
| 2099 | 29047 `start-conditions` |
| 15352 | 3975 `start-conditions` |
| 18910 | 18911、18912、18913 `start-conditions` |
| 25352 | 4975 `start-conditions` |
| 28910 | 28911、28912、28913 `start-conditions` |
| 29054 | 29055 `start-conditions` |

### 处置边界

上述 17 个目标都是相似问题候选：若对应 NPC/升级入口可达，缺失前置会使 `StartEligible` 失败，随后可能落到通用对话回退并表现为页面 load fail。但当前 checkout 没有足够的客户端页面、旧 handler 或用户运行证据证明这些前置都应删除，因此全部标记为 `EVIDENCE_REQUIRED`，不做批量修改。后续应按“客户端任务页面/旧 handler/生产运行路径”逐任务确认；不能因为目标缺失就推断前置无效。

## 验证证据

- `xmllint --noout --schema src/main/resources/aion/data/static_data/quest_definition/quest_definition.xsd src/main/resources/aion/data/static_data/quest_definition/quests/19055.xml`：通过。
- 专项 Maven：
  `Quest19055ClientDialogAlignmentTest,QuestDialogOrderAuditTest,QuestDialogSequenceAuditTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest`：通过。
- 生产目录输出：`PRODUCTION_COMPILE_OK=6200`、`PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。
- 全量 `mvn -q test`：2995 个测试，39 failures、98 errors、2 skipped。19055 专项测试通过；失败集合主要包含 88 个 `Min Threads must be positive`/`AionConnection` 初始化错误、缺失 `./data/static_data/static_data.xml`、Java 25 static-final 反射限制、既有任务断言，以及当前工作区中其他任务的用户修改。全量红灯不能归因于 19055。
- 未启动、停止或重启服务；未进行客户端人工复测，故本记录不把 19055 标记为客户端验收完成。

## 工作区边界

本记录只观察并记录 19055 修复。当前工作区已有其他任务的已暂存/未暂存修改、`.agents/summary/quest/__pycache__/` 和 `../../../startup-20260824.jfr` 均未修改、未暂存、未提交。
