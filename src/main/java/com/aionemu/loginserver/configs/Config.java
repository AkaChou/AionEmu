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

/**
 * 登录服配置项与加载逻辑。
 * LoginServer configuration properties and loader.
 *
 * @author -Nemesiss-
 * @author SoulKeeper
 */
@Slf4j
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

    /**
     * 配置目录路径。
     * Config directory path.
     *
     * Config directory
     */
    private static String configDir() {
        return Objects.requireNonNull(System.getProperty("aion.config.dir"), "aion.config.dir is not configured");
    }

    /**
     * 设置 boot 层属性覆盖。
     * Set boot-layer property overrides.
     *
     * Override properties
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
                log.info(I18n.get("log.d74fbaa2d37d"));
                myProps = PropertiesUtils.load(configDir() + "/login/myls.properties");
            } catch (Exception e) {
                log.info(I18n.get("log.c453f21e95f6"));
            }

            String config = configDir();
            Properties[] props = loadProperties(config);
            PropertiesUtils.overrideProperties(props, myProps);
            PropertiesUtils.overrideProperties(props, bootOverrides);
            log.info(I18n.get("log.82761a77d855", config));
            ConfigurableProcessor.process(Config.class, props);
            log.info(I18n.get("log.b4cc54f61657", config));
            ConfigurableProcessor.process(SvStatsConfig.class, props);
            ConfigurableProcessor.process(VipConfig.class, props);
            VipConfig.validate();
            log.info(I18n.get("log.9d5403420a43", config));
            ConfigurableProcessor.process(CommonsConfig.class, props);
            log.info(I18n.get("log.e2109203a77c", config));
            ConfigurableProcessor.process(DatabaseConfig.class, props);

        } catch (Exception e) {
            log.error(I18n.get("log.a4dc4f436c94", e));
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
}
