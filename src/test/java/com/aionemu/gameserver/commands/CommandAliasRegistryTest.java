package com.aionemu.gameserver.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 命令别名注册表守卫：配置别名与命令类声明的别名必须双向一致。
 * Command alias registry gate: aliases in the access-level config and aliases declared by command
 * classes must match in both directions.
 * <p>命令类由 {@code ChatProcessor#init} 经 {@code CompiledScriptLoader} 按包反射注册，别名只写在
 * 构造器的 {@code super("...")} 中，因此静态引用永远为零；配置里的别名一旦缺少对应类（或反之），
 * 命令只会静默失效而不会编译失败。中文别名（例如 {@code 移动}）同样以别名键写进同一份配置，
 * 因此也受本守卫覆盖。
 * Command classes are registered reflectively by package scan, so a missing class silently disables a
 * command instead of failing the build. Chinese aliases (for example {@code 移动}) are configured as
 * ordinary alias keys in the same config file, so this gate covers them as well.
 */
class CommandAliasRegistryTest {

	/** 反射扫描的命令包根目录。 / Command package roots scanned reflectively. */
	private static final List<Path> COMMAND_PACKAGES = List.of(
		Path.of("src/main/java/com/aionemu/gameserver/commands/admin"),
		Path.of("src/main/java/com/aionemu/gameserver/commands/player"));

	/** 访问等级配置，键即命令别名。 / Access-level config whose keys are the command aliases. */
	private static final Path ACCESS_LEVEL_CONFIG = Path.of(
		"src/main/resources/aion/config/administration/commands.properties");

	/**
	 * 命令类构造器里的别名声明，捕获 {@code super(...)} 的整个参数列表（支持多别名与中文别名）。
	 * Alias declaration in the command constructor, capturing the whole {@code super(...)} argument list
	 * (several aliases and non-ASCII aliases supported).
	 */
	private static final Pattern DECLARED_ALIAS_DECLARATION = Pattern.compile(
		"super\\(\\s*((?:\"[^\"]*\"\\s*,?\\s*)+)\\)");
	/** {@code super(...)} 参数列表中的单个字符串字面量。 / Single string literal inside a {@code super(...)} list. */
	private static final Pattern ALIAS_LITERAL = Pattern.compile("\"([^\"]*)\"");
	/** 配置行 {@code alias = level}，别名允许中文。 / Config line {@code alias = level}; alias may be non-ASCII. */
	private static final Pattern CONFIGURED_ALIAS = Pattern.compile("^\\s*([^\\s=#]+)\\s*=");

	/**
	 * 配置里的每个别名都必须有命令类，否则该命令在运行期静默失效。
	 * Every configured alias must have a command class, otherwise the command dies silently at runtime.
	 */
	@Test
	void everyConfiguredAliasHasACommandClass() throws IOException {
		TreeSet<String> missingHandlers = new TreeSet<>(configuredAliases());
		missingHandlers.removeAll(declaredAliases().keySet());

		assertTrue(missingHandlers.isEmpty(),
			"commands.properties aliases without a command class (dropped silently at registration): " + missingHandlers);
	}

	/**
	 * 命令类声明的别名都必须出现在配置里，否则 {@code registerCommand} 会直接丢弃该类。
	 * Every alias declared by a command class must be configured, otherwise {@code registerCommand} drops it.
	 */
	@Test
	void everyCommandClassAliasIsConfigured() throws IOException {
		TreeSet<String> missingAccessLevels = new TreeSet<>(declaredAliases().keySet());
		missingAccessLevels.removeAll(configuredAliases());

		assertTrue(missingAccessLevels.isEmpty(),
			"command classes whose alias is missing from commands.properties (never registered): " + missingAccessLevels);
	}

	/**
	 * 每个命令源文件必须且只能有一次 {@code super(...)} 别名声明，保证上面的解析结果完整可信。
	 * Every command source must declare its aliases in exactly one {@code super(...)} call so the parsing above stays complete.
	 */
	@Test
	void everyCommandClassDeclaresAliasesInExactlyOneSuperCall() throws IOException {
		for (Path source : commandSources()) {
			int declarations = 0;
			Matcher matcher = DECLARED_ALIAS_DECLARATION.matcher(Files.readString(source));
			while (matcher.find()) {
				declarations++;
			}
			assertEquals(1, declarations,
				source + " must declare all aliases in exactly one super(\"alias\", ...) call");
		}
	}

	/**
	 * 别名不能包含空白，否则命中判定与参数切分会错位。
	 * Aliases must not contain whitespace, otherwise routing and argument splitting break.
	 */
	@Test
	void everyAliasIsWhitespaceFree() throws IOException {
		for (Map.Entry<String, Path> entry : declaredAliases().entrySet()) {
			assertTrue(entry.getKey().indexOf(' ') < 0 && entry.getKey().indexOf('\t') < 0,
				entry.getValue() + " declares an alias containing whitespace: '" + entry.getKey() + "'");
		}
	}

	/**
	 * 收集命令类声明的别名。
	 * Collects aliases declared by command classes.
	 * @return 别名到源文件的映射 / Alias-to-source map
	 * @throws IOException 读取命令源文件失败时 / When a command source cannot be read
	 */
	private static Map<String, Path> declaredAliases() throws IOException {
		Map<String, Path> aliases = new TreeMap<>();
		for (Path source : commandSources()) {
			Matcher declaration = DECLARED_ALIAS_DECLARATION.matcher(Files.readString(source));
			while (declaration.find()) {
				Matcher literal = ALIAS_LITERAL.matcher(declaration.group(1));
				while (literal.find()) {
					Path previous = aliases.put(literal.group(1), source);
					assertTrue(previous == null,
						"duplicate command alias " + literal.group(1) + " declared by " + previous + " and " + source);
				}
			}
		}
		return aliases;
	}

	/**
	 * 读取访问等级配置里的别名集合。
	 * Reads the configured alias set from the access-level config.
	 * @return 配置别名集合 / Configured alias set
	 * @throws IOException 读取配置失败时 / When the config cannot be read
	 */
	private static TreeSet<String> configuredAliases() throws IOException {
		TreeSet<String> aliases = new TreeSet<>();
		for (String line : Files.readAllLines(ACCESS_LEVEL_CONFIG)) {
			if (line.trim().startsWith("#")) {
				continue;
			}
			Matcher matcher = CONFIGURED_ALIAS.matcher(line);
			if (matcher.find()) {
				aliases.add(matcher.group(1));
			}
		}
		return aliases;
	}

	/**
	 * 列出两个命令包下的全部源文件。
	 * Lists all command sources under both command packages.
	 * @return 源文件列表 / Source file list
	 * @throws IOException 遍历命令包失败时 / When the command packages cannot be walked
	 */
	private static List<Path> commandSources() throws IOException {
		List<Path> sources = new ArrayList<>();
		for (Path root : COMMAND_PACKAGES) {
			try (Stream<Path> files = Files.walk(root)) {
				files.filter(path -> path.getFileName().toString().endsWith(".java")).forEach(sources::add);
			}
		}
		return sources;
	}
}
