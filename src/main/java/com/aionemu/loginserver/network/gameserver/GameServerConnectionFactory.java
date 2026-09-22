package com.aionemu.loginserver.network.gameserver;

import java.io.IOException;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.commons.network.NettyConnectionFactory;

/**
 * 创建游戏服（{@link GsConnection}）入站连接的 Netty 连接工厂。
 * Netty connection factory for inbound game-server ({@link GsConnection}) connections.
 * @author -Nemesiss-
 */
public class GameServerConnectionFactory implements NettyConnectionFactory {

    /**
     * 基于传输层创建 GS 连接。
     * Create a GS connection for the given transport.
     * @param transport 连接传输 / connection transport
     * @return 新建的 GS 连接 / new GS connection
     * @throws IOException 创建失败时 / when creation fails
     */
    @Override
    public AConnection create(ConnectionTransport transport) throws IOException {
        return new GsConnection(transport);
    }
}
