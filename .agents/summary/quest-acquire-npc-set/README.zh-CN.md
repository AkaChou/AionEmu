# quest-acquire-npc-set：接取 NPC「集合」轴的取证与引擎方案（22 行待解锁）

> 用户 Goal：「摒弃当前的任务引擎，完全按真端来，当前任务引擎只负责现有的任务 XML 运行。」
> 本片处理保留清单里最大的一轴暂缓：`ADJUDICATED:RETAIL_ACQUIRE_NPC_UNRESOLVED`（25 行，其中 22 行属本轴）。
> **结论：名字已可解析，但解析结果是「多个 NPC id 的集合」——现行合成器要求每角色恰一个 id，故仍 fail-closed；
> 解锁需要引擎层支持「接取角色 = 客户端声明的 NPC 集合」，本片只固化证据与方案，不改生产行为。**

> **后续批次（2026-09-30）：引擎层已支持本轴，见 `2026-09-30-qe106-client-confirmed-npc-sets.zh-CN.md`**
> （接取 + 交付两侧；23 行翻转、2 行升级为 TALK_NPC_AMBIGUOUS、3 行 ItemPlay 换轴）。

## 1. 缺口与三方证据（真端 × 客户端 × 服务器）

暂缓理由原文：「name absent from this server's npc_name_index」。本轮把三源对齐后，**名字并非不存在**，
而是服务器 NPC 的 `name_desc` 与真端模板名在「变体编号 / 区域后缀」上不同形：

| 真端 `acquired_npc_name` | 客户端 `start_npc_ids`（`legacy-quest-dialog-contracts.csv`） | 服务器 `npc_template` |
|---|---|---|
| `GAb1_Sub_Fuen_E`（14211/24211） | 804749 804750 804751 804752 | `GAb1_Sub_01..04_Fuen_E` |
| `HousingManager_Li`（18806/18821/18828/18829） | 810017 810018 810019 810020 810021 | `HousingManager_L_{S,A,B,C,D}` |
| `HousingManager_Da`（28806/28821/28828/28829） | 810022 810023 810024 810025 810026 | `HousingManager_D_{S,A,B,C,D}` |
| `NPC_event_svs_jabsuroong`（50042/50043/51042/51043） | 832850 832851 | `NPC_event_svs_{l,d}c1_jabsuroong` |
| `NPC_event_devasday_shugo`（50047/50048） | 833051 833052 | `event_devasday_shugo_{l,d}_01` |
| `NPC_event_2014christmas`（50049/50050/50051） | 833509 | `event_2014christmas_l_redshugo` |
| `NPC_event_2014christmas_D`（51049/51050/51051） | 833510 | `event_2014christmas_d_blueshugo` |

逐行证据表：`acquire-npc-set-evidence.tsv`（22 行，含每个 id 的服务器 `name_desc`）。三源一致：真端是「逻辑 NPC 名」，
客户端把它展开成**可接取 NPC 集合**（多区域/多实例变体），服务器按变体逐个建 NPC。

## 2. 实测：加了别名之后仍然被拒（复现见 §5）

给 `retail-npc-name-aliases.tsv` 加 7 条别名（name → 客户端声明的 id 集合）后重跑家族门：

| 行 | 结果 |
|---|---|
| 14211 / 24211 / 18806 / 18821 / 18829 / 28806 / 28821 / 28829 / 50042 / 50043 / 50047 / 51042 / 51043 / 18828 / 28828 / 50048（16 行） | `REJECTED:RETAIL_ACQUIRE_NPC_AMBIGUOUS` |
| 50049 / 50050 / 50051 / 51049 / 51050 / 51051（6 行，单 id） | `DIFF:NODE_PROJECTION`（已能编译，与现行 XML 形有节点投影差异） |

根因（源码）：`RetailSimpleTalkDefinitionCompiler#requireNpc` 与 SimpleItemPlay 同族要求
`resolved.npcIds().size() == 1`，否则返回 `RETAIL_<role>_NPC_AMBIGUOUS`；`build()` 里
`int acquiredNpc = ...iterator().next()` 只接受单 id。**集合是引擎能力缺口，不是数据缺口。**

## 3. 顺带发现的守卫缺口（本片未改，登记）

生产入口 `RetailQuestDriver` 建 NPC 索引时含别名表
（`RetailNpcNameIndex.build(..., RetailQuestAiNameGroups.streams(), RetailNpcNameAliases.streams())`），
但 `RetailSimpleTalkGateTest` / `RetailSimpleItemPlayGateTest` / `RetailDataDrivenGateTest` 等**只含名字分组、不含别名**；
只有 `RetailSimpleHuntFamilyGateTest` 与生产一致。⇒ 别名成功解析的行，门会看到 UNRESOLVED 而生产已解析（门与生产分叉）。
解锁本轴时必须同片把受影响家族门补齐别名流（并优先统一所有门）。

## 4. 引擎方案（下一批实现，按用户「正确的引擎层支持」口径）

1. **接取角色支持集合**：`requireAcquire` 允许 `npcIds().size() > 1`，前提是**与客户端为该任务声明的
   `start_npc_ids` 集合逐元素相等**（客户端仲裁，真端名只负责给出候选集）；不相等 / 客户端无声明 ⇒ 维持 fail-closed
   （`RETAIL_ACQUIRE_NPC_AMBIGUOUS`）。
2. **合成：每个接取 NPC 一条接取段**：接取段（SELECT1 询问窗 + 1002/20000 提交边 + 1004/20001 拒绝 + 1008 关窗）
   对集合内每个 id 各展开一份（事件键 `TalkToNpc(npcId, action)` 天然按 NPC 分开，无 `AMBIGUOUS_TRANSITION` 风险）；
   谈话/领奖角色仍要求唯一（领奖 owner 唯一是既有合同）。
3. **数据来源**：客户端 `start_npc_ids` 需要仓库内投影（当前只在仓库外 headless 映射里）。做法 = 生成器 +
   冻结 fixture（`quest_client_accept_npc_sets.tsv`），与既有 `quest_client_*` 定义表同规范；**不是补丁表**，
   是客户端数据的投影，且必须与真端名候选集交叉验证。
4. **门禁**：受影响家族门补别名流 + 新投影；新增「集合 ≠ 客户端声明即拒」的负例测试；
   指纹重冻只在解锁行。
5. **翻转**：引擎支持落地后按 QE-104 做 retail-vs-XML IR 对拍，再走 owner 翻转三件套（retention + 删 XML + catalog）。

## 5. 复现与边界

- 复现：临时给 `retail-npc-name-aliases.tsv` 追加 §1 的 7 条别名 → `mvn -o -Dtest=RetailSimpleTalkGateTest,RetailSimpleItemPlayGateTest test`，
  失败信息即 §2 表；**本片已回滚该实验**（别名表与两个门均已 `git checkout` 还原）。
- 未做：引擎改动、客户端投影表、owner 翻转、指纹重冻（本片不改变生产行为）。
- 另：另外 3 行同码暂缓（80885/80940/80961，DataDriven）实测分类是 `RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED`
  （retention 滞后视图），属 ItemPlay 世界事件轴，不在本轴。
