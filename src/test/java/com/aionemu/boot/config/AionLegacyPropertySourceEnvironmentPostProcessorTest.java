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
        Path gameConfig = tempDir.resolve("game");
        Path loginConfig = tempDir.resolve("login");
        Path chatConfig = tempDir.resolve("chat");
        Files.createDirectories(gameConfig.resolve("main"));
        Files.createDirectories(gameConfig.resolve("network"));
        Files.createDirectories(loginConfig.resolve("network"));
        Files.createDirectories(chatConfig);

        Files.writeString(gameConfig.resolve("main/main.properties"), """
            gameserver.name=from-game-main
            gameserver.network.login.gsid=1
            """);
        Files.writeString(gameConfig.resolve("network/network.properties"), """
            gameserver.network.login.gsid=2
            """);
        Files.writeString(gameConfig.resolve("mygs.properties"), """
            gameserver.network.login.gsid=3
            """);
        Files.writeString(loginConfig.resolve("network/network.properties"), """
            loginserver.network.client.port=2106
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
                "aion.game.config.dir", gameConfig.toString(),
                "aion.login.config.dir", loginConfig.toString(),
                "aion.chat.config.dir", chatConfig.toString(),
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
