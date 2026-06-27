package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.PacketLoggerService;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;

final class GameServerNetworkFallbacks {

    private GameServerNetworkFallbacks() {
    }

    static BannedMacManager bannedMacManager() {
        return BannedMacManagerFallback.INSTANCE;
    }

    static LoginServer loginServer() {
        return LoginServerFallback.INSTANCE;
    }

    static ChatServer chatServer() {
        return ChatServerFallback.INSTANCE;
    }

    static PacketLoggerService packetLoggerService() {
        return PacketLoggerServiceFallback.INSTANCE;
    }

    private static final class BannedMacManagerFallback {
        private static final BannedMacManager INSTANCE = BannedMacManager.getInstance();
    }

    private static final class LoginServerFallback {
        private static final LoginServer INSTANCE = LoginServer.getInstance();
    }

    private static final class ChatServerFallback {
        private static final ChatServer INSTANCE = ChatServer.getInstance();
    }

    private static final class PacketLoggerServiceFallback {
        private static final PacketLoggerService INSTANCE = PacketLoggerService.getInstance();
    }
}
