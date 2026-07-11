package com.aionemu.chatserver.service;


import com.aionemu.boot.i18n.I18n;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.chatserver.model.ChannelType;
import com.aionemu.chatserver.model.ChatClient;
import com.aionemu.chatserver.model.channel.Channel;
import com.aionemu.chatserver.model.channel.ChatChannels;
import com.aionemu.chatserver.network.aion.serverpackets.SM_PLAYER_AUTH_RESPONSE;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler.State;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天业务服务：玩家令牌注册、连接认证、频道加入与禁言管理。
 * Chat business service: player token registration, connection auth, channel join, and gag management.
 *
 * @author ATracer
 */
@Slf4j
public class ChatService {

    /**
     * 获取单例（已废弃，迁移至 Boot 后请使用注入）。
     * Return the singleton (deprecated; prefer injection after Boot migration).
     *
     * Singleton instance
     * @deprecated boot-migration
     */
    @Deprecated(since = "boot-migration")
    public static ChatService getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private Map<Integer, ChatClient> players = new ConcurrentHashMap<>();
    private final BroadcastService broadcastService;

    /**
     * 使用核心服务门面解析的广播服务构造。
     * Construct using the broadcast service resolved from the core services facade.
     */
    public ChatService() {
        this(ChatCoreServices.broadcastService());
    }

    /**
     * 使用指定广播服务构造。
     * Construct with the given broadcast service.
     *
     * Broadcast service
     */
    public ChatService(BroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }

    /**
     * 由游戏服侧注册玩家并生成认证令牌。
     * Register a player from the game-server side and generate an auth token.
     *
     * Player id
     * Login account
     * Nickname
     *
     * @param playerId @return 聊天客户端对象 / Chat client
     * @param playerLogin @throws NoSuchAlgorithmException 摘要算法不可用 / Digest algorithm unavailable
     * @param nick @throws UnsupportedEncodingException 字符编码不支持 / Encoding unsupported
     */
    public ChatClient registerPlayer(int playerId, String playerLogin, String nick) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.reset();
        md.update(playerLogin.getBytes("UTF-8"), 0, playerLogin.length());
        byte[] accountToken = md.digest();
        byte[] token = generateToken(accountToken);
        ChatClient chatClient = new ChatClient(playerId, token, nick);
        players.put(playerId, chatClient);
        return chatClient;
    }

    /**
     * 将动态随机段与账号摘要拼接为 48 字节令牌。
     * Build a 48-byte token from a random dynamic segment and the account digest.
     *
     * Account digest
     * Full token
     */
    private byte[] generateToken(byte[] accountToken) {
        byte[] dynamicToken = new byte[16];
        new Random().nextBytes(dynamicToken);
        byte[] token = new byte[48];
        for (int i = 0; i < token.length; i++) {
            if (i < 16) {
                token[i] = dynamicToken[i];
            } else {
                token[i] = accountToken[i - 16];
            }
        }
        return token;
    }

    /**
     * 处理客户端侧连接认证：校验令牌并绑定通道处理器。
     * Handle client-side connection auth: verify token and bind the channel handler.
     *
     * Player id
     * Token
     * Identifier bytes
     * @param channelHandler 通道处理器 / Channel handler
     * @param realName 真实角色名 / Real character name
     * Encoding unsupported。 / Encoding unsupported.
     */
    public void registerPlayerConnection(int playerId, byte[] token, byte[] identifier, ClientChannelHandler channelHandler, String realName) throws UnsupportedEncodingException {
        ChatClient chatClient = players.get(playerId);
        if (chatClient != null) {
            byte[] regToken = chatClient.getToken();
            chatClient.same(realName);

            if (Arrays.equals(regToken, token)) {
                String sreal = chatClient.getRealName() + "@" + new String(identifier);
                chatClient.setIdentifier(sreal.getBytes("utf-16le"));
                chatClient.setChannelHandler(channelHandler);
                channelHandler.sendPacket(new SM_PLAYER_AUTH_RESPONSE());
                channelHandler.setState(State.AUTHED);
                channelHandler.setChatClient(chatClient);
                broadcastService.addClient(chatClient);
            }
        }
    }

    /**
     * 将玩家加入指定频道（群组类频道避免重复加入）。
     * Join the player to the given channel (group channels avoid duplicate joins).
     *
     * @param chatClient 聊天客户端 / Chat client
     * Channel index
     * Channel identifier
     * @return 加入的频道；未找到或重复群组则为 null / Joined channel, or null if missing/duplicate group
     */
    public Channel registerPlayerWithChannel(ChatClient chatClient, int channelIndex, byte[] channelIdentifier) {
        Channel channel = ChatChannels.getChannelByIdentifier(channelIdentifier);
        if (channel != null) {
            ChannelType channelType = channel.getChannelType();
            if (channelType == ChannelType.GROUP /*|| channelType == ChannelType.JOB*/) {
                if (chatClient.isInChannel(channel)) {
                    return null;
                }
            }
            chatClient.addChannel(channel);
        }
        return channel;
    }

    /**
     * 玩家下线：移除会话、广播集合并关闭通道。
     * Player logout: remove session, leave broadcast set, and close the channel.
     *
     * Player id
     */
    public void playerLogout(int playerId) {
        ChatClient chatClient = players.get(playerId);
        if (chatClient != null) {
            players.remove(playerId);
            broadcastService.removeClient(chatClient);
            if (chatClient.getChannelHandler() != null) {
                chatClient.getChannelHandler().close();
            } else {
                log.warn(I18n.get("log.d22b2f1b1e32", playerId));
            }
        }
    }

    /**
     * 对在线玩家设置禁言截止时间。
     * Set gag end time for an online player.
     *
     * Player id
     * @param gagTime 禁言截止时间戳 / Gag end timestamp
     */
    public void gagPlayer(int playerId, long gagTime) {
        if (players.containsKey(playerId)) {
            ChatClient client = players.get(playerId);
            client.setGagTime(gagTime);
        }
    }

    /**
     * 单例持有者。
     * Singleton holder.
     */
    private static final class SingletonHolder {

        private static final ChatService INSTANCE = new ChatService();
    }
}
