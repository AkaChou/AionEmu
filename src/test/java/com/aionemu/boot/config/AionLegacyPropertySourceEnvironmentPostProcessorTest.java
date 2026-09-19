package com.aionemu.boot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import com.aionemu.commons.configs.DatabaseConfig;
import com.aionemu.commons.configuration.ConfigSourceResolverHolder;
import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.loginserver.configs.SvStatsConfig;
import com.aionemu.testutil.ConfigSnapshot;

class AionLegacyPropertySourceEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearPublishedResolver() {
        ConfigSourceResolverHolder.publish(null);
    }

    @Test
    void loadsExistingPropertiesFilesIntoSpringEnvironmentWithoutChangingPrecedence() throws Exception {
        Path configDir = tempDir.resolve("config");
        Path loginConfig = configDir.resolve("login");
        Path chatConfig = configDir.resolve("chat");
        Files.createDirectories(configDir.resolve("main"));
        Files.createDirectories(configDir.resolve("network"));
        Files.createDirectories(loginConfig);
        Files.createDirectories(chatConfig);

        Files.writeString(configDir.resolve("main/main.properties"), """
            gameserver.name=from-game-main
            gameserver.network.login.gsid=1
            """);
        Files.writeString(configDir.resolve("network/network.properties"), """
            gameserver.network.login.gsid=2
            loginserver.network.client.port=2106
            chatserver.network.public.address=203.0.113.30:10241
            """);
        Files.writeString(configDir.resolve("mygs.properties"), """
            gameserver.network.login.gsid=3
            """);
        Files.writeString(loginConfig.resolve("myls.properties"), """
            loginserver.network.client.port=2206
            """);
        Files.writeString(chatConfig.resolve("chatserver.properties"), """
            chatserver.chat.lang=1
            """);
        Files.writeString(chatConfig.resolve("mycs.properties"), """
            chatserver.chat.lang=2
            """);

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
            "commandLine",
            Map.of(
                "aion.config.dir", configDir.toString(),
                "gameserver.name", "from-command-line"
            )
        ));

        new AionLegacyPropertySourceEnvironmentPostProcessor()
            .postProcessEnvironment(environment, new SpringApplication());

        assertEquals("from-command-line", environment.getProperty("gameserver.name"));
        assertEquals("3", environment.getProperty("gameserver.network.login.gsid"));
        // 两个服务为同一原始键给出不同值时，扁平镜像不发布该键，各服务的前缀键保留自己的值。
        // When two services disagree on one raw key the flat mirror publishes neither, and each
        // prefixed key keeps its own value.
        assertNull(environment.getProperty("loginserver.network.client.port"));
        assertEquals("2", environment.getProperty("chatserver.chat.lang"));
        assertEquals("3", environment.getProperty("aion.legacy.game.property.gameserver.network.login.gsid"));
        assertEquals("2106", environment.getProperty("aion.legacy.game.property.loginserver.network.client.port"));
        assertEquals("2206", environment.getProperty("aion.legacy.login.property.loginserver.network.client.port"));
        assertEquals("2", environment.getProperty("aion.legacy.chat.property.chatserver.chat.lang"));
        assertEquals("203.0.113.30:10241", environment.getProperty("aion.legacy.chat.property.chatserver.network.public.address"));

        LegacyGameProperties gameProperties = bindLegacyGameProperties(environment);
        LegacyLoginProperties loginProperties = bindLegacyLoginProperties(environment);
        LegacyChatProperties chatProperties = bindLegacyChatProperties(environment);

        assertEquals("3", gameProperties.getProperty().get("gameserver.network.login.gsid"));
        assertEquals("2206", loginProperties.getProperty().get("loginserver.network.client.port"));
        assertEquals("2", chatProperties.getProperty().get("chatserver.chat.lang"));
    }

    /**
     * 回归：登录服与游戏服的 {@code database.properties} 同名不同值，镜像不得把登录服 URL 泄漏给游戏服。
     *
     * <p>启动路径上解析器（{@link BootConfigSourceResolver}）优先于本地文件，一旦扁平镜像里只剩下
     * 登录服的 {@code database.url}，游戏服连接池就会指向 {@code al_server_ls}，随后所有玩家、背包、
     * 住宅、城镇表查询都会失败。因此这里同时断言镜像与遗留静态字段。</p>
     *
     * Regression: the login and game {@code database.properties} files define the same key with
     * different values, so the mirror must not leak the login-server URL into the game server.
     *
     * <p>The published resolver ({@link BootConfigSourceResolver}) outranks local files on the startup
     * path, so a flat mirror holding only the login-server {@code database.url} would point the game
     * connection pool at {@code al_server_ls} and break every player, inventory, house and town query.
     * The test therefore pins both the mirror and the legacy static fields.</p>
     *
     * @throws Exception 临时配置写入失败 / when writing the temporary configuration fails
     */
    @Test
    void keepsEachServicesOwnDatabaseUrlWhenLoginAndGameFilesDisagree() throws Exception {
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir.resolve("network"));
        Files.createDirectories(configDir.resolve("login"));
        Files.writeString(configDir.resolve("network/database.properties"), """
            database.url=jdbc:mysql://127.0.0.1:3306/al_server_gs
            database.user=root
            database.password=123456
            """);
        Files.writeString(configDir.resolve("login/database.properties"), """
            database.url=jdbc:mysql://127.0.0.1:3306/al_server_ls
            database.user=root
            database.password=123456
            """);

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
            "commandLine",
            Map.of("aion.config.dir", configDir.toString())
        ));
        new AionLegacyPropertySourceEnvironmentPostProcessor()
            .postProcessEnvironment(environment, new SpringApplication());

        assertNull(environment.getProperty("database.url"),
            "a raw key with two different service values must not be mirrored");
        assertEquals("jdbc:mysql://127.0.0.1:3306/al_server_gs",
            environment.getProperty("aion.legacy.game.property.database.url"));
        assertEquals("jdbc:mysql://127.0.0.1:3306/al_server_ls",
            environment.getProperty("aion.legacy.login.property.database.url"));
        // 两个文件里取值一致的键仍照常镜像。 / Keys the two files agree on stay mirrored.
        assertEquals("123456", environment.getProperty("database.password"));

        // 生产启动路径的桥接：每个服务的静态字段必须落回自己的文件值。
        // The startup bridge: each service's static field must fall back to its own file value.
        new BootConfigSourceResolver(environment).publishEnvironmentResolver();
        ConfigSnapshot snapshot = ConfigSnapshot.of(DatabaseConfig.class, "DATABASE_URL");
        try {
            ConfigurableProcessor.process(DatabaseConfig.class,
                propertiesOf("database.url", "jdbc:mysql://127.0.0.1:3306/al_server_gs"));
            assertEquals("jdbc:mysql://127.0.0.1:3306/al_server_gs", DatabaseConfig.DATABASE_URL,
                "the game-server file value must survive the published resolver");

            ConfigurableProcessor.process(DatabaseConfig.class,
                propertiesOf("database.url", "jdbc:mysql://127.0.0.1:3306/al_server_ls"));
            assertEquals("jdbc:mysql://127.0.0.1:3306/al_server_ls", DatabaseConfig.DATABASE_URL,
                "the login-server file value must survive the published resolver");
        } finally {
            snapshot.restore();
        }
    }

    @Test
    void postProcessorIsRegisteredForBootStartup() throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("META-INF/spring.factories")) {
            assertTrue(new String(stream.readAllBytes()).contains(
                "com.aionemu.boot.config.AionLegacyPropertySourceEnvironmentPostProcessor"
            ));
        }
    }

    @Test
    void loadsDefaultConfigDirectoriesFromAionHome() throws Exception {
        Path gameConfig = tempDir.resolve("aion/config/main");
        Files.createDirectories(gameConfig);
        Files.writeString(gameConfig.resolve("gameserver.properties"), "gameserver.country.code=5\n");

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
            "commandLine",
            Map.of("aion.home", tempDir.resolve("aion").toString())
        ));

        new AionLegacyPropertySourceEnvironmentPostProcessor()
            .postProcessEnvironment(environment, new SpringApplication());

        assertEquals("5", environment.getProperty("gameserver.country.code"));
    }


    /**
     * 配置路径保持现状：{@code aion/config/main/main.properties} 里的点号长键必须既能进入 Spring
     * Environment，又能被 {@code @ConfigurationProperties} 配置类直接绑定（无需新建 application.yml 键，
     * 也无需移动任何配置文件）。
     *
     * Configuration paths stay as they are: dotted keys in
     * {@code aion/config/main/main.properties} must both reach the Spring Environment and bind straight
     * onto {@code @ConfigurationProperties} beans, without new application.yml keys or moved files.
     */
    @Test
    void legacyDottedKeysBindOntoConfigurationPropertiesFromUnchangedPaths() throws Exception {
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir.resolve("main"));
        Files.createDirectories(configDir.resolve("network"));
        Files.createDirectories(configDir.resolve("login"));
        Files.writeString(configDir.resolve("main/main.properties"), """
            gameserver.thread.basepoolsize=7
            gameserver.thread.threadpercore=2
            """);
        Files.writeString(configDir.resolve("login/loginserver.properties"), """
            svstats.enable_svstats=true
            """);

        ConfigSnapshot threadConfigSnapshot = ConfigSnapshot.of(ThreadConfig.class,
            "BASE_THREAD_POOL_SIZE", "EXTRA_THREAD_PER_CORE");
        ConfigSnapshot svStatsConfigSnapshot = ConfigSnapshot.of(SvStatsConfig.class, "SVSTATS_ENABLE");
        try {
            StandardEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().addFirst(new MapPropertySource(
                "commandLine",
                Map.of("aion.config.dir", configDir.toString())
            ));
            new AionLegacyPropertySourceEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

            ThreadConfig threadConfig = new ThreadConfig();
            Binder.get(environment).bind("gameserver.thread", Bindable.ofInstance(threadConfig));

            assertEquals(7, threadConfig.getBasepoolsize(),
                "dotted legacy key must bind through the configuration-properties setter");
            assertEquals(7, ThreadConfig.BASE_THREAD_POOL_SIZE,
                "binding must keep the static facade readable by non-bean callers");

            SvStatsConfig svStatsConfig = new SvStatsConfig();
            Binder.get(environment).bind("svstats", Bindable.ofInstance(svStatsConfig));

            assertEquals(true, svStatsConfig.isEnableSvstats());
            assertEquals(true, SvStatsConfig.SVSTATS_ENABLE);
        } finally {
            threadConfigSnapshot.restore();
            svStatsConfigSnapshot.restore();
        }
    }

    private LegacyGameProperties bindLegacyGameProperties(StandardEnvironment environment) {
        LegacyGameProperties properties = new LegacyGameProperties();
        Binder.get(environment).bind("aion.legacy.game", Bindable.ofInstance(properties));
        return properties;
    }

    private LegacyLoginProperties bindLegacyLoginProperties(StandardEnvironment environment) {
        LegacyLoginProperties properties = new LegacyLoginProperties();
        Binder.get(environment).bind("aion.legacy.login", Bindable.ofInstance(properties));
        return properties;
    }

    private LegacyChatProperties bindLegacyChatProperties(StandardEnvironment environment) {
        LegacyChatProperties properties = new LegacyChatProperties();
        Binder.get(environment).bind("aion.legacy.chat", Bindable.ofInstance(properties));
        return properties;
    }

    /**
     * 构造单键覆盖属性，模拟某一个服务自己加载的遗留属性。
     * Builds a single-key override set standing for the legacy properties one service loads itself.
     *
     * @param key 配置键 / configuration key
     * @param value 配置值 / configuration value
     * @return 属性集 / properties
     */
    private static Properties propertiesOf(String key, String value) {
        Properties properties = new Properties();
        properties.setProperty(key, value);
        return properties;
    }
}
