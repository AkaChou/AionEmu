package com.aionemu.tools;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.mybatisflex.codegen.config.PackageConfig;
import com.mybatisflex.codegen.config.StrategyConfig;
import com.mybatisflex.codegen.config.TemplateConfig;
import com.mybatisflex.codegen.entity.Column;
import com.mybatisflex.codegen.entity.Table;
import com.mybatisflex.core.service.IService;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mysql.cj.jdbc.MysqlDataSource;

/**
 * MyBatis-Flex 代码生成器：按 aion_emu 表结构生成 XxxPo / XxxMapper / XxxDao / XxxDaoImpl。
 * MyBatis-Flex code generator producing XxxPo / XxxMapper / XxxDao / XxxDaoImpl from the aion_emu schema.
 *
 * <p>一次性工具，生成物入库后作为普通源码维护；已存在的文件不覆盖（overwrite=false）。
 * 前置条件：先建好 aion_emu（全新库启动一次服务端由 Flyway 建表；旧库数据用 scripts/sql 下的迁移脚本搬），
 * 再在 IDE 中直接运行本类 main。
 * One-shot tool: generated files are maintained as regular sources afterwards and existing files are
 * never overwritten. Prepare aion_emu first (start the server once so Flyway builds the schema; move
 * legacy data with the script under scripts/sql), then run this main from the IDE.</p>
 *
 * <p>环境变量 / Environment: AION_DB_URL, AION_DB_USER, AION_DB_PASSWORD.</p>
 */
public final class FlexCodeGenerator {

	private static final String GS_PACKAGE = "com.aionemu.gameserver.persistence";
	private static final String LS_PACKAGE = "com.aionemu.loginserver.persistence";
	private static final String LS_TABLE_PREFIX = "ls_";

	private static final Set<String> JAVA_KEYWORDS = Set.of("abstract", "assert", "boolean", "break", "byte",
		"case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum",
		"extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
		"int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return",
		"short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
		"transient", "try", "void", "volatile", "while", "true", "false", "null");

	public static void main(String[] args) throws Exception {
		String sourceDir = env("AION_SOURCE_DIR", "src/main/java");
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setUrl(env("AION_DB_URL", "jdbc:mysql://127.0.0.1:3306/aion_emu?useUnicode=true"
			+ "&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"));
		dataSource.setUser(env("AION_DB_USER", "root"));
		dataSource.setPassword(env("AION_DB_PASSWORD", "123456"));

		List<String> tables = tableNames(dataSource);
		String filter = env("AION_TABLES", "");
		if (!filter.isEmpty()) {
			Set<String> wanted = new LinkedHashSet<>(List.of(filter.split(",")));
			tables = tables.stream().filter(wanted::contains).toList();
		}
		List<String> gsTables = tables.stream().filter(table -> !table.startsWith(LS_TABLE_PREFIX)).toList();
		List<String> lsTables = tables.stream().filter(table -> table.startsWith(LS_TABLE_PREFIX)).toList();
		System.out.println("gameserver tables: " + gsTables.size() + ", loginserver tables: " + lsTables.size());

		generate(dataSource, sourceDir, GS_PACKAGE, "", gsTables);
		generate(dataSource, sourceDir, LS_PACKAGE, LS_TABLE_PREFIX, lsTables);
	}

	/** 生成一组表到指定基础包。Generate one table group into the given base package. */
	private static void generate(DataSource dataSource, String sourceDir, String basePackage, String tablePrefix,
		List<String> tables) {
		if (tables.isEmpty()) {
			System.out.println("No tables for " + basePackage + "; skipped.");
			return;
		}
		GlobalConfig config = new GlobalConfig();
		StrategyConfig strategy = config.getStrategyConfig();
		strategy.setGenerateTables(new LinkedHashSet<>(tables));
		if (!tablePrefix.isEmpty()) {
			strategy.setTablePrefix(tablePrefix);
		}

		config.enableEntity().setClassSuffix("Po").setSourceDir(sourceDir).setOverwriteEnable(false);
		config.enableMapper().setClassSuffix("Mapper").setOverwriteEnable(false);
		config.getMapperConfig().setSourceDir(sourceDir);
		config.enableService().setClassSuffix("Dao").setOverwriteEnable(false);
		config.getServiceConfig().setSourceDir(sourceDir);
		config.getServiceConfig().setSuperClass(IService.class);
		config.enableServiceImpl().setClassSuffix("DaoImpl").setOverwriteEnable(false);
		config.getServiceImplConfig().setSourceDir(sourceDir);
		config.getServiceImplConfig().setSuperClass(ServiceImpl.class);

		PackageConfig packages = config.getPackageConfig();
		packages.setEntityPackage(basePackage + ".entity");
		packages.setMapperPackage(basePackage + ".mapper");
		packages.setServicePackage(basePackage + ".dao");
		packages.setServiceImplPackage(basePackage + ".dao.impl");

		TemplateConfig templates = config.getTemplateConfig();
		templates.setEntity("/flex-templates/entity.tpl");
		templates.setMapper("/flex-templates/mapper.tpl");
		templates.setService("/flex-templates/dao.tpl");
		templates.setServiceImpl("/flex-templates/daoImpl.tpl");

		config.disableController();
		config.disableTableDef();
		config.disableMapperXml();

		Generator generator = new Generator(dataSource, config);
		List<Table> generated = generator.getTables();
		generated.forEach(FlexCodeGenerator::normalizeProperties);
		generator.generate(generated.stream().map(FlexCodeGenerator::singularize).toList());
		System.out.println("Generated " + generated.size() + " tables into " + basePackage);
	}

	/**
	 * 将表名派生的类名单数化：players→Player、blocks→Block、houses→House。
	 * 规则为去掉一个尾 s；词尾为 ss/us/is 时不动（如 abyss、status）。当前库内表名均为规则复数，
	 * 无该陷阱；若将来出现不规则复数表，在此补映射。
	 * Singularizes the table-derived class name by dropping one trailing {@code s}, leaving words ending in
	 * {@code ss}/{@code us}/{@code is} untouched. Every current table name is a regular plural, so no
	 * irregular form exists today; add a mapping here if one appears.
	 */
	private static Table singularize(Table table) {
		SingularTable copy = new SingularTable();
		copy.setGlobalConfig(table.getGlobalConfig());
		copy.setTableConfig(table.getTableConfig());
		copy.setEntityConfig(table.getEntityConfig());
		copy.setSchema(table.getSchema());
		copy.setName(table.getName());
		copy.setComment(table.getComment());
		copy.setColumns(table.getColumns());
		copy.setPrimaryKeys(table.getPrimaryKeys());
		return copy;
	}

	/** 覆写类名派生，仅单数化 CamelCase 类名，{@code @Table} 注解仍用真实表名。 */
	private static final class SingularTable extends Table {

		@Override
		public String getEntityJavaFileName() {
			String camel = super.getEntityJavaFileName();
			if (camel.length() > 1 && camel.endsWith("s")
				&& !camel.endsWith("ss") && !camel.endsWith("us") && !camel.endsWith("is")) {
				return camel.substring(0, camel.length() - 1);
			}
			return camel;
		}
	}

	/** 规范化属性：is_ 前缀布尔列去 is、关键字列名加 _、tinyint(1) 统一 Integer（本库按小整数读写，非纯布尔）。 */
	private static void normalizeProperties(Table table) {
		for (Column column : table.getColumns()) {
			String property = column.getProperty();
			if (column.getName().startsWith("is_") && property.startsWith("is")
				&& property.length() > 2 && Character.isUpperCase(property.charAt(2))) {
				property = Character.toLowerCase(property.charAt(2)) + property.substring(3);
			}
			if (JAVA_KEYWORDS.contains(property)) {
				property = property + "_";
			}
			column.setProperty(property);
			String rawType = column.getRawType() == null ? "" : column.getRawType().toLowerCase();
			// MySQL 驱动把 tinyint(1) 报告为 BIT(1)；is_ 前缀列保持 Boolean（真布尔），
			// 其余按小整数读写（如 status=2/3、activated 档位）。
			if ("bit".equals(rawType) && column.getRawLength() == 1 && !column.getName().startsWith("is_")) {
				column.setPropertyType("java.lang.Integer");
			}
		}
	}

	private static List<String> tableNames(DataSource dataSource) throws Exception {
		try (Connection connection = dataSource.getConnection();
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() ORDER BY table_name")) {
			List<String> names = new ArrayList<>();
			while (rs.next()) {
				names.add(rs.getString(1));
			}
			return names;
		}
	}

	private static String env(String name, String fallback) {
		String value = System.getenv(name);
		return value == null || value.isBlank() ? fallback : value;
	}

	private FlexCodeGenerator() {
	}
}
