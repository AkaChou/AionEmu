package com.aionemu.boot.web.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

/**
 * 生效值解析：按服务端的优先级链算出每个键「真正生效的值」以及该值来自哪一层。
 * Effective value resolver: computes the value that actually takes effect for every key, following the
 * server's precedence chain, together with the layer that value comes from.
 * <p>
 * 优先级链（从高到低）：
 * <ol>
 * <li>命令行参数</li>
 * <li>操作系统环境变量</li>
 * <li>{@code application.yml}</li>
 * <li>运维覆盖层（{@code mygs.properties} / {@code myls.properties} / {@code mycs.properties}）</li>
 * <li>被编辑的配置文件</li>
 * <li>配置类声明的默认值</li>
 * </ol>
 * 控制台必须展示这一层，否则运维改了文件却看到值没变时无从判断是被覆盖了还是改错了。
 * <p>
 * The precedence chain, from highest to lowest: command-line arguments, operating-system environment
 * variables, {@code application.yml}, the operator override layer ({@code mygs.properties} /
 * {@code myls.properties} / {@code mycs.properties}), the edited configuration file, and finally the
 * configuration class's declared default.
 * <p>
 * The console must surface this layer, otherwise an operator who edits a file and sees the value unchanged
 * cannot tell whether it was overridden or mistyped.
 *
 * @author AionEmu
 */
public class EffectiveValueResolver {

	/**
	 * 应用环境。
	 * Application environment.
	 */
	private final ConfigurableEnvironment environment;

	/**
	 * 运维覆盖层键值：来自 {@code mygs.properties} 等运维覆盖文件。
	 * Operator override layer entries, coming from the override files such as {@code mygs.properties}.
	 */
	private volatile Map<String, String> overrideLayer;

	/**
	 * 配置根目录：覆盖层需要随文件改动重新读取。
	 * Configuration root directory: the override layer has to be re-read after a file change.
	 */
	private final Path configDir;

	/**
	 * 构造生效值解析器。
	 * Constructs the effective value resolver.
	 * @param environment 应用环境 / application environment
	 * @param configDir 配置根目录 / configuration root directory
	 */
	public EffectiveValueResolver(ConfigurableEnvironment environment, Path configDir) {
		this.environment = environment;
		this.configDir = configDir;
		this.overrideLayer = readOverrideLayer(configDir);
	}

	/**
	 * 重新读取运维覆盖层：改盘后调用，使覆盖来源随之更新。
	 * Re-reads the operator override layer: called after a write so the override sources stay current.
	 */
	public void reloadOverrideLayer() {
		this.overrideLayer = readOverrideLayer(configDir);
	}

	/**
	 * 解析指定文件内某个键的生效值与来源。
	 * Resolves the effective value and its source for a key inside the given file.
	 * @param key 键 / key
	 * @param fileValue 文件中的值；键不存在时为 {@code null} / the value in the file, {@code null} when the key
	 *                 is absent
	 * @return 生效值与来源 / effective value and its source
	 */
	public Resolved resolve(String key, String fileValue) {
		String commandLine = valueFromSource(CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME, key);
		if (commandLine != null) {
			return new Resolved(commandLine, ValueSource.COMMAND_LINE);
		}
		String systemProperty = valueFromSource(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, key);
		if (systemProperty != null) {
			return new Resolved(systemProperty, ValueSource.SYSTEM_PROPERTY);
		}
		String environmentVariable = valueFromSource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, key);
		if (environmentVariable != null) {
			return new Resolved(environmentVariable, ValueSource.ENVIRONMENT);
		}
		String override = overrideLayer.get(key);
		if (override != null) {
			return new Resolved(override, ValueSource.OVERRIDE_LAYER);
		}
		if (fileValue != null) {
			return new Resolved(fileValue, ValueSource.FILE);
		}
		String applicationValue = valueFromApplicationConfig(key);
		if (applicationValue != null) {
			return new Resolved(applicationValue, ValueSource.APPLICATION_YML);
		}
		return new Resolved(null, ValueSource.DEFAULT);
	}

	/**
	 * 从指定名称的属性源读取键值。
	 * Reads a key from the property source with the given name.
	 * @param sourceName 属性源名称 / property source name
	 * @param key 键 / key
	 * @return 值；该源未定义此键时为 {@code null} / the value, {@code null} when the source does not define the key
	 */
	private String valueFromSource(String sourceName, String key) {
		var source = environment.getPropertySources().get(sourceName);
		return source == null ? null : asText(source.getProperty(key));
	}

	/**
	 * 从 {@code application.yml} 所属的属性源读取键值。
	 * <p>
	 * {@code application.yml} 的内容由 Spring 合并进一个复合属性源，该源的名字是 Boot 内部实现细节
	 * （Boot 4 中为 {@code applicationConfig}），因此这里按名字匹配而不是引用内部常量。
	 * <p>
	 * Reads a key from the property source backing {@code application.yml}.
	 * <p>
	 * The {@code application.yml} content is merged by Spring into a composite property source named
	 * {@code applicationConfig}, which in turn splits into per-file child sources; the composite source is
	 * queried as a whole here.
	 * @param key 键 / key
	 * @return 值；该源未定义此键时为 {@code null} / the value, {@code null} when the source does not define the key
	 */
	private String valueFromApplicationConfig(String key) {
		for (var source : environment.getPropertySources()) {
			if (source.getName().startsWith("applicationConfig")) {
				String value = asText(source.getProperty(key));
				if (value != null) {
					return value;
				}
			}
		}
		return null;
	}

	/**
	 * 把属性源中的值转成文本。
	 * Converts a property source value into text.
	 * @param value 原始值 / raw value
	 * @return 文本；值为空时为 {@code null} / the text, {@code null} when the value is absent
	 */
	private String asText(Object value) {
		return value == null ? null : value.toString();
	}

	/**
	 * 读取运维覆盖层的键值。
	 * Reads the operator override layer entries.
	 * @param configDir 配置根目录 / configuration root directory
	 * @return 覆盖层键值 / override layer entries
	 */
	public static Map<String, String> readOverrideLayer(Path configDir) {
		Map<String, String> entries = new LinkedHashMap<>();
		for (String relative : new String[] { "mygs.properties", "login/myls.properties", "chat/mycs.properties" }) {
			Path file = configDir.resolve(relative);
			if (!Files.isRegularFile(file)) {
				continue;
			}
			Properties properties = new Properties();
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				properties.load(reader);
			} catch (IOException e) {
				continue;
			}
			for (String name : properties.stringPropertyNames()) {
				entries.put(name, properties.getProperty(name));
			}
		}
		return entries;
	}

	/**
	 * 解析结果：生效值与其来源。
	 * The resolution result: the effective value and the layer it comes from.
	 *
	 * @param value 生效值；所有层都没有该键时为 {@code null} / the effective value, {@code null} when no layer
	 *               defines the key
	 * @param source 来源 / source
	 */
	public record Resolved(String value, ValueSource source) {
	}
}
