# QE-106：客户端确认的 NPC 集合轴（接取 + 交付）与 23 行 owner 翻转

> 用户 Goal（2026-09-30）：「摒弃当前的任务引擎，完全按真端来，当前任务引擎只负责现有的任务 XML 运行。」
> 上一片（`README.zh-CN.md`）证明 25 行 `ADJUDICATED:RETAIL_ACQUIRE_NPC_UNRESOLVED` 的真端名**不是不存在**，
> 而是「逻辑名 → 多变体 id 集合」，且集合与客户端 `start_npc_ids` **逐元素相等**；当时结论是引擎能力缺口。
> 本片把该能力落到引擎层，并把同一缺陷类的**交付侧**（客户端 `end_npc_ids`）一并补齐。

## 1. 判据（客户端是唯一仲裁）

真端模板的 `acquired_npc_name` / `reward_npc_name` 是**逻辑 NPC 名**（例：`HousingManager_Li` = 任意一名住宅管家；
`NPC_event_svs_jabsuroong` = 光/暗两侧的活动 NPC）。服务器按变体逐 id 建模板：
`810017:HousingManager_L_S … 810021:HousingManager_L_D`、`804749:GAb1_Sub_01_Fuen_E …`。
因此：

- 真端名解析出**恰一个** id ⇒ 放行（既有行为）。
- 真端名解析出**多 id** ⇒ 只有与该任务**客户端声明的集合逐元素相等**才放行；否则维持 fail-closed
  `RETAIL_ACQUIRE_NPC_AMBIGUOUS` / `RETAIL_REWARD_NPC_AMBIGUOUS`。禁止按名字猜、禁止按登记补页。
- 投影表自身 fail-fast：单 id 行不是集合轴输入（`load()` 抛 `IOException`）。

## 2. 引擎改动（本片生产代码）

| 文件 | 改动 |
|---|---|
| `RetailClientAcceptNpcSets`（新） | 客户端接取集合投影加载器（`/quest/quest_client_accept_npc_sets.tsv`，680 行） |
| `RetailClientHandinNpcSets`（新） | 客户端交付集合投影加载器（`/quest/quest_client_handin_npc_sets.tsv`，215 行） |
| `RetailSimpleTalkDefinitionCompiler` | `requireAcquire`/`requireReward` 改为「唯一 id 或客户端确认集合」；接取段按集合逐 NPC 展开（`TalkToNpc(npcId, action)` 天然分键）；交付集合同口径；**过场泄漏修复**：`cs1_haction=1009` 已重挂到交付段，不再落阶段同号动作边（否则 18806/28806 各播两次片） |
| `RetailSimpleItemPlayDefinitionCompiler` | 同上两轴；交付段与完成流按集合展开；奖励窗自动确认共用**一条全局 108 路由** + 每个交付 NPC 一条（避免重复注册） |
| `RetailQuestDriver` | 生产装配注入两个投影（与既有客户端登记同规格） |
| `retail-npc-name-aliases.tsv` | 追加 7 条真端逻辑名 → 变体 id 集（`gab1_sub_fuen_e`、`housingmanager_li|da`、`npc_event_svs_jabsuroong`、`npc_event_devasday_shugo`、`npc_event_2014christmas[_d]`） |

客户端投影生成器：`.agents/summary/quest-acquire-npc-set/build_quest_client_accept_npc_sets.py`
（来源 = 客户端对话映射 `legacy-quest-dialog-contracts.csv`，`source_sha256=85016518d757e07f829c5a92fa8189053f65d9db865b1ee34690079486b6adb7`）。

## 3. 效果（25 行分类变化，逐行可复核）

- **23 行首次可合成并翻转**（`RETAIL_TABLE` + XML 删除 + 目录登记行删除）：
  `14211 18806 18821 18827 18829 18831 24211 28806 28821 28827 28829 28831 50042 50043 50047
   50049 50050 50051 51042 51043 51049 51050 51051`（全 SimpleTalk）。
- **2 行换轴仍保留**：`18830 28830` 的**谈话角色**（`HousingManager_Li/Da`）多解且客户端无该角色集合声明
  ⇒ 分类由 `REJECTED:RETAIL_TALK_NPC_UNRESOLVED` 升级为 `REJECTED:RETAIL_TALK_NPC_AMBIGUOUS`（drift 登记同片更新）。
- **3 行 ItemPlay 换轴仍保留**：`18828 28828` → `RETAIL_CON_QUEST`（前置任务轴）、`50048` → `RETAIL_TALK_CHAIN`（对话链轴）；
  retention 理由与 `RetailSimpleItemPlayGateTest.REGISTERED_REJECTS` 同片改为新码。
- 家族门口径刷新：`retail-simple-talk-drift.tsv` 25 行、`RetailSimpleTalkGateTest` 的接取不变量改为「集合内每个 NPC 各有一条接取段」
  + 新增负例门 `clientNpcSetMismatchStaysFailClosed`（少 id / 未声明 / 交付侧缺登记 / 单 id 投影行）。

## 4. 翻转证据（QE-104 生产路径对拍）

方法（`probe/`）：A = 现行 owner（XML 在场，`ProductionQuestDefinitions.definitionInOverlay(id)`）；
B = 临时把 23 行翻成 `RETAIL_TABLE` **并**移走 XML + 删目录登记行后，同一入口再 dump（overlay fail-fast 要求两者同时改）；
判据 = 节点标签集 + `event/conditions/actions/afterCommit` 四元组计数。

结论：**节点集 21/23 相同**（18806/28806 因真端行含 `talk_npc1` 链段 ⇒ 多一个 `step1` 态，客户端任务书同为 2 行）；
**23/23 行共享边非零**（无 QE-104 的零共享红线）；逐行 `shared/onlyXml/onlyRetail` 见
`probe/divergence-summary.json` 与决策台账 `qe-106-npc-set-decisions.tsv`。
差异面完全落在本片已定的轴上：①接取/交付角色按客户端集合展开（同一逻辑名的多个变体各一条路由）；
②页梯退场（阶段首屏直发 + `1008` 关窗，本地翻页交客户端）；③奖励窗 `108` 自动入口。
无新增轴、无隐藏行为（临时翻转已还原，`git status` 复核）。

**旧形缺陷实证**：被删的 `18821.xml` 注释写明「五类管家 NPC（810017-810021）共用同一对话状态机」，
而 14211 旧形只带 `804749` 一个变体 —— 集合轴正是把「只能找某一个变体」修成「客户端声明的任一变体」；
管家由 `houses.xml` 的 `manager_npc` + `House#spawnButler` 按地块生成，五类都会真实出现。

## 5. 真机验收清单（用户执行；每行按任务书自上而下走一遍）

1. **18806 / 28806**（`[Daily]` 住宅管家）：①在自己的房子里与**任意**管家对话能接取（旧形只认 `810017/810022` 一个变体）；
   ②接取后播过场 803 一次（不再播两次）；③与 `Kaionen`（830194/830211）交付，任务书行号投影与客户端一致。
2. **18821 / 28821 / 18827 / 18831 / 28827 / 28831**（管家日常/连锁）：在**任意**管家的房子内接取与交付；确认没有页梯中转页
   （只出现阶段首屏），且交付直翻领奖窗。
3. **14211 / 24211**（要塞委托）：与 `GAb1_Sub_01..04_Fuen_E`（804749-804752）四名中任意一名接取与交付。
4. **50042 / 50043 / 51042 / 51043**（活动物资）：与 `NPC_event_svs_{l,d}c1_jabsuroong`（832850/832851）两侧任一名接取与交付。
5. **50047 / 50048**（活动日）：与 `event_devasday_shugo_{l,d}_01`（833051/833052）任一名交互。
6. **50049-50051 / 51049-51051**（2014 圣诞红色/蓝色雪人）：接取与交付 NPC = 833509 / 833510，任务书页面链与奖励窗一致。
7. 通用回归：接取窗直发（页 4）→ 交付 `QUEST_SELECT` 直翻领奖窗 → `SELECTED_QUEST_REWARD1..NOREWARD` 全段可完成；
   奖励窗 108 自动领奖入口出现且只出现一次。

## 6. 门禁与验证

- `RetailSimpleTalkGateTest` 6/6、`RetailSimpleItemPlayGateTest` 1/1、`RetailOwnershipGateTest` 3/3 —— 绿。
- `run_quest_gates.sh T3`（整个 questEngine 树）：`grep "^\[ERROR\]   [A-Za-z]" | sed 's/:.*//' | sort -u`
  与基线 `target/agent-logs/full-head.ids` / `full-reanchor.ids` / `full-final-keep.ids` 对拍 ⇒ **零新增失败**
  （日志 `target/agent-logs/qe106/T3-204934.log`、id 集 `target/agent-logs/qe106/t3.ids`）。
- `verify_retirement.py`：`catalog=761 directory=761 retired=5463 sum=6224 — OK`。
- 临时探针（翻转 + XML 移走）已全部还原；探针源码与产物留在 `probe/`（`ZzAcquireSetFlipProbeTest.java.txt`、
  `run_flip_probe.sh`、`ir-xml-owner.txt`、`ir-retail-owner.txt`、`divergence-summary.json`）。

## 7. 边界与未做

- 未做真机验收（上表）；未动其他家族的集合轴（Hunt/DataDriven 等仍走各自编译器的既有判据）。
- DataDriven 的 3 行（80885/80940/80961）不在本轴（`RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED`，世界事件轴）。
- 谈话角色（`talk_npc1/2`）多解仍是缺口（18830/28830）：需要「客户端声明的谈话角色集合」轴，本片只把代码升级为 `AMBIGUOUS`。
