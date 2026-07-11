package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.aion.GameConnectionFactoryImpl;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 服务器网络运行时桥接：解析 MAC 封禁、登录/聊天服连接，并创建客户端连接工厂。
 * Runtime bridge for server networking: resolves banned-MAC, login/chat peers, and the client connection factory.
 */
@Component
public class GameServerNetworkRuntimeBridge {

    /**
     * MAC 封禁管理器提供者（可选）。
     * Optional banned-MAC manager provider.
     */
    private ObjectProvider<BannedMacManager> bannedMacManagerProvider;

    /**
     * 登录服连接提供者（可选）。
     * Optional login-server provider.
     */
    private ObjectProvider<LoginServer> loginServerProvider;

    /**
     * 聊天服连接提供者（可选）。
     * Optional chat-server provider.
     */
    private ObjectProvider<ChatServer> chatServerProvider;

    /**
     * 注入 MAC 封禁管理器提供者。
     * Inject the banned-MAC manager provider.
     *
     * @param bannedMacManagerProvider MAC 封禁管理器提供者 / Banned-MAC manager provider
     */
    @Autowired(required = false)
    void setBannedMacManagerProvider(ObjectProvider<BannedMacManager> bannedMacManagerProvider) {
        this.bannedMacManagerProvider = bannedMacManagerProvider;
    }

    /**
     * 注入登录服连接提供者。
     * Inject the login-server provider.
     *
     * @param loginServerProvider 登录服连接提供者 / Login-server provider
     */
    @Autowired(required = false)
    void setLoginServerProvider(ObjectProvider<LoginServer> loginServerProvider) {
        this.loginServerProvider = loginServerProvider;
    }

    /**
     * 注入聊天服连接提供者。
     * Inject the chat-server provider.
     *
     * @param chatServerProvider 聊天服连接提供者 / Chat-server provider
     */
    @Autowired(required = false)
    void setChatServerProvider(ObjectProvider<ChatServer> chatServerProvider) {
        this.chatServerProvider = chatServerProvider;
    }

    /**
     * 解析 MAC 封禁管理器。
     * Resolve the banned-MAC manager.
     *
     * @return MAC 封禁管理器 / Banned MAC manager
     */
    public BannedMacManager bannedMacManager() {
        if (bannedMacManagerProvider == null) {
            return GameServerNetworkFallbacks.bannedMacManager();
        }
        return bannedMacManagerProvider.getIfAvailable(GameServerNetworkFallbacks::bannedMacManager);
    }

    /**
     * 解析登录服连接。
     * Resolve the login-server connection.
     *
     * Login server
     */
    public LoginServer loginServer() {
        if (loginServerProvider == null) {
            return GameServerNetworkFallbacks.loginServer();
        }
        return loginServerProvider.getIfAvailable(GameServerNetworkFallbacks::loginServer);
    }

    /**
     * 解析聊天服连接。
     * Resolve the chat-server connection.
     *
     * Chat server
     */
    public ChatServer chatServer() {
        if (chatServerProvider == null) {
            return GameServerNetworkFallbacks.chatServer();
        }
        return chatServerProvider.getIfAvailable(GameServerNetworkFallbacks::chatServer);
    }

    /**
     * 创建游戏客户端连接工厂。
     * Create the game-client connection factory.
     *
     * @return 客户端连接工厂 / Game connection factory
     */
    public GameConnectionFactoryImpl gameConnectionFactory() {
        return new GameConnectionFactoryImpl();
    }
}
