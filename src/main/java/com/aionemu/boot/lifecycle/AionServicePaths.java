package com.aionemu.boot.lifecycle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

final class AionServicePaths {

    private AionServicePaths() {
    }

    static void configureLogin() {
        configureLogging();
        configureConfig("aion.login.config.dir", "login/config", "aion/login/config");
        configureResourceDirectory("aion.login.data.dir", "login/data", "aion/login/data");
    }

    static void configureChat() {
        configureLogging();
        configureConfig("aion.chat.config.dir", "chat/config", "aion/chat/config");
    }

    static void configureGame() {
        configureLogging();
        configureConfig("aion.game.config.dir", "game/config", "aion/game/config");
        configureGameData("aion.game.data.dir", "game/data", "aion/game/data");
        configureDirectory("aion.game.cache.dir", "game/cache");
    }

    private static void configureLogging() {
        String property = "aion.logging.config";
        if (System.getProperty(property) != null) {
            return;
        }

        String resourcePath = "logback-spring.xml";
        Path sourceFile = Path.of(System.getProperty("aion.home", "."))
            .resolve("src/main/resources")
            .resolve(resourcePath)
            .normalize();
        if (Files.isRegularFile(sourceFile)) {
            System.setProperty(property, sourceFile.toString());
            return;
        }

        Path targetFile = Path.of(System.getProperty("aion.home", "."))
            .resolve(resourcePath)
            .normalize();
        System.setProperty(property, targetFile.toString());
        materializeDefaultFile(resourcePath, targetFile);
    }

    private static void configure(String property, String defaultPath) {
        if (System.getProperty(property) != null) {
            return;
        }

        String home = System.getProperty("aion.home", ".");
        System.setProperty(property, Path.of(home).resolve(defaultPath).normalize().toString());
    }

    private static void configureConfig(String property, String defaultPath, String resourcePath) {
        if (configureSourceResourceDirectory(property, resourcePath)) {
            return;
        }
        configureResourceDirectory(property, defaultPath, resourcePath);
    }

    private static void configureResourceDirectory(String property, String defaultPath, String resourcePath) {
        boolean explicit = System.getProperty(property) != null;
        configure(property, defaultPath);
        if (!explicit) {
            materializeDefaults(resourcePath, Path.of(System.getProperty(property)));
        }
    }

    private static void configureGameData(String property, String defaultPath, String resourcePath) {
        if (configureSourceResourceDirectory(property, resourcePath)) {
            return;
        }

        configureResourceDirectory(property, defaultPath, resourcePath);
    }

    private static boolean configureSourceResourceDirectory(String property, String resourcePath) {
        if (System.getProperty(property) != null) {
            return true;
        }

        Path sourceDirectory = Path.of(System.getProperty("aion.home", "."))
            .resolve("src/main/resources")
            .resolve(resourcePath)
            .normalize();
        if (!Files.isDirectory(sourceDirectory)) {
            return false;
        }

        System.setProperty(property, sourceDirectory.toString());
        return true;
    }

    private static void configureDirectory(String property, String defaultPath) {
        configure(property, defaultPath);
        try {
            Files.createDirectories(Path.of(System.getProperty(property)));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare runtime directory " + System.getProperty(property), e);
        }
    }

    private static void materializeDefaults(String resourcePath, Path targetDirectory) {
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(AionServicePaths.class.getClassLoader());
            Resource[] resources = resolver.getResources("classpath*:" + resourcePath + "/**/*");
            for (Resource resource : resources) {
                copyResource(resourcePath, resource, targetDirectory);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare default configuration from " + resourcePath, e);
        }
    }

    private static void materializeDefaultFile(String resourcePath, Path targetFile) {
        if (Files.exists(targetFile)) {
            return;
        }
        try (InputStream inputStream = AionServicePaths.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Default configuration resource not found: " + resourcePath);
            }
            Path parent = targetFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(inputStream, targetFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare default configuration from " + resourcePath, e);
        }
    }

    private static void copyResource(String resourcePath, Resource resource, Path targetDirectory) throws IOException {
        if (!resource.isReadable() || resource.getFilename() == null) {
            return;
        }
        if (resource.isFile() && Files.isDirectory(resource.getFile().toPath())) {
            return;
        }

        String relativePath = relativePath(resourcePath, resource.getURL());
        if (relativePath == null || relativePath.isEmpty() || relativePath.endsWith("/")) {
            return;
        }

        Path target = targetDirectory.resolve(relativePath).normalize();
        if (!target.startsWith(targetDirectory.normalize()) || Files.exists(target)) {
            return;
        }
        Files.createDirectories(target.getParent());
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, target);
        }
    }

    private static String relativePath(String resourcePath, URL url) {
        String external = url.toExternalForm();
        String marker = resourcePath + "/";
        int index = external.indexOf(marker);
        if (index < 0) {
            return null;
        }
        return URLDecoder.decode(external.substring(index + marker.length()), StandardCharsets.UTF_8);
    }
}
