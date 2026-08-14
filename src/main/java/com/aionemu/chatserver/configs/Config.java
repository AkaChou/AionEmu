package com.aionemu.chatserver.configs;


import com.aionemu.boot.i18n.I18n;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.configs.CommonsConfig;
import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.commons.configuration.Property;
import com.aionemu.commons.utils.PropertiesUtils;

/**
 * 聊天服务器静态配置项与从文件加载的入口。
 * Static chat-server configuration properties and file-loading entry point.
 *
 * @author ATracer
 */
@Slf4j
@UtilityClass
public class Config {

    private static volatile Properties bootOverrides = new Properties();

    /**
     * 聊天服客户端监听地址。
     * Chat-server client listen address.
     */
    @Property(key = "chatserver.network.client.address", defaultValue = "localhost:10241")
    public static InetSocketAddress CHAT_ADDRESS;

    /**
     * 向游戏客户端公布的聊天服务器地址。
     * Chat-server address advertised to game clients.
     */
    @Property(key = "chatserver.network.public.address", defaultValue = "localhost:10241")
    public static InetSocketAddress PUBLIC_CHAT_ADDRESS;

    /**
     * 游戏服对接监听地址。
     * Game-server connection listen address.
     */
    @Property(key = "chatserver.network.gameserver.address", defaultValue = "localhost:9021")
    public static InetSocketAddress GAME_ADDRESS;

    /**
     * 游戏服认证密码。
     * Password for game-server authentication.
     */
    @Property(key = "chatserver.network.gameserver.password", defaultValue = "*")
    public static String GAME_SERVER_PASSWORD;

    /**
     * 是否记录新建频道请求。
     * Whether to log requests for new channels.
     */
    @Property(key = "chatserver.log.channel.request", defaultValue = "false")
    public static boolean LOG_CHANNEL_REQUEST;

    /**
     * 是否记录无效频道请求。
     * Whether to log requests for invalid channels.
     */
    @Property(key = "chatserver.log.channel.invalid", defaultValue = "false")
    public static boolean LOG_CHANNEL_INVALID;

    /**
     * 是否记录聊天内容。
     * Whether to log chat messages.
     */
    @Property(key = "chatserver.log.chat", defaultValue = "false")
    public static boolean LOG_CHAT;

    /**
     * 聊天语言码。
     * Chat language code.
     */
    @Property(key = "chatserver.chat.lang", defaultValue = "1")
    public static int LANG_CHAT;

    /**
     * 聊天消息发送延迟（秒等配置单位以属性定义为准）。
     * Chat message send delay (unit per property definition).
     */
    @Property(key = "chatserver.chat.message.delay", defaultValue = "30")
    public static int MESSAGE_DELAY;

    /**
     * 聊天服定时重启频率。
     * Scheduled chat-server restart frequency.
     */
    @Property(key = "chatserver.restart.frequency", defaultValue = "NEVER")
    public static String CHATSERVER_RESTART_FREQUENCY;

    /**
     * 在重启频率约束下的具体重启时刻。
     * Exact time of day for restart under the configured frequency.
     */
    @Property(key = "chatserver.restart.time", defaultValue = "5:00")
    public static String CHATSERVER_RESTART_TIME;

    /**
     * 解析统一配置根目录。
     * Resolves the shared configuration root directory.
     */
    private static String configDir() {
        return Objects.requireNonNull(System.getProperty("aion.config.dir"), "aion.config.dir is not configured");
    }

    /**
     * 设置 boot 层覆盖属性（会复制一份）。
     * Set boot-layer override properties (copied defensively).
     *
     * @param properties 覆盖属性，可为 null / Override properties, may be null
     */
    public static void setBootOverrides(Properties properties) {
        Properties copy = new Properties();
        if (properties != null) {
            copy.putAll(properties);
        }
        bootOverrides = copy;
    }

    /**
     * 从配置目录加载并处理 commons 与本类配置。
     * Load and process commons and this class configuration from the config directory.
     */
    public static void load() {
        try {

            Properties myProps = null;
            try {
                log.info(I18n.get("log.42a96ab0c032"));
                myProps = PropertiesUtils.load(configDir() + "/chat/mycs.properties");
            } catch (Exception e) {
                log.info(I18n.get("log.c453f21e95f6"));
            }

            Properties[] props = loadProperties();
            PropertiesUtils.overrideProperties(props, myProps);
            PropertiesUtils.overrideProperties(props, bootOverrides);

            log.info(I18n.get("log.985e991606f9"));
            ConfigurableProcessor.process(CommonsConfig.class, props);
            log.info(I18n.get("log.2d8e1d8b4e5a"));
            ConfigurableProcessor.process(Config.class, props);
        } catch (Exception e) {
            log.error(I18n.get("log.7e1c21e90609", e));
            throw new Error("Can't load chatserver configuration", e);
        }
    }

    private static Properties[] loadProperties() throws IOException {
        Properties[] serviceProperties = PropertiesUtils.loadAllFromDirectory(Path.of(configDir()).resolve("chat").toFile());
        Properties[] properties = new Properties[serviceProperties.length + 1];
        properties[0] = PropertiesUtils.load(
            Path.of(configDir()).resolve("network/network.properties").toFile()
        );
        System.arraycopy(serviceProperties, 0, properties, 1, serviceProperties.length);
        return properties;
    }
}
