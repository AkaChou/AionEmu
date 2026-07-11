package com.aionemu.chatserver.network.gameserver;

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
     * 基于传输层创建游戏服连接。
     * Creates a game-server connection for the given transport.
     *
     * @param transport 底层连接传输 / underlying connection transport
     * @return 新建的连接实例 / newly created connection
     * thrown when creation fails。 / thrown when creation fails.
     */
    @Override
    public AConnection create(ConnectionTransport transport) throws IOException {
        return new GsConnection(transport);
    }
}
