# P7 步 2 步 d 报告：DD 原生进度运行时（6 类接线 + 真端算术守卫修正，零行为变更）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 批次：P7 DataDriven 步 2 步 d（计划 §7「P7 DataDriven」/ §10.2 / §10.3-#24）
- 日期：2026-10-02（承接步 c 的 2026-10-01 收口）
- 结论：DD 原生**进度运行时**落地——按真端「每步一条 handler 记录」建兴趣面与执行面（Hunt / PvP / Talk /
  EnterArea / EnterWorld / TalkFOBJ 六类接线；CollectItem / ItemPlay 留步 d2）。生产口径**路由集为空**：
  DD 切换集仍由旧 IR 车道 owns ⇒ 事件恒 false、零兴趣注册，**零行为变更**。
- 同批按真端原码**修正 `DataDrivenProgress` 的自增守卫**（QE-128 补正）：自增只在 `counter < target` 时发生。

## 1. 真端事实（原码逐函数，本次复读）

| 类别 | 真端 handler | 本次坐实的关键语义 |
|---|---|---|
| Hunt | `FUN_180c46020`（`ScriptDLL64.c:2069982`） | ① 步号守卫 `(vars & 0x3f) != expectedStep → return`；② **4 组**（移位 6/12/18/24；第 5 组落在守卫位，零使用）；③ 组内命中：`if (counter < target) { vars += 1 << shift; }` ——**自增有守卫**，满组槽超杀零写（不污染下一组）；④ 全部组槽达标才步进；⑤ 步进后 < 步数 ⇒ `SetQuestProgress(0xF0)`，否则 `SetQuestSuccess(0x100)`；⑥ 尾部调 `FUN_180c4d190(def+0x20)` 执行附加动作 |
| PvP | `FUN_180c46980` | 单组计数（`(vars>>6 & 0x3f)+1 < target` 则自增，否则步进）；闸门三条：`killerLevel <= victimLevel + levelGap`（def+0x14）、`minRank == 0 \|\| minRank <= victimRank`（def+0xC）、`maxRank == 0 \|\| victimRank <= maxRank`（def+0x10） |
| Talk | `FUN_180c466a0` | 状态必须 = 3（START）；`ctx+0xC == def+0x48`（步号）且 `(vars&0x3f) < def+0x44` ⇒ 写 `def+0x40`（目标步；`< 0` 走 `DeleteWorkingQuest`）；随后逐项 `RemoveItemMax`（附加动作面，留步 e） |
| TalkFOBJ | `FUN_180c478e0` | 组计数**二值**（仅 `counter == 0` 时置 1 并跑该组动作类型 0/1/2）；载荷尾整数是**动作类型**，不是计数；全部组槽非 0 才步进 |
| EnterArea | `FUN_180c47bf0` | 名哈希逐值比对（同名区，QE-130）后**直接步进**（`< 步数` ⇒ `SetQuestProgress`，否则 `SetQuestSuccess`）+ 附加动作 |
| EnterWorld | `FUN_180c467b0` | 步号校验后**直接步进** |

> **修正（QE-128 补正）**：步 a 把命中自增写成「无掩码自增」，但真端 `FUN_180c46020`/`FUN_180c46980`
> 的自增在 `counter < target` 守卫之内——满组槽超杀**不写 vars**（因此不会进位污染下一组）。80817 的
> 100 杀形（target 100 > 6 位上限 63）在守卫下依然**不可完成**（槽回绕后读数永 `< 100`），裁定不变。
> 本批把守卫补进 `DataDrivenProgress.hit` 并加门用例（`saturatedGroupIsNotIncrementedAgain`）。

## 2. 载荷语法（逐类复算，切换集 1467 行）

| 类别 | 载荷形 | 语法 |
|---|---|---|
| `hunt` | `名字1, 名字2, …, <计数>; 名字…, <计数>;` | `;` 分组（≤4 组）、组内名字逗号分隔、尾整数（**空格或逗号**前导）为计数，缺省 1；名字含空白时按空白拆分的回退（真端表有 `LF2A_StatueT_49_An LF2A_StatueT_50_An` 形） |
| `talk` | 单个 NPC 名 | 名解析必须 `UNIQUE`，否则整行冻结 |
| `talkfobj` | `名字, <动作类型>` | 二值组槽（目标恒 1），尾整数是动作类型 |
| `enterarea` | 同名区别名 | 走 `NativeEnterAreaPort`（步 c） |
| `enterworld` | world id（整数） | 直接步进 |
| `pvp` | `<击杀数>` + 列 1/2/3 | 列 1 = `PvP Target Min Rank`、列 2 = `Max Rank`、列 3 = `Level Gap`（QE-127） |
| `collectitem` / `itemplay` | — | **本批不接线**（整行冻结，见 §4） |

## 3. 落地物

| 交付 | 说明 |
|---|---|
| `src/main/java/.../tablelane/DataDrivenNativeRuntime.java` | 运行时时序：按路由集逐行建兴趣面（击杀/对话/FOBJ/进区/进世界/PvP 行集）+ 六类事件入口（`onKill`/`onDialog`/`onEnterZone`/`onEnterWorld`/`onKillRanked`/`installInterest`）+ 真端算术执行（`DataDrivenProgress`）+ 状态写回（`UPDATE_REQUIRED` + `SM_QUEST_ACTION`，末步 `REWARD`） |
| `src/main/java/.../tablelane/DataDrivenProgress.java` | 补真端自增守卫（`counter < target`）；全组达标形仍按 `bVar3` 分支收口 |
| `src/main/java/.../questEngine/QuestEngine.java` | 接线点：`onKill` / `onDialog` / `onEnterZone` / `onEnterWorld` / `onKillRanked` / `installInterest`（生产路由集为空 ⇒ 恒 false） |
| `src/test/java/.../tablelane/DataDrivenNativeRuntimeGateTest.java` | 新门 **9 例**：生产零路由（零行为变更）/ 切换集两桶互斥闭合 / 兴趣面逐元素复算 / Hunt 组计数 / Talk·EnterArea·EnterWorld 直接步进 / TalkFOBJ 二值 / PvP 闸门（含失败零写）/ 冻结行不入兴趣面 / 分桶与逐类步数冻结 |
| `src/test/java/.../tablelane/DataDrivenProgressTest.java` | 补 `saturatedGroupIsNotIncrementedAgain`（满组槽超杀零写 + 全组达标仍收口）⇒ **9/9** |

## 4. 分桶与冻结面（本批口径）

- 切换集 **1467 行**；本批 **可路由 1017 行 / 冻结 450 行**：
  - `KIND_NOT_WIRED` **374**（含 `collectitem`/`itemplay` 步的行 ⇒ 步 d2 接线）；
  - `NAME_UNRESOLVED` **68**（未解析名字 48 个，见 §5）；
  - `ZONE_ABSENT` **8**（LF6 侧真端缺席别名，§10.3-#23 冻结面）。
- 已路由行的逐类步数：hunt **697** / pvp **205** / talk **212** / enterarea **117** / enterworld **32** /
  talkfobj **15**；切换集逐类步数与 P7 步 1 契约**逐值一致**（hunt 827 / collectitem 348 / pvp 207 /
  talk 403 / enterarea 153 / itemplay 42 / enterworld 34 / talkfobj 19）——独立复算交叉验证。
- **整行原子可路由**：任一步不可服务即整行冻结（切换批不允许半行原生半行旧 IR）。

## 5. 未解析名字（48 个；步 d2 的前置清单）

- **副本全图击杀名**：`IDAbRe_Low_Divine`、`IDAbRe_Low_Eciel`、`IDAbRe_Low_Wciel`、`IDSeal_A_Single`、
  `IDSeal_Twin_Q18952`、`IDEternity_02_A_S1_Zone`、`IDTransform_TransRoom_01_Ai`、`IDF5_U01_N_Boss` 等
  —— SimpleHunt 家族已有的「副本世界全图击杀」规则需按同轴搬进 DD 名字解析（PQ §8.5 阻塞点二）。
- **未登记 boss / 事件名**：`Ab1_1131_Boss_Dr_Q1737`、`Ab1_1221_Boss_Dr_Q1739`、`DF6_B_Event_G3_2S`、
  `F6_Raid_Drop_Dark`、`DF5_P1_NewMerman_Fi_58_Ae`、`LF4_Rotation_65_Deva_Q15321_01` 等（含等级/阵营后缀形）。
- **真端缺席别名**：`LF6_SensoryArea_Q15551_AtoB` 等 8 个（§10.3-#23，进区轴已冻结）。
- 处置：**不猜、不近似**（无“去掉后缀再解析”这类换算规则）；步 d2 逐类坐实后单独登记。

## 6. 门态（本批显式命令）

```
# 新门 + 算术门
mvn -o test -Dtest='DataDrivenNativeRuntimeGateTest,DataDrivenProgressTest' -DfailIfNoTests=false   # 9 + 9 全绿
# 族门 + tablelane（含 zone 注册两门）
mvn -o test '-Dtest=*FamilyGateTest,*RowAlignmentGateTest,*InventoryGateTest,NativeQuestTableLoaderTest,*RewardClaimGateTest,*NativeTalk*Test,NativeNearbyQuestAxisGateTest,SMNearbyQuestsPacketTest,NativeAcceptEntryAskFlowGateTest,DataDrivenNativeContractGateTest,DataDrivenProgressTest,DataDrivenQuestTableGateTest,DataDrivenEnterAreaPortGateTest,DataDrivenNativeRuntimeGateTest,MultiCellSensoryZoneRegistrationTest,RetailEnterAreaZoneRegistrationGateTest' -DfailIfNoTests=false
# → 180/180 绿（上一批 170 + 新门 9 + 算术守卫用例 1）
# 聚焦套件
mvn -o test '-Dtest=*Quest*Test,*Retail*Test' -DfailIfNoTests=false
# → 1703 / 161F+137E / 105 红类（对 P7 步 2 步 c 基线 ADDED 0 / REMOVED 0 / changed 0；零行为变更）
```

日志：`gates/2026-10-02-p7-step2d-family-tablane.log`、`gates/2026-10-02-focused-run-p7-step2d{,-red-classes,-delta}.tsv`。

## 7. 反漂移规则（本批新增）

1. **兴趣面必须逐类闭合**：已路由行的每一步都必须落进对应兴趣面（击杀/对话/FOBJ/进区/进世界/PvP 行集），
   任何一步缺失即整行冻结——不得「装载了但没人派发」。
2. **整行原子**：路由判定按**行**而不是按步；半行可路由在切换批里等于死边。
3. **自增有守卫**：`counter < target` 才允许写 vars；满组槽超杀零写（不得进位污染邻组）。
4. **未解析名不得换算**：`Ab1_xxx_Boss_Dr_Q1737` 这类名字不许用「去后缀/去前缀」规则猜；副本全图击杀名
   必须走真端同轴的「世界全图击杀」规则（步 d2 单独坐实）。
5. **生产零路由**：`DataDrivenNativeRuntime.instance()` 的路由集在步 f 原子切换前恒为空（门内断言）。

## 8. 后续（P7 步 2 剩余）

- 步 d2：CollectItem + ItemPlay 事件源接线，48 个未解析名的逐类裁定（副本全图击杀名 / quest_ai_name 组名 /
  等级阵营后缀），以及**距离闸门取值来源**（真端 `def+0x1c` int 索引 → 2500/10000/40000 + 同图/同盟/战场组；
  未坐实前不实现，登记 §10.3-#24）。
- 步 e：接取轴 6 类（含进区接取 15 别名与 DF6 跨世界冻结面）+ 通用附加动作执行面（列 1..10）。
- 步 f：1467 行原子切换 + 同批删 `RetailDataDriven*Compiler` / `RetailEnterAreaZoneResolution` /
  `RetailQuestAiNameGroups` / typed `RetailClientAcceptEntryPage` / 旧壳区名 + `quest_enterarea_zone_resolution.tsv`。
