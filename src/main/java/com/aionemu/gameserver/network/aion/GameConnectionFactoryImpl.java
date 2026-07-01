/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.network.aion;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.commons.network.NettyConnectionFactory;
import com.aionemu.commons.services.ServiceContext;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.network.sequrity.FloodManager;
import com.aionemu.gameserver.network.sequrity.FloodManager.Result;

/**
 * NettyConnectionFactory implementation that will be creating AionConnections.
 * 
 * @author -Nemesiss-
 */
@Slf4j
public class GameConnectionFactoryImpl implements NettyConnectionFactory {

	private static final String GAME_CONTEXT = "game";

	private FloodManager floodAcceptor;

	public GameConnectionFactoryImpl() {
		if (NetworkConfig.ENABLE_FLOOD_CONNECTIONS) {
			floodAcceptor = new FloodManager(NetworkConfig.Flood_Tick,
					new FloodManager.FloodFilter(NetworkConfig.Flood_SWARN, NetworkConfig.Flood_SReject,
							NetworkConfig.Flood_STick), // short period
					new FloodManager.FloodFilter(NetworkConfig.Flood_LWARN, NetworkConfig.Flood_LReject,
							NetworkConfig.Flood_LTick)); // long period
		}
	}

	@Override
	public AConnection create(ConnectionTransport transport) throws IOException {
		if (NetworkConfig.ENABLE_FLOOD_CONNECTIONS) {
			final Result isFlooding = floodAcceptor.isFlooding(transport.getIP(), true);
			switch (isFlooding) {
			case REJECTED: {
				log.warn("Rejected connection from " + transport.getIP());
				transport.close(true);
				return null;
			}
			case WARNED: {
				log.warn("Connection over warn limit from " + transport.getIP());
				break;
			}
			}
		}
		try (ServiceContext.Scope ignored = ServiceContext.use(GAME_CONTEXT)) {
			return new AionConnection(transport);
		}
	}
}
