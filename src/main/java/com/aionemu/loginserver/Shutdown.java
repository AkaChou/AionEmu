package com.aionemu.loginserver;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.network.CommonsNetworkThreadPoolServices;
import com.aionemu.commons.utils.AionProcessExit;
import com.aionemu.commons.utils.ExitCode;
import com.aionemu.loginserver.configs.SvStatsConfig;
import com.aionemu.loginserver.network.NetConnector;
import com.aionemu.loginserver.persistence.dao.SvstatDao;
import com.aionemu.loginserver.service.LoginCronService;
import com.aionemu.loginserver.utils.ThreadPoolManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录服关闭钩子：有序关闭网络、线程池与数据库。
 * LoginServer shutdown hook: orderly stop of network, pools and database.
 * @author -Nemesiss-, nrg
 */
@Slf4j
@Component
@Lazy
@RequiredArgsConstructor
public class Shutdown extends Thread {

    private final NetConnector netConnector;
    private final LoginCronService loginCronService;
    private final ThreadPoolManager threadPoolManager;
    private final SvstatDao svstatsDao;

    private final AtomicBoolean shutdownStarted = new AtomicBoolean(false);
    /**
     * 为 true 时仅重启，否则正常退出。
     * When true, restart only; otherwise normal exit.
     */
    private volatile boolean restartOnly = false;

    /**
     * 设置是否仅重启。
     * Set whether to restart only.
     * @param restartOnly 仅重启则为 true / true for restart only
     */
    public void setRestartOnly(boolean restartOnly) {
        this.restartOnly = restartOnly;
    }

    /**
     * 作为 shutdown hook 线程入口，执行完整关闭并 halt JVM。
     * Entry as shutdown-hook thread: full shutdown then halt JVM.
     */
    @Override
    public void run() {
        shutdown(true);
    }

    /**
     * 有序关闭登录服资源。
     * Orderly shut down LoginServer resources.
     * @param haltJvm 是否在结束后 halt JVM / Whether to halt JVM afterwards
     */
    public void shutdown(boolean haltJvm) {
        if (!shutdownStarted.compareAndSet(false, true)) {
            return;
        }
        try {
            netConnector.shutdownIfInitialized();
        } catch (Throwable t) {
            log.error(I18n.get("log.8a48277abecd"), t);
        }


        // 在线程池关闭前先关闭 cron 服务 / shutdown cron service prior to threadpool shutdown
        try {
            loginCronService.shutdown();
        } catch (Throwable t) {
            log.error(I18n.get("log.203da8e5761c"), t);
        }

        /* 关闭线程池 / Shutting down threadpools */
        try {
            threadPoolManager.shutdown();
        } catch (Throwable t) {
            log.error(I18n.get("log.6fb05b263d35"), t);
        }
        try {
            CommonsNetworkThreadPoolServices.threadPoolManager().shutdown();
        } catch (Throwable t) {
            log.error(I18n.get("log.c02220991afd"), t);
        }

        try {
            if (SvStatsConfig.SVSTATS_ENABLE) {
                svstatsDao.update_SvStats_All_Offline(0, 0);
            }
        } catch (Throwable t) {
            log.error(I18n.get("log.3c9d0695e9af"), t);
        }

        if (!haltJvm) {
            return;
        }

        // 执行系统退出 / Do system exit
        if (restartOnly) {
            AionProcessExit.halt(ExitCode.CODE_RESTART);
        } else {
            AionProcessExit.halt(ExitCode.CODE_NORMAL);
        }
    }
}
