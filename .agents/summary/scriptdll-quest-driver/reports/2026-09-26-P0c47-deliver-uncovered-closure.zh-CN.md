# P0c-47 `DELIVER_UNCOVERED` 收口：交付流改道（2964）+ 阶段推进行领奖窗外溢收窄（四任务）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-26 20:19–20:45（本片 lane，续片 30；安装 20:21:39、指纹重冻 20:24:14）
- 上游：P0c-46 报告 §9.1 登记的 `DELIVER_UNCOVERED` 5 项（P0c-46 判"结构不同 ⇒ 不得剪"，逐项取证裁定）
- 产物：`quest_client_talk_chain_steps.tsv` `ef2cc1a5…`（5071 行）→ **`395f4016…`（5076 行）**；裁定表 `p0c43-canonical-resynthesis.tsv`（新增 code）+ `p0c47-stage-window-spread.tsv`（新建，4 行）

## 1. 结论

P0c-46 留下的 5 项 `DELIVER_UNCOVERED` **全部收口**，且收口由**独立判据复算**证明（不是"看代码觉得对了"）：

| # | 任务 | 越界 NPC | 形状 | 裁定 | 通道 |
|---|---|---|---|---|---|
| 1 | 2964 | 278137 Jafnhar | 真端 `reward` == `acquired`（Srudgelmir 204253），转写把**整条交付流**（`QUEST_SELECT→SELECT5` / `SELECT_QUEST_REWARD`+自造 `HAS_ITEM` 门 / `npc-complete`）绑在**声明的阶段 NPC** Jafnhar 上 | **整块改道**（弃转写，按真端+客户端重合成）：阶段（`SELECT2→SELECT2_1→SETPRO1` 发物 182207043）归 Jafnhar、交付（`SELECT5` + 领奖窗 + `npc-complete`）归 Srudgelmir | 第三条通道**改道**（新 code `LEGACY_DELIVER_FLOW_ON_TALKNPC`） |
| 2 | 11106 | 798979 Gelon（talk_npc2） | `SETPRO2`（s1→reward）行**自带领奖窗下发** | 行内 `after-commit` 末位窗外溢令牌 → `CLOSE` | 第四条通道**行内令牌收窄**（新 code `LEGACY_STAGE_REWARD_WINDOW_SPREAD`） |
| 3 | 21036 | 798713 Fjoersvith（talk_npc2） | 同上（`SETPRO2`） | 同上 | 同上 |
| 4 | 39003 | 800512 DF2a_Ionia_E_LHW（talk_npc1） | 同上（`SETPRO1`） | 同上 | 同上 |
| 5 | 49003 | 800511 LF2a_Noorn_E_DHM（talk_npc1） | 同上（`SETPRO1`） | 同上 | 同上 |

**收口证明（普查复算）**：`p0c46_role_axis_census.py` 对本片产物重跑 → `DELIVER_UNCOVERED` **5 → 0 行**（68 行 → 63 行，其中 INFO 两轴 28/34 逐字不变，无任何新增行）✓。

**审计（生产 IR）前后对比**（同一探针，`p0c53-deliver-uncovered-probe.txt` vs `p0c47-audit-post.txt`）：

| 任务 | `CLIENT_PAGE_UNREACHED` 前 → 后 | 领奖窗（page 5）可达路径 前 → 后 |
|---|---|---|
| 2964 | **1352, 1353 → 无** | 前：`reward + NPC 278137`、`started + NPC 278137 + 1009`；后：**`reward + NPC 204253`、`s1 + NPC 204253 + 1009`** |
| 11106 | 无 → 无 | 前：owner `reward + 203832 + 1009` **+ 阶段 `s1 + 798979 + 10001`**；后：**仅 owner** |
| 21036 | 无 → 无 | 前：owner ×2 **+ 阶段 `s1 + 798713 + 10001`**；后：**仅 owner ×2** |
| 39003 | 1352, 1353 → **同** | 前：owner（`reward`/`started` + 800504 + 1009）；后：**逐字同** |
| 49003 | 1352, 1353 → **同** | 前：owner（`reward`/`started` + 800505 + 1009）；后：**逐字同** |

即：2964 改道后**顺带补齐**了客户端 select2/select2_1 页（1352/1353 由未达变可达）；四任务的领奖窗**只从阶段 NPC 上消失、在 owner 上仍在**（无页丢失）；39003/49003 的 1352/1353 未达是**另一条轴**的既有缺口（§9.1），本片不动、也未加剧。

## 2. 轴的来历（为什么它不是"多了一个 owner"而是"角色写反了"）

2964 的真端行只有三个字段组：`acquired_npc_name=Srudgelmir`、`talk_npc1=Jafnhar`、`give_item1=ITEM_QUEST_2964A 1`、`reward_npc_name=Srudgelmir` —— **交付 owner 与接取 NPC 同人**。遗留 XML（git 历史）却把交付流整条挂在 Jafnhar（talk_npc1）：`QUEST_SELECT→SELECT5`、`SELECT_QUEST_REWARD`（带 `has-item` 门 + priority 1 回选择页）、`npc-complete`；并把真端的 `give_item1` 物件**提前到接取发放**（`NPC_START` 的 accept-actions）。

- P0c-45/46 的剪除通道在这里**无效**：Jafnhar 的交付投影（`act:SELECT_QUEST_REWARD|block:NPC_COMPLETE|page:SELECT5|page:SHOW_SELECT_QUEST_REWARD_WINDOW1`）**不是** owner 投影的子集（owner 当时零交付行）⇒ P0c-46 判"结构不同，不得剪" ✓。本片按改道处置：**行的归属与节点结构都变**（新增 s1 节点、发物改到阶段行、门去掉）。
- 同族判例 **2963**（并排 id、同三 NPC）在 XML 里就是正确形：阶段=278137（`SELECT2`/`SETPRO1`）、交付=204253（`USE_OBJECT→SELECT5`）——2964 的 XML 是把两角色**写反**，不是真端缺数据。

## 3. 五源取证（逐任务，全部可复核）

真端权威：`src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml`
客户端解包：`/private/tmp/dataverify/Dialogs/**/QUEST_Q<id>.html`（与 `docs/quest/client-dialog-mapping/*` 同源）

**2964**（改道）：
1. 真端：`acquired=reward=Srudgelmir(204253)`、`talk_npc1=Jafnhar(278137)`、`give_item1=ITEM_QUEST_2964A`；`item_name_index.tsv` 解 `quest_2964a→182207043`，与 `quest_data` 的 work_items **一致**（`p0c38-canonical-item-channel.tsv` 记 `MATCH`）。
2. 客户端任务书两行逐字点名：**「和[%dic:STR_DIC_N_Jafnhar]对话 / 和[%dic:STR_DIC_N_Srudgelmir]对话」**（阶段=贾夫纳、交付=司鲁德盖密尔）。
3. 客户端页正文：`select_quest_reward1`(5) 是**司鲁德盖密尔**在说话（「请您把找到的东西交给我吧」），`select5` 按钮 `HACTION_SELECT_QUEST_REWARD`「拿出雅芬哈尔给的证物。」；`select2`(1352) 是贾夫纳（「你该不会是告诉了司鲁德盖密尔我在这里吧？」）。
4. 客户端页链 = 接取 `1011→1012→4→1003/1004`、阶段 `1352→1353(SETPRO1「结束对话。」)`、报告 `2375(1009)`→领奖窗 5；`end_npc_ids=204253` 单值、任务书行数 2 = K+1。
5. 同族判例 2963（见 §2）。XML 自造的门另有反证：真端 2964 无 `<item_check>`，客户端 select5 页**只有 1009 一个按钮**（无检查按钮、无 select6 页）。

**11106 / 21036 / 39003 / 49003**（窗外溢收窄）：
1. 真端：`talk_npc<k>` 与裁定表 `stage_npc` 一致、`reward_npc_name` 唯一解析 = `owner_npc`（203832 / 799239 / 800504 / 800505）。
2. 客户端 `end_npc_ids` 单值 == owner；`progress_npc_ids`（39003/49003）== stage_npc。
3. 客户端 HTML 阶段页按钮文案：`HACTION_SETPRO1/SETPRO2` = **「结束对话。」**（11106 `select2_3`/`select3_2`、21036 `select2_1`/`select3_1`、39003/49003 `select2_1`）⇒ 阶段推进**只关窗**，不下发领奖窗。
4. 客户端任务书行序与阶段归属一致（11106「和萨比努斯对话/和盖尔伦对话/去极乐世界和迪莫斯对话」；21036「把标本袋子交给阿尔文/交给菲尔斯贝特/和沃夫冈对话」；39003/49003 单阶段）。
5. 真端 `reward_npc_name` 唯一 + 客户端 end 单值 + 客户端任务书末行点名 owner ⇒ 领奖窗归 owner（`npc-complete` 的 `preview=` 与 owner 名下报告页行仍在册）。

> 顺带核实（避免误判）：`talk_npcK` 顺序 = 客户端 `progress_npc_ids` 顺序的**全局一致性 55/55**（本片做了一次普查：`talk_npc` 序 vs 客户端 progress 序，同集合对照 55 组全部相同，0 组相反）——所以"阶段归属轴"在本片可安全作为守卫（`action=SETPRO<k>` ⇔ `talk_npc<k>`）。

## 4. 生成器改动（两条新通道 + 守卫）

`build_quest_client_talk_chain_steps.py`：

1. **改道 code 域第三形** `LEGACY_DELIVER_FLOW_ON_TALKNPC`（`p0c43-canonical-resynthesis.tsv`）：
   - 反向轴：真端 `reward_id` **必须 ==** `acquired_id`，且客户端 `end_npc_ids`（非空时）**必须 ==** 该唯一 id；
   - 转写交付 owner **必须**等于某个真端 `talk_npcK`（且 ≠ acquired）——与"全绑接取"（`..._ON_ACQUIRED`）、"第三方漂移"（`..._STAGE_OWNER_DRIFT`）两形互斥；
   - 共用轴不变：talk_npcK 唯一解析且 ≠ acquired、客户端任务书行数 == K+1、客户端页册必须有 `SELECT{i+2}`。
   - **类型对齐修正（QE-076 同类）**：`client_roles` 是 int 集、`resolve_npc` 返回 str ⇒ 比较前 `int(reward_id)`。这条守卫在首次干跑就 fail-closed 报错（`end=[204253] 非单值 == owner(204253)`），证明它是活的。
2. **行内令牌收窄通道** `LEGACY_STAGE_REWARD_WINDOW_SPREAD`（新表 `p0c47-stage-window-spread.tsv`，4 行）：
   - 装载期轴（11 条 fail-closed）：code 域；与阶梯/接取入口/改道/多余 owner/角色收窄五表互斥；owner 必须 `RETAIL_TABLE`；`stage_npc` 唯一解析且 == 真端 `talk_npc<k>`（`action=SETPRO<k>` 的 k）；`owner` == 真端 `reward_npc_name` 唯一解析 == 裁定表、且 ≠ `stage_npc`；客户端 end（若登记）== owner；`client_end` 列与 owner 一致。
   - 转写期轴：该 `(npc, action)` 行必须存在、`target` 节点投影 **必须 = REWARD**、`after-commit` **必须逐字**为 `SYNC:<mode>;DIALOG:SHOW_QUEST_PAGE:SHOW_SELECT_QUEST_REWARD_WINDOW1`（只此一处、且在末位）⇒ 改写 = 末位令牌换 `CLOSE`（SYNC 模式逐字保留）。
   - **页列同步**：改写后该行的 `page_check` 页列同时去掉 `SHOW_SELECT_QUEST_REWARD_WINDOW1=`（否则普查/审计仍会把领奖窗记在阶段 NPC 名下 = 收窄未生效）。
   - 收尾守卫①：裁定表条目必须全部命中转写行（否则 fail，"裁定表腐化"）；
   - 收尾守卫②**无页丢失**：改写后每任务仍有 **owner 名下**开领奖窗的记录（`B NPC_COMPLETE` 的 `preview` 非 `-`，或 owner 名下记录含领奖窗页令牌）。

## 5. 裁定表落盘

| 表 | 变更 | md5 |
|---|---|---|
| `p0c43-canonical-resynthesis.tsv` | 头注释（code 域第三形 + 反向轴 + 判例 2964 三证）+ 1 行 | `097428e92f1f384bf95fab78ac691d51` |
| `p0c47-stage-window-spread.tsv` | **新建**（11 条装载期轴 + 五源取证注释 + 4 行） | `d69489ff9c484d3f5cff32a6d72e6b08` |

两表都在干跑/保真的"21 张输入表零漂移"门内（`p0c43_dryrun_diff.py`、`p0c42_builder_fidelity_check.py` 的 DECISION_TABLES 已登记新表）。

## 6. 干跑 → 安装 → 爆炸半径 → 指纹

- **干跑**（`p0c43_dryrun_diff.py --expect 2964,11106,21036,39003,49003`）：`CHANGED [2964, 11106, 21036, 39003, 49003]` / `ADDED []` / `REMOVED []` / `DECISION_TABLES_STABLE: 21 张裁定表零漂移` / `DRYRUN_OK`；REGEN `395f4016…`（422519 字节）。
- **安装**：`build_quest_client_talk_chain_steps.py` → `395f40169ef00ad05c8bf04a8a99e118`，与干跑产物 **`cmp` 逐字节相同**；`target/classes` 同步。
- **爆炸半径**（`p0c47-blast-radius.txt`，pre = `p0c47-registry-pre.tsv` `ef2cc1a5…`）：`BLOCKS 295 -> 295`、`CHANGED` 5 任务、`ADDED [] / REMOVED []`；2964 `+6/-11` 行（新增 s1 节点 + NPC_REPORT 块 + 7 条 R），四个任务各 `+1/-1` 行（只动 after-commit 与页列）。
- **指纹重冻**（`p0c47_refreeze_fingerprints.py`，干跑先看 set-diff 再 `--apply`）：`added=[] removed=[] changed=['11106','21036','2964','39003','49003']`，288 行布局不变；`9c60b0c3…` → **`770064ea…`**（`src/test/resources` + `target/test-classes` 同值）。

## 7. 门禁

| 门 | 命令 | 结果 |
|---|---|---|
| 净树（链门 + 审计 + 契约门） | `mvn -o -B test -Dtest='RetailSimpleTalkChainGateTest,QuestDialogOrderAuditTest,QuestClientContractGateTest'` | **20/20 绿**（20:24:34–20:25:27）⇒ 审计绿 = 领奖窗收窄**未孤立任何客户端声明页**，2964 的阶段页补齐未引入新未达页 |
| 生成器保真 | `p0c42_builder_fidelity_check.py` | `FIDELITY_OK`（**422519 字节**，md5 `395f4016…`）+ **21 张输入表零漂移**（20:27:14） |
| T1 | `run_quest_gates.sh T1` | 75 例 **1F** = 在册 lane 项 `20035`（`登记=REJECTED:RETAIL_TALK_HUNT_CHAIN_DEFERRED 实际=REJECTED:RETAIL_ITEMPLAY_OUTPUT_UNRESOLVED`，DataDriven/ItemPlay 轴，与本片 5 任务无关）；用例数 73→75 = lane 扩过 T1 类集（`gates/T1-202725.log`，441s） |
| T2 | `run_quest_gates.sh T2 2964 11106 21036 39003 49003` | 90 例 **4F**（`gates/T2-203459.log`，442s）：`RetailDataDrivenGateTest`(20035 在册项)、`RewardRowTwoRowTalkFamilyContractTest`(13965 var0)、`QuestDialog31RegressionTest`(1131 路由)、`JournalRewardRowRepairContractTest`(15613 var0) —— **本片 5 个 id 在全部失败条目零命中**；归因见 §8 |
| T3（切片收口跑一次） | `QUEST_FORK_COUNT=2 run_quest_gates.sh T3` | **2013 例 / 117F / 22E / 1S**（`gates/T3-204340.log`，20:44:07–20:50:49，7m07s）。对 **装入前**基线 `gates/T3-194812.log`（19:48:12，2006/118F/27E/1S）按**身份集**对拍：**ADDED ×1** = `RetailDataDrivenGateTest.driftVersusShellsIsRegistered`（**在册 lane 项 20035**）、REMOVED ×7（lane 修好的 50073.xml/50089 catalog/15476/driftRegistry）。对 lane 前基线 `T3-175816`（2006→2011/116F/21E/1S）ADDED ×2 —— 两条**在 19:48 基线里就已在册**（见 §8）。**本片 13 个相关 id（5 任务 + 8 个 NPC/页 id）在整份 T3 日志零命中** ⇒ 零新增失败 |

## 8. T2/T3 归因（并发 lane）

T2 的 4 个失败**全部 lane 侧**，逐条机械归因：

| 失败 | 归因依据 |
|---|---|
| `RetailDataDrivenGateTest.driftVersusShellsIsRegistered`（20035） | lane 在册项：P0c-46 基线（T2-175135/T2-194055）**同身份**已在；lane 20:01:53 改本类、20:03:24 改 `retail-data-driven-drift.tsv` |
| `RewardRowTwoRowTalkFamilyContractTest.qe045LockedSiblingsKeepTheirLegacyBaseline`（13965） | 13965 ∉ 本片 5 任务、不在本片任何裁定表；该类 **git 状态 = M**（lane 改过）；lane 20:01:17 翻 `50089/50090.xml` + `quest_definition_catalog.xml`、20:02:01 翻 `retail-xml-retention.tsv`、20:21:10 改 `RetailNpcNameIndex.java`（正是 NPC 名→id 解析入口） |
| `QuestDialog31RegressionTest.migratedQuestHandlersKeepLegacyStartDialogRoutes`（1131 缺路由） | **机械反证**：1131 的冻结 IR 指纹装前装后**逐字相同**（`0b1adb7e17c1c4b2729c058e08c538794d973cef458b08ddbc8829098bf8f44b`，见重冻 set-diff `changed==5`）⇒ 本片未改 1131 的 IR；1131 不在本片 block（爆炸半径 `CHANGED` 只有 5 个）；该类不读本片两个文件（`talk_chain_steps`/`ir-fingerprints`） |
| `JournalRewardRowRepairContractTest.persistedRewardRowsAreRepairedOnEnterWorld`（15613 var0 5 vs 0） | P0c-46 报告已归因 lane（`RetailDataDrivenDefinitionCompiler` 19:43:27）；本片未碰 DataDriven 通道 |

本片**自身改动面**只有两个文件的 5 个任务块：`quest_client_talk_chain_steps.tsv`（20:21:39）与 `retail-simple-talk-chain-ir-fingerprints.tsv`（20:24:14）；lane 窗口（19:42:30 / 19:43:27 / 19:51:19 / 19:53:13 / **20:01:17** / 20:01:53 / **20:02:01** / 20:03:24 / 20:19:48 / **20:21:10** / 20:24:43 / 20:30:15）覆盖了 T2 失败类所依赖的 retention/catalog/NpcNameIndex 三个面。

**T3 归因**（`gates/T3-204340.log`，2013/117F/22E/1S；基线 `gates/T3-194812.log` 2006/118F/27E/1S = 19:48:12，**早于**本片装入 20:21:39/20:24:14）：

| 对拍 | 结果 |
|---|---|
| 装入前基线 `T3-194812` | **ADDED ×1** = `RetailDataDrivenGateTest.driftVersusShellsIsRegistered`（L423，`20035` 登记/实际不符 = **在册 lane 项**）；**REMOVED ×7** = `LegacyTemplateMirrorRouteRegressionTest`(50089 accept 页)、`QuestClientContractGateTest`(契约问题 12→) 、`QuestEventShardRetailAlignmentTest` ×3(50073.xml 缺失)、`QuestRepeatLifecycleTest`(15476)、`RetailDataDrivenGateTest.loadFixtures`(driftRegistry 空串) —— 全是 lane 在 19:48 之后修好的项。**没有任何本片 5 任务的身份** |
| lane 前基线 `T3-175816`（17:58，2006→2011） | **ADDED ×2**，均为 lane 侧且**在 19:48 基线就已存在**（见下）|

两条"新增"的机械归因（与 17:58 基线比是新增，但**与本片无因果**）：

| 失败 | 归因依据 |
|---|---|
| `QuestMonsterProgressContractAuditTest.repairedMultiKillQuestsDeclareSeparateKillCounters`（quest 50091 must declare var1） | ①该身份在 **19:48 基线已出现**（`grep -c` = 3），而本片装入在 20:21:39 ⇒ 时间上早于本片；②50091 **无 XML**（DataDriven 任务）⇒ 归 lane 的 `RetailDataDrivenDefinitionCompiler`(19:43:27) + `quest_definition_catalog.xml`(20:01:17)；③50091 在本片爆炸半径 `grep -c` = **0**、在登记表 R/N 行 = **0** ⇒ 本片完全未触及该任务块；④该类不读本片两个文件（`grep` 零命中） |
| `QuestPrematureRewardRouteExclusionTest...unknown progress field`（`ProgressLayout.pack:65`） | ①同样在 **19:48 基线已出现**（`grep -c` = 3；`unknown progress field` 字样 = 2）；②抛错在**生产类** `ProgressLayout.pack`（mtime 23:16:03，非今日改动）读到**未登记字段名**，该字段名来自 lane 新增的 DataDriven 进度字段；③该类 mtime 03:36:36（本次未动）、不读本片两个文件 |

⇒ **T3 结论：零新增失败归本片**（ADDED 仅 1 条 lane 在册项；另 2 条是时长更早出现的 lane 红点）；且本片 13 个相关 id（2964 / 11106 / 21036 / 39003 / 49003 / 1352 / 1353 / 278137 / 204253 / 798979 / 798713 / 800512 / 800511）在整份日志**零命中**。

## 9. 残留与登记

1. **阶段页 owner 扩散（新登记）**：39003/49003 的 1352/1353 仍未达 —— 客户端 `progress_page_ids=1352` 声明阶段入口页 `SELECT2`，但登记表只有 `SELECT2_1`（`800512 R2`），且**接取 NPC** 也有一条 `SELECT2_1`（`800500 R1` / `800502 R1`，P0c-46 普查 `INFO_STAGE_SPREAD` 已记）；11106 进一步有 `SELECT3_1/SELECT3_2` 在 `798978`（talk_npc1）与 `798979`（talk_npc2）上**双份**（R9/R10 vs R11/R13）。⇒ 与阶梯轴同域（页归属 + 入口页缺失），判据须与 `apply_talk_ladder` 的声明一致性一起定，登记 `STAGE_PAGE_OWNER_SPREAD_PENDING`。
2. **`HAS_ITEM` 门 vs 真端 `item_check`（新登记）**：89 个"真端 `give_itemN` 且无 `item_check`"的任务里，只有 **3** 个登记行仍带 `HAS_ITEM`（2964 = 本片已裁"XML 自造"；21033 `SETPRO2`、21455 `SETPRO1`/`SELECT_QUEST_REWARD`）——这两个的 `HAS_ITEM` 与真端 `remove_itemN` 同域（真端确实声明了阶段移除），但 `SETPRO` 编号/节点（无 s1）另有轴，登记 `GATE_VS_ITEM_CHECK_PENDING` 待与阶梯轴合并裁定。
3. 既有在册余量不变：`XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING`(34 项/25 任务)、`XML_ONLY_STAGE_ROLE_SPREAD_PENDING`(28 项/25 任务)、`SELECT6_CLOSE_EXIT_OTHER_FAMILIES`(62)、`XML_RETENTION` 1011 未达(44)、25070 FOBJ 页形、REWARD 投影余量、真端数据缺口桶（`ACQUIRE_NPC_UNRESOLVED` 62 / `MONSTER_UNRESOLVED` 56 / `SENTINEL_AREA_PENDING` 64）。
4. **运行时/客户端目检 PENDING**（AGENTS.md 规则 1：未启服）：2964 全流程（接取→贾夫纳阶段发物→司鲁德盖密尔交物开窗→领奖完成）、11106/21036 两阶段后的关窗、39003/49003 单阶段关窗——需用户授权启服后目检；未执行的命令已登记在报告与台账。

## 10. 复现命令

```bash
D=.agents/summary/scriptdll-quest-driver
python3 -B $D/p0c43_dryrun_diff.py --expect 2964,11106,21036,39003,49003
python3 -B $D/p0c42_builder_fidelity_check.py
python3 -B $D/build_quest_client_talk_chain_steps.py
python3 -B $D/p0c46_role_axis_census.py --registry src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv \
        --out $D/p0c47-role-axis-census-post.tsv        # 期望 DELIVER_UNCOVERED = 0
python3 -B $D/p0c47_blast_radius.py
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest -Dretail.talkChain.fingerprintOut=/tmp/p0c47-fp.tsv
python3 -B $D/p0c47_refreeze_fingerprints.py /tmp/p0c47-fp.tsv --apply
mvn -o -B test -Dtest='RetailSimpleTalkChainGateTest,QuestDialogOrderAuditTest,QuestClientContractGateTest'
bash $D/run_quest_gates.sh T1
bash $D/run_quest_gates.sh T2 2964 11106 21036 39003 49003
python3 -B $D/t3_failure_diff.py <baseline T3 log> <candidate T3 log>
```
