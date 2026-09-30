# QE-110：`EnterArea` 具名区域的悬空引用封板（50125/51125 维持 XML 保留）

> 用户 Goal（2026-09-30）：「摒弃当前的任务引擎，完全按真端来，当前任务引擎只负责现有的任务 XML 运行。」
> 本片处理接取轴的最后一条保留码：`ADJUDICATED:RETAIL_ACQUIRE_NPC_SENTINEL_AREA_PENDING`
> （真端 `category_acquire_=EnterArea`，但生产 `ai-areas.xml` 里没有该任务的区域绑定）。
> **本片结论是 HOLD（证据封板）：真端与客户端数据里该区域只有引用、没有几何定义，
> 在不发明坐标的前提下无法发放 ⇒ 两行继续 XML 保留，无翻转、无引擎改动。**

## 0. 摘要（TL;DR）

| 项 | 本片结果 |
|---|---|
| 涉及行 | `50125`（Elyos，奖励 NPC `Marmara`）、`51125`（Asmodian，`Grimron`） |
| 真端接取形 | `category_acquire_=EnterArea`、`value0_acquire_=Tiamat_Down_QuestArea_Q50125`、进度 `PVP 20` |
| 区域几何 | **不存在**：世界文件 `<真端根>/Map/Worlds/tiamat_down/world.xml` 的 `<questscript_area>` 计数 = **0** |
| 唯一另一处引用 | 活动 AI 模式 `<真端根>/Map/XML/NpcAIPatterns_Event_KJS.xml` 的 `enable_area`（启用一个不存在的区域 ⇒ 悬空） |
| 客户端口径 | `world.pak` 条目 `client_world_tiamat_down.xml` 与 `Quest_unpacked/*.xml` 同样只有引用、没有几何 |
| 裁定 | **HOLD**：owner/reason 不变（`XML_RETENTION` + `ADJUDICATED:RETAIL_ACQUIRE_NPC_SENTINEL_AREA_PENDING`），只升级证据列 |
| DD 分类桶 | 不变：`ADOPTED` 1463 / `REJECTED:RETAIL_ACQUIRE_NPC_SENTINEL_AREA_PENDING` 2（本片零翻转） |
| 引擎改动 | 无（本轴已有的两条常设 tripwire 覆盖「绑定出现 ⇒ 必须翻片」，见 §6） |
| 待用户裁定 | 真端数据里这两行**实际不可接取**；本服 XML 仍保留「靠近 Marmara(205958) 接取」的历史路径。是否按真端改为不可接取，需单独授权（本片不删语义边） |

## 1. 判据：真端的区域发放必须先有区域对象

1. **接取类别 `EnterArea` 的 `value0` 是区域名**（不是 NPC、不是页）：真端 ScriptDLL 的 DataDriven 装载器
   对 `category_acquire_` 做 `_wcsicmp` 分派，`EnterArea` 分支构造 `QuestProgressExtraInfo_EnterArea`
   （`<真端根>/server58/MainServer_ScriptDLL64/ScriptDLL64.c` 中 `LoadProgressInfo()` 的
   `L"EnterArea"` 分支；同一分派表里还有 `Talk`/`Pvp`/`EnterWorld`/`TalkFOBJ`）。
2. **区域对象只由世界数据创建**：NPCServer 的 `QuestArea` 由 `CreateQuestArea` 解析世界文件的
   `<name>` / `<points_info>` / `<quest>` 三段（`<真端根>/server58-source/NPCServer_NPCSvr64/classes/Quest/QuestArea.cpp:147`、
   `:157`；类头 `include/Quest/QuestArea.h`）。生产同义载体即 `ai-areas.xml` 的
   `<quest_area world_id=… name=… quests="…">`。
3. **AI 模式的 `enable_area` 只是"启用已存在的区域"**：`AP_EnableArea` 只有注册与默认名
   `__NoName_Area__`（`classes/Misc/AP_EnableArea.cpp`），没有几何创建路径；因此区域名不存在时该动作是悬空引用。
4. 推论：**"真端行写了区域名" ≠ "该区域存在"**。缺几何就不能编译成 `SystemGrant`——那会变成凭空发任务（发明数据），
   与「禁止发明 id/页/数据、缺声明 fail-closed」的既定口径冲突。

## 2. 逐行证据（可复算，见 §5 脚本）

| quest | 真端 DD 行 | 世界文件 | AI 模式引用 | 客户端口径 | 裁定 |
|---|---|---|---|---|---|
| 50125 | `data_driven_quest.xml:30066`（`value0_acquire_` 在 `:30070`） | `tiamat_down/world.xml`：`<questscript_area>` 计数 0，区域名 0 命中 | `NpcAIPatterns_Event_KJS.xml:5037`（模式 `Tiamat_Down_Quest_Npc_01`，`:5026`） | 客户端 DD 同值（`Quest_unpacked/data_driven_quest.xml:29759-29771`）；任务表有该行（`quest.xml:229773`）但无区域字段；世界文件无该区域 | **HOLD** |
| 51125 | `data_driven_quest.xml:30542`（`:30546`） | 同上 | 同上（Elyos/Asmodian 共用同一区域名） | 客户端 DD 同值（`:30235-30247`） | **HOLD** |

补充证据链：

- **世界归属**：`WorldId.xml` 的 `id="600040000" → Tiamat_Down`（DD 里 `50128` 的 `EnterWorld` 进度值就是 `600040000`），
  与 `tiamat_down/world.xml` 的 `<name>tiamat_down</name>`（`:258`）一致；该世界文件里唯一的怪物族名（`TDown_DrakanWi_Tiamat_60_An` 等）
  也与兄弟行 `50127/50129` 的真端击杀名单同族。
- **字节级排他扫描**：`<真端根>` 全树 18,877 个文件按 UTF-16LE 与 UTF-8 两种编码扫描区域名，命中 **2 个文件**
  （DD 行、活动 AI 模式），**没有任何几何定义**。
- **客户端世界包**：`<客户端目录>/data/world/world.pak` 的 `client_world_tiamat_down.xml`（aion_pak 解包）
  是 `clientzones` 文档（subzone / radar_area / item_use_area / generalarea 等），**不含 `questscript` 容器、不含该区域名**。
- **同族悬空**：本轴不是孤例——活动模式里的 `enable_area` 共引用 385 个区域名，其中 **43 个**在任何世界文件里都没有定义
  （2017「데바의 날」活动退役后的残留）。50130/51130 的 `Tiamat_Down_M_QuestArea_Q50130` 属同一族，但这两个 id 不在生产 catalog（6224）内，
  不在本片范围。

## 3. 交叉证据：为什么判据仍是「按 quest id 的区域绑定」而不是「按区域名匹配」

若把接取门改成"DD 的区域名必须与世界文件的区域名相等"，**会误杀 7 行早已采纳的真端行**：

| quest | DD `value0_acquire_` | 世界文件实际的区域名 |
|---|---|---|
| 13965 / 23965 | `IDEternity_War_QuestArea_Q13965` | `QuestArea_Q13965a` / `QuestArea_Q23965a` |
| 13966 / 23966 | `IDEternity_War_QuestArea_Q13966` | `QuestArea_Q13966` |
| 13967 / 23967 | `IDEternity_War_QuestArea_Q13967` | `QuestArea_Q13967` |
| 25674 | `DF6_QuestArea_Q25674` | `LF6_QuestArea_Q25674`（世界 lf6） |

⇒ 真端**生效**的判据是「世界文件的 `<questscript_area><quest>` 绑定（按 quest id）」；DD 的区域名是可能陈旧的元数据。
因此本片**不改** `RetailSimpleHuntDefinitionCompiler#requireAcquire` 的 `questAreas.isBound(questId)` 判据，也不新增按名匹配的门。

## 4. 改动清单

| 文件 | 改动 |
|---|---|
| `.agents/summary/quest-acquire-area-q50125/scan_acquire_area_unbound.py` | 新增：可复算取证脚本（真端字节级扫描 / 世界文件 questscript_area / AI 模式 enable_area / 客户端 DD 与任务表 / 客户端世界文件 / 世界 id 表 / 真端装载器标签） |
| `.agents/summary/quest-acquire-area-q50125/forensics_acquire_area_unbound.tsv` | 新增：脚本输出的 12 条判据结果（C1–C10） |
| `.agents/summary/quest-acquire-area-q50125/client_evidence/client_world_tiamat_down.xml`、`WorldId.xml` | 新增：客户端 `world.pak` 的 Tiamat Down 世界条目与世界 id 表（解包产物，44MB 整包只留这两份证据） |
| `.agents/summary/quest-acquire-area-q50125/2026-09-30-qe110-acquire-area-unbound.zh-CN.md` | 本报告 |
| `src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.tsv` | 50125/51125（`:5243`、`:5299`）第 5 列证据升级为 QE-110 取证；**owner / reason 不变** |
| `src/test/resources/quest/retail-xml-retention.tsv` | 同上（双副本逐字节一致） |

**未改**生产代码、任务 XML、`ai-areas.xml`、漂移/指纹登记：本片没有可采纳的形状（见 §1.4）。

## 5. 复算方法

```bash
# ① 真端 + 客户端解包数据的全部判据（约 40 秒，输出 12 条）
python3 .agents/summary/quest-acquire-area-q50125/scan_acquire_area_unbound.py

# ② 客户端世界条目（world.pak 是签名 XOR 0xFF 的 zip；用既有 aion_pak 工具解包，只留需要的两份）
python3 "<客户端解包根>/aion_pak.py" unpack "<客户端目录>/data/world/world.pak" \
    --out <临时目录> --no-progress
# 取 <临时目录>/client_world_tiamat_down.xml、WorldId.xml → client_evidence/
```

`scan_acquire_area_unbound.py` 按 `ENVIRONMENT.md` 的同宿主目录约定解析 `<真端根>`/`<客户端目录>`/`<客户端解包根>`
（可用 `--server/--client/--client-unpack` 覆盖；`<客户端解包根>` 回退到家目录下的标准位置）。

## 6. 门禁与验证

本轴**不需要新门禁**：两条既有常设 tripwire 已经把两个方向都锁住了（本片复核后确认，未改动）：

1. `RetailDataDrivenGateTest.driftVersusShellsIsRegistered`：DD 全族漂移登记必须与逐任务分类一致，
   50125/51125 登记为 `REJECTED:RETAIL_ACQUIRE_NPC_SENTINEL_AREA_PENDING`。
   一旦有人补上区域绑定，编译立刻变 `ACCEPTED` ⇒ 该门变红，强制按 QE-104 对拍翻片。
2. `RetailDataDrivenGateTest.retentionManifestMatchesDriverOwnership`：可驱动却留在 `XML_RETENTION` = 违规（反向同锁）。
3. 本片额外复核（只读，未新增断言）：DD 表中 161 行已采纳的 `EnterArea` 行**全部**有区域绑定；
   仅有的 2 行无绑定就是本轴——判据与台账当前完全一致。

运行记录：

- 聚焦门（`RetailOwnershipGateTest`、`RetailDataDrivenGateTest`、`RetailSimpleHuntFamilyGateTest`、`RetailNonIrAxisGateTest`、`RetailTsvManifestGateTest`）：
  21 例，红 2 例，均为 §8 的既存红（80817 指纹冻结、26 行封顶登记）。
- `run_quest_gates.sh T1`（`QUEST_FORK_COUNT=2 QUEST_LOG_DIR=target/agent-logs/qe110`）：
  81 例、红 2 例 = 同一组既存红；日志 `target/agent-logs/qe110/T1-225006.log`。
- `run_quest_gates.sh T3`（`QUEST_FORK_COUNT=2 QUEST_LOG_DIR=target/agent-logs/qe110`）：1856 例
  （176 failures + 64 errors），红身份集 `target/agent-logs/qe110/t3.ids` = **198 条**，与
  `target/agent-logs/qe107b/t3.ids` 逐行对拍 = **ADDED 0 / REMOVED 0**；日志 `target/agent-logs/qe110/T3-225030.log`。
- `verify_retirement.py`：`catalog=746 directory=746 retired=5478 sum=6224` → `OK: 目录一致，无悬空生产引用`
  （本片不动 owner，与 QE-109 收尾值相同）。
- 双副本一致性：`diff -q src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.tsv src/test/resources/quest/retail-xml-retention.tsv` = 相等。

## 7. 真机验收清单（若用户要求复现真端现状）

本片**不是行为变更片**（owner 不变、语义边未动），因此没有新增真机验收项。可选复核：

1. 在 Tiamat's Down（600040000）内**不应该**因为进入某个区域而自动接到 Q50125/Q51125（真端数据里区域不存在）；
   本服现行行为仍由 XML 的「靠近 Marmara(205958)/Grimron」历史路径接取（与真端不同，见 §9.3）。
2. 已完成/进行中的存档不受影响（本片未改 XML、未改 owner）。

## 8. 既存红（本片未引入、未顺手回写）

1. `RetailDataDrivenGateTest.frozenFingerprintsCoverExactlyTheRetiredQuests`：`80817` 冻结值与实际 IR 不一致
   （`f5245215…` → `00a65835…`），QE-102 起刻意保留。
2. `RetailNonIrAxisGateTest.cappedQuestsAreNeverRetailDriven`：26 条封顶登记行在 HEAD 上已是 `RETAIL_TABLE`
   （35052/35055/35056/35057/35064/35065/35514/35515/36542-36548/45052-46548 同形），属行为变更，需单独成片 + 用户裁定。
3. `CollectTurnInClientActionAlignmentBatchTest#quest1137…` / `#quest18745…`：HEAD 既存红（旧壳 39 检查对与入口页合同），本片未触碰。

## 9. 边界与未做 / 待用户裁定

1. **未做真机验收**：本片是台账/证据片，未启动服务器、未做客户端实测。
2. **`sql.rar` 未解压**（本机无 rar 工具）：世界几何结论基于 ①世界文件全树扫描 ②客户端世界包 ③真端装载器源码三处，
   而 `<questscript_area>` 属世界文件的装载路径已由 `QuestArea.cpp` 的解析标签证实，故 DB 侧不影响本结论。
3. **待裁定（真端对齐口径）**：真端数据里这两行**没有发放源**（活动数据退役后的悬空引用），
   本服 XML 仍给「靠近 Marmara 接取」的历史路径 —— 这是一处**已知的真端/本服差异**。
   若要求 100% 对齐真端（即：不可接取），需要单独授权删该接取边（属语义边删除，本片按既定口径不删）。
4. **本片无关的兄弟轴**：`15548/25548`（`RETAIL_ACQUIRE_GRANT_UNSUPPORTED`，真端发放源未定位）与
   `3122/4122/3123/4123`（多段击杀）仍是各自轴的 HOLD/待办。

## 10. 顺带发现（QE-111 候选，不在本片范围）

台账 `ADJUDICATED:<码>` 与真端漂移登记 `REJECTED:<码>` 在 4 行上不一致（其余 41 行一致）：

| quest | 台账 reason | 漂移登记 | 备注 |
|---|---|---|---|
| 25051 | `ADJUDICATED:RETAIL_TALK_HUNT_CHAIN_DEFERRED` | `REJECTED:CURATED_LEGACY_CONTRACT_LOCK` | `RetailDataDrivenGateTest.CURATED_DEFERRED` 覆盖后应记 curated 码；台账行未同步 |
| 80885 / 80940 / 80961 | `ADJUDICATED:RETAIL_ACQUIRE_NPC_UNRESOLVED` | `REJECTED:RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED` | 编译器现行拒绝码与台账登记码不同源（台账陈旧） |

建议下一片：把「DD 族 `ADJUDICATED:` 码 = 漂移登记码」做成常设门（curated 行按 curated 码），并同步这 4 行 —— 属**可独立成片**的代码/台账修复。
