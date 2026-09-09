package com.aionemu.loginserver.model;

/**
 * 快速重连账号：保存从游戏服返回登录服时的账号与 reconnectionKey。
 * Reconnecting account: holds Account and reconnectionKey for fast reconnect from GameServer.
 *
 * @param account         即将重连的账号。
 *                        Account that will reconnect.
 * @param reconnectionKey 用于认证的重连密钥。
 *                        Reconnection key used for authentication.
 * @author -Nemesiss-
 */
public record ReconnectingAccount(Account account, int reconnectionKey) {

}
