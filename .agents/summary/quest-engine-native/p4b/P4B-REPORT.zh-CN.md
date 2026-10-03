# P4B 报告：collect 族残余轴批（`con_quest` 链式接取窗 + `cutscene` 过场接线）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 主题：SimpleCollectItem（262 行）原生车道**残余两轴**——真端交付节点 `0x1e` 槽（链式接取窗
> `con_quest` 37 行）与 `0x35` 槽（过场 `cutsceneid1`/`cs1_haction` 2 行）——按真端原码坐实后接线，
> 并给出跨族同轴审计（哪几族仍声明未消费）。
> 日期：2026-10-01。分支：`quest`。前置：`p4/P4-REPORT.zh-CN.md`、`p3/P3-STEP4-REPORT.zh-CN.md` §3、
> `p3-prereqs/simple-talk-codegen.md` §1/§2。
> 批门：`SimpleCollectItemNativeFamilyGateTest` **14/14**（+2）、`SimpleCollectItemRowAlignmentGateTest`
> **7/7**（+2）、族门 + tablelane **122/122**、聚焦套件 **1699 / 162F+142E / 108 类红**
> （对 P6.5 基线 **ADDED 0 / REMOVED 0**）。客户端验收 `PENDING_CLIENT`。

---

## 1. 真端证据（原码坐实，非推断）

### 1.1 交付节点 `0x1e` 槽 = 链式接取窗

| 事实 | 证据 |
|---|---|
| collect 行的交付节点注册 `0x1e` 槽 | `ScriptDLL64.c:1016169` `FUN_180cb2ac0(&DAT_1848e4d8c,&DAT_1848e4d90,0x1e,FUN_180d39370,0)` |
| 该节点是 **交付 NPC** 节点 | `ScriptDLL64.c:1003528` `FUN_180cb5920(&DAT_1848e4d90,L"Mires",0x44f)`；collect 行 1103 = acquire `Mires` / reward `Mires` |
| 槽 thunk 语义 = 「打开下一环接取窗」 | `ScriptDLL64.c:2220136` `FUN_180d39370` 体：`mgr+0x1a8(player, 0x450)`；0x450 = **1104** = 行 1103 的 `con_quest` |
| 该调用被 accept 侧分派器共用 | `ScriptDLL64.c:2139262` `FUN_180cab520` 首段对状态 `0/10` 调 `mgr+0x1a8(player, questId)`（同一 API）|
| 列在运行期被逐行读入 | `MainServer_ScriptDLL64/fun/fun_912.cpp:1542`（`con_quest` → 行对象 `+0x2b4`）|

**本车道等价物**：本车道接取路由按 NPC 建表（`acquireNpcByQuestId`），故「下一环的接取 NPC 恰是本行的
交付 NPC」即窗口成立，**不新增第二套路由**（下一环的接取路由永远由它自己那一行提供）。

### 1.2 交付节点 `0x35` 槽 = 过场（PlayMovie）

| 事实 | 证据 |
|---|---|
| collect 行的交付节点注册 `0x35` 槽 | `ScriptDLL64.c:1032914` `FUN_180cb2ac0(&DAT_18494422d,&DAT_184944870,0x35,FUN_180d46c00,0)` |
| 该节点是 **交付 NPC** 节点 | `ScriptDLL64.c:1004128` `FUN_180cb5920(&DAT_184944870,L"Shugo_IDNovice_2",0x4845)`；18501 的 reward = `Shugo_IDNovice_2` |
| thunk = `player+0x1b8(...)`，第一实参来自**页动作事件** | `ScriptDLL64.c:2226943` `FUN_180d46c00`：`(**(code **)(lVar1 + 0x1b8))(param_1, evt[+0x40], 0x4845, evt[+0x48], evt[+0x38], evt[+0x18])`；**movie id 不内联在 codegen 里** |
| 列在运行期被逐行读入 | `fun_912.cpp:1553`（`cutsceneId1`）、`:1564`（`cs1_haction`） |
| movie 456 的身份 | `<真端根>/Map/XML/CutScenes.xml`：`456 = CS_ID_056 / cs_id_056.seq`（ID 新手过场） |

**本车道等价物**：真端把 movie 挂在**页动作**上（thunk 不内联 movie id），故以表列为唯一事实
——`cutsceneid1` = movie、`cs1_haction` = 触发动作——并在**该动作被本行服务时**经 `NativeMoviePort`
下发（与 P3 talk 车道同一包装层形态，见 §3）。触发**时机/节点面**（0x35 挂在交付节点）随本族代表任务
的真实客户端验收收敛，记 `PENDING_CLIENT`。

---

## 2. 落地（实现面）

| # | 文件 | 变更 |
|---|---|---|
| 1 | `questEngine/tablelane/SimpleCollectItemHandler.java` | 新增 `Cutscene(movieId, triggerAction)` 记录 + `conQuestByQuestId` / `unresolvedChainQuestIds` / `cutsceneByQuestId` 三面 + 注入 `NativeMoviePort`；构造期逐行装载 `con_quest`/`cutsceneid1`/`cs1_haction`，并在构造尾按 §1.1 不变量验证**本表内**目标（不闭环即登记 fail-closed）；`onDialog` 拆为**包装层**（`handleDialog` + 命中动作才下发 movie）+ 表声明动作的「被服务但不推进节点」尾段；新增 `conQuest(int)` / `unresolvedChainQuestIds()` / `cutscene(int)` 取数面 |
| 2 | `SimpleCollectItemNativeFamilyGateTest` | +2 例：`chainWindowsAreLoadedAndCloseAtTheHandInNpc`（37 行装载 + 本表内 6 行闭环 + `unresolvedChainQuestIds` 空）、`cutscenePlaysOnTheDeclaredActionWithoutAdvancingTheNode`（注入记录式 `NativeMoviePort`：非触发动作不播、接取页链命中 1012 下发 456、不建任务档、vars 不变） |
| 3 | `SimpleCollectItemRowAlignmentGateTest` | +2 例：`chainAcquireWindowsCloseAtTheRewardNpc`（独立重解析 + 六张兄弟表 + XML-owner 判定 + 分布冻结 + `handler.conQuest` 逐行对拍）、`cutsceneFaceFollowsTheRetailTable`（逐行过场面 = 表列，未声明行不得有过场面） |
| 4 | 证据工具 | `p4b/tools/conquest-axis-audit.py`（跨七族只读复算，§4） |

**不改动的边界**：接取路由（`acquireNpcByQuestId`）、相机、交付门、领奖段与注册/路由分解（owns 262 =
routed 246 + 不可路由 16）**逐字未动**；本批只新增两条消费面与一条包装层。

---

## 3. 逐行对拍（真端全表复算，独立于实现）

`con_quest` 37 行的目标分布（`chainAcquireWindowsCloseAtTheRewardNpc` 冻结 + 工具复算一致）：

| 目标类型 | 行数 | 判定 |
|---|---|---|
| 本表内 | 6 | 全部满足 `target.acquired == source.reward`（窗口已由目标自身那一行实现） |
| 兄弟族表内（Talk/Hunt/ItemPlay/CombineTask/UseItem/SerialHunt） | 24 | 同上不变量，0 例外 |
| 无任何真端表行 | 7 | 其中 3 行 owner 在 XML 车道（2106/11012/21249，由 XML 行负责）、4 行不在本服宇宙 |
| 用物品接取（无接取 NPC 列） | 0 | 本族无此形态 |
| **不闭环** | **0** | `handler.unresolvedChainQuestIds()` 为空 |

`cutscene` 2 行（18501/28501）：movie **456** / 动作 **1012 = `QuestDialogPage.SELECT1_1`**；
客户端契约中 18501 声明页 `4 / 1003 / 1004 / 1011 / 1012(select1_1) / 2375 / 2716` ⇒ 触发动作属**接取页链**，
本车道在该动作被服务时下发 movie（实测：接取侧 `1012` → 页 `1012` 续发 + movie 456；交付面动作集
`31/26/1009` 不含该动作 ⇒ 不误播）。

---

## 4. 跨族同轴审计（共享缺陷面：装载但未消费）

`python3 .agents/summary/quest-engine-native/p4b/tools/conquest-axis-audit.py`（七族全量复算）：

| 家族 | `con_quest` 行 | 本表内 | 兄弟族 | 无行(XML) | 不闭环 | `cutsceneid1` 行 | 接线状态 |
|---|---|---|---|---|---|---|---|
| SimpleTalk | 492 | 308 | 87+2 | 25(70) | 0 | 67 | ✅ P3 已接线 |
| **SimpleCollectItem** | **37** | **6** | **24** | **4(3)** | **0** | **2** | ✅ **本批接线** |
| SimpleHunt | 132 | 65 | 57 | 7(3) | 0 | 3 | ❌ **未接线（P1 车道，新发现）** |
| SimpleUseItem | 32 | 0 | 30 | 2(0) | 0 | 0 | ❌ 未接线（P5 残余） |
| SimpleItemPlay | 9 | 2 | 7 | 0(0) | 0 | 2 | ❌ 未接线（P5 残余；2 行过场在不可路由长尾上）|
| SimpleSerialHunt | 0 | — | — | — | — | 0 | 无该列 |
| CombineTask | 0 | — | — | — | — | 0 | 无该列 |

**审计结论**：同一不变量在**全部七族 702 行**上 0 例外（不存在「目标不在本行交付 NPC 可接」的行），
故接线是纯等价物落地、不需要新增路由；剩余未接线面（Hunt 132+3 / UseItem 32 / ItemPlay 9+2）登记为
计划 §10.3 阻塞项，按族批次落地。

---

## 5. 门态（可复跑）

| 门 | 命令 | 结果 |
|---|---|---|
| 本族族门 | `mvn -o test -Dtest=SimpleCollectItemNativeFamilyGateTest` | **14/14**（原 12 + 2） |
| 本族逐行门 | `mvn -o test -Dtest=SimpleCollectItemRowAlignmentGateTest` | **7/7**（原 5 + 2） |
| 族门 + tablelane | `mvn -o test '-Dtest=*FamilyGateTest,*RowAlignmentGateTest,*InventoryGateTest,NativeQuestTableLoaderTest,*RewardClaimGateTest,*NativeTalk*Test,NativeNearbyQuestAxisGateTest,SMNearbyQuestsPacketTest'` | **122/122**（原 118 + 4） |
| 聚焦套件 | `mvn -o test '-Dtest=*Quest*Test,*Retail*Test'` | **1699 例 / 162F+142E / 108 类红**，对 P6.5 基线 **ADDED 0 / REMOVED 0** |

证据：`gates/2026-10-01-p4b-family-tablane.log`、`gates/2026-10-01-focused-run-p4b.log`（两者被
`.gitignore` 的 `*.log` 忽略 ⇒ 仅作本机复现证据，**未入仓**）、`gates/2026-10-01-focused-run-p4b-red-classes.tsv`、
`gates/2026-10-01-focused-run-p4b-delta.tsv`（后两者入仓）。

失败面：无（本批未新增红类；既有 108 类红全为 §10.3 阻塞 1（QE-112，工作区在飞切片）登记的旧金标）。

---

## 6. 残余与下一步

1. **P1 SimpleHunt**：132 行 `con_quest` + 3 行过场（3016/4007/4014，动作 1007）未消费 —— 新登记阻塞项；
2. **P5 SimpleUseItem / SimpleItemPlay**：32 / 9 行 `con_quest` + itemplay 2 行过场（13400/23400，**无
   `cs1_haction`**）未消费；itemplay 的 2 行落在「不路由长尾」上，接线需与长尾路由同批裁决；
3. **代表任务真实客户端验收**：本族（18501 或其同形行）未跑 ⇒ `PENDING_CLIENT`；过场的触发时机/节点面
   随该验收收敛；
4. typed 车道（未迁移族）的 `con_quest`/过场面不在本批范围（行仍由 XML 车道负责）。
