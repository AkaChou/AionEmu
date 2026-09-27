# 迁移现场清理：漂移/快照/转储产物退役（2026-09-27）

> 车道：`quest-native-dispatch`；触发：用户裁定「提交的 txt/py/tsv 里有些是**漂移的产物**——
> 保留对真端改造有用的内容，删除无效无用的内容」。
> 上游提交：`5318a8686`（台账/工具链/证据入库）+ `4ede058c0`（源码迁移）。
> 清理提交：见本目录随附提交（`chore(agents): …`）。
> 纪律：未 commit 前逐项列清单；删除对象**全部来自本次入库的 `5318a8686`**，
> 不触碰更早入库的历史车道产物（`ai-artifacts.md` 规则 6：不得删除既有/无关产物）。

## 1. 判据（可复核）

删除条件 = **全部满足**：

1. 文件由 `5318a8686` 入库（即本车道/兄弟车道近两日产物）；
2. **零引用**：全仓（`git ls-files` + 未跟踪，排除 `.git/target/aion`）按**精确文件名** grep
   **零命中**——既无活脚本/测试消费，也无台账引用；
3. 属**瞬态族**：漂移/指纹/等价转储、`-pre`/`-post` 快照、`.new`/`-fresh`、census/dump/diff、
   probe 输出、selector/runner 输出、调试/草稿/单例快照、零字节文件、一次性 id 清单与台账改写器。

保留条件（任一）：被任何文件按名引用；`.md` 台账；`-reds.txt` 红身份集基线；
决策表/登记表（`*-decisions*`、`*-registry*`、`retention` 等）；生产表格与生成器脚本；探针源码（`.java.txt`）。

## 2. 结果

| 类别 | 个数 | 说明 |
|---|---|---|
| 漂移/指纹/等价转储、`-pre`/`-post` 快照、census/dump/diff | 156 | 如 `p0c58-manifest-pre.tsv`、`dd-drift-*.tsv`、`*-ir-pre.txt`、`m5b2b-*-census` 外的单例转储 |
| 调试/草稿/单例快照 | 26 | `p0c10n-debug*.tsv`、`*-draft.tsv`、`*-classification-after*.tsv`、`tmp-*` |
| 零字节文件 | 16 | 空的 `T*-*-added.txt` / `-removed.txt`（ADDED 0 / REMOVED 0 的产物） |
| probe 输出（`probe/` 目录） | 5 | `probe-1101.txt` 等早期探针输出 |
| 一次性 id 清单 | 3 | `p0c9-*-ids.txt`、`p0c10n-cutscene-ids.txt` |
| 一次性台账改写器（`.py`） | 2 | `p0c55_ledger_update{2,3}.py`（已执行完毕的文档改写脚本） |
| **合计（已入库删除）** | **208** | 6.85 MB；逐条清单位于本目录 `DELETED-2026-09-27.tsv` |
| **未跟踪原始转储删除** | **5** | 35.5 MB：`p0c55-audit-all-{pre,post}.txt`、`p0c8c-gap-shape-census{,-full}.tsv`、`definitions-dump.txt` |

**保留（未删，判定为对真端改造有用）**：全部 `.md` 台账；`gates/*-reds.txt` 红身份集基线；
`m5b2b-*` 家族交叉表与 `m5b2b_*.py` 探针脚本；`.java.txt` 探针源码；`*-decisions*`/`*-registry*`/`retention`
决策与登记表；`build_*`/`retire_*`/`refreeze_*`/`insert_fp_*`/`update_drift_*` 等生成器；
4 个被活脚本消费的未跟踪输入表（`item_name_index.tsv`、`npc_name_index.tsv`、`quest_registry.tsv`、
`m5b2b-quest-event-census.tsv`——`build_retention_list.py` / `p0c11_*` / `p0c43_*` 等直接读取，**不得删**）。

**未处置（留待裁定）**：`gates/**/*.log`（本车道 14 MB + 兄弟车道 31 MB，共 46 MB）——
`.gitignore` 已排除故从未入库；其中 `T3-w5g34.log` 被交接包 §4 引用为 T3 证据路径，故未一并清理。

## 3. 恢复路径

删除内容全部来自 `5318a8686`（已入库）⇒ 逐字恢复：

```bash
git checkout 5318a8686 -- <path>          # 单文件
git archive 5318a8686 .agents/summary/scriptdll-quest-driver | tar -x   # 整目录
```
