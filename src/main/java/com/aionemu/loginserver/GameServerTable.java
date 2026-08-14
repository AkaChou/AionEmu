package com.aionemu.loginserver;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.network.IPRange;
import com.aionemu.commons.utils.NetworkUtils;
import com.aionemu.loginserver.dao.GameServersDAO;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.network.gameserver.GsAuthResponse;
import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.serverpackets.SM_REQUEST_KICK_ACCOUNT;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录服已注册游戏服表（在线/离线均包含）。
 * Table of GameServers registered on this LoginServer (online or down).
 *
 * @author -Nemesiss-
 */
@Slf4j
@UtilityClass
public class GameServerTable {

    /**
     * 游戏服映射 Map&lt;Id, GameServerInfo&gt;。
     * GameServer map Map&lt;Id, GameServerInfo&gt;.
     */
    private Map<Byte, GameServerInfo> gameservers;

    /**
     * 返回全部已注册游戏服的不可变集合。
     * Return an unmodifiable collection of all registered GameServers.
     *
     * @return 游戏服集合 / GameServer collection
     */
    public Collection<GameServerInfo> getGameServers() {
        return Collections.unmodifiableCollection(gameservers.values());
    }

    /**
     * 从数据库加载游戏服列表。
     * Load GameServers from database.
     */
    public void load() {
        gameservers = getDAO().getAllGameServers();
        log.info(I18n.get("log.f7d64efdf7a8", gameservers.size()));
    }

    /**
     * 在允许时注册游戏服连接。
     * Register a GameServer connection when allowed.
     *
     * @param gsConnection 游戏服连接 / GameServer connection
     * @param requestedId 请求的服务器 ID / Requested server id
     * @param defaultAddress 默认网络地址（通常公网） / Default network address (usually public)
     * @param ipRanges IP 段映射（通常局域网） / IP range mappings (usually LAN)
     * @param port 客户端端口 / Client port
     * @param maxPlayers 最大在线人数 / Max players
     * @param password 配置中的服务器密码 / Server password from config
     * @return 认证响应 / Auth response
     */
    public GsAuthResponse registerGameServer(GsConnection gsConnection, byte requestedId, byte[] defaultAddress,
            List<IPRange> ipRanges, int port, int maxPlayers, String password) {
        GameServerInfo gsi = gameservers.get(requestedId);

        if (gsi == null) {
            log.info(I18n.get("log.af1c246e807f", gsConnection, requestedId));
            return GsAuthResponse.NOT_AUTHED;
        }

        synchronized (gsi) {
            if (gsi.getConnection() != null) {
                return GsAuthResponse.ALREADY_REGISTERED;
            }

            if (!gsi.getPassword().equals(password) || !NetworkUtils.checkIPMatching(gsi.getIp(), gsConnection.getIP())) {
                log.info(I18n.get("log.b245f5814993", gsi.getPassword(), password));
                log.info(I18n.get("log.ef51b73f3147", gsConnection));
                return GsAuthResponse.NOT_AUTHED;
            }

            gsi.setDefaultAddress(defaultAddress);
            gsi.setIpRanges(ipRanges);
            gsi.setPort(port);
            gsi.setMaxPlayers(maxPlayers);
            gsi.setConnection(gsConnection);

            gsConnection.setGameServerInfo(gsi);
            return GsAuthResponse.AUTHED;
        }
    }

    /**
     * 按 ID 获取游戏服信息。
     * Get GameServerInfo by gameserver id.
     *
     * @param gameServerId 游戏服 ID / GameServer id
     * @return 游戏服信息 / GameServer info
     */
    public GameServerInfo getGameServerInfo(byte gameServerId) {
        return gameservers.get(gameServerId);
    }

    /**
     * 检查账号是否已在任意游戏服在线。
     * Check whether the account is already in use on any GameServer.
     *
     * @param acc 待检查账号 / Account to check
     * @return 已在任意游戏服登录则为 true / true if logged in on any GameServer
     */
    public boolean isAccountOnAnyGameServer(Account acc) {
        for (GameServerInfo gsi : getGameServers()) {
            if (gsi.isAccountOnGameServer(acc.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 若账号在任意游戏服在线则踢下线。
     * Kick account from any GameServer if it is logged in.
     *
     * @param account 待踢账号 / Account to kick
     */
    public void kickAccountFromGameServer(Account account) {
        for (GameServerInfo gsi : getGameServers()) {
            if (gsi.isAccountOnGameServer(account.getId())) {
                gsi.getConnection().sendPacket(new SM_REQUEST_KICK_ACCOUNT(account.getId()));
                break;
            }
        }
    }

    /**
     * 获取 {@link GameServersDAO} 快捷方法。
     * Shortcut for {@link GameServersDAO}.
     *
     * @return DAO 实例 / DAO instance
     */
    private GameServersDAO getDAO() {
        return DAOManager.getDAO(GameServersDAO.class);
    }

    /**
     * 向指定游戏服转发 pong。
     * Forward pong to the given GameServer.
     *
     * @param serverId 游戏服 ID / GameServer id
     * @param pid 进程 ID / Process id
     */
    public void pong(byte serverId, int pid) {
        for (GameServerInfo gsi : getGameServers()) {
            if (gsi.getId() == serverId) {
                gsi.getConnection().pong(pid);
                break;
            }
        }
    }
}
