# SimpleTalk S3（γ 面：82 行纯 R 驱动链行）逐行形状普查


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 车道：`quest-native-dispatch`；面：**SimpleTalk S3 = γ 面**（G2 80 行 + G3 2 行）。
> 角色：**只读普查**（不落码、不改登记表、不改任何生产/测试文件）；本目录两个产物（本文 +
> `s3-gamma-rows.tsv`）即本片全部输出。
> 上游：`README.zh-CN.md`（下一面：γ 面）、`2026-09-27-s2-canonical-segments.zh-CN.md`（§1 切片边界、
> §2 策略 A、§6 未决）、`2026-09-27-s2-buildchain-spec.zh-CN.md`（§3.4 层 A/B1/B2、§9 guard/零残差）、
> `2026-09-27-s2-r-record-conflicts.zh-CN.md` + `s2-r-record-conflicts.tsv`（2235 条 R 记录全量分类）。

## 0. 输入指纹（复验用，只读）

| 输入 | 行数 | md5 前 12 | mtime |
|---|---|---|---|
| `src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv` | 5000（4990 数据 + 10 注） | `0ae2d9243e96` | 2026-09-26 22:48:34 |
| `src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml` | 3152 | `85d84d329fd2` | 2026-09-23 13:14:18 |
| `src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv` | 6226 | `24f8ca5d7950` | 2026-09-26 22:40:41 |
| `src/main/resources/aion/data/static_data/quest_retail/quest.xml` | 10036 quest 块 | `f3c901c7cb0b` | 2026-09-23 00:38:02 |
| `src/main/resources/aion/data/static_data/quest_retail/quest_client_dialog_exits.tsv` | 3942 | `32a4d5f9cf08` | 2026-09-26 10:31:31 |
| `src/main/resources/aion/data/static_data/quest_retail/quest_client_reward_npcs.tsv` | 12 数据行 | `d5ba1a5d9c34` | 2026-09-24 09:54:05 |
| `src/main/java/.../retail/RetailSimpleTalkDefinitionCompiler.java` | 1350+ | `afa06e48fe8d` | **2026-09-27 14:32:25（S2 已落地）** |
| `src/main/java/.../retail/RetailClientTalkChainSteps.java` | 179 | `8c0b827c9a6c` | 2026-09-26 10:44:35 |

产物自身：`s3-gamma-rows.tsv`（82 数据行 + 1 表头；本文 md5 以 `md5 -q` 现算为准）。

## 1. 口径与 82 行复算对账

**口径**：`S3 = RETAIL_TABLE ∩ family=SimpleTalk ∩ 有 talk_npcN（285 行） − S2 的 G1`
（G1 = 同时有 `NPC_START` 与 `NPC_REPORT` 块且 `acquired_npc_name` 非哨兵 ⇒ 203 行）。

复算结果（`python3 -B`，见 §7 复算脚本）：

| 组 | 定义 | 行数 | 对账 |
|---|---|---|---|
| **S2 全集** | `RETAIL_TABLE ∩ SimpleTalk ∩ ∃ talk_npcN` | **285** | 与 S2 台账一致 |
| **G1** | 有 START **且** 有 REPORT（非系统发放） | **203** | 与 S2 收口记录 `203/0/0` 一致 |
| **G2** | 无 `NPC_REPORT` 块 | **80** | 与 README 一致（80 行 id 集与 spec 附录 A 逐字相同） |
| **G3** | 有 REPORT 无 START | **2**（`2611` / `35017`） | 与 README 一致 |
| **S3 = G2 ∪ G3** | 本片分析面 | **82** | **对账通过**；id 落盘 `/tmp/s3-ids.txt`（升序 82 行） |

**与 S2 的重叠面**：19 行「同 285 行、有 START 无 REPORT 且接受段需退场记录」出现在 S2 §3.4 层 B1a 的
50 行清单内（本片 19 行 ⊂ S2 的 50 行）——S2 未动这 19 行（其 `canonicalSegments=false`），本片翻转时
这些记录随段退场，**S2 的门禁/指纹留痕不受影响**（82 行 S2 轮三次重算均逐字未动）。

## 2. 全局事实（82 行）

### 2.1 块分布 / 发放类别

| 类别 | 行数 | id |
|---|---|---|
| **系统发放**（`acquired_npc_name` = `_faction_`） | **14** | 35010, 35011, 35017, 35018, 35024, 35025, 35026, 45010, 45011, 45017, 45018, 45024, 45025, 45026 |
| **有 `NPC_START` 块（非系统发放）** | **61** | 其余 61 行（见 TSV `blocks` 列） |
| **零 B 块（接取段纯 R 驱动）** | **7** | 1323, 2611, 3001, 3023, 21136, 24202, 80320 |
| — 其中 35011 / 35026 **有** `NPC_START` 块但系统发放（块接取边被 `removeIf` 剔除） | 2 | 35011, 35026 |

- **`NPC_COMPLETE` 块覆盖 71/82 行**；**11 行无 COMPLETE 块**：`2611, 3001, 3023, 4970, 35011, 35025, 35026,
  45010, 45017, 45024, 80320`（其 `complete` 由 R 记录 `SELECTED_QUEST_REWARD1..NOREWARD` 承载或由完成流兜底）。
- **`NPC_REPORT` 块只存在于 G3 两行**：`2611`（204763，4 个 REPORT 块：started/step1/step2/step3 → reward）、
  `35017`（799806，s1 → reward，系统发放）。
- **多变体接取**：`2266` 有 **3 个 `NPC_START` 块**（203558 / 203655 / 203654），是本片唯一多接取块行。
- **奖励组**：82 行 `quest.xml` 奖励槽位全部 `maxSlot == 1` ⇒ 档位 0 ⇒ `deliveryWindowPage` = **窗 1（页 5）**，
  与现存的固定窗 1 逐字等价（**零奖励组行 = 0**，不会触发 QE-028 的兜底分支）。

### 2.2 交付段（`target == "reward"`）结构

**核心事实（本轮最重要的判据）**：

> **82 行里 `QUEST_SELECT(pre-reward 源 → reward)` 这条 canonical 落点边的现存实例 = 0 条。**
> 所有 `target==reward` 的 `QUEST_SELECT` 记录都是 **`reward→reward` 的领奖态重开窗**（源节点就是 reward），
> 不是交付落点边。⇒ γ 面的交付 canonical 化**全部是"合成边"**，没有一条是"把现成边改指向"。

按**翻面记录**（`target==reward ∧ source!=reward`）的归属分：

| 类别 | 行数 | 说明 |
|---|---|---|
| **A. 翻面在 reward NPC**（`npc_id ∈ 解析后的 reward NPC 集`） | **27** | 交付落点 NPC 正确 ⇒ canonicalDelivery 同构替换 |
| **B. A + `I` 记录** | **2** | `24202`（`SET_SUCCEED`）、`80320`（`SET_SUCCEED`）：无门直翻 + I 记录成对 |
| **C. 仅 `I` 记录**（无任何 R 翻面） | **1** | `1152`（39/20002 检查对是唯一 reward 入边） |
| **D. 翻面在非 reward NPC**（"中间人终局步"） | **50** | 见 §5（含 5 行同时有 A 类翻面的 mixed 行） |
| **E. 无 R 翻面但由 `NPC_REPORT` 块承载** | **2** | `2611`、`35017` ⇒ 同 S2 的块交付翻转，零迁移 |

**reward NPC 解析口径**（与生产一致，`buildChain:716-718`）：`reward_npc_name` 经 `RetailNpcNameIndex`
唯一解析优先；解析不出唯一值时回落 `RetailClientRewardNpcs`（客户端登记）。本片命中回落的是
`35024/35025/35026`（`LF4_GuardianOfDivine` → 799800,799801）与 `45024/45025/45026`
（`DF4_GuardianOfTower` → 799842,799843）**共 6 行**——**不做该回落会把 6 行误判为 D 类**。

**交付门形分布**（`delivery_gate_kind` 列）：

（TSV `delivery_gate_kind` 列的去重值与合计；一行多源/多门时按 `+` 组合计数，故值多于门形类。）

| 门形（列值） | 行数 | 备注 |
|---|---|---|
| `empty`（无门直翻） | **44** | = **A 类 11 + D 类 33**（另有 2 行 E 类以 `empty(块交付)` 单列） |
| `VAR_IS:var0=k`（阶段门） | **23** | 系统发放行的 `SELECT_QUEST_REWARD` 交付（12 行）+ D 类 `SETPRO2 … SET_VAR` 翻面（11 行） |
| `HAS_ITEM`（物品门） | **6** | 1118, 2953, 3218, 3966, 4218, 21455 |
| `HAS_ITEM+empty`（同 NPC 双路） | **2** | 4209, 21033 |
| `VAR_AT_LEAST+empty`（阶段门变体 + 无门） | **2** | 30711, 30761 |
| `I-record(HAS_ITEM)` | **1** | 1152（唯一入边） |
| `I-record(HAS_ITEM)+empty` | **2** | 24202, 80320（I 门 + 无门 `SET_SUCCEED`） |
| `empty(块交付)`（`NPC_REPORT` 块门） | **2** | 2611, 35017 |
| 合计 | **82** | — |

> **阶段门（`VAR_IS`/`VAR_AT_LEAST`）= 25 行**是 γ 面交付段的第一大形状难点：`canonicalDelivery`
> 现签名 `(rewardNpc, hasItems, removeItems, rewardWindowPage, collectSource)` 只收
> `List<QuestCondition>`（参数名叫 hasItems 但类型是通用条件）——阶段门**可以塞进去**，但语义上需要
> 一个具名重载/参数（否则调用点会写"把 var0 门当物品门"的误导代码）；若坚持不改签名则这 25 行
> 交付段不可规范。

**`I` 记录（`itemReportGate`）独占性判定（判例 1152/24202/80320）**：

| 行 | I 记录 | 该行 R 侧 reward 入边 | 独占? |
|---|---|---|---|
| `1152` | `203130 pepper→reward 169400112 ×1 remove=- fail=-`（失败页 SELECT6） | **无** | ✅ **独占**（迁移前交付无任何 R 记录承载） |
| `24202` | `205150 s2→reward 182215465 ×1 fail=CLOSE` | `SET_SUCCEED(205150) s2→reward`（无门） | ❌ 非独占（I 记录 + 无门直翻并存） |
| `80320` | `831427 s1→reward 182215303 ×12 fail=-`（SELECT6） | `SET_SUCCEED(831427) s1→reward`（无门） | ❌ 非独占（同上） |

**`E` 记录**：28 行带 `E`（`EnterWorld` 无源自愈边，`STATUS_IS:REWARD;VAR_IS:var0=k → SET_VAR:var0=m`）。
逐条核验：**`E` 记录全部是"领奖行修复"边（`source=null`，目标同为 `reward`），无一行是 reward 的
唯一入边** ⇒ 「交付段被 `E` 记录独占」这一形状在本片 **0 例**（任务书列举的判例实际全部落在 `I` 记录上）。
`C` 记录（`CanAct`）仅 1 行（`21111`，`ACTION_ITEM_USE` 交互物门，不产生 reward 入边）。

### 2.3 阶段段（中段记录，一律逐字保留）

- 中段记录总数 **483 条**（82 行），全部逐字保留（S2 已确立：中段页链/推进/交互物不 canonical 化）。
- 形态：`QUEST_SELECT sK→sK → SELECTn 页` + `SETPROn sK→sK+1`（或 `SET_VAR`/`REMOVE_ITEM`/`GIVE_ITEM` 载荷）
  + `SELECTn_1 sK→sK`（客户端续页按钮）+ 交互物 `USE_OBJECT`/`CanAct`。
- `USE_OBJECT` 共 3 种角色，**须分开计**：
  (a) **接取入口** 1 条（`1323`，`unaccepted→unaccepted`）；
  (b) **中段"点对象开对话"** 11 条 / 6 行（`1394`×2、`13809`×3、`23809`×3、`21111`、`30711`、`30761`）；
  (c) **领奖态重开窗** 13 行（`reward→reward`，KEEP 面）。
  三者都逐字保留（QE-082 判据 3：`dialogId=-1` = `CM_SHOW_DIALOG` 的开对话语义）。

## 3. 接取段逐行形状（82 行全覆盖）

### 3.1 三类形状（任务书 ①②③ 口径）

| 类 | 定义 | 行数 | id |
|---|---|---|---|
| **① 规范形样本**（已是 `QUEST_SELECT→页4` + 1002/20000 + 拒绝族 + `FINISH_DIALOG→页10` 的 R 记录） | R 记录里直接就是 canonical | **0** | — |
| **② 可翻形——块驱动**（有 `NPC_START` 块，接取流由块合成，现为 legacy `acceptFlowChain`） | 翻转 = 换 `canonicalAcceptFlow` | **61** | 见 TSV `accept_shape=BLOCK_*` |
| **② 可翻形——R 驱动**（无 `NPC_START` 块，接取流由 R 记录承载） | 需**合成** canonical 边 | **7** | 1323, 2611, 3001, 3023, 21136, 24202, 80320 |
| **③ 不可翻形** | — | **0** | — |
| **n/a——系统发放** | 无 NPC 接取段 | **14** | §2.1 清单 |

**①的近似样本（值得单独记）**：`35011` / `35026` 两行的 `NPC_START.extra` 第二段 `startPage`
已经是 **`SHOW_ASK_QUEST_ACCEPT_WINDOW`（页 4）**——即块合成的 `QUEST_SELECT` 落点本就是 canonical 页；
只因这两行是**系统发放**，其接取边在编译期被 `removeIf` 剔除，所以"已是规范形但不下发"。
⇒ 全 285 行的 `startPage` 取值统计：`SELECT1`（legacy）为主，**页 4 仅上述 2 行**。

### 3.2 块驱动 61 行的接取段细节（S2 策略 A 作用域复算）

| 子形 | 行数 | 说明 |
|---|---|---|
| `BLOCK_LADDER`（块 NPC 上零退场记录） | 14 | 1118, 2486, 2488, 3100, 4209, 4218, 4970, 11106, 19004, 21036, 21135, 21217, 21455, 29004 |
| `+RETIRE(SELECT1_1)` | **19 行 / 20 条** | 1152, 2486, 2538, 2914, **2953（2 条）**, 3037, 3041, 3087, 3093, 3218, 4218, 11010, 11103, 11106, 21004, 21068, 21071, 21106, 21111 |
| `+KEEP(QUEST_SELECT)`（块 NPC 上的中段开页/简报记录） | 33 行 | `started`/`reward` 源，非 `unaccepted` ⇒ 逐字保留 |
| `+KEEP(USE_OBJECT)`（领奖态重开窗预览） | 13 行 | 2963, 4971, 4972, 4973, 4974, 4976, 11070, 11105, 11117, 21106, 21111, 35011, 35026 |
| `+KEEP(SELECT_QUEST_REWARD)` | 9 行 | 2553, 2646, 3966, 21004, 21033, 21065, 28809, 35011, 35026 |
| `+KEEP(SELECT2_1 / SETPRO1)` | 3 行 | 2266, 30711, 30761 |
| `+KEEP(SELECT3_1 / SETPRO2)` | 1 行 | 21068 |
| `+KEEP(SELECTED_QUEST_REWARD1..NOREWARD)` | 2 行 | 35011, 35026 |
| `+SEL_OUT(s1,s2)` | **1** | **28809**（见 §6-裁定 ②） |
| `+multiStart=3` | **1** | 2266 |

（同一行可落入多个 KEEP 子项，故 KEEP 各行数之和 > 61；`RETIRE` 与 `KEEP` 互斥。）

**接取段载荷审计（全 82 行，`unaccepted` 源记录）**：条件只有 `START_ELIGIBLE`（54 条）；
动作只有 **2 条 `GIVE_ITEM`**（`1323` 的对象入口 `GIVE_ITEM:182201309:1`、`21136` 的 `QUEST_ACCEPT_1`
`GIVE_ITEM:182207919:1`）；after 全在标准词表内（`SHOW_ASK_QUEST_ACCEPT_WINDOW` 21 条 = 1007 中转、
`SELECT1_1` 21 条 = 续页梯、`SELECT_QUEST`(页10) 20 条、`CLOSE` 86 条）——**唯一越界 token**：
`DIALOG:SHOW_QUEST_PAGE:SELECT3_1` / `SELECT3_1_1` 各 1 条（**3023**）。
⇒ **接取段不存在"带非零载荷/非标准动作"级别的不可翻行**；③ 为空。

### 3.3 R 驱动 7 行的接取段逐条（需合成 canonical 边）

| 行 | 入口/形状 | 关键记录 | 判定 |
|---|---|---|---|
| `2611` | `QUEST_SELECT→SELECT1` + `ASK_QUEST_ACCEPT(1007)→页4` + ACC1 + ACCS + 拒绝族 3 + `SELECT1_1` + `FINISH_DIALOG→页10`（9 条） | 与 legacy `acceptFlow` + `acceptContinuation` **逐字同形** | **可翻**（合成 canonical，退场 9 条） |
| `24202` | 同上 + **`SETPRO1 unaccepted→started`**（判例 1914/1915/1916 同形，含 `START_ELIGIBLE`+`Sync(VISIBILITY_REFRESH)`+`Close`）+ `SELECT1_1`（10 条） | — | **可翻**（SETPRO1 提交边随段退场，语义由 canonical `20000` 边承担） |
| `80320` | 同上（9 条，无 SELECT1_1） | — | **可翻** |
| `3001` | `QUEST_SELECT→SELECT1` + 1007 + ACC1 + REFUSE_1/2 + `FINISH_DIALOG`（6 条；**无 ACCS/REFUSE_SIMPLE**） | 提交形只声明了 `QUEST_ACCEPT_1(1002)` | **可翻**（canonical 会**新增** `20000` 形 ⇒ 增量） |
| `3023` | 同上（8 条；**另有 `SELECT3_1` / `SELECT3_1_1` unaccepted 源页梯**） | 接取 NPC（798138）= 第 2 个 `talk_npc` | **可翻但要裁定**：`SELECT3_1/SELECT3_1_1` 不在 S2 退场词表内（见 §6-裁定 ④） |
| `21136` | `QUEST_SELECT→SELECT1` + 1007 + ACC1（**带 `GIVE_ITEM:182207919:1`**）+ 拒绝族 3 + `FINISH_DIALOG`（7 条） | 接取发物挂在提交边 | **可翻**（走 `canonicalAcceptFlow(..., acceptActions)` 三参重载，P0c-10m 通道，判例 1131） |
| `1323` | **`USE_OBJECT` 入口**（对象 730032 = `LF2_Lost_JewelBox`，带 `GIVE_ITEM:182201309:1` → SELECT1 页）+ 1007 + ACC1 + REFUSE_1 + `FINISH_DIALOG`（5 条，**无 QUEST_SELECT**） | 入口是"点宝箱"而非对话 | **可翻但要扩充**：现 `canonicalItemAcceptFlow` 以 **UseItem** 为触发；本行是 **USE_OBJECT** ⇒ 需对象形变体；且 `GIVE_ITEM` 现挂入口边（每次点击都发）⇒ 须裁定是否移到 1002/20000 提交边（见 §6-裁定 ③） |

### 3.4 接取段结论

- **accept_canonicalizable = yes 68 行 / n/a 14 行 / no 0 行**。
- **必须实装 S2 未实装的「层 A」（键覆盖过滤）**：对 R 驱动的 7 行，合成的 canonical 边与现存 R 记录
  **同键**（`(unaccepted, npc, 31/1002/20000/1003/1004/20001)`），若不实装层 A，`explicitRoutes` 会
  反过来把 canonical 边删掉 ⇒ **形状回退旧形而门禁全绿（假绿）**。S2 因"实测 0 条"未实装。
  独立复算（conflicts TSV 口径）：`class_gamma == A_filter_keep ∧ source == unaccepted` = **138 条 / 19 行**；
  落进 S3 的 = **同 19 行**（1323, 2611, 3001, 3023, 21136, 24202, 35010, 35017, 35018, 35024, 35025, 45010,
  45011, 45017, 45018, 45024, 45025, 45026, 80320）——即 **S3 的 7 个纯 R 接取行 + 12 个系统发放行**
  （后者接取边被 `removeIf` 剔除，层 A 对它们是空转，但**键冲突真实存在**、不可依赖 removeIf 的先后顺序）。
- 另一条同源风险：`2611` 的 `FINISH_DIALOG started→started`（R19）与 canonical 的
  `FINISH_DIALOG(started)` 同键 ⇒ 同键覆盖后 canonical 边消失，但 R19 形状与 canonical **逐字相同**
  （`ShowQuestSelectionDialog(页10)`），无行为差；**仍须由层 A 统一处置并在守卫里登记**。

## 4. 交付段结论与四桶

### 4.1 四桶（按 `accept × delivery` 的 是/否 判定）

| 桶 | 行数 | id 清单 |
|---|---|---|
| **两段可翻** | **32** | 1118, 1152, 1323, 1394, 2553, 2611, 2646, 3001, 3023, 3218, 3966, 4218, 21004, 21065, 24202, 28809, 35010, 35011, 35017, 35018, 35024, 35025, 35026, 45010, 45011, 45017, 45018, 45024, 45025, 45026, 80320, 80752 |
| **仅接取可翻** | **50** | 1163, 1484, 1851, 2266, 2271, 2480, 2486, 2488, 2538, 2663, 2914, 2953, 2954, 2963, 3037, 3041, 3087, 3093, 3100, 4052, 4209, 4970, 4971, 4972, 4973, 4974, 4976, 11010, 11070, 11103, 11105, 11106, 11117, 11460, 13809, 19004, 21033, 21036, 21068, 21071, 21106, 21111, 21135, 21136, 21217, 21455, 23809, 29004, 30711, 30761 |
| **仅交付可翻** | **0** | —（接取段在 68 行全部可翻、14 行 n/a） |
| **都不可翻** | **0** | — |
| （子注）32 行中 **14 行接取段为 n/a（系统发放）** | — | 35010, 35011, 35017, 35018, 35024, 35025, 35026, 45010, 45011, 45017, 45018, 45024, 45025, 45026 |

### 4.2 「两段可翻」32 行的交付细节

- **A 类 27 行（翻面在 reward NPC）**：canonical 落点边 = `canonicalDelivery(rewardNpc, gate, 窗1, source)`，
  `source` 取翻面记录的源节点（多为 `started`/`s2`/`v1`/`v3`）。
  其中 **22 行的翻面已是 `SELECT_QUEST_REWARD(1009)` 且 `after` 已含窗 1**（1118, 1323, 2553, 2646, 3218, 3966,
  4218, 21004, 21065, 35010, 35011, 35018, 35024, 35025, 35026, 45010, 45011, 45017, 45018, 45024, 45025, 45026）
  ⇒ 与 canonical 边**逐字同形**，只差"动作 id 31 vs 1009"与"是否删掉其前的报告页记录"。
- **6 行的翻面 `after` 不带窗**（`3001`/`3023` = 回任务列表页；`24202`/`80320`/`28809`/`80752` = `CLOSE`）：
  规范边会把 `after` 换成 `Sync(LVR)+窗1` ⇒ **交付即开分档奖励窗**（这是 canonical 的既定语义，
  与 Goal 一致；但属可观测行为变化，须在门禁里锁定）。
- **翻面动作非 1009 的 4 行**：`1394`（`SETPRO1`，另有 1009 同源）、`24202`/`80320`（`SET_SUCCEED(10255)`）、
  `80752`（`SETPRO1`）。
  ⇒ canonical 化把动作统一为 `QUEST_SELECT(31)` 并把原动作的载荷并入规范边 `actions`。
- **同 NPC 多源翻面 6 行**（`1394`、`35011`、`35026`、`35024`、`45024`、`45025`… 见 TSV `delivery_basis`）：
  逐源各合成一条规范交付边（多条同 `(source, npc)` 但条件不同的 `QUEST_SELECT` ⇒ 需**显式 priority**
  或按门互斥，实现时须确认 IR 不判 `AMBIGUOUS_TRANSITION`）。

### 4.3 口径差异：`s2-r-record-conflicts.tsv` 的 γ 交付规则在本片会退化（实现前必读）

`2026-09-27-s2-r-record-conflicts.zh-CN.md` §3 的 γ 口径把"为 R 驱动交付段合成 canonical 边"形式化为
**「交付段取 `target=reward` 的 `31/1009` 边的 `(source,npc)`」**。独立复算（直接读 TSV）：

| 判据 | 条数 | 行数 | 落进 S3 |
|---|---|---|---|
| `class_gamma == A_filter_keep ∧ target == reward` | 19 | **19** | 19 行（1163, 1484, 1851, 2480, 2538, 2953, 3093, 3100, 4052, 11010, 11103, 13809, 21036, 21071, 21217, 21455, 23809, 28809, 80752） |
| `class_gamma == A_filter_keep ∧ source == unaccepted` | 138 | 19 | 19 行（见 §3.4） |
| `target==reward ∧ source!=reward ∧ action ∈ {31,1009}`（直接读登记表） | — | **31** | 31 行 |

**本片发现的口径退化**：对 **D 类（`SETPROn` 翻面）** 行，`target=reward` 的记录里**没有** `31/1009` 动作
（翻面动作是 `SETPRO1/2/3`），于是 γ 规则只能落到**领奖态重开记录 `QUEST_SELECT reward→reward`** 上，
合成出 **`(reward, rewardNpc, 31)` 的自环键**——例如 `1163` 的 γ 键 = `reward:203155:31`
（其真实交付翻面是 `SETPRO1@203151 started→reward`）。⇒ **γ 的交付键集不能直接当作 S3 的规范交付落点键集**；
本普查的 `delivery_basis` 列（按"翻面记录的 npc 是否 == 解析后的 reward NPC"判定）才是可执行的判据。

顺带修正：`s2-r-record-conflicts.tsv` 里 `A_filter_keep` 的 `hit_keep_block_edges` 字符串
（如 `v1:203079:31[DELIVER:canon(synth)]`）**确实是** canonical 交付边的键（`source` 取翻面记录的源节点、
`npc` 取解析后的 reward NPC）——A 类行的**键冲突命中的是"报告页记录"**（`QUEST_SELECT v1→v1`），
即 canonical 化要退场的那条；D 类行的命中才是上面的自环退化。两类共用同一列名，**须按行区分**。

## 5. 不可翻清单（50 行，交付段）

**统一阻断原因**：**交付翻面不在 reward NPC**——`target==reward` 的翻面记录挂在链上的**中间 NPC**，
其动作**全部是 `SETPROn`（10000/10001/10002）**（无一条 1009）。按 S1/S2 同形的"reward-NPC 落点"
合成 canonical 边，会**搬走该中间步的终端推进**：该中间 NPC 的页链（`QUEST_SELECT sK→sK → SELECTn 页`
+ `SELECTn_1` 续页按钮）在 `SELECTn` 页上只剩"继续"按钮而**失去原本推进/翻面的那个按钮的目标**。

**形状细分（4 子类）**：

| 子类 | 行数 | 翻面 NPC | 门形 | 载荷 | id 清单 |
|---|---|---|---|---|---|
| **D1 纯中间人 / 无门** | **33** | ∈ `talk_npcN` | `empty` | 无 | 1163, 1484, 1851, 2480, 2486, 2488, 2538, 2963, 3093, 3100, 4052, 4970, 4971, 4972, 4973, 4974, 4976, 11010, 11070, 11103, 11105, 11106, 11117, 13809, 19004, 21036, 21068, 21071, 21106, 21111, 21135, 23809, 29004 |
| **D2 纯中间人 / 带门** | **12** | ∈ `talk_npcN` | `VAR_IS:var0=0/1` 或 `HAS_ITEM` | 无 或 `GIVE_ITEM`/`REMOVE_ITEM`/`SET_VAR` | 2266, 2271, 2663, 2914, 2953, 2954, 3037, 3041, 3087, 11460, 21136, 21217 |
| **D3 双路 / 无门** | **4** | `4209`@805843（`talk_npc1`）、`21033`@204734（`talk_npc1`）；**`30711`/`30761`@730701 = `IDTiamat_FOBJ_Dagger_1` = 接取"NPC"（物体）**（本片唯一 2 行"翻面 NPC 不在 `talk_npcN`"） | `empty` | `SET_VAR` | 4209, 21033, 30711, 30761 |
| **D4 双路 / 带门** | **1** | 中间 NPC 799240（`Schiemann`） | `HAS_ITEM:182209514` | `GIVE_ITEM:182209515;REMOVE_ITEM:182209514` | 21455 |

**D3/D4 的额外复杂度（mixed）**：这 5 行在 **reward NPC 侧同时存在一条 `SELECT_QUEST_REWARD(1009)+HAS_ITEM`
翻面**（`4209`→798331、`21033`→799256、`21455`→799244、`30711`→804870、`30761`→804871）。
两条翻面**同源节点、不同门**（如 21455：中间 NPC 门 = 提交物 `182209514`，reward NPC 门 = 成品 `182209515`）
⇒ canonical 化必须裁定**保留哪一条 / 是否合并成"两条同键不同门的规范边"**；只保留 reward 侧会**丢掉
中间步的换物语义**（成品拿不到 ⇒ 门不可满足 ⇒ 死任务）。

**两行特殊（`30711`/`30761`）**：reward NPC 侧的翻面**已经是对象形**
（`USE_OBJECT(804870) started→reward cond=VAR_AT_LEAST:var0=1 act=SET_VAR:var0=2 after=…;窗1`），
即"用对象开交付窗"——与 SimpleItemPlay 的 `useObject` 交付同构；唯一阻断是该行**接取 NPC（730701）
上另有一条无门 `SETPRO1 started→reward`**。

## 6. 实现前必须裁定的点（≤5）

1. **交付落点的两候选形（决定 50 行归属）**——D1–D4 的中间人翻面，二选一：
   **(i) 搬到 reward NPC**：`canonicalDelivery(rewardNpc, gate, 窗1, 中间步的源节点)`，
   代价 = 该中间 NPC 的 `SETPROn` 终端按钮与其页链（`SELECTn`/`SELECTn_1`）失效（需一并清理或接受死按钮）；
   **(ii) 留在原 NPC**：`canonicalDelivery(中间NPC, gate, 窗1, 同源节点)`，即**把 `SETPROn` 换成 `QUEST_SELECT(31)`
   并追加"开档位窗"**——保持零售的"最后一个中间人收官"语义，但改变了页面动作（需客户端侧证据
   证明该 NPC 在 `sK` 行的页面上有 31 动作按钮，或接受"对话即推进"的服务端语义）。
   ⚠ 不裁定则 50 行交付段无法动工（本片四桶中"仅接取可翻"整桶的成因）。
2. **28809 的 `selectionSources` 越界**：`START` 块声明 `unaccepted s0 s1 s2`，而 `assertCanonicalSelectionSources`
   只允许 `{unaccepted, target}` ⇒ 直接 throw。S2 §2.4 已就该行给出"丢 `s1`/`s2` 两条 `FINISH_DIALOG`、
   关窗兜底交 DialogService"的裁定，但**该裁定当时属口径 A 的预期清单、未落地**（该行在 S2 范围外）；
   本片需确认沿用并把守卫例外**登记在案**（否则 28809 编译失败）。
3. **1323 的对象形接取 + 21136/3001/3023 的载荷/缺形**：
   (a) `USE_OBJECT` 触发未有无主 canonical 变体（现只有 `UseItem` 版）⇒ 新增 or 拒绝该行；
   (b) 1323 的 `GIVE_ITEM` 现挂**入口边**（每次点宝箱都发）⇒ 移到 1002/20000 提交边会不会改语义（可否重复领取）；
   (c) `3001`/`3023`/`21136` 缺 `20000`/`20001` 形：canonical 会**新增**该提交/拒绝按钮 ⇒ 属增量还是越界。
4. **3023 的 `SELECT3_1` / `SELECT3_1_1`（unaccepted 源）页梯记录**：不在 S2 的 `CHAIN_ACCEPT_RETIRED`/
   `CHAIN_RETIRED_PAGES` 词表内，canonical 接取形也不再产生它们；须裁定**退场**（词表扩容）还是**保留**
   （保留则为无主残留边，零残差守卫需登记例外）。
5. **层 A 实装范围 + 阶段门重载 + 报告页退场**：
   (a) 层 A（R 记录键 ∈ canonical 边键 ⇒ R 退场，方向反转）S2 因"实测 0 条"未实装；γ 面真实命中
   = 接取侧 **138 条 / 19 行**、交付侧 **19 条 / 19 行**（§4.3）⇒ **必须实装**，否则合成边被同键 R 覆盖（假绿）；
   (b) **报告页记录必须与 1009 记录成对退场**：A 类行的 `QUEST_SELECT(src→src → SELECT5)` 与
   `SELECT_QUEST_REWARD(src→reward)` 是**两条**记录（前者键冲突、后者无键冲突），只做层 A 会漏掉后者
   ⇒ 交付段仍残留 1009 中转；须按"段作用域 + 动作"过滤（S2 已确立的 `CHAIN_DELIVERY_RETIRED` 口径）；
   (c) `VAR_IS`/`VAR_AT_LEAST` 阶段门 **25 行**：`canonicalDelivery` 是否新增"额外条件"参数（具名重载）
   或允许把阶段门塞进 `hasItems` 形参（后者会写出误导性调用点）。

## 7. 逐行产物与复算

- **逐行事实源**：`s3-gamma-rows.tsv`（11 列 × 82 行）——
  `quest_id / blocks / accept_shape / accept_canonicalizable / accept_basis / stage_summary /
  delivery_records / delivery_gate_kind / delivery_canonicalizable / delivery_basis / blocker`。
  （列集在任务书 9 列基础上**增补 `accept_basis` / `delivery_basis`**，承载"一句依据"。）
- **复算（只读，`python3 -B`，无中间产物）**：

```bash
cd <仓库根>
# ① 82 行复算（S2 285 − G1 203）
python3 -B - <<'PY'
import re,collections as C
RET='src/main/resources/aion/data/static_data/quest_retail/'
own={};fam={}
for ln in open(RET+'retail-xml-retention.tsv',encoding='utf-8'):
    p=ln.rstrip('\n').split('\t')
    if len(p)>=5 and not ln.startswith('#'): own[int(p[0])]=p[1]; fam[int(p[0])]=p[2]
s=open(RET+'Quest_SimpleTalk.xml',encoding='utf-8').read()
x={int(b.split('"')[0]):{k:v.strip() for k,v in re.findall(r'<(\w+)>([^<]*)</\1>',b)}
   for b in re.split(r'<id\s+id="',s[s.index('<quest_simpletalks>'):])[1:]}
S2=[q for q,d in x.items() if fam.get(q)=='SimpleTalk' and own.get(q)=='RETAIL_TABLE'
    and any(k.startswith('talk_npc') and v for k,v in d.items())]
recs=C.defaultdict(list)
for ln in open(RET+'quest_client_talk_chain_steps.tsv',encoding='utf-8'):
    if ln.startswith('#') or not ln.strip(): continue
    p=ln.rstrip('\n').split('\t'); recs[int(p[0])].append(p)
G1=[q for q in S2 if {p[2] for p in recs[q] if p[1]=='B'} >= {'NPC_START','NPC_REPORT'}]
print('S2',len(S2),'G1',len(G1),'S3',len(S2)-len(G1))     # 285 203 82
PY
# ② 交付翻面归属（A/B/C/D/E 类）：见 §2.2；reward NPC 解析 = 名索引唯一解析优先，
#    非唯一时回落 quest_client_reward_npcs.tsv（命中 6 行：35024-35026 / 45024-45026）
#    名索引 = 读 src/main/resources/aion/data/static_data/npcs/npc_template_*.xml 的
#    name_desc → npc_id（含去 NPC_ 前缀别名）
# ③ 接取段载荷词表：unaccepted 源记录的 cond/act/after 全域统计（§3.2 数字）
# ④ 「报告页 + 1009 成对」判据（§6-5b）：31 行有 pre-reward 1009 翻面，
#    31/31 都在同源节点上有 QUEST_SELECT src→src 报告页记录（0 例外）
# ⑤ 门形去重计数（§2.2 表）：按 TSV delivery_gate_kind 列聚合，82 == 82
```

## 8. 纪律回执

- 只读普查：**未改任何生产/测试/TSV 文件**（`retail-xml-retention.tsv`、`quest_client_*.tsv`、
  `Quest_SimpleTalk.xml`、`quest.xml` 一字节未改）；未跑 Maven / 未编译 / 未跑测试；未启停服务；未 commit。
- 临时脚本与中间产物全在 `/tmp`（`/tmp/s3-ids.txt`、`/tmp/s3-rows.json`、`/tmp/npc-names.json`、
  `/tmp/s3-analysis.txt`、`/tmp/s3-facts.txt`、`/tmp/s3-flip2.txt`、`/tmp/s3-detail.tsv`），仓库内零新增脚本。
- 产物仅两个：本文件 + `s3-gamma-rows.tsv`。
