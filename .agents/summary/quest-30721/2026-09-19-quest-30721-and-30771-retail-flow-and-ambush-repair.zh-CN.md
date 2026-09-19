# 任务 30721 与 30771 缺陷排查与真端流程修复报告

## 1. 任务背景与问题现象

在先前提交 `c96576121` 移除了任务 30721 的接取前置道具限制后，玩家反馈该任务及其对等魔族任务 30771 在后续流程中仍存在多处严重缺陷，无法正常流转与交付：

1. **道具无法使用（致命卡死）**：玩家到达任务指引的“坠落的城堡残骸”(`Collapsed Debris Pile`, NPC 805207) 尝试使用任务道具 `182215698`（Sedative 镇静剂）时，服务端提示 `STR_CANNOT_USE_ITEM_INVALID_ZONE` (1300143)，“无法在此处使用该物品”，任务无法推进。
2. **使用道具后剧情怪物缺失**：剧情中玩家服药休息时遭到德拉坎偷袭，下一步 Rosalee 对话询问“说德拉坎突然出现，攻击了自己”，但原本服务端无怪物生成。
3. **完成 NPC（`npc-complete`）错误混入接取 NPC**：营地接取 NPC 804704 / 804728 被错误配置在 `npc-complete` 与 `reward` 对话路由中，可直接在营地提前领奖。
4. **领奖对话直接跳过 `DEFAULT_SUCCESS` 故事页**：与真正调查官对话时直接弹第 5 页领奖窗，跳过了剧情故事页 `select_success` (10002)。
5. **残留怪物追打与异常状态存档自愈缺失**：偷袭怪缺乏在对话时的 despawn 清理机制，且缺乏 `enter-world` 下 `status=REWARD` 的自愈保护。

---

## 2. 根因分析与真端证据链

### 2.1 物品使用区域（ITEMUSEAREA）缺失
- **代码根因**：`PlayerRestrictions.java#canUseItem` 检查 `item.getItemTemplate().hasAreaRestriction()`，读取道具模板中的 `usearea="LF5_ITEMUSEAREA_Q30721"`（天族）与 `usearea="DF5_ITEMUSEAREA_Q30771"`（魔族）。
- **缺陷**：`zones_quest.xml` 中完全缺少上述两个区域定义，导致 `player.isInsideZone(restriction)` 永远返回 false，抛出系统消息 1300143。
- **真端证据**：5.8 客户端解包 `source_sphere.csv`：
  - `usearea_lf5_itemusearea_q30721,itemUseArea,lf5,0,152.65,1430.13,488.10,41.04` (Cygnea, 210070000)
  - `usearea_df5_itemusearea_q30771,itemUseArea,df5,0,2924.01,1672.71,322.26,58.71` (Enshar, 220080000)
  残骸堆坐标（805207: 145.91, 1427.65, 484.66；805208: 2906.78, 1664.24, 321.17）完全处于各自球体内。

### 2.2 德拉坎怪物偷袭机制
- **真端证据**：`data_driven_quest.xml` 的 `ItemPlay` 步骤定义：
  `<value5_progress_>Relative IDTiamat_R2_NobleDrakanFi_Quest_58_An, 2, 120</value5_progress_>`
- 怪物模板为 `236654`（Protectorate Drakstrike，守护军团德拉坎），生成数量为 2，留存时间 120 秒。
- 剧情对话 `select4` 按钮明确为 `HACTION_SELECT4_1`：“说德拉坎突然出现，攻击了自己”。
- 修复方案：在 `item-play` 的 `after-commit` 中添加 `<spawn-npc-at-player slot="drakan1" template-id="236654" heading="0"/>` 与 `slot="drakan2" heading="60"`，并在返回对话及完成时执行 `<despawn-npc>`。

### 2.3 领奖 NPC 与对话路由收口
- **真端证据**：`data_driven_quest.xml` 明确：
  - 30721: `value0_acquire_: LF5_Eukraton_E` (804704), `reward_npc_name: LF5_Monroe_E` (804870)
  - 30771: `value0_acquire_: DF5_Engrid_E` (804728), `reward_npc_name: DF5_Hank_E` (804871)
- 客户端 HTML（`quest_q30721.html` / `quest_q30771.html`）的 `quest_summary` 明确第 5 步为“向调查官报告”。接取 NPC 绝不承担交付。
- 领奖预览：原 XML 中 `npc-complete` 配置了 `<preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>`，导致玩家点击 NPC 触发 `USE_OBJECT` (-1) 时立即弹出奖励选择框，绕过了 `QUEST_SELECT` 应答的 `DEFAULT_SUCCESS` (10002)。移除 `USE_OBJECT`，仅保留 `SELECT_QUEST_REWARD`，确保剧情对话正常展现。

---

## 3. 修复内容清单

1. **`src/main/resources/aion/data/static_data/zones/zones_quest.xml`**：
   - 补齐 Cygnea (210070000) 的 `LF5_ITEMUSEAREA_Q30721`（球心 152.65, 1430.13, 488.10，半径 41.04）。
   - 补齐 Enshar (220080000) 的 `DF5_ITEMUSEAREA_Q30771`（球心 2924.01, 1672.71, 322.26，半径 58.71）。
2. **`src/main/resources/aion/data/static_data/quest_definition/quests/30721.xml`**：
   - `s2 -> s3`（`item-play 182215698`）：增加 `spawn-npc-at-player`（德拉坎 236654 x2）。
   - `s3` 对话与 `s3 -> reward`：增加 `despawn-npc` 清理偷袭怪。
   - 移除 804704 的所有 `reward` / `npc-complete` 路由。
   - 804870 的 `npc-complete` 移除 `USE_OBJECT` 预览动作，保留 `SELECT_QUEST_REWARD`。
   - 增加 `enter-world` 下 `status=REWARD` 自愈为 `var0=4`。
3. **`src/main/resources/aion/data/static_data/quest_definition/quests/30771.xml`**：
   - 镜像应用上述所有修正（804728 仅接取，804871 唯一领奖，偷袭怪生成与清理，自愈与预览动作收敛）。
4. **`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest30721And30771RetailFlowTest.java`**：
   - 新增端到端单元测试，全覆盖 30721 与 30771 从接取、中途对话、道具使用、袭击怪生成/销毁、到单 NPC 报告领奖的全流程与区域覆盖判定。

---

## 4. 验证结果

- **专项单元测试**：
  `mvn test -Dtest=Quest30721And30771RetailFlowTest` -> **3/3 PASS**
- **回归与生产门禁**：
  `mvn test -Dtest=QuestInventoryStartItemGateTest,Quest30721And30771RetailFlowTest,QuestItemSourceContractGateTest,QuestRetailStartMetadataGateTest,QuestStartEligibilityContractTest,PlayerQuestStartEligibilityPortTest,QuestEnterZoneStartOwnerRegressionTest,ProductionCatalogWhitelistVerificationTest`
  -> **36/36 PASS**
  - `PRODUCTION_COMPILE_OK=6189`
  - `PRODUCTION_COMPILE_FAILURES=0`
  - `PRODUCTION_INTERACTION_OBJECT_FAILURES=0`
  - `PRODUCTION_WHITELIST_VIOLATIONS=0`
- **XML Schema 校验**：
  - `zones.xsd` 校验 `zones_quest.xml` -> **PASS**
  - `quest_definition.xsd` 校验 `30721.xml` 与 `30771.xml` -> **PASS**
