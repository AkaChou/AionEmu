# P5B 报告：SimpleUseItem / SimpleItemPlay 残余轴批（`con_quest` 链式接取窗 + itemplay 过场休眠面）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 主题：P5 两族（`Quest_SimpleUseItem.xml` **160 行** / `Quest_SimpleItemPlay.xml` **43 行**）的
> **残余两轴**——真端交付节点 `0x1e` 槽（`con_quest`：UseItem 32 行 / ItemPlay 9 行）与 `0x35` 槽
> （itemplay 2 行 `cutsceneid1`）——按 P4B/P1B 同一不变量接线，并把**触发面在真端表里不存在**的过场
> 如实冻结为休眠面。
> 日期：2026-10-01。分支：`quest`。前置：`p4b/P4B-REPORT.zh-CN.md`（同轴形态 + 跨族审计工具）、
> `p1b/P1B-REPORT.zh-CN.md`（同轴 last-mile）、`p5/P5-REPORT.zh-CN.md`（本族切换批）。
> 批门：族门 28/28、族门 + tablelane **125/125**（P1B 基线 124 → +1）、聚焦套件
> **1699 / 162F+142E / 108 类**（对 P1B 基线 **ADDED 0 / REMOVED 0**）。客户端验收 `PENDING_CLIENT`。

---

## 1. 真端证据（原码坐实，非推断）

### 1.1 交付节点 `0x1e` 槽 = 链式接取窗（**本族第一次逐节点坐实**）

P4B/P1B 的 `0x1e` 证据取自 collect 行（`ScriptDLL64.c:1016169`，节点 `Mires`/quest 1103）。
本批把同一形态**在 P5 两族自己的交付节点上复算**，证据链闭合到行：

| 家族 | 行 | 交付节点注册 | `0x1e` 槽注册 | thunk 内联值 |
|---|---|---|---|---|
| UseItem | 18833（reward `Gadorus`） | `:1857125` `FUN_180cb5920(&DAT_185f0f500,L"Gadorus",0x4991)` | `:1859456` | `FUN_180eb5430`：`mgr+0x1a8(player, 0x4992)` = **18834** = 本行 `con_quest` |
| UseItem | 18835 | `:1857137`（`0x4993` = 18835） | `:1859467` | `FUN_180eb5460`：`0x4994` = **18836** = 本行 `con_quest` |
| UseItem | 18837 | `:1857149`（`0x4995` = 18837） | `:1859478` | `FUN_180eb5490`：`0x4996` = **18838** = 本行 `con_quest` |
| ItemPlay | 13400（reward `LDF5b_Watering_E`） | `:1397693` `FUN_180cb5920(&DAT_1856f24a0,L"LDF5b_Watering_E",0x3458)` | `:1398877` | `FUN_180dba720`：`mgr+0x1a8(player, 0x3459)` = **13401** = 本行 `con_quest` |
| ItemPlay | 23400 | `:1397717`（`0x5b68` = 23400） | `:1398888` | `FUN_180dba750`：`0x5b69` = **23401** = 本行 `con_quest` |
| ItemPlay | 13401 | `:1397645`（`0x3459` = 13401） | `:1398866` | `FUN_180dba6f0`：内联 13402 = 本行 `con_quest` |
| ItemPlay | 23401 | `:1397453`（`0x5b69` = 23401） | `:1398855` | `FUN_180dba6c0`：内联 23402 = 本行 `con_quest` |

要点（与 P4B 完全同形，非类比）：

1. 节点注册第三实参 = **quest id**（`0x4991` = 18833 = 该行的 reward NPC 行 id），故节点身份 = (NPC, questId)。
2. `0x1e` thunk 走 `mgr+0x1a8`，第二实参**内联本行的 `con_quest` 值**（18834 / 13401 …），不是 NPC 句柄 ——
   本批一度按「`0x4992` 是句柄」误读，按十进制还原后与表列逐行相等（18834 = UseItem 18833 的 `con_quest`）。
3. 与本车道等价物：接取路由按 NPC 建表 ⇒ 「下一环的接取 NPC = 本行的交付 NPC」即窗口成立，**不新增第二套路由**
   （`SimpleUseItemHandler` 只有证据面 `conQuest(int)`，`SimpleItemPlayHandler` 另有 fail-closed 面）。

### 1.2 交付节点 `0x35` 槽 = 过场（PlayMovie）——**P5 两族同样注册了槽，但触发面不同**

| 家族 | 行 | 节点 | `0x35` 槽注册 | thunk 形态 |
|---|---|---|---|---|
| UseItem | 18833 | `DAT_185f0f500` | `:1866953` `FUN_180ec01a0` | `player+0x1b8(player, evt[+0x40], 0x4991/*本行 id*/, evt[+0x48], evt[+0x38], evt[+0x18])` |
| ItemPlay | 13400 | `DAT_1856f24a0` | `:1401613` `FUN_180dbef10` | `player+0x1b8(..., evt[+0x40], 0x3458/*本行 id*/, ...)` |
| ItemPlay | 23400 | `DAT_1856f43f0` | `:1401624` `FUN_180dbefa0` | 同上，第三实参 `0x5b68` = 23400 |

三点结论（**与 P4B 的差别是本批的关键事实**）：

1. 与 collect 的 `FUN_180d46c00`（`ScriptDLL64.c:2226943`）**同一形态**：movie id 由**页动作事件**携带
   （`evt[+0x40]`），不在 codegen 里内联 ⇒ 表列 `cutsceneid1` / `cs1_haction` 是唯一数据事实。
2. `0x35` 槽在**未声明过场的行上也存在**（UseItem 18833 无 `cutsceneid1` 却注册了 `0x35`）⇒
   槽注册是节点模板的通用行为，**不能把「槽存在」当作「该行会播过场」**——反之亦然。
3. **UseItem 表 160 行 `cutsceneid1` / `cs1_haction` 全 0 命中**；**ItemPlay 表 `cs1_haction` 全表 0 命中**
   （对比 `Quest_SimpleCollectItem.xml` 2 行带动作列）⇒ itemplay 的 2 行过场（13400=859、23400=860）
   **声明了 movie、但触发动作列在真端表里不存在**，与「动作未被服务」（P1B 的 1007 情形）是**两种不同的缺口**：
   - P1B/1007：动作列存在（1007），但本车道未实现该页 ⇒ 动作不可达；
   - 本批 itemplay：动作列**根本不存在** ⇒ 无触发面可依，禁止用合成页补。
   两者共同处置 = 按「未被服务的动作不播」冻结为**休眠面**，只留证据、不上线。

### 1.3 运行期列读入（与 collect/hunt 同一解析器）

`<真端根>/server58-source/MainServer_ScriptDLL64/fun/fun_912.cpp`：
`con_quest` `:1542`（→ 行对象 `+0x2b4`）、`cutsceneId1` `:1553`（`+0x2b8`）、`cs1_haction` `:1564`（`+0x2bc`）、
`item_check` `:1531`。该函数按列名（`_wcsicmp`）从行键值表取列，是 **simple-\* 家族共用的行解析器**，
故「列被读入行对象」这一事实对 UseItem/ItemPlay 同样成立。

---

## 2. 落地（实现面）

| # | 文件 | 变更 |
|---|---|---|
| 1 | `SimpleUseItemHandler` | 新增 `conQuestByQuestId` + `conQuest(int)`；构造循环按原文装载 `row.conQuest()`；注释写明**本族无接取 NPC 面**（接取 = 用物品）⇒ 闭环判据归逐行门跨族复算，handler 内不判定、不新增路由、不静默放行 |
| 2 | `SimpleItemPlayHandler` | 新增 `conQuestByQuestId`/`unresolvedChainQuestIds` + `conQuest(int)`/`unresolvedChainQuestIds()`；构造循环装载 + 构造尾闭环验证（本表内目标按「下一环接取 NPC = 本行交付 NPC」登记 fail-closed）；注释按 §1.2 记录 itemplay 过场的**触发面缺失**（表无 `cs1_haction` 列）与休眠处置 |
| 3 | `UseItemFamilyRowAlignmentGateTest` | 新增 `chainAcquireWindowsCloseAtTheRewardNpcForBothFamilies` + `assertChainFace(...)`：两族逐行对拍（表列 vs `handler.conQuest`）+ 目标分布冻结（UseItem 0/30/2、ItemPlay 2/7/0）+ 目标接取 NPC 必须等于本行交付 NPC + `unresolvedChainQuestIds` 空 |

**未改动**：两族的接取/中继/步内发扣/交付门（`item_check`）/领奖段、注册与路由分解、itemplay 37 行不路由长尾
（帧外声明行仍不上线）。**未新增**：itemplay 过场的合成触发页、任何 questId 特例。

---

## 3. 逐行复算（独立于实现，工具 `p4b/tools/conquest-axis-audit.py` 七族口径）

| 指标 | SimpleUseItem | SimpleItemPlay | 判定 |
|---|---|---|---|
| 表行 | 160 | 43 | 与真端表一致 |
| `con_quest` 行 | **32** | **9** | 与真端表一致（装载面逐行对拍 0 漂移） |
| 本表内目标 | 0 | **2**（13400→13401、23400→23401） | 本表内目标逐行闭环（`SimpleItemPlayHandler` 构造期验证，残余集空） |
| 兄弟族目标 | **30** | **7** | 目标接取 NPC = 本行交付 NPC，**0 例外** |
| 无表行目标 | **2**（30505→30506、30555→30556，reward `Oreitia`） | 0 | 无行可判 ⇒ 按真端缺声明冻结（不合成） |
| 不闭环 | **0** | **0** | — |
| 过场行 | 0（表列全缺） | **2**（859/860），`cs1_haction` **全表缺列** | 休眠面（§1.2-3） |

闭环判据示例（跨族，最典型的三行链）：
UseItem `18833`(reward `Gadorus`, `con_quest=18834`) → SimpleTalk `18834`(`acquired_npc_name=Gadorus`) ✓；
同形 `18835→18836`、`18837→18838`（Aridella/28833-28838 同构）。ItemPlay `13400`(reward `LDF5b_Watering_E`)
→ 本表 `13401`(`acquired_npc_name=LDF5b_Watering_E`) ✓。

---

## 4. 门态（可复跑）

| 门 | 命令 | 结果 |
|---|---|---|
| 本族族门 + 逐行门 + 行集门 | `mvn -o test -Dtest='UseItemFamilyRowAlignmentGateTest,SimpleUseItemNativeFamilyGateTest,SimpleItemPlayNativeFamilyGateTest,UseItemFamilyRowInventoryGateTest'` | **28/28**（族门 11/9、逐行门 7/7、行集门 1/1） |
| 族门 + tablelane | `mvn -o test '-Dtest=*FamilyGateTest,*RowAlignmentGateTest,*InventoryGateTest,NativeQuestTableLoaderTest,*RewardClaimGateTest,*NativeTalk*Test,NativeNearbyQuestAxisGateTest,SMNearbyQuestsPacketTest'` | **125/125**（P1B 基线 124 → +1） |
| 聚焦套件 | `mvn -o test '-Dtest=*Quest*Test,*Retail*Test'` | **1699 例 / 162F+142E / 108 类红**，对 P1B 基线 **ADDED 0 / REMOVED 0**（逐类三元组差集 0 变化） |

证据：`gates/2026-10-01-focused-run-p5b.log`（`*.log` 被 `.gitignore` 忽略 ⇒ 本机复现证据，未入仓）、
`gates/2026-10-01-focused-run-p5b-red-classes.tsv`、`gates/2026-10-01-focused-run-p5b-delta.tsv`（入仓）、
`p5b/tools/native-node-slot-probe.py`（本报告 §1 的节点槽/行号复算工具）。

---

## 5. 残余与下一步

1. **itemplay 过场触发面缺失**（本批新暴露、与 #20 不同型）：真端表无 `cs1_haction` 列，无动作可依 ⇒
   保持休眠；若后续能从真端页动作数据（非表）坐实触发时机，再单独立批（禁止用合成页补）。
2. **itemplay 37 行长尾**（声明中继/步物品/过场/交付门 ⇒ `resolvable=false`）与 **UseItem 2 行无表行目标**
   （30506/30556）仍挂账，见计划 §10.3-#16。
3. **拒绝流页 1007**（talk 2 行 + hunt 3 行过场休眠）见 §10.3-#20，与本批同族但不同缺口。
4. 代表任务真实客户端验收 `PENDING_CLIENT`（两族各需一条）。
