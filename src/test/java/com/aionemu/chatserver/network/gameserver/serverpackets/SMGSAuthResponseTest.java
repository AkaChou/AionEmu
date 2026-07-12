package com.aionemu.chatserver.network.gameserver.serverpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.aionemu.chatserver.configs.Config;
import com.aionemu.chatserver.network.gameserver.GsAuthResponse;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class SMGSAuthResponseTest {

    @Test
    void writesPublicChatAddressInsteadOfBindAddress() {
        Config.CHAT_ADDRESS = new InetSocketAddress("0.0.0.0", 10241);
        Config.PUBLIC_CHAT_ADDRESS = new InetSocketAddress("203.0.113.20", 12000);
        ByteBuffer buffer = ByteBuffer.allocate(16);

        new SM_GS_AUTH_RESPONSE(GsAuthResponse.AUTHED).write(null, buffer);

        byte[] packet = new byte[buffer.limit()];
        buffer.get(packet);
        assertArrayEquals(new byte[] {
            10, 0,
            0, 0,
            (byte) 203, 0, 113, 20,
            (byte) 0xE0, 0x2E
        }, packet);
    }
}
