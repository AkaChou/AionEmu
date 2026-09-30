# P0c-57 接取入口角色收窄：18 个任务的每链 NPC `NPC_START` 块按真端/客户端 owner 收窄（SimpleTalk 链车道 / 续片 33）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 上游：P0c-55 在 P0c-46 角色普查的 `INFO_ACCEPT_SPREAD` 基础上登记了 `XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING`（34 项 / 25 任务，`p0c42-blocked-rows.tsv` 行 32）。本片完成复算 → 定根因 → 受守卫通道 → 落地 → 全门禁。**真端与客户端为准，遗留 XML 只作对照**（不可推翻裁定 ⑥）。

## 1. 缺口形状（块级根因）

登记口径先复算到位：从 `p0c46-role-axis-census.tsv` 的 `INFO_ACCEPT_SPREAD` 重推 = **34 项 / 25 任务，RECOMPUTED == REGISTERED**（QE-072 校准：先证明判据认得登记时刻的坏面）。

再下钻到**块级**找到了根因：遗留 XML 把真端**步骤行**（`talk_npc<k>`）误读成**起始行**，给每个链上 NPC 各写一个 `<dialog type="NPC_START">` 块；编译器 `RetailSimpleTalkDefinitionCompiler.acceptFlowChain` 对每个 `NPC_START` 块各合成一条接取流 ⇒ **链上每个 NPC 都成了接取人**。HEAD `quest_definition/quests/1484.xml` 即铁证——4 个 `NPC_START` 块，其中 3 个紧贴着遗留编译器自己的注释自认误读：

```
74: <dialog type="NPC_START" npc-id="204045" source="unaccepted" ... start-page="SELECT1"/>
75: <!-- retail 步骤0(zz_retail_simple_quests.xml):type="TALK" ids="204045" give_item_id="182201403" ...;对话链按客户端按钮图重建。 -->
107: <dialog type="NPC_START" npc-id="204048" ... />
108: <!-- retail 步骤1(...):type="TALK" ids="204048" ... -->
140: <dialog type="NPC_START" npc-id="204011" ... />
173: <dialog type="NPC_START" npc-id="798126" ... />   ← 领奖 NPC 也被铺了接取
```

真端 `Quest_SimpleTalk.xml`（data_driven 类）与客户端任务书（`legacy-quest-dialog-template-index.csv` 行 `1484,data_driven_quest,...,TALK,798127,798126,...`）双侧一致：**接取 owner 唯一 = 798127（Shugo_LF2_14）**，204045/204048/204011 是步骤 NPC、798126 是交付 NPC。

**块轴普查**（`p0c57-accept-entrance-census.tsv`，27 任务）：18 `ACCEPT_ENTRANCE_DUPLICATE`（37 块，本片裁面）+ 1 `ACCEPT_ENTRANCE_VARIANT_DIFF`（2266）+ 5 `ACCEPT_ENTRANCE_OWNER_MISSING`（3218/4218/4970/21110/26990）+ 3 `ACCEPT_ENTRANCE_DECL_UNRESOLVED`（3006/35011/35026，`_faction_`）。

**物品轴普查**（`p0c57-accept-axis-census.tsv`，34 项）：15 `SPREAD_PRUNE_CANDIDATE`（8 任务：2538/2914/3037/3041/3087/3093/11010/11103）+ 5 `SPREAD_GUARD_FAIL`（3218/4218/19070/19071/21110）+ 14 `SPREAD_OWNER_UNRESOLVED`（35010/35017/35018/35024/35025/45010/45011/45017/45018/45024/45025/45026）。**已知陷阱（已写进产物头注）**：块级 `B NPC_START` 不是接取 owner 的权威——owner 要用客户端模板索引 `start_npc_ids`；35017/45010/45017 客户端 `start_npc_ids` 为空。

## 2. 裁决口径（逐轴 fail-closed）

裁定表 `p0c57-accept-entrance-decisions.tsv`（37 行 / 18 任务，code=`LEGACY_ACCEPT_ENTRANCE_SPREAD`）与 `p0c57-accept-axis-decisions.tsv`（15 行 / 8 任务，code=`LEGACY_ACCEPT_ROLE_SPREAD`），判据：

- **G1** owner 双侧唯一且相等：真端 `acquired_npc_name` == 客户端 `start_npc_ids`（恰一员）；
- **G2** 被剪 NPC 是链上 NPC（真端 acquired∪talk∪reward ∪ 客户端 start∪progress∪end）且 ≠ owner；
- **G3** 接取投影 ⊆ owner（被剪块/行下发的接取族页 owner 侧都有）；
- **G4** owner 侧确有接取族行（剪完任务仍可接取）；
- **G5** 被剪行不含 `QUEST_SELECT` 入口行（不得剪到真接取入口）；
- **G6** 被剪行 `source == unaccepted`（只在接取相位，运行期相位不受影响）；
- **装载轴**：code 域限定；与 P0c-42 接取入口表互斥；retention == `RETAIL_TABLE`；真端 `resolve_npc`/`acquired_npc_name` 与 owner 逐字对拍；**每任务恰一 owner**（多个即 fail）。

生成器侧另有逐行守卫 `accept_pruned_route`：只剪接取族动作/页且 `source != unaccepted` 即 fail-closed 拒绝；收口守卫 `P0C57-ACCEPT-KEEP` 要求剪完后每任务恰剩一个 `NPC_START` 块（= owner）。

**客户端按钮图优先**：1484 客户端任务书只有 owner 一行在接取相位有按钮（`QUEST_SELECT`@798127），被剪 NPC 的"接取按钮"在客户端不存在——遗留扩散在客户端侧无对应物，这正是判据 ⑥ 的直接应用。

## 3. 落地

`build_quest_client_talk_chain_steps.py` 新增受守卫通道：接取族动作/页常量（`ACCEPT_ROLE_ACTIONS`/`ACCEPT_ROLE_DIALOG_ACTIONS`/`ACCEPT_ROLE_PAGE`）、裁定表装载器（逐轴复算，违轴即拒）、`accept_pruned`（NPC_START 循环跳过）与 `accept_pruned_route`（R 行过滤）、重编序含 `accept_narrowing`、逐任务打印 `P0C57-ACCEPT-NARROW %d：剪除接取入口块 %d 个 + 接取族行 %d 条…`、`P0C57-ACCEPT-KEEP` 收口守卫。登记表由生成器重生成：**5052 行 → 5000 行（净 −52 = 37 块 + 15 条 `R:unaccepted` 行），md5 `5bf47fde…` → `0ae2d924…`（415369 字节）**；保真重跑逐字节相同、24 张裁定表零漂移。

爆炸半径（seq 不敏感对拍，`p0c57-blast-radius.txt`）：**恰 18 任务，`added=0 removed=52 net=-52`**——全部落在本片裁定面。

## 4. 冻结面

- **指纹重冻**（`p0c57_refreeze_fingerprints.py`，dry-run→apply，双重校验）：dump 与冻结 285 行逐键对拍 ⇒ changed **恰 18**、added/removed 皆空、**声明面 == 实测面 == 裁定表**（三面互证）、18 行 PRE 值逐一对上；267 行逐字节不动。冻结文件 md5 `13a5c40e…`（src 与 target 双份同值）。
- **IR**（探针 dump）：353 行移除 / 0 行新增（198737 → 153820 字符），全部为被剪块的 IR 投影。
- **对话序审计**：pre/post `unreachedOrEvidence=10` 同值；diff 仅 18 条 `### quest N rows=M` 头行计数变小（被剪的都是 `PAGE_ACTION_MATCHED` 非致命行），无任何行内容变化、无新增未达页。

## 5. 门禁

| 门 | 命令/日志 | 结果 |
|---|---|---|
| 保真 | `p0c42_builder_fidelity_check.py` | `FIDELITY_OK` 逐字节相同（415369 字节）；`REGISTRY_MD5 0ae2d924…`；`DECISION_TABLES_STABLE 24 张零漂移` |
| 净树 | `p0c57-nettree-gate.log`（链门+序审计+客户端契约） | **20/20 绿**（2+17+1），客户端契约门 fatal 0（空基线保持） |
| T1 | `gates/T1-225105.log` | 75 例 1F = lane `20035` 唯一身份（与 P0c-55 `T1-211816.log` 同值；源行 423→430 为并发 lane 对该测试类自身的编辑） |
| T2（18 id） | `gates/T2-225730.log` | 97 例 7F：`20035`×1 + `QuestMultistepChainContractTest` 1514×5 + `JournalRewardRowRepairContractTest` 15613×1 —— **六条 1514/15613 身份逐字见于装机前 `T3-221706.log` 与 P0c-55 基线 `T3-221828.log`** ⇒ 车道既有，非本片 |
| T3 | `gates/T3-230841.log` | **2015 例 117F/22E/1S vs P0c-55 基线 `T3-221828.log`（2013 例同值）——class.method 身份集 105 == 105，ADDED 0 / REMOVED 0**（例数 +2 为两轮之间并入的 lane 新测试类） |

## 6. T3 归因（QE-069 次序 + 同态互证）

T3 覆盖共享树（并发 DataDriven 车道同窗在飞，其 T1/T2 在 `gates/T1-225858.log`/`T2-230615.log`）。归因三件套：① 身份集零漂移（105==105）；② 本片改面只有登记表 + 冻结指纹 + 生成器/裁定表（git 可列），且爆炸半径恰 18 任务全落靶面；③ T2 中 1514/15613 身份在装机前基线逐字存在（该两任务是**并发多步链车道**的在飞面，其 23:15 的 `T3-231545.log` 显示 1514/1183/1218 另一套在飞失败）。**本片自身归因：零新增失败。**

## 7. 残留与登记

- `p0c42-blocked-rows.tsv` 行 37：`XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING` **收口**（`ACCEPT_ENTRANCE_ROLE_NARROW_CLOSED`）：18 任务 37 块 + 8 任务 15 行采纳；**19/34 项暂缓**（12 任务 `35010…45026` 的 `_faction_` 声明未解 + 5 任务 3218/4218/19070/19071/21110 owner 缺失/变体差）；块轴另有 2266（VARIANT_DIFF）、3006/35011（DECL_UNRESOLVED）、4970/26990（OWNER_MISSING）未动。
- `ACCEPT_ENTRY_PAGE_WRONG_PENDING`（7 条）**仍开放**：其入口行 `source != unaccepted`，本通道按 G6 剪不到，另轴另判。
- 运行时/客户端目检 PENDING（未启服，需用户授权）。
- 编号备注：并发 DataDriven 车道已取 `P0c-58`（challenge 哨兵轴），本片独占 `P0c-57 / 续片 33` 前缀。

## 8. 复现命令

```bash
# 侦察/普查（产物在 .agents/summary/scriptdll-quest-driver/）
python3 -B .agents/summary/scriptdll-quest-driver/p0c57_accept_axis_recon.py
python3 -B .agents/summary/scriptdll-quest-driver/p0c57_accept_axis_census.py
python3 -B .agents/summary/scriptdll-quest-driver/p0c57_accept_entrance_census.py
# 生成 + 爆炸半径
python3 -B .agents/summary/scriptdll-quest-driver/build_quest_client_talk_chain_steps.py
python3 -B .agents/summary/scriptdll-quest-driver/p0c57_blast_radius.py
# 保真 + 指纹重冻（dry-run 后 --apply）
python3 -B .agents/summary/scriptdll-quest-driver/p0c42_builder_fidelity_check.py
python3 -B .agents/summary/scriptdll-quest-driver/p0c57_refreeze_fingerprints.py p0c57-fingerprints-dump.tsv
# 探针（源码归档 .agents/summary/scriptdll-quest-driver/P0c57*ProbeTest.java.txt，用后即删）：
#   retail 侧 P0c57AcceptProbeTest（IR dump）/ definition 侧 P0c57AuditProbeTest（审计 dump），系统属性 p0c57.*
mvn -o -B surefire:test -Dtest=RetailSimpleTalkChainGateTest -Dretail.talkChain.fingerprintOut=$PWD/p0c57-fingerprints-dump.tsv
QUEST_FORK_COUNT=2 .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T3
```
