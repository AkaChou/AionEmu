package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

class SM_LEGION_SEARCHTest {

    @Test
    void writesZeroCountForEmptySearchResults() {
        SM_LEGION_SEARCH packet = new SM_LEGION_SEARCH(List.of());

        ByteBuffer buffer = write(packet);

        assertEquals(0, Short.toUnsignedInt(buffer.getShort()));
    }

    private static ByteBuffer write(SM_LEGION_SEARCH packet) {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        packet.setBuf(buffer);
        packet.writeImpl(null);
        buffer.flip();
        return buffer;
    }
}
