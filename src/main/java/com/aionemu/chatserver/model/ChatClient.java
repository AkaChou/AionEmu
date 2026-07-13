package com.aionemu.chatserver.model;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

import com.aionemu.chatserver.configs.Config;
import com.aionemu.chatserver.model.channel.Channel;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天客户端会话模型，绑定玩家标识、令牌、频道与禁言状态。
 * Chat client session model binding player identity, token, channels and gag state.
 *
 * @author ATracer
 */
@Slf4j
public class ChatClient {

    /**
     * 聊天客户端 ID（玩家 ID）。
     * Chat client id (player id).
     */
    private int clientId;
    /**
     * 发送消息时使用的标识字节。
     * Identifier bytes used when sending messages.
     */
    private byte[] identifier;
    /**
     * 与游戏服鉴权时使用的令牌。
     * Token used during auth with the game server.
     */
    private byte[] token;
    /**
     * 客户端网络通道处理器。
     * Network channel handler of this chat client.
     */
    private ClientChannelHandler channelHandler;
    /**
     * 已加入频道映射；同类型频道仅可存在一个。
     * Joined channels map; only one channel per type is allowed.
     */
    private Map<ChannelType, Channel> channelsList = new ConcurrentHashMap<>();
    /**
     * 最近一次请求并广播消息的时间戳。
     * Timestamp of the last requested and broadcasted message.
     */
    private long lastMessage;
    /**
     * 玩家真实昵称。
     * Player real nickname.
     */
    private String realName;
    /**
     * 禁言结束时间戳（毫秒）；0 表示未禁言。
     * Gag end timestamp in millis; 0 means not gagged.
     */
    private long gagTime;

    /**
     * 创建聊天客户端会话。
     * Creates a chat client session.
     *
     * @param clientId 客户端/玩家 ID / client (player) id
     * @param token 鉴权令牌 / auth token
     * @param nick 玩家昵称 / player nickname
     */
    public ChatClient(int clientId, byte[] token, String nick) {
        this.clientId = clientId;
        this.token = token;
        this.realName = nick;
    }

    /**
     * 加入指定频道（按频道类型覆盖）。
     * Joins the given channel (overwrites by channel type).
     *
     * target channel
     */
    public void addChannel(Channel channel) {
        channelsList.put(channel.getChannelType(), channel);
    }

    /**
     * 获取网络通道处理器。
     * Returns the network channel handler.
     *
     * @return 通道处理器 / channel handler
     */
    public ClientChannelHandler getChannelHandler() {
        return channelHandler;
    }

    /**
     * 获取客户端 ID。
     * Returns the client id.
     *
     * client id
     */
    public int getClientId() {
        return clientId;
    }

    /**
     * 获取发送标识字节。
     * Returns the sender identifier bytes.
     *
     * identifier bytes
     */
    public byte[] getIdentifier() {
        return identifier;
    }

    /**
     * 获取玩家真实昵称。
     * Returns the player real nickname.
     *
     * nickname
     */
    public String getRealName() {
        return realName;
    }

    /**
     * 获取鉴权令牌。
     * Returns the auth token.
     *
     * token bytes
     */
    public byte[] getToken() {
        return token;
    }

    /**
     * 判断是否已在指定类型频道中。
     * Checks whether the client is already in a channel of the same type.
     *
     * @param channel 待检查频道 / channel to check
     * @return 已加入则为 true / true if already joined
     */
    public boolean isInChannel(Channel channel) {
        return channelsList.containsKey(channel.getChannelType());
    }

    /**
     * 设置网络通道处理器。
     * Sets the network channel handler.
     *
     * @param channelHandler 通道处理器 / channel handler
     */
    public void setChannelHandler(ClientChannelHandler channelHandler) {
        this.channelHandler = channelHandler;
    }

    /**
     * 设置发送标识字节。
     * Sets the sender identifier bytes.
     *
     * identifier bytes
     */
    public void setIdentifier(byte[] identifier) {
        this.identifier = identifier;
    }

    /**
     * 校验消息发送间隔是否满足配置延迟。
     * Verifies whether message send interval satisfies the configured delay.
     *
     * @return 允许发送则为 true / true if sending is allowed
     */
    public boolean verifyLastMessage() {
        if (Config.MESSAGE_DELAY == 0) {
            return true;
        }

        if (this.lastMessage == 0) {
            this.lastMessage = System.currentTimeMillis();
            return true;
        } else {
            long diff = System.currentTimeMillis() - this.lastMessage;
            if (Config.MESSAGE_DELAY * 1000 > diff) {
                log.warn(I18n.get("log.c2c94e02b03f", this.getClientId(), diff));
                return false;
            } else {
                this.lastMessage = System.currentTimeMillis();
                return true;
            }
        }
    }

    /**
     * 判断当前是否处于禁言状态。
     * Checks whether the client is currently gagged.
     *
     * @return 禁言中则为 true / true if gagged
     */
	public boolean isGagged() {
		if(this.gagTime == 0)
			return false;
		if(System.currentTimeMillis() > this.gagTime)
			return false;
		return true;
	}

    /**
     * 设置禁言结束时间戳。
     * Sets the gag end timestamp.
     *
     * @param gagTime 结束时间戳（毫秒） / end timestamp in millis
     */
    public void setGagTime(long gagTime) {
        this.gagTime = gagTime;
    }

    /**
     * 获取禁言结束时间戳。
     * Returns the gag end timestamp.
     *
     * @return 结束时间戳（毫秒） / end timestamp in millis
     */
    public long getGagTime() {
        return this.gagTime;
    }

    /**
     * 比较昵称是否与会话一致，并接受私有区字形开头的管理员标签。
     * Compares the nickname with the session name and accepts admin tags starting with a private-use glyph.
     *
     * @param nick 待比较昵称 / nickname to compare
     * comparison result
     */
    public boolean same(String nick) {
        boolean matches = this.realName.equals(nick)
                || nick != null && !nick.isEmpty() && Character.getType(nick.charAt(0)) == Character.PRIVATE_USE
                && nick.endsWith(" " + this.realName);
        if (!matches) {
            log.warn(I18n.get("log.46e58c597440", nick, this.realName));
        }
        return matches;
    }
}
