package com.aionemu.loginserver.network.aion.clientpackets;

import com.aionemu.loginserver.GameServerTable;
import com.aionemu.loginserver.controller.AccountController;
import com.aionemu.loginserver.network.aion.AionAuthResponse;
import com.aionemu.loginserver.network.aion.AionClientPacket;
import com.aionemu.loginserver.network.aion.LoginConnection;
import com.aionemu.loginserver.network.aion.serverpackets.SM_LOGIN_FAIL;
import java.nio.ByteBuffer;

/**
 * 客户端请求服务器列表：校验会话后加载角色数并下发列表。
 * Client server-list request: validate session then load character counts and list.
 *
 * @author -Nemesiss-
 */
public class CM_SERVER_LIST extends AionClientPacket {

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
     * 构造 CM_SERVER_LIST 包。
     * Construct CM_SERVER_LIST packet.
     *
     * @param buf 包体数据 / Packet data
     * Login connection
     */
    public CM_SERVER_LIST(ByteBuffer buf, LoginConnection client) {
        super(buf, client, 0x05);
    }

    /**
     * 读取 accountId、loginOk 及预留字段。
     * Read accountId, loginOk and reserved field.
     */
    @Override
    protected void readImpl() {
        accountId = readD();
        loginOk = readD();
    	readD();
    }

    /**
     * 校验会话；无 GS 则失败，否则加载角色计数。
     * Validate session; fail if no GS, otherwise load character counts.
     */
    @Override
    protected void runImpl() {
        LoginConnection con = getConnection();
        if (con.getSessionKey().checkLogin(accountId, loginOk)) {
            if (GameServerTable.getGameServers().size() == 0) {
                con.close(new SM_LOGIN_FAIL(AionAuthResponse.NO_GS_REGISTERED), false);
            } else {
                AccountController.loadGSCharactersCount(accountId);
            }
        } else {
            /**
             * 会话密钥不匹配：通知客户端并断开。
             * Session key mismatch: notify client and disconnect.
             */
            con.close(new SM_LOGIN_FAIL(AionAuthResponse.SYSTEM_ERROR), false);
        }
    }
}
