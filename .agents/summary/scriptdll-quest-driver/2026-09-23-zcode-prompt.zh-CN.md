# ZCODE 交接提示词：AionEmu 任务系统改为「真端文件驱动」（终局交付）

> 可整段粘贴给 zcode 作为首条消息。配套细节（数据地图、字段样例、DLL 函数、坑位）见同目录
> `2026-09-23-zcode-appendix.zh-CN.md`；其中 §10 是一个**可选**的工作切分参考，不是验收要求。
> 仓库：`/Users/mc/IdeaProjects/AionEmu-test`（Aion 5.8 社区服务端，Java 25 + Maven + Spring Boot，单模块）。

---

## 1. 终局：交付后系统必须处于的状态

服务端加载任务定义时：

1. **5554 个任务的定义完全来自真端文件**（模板表 + 真端 `quest.xml` 元数据 + 反编译 `ScriptDLL64` 恢复的推进语义），**不读任何 `quest-definition` XML**。
2. **只有 670 个任务保留 `quest-definition` XML**：494 个是真端用 per-quest 脚本注册的（无模板行）、176 个真端无任何覆盖。这份保留清单**可枚举、逐条有证据**。
3. **生产路径默认走真端驱动**；XML 只作为保留清单内的降级来源，且降级原因可查。
4. **客户端与存档表现与改造前逐帧一致**：`QUEST_Q*.html` 的 `quest_summary` 行/`SECTION_n` 计数、`quest_vars` 打包值、协议包、任务状态机（START/REWARD/COMPLETE 迁移）都不变。

数字口径（实测）：本仓库生产任务定义 **6224** = 真端表覆盖 **5554（89.2%）** + 无模板行 **670**（其中注册点脚本 494、真端无覆盖 176）。重跑脚本：`python3 -B .agents/summary/scriptdll-quest-driver/retail_family_coverage.py`。

---

## 2. 终局不变量（任何实现都必须满足）

1. **单一运行时**：不得新增第二套任务运行时、第二套存储或并行调度；定义来源可变，`CompiledQuestDefinition → QuestEventIndex → QuestProductionDispatcher → QuestState/QuestVars` 这条链必须复用。
2. **单一 owner**：任一任务的定义来源唯一（真端表 **或** XML），且有机器可读的归属表；不允许运行时两条来源互相覆盖或反复漂移。
3. **可回退**：提供一个开关，关闭后系统行为与改造前完全一致（同一套加载路径、同一份 XML）；开启才走真端驱动。
4. **逐帧等价**：每个被迁移的任务都必须能证明"真端驱动的定义 == 原 XML 定义"（定义 IR 等价 + 调度层逐帧等价），不能证明的一律进保留清单，**不许改 XML 去凑真端**。
5. **口径冲突真端优先**（用户已定）：XML 与真端不一致但真端语义可证 → 按真端；不可证 → 保留 XML 并登记。

---

## 3. 起点（已完成，必须复用，不要重写）

`HEAD = 943419462`；真端改造整体**未提交**（47 M + 8 ??，清单见附录 §1）。已有资产：

| 资产 | 作用 |
|---|---|
| `resources/aion/data/static_data/quest_retail/Quest_SimpleHunt.xml` | 真端 SimpleHunt 表入仓（UTF-16→UTF-8，1859 行可驱动） |
| `questEngine/retail/RetailSimpleHuntTable` | 真端 XML 表加载范式（允许内部 DTD、禁外部访问） |
| `questEngine/retail/RetailHuntCounterLayout` | DLL `FUN_180cb13b0` 的 6 位槽位语义：`canIncrement`/`increment`/`goal=Σ countN<<6(N-1)` |
| `questEngine/retail/RetailNpcNameIndex` | 真端 spawn 名 → npc_id（87,719 名） |
| `questEngine/retail/RetailSimpleHuntPlan` | 表行 → 执行计划（`slotForNpc/canCount/count/isComplete`） |
| `questEngine/retail/RetailQuestCatalog` | 三态归属判定 `RETAIL_TABLE / XML_FALLBACK / UNKNOWN` |
| `questEngine/retail/RetailSimpleHuntIrCompiler` | 表行 → 定义 IR；`tableFirst(catalog, questId, shell)`；11 个降级码 |
| 4 个 retail 测试类 + `QuestSimpleHuntRetailContractTest` + `quest-simple-hunt-retail-contract.tsv` | 表/计划/归属/IR 门禁 + 1366 行真端合同快照 |
| `.agents/summary/scriptdll-quest-driver/` | 反编译语义（phase2）、对账与覆盖报告、`phase5-3-rejections.txt`（25 条必须保留 XML） |

已证：SimpleHunt 的 COUNTER_GRID **520/527**、KILL_CHAIN **150/168** 可由真端表整表复现（下限已写死在测试里）。

---

## 4. 你必须交付的东西（每项都给出"完成判据"）

### A. 真端定义来源（覆盖 5554）

| 族 | 仓库任务数 | 真端表 | 已恢复的 DLL 语义 |
|---|---|---|---|
| SimpleHunt | 942 | `Quest_SimpleHunt.xml` | `FUN_180cb13b0`（6 位计数）、`FUN_180caf7c0`（报告）、`FUN_180cafa40`（完成/奖励） |
| SimpleTalk | 2223 | `Quest_SimpleTalk.xml` | `FUN_180cab520`（单步）、`FUN_180cabb10`（状态链 0→1→2）、`FUN_180caca90`（报告） |
| CombineTask | 574 | `Quest_CombineTask.xml` | `FUN_180caac10`（结束进度→刷新→写分量→发产物）、`FUN_180caaf00` |
| SimpleCollectItem | 178 | `Quest_SimpleCollectItem.xml` | 采集/对象交互步 |
| SimpleUseItem | 104 | `Quest_SimpleUseItem.xml` | 使用道具步 |
| SimpleItemPlay | 15 | `Quest_SimpleItemPlay.xml` | 对话 + 道具播放步 |
| SimpleSerialHunt | 10 | `Quest_SimpleSerialHunt.xml` | `count_first..fifth`/`monster_first..fifth` 串行链 |
| DataDriven | 1508 | `data_driven_quest.xml` | `category_acquire_`/`category_progress_` + `value0_*` 步骤 |

**完成判据**：这 5554 个任务中，每个任务都满足"真端定义 IR ==（或按真端语义修正后 ==）原 XML 定义 IR"，且**不需要读 XML**；不满足的从本项移入 B。

### B. 保留 XML 的 670 个任务清单

**完成判据**：产出机器可读清单（建议 `retail-xml-retention.tsv`），逐行给出 `quest_id / 原因(SCRIPTED|NO_TABLE|口径分歧|语义未闭环) / 证据（helper 函数名或"真端无覆盖"）`；清单与 `quest_definition_catalog.xml` 的实际内容一致（有门禁断言）。

### C. 生产接线（默认真端，开关回退）

- 挂在现有装载链上：`questEngine/QuestEngine.java:1853 → QuestDefinitionCatalogManifest.compile(...)` / `QuestDefinitionDirectoryLoader:47`。
- overlay 语义：命中真端来源且可交付 → 用真端定义；否则用 XML（保留清单内）。
- 开关：默认开启真端（或先用开关默认关闭并说明切换计划 —— 二者选一，但必须写清楚），关闭时行为与改造前一致。

**完成判据**：`ProductionCatalogWhitelistVerificationTest`、`QuestDefinitionCatalogManifestTest`、`QuestClientContractGateTest`、`QuestRetailStartMetadataGateTest` 全绿；开/关两种状态下都能加载 6224 个任务且归属与 A/B 清单一致。

### D. 目录与文件收敛（6224 → 670）

**完成判据**：被迁移任务的 `quest_definition/quests/<id>.xml` 已删除、`quest_definition_catalog.xml` 中对应 `<definition>` 条目已移除；全仓 `grep` 确认没有测试/脚本/资源再引用被删文件；`question_definition` 的 XSD/装载器不再要求这些条目。

### E. 门禁（必须长期有效，不是一次性脚本）

1. **归属门禁**：每个任务 ID 的 owner 唯一，且与 A/B 清单一致。
2. **家族等价门禁**：对每个族，真端定义 vs XML 定义（IR 层：节点投影集合、事件→目标、条件/动作/after-commit 一致；节点标签按投影值归一化）。
3. **调度层逐帧门禁**：抽样/全量跑"击杀/对话/采集序列 → `nextStatus` + `nextPackedVariables`"，两侧一致。
4. **客户端契约门禁**：`QUEST_Q*.html` 的 `quest_summary` 行与 `SECTION_n` 计数与真端定义产出的值域一致（含报告行 = SECTION_5）。
5. **降级清单门禁**：保留清单内必须走 XML；清单外必须走真端；未知降级码一律失败。

### F. 文档与覆盖率

**完成判据**：`.agents/summary/scriptdll-quest-driver/` 下有终局报告：覆盖率（表驱动/XML 各多少）、逐族完成情况、保留清单、已知口径分歧、未验证项。

---

## 5. 判定「某任务可以删 XML」的充分条件（对所有族一致）

同时满足才允许删除：

1. **元数据对拍**：真端 `quest.xml` 字段映射出的 `QuestMetadata` == 原 XML 的 metadata（真端优先的分歧要单独列证）。
2. **进度/事件对拍**：真端表语义推出的步骤与真端 DLL 反编译行为一致，且与客户端 `quest_monster.csv` / `quest_script_monster.csv` / `QUEST_Q*.html` 的守卫一致。
3. **IR 等价**：真端编译结果与原 XML 编译结果在 IR 层等价（第 4.E.2 条）。
4. **逐帧等价**：调度层序列对拍一致（第 4.E.3 条）。
5. **无阻塞分歧**：该任务不在保留清单，且没有未闭环的口径冲突。

任一条不满足 → 进保留清单，写明原因与证据，**不要改 XML 去凑**。

---

## 6. 执行纪律与硬约束（违反即失败）

### 6.0 执行纪律：**连续执行，中间不要停；绝不主动提交**

> **本任务以 goal 模式运行**：一个持久目标 + 不停机循环，直到完成检查清单（§7）全绿。

**① 用下面这段作为唯一 goal 的 objective**（若运行环境支持 goal/长期目标，用 `create_goal` 创建**一个**；不支持则把同一段写进台账文件的顶端，每轮续跑）：

```
把 AionEmu 任务系统改为真端文件驱动：5554 个任务（SimpleHunt 942 / SimpleTalk 2223 / CombineTask 574 /
CollectItem 178 / UseItem 104 / ItemPlay 15 / SerialHunt 10 / DataDriven 1508）的定义完全来自真端模板表
+ 真端 quest.xml 元数据 + ScriptDLL64 反编译语义，不再读 quest-definition XML；仅保留 670 个任务
（494 个真端 per-quest 脚本 + 176 个真端无覆盖）继续走 XML；生产装载链支持真端优先、XML 降级且开关可回退；
删除已迁移任务的 XML 并从 quest_definition_catalog.xml 移除对应条目（6224 → 670）；建立并跑通五类长期门禁
（归属 / 家族等价 / 调度逐帧 / 客户端契约 / 降级清单）；产出终局覆盖率报告与保留清单。
全程只改工作区：不做任何 git 提交、推送、打 tag 或新建分支。
```

**② 每轮按这个循环推进，不要重新做发现阶段**：

1. 打开台账 `.agents/summary/scriptdll-quest-driver/GOAL-retail-driver-progress.zh-CN.md`，读「当前切片 / 下一步 / 阻塞」；
2. 取**下一个可推进的最小切片**（一个族的一个子集、一个门禁、一次目录收敛），直接实现；
3. 跑对应门禁（§6.1 的两组 Maven 命令之一），把命令与结果写进台账的「证据索引」；
4. 更新台账（家族进度表、DoD 勾选、变更日志、下一步），**然后立刻开始第 5 步之后的下一个切片**；
5. 只有命中下面「允许停」的三种情况才结束本轮；否则继续循环。

**③ goal 状态纪律**：

- 只创建**一个** goal；不要为子任务新建 goal，也不要把 goal 状态改成 `complete`，直到 §7 清单**全部**勾上。
- 不要因为"轮次/上下文/预算用尽"而结束：先把进度、TODO、复现命令落盘到台账，再继续。
- 不要用「要不要继续？」「下一步建议…」结束回复；汇报只用于同步，写完继续干。


- **不要主动提交**：`git commit` / `git push` / `git tag` / `git stash` / 新建分支或 worktree 一律**不要做**，除非用户在当轮明确说「提交」。**到终局也不要提交**——交付物留在工作区，用户自行决定提交范围。
- **中间不要停**：单个任务内持续推进，直到 §7 完成检查清单全绿，或命中下面列出的「真阻塞」。禁止用「要不要我继续？」「是否需要我…」「下一步建议…」这类**提问或建议**结束回复；也禁止做完一个切片就停下等确认。
- **汇报不是停止点**：可以在同一轮里简短写「已完成 X / 证据 Y / 正在做 Z」，然后**继续执行 Z**。
- **把需要拍板的事变成不阻塞**：口径分歧（如 XML 比真端多怪、`displayNameId` 来源未定）先写进降级清单 + 保留 XML，然后继续推进其它任务/族，不要停下来等用户回答。
- **允许停的只有三种情况**：① 用户明确要求停或改方向；② 需要用户才能做的外部动作（重启服务端、实机客户端验收、提交/推送）——记录为「待用户执行 / 未验证」后**继续其它工作**；③ 确实已无可推进项（此时直接给出终局报告与未验证清单）。
- **上下文/预算将尽时**：先把进度、TODO、复现命令落盘到 `.agents/summary/scriptdll-quest-driver/`，然后继续；不要以「篇幅/轮次不够」为由停在半途。
- 需要用户授权的**越界命令**（例如全量 `mvn test`、修改他人并发文件）仍要先问，但问的同时要继续做**不受影响**的部分，不要整轮挂起。

### 6.1 硬约束


1. **不要 `git commit` / `git push`**（细则见 §6.0）：本改造只留在工作区，提交需用户当轮明确要求。
2. **不要 `git checkout` / `git reset --hard` / `git clean`**：工作区有大量未提交的真端改造文件，硬操作会毁掉它们；撤回过的提交指针在 tag `wip/retail-quest-phase5`，不要动。
3. **不要启动/停止/重启服务端进程**。
4. **Maven 已授权（阶段收口必跑，无需再问）**：范围见下方「授权命令」，覆盖焦点为 `com.aionemu.gameserver.questEngine.*` 的 `-Dtest` 选择器（含你为各族新增的门禁测试）。
   超出范围的动作（全量 `mvn test`、`mvn package/install`、改 `pom.xml`、非 quest 模块测试、预计 >10 分钟的构建）仍需先说明「命令 + 范围」并请求授权。
5. 中间脚本/报告只放 `.agents/summary/scriptdll-quest-driver/`（禁止 `scripts/`，禁止 `.agent/`）。
6. **不要动他人并发改动**：`quests/10528.xml`、`quests/20528.xml`、`.agents/memory-bank/*`、`.agents/summary/index.jsonl`、`.agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py`、`src/test/java/.../definition/ArchdaevaRewardRowContractTest.java`、`.agents/summary/idea-unused-false-positive/`、`.agents/summary/quest-20528-reward-row/`。
7. 规范：Tab 缩进、行宽 ≤120、注释中英双语、显式 import、类/记录双语 javadoc、日志走 `I18n.get(...)`、全中文回答。
8. **家族级纪律**：禁止只改单个任务；共享缺陷必须整族审计 + 批量回归门禁。
9. 证据分层诚实：静态检查 / 聚焦测试 / 服务端重启 / 实机客户端验收分别陈述，未做的写「未验证」。

**授权命令（阶段收口必跑，不必再征求同意）**

```bash
# ① 基线门禁：每个阶段开始与结束各跑一次
mvn -q -Dtest='com.aionemu.gameserver.questEngine.retail.*Test,QuestSimpleHuntRetailContractTest' test

# ② 本阶段新增/改动的族门禁（选择器限定在 com.aionemu.gameserver.questEngine.*）
#    例：mvn -q -Dtest='com.aionemu.gameserver.questEngine.retail.SimpleTalk*Test' test
mvn -q -Dtest='<本切片测试选择器>' test

# ③ 生产装载 / 客户端契约门禁：涉及生产接线或删 XML 时必跑
mvn -q -Dtest='ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest,QuestClientContractGateTest,QuestRetailStartMetadataGateTest' test
```

**阶段收口流程（Definition of Stage Done，缺一不可）**

1. 跑 ①（阶段前后各一次）→ 跑 ②（本切片门禁）→ 涉及接线/删 XML 再跑 ③；
2. 失败的先修，**不要停**；确认与本阶段无关的既有失败（如 `ai.RetailPatternAI2Test` 的 NPE）按"已知既有失败"记录后继续；
3. 把「命令 + 通过数」写进阶段报告的证据段与台账「证据索引」；
4. 报告落盘 `reports/<date>-<slice>-<topic>.zh-CN.md` + `reports/INDEX.zh-CN.md` 追加 + 台账更新；
5. 立刻进入下一个切片，不要等确认。

已知**与本改造无关**的既有失败：`com.aionemu.gameserver.ai.RetailPatternAI2Test`（`RetailPatternAI2:893` NPE）。不要去"修"它。

---

## 7. 完成检查清单（逐项勾选后才算交付）

- [ ] 5554 个任务各有机器可读 owner 记录，且全部由真端文件驱动（不读 XML）
- [ ] 670 个任务在保留清单中，逐条有原因与证据；无"未归类"任务（5554+670=6224）
- [ ] 生产接线可用，开关关闭时行为与改造前一致，开启时按 owner 表加载
- [ ] 被迁移的 XML 已从 `quest_definition/quests/` 删除，`quest_definition_catalog.xml` 已同步收敛
- [ ] 五类门禁（归属/家族等价/调度逐帧/客户端契约/降级清单）全部实现且绿
- [ ] 客户端抽检：至少每族抽 3 个任务，`quest_summary` 行与 `SECTION_n` 计数与真端定义一致
- [ ] 终局报告（覆盖率、保留清单、口径分歧、未验证项）落盘
- [ ] 全仓无对被删 XML 的悬空引用（`grep` 证据）

---

## 8. 已知事实与坑（细节见附录 §4/§5/§6/§8）

1. counter-grid 必须是**完整笛卡尔积**，START 节点投影字段集合完全等于网格字段。
2. **定义首个节点必须是接取态（NONE）**，否则 `UNREACHABLE_NODE`。
3. 同事件并行边必须给唯一 `priority`，否则 `AMBIGUOUS_TRANSITION`。
4. `Set.copyOf` 顺序不稳定 → 生成边/节点前必须排序。
5. 自环计数不得自增被 source 投影钉死的字段（`COUNTER_SELF_LOOP_PINS_INCREMENTED_FIELD`）。
6. **击杀步的语义顺序由 `param_3` 的 SECTION 序号决定，不是注册点顺序**（1517 反例）。
7. **服务端状态数 ≠ 客户端行数**（1001 客户端 5 行 / 服务端 5 script 步；2641 客户端 4 行）；报告行固定 SECTION_5，「进度不是独立行而是行内 `(已杀/上限)`」。
8. 真端文件 UTF-16 + 内部 DTD；`ScriptDLL64.c` latin-1；客户端 UTF-8；入仓的 `Quest_SimpleHunt.xml` 已是 UTF-8，不要覆盖回去。
9. `displayNameId` 目前无运行时消费方（只有测试断言数值），真端只给 `STR_QUEST_NAME_Qxxxx` 符号名；先查清客户端任务名的驱动来源再动它，不要伪造。
10. 已知口径分歧举例：12 个任务的 XML 比真端表多怪（客户端 CSV 口径）、3 个 XML 边数少于真端、2 个 START 投影含阶段位（清单见 `phase5-3-rejections.txt`）——这些先保留 XML 并单独列证。

---

## 9. 汇报 = **落盘到文档**（每完成一个稳定切片都要写，不是只在对话里说）

**规矩：文档才是记录，对话里只给摘要 + 文档路径。**

1. **新建阶段报告**：`reports/<YYYY-MM-DD>-<切片或里程碑>-<主题>.zh-CN.md`
   （目录 `.agents/summary/scriptdll-quest-driver/reports/`，示例 `2026-09-23-M1-retail-metadata.zh-CN.md`），
   内容必须包含：

   ```
   # <切片名> 报告
   - 日期 / 切片 / 覆盖范围（族 + 任务数）
   ## 1. 交付（新增/修改文件 → 一句话作用，标出可复用资产）
   ## 2. 证据（命令 + 结果：测试通过数、家族扫描 N/M、降级 K 条及原因）
   ## 3. 结论（本轮可删 XML 的任务数与判据来源；不可删的逐条原因）
   ## 4. 对拍结果（元数据 / 进度事件 / IR / 调度逐帧 / 客户端 SECTION 各一项结论）
   ## 5. 未验证（+原因：服务端重启、实机验收、未授权命令等）
   ## 6. 阻塞与决策项（口径分歧、需用户拍板的点；附"不停等的处理方式"）
   ## 7. 下一步（下一个最小切片 + 复现命令）
   ```

2. **更新报告索引**：在 `reports/INDEX.zh-CN.md` 顶部追加一行（日期 / 切片 / 文档 / 一句话结论）。
3. **更新 goal 台账** `GOAL-retail-driver-progress.zh-CN.md`：DoD 勾选、家族进度表、当前切片/下一步、阻塞表、
   证据索引（命令→结果）、变更日志（每轮一行，带报告链接）。
4. **对话回复**只给：本切片结论 + 证据数字 + 报告路径 + 已继续推进的下一个切片；**不要**把完整报告贴对话里代替落盘。
5. 报告与台账属于本任务产物：**不要提交**，但必须留盘，确保换会话/换 agent 能无缝续跑。

工作顺序由你决定；附录 §10 的切分只是降低风险的参考。

汇报只用于同步进度，**不是停止点**：写完汇报继续推进下一步，直到 §7 清单全绿或命中 §6.0 的真阻塞。
