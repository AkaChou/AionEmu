# 缺口批 1 收口台账（哨兵接取族 192 行 · quest-native-dispatch）

> SEMANTIC_GAP：**595 → 412**（−173 = 24 受理 flip + 149 逐行裁定）；9 行码对齐（仍在 SEMANTIC_GAP 内换码）。
> 硬规则：单写者串行、锚定式编辑、fail-closed 门先红后修（10 行回退留痕）。

## 根因二分（逐码逐行，判据 = 三路交叉证据）

| 码 | 行数 | (a) 可迁移 | (b) 不可迁移/其他 | 判据与证据 |
|---|---|---|---|---|
| `RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH`（_challengetask_，SimpleHunt） | 84 | **24 flip** | 50 裁定 + 10 延期 | 四源二源硬判据：真端 `reward_npc_name` 唯一解析 ∧ 客户端 lifecycle 在**同一 NPC** 登记 `NPC_START` 接取边（NONE→START）⇒ 挑战 NPC 本人即接取人；`b1-acquire-sentinel/b1_forensics.tsv` 逐行 |
| `RETAIL_ACQUIRE_NPC_SENTINEL_AREA_PENDING`（_area_，DataDriven） | 64 | 0 | 64 裁定 | `ai-areas.xml` 无该任务的 `quest_area` 绑定（P0c-4 已按真端世界文件补齐过 ⇒ 未绑定=真端世界文件本就没接线）；迁移=发明接线，禁止 |
| `RETAIL_ACQUIRE_GRANT_UNSUPPORTED`（DataDriven） | 17 | 0 | 8 裁定 + **9 码对齐** | 冻结 drift 表为准（retention 滞后陷阱，§5-4）：9 行真实码已漂移 `RETAIL_TALK_HUNT_CHAIN_DEFERRED`×8 / `RETAIL_TALK_COLLECT_CHAIN_DEFERRED`×1 → 换码归批 3；8 行 EnterArea（6 行 CollectItem 进度形 + 2 行 noProgress 未绑定）裁定 |
| `RETAIL_ACQUIRE_NPC_UNRESOLVED`（Talk 19 / DD 3 / ItemPlay 3） | 25 | 0 | 25 裁定 | 接取 NPC 名（`HousingManager_*`/`NPC_event_*`/`GAb1_*` 等）在本服 `npc_name_index` 缺失 ⇒ 无法合成接取/交付边 |
| `RETAIL_ACQUIRE_NPC_SENTINEL`（_faction_，SimpleItemPlay） | 2 | 0 | 2 裁定 | 39713/49713：`_faction_` 接取 + 交付/对话 NPC `LDF5b_Greenhat_LD` 本服缺失（无交付 owner，faction 发放形不可用） |
| **合计** | **192** | **24** | **159** | +9 码对齐 |

## 生产改动（受理 flip 三件套 + 归一化边）

1. **新常量类** `RetailChallengeAcquireAdoptions`（24 条目冻结集：quest_id → 挑战/交付 NPC id；
   800447×11 / 800452×11 / 800446×3... 见类 javadoc 四方证据清单）。
2. **归一化边** `RetailSimpleHuntPlan.bind()`：`CHALLENGE_TASK ∧ isAdopted` ⇒ 接取名/ids/grantKind/
   对话名组判定**整条管道**归一化为交付 NPC（并行车道方法论：归一化名必须贯到下游表行）。
3. **三件套**：retention（双副本）24 行 `RETAIL_TABLE/OK` + 证据列指向裁定登记；遗留 XML 删 24 个；
   `quest_definition_catalog.xml` 删 24 条（`target/classes` 孤副本同片清理）。
4. **裁定登记**：`retail-simple-hunt-adjudicated-decisions.tsv` +24 行（`ADOPT_RETAIL /
   CHALLENGE_TASK_NPC_DELIVERY`）；rejects fixture −24 行（24 行不再是拒绝）。
5. **裁定保留新前缀** `ADJUDICATED:<码>`：retention 149 行（保留行逐行有证据）；判据 ② 词汇按
   DoD 扩展（归属门 `RETENTION_REASONS` +ADJUDICATED）。
6. **fail-closed 门**：家族门新增分支——`ADJUDICATED:` 行必须**仍以裁定内嵌码被拒**（开始受理=裁定
   过期 ⇒ 红；拒绝码漂移 ⇒ 红）；新增采纳门 `challengeAcquireAdoptionsMatchRegistryAndResolveToDeliveryNpc`
   （采纳集 == 裁定登记 ∧ 逐条真端行 CHALLENGE_TASK ∧ 交付名 `resolvePartyName` 恰为采纳 NPC）。

## fail-closed 实录（10 行回退，门先红后修）

首轮采纳 34 行 ⇒ 等价门红 `哨兵裁定登记失同步`：17011/17015-17018/27011/27015-27018 十行编译被拒
（`retail.hunt.rejectsOut` 实测全部 `RETAIL_MONSTER_UNRESOLVED`——击杀目标是 `WorldRaid_*`/
`IDSeal_Boss_Vritra_Q18952` 类别名 token，本服 NPC 数据不可解析；`withClientKillTargets` 通道只接在
DD 编译器且客户端登记只覆盖 Iluma/Norsvold）。**处置**：10 行退出采纳集（采纳类/decisions/retention
回 `XML_RETENTION + ADJUDICATED:NO_GRANT_PATH`、恢复 XML + catalog + rejects 登记），flip 延期到批 2
击杀目标轴——**采纳边本体无罪，10 行的堵点是另一个码族**。

## 快筛（`gates/b1-screen1.log` / `gates/b1-freeze2.log` / `gates/b1-screen2.log`）

- 归属门 4/4 绿（6224 全集 + catalog∪retired 恒等 + ADJUDICATED 前缀合规）；
- ItemPlay 门 1/1 绿（5 行 ADJUDICATED 前缀期望）；
- **家族门 2/2 绿**（覆盖全 942 登记行重编译：24 行采纳全合成；50+10 裁定行以同码被拒；
  采纳门新测试绿——采纳集 == 裁定登记 ∧ 逐条交付名唯一解析；RETAIL_TABLE_GAPS/DRIVER_COVERAGE
  既有不变量零回退）；
- **等价门 2/2 绿**（裁定集 344→368 = +24，全部真端编译；24 行不参与 XML 对拍——XML 已非金标准，
  语义由家族门 + 裁定指纹冻结承担）；裁定指纹 fixture 安装（+24 行，两轮 dump 逐字节相同证确定性）；
- 指纹/红集对拍：M1 里程碑 T1∥T3 统一执行（紧随本批提交）。

## 批 1 记录

- 移交/延期：10 行挑战采纳延期（批 2 击杀轴解锁后凭同一采纳边直接 flip）；9 行码对齐行归批 3 裁定。
- 待实机复验：24 行进入复验清单（见 `client-recheck-list.zh-CN.md`）。
- 纪律：未 push；未启停服务；未新增/退役 TSV（EXPECTED_TSV_COUNT 保持 22）；兄弟车道文件只读；
  中间产物落 `b1-acquire-sentinel/`（forensics/apply/revert 脚本 + 报告）。
