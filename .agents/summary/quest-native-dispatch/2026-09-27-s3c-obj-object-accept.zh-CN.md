# SimpleTalk S3c-obj · 物件哨兵接取段变体（判例 1323）落地记录


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 车道：`quest-native-dispatch`；面：**SimpleTalk γ 面第五片（S3c-obj：物件接取变体）**。
> 上游：`GOAL.zh-CN.md` §3 的 **W3**（`1323` 物件接取变体）；`2026-09-27-s3b-r-driven-accept.zh-CN.md`
> §6 移交（S3b-β）；`2026-09-27-s3-special-rows.zh-CN.md` §8.2-4（三条备选 + `GIVE_ITEM` 落点裁定）。
> 纪律：未 commit；未新增/退役任何 TSV（链登记表只读，一字节未改）；未启停服务；T3 在仓库外副本、跑完即删。

## 0. 一句话结论

物件哨兵接取者（真端 `acquired_npc_name=LF2_Lost_JewelBox` → 宝箱 730032）的接取段由编译器按**物件梯
合成**：入口 `USE_OBJECT` 自环下发入口页、中转 `ASK_QUEST_ACCEPT` 下发接取窗、提交 `QUEST_ACCEPT_1` 落
`started`，真端 `give_item` 从**可重复的入口自环**移到**提交边**；**客户端三页逐页可达**（入口页 1011 /
接取窗 4 / 拒绝页 1004）。指纹变化面逐行等于该 1 行（**1/0/0**），S2 203 / S3a 60 / S3b 7 / S3c-A 31 /
S3c-D 39 零漂移。

## 1. 两条裁定（各带被否方案与证据）

### 1.1 页梯**保留**（否决"塌缩成 `USE_OBJECT → 页 4`"）

| 方案 | 内容 | 结论 |
|---|---|---|
| **① 保留梯（本片采用）** | 全梯由编译器合成（含入口边）——入口边仍下发入口页 `select1`，`ASK_QUEST_ACCEPT(1007)` 中转仍下发接取窗（页 4） | **采用**——W3 完成判据字面要求"客户端页可达（**入口页**/接取窗/拒绝页三页有路由）"，且 P0c-42 的三页合同（`CLIENT_PAGE_UNREACHED` 1011/4/1004 → 0）正是该物件的历史缺陷面；塌缩会把入口页重新打成未达 |
| ② 塌缩（`canonicalAcceptFlow` 传入口事件 `USE_OBJECT`，直发页 4；= 裁定简报 §③ 的 (a)-②） | 与 NPC 规范形一致（NPC 形确实把 select1 梯随段退场） | **否决**：物件与 NPC 的**对话框驱动方式不同**——物件的对话窗口由服务端下发（`QuestStartItemNpcAi2.handleUseItemFinish`：物件上无任务路由时回落 `SM_DIALOG_WINDOW(objectId, SELECT1)`），客户端 5.8 为该物件 authored 的入口按钮就是 `HACTION_ASK_QUEST_ACCEPT(1007)`（`quest-dialog-action-details.csv`：1007「打开盖子看看。」）⇒ 丢入口页等于丢客户端 authored 的入口动作 |
| ③ 整行留 legacy（= (a)-③） | 不动 | 否决：`give_item` 落在**无门且可重复**的 `unaccepted→unaccepted` 自环上 ⇒ 反复用物件即叠加任务物品、拒接后残留；其余族 give 均在提交边（判例 21136） |
| ✗ (a)-① 半规范（保留 R5 记录 + 只合成其后） | 简报**明令勿取**（"半规范形态会让后续等价性对拍无法归因"） | 未取——本形**不是**半规范：入口边同样由编译器合成、`R5` 整条退场（载荷清空），登记表只作**合同证人**（由 `assertObjectAcceptContract` 逐键绑定）⇒ 单一真源、对拍仍可归因（变化面 = 载荷落位） |
| ✗ (c) 接受 inert 增量（补 `20000`/`20001`） | 简报对 **S3b 行**的裁定（S3b 行已由 `canonicalAcceptFlow` 带上该超集） | 物件形**不**补：客户端对该物件只发 `1002`/`1003`，补边即"发明客户端不会发的按钮"，与本片 fail-closed 判据（键集逐键相等）直接冲突；不补是**更严**的选择（(c) 的理由是"无害"，非"必需"） |

> 与裁定简报的差异已逐条对账：`(b) GIVE_ITEM 移提交边` **照做**（并复用简报点名的现成通道 `acceptGiveItemActions`）；
> `(a)-②/(c)` **未取**（理由如上，均为"更保守 + 判据字面"）；`(a)-③` 未取（修掉重复发物是净收益）。
> NPC 形的 select1 未达是"同形取舍"（S2 §8.3 已登记）；本片**不**把该取舍外推到物件——物件没有可替代的
> 客户端入口，且 W3 判据把入口页列为必达。

### 1.2 `GIVE_ITEM` **落提交边**（真端 `give_item` 的相位）

- 真端来源：`Quest_SimpleTalk.xml` 的 1323 行 `<give_item>ITEM_QUEST_1323A 1</give_item>`；
  `quest_data.xml` 的 `quest_work_items = [{182201309, 1}]` ⇒ 物 = 182201309、数 = 1（两条真端通道互证）。
- 原形（入口自环 `actions=GIVE_ITEM:182201309:1`）的两个副作用：**①可重复叠加**（自环 conditions 为 `-`、
  状态不变 ⇒ 每次使用都发 1 个）；**②拒接残留**（玩家拒接后仍持有任务物品，交付只扣 1）。
- 新形：提交边 `[StartEligible] + [GiveItem(182201309, 1)]`，入口自环载荷为空——
  与 S1 单步形（`acceptGiveItemActions` 落 1002/20000）与 S3b 的 21136 判例同轴。

## 2. 判据（五轴全派生）与普查

```
objectAccept = 非系统发放 ∧ 无 NPC_START 块 ∧ acquired 解析唯一（npc）
             ∧ 该 npc 上有 unaccepted 源、USE_OBJECT 动作、**下发页**的入口记录
             ∧ 同一 npc 上有 unaccepted 源、ASK_QUEST_ACCEPT 动作、下发页的中转记录
             ∧ 同一 npc 上有 unaccepted→started 的 QUEST_ACCEPT* 提交记录
```

全量普查（`.agents/summary/quest-native-dispatch/w3_object_entry_census.py`，只读复算）：
**候选行集 = {1323}**（`entry_page=DIALOG:SHOW_QUEST_PAGE:SELECT1`、relay 1、commit 1，
`acquired=LF2_Lost_JewelBox`、`give=ITEM_QUEST_1323A 1`）。
对照面：`USE_OBJECT` 记录**非** `unaccepted` 源的行 37 行（1156/1158/1394/…/80320，中段交互物推进与
reward 态预览），全部不在面内 ⇒ 切片边界 = 1 行，上限断言锁死（`S3C_OBJECT_ROW_CEILING=1`）。
两形互斥：`objectAccept ∧ rDrivenAccept` 同时成立 → **拒绝编译**（不得静默择一）。

## 3. 合成形与守卫

**合成梯**（`canonicalObjectAcceptFlow`，与 NPC 规范形同构的两处差异均有证据）：

| 边 | 事件 | 目标 | after |
|---|---|---|---|
| 入口 | `TalkToNpc(730032, USE_OBJECT=-1)` | `unaccepted` | `ShowQuestDialog(select1=1011)`（**零载荷**） |
| 中转 | `TalkToNpc(730032, ASK_QUEST_ACCEPT=1007)` | `unaccepted` | `ShowQuestDialog(接取窗=4)` |
| 提交 | `TalkToNpc(730032, QUEST_ACCEPT_1=1002)` | `started` | `Sync(VISIBILITY_REFRESH) + ShowQuestDialog(1003)`；`[StartEligible]` + `[GiveItem(182201309,1)]` |
| 拒绝 | `TalkToNpc(730032, QUEST_REFUSE_1=1003)` | `unaccepted` | `ShowQuestDialog(拒绝页=1004)` |
| 关窗 | `TalkToNpc(730032, FINISH_DIALOG=1008)` × {`unaccepted`, `started`} | 同源 | `CloseDialog`（**不套** NPC 形的任务列表页 10：物件没有 NPC 的任务选择列表，登记记录同形） |

不加客户端不会发出的 inert 边（该物件的 ask 页只有 1002/1003 ⇒ 不合成 `QUEST_ACCEPT_SIMPLE` 与拒绝族
其余两项——与 NPC 形的超集策略**有意**不同）。

**守卫（fail-closed）**

| 守卫 | 判据 | 触发后果 |
|---|---|---|
| 合同逐键相等 `assertObjectAcceptContract` | 合成梯的键集（`源节点:动作`）必须与该物件在登记表里的**整条对话面**逐键相等；入口页/中转页必须恰为 `SELECT1`/`SHOW_ASK_QUEST_ACCEPT_WINDOW` | 多一条 = 发明客户端不会发的按钮、少一条 = 静默丢出口 ⇒ **拒绝编译**（客户端合同变更交裁定） |
| 层 A 对偶 | 入口（`-1`）与中转（`1007`）键必须随段退场，否则 `explicitRoutes` 覆盖会**反向删除**合成的入口/中转边 | 形状静默回退旧形而指纹零漂移（判例 80752 同类） |
| 载荷覆盖 `assertAcceptPayloadCovered`（既有） | 退场记录的 `GIVE_ITEM`/`REMOVE_ITEM` 必须被规范接取边覆盖 | 静默丢物 ⇒ 拒绝编译 |
| 退场静默守卫 `assertRetiredChainRouteQuiet`（既有） | 退场记录的条件/动作/after 只允许白名单 token | 未知载荷 ⇒ 拒绝编译 |
| 测试侧独立重算（S3c-obj 臂） | 键集相等 + 三页各**恰一条**下发 + 入口零载荷 + 提交边载荷 == 真端 `give_item` + 提交边 `StartEligible` + 落 `started` + 不得并立 NPC 形入口（31） | 任一不成立即红 |

> 物件行的接取段**不套** `assertCanonicalAcceptResidueFree`（该守卫禁止 SELECT1 族页下发与 1007/1011
> 动作——正是本变体要保留的梯）；其"零残留"判据换成**合同逐键相等**（更强：键集相等 ⇒ 无未声明的物件边）。

### 3.1 守卫自检（fail-closed 负例，在**仓库外副本**里做，主树与登记表未被触碰）

| 负例（副本内改登记表） | 期望 | 实测 |
|---|---|---|
| **A** 入口页 `SELECT1` → `SELECT1_1`（客户端合同变更） | 拒绝编译 | `object accept ladder page contract broken: quest 1323 entryPages=1 relayPages=1 declaredEntry=[1012] declaredRelay=[4]` ✅ |
| **C** 追加一条未声明的物件对话记录（`SELECT1_1 unaccepted`） | 拒绝编译 | `object accept ladder key drift: quest 1323 emitted=[…6 键…] declared=[…7 键…]` ✅ |
| **B** 删除中转记录（谓词收缩 ⇒ 行退出变体） | 覆盖面下限红 | `物件接取变体覆盖面回退：0 < 1 行集=[]` ✅（不静默回 legacy） |

> 副本登记表自检后按备份复原（`md5 0ae2d924…` 与主树一致），副本整体跑完即删。

## 3.2 异径独立复核（Python 直读数据文件，不经 Java 守卫）

`.agents/summary/quest-native-dispatch/w3_object_accept_independent_check.py`（只读脚本）用**另一条工具路径**
复算四条判据，全部成立（`INDEPENDENT_CHECK PASS`）：

| 判据 | 复算结果 |
|---|---|
| 物件 730032 的对话面键集 == 编译期合同键集 | `{started:1008, unaccepted:-1, unaccepted:1002, unaccepted:1003, unaccepted:1007, unaccepted:1008}` **相等** |
| 入口记录页下发 == `SELECT1`、中转记录页下发 == `SHOW_ASK_QUEST_ACCEPT_WINDOW` | 均为**单页且相符** |
| 入口自环载荷 == 真端 `give_item`（`ITEM_QUEST_1323A 1` → `182201309`×1） | `GIVE_ITEM:182201309:1` **相等** |
| 提交记录恰一条且条件 `START_ELIGIBLE`、落 `started` | **成立** |

## 4. 变化面 == 缺陷面

| 判据 | 结果 |
|---|---|
| 指纹重冻（`-Dretail.talkChain.fingerprintOut`） | **1 / 0 / 0**（ADDED 0 / REMOVED 0），漂移集逐行 == `{1323}` == 本片行集 |
| 行集身份 | 冻结表 285 行 id 集合在重冻前后**逐行相同**（`diff` 空） |
| S2 203 / S3a 60 / S3b 7 / S3c-A 31 / S3c-D 39 | **零漂移** |
| 缺陷面 | 载荷落位：入口自环（可重复）→ 提交边；页梯形状**逐字节不变**（有意，见 §1.1） |

> 说明：本片的 IR 变化恰为**载荷落位**（入口边动作清空、提交边动作出现），页梯本身不变——这不是
> "没改动"，而是"改对了地方"：把 `give_item` 从可重复的自环搬到一次性提交边上。

## 5. 门禁

| 门 | 结果 |
|---|---|
| `RetailSimpleTalkChainGateTest`（链门，**新增 S3c-obj 臂**） | **8/8 绿** |
| `RetailSimpleTalkGateTest`（家族门） | **4/4 绿** |
| 聚焦（1323 相关） | `QuestStartItemDefinitionRegressionTest`（1297… 首条在册红）、`LegacyTemplateMirrorRouteRegressionTest`（1131 在册红）、`QuestItemSourceContractGateTest`（4966 在册红）三条**均为基线在册红**（见 §6 红集恒等）；`MissionItemConsumptionBatchRegressionTest` 4/4、`ReportToManySetSucceedAlignmentTest` 3/3 绿 |
| **守卫自检（fail-closed 负例，仓库外副本）** | 三例全部被拦（详见 §3.1） |
| **T2**（树内，285 选择器） | 见 §6 |
| **T3**（仓库外全树副本，跑完即删） | 见 §6 |

## 6. 门禁证据

| 门 | 结果 | 证据 |
|---|---|---|
| **T2**（树内，285 选择器） | **ADDED 0 / REMOVED 0**（597 测试 / 35F+16E） | `gates/T2-195358.log` + `gates/T2-s3cobj-{reds,added,removed}.txt` |
| ↳ 哈希级加强证据 | T2 红身份集 sha256 `57bb0621b2aa90f861e41358f63baa5450e73753440160ba276bb0255df721fd`（51 条）与 W1/W2 基线**逐字节相同** ⇒ 集合恒等（强于差集为空） | `gates/T2-s3cd-reds.txt` vs `gates/T2-s3cobj-reds.txt` |
| **T3**（仓库外全树副本，跑完即删） | **ADDED 0 / REMOVED 0**（2022 测试 / 109F+23E） | `gates/T3-195403.log` + `gates/T3-s3cobj-{reds,added,removed}.txt` |
| ↳ 哈希级加强证据 | T3 红身份集 sha256 `ce4673c74ed61fdcff494fedcc1b8e98c78b252d54b150a05c4bdaa937197870`（97 条）与 W1/W2 基线**逐字节相同**（集合恒等）；副本内链门同样 **8/8 绿** | `gates/T3-s3cd-reds.txt` vs `gates/T3-s3cobj-reds.txt` |
| ↳ 守卫加固前后两次独立跑 | 加固前 `T2-193512`（597/35F+16E）、`T3-194213`（2022/109F+23E）与加固后 `T2-195358`、`T3-195403` 四份红集**同一 sha256**；加固不改变任何已接受行的 IR（重冻逐字节相同 ⇒ 形状中性） | 四份日志 |

> 红集中无任何条目涉及 1323：`QuestStartItemDefinitionRegressionTest` 的首条命中在 1197（在册），
> 1323 的 `unaccepted + USE_OBJECT` 入口合同由新增 S3c-obj 臂逐条验证（入口边存在且载荷为空）。

## 6.1 W4 全片复验（SimpleTalk γ 面收口验证）

| 门 | 结果 | 证据 |
|---|---|---|
| 链门 `RetailSimpleTalkChainGateTest`（全 285 行 + 指纹表） | **8/8 绿** | `gates`（本片四次跑均绿，含副本内一次） |
| 家族门 `RetailSimpleTalkGateTest` | **4/4 绿** | 同上 |
| 契约门 `RetailQuestContractTest`（黑盒四相位） | **1/1 绿** | 本片终态跑 |
| **T1**（固定门禁套件） | **ADDED 0 / REMOVED 0**（唯一红 = 在册 `RetailDataDrivenGateTest.driftVersusShellsIsRegistered`，属并行车道） | `gates/T1-200912.log` + `gates/T1-s3cobj-{reds,added,removed}.txt` |
| **T2 / T3** | 见 §6 | — |

## 7. 纪律回执

未 commit/push；未启停服务；未新增/退役任何 TSV（`quest_client_talk_chain_steps.tsv` 只读，一字节未改；
`quest-retail-tsv-manifest.tsv` 与 `EXPECTED_TSV_COUNT` 未动）；未创建 worktree；T3 在仓库外全树副本
（`/private/tmp/aion-t3-s3cobj`）跑、跑完即 `rm -rf` 并确认无残留；守卫负例同样只在副本内改文件（自检后
按备份复原、md5 与主树一致）；Maven 仅用于聚焦测试与 T2/T3 门禁；树内与副本并行时树内只有一个 Maven；
AI 中间产物（普查脚本）落 `.agents/summary/quest-native-dispatch/`。

**记忆库**：新增 **QE-093**（物件入口的接取梯是客户端合同 / 发物只落一次性边 / 键集逐键相等 + 页 id 互证）
并补 `systemPatterns.md` 路由；`MEMORY_BANK_SYNC_OK ENTRIES=138`、`MEMORY_BANK_VERIFY_OK STEPS=3`。
**真端依据（本片全程）**：真端 `Quest_SimpleTalk.xml` 的 `give_item` + `quest_data.xml` 的 `quest_work_items`
（互证 182201309×1）+ 客户端对话合同（`ACT:1323` 的 1011/4/1004 三页与 1002/1003 按钮）——形状与载荷落点
全部由真端/客户端证据决定，未回填旧 XML 形。
