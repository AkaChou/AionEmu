# AI 多别名注册与启动缺失异常修复

## 现象与根因

### 现象
服务端启动时 `InstantPool-3` 抛出异常：
`com.aionemu.gameserver.GameServerError: Can't initialize ai handlers.`
`Caused by: java.lang.IllegalStateException: NPC 模板引用了未注册的 AI 名称：quest_start_use_item`
随后 `world-spawner` 生成 NPC 时触发 NPE：
`ERROR [world-spawner] c.aionemu.gameserver.ai2.AI2Engine - AI 工厂出错：quest_start_use_item`
`java.lang.NullPointerException: Cannot invoke "java.lang.Class.getDeclaredConstructor(java.lang.Class[])" because the return value of "java.util.Map.get(Object)" is null`

### 根因
在 commit `c87d5393a4` 中，`QuestStartItemNpcAi2` 为对齐真端脚本别名，在注解中声明了多个 AI 名称：
`@AIName("quest_start_use_item,scroll_q41,scroll_q49,scroll_q2498,npc_ai_box_q1559,npc_ai_fobj_q11036a,npc_ai_fobj_q11123a,npc_ai_fobj_q11143a")`
但 `AI2Engine.registerAI` 此前直接取注解的完整字符串存入 `aiMap`，未对逗号分隔的别名列表进行拆分。
导致：
1. `aiMap` 中仅存在一个包含完整长字符串的复合 key，没有独立的 `"quest_start_use_item"` 及各真端别名；
2. NPC 模板引用的 `ai="quest_start_use_item"` 在 `aiMap` 中无法命中，`validateScripts` 抛出缺失异常；
3. `setupAI` 查找时返回 null 并抛出空指针异常；
4. `AbstractAI.getName()` 之前直接返回整串注解值，未按主名称或实际实例名称返回；
5. `AI2Engine.selectNpcAi` 的白名单未收拢全部别名。

## 修复措施

1. **`AI2Engine.registerAI` 支持多别名注册**：
   - 使用 `split(",")` 遍历拆分注解值，对各别名执行 `trim()` 并过滤空串；
   - 逐个校验重名冲突（同一类重复注册保持幂等，不同类注册同名 AI 抛出异常）；
   - 将各个别名分别注册到 `aiMap`。
2. **`AI2Engine.selectNpcAi` 保护所有别名**：
   - 提取 `SCRIPTED_ACTION_ITEM_AI` 常量集合，将 `quest_start_use_item` 及所有别名纳入保护，防止被零售 pattern 覆盖掉物品交互协议。
3. **`AbstractAI` 记录并返回有效 AI 名称**：
   - 增加 `aiName` 属性并在 `setupAI` 时按所选名称赋予实例；
   - `getName()` 优先返回 `aiName`，未指定时取注解中逗号前的第一个主名称并 `trim()`，杜绝返回逗号字符串。
4. **`AIName` 文档补充**：
   - 明确标注支持逗号分隔声明别名，移除过时文件头注释。
5. **审计脚本对齐**：
   - 更新 `.agents/summary/ai-registration-gate/audit_ai_registration.py` 支持逗号别名拆分。审计结果：819 类，826 名称，792 模板引用，0 缺失，0 重复。
6. **单元测试补充**：
   - `AI2EngineRegistrationValidationTest` 补充多别名注册、跨类别名冲突检测、实例名称返回测试；
   - `AI2EngineRetailSelectionTest` 补充真端别名免受 retail pattern 覆盖断言；
   - `QuestStartItemNpcAi2Test` 补充引擎按各个别名正常实例化断言。
