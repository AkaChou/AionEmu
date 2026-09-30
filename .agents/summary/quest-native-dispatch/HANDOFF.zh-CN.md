# 交接包：quest 真端驱动改造（下一阶段 · 基准 `2ab91c0f8`）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 生成：2026-09-27（车道 `quest-native-dispatch`，**已提交基线**）。
> 取代：`ZCODE-HANDOFF.zh-CN.md`（旧版假设"未提交大迁移现场"，已过时）。
> **执行体无关**：Claude Code / Codex / zcode / 其他具备 shell 与文件读写的 agent 均可直接执行 §1。
> 本文与 `GOAL.zh-CN.md §0.5`、`README.zh-CN.md`（台账）构成下一阶段的**唯一权威入口**。

---

## §1 完整提示词（可整段粘贴给任意执行体）

```
你是 AionEmu 仓库（/Users/mc/IdeaProjects/AionEmu-test，分支 quest）的 quest「真端驱动」迁移执行体。

【目标 · 完成改造标准（Definition of Done）】本阶段的终点不是"再推进若干片"，而是让 Goal §0 三条同时成立：
① 全部"可迁移"任务由真端表在加载期合成生命周期分发（RETAIL_TABLE）；
② 仍保留 XML 的行**每一行都有登记理由**（SCRIPTED / NO_TABLE / 或经逐行裁定的"不可迁移"）——
   `retail-xml-retention.tsv` 里 **SEMANTIC_GAP:* 归零**；
③ 页码类 TSV 不扩建、已退役的四张保持退役（`EXPECTED_TSV_COUNT` 只减不增）。
**当前实测（起点）**：6224 全量 = 4959 RETAIL_TABLE（可迁移面 5554 的 89.3%）+ 670 计划保留
（SCRIPTED 494 + NO_TABLE 176）+ **595 SEMANTIC_GAP（37 个缺口码）**。这 595 行就是本阶段主线；
R1–R4 只是开场收尾（批 0）。

【工作树状态（重要）】迁移现场**已提交**；**基线 = 你启动时的 HEAD**（写作时 = `837e4c58e`；链路：
4ede058c0 源码迁移 → 5318a8686 台账/工具链 → b5d2a7dea 产物清理 → d1806b476 交接包与工具 →
2ab91c0f8 工具链输入表入库 → 837e4c58e 自主推进契约）。
工作树应**干净**；4 个工具链输入表（`item_name_index.tsv`/`npc_name_index.tsv`/`quest_registry.tsv`/
`m5b2b-quest-event-census.tsv`）**已入库且被活脚本消费——不得删除、不得移动**（§2-13）。
未推送（写作时领先 origin/quest 25 个提交）：不要 push；提交仅在用户当轮显式授权后进行。

【必读顺序（约 15 分钟）】
1. AGENTS.md + .agents/rules/{i18n,java_general,backend,formatting,lombok,ai-artifacts,quest-repair}.md
   —— i18n（中英双语注释/日志）与 formatting（Java 用 Tab 缩进）是硬规则；
2. .agents/summary/quest-native-dispatch/GOAL.zh-CN.md —— 先读 §0.5「执行现状与勘误」（当前权威现状）；
3. .agents/summary/quest-native-dispatch/README.zh-CN.md —— 车道台账（每片一节，含判据与证据路径）；
4. 四份 W5 退役台账 2026-09-27-w5g{1,2,3,4}-*.zh-CN.md（三范式与 fail-open 点）；
5. .agents/summary/quest-native-dispatch/2026-09-27-w5c-w6-recon-pack.zh-CN.md（取证包，含勘误块）；
6. 记忆库：python3 .agents/memory-bank/search_memory_bank.py "<现象/关键词>" --json 定位 Pattern ID，
   再 --id <PATTERN_ID> 展开单条（不要整篇读领域卡片）。重点：QE-094/095/096（退役三范式）、
   ENV-004（文本编辑/删除纪律）、QE-051（领奖行）、QE-083/089/090/091/092/093（形状守卫族）。

【当前状态（截至 2ab91c0f8）】
- SimpleTalk γ 面（W1–W4）收口：S1/S2/S3a/S3b/S3c-A/S3c-D/S3c-obj（285 链行 + 1323 物件行）。
- W5 已退役四张页码类 TSV：report_pages(5995 行) / entry_pages(2449) / talk_pages(1340) /
  briefing_chains(3230)；`RetailTsvManifestGateTest.EXPECTED_TSV_COUNT` = 22。
- W6 部分完成（shell 编译器退役、命名修正、登记补注）；漂移产物已清理（208 个已入库 + 5 个原始转储）。
- 门禁基线（红身份集，必须逐字节保持）：
    T1 = 87 个测试，红集 1 条，sha256 3b92439da8052988f0acd3a23a646786a78c3ac58e7c64a9c1dcabdd1d5cd137
         （唯一红 = RetailDataDrivenGateTest.driftVersusShellsIsRegistered 的并行车道「在册红」20035）
    T2 = 链选择器 285 id，红集 51 条，sha256 57bb0621b2aa90f861e41358f63baa5450e73753440160ba276bb0255df721fd
    T3 = questEngine 全树 2017 个测试，红集 97 条，sha256 ce4673c74ed61fdcff494fedcc1b8e98c78b252d54b150a05c4bdaa937197870
  基线文件：gates/T1-baseline-reds.txt、gates/T2-w6a-reds.txt、gates/T3-w6a-reds.txt（均在车道 gates/ 下）。

【你的任务：批 0 收尾 → 缺口批（主线）→ 客户端验收批；每批独立验收、独立回滚、指标单调下降】

★【自主推进契约（一次跑完所有阶段；**中途不回报，只在终点交一次总收口报告**）】
  连续执行：批 0 → 缺口批 1–8 → 客户端实机复验清单 → §7.1 完成判据自检；**中间不请示、不逐批汇报**。
  **快速口径（用户 2026-09-27 明确要求：快速推进，不要分片逐个校对）**：
  - 批内可连续改多个片，**不逐片跑门禁**；批末跑一次**快筛**（聚焦门 + 家族门，一次 mvn 合并选择器）；
  - 每批收口**直接落提交**（见下），不写中间报告、不等确认；台账按批合成一篇（不必一微片一篇）；
  - 全量门禁按**里程碑**跑：**M1 = 批0+1、M2 = 批2+3、M3 = 批4+5、M4 = 批6+7+8**；每个里程碑跑一次
    **T1 ∥ T3**（T3 走暖副本、forkCount=4，§6.1），红集与基线逐字节对拍（§2-1③）；
  - **终点必须跑一次最终全量**（T1 + T2 + T3）对拍，这一道不能省——它是 DoD 判据 4 的唯一证据。
  **提交授权（用户已明确授予本阶段）**：每批落**两条提交**——`feat/fix(quest): …`（源码/资源）+ `docs(quest): …`
  （该批台账）；`git add <显式路径>`（**禁 `git add -A`**）、**不 push**、不提交中间产物；两条均以
  `Co-Authored-By: Claude Code <noreply@anthropic.com>` 结尾。
  **任何阻塞一律"记录 + 跳过 + 继续"**，全程不中断、不停下提问：① 需用户客户端实机复验（你无法执行）
  ⇒ 写入"待实机复验清单"继续；② 需改兄弟车道文件（`scriptdll-quest-driver/**`，唯一例外 =
  `affected_quest_tests.py` 的 `T1_GATE_CLASSES` 追加一行）⇒ 登记"移交项"继续；③ 判据与既有裁定冲突
  （如复活被否决方案）⇒ 记"待裁定"继续；④ 需 push / 启停服务 ⇒ 直接不做、记一条继续；⑤ 同一根因连续
  2 次失败 / 单批 3 次尝试无进展 ⇒ 记 BLOCKED（写清已试路径 + 证据）并转下一批。
  **除"工具本身不可用 / 仓库不可写"这类执行环境故障外，没有别的停下条件。**
  全部跑完 ⇒ **只输出一次总收口报告**：缺口逐批曲线（595→0）、各批台账与提交号、T1/T2/T3 红集 sha256、
  BLOCKED / 待裁定 / 移交项 / 待实机复验清单（逐行：任务 id / 复验动作 / 期望现象）。

■ 批 0（收尾，先做，~1.5–2 h；提速口径见 §6）
  R1 `quest_client_dialog_exits.tsv` 行级缩表（**非删除**表）
     - 事实：7 个旗标无整旗标死亡；死面只在行级（singleStep 行 / 非 systemGrant 链行 / 已 canonical 交付段行）；
       `requires(` 调用点 12 处 / 3 文件（测试零调用）。
     - 做法：① 按行 × 旗标做可达性普查（生成器 build_quest_client_dialog_exits.py 与客户端映射同源，
       可在同处加过滤）；② 只删"该行永不被读"的 token，**删 token 不删旗标**；
       ③ 判据 = 缩表前后**家族门定义快照逐键相同**（RetailDataDrivenGateTest / RetailSimpleTalkGateTest /
       RetailSimpleTalkChainGateTest / RetailSimpleCollectItemGateTest / RetailSimpleHuntEquivalenceGateTest /
       RetailSimpleHuntFamilyGateTest / RetailSimpleUseItemGateTest）；④ 表与清单行数不变（不删表）。
     - 禁止：因"交付已接管"就删 SELECT6 / SELECT5_CHECK* / SELECT2_CONTINUE（体级仍读）。
  R2 D 类加窗独立裁定（S3c-D 遗留）：三选项（镜像领奖腿 / 窗落交付 NPC / 维持现状）；
     "就地加窗"已被 QE-092 否决、不得复活；逐行 e2e 证据（契约门 + 路由审计）后裁决入台账。
  R3 W6 残项：EarlyElyos 3 条在册红（1131 started→shugo 交付边 / 1561 CanAct 自环门 /
     1691 spoken-to-diana→returned-to-sneaker，期望形见 w5c-w6-recon-pack §4）、
     `FailurePage` 一名指两页（SELECT6=2716 vs CHECK_USER_ITEM_FAIL=10001）、生成器停写移交登记。
  R4 W7 收口：README 全片台账 / 记忆库 QE 覆盖 / 迁移总量复算。

■ 缺口批（主线；**一轮一个缺口码或同源码族**，按行数从大到小；批次表见 §7）
  每批固定五步，**第 1 步不许跳**：
  1) **根因二分**：把该码的每一行归因到唯一根因，并二分为 (a) **可迁移**（我方解析器/编译器缺一条边或
     一个词汇 → 补实现后 flip）与 (b) **不可迁移**（真端确实无对应语义 → 转"有裁定的保留"，写清证据）；
     判据 = 两类行数之和 == 该码实测行数，且**逐行有证据**（真端表字段 / 客户端 dump / DLL 语义）。
  2) **取证**：只读取证先行（子代理并行；**子代理禁跑 Maven**）；真端表语义 × 客户端 dump（HTML/页册/
     任务书）× ScriptDLL 语义三路交叉；结论必须复核（本项目已两次否决代理结论）。
  3) **生产改动**：单写者串行、锚定式编辑；可迁移行走**受理 flip 三件套**（§2-4：retention + 遗留 XML +
     catalog 同片，并清 `target/` 孤副本）。
  4) **批末快筛 + 提交**：聚焦门 + 家族门**一次合并跑**（~1–2 min）；绿即落两条提交（源码 + 台账）；
     全量 T1∥T3 留给里程碑。出红先按项二分定位（§6 注），修完重跑快筛。
  5) **台账 + 指标**：写**批台账**（一篇可覆盖该批多片）+ 更新 README，并**重算缺口计数**（§7.3），
     把「SEMANTIC_GAP <前>→<后>」写进该批提交说明与台账——**不单独回报**，留到终点总收口报告。

■ 客户端验收批（与缺口批并行，不占服务端门禁）
  - 受理 flip 的行失去逐页审计兜底（§5-5）⇒ flip **前**必须逐行取到客户端证据；flip 后进入"待实机复验"清单。
  - 出口 = 一份给用户的实机复验清单（每行：任务 id / 复验动作 / 期望现象），由用户在客户端执行。

■ 完成判据（全部满足才算"改造完成"）
  ① `retail-xml-retention.tsv` 中 SEMANTIC_GAP:* == 0；
  ② 保留行（670 + 经裁定的不可迁移行）每行 reason 与证据列齐备；
  ③ EXPECTED_TSV_COUNT == 22（无新增页码类 TSV）；
  ④ T1/T2/T3 红身份集与基线逐字节相同（§4）；
  ⑤ 客户端实机复验清单执行完毕、无新增报障。

【每片标准流水线（固定六步）】
1) 事实冻结：读相关台账 + 搜记忆库 + 实测计数（不要把推断当事实）；
2) 生产改动：单写者串行，锚定式编辑（最小唯一锚点 + 断言计数；禁用"往上找最近注释"式启发）；
3) 守卫自检：fail-closed 负例（在仓库外副本里改数据，验证"拒绝编译/下限红"，跑完复原并核 md5）；
4) 聚焦门 + 家族门（命令见 §3）；
5) T1（必跑）；T2/T3 按批次合并跑一次（§3）；
6) 台账（.agents/summary/quest-native-dispatch/2026-09-27-<slice>.zh-CN.md）+ README 章节 + 记忆库
   （sync_memory_bank.py + verify_memory_bank.py，绿了才算收口）。

【长命令纪律（防空等，硬要求）】所有长命令（门禁 / T3 / mvn / rsync）走
  .agents/summary/quest-native-dispatch/tools/gate_bg.sh <日志名> <超时秒> -- <命令...>
**超时秒一律 ≤600 —— 单条命令不得超过 10 分钟**；预计超限的命令**必须先切分**（分片选择器 / 提高 forkCount /
拆多次调用），T3 已按 definition / retail / 其余 **三片**给出命令（§3，每片 ≤600s）。
（nohup+disown+哨兵+看门狗；超时按进程组杀，连带 surefire fork）。**完成判定只看产物**：日志尾部出现
`[DONE] exit=` 或 `BUILD SUCCESS|FAILURE` 或 `Tests run:` 汇总行 + `pgrep -fl surefirebooter` 为空
—— 任一即立刻决策，**不等进程退出事件、禁 `sleep` 轮询**；日志已出结果而 fork 仍悬停就
`pkill -f surefirebooter` 并按已有结果继续。命令一律 `</dev/null`、mvn 一律 `-B`、禁裸 `&`（详见 §3）。

【硬约束】见本包 §2（14 条）；【命令与防空等】见 §3；【基线与路径】见 §4；【陷阱】见 §5（10 条）；
【缺口批全表】见 §7.2；【缺口计数复算】见 §7.3。
```

---

## §2 防漂移约束（硬约束，任何执行体不得违反）

1. **切片判据（每片必用）**：① 指纹/漂移集 **逐行等于该片行集**（零变化片 = 逐行空集）；
   ② 链门/家族门绿；③ **T1/T2/T3 `ADDED 0 / REMOVED 0`**，更强口径 = 红身份集 **sha256 逐字节相同**。
2. **不 push**；**不启停、不重启任何服务器进程**；不新增页码类 TSV。
3. **提交纪律**：提交只在**用户当轮显式授权**后进行；用 `git add <显式路径>`（**禁 `git add -A`**）；
   源码与台账分提交（本仓既有惯例 `feat/fix(quest)` + `docs(quest)`）；提交信息以
   `Co-Authored-By: Claude Code <noreply@anthropic.com>` 结尾；**AI 中间产物（未跟踪的 dumps）不入库**。
4. **受理 flip = 三件套同片**：`retail-xml-retention.tsv` owner 翻转 + **遗留 XML 退役**
   （`quest_definition/quests/<id>.xml`）+ `quest_definition_catalog.xml` 条目删除；
   并**清理 `target/` 孤副本**（否则生产视图继续按"XML 存在"判 owner，级联十几条"生产视图不可读"错误）。
5. **drift/指纹重冻只写本片变化面**：与在册登记不一致的 foreign 行（并行车道「在册红」，代表：
   DD drift 的 `20035`）**必须按原值回置**——把它改绿 = REMOVED 1，违反判据③。
6. **派生生成器重跑前必须备份 + 逐行核差**：`build_retention_list.py` 等会因不消费其他车道的新裁定
   文件而**回退**历史行（实测回退 472 行）；若回退 foreign 行 → 立即还原 + 改用**定向手工编辑**
   （锚定 + 断言）+ 登记"生成器已滞后"移交项。
7. **退役表先落快照**：`static_data/quest_retail/` 已在库，但退役表仍须先复制到
   `.agents/summary/quest-native-dispatch/retired-tsv/<name>.retired-<YYYYMMDD>` 并记 sha256
   （既有 4 张：report_pages / entry_pages / talk_pages / briefing_chains）。
8. **T3 只在仓库外副本跑**：`rsync -a --exclude .git --exclude target`（**`.agents` 必须带上**，
   有测试直接读 `.agents/`）；跑完 `rm -rf` 并确认无残留；**同一棵树同时只跑一个 Maven**；
   shipped 树内不跑全量扫。
9. **兄弟车道只读**：`.agents/summary/scriptdll-quest-driver/**` 的生成器脚本与输入登记表**一字节不改**
   （只读执行生成器可以；其输出若被本片变更牵动，登记"停写/更新"移交项）。例外：
   `affected_quest_tests.py` 的 `T1_GATE_CLASSES` 按既有惯例追加新常设门（每次只追加本片那一行）。
10. **AI 中间产物**只落 `.agents/summary/<topic>/`；禁止 `.agent/`、禁止 `scripts/`；临时 worktree 只在
    必要时创建于仓库外并及时 `git worktree remove --force` + `git worktree prune`。
11. **守卫 fail-closed**：只允许"拒绝编译 + 交裁定"或"测试下限红"；禁止静默丢边/丢页/丢门/丢片。
    守卫改动必须用**变异负例**单独证明可拦性（"重跑逐字节相同"只证可复现，不证可拦）。
12. **不做未经核实的结论**：取证代理/脚本的结论必须复核（本项目已两次否决代理结论：drift 计数口径、
    "briefing 唯一解锁面"）。被否决的结论要**留在台账里**（含被否方案）。
13. **产物清理与保护（2026-09-27 新增）**：清理只删「**全仓按精确文件名零引用** + 瞬态族（漂移/指纹/
    快照/pre-post/`.new`/census/dump/probe 输出/零字节/一次性脚本）」的产物；**以下一律不删**——
    被活脚本消费的 4 个工具链输入表 `item_name_index.tsv`、`npc_name_index.tsv`、`quest_registry.tsv`、
    `m5b2b-quest-event-census.tsv`（`build_retention_list.py` / `p0c11_build_item_name_index.py` / `p0c43_*`
    等直接读取），以及全部 `.md` 台账、`gates/*-reds.txt` 红身份集基线、决策/登记表
    （`*-decisions*`/`*-registry*`/`retention`）、`build_*`/`retire_*`/`refreeze_*` 生成器。
    已入库内容可逐字恢复：`git checkout 5318a8686 -- <path>`（逐条清单见
    `.agents/summary/quest-native-dispatch/cleanup-2026-09-27/`）。
14. **记忆库协议**：任务收口时更新 Pattern（若可复用）、跑 `sync_memory_bank.py` + `verify_memory_bank.py`，
    并在回复末尾附 `[Memory Bank Auto-Updated]` 回执。

---

## §3 命令与"防空等/空转"纪律（提速口径）

**总则（用户 2026-09-27 明确要求）**：**单条命令硬上限 600s（10 分钟）**——`gate_bg.sh` 第二个参数一律 ≤600；
**预计超过 10 分钟的命令必须切分**（分片选择器 / 提高 forkCount / 拆成多次调用），**"超 10 分钟的命令"视为配置错误**，
不是可以商量的慢。长命令**后台跑**；**禁止 `sleep N` 轮询**（查进度看一次日志即可）；Maven 加 `-o -B`；
`forkCount=4`（10 核机）；日志写文件，事后 grep 摘要。

```bash
# 聚焦测试（首选；类列表按本片受影响面选）
mvn -o -B test -Dtest=ClassA,ClassB -DforkCount=2 -Dmaven.test.failure.ignore=true > /tmp/<slice>-focus.log 2>&1

# T1（固定门禁：清单门/身份门/家族门/常设门；forkCount=2 提速）
QUEST_FORK_COUNT=2 QUEST_LOG_DIR=.agents/summary/quest-native-dispatch/gates \
  .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1

# T2（链选择器 285 id；**zsh 必须用 ${=IDS} 分词**，否则选择器工具报 "quest id must be numeric"）
IDS=$(cat /tmp/<ids>.txt)   # 复算：retention owner=RETAIL_TABLE ∩ quest_client_talk_chain_steps.tsv 去重 = 285
QUEST_LOG_DIR=.agents/summary/quest-native-dispatch/gates \
  .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 ${=IDS}

# T3（暖副本/仓库外副本；**只能排除 .git 与 target**，.agents 必须带上）——**分 3 片，每片 ≤600s**
rsync -a --delete --exclude .git --exclude target /Users/mc/IdeaProjects/AionEmu-test/ /private/tmp/aion-t3-warm/
cd /private/tmp/aion-t3-warm
# 片 1/3（definition 包）
mvn -o -B test '-Dtest=com.aionemu.gameserver.questEngine.definition.**' -DfailIfNoTests=false -DforkCount=4
# 片 2/3（retail 包）
mvn -o -B test '-Dtest=com.aionemu.gameserver.questEngine.retail.**' -DfailIfNoTests=false -DforkCount=4
# 片 3/3（questEngine 其余：e2e / runtime / handlers…）
mvn -o -B test '-Dtest=com.aionemu.gameserver.questEngine.**,!com.aionemu.gameserver.questEngine.definition.**,!com.aionemu.gameserver.questEngine.retail.**' \
  -DfailIfNoTests=false -DforkCount=4
# 每片都套 gate_bg.sh（≤600s），日志分别落 gates/T3-<slice>-{1,2,3}.log；
# 红集 = 三片抽取后拼接再排序（与基线对拍的口径不变）：
{ grep -ohE "^\[ERROR\] [A-Za-z0-9_.$]+\.[A-Za-z0-9_]+ -- Time elapsed" \
    .agents/summary/quest-native-dispatch/gates/T3-<slice>-{1,2,3}.log | sed -E 's/^\[ERROR\] //; s/ -- Time elapsed$//'; } \
  | LC_ALL=C sort -u > .agents/summary/quest-native-dispatch/gates/T3-<slice>-reds.txt
rm -rf /private/tmp/aion-t3-warm     # 收口时清零（§6.1 暖副本纪律）

# 红身份集提取 + 对拍（必用 LC_ALL=C sort；locale 排序会造假 ADDED/REMOVED 假象）
grep -oE "^\[ERROR\] [A-Za-z0-9_.$]+\.[A-Za-z0-9_]+ -- Time elapsed" <log> \
  | sed -E 's/^\[ERROR\] //; s/ -- Time elapsed$//' | LC_ALL=C sort -u > gates/<slice>-reds.txt
diff gates/T1-baseline-reds.txt gates/<slice>-reds.txt && echo "ADDED 0 / REMOVED 0"   # T2/T3 同理换基线文件
shasum -a 256 gates/<slice>-reds.txt

# 本片改动面（已提交基线 ⇒ 直接对 diff，不再靠"反向应用"回滚）
git diff --stat 2ab91c0f8 -- src/ | tail -3
git checkout 2ab91c0f8 -- <path>          # 单文件回滚

# 排查导出（只在需要时用）
#   DD 漂移：  -Dretail.dataDriven.equivOut=/tmp/<slice>-drift.tsv
#   DD 指纹：  -Dretail.dataDriven.fpOut=/tmp/<slice>-fp.tsv
#   SimpleTalk 链指纹：-Dretail.talkChain.fingerprintOut=/tmp/<slice>-chain-fp.tsv
#   狩猎/采集等价：-Dretail.hunt.equivOut=/…、-Dretail.collect.equivOut=/…、-Dretail.itemPlay.equivOut=/…

# 记忆库
python3 .agents/memory-bank/sync_memory_bank.py
python3 -B .agents/memory-bank/verify_memory_bank.py   # 期望 MEMORY_BANK_VERIFY_OK STEPS=3
```

### 防空等 / 防空转（**含"已完成却仍在等"的判定规则** —— 本机实测事故）

**三种根因**（都会表现为"测试已经跑完、命令却还在等"）：

| # | 根因 | 症状 | 对策 |
|---|---|---|---|
| ① | 命令等 stdin / 等 pager | `git log` 起 `less`、命令读标准输入 | `</dev/null`、`--no-pager`、`PAGER=cat` |
| ② | **mvn 的 surefire fork 悬停** | 日志已打印 `Tests run:`，JVM 不退，`mvn` 不退出 | 超时看门狗（按进程组杀） |
| ③ | 后台进程继承 stdout 管道 / 未 `disown` | 主进程早已结束，调用方收不到"结束"事件 | `nohup … </dev/null >log 2>&1 &` + `disown` |

**规则（每次跑长命令前后自检）**

1. **完成判定只看产物，不等进程事件**：日志尾部出现下列**任一**即可立即决策——
   `[DONE] exit=`（哨兵行）、`BUILD SUCCESS|FAILURE`、或 `Tests run:` 汇总行 **且** `pgrep -fl surefirebooter` 为空。
2. **长命令一律走本车道工具**（2026-09-27 新增，已冒烟验证）：
   ```bash
   # 后台不阻塞：nohup + disown + 哨兵 + 超时看门狗（超时按进程组杀，连带 surefire fork）
   .agents/summary/quest-native-dispatch/tools/gate_bg.sh <日志名> <超时秒> -- <命令...>
   # 前台但带超时与哨兵：
   python3 .agents/summary/quest-native-dispatch/tools/gate_run.py <超时秒> <日志路径> <命令...>
   ```
   退出码：命令退出码；超时 = **124**（日志尾部同样有 `[DONE]`，可判定为红+留痕）。
3. **非交互化**：命令加 `</dev/null`；`git --no-pager`；`PAGER=cat GH_PAGER=cat`；禁 `| less`、
   禁交互式 `rebase`；`mvn` 一律 `-B`。**禁裸 `&`**（必须 `nohup … &` + `disown`）。
4. **只允许一次检查**：收到"疑似完成"信号后做一次 `tail -3 <log>` + `pgrep -fl surefirebooter`，
   随即决策；**禁 `sleep N` 轮询**、禁反复 `tail`。
5. **悬挂 fork 处置**：若日志已出 `BUILD`/`Tests run` 而 fork 仍在 ⇒ `pkill -f surefirebooter`，
   将该次结果**当作最终结果**使用并写入台账（不再重跑）。
6. T1/T2/T3 能合并就合并（同一次收口只跑一遍 T3）；副本 rsync（2.1G 级）一律后台。
7. 删除源文件后只清 `target/` 对应孤副本（**不要 `mvn clean`**，太慢）。
8. **子代理不得运行 Maven、不得跨 session 提权**（取证代理只读）。

---

## §4 关键路径与基线（速查）

| 项 | 值 |
|---|---|
| 仓库 | `/Users/mc/IdeaProjects/AionEmu-test`（分支 `quest`） |
| **基线提交** | 启动时的 `HEAD`（写作时 `837e4c58e`）← 2ab91c0f8（输入表）← d1806b476（交接包/工具）← b5d2a7dea（产物清理）← 5318a8686（台账/工具链/证据）← 4ede058c0（源码迁移） |
| 远端状态 | 领先 `origin/quest` **25 个提交，未推送**（不要 push） |
| 工作树 | 应**干净**；4 个工具链输入表已入库、不得删（§2-13） |
| 车道台账 | `.agents/summary/quest-native-dispatch/`（GOAL §0.5 / README / 各片台账 / gates/ 红集与日志） |
| 冻结面门 | `RetailTsvManifestGateTest`（磁盘集合 == 清单集合 == `EXPECTED_TSV_COUNT`，**现值 22**） |
| 新常设门 | `RetailBriefingChainEvidenceGateTest`（简报链不变量，登记 3225 + 基数 47/5 冻结） |
| T1 红集基线 | `gates/T1-baseline-reds.txt` = 1 条，sha256 `3b92439da8052988f0acd3a23a646786a78c3ac58e7c64a9c1dcabdd1d5cd137` |
| T2 红集基线 | `gates/T2-w6a-reds.txt` = 51 条，sha256 `57bb0621b2aa90f861e41358f63baa5450e73753440160ba276bb0255df721fd` |
| T3 红集基线 | `gates/T3-w6a-reds.txt` = 97 条，sha256 `ce4673c74ed61fdcff494fedcc1b8e98c78b252d54b150a05c4bdaa937197870`（2017 个测试） |
| 长命令工具 | `.agents/summary/quest-native-dispatch/tools/gate_bg.sh`（后台+哨兵+看门狗）、`tools/gate_run.py`（前台版）；见 §3 防空等 |
| 快照目录 | `.agents/summary/quest-native-dispatch/retired-tsv/`（4 张退役表的 sha256 快照） |
| 清理记录 | `.agents/summary/quest-native-dispatch/cleanup-2026-09-27/`（判据 + 逐条清单 + 恢复路径） |
| 记忆库入口 | `.agents/memory-bank/README.md` + `search_memory_bank.py`（`index.jsonl` 由 sync 生成，禁止手改） |

---

## §5 已知陷阱（本项目实测过，别再踩）

1. `zsh` 不按空格分词：`run_quest_gates.sh T2 $IDS` 会把整串当一个 id → 用 `${=IDS}`。
2. `grep -c <码> file` 会把**注释行**也计入（`drift.tsv` 的 "6" 是 6 数据行 + 1 注释行，注释是对的）。
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
9. **清理产物的边界**：只删「零引用 + 瞬态族」；生成器输入表与红集基线一旦删掉，
   工具链与门禁对拍会立刻断链（4 个工具链输入表是活依赖，删了 `build_retention_list.py` 直接报错）。
10. **"跑完还在等"不是必然要等**：日志出现 `Tests run:` / `BUILD …` / `[DONE]` 就是**判定已到**，
    不是"再等等看进程退没退"；悬挂的 surefire fork 直接 `pkill -f surefirebooter` 并按已有结果决策
    （§3 防空等表：三种根因与对策）。**禁 `sleep` 轮询**——一次 `tail` 即决策。

---

## §6 提速方案（批次化，替代"一片一跑"）

**成本结构（实测）**：单片 ≈ 门禁 30 min（家族门 5–6 min + T1 6–10 min + T3 20–25 min）+ 分析/编码 15 min。
**四个杠杆（不放松任何判据）**：

1. **合并批次**：零行为的项（重命名 / 登记 / 文档）并入形状片，**同一次门禁**验收；
2. **门禁并行**：T1（主树）∥ T3（副本，含 `.agents`）同时跑；`QUEST_FORK_COUNT=3~4`（10 核机实测可行）；
3. **分级门禁**：形状批 = 聚焦 + T1 + T3；**零行为批** = 聚焦 + T1（**免 T3**）；
   **T2 一般可省**（T3 覆盖 questEngine 全树，T2 的类集 ⊂ T3 ∪ T1）；
4. **分析并行**：只读取证（子代理，禁 Maven）与编码同时进行；取证结论进台账。

**推荐批次（剩余 R1–R4）**：

| 批 | 内容 | 门禁 | 预估 |
|---|---|---|---|
| **A（形状片）** | R1 `dialog_exits` 行级缩表（行级普查脚本 + 家族门定义快照对拍） | 聚焦 + T1 ∥ T3（1 轮，T3 分 3 片） | ~25–30 min |
| **B（零行为，与 A 同批）** | R3：`FailurePage` 命名（跨文件重命名）+ 生成器停写移交登记 | 复用 A 的门禁 | +0 |
| **C（裁定片）** | R2 D 类加窗（先取证 → 裁定 → 可能含 EarlyElyos 3 条断言重锚） | 聚焦 + `RetailQuestContractTest` + T1 ∥ T3 | ~60 min |
| **D（文档批）** | R4 W7 收口（README / 记忆库 / 迁移总量复算） | 无 Maven；`sync_memory_bank.py` + `verify_memory_bank.py` | ~20 min |

⇒ 门禁轮数 **5 → 2**；剩余总时长预估 **~4–5 h → ~1.5–2 h**。

### §6.1 追加三杠杆（实测瓶颈：家族门 5–6 min 占 T1 九成；T3 全量 20–25 min，**按 ≤600s 规则分 3 片跑，见 §3**；每次 mvn 启动/资源拷贝 30–60 s）

1. **暖副本（T3 增量跑）**：持久化一个副本 `/private/tmp/aion-t3-warm`，**首次**全量 rsync + `test-compile` 暖一次；
   之后每次只 `rsync -a --delete --exclude .git --exclude target <repo>/ /private/tmp/aion-t3-warm/`
   （**target 被 exclude ⇒ 保留暖编译产物**），再跑测试 ⇒ 省掉每次 3–4 min 全量编译。
   纪律：副本在仓库外；**工作全部结束时必须 `rm -rf`**（本工程 sprint 模式下的唯一放宽，收口时清零）。
2. **T1-fast（批次级快筛）**：T1 的 87 个测试里 `RetailSimpleHuntFamilyGateTest` 独占 ~6 min ⇒ 批次内跑
   **T1 类表 − 家族门**（~1–2 min），家族门留给**里程碑 T3**（T3 ⊇ 它）。红集对拍时按类名过滤本批次日志即可保持口径。
3. **单次 mvn 合并选择器**：把"T1-fast + 本片聚焦类 + T2 选择器"并成一个 `-Dtest=` 列表跑一次
   （一次编译一次启动），比分开跑 3 次省 ~2–4 min；日志事后按类名过滤即可产出各档红集。
4. **`-DforkCount=4`**（10 核机）：T2/T3 实测可从 forks=2 再压 30–40%；T1-fast 受益小（类少）。

**极限计划（剩余全部工作一次门禁周期）**：
```
批 A+（一次后台跑）：R1 dialog_exits 缩表 + R3 FailurePage 命名 + 生成器移交登记
    → 一次 mvn：T1-fast + 聚焦(7 门) + T2 选择器，forkCount=4        ≈ 6–8 min
    → 逐项 patch 记录（出红按项二分）
R2（裁定）：先只读取证；若裁定 = 维持现状 ⇒ 零门禁；若需改形状 ⇒ 追加一次门禁周期
里程碑：暖副本 T3（forkCount=4）                                       ≈ 10–12 min
R4（文档）：无 Maven；记忆库 sync+verify                                ≈ 20 min
```
⇒ 剩余总时长可压到 **~1–1.5 h**（相对最初单片节奏 ~4–5 h）。

> 注：批次内改动要**逐项留独立 patch 记录**；基线已提交，"回滚" = `git checkout 2ab91c0f8 -- <path>`
> 或反向应用该项改动，这样某批若出红，可按项二分定位，不必重跑整批。

---

## §7 完成改造标准与缺口批计划（R5 系列 · 595 行主线）

### 7.1 完成判据（Definition of Done，全部满足才算"改造完成"）

| # | 判据 | 现状（起点 `2ab91c0f8`） | 终点 |
|---|---|---|---|
| 1 | `retail-xml-retention.tsv` 中 `SEMANTIC_GAP:*` 行数 | **595** | **0** |
| 2 | 保留行每行有 reason + 证据 | 670（494 SCRIPTED + 176 NO_TABLE）已有；595 缺口行缺 | 670 + 经裁定的"不可迁移"行，逐行有证据 |
| 3 | `RetailTsvManifestGateTest.EXPECTED_TSV_COUNT` | 22 | 22（只减不增；不新增页码类 TSV） |
| 4 | T1/T2/T3 红身份集 | 1 / 51 / 97 条，sha256 见 §4 | 与基线**逐字节相同** |
| 5 | 客户端实机复验（受理 flip 行） | 多批"复测未做" | 复验清单执行完毕、无新增报障 |

**进度的唯一硬指标 = 判据 1 的数（595 → 0），每批收口后重算并回报。**
推进方式见 §1「自主推进契约」：**一次跑完所有批次，不逐批请示**；**任何阻塞只记录不中断**（含需用户
实机复验、需改兄弟车道、判据冲突、连续失败），收口时集中列清单。

### 7.2 缺口码全表（实测 595 行 / 37 码）与批次归并

| 批 | 缺口码（行数） | 小计 |
|---|---|---|
| **1 哨兵接取族** | `ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH` 84、`ACQUIRE_NPC_SENTINEL_AREA_PENDING` 64、`ACQUIRE_NPC_UNRESOLVED` 25、`ACQUIRE_GRANT_UNSUPPORTED` 17、`ACQUIRE_NPC_SENTINEL` 2 | **192** |
| **2 击杀/狩猎族** | `MONSTER_UNRESOLVED` 69、`KILL_COVERAGE_LOSS` 35、`HUNT_MULTI_STAGE_DEFERRED` 4 | **108** |
| **3 闲聊链族** | `TALK_CHAIN` 25、`TALK_HUNT_CHAIN_DEFERRED` 24、`TALK_CHAIN_DEFERRED` 23、`TALK_CUTSCENE` 12、`TALK_CHAIN_NO_START` 7、`TALK_JOURNAL_MISSING` 5、`TALK_CHAIN_COMPOUND` 5、`TALK_COLLECT_CHAIN_DEFERRED` 3、`TALK_NPC_UNRESOLVED` 2 | **106** |
| **4 交付/制造/步骤族** | `HANDIN_VOCABULARY_UNSUPPORTED` 48、`CRAFT_AXIS_UNDECLARED` 28、`STEP_UNSUPPORTED` 18 | **94** |
| **5 客户端契约族** | `CLIENT_REPORT_VARIANT` 17、`CLIENT_BUTTON_UNWIRED` 11、`CLIENT_ROUTE` 11、`REPORT_NPC_DIVERGENCE` 1 | **40** |
| **6 领奖/计数族** | `COUNTER_EXCEEDS_6BIT` 14、`REWARD_NPC_UNRESOLVED` 13、`REWARD_NPC_FACTION_COMPOSITE` 4、`REWARD_NPC_NPC_UNRESOLVED` 2 | **33** |
| **7 余量** | `ENTERAREA_ZONE_UNRESOLVED` 4、`ITEMPLAY_ACQUIRE_EVENT_DEFERRED` 3、`QUEST_SPAWN_UNEXPRESSED` 2、`ADVANCE_UNEXPRESSED` 2、`COLLECT_ITEM_SHAPE` 2、`WORK_ITEM_GRANT_AND_COLLECT_ROUTE` 1、`XML_EXTRA_REWARD` 1、`COLLECT_PROGRESS_ROUTE` 1 | **16** |
| **8 裁定族** | `CURATED_LEGACY_CONTRACT_LOCK` 6（逐行裁定：迁 or 有理由保留） | **6** |
| | | **595** |

**按家族**：DataDriven 290 / SimpleHunt 157 / SimpleTalk 134 / SimpleItemPlay 9 / SimpleCollectItem 3 / SimpleUseItem 2。

**批次顺序可变**：某码若"根因二分"显示大头是 (b) 不可迁移，该批可一夜关闭（全部转有裁定保留）；
反之若 (a) 可迁移占多数，走 flip 三件套。**允许批次间行提前解锁，按重算结果重排。**

### 7.3 缺口计数复算（每批必跑，指标来源）

```bash
R=src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv
grep -c 'SEMANTIC_GAP' "$R"                                  # 总缺口（起点 595）
awk -F'\t' '!/^#/ && $4 ~ /^SEMANTIC_GAP/ {print $4}' "$R" \
  | sort | uniq -c | sort -rn                                 # 按码分解（起点 37 码）
```

**派生视图纪律**：retention 由 `build_retention_list.py` 派生（且**滞后于 drift**）⇒ 每批只改本批行、
跑前备份 + 逐行核差，禁整表重跑（ENV-004 规则 5；实测一次回退 472 行）。

### 7.4 与既有轴的关系

- **不新增页码类 TSV**；已退役四张保持退役；`dialog_exits` 只缩表（批 0-R1）。
- **Handin 例外面**：`RetailHandinDialogFlowCompiler`（223 行 DD 行）三票否决、明确排除迁移，属
  "有理由的例外"；批 4 的 `HANDIN_VOCABULARY_UNSUPPORTED`（48 行）是否与它同源，先在该批根因二分里判定。
- **客户端验收**与缺口批并行推进：flip 前取客户端证据、flip 后进实机复验清单（§5-5 陷阱）。
