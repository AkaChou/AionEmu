package com.aionemu.boot.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 将遗留 properties 文件映射进 Spring Environment，供 {@code aion.legacy.*} 绑定使用。
 * Maps legacy properties files into the Spring Environment for {@code aion.legacy.*} binding.
 * <p><b>不再发布不带前缀的原始键</b>：裸键曾被遗留装配器优先查询，导致 {@code Config.reload()}
 * 重读磁盘后的新值永远用不上（手改配置文件后执行 {@code //reload config} 对任何
 * {@code gameserver.*} 键均无效）。裸键改由 {@link AionConfigFilePropertySourceEnvironmentPostProcessor}
 * 提供的磁盘配置源承担，该源直接读取配置文件本身，因此改盘后立即可见。
 * 带前缀的 {@code aion.legacy.*} 键保留：Spring Binder 依赖它们完成
 * {@code @ConfigurationProperties} 绑定，{@code mygs.properties} 的运维覆盖链也走这条路径。</p>
 * <p><b>Raw keys are no longer published</b>: the legacy assembler used to query them first, which made
 * the values re-read from disk by {@code Config.reload()} unreachable (hand-editing a configuration file
 * and then running {@code //reload config} had no effect on any {@code gameserver.*} key). Raw keys are
 * now served by the disk-backed property source of
 * {@link AionConfigFilePropertySourceEnvironmentPostProcessor}, which reads the configuration files
 * themselves, so an on-disk change is visible immediately. The prefixed {@code aion.legacy.*} keys stay:
 * the Spring Binder needs them for {@code @ConfigurationProperties} binding, and the
 * {@code mygs.properties} operator-override chain runs through the same path.</p>
 * <p>同时把游戏服的 {@code network/database.properties} 翻译成 Boot 数据源键 {@code spring.datasource.*}。
 * <p>It also translates the game server's {@code network/database.properties} into Boot DataSource keys
 * ({@code spring.datasource.*}).
 */
public class AionLegacyPropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "aionLegacyProperties";

    static final String DATA_SOURCE_PROPERTY_SOURCE_NAME = "aionGameDatabaseProperties";

    /** Boot 数据源键 ↔ 游戏服 {@code database.properties} 遗留键 / Boot DataSource keys ↔ legacy keys. */
    private static final Map<String, String> DATA_SOURCE_KEYS = Map.of(
        "url", "database.url",
        "username", "database.user",
        "password", "database.password",
        "hikari.maximum-pool-size", "database.maxconnections",
        "hikari.max-lifetime", "database.hikari.maxLifetime",
        "hikari.connection-test-query", "database.hikari.connectionTestQuery");

    /**
     * 加载游戏/登录/聊天遗留配置并注册为低优先级 PropertySource。
     * Loads game/login/chat legacy configs and registers them as a low-precedence PropertySource.
     * @param environment 可配置环境 / configurable environment
     * @param application Spring 应用实例 / Spring application instance
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> properties = collectLegacyEntries(configDir(environment));
        if (!properties.isEmpty()) {
            // 以最低优先级注册：命令行、系统属性、环境变量与 application.yml 仍是有效覆盖，
            // 文件值只在没有任何高优先级来源时生效。
            // Registered at the lowest precedence: command line, system properties, environment
            // variables and application.yml still override; file values apply only when nothing
            // with a higher precedence defines the key.
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        }
        Map<String, Object> dataSourceProperties = dataSourceProperties(environment);
        if (!dataSourceProperties.isEmpty()) {
            environment.getPropertySources().addLast(
                new MapPropertySource(DATA_SOURCE_PROPERTY_SOURCE_NAME, dataSourceProperties));
        }
        // 这里刻意**不**发布 ConfigSourceResolverHolder：本处理器在任何 SpringApplication 实例化时
        // 都会执行，一旦在此写入全局持有者，测试与并行上下文就会互相污染。解析器只由启动层单例
        // BootConfigSourceResolver 在上下文装配时发布一次。
        // Deliberately does NOT publish ConfigSourceResolverHolder here: this post-processor runs for
        // every SpringApplication instantiation, so writing the global holder here would leak state
        // between tests and parallel contexts. The resolver is published once by the bootstrap
        // singleton BootConfigSourceResolver during context wiring.
    }

    /**
     * 收集带前缀的遗留键值，供启动期注册与运行期刷新共用。
     * <p>
     * 收集结果只含 {@code aion.legacy.{game,login,chat}.property.*} 前缀键；裸键由
     * {@link AionConfigFilePropertySource} 承担。
     * <p>
     * Collects the prefixed legacy entries, shared by the boot-time registration and the runtime refresh.
     * <p>
     * The result carries only {@code aion.legacy.{game,login,chat}.property.*} prefixed keys; the raw keys
     * belong to {@link AionConfigFilePropertySource}.
     * @param configDir 配置根目录 / configuration root directory
     * @return 带前缀的键值表 / prefixed key to value map
     */
    public static Map<String, Object> collectLegacyEntries(Path configDir) {
        LegacyPropertyCollector collector = new LegacyPropertyCollector();
        String gamePrefix = "aion.legacy.game.property.";
        loadDirectory(collector, configDir.resolve("administration"), gamePrefix);
        loadDirectory(collector, configDir.resolve("main"), gamePrefix);
        loadDirectory(collector, configDir.resolve("network"), gamePrefix);
        loadFile(collector, configDir.resolve("mygs.properties"), gamePrefix);
        String loginPrefix = "aion.legacy.login.property.";
        loadDirectory(collector, configDir.resolve("login"), loginPrefix);
        loadFile(collector, configDir.resolve("network/network.properties"), loginPrefix);
        loadFile(collector, configDir.resolve("login/myls.properties"), loginPrefix);
        String chatPrefix = "aion.legacy.chat.property.";
        loadDirectory(collector, configDir.resolve("chat"), chatPrefix);
        loadFile(collector, configDir.resolve("network/network.properties"), chatPrefix);
        loadFile(collector, configDir.resolve("chat/mycs.properties"), chatPrefix);
        return collector.published();
    }

    /**
     * 返回本处理器顺序：略高于最低优先级，确保在大部分默认源之后生效。
     * Returns this processor's order: slightly above lowest precedence so it applies after most defaults.
     * @return 处理器顺序值 / order value
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 20;
    }

    /**
     * 解析统一配置根目录。
     * Resolves the shared configuration root directory.
     * @param environment 环境 / environment
     * @return 配置目录路径 / config directory path
     */
    private Path configDir(ConfigurableEnvironment environment) {
        String configured = environment.getProperty("aion.config.dir");
        if (configured != null) {
            return Path.of(configured);
        }
        if (!environment.containsProperty("aion.home")) {
            Path sourceDirectory = Path.of("src/main/resources/aion/config");
            if (Files.isDirectory(sourceDirectory)) {
                return sourceDirectory;
            }
        }
        return Path.of(environment.getProperty("aion.home", "aion")).resolve("config");
    }

    /**
     * 按文件名排序加载目录下全部 {@code .properties} 文件。
     * Loads all {@code .properties} files under a directory in filename order.
     * @param collector 遗留键收集器 / legacy key collector
     * @param directory 目录路径 / directory path
     * @param legacyPrefix 遗留键前缀 / legacy key prefix
     */
    private static void loadDirectory(LegacyPropertyCollector collector, Path directory, String legacyPrefix) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.list(directory)) {
            paths
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".properties"))
                .sorted(Comparator.comparing(Path::toString))
                .forEach(path -> loadFile(collector, path, legacyPrefix));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load legacy properties from " + directory, e);
        }
    }

    /**
     * 读取单个 properties 文件并写入收集器。
     * Reads a single properties file into the collector.
     * @param collector 遗留键收集器 / legacy key collector
     * @param file 文件路径 / file path
     * @param legacyPrefix 遗留键前缀 / legacy key prefix
     */
    private static void loadFile(LegacyPropertyCollector collector, Path file, String legacyPrefix) {
        readProperties(file).forEach((key, value) ->
            collector.put(legacyPrefix, String.valueOf(key), value));
    }

    /**
     * 读取单个 properties 文件；文件不存在时返回空集。
     * Reads one properties file; a missing file yields an empty set.
     * @param file 文件路径 / file path
     * @return 属性集 / properties
     */
    private static Properties readProperties(Path file) {
        Properties properties = new Properties();
        if (!Files.isRegularFile(file)) {
            return properties;
        }
        try (InputStream inputStream = Files.newInputStream(file)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load legacy properties from " + file, e);
        }
        return properties;
    }

    /**
     * 把游戏服的库配置翻译成 Boot 数据源键（{@code spring.datasource.*}），以最低优先级注册。
     * Translates the game server's database settings into Boot DataSource keys ({@code spring.datasource.*}),
     * registered at the lowest precedence.
     * @param environment 环境 / environment
     * @return 数据源键值 / DataSource key-value pairs
     */
    private Map<String, Object> dataSourceProperties(ConfigurableEnvironment environment) {
        Properties properties = readProperties(configDir(environment).resolve("network/database.properties"));
        Map<String, Object> translated = new LinkedHashMap<>();
        DATA_SOURCE_KEYS.forEach((bootKey, legacyKey) -> {
            String value = properties.getProperty(legacyKey);
            if (value != null) {
                translated.put("spring.datasource." + bootKey, value);
            }
        });
        return translated;
    }

    /**
     * 遗留键收集器：只保存带前缀的键，裸键一律不发布。
     * <p>服务身份取自遗留键前缀（{@code aion.legacy.game.property.} 等）。同一服务内后加载的文件
     * 覆盖先前值。裸键由磁盘配置源承担，此处不再镜像，避免遮蔽配置文件本身。</p>
     * Collector for legacy configuration: only prefixed keys are kept, raw keys are never published.
     * <p>The service identity is the legacy key prefix ({@code aion.legacy.game.property.} and so on).
     * A later file of the same service overrides the earlier value. Raw keys are served by the disk-backed
     * property source instead, so they are not mirrored here and never shadow the configuration files.</p>
     */
    private static final class LegacyPropertyCollector {

        /**
         * 待注册的镜像键值：带前缀的键。
         * Mirror entries to register: prefixed keys.
         */
        private final Map<String, Object> published = new LinkedHashMap<>();

        /**
         * 记录一个遗留键。
         * Records one legacy key.
         * @param legacyPrefix 遗留键前缀，也标识归属服务 / legacy prefix, also the owning service identity
         * @param key 原始键 / raw key
         * @param value 属性值 / property value
         */
        void put(String legacyPrefix, String key, Object value) {
            published.put(legacyPrefix + key, value);
        }

        /**
         * 返回待注册的镜像键值。
         * Returns the collected mirror entries.
         * @return 键值映射 / key-value map
         */
        Map<String, Object> published() {
            return published;
        }
    }
}
