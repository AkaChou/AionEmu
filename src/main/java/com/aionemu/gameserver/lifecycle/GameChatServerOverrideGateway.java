package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.GSConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 聊天服覆盖网关：运行时覆盖聊天服启用开关。
 * Chat-server override gateway: overrides the chat-server enable switch at runtime.
 */
@Component
@Slf4j
public class GameChatServerOverrideGateway {

    /**
     * 覆盖聊天服启用配置并记录日志。
     * Override the chat-server enabled flag and log it.
     *
     * @param chatServerEnabled 是否启用聊天服 / Whether chat server is enabled
     */
    public void overrideChatServerEnabled(boolean chatServerEnabled) {
        GSConfig.ENABLE_CHAT_SERVER = chatServerEnabled;
        log.info(I18n.get("shutdown.chat_override", chatServerEnabled));
    }

    /**
     * 返回当前时间毫秒数。
     * Return the current time in milliseconds.
     *
     * @return 当前时间毫秒 / Current time millis
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
