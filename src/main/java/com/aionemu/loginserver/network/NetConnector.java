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


package com.aionemu.loginserver.network;

import com.aionemu.commons.network.NioServer;
import com.aionemu.commons.network.NettyServer;
import com.aionemu.commons.network.NettyServerCfg;
import com.aionemu.commons.network.ServerCfg;
import com.aionemu.commons.network.ServerTransport;
import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.network.aion.AionConnectionFactoryImpl;
import com.aionemu.loginserver.network.gameserver.GsConnectionFactoryImpl;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author KID
 *
 */
public class NetConnector {

    /**
     * NioServer instance that will handle io.
     */
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    private static ServerTransport createTransport() {
        ServerCfg aion = new ServerCfg(Config.LOGIN_BIND_ADDRESS, Config.LOGIN_PORT, "Aion Connections", new AionConnectionFactoryImpl());

        ServerCfg gs = new ServerCfg(Config.GAME_BIND_ADDRESS, Config.GAME_PORT, "Gs Connections", new GsConnectionFactoryImpl());

        if (Boolean.getBoolean("aion.transport.netty")) {
            return new NettyServer(
                new NettyServerCfg(Config.GAME_BIND_ADDRESS, Config.GAME_PORT, "Gs Connections", new GsConnectionFactoryImpl()),
                new NettyServerCfg(Config.LOGIN_BIND_ADDRESS, Config.LOGIN_PORT, "Aion Connections", new AionConnectionFactoryImpl())
            );
        }

        return new NioServer(Config.NIO_READ_THREADS, gs, aion);
    }

    private static final class SingletonHolder {
        private static final ServerTransport instance = createTransport();
    }

    /**
     * @return NioServer instance.
     */
    public static ServerTransport getInstance() {
        ServerTransport transport = SingletonHolder.instance;
        initialized.set(true);
        return transport;
    }

    public static boolean shutdownIfInitialized() {
        if (!initialized.get()) {
            return false;
        }

        getInstance().shutdown();
        return true;
    }
}
