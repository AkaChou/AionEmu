package com.aionemu.boot.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 将遗留 properties 文件映射进 Spring Environment，供 {@code aion.legacy.*} 绑定使用。
 * Maps legacy properties files into the Spring Environment for {@code aion.legacy.*} binding.
 *
 * <p>镜像同时写入原始键，但同一原始键在不同服务取值不一致时不写入。扁平命名空间无法同时表达
 * 两个服务的值，保留其中任意一个都会让全局解析器把登录服的 {@code database.url} 回灌给游戏服。
 * 这类键交由各服务自己的遗留加载器读取本地文件，取值一致的键（如 {@code gameserver.thread.*}、
 * {@code svstats.*}）仍照常镜像。</p>
 *
 * The mirror also publishes raw keys, except when two services define the same raw key with different
 * values. A flat namespace cannot express both, and keeping either one would make the global resolver
 * feed the login-server {@code database.url} back into the game server. Such keys are left to each
 * service's own legacy loader reading its own file, while keys every service agrees on (for example
 * {@code gameserver.thread.*} and {@code svstats.*}) keep being mirrored.
 */
public class AionLegacyPropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "aionLegacyProperties";

    /**
     * 加载游戏/登录/聊天遗留配置并注册为低优先级 PropertySource。
     * Loads game/login/chat legacy configs and registers them as a low-precedence PropertySource.
     *
     * @param environment 可配置环境 / configurable environment
     * @param application Spring 应用实例 / Spring application instance
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        LegacyPropertyCollector collector = new LegacyPropertyCollector();
        loadGameProperties(environment, collector);
        loadLoginProperties(environment, collector);
        loadChatProperties(environment, collector);
        Map<String, Object> properties = collector.published();
        if (!properties.isEmpty()) {
            // 以最低优先级注册：命令行、系统属性、环境变量与 application.yml 仍是有效覆盖，
            // 文件值只在没有任何高优先级来源时生效。
            // Registered at the lowest precedence: command line, system properties, environment
            // variables and application.yml still override; file values apply only when nothing
            // with a higher precedence defines the key.
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
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
     * 返回本处理器顺序：略高于最低优先级，确保在大部分默认源之后生效。
     * Returns this processor's order: slightly above lowest precedence so it applies after most defaults.
     *
     * @return 处理器顺序值 / order value
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 20;
    }

    /**
     * 从游戏配置目录加载遗留属性。
     * Loads legacy properties from the game config directory.
     *
     * @param environment 环境 / environment
     * @param collector 遗留键收集器 / legacy key collector
     */
    private void loadGameProperties(ConfigurableEnvironment environment, LegacyPropertyCollector collector) {
        Path configDir = configDir(environment);
        String legacyPrefix = "aion.legacy.game.property.";
        loadDirectory(collector, configDir.resolve("administration"), legacyPrefix);
        loadDirectory(collector, configDir.resolve("main"), legacyPrefix);
        loadDirectory(collector, configDir.resolve("network"), legacyPrefix);
        loadFile(collector, configDir.resolve("mygs.properties"), legacyPrefix);
    }

    /**
     * 从登录配置目录加载遗留属性。
     * Loads legacy properties from the login config directory.
     *
     * @param environment 环境 / environment
     * @param collector 遗留键收集器 / legacy key collector
     */
    private void loadLoginProperties(ConfigurableEnvironment environment, LegacyPropertyCollector collector) {
        Path configDir = configDir(environment).resolve("login");
        String legacyPrefix = "aion.legacy.login.property.";
        loadDirectory(collector, configDir, legacyPrefix);
        loadFile(collector, networkConfigFile(environment), legacyPrefix, false);
        loadFile(collector, configDir.resolve("myls.properties"), legacyPrefix);
    }

    /**
     * 从聊天配置目录加载遗留属性。
     * Loads legacy properties from the chat config directory.
     *
     * @param environment 环境 / environment
     * @param collector 遗留键收集器 / legacy key collector
     */
    private void loadChatProperties(ConfigurableEnvironment environment, LegacyPropertyCollector collector) {
        Path configDir = configDir(environment).resolve("chat");
        String legacyPrefix = "aion.legacy.chat.property.";
        loadDirectory(collector, configDir, legacyPrefix);
        loadFile(collector, networkConfigFile(environment), legacyPrefix, false);
        loadFile(collector, configDir.resolve("mycs.properties"), legacyPrefix);
    }

    private Path networkConfigFile(ConfigurableEnvironment environment) {
        return configDir(environment).resolve("network/network.properties");
    }

    /**
     * 解析统一配置根目录。
     * Resolves the shared configuration root directory.
     *
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
     *
     * @param collector 遗留键收集器 / legacy key collector
     * @param directory 目录路径 / directory path
     * @param legacyPrefix 遗留键前缀 / legacy key prefix
     */
    private void loadDirectory(LegacyPropertyCollector collector, Path directory, String legacyPrefix) {
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
     *
     * @param collector 遗留键收集器 / legacy key collector
     * @param file 文件路径 / file path
     * @param legacyPrefix 遗留键前缀 / legacy key prefix
     */
    private void loadFile(LegacyPropertyCollector collector, Path file, String legacyPrefix) {
        loadFile(collector, file, legacyPrefix, true);
    }

    /**
     * 读取单个 properties 文件，按是否允许镜像原始键写入收集器。
     * Reads a single properties file into the collector, mirroring raw keys only when allowed.
     *
     * @param collector 遗留键收集器 / legacy key collector
     * @param file 文件路径 / file path
     * @param legacyPrefix 遗留键前缀 / legacy key prefix
     * @param includeRawProperty 是否允许镜像原始键 / whether raw keys may be mirrored
     */
    private void loadFile(LegacyPropertyCollector collector, Path file, String legacyPrefix,
        boolean includeRawProperty) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(file)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load legacy properties from " + file, e);
        }
        properties.forEach((key, value) ->
            collector.put(legacyPrefix, String.valueOf(key), value, includeRawProperty));
    }

    /**
     * 遗留键收集器：始终保存带前缀的键，原始键只在跨服务取值一致时才发布。
     *
     * <p>服务身份取自遗留键前缀（{@code aion.legacy.game.property.} 等）。同一服务内后加载的文件
     * 覆盖先前值；两个服务为同一原始键给出不同值时，该键被永久移出镜像，避免扁平命名空间里的
     * “最后写入者”覆盖另一个服务的本地文件值。</p>
     *
     * Collector for legacy configuration: the prefixed key is always kept, and a raw key is published
     * only while every service agrees on its value.
     *
     * <p>The service identity is the legacy key prefix ({@code aion.legacy.game.property.} and so on).
     * A later file of the same service overrides the earlier value; when two services give one raw key
     * different values, that key is removed from the mirror for good, so a flat-namespace "last writer
     * wins" can no longer shadow another service's own file value.</p>
     */
    private static final class LegacyPropertyCollector {

        /**
         * 待注册的镜像键值：带前缀的键，外加可安全发布的原始键。
         * Mirror entries to register: prefixed keys plus raw keys that are safe to publish.
         */
        private final Map<String, Object> published = new LinkedHashMap<>();

        /**
         * 原始键当前归属的遗留前缀。
         * Legacy prefix currently owning each raw key.
         */
        private final Map<String, String> rawKeyOwners = new HashMap<>();

        /**
         * 已判定跨服务冲突、不再发布的原始键。
         * Raw keys already judged conflicting across services and therefore never published.
         */
        private final Set<String> conflictingRawKeys = new HashSet<>();

        /**
         * 记录一个遗留键。
         * Records one legacy key.
         *
         * @param legacyPrefix 遗留键前缀，也标识归属服务 / legacy prefix, also the owning service identity
         * @param key 原始键 / raw key
         * @param value 属性值 / property value
         * @param includeRawKey 是否允许发布原始键 / whether the raw key may be published
         */
        void put(String legacyPrefix, String key, Object value, boolean includeRawKey) {
            published.put(legacyPrefix + key, value);
            if (!includeRawKey || conflictingRawKeys.contains(key)) {
                return;
            }
            String owner = rawKeyOwners.get(key);
            if (owner == null) {
                rawKeyOwners.put(key, legacyPrefix);
                published.put(key, value);
                return;
            }
            if (owner.equals(legacyPrefix)) {
                // 同一服务内后加载的文件覆盖先前值。 / A later file of the same service wins.
                published.put(key, value);
                return;
            }
            if (value.equals(published.get(key))) {
                // 跨服务取值一致，镜像无需改动。 / Services agree, so the mirrored value stays valid.
                return;
            }
            // 不同服务为同一原始键给出不同值：扁平命名空间无法同时表达两者，故不发布该键。
            // Two services disagree on one raw key: a flat namespace cannot express both, so the key is
            // not published at all.
            conflictingRawKeys.add(key);
            rawKeyOwners.remove(key);
            published.remove(key);
        }

        /**
         * 返回待注册的镜像键值。
         * Returns the collected mirror entries.
         *
         * @return 键值映射 / key-value map
         */
        Map<String, Object> published() {
            return published;
        }
    }
}
