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
| Mapper | `persistence/mapper/XxxMapper` | `extends BaseMapper<XxxPo>`；**仅**复杂/方言 SQL 用注解（`@Select/@Insert/@Update/@Delete`），其余走 Flex CRUD |
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

## 4. SQL 转换规则

- 常规单表 CRUD → Flex（`selectListByQuery` / `insert` / `UpdateChain`）。
- 方言 SQL 逐字节保留，仅 `?` → `#{name}`：`REPLACE INTO`、`INSERT ... ON DUPLICATE KEY UPDATE`、`UPDATE t SET c=?`（无 WHERE，Flex 硬拒）、`COUNT(DISTINCT col)` 以外的复杂聚合、JOIN/子查询。
- `storeInTransaction(Connection, ...)` 类方法：**不经 Mapper**，用原生 `PreparedStatement` 并向上抛 `SQLException`（Mapper 走独立 SqlSession，无法与调用方事务同事务）；Mapper 中保留 `?` 原文常量命名 `<原名>_JDBC`。
- 无 WHERE 的 UPDATE/DELETE：Flex 运行期抛 `UPDATE_OR_DELETE_NOT_ALLOW`，必须保留注解 SQL。

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

## 7. 终态判据（终批审计）

grep 归零：`gameserver.dao.` / `loginserver.dao.`、`DAOManager.getDAO`、`buildOfflineDaos`、`safeDao`、`GameDAOClassProvider`、`DAOUtils`；删除 `DatabaseFactory`、`commons/database/**`、`gameserver/dao/**`、`loginserver/dao/**`。
**核对结果（2026-10-12）：全部归零，目录已不存在；`LocalizedLogArgumentsTest` 全库 0 缺失键；`GameServiceProviderCompatibilityTest` / `MinionServiceTest` / `ModelCollectionImplementationTest` / `TransactionSafetyTest` 等回归绿。**
