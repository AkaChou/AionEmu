package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.NetworkController;
import com.aionemu.gameserver.network.PacketFloodFilter;
import com.aionemu.gameserver.network.PacketLoggerService;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.factories.AionPacketHandlerFactory;
import com.aionemu.gameserver.network.factories.LsPacketHandlerFactory;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 服务器网络服务静态绑定：把 Spring ObjectProvider 喂入服务器网络域服务自持的 fail-fast 单例通道。
 * Server-network static bindings: feeds the Spring ObjectProviders into the fail-fast singleton accessors
 * held by the Server-network services.
 * <p>本组件取代已退役的服务器网络静态定位器（R5 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired Server-network static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class ServerNetworkServiceBindings implements DisposableBean {

    /**
     * 构造并注册各服务器网络服务的实例提供者。
     * Construct and register instance providers for Server-network services.
     * @param loginServerProvider LoginServer 提供者 / LoginServer provider
     * @param chatServerProvider ChatServer 提供者 / ChatServer provider
     * @param bannedMacManagerProvider BannedMacManager 提供者 / BannedMacManager provider
     * @param networkControllerProvider NetworkController 提供者 / NetworkController provider
     * @param packetLoggerServiceProvider PacketLoggerService 提供者 / PacketLoggerService provider
     * @param aionPacketHandlerFactoryProvider AionPacketHandlerFactory 提供者 / AionPacketHandlerFactory provider
     * @param packetFloodFilterProvider PacketFloodFilter 提供者 / PacketFloodFilter provider
     * @param lsPacketHandlerFactoryProvider LsPacketHandlerFactory 提供者 / LsPacketHandlerFactory provider
     */
    public ServerNetworkServiceBindings(ObjectProvider<LoginServer> loginServerProvider, ObjectProvider<ChatServer> chatServerProvider,
            ObjectProvider<BannedMacManager> bannedMacManagerProvider, ObjectProvider<NetworkController> networkControllerProvider,
            ObjectProvider<PacketLoggerService> packetLoggerServiceProvider,
            ObjectProvider<AionPacketHandlerFactory> aionPacketHandlerFactoryProvider,
            ObjectProvider<PacketFloodFilter> packetFloodFilterProvider,
            ObjectProvider<LsPacketHandlerFactory> lsPacketHandlerFactoryProvider) {
        LoginServer.setInstanceProvider(loginServerProvider);
        ChatServer.setInstanceProvider(chatServerProvider);
        NetworkController.setInstanceProvider(networkControllerProvider);
        PacketLoggerService.setInstanceProvider(packetLoggerServiceProvider);
        AionPacketHandlerFactory.setInstanceProvider(aionPacketHandlerFactoryProvider);
        PacketFloodFilter.setInstanceProvider(packetFloodFilterProvider);
        LsPacketHandlerFactory.setInstanceProvider(lsPacketHandlerFactoryProvider);
        BannedMacManager.setInstanceProvider(bannedMacManagerProvider);
    }

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        LoginServer.setInstanceProvider(null);
        ChatServer.setInstanceProvider(null);
        NetworkController.setInstanceProvider(null);
        PacketLoggerService.setInstanceProvider(null);
        AionPacketHandlerFactory.setInstanceProvider(null);
        PacketFloodFilter.setInstanceProvider(null);
        LsPacketHandlerFactory.setInstanceProvider(null);
        BannedMacManager.setInstanceProvider(null);
    }
}
