package com.aionemu.boot.lifecycle;

import org.springframework.boot.ApplicationArguments;

/**
 * 内嵌 Aion 服务（login/chat/game）的生命周期契约。
 * Lifecycle contract for embedded Aion services (login/chat/game).
 */
public interface AionServiceLifecycle {

    /**
     * 返回服务逻辑名称（如 login、chat、game）。
     * Returns the logical service name (e.g. login, chat, game).
     * @return 服务逻辑名称 / service name
     */
    String getName();

    /**
     * 返回启动相位；数值越小越先启动。
     * Returns the startup phase; lower values start earlier.
     * @return 启动相位值 / phase value
     */
    int getPhase();

    /**
     * 判断该服务是否在配置中启用。
     * Whether this service is enabled in configuration.
     * @return 启用则为 true / true if enabled
     */
    boolean isEnabled();

    /**
     * 启动服务。
     * Starts the service.
     * @param args 应用启动参数 / application arguments
	 */
    void start(ApplicationArguments args);

    /**
     * 全部服务启动完成后的回调；默认空实现。
     * Callback invoked after all enabled services have started; the default is a no-op.
     */
    default void onStartupComplete() {
    }

    /**
     * 停止服务；默认空实现。
     * Stops the service; default is a no-op.
     */
    default void stop() {
    }
}
