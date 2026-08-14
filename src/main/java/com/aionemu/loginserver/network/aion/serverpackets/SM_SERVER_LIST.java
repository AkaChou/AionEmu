package com.aionemu.loginserver.network.aion.serverpackets;

import java.util.Collection;
import java.util.Map;

import com.aionemu.loginserver.GameServerInfo;
import com.aionemu.loginserver.GameServerTable;
import com.aionemu.loginserver.controller.AccountController;
import com.aionemu.loginserver.network.aion.AionServerPacket;
import com.aionemu.loginserver.network.aion.LoginConnection;

/**
 * 登录服→客户端：下发可用游戏服列表及各服角色数。
 * LoginServer → client: available game-server list with per-server character counts.
 *
 * @author -Nemesiss-
 * @modified cura
 */
public class SM_SERVER_LIST extends AionServerPacket {

    public SM_SERVER_LIST() {
        super(0x04);
    }

    @Override
    protected void writeImpl(LoginConnection con) {
        Collection<GameServerInfo> servers = GameServerTable.getGameServers();
        Map<Integer, Integer> charactersCountOnServer = null;

        int accountId = con.getAccount().getId();
        //int accessLevel = con.getAccount().getAccessLevel();
        int maxId = 0;

        charactersCountOnServer = AccountController.getGSCharacterCountsFor(accountId);

        writeC(servers.size());// 服务器数量 / servers
        writeC(con.getAccount().getLastServer());// 上次登录服务器 / last server
        for (GameServerInfo gsi : servers) {
            if (gsi.getId() > maxId) {
                maxId = gsi.getId();
            }

            writeC(gsi.getId());// 服务器 ID / server id
            writeB(gsi.getIPAddressForPlayer(con.getIP())); // 服务器 IP / server IP
            writeD(gsi.getPort());// 端口 / port
            writeC(0x00); // 年龄限制 / age limit
            writeC(0x01);// PvP 标志 / pvp=1
            writeH(gsi.getCurrentPlayers());// 当前玩家数 / current players
            writeH(gsi.getMaxPlayers());// 最大玩家数 / max players
            writeC(gsi.isOnline() ? 1 : 0);// 服务器状态，在线=1 / server status, up=1
            writeD(1);// 位标志 / bits
            writeC(1);// 括号标志 / server.brackets ? 0x01 : 0x00
        }

        writeH(maxId + 1);
        writeC(0x01);

        for (int i = 1; i <= maxId; i++) {
            if (charactersCountOnServer.containsKey(i)) {
                writeC(charactersCountOnServer.get(i));
            } else {
                writeC(0);
            }
        }
    }
}
