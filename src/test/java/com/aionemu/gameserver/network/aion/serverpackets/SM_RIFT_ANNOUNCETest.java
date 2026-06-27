package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SM_RIFT_ANNOUNCETest {

    @Test
    void writesAnnounceDataInMapIterationOrder() {
        Map<Integer, Integer> rifts = new LinkedHashMap<>();
        rifts.put(0, 10);
        rifts.put(1, 20);
        rifts.put(2, 30);

        SM_RIFT_ANNOUNCE packet = new SM_RIFT_ANNOUNCE(rifts);

        ByteBuffer buffer = write(packet);

        assertEquals(0x57, Short.toUnsignedInt(buffer.getShort()));
        assertEquals(0, Byte.toUnsignedInt(buffer.get()));
        assertEquals(10, buffer.getInt());
        assertEquals(20, buffer.getInt());
        assertEquals(30, buffer.getInt());
    }

    private static ByteBuffer write(SM_RIFT_ANNOUNCE packet) {
        ByteBuffer buffer = ByteBuffer.allocate(128);
        packet.setBuf(buffer);
        packet.writeImpl(null);
        buffer.flip();
        return buffer;
    }
}
