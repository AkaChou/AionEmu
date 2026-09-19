# Memory Bank AI 检索入口（机读索引）2026-09-19

## 目标

把 `.agents/memory-bank` 从「人工阅读的领域卡片」升级为「AI 可按需检索的知识库」：不改 Pattern 正文、不建第二事实源，只补一层由元数据派生的机读索引与检索入口。

## 变更

| 文件 | 变更 |
|---|---|
| `.agents/memory-bank/memory_bank.py` | 新增 `section_heading` / `section_sha256` / `entry_record` / `render_index_jsonl`，统一机读记录 schema |
| `.agents/memory-bank/sync_memory_bank.py` | 派生集合由一个变为两个：`symptom-index.md` + `index.jsonl`；`--check` 同时校验两者是否过期 |
| `.agents/memory-bank/search_memory_bank.py` | 新增 `--json`、`--id <PATTERN_ID>`、`--status`；默认文本输出保持不变 |
| `.agents/memory-bank/index.jsonl` | 新增派生索引（首行 meta，其后每个 Pattern 一行） |
| `.agents/memory-bank/README.md` | 目录树、读取顺序，新增第 8 节「机器可读检索入口」 |
| `AGENTS.md` | Memory Bank 段落新增机器可读检索入口一行 |

## 接口契约

| 用途 | 命令 |
|---|---|
| 关键词检索（机读） | `python3 .agents/memory-bank/search_memory_bank.py "<现象 关键词>" --json --limit 5` |
| 单条展开（只取一段） | `python3 .agents/memory-bank/search_memory_bank.py --id QE-013 --json` |
| 状态过滤 | 追加 `--status CONFIRMED` |
| 全库索引 | `.agents/memory-bank/index.jsonl`（`record=meta` 首行 + `record=pattern` 行） |

`index.jsonl` 每条记录字段 = Pattern 元数据块字段 + `card`、`line`、`end_line`、`section_bytes`、`section_sha256`、`related`；用于过滤、排序与引用，正文按需用 `--id` 展开。

## 验证（已执行）

- `python3 -B .agents/memory-bank/sync_memory_bank.py --check` → `MEMORY_BANK_SYNC_OK ENTRIES=80`
- `python3 -B .agents/memory-bank/check_memory_bank.py` → `MEMORY_BANK_OK ROUTER_IDS=80 PATTERNS=80 SYMPTOM_INDEX=80 LINKS=23 EVIDENCE_REFS=409 ARCHIVE_ENTRIES=1`
- 检索模式实测：默认文本输出未变；`--json` 可解析；`--id` 大小写不敏感并返回单条正文；未知 ID、非法 `--status`、无参数均以退出码 1 失败
- `index.jsonl` 逐行解析：1 条 meta + 80 条 pattern，Pattern ID 唯一，必填字段无缺失

## 未执行边界

- 未运行 Maven 构建或 Java 测试：本次只改 `.agents` 工具链、AGENTS.md 与派生索引，不涉及 Java/资源产物。
- 未提交：等待显式提交指令。工作树中 `patterns/architecture-runtime.md`、`symptom-index.md`、`systemPatterns.md` 的改动属于并行任务，不在本次范围内。

## 后续可选（未实施）

- 老化报告：按 `last_verified` 生成 CONFIRMED 超期清单。
- 让 `.agents/summary/<topic>/` 的保留文档带固定 front-matter 后并入同一索引。
- 若检索需要更细粒度，再把单条 Pattern 拆分为独立文件（当前用 `--id` 已可避免整篇读入）。

## 检索盲检（P1）结果 2026-09-19

工具：`.agents/summary/ai-kb-index/retrieval_eval.py`（从 Pattern `evidence` 引用的 summary 文档构造查询，去掉路径、topic 名、Pattern ID、commit hash 后调用 `search_memory_bank.py --json`）。
报告：`retrieval-eval-2026-09-19.zh-CN.md` / `.json`。

口径：一份证据被多个 Pattern 引用时命中任意一个算成功；没有「症状/现象/问题/Symptom」章节的文档（16 份验收记录、量化记录）标为低信号并剔除。

| 指标 | 数值 |
|---|---|
| 有效查询 | 23 |
| Top-1 | 18/23（78%） |
| Top-3 | 18/23（78%） |
| Top-5 | 19/23（83%） |
| MRR | 0.791 |

5 条未命中及归因：

- `QE-007`：证据文档用「可选工作物品/收集物/阻断」，卡片 `symptom` 写作「多选一交付后领奖卡死，removalFeasible 为 BLOCKED」——**元数据词汇缺口**。
- `QE-008`：证据文档用「不可达接取前置/finished quest-id」，卡片写作「前置任务为 999 级或不存在」——**元数据词汇缺口**。
- `AR-001`、`AR-011`：命中的查询来自「改造/量化记录」类文档，标题描述的是改动而不是症状——**查询侧噪声**（这类文档应视作低信号）。
- `AR-006`（与 `AR-008` 共用一份 JFR 文档）：同一文档承载多个结论，抽出的段落对应另一条 Pattern——**一文档多结论的固有歧义**。

结论：卡片元数据整体可用（症状型证据文档 Top-1 78%），主要缺口是少数 `symptom` 只写术语、没写用户/日志原话。

## 改造：可选 `keywords:` 字段（2026-09-19）

已实施：

| 位置 | 变更 |
|---|---|
| `memory_bank.py` | 新增 `OPTIONAL_FIELDS = ("keywords",)` 与 `OPTIONAL_FIELD_LIMIT = 300`；`entry_record` / `index.jsonl` 输出该字段 |
| `check_memory_bank.py` | 可选字段存在时校验长度上限，缺失不算错误 |
| `search_memory_bank.py` | `keywords` 加入 `HEADLINE_FIELDS`，与 `symptom` 同级加权 |
| `README.md` §3 | 记录可选字段用法 |
| `patterns/quest-engine.md`、`patterns/architecture-runtime.md` | 回填 6 张命中落空卡片：QE-007、QE-008、AR-001、AR-006、AR-008、AR-011 |

## 改造后复测（同口径对比）

### 1. 证据文档盲检（`retrieval-eval-2026-09-19*`）

| 指标 | 改造前 | 改造后 |
|---|---|---|
| 有效查询 | 23 | 23 |
| Top-1 | 18（78%） | 23（100%） |
| Top-3 | 18（78%） | 23（100%） |
| Top-5 | 19（83%） | 23（100%） |
| MRR | 0.791 | 1.000 |

变动明细：QE-007（miss→1）、QE-008（5→1）、AR-001（miss→1）、AR-006/AR-008（miss→1）、AR-011（miss→1）。
**该组数字偏高**：查询与 keywords 同源，只能证明「别名字段被正确检索」，不能代表真实召回上限。

### 2. 手写留出集（`retrieval-eval-2026-09-19-holdout*`，15 条用户口吻查询）

| 指标 | 改造前 | 改造后 |
|---|---|---|
| Top-1 | 13/15（87%） | 13/15（87%） |
| Top-3 | 14/15（93%） | 15/15（100%） |
| MRR | 0.900 | 0.922 |

对照组是把工作树卡片的 `keywords:` 行剥离后复制到临时目录重跑（未改动仓库文件）。
结论：留出集上改造前已能用，`keywords` 把 QE-007 从 miss 提升到 Top-3，且 **没有任何查询排名下降**（无回归）。

### 3. 复现命令

```bash
python3 -B .agents/memory-bank/sync_memory_bank.py --check
python3 -B .agents/memory-bank/check_memory_bank.py
python3 -B .agents/summary/ai-kb-index/retrieval_eval.py --label keywords
python3 -B .agents/summary/ai-kb-index/holdout_eval.py
```

结论：可选 `keywords` 字段在本仓库是低风险、可验证的召回手段；后续在触碰某条 Pattern 时按同一格式补别名即可，无需一次性重写全部卡片。

## 第二批改造：证据索引 / 统一门禁（2026-09-19）

### 1. 证据索引 `.agents/summary/index.jsonl`

由 `sync_memory_bank.py` 作为第三个派生文件生成，`--check` 一并校验新鲜度。每行一个被 Pattern `evidence:` 引用且真实存在的文档：

| 字段 | 说明 |
|---|---|
| `path` / `topic` | 仓库相对路径与主题目录 |
| `kind` | `topic-readme` / `acceptance` / `audit` / `evidence` / `report` / `other`（按文件名判定） |
| `date` / `title` / `lines` / `bytes` / `sha256` | 文件名日期前缀、首个 H1、规模与内容哈希 |
| `referenced_by` | 引用该文档的 Pattern ID 列表 |

当前：40 篇被引用文档，`missing` 为空；分布 `report` 20、`acceptance` 8、`audit` 5、`topic-readme` 5、`other` 2。
设计取舍：只登记被 Pattern 引用的文档，索引随卡片变化，不会因为任务目录增加而要求重新生成；未被引用的 summary 不进入索引。

### 2. 统一门禁 `verify_memory_bank.py`

一条命令跑完全部校验：派生索引新鲜度（`sync_memory_bank.py --check`）→ 结构校验（`check_memory_bank.py`）→ 时效门禁（`memory_bank_stats.py --fail-over-days 90`）。
新增统计工具 `memory_bank_stats.py`：`--json` 输出状态/领域/验证方式/证据引用/时效，`--fail-over-days N` 超期即退出码 1，
`--today` 支持可复现运行。`AGENTS.md` 的自动沉淀协议已改为「先 sync，再 verify」。

当前快照（2026-09-19，81 条）：CONFIRMED 78 / PROVISIONAL 3；领域 quest-engine 43、architecture-runtime 13、instance-runtime 11、ai-movement 6、build-and-env 5、static-data-jaxb 3；验证方式 static 30、focused-test 32、runtime 26、client 27、production-gate 9；证据引用 summary 42、commit 34、源码 81；`keywords` 6/81；时效 unknown 0 / fresh 81 / aging 0 / stale 0（最旧 5 天）。

### 状态

- 已提交（前一批）：`5bf141659 docs(memory-bank): add AI retrieval index and search entrypoints`。
- 本批（证据索引 + 统计 + 门禁 + 文档）位于工作树，等待提交指令；未运行 Maven/服务器/客户端验证。
- 工作树中 `quests/10503.xml`、`.agents/summary/quest-10503/`、`Quest10503ClientDialogAlignmentTest.java` 属于并行任务，未触碰。

### 3. 未采用：MCP 服务（2026-09-19 决定不做）

曾实现标准库版 stdio MCP 服务（`memory_bank_search` / `memory_bank_get` / `memory_bank_stats`）并通过冒烟测试，随后按用户决定删除，文件未进入提交。原因：

- CLI 已覆盖同样能力，Agent 直接用 shell 调用即可，无需客户端配置、常驻进程或额外故障面；
- MCP 只暴露 Pattern 层：summary 正文、`docs/` 与源码仍要另走路径，若 Agent 只依赖它会**缩小**可搜范围；
- 仓库已有 `jbcontext search` / CodeGraph / grep 覆盖代码与文档层，不必再多一个需要同步维护的入口。
