package com.aionemu.loginserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.loginserver.service.PlayerTransferService;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 登录服启动网关：将启动步骤委托给 {@link LoginStartupRuntimeBridge}，并协调玩家转移服务初始化。
 * Login-server startup gateway that delegates startup steps to {@link LoginStartupRuntimeBridge}
 * and coordinates player-transfer service initialization.
 */
@Component
@Slf4j
public class LoginStartupGateway {

    private ObjectProvider<PlayerTransferService> playerTransferServiceProvider;
    private ObjectProvider<LoginStartupRuntimeBridge> runtimeBridgeProvider;

    /**
     * 注入可选的玩家转移服务提供者。
     * Inject optional player-transfer service provider.
     *
     * @param playerTransferServiceProvider 玩家转移服务提供者 / player-transfer service provider
     */
    @Autowired(required = false)
    void setPlayerTransferServiceProvider(ObjectProvider<PlayerTransferService> playerTransferServiceProvider) {
        this.playerTransferServiceProvider = playerTransferServiceProvider;
    }

    /**
     * 注入可选的启动运行时桥接提供者。
     * Inject optional startup runtime-bridge provider.
     *
     * @param runtimeBridgeProvider 运行时桥接提供者 / runtime-bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<LoginStartupRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 初始化日志子系统。
     * Initialize the logging subsystem.
     */
    public void initializeLogger() {
        runtimeBridge().initializeLogger();
    }

    /**
     * 初始化 Cron 调度服务。
     * Initialize the cron scheduling service.
     */
    public void initializeCronService() {
        runtimeBridge().initializeCronService();
    }

    /**
     * 输出启动时间戳日志。
     * Log the startup timestamp.
     */
    public void logStartupTimestamp() {
        log.info(I18n.get("log.189724bde746", new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date(currentTimeMillis()))));
    }

    /**
     * 加载登录服配置。
     * Load login-server configuration.
     */
    public void loadConfig() {
        runtimeBridge().loadConfig();
    }

    /**
     * 初始化数据库连接工厂。
     * Initialize the database connection factory.
     */
    public void initializeDatabase() {
        runtimeBridge().initializeDatabase();
    }

    /**
     * 初始化 DAO 管理器。
     * Initialize the DAO manager.
     */
    public void initializeDaos() {
        runtimeBridge().initializeDaos();
    }

    /**
     * 启动死锁检测器。
     * Start the deadlock detector.
     */
    public void startDeadlockDetector() {
        runtimeBridge().startDeadlockDetector(isBootEmbedded());
    }

    /**
     * 初始化线程池。
     * Initialize the thread pool.
     */
    public void initializeThreadPool() {
        runtimeBridge().initializeThreadPool();
    }

    /**
     * 初始化加密密钥生成器。
     * Initialize the crypto key generator.
     *
     * thrown when key generation fails。
     */
    public void initializeKeyGenerator() throws Exception {
        runtimeBridge().initializeKeyGenerator();
    }

    /**
     * 记录密钥生成器初始化失败。
     * Log key-generator initialization failure.
     *
     * @param e 异常 / exception
     */
    public void logKeyGeneratorFailure(Exception e) {
        log.error(I18n.get("log.9802aeee5f36", e.getMessage(), e));
    }

    /**
     * 加载游戏服务器表。
     * Load the game-server table.
     */
    public void loadGameServers() {
        runtimeBridge().loadGameServers();
    }

    /**
     * 启动 IP 封禁控制器。
     * Start the banned-IP controller.
     */
    public void startBannedIpController() {
        runtimeBridge().startBannedIpController();
    }

    /**
     * 清理过期 MAC 封禁。
     * Clean expired MAC bans.
     */
    public void cleanExpiredMacBans() {
        runtimeBridge().cleanExpiredMacBans();
    }

    /**
     * 连接网络传输层。
     * Connect the network transport layer.
     */
    public void connectNetwork() {
        runtimeBridge().connectNetwork();
    }

    /**
     * 初始化玩家转移服务（触发懒加载）。
     * Initialize the player-transfer service (trigger lazy resolution).
     */
    public void initializePlayerTransferService() {
        playerTransferService();
    }

    /**
     * 初始化数据库任务管理器。
     * Initialize the DB-backed task manager.
     */
    public void initializeTaskManager() {
        runtimeBridge().initializeTaskManager();
    }

    /**
     * 判断当前是否为嵌入式启动模式。
     * Whether the process is running in boot-embedded mode.
     *
     * @return 嵌入式模式返回 true / true if boot-embedded
     */
    public boolean isBootEmbedded() {
        return AionRuntimeMode.isBootEmbedded();
    }

    /**
     * 注册 JVM 关机钩子。
     * Register the JVM shutdown hook.
     */
    public void registerShutdownHook() {
        runtimeBridge().registerShutdownHook();
    }

    /**
     * 打印运行时环境信息。
     * Print runtime environment information.
     */
    public void printInfos() {
        runtimeBridge().printInfos();
    }

    /**
     * 初始化会员/增值控制器。
     * Initialize the premium controller.
     */
    public void initializePremiumController() {
        runtimeBridge().initializePremiumController();
    }

    /**
     * 以错误码退出进程。
     * Exit the process with an error code.
     */
    public void exitWithError() {
        runtimeBridge().exitWithError();
    }

    /**
     * 返回当前系统时间毫秒数。
     * Return the current system time in milliseconds.
     *
     * @return 当前时间毫秒 / current time millis
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 解析玩家转移服务：优先 Spring 提供者，否则回退到运行时桥接。
     * Resolve player-transfer service: prefer Spring provider, else fall back to the runtime bridge.
     *
     * @return 玩家转移服务 / player-transfer service
     */
    private PlayerTransferService playerTransferService() {
        if (playerTransferServiceProvider == null) {
            return runtimeBridge().playerTransferService();
        }
        return playerTransferServiceProvider.getIfAvailable(() -> runtimeBridge().playerTransferService());
    }

    /**
     * 解析启动运行时桥接：优先 Spring 提供者，否则新建实例。
     * Resolve the startup runtime bridge: prefer Spring provider, else create a new instance.
     *
     * @return 启动运行时桥接 / startup runtime bridge
     */
    private LoginStartupRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new LoginStartupRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(LoginStartupRuntimeBridge::new);
    }
}
