package com.aionemu.boot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class AionLegacyPropertySourceEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

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
        assertEquals("2206", environment.getProperty("loginserver.network.client.port"));
        assertEquals("2", environment.getProperty("chatserver.chat.lang"));
        assertEquals("3", environment.getProperty("aion.legacy.game.property.gameserver.network.login.gsid"));
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
}
