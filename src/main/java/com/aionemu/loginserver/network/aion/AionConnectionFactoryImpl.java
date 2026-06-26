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


package com.aionemu.loginserver.network.aion;

import java.io.IOException;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.commons.network.NettyConnectionFactory;
import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.service.LoginProtectionServices;

/**
 * NettyConnectionFactory implementation that will be creating AionConnections.
 *
 * @author -Nemesiss-
 */
public class AionConnectionFactoryImpl implements NettyConnectionFactory {

    @Override
    public AConnection create(ConnectionTransport transport) throws IOException {
        if (Config.ENABLE_FLOOD_PROTECTION) {
            if (LoginProtectionServices.floodProtector().tooFast(transport.getIP())) {
                transport.close(true);
                return null;
            }
        }

        return new LoginConnection(transport);
    }
}
