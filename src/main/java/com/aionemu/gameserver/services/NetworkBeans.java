package com.aionemu.gameserver.services;

import com.aionemu.gameserver.ShutdownHook;
import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.NetworkController;
import com.aionemu.gameserver.network.PacketFloodFilter;
import com.aionemu.gameserver.network.PacketLoggerService;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.factories.AionPacketHandlerFactory;
import com.aionemu.gameserver.network.factories.LsPacketHandlerFactory;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * NetworkBeans：从 GameLegacyServiceBridgeConfiguration 拆出的域内 Bean 装配。
 * NetworkBeans: domain-scoped bean wiring split out of GameLegacyServiceBridgeConfiguration.
 */
@Configuration(proxyBeanMethods = false)
public class NetworkBeans {

    @Bean
    @Lazy
    public ShutdownHook shutdownHook() {
        return new ShutdownHook();
    }

    @Bean
    @Lazy
    public BannedMacManager bannedMacManager() {
        return new BannedMacManager();
    }

    @Bean
    @Lazy
    public PacketLoggerService packetLoggerService() {
        return new PacketLoggerService();
    }

    @Bean
    @Lazy
    public NetworkController networkController() {
        return new NetworkController();
    }

    @Bean
    @Lazy
    public AionPacketHandlerFactory aionPacketHandlerFactory() {
        return new AionPacketHandlerFactory();
    }

    @Bean
    @Lazy
    public PacketFloodFilter packetFloodFilter() {
        return new PacketFloodFilter();
    }

    @Bean
    @Lazy
    public LsPacketHandlerFactory lsPacketHandlerFactory() {
        return new LsPacketHandlerFactory();
    }

    @Bean
    @Lazy
    public LoginServer loginServer() {
        return new LoginServer();
    }

    @Bean
    @Lazy
    public ChatServer chatServer() {
        return new ChatServer();
    }
}
