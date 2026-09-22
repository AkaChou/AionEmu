package com.aionemu.loginserver.model;

/**
 * 快速重连账号：保存从游戏服返回登录服时的账号与 reconnectionKey。
 * Reconnecting account: holds Account and reconnectionKey for fast reconnect from GameServer.
 *                        Account that will reconnect.
 *                        Reconnection key used for authentication.
 * @author -Nemesiss-
 */
public record ReconnectingAccount(Account account, int reconnectionKey) {

}
