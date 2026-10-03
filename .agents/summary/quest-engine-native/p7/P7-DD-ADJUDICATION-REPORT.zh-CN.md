# P7 前置裁定批：DD 行「客户端三表存在性」与 80817 真端算术（计划 §10.3-#5 闭环）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

日期：2026-10-01　分支：`quest`　基线：P5D 步 3 `729cdd906`　性质：**只读证据裁定 + 冻结门（零运行时改动）**

## 1. 本批要解决的问题

计划 §10.3-#5 的原判据是「DD-337 客户端缺失行 + 80817 的 100 杀」，处置待定（复刻 vs 显式禁用）。它是 P7（DataDriven 切换批）的硬前置：
不裁定清楚「哪些 DD 行必须实现、哪些保持不生产、80817 的计数算术按哪一套」，P7 的实现与验收数字都无处落脚。

## 2. 真端/客户端事实（四源复算，工具化）

工具：`p7/tools/dd-client-presence-probe.py`（只读；真端 `data_driven_quest.xml` / 真端 `quest.xml` / 真端 `challenge_task.xml` /
客户端 `<客户端解包根>/quest.xml`、`data_driven_quest.xml`、`challenge_task.xml` / 本仓 owner 台账）。逐行证据：`p7/dd-client-presence.tsv`（2494 行）。

| 项 | 事实 |
|---|---|
| DD 表块数 | 2526（含 XML 注释块） |
| **DD 表活行（生产装载器口径）** | **2492**（32 块被注释；其中 18 个 id 只存在于注释里，全 99xxx） |
| 客户端三表 | `quest.xml` 10035 行（sha256 前 16 `0edade9f28411d73`）/ 客户端 DD 表 2155 行（`8137f99d16403dd2`）/ `challenge_task.xml` 123 行（`321a60df313f1cb5`） |
| 分桶 | `CLIENT_PRESENT` 2155 / `CLIENT_ABSENT_LIVE` **337** / `COMMENTED_OUT` **18** |
| 337 的统一形态 | 0 例外：99xxx 段、`category_acquire_ = Talk`、真端 `quest.xml` 无元数据行、owner `ABSENT`（无 XML、不在保留清单）、接取名要塞/BG 系、`[주간]/[일일]/[파티]` 标签 |
| P7 切换集 | `CLIENT_PRESENT ∧ owner RETAIL_TABLE` = **1467 行**（100% 客户端可见），与孤行集零交集 |
| 80817 | 客户端两表 + 真端 `quest.xml` 三面都有 ⇒ 必须实现；规格 = `Talk(event_iaso)` + `Hunt(world_event_camel 100)` |

## 3. 裁定

1. **337 行 = 表内孤行，保持不生产**：客户端三表皆无 **且** 真端 `quest.xml` 无元数据 ⇒ 真端同版客户端上同样不可渲染、不可领奖。
   P7 不得把它们拉进路由/注册/派发任一集合；将来上线须另立「上线」案并补客户端正文 + 元数据，不是在切换批里顺手接线。
2. **18 行注释禁用不入装载器**：`RetailDataDrivenTable` 已与「活行」口径一致（块数 ≠ 行数），本批将其固化为门。
3. **80817 = 原样复刻真端算术（含真端自身不可完成）**：DD Hunt 只有「6 位步号（bits 0-5）+ 4×6 位组槽（bits 6-29）」一种布局，
   单组上限 63 ⇒ target=100 在第 64 杀槽回绕并污染下一组 ⇒ `(slot < target)` 永不稳定。
   **禁止**改成 10 位相机「修好」（那是家族相机 `FUN_180cb14e0` 的形，DD 不走它），**禁止**显式禁用（客户端与 `quest.xml` 都有行 ⇒ 玩家可见）。
4. **计划文档同步更正**：§2.8 / §4.1 的「80817 走 10 位相机」措辞按本裁定改写；附录 D.1-#3 现场证据与「阻塞点三」历史陈述一并更新
   （避免同一文档内自相矛盾）；顺带修复 §10.3 表内 13/14/15 三行的物理行断裂。

## 4. 交付物

| 类型 | 路径 |
|---|---|
| 复算工具 | `.agents/summary/quest-engine-native/p7/tools/dd-client-presence-probe.py` |
| 逐行证据 | `.agents/summary/quest-engine-native/p7/dd-client-presence.tsv`（2492 活行 + 2 行表头） |
| 裁定书 | `.agents/summary/quest-engine-native/p7/DD-CLIENT-PRESENCE-ADJUDICATION.zh-CN.md` |
| 冻结夹具 | `src/test/resources/quest/retail-data-driven-client-absent.tsv`（357 行 = 2 表头 + 337 + 18） |
| 冻结门 | `src/test/java/com/aionemu/gameserver/questEngine/retail/RetailDataDrivenClientPresenceGateTest.java`（4 例） |
| 计划文档 | `.agents/summary/quest-engine-native/2026-10-01-真端引擎迁移计划.zh-CN.md`（§2.8/§4.1/§10.1/§10.2/§10.3-#5/附录 D.1/附录 C 第十九版） |
| 模式卡 | `.agents/memory-bank/patterns/quest-engine.md` → QE-124（`DATA_DRIVEN_CLIENT_PRESENCE_BUCKETS`） |

## 5. 门态与回归

| 门 | 结果 |
|---|---|
| `RetailDataDrivenClientPresenceGateTest` | **4/4 绿**（逐元素冻结 337 孤行 + 18 注释行、P7 切换集 1467 与孤行集零交集、80817 规格冻结、注释行不入装载器） |
| 族门 + tablelane（显式 18 类命令） | **138/138 绿** |
| 聚焦套件 `-Dtest=*Quest*Test,*Retail*Test` | **1703 例 / 162F+142E / 108 红类**；对 P5D 步 3 基线 **ADDED 0 / REMOVED 0**，唯一逐类差异 = 本批新增门 4 例（NEW, 0F/0E） |
| 探针复算 | 与入仓 TSV 逐行一致（`dd-client-presence.tsv`、冻结夹具行集 0 差异） |

日志：`gates/2026-10-01-p7-dd-adjudication.log`、`gates/2026-10-01-p7-dd-adjudication-family-tablane.log`、
`gates/2026-10-01-focused-run-p7-dd-adjudication{,-red-classes,-delta}.tsv`。

## 6. 未做 / 边界

- **零运行时改动**：本批不动 DD 路由/注册/装载器，不抢 P7 实现批。
- `RetailDataDrivenGateTest` 里 80817 的指纹红属于**旧自造宽计数形状**（既有红），P7 落地时按真端派生形状重冻；该指纹文件与在飞切片共文件 ⇒ 本批不触碰。
- 客户端三表存在性在仓库外（`<客户端解包根>`）复算；仓内门只复算可复算的一半（真端表行、99xxx 段、`Talk`、真端 `quest.xml` 无行、owner `ABSENT`、无 XML、非退役），并在裁定书登记三表 sha256 前 16。
- 各族代表任务的真实客户端验收仍 `PENDING_CLIENT`（服务器不得启停）。

## 7. 下一步

- **P7 DataDriven 切换批**：按 `CLIENT_PRESENT` 1467 行切换（含 80817 的 6 位组槽算术复刻），同批删 DD 编译器与 zone/AI 台账读取（§10.3-#14）。
- 硬前置仍是 **QE-112 在飞切片**（`RetailSimpleHuntDefinitionCompiler`/`RetailDataDrivenDefinitionCompiler` 与 DD 指纹文件共文件）⇒ P7 验收数字待其落地后冻结。
