package com.aionemu.gameserver.network;

import com.aionemu.commons.utils.AionEmbeddedFailureHandler;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.commons.utils.ExitCode;
import java.util.function.IntConsumer;

/**
 * 游戏服认证失败处理：嵌入模式走失败处理器，独立模式退出进程。
 * Game-server auth failure handling: embedded mode uses failure handler, standalone exits the process.
 */
public final class GameServerAuthFailure {

    private static volatile IntConsumer exitAction = System::exit;

    private GameServerAuthFailure() {
    }

    /**
     * 对端未通过认证时的处理入口。
     * Entry point when this game server is not authenticated on the peer side.
     *
     * peer name
     */
    public static void notAuthenticated(String peerName) {
        String message = "GameServer is not authenticated at " + peerName + " side";
        if (AionRuntimeMode.isBootEmbedded()) {
            AionEmbeddedFailureHandler.fail(new IllegalStateException(message));
            return;
        }
        exitAction.accept(ExitCode.CODE_ERROR);
    }

    /**
     * 测试用：注入退出动作。
     * Test-only: inject exit action.
     *
     * exit action
     */
    static void setExitActionForTesting(IntConsumer exitAction) {
        GameServerAuthFailure.exitAction = exitAction;
    }

    /**
     * 测试用：恢复默认退出动作。
     * Test-only: restore default exit action.
     */
    static void clearExitActionForTesting() {
        exitAction = System::exit;
    }
}
