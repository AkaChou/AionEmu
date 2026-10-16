package com.aionemu.loginserver.controller;

import com.aionemu.boot.i18n.I18n;

import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.commons.utils.NetworkUtils;
import com.aionemu.commons.utils.PasswordHash;
import com.aionemu.loginserver.GameServerInfo;
import com.aionemu.loginserver.GameServerTable;
import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.configs.VipConfig;
import com.aionemu.loginserver.persistence.dao.AccountDataDao;
import com.aionemu.loginserver.persistence.dao.AccountRewardDao;
import com.aionemu.loginserver.persistence.dao.AccountTimeDao;
import com.aionemu.loginserver.persistence.dao.AccountVipDao;

import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.model.ReconnectingAccount;
import com.aionemu.loginserver.model.Vip;
import com.aionemu.loginserver.network.aion.AionAuthResponse;
import com.aionemu.loginserver.network.aion.LoginConnection;
import com.aionemu.loginserver.network.aion.LoginConnection.State;
import com.aionemu.loginserver.network.aion.SessionKey;
import com.aionemu.loginserver.network.aion.serverpackets.SM_SERVER_LIST;
import com.aionemu.loginserver.network.aion.serverpackets.SM_UPDATE_SESSION;
import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.serverpackets.SM_ACCOUNT_AUTH_RESPONSE;
import com.aionemu.loginserver.network.gameserver.serverpackets.SM_GS_CHARACTER_RESPONSE;
import com.aionemu.loginserver.network.gameserver.serverpackets.SM_REQUEST_KICK_ACCOUNT;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 账号动作总控：登录、重连、踢人及 GS 角色数统计。
 * Controls all account actions: login, reconnect, kick and GS character counts.
 * @author KID
 * @author SoulKeeper
 */
@Slf4j
@Service
@Lazy
@RequiredArgsConstructor
public class AccountController {

    private final AccountDataDao accountDao;
    private final AccountTimeDao accountTimeDao;
    private final AccountVipDao accountVipDao;
    private final AccountRewardDao accountRewardsDao;
    private final AccountTimeController accountTimeController;
    private final GameServerTable gameServerTable;
    private final BannedIpController bannedIpController;

    /**
     * 当前在登录服上、或已加入游戏服但尚未完成认证的账号连接。
     * Accounts active on LoginServer or joined GameServer but not yet authenticated.
     */
    private final Map<Integer, LoginConnection> accountsOnLS = new ConcurrentHashMap<>();

    /**
     * 正在从游戏服快速重连回登录服的账号。
     * Accounts reconnecting to LoginServer after joining GameServer.
     */
    private final Map<Integer, ReconnectingAccount> reconnectingAccounts = new ConcurrentHashMap<>();

    /**
     * 各账号在各游戏服上的角色数量。
     * Character counts per gameserver for each account.
     */
    private final Map<Integer, Map<Integer, Integer>> accountsGSCharacterCounts = new ConcurrentHashMap<>();

    /**
     * 从登录服连接列表移除账号。
     * Removes account from list of LoginServer connections.
     * @param account 账号 / Account
     */
    public synchronized void removeAccountOnLS(Account account) {
        accountsOnLS.remove(account.getId());
    }

    /**
     * 响应游戏服侧的账号会话校验请求。
     * Answers GameServer question about account authentication on GS side.
     * @param key 会话密钥 / Session key
     * @param gsConnection 游戏服连接 / GameServer connection
     */
    public synchronized void checkAuth(SessionKey key, GsConnection gsConnection) {
        LoginConnection con = accountsOnLS.get(key.accountId());

        if (con != null && con.getSessionKey().checkSessionKey(key)) {
			accountsOnLS.remove(key.accountId());

            GameServerInfo gsi = gsConnection.getGameServerInfo();
            Account acc = con.getAccount();

            gsi.addAccountToGameServer(acc);

            acc.setLastServer(gsi.getId());
            accountDao.updateLastServer(acc.getId(), acc.getLastServer());

            long toll = accountDao.getToll(acc.getId()) + accountRewardsDao.claimPendingRewards(acc.getId());
            long luna = accountDao.getLuna(acc.getId());
            Vip vip = accountVipDao.findByAccountId(acc.getId());
            long now = System.currentTimeMillis() / 1000L;
            boolean vipActive = vip != null && vip.isActive(now);
            int vipLevel = vipActive ? vip.level() : 0;
            long vipExp = vipActive ? vip.experience() : 0L;
            // keep expire even when permanent (0); zero only when inactive
            long vipExpire = vipActive ? vip.expireTime() : 0L;
            gsConnection.sendPacket(new SM_ACCOUNT_AUTH_RESPONSE(key.accountId(), true, acc.getName(), acc.getAccessLevel(),
                acc.getMembership(), toll, luna, acc.getReturn(), vipLevel, vipExp, vipExpire));
        } else {
            gsConnection.sendPacket(new SM_ACCOUNT_AUTH_RESPONSE(key.accountId(), false, null, (byte) 0, (byte) 0,
                0, 0, (byte) 0, 0, 0, 0));
        }
    }

    /**
     * 将账号加入重连列表。
     * Adds account to reconnection list.
     * @param acc 重连账号 / Reconnecting account
     */
    public synchronized void addReconnectingAccount(ReconnectingAccount acc) {
        reconnectingAccounts.put(acc.account().getId(), acc);
    }

    /**
     * 校验快速重连账号是否允许重新认证。
     * Checks whether a reconnecting account may re-authenticate.
     * @param accountId 账号 ID / Account id
     * @param loginOk 登录确认码 / Login ok token
     * @param reconnectKey 重连密钥 / Reconnect key
     * @param client 客户端连接 / Aion client connection
     */
    public synchronized void authReconnectingAccount(int accountId, int loginOk, int reconnectKey, LoginConnection client) {
        ReconnectingAccount reconnectingAccount = reconnectingAccounts.remove(accountId);

        if (reconnectingAccount != null && reconnectingAccount.reconnectionKey() == reconnectKey) {
            Account acc = reconnectingAccount.account();

            client.setAccount(acc);
            accountsOnLS.put(acc.getId(), client);
            client.setState(State.AUTHED_LOGIN);
            client.setSessionKey(new SessionKey(client.getAccount()));
            client.sendPacket(new SM_UPDATE_SESSION(client.getSessionKey()));
        } else {
            client.closeNow();
        }
    }

    /**
     * 尝试登录账号。<br>
     * 成功时返回 {@link AionAuthResponse#AUTHED} 并将账号绑定到连接。<br>
     * 若启用 {@link com.aionemu.loginserver.configs.Config#ACCOUNT_AUTO_CREATION} 则自动建号。
     * Tries to authenticate account.<br>
     * On success returns {@link AionAuthResponse#AUTHED} and binds account to connection.<br>
     * Creates a new account when {@link com.aionemu.loginserver.configs.Config#ACCOUNT_AUTO_CREATION} is enabled.
     * @param name 账号名称 / Account name
     * @param password 密码 / Password
     * @param connection 登录连接 / Login connection
     * @return 认证结果码 / Auth response with error code
     */
    public AionAuthResponse login(String name, String password, LoginConnection connection) {
        // 若 IP 被封禁 / if ip is banned
        if (bannedIpController.isBanned(connection.getIP())) {
            return AionAuthResponse.BAN_IP;
        }

        Account account = loadAccount(name);

        // 尝试创建新账号。 / Try to create new account
        if (account == null && Config.ACCOUNT_AUTO_CREATION) {
            account = createAccount(name, password);
        }

        // 若账号未找到且未创建 / If account not found and not created
        if (account == null) {
            return AionAuthResponse.INVALID_PASSWORD;
        }

        if (account.getAccessLevel() < Config.MAINTENANCE_MOD_GMLEVEL && Config.MAINTENANCE_MOD) {
            return AionAuthResponse.GM_ONLY;
        }

        // 检查密码是否相等 / check for paswords beeing equals
        if (!account.getPasswordHash().equals(encodePassword(password))) {
            return AionAuthResponse.INVALID_PASSWORD;
        }

        // 检查密码是否相等 / check for paswords beeing equals
        if (account.getActivated() != 1) {
            return AionAuthResponse.INVALID_PASSWORD;
        }

        // 若账号过期 / If account expired
        if (accountTimeController.isAccountExpired(account)) {
            return AionAuthResponse.TIME_EXPIRED;
        }

        // 若账号被封禁 / if account is banned
        if (accountTimeController.isAccountPenaltyActive(account)) {
            return AionAuthResponse.BAN_IP;
        }

        // 若账号限制于某些 IP 或掩码 / if account is restricted to some ip or mask
        if (account.getIpForce() != null) {
            if (!NetworkUtils.checkIPMatching(account.getIpForce(), connection.getIP())) {
                return AionAuthResponse.BAN_IP;
            }
        }

        // 不允许同一账号重复登录 / Do not allow to login two times with same account
        synchronized (AccountController.class) {
            if (gameServerTable.isAccountOnAnyGameServer(account)) {
                gameServerTable.kickAccountFromGameServer(account);
                return AionAuthResponse.ALREADY_LOGGED_IN;
            }

            // 若有人在登录服，应断开其连接 / If someone is at loginserver, he should be disconnected
            if (accountsOnLS.containsKey(account.getId())) {
                LoginConnection aionConnection = accountsOnLS.remove(account.getId());

                aionConnection.closeNow();
                return AionAuthResponse.ALREADY_LOGGED_IN;
            }
            connection.setAccount(account);
            accountsOnLS.put(account.getId(), connection);
        }

        accountTimeController.updateOnLogin(account);

        // 若一切正常 / if everything was OK
        accountDao.updateLastIp(account.getId(), connection.getIP());
        // 收到游戏服务器数据包后更新 last mac / last mac is updated after receiving packet from gameserver
        accountDao.updateMembership(account.getId());

        return AionAuthResponse.AUTHED;
    }

    /**
     * 从登录服与游戏服踢出指定账号。
     * Kicks account from LoginServer and GameServers.
     * @param accountId 要踢出的账号 ID / Account ID to kick
     */
    public void kickAccount(int accountId) {
        synchronized (AccountController.class) {
            for (GameServerInfo gsi : gameServerTable.getGameServers()) {
                if (gsi.isAccountOnGameServer(accountId)) {
                    gsi.getConnection().sendPacket(new SM_REQUEST_KICK_ACCOUNT(accountId));
                    break;
                }
            }
            if (accountsOnLS.containsKey(accountId)) {
                LoginConnection conn = accountsOnLS.remove(accountId);
                conn.closeNow();
            }
        }
    }

    /**
     * 刷新账号的 last_mac。
     * Refreshes last_mac of account.
     * @param accountId 账号 ID / Account id
     * @param address 新 MAC 地址 / New MAC address
     * @return 是否刷新成功 / Whether refresh succeeded
     */
    public boolean refreshAccountsLastMac(int accountId, String address) {
        return accountDao.updateLastMac(accountId, address);
    }

    /**
     * 按名称从数据库加载账号；不存在则返回 null。
     * Loads account from DB by name; returns null if not found.
     * @param name 账号名称 / Account name
     * @return 已加载账号，或 null / Loaded account or null
     */
    public Account loadAccount(String name) {
        Account account = accountDao.getAccount(name);
        if (account != null) {
            account.setAccountTime(accountTimeDao.getAccountTime(account.getId()));
        }
        return account;
    }

    /**
     * 按 ID 从数据库加载账号；不存在则返回 null。
     * Loads account from DB by id; returns null if not found.
     * @param id 账号 ID / Account id
     * @return 已加载账号，或 null / Loaded account or null
     */
    public Account loadAccount(int id) {
        Account account = accountDao.getAccount(id);
        if (account != null) {
            account.setAccountTime(accountTimeDao.getAccountTime(id));
        }
        return account;
    }

    /**
     * 创建新账号并写入数据库；成功返回账号对象，失败返回 null。
     * Creates new account and stores it in DB; returns account on success or null on failure.
     * @param name 账号名称 / Account name
     * @param password 账号密码 / Account password
     * @return 账号对象或 null / Account object or null
     */
    public Account createAccount(String name, String password) {
        String passwordHash = encodePassword(password);
        Account account = new Account();

        account.setName(name);
        account.setPasswordHash(passwordHash);
        account.setAccessLevel((byte) 0);
        account.setMembership((byte) 0);
        account.setActivated((byte) 1);
        account.setReturn((byte) 0);
        account.setReturnEnd(new Timestamp(System.currentTimeMillis()));

        if (accountDao.insertAccount(account)) {
            if (VipConfig.AUTO_ENABLE) {
                accountVipDao.insertIfAbsent(account.getId(), VipConfig.AUTO_ENABLE_LEVEL);
            }
            return account;
        }
        return null;
    }

    /**
     * 向各游戏服请求该账号的角色数量。
     * Requests character counts for the account from all game servers.
     * @param accountId 账号 ID / Account id
     */
    public synchronized void loadGSCharactersCount(int accountId) {
        GsConnection gsc;
        Map<Integer, Integer> accountCharacterCount = new ConcurrentHashMap<>();
        accountsGSCharacterCounts.put(accountId, accountCharacterCount);

        for (GameServerInfo gsi : gameServerTable.getGameServers()) {
            gsc = gsi.getConnection();

            if (gsc != null) {
                gsc.sendPacket(new SM_GS_CHARACTER_RESPONSE(accountId));
            } else {
                accountCharacterCount.put((int) gsi.getId(), 0);
            }
        }

        if (hasAllGSCharacterCounts(accountId)) {
            sendServerListFor(accountId);
        }
    }

    /**
     * 判断是否已收集齐该账号在所有游戏服的角色数。
     * Whether all GS character counts for the account have been collected.
     * @param accountId 账号 ID / Account id
     * @return 是否已齐全 / Whether all counts are present
     */
    public synchronized boolean hasAllGSCharacterCounts(int accountId) {
        Map<Integer, Integer> characterCount = accountsGSCharacterCounts.get(accountId);

        if (characterCount != null) {
			return characterCount.size() == gameServerTable.getGameServers().size();
        }

        return false;
    }

    /**
     * 向指定账号发送服务器列表包 {@link SM_SERVER_LIST}。
     * Sends {@link SM_SERVER_LIST} to the given account.
     * @param accountId 账号 ID / Account id
     */
    public void sendServerListFor(int accountId) {
        LoginConnection connection = accountsOnLS.get(accountId);
        if (connection != null) {
            Map<Integer, Integer> characterCounts = getGSCharacterCountsFor(accountId);
            connection.sendPacket(new SM_SERVER_LIST(gameServerTable.getGameServers(),
                characterCounts == null ? Map.of() : characterCounts));
        }
    }

    /**
     * 返回该账号各游戏服角色数的不可变视图。
     * Returns an unmodifiable view of GS character counts for the account.
     * @param accountId 账号 ID / Account id
     * @return 角色数映射，可能为 null / Character-count map, or null
     */
    public Map<Integer, Integer> getGSCharacterCountsFor(int accountId) {
        Map<Integer, Integer> characterCount = accountsGSCharacterCounts.get(accountId);
        return characterCount == null ? null : Map.copyOf(characterCount);
    }

    /**
     * 记录某账号在指定游戏服上的角色数量。
     * Records character count for an account on a specific gameserver.
     * @param accountId 账号 ID / Account id
     * @param gsid 游戏服 ID / GameServer id
     * @param characterCount 角色数量 / Character count
     */
    public synchronized void addGSCharacterCountFor(int accountId, int gsid, int characterCount) {
        accountsGSCharacterCounts
            .computeIfAbsent(accountId, _ -> new ConcurrentHashMap<>())
            .put(gsid, characterCount);
    }

    /**
     * 对密码进行编码：先 SHA-1 哈希，再以 Base64 包装为字符串。
     * Encodes the password: SHA-1 hash wrapped in Base64.
     * <p>实现委托给 {@link PasswordHash}，与网页商城共用同一份散列格式。
     * Delegates to {@link PasswordHash} so the web shop shares the exact same hash format.</p>
     * @param password 待编码密码 / password to encode
     * @return 编码后的密码 / encoded password
     */
    private static String encodePassword(String password) {
        try {
            return PasswordHash.encode(password);
        } catch (IllegalStateException e) {
            log.error(I18n.get("log.4f2731090659"));
            throw new Error(e);
        }
    }
}
