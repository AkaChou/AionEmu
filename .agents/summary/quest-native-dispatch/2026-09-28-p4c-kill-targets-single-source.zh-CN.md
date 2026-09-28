# 批 P4c 执行台账：`quest_client_kill_targets.tsv` 单源化（M4）

> 授权：用户「2、3 选 1」——在 P4c（快赢）与 P4d（monster 系，待三问）之间选择 P4c。
> 上游：`2026-09-28-p4-projection-layer-architecture-charter.zh-CN.md` §6.3（P4c 快赢）。
> 性质：**M4 单源化**——生产表保留且冻结面不变；只消除逐字节相同的测试夹具副本。

## 0. 一句话结论

`quest_client_kill_targets.tsv` 与测试夹具 `iluma-norsvold-kill-target-contract.tsv`
本是**逐字节相同的两份副本**（50 行 / 46209 字节 / sha256 `250ff5ee…`）。
P4c 让覆盖率门禁直读生产表并删除测试夹具；G3 血缘 pin 转为单源冻结，
fixture→production 提升通道关闭（`--apply` fail-closed）。manifest 与
`EXPECTED_TSV_COUNT` = 20 不变，无生产代码改动。
门禁收口：关键门 4/4 绿；DD/链指纹与 P4b 逐字节相同；T1 红集 `3b92439da8…`、
T3 调用级 129 条 `720e2cb3…` / 基线口径 94 条 `5e3acdb9…` 均与基线逐字节恒等。

## 1. 对象与前置复核（只读，已完成）

```text
shasum -a 256 生产表 测试夹具
250ff5ee7dbfb5ce7aba19c5075d4f998632e2051dbb46d8becbce2b2cb0a9f9  quest_client_kill_targets.tsv
250ff5ee7dbfb5ce7aba19c5075d4f998632e2051dbb46d8becbce2b2cb0a9f9  iluma-norsvold-kill-target-contract.tsv
diff → BYTE_IDENTICAL_CONTENT（两文件同为 50 行）
```

消费者盘点：测试夹具唯一读取点 = `QuestIlumaNorsvoldKillTargetCoverageTest.contractSnapshot()`；
生产表读取点 = `RetailClientKillTargets.load`（经 `RetailQuestDriver`）。

## 2. 处置清单

| 面 | 文件 | 处置 |
|---|---|---|
| 门禁测试 | `QuestIlumaNorsvoldKillTargetCoverageTest.java` | `CONTRACT_RESOURCE` 改为生产 classpath 路径 `/aion/data/static_data/quest_retail/quest_client_kill_targets.tsv`；Javadoc 双语说明单源化 |
| 测试夹具 | `src/test/resources/quest/iluma-norsvold-kill-target-contract.tsv` | **删除**（重复副本退役） |
| G3 血缘 pin | `phase3-provenance/provenance-pins.tsv` | G3 行 source 列改 `-`/`-`，regeneration 改 `SINGLE_SOURCE（P4c）`，invariant 改 artifact-only 冻结 |
| G3 通道 | `phase3-provenance/regenerate_kill_targets_production.py` | 改为只读单源校验（生产表 sha == pin）；`--apply` 关闭并 fail-closed（exit 2） |
| 生成器 | `.agents/summary/quest-15546-kill-progress/generate_kill_target_contract_tsv.py` | 属兄弟车道，**只读保留 + 停写登记**：重跑只会重建已退役夹具，不再影响生产表 |
| 生产表 / manifest | — | **不改**：manifest 与 `EXPECTED_TSV_COUNT` = 20 不变 |

## 3. 静态校验（已完成，零 Maven）

```text
python3 -B phase3-provenance/check_provenance_pins.py
PROVENANCE_PINS checks=7 skipped=0 failed=0 / PROVENANCE_PINS_OK

python3 -B phase3-provenance/regenerate_kill_targets_production.py --check
G3_CHECK_OK (single source, pinned sha match)

python3 -B phase3-provenance/regenerate_kill_targets_production.py --apply
G3_APPLY_DISABLED (P4c single-sourcing: no fixture to promote) / exit 2
```

`grep -rn iluma-norsvold-kill-target-contract src/` = 0 命中；`git diff --check` = 0。

## 4. 门禁结果（已执行）

| 门 | 命令 | 结果 | 证据 |
|---|---|---|---|
| 聚焦门 | `mvn -o -B test -Dtest=QuestIlumaNorsvoldKillTargetCoverageTest,QuestA03ShardRetailAlignmentTest,RetailTsvManifestGateTest,RetailDataDrivenGateTest,RetailSimpleTalkChainGateTest -DfailIfNoTests=false -DforkCount=2 -Dmaven.test.failure.ignore=true` | 24 例 1F + 1E：1F = 在册 20035；1E = 在册 `QuestA03Shard…pureTalkQuest21065`（P4b T3 基线红集第 66 行）。关键门 `QuestIlumaNorsvoldKillTargetCoverageTest` **4/4 绿**、清单门 3/3、链门 8/8 | `gates/p4c-focused.log` |
| 指纹对拍 | `mvn -o -B test -Dtest=RetailDataDrivenGateTest,RetailSimpleTalkChainGateTest -Dretail.dataDriven.fpOut=.../p4c.post-dd-fp.tsv -Dretail.talkChain.fingerprintOut=.../p4c.post-chain-fp.tsv -DforkCount=2 -Dmaven.test.failure.ignore=true` | DD 1218 数据行 sha `016e4542…`、链 285 数据行 sha `49e34999…`，与 `p4b.post-*` **逐字节相同** | `gates/p4c-fingerprint.log` + `phase3-provenance/p4c.post-*.tsv` |
| T1 | `QUEST_FORK_COUNT=2 .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1` | 88 例 1 红（同一在册）；红集 sha `3b92439da8052988f0acd3a23a646786a78c3ac58e7c64a9c1dcabdd1d5cd137` 与 P4b 逐字节相同（日志 + XML 双口径） | `gates/T1-121255.log` + `gates/p4c-T1-reds.txt` |
| T3 | 仓库外副本 `/private/tmp/aion-t3-p4c` 三片（同 P4b 口径，`forkCount=4`） | 三片 1352/83/583 例；调用级红集 129 条 sha `720e2cb321c1f8a8941e82019e204ceb71c43a719fda9388e4b371c458d4ee47`、基线口径 94 条 sha `5e3acdb9dcaf255c641d4e39deb73bd36af6ee89eaac1153e9941980023c8952` 均与 P4b 逐字节相同；`QuestIlumaNorsvoldKillTargetCoverageTest` 0 红 | `gates/p4c-t3-*.log` + `gates/p4c-T3-*.txt` |

## 5. 交付面与纪律回执

- 无生产代码改动；生产表内容与冻结计数不变；未新增 TSV。
- 测试夹具删除后，命名/生成链在台账与 pin 中留痕；兄弟车道生成器只登记不改。
- T3 按纪律在仓库外副本跑（`rsync --exclude .git --exclude target`，运行前 `*.class` 计数 = 0），
  跑完 `shutil.rmtree` 删除，无残留副本、无 worktree。
- 未 push；未启停服务。
