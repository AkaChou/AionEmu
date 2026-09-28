# 批 P1 · 零消费者审计账退役：`retail-quest-ai-name-groups-rejected.tsv`（2026-09-28）

> 车道：`quest-native-dispatch`；面：Phase 3 执行批 **P1**（整表退役）。
> 上游：`phase3-recon/2026-09-28-phase3-feasibility.zh-CN.md` §判读-2（批 P1）。
> 用户决定「1」= 立项并执行 P1（2026-09-28），含对删除审计账的认可。
> 范围：**零代码变化** —— 快照 → 删表 → 删清单行 → 计数 22→21 → 生成器停写移交登记。

## 0. 一句话结论

`retail-quest-ai-name-groups-rejected.tsv`（23 行含头 / 18 数据行 / sha256 `180627ee…`）在
`src/main` + `src/test` 是**双零引用**（唯一命中 = 清单行自身）；唯一写入方是兄弟车道生成器
`p0c52_quest_ai_name_groups.py`（`if emit:` 内无条件写）。按 QE-094 正规动作整表退场：
退役前快照留档 + 删文件 + 删清单行 + `EXPECTED_TSV_COUNT` **22 → 21**；
若有人重跑生成器，`RetailTsvManifestGateTest`（磁盘集合 == 清单集合）会**立刻拦红**（fail-closed 兜底）。

## 1. 退役前提证据（实测当日树）

| 判据 | 证据 |
|---|---|
| 生产/测试零引用 | `grep -rn "ai-name-groups-rejected" src/main src/test` 唯一命中 = `quest-retail-tsv-manifest.tsv:43`（清单行自身）；退役后复测 **0 命中** |
| 无读取者类 | `RetailQuestAiNameGroupsRejected*` 全仓 0 命中；`phase3-recon/census-readers.tsv` 第 19 行 main/test/类名引用三列全空 |
| 唯一写入方 | `.agents/summary/scriptdll-quest-driver/p0c52_quest_ai_name_groups.py:37-40`（`OUT_REJECTED_TSV` 路径）+ `:313`（`if emit:` 内 `write_text`）——**兄弟车道，本车道只读** |
| 审计史保全 | 快照 `retired-tsv/retail-quest-ai-name-groups-rejected.tsv.retired-20260928`，sha256 与退役前逐字节相同 = `180627eef1105b60ebfcb55bed48845589157f460e1db2b45b8a27da7040e294`（23 行） |
| 可重算再生 | 生成器输入 = 客户端 npc 块 + 服务端 npc 模板 + 客户端词典 + 遗留 XML 接取流，属可重算数据；快照与生成器路径双留档 |

## 2. 处置清单（同一提交范围）

| # | 动作 | 对象 | 状态 |
|---|---|---|---|
| 1 | 快照 | `.agents/summary/quest-native-dispatch/retired-tsv/retail-quest-ai-name-groups-rejected.tsv.retired-20260928` | 已做 |
| 2 | 删表 | `src/main/resources/aion/data/static_data/quest_retail/retail-quest-ai-name-groups-rejected.tsv` | 已做 |
| 3 | 删清单行 | `quest-retail-tsv-manifest.tsv` 的 `retail-quest-ai-name-groups-rejected.tsv` 行（role=retention） | 已做 |
| 4 | 计数 | `RetailTsvManifestGateTest.EXPECTED_TSV_COUNT` **22 → 21** | 已做 |
| 5 | 生成器移交登记 | `p0c52_quest_ai_name_groups.py` 停写该表（README 终版移交清单第 ④ 项） | 已登记 |

## 3. 静态门（无 Maven 等价模拟）

- 集合门：磁盘 21 张 vs 清单 21 行，**双向零差集**（unregistered / dangling 均空）。
- 结构门：清单每行 4 列、file 唯一、role 均在词表内（`retention` 仍有 `retail-xml-retention.tsv` 在用）。
- 引用门：`src/main` + `src/test` 对表名与 `RetailQuestAiNameGroupsRejected` 均 **0 命中**。
- 快照门：快照 sha256 = 退役前原文 sha256（`180627ee…`）。

以上仅为静态一致性检查，**不替代** Maven 门禁；见 §4。

## 4. 门禁结果（2026-09-28，已授权执行）

| 门 | 命令 | 结果 | 证据 |
|---|---|---|---|
| 聚焦清单门 | `mvn -o -B test -Dtest=RetailTsvManifestGateTest -DfailIfNoTests=false -DforkCount=1` | **3/3 绿**（16.8s） | `gates/p1-manifest-focus.log` |
| T1（固定全局门，内含清单门 3/3） | `QUEST_FORK_COUNT=2 .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1` | 88 例 1 红（在册 DD 漂移）；红集 ADDED 0 / REMOVED 0；sha256 `3b92439da8052988…` 与基线**逐字节相同**（384s） | `gates/T1-090620.log` + `gates/p1-T1-reds.txt` |
| T3（questEngine 全树，仓库外副本三片：definition / retail / 其余） | 同 `gates/n3-t3-*.log` 头部 `# cmd:` 口径（`-DforkCount=4 -Dmaven.test.failure.ignore=true`） | 调用级 n1=129 / 本片=131（差异见 §4.1，全部为副本残渣）；**车道基线口径红集 94 条，sha256 `5e3acdb9dcaf255c…` 与 n1 基线逐字节相同** | `gates/p1-t3-1/2/3.log` + `gates/p1-T3-reds-baseline-convention.txt` + `gates/p1-T3-vs-n1-invocation-diff.txt` |

### 4.1 T3 差异分诊（幽灵红，与 P1 无关）

- 首轮调用级 n1=129 / 本片=131；ADDED 恰 2 条：`RetailTalkChainGateProbeTest.probeSingleStepGrantRows`、`RetailTalkChainProbeTest.probeChainEquivalence`。
- 根因：两条是**源码已删、仅存于 `target/test-classes` 的陈旧编译残渣**（`git ls-files` 无、`src/test` 无），被 T3 仓库外副本（rsync 全树）带入执行；与 P1 退役零关系。
- 处置：主树 + 副本共清 6 个 source-less class（另含 `ZzProbeDefinitionDumpTest`、`RetailSimpleHuntIrCompilerTest` 含内部类、`TempPvpDiagTest`）；清后基线口径红集 = 94 条、sha 恒等；可选清理后复跑被用户中断，未计入证据。
- 口径说明：`T3-n1-baseline-reds.txt` 不含参数化调用下标条目（n1 全量 129 条中 35 条参数化 → 94 条）；本片沿用该口径并另留调用级文件，避免误读。

判据达成：清单门 3/3 绿；T1 红集恒等；T3 基线口径红集恒等（逐字节相同）。
（本片零任务 id ⇒ T2 = T1 ∪ ∅，可由 T1 覆盖。）

注：缺口批 DoD④「`EXPECTED_TSV_COUNT` = 22 不变」已被本次用户授权的 P1 退役正式取代；`tools/dod_selfcheck.sh` 内的『目标 22』为历史 DoD 标签，本片不改历史自检脚本。

## 5. 沉淀（已完成，2026-09-28）

- 记忆库 **QE-094 追加 P1 验证**：零消费者生成物账整表退役 + 快照先行保全审计史；scope/boundaries 扩到"零消费者生成物账"。
- 记忆库 **QE-084 追加第三条门禁对拍假象**：副本带入源码已删的陈旧 `target/test-classes` 类（幽灵红）+ source-less class 普查纪律。
- 收口：`sync_memory_bank.py` → `MEMORY_BANK_SYNC_OK ENTRIES=143`；`verify_memory_bank.py` → `MEMORY_BANK_VERIFY_OK STEPS=3`。

## 6. 纪律回执

未 commit / push；未启停服务；**未新增任何 TSV**（本片只做退役）；未创建 worktree；
兄弟车道生成器**只读**、只登记停写；Maven 仅用于本片门禁（清单门 / T1 / T3 副本三片）；仓库外副本 `/private/tmp/aion-t3-p1` 跑完即删。
