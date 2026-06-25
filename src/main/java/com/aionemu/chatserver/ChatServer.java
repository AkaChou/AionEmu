/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 *  Aion-Lightning is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Aion-Lightning is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details. *
 *  You should have received a copy of the GNU General Public License
 *  along with Aion-Lightning.
 *  If not, see <http://www.gnu.org/licenses/>.
 */


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
import org.slf4j.Logger;

/**
 * @author ATracer, KID, nrg
 */
public class ChatServer {

    /**
     * Logger for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(ChatServer.class);

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
     * Starts ChatServer from the boot-managed service lifecycle.
     *
     * @param args startup arguments
     */
    public static void start(String[] args) {
        start(args, ChatServerDependencies.legacy());
    }

    static void start(String[] args, ChatServerDependencies dependencies) {
        ChatServerStartupBridge startupBridge = dependencies.startupBridge();
        long start = startupBridge.currentTimeMillis();

        startupBridge.initializeLogger();

        startupBridge.loadConfig();
        startupBridge.printInfos();
        dependencies.idFactory();
        dependencies.gameServerService();
        dependencies.broadcastService();
        dependencies.chatService();
        dependencies.nettyServer();
        dependencies.restartService();

        if (!startupBridge.isBootEmbedded()) {
            startupBridge.registerShutdownHook();
        }
        log.info("AL Chat Server started in " + (startupBridge.currentTimeMillis() - start) / 1000 + " seconds.");
    }
}
