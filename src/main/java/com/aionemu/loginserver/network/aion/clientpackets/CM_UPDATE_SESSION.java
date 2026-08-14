package com.aionemu.loginserver.network.aion.clientpackets;

import com.aionemu.loginserver.controller.AccountController;
import com.aionemu.loginserver.network.aion.AionClientPacket;
import com.aionemu.loginserver.network.aion.LoginConnection;
import java.nio.ByteBuffer;

/**
 * 客户端从游戏服重连登录服时的会话恢复包。
 * Client packet to restore session when reconnecting from game server to login server.
 *
 * @author -Nemesiss-
 */
public class CM_UPDATE_SESSION extends AionClientPacket {

    /**
     * 会话密钥中的 accountId，用于安全校验。
     * accountId part of session key for security checks.
     */
    private int accountId;
    /**
     * 会话密钥中的 loginOk，用于安全校验。
     * loginOk part of session key for security checks.
     */
    private int loginOk;
    /**
     * 服务端下发的快速重连密钥，需校验有效性。
     * Reconnect key previously sent by server for fast re-login.
     */
    private int reconnectKey;

    /**
     * 构造 CM_UPDATE_SESSION 包。
     * Construct CM_UPDATE_SESSION packet.
     *
     * @param buf 包体数据 / Packet data
     * @param client 登录连接 / Login connection
     */
    public CM_UPDATE_SESSION(ByteBuffer buf, LoginConnection client) {
        super(buf, client, 0x08);
    }

    /**
     * 读取 accountId、loginOk 与 reconnectKey。
     * Read accountId, loginOk and reconnectKey.
     */
    @Override
    protected void readImpl() {
        accountId = readD();
        loginOk = readD();
        reconnectKey = readD();
    }

    /**
     * 委托账号控制器完成重连鉴权。
     * Delegate reconnect authentication to account controller.
     */
    @Override
    protected void runImpl() {
        AccountController.authReconnectingAccount(accountId, loginOk, reconnectKey, getConnection());
    }
}
