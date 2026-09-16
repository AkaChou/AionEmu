# 任务追踪日志开关（//quest log + 日志配置迁移）

- 状态: 客户端验收通过（CLIENT_ACCEPTED）
- 日期: 2026-09-15

## 需求

`[QUEST-TRACE] SM_QUEST_ACTION ... 状态=../步数=..` 一类任务追踪日志默认不再输出；管理员可在游戏内用 `//quest log`
按选中玩家开启/关闭，避免全局刷屏。

## 实现

- `c573bf48b` feat(quest): add per-player quest trace toggle via //quest log
  - `Player.questTraceEnabled`（volatile，仅内存，不持久化，默认 false）。
  - `//quest log [on|off]` 作用于选中玩家；未选中玩家时默认对自己生效；沿用 `quest = 3` 即默认仅管理员可用。
  - 四处打点 `SM_QUEST_ACTION` / `SM_DIALOG_WINDOW` / `CM_DIALOG_SELECT` / `CM_USE_ITEM`
    改为「全局开关或该玩家开关任一成立即输出」。
- `9b0d677ce` refactor(config): move quest trace switch into logging.properties
  - 开关迁到 `LoggingConfig.LOG_QUEST_TRACE`，键 `gameserver.log.questtrace`，声明在
    `src/main/resources/aion/config/main/logging.properties`（默认 false），与该文件其它 `gameserver.log.*` 命名一致。
  - 删除 `NetworkConfig.DISPLAY_QUEST_TRACE` 与 `src/main/resources/aion/config/network/network.properties`
    中的 `gameserver.network.display.questtrace`；旧键不再被任何代码读取。
  - `//configure` 增加 `logging` 映射，运行时可查改：`//configure show logging LOG_QUEST_TRACE`。

## 验收

- user acceptance confirmation: 2026-09-15 用户回复“验证通过”；覆盖本功能整体（默认关闭、按选中玩家开启）。
- repository commit: `9b0d677ce`（配置迁移）、`c573bf48b`（按玩家开关）。
- server launch mode: not captured。
- runtime logs / protocol trace / screenshots and SHA-256: not captured（用户游戏内确认，无随附证据）。
- 生效方式: `Config.reload()` 会重新处理 `LoggingConfig`，可 `//reload config` 热加载或重启生效。

## 部署注意

- `package.sh` 默认 `AION_PRESERVE_CONFIG=false`，会用 `src/main/resources/aion` 覆盖运行配置；
  `re-package.sh` 设为 `true`，仅补齐缺失文件、保留已有运行配置。
- 采用保留模式升级的实例需要手工迁移：从 `config/network/network.properties` 删除旧键，
  在 `config/main/logging.properties` 写入 `gameserver.log.questtrace`；否则旧键被忽略后只剩默认关闭加玩家级开关。
- 本次已同步本地未跟踪部署目录 `aion/config`（`network.properties` 删除旧键、`main/logging.properties` 增加新键）。

## 门禁结果（2026-09-16 授权执行）

- 编译：`mvn -B -DskipTests compile`（先 touch 本次变更源文件以强制全量重编译）→ 4718 个源文件、`release 25`、BUILD SUCCESS。
- 聚焦测试：`mvn -B -Dtest=QuestClientListSyncTest,QuestReloadAtomicityTest,QuestStatePersistenceSafetyTest,LocalizedLogCallsTest,ConfigBindingTest,LegacyConfigOverridesTest,LegacyServerConfigOverridesTest test` → 16/16 通过。
- 全量测试：`mvn -B test` → 3277 用例、12 failures / 0 errors / 2 skipped。12 项失败均为与本改动无关的既有失败（零售 AI 定义与路径数量、windstream 模板、任务 1722/1367/3935/80805 路由、SETPRO 提前领奖审计、实例编队分组），与 `57bd4ec21` 记录的历史失败集一致。
- 门禁发现并修复本次改动引入的回归：`SM_QUEST_ACTION.writeImpl` 新增的 `con.getActivePlayer()` 在 `con == null` 时抛 NPE（`SM_QUEST_ACTIONTest` 以 null 连接驱动 `writeImpl` 校验包体），导致 3 个用例报错；改为 `con == null ? null : con.getActivePlayer()` 后该测试 3/3 通过，全量 errors 归零。修复文件 `src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_QUEST_ACTION.java` 与本记录同批提交。
- 运行环境：检测到 IDEA 启动的服务进程（`com.aionemu.AionBootApplication`）在跑，因此未执行 `mvn clean`（避免删除运行中的 `target/classes`），也未做任何服务生命周期操作；构建日志留在仓库外的 `/tmp/aion-mvn-*-2026-09-16.log`。

## 剩余风险

- 键名变更属破坏性迁移：外部 `mygs.properties` 若仍写旧键将不生效。
- 全量套件的 12 项既有失败仍未处理，不属于本任务范围。
- 该改动未形成跨域新不变量，未新增 Pattern，也未更新 memory-bank。
