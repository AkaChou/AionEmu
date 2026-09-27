# SimpleTalk 守卫加固片 · G-1..G-4（零形状变化）落地记录

> 车道：`quest-native-dispatch`；面：**守卫加固（S3a 之后、S3b/S3c 之前的前置片）**；角色：生产守卫加固 + 门禁同步 + 证据。
> 上游：`2026-09-27-s3-adjudication-brief.zh-CN.md` §2 第 1 片、`2026-09-27-s3-special-rows.zh-CN.md` §13（三条缺口）
> + 裁定代理补的 G-4。
> 纪律：未 commit；未新增/退役任何 TSV；未启停服务；T3 用仓库外副本、跑完即删。

## 0. 一句话结论

四条 **fail-open 守卫缺口**（退场载荷只收条件侧 / 交付中转判据写死字面 `"started"` / 缺 reward 态重开载体断言 /
退场阶段门无蕴含断言）在编译期守卫与链门各补一条对应判据；**对现存 285 行完全惰性**
（指纹对拍 changed 0 / added 0 / removed 0、编译 problems=[]），为 S3b/S3c 的 R 驱动合成铺路。

## 1. 四条缺口与实装（生产单文件 `RetailSimpleTalkDefinitionCompiler`）

| 缺口 | 症状（为什么是 fail-open） | 实装 |
|---|---|---|
| **G-1** 动作侧载荷 | `retiredChainGate` 只收 conditions 的 `HAS_ITEM`；退场记录若带 `REMOVE_ITEM` 而无条件门，扣物**静默消失**（玩家永久持有任务物品），且覆盖断言因载荷集为空而空过 | `retiredChainGate` 同轴收集 actions 的 `REMOVE_ITEM:id:count`（逐物品取最大需求）；既有覆盖断言（载荷 ⊆ 规范门，含 `itemCheck=false ⇒ 空门`）因此对动作侧同样生效——不被覆盖即拒绝编译 |
| **G-2** 交付中转判据写死源名 | 旧判据 `"started".equals(source)`：I 记录展开的 `39/20002` 腿落在 `s2` 等**中间态**时整条空过（判例 24202 的 `failure_page='CLOSE'`：门边存活、无页承载、**任务不可交付而全部门禁绿**） | 判据改为 `isPreRewardSource(...)`＝**源节点投影状态 == START**；REWARD 态同名路由是 QE-083 的**重开预览**语义，必须保留 ⇒ 不能一刀切。实测该口径下现存行风险集 **0**（129 条 `SELECT_QUEST_REWARD@reward` 全部是预览腿） |
| **G-3** reward 态重开载体 | 无守卫：规范交付把玩家送进 REWARD 后，若定义里没有 reward 态奖励窗路由，领奖后关窗即**死档**（82 行口径实测 11 行无 `NPC_COMPLETE` 块） | `canonicalDelivery` 行必须存在"源节点投影 REWARD ∧ 下发六档奖励窗之一"的转换，否则拒绝编译。实测 209 行 canonicalDelivery **全部**有预览腿（`NPC_COMPLETE` 的 `preview` 段非 `-`、块 source 恒为 `reward`）⇒ 零误伤 |
| **G-4** 退场阶段门无蕴含断言 | 退场的 `VAR_IS:var0=k`/`VAR_AT_LEAST` 若不被规范边源节点投影蕴含，门在规范形下消失（"简报前即可领奖"的 premature），而 `QuestPrematureRewardRouteAudit` **看不见**（其候选要求客户端页含该 dialogId，规范边动作是 31、在客户端页动作集 0 命中） | 条件白名单放行 `VAR_IS`/`VAR_AT_LEAST`，但必须由 `canonicalSources`（接取 `unaccepted` ∪ 报告块 source）中某节点的**投影**蕴含（同 `QuestMutationPlanner.matchesSourceNode` 口径，`VAR_AT_LEAST` 取下界）；不蕴含即拒绝编译并给出源集。判例预置：`35010/35018/35024/45011/45025` 的 `VAR_IS:var0=1 @ started`（投影 `var0=-`）在 S3c 必须走"条件通道"而非锚定 |

## 2. 零形状变化证明

| 判据 | 结果 |
|---|---|
| 指纹重冻（`-Dretail.talkChain.fingerprintOut=/tmp/gh-fp.tsv`） | 285 行 **changed 0 / added 0 / removed 0**；**problems=[]**（无一行被新守卫拒绝） |
| 前置普查（改码前，登记表侧独立复算） | G-2 精确化后风险集 **0**；G-3 缺预览载体 **0 行**；G-1 现存退场面"有扣物无条件门" **0 条**（S2 203 行 + S3a 60 行同轴复算）；G-4 现存退场记录带 `VAR_*` **0 条**（旧白名单本就拒绝） |
| 链门 | **6/6 绿**（新增 `hardenedGuardsHoldAcrossCanonicalRows`：四条性质测试侧独立重算；下限 `CANONICAL_SEGMENT_ROW_FLOOR=200`、交付面 `≥200`） |
| 家族门 | **4/4 绿** |

> 诚实边界：G-1/G-4 对现存行是**潜在**缺口（无现存命中），其"可拦性"由构造性实现 + S3b/S3c 落地时的判例（1323 的 `REMOVE_ITEM`、35010 等的阶段门）验证；收口时不得表述为"修了现存缺陷"。

## 3. 收口记录（2026-09-27）

| 判据 | 结果 | 证据 |
|---|---|---|
| 变化面 == 缺陷面 | **∅**（本片声明零形状变化；changed 0） | `/tmp/gh-fp.tsv` 对拍（一次性中间产物） |
| 链门 / 家族门 | **6/6 / 4/4 绿** | `RetailSimpleTalkChainGateTest`、`RetailSimpleTalkGateTest` |
| **T2**（链选择器 285 id，基线 = S3a 终版 `T2-164459.log`） | **ADDED 0 / REMOVED 0**（身份 51 == 51；595 测试 / 35F+16E，与基线同 +1 个新增测试方法） | `gates/T2-171838.log` |
| **T3**（全 questEngine 树，仓库外副本，基线 = S3a 终版 `T3-165559.log`） | **ADDED 0 / REMOVED 0**（身份 97 == 97；2020 测试 / 109F+23E，与基线同 +1） | `gates/T3-171907.log` |
| ↳ 口径补强（执行代理独立提取） | **完整方法级 132 == 132**（计数恰等于 `Failures+Errors` 109+23 ⇒ 提取无损） | `gates/T3-hardening-{reds,added,removed}.txt` |
| 记忆库 | 追加 **QE-089**（守卫缺口四轴）+ 路由 | `patterns/quest-engine.md`、`systemPatterns.md` |

## 5. 纪律回执

未 commit/push；未启停服务；未新增/退役任何 TSV（链登记表只读）；未创建 worktree；
T3 在仓库外全树副本跑、跑完即删；Maven 仅用于聚焦测试与 T2/T3 门禁；
tree 内与副本并行时始终只有一个树内 Maven。
