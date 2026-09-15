---
alwaysApply: true
---

# AI-Generated Intermediate Artifacts / AI 生成中间产物

1. AI-generated intermediate artifacts that are not part of the product source, including temporary or one-off scripts, audit and conversion tools, generated reports, logs, exports, patch drafts, and scratch data, must be created under `.agents/summary/<topic>/`.
2. Use a descriptive, stable `<topic>` directory and keep all artifacts for the same task together. Create the topic directory when it does not already exist.
3. Do not create these artifacts in `../../scripts`, the repository root, `../../src`, `../../docs`, `../../target`, `../../aion`, or another production path. In particular, an AI-generated temporary script must not be placed under `../../scripts`.
4. Only move a tool to `../../scripts` or another project-owned source directory when the user explicitly asks to promote it to maintained project tooling; otherwise, keep it under the relevant `.agents/summary/<topic>/` directory.
5. Existing project-owned scripts and historical artifacts do not need to be moved solely because of this rule. Apply the rule to new AI-generated intermediate artifacts unless the task explicitly includes migration.
6. When the task is complete, automatically remove all AI-generated intermediate files and temporary or one-off scripts created for that task, including the task's `.agents/summary/<topic>/` directory when it is empty. Preserve only final deliverables, files the user explicitly asks to retain, or tools explicitly promoted to maintained project tooling; do not delete pre-existing or unrelated artifacts.
7. A retained summary artifact must have a stable task-relative name and be referenced by the task record or acceptance document; do not promote raw logs, generated reports, or one-off scripts into `memory-bank/patterns/` without distilling the reusable rule and its evidence.
8. Temporary git worktrees are intermediate artifacts: create one only when the main working tree cannot run the required verification (for example, a parallel task blocks the production catalog build), keep it under a temporary path outside the repository, and remove it immediately after use with `git worktree remove --force <path>` plus `git worktree prune`. Never leave a worktree, its `target/` build output, or a stale entry in `git worktree list` behind, and never commit from a worktree; ask the user for build authorization before running tests or packaging in it.
