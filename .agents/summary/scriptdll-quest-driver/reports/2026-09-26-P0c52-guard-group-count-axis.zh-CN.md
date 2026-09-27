# 2026-09-26 续片 26 / P0c-52：守备队区名组退役（24 行）+ 计数轴第二次裁定 + 击杀门禁真端化

## 0. 一句话

把 `RETAIL_ACQUIRE_NPC_UNRESOLVED` 桶里的 **LDF4_Advance_Village_Guard 区名组**（8 组 / 24 行）
按四源一致裁定采纳退役；采纳过程暴露并收口了**计数轴第二次分歧**（13841 族，真端名单含改名模板）；
顺手把**击杀计数常设门从 XML-only 目录迁到生产视图**（真端化），清掉已登记的"对退役行结构性失明"
缺口，并给该门换上不依赖怪名比对的计数对齐判据与集合对拍人口护栏。

## 1. 缺口：`RETAIL_ACQUIRE_NPC_UNRESOLVED` 与守备队区名组

真端 DD 表 24 行的接取/交付字段写的是**对话路由组名**（如 `LDF4_Advance_Village_Guard_D_East`），
服务端模板里没有这个 `name_desc` ⇒ 精确解析必须失败。破局证据链（**四源一致**，生成器
`p0c52_quest_ai_name_groups.py` 可复算）：

| 源 | 提供什么 |
|---|---|
| 客户端 npc 块 `<quest_ai_name>` | 24 个区名组名 ↔ 组员名的**对应关系**（客户端自己按组路由对话） |
| 客户端词典正文 `STR_DIC_E_<组名>` | 组员名清单（词典正文枚举成员） |
| 服务端 npc 模板 | 组员共享一个 `title_id`（title 是**客户端**侧小队名，不是判别键——同 title 跨区共享） |
| 遗留 XML `NPC_START` id 集 | 组员 id 实集（与客户端名单同侧） |

产物：`retail-quest-ai-name-groups.tsv`（8 行 = 组名 → 组员名清单）+ `RetailNpcNameIndex` 新增
**组通道**（`isQuestAiNameGroup` / `questAiNameGroupMembers` / `resolveQuestAiNameGroup` /
`resolveAllOrQuestAiNameGroup`）。**通道只在 hunt 族生效**（hunt 合成器本来就按组员逐条发接取/报告路由，
与遗留 XML 的多 NPC 形同构）；其余族保持精确解析，组名照旧 fail-closed。

## 2. 计数轴第一次裁定（本片的返工段）

首次 flip 的编译 dump 立刻打了自己一巴掌：24 行的**服务端读数**（15/12/8/6/20）与
**客户端任务书/SECTION 门控**（统一 5）冲突 25/26 行。三份证据同侧指客户端：

1. 客户端 `quest_monster.csv` 的 `SECTION_1<5`；
2. 客户端自己的 `Quest_unpacked/data_driven_quest.xml`（玩家看到并驱动的计数器）；
3. `docs/.../quest-counter-audit/2026-09-18-...` 早已按三证据裁定为 5 杀；
4. 遗留 XML 的 `var1` 上限与门槛（`max=5`、`below 4`/`atleast 4`）。

裁定：**客户端计数为权威**（`RetailSimpleHuntPlan.withClientStageCounts`）。执行顺序是
**先全量回退**（`p0c52_revert_guard_group_flip.py`，幂等）→ 修轴 → 重翻 → 重冻，绝不含偏差采纳。
修轴后 `KILL AXIS CONFLICTS 0/26`。

## 3. flip 前双侧客户端契约对拍（P0c-39 判据）

XML 侧（`git show HEAD:` 落盘的真前 XML 逐件编译）与 RET 侧（真端链编译结果）审计剖面：

| 侧 | 致命 |
|---|---|
| XML（24 行真前状态） | **0** |
| RET（24 行真端编译） | **0** |

（`p0c52_dual_side.out.txt`；页/按钮覆盖数两侧同集，未达页同为已登记的独立轴。）准予采纳。

## 4. 落地与收口（24 行）

- `p0c52_retire_guard_group_rows.py`：manifest（main / target/classes / src/test / target/test-classes
  + `.agents` 快照）翻 `XML_RETENTION → RETAIL_TABLE`，删 24 个 XML（main + target），目录行同步移除；
  `verify_retirement.py` = `catalog=1309 directory=1309 retired=4915 sum=6224 OK`。
- 指纹手术：`p0c52_fp_flip.py` 插入 24 行（既有行改值 0）1150 → **1174** 行；
  漂移登记 24 行 → `ADOPTED`（`p0c52_update_drift_rows.py`）。
- 分类 diff（防掩盖）：24 行 `REJECTED:RETAIL_ACQUIRE_NPC_UNRESOLVED → ADOPTED`；同窗口车道在飞面
  （DD 指纹 5 行 + `20035` 码位）不计本片——车道已于 17:2x 自行重冻，现在与实测 dump 逐字节一致。

## 5. 计数轴第二次裁定（13841 族 / 6 行）

真端化击杀门禁后，414 行合同扫出**两个新违约**（`13841`/`23841`：真端 60 vs 客户端 40）。
扩查得单段行计数分歧共 **40 行**：34 行怪名集合一致（已在册）+ **6 行真端名单含改名/幽灵模板**
（`13841/13845/13849` 与阿族孪生 `23841/23845/23849`，真端 60/60/55 vs 客户端 40；其中
`IDAbRe_Low_Wciel` 在本端**解析不出任何 npc**）。三证同侧指 40：

- 客户端门控 `SECTION_1<40`；
- 客户端进度行 40；
- 遗留 XML `var1` 上限 40、门槛 `below 39`/`atleast 39`、击杀 npc 集 = 客户端那一族 8 个
  （214740/1/2/3、214824/5/6、215424，逐个解析确认）。

判据从「怪名集合一致」改为「**单计数器 + 单条客户端进度行**」：单段行只有一个计数器，客户端也只登记
一行 ⇒ 该行就是这一段，按名比对会被改名模板骗过。落地后只有 `13841/23841` 的 IR 真的改判
（另 4 行本来就是 40，指纹不动 ⇒ 爆炸半径**实测 2 行**），`p0c52b_refreeze_count_rows.py` 重冻这 2 行
（行集不变、其余逐字节保留），常设门 `RetailHuntClientCountGateTest` 的裁定集 34 → **40**。

## 6. 击杀计数常设门真端化（关闭已登记缺口）

原门（`QuestKillCounterRetailGateTest`）用 `QuestDefinitionCatalogManifest.compile` 的 **XML-only 目录**，
对 4915 行退役行结构性失明（`quest X is not an executable owner`），且模拟器读不到真端计数器
（`uses []`）。本片收口：

1. **目录换生产视图**（`ProductionQuestDefinitions.catalog()`，`@BeforeAll` 打开开关、`@AfterAll` 还原）：
   合同 414 行 `unresolved=0`，全部门内可解析。
2. **模拟器补台阶口径**（`QuestKillCounterSimulator.killCounterFields`）：真端 IR 的计数没有动作，
   击杀把局面一级一级推上节点台阶（`a0→…→a5`，每级钉 `var0`）⇒ 动作口径一无所获时按
   "每级往前挪的那个进度字段"识别计数器。XML 行仍走动作口径（台阶规则不参与，避免把阶段标记
   误当计数器）。
3. **负对照换锚**：原锚 13765 已退役，且真端台阶靠"没有下一条击杀路由"收口（不是 REWARD 落点）⇒
   负对照改为"在末级落点后再加一级台阶"，模拟结果必须 5 → 6（门不是空转）。
4. **人口护栏换口径**：`<kills>` 声明只活在 XML 侧（真端元数据无 kills 源，该轴分歧在
   `RetailMetadataEquivalenceGateTest` 注册）；原来的魔法下限（`declaring >= 90`）随退役自然失效，
   改为**集合对拍**：目录里的声明行集必须与**机房 XML 目录文本扫描**（独立于编译器）逐一相等。
   解析器漏读会少行、装配漏带会多行，两个方向都会红；退役合法地移出人口，护栏自动跟随。
5. 该门 4/4 绿；合同 414 行扫描**全部等于客户端门控**（含上节的 6 行改判）。

## 7. 退役行对齐测试换锚（13765）

`Quest13765RetailAlignmentTest` 原本逐条断言 XML 形态（节点标签 `started/reward`、`var1` 累加计数、
`<kills>` 声明）。13765 退役后 XML 已删，测试改锚**生产视图**（真端对齐）：显示名 id 1801283、
等级 65、类别 `SEEN_MARKER`、种族 ELYOS、重复上限 255、奖励（EXP 3618881 + 186000236×5）、
击杀 235357 共 5 次（台阶计数 `var0`）、报告页 1009 只落三位守备队 NPC（805272/3/4），
并显式断言真端元数据**不带** `<kills>`（XML 侧展示通道）。

## 8. 机械核验（防掩盖）

- 指纹 `8d3500a4186e4982616e5fc70518a2d4`（1174 行，双副本一致）；漂移登记
  `8e3aa0f83d81e6caae2c641d96fbd531`（1508 行，双副本一致）；24 行 + 13841 全为 `ADOPTED`。
- 清单五副本：main / target/classes / src/test / target/test-classes 四副本 md5 相同；
  `.agents` 快照此前落后 13 行（早期切片退役未回写），本片顺手刷新为与 main 逐字节一致。
- 计数轴爆炸半径：`p0c52b` 脚本断言"除 2 行外逐字节相同"，实测通过。
- 用户文档一致性：`docs/QUEST_CATALOG.zh-CN.md` 的「定义文件」列刷新（`refresh_catalog_doc_links.py`：
  本片 24 行 + **1530 行历届切片欠账**共 1554 行改标注、悬空 0；复跑 `replaced=0` 幂等）。
- T1 固定清单扩容：`affected_quest_tests.py` 的 `T1_GATE_CLASSES` 18 → **21** 类
  （新增 `QuestKillCounterRetailGateTest` / `RetailHuntClientCountGateTest` / `RetailQuestAiNameGroupGateTest`），
  本片新增的常设门从此每次 T1 必跑。

## 9. 门禁

- **T1**（21 类 / **73 例**，含本片新增三门）**1F**：仅 `RetailDataDrivenGateTest.driftVersusShellsIsRegistered`
  （`20035` 登记码 `RETAIL_TALK_HUNT_CHAIN_DEFERRED` vs 实际 `RETAIL_ITEMPLAY_OUTPUT_UNRESOLVED`）——**车道在册**
  （`gates/T1-180704.log`，375s；扩表前 `T1-174415.log` 63 例同一条）。采集族本片期间由车道自愈（155/175 已消失）。
- **T2**（76 例，24 行 + 6 行族）**1F**：同上，本片 id 零命中。
- **T3**：见 §10。

## 10. T3 全量对拍（归因）

`gates/T3-175816.log`：**2011 例 116F/21E/1S**（137 条失败身份）。用
`p0c52_t3_attribution.py` 做两份日志的失败方法集差集，取得两次可复算结论：

1. **对本轮最近基线 `gates/T3-175259.log`（17:52:59，车道 P0c-45）**：`baseline=105 candidate=105`，
   **ADDED 0 / REMOVED 0** ⇒ 身份集**完全不变**。
2. **对本片 flip 后、门禁改造前基线 `gates/T3-170837.log`（17:08）**：`baseline=112 candidate=105`，
   **ADDED 0 / REMOVED 7**，消红 7 条全为正当闭合：

| 消红项 | 归属 |
|---|---|
| `Quest13765RetailAlignmentTest.preservesWeekly…`（NoSuchFile 13765.xml） | **本片**（换锚生产视图后绿） |
| `QuestKillCounterRetailGateTest.simulatorReproducesTheFixedOverkillDrift`（NoSuchElement） | **本片**（负对照换锚台阶） |
| `QuestKillCounterRetailGateTest.singleCounterQuests…`（`quest 13758 is not an executable owner`） | **本片**（目录换生产视图 + 台阶计数） |
| `QuestKillCounterRetailGateTest.killDeclarations…`（19 declaring < 90） | **本片**（人口护栏改集合对拍） |
| `RetailCombineTaskGateTest.frozenFingerprintsMatch`（574 行漂移） | 车道自愈 |
| `RetailDataDrivenGateTest.frozenFingerprints…`（5 行指纹） | 车道已重冻 |
| `RetailSimpleCollectItemGateTest.frozenFingerprints…`（155 vs 175） | 车道自愈 |

本片 id（13758–13769 / 23758–23769 / 13841 / 13845 / 13849 / 23841 / 23845 / 23849）
在 137 条失败身份与消息里**零命中**（机械 grep 计数 0）。T3 剩余红点（含车道 `20035` 码位、
以及其余长期债池）与本片改动面不可达。

## 11. 余量

`RETAIL_ACQUIRE_NPC_UNRESOLVED` 桶 62 → **38 行**（本片消 24）。余下 38 行按名字形状分五族
（`p0c52` 收口后实测，全表代码分布另见 drift 登记）：

| 形状 | 行数 | 例 |
|---|---|---|
| 道具/工作物符号名（`doc_quest_*` / `quest_*` / `QUEST_*`） | 15 | 13952、16809-16811、80885、80961 |
| Abyss 等级模板名（`Ab1_BLv4_L01` / `Ab1_BLv8_D`） | 6 | 15476/15477/15480 + 阿族孪生 |
| 哨兵占位名 `_challengetask_` | 6 | 17160-17162 + 阿族孪生 |
| 变体族形（`IDRaksha_Solo_StageStart` / `…_Dark`） | 2 | 18743、28743 |
| 事件/副本 NPC 名（`NPC_event_idevent_s2` / `event_npc_idsolo_s4`） | 9 | 50068-50105 |

其余桶（同表实测）：`MONSTER_UNRESOLVED` 56、`TALK_HUNT_CHAIN_DEFERRED` 55、
`HANDIN_VOCABULARY_UNSUPPORTED` 48、`TALK_CHAIN_DEFERRED` 23、`REWARD_NPC_UNRESOLVED` 19、
`SENTINEL_AREA_PENDING` 64（已证零真端区域绑定，勿从代码轴再攻）。
击杀门禁（真端化后）已无失明面；后续若出现"台阶 + 动作"混合形态，台阶规则会让位给动作口径，
需按新样本再裁定。
