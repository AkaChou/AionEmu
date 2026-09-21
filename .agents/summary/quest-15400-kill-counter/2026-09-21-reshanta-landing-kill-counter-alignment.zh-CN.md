# 任务 15400/25400/15604/16821/26821 击杀计数收口缺陷全库排查与治理纪要

## 1. 现象与报障
- **报障任务**：15400（军团长吉斯库尔的召唤 / Aiding General Giscours，天族 66+ 主线 MISSION，`ELYOS`）。
- **现象 A**：第 6 步要求击杀 2 个 `885101`（艾莱休奇卡的侦察兵 / Ereshkigal's Searcher），击杀 1 个就跳到第 7 步。
- **现象 B**：到第 7 步后与「玛高斯据点 佩里埃尔 ID 805355」对话**无法下一步**（NPC 无任务对白标记，点击交互下发 `questId=0` 的通用第 10 页）。

## 2. 根因（同一根因的两个可见面）
`15400.xml` 第 6 步收口转移写坏了两个地方：

```xml
<transition source="s6" target="s7" priority="0">
  <event><kill-npc npc-id="885101"/></event>
  <conditions>
    <variable-is field="var0" value="6"/>
    <variable-at-least field="var1" value="0"/> <!-- 缺陷 1：阈值应为 1 -->
  </conditions>
  <actions>
    <increment-variable field="var1" delta="1"/> <!-- 缺陷 2：收口应清零 var1 -->
  </actions>
  ...
```

1. **缺陷 1（提前跳步）**：`priority=0` 的收口在 `var1 >= 0` 时恒真，第 1 次击杀就被收口抢占，`priority=1` 的自环（`variable-below var1 1`）永远不执行 → 现象 A。
2. **缺陷 2（打包步数污染）**：任务引擎按 `(varN<<6*N) | var0` 打包 `quest_vars`，且**目标节点投影不会覆盖本转移显式改写的字段**（`QuestMutationPlanner` 的 `actionTouchedFields` 语义）。收口 `increment var1` 把计数器带到下一步：第 2 次击杀后 `var1=2`、`var0=7` → 打包值 `(2<<6)|7 = 135`。
3. **客户端门控脱节**：客户端只在 `progress == 步骤` 时才为该 NPC 发起任务对白请求；`135 != 7` 时无论点多少次 805355，客户端都只下发 `questId=0` 的通用第 10 页 → 现象 B。
4. **旧存档同样中招**：已经停在 s7/s8/reward 且 `var1>0` 的存档不会自行恢复，需要一次性自愈。

## 3. 三方真端证据闭环
1. **客户端计数契约（`Quest.pak/quest_monster.csv`）**：
   ```csv
   15400,Progress(SECTION_0==6; SECTION_1<2),,simpleQuest,,1,ab1_mission_eresh_ra_65_ae
   25400,Progress(SECTION_0==6; SECTION_1<2),,simpleQuest,,1,ab1_mission_eresh_ra_65_ae
   15604,Progress(SECTION_0==1; SECTION_1<5),,simpleQuest,,1,lf6_e_environment_octaside_66_an
   16821,Progress(SECTION_0==2; SECTION_1<35),,simpleQuest,,1,ideternity_02_a_...
   26821,Progress(SECTION_0==2; SECTION_1<35),,simpleQuest,,1,ideternity_02_a_...
   ```
2. **客户端步骤行（`data_driven_quest.xml`）**：15400 第 7 行 `Talk / Ab1_Ferriere_E`（= 805355 佩里埃尔），第 6 行 `Hunt / Ab1_Mission_Eresh_Ra_65_Ae 2;`（2 杀）。
3. **旧 handler（`origin/history`）**：
   - `_15400Aiding_General_Giscours`：`var1<1` 累加、`var1==1` → `qs.setQuestVar(7)`（**清零**高位）；`_25400Pontekane_Plight` 同形（805360）。
   - `_15604Queen_Of_The_Copperclaws`：`var1` 0→4 累加，`var1==4` 再过 1 杀才 `qs.setQuestVar(2)` → **5 杀**。
   - `_16821Lost_Agent_Viola`：`var1` 0→33 累加，`var1==34` 再过 1 杀才 `qs.setQuestVar(3)` → **35 杀**。
   - 结论：收口阈值 = 客户端上限 `L`（第 L 次击杀收口），且旧版收口一律 `setQuestVar(step)`（= 清零高位）。

## 4. 全库同类审计（“严禁只修报障的单个任务”）
- `audit_stage_counter_residue.py`：以客户端 `quest_monster.csv` 的计数阶段为基准，审计全库「离开/进入计数阶段是否清零」；`audit-output.tsv` 记录 `audited_quests=689 findings=385`（含设计上合法的跨阶段残留，需逐条判定）。
- `list_kill_closure_residue_candidates.py`：只看「击杀收口累加残留」子集，输出 9 个第二批候选（13955、23955、16801、16803、16988、17160、17161、26801、26988），证据与不修理由见 `2026-09-21-kill-closure-residue-batch2-candidates.md`。
- 本批修复后，15400/25400/15604/16821/26821 在上面的审计输出中 **0 finding**。

## 5. 修复实施
| 任务 | 修复内容 |
|---|---|
| 15400 / 25400 | `s6 -> s7` 收口阈值 `0 -> 1`；收口动作 `increment var1` → `set var1 0`；`s5 -> s6` 进入计数阶段补 `set var1 0`；新增 `s7/s8/reward + enter-world + var1>=1 -> set var1 0` 自愈边（`PACKET_ONLY`） |
| 15604 | `var1` max `4 -> 5`；`s1 -> s2` 收口阈值 `3 -> 4` 且动作改 `set var1 0`；进入/离开计数阶段（`started->s1`、`s3->s4`、`s4->reward`）补清零；新增 `s2/s3/s4/reward` 自愈边 |
| 16821 / 26821 | `var1` max `34 -> 35`；13 处 `s2 -> s3` 收口阈值 `33 -> 34` 且动作改 `set var1 0`；`s1->s2`、`s4->s5`、`s5->reward` 补清零；新增 `s3/s4/s5/reward` 自愈边 |

说明：自愈边只在登录/切图触发，不改变在线玩家的既有状态；在线旧存档可用第 7 节的 GM 命令即时纠正。

## 6. 测试与门禁
- `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest15400And25400KillCounterContractTest.java`
  - 逐任务断言：`var0` 必须落在 `SECTION_0`、`var1` 必须落在 `SECTION_1(offset 6)` 且 `maxValue == 客户端需求击杀数`；
  - 自环必须 `priority=1` + `variable-below (N-1)` + 只 increment；收口必须 `priority=0` + `variable-at-least (N-1)` + 动作**只有** `set var1 0`、after-commit **只有** `PACKET_ONLY`；
  - 进入计数阶段必须清零；被污染阶段必须有唯一 `enter-world` 自愈边（条件精确、动作 `set var1 0`）；
  - 行为模拟：用真实 `QuestMutationPlanner` 跑真实连杀序列，断言第 N 杀后 `packedVariables == stageNext`（纯净步数），并用污染值重放 `enter-world` 验证自愈收敛。
- 基线说明：`src/test/resources/quest/quest-kill-counter-retail-contract.tsv`（`QuestKillCounterRetailGateTest`，归零后 414 行）当前**未收录**这 5 个任务（旧形态收口阈值错误，模拟器算出的击杀数与客户端门控不符，生成时被排除）。修复后可把这 5 个任务（门控 2/2/5/35/35）补进基线并同步 `EXPECTED_CONTRACT_ROWS`，让零售门禁长期守护；补基线前必须先跑 `QuestKillCounterSimulator` 验证，故同样列为 PENDING。
- 待执行（**未授权 Maven，保持 PENDING**）：
  - `mvn -q -Dtest=Quest15400And25400KillCounterContractTest test`
  - 同批门禁：`QuestDefinitionCatalogManifestTest`、`ProductionCatalogWhitelistVerificationTest`、`QuestMonsterProgressContractAuditTest`、`QuestCollectProgressAlignmentGateTest`、`QuestKillCounterRetailGateTest`、`QuestDialog31RegressionTest`

## 7. 状态与运维
- 静态校验：`xmllint --noout` 5 个 XML 通过、`git diff --check` 通过、IDE 静态检查 0 error。
- 修复需要**重启/重载任务定义**后生效（旧存档由自愈边或 GM 命令纠正）。
- GM 命令语法（`src/main/java/com/aionemu/gameserver/commands/admin/Quest.java`）：`//quest set <questId> <START|NONE|REWARD|COMPLETE> <var> [varNum]`
  - 省略 `varNum`（=0）→ `QuestState.setQuestVar(var)`：**var0=var 且其余 SECTION 全部归零**；
  - 给出 `varNum=N` → `setQuestVarById(N, var)`：只改该 SECTION，保留 var0。
- **回到击杀前状态**（天族 15400，s6 = 杀 2 个 885101 之前，计数归零）：
  - `//quest set 15400 START 6`（推荐：一步到位，var0=6 且 var1=0）
  - 等价两步写法：`//quest set 15400 START 6` 后 `//quest set 15400 START 0 1`（第二句写 var1=0）
- **已被污染、卡在 s7 无法与 805355 对话**（现象 B，保留当前步只清计数）：
  - `//quest set 15400 START 0 1`（只清 var1，var0 仍为 7）
  - 或 `//quest set 15400 START 7`（var0=7 且 var1=0），或让玩家重新上线触发新增自愈边
- 魔族孪生 25400 同形（805360）：`//quest set 25400 START 6`（回击杀前）/ `//quest set 25400 START 0 1`（解卡）
- 未提交：工作区改动保留，等待用户授权测试与提交。
