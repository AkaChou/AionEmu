# 表车道 native 掉落注册补面 + 采集物件使用面：10034「锯齿形状的刀」与 15011 右击 702730（2026-10-08）

> 主题：两笔同日实机缺陷。**① 掉落供源面**：DD 车道（163 行）与 SimpleUseItem 车道（2435）未从真端
> `drop_*` 列注册任务掉落——10034 击杀 216494 不掉「锯齿形状的刀」（已实机验证修复：用户确认掉落
> 恢复正常）。**② 采集物件使用面**：15011 右击 702730 仍零响应——真端 DD 表 CollectItem 步的**追加
> FOBJ 列（value1..4）从未被消费**，物件的 ACTION_ITEM_USE 资格（can-act）与交互认领双缺，交互 AI
> 在 can-act 门早退（零包，进度条都不出现）；掉落注册是必要但**不充分**条件。两笔合收 memory-bank
> `QE-139` 悬案 ⑤⑥。主题目录 = `.agents/summary/quest-native-drops-registration/`。

## 1. 现象与根因

- **现象**：10034（ELYOS，Lv53，英吉斯温链；本批前链式接取面已修复，玩家已能接到）在收集步击杀
  216494（`LF4_B6_FanaticAs_NamedQ_53_An`，实刷于 `spawns/Npcs/210050000_Inggison.xml:5361`）
  不掉 `quest_10034a`（item **182215627**，"Jagged Sword"）⇒ 任务卡死。
- **掉落声明**（真端 `quest/retail/quest.xml` 10034 行）：`drop_monster_1=LF4_B6_FanaticAs_NamedQ_53_An`
  / `drop_item_1=quest_10034a` / `drop_prob_1=100` / `drop_each_member_1=1` / `collect_progress=3`；
  退役 XML（`git c947f0373^:…/quest_definition/quests/10034.xml`）曾带 `<drops><drop npc-id="216494"
  item-id="182215627" chance="100" each-member="true" collecting-step="3"/></drops>`。
- **断供链**：运行期查询 `QuestService.getQuestDrop`（`:1526`）= `QuestEngine.questDrops`（`:231-249`，
  目录 ∪ 采集 ∪ Talk）∪ handler 侧表；**DD 运行时从不注册掉落**（全文零 drop 触点），UseItem 族同缺
  ⇒ 击杀装配（`DropRegistrationService:136`）查不到该任务 ⇒ 0%。消费面共两处：击杀装配 +
  `QuestItemNpcAI2:93`（对象交互）；`getEachDropMembers*` 内部同走该查询（each-member 自动继承）。
- **15011 第一轮诊断（不完整，同日被实机推翻）**：曾把「点击无反应」归因于 `getQuestDrop` 为空时
  交互 AI 静默早退（门禁 ⑳）。用户实机复测（掉落修复生效后）：**右击 702730 依然零响应**——该早退
  位于 `selectDialog` **成功之后**，而实测连 3 秒采集进度条都不出现（`talk_info delay=3`），说明断在
  更上游。
- **15011 首因（第二轮定位，代码级确证）**：真端 `data_driven_quest.xml` 的 15011 行是 **CollectItem
  步**：`value0_progress_=LF5_Biologia_E`（交付 NPC，注册进对话平面）+ **`value1_progress_=
  LF5_FOBJ_Starflower_Coral_Q15011a`（追加 FOBJ 列 = 采集物件 702730）+ `value5_progress_=1`**。真端
  载荷词典（`DataDrivenQuestTable` 注释）：**列 1..4 = 追加 FOBJ、列 5 = 整数**；但 DD 运行时只消费
  `Step.payload()`（= 列 0）⇒ 物件面零触点：
  1. `QuestItemNpcAI2.handleDialogStart` 的资格门 `onCanAct(ACTION_ITEM_USE, 702730)` 返回 false
     （采集族 `allowsItemUse` 不认它；typed 目录已退役；DD 无此面）；
  2. `getQuestNpc(702730).getOnTalkEvent()` 为空（该对象从未注册）⇒ `canStartInteraction=false`
     ⇒ **直接 return，零包**（无进度条、无掉落、无系统消息——与「没反应」完全吻合）；
  3. 即便越门，交互认领 `selectDialog(-1/31)` 同样无人认领（DD 对话平面不认领 -1；31 需 talks 表，
     而该对象不在其中）。
  对照退役 XML（`git show 4ede058c0^:…/quest_definition/quests/15011.xml`）：702730 上只声明一条
  **can-act `ACTION_ITEM_USE` 自环**（`source=started`，零副作用、零条件——资格语义）+ `<drops>`
  条目；两条面在 DD 车道都没接。全表复算：**82 个 CollectItem 步携带 FOBJ 列**（含表内 15 块
  `Collectitem` 大小写变体；`Kind.of` 归一为小写，加载器口径），94 个物件名；其中 10 行
  XML_RETENTION/非台账（如 10112/20112 的 `Ab1_FOBJ_Supply_Box_Q10112a`）不得由 DD 供源。

## 2. 影响面（真端全量复算，995 个 `RETAIL_TABLE` 掉落行，0 残留）

| 车道（台账 family） | 掉落行 | 状态 |
|---|---|---|
| SimpleTalk | 654（含 4105 第 2 槽） | ✅ 2026-10-04 已注册；4105 本次补注册 |
| SimpleCollectItem | 177 | ✅ 2026-10-04 已注册 |
| **DataDriven** | **163**（注册 160 + 冻结残差 3） | ❌ → 本次补齐 |
| **SimpleUseItem** | **1（2435 덩굴 목걸이）** | ❌ → 本次补齐 |
| Hunt / SerialHunt / ItemPlay / Combine | 0 | — |

- **次级缺口（本次暴露）**：共享预筛 `hasRetailDropColumns` 原只查第 1 槽 ⇒ 任务 **4105**（掉落只
  写在 `drop_monster_2`/`drop_item_2`）从未注册；跨族护栏 653 vs 654 暴露后已收为**任意槽位**
  （`numbered("drop_item_")`/`numbered("drop_monster_")`）。

- DD 侧另有两类**不得注册**的行：**13 行 XML_RETENTION**（10032/10112/10527/10530/15606/17540/20031/
  20112/20527/20530/25050/25082/27540，目录供源单一 owner）与 45 行不在台账（非生产全集）。
- **冻结 ∩ drop = {15602, 15605, 15608}**（ZONE_ABSENT，LF6 进区别名真端缺席）⇒ 显式惰性残差
  （不可达 ⇒ 掉落惰性；解冻后注册自动接手，门禁钉子翻红强制复核）。

## 3. 承重事实：collectingStep 与 DD 步号对齐

- 客户端 `quest_monster.csv:2825`＝`10034,Progress(3),quest_10034a,questItemDropMonster,…`；
  真端 `collect_progress=3`；DD 行第 4 步（idx3 = Talk `LF4_FOBJ_Q10023D`，`value2_progress_=QUEST_10034A 1`
  = 携物交谈话）——三源同值互证。
- `QuestVars.setVar`（`model/QuestVars.java:116-122`）把打包字按 **6-bit 拆槽** ⇒ `getQuestVarById(0)`
  就是 DD 步号本身，`QuestService.isQuestDrop` 的 `collectingStep == var0` 直比成立（**非 1-based 错位**）。
- 163 行 DD 掉落行复算：`collect_progress` = 0（122 行）或 < 步数（41 行），0 越界（QE-025 死锁类无违例）。

## 4. 实现面

| 文件 | 变更 |
|---|---|
| `tablelane/NativeQuestXmlTable.java` | 新增 `hasRetailDropColumns(int)`（三族共享预筛，从 SimpleTalkHandler 私有方法提级单一源） |
| `tablelane/DataDrivenNativeRuntime.java` | `create` 路由循环后注册段 `registerRetailDrops(routed, sink)`（逐行 catch IOException/RuntimeException → fail-closed；`clean()` 才收）+ 字段/构造参数/早退分支/视图 `questDropsFor(int)`、`dropInterests()`；**`create` 不声明 throws**（原实现即如此），故逐行内部捕获 |
| `tablelane/SimpleUseItemHandler.java` | 路由分支内同构注册 + 字段/装配/视图；`metadataOf(int)` 抽出并与既有 `metadataClean` 单码路 |
| `questEngine/QuestEngine.java` | `questDrops` = 目录 ∪ Collect ∪ Talk ∪ **UseItem** ∪ **DD**；目录 id 去重保持现状；native 源之间不运行期去重（族间由台账 family 列互斥，重复即缺陷 → 门禁报红） |
| `tablelane/SimpleTalkHandler.java` | 预筛改为共享方法（原私有静态删除） |

**激活语义不变**：注册无条件，激活由 `QuestService.isQuestDrop`（START + `var0==collectingStep`/0 + 上限）
负责；冻结行/XML_RETENTION/非台账行被 `routed` 过滤天然排除。

**第二轮（采集物件使用面，15011 首因）**：

| 文件 | 变更 |
|---|---|
| `tablelane/DataDrivenNativeRuntime.java` | 新增 `collectObjectsByNpcId` 索引（`registerCollectObjects`：routed 行 CollectItem 列 1..4 → 解析物件名，逐条 fail-closed；**零改路由**）+ 字段/构造参数（末位，避让并行在飞的 `instanceLeaveRollbacks`）/早退分支；新 API `allowsItemUse(player, objectNpc)`（START + `guardClear` + 当前步命中）、`isCollectObject`、`collectObjectQuestIds`、`collectObjectInterests`（门禁对拍面）；`onDialog` 增 `dispatchCollectObjectUse`（requestedOwner≠0 时认领：零状态写、零发包；owner=0 由引擎先手重放） |
| `questEngine/QuestEngine.java` | `onCanAct(ACTION_ITEM_USE)` 增 DD 分支（OR，采集族口径并列）；`onDialog` 的 questId=0 先手重放区增 DD 物件分支（按候选重放、`env.setQuestId` 携带 owner ⇒ 组队每人一枚的掉落过滤保留；认领即吞掉、不落通用页 10） |
| `ai/QuestItemNpcAI2.java` | 失败应答分型并入 DD 采集物件（`isCollectObject`）⇒ `SILENT_COLLECT` 零包口径（QE-137） |

**认领语义**（对齐采集族 `onObjectUse`）：START + `step(vars)==物件步` 才认领；未接取/步不符/REWARD
⇒ 认领但零副作用（不推进、不发包、不开窗——物件绝不落通用页 10）。

## 5. 门禁

- `DataDrivenNativeRuntimeGateTest` ⑱/⑲：注册面 = routed ∧ 真端 drop 列（独立重解析对拍，零静默跳过）；
  计数钉 163/3/160；13 行 XML_RETENTION 负例；collectingStep ∈ {0}∪[1,步数) 全量；10034 形状冻结
  （216494 → 182215627 / 100 / each-member / step 3）+ `new QuestEngine().questDrops(216494)` 端到端。
- `SimpleUseItemNativeFamilyGateTest#useItemFamilyServesTheRetailKillDrops`：`routes(2435)` +
  212548/212549 → quest_2435a（182204181）/ 80% / step 0 + 负例（同 NPC 无其它任务条目）。
- **新 `RetailDropLaneCoverageGateTest`（跨族护栏）**：∀ `RETAIL_TABLE` ∧ drop 列的行必须落在四张
  已注册车道之一且在其车道路由（DD 3 行冻结残差显式白名单）；族直方图冻结 654/177/1/163；
  XML_RETENTION ∩ drop = 163 不得由 native 四车道路由（单一 owner）。防第五例同因事故。该门首次
  运行时 653 vs 654 暴露 4105 第 2 槽缺口（预筛放宽后绿）。
- **DD 门 ㉑ `collectStepFobjColumnsServeTheObjectUseFace`（第二轮新增）**：独立重解析真端 DD 表对拍
  `routed ∩ CollectItem 列 1..4 → 物件索引`（逐条相等、零静默跳过；解析失败在测试侧先红）；
  计数钉 **82 行**（含 15 块 `Collectitem` 大小写变体——`Kind.of` 归一）；15011/702730 资格与认领
  端到端（`allowsItemUse` + `QuestEngine.onDialog` 先手认领 + `env.questId=15011`）；负例：未接取/
  步不符/REWARD 不可交互、未接取的打开被"认领+零发包"吞掉（`dialogPages` 空钉）、XML_RETENTION
  物件（`Ab1_FOBJ_Supply_Box_Q10112a`）缺席。

## 6. 验收状态（2026-10-08，IDEA MCP 已授权运行）

- **实现完成**（IDE 检查 0 error；`create` 早退分支 arity 已随构造参数同步）。
- **单测全绿**：
  - `DataDrivenNativeRuntimeGateTest` **45/45**（含新 ⑱ `routedRowsCarryTheirRetailDropColumns`
    注册面独立对拍 163/3/160 + 13 行 XML_RETENTION 负例；⑲ `row10034ServesItsRetailDropThroughTheEngineQuery`
    ＝10034 形状 216494→182215627/100/each-member/step3 + `new QuestEngine().questDrops(216494)`
    端到端；⑳ `growthCollectRowsServeTheirObjectDrops`＝15011/702730；任意槽位改动后复跑确认）。
  - `SimpleUseItemNativeFamilyGateTest`（含 `useItemFamilyServesTheRetailKillDrops`：2435/212548/
    212549/182204181/80%）。
  - `RetailDropLaneCoverageGateTest`（新，4105 补注册后绿）。
  - 回归：`SimpleTalkNativeFamilyGateTest` 17/17、`SimpleCollectItemNativeFamilyGateTest` 19/19、
    `QuestDropContractGateTest` 1/1、`QuestProductionStartupGateTest` 2/2、`RetailOwnershipGateTest`
    5/5、`QuestEngineOpenDoorReplayOrderTest` 1/1。
- **第二轮补充（15011 首因修复）单测全绿**（2026-10-08 IDEA MCP 第二次授权）：
  `DataDrivenNativeRuntimeGateTest` **50/50**（含 ㉑；并行在飞的 instance-rollback 用例同批全绿）、
  `QuestItemNpcAI2Test` 3/3、`QuestEngineOpenDoorReplayOrderTest` 1/1、
  `SimpleCollectItemNativeFamilyGateTest` 19/19、`SimpleTalkNativeFamilyGateTest` 17/17。
- **实机验收（2026-10-08）**：10034 击杀 216494 ⇒ ✅ 用户确认「锯齿形状的刀」恢复正常；15011
  第一轮修复（掉落）实机仍零响应 ⇒ 定位首因并二次修复（上表），重启后复测 ⇒ ✅ 用户确认
  **「实机验证成功」**（右击 702730 整链：3 秒采集进度条 → 掉落 quest_15011a → 7/7 交 804875）。
  验收记录 = `.agents/summary/quest-acceptance/10034-15011-2026-10-08-client-accepted.md`；
  2435 同型抽验（212548/212549，80%）仍 PENDING（用户未提及，未纳入本次确认范围）。

## 7. 残留 / 风险

1. DD 冻结 3 行（15602/15605/15608）为显式惰性残差；解冻即自动接手。
2. `create` 新增构造期 I/O 依赖（`NativeQuestXmlTable.instance()` + `RetailQuestDriver.ensureLoaded()`）：
   生产启动同相已装载；测试侧由 ⑱ 对拍暴露静默失败（dirty 元数据会让注册面变窄 → 红）。
3. 性能：DD 侧 160 行元数据编译（driver 进程内缓存；Talk 654 行既有先例）。
4. `QE-138` boundaries ⑤ 的 DD 8 件 Shape B 行属报告面，另案。
5. 非台账 45 行（非生产全集）与 XML_RETENTION 13 行不注册（门禁负例钉死）。
6. CollectItem 列 5（整数；全表 94 步）语义仍未消费（载荷词典注记；15011=1 / 15010=0，无实机面）；
   列 2..4 多物件步（17/6/4 步）已按同口径注册，无实机复测面。

## 8. memory-bank 收口

- `QE-139`（`COLLECT_FAMILY_ITEM_DRIVEN`）：boundaries ⑤（DD 221 行）与 ⑥（UseItem 2435）两条悬案
  收口为「2026-10-08 已注册（DD 160 + UseItem 1）」；scope / last_verified / symptom / root_cause /
  fix_or_guardrail / evidence / validation / keywords 与安全网三行随本批更新（symptom-index 与
  index.jsonl 由 `sync_memory_bank.py` 重生成）。
