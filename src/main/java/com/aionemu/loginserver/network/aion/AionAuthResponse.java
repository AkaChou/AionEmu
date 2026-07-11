package com.aionemu.loginserver.network.aion;

import lombok.Getter;

/**
 * 登录服可能返回给客户端的鉴权/登录失败等响应码。
 * Auth and login failure response codes the login server may send to the client.
 *
 * @author KID
 */
public enum AionAuthResponse {

    /**
     * 仅内部使用，不发给客户端：鉴权成功。
     * Internal only, not sent to client: authentication succeeded.
     */
    AUTHED(0),
    /**
     * 系统错误。
     * System error.
     */
    SYSTEM_ERROR(1),
    /**
     * 账号或密码不匹配。
     * ID or password does not match.
     */
    INVALID_PASSWORD(2),
    /**
     * 账号或密码不匹配。
     * ID or password does not match.
     */
    INVALID_PASSWORD2(3),
    /**
     * 加载账号信息失败。
     * Failed to load account info.
     */
    FAILED_ACCOUNT_INFO(4),
    /**
     * 加载社保号失败。
     * Failed to load social security number.
     */
    FAILED_SOCIAL_NUMBER(5),
    /**
     * 鉴权服未注册任何游戏服。
     * No game server is registered to the authorization server.
     */
    NO_GS_REGISTERED(6),
    /**
     * 已在线。
     * Already logged in.
     */
    ALREADY_LOGGED_IN(7),
    /**
     * 所选服务器已关闭。
     * The selected server is down.
     */
    SERVER_DOWN(8),
    /**
     * 登录信息与提供的信息不匹配。
     * Login information does not match the provided data.
     */
    INVALID_PASSWORD3(9),
    /**
     * 无可用登录信息 / 账号不存在。
     * no such account. / no such account.
     */
    NO_SUCH_ACCOUNT(10),
    /**
     * 已与服务器断开，请稍后重连。
     * Disconnected from the server; try again later.
     */
    DISCONNECTED(11),
    /**
     * 年龄不足，无法进入游戏。
     * Not old enough to play.
     */
    AGE_LIMIT(12),
    /**
     * 检测到重复登录。
     * Double login attempts detected.
     */
    ALREADY_LOGGED_IN2(13),
    /**
     * 已在线。
     * Already logged in.
     */
    ALREADY_LOGGED_IN3(14),
    /**
     * 服务器人数已满。
     * Server is full.
     */
    SERVER_FULL(15),
    /**
     * 服务器维护中 / 仅 GM 可进。
     * GM only. / GM only.
     */
    GM_ONLY(16),
    /**
     * 请修改密码后再登录。
     * Please change password before logging in.
     */
    ERROR_17(17),
    /**
     * 已用完允许的游戏时间。
     * Allowed play time used up.
     */
    TIME_EXPIRED(18),
    /**
     * 账号分配时间已耗尽。
     * Allocated account time exhausted.
     */
    TIME_EXPIRED2(19),
    /**
     * 系统错误。
     * System error.
     */
    SYSTEM_ERROR2(20),
    /**
     * IP 已被占用。
     * IP already in use.
     */
    ALREADY_USED_IP(21),
    /**
     * 该 IP 禁止进入游戏。
     * Cannot access the game from this IP.
     */
    BAN_IP(22);

    /**
     * 可发送给客户端的消息 ID。
     * Message id that may be sent to the client.
     */
    @Getter
    private final int messageId;

    /**
     * 构造响应枚举。
     * Construct response enum.
     *
     * Message id
     */
    AionAuthResponse(int msgId) {
        messageId = msgId;
    }
}
