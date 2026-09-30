# P0c-46 角色轴：交付角色收窄（`XML_ONLY_ROLE_AXIS_PENDING` 收口）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-26
- 车道：SimpleTalk 链（`quest_client_talk_chain_steps.tsv` 生成器）
- 判例：QE-076（守卫类型对齐 / 恒真守卫）、QE-077（角色轴与角色域收窄剪除）
- 前置：P0c-43（改道 24123）、P0c-45（身份轴剪除 35010/35011 + `npc_check` 口径修复）
- 编号消歧：本片编号 **P0c-46**（本车道续片 29）。并发 lane **同号**：其「续片 20 / P0c-46」= hunt+collectitem 混合链词汇
  （15608/80848/80958，产物 `p0c46_retire_hunt_collect_rows.py`）；此外 "P0c-45 / 续片 19"（链式接取登记缺口）与
  "续片 25 / P0c-51"（FOBJ 采集行）亦为该 lane。碰撞共 3 处 ⇒ 唯一键 = 「车道 + 续片号」，本片产物一律含
  `p0c46_role_axis`/`p0c46_refreeze`/`p0c46_blast_radius`/`p0c46-role-narrowing` 等可辨识名。

## 1. 结论

遗留 XML 把**交付/领奖角色**铺到链上每个 NPC（判例 1484：5 个 NPC 各带一份**逐字相同**的
`npc-complete` 块），而真端 `reward_npc_name` 唯一、客户端任务书 `end_npc_ids` 唯一且与之相等——
两源都不给这些 NPC 交付角色。本片按**角色域投影**判据把交付角色收窄到真端声明的交付 owner：

| 项 | 值 |
|---|---|
| 裁定表 | `p0c46-role-narrowing-decisions.tsv`（33 行 / 17 任务，code `LEGACY_ROLE_SPREAD_DELIVER`） |
| 产出（被剪） | 33 份重复 `B NPC_COMPLETE` 块 + 80752 的 2 条重复领奖窗 R 行 |
| 收口验证 | `p0c46-role-axis-census-post.tsv` 上 `DELIVER_DUPLICATE_BLOCK` **0 项**（33 候选清零），只余 5 项结构不同的 `DELIVER_UNCOVERED` |
| 登记表 | 5105 → **5071** 行（`1a3386b2…` → **`ef2cc1a5…`**，422554 字节） |
| 指纹重冻 | `changed={17 任务}`、`added/removed []`，双副本 **`9c60b0c3…`** |
| 生成器保真 | `FIDELITY_OK`，**20 张**输入表零漂移（新增第 20 张 = 本裁定表） |
| 净树 | 20/20（链门 2 + 审计 17 + 契约门 1） |
| T1 | 73 例 1F（唯一失败 = 在册 lane 项 `20035` 漂移码；本片 17 任务不在其中） |
| T2 / T3 | 见 §8（T3 归因按失败身份集对拍） |
| 保留面 | 5 项 `DELIVER_UNCOVERED` 结构不同 → **不剪**，登记（§9） |

## 2. 轴的定义与来历

P0c-45 把 `npc_check` 标记口径修好（复合条目展开）后，留下一条**明确的边界**（P0c-45 报告 §10）：

> `npc_check` 只判「集合成员」，**不判角色**——同一 NPC 在声明集内、但服务了真端没给它的角色
> （例如把 `talk_npc1` 的页服务在 `reward` NPC 上）不会被标记。

本片就是这条轴的收口。它与 P0c-45 的身份轴**正交**：

- **身份轴**（P0c-45，`LEGACY_EXTRA_DELIVERY_OWNER`）：NPC 不在任何声明集内（多余 owner）⇒ 整组记录剪除；
- **角色轴**（P0c-46，`LEGACY_ROLE_SPREAD_DELIVER`）：NPC 是链上**另有角色**的合法 NPC（接取/阶段），
  只是不该服务交付角色 ⇒ **角色域**剪除（接取/阶段记录必须原样保留）。

## 3. 判据历史与校准（QE-072 纪律：先校准再判）

| 版本 | 判据 | 结果 |
|---|---|---|
| v1 | 「同名块在多个 NPC 上重复」 | **否决**：40 项中绝大多数是**逐 NPC 对话绑定**（1484 的 5 个 NPC = 接取 + 3 阶段 + 报告，全 `RETAIL_MATCH`；`B NPC_START` 是每个链上 NPC 的 `QUEST_SELECT` 入口，删掉会打断阶段交互）。误报源 = 判据不看角色、也不看真端 `talk_npcK` 声明 |
| v2 | 投影式三轴，但页分类把 `SELECT5/SELECT6` 当阶段页（实为真端报告页族），且覆盖比较未做角色域裁剪 | **否决**：248 项压倒性假阳性（主体 `page:SELECT5`）；覆盖守卫对全部候选 FAIL |
| **v3（本片）** | 轴域收窄为 **DELIVER**；页分类按真端语义；覆盖守卫**角色域内**；裁定对象 = 声明集**单值** | 33 可剪 + 5 保留 + 信息轴（见 §4） |

校准（`p0c46_role_axis_census.py`，两个方向都必须成立）：

- **A 前态触发**：`p0c43-registry-pre.tsv`（24123 改道前）必须出现 `24123 DELIVER_UNCOVERED@204345`
  ——接取 NPC 服务整条交付流（`SELECT5/SELECT6/reward window/CHECK_*/SET_SUCCEED`），而 owner 204387
  当时只有阶段行 ⇒ 覆盖不成立 ✓；同时 35010/35011 按 P0c-45 前态触发 ✓。
- **B 现态不触发**：现行登记表上 24123 / 35010 / 35011 **均无候选** ✓（已裁定项的判据不再重复触发）。

## 4. 轴分解（先分解再动手）

```
DELIVER 候选 38 项 / 22 任务
├─ 33 项 / 17 任务  DELIVER_DUPLICATE_BLOCK  →  可剪（块逐字相同 + 角色域投影 ⊆ owner）
└─  5 项 /  5 任务  DELIVER_UNCOVERED        →  不剪（自带 owner 没有的交付页/动作，另行裁定）
ACCEPT 扩散 34 项 / 25 任务  INFO（分属 P0c-42 接取入口轴，不混判）
STAGE  扩散 28 项 / 25 任务  INFO（分属阶梯轴，不混判）
```

22 个任务全部满足「真端 reward 唯一解析 ∧ 客户端 end 唯一 ∧ 两者相等」；33 个被剪 NPC 全部
**另有链上角色**（7 个 = 接取 NPC，26 个 = 阶段 NPC）——这是轴⑥的机器核验（生成器复算）。

## 5. 五源取证

| 源 | 1484 | 2266 | 3087 | 80752 |
|---|---|---|---|---|
| 真端 `Quest_SimpleTalk.xml` `reward_npc_name` | `Shugo_LF2_13`(798126) | `Aurtri`(203654) | `Talos`(798144) | `event_warewolf`(833584) |
| 真端 `acquired_npc_name` / `talk_npcK` | `Shugo_LF2_14` + 3 阶段 | `Valuerin` + `Neifenmer` | `Shugo_LF2a_16` + 宝箱 | `event_ivan` + `event_warewolf` |
| 客户端任务书角色列 `end_npc_ids` | 798126 单值 | 203654 单值 | 798144 单值 | 833584 单值 |
| 客户端生命周期对齐 | 仅接取 NPC `CLIENT_LIFECYCLE_ALIGNED`，其余 `EVIDENCE_REQUIRED` | 同左 | 同左 | 同左 |
| census `xml_ladder` DEVIATION | `SETPRO1@204011/204045/204048` | `SETPRO1@203655` | `SELECT1_1@700419/798144/798201` | `SETPRO1@833584` |

对立面只有退役 XML 自身（它把 `npc-complete` 复制到每个链上 NPC）；本片不动 `npc-complete` 的
**参数**（被剪块与 owner 的块逐字相同），所以这次裁定不涉及任何奖励索引/动作语义的取舍。

## 6. 生成器通道（修复必须落进生成器）

`p0c46-role-narrowing-decisions.tsv` → `build_quest_client_talk_chain_steps.py` 装载段，**10 轴 fail-closed**：

① `code` 域；② 与阶梯/接取入口/改道/多余 owner 四表**互斥**；③ owner 必须 `RETAIL_TABLE`；
④ 真端 `reward_npc_name` 唯一解析 == 裁定表 `owner_name` == `owner_npc` ≠ `prune_npc`；
⑤ 客户端 `end`（任务书索引**全行并集**）唯一 == `owner_npc`；⑥ `prune_npc` 另有链上角色
（真端 `acquired ∪ talk_npcK` 或客户端 `start ∪ progress`）；⑦ `prune_npc` ∉ 交付声明集；
⑧ 同任务同 owner；⑨（转写循环内）**覆盖守卫**：交付块参数**逐字相同** + 交付动作/页集 ⊆ owner；
⑩（转写循环内）**越界守卫**：被剪 NPC 在 XML 里只许出现在 `<dialog>`/`<npc-complete>`。

**剪除是角色域的**：新增 `role_pruned()`（B 块）与 `role_pruned_route()`（R 行，仅交付动作/交付页），
接取与阶段记录一律保留——这正是与 P0c-45 整组剪除的本质区别。

### 6.1 顺带修复：P0c-45 的覆盖守卫曾是**恒真**的（QE-076）

写轴⑨时发现 P0c-45 的 `_served(npc_id)` 里有一处类型不对齐：`TR.group(4)` 是**字符串**，而
`owner_id` 是 **int**（`resolve_npc` 返回 `ids[0]`，是数字字符串经 `int()` 后仍是 str——见生成器
`resolve_npc` 返回 `ids[0]`）。两侧都取空集 ⇒ `xa <= oa and xp <= op` 退化为「空集 ⊆ 空集」**恒真**。
本片把两侧统一为字符串，并复核：修好后 **35010/35011 的剪除仍通过**（输出未变，见 §7 的
"撤表重生成 = 逐字节等于 P0c-45 收口值"）⇒ 之前的裁定结论不受影响，但"守卫通过"在 P0c-45 那
一轮**不构成**证据。教训入卡 QE-076：**跨通道比较必须做类型/符号空间对齐；守卫本身要用校准证明非空**。

## 7. 干跑 / 安装 / 爆炸半径 / 指纹

- **干跑**（`p0c43_dryrun_diff.py --expect <17 ids>`）：`DRYRUN_OK`，变化面 == 17 任务，
  `ADDED [] / REMOVED []`（无整块增删），20 张裁定表零漂移。
- **撤表重生成**（判据 = 生成器是纯函数）：把本裁定表移开后重跑 ⇒ 输出**逐字节等于 P0c-45 收口值**
  `1a3386b26a84c34f975e1da7725dbc51` / 5105 行 / 429549 字节 ⇒ 本片对生成器的行为增量**只有**新通道。
- **爆炸半径**（`p0c46_blast_radius.py`，pre = 上一步重建的前态）：`BLOCKS 295 -> 295`，
  `CHANGED [17 任务]`，`ADDED [] / REMOVED []`；逐行核对 = 删 **33 块 + 2 R 行**、增 **1 R 行**（重编序号）。
- **保留性抽查**：被剪 NPC 的 `B NPC_START` 与阶段 R 行（`QUEST_SELECT/SELECT2/SELECT2_1/SETPRO1`）
  逐字保留（1484@204045、2266@203655、3087@700419、4052@205166、80752@833629 五处实录）。
- **指纹**：`-Dretail.talkChain.fingerprintOut`（冻结模式）→ `p0c46_refreeze_fingerprints.py --apply`
  ⇒ `changed = 17 任务`、`added/removed []`、PRE 值校验通过、双副本 **`9c60b0c3f5d92d89a3a71799f1778910`**；
  校验模式（无 `-D`）`RetailSimpleTalkChainGateTest` 2/2 绿。
  （重冻脚本另修一处：`changed` 是**字符串排序**的 tuple，与按数字序书写的 `EXPECTED_CHANGED` 直接
  比较在 17 个 id 跨 4/5 位时必然不等——改为集合比较；P0c-45 两个 id 时侥幸一致。）

## 8. 门禁

| 门 | 命令 | 结果 |
|---|---|---|
| 净树（链门 + 审计 + 契约门） | `mvn -o -B test -Dtest='RetailSimpleTalkChainGateTest,QuestDialogOrderAuditTest,QuestClientContractGateTest'` | **20/20 绿**（`gates/P0c46-nettree.log`）；审计绿 ⇒ 被剪 NPC 的块移除**未孤立任何客户端声明页**（被剪块与 owner 逐字相同、owner 仍在册） |
| 生成器保真 | `p0c42_builder_fidelity_check.py` | `FIDELITY_OK`（422554 字节）+ **20 张输入表零漂移** |
| T1 | `run_quest_gates.sh T1` | 73 例 **1F** = 在册 lane 项 `20035`（`登记=REJECTED:RETAIL_TALK_HUNT_CHAIN_DEFERRED 实际=REJECTED:RETAIL_ITEMPLAY_OUTPUT_UNRESOLVED`，DataDriven 轴，与本片 17 任务无关）（`gates/T1-193333.log`） |
| T2 | `run_quest_gates.sh T2 <17 ids>` | 85 例 **3F + 1E**（`gates/T2-194055.log`）；对拍最近基线 `T2-175135` 的 only-in-right = `RetailOwnershipGateTest`（catalog+retention 在 **19:47:57** 被 lane 翻转 ⇒ 6224 vs 6199）、`JournalRewardRowRepairContractTest`（15613 var0，lane 19:43:27 改 `RetailDataDrivenDefinitionCompiler`）、`QuestKillCounterRetailGateTest`（声明集 +50091/50092，lane 计数轴）——**全部落在 lane 19:42–19:50 的窗口**，本片 17 个 id 零命中 |
| T3 | `run_quest_gates.sh T3` | 2006 例 **118F + 27E + 1S**（`gates/T3-194812.log`）；对拍基线 `T3-175816`（18:05，早于本片 18:15 安装）的 only-in-right = 10 条身份：`QuestClientContractGateTest`（12 条 fatal **全是 50089/50090 + NPC 835680/835681**）、`QuestEventShardRetailAlignmentTest`×3、`LegacyTemplateMirrorRouteRegressionTest.uniqueClientGraphCandidatesUseOwnedAcceptAndReportPages`（50089）、`QuestRepeatLifecycleTest`（15476 NPE）、`QuestMonsterProgressContractAuditTest`、`QuestPrematureRewardRouteExclusionTest`、`RetailDataDrivenGateTest`（类级：基线的方法级条目被 tee 截断改名，实为同一在册项 20035）——**全部溯源 lane 19:42:30–19:50:08 的源改动**（`RetailSimpleHuntPlan`/`RetailNpcNameIndex`/`RetailDataDrivenDefinitionCompiler` + 19:47:57 catalog·retention 翻转 + 19:49 DD 漂移/指纹）；**本片 17 个 id 在全部失败条目里零命中**。反证：契约门在 **19:33 的净树跑（含本片改动）绿**，T3 于 19:48:12 启动（catalog 写入后 15s，lane 半途态）。 |

> 时序：本片安装 18:15–18:17 → 净树 + T1 19:33（**早于 lane 全部窗口**，干净读数）→ T2 19:40:55 → lane 改源 19:42:30–19:50:08 → T3 19:48:12。
> T1 用例数 63 → 73 说明**并发 lane 扩过 T1 类集**（如 `RetailSimpleHuntFamilyGateTest` 单类 385s），
> 故归因一律按**失败身份集**而非条数（QE-069）。

## 9. 残留与登记

1. **`DELIVER_UNCOVERED` 5 项**（2964@278137、11106@798979、21036@798713、39003@800512、49003@800511）：
   这些 NPC 自带 owner 没有的交付页（`SHOW_SELECT_QUEST_REWARD_WINDOW1`/`SELECT3*`）或交付动作
   （2964 的 `SELECT_QUEST_REWARD`）⇒ 结构不同，**不得剪**；登记待逐项取证（可能形：真端数据缺口
   / 客户端多样式 / 镜像族）。见 `p0c46-blocked-rows.tsv`。
2. **ACCEPT 扩散 34 项 / 25 任务**：`SELECT1_1`/接取窗页被铺到多 NPC（3087@700419/798144 等）。
   与 P0c-42 的 `ACCEPT_PAGE_NOT_EMITTED` 同域但方向相反（那边是缺页、这边是滥铺），
   判据需与编译器 `acceptFlow` 交互，另立面：`XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING`。
3. **STAGE 扩散 28 项 / 25 任务**：阶段页/`SETPRO` 在声明集外 NPC（含 1323@203939 这类**物件哨兵**
   阶段的派生形）⇒ 与阶梯轴同域，登记 `XML_ONLY_STAGE_ROLE_SPREAD_PENDING`。
4. 既有在册余量不变：`SELECT6_CLOSE_EXIT_OTHER_FAMILIES`(62)、`XML_RETENTION` 1011 未达(44)、
   25070 FOBJ 页形、REWARD 投影余量(15)、真端数据缺口桶（`ACQUIRE_NPC_UNRESOLVED` 62 /
   `MONSTER_UNRESOLVED` 56 / `SENTINEL_AREA_PENDING` 64）。

## 10. 复现命令

```bash
D=.agents/summary/scriptdll-quest-driver
python3 -B $D/p0c46_role_axis_census.py --out $D/p0c46-role-axis-census-post.tsv \
        --emit-decisions $D/p0c46-role-narrowing-decisions.tsv   # 现态 0 候选（收口证明）
python3 -B $D/p0c43_dryrun_diff.py --expect 1484,2266,2271,2480,2538,2663,2914,2954,3037,3041,3087,3093,4052,11010,11103,11460,80752
python3 -B $D/p0c42_builder_fidelity_check.py
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest -Dretail.talkChain.fingerprintOut=/tmp/p0c46-fp.tsv
python3 -B $D/p0c46_refreeze_fingerprints.py /tmp/p0c46-fp.tsv --apply
mvn -o -B test -Dtest='RetailSimpleTalkChainGateTest,QuestDialogOrderAuditTest,QuestClientContractGateTest'
bash $D/run_quest_gates.sh T1
```
