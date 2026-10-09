---
alwaysApply: true
---

# AI 生成产物与文档管理 (AI-Generated Artifacts and Documentation Management)

## 1. 总结与沉淀由 Hindsight MCP 统一承载 (Knowledge Retention via Hindsight MCP)
1. 任务排查经验、根因分析、避坑指南与架构模式由 **Hindsight MCP**（Bank ID: `AionEmu`）统一承载与管理。
2. 任务完成后，所有经验沉淀、根因链与防错规则自动通过 Hindsight MCP 的 `sync_retain` 或 `retain` 接口记录（附带 `['summary', <topic>]` 或 `['pattern', <domain>, <ID>]` 等语义标签），无需在本地创建或维护总结文件。

## 2. 持久化文档统一落盘至 `docs/` (Persistent Documentation in `docs/`)
1. 凡是架构设计、演进规划（Plan）、重构方案、技术规范或需长期维护的参考文档需要持久化落盘时，**统一规范存储在 `docs/` 目录下**（例如 `docs/plans/`、`docs/architecture/`、`docs/quest/` 等对应领域子目录）。
2. 持久化落盘文档遵循清晰的 Markdown 结构与中英双语规范，具备明确的背景、设计决策、实施步骤与版本信息。

## 3. 临时中间产物即用即清 (Ephemeral Intermediate Artifacts)
1. AI 生成的一次性分析脚本、探针代码、数据提取工具、临时测试输出及草稿等，均属于瞬时中间产物。
2. **严禁将临时产物放置于 `scripts/`、`src/`、仓库根目录或生产源码路径中**。临时脚本绝不可混入生产维护脚本目录 `scripts/`。
3. 任务完成后，智能体必须**自动清理并彻底删除**为该任务创建的所有临时文件及一次性脚本，绝不可残留在 Git 工作树中。
4. 仅在用户明确要求将某个辅助工具提升为项目长期维护的正式工具时，方可将其移动至 `scripts/` 或相关正式源码目录。

## 4. 临时 Git Worktree 纪律 (Worktree Discipline)
1. 临时 git worktree 属于瞬时验证环境：仅在主工作树无法执行必要验证时（例如并行任务导致生产目录构建阻塞）才可创建。
2. 经允许创建时，务必将其置于仓库外部的临时路径下，仅用于单次只读/验证；**严禁在 worktree 中提交代码**。
3. 验证完毕后必须立即使用 `git worktree remove --force <path>` 及 `git worktree prune` 彻底清理，绝不可残留 worktree、其 `target/` 构建输出或 `git worktree list` 中的陈旧条目；在其中运行测试或打包前必须先征得用户的构建授权。

## 5. 诊断产物隔离纪律 (Diagnostic Dump Discipline)
1. 每次运行的诊断产物——JFR 记录文件（`*.jfr`）、堆/线程转储（dump）、GC 日志、导出日志等——均属于临时产物：切勿提交它们，亦不可将其具体文件名或路径硬编码写入代码、注释、文档或记忆库证据中。
2. 应记录采集参数（参数标志/执行命令），并使用泛化代称（例如“启动期 JFR 临时采样”或 `/tmp/play-N.jfr` 风格的占位符）。确保相关文件模式已列入 `.gitignore`。
