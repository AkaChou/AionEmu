# P1 SimpleHunt 切换批 · 声明书（设计 + 文件清册 + 测试清册 + 前置门）

日期：2026-10-01　状态：**READY — 准备执行**（真端阻塞已扫清，G2 已闭环，待 G1 协调与执行授权）
输入：P0a/P0b 审计与数据基础、P1 第一/第二横切（codec/相机/注册表/解析器/装载器/owner 解析器，45 例绿）、
真端集成面考古（§7 引用）、SerialHunt/DD 考古（`../p2-prereqs/`、`../p7-prereqs/`）。
tablelane 套件现况：**41/41 绿**（本批新增 NativeQuestOwnerResolver 6 例）。

---

## 1. 批次范围（一次原子切换）

**切换**：retention `family=SimpleHunt` 且 `owner=RETAIL_TABLE` 的网格形任务（快照 939 行）从
「retail→IR 编译器」切到「真端表行 → native handler 直驱」。

**不在本批**（理由随条）：

| 项 | 去向 | 理由 |
|---|---|---|
| SimpleSerialHunt 串行链 10 行 | P2 | 计划批次表即如此；串行=独立 retention family、独立推进语义（bits26-31 步指针，`p2-prereqs` 已还原） |
| ~~172 名字冻结行~~ | **已纳入本批全量切换** | 经排查真端原码与表，22 假歧义（本服 `name_desc` 对应真端唯一英文名）与 150 假缺名（真端 `<quest_ai_name>` 与 World ID 区域杀怪）已彻底查实。本批 939 行 100% 全量切换，不留任何冻结行 |
| `RetailSimpleHuntTable/Plan`、`RetailHuntCounterLayout`、狩猎 TSV | P2/P7 | 考古实证：`RetailSimpleHuntPlan.bind` 被 DD 编译器消费（`RetailDataDrivenDefinitionCompiler.java:440`）、`RetailHuntCounterLayout` 被 DD 链共享、`hunt_stages.tsv` 消费者=串行编译器（P2）、`kill_targets*.tsv` 消费者=DD（P7）。**对计划 §7 P1 行「同批删除狩猎客户端 TSV runtime 读取」的修正：本批无可删 TSV（其运行时消费者均属 P2/P7 家族），按消费者家族退出** |
| 奖励窗/`can_report` 全量建模 | 随本批做 SimpleHunt 子集，全量在 P3+ | 计划 §4.3 出口按族渐进 |

### 1.1 范围增补（2026-10-01 深夜形状审计发现；`../p3-prereqs/family-table-shapes.md` §2.2）

939 行内存在四组特殊行，handler 设计（§2）与族级对拍（§4）必须覆盖，不构成停批项（全部表声明、零硬编码）：

| 组 | 行数 | 行 | 处置 |
|---|---:|---|---|
| talk 链中继 | 21+1 | talk_npc1×21；14152 另有 talk_npc2（双中继） | handler 支持中继 NPC 对话推进（页流按 G3 约定）；对拍以旧 IR 行为为过渡 oracle，真端口径按该行 codegen 条目抽查 |
| con_quest 前置门 | 85 | 首行 1102… | handler 接取侧检查 con_quest（真端 record +0x2b4 已知偏移） |
| cutscene | 3 | 3016/4007/4014 | 完成侧 PlayCutSceneByQuestShare（IUserImp +0x1b0，P7 定名） |
| give_item | 1 | 24115 | 接取侧发物（AddItem 路径） |

另登记（不在 939 内）：**80281/80283 缺 `reward_npc_name` 且 retention 无 owner**——P1 不路由；
P8 go-live 按 resolver 表集优先将成 native-owned ⇒ 届时缺交付 NPC fail-closed，处置见形状审计 §2.2。
跨表 id 相交门预检 PASS（7 表两两零相交，形状审计 §1）。

## 2. 运行时设计（对齐考古事实）

### 2.1 状态写入：不新建第二条持久化路径
考古确认 typed 车道已有完整机制：`QuestExecutionCoordinator`（按玩家串行 + 每事件一事务，
`QuestUnitOfWork.open/commit/rollback`）→ `PlayerQuestStatePort.apply/publish/rollback`（暂存投影→
提交后原子写 live，`QuestState.setQuestVar(int)` 即原始整字口）→ `PlayerQuestStateSyncPort.sync`
（`SM_QUEST_ACTION.add/update`，`step`=packed vars；冗余同步抑制已有）。
**native 复用全部机制**：handler 只产出决策（newStatus, newRawVars, afterCommit 动作），经
`QuestState.setQuestVar(int)` 走同一 state port；禁止绕过（不变式：state port 单一实现）。
10 位值只走整字路径（`QuestVars` 槽位 API 会钳 63，考古已证）。

### 2.2 事件进入：native 子运行时
现链：`NpcController.doReward`/`PlayerTeamDistributionService.doReward` → `QuestEngine.onKill` →
`QuestEvent.KillNpc(npcId)` BROADCAST → `QuestRuntimeRouter.dispatch`（owner 唯一）→ 子 dispatcher。
**native 以第三种子运行时接入 router**（与 xml/retail-IR 子 dispatcher 同接口）：
- 安装期把 native 击杀兴趣（npcId → questIds）注册进 `QuestEngine` 的 questNpcs 索引（npc id 由
  `NativeNpcNameResolver` 从表行 monster 名单唯一解析；歧义/缺失 = 该行不注册并记 `NATIVE_NAME_UNRESOLVED`）；
- dispatch：KillNpc → 命中任务 → 快照玩家 state+vars → `ProgressCamera.advance`（P0a 三守卫）→
  变化则走 §2.1 事务路径；无变化静默（对齐真端「超杀无动作」）。
- 接取/交付对话：`onDialog` EXCLUSIVE 按 questId 到 native 子运行时（router 已按 owner 分派，
  无需改 `QuestEngine.onDialog` 结构）。

### 2.3 接取/报告/奖励页流：~~本批最大设计轴~~ **G3 已完成（`pageflow-spike.md`）**
SPIKE 结论：全部页 id（4/1003/1004/10/5/奖励窗 5-8/45/46）在 HtmlPages 核对通过、无缺口；核心动作
词汇（31/1002/1003/1004/20000/20001/1008/1009）客户端 HTML 与真端派发器双侧一致；计数位布局与简报
位偏移 30 有 SRV58 逐字出处。**8 项家族级约定**（页 4 直跳、回页 10、确认区间 8..23 + 108/110+k 自动
确认 + 双协议 + 槽上限 15、档位→窗映射、class 优先级公式、select_none 14 任务集〔网格形不消费〕、
简报位 slot6 一步清位、满段杀可见性刷新）无真端逐字出处——**全族共形，按"家族级冻结约定"登记
（出处=本 SPIKE 文件），不产生行级冻结，不阻切换**。网格形不消费 `clientDialogExits`（结构性事实，
native 无需该输入）；进度中再对话=运行时兜底（同构缺席处理）。顺带发现文档漂移：javadoc 宣称的
`RETAIL_MULTI_TIER` 拒绝码实际不存在（落 `COMPILATION_FAILED`），随本批矩阵消除。

### 2.5 raw vars 存档结论（不变式 7）：**已闭环（CLEAN）**
`simplehunt-rawvars-archive.md`：SimpleHunt 全部 213 行玩家存档（94 组值）100% 落在表推导相机可达
集合内，零高位/零意外/零休眠行非零 ⇒ **无需迁移**；native 运行时加 `NATIVE_RAW_VARS_INVALID`
保险丝（实测违例 0）。

### 2.4 行宇宙与 owner 裁决
- 路由 owner = `NativeQuestOwnerResolver`（已落地，45 例测试内）；切换 go-live 调
  `requireDisjoint()`：现交叠恰 3 个（14112/16961 表行 READY 但 XML 定义残留 → **本批同批删除
  这两个 XML 定义**；14123 表行名字多义 → 前置门 B 冻结行，XML 侧不动）。
- 表 1863 行 vs retention family=SimpleHunt 942 行（939 RETAIL + 3 XML）的差异 = 跨族表行
  （同 id 出现在他族 retention 名下）：**owner 以 retention 家族裁决为准**；CameraRegistry 全量
  1863 行照常注册（注册 ≠ 路由）。本批开工时逐行重算该裁决并落矩阵（QE-112 落地后跑）。

## 3. 新增/修改文件清册（main）

| 动作 | 文件 | 内容 |
|---|---|---|
| 新增 | `tablelane/NativeQuestXmlTable.java` | **已落地（第三横切，2026-10-01）**：quest.xml 元数据行装载（数据层零语义合成，10035 行，多值直接子字段模型）；handler 的起始条件/奖励组/职业奖励语义提取在切换批内基于它实现 |
| 新增 | `tablelane/NativeQuestRuntime.java` | native 子运行时：owner 集、npc→quest 索引、KillNpc/Dialog 分派入口（实现 router 子运行时接口） |
| 新增 | `tablelane/SimpleHuntHandler.java` | 网格形 handler：表行+相机+页流规则 → 状态决策；零 quest 常量 |
| 新增 | `tablelane/NativeDialogFlow.java`（若 SPIKE 证明需要独立类；否则并入 handler） | 接取/报告/奖励页流 |
| 修改 | `QuestRuntimeRouter.java` | 接入 native 子运行时（组合处 +1 分支） |
| 修改 | `QuestEngine.java`（installProductionDefinitions 附近） | native 运行时安装 + questNpcs 兴趣注册 + `requireDisjoint()` go-live 门 |
| 删除 | `RetailQuestDriver.compileSimpleHunt` 分支及 compileByFamily 的 SimpleHunt 网格分派 | 该族网格形不再产 IR |
| 删除 | `RetailSimpleHuntDefinitionCompiler` 中网格形专有方法（`compile` 网格入口、网格 compileCanonical 等） | 同批删旧；**串行 `compileSerialChain` 与被 9 族复用的规范形 helper 先做纯移动抽取到独立类（零行为变更），再删网格专有部分** |

## 4. 测试清册

**退役/迁移（同批）**：`RetailSimpleHuntFamilyGateTest`（942 行不变量改由 native 家族门接管）、
`RetailSimpleHuntEquivalenceGateTest`、`RetailSimpleHuntPlanEquivalenceTest`、
`QuestSimpleHuntRetailContractTest`（网格部分；串行断言保留至 P2）。

**新增**：
1. `SimpleHuntNativeFamilyGateTest`：939 行逐行「native 决策回放 vs 旧 IR 行为」族级对拍，
   `ADDED 0 / REMOVED 0`（事件序列 → status/rawVars/页/奖励）；
2. 相机/名字负例：歧义名不注册、未知页 fail-closed、相机三守卫、超杀静默；
3. go-live 门：交叠非空 `NATIVE_OWNER_CONFLICT`（已有 resolver 测试）；
4. XML-only 基线回归：本批零触碰 XML 车道内部，跑既有 XML 代表门。

**聚焦命令（开工时声明执行）**：
`mvn -Dtest='SimpleHuntNativeFamilyGateTest,NativeQuestTableLoaderTest,CameraRegistryTest,ProgressCameraTest,NativeQuestOwnerResolverTest,QuestVarsTest' test`
+ 收口全量 `mvn -Dtest='Retail*Test,Quest*Test' test` 按当时红集基线归因。

## 5. 客户端验收
代表任务 ≥1（建议 2354：双槽网格 + P0a 锚）。未跑 = 本族 `PENDING_CLIENT`，不得记完成。

## 6. 硬编码门
SimpleHuntHandler/NativeDialogFlow 零 questId 字面量/零 name→id 常量；全部参数来自表行 +
NativeNpcNameResolver + HtmlPagesRegistry。跑 `p0a/tools/hardcode_scan.py` 召回门。

## 7. 事实依据（考古要点，全文另存 `../p1/integration-surface.md`）
击杀链：`NpcController.java:312` / `PlayerTeamDistributionService.java:199` → `QuestEngine.onKill:410-430`
（KillNpc BROADCAST，questNpcs 索引 `:2060-2065`）→ `QuestRuntimeRouter.dispatch:95-153` →
`QuestExecutionCoordinator:92-119`（按玩家串行+事务）→ `PlayerQuestStatePort:50-114`（apply/publish/
rollback，原始整字口 `QuestState.setQuestVar(int):92-95`）→ `PlayerQuestStateSyncPort:60-105`
（SM_QUEST_ACTION；`step`=packed vars）。零售 SimpleHunt 最终形态=IR transitions（编译器 `:156-158`）。
helper 复用面：9 个他族编译器引用 `RetailSimpleHuntDefinitionCompiler` 的 canonical 流 helper 与
`RetailSimpleHuntPlan.bind`（DD `:440`）。

## 8. 前置门（未开不开工）

| 门 | 内容 | 状态 |
|---|---|---|
| G1 | **QE-112 落地或用户裁决**：`RetailSimpleHuntDefinitionCompiler.java` 正是 QE-112 脏文件之一（git status M）；在飞切换批与其同文件原子删改 = 并发车道冲突。D.4 开工检查表硬条件 | ❌ 未开 |
| G2 | **名字与怪物名真端对齐（闭环，消解 172 冻结行）**：依据用户“阻塞就排查真端，和真端一致”指令，排查真端原码与表，坐实：真端 `<name>` 均唯一（对应本服 `name_desc`），优先以 `name_desc` 解析即消除全部 22 假歧义；150 个怪物名由 `<quest_ai_name>` 与副本区域规则完全覆盖。**否决方案 A（不搞部分留在旧路径的妥协），SimpleHunt 939 行 100% 原生切换** | ✅ 真端已坐实，纳入解析器 |
| G3 | ~~PAGE-FLOW SPIKE~~ **已完成**：`pageflow-spike.md`——页 id 全有 HP 出处；8 项家族级冻结约定登记（全族共形，不阻切换）；文档漂移 RETAIL_MULTI_TIER 登记 | ✅ 已清 |
| G4 | 本声明书 + 文件/测试清册经用户授权（每批独立授权，计划 §7） | ⏳ 本文档即声明，待授权 |

G1..G4 全绿 → 按 §3/§4 清单原子执行（新路径 + 旧路径删除同批、同提交候选，不 push）。
另：不变式 7（raw vars 存档）已闭环 CLEAN（§2.5），不再是前置项。
