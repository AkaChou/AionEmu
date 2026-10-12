package com.aionemu.loginserver.network.gameserver;


import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.commons.network.NettyConnectionFactory;
import com.aionemu.loginserver.network.factories.GsPacketHandlerFactory;
import com.aionemu.loginserver.persistence.dao.SvstatDao;
import com.aionemu.loginserver.utils.ThreadPoolManager;

import lombok.RequiredArgsConstructor;

/**
 * 创建游戏服（{@link GsConnection}）入站连接的 Netty 连接工厂。
 * Netty connection factory for inbound game-server ({@link GsConnection}) connections.
 * @author -Nemesiss-
 */
@RequiredArgsConstructor
public class GameServerConnectionFactory implements NettyConnectionFactory {

    private final GsPacketHandlerFactory packetFactory;
    private final SvstatDao svstatsDao;
    private final ThreadPoolManager threadPoolManager;

    /**
     * 基于传输层创建 GS 连接。
     * Create a GS connection for the given transport.
     * @param transport 连接传输 / connection transport
     * @return 新建的 GS 连接 / new GS connection
     */
    @Override
    public AConnection create(ConnectionTransport transport) {
        return new GsConnection(transport, packetFactory, svstatsDao, threadPoolManager);
    }
}
