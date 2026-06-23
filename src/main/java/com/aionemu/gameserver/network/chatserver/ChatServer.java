/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.network.chatserver;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.network.Dispatcher;
import com.aionemu.commons.network.NettyClient;
import com.aionemu.commons.network.NioServer;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.chatserver.serverpackets.SM_CS_PLAYER_AUTH;
import com.aionemu.gameserver.network.chatserver.serverpackets.SM_CS_PLAYER_LOGOUT;
import com.aionemu.gameserver.network.factories.CsPacketHandlerFactory;
import com.aionemu.gameserver.utils.ThreadPoolManager;

public class ChatServer {
	private static final Logger log = LoggerFactory.getLogger(ChatServer.class);
	private volatile ChatServerConnection chatServer;
	private volatile NettyClient nettyClient;
	private NioServer nioServer;
	private volatile boolean serverShutdown = false;
	private final AtomicBoolean connectionTaskQueued = new AtomicBoolean(false);
	private volatile ScheduledFuture<?> connectionTask;

	public static final ChatServer getInstance() {
		return SingletonHolder.instance;
	}

	private ChatServer() {
	}

	public void setNioServer(NioServer nioServer) {
		this.nioServer = nioServer;
		serverShutdown = false;
	}

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

	public void connectAsync() {
		scheduleConnect(0);
	}

	private void scheduleConnect(long delay) {
		if (serverShutdown || !connectionTaskQueued.compareAndSet(false, true)) {
			return;
		}
		connectionTask = ThreadPoolManager.getInstance().schedule(new Runnable() {
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

	private boolean connectOnce() {
		chatServer = null;
		log.info("Connecting to ChatServer: " + NetworkConfig.CHAT_ADDRESS);
		if (Boolean.getBoolean("aion.transport.netty")) {
			return connectWithNetty();
		}
		SocketChannel sc = null;
		try {
			sc = SocketChannel.open(NetworkConfig.CHAT_ADDRESS);
			sc.configureBlocking(false);
			Dispatcher d = nioServer.getReadWriteDispatcher();
			CsPacketHandlerFactory csPacketHandlerFactory = new CsPacketHandlerFactory();
			chatServer = new ChatServerConnection(sc, d, csPacketHandlerFactory.getPacketHandler());
			d.register(sc, SelectionKey.OP_READ, chatServer);
			chatServer.initialized();
			return true;
		} catch (Exception e) {
			chatServer = null;
			if (sc != null) {
				try {
					sc.close();
				} catch (Exception ignored) {
				}
			}
			log.info("Cant connect to ChatServer: " + e.getMessage());
			return false;
		}
	}

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
			log.info("Cant connect to ChatServer: " + e.getMessage());
			return false;
		}
	}

	public void chatServerDown() {
		log.warn("Connection with ChatServer lost...");
		chatServer = null;
		shutdownNettyClient();
		if (!serverShutdown) {
			scheduleConnect(5000);
		}
	}

	public void gameServerDisconnected() {
		serverShutdown = true;
		cancelConnectionTask();
		if (chatServer != null) {
			chatServer.close(false);
			chatServer = null;
		}
		shutdownNettyClient();
		log.info("GameServer disconnected from the Chat Server...");
	}

	private void shutdownNettyClient() {
		NettyClient client = nettyClient;
		if (client != null) {
			nettyClient = null;
			client.shutdown();
		}
	}

	private void cancelConnectionTask() {
		ScheduledFuture<?> task = connectionTask;
		if (task != null) {
			task.cancel(false);
			connectionTask = null;
		}
		connectionTaskQueued.set(false);
	}

	public void sendPlayerLoginRequst(Player player) {
		if (chatServer != null) {
			chatServer
					.sendPacket(new SM_CS_PLAYER_AUTH(player.getObjectId(), player.getAcountName(), player.getName()));
		}
	}

	public void sendPlayerLogout(Player player) {
		if (chatServer != null) {
			chatServer.sendPacket(new SM_CS_PLAYER_LOGOUT(player.getObjectId()));
		}
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final ChatServer instance = new ChatServer();
	}
}
