# Phase 4-1：SimpleHunt 真端表 ↔ 本仓库定义 全量对账

- 日期：2026-09-22；性质：只读对账（未改生产源码、未提交）；工具：`reconcile_simple_hunt.py`
- 输出：`simple-hunt-reconciliation.tsv`（942 行逐任务明细）、`simple-hunt-reconciliation.txt`（汇总）、`npc_name_index.tsv`

## 1. 方法与链路

```
真端 Map/XML/Quest_SimpleHunt.xml  (count1..N / monster1..N)
        │  name_desc 索引（87,719 个名字 → npc_id，0 冲突）
        ▼
本仓库 <progress><bit-field width="6" offset="6*(N-1)"> + <counter-grid><dimension required npc-ids>
        │
        ▼
客户端 QUEST_Q*.html 的 ([%n]/count) 显示 + <step> 行数（第二判据）
```

真端表 1,863 行，其中本仓库存在对应任务 942 个；921 个任务本仓库尚未移植（`NO_REPO_XML`）。

## 2. 结果

真端表 1,863 行；本仓库存在对应任务 942 个（其余 921 个尚未移植）。

**计数器级**（942 个任务共 1,404 个计数器判定）：

| 分类 | 数量 | 含义 |
|---|---:|---|
| `MATCH` | **759** | 6 位字段 + dimension 的 required/npc-ids 与真端表逐字段一致 |
| `EQUIVALENT_KILL_CHAIN` | **183** | 用串行 `<kill-chain>` 建模，杀数 = 表 count（语义等价，IR 形状不同） |
| `MISMATCH` | **87** | dimension 与真端表不符（多算/少算/数量差） |
| `CHAIN_COUNT_MISMATCH` | **31** | 链步数 ≠ 表 count |
| `FIELD_WITHOUT_KILL_MODEL` | 191 | 有 6 位字段，但既无 dimension 也无 kill-chain |
| `NO_FIELD` | 103 | 连字段都没有 |
| `WIDE_FIELD_FOR_COUNT_GT_63` | 5 | 表 count > 63，仓库改用 7 位字段（偏离真端布局） |
| `UNRESOLVED_NAME` | 45 | 真端 spawn 名在本仓库 NPC 表中不存在（未移植怪） |

**任务级**：完全一致 `477`；计数器语义等价（kill-chain）`143`；
**真差异候选** `MISMATCH 63` 与 `CHAIN_COUNT_MISMATCH 25`（共 88 个任务，需家族级复核）。

客户端旁证：423 个任务在 `quest_summary` 里有 `([%n]/N)` 计数显示，与真端表 count 一致。

## 3. 三类必须处理的真差异候选

### 3.1 MISMATCH 87 个：计数器多算（家族级）

例 `1179`：真端 `10 x Undead_17_An`（npc 210343），仓库写成
`10 x [210343, 211099, 211135, 211153, 211960, 211961]` —— 多出来的 5 个是
`UndeadLight_36..40_An`（同族不同地图 spawn 名）。真端只按 `Undead_17_An` 计数，
仓库把同族怪一起算 → 击杀进度会偏快。这正是"同一根因、跨任务族"的典型形态。

### 3.2 CHAIN_COUNT_MISMATCH 31 个

例 `1196`：真端 `n1=14 / n2=8`，仓库链是 15 节点与 15 节点（应为 14+1 与 8+1）→ 第二条链多 6 个击杀步。

### 3.3 count > 63 的 6 个任务（1842/1843/1844/2843/2844…）

真端 6 位打包字段上限 63，但表里写了 `count1=80`（阿比斯上层 80 杀）。
仓库改用 `width="7" max="80"` —— **偏离真端布局**，需确认这些任务真端是否走 per-quest 脚本而非模板族。

## 4. 下一步

1. 把本对账固化为 **Maven 门禁**（JUnit + 同一份期望值），先锁 `MATCH`/`EQUIVALENT_KILL_CHAIN` 两类不回归。
2. 处理 §3.1/§3.2 共 **118 个真差异候选**（家族级：同族 spawn 名过度并集、链步数错位）。
3. 查清 `FIELD_WITHOUT_KILL_MODEL` 191 + `NO_FIELD` 103 的实际驱动路径（可能有旧模板/脚本驱动）。
4. `UNRESOLVED_NAME` 45 个随未移植怪一起跟进。
