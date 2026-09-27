# Phase 4-2：SimpleHunt 门禁 + 对齐批次 + SimpleTalk/DataDriven 对账

- 日期：2026-09-22；性质：门禁与数据对齐（未提交）
- 新工具：`apply_simple_hunt_align.py`、`reconcile_talk_and_datadriven.py`
- 新门禁：`QuestSimpleHuntRetailContractTest` + `src/test/resources/quest/quest-simple-hunt-retail-contract.tsv`

## 1. 门禁（任务 1）：真端计数器合同

快照 1,366 行（每行 = 一个任务的一个击杀计数器）：

| model | 数量 | 门禁断言 |
|---|---:|---|
| `COUNTER_GRID` | **813** | 6 位字段 offset=6*(n-1)；dimension `required` == 真端 `countN`；npc-ids ⊇ 真端怪集合，且 ⊆ 真端 ∪ 客户端 CSV |
| `KILL_CHAIN` | 185 | 串行链击杀节点数 == 真端 `countN` |
| `PENDING_MISMATCH` | 34 | 已知偏差：仍须是 counter-grid 形态，被"悄悄对齐"时必须显式刷新快照 |
| `FIELD_NO_KILL_MODEL` | 191 | 有字段但无 dimension/chain：锁定该形态 |
| `NO_FIELD` / `WIDE_FIELD` | 108 | 无 6 位字段：一旦出现必须刷新快照 |
| `UNRESOLVED_NAME` | 35 | 真端怪名在本仓库 NPC 表缺失（未移植怪），只锁形态 |

`mvn -q -Dtest=QuestSimpleHuntRetailContractTest test` → **通过**。

## 2. 对齐批次（任务 2）：本次只落"安全子集"

对 942 个任务、1,404 个计数器逐一对账，真端表为权威、客户端 CSV 为佐证。

**已应用（38 个任务 / 47 个计数器）**：删除"多算"的 npc-id（真端表与客户端 CSV 都没有的怪）。
例：1179 从 6 个 id 收敛到真端唯一的 `Undead_17_An`(210343)；11293 从 11 个收敛到 4 个。

**已回滚（合并进后续批次）**：机械改写曾一次性覆盖 56 个任务 / 75 个计数器，
但生产目录编译给出两类硬失败，说明改写必须与 counter-grid 阶梯同步：

| 失败 | 原因 | 受影响 |
|---|---|---|
| `COUNTER_GRID_NODE_VALUE_OUT_OF_RANGE` | 改 `required` 后，`<counter-grid nodes="...">` 里的节点投影仍指向旧计数（如 var1=3 越界 0..2） | 80419/80446/80454 等 |
| `AMBIGUOUS_TRANSITION: KILL_NPC` | 补 id 后与同任务其它 dimension 的 npc-id 重叠，展开出优先级相同的击杀转移 | 18605/21079/28605/30220/80418/80444/80453 等 |

**遗留工作清单**：
- 34 个 `PENDING_MISMATCH` 计数器（需台阶重建/补 id，含 5 个 80 杀超 6 位字段的特殊形态）
- 32 个 `CHAIN_COUNT_MISMATCH` 计数器（串行链步数与真端 countN 不符）
- 191 个 `FIELD_NO_KILL_MODEL` + 103 个 `NO_FIELD`（需确认驱动路径）

## 3. 验证边界（Maven，已授权）

```
mvn -q -Dtest=QuestSimpleHuntRetailContractTest test                     → 1/1 通过
mvn -q -Dtest=<13 个受影响家族测试> test                                   → 74 用例全绿
PRODUCTION_COMPILE_OK=6191  PRODUCTION_COMPILE_FAILURES=0
PRODUCTION_INTERACTION_OBJECT_FAILURES=0  PRODUCTION_WHITELIST_VIOLATIONS=0
```

未做：服务端重启与实机验收；未提交任何改动。

## 4. Phase 4-2 对账（任务 3）

### 4.1 SimpleTalk（真端 3,152 行）

| 结果 | 数量 |
|---|---:|
| `OK`（接取/报告/talk NPC 都出现在本仓库定义里） | **2,006** |
| `MISSING_IN_DEFINITION` | 66 |
| `UNRESOLVED_NAME`（真端 NPC 名本仓库没有） | 151 |
| `NO_REPO_XML`（未移植任务） | 929 |

### 4.2 DataDriven（真端 2,510 行 / 3,390 个步骤）

按 `value0` 解析后与定义内引用比对：`ALL_STEPS_PRESENT` 324、`PARTIAL` 121、`NONE` 952、`NO_STEPS` 111。

**但 NONE 主要是工具口径问题**：3,390 个步骤里 value0 可解析 1,805（53%），分类型看：

| 分类 | 可解析 | 说明 |
|---|---|---|
| Talk | 691/692 | name_desc 直接命中 |
| EnterArea / EnterWorld / PVP | 207/207、66/66、383/383 | 数字或名字均可解析 |
| CollectItem | 432/541 | 部分 FOBJ 名缺失 |
| **Hunt** | **3/1368** | value0 是"逗号分隔的怪名列表"，需要按 SimpleHunt 规则拆分 |
| **ItemPlay / TalkFOBJ** | **0/70、0/33** | value0 是道具/FOBJ 名，不在 NPC 名索引里 |

→ DataDriven 的步骤级对账需要先补：怪物列表解析器、道具/FOBJ 名索引（下一步）。

## 5. 下一步建议

1. **台阶重建式修正**（34 个 PENDING + 32 个链数不符）：改 `required` 的同时重算 `counter-grid nodes` 投影，
   并在同任务内做 npc-id 重叠审计，逐族提交 + 门禁复跑。
2. **补 id 分支**：同样先做重叠审计，再落 17 个"少算"与 16 个"多算+少算"混合计数器。
3. DataDriven：补怪物列表/道具/FOBJ 索引后重跑对账，再谈驱动实现。
