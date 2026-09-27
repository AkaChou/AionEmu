# SimpleTalk S3c-D · D 类交付段收口（R-REP 报告页退场 39 行）与「就地加窗」否决记录

> 车道：`quest-native-dispatch`；面：**SimpleTalk γ 面第四片（S3c-D）**。
> 上游：`2026-09-27-s3-adjudication-brief.zh-CN.md` §1-①（裁定 ①(iii)）与 §3 风险清单（R4/R5/R11/R-REP）；
> 独立事实工作流 `wf_6248f42d-91c`（50 行逐行取证 + 完整性批判）。
> 纪律：未 commit；未新增/退役任何 TSV；未启停服务；T3 在仓库外副本、跑完即删。

## 0. 一句话结论

D 类 50 行里**可证安全的部分已收口**：交付 NPC 侧下发报告页（`SELECT5`/`SELECT6`）的记录按 **R-REP
页下发判据**退场（实测 **39 条 / 39 行**、载荷全空），**指纹变化面逐行 = 39 行**；
裁定 ①(iii) 的另一半「**中间人翻面就地加窗**」**被否决**——e2e 契约门实测中间人 owner **没有领奖选择腿**，
加窗会造**可见死按钮**（致命类 `BUTTON_WITHOUT_ROUTE`）⇒ 中间人翻面**逐字保留**，加窗退回独立裁定。

## 1. 交付内容（R-REP 报告页退场）

```
reportPageSide = dClassReportRetire ∧ 交付 NPC 作用域 ∧ （after 下发 SELECT5/SELECT6）
dClassReportRetire = ∃ reward 入边翻面记录(源投影 START、动作族 A_SHAPED) ∧ 无 NPC_REPORT 块
```

| 项 | 说明 |
|---|---|
| 判据 | **只认页下发**（`R_REP_REPORT_PAGES = {SELECT5, SELECT6}`）；**禁用动作词表**——`SELECT_QUEST_REWARD ∈ CHAIN_DELIVERY_RETIRED` 会打掉 11 行的 reward 态重开载体（判例 4970/35025，R4） |
| 作用域 | 交付 NPC = 真端 `reward_npc_name` 解析集 ∪ 客户端回落（与 W1 同源），**不叠加块 NPC**（避免波及其它片） |
| 载荷 | 退场对象实测**全空**（39/39）；新增 fail-closed 守卫 `assertReportPageRetirementQuiet`：**无规范承接边**的行（D 类）若退场对象带物品/阶段令牌即**拒绝编译**（canonical 行仍由 QE-089 四轴守卫兜底） |
| 载体 | 新增测试侧 **G-3 对偶断言**：D 类行必须保留 reward 态奖励窗重开载体（否则 R-REP 把载体一起打掉） |

## 2. 「就地加窗」否决记录（**证据优先**，不是实现取舍）

| 判据 | 事实 |
|---|---|
| 契约门实测 | `Quest3100ClientDialogAlignmentTest.completesThroughTheProductionHeadlessJourney`：`Failure[questId=3100, node=reward, page=5, reason=native reward window has no completion route]`（e2e 行走器按真实协议原语驱动，锁四相位生命周期合同） |
| 机理 | 领奖选择动作（`SELECTED_QUEST_REWARD1..6`）按**当前对话 owner** 路由；D 类的完成腿注册在**交付 NPC**，而中间人翻面的 owner 是**中间 NPC** ⇒ 中间人处下发的奖励窗按钮**无路由**（致命类，与 QE-085/086 的口径一致） |
| 结论 | (iii) 字面形（只改翻面 `after`）在本族**不成立**；可行形只有三种，均**越出本片**：①把领奖腿镜像到翻面 owner；②把窗落到交付 NPC（=W1 的 A 形，但 D 类翻面本就在中间人）；③维持现状（翻面 `CLOSE`、领奖在交付 NPC 完成）。**退回独立裁定**，本片只做 R-REP。 |
| 已回滚 | `withDeliveryWindow`、D 类工作物品门（`dClassWorkItem`）与其回放分支已移除；中间人翻面 after **逐字保留**（测试侧逐行断言 `expectedRegistryAfter(...)` 相等） |

> 说明：这一条正是契约门（QE-082：只锁生命周期四相位合同、不锁页链形状）**作为安全网**的价值体现——
> 若只有指纹/形状不变量，加窗会在"形状自洽"下静默带来死按钮。

## 3. 变化面 == 缺陷面

| 判据 | 结果 |
|---|---|
| 指纹重冻（vs W1 终版） | **39 / 0 / 0**（ADDED 0 / REMOVED 0），逐行 == R-REP 退场对象行集（39 条/39 行） |
| W1 的 31 行 / S2 203 / S3a 60 / S3b 7 | **零漂移** |
| 累计漂移（vs S3c 前基线） | 31（W1）+ 39（W2）= **70 行**，与两片行集逐行一致 |

**登记（谓词内但零变化，不得读成漏改）**
- `21455`：无报告页可退场（简报"无 `SELECT5` 页下发"11 行之一），其 mixed 中间人翻面按裁定逐字保留 ⇒ 零变化。
- `39003/49003`：有 `NPC_REPORT` 块 ⇒ 交付段由块路径接管，中间人翻面是"已接受的 premature 边" ⇒ 不在面内。
- 其余 8 行（`2266 2271 2663 2914 2954 3037 3041 3087 11460 19004` 中无报告页者）：同 21455 口径。

## 4. 门禁

| 门 | 结果 |
|---|---|
| `RetailSimpleTalkChainGateTest`（链门，S3c-D 臂） | **7/7 绿** |
| `RetailSimpleTalkGateTest`（家族门） | **4/4 绿** |
| **T2**（树内，285 选择器） | 见 §5 |
| **T3**（仓库外全树副本，跑完即删） | 见 §5 |
| 分拣波（W2 首轮 5 条新增红，两条门同身份） | `Quest1163ClientDialogAlignmentTest.followsTheRetailPotionHandoffAndRewardOwner`、`Quest3100…followsTheRetailWorkItemHandoffAndSoleRewardOwner`、`Quest3100…completesThroughTheProductionHeadlessJourney`、`Batch31GelkmarosRowLadderContractTest.rewardWindowButtonsKeepTheirClientRoutes`、`ReportToManySetSucceedAlignmentTest.completedReportToManyStepsOpenTheRewardSelectionDirectly`——**前 4 条按回滚后的真端形复绿**（其中 3100 的 headless journey 是加窗否决的直接证据），`Batch31`/`1163`/`3100` 的"奖励页路由"断言按 R-REP 重锚（页退场 + 载体承接）|

## 5. 门禁证据

| 门 | 结果 | 证据 |
|---|---|---|
| **T2**（树内，285 选择器） | **ADDED 0 / REMOVED 0**（595 测试 / 35F+16E 与基线逐项同） | `gates/T2-191349.log` + `gates/T2-s3cd-{reds,added,removed}.txt` |
| **T3**（仓库外全树副本，跑完即删） | **ADDED 0 / REMOVED 0**（2021 测试 / 109F+23E 与基线逐项同） | `gates/T3-191407.log` + `gates/T3-s3cd-{reds,added,removed}.txt` |
| ↳ 哈希级加强证据 | T2 红身份集 sha256 `57bb0621b2aa90f861e41358f63baa5450e73753440160ba276bb0255df721fd`、T3 `ce4673c74ed61fdcff494fedcc1b8e98c78b252d54b150a05c4bdaa937197870`——与 W1 终版**逐字节相同**（集合恒等，强于差集为空）；红集中无本片行 | `T*-s3cd-reds.txt` |

> 注：W2 首轮曾出现 5 条新增红（见 §4 分拣波），经**回滚加窗 + 断言重锚**后归零；上表为终版形。

## 6. 独立事实工作流的危险面清单（对账）

| 危险 | 状态 |
|---|---|
| R-REP 退场对象带载荷 ⇒ 无承接边静默丢物 | **已闭环**：实测 39/39 空 + 新增 `assertReportPageRetirementQuiet`（D 类 fail-closed） |
| D 类无 `canonicalDelivery` ⇒ G-2/G-3 自动守卫不运行 | **已补**：测试侧 G-3 对偶断言（重开载体）+ 无报告页断言（G-2 对偶） |
| 动作词表会打掉重开载体（R4） | **已按 R-REP 规避**（词表仅用于 W1 的 canonical 面） |
| 词表对 R-REP 对象 0 命中 ⇒ 页判据是唯一命中通道 | **已核**（39 条对象动作 = `QUEST_SELECT`×20 + `USE_OBJECT`×19，词表 0 命中） |
| 多变体/同体行（1851 接取 NPC = 交付 NPC）作用域误伤 | **已核**：作用域只按交付 NPC 集合判，不叠加块/接取 NPC |
| 阶段门 `VAR_IS` 随翻面保留 | **守恒**（本片不动翻面 conditions） |

## 7. 纪律回执

未 commit/push；未启停服务；未新增/退役任何 TSV（链登记表只读）；未创建 worktree；T3 在仓库外全树副本跑、
跑完即删；Maven 仅用于聚焦测试与 T2/T3 门禁；树内与副本并行时树内只有一个 Maven。
