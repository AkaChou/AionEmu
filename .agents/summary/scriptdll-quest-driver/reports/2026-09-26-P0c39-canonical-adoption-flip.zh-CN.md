# P0c-39：7 行 canonical 采纳 flip（18035/18807/18809/21070/24120/28035/28807）——7 个客户端死页收口 + 24120 阶梯形状裁定


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 切片窗口：2026-09-26（承接 P0c-38 登记的"10 行采纳 flip，先裁定接取入口通道"）。
> **做法**：flip 前先用探针把 10 行按**真端路径**编译并与客户端页对拍，与 **XML 侧**（现生产视图）逐行比致命类与未达页 ——
> 判准是"真端侧致命类不得比 XML 侧多"。结果**分成两批**：7 行真端侧 0 未达 0 致命（XML 侧尚有 7 个死页）⇒ 采纳；
> 3 行（21460/29070/29071）真端侧会**新增 1 条致命**（接取对话页 `1011 SELECT1 → 1012 SELECT1_1` 无路由）⇒ 保留 XML 并登记接取入口轴。
> 落地后 7 个死页全部转 `PAGE_ACTION_MATCHED`，`verify_retirement` `1344+4880=6224`，契约致命集**一条未增**。

## 1. 结论摘要

- **flip 前对拍（本片的关键动作）**：探针 `P0c39PreflipProbeTest` 把 10 行按真端编译器编译成定义、
  组 `ImmutableQuestCatalog` 后跑 `QuestDialogOrderAudit`，与生产视图（这 10 行 owner=`XML_RETENTION` ⇒ 走 XML）对拍：

  | 侧 | 未达/待证行 | 致命（`BUTTON_WITHOUT_ROUTE`/`PAGE_NOT_IN_TASK_HTML`） |
  |---|---:|---:|
  | XML（现生产） | 20 | **0** |
  | 真端（flip 后） | 14 | **3**（21460/29070/29071 各一条） |

  ⇒ 全量 flip 会**引入 3 条新致命契约行**。逐行拆开后：**7 行真端侧 0 未达 0 致命**（其中 5 行还把 XML 侧的死页闭合），
  **3 行**真端侧把非致命未达页升级成致命（页面已下发但按钮无路由）⇒ 本批只采纳 7 行。
- **采纳 7 行**：18035 / 18807 / 18809 / 21070 / 24120 / 28035 / 28807。
  - **7 个客户端死页闭合**（真端阶梯把每个阶段页交给自己的阶段 NPC）：18807 `2375`；18809 `1693`+`2375`；
    21070 `2375`；24120 `1693`+`1694`；28807 `2375`（18035/28035 本来就全 matched，属形状替换）。
  - **flip 后生产视图复核**：10 行仍全部 `executable=true`；7 行未达/致命 **0/0**，
    留存的 13 行未达全在 3 行 HOLD 里（`18035..28807` 在探针输出中一条未达都没有）。
- **24120 的阶梯形状裁定（真端对、XML 错）**：客户端 `quest_summary` 三行文本逐字
  `row0 → STR_DIC_N_Tree_Move_Nabalu`、`row1 → STR_DIC_N_DF2_Tree_Bellumus_Q24120A`、`row2 → STR_DIC_N_Tree_Move_Nabalu`，
  真端表 `talk_npc1=Tree_Move_Nabalu` / `talk_npc2=DF2_Tree_Bellumus_Q24120A` / `reward_npc_name=Tree_Move_Nabalu`——
  即**阶段 2 是 Bellumus（802441），交付/报告是 Nabalu（730038）**。XML 期把 Bellumus 当交付 NPC（`npc-complete npc-id=802441`）
  并给它派 `SELECT5` 报告页，于是 `select3/select3_1` 成了死页。真端阶梯（`started→s1→s2→reward`）与之逐轴一致 ⇒ **真端对**。
- **一条陈期望已按证据更新**：`ReportToManyDialogRouteRegressionTest` 把 24120 的 `(802441, 2375)` 写成期望——
  它正是上面 XML 期形状的产物。改为按客户端证据锁两段：`(802441, 1693)`（阶段 2 NPC → 本阶段页）+
  `(730038, 2375)`（报告 NPC → 报告页）。改后该测试只剩 3 条与本片无关的缺口（见 §5）。

## 2. 落地物

| 产物 | 路径 | 说明 |
|---|---|---|
| 裁定表 | `.agents/summary/scriptdll-quest-driver/p0c39-canonical-adoption-decisions.tsv` | 10 行逐行：7 `ADOPT` + 3 `HOLD`，带两侧未达/致命计数与闭合页 |
| flip 脚本 | `.agents/summary/scriptdll-quest-driver/p0c39_adopt_flip_rows.py` | dry-run/`--apply`；四副本 whipsaw 守卫；XML/目录前置存在性校验；幂等可重跑 |
| 清单 | `retail-xml-retention.tsv`（4 副本一致，含 `.agents` 快照按行手术） | 7 行 `XML_RETENTION → RETAIL_TABLE`，evidence = `p0c39-canonical-adoption-decisions.tsv basis=CANONICAL_ITEM_CHANNEL` |
| 目录 | `quest_definition_catalog.xml`（main + target） | 删 7 条 `<definition …>` 行：**1351 → 1344** |
| 生产 XML | `quest/definitions/quests/{7 行}.xml`（main + target） | 删除（源文件 tracked，内容留在 git 历史，`git show HEAD:<path>` 可复现） |
| 指纹 | `retail-simple-talk-chain-ir-fingerprints.tsv`（+target，md5 `5b410179…`） | 275 → **282** 数据行；set 对拍 **added=7 / changed=0 / removed=0**；插入保留文件既有布局（尾部历史块未动） |
| 回归测试期望 | `ReportToManyDialogRouteRegressionTest.java` | 24120 的 1 条陈期望 → 2 条客户端一致期望（见 §1/§3③） |
| 探针（临时） | `P0c39PreflipProbeTest.java` + `p0c39-probe-before.txt` / `p0c39-probe-after.txt` | flip 前后两轮；**树内已删**（归档 `.java.txt` + 输出 txt） |

## 3. 关键证据（可复核）

**① flip 前逐行对拍（探针原始输出，`p0c39-probe-before.txt`）**

```
=== XML statuses={CLIENT_PAGE_UNREACHED=20, PAGE_ACTION_MATCHED=78, TERMINAL_PAGE_REACHED=22}  FATAL-XML count=0
=== RET statuses={CLIENT_PAGE_UNREACHED=14, EVIDENCE_REQUIRED=3, PAGE_ACTION_MATCHED=57, TERMINAL_PAGE_REACHED=20}  FATAL-RET count=3
  FATAL-RET BUTTON_WITHOUT_ROUTE 21460 1011 1012 unaccepted + NPC 799258 + 31 -> unaccepted + page 1011
  FATAL-RET BUTTON_WITHOUT_ROUTE 29070 1011 1012 unaccepted + NPC 801220 + 31 -> unaccepted + page 1011
  FATAL-RET BUTTON_WITHOUT_ROUTE 29071 1011 1012 unaccepted + NPC 801221 + 31 -> unaccepted + page 1011
```

7 行（18035/18807/18809/21070/24120/28035/28807）在 RET 侧一条未达都没有；RET 侧 14 条未达全属 3 行 HOLD。

**② flip 后生产视图复核（`p0c39-probe-after.txt`）**

```
PROD 18035 executable=true nodes=5 transitions=36     （真端 IR：5–6 节点 / 36–39 路由）
PROD 21460 executable=true nodes=4 transitions=100     （HOLD：仍是 XML IR）
=== PROD statuses={CLIENT_PAGE_UNREACHED=13, PAGE_ACTION_MATCHED=75, TERMINAL_PAGE_REACHED=21}
FATAL-PROD count=0
```

未达 20 → 13：闭合的 7 页 = 18807 `2375`、18809 `1693`+`2375`、21070 `2375`、24120 `1693`+`1694`、28807 `2375`；剩余 13 行全在 21460(1)/29070(6)/29071(6)。

**③ 24120 的形状裁定（客户端 + 真端双证据）**

```
客户端 Dialogs/20000_29999/quest_q24120.html quest_summary（3 行）：
  row0 和[STR_DIC_W_DF2_SUB_A3]的[STR_DIC_N_Tree_Move_Nabalu]对话
  row1 和[STR_DIC_E_Bellumushill]的[STR_DIC_N_DF2_Tree_Bellumus_Q24120A]对话
  row2 和[STR_DIC_W_DF2_SUB_A3]的[STR_DIC_N_Tree_Move_Nabalu]对话
真端表：talk_npc1=Tree_Move_Nabalu(talk) / talk_npc2=DF2_Tree_Bellumus_Q24120A / reward_npc_name=Tree_Move_Nabalu
XML 期（git HEAD 版）：<node label="k1"> 单阶段 + <npc-complete npc-id="802441">（Bellumus 当交付）
真端 IR：started→s1(SETPRO1@730038 发物)→s2(SETPRO2@802441 收物)→reward(730038 展示 SELECT5=2375)
```

**④ 陈期望的隔离（58 条逐条复算，见 §5）**：改期望前 4 条 `MISSING`，其中仅 `24120 (802441,2375)` 属本片；
改后 3 条 `MISSING`（3914 / 80310 / 80311），均非本片族。

## 4. 门禁结果

| 门 | 结果 |
|---|---|
| 链指纹门 `RetailSimpleTalkChainGateTest` | **2/2 绿**（先红于"7 行 RETAIL_TABLE 但不在冻结指纹里" → dump 对拍 added=7 → 外科装入双副本） |
| 家族语义门 `RetailSimpleTalkGateTest` | **3/3 绿**（7 行转退役 ⇒ 分类取冻结登记值 `DIFF:*`，无需改漂移表） |
| 契约审计 `QuestDialogOrderAuditTest` | **17/17 绿** |
| 归属门 `RetailOwnershipGateTest` / 目录门 `QuestDefinitionCatalogManifestTest` / 白名单 | **4/4、10/10、1/1 绿** |
| 契约致命集 `QuestClientContractGateTest` | **44（与 flip 前逐字同数）**：本片 7 行贡献 0；该门基线为空 ⇒ 仍红，属既有债（P0c-36 起 44 未变） |
| 回归测试 `ReportToManyDialogRouteRegressionTest` | 58 条期望：4 `MISSING` → 本片 1 条按证据更新 → **3 条 MISSING（非本片族）** |
| 退役恒等式 `verify_retirement.py` | `catalog=1344 directory=1344 retired=4880 sum=6224` — OK |
| 副本一致性 | 清单×4 `6db19738…`（含 `.agents` 快照 7 行已翻）、目录×2 `d765a50d…`、指纹×2 `5b410179…` |
| T2（7 行 id） | `gates/T2-135139.log`：80 例 **6 红 5 类，与本片 flip 前逐类相同** —— `RetailSimpleCollectItemGateTest`（frozen 155 vs retired 175）、`RetailDataDrivenGateTest`×2、`JournalRewardRowRepairContractTest`（15613）、`QuestClientContractGateTest`（count=44）、`ReportToManyDialogRouteRegressionTest`（**仅剩 3914**，本片 24120 一条已按证据更新后通过）；**本片 7 个 id 在失败消息中零命中** |

## 5. 本片未收口 / 他车道红（登记，不代修）

- **3 行 HOLD · `ACCEPT_DIALOG_ENTRANCE`**：21460 / 29070 / 29071。真端 IR 已下发接取对话页 `1003 QUEST_ACCEPT_1`、
  `1004 QUEST_REFUSE_1`、`1011 SELECT1`、`1012 SELECT1_1`、`1013 SELECT1_1_1`、`4 SHOW_ASK_QUEST_ACCEPT_WINDOW`，
  但按钮（`1012`/`1013`/`1007`/`1002 1003`）没有对应路由 ⇒ 其中 `1011 → 1012` 被判致命。
  需按 P0c-32 的"接取入口两形放行"通道补页流后再采纳（或先补路由再 flip）。
- **`ReportToManyDialogRouteRegressionTest` 剩余 3 条 MISSING（非本片）**：
  `3914 npc=203752 page=1352`（SimpleUseItem 族）、`80310 npc=831384 page=1003`、`80311 npc=831387 page=1003`。
  探针逐条复算证明本片只影响 24120 那一条（已按证据修），这三条在 flip 前后**同样 MISSING**。
- **契约致命集 44 行**（`QuestClientContractGateTest`）：自 P0c-36 起逐字未变，基线文件为空（v1 = 0/0），
  属跨族既有债；本片 0 增。

## 6. 未验证 / 阻塞（PENDING）

- 运行时与客户端目检（PENDING）：7 行采纳后进游戏走一遍（接取 → 阶段 NPC 页 → 报告页 → 领奖），
  以及 24120 的 `select3/select3_1` 是否真在客户端点亮。按约束**未启动服务器**、未做客户端抽检。
- 探针已归档并删除：源码 `.agents/summary/scriptdll-quest-driver/P0c39PreflipProbeTest.java.txt`，
  输出 `p0c39-probe-before.txt` / `p0c39-probe-after.txt` / `p0c39-start-dialog-routes.txt` / `p0c39-expectation-replay.txt`；
  树内文件（含编译产物）已删，末轮 T2 之后本片绿不依赖探针类。

## 7. 命令索引

```bash
# 裁定（探针：真端侧 vs XML 侧对拍；源码见 .java.txt 归档，树内已删）
mvn -o -B test -Dtest=P0c39PreflipProbeTest

# 落地（dry-run → apply）
python3 -B .agents/summary/scriptdll-quest-driver/p0c39_adopt_flip_rows.py
python3 -B .agents/summary/scriptdll-quest-driver/p0c39_adopt_flip_rows.py --apply
python3 -B .agents/summary/scriptdll-quest-driver/verify_retirement.py

# 指纹（dump 对拍 → 外科插入；保留文件既有布局）
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest -Dretail.talkChain.fingerprintOut=/tmp/p0c39-fp.tsv

# 门禁
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest,RetailSimpleTalkGateTest,QuestDialogOrderAuditTest,\
RetailOwnershipGateTest,QuestDefinitionCatalogManifestTest,QuestClientContractGateTest,\
ReportToManyDialogRouteRegressionTest
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 18035 18807 18809 21070 24120 28035 28807
```
