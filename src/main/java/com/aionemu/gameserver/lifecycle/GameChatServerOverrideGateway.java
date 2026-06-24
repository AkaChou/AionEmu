package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.GSConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameChatServerOverrideGateway {

    public void overrideChatServerEnabled(boolean chatServerEnabled) {
        GSConfig.ENABLE_CHAT_SERVER = chatServerEnabled;
        log.info("Chat Server connection overridden by boot configuration: {}", chatServerEnabled);
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
