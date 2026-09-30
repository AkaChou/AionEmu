# 2026-09-26 续片 25 / P0c-42：物件哨兵接取入口 —— 1323 接取阶梯改绑物件 owner（1011 入口页轴收口）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

## 1. 结论

台账登记的 `ACCEPT_PAGE_NOT_EMITTED`（"客户端有 `select1(1011)` 页但真端 IR 没有任何转换下发该页"）

在 P0c-41 记 48 行，本片探针实测 **46 行**（客户端页册含活跃 1011 页且任务可执行的行）。按 owner
分解后该轴的**真缺陷面只有 1 行**：

| 分解 | 行数 | 性质 |
|---|---|---|
| `RETAIL_TABLE` / SimpleTalk / order1 | **1**（**1323**） | **真缺陷**：物件哨兵接取者的接取阶梯丢失 ⇒ 玩家打不开接取窗。本片修复 |
| `RETAIL_TABLE` / DataDriven / order2 | 1（25070） | **族形不同**：1011 是该族的 FOBJ 交互推进页，不是接取入口（见 §6）。登记为独立轴 |
| `XML_RETENTION`（order1–7，含 SimpleTalk 2 / DataDriven 15 / 无族 22） | 44 | **非本轴**：IR 来自遗留 XML 而非真端链编译器；随各自族采纳自然消失（采纳前须按族门做双侧契约对拍，QE-066） |

**修复效果（修复面 = 缺陷面）**：1011 缺口 46→**45**（`removed={1323}`、`introduced=[]`）；1323 的审计从
3 条 `CLIENT_PAGE_UNREACHED`（页 1011 / 4 / 1004）变为 **0**（11 行全 `PAGE_ACTION_MATCHED`/`TERMINAL`，
接取侧四条路由全部绑定物件 730032）；链式冻结指纹 `changed={1323}` / `added=0` / `removed=0`；契约门
fatal 保持 **0**；净树复跑 20/20 全绿（链门 2 + 审计 17 + 契约门 1）。

**本片第二个交付物是耐久性**：登记表 `quest_client_talk_chain_steps.tsv` 是**生成物**，直接手改会被下次
重生成静默回退。修复因此落进生成器（`build_quest_client_talk_chain_steps.py` 新增
`rebind_accept_entrance` + 裁定表 `p0c42-accept-entrance-decisions.tsv`，六轴 fail-closed），落地判据是
**生成器重跑输出与人工编辑结果逐字节相同**（md5 `badc6ee20acfd17cb66ca04005a7e206`，且 8 张既有裁定表零漂移）。

## 2. 缺口形状（判例 1323）

### 2.1 症状

客户端 `QUEST_Q1323.html` 的接取段有 4 页，审计全部"未达"或错配：

| 页 | 页序 | 客户端按钮（CSV 逐字） | 修复前审计 |
|---|---|---|---|
| `select1`(1011) | 1 | 1007 `HACTION_ASK_QUEST_ACCEPT`「打开盖子看看。」 | `CLIENT_PAGE_UNREACHED` |
| `ask_quest_accept`(4) | 2 | 1002 `HACTION_QUEST_ACCEPT_1`「接受。」/ 1003 `HACTION_QUEST_REFUSE_1`「拒绝。」 | `CLIENT_PAGE_UNREACHED` |
| `quest_accept_1`(1003) | 3 | 1008 `HACTION_FINISH_DIALOG`「结束对话。」 | `PAGE_ACTION_MATCHED`（靠一条**无主** `QUEST_ACTION + 1002` 侥幸匹配） |
| `quest_refuse_1`(1004) | 4 | 1008 `HACTION_FINISH_DIALOG`「结束对话。」 | `CLIENT_PAGE_UNREACHED` |

即：**玩家与箱子交互后没有任何页下发，接取窗永远打不开 ⇒ 任务不可接取**。

### 2.2 真端侧事实

`Quest_SimpleTalk.xml` 1323：`acquired_npc_name=LF2_Lost_JewelBox`（箱子对象 **730032**）、
`give_item=ITEM_QUEST_1323A 1`、`talk_npc1=Tree_NoMove_Lodas`(730019)、`reward_npc_name=Justachys`(203939)。
即**接取者是物件**（物件哨兵），接取阶梯必须锚在该物件上。

### 2.3 登记表里的形状（缺陷现场）

```
1323  Q  4  QUEST_REFUSE_1   unaccepted unaccepted - - CLOSE                  -
1323  Q  5  QUEST_ACCEPT_1   unaccepted started  START_ELIGIBLE - SYNC:…;DIALOG:…:QUEST_ACCEPT_1 -
1323  Q  6  FINISH_DIALOG    unaccepted unaccepted - - CLOSE                  -
1323  Q  7  FINISH_DIALOG    started    started    - - CLOSE                  -
…
1323  R  1  730032  USE_OBJECT  unaccepted unaccepted - GIVE_ITEM:182201309:1  -   RETAIL_MATCH  -  -
```

四条 `Q` 记录是**无主**的（`Q` = `QUEST_ACTION` 无目标事件，npcId 记 0，见 `RetailClientTalkChainSteps`
记录类型），而唯一的物件路由 `R 1 USE_OBJECT` **丢了下发页**（`after='-'`）⇒ 入口页从未下发；
无主阶梯连"谁下发这些页"都没写，接取窗页与拒绝页双双落空。这正是 `p0c10h-chain-keep-closure.tsv`
记的 `NO_START_OBJECT`（"acquired_npc 为物件哨兵：接取经物件交互，链表无物件起始词汇"）。

## 3. 修法与证据链（三轴 fail-closed）

### 3.1 落地形状（登记表原位改写）

```
1323  R  1  730032  QUEST_REFUSE_1    unaccepted unaccepted - -  DIALOG:SHOW_QUEST_PAGE:QUEST_REFUSE_1   RETAIL_MATCH  QUEST_REFUSE_1=CLIENT  -
1323  R  2  730032  QUEST_ACCEPT_1    unaccepted started  START_ELIGIBLE - SYNC:VISIBILITY_REFRESH;DIALOG:SHOW_QUEST_PAGE:QUEST_ACCEPT_1  RETAIL_MATCH  QUEST_ACCEPT_1=CLIENT  -
1323  R  3  730032  FINISH_DIALOG     unaccepted unaccepted - -  CLOSE   RETAIL_MATCH  -  -
1323  R  4  730032  FINISH_DIALOG     started    started    - -  CLOSE   RETAIL_MATCH  -  -
1323  R  5  730032  USE_OBJECT        unaccepted unaccepted - GIVE_ITEM:182201309:1  DIALOG:SHOW_QUEST_PAGE:SELECT1  RETAIL_MATCH  SELECT1=CLIENT  -
1323  R  6  730032  ASK_QUEST_ACCEPT  unaccepted unaccepted - -  DIALOG:SHOW_QUEST_PAGE:SHOW_ASK_QUEST_ACCEPT_WINDOW  RETAIL_MATCH  SHOW_ASK_QUEST_ACCEPT_WINDOW=GLOBAL  -
```

形状逐字取自同族已采纳兄弟行（2611/3001/24123/35010/35017 的 `ASK_QUEST_ACCEPT →
DIALOG:SHOW_QUEST_PAGE:SHOW_ASK_QUEST_ACCEPT_WINDOW` + `…=GLOBAL`）。审计 `sameDialogOwner` 要求
"下发页的路由"与"页按钮的路由"同 owner，故接取阶梯四条路由全部绑箱子 730032。

### 3.2 生成器侧六轴断言（`rebind_accept_entrance`）

1. 该任务无 `B NPC_START` 块（物件哨兵形状：接取不经 NPC 块）；
2. 物件交互路由唯一且丢页（`after='-'` 且 `page_check='-'`，重复应用即中止）；
3. 入口页唯一按钮常量 = `HACTION_ASK_QUEST_ACCEPT`；
4. 接取窗页为引擎常页：`page_constant` 去 `HTML_PAGE_` 前缀的符号在 `QuestDialogPage` 内存在，**且其枚举号 == 客户端页册的页号**（实测 4 == `SHOW_ASK_QUEST_ACCEPT_WINDOW(4)`）；
5. 阶梯结果页唯一按钮常量 = `HACTION_FINISH_DIALOG`，且两页关闭按钮一致；
6. 无主 `Q` 动作集 == 阶梯页名集 ∪ {`FINISH_DIALOG` × |阶梯页|}，且关闭出口的 status 集合 == 阶梯页目标 status 集合（`unaccepted`→页 1004、`started`→页 1003 一一对应）。

接取窗页的按钮按**常量名**校验而非 id（客户端两套 id 空间不同号：按钮 1002 `HACTION_QUEST_ACCEPT_1`
的目标页是 1003）。

### 3.3 效果（修复面 = 缺陷面）

| 观测量 | 修复前 | 修复后 |
|---|---|---|
| 1011 缺口任务数 | 46 | **45**（`removed=1323`、`introduced=[]`） |
| 1323 审计 | 3×`CLIENT_PAGE_UNREACHED` + 1×无主 `QUEST_ACTION` 匹配 | **11 行全 MATCHED/TERMINAL，0 未达** |
| 1323 IR 转换数 | 31 | 32（+1 = 新 `ASK_QUEST_ACCEPT` 边） |
| 链式冻结指纹 | `1323=d6ff810e…` | `1323=9e3f26da…`（`changed={1323}`，284 行不动） |
| 契约门 fatal | 0 | **0** |

## 4. 落地物

| 文件 | 说明 |
|---|---|
| `src/main/resources/…/quest_client_talk_chain_steps.tsv`（+`target/classes` 副本） | 1323 接取侧 13 行改写 + 1 行插入（R seq 重编到 14）；md5 `badc6ee20acfd17cb66ca04005a7e206` |
| `.agents/…/build_quest_client_talk_chain_steps.py` | 新增 `rebind_accept_entrance()` + 裁定表加载 + 按钮常量/页常量两个取证通道（`client_action_constants` / `client_page_constants` / `page_enum_ids`） |
| `.agents/…/p0c42-accept-entrance-decisions.tsv` | 裁定表（7 列，生成器输入；含六轴判据说明） |
| `.agents/…/p0c42_accept_entrance_repair.py` | 一次性外科手术脚本（首轮落地用；此后由生成器接管，脚本保留为审计痕迹） |
| `.agents/…/p0c42_builder_fidelity_check.py` | 生成器忠实性检查（`OUT` 重定向跑全量生成 → 与登记表逐字节比对 + 8 张裁定表零漂移断言；`FIDELITY_OK`） |
| `.agents/…/p0c42_refreeze_fingerprints.py` | 指纹外科重冻（`EXPECTED_CHANGED=("1323",)` + PRE 值漂移守卫 + whipsaw 守卫） |
| `.agents/…/p0c42-accept-page-gap-{pre,post}.tsv` | 轴全量清单（各 4073 行：4067 carriers 的 EMITTED/GAP 逐行身份 + 汇总行） |
| `.agents/…/p0c42-accept-page-blast.tsv` | 前后差分（`removed=1 introduced=0`） |
| `.agents/…/p0c42-1323-audit-{pre,post}.tsv` | 1323 审计逐行前后对照（10 行 → 11 行） |
| `.agents/…/p0c42-blocked-rows.tsv` | 残留轴登记（25070 族形资质、44 行 XML_RETENTION、其他承轴、生成器耐久性注记） |
| `.agents/…/P0c42AcceptPageNotEmittedProbeTest.java.txt` | 探针源码归档（树内 `.java` 与 `.class` 已删除） |
| `src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv`（+target 副本） | 外科重冻 1323 一行（288 行布局逐字节保留；双副本 md5 `55302172e2f95cce2b9b324b8711113c`） |

## 5. 验证

| 门 | 结果 |
|---|---|
| 生成器忠实性 | `python3 -B .agents/summary/scriptdll-quest-driver/p0c42_builder_fidelity_check.py` → `FIDELITY_OK: 生成器重跑输出与登记表逐字节相同（431533 字节）`、`REGISTRY_MD5 badc6ee20acfd17cb66ca04005a7e206`、`DECISION_TABLES_STABLE: 8 张裁定表零漂移`。首次运行时（本片修复前）该检查的**唯一**差异恰为 1323 的 14 行 ⇒ 生成器确定性 + 本片修复可重生成 |
| 链门（校验轮，无 `-D`） | `RetailSimpleTalkChainGateTest` 2/2 绿（重冻后） |
| 契约门 | `QuestClientContractGateTest` 1/1 绿，fatal 0 |
| 审计 | `QuestDialogOrderAuditTest` 17/17 绿 |
| 净树复跑 | 链门 2 + 审计 17 + 契约门 1 = **20/20**（探针删除后） |
| T1（18 类 63 例） | **3F**（`gates/T1-154045.log`）：CollectItem 冻结 155/退役 175 + DataDriven 指纹 5 条 + DataDriven 20035 漂移登记 —— 与 P0c-41 基线 `T1-143553` **同名同条** ⇒ 零新增 |
| T2（1323，75 例） | **5F**（`gates/T2-154648.log`）：上 3 条 + `LegacyTemplateMirrorRouteRegressionTest`(1131 action 10000) + `QuestStartItemDefinitionRegressionTest`(1197 NPC 700004)。后两条在 09:26–14:27 的多轮 T2/T3 日志在册（chronic），且 1131/1197 均不在本片改动面 |
| T3（全树复跑，`gates/T3-155440.log`） | 2003 例 / 124F+23E。与并发 lane 的 `T3-153216`（2007 例 / 122F+21E）身份差集 = **−1 / +5**：`−1` = 本片在 15:32 窗口留下的链门瞬态（重冻已闭合，见 §5.1）；`+5` = 并发 lane P0c-51 的 **25052 半途翻窗**（见 §5.1）。用例数 −4 = 那 5 条里的 2 个类在 `@BeforeAll` 报错导致其方法不再计数 |

### 5.1 两个瞬态窗口的归因（都不是本片缺陷）

- **链门瞬态（已闭合）**：并发 lane 的 T3（15:32:16 起跑）窗口落在"登记表已改、指纹未重冻"之间 ⇒ 唯一
  新增失败 `RetailSimpleTalkChainGateTest.adoptRowsReplayRegistryAndMatchFrozenFingerprints`。15:39 重冻后
  该门在**三处独立运行**转绿（链门单跑、T1、T2）。
- **25052 半途翻窗（并发 lane）**：其 `retail-xml-retention.tsv` mtime **15:57:08**（正在本片 T3 窗口
  15:54:40–16:03 内），且该轮引用的 `p0c51-fobj-collect-decisions.tsv` 当时尚不存在；5 条新增全是
  "清单/所有权一致性"类（RetailOwnership / RetailMetadataEquivalence / QuestRewardItem /
  QuestRewardValue / RetailDataDriven.retentionManifestMatchesDriverOwnership），与 1323 零交集。
  已登记进 `p0c42-blocked-rows.tsv` 的 `OTHER_LANE_IN_FLIGHT`。

## 6. 残留与下一步

1. **25070（`DATADRIVEN_SELECT1_IS_FOBJ_STEP_PAGE_NOT_EMITTED`）**：客户端 order1 = `select_none(4762)`
   （按钮 20000/20001 接受/拒绝）才是接取入口；1011 在本族是 order2 的 **FOBJ 交互推进页**
   （10255 `HACTION_SET_SUCCEED`「取下水晶。」+1008「停止。」），对应真端 `started --TalkToNpc(731552,
   USE_OBJECT)--> reward`（`SetVariable(var0,1)` + `PACKET_ONLY`）。P0c-50 实测"两侧未达页同为 1011"
   ⇒ 遗留 XML 侧同病、非采纳回归。裁定要点 = 本族 1011 是否应由服务端下发（`PACKET_ONLY` vs 页下发）。
2. **44 行 `XML_RETENTION` 的 1011 未达**：随各自族采纳消失，采纳前按 QE-066 做双侧契约对拍；
   不得因为是"非致命未达页"就跳过（QE-066 已实证采纳会把非致命升级成致命）。
3. **接取页梯余量**（承接 P0c-40 的 42 行 + 本片）：审计"已确认接取对话"过滤集只覆盖按钮 1007 与页
   4/1003/1004，**不覆盖 1011 页**（QE-067 边界）⇒ 这类缺口只能靠空基线的契约门现形；
   剩余 11 行 RETAIL_TABLE 接取页梯待按行裁定。
4. 其他承轴见 `p0c42-blocked-rows.tsv`（`SELECT6_CLOSE_EXIT_OTHER_FAMILIES` 62 行、`NOT_IN_CANONICAL_SET`、
   `DEFERRED_BLOCK`、`CUTSCENE_AXIS`、`24123 XML_NPC_AXIS`、`RUNTIME_PENDING`）。

## 7. PENDING（需授权）

运行时与客户端目检（1323 与箱子 730032 交互 → 入口页「打开盖子看看」→ 接取窗 → 接受/拒绝 → 结果页
「结束对话」；含 21460/29070/29071、2458/4905/4906、P0c-40 的 3 行、P0c-39 的 7 行、P0c-41 的 1932/3092）。
按约束**未启服**，本轮未执行任何服务器生命周期操作。另：全树 T3 在并发 lane 活跃时无法给出干净的
"零新增"测量（本片以并发 lane 两轮 T3 身份差集 + 净树 20/20 替代）。

## 8. 命令索引

```
python3 -B .agents/summary/scriptdll-quest-driver/p0c42_accept_entrance_repair.py --apply
python3 -B .agents/summary/scriptdll-quest-driver/p0c42_builder_fidelity_check.py   # 生成器忠实性：逐字节 + 裁定表零漂移
python3 -B .agents/summary/scriptdll-quest-driver/p0c42_refreeze_fingerprints.py /tmp/p0c42-fp.tsv --apply
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest -Dretail.talkChain.fingerprintOut=/tmp/p0c42-fp.tsv
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 1323
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T3
mvn -o -B test -Dtest='RetailSimpleTalkChainGateTest,QuestClientContractGateTest,QuestDialogOrderAuditTest'
```

## 9. 并发车道注记

本片期间另一车道为 **P0c-48/49/50/51**（DataDriven TalkFOBJ 词汇与单步行采纳、25070/25052/15673/25673/
80846/80847 等）。共享面只有 `retail-xml-retention.tsv` 与目录/XML，与登记表/指纹无交集；两次跨窗口
瞬态（链门、25052）已在 §5.1 逐条归因。**本片未触碰并发 lane 的任何文件**。
