# P9 语义层收口批（NPC 名成员集语义 + 组表轴扩域 + NPC_ 前缀归一化）

- 日期：2026-10-03；性质：**行为变更批**（native 车道接取/交付/中继路由面扩张），测试 PENDING。
- 授权口径：用户 2026-10-03「1.完善语义层」；Maven 未授权 ⇒ 全部聚焦套件待跑（见 §7）。

## 1. 根因（一句话）

真端 codegen 把任务注册在**名字**节点上（`FUN_180cb5920(node, npcName, questId)`：`<真端根>/server58/MainServer_ScriptDLL64/ScriptDLL64.c`），运行期由 NPC 自身的
对话名匹配；因此**同一名字下的全部模板**（同名多模板 NPC、`quest_ai_name` 组、别名表多值）
都是合法受理者——「任一成员可接取/交付/中继」，不是歧义。

native 车道把「非唯一命中」一律 fail-closed（`NativeNpcNameResolver.resolve` → `Resolution.AMBIGUOUS`），
把这条真端语义当成数据缺陷。旧车道 `RetailNpcNameIndex` **早已具备**组通道与 `NPC_` 前缀归一化，
native 车道只差这三条规则 ⇒ 五族共 221 行不可路由。

## 2. 三条缺失规则（本批补齐）

| # | 规则 | 证据 | 影响面（复算） |
|---|---|---|---|
| R1 | **多成员 = 任一受理**（name_desc/name 多值、别名多值、`quest_ai_name` 组展开） | ScriptDLL `b5920` 按名字注册；`<真端根>/Map/XML/npcs*.xml` 的 `<quest_ai_name>`；旧车道 `RetailNpcNameIndex.resolve` 返回 `Set` | 五族 221 行 |
| R2 | **复合单元格按逗号拆**（`TOWN_SHUGO_GARDENER_1001,1002,1003` 形） | 真端 `Quest_SimpleTalk.xml` 实测 6 单元格（3 花园 + 3 `TEST_SimpleTalk_*`） | SimpleTalk 2 行 |
| R3 | **`NPC_` 前缀归一化**（表 `Gardugu` ↔ 模板 `NPC_Gardugu`；表 `NPC_Housing_FOBJ_01` ↔ 模板 `Housing_FOBJ_01`） | 旧车道 `RetailNpcNameIndex.NPC_PREFIX` 同规；本仓 `npc_template_*.xml` 实测两向都存在 | SimpleTalk 3 单元格 |
| R4 | **组表轴只收 DD 表** → 扩到**全部族表**（原实现把服务面写成 DD-only） | 生成器 `p0c52_quest_ai_name_groups.py` 原 `dd_references()` | 组表 33 → 60 组（+27，零删行） |

## 3. 复算（离线独立脚本，非运行时代码自证）

口径：`name_desc → name → 别名 → NPC_ 前缀 → 对话名组`，复合单元格先拆；对每族表的
`acquired_npc_name / reward_npc_name / talk_npcN / task_npc / value0_acquire_` 逐格解析。

| 族 | 行数 | 收口前「非唯一 ⇒ 不可路由」行 | 收口后残余行 | 收口后残余名 |
|---|---:|---:|---:|---|
| SimpleHunt | 1865 | 84 | 16 | `GAb1_0X_VillageNN_Guard` ×16（既有 MULTI_TITLE_ID 裁定） |
| SimpleTalk | 3152 | 104 | 2 | `LDF5A_Munition_Vritra`（真端缺名，中继列 2 行） |
| SimpleCollectItem | 262 | 24 | 0 | — |
| SimpleUseItem | 160 | 4 | 2 | `magician_apprentice`（既有 LEGACY_ACCEPT_CONTRADICTS_CLIENT 裁定） |
| SimpleItemPlay | 43 | 5 | 0 | — |
| SimpleSerialHunt | 16 | 0 | 0 | —（本批未改该类） |
| CombineTask | 574 | 0 | 0 | —（`task_npc` 574 行全唯一可解，本批未改） |

系统性发放哨兵（`_faction_` 98 / `_challengetask_` 75 / `_area_` 8，另 `_Area_` 4 单元格在 SimpleHunt）
不属 NPC 名解析面，由 `NativeSystemGrantLane` 承接，维持现状。

## 4. 落地清单（本批 diff）

| 文件 | 变更 |
|---|---|
| `questEngine/tablelane/NativeNpcNameResolver.java` | 新增 `resolveMembers(String)`：R1+R2+R3，仅完全无命中返回空表（fail-closed 保持）；`resolve()` 语义未动 |
| `questEngine/tablelane/SimpleTalkHandler.java` | 接取/交付/中继改成员集（`Map<Integer,List<Integer>>` + 全成员 `installInterest`），运行期判定改 `contains` |
| `questEngine/tablelane/SimpleHuntHandler.java` | 同上（含 `questsForNpc` 成员判定） |
| `questEngine/tablelane/SimpleCollectItemHandler.java` | 接取改成员集；交付 helper 改 `resolveMembers` |
| `questEngine/tablelane/SimpleItemPlayHandler.java` | 接取/交付/中继改成员集 |
| `questEngine/tablelane/SimpleUseItemHandler.java` | 交付 helper + 中继链改成员集（步序号仍按单元格位取 `index+1`） |
| `aion/data/static_data/quest/retail/retail-quest-ai-name-groups.tsv` | 33 → 60 组（生成器重跑；原 33 组零删行） |
| `.agents/summary/scriptdll-quest-driver/p0c52_quest_ai_name_groups.py` | 轴扩域 + 路径归位（`quest_retail/` → `quest/retail/`）+ 客户端根双布局解析 + 停写已退役的 rejected 表 |
| `SimpleTalkNativeFamilyGateTest` | 冻结值 39→4 / 207→181 / 86→0 / 9→0 |
| `SimpleUseItemNativeFamilyGateTest` | 58→57；`unresolvedNames` = {`magician_apprentice`} |

## 5. 残余 20 行的性质（均为**既有裁定**或真端缺名，非本批缺陷）

1. 16 行 `GAb1_0X_VillageNN_Guard`（SimpleHunt）：生成器 `MULTI_TITLE_ID` 排除（成员跨 4 个 title_id）。
   客户端 `<quest_ai_name>` 明示 4 名成员共享对话名，按 R1 应可收口；**放开该判据 = 推翻 P0c-53 裁定**，
   须用户裁定（建议：按「客户端声明优先」放开，并把 title 不一致降级为告警）。
2. 2 行 `magician_apprentice`（SimpleUseItem）：`LEGACY_ACCEPT_CONTRADICTS_CLIENT` 登记排除（客户端
   [804897,804898] vs 遗留 [804868,804869]）——须逐行身份裁定。
3. 2 行 `LDF5A_Munition_Vritra`（SimpleTalk 中继列）：真端 `npcs*.xml` 与客户端块**双向零命中** ⇒
   待真端/客户端补充取证（ScriptDLL 名字串或客户端新版本快照）。

## 6. 风险与未验证面

- ~~**SimpleItemPlay 门常量可能下降**~~ → **已裁决：不是常量漂移，而是本地一条假设判据与真端不符，见 §8**：其 `unroutableQuestIds = 43 − 8` 的成因含 `metadataClean`
  （走旧车道 `RetailQuestMetadataCompiler`，消费同一张组表）。组表扩域后该面可能由 unclean 翻 clean，
  35 需按实测重冻；本批**未**预改该常量（避免猜测）。
- `SimpleHunt/SimpleCollectItem` 族门若断言路由/NPC 注册计数，同样可能需重冻。
- `RetailDataDrivenClientPresenceGateTest`（337 孤行冻结）理论上不受影响（本批只增组行、零删行），
  仍须跑门确认。
- 运行期 `installInterest` 注册面扩大（例：SimpleTalk 交付面 82 行由单 NPC 变多 NPC），
  重复注册由 `QuestEngine.registerQuestNpc(...)` 幂等承接，需真机验收确认无重复弹窗。

## 7. PENDING：验证命令（需用户授权）

```
mvn -q test-compile
mvn -q -Dtest='NativeNpcNameResolverTest,SimpleTalkNativeFamilyGateTest,SimpleHuntNativeFamilyGateTest,SimpleCollectItemNativeFamilyGateTest,SimpleUseItemNativeFamilyGateTest,SimpleItemPlayNativeFamilyGateTest,SimpleSerialHuntNativeFamilyGateTest' test
mvn -q -Dtest='*Quest*Test,*Retail*Test' test   # 聚焦套件（门禁债对比基线）
```

## 8. 风险点「SimpleItemPlay 门常量 / cutsceneid1 判据」的真端取证（2026-10-03 第二轮，只读取证）

**结论**：这不是「组表扩域后常量漂移」的问题，而是本地车道的一条**假设判据与真端不符**：
真端本族用 `cutsceneid1` 列**直接驱动过场**，既没有 `cs1_haction` 触发列，也没有 `item_check` 列。
应按真端接线（把 `cutsceneId` 交给过场口）而不是继续 fail-closed。

**甲方证据（4 条，全部第一手复算）**：

1. 真端表 `Map/XML/Quest_SimpleItemPlay.xml`：43 行；`cutsceneid1` **2 行**（13400=859、23400=860）、
   列覆盖 `acquired_npc_name` 43 / `use_item_name` 41 / `talk_npc1` 11；**`cs1_haction` 0 行、`item_check` 0 行**。
2. `ScriptDLL64.c` 里 13400 的注册 thunk `FUN_180dbc490`（:2302614 起）：
   先 `FUN_180cab520(0x3458, …)` 登记族表行，随后取行记录字段 `*puVar1` **直接**调
   `FUN_180cacb30(0x3458, *puVar1, 1, …)`（:2302617）；23400 同型（:2302646，`0x5b68`）。
   ⇒ 过场由 thunk 用**行内字段**驱动，不存在任何「haction 触发列」。
3. `item_check` 是 **SimpleUseItem** 族的列（该族 5 行），不是本族的列 ⇒ 本族 `row.itemCheck()` 恒假。
4. 客户端随包 `data_unpacked/CutScene/CutScenes.xml` 实测含 `<id>859</id>` 与 `<id>860</id>`
   ⇒ 859/860 是**客户端过场资源 id**（cs_*.seq），与任务号、与 NPC 无关。

**落地建议（本批未实施，待排期）**：

- `SimpleItemPlayHandler` 取消 `row.cutsceneId() != null → fail-closed`；把 `cutsceneId` 直接交给过场/电影端口。
- 本族无 `item_check` 列，该判据可一并删除（或降级为装载期告警，保持证据面可观测）。
- 门常量重冻**预期**（须以实测为准，勿照抄）：`routed 8 → 10`、`unroutable 35 → 33`。

### 8.1 落地实测（2026-10-03，A 批）

已按上节建议拆除该假设判据（**未**新增页动作触发）：

- `SimpleItemPlayHandler`：删除 `row.cutsceneId() != null || row.itemCheck() → fail-closed`；
  `cutsceneId1` 改为**证据面**（新字段 + `cutsceneId(int)` 访问器，2 行：13400=859、23400=860），
  并注明本族无 `cs1_haction` ⇒ 不合成触发、不参与路由闸门。
- 门禁新增 `cutsceneColumnIsAnEvidenceFaceOnly`（冻结覆盖行 + 2 个资源 id + 未声明行为 null）。
- **门常量实测未变**：`routed 8` / `unroutable 35` 保持不变（上节"预期 8→10 / 35→33"**不成立**，已作废）。
  真因：13400/23400 不在 `retail-xml-retention` 清单内 ⇒ `RetiredQuestIds` 不含 ⇒ owner 未退役，
  该两行的开关是**owner 裁定**而不是过场判据。⇒ 若要上线这两行，须先做 owner 裁定（C 类事项）。
- 新增反证/正证（`ScriptDLL64.c:2686058` 区间）：row loader 按 `_wcsicmp` 连续读取
  `con_quest`(+0x2b4) / `cutsceneId1`(+0x2b8) / `cs1_haction`(+0x2bc) / `cs1_progress`(+0x2c0)
  ⇒ 表列确为装载期字段；本表缺 `cs1_haction` ⇒ 该槽恒空，与"无页动作触发"一致。
  thunk `FUN_180cacb30(questId, rowValue, 1, nodes, actions, curPtr, ctx)` 的**播放点绑定仍未坐实**
  （函数体按 node/action 数组比较后播放 node 值），故本批不做推测实现。
- 验证：`mvn -q -Dtest=SimpleItemPlayNativeFamilyGateTest test` EXIT=0。

### 5.1 裁决执行（2026-10-03）：16 行 `GAb1_*_Guard` 已放开

- 生成器 `p0c52_quest_ai_name_groups.py`：新增 `MULTI_TITLE_ALLOWED_PREFIXES = ("GAb1_",)`；
  命中的组**不再排除**，只打印 `WARN <组>: members span title_ids …`（title 不一致降级为告警），
  逐候选的 `title_ids != 1` 失败断言同步降级（仅非白名单前缀仍 fail-closed）。
- 重建产物：`retail-quest-ai-name-groups.xml` 60 → **76 组**（+16 Guard 组，零删行），生成器自检 `OK: 76 groups`。
- 门禁实测**零重冻**：家族聚焦 10 类 + `RetailTableSchemaGateTest` + `RetailOwnership/MetadataEquivalence/
  DriverOverlay` + `XmlDataLoaderTest` + `NativeNpcNameResolverTest` + `QuestEngineNpcDialogDispatchTest`
  + `QuestNpcFactionRetailGateTest` 全绿（EXIT=0）——16 组的成员此前已由其它解析通道供给，
  本批修的是**证据面**（真端/client 分组登记）而非路由面。
