package com.aionemu.loginserver.lifecycle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.joran.spi.JoranException;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.logging.slf4j.LogbackConfiguration;
import com.aionemu.commons.utils.AEInfos;
import com.aionemu.commons.utils.AionProcessExit;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.commons.utils.ExitCode;
import com.aionemu.loginserver.GameServerTable;
import com.aionemu.loginserver.Shutdown;
import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.controller.BannedIpController;
import com.aionemu.loginserver.network.NetConnector;
import com.aionemu.loginserver.network.ncrypt.KeyGen;
import com.aionemu.loginserver.persistence.dao.BannedMacDao;
import com.aionemu.loginserver.service.LoginCronService;
import com.aionemu.loginserver.service.VipService;
import com.aionemu.loginserver.utils.DeadLockDetector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录服启动/关闭序列：按固定顺序驱动各启动步骤并记录加载状态与耗时。
 * 各服务由 Spring 构造注入，本类只负责编排顺序。
 * Login-server startup/shutdown sequence: drives every startup step in a fixed order and records load
 * state and elapsed time. Services are constructor-injected by Spring; this class only orders the steps.
 */
@Slf4j
@Service
@Lazy
@RequiredArgsConstructor
public class LoginStartupSequence {

    private final LoginCronService loginCronService;
    private final GameServerTable gameServerTable;
    private final BannedIpController bannedIpController;
    private final BannedMacDao bannedMacDao;
    private final NetConnector netConnector;
    private final VipService vipService;
    /**
     * 关机钩子提供者：保持 Shutdown 为懒加载且不暴露 CGLIB 代理（它需作为真实 Thread 注册）。
     * Shutdown provider: keeps Shutdown lazy while avoiding a CGLIB proxy (it must register as a real Thread).
     */
    private final ObjectProvider<Shutdown> shutdownProvider;
    /**
     * 启动期解析出的关机句柄；销毁阶段容器已禁止创建单例，届时不能再向 provider 索取。
     * Shutdown handle resolved at startup: the container forbids singleton creation during destruction,
     * so the provider must not be asked again at that point.
     */
    private Shutdown shutdown;

    private boolean loaded;
    private long loadTimeMillis = -1;

    /**
     * 执行完整启动序列；已加载时直接返回。
     * Run the full startup sequence; no-op if already loaded.
     */
    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            initializeLogger();
            loginCronService.initialize();
            log.info(I18n.get("log.189724bde746",
                new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date(System.currentTimeMillis()))));
            Config.load();
            vipService.grantToAllAccounts();
            startDeadlockDetector();
            initializeKeyGenerator();
            gameServerTable.load();
            bannedIpController.start();
            bannedMacDao.cleanExpiredBans();
            netConnector.currentTransport().connect();
            shutdown = shutdownProvider.getObject();
            if (!AionRuntimeMode.isBootEmbedded()) {
                Runtime.getRuntime().addShutdownHook(shutdown);
            }
            AEInfos.printAllInfos();
            loaded = true;
        } catch (RuntimeException | Error e) {
            loaded = false;
            throw e;
        } finally {
            loadTimeMillis = System.currentTimeMillis() - start;
        }
    }

    /**
     * 关闭登录服运行时（不 halt 进程），并复位加载状态。
     * Shuts down login runtime without halting the process and resets the loaded flag.
     */
    public synchronized void stop() {
        Shutdown current = shutdown;
        shutdown = null;
        if (current != null) {
            current.shutdown(false);
        }
        loaded = false;
    }

    /**
     * 是否已成功完成启动序列。
     * Whether the startup sequence completed successfully.
     * @return 已加载返回 true / true if loaded
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

    /**
     * 最近一次启动序列耗时（毫秒）；未执行过为 -1。
     * Elapsed millis of the last startup sequence; -1 if never run.
     * @return 上次启动耗时毫秒 / elapsed milliseconds
     */
    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    /**
     * 初始化日志：归档旧日志并配置 Logback（嵌入式启动时跳过归档）。
     * Initializes logging: archives old logs and configures Logback (archive skipped when boot-embedded).
     */
    private void initializeLogger() {
        if (AionRuntimeMode.isBootEmbedded()) {
            return;
        }
        new File("./log/backup/").mkdirs();
        File[] files = new File("log").listFiles((dir, name) -> name.endsWith(".log"));

        if (files != null && files.length > 0) {
            byte[] buf = new byte[1024];
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(
                    "./log/backup/" + new SimpleDateFormat("yyyy-MM-dd HHmmss").format(new Date()) + ".zip"))) {
                out.setMethod(ZipOutputStream.DEFLATED);
                out.setLevel(Deflater.BEST_COMPRESSION);

                for (File logFile : files) {
                    try (FileInputStream in = new FileInputStream(logFile)) {
                        out.putNextEntry(new ZipEntry(logFile.getName()));
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        out.closeEntry();
                    }
                    logFile.delete();
                }
            } catch (IOException e) {
                log.warn(I18n.get("log.9802aeee5f36", e.getMessage()), e);
            }
        }
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            LogbackConfiguration.configure(lc);
        } catch (JoranException je) {
            throw new RuntimeException("Failed to configure loggers, shutting down...", je);
        }
    }

    /**
     * 启动死锁检测器；嵌入式模式下仅记录，独立模式下触发重启退出。
     * Start the deadlock detector; embedded mode only records, standalone mode exits for restart.
     */
    private void startDeadlockDetector() {
        boolean bootEmbedded = AionRuntimeMode.isBootEmbedded();
        DeadLockDetector deadLockDetector = new DeadLockDetector(60,
            bootEmbedded ? DeadLockDetector.NOTHING : DeadLockDetector.RESTART, AionProcessExit::halt);
        deadLockDetector.setDaemon(bootEmbedded);
        deadLockDetector.start();
    }

    /**
     * 初始化密钥生成器：失败时嵌入式模式抛错，独立模式错误退出。
     * Initialize the key generator: rethrow in embedded mode, exit with error in standalone mode.
     */
    private void initializeKeyGenerator() {
        try {
            KeyGen.init();
        } catch (Exception e) {
            log.error(I18n.get("log.9802aeee5f36", e.getMessage()), e);
            if (AionRuntimeMode.isBootEmbedded()) {
                throw new IllegalStateException("Failed initializing Key Generator", e);
            }
            AionProcessExit.halt(ExitCode.CODE_ERROR);
        }
    }
}
