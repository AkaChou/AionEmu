# 2026-09-26 续片 22 / P0c-48：链内 PVP 计数词汇 —— 15673/25673/80846/80847 采纳退役


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

## 1. 缺口形状与裁定

`RETAIL_STEP_UNSUPPORTED` 桶（8 行）里的两形共 4 行：**PVP 计数步与其他步类别交错**。

| 行 | 真端 DD 步 | 客户端任务书行 | 关键载荷 |
|---|---|---|---|
| 80846/80847 | `pvp(2)` → `collectitem(交付 NPC)` | 2 行（击杀 2 / 交 800 枚） | `check_item1_1 = world_promotion_vip_continue_coin 800`、`collect_progress=1` |
| 15673/25673 | `enterarea(区别名)` → `pvp(12)` | 3 行（前往区 / 击杀 12 / 报告） | `finished_quest_cond1=Q15550|Q25550`、`quest_permitted_worlds=220110000|210100000`、`can_report=1` |

- **接取**：四行均 `Talk` 接取（80846/80847 `world_event_megaquest_shugo_01`；15673 `LF6_Marzian_E` / 25673 `DF6_Shatropin_E`），交付 15673 `LF6_Ilisia_E` / 25673 `DF6_Reinhard_E`（与客户端 `select_success`/报告页文本一致）。
- **段号裁定（QE-012）**：客户端 `quest_monster.csv` 对 PVP 行**没有**需求行（无 pvp 源类型），段号不可由客户端 gate 校准；按 QE-012 的硬规则「行索引留在 SECTION_0、计数只能放 SECTION_1+」与混合链既有惯例（hunt/采集/itemplay 计数全在 SECTION_1+），PVP 计数取**未被占用的最小段号** = SECTION_1（var1）。已登记为裁定（客户端无 gate 证据，段号由链内计数惯例 + QE-012 规则共同决定）。
- **等级窗**：真端表 PVP 无 `value3`（PvP Target Level Gap）→ 取 ScriptDLL 缺省 10（`PvpVictimLevelDelta(Integer.MIN_VALUE, 10)`，victim 等级 ≥ killer − 10）。**遗留差异（真端优先）**：遗留 15673/25673 用 `kill-in-world 220110000/210100000`（具体世界）+ `pvp-victim-level-delta min=-5 max=9`（双侧窗）；真端表无世界/窗声明 ⇒ 发 `KillInWorld(0)` 世界通配 + 单侧窗，与 P5-4 网格族同判据。
- **遗留形状对照**：80846 遗留壳**完全没有 PVP 步**（1 bit var0 + 交付即完成），真端表 + 客户端任务书两源一致要求 `([%2]/2)` 击杀 ⇒ 采纳即补齐；15673/25673 遗留把**击杀数写在 var0**（`started→k1..k11→reward`，var0=12），违反 QE-012 的段位规则（行索引应留 SECTION_0），真端形状改为 var0 = 行梯（0→1→2）+ var1 = 击杀数。

## 2. 实现（一行词汇 + 一条判据）

- `RetailDataDrivenTalkHuntChainCompiler`：
  - `Step` 增 `PvpTarget(count, minRank, levelGap)` 载荷 + `isPvp()`；
  - `expandSteps` 放行 `pvp` 类别（占行不占对话段），逐 pvp 块消费扁平计数序，**块数 ≠ 计数个数**（分号多段 = 并行槽位，未覆盖形状）时如实返回空表；
  - `build` 的段分配：PVP 与多播 itemplay 同判据取最小未占用段（SECTION_1..5，超限抛错），末步为 PVP 时领奖投影携带满计数（QE-051）；
  - 发射 = 单行 hunt 块的计数对同形：完成边 `VariableAtLeast(varN, count-1)` + 自环 `VariableBelow(varN, count-1)`，两边形都带 `PvpVictimLevelDelta`；事件 `KillRanked(minRank)`（军衔阈值行）或 `KillInWorld(0)`；计数超 6 位（>63）抛错（段位上限）。
  - 段序映射（`stageIndexOf`）把 PVP 步计入「不占对话段」集合。
- `RetailDataDrivenDefinitionCompiler`：新增 `isPvpMix(entry)`（含 pvp 且另有非 pvp 步；纯 PVP 行仍走 allPvp 网格）并 OR 入混合链路由表。
- `RetailDataDrivenGateTest`：采纳白名单新增 `pvpMixScope`（`contains("pvp") && !allPvp()` + 域内类别集）。
- **区名解析（EA 步）**：15673/25673 的感官区别名未登记（`RETAIL_ENTERAREA_ZONE_UNRESOLVED`）。重跑 `p0c41_sensory_area_scan.py`（补入两个别名）：真端世界文件里唯一多边形证据 —— `DF6_SensoryArea_Q15673`（world=df6, mapid=**220110000**）与 `LF6_SensoryArea_Q25673`（world=LF6, mapid=**210100000**），mapid 与 quest.xml 的 `quest_permitted_worlds` **逐字一致**（强互证）。`p0c48_register_pvp_sensory_zones.py` 追加 2 个 POLYGON/SUB 区（zones_quest.xml）+ 2 条别名映射（`retail-world-sensory-area`），双副本 md5 一致。
- **采集段登记**：`build_quest_client_talk_collect_chain_pages.py` 放行 pvp 骑行类别（kind 集 + `wanted_visible` 排除 + 资格句加 `pvp+collectitem` 配对；纯 collectitem 行仍归采集族 HANDIN），并新增 `--out=` 干跑开关。**登记表增量落地（`p0c48_add_pvp_collect_stage_rows.py`）**只带本批 2 行 + 9640（家族外），**刻意不带入 20035/20501**（TALK_HUNT_CHAIN_DEFERRED 桶、并行车道的在改行）——登记表与生成器差 2 行，在此登记为「车道在改行待其自己 slice 落地」。

## 3. 防掩盖对拍（编译改动前后 1508 行分类逐行 diff）

- 编译改动后（登记表/区名未补时）：增量 = **15673/25673 → `RETAIL_ENTERAREA_ZONE_UNRESOLVED`、80846/80847 → `RETAIL_TALK_HUNT_CHAIN_DEFERRED`**，其余 1504 行零增减；并行车道既有红 `20035`（`TALK_HUNT_CHAIN_DEFERRED` → `ITEMPLAY_OUTPUT_UNRESOLVED`，非本片代码路径：itemplay 块先行拒绝）。
- 冻结指纹对拍：退役行 1142 行中**漂移集 = 车道的 5 行**（15042/16821/16823/26821/26823），本片**0 行**。
- 补区名 + 登记表后：增量 = 本批 4 行 `REJECTED → ADOPTED`，再加 `20501`（登记表干跑的家族内副产品，**未落地**，见 §2）。

## 4. IR 逐边核对（真值来源 = 编译 dump）

- 80846/80847：`started(var0=0)` --`KillInWorld(0)` ×2 计数对（var1<1 递增 / var1≥1 完成，带 `PvpVictimLevelDelta[MIN,10]`）--> `s1(var0=1)`；`s1` 39/1009 检查对（`HasItem[186000344 ×800]` → `RemoveItem[186000344 ×800]`，`ShowQuestDialog[5]`）--> `reward(var0=1)`；与已采纳同族 80848/80958 **同构到边**（仅首步事件由 `KillNpc` 换成 `KillInWorld`）。
- 15673/25673：`started(var0=0)` --`EnterZone[DF6_SENSORYAREA_Q15673_220110000]`--> `s1(var0=1)`（EA 步，`SetVariable(var0,1)`）；`s1` --`KillInWorld(0)` ×12 计数对--> `reward`（末步直落领奖，`SetVariable(var0,2)`，投影携带 `var1=12` = 客户端末行报告行）；`EnterWorld → reward` 自愈边=`var0==0` 门（末步非对话段形，与 15608 同判据）；交付 NPC 806114/806116。
- 客户端任务书行数与领奖行：80846/80847 = 2 行（lastRow=1）、15673/25673 = 3 行（lastRow=2），与 `reward` 投影逐个一致。
- 前置：`finished_quest_cond1` 由 `StartEligible` + 起始条件组表达——`QuestPrerequisiteRetailContractTest` 3/3 绿（含 `PREREQ_BATCH` 里锁死的 15673→15550）。
- **遗留差异（真端优先，登记在案）**：(a) 遗留的 `AtDistance` 接取路由（15673: 806114/806671；25673: 806116/806672）在真端形状下不存在（族内规范接取流 = Talk 路由）；(b) 末步 PVP 完成的 after-commit = `PACKET_ONLY`（与 Hunt 族同形，遗留用 `LEVEL_AND_VISIBILITY_REFRESH`）——族内既有观察项；(c) 世界通配 vs 遗留具体世界（见 §1）。

## 5. 收口

- `p0c48_retire_pvp_chain_rows.py` 翻转 4 行 → `verify_retirement.py` = `catalog=1337 directory=1337 retired=4887 sum=6224 OK`；清单五副本（src/main + src/test + target/classes + target/test + .agents 快照）一致。
- 翻转后 dump 指纹手术插入 4 行（`p0c48_insert_fp_rows.py`：**既有行改值 0**、车道 5 行保持基线值=不掩盖、整块升序、打补丁后逐行等于 dump）；1142 → **1146** 行，双副本 md5 一致（15673=c950f6b0…/5-40、25673=00e473f3…/5-40、80846/80847=41440eb8…/5-47）。
- 漂移登记本批 4 行改 `ADOPTED`（`p0c48_update_drift_rows.py`，只改本批、双副本 + .agents 快照一致）。
- **DD 门（翻转后）6 例 2 红，全为并行车道既有项**：指纹失同步 = 车道 5 行；漂移登记失同步 = 车道 `20035` 单行。
- **T2（4 id / 68 例 4 红，全归车道/既有债）**：`RetailSimpleCollectItemGateTest`（冻结 155 vs 退役 175，车道）、`RetailDataDrivenGateTest`（上）、`QuestClientContractGateTest`（`BUTTON_WITHOUT_ROUTE` 2 项：1932/3092 —— 两者均 **SimpleTalk 族、无 DD 行**，本片改动不可达，属车道在改的 SimpleTalk 编译器）。
- **T3 盲点补跑**：`QuestHandoverContinuationAuditTest` 红项 = 18742/28742（车道新采纳行待重塑，非本批 4 行）；`QuestDefinitionCatalogManifestTest` 10/10 绿（在 T2 门禁列表内）。
- 本批 4 行在：分类门 / 指纹门 / 保留清单门 / 采纳白名单门 / 契约门 / 前置合同门 / 报告奖励覆盖门 **全部 0 命中**。
- **T3 全量（2006 例 123F/21E/1S，`gates/T3-143841.log`）**：与**最后一次完整基线 13:03:22**（2004 例 123F/21E/1S，`gates/T3-130322.log`）按「类集 + 类.方法集」机械对拍（`t3_failure_diff.py`；日志摘要行被 tee 截尾，故按截断前缀比对）⇒ **only-in-right = 空**（新增失败类 0 / 新增失败方法 0），only-in-left = 10 类（车道自愈：10501/10503/10504/26800/Archives/CollapsedSingleStep/CollectProgress/QuestDependencyIndex/MissionItemConsumption——均仍在树内=修复非删除）。对 10:27 基线（`gates/T3-102720.log`）only-in-right = 3 类，**全部为此前已在册的车道在飞项**：`RetailDataDrivenGateTest`×2（指纹失同步=车道 5 行；漂移登记失同步=车道 `20035`）、`QuestMonsterProgressContractAuditTest.quest16823And26823…`（车道 16823 影片剪辑断言）、`Quest25608RetailSevenStepAlignmentTest.sevenRetailStepsCoverEveryClientJournalRow`（续片 17 已登记的车道判官迁移，且 13:03 已在红）。本批 4 个 id（15673/25673/80846/80847）在 T3 全部失败消息中**零命中**（仅出现在探针 IR dump 段）。归因表 `gates/p0c48-t3-diff-vs-1303.txt`、`gates/p0c48-t3-diff-vs-1027.txt`。
- **探针残留（自查自纠）**：`src/test/java` 里的 `P0c48PvpChainProbeTest.java` 已删，但 `target/test-classes/...class` 旧产物未清 → T3 仍执行了该探针（1 例、0 失败，即那两处 id 命中来源）。已删该 `.class`；对失败集无影响。**判例：删探针源文件必须连 `target/test-classes` 的 `.class` 一起删**（旧产物会被 surefire 直接跑）。

## 6. 余量（STEP_UNSUPPORTED 8 → 4）

TalkFOBJ 单步 2（25052/25070：交付物门挂在报告步，属 FOBJ 物品门链轴的 `select_none_1`+`SET_SUCCEED` 词汇域）；talkfobj+hunt 2（18931/28931：谓词可扩 `isTalkFobjHuntMix`，须先核 `Quest28931ClientDialogAlignmentTest`）。
