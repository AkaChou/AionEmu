# MyBatis-Flex 域迁移规约 / Flex Domain Migration Spec

> 本文件是「双库合并（al_server_gs + al_server_ls → aion_emu）+ MyBatis-Flex 全量替换旧 JDBC DAO」的
> **权威执行规约**。每批迁移前先读本文；批次结束回填台账。
> Authoritative execution spec for the Flex migration. Read before each batch; append to the ledger when done.

## 0. 已落地基线（勿重复）

- 合库 + Flyway：`src/main/resources/db/migration/V1__init.sql`（89 表 = 78 gs + 11 `ls_` 前缀）。
- 代码生成器：`src/test/java/com/aionemu/tools/FlexCodeGenerator.java` + `src/test/resources/flex-templates/*.tpl`。
  已生成 gs 78 表 × 4 = 312 个骨架（`gameserver/persistence/{entity,mapper,dao,dao/impl}`）与 ls 11 表。
- 数据源：旧 `DatabaseFactory` / `ContextRoutingDataSource` **已整体删除**；事务与连接统一走 `gameserver/persistence/DatabaseAccess`（`DataSourceUtils` + `TransactionTemplate`）。
- ls 迁移已完成（`loginserver/persistence/*`，参照实现见 `AccountDataDaoImpl`）。
- gs 迁移已完成：78 表 × 4 四件套（`gameserver/persistence/{entity,mapper,dao,dao/impl}`），旧 `gameserver/dao/**` 与 `commons/database/**` 全删；终批审计见 §7。

## 1. 每个闭包的产物与职责

闭包单元 = 一个表（或其集合）+ 引用它的遗留 DAO。

| 产物 | 路径 | 要求 |
|---|---|---|
| 实体 Po | `persistence/entity/XxxPo` | `@Data @Table("表名")`；`@Id` 单主键 / `KeyType.Auto` 自增；`is_` 前缀布尔列改字段名去 `is` 前缀 + `@Column("is_xxx")` |
| Mapper | `persistence/mapper/XxxMapper` | `extends BaseMapper<XxxPo>`；**默认空接口**，一律走 Flex CRUD；仅 §4.2 例外清单内的语句才用注解（`@Select/@Insert/@Update/@Delete`），且注解 Javadoc 必须写明保留理由 |
| Dao 接口 | `persistence/dao/XxxDao` | `extends IService<XxxPo>`；**方法签名 = 遗留 DAO 公开方法签名**（去掉 `getClassName`/`supports`），使调用点零改动 |
| DaoImpl | `persistence/dao/impl/XxxDaoImpl` | `@Service @Lazy extends ServiceImpl<Mapper,Po> implements Dao`；**保留原降级语义**（catch `DataAccessException` → 原 I18n 键 → 原降级返回值） |

## 2. 调用点接入（关键：签名不变则调用点不动）

- 调用点 `daos().xxxDAO().method(...)` **无需改动**——`PlayerServiceDaos` 的组件名与方法签名保持不变，只把**字段类型**从 `gameserver.dao.XxxDAO` 换成 `persistence.dao.XxxDao`。
- 因此每闭包的改动面固定为 4 处：
  1. `persistence/dao/XxxDao` + `impl/XxxDaoImpl` + `mapper/XxxMapper`（实体已由生成器就位，按 §3 校正）；
  2. `PlayerServiceDaos`（import + record 组件类型）；
  3. `CoreRuntimeServiceBeans.playerServiceDaos`（`daoFrom(beanFactory, XxxDao.class)`，泛型已放宽为 `<T>`）；
  4. `Player.java` 等直持 DAO 字段的类型（如 `daoVars`）。
- 每闭包完成后**必须编译绿**再进下一个。

## 3. 实体校正规则（生成器按 DDL 推类型，需按原读取方式修正）

- 字段类型以**原 `rs.getXxx` 读取方式**为准：`rs.getLong` → `Long/long`（哪怕 DDL 是 decimal/int）；`rs.getByte` → `Byte`；`rs.getBoolean` → `Boolean`。
- 枚举列（DDL `enum(...)`）→ `String`。
- `timestamp/datetime`：原 `rs.getTimestamp` → `Timestamp`；原 `rs.getString` 读时间列的保持 `String`。
- 类型修正须在实体 Javadoc 注明 DDL 原类型。

## 4. SQL 转换规则（本版口径：最小化手写 SQL）

> **本节于 2026-10-13 改写。** 旧口径是「方言 SQL 逐字节保留，仅 `?` → `#{name}`」，把可转语句
> 大量留在注解里；新口径是「**能由 Flex 表达的，一律不手写 SQL**；只有确实无法表达时才保留注解，
> **且必须在 Javadoc 注明为何 Flex 表达不了**」。策略逆转的动因与备选方案见
> [`adr/0001-minimize-handwritten-sql-over-verbatim-dialect.md`](adr/0001-minimize-handwritten-sql-over-verbatim-dialect.md)。

### 4.1 默认：走 Flex

- 单表查询 → `selectListByQuery` / `selectOneByQuery` / `selectCountByQuery` + `QueryWrapper` 谓词（**用 `Po::getX` 方法引用，不写字符串列名**）。
- 单表更新 → `UpdateChain.of(Po).set(...).where(...).update()`；需要影响行数用 `mapper.updateByQuery(po, wrapper)`（返回 `int`，`ignoreNulls=true`，SET 自动排除主键列）。
- 单表删除 → `mapper.deleteByQuery(wrapper)`。
- 批量写入 → `mapper.insertBatch(rows)`。
- 列自运算 → `QueryColumn.add/subtract/multiply/divide`；方言函数 → `new FunctionQueryColumn("LEAST", col.add(n), new RawQueryColumn(...))`。
- 时间函数 → `QueryMethods.now()/currentTimestamp()`（**禁止 `setRaw(col, "NOW()")` 之类的裸片段**）。
- 无 `@Id` 表（`outpost_location`/`competition_ranking`/`item_drop`/`legion_announcement_list`/`player_skill_skins`/`player_transform`）：**不改 DDL**，一律查询式 CRUD，不用 byId。

### 4.2 例外清单（保留注解 SQL，共 62 条，按理由归类）

| 理由 | 条数 | 代表 | 为何 Flex 表达不了 |
|---|---|---|---|
| 原子 upsert (`INSERT ... ON DUPLICATE KEY UPDATE`) | 32 | `PlayerVar.upsert`、`PlayerRecipe.addRecipe`、`LadderPlayer.addWins`、`AccountPlaytime.accumulateOnlineTime` | **Flex 1.11.8 无 SQL 级 upsert**：`insertOrUpdate(entity, ignoreNulls)` 是 Java 分支（主键有值 → `update`，否则 → `insert`）。这些表主键全为业务值，故它**只发 UPDATE**，行不存在时 0 行、从不插入 = 静默丢数据；`IService.saveOrUpdate` 只是它的包装 |
| `REPLACE INTO` | 7 | `PlayerSetting.replaceSetting`、`ServerVariable.replace`、`BannedMac.replaceBan`、`AccountTime.replaceAccountTime` | 同上（无 SQL 级 upsert）。改成「`deleteByQuery` + `insert`」会把单语句原子操作拆成两条，重新引入竞态 |
| `INSERT IGNORE` | 3 | `LimitedQuestCounter.initialize`、`AccountVip.insertIfAbsent`、`AccountVip.syncMissingAccounts`（后者是 `INSERT IGNORE ... SELECT`） | Flex 无 `insertIgnore`；同样不能退化成「存在性检查 + 插入」 |
| 无 `WHERE` 全表 `UPDATE`/`DELETE` | 4 | `Player.setAllOnline`、`Svstat.updateAllOffline`、`PlayerLunaShop.deleteAll`、`PlayerShugoSweep.deleteAll` | Flex 运行期硬拒无 `WHERE` 的更新/删除（`UPDATE_OR_DELETE_NOT_ALLOW`） |
| JOIN / 逗号连表 / 子查询 | 8 | `AbyssRank.selectPlayerRanking`、`CompetitionRanking.selectCompetitionRankingPlayers`、`LegionMember.selectLegionMemberExById`、`AccountData.deleteInactiveAccounts` | 跨表结果集（多表列混合 + 别名），Flex 的 `QueryWrapper` 无法表达这些列的投影与连接语义 |
| 跨闭包单表语句（**mapper 归属表 ≠ 语句目标表**） | 7 | `Broker.selectBrokerItems` / `Mail.selectMailboxItems`（读 `inventory`）、`Inventory.selectAccountId`（读 `players`）、`Inventory.selectLegionId`（读 `legion_members`）、`Mail.updateMailboxLetters`（更新 `players`）、`AbyssRank.selectRankByPlayerId` / `countLegionMembers` | 归属表与目标表分属不同闭包；用 Flex 表达需在 AbyssRank 闭包里注入 Legion/Player/Inventory 的 Mapper/Dao，把跨闭包依赖固化进 DAO。**这些是「归属越界」而非「表达力不足」**，换闭包时优先重新评估 |
| `INSERT` 值列表内嵌库端函数 `NOW()` | 1 | `PlayerMinion.insertMinion`（`birthday = NOW()`） | `QueryMethods.now()` 只能出现在 `QueryWrapper`/`UpdateChain` 中；`insert(entity)` 的值来自实体字段，无法注入库端求值 |

> **`*_JDBC` 常量已清零**（唯二保留：`AbyssRankMapper.UPDATE_PLAYER_RANK_LIST_JDBC` /
> `UPDATE_LEGION_RANK_LIST_JDBC`——会话变量排名 + 动态 `LIMIT`，由 `AbyssRankDaoImpl` 用
> `PreparedStatement` 执行，仍复用调用方事务连接）。

### 4.3 事务归 Spring（不再由调用方持有 `Connection`）

- DAO 方法**不再接收 `Connection`**：`storeInTransaction(Xxx)` / `deleteInTransaction(...)` 等一律去掉连接形参，
  内部走 Flex（`@Transactional` 或复用调用方已开启的 Spring 事务）。
- 调用方手工事务（`setAutoCommit(false)` / `commit()` / `rollback()`）→ `DatabaseAccess.transactionTemplate()`
  或 `QuestUnitOfWork`（内部包 `PlatformTransactionManager`）。生产源码中 `setAutoCommit` **已清零**
  （仅 `TransactionSafetyTest` 保留两条「不得再出现」的否定断言）。
- **「Flex 与裸 JDBC 同事务」的接缝已存在**：`DatabaseAccess.getConnection()` 是 `DataSourceUtils.getConnection`，
  事务内自动复用 Spring 的事务连接，无需新增基础设施。
- `PlayerQuestDaoImpl.store` 的原「`con.getAutoCommit()==true` → `IllegalStateException`」守卫，等价替换为
  `TransactionSynchronizationManager.isActualTransactionActive()`。
- 非事务的单语句 DML（`Repairkit` / `Bk` / `MotionLoggingService`）：**不套事务**，改走 `DatabaseAccess.jdbcClient()`
  或对应的 Flex DAO/Mapper（`MotionLoggingService` 已改走 `SkillMotionDao`）。

> **已知残留接缝（非本批范围）**：`QuestEventPort` / `PlayerQuestEventPort` 仍保留
> `snapshot(Connection, ...)` 重载，但**形参已被忽略**（直接委托给无连接的 `snapshot(...)`），
> 属兼容入口而非真实连接传递。删除它要牵动 questEngine tablelane 原生夹具，故不在本批处置，
> 后续如需彻底去 `java.sql.Connection` 从这里下手。


## 5. 离线兜底删除（本批确立）

- `PlayerService.buildOfflineDaos` / `safeDao` **已删除**：Flex DAO 只能由 Spring 提供，离线兜底不可达。
- `daos()` 未绑定即抛 `IllegalStateException`（fail-fast）。
- `CoreRuntimeServiceBeans.playerServiceDaos` 的 `catch (NullPointerException)` 兜底已删除；`daoFrom` 泛型放宽为 `<T>` 以接纳 Flex DAO。
- 地点/战斗等静态单例服务的 `XxxServiceHolder.INSTANCE` 双源兜底同步退役，改为 provider `fail-fast`（见 `LegacySingletonFallbackAuditTest`）。
- i18n 键：迁移期由掩码输出复制的键曾有 1 处差一位（`log.191420844445f` vs 真实 `log.19142084445f`），已由 `LocalizedLogArgumentsTest` 拦截；键值一律按码位核对。
- **离线兜底移除的连带效应（测试侧）**：HEAD 上 `playerServiceDaos` 靠 `catch (NullPointerException)` 回退离线 DAO，使「只 register `GameLegacyServiceBridgeConfiguration`、不走组件扫描」的非 Boot 注解上下文也能 refresh；兜底删除后这些上下文立即 `playerDAO must not be null` 失败。修法：新增测试用品 `aionemu/testkit/StubPersistenceDaos`（46 个 `@Bean` + 动态代理桩），并注册进 `new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class, StubPersistenceDaos.class)`。受影响：`GameLegacyServiceBridgeConfigurationTest`（53 处）、`GameStaticDataLifecycleTest`（1 处）。
  - **放包铁律**：该桩类必须放在组件扫描根 `com.aionemu` **之外**（现置于顶层 `aionemu.testkit`）。否则 `@SpringBootApplication` 会把它的 `@Bean` 当作真 DAO 扫进来，与 `@Service` DaoImpl 撞名（`playerDao` vs `playerDaoImpl`，`No qualifying bean ... expected single matching bean but found 2`），打挂 `AionBootApplicationTest` 的扫描用例。生产装配本身不受影响（扫描 `com.aionemu.**` 的 `@Service` DaoImpl + `@MapperScan` 注册 mapper）。

## 6. 台账（每闭包回填）

| 闭包 | 遗留 DAO | 新 Dao | 方法数 | I18n 键 | 降级语义 | 交叉依赖 | 编译 |
|---|---|---|---|---|---|---|---|
| player_vars | dao/PlayerVarsDAO | PlayerVarDao | 3 | ✅ | ✅ | 无 | ✅ |
| gs（合计） | gameserver/dao/**（69 接口 + 70 实现，已删） | persistence/dao/ 78 个 | 与遗留等签名 | ✅ | ✅ | 无 | ✅ |
| ls（合计） | loginserver/dao/**（已删） | loginserver/persistence/dao/ 11 个 | 与遗留等签名 | ✅ | ✅ | 无 | ✅ |

## 6b. 台账（本批：最小化手写 SQL + 事务归 Spring，2026-10-13）

| 项 | 结果 |
|---|---|
| 事务重构 | 15 个 DAO 去 `Connection` 形参；`QuestUnitOfWork` 改包 `PlatformTransactionManager`；`BrokerService` / `ExchangeService` / `MailService` / `SystemMailService` 改 `DatabaseAccess.transactionTemplate()`；全库 `setAutoCommit` 与 `*InTransaction(Connection` **归零** |
| `QueryWrapper` 裸片段 | `.setRaw(col,"NOW()")` 6 处 → `QueryMethods.now()`；`.select("列名")` 11 处 → `Po::getX`（两项均**归零**） |
| 前缀为 `_JDBC` 的常量 | 除会话变量排名 2 处例外外**归零**；`PreparedStatement` 仅剩 `AbyssRankDaoImpl`（排名）与 `commands/admin/{Bk,Repairkit}`（已改 `JdbcClient`，无 `Connection` 管理） |
| 保留注解 | 62 条，全部落入 §4.2 例外清单（原子 upsert 32 / `REPLACE INTO` 7 / `INSERT IGNORE` 3 / 无 WHERE 全表 4 / JOIN 与子查询 8 / 跨闭包单表 7 / 内嵌 `NOW()` 1） |
| 测试改造 | `TransactionSafetyTest` / `LimitedQuestDAOSqlTest` / `PlayerQuestListDAOTest` 改写为新契约；`RecordingTransactionManager` 事务替身替代连接代理；`QuestExecutionCoordinatorTest` / `QuestE2eRuntime` / `QuestCurrencyPortFailClosedTest` 随签名同步；连接断言删除后，先编码了「调用方 Connection」旧契约的 5 个测试方法已改名（`…OnCallerConnection` → `…InCallerTransaction`、`…SharesTheExactConnection` → `…SharesOneTransaction`） |
| 已知既有失败（**与本次无关**） | `QuestInteractionObjectCatalogTest.productionQuestUseItemTalkRoutesDeclareActionEligibility` 断言 13 条 `questId:node:npcId` 缺少 `CanAct(… ACTION_ITEM_USE)` 资格路由。该用例只读 `quest_definition_catalog.xml`（本工作树未改动）且断言体未被本次触碰，属 `quest` 分支既有的任务定义数据缺口，需走 quest-repair 流程另行处理，**不要**在持久层批次里顺手改 |
| 验证 | `build_project` 全绿；已跑 `TransactionSafetyTest`、`LimitedQuestDAOSqlTest`、`PlayerQuestListDAOTest`、`PlayerEquipmentSettingDAOTest`、`VipDaoSqlTest`、`BannedMacManagerTest`、`ServiceConstructorInjectionTest`、`GameLegacyServiceBridgeConfigurationTest`、`AionBootApplicationTest`，以及 questEngine runtime 的 `QuestExecutionCoordinatorTest` / `QuestUnitOfWorkTest` / `QuestProductionDispatcherTest` / `QuestCurrencyPortFailClosedTest` / 各 `PlayerQuest*PortTest` / 三个 production-flow 回归 —— 全部通过 |

## 7. 终态判据（终批审计）

grep 归零：`gameserver.dao.` / `loginserver.dao.`、`DAOManager.getDAO`、`buildOfflineDaos`、`safeDao`、`GameDAOClassProvider`、`DAOUtils`；删除 `DatabaseFactory`、`commons/database/**`、`gameserver/dao/**`、`loginserver/dao/**`。
**核对结果（2026-10-12）：全部归零，目录已不存在；`LocalizedLogArgumentsTest` 全库 0 缺失键；`GameServiceProviderCompatibilityTest` / `MinionServiceTest` / `ModelCollectionImplementationTest` / `TransactionSafetyTest` 等回归绿。**

补充归零项（2026-10-13，配合 §4 新口径）：`setAutoCommit`、`QueryWrapper.setRaw(`、`.select("字符串列名")`、带 `Connection` 形参的 `*InTransaction(...)`、除 2 处例外外的 `*_JDBC` 常量。
**核对结果：上述五项全部归零；保留注解 62 条已逐条落入 §4.2 并注明理由。**
