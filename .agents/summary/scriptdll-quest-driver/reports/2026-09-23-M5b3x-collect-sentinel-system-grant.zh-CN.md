# M5-b3x：SimpleCollectItem 类别哨兵行 = 系统发放形状落驱动（批次 3）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 口径：真端优先。接续 M5-b2c 的裁定（`_faction_` 40 行不是解析缺口，而是"系统发放 + 无接取路由"），
> 本切片把该形状落进 SimpleCollectItem 驱动并完成对应退役。

## 1 交付

**生产代码**
- `QuestEvent.java`：新增 `QuestEvent.SystemGrant`（sealed 成员，字段级 record）——系统发放边的事件事实。
  真端普查证据：哨兵行 43/43 无生命周期三元组处理器；发放走真端系统（阵营星期位 / 挑战任务 / quest_area），
  不是 NPC 对话。发放服务后续显式分发该事件（或按 `RetailAreaEngine` 先例直启），定义图由此在无接取路由时保持连通。
- `RetailSimpleCollectItemTable.java`：新增 `GrantKind`（NPC / FACTION / CHALLENGE_TASK / AREA / UNKNOWN_SENTINEL）
  与 `grantKindOf()`（三类真端发放哨兵大小写不敏感识别）；`Entry` 携带 `grantKind`。
- `RetailSimpleCollectItemDefinitionCompiler.java`：
  - 哨兵行不再要求接取 NPC 解析；`UNKNOWN_SENTINEL` 仍按 `RETAIL_ACQUIRE_NPC_SENTINEL` 拒绝；
  - 系统发放形状：跳过 acceptFlow / acceptContinuation / setproRoute，改登记一条
    `SystemGrant`（unaccepted→started，StartEligible + VISIBILITY_REFRESH）边；保留采集对象路由、
    报告流（QUEST_SELECT→SELECT5 + 39/20002 交付检查）、完成流（8..23）与报告 NPC 关窗出口（无条件登记）；
  - 新稳定拒绝码 `RETAIL_REWARD_NPC_FACTION_COMPOSITE`：奖励引用是 `<地图>_<势力名>` 复合名
    （如 `LDF5a_Silverlin_L`，真端 npcs.xml 亦无此 NPC；含 `_LD` 双侧变体）——解到唯一管理 NPC 需要
    客户端 `STR_DIC_E` 逐条裁决，另切片处理；
- `RetailQuestMetadataCompiler.java`：`NPC_FACTIONS`（势力名→id）开放为包内共享（供复合名识别）。

**生产数据**
- `npc_factions/npc_factions_quest.xml`：12 行真端全 0（Silverlin/Greenhat 禁用任务）的星期位由全 1 修为全 0
  （早前生成脚本写错；真端 `npcfactions_quest.xml` 是唯一权威）。修正后阵营随机池按 `isActiveOn` 永不发放。
- 退役 **20** 个 SimpleCollectItem XML（GRANT_DAILY 且奖励 NPC 唯一可解析的哨兵行），
  `quest_definition_catalog.xml` 同步收敛（3780 → **3760**）。

**清单与登记（机器生成，未手改）**
- `retail-simple-collect-item-drift.tsv`：178 行按新编译口径重算（accepted 89 → **109**）。
- `retail-simple-collect-item-ir-fingerprints.tsv`：冻结指纹 86 → **106** 行。
- `retail-xml-retention.tsv`：RETAIL_TABLE/OK 2444 → **2464**。
- 新工具：`m5b3x_faction_sentinel_grants.py`（43 行三方对账 + `--fix-production` 星期位修正）、
  `m5b3x_build_sentinel_decisions.py`（20 行 ADOPT_RETAIL 裁定）；证据
  `m5b3x-collect-sentinel-grants.tsv`、裁定 `m5b3x-collect-sentinel-decisions.tsv`
  （`build_retention_list.py` 合并消费两份裁定文件）。
- 门禁 `RetailSimpleCollectItemGateTest`：新增系统发放形状不变量（接取/续页/SETPRO1 路由必须为零）、
  哨兵行分支；下限 89/86 → 109/106。

## 2 证据

- 真端对账：`python3 -B m5b3x_faction_sentinel_grants.py --fix-production`
  → `rows=43 verdicts={'GRANT_DAILY': 28, 'RETAIL_DISABLED': 15} production_fixed=12`
  （28/15 与 M5-b2c 完全一致；客户端接取页全部 `ask_quest_accept`、DLL 三元组全部 no）。
- 生产宇宙交集：40 行（`39611/47112/49611` 无生产 XML，不在宇宙）。
- 驱动结果：28 行 GRANT_DAILY 中 **20 行可编译并退役**（奖励 NPC 唯一：Palas 799805 / Priamos 799806 /
  Columba 799848 / Alaum 799849 / LF5_* 804941..804951 / DF5_* 804930..804957）；
  8 行 GRANT_DAILY + 12 行 RETAIL_DISABLED = 20 行复合奖励引用 → 新稳定码留 XML。
- 退役：`m5b3_retire_collect_item_xml.py` → `migrated=106 moved=20 missing=0`，
  `verify_retirement.py` → `catalog=3760 directory=3760 retired=2464 sum=6224 — OK`；
  `refresh_catalog_doc_links.py --apply` → `replaced=20 dangling=0`。
- 门禁：T1 七类 **25/25 绿 BUILD SUCCESS**；T2（20 id 反查 + T1）**82/82 绿**；
  T3 全树首轮 1947 例 / **1F**（`QuestRetailStartMetadataGateTest` 封顶例外台账残留本批退役 4 任务
  46537/46538/45053/45054——M4-b 同款过期）→ 按 M2-e/M4-b 惯例重跑
  `quest-systemic-goal/build_retail_contract_tsv.py`（cap 例外 42→38 行）后
  T3 复跑 **1947 例 / 0F / 0E / 1 skipped / BUILD SUCCESS / 289 s**（`gates/T3-m5b3x-run2.log`，
  唯一 skip = 既有 `QuestDialogMigrationEquivalenceTest`）。

## 3 结论

- **可删 XML 数 = 20**（35007/35008/35014/35015/35053/35054/36529/36530/36537/36538/45007/45008/45014/
  45015/45053/45054/46529/46530/46537/46538）：五条充分条件逐条满足——元数据对拍一致（npc-faction-id 由
  真端 quest.xml `<npcfaction_name>` 直达）；进度事件与 DLL 语义一致（普查 43/43 无三元组，报告流挂在奖励 NPC +
  采集对象签名 0x36/37/38）；合成 IR 与退役前 XML 的差异全部落在"XML 历史接取半边"（真端/客户端双侧证据判定
  XML 错，裁定 20/20 ADOPT_RETAIL）；家族门禁绿；不在保留清单、无未闭环口径冲突。
- **不可删逐条原因**：
  - 20 行复合奖励引用（8 GRANT_DAILY + 12 RETAIL_DISABLED）→ `RETAIL_REWARD_NPC_FACTION_COMPOSITE`，
    需要先按 `npc_factions.xml` 管理 NPC + 客户端 `STR_DIC_E_<qid>` 显示名逐条裁决（45021 的客户端文本点名
    Amphibia+Mollusca 两个管理 NPC，报告流可能要多 NPC 展开）——P1 收口切片处理。
  - 25 行可选奖励 + 24 行多物品 → 既有稳定码不变。

## 4 对拍结果

- **元数据**：43 行 `_faction_` 全部携带 `npc-faction-id`（真端 quest.xml 直达，`RetailMetadataEquivalenceGateTest` 全树绿）。
- **进度事件（DLL）**：普查 43/43 无生命周期三元组；奖励 NPC + 采集对象携带 0x3 + 0x36/37/38 采集签名，
  与合成 IR 的路由面（报告 NPC + 对象路由、零接取路由）一致。
- **IR**：漂移重算 178/178 有登记；20 行退役后由冻结指纹 106 行守等价证据（旧 86 行逐字节不变）。
- **调度**：T3 全树 1947 例 / 0F / 0E / 1 skipped（与 M5-c 终态同口径，零新增失败）。
- **客户端**：40 行客户端任务书 40/40 只有 `ask_quest_accept`（委托书 + 收起按钮），无 select1 ——
  XML 的 NPC 接取页（SELECT1/ACCEPT/REFUSE）确认为历史错误；QuestClientContractGateTest 绿。

## 5 未验证

- **阵营日常的客户端接受手势**：`NpcFactions.sendDailyQuest()` 用 `SM_QUEST_ACTION(questId, action=6)` 弹出
  任务窗口后，客户端回包落点（目标 NPC 对话动作还是无目标动作）需要实机抓包确认。SystemGrant 事件与
  `RetailAreaEngine` 直启先例已就位，抓包后可按证据接线（单独小切片）。在此之前，这 20 个任务由阵营系统
  每日发放、但不经 NPC 对话接取（与真端"无 NPC 接取"一致；GM 命令 `CmdStartQuest` 可用）。
- 实机客户端验收（领奖页 / 任务书行数 / 委托书 UI）继续记「待用户执行」。

## 6 阻塞与决策项

| 项 | 处理（不停等） |
|---|---|
| SystemGrant 分发接线（见 §5） | 记录为待实机抓包；不阻塞其它族。若实机证明接受手势走定义路由，则把该路由按证据补进系统发放形状 |
| 复合奖励引用 20 行（含 8 个 GRANT_DAILY） | P1 收口切片：npc_factions 管理 NPC + STR_DIC_E 逐条裁决 |

## 7 下一步

P1：SimpleCollectItem 收口（92 行 → 归零或全部有裁决）——先做 20 行复合奖励引用
（输入：`npc_factions.xml` 管理表 + `client_strings_dic_etc.xml` 的 `STR_DIC_E_<qid>` + 现行 XML 的管理 NPC 对），
再做 25 可选奖励 + 24 多物品的逐类裁定；复现：
`python3 -B .agents/summary/scriptdll-quest-driver/m5b3x_faction_sentinel_grants.py`
与 `mvn -o test -Dtest=RetailSimpleCollectItemGateTest`。
