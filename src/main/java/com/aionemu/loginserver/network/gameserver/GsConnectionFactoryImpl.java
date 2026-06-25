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


package com.aionemu.loginserver.network.gameserver;

import java.io.IOException;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.commons.network.NettyConnectionFactory;

/**
 * NettyConnectionFactory implementation that will be creating GsConnections.
 *
 * @author -Nemesiss-
 */
public class GsConnectionFactoryImpl implements NettyConnectionFactory {

    @Override
    public AConnection create(ConnectionTransport transport) throws IOException {
        return new GsConnection(transport);
    }
}
