package com.aionemu.gameserver.network.chatserver;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.network.NettyClient;
import com.aionemu.gameserver.network.chatserver.ChatServerConnection.State;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.chatserver.serverpackets.SM_CS_PLAYER_AUTH;
import com.aionemu.gameserver.network.chatserver.serverpackets.SM_CS_PLAYER_LOGOUT;
import com.aionemu.gameserver.network.chatserver.clientpackets.CM_CS_AUTH_RESPONSE;
import com.aionemu.gameserver.network.chatserver.clientpackets.CM_CS_PLAYER_AUTH_RESPONSE;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * 游戏服连接聊天服的门面：负责建连、重连、断开以及玩家登录/登出通知。
 * Facade for the game server's connection to the chat server: connect, reconnect,
 * disconnect, and player login/logout notifications.
 * 资源认领在实例锁内，网络关闭在锁外；在途建连结束后补清理。
 * Resources are claimed under the instance lock, closed outside it, and drained again after in-flight connects.
 */
@Slf4j
public class ChatServer {
	/**
	 * Spring 注入的单例提供者（优先于内部 SingletonHolder）。
	 * Spring-injected singleton provider (preferred over internal SingletonHolder).
	 */
	private static volatile ObjectProvider<ChatServer> instanceProvider;
	private static volatile ChatServer resolvedInstance;

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
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean / when no provider or bean is available
	 */
	public static ChatServer getInstance() {
		ChatServer resolved = resolvedInstance;
		if (resolved != null) {
			return resolved;
		}
		ObjectProvider<ChatServer> provider = instanceProvider;
		resolved = provider == null ? null : provider.getIfAvailable();
		if (resolved == null) {
			throw new IllegalStateException("ChatServer 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		resolvedInstance = resolved;
		return resolved;
	}

	/**
	 * 设置 Spring 单例提供者。
	 * Sets the Spring singleton provider.
	 * @param instanceProvider 提供者 / provider
	 */
	public static void setInstanceProvider(ObjectProvider<ChatServer> instanceProvider) {
		ChatServer.instanceProvider = instanceProvider;
		resolvedInstance = null;
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
	public synchronized void prepareForConnect() {
		serverShutdown = false;
	}

	/**
	 * 同步阻塞连接到聊天服，失败则每 10 秒重试直至成功。
	 * Connects to the chat server synchronously, retrying every 10 seconds on failure.
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
	 * @param delay 延迟毫秒 / delay in milliseconds
	 */
	private void scheduleConnect(long delay) {
		if (serverShutdown || !connectionTaskQueued.compareAndSet(false, true)) {
			return;
		}
		connectionTask = ThreadPoolManager.getInstance().schedule(() -> {
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
		}, delay);
	}

	/**
	 * 执行一次连接尝试。
	 * Performs a single connect attempt.
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
	 * @return 是否连接成功 / whether the connection succeeded
	 */
	private boolean connectWithNetty() {
		shutdownNettyClient();
		CsPacketHandler handler = buildPacketHandler();
		NettyClient client = new NettyClient(NetworkConfig.CHAT_ADDRESS, "ChatServer", transport -> {
			ChatServerConnection connection = new ChatServerConnection(transport, handler);
			synchronized (this) {
				chatServer = connection;
			}
			return connection;
		});
		return connectWithNetty(client);
	}

	/**
	 * 建连后复查关闭状态，保留本次客户端以释放关闭后才创建的资源。
	 * Rechecks shutdown after connecting and retains this client to release resources created after close.
	 */
	private boolean connectWithNetty(NettyClient client) {
		try {
			synchronized (this) {
				nettyClient = client;
			}
			client.connect();
			return chatServer != null && !chatServer.isClosing();
		} catch (Exception e) {
			chatServer = null;
			shutdownNettyClient();
			log.info(I18n.get("log.4b5f26f1f747", e.getMessage()), e);
			return false;
		} finally {
			if (serverShutdown) {
				try {
					gameServerDisconnected();
				} finally {
					client.shutdown();
				}
			}
		}
	}

	/**
	 * 聊天服掉线回调：清理连接并在非关闭状态下安排重连。
	 * Callback when the chat server goes down: clear connection and reschedule reconnect if not shutting down.
	 */
	public void chatServerDown() {
		if (!serverShutdown) {
			log.warn(I18n.get("log.913b9c4068ef"));
		}
		chatServer = null;
		shutdownNettyClient();
		if (!serverShutdown) {
			scheduleConnect(5000);
		}
	}

	/**
	 * 主动关闭并认领当前资源；重复调用清理迟到连接，但只记录一次结果。
	 * Claims resources for active shutdown; repeated calls drain late connections but log only once.
	 */
	public void gameServerDisconnected() {
		ChatServerConnection connection;
		boolean firstShutdown;
		synchronized (this) {
			firstShutdown = !serverShutdown;
			serverShutdown = true;
			cancelConnectionTask();
			connection = chatServer;
			chatServer = null;
		}
		try {
			if (connection != null) {
				connection.close(false);
			}
		} finally {
			shutdownNettyClient();
		}
		if (firstShutdown) {
			log.info(I18n.get("log.29e4ab1c55a4"));
		}
	}

	/**
	 * 关闭并释放底层 Netty 客户端。
	 * Shuts down and releases the underlying Netty client.
	 */
	private void shutdownNettyClient() {
		NettyClient client;
		synchronized (this) {
			client = nettyClient;
			nettyClient = null;
		}
		if (client != null) {
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
	 * @param player 登录中的玩家 / logging-in player
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
	 * @param player 登出中的玩家 / logging-out player
	 */
	public void sendPlayerLogout(Player player) {
		if (chatServer != null) {
			chatServer.sendPacket(new SM_CS_PLAYER_LOGOUT(player.getObjectId()));
		}
	}

	/**
	 * 构建聊天服 CS 包处理器（原型 + 合法连接状态）。
	 * Builds the chat-server CS packet handler (prototypes + valid connection states).
	 * @return 已注册原型的处理器 / handler with registered prototypes
	 */
	private static CsPacketHandler buildPacketHandler() {
		return new CsPacketFactory().getPacketHandler();
	}

	/**
	 * 聊天服包处理器工厂：注册 CS 客户端包原型。
	 * Chat-server packet handler factory: registers CS client packet prototypes.
	 */
	private static final class CsPacketFactory {

		/** 包原型 → 合法状态注册表 / packet prototype → valid-state registry */
		private final CsPacketHandler handler = new CsPacketHandler();

		/**
		 * 注册聊天服包处理器。
		 * Registers chat-server packet handlers.
		 */
		private CsPacketFactory() {
			addPacket(new CM_CS_AUTH_RESPONSE(0x00), State.CONNECTED);
			addPacket(new CM_CS_PLAYER_AUTH_RESPONSE(0x01), State.AUTHED);
		}

		/**
		 * 向处理器注册包原型及合法状态。
		 * Registers a packet prototype with valid states.
		 * @param prototype 包原型 / packet prototype
		 * @param states    合法连接状态 / valid connection states
		 */
		private void addPacket(CsClientPacket prototype, State... states) {
			handler.addPacketPrototype(prototype, states);
		}

		/**
		 * 获取已注册的包处理器。
		 * Returns the registered packet handler.
		 * @return 包处理器 / packet handler
		 */
		private CsPacketHandler getPacketHandler() {
			return handler;
		}
	}
}
