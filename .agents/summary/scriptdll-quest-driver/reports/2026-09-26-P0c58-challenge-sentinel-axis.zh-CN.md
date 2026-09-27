# 2026-09-26 续片 30 / P0c-58：挑战任务哨兵轴（6 行采纳，`ACQUIRE_NPC_UNRESOLVED` 桶清零）

## 0. 一句话

`RETAIL_ACQUIRE_NPC_UNRESOLVED` 桶的最后 6 行是**挑战任务哨兵行**：真端表的接取参数写
`_challengetask_`（哨兵，不是 NPC 名），而挑战任务由**交付 NPC 本人**发放——客户端 npc 块在该 NPC 上
声明的对话路由名（`quest_ai_name`）就是真端 reward 名 ⇒ 接取名回退为真端 reward 名，走既有 hunt/pvp
合成器；**6 行全部采纳退役**，该桶从 6 → **0**（1508 行宇宙里此码不再出现）。

## 1. 缺口：桶里最后 6 行的形状

`p0c58-challenge-sentinel-decisions.tsv`（6 行 × 14 列）逐行形状：

| quest | DD 接取 | DD reward | 解析 id | 客户端 `quest_ai_name` | 进度 | 客户端登记 |
|---|---|---|---|---|---|---|
| 17160 / 17161 | `Talk` + `_challengetask_` | `LF5_Atmos_E` | **804699** | `LF5_Atmos_E` | Hunt 3 怪 ×10 | 入口页 4762 / 任务书 2 行 / 击杀进度行（section 1, count 10） |
| 17162 | 同上 | `LF5_Atmos_E` | **804699** | `LF5_Atmos_E` | PVP 6 | 入口页 4762 / 任务书 2 行 |
| 27160 / 27161 | 同上 | `DF5_Haldor_E` | **804719** | `DF5_Haldor_E` | Hunt 3 怪 ×10 | 同形（暗面） |
| 27162 | 同上 | `DF5_Haldor_E` | **804719** | `DF5_Haldor_E` | PVP 6 | 同形（暗面） |

**判据（全静态，遗留 XML 只作见证）**：

| 源 | 提供什么 |
|---|---|
| 真端 DD 表 | `category_acquire_ = Talk` + `value0_acquire_ = _challengetask_`（哨兵语义："由挑战任务 NPC 发放"）+ `reward_npc_name` 唯一解析（服务端 npc 模板 `name_desc` → npc_id，804699/804719 各 1 命中） |
| 客户端 npc 块（外部 `client_npcs_npc.xml`） | 804699 的 `<quest_ai_name>LF5_Atmos_E</quest_ai_name>` / 804719 的 `<quest_ai_name>DF5_Haldor_E</quest_ai_name>`——客户端自己把该对话路由名声明在这只 NPC 上（接取与交付同人的客户端侧背书） |
| 客户端登记 | 入口页 4762（select_none）+ 任务书 2 行 + 4 行的击杀进度行（`ladder=0 section=1 count=10` + 怪名，与 DD 怪名同集合）+ 按钮签名 `select_none#20000/#20001 + select_success#1009` |
| 遗留 XML（见证） | `NPC_START npc-id=804699 start-page=SELECT_NONE source=unaccepted target=started` 与同 id `npc-complete`（**接取与交付同一 NPC**）；节点集 `unaccepted: NONE, started/k1..k6: START, reward: REWARD, complete: COMPLETE` |

⇒ 结论：**accept-at-reward-npc**（P0c-12 判例的第二次兑现，这次是 DD 家族、且有客户端 `quest_ai_name` 直证）。

## 2. 代码改动（受守卫的一处名字回退）

`RetailDataDrivenDefinitionCompiler`：

- 新增常量 `CHALLENGE_TASK_SENTINEL = "_challengetask_"`；
- 在接取名解析前插入回退：`哨兵（trim 后相等）∧ metadata.category() == "CHALLENGE_TASK"` ⇒
  `acquireParam = rewardName`（回退名仍走同一条名字通道解析；**未解析即由下方门如实拒绝**，
  拒绝详情带 `原始参数 -> 回退名` 便于排障）；
- **接取名必须贯到 hunt 表行**：`toHuntEntry(entry, acquireParam)` 新增参数——hunt 合成器从 plan
  重新读接取名，若仍传原始参数，哨兵会被 hunt 表按"未知类别哨兵"拒绝
  （**实测踩到**：首版只改了 DD 层的门，分类结果从 `ACQUIRE_NPC_UNRESOLVED` 变成
  `ACQUIRE_NPC_SENTINEL` 而不是采纳 ⇒ 说明它已走到 hunt 的哨兵判定，但 plan 里还是哨兵）。

`RetailDataDrivenGateTest` **无需改动**：这 6 行的形状（Talk 接取 + 单段 hunt / 纯 PVP）本就在
`huntScope`/PVP 已覆盖范围内，门禁未报"采纳行越出已覆盖形状"。

## 3. 爆炸半径（改动前先量）

两次 `-Dretail.dataDriven.equivOut` 实测对拍（1508 行全量分类）：

| 桶 | 改前（`p0c56-classification-post.tsv`） | 改后（`p0c58-classification-post.tsv`） | Δ |
|---|---|---|---|
| `ADOPTED` | 1211 | **1217** | **+6** |
| `REJECTED:RETAIL_ACQUIRE_NPC_UNRESOLVED` | 6 | **0** | **−6** |

逐行只有 **6 行**变动（全部 `→ ADOPTED`），其余 1502 行逐字节同值、键集不变。

## 4. 落地与收口

| 步 | 内容 |
|---|---|
| 代码 | §2（1 文件 2 处；DD 门禁文件零改动）；另有两个契约测试的**退役连带口径迁移**（§5.1：`MirrorRewardProjectionLagContractTest` 名单 + 注释、`QuestLegacyMonsterHuntProductionFlowTest` 该方法断言体迁真端形） |
| 清单 | 5 副本（src/main + src/test + target/classes + target/test-classes + `.agents` 快照）6 行翻 `RETAIL_TABLE`（basis=`DD_CHALLENGE_SENTINEL_ACCEPT_AT_REWARD_NPC`；whipsaw 守卫先验四副本同字节） |
| 生产面 | `quests/{17160,17161,17162,27160,27161,27162}.xml` 各删 2 副本；`quest_definition_catalog.xml` 各删 2 行 |
| 登记 | 漂移表 6 行 → `ADOPTED`（双副本，头注直方图按数据行重算；其余行逐字节保留） |
| 指纹 | `retail-data-driven-ir-fingerprints.tsv` 新增 6 行（1211 → **1217**，双副本升序插入）；存量 1211 行对 dump **零漂移** |
| 核验 | `verify_retirement.py` = `catalog=1266 / directory=1266 / retired=4958 / sum=6224` + `OK 目录一致，无悬空生产引用` |

采纳行的真端拥有量：`owned ∩ family = 1217`（门禁下限 1073）。三对副本 md5 一致
（manifest `24f8ca5d7950` / 漂移 `9a24b8365e9a` / 指纹 `e28a8101a8de`）。

**IR 形状**：4 个 hunt 行为 14 节点 / 87 转移（单段 10 杀 + 领奖行），2 个 PVP 行为 10 节点 / 55 转移
（`KillInWorld(0)` 单计数段）。

## 5. 门禁

### 5.1 退役连带：两个契约测试的口径迁移（本片必做，非掩盖）

退役 XML 会把"锁遗留 XML 形状"的合同测试打成红：这类红**是退役的真实后果**（被锁的形状已不存在），
按判例（P0c-8c/P5-1 对 21120/26988 的处理）把断言迁到真端形，并在文件内写明"旧形 = 同一语义的另一种表示"。

| 测试 | 旧断言（遗留 XML 形） | 迁移后（真端形） | 判据 |
|---|---|---|---|
| `MirrorRewardProjectionLagContractTest`（批次 18 行锁） | 17160/17161 是"镜像单侧落后"行：领奖投影 = 镜像行号 1，且有一条**无 source 的 enter-world 修复边**把 var0=0 自愈成 1 | 四个 id（17160/17161/27160/27161）进 `RETAIL_DRIVEN_RETAIL` ⇒ 行锁豁免；改由 `retailDrivenShardsProjectTheirSaturatedCounters` 断言**无修复边 + 领奖投影为饱和计数**（更强的真端不变量） | 实测：17160 修复边数 = **0**（真端形不保留无 source 修复边）；27160 领奖投影 var0 = **10**（击杀计数）而非行号 1 |
| `QuestLegacyMonsterHuntProductionFlowTest.challengeMonsterHuntsUseTheRetailClientAcceptAndReportPages` | `started` 自环 `KillNpcSet` 计数边（priority 1/0）+ `reward -> reward` 重开页（DEFAULT_SUCCESS）+ `unaccepted -> started` 接取边（804719） | 网格形：`a0 -> a1` 三目标击杀步（空条件/动作、`PACKET_ONLY`）+ 未接态 `QUEST_SELECT` → **入口页 `SELECT_NONE`(4762)** + `unaccepted -> a0` 的 `ACCEPT_SIMPLE`(20000)（`StartEligible` + 可见性刷新 + 关窗）+ 饱和段 `QUEST_SELECT` → `DEFAULT_SUCCESS`(10002) + `a10 -> reward` 的 1009 报告页，并断言零段**无**提前上交 | 探针实测（临时探针已删）两测 id 的 IR：接取人 = 804699（光）/804719（暗）、三目标、10 杀、饱和段即报告门控；本片顺手把**光侧 17160/17161 也纳入**（原测试只覆盖暗侧） |

测试文件的改动只有：`MirrorRewardProjectionLagContractTest` 的 `RETAIL_DRIVEN_RETAIL` 名单 + 注释（+ 类 javadoc 一句），
`QuestLegacyMonsterHuntProductionFlowTest` 该方法的断言体 + 新增 `CHALLENGE_HUNTS` 常量表。**没有删除任何断言族**，
豁免仅限"已采纳真端驱动的行"，且豁免后立刻由真端不变量（饱和计数 / 无修复边 / 客户端页边）接管。

### 5.2 结果

| 档 | 结果 | 归因 |
|---|---|---|
| DD 门（`p0c58-dd-gate-post-patch.log`） | 6 例 **1F** | 唯一红 = 车道 `20035` 码位（**本片不改**；与 P0c-56 同一行） |
| T1（`gates/T1-225858.log`） | **75 例 1F**（437s，与车道并发跑） | 唯一红 = 同一条车道 `20035`（`driftVersusShellsIsRegistered:430`）；`RetailSimpleTalkChainGateTest` 本轮已绿（车道 22:49 的指纹在飞重冻已自愈）；`PRODUCTION_COMPILE_OK=1239` |
| T2（`gates/T2-231800.log`，6 个 id） | 复跑 **124 例 1F**（421s） | 唯一红 = 车道 `20035`。首跑（22:47，`T2-224728.log`）曾 **124 例 5F**：3F = 本片退役连带（MirrorReward ×2 + 挑战 hunt 遗留形 1，按 §5.1 迁移口径后单类复跑 49/49 绿）+ 1F 车道 `20035` + 1F 车道 talk-chain 指纹在飞重冻（22:49:57 车道写冻结表所致，22:58 的 T1 已绿） |
| T3（`gates/T3-232501.log`） | **2013 例 117F/22E/1S**（415s，forkCount=2） | 对三基线逐条身份集对拍（`p0c52_t3_attribution.py`，107 ↔ 107）各 **ADDED 0 / REMOVED 0**：`T3-221706`（本片代码改动前）/ `T3-221828`（车道并发）/ `T3-230841`（车道同态、已含本片改动）；本片 6 个 id 在全量日志**零命中** |

## 6. 余量：接管轴清零后的下一站

`ACQUIRE_NPC_UNRESOLVED` **清零**（1508 行宇宙内此码不再出现）。余量桶现状：
`SENTINEL_AREA 64`（真端零绑定，**禁从代码轴再攻**）/ `MONSTER_UNRESOLVED 58` /
`TALK_HUNT_CHAIN_DEFERRED 55` / `HANDIN_VOCABULARY_UNSUPPORTED 48` / `TALK_CHAIN_DEFERRED 23` /
`REWARD_NPC_UNRESOLVED 7` / `ITEMPLAY_ACQUIRE_EVENT_DEFERRED 6`（事件轴，登记真端缺口）。

## 7. 编号消歧（同轮并发车道，第三次撞号）

| lane | 编号 | 续片 | 主题 |
|---|---|---|---|
| **DataDriven / 门禁车道（本片）** | **P0c-58** | 续片 30 | 挑战任务哨兵轴（6 行采纳，桶清零） |
| SimpleTalk 链车道（并发） | P0c-57 | 续片 33 | 接取轴（`LEGACY_ACCEPT_ROLE_SPREAD`）——其产物 `p0c57-accept-axis-*.tsv` / `p0c57_accept_axis_*.py` |

本片原编号 P0c-57，动工时发现链车道已占用（其 `p0c57-accept-axis-census.tsv` 已在盘），
按判例**让号改取 P0c-58**，本片全部产物（8 个文件）随即改号，`p0c57*` 前缀归链车道。

## 8. 结论

挑战任务哨兵行的接取人在**真端数据内可静态判定**（DD reward 名 + 客户端 `quest_ai_name` 双重声明），
不需要真端 `challenge_task.xml` 的 npc 字段（该文件本就不含 npc id）；由此 `ACQUIRE_NPC_UNRESOLVED`
桶清零，DD 家族的可驱动行达到 **1217 / 1508**。
