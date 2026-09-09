package com.aionemu.chatserver.model;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

import com.aionemu.chatserver.configs.Config;
import com.aionemu.chatserver.model.channel.Channel;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;

/**
 * 聊天客户端会话模型，绑定玩家标识、令牌、频道与禁言状态。
 * Chat client session model binding player identity, token, channels and gag state.
 *
 * @author ATracer
 */
@Slf4j
@RequiredArgsConstructor
public class ChatClient {

    /**
     * 聊天客户端 ID（玩家 ID）。
     * Chat client id (player id).
     */
    @Getter
    private final int clientId;
    /**
     * 发送消息时使用的标识字节。
     * Identifier bytes used when sending messages.
     */
    @Getter
    @Setter
    private byte[] identifier;
    /**
     * 与游戏服鉴权时使用的令牌。
     * Token used during auth with the game server.
     */
    @Getter
    private final byte[] token;
    /**
     * 客户端网络通道处理器。
     * Network channel handler of this chat client.
     */
    @Getter
    @Setter
    private ClientChannelHandler channelHandler;
    /**
     * 已加入频道映射；同类型频道仅可存在一个。
     * Joined channels map; only one channel per type is allowed.
     */
    private final Map<ChannelType, Channel> channelsList = new ConcurrentHashMap<>();
    /**
     * 最近一次请求并广播消息的时间戳。
     * Timestamp of the last requested and broadcasted message.
     */
    private long lastMessage;
    /**
     * 玩家真实昵称。
     * Player real nickname.
     */
    @Getter
    private final String realName;
    /**
     * 禁言结束时间戳（毫秒）；0 表示未禁言。
     * Gag end timestamp in millis; 0 means not gagged.
     */
    @Getter
    @Setter
    private long gagTime;

    /**
     * 加入指定频道（按频道类型覆盖）。
     * Joins the given channel (overwrites by channel type).
     *
     * @param channel 目标频道 / target channel
     */
    public void addChannel(Channel channel) {
        channelsList.put(channel.getChannelType(), channel);
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
            if (Config.MESSAGE_DELAY * 1000L > diff) {
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
		return System.currentTimeMillis() <= this.gagTime;
	}

    /**
     * 比较昵称是否与会话一致，并接受私有区字形开头的管理员标签。
     * Compares the nickname with the session name and accepts admin tags starting with a private-use glyph.
     *
     * @param nick 待比较昵称 / nickname to compare
     * @return 匹配返回 true / true if matching
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
