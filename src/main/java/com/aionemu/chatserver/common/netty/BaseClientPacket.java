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


import lombok.extern.slf4j.Slf4j;
@Slf4j
public abstract class BaseClientPacket extends AbstractPacket {

    private PacketReader buf;

    /**
     * @param packetReader
     * @param opCode
     */
    public BaseClientPacket(PacketReader packetReader, int opCode) {
        super(opCode);
        this.buf = packetReader;
    }

    public int getRemainingBytes() {
        return buf.readableBytes();
    }

    /**
     * Perform packet read
     *
     * @return boolean
     */
    public boolean read() {
        try {
            readImpl();
            if (getRemainingBytes() > 0) {
                log.debug("Packet " + this + " not fully readed!");
            }
            return true;
        } catch (Exception ex) {
            log.error("Reading failed for packet " + this, ex);
            return false;
        }

    }

    /**
     * Perform packet action
     */
    public void run() {
        try {
            runImpl();
        } catch (Exception ex) {
            log.error("Running failed for packet " + this, ex);
        }
    }

    protected abstract void readImpl();

    protected abstract void runImpl();

    /**
     * Read int from this packet buffer.
     *
     * @return int
     */
    protected final int readD() {
        try {
            return buf.readD();
        } catch (Exception e) {
            log.error("Missing D for: " + this);
        }
        return 0;
    }

    /**
     * Read byte from this packet buffer.
     *
     * @return int
     */
    protected final int readC() {
        try {
            return buf.readC();
        } catch (Exception e) {
            log.error("Missing C for: " + this);
        }
        return 0;
    }

    /**
     * Read short from this packet buffer.
     *
     * @return int
     */
    protected final int readH() {
        try {
            return buf.readH();
        } catch (Exception e) {
            log.error("Missing H for: " + this);
        }
        return 0;
    }

    /**
     * Read double from this packet buffer.
     *
     * @return double
     */
    protected final double readDF() {
        try {
            return buf.readDF();
        } catch (Exception e) {
            log.error("Missing DF for: " + this);
        }
        return 0;
    }

    /**
     * Read double from this packet buffer.
     *
     * @return double
     */
    protected final float readF() {
        try {
            return buf.readF();
        } catch (Exception e) {
            log.error("Missing F for: " + this);
        }
        return 0;
    }

    /**
     * Read long from this packet buffer.
     *
     * @return long
     */
    protected final long readQ() {
        try {
            return buf.readQ();
        } catch (Exception e) {
            log.error("Missing Q for: " + this);
        }
        return 0;
    }

    /**
     * Read String from this packet buffer.
     *
     * @return String
     */
    protected final String readS() {
        StringBuffer sb = new StringBuffer();
        char ch;
        try {
            while ((ch = buf.readChar()) != 0) {
                sb.append(ch);
            }
        } catch (Exception e) {
            log.error("Missing S for: " + this);
        }
        return sb.toString();

    }

    /**
     * Read n bytes from this packet buffer, n = length.
     *
     * @param length
     * @return byte[]
     */
    protected final byte[] readB(int length) {
        byte[] result = new byte[length];
        try {
            buf.readBytes(result);
        } catch (Exception e) {
            log.error("Missing byte[] for: " + this);
        }
        return result;
    }
}
