package com.aionemu.loginserver.network.gameserver;

import lombok.Getter;

/**
 * 登录服对游戏服认证请求的应答枚举（失败原因、成功等）。
 * Possible LoginServer responses to GameServer authentication (failure reasons, success, etc.).
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
     * 密码/IP 等不匹配。
     * Password/IP or related credentials do not match.
     */
    NOT_AUTHED(1),
    /**
     * 请求的服务器 ID 已被占用。
     * Requested server id is already registered.
     */
    ALREADY_REGISTERED(2);

    /**
     * 可下发给客户端的应答 ID。
     * Response id that may be sent to the client.
     */
    @Getter
    private final byte responseId;

    /**
     * 构造应答枚举项。
     * Construct response enum constant.
     *
     * Response message id
     */
    GsAuthResponse(int responseId) {
        this.responseId = (byte) responseId;
    }
}
