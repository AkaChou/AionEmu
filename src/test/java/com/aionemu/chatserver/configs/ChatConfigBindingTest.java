package com.aionemu.chatserver.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import com.aionemu.testutil.ConfigSnapshot;

/**
 * 聊天服配置绑定验证：Binder 侧与遗留装配必须收敛到同一组静态门面字段。
 * Chat-server configuration binding check: Binder binding and the legacy assembly must converge
 * onto the same set of static facade fields.
 */
class ChatConfigBindingTest {

    @TempDir
    Path tempDir;

    private final ConfigSnapshot snapshot = ConfigSnapshot.of(Config.class,
        "CHAT_ADDRESS", "PUBLIC_CHAT_ADDRESS", "GAME_ADDRESS", "GAME_SERVER_PASSWORD",
        "LOG_CHANNEL_REQUEST", "LOG_CHANNEL_INVALID", "LOG_CHAT",
        "LANG_CHAT", "MESSAGE_DELAY", "CHATSERVER_RESTART_FREQUENCY", "CHATSERVER_RESTART_TIME");

    @AfterEach
    void resetConfig() {
        System.clearProperty("aion.config.dir");
        Config.setBootOverrides(null);
        snapshot.restore();
    }

    /**
     * Binder 侧绑定镜像键时必须经嵌套属性写入静态门面字段（含 host:port 地址解析）。
     * Binding mirrored keys through the Binder must reach the static facade fields via the nested
     * properties (including host:port address parsing).
     */
    @Test
    void binderBindingWritesStaticFacadeFields() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
            "chatserver.network.client.address", "0.0.0.0:10241",
            "chatserver.network.public.address", "10.0.0.5:10241",
            "chatserver.network.gameserver.address", "127.0.0.1:9021",
            "chatserver.network.gameserver.password", "secret",
            "chatserver.log.channel.request", "true",
            "chatserver.log.chat", "true",
            "chatserver.chat.lang", "3",
            "chatserver.chat.message.delay", "45",
            "chatserver.restart.frequency", "DAILY",
            "chatserver.restart.time", "4:30"
        )));

        Binder.get(environment).bind("chatserver", Bindable.ofInstance(new Config()));

        assertEquals(new InetSocketAddress("0.0.0.0", 10241), Config.CHAT_ADDRESS);
        assertEquals(new InetSocketAddress("10.0.0.5", 10241), Config.PUBLIC_CHAT_ADDRESS);
        assertEquals(new InetSocketAddress("127.0.0.1", 9021), Config.GAME_ADDRESS);
        assertEquals("secret", Config.GAME_SERVER_PASSWORD);
        assertTrue(Config.LOG_CHANNEL_REQUEST);
        assertTrue(Config.LOG_CHAT);
        assertEquals(3, Config.LANG_CHAT);
        assertEquals(45, Config.MESSAGE_DELAY);
        assertEquals("DAILY", Config.CHATSERVER_RESTART_FREQUENCY);
        assertEquals("4:30", Config.CHATSERVER_RESTART_TIME);
    }

    /**
     * 遗留装配沿裸键（无子键冲突的既有形态）取值，行为保持不变。
     * The legacy assembly keeps reading the existing bare keys (no scalar-plus-sub-key conflicts).
     */
    @Test
    void legacyLoadReadsBareKeysUnchanged() throws Exception {
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir.resolve("chat"));
        Files.createDirectories(configDir.resolve("network"));
        Files.writeString(configDir.resolve("network/network.properties"), """
            chatserver.network.client.address = 0.0.0.0:10241
            chatserver.network.public.address = 10.0.0.5:10241
            chatserver.network.gameserver.password = secret
            """);
        Files.writeString(configDir.resolve("chat/chatserver.properties"), """
            chatserver.log.channel.request = true
            chatserver.log.chat = true
            chatserver.chat.lang = 3
            chatserver.chat.message.delay = 45
            chatserver.restart.frequency = DAILY
            chatserver.restart.time = 4:30
            """);
        System.setProperty("aion.config.dir", configDir.toString());

        Config.load();

        assertEquals(new InetSocketAddress("0.0.0.0", 10241), Config.CHAT_ADDRESS);
        assertEquals(new InetSocketAddress("10.0.0.5", 10241), Config.PUBLIC_CHAT_ADDRESS);
        assertEquals("secret", Config.GAME_SERVER_PASSWORD);
        assertTrue(Config.LOG_CHANNEL_REQUEST);
        assertTrue(Config.LOG_CHAT);
        assertEquals(3, Config.LANG_CHAT);
        assertEquals(45, Config.MESSAGE_DELAY);
        assertEquals("DAILY", Config.CHATSERVER_RESTART_FREQUENCY);
        assertEquals("4:30", Config.CHATSERVER_RESTART_TIME);
    }
}
