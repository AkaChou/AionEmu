# P0c-40 接取对话页链：链式路径补 acceptContinuation + 3 行 HOLD 采纳

- 日期：2026-09-26
- 切片：P0c-40（承 P0c-39 的 3 行 `ACCEPT_DIALOG_ENTRANCE` HOLD）
- 台账：`../GOAL-retail-driver-progress.zh-CN.md`
- 探针（已归档，不入测试树）：`../P0c40AcceptChainProbeTest.java.txt`

## 1. 结论（TL;DR）

**P0c-39 的 3 行 HOLD 不是"登记表缺记录"，而是编译器链式路径的缺口**：`buildChain` 的
`NPC_START` 块合成接取流（`acceptFlowChain`）时**没有**像单步路径那样补客户端接取页梯
（`SELECT1_1` / `SELECT1_1_1`）。后果是 `select1` 页（客户端 1011）的"继续听"按钮
（`HACTION_SELECT1_1`，动作 id 1012）在 IR 里无路由 ⇒ 审计报 `BUTTON_WITHOUT_ROUTE`。

处置与效果：

1. **编译器补丁**（`RetailSimpleTalkDefinitionCompiler.buildChain`）：NPC_START 块路径按
   `RetailClientDialogExits` 补 `acceptContinuation`，与单步路径同形同源（客户端出口登记是
   真端模板表没有页链列时的唯一权威）。
2. **全目录同轴缺口一次清零**：修复前 42 行采纳行的 `1011→1012` 死按钮（含门禁"已确认接取
   对话"集里的 3006/3020/3035/3092）**全部闭合**；`EVIDENCE_REQUIRED` 44 → **2**，
   `CLIENT_PAGE_UNREACHED` 1555 → **1389**，契约门 fatal 集 **44 → 2**（余 2 条为改动前既有，
   见 §5）。
3. **3 行 HOLD 采纳**（21460/29070/29071）：双侧对拍后真端侧 **0 未达 / 0 致命**（XML 侧同
   3 行 13 个未达页全被真端侧闭合）⇒ flip 四件套落地，`verify_retirement` `1341/1341/4883/6224 — OK`。
4. **链式指纹外科重冻**：42 行改值 + 3 行新增（282 → **285**），改值集合与缺口集合
   **逐元素相同**（`identical_to_gap_set=True`）——即"指纹变化恰好等于缺陷面"，无溢出。

## 2. 根因（证据链）

### 2.1 症状

探针（P0c-39 遗留形态）对 3 行做双侧审计，修复前：

| quest | XML 侧未达/致命 | 真端侧未达/致命 | 真端侧致命形状 |
|---|---|---|---|
| 21460 | 1 / 0 | 6 / **1** | `BUTTON_WITHOUT_ROUTE 1011→1012`（NPC 799258） |
| 29070 | 6 / 0 | 6 / **1** | 同轴（NPC 801220） |
| 29071 | 6 / 0 | 6 / **1** | 同轴（NPC 801221） |

### 2.2 客户端页链（强二证据）

`docs/quest/client-dialog-mapping/quest-dialog-action-details.csv`（`href` 列 = 按钮动作）：

- 21460：`select1(1011) -1012→ select1_1(1012) -1007→ ask_quest_accept(4) -1002/1003→ 1003/1004 -1008→ close`
- 29070/29071：`select1(1011) -1012→ select1_1(1012) -1013→ select1_1_1(1013) -1007→ ask_quest_accept(4) -1002/1003→ 1003/1004`

即 1011 页唯一个可见按钮的动作 id 就是 1012（`HACTION_SELECT1_1`），客户端出口登记
`quest_client_dialog_exits.tsv` 同判（3 行皆带 `SELECT1_1`，29070/29071 另带 `SELECT1_1_1`）。

### 2.3 真端 IR 缺什么

修复前真端 IR（`P0c40AcceptChainProbeTest` dump）在 acquired NPC 上已有
`unaccepted --31--> +1011`、`--1007--> +4`、`--1002--> started +1003`、`--1003--> +1004`、
`--1008-->`，**但没有** `--1012-->` / `--1013-->`；审计的按钮闭包因此断在 1011 页。

根因定位在同一文件的单步/链式分道：

- 单步路径（`buildDefinition`）有
  `if (exits.requires(questId, SELECT1_1)) transitions.addAll(acceptContinuation(...));`
- 链式路径（`buildChain`）只消费了 `exits` 的 `SELECT2_CONTINUE` 与 `SELECT5_CHECK[_SIMPLE]`，
  **漏了接取页梯这一支**。

补丁（`buildChain` 的 NPC_START 块循环内，紧随 `acceptFlowChain`）：

```java
if (exits.requires(entry.questId(), RetailClientDialogExits.SELECT1_1)) {
    blockTransitions.addAll(acceptContinuation(start.npcId(),
        exits.requires(entry.questId(), RetailClientDialogExits.SELECT1_1_1)));
}
```

### 2.4 为什么不是"登记表补 R 记录"

生成器（`build_quest_client_talk_chain_steps.py`）的 `synthesize_canonical` 按设计把接取流
交给规范块（`B NPC_START` 的 `unaccepted started|SELECT1|<accepts>` 三参数），只为
`ASK_QUEST_ACCEPT` 补一条 `started→started` 的显式路由；页梯（1011→1012→4）在单步路径已由
`acceptContinuation` 承担。链式路径照抄该口径即可，无需改表（改表反而会撞上"显式路由覆盖块
路由"的既有语义，并把 42 行以外的行拖进变更面）。

## 3. 落地物

| 类别 | 路径 | 变化 |
|---|---|---|
| 编译器 | `src/main/java/.../retail/RetailSimpleTalkDefinitionCompiler.java` | NPC_START 块路径补 `acceptContinuation`（6 行 + 5 行注释） |
| 清单×4 | `src/main/resources/.../quest_retail/retail-xml-retention.tsv`、`src/test/resources/quest/retail-xml-retention.tsv`、`target/{classes,test-classes}/...` | 3 行 `XML_RETENTION → RETAIL_TABLE`，理由 `retired-xml-in-git-history p0c40-accept-chain-decisions.tsv basis=CHAIN_ACCEPT_CONTINUATION`；四副本 md5 `ea6170e93004ae51f5dc99a53d51e1c7` |
| 清单快照 | `.agents/summary/scriptdll-quest-driver/retail-xml-retention.tsv` | 同 3 行按行手术（snapshot 落后于生产副本，单独核对） |
| 目录×2 | `src/main/resources/.../quest_definition_catalog.xml`、`target/classes/...` | 删 3 条 `<definition>`，条目 **1344 → 1341**；md5 `5248f3a6eefec84961e4813457a76a29` |
| 生产 XML×2 | `.../quest_definition/quests/{21460,29070,29071}.xml` | 删除（tracked，`git show HEAD:<path>` 可复现） |
| 链式指纹×2 | `src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv`、`target/test-classes/...` | 42 行改值 + 3 行新增，**282 → 285**；md5 `c458370f932e5c8c43e1dd2bae11693f`；尾部历史块（`80752, 1323, 2611, 3001, 3023, 21136, 24123`）逐字节保留 |
| 探针归档 | `.agents/summary/scriptdll-quest-driver/P0c40AcceptChainProbeTest.java.txt`（355 行） | 归档并从测试树删除（含 `target` 下 `.class`） |
| 机械脚本 | `.agents/summary/scriptdll-quest-driver/p0c40_flip_hold_rows.py`、`p0c40_refreeze_fingerprints.py` | dry-run/`--apply` 双档；whipsaw 守卫 + 行数/升序/尾部块三重断言 |

> 并发车道注记：`.agents/summary/scriptdll-quest-driver/p0c40_reflip_my_wave_rows.py` 使用同一 `P0c-40` 标签但属**另一条车道**（与我方 42 行缺口面无关），本片未触碰；本片产物一律以 `p0c40-*` / `p0c40_*` 中上表所列文件为准。

## 4. 证据（原始数据）

### 4.1 双侧对拍（探针，修复后）

真端侧 3 行：`{PAGE_ACTION_MATCHED=30, TERMINAL_PAGE_REACHED=6}` —— **0 未达 0 致命**；
逐行含 `1011 -1012→ 1012 -1007→ 4 -1002/1003→ 1003/1004`（29070/29071 另有 1012 -1013→ 1013）。
XML 侧（现生产视图，flip 前）：`{CLIENT_PAGE_UNREACHED=13, PAGE_ACTION_MATCHED=28, TERMINAL_PAGE_REACHED=7}`
（21460 的 2375；29070/29071 各 6 页）。flip 后生产视图同 3 行为
`{PAGE_ACTION_MATCHED=30, TERMINAL_PAGE_REACHED=6}`。

### 4.2 爆炸半径（`p0c40-accept-continuation-blast.tsv`）

| 指标 | 修复前 | 修复后 |
|---|---|---|
| `EVIDENCE_REQUIRED` | 44 | **2** |
| `CLIENT_PAGE_UNREACHED` | 1555 | **1389** |
| `PAGE_ACTION_MATCHED` | 106492 | 106742 |
| `TERMINAL_PAGE_REACHED` | 22707 | 22707 |
| 契约门 fatal 集 | 44 | **2** |
| `selectorGap(1011→1012)` | 42 | **0** |
| 链式指纹改值行 | — | 42（与缺口集合逐元素相同） |

按页的未达收敛：`4: 216→178`、`1003: 58→17`、`1004: 67→26`、`1012: 64→22`、`1013: 11→7`
（`1011` 保持 48 —— 那是"根本没有转换下发 1011 页"的另一轴，不在本片）。

### 4.3 退产恒等式

`verify_retirement.py` → `catalog=1341 directory=1341 retired=4883 sum=6224 / OK: 目录一致，无悬空生产引用`。

### 4.4 指纹重冻对拍

`-Dretail.talkChain.fingerprintOut` dump 与仓内文件 set 对拍：`changed=42`、`inserted=[21460,29070,29071]`、
`added=0/removed=0`（除 3 行新增），落地后 `dump==file`（285 行值全等）。

## 5. 门禁

| 门禁 | 结果 |
|---|---|
| `RetailSimpleTalkChainGateTest` | **2/2 绿**（重冻前先红：42 行偏离 + 3 行"RETAIL_TABLE 但不在冻结指纹里"，符合预期） |
| `QuestDialogOrderAuditTest` | **17/17 绿** |
| `QuestClientContractGateTest` | 44 → **2** fatal（余 2 条既有，见下） |
| `ReportToManyDialogRouteRegressionTest` | 仅剩既有 3 缺口（`3914 (203752,1352)`、`80310`、`80311`），本片 3 行不在其 `EXPECTED_ROUTES` 内（grep 无命中） |
| T1（净树复跑） | **63 例 4F**（`gates/T1-143553.log`，392 s）：契约门（残留 2 条既有）/ DataDriven ×2（20035 漂移 + 指纹，他车道在途）/ 采集族 155 vs 175（自 10:19 起红）。与**改动前 T1**（`gates/T1-141318.log`，63 例 **5F**）比 **净减 1 红**（链门重冻后转绿）、**零新增** |
| T2（45 id = 42 缺口 + 3 flip） | 204 例 18F/5E，**归因见 `p0c40-t2-attribution.tsv`：CHRONIC 20 / TRANSIENT 2（复跑绿）/ MINE-IMPROVED 1** |

**T2 归因方法与结论**：与改动前最后一次全量 T3（`gates/T3-130322.log`，13:03:22）**逐条对拍**失败集合，
23 条失败条目里 **21 条在 13:03 全量日志已存在**（20 条 CHRONIC：19 条逐字相同 + `QuestItemSourceContractGateTest` 仅集合序不同、同一 quest 4966 同一
actual 值；另 1 条是本片**设计内改善**的契约门 44→2）⇒ 本片**零新增失败**。两条 13:03 无记录的失败（`RetailOwnershipGateTest.loadFixtures` 6224 vs
6220、`RetailDataDrivenGateTest.retentionManifestMatchesDriverOwnership` 15673/25673/80846/80847）
**复跑即绿/消失**，且所指 id 全属 DataDriven 车道在途行，与本片无关。

**契约门残留 2 条（既有，另轴）**：

- `BUTTON_WITHOUT_ROUTE|1932|reward|203893|20002|2716|1008|reward + NPC 203893 + 20002 -> reward + page 2716`
- `BUTTON_WITHOUT_ROUTE|3092|reward|798191|20002|2716|1008|reward + NPC 798191 + 20002 -> reward + page 2716`

证明为既有的两条独立证据：① 同指纹在 Sep-26 09:26 / 10:19 / 13:48 / 13:56 的历史门禁日志中已在册；
② 探针的**修复前仿真**（post-fix 目录去掉 `unaccepted` 源 1012/1013 转换）**仍复现**这两条
（`DIAG-PRE` 段），而该仿真同时复现了 42 条缺口行 —— 即这两条与本次改动无因果。
形状：显式 `R` 记录把 `CHECK_USER_HAS_QUEST_ITEM[_SIMPLE]` 的失败页 `SELECT6(2716)` 锚在 **reward** 态，
而 `FINISH_DIALOG(1008)` 关闭路由只登记在阶段/接取态 ⇒ `reward` 态失败页的"结束对话"按钮是死的。
登记为后续轴 `REPORT_ALIAS_SELECT6_FROM_REWARD`（见 `p0c40-blocked-rows.tsv`）。

## 6. 未验证 / PENDING

- **运行时与客户端目检**（未启服）：3 行采纳后的接取对话页流（1011→1012[→1013]→4→1003/1004）、
  以及 42 行同轴死按钮的客户端行为。按约束未启动服务器。
- **1011 页未达的 48 行**（无转换下发 1011）：另一轴（`ACCEPT_PAGE_NOT_EMITTED`），本片未触碰，
  已登记进 `p0c40-blocked-rows.tsv`。

## 7. 命令索引（可复现）

```bash
# 双侧对拍 + 爆炸半径（探针，运行前需从归档恢复 .java.txt → .java）
mvn -o -B test -Dtest=P0c40AcceptChainProbeTest -DfailIfNoTests=false      # 输出：RET/XML dump + BLAST 计数
# 缺口归零核对
grep -E "BLAST selectorGap|BLAST production statuses" <maven log>
# flip（dry-run / apply）
python3 -B .agents/summary/scriptdll-quest-driver/p0c40_flip_hold_rows.py
python3 -B .agents/summary/scriptdll-quest-driver/p0c40_flip_hold_rows.py --apply
# 退产恒等式
python3 -B .agents/summary/scriptdll-quest-driver/verify_retirement.py
# 链式指纹重冻（dump → 外科改值/插入）
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest -DfailIfNoTests=false \
  -Dretail.talkChain.fingerprintOut=/tmp/fp-flipped.tsv
python3 -B .agents/summary/scriptdll-quest-driver/p0c40_refreeze_fingerprints.py /tmp/fp-flipped.tsv --apply
# 门禁
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 $(awk -F'\t' '!/^#/{print $1}' \
  .agents/summary/scriptdll-quest-driver/p0c40-accept-continuation-blast.tsv) 21460 29070 29071
```

## 8. 下一步

1. `REPORT_ALIAS_SELECT6_FROM_REWARD`（1932/3092：`reward` 态失败页缺 `FINISH_DIALOG` 关闭路由）——
   契约门归零的最后一刀；需先做该轴的爆炸半径（所有"显式 CHECK 失败页锚在 reward 态"的行）。
2. `ACCEPT_PAGE_NOT_EMITTED`（1011 页未达 48 行）逐行裁定。
3. 承接 P0c-37/38 的剩余 `ADOPTION_BLOCKED`（1324/2428、deferred 11001/21138/30055/30202/30302、
   cutscene 18806/28806）与 `24123 XML_NPC_AXIS`。
