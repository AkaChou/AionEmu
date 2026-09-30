# 2026-09-26 续片 25 / P0c-51：FOBJ 采集行 —— 25052 采纳退役（掉落驱动 FOBJ 形）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

## 1. 缺口与判据

`RETAIL_FOBJ_COLLECT_UNSUPPORTED` 桶（P0c-50 设立）最后一行 **25052**（"An Offering of Peace"）：

- 真端表：`acquire=Talk DF5_Redelf_E`（804913）、`reward=DF5_Soglo_E`（804915）、**单步 `TalkFOBJ
  DF5_FOBJ_giantcrystal_Q25052`**（731561，`quest_use_item_npcs.tsv` 在册 ⇒ `ai=quest_use_item`）。
- `quest.xml`：`collect_item1 = quest_25052a 5`（182215721×5）、`drop_monster_1 = 该 FOBJ`、
  `drop_item_1 = quest_25052a`、`drop_prob_1=100`、`drop_each_member_1=1`、`collect_progress=0`。
- 客户端任务书 `quest_q25052.html`：`select_none`(4762) 接受/拒绝；`select1`(1011) = 水晶页（按钮
  10255 `SET_SUCCEED` 抽取 / 1008 结束对话）；`select_success`(10002) 唯一按钮 1009
  `SELECT_QUEST_REWARD`；`select_quest_reward1`(5) 领奖窗；`quest_summary` **两行**（行 0 = 从水晶提取、
  行 1 = 交给 Soglo）⇒ 领奖投影 `var0=1`。

**判据（本片主产出）**：同一「单步 TalkFOBJ + 物品要求」再分两形——`check_item*`（工作物证明）走
P0c-50 的**交互推进**形（25070）；`collect_item*`（采集交付）则是**掉落驱动**形：FOBJ 是该采集物的
**掉落源**，交互抽取不推进行态，交付门挂在领奖 NPC 的 `SELECT_QUEST_REWARD(1009)` 上。判据源 =
真端元数据（`itemRequirements` 非空且 `inventoryItems` 为空）——与 P0c-50「元数据即形状切分器」同轴。

## 2. 实现

- `RetailDataDrivenTalkHuntChainCompiler`：
  - 新谓词 `isDropDrivenFobjCollect(entry, metadata)`（步类别恰为 `[talkfobj]` ∧ `itemRequirements`
    非空 ∧ `inventoryItems` 空）；
  - FOBJ 步在该形下**不发推进边**（掉落源自环循环已按 `drop.collectingStep` 发出 `TalkToNpc` 通配 +
    `CanAct(ACTION_ITEM_USE)` 自环对，满足 `QuestInteractionObjectValidator` 的 START 态合同）；
  - 新 `fobjCollectHandIn(metadata, rewardNpc)`：`QUEST_SELECT → 10002` 报告页自环 +
    `SELECT_QUEST_REWARD(1009)` 组检查对（有货：整组 `HasItem 5` → 扣 5 + `LEVEL_AND_VISIBILITY_REFRESH`
    + 开领奖窗 5；缺货：priority 1 关窗——客户端无 `check_user_item_fail` 页）；
  - `build(...)` 增一个 `dropDrivenFobj` 形参（判据只在 `compile` 侧算一次，避免两处判据漂移）。
- `RetailDataDrivenDefinitionCompiler`：路由侧守卫改为「纯 FOBJ 单步行 ∧ **非**掉落驱动采集形 ∧
  （itemRequirements/inventoryItems/drops 任一非空）⇒ `RETAIL_FOBJ_COLLECT_UNSUPPORTED`」——
  两通道并存（itemRequirements + inventoryItems）等未证组合仍 fail-closed。

## 3. 真端 IR（编译 dump 真值，25052）

- 布局：单 `var0` bits0..5；节点 `unaccepted{0}/started{0}/reward{1}/complete{0}`。
- 掉落源（731561）：`started --TalkToNpc(731561, 通配)--> started` + `started --CanAct(731561,
  ACTION_ITEM_USE)--> started`（无动作、无 after-commit——抽取物由掉落表按 `collect_progress=0` 发放）。
- 交付（804915）：`started --TalkToNpc(804915, 31)--> started [ShowQuestDialog(10002)]`；
  `started --TalkToNpc(804915, 1009) [HasItem(182215721,5)] --> reward`（`RemoveItem ×5` +
  `LEVEL_AND_VISIBILITY_REFRESH` + 开窗 5，priority 0）；同事件 priority 1 → `started` + `CloseDialog`。
- 接取（804913）：`QUEST_SELECT → 4762`（入口页登记）＋ 20000/20001 接受对 ＋ 1002/1003/1004 确认窗 +
  1007/1008/31 既有词汇（与 25070 同形）。
- 领奖：`reward --QuestDialog[110..113]--> complete`（4 个 selectable 的窗口确认通道）＋
  `reward --TalkToNpc(804915, 8..23)` 选择梯（含 `QuestDialog[10]` 与 `TalkToNpc[108]` 收窗）＋
  `reward --TalkToNpc(804915, -1|1009)--> reward [Show 5]` 预览；自愈边 `null --EnterWorld--> reward`
  （`StatusIs(REWARD)+var0==0 → var0=1`，QE-051 领奖行）。

## 4. 与遗留 XML 的差异（真端优先，登记）

1. **交付 NPC 收敛为一处**：遗留对 804913（接取）/804915/731561（掉落源）各铺一整套交付流；真端权威 =
   交付在 `reward_npc_name`(804915)，客户端任务书 step1 只点名 `STR_DIC_N_DF5_Soglo_E` ⇒ 另两处属
   手工转写迁移（P0c-19/20「页面级证据 ≠ 路由级契约」判例同源）。
2. **失败分支改关窗**：遗留 `CHECK_USER_ITEM_FAIL` 页在客户端任务书里不存在（唯一交付页
   `select_success` 只有 1009 一个按钮）⇒ 缺货分支只关窗。
3. **抽取语义**：遗留 `can-act(731561, ACTION_ITEM_USE)` 自环保留；道具发放由掉落表（`drop_monster_1`
   指向该 FOBJ + `collect_progress=0`）承担，`itemRequirements` 只在交付边作为整组门禁。
4. **页 1011 两侧同病**：水晶页由客户端按对象交互自绘、服务端从不下发 ⇒ flip 前后同为
   `CLIENT_PAGE_UNREACHED`（非致命，非本片引入）。

## 5. flip 前双侧契约对拍（P0c-39 判据，可复算件）

探针 `P0c51FobjCollectProbeTest`（用后删源 + `.class`）；flip 前的 XML 侧由
`git show HEAD:…/quests/25052.xml` 落盘到 `gates/p0c51-25052-preflip.xml` 后编译审计：

| 侧 | 状态分布 | 致命 |
|---|---|---|
| PREFLIP（真前 XML，25052 单行） | `PAGE_ACTION_MATCHED=8, TERMINAL_PAGE_REACHED=6, CLIENT_PAGE_UNREACHED=1` | **0** |
| RET（真端形，25052+25070 两行） | `PAGE_ACTION_MATCHED=8, TERMINAL_PAGE_REACHED=5, CLIENT_PAGE_UNREACHED=2` | **0** |

客户端按钮覆盖数相同（8），两侧未达页同为 **1011**（已登记独立轴）⇒ 真端侧不劣，准予采纳。

## 6. 收口

- 分类 diff（防掩盖，`-Dretail.dataDriven.equivOut` 实测 dump 对基线漂移登记表）：1508 行全等，
  变化 23 行 = **本片 25052（`REJECTED:RETAIL_FOBJ_COLLECT_UNSUPPORTED` → `ADOPTED`）+ 车道在飞的
  22 行**（talk/collect/hunt/itemplay 链，与 DD 门既有 2 条车道红同源）；机械核验 = 23 行里
  `stepCategories == [TalkFOBJ]` 的**恰好只有 25052** ⇒ 本片改动面 = 1 行。
- `p0c51_retire_fobj_collect_row.py` 翻转 25052（清单五副本 + 目录×2 删 1 + XML×2 各删 1）
  → `verify_retirement` = `1333/1333/4891/6224 OK`。
- fp 手术插入 1 行（既有行改值 0、车道 5 行 FOREIGN 保持基线）1149 → **1150** 行，双副本 md5
  `a9220f8d2f2b171f7ce8dc2cb17ca546`；漂移登记 25052 → `ADOPTED`（`b9ec6f9ca804dd6ba049e276443e9210`）。
- **门禁**：
  - T1 63 例 **3F**（全部既有/车道：DD 漂移 `20035`、DD 指纹 15042 等 5 行、采集族 155 vs 175）；
    契约门 `QuestClientContractGateTest` 1/1 绿（新形状零致命）；
  - T2（25052）69 例 3F/0E：上述三条；启动门 2/2、交互物合同门 2/2 绿（新自环对满足 validator）；
    目录/归属/白名单/非 IR/凭证全绿；
  - 详见 §7 的 T3 全量对拍。
- 报告本文件；证据 `p0c51-fobj-collect-decisions.tsv`；机械 `p0c51_retire_fobj_collect_row.py`、
  `p0c51_insert_fp_row.py`、`p0c51_update_drift_row.py`；探针 `P0c51FobjCollectProbeTest.java.txt`；
  基线件 `gates/p0c51-25052-preflip.xml`、`gates/p0c51-drift-post.tsv`、`gates/p0c51-fp-post.tsv`。
- **T3 全量对拍（2006 例 121F/21E/1S，`gates/T3-161145.log`）**——与续片 24 的 T3
  （2007 例 122F/21E/1S，`gates/T3-153216.log`）按类集 + 方法集机械对拍（左 79 类/110 方法 →
  右 78 类/110 方法）：**新增失败类 0 / 新增失败方法 0**；消失 1 条 =
  `RetailSimpleTalkChainGateTest.adoptRowsReplayRegistryAndMatchFrozenFingerprints`——即续片 24 归因的
  车道指纹读序竞态，本轮车道已自愈（QE-069 判例的闭环验证）。用例总数 2006 与执行类集均为 484：
  两轮差异恰为「车道删 `P0c42AcceptPageNotEmittedProbeTest`（2 例）／本片增探针（1 例）」
  ⇒ 计数差 −1 完全闭合。本片 id（25052）在全部失败消息**零命中**；探针用后按纪律删源 + `.class`
  （`P0c51FobjCollectProbeTest.java.txt` 留档）。

## 7. 余量

`RETAIL_FOBJ_COLLECT_UNSUPPORTED` 桶清零（累计 1 行 → 0；该码保留为未证组合的 fail-closed 守卫）。
`RETAIL_STEP_UNSUPPORTED` 与 FOBJ 轴已无余量；下一面 = 真端数据缺口桶
（`ACQUIRE_NPC_UNRESOLVED` 62 / `MONSTER_UNRESOLVED` 56 / `SENTINEL_AREA_PENDING` 64）。
