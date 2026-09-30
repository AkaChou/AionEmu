# 真端模板表 <-> 本仓库 typed quest XML 对账（只读基线 + PREREQ 批次）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-22；性质：对账为只读；PREREQ 批次已改 25 个生产 XML（未跑构建）。
- 真端源：`<真端根>/Map/XML/`（UTF-16LE，5.8 服务端数据）
- 本仓库：`src/main/resources/aion/data/static_data/quest/definitions/quests/*.xml`（6224 个定义）
- 对账脚本：`reconcile_retail_templates.py`；逐任务结果：`reconciliation.tsv`

## 1. 总量（修正抽取器后）

- 本仓库定义：6224
- 命中真端模板表（8 张模板 + data_driven）：5554
- 无模板（真端为 per-quest 脚本，无法对账）：670
- 模板覆盖内 ALIGNED：**4570/5554（82%）**

差异类型（任务级计数，可重叠）：STEPS 704、TARGETS 188、ACQUIRE 115、REWARD 71、PREREQ 48。

## 2. 模板 × 差异

| 模板 | 任务数 | ALIGNED | 主要差异（项:数量） |
|---|---:|---:|---|
| CombineTask | 574 | 574 | - |
| SimpleTalk | 2223 | 2130 | ACQUIRE:35, REWARD_ACQUIRE:26, PREREQ:21, REWARD:8 |
| DataDriven | 1508 | 872 | STEPS:452, TARGETS:89, TARGETS_STEPS:37, ACQUIRE:20 |
| SimpleHunt | 942 | 710 | STEPS:180, TARGETS_STEPS:22, TARGETS:13, PREREQ:6 |
| SimpleCollectItem | 178 | 171 | REWARD:4, ACQUIRE:1, TARGETS:1, PREREQ:1 |
| SimpleUseItem | 104 | 94 | REWARD:8, ACQUIRE:2 |
| SimpleItemPlay | 15 | 15 | - |
| SimpleSerialHunt | 10 | 4 | PREREQ:4, TARGETS:1, STEPS:1 |

## 3. 差异清单（按置信度）

### 3.1 PREREQ（48 个 → 已分类，批次执行见 §4）
- 文件：`prereq-mismatch.tsv`（粗口径）、`prereq-semantic-audit.tsv`（语义口径）、`prereq-reachable-audit.tsv`（可达分支口径）
- 真端权威：真端 `quest.xml` 的 `finished_quest_condN`；本仓库口径 = `<metadata><prerequisites>` + `<start-conditions>` + `<start-condition-groups>` 的 finished 并集。
- 语义统一：每个 `finished_quest_condN` 是一个 OR 分支，分支内逗号是 AND（QE-021）；未移植任务分支按仓库既有策略不声明为悬空前置（见 §4.3）。

### 3.2 TARGETS（188 个，中等置信）
- 文件：`targets-mismatch.tsv`；真端 monster/area/talk/item 名称解析出的 NPC/物品 id 未出现在本仓库该任务的 `npc-id`/`item-id` 引用里（同名多 id 已用「任一命中即通过」）。

### 3.3 REWARD / ACQUIRE（71 / 115 个，中等置信）
- 文件：`owner-mismatch.tsv`；真端 `reward_npc_name`/`acquired_npc_name`/`talk_npcN` 解析 id 未命中本仓库 `npc-complete`/`dialog`/`npc-id` 集合，与 QE-052 owner 收敛同族。

### 3.4 STEPS（704 个，低置信）
- 文件：`steps-mismatch.tsv`；步数口径未统一（counter-grid 维度求和 vs 真端 countN），只能当候选。

## 4. PREREQ 批次（2026-09-22 执行）

### 4.1 三重证据
逐条比对：真端服务端 `quest.xml`、Aion 5.8 客户端 `Quest_unpacked/quest.xml`、仓库内旧库 `quest_data.xml`（QE-001 权威）。

### 4.2 已应用（25 个文件，均只补/改单一前置）
- 补前置（24）：2533←2532、3050←3049、15471←15402、15551←15550、15552←15551、15553←15552、15554←15553、15563←15550、15595←15550、15673←15550、16823←16822、16824←16821、16825←16822、18035←18036、18821←18830、18993←18992、21004←21001、21080←21065、21201←21200、26823←26822、28035←28036、30719←30708、49004←49003、80343←80341
- 修正迁移漂移（1）：2641 的 `start-conditions finished 2640` → `<prerequisites>2619`（真端/客户端/quest_data 三源一致）
- 脚本：`apply_prereq_batch.py`（可复跑，幂等保护：已存在前置时中止）

### 4.3 刻意不改（等价或阻塞）
- 25 个任务（1365/1510/1517/1518/2217/2371/2433/2486/2585..2588/2697/3102/3975/4079/4975/18911..18913/19047/28911..28913/29047）真端的另一分支指向未移植任务（1036/1062/1094/1099/2013/2035/2038/2039/2053/2055/2092/2099/15352/18910/25352/28910），仓库保留可达到支即语义等价（与 2026-09-17 `043426b47` 的清理策略一致）。
- 3 个阻塞（1870←1868、2869←2868、2870←2868）：依赖任务未移植，补前置会把任务永久锁死 → 保持 fail-open 并登记，待移植后自动转强制（门禁已内置）。

### 4.4 新增家族级门禁
- 基线：`src/test/resources/quest/quest-prerequisite-retail-contract.tsv`（2611 行，真端 DNF 快照；生成脚本 `.agents/summary/retail-template-reconciliation/build_prereq_contract_tsv.py`）
- 测试：`src/test/java/com/aionemu/gameserver/questEngine/definition/QuestPrerequisiteRetailContractTest.java`
  - 可达分支必须被目录表达（本批覆盖 1734 个分支，跳过 31 个未移植分支）
  - 本批 25 行精确锁定（含 2641 不得回退到 2640）
  - 未移植单分支链保持 fail-open，移植后必须补齐

### 4.5 静态验证（已完成）
- `xmllint --schema quest_definition.xsd`：25/25 通过（负向对照可判失败）
- 语义审计：equivalent 1578、stricter 67、looser 3（修复前 looser 27 含本批 25）
- 前置图：6224 任务、0 自环、0 悬空引用、0 环路（QE-023 口径）
- 门禁离线模拟：0 违规；反事实（用 HEAD 版本重算）恰好 25 违规
- **未执行**：Maven/测试（未授权）
- 本目录 TSV 为**改动前基线快照**（2026-09-22 22:10）；重跑同目录脚本会输出改动后的数值（`looser` 27→3、ALIGNED 上升），脚本写入已去行尾空白、可重复生成。

## 5. 后续批次建议

1. **PREREQ-STRICTER（67 个）**：仓库比真端更严。其中 10507/20507/21296 为真端未声明的多余前置（21296 会挡住只完成 21295 的玩家），其余为既有独占网（15311..15316、16805..16808、19008..、80060.. 等）需要单独定性。
2. REWARD/ACQUIRE（71/115）：与 QE-052 owner 收敛合并处理。
3. TARGETS（188）：先抽样 10~20 条确认真端 instance/阵营变体，再决定成批。
4. STEPS（704）：先定义统一口径（kill/collect/counter-grid 维度是乘积还是求和），再升级为门禁。
