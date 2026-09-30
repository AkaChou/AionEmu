# SimpleTalk 规范形勘察（下一面执行底稿）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 车道：`quest-native-dispatch`；面：**SimpleTalk 规范形（S1 单步面 + S2 链面）**。
> 本文只勘察、不落码；所有数字与 file:line 均为 2026-09-27 在本工作区实测（工作区处于
> 兄弟车道在飞状态，动工前必须按 §5 复验窗口）。
> 参照物：已收口 8 片 + DD 四子面 D-a（DD talk/collect canonical 化）。

## 0. 一句话结论

SimpleTalk 是全族最大的单次改造面（**2089 行 RETAIL_TABLE**），但**单步面（1804 行）是纯形状迁移**：
接取段换成 `canonicalAcceptFlow` 已有三参重载（签名逐字兼容，零新增函数），交付段换成
`canonicalDelivery` + `deliveryWindowPage`（零新增函数）；**唯一语义新增点是 13 行过场轴的重挂**
（canonical 形不存在 1007/1009 路由，`attachMovie` 会静默空转 ⇒ 过场静默丢失）。
链面（285 行）是**独立的第二片**：其形状由登记表 R 记录逐字回放 + 块路由覆盖规则决定，
**不做加载期过滤就会 100% 假绿**（量化见 §3.4）。

## 1. 规模与轴分布（实测）

| 口径 | 数量 | 证据 |
|---|---|---|
| 真端模板表总行 | 3152 | `src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml` |
| 家族规模（表 ∩ 生产宇宙） | **2223** | `RetailSimpleTalkGateTest.java:66`（`FROZEN_FAMILY_SIZE`） |
| ├ RETAIL_TABLE | **2089** | 交付面；本轮改造对象 |
| │ ├ 单步（无 `talk_npcN`）= S1 | **1804** | 本轮主面 |
| │ └ 链式（有 `talk_npcN`）= S2 | **285** | 第二片（登记表回放面） |
| └ XML_RETENTION | **134** | 不动 |
| 单步轴：`item_check` | **1346** | S1 交付段改造依据 |
| 单步轴：`con_quest` | **249** | 前置条件轴，接取段不读，不动 |
| 单步轴：`give_item` | **134** | 接取发物（`acceptGiveItemActions`） |
| 单步轴：`cutsceneid1` / `cs1_haction` | **13 / 13** | **唯一语义新增点**（§3.3） |
| 链式轴（本片不动，供 S2 参考）：`cutsceneid1` | 4 | 1422/2421/3006/3020，编码在 R 记录 after 列 |

复算命令（只读）：

```bash
# 家族/单步/链/轴：把 Quest_SimpleTalk.xml 的 <id> 块按 retention owner=SimpleTalk 过滤后计数
python3 - <<'PY'
import re, collections
own={}; fam={}
for ln in open('src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv',encoding='utf-8'):
    p=ln.rstrip('\n').split('\t')
    if len(p)<5 or ln.startswith('#'): continue
    own[int(p[0])]=p[1]; fam[int(p[0])]=p[2]
s=open('src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml',encoding='utf-8').read()
blocks=re.split(r'<id\s+id="', s[s.index('<quest_simpletalks>'):])[1:]
rows={int(b.split('"')[0]):{k:v.strip() for k,v in re.findall(r'<(\w+)>([^<]*)</\1>', b)} for b in blocks}
def chain(d): return any(k.startswith('talk_npc') and v for k,v in d.items())
ret=[q for q,d in rows.items() if fam.get(q)=='SimpleTalk' and own.get(q)=='RETAIL_TABLE']
print('RETAIL_TABLE', len(ret), 'single', sum(1 for q in ret if not chain(rows[q])), 'chain', sum(1 for q in ret if chain(rows[q])))
PY
```

## 2. S1 单步面：落点与改造规格

落点文件：`src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkDefinitionCompiler.java`（1416 行）。
单一入口：`compile(...)` → `:114-117` 按 `entry.singleStep()` 分派 `build`（单步）与 `buildChain`（链）。
单步面**无外部调用者**（链面走 `buildChain`），因此 S1 可独立收口。

### 2.1 接取段（`:324-331`）

现状：

```java
transitions.addAll(attachMovie(acceptFlow(acquiredNpc, "started",
    acceptGiveItemActions(entry)), acquiredNpc, QuestDialogAction.ASK_QUEST_ACCEPT.id(), ...));
if (exits.requires(entry.questId(), RetailClientDialogExits.SELECT1_1)) {
    transitions.addAll(acceptContinuation(acquiredNpc, exits.requires(..., SELECT1_1_1)));
}
```

改造：`acceptFlow(...)` → `RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow(npc, "started", actions)`
（`RetailSimpleHuntDefinitionCompiler.java:1096-1122`，三参签名与本文件 `acceptFlow(int,String,List<QuestAction>)`
（`:1014`）**逐字一致 ⇒ 零新增函数**）；`:328-331` 续页块随页链删除；私有 `acceptFlow`（`:1005/1009/1014`）
与 `acceptContinuation`（`:1049-1058`）在链面同步退场后删除（链面 `acceptFlowChain:769` 亦调用 `acceptFlow` ⇒
**S2 未收口前不能删**，与 SimpleCollectItem 的 legacy statics 保留同口径）。

`acceptGiveItemActions`（`:958-970`）与交互物门（`:341-356`）**保留**：前者是接取发物通道（判例 1131），
后者是 P0c-22 启动期交互对象合同（判例 18509/28509）。`reportNpcExit`（`:1065-1071`）保留。

### 2.2 交付段（`:357-372`）

现状三分支（`entry.itemCheck()` × `metadata.itemRequirements()`）：

> 下表三个数字是**全部 1804 个单步行按轴分解的形状计数**（`item_check` × `quest.xml` 是否声明
> `collect_item*`），**不是受理数**——受理数只能由编译器/家族门实测（原因见 §4.1：漂移登记对已退役行是
> 陈旧冻结值，不能用来推断受理状态）。三者相加 = 1804，与 §1 一致。

| 子形 | 行数（轴分解） | 现状实现 | 规范形映射 |
|---|---|---|---|
| 无 `item_check` | **458** | `reportFlow(npc, [])`（`:1074-1102`）：QUEST_SELECT→SELECT5 页 + 1009 无门 → REWARD+窗 1 | `canonicalDelivery(npc, [], [], deliveryWindowPage(...), "started")` |
| `item_check` + 有 `collect_item` | **1341** | `itemCheckReportFlow(npc, items, SELECT6?)`（`:1144-1174`）：SELECT5 页 + 39/20002 双按钮成功/失败对 | 同上，`hasItems/removeItems` 取 `metadata.itemRequirements()` |
| `item_check` + 无 `collect_item`（工作物品门） | **5** | `reportFlow(npc, checkGate)`（`:1103-1115`）：1009 prio0 HasItem→REWARD+窗 / prio1 →started+选择页 | 同上，门物品取 `workItemRequirement(entry)`（`:1122-1130`，give_item 同物） |

- 交付窗页：`RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage(metadata, questId)`
  （`RetailSimpleCollectItemDefinitionCompiler.java:455-462`）——零奖励组兜底固定窗 1，禁分档推算。
  SimpleTalk 的 `completeFlow` 对 `rewardGroups().size() > 1` 抛错（`:1318-1321`）⇒ 单步面不存在分档 ≥2 的行，
  窗页实际恒为档 0/兜底 1（**语义与现状 `SHOW_SELECT_QUEST_REWARD_WINDOW1` 等价，非行为变更**）。
- 交付边：`canonicalDelivery(npc, hasItems, removeItems, windowPage, "started")`
  （`RetailSimpleCollectItemDefinitionCompiler.java:473-479`）——after 序 `[Sync(LEVEL_AND_VISIBILITY_REFRESH), ShowQuestDialog(窗)]`
  与现状 success 分支逐字相同；未集齐零路由（关窗兜底交 DialogService），**替代**现状的 prio-1 失败回页
  （工作物品门子形）与 39/20002 失败页（collect 子形）——与采集族规范形同口径，已获 DD/采集两片先例。
- 随页链删除：`REPORT_PAGE` 常量（`:62`）、`reportFlow`（2 参）/ `itemCheckReportFlow` / `workItemRequirement`
  在 S1 无调用者后删除（**`itemCheckReportFlow` 无链面调用者，可随 S1 删；`reportFlow` 同名链面函数是
  `reportFlowChain`，互不影响**）。顺手可清的死代码：一参重载 `reportFlow(int)`（`:1074-1076`）**当前已无调用者**
  （实测 `grep "reportFlow(rewardNpc)"` 零命中）。
- 保留：`completeFlow`（`:1313-1336`，8..23 + 108/110+k 自动通道 + 职业路由）、legacy heal 边
  （`:374-392`，`RetailLegacySaveHealRows` 2 行）、交互物门、`journalRowRepair` 等价物。

### 2.3 过场轴重挂（唯一语义新增点）

**问题**：`attachMovie`（`:979-1003`）按 `(npcId, dialogId==triggerActionId)` 匹配并重写路由；
canonical 形里**不存在** 1009(SELECT_QUEST_REWARD) 与 1007(ASK_QUEST_ACCEPT) 路由 ⇒
match 数 = 0 ⇒ 函数原样返回（"无匹配路由时原样返回"）⇒ **过场静默丢失**，且当前无任何断言会发现。

**受影响 id（13 行，与 `p0c10n-cutscene-decisions.tsv` 的 13 ADOPT 行 ID 集完全一致）**：

| quest_id | movie | cs1_haction | 触发语义 | item_check | give_item | 接取 NPC | 交付 NPC |
|---|---|---|---|---|---|---|---|
| 3943 | 93 | 1009 | 报告侧 | - | ITEM_DOC_QUEST_3943A | Anteros | Fasimedes |
| 3946 | 94 | 1009 | 报告侧 | - | ITEM_DOC_QUEST_3946A | Vulcanus | Fasimedes |
| 3949 | 95 | 1009 | 报告侧 | - | ITEM_DOC_QUEST_3949A | Utisda | Fasimedes |
| 3952 | 96 | 1009 | 报告侧 | - | ITEM_DOC_QUEST_3952A | Hestia | Fasimedes |
| 3955 | 97 | 1009 | 报告侧 | - | ITEM_DOC_QUEST_3955A | Diana | Fasimedes |
| 3958 | 98 | 1009 | 报告侧 | - | ITEM_DOC_QUEST_3958A | Daphnis | Fasimedes |
| **4056** | **403** | **1007** | **接取侧** | **1** | ITEM_QUEST_4056B | Jackspaner | **Jackspaner（同 NPC）** |
| 4947 | 125 | 1009 | 报告侧 | - | ITEM_DOC_QUEST_4947A | Logi | Vidar |
| 4950 | 126 | 1009 | 报告侧 | - | ITEM_DOC_QUEST_4950A | Kinterun | Vidar |
| 4953 | 127 | 1009 | 报告侧 | - | ITEM_DOC_QUEST_4953A | Lanse | Vidar |
| 4956 | 128 | 1009 | 报告侧 | - | ITEM_DOC_QUEST_4956A | Lainita | Vidar |
| 4959 | 129 | 1009 | 报告侧 | - | ITEM_DOC_QUEST_4959A | Honir | Vidar |
| 4962 | 130 | 1009 | 报告侧 | - | ITEM_DOC_QUEST_4962A | Zyakia | Vidar |

**重挂方案（证据驱动，逐条可查）**：

1. 客户端映射证据：`docs/quest/client-dialog-mapping/page-action-map.csv:30-34` 证明**页 4**
   （`HTML_PAGE_SHOW_ASK_QUEST_ACCEPT_WINDOW`）的按钮集恰为 `1002/1003/1008/20000/20001`（与
   `canonicalAcceptFlow` 的提交/拒绝/关窗族逐字对应，无死按钮）；同表 `:76`（`select1` 带 1007）、
   `:159`（`select1_1` 带 1007）、`:269`（`select2` 带 1009）证明 **1007/1009 都是"页链按钮"，
   页链在 canonical 形退场后不再由服务端驱动**。因此过场的同构落点不是"某个消失的按钮"，
   而是"**下发对应窗页的那条 QUEST_SELECT 边**"。
2. 落点规则（触发 → canonical 边）：

   | cs1_haction | 现状重挂边（legacy） | canonical 重挂边 | 插入点 |
   |---|---|---|---|
   | 1009（12 行） | `SELECT_QUEST_REWARD` on 交付 NPC（reportFlow started→reward） | **`QUEST_SELECT`(31)，交付边 `started→reward`，`npcId == 交付 NPC`** | 在 `ShowQuestDialog(交付窗)` **之前**（保持现状 `Sync → PlayMovie → 开窗` 编码序） |
   | 1007（4056） | `ASK_QUEST_ACCEPT` on 接取 NPC（unaccepted→unaccepted） | **`QUEST_SELECT`(31)，接取开窗边 `unaccepted→unaccepted`，`npcId == 接取 NPC`** | 在 `ShowQuestDialog(页 4)` 之前 |

3. **必须换匹配键（禁止直接复用 `attachMovie` 的 `(npc, action)` 键）**：4056 的接取 NPC 与交付 NPC
   是同一个 `Jackspaner`，canonical 形下该 NPC 同时拥有"接取开窗 QUEST_SELECT（unaccepted→unaccepted）"
   与"交付 QUEST_SELECT（started→reward）"两条边 ⇒ `(npc, action)` 键**同时命中两条**，会把过场
   复制到交付边上。**规格：新增作用域匹配器 `(sourceNode, npcId, actionId)`（或等价谓词），
   并加 fail-closed 断言：过场行的 match 数必须恰为 1，否则抛错/拒绝**（现状"零匹配静默通过"必须消失，
   这是本片唯一必须新增的防假绿机制）。
4. `precheck`（`:208-224`）的受理矩阵**保持不变**（1009 且 `!item_check` / 1007），仅把注释语义从
   "触发路由"改写为"重挂边"；`supported=false` 的行（含 28 行 craft KEEP_XML 行）继续拒绝/留 XML。
5. 回归位：`RetailSimpleTalkGateTest` 目前**没有任何过场断言**；建议 S1 收口时在家族门补一条
   "已受理的过场行，定义内 `PlayMovie` 数 == 1 且 movieId == 真端 cutsceneid1"的不变量
   （`ACCEPTED_FLOOR` 只数受理行数，静默丢失电影不会改变计数，拦不住）。

## 3. S2 链面：落点与"假绿"机制

### 3.1 落点（`buildChain`，`:403-624`）

| 组成 | 位置 | S2 处理 |
|---|---|---|
| N 记录 → 节点 | `:405-415` | 不动（链门断言节点 = 登记表） |
| SystemGrant 边 | `:416-420` | 不动 |
| NPC_START 块 → `acceptFlowChain` | `:421-440`（含 `:435-438` 续页梯） | 接取段翻 canonical；续页梯删 |
| **R/C/Q/E 记录逐字回放** | `:441-463` | **必须过滤冲突键**（§3.3） |
| I 记录 → `itemReportGate` | `:468-470`（实现 `:857-907`） | 交付段翻 canonical 后 I 记录整类退场（39/20002 对不再下发） |
| NPC_REPORT 块 → `reportFlowChain` | `:471-482`（实现 `:790-847`） | 交付段翻 canonical（QUEST_SELECT 带门直翻 REWARD + 分档窗） |
| NPC_COMPLETE 块 → `completeFlowFromBlock` | `:492-494`（实现 `:1182-1274`） | 不动（已是规范形 8..23） |
| **显式路由覆盖块路由** | `:495-506` | **假绿根因所在** |
| 块间同键去重 | `:507-519` | 不动 |
| SELECT2_CONTINUE 关窗闭包 | `:523-547` | 页链退场后可删（需逐行核） |
| SELECT5_CHECK 领奖态重开窗 | `:548-570` | 规范形交付已含 REWARD 重开 ⇒ 可删（需核） |
| SELECT6 失败页关窗闭包 | `:571-598` | I 记录退场后无 select6 下发者 ⇒ 可删（需核） |
| 领奖行自愈（无 E 记录时发射） | `:599-614` | 不动 |
| 布局回放 | `:615-621` | 不动 |

### 3.2 假绿机制（`buildChain:495-506` 原文口径）

```
explicitRoutes = { sourceNode:npcId:dialogId | R 记录逐字回放出的 TalkToNpc 边 }
blockTransitions.removeIf(t => key(t) ∈ explicitRoutes)   // 显式路由覆盖块路由
```

即：**只要 R 记录里有同键的 `QUEST_SELECT`，块里 canonical 合成的 `QUEST_SELECT` 交付边就整条被删掉**，
最终形状 = 老的 R 记录形状。改完编译跑起来"没崩、门禁也绿"，但形状一点没变 —— 这是本片最大的假绿源。

### 3.3 冲突面量化（285 行冻结行实测）

| 指标 | 数值 |
|---|---|
| 冻结行（`retail-simple-talk-chain-ir-fingerprints.tsv`，285 行 = 表内全部 RETAIL_TABLE 链行） | 285 |
| 其中有 R/C/Q/E 路由记录的行 | **280**（5 行为"纯规范块残组行"） |
| 带 `QUEST_SELECT` R 记录的行 | **268** |
| 带 ≥1 个 canonical 键动作（QUEST_SELECT / FINISH_DIALOG / QUEST_ACCEPT_1 / QUEST_ACCEPT_SIMPLE / QUEST_REFUSE_1 / 2 / SIMPLE）的行 | **269** |
| R 记录动作分布（Top） | QUEST_SELECT 744、SETPRO1 300、SELECT_QUEST_REWARD 192、SELECT2_1 154、ASK_QUEST_ACCEPT 135、SETPRO2 111、1353 102、SELECT3_1 62、SELECT1_1 53、USE_OBJECT 50、SETPRO3 47、FINISH_DIALOG 41、1694 41、SELECT4_1 29、QUEST_ACCEPT_1 21、QUEST_REFUSE_1 21、QUEST_REFUSE_2 20、QUEST_REFUSE_SIMPLE 18、2035 18、QUEST_ACCEPT_SIMPLE 17 |

⇒ **不做过滤就 269/280 ≈ 96% 的行形状不变（假绿）**。任务书给的四个数（764/135/160/55）是**全登记表**
口径（含 XML_RETENTION 行）；本表是**仅 285 冻结行**口径（744/135/154/53），二者都被本轮证据支持，
引用时需注明口径。

### 3.4 过滤策略三选一（S2 动工前必须先定）

| 策略 | 做法 | 优点 | 风险 |
|---|---|---|---|
| **A 加载期过滤（推荐）** | `buildChain` 增 canonical 旗标：对 canonical 键动作中"已被块边覆盖"的 R 记录（同 `(source,npc,action)` 且在 canonical 块边集合内）不再参与 `explicitRoutes` 集合，从而不覆盖块边；冲突 R 记录本身也不再回放 | 登记表保持**冻结证据**不动；与"生成物即留痕通道"纪律一致；改动集中在一个函数 | 需要精确的"canonical 键集合"定义（建议常量表 + fail-closed：未被集合覆盖但形状与 canonical 矛盾的记录必须报错而非静默忽略） |
| B 删键冲突行 | 直接删登记表里冲突的 R 行 | 最直观 | 登记表是**跨车道共享输入**（§5），删行 = 改冻结证据，需与 `scriptdll-quest-driver` 车道协商；且链门"登记形状下限"断言会一起动 |
| C 重生成登记表 | 用生成器重算式重写 `quest_client_talk_chain_steps.tsv` | 一步到位 | 生成器在兄弟车道目录；重生成会让 4832 条记录整体换血，指纹/分区/对拍证据全部作废 |

**B/C 均触碰兄弟车道产物，A 是唯一不动别人输入的选项**；若最终选 B/C，必须先走跨车道协商（README"先查作者"纪律）。

### 3.5 S2 附带项：4 行链式过场的电影迁移（易被 A 策略顺手删掉）

`quest_client_talk_chain_steps.tsv` 中 `MOVIE:` token 共 4 处，全在 R 记录的 after 列：

| quest_id | R 记录 | 电影 | 承载边（页） |
|---|---|---|---|
| 1422 | `R 4 … 1353 started→started` | `MOVIE:100:CUTSCENE` | `DIALOG:SHOW_QUEST_PAGE:SELECT2_1` |
| 2421 | `R 4 … 1353 started→started` | `MOVIE:132:CUTSCENE` | 同上 |
| 3006 | `R 8 … 1694 s1→s1` | `MOVIE:361:CUTSCENE` | `SELECT3_1` |
| 3020 | `R 2 … ASK_QUEST_ACCEPT started→started` | `MOVIE:363:CUTSCENE` | `SHOW_ASK_QUEST_ACCEPT_WINDOW` |

A 策略若把 `ASK_QUEST_ACCEPT`(3020) 与页链 `1353/1694` 记录一并过滤，**这 4 部电影会静默消失**。
⇒ S2 规格必须写明：4 行的 `MOVIE:` token 逐个迁到 canonical 边（3020 → 接取开窗 `QUEST_SELECT`；
1422/2421/3006 → 对应 briefing/报告 canonical 边），并在链门补"电影数守恒"断言。

## 4. 门禁与测试面（改造前必须逐项对账）

### 4.1 T1 家族门 `RetailSimpleTalkGateTest`（647 行，`src/test/java/.../retail/RetailSimpleTalkGateTest.java`）

| 断言点 | 位置 | S1 同步动作 |
|---|---|---|
| `FROZEN_FAMILY_SIZE = 2223` | `:66` | 不动（家族规模与形状无关） |
| `ACCEPTED_FLOOR = 1638` | `:69` | 不动（S1 不改变受理集合）；它是**下限**断言，13 行过场静默丢失（少 13 部电影，受理数不变）**不可能被它拦住** ⇒ 必须另设不变量 |
| `hasAccept`（QUEST_ACCEPT_1/SIMPLE + StartEligible → START） | `:340-348` | **不改**（canonical 形保留两形提交） |
| `hasSelect1Continuation`（SELECT1_1/SELECT1_1_1 路由） | `:285-289` + `:450-463` | **删**（页链退场，与 CollectItem 门同口径） |
| `hasReport`（QUEST_SELECT+SELECT5 页；39/20002 对；1009 工作物品门对） | `:350-448` | **整体改写**为 canonical 交付三态：QUEST_SELECT +（空门 / collect 整组门 / 工作物品门）→ REWARD + `rewardWindowForTier` 查表窗；失败侧断言改"零路由" |
| `hasComplete`（8..23 全 16 档） | `:465-474` | 不动 |
| `hasAnyAcceptRoute` / `hasSystemGrant` | `:311-329` | 不动 |
| `driftVersusLegacyXmlIsRegistered` | `:154-185` | **已退役行只校验"有登记"，不重算** ⇒ S1 不会引起已退役行登记漂移；**未退役的 134 行（XML_RETENTION）会重算**：其中 60 行是 `DIFF:TRANSITION_SET`，因为改的是 transitions 而非 nodes，判定种类应保持 `TRANSITION_SET`。**动工前用 `-Dretail.talk.equivOut=<path>` dump 一次基线，改后再 dump 对拍**；出现种类翻转（→ `NODE_PROJECTION`/`EQUIVALENT`）才是真信号 |
| 登记表行数 == 2223 | `:181-182` | 不动 |

**重要发现（对称性缺口）**：`retail-simple-talk-drift.tsv`（2223 行）对已退役行是**冻结证据、不重算**——
本工作区实测：285 个链行全部被指纹门证明"已受理"，而同一批行的漂移登记里有 **189 行写着 `REJECTED`**
（陈旧冻结值）。⇒ **不得用漂移登记推断受理状态**；且**单步面没有任何 IR 指纹冻结器**
（链面有 `retail-simple-talk-chain-ir-fingerprints.tsv`）。建议 S1 顺手补一个单步面冻结通道
（复用 `RetailIrFingerprint` + `-Dretail.talk.fpOut`，1804 行一组），否则 S1 的形状保护只剩 T2/T3 + 契约门。

### 4.2 T1 链门 `RetailSimpleTalkChainGateTest`（300 行）

- 指纹冻结器：`retail-simple-talk-chain-ir-fingerprints.tsv`（**285 行** = 表内全部 RETAIL_TABLE 链行；
  类注释里的"83 行 ADOPT"是 wave-A 遗留文案，已过期）；重冻通道 `-Dretail.talkChain.fingerprintOut`（`:115`）。
- 分区不变量（`:162-204`）：登记行集 = ADOPT（RETAIL_TABLE）× KEEP（XML_RETENTION），无孤儿；ADOPT 行必须
  节点/布局记录齐备且"有路由或 NPC_START 块"⇒ **B 策略删行会直接打这个断言**。
- 回放保真（`:112-159`）：节点投影必须等于 N 记录 ⇒ **过场重挂不得改动任何节点**（只改 after 动作）。

### 4.3 T2：180 个受影响测试类（选择器实测）

```
python3 -B .agents/summary/scriptdll-quest-driver/affected_quest_tests.py --json <2089 个 id>
→ scanned=1104 affected=180 unmatched_ids=1647 missing_t1_gates=[]
```
（受影响类分布：`questEngine.definition` 118、`runtime` 20、`e2e` 7、`retail` 3、其余 32 类为数字巧合命中；
1647/2089 个 id 没有任何测试引用。）

**锁旧形粗筛（机器筛，非裁定）**：180 类中，id 集 ∩ SimpleTalk RETAIL_TABLE 非空且源码含
`SELECT5 / CHECK_USER_HAS_QUEST_ITEM / SELECT_QUEST_REWARD / SELECT1_1 / SELECT6 / 2375` 之一者共 **89 类**。
形状锁最重的一批（`SELECT5`+`CHECK39`+`SELECT6`+`1009` 全命中，必改）：

| 类 | 家族 id | 锁的东西 |
|---|---|---|
| `RetailSimpleTalkMigrationReviewContractTest` | 3961-3964 / 4966-4969 / 1909 / 3208 / 3209 / 4208 / 11077 / 1971 + 11008 / 1218 / 11294 / 11458 | SELECT5 页 + 39/20002 双按钮对 + 失败页 SELECT6 + 工作物品门 prio1 回页；**外加 11008 的 select2 简报链 + 1218/11294/11458 heal 边**（heal 边保留、其余皆改） |
| `QuestKaligaCollectionClientDialogAlignmentTest` | 20 个 | 采集型交付页链 |
| `Quest3961To3964RetailAlignmentTest` | 3961-3964 | 同上 |
| `Quest1152RetailAlignmentTest` / `Quest28625And28626ClientDialogAlignmentTest` / `CollectTurnInClientActionAlignmentBatchTest` | 各 1-5 个 | 39/20002 + SELECT5/6 |

其余 80 余类的分档（`page2375` 报告页族、`SELECT1_1` 续页族、`1009` 报告族、`SELECT5` 交付族）按
README 既有"三模式分拣"（入口页 / 报告页 / 交付边 / 负控断言）逐条走：**先跑 T2 全量，按红集合分拣，
不预先改测试**（DD/CombineTask 两片的教训：预改会漏掉 T3 才暴露的契约类）。
另注意 `RetailQuestContractTest`（黑盒行走器，SimpleTalk#1118 用例）：**形状无关，改造前后必须同绿**，
是本片最重要的"不是假绿"证据。

### 4.4 T3 身份集对拍（收口线）

与 README 完全一致：T3 全树跑一次，对上一片收口基线做 `LC=C sort -u` 身份集对拍，
要求 **ADDED 0 / REMOVED 0**（S1 与 S2 各自收口一次）。T1 要求"唯一红仍是 20035 车道红"。

**新增 T1 门（2026-09-27 09:58 落地，已注册进 T1 列表）**：`RetailTsvManifestGateTest`
（真端 TSV 清单冻结门，`quest-retail-tsv-manifest.tsv` 27 行 + `EXPECTED_TSV_COUNT`）。
S1/S2 **不新增、不删除任何 TSV** ⇒ 该门在本片前中后必须**恒绿**；若它变红，说明改动作动了表集合
（例如为过场新造页码类表）——这正是本工程"停止为微观页码扩建 TSV"的红线，按红线处理而不是改清单。

## 5. 输入表依赖与并发窗口纪律

S1/S2 读取的生产输入表（任一被兄弟车道改写都会让本片证据失效）：

| 表 | 行数 | 本片用途 | 最近写入 |
|---|---|---|---|
| `quest_client_talk_chain_steps.tsv` | 4990 | S2 逐字回放（N/R/B/I/P 记录） | **2026-09-26 22:48**（兄弟车道） |
| `retail-simple-talk-chain-ir-fingerprints.tsv` | 285 | S2 指纹冻结（重冻通道） | **2026-09-26 22:49**（兄弟车道） |
| `Quest_SimpleTalk.xml` | 3152 | 表行（两片共用） | 2026-09-23 13:14 |
| `retail-xml-retention.tsv` | 6224 | owner 判定 | 2026-09-26 22:40 |
| `quest_client_dialog_exits.tsv` | 3938 | S1 现用（SELECT1_1/SELECT6 两处，改造后归零）、S2 仍用 | — |
| `quest_client_summary_rows.tsv` | 8931 | 领奖行投影（QE-051，保留） | — |
| `quest_client_reward_npcs.tsv` | 118 | 复合势力交付 NPC 集（保留） | — |
| `retail-simple-talk-drift.tsv` | 2223 | 未退役行重算对拍（`equivOut`） | 2026-09-26 13:10 |

动工前复验（窗口证据，任一新于本勘察文档即需先与作者对账）：

```bash
find src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv \
     src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv \
     src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv \
     -newer .agents/summary/quest-native-dispatch/2026-09-27-simpletalk-canonical-survey.zh-CN.md
ls -lT src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv \
      src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv   # 期望仍是 09-26 22:48 / 22:49
```

## 6. 建议的两片切分与收口线

| 片 | 面 | 新增代码 | 必须新增的防假绿机制 |
|---|---|---|---|
| **S1** | 单步 1804 行（接取 + 交付 + 13 行过场重挂） | **零**（全部复用 `canonicalAcceptFlow`/`canonicalDelivery`/`deliveryWindowPage`） | ① 过场作用域匹配器 + "match 数 == 1" fail-closed；② 家族门过场不变量；③（建议）单步 IR 指纹冻结通道 |
| **S2** | 链 285 行（块级接取/报告 canonical 化 + R 记录过滤策略） | 过滤策略（推荐 A）+ 4 行电影迁移 | ① canonical 键集合常量 + 未覆盖即报错；② 电影数守恒断言；③ 指纹重冻整表留痕 |

每片收口线（同 README）：`RetailQuestContractTest` 全绿 + T1 零新增失败（唯一红 = 20035 车道红）+
T3 身份集 `ADDED 0 / REMOVED 0`。

**顺序建议**：先 S1（不动登记表、不动链门指纹、可独立验证过场机制），S2 随后——S2 需要先定过滤策略，
且 S2 会改动链门指纹（285 行全漂移）。

## 7. 未决项（动工前需裁定）

1. **S2 过滤策略 A/B/C 的最终选择**（A 推荐；B/C 需跨车道协商）。
2. 过场重挂的落地形态：新增重载 `attachMovie(transitions, sourceNode, npcId, actionId, movieId)`
   还是把作用域匹配做成独立 helper（倾向后者：`attachMovieToRoute(...)`，与 `attachMovie` 并存，
   `attachMovie` 在 S2 收口后随 legacy 路径删除）。
3. 单步面是否新设 IR 指纹冻结器（建议设；不设则须在收口报告里显式声明"形状保护由 T2/T3 承担"）。
4. `SELECT2_CONTINUE` / `SELECT5_CHECK` / `SELECT6` 三个链面闭包块（`:523-598`）在 canonical 交付后
   是否整块删除——需逐行核对是否还有 canonical 边下发对应页（本勘察未逐行判定，标记为 S2 待办）。
