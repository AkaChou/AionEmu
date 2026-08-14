package com.aionemu.loginserver.network.aion.clientpackets;

import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.controller.AccountController;
import com.aionemu.loginserver.network.aion.AionAuthResponse;
import com.aionemu.loginserver.network.aion.AionClientPacket;
import com.aionemu.loginserver.network.aion.LoginConnection;
import com.aionemu.loginserver.network.aion.LoginConnection.State;
import com.aionemu.loginserver.network.aion.SessionKey;
import com.aionemu.loginserver.network.aion.serverpackets.SM_LOGIN_FAIL;
import com.aionemu.loginserver.network.aion.serverpackets.SM_LOGIN_OK;
import com.aionemu.loginserver.network.sts.StsVipServer;
import com.aionemu.loginserver.service.LoginProtectionServices;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import javax.crypto.Cipher;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端登录包：RSA 解密账号密码并完成鉴权。
 * Client login packet: RSA-decrypt credentials and authenticate.
 *
 * @author -Nemesiss-, KID, Lyahim
 */
@Slf4j
public class CM_LOGIN extends AionClientPacket {

    /**
     * 加密的登录名与密码字节数组。
     * Encrypted login and password bytes.
     */
    private byte[] data;

    /**
     * 构造 CM_LOGIN 包。
     * Construct CM_LOGIN packet.
     *
     * @param buf 包体数据 / Packet data
     * @param client 登录连接 / Login connection
     */
    public CM_LOGIN(ByteBuffer buf, LoginConnection client) {
        super(buf, client, 0x0b);
    }

    /**
     * 读取 128 字节加密凭证。
     * Read 128-byte encrypted credentials.
     */
    @Override
    protected void readImpl() {
    	readD();
    	if (getRemainingBytes() >= 128) {
      		data = readB(128);
    	}
    }

    /**
     * 解密凭证、调用账号控制器，并按结果回包或封禁。
     * Decrypt credentials, call account controller, reply or ban by result.
     */
    @Override
    protected void runImpl() {
        if (data == null) {
            return;
        }

        byte[] decrypted;
        try {
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
            rsaCipher.init(Cipher.DECRYPT_MODE, getConnection().getRSAPrivateKey());
            decrypted = rsaCipher.doFinal(data, 0, 128);
        } catch (GeneralSecurityException e) {
            sendPacket(new SM_LOGIN_FAIL(AionAuthResponse.SYSTEM_ERROR));
            return;
        }
        String user = new String(decrypted, 64, 32).trim().toLowerCase();
        String password = new String(decrypted, 96, 32).trim();

        @SuppressWarnings("unused")
        int ncotp = decrypted[0x7c];
        ncotp |= decrypted[0x7d] << 8;
        ncotp |= decrypted[0x7e] << 16;
        ncotp |= decrypted[0x7f] << 24;

        LoginConnection client = getConnection();
        AionAuthResponse response = AccountController.login(user, password, client);
        switch (response) {
            case AUTHED:
                StsVipServer.rememberAuthenticatedAccount(client.getIP(), client.getAccount().getId());
                client.setState(State.AUTHED_LOGIN);
                client.setSessionKey(new SessionKey(client.getAccount()));
                client.sendPacket(new SM_LOGIN_OK(client.getSessionKey()));
                log.debug("" + user + " got authed state");
                break;
            case INVALID_PASSWORD:
                if (Config.ENABLE_BRUTEFORCE_PROTECTION) {
                    String ip = client.getIP();
                    if (LoginProtectionServices.bruteForceProtector().addFailedConnect(ip)) {
                        Timestamp newTime = new Timestamp(System.currentTimeMillis() + Config.WRONG_LOGIN_BAN_TIME * 60000);
                        LoginProtectionServices.bannedIpService().banIp(ip, newTime);
                        log.debug(user + " on " + ip + " banned for " + Config.WRONG_LOGIN_BAN_TIME + " min. bruteforce");
                        client.close(new SM_LOGIN_FAIL(AionAuthResponse.BAN_IP), false);
                    } else {
                        log.debug(user + " got invalid password attemp state");
                        client.sendPacket(new SM_LOGIN_FAIL(response));
                    }
                } else {
                    log.debug(user + " got invalid password attemp state");
                    client.sendPacket(new SM_LOGIN_FAIL(response));
                }
                break;
            default:
                log.debug(user + " got unknown (" + response.toString() + ") attemp state");
                client.close(new SM_LOGIN_FAIL(response), false);
                break;
        }
    }
}
