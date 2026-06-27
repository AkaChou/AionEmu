package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.NetworkController;
import com.aionemu.gameserver.network.PacketFloodFilter;
import com.aionemu.gameserver.network.PacketLoggerService;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.factories.AionPacketHandlerFactory;
import com.aionemu.gameserver.network.factories.LsPacketHandlerFactory;
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

    static NetworkController networkController() {
        return NetworkControllerFallback.INSTANCE;
    }

    static AionPacketHandlerFactory aionPacketHandlerFactory() {
        return AionPacketHandlerFactoryFallback.INSTANCE;
    }

    static PacketFloodFilter packetFloodFilter() {
        return PacketFloodFilterFallback.INSTANCE;
    }

    static LsPacketHandlerFactory lsPacketHandlerFactory() {
        return LsPacketHandlerFactoryFallback.INSTANCE;
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

    private static final class NetworkControllerFallback {
        private static final NetworkController INSTANCE = NetworkController.getInstance();
    }

    private static final class AionPacketHandlerFactoryFallback {
        private static final AionPacketHandlerFactory INSTANCE = AionPacketHandlerFactory.getInstance();
    }

    private static final class PacketFloodFilterFallback {
        private static final PacketFloodFilter INSTANCE = PacketFloodFilter.getInstance();
    }

    private static final class LsPacketHandlerFactoryFallback {
        private static final LsPacketHandlerFactory INSTANCE = LsPacketHandlerFactory.getInstance();
    }
}
