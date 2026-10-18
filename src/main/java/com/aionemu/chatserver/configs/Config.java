package com.aionemu.chatserver.configs;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.configs.CommonsConfig;
import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.commons.configuration.Property;
import com.aionemu.commons.configuration.transformers.InetSocketAddressTransformer;
import com.aionemu.commons.configuration.TransformationException;
import com.aionemu.commons.utils.PropertiesUtils;

/**
 * 聊天服务器静态配置项与从文件加载的入口，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Static chat-server configuration properties and file-loading entry point, supporting Spring Boot
 * ConfigurationProperties and static facade access.
 * @author ATracer
 */
@Slf4j
@Component("chatConfig")
@ConfigurationProperties(prefix = "chatserver")
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

    @Getter
    private final NetworkProperties network = new NetworkProperties();
    private final LogProperties logProperties = new LogProperties();
    @Getter
    private final ChatProperties chat = new ChatProperties();
    @Getter
    private final RestartProperties restart = new RestartProperties();

	/**
     * no-op：绑定值经嵌套属性直接写入静态字段，防止重复绑定覆盖。
     * no-op: bound values reach the static fields via the nested properties,
     * so re-binding must not overwrite them.
     * @param network 绑定产生的嵌套实例 / nested instance produced by binding
     */
    public void setNetwork(NetworkProperties network) {
    }

    public LogProperties getLog() {
        return logProperties;
    }

    /** 同 {@link #setNetwork}。 / See {@link #setNetwork}. */
    public void setLog(LogProperties logProperties) {
    }

	/** 同 {@link #setNetwork}。 / See {@link #setNetwork}. */
    public void setChat(ChatProperties chat) {
    }

	/** 同 {@link #setNetwork}。 / See {@link #setNetwork}. */
    public void setRestart(RestartProperties restart) {
    }

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
                log.debug(I18n.get("log.42a96ab0c032"));
                myProps = PropertiesUtils.load(configDir() + "/chat/mycs.properties");
            } catch (Exception e) {
                log.info(I18n.get("log.c453f21e95f6"));
            }

            Properties[] props = loadProperties();
            PropertiesUtils.overrideProperties(props, myProps);
            PropertiesUtils.overrideProperties(props, bootOverrides);

            log.debug(I18n.get("log.985e991606f9"));
            ConfigurableProcessor.process(CommonsConfig.class, props);
            log.debug(I18n.get("log.2d8e1d8b4e5a"));
            ConfigurableProcessor.process(Config.class, props);
        } catch (Exception e) {
            log.error(I18n.get("log.7e1c21e90609"), e);
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

    /**
     * 解析遗留转换器同款语义的 "host:port" 地址串。
     * Parses an "host:port" string with the same semantics as the legacy transformer.
     * @param value 地址串 / address string
     * @return 解析结果 / parsed address
     */
    private static InetSocketAddress parseAddress(String value) {
        try {
            return InetSocketAddressTransformer.SHARED_INSTANCE.transform(value, null);
        } catch (TransformationException e) {
            throw new IllegalArgumentException("Invalid address: " + value, e);
        }
    }

    /**
     * {@code chatserver.network} 绑定节点。
     * {@code chatserver.network} binding node.
     */
    public static class NetworkProperties {

        @Getter
        private final ClientProperties client = new ClientProperties();
        private final PublicProperties publicAddress = new PublicProperties();
        @Getter
        private final GameserverProperties gameserver = new GameserverProperties();

		/** no-op，理由同 {@link Config#setNetwork}。 / no-op, see {@link Config#setNetwork}. */
        public void setClient(ClientProperties client) {
        }

        /**
         * 属性名必须为 {@code public}（键 {@code chatserver.network.public.*}），字段名规避关键字。
         * The property name must be {@code public} (keys {@code chatserver.network.public.*});
         * only the field name avoids the keyword.
         */
        public PublicProperties getPublic() {
            return publicAddress;
        }

        /** no-op，理由同 {@link Config#setNetwork}。 / no-op, see {@link Config#setNetwork}. */
        public void setPublic(PublicProperties publicAddress) {
        }

		/** no-op，理由同 {@link Config#setNetwork}。 / no-op, see {@link Config#setNetwork}. */
        public void setGameserver(GameserverProperties gameserver) {
        }

        /**
         * {@code chatserver.network.client} 绑定节点。
         * {@code chatserver.network.client} binding node.
         */
        public static class ClientProperties {

            public String getAddress() {
                return CHAT_ADDRESS.getHostString() + ":" + CHAT_ADDRESS.getPort();
            }

            public void setAddress(String address) {
                CHAT_ADDRESS = parseAddress(address);
            }
        }

        /**
         * {@code chatserver.network.public} 绑定节点（向客户端宣告的地址）。
         * {@code chatserver.network.public} binding node (the address advertised to clients).
         */
        public static class PublicProperties {

            public String getAddress() {
                return PUBLIC_CHAT_ADDRESS.getHostString() + ":" + PUBLIC_CHAT_ADDRESS.getPort();
            }

            public void setAddress(String address) {
                PUBLIC_CHAT_ADDRESS = parseAddress(address);
            }
        }

        /**
         * {@code chatserver.network.gameserver} 绑定节点。
         * {@code chatserver.network.gameserver} binding node.
         */
        public static class GameserverProperties {

            public String getAddress() {
                return GAME_ADDRESS.getHostString() + ":" + GAME_ADDRESS.getPort();
            }

            public void setAddress(String address) {
                GAME_ADDRESS = parseAddress(address);
            }

            public String getPassword() {
                return GAME_SERVER_PASSWORD;
            }

            public void setPassword(String password) {
                GAME_SERVER_PASSWORD = password;
            }
        }
    }

    /**
     * {@code chatserver.log} 绑定节点。
     * {@code chatserver.log} binding node.
     */
    @Getter
    public static class LogProperties {

        private final ChannelProperties channel = new ChannelProperties();

		/** no-op，理由同 {@link Config#setNetwork}。 / no-op, see {@link Config#setNetwork}. */
        public void setChannel(ChannelProperties channel) {
        }

        public boolean isChat() {
            return LOG_CHAT;
        }

        public void setChat(boolean chat) {
            LOG_CHAT = chat;
        }

        /**
         * {@code chatserver.log.channel} 绑定节点。
         * {@code chatserver.log.channel} binding node.
         */
        public static class ChannelProperties {

            public boolean isRequest() {
                return LOG_CHANNEL_REQUEST;
            }

            public void setRequest(boolean request) {
                LOG_CHANNEL_REQUEST = request;
            }

            public boolean isInvalid() {
                return LOG_CHANNEL_INVALID;
            }

            public void setInvalid(boolean invalid) {
                LOG_CHANNEL_INVALID = invalid;
            }
        }
    }

    /**
     * {@code chatserver.chat} 绑定节点。
     * {@code chatserver.chat} binding node.
     */
    @Getter
    public static class ChatProperties {

        private final MessageProperties message = new MessageProperties();

        public int getLang() {
            return LANG_CHAT;
        }

        public void setLang(int lang) {
            LANG_CHAT = lang;
        }

		/** no-op，理由同 {@link Config#setNetwork}。 / no-op, see {@link Config#setNetwork}. */
        public void setMessage(MessageProperties message) {
        }

        /**
         * {@code chatserver.chat.message} 绑定节点（点分子键必须显式嵌套，驼峰属性无法承接两段点分键）。
         * {@code chatserver.chat.message} binding node (dotted sub-keys require explicit nesting;
         * a camelCase property cannot absorb a two-segment dotted key).
         */
        public static class MessageProperties {

            public int getDelay() {
                return MESSAGE_DELAY;
            }

            public void setDelay(int delay) {
                MESSAGE_DELAY = delay;
            }
        }
    }

    /**
     * {@code chatserver.restart} 绑定节点。
     * {@code chatserver.restart} binding node.
     */
    public static class RestartProperties {

        public String getFrequency() {
            return CHATSERVER_RESTART_FREQUENCY;
        }

        public void setFrequency(String frequency) {
            CHATSERVER_RESTART_FREQUENCY = frequency;
        }

        public String getTime() {
            return CHATSERVER_RESTART_TIME;
        }

        public void setTime(String time) {
            CHATSERVER_RESTART_TIME = time;
        }
    }
}
