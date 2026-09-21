# AI 反射注册启动门禁

## 变更

- `AI2Engine.registerAI` 检测不同类注册同一 `@AIName`；同一类重复注册仍视为幂等。
- `AI2Engine.validateScripts` 将缺失 NPC AI 引用从 warning 升级为启动失败。
- 启动时逐一调用所有已注册 AI 的无参构造器，聚合失败项后 fail-fast。
- 新增单元回归与生产目录合同测试，合同测试同时扫描编译后的 AI 类和 NPC 模板 `ai` 属性。
- 将 15 个历史被覆盖的变体 AI 注册键改为包名前缀键，保留当前字典序加载时的有效实现不变。

## 审计结果

```text
annotation_classes=819
annotation_names=819
npc_references=792
duplicate_names=0
missing_npc_references=0
```

历史重复键的处理原则：`CompiledScriptLoader` 按类名排序加载，旧 `HashMap.put` 的实际胜者分别是
`dragonLordRefuge`、`luckyDanuarReliquary`、`kromedesTrial`。这些原键保留；仅给已被覆盖的变体类增加
包名前缀键，避免改变既有运行选择。

## 验证

- 静态审计：`python3 .agents/summary/ai-registration-gate/audit_ai_registration.py`
- 审计输入包含 NPC 模板原始 `ai` 值、`NpcTemplate.getAi()` 的 `fearful_beast`/`siege_teleporter`
  派生值，以及代码直传的 `dummy`/`retail_pattern`/`retail_direct_portal`。
- IDE inspection：`AI2EngineRegistrationValidationTest` 与 `AI2ProductionRegistrationContractTest` 无错误。
- `git diff --check`：焦点文件通过。
- Maven 首轮：3 个单元断言失败，原因是测试环境无 Spring `MessageSource` 时 `I18n.get` 返回消息键；
  断言修正为检查对应错误键。
- Maven 复验：
  `mvn -Dtest=AI2EngineRegistrationValidationTest,AI2ProductionRegistrationContractTest test`
  结果 `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
