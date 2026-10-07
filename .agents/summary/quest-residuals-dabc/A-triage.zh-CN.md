# 遗留问题 A 组取证：表车道 5 行（15613/25023/25606/80020/80021）现状投影复核

- 日期：2026-10-07
- 触发：QE-054 全量收口 AUDIT §5 遗留问题 A（"表车道现状如何投影需另查"）。
- 判据：原生车道代码（表驱动）+ 真端表数据 + 退役壳 XML（git 历史）+ 客户端行数四源交叉。
- 结论：**5 行原生投影与退役壳 XML/批次值完全一致，与真端表结构自洽——遗留问题 A 关闭，无需修复。**

## 1. 逐任务四源对照

| quest | 族 | 客户端行数 | 退役壳 XML reward | 批次值 | 原生车道投影 | 对齐 |
|---|---|---|---|---|---|---|
| 15613 | DataDriven | 7（0..6） | 6 | 6 | **6**（DD 表 6 步，末步收口写 `(prog&0x3F)+1 = 6`） | ✅ |
| 25023 | DataDriven | 4（0..3） | 3 | 3 | **3**（DD 表 3 步） | ✅ |
| 25606 | DataDriven | 9（0..8） | 8 | 8 | **8**（DD 表 8 步） | ✅ |
| 80020 | SimpleTalk | 4（0..3） | 3 | 3 | **3**（3 中继，中继步进写 1/2/3，报告态保持 3） | ✅ |
| 80021 | SimpleTalk | 4（0..3） | 3 | 3 | **3**（3 中继，同 80020） | ✅ |

## 2. 原生投影机制（代码证据）

- **DataDriven**（`DataDrivenNativeRuntime#apply` L2231-2237 + `DataDrivenProgress.advance` L116-118）：
  raw vars = 6 位步号 + 组槽；末步收口 `STEP_COMPLETE` 写 `(vars & 0x3F) + 1` 后 `SM_QUEST_ACTION(quest, REWARD, newVars)`。
  6/3/8 步 ⇒ REWARD 步 = 6/3/8 = 各自客户端末行索引。
- **SimpleTalk**（`SimpleTalkHandler` L949-977 中继段 + L1036-1045 报告段 + L673-695 进世界自愈）：
  中继第 K 步 `vars == K-1` 时写 `vars = K`（K=1..3）；报告确认后 `SM_QUEST_ACTION(quest, REWARD, vars)` 保持 3。
  进世界自愈注释明确「链形行 REWARD 态 vars=0（XML 时代存档）→ vars = 中继步数（真端投影行）」，即**真端投影行 = 中继数**。
- 两族投影均由真端表（`data_driven_quest.xml` / `Quest_SimpleTalk.xml`）结构决定，非人工投影值，无「抬行」可发。

## 3. 批次值与壳 XML 的来源（git 历史）

- `7a7d27809`（领奖行批次 1-7）改过 `quest_definition/quests/{15613,25023,25606,80020,80021}.xml`（reward 抬到末行索引，即批次值 6/3/8/3/3）。
- `4ede058c0`（任务生命周期迁移到真端表驱动）删除壳 XML，所有权移交表车道（retention owner=RETAIL_TABLE）。
- 关键：**表车道独立从真端表推出同一组值**（DD 步数 / 中继数），与批次抬行结论巧合一致——这 5 行不属于 QE-054「抬行是错的」模式（那里的错在轴值 ≠ 真端玩法落盘值；这里表结构本身决定了收口值 = 末行索引）。

## 4. legacy 落 2 的解释（AUDIT 原文的疑点）

80020/80021 的 legacy `defaultCloseDialog(2, 3, true)` 按旧引擎 reward=true 只 setStatus 语义落 2——那是 legacy 自有链设计（末中继交付兼作报告）。表车道按真端 cabb10 语义重实现为「中继步进写步号 + 交付面报告」，落 3。**表车道为所有权方，legacy 不再是权威**；批次值 3 与表车道一致，说明此处批次的抬行恰好与真端表一致。

## 5. 遗留风险与门覆盖

- 这 5 行无专属 REWARD 步锁定门（`JournalRewardRowRepairContractTest.RETAIL_DRIVEN` 只断言 retired 身份；SimpleTalk/DD 家族门覆盖行对齐与语义，无逐任务投影断言）。
- 现值由表数据驱动：若未来编辑 `Quest_SimpleTalk.xml` 中继列或 DD 表步块，投影跟随变化——属数据变更评审范围，不另造门。
- 工具：`dump_retail_evidence.py` 对这 5 任务无按任务号常量（表驱动家族，ScriptDLL64.c 无 per-quest SetProgress），与 crosscheck 的 NO_RETAIL 一致。
