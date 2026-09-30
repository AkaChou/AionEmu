# QE-107：真端逻辑名别名 + DD 领奖集合轴（客户端交付集仲裁）与 5 行 owner 翻转

> 用户 Goal（2026-09-30）：「摒弃当前的任务引擎，完全按真端来，当前任务引擎只负责现有的任务 XML 运行。」
> 上一片（QE-106）把 SimpleTalk / SimpleItemPlay 的接取与交付角色改成「唯一 id 或客户端确认的集合」。
> 本片把**同一缺陷类的 DataDriven 侧**补齐：领奖名多解（`REWARD_NPC_UNRESOLVED`）与真端逻辑名未登记
> （活动 NPC 的 `LC1_/DC1_` 变体族）两条轴共用一套引擎判据。

## 1. 判据（客户端交付投影 + 版本化别名，两条通道都不许猜）

1. **多 id 领奖集**：真端 `reward_npc_name` 走统一名字通道（精确 → 声明组 → 名前变体）；解析出**恰一个** id ⇒ 放行；
   解析出**多 id** ⇒ 只有与该任务客户端交付投影（`/quest/quest_client_handin_npc_sets.tsv`，来源 = 客户端 npc 块的
   `end_npc_ids`）**逐元素相等**才放行；否则维持 fail-closed `RETAIL_REWARD_NPC_UNRESOLVED`。禁止按名字形状猜集合。
2. **真端逻辑名未登记**：真端活动表用逻辑名（`Event_NPC_guardrung_l` / `event_npc_idsweep_eyeloong`），
   服务器按 `LC1_/DC1_`（光/暗）× 名后缀逐 id 建模板（`LC1_event_npc_shugo_guardrung`=835132、
   `DC1_event_npc_shugo_guardrung`=835133、`LC1/DC1_event_npc_idsweep_eyeloong`=836258/836259）。
   名字形态不可机械推导 ⇒ 走**版本化别名表** `retail-npc-name-aliases.tsv`（数据，不写死在编译器里），
   每条别名都要有服务器模板 + 客户端集合或遗留 XML 见证。

## 2. 引擎改动（本片生产代码）

| 文件 | 改动 |
|---|---|
| `RetailDataDrivenDefinitionCompiler` | 编译入口收敛为**单一签名**（接取/交付集合轴与 EA 别名表都是显式参数，门与生产同源；删掉会静默关掉该轴的便捷重载）；混合链分支的领奖判据由「恰一个 id」改为 `rewardNpcSet(...)`：唯一 id 直接放行、多 id 集按客户端交付投影逐元素仲裁、空集按原码如实拒绝（拒绝详情仍给出完整解析集） |
| `RetailDataDrivenTalkHuntChainCompiler` | `compile`/`build` 的领奖参数由 `int` 改 `Set<Integer>`；领奖 `QUEST_SELECT` 交付入口、`fobjCollectHandIn`、`journalRowRepair` 按集合**逐 NPC** 展开（`TalkToNpc(npcId, action)` 天然分键）；完成流与奖励窗自动确认只发**一次全局路由**（`completeFlow(metadata, rewardNpcs, …)`，避免 `AMBIGUOUS_TRANSITION`） |
| `RetailQuestDriver` | 生产装配把交付投影注入 DD 编译器 |
| `retail-npc-name-aliases.tsv` | 追加 3 条真端逻辑名 → 变体 id（`event_npc_guardrung_l`=835132、`event_npc_guardrung_d`=835133、`event_npc_idsweep_eyeloong`=836258,836259） |
| `quest_enterarea_zone_resolution.tsv`（双副本） | 追加 `30722 DF5_SensoryArea_Q30722 → DF5_SENSORYAREA_Q30722_220080000`、`30772 LF5_SensoryArea_Q30772 → LF5_SENSORYAREA_Q30772_210070000`（source=`legacy-enterzone`；两个区名早已在 `zones_quest.xml` 用真端世界文件的球体坐标登记） |

## 3. 效果（5 行翻转，逐行可复核）

`30722 30772 50064 50108 51064`：`ADJUDICATED:RETAIL_REWARD_NPC_UNRESOLVED` → `ADOPTED` →
`RETAIL_TABLE`（retention 双副本 + XML 删除 + 目录登记行删除）。DD 分类桶：`ADOPTED` 1448 → 1453，
`REJECTED:RETAIL_REWARD_NPC_UNRESOLVED` 7 → 2（余下 15690/25690 的真端名 `ld_rw_npc_gd5001` 在服务器模板与客户端
投影里都零命中，属真数据缺口，继续留 XML）。

## 4. 翻转证据（QE-104 生产路径对拍）

方法（`probe/`，与 QE-106 同规格）：A = 现行 owner（XML 在场，`ProductionQuestDefinitions.definitionInOverlay(id)`）；
B = 临时把这 5 行翻成 `RETAIL_TABLE` **并**移走 XML + 删目录登记行后，同一入口再 dump（overlay fail-fast 要求两者同时改）。

| quest | shared / onlyXml / onlyRetail | 节点集 |
|---|---|---|
| 30722 | 46 / 3 / 5 | 相同 |
| 30772 | 46 / 3 / 5 | 相同 |
| 50064 | 37 / 3 / 5 | 不同（`reward` 投影 var0 1→0） |
| 50108 | 72 / 6 / 11 | 不同（同上） |
| 51064 | 37 / 3 / 5 | 不同（同上） |

**5/5 行共享边非零**（无零共享红线）。差异面完全落在已定轴上：

1. **30722 / 30772**：`started --EnterZone--> reward` 由「直发 `LEVEL_AND_VISIBILITY_REFRESH`」改为
   「置行号 + `PACKET_ONLY`」并补一条登录自愈边（`EnterWorld` + `StatusIs(REWARD)` + `var0<末行` ⇒ 置末行）；
   领奖窗改为**每个交付 NPC 一条** `TalkToNpc(npc, 108)` + 一条全局 `QuestDialog(108)`（旧 XML 是 `npc-complete`
   逐 NPC 的 `SELECTED_QUEST_REWARD*` 选择梯）。遗留 XML 的交付/完成块本来就同时绑定 804897 与 804898，
   与客户端 `end_npc_ids={804897,804898}` 一致 —— 既有多 owner 交付形被完整保留。
2. **50064 / 50108 / 51064**（DD 采集族规范形）：交付检查对由旧 XML 的 `TalkToNpc(npc, 1009)+ShowQuestDialog(10002)`
   改为真端采集族的 `TalkToNpc(npc, 20002)`（带 `HasItem` 全组条件 + `RemoveItem` 全组动作）与不足时的
   `ShowQuestDialog(10001)` 自环；`reward` 节点投影随客户端任务书行（1 行 ⇒ 末行 0）而非旧 XML 的 `var0=1`。

临时翻转已还原（`git status` 复核）；A/B dump、比值与探针脚本留在 `probe/`。

## 5. 真机验收清单（用户执行）

1. **30722 / 30772**（SensoryArea 引导任务）：与 `Rosalee`/`Ginie` 接取 → 走进 `DF5/LF5_SENSORYAREA_Q3072x` 区域
   应立刻置领奖态；与 `Ajinos(804897)` **或** `Werine(804898)` 任一人对话都能开领奖窗并完成（旧形只认其一）。
2. **50064 / 51064**（活动「红眼兔军团讨伐」）：与 `LC1_event_npc_shugo_guardrung(835132)` / `DC1_…(835133)`
   接取 → 收集 10 件交付物 → 交付检查页应「够 10 件直接进领奖窗、不足显示结果页且不扣物」。
3. **50108**（活动「德哈里尔的密约」）：与 `LC1/DC1_event_npc_idsweep_eyeloong(836258/836259)` 任一名接取与交付。
4. 通用回归：上述 5 条任务在**旧存档**（旧 XML 停在领奖态）登录后应能继续完成；奖励窗 `108` 自动领取入口只出现一次。

## 6. 门禁与验证

- `RetailDataDrivenGateTest`：漂移登记与冻结指纹与实现同源（新翻转 5 行指纹入库；**80817 的既存漂移刻意不重冻**）。
- `RetailSimpleTalkGateTest`、`RetailOwnershipGateTest`、`RetailTsvManifestGateTest`、
  `RetailEnterAreaZoneRegistrationGateTest`（新区名必须是 zones XML 登记名）、`ProductionCatalogWhitelistVerificationTest`
  —— 绿。
- `run_quest_gates.sh T3`：1856 例，红身份集 198 个，对上一片（QE-106）日志 `target/agent-logs/qe106/T3-205424.log`
  **ADDED 0 / REMOVED 0**（本片日志 `target/agent-logs/qe107b/T3-212317.log`）。
- `verify_retirement.py`：`catalog=756 directory=756 retired=5468 sum=6224 — OK`。

## 7. 既存红（本片未引入，也未顺手回写）

1. `RetailDataDrivenGateTest.frozenFingerprintsCoverExactlyTheRetiredQuests`：`80817` 冻结值与实际 IR 不一致
   （`f5245215…` → `00a65835…`），QE-102 起刻意保留，作为「指纹只增不改」政策的活体证明。
2. `RetailNonIrAxisGateTest.cappedQuestsAreNeverRetailDriven`：`quest-start-metadata-retail-cap-exceptions.tsv`
   尚有 26 条封顶行（`35052/35055/35056/35057/35064/35065/35514/35515/36542/36543/36544/36547/36548` 及镜像）在
   HEAD 上已是 `RETAIL_TABLE`，登记行必须按 P0c-8c/9 流程随采纳切片移除（会把该任务从「服务端封顶 82」改为
   真端 `UNLIMITED`，属行为变更，需单独成片 + 用户裁定）。

## 8. 边界与未做

- 未做真机验收（第 5 节清单待用户执行）。
- DD **领奖**侧的多 owner 已支持；DD **接取**侧仍走各分支既有判据（混合链只要求非空，采集/谈话分支仍要唯一）。
- 15690/25690 的真端名 `ld_rw_npc_gd5001` 在服务器模板与客户端投影里都零命中：真数据缺口，不猜、不硬编码。
- 8 行 `RETAIL_ACQUIRE_GRANT_UNSUPPORTED`（全 `category_acquire_=EnterArea`，如 15548/15674/18739/18740）
  需先取 quest_area/客户端发放证据，属「区域接取」另轴。
