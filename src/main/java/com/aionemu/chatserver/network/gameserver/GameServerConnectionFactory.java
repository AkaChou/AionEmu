package com.aionemu.chatserver.network.gameserver;

import java.io.IOException;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.commons.network.NettyConnectionFactory;

/**
 * 创建游戏服（{@link GsConnection}）入站连接的 Netty 连接工厂。
 * Netty connection factory for inbound game-server ({@link GsConnection}) connections.
 *
 * @author -Nemesiss-
 */
public class GameServerConnectionFactory implements NettyConnectionFactory {

    /**
     * 基于传输层创建游戏服连接。
     * Creates a game-server connection for the given transport.
     *
     * @param transport 底层连接传输 / underlying connection transport
     * @return 新建的连接实例 / newly created connection
     * @throws IOException 连接创建失败时抛出 / thrown when connection creation fails
     */
    @Override
    public AConnection create(ConnectionTransport transport) throws IOException {
        return new GsConnection(transport);
    }
}
