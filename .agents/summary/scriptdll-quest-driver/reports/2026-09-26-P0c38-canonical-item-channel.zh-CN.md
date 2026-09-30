# P0c-38：canonical 行的 item 符号通道 —— 真端名索引优先 + 3 行物品置换修正


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 切片窗口：2026-09-26（承接 P0c-37 登记的"17 行 `ADOPTION_BLOCKED` 的采纳通道"）。
> **主产出**：canonical 合成器（`build_quest_client_talk_chain_steps.py`）新增**真端 item 名索引通道**
> （`item_name_index.tsv`：符号去 `ITEM_` 前缀 + 小写 → id），命中即用，序数通道仅作兜底，两通道分歧时真端赢。
> 效果：**10 行首次合成成功**（canonical 101 → 111，KEEP 缺口 0）；并修正 **2458/4905/4906** 三行被序数通道
> **错位置换**的物品 id —— 这三行是 `RETAIL_TABLE`（已采纳），故本片含**生产行为变更**，指纹重冻 3 行。

## 1. 结论摘要

- **问题一（合成失败）**：P0c-37 登记的 17 行 `ADOPTION_BLOCKED`，其共同点是 canonical 合成器解析
  `ITEM_*` 符号时只走**历史 `quest_data.xml` work_items 序数通道**；这些行在该通道**没有 work_items**
  （或符号不在其中），于是合成直接失败、登记表无 `N` 记录，阶梯通道因此不可用。
- **问题二（静默置换，更危险）**：同样的序数通道对**能解出**的行并不等于解对——4 个符号与 4 个 work_items
  按序配位时，真端的 `A/B/C/D` 会被配成 `D/A/B/C`。本片逐符号对拍真端名索引后确认 **3 行共 10 条分歧**
  （见 §3.2），而这三行恰好是**已采纳（`RETAIL_TABLE`）**行 ⇒ 生产里的物品链此前是**倒着走**的。
- **处置**：符号解析改为 `真端 item 名索引 → （无解才）quest_data 序数`；两通道都无解仍 **fail-closed**。
  落地留痕 132 条：`MATCH 111 / INDEX_ONLY 10 / DIVERGED_RETAIL_WINS 10`（分歧一律取真端值并在表内留痕）。
- **10 行新合成是惰性的**：这 10 行 owner = `XML_RETENTION`，生产仍由 XML 拥有；登记表合成记录只进链门的
  KEEP 桶。**采纳需另做 manifest flip**（owner + 目录 + XML 删除），属后续切片（§5）。
- **3 行置换修正是生效的**：修正后物品链严格递进（2458 `194→195`；4905 `071→072→073→074`；
  4906 `075→076→077→078`，见 §3.2），指纹 dump 对拍 **changed=[2458,4905,4906]、added=[]、removed=[]**。

## 2. 落地物

| 产物 | 路径 | 说明 |
|---|---|---|
| 生成器（改动本体） | `.agents/summary/scriptdll-quest-driver/build_quest_client_talk_chain_steps.py` | `synthesize_canonical` 内新增 `item_id(sym)` 双通道解析 + 模块级 `item_channel` 留痕 + 守卫放宽（符号可经真端索引解即放行） |
| 通道留痕表 | `.agents/summary/scriptdll-quest-driver/p0c38-canonical-item-channel.tsv` | 132 条；列 = `quest_id / symbol / retail_index_id / work_items_id / verdict` |
| 生成器运行收据 | `.agents/summary/scriptdll-quest-driver/p0c38-generate.log` | 106 行；回显 `CANONICAL-INDEX` ×10 + `canonical 合成：111 行；缺口（KEEP）：0 行 []`；**重跑幂等**（两个输出 md5 不变） |
| 登记表 | `src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv`（+ target 同 md5 `6fefccab93b30725cbc1bef0131fdf59`，5112 行） | 10 行新增合成；2458/4905/4906 的 `R` 行动作串改为真端 id |
| 指纹 | `src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv`（+ target 同 md5 `ba309697552d4830c0a362f6fcf00cc6`，275 数据行） | dump 对拍后外科替换 3 行（§3.2） |
| 漂移登记 | `src/test/resources/quest/retail-simple-talk-drift.tsv`（+ target 同 md5 `69f9e2159eb3164b9bd390bb1eb853e8`，2223 数据行） | dump 对拍后外科替换 10 行（§3.6） |
| 探针（源码归档，树内已删） | `.agents/summary/scriptdll-quest-driver/P0c38ChannelProbeTest.java.txt` + `p0c38-probe.txt` | 273 行：10 行生产视图可执行 + 节点/路由/掉落 + AUDIT 逐页 + 三行 give/remove 逐字 |
| 阻塞登记（更新） | `.agents/summary/scriptdll-quest-driver/p0c38-blocked-rows.tsv` | 本片后仍未收口的轴（§5） |

## 3. 关键证据（可复核）

**① 通道留痕表（132 条）直方图**

```
MATCH 111          # 两通道一致（历史行，未被本片改动）
INDEX_ONLY 10      # 仅真端索引可解（= 本片新合成的 10 行）
DIVERGED_RETAIL_WINS 10   # 两通道分歧，取真端（= 2458/4905/4906 的 10 条）
```

`INDEX_ONLY` 的 quest：18035 / 18807 / 18809 / 21070 / 21460 / 24120 / 28035 / 28807 / 29070 / 29071。

**② 3 行置换修正：逐字前后对拍（最重要的一条）**

指纹 dump 对拍（两侧均 275 数据行）：

```
$ diff <(grep -v '^#' /tmp/p0c37-fp.tsv) <(grep -v '^#' /tmp/p0c38-fp.tsv)
66c66   2458  f8973285… -> 0ed7b066…
158,159c158,159
        4905  e0fff9d0… -> 408515dc…
        4906  e5107dff… -> 6c90d6a4…
```

即 `added=[]、removed=[]、changed=[2458,4905,4906]`。分歧表的 10 条（真端索引 vs 序数通道）：

```
2458 ITEM_DOC_QUEST_2458B 182204195 / 182204194   ITEM_QUEST_2458A 182204194 / 182204195
4905 ITEM_QUEST_4905A     182207071 / 182207074   ITEM_DOC_QUEST_4905B 182207072 / 182207071
     ITEM_DOC_QUEST_4905C 182207073 / 182207072   ITEM_DOC_QUEST_4905D 182207074 / 182207073
4906 ITEM_QUEST_4906A     182207075 / 182207078   ITEM_DOC_QUEST_4906B 182207076 / 182207075
     ITEM_DOC_QUEST_4906C 182207077 / 182207076   ITEM_DOC_QUEST_4906D 182207078 / 182207077
```

序数通道把 4 元组按 `D/A/B/C` 配位（`A` 拿到 `D` 的 id），修正后登记表 `R` 行动作串（`quest_client_talk_chain_steps.tsv`）：

```
2458 R seq5 npc=204386 SETPRO1 started->s1  acts=GIVE_ITEM:182204195:1;REMOVE_ITEM:182204194:1
4905 R seq5 npc=205155 SETPRO1 started->s1  acts=GIVE_ITEM:182207072:1;REMOVE_ITEM:182207071:1
4905 R seq8 npc=205156 SETPRO2 s1->s2       acts=GIVE_ITEM:182207073:1;REMOVE_ITEM:182207072:1
4905 R seq11 npc=205157 SETPRO3 s2->s3      acts=GIVE_ITEM:182207074:1;REMOVE_ITEM:182207073:1
4906 R seq5/8/11 同形：076/075、077/076、078/077
```

生产视图（`RetailQuestDriver.overlay` → `findExecutable`）逐字复核同一个链：
`4905 T started->s1 … act=[GiveItem[182207072,1], RemoveItem[182207071,1]]`、`s1->s2 … 073/072`、
`s2->s3 … 074/073`——严格递进，无回流。修正前该链为 `074 → 071 → 072 → 073`（接取即给末期物品）。

**③ 10 行新合成：形状 + 页面可达性（生产视图审计）**

| quest | 节点投影 | transitions | AUDIT 状态 |
|---|---|---|---|
| 18035 | `started(0)/s1(1)/reward(1)/complete(0)` | 27 | MATCHED 5 / TERMINAL 2 |
| 28035 | 同上 | — | MATCHED 5 / TERMINAL 2 |
| 18807 | `started(0)/reward(1)/complete(0)` | 59 | MATCHED 6 / UNREACHED 1 / TERMINAL 2 |
| 21070 | `started(0)/reward(1)/complete(0)` | 65 | MATCHED 14 / UNREACHED 1 / TERMINAL 2 |
| 21460 | `started(0)/reward(1)/complete(0)` | — | MATCHED 24 / UNREACHED 1 / TERMINAL 3 |
| 18809 | `started(0)/s1(1)/reward(2)/complete(0)` | 87 | MATCHED 8 / UNREACHED 2 / TERMINAL 3 |
| 24120 / 28807 | — | — | MATCHED 6 / UNREACHED 2 与 1 |
| 29070 / 29071 | — | — | MATCHED 2 / **UNREACHED 6** / TERMINAL 2 |

`CLIENT_PAGE_UNREACHED` 是审计的**登记桶**（非致命；`QuestDialogOrderAuditTest` 要求为空的只是
1149/1913/25512/1993 系等指名行，见 §4）。这 10 行共 20 行 UNREACHED，其中 **29070/29071 各 6 行**
是已知缺口（需补 `1013/quest_accept/ask_quest_accept` 页流），沿用 P0c-37 登记，本片不裁。

**④ "对 XML 拥有行是惰性的"（10 行不进生产）**

- `retail-xml-retention.tsv`：10 行 owner 均为 `XML_RETENTION`（对比：1479/2458/4905/4906 为 `RETAIL_TABLE`）。
- 链门 `registryPartitionsIntoAdoptAndKeepWithoutOrphans`：`RETAIL_TABLE` 行必须在冻结指纹里；
  `XML_RETENTION` 行只进 KEEP 桶 —— 10 行登记记录全部落在 KEEP，指纹文件里查无这 10 行（实测 0 命中）。
- 因此本片对这 10 行的**生产行为零影响**；它们只是"采纳素材已就位"，采纳仍待 flip。

**⑤ 生成器幂等（重跑收据）**

```
chain_steps: 6fefccab93b30725cbc1bef0131fdf59 -> 6fefccab93b30725cbc1bef0131fdf59
evidence:    648c792f992ac806cd14e8b67cf8b22c -> 648c792f992ac806cd14e8b67cf8b22c
```

**⑥ 漂移登记 diff（2223 数据行两侧，恰好 10 行变化）**

```
18035/28035/29070/29071  REJECTED:RETAIL_TALK_CHAIN_COMPOUND -> DIFF:TRANSITION_SET
18807/18809/21070/21460/24120/28807  -> DIFF:NODE_PROJECTION
```

`2458/4905/4906/1479` 的漂移行**不动**：前三行已退役（XML 只在 git 历史），族门按设计对退役行直接采用
冻结登记值（`REJECTED:RETAIL_TALK_CHAIN_COMPOUND` 是采纳当时的分类快照，登记本就不是退役门槛）；
1479 的 `DIFF:NODE_PROJECTION` 保持 P0c-37 值。

## 4. 门禁结果

| 门 | 命令 | 结果 |
|---|---|---|
| 链指纹门 | `mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest` | **2/2 绿**；本片先红于 2458/4905/4906 指纹偏离 → `-Dretail.talkChain.fingerprintOut=/tmp/p0c38-fp.tsv` dump 对拍（changed=[2458,4905,4906]/added=[]/removed=[]）→ 外科装入双副本后转绿 |
| 家族语义门 | `mvn -o -B test -Dtest=RetailSimpleTalkGateTest` | **3/3 绿**（含 `driftVersusLegacyXmlIsRegistered`，10 行 dump 对拍后装入） |
| 契约审计 | `mvn -o -B test -Dtest=QuestDialogOrderAuditTest` | **17/17 绿** |
| 复跑（探针移出后净树） | 同三条门一次跑 | **22/22 绿 / 0 失败**（`Tests run: 22, Failures: 0, Errors: 0`，`/tmp/p0c38-gates-final.log`）——证明本片绿不依赖探针类 |
| T2（14 id） | `run_quest_gates.sh T2 2458 4905 4906 1479`（`gates/T2-131111.log`） | 67 例 **5 红，全归因他车道**（下表） |
| 退役恒等式 | `python3 -B verify_retirement.py` | `catalog=1351 directory=1351 retired=4873 sum=6224 — OK` |
| 副本一致性 | `md5 -q` | 登记表 `6fefccab…`、指纹 `ba309697…`、漂移 `69f9e215…` 均 src↔target 相等 |

T2 五红的归因证据（本片 14 个 id 在四条红里**零命中**）：

| 红 | 证据 |
|---|---|
| `QuestClientContractGateTest`（count=44） | 44 行的 quest id 集与**改前** 10:19 基线（`T2-101946.log`，同 count=44）**逐字相同**（`diff` ⇒ `IDENTICAL_ID_SETS`）；全为 `BUTTON_WITHOUT_ROUTE`（1011/11068/11072/11139/79xxxx 等**另一族**）⇒ 本片 0 delta |
| `JournalRewardRowRepairContractTest`（quest 15613） | 契约期望恢复条件 `var0=5` 实际 `0`；15613 ∉ 本片 14 id，测试文件 mtime 09-25、其数据在 DataDriven/Collect 族；窗口内 `QuestCollectProgressAlignmentGateTest.java` mtime 13:18（**落在本轮 T2 运行中**）⇒ 并行车道在飞 |
| `RetailSimpleCollectItemGateTest`（frozen=155 / retired=175） | 10:19 起每一轮都红（`T2-101946` 起）⇒ 先于本片落盘（13:05） |
| `RetailDataDrivenGateTest`×2 | 12:30 起红（`T2-123039`），早于本片落盘；属 DataDriven lane 的 20035 拒绝码与 5 行指纹演进 |

## 5. 采纳前置（后续切片）与阻塞登记

- **10 行的采纳 flip 未做**：`XML_RETENTION → RETAIL_TABLE`（owner + 目录 + XML 删除，P0c-35 双轴 flip 模式）。
  本片只把"素材"备齐；flip 之后这 10 行才进链门 ADOPT 桶、才需要指纹，也才能套阶梯收口那 20 行 UNREACHED。
- **29070/29071 的 6+6 不可达页**：需补 `1013 / quest_accept / ask_quest_accept` 页流（客户端页链证据），
  与 flip 同批处理。
- **仍未合成的 `GENUINE_GAP`**：1324 / 2428（普查 `-`、登记表无记录）、11001 / 21138 / 30055 / 30202 / 30302
  （`con_quest`/`cutscene` 延迟波）、18806 / 28806（cutscene）——它们的符号轴之外还有 **compound 分解 / cutscene 轴**
  未裁定；24123 仍卡 `XML_NPC_AXIS`（204345 Rikesh vs 真端 Ananta→204387；reward 1 vs 末行 2）。
- **canonical 通道换源**（登记表从"XML 对照"彻底换到"真端形状"全量合成）仍未开。

## 6. 未验证 / 阻塞（PENDING）

- 运行时/客户端目检（PENDING）：2458/4905/4906 修正后的物品链需实际进游戏核对（接取给 A、每阶段换 B/C/D）；
  10 行采纳后需目检页流。按约束**未启动服务器进程**，未执行客户端抽检。
- T2 的四条他车道红（44 行 fatal / CollectItem 冻结集 / DataDriven 2 条 / 15613 契约）本片不代修，
  归因证据在 §4；`QuestClientContractGateTest` 的 44 行属其自身登记演进。

## 7. 命令索引

```bash
# 落地（幂等：重跑两个输出 md5 不变）
python3 -B .agents/summary/scriptdll-quest-driver/build_quest_client_talk_chain_steps.py \
  > .agents/summary/scriptdll-quest-driver/p0c38-generate.log

# 指纹重冻（只能经 dump 对拍后外科替换）
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest -Dretail.talkChain.fingerprintOut=/tmp/p0c38-fp.tsv
diff <(grep -v '^#' /tmp/p0c37-fp.tsv) <(grep -v '^#' /tmp/p0c38-fp.tsv)

# 漂移登记重算（同法：dump → 逐行对拍 → 外科替换）
mvn -o -B test -Dtest=RetailSimpleTalkGateTest -Dretail.talk.equivOut=/tmp/p0c38-drift.tsv

# 门禁
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest,RetailSimpleTalkGateTest,QuestDialogOrderAuditTest
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 2458 4905 4906 1479
python3 -B .agents/summary/scriptdll-quest-driver/verify_retirement.py

# 探针（源码见 .java.txt 归档；树内已删）
mvn -o -B test -Dtest=P0c38ChannelProbeTest
```
