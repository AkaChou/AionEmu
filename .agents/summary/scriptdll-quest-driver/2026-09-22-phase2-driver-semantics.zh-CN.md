# Phase 2：ScriptDLL64 任务驱动语义解码（真端驱动恢复第二步）

- 日期：2026-09-22；性质：只读分析（未改生产代码）；上游：Phase 1 报告 `2026-09-22-phase1-quest-registry.zh-CN.md`
- 新增脚本：`extract_helper_body.py`（从 Ghidra dump 抽函数体）、`validate_hunt_counter_model.py`、`report_datadriven_coverage.py`
- 结论文件：`hunt-counter-model-validation.txt`、`datadriven-coverage-report.txt`

## 1. 核心结论：击杀计数是「6 位打包字段」

反编译 `FUN_180cb13b0`（SimpleHunt 击杀步，覆盖 1786 个任务）：

```c
readVar(plVar1, &state, questId);              // 0xd0：读 (状态, packed 值)
if (packed < 0x40000000 && state == 3 &&
    ((packed >> (6*(param_3-1))) & 0x3F) < param_4) {   // SECTION_(param_3-1) < limit
        newVar = packed + (1 << (6*(param_3-1)));
        if (param_6 == 0 || newVar != param_5) setVar(0xf0);      // 普通推进
        else                                   setVarGoal(0x100); // 达成整条任务的完成值
        pushCounterInfo(0x118);                // 推给客户端刷新摘要
}
```

| 参数 | 语义 |
|---|---|
| `param_1` | 任务 ID |
| `param_3` | 计数器序号（1 起）→ 占 packed 字段第 `(param_3-1)*6` 位，宽 6 bit（上限 63） |
| `param_4` | 该计数器上限 → 与客户端 `SECTION_n<limit` 的 `limit` 完全一致 |
| `param_5` | 整条任务该字段的**完成值** `Σ limit_n << 6n` |
| `param_6` | 1 = 命中完成值时用「完成态」写入（0x100），0 = 一律普通写入（0xf0） |

**这是与客户端表、与本仓库定义格式三边吻合的硬证据：**

1. 客户端 `quest_monster.csv` 的 `questProgress` 列就是这些守卫：
   `1102,Progress(SECTION_0<3; SECTION_5==0)` ↔ 注册点 `FUN_180cb13b0(1102, …, 1, 3, 3, 1)`
2. 本仓库 `1102.xml` 的 `<progress>` 正是 `<bit-field name="var0" offset="0" width="6" max="63"/>`
   ——「6 位宽」不是我们自己发明的，是真端的原始布局。

### 1.1 批量校验（`validate_hunt_counter_model.py`）

对 1,650 个同时有注册点与 `quest_monster.csv` 守卫的击杀任务：

| 校验项 | 结果 |
|---|---|
| 注册点 `(param_3, param_4)` == 客户端 `(SECTION_n, limit)` | **1650 / 1650 完全一致** |
| 注册点 `param_5` == `Σ limit_n << 6n` | 1648 / 1650（99.88%） |

2 个例外：`13912` / `23912`（`param_5 = 0x1001`，含 `SECTION_2` 位，而 CSV 只写了 `SECTION_0<1`）
→ 属「完成值包含其他步骤置位」的情形，留待 Phase 3 复核，不影响主模型。

### 1.2 修正 Phase 1 的一个假设

Phase 1 假设「步骤顺序 = 调用点偏移序」——**对计数器步骤不成立**。
例：1517 的注册序是 `SECTION_1(6 只) → SECTION_0(4 只)`，而客户端 CSV 与摘要顺序是 `SECTION_0 → SECTION_1`。
即：**击杀步的语义顺序由 `param_3` 给出的字段序号决定，注册顺序只是代码组织顺序。**
（对话链 SimpleTalk 另有一套显式状态变量 0→1→2→…，见 §2.2。）

## 2. 各族 helper 语义（反编译实证）

公共结构：`param_2` = 脚本接口对象（虚表），`param_3` = 记录/上下文对象；
下表槽位为**推断**（同族多函数一致，但未做 vtable 符号级对齐）：

| 槽位 | 推断语义 |
|---|---|
| `+0xd0` / `+0xd8` | 读进度（值 / 状态） |
| `+0xf0` / `+0x100` | 写进度（普通 / 完成态） |
| `+0x118` | 推送计数进度给客户端 |
| `+0x188` | 向客户端下发「状态码」（对话/摘要页） |
| `+0x410` | 下发对话页（`param_4, param_5`） |
| `+0x5d8` | 刷新/提交任务显示 |
| `+0x1c0/0x1d0/0x1f0/0x1f8` | CombineTask 的分量/产物/检查 |

### 2.1 SimpleHunt（1865 行，注册命中 1863）

| helper | 作用 |
|---|---|
| `FUN_180cb13b0` | 击杀步：6 位计数器自增（上文模型） |
| `FUN_180caf7c0` | 收尾/报告步：按客户端动作码分派；`20000`/`0x3ea` 分支要求 packed 值已达目标，未达直接 `return` |
| `FUN_180cafa40` | 完成/奖励步：动作码 + 目标值双重校验，目标值还从全局表 `DAT_184720398+0x48` 反查真值 |

两者 switch 的动作码一致：`0x3ea 0x3eb 0x3ec 0x3ef 0x3f1 0x3f4 0x3f5 10000 0x2711 0x2712 0x2724..0x2727 0x4e21 0x4e22 0x4e24 0x4e25`。

### 2.2 SimpleTalk（3152 行，注册命中 3151）

| helper | 作用 |
|---|---|
| `FUN_180cab520` | 单步对话：状态 ∈ {0,10} 才推进；写状态码 `0x3eb/0x3ec/0x3f4/0x3f5`，再发对话页 |
| `FUN_180cabb10` | 多步对话链（2641 用它注册 4 步 + 每步 4 个描述符指针）：状态变量 `0→1→2`（0xf0 普通写）、`0x15..0x18`（0x100 + 0x1b0 完成写） |
| `FUN_180caca90` | 报告步：`0xd8` 校验后发 `0x3eb` + 对话页 |

观察到的**状态码映射**（`FUN_180cabb10`）：
`state 0,1,2 → 0x548, 0x69d, 0x7f2`（步长 `0x155=341`），
`state 0x15..0x18 → 0x948, 0x99d, 0x9f2, 0xa47`，
`state == param_4（当前注册步序号） → 0x947`。
含义（客户端摘要行/对话页 id）尚未闭环，列入 Phase 3 验证项。

### 2.3 CombineTask（574 行，注册命中 574）

`FUN_180caac10`：`0x100` 结束当前进度 → `0x5d8` 刷新 → `0x1c0` 写 8 个分量 → `0x1f8`/`0x1d0` 逐个发放产物 → `0x1f0` 收尾。
`FUN_180caaf00`：13 行，状态谓词命中后走 `0x1a8`，属分支收尾。
数据源为客户端 `combine_task.xml`（本仓库 574 个任务 **100% 都在该表**）。

### 2.4 客户端摘要契约（已闭环）：一行 = 一个 SECTION 状态

`Dialogs/QUEST_Q<id>.html` 的 `<HtmlPage name="quest_summary">` 里每个 `<step>` 的模板变量：

| 变量 | 含义 |
|---|---|
| `[%3n]` | 第 n 个 SECTION 状态的 `visible` |
| `[%3n+1]` | 该行文字颜色 |
| `[%3n+2]` | 该行计数显示（如 `([%2]/3)`、`([%5]/5)`） |

实测（`visible` 变量下标 / 3 = SECTION 序号）：

| 任务 | `<step>` 行数 | SECTION 序号 | CSV 守卫 |
|---|---:|---|---|
| 1102 | 2 | 0, 5 | `SECTION_0<3; SECTION_5==0` |
| 1517 | 2 | 0, 5 | `SECTION_0<4; SECTION_1<6; SECTION_5==0` |
| 1365 | 2 | 0, 5 | `SECTION_0<5; SECTION_1<5; SECTION_5==0` |
| 1001 | 5 | 0,1,2,3,4 | script 5 行 |
| 1002 | 10 | 0..9 | — |
| 2641 | 4 | 0,1,2,3 | SimpleTalk 链 |

据此：
- **击杀进度不是独立的一行**，而是在本行内显示 `(已杀/上限)`（1102 → `[%2]/3`）。
- **报告行固定用 SECTION_5**：`simpleQuest` 每行的 `SECTION_5==0` 守卫即"尚未报告"。
- 1001 这类 script 型任务在客户端仍是 5 行（SECTION_0..4），但服务端把 5 次击杀拆成 5 个
  script 步（`quest_script_monster.csv` 的 `Progress(1..5)`），所以服务端状态数 ≠ 客户端行数。

## 3. Phase 1.1：注册点之外的 1,684 个任务

`report_datadriven_coverage.py`（本仓库 6,224 个任务 vs 客户端表）：

| 表 | 表内任务 | ∩本仓库 |
|---|---:|---:|
| 注册表（ScriptDLL64） | 6,814 | 4,540 |
| `data_driven_quest.xml` | 2,155 | 1,508 |
| `combine_task.xml` | 574 | 574 |
| `quest_monster.csv` | 4,878 | 2,948 |
| `quest_script_monster.csv` | 430 | 230 |

本仓库**无注册点**的任务 1,684 个，其中：

- 1,508 由 `data_driven_quest.xml` 覆盖
- 863 由 `quest_monster.csv`、32 由 `quest_script_monster.csv` 覆盖
- **合计被表覆盖 1,569 → 只剩 115 个既无注册点也无表行**
  （其中 7 个客户端 `quest.xml` 已不存在；剩下 115 的 category 分布：
  QUEST 43 / EVENT 24 / NON_COUNT 20 / MISSION 14 / IMPORTANT 6 / CHALLENGE_TASK 6 / SIGNIFICANT 2）

**Phase 1 里「SEEN_MARKER 无注册点」的谜团已解决**：15551/15552/15563/16824 全部在 `data_driven_quest.xml`，
与 10501 同属 DataDriven 通用驱动。DLL 内错误字符串也证实了这条链路：

```
DataDrivenQuestLoader::LoadBasicInfo() / LoadProgressInfo() / LoadProgressInfoValues() / LoadExtraAction()
```

即真端只有两条主线：**模板族类（7 个）+ DataDriven 通用驱动**，覆盖几乎全部任务；
per-quest 脚本（9 个无模板 helper，494 个任务）才是真正的"脚本任务"。

## 4. 下一步（Phase 3 建议）

1. ~~状态码闭环~~ —— 见 §2.4：客户端摘要「一行 = 一个 SECTION 状态」已确认。
   仍待定的是 `FUN_180cabb10` 里 `0x548 + 0x155*k` 那组状态码的具体消费方（对话页 vs 摘要行）。
2. **DataDriven 驱动取参**：解 `DataDrivenQuestLoader::LoadProgressInfo*` 的加载函数，
   得到 `category_progress_`（Talk/ItemPlay/Hunt/CollectItem/EnterArea/…）→ `QuestProgressExtraInfo_*` 类的映射；
   这是 1,508 个本仓库任务的唯一驱动来源。
3. **落地顺序建议**：先 SimpleHunt（1650 个任务模型已验证）→ SimpleTalk → DataDriven，
   逐步用表驱动替换对应 XML；每族配一个「注册表/表 ↔ 现有 XML」等价门禁。
