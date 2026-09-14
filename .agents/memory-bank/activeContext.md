# Active Context (当前攻坚上下文)

记录当前正在推进的任务与未解决的问题焦点。跨 Agent 接力时，先读此文件了解当前状态。

> last_updated: 2026-09-14
> status: ACTIVE
> scope: current checkout only
> owner: shared agents
> expires: focus changes, work is closed, or the recorded branch/worktree is replaced

---

## 当前攻坚焦点

- **主分支 / 工作分支**：`quest`
- **当前核心关注域**：
  - Quest 任务流转、状态机与 NPC 交互（Taloc's Hollow 副本实例脚本、Condition Spawns、Retail AI 判定）。
  - 跟随 NPC 寻路轨迹（Breadcrumb trail）与护送 AI 选型优化。

---

## 正在进行中的注意事项 (Next Steps & Gotchas)

1. **工作区未提交改动防御**：
   - 工作区存在并行任务与副本相关修改（如 `CelestiusAI2.java`, `KinquidAI2.java`, `RetailPatternAI2.java` 等）。
   - 动手修改代码前务必先执行 `git status`，确认当前改动范围，避免覆盖未提交的成果。
2. **任务排查标准流程**：
   - 检查任务 XML 节点时，对照 `docs/quest/` 维护文档及旧 Handler 行为。

## 交接规则 (Handoff Rules)

- 本文件只记录当前未完成事项，不承载已经验证的长期模式。
- 焦点变化时更新 `last_updated`、`status`、阻塞原因和下一检查点；完成后标记 `DONE` 或删除已解决条目。
- 分支名、未提交路径和运行环境属于当前 checkout 的上下文，不能直接升级为跨 checkout 的永久规则。
