# §10.3-#26 进区触发器接线裁定（P8 第六批，2026-10-02）

对应：§10.3-#26「进区触发器缺口」（#25 连带登记）独立批。本批闭环：**家族道 `_area_` 行的
进区触发接线落地**，绑定数据按真端权威校正。

## 1. 触发链取证（修正两次早期误判）

| 断言 | 早期登记 | 本批复核 |
|---|---|---|
| "MoveNew 链无对应物" | #26 初登记 | **错**：`ZoneInstance` → `PlayerController.onEnterZone` → `QuestEngine.onEnterZone` → `DataDrivenNativeRuntime.onEnterZone`（DD 侧含接取双角色 kind 6）已通 |
| "questAreasByWorld 零消费者" | #26 取证修正 | **错**：`RetailAreaEngine.onPlayerMoved`（MovementNotifyTask 移动管线 → RetailSensoryAreaEngine → RetailAreaEngine）按多边形判定进入区并 `QuestService.startQuest` 分发——访问器名 `getQuestAreas(worldId)`，首查 grep 未中 |
| 真缺口 | — | **坐实**：①整区滤网 `hasQuestTemplates`（typed 目录 allMatch）把含 native 行的区**整体静默丢弃**；②就算放行，`startQuest` 对 native 行在 `template.getNpcFactionId()` 处 NPE（typed 模板缺失） |

## 2. 绑定权威裁定（258 世界目录 × 三类区全量对拍）

真端绑定权威 = `world{,_M,_N}.xml` 的 `<questscript_area><name>/<quest>` 子元素
（`NpcScriptMgr_AddQuestArea` 的绑定源）。扫描 `sensory_area`/`item_use_area` 同步排除。
哨兵大小写**不敏感**（真端表原文即有 `_Area_` 形——首轮精确串比较漏 4 行，车道映射是对的）。

家族道 `_area_` 行全集 = **29 行**（SimpleTalk 8 + SimpleHunt 21）：

- **活 6**（真端有 questscript_area 绑定）：
  `12504→LDF5a_QuestArea_Q12504`（Talk）、`22504→LDF5a_QuestArea_Q22504`（Talk）、
  `12505/12524/22524→LDF5a_QuestArea_Q125xx`（Hunt）、`39005→InvadePortalDest_41_questArea_02`（Hunt，df2a）。
  ai-areas 几何与真端多边形逐值一致（float32 舍入内；QE-109 归一化的延续验证）。
- **死 23**（区名缺席或区在而无 quest 绑定 = 真端本征死边，沿 §10.3-#23 口径冻结，禁止补绑定）：
  13523-13525、16972、18003、18033、22505、23523-23525、26972、28003、28033、39007、39009、
  49005、49007、49009、99000、**13912/13913/23912/23913**（`_Area_` 驼峰四行）。

**ai-areas.xml 幻影绑定校正**（P0c-4「ai-areas = 真端镜像」前提被推翻）：清空 8 行的
quest_area 绑定（区定义保留、绑定置空，与既有先例 `LDF5a_QuestArea_Q22505 quests=""` 同形）：
IDLDF5_Under_01_QuestArea01 ×2（16972,26972）、IDLDF5b_TD_QuestArea_Q18033（18033,28033）、
InvadePortalDest_42_questArea_03（39007,39009）、GAb1_01/02/03/04_QuestArea（13912,23912 /
13913,23913）。观察登记：`InvadePortalDest_42_questArea_02 quests="49004"` 非家族行、真端
未见绑定——typed 行为不变，留待全表对拍批（不在 #26 范围）。

## 3. 接线（RetailAreaEngine.onPlayerMoved 按行分流）

```java
if (laneOf(questId) != null && lane.routes(questId) && lane.grantKind(questId) == AREA) {
    NativeQuestStartPort.instance().start(player, questId);   // native：CanAcquireQuest 同形全条件面
} else if (catalog.findMetadata(questId).isPresent()) {
    QuestService.startQuest(new QuestEnv(null, player, questId, 0));  // typed：原路
}   // 两者皆非：跳过不猜
```

- 去掉整区 `hasQuestTemplates` 滤网（onPlayerMoved 处；`supports()` 的 allMatch 属 AI 动态区
  支持面，不在本批）。
- native 判据 = `routes() ∧ grantKind == AREA`：`start` = 真端 `User_AddAreaQuest` → `AddQuest`
  前置 `CheckQuestAcquireCondition` 的同形（等级/种族/职业/性别/前置全条件面，§10.3-#25/QE-134
  同源证据）；**冻结行不建档**（禁止半接线）。
- DD 行不在 `NativeSystemGrantLanes`（laneOf 为空）且无 typed 目录 ⇒ 天然跳过（DD 进区接取走
  自己的 `NativeEnterAreaPort` 区名恒等链，两链无交叠）。

## 4. 门禁

- 新增 `RetailAreaEngineQuestAreaGateTest`：家族 AREA 行全集（29）== 车道装载；ai-areas 家族
  绑定 == 真端活集（双向）；活行绑定区名逐字 == 真端；活行 ⊆ 可路由面（触发面齐备由数据门界定）。
- `RetailSystemGrantDispatchTest.retiredAreaRowsStayBoundToQuestAreasAndCarrySystemGrantEdge`
  重锚：旧断言「所有退役 Hunt `_area_` 行必绑」锁的是幻影数据，改为双向（活 4 行必绑 / 死行
  必不绑）+ 发放面保持。
- 门态：三门 11/11；聚焦套件对 p8 第二刀基线 ADDED 0（gates/2026-10-02-focused-run-p8-cut6.log）。

## 5. 证据命令

```bash
python3 - <<'PY'   # 258 世界 × 三类区全量对拍（活 6 / 死 23）
PY
grep -n 'onPlayerMoved' src/main/java/com/aionemu/gameserver/taskmanager/tasks/MovementNotifyTask.java
mvn -o test -Dtest='RetailAreaEngineQuestAreaGateTest,RetailAreaEngineTest,RetailSystemGrantDispatchTest' -DfailIfNoTests=false
```
