package com.aionemu.chatserver.service;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 聊天重启服务静态访问门面：在 Spring 可用时委托 {@link RestartService} 提供者。
 * Static facade for chat restart service: delegates to a {@link RestartService} provider when Spring is available.
 */
@Component
@ConditionalOnProperty(prefix = "aion.services.chat", name = "enabled", havingValue = "true")
public final class ChatRestartServices implements DisposableBean {

    private static volatile ObjectProvider<RestartService> restartServiceProvider;

    /**
     * 注册 {@link RestartService} 的 Spring 提供者。
     * Register the Spring provider for {@link RestartService}.
     *
     * @param restartServiceProvider 重启服务提供者 / Restart service provider
     */
    public ChatRestartServices(ObjectProvider<RestartService> restartServiceProvider) {
        ChatRestartServices.restartServiceProvider = restartServiceProvider;
    }

    /**
     * 获取重启服务实例。
     * Obtain the restart service instance.
     *
     * @return 重启服务实例 / restart service instance
     */
    public static RestartService restartService() {
        ObjectProvider<RestartService> provider = restartServiceProvider;
        if (provider == null) {
            return fallbackRestartService();
        }
        return provider.getIfAvailable(ChatRestartServices::fallbackRestartService);
    }

    /**
     * Bean 销毁时清空静态提供者引用。
     * Clear the static provider reference when the bean is destroyed.
     */
    @Override
    public void destroy() {
        restartServiceProvider = null;
    }

    /**
     * 回退到本地重启服务。
     * Fall back to the local restart service.
     *
     * @return 回退实例 / fallback instance
     */
    private static RestartService fallbackRestartService() {
        return Fallbacks.RESTART_SERVICE;
    }

    /**
     * 非 Spring 环境下的回退实例容器。
     * Holder for the fallback instance outside Spring.
     */
    private static final class Fallbacks {

        private static final RestartService RESTART_SERVICE = new RestartService();
    }
}
