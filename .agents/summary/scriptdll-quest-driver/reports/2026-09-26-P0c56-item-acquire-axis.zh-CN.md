# 2026-09-26 续片 29 / P0c-56：物品接取轴（ItemPlay 非 event 12 行采纳）+ event/challenge 延后码收敛

## 0. 一句话

`RETAIL_ACQUIRE_NPC_UNRESOLVED` 桶 21 行的接取字段写的是**任务起始道具符号**（`doc_quest_13952a` 形），
不是 NPC——它是**物品接取轴**，此前零采纳；本片给 `category_acquire_ = ItemPlay` 开一条受守卫的通道
（`UseItem(道具)` 开客户端入口页 → 无主 `ACCEPT_SIMPLE/REFUSE_SIMPLE`），**采纳退役 12 行**；
同桶里 `category1=event` 的 3 行与超集 3 行（80827/80828/80832）从"接取 NPC 解析失败"改登记为
更准确的 **`RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED`**（事件激活轴未落地），余 6 行 `_challengetask_`
哨兵行留作下一站。

## 1. 缺口：21 行里 12 行是"用道具接取"

`p0c56-item-acquire-census.tsv`（21 行 × 六源）逐行形状：

| 形状 | 行数 | `category1` | 接取参数 | 处置 |
|---|---|---|---|---|
| 物品接取（非事件） | **12** | `seen_marker` 10 / `Primary` 2 | `doc_quest_*` / `quest_*` / `QUEST_*a` 道具符号 | 本片 **ADOPT** |
| 物品接取（事件） | 3 | `event` | `quest_80885a` 形 | 延后（`RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED`） |
| Talk + 哨兵参数 | 6 | `challenge_task` | `_challengetask_` | 延后（下一站，非物品轴） |

12 行的**四源一致**（决策表 `p0c56-item-acquire-decisions.tsv`，basis=`DD_ITEMPLAY_ACQUIRE`）：

| 源 | 提供什么 |
|---|---|
| 真端 DD 表（接取轴权威） | `category_acquire_ = ItemPlay`，`value0_acquire_` = 任务起始道具符号；`value1_acquire_` 空（无第二参数） |
| 遗留 XML（形状见证） | 接取边**恰为** `<use-item item-id=…>` + 条件 `start-eligible` + `SHOW_QUEST_PAGE`；接取/拒绝是**无主** `<dialog type="QUEST_ACTION" action="QUEST_{ACCEPT,REFUSE}_SIMPLE"/>`；节点集 `unaccepted:NONE,reward:REWARD,complete:COMPLETE`；`use_items` 与 `work_items` 同 id（15563/16809 逐字核） |
| 客户端 action-details（按钮权威） | 12 行**同一签名**：`select_none#20000:HACTION_QUEST_ACCEPT_SIMPLE \| select_none#20001:HACTION_QUEST_REFUSE_SIMPLE \| select_success#1009:HACTION_SELECT_QUEST_REWARD`（页 4763 不在任何一行） |
| 道具模板（道具侧见证） | 4 行 `queststart questid=<自身>`（+2 行 read）；2 行无 `<actions>`；6 行仅 `<read/>`——**证据列不否决**：AionEmu 不消费 `QuestStartAction`（全仓无消费者，`ItemActions` 仅反序列化），接取窗由服务端 `SHOW_QUEST_PAGE` 下发，遗留实现对这 12 行一律同形 |

⇒ 判据：**接取位点由道具承担**（无接取 NPC）、接受/拒绝为无主对话事件、交付仍走既有交付 NPC 通道。

## 2. 代码改动（受守卫的通道扩域，不改既有形状）

1. `RetailDataDrivenDefinitionCompiler`
   - 新轴 `itemAcquire = "itemplay".equalsIgnoreCase(acquireCategory)`；**事件判据优先**于道具解析
     （`itemAcquire ∧ metadata.category()=="EVENT"` ⇒ `RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED`），
     事件行不因道具表变动在 `ITEM_UNRESOLVED`/`EVENT_DEFERRED` 间漂移；
   - `itemSymbol`（大小写不敏感，委托 `RetailItemNameIndex.resolve`）解析道具 id，失败 ⇒
     `RETAIL_ITEMPLAY_ACQUIRE_ITEM_UNRESOLVED`；
   - `npcAcquire` 收窄为 `¬enterarea ∧ ¬none ∧ ¬enterworld ∧ ¬itemplay`（物品行不查 NPC 名表）；
   - 接取门放行 `itemAcquire`（其余形状照旧如实拒绝）。
2. `RetailDataDrivenTalkCompiler`
   - 新增 `buildItemAcquire(questId, itemId, rewardNpc, …)` 与 `itemAcceptFlow(itemId, "started", entryPage)`：
     `UseItem(item)` → `ShowQuestDialog(入口页)`；`QUEST_ACCEPT_SIMPLE` ⇒ `started` + 可见性刷新 + 关窗；
     `QUEST_REFUSE_SIMPLE` ⇒ 原地关窗（条件为空，与遗留同形）；
   - 抽出共享装配 `assemble(...)`：**报告流与完成流与既有 NPC 形逐字节同源**（领奖窗/RewardNpc 通道/
     领奖行修复共用），物品形与 NPC 形不可能各写一份而漂移。
3. `RetailDataDrivenGateTest`
   - 采纳范围谓词新增 `itemAcquireNoProgressScope`（`itemplay` ∧ 无进度）——采纳集范围的显式白名单，
     越出即红。

**只注册客户端实际可见的按钮**：物品形不发 `ASK/ACCEPT_1` 等页（客户端本行未声明，多发会让契约门报
`BUTTON_WITHOUT_ROUTE`/未声明页）；门禁 `QuestClientContractGateTest` 1/1 绿为该设计的独立证据。

## 3. 爆炸半径（改动前先量）

两次 `-Dretail.dataDriven.equivOut` 实测对拍（1508 行全量分类）：

| 桶 | 改前（`p0c56-classification-now.tsv`） | 改后（`p0c56-classification-post.tsv`） | Δ |
|---|---|---|---|
| `ADOPTED` | 1199 | **1211** | **+12** |
| `REJECTED:RETAIL_ACQUIRE_NPC_UNRESOLVED` | 21 | 6 | −15 |
| `REJECTED:RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED` | 0 | **6** | **+6** |
| `REJECTED:RETAIL_TALK_VOCABULARY_UNSUPPORTED` | 9 | 6 | −3 |

逐行只有 **18 行**变动（12 采纳 + 6 码位前移），其余 1490 行逐字节同值，键集不变。

## 4. 落地与收口

| 步 | 内容 |
|---|---|
| 代码 | §2（3 文件；`RetailDataDrivenTalkCompiler`/`RetailDataDrivenDefinitionCompiler`/`RetailDataDrivenGateTest`） |
| 清单 | 5 副本（src/main + src/test + target/classes + target/test-classes + `.agents` 快照）12 行翻 `RETAIL_TABLE`，reason `basis=DD_ITEMPLAY_ACQUIRE`；whipsaw 守卫（四副本先逐字节一致） |
| 生产面 | `quests/{13952,15563,16809,16810,16811,16975,23952,25563,26809,26810,26811,26975}.xml` 各删 2 副本；`quest_definition_catalog.xml` 各删 2 行 |
| 登记 | 漂移表 18 行（12 → `ADOPTED`；6 → 事件码；双副本，头注直方图按数据行重算），其余行逐字节保留 |
| 指纹 | `retail-data-driven-ir-fingerprints.tsv` 新增 12 行（1199 → **1211**，双副本，升序插入）；存量 1199 行对 dump **零漂移**（守卫证明本片不动既有语义） |
| 核验 | `verify_retirement.py` = `catalog=1272 / directory=1272 / retired=4952 / sum=6224` + `OK 目录一致，无悬空生产引用` |

采纳行的真端拥有量：`owned ∩ family = 1211`（门禁下限 1073）。

**既有 pin 独立确认**：`QuestPrerequisiteRetailContractTest`（`15563 → 15550` 前置映射）在本片 T2 中通过
——物品接取行的元数据前置链与遗留一致。

## 5. 门禁

| 档 | 结果 | 归因 |
|---|---|---|
| DD 门（`p0c56-dd-gate-post-patch.log`） | 6 例 **1F** | 唯一红 = 车道 `20035` 码位（**本片不改**，见 §7 归因） |
| T1（`gates/T1-220227.log`） | 75 例 **1F** | 同上；**客户端契约门 1/1 绿**；归属/白名单/目录清单/物品授予门全绿 |
| T2（`gates/T2-220956.log`，18 个 id） | 78 例 **1F** | 同上（唯一红仍是车道 `20035`） |
| T3（`gates/T3-221706.log`） | 2015 例 **117F/22E/1S** | 对车道同态 `T3-213222`（107 失败身份）与车道并发 `T3-221828`（107）**ADDED 0 / REMOVED 0** ⇒ 零新增，见 §7 |

## 6. 余量：两条轴各自下一站

| 轴 | 行数 | 现状 | 下一站需要的真端证据 |
|---|---|---|---|
| `_challengetask_` 哨兵（Talk 接取） | 6（17160/17161/17162/27160/27161/27162） | `REJECTED:RETAIL_ACQUIRE_NPC_UNRESOLVED`（参数是哨兵而非 NPC 名，属**另一条轴**：接取 NPC 目前只有遗留见证 `804699:SELECT_NONE`/`804719:SELECT_NONE`） | 真端挑战任务表/事件表里"谁发任务"的静态来源 |
| 事件物品行 | 6（80885/80940/80961 + 80827/80828/80832） | `REJECTED:RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED` | 事件激活轴（EventActive/事件 NPC）——**登记为真端缺口**，不谎报为物品轴可解 |

## 7. T3 全量对拍（归因）

本片 T3 = `gates/T3-221706.log`（22:17:06 启动，464s，**2015 例 117F/22E/1S**）。对拍工具
`p0c52_t3_attribution.py`（失败方法集合算术）：

| 对拍对象 | 结果 | 说明 |
|---|---|---|
| 车道同态 `gates/T3-213222.log`（21:32，**本片代码改动前**） | **ADDED 0 / REMOVED 0**（107 ↔ 107） | 零新增失败的**主证据**：失败身份集逐条相同（`p0c56-t3-vs-lane-samestate.out.txt`） |
| 车道并发 `gates/T3-221828.log`（22:18，**含本片改动**） | **ADDED 0 / REMOVED 0**（107 ↔ 107） | 双向互证：另一进程在同一树上跑出的失败集与本片逐条相同（`p0c56-t3-vs-lane-concurrent.out.txt`） |
| 18 个相关 id 在 T3 日志 | **零命中** | `13952/15563/16809/16810/16811/16975/23952/25563/26809/26810/26811/26975` + 6 事件 id 逐个数 0 |
| DD 门残留红 | 仅 `20035` | 与 §5 三档同一行，且**本片动工前**（`p0c54-classification-preflip.tsv`、21:38 车道门日志）同值 ⇒ 车道在飞，本片不触碰 |

**口径说明**：两处并发 T3（本片 22:17:06 与车道 22:18:47）在同一 `target/` 上并行（机器 load ~36），
用例数 2015 ↔ 2013 的差为并发进程所见测试类集不同（本片多 2 例），**失败身份集完全相同**；
由此两份日志互为同态对拍，结论不依赖于单次运行。

## 8. 编号消歧（同轮并发车道）

| lane | 编号 | 续片 | 主题 |
|---|---|---|---|
| **DataDriven / 门禁车道（本片）** | P0c-56 | 续片 29 | 物品接取轴（ItemPlay 非 event 12 行采纳 + 事件码收敛） |
| SimpleTalk 链车道（并发） | P0c-55 | 续片 32 | 阶段腿轴（`p0c55_stage_leg_census.py` / `p0c55-stage-leg-decisions.tsv`） |

`p0c56*` 前缀为**本片独占**（另一车道已用 55，本片按用户指示跳过 55 改取 56）。

## 9. 结论

物品接取轴证明：**接取位点由道具承担**的族在真端数据上可静态判定并整族采纳（12 行），
且报告/完成流与既有 NPC 形共用同一装配（零漂移），客户端契约门独立通过；
同桶余量为**两条各自独立、需真端新证据的轴**（挑战哨兵 / 事件激活），不得从物品轴再攻。
