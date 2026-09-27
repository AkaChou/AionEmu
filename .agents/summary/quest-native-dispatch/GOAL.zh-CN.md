# 终局目标书（Goal / Definition of Done）· 车道 quest-native-dispatch

> 用途：作为 Claude Code workflow / 自主执行的**目标书**（可整体作为 goal prompt 或工作流的 meta.description）。
> 状态：2026-09-27 冻结（数字为当次实测）；执行期以「变化面 == 缺陷面」逐片推进，每片独立验收、独立回滚。
> 起点导航见 §6；验收命令见 §7；硬约束见 §5。

## 0. 一句话目标

**让全部"可迁移"任务由真端表在加载期合成生命周期分发（RETAIL_TABLE），停止扩建任何页码类 TSV 补丁，
并把已死的页码类 TSV 退役掉；剩下来的 XML 保留行必须逐行有登记理由。**

## 0.5 执行现状与勘误（2026-09-27 22:40 收口后，**下一位执行体先读本节**）

**已完成**（全部有台账 + T1/T2/T3 红集恒等证据）：

- **W1–W4（SimpleTalk γ 面）**：S1/S2/S3a/S3b/S3c-A/S3c-D/S3c-obj 全片收口（285 链行 + 1323 物件行）。
  **W2 的实际执行 = S3c-D 报告页退场（39 行）**——本文 W2 行字面的「(iii) 就地加窗」经裁定**不成立**
  （QE-092：中间人翻面按当前对话 owner 路由，在翻面 after 挂窗 = 下发一页按钮无路由的窗，只有 e2e 契约门拦得住），
  **不得照字面执行**。
- **W5**：四张页码类 TSV 已退役——`report_pages`(5995 行, g1) / `entry_pages`(2449, g2) / `talk_pages`(1340, g3) /
  `briefing_chains`(3230, g4)；`EXPECTED_TSV_COUNT` **26 → 22**。
- **W6 部分**：`RetailSimpleHuntIrCompiler` 退役 + 命名反义修正 + rejected.tsv 登记（W6-a）。
- **记忆库**：QE-092..096；`MEMORY_BANK_SYNC_OK ENTRIES=141`、`VERIFY_OK STEPS=3`。

**W5 判据升级（g3/g4 新增两类退役范式，取代本文 W5 行"先出死分支证明"的单一口径）**：

| 范式 | 适用 | 动作 | 判例 |
|---|---|---|---|
| 纯机械退役 | 值死 + 门死（入口重载零调用者） | 删读取者/穿线/表/清单行，计数 −1 | g1、g2（QE-094） |
| **活门退役** | 值死但门活 | **先派生可派生判据**（在常设生产表里找，判例 = 任务书行）+ 非回归计数 + 门与值同片改 | g3（QE-095） |
| **零拒绝守卫退役** | 值死门活但**今日零拒绝** | 不变量迁**构建期常设门**（冻结快照 + 基数冻结 + 注册 T1） | g4（QE-096） |

**剩余工作（下一位执行体的候选，按风险从低到高）**：

| # | 项 | 依据 | 完成判据 |
|---|---|---|---|
| R1 | **`dialog_exits` 行级缩表**（非删除） | 四路取证：**无整旗标死亡**；死面只在行级（singleStep 行 / 非 systemGrant 链行 / 已 canonical 交付段行） | 缩表前后**家族门定义快照逐键相同**；表与清单行数不变 |
| R2 | **D 类加窗独立裁定**（S3c-D 遗留） | `2026-09-27-s3-adjudication-brief.zh-CN.md` + S3c-D 台账（(iii) 已否决） | 三选项（镜像领奖腿 / 窗落交付 NPC / 维持现状）逐行 e2e 证据 |
| R3 | **W6 残项**：EarlyElyos 3 条在册红 / `FailurePage` 一名指两页（SELECT6=2716 vs CHECK_USER_ITEM_FAIL=10001）/ 生成器停写移交 | `2026-09-27-w5c-w6-recon-pack.zh-CN.md` §4 | 逐条关闭 + 台账 |
| R4 | **W7 收口**：README/记忆库/迁移总量复算 | 本文 §3 W7 | 台账齐 + 记忆库绿 |

**勘误（以本节为准）**：

- 本文 W5 行把 `quest_use_item_npcs.tsv` 称"空表"——**过期**：实测 **808 行**且有多消费者，**必须保留**。
- 本文 W5 行"每张先出死分支证明"——**不完整**：按上面三类范式的适用面分别处理。
- `2026-09-27-tsv-retirement-candidates.zh-CN.md` 的 "清单 27 行"/"briefing 唯一会解锁" ——**已勘误**（该文档顶部勘误块）。
- `static_data/quest_retail/` **未纳入 git** ⇒ 每张退役表先落快照 `retired-tsv/<name>.retired-<日期>` + sha256（g2 起执行）。


## 1. 验收终态（四条，全部可机检）

| # | 终态 | 机检判据（命令/文件） |
|---|---|---|
| **F1 迁移终态** | 目标集内**无一行因"有 XML"而保留**；保留行逐行属 {计划保留 / SEMANTIC_GAP 有登记理由} | `retail-xml-retention.tsv` 每行 `owner,family,reason,evidence` 非空；`RetailOwnershipGateTest` + T1 绿；总量复算 = 4958+1266=6224（基线），可迁移比 ≥ 89.3% |
| **F2 形状终态**（主战场） | RETAIL_TABLE 面**不再有任何行消费 legacy 页链补丁** | `RetailSimpleTalkDefinitionCompiler` 的 legacy 分支（`acceptFlowChain`/`reportFlowChain`/`itemReportGate`/`acceptContinuation`）**无调用点**；链门 6/6 + 家族门 4/4；DD 四子面 / SimpleTalk 全部收口；**HandinDialogFlow 明确排除**（三票否决） |
| **F3 TSV 终态** | 页码类 TSV **只减不增**，且候选表实际退役（manifest 出现并落定 `RETIREMENT_CANDIDATE` → 删行删文件） | `quest-retail-tsv-manifest.tsv` 中页码类表的 `READERS=0`（或消费方已换规范形）+ `EXPECTED_TSV_COUNT` 下降；`RetailTsvManifestGateTest`（在 **T1 固定门禁**）绿 |
| **F4 质量终态** | 每片"零新增失败"且**红集集合恒等**（更强口径：sha256 逐字节相同） | T2（285 选择器）与 T3（仓库外全树副本）各 **ADDED 0 / REMOVED 0**；无新增 TSV；无 worktree/临时产物残留；记忆库 QE 覆盖每个可复用机制 |

## 2. 终态的四条铁律（判定纪律，任何执行体不得违反）

1. **真端为唯一权威**：形状/逻辑取真端模板表 + 真端 `quest.xml` + ScriptDLL 语义；旧 XML / Java handler 只作**证据**。
2. **变化面 == 缺陷面**：每片重冻指纹（`-Dretail.talkChain.fingerprintOut`），**漂移集必须逐行等于该片行集**（判例：S2 203/0/0、S3a 60/0/0、加固片 ∅、S3b 7/0/0）。
3. **fail-closed**：守卫只允许"拒绝编译 + 交裁定"，禁止静默丢边/丢页/丢门/丢片（QE-086/087/088/089/090 即五条已沉淀的口径）。
4. **判据看 retention owner**：断言重锚与形状判定一律先查 `retail-xml-retention.tsv`（XML_RETENTION 行保持 legacy 形，判例 21081）。

## 3. 剩余工作分解（WBS，含完成判据）

| # | 工作 | 行集/对象 | 前置 | 完成判据 |
|---|---|---|---|---|
| **W1** | S3c-A 交付段同构替换 | 30 行（A1 12 / A2 9 / A3 2 / A4 2，见 `2026-09-27-s3c-a-prep.zh-CN.md`） | 三前提①G-4 加"被承担"分支②`retiredChainGate` 读 `I` 记录③`canonicalDelivery` 解耦为独立谓词 | 指纹 **30/0/0** + 链门/家族门绿 + T2/T3 0/0 |
| **W2** | S3c-D 交付段（iii）就地加窗 | 50 行（含 4a 最薄子切片 5 行：翻转边已带窗） | W1 的③；两红线：**terminal `CLOSE` 必须替换而非追加**、报告页退场用 **R-REP 页下发判据**（不得用动作词表） | 指纹 **50/0/0** + 新断言（每行恰 1 条窗口下发边且窗口在 terminal CLOSE 位、退场对象 39 条） |
| **W3** | `1323` 物件接取变体 | 1 行 | `canonicalAcceptFlow` 增入口事件参数（或按"物件哨兵接取者"全绑物件）+ `GIVE_ITEM` 移到提交边 | 指纹 **1/0/0** + 客户端页可达（入口页/接取窗/拒绝页三页有路由） |
| **W4** | 收口验证 | 全 SimpleTalk 链 285 行 + 全树 | W1–W3 | 链门 6/6、家族门 4/4、契约门（`RetailQuestContractTest`）绿、T1/T2/T3 零新增 |
| **W5** | TSV 退役审计 + 执行（**g1/g2/g3/g4 已完成，仅剩 dialog_exits 缩表**） | WP6 候选 5 张（report_pages / entry_pages / talk_pages / briefing_chains / dialog_exits）+ 无消费者表（`retail-quest-ai-name-groups-rejected.tsv`；**勘误：`quest_use_item_npcs.tsv` 非空表（808 行有消费者），必须保留**——见 §0.5） | W4（消费方已换规范形才可退） | 每张先出**死分支证明**（逐调用点），再删文件 + 删清单行 + 改 `EXPECTED_TSV_COUNT`；T1/T3 绿 |
| **W6** | 清理 | `RetailSimpleHuntIrCompiler`（零生产引用）、`EarlyElyos` 3 条在册红、`FailurePage` 命名、`retail-quest-ai-name-groups-rejected.tsv` 生成器登记 | 无 | 清单逐条关闭 + 台账留痕 |
| **W7** | 收口文档 | README + 记忆库 + 迁移总量复算 | W1–W6 | 车道 README 全片台账齐；QE 覆盖新增机制；`sync_memory_bank.py` + `verify_memory_bank.py` 绿 |

**建议执行序**：W1 →（W3 可与 W1 并行，互不依赖）→ W2 → W4 → W5 → W6 → W7。
**每片的固定流水线**：事实冻结（指纹/选择器）→ 生产改动 → 编译期守卫自检（重冻 + problems=[]）→ 链门/家族门 → T2（树内）+ T3（仓库外副本，**并行**）→ 若有红则分拣波（子代理，只改测试、不跑 Maven）→ 台账 + 记忆库。

## 4. 工作流编排建议（供 Workflow / 多代理执行）

- **阶段**：`P0 事实冻结`（只读：指纹、选择器、`s3-gamma-rows.tsv`）→ `P1 生产改动`（单写者，串行）→ `P2 门禁`（T2 树内 ∥ T3 副本）→ `P3 分拣`（扇出：每个红类一个代理，**禁止跑 Maven**）→ `P4 收口`（台账/记忆库/README）。
- **并行纪律（硬）**：
  1. **同一个仓库树内同一时刻只允许一个 Maven**；T3 一律在**仓库外全树副本**（`rsync --exclude .git --exclude target`）跑、跑完即删；
  2. 分拣/研究类代理**只改测试或不改文件**，Maven 由主执行体统一跑；
  3. 子代理产物只落 `.agents/summary/quest-native-dispatch/`；禁 `.agent/`；禁仓库内 worktree。
- **不 commit**（沿用用户"先不提交"），除非用户显式授权；授权后按 `AGENTS.md` 的"沉淀文档随提交"整篇暂存。

## 5. 硬约束（执行体必须遵守）

1. 不 commit / 不 push（除用户显式授权）；不启动/停止/重启服务进程；
2. **不新增页码类 TSV**（manifest 冻结 + `RetailTsvManifestGateTest` 在 T1 门禁）；页码类 TSV **只允许退役**；
3. Maven 仅用于聚焦测试与 T1/T2/T3 门禁；`surefire:test` 不编译（会验旧字节码，禁用）；
4. 兄弟车道输入（`quest_client_talk_chain_steps.tsv` 等）**只读**，一字节不改；
5. 中间产物进 `.agents/summary/<topic>/`（禁 `scripts/`）；临时探针用完即删（源 + `.class`）。

## 6. 起点导航（执行体先读这些，再动手）

1. `.agents/summary/quest-native-dispatch/README.zh-CN.md`（全片台账 + 下一面）；
2. `2026-09-27-s3-adjudication-brief.zh-CN.md`（S3c 裁定与切片表，§1-① 推荐 (iii)、§3 R1–R11 风险）；
3. `2026-09-27-s3c-a-prep.zh-CN.md`（A 类四子形 + 三前提）；
4. `2026-09-27-s3-guard-hardening.zh-CN.md` / `2026-09-27-s3b-r-driven-accept.zh-CN.md` / `2026-09-27-s3a-accept-segment.zh-CN.md`（已落地的机制与开发期修正）；
5. `.agents/memory-bank/patterns/quest-engine.md` 的 **QE-086..QE-090**（过场重挂 / 规范段过滤 / 按段接管 / 守卫四轴 / 层 A+合成）+ `python3 .agents/memory-bank/search_memory_bank.py "<现象>" --json`；
6. 生产入口：`src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkDefinitionCompiler.java`（链式）与其单步同族；客户端页-按钮映射只读参考 `docs/quest/client-dialog-mapping/`。

## 7. 验收命令（Maven 已在本车道既有授权范围内用于聚焦测试与门禁）

```bash
# 链门 + 家族门（每片必跑）
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest,RetailSimpleTalkGateTest -DforkCount=1

# 指纹重冻（分片验收：漂移集必须逐行 == 切片行集）
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest -Dretail.talkChain.fingerprintOut=/tmp/<slice>-fp.tsv -DforkCount=1

# T2（树内，285 选择器）
QUEST_LOG_DIR=.agents/summary/quest-native-dispatch/gates QUEST_FORK_COUNT=2 \
  .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 \
  $(awk -F'\t' '!/^#/ && NF>1 {print $1}' \
    src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv | sort -u)

# T3（仓库外全树副本；跑完即删）
rsync -a --exclude .git --exclude target <repo>/ /private/tmp/aion-t3-<slice>/
cd /private/tmp/aion-t3-<slice> && QUEST_LOG_DIR=<repo>/.agents/summary/quest-native-dispatch/gates \
  QUEST_FORK_COUNT=1 .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T3
# 对拍：统一正则提取两侧红身份 + LC=C 集合运算，要求 ADDED 0 / REMOVED 0（更强口径：sha256 集合恒等）
```

## 8. 终态自检清单（收口时逐条打勾）

- [ ] F1：retention 每行有 reason/evidence；迁移总量复算 ≥ 89.3%；无"因有 XML 而保留"的目标集行
- [ ] F2：SimpleTalk legacy 分支无调用点；链门/家族门/契约门绿；HandinDialogFlow 仍按排除态登记
- [ ] F3：页码类 TSV 的 `EXPECTED_TSV_COUNT` 已下降；每张退役表有死分支证明；无新增 TSV
- [ ] F4：末片 T2/T3 ADDED 0 / REMOVED 0；QE 覆盖齐全；无 worktree/临时残留；README 与记忆库同步
