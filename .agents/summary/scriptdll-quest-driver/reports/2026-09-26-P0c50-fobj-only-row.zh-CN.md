# 2026-09-26 续片 24 / P0c-50：纯 TalkFOBJ 单步行 —— 25070 采纳退役（25052 码位前移）

## 1. 缺口与分形判据

`RETAIL_STEP_UNSUPPORTED` 桶最后两行都是**单步 TalkFOBJ**（无 talk/hunt/collect），但零售 `quest.xml`
元数据把它们分成两形——这是本片的核心裁据：

| 行 | 元数据声明 | 语义 | 处置 |
|---|---|---|---|
| **25070**（暗，"Truth of the Crystal"） | `quest_work_item1 = quest_25070a`、`check_item1_1 = quest_25070a 1`（**无 collect_item / 无 drop**） | 与冰晶 731552 交互即入领奖；凭证接取即发 | **采纳退役** |
| **25052**（暗，"An Offering of Peace"） | `collect_item1 = quest_25052a 5`、`drop_monster_1 = DF5_FOBJ_giantcrystal_Q25052`、`drop_item_1 = quest_25052a`、`drop_prob_1=100` | 采集 5 件（掉落源 = FOBJ 巨人水晶）→ 交 804913，交付/完成边都带 `has-item 5` | **拒绝（码位前移）** |

链内 FOBJ 步**不带物品门**，放行 25052 会静默丢掉「交 5 件」合同（假采纳）⇒ 机器判据 =
`itemRequirements`（`collect_item*` 的落点）／`inventoryItems`／`drops` 任一非空即拒绝。

## 2. 实现

- `RetailDataDrivenDefinitionCompiler`：
  - 新谓词 `isTalkFobjOnly(entry)`（步类别恰为 `[talkfobj]`）入混合链路由；
  - 路由侧守卫：纯 FOBJ 单步行若 `itemRequirements` / `inventoryItems` / `drops` 非空 ⇒
    `RETAIL_FOBJ_COLLECT_UNSUPPORTED`（带 detail 打印三列，便于下一片直接取证）。
- `RetailDataDrivenGateTest`：采纳白名单新增 `talkFobjOnlyScope`。
- 无新发射边：链内 talkfobj 分支（续片 13/23 词汇）已给出本行形状。

## 3. 真端 IR（编译 dump 真值，25070）

- 布局：单 `var0` bits0..5。
- 节点：`unaccepted{var0=0}` / `started{var0=0}` / `reward{var0=1}` / `complete{var0=0}`。
- 边：
  - `unaccepted --TalkToNpc(804919, 1002|20000)--> started`（接取，`StartEligible`）；
  - `started --TalkToNpc(731552, USE_OBJECT=-1)--> reward`：`SetVariable(var0,1)` + `PACKET_ONLY`（FOBJ 交互推进）；
  - 领奖窗：`reward --TalkToNpc(804919, -1 | 1009)--> reward` 开窗 5；`dialogId=31 → 10002` 报告页；完成分流 `8..23` + `QuestDialog[108]`（afterCommit `[RefreshPlayerStats, COMPLETION, ShowQuestSelectionDialog(10)]`）；
  - 自愈边：`null --EnterWorld--> reward` `[StatusIs(REWARD), var0==0]` → `var0=1` + `LEVEL_AND_VISIBILITY_REFRESH`（与镜像 15070 的 QE-051 领奖行合同逐字同形）。

## 4. 与遗留 XML 的差异（真端优先，登记）

1. **凭证发放**：遗留 `started --TalkToNpc(731552, USE_OBJECT)--> reward` 自带 `give-item 182215723×1`；
   真端按凭证模型**接取即发**、planner 回收（P0c-21/续片 23 判例）。
2. **after-commit**：遗留 `LEVEL_AND_VISIBILITY_REFRESH + CloseDialog`；真端链内 FOBJ 边 `PACKET_ONLY`
   （交互/EA/EW 驱动的领奖入口族惯例；观察项：客户端领奖标记不刷新时先查此处）。
3. **can-act 自环**：遗留有 `can-act(731552, ACTION_ITEM_USE)`，真端无——25070 的零售元数据**无 drop**
   ⇒ `QuestInteractionObjectValidator` 不要求该门（10011/1141 判例同源）；启动门实测绿。
4. `check_item1_1 = quest_25070a 1` 是客户端**目标行标记**（本行客户端任务书 2 行：FOBJ / 领奖），
   遗留同样没有服务端 `has-item` 门 ⇒ 真端「凭证在包 + 交互推进」等价。

## 5. flip 前双侧契约对拍（P0c-39 判据）

探针 `P0c50PreflipContractProbeTest`（用后删源 + `.class`）：

| 侧 | 状态分布 | 致命 |
|---|---|---|
| XML | `PAGE_ACTION_MATCHED=11, TERMINAL_PAGE_REACHED=8, CLIENT_PAGE_UNREACHED=2` | **0** |
| 真端 | `PAGE_ACTION_MATCHED=3, TERMINAL_PAGE_REACHED=2, CLIENT_PAGE_UNREACHED=1` | **0** |

两侧未达页同为 **1011（client 1008/10255）**——即已登记的 `ACCEPT_PAGE_NOT_EMITTED` 独立轴（XML 侧同病）
⇒ 真端侧不劣，准予采纳。

## 6. 收口

- `p0c50_retire_fobj_only_rows.py` 翻转 25070（清单五副本 + 目录×2 删 1 + XML×2 各删 1）
  → `verify_retirement` = `1334/1334/4890/6224 OK`。
- fp 手术插入 1 行（既有行改值 0、车道 5 行 FOREIGN 保持基线）1148 → **1149** 行，双副本 md5 `0c7b3c87…`；
  漂移登记 25070 → `ADOPTED`。
- **25052 码位前移**（`p0c50_shift_25052_code.py`，只改该行）：漂移登记
  `REJECTED:RETAIL_STEP_UNSUPPORTED` → `REJECTED:RETAIL_FOBJ_COLLECT_UNSUPPORTED`（2 副本 + `.agents` 快照），
  保留清单 reason 列 `SEMANTIC_GAP:<码>`（**去 `REJECTED:` 前缀**的写法）同步 5 副本，md5 全一致。
- **门禁**：
  - DD 门 6 例 2 红（**全车道**：指纹 5 行 + 漂移 `20035`）；
  - T2（25070）67 例 5 红：采集族冻结 155/退役 175 + DD 门 2 红（车道）+ `AlignedMirrorRewardRowContractTest` 2 红
    ——**两条失败消息与翻转前的 T3 基线逐字相同**（`quest 11294 route…`、`quest 26820 recovery conditions`，均为车道行）；
    该类的**镜像侧只出现在「领奖投影」断言**（Test 1/4，全绿 ⇒ 25070 投影 `{var0=1}` 已验），Test 2/3 只跑 questId 侧
    （15070，未动）⇒ 25070 无未验证面（判官 abort 掩蔽判例自查）；
  - 契约/启动/清单/归属/非 IR/凭证/**交互物目录**七门全绿（启动门含交互物 validator）。
- **T3 全量对拍（2007 例 122F/21E/1S，`gates/T3-153216.log`）**——与续片 23 的 T3
  （2007 例 121F/21E/1S，`gates/T3-150834.log`）按类集 + 方法集机械对拍（`t3_failure_diff.py`，
  左 78 类/110 方法 → 右 79 类/111 方法）：**新增失败类 1 / 新增失败方法 1 / 消失 0**，增量**唯一**为车道在飞项
  `RetailSimpleTalkChainGateTest.adoptRowsReplayRegistryAndMatchFrozenFingerprints`——其冻结表
  `src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv`（车道的 wave-A 新产物，未入仓）
  于 **15:39:21** 被车道重冻，而本片 T3 于 15:32:16 启动、读到的是冻结前旧值；唯一分歧行 **1323**
  的新冻结值 `9e3f26da…` 与本片运行内算值**逐字相同** ⇒ 纯读序竞态（车道已自愈），与本片改动无因果。
  本片 5 个 id（25070/25052/28931/18931/25030）在全部失败消息**零命中**；用例总数 2007 不变
  （续片 22 的探针 `.class` 残留清理仍然有效）。
- 报告本文件；证据 `p0c50-fobj-only-decisions.tsv`；机械 `p0c50_retire_fobj_only_rows.py`、
  `p0c50_insert_fp_rows.py`、`p0c50_update_drift_rows.py`、`p0c50_shift_25052_code.py`；探针
  `P0c50FobjOnlyProbeTest.java.txt`、`P0c50PreflipContractProbeTest.java.txt`。

**判例 QE-069（并行车道 T3 归因）**：并行车道若在本次 T3 运行窗口内重冻指纹/登记表（`.tsv`、
`target/test-classes` 快照等），对拍会显示"新增红"。归因次序 = ①先看该行**冻结产物 mtime 是否晚于本次运行起点**
（本片：15:39:21 > 15:32:16）；②再取该行数值与本次运行内算值比对（相等即纯读序竞态）；③最后才与本片 id 集取交。
仅凭类名/方法名 diff 会把车道在飞项记成自身回归。

## 7. 余量

`RETAIL_STEP_UNSUPPORTED` 桶**清零**（累计 8 行 → 0）。剩 25052 = `RETAIL_FOBJ_COLLECT_UNSUPPORTED`
（须采集族 + FOBJ 掉落源 `CanAct(ACTION_ITEM_USE)` 通道，设计见
`design/2026-09-26-fobj-item-gate-axis-25052-25070.zh-CN.md`）。
