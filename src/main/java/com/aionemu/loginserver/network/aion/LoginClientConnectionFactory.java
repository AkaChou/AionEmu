package com.aionemu.loginserver.network.aion;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.commons.network.NettyConnectionFactory;
import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.controller.AccountController;
import com.aionemu.loginserver.controller.AccountTimeController;
import com.aionemu.loginserver.network.factories.AionPacketHandlerFactory;
import com.aionemu.loginserver.utils.FloodProtector;

import lombok.RequiredArgsConstructor;

/**
 * 创建登录客户端 {@link LoginConnection} 的 Netty 连接工厂；可选洪水防护。
 * Netty connection factory for login-client {@link LoginConnection} instances; optional flood protection.
 * @author -Nemesiss-
 */
@RequiredArgsConstructor
public class LoginClientConnectionFactory implements NettyConnectionFactory {

    private final AionPacketHandlerFactory packetFactory;
    private final AccountController accountController;
    private final AccountTimeController accountTimeController;
    private final FloodProtector floodProtector;

    /**
     * 为传入传输创建登录连接；洪水过快则关闭并返回 null。
     * Create a login connection for the inbound transport; close and return null if flood-limited.
     * @param transport 连接传输 / Connection transport
     * @return 新连接，或洪水拦截时为 null / New connection, or null when flood-blocked
     */
    @Override
    public AConnection create(ConnectionTransport transport) {
        if (Config.ENABLE_FLOOD_PROTECTION) {
            if (floodProtector.tooFast(transport.getIP())) {
                transport.close(true);
                return null;
            }
        }

        return new LoginConnection(transport, packetFactory, accountController, accountTimeController);
    }
}
