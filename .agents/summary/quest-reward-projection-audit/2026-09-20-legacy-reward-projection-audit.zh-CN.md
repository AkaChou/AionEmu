# 旧 handler 领奖 packed step 投影全库审计（15300/25300 后续）

- 日期：2026-09-20
- 触发：用户确认 15300 客户端验收通过后要求「反思为什么领奖路线缺失修过多次仍未发现，排查类似问题，防止再次发生」。
- 状态：审计完成，**未批量修改 XML**（决策规则见第 4 节）；脚本与逐任务报告留在本目录。
- 关联证据：`.agents/summary/quest-15300-reward/2026-09-19-reward-projection.zh-CN.md`、`docs/quest/repair-playbook/CASES.zh-CN.md` 案例 8.18、memory-bank `QE-045`。

## 1. 方法（可复现）

```bash
python3 .agents/summary/quest-reward-projection-audit/audit_legacy_reward_projection.py
```

- 语料：`git log --all --diff-filter=D` 找出历史上被迁移删除的全部 `quest/handlers/**` Java 文件（2358 个），用 `git cat-file --batch` 批量读取每个文件删除前最后一次内容。
- 提取：文件名/`questId` 常量定位任务号；正则 `changeQuestStep(env, from, to, true)` 取旧 handler 进入 `REWARD` 前后的 packed step。
- 比对：解析当前 `src/main/resources/aion/data/static_data/quest_definition/quests/<id>.xml` 的 `reward` 节点投影 `var0`、`START -> REWARD` 交接是否 `set-variable var0`、以及是否存在无 `source` 的 `enter-world` 恢复边（`status-is REWARD` + `variable-is var0`）。
- 输出：`report.tsv`（490 行逐任务判定）+ stdout 汇总。

## 2. 审计结果

| 判定 | 数量 | 含义 |
| --- | --- | --- |
| `MISMATCH_PROJECTION` | 106 | reward 投影 `var0` 不等于旧 handler 进入 `REWARD` 前的 packed step |
| `MISSING_RECOVERY_EDGE` | 32 | 投影正确，但没有纠正已落盘错位存档的恢复边 |
| `SELF_REWARD_NO_STEP` | 288 | 旧 handler 用 `changeQuestStep(env, X, X, true)`（奖励即当前步），需要单独判定，不是同一形状 |
| `NO_XML` | 62 | 旧 handler 有领奖分支但当前没有对应 XML（尚未迁移或另有实现） |
| `OK` | 2 | 完全匹配本次合同 |
| 跳过（多 `from/to` 分支） | 1 | `_2430SecretInformation`，需人工判读 |

典型批次（`MISMATCH_PROJECTION`）：`154xx` redemption_landing 家族 40+ 个任务（旧 handler `from=5/to=6`，XML 一律投影 `var0=1`）、`15301/15302/15303/15305` 与魔族镜像 `25301/25302/25303/25305`（旧 handler `from=2/to=3`，XML 投影 `var0=0`）、`13950/23950`、`13953/23953`、`13956/23956`、`26800`、`30709/30759`、`50128/51128` 等。完整清单见 `report.tsv`。

## 3. 反思：为什么领奖投影错位"修过好几次"仍然漏掉 15300

1. **修复对象来自人工上报，不是规则推导。** `f6aff952a`（2026-09-09）修的是当时已经出现症状并被逐个定位的 22 个任务，落地形式是硬编码 22 个 ID 的回归锁（`LegacyRewardStepProjectionRegressionTest`）。它不是"扫描全部旧 handler"的规则门禁，所以任何不在名单里的任务都天然通过。
2. **缺陷在修复时已经存在于仓库里。** `cfc2fa048`（2026-08-21）时 `15300.xml` 的 `reward` 节点就已经是 `var0=14`，`15300/25300` 只是不在那 22 个 ID 里；也就是说 2026-09-09 那轮修复面前就摆着这个实例，但没有入口去发现它。
3. **迁移工具链没有这一项检查。** 2026-08-04 的 1500 任务迁移、2026-08-05 的 evergale/high-daevanion 迁移、redemption_landing 批次迁移都可以重复引入同一错位；Playbook 8.18 只写了适用范围（"仅适用于 legacy 明确进入 REWARD 但没有改 packed var 的任务"），没有可执行入口，后续 Agent 只能靠记忆套用。
4. **症状只在单任务的领奖阶段出现。** 共享引擎（`QuestMutationPlanner`、`SM_QUEST_ACTION`）逻辑正确，静态门禁、目录编译、白名单全部为绿；错误数据只体现在某一个任务的 packed step 上，只有玩家跑到该任务领奖时才会暴露，检出率天然很低。
5. **旧存档恢复边被当成"可选补充"。** 首轮修复只改投影不改旧存档时，已经落盘的 `REWARD var0=14` 仍然空白（15300 本次就是靠新增 `enter-world` 恢复边才让旧存档自愈），说明"投影 + 恢复边"必须作为一个合同整体交付。

## 4. 判定规则与未决问题（本次不同步批量修改的原因）

- **规则 A（项目既有合同，Playbook 8.18）**：`reward` 节点投影必须等于旧 handler 进入 `REWARD` 前的 packed step；`START -> REWARD` 交接不得再写该字段；必须补 `REWARD && var0 == to` 的无 source `enter-world` 恢复边。15300/25300 按规则 A 修复并已客户端验收。
- **规则 B（客户端行号候选）**：Aion 5.8 客户端 `Dialogs/*/quest_q<id>.html` 的 `<HtmlPage name="quest_summary">` 内 `<steps>/<step>` 是任务书行清单。实测 15300 有 15 行，最后一行是第 0 行的重复行；报告行 = 13，与规则 A 的 pre-step 相同。
- **未决点**：两条规则在部分任务上结论不同。例如 `15304` 客户端报告行（"向 Dike 报告"）= 4，而旧 handler pre-step = 3；`26800`（客户端已验收）客户端报告行 = 2 = pre-step，但当前 XML 投影 = 3。两者只能由一次客户端观测区分：15300 修复后在 `REWARD` 时任务书显示的是第 13 行（"和 Dike 见面，移动到主神的安息处后和 Kaisinel 对话"）还是第 14 行（"在希哥尼亚和代理人 Dike 对话"）。
- 因此本轮**只修客户端已验收的 15300/25300**，其余进入第 5 节清单，禁止机械批量套用规则 A；对同一 `from/to` 形状但客户端报告行与 pre-step 不一致的任务（如 15304），必须先按客户端 `quest_summary` 行清单逐个核对。
- 双信号一致（规则 A 与规则 B 同时判定错位）的候选只有 4 个：`13953`、`23953`、`23703`、`26800`；其中 `26800` 已客户端验收，改前需确认是否属于"迁移有意重定基线"。

## 5. 后续工作清单

1. 用一次客户端观测确定规则 A / 规则 B 的取舍（第 4 节未决点），确定后把结论写回本文件与 `QE-045`。
2. 按确定的规则处理 `report.tsv` 中 106 个 `MISMATCH_PROJECTION`，优先批次：
   - `154xx` redemption_landing 家族（40+ 个，形状一致，建议先抽 1 个做客户端验证再批量）；
   - `15301/15302/15303/15305` + `25301/25302/25303/25305`（15300 同链姊妹任务，XML 把步骤链折叠到 `var0=0`，需要同时决定折叠模型是否要恢复）；
   - `13950/23950`、`13953/23953`、`13956/23956`、`26800`、`30709/30759`、`50128/51128`。
3. 处理 32 个 `MISSING_RECOVERY_EDGE`：投影正确但缺少旧存档修复边（如 `3057`、`10528`、`13702`、`13703`、`13705`、`13880`、`15409`、`15410`、`15472`-`15475`、`80927`-`80932`）。
4. 62 个 `NO_XML`：迁移时必须先跑本脚本，把 pre-step 写进 reward 投影。
5. 每次迁移/新增任务后重新运行本脚本；偏差要么修，要么在本目录记录"有意重定基线"的理由与客户端证据。

## 6. 已经落地的防复发措施

- `Quest15300And25300RewardProjectionTest`：锁定 reward 投影、交接 actions、恢复边与 `QuestMutationPlanner` 计划（15300/25300 家族门禁）。
- `QuestInstanceExitRecoveryTest`：改为 null-safe 比较，允许任务定义出现无 source 的恢复边（本次 XML 新增该形状后暴露的测试端非空假设）。
- 本目录脚本 + `report.tsv`：把"人工回忆"换成可重复执行的清单，任何迁移提交后都能重新跑。
