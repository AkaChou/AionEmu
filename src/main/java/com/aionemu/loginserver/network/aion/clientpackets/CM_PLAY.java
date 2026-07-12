package com.aionemu.loginserver.network.aion.clientpackets;

import com.aionemu.loginserver.GameServerInfo;
import com.aionemu.loginserver.GameServerTable;
import com.aionemu.loginserver.network.aion.AionAuthResponse;
import com.aionemu.loginserver.network.aion.AionClientPacket;
import com.aionemu.loginserver.network.aion.LoginConnection;
import com.aionemu.loginserver.network.aion.SessionKey;
import com.aionemu.loginserver.network.aion.serverpackets.SM_LOGIN_FAIL;
import com.aionemu.loginserver.network.aion.serverpackets.SM_PLAY_FAIL;
import com.aionemu.loginserver.network.aion.serverpackets.SM_PLAY_OK;
import java.nio.ByteBuffer;

/**
 * 客户端选服进入游戏请求：校验会话并回 SM_PLAY_OK / FAIL。
 * FAIL.
 *
 * @author -Nemesiss-
 */
public class CM_PLAY extends AionClientPacket {

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
     * 目标游戏服 ID。
     * Target game server id.
     */
    private byte servId;

    /**
     * 构造 CM_PLAY 包。
     * Construct CM_PLAY packet.
     *
     * @param buf 包体数据 / Packet data
     * Login connection
     */
    public CM_PLAY(ByteBuffer buf, LoginConnection client) {
        super(buf, client, 0x02);
    }

    /**
     * 读取 accountId、loginOk 与服务器 ID。
     * Read accountId, loginOk and server id.
     */
    @Override
    protected void readImpl() {
        accountId = readD();
        loginOk = readD();
        servId = (byte) readC();
    }

    /**
     * 校验会话；按服务器在线/满员状态回包。
     * Validate session; reply by server online/full status.
     */
    @Override
    protected void runImpl() {
        LoginConnection con = getConnection();
        SessionKey key = con.getSessionKey();
        if (key.checkLogin(accountId, loginOk)) {
            GameServerInfo gsi = GameServerTable.getGameServerInfo(servId);
            if (gsi == null || !gsi.isOnline()) {
                con.sendPacket(new SM_PLAY_FAIL(AionAuthResponse.SERVER_DOWN));
            } // else if(serv gm only)
            // con.sendPacket(new SM_PLAY_FAIL(AionAuthResponse.GM_ONLY));
            else if (gsi.isFull()) {
                con.sendPacket(new SM_PLAY_FAIL(AionAuthResponse.SERVER_FULL));
            } else {
                con.setJoinedGs();
                sendPacket(new SM_PLAY_OK(key, servId));
            }
        } else {
            con.close(new SM_LOGIN_FAIL(AionAuthResponse.SYSTEM_ERROR), false);
        }
    }
}
