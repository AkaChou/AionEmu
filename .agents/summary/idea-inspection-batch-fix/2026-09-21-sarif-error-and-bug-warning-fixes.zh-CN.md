# IDEA 检查报告批量修复（2026-09-21）

来源：`report_2026-09-21_09-18-16.sarif.json`（17,812 条结果）。验证方式：IDEA MCP 实时检查（`get_file_problems` / `lint_files`），按项目规则未执行 Maven 构建/测试。

## 一、错误级（76 条 → 全部闭环）

| 类别 | 数量 | 处理 |
|---|---|---|
| JavadocReference | 69 | 修复（见下） |
| WrongPackageStatement | 1 | `git mv` SMSkillCooldownTest 至 `network/aion/serverpackets/`（与包声明一致） |
| InconsistentResourceBundle | 5 | SARIF 过期：`messages.properties` 报告生成后已改，实时检查为 0 |
| Annotator（Tutorial.xml 无根） | 1 | 包 `<global_rules>` 根元素（文档文件，不被 static_data.xml 引用） |

JavadocReference 根因三类：
1. DAO `supports()` 参数名与 javadoc 不匹配（60 条）：22 个文件统一为接口规范名 `(String database, int majorVersion, int minorVersion)`（变体 `s,i,i1` / `arg0,arg1,arg2` / `db` / javadoc 侧 `databaseName`、重复 `@param arg0` 行）。另 33 个文件参数名本为 `databaseName` 可解析，未动。
2. `{@link}` 签名错误：`Area.java` `(int,int)`→`(float,float)`、`isInsideZ(int)`→`(float)`；`QuestTimers` `#lock`→`#questTimerLock`；`QuestStatePort` 补 `QuestState` import；`PlaceableHouseObject` `{@link LimitType.NONE}`→`{@code}`。
3. `@param` 名称与形参不符（散点 15 处）：InstanceService×3、QuestEngine、CM_HOUSE_EDIT、三个 Controller 的 onDialogSelect（裸行补 `@param`、删幽灵 `unk`）、AtreianPassportService、Reload、GeneralNpcAI2、SecretMunitionsFactoryInstance、BrokerService `TotalBuyPrice`。

## 二、警告级缺陷类（约 30 条修复，均经 IDEA 实时检查确认消除）

- **死循环**：`TwoTeamBg.onDie` / `TwoTeamSmallBg.onDie` 空 for 循环从不调用 `iterator.next()`，killer 有组时必挂起 → 删除（体为空，纯死代码）。
- **恒假比较**：`TrapNpcAI2` 5 个大写字面量与 `toLowerCase()` 后的 ownerName 比较永不命中 → 小写化（EV_RA_N_*、Highdeva_Fire_*、IDEvent_*）。
- **恒真/自比较**：`AbyssLandingService` `estate.equals(estate)` 空块删除；`WrappingAction` 品质自比较 → 与 `parentItem`（包装纸）品质比较。
- **除零（未修，待决策）**：`EnchantService.enchantItem` 的 `qualityCap` 自上游导入起从未赋值（始终 0），`levelDiff*3f/qualityCap` 得 Infinity ⇒ 强化几乎必成。**涉及强化概率平衡，常数不可臆造，需运营侧给出各品质 cap。** git 全历史无旧赋值可恢复（911440146 导入即缺失）。
- **Map 覆盖**：`Battleground.aliases` 重复键 `"[Team VS Team]"` → 删除被覆盖的 `TwoTeamBg` put（保留现有生效映射，`getAliases()` 无外部调用方）。
- **死代码**：`BIHTree.comparators` 只写不读（仅注释引用）→ 删字段+static 块；`BrigadeGeneralLaksyakaAI2.isHome` 只写不读 → 删；`PetService` 单次假循环 `for(;;)` 解包（`break` 语义转 else 分支，控制流等价）。
- **冗余 catch-rethrow**：`TemporarySpawnEngineTest`、`QuestDefinitionCatalogManifest`（QCE 非 IOException，传播路径不变）。
- **格式/字面量**：`NpcMoveController` `String.format` 误用 SLF4J `{}` → `%d`；`DarkPoetaInstance` `(byte) 05`→`5`；`DropDistributionService` `0xFFFFFFFF`→`0xFFFFFFFFL`（`writeD((int)luck)` 线上字节不变）；`QuestService` `dialogId==23 && dialogId!=0`→`dialogId==23`；`Account`/`BannedIP` 双重否定展开。
- **equals 契约**：`WeatherService.WeatherKey.equals` 补 `instanceof` 守卫（原强转对外类型/null 抛 CCE）。
- **测试修正**：GameTimeTest 参数序/不兼容类型断言、QuestRuntimeInfrastructureTest `getAsInt`→`orElseThrow`、NpcMoveControllerPathTest 自比较断言删除、QuestE2e `assertNotSame` 参数序、GeoWorldLoader `assertEquals` 参数序。

## 三、有意不修（语义敏感/环境噪音）

- `NonAtomicOperationOnVolatileField`×3（ChatServer/LoginServer/MapRegion）：并发语义重构，需单独立项。
- `BusyWait`×9（重连/PingPong/测试轮询均为有意模式）、`InstantiationOfUtilityClass`×4（Spring `@Bean` 工厂）、测试中默认 run 的 Thread×8。
- 风格类大批量积压（Convert2Diamond 2752、Convert2Lambda 1960、UNUSED_IMPORT 1774、JavadocBlankLines 1515 等）：适合 IDEA 端批量 quick-fix，另行处理。
- IDE 环境噪音：DAO 层"无法解析表"（需挂数据库数据源），非代码缺陷。

## 追加修复（2026-09-21 二轮排查）

- **裂隙注册表泄漏**（原 WriteOnlyObject RVController:213 线索）：`RiftManager.rifts` 只在生成时 `add`，全仓库无 remove；`RVController.onDelete` 对 `getSpawned()` 返回的**快照副本**做 `remove` 是无效操作 ⇒ 已消失的裂隙 NPC 永久滞留注册表。修复：新增 `RiftManager.removeSpawned(Npc)`（直接操作 CopyOnWriteArrayList），`RVController` 改调用之。IDEA 复检 0 error。
- **批量 quick-fix 可行性结论**：JetBrains MCP 无 quick-fix 应用类工具（execute_tool 探测已枚举全部工具）；但 `lint_files`(warning) → 按行号确定性修改 → 复检 的模拟链路已验证（CM_AUTO_GROUP 12 条 UNUSED_IMPORT 删除后 0 error）。

## 批量清理：main 源 UNUSED_IMPORT（2026-09-21 二轮）

- 流程：SARIF 行数据（含行内容）→ 行号+内容双校验删除 → git diff 复核"被删符号是否仍被使用"→ 可疑文件恢复 → MCP 错误级复检。
- 结果：1025 个文件删除 1688 条未使用 import（另含演示文件 CM_AUTO_GROUP 12 条）；其中 9 个文件在 SARIF 生成后当天又被改动、用量已恢复，自动回补 10 条 import（CronService 1 条为 FQN 撞词误报，已再次移除）。
- 已知噪音：IDE 未挂数据库时 DAO 层"无法解析表"错误属环境噪音，非代码缺陷。
- 经验（可复用）：SARIF 是快照，工作区同日有改动时"未使用 import"类结论会过期，批量删除必须配合"删除符号仍被使用"的 git diff 复核；字符串字面量与 FQN 尾段（`a.b.Logger`）会撞词，需剔除字符串与注释后再判定。

## 验证状态

- 已执行：IDEA MCP 实时检查，全部被改文件 0 error；目标警告按文件抽查确认消失。
- 已执行（用户授权）：IDEA `build_project` 全量编译 **成功**，仅 12 条 `QuestDialog` deprecation 警告（任务分支进行中的 QuestDialogAction 迁移所致，与本批修复无关）。
- 未执行：单测（如需请另行授权指定范围）。
