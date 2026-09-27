# W5-g3 · 页码类 TSV 退役第三张：`quest_client_talk_pages.tsv`（活门 → 可派生判据 + 1 行解锁）

> 车道：`quest-native-dispatch`；面：**W5 页码类 TSV 退役（g3，形状片）**。
> 上游：`GOAL.zh-CN.md` §3 W5；`2026-09-27-w5g3-w5g4-prep-and-rulings.zh-CN.md`（本次裁定取证）；
> `2026-09-27-tsv-retirement-candidates.zh-CN.md` §2.1-③。
> 裁定（用户，2026-09-27）：**A 换判据 + 退役**（否决 B 保留门 / C 删门不补判据）。
> 纪律：未 commit；未启停服务；**未新增任何 TSV**；T3 在仓库外副本、跑完即删。

## 0. 一句话结论

`quest_client_talk_pages.tsv`（1340 行）的**值已死**（`:462` 读出的两页在装配函数内零使用）、**门仍活**（`:174` 拒
6 行）。本片把活门换成**可派生的 fail-closed 判据**——无进度行必须有**客户端任务书行**
（`clientSummaryRows.rows(questId) > 0`，否则 `RETAIL_TALK_JOURNAL_MISSING`）——再整体退役表与读取者。
净效果：**25200 受理**（客户端证据齐备）+ **5 行换码仍拒**（客户端无 HTML/无任务书，fail-closed）；**零其它 IR 漂移**。

## 1. 裁定与被否方案（附副本实验证据）

**实验**（`w5g3-experiment/`，仓库外副本、双跑、跑完即删）：删门 + `:462` 换常量后，DD 漂移登记 **ADDED 0 / REMOVED 0**，
分类变化**恰 6 行**全部 `ADOPTED`（`25200 80814 80823 80824 80830 80833`）；3 行 itemplay-event 行不变（被 `:102` 更早的门拦下）。
⇒ 取证代理"大概率是拒绝码漂移"的假设被**实验否决**（是真解锁）；本片据此把"删门"改为"换判据"。

| 方案 | 内容 | 结论 |
|---|---|---|
| **A（采用）** | 门换**任务书行存在性**判据 + 退役表 | 采用：判据可派生（`quest_client_summary_rows.tsv` 为常设生产表）、fail-closed、且只留 1 行真解锁（证据齐备） |
| B 保留门只清死值 | 表留下当守卫 | 否决：守卫的"页"语义已死（规范形走全局页），留下的是**陈旧守卫** |
| C 删门不补判据 | 6 行全受理 | 否决：5 行客户端**连 HTML 与任务书都没有**，形状会静默引用不存在的任务书行（**fail-open**） |

**为什么"删门"是 fail-open（实测）**：`RetailClientSummaryRows.lastRowIndex` = `Math.max(0, rows - 1)`——缺行**静默返回 0**，
形状会下发 `var0=0` 的 REWARD 投影与修复行，而客户端没有对应任务书。5 行的客户端证据实测为零：
dump 全树 `find -iname "*q80814.html"` 等零命中、`quest_client_summary_rows.tsv` 零行。

## 2. 判据派生与非回归

| 判据 | 事实 |
|---|---|
| 证据分层（25200 vs 5 行） | 25200：HTML 存在（DD 模板，接取窗 authored 为 `ask_quest_accept`(4)）、任务书 1 行；5 行：HTML 与任务书**双缺** |
| 规范形对客户端页的实际依赖 | 走**全局页**（`HtmlPages.xml` 5904 页含 4/5/9/10/1003/1004/1008/4762/10002…）；逐任务页链不再驱动（P0-2 审计收窄同源） |
| 唯一逐任务客户端证据 | 任务书行（`lastRowIndex` → REWARD 投影 + `journalRowRepair`） |
| **非回归** | `talk_pages 登记 ∩ DD-noProgress = 181 行，缺任务书 = 0` ⇒ 新判据**不拒绝任何已受理行** |
| 新拒绝面 | 5 行（8 行 = 5 + 3 itemplay-event，后者被 `:102` 更早拦下，不受影响） |

## 3. 处置清单

| # | 动作 | 对象 |
|---|---|---|
| 1 | 门换判据 | `RetailDataDrivenDefinitionCompiler:171-177` → `clientSummaryRows.rows(id) == 0` ⇒ `RETAIL_TALK_JOURNAL_MISSING`（保留 `if/else if` 结构） |
| 2 | 死值清除 | `:462` 的 `orElseThrow` + `pages.entryPage()/completionPage()` 删除；`RetailDataDrivenTalkCompiler` 的 `entryPage/completionPage` 死参从 `build`×2 / `buildItemAcquire` / `assemble` 全部移除；类 javadoc 重写（陈旧信件描述 → 规范形+任务书证据） |
| 3 | 穿线删除 | `RetailDataDrivenDefinitionCompiler`（2 签名 + 1 转发）、`RetailQuestDriver`（常量 + 孤儿注释 1 行 + 字段 + 构造参 + 赋值 + 加载块 + 2 调用点） |
| 4 | 删读取者类 | `RetailClientTalkPages.java` |
| 5 | 删表 | `quest_client_talk_pages.tsv`（1340 行）——**快照先行**：`retired-tsv/quest_client_talk_pages.tsv.retired-20260927`（sha256 `950ed74f8983447828f0048051319aaa803416076cf66076411d32eb342ff374`，与源一致） |
| 6 | 清单三步 | 删清单行 + `EXPECTED_TSV_COUNT` **24 → 23** |
| 7 | 测试夹具 | `RetailDataDrivenGateTest` 删字段 + 加载块 + 实参 |
| 8 | drift 重冻 | `retail-data-driven-drift.tsv`：本片 **9 行**（25200→ADOPTED；5 行→新码；3 行 detail 修正）；**foreign 44 行按纪律回置**（见 §4.2） |
| 9 | 冻结指纹 | `retail-data-driven-ir-fingerprints.tsv` 增 25200（`a828acdb…` / nodes 4 / transitions 25），1217 → 1218 |
| 10 | retention | 三份副本同改 9 行：25200 → `RETAIL_TABLE/OK`（`basis=DD_TALK_JOURNAL_ROW`）；5 行 → `SEMANTIC_GAP:RETAIL_TALK_JOURNAL_MISSING`；3 行 reason 修正为 `RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED` |
| 11 | **遗留 XML 退役** | `quest_definition/quests/25200.xml`（91 行，git blob `7b1aebbe2ff1c3a6d8e5431c381dc0d8c6880682`）+ `quest_definition_catalog.xml` 条目（1265 → 1264，与 XML 文件数一致） |

## 4. 级联与两起操作事故（诚实留痕）

### 4.1 事故一：受理行必须同步退役遗留 XML（T1 首跑 1F+15E）

只改 retention 不删 XML ⇒ `verifyProductionCoverage` 报 `wrongOwner=[25200]`，级联 15 个错误（启动门/交互物门/覆盖门/系统发放门…全因生产视图不可读）。
修复：删 XML + catalog 条目（见 §3-11）。教训：**owner 翻转 = 三件套（retention + XML + catalog）同片**。

### 4.2 事故二：`target/` 孤副本（修复后 T1 仍 1F+15E）

Maven 资源拷贝**不删除**已从源码删除的文件 ⇒ `target/classes/.../quests/25200.xml`（Sep 25 的旧副本）还活着，生产视图继续按"XML 存在"判 owner。
修复：删该孤副本（等价于对该文件 `clean`）后 T1 转为**仅 1 红（foreign 在册红）**。教训：**删除 `src/main/resources` 下的文件后必须清理 target 孤副本**（写入记忆库）。

### 4.3 事故三：重跑派生生成器是破坏性的（未造成后果）

按"生成器产出则改生成器"的常规思路试跑 `build_retention_list.py` ⇒ 它**回退 472 行**（该脚本不消费其他车道更新的裁定文件，如 `wave10-chain-acquire-registry-decisions.tsv`）。
处置：**立即从备份还原三份**，改为**手工精确改本片 9 行**（锚定 + 逐行断言），三份一致性复核通过。教训：**派生生成器重跑前必须备份 + 逐行核差**。

### 4.4 drift 登记的"在册红"保护

在册 drift 表相对真实编译输出已滞后 **44 行**（并行车道的迁移成果尚未重冻，`20035` 是其代表）。本片重冻时**只写本片 9 行、foreign 行按原值回置** ⇒ 红集（方法身份）保持 `ADDED 0 / REMOVED 0`（把别人的在册红改绿 = REMOVED 1，违反本片判据）。

## 5. 门禁

| 门 | 结果 | 证据 |
|---|---|---|
| 聚焦（DD 门 / 清单门 / 黑盒契约门） | DD 门仅 foreign 在册红；清单门 **3/3**；`RetailQuestContractTest` **1/1**（25200 退役后黑盒四相位通过） | `w5g3-focus1.log` / `w5g3-recheck2.log` |
| **T1**（固定套件 **87** 测试，含 W5-g4 新增常设门；`forkCount=2`） | **ADDED 0 / REMOVED 0**；红身份集 sha256 `3b92439da8052988f0acd3a23a646786a78c3ac58e7c64a9c1dcabdd1d5cd137` 与基线**逐字节相同**（唯一红 = foreign `20035`） | `gates/T1-230021.log` + `gates/T1-w5g4-reds.txt`；早前 86 测试同结论见 `gates/T1-224331.log` |
| **T2 / T3**（合并收口扫，`forkCount=2`） | T3：**2017 测试**（2016 + W5-g4 新门 1）；红集 98 = 基线 97 + **1 条副本假红**（`QuestDialogMigrationGateTest` 依赖 `.agents/`，首跑副本漏带）⇒ 已用**含 `.agents` 的副本重跑**（`gates/T3-w5g34b.log`）；主树内该测试 4/4 绿 | `gates/T3-w5g34.log` / `T3-w5g34b.log` |

## 6. 沉淀

- 记忆库：新增 **QE-095**（活门退役 = 先派生可派生判据 + 非回归计数 + fail-open 点识别：`Math.max(0, …)` 型静默退化）。
- `ENV-004` 追加两条：**删除 `src/main/resources` 文件后清理 target 孤副本**；**派生生成器重跑前备份 + 逐行核差**。
- **真端依据**：受理面由真端表（`data_driven_quest.xml`）+ 客户端 dump（HTML/`HtmlPages.xml`/任务书）+ 全局页 doctrine 共同决定；未回填任何旧 XML 形。

## 7. 纪律回执

未 commit/push；未启停服务；**未新增任何 TSV**（本片只退役 + 重登记）；未创建 worktree；T3 在仓库外副本
（`/private/tmp/aion-t3-w5g3`）跑、跑完即 `rm -rf`；Maven 仅用于聚焦测试与 T1/T2/T3；树内同时只有一个 Maven；
兄弟车道文件只读（生成器为**只读执行**，其回退产物已还原；三份 retention 为**定向手工改**并留差核）；
AI 中间产物落 `.agents/summary/quest-native-dispatch/`。
