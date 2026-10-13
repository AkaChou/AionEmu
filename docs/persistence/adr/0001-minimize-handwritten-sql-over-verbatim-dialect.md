# ADR 0001：放弃「方言 SQL 逐字节保留」，改为最小化手写 SQL / Minimize handwritten SQL over verbatim dialect

- **状态 / Status**: accepted（2026-10-13，取代 `flex-domain-migration-spec.md` §4 旧口径）
- **范围 / Scope**: `gameserver/persistence/**`、`loginserver/persistence/**`，以及引用它们的事务边界

## 决策 / Decision

Flex 迁移期确立过一条口径：**方言 SQL 逐字节保留**（只把 `?` 改成 `#{name}`）。本次撤销它——**默认走 Flex CRUD，只有 Flex 确实表达不了的语句才保留注解 SQL，并在注解 Javadoc 写明保留理由**。同时把「调用方持有 `Connection` 手工管事务」改为「Spring 事务边界」。两条改动同批落地：注解豁免和手工连接是同一个病灶的两半——只要调用方还握着连接，DAO 就不可能干净地退回 Flex。

## 为什么旧口径是错的 / Why the old stance was wrong

旧的保留判据是「这条语句是不是方言」——这是个与表达力无关的判据。结果是大量**纯单表**语句（`UPDATE t SET c=? WHERE id=?`、`SELECT c FROM t WHERE ...`）因为「写法特殊」被留在注解里，DAO 里同时存在 Flex 与 `PreparedStatement` 两套写库路径，事务归属也随之分裂。

真正该问的是：**Flex 能不能表达它？** 按这个判据重新穷尽后：

- 可转的（已转）：单表带 `WHERE` 的 `UPDATE`/`DELETE`/`SELECT`、列自运算（`col - 1`）、`LEAST(...)`、`NOW()`、`COUNT(...)` 等。
- 确实不可转的（保留，62 条）：原子 upsert（`ON DUPLICATE KEY UPDATE` 32 / `REPLACE INTO` 7 / `INSERT IGNORE` 3）、无 `WHERE` 全表 4、JOIN 与子查询 8、跨闭包单表 7、`INSERT` 内嵌 `NOW()` 1。逐条理由见 §4.2。

## 已考虑并否决的备选 / Considered and rejected

1. **继续逐字节保留**——否决：判据与目标（少写 SQL）背道而驰，迁移永远「不彻底」。
2. **用「存在性检查 + 插入」模拟原子 upsert**——否决：把单语句原子操作拆成两条，正是 `TransactionSafetyTest` 当年要防的竞态；宁可保留注解。已核实 **Flex 1.11.8 没有任何 SQL 级 upsert**：`insertOrUpdate(entity, ignoreNulls)` 是 Java 分支（主键有值走 `update`、否则走 `insert`），这些表主键全为业务值，直接用它**只会发 UPDATE、行不存在时永不插入 = 静默丢数据**。
3. **为无 `@Id` 表补主键/改 DDL 以便用 byId**——否决：本次不动 schema，一律查询式 CRUD（`updateByQuery`/`deleteByQuery`/`selectListByQuery`）。
4. **新建一套「Flex + 裸 JDBC 同事务」基础设施**——否决：接缝已存在，`DatabaseAccess.getConnection()` 就是 `DataSourceUtils.getConnection`，事务内自动复用 Spring 的事务连接。

## 后果 / Consequences

- 注解数量从「按方言判据残留」收敛到 62 条，且每条都有可审计的理由；新增注解必须过 §4.2 这道门。
- **跨闭包单表语句（7 条）是例外里的例外**：它们不是「Flex 表达不了」，而是「归属表与目标表分属不同闭包」（如 `BrokerMapper` 读 `inventory`）。换闭包边界时应优先重估，而不是当成永久豁免。
- 事务全部归 Spring：DAO 不再有 `Connection` 形参，`QuestUnitOfWork` 包 `PlatformTransactionManager`；`setAutoCommit` 在全库归零。代价是 DAO 方法**只能在事务内被调用**，脱离事务的调用点会立刻暴露（`PlayerQuestDaoImpl` 用 `TransactionSynchronizationManager.isActualTransactionActive()` 显式 fail-fast）。
- 少数原 `SQLException` 捕获点改为捕获 `RuntimeException`（`JdbcClient`/Flex 抛 `DataAccessException`），顺带把此前会冒泡的 `NullPointerException` 一并吞掉——只影响日志与命令不中断，不改可观测数据行为。
