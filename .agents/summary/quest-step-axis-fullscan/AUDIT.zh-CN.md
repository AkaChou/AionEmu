# 领奖行批次（JournalRewardRowRepairContractTest 第三批）全量收口审计报告

- 日期：2026-10-07
- 触发：用户「排查当前和真端是否还有其他的 bug」——接 itemUseArea 审计 §4 的未竟线索
  （「CONTRACTS 名单 200+ 行，未逐行核对」）
- 范围：第三批领奖行修复的在册 193 行（203 任务批次），交叉真端 `SetProgress` 常量集合 +
  legacy handler 落盘语义（源码级）+ 批次落地形态
- 状态：**AUDIT_COMPLETE / REPAIR_APPLIED_PENDING_GATE（需授权跑测试）与实机验收**
- 增补（同日）：§8 第二批（triage 全量扫描候选 11 个）与 §9 镜像对批次（MirrorPair 6 个）同根因收口；
  最终修复合计 **56 个任务**。
- 遗留问题 A-D 后续复核（2026-10-07，见 [../quest-residuals-dabc/](../quest-residuals-dabc/)）：
  **A 关闭**（表车道 5 行原生投影 = 中继数/步数 = 退役壳 XML = 批次值，无抬行偏差，不修）；
  **B 修 36512**（reward 2→1 越界修复 + 自愈边；其余 16 个在界内维持）+ 新门禁 `FactionDailyRewardRowInRangeContractTest`；
  **C 全量界内复核**（260 在册 → 200 已退役、60 有 XML 全部 = 末行索引，OUT_OF_RANGE 0，不修）；
  **D 修 10525/20525**（reward 7→6 + 自愈边反转；10101/20101/14014/14043 误报）。

## 1. 判据与方法（三源，QE-054 口径的推广）

1. **真端**（`<真端根>/server58/MainServer_ScriptDLL64/ScriptDLL64.c`）：
   `0xf0))(obj, quest, 值[, 0])`（SetProgress）与 `0x110))(obj, quest, from, to[, ...])`
   （显式轴推进）常量集合，按任务聚合。
2. **legacy handler**（迁移前 Java 实现，`git show <最后含文件提交>:<路径>`）：
   落盘语义由旧引擎源码定死（`QuestHandler.changeQuestStep` L99-111）——
   **`reward=true` 时只 setStatus(REWARD) 不写 var**（轴保持当前值），
   故 `useQuestItem/useQuestObject/checkQuestItems/defaultCloseDialog/changeQuestStep(env, from, to, true, ...)`
   一律**落盘 from**；显式 `setQuestVar(N)` 后 `setStatus(REWARD)` 落盘 N。
3. **批次落地形态**：`JournalRewardRowRepairContractTest` 的 `Contract(questId, rewardRow, staleRow)`
   记录每行改动；reward 节点现值 + 无 source enter-world 自愈边在 XML 中可直接核对。

**集合级判据**：批次值 rewardRow ∉ 真端集合 ⇒ 真端从未写过该值 ⇒ 批次值无真端支撑。

## 2. 扫描与分诊统计

| 分类 | 数量 | 处置 |
|---|---|---|
| **RISKY**（集合含 staleRow 不含 rewardRow） | 25 | 全部人工复核定案：**25 真错** |
| **NO_RETAIL 嫌疑**（staleRow≠0 或集合上限 < rewardRow） | ~22 | 复核定案：**13 真错 + 1 批次本来正确（25000）+ 1 无源存疑（24202）+ 其余留后续** |
| OK_RETAIL（集合含 rewardRow，批次有真端支撑） | 6 | 不动 |
| NO_RETAIL 其余（staleRow=0 且集合空/{0}） | ~140 | 无两源可判，留后续批次（见 §6） |
| 表车道退役（无 XML，定义由真端表合成） | 5 | 移入 RETAIL_DRIVEN：15613/25023/25606/80020/80021 |

**合计定案真错 38 个修复 + 2 个表车道存档（10530/20530 属 XML 修复、另有 5 个表车道行）→
实际 XML 修复 40 个**（RISKY 25 + NO_RETAIL 15，见 §4 清单）。

## 3. 根因

第三批的判定规则「客户端 quest_summary 末行是领奖行 ⇒ reward 投影抬到末行索引 + 补自愈边」
对**玩法步后进 REWARD** 的任务系统性不成立：真端 `0x100` 状态推进不写轴（REWARD 轴保持玩法末值），
客户端 REWARD 态按自身 `[%N]` 行门槛显示报告行——投影抬到末行索引后轴值越出行门槛集合，
任务书步骤空白（1361 实机现象同根因）；且自愈边方向反了，会把**正确的存量存档改坏**。

与已修复的 1361/1373/1345/4338/11006/24021/24052 同族——本批是同一错误模式在
第三批领奖行修复（203 任务）里的继续暴露。

## 4. 修复清单（40 个 XML；权威值 = 真端/legacy 落盘值）

| quest | 批次值→权威 | 证据摘要 |
|---|---|---|
| 1626 | 7→6 | legacy useQuestObject(6,6,true) 落 6；真端 1..6 |
| 1636 | 4→3 | legacy useQuestItem(3,3,true) 落 3；真端 1..3 |
| 2122 | 2→1 | legacy defaultCloseDialog(1,1,true) 落 1；真端 1 |
| 2208 | 2→1 | legacy 击杀后 setQuestVar(1)；真端 1 |
| 2284 | 3→2 | 真端 1|2 无 3；staleRow=2 |
| 2333 | 3→2 | legacy 护送 1→2 完成进 REWARD 轴保持 2；真端 1|2 |
| 2436 | 2→1 | legacy defaultFollowEndEvent(1,1,true) 落 1；真端 0|1 |
| 2620 | 2→1 | legacy setStatus(REWARD) 不写 var（=1）；staleRow=1 |
| 3056 | 2→1 | 真端 SetProgress=1 且 0x110(1,1)；legacy 计数递进后轴 1 |
| 1319 | 8→7 | legacy 七次递进落 7 后 REWARD；真端 1..7（型 B：旧档 0 直修） |
| 1900 | 4→3 | legacy defaultCloseDialog(3,4,true) 落 3；真端 1..3（型 B） |
| 3082 | 3→2 | legacy useQuestObject(2,2,true) 落 2（型 B 前值 0） |
| 3200 | 4→3 | 真端 3；itemUseArea 审计已证 use-item 链轴 3 |
| 3721 | 3→2 | 真端 2；镜像 4721 同 |
| 4038 | 3→2 | legacy changeQuestStep(2,3,true) 落 2；真端 2（型 B） |
| 4502 | 3→2 | legacy checkQuestItems(2,2,true) 落 2；真端 2 |
| 4721 | 3→2 | 真端 2；镜像 3721 同 |
| 4939 | 5→4 | legacy defaultCloseDialog(4,4,true) 落 4；真端 4（型 B） |
| 4943 | 4→3 | legacy defaultCloseDialog(3,3,true) 落 3；真端 2|3（型 B） |
| 11116 | 3→2 | 真端 SetProgress(11116,2) 直证；legacy var+1 与真端矛盾按真端（型 B） |
| 11076 | 4→3 | legacy defaultCloseDialog(3,4,true) 落 3；真端 1..3 |
| 14046 | 7→6 | legacy defaultCloseDialog(6,6,true) 落 6；真端 3|5 无 7 |
| 14051 | 4→3 | legacy STEP_TO_4 仅 setStatus(REWARD)（var=3）；真端 1|3 |
| 14153 | 6→5 | legacy setQuestVarById(0,5)+REWARD 落 5；真端 3 无 6 |
| 10530 | 9→8 | legacy checkQuestItems(8,9,true) 落 8；真端无 9 |
| 20530 | 9→8 | 镜像对称 + 真端 0x100 不写轴全局语义（legacy reward=false 为历史误写） |
| 21114 | 5→4 | legacy defaultOnKillEvent(216563,4,true) 落 4；真端 1|3|4 |
| 24022 | 8→7 | legacy defaultCloseDialog(7,7,true) 落 7；真端 2|3|5|6|7 |
| 24023 | 4→3 | 真端 1|2|3 无 4；staleRow=3 |
| 24024 | 5→4 | legacy defaultOnKillEvent(212861,4,true) 落 4；真端 1|3|4 |
| 24025 | 4→3 | legacy defaultCloseDialog(3,3,true) 落 3；真端 3 |
| 24030 | 9→8 | 真端 2|4|5|6|8 无 9；staleRow=8 |
| 24046 | 7→6 | legacy defaultCloseDialog(6,6,true) 落 6；真端 4|6 无 7 |
| 24051 | 6→5 | legacy changeQuestStep(5,5,true) 落 5；真端 1|3|5 |
| 30111 | 2→1 | 真端 1 无 2；staleRow=1 |
| 30227 | 3→2 | legacy 击杀 var+1→2 后 REWARD；真端 1|2 |
| 30327 | 3→2 | 镜像 30227 同 |
| 30217 | 3→2 | legacy setVar(2) 后 REWARD；staleRow=2 |
| 30317 | 3→2 | 镜像 30217 同 |
| 1921 | 4→3 | legacy defaultCloseDialog(3,3,true) 落 3；staleRow=3 |

修复模式（`apply_reward_axis_fix.py`）：

1. reward 节点 var0 回权威值；
2. 批次自愈边反转（型 A：`REWARD/权威→set 批次值` 改 `REWARD/批次值→set 权威值`；
   型 B 六例 1319/1900/4038/4939/4943/11116：原 `REWARD/0→set 批次值` 的旧档直修边目标改权威值，
   **另新增**批次坏档回滚边）；
3. 旧「QE-051 领奖行合同」注释替换为 QE-054 双语权威说明（含逐任务证据）。

配套：`JournalRewardRowRepairContractTest` 移出 45 行（40 修复 + 5 表车道移入 RETAIL_DRIVEN）；
新增参数化回归测试 `RewardRowProjectionRegressionTest`（40 行锁定：投影值 + 回滚边结构 + 旧档直修边）。

## 5. 排除与存疑

- **25000**：`setStatus(REWARD); setQuestVarById(0, var+1)` 显式写 3 = rewardRow——批次本来就对，不动。
- **24202**：无 legacy、真端无脚本常量，无两源可判——留后续（客户端行门槛解析后复核）。
- **其余 ~140 行 NO_RETAIL**（staleRow=0 且集合空）：批次前投影 0、真端无脚本——多为接取即报告的
  简单任务，现状无实机报障；留后续批次按客户端行门槛逐族复核。
- **表车道 5 行**（15613/25023/25606/80020/80021）：无 XML 结构，投影由真端表车道运行时合成；
  legacy 取证已留档（80020/80021 的 legacy `defaultCloseDialog(2,3,true)` 落 2 与批次值 3 冲突，
  但表车道现状如何投影需另查——**遗留问题 A**）。

## 6. 工具与产物（本目录）

- `audit_all_step_axis.py` / `fullscan-step-axis.tsv` / `fullscan-mismatch.tsv`——全量步号轴扫描
  （733 XML × 真端集合；`triage_mismatch.py` 分诊玩法事件越集 74 候选）
- `audit_reward_contracts.py` / `reward-contracts-crosscheck.tsv`——CONTRACTS 名单 × 真端集合交叉
- `dump_retail_evidence.py` / `risky-retail-evidence.txt`——真端推进调用上下文
- `dump_legacy_evidence.py` / `risky-legacy-evidence.txt`——legacy handler 推进/落盘调用
- `apply_reward_axis_fix.py`——批量修复（40 文件）

## 7. 待验证

1. **门禁（需用户授权，IDEA MCP 运行）**：`RewardRowProjectionRegressionTest`（56×3 用例）、
   `JournalRewardRowRepairContractTest` / `MirrorPairRewardRowContractTest`（改后名单）、
   已重锚的 `ShadowCourtRowLadderContractTest` / `ArenaPhaseRowContractTest` /
   `Batch37TalkKillReportRowLadderContractTest` / `QuestMissionRewardIndexRegressionTest` /
   `Quest14051ClientDialogAlignmentTest` / `SequentialItemCheckQuestFamilyTest` /
   `Quest21114PoisonedFungiRetailFlowTest`。
2. **实机验收**：任选受影响任务（如 1626 点完 7 盏灯、24022 链尾）确认任务书步骤不空白、
   领奖对话正常、重登后投影稳定。

## 8. 第二批：triage 全量扫描候选（非 CONTRACTS 来源）11 个

`triage_mismatch.py` 的 74 个 TRUE_CANDIDATE 中，非 CONTRACTS 来源的 ~33 个经 legacy 分诊：

- **定案真错并修复 11 个**：3711/4711（3→2，legacy kill(214823,2,true) 落 2，三值自愈边+回滚）、
  11031/11032/11033（3→2，legacy useQuestItem(2,3,true) 落 2，补回滚边）、
  18208/28208/18209/28209（2→1，legacy 击杀链后 var0=1 + setStatus(REWARD)，范围边+回滚；
  重构叙事注释「2=领奖行」按 QE-054 修正）、28602（4→3，镜像 18602，legacy kill(3,true) 落 3）。
- **误报 ~22 个**（不修）：retail 集合提取不全型（1044/1006/2008/2916/1922/14026?/24031/24043/
  10521/20521/10529/20529/15300/25300/17511/27511/24011/24014/24015/2289/2947/14022/14021）、
  状态编码型（1006/2008 的 50-54/99 节点）、坐标污染型（14054 的 retail={12675…} 为正则误抓）。
- **提取器已知局限**（记录）：`SETPROG` 四参数变体会把个别非 SetProgress 的 vtable 调用值抓进集合
  （14054 案）；大量任务真端轴值经 `uVar+1` 动态推进或其它 wrapper 写入，常量集合天然不全
  （24013 先例）——**集合级判据只用于「rewardRow ∉ 集合」方向的排除性结论**。
- **36500/46500 族 17 个**：`started` 无 var0 的表车道混合型（reward=1、retail={0}），非本模式，
  留专项（**遗留问题 B**）。
- **待细查 4 组**（triage 候选中未完成逐个取证的）：10525/20525（bad 7，retail {3,5,6}）、
  10101/20101（bad 3,4，retail {0,7,8}）、14014（bad 6，retail 1..5,7）、14043（bad 6,8，
  retail {2,4,7}）——legacy 均有 var+1/多 var 形态，倾向误报但未逐函数体定案（**遗留问题 D**）。

## 9. 镜像对批次（MirrorPairRewardRowContractTest，第二批领奖行修复 118 任务）交叉

同一判定规则的第二份批次名单（120 行）。交叉结果 RISKY 7 + 复核新增 1：

- **修复 6 个**：10110/20110（6→5，retail {2,3,5}）、14052（5→4，legacy REWARD 不写 var=4）、
  14026/24026（5→4，legacy STEP_TO_4 落 4，两任务同构）、18602（4→3，retail {1,2,3}，
  镜像 28602；**与进行中的 18602 药水传送修复同文件不同区块**）。
- **移出名单 2 个**：24052/28602（此前已修，名单残留）。
- NO_RETAIL 104 个（多为「接取即报告」简单任务，retail 无常量）留后续批次按客户端行门槛复核
  （**遗留问题 C**）。

## 10. 最终修复统计

| 批次 | 来源 | 修复数 |
|---|---|---|
| 第一批 | 第三批领奖行 CONTRACTS（RISKY 25 + NO_RETAIL 嫌疑 15） | 40 |
| 第二批 | triage 全量扫描候选 | 11 |
| 第三批 | 镜像对批次 CONTRACTS | 5（+18602 计 6） |
| **合计** | | **56** |

配套测试：`RewardRowProjectionRegressionTest`（56 行锁定）、两份批次名单更新、
7 份既有测试重锚（ShadowCourt/ArenaPhase/Batch37/MissionRewardIndex/14051 对齐/3082 族/21114）。
