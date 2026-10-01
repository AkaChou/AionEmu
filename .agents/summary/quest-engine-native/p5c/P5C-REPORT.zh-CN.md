# P5C 报告：接取入口页按真端重锚（信页/阶段页）+ 页动作 `1007` 的 ask 流接线

> 主题：跨族（已切换七族的 NPC 接取面）**接取页链**——真端入口选择器只发信页/阶段页
> （`select_none`(4762) / `select1..select14` / `default_success`(10002)，**从不发接取窗页 4**），
> 页 4 只能由页动作 `1007`（`ASK_QUEST_ACCEPT` → `mgr+0x1a0`）打开。本批把车道的入口页从
> 「页 4 优先」改回真端形态，并接线 `1007`；计划 §10.3-#20 由此闭环（5 行过场从休眠转为可达）。
> 日期：2026-10-01。分支：`quest`。前置：`p1b/P1B-REPORT.zh-CN.md`（过场休眠面）、
> `p1/pageflow-spike.md`（页 4 只经 1007）、`.agents/summary/quest-80787-event-npc-missing/`（页 4 直发 load fail）。
> 批门：新门 `NativeAcceptEntryAskFlowGateTest` 4/4、族门 + tablelane **129/129**（P5B 基线 125 → +4）、
> 聚焦套件 **1699 / 162F+142E / 108 类**（对 P5B 基线 **ADDED 0 / REMOVED 0**）。客户端验收 `PENDING_CLIENT`。

---

## 1. 真端证据（原码坐实）

### 1.1 入口页 = 信页/阶段页表（**从不发页 4**）

`<真端根>/server58-source/MainServer_ScriptDLL64/fun/fun_731.cpp`：

| 事实 | 证据 |
|---|---|
| 入口选择器只对未接态（状态 `0`/`10`）出手 | `:4155` `if ((*param_3 != 0) && (*param_3 != 10)) return 0;` |
| 形态 0 → **`select_none`(0x129a = 4762)** | `:4158-4161`（调用方 `FUN_180caf640` 固定传入 0） |
| 形态 3 → **阶段页表**（阶段 i → 页） | `:4162-4211` + 调用方 `FUN_180caf740`；表 = `0x3f3`(1011 select1) / `0x548`(1352 select2) / `0x69d`(1693 select3) / `0x7f2`(2034) / `0x947`(2375) / `0xa9c`(2716) / `0xbf1`(3057) / `0xd46`(3398) / `0xe9b`(3739) / `0xff0`(4080) / `0x1964`(6500) / `0x1ab9`(6841) / `0x1c0e`(7182) / `0x1d63`(7523) / `0x1eb8`(7864) |
| 形态 4 → **`default_success`(0x2712 = 10002)** | `:4214-4217` + 调用方 `FUN_180caf6c0` |
| 选中页经 `mgr+0x188(player, page, questId)` 下发 | `:4219` |

**结论**：真端入口页表里**没有任何分支返回页 4**；页 4（`ask_quest_accept`）只能由页动作 `1007` 打开。

### 1.2 页动作 `1007` = `ASK_QUEST_ACCEPT` → `mgr+0x1a0`

| 事实 | 证据（`server58/MainServer_ScriptDLL64/ScriptDLL64.c`） |
|---|---|
| 派发器：客户端页 id `0x3ef`(1007) → `mgr+0x1a0(player, questId)` | `:2071005-06`（`FUN_180c47220`）、`:2139091`（`FUN_180cab520`）、`:2139155`、`:2139224`、`:2139301`、`:2141698`、`:2141998` |
| 单行派发器同形（入口先发信页、再等 1007） | `FUN_180f18b80`（quest 1371）：`:2474921` 未接态发 **1011**；`:2474924-25` 客户端发 1007 → `mgr+0x1a0` |
| 同上（quest 3072 家族） | `FUN_180f287a0`：`:2483492` 未接态发入口页；`:2483521-22` 1007 → `mgr+0x1a0` |
| 0x1e 链窗与入口是**两个不同 API** | `mgr+0x1a8` = 打开下一环**入口对话**（P4B 已坐实：0x1e 槽）；`mgr+0x1a0` = 本批的 **ask 流** |

**交叉证据（本仓既有）**：`p1/pageflow-spike.md` §A1 记「SRV58 派发器 QUEST_SELECT 态发 4762/阶段页表，
**页 4 只经 1007 打开**（fun_731.cpp:4146 起）」；`quest-80787-event-npc-missing` 记「页 4 在客户端只能由
1007 那条链到达，直接下发即 load fail」；客户端派生枚举 `QuestDialogAction` 中 `1007 = ASK_QUEST_ACCEPT`
（`QuestStartAction` 亦用它表示「弹接取窗」）。

### 1.3 迁移前 XML 见证（同源，可复算）

`git show 4ede058c0^:.../quest_definition/quests/<id>.xml` 的 `NPC_START` 边：

| 行 | 迁移前 `start-page` | 本批新规则 |
|---|---|---|
| 3016 / 4007 / 4014 / 1371（声明 `4 + 1011`） | `SELECT1` | 1011 ✓ |
| 1430 / 1464 / 1472 / 1482 / 1604 / 1626 / 1634（同时声明 `4762 + 1011`） | `SELECT_NONE` | 4762 ✓ |

---

## 2. 落地（实现面）

| # | 文件 | 变更 |
|---|---|---|
| 1 | `QuestDialogContract` | 新增真端偏好序 `RETAIL_ENTRY_PAGE_PREFERENCE = select_none(4762) → select1(1011) → 页 4 兜底` + `retailEntryPage(int)`；新增 `askWindowPage(int)`（客户端声明页 4 才返回 4，否则 `-1` fail-closed）。旧 `acceptEntryPage`（页 4 优先）保留给**尚未迁移的 typed 车道**编译产物（P8 随该车道退场），doc 写明两者差异与理由 |
| 2 | `SimpleTalkHandler` / `SimpleHuntHandler` / `SimpleSerialHuntHandler` / `SimpleCollectItemHandler` / `SimpleItemPlayHandler` / `SimpleCombineTaskHandler` | 入口边（26/31/-1）改用 `retailEntryPage`；新增 `dialogId == ASK_QUEST_ACCEPT(1007)` 分支：下发 `askWindowPage`，未声明即 `return false`（无包） |
| 3 | `SimpleUseItemHandler` | **不改**：本族接取 = 用物品，物品路径已是 ask 语义（用道具 → 页 4），无 NPC 接取面可加 |
| 4 | `NativeTalkFixture` | `clientEntryPage` 改走 `retailEntryPage`（车道口径），新增 `askWindowPage` 访问器 |
| 5 | 新增门 `NativeAcceptEntryAskFlowGateTest` | ① 9105 行客户端契约全量复算入口页规则（且不得落在未声明页上）；② 样本冻结（3016→1011、1430→4762、5000→页 4 兜底、1007 目标页）；③ **5 行过场可达性**（`onDialog(1007)` 被服务 + 只下发页 4）；④ 未声明页 4 的行 `1007` fail-closed（无包） |
| 6 | 重锚旧断言 | `SimpleCollectItemNativeFamilyGateTest`（1137 入口 4 → 1011 + 新增 1007 例）、`SimpleHuntNativeFamilyGateTest`（1007 由「不服务」改为「服务 + 下发 movie 362」）、`QuestEventQuestBatchDefinitionTest`（80028/80031/80032 入口页 + 新增 1007 例）、`QuestMultistepChainContractTest`（10 行入口页按信页偏好序独立复算） |

**未改动**：各族接取提交（1002/20000）、拒绝族（1003 → 页 1004、1004/20001 关窗）、
`select1` 续页（1012/1013）、报告/交付/领奖段、typed 车道的页 4 优先规则。

---

## 3. 结果：计划 §10.3-#20 闭环（5 行过场从休眠转为可达）

| 行 | 家族 | movie | 客户端声明 | 入口页 | 1007 目标 |
|---|---|---|---|---|---|
| 3016 | SimpleHunt | 362 | `4 + 1011 + 1352` | 1011 | 页 4 |
| 4007 | SimpleHunt | 391 | `4 + 1011 + 1352` | 1011 | 页 4 |
| 4014 | SimpleHunt | 393 | `4 + 1011 + 1012 + 1352` | 1011 | 页 4 |
| 3020 | SimpleTalk | 363 | `4 + 1011 + …` | 1011 | 页 4 |
| 4056 | SimpleTalk | 403 | `4 + 1011 + …` | 1011 | 页 4 |

流程（真端形）：点接取 NPC（26/31）→ **信页 1011** → 客户端「接取」按钮 = 页动作 **1007** →
本车道下发**接取窗页 4** 并下发该行 movie（0x35 槽语义）→ 1002/20000 建档 → 页 1003 …

---

## 4. 门态（可复跑）

| 门 | 命令 | 结果 |
|---|---|---|
| 新跨族门 | `mvn -o test -Dtest='NativeAcceptEntryAskFlowGateTest'` | **4/4** |
| 重锚类 | `mvn -o test -Dtest='QuestEventQuestBatchDefinitionTest,QuestMultistepChainContractTest,NativeAcceptEntryAskFlowGateTest'` | **18/18** |
| 族门 + tablelane | `mvn -o test '-Dtest=*FamilyGateTest,*RowAlignmentGateTest,*InventoryGateTest,NativeQuestTableLoaderTest,*RewardClaimGateTest,*NativeTalk*Test,NativeNearbyQuestAxisGateTest,SMNearbyQuestsPacketTest,NativeAcceptEntryAskFlowGateTest'` | **129/129**（P5B 基线 125 → +4） |
| 聚焦套件 | `mvn -o test '-Dtest=*Quest*Test,*Retail*Test'` | **1699 例 / 162F+142E / 108 类红**，对 P5B 基线 **ADDED 0 / REMOVED 0** |

证据：`gates/2026-10-01-p5c-family-tablane.log`（`*.log` 被 `.gitignore` 忽略 ⇒ 本机复现证据，未入仓）、
`gates/2026-10-01-focused-run-p5c-red-classes.tsv`、`gates/2026-10-01-focused-run-p5c-delta.tsv`（入仓）。

---

## 5. 残余与下一步

1. **typed 车道的入口页仍是页 4 优先**（`QuestDialogContract.acceptEntryPage` + `RetailClientAcceptEntryPage`）：
   该规则服务于尚未迁移的 XML-only / DD 编译产物，随 P8 车道删除；若要提前对齐，需同时给 compiled 形补
   `1007 → 页 4` 边（不得只改入口页，否则信页无出口）。与 QE-112 在飞切片相关 ⇒ 冻结到该切片落地后。
2. **玩家可见变更**：入口从「页 4 直接开窗」变为「信页 → 1007 → 页 4」，代表任务真实客户端验收
   `PENDING_CLIENT`（本批必须优先复测 3016/4007/4014/3020/4056 与 1137/80028 家族）。
3. `select_none`(4762) 行的接取按钮是 20000/20001（已服务）；若客户端某行信页按钮另有形态，按同一
   「未声明页 fail-closed」口径单独取证。
