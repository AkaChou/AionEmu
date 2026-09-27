# W5-g3 / W5-g4 取证与裁定请求（talk_pages 与 briefing_chains）

> 车道：`quest-native-dispatch`；面：**W5 页码类 TSV 退役 · 第三/第四张**。
> 上游：`GOAL.zh-CN.md` §3 W5；`2026-09-27-tsv-retirement-candidates.zh-CN.md` §2.1-③/④、§3 第 2 层；
> `2026-09-27-w5c-w6-recon-pack.zh-CN.md` §1-2；四路只读取证代理报告。
> 纪律：本文**只记录取证与选项**，未改主树任何生产文件；实验在仓库外副本完成、跑完即删（副本 `/private/tmp/aion-w5g3-exp` 已删）。

## 0. 一句话结论

- **W5-g3（`talk_pages`）**：删门**真解锁 6 行**（副本双跑实测，非拒绝码漂移）——其中 **5 行客户端连 HTML 与任务书都没有**（放开会 fail-open），**1 行（25200）证据齐备**（HTML = DD 模板 + `ask_quest_accept`、任务书 1 行）。
  可派生且 **fail-closed** 的替代判据 = **任务书行存在性**（`clientSummaryRows.rows(questId) > 0`）：非回归实测 **181/181 不误伤**，变化面 = 1 行受理 + 5 行换码。
- **W5-g4（`briefing_chains`）**：该门**今日零拒绝**（retention 全表零 `RETAIL_BRIEFING_*`；47 行内宇宙 23 行全部有 SETPRO 终点链；六族 389/389 交叉覆盖）⇒ 换门/退役**不产生任何受理变化**；原候选文档"唯一会解锁新受理行"的判断**应归 talk_pages**。选项集中在"运行期证据守卫是否迁到构建期"。

## 1. W5-g3 实验（副本双跑，实测）

**方法**：`rsync` 副本（排除 `.git/target/.agents`）；副本内两处最小改动 = ①删 `:174-177` 门（保留 `if/else if` 结构，仅清空 noProgress 分支）②`:462` 的 `pages.*` 换常量 `0,0`（值已证死）并去掉 `orElseThrow`。**双跑**：改动版 dump + 还原原文件后的基线 dump（同命令），逐行对比。

命令（留痕）：

```
mvn -o -B test -Dtest=RetailDataDrivenGateTest -DforkCount=1 \
    -Dretail.dataDriven.equivOut=<dump>  -Dmaven.test.failure.ignore=true
```

| 判据 | 结果 | 证据 |
|---|---|---|
| dump 行数 | before 1508 / after 1508 | `w5g3-experiment/drift-{before,after}.tsv` |
| quest 集合差 | **ADDED 0 / REMOVED 0** | 同上 |
| 分类变化 | **恰 6 行**：`25200 80814 80823 80824 80830 80833` → `ADOPTED` | 同上 |
| 3 行 itemplay 行 | 不变（`RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED`），且 detail 由陈旧的"client pages outside…"变为准确的 `ItemPlay` | 同上 |
| ADOPTED 总数 | 1217 → 1223（+6）；noProgress∩ADOPTED 98 → 104 | 计数实测 |

**推断修正**：取证代理的"9 行放开后**大概率是拒绝码漂移**"被实验**否决**——它们是**真解锁**（全部 ADOPTED），只是其中 5 行**不该被解锁**（见 §2）。

## 2. W5-g3 证据分层（为什么不能只按"删门"处理）

| 证据层 | 已登记行 1872（对照） | 25200 | 5 行（80814/80823/80824/80830/80833） |
|---|---|---|---|
| 客户端 HTML | 有（`dialog_unpacked/quest_q1872.html`） | 有（`data_unpacked/Dialogs/20000_29999/quest_q25200.html`） | **无**（dump 全树 find 零命中） |
| 页集（逐任务） | 7 页：`select_none(4762)` + select_success(10002) + 5/9/1008/11/12 | 7 页：**`ask_quest_accept(4)`** + select_success(10002) + 5/9/1008/11/12 | 空 |
| 任务书行（`quest_client_summary_rows.tsv`，由 HTML `quest_summary` 生成） | 2 | 1 | **无** |
| 全局页表（`HtmlPages.xml`，5904 页） | 4/5/9/10/1003/1004/1008/4762/10002… **全部在** | 同左 | 同左 |

判据（生成器 `build_quest_client_talk_pages.py` 原文）：登记 = 客户端页集**恰为** `select_none + select_success + select_acqusitive_quest_desc` 且**不含** `select1/2/3`、`ask_quest_accept`、`check_user_item_*`。
⇒ 25200 因**含 `ask_quest_accept`** 被拒；5 行因**页集为空**被拒。二者失败原因**不同质**。

**规范形对客户端页的实际依赖**（P0-2 教义，经 P0c-55/13952 与审计收窄确认）：已退役行走**全局页**（4/10/1003/1004/1008/5/9…），不再驱动逐任务信件页 ⇒ 登记表的"页"语义**已死**；仍在使用的客户端证据是 **任务书行**（`lastRowIndex` → REWARD 投影 + `journalRowRepair`）。

**关键实现细节（fail-open 点）**：`RetailClientSummaryRows.lastRowIndex` = `Math.max(0, rows - 1)` —— **缺行静默返回 0**。5 行任务书缺失时，形状会静默下发 `var0=0` 的 REWARD 投影与修复行（客户端无对应任务书）⇒ 必须由门拦住。

**替代判据（可派生、fail-closed）**：`entry.noProgress() && clientSummaryRows.rows(questId) == 0` → 拒绝（新稳定码，建议 `RETAIL_TALK_NO_JOURNAL_ROW`，reason 文案指向"客户端任务书缺失/无法表达"）。

**非回归实测**：`talk_pages 登记 ∩ DD-noProgress = 181 行，缺任务书 = 0` ⇒ 新判据**不拒绝任何已受理行**；`DD-noProgress ∩ 宇宙 ∩ 缺任务书 = 8`（5 行 + 3 行 itemplay-event，后者被 `:102` 更早的门拦下，不受影响）。

## 3. W5-g3 裁定选项

| 选项 | 内容 | 变化面 | 风险 |
|---|---|---|---|
| **A（建议）** | 门换判据（任务书行）+ `:462` 换常量 + 删表/清单/计数（24→23）+ 删读取类与穿线；5 行 retention/drift 换码重登记 | **1 行受理（25200）+ 5 行换码** | 低：25200 证据齐备（HTML=DD 模板、页 4 由客户端 authored、任务书 1 行、奖励窗页 5 在册）；5 行仍拒绝（fail-closed） |
| B | 保持表与门；本片只做"死值清理"（`:462` 换常量 + 死穿线移除） | 0 受理变化 | 0，但表保留（其"页"语义已死，属陈旧守卫） |
| C | 删门、不补判据（实验版原样） | 6 行受理 | **fail-open**：5 行无 HTML/任务书，形状会静默引用不存在的任务书行——不建议 |

> 25200 备注：客户端页 4 authored 的按钮只有 `FINISH_DIALOG(1008)`，而物品规范流路由 `1002/1003/1008`；`1002/1003` 对该行为 inert 边。此与既有 P0c-55 物品信件判例（13952）同型（该判例亦按服务端规范按钮集路由），不新增判据。

## 4. W5-g4 证据（briefing_chains：活门但零拒绝）

| 项 | 实测 |
|---|---|
| 真端 `talk_npc1` 非空 | `Quest_SimpleHunt.xml` **47** 行；∩本服宇宙 = **23**；差集 **24 行在 `quest_definition` 零命中 ⇒ 不在宇宙、永不编译** |
| 23 在册行的链终点 | **21×`10000:0` + 2×`10001:0`**（= SETPRO1/SETPRO2 家族、pageId=0），与 `:219` 注释"45+2"口径一致；**全部有登记** |
| 六族交叉覆盖 | Hunt 23/23、Collect 2/2、Talk 324/324、UseItem 33/33、ItemPlay 5/5、SerialHunt 2/2 —— **389/389 全登记，0 漏** |
| 门的拒绝面 | retention 全表**零** `RETAIL_BRIEFING_*` / `RETAIL_TALK_NPC_*`（hunt 的 4 码 + collect 的 `RETAIL_BRIEFING_CHAIN_UNREGISTERED`/`WITHOUT_COLLECT_STEP` 均零命中；仅 2 行 SimpleTalk 族的 `RETAIL_TALK_NPC_UNRESOLVED` 属 talk 编译器自己的码） |
| 表载荷 | hunt `build` 的 `briefingChains` 形参**零读**（死穿线）；collect 仅 `chain()==null` 布尔；`entry_page` 列全仓零读 |
| 23 在册行 retention | 21×`RETAIL_TABLE/OK` + 2×`XML_RETENTION/QUEST_SPAWN_UNEXPRESSED`（14112/14123，与简报门无关） |

⇒ 换门/退役**零受理变化**；原候选文档"唯一会解锁新受理行"应改挂 W5-g3。

## 5. W5-g4 裁定选项

| 选项 | 内容 | 风险 |
|---|---|---|
| **a（建议）** | 退役生产表（删表/清单/计数 24→23、删两处门子句与死穿线）；**把交叉校验迁到构建期**：登记表作为冻结夹具进 `src/test/resources/quest/`，新增常设门禁断言"宇宙内每个非空 `talk_npc1` 行必须有 SETPRO 终点的链登记" | 低：fail-closed 保留（数据修订若新增简报行却无链，CI 立刻红） |
| b | 退役 + 生产侧直接删两处门（不留构建期校验） | 今日零变化；失掉证据守卫，未来数据修订无网 |
| c | 保持表与门 | 0 风险，但保留一张载荷已死的 3225 行表 |

> 注：hunt 的 `RETAIL_TALK_NPC_UNRESOLVED`/`AMBIGUOUS`/`SLOT_CONFLICT` 三码**保留**（与表无关、须继续守）；collect 的 `:172` 采集步检查保留。

## 6. 附：陈旧登记更正（本次实测）

| 位置 | 陈旧内容 | 实测 |
|---|---|---|
| `retail-xml-retention.tsv` 的 80827/80828/80832 | `SEMANTIC_GAP:RETAIL_TALK_VOCABULARY_UNSUPPORTED` | 实际由 `:102` ITEMPLAY+EVENT 门拒绝（`RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED`）⇒ W5-g3 重登记时一并修 |
| `src/test/resources/quest/retail-data-driven-drift.tsv` 同 3 行 | detail = "client pages outside the minimal talk letter" | 实际 detail = `ItemPlay`（本实验 after dump 为准） |
| `retail-data-driven-drift.tsv:13` 注释 | 计数 6 | **正确**（6 数据行 + 1 注释行自匹配；"9"是 retention 口径）——延续 recon pack §5 的复核结论 |

## 7. 纪律回执

未 commit/push；未启停服务；**未新增页码类 TSV**；主树生产文件**一字节未改**（实验全部在仓库外副本，副本已 `rm -rf` 并确认删除）；实验 Maven 跑在副本内、主树同时无 Maven；证据（两份 dump + 两份日志）固化于 `w5g3-experiment/`；兄弟车道文件只读。
