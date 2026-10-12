package com.aionemu.loginserver.network;

import com.aionemu.commons.network.NettyServer;
import com.aionemu.commons.network.NettyServerCfg;
import com.aionemu.commons.network.ServerTransport;
import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.controller.AccountController;
import com.aionemu.loginserver.controller.AccountTimeController;
import com.aionemu.loginserver.network.aion.LoginClientConnectionFactory;
import com.aionemu.loginserver.network.factories.AionPacketHandlerFactory;
import com.aionemu.loginserver.network.factories.GsPacketHandlerFactory;
import com.aionemu.loginserver.persistence.dao.SvstatDao;
import com.aionemu.loginserver.utils.FloodProtector;
import com.aionemu.loginserver.utils.ThreadPoolManager;
import com.aionemu.loginserver.network.gameserver.GameServerConnectionFactory;
import java.util.function.Supplier;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * 登录服网络传输生命周期：按配置创建、获取与关闭 GS/Aion 监听。
 * Login-server network transport lifecycle: create, obtain and shut down GS/Aion listeners.
 * @author KID
 */
@Service
@Lazy
@RequiredArgsConstructor
public class NetConnector {

    private final GsPacketHandlerFactory gsPacketFactory;
    private final AionPacketHandlerFactory aionPacketFactory;
    private final AccountController accountController;
    private final AccountTimeController accountTimeController;
    private final FloodProtector floodProtector;
    private final SvstatDao svstatsDao;
    private final ThreadPoolManager threadPoolManager;

    private final Object lifecycleLock = new Object();
    private Supplier<ServerTransport> transportFactory = this::createTransport;
    private ServerTransport transport;
    private boolean initialized;

    /**
     * 按配置创建默认 Netty 传输（GS + Aion 双监听）。
     * Create the default Netty transport (GS + Aion listeners) from config.
     * @return 新建传输实例 / New transport instance
     */
    private ServerTransport createTransport() {
        return new NettyServer(
            new NettyServerCfg(Config.GAME_BIND_ADDRESS, Config.GAME_PORT, "Gs Connections", new GameServerConnectionFactory(gsPacketFactory, svstatsDao, threadPoolManager)),
            new NettyServerCfg(Config.LOGIN_BIND_ADDRESS, Config.LOGIN_PORT, "Aion Connections", new LoginClientConnectionFactory(aionPacketFactory, accountController, accountTimeController, floodProtector))
        );
    }

    /**
     * 返回当前传输；首次调用时懒创建并标记已初始化。
     * Return current transport; lazily create and mark initialized on first call.
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
     * 若已初始化则关闭传输并复位状态。
     * Shut down transport and reset state when previously initialized.
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
     * @param factory 传输工厂 / transport factory
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
        useTransportFactory(this::createTransport);
    }
}
