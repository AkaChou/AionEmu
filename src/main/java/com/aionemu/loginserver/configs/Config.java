package com.aionemu.loginserver.configs;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.configs.CommonsConfig;
import com.aionemu.commons.configs.DatabaseConfig;
import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.commons.configuration.Property;
import com.aionemu.commons.utils.PropertiesUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 登录服配置项与加载逻辑，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * LoginServer configuration properties and loader, supporting Spring Boot ConfigurationProperties and static facade access.
 * @author -Nemesiss-
 * @author SoulKeeper
 */
@Slf4j
@Component("loginConfig")
@ConfigurationProperties(prefix = "loginserver")
public class Config {

    private static volatile Properties bootOverrides = new Properties();
    /**
     * 快速重连判定时间（秒）。
     * Fast reconnection threshold in seconds.
     */
    @Property(key = "network.fastreconnection.time", defaultValue = "10")
    public static int FAST_RECONNECTION_TIME;
    /**
     * 客户端监听端口。
     * Client listen port.
     */
    @Property(key = "loginserver.network.client.port", defaultValue = "2106")
    public static int LOGIN_PORT;
    /**
     * 客户端绑定地址。
     * Client bind address.
     */
    @Property(key = "loginserver.network.client.host", defaultValue = "localhost")
    public static String LOGIN_BIND_ADDRESS;
    /**
     * 游戏服监听端口。
     * GameServer listen port.
     */
    @Property(key = "loginserver.network.gameserver.port", defaultValue = "9014")
    public static int GAME_PORT;
    /**
     * 游戏服绑定地址。
     * GameServer bind address.
     */
    @Property(key = "loginserver.network.gameserver.host", defaultValue = "*")
    public static String GAME_BIND_ADDRESS;
    /**
     * 封禁前允许的失败登录次数。
     * Failed login attempts before ban.
     */
    @Property(key = "loginserver.network.client.logintrybeforeban", defaultValue = "5")
    public static int LOGIN_TRY_BEFORE_BAN;
    /**
     * 暴力破解封禁时长（分钟）。
     * Brute-force ban duration in minutes.
     */
    @Property(key = "loginserver.network.client.bantimeforbruteforcing", defaultValue = "15")
    public static int WRONG_LOGIN_BAN_TIME;
    /**
     * 是否自动创建账号。
     * Whether to auto-create accounts.
     */
    @Property(key = "loginserver.accounts.autocreate", defaultValue = "true")
    public static boolean ACCOUNT_AUTO_CREATION;
    /**
     * 是否维护模式。
     * Whether maintenance mode is enabled.
     */
    @Property(key = "loginserver.server.maintenance", defaultValue = "false")
    public static boolean MAINTENANCE_MOD;
    /**
     * 维护模式下允许登录的 GM 等级。
     * GM level allowed during maintenance.
     */
    @Property(key = "loginserver.server.maintenance.gmlevel", defaultValue = "3")
    public static int MAINTENANCE_MOD_GMLEVEL;
    /**
     * 是否启用同 IP 洪水防护。
     * Enable flood protection per IP on login.
     */
    @Property(key = "loginserver.server.floodprotector", defaultValue = "true")
    public static boolean ENABLE_FLOOD_PROTECTION;
    /**
     * 是否启用暴力破解防护。
     * Enable brute-force protection.
     */
    @Property(key = "loginserver.server.bruteforceprotector", defaultValue = "true")
    public static boolean ENABLE_BRUTEFORCE_PROTECTION;
    /**
     * 是否启用游戏服 ping/pong。
     * Enable GameServer ping/pong.
     */
    @Property(key = "loginserver.server.pingpong", defaultValue = "true")
    public static boolean ENABLE_PINGPONG;
    /**
     * ping/pong 间隔（毫秒）。
     * Ping/pong delay in milliseconds.
     */
    @Property(key = "loginserver.server.pingpong.delay", defaultValue = "3000")
    public static int PINGPONG_DELAY;
    /**
     * 洪水防护排除 IP 列表（逗号分隔）。
     * Flood-protection excluded IPs (comma-separated).
     */
    @Property(key = "loginserver.excluded.ips", defaultValue = "")
    public static String EXCLUDED_IP;

    private final NetworkProperties network = new NetworkProperties();
    private final AccountsProperties accounts = new AccountsProperties();
    private final ServerProperties server = new ServerProperties();
    private final ExcludedProperties excluded = new ExcludedProperties();

    public NetworkProperties getNetwork() {
        return network;
    }

    /**
     * no-op：绑定值经嵌套属性直接写入静态字段，防止重复绑定覆盖。
     * no-op: bound values reach the static fields via the nested properties,
     * so re-binding must not overwrite them.
     * @param network 绑定产生的嵌套实例 / nested instance produced by binding
     */
    public void setNetwork(NetworkProperties network) {
    }

    public AccountsProperties getAccounts() {
        return accounts;
    }

    /** 同 {@link #setNetwork}。 / See {@link #setNetwork}. */
    public void setAccounts(AccountsProperties accounts) {
    }

    public ServerProperties getServer() {
        return server;
    }

    /** 同 {@link #setNetwork}。 / See {@link #setNetwork}. */
    public void setServer(ServerProperties server) {
    }

    public ExcludedProperties getExcluded() {
        return excluded;
    }

    /** 同 {@link #setNetwork}。 / See {@link #setNetwork}. */
    public void setExcluded(ExcludedProperties excluded) {
    }

    /**
     * 配置目录路径。
     * Config directory path.
     * @return 配置目录 / Config directory
     */
    private static String configDir() {
        return Objects.requireNonNull(System.getProperty("aion.config.dir"), "aion.config.dir is not configured");
    }

    /**
     * 设置 boot 层属性覆盖。
     * Set boot-layer property overrides.
     * @param properties 覆盖属性 / Override properties
     */
    public static void setBootOverrides(Properties properties) {
        Properties copy = new Properties();
        if (properties != null) {
            copy.putAll(properties);
        }
        bootOverrides = copy;
    }

    /**
     * 从文件加载配置。
     * Load configuration from files.
     */
    public static void load() {
        try {
            Properties myProps = null;
            try {
                log.debug(I18n.get("log.d74fbaa2d37d"));
                myProps = PropertiesUtils.load(configDir() + "/login/myls.properties");
            } catch (Exception e) {
                log.info(I18n.get("log.c453f21e95f6"));
            }

            String config = configDir();
            Properties[] props = loadProperties(config);
            PropertiesUtils.overrideProperties(props, myProps);
            PropertiesUtils.overrideProperties(props, bootOverrides);
            log.debug(I18n.get("log.82761a77d855", config));
            ConfigurableProcessor.process(Config.class, props);
            log.debug(I18n.get("log.b4cc54f61657", config));
            ConfigurableProcessor.process(SvStatsConfig.class, props);
            ConfigurableProcessor.process(VipConfig.class, props);
            VipConfig.validate();
            log.debug(I18n.get("log.9d5403420a43", config));
            ConfigurableProcessor.process(CommonsConfig.class, props);
            log.debug(I18n.get("log.e2109203a77c", config));
            ConfigurableProcessor.process(DatabaseConfig.class, props);

        } catch (Exception e) {
            log.error(I18n.get("log.a4dc4f436c94"), e);
            throw new Error("Can't load loginserver configuration", e);
        }
    }

    private static Properties[] loadProperties(String config) throws IOException {
        Path configPath = Path.of(config);
        Properties[] serviceProperties = PropertiesUtils.loadAllFromDirectory(configPath.resolve("login").toFile());
        Properties[] properties = new Properties[serviceProperties.length + 2];
        properties[0] = PropertiesUtils.load(
            configPath.resolve("network/network.properties").toFile()
        );
        var vipFile = configPath.resolve("main/vip.properties").toFile();
        properties[1] = vipFile.isFile() ? PropertiesUtils.load(vipFile) : new Properties();
        System.arraycopy(serviceProperties, 0, properties, 2, serviceProperties.length);
        return properties;
    }

    /**
     * {@code loginserver.network} 绑定节点。
     * {@code loginserver.network} binding node.
     */
    public static class NetworkProperties {

        private final ClientProperties client = new ClientProperties();
        private final GameserverProperties gameserver = new GameserverProperties();

        public ClientProperties getClient() {
            return client;
        }

        /** no-op，理由同 {@link Config#setNetwork}。 / no-op, see {@link Config#setNetwork}. */
        public void setClient(ClientProperties client) {
        }

        public GameserverProperties getGameserver() {
            return gameserver;
        }

        /** no-op，理由同 {@link Config#setNetwork}。 / no-op, see {@link Config#setNetwork}. */
        public void setGameserver(GameserverProperties gameserver) {
        }

        /**
         * {@code loginserver.network.client} 绑定节点。
         * {@code loginserver.network.client} binding node.
         */
        public static class ClientProperties {

            public int getPort() {
                return LOGIN_PORT;
            }

            public void setPort(int port) {
                LOGIN_PORT = port;
            }

            public String getHost() {
                return LOGIN_BIND_ADDRESS;
            }

            public void setHost(String host) {
                LOGIN_BIND_ADDRESS = host;
            }

            public int getLogintrybeforeban() {
                return LOGIN_TRY_BEFORE_BAN;
            }

            public void setLogintrybeforeban(int logintrybeforeban) {
                LOGIN_TRY_BEFORE_BAN = logintrybeforeban;
            }

            public int getBantimeforbruteforcing() {
                return WRONG_LOGIN_BAN_TIME;
            }

            public void setBantimeforbruteforcing(int bantimeforbruteforcing) {
                WRONG_LOGIN_BAN_TIME = bantimeforbruteforcing;
            }
        }

        /**
         * {@code loginserver.network.gameserver} 绑定节点。
         * {@code loginserver.network.gameserver} binding node.
         */
        public static class GameserverProperties {

            public int getPort() {
                return GAME_PORT;
            }

            public void setPort(int port) {
                GAME_PORT = port;
            }

            public String getHost() {
                return GAME_BIND_ADDRESS;
            }

            public void setHost(String host) {
                GAME_BIND_ADDRESS = host;
            }
        }
    }

    /**
     * {@code loginserver.accounts} 绑定节点。
     * {@code loginserver.accounts} binding node.
     */
    public static class AccountsProperties {

        public boolean isAutocreate() {
            return ACCOUNT_AUTO_CREATION;
        }

        public void setAutocreate(boolean autocreate) {
            ACCOUNT_AUTO_CREATION = autocreate;
        }
    }

    /**
     * {@code loginserver.server} 绑定节点。
     * {@code loginserver.server} binding node.
     */
    public static class ServerProperties {

        private final MaintenanceProperties maintenance = new MaintenanceProperties();
        private final PingpongProperties pingpong = new PingpongProperties();

        public MaintenanceProperties getMaintenance() {
            return maintenance;
        }

        /** no-op，理由同 {@link Config#setNetwork}。 / no-op, see {@link Config#setNetwork}. */
        public void setMaintenance(MaintenanceProperties maintenance) {
        }

        public PingpongProperties getPingpong() {
            return pingpong;
        }

        /** no-op，理由同 {@link Config#setNetwork}。 / no-op, see {@link Config#setNetwork}. */
        public void setPingpong(PingpongProperties pingpong) {
        }

        public boolean isFloodprotector() {
            return ENABLE_FLOOD_PROTECTION;
        }

        public void setFloodprotector(boolean floodprotector) {
            ENABLE_FLOOD_PROTECTION = floodprotector;
        }

        public boolean isBruteforceprotector() {
            return ENABLE_BRUTEFORCE_PROTECTION;
        }

        public void setBruteforceprotector(boolean bruteforceprotector) {
            ENABLE_BRUTEFORCE_PROTECTION = bruteforceprotector;
        }

        /**
         * 「标量+子键」共存节点：{@code value} 承载规范化后的布尔标量，{@code gmlevel} 承载子键。
         * Scalar-plus-sub-key node: {@code value} carries the normalized boolean scalar, {@code gmlevel} the sub-key.
         */
        public static class MaintenanceProperties {

            /**
             * 裸键值形态兼容入口：直接解析布尔并写入静态字段。
             * Bare-key value-shape entry: parses the boolean and writes the static field directly.
             * @param value 裸键布尔值 / bare-key boolean value
             * @return 嵌套实例 / nested instance
             */
            public static MaintenanceProperties valueOf(String value) {
                MAINTENANCE_MOD = Boolean.parseBoolean(value);
                return new MaintenanceProperties();
            }

            public boolean isValue() {
                return MAINTENANCE_MOD;
            }

            public void setValue(boolean value) {
                MAINTENANCE_MOD = value;
            }

            public int getGmlevel() {
                return MAINTENANCE_MOD_GMLEVEL;
            }

            public void setGmlevel(int gmlevel) {
                MAINTENANCE_MOD_GMLEVEL = gmlevel;
            }
        }

        /**
         * 「标量+子键」共存节点：{@code value} 承载规范化后的布尔标量，{@code delay} 承载子键。
         * Scalar-plus-sub-key node: {@code value} carries the normalized boolean scalar, {@code delay} the sub-key.
         */
        public static class PingpongProperties {

            /**
             * 裸键值形态兼容入口：直接解析布尔并写入静态字段。
             * Bare-key value-shape entry: parses the boolean and writes the static field directly.
             * @param value 裸键布尔值 / bare-key boolean value
             * @return 嵌套实例 / nested instance
             */
            public static PingpongProperties valueOf(String value) {
                ENABLE_PINGPONG = Boolean.parseBoolean(value);
                return new PingpongProperties();
            }

            public boolean isValue() {
                return ENABLE_PINGPONG;
            }

            public void setValue(boolean value) {
                ENABLE_PINGPONG = value;
            }

            public int getDelay() {
                return PINGPONG_DELAY;
            }

            public void setDelay(int delay) {
                PINGPONG_DELAY = delay;
            }
        }
    }

    /**
     * {@code loginserver.excluded} 绑定节点。
     * {@code loginserver.excluded} binding node.
     */
    public static class ExcludedProperties {

        public String getIps() {
            return EXCLUDED_IP;
        }

        public void setIps(String ips) {
            EXCLUDED_IP = ips;
        }
    }
}
