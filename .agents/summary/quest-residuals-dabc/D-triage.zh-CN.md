# 遗留问题 D 组逐任务取证（10525/20525·10101/20101·14014·14043）

- 日期：2026-10-07
- 触发：QE-054 全量收口 AUDIT §8 遗留问题 D（triage 候选中未逐函数体定案的 4 组 9 个任务）。
- 判据：三源（真端 ScriptDLL64.c 集合/槽位注册 × legacy handler 落盘语义 × XML 投影），同 QE-054。
- 结论：**2 个真错已修（10525/20525），4 组 7 个任务确认误报**。

## 1. 真错修复：10525 / 20525（镜像对）

| 项 | 值 | 证据 |
|---|---|---|
| 批次误抬 | reward 投影 7（末行索引） | 原 XML 注释「第 7 行才是领奖行」 |
| 真端权威 | 6 | `SetProgress(10525)` 常量集合 = {3,5,6}（含 `0xf0(0x291d,6)`），无 7；且 `0x100(param_1,0x291d,6)` 状态推进携带 6 |
| legacy 落盘 | 6 | `_10525Agent_Viola_Call#onItemUseEvent`：`qs.setStatus(QuestStatus.REWARD)` 不写 var（轴保持 6），与 `defaultCloseDialog(env, 5, 6, false, ...)` 交出的 6 一致 |
| 修复 | reward 节点 7→6；自愈边反转为 `REWARD/var0=7 → set 6`（型 A：批次值→权威值） | `apply` 直接编辑；xmllint + XSD validates 2/2 |

镜像 20525 同形（`_20525Agent_Peregrine_Call` 同源结构）。

## 2. 误报确认（不修改）

| 任务 | triage "bad" 值 | 定案依据 |
|---|---|---|
| 10101 / 20101 | bad 3,4，retail {0,7,8} | legacy `defaultCloseDialog(env, 8, 9, true, false)`（reward=true 只 setStatus）落盘 **8**；XML reward 投影 8 ∈ 集合。bad 3/4 是 XML 中间步写入，落在「常量集合天然不全（uVar+1/0x110 动态推进）」的已知局限内，非 reward 投影问题 |
| 14014 | bad 6，retail 1..5,7 | legacy 击杀链 `defaultOnKillEvent(env, mobs, 5, 7)` 逐级推进到 7 后 `qs.setStatus(REWARD)`（不写 var）落盘 **7**；XML reward 投影 7 ∈ 集合。6 是击杀链中间值 |
| 14043 | bad 6,8，retail {2,4,7} | 双分支领奖：legacy `var==6 || var==8` 时 `setStatus(REWARD)` 不写 var，两分支分别落盘 6/8；XML 建 reward6/reward8 双节点（var0=6/8）与 legacy 一致。真端 `FUN_180f...` L2460049：`uVar2 = 6; if (iVar1 != 5) uVar2 = 8; SetProgress(0x36db, uVar2)` —— **变量传参的双分支写 6/8**，常量提取器只抓常量故漏收，集合 {2,4,7} 不全 |

## 3. 副作用处理

- `ArchdaevaRewardRowContractTest` 重锚：`quest10525AdvancesJournalToTheFinalRowWhenTheWorkItemIsUsed` 改名
  `quest10525KeepsTheLegacyPlayRowWhenTheWorkItemIsUsed`（旧名语义已被 QE-054 推翻，无外部引用）；
  断言 7→6、handover (6,7)→(6,6)、`assertRecovery(10525/20525, 6, 7)` → `(7, 6)`、镜像行 7→6。
- 其余引用 10525/20525 的测试（Quest10525TestimonyCounterContractTest / MissionItemConsumptionBatchRegressionTest /
  QuestIncrementRangeContractTest / Quest10520ClientDialogAlignmentTest）只涉及 var1 计数器、道具扣除与名单，不涉及 reward 投影，无需改。

## 4. 工具与产物

- 真端证据：`.agents/summary/quest-residuals-dabc/D-retail-evidence.txt`（dump_retail_evidence.py 输出）
- legacy 证据：git `3e40f5116`（10525/20525/14014/14043 handler）、`943b791e7`（10101/20101 handler）
- 修复文件：`quests/10525.xml`、`quests/20525.xml`、`ArchdaevaRewardRowContractTest.java`
