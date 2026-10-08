# 帕休曼迪尔寺院 Ahbana 真端条件刷对齐（Warrior Monument 计数链）

## 1. 需求
- 实机反馈（截图 GPS：1356.58 / 151.99 / 246.27，world 300170000）：「位置附近应该有个 boss，但现在看不到」。
- 复核：该点距 **Ahbana The Wicked（216239）** 出生点 (1356.55, 147.81) 仅 2.5m；玩家确认「只是走过去看，**没砸纪念碑**」。
  现象本身符合我方旧机制（旧 handler 要求砸满 15 个才刷），但对照真端后发现三处偏差，
  属于与 Macunbello 同根因族的第二个实例（真端计数器条件刷未落地 + 变量未声明门禁 + handler 自造阈值）。

## 2. 根因（四处偏差）
1. **阈值不一致**：真端 `IDCT_SpecterN_Spawn >= 10`（砸 **10** 个纪念碑即出）；我方 handler 写死 `warriorMonument == 15`。
2. **真端条件刷未落地**：Ahbana 真端由 territory 条件刷生成（world_N.xml#16115 `SPG_N_SpecterNmd_55_Ah`），
   我方无对应条件、无 `IDCT_SpecterN_Spawn` 变量声明。
3. **门禁拦写入方**：纪念碑 216739 的 pattern（`IDCT_Quest_Reric_Normal.on_die`：发消息 + 计数器 +1）
   因变量未声明，被 `RetailPatternAI2` 运行时门禁整体拦下（静默回落模板 AI）——砸碑也不计数、不发消息。
4. **模板 AI 短路（更深一层）**：216739 的模板 `ai="warrior_monument"` 列在 `AI2Engine.QUEST_SIDE_EFFECT_AI`
   保护名单里 → `selectNpcAi` 对它有**硬短路**（`AI2EngineRetailSelectionTest:71` 锁定该契约），
   **即使变量声明也无法让 pattern 运行**；旧行为（`warrior_monumentAI2.handleAttack` 的
   "受击 10% 概率在碑位刷 Ahbana"）是 emu 自造，真端机制（阈值 10 条件刷）从来不生效。

## 3. 真端机制（world_N.xml territory，已解析）
| 项 | N 套 | H 套 |
|---|---|---|
| 纪念碑 | 216739 `IDCatacombsN_Reric_Q30222`（ai=IDCT_Quest_Reric_Normal → `IDCT_SpecterN_Spawn +1`） | 216740 `IDCatacombsH_Reric_Q30222`（ai=IDCT_Quest_Reric_Hard → `IDCT_SpecterH_Spawn +1`） |
| Boss | 216239 `IDCatacombsN_SpecterNmd_55_Ah`（ai=IDCT_Boss_Spectre） | 216158 `IDCatacombsH_SpecterNmd_55_Ah`（ai=IDCT_Boss_Spectre_Hard） |
| 条件 | `(IDCT_SpecterN_Spawn >= 10) && (SpecialServer_Cond == 0)`（#16203，spawn_page 1） | `(IDCT_SpecterH_Spawn >= 10) && (SpecialServer_Cond == 0)`（#4502，spawn_page 2） |
| 生成点 | (1356.546875, 147.808456, 250.0) dir 90 | (1356.571899, 147.764175, 250.5) dir 90 |
| initial_spawn_time | 1 (+1) | 1 (+1) |

- 真端纪念碑共 **15 组**（`SPG_Reric_Q30222_1..15`，全部 `no_respawn="TRUE"`）；砸 10 个即达阈值。
- Ahbana 真端带 7 点巡逻区（move_area_points，1317~1395 × 134~211），我方无此机制 → 用固定落点。
- 消息：砸碑每次发 **1400465**（STR_MSG_IDCatacombs_NmdSpecter_Spawn，纪念碑 pattern）；Ahbana 出现由
  on_wake_up 发 **1400470**（STR_MSG_IDCatacombs_NmdSpecter_Start）。
- 死亡链：Ahbana pattern `on_die` → `control_door(1)`（真端小号门，见 §6）；我方另有 handler 分支开 471 + 1401839。

## 4. 改动（3 个文件）
1. `src/main/resources/aion/definitions/compact/ai/condition-spawns.xml`：
   - 声明 `IDCT_SpecterN_Spawn` / `IDCT_SpecterH_Spawn`（L22191-22192，附 H 无写入方说明）；
   - 新增 #5021（N，`bt_page == 0`）/ #5022（H，`bt_page == 1`）：阈值 10、delay 1+1、
     落点 z 取我方 geo 实测面高 **246.27036**（真端 z=250 在我方会悬空 3.7m，探针证据见 §5）。
2. `src/main/java/com/aionemu/gameserver/instance/handlers/scripts/BeshmundirTempleInstance.java`：
   - `case 216739`：删自造计数（`warriorMonument==15` → `sp(216239)` + 1400470）与 1400465（改由 pattern 承担）；
     **保留** `despawnNpc(npc)`（砸碎表现）；同步删除 `warriorMonument` 字段；
   - `case 216239`（门 471 + 1401839）**保留**（见 §6 门号映射遗留）。
3. `src/main/java/com/aionemu/gameserver/ai/worlds/heiron/warrior_monumentAI2.java`（IR-010 幂等适配器）：
   - **删** `handleAttack` 的"受击 10% 概率在碑位刷 Ahbana"自造逻辑（真端无此机制）；
   - **`handleDied`**：保留打碑手感（`modifyDamage=1`、`maxHp=20` → 打 20 下碎），追加真端执行器驱动：
     `RetailConditionSpawnEngine.setVariable(instance, "IDCT_SpecterN_Spawn", 0, 1)`（驱动 #5021）
     + 全实例补发真端消息 1400465；注释写明"若将来放开 `QUEST_SIDE_EFFECT_AI` 短路需同批删本适配器"。
4. `src/test/java/com/aionemu/gameserver/dataholders/loadingutils/RetailAiDefinitionLoaderTest.java`：
   - 条件总数 4450 → **4452**；bt_page 分流 4/4 → 5/5；
   - Ahbana 断言（变量声明、阈值表达式 `IDCT_SpecterN_Spawn >= 10`、坐标/朝向 90/延迟 1）；
   - `supports` 静态列表 +4：216239 / 216158 / 216739 / 216740。
5. `src/test/java/com/aionemu/gameserver/ai/RetailPatternAI2Test.java`（新增
   `btConditionChainNpcsKeepRetailSupportWithProductionData`）：19 个条件链 NPC 带真实 owner
   （worldId=300170000 + 生产技能组）跑 `supports(pattern, npc)`——**这是条件刷能否接管的先决门禁**，
   只做 pattern 级静态判定（上一批）会漏掉技能组缺口造成的静默回落。

## 5. 验证
- 静态（已完成）：`xmllint`、`git diff --check`、IDE 三文件 0 错误；
  pattern 白名单审计（216739/216740/216239/216158 四 pattern 事件/条件/动作全过，0 问题）；
  `geo_surface_probe.py 300170000 1356.546875 147.808456 240 260` 实测碰撞面 z=246.27361（mesh
  `bu_ab_catacom_specterroom_01a.cgf`），且与玩家实测 z=246.26555 一致；
  216739 模板 `<stats maxHp="20" pdef="65000"/>`（打 20 下碎，`modifyDamage=1` 与之配套）。
- focused-test（已完成）：`RetailAiDefinitionLoaderTest` **6/6 全绿**（IDEA MCP，2026-10-08）；
  `RetailPatternAI2Test#btConditionChainNpcsKeepRetailSupportWithProductionData` **通过**
  （216739/216239/216583/… 19 NPC 带真实 owner 的 supports 全过）。
- 实机（待用户，需重启服务端）：砸 **10 个**纪念碑 → 看守者之枢纽出现 Ahbana + 1400470；
  击杀 → 1401839 + 门 471 打开 + 掉落链（Armor/宝箱）正常；砸碑期间每次收到 1400465。

## 6. 边界与遗留
- **H 套无写入方**：我方无 H 纪念碑（216740）刷点 → `IDCT_SpecterH_Spawn` 永不增长，#5022 当前不可达
  （随 `bt_page == 1` 预留，与 Macunbello 口径一致）。注意 216740 的模板 `ai="noaction"` **不在短路名单**、
  也没有 `modifyDamage=1`（`pdef=65000` 下普通伤害趋零）——将来补 H 版碑时两者都要处理。
- **门号映射遗留（独立批次）**：真端 BT 门用小号（1=Ahbana 门、3=Temadaro/DespawnLich 门、4=StatueDrakan 等），
  我方为主线路门 467/470/471/473 重编号；`RetailPatternAI2.controlDoor` 目前只对 300190000 有映射，
  故 Ahbana pattern 的 `control_door(1)` 在 BT 会找不到门（静默跳过）→ **handler 的 `case 216239`（开 471）
  保留为门链执行者**。做门号映射前先完成「真端小号 ↔ 我方门」全表对照（1→471、3→467 已有强旁证）。
- **适配器与短路的双写边界**：`warrior_monument` 的短路受 `AI2EngineRetailSelectionTest` 契约锁定；
  若将来放开短路让 pattern 接管，必须同批删除 `warrior_monumentAI2.handleDied` 的适配器，否则计数翻倍。
- Ahbana 的 move_area_points 巡逻未做（固定落点）。
- `SpecialServer_Cond` 不声明（普通服恒 0，表达式保留真端字面）。

## 7. 工具（保留于本目录）
- `extract_specter_rows.py <world_N.xml> [needle ...]`：从真端 UTF-16 world_N.xml 提取 Specter 相关行。
- geo 探针复用 `.agents/summary/inggison-somation-rock-z/geo_surface_probe.py`（只读）。
