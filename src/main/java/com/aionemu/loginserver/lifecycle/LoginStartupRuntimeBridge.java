package com.aionemu.loginserver.lifecycle;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.AEInfos;
import com.aionemu.loginserver.GameServerTable;
import com.aionemu.loginserver.LoginServer;
import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.dao.BannedMacDAO;
import com.aionemu.loginserver.network.ncrypt.KeyGen;
import com.aionemu.loginserver.network.sts.StsVipServer;
import com.aionemu.loginserver.service.LoginCronServices;
import com.aionemu.loginserver.service.LoginNetworkServices;
import com.aionemu.loginserver.service.LoginPremiumServices;
import com.aionemu.loginserver.service.LoginProtectionServices;
import com.aionemu.loginserver.service.LoginTaskManagerServices;
import com.aionemu.loginserver.service.LoginThreadPoolServices;
import com.aionemu.loginserver.service.LoginTransferServices;
import com.aionemu.loginserver.service.PlayerTransferService;
import com.aionemu.loginserver.service.VipService;
import com.aionemu.loginserver.utils.DeadLockDetector;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 登录服启动运行时桥接，将各启动步骤转发到具体服务/静态入口。
 * static entry points.
 */
@Component
@Lazy
public class LoginStartupRuntimeBridge {

    private ObjectProvider<LoginProcessRuntimeBridge> processBridgeProvider;

    /**
     * 注入可选的进程运行时桥接提供者。
     * Inject optional process runtime-bridge provider.
     *
     * @param processBridgeProvider 进程桥接提供者 / process-bridge provider
     */
    @Autowired(required = false)
    void setProcessBridgeProvider(ObjectProvider<LoginProcessRuntimeBridge> processBridgeProvider) {
        this.processBridgeProvider = processBridgeProvider;
    }

    /**
     * 初始化登录服日志。
     * Initialize login-server logging.
     */
    public void initializeLogger() {
        LoginServer.initializeLogger();
    }

    /**
     * 初始化 Cron 服务。
     * Initialize the cron service.
     */
    public void initializeCronService() {
        LoginCronServices.initialize();
    }

    /**
     * 加载配置。
     * Load configuration.
     */
    public void loadConfig() {
        Config.load();
    }

    /**
     * 初始化数据库工厂。
     * Initialize the database factory.
     */
    public void initializeDatabase() {
        DatabaseFactory.init();
    }

    /**
     * 初始化 DAO 管理器。
     * Initialize the DAO manager.
     */
    public void initializeDaos() {
        DAOManager.init();
    }

    /**
     * Synchronize independent VIP data for accounts that do not have a row yet.
     */
    public void synchronizeVipAccounts() {
        new VipService().syncMissingAccounts();
    }

    /**
     * 启动死锁检测器；嵌入式模式下仅记录，独立模式下触发重启退出。
     * Start the deadlock detector; embedded mode only records, standalone mode exits for restart.
     *
     * @param bootEmbedded 是否嵌入式启动 / whether boot-embedded
     */
    public void startDeadlockDetector(boolean bootEmbedded) {
        DeadLockDetector deadLockDetector = new DeadLockDetector(
            60,
            bootEmbedded ? DeadLockDetector.NOTHING : DeadLockDetector.RESTART,
            status -> processBridge().exit(status)
        );
        deadLockDetector.setDaemon(bootEmbedded);
        deadLockDetector.start();
    }

    /**
     * 初始化线程池管理器。
     * Initialize the thread-pool manager.
     */
    public void initializeThreadPool() {
        LoginThreadPoolServices.threadPoolManager();
    }

    /**
     * 初始化密钥生成器。
     * Initialize the key generator.
     *
     * thrown when initialization fails。
     */
    public void initializeKeyGenerator() throws Exception {
        KeyGen.init();
    }

    /**
     * 加载游戏服务器列表。
     * Load the game-server list.
     */
    public void loadGameServers() {
        GameServerTable.load();
    }

    /**
     * 启动 IP 封禁服务。
     * Start the banned-IP service.
     */
    public void startBannedIpController() {
        LoginProtectionServices.bannedIpService().start();
    }

    /**
     * 清理过期 MAC 封禁记录。
     * Clean expired MAC-ban records.
     */
    public void cleanExpiredMacBans() {
        DAOManager.getDAO(BannedMacDAO.class).cleanExpiredBans();
    }

    /**
     * 连接登录服网络传输。
     * Connect the login-server network transport.
     */
    public void connectNetwork() {
        LoginNetworkServices.serverTransport().connect();
        // TODO: STS 尚未实现完成，暂不启动。 / STS is not fully implemented; keep it disabled.
        // StsVipServer.startIfEnabled();
    }

    /**
     * 获取玩家转移服务实例。
     * Obtain the player-transfer service instance.
     *
     * @return 玩家转移服务 / player-transfer service
     */
    public PlayerTransferService playerTransferService() {
        return LoginTransferServices.playerTransferService();
    }

    /**
     * 初始化数据库任务管理器。
     * Initialize the DB task manager.
     */
    public void initializeTaskManager() {
        LoginTaskManagerServices.taskFromDBManager();
    }

    /**
     * 注册 JVM 关机钩子。
     * Register the JVM shutdown hook.
     */
    public void registerShutdownHook() {
        LoginProcessRuntimeBridge processBridge = processBridge();
        processBridge.registerShutdownHook(processBridge.shutdownHook());
    }

    /**
     * 打印全部运行时信息。
     * Print all runtime information.
     */
    public void printInfos() {
        AEInfos.printAllInfos();
    }

    /**
     * 初始化会员/增值控制器。
     * Initialize the premium controller.
     */
    public void initializePremiumController() {
        LoginPremiumServices.premiumController();
    }

    /**
     * 以错误码退出进程。
     * Exit the process with an error code.
     */
    public void exitWithError() {
        processBridge().exitWithError();
    }

    /**
     * 解析进程运行时桥接：优先 Spring 提供者，否则新建实例。
     * Resolve the process runtime bridge: prefer Spring provider, else create a new instance.
     *
     * @return 进程运行时桥接 / process runtime bridge
     */
    private LoginProcessRuntimeBridge processBridge() {
        if (processBridgeProvider == null) {
            return new LoginProcessRuntimeBridge();
        }
        return processBridgeProvider.getIfAvailable(LoginProcessRuntimeBridge::new);
    }
}
