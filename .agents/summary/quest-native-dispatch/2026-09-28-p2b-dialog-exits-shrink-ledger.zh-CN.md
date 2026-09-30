# 批 P2b 执行台账：`dialog_exits` 阶梯 token 行级缩表（2026-09-28）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 授权：用户「授权，继续」；范围 = 按 P2a 普查结果执行 P2b 缩表并跑门禁。
> 上游：`2026-09-28-p2-dialog-exits-shrink-charter.zh-CN.md`、
> `phase3-provenance/2026-09-28-p2a-census-report.zh-CN.md`。
> 性质：**只缩行、不删表**；零 IR 变化；manifest 行与 `EXPECTED_TSV_COUNT`（21）不变。

## 0. 一句话结论

`quest_client_dialog_exits.tsv` 的 **3938 数据行不变**，token **443 → 339**：
删除 104 个阶梯 token（`SELECT1_1` × 98 + `SELECT1_1_1` × 6，分布在 98 行；
其中 11 行删后为空出口）。DD 冻结 IR 指纹（1218 数据行）与链式 IR 指纹（285 数据行）
前后 **逐字节相同**；清单门 3/3 绿；T1 / T3 红集与基线 **sha256 恒等**。

## 1. 前置复核（fail-closed）

1. P2a 普查脚本要求 `requires(` 仍为 12 处 / 3 文件；P2b 前重跑通过。
2. 缩表脚本预演：

```text
python3 -B .agents/summary/quest-native-dispatch/phase3-provenance/apply_dialog_exits_shrink.py --check
CHECK_OK deletes=104 rows=98 tokens 443->339 live=0
```

预演还断言：census 中 `talk-ladder-live` = 0、删除 token 仅为 `SELECT1_1` / `SELECT1_1_1`、
生产表 sha256 与 `.before` 快照一致。

## 2. 写回结果与快照

```text
python3 -B .agents/summary/quest-native-dispatch/phase3-provenance/apply_dialog_exits_shrink.py --apply
APPLIED tokens 443->339 rows=98
after sha256=684a5a4b3e9abfda26bac1724ad2f5cb8fa4bdd2bd21720a4f12e79fc90f3e20
snapshots before=a3b360d15f7c65653dbcd00e42aaeaa213c2d2d0ed0592fa55a9a57c3c104a41 after=684a5a4b3e9abfda26bac1724ad2f5cb8fa4bdd2bd21720a4f12e79fc90f3e20
```

| 证据 | 路径 | sha256 |
|---|---|---|
| 缩表前快照 | `.agents/summary/quest-native-dispatch/retired-tsv/quest_client_dialog_exits.tsv.rows-20260928.before` | `a3b360d15f7c65653dbcd00e42aaeaa213c2d2d0ed0592fa55a9a57c3c104a41` |
| 缩表后快照 | `.agents/summary/quest-native-dispatch/retired-tsv/quest_client_dialog_exits.tsv.rows-20260928.after` | `684a5a4b3e9abfda26bac1724ad2f5cb8fa4bdd2bd21720a4f12e79fc90f3e20` |
| 删除 token 明细 | `.agents/summary/quest-native-dispatch/phase3-provenance/dialog-exits-removed-tokens-20260928.tsv` | 104 行（含每 token 客户端证据） |

行级差分：3938 数据行 + 16 注释行不变；98 行发生变化；`SELECT1_1` 98 个、`SELECT1_1_1` 6 个；
11 行删后为空出口，87 行仍保留体级 token（`SELECT2_CONTINUE` 289 / `SELECT_NONE_1` 14 /
`SELECT6` 13 / `SELECT5_CHECK` 12 / `SELECT5_CHECK_SIMPLE` 11）。

每一条删除 token 的客户端证据见明细表；抽样：`1131 → pages:select1_1@QUEST_Q1131.html:41a724ff`、
`1152 → pages:select1_1@QUEST_Q1152.html:2bc7760e`、`1156 → pages:select1_1@QUEST_Q1156.html:a2acb0db`。

## 3. 零 IR 变化指纹门

命令（Maven，2026-09-28 已授权）：

```text
mvn -o -B test -Dtest=RetailDataDrivenGateTest,RetailSimpleTalkChainGateTest \
  -Dretail.dataDriven.fpOut=.agents/summary/quest-native-dispatch/phase3-provenance/dialog-exits.post-dd-fp.tsv \
  -Dretail.talkChain.fingerprintOut=.agents/summary/quest-native-dispatch/phase3-provenance/dialog-exits.post-chain-fp.tsv \
  -DforkCount=2 -Dmaven.test.failure.ignore=true
```

结果：14 例 1 红（唯一红 = 在册 `20035` DD 漂移登记失同步，与缩表前基线一致），BUILD SUCCESS；
日志 `.agents/summary/quest-native-dispatch/gates/p2b-post-fp.log`。

| 指纹 | 数据行 | 缩表前 sha256 | 缩表后 sha256 | 前后 diff |
|---|---:|---|---|---|
| DataDriven | 1218 | `016e4542d681937cf3779d40fcaf990addf3c59a79115f35916f99f639109cac` | 同左 | 空 |
| SimpleTalk 链 | 285 | `49e34999276c80acea5cf1514e5c3116ff4ab209523c1bc2f0a745532d32dd6a` | 同左 | 空 |

## 4. 门禁结果

| 门 | 命令 / 副本 | 结果 | 证据 |
|---|---|---|---|
| 聚焦清单门 | `mvn -o -B test -Dtest=RetailTsvManifestGateTest -DfailIfNoTests=false -DforkCount=1` | **3/3 绿** | `gates/p2b-manifest-focus.log` |
| T1 | `QUEST_FORK_COUNT=2 .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1` | 88 例 1 红（在册 DD 漂移）；红集 ADDED 0 / REMOVED 0；sha256 `3b92439da8052988f0acd3a23a646786a78c3ac58e7c64a9c1dcabdd1d5cd137` 与基线逐字节相同 | `gates/T1-103054.log` + `gates/p2b-T1-reds.txt` |
| T3 定义片 | 仓库外副本 `/private/tmp/aion-t3-p2b`：`-Dtest=com.aionemu.gameserver.questEngine.definition.**` | 1352 例（101F / 16E / 1S），BUILD SUCCESS | `gates/p2b-t3-1.log` |
| T3 retail 片 | 同副本：`-Dtest=com.aionemu.gameserver.questEngine.retail.**` | 83 例（1F），BUILD SUCCESS | `gates/p2b-t3-2.log` |
| T3 其余片 | 同副本：`-Dtest=com.aionemu.gameserver.questEngine.**,!...definition.**,!...retail.**` | 583 例（7F / 4E），BUILD SUCCESS | `gates/p2b-t3-3.log` |
| T3 基线口径红集 | 三片合并、剔除参数化下标条目 | **94 条**，ADDED 0 / REMOVED 0；sha256 `5e3acdb9dcaf255c641d4e39deb73bd36af6ee89eaac1153e9941980023c8952` 与 n1 基线逐字节相同 | `gates/p2b-T3-reds-baseline-convention.txt` |
| T3 调用级红集 | 同上，保留参数化下标条目 | 129 条；sha256 `720e2cb321c1f8a8941e82019e204ceb71c43a719fda9388e4b371c458d4ee47` | `gates/p2b-T3-invocation-reds.txt` |

T3 副本由主树 `rsync`（排除 `.git`、`target`）建立，运行前 `*.class` 计数 = 0，
避免 P1 遇到的 source-less 陈旧 `target/test-classes` 幽灵红；跑完已用 `shutil.rmtree` 删除。
本批零任务 id ⇒ T2 = T1，不另跑 T2。

## 5. 交付面与纪律回执

- manifest 行未改，`EXPECTED_TSV_COUNT` 仍为 **21**；未新增任何 TSV；`SEMANTIC_GAP` 不回退。
- 生产表只删除 token，不删行、不删表、不改旗标常量；兄弟车道生成器
  `.agents/summary/scriptdll-quest-driver/build_quest_client_dialog_exits.py` **只读**，
  重跑会写回删除的 token（README 移交清单第 ② 项维持）。
- `git diff --check` 对本表的 11 条“trailing whitespace”是 **TSV 空出口字段的期望形**
  （`qid\t`，空 token 列；`RetailClientDialogExits.load` 要求 `parts.length >= 2`），
  与批 0 R1 提交 `fb43f97ee` 的同类空出口行一致，不是内容漂移。
- 本批执行期间未启动/停止/重启服务；未创建 worktree；未 push。
- 台账执笔后按「`fix(quest)` 数据缩行 + `docs(quest)` 台账/证据/记忆库」两条显式路径提交，
  不含 `git add -A`，不含无关脏文件。
