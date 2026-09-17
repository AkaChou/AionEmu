# 2026-09-18 任务 51022 商团货物箱子 ACTION_ITEM_USE 启动门禁修复

## 1. 故障现象

服务端启动时 `QuestEngine.load` 抛出：

```text
quest 51022 quest_use_item catalog drop npc 701470 item 182215183 collecting step 0
has no matching START ACTION_ITEM_USE eligibility route
```

## 2. 根因

提交 `552e80f5c` 为 `51022` 恢复了真端 `Event_Cargobox` 掉落：

```xml
<drop npc-id="701470" item-id="182215183" chance="100" each-member="true" collecting-step="0"/>
```

但 `701470` 的 AI 是 `quest_use_item`。启动校验器要求每个
`quest_use_item` 目录掉落都存在匹配的 `START + ACTION_ITEM_USE` 资格路由，
否则交互物无法通过 `CanAct` 门禁，掉落也不会触发。

`51022` 原先只缺失落行，因此没有该资格路由；恢复掉落后，启动门禁立即暴露这一缺口。

## 3. 全库定性

按 `QuestInteractionObjectValidator.validateCatalogDrops` 的同一判定口径扫描生产任务：

- `quest_use_item` NPC：804 个；
- 修复前缺失资格路由：仅 `51022`；
- 修复后缺失资格路由：0。

## 4. 修复

文件：

```text
src/main/resources/aion/data/static_data/quest_definition/quests/51022.xml
```

在 `started` 状态补充无副作用的资格自环：

```xml
<transition source="started" target="started">
  <event>
    <can-act template-id="701470" action-type="ACTION_ITEM_USE"/>
  </event>
</transition>
```

该路由只允许交互物使用并触发原有 `quest_use_item` 掉落，不改变 `var0`、任务状态或对话页面。

## 5. 验证

已执行：

```bash
xmllint --noout src/main/resources/aion/data/static_data/quest_definition/quests/51022.xml
mvn test -Dtest=QuestInteractionObjectCatalogTest
```

结果：`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`，构建成功。

`git diff --check` 无告警；同口径全库静态扫描为 0 缺口。

## 6. 后续门禁

`QuestInteractionObjectCatalogTest` 必须纳入任务掉落或交互物修复后的 focused test 集。
上一批只跑了掉落/编译门禁，未执行该测试，因此 `51022` 的启动阻断逃逸到运行时。

## 7. 门禁前置（2026-09-18 补充）

为避免以后再次依赖“记得单独跑某个测试类”，已将同一条校验逻辑接入
`ProductionCatalogWhitelistVerificationTest`：

```text
QuestInteractionObjectValidator#validateDefinition
  -> 启动时由 QuestEngine.prepareProductionDefinitions 调用
  -> 生产目录门禁由 ProductionCatalogWhitelistVerificationTest 逐任务调用
```

该门禁新增输出：

```text
PRODUCTION_INTERACTION_OBJECT_FAILURES=0
```

验证命令与结果：

```bash
mvn test -Dtest=QuestInteractionObjectCatalogTest,ProductionCatalogWhitelistVerificationTest
```

`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`；
`PRODUCTION_COMPILE_OK=6189`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。

## 8. 验收边界

未启动、停止或重启服务器；部署本次 XML 后需由用户确认 `QuestEngine` 启动日志恢复正常。
