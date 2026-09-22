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

## 三轮：用户 IDEA 批量 quick-fix 的检查与提交（2026-09-21 深夜）

- 用户在 IDE 内对约 1457 个 Java 文件执行 Fix all（lambda/diamond/switch/集合 API/行尾空白等）。
- **批量转换的踩坑与修复**：「删除冗余花括号」类修复摘掉了 switch case 块的花括号，case 局部变量作用域合并 → 32 处「变量已在方法中定义」编译错误（10 文件）。按错误行定位所在 switch、对声明局部变量的 case 块恢复花括号（49 处）+ EnergyBuff 外层 case 手工补括号；PlayerInfo.java 结构受损回退至 HEAD（其 quick-fix 收益待重跑）。
- 提交 `06e33cee8`（一轮 lint 修复）与 `b6e6957dd`（三轮 quick-fix 波次，1455 文件）；`QuestDialogPage.java` 为用户 WIP（增强 switch 降级实验）未纳入，非 Java WIP 一律未动。
- 经验（可复用）：IDEA「Fix all」对「冗余花括号」的判定在 case 块含局部声明时会误删；跑完批量 quick-fix 后必须全量编译，再按错误行恢复 case 括号。

## 四轮：批次 A 空体语句分诊（2026-09-22 凌晨）

- 新写空体检测器（注释/字符串掩码 + 括号配对）扫出当前代码 190 处（SARIF 已两轮过期，不可复用行号）。
- 六类处理：AI2 守卫反转（~70）、CAS 裸语句（~30）、副本计数空链删除（~40，`==N` 互斥才安全）、服务死 if（14）、副作用条件转裸调用（6）、注释占位清理（~15）。
- **关键纠错（脚本化批量修改的教训）**：
  1. 通用「空 if 删除」误删了 3 处**物品消耗调用**（条件副作用）——AnimationAddAction/AssemblyItemAction/ItemUseAction，已恢复为裸调用；
  2. 空链分支删除对**条件重叠**链不健全——Outpost/Base 丢 `getFlag()==null` 守卫会 NPE、CM_FRIEND_ADD 满员分支使拒绝失效，均已修复（后者顺势补全 TARGET_LIST_FULL 拒绝，上游缺失逻辑）；
  3. 链尾删除模式会吞前分支闭合括号（PlayerCommonData/CM_HOTSPOT_TELEPORT 语法破损，编译兜底抓出后修复）。
- 有意保留：测试 while 游标推进 ×3、do-while 尾 ×4、ThreadUncaughtExceptionHandler OOM 预留点。
- 提交时用户暂存区有其 ai-registration-gate WIP，首次提交误卷入，`reset --soft` 重做剔除并恢复其暂存；**提交前必须核对 `git diff --cached` 仅含本任务文件**。
- 待办批次：B+C 机械大批量（JavadocBlankLines 1515 / Size→isEmpty 146 / FieldMayBeFinal 70 / 残余 import）、D 算术缺陷 ~40、E NPE 分诊 310；用户并行重构（XmlDataLoader 包移动）落定后再跑全量编译。

## 五轮：批次 B+C/D（2026-09-22）

- **B 悬空 Javadoc 参数回挂**：495 行/141 文件提交。首版曾误将双语描述挂成 @param（1082 文件全回滚），严格门控重做（段前必须真空行分隔 + 无同名 @param + 非句末标点 + 行数==形参数），抽样 6/6 正确。经验：javadoc「空白行将被忽略」警告的真根因是悬空参数行，修复悬空即消除。
- **C size()==0→isEmpty() 整体放弃**：正则类型盲改在 ByteArrayOutputStream/NpcSkillList 等 5 处编译报错（无 isEmpty 方法），该类别必须交给 IDEA 类型感知修复。
- **D 恒等算术包装移除**（5 文件提交）：ceil/floor/round(int) 恒等、`<< 0`/`>> 0`、`+ +` 手滑。EncryptionKeyPair 正则曾吞外层括号，lint 抓出修复。Stats/CmdAttrBonus 的同类移除混入用户 Lombok 改造，随用户批次走。
- **数值行为类待拍板清单**（int 截断后才 round/乘浮点，修复=数值变化）：
  1. `PvpService` AP 分配 `Math.round(baseApReward * dmg / totalDamage)`（全 int 截断，修复后分配略增）
  2. `NpcController` PvE AP `Math.round(baseApReward * percentage / players.size())`
  3. `XPLossEnum` 死亡经验损失 `Math.round(expNeed / 100 * param)`
  4. `Stats`/`CmdAttrBonus` GM 属性百分比 `(stat * modifier) / 100`
  5. `FortressAssault` 围攻阵型 `amount / 2`（float 上下文，奇数偏移 0.5）
  6. `LadderService` 1100-1105（SARIF 行号漂移未定位）

## 验证状态

- 已执行：IDEA MCP 实时检查，全部被改文件 0 error；目标警告按文件抽查确认消失。
- 已执行（用户授权）：IDEA `build_project` 全量编译 **成功**（三轮后含 test 源），仅存量 `QuestDialog` deprecation 警告；批次 A 因用户并行重构（XmlDataLoader 包移动中）改用 108 文件逐批 lint 验证 0 error。
- 未执行：单测（如需请另行授权指定范围）。

## 六轮：注释规则全仓清零（2026-09-22）

- 范围：DanglingJavadoc / JavadocDeclaration / JavadocBlankLines + 重复 @param（main+test 5788 文件全扫）。
- 工具链：`comment_wave_detect.py` 静态检测（javadoc 块解析 + 悬空判定 + 标签合法性）→ `comment_wave_fix.py` 按块修复（逐行内容校验，backup/ 全量原状备份）→ 检测器复检 → IDEA lint 抽样 → build_project 全量编译。
- 结果：3941 文件修改（17,926 行操作）：删悬空/仅标签垃圾块 370、悬空转行注释 3、块内空白行清理 28,582 行；非法 `@param`/`@return` 转文本或删除（PlayerAppearance 60、IStorage 整文件空标签接口块等）；重复 @param 5 处（含 Matrix4f.fromFrustum near/far/right/left 标签错配修正）；BoundingBox/BrokerItem 多变量声明中夹带的字段 Javadoc 拆分独立声明（语义等价）；SocialService/BlockListDAO 残句清理。
- **误报教训（可复用）**：
  1. 悬空判定不能只看花括号深度——嵌套类成员 Javadoc 深度≥2 是合法的；必须看"块后第一个有效代码行是否为声明"；
  2. 跳过注解时必须连同其跨行括号参数一起跳过（`@EnableConfigurationProperties({...})` 的参数行会被误判为悬空位置，首轮因此误删/误转 70+ 块，已从 backup 全量恢复重跑）；
  3. 字段带 `new Foo()` 初始化器时 `extract_params` 会把初始化器括号当参数表——`=` 先于 `(` 即字段；
  4. 变长参数 `String...params`（无空格）需特殊拆名；类/方法级 `@param <T>` 类型参数是合法标签；
  5. 行尾裸枚举常量（无逗号/分号，如 `Never`）是合法声明位置。
- 排除未动：quest 并行任务文件（QuestDefinitionXmlCompiler/QuestXmlBlockExpander/questEngine 测试/QuestBClassRouteContractTest 新文件）、package-info。
- 验证：检测器复检仅剩排除文件 2 条；IDEA lint 抽样 12 文件零注释告警；build_project 全量编译成功（仅存量 DataManager Thread.getId() deprecation 警告，与本轮无关）。
- 有意保留：`//` 风格内容的类级 Javadoc（AionBootApplication 等，IDEA 不告警）、WEAK_WARNING 级 CommentedOutCode（另行批次）。
