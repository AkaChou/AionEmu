# 2026-09-26 续片 27 / P0c-53：`ACQUIRE_NPC_UNRESOLVED` 38 行形状裁定（组表判据静态化 + 23 行采纳）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

## 0. 一句话

把 `RETAIL_ACQUIRE_NPC_UNRESOLVED` 38 行按**形状**拆成三类（真端字段是工作物符号的 11 行 / 既有变体通道
可解析但被"变体→领奖门形状"挡住的 10 行 / `_challengetask_` 行），用**纯静态判据**把真端对话名组表从 8 组
加宽到 31 组（17 组因成员跨 `title_id` 排除、1 组既有矛盾登记排除），据此**净采纳 23 行**（25 行 flip，
50089/50090 撞客户端契约门回退并入 curated）；同时修掉组索引的**集合顺序不确定**（`Set.copyOf` 哈希序
→ 插入序），治愈 18742 接取路由的排序红；两支退役行的对齐 pin 换锚生产视图。

## 1. 缺口分解：38 行为什么"读不到接取名"

`RETAIL_ACQUIRE_NPC_UNRESOLVED` 的行在 DD 表里 `value0_acquire_` / `reward_npc_name` 写的是
**对话路由名**而不是 spawn 名，精确解析必然为空。普查（`p0c53-acquire-variant-census.tsv`，38 行）：

| 形状 | 行数 | 判据 | 处置 |
|---|---|---|---|
| 字段是**工作物符号**（`ITEM_*` / 任务物品名） | 11 | `acquire_category_=ItemPlay` 且字段不在 npc 索引里但命中 item 索引 | 诚实暂缓（`RETAIL_ITEMPLAY_*`），不属本片 |
| 既有**变体通道**可解析，但被"变体→领奖门"形状挡住 | 10 | `resolveVariants` 命中，而该族的领奖门要求单一 owner | 本片由组表声明收编（下节） |
| `_challengetask_` 行 | 其余 | 需被拒的壳 XML 才能读出任务线 | 本片不动，留 `TALK_CHAIN_DEFERRED` 桶 |

关键结论：38 行里**没有一行**是"真端数据缺失"——它们的接取人都在真端数据里，只是写在**对话路由名**
（`quest_ai_name` / 客户端 npc 块）这一层。破局点因此是**把组表的候选判据从"名字形态"改成"客户端声明"**。

## 2. 组表加宽：候选判据静态化（本片的返工段）

P0c-52 的组表只收了守备队族（8 行）。本片把生成器 `p0c52_quest_ai_name_groups.py` 的候选判据重写为
**全部可在候选阶段判定的静态谓词**：

```
候选 = DD 引用 ∧ 客户端声明的成员 ≥2 ∧ 组名 ≠ 任一成员自己的名字 ∧ 成员共享一个 title_id
```

- **组名 ≠ 成员自己的名字**：排除 `NPC_event_goldstar_l` / `_d` / `_alchemy` 这类"每个 NPC 各有自己的
  对话名"的伪组；真正的共享对话名形如 `NPC_event_goldstar_master`（`_l_master`/`_d_master` 共用）。
- **成员共享一个 `title_id`**：真端编制不变式（同编制才同对话名）。

结果：**31 组通过**，**18 组排除**（17 组 `MULTI_TITLE_ID` + 1 组 `LEGACY_ACCEPT_CONTRADICTS_CLIENT`）。
产物对（生成器 `--emit`，重跑逐字节相同）：

| 文件 | 行数 | md5 |
|---|---|---|
| `quest_retail/retail-quest-ai-name-groups.tsv` | 31 | `f1c99185e42d73d78fe85f219626b064` |
| `quest_retail/retail-quest-ai-name-groups-rejected.tsv` | 18 | `243bda2158fff08877842afb40d43421` |

**返工记录（判例素材）**：第一版生成器把"遗留 XML 接取流 ⊆ 客户端声明"也当判据 ⇒ 同一张表在兄弟
XML 退役前后**产出不同**（Raksha 组从候选翻成 `LEGACY_ACCEPT_CONTRADICTS_CLIENT`）。修法 = 删掉该
谓词、把已知矛盾（`magician_apprentice`）冻结进 `REGISTERED_EXCLUSIONS`、遗留 XML 降级为**只打印的
见证**。教训：**判据只能取静态输入**（客户端 npc 块 + 服务端模板），否则产物随 checkout 状态漂移。

## 3. 通道语义：`resolvePartyName` 的优先级

`RetailNpcNameIndex.resolvePartyName(name)` = 精确 → **声明的组** → 名字形态变体，命中即止。顺序的
理由在 P0c-52 已裁定（客户端声明优先于形态推导：`IDRaksha_Solo_StageStart` 的前缀族含 `_A_Dark.._C_Dark`
三只暗面模板，而客户端把暗面**另起一个对话名**声明；按族展开会把对面阵营绑进来）。本片把该通道接到
`RetailSimpleHuntPlan.bind` 与 DD 编译器的各接取/交付分支，并保持其他位点（步内 npc、采集目标）仍走
精确解析——通道**按字段**分道，不让组名静默变宽非对话位点。

## 4. 落地：25 flip → 净 23（50089/50090 回退）

flip 前逐行裁定表 `p0c53-group-table-decisions.tsv`（25 行，含 before/after 与四源见证）：

- **采纳 23 行**：15476-15479 / 18743 / 25476-25479 / 28743 / 50068 / 50069 / 50072 / 50073 / 50074 /
  50088 / 50091 / 50092 / 50094 / 50095 / 50104 / 50105 / 50107。
- **回退 2 行**：**50089 / 50090**——编译通过、组表解析成功，但**客户端契约门**判
  `PAGE_NOT_IN_TASK_HTML`：该 collect 模板为组员发接取页（1002→1003 / 1003→1004），而客户端 task HTML
  对这两个 id **一个接取页都没声明**（且 HTML 正文是击杀任务、DD 行是采集）⇒ 不可采纳。回退面：
  XML 还原 + 目录行还原 + 保留清单 4 副本与 `.agents` 快照回 `XML_RETENTION`、漂移登记改
  `REJECTED:CURATED_LEGACY_CONTRACT_LOCK`、指纹行删除；并在门里登记为 curated 暂缓
  （`RetailDataDrivenGateTest.CURATED_DEFERRED` 4 项 → **6 项**，code `CURATED_LEGACY_CONTRACT_LOCK` 不变）。
- 落地脚本：`p0c53_retire_group_table_rows.py`（flip）、`p0c53_revert_row_flip.py`（50089/50090 回退）、
  `p0c53_update_drift_rows.py`、`p0c53_insert_fp_rows.py`（含 2 处码位迁移 15480/25480）。

**桶变化**（`p0c53-classification-guardonly.tsv` → 现态）：

| 桶 | 前 | 后 | 说明 |
|---|---|---|---|
| `ADOPTED`（驱动读数） | 1174 | 1199 | +25 flip |
| `REJECTED:RETAIL_ACQUIRE_NPC_UNRESOLVED` | 38 | **21** | −15 采纳 −2 回退 |
| `REJECTED:RETAIL_REWARD_NPC_UNRESOLVED` | 19 | **9** | −10 采纳 |
| `REJECTED:RETAIL_MONSTER_UNRESOLVED` | 56 | **58** | +2（15480/25480 接取/交付已通，暴露更深的怪名轴 ⇒ 诚实收窄） |

## 5. 集合顺序：`Set.copyOf` 哈希序 → 插入序

`RetailNpcNameIndex` 组表落地原用 `Set.copyOf(ids)`，成员 id 集变成**哈希序**，使按 owner 展开的接取
路由顺序在两次运行间不稳定（实测 18742 接取路由序 = `206380/206379/206378`，客户端契约测试按列表比较
即红）。改为 `Collections.unmodifiableSet(new LinkedHashSet<>(ids))`（表内声明序、生成器按 id 升序写出）：

- 探针 `p0c53-dialog-axis-orderfix.txt`：18742 接取路由序 = `206378 → 206379 → 206380`（= 客户端声明序）。
- 指纹 dump 对拍 `p0c53-fp-dump-orderfix.tsv` vs 冻结文件：**0 行变化**（哈希本身与顺序无关 ⇒ 顺序修复
  是纯确定性修复，不改任何 IR 语义）。

## 6. 退役行对齐 pin 换锚生产视图（两支）

采纳使两个 pin 撞上"XML 已退役"：

| 测试 | 换锚 | 断言变化 |
|---|---|---|
| `QuestEventShardRetailAlignmentTest`（50073/50074） | 逐 id `path` 读 XML → `ProductionQuestDefinitions.definitionInOverlay` | 四支 XML 行断言不动；退役两支按**真端族形**断言：击杀计数 = **客户端进度行计数**（15，自锚而非硬编码）、阶梯目标集 `{246293, 246326}`（DD 行名 `IDEvent_Solo_Saam_65_N` + 其显示名同族模板）、完成路线 `17 × 报告NPC`（16 领奖窗页 + 1 条 108 领奖窗确认）、`name` = 真端占位串 `Q<id>`（客户端只读 `displayNameId`，仍逐任务断言） |
| `QuestRepeatLifecycleTest`（15476） | XML 资源流 + `requireNonNull`（退役后 NPE）→ `definitionInOverlay(15476)` | 断言语义不变（完成态可再起） |

**未改任何判据的严格性**：换锚后 50073/50074 的 metadata（`displayNameId` 1803481/1803482、等级
46/51、`PC_ALL`、重复策略、奖励）逐项仍与 pin 相同；击杀轴由"遗留 XML 的 `var1` 上限"改为"客户端进度
行计数"（更强：自锚）。真端族形与遗留形的三处差异均为**表达差异**（`KillNpcSet` ↔ 逐 npc `KillNpc`，
见既有 `RetailKillRoutes` 归一化；`var1` 计数槽 ↔ 阶梯深度；领奖窗 108 确认路线），不是契约差异。

## 7. 机械核验（防掩盖）

- `verify_retirement.py`：`catalog=1286 directory=1286 retired=4938 sum=6224` + `OK 目录一致，无悬空生产引用`
  （相对 P0c-52 收口值 1309/4915：**净退役 +23**，恰等于净采纳行数）。
- 漂移登记 2 副本逐字节相同（`c7584a56938e8de5adbe7d1d6b83c5dd`，1508 数据行）；冻结指纹 2 副本相同
  （`0cf0739ed405dfcf37c431781b77cf13`，1197 数据行）；保留清单 **5 副本**相同
  （`9720cadcb9ca3b227d3c5a6c4b9fc830`，含 `target/classes`、`target/test-classes` 与 `.agents` 快照），
  回退的 50089/50090 在两个 target 副本里都恢复了 XML 与目录行；组表 2 份产物 md5 见 §2。
- 目录文档一致性：`refresh_catalog_doc_links.py` 干跑 `retired=4938 replaced=0 dangling=0`；回退的两行
  从"已退役"标注**改回真实链接**（该工具只按"链接指向缺失文件"重写，认不出"已加退役标注但 XML 已回来"
  的陈旧行 ⇒ 手工修正并复跑确认）。
- **顺序修复不改语义**的机器证明：指纹 dump 与冻结文件对拍 0 行差异（§5）。
- 生成器重跑逐字节相同（组表/拒绝表 md5 不变 ⇒ 组表判据已静态化）。
- 探针按纪律删除：`P0c53DialogAxisProbeTest` / `P0c53DeliverUncoveredProbeTest` /
  `P0c53bShardPinProbeTest`（源码 `.txt` 与输出归档进 `.agents/summary/scriptdll-quest-driver/`，
  `target/test-classes` 的 `.class` 一并删除）。

## 8. 门禁

| 档 | 结果 | 归因 |
|---|---|---|
| T1（`gates/T1-203317.log`） | 75 例 **1F** | 唯一红 = 车道行 `20035` 的漂移登记失同步（登记 `TALK_HUNT_CHAIN_DEFERRED` vs 实际 `ITEMPLAY_OUTPUT_UNRESOLVED`）。**本片未改变它**：`p0c53-classification-{guardonly,post}.tsv` 两处 20035 均为 `REJECTED:RETAIL_ITEMPLAY_OUTPUT_UNRESOLVED`（车道改了编译器而漂移文件行未同步） |
| T2（`gates/T2-204130.log`，30 个 id） | 158 例 7F/2E | 失败身份**逐条在车道基线 `gates/T3-194812.log` 在册**：18742 turn-in NPC（见下）、1131 action 10000、16823 movie、50091 var1、2569 report、unknown progress field、18745 reward owner、18742/28742 page 5；外加车道 20035 |
| T3（全树，`gates/T3-204925.log`） | 2013 例 **117F/22E/1S** | vs 车道同态 `T3-204340`（同 2013/117/22/1）**ADDED 0 / REMOVED 0** = 零新增；vs mid-flip `T3-194812` ADDED 1（车道 `20035`，解除遮蔽）/ REMOVED 7（含本片 3+1 pin 换锚、1 夹具修复、2 由 50089/50090 回退自愈） |

**18742 turn-in NPC 一条的解释（重要）**：基线里该**方法**红在 line 46（接取路由**排序**失败，正是本片
§5 修掉的哈希序）；顺序修好后测试推进到 line 52 的 turn-in 绑定断言。该绑定**早于本片即已如此**，三重
证据：① 18742 的 IR 指纹自加宽前的 `p0c53-fp-dump-post.tsv` 起逐字节相同（`d80e195a…`）直到现在；
② 该行用的组名 `IDRaksha_Solo_StageStart` **早就在**加宽前的组表（`p0c53-groups-guardonly-tmp.tsv` 第 18 行）；
③ 加宽前后分类 dump 中 18742 均为 `ADOPTED`。⇒ 属车道既有缺陷（item-check 页绑到接取 NPC 而非交付
NPC），本片只解除其遮蔽，不做超出本片的裁判。

## 9. T3 全量对拍（归因）

本片 T3 = `gates/T3-204925.log`（20:49:25 启动，510s，**2013 例 117F/22E/1S**）。两份对拍（工具
`p0c52_t3_attribution.py`，只做失败方法集合算术）：

| 对拍对象 | 结果 | 说明 |
|---|---|---|
| 车道 T3 `gates/T3-204340.log`（20:43 启动，本片落定后；**2013 例 117F/22E/1S**） | **ADDED 0 / REMOVED 0**（107 ↔ 107 身份集完全相同） | 本片零新增失败的**主证据**：另一会话在同一落定态跑出的失败面与本片逐条相同 |
| 车道 T3 `gates/T3-194812.log`（19:48:12 启动；该轮撞上本片 flip 中途，19:47:47 翻转、19:48:54 漂移改写、19:49:26 指纹插入） | ADDED **1** / REMOVED **7** | ADDED = 车道 `RetailDataDrivenGateTest.driftVersusShellsIsRegistered`（`20035` 码位，被本片**修好的漂移文件头损坏**（`loadFixtures` NumberFormat）**解除遮蔽**）；REMOVED = 3×`QuestEventShardRetailAlignmentTest`（50073 NoSuchFile）+ `QuestRepeatLifecycleTest`（NPE）+ `RetailDataDrivenGateTest.loadFixtures` + `QuestClientContractGateTest`（12 条 `PAGE_NOT_IN_TASK_HTML`）+ `LegacyTemplateMirrorRouteRegressionTest`（50089 接取响应）——后两条正是 50089/50090 **回退**后自愈 |

时间线（用于判定哪份日志能当基线）：翻转 `p0c53_retire_group_table_rows.py` **19:47:47** → 漂移改写
19:48:54 → 指纹插入 19:49:26 → 回退 `p0c53_revert_row_flip.py` **20:01:10** → 保留清单终稿 **20:02:01**
→ 顺序修复 `RetailNpcNameIndex` 20:21:10 → pin 换锚 20:24–20:30 → 本片 T3 20:49:25。
⇒ `T3-194812` 的 50073/50089 面是**本片 flip 的中途态**（不能当独立基线），`T3-204340` 才是同态互证。

**结论**：T3 零新增失败（同态对拍 ADDED 0 / REMOVED 0）；对 mid-flip 基线的唯一 ADDED 是车道行
（`20035`，加宽前后分类 dump 逐值相同 ⇒ 非本片），且它此前被本片修好的夹具损坏掩蔽。

## 10. 余量（下一步）

| 桶 | 行数 | 备注 |
|---|---|---|
| `RETAIL_ACQUIRE_NPC_SENTINEL_AREA_PENDING` | 64 | **禁从代码轴再攻**（P0c-48 已判：需区名/客户端证据） |
| `RETAIL_MONSTER_UNRESOLVED` | 58 | 含 15480/25480 两行新暴露 |
| `RETAIL_TALK_HUNT_CHAIN_DEFERRED` | 55 | 链形状（talk-hunt 交错） |
| `RETAIL_HANDIN_VOCABULARY_UNSUPPORTED` | 48 | 交付词汇 |
| `RETAIL_TALK_CHAIN_DEFERRED` | 23 | 纯 talk 链 |
| `RETAIL_ACQUIRE_NPC_UNRESOLVED` | 21 | 本片后余量（工作物符号 / `_challengetask_` / 变体门形状） |
| `REJECTED:RETAIL_REWARD_NPC_UNRESOLVED` | 9 | 本片后余量 |

## 11. 一句话结论（交付）

P0c-53 把"接取名读不到"这一桶从 38 行压到 21 行，方法是**把组表判据换成静态可判的客户端声明**
（31 组入选 / 18 组排除，生成器可复现），并用一次 25 行 flip 验证了通道的爆炸半径可完全掌握
（净 23 行、2 行撞客户端契约回退且登记为 curated）；附带修掉了让 18742 契约测试发红的哈希序不确定，
并把两支退役行的 pin 换锚到生产视图与客户端计数轴。
