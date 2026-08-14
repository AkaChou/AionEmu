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
        Map<String, Object> properties = new LinkedHashMap<>();
        loadGameProperties(environment, properties);
        loadLoginProperties(environment, properties);
        loadChatProperties(environment, properties);
        if (!properties.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        }
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
     * @param target 目标属性映射 / target property map
     */
    private void loadGameProperties(ConfigurableEnvironment environment, Map<String, Object> target) {
        Path configDir = configDir(environment);
        String legacyPrefix = "aion.legacy.game.property.";
        loadDirectory(target, configDir.resolve("administration"), legacyPrefix);
        loadDirectory(target, configDir.resolve("main"), legacyPrefix);
        loadDirectory(target, configDir.resolve("network"), legacyPrefix);
        loadFile(target, configDir.resolve("mygs.properties"), legacyPrefix);
    }

    /**
     * 从登录配置目录加载遗留属性。
     * Loads legacy properties from the login config directory.
     *
     * @param environment 环境 / environment
     * @param target 目标属性映射 / target property map
     */
    private void loadLoginProperties(ConfigurableEnvironment environment, Map<String, Object> target) {
        Path configDir = configDir(environment).resolve("login");
        String legacyPrefix = "aion.legacy.login.property.";
        loadDirectory(target, configDir, legacyPrefix);
        loadFile(target, networkConfigFile(environment), legacyPrefix, false);
        loadFile(target, configDir.resolve("myls.properties"), legacyPrefix);
    }

    /**
     * 从聊天配置目录加载遗留属性。
     * Loads legacy properties from the chat config directory.
     *
     * @param environment 环境 / environment
     * @param target 目标属性映射 / target property map
     */
    private void loadChatProperties(ConfigurableEnvironment environment, Map<String, Object> target) {
        Path configDir = configDir(environment).resolve("chat");
        String legacyPrefix = "aion.legacy.chat.property.";
        loadDirectory(target, configDir, legacyPrefix);
        loadFile(target, networkConfigFile(environment), legacyPrefix, false);
        loadFile(target, configDir.resolve("mycs.properties"), legacyPrefix);
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
     * @param target 目标属性映射 / target property map
     * @param directory 目录路径 / directory path
     * @param legacyPrefix 遗留键前缀 / legacy key prefix
     */
    private void loadDirectory(Map<String, Object> target, Path directory, String legacyPrefix) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.list(directory)) {
            paths
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".properties"))
                .sorted(Comparator.comparing(Path::toString))
                .forEach(path -> loadFile(target, path, legacyPrefix));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load legacy properties from " + directory, e);
        }
    }

    /**
     * 读取单个 properties 文件并写入目标映射。
     * Reads a single properties file into the target map.
     *
     * @param target 目标属性映射 / target property map
     * @param file 文件路径 / file path
     * @param legacyPrefix 遗留键前缀 / legacy key prefix
     */
    private void loadFile(Map<String, Object> target, Path file, String legacyPrefix) {
        loadFile(target, file, legacyPrefix, true);
    }

    private void loadFile(Map<String, Object> target, Path file, String legacyPrefix, boolean includeRawProperty) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(file)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load legacy properties from " + file, e);
        }
        properties.forEach((key, value) -> {
            String propertyKey = String.valueOf(key);
            if (includeRawProperty) {
                target.put(propertyKey, value);
            }
            target.put(legacyPrefix + propertyKey, value);
        });
    }
}
