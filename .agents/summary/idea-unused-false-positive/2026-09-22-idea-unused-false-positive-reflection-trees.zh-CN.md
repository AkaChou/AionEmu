# IDEA「类从未使用」在动态反射加载树上的误报（实测）

> 日期：2026-09-22　范围：`AionEmu-test`
> 环境：IntelliJ IDEA 2026.2（Ultimate，Spring 插件已启用，未在 `disabled_plugins.txt` 中）
> 结论一句话：**不要为了消掉这个告警去改运行时加载方式**；`@Component` 也不能消除类级告警，IDEA 认的是「注册点」，不是「Spring 反射」。

## 1. 现象

`MoveToNpc`（`commands/admin`）在 IDE 内被报 `类 'MoveToNpc' 从未使用` + `构造函数 'MoveToNpc()' 从未使用`，
但它确实是生效命令：`ChatProcessor.java:116` 经 `CompiledScriptLoader` 扫包 → `ChatCommandsLoader.java:40`
反射实例化 → `registerCommand`，别名 `movetonpc` 在 `commands.properties:90`（访问等级 3）。

## 2. 误报规模（本次实测，按 `CompiledScriptLoader.load` 的 4 棵反射树统计）

| 反射树 | 类总数 | 零外部静态引用（=IDEA 会报未使用） |
|---|---|---|
| `com.aionemu.gameserver.ai` | 828 | **746**（全部带 `@AIName`） |
| `commands/admin` | 153 | 64 |
| `commands/player` | 37 | 36 |
| `instance/handlers/scripts` | 118 | 63 |
| `world/zone/scripts` | 4 | 2 |

合计约 **911 个类**会被误报，说明这是工具链问题，不是个别类的疏漏。

## 3. A/B 实测（`idea get_file_problems`，探针文件用完即删）

| 实验对象 | 类级「从未使用」 | 构造函数「从未使用」 |
|---|---|---|
| 普通公开类（无注解，`com.aionemu.probe`） | 报 | 报 |
| 同上 + `@Component` | **仍报** | 不报 |
| `@Component` 且放在真实 Spring 组件包（`boot/lifecycle`） | **仍报** | 不报 |
| `@Component` + `ApplicationContext.getBeansOfType(ZzTargetProbe.class)` 类型化查找 | **不报** | 不报 |
| `MoveToNpc` + `META-INF/services/…ChatCommand` 清单 | 不报 | 不报 |
| 删除该 services 清单后 | 又报 | 又报 |

解读：
- Spring 构造型注解只让 IDEA 认可「可以由 Spring 实例化」，**类本身仍判定为未被使用**。
- IDEA 真正认的是**注册点/入口点**：`META-INF/services` 这类可解析的注册清单会被当作 entry point。
- 类型化查找（`getBeansOfType(X.class)`）同样被认作入口点，即「迁 Spring」确实能换来 IDE 静音——但仅对能做成单例的树成立，见下。

## 3.1 为什么不能整树迁移（实例化语义实测）

| 反射树 | 实例化点 | 语义 | 能否做成 Spring 豆 |
|---|---|---|---|
| `com.aionemu.gameserver.ai`（746 类误报，占 82%） | `AI2Engine.java:133` `aiMap.get(name).getDeclaredConstructor().newInstance()` + `setOwner(owner)` | **每个生物一个的有状态实例** | ✗ 单例会串状态；全加 `@Scope("prototype")` 则失去「简单豆」的识别优势 |
| `instance/handlers/scripts`（63） | `InstanceEngine.java:96` 同类 new | 每个副本实例一个 | ✗ 原型语义 |
| `world/zone/scripts`（2） | `ZoneService.java:120` 同类 new | 每个区域加载时一个 | ✗ 原型语义 |
| `commands/admin`+`player`（100） | `ChatCommandsLoader.java:40` 每个类实例化一次，状态仅 `aliases`/`aliasLevels`（注册期） | 单例、执行期无状态 | ✓ 唯一可行的一棵 |

即：约 911 个误报里，**只有 100 个（commands）在语义上允许豆化**；为消 11% 的告警去重构核心子系统，收益/成本比很差。

## 4. 建议

1. 首选（零运行时风险）：IDEA 侧配置 entry point —— `Settings → Editor → Inspections → Java → Declaration redundancy → Unused declaration → Options/Entry points`，
   把 4 棵反射树（或基类模式）加进去。注意 `.idea/` 已被 `.gitignore:11` 忽略，该配置只对本机生效，无法随仓库共享。
2. 仓库级守卫已存在，不需要靠 IDE：`CommandAliasRegistryTest`（别名 ↔ `commands.properties` 双向核对）+ memory-bank `SDJ-001` 的 4 棵包树白名单。
3. 若确实要迁 Spring：仅加 `@Component` 无效，必须让加载器以 IDEA 能解析的类型化查找取豆
   （如 `getBeansOfType(ChatCommand.class)`，已实测可消告警），否则类级告警照旧；
   且只应迁 `commands/*` 这一棵（见 §3.1），并需同步处理 `ChatProcessor.reload()`（当前整树重建）、
   `load(CountDownLatch)` 启动进度、`AggregatedClassListener`（`OnClassLoadUnloadListener`/`ScheduledTaskClassListener`）
   与 `CommandAliasRegistryTest` 的双向核对。属于架构级变更，不应只为消告警而做。
4. 现状并非「原始反射」：`CompiledScriptLoader` 已在用 Spring 的 `PathMatchingResourcePatternResolver`
   + `CachingMetadataReaderFactory`，与 Spring 组件扫描是同一套 classpath 扫描基础设施；换成 Spring 豆
   不会让加载更健壮或更快，只是把自有注册表换成容器，并失去整树热重建与三棵树的原型语义。

## 5. 未覆盖

- 只验证了 IDEA 的静态分析行为，未跑 Maven、未起服务器（AGENTS.md 规则 1/2）。
- 若走 Spring 方案，`reload()` 语义、启动进度与 `CommandAliasRegistryTest` 的改造量尚未做原型验证。
