package com.aionemu.chatserver.network.gameserver;

import lombok.Getter;

/**
 * 聊天服对游戏服认证结果的应答枚举。
 * Authentication response codes that the chat server may return to a game server.
 *
 * @author -Nemesiss-
 */
public enum GsAuthResponse {

    /**
     * 认证成功。
     * Authentication succeeded.
     */
    AUTHED(0),

    /**
     * 密码、IP 等校验不匹配。
     * Password, IP, or other credentials do not match.
     */
    NOT_AUTHED(1),

    /**
     * 请求的游戏服 ID 已被占用。
     * Requested game server id is already registered.
     */
    ALREADY_REGISTERED(2);

    /**
     * 可下发给客户端的应答 ID。
     * Response id that may be sent to the client.
     */
    @Getter
    private final byte responseId;

    /**
     * 构造应答枚举值。
     * Constructs a response enum constant.
     *
     * @param responseId 应答消息 ID / response message id
     */
    private GsAuthResponse(int responseId) {
        this.responseId = (byte) responseId;
    }
}
