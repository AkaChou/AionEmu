# W5-g2 · 页码类 TSV 退役第二张：`quest_client_entry_pages.tsv`（零效果死参数整体退场）

> 车道：`quest-native-dispatch`；面：**W5 页码类 TSV 退役（g2）**。
> 上游：`GOAL.zh-CN.md` §3 **W5** / §1 **F3**；`2026-09-27-tsv-retirement-candidates.zh-CN.md` §2.1-②、§3 第 0 层。
> 纪律：未 commit；未启停服务；**未新增任何 TSV**；T3 在仓库外副本、跑完即删。

## 0. 一句话结论

`quest_client_entry_pages.tsv`（2449 行）的三处读取**全部零效果**（DD 链编译器把 `entryPage` 接成形参后
**从不读**；DD 定义编译器读出的局部在 W5-g1 删掉 `acceptEntryPage` 参数后**已无处可传**）⇒ 与 report_pages
同型的**纯机械退役**：删读取者类 + 全部参数穿线 + 删表 + 删清单行 + `EXPECTED_TSV_COUNT` **25 → 24**；
**零 IR 变化**。

## 1. 死分支证明（逐调用点，实测）

| 事实 | 证据（file:line，实测） |
|---|---|
| 链侧"接住不读" | `RetailDataDrivenTalkCollectChainCompiler.build(...)` 的形参 `int entryPage` 在**方法体内零出现**（唯一命中即签名行）；`RetailDataDrivenTalkHuntChainCompiler.build(...)` 的 `int acceptEntryPage` 同理 |
| 定义侧读出值无处可传 | `RetailDataDrivenDefinitionCompiler:434` 的局部 `int entryPage = clientEntryPages.entryPage(...).orElse(0)` 在 W5-g1 删掉 `compileSequentialStages/compileCanonical` 的 `acceptEntryPage` 参数后**已无任何使用点**（本次删除该行） |
| 唯二值读取点 | 上述两处 `clientEntryPages.entryPage(...)`；其余全是参数穿线（driver 常量/字段/加载 + DD 两签名 + 链两签名） |
| 引用面 | 1 类 + 4 生产文件（DD 定义 / DD talk-hunt 链 / DD talk-collect 链 / driver）+ 1 测试（`RetailDataDrivenGateTest` 夹具） |

## 2. 处置清单

| # | 动作 | 对象 |
|---|---|---|
| 1 | 删参数穿线 | DD 定义编译器（2 签名 + 3 转发实参 + 1 死局部）；DD talk-hunt 链（签名 + 实参 + `build` 形参）；DD talk-collect 链（签名 + 实参 + `build` 形参） |
| 2 | 删驱动接线 | `RetailQuestDriver`：路径常量 + 字段 + 构造参数 + 赋值 + 加载块 + 2 调用点 |
| 3 | 删读取者类 | `RetailClientEntryPages.java` |
| 4 | 删表 | `quest_client_entry_pages.tsv`（2449 行） |
| 5 | 清单三步 | 删清单行 + `EXPECTED_TSV_COUNT` **25 → 24** |
| 6 | 测试夹具 | `RetailDataDrivenGateTest` 删 1 加载块 + 1 字段（含 javadoc）+ 1 实参 |

**删除前快照（纪律新增）**：因 `static_data/quest_retail/` **未纳入 git**（`git ls-files` = 0，删表不可回滚），
本片起每张退役表先在台账目录落快照：
`.agents/summary/quest-native-dispatch/retired-tsv/quest_client_entry_pages.tsv.retired-20260927`
（sha256 `e8e671a6e77cdd4c62d59823e2141b2f895f21d257f480c6076e256f02e57dff`，与源表一致）。

## 3. 门禁

| 门 | 结果 | 证据 |
|---|---|---|
| `RetailTsvManifestGateTest` | **3/3 绿**（磁盘集合 23 == 清单集合 24？—— 见注） | 聚焦跑 |
| `RetailDataDrivenGateTest`（受影响面） | **5/6 绿**；唯一红 = 在册 20035（`driftVersusShellsIsRegistered`，属并行车道） | 聚焦跑 |
| `RetailSimpleHuntEquivalenceGateTest` / SimpleTalk 链门 / 家族门 | **2/2 / 8/8 / 4/4 绿** | 聚焦跑 |
| **T1**（固定套件） | **ADDED 0 / REMOVED 0**；红集 sha256 `3b92439da8052988f0acd3a23a646786a78c3ac58e7c64a9c1dcabdd1d5cd137` 与基线逐字节相同 | `gates/T1-212654.log` + `gates/T1-w5g2-{reds,added,removed}.txt` |
| **T2**（树内 285 选择器） | **ADDED 0 / REMOVED 0**；红集 sha256 `57bb0621b2aa90f861e41358f63baa5450e73753440160ba276bb0255df721fd` 恒等 | `gates/T2-213340.log` + `gates/T2-w5g2-*.txt` |
| **T3**（仓库外全树副本，跑完即删） | **ADDED 0 / REMOVED 0**（2022 测试 / 109F+23E）；红集 sha256 `ce4673c74ed61fdcff494fedcc1b8e98c78b252d54b150a05c4bdaa937197870` 恒等 | `gates/T3-212647.log` + `gates/T3-w5g2-*.txt` |

> 计数口径：清单门比对的是"门禁目录集合（`quest_retail/*.tsv` + `quest_dialog/*.tsv`，**排除清单自身**）==
> 清单行数 == `EXPECTED_TSV_COUNT`"。本片删表后 `quest_retail` 磁盘表 23 张 + `quest_dialog` 1 张 = 24 ✅。

## 4. 沉淀

- **QE-094 复核通过**（第二张同型退役，四环节同轴、零 IR 变化、T1/T2/T3 集合恒等）——该模式已可作
  **纯机械退役**的模板：`report_pages` 与 `entry_pages` 两例均无形状裁定。
- **新增纪律**：退役前落快照（数据目录未纳入 git）；**"编译通过 + 门禁绿"不能证明删除发生**（死代码删不删都绿）
  ⇒ 删后必须复核"引用真的为零"（本片以 `grep -rn` 归零 + `test-compile` 双证）。

## 5. 纪律回执

未 commit/push；未启停服务；**未新增任何 TSV**；未创建 worktree；T3 在仓库外全树副本
（`/private/tmp/aion-t3-w5g2`）跑、跑完即 `rm -rf`；Maven 仅用于聚焦测试与 T1/T2/T3 门禁；树内与副本并行时
树内只有一个 Maven；兄弟车道文件**只读**；取证子代理**只读且不跑 Maven**（结论由主执行体复核后入台账）。
