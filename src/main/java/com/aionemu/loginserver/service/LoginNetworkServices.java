package com.aionemu.loginserver.service;

import com.aionemu.commons.network.ServerTransport;
import com.aionemu.loginserver.network.NetConnector;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 登录服网络传输的静态访问桥（Spring 与遗留调用点兼容）。
 * Static access bridge for login network transport (Spring + legacy call sites).
 */
@Component
public final class LoginNetworkServices implements DisposableBean {

    private static volatile ObjectProvider<ServerTransport> serverTransportProvider;

    /**
     * 注入并缓存传输层 {@link ObjectProvider}。
     * Inject and cache the transport {@link ObjectProvider}.
     *
     * @param serverTransportProvider 传输层提供者 / transport provider
     */
    public LoginNetworkServices(ObjectProvider<ServerTransport> serverTransportProvider) {
        LoginNetworkServices.serverTransportProvider = serverTransportProvider;
    }

    /**
     * 获取当前服务器传输；容器不可用时回退到 {@link NetConnector}。
     * Resolve current server transport; fall back to {@link NetConnector} outside the container.
     *
     * @return 服务器传输 / server transport
     */
    public static ServerTransport serverTransport() {
        ObjectProvider<ServerTransport> provider = serverTransportProvider;
        if (provider == null) {
            return fallbackServerTransport();
        }
        return provider.getIfAvailable(LoginNetworkServices::fallbackServerTransport);
    }

    /**
     * Spring 销毁时清理静态 provider。
     * Clear the static provider on Spring destroy.
     */
    @Override
    public void destroy() {
        serverTransportProvider = null;
    }

    /**
     * 回退到当前 NetConnector 传输。
     * Fall back to the current NetConnector transport.
     *
     * @return 服务器传输 / server transport
     */
    private static ServerTransport fallbackServerTransport() {
        return Fallbacks.currentTransport();
    }

    /**
     * 无容器场景下的传输回退实现。
     * Transport fallback when Spring is unavailable.
     */
    private static final class Fallbacks {

        /**
         * 读取 {@link NetConnector} 当前传输。
         * Read the current {@link NetConnector} transport.
         *
         * @return 服务器传输 / server transport
         */
        private static ServerTransport currentTransport() {
            return NetConnector.currentTransport();
        }
    }
}
