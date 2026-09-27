# 2026-09-26 续片 23 / P0c-49：TalkFOBJ/hunt 链词汇 —— 18931/28931 采纳退役

## 1. 缺口形状

`RETAIL_STEP_UNSUPPORTED` 桶（8 → 4 行后）里「TalkFOBJ 步 + 首领击杀块」两行（光暗镜像）：

| 行 | 真端 DD 步 | 客户端任务书行 | 关键载荷 |
|---|---|---|---|
| 18931（光） | `talkfobj(IDdreadgion_04_DrakanWeapon_Q18931, 1)` → `hunt(IDDreadgion_04_DrakanFi_Noble_71_Ae 2)` | 3 行（FOBJ / 击杀 / 报告） | 接取 `IDDreadgion_04_Nizna_E`、交付 `LF6_Herodion_E`、work item `quest_18931a`（=182213555） |
| 28931（暗） | 同上（共用 FOBJ 模板） | 3 行 | 接取 `IDDreadgion_04_Lenanti_E`、交付 `DF6_Olivia_E`、work item `quest_28931a`（=182213556 补给品） |

**路由缺口**：混合链路由表里没有任何谓词覆盖「无 talk、无 collectitem 的 talkfobj+hunt」——`isHuntCollectMix` 要求 collectitem，`isTalkHuntMix` 要求 talk。

## 2. 实现（一行谓词 + 白名单）

- `RetailDataDrivenDefinitionCompiler`：新增 `isTalkFobjHuntMix(entry)`（talkfobj ∧ hunt，骑行类别 talk/collectitem/enterarea/enterworld/itemplay 放行）并 OR 入混合链路由条件。混合链展开循环与发射分支**早已支持** talkfobj 步（续片 13 的 15601 词汇），故本片只补路由判据。
- `RetailDataDrivenGateTest`：采纳白名单新增 `talkFobjHuntScope`（talkfobj ∧ hunt + 域内类别集）。
- **无新发射边、无布局变更**：真端 IR 的形状由既有链合成器给出。

## 3. 真端 IR（编译 dump 真值）

- 布局：`var0` bits0..5（行梯）+ `var1` bits6..11（击杀计数，SECTION_1）——与 SECTION 对齐门禁一致。
- 节点：`unaccepted{var0=0}` / `started{var0=0}` / `s1{var0=1}` / `reward{var0=2, var1=2}` / `complete{var0=0,var1=0}`。
- 边：
  - `started --TalkToNpc(703344, USE_OBJECT)--> s1`：`SetVariable(var0,1)` + `PACKET_ONLY`（FOBJ 交互步，与遗留 `TALK_TO_NPC action=USE_OBJECT` 同形；`USE_OBJECT.id()=-1`）。
  - `s1 --KillNpc(243797)--> s1`（priority 1）：`VariableBelow(var1,1)` + `IncrementVariable(var1,1)` + `PACKET_ONLY`。
  - `s1 --KillNpc(243797)--> reward`（priority 0）：`VariableAtLeast(var1,1)` + `SetVariable(var0,2)` + `PACKET_ONLY`。
  - 领奖：`reward --TalkToNpc(806260/806258, USE_OBJECT | SELECT_QUEST_REWARD | SELECT_QUEST(31))--> reward` 开窗 5 / 报告页 10002；完成分流 `reward --TalkToNpc(npc, 8..23)--> complete` + 无主键 `QuestDialog[108]`，afterCommit `[RefreshPlayerStats, SyncQuestState(COMPLETION), ShowQuestSelectionDialog(SELECT_QUEST)]`（与遗留断言逐字一致，动作 id 通道 = 真端奖励窗族）。
  - 通用自愈边 `EnterWorld + [StatusIs(REWARD), var0==0] → reward`（链路径合成，P0c-26/28 判例）。

## 4. 与遗留 XML 的差异（真端优先，逐条登记）

1. **FOBJ 步语义**：遗留 = `get-item(182213556)` 推进 + 掉落声明 `<drop npc-id=703344 item-id=182213556 collecting-step=0>`；真端 = FOBJ 交互边推进，凭证（work item）**接取即发**、完成/放弃清理归 planner（P0c-21 判例）。⇒ 真端元数据**无 drop 声明**，`QuestInteractionObjectValidator` 的 ACTION_ITEM_USE 合同不适用（启动门禁实测绿）；遗留 can-act 自环保留 XML 期形（与 10011 判例同源）。
2. **末段击杀落领奖 after-commit** = `PACKET_ONLY`（hunt 族链内计数惯例）；遗留手写 `LEVEL_AND_VISIBILITY_REFRESH`。
3. **计数不重置**：领奖投影携带满击杀数（QE-051 ⇒ `var1=2`），与遗留 deployed 值 `reward{var0=2,var1=2}` **恰好一致**（遗留里完成边写 2 是 deployed 语义，真端由投影表达）。
4. **紧凑存档**：遗留布局 `var0` width 2 + `var1@6`；真端布局 `var0` width 6 + `var1@6`。旧紧凑打包存档（计数压位 2）在真端布局下是 `var0=4` = 无匹配行 ⇒ **不再归一**（登记为历史遗留；`ClientQuestSectionAlignmentTest` 的机制用例改为真端形「字段外残 bit 在重打包时丢弃」）。

## 5. flip 前双侧客户端契约对拍（P0c-39 判据）

探针 `P0c49PreflipContractProbeTest`（已删源 + `.class`）：

| 侧 | 状态分布 | 致命 |
|---|---|---|
| XML（现生产视图） | `PAGE_ACTION_MATCHED=19, TERMINAL_PAGE_REACHED=10, CLIENT_PAGE_UNREACHED=4` | **0** |
| 真端（本批 2 行合成目录） | `PAGE_ACTION_MATCHED=10, TERMINAL_PAGE_REACHED=4` | **0** |

真端侧 **0 致命且 0 未达**（XML 侧的 4 条未达里 18931/28931 各占 1 条 = 报告页 10002/动作 1009，真端 IR 的 `reward` 节点 `dialogId=31 → ShowQuestDialog(10002)` 修好了它）⇒ 满足准入判据（真端致命 ≤ XML 致命）。

## 6. 收口

- `p0c49_retire_talkfobj_hunt_rows.py` 翻转 2 行（清单五副本 + 目录×2 删 2 条 + XML×2 各删 1）→ `verify_retirement.py` = `catalog=1335 directory=1335 retired=4889 sum=6224 OK`。
- 翻转后 fp dump 手术插入 2 行（`p0c49_insert_fp_rows.py`：既有行改值 0、车道 5 行 FOREIGN 保持基线、整块升序、补丁后 == dump）；冻结指纹 1146 → **1148** 行，双副本 md5 `2f22775b…`。
- 漂移登记 2 行改 `ADOPTED`（`p0c49_update_drift_rows.py`，三副本一致，md5 `f7b1cc95…`）。
- **DD 门（翻转后）6 例 2 红，全车道既有**：指纹失同步 = 车道 5 行（15042/16821/16823/26821/26823）；漂移登记失同步 = 车道 `20035`。
- **T2（2 id）72 例 3 红，全既有/车道**：`RetailSimpleCollectItemGateTest`（冻结 155 vs 退役 175 车道债）、`RetailDataDrivenGateTest`×2（上）。本批 2 行零命中。
- **契约/启动/清单/归属/非 IR/凭证六门全绿**（`QuestClientContractGateTest` + `QuestProductionStartupGateTest` + `QuestDefinitionCatalogManifestTest` + `RetailOwnershipGateTest` + `RetailNonIrAxisGateTest` + `QuestItemPlayGrantGateTest`，`exit=0`）——翻转后契约门复跑（P0c-39 纪律）无新增致命指纹；启动门含交互物 validator 合同。
- **判官 reshape 2 处（先翻转实跑取证）**：
  1. `Quest28931ClientDialogAlignmentTest`：加载器改生产视图（退役行 XML 只存 git 历史）；按真端形改写断言——节点投影（`s1{var0=1}`、`reward{var0=2,var1=2}`）+ 剩余条件（计数门）、FOBJ 推进边、完成边 after-commit `PACKET_ONLY`、领奖预览零动作（扣物归 planner）、完成 afterCommit 逐字保留；新增 work item 断言（`quest_28931a`=182213556）。2/2 绿（含生产 journey）。
  2. `ClientQuestSectionAlignmentTest.nextKillNormalizesADeployedCompactCounter` → `nextKillNormalizesBitsOutsideTheSectionFields`：真端布局下「紧凑打包位」不再存在，机制用例改为「落在所有字段之外的位在重打包时被丢弃」（位 12 表达），并断言段计数正确落位。
- **顺带偿还（既有红，非本片引入）**：`ClientQuestSectionAlignmentTest.sectionNamedCountersStayAtTheirFixedSixBitOffset` 自 10:27 基线起红（`T3-102720.log:629`）——口径是「生产目录内的例外集」，18738/28738 按 `DD_ITEMPLAY_VARIANTS` 退役离开目录后名单未同步 ⇒ 名单 13 → **11**（移除 18738/28738），修后该类 7/7 绿。
- **T3 全量（2007 例 121F/21E/1S，`gates/T3-150834.log`）**：与续片 22 的 T3（`T3-143841.log` 2006 例 123F/21E）按类集+方法集机械对拍（`t3_failure_diff.py`）⇒ **新增失败类 0 / 新增失败方法 0**；消失 2 条全为改善（本片的 `sectionNamedCountersStayAtTheirFixedSixBitOffset` + 并行车道同时落地的契约门 `productionQuestDialogsDoNotIntroduceFatalClientContractRegressions`）。归因表 `gates/p0c49-t3-diff-vs-1438.txt`。
  - **踩坑记录**：同目录还有并行车道自己的 T3 日志（`T3-154456.log`，其契约门修复在其树内进行中）；误按其日志对拍会得出「31 类新增」的假象。**判据：以本次 `[T3] ... log=<path>` 行给出的日志为准，不要按时间戳取最新**。

## 7. 余量（STEP_UNSUPPORTED 4 行）

`25052/25070`（纯 TalkFOBJ 单步，接取=交付 NPC 或双 NPC）留待 FOBJ 物品门链轴：25052 另有 `value5=Relative DF5_E1_QuestNepilim_59_An, 1, 300`（相对刷怪声明）与 `value0` 计数 1、25070 计数 0（需先裁定计数字段语义与客户端的 `select_none_1`+`SET_SUCCEED` 词汇）。
