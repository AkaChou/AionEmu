# P3 步骤 3 报告：物品符号解析规则修正（真端两通道约定）+ 逐行交付门判定

> **批次**：P3 SimpleTalk（计划 §7）残余收口。**时点**：2026-10-01。
> **授权**：用户 2026-10-01「完全按真端方式来，理论上不需要再询问我，授权 maven 和提交」。
> **结论**：计划 §10.3 阻塞 9（静态数据缺口 14 项）**闭环 = 白名单归零**；阻塞根因不是数据缺失，
> 而是 native 侧的符号解析规则少了一条真端约定。另修一处顺序相关的交付门判定缺陷。

## 1. 阻塞排查：14 项"缺口"实为解析规则缺陷

| 步骤 | 事实 | 证据 |
|---|---|---|
| 初判（步骤 2） | 14 项未解：11 行门整体不可解 + 3 个部分缺口符号；归因写成"本服静态物品数据缺口" | `p3/simple-talk-unresolved-items.tsv`（旧版） |
| 排查 | 逐个符号在**本仓物品模板**中反查：`name_desc="item_exp_extraction_65a"`、`name_desc="item_idunderrune_quest_01"`、`name_desc="item_idruneweapon_quest_01"` **均已存在** | `src/main/resources/aion/data/static_data/items/item/item_template_*.xml` |
| 根因 | native 侧沿用了老链路 `resolveItemId` 的"无条件去 `ITEM_` 前缀"规则；而真端两类通道各有一套统一约定，前缀不是全局规则 | 全量复算（见 §2） |
| 真端侧确认 | 真端 `Items.xml` 的物品名同样并存两形：`<name>quest_1131a</name>`（182200506）与 `<name>item_idunderrune_quest_01</name>`（186000256） | `<真端根>/Map/XML/Items.xml`（584MB，UTF-16LE，流式扫描 128,163 行） |
| 真端运行期 | **不做名字解析**：`Quest_SimpleTalk.xml` 的 `ITEM_...` 符号在离线 codegen 期即解析为数值 id，运行期 thunk 只带立即数（例：1131 → cab520 `182200506`、cabb10 `182200507`/`182200506`） | `server58-source/MainServer_ScriptDLL64`（thunk 立即数）+ `P3-STEP2-REPORT` §1 逐字节对拍 |

## 2. 全量复算（独立脚本，非运行时代码自证）

工具：`p3/tools/simple_talk_item_face.py`（只读；输入 = 真端表入仓快照 + quest.xml + 物品模板）。

| 通道 | 符号形态（全量） | 计数 |
|---|---|---|
| SimpleTalk 表 `give_item` / `give_itemN` / `remove_itemN` | `ITEM_X` 形（去前缀后 = `name_desc`） | 663 个单元，**全部** strip-only 可解 |
| quest.xml `collect_item*` / `quest_work_item*` | 原名形（含 `item_*` 真名） | 3394 个单元，**全部** full-name-only 可解 |
| 合并去重 | — | 3145 个符号：full-only 2587 / strip-only 558 / **both 0 / neither 0** |

⇒ 规则：**先按原名（小写）查 `name_desc`，未命中再按去 `ITEM_` 前缀重查**。因两通道零重叠，
顺序无歧义；白名单归零。

复跑：

```
python3 .agents/summary/quest-engine-native/p3/tools/simple_talk_item_face.py \
  --out .agents/summary/quest-engine-native/p3/simple-talk-gate-recompute.tsv
```

真端来源对拍（同一脚本内置）：**3152/3152 行逐字段一致**（唯一差异是 `dev_name` 内的 CRLF→LF）。
`p3/simple-talk-gate-recompute.tsv` 为逐行门解析结果（RESOLVED / GATE_GAP / GATE_PARTIAL）。

## 3. 交付门（item_check）判定修正：从集合增量改为逐行

- **缺陷**：步骤 2 用「全局未解符号集合是否增长」判定该行门是否失败。同一未解符号在首行登记后，
  后续同形行不再触发增长 ⇒ 80669 判 fail-closed，而**同内容的 80670/80671/80672 反而放行**（顺序相关）。
- **修正**：门解析改为逐行独立收集（`gateUnresolved`），行级失败不再受其他行影响；
  行级事实由 `unresolvedGate(int)` 承载，不再混入符号证据面（`unresolvedItemSymbols()` 语义单一化）。
- 修正后（复算 = 运行期一致）：**1988 个 item_check 行中 1981 行门成立**，7 行门通道全缺。

## 4. 7 行"门通道全缺"的裁定（真端不可接取行）

| questId | 表行 | quest.xml | 裁定 |
|---|---|---|---|
| 2732 / 41571 / 50011 / 50012 / 51011 / 51012 | `item_check=1`，无 give/remove | `client_level=999` 且 `minlevel_permitted=999`，无 `collect_item`/`quest_work_item` | 真端不可接取 |
| 30509 | 同上 | `client_level=60`、`minlevel_permitted=999` | 真端不可接取 |

真端判据：`Quest::CanAcquireQuest` 对 `minlevel != 0 && playerLevel < minlevel` 一律拒绝
（`server58-source/NPCServer_NPCSvr64/classes/Quest/Quest.cpp`），`999` 不是"无限制"哨兵而是不可达值。
⇒ 这些行的报告门在真端不可达；native 侧统一 `unresolvedGate` fail-closed（**不可观测**），
并在族门中**逐行冻结**（`UNREACHABLE_GATE_ROWS`），数据一旦变化即红灯。

> 说明：行内 `item_check=1` 而无任何物品通道，本身不构成真端"门"（真端门只来自 quest.xml 收集通道）；
> native 的 fail-closed 属防漂移选择，不冒充真端语义。

## 5. 门态（本步实测，全部可复跑）

| 门 | 命令 | 结果 |
|---|---|---|
| tablelane 套件 | `mvn -o test -Dtest='com.aionemu.gameserver.questEngine.tablelane.*Test'` | **70/70 绿**（`SimpleTalkNativeFamilyGateTest` 8/8，新增门缺通道行集门） |
| 启动与派发 | `mvn -o test -Dtest='QuestProductionStartupGateTest,QuestEngineRuntimeCompositionTest,QuestEngineNpcDialogDispatchTest,QuestEngineEscortAndProximityRegistrationTest,RetailQuestDriverOverlayTest'` | **全绿** |
| 聚焦套件 | `mvn -o test '-Dtest=*Quest*Test,*Retail*Test'` | **1678 例 / 164F+226E**（与步骤 2 基线**逐类一致**：失败类集合 ADDED 0 / REMOVED 0）⇒ 本批零新增门禁债；日志 `gates/2026-10-01-focused-run-p3step3.log` |

新增/改写的族门断言：
1. `unresolvedItemSymbols()` **必须为空**（白名单归零，不再有冻结白名单）；
2. `1988` 个 item_check 行中 `1981` 行门成立；新解出样本逐项对拍
   （16921 = `item_idunderrune_quest_01 20` → 186000256×20；80669 = medal_07 1050 / junk_world_event_s4_quest_01a 1 / item_exp_extraction_65a 1）；
3. `gateLessRowsAreTheRetailUnreachableSet`：门缺通道行集 = 冻结 7 行，且逐行校验 quest.xml `client_level`/`minlevel_permitted` = 999。

## 6. 双主（double owner）收尾：SimpleHunt 3 行 + SimpleSerialHunt 泛化

排查同类缺陷时发现：SimpleTalk 之外的两个 native 家族**没有** `routes()` 分离（只有 `owns()`），
而 `retail-xml-retention.tsv` 里 SimpleHunt 仍有 **3 行 XML_RETENTION**（14112 / 14123 / 16961，
XML 定义在仓）⇒ 这 3 行同时被 native 注册与 XML 定义覆盖（双 owner），且击杀路径会由 native 相机推进
XML 车道正在使用的存档变量。

修正（与 SimpleTalk 同法，族级而非单点）：

| 面 | 修正 |
|---|---|
| `SimpleHuntHandler` | 新 `routes(int)` / `routedQuestIds()`（= 注册集 939 − 3 = **936**）；`installInterest` 三个注册面（接取/交付/击杀）全部按路由集过滤；`onKill` 逐 ref 过滤 |
| `SimpleSerialHuntHandler` | 同法：`routes(int)` / `routedQuestIds()`（16 行，当前与注册集相同但不再依赖"今天没有交叠"）；`installInterest` 四个注册面 + `onKill` 过滤 |
| `QuestEngine` | 三入口（`hasMatchingRoutes` / `onDialog` / 覆盖检查）由 `owns` 改 `routes` |

门禁：`SimpleHuntNativeFamilyGateTest` 增补单一 owner 不变量断言（注册集 = 路由集 + 3，且
14112/14123/16961 必须 `owns ∧ ¬routes`）；`SimpleTalkNativeFamilyGateTest` 增补路由集 3134 断言；
两族与族门回归全绿（tablelane 套件、启动派发门禁、`SimpleHuntHandlerTest`/`SimpleSerialHuntHandlerTest`/
`RetailSimpleHuntEquivalenceGateTest`/`QuestSimpleHuntRetailContractTest` 等）。

> 说明：`RetailQuestDriver` 的 owner-manifest 覆盖检查仍按注册集 `owns` 判定（覆盖语义而非路由语义），
> 与"谁路由"分离；若后续把它也切到 `routes`，须先确认 XML_RETENTION 行在 manifest 中的类别（现为 XML_RETENTION）。

## 7. 残余（不变）

1. 旧金标重锚 34 类 + 同批删死码（`compileSimpleTalk` / `RetailSimpleTalkTable` / `RetailSimpleTalkDefinitionCompiler`）。
2. `con_quest`（492 行链式接取窗）接线；cutscene 已接线（本批）。
3. 客户端验收 `PENDING_CLIENT`（建议 1131）。
