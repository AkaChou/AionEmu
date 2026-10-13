package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.GSConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 聊天服覆盖生命周期：在启动序列中按需覆盖聊天服开关并记录加载状态。
 * Lifecycle for chat-server override: applies the optional override during startup and tracks load state.
 */
@Component
@Slf4j
public class GameChatServerOverrideLifecycle {

    /**
     * 是否已加载。
     * Whether this stage is loaded.
     */
    private boolean loaded;

    /**
     * 加载耗时毫秒；未启动前为 -1。
     * Load time in milliseconds; {@code -1} before start.
     */
    private long loadTimeMillis = -1;

    /**
     * 最近一次失败。
     * Last failure, if any.
     */
    private Throwable lastFailure;

    /**
     * 启动本阶段：若提供覆盖值则写入配置。
     * Start this stage: apply the override when provided.
     * @param chatServerEnabledOverride 覆盖值，null 表示不覆盖 / Override value; {@code null} means no override
     */
    public synchronized void start(Boolean chatServerEnabledOverride) {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            if (chatServerEnabledOverride != null) {
                overrideChatServerEnabled(chatServerEnabledOverride);
            }
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = System.currentTimeMillis() - start;
        }
    }

    /**
     * 覆盖聊天服启用配置并记录日志。
     * Override the chat-server enabled flag and log it.
     * @param chatServerEnabled 是否启用聊天服 / Whether chat server is enabled
     */
    protected void overrideChatServerEnabled(boolean chatServerEnabled) {
        GSConfig.ENABLE_CHAT_SERVER = chatServerEnabled;
        log.info(I18n.get("shutdown.chat_override", chatServerEnabled));
    }

    /**
     * 是否已加载。
     * Whether this stage is loaded.
     * @return 已加载为 {@code true} / {@code true} if loaded
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

    /**
     * 加载耗时毫秒。
     * Load time in milliseconds.
     * @return 耗时毫秒，未启动为 -1 / Elapsed millis, or {@code -1} if not started
     */
    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    /**
     * 最近失败。
     * Last failure.
     * @return 最近异常，无则为 null / Last throwable, or {@code null}
     */
    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
