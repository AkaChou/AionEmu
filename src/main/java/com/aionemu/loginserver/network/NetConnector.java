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

import com.aionemu.commons.network.NettyServer;
import com.aionemu.commons.network.NettyServerCfg;
import com.aionemu.commons.network.ServerTransport;
import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.network.aion.AionConnectionFactoryImpl;
import com.aionemu.loginserver.network.gameserver.GsConnectionFactoryImpl;
import java.util.function.Supplier;

/**
 *
 * @author KID
 *
 */
public class NetConnector {

    private static final Object lifecycleLock = new Object();
    private static Supplier<ServerTransport> transportFactory = NetConnector::createTransport;
    private static ServerTransport transport;
    private static boolean initialized;

    private static ServerTransport createTransport() {
        return new NettyServer(
            new NettyServerCfg(Config.GAME_BIND_ADDRESS, Config.GAME_PORT, "Gs Connections", new GsConnectionFactoryImpl()),
            new NettyServerCfg(Config.LOGIN_BIND_ADDRESS, Config.LOGIN_PORT, "Aion Connections", new AionConnectionFactoryImpl())
        );
    }

    /**
     * @return server transport instance.
     */
    public static ServerTransport getInstance() {
        synchronized (lifecycleLock) {
            if (transport == null) {
                transport = transportFactory.get();
            }
            initialized = true;
            return transport;
        }
    }

    public static boolean shutdownIfInitialized() {
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

    static void useTransportFactory(Supplier<ServerTransport> factory) {
        synchronized (lifecycleLock) {
            transportFactory = factory;
            transport = null;
            initialized = false;
        }
    }

    static void resetTransportFactory() {
        useTransportFactory(NetConnector::createTransport);
    }
}
