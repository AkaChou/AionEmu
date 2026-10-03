# P7 步 2 前置：DD 步列（`valueN_progress_`）→ 真端语义坐实

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 日期：2026-10-01。分支：`quest`。性质：**只读真端原码考古 + 门禁补强**（零运行时变更）。
> 动机：P7 步 1 报告 §4 把 `6/9/10` 列与 `5` 的 hunt/talk 生成形标为「待坐实」。本批把
> **每列 → 真端的读写语义**逐条坐实（真端 `ScriptDLL64.dll` 反编译），该缺口关闭。
> 证据根：`<真端根>/server58/MainServer_ScriptDLL64/ScriptDLL64.c`（Ghidra 全量反编译，行号如下）。

---

## 1. 真端解析链（两个函数，一次调用两个动作）

`DataDrivenQuestLoader::LoadProgressInfo`（`/* 180c4b330 */`，行 2073529）遍历 `<data>` 的每个属性：

1. 属性标签 `0xfe9` = `category_progress_`（日志串 `T_PROGRESSn_CATEGORY`，行 2073607）⇒ 按值
   （`CollectItem`/`Hunt`/…）构造对应的 `QuestProgressExtraInfo_*` 对象（对象类型即**步 kind**）；
2. 属性标签 `0xfec..0xff6` = `value0_progress_..value10_progress_`（映射 `uVar10 = 0..10`，行 2073818-2073847）
   ⇒ 调 `FUN_180c4b980(kind, index, 值)`；
3. `FUN_180c4b980`（`/* 180c4b980 */`，行 2073865）按 **kind** 解析「类别载荷」，解析完（或该类别不管这个 index 时）
   **尾调** `FUN_180c49610(index, kind, 值)`（行 2074475）——即 `DataDrivenQuestLoader::LoadExtraAction`
   （`/* 180c49610 */`，行 2072615）解析「通用附加动作」。

**关键判据**：每个 kind 的 `switch` 分支以 `return 1` 结束者**不挂附加动作**；以 `break` 结束者才落到
附加动作调用（逐分支实测：`case 1` CollectItem = `return 1`；`case 5` PvP = `return 1`；`case 2/3/4/6/7/8/10/9`
= `break`）。

---

## 2. 通用附加动作（`FUN_180c49610`，按 index 分派）

标签 `%d` = `index+1`（`LoadExtraAction() Wrong Category for Extra Action : %d` 的入参是 kind；见 §3 适用表）：

| index | 语义（真端日志串） | 解析形（实测数据） |
|---:|---|---|
| 1 / 2 | `DataDrivenQuest - Give/Remove Items` | `符号 数量`（可多个，`,`/空格分隔）——1 = 发、2 = 扣（`GiveItems/Remove item`） |
| 3 | `DataDrivenQuest - Teleport To` | `世界 x y z heading`（实测 talk 3 行：`210050000 1440 408 553 77`） |
| 4 | `DataDrivenQuest - Play Cutscene` | `Cutscene N` / `Cutscene2 N` / `Movie N` / `Movie2 N` [+ `HACTION_*` 超链接 id]（`Wrong Type!!` 分支列举四种类型） |
| 5 | `DataDrivenQuest - Spawn Npcs` | `Absolute|Relative 名, 数量, 时间[, x y z h]`（`Wrong NPC Spawn Type!!`） |
| 6 | `DataDrivenQuest - Delay Time` | 整数（毫秒） |
| 7 / 8 | `DataDrivenQuest - Message` | 字符串索引（`Wrong String Index!!`）——实测 `STR_QUEST_SAY_*`/`STR_CHAT_*` |
| 9 | `DataDrivenQuest - Enter Instance` | `创建 id, 世界 id, 离开进度`（`Enter Instance - Ins Creation ID/World ID/Leave Progress Not Exists!!`） |
| 10 | `DataDrivenQuest - Add Timer` | `时间, 目标进度, 动作 id`（`Add Timer - Timer Time/Dest Progress Not Exists!!`，写入 `+0x68/+0x6c`） |

guard（行 2072659 附近）：`if ((kind < 2) || (4 < kind && 4 < (u32)(kind - 6)))` ⇒ 只有 kind ∈ {2,3,4,6,7,8,9,10}
允许附加动作；kind 1（CollectItem）与 5（PvP）**没有**附加动作面。

---

## 3. 类别载荷（`FUN_180c4b980`，按 kind × index）

| kind | 类别 | index 0（载荷） | 其它 index | 附加动作 |
|---:|---|---|---|---|
| 1 | CollectItem | NPC 名（写 `+8`） | 1..4 = 追加 FOBJ 名（写 `+0x28` 向量）；5 = 整数（写 `+0x40`） | ❌（return 1） |
| 2 | Hunt | 子目标组（`名列表 + 计数`，`,`/`;`/空格分词，写 `+0x08/+0x18` 等） | 只有 **4/5** 落到附加动作（`if (1 < (u32)(index-4)) return 1`）；其余忽略 | ✅ 仅 index 4/5 |
| 3 | ItemPlay | 道具玩法串（`DataDrivenQuest - QPCAT_ITEM_PLAY`，含名字 id 解析） | 全部落附加动作 | ✅ |
| 4 | Talk | 对话 NPC 名（`Talk Progress`） | 全部落附加动作 | ✅ |
| 5 | PvP | 击杀数（`PvP Kill Num`） | 1 = `PvP Target Min Rank`、2 = `PvP Target Max Rank`、3 = `PvP Target Level Gap` | ❌（return 1） |
| 6 | EnterArea | 感知区名（`Enter Sensory Area`） | 全部落附加动作 | ✅ |
| 7 | EnterWorld | 世界 id（`Enter World`） | 全部落附加动作 | ✅ |
| 8 / 10 | LevelUp / LevelUpLogIn | 等级（`Level Up / Level Up LogIn`） | 全部落附加动作 | ✅ |
| 9 | TalkFOBJ | FOBJ 名列表（`Talk FOBJ`）+ `+0x20` 击杀手选项 + `+0x38` | 全部落附加动作 | ✅ |

动作表解释器（步完成/推进后执行）：`FUN_180c4cd50`（`/* 180c4cd50 */`，行 2074638）、
`FUN_180c4d190`（Hunt 专用，行 2074785）、`FUN_180c4c8d0`（EnterArea/TalkFOBJ，行 2074485）。

---

## 4. 与本服实测列面的对齐（0 例外）

P7 步 1 复算的切换集列直方图（`p7/dd-native-shape-matrix.tsv`）与上表**逐列相符**：

| 类别 | 实测非零列 | 与真端一致 |
|---|---|---|
| hunt | 4、5 | ✅（Hunt 只允许 4/5） |
| collectitem | 1、2、3、4、5 | ✅（1..4 追加 FOBJ、5 整数） |
| pvp | 1、2、3 | ✅（无附加动作） |
| talk | 1、2、3、4、5、6、7、9、10 | ✅（全套附加动作） |
| enterarea | 4、5、7、10 | ✅ |
| itemplay | 1、2、4、5、7、10 | ✅ |
| enterworld | 1、2、4 | ✅ |
| talkfobj | 1、2、3、4、5、6、10 | ✅ |

⇒ P7 步 2 的实现面 = **9 类类别载荷**（8 progress + LevelUp/LevelUpLogIn 的接取载荷）+ **8 类附加动作**
（1/2 发扣、3 传送、4 过场、5 生成、6 延迟、7/8 消息、9 进副本、10 定时器）；未出现在切换集的组合
（如 CollectItem 的 6..10、PvP 的 4..10）在真端同样非法 ⇒ 运行时一律 fail-closed。

---

## 5. 本批门禁补强

`DataDrivenNativeContractGateTest` 增 `everyStepColumnCarriesAdjudicatedRetailSemantics`：
把 §2/§3 的表编成模式断言（每列必须落在「类别载荷 / 附加动作 / 非法」三类之一），
对切换集逐 (类别, index) 复算——非法组合零命中、未知组合零命中。

```bash
mvn -o test -Dtest=DataDrivenNativeContractGateTest -DfailIfNoTests=false
```

**门态（2026-10-01）**：门 **7/7**（原 6 + 本批语义闭合 1）；族门 + tablelane **145/145**（日志 `gates/2026-10-01-p7-step2-prep-family-tablane.log`）；聚焦套件 **1699 例 / 161F+137E / 105 红类**，对 QE-112 基线 **ADDED 0 / REMOVED 0 / changed 0**（`gates/2026-10-01-focused-run-p7-step2-prep*.tsv`）。

边界（诚实声明）：本批只坐实**列 → 语义/解析形**；附加动作的**执行时机**（步完成/推进后由动作表解释器执行）
与**逐动作副作用细节**（发扣的批量语义、生成 NPC 的生命周期、定时器目标进度写回）仍属 P7 步 2 的实现面，
本批不把它们写死成运行时行为。
