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


package com.aionemu.chatserver.common.netty;

/**
 * @author ATracer
 */
public abstract class BaseServerPacket extends AbstractPacket {

    /**
     * @param opCode
     */
    public BaseServerPacket(int opCode) {
        super(opCode);
    }

    /**
     * @param buf
     * @param value
     */
    protected final void writeD(PacketWriter buf, int value) {
        buf.writeD(value);
    }

    /**
     * @param buf
     * @param value
     */
    protected final void writeH(PacketWriter buf, int value) {
        buf.writeH(value);
    }

    /**
     * @param buf
     * @param value
     */
    protected final void writeC(PacketWriter buf, int value) {
        buf.writeC(value);
    }

    /**
     * Write double to buffer.
     *
     * @param buf
     * @param value
     */
    protected final void writeDF(PacketWriter buf, double value) {
        buf.writeDF(value);
    }

    /**
     * Write float to buffer.
     *
     * @param buf
     * @param value
     */
    protected final void writeF(PacketWriter buf, float value) {
        buf.writeF(value);
    }

    /**
     * @param buf
     * @param data
     */
    protected final void writeB(PacketWriter buf, byte[] data) {
        buf.writeB(data);
    }

    /**
     * Write String to buffer
     *
     * @param buf
     * @param text
     */
    protected final void writeS(PacketWriter buf, String text) {
        if (text == null) {
            buf.writeChar('\000');
        } else {
            final int len = text.length();
            for (int i = 0; i < len; i++) {
                buf.writeChar(text.charAt(i));
            }
            buf.writeChar('\000');
        }
    }

    /**
     * @param buf
     * @param data
     */
    protected final void writeQ(PacketWriter buf, long data) {
        buf.writeQ(data);
    }
}
