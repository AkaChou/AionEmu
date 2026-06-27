package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

class SM_DISPUTE_LANDTest {

    @Test
    void writesActiveStateForEachDisputeWorld() {
        SM_DISPUTE_LAND packet = new SM_DISPUTE_LAND(List.of(210020000, 220020000), true);

        ByteBuffer buffer = write(packet);

        assertEquals(2, Short.toUnsignedInt(buffer.getShort()));
        assertWorld(buffer, 0x02, 210020000);
        assertWorld(buffer, 0x02, 220020000);
    }

    private static ByteBuffer write(SM_DISPUTE_LAND packet) {
        ByteBuffer buffer = ByteBuffer.allocate(128);
        packet.setBuf(buffer);
        packet.writeImpl(null);
        buffer.flip();
        return buffer;
    }

    private static void assertWorld(ByteBuffer buffer, int state, int worldId) {
        assertEquals(state, buffer.getInt());
        assertEquals(worldId, buffer.getInt());
        assertEquals(0, buffer.getLong());
        assertEquals(0, buffer.getLong());
        assertEquals(0, buffer.getLong());
        assertEquals(0, buffer.getLong());
    }
}
