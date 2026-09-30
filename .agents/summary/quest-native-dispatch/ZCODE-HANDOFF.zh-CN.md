# zcode 交接包：quest 真端驱动改造（下一阶段）【已被取代】


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> ⚠️ **本包已于 2026-09-27 被 `HANDOFF.zh-CN.md` 取代**（执行体无关版，基准提交 `b5d2a7dea`）。
> 本包写作时"工作树是未提交的大迁移现场"的前提**已不成立**：迁移现场已提交为
> `4ede058c0`（源码）+ `5318a8686`（台账/工具链）+ `b5d2a7dea`（产物清理），工作树干净。
> 下文的 §1 提示词、§2-2 提交纪律、§4 基线表均已按新基准就地更新，但**请优先使用 `HANDOFF.zh-CN.md`**。
> 保留本文件仅为历史留痕（同片证据与陷阱章节仍可参考）。

> 生成：2026-09-27 23:1x（车道 `quest-native-dispatch` 收口点）。
> 用法：**§1 完整提示词**可直接整段粘贴给 zcode 执行体；§2 防漂移约束、§3 防空等命令纪律是提示词的配套硬约束。
> 本文与 `GOAL.zh-CN.md §0.5`、`README.zh-CN.md`（台账）构成下一阶段的**唯一权威入口**。
> （2026-09-27 更新：唯一权威入口改为 `HANDOFF.zh-CN.md`。）

---

## §1 完整提示词（可整段粘贴）

```
你是 AionEmu 仓库（<仓库根>，分支 quest）的 quest「真端驱动」迁移执行体。

【目标】让"可迁移"任务由真端表在加载期合成生命周期分发（RETAIL_TABLE），停止扩建任何页码类 TSV 补丁，
把已死的页码类 TSV 退役掉；保留 XML 的行必须逐行有登记理由。

【工作树状态（重要）】迁移现场**已提交**：基线 = `b5d2a7dea`（源码 `4ede058c0`、台账/工具链 `5318a8686`、
产物清理 `b5d2a7dea`）；工作树干净，仅 4 个未跟踪的工具链输入表（`item_name_index.tsv`、
`npc_name_index.tsv`、`quest_registry.tsv`、`m5b2b-quest-event-census.tsv`，**不得删除**）。
**不 push、不启停任何服务进程**；**commit 仅在用户当轮显式授权后进行**，授权后源码与台账分提交。

【必读顺序（约 15 分钟）】
1. AGENTS.md + .agents/rules/{i18n,java_general,backend,formatting,lombok,ai-artifacts,quest-repair}.md
   —— 其中 i18n（中英双语注释/日志）与 formatting（Java 用 Tab 缩进）是硬规则；
2. .agents/summary/quest-native-dispatch/GOAL.zh-CN.md —— **先读 §0.5「执行现状与勘误」**（当前权威现状）；
3. .agents/summary/quest-native-dispatch/README.zh-CN.md —— 车道台账（每片一节，含判据与证据路径）；
4. 四份 W5 退役台账 2026-09-27-w5g{1,2,3,4}-*.zh-CN.md；
5. .agents/summary/quest-native-dispatch/2026-09-27-w5c-w6-recon-pack.zh-CN.md（取证包，含勘误块）；
6. 记忆库：`python3 .agents/memory-bank/search_memory_bank.py "<现象/关键词>" --json` 定位 Pattern ID，
   再 `--id <PATTERN_ID>` 展开单条（不要整篇读领域卡片）。重点：QE-094/095/096（退役三范式）、
   ENV-004（文本编辑/删除纪律）、QE-051（领奖行）、QE-083/089/090/091/092/093（形状守卫族）。

【当前状态（截至 2026-09-27 22:40）】
- SimpleTalk γ 面（W1–W4）已收口：S1/S2/S3a/S3b/S3c-A/S3c-D/S3c-obj（285 链行 + 1323 物件行）。
- W5 已退役四张页码类 TSV：report_pages(5995 行) / entry_pages(2449) / talk_pages(1340) / briefing_chains(3230)；
  `EXPECTED_TSV_COUNT` 26 → 22；退役三范式见 GOAL §0.5（纯机械 / 活门换判据 / 零拒绝迁构建期）。
- W6 部分完成（shell 编译器退役、命名修正、登记补注）。
- 门禁基线：T1 = 87 测试，唯一红 = `RetailDataDrivenGateTest.driftVersusShellsIsRegistered`（行号 20035，
  属并行车道「在册红」——**不要修它、不要把它改绿**）；红集 sha256 基线 `3b92439da8052988f0acd3a23a646786a78c3ac58e7c64a9c1dcabdd1d5cd137`。
- T3（仓库外全树副本，forkCount=2）最近一次结果见 `.agents/summary/quest-native-dispatch/gates/T3-w5g34.log`。

【你的任务：按风险从低到高，一次一片，逐片验收】
R1 `quest_client_dialog_exits.tsv` 行级缩表（W5-g5，非删除）
   - 事实：7 个旗标**无整旗标死亡**；死面只在行级（singleStep 行 / 非 systemGrant 链行 / 已 canonical 交付段行）；
     `requires(` 调用点 12 处/3 文件（测试零调用）。
   - 做法：①按行 × 旗标做可达性普查（生成器 `build_quest_client_dialog_exits.py` 与客户端映射同源，可在同处加过滤）；
     ②只删"该行永不被读"的 token，删 token **不删旗标**；③判据 = 缩表前后**家族门定义快照逐键相同**
     （`RetailDataDrivenGateTest` / `RetailSimpleTalkGateTest` / `RetailSimpleTalkChainGateTest` /
     `RetailSimpleCollectItemGateTest` / `RetailSimpleHuntEquivalenceGateTest` / `RetailSimpleHuntFamilyGateTest` /
     `RetailSimpleUseItemGateTest`）；④表与清单行数不变（不删表）。
   - 禁止：因"交付已接管"就删 `SELECT6`/`SELECT5_CHECK*`/`SELECT2_CONTINUE`（它们仍被体级 :1465/:1438/:1411 读）。
R2 D 类加窗独立裁定（S3c-D 遗留）
   - 三选项：镜像领奖腿 / 窗落交付 NPC / 维持现状；(iii) 就地加窗**已被 QE-092 否决**，不得复活。
   - 判据：逐行 e2e 证据（黑盒契约门 `RetailQuestContractTest` + 预览/领奖路由审计），裁决入台账。
R3 W6 残项
   - EarlyElyos 3 条在册红（期望形见 `2026-09-27-w5c-w6-recon-pack.zh-CN.md` §4：1131 `started→shugo` 交付边 /
     1561 `CanAct` 自环门 / 1691 `spoken-to-diana→returned-to-sneaker`）——属**形状工作**，先裁定再改断言；
   - `FailurePage` 一名指两页（SELECT6=2716 vs CHECK_USER_ITEM_FAIL=10001）——跨文件重命名，先定范围；
   - 生成器停写移交：`build_quest_client_report_pages.py` 仍会写回已退役路径（重跑会被清单门拦红，属 fail-closed 兜底）。
R4 W7 收口：README 全片台账、记忆库 QE 覆盖、迁移总量复算（retention 的 RETAIL_TABLE 计数与比列）。

【每片标准流水线（固定六步）】
1) 事实冻结：读相关台账 + 搜记忆库 + 实测计数（不要把推断当事实）；
2) 生产改动：**单写者串行**，锚定式编辑（最小唯一锚点 + 断言计数；禁用"往上找最近注释"式启发）；
3) 守卫自检：fail-closed 负例（在仓库外副本里改数据，验证"拒绝编译/下限红"，跑完复原并核 md5）；
4) 聚焦门 + 家族门：见 §3 命令；
5) T1（必跑）；T2/T3 合并跑一次（见 §3）；
6) 台账（`.agents/summary/quest-native-dispatch/2026-09-27-<slice>.zh-CN.md`）+ README 章节 + 记忆库
   （`sync_memory_bank.py` + `verify_memory_bank.py`，绿了才算收口）。

【硬约束】见本包 §2；【命令与防空等】见本包 §3。
```

---

## §2 防漂移约束（硬约束，任何执行体不得违反）

1. **切片判据（每片必用）**：① 指纹/漂移集 **逐行等于该片行集**（零变化片 = 逐行空集）；
   ② 链门/家族门绿；③ **T1/T2/T3 `ADDED 0 / REMOVED 0`**，更强口径 = 红身份集 **sha256 逐字节相同**。
2. **不 push**；**提交仅在用户当轮显式授权后进行**（源码 / 台账分提交，`git add <显式路径>`，禁 `git add -A`）；
   **不启停、不重启任何服务器进程**；不新增页码类 TSV。
3. **受理 flip = 三件套同片**：`retail-xml-retention.tsv` owner 翻转 + **遗留 XML 退役**（`quest/definitions/quests/<id>.xml`）
   + `quest_definition_catalog.xml` 条目删除；并**清理 `target/` 孤副本**（否则生产视图继续按"XML 存在"判 owner，
   级联十几条"生产视图不可读"错误）。
4. **drift/指纹重冻只写本片变化面**：与在册登记不一致的 foreign 行（并行车道的「在册红」，代表：DD drift 的
   `20035`）**必须按原值回置**——把它改绿 = REMOVED 1，违反判据②。
5. **派生生成器重跑前必须备份 + 逐行核差**：`build_retention_list.py` 等脚本会因不消费其他车道的新裁定文件而
   **回退**历史行（实测一次回退 472 行）；若回退 foreign 行 → 立即还原 + 改用**定向手工编辑**（锚定 + 断言）+
   登记"生成器已滞后"移交项。
6. **退役表先落快照**：`static_data/quest_retail/` **未纳入 git** ⇒ 每张退役表先复制到
   `.agents/summary/quest-native-dispatch/retired-tsv/<name>.retired-<YYYYMMDD>` 并记 sha256。
7. **T3 只在仓库外副本跑**（`rsync -a --exclude .git --exclude target --exclude .agents`），跑完
   `rm -rf` 并确认无残留；**同一棵树同时只跑一个 Maven**；shipped 树内不跑全量扫。
8. **兄弟车道只读**：`.agents/summary/scriptdll-quest-driver/**` 的生成器脚本与输入登记表**一字节不改**
   （只读执行生成器可以；其输出若被本片变更牵动，登记"停写/更新"移交项）。例外：`affected_quest_tests.py` 的
   `T1_GATE_CLASSES` 按既有惯例追加新常设门（每次只追加本片那一行）。
9. **AI 中间产物**只落 `.agents/summary/<topic>/`；禁止 `.agent/`、禁止 `scripts/`；临时 worktree 只在必要时
   创建于仓库外并及时 `git worktree remove --force` + `git worktree prune`。
10. **守卫 fail-closed**：只允许"拒绝编译 + 交裁定"或"测试下限红"；禁止静默丢边/丢页/丢门/丢片。
    守卫改动必须用**变异负例**单独证明可拦性（"重跑逐字节相同"只证可复现，不证可拦）。
11. **不做未经核实的结论**：取证代理/脚本的结论必须复核（本项目已两次否决代理结论：drift 计数口径、
    "briefing 唯一解锁面"）。被否决的结论要**留在台账里**（含被否方案）。
12. **记忆库协议**：任务收口时更新 Pattern（若可复用）、跑 `sync_memory_bank.py` + `verify_memory_bank.py`，
    并在回复末尾附 `[Memory Bank Auto-Updated]` 回执。

---

## §3 命令与"防空等/空转"纪律（提速口径）

**总则**：长命令一律**后台跑 + 等完成通知**；**禁止 `sleep N` 轮询**（需要查进度时看一次日志文件即可）；
Maven 加 `-o -B`，测试用 `-DforkCount=2`；测试日志写文件，事后 grep 摘要。

```bash
# 聚焦测试（首选；类列表按本片受影响面选）
mvn -o -B test -Dtest=ClassA,ClassB -DforkCount=2 -Dmaven.test.failure.ignore=true > /tmp/<slice>-focus.log 2>&1

# T1（固定门禁，含清单门/身份门/家族门/新常设门；forkCount=2 提速）
QUEST_FORK_COUNT=2 QUEST_LOG_DIR=.agents/summary/quest-native-dispatch/gates \
  .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1

# T2（链选择器 285 id；**zsh 必须用 ${=IDS} 分词**，否则选择器工具报 "quest id must be numeric"）
IDS=$(cat /tmp/<ids>.txt)   # 复算：retention owner=RETAIL_TABLE ∩ quest_client_talk_chain_steps.tsv 去重 = 285
QUEST_LOG_DIR=.agents/summary/quest-native-dispatch/gates \
  .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 ${=IDS}

# T3（仓库外副本，跑完即删）
# 注意：**只能排除 .git 与 target**；`.agents` 必须带上（有测试直接读 .agents/summary/quest/*.py）
rsync -a --exclude .git --exclude target <仓库根>/ /private/tmp/aion-t3-<slice>/
cd /private/tmp/aion-t3-<slice>
mvn -o -B test '-Dtest=com.aionemu.gameserver.questEngine.**' -DfailIfNoTests=false -DforkCount=2 \
  > <仓库根>/.agents/summary/quest-native-dispatch/gates/T3-<slice>.log 2>&1
rm -rf /private/tmp/aion-t3-<slice>

# 红身份集提取 + 对拍（必用 LC_ALL=C sort；locale 排序会造假 ADDED/REMOVED 假象）
grep -oE "^\[ERROR\] [A-Za-z0-9_.$]+\.[A-Za-z0-9_]+ -- Time elapsed" <log> \
  | sed -E 's/^\[ERROR\] //; s/ -- Time elapsed$//' | LC_ALL=C sort -u > gates/<slice>-reds.txt
diff gates/<baseline-reds>.txt gates/<slice>-reds.txt && echo "ADDED 0 / REMOVED 0"
shasum -a 256 gates/<slice>-reds.txt

# 排查导出（只在需要时用）
#   DD 漂移：  -Dretail.dataDriven.equivOut=/tmp/<slice>-drift.tsv
#   DD 指纹：  -Dretail.dataDriven.fpOut=/tmp/<slice>-fp.tsv
#   SimpleTalk 链指纹：-Dretail.talkChain.fingerprintOut=/tmp/<slice>-chain-fp.tsv
#   狩猎/采集等价：-Dretail.hunt.equivOut=/…、-Dretail.collect.equivOut=/…、-Dretail.itemPlay.equivOut=/…

# 记忆库
python3 .agents/memory-bank/sync_memory_bank.py
python3 -B .agents/memory-bank/verify_memory_bank.py   # 期望 MEMORY_BANK_VERIFY_OK STEPS=3
```

**防空等 checklist（每次跑长命令前自检）**
1. 用 `run_in_background: true`，不要在前台等 >10 分钟；
2. 不写 `sleep` 轮询循环；收到完成通知后再读结果；
3. T1/T2/T3 能合并就合并（同一次收口只跑一遍 T3）；
4. 副本 rsync 是 2.1G 级操作——后台跑；
5. 删除源文件后只清 `target/` 对应孤副本（**不要 `mvn clean`**，太慢）；
6. 一次检查（`pgrep -fl surefirebooter` + `tail -3 <log>`）即决定下一步，不反复轮询。

---

## §4 关键路径与基线（速查）

| 项 | 值 |
|---|---|
| 仓库 | `<仓库根>`（分支 `quest`；基线提交 `b5d2a7dea`，领先 origin/quest 22 个未推送提交） |
| 车道台账 | `.agents/summary/quest-native-dispatch/`（GOAL §0.5 / README / 各片台账 / gates/ 日志与红集） |
| 冻结面门 | `RetailTsvManifestGateTest`（磁盘集合 == 清单集合 == `EXPECTED_TSV_COUNT`，**现值 22**） |
| 新常设门 | `RetailBriefingChainEvidenceGateTest`（简报链不变量，登记 3225 + 基数 47/5 冻结） |
| T1 红集基线 | sha256 `3b92439da8052988f0acd3a23a646786a78c3ac58e7c64a9c1dcabdd1d5cd137`（1 条：DD drift 20035） |
| T2 红集基线 | sha256 `57bb0621b2aa90f861e41358f63baa5450e73753440160ba276bb0255df721fd`（51 条） |
| T3 红集基线 | sha256 `ce4673c74ed61fdcff494fedcc1b8e98c78b252d54b150a05c4bdaa937197870`（97 条；测试数 2016 + W5-g4 新门 1 = 2017） |
| 快照目录 | `.agents/summary/quest-native-dispatch/retired-tsv/`（4 张退役表的 sha256 快照） |
| 记忆库入口 | `.agents/memory-bank/README.md` + `search_memory_bank.py`（索引 `index.jsonl` 由 sync 生成，禁止手改） |

## §5 已知陷阱（本项目实测过，别再踩）

1. `zsh` 不按空格分词：`run_quest_gates.sh T2 $IDS` 会把整串当一个 id → 用 `${=IDS}`。
2. `grep -c <码> file` 会把**注释行**也计入（`drift.tsv:13` 的 "6" 是 6 数据行 + 1 注释行，注释是对的）。
3. Maven 资源拷贝**不删**已删源文件 → `target/classes/**` 孤副本会让"改了没生效"（先比对 target 与源码树）。
4. retention 是 drift 的**派生滞后视图**（实测滞后 33 行）→ 判定"谁被拒"以 **drift code 列为准**。
5. 契约门对**已退役行豁免**全页审计（`RetiredQuestIds` 过滤）⇒ 受理 flip 后的行不再有逐页审计兜底，
   客户端证据必须在 flip 前逐行取到（HTML 存在性 / 任务书行 / 全局页 doctrine）。
6. `lastRowIndex` 缺行**静默返回 0**（`Math.max(0, rows-1)`）——这类静默退化点必须由门拦住。
7. 批量删除成员禁用启发式锚定（会吃类头）；对未跟踪文件先落副本；误删可从
   `~/.claude/projects/**/*.jsonl` 会话记录逐字恢复（ENV-004 规则 3）。
8. **T3 副本不能排除 `.agents`**：`QuestDialogMigrationGateTest` 直接读
   `.agents/summary/quest/generate_quest_dialog_enums.py`；排除它会在 T3 里造出 1 条**假新增红**
   （实测：2017 测试中 98 红 = 基线 97 + 该假红；主树内该测试 4/4 绿）。

## §6 提速方案（批次化，替代"一片一跑"）

**成本结构（实测）**：单片 ≈ 门禁 30 min（家族门 5–6 min + T1 6–10 min + T3 20–25 min）+ 分析/编码 15 min。
**三个杠杆（不放松任何判据）**：

1. **合并批次**：零行为的项（重命名 / 登记 / 文档）并入形状片，**同一次门禁**验收；
2. **门禁并行**：T1（主树）∥ T3（副本，含 `.agents`）同时跑；`QUEST_FORK_COUNT=3~4`（10 核机器实测可行）；
3. **分级门禁**：形状批 = 聚焦 + T1 + T3；**零行为批** = 聚焦 + T1（**免 T3**）；
   **T2 一般可省**（T3 覆盖 questEngine 全树，T2 的类集 ⊂ T3 ∪ T1）。
4. **分析并行**：只读取证（子代理，禁 Maven）与编码同时进行；取证结论进台账。

**推荐批次（剩余 R1–R4）**：

| 批 | 内容 | 门禁 | 预估 |
|---|---|---|---|
| **A（形状片）** | R1 `dialog_exits` 行级缩表（行级普查脚本 + 家族门定义快照对拍） | 聚焦 + T1 ∥ T3（1 轮） | ~45 min |
| **B（零行为，与 A 同批）** | R3：`FailurePage` 命名（跨文件重命名）+ 生成器停写移交登记 | 复用 A 的门禁 | +0 |
| **C（裁定片）** | R2 D 类加窗（先取证 → 裁定 → 可能含 EarlyElyos 3 条断言重锚） | 聚焦 + `RetailQuestContractTest` + T1 ∥ T3 | ~60 min |
| **D（文档批）** | R4 W7 收口（README / 记忆库 / 迁移总量复算） | 无 Maven；`sync_memory_bank.py` + `verify_memory_bank.py` | ~20 min |

⇒ 门禁轮数 **5 → 2**；剩余总时长预估 **~4–5 h → ~1.5–2 h**。

### §6.1 追加三杠杆（实测瓶颈：家族门 5–6 min 占 T1 九成；T3 全量 20–25 min；每次 mvn 启动/资源拷贝 30–60 s）

1. **暖副本（T3 增量跑）**：持久化一个副本 `/private/tmp/aion-t3-warm`，**首次**全量 rsync + `test-compile` 暖一次；
   之后每次只 `rsync -a --delete --exclude .git --exclude target <repo>/ /private/tmp/aion-t3-warm/`
   （**target 被 exclude ⇒ 保留暖编译产物**），再跑测试 ⇒ 省掉每次 3–4 min 全量编译。
   纪律：副本在仓库外；**工作全部结束时必须 `rm -rf`**（本工程 sprint 模式下的唯一放宽，收口时清零）。
2. **T1-fast（批次级快筛）**：T1 的 87 个测试里 `RetailSimpleHuntFamilyGateTest` 独占 ~6 min ⇒ 批次内跑
   **T1 类表 − 家族门**（~1–2 min），家族门留给**里程碑 T3**（T3 ⊇ 它）。红集对拍时按类名过滤本批次日志即可保持口径。
3. **单次 mvn 合并选择器**：把"T1-fast + 本片聚焦类 + T2 选择器（103 类）"并成一个 `-Dtest=` 列表跑一次
   （一次编译一次启动），比分开跑 3 次省 ~2–4 min；日志事后按类名过滤即可产出各档红集。
4. **`-DforkCount=4`**（10 核机）：T2/T3 实测可从 forks=2 再压 30–40%；T1-fast 受益小（类少）。

**极限计划（剩余全部工作一次门禁周期）**：
```
批 A+（一次 screen）：R1 dialog_exits 缩表 + R3 FailurePage 命名 + 生成器移交登记
    → 一次 mvn：T1-fast + 聚焦(7 门) + T2 选择器，forkCount=4        ≈ 6–8 min
    → 逐项 patch 记录（出红按项二分）
R2（裁定）：先只读取证；若裁定 = 维持现状 ⇒ 零门禁；若需改形状 ⇒ 追加一次 screen
里程碑：暖副本 T3（forkCount=4）                                       ≈ 10–12 min
R4（文档）：无 Maven；记忆库 sync+verify                                ≈ 20 min
```
⇒ 剩余总时长可压到 **~1–1.5 h**（相对最初单片节奏 ~4–5 h）。

> 注：批次内改动要**逐项留独立 patch 记录**（基线已提交，"回滚"= `git checkout b5d2a7dea -- <path>` 或反向应用该项改动），
> 这样某批若出红，可按项二分定位，不必重跑整批。
