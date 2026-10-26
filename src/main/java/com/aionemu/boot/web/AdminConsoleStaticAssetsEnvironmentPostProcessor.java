package com.aionemu.boot.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

/**
 * 管理控制台前端产物的静态资源兜底：classpath 里没有已构建的控制台页面时，把磁盘上的构建目录
 * 加入静态资源位置。
 * Static-resource fallback for the admin console frontend: when the classpath carries no built console
 * page, the on-disk build directory is added to the static resource locations.
 * <p>
 * 打包启动时前端产物由构建流程拷入 {@code BOOT-INF/classes/static}，classpath 命中，无需兜底。
 * IDE 直接以 {@code target/classes} 为 classpath 启动时，增量构建可能尚未拷贝前端产物，此时从
 * {@code web/dist} 直接读取文件，避免控制台页面 404。
 * <p>
 * In a packaged run the frontend assets are copied into {@code BOOT-INF/classes/static} by the build and
 * the classpath hits them, so no fallback is needed. When the IDE starts with {@code target/classes} as the
 * classpath, an incremental build may not have copied the assets yet, and they are then read straight from
 * {@code web/dist} so that the console page does not 404.
 */
public class AdminConsoleStaticAssetsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/**
	 * 静态资源位置键。
	 * Static resource locations key.
	 */
	public static final String STATIC_LOCATIONS_KEY = "spring.web.resources.static-locations";

	/**
	 * 属性源名称。
	 * Property source name.
	 */
	public static final String PROPERTY_SOURCE_NAME = "aionAdminConsoleStaticAssets";

	/**
	 * 控制台页面入口文件，用于探测 classpath 里是否已有前端产物。
	 * Console page entry file, used to probe whether the classpath already carries the frontend assets.
	 */
	private static final String INDEX_PATH = "static/index.html";

	/**
	 * Spring Boot 的默认静态资源位置。
	 * The Spring Boot default static resource locations.
	 */
	private static final String DEFAULT_STATIC_LOCATIONS =
		"classpath:/META-INF/resources/,classpath:/resources/,classpath:/static/,classpath:/public/";

	/**
	 * 注册静态资源兜底位置。
	 * Registers the fallback static resource location.
	 * @param environment 可配置环境 / configurable environment
	 * @param application Spring 应用实例 / Spring application instance
	 */
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (classpathHasConsoleAssets()) {
			return;
		}
		Path buildDirectory = frontendBuildDirectory();
		if (buildDirectory == null) {
			return;
		}
		MutablePropertySources sources = environment.getPropertySources();
		Set<String> locations = new LinkedHashSet<>();
		String configured = configuredLocations(sources);
		for (String location : configured.split(",")) {
			String trimmed = location.trim();
			if (!trimmed.isEmpty()) {
				locations.add(trimmed);
			}
		}
		locations.add(staticLocation(buildDirectory));
		String merged = String.join(",", locations);

		// 兜底位置必须排在显式配置之后，但两者必须出现在同一个列表里：Spring Boot 会整体替换
		// spring.web.resources.static-locations 的值，分散在两个属性源里会丢掉显式配置。
		// 因此在拥有该键的属性源上就地追加；没有则注册一个最低优先级的属性源。
		// The fallback location must come after the explicit configuration, yet both must live in the same
		// list: Spring Boot replaces the value of spring.web.resources.static-locations as a whole, so
		// spreading them over two property sources would drop the explicit configuration. The property source
		// that owns the key is therefore replaced in place, keeping its precedence, or a new one is registered
		// at the lowest precedence when there is none.
		for (PropertySource<?> source : sources) {
			if (source instanceof MapPropertySource mapSource && mapSource.getSource().containsKey(STATIC_LOCATIONS_KEY)) {
				Map<String, Object> replacement = new LinkedHashMap<>(mapSource.getSource());
				replacement.put(STATIC_LOCATIONS_KEY, merged);
				sources.replace(source.getName(), new MapPropertySource(source.getName(), replacement));
				return;
			}
		}
		sources.addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, Map.of(STATIC_LOCATIONS_KEY, merged)));
	}

	/**
	 * 解析当前的静态资源位置配置。
	 * Resolves the currently configured static resource locations.
	 * @param sources 属性源 / property sources
	 * @return 位置列表；未配置时为 Spring Boot 的默认值 / the location list, or the Spring Boot defaults
	 */
	private String configuredLocations(MutablePropertySources sources) {
		for (PropertySource<?> source : sources) {
			Object value = source.getProperty(STATIC_LOCATIONS_KEY);
			if (value instanceof String configured && !configured.isBlank()) {
				return configured;
			}
		}
		return DEFAULT_STATIC_LOCATIONS;
	}

	/**
	 * 返回本处理器顺序：最低优先级，确保在所有默认源之后生效。
	 * Returns the processor's order: the lowest precedence, so it applies after every default source.
	 * @return 处理器顺序值 / processor order value
	 */
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	/**
	 * 判断 classpath 里是否已有控制台前端产物。
	 * Tells whether the classpath already carries the console frontend assets.
	 * @return 存在入口文件则为 true / true when the entry file is present
	 */
	private boolean classpathHasConsoleAssets() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		return classLoader != null && classLoader.getResource(INDEX_PATH) != null;
	}

	/**
	 * 定位磁盘上的前端构建目录。
	 * Locates the on-disk frontend build directory.
	 * <p>
	 * 未设置 {@code aion.home}（本地 IDE 或检出运行）时使用源码树的 {@code web/dist}；
	 * 打包启动时前端产物已进 classpath，此处不会命中。
	 * <p>
	 * The source tree's {@code web/dist} is used when {@code aion.home} is not set (a local IDE or checkout
	 * run); in a packaged run the frontend assets are already on the classpath and this never hits.
	 * @return 构建目录；不存在时为 {@code null} / the build directory, or {@code null} when absent
	 */
	private Path frontendBuildDirectory() {
		Path directory = System.getProperty("aion.home") == null
			? Path.of("web/dist")
			: Path.of(System.getProperty("aion.home")).resolve("web/dist");
		return Files.isRegularFile(directory.resolve("index.html")) ? directory : null;
	}

	/**
	 * 把构建目录转换为静态资源位置。
	 * <p>
	 * 使用绝对路径并补上尾斜杠：Spring 的资源处理器要求位置以 {@code /} 结尾才能正确解析其中的
	 * 相对路径，绝对路径则避免依赖进程工作目录。
	 * <p>
	 * Converts the build directory into a static resource location.
	 * <p>
	 * An absolute path with a trailing slash is used: the Spring resource handler expects locations to end
	 * with {@code /} to resolve the relative paths inside them, and the absolute path avoids depending on
	 * the process working directory.
	 * @param directory 构建目录 / build directory
	 * @return 静态资源位置 / the static resource location
	 */
	private String staticLocation(Path directory) {
		String uri = directory.toAbsolutePath().toUri().toString();
		return uri.endsWith("/") ? uri : uri + "/";
	}
}
