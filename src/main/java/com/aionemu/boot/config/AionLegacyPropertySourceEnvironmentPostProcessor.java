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

public class AionLegacyPropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "aionLegacyProperties";

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

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 20;
    }

    private void loadGameProperties(ConfigurableEnvironment environment, Map<String, Object> target) {
        Path configDir = configDir(environment, "aion.game.config.dir");
        loadDirectory(target, configDir.resolve("administration"));
        loadDirectory(target, configDir.resolve("main"));
        loadDirectory(target, configDir.resolve("network"));
        loadFile(target, configDir.resolve("mygs.properties"));
    }

    private void loadLoginProperties(ConfigurableEnvironment environment, Map<String, Object> target) {
        Path configDir = configDir(environment, "aion.login.config.dir");
        loadDirectory(target, configDir.resolve("network"));
        loadFile(target, configDir.resolve("myls.properties"));
    }

    private void loadChatProperties(ConfigurableEnvironment environment, Map<String, Object> target) {
        Path configDir = configDir(environment, "aion.chat.config.dir");
        loadDirectory(target, configDir);
        loadFile(target, configDir.resolve("mycs.properties"));
    }

    private Path configDir(ConfigurableEnvironment environment, String propertyName) {
        return Path.of(environment.getProperty(propertyName, "./config"));
    }

    private void loadDirectory(Map<String, Object> target, Path directory) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.list(directory)) {
            paths
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".properties"))
                .sorted(Comparator.comparing(Path::toString))
                .forEach(path -> loadFile(target, path));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load legacy properties from " + directory, e);
        }
    }

    private void loadFile(Map<String, Object> target, Path file) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(file)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load legacy properties from " + file, e);
        }
        properties.forEach((key, value) -> target.put(String.valueOf(key), value));
    }
}
