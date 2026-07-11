package com.aionemu.loginserver.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 快速重连账号：保存从游戏服返回登录服时的账号与 reconnectionKey。
 * Reconnecting account: holds Account and reconnectionKey for fast reconnect from GameServer.
 *
 * @author -Nemesiss-
 */
@Getter
@RequiredArgsConstructor
public class ReconnectingAccount {

    /**
     * 即将重连的账号。
     * Account that will reconnect.
     */
    private final Account account;

    /**
     * 用于认证的重连密钥。
     * Reconnection key used for authentication.
     */
    private final int reconnectionKey;
}
