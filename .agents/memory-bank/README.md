# Memory Bank (跨 Agent 持久化记忆库)

本目录是 AionEmu 项目跨会话、跨工具共享的持久化记忆与避坑体系。它保存的是经过提炼、可复用的知识，不是每次任务的流水账。

## 1. 信息源边界 (Sources of Truth)

| 位置 | 负责内容 | 写入时机 | 默认生命周期 |
|---|---|---|---|
| `.agents/summary/<topic>/` | 单次任务的原始证据、审计结果、日志、脚本和中间产物 | 任务进行中或需要保留复现证据时 | 任务级；临时产物完成后清理 |
| `.agents/memory-bank/patterns/` | 已提炼的跨任务模式、根因和防错规则 | 发现可复用的根因或修复契约时 | 长期；必须可追溯、可失效 |
| `.agents/memory-bank/systemPatterns.md` | 模式路由和跨域不变量的短摘要 | 新增跨域规则或模式 ID 时 | 长期；只做导航，不做案例账本 |
| `docs/quest/` | Quest Playbook、代表案例和验收规则 | 已接受的 Quest 模式需要纳入维护文档时 | 项目文档；按 Playbook 规则维护 |
| `.agents/memory-bank/activeContext.md` | 当前工作区的未完成焦点、阻塞项和交接信息 | 焦点变化、阻塞变化或交接时 | 短期；完成或过期后清空/归档 |
| `.agents/memory-bank/archive/` | 已被替代或不再参与日常排查的历史记录 | 条目明确关闭且不再是日常规则时 | 只读备查；默认不参与路由 |

单次任务不要直接升级为永久规则；永久规则也不要反向承载完整的原始审计输出。Quest 的客户端/运行时验收仍以 `docs/quest/` 和 `summary` 中的结构化证据为准，memory-bank 只保存可复用结论。

## 2. 目录结构与读取顺序 (Layout and Read Order)

```text
.agents/memory-bank/
├── README.md
├── systemPatterns.md              ← 顶级路由，只保留短摘要
├── activeContext.md               ← 当前工作区上下文，可过期
├── patterns/                      ← 带 Pattern ID 的长期模式卡片
│   ├── quest-engine.md
│   ├── static-data-jaxb.md
│   ├── architecture-runtime.md
│   └── build-and-env.md
└── archive/                       ← 已关闭/被替代的历史记录
```

排查疑难问题或修改核心模块时按以下顺序读取：

1. 先检查 `git status` 和当前运行/工作区范围。
2. 读取 `systemPatterns.md`，确定候选领域和 Pattern ID。
3. 读取对应的 `patterns/<module>.md`，核对适用范围、证据和边界。
4. Quest 问题继续读取 `docs/quest/`；单次证据按需要读取 `.agents/summary/<topic>/`。
5. 只有当前未完成事项才写入 `activeContext.md`。

## 3. Pattern 卡片最低格式 (Minimum Pattern Schema)

新增或实质修改模式时，使用稳定的 Pattern ID，并至少包含以下字段：

```text
## [DOMAIN-NNN] 模式名称
- status: CONFIRMED | PROVISIONAL | SUPERSEDED
- scope: 适用模块、运行模式、数据版本或 checkout 范围
- first_seen: YYYY-MM-DD 或 unknown
- last_verified: YYYY-MM-DD 或 unknown
- symptom: 可检索的现象和关键词
- root_cause: 已证实的根因；推测必须标为 PROVISIONAL
- fix_or_guardrail: 修复方式或禁止事项
- evidence: commit、仓库相对 file:line、测试、日志/协议/客户端证据
- validation: static | focused-test | production-gate | runtime | client；逐项写结果
- boundaries: 不适用场景、未验证分支和剩余风险
- superseded_by: 新 Pattern ID 或 none
```

状态含义：

- `CONFIRMED` 才能作为稳定规则被总控路由；必须有可回溯证据。
- `PROVISIONAL` 只能辅助排查，不能单独作为 Quest 验收或全局删除依据。
- `SUPERSEDED` 必须链接替代模式，不能静默删除历史原因。

现有卡片按历史内容保留，不做一次性大规模重写；下次触碰某条模式时补齐该条的字段和证据。

## 4. 沉淀决策表 (Promotion Decision)

| 结果类型 | 应写入的位置 | 不应写入的位置 |
|---|---|---|
| 一次性调查、原始日志、候选清单 | `.agents/summary/<topic>/` | `patterns/` |
| 已复现且可复用的根因/修复契约 | 对应 `patterns/`，分配 Pattern ID | `systemPatterns` 详细展开 |
| 影响多个领域的新不变量 | `patterns/` + `systemPatterns` 一行路由 | 只写在某个任务 summary |
| 已接受的 Quest 代表性案例 | `docs/quest/repair-playbook/`，按现有规则去重 | 每个 Quest ID 都新增一份模式 |
| 当前未完成、阻塞或待用户验证事项 | `activeContext.md` | 长期 `patterns/` |
| 已关闭且被替代的历史方案 | `archive/`，附替代链接 | 删除而不留出处 |

完成非平凡任务时，先保留证据，再判断是否形成模式；只有确实产生新跨域不变量时才改 `systemPatterns.md`。这避免每个 Bug 都造成总控文件和长期卡片噪声增长。

## 5. 并行协作与归档 (Concurrency and Archive)

- 每个任务使用独立的 `.agents/summary/<topic>/`，避免共享临时文件名。
- 领域卡片采用追加新段落的方式；不要重写已经引用的历史证据。
- `systemPatterns.md` 只追加/调整 Pattern ID 和一行摘要；详细信息留在卡片。
- 归档条目必须说明 `status`、原始来源、`last_verified`、替代条目和是否仍需人工读取。
- 不因本规则变化自动恢复、迁移或删除既有 summary 文件；只处理当前任务明确产生的中间产物。
