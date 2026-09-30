# 批 P4d：monster 系投影表构建期生成（源入仓 + Java/Maven）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 授权：用户对 P4d 三问的答复——① 外部源**入仓库、不钉 sha**（因为会修改）；
> ② 构建期生成**必须 Java 或 Maven**；③（第三点 `events.properties:82` 经核为并行的开关改动，
> 非本问答案，用户未否决下述推断）。
> 推断执行口径：**M1/M2**（目标是减少源码树 TSV 数；生成物仍进 classpath/部署面）。
> 上游：`2026-09-28-p4-projection-layer-architecture-charter.zh-CN.md` §6.4、§12。

## 0. 范围裁定（与立项书的偏差，必须先记）

立项书列了 4 张 monster 系表。P4d 实施前复核发现：

- `quest_client_hunt_progress_rows.tsv`、`quest_client_hunt_stages.tsv`、
  `quest_client_kill_targets_stages.tsv` 的源 = 入仓 `quest_monster.csv` + `npc_template` ⇒ **可构建期重放**；
- `quest_client_kill_targets.tsv` 的源 = **45 个 ZONE 任务 XML**（`quest/definitions/quests/*.xml`），
  而这 45 个 XML **45/45 已随真端迁移退役**（实测缺失）⇒ 生成链早已断裂，不能构建期重放。
  该表由 P4c 单源冻结保留为唯一权威（G3 pin 已转 `SINGLE_SOURCE`）。

**P4d 实际范围 = 3 张表**；`quest_client_kill_targets.tsv` 不动。

## 1. 源入仓（已完成）

| 项 | 值 |
|---|---|
| 入仓路径 | `src/main/resources/aion/definitions/quest_monster/quest_monster.csv` |
| 来源 | `<客户端解包根>/Quest_unpacked/quest_monster.csv` |
| 规模 / sha256 | 8499 行 / 2227716 字节 / `aaa8da03…`（**记录但不钉**：用户明确该文件会持续修订） |
| 形态 | UTF-8（首行 BOM）、CRLF、无引号字段、列数 7–569 可变 |

## 2. 生成管线（Java + Maven）

- **生成器**：`src/main/generator/java/com/aionemu/tools/questgen/QuestMonsterTableGenerator.java`
  （341 行，**不参与主编译**；逐行移植 3 个 Python 分析脚本的过滤/排序/表头语义）。
  入口 `main(repoRoot, outputRoot)`，只读 `${project.basedir}`，**无绝对路径依赖**。
- **Maven**：`maven-antrun-plugin`（3.2.0）绑定 `generate-resources`：
  `<javac srcdir=src/main/generator/java release="25">` → `target/generator-classes`
  → `<java fork failonerror>` → `target/generated-resources/aion/data/static_data/quest_retail/`。
- **resources（互斥双 profile）**：
  - base `<build>` **无** `<resources>`（现状即如此）；
  - `checkout-resources`（`activeByDefault=true`）= `src/main/resources` + `target/generated-resources`
    （IDE/测试 classpath 可读生成表）；
  - `external-runtime-resources`（`-Daion.external-resources=true`）= 同两项且**均排除 `aion/**`**；
    该 profile 被激活时 `activeByDefault` 自动失效 ⇒ 两项天然互斥。
- **部署**：`scripts/package.sh` 在源码树 `aion/**` rsync 之后，追加
  `target/generated-resources/aion/**` 同步；目录缺失直接 **exit 1（fail-closed）**。
- **Python 脚本**：三个分析脚本 + 旧生成器**只读停写**，仅作移植参照。
- **构建环境说明**：antrun 3.2.0 需要 `org.apache.ant:ant(-launcher):1.10.15` 的 jar；
  本地仓库原先只有 pom，已用一次联网 `mvn generate-resources` 补齐，之后 `-o` 离线可跑。

## 3. 验收判据（实测结果）

| # | 判据 | 结果 |
|---|---|---|
| 1 | `mvn -o -B generate-resources` 成功、生成 3 张表 | ✅ `hunt_stages: rows=34 unresolved=0`；`kill_targets_stages: rows=8 problems=0`；`wrote 3 tables`（`gates/p4d-generate.log`） |
| 2 | 生成物与源码树旧表**逐字节相同** | ✅ 三对 sha256 全等：`hunt_progress_rows` `b83d134f…`、`hunt_stages` `753e15a2…`、`kill_targets_stages` `319e699f…`（旧表取 `git show HEAD:<path>`，新表取 `target/generated-resources/…`） |
| 3 | 删源码树 3 张表 + manifest 删 3 行 + `EXPECTED_TSV_COUNT` 20→17 同片 | ✅ manifest 45→42 行、冻结计数 20→17 |
| 4 | 聚焦门（28 例） | ✅ 1 红=在册 20035（`RetailDataDrivenGateTest.driftVersusShellsIsRegistered`）；清单门 3/3、`RetailHuntClientCountGateTest` 3/3、`RetailSimpleSerialHuntGateTest` 5/5、`RetailSimpleTalkChainGateTest` 8/8、`QuestEventShardRetailAlignmentTest` 3/3（`gates/p4d-refocus.log`） |
| 5 | DD / 链指纹 | ✅ `phase3-provenance/p4d.post-dd-fp.tsv`（1220 行 / 1218 数据行）`016e4542…`、`p4d.post-chain-fp.tsv`（288 行 / 285 数据行）`49e34999…`，与 P4b/P4c 基线逐字节相同 |
| 6 | T1 / T3 红集恒等 | ✅ T1（88 例 1 红）红集 `gates/p4d-T1-reds.txt` = `3b92439da8…`；T3（1352+83+583 例）调用级 `gates/p4d-T3-invocation-reds.txt` 129 条 `720e2cb3…`、基线口径 `gates/p4d-T3-reds-baseline-convention.txt` 94 条 `5e3acdb9…`，三项均与 P4b/P4c 基线逐字节相同 |
| 7 | 离线、无绝对路径依赖 | ✅ 生成器只读 `${project.basedir}`；`mvn -o` 全程通过 |
| 8 | `package.sh` 静态检查 | ✅ `bash -n` 通过；`pom.xml` XML well-formed 通过 |
| 9 | 两 profile 的 classpath 形态 | ✅ 默认：`target/classes/aion` 存在**且含 3 张生成表**；`-Daion.external-resources=true`：`target/classes/aion` **不存在**、`target/generated-resources` 仍产出 |
| 10 | 真实打包面（部署路径） | ✅ `mvn -DskipTests -Daion.external-resources=true package`：jar 内 `BOOT-INF/classes/aion/**` 条目 **0**、生成表条目 **0**（aion 数据由 `package.sh` 部署到 `AION_HOME`） |
| 11 | 行尾归一化不影响产物 | ✅ git 把入仓 CSV 归一为 **LF**（blob 0 个 CR，工作树仍 CRLF）——用 `git show HEAD:<csv>` 生成的 LF 源重跑生成器，三张表 sha256 与 CRLF 源**逐字节相同**（`gates/p4d-static-equivalence.txt`）；`readCsv` 走 `Files.readAllLines` + BOM 防御 |

## 4. 口径订正与决策留痕

- **`combine.self="override"` 保留**：`external-runtime-resources` 的 `<resources>` 上仍带该属性
  （原文件无）。实测（判据 9/10）**未出现**「aion 叠加进 jar」的现象——互斥由 `activeByDefault`
  提供，该属性在本 POM 结构下等价冗余；为不改动已实测通过的形态，保留并在此留痕。
- **`activeByDefault` 互斥**：不用“配置文件覆盖”的思路，而是让两个 profile 天然互斥，
  避免 `<resources>` 合并语义（Maven 对 resources 是 list 合并，不是 map 覆盖）产生双写。
- **范围收缩**：立项书的 4 张 → 实际 3 张（原因见 §0），已同步到 README 与立项书 §6.4 回执。

## 5. 纪律回执与遗留

- 未启动/停止/重启服务；未创建 worktree；未 push。
- 工作树内 `events.properties:82` 的 `false→true` 是用户并行改动，**不纳入本批提交**。
- **主树聚焦门复跑受并行车道阻塞**：`src/test/java/.../definition/Quest80787To80794RetailAlignmentTest.java`
  （13:10 由并行车道改动）引用 `RetailClientAcceptEntryPage` 但缺 import ⇒ 主树 `test-compile` 失败，
  与本批无关。判据 4/6 的实测取自 T3 副本 `/private/tmp/aion-t3-p4d`
  （该副本与本批受控文件逐字节相同：pom.xml / package.sh / manifest / 门禁测试 / 生成器 / 源 CSV；副本按工作树纪律**用后即删**，可复现的证据落在 `gates/` 的 txt/tsv 里）。
- 判据 4/6 的 **T3 全量分片与红集**记录在 `gates/p4d-t3-{1,2,3}.log`、`gates/p4d-t1.log`。
- 遗留：P4e（`talk_collect_chain_pages`、`enterarea_zone_resolution`）、P4f（`talk_chain_steps` IR）待办。
