package com.aionemu.chatserver;

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

import com.aionemu.commons.logging.slf4j.LogbackConfiguration;
import com.aionemu.commons.utils.AionRuntimeMode;
import lombok.experimental.UtilityClass;

/**
 * 聊天服务器入口与日志初始化静态门面。
 * Static facade for chat-server entry and logger initialization.
 *
 * @author ATracer, KID, nrg
 */
@UtilityClass
public class ChatServer {

    /**
     * 初始化日志：备份旧日志、配置 Logback；嵌入式启动时跳过。
     * Initialize logging: back up old logs and configure Logback; skip when boot-embedded.
     */
    static void initializeLogger() {
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
     * 由 boot 管理的服务生命周期启动聊天服（使用遗留依赖实现）。
     * Start ChatServer from the boot-managed service lifecycle (legacy dependencies).
     *
     * @param args 启动参数 / Startup arguments
     */
    public static void start(String[] args) {
        start(args, new ChatServerLegacyDependencies());
    }

    /**
     * 使用给定依赖启动聊天服启动序列。
     * Start the chat-server startup sequence with the given dependencies.
     *
     * @param args 启动参数 / Startup arguments
     * Startup dependencies
     */
    static void start(String[] args, ChatServerDependencies dependencies) {
        ChatServerStartupSequence.start(dependencies);
    }
}
