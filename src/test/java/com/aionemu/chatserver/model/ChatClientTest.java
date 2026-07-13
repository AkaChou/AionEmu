package com.aionemu.chatserver.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatClientTest {

    @Test
    void acceptsPrivateUseAdminTagWithoutAcceptingArbitraryPrefix() {
        ChatClient client = new ChatClient(1, new byte[0], "Zz");

        assertTrue(client.same("Zz"));
        assertTrue(client.same("\uE050 Admin \uE050 Zz"));
        assertFalse(client.same("Fake Zz"));
    }
}
