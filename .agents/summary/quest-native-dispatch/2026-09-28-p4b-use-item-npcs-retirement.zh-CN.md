# 批 P4b 执行台账：`quest_use_item_npcs.tsv` 直接源消除（2026-09-28）

> 授权：用户「授权」（承接「辅助表是必要的吗」→ P4a 只读立项 → P4b 试点执行）。
> 上游：`2026-09-28-p4-projection-layer-architecture-charter.zh-CN.md`（P4a）、
> `phase3-provenance/2026-09-28-authority-provenance-audit.zh-CN.md`（Phase 3 血缘）。
> 性质：**M3 直接源消除**（不是 M1 构建期生成）；退役对象是**有活读取者的派生投影表**；
> manifest 21→20；零 IR 变化；无生成器改属。

## 0. 一句话结论

`quest_use_item_npcs.tsv`（4 注释行 + 804 数据行）整表退役：生产侧不再读表，改为在启动期
复用同一次读取构建的 `RetailNpcNameIndex`，取其从 8 个 `npc_template_*.xml` 分片收集的
`ai="quest_use_item"` id 集；静态复算 **804 = 804（missing 0 / extra 0）**；
DD（1218 数据行）/链（285 数据行）IR 指纹与 P2b 基线**逐字节恒等**；
T1 红集 `3b92439da8…`、T3 基线口径红集 `5e3acdb9…` 与基线**逐字节恒等**；
清单门 3/3 绿（`EXPECTED_TSV_COUNT` 21 → 20）。

## 1. 静态等价复算（fail-closed）

退役快照与 8 个模板分片逐 id 复算（脚本随本片落盘，可重放）：

```text
python3 -B .agents/summary/quest-native-dispatch/tools/verify_use_item_npcs_projection.py \
  .agents/summary/quest-native-dispatch/retired-tsv/quest_use_item_npcs.tsv.retired-20260928 \
  src/main/resources/aion/data/static_data/npcs
snapshot=...ids=804 sha256=2f7b4d226b0c0297a4a2b673c1a746148a4abf3cd43570b0db96651863973dcf
projection_ids=804
missing_in_projection=0 []
extra_in_projection=0 []
canonical_ids_sha256=62c0095f9bdb5b50f3aa00cffa32b8b0b66be68c802acf856e3212299c13f160
VERDICT=EQUIVALENT
```

输出留档：`gates/p4b-static-equivalence.txt`。分片清单与 `RetailQuestDriver.NPC_FILES` 完全一致
（`npc_template_200000_216188.xml` … `npc_template_834290_885645.xml` 共 8 片），
不是"全目录扫一遍"的近似口径。

## 2. 退役快照

| 证据 | 路径 | sha256 |
|---|---|---|
| 旧表退役快照（808 行全文） | `.agents/summary/quest-native-dispatch/retired-tsv/quest_use_item_npcs.tsv.retired-20260928` | `2f7b4d226b0c0297a4a2b673c1a746148a4abf3cd43570b0db96651863973dcf` |

数据目录 `src/main/resources/aion/data/static_data/quest_retail/` 未纳入 git 记录删除前的字节，
快照是审计史保全；重算通道 = 上述脚本（模板分片在 git 内）。

## 3. 处置清单（读取者 → 源索引 → 驱动 → 清单 → 计数）

| 面 | 文件 | 处置 |
|---|---|---|
| 读取者类 | `questEngine/retail/RetailQuestUseItemNpcs.java` | 删 `load(InputStream)`；改为 `fromIds(Collection<Integer>)` 内存视图，保留 `empty()` / `isInteractionObject` / `size` |
| 源索引 | `questEngine/retail/RetailNpcNameIndex.java` | 同一批模板流新增 `ai="quest_use_item"` id 收集（`LinkedHashSet`）+ `Set.copyOf` 冻结 + `questUseItemNpcIds()` getter |
| 驱动 | `questEngine/retail/RetailQuestDriver.java` | 删 `USE_ITEM_NPCS` 常量与 `load` 块；`npcIndex = build(...)` 之后 `RetailQuestUseItemNpcs.fromIds(npcIndex.questUseItemNpcIds())` |
| 门禁夹具 | `RetailDataDrivenGateTest.java` | 同型替换（删 load 块，改为从 `npcIndex` 取 id 集） |
| 冻结清单 | `quest-retail-tsv-manifest.tsv` | 删 `quest_use_item_npcs.tsv` 登记行（21→20 行） |
| 计数门 | `RetailTsvManifestGateTest.java` | `EXPECTED_TSV_COUNT` 21 → 20 |
| 生产表 | `static_data/quest_retail/quest_use_item_npcs.tsv` | 删除文件 |

生成器 `.agents/summary/scriptdll-quest-driver/build_quest_use_item_npcs.py` **只读保留，停写移交**：
它仍会写回 `src/main/resources/.../quest_use_item_npcs.tsv`，重跑即被清单门（磁盘集 ⊃ 清单集）拦红，
不会静默复活。

## 4. 零 IR 变化指纹门（与 P2b 基线对拍）

```text
mvn -o -B test -Dtest=RetailDataDrivenGateTest,RetailSimpleTalkChainGateTest \
  -Dretail.dataDriven.fpOut=.agents/summary/quest-native-dispatch/phase3-provenance/p4b.post-dd-fp.tsv \
  -Dretail.talkChain.fingerprintOut=.agents/summary/quest-native-dispatch/phase3-provenance/p4b.post-chain-fp.tsv \
  -DforkCount=2 -Dmaven.test.failure.ignore=true
```

| 指纹 | 数据行 | P2b 基线 sha256 | P4b sha256 | `cmp` |
|---|---:|---|---|---|
| DataDriven | 1218 | `016e4542d681937cf3779d40fcaf990addf3c59a79115f35916f99f639109cac` | 同左 | `IDENTICAL` |
| SimpleTalk 链 | 285 | `49e34999276c80acea5cf1514e5c3116ff4ab209523c1bc2f0a745532d32dd6a` | 同左 | `IDENTICAL` |

日志：`gates/p4b-fingerprint.log`；产物 `phase3-provenance/p4b.post-dd-fp.tsv` / `p4b.post-chain-fp.tsv`。

## 5. 门禁结果

| 门 | 命令 / 副本 | 结果 | 证据 |
|---|---|---|---|
| 编译 | `mvn -o -B -DskipTests compile` | BUILD SUCCESS（24s） | `gates/p4b-compile.log`（见纪律注） |
| 聚焦门（6 类） | `mvn -o -B test -Dtest=RetailTsvManifestGateTest,RetailDataDrivenGateTest,RetailSimpleTalkGateTest,RetailSimpleCollectItemGateTest,QuestInteractionObjectCatalogTest,QuestInteractionObjectContractGateTest -DfailIfNoTests=false -DforkCount=2 -Dmaven.test.failure.ignore=true` | 28 例 1 红（在册 20035 DD 漂移登记失同步）；清单门 3/3 绿、交互物合同门全绿 | `gates/p4b-focused.log` |
| T1 | `QUEST_FORK_COUNT=2 .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1` | 88 例 1 红（同一在册）；红集 ADDED 0 / REMOVED 0；sha256 `3b92439da8052988f0acd3a23a646786a78c3ac58e7c64a9c1dcabdd1d5cd137` 与 P2b 基线逐字节相同 | `gates/T1-114815.log` + `gates/p4b-T1-reds.txt` |
| T3 定义片 | 仓库外副本 `/private/tmp/aion-t3-p4b`：`-Dtest=com.aionemu.gameserver.questEngine.definition.**` | 1352 例（101F / 16E / 1S），BUILD SUCCESS，与 P2b 逐项相同 | `gates/p4b-t3-1.log` |
| T3 retail 片 | 同副本：`-Dtest=com.aionemu.gameserver.questEngine.retail.**` | 83 例（1F），BUILD SUCCESS，与 P2b 逐项相同 | `gates/p4b-t3-2.log` |
| T3 其余片 | 同副本：`-Dtest=com.aionemu.gameserver.questEngine.**,!com.aionemu.gameserver.questEngine.definition.**,!com.aionemu.gameserver.questEngine.retail.**` | 583 例（7F / 4E），BUILD SUCCESS，与 P2b 逐项相同 | `gates/p4b-t3-3.log` |
| T3 基线口径红集 | 三片合并、剔除参数化下标条目 | **94 条**，ADDED 0 / REMOVED 0；sha256 `5e3acdb9dcaf255c641d4e39deb73bd36af6ee89eaac1153e9941980023c8952` 与 P2b 基线逐字节相同 | `gates/p4b-T3-reds-baseline-convention.txt` |
| T3 调用级红集 | 同上，保留参数化下标条目 | 129 条；sha256 `720e2cb321c1f8a8941e82019e204ceb71c43a719fda9388e4b371c458d4ee47` 与 P2b 调用级逐字节相同 | `gates/p4b-T3-invocation-reds.txt` |

红集口径（本片落盘两个工具，均先以 P2b 历史数据自检后才用于本批）：

- **T1**：`tools/extract_surefire_reds.py` 从运行日志 Results 段还原全限定名（自检输出与
  `p2b-T1-reds.txt` 逐字节相同）；再用
  `tools/extract_surefire_reds_from_reports.py --only-classes gates/T1-114815.log`
  对主树 `target/surefire-reports/` 交叉验证，两法同为 `3b92439da8…`。
- **T3**：副本 `target/` 由 `rsync --exclude target` 后本批三片独占生成，
  用 `extract_surefire_reds_from_reports.py` 逐 testcase 提 XML 失败集后合并；
  调用级 129 条 / 基线口径 94 条，与 P2b 两份红集 sha256 逐字节相同。
- **教训（已固化进工具注释）**：日志 Results 段不是逐 testcase 权威——lambda 内部帧
  （`lambda$…$1`）会把同一逻辑失败重复列出（P2b slice1：117 行 → 85 个唯一 token）；
  逐 testcase 红集必须取 XML。而主树 `target/surefire-reports/` 混有历史陈旧报告
  （含已删探针类），所以 T1 必须 `--only-classes` 过滤、T3 必须用独占副本。

T3 副本由主树 `rsync`（排除 `.git`、`target`）建立，运行前 `*.class` 计数 = 0，
避免 P1 遇到的 source-less 陈旧 `target/test-classes` 幽灵红；跑完用 `shutil.rmtree` 删除。

## 6. 交付面与纪律回执

- manifest 行 21→20，`EXPECTED_TSV_COUNT` 同片 21→20；未新增任何 TSV；客户端合同表未动。
- 有活读取者的派生投影退役按"读取者接同源索引 + 静态集合复算 + IR 指纹恒等"执行（QE-099）。
- 生成器在分析工具目录，只读保留并登记停写；重跑生成器会被清单门立即拦红。
- 未启动/停止/重启服务；未创建 worktree；T3 副本跑完即删；`git push` 未授权未执行。
- 提交按「`fix(quest)` 代码/数据 + `docs(quest)` 台账/证据/记忆库」两条显式路径，无 `git add -A`。
- 编译：首跑 24s BUILD SUCCESS；复跑落盘 `gates/p4b-compile.log`（`Nothing to compile` + BUILD SUCCESS，
  证明首跑产物即当前源码）。
