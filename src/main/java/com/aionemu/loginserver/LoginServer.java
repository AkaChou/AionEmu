package com.aionemu.loginserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.joran.spi.JoranException;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.logging.slf4j.LogbackConfiguration;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.loginserver.lifecycle.LoginStartupGateway;
import com.aionemu.loginserver.lifecycle.LoginStartupSequenceLifecycle;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录服入口：日志初始化与启动序列。
 * LoginServer entry: logger init and startup sequence.
 *
 * @author -Nemesiss-
 */
@Slf4j
@UtilityClass
public class LoginServer {

    /**
     * 备份旧日志并配置 Logback（嵌入式启动时跳过）。
     * Archive old logs and configure Logback (skipped when boot-embedded).
     */
    private void initalizeLoggger() {
        if (AionRuntimeMode.isBootEmbedded()) {
            return;
        }
        new File("./log/backup/").mkdirs();
        File[] files = new File("log").listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".log");
            }
        });

        if (files != null && files.length > 0) {
            byte[] buf = new byte[1024];
            try {
                String outFilename = "./log/backup/" + new SimpleDateFormat("yyyy-MM-dd HHmmss").format(new Date()) + ".zip";
                ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));
                out.setMethod(ZipOutputStream.DEFLATED);
                out.setLevel(Deflater.BEST_COMPRESSION);

                for (File logFile : files) {
                    FileInputStream in = new FileInputStream(logFile);
                    out.putNextEntry(new ZipEntry(logFile.getName()));
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.closeEntry();
                    in.close();
                    logFile.delete();
                }
                out.close();
            } catch (IOException e) {
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
     * 初始化日志系统。
     * Initialize the logging system.
     */
    public void initializeLogger() {
        initalizeLoggger();
    }

    /**
     * 由 boot 托管生命周期启动登录服。
     * Start LoginServer via the boot-managed service lifecycle.
     *
     * @param args 启动参数 / Startup arguments
     */
    public void start(final String[] args) {
        start(args, new LoginStartupSequenceLifecycle(new LoginStartupGateway()));
    }

    /**
     * 使用指定启动序列生命周期启动登录服。
     * Start LoginServer with the given startup sequence lifecycle.
     *
     * @param args 启动参数 / Startup arguments
     * @param startupSequenceLifecycle 启动序列生命周期 / Startup sequence lifecycle
     */
    public void start(final String[] args, LoginStartupSequenceLifecycle startupSequenceLifecycle) {
        startupSequenceLifecycle.start();
        log.info(I18n.get("log.23ddf7057872", startupSequenceLifecycle.getLoadTimeMillis() / 1000));
    }
}
