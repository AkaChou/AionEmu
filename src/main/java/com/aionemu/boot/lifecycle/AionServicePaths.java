package com.aionemu.boot.lifecycle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.experimental.UtilityClass;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * 内嵌 login/chat/game 运行时路径与默认资源落盘工具。
 * Utility for embedded login/chat/game runtime paths and default resource materialization.
 */
@UtilityClass
class AionServicePaths {

    private final RuntimeProperties RUNTIME_PROPERTIES = new RuntimeProperties();

    /**
     * 配置登录服日志、配置目录与数据目录。
     * Configures login-server logging, config directory, and data directory.
     */
    void configureLogin() {
        configureLogging();
        configureConfig("aion.login.config.dir", "login/config", "aion/login/config");
        configureResourceDirectory("aion.login.data.dir", "login/data", "aion/login/data");
    }

    /**
     * 配置聊天服日志与配置目录。
     * Configures chat-server logging and config directory.
     */
    void configureChat() {
        configureLogging();
        configureConfig("aion.chat.config.dir", "chat/config", "aion/chat/config");
    }

    /**
     * 配置游戏服日志、配置、数据、地理与缓存目录。
     * Configures game-server logging, config, data, geo, and cache directories.
     */
    void configureGame() {
        configureLogging();
        configureConfig("aion.game.config.dir", "game/config", "aion/game/config");
        configureGameData("aion.game.data.dir", "game/data", "aion/game/data");
        configureGeoData("aion.game.geo.dir", "game/geo", "aion/game/geo");
        configureDirectory("aion.game.cache.dir", "game/cache");
    }

    /**
     * 若未显式配置，则准备默认 logback 配置文件路径并落盘。
     * Prepares default logback config path and materializes the file when unset.
     */
    private void configureLogging() {
        String property = "aion.logging.config";
        if (RUNTIME_PROPERTIES.has(property)) {
            return;
        }

        String resourcePath = "logback-spring.xml";
        Path targetFile = RUNTIME_PROPERTIES.resolveHome("log").resolve(resourcePath).normalize();
        RUNTIME_PROPERTIES.set(property, targetFile);
        materializeDefaultFile(resourcePath, targetFile);
    }

    /**
     * 若系统属性未设置，则写入基于 aion.home 的默认路径。
     * Sets a home-relative default path when the system property is absent.
     *
     * @param property 系统属性名 / system property name
     * @param defaultPath 相对 aion.home 的默认路径 / default path relative to aion.home
     */
    private void configure(String property, String defaultPath) {
        if (RUNTIME_PROPERTIES.has(property)) {
            return;
        }

        RUNTIME_PROPERTIES.set(property, RUNTIME_PROPERTIES.resolveHome(defaultPath));
    }

    /**
     * 配置配置目录：未显式设置时从 classpath 资源物化默认文件。
     * Configures a config directory; materializes classpath defaults when not explicit.
     *
     * @param property 系统属性名 / system property name
     * @param defaultPath 默认目录路径 / default directory path
     * classpath resource root
     */
    private void configureConfig(String property, String defaultPath, String resourcePath) {
        if (RUNTIME_PROPERTIES.has(property)) {
            return;
        }
        configureResourceDirectory(property, defaultPath, resourcePath);
    }

    /**
     * 配置资源目录并在非显式路径时物化默认资源树。
     * Configures a resource directory and materializes defaults when the path is not explicit.
     *
     * @param property 系统属性名 / system property name
     * @param defaultPath 默认目录路径 / default directory path
     * classpath resource root
     */
    private void configureResourceDirectory(String property, String defaultPath, String resourcePath) {
        boolean explicit = RUNTIME_PROPERTIES.has(property);
        configure(property, defaultPath);
        if (!explicit) {
            materializeDefaults(resourcePath, Path.of(RUNTIME_PROPERTIES.get(property)));
        }
    }

    /**
     * 优先使用源码树中的 game 数据目录，否则回退到资源物化。
     * Prefers checkout game data under source tree; otherwise falls back to resource materialization.
     *
     * @param property 系统属性名 / system property name
     * @param defaultPath 默认目录路径 / default directory path
     * classpath resource root
     */
    private void configureGameData(String property, String defaultPath, String resourcePath) {
        if (configureSourceResourceDirectory(property, resourcePath)) {
            return;
        }

        configureResourceDirectory(property, defaultPath, resourcePath);
    }

    /**
     * 优先使用源码树中的 geo 目录，否则仅设置默认路径（不强制物化）。
     * Prefers checkout geo under source tree; otherwise only sets the default path (no forced materialize).
     *
     * @param property 系统属性名 / system property name
     * @param defaultPath 默认目录路径 / default directory path
     * classpath resource root
     */
    private void configureGeoData(String property, String defaultPath, String resourcePath) {
        if (!configureSourceResourceDirectory(property, resourcePath)) {
            configure(property, defaultPath);
        }
    }

    /**
     * 若存在源码 resources 下的目录，则直接指向该路径。
     * Points the property at a source-tree resources directory when present.
     *
     * @param property 系统属性名 / system property name
     * @param resourcePath 资源相对路径 / resource-relative path
     * @return 已配置源码路径则为 true / true if a source path was configured
     */
    private boolean configureSourceResourceDirectory(String property, String resourcePath) {
        if (RUNTIME_PROPERTIES.has(property)) {
            return true;
        }

        if (!RUNTIME_PROPERTIES.hasHome()) {
            Path checkoutSourceDirectory = Path.of("src/main/resources").resolve(resourcePath).normalize();
            if (Files.isDirectory(checkoutSourceDirectory)) {
                RUNTIME_PROPERTIES.set(property, checkoutSourceDirectory);
                return true;
            }
        }

        Path sourceDirectory = RUNTIME_PROPERTIES.resolveHome("src/main/resources").resolve(resourcePath).normalize();
        if (Files.isDirectory(sourceDirectory)) {
            RUNTIME_PROPERTIES.set(property, sourceDirectory);
            return true;
        }
        return false;
    }

    /**
     * 配置目录属性并确保目录存在。
     * Configures a directory property and ensures the directory exists.
     *
     * @param property 系统属性名 / system property name
     * @param defaultPath 默认目录路径 / default directory path
     */
    private void configureDirectory(String property, String defaultPath) {
        configure(property, defaultPath);
        try {
            Files.createDirectories(Path.of(RUNTIME_PROPERTIES.get(property)));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare runtime directory " + RUNTIME_PROPERTIES.get(property), e);
        }
    }

    /**
     * 将 classpath 下 resourcePath 树中的文件复制到目标目录（已存在则跳过）。
     * Copies files under a classpath resourcePath tree into the target directory (skip existing).
     *
     * classpath resource root
     * target directory
     */
    private void materializeDefaults(String resourcePath, Path targetDirectory) {
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

    /**
     * 若目标文件不存在，则从 classpath 复制单个默认配置文件。
     * Copies a single default config file from the classpath when the target is missing.
     *
     * classpath resource path
     * target file
     */
    private void materializeDefaultFile(String resourcePath, Path targetFile) {
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

    /**
     * 将单个资源复制到目标目录中的相对路径位置。
     * Copies one resource into the target directory at its relative path.
     *
     * @param resourcePath 资源根路径 / resource root path
     * Spring resource
     * target directory
     * if I/O fails。 / if I/O fails.
     */
    private void copyResource(String resourcePath, Resource resource, Path targetDirectory) throws IOException {
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

    /**
     * 从资源 URL 中截取相对 resourcePath 的路径片段。
     * Extracts the path segment relative to resourcePath from a resource URL.
     *
     * @param resourcePath 资源根路径 / resource root path
     * resource URL
     * @return 相对路径，无法解析则为 null / relative path, or null if unresolvable
     */
    private String relativePath(String resourcePath, URL url) {
        String external = url.toExternalForm();
        String marker = resourcePath + "/";
        int index = external.indexOf(marker);
        if (index < 0) {
            return null;
        }
        return URLDecoder.decode(external.substring(index + marker.length()), StandardCharsets.UTF_8);
    }

    /**
     * 封装 aion.home 与相关系统属性的读写。
     * Encapsulates reads/writes of aion.home and related system properties.
     */
    private static final class RuntimeProperties {

        private static final String AION_HOME = "aion.home";

        /**
         * 是否已设置 aion.home。
         * Whether aion.home is set.
         *
         * @return 已设置则为 true / true if set
         */
        private boolean hasHome() {
            return System.getProperty(AION_HOME) != null;
        }

        /**
         * 是否已设置指定系统属性。
         * Whether the given system property is set.
         *
         * property name
         *
         * @param property @return 已设置则为 true / true if set
         */
        private boolean has(String property) {
            return System.getProperty(property) != null;
        }

        /**
         * 读取系统属性值。
         * Reads a system property value.
         *
         * property name
         * property value
         */
        private String get(String property) {
            return System.getProperty(property);
        }

        /**
         * 将路径规范化后写入系统属性。
         * Writes a normalized path into a system property.
         *
         * property name
         * path value
         */
        private void set(String property, Path value) {
            System.setProperty(property, value.normalize().toString());
        }

        /**
         * 基于 aion.home（默认 aion）解析相对路径。
         * Resolves a path under aion.home (default aion).
         *
         * @param path 相对路径 / relative path
         * @return 规范化绝对 / 相对结果路径 / normalized resolved path
         */
        private Path resolveHome(String path) {
            return Path.of(System.getProperty(AION_HOME, "aion")).resolve(path).normalize();
        }
    }
}
