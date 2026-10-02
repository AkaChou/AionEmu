# §10.3-#4 裁定：80281/80283 按活动任务子系排除（P8 收口批，2026-10-02）

对应：§10.3-#4「`80281/80283` 缺 `reward_npc_name`——P8 go-live 前裁定：按活动任务子系排除，
或补真端交付声明」。**裁定 = 按活动任务子系排除**（fail-closed 保持，零代码变更）。

## 证据链（五点互证）

1. **活动任务子系**：真端 quest.xml 两行 `category1 = event`（dev_name「[이벤트][인던] 신무기
   성능 테스트」/「내구도 테스트」= 新武器性能测试/耐久测试——**内部测试任务**）。event 子系
   的驱动表 `event_quest.xml` 全域缺失（§10.3-#3 不可实证假设），交付面在真端由事件系统驱动，
   任务表本就不承载。
2. **停用形**：`minlevel_permitted = 999` 且 `client_level = 999`——真端与客户端一致的停用形
   （P5D 步 1 判例：停用形上的长尾缺口没有运行期影响）。
3. **交付声明缺失是真端本征**：quest.xml 与 Quest_SimpleHunt 表行均无 `reward_npc_name`
   （表内 135 行 event 同形——事件任务交付不写任务表）。
4. **接取不可达**：acquire = `IDEvent01_In_NPC_Da`（npc_id 831131），全 spawn 注册表零引用
   ——NPC 不存在，接取路径物理不可达。
5. **条件闸兜底**：若 NPC 被刷出，`NativeQuestStartPort.start` 按 quest.xml `minlevel 999`
   判 `LEVEL_BLOCKED`（接取第一条件面），仍 fail-closed。

## 生产现状与处置

- 两行不在 retention 台账（非 1467 切换集成员）、无 typed 定义（生产目录零载体）⇒ 生产零存在。
- **禁止**后续给这两行补 reward_npc_name 或刷出 831131 来"复活"它们——那会偏离真端
  （真端未交付声明 = 真端玩家同样不可完成）；若未来取得 event_quest.xml 或活动子系立项，
  按该子系单独立项（§10.3-#3 同口）。
- 泛化规则**否决**：`category1=event` 全表 1193 行（SimpleTalk 582 / SimpleHunt 135），其中
  含大量既有活行——一刀切按类别排除会伤及存量，排除面**只钉这两行的事实组合**
  （event ∧ minlevel 999 ∧ 无交付声明 ∧ 接取 NPC 未刷）。

## 门禁钉

`RetailSystemGrantDispatchTest.eventSubsystemTestRowsStayFailClosed`（本批新增）：
断言两行 quest.xml 停用形事实（category1=event ∧ minlevel 999）+ Hunt 车道无交付面
（`rewardNpc` 为空）——防未来数据"修复"让死行复活。
