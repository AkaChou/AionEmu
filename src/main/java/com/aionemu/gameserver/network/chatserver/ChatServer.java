package com.aionemu.gameserver.network.chatserver;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.network.NettyClient;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.chatserver.serverpackets.SM_CS_PLAYER_AUTH;
import com.aionemu.gameserver.network.chatserver.serverpackets.SM_CS_PLAYER_LOGOUT;
import com.aionemu.gameserver.network.factories.CsPacketHandlerFactory;

/**
 * 游戏服连接聊天服的门面：负责建连、重连、断开以及玩家登录/登出通知。
 * Facade for the game server's connection to the chat server: connect, reconnect,
 * disconnect, and player login/logout notifications.
 */
@Slf4j
public class ChatServer {
	/**
	 * Spring 注入的单例提供者（优先于内部 SingletonHolder）。
	 * Spring-injected singleton provider (preferred over internal SingletonHolder).
	 */
	private static volatile ObjectProvider<ChatServer> instanceProvider;

	/**
	 * 当前与聊天服的活动连接。
	 * Active connection to the chat server.
	 */
	private volatile ChatServerConnection chatServer;

	/**
	 * 底层 Netty 客户端。
	 * Underlying Netty client.
	 */
	private volatile NettyClient nettyClient;

	/**
	 * 游戏服是否已进入关闭流程。
	 * Whether the game server has entered shutdown.
	 */
	private volatile boolean serverShutdown = false;

	/**
	 * 是否已排队一次重连任务，避免重复调度。
	 * Whether a reconnect task is already queued, to avoid duplicate scheduling.
	 */
	private final AtomicBoolean connectionTaskQueued = new AtomicBoolean(false);

	/**
	 * 当前待执行的重连任务。
	 * Currently scheduled reconnect task.
	 */
	private volatile ScheduledFuture<?> connectionTask;

	/**
	 * 获取 ChatServer 单例：优先 Spring 提供者，否则回退到内部 holder。
	 * Returns the ChatServer singleton: prefer Spring provider, else internal holder.
	 *
	 * ChatServer instance
	 */
	public static final ChatServer getInstance() {
		ObjectProvider<ChatServer> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 设置 Spring 单例提供者。
	 * Sets the Spring singleton provider.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<ChatServer> instanceProvider) {
		ChatServer.instanceProvider = instanceProvider;
	}

	/**
	 * 默认构造。
	 * Default constructor.
	 */
	public ChatServer() {
	}

	/**
	 * 为建连做准备，清除关闭标记。
	 * Prepares for connection by clearing the shutdown flag.
	 */
	public void prepareForConnect() {
		serverShutdown = false;
	}

	/**
	 * 同步阻塞连接到聊天服，失败则每 10 秒重试直至成功。
	 * Connects to the chat server synchronously, retrying every 10 seconds on failure.
	 *
	 * @return 已建立的聊天服连接 / established chat-server connection
	 */
	public ChatServerConnection connect() {
		for (;;) {
			if (connectOnce()) {
				return chatServer;
			}
			try {
				Thread.sleep(10 * 1000);
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 异步启动连接（立即调度一次）。
	 * Starts an asynchronous connect attempt (scheduled immediately).
	 */
	public void connectAsync() {
		scheduleConnect(0);
	}

	/**
	 * 延迟调度一次连接尝试；若已有任务在队或正在关闭则跳过。
	 * Schedules a delayed connect attempt; skips if already queued or shutting down.
	 *
	 * @param delay 延迟毫秒 / delay in milliseconds
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
				if (serverShutdown || chatServer != null) {
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
	 * Performs a single connect attempt.
	 *
	 * @return 是否连接成功 / whether the connection succeeded
	 */
	private boolean connectOnce() {
		chatServer = null;
		log.info(I18n.get("log.e4b304bdbc1d", NetworkConfig.CHAT_ADDRESS));
		return connectWithNetty();
	}

	/**
	 * 通过 Netty 建立到聊天服的连接。
	 * Establishes the chat-server connection via Netty.
	 *
	 * @return 是否连接成功 / whether the connection succeeded
	 */
	private boolean connectWithNetty() {
		shutdownNettyClient();
		try {
			CsPacketHandlerFactory csPacketHandlerFactory = new CsPacketHandlerFactory();
			NettyClient client = new NettyClient(NetworkConfig.CHAT_ADDRESS, "ChatServer", transport -> {
				ChatServerConnection connection = new ChatServerConnection(transport, csPacketHandlerFactory.getPacketHandler());
				chatServer = connection;
				return connection;
			});
			nettyClient = client;
			client.connect();
			return chatServer != null;
		} catch (Exception e) {
			chatServer = null;
			shutdownNettyClient();
			log.info(I18n.get("log.4b5f26f1f747", e.getMessage()));
			return false;
		}
	}

	/**
	 * 聊天服掉线回调：清理连接并在非关闭状态下安排重连。
	 * Callback when the chat server goes down: clear connection and reschedule reconnect if not shutting down.
	 */
	public void chatServerDown() {
		log.warn(I18n.get("log.913b9c4068ef"));
		chatServer = null;
		shutdownNettyClient();
		if (!serverShutdown) {
			scheduleConnect(5000);
		}
	}

	/**
	 * 游戏服主动断开与聊天服的连接（关闭流程）。
	 * Actively disconnects from the chat server (game-server shutdown path).
	 */
	public void gameServerDisconnected() {
		serverShutdown = true;
		cancelConnectionTask();
		if (chatServer != null) {
			chatServer.close(false);
			chatServer = null;
		}
		shutdownNettyClient();
		log.info(I18n.get("log.29e4ab1c55a4"));
	}

	/**
	 * 关闭并释放底层 Netty 客户端。
	 * Shuts down and releases the underlying Netty client.
	 */
	private void shutdownNettyClient() {
		NettyClient client = nettyClient;
		if (client != null) {
			nettyClient = null;
			client.shutdown();
		}
	}

	/**
	 * 取消已调度的重连任务。
	 * Cancels any scheduled reconnect task.
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
	 * 通知聊天服玩家上线并请求认证令牌。
	 * Notifies the chat server of a player login and requests an auth token.
	 *
	 * logging-in player
	 */
	public void sendPlayerLoginRequst(Player player) {
		if (chatServer != null) {
			chatServer
					.sendPacket(new SM_CS_PLAYER_AUTH(player.getObjectId(), player.getAcountName(), player.getName()));
		}
	}

	/**
	 * 通知聊天服玩家下线。
	 * Notifies the chat server of a player logout.
	 *
	 * logging-out player
	 */
	public void sendPlayerLogout(Player player) {
		if (chatServer != null) {
			chatServer.sendPacket(new SM_CS_PLAYER_LOGOUT(player.getObjectId()));
		}
	}

	/**
	 * 内部单例持有者。
	 * Internal singleton holder.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final ChatServer instance = new ChatServer();
	}
}
