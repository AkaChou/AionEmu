# P0c-9：SimpleHunt 族收口对账（23 行旧码残留裁定 + 职业可选奖励归零 + 族级分歧表）

- 日期：2026-09-24
- 归属：真端任务驱动替换目标（P0c 线，v3 提示词 §4 P0c-9）
- 触发：P0c-8c 后族面还有一处**未进过逐行裁定的残留**——`phase5-3-rejections.txt` 23 行
  （P0c-8a 差异轴普查里与 275 行缺口表同轴：20 行转换多重集差异 + 3 行节点集差异，
  但登记在旧表、从未过 8b/8c 判据）；加上 v3 点名的 42 行缺口复核与 `CLASS_SELECTABLE_REWARD`
  唯一可归零路径。本切片把 SimpleHunt 推进到**族口径闭环**：942 行每行有 owner、稳定码与可重跑证据。

## 1. 交付

| 文件 | 作用 |
|---|---|
| `p0c9_simplehunt_family_closure.py` | **族收口对账脚本（可重跑）**：942 行三分区 + 四桶互斥并集 + 磁盘/catalog 对账 + 裁定表互斥 + 三源码一致性断言；产出 `p0c9-family-reconciliation.tsv`（逐行）与 `p0c9-family-divergence-table.tsv`（码→行数→可否归零→路径） |
| `p0c9_retire_rows.py` | 落地执行器（复用 `p0c8c_gap_decisions.py` 判据机，禁止二套判据）：断言两批 0 UNRESOLVED → 写 owner 记录 → 追加生产裁定表 → 删 XML + 同步 catalog → 重写缺口表/phase5-3 表 → 追加 p0c6 归一化登记 |
| `p0c9_keep_recheck.py` | 47 行 KEEP 复核留证：缺失可达怪逐 id 对照 `quest_script_monster.csv`（服务端族表外唯一候选源）+ 客户端按钮动作集逐行列出 |
| `p0c9-phase53-shape-census.tsv` / `p0c9-class-select-census.tsv` | 两批形状普查（探针 `P0c9GapShapeProbeTest`，源码留档 `p0c9_gap_shape_probe.java.txt`，本体已删；新增第 16 列 `classRoutesRetail` 与 LIMIT 80→1000——11102/28313 的差异条目 176/435 条会截断出伪差） |
| `p0c9-decisions.tsv` | 25 行 owner 记录（batch=phase53/class-select，verdict/basis/evidence 含机制计数） |
| 判据机增量 `p0c8c_gap_decisions.py` | +3 个机器类别：`BRIEFING_ROUTE_RETAIL`（真端简报自环下发**客户端登记页**）、`CONFIRM_FROM_START_LEGACY`（旧 XML 从击杀态按确认 id 直跳领奖——客户端非领奖态发不出这些 id）、`LEGACY_SAVE_NORMALIZATION`（EnterWorld 自愈边，P0c-6 先例）；`CLASS_SELECTABLE_REWARD` 短路改为**仅在真端侧未落地职业路由时触发**（普查新列）；`--census/--out` 参数化 + REJECTED/短行保护（绝不误判 ADOPT） |

## 2. 结论

1. **23 行 phase5-3 残留全部裁定（0 UNRESOLVED）**：ADOPT_RETAIL 16（退役）+ KEEP_XML 7
   （全部 `KILL_COVERAGE_LOSS`，并码入缺口表——如 30318/30320 家族"客户端点名 216195..216202
   且生产刷怪可达、真端编译集缺"）。旧码 `KILL_ROUTE_MISMATCH`/`NODE_SET_MISMATCH`/`NODE_FIELDS_MISMATCH`
   就此清空：这些是 Phase5-3 时代导出的旧口径名，差异本体与缺口表同轴，逐行机制归类后全部落进
   ADOPT 判据（`CONFIRM_RANGE` / `ACCEPT_NPC_DRIFT` / `KILL_VARIANT_UNREACHABLE` / `GRID` 等）或并码 KEEP。
2. **`CLASS_SELECTABLE_REWARD` 归零（族内 2 行 → 0）**：根因是**真端表方言差异**——
   三个技术职业的奖励块用短名标签 `gunner_/bard_/rider_selectable_reward`（全库 90 任务短名+长名成对、
   零冲突），而 `RetailQuestMetadataCompiler.CLASS_REWARD_TAGS` 只映射长名 ⇒ 11102/28313/18313 的
   技术职业奖励被静默丢弃。修复两层：
   - 元数据层：+3 短名别名（映射到同一 class id；`RetailItemNameIndex` 大小写不敏感，`Stigma_RI_TuneSensor_G1` 正常解析）；
   - 合成层：`completeFlow` 在无普通可选项且 `classRewards` 非空时，确认段（dialogId 8..23）按
     `AdvancedClassIs` 职业条件展开（11 职业 × 确认段），动作 = 固定奖励 + 该职业物品 + CompleteQuest，
     与 `QuestXmlBlockExpander` 的 npc-complete CLASS 形同构（`classRewardKey` 同映射）；
     同职业多物品的同事件转换按物品序号给唯一优先级（过 `AMBIGUOUS_TRANSITION` 结构校验；单物品无优先级）。
   落地后 11102/28313 与旧 XML 逐条配对（`order` 档吸收优先级写法差），转 ADOPT_RETAIL 退役。
3. **非 IR 轴连带（显式登记，非静默）**：
   - **18313（已退役行）的真端 IR 变化**：旧 XML 的完成段是"全 11 职业发同一物品 100001238"，
     真端表按职业点名 27 件武器；职业奖励落地后其真端 IR 改为按职业发放（**更忠实真端表**），
     随裁定指纹重算生效。这是 P0c-8c"采纳会连带改变非 IR 轴"判例的又一次显式登记。
   - **28313 的 3 条 EnterWorld 存档自愈边**：按 P0c-6 先例追加进 `p0c6-legacy-save-normalization.tsv`
     （可选一次性 DB 归一化，不要求零进度损失）。
   - **8c 普查复跑零搅动（verdict/basis 层面）**：判据机增量不影响既有裁定；3 行（11110/80601/80606）
     basis 由 `PREMATURE` 细化为更精确的 `LEGACY_SAVE_NORMALIZATION`（verdict 不变，边早已登记）；
     807xx 家族 8 行的 KEEP 证据补上"真端侧残余 = 已登记简报自环"的解释（unsafe 1→0）。
4. **族级分歧表（对账产出）**：
   | 码 | 行数 | 可否归零 | 路径 |
   |---|---:|---|---|
   | `KILL_COVERAGE_LOSS` | 35 | 否（本轮实测） | 35 行共 309 个缺失可达怪，`quest_script_monster.csv` 命中 **0**——真端源无补齐可能（逐行留证 `p0c9-keep-recheck.tsv`） |
   | `CLIENT_BUTTON_UNWIRED` | 11 | 否（本轮实测） | 客户端动作集逐行列出（select5 检查 20002 / 报告页 1009），真端路由集无对应动作、族表无 report/check 列 |
   | `RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH` | 84 | 可（未排期） | 需 CHALLENGE_TASK 挑战任务发放接线（M5-b2c 三类哨兵 / P0c-2/3 先例），需 ScriptDLL 证据 |
   | `RETAIL_COUNTER_EXCEEDS_6BIT` | 13 | 否（当前） | 真端计数 > 63 超 6 位 quest_vars 打包上限；扩位破坏客户端 SECTION 门控合同 |
   | `RETAIL_MONSTER_UNRESOLVED` | 11 | 可（需证据） | npc_templates 无精确名匹配；需补客户端/ScriptDLL 名→id 解析证据 |
   | `QUEST_SPAWN_UNEXPRESSED` | 2 | 否（当前） | 族表无刷怪列（P0c-6 裁定） |
   | `XML_EXTRA_REWARD` | 1 | 否（当前） | 旧 XML 多出客户端点名奖励物品 |
   | ~~`CLASS_SELECTABLE_REWARD`~~ | **0** | **已归零（本切片）** | 短名别名 + 职业路由发射 |
   | SimpleTalk `M3D_CLIENT_ROUTE` | 83 | P0c-10 面 | 降级复核与 585 稳定码收敛 |
5. **数据面（P0c-8c → P0c-9）**：缺口表 42 → **47**（+7 并码 −2 退役）；phase5-3 表 23 → **0**；
   生产裁定表 328 → **346**；裁定冻结 IR 326 → **344**（+18，含 18313 重冻结）；
   SimpleHunt RETAIL_TABLE 767 → **785**、XML_RETENTION 175 → **157**；
   catalog 2352 → **2334**；`verify_retirement.py` = `catalog=2334 directory=2334 retired=3890 sum=6224 — OK`；
   保留清单三副本 sha256 一致。
6. **族收口不变量（`p0c9_simplehunt_family_closure.py` 全绿）**：942 行 owner 分区封闭；
   XML_RETENTION 157 = 缺口表 47 + 拒绝表 108 + 驱动覆盖 2（互斥零差集，phase5-3 桶清零）；
   RETAIL_TABLE 行磁盘/catalog 双消、XML_RETENTION 行双在；裁定表 ADOPT 行与保留行交集 0。
7. **退役连带测试修复（按"真端/客户端为形状权威"重写，不是迁就旧 XML 断言）**：
   - `CounterChainTripletContractTest`（28313）：节点定位改按 (状态, 打包投影)（旧标签 started/k1..k3 不存在）；
     "乱序不计数"链式约束按真端网格语义重写为"各维度独立推进、只推自己那槽"（全 8 网格态逐态断言）；
     类奖励分支断言从"27 条路由"改为"27 个 (职业×物品) 分支 × 确认段 8..23 全段"（432 条）；
     EnterWorld 自愈边改为否定式断言（真端不表达，P0c-6 登记代替）；接取/报告/完成 NPC 归属改按状态定位。
   - `ClientQuestSectionAlignmentTest`：`load(int)` 改生产视图（11102 退役后直读 classpath XML 会 NPE）；
     11102 三槽 SECTION 断言在真端网格下逐字成立（布局同形：var0/var1/var2 @0/6/12 w6）。
   - `quest-start-metadata-retail-cap-exceptions.tsv`：移除 80602/80603/80605/80607/80608/80610
     （真端行 `maxlevel_permitted=UNLIMITED`，按 P0c-8c 的 80604/80609 先例"只登记仍存活封顶行"）。

## 3. 机器证据（可重跑）

| 证据 | 内容 |
|---|---|
| `python3 -B p0c9_simplehunt_family_closure.py` | 收口不变量全绿；两份 TSV 输出 |
| `python3 -B p0c9_retire_rows.py --dry-run` | `裁定 25 行 = ADOPT 18 + KEEP 7（UNRESOLVED 0）`（判据重跑确定） |
| `python3 -B p0c9_keep_recheck.py` | 47 行复核留证（309 缺失怪 / script_monster 命中 0） |
| `python3 -B p0c8c_gap_decisions.py --analyze --census p0c9-*.tsv` | 两批逐行裁定；`--write` 对 8c 全量普查复跑 verdict/basis 零搅动 |
| 冻结模式 | `-Dretail.hunt.adjudicatedFingerprintOut=src/test/resources/quest/retail-simple-hunt-adjudicated-ir-fingerprints.tsv` 重算 → 2 例 0F、344 行 |
| 探针（已删） | `P0c9GapShapeProbeTest`，源码留档 `p0c9_gap_shape_probe.java.txt`（LIMIT=1000 + classRoutesRetail 列 + REJECTED detail） |

## 4. 门禁

### 4.1 T1（主工作树，残留清理后）

首跑 50 例 / 1F / 14E 为**假红**：本批退役的 18 个 XML 残留在主树 `target/classes`，
`RetailQuestDriver.verifyProductionCoverage` 按 owner=RETAIL_TABLE 校验看到"XML 还在" ⇒
`wrongOwner=[2357, 2360, 11102, …] (18)`，生产装载链测试连锁报错。精确清除 18 个残留文件后复跑：
**50 例 / 4F / 0E，本切片自因失败 0**——4 个失败全部为并发 DataDriven 批在飞面
（`RetailDataDrivenGateTest` 3F + `QuestClientContractGateTest` 1F 的 BUTTON_WITHOUT_ROUTE，
逐条归因其 owner=DataDriven 行 13770/13841，本批 18 行零出现）。

### 4.2 T3 clean 副本（唯一验收证据）

命令：仓库外隔离副本 `/private/tmp/aion-p0c9`（rsync 排除 `/target`、`/.git`、`/aion`、`/patch`、
`/log`、`/.codegraph`），`mvn -o -B test -Dtest='com.aionemu.gameserver.questEngine.**,
!com.aionemu.gameserver.questEngine.retail.RetailDataDrivenGateTest' -DfailIfNoTests=false -DforkCount=2`。

| 运行 | 结果 | 日志 |
|---|---|---|
| 基线（P0c-8c 收口） | 1961 例 / 30F / 69E / 1 skipped | `gates/T3-p0c8c-clean2.log` |
| P0c-9 首跑（修复前） | 1965 例 / 43F / 71E / 1 skipped | `gates/T3-p0c9-clean.log`（暴露 28313 退役连带测试 + 封顶清单 6 行 + 11102 直读；并发批同期新增其直读面） |
| P0c-9 收口复跑 | **1964 例 / 36F / 71E / 1 skipped** | `gates/T3-p0c9-clean2.log` |

失败方法集 `comm` 逐行对账（对基线）：**only-now 21 个，全部为并发 DataDriven 批自因面**
——Quest18950/21320/28950/2929/30515ClientDialogAlignmentTest、Quest80787To80794RetailAlignmentTest、
QuestNoHandlerShard3DefinitionTest（29634/30565）、`ZZProbeDDHuntContractTest`（该批留树探针）直读
其退役 XML（owner=DataDriven 实测），`QuestClientContractGateTest`（DD 行 13770/13841）、
`QuestE2eInfrastructureTest` pvp 行。**本切片自因新增失败 = 0**。

本切片修复面在收口复跑中全部绿：
`CounterChainTripletContractTest` 8/0F（28313 真端网格重写）、`ClientQuestSectionAlignmentTest` 7/0F
（生产视图）、`QuestRetailStartMetadataGateTest` 6/0F（封顶清单 6 行移除）、
`RetailSimpleHuntFamilyGateTest` 1/0F（347.6 s，942 行族不变量 + 真端表缺口类别）、
`RetailSimpleHuntEquivalenceGateTest` 2/0F（344 行裁定冻结指纹复验）、
`RetailMetadataEquivalenceGateTest` 1/0F（**M1 元数据门禁在短名别名落地后直接绿**——
登记分歧表 `classRewards RETAIL_PRIORITY` 行覆盖该轴，无需重算）。

## 5. 未验证 / 阻塞 / 下一步

- **未验证**：数据库持久化路径与真实客户端抽检不在本轮范围。M1 元数据门禁在短名别名落地后
  于 T3 clean 中直接绿（登记分歧表 `classRewards RETAIL_PRIORITY` 行覆盖该轴），未重算分歧表；
  若后续族以 classRewards 为编译源，建议届时重算一次登记表核对逐行漂移。
- **并发观察（不触碰）**：DataDriven 批（P5）在飞——`RetailSimpleHuntDefinitionCompiler` 被其加入
  `incompleteReportRoutes`（DD 专属开关，家族默认 false）期间出现过两次编译中间态（23:55、00:2x），
  均自行恢复；其退役行（2929/18950/21320/28950/30515/80787 族/29634/30565/13770/13841 等）的直读
  测试失败已在 §4.2 点名归因，属该批自因面。catalog 绝对值随其推进继续漂移（本切片期间
  2334 → 2314），以实时对账为准。
- **下一步**（v3 队列）：P0c-10 SimpleTalk 收口（585 稳定码 + 83 行 M3-d 降级复核，复用本切片分歧表格式）；
  随后 P0c-11 非 IR 轴系统化、P1 SimpleItemPlay（15 行，真端表 43 行已入仓，`FAMILY_PENDING` 已登记）。
