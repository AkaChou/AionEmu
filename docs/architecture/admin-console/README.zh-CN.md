# AionEmu 管理控制台架构说明

> 版本：2026-10-26 · 第一批范围：`config/administration/`
> Version: 2026-10-26 · First batch scope: `config/administration/`

管理控制台是 AionEmu 的运维入口：直接编辑服务器配置文件，改动即时热应用到运行中的游戏服，无需重启。

The admin console is the operator entry point of AionEmu: it edits the server configuration files directly and
hot-applies the change to the running game server without a restart.

---

## 一、进程拓扑 (Process Topology)

控制台以**嵌入式 Tomcat** 形态与游戏服同进程、同 JAR 运行。

The console runs as an embedded Tomcat **inside** the game server process, sharing one JAR.

| 决策项 | 结论 | 理由 |
|---|---|---|
| 进程拓扑 | 嵌入式 Tomcat，同进程 | 热重载可直接触达游戏服内存状态（静态配置字段、服务缓存）。独立进程需经文件+IPC 才能生效。 |
| 单入口约束 | 保持全仓唯一 `public static void main` | `AionBootApplicationTest.productionSourcesExposeOnlyBootMain` 钉死了该不变量。 |
| 启动判定 | 在 `main()` 中依据 `config/web/web.properties` 决定 | `SpringApplicationBuilder.web(...)` 的取值早于 `Environment` 成形，届时 `aion.config.dir` 尚未写入系统属性。 |
| 静态资源 | `web/dist` 经 `<resources>` 映射到 `target/classes/static` | 不使用外部进程调用插件（被 `AionBootApplicationTest` 钉死），产物不入版本库；`<resources>` 声明让 IDE 增量构建也同步产物。 |
| 静态资源兜底 | classpath 无产物时把磁盘 `web/dist` 加入 `spring.web.resources.static-locations` | IDE 直接以 `target/classes` 为 classpath 启动时增量构建可能尚未拷贝产物，回退到磁盘目录避免控制台页面 404。 |

**Web 层开关**：`config/web/web.properties` 的 `web.enabled = false` 时完全不启动 Tomcat，游戏服行为与未接入
控制台时逐字节一致。

**Web layer switch**: with `web.enabled = false` in `config/web/web.properties`, no Tomcat starts at all and the
game server behaves exactly as before.

**`web-application-type` 的唯一决定权**：`application.yml` **不得**出现 `spring.main.web-application-type`。
该键会在环境绑定阶段（`SpringApplication.bindToSpringApplication`）覆盖 `SpringApplicationBuilder.web(...)`
的取值，把 Servlet 上下文改回非 Web 上下文，Tomcat 永不启动。该决定权只属于
`AionBootApplication.adminConsoleWebApplicationType()`。

**`web-application-type` has a single owner**: `application.yml` must not carry
`spring.main.web-application-type`. The key is rebound during the environment binding stage
(`SpringApplication.bindToSpringApplication`), which overrides the `SpringApplicationBuilder.web(...)` value and
turns the servlet context back into a non-web one, so Tomcat never starts. The decision belongs solely to
`AionBootApplication.adminConsoleWebApplicationType()`.

---

## 二、模块边界 (Module Boundaries)

```text
src/main/java/com/aionemu/boot/
├── config/
│   ├── AionConfigFilePropertySource.java                  磁盘配置源：裸键直读配置文件
│   ├── AionConfigFilePropertySourceEnvironmentPostProcessor.java  注册磁盘配置源
│   ├── AionConfigSourceRefresher.java                     运行期重建配置源（改盘后调用）
│   └── AionLegacyPropertySourceEnvironmentPostProcessor.java  带前缀的遗留键镜像
└── web/
    ├── AdminConsoleSettings.java          控制台设置（开关/地址/端口/凭据/会话）
    ├── AdminConsoleConfigDir.java         配置目录解析
    ├── AdminConsoleEnvironmentPostProcessor.java  web.port → server.port 翻译
    ├── AdminConsoleStaticAssetsEnvironmentPostProcessor.java  前端产物静态资源兜底
    ├── AdminConsoleSessionService.java    凭据校验与会话生命周期
    ├── AdminConsoleAuthenticationFilter.java  API 鉴权
    ├── AdminConsoleController.java        REST API
    ├── AdminConsoleConfiguration.java     Spring 装配
    └── config/
        ├── ConfigGroup.java               一级菜单分组
        ├── EditableFile.java              可编辑文件描述符（注册式）与热应用目标
        ├── MainConfigFiles.java           main/ 目录的文件到配置类映射
        ├── PropertiesDocument.java        行级保留的 properties 文档模型
        ├── ConfigCommentParser.java       键上方的双语注释块解析
        ├── ConfigKeyComment.java          键的备注（说明与默认值）
        ├── EffectiveValueResolver.java    生效值与覆盖来源解析
        ├── ValueSource.java               生效来源枚举
        └── AdminConfigService.java        读写、差异预览、失败回滚
```

前端（`web/src/`）：

```text
web/src/
├── api/           API 客户端与类型定义
├── components/    控制台视图（分组菜单、键值表、差异面板、登录视图）
└── i18n/          界面文案字典与语言上下文
```

---

## 三、分组菜单与国际化 (Group Menu and Internationalization)

### 分组模型 (Group Model)

控制台菜单是**两级**的：一级是配置分组（`ConfigGroup`），二级是该分组下的可编辑文件。

| 分组 | 一级菜单 | 覆盖范围 |
|---|---|---|
| `administration` | 管理配置 | 首批六个文件 |
| `main` | 主服配置 | `main/` 目录下全部 44 个 `.properties` |
| `shop` | 商城管理 | 商城开关与商城限制（与「主服配置」共享） |

**分组与目录解耦**：一个分组可以覆盖多个目录，一个文件也可以同时归属多个分组。商城开关既出现在
「主服配置」也出现在「商城管理」下，正是靠这一点实现的 —— 分组是**菜单视角**，不是**目录视角**。

**描述符是注册式的**（`EditableFile` 不再是枚举）：新增配置目录只需注册新的描述符，既有代码不必改动，
菜单因此可以随配置范围一起扩展。

**主服文件的配置类映射**：`main/` 下每个 `.properties` 都映射到它喂给的那个配置类
（`rates.properties` → `RateConfig`，`custom.properties` → `CustomConfig`，……）。映射由「文件里的键」与
「配置类 `@Property` 声明的键」求交集得到，因此新增配置文件时必须同时补上它的映射，否则该文件不会出现在
控制台里 —— `ConfigGroupTest.mainGroupCoversEveryMainFileWithAConfigClass` 钉死了这条不变量。

**热应用仍然是定向的**：改 `rates.properties` 只重跑 `RateConfig` 的字段装配，不触碰其它配置类，也不触发
副本、攻城与日常调度等二十余个调度器。装配走 `ConfigurableProcessor.process`，它是纯字段赋值，没有副作用。

**`main/vip.properties` 是例外**：它虽放在主目录，却由登录服读取（属性来源是 `login/` 加 `network/` 加本
文件的组合），因此走专用的 `LOGIN_VIP_CONFIG` 目标，而不是通用的主目录装配。

### 国际化 (Internationalization)

界面文案全部走前端字典（`web/src/i18n/dictionaries.ts`），**默认中文**，右上角可即时切换英文，选择持久化在
`localStorage`（键 `aion-console-language`），并同步写入 `document.documentElement.lang`。

**键的备注**不另建字典：配置文件里的注释块本身就是双语的（`# GM 面板 / GM Panel`），后端解析后随每个键一起
返回（`comment.description` / `comment.descriptionEn` / `comment.defaultValue`）。界面按当前语言选取对应的一侧，
两侧都缺时显示占位符。这样备注与文件内容永远一致，不会出现「翻译与文件脱节」。

---

## 四、配置真相模型 (Configuration Truth Model)

### 4.1 优先级链 (Precedence Chain)

从高到低：

1. 命令行参数
2. JVM 系统属性
3. 操作系统环境变量
4. `application.yml`
5. 运维覆盖层（`mygs.properties` / `myls.properties` / `mycs.properties`）
6. 被编辑的配置文件
7. 配置类声明的默认值

控制台对每个键展示**文件值**、**实际生效值**与**覆盖来源**三层信息。运维改了文件却看不到变化时，能立刻分辨
是被覆盖了还是改错了。

The console shows three layers per key: the file value, the effective value and the override source. An operator
who edits a file and sees no change can immediately tell whether it was overridden or mistyped.

### 4.2 裸键来源的改造 (The Raw-Key Source Change)

**改造前的缺陷**：`AionLegacyPropertySourceEnvironmentPostProcessor` 在启动时把全部 `.properties` 快照进
`Environment`，而遗留装配器 `ConfigurableProcessor` **优先**查询 `Environment`，于是 `Config.reload()` 从磁盘
重读到的值永远用不上。结果：**手改配置文件后执行 `//reload config` 对任何 `gameserver.*` 键都无效**。

**改造后的结构**：

| 键形态 | 承载者 | 理由 |
|---|---|---|
| 裸键（`gameserver.administration.gmlevel`） | `AionConfigFilePropertySource`（磁盘直读） | 改盘后立即可见，重载真正生效。 |
| 带前缀键（`aion.legacy.game.property.*`） | `AionLegacyPropertySourceEnvironmentPostProcessor` | Spring Binder 的 `@ConfigurationProperties` 绑定与 `mygs` 覆盖链依赖它。 |

**跨服务冲突剔除**：扁平命名空间无法同时表达两个服务对同一键的不同取值（如登录服与游戏服的
`database.url`）。磁盘配置源按服务分组读取，跨服务取值不一致的键一律不发布，交由各服务自己的加载器读取本地
文件；取值一致的键（如 `database.password`）照常发布。

### 4.3 行级保留的写入 (Line-Preserving Writes)

配置文件里的双语注释块（`# 默认值: 3 / Default: 3`）承载运维语义。控制台**绝不整表重写**：
`PropertiesDocument` 以「行」为单位建模，只替换值发生变化的那一行，其余行逐字节不变。

---

## 五、热应用与回滚 (Hot Apply and Rollback)

热应用是**定向**的，只重跑被改文件对应的动作，绝不调用完整的 `Config.reload()`（那会连带刷新副本、攻城与
日常调度等二十余个调度器）。

| 文件 | 热应用动作 |
|---|---|
| `admin.properties` | 重跑 `AdminConfig` 装配 |
| `developer.properties` | 重跑 `DeveloperConfig` 装配 |
| `panel.properties` | 重跑 `PanelConfig` 装配 |
| `commands.properties` | `ChatProcessor.reload()` |
| `pff.properties` | `PacketFloodFilter.reload()` |
| `item.restriction.txt` | `AdminService.reload()` |
| `main/*.properties` | 重跑该文件映射到的**单个**配置类装配（如 `rates.properties` → `RateConfig`） |
| `main/vip.properties` | 重跑登录服的 `VipConfig` 装配并校验 |

**主目录的热应用为什么仍然是定向的**：配置类的装配走 `ConfigurableProcessor.process`，它是纯字段赋值，没有
副作用；因此「改一个文件只影响一个配置类」成立。需要额外 `refresh()` 的配置类（`DropConfig`、
`InstanceConfig`、`SkillConfig`、`TransportConfig`）在装配后显式调用一次。**完整的 `Config.reload()` 仍然
从不被调用**，副本、攻城与日常调度等二十余个调度器不受影响。

**写入流程**：备份 → 写盘 → 重建配置源 → 定向热应用。
**回滚**：任一步失败即用备份覆盖回原文件、再次热应用恢复旧值，并向前端回报失败与回滚后状态。
写操作**永远不会**把服务端带进「文件已改、内存未改」的分裂状态。

---

## 六、认证与安全 (Authentication and Security)

- 凭据来自 `config/web/web.properties`，**不**来自游戏账号表：控制台是与游戏服并列的运维入口，独立凭据
  避免把「能改 GM 权限」的能力绑在玩家账号上。
- 密码比对沿用游戏服的 `AccountController.encodePassword` 语义（SHA-1 + Base64）。
- **默认账号密码均为 `aion`**：这是明示的安全取舍，不是遗漏。默认只绑定 `127.0.0.1`；**暴露到非本机地址前
  必须改掉默认密码**。
- 凭据缺失（`web.password` 为空）时拒绝一切登录（fail-closed）：宁可无人能登录，也不放行无凭据访问。
- `web.properties` **不**被遗留属性镜像收录，密码因此不会进入 Spring `Environment`，也不会被任何读取环境的
  组件看到。

---

## 七、运维须知 (Operator Notes)

1. 控制台可修改 **GM 权限等级**与**命令权限**。任何能访问控制台的人都能改变游戏的权限体系，务必限制访问。
2. 默认监听 `127.0.0.1:8080`。对外暴露前必须同时做两件事：改 `web.password`、确认 `web.address`。
3. `web.enabled` 与 `web.port` 的改动需要**重启**才生效（它们决定的是 Web 层是否启动与监听哪里）。
4. 被 `mygs.properties` 或环境变量覆盖的键，在控制台里会显式标注「被覆盖」，改了不会生效——这是预期行为。

---

## 八、相关文档 (Related Documents)

- [启动指南](./启动指南.zh-CN.md)
- [API 契约](./api-contract.zh-CN.md)
- [配置编辑模型](./config-edit-model.zh-CN.md)
- 前端工程说明：[`web/README.md`](../../../web/README.md)
