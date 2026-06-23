package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.GameServer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameServerNetworkLifecycleTest {

    @Test
    void startAndStopDelegateToLegacyNetworkBoundary() {
        List<String> events = new ArrayList<>();
        GameServer server = new GameServer();
        GameServerNetworkLifecycle lifecycle = new GameServerNetworkLifecycle(
            startedServer -> events.add("start:" + (startedServer == server)),
            () -> events.add("stop")
        );

        lifecycle.start(server);
        lifecycle.stop();

        assertEquals(List.of("start:true", "stop"), events);
    }
}
