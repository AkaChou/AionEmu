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


package com.aionemu.chatserver.network.aion;

import com.aionemu.chatserver.common.netty.BaseServerPacket;
import com.aionemu.chatserver.common.netty.PacketWriter;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;

/**
 * @author ATracer
 */
public abstract class AbstractServerPacket extends BaseServerPacket {

    /**
     * @param opCode
     */
    public AbstractServerPacket(int opCode) {
        super(opCode);
    }

    /**
     * @param clientChannelHandler
     * @param buf
     */
    public void write(ClientChannelHandler clientChannelHandler, PacketWriter buf) {
        buf.writeH(0);
        writeImpl(clientChannelHandler, buf);
    }

    /**
     * @param cHandler
     * @param buf
     */
    protected abstract void writeImpl(ClientChannelHandler cHandler, PacketWriter buf);
}
