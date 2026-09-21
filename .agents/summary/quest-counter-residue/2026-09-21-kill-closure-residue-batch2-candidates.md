# 击杀收口残留同形态候选（第二批，未修改）

- 生成脚本：`list_kill_closure_residue_candidates.py`（默认只列 `increment-variable` 累加式收口；`--include-sets` 追加赋值式收口）
- 判定口径与 QE-044 规则 A 一致：从客户端 `quest_monster.csv` 声明的计数阶段（`SECTION_0==S; SECTION_C<L`）离开时，收口转移既不清零 `varC`，目标节点投影也不声明 `varC`，就会把 `(varC<<6*C)|T` 带进下一阶段。
- 结论：候选 9 个，**均尚未改动**；本轮只修了 15400/25400/15604/16821/26821。

## 候选清单（脚本输出）

| 任务 | 收口转移 | 残留动作 | 击杀目标 | 客户端计数契约 |
|---|---|---|---|---|
| 13955 | `started(stage 0)->reward` | `increment var2` | kill-npc 246561 | `SECTION_0==0; SECTION_1<5` + `SECTION_0==0; SECTION_2<5` |
| 23955 | `started(stage 0)->reward` | `increment var2` | kill-npc 246561 | 同上（魔族孪生） |
| 16801 | `started(stage 0)->reward` | `increment var1` | kill-npc 220305 等 9 种 | `SECTION_0==0; SECTION_1<30` |
| 26801 | `started(stage 0)->reward` | `increment var1` | kill-npc 220305 等 8 种 | `SECTION_0==0; SECTION_1<30` |
| 16803 | `started(stage 0)->k1` | `increment var1` | kill-npc 220307 等 8 种 | `SECTION_0==0; SECTION_1<30` |
| 16988 | `started(stage 0)->k1` | `increment var1` | kill-npc 233129/233130/233131 | `SECTION_0==0; SECTION_1<5` |
| 26988 | `started(stage 0)->k1` | `increment var1` | kill-npc 233126/233127/233128 | `SECTION_0==0; SECTION_1<5` |
| 17160 | `started(stage 0)->k1` | `increment var1` | kill-npc 235830/235916/235912 | `SECTION_0==0; SECTION_1<10` |
| 17161 | `started(stage 0)->k1` | `increment var1` | kill-npc 219699/219787/219776 | `SECTION_0==0; SECTION_1<10` |

小计：9 个任务，全部为「step 0 计数 + 收口累加」形态，收口后把计数器带进 `reward`/`k1`。

## 为什么本轮不动

1. **13955/23955 与旧 handler 有真实偏离**：`origin/history` 的 `_13955A_Diversion`/`_23955Diversion_Tactics`（删除提交 `67bb553a2`）在收口处执行 `qs.setQuestVar(1)`，语义是「var0=1 且 var1/var2 全部清零」；当前 XML 的 `started -> reward priority=1` 却只做 `increment var2` + `set var0=1`，REWARD 打包值会变成 `(var2<<12)|(var1<<6)|1`（例：var1=5,var2=1 → 4417），与旧版 `1` 不符。修法方向明确（收口清零 var1/var2 = `setQuestVar(1)`），但必须先按 QE-045 口径确认客户端领奖入口页门控是否按整型步数判定，再连同门禁测试一起改。
2. **另外 7 个与旧 handler 一致**：`_16801Silence_In_The_Library`、`_16803Hunting_Reliquarians`、`_16988Asmo_Squad_Wipeout`、`_17160Mutant_Monster_Mash`、`_17161Frenzied_Monsters_In_The_Fray`、`_26801Beware_The_Librarians`、`_26988Expunge_The_Expeditionary_Squad` 的收口都是 `qs.setQuestVarById(0, 1)`，**刻意保留**计数器（收口累加与旧版等价）。要判定它们是否同样打断对白，需要客户端侧证据（`data_driven_quest.xml` 步骤行 + 客户端门控或零售包序），不能仅凭审计脚本批量清零。
3. 15400 之所以确诊，是因为同时具备三方证据：客户端 `quest_monster.csv`（`SECTION_1<2`）、客户端 DDQ 第 7 行 = `Ab1_Ferriere_E`、旧 handler `_15400Aiding_General_Giscours` 的收口 `qs.setQuestVar(7)`（**清零** 高位）+ 用户实机「805355 无法下一步」。第二批候选缺第 2/3 条中的客户端侧证据。

## 下一步（待用户确认）

- 先补客户端证据：对这 9 个任务的下一阶段（`k1`/`reward`）确认客户端是否按整型 `progress == 步骤` 门控；13955/23955 另需按 QE-045 核对领奖入口页。
- 证据齐备后按本轮同一机械改法修复：收口 `increment-variable` → `set-variable 0`，补进入计数阶段的清零与 `enter-world` 自愈边，并扩写 `Quest15400And25400KillCounterContractTest`（或新增同族测试）与门禁。
