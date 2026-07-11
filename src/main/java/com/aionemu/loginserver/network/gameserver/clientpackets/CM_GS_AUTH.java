package com.aionemu.loginserver.network.gameserver.clientpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

import com.aionemu.commons.network.IPRange;
import com.aionemu.loginserver.GameServerTable;
import com.aionemu.loginserver.network.gameserver.GsAuthResponse;
import com.aionemu.loginserver.network.gameserver.GsClientPacket;
import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsConnection.State;
import com.aionemu.loginserver.network.gameserver.serverpackets.SM_GS_AUTH_RESPONSE;
import com.aionemu.loginserver.network.gameserver.serverpackets.SM_MACBAN_LIST;
import com.aionemu.loginserver.service.LoginThreadPoolServices;

/**
 * GS→LS：游戏服向登录服注册并鉴权。
 * GS→LS: GameServer authentication/registration packet to LoginServer.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class CM_GS_AUTH extends GsClientPacket {

    /**
     * 鉴权密码。
     * Password for authentication.
     */
    private String password;
    /**
     * 游戏服 ID。
     * Id of GameServer.
     */
    private byte gameServerId;
    /**
     * 最大在线人数。
     * Maximum number of players this GameServer can accept.
     */
    private int maxPlayers;
    /**
     * 游戏服端口。
     * Port of this GameServer.
     */
    private int port;
    /**
     * 默认对外地址。
     * Default address for server.
     */
    private byte[] defaultAddress;
    /**
     * 本游戏服 IP 段列表。
     * List of IPRanges for this GameServer.
     */
    private List<IPRange> ipRanges;

    /**
     * 读取 GS ID、默认地址、IP 段、端口、人数上限与密码。
     * Reads GS id, default address, IP ranges, port, max players, and password.
     */
    @Override
    protected void readImpl() {
        gameServerId = (byte) readC();

        byte len1 = (byte) readC();
        defaultAddress = readB(len1);
        int size = readD();
        ipRanges = new ArrayList<IPRange>(size);
        for (int i = 0; i < size; i++) {
            byte[] min = readB(readC());
            byte[] max = readB(readC());
            byte[] address = readB(readC());
            try {
                ipRanges.add(new IPRange(min, max, address));
            } catch (IllegalArgumentException e) {
                log.warn(I18n.get("log.7d4955b67564", i, e.getMessage()));
            }
        }

        port = readH();
        maxPlayers = readD();
        password = readS();
    }

    /**
     * 注册游戏服；成功则进入 AUTHED 并下发 MAC 封禁列表，否则关闭连接。
     * Registers GameServer; on success enters AUTHED and sends MAC ban list, otherwise closes connection.
     */
    @Override
    protected void runImpl() {
        final GsConnection client = this.getConnection();

        GsAuthResponse resp = GameServerTable.registerGameServer(client, gameServerId, defaultAddress, ipRanges, port, maxPlayers, password);
        switch (resp) {
            case AUTHED:
                log.info(I18n.get("log.8317d73f1707", gameServerId));
                client.setState(State.AUTHED);
                client.sendPacket(new SM_GS_AUTH_RESPONSE(resp));
                LoginThreadPoolServices.threadPoolManager().schedule(new Runnable() {
                    @Override
                    public void run() {
                        client.sendPacket(new SM_MACBAN_LIST());
                    }
                }, 500);
                break;

            default:
                client.close(new SM_GS_AUTH_RESPONSE(resp), false);
        }
    }
}
