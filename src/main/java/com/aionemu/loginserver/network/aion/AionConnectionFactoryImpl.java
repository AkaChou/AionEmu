package com.aionemu.loginserver.network.aion;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.commons.network.NettyConnectionFactory;
import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.service.LoginProtectionServices;
import java.io.IOException;

/**
 * 创建 {@link LoginConnection} 的 Netty 连接工厂；可选洪水防护。
 * Netty connection factory that creates {@link LoginConnection}; optional flood protection.
 *
 * @author -Nemesiss-
 */
public class AionConnectionFactoryImpl implements NettyConnectionFactory {

    /**
     * 为传入传输创建登录连接；洪水过快则关闭并返回 null。
     * Create a login connection for the inbound transport; close and return null if flood-limited.
     *
     * @param transport 连接传输 / Connection transport
     * @return 新连接，或洪水拦截时为 null / New connection, or null when flood-blocked
     * @throws IOException 创建失败时 / On creation failure
     */
    @Override
    public AConnection create(ConnectionTransport transport) throws IOException {
        if (Config.ENABLE_FLOOD_PROTECTION) {
            if (LoginProtectionServices.floodProtector().tooFast(transport.getIP())) {
                transport.close(true);
                return null;
            }
        }

        return new LoginConnection(transport);
    }
}
