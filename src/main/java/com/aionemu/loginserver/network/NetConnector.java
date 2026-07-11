package com.aionemu.loginserver.network;

import com.aionemu.commons.network.NettyServer;
import com.aionemu.commons.network.NettyServerCfg;
import com.aionemu.commons.network.ServerTransport;
import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.network.aion.AionConnectionFactoryImpl;
import com.aionemu.loginserver.network.gameserver.GsConnectionFactoryImpl;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;

/**
 * 登录服网络传输生命周期门面：创建、获取与关闭 GS/Aion 监听。
 * Login-server network transport lifecycle facade: create, obtain and shut down GS/Aion listeners.
 *
 * @author KID
 */
@UtilityClass
public class NetConnector {

    private final Object lifecycleLock = new Object();
    private Supplier<ServerTransport> transportFactory = NetConnector::createTransport;
    private ServerTransport transport;
    private boolean initialized;

    /**
     * 按配置创建默认 Netty 传输（GS + Aion 双监听）。
     * Create the default Netty transport (GS + Aion listeners) from config.
     *
     * @return 新建传输实例 / New transport instance
     */
    private ServerTransport createTransport() {
        return new NettyServer(
            new NettyServerCfg(Config.GAME_BIND_ADDRESS, Config.GAME_PORT, "Gs Connections", new GsConnectionFactoryImpl()),
            new NettyServerCfg(Config.LOGIN_BIND_ADDRESS, Config.LOGIN_PORT, "Aion Connections", new AionConnectionFactoryImpl())
        );
    }

    /**
     * 返回当前传输；首次调用时懒创建并标记已初始化。
     * Return current transport; lazily create and mark initialized on first call.
     *
     * @return 服务器传输实例 / Server transport instance
     */
    public ServerTransport currentTransport() {
        synchronized (lifecycleLock) {
            if (transport == null) {
                transport = transportFactory.get();
            }
            initialized = true;
            return transport;
        }
    }

    /**
     * 兼容旧调用，等同 {@link #currentTransport()}。
     * Legacy alias of {@link #currentTransport()}.
     *
     * @return 服务器传输实例 / Server transport instance
     * Prefer {@link #currentTransport()}。 / Prefer {@link #currentTransport()}
     */
    @Deprecated(since = "boot-migration")
    public ServerTransport getInstance() {
        return currentTransport();
    }

    /**
     * 若已初始化则关闭传输并复位状态。
     * Shut down transport and reset state when previously initialized.
     *
     * @return 是否执行了关闭 / Whether shutdown ran
     */
    public boolean shutdownIfInitialized() {
        ServerTransport activeTransport;
        synchronized (lifecycleLock) {
            if (!initialized) {
                return false;
            }
            activeTransport = transport;
            transport = null;
            initialized = false;
        }

        if (activeTransport != null) {
            activeTransport.shutdown();
        }
        return true;
    }

    /**
     * 测试用：替换传输工厂并清空当前实例。
     * Test helper: replace transport factory and clear current instance.
     *
     * Transport factory
     */
    void useTransportFactory(Supplier<ServerTransport> factory) {
        synchronized (lifecycleLock) {
            transportFactory = factory;
            transport = null;
            initialized = false;
        }
    }

    /**
     * 测试用：恢复默认传输工厂。
     * Test helper: restore the default transport factory.
     */
    void resetTransportFactory() {
        useTransportFactory(NetConnector::createTransport);
    }
}
