package com.aionemu.gameserver.network;

import com.aionemu.commons.utils.AionEmbeddedFailureHandler;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.commons.utils.ExitCode;
import java.util.function.IntConsumer;

public final class GameServerAuthFailure {

    private static volatile IntConsumer exitAction = System::exit;

    private GameServerAuthFailure() {
    }

    public static void notAuthenticated(String peerName) {
        String message = "GameServer is not authenticated at " + peerName + " side";
        if (AionRuntimeMode.isBootEmbedded()) {
            AionEmbeddedFailureHandler.fail(new IllegalStateException(message));
            return;
        }
        exitAction.accept(ExitCode.CODE_ERROR);
    }

    static void setExitActionForTesting(IntConsumer exitAction) {
        GameServerAuthFailure.exitAction = exitAction;
    }

    static void clearExitActionForTesting() {
        exitAction = System::exit;
    }
}
