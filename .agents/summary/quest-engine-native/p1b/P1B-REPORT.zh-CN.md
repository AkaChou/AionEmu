# P1B 报告：SimpleHunt 残余轴批（`con_quest` 链式接取窗 + `cutscene` 过场装载面）

> 主题：SimpleHunt 车道（939 行 / 1865 表行）**残余两轴**——真端交付节点 `0x1e` 槽（`con_quest` 132 行）
> 与 `0x35` 槽（`cutsceneid1`/`cs1_haction` 3 行）——按 P4B 同一不变量接线装载/消费面，并把
> 「动作未被服务 ⇒ 不播」的**休眠面**如实冻结。
> 日期：2026-10-01。分支：`quest`。前置：`p4b/P4B-REPORT.zh-CN.md`（同轴形态）、
> `p1/P1-TRANCHE*.md`（本族切换批）。
> 批门：`SimpleHuntNativeFamilyGateTest` **5/5**（+2）、`NativeQuestTableLoaderTest` 12/12、
> 族门 + tablelane **124/124**、聚焦套件见下。客户端验收 `PENDING_CLIENT`。

---

## 1. 为什么是同一缺陷面（P4B 审计新发现）

P4B 交付的跨族审计（`p4b/tools/conquest-axis-audit.py`，七族全量复算）显示 SimpleHunt **132 行**
声明 `con_quest`、**3 行**声明过场，而 `SimpleHuntRow` 当时只有 `questId/acquiredNpcName/
rewardNpcName/killSlots` 四个字段 ⇒ **列连装载都没有**（比 P4 的「装载未消费」更深一层），
属计划 §10.3-#19 登记的 P1 车道残余。

真端机制与 P4B 同源（同一节点槽语义，见 `p4b/P4B-REPORT.zh-CN.md` §1）：
交付节点 `0x1e` = `mgr+0x1a8(player, con_quest)`（下一环接取窗）、`0x35` = PlayMovie；
运行期列解析器 `fun_912` 逐列读 `con_quest` / `cutsceneId1` / `cs1_haction`。

---

## 2. 落地（实现面）

| # | 文件 | 变更 |
|---|---|---|
| 1 | `NativeQuestTableLoader.SimpleHuntRow` | 新增 `conQuest` / `cutsceneId` / `cutsceneAction` 三列（`optionalInt` 逐行装载，缺列 null） |
| 2 | `SimpleHuntHandler` | 新增 `Cutscene` 记录 + `conQuestByQuestId` / `unresolvedChainQuestIds` / `cutsceneByQuestId` + 注入 `NativeMoviePort`；构造尾按「下一环接取 NPC = 本行交付 NPC」验证**本表内**目标（不闭环 fail-closed 登记，不新增路由）；`onDialog` 拆包装层（命中表声明动作且被本行服务 ⇒ 下发 movie）；新增 `conQuest(int)` / `unresolvedChainQuestIds()` / `cutscene(int)` |
| 3 | `NativeQuestTableLoaderTest` | 逐列样本断言（1339→1340 链、3016 = movie 362 / 动作 1007、缺列行 null）+ 列覆盖冻结（132 / 3） |
| 4 | `SimpleHuntNativeFamilyGateTest` | +2 例：`chainWindowsCloseAtTheHandInNpc`（132 行装载 + 本表内 65 行闭环 + `unresolvedChainQuestIds` 空）、`cutsceneFaceIsLoadedAndFiresOnlyOnAServedAction`（3 行装载 + 注入记录式端口断言「动作未被服务 ⇒ 不播」） |

**未改动**：相机/击杀守卫、接取与领奖段、注册/路由分解（939 = routed + XML_RETENTION 3）逐字未动。

---

## 3. 逐行复算（独立于实现）

| 指标 | 值 | 判定 |
|---|---|---|
| `con_quest` 行 | 132 | 与真端表一致 |
| 本表内目标 | 65 | 全部满足 `target.acquired_npc_name == source.reward_npc_name`（0 例外） |
| 兄弟族目标 | 57 | 同上不变量（Talk/CollectItem/ItemPlay/CombineTask/SerialHunt/UseItem 复核） |
| 无表行目标 | 7 | 其中 3 行 owner 在 XML 车道（目标 1371 / 2258 / 21075），4 行不在本服宇宙 |
| 不闭环 | **0** | `handler.unresolvedChainQuestIds()` 为空 |
| 过场行 | 3 | 3016 = 362 / 4007 = 391 / 4014 = 393，动作均 **1007** |

**过场触发面（如实记录）**：3 行的 `cs1_haction` 都是 **1007**（真端拒绝流页 `quest_refuse_4`）。
本车道当前的接取/拒绝对话面只服务 `31/26`、`1012/1013`、`1002/20000`、`1003/1004/20001`，
**未实现 1007 拒绝页** ⇒ 该动作不可达 ⇒ 过场保持**休眠**（装载与消费面就绪，触发面待拒绝流落地）。
族门按此语义冻结（「未被服务的动作不得下发过场」），避免用合成页把休眠面伪装成已接线。
同型边界在 P3 talk 车道已存在（talk 67 行过场里有 2 行动作 1007）。

---

## 4. 门态（可复跑）

| 门 | 命令 | 结果 |
|---|---|---|
| 本族族门 + 装载器 + 领奖门 | `mvn -o test -Dtest='SimpleHuntNativeFamilyGateTest,NativeQuestTableLoaderTest,SimpleHuntHandlerTest,NativeQuestRewardClaimGateTest'` | **31/31**（族门 5/5，原 3 + 2） |
| 族门 + tablelane | `mvn -o test '-Dtest=*FamilyGateTest,*RowAlignmentGateTest,*InventoryGateTest,NativeQuestTableLoaderTest,*RewardClaimGateTest,*NativeTalk*Test,NativeNearbyQuestAxisGateTest,SMNearbyQuestsPacketTest'` | **124/124**（原 122 + 2） |
| 聚焦套件 | `mvn -o test '-Dtest=*Quest*Test,*Retail*Test'` | **1699 例 / 162F+142E / 108 类红**，对 P4B 基线 **ADDED 0 / REMOVED 0** |

证据：`gates/2026-10-01-p1b-family-tablane.log`（`*.log` 被 `.gitignore` 忽略 ⇒ 本机复现证据，未入仓）、
`gates/2026-10-01-focused-run-p1b-red-classes.tsv`、`gates/2026-10-01-focused-run-p1b-delta.tsv`（入仓）。

---

## 5. 残余与下一步

1. **拒绝流页 1007**（本批暴露的独立面）：把 `1007` 的拒绝页语义按真端 `cab520`→`mgr+0x1a0` 坐实后接线，
   talk/hunt 共 5 行过场将自动转为可达；在此之前保持休眠并冻结。
2. **P5 残余**：SimpleUseItem 32 行、SimpleItemPlay 9 行 `con_quest` + itemplay 2 行过场（13400/23400，无 `cs1_haction`，落在不路由长尾上）——下一批。
3. 代表任务真实客户端验收 `PENDING_CLIENT`。
