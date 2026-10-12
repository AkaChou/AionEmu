import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 一次性迁移工具：把 al_server_gs 与 al_server_ls 合并进 aion_emu（登录服表加 ls_ 前缀）。
 * One-shot migration tool merging al_server_gs and al_server_ls into aion_emu (ls_ prefix for login tables).
 *
 * <p>表结构来自与运行时 Flyway 共用的 V1__init.sql；存量数据按同实例 INSERT ... SELECT 搬运；
 * 搬运前 TRUNCATE 目标表（重跑 = 用旧库快照覆盖），搬运后逐表校验行数。
 * Schema comes from the same V1__init.sql the runtime Flyway uses; existing rows are copied with
 * same-instance INSERT ... SELECT; target tables are truncated first (a re-run overwrites with the
 * legacy snapshot) and row counts are verified per table afterwards.</p>
 *
 * <p>环境变量 / Environment: MYSQL_HOST, MYSQL_PORT, MYSQL_USER, MYSQL_PASSWORD,
 * SRC_GS_DB, SRC_LS_DB, DST_DB, V1_SQL. Assumes both legacy databases are on the same MySQL instance.</p>
 */
public final class MigrateToAionEmu {

	private static final int PROGRESS_EVERY = 10;

	public static void main(String[] args) throws Exception {
		String host = env("MYSQL_HOST", "127.0.0.1");
		int port = Integer.parseInt(env("MYSQL_PORT", "3306"));
		String user = env("MYSQL_USER", "root");
		String password = env("MYSQL_PASSWORD", "123456");
		String srcGs = env("SRC_GS_DB", "al_server_gs");
		String srcLs = env("SRC_LS_DB", "al_server_ls");
		String dst = env("DST_DB", "aion_emu");
		Path v1Sql = Path.of(env("V1_SQL", "src/main/resources/db/migration/V1__init.sql"));

		String url = "jdbc:mysql://%s:%d/?useUnicode=true&characterEncoding=UTF-8&useSSL=false"
			+ "&allowPublicKeyRetrieval=true&allowMultiQueries=true".formatted(host, port);
		System.out.println("Migrating " + srcGs + " + " + srcLs + " -> " + dst + " on " + host + ":" + port);

		try (Connection connection = DriverManager.getConnection(url, user, password)) {
			connection.createStatement().execute("CREATE DATABASE IF NOT EXISTS `" + dst + "` DEFAULT CHARACTER SET utf8mb4");
			applySchemaIfEmpty(connection, dst, v1Sql);
			copyData(connection, srcGs, srcLs, dst);
		}
		System.out.println("Migration complete.");
	}

	/** 目标库为空时执行 V1 建表与种子数据。Apply V1 DDL and seed rows when the target database is empty. */
	private static void applySchemaIfEmpty(Connection connection, String dst, Path v1Sql) throws Exception {
		long tables = scalar(connection, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '" + dst + "'");
		if (tables > 0) {
			System.out.println("Schema already present in " + dst + " (" + tables + " tables); skipping DDL.");
			return;
		}
		System.out.println("Applying schema from " + v1Sql + " ...");
		try (Statement statement = connection.createStatement()) {
			for (String block : splitSql(Files.readString(v1Sql))) {
				statement.execute(block);
			}
		}
	}

	/** 按行首的 CREATE TABLE / INSERT INTO 把 V1 脚本切成独立语句块。Split the V1 script into statement blocks. */
	private static List<String> splitSql(String script) {
		List<String> blocks = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String line : script.split("\n")) {
			if (line.startsWith("--")) {
				continue;
			}
			if ((line.startsWith("CREATE TABLE") || line.startsWith("INSERT INTO")) && !current.isEmpty()) {
				blocks.add(current.toString());
				current.setLength(0);
			}
			if (!line.isBlank()) {
				current.append(line).append('\n');
			}
		}
		if (!current.isEmpty()) {
			blocks.add(current.toString());
		}
		return blocks;
	}

	/** 逐表搬运：目标表先清空，再按列交集从源库同实例复制，最后校验行数。Copy every table and verify row counts. */
	private static void copyData(Connection connection, String srcGs, String srcLs, String dst) throws SQLException {
		Set<String> dstTables = tableNames(connection, dst);
		Set<String> gsTables = tableNames(connection, srcGs);
		Set<String> lsTables = tableNames(connection, srcLs);

		try (Statement statement = connection.createStatement()) {
			statement.execute("SET FOREIGN_KEY_CHECKS = 0");
			int copied = 0;
			int skipped = 0;
			for (String table : dstTables) {
				String sourceDb = table.startsWith("ls_") ? srcLs : srcGs;
				String sourceTable = table.startsWith("ls_") ? table.substring(3) : table;
				Set<String> sourceTables = srcLs.equals(sourceDb) ? lsTables : gsTables;
				if (!sourceTables.contains(sourceTable)) {
					System.out.println("WARNING: " + sourceDb + "." + sourceTable + " not found; keeping schema defaults for " + dst + "." + table);
					skipped++;
					continue;
				}
				Set<String> targetColumns = columnNames(connection, dst, table);
				Set<String> sourceColumns = columnNames(connection, sourceDb, sourceTable);
				List<String> columns = targetColumns.stream().filter(sourceColumns::contains).toList();
				if (columns.isEmpty()) {
					System.out.println("WARNING: " + table + " has no columns in common with " + sourceDb + "." + sourceTable + "; skipped.");
					skipped++;
					continue;
				}
				if (!targetColumns.equals(sourceColumns)) {
					System.out.println("WARNING: column sets differ for " + table + " (target " + targetColumns.size()
						+ ", source " + sourceColumns.size() + "); inserting the " + columns.size() + " common columns only.");
				}
				String columnList = String.join(", ", columns.stream().map(c -> "`" + c + "`").toList());
				truncate(statement, dst, table);
				statement.execute("INSERT INTO `" + dst + "`.`" + table + "` (" + columnList + ") "
					+ "SELECT " + columnList + " FROM `" + sourceDb + "`.`" + sourceTable + "`");
				copied++;
				if (copied % PROGRESS_EVERY == 0) {
					System.out.println("Copied " + copied + " tables ...");
				}
			}
			statement.execute("SET FOREIGN_KEY_CHECKS = 1");
			System.out.println("Copied " + copied + " tables (" + skipped + " skipped).");
		}
		verifyRowCounts(connection, srcGs, srcLs, dst, dstTables, gsTables, lsTables);
	}

	/** 清空目标表：优先 TRUNCATE，被外键引用而拒绝时退回 DELETE。Empty a target table, falling back to DELETE. */
	private static void truncate(Statement statement, String dst, String table) throws SQLException {
		try {
			statement.execute("TRUNCATE TABLE `" + dst + "`.`" + table + "`");
		} catch (SQLException e) {
			statement.execute("DELETE FROM `" + dst + "`.`" + table + "`");
			statement.execute("ALTER TABLE `" + dst + "`.`" + table + "` AUTO_INCREMENT = 1");
		}
	}

	/** 逐表比较源库与目标库行数，不一致则失败。Fail when any migrated table's row count differs from its source. */
	private static void verifyRowCounts(Connection connection, String srcGs, String srcLs, String dst, Set<String> dstTables,
		Set<String> gsTables, Set<String> lsTables) throws SQLException {
		System.out.println("Verifying row counts ...");
		List<String> mismatches = new ArrayList<>();
		for (String table : dstTables) {
			String sourceDb = table.startsWith("ls_") ? srcLs : srcGs;
			String sourceTable = table.startsWith("ls_") ? table.substring(3) : table;
			Set<String> sourceTables = srcLs.equals(sourceDb) ? lsTables : gsTables;
			if (!sourceTables.contains(sourceTable)) {
				continue;
			}
			long source = scalar(connection, "SELECT COUNT(*) FROM `" + sourceDb + "`.`" + sourceTable + "`");
			long target = scalar(connection, "SELECT COUNT(*) FROM `" + dst + "`.`" + table + "`");
			if (source != target) {
				mismatches.add(table + " source=" + source + " target=" + target);
			}
		}
		if (!mismatches.isEmpty()) {
			mismatches.forEach(m -> System.out.println("MISMATCH " + m));
			throw new IllegalStateException("Row-count verification failed for " + mismatches.size() + " table(s)");
		}
		System.out.println("Row counts verified for " + dstTables.size() + " tables.");
	}

	private static Set<String> tableNames(Connection connection, String schema) throws SQLException {
		return strings(connection, "SELECT table_name FROM information_schema.tables WHERE table_schema = '" + schema + "' ORDER BY table_name");
	}

	private static Set<String> columnNames(Connection connection, String schema, String table) throws SQLException {
		return strings(connection, "SELECT column_name FROM information_schema.columns WHERE table_schema = '"
			+ schema + "' AND table_name = '" + table + "' ORDER BY ordinal_position");
	}

	private static Set<String> strings(Connection connection, String sql) throws SQLException {
		Set<String> values = new LinkedHashSet<>();
		try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
			while (rs.next()) {
				values.add(rs.getString(1));
			}
		}
		return values;
	}

	private static long scalar(Connection connection, String sql) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
			rs.next();
			return rs.getLong(1);
		}
	}

	private static String env(String name, String fallback) {
		String value = System.getenv(name);
		return value == null || value.isBlank() ? fallback : value;
	}

	private MigrateToAionEmu() {
	}
}
