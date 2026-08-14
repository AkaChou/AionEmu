package com.aionemu.gameserver.network.loginserver;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.network.NettyClient;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.account.AccountTime;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.serverpackets.SM_L2AUTH_LOGIN_CHECK;
import com.aionemu.gameserver.network.aion.serverpackets.SM_RECONNECT_KEY;
import com.aionemu.gameserver.network.loginserver.LoginServerConnection.State;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_ACCOUNT_AUTH;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_ACCOUNT_DISCONNECTED;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_ACCOUNT_RECONNECT_KEY;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_BAN;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_LS_CONTROL;
import com.aionemu.gameserver.services.AccountService;

/**
 * 游戏服与登录服通信的门面工具类。
 * Utility facade for connecting the GameServer to the LoginServer.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class LoginServer {

	/**
	 * Spring ObjectProvider，用于覆盖静态单例。
	 * Spring ObjectProvider used to override the static singleton.
	 */
	private static volatile ObjectProvider<LoginServer> instanceProvider;

	/**
	 * 等待登录服响应的账号连接表（accountId → AionConnection）。
	 * Map of accountId to connection for pending LoginServer requests.
	 */
	private Map<Integer, AionConnection> loginRequests = new HashMap<Integer, AionConnection>();

	/**
	 * 已登录账号连接表（accountId → AionConnection）。
	 * Map of accountId to connection for all logged-in accounts.
	 */
	private Map<Integer, AionConnection> loggedInAccounts = new HashMap<Integer, AionConnection>();

	/**
	 * 与登录服的当前连接。
	 * Current connection to the LoginServer.
	 */
	private volatile LoginServerConnection loginServer;

	/**
	 * 底层 Netty 客户端实例。
	 * Underlying Netty client instance.
	 */
	private volatile NettyClient nettyClient;

	/**
	 * 是否处于游戏服关闭流程。
	 * Whether the GameServer is in shutdown sequence.
	 */
	private volatile boolean serverShutdown = false;

	/**
	 * 连接任务是否已排队，防止重复调度。
	 * Whether a connect task is already queued; prevents double scheduling.
	 */
	private final AtomicBoolean connectionTaskQueued = new AtomicBoolean(false);

	/**
	 * 异步连接/重连定时任务。
	 * Scheduled task for async connect/reconnect.
	 */
	private volatile ScheduledFuture<?> connectionTask;

	/**
	 * 获取 LoginServer 单例（优先 Spring Provider）。
	 * Returns the LoginServer singleton (prefers Spring provider).
	 *
	 * LoginServer instance
	 */
	public static final LoginServer getInstance() {
		ObjectProvider<LoginServer> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring ObjectProvider，供 DI 覆盖静态单例。
	 * Injects Spring ObjectProvider to override the static singleton.
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<LoginServer> instanceProvider) {
		LoginServer.instanceProvider = instanceProvider;
	}

	/**
	 * 默认构造。
	 * Default constructor.
	 */
	public LoginServer() {

	}

	/**
	 * 为建立连接做准备（清除关闭标志）。
	 * Prepare for connecting (clears the shutdown flag).
	 */
	public void prepareForConnect() {
		serverShutdown = false;
	}

	/**
	 * 阻塞连接登录服，成功后返回连接对象；失败则每隔 10 秒重试。
	 * Blocking connect to LoginServer; retries every 10s until success.
	 *
	 * @return 已建立的登录服连接 / Established LoginServer connection
	 */
	public LoginServerConnection connect() {
		for (;;) {
			if (connectOnce()) {
				return loginServer;
			}
			try {
				/**
				 * 休眠 10 秒后重试。
				 * Sleep 10s before retry.
				 */
				Thread.sleep(10 * 1000);
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 异步发起连接（延迟 0）。
	 * Start an asynchronous connect (delay 0).
	 */
	public void connectAsync() {
		scheduleConnect(0);
	}

	/**
	 * 按指定延迟调度一次连接尝试；已关闭或已排队时忽略。
	 * Schedule one connect attempt after the given delay; no-op if shut down or already queued.
	 *
	 * @param delay 延迟毫秒 / Delay in milliseconds
	 */
	private void scheduleConnect(long delay) {
		if (serverShutdown || !connectionTaskQueued.compareAndSet(false, true)) {
			return;
		}
		connectionTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				connectionTaskQueued.set(false);
				connectionTask = null;
				if (serverShutdown || loginServer != null) {
					return;
				}
				if (!connectOnce()) {
					scheduleConnect(5000);
				} else if (serverShutdown) {
					gameServerDisconnected();
				}
			}
		}, delay);
	}

	/**
	 * 执行一次连接尝试。
	 * Perform a single connect attempt.
	 *
	 * @return 是否连接成功 / Whether the connect succeeded
	 */
	private boolean connectOnce() {
		loginServer = null;
		log.info(I18n.get("log.0548078e4933", NetworkConfig.LOGIN_ADDRESS));
		return connectWithNetty();
	}

	/**
	 * 通过 NettyClient 连接登录服。
	 * Connect to LoginServer via NettyClient.
	 *
	 * @return 是否连接成功 / Whether the connect succeeded
	 */
	private boolean connectWithNetty() {
		shutdownNettyClient();
		try {
			NettyClient client = new NettyClient(NetworkConfig.LOGIN_ADDRESS, "LoginServer", transport -> {
				LoginServerConnection connection = new LoginServerConnection(transport);
				loginServer = connection;
				return connection;
			});
			nettyClient = client;
			client.connect();
			return loginServer != null;
		} catch (Exception e) {
			loginServer = null;
			shutdownNettyClient();
			log.info(I18n.get("log.26fdd67c39d5", e.getMessage()));
			return false;
		}
	}

	/**
	 * 与登录服连接断开时调用：清理等待认证的客户端，并在非关闭流程下 5 秒后重连。
	 * Called when the LoginServer link is lost: disconnect clients pending auth and reconnect after 5s unless shutting down.
	 */
	public void loginServerDown() {
		log.warn(I18n.get("log.8bebb9784be8"));

		loginServer = null;
		shutdownNettyClient();
		synchronized (this) {
			/**
			 * 登录服连接丢失，等待认证的客户端永远无法完成认证，应立即断开。
			 * LoginServer link lost; clients pending authentication will never finish and must be closed.
			 */
			for (AionConnection client : loginRequests.values()) {
				client.close(true);
			}
			loginRequests.clear();
		}

		/**
		 * 非关闭流程时 5 秒后重连。
		 * Reconnect after 5s if not in server shutdown sequence.
		 */
		if (!serverShutdown) {
			scheduleConnect(5000);
		}
	}

	/**
	 * 客户端断开时清理等待请求，并通知登录服该账号已不在本游戏服。
	 * On client disconnect, clear pending requests and notify LoginServer the account is no longer on this GameServer.
	 *
	 * @param accountId 账号 ID / Account id
	 */
	public void aionClientDisconnected(int accountId) {
		synchronized (this) {
			loginRequests.remove(accountId);
			loggedInAccounts.remove(accountId);
		}
		sendAccountDisconnected(accountId);
	}

	/**
	 * 向登录服发送账号断开通知。
	 * Send account-disconnected notification to LoginServer.
	 *
	 * @param accountId 账号 ID / Account id
	 */
	private void sendAccountDisconnected(int accountId) {
		log.info(I18n.get("log.2affe124175e", accountId));
		if (loginServer != null && loginServer.getState() == State.AUTHED) {
			loginServer.sendPacket(new SM_ACCOUNT_DISCONNECTED(accountId));
		}
	}

	/**
	 * 启动客户端认证流程；登录服将回传账号名等认证结果。
	 * Start client authentication; LoginServer will reply with account name and result.
	 *
	 * @param accountId 账号 ID / Account id
	 * @param client 客户端连接 / Client connection
	 * @param loginOk 登录校验码 / Login OK token
	 * @param playOk1 游戏校验码 1 / Play OK token 1
	 * @param playOk2 游戏校验码 2 / Play OK token 2
	 */
	public void requestAuthenticationOfClient(int accountId, AionConnection client, int loginOk, int playOk1,
			int playOk2) {
		/**
		 * 无登录服连接时无法认证，应断开客户端。
		 * Without a LoginServer link authentication is impossible; disconnect the client.
		 */
		if (loginServer == null || loginServer.getState() != State.AUTHED) {
			log.warn(I18n.get("log.a7a0f4156970", (loginServer == null ? "NULL" : loginServer.getState())));
			client.close(true);
			return;
		}

		synchronized (this) {
			if (loginRequests.containsKey(accountId)) {
				return;
			}
			loginRequests.put(accountId, client);
		}
		loginServer.sendPacket(new SM_ACCOUNT_AUTH(accountId, loginOk, playOk1, playOk2));
	}

	/**
	 * 由 CM_ACCOUNT_AUTH_RESPONSE 调用，通知游戏服客户端认证结果。
	 * Called by CM_ACCOUNT_AUTH_RESPONSE to notify GameServer of client authentication results.
	 *
	 * @param accountId 账号 ID / account id
	 * @param accountName 账号名 / account name
	 * @param result 是否认证成功 / whether authentication succeeded
	 * @param accountTime 账号时间信息 / account time info
	 * @param accessLevel 权限等级 / access level
	 * @param membership 会员等级 / membership level
	 * @param toll 特级点余额 / toll balance
	 * @param luna 露娜点余额 / luna balance
	 * @param vipLevel VIP 等级 / vip level
	 * @param vipExp VIP 经验 / vip exp
	 * @param vipExpireTime VIP 到期时间 / vip expire time
	 */
	public void accountAuthenticationResponse(int accountId, String accountName, boolean result,
			AccountTime accountTime, byte accessLevel, byte membership, long toll, long luna, byte vipLevel, long vipExp,
			long vipExpireTime) {
		AionConnection client = loginRequests.remove(accountId);

		if (client == null) {
			return;
		}

		Account account = AccountService.getAccount(accountId, accountName, accountTime, accessLevel, membership, toll,
				luna, vipLevel, vipExp, vipExpireTime);

		if (!validateAccount(account)) {
			log.info(I18n.get("log.0402d3ab6af9", accountId));
			client.close(new SM_L2AUTH_LOGIN_CHECK(false, accountName), true);
			return;
		}

		if (result) {
			client.setAccount(account);
			client.setState(AionConnection.State.AUTHED);
			loggedInAccounts.put(accountId, client);
			log.info(I18n.get("log.b897dae68a6c", accountId, accountName));
			client.sendPacket(new SM_L2AUTH_LOGIN_CHECK(true, accountName));
		} else {
			log.info(I18n.get("log.9499ba394cdf", accountId));
			client.close(new SM_L2AUTH_LOGIN_CHECK(false, accountName), true);
		}
	}

	/**
	 * 校验账号：若任一角色仍标记为在线则拒绝登录。
	 * Validate account: reject login if any character is still marked online.
	 *
	 * @param account 账号 / Account
	 * @return 是否通过校验 / Whether validation passed
	 */
	private boolean validateAccount(Account account) {
		for (PlayerAccountData accountData : account)
			if (accountData.getPlayerCommonData().isOnline()) {
				log.warn(I18n.get("log.8d691f72fea4", account.getId()));
				return false;
			}
		return true;
	}

	/**
	 * 启动重连登录服流程；登录服将回传重连密钥。
	 * Start LoginServer reconnection procedure; LoginServer will reply with a reconnection key.
	 *
	 * @param client 客户端连接 / Client connection
	 */
	public void requestAuthReconnection(AionConnection client) {
		/**
		 * 无登录服连接时无法认证，应断开客户端。
		 * Without a LoginServer link authentication is impossible; disconnect the client.
		 */
		if (loginServer == null || loginServer.getState() != State.AUTHED) {
			client.close(false);
			return;
		}

		synchronized (this) {
			if (loginRequests.containsKey(client.getAccount().getId())) {
				return;
			}
			loginRequests.put(client.getAccount().getId(), client);
		}
		loginServer.sendPacket(new SM_ACCOUNT_RECONNECT_KEY(client.getAccount().getId()));
	}

	/**
	 * 由 CM_ACCOUNT_RECONNECT_KEY 调用，将重连密钥下发给请求重连的客户端。
	 * Called by CM_ACCOUNT_RECONNECT_KEY to deliver the reconnection key to the requesting client.
	 *
	 * 账号 ID / Account id
	 * Reconnection key
	 */
	public void authReconnectionResponse(int accountId, int reconnectKey) {
		AionConnection client = loginRequests.remove(accountId);

		if (client == null) {
			return;
		}
		log.info(I18n.get("log.34f73a7847ac", accountId, client.getAccount().getName()));
		client.close(new SM_RECONNECT_KEY(reconnectKey), false);
	}

	/**
	 * 由 CM_REQUEST_KICK_ACCOUNT 调用，请求踢下指定账号的客户端。
	 * Called by CM_REQUEST_KICK_ACCOUNT to request GameServer kick the client with the given account id.
	 *
	 * @param accountId 账号 ID / Account id
	 */
	public void kickAccount(int accountId) {
		synchronized (this) {
			AionConnection client = loggedInAccounts.get(accountId);
			if (client != null) {
				closeClientWithCheck(client, accountId);
			} else { // 该账号未在本游戏服登录，但登录服认为已登录。 / This account is not logged in on this GameServer but LS thinks different...
				sendAccountDisconnected(accountId);
			}
		}
	}

	/**
	 * 关闭客户端并在 5 秒后复查是否仍残留在登录表中。
	 * Close the client and re-check after 5s whether it still remains in the logged-in map.
	 *
	 * @param client 客户端连接 / Client connection
	 * @param accountId 账号 ID / Account id
	 */
	private void closeClientWithCheck(AionConnection client, final int accountId) {
		log.info(I18n.get("log.2e4538832cae", accountId));
		client.close(/* closePacket, */false);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				AionConnection client = loggedInAccounts.get(accountId);
				if (client != null) {
					log.warn(I18n.get("log.9300ce8c338a"));
					client.close(false);
					loggedInAccounts.remove(accountId);
					sendAccountDisconnected(accountId);
				}
			}
		}, 5000);
	}

	/**
	 * 返回当前游戏服已登录账号的不可变映射（键：账号 ID，值：连接）。
	 * Returns an unmodifiable map of accounts logged in on this GS (key: account id, value: connection).
	 *
	 * @return 不可变的已登录账号表 / Unmodifiable map of logged-in accounts
	 */
	public Map<Integer, AionConnection> getLoggedInAccounts() {
		return Collections.unmodifiableMap(loggedInAccounts);
	}

	/**
	 * 游戏服关闭时：关闭所有等待中的客户端连接并断开登录服。
	 * On GameServer shutdown: close all pending client connections and disconnect LoginServer.
	 */
	public void gameServerDisconnected() {
		synchronized (this) {
			serverShutdown = true;
			cancelConnectionTask();
			/**
			 * 游戏服关闭，必须关闭所有等待中的登录请求。
			 * GameServer is shutting down; must close all pending login requests.
			 */
			for (AionConnection client : loginRequests.values()) {
				client.close(true);
			}
			loginRequests.clear();

			if (loginServer != null) {
				loginServer.close(false);
				loginServer = null;
			}
			shutdownNettyClient();
		}
		log.info(I18n.get("log.79fd1a51455e"));
	}

	/**
	 * 关闭并清理 Netty 客户端。
	 * Shut down and clear the Netty client.
	 */
	private void shutdownNettyClient() {
		NettyClient client = nettyClient;
		if (client != null) {
			nettyClient = null;
			client.shutdown();
		}
	}

	/**
	 * 取消待执行的连接任务。
	 * Cancel any pending connection task.
	 */
	private void cancelConnectionTask() {
		ScheduledFuture<?> task = connectionTask;
		if (task != null) {
			task.cancel(false);
			connectionTask = null;
		}
		connectionTaskQueued.set(false);
	}

	/**
	 * 向登录服发送 LS 控制包（权限/会员等管理操作）。
	 * Send an LS control packet to LoginServer (access/membership admin ops).
	 *
	 * Account name
	 * Player name
	 * Admin name
	 * Parameter
	 * @param type 控制类型 / Control type
	 */
	public void sendLsControlPacket(String accountName, String playerName, String adminName, int param, int type) {
		if (loginServer != null && loginServer.getState() == State.AUTHED) {
			loginServer.sendPacket(new SM_LS_CONTROL(accountName, playerName, adminName, param, type));
		}
	}

	/**
	 * 根据登录服回传更新本地账号的权限或会员等级。
	 * Update local account access level or membership from LoginServer response.
	 *
	 * 账号 ID / Account id
	 * New value
	 * @param type 1=权限等级，2=会员等级 / 1=access level, 2=membership
	 */
	public void accountUpdate(int accountId, byte param, int type) {
		synchronized (this) {
			AionConnection client = loggedInAccounts.get(accountId);
			if (client != null) {
				Account account = client.getAccount();
				if (type == 1) {
					account.setAccessLevel(param);
				}
				if (type == 2) {
					account.setMembership(param);
				}
			}
		}
	}

	/**
	 * 向登录服发送封禁包。
	 * Send a ban packet to LoginServer.
	 *
	 * @param type 封禁类型 / Ban type
	 * 账号 ID / Account id
	 * @param ip IP 地址 / IP address
	 * Duration
	 * @param adminObjId 管理员对象 ID / Admin object id
	 */
	public void sendBanPacket(byte type, int accountId, String ip, int time, int adminObjId) {
		if (loginServer != null && loginServer.getState() == State.AUTHED) {
			loginServer.sendPacket(new SM_BAN(type, accountId, ip, time, adminObjId));
		}
	}

	/**
	 * 在登录服已认证时发送任意 LS 服务端封包。
	 * Send any LS server packet when LoginServer is authenticated.
	 *
	 * @param pk 待发送封包 / Packet to send
	 * @return 是否发送成功 / Whether the packet was sent
	 */
	public boolean sendPacket(LsServerPacket pk) {
		if (loginServer != null && loginServer.getState() == State.AUTHED) {
			loginServer.sendPacket(pk);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 静态单例持有者。
	 * Static singleton holder.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		/**
		 * 默认 LoginServer 实例。
		 * Default LoginServer instance.
		 */
		protected static final LoginServer instance = new LoginServer();
	}
}
