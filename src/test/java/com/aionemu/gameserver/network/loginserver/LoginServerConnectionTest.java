package com.aionemu.gameserver.network.loginserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.commons.network.ConnectionTransport;
import org.junit.jupiter.api.Test;

class LoginServerConnectionTest {

    @Test
    void supportsNettyTransportConstruction() {
        RecordingTransport transport = new RecordingTransport();
        LoginServerConnection connection = new LoginServerConnection(transport);

        assertEquals(LoginServerConnection.State.CONNECTED, connection.getState());

        connection.close(false);

        assertTrue(transport.closed);
    }

    private static final class RecordingTransport implements ConnectionTransport {
        private boolean closed;

        @Override
        public String getIP() {
            return "127.0.0.1";
        }

        @Override
        public void enableWriteInterest() {
        }

        @Override
        public void close(boolean forced) {
            closed = true;
        }

        @Override
        public boolean onlyClose() {
            closed = true;
            return true;
        }
    }
}
