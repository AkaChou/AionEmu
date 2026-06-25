package com.aionemu.chatserver;

import com.aionemu.chatserver.configs.Config;
import com.aionemu.commons.utils.AEInfos;
import com.aionemu.commons.utils.AionRuntimeMode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
@ConditionalOnProperty(prefix = "aion.services.chat", name = "enabled", havingValue = "true")
public class ChatServerStartupBridge {

    private ObjectProvider<ChatProcessRuntimeBridge> processBridgeProvider;

    @Autowired(required = false)
    void setProcessBridgeProvider(ObjectProvider<ChatProcessRuntimeBridge> processBridgeProvider) {
        this.processBridgeProvider = processBridgeProvider;
    }

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
        ChatProcessRuntimeBridge processBridge = processBridge();
        processBridge.registerShutdownHook(processBridge.shutdownHook());
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private ChatProcessRuntimeBridge processBridge() {
        if (processBridgeProvider == null) {
            return new ChatProcessRuntimeBridge();
        }
        return processBridgeProvider.getIfAvailable(ChatProcessRuntimeBridge::new);
    }
}
