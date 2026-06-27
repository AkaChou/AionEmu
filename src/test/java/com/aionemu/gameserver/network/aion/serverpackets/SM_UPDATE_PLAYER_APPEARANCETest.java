package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

class SM_UPDATE_PLAYER_APPEARANCETest {

    @Test
    void writesPlayerIdAndEmptyMaskForNoAppearanceItems() {
        SM_UPDATE_PLAYER_APPEARANCE packet = new SM_UPDATE_PLAYER_APPEARANCE(42, List.of());

        ByteBuffer buffer = write(packet);

        assertEquals(42, buffer.getInt());
        assertEquals(0, buffer.getInt());
    }

    private static ByteBuffer write(SM_UPDATE_PLAYER_APPEARANCE packet) {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        packet.setBuf(buffer);
        packet.writeImpl(null);
        buffer.flip();
        return buffer;
    }
}
