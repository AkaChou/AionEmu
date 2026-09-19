# Quest 30721 接取失败：check_item 被误当成 inventory_item_name

## 症状

玩家 `Ww` 在 NPC `804704` 的任务列表页 `10` 点击任务 `30721` 后进入
`SELECT_NONE(4762)`，客户端随后发送：

```text
CM_DIALOG_SELECT 玩家=Ww npcId=804704 targetObj=69871 questId=30721 上一页=4762 动作=20000
```

`20000` 是 `QUEST_ACCEPT_SIMPLE`。该动作被任务引擎拒绝后，
`DialogService` 按“未处理的任务动作不得回显为页面”的规则关闭窗口：

```text
SM_DIALOG_WINDOW 玩家=Ww targetObj=0 questId=0 下发页=0
```

## 根因

`30721.xml` 的 `metadata/inventory-items` 声明了 `182215698`（Sedative）。
`NPC_START` 展开出的 `QUEST_ACCEPT_SIMPLE(20000)` 转换带有
`<start-eligible/>`，而 `PlayerQuestStartEligibilityPort` 会把
`metadata.inventoryItems()` 当作接取前置；玩家接取前没有该道具，因此返回
`REQUIRED_INVENTORY_ITEM_MISSING`，引擎不提交 `unaccepted -> s0`。

该道具实际是任务中段 `804868` 交给玩家、随后由 `ITEM_PLAY` 消耗的任务道具：

```text
s1 -> s2: give-item 182215698 x1
s2 -> s3: item-play 182215698, remove-item 182215698 x1
```

真端 `Quest_unpacked/quest.xml` 的 `30721` 只有
`check_item1_1 = quest_30721a 1`，没有 `inventory_item_name*`。
私有 `quest_data.xml` 先前把 retail `check_item` 填进了
`inventory_items`，与 `inventory_item_name`（真正的接取携带道具）混淆。

## 修复

- `src/main/resources/aion/data/static_data/quest_definition/quests/30721.xml`：
  删除 `metadata/inventory-items` 中的 `182215698`。
- `src/main/resources/aion/data/static_data/quest_data/quest_data.xml`：
  同步删除 quest `30721` 的对应 `inventory_items` 行，保持迁移来源一致。

任务中段的 `give-item` / `item-play` / `remove-item` 合同保持不变。

## 静态验证

```text
xmllint --noout --schema .../quest_definition.xsd .../quests/30721.xml
  -> validates
xmllint --noout --schema .../quest_data.xsd .../quest_data.xml
  -> validates
```

补充 Python 断言：

- `30721.xml`：`inventory-items = 0`；
- `give-item` / `item-play` / `remove-item` 三者仍包含 `182215698`；
- `quest_data.xml`：quest `30721` 的 `inventory_items = 0`。

## 单元与门禁验证（2026-09-19，用户授权测试）

```text
mvn -B -Dtest=QuestInventoryStartItemGateTest test
  -> Tests run: 2, Failures: 0, Errors: 0

mvn -B -Dtest=QuestInventoryStartItemGateTest,QuestItemSourceContractGateTest,QuestRetailStartMetadataGateTest,QuestStartEligibilityContractTest,PlayerQuestStartEligibilityPortTest,QuestEnterZoneStartOwnerRegressionTest test
  -> Tests run: 32, Failures: 0, Errors: 0

mvn -B -Dquest.client.contract.failOnStaleBaseline=true -Dtest=QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestClientContractGateTest,QuestDialogOrderAuditTest,QuestPageButtonAuditTest test
  -> Tests run: 31, Failures: 0, Errors: 0
  -> PRODUCTION_COMPILE_OK=6189 / PRODUCTION_COMPILE_FAILURES=0 /
     PRODUCTION_INTERACTION_OBJECT_FAILURES=0 / PRODUCTION_WHITELIST_VIOLATIONS=0
```

全包扫描的附带结论（失败与本任务无关，工作区存在并行 in-flight 改动）：

```text
mvn -B -Dtest='com.aionemu.gameserver.questEngine.**.*Test' test
  -> Tests run: 1458, Failures: 0, Errors: 13
     · 7 个 NoClassDefFoundError 是并行 IDE/Maven 重建 target/test-classes 的类装载竞争：
       涉及 5 个测试类单独重跑 24/24 全绿（mvn-classload-probe.log）
     · 6 个 NullPointerException（QuestTransition.sourceNode() 为空）集中在
       Quest25512ClientDialogAlignmentTest / QuestLegacyMonsterHuntProductionFlowTest；
       把本任务两处改动还原到 HEAD 后同样复现（mvn-head-baseline-probe.log），
       属工作区并行改动引入，不由本任务引入
```

原始日志：`.agents/summary/quest-inventory-start-item/mvn-*.log`

## 未验证边界

- 未启动、停止或重启服务端；未做真实客户端复验（需要用户重建资源并重启服务端后实测）。
- IDE 运行模式使用 `target/classes` 中的旧资源；需要用户自行重建/重启后，
  旧资源才会被替换。服务端生命周期由用户管理。
