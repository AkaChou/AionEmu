# 任务 1361/1373 取水步骤空白诊断与修复报告

- 日期：2026-10-07
- 任务：1373（天族 34级 Water Therapy / 帕诺的特制温泉水）与 1361（天族 31级 Finding Drinking Water / 取得饮用水）
- 状态：**CLIENT_ACCEPTED**（2026-10-07；修复提交 `a279fe6da`）

---

## 1. 现场故障现象

### 1.1 玩家报障（2026-10-07，玩家 Kk）

1. **1373**：使用保温瓶（182201372）打水后，任务书「任务说明」里只剩描述行
   （“按照阿埃洛佩的拜托，去为长疮的病人打取温泉水吧。”），**两行步骤全部消失**。
2. **1361**：获取到水后同样现象（“难民们正在经受干渴的折磨。帮他们打回可以饮用的水吧。”，
   步骤行全空）。

### 1.2 1373 现场 Trace

```text
10-07 20:25:43 [S->C] SM_QUEST_ACTION 任务=1373 状态=3 步数=0
10-07 20:25:43 [S->C] SM_DIALOG_WINDOW 玩家=Kk targetObj=41305 questId=1373 下发页=1003
10-07 20:26:05 [C->S] CM_USE_ITEM 玩家=Kk 道具=182201372 itemObj=183674
10-07 20:26:08 [S->C] SM_QUEST_ACTION 任务=1373 状态=3 步数=1   ← 错轴值
```

---

## 2. 客户端权威数据（两任务同族：quest_script_monster 的 itemUseArea）

`<客户端解包根>/Quest_unpacked/quest.xml`：

| 任务 | work item | collect_progress | 客户端脚本表 `quest_script_monster.csv` |
|---|---|---|---|
| 1373 | quest_1373a / quest_1373b | 1 | `1373,Progress(0),quest_1373a,itemUseArea,,1,usearea_lf2_itemusearea_q1373` |
| 1361 | quest_1361a / quest_1361b | （无 collect_progress） | `1361,Progress(0),quest_1361a,itemUseArea,,1,usearea_lf2_itemusearea_q1361` |

CHS 任务书页（`L10N/CHS/Data/data.pak` 解密，副本见本目录 `CHS_QUEST_Q1373.html`）：

- 1373 共 2 行：`[%0]` 打水 / `[%3]` 把保温瓶交给阿埃洛佩。
- 1361 共 3 行：`[%0]` 打水 / `[%3]` 灌满水箱 / `[%6]` 和 Turiel 对话。

**行 1 的 visible 槽位 `[%3]` 的绑定值**（真端 `FUN_180cb3070(&slot,&obj,questId,3,值,0)` 注册）：

- 1373：`值 = 2`（`&DAT_185f3efe3, &DAT_185f3eff0(Aerope), 0x55d, 3, 2, 0`）
- 1361：`值 = 1`（`&DAT_185f3bdac, &DAT_185f3c3f0(LF2_Watertank_Q1361), 0x551, 3, 1, 0`）

---

## 3. 根因

### 3.1 1373：collect_progress 机械推断覆盖权威步号

| 阶段 | legacy `_1373WaterTherapy`（迁移前 handler） | 真端 `FUN_180effb70` | 9-19 批次后 XML（错） |
|---|---|---|---|
| 打水后 | `qs.setQuestVar(2)` | `status==START && step==0` → `SetProgress(0x55d, 2)` + 180s 计时器 | **var0=1** |
| 交付条件 | `qs.getQuestVarById(0) == 2` | （检查面读任务轴） | var0==1 |
| 交付后 REWARD | `checkQuestItems(env, 2, 3, true, 5, 2716)`；旧引擎 reward 分支**不写 nextStep**，落盘 2 | 发页 2716 + `0x160` 状态推进，不带步号 | **var0=1** |

错误来源：commit `7d5bb5317`（2026-09-19「收集进度对齐」批次）按
“客户端 collect_progress=1”推断把 `v2(2)` 改成 `v1(1)`，并把 legacy 迁移期自愈边写成
带 source 的 `v1 + var0==2 → 1`（该形态因 `QuestMutationPlanner#matchesSourceNode` 要求
source 节点投影全等而**结构性失效**）。该批 README 中 1373 标记
`PENDING_CLIENT_ACCEPTANCE`，从未实机复验。

### 3.2 1361：领奖行批次按末行索引抬升

| 阶段 | legacy `_1361FindingDrinkingWater` | 真端 | 领奖行批次后 XML（错） |
|---|---|---|---|
| 打水后 | `qs.setQuestVar(1)` | `FUN_180f01180` `SetProgress(0x551, 1)` | var0=1（一致，无错） |
| 灌满水箱 → REWARD | `useQuestObject(env, 1, 1, true, 0, 0, 0, 182201327, 1)` → reward 分支落盘 **1** | `FUN_180f98460` `0x100(0x551,0,0)` 推进、不带步号 → 保持 1 | **var0=2** |

错误来源：commit `7a7d27809`（「领奖行全库审计批次 1-7」，375 个任务）把 reward 投影
“收口”为客户端末行索引 2（3 行任务），与真端/legacy 的 1 冲突——与 1926/2938
（2026-10-07 同型实机报障，commit `529b75a9e`）为同一错误族。

### 3.3 统一口径

两任务同属 QE-054/QE-045 教训：**取水/itemUseArea 族（以及一切非脚本自算进度的任务）的
中间步与领奖态 packed step 以真端 `SetProgress` 实际值与 legacy 落盘值为权威**，
不得按 `collect_progress` 或“末行索引”机械推断。
真端槽位注册 `FUN_180cb3070(_,_,questId,3,值,0)` 的第 5 参（行 1 绑定值）可作为
独立交叉证据（1373=2、1361=1，均与 legacy 打水后值一致）。

---

## 4. 修复

### 4.1 生产定义

- `1373.xml`：节点 `v1(1) → v2(2)`；reward 投影 `1 → 2`；所有 `v1` 路由改回 `v2`；
  9-19 的失效自愈边替换为**无 source** 边 `START && var0==1 && has-item(182201373) → 2`
  （`PACKET_ONLY`）；新增无 source 边 `REWARD && var0==1 → 2`
  （`LEVEL_AND_VISIBILITY_REFRESH`）。
- `1361.xml`：reward 投影 `2 → 1`；原自愈边反转条件/动作
  （`REWARD && var0==2 → 1`，`LEVEL_AND_VISIBILITY_REFRESH`）。
- 两任务的 `complete`、对话页、道具动作、计时器均未改动
  （1361 的领奖页 1352 在 `client_dialog_contract.tsv` 有声明，合法保留）。

### 4.2 测试

- 更新 `Quest1373ClientDialogAlignmentTest`：节点轴 v2/reward=2 + 两条无 source 自愈边断言。
- 新增 `Quest1361ClientDialogAlignmentTest`：0/1/1 轴 + 打水/灌水箱分支 + `REWARD/2 → 1` 自愈边。
- `JournalRewardRowRepairContractTest`：1361 移出 `CONTRACTS`（附双语说明，口径由新测试接管）。

---

## 5. 验证与验收状态

- 静态自检：两 XML 通过 XML 解析；diff 复核无残留 `v1`/意外变更。
- 聚焦测试（IDEA MCP，2026-10-07）：`Quest1361ClientDialogAlignmentTest` 1/1、
  `Quest1373ClientDialogAlignmentTest` 1/1、`JournalRewardRowRepairContractTest` 4/4、
  `QuestDefinitionCatalogManifestTest` 10/10、`ProductionCatalogWhitelistVerificationTest` 1/1
  （`PRODUCTION_COMPILE_OK=707` / `FAILURES=0` / `WHITELIST_VIOLATIONS=0`），均 exit 0。
- 客户端实机：**CLIENT_ACCEPTED（2026-10-07）**——用户冷重启后走通两任务整链并回复
  「实机验证成功，提交」；旧存档自愈（本角色 1373 `START/var0=1` 持水）一并实机覆盖。
  验收记录：`.agents/summary/quest-acceptance/1361-1373-2026-10-07-client-accepted.md`。
- 修复提交：`a279fe6da`。

## 6. 剩余风险

- **同族批次遗留**：`7a7d27809`（375 任务领奖行）与 `7d5bb5317`（收集进度族）中可能存在
  其它“按索引推断覆盖真端/legacy 值”的受害者（QE-045 boundaries 已登记同类 106 个 MISMATCH
  待逐族复核）；本次仅修玩家实机报障的 1361/1373。
- 真端槽位注册法（`FUN_180cb3070` 第 5 参）建议在后续逐族复核中作为交叉证据来源。
