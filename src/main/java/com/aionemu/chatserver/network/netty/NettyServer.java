/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 *  Aion-Lightning is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Aion-Lightning is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details. *
 *  You should have received a copy of the GNU General Public License
 *  along with Aion-Lightning.
 *  If not, see <http://www.gnu.org/licenses/>.
 */


package com.aionemu.chatserver.network.netty;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.chatserver.configs.Config;
import com.aionemu.chatserver.network.aion.ClientPacketHandler;
import com.aionemu.chatserver.network.gameserver.GsConnectionFactoryImpl;
import com.aionemu.commons.network.NettyServerCfg;
import com.aionemu.commons.network.ServerTransport;

/**
 * @author ATracer
 */
@Slf4j
public class NettyServer {

    private Netty4ChatClientServer netty4ChatClientServer;
    private ServerTransport gameServerTransport;
    private static NettyServer instance;

    @Deprecated(since = "boot-migration")
    public static synchronized NettyServer getInstance() {
        if (instance == null) {
            instance = new NettyServer(new ClientPacketHandler());
        }
        return instance;
    }

    @Deprecated(since = "boot-migration")
    public static synchronized NettyServer getInstance(ClientPacketHandler clientPacketHandler) {
        if (instance == null) {
            instance = new NettyServer(clientPacketHandler);
        }
        return instance;
    }

    public static void shutdownIfInitialized() {
        NettyServer server;
        synchronized (NettyServer.class) {
            server = instance;
            instance = null;
        }
        if (server != null) {
            server.shutdownAll();
        }
    }

    static synchronized boolean isInitialized() {
        return instance != null;
    }

    public NettyServer() {
        initialize(new ClientPacketHandler());
    }

    public NettyServer(ClientPacketHandler clientPacketHandler) {
        initialize(clientPacketHandler);
    }

    /**
     * Initialize listening on login port
     */
    public void initialize() {
        initialize(new ClientPacketHandler());
    }

    private void initialize(ClientPacketHandler clientPacketHandler) {
        netty4ChatClientServer = new Netty4ChatClientServer(Config.CHAT_ADDRESS, clientPacketHandler);
        netty4ChatClientServer.connect();
        String gameHost = Config.GAME_ADDRESS.getAddress().getHostAddress();
        int gamePort = Config.GAME_ADDRESS.getPort();
        gameServerTransport = new com.aionemu.commons.network.NettyServer(
            new NettyServerCfg(gameHost, gamePort, "Gs Connections", new GsConnectionFactoryImpl())
        );
        gameServerTransport.connect();
    }

    /**
     * Shutdown server
     */
    public void shutdownAll() {
        if (netty4ChatClientServer != null) {
            netty4ChatClientServer.shutdown();
        }
        if (gameServerTransport != null) {
            gameServerTransport.shutdown();
        }
    }
}
