import java.lang.reflect.Method;

/**
 * 本地 IDE 模式（未设置 aion.home / aion.config.dir）下路径解析与各服务 database.url 的实测探针。
 * Probe for the IDE mode: resolves the runtime paths like an IDEA run and prints the JDBC URL each
 * service ends up with.
 */
public class ProbeIdeConfigTree {

    public static void main(String[] args) throws Exception {
        System.setProperty("aion.logging.config", System.getProperty("java.io.tmpdir") + "/probe-logback.xml");

        Class<?> paths = Class.forName("com.aionemu.boot.lifecycle.AionServicePaths");
        Method configureGame = paths.getDeclaredMethod("configureGame");
        configureGame.setAccessible(true);
        configureGame.invoke(null);

        System.out.println("[PROBE] aion.home            = " + System.getProperty("aion.home"));
        System.out.println("[PROBE] aion.config.dir      = " + System.getProperty("aion.config.dir"));
        System.out.println("[PROBE] aion.game.data.dir   = " + System.getProperty("aion.game.data.dir"));

        com.aionemu.loginserver.configs.Config.load();
        System.out.println("[PROBE] login DatabaseConfig.DATABASE_URL = "
            + com.aionemu.commons.configs.DatabaseConfig.DATABASE_URL);
        System.out.println("[PROBE] login LOGIN_PORT = " + com.aionemu.loginserver.configs.Config.LOGIN_PORT);

        com.aionemu.gameserver.configs.Config.load();
        System.out.println("[PROBE] game DatabaseConfig.DATABASE_URL = "
            + com.aionemu.commons.configs.DatabaseConfig.DATABASE_URL);
    }
}
