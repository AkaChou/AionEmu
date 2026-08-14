package com.aionemu.loginserver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.commons.network.IPRange;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsConnection.State;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 登录服侧的游戏服信息（id、IP、在线账号等）。
 * GameServer representation on LoginServer side (id, IP, online accounts, etc.).
 *
 * @author -Nemesiss-
 */
@RequiredArgsConstructor
public class GameServerInfo {

    /**
     * 游戏服 ID。
     * GameServer id.
     */
    @Getter
    private final byte id;
    /**
     * 允许接入的 IP；其他 IP 无法注册。
     * Allowed IP; other IPs cannot register.
     */
    @Getter
    private final String ip;
    /**
     * 游戏服密码。
     * GameServer password.
     */
    @Getter
    private final String password;
    /**
     * 默认地址，通常为公网地址。
     * Default address, usually the public internet address.
     */
    @Getter
    @Setter
    private volatile byte[] defaultAddress;
    /**
     * IP 段映射，多用于局域网接入。
     * IP range mappings, often used for LAN access.
     */
    @Getter
    @Setter
    private volatile List<IPRange> ipRanges;
    /**
     * 客户端接入端口。
     * Client accept port.
     */
    @Getter
    @Setter
    private volatile int port;
    /**
     * 与登录服的连接；下线时为 null。
     * Connection to LoginServer; null when offline.
     */
    @Getter
    @Setter
    private volatile GsConnection connection;
    /**
     * 最大在线人数。
     * Max allowed players.
     */
    @Getter
    @Setter
    private volatile int maxPlayers;
    /**
     * 本服在线账号 Map&lt;账号 ID, Account&gt;。
     * Online accounts Map&lt;accountId, Account&gt;.
     */
    private final Map<Integer, Account> accountsOnGameServer = new ConcurrentHashMap<>();

    /**
     * 判断游戏服是否在线。
     * Check whether this GameServer is online.
     *
     * @return 已认证连接存在则为 true / true if an authenticated connection exists
     */
    public final boolean isOnline() {
        return connection != null && connection.getState() == State.AUTHED;
    }

    /**
     * 判断账号是否已在本游戏服。
     * Check whether the account is already on this GameServer.
     *
     * @param accountId 账号 ID / Account id
     * @return 存在则为 true / true if present
     */
    public final boolean isAccountOnGameServer(int accountId) {
        return accountsOnGameServer.containsKey(accountId);
    }

    /**
     * 从本游戏服移除账号。
     * Remove account from this GameServer.
     *
     * @param accountId 账号 ID / Account id
     * @return 被移除的账号 / Removed account
     */
    public final Account removeAccountFromGameServer(int accountId) {
        return accountsOnGameServer.remove(accountId);
    }

    /**
     * 将账号加入本游戏服。
     * Add account to this GameServer.
     *
     * @param acc 账号 / Account
     */
    public final void addAccountToGameServer(Account acc) {
        accountsOnGameServer.put(acc.getId(), acc);
    }

    /**
     * 按账号 ID 取本服账号对象。
     * Get account object by id on this GameServer.
     *
     * @param accountId 账号 ID / Account id
     * @return 账号对象；不存在则为 null / Account or null
     */
    public final Account getAccountFromGameServer(int accountId) {
        return accountsOnGameServer.get(accountId);
    }

    /**
     * 清空本服全部在线账号。
     * Clear all accounts on this GameServer.
     */
    public void clearAccountsOnGameServer() {
        accountsOnGameServer.clear();
    }

    /**
     * 当前在线人数。
     * Current online player count.
     *
     * @return 在线人数 / Online count
     */
    public int getCurrentPlayers() {
        return accountsOnGameServer.size();
    }

    /**
     * 是否已满员。
     * Whether the server is full.
     *
     * @return 已满员时为 {@code true} / {@code true} if full
     */
    public boolean isFull() {
        return getCurrentPlayers() >= getMaxPlayers();
    }

    /**
     * 按玩家 IP 选择应下发的游戏服地址。
     * Resolve the GameServer IP address valid for the given player IP.
     * <p>
     * 不同子网可能需要不同地址；离线时返回 127.0.0.1。
     * Different subnets may need different addresses; returns 127.0.0.1 when offline.
     *
     * @param playerIp 玩家 IP / Player IP
     * @return 对该玩家有效的地址字节 / Address bytes valid for the player
     */
    public byte[] getIPAddressForPlayer(String playerIp) {
        if (!isOnline()) {
            return new byte[]{127, 0, 0, 1};
        }

        for (IPRange ipr : ipRanges) {
            if (ipr.isInRange(playerIp)) {
                return ipr.getAddress();
            }
        }

        return defaultAddress;
    }
}
