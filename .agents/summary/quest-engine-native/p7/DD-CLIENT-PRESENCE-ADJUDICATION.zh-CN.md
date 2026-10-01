# P7 前置裁定：DD 行「客户端三表存在性」与 80817 真端算术（计划 §10.3-#5 闭环）

> 日期：2026-10-01。分支：`quest`。性质：**只读证据裁定 + 证据面冻结门**（不改运行时，不抢 P7 实现批）。
> 复算工具：`p7/tools/dd-client-presence-probe.py`；逐行证据：`p7/dd-client-presence.tsv`（2492 活行）；
> 冻结夹具：`src/test/resources/quest/retail-data-driven-client-absent.tsv`（355 行 = 337 + 18）；
> 门：`RetailDataDrivenClientPresenceGateTest`（4/4）。

---

## 1. 真端表事实：块数 ≠ 活行数

| 口径 | 数量 | 说明 |
|---|---:|---|
| `<quest_data_driven>` 块 | 2526 | 原始文本里的块（含注释） |
| **活行（生产装载器口径）** | **2492** | 32 个块被 XML 注释掉；其中 18 个 id **只**存在于注释里 |
| id 只存在于注释 | 18 | 99142/99146/99151/99172/99173/99181/99183/99189/99242/99246…（全 99xxx） |

- 生产装载器 `RetailDataDrivenTable.load` 与 P0a 报告（2492 行）同口径：**注释行不是行**；探针此前用裸正则把注释块也算进来（2510），已修正并复算。
- 这与「本服自造宽计数」的历史结论一致：旧审计的 8 行 >63 里 7 行是刷怪/计时描述符误读、1 行（80817）真实超限，见 `p0a/semantic-matrix-dd-wide-count.md`。

## 2. 客户端三表存在性（玩家可渲染性判据）

| 客户端表 | 行数 | sha256（前 16） |
|---|---:|---|
| `<客户端解包根>/quest.xml` | 10035 | `0edade9f28411d73` |
| `<客户端解包根>/data_driven_quest.xml` | 2155 | `8137f99d16403dd2` |
| `<客户端解包根>/challenge_task.xml` | 123 | `321a60df313f1cb5` |

| 分桶 | 数量 | 处置（本批裁定） |
|---|---:|---|
| `CLIENT_PRESENT` | 2155 | 客户端可渲染 ⇒ P7 必须实现（**不得**以「客户端缺行」为由冻结） |
| `CLIENT_ABSENT_LIVE` | **337** | 真端装**载**但客户端三表皆无 ⇒ **类级冻结：不路由 / 不注册**（与 P0a 的 337 CLIENT_MISSING 数一致） |
| `COMMENTED_OUT` | 18 | 真端表注释禁用 ⇒ **装载器不得含**（`RetailDataDrivenTable` 已满足） |

**337 行的统一形态（逐行复算，0 例外）**：id ∈ 99003..99749（99xxx 段）、`category_acquire_ = Talk`、
**真端 `quest.xml` 无元数据行**（0/337）、台账 owner `ABSENT`（0 行有 XML、0 行在保留清单）、
接取名分布 = 要塞/BG 系（`WORLD_*` 114 / `DF6_*` 70 / `LDF5_*` 50 / `LF6`·`LF5`·`DF5`·`Ab1`…），
dev_name 标签 = `[주간]` 周常 39 / `[일일]` 日常 18 / `[파티]` 组队 8 / 无标签 290。

**裁定理由（真端口径）**：玩家可见面的两条硬前提是「客户端任务书/正文有行」与「真端 `quest.xml` 有元数据（名称/等级/奖励）」；
337 行两条都不满足（客户端三表皆无 **且** 真端 `quest.xml` 无行），即在真端同版客户端上同样**不可渲染、不可领奖**。
因此裁定 = **表内孤行，保持不生产**（台账 owner `ABSENT` 不变），P7 不得把它们拉进路由/注册集；
将来若要上线，必须另立「上线」案并补客户端正文 + `quest.xml` 元数据，而不是在切换批里顺手接线。

## 3. 80817 裁定：**复刻真端算术**（既不复刻 10 位相机，也不显式禁用）

| 项 | 事实 |
|---|---|
| 客户端存在性 | ✅ 客户端 `quest.xml` + 客户端 `data_driven_quest.xml` 都有 80817 |
| 真端 `quest.xml` | ✅ 有元数据行（可领奖） |
| 台账 owner | `RETAIL_TABLE`（属于 P7 切换集） |
| 真端规格 | `category_acquire_ = Talk`（`event_iaso`）、`category_progress_ = Hunt`、`value0_progress_ = world_event_camel 100` |
| 真端算术 | DD Hunt = **6 位步号（bits 0-5）+ 4×6 位组槽（bits 6-29）**，单组上限 63（`p0a/semantic-matrix-dd-wide-count.md` §2b/§2c，`FUN_180c46020` 逐指令） |
| 结论 | target=100 > 63 ⇒ 第 64 杀槽回绕并污染下一组 ⇒ `(slot < target)` 永不稳定 ⇒ **该任务在真端自身无法通过击杀完成** |

**裁定**：P7 实现必须**原样复刻**该算术（含溢出行为）——**禁止**把它「修好」成 10 位相机
（10 位 ×3 槽是家族相机 `FUN_180cb14e0` 的形，DD 不用它），**禁止**显式禁用（客户端与 `quest.xml` 都有行 ⇒ 玩家可见）。
计划 §4.1 原文「仅 80817 严格按 10 位相机处理」按本裁定**更正**（那是浅层审计的措辞，深挖算术后已被 §2b 取代）。

## 4. P7（DD 切换批）验收前提（本批产出）

1. 路由集 = `CLIENT_PRESENT ∧ owner RETAIL_TABLE`（1467 行，**100% 客户端可见**；本批门已冻结该等式的一侧）；
2. 337 行表内孤行**不得**出现在注册/路由/派发任一集合（门已冻结 owner/XML/退役三面）；
3. 18 行注释禁用不得进装载集（门已冻结 `RetailDataDrivenTable.find(...) == empty`）；
4. 80817 按 6 位组槽算术实现，其真端规格 `world_event_camel 100` 与「真端不可完成」的事实冻结在门里；
5. `RetailDataDrivenGateTest` 里 80817 的指纹红属于**旧自造宽计数形状**：P7 落地时按真端派生形状重冻
   （该指纹文件与 QE-112 在飞切片共文件 ⇒ 不在本批触碰）。

## 5. 复算

```bash
python3 .agents/summary/quest-engine-native/p7/tools/dd-client-presence-probe.py
mvn -o test -Dtest='RetailDataDrivenClientPresenceGateTest' -DfailIfNoTests=false
```
