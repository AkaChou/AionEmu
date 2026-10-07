# itemUseArea（使用道具）族步号轴全族审计报告

- 日期：2026-10-07
- 触发：1361/1373 取水步骤空白修复（`a279fe6da`）后的「排查类似问题」全库审计
- 范围：客户端脚本表 `quest_script_monster.csv` 的 **itemUseArea 族 96 个任务**（`<客户端解包根>` 的 Quest_unpacked 产物）
- 状态：**AUDIT_COMPLETE / REPAIR_APPLIED_PENDING_CLIENT_ACCEPTANCE**（5 个真错已修复 + 锁定测试通过，见 §7；实机验收待用户冷重启）

---

## 1. 判据与方法

沿用 1361/1373 已验证的三源口径（QE-054 / `STEP_AXIS_RETAIL_SETPROGRESS_AUTHORITY`）：

1. **真端**（`<真端根>/server58/MainServer_ScriptDLL64/ScriptDLL64.c`）：
   - `SetProgress(questId, 值)` 实际推进值（含 4 参数变体 `(obj, quest, 值, 0)`）；
   - `0x100(quest, 0, 0)` = 状态推进（不写轴，轴保持）；
   - `0x110(quest, from, to, ...)` = 显式轴推进（如 4338 的 4→5）；
   - handler 函数体中的 `0xd0` 读取守卫（`status==3 && step==N`）确定触发轴的上下文。
2. **legacy handler**（迁移前 Java 实现，`git show <迁移提交>^:<原路径>`）：
   - `setQuestVar(N)` / `setQuestVarById(0, v+1)` / `useQuestItem(env, item, from, to, reward, ...)` / `useQuestObject` / `checkQuestItems` / `defaultCloseDialog(env, from, to)`；
   - 旧引擎 reward 分支**不写 nextStep** ⇒ `useQuestItem(..., from, to, reward=true, ...)` 落盘 **from**（1373 `checkQuestItems(2,3,true)` 落盘 2 实证）。
3. **客户端任务书行**（`[%N]` 可见槽，N=3×行索引；门槛值 = 该行显示时的轴值）：
   - CHS 覆盖页：`<客户端目录>/L10N/CHS/Data/data.pak`（zip，`aionpak.core.decrypt_aion_html_blob`）；
   - KR 基础页：`<客户端目录>/data/Dialogs/<区间>/<区间>.pak`（aion_pak 解析）——行结构与 CHS 相同。

审计脚本：`audit_itemusearea_step_axis.py`（本目录）；输出 `itemusearea-step-axis-scan.tsv` / `itemusearea-step-axis-mismatch.tsv`。

### 1.1 脚本判据的一句话

每个 `item-play` / `use-item` transition 的目标节点 `var0` 应落在该任务真端 `SetProgress` 常量值集合内。**MISMATCH 只是候选**，必须逐任务人工复核（本次 6 个候选全部完成复核）。

## 2. 扫描统计（96 任务）

| 判定 | 数量 | 说明 |
|---|---|---|
| MATCH | 10 | 1006, 1361, 1373, 1573, 2533, 3200, 4200, 14031, 24031, 24154 |
| **MISMATCH（复核后 5 真错 + 1 误报）** | 6 | 1345, 4338, 11006, 24013*, 24021, 24052 |
| NO_ITEMPLAY | 31 | 有 XML 但无使用道具 transition（推进用其它事件表达，未细查） |
| NO_RETAIL_SETPROGRESS | 3 | 2393, 3722, 4722（真端无常量推进，可能对话/计数型，未细查） |
| NO_XML | 46 | 生产目录无该任务定义（未迁移或 ID 不匹配，未细查） |

\* 24013 复核为误报（见 §3.6）。

**24154 曾因正则漏检四参数变体 `0xf0(plVar3,0x5e5a,4,0)` 误报**，修正后归入 MATCH（真端显式推进 4 = XML 目标）。

## 3. 六个候选的定性与证据

### 3.0 总表

| quest | 客户端行数（末行索引） | XML 现状 | 权威值（legacy+真端） | 判定 | 归因提交 |
|---|---|---|---|---|---|
| 1345 | 3（2） | use-item→reward(2)，reward=2，无自愈边 | **1** | **真错** | 0ab7e4db8 起（08-12，迁移期） |
| 4338 | 7（6） | use-item s5→s6(6)，s6→reward(set 7)，reward=7，无自愈边 | **5** | **真错** | caf547879（09-17） |
| 11006 | 4（3） | use-item v2→reward(3)，reward=3，自愈 `REWARD/2→3` | **2** | **真错** | 7a7d27809（09-21） |
| 24013 | 5 | use-item s2→s3，kill 3→7，reward=7 | 3（计数基值） | 误报 | — |
| 24021 | 6（5） | use-item s4→reward(5)，reward=5，自愈 `REWARD/4→5` | **4** | **真错** | 7a7d27809（09-21） |
| 24052 | 6（5） | use-item 递进✓，reward=5，自愈 `REWARD/4→5` | **4** | **真错**（仅领奖投影） | 7a7d27809（09-21） |

### 3.1 quest 1345（Bearer Of Bad News，天界 Eltnen）

- **流程**：Demokritos 接取 → Kreon 对话（0→1）→ 喷泉用戒指 182201320（use-item）→ Demokritos 对话领奖。
- **legacy** `_1345BearerOfBadNews`：Kreon `STEP_TO_1 → defaultCloseDialog(env, 0, 1)`（0→1）；扔戒指 `useQuestItem(env, item, 1, 2, true, 0, 0, 0)`（从 1，reward=true）→ **落盘 from=1**。
- **真端**：Kreon 对话 handler `FUN_180f7d810`（页 1352/1353 + 选择 10000）→ `SetProgress(0x541, 1)`；扔戒指 handler `FUN_180f03fc0`（事件 0xadc2be8）守卫 `status==START && step==1` → `0x100` 推进（**轴保持 1**，进 REWARD）。
- **客户端**：3 行（和 Kreon 对话 / 扔戒指 / 和 Demokritos 对话）——轴 1 时显示 row1，REWARD@1 显示 row2。
- **XML 现状**：`v1(1) -> reward`（use-item）目标 reward 节点 var0=2；reward 投影 2；**无自愈边**。
- **偏差**：扔戒指后轴应 1，XML 写 2（= 末行索引）→ 客户端行 0/1/2 的门槛（0/1/1 或 0/1/2）与轴 2 不匹配即空白，且 REWARD 档被写成 2。

### 3.2 quest 4338（Gundalpun / 姆格尔引导，魔族）

- **流程**：接取 → 搜 4338a×3 交给 Gundalpun（→2）→ 对话（→3，去见 Kimci）→ KimciFriends 对话选姆格尔（显式 4→5）→ **把 4338b 扔进岩浆（use-item，step==5）→ REWARD@5** → 向 Gundalpun 报告领奖。
- **真端**：
  - `FUN_180ee78b0` 无条件 `SetProgress(0x10f2, 2)`；`FUN_180fa4c80` 选择 0x2712 → `SetProgress(0x10f2, 3)`；
  - `FUN_180fb8390`/`FUN_180fb84d0` 选择 0x2714 → `SetProgress(0x10f2, 5)` + **`0x110(0x10f2, 4, 5, ...)`**（显式 4→5）；
  - 扔岩浆 handler `FUN_180f00820`（事件 0xadc629e）守卫 `status==START && step==5` → `0x100`（**轴保持 5**）= REWARD。
- **客户端**：7 行（row5=扔岩浆 [%15]，row6=报告 [%18]）；槽位注册 `FUN_180cb3070(_, _, 0x10f2, 3, 值, 0)` 值序列 0/1/2（Gundalpun）、3（Kimci）、4（KimciFriend×5）与轴序列自洽。
- **XML 现状**：`s5 -> s6`（use-item）`set var0=6`；`s6 -> reward`（dialog）`set var0=7`；reward 投影 7；**无自愈边**。
- **偏差**：扔岩浆后轴应 5（REWARD），XML 推到 s6(6) 并要再对话才 reward(7)——两处推进全错。无 legacy 对照（AionEmu 从未实现此任务）；XML 于 2026-09-17 `caf547879` promote 时写成"末行/末行+1"形态。

### 3.3 quest 11006（Testing The Waters，天界 Inggison）

- **流程**：Clodia 接取（给道具A 182206704）→ **装水A（use-item 0→1）** → Clodia 对话（STEP_TO_2，1→2，给道具B 182206705）→ **装水B（use-item）→ REWARD@2** → 领奖。
- **legacy** `_11006TestingTheWaters`：道具A `useQuestItem(env, item, 0, 1, false, ...)`（0→1）；对话 `defaultCloseDialog(env, 1, 2)`；道具B `useQuestItem(env, item, 2, 2, true, ...)`（reward=true）→ **落盘 2**。
- **真端**：道具A handler `FUN_180f00a50` → `SetProgress(0x2afe, 1)` ✓；道具B handler `FUN_180f03f20` 守卫 `status==START && step==2` → `0x100`（轴保持 2）。
- **客户端**：4 行（装水A / 对话 / 装水B / 对话领奖）——末行索引 3。
- **XML 现状**：`started -> v1`（道具A，→1 ✓）；`v1 -> v2`（对话 ✓）；`v2 -> reward`（道具B）目标 reward var0=**3** ✗；reward 投影 **3** ✗；自愈边 `REWARD/2 → 3` ✗（**方向反了**：会把 legacy/真端正确落盘的 REWARD/2 存档改坏）。
- **偏差**：道具B 后应 REWARD@2；XML 把 reward 抬到末行索引 3 且自愈边方向相反。
- **CONTRACTS 佐证**：`JournalRewardRowRepairContractTest` 名单 `new Contract(11006, 3, 2)` —— rewardRow=3（错值）、**staleRow=2（= 权威值）**。

### 3.4 quest 24021（Ghosts In The Desert，龙界 DF2）

- **流程**：Bragi（0→1）→ Tofa（1→2）→ 收集 24021a/b 交 Tuata_E（2→3）→ 听从指示（3→4，给 182215363）→ **在某处撒 24021c（use-item，区域 DF2_ITEMUSEAREA_Q2032）→ REWARD@4** → Tofa 领奖。
- **legacy** `_24021Ghosts_In_The_Desert`：`STEP_TO_4 → defaultCloseDialog(env, 3, 4, 182215363, 1, ...)`；道具 `useQuestItem(env, item, 4, 4, true, 88)`（reward=true）→ **落盘 4**。
- **真端**：选择 10003（STEP_TO_4）`FUN_180faea90` → `SetProgress(0x5dd5, 4)`；道具 handler `FUN_180f00500`（事件 0xadc62c3）→ `0x100`（轴保持 4）。
- **客户端**：6 行（Bragi / Tofa / 收集交 Tuata / 听指示 / 撒道具 [%12] / 向 Tofa 报告 [%15]）——末行索引 5。
- **XML 现状**：前段递进 ✓（0→1→2→3→4 全部正确）；`s4 -> reward`（use-item）→ reward var0=**5** ✗；reward 投影 **5** ✗；自愈边 `REWARD/4 → 5` ✗（**方向反了**）。
- **偏差**：撒道具后应 REWARD@4；XML 抬到末行索引 5，且自愈边会把正确档（REWARD/4）改坏。
- **CONTRACTS 佐证**：`new Contract(24021, 5, 4)` —— rewardRow=5（错值）、**staleRow=4（= 权威值）**。

### 3.5 quest 24052（A Frozen City，龙界 DF3 使命）

- **流程**：Kistenian 接取（0→1，给 3 个道具）→ 依次使用 24052a/b/c（1→2→3→4，任意顺序各推一步）→ 召唤怪 233864 击杀（var==4）→ **REWARD@4** → Kistenian 领奖；计时器超时 var==4 → 0。
- **legacy** `_24052A_Frozen_City`：`onItemUseEvent` var 1/2/3 各 `setQuestVarById(0, var+1)`（1→2→3→4）；`defaultOnKillEvent(env, 233864, 4, true)` → REWARD（**落盘 4**）；`onQuestTimerEndEvent` var==4 → 0。
- **真端**：`SetProgress(0x5df4, 0, 0)`（4 参数、值 0，语义待解，与 legacy 计数模型不冲突）；无与 1→2→3→4 冲突的常量推进。
- **客户端**：6 行（对话 / a / b / c / 击杀 [%12] / 报告 [%15]）——末行索引 5。
- **XML 现状**：道具递进 ✓（3×3 条 use-item 与 legacy 等价：任意道具在当前阶段都推一步）；`s4 -> reward`（kill-npc）✓ 路径正确；reward 投影 **5** ✗；自愈边 `REWARD/4 → 5` ✗（**方向反了**）。
- **偏差**：只有领奖投影被抬到末行索引 5；击杀路径本身正确。

### 3.6 quest 24013（Poison In The Waters，魔界 DF1A）——**误报**

- legacy 与 XML 逐字一致：`useQuestItem(env, item, 2, 3, false)`（2→3）＋ `onKillEvent` var 3..6 每杀 +1、var==7 → REWARD；XML `s2->s3`（use-item）、`s3..s6` kill +1、`s7 -> reward`（var0=7）完全同构。
- 真端 `SetProgress(0x5dcd, uVar2+1, 0)` 为**动态击杀计数**，与常量集合比较天然 MISMATCH——属脚本判据的族外形态（计数任务），无缺陷。
- 客户端 5 行（对话/对话/用 24013a/击杀 [%11]/5/报告），与计数车道自洽。

## 4. 错误归因

| 提交 | 日期 | 受影响 | 形态 |
|---|---|---|---|
| 0ab7e4db8（compact XML migration）起 | 2026-08-12 | 1345 | 迁移期把 reward 写成末行索引 2（legacy=1）；2ac2c52b5 旧格式时已引入 |
| caf547879（promote 4338/25082/25608） | 2026-09-17 | 4338 | promote 时 use-item 推进写 s6(6)、reward 写 7，均非真端值 |
| **7a7d27809（领奖行批次 1-7，375 任务）** | 2026-09-21 | **11006, 24021, 24052** | reward 投影"收口"到客户端末行索引；自愈边 `REWARD/(权威值) → (末行索引)` **方向反了，会把正确存档改坏** |

- **1361/1373 与 11006/24021/24052 同批**（7a7d27809 / 7d5bb5317），本次是同一错误族在全库的继续暴露。
- `JournalRewardRowRepairContractTest` 的 CONTRACTS 名单以 `(questId, 末行索引, 旧值)` 记录该批改动——**旧值列在多行上就是权威值**（1361 `(2,1)`、11006 `(3,2)`、24021 `(5,4)` 三例已逐一对上 legacy/真端），是高价值复核线索（名单 200+ 行，未逐行核对）。

## 5. 修复（2026-10-07 已执行，待实机验收）

统一口径：**取水/使用道具族的领奖投影与使用后轴 = legacy/真端权威值**；自愈边方向 = 把被错误批次写坏的存档（末行索引值）**回滚**到权威值。

| quest | reward 节点 var0 | reward 路由 | 自愈边（enter-world） | 其它 |
|---|---|---|---|---|
| 1345 | 2 → **1** | `v1 -> reward`（use-item）不变 | **新增** `REWARD && var0==2 → set 1`（LEVEL_AND_VISIBILITY_REFRESH） | complete 投影保持现状（各任务无统一惯例，1361/1373 先例不动） |
| 4338 | 7 → **5** | use-item 改为 `s5 -> reward`（真端扔岩浆即 REWARD@5） | **新增两条**：`REWARD && var0==7 → set 5`（领奖态）、`START && var0==6 → set 5`（扔过项链的中间态回 s5，PACKET_ONLY） | **s6 节点与其对话全部删除**——编译器 `UNREACHABLE_NODE` 校验拒绝不可达节点，旧档改由 `START/6` 自愈边归一；REWARD 态领奖对话由既有 `reward -> reward | QUEST_SELECT → SHOW_SELECT_QUEST_REWARD_WINDOW1` 承担 |
| 11006 | 3 → **2** | `v2 -> reward`（use-item）目标节点不变（节点改后自洽） | 现存 `REWARD/2→3` 改为 `REWARD/3→2`（条件 3，动作 2） | 道具/对话递进段不动（已验证正确） |
| 24021 | 5 → **4** | `s4 -> reward`（use-item）目标节点不变 | 现存 `REWARD/4→5` 改为 `REWARD/5→4` | 前段 0→4 递进不动 |
| 24052 | 5 → **4** | `s4 -> reward`（kill-npc）不变 | 现存 `REWARD/4→5` 改为 `REWARD/5→4` | 道具递进 3×3 条不动；`s4 -> s0`（timer）不动 |

配套（已执行）：

1. `JournalRewardRowRepairContractTest`：`(11006, 3, 2)`、`(24021, 5, 4)` 移出 CONTRACTS（双语说明，基线交给专属测试）。
2. 新增 5 个锁定测试：`Quest1345ClientDialogAlignmentTest`、`Quest4338ClientDialogAlignmentTest`、`Quest11006ClientDialogAlignmentTest`、`Quest24021ClientDialogAlignmentTest`、`Quest24052ClientDialogAlignmentTest`。
3. 实机复验（用户冷重启）覆盖 5 任务整链与旧档自愈面（含 4338 的 `START/6` 旧档）。

## 6. 修复验证记录（2026-10-07，IDEA MCP）

| 测试 | 结果 |
|---|---|
| `Quest1345ClientDialogAlignmentTest` | 1/1 通过 |
| `Quest4338ClientDialogAlignmentTest` | 1/1 通过（含两条自愈边断言） |
| `Quest11006ClientDialogAlignmentTest` | 1/1 通过 |
| `Quest24021ClientDialogAlignmentTest` | 1/1 通过 |
| `Quest24052ClientDialogAlignmentTest` | 1/1 通过 |
| `JournalRewardRowRepairContractTest` | 4/4 通过（移出 11006/24021 后） |
| `QuestDefinitionCatalogManifestTest` | 10/10 通过（含全库编译） |
| `ProductionCatalogWhitelistVerificationTest` | 1/1 通过（`PRODUCTION_COMPILE_OK=707`、`FAILURES=0`、`WHITELIST_VIOLATIONS=0`） |

过程中编译器/测试暴露并修正的形态问题（供后续复用）：

- **`UNREACHABLE_NODE` 编译校验**：删改 use-item 路由后，不再被任何 transition 到达的节点（4338 的 s6）会被编译器拒绝；旧的"兼容节点"方案不可行，改用自愈边归一。
- **after-commit 全量断言**：`v2 -> reward`（11006）含 `ShowQuestSelectionDialog(SELECT_QUEST)`、`s4 -> reward`（24021）含 `PlayMovie(88, CUTSCENE)`——新增测试时需对照 XML 完整列出，不要只写 SyncQuestState。
- **IDEA 增量编译滞后**：修改 XML/测试后立即运行可能读到旧产物（本目录两次假失败均由此而来）；运行前先对被改测试执行一次显式构建。

## 7. 残留风险（本报告未展开）

- `NO_ITEMPLAY` 31 个任务：有 XML 无使用道具 transition，推进或由对话/has-item 表达（如 30235-30250 批次成簇），未逐一核对轴。
- `NO_RETAIL_SETPROGRESS` 3 个（2393/3722/4722）：真端无常量推进，未细查。
- `NO_XML` 46 个：生产目录缺定义（未迁移/已退役/ID 不匹配），未细查。
- `7a7d27809` 全批 375 任务与 `7d5bb5317` 收集进度族：CONTRACTS stale 列仅抽查 3 例全中；建议后续按"stale 列 vs legacy/真端"批量复核。
- 4338 的 s6 旧档（START/6）走 Gundalpun 对话的兼容路径未在真端验证（真端无此状态，属 AionEmu 错误批次遗留档）。
