package com.aionemu.chatserver;

import com.aionemu.chatserver.configs.Config;
import com.aionemu.commons.utils.AEInfos;
import com.aionemu.commons.utils.AionRuntimeMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
@ConditionalOnProperty(prefix = "aion.services.chat", name = "enabled", havingValue = "true")
public class ChatServerStartupBridge {

    public void initializeLogger() {
        ChatServer.initializeLogger();
    }

    public void loadConfig() {
        Config.load();
    }

    public void printInfos() {
        AEInfos.printAllInfos();
    }

    public boolean isBootEmbedded() {
        return AionRuntimeMode.isBootEmbedded();
    }

    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(ShutdownHook.getInstance());
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
