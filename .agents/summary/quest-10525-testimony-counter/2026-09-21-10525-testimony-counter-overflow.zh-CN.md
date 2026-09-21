# 任务 10525 证言计数器越界（value out of range for progress field: var1）与同族 10529/20529 修复

- 日期：2026-09-21
- 范围：`10525`/`20525`（代理人维达的召唤 / 代理人维达的召唤·魔族）、`10529`/`20529`（破坏圣物 2）
- 状态：**实现完成、静态验证与 Maven 聚焦/生产门禁通过；客户端实机复测未做（PENDING_CLIENT）**
- 后续实机又发现 s4/s5 报告链 `QUEST_SELECT(31)`/`SELECT6(2716)` 路由缺失，证据与修复见
  [quest-10525-report-dialog31/2026-09-21-10525-s4-s5-report-dialog31.zh-CN.md](../quest-10525-report-dialog31/2026-09-21-10525-s4-s5-report-dialog31.zh-CN.md)
- 证据脚本：[audit_increment_range.py](audit_increment_range.py)、[simulate_testimony_counter.py](simulate_testimony_counter.py)、[increment-range-findings.csv](increment-range-findings.csv)

## 一、现象

玩家报障日志（2026-09-21 12:29:15，角色 `Ww`，NPC `806227` Ceber，对话 `1950` = `SELECT3_4_1`）：

```text
WARN [PacketProcessor:3] QUEST_AUDIT - 任务 owner 10525 处理事件 TALK_TO_NPC（合同 EXCLUSIVE，结果 FAILED，
路由 s2->s2，NPC 806227，对话 1950）在阶段 PLAN 失败，已提交=false，失败=java.lang.IllegalArgumentException，
根因=java.lang.IllegalArgumentException：value out of range for progress field: var1
    at com.aionemu.gameserver.questEngine.definition.ProgressLayout.pack(ProgressLayout.java:60)
    at com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner.build(QuestMutationPlanner.java:196)
```

同批服务端任务日志（QUEST-TRACE）显示玩家连续点击同一名证人的“听取证言”按钮：

```text
[S->C] SM_QUEST_ACTION 任务=10525 状态=3 步数=130   -> var0=2, var1=8
[S->C] SM_QUEST_ACTION 任务=10525 状态=3 步数=258   -> var0=2, var1=16
[S->C] SM_QUEST_ACTION 任务=10525 状态=3 步数=386   -> var0=2, var1=24
[S->C] SM_QUEST_ACTION ...（下一次点击）PLAN 阶段抛 IllegalArgumentException
```

即：第四次点击计算出的 `var1 = 24 + 8 = 32` 超出声明位段，整笔对话在 PLAN 阶段失败，客户端只收到通用窗口
（`SM_DIALOG_WINDOW questId=0 下发页=0`），玩家永久卡在 s2“听取证言”。

## 二、根因

### 1. 直接根因：四名证人的自环增量没有任何上限守卫

`10525.xml` 的 s2 有四条“听取证言”自环（NPC 806224/806225/806226/806227 → `SELECT3_1_1/3_2_1/3_3_1/3_4_1`），
旧定义是无条件累加 `+1/+2/+4/+8`，完成路线用**精确匹配** `var1 == 14/13/11/7`：

- `QuestMutationPlanner#build` 把 `IncrementVariable` 合并进解包后的存档值，再由 `ProgressLayout#pack` 做范围校验；
- `var1` 声明为 `offset=4 width=5 max=31`，`24+8=32` 越界 → `IllegalArgumentException` → PLAN 阶段整笔失败（已提交=false）；
- 即使没抛异常，`variable-is` 精确阈值也只能被单一取值命中，累加值一旦越过 14/13/11/7 就**再也回不到完成路线**。

可达性模型（`simulate_testimony_counter.py`）量化了这两种后果：

| 契约 | 从 0 出发可达状态 | 越界 | 仍能走到完成阈值 |
|---|---|---|---|
| 旧（无守卫，max 31） | 32 个（0..31） | 是（24+8=32） | 15/32（17 个状态永久死锁） |
| 新（守卫，max 15） | 15 个（0..14） | 否 | 15/15 |

### 2. 客户端契约根因：低位证言污染 SECTION_0 行索引

旧定义把 `var1` 紧凑放在 `offset=4`，`packed = var0 | (var1 << 4)`，而 Aion 5.8 客户端按固定 6-bit 位段读取
`SECTION_n = bits[6n..6n+5]`（`QuestVars` 六槽位；见 memory-bank `QE-012`）：

- `var0`（stage=2）本应独占 `SECTION_0` 作为任务说明行索引，但只要玩家听取 Este（+1）或 Ovest（+2），
  `SECTION_0` 就变成 `2 + 16 = 18` 或 `2 + 32 = 34`，行索引直接脱节；
- 旧 handler `origin/history:.../handlers/archdaeva/_10525Agent_Viola_Call.java` 用的是
  `qs.getQuestVarById(1)` / `setQuestVarById(1, …)`，即 **SECTION_1**；
- 同族的 `10527/10528/10529` 早已使用 `var1 offset="6"`，`20525` 亦为同一四位集合；
- 2026-09-17 的全库系统审计（`.agents/summary/quest-systemic-audit/`）已把 `10525/20525` 列入
  “var1 必须回到 SECTION_1”的待修清单，本次一并落地。

### 3. 同族同因：10529/20529 的 s8 boss 击杀必然失效

`10529.xml`/`20529.xml` 的 s8 沿用了旧 handler 的 `if (var1 != 0) 累加 else 推进` 分支：

- s6 的两个击杀计数（`var1` 0..7、`var2` 0..3）在 s6→s7、s7→s8 都不清零，进入 s8 时 `var1` 通常为 6 或 7；
- 于是第一次击杀把 7 写成 7（或 6→7），第二次击杀计算到 **8 > max 7** → 同样在 PLAN 阶段抛
  `value out of range for progress field`，之后每次击杀都失败，任务永久停在“消灭潜入部队军团长 (0/1)”；
- 旧 handler 之所以没暴露，是因为 `setQuestVarById` 会按 6-bit 掩码回绕（64 后才回到 0），typed 位段不会回绕。

客户端证据：`Quest_unpacked/quest_script_monster.csv:206-208` 声明 `Progress(SECTION_0==6; SECTION_1<7)`、
`Progress(SECTION_0==6; SECTION_2<3)` 与 `Progress(8)`（boss 步无 SECTION 条件）；
`Dialogs/10000_19999/quest_q10529.html` 的 step 8 文案是“消灭潜入部队军团长 (…/1)”。

## 三、修复

### 1. 10525 / 20525（`quests/10525.xml`、`quests/20525.xml`）

- `var1` 位段改为 `offset="6" width="4" min="0" max="15"`：`SECTION_0` 只保留 `var0` 行索引，
  四位证言集合落在 `SECTION_1`；文件内新增中英双语注释说明该布局。
- 四条证言自环补上限守卫 `variable-below var1 14/13/11/7`（与镜像任务 `20525` 完全一致）：

  | NPC（天/魔） | 动作 | 守卫 | 增量 | 完成阈值 |
  |---|---|---|---|---|
  | 806224 / 806228 | `SELECT3_1_1` | `var1 < 14` | `+1` | `var1 == 14` |
  | 806225 / 806229 | `SELECT3_2_1` | `var1 < 13` | `+2` | `var1 == 13` |
  | 806226 / 806230 | `SELECT3_3_1` | `var1 < 11` | `+4` | `var1 == 11` |
  | 806227 / 806231 | `SELECT3_4_1` | `var1 < 7` | `+8` | `var1 == 7` |

- 新增无 source 的进入世界自愈边（`status=START`、`var0=2`、`var1>=15` → `set-variable var1 0` +
  `PACKET_ONLY`），把旧版无限累加留下的饱和残值归一化到可继续收集的状态（沿用 10522/15545 的自愈模式）。

### 2. 10529 / 20529（`quests/10529.xml`、`quests/20529.xml`）

- s7→s8（`703317`/`703325` `USE_OBJECT`）补 `set-variable var1 0`：对齐 memory-bank `QE-044`
  “进入新步骤必须显式清零局部计数”的约定，新流程一次击杀即可推进（客户端 step 8 本就只显示 /1）。
- s8 击杀路线不再沿用“`var1 != 0` 只累加”的旧 handler 分支：客户端 step 8 只显示 `/1`，因此击杀 boss 一次
  即 `set-variable var0 9`，并在同一 mutation 中 `set-variable var1 0`；无论旧存档残留 `var1` 为 0..15（新布局
  可读到的 4-bit 残值）都能一次击杀推进且不再触发 `ProgressLayout.pack` 越界。

### 3. 全服门禁（新增 `QuestIncrementRangeContractTest`）

对生产目录 6222 个定义做静态不变量检查：**凡是被 `variable-is` 精确匹配消费的字段，其
`increment-variable` 必须带有上界（`variable-below`/`variable-is` 或 source 节点投影），且
`bound + delta <= max`**。当前覆盖 124 条精确匹配增量，违规 0。专项回归：

- `Quest10525TestimonyCounterContractTest`：双阵营布局/路由/after-commit 断言；可达性 BFS 断言可达集恰为
  0..14、每个状态都能回到完成路线；报障存档 `packed=386` 在新布局下解包为 `var1=6` 且仍可完成；
  `var1=15` 由自愈边归零。
- `Quest10529BossKillCounterContractTest`：s7→s8 清零；s8 只保留一条 boss 击杀推进路线并同事务清零 `var1`；
  旧残值 0..15 全部一次击杀推进且不越界。

### 4. 同族审计（`audit_increment_range.py`，全库口径）

| 口径 | 修复前 | 修复后 |
|---|---|---|
| `OUT_OF_RANGE`（可静态证明越界） | 0 | 0 |
| `EXACT_COUNTER_UNBOUNDED`（精确匹配字段无上界守卫） | 6（10525 ×4、10529、20529） | 0 |
| `UNBOUNDED`（无上界但非精确匹配，可见性记录） | 77 | 71 |

`UNBOUNDED` 的 71 条经抽样核对为 `variable-at-least` 溢出臂、跨步骤（target≠source）写入或 source 投影
已钉死的计数，属既有设计；若后续要收紧，需要逐任务客户端证据，本轮不改。

## 四、验证

已完成（本轮实际执行）：

- XML：4 个改动文件 `ElementTree` 解析 + `quest_definition.xsd` 校验全部通过（4/4 xsd-valid）；
- `git diff --check` 通过（无空白错误）；
- IDE 检查：3 个新增测试文件与 4 个改动 XML 无 error / warning；
- 全库静态审计：`EXACT_COUNTER_UNBOUNDED = 0`、`OUT_OF_RANGE = 0`（6222 定义 / 1012 条增量）；
- 可达性模型：新契约可达集 0..14、越界 0、死锁 0。

已执行（用户授权，2026-09-21）：

```bash
mvn -q -Dtest=Quest10525TestimonyCounterContractTest,Quest10529BossKillCounterContractTest,QuestIncrementRangeContractTest test
mvn -q -Dtest=QuestDefinitionCatalogManifestTest,QuestCollectProgressAlignmentGateTest,ClientQuestSectionAlignmentTest,QuestSection0ReportRowContractTest,QuestMonsterProgressContractAuditTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionDirectoryLoaderTest test
```

- 聚焦测试：5/5 全绿（10525/20525 契约、10529/20529 契约、全服增量范围门禁）；
- 生产目录/客户端契约门禁：命令退出码 0，`PRODUCTION_COMPILE_OK=6189`、`PRODUCTION_COMPILE_FAILURES=0`、
  `PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。

配套未执行：服务端重启验证、Aion 5.8 客户端实机复测。

## 五、边界与风险

1. **证言语义沿用“累加 + 精确阈值”模型**（旧 handler 与镜像任务 `20525` 的口径）。客户端四位标记若按
   位掩码渲染，“每名证人各一次”的正常流程下 `sum` 与掩码等价；重复点击同一证人仍会产生非期望的位组合
   （例如 Este 两次得到 `var1=2`）。要做到严格的 `|=` 语义需要引擎新增 set-bit/or 动作，本轮未引入。
2. **旧存档解读变化**：`var1` 从 `offset=4` 移到 `offset=6` 后，历史 packed 值按新布局解读（报障存档
   `386` → `var0=2, var1=6`），仍可完成；饱和残值（`var1>=15`）需要一次重新进入世界触发自愈边。
3. **10529/20529 的 s7→s8 清零与 s8 一次击杀推进是行为变化**：依据 `quest_script_monster.csv` 的
   `Progress(8)` 与客户端 step 8 的 `/1` 文案（该步无 SECTION 计数器），新流程一次击杀推进；旧版
   “`var1 != 0` 时多次击杀等待 6-bit 回绕”的行为已被替换为显式清零 + 推进。
4. 未做客户端实机验证：任务说明行索引是否稳定等于 `var0=2`、四名证人逐一对话后是否进入 s3、重复点击是否
   不再报错，需用户复测确认。

## 六、后续

1. 客户端实机复测：任务说明行索引稳定等于 `var0=2`、四名证人逐一对话后进入 s3、重复点击不再报错，以及
   10529/20529 进入 boss 步后一次击杀推进并正常领奖；
2. 客户端验收通过后再按 Playbook 规则评估是否新增代表案例；
3. memory-bank 已按自动沉淀协议升级 `QE-047` 为 `CONFIRMED`，并记录本轮 Maven 与生产门禁结果。
