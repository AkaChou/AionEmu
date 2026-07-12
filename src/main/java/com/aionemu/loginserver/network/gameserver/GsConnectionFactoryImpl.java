package com.aionemu.loginserver.network.gameserver;

import java.io.IOException;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.commons.network.NettyConnectionFactory;

/**
 * 创建 {@link GsConnection} 的 Netty 连接工厂实现。
 * {@link NettyConnectionFactory} implementation that creates {@link GsConnection} instances.
 *
 * @author -Nemesiss-
 */
public class GsConnectionFactoryImpl implements NettyConnectionFactory {

    /**
     * 基于传输层创建 GS 连接。
     * Create a GS connection for the given transport.
     *
     * Connection transport
     *
     * @param transport
     * @return 新建的 GS 连接 / New GS connection
     * @return
     * @throws IOException 创建失败时 / When creation fails
     */
    @Override
    public AConnection create(ConnectionTransport transport) throws IOException {
        return new GsConnection(transport);
    }
}
