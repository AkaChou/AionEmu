package com.aionemu.loginserver.controller;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.NetworkUtils;
import com.aionemu.loginserver.GameServerInfo;
import com.aionemu.loginserver.GameServerTable;
import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.dao.AccountDAO;
import com.aionemu.loginserver.dao.AccountTimeDAO;
import com.aionemu.loginserver.dao.PremiumDAO;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.model.ReconnectingAccount;
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
import com.aionemu.loginserver.service.LoginProtectionServices;
import com.aionemu.loginserver.utils.AccountUtils;

import lombok.experimental.UtilityClass;

/**
 * 账号动作总控：登录、重连、踢人及 GS 角色数统计。
 * Controls all account actions: login, reconnect, kick and GS character counts.
 *
 * @author KID
 * @author SoulKeeper
 */
@UtilityClass
public class AccountController {

    /**
     * 当前在登录服上、或已加入游戏服但尚未完成认证的账号连接。
     * Accounts active on LoginServer or joined GameServer but not yet authenticated.
     */
    private final Map<Integer, LoginConnection> accountsOnLS = new ConcurrentHashMap<Integer, LoginConnection>();

    /**
     * 正在从游戏服快速重连回登录服的账号。
     * Accounts reconnecting to LoginServer after joining GameServer.
     */
    private final Map<Integer, ReconnectingAccount> reconnectingAccounts = new ConcurrentHashMap<Integer, ReconnectingAccount>();

    /**
     * 各账号在各游戏服上的角色数量。
     * Character counts per gameserver for each account.
     */
    private final Map<Integer, Map<Integer, Integer>> accountsGSCharacterCounts = new ConcurrentHashMap<Integer, Map<Integer, Integer>>();

    /**
     * 从登录服连接列表移除账号。
     * Removes account from list of LoginServer connections.
     *
     * @param account 账号 / Account
     */
    public synchronized void removeAccountOnLS(Account account) {
        accountsOnLS.remove(account.getId());
    }

    /**
     * 响应游戏服侧的账号会话校验请求。
     * Answers GameServer question about account authentication on GS side.
     *
     * @param key 会话密钥 / Session key
     * @param gsConnection 游戏服连接 / GameServer connection
     */
    public synchronized void checkAuth(SessionKey key, GsConnection gsConnection) {
        LoginConnection con = accountsOnLS.get(key.accountId);

        if (con != null && con.getSessionKey().checkSessionKey(key)) {
            /**
             * 账号已在游戏服成功登录，从登录服列表移除。
             * Account successfully logged in on GS; remove it from here.
             */
            accountsOnLS.remove(key.accountId);

            GameServerInfo gsi = gsConnection.getGameServerInfo();
            Account acc = con.getAccount();

            /**
             * 加入游戏服在线列表并更新上次服务器。
             * Add account to GameServer list and update last server.
             */
            gsi.addAccountToGameServer(acc);

            acc.setLastServer(gsi.getId());
            getAccountDAO().updateLastServer(acc.getId(), acc.getLastServer());

            long toll = DAOManager.getDAO(PremiumDAO.class).getPoints(acc.getId());
            long luna = DAOManager.getDAO(PremiumDAO.class).getLuna(acc.getId());
            /**
             * 向游戏服发送认证结果。
             * Send auth response to GameServer.
             */
            gsConnection.sendPacket(new SM_ACCOUNT_AUTH_RESPONSE(key.accountId, true, acc.getName(), acc.getAccessLevel(), acc.getMembership(), toll, luna, acc.getReturn()));
        } else {
            gsConnection.sendPacket(new SM_ACCOUNT_AUTH_RESPONSE(key.accountId, false, null, (byte) 0, (byte) 0, 0, 0, (byte) 0));
        }
    }

    /**
     * 将账号加入重连列表。
     * Adds account to reconnection list.
     *
     * @param acc 重连账号 / Reconnecting account
     */
    public synchronized void addReconnectingAccount(ReconnectingAccount acc) {
        reconnectingAccounts.put(acc.getAccount().getId(), acc);
    }

    /**
     * 校验快速重连账号是否允许重新认证。
     * Checks whether a reconnecting account may re-authenticate.
     *
     * 账号 ID / Account id
     * @param loginOk 登录确认码 / Login ok token
     * Reconnect key
     * @param client 客户端连接 / Aion client connection
     */
    public synchronized void authReconnectingAccount(int accountId, int loginOk, int reconnectKey, LoginConnection client) {
        ReconnectingAccount reconnectingAccount = reconnectingAccounts.remove(accountId);

        if (reconnectingAccount != null && reconnectingAccount.getReconnectionKey() == reconnectKey) {
            Account acc = reconnectingAccount.getAccount();

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
     *
     * Account name
     * Password
     * Login connection
     * @return 认证结果码 / Auth response with error code
     */
    public AionAuthResponse login(String name, String password, LoginConnection connection) {
        // 若 IP 被封禁 / if ip is banned
        if (LoginProtectionServices.bannedIpService().isBanned(connection.getIP())) {
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
        if (!account.getPasswordHash().equals(AccountUtils.encodePassword(password))) {
            return AionAuthResponse.INVALID_PASSWORD;
        }

        // 检查密码是否相等 / check for paswords beeing equals
        if (account.getActivated() != 1) {
            return AionAuthResponse.INVALID_PASSWORD;
        }

        // 若账号过期 / If account expired
        if (AccountTimeController.isAccountExpired(account)) {
            return AionAuthResponse.TIME_EXPIRED;
        }

        // 若账号被封禁 / if account is banned
        if (AccountTimeController.isAccountPenaltyActive(account)) {
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
            if (GameServerTable.isAccountOnAnyGameServer(account)) {
                GameServerTable.kickAccountFromGameServer(account);
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

        AccountTimeController.updateOnLogin(account);

        // 若一切正常 / if everything was OK
        getAccountDAO().updateLastIp(account.getId(), connection.getIP());
        // 收到游戏服务器数据包后更新 last mac / last mac is updated after receiving packet from gameserver
        getAccountDAO().updateMembership(account.getId());

        return AionAuthResponse.AUTHED;
    }

    /**
     * 从登录服与游戏服踢出指定账号。
     * Kicks account from LoginServer and GameServers.
     *
     * @param accountId 要踢出的账号 ID / Account ID to kick
     */
    public void kickAccount(int accountId) {
        synchronized (AccountController.class) {
            for (GameServerInfo gsi : GameServerTable.getGameServers()) {
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
     *
     * 账号 ID / Account id
     * New MAC address
     *
     * @return 是否刷新成功 / Whether refresh succeeded
     */
    public boolean refreshAccountsLastMac(int accountId, String address) {
        return getAccountDAO().updateLastMac(accountId, address);
    }

    /**
     * 按名称从数据库加载账号；不存在则返回 null。
     * Loads account from DB by name; returns null if not found.
     *
     * Account name
     *
     * @param name
     * @return 已加载账号，或 null / Loaded account or null
     */
    public Account loadAccount(String name) {
        Account account = getAccountDAO().getAccount(name);
        if (account != null) {
            account.setAccountTime(getAccountTimeDAO().getAccountTime(account.getId()));
        }
        return account;
    }

    /**
     * 按 ID 从数据库加载账号；不存在则返回 null。
     * Loads account from DB by id; returns null if not found.
     *
     * @param id 账号 ID / Account id
     * @return 已加载账号，或 null / Loaded account or null
     */
    public Account loadAccount(int id) {
        Account account = getAccountDAO().getAccount(id);
        if (account != null) {
            account.setAccountTime(getAccountTimeDAO().getAccountTime(id));
        }
        return account;
    }

    /**
     * 创建新账号并写入数据库；成功返回账号对象，失败返回 null。
     * Creates new account and stores it in DB; returns account on success or null on failure.
     *
     * Account name
     * Account password
     *
     * @return 账号对象或 null / Account object or null
     */
    public Account createAccount(String name, String password) {
        String passwordHash = AccountUtils.encodePassword(password);
        Account account = new Account();

        account.setName(name);
        account.setPasswordHash(passwordHash);
        account.setAccessLevel((byte) 0);
        account.setMembership((byte) 0);
        account.setActivated((byte) 1);
        account.setReturn((byte) 0);
        account.setReturnEnd(new Timestamp(System.currentTimeMillis()));

        if (getAccountDAO().insertAccount(account)) {
            return account;
        }
        return null;
    }

    /**
     * 获取 {@link AccountDAO} 快捷方法。
     * Shortcut for {@link AccountDAO}.
     *
     * Account DAO
     */
    private AccountDAO getAccountDAO() {
        return DAOManager.getDAO(AccountDAO.class);
    }

    /**
     * 获取 {@link AccountTimeDAO} 快捷方法。
     * Shortcut for {@link AccountTimeDAO}.
     *
     * Account time DAO
     */
    private AccountTimeDAO getAccountTimeDAO() {
        return DAOManager.getDAO(AccountTimeDAO.class);
    }

    /**
     * 向各游戏服请求该账号的角色数量。
     * Requests character counts for the account from all game servers.
     *
     * @param accountId 账号 ID / Account id
     */
    public synchronized void loadGSCharactersCount(int accountId) {
        GsConnection gsc = null;
        Map<Integer, Integer> accountCharacterCount = new ConcurrentHashMap<Integer, Integer>();
        accountsGSCharacterCounts.put(accountId, accountCharacterCount);

        for (GameServerInfo gsi : GameServerTable.getGameServers()) {
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
     *
     * @param accountId 账号 ID / Account id
     * @return 是否已齐全 / Whether all counts are present
     */
    public synchronized boolean hasAllGSCharacterCounts(int accountId) {
        Map<Integer, Integer> characterCount = accountsGSCharacterCounts.get(accountId);

        if (characterCount != null) {
            if (characterCount.size() == GameServerTable.getGameServers().size()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 向指定账号发送服务器列表包 {@link SM_SERVER_LIST}。
     * Sends {@link SM_SERVER_LIST} to the given account.
     *
     * @param accountId 账号 ID / Account id
     */
    public void sendServerListFor(int accountId) {
        LoginConnection connection = accountsOnLS.get(accountId);
        if (connection != null) {
            connection.sendPacket(new SM_SERVER_LIST());
        }
    }

    /**
     * 返回该账号各游戏服角色数的不可变视图。
     * Returns an unmodifiable view of GS character counts for the account.
     *
     * @param accountId 账号 ID / Account id
     * @return 角色数映射，可能为 null / Character-count map, or null
     */
    public Map<Integer, Integer> getGSCharacterCountsFor(int accountId) {
        Map<Integer, Integer> characterCount = accountsGSCharacterCounts.get(accountId);
        return characterCount == null ? null : Collections.unmodifiableMap(new HashMap<Integer, Integer>(characterCount));
    }

    /**
     * 记录某账号在指定游戏服上的角色数量。
     * Records character count for an account on a specific gameserver.
     *
     * 账号 ID / Account id
     * GameServer id
     * Character count
     */
    public synchronized void addGSCharacterCountFor(int accountId, int gsid, int characterCount) {
        accountsGSCharacterCounts
            .computeIfAbsent(accountId, id -> new ConcurrentHashMap<Integer, Integer>())
            .put(gsid, characterCount);
    }
}
