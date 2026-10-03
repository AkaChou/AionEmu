# P5D 步 1 报告：SimpleItemPlay 行集真实分解 + 真端节点槽形态（零行为变更）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 主题：计划 §10.3-#16① 的判据修正——原文「itemplay 37 行声明中继 / `cutsceneid1` / `item_check`」与真端表
> 事实不符。本步按真端 `ScriptDLL64.c` **逐行对拍节点与槽**，得到真实分解与族不变量，并把结论冻结成门。
> 日期：2026-10-01。分支：`quest`。前置：`p5/P5-REPORT.zh-CN.md`、`p5b/P5B-REPORT.zh-CN.md`、
> `p3/P3-STEP3-REPORT.zh-CN.md`（talk 族中继/步物品/交付门形态）。
> 批门：`ItemPlayFamilyRowInventoryGateTest` 3/3；族门 + tablelane **132/132**（P5C 基线 129 → +3 例）。
> 本批**零行为变更**（不接线、不改路由、不改页流）。

---

## 1. 真端证据（原码逐行坐实）

### 1.1 注册形态

`<真端根>/server58/MainServer_ScriptDLL64/ScriptDLL64.c`：

| 形态 | 证据 | 含义 |
|---|---|---|
| `FUN_180cb5920(&DAT_node, L"<NPC 名>", <questId>)` | 例 `:1397261`（Inggril / 0x4725）、`:1397297`（Junos / 0x4725）、`:1397813`（Romedon / 0x4725） | **每个 NPC 一个节点**，同一 quest 可注册多个 NPC 节点 |
| `FUN_180cb3070(&DAT_slot, &DAT_node, <questId>, <slot>, <index>, 0)` | 例 18213：`:1402427`（Junos, slot 0）、`:1401657`（Inggril, slot 3, #0）、`:1402130`（Romedon, slot 3, #1）、`:1402295`（Romedon, slot 3, #3）、`:1403153`（Romedon, slot 4） | 节点槽注册：`slot` 为字面量，`index` 为该槽内的步号 |
| `FUN_180cb2eb0(&DAT_x, <questId>, 5, <hash>, <thunk>)` | 例 18213 `:1399108`、9623 `:1399339`、13704 `:1399086`、13400 `:1399064` | 行主注册；第 3 实参 `5` = 本族（ItemPlay）种类 id |

### 1.2 族不变量（43 行逐行复算）

| 槽 | 绑定列 | 判据 |
|---|---|---|
| `slot 0` | `acquired_npc_name` | 接取节点 |
| `slot 3 #K`（K = 0..relayCount-1） | `talk_npc(K+1)` | 第 K 中继步（**严格表序**） |
| `slot 3 #min(relayCount+1, 3)` | `reward_npc_name` | 交付步 |
| `slot 4` | `reward_npc_name` | 交付节点 |

复算结果：**43 行中 39 行与该不变量完全一致，0 例外**；偏离的 4 行见 §2.2。

### 1.3 与 talk 族同形（跨族对拍，非类比）

同一探针对 talk 族抽样复算（1468 / 1471）得到同一形态：1471 = `Likasas#0 → Shugo_c16#1 → Shugo_LC1_26#2`，
交付步 `#3`，`slot 0` = 接取 NPC（Dionera）、`slot 4` = 交付 NPC（Likasas）⇒ 「slot 3 = 步号」是**家族共享**
约定（talk 族的中继/步物品/交付门实现见 `SimpleTalkHandler`：`relaysByNpcId` / `relayCount` /
`stepGive|stepRemoveItem` / `RELAY_STEP_PAGES = {1352, 1693, 2034}`）。

### 1.4 相机（步进边）= 用道具推进的闸门（**步 2 的前置证据，本步闭合**）

每行的**主注册 thunk**（`FUN_180cb2eb0(..., <questId>, 5, <hash>, <thunk>)` 指向的函数体）就是该行的相机：

```c
void FUN_180dbb1d0(longlong *param_1)          /* quest 0x4725 = 18213 */
{
  (**(code **)(*param_1 + 0xd0))(param_1,&local_res8,0x4725);      /* 取 (状态, 步) */
  if ((local_res8 == '\x03') && (local_res9 == 2)) {               /* 闸门：状态 3 且 step == 2 */
    (**(code **)(*param_1 + 0xf0))(param_1,0x4725,3,0);            /* 推进：step = 3 */
  }
  (**(code **)(*param_1 + 0x110))(param_1,0x4725,2,3,0,1,0,0,0,0); /* 注册步进边 2→3 */
  return;
}
```

43 行全量复算（工具 `itemplay-shape-audit.py` 的 `camera-guard`/`camera-target` 列）：**41 行满足
`闸门步 = relayCount`、`目标步 = relayCount + 1`**，唯一无相机的两行正是 80255/80256
（⇒ 其「advance 轴在真端不存在」的裁定再次被独立坐实）。例：13704（0 中继）= 0→1、13400（1 中继）= 1→2、
18213/9623/13054（2 中继）= 2→3。

**⇒ 结论：本族「用道具推进」不是无条件推进，而是闸门 `状态 == 3 ∧ step == relayCount` 的一步推进。**
中继谈话负责把 step 从 0 推到 `relayCount`（逐节点、严格表序），道具推进负责最后一步
（`relayCount → relayCount + 1`），交付节点在 step == `relayCount + 1` 时激活（= `slot 3 #K` 的 K）。
这同时解释了为什么 9623 等行**没有接取发放**（`give_item` 空）：道具来源可以在族外，但**步数前置**仍
卡死「绕过中继直接用道具跳 REWARD」。

---

## 2. 行集真实分解（§10.3-#16① 判据修正）

### 2.1 43 行的真实构成

| 桶 | 行数 | 判据（可复算） |
|---|---|---|
| **已路由**（owner `RETAIL_TABLE`） | **6** | 13704/13708/19048/23704/23708/29048；XML 已删、handler `routes` = true |
| **XML 保留**（owner `XML_RETENTION`，各有 `ADJUDICATED:*` 理由） | **9** | 18213/28213/50048（talk chain）、18828/28828（con_quest）、39713/49713（接取哨兵）、80255/80256（advance 未表达） |
| **不在本服生产**（清单无行、仓内无 XML） | **28** | 41267/41514/41540/41300/41577/41593/12066/22066/50013/50014/51013/51014/18014/28014/12525/12562/22525/22562/13064/23064/13401/23401/9623/13054/23054/23562/13400/23400 |

即：**37 不路由 ≠ 37 行声明长尾列**。真端表里声明中继/步物品/过场/交付门的行只有 11 行
（9623/18213/28213/39713/49713/13054/23054/23562/13400/23400/50048）；其中 28 行的主体是
**本服从未上线**（无 XML、无 owner 条目），与「长尾未接线」是两类问题。

### 2.2 4 个形态偏离行 = 已裁定理由的独立互证

| 行 | 真端形态实测 | 裁定理由 | 结论 |
|---|---|---|---|
| 80255 / 80256 | 交付步注册在 `slot 3 #0`（其余行是 `#1`）⇒ 原地推进、无步链 | `ADJUDICATED:RETAIL_ADVANCE_UNEXPRESSED` | 真端确认「推进轴不可表达」（本族推进 = 用道具） |
| 39713 / 49713 | `slot 0` **缺失**（接取名是 `_faction_` 哨兵，无节点）⇒ 无接取面 | `ADJUDICATED:RETAIL_ACQUIRE_NPC_SENTINEL` | 真端确认「接取哨兵无解」 |

⇒ 4 行的 XML 保留裁定**由真端形态独立坐实**（不是实现能力问题）；其余 **5 行**（18213/28213/18828/28828/50048）
形态已成立、只差接线面 ⇒ 下一增量目标集。

---

## 3. 落地

| # | 交付 | 说明 |
|---|---|---|
| 1 | `p5d/tools/itemplay-node-shape-probe.py` | 按 quest id 列出真端节点注册、`slot/index` 注册点与行主注册（kind/thunk） |
| 2 | `p5d/tools/itemplay-shape-audit.py` | 43 行全量复算：行集分桶 + 不变量判定 + 逐行证据导出 |
| 3 | `p5d/itemplay-shape.tsv` | 43 行证据表（owner / 理由 / XML 在仓 / 节点 / slot0 / slot3 步形 / 期望步形 / 不变量） |
| 4 | `ItemPlayFamilyRowInventoryGateTest`（新增，3 例） | ① 行集分解冻结（43 = 6 + 9 + 28，互斥且覆盖；装载/注册/路由/不可路由四个集合一致）；② 裁定理由 ↔ 真端槽形态互证（偏离集 4 行、形态就绪集 5 行冻结）；③ 证据面不回退（未解析名不扩面、con_quest 闭环不得退化） |

**未改动**：`SimpleItemPlayHandler` 的路由/页流/道具面、retention 清单、XML 文件（本步零行为变更）。

---

## 4. 门态（可复跑）

| 门 | 命令 | 结果 |
|---|---|---|
| 新行集门 | `mvn -o test -Dtest='ItemPlayFamilyRowInventoryGateTest'` | **3/3** |
| 族门 + tablelane | `mvn -o test '-Dtest=*FamilyGateTest,*RowAlignmentGateTest,*InventoryGateTest,NativeQuestTableLoaderTest,*RewardClaimGateTest,*NativeTalk*Test,NativeNearbyQuestAxisGateTest,SMNearbyQuestsPacketTest,NativeAcceptEntryAskFlowGateTest'` | **132/132**（P5C 基线 129 → +3 例） |
| 聚焦套件 | `mvn -o test '-Dtest=*Quest*Test,*Retail*Test'` | **1699 例 / 162F+142E / 108 类红**，对 P5C 基线 **ADDED 0 / REMOVED 0** |

证据：`gates/2026-10-01-p5d-family-tablane.log`（`*.log` 被 `.gitignore` 忽略 ⇒ 未入仓）、
`gates/2026-10-01-focused-run-p5d-red-classes.tsv`、`gates/2026-10-01-focused-run-p5d-delta.tsv`（入仓）。

---

## 5. 残余与下一步（P5D 步 2）

1. **步 2 可直接动工（前置证据已在本步闭合）**：按 §1.4 的闸门证据接线——
   * 接取（`acquired_npc_name`，26/31 → 1002/20000）⇒ step = 0，发放 `give_item`（若有）；
   * 中继 `talk_npcK`（严格表序，节点 `slot 3 #K-1`）⇒ step K-1 → K，并执行第 K 步的 `give_itemK`/`remove_itemK`；
   * 用道具（真端相机）⇒ **闸门 step == relayCount** 时才推进到 `relayCount + 1`（新实现必须带这个门，
     不得沿用现有 `SimpleItemPlayHandler.onItemUse` 的无条件推进）；
   * 交付节点（`slot 3 #min(relayCount+1,3)` / `slot 4`）⇒ step == relayCount+1 时开奖励窗；
     `item_check = true` 的行另需交付门；旧存档自愈随批。
   * 目标集 = 形态与闸门都已成立的行（XML 保留 5 行 + 表中其余同形行，共 39 行族内形状一致行；
     路由上线仍按 owner 裁定逐行决定）。
2. **保持冻结**：80255/80256（真端无相机 ⇒ advance 轴不存在）、39713/49713（接取哨兵 `_faction_` 无
   `slot 0` 节点）、28 行不在生产——三者都不得靠合成页/补名/无条件推进上线。
3. 客户端验收 `PENDING_CLIENT`（随步 2 一并跑）。
