package com.aionemu.gameserver.network.aion;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.commons.network.PacketProcessor;
import com.aionemu.commons.utils.concurrent.ExecuteWrapper;
import com.aionemu.commons.utils.concurrent.RunnableStatsManager;
import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.Crypt;
import com.aionemu.gameserver.lifecycle.GameServerNetworkServices;
import com.aionemu.gameserver.network.aion.serverpackets.SM_KEY;
import com.aionemu.gameserver.network.factories.AionPacketHandlerFactory;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_MAC;
import com.aionemu.gameserver.services.player.PlayerLeaveWorldService;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏服与 Aion 客户端之间的连接对象。
 * Connection object between GameServer and an Aion client.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class AionConnection extends AConnection {


	/** 客户端包线程池处理器 / client packet processor thread pool */
	private static final PacketProcessor<AionConnection> packetProcessor = new PacketProcessor<AionConnection>(
			NetworkConfig.PACKET_PROCESSOR_MIN_THREADS, NetworkConfig.PACKET_PROCESSOR_MAX_THREADS,
			NetworkConfig.PACKET_PROCESSOR_THREAD_SPAWN_THRESHOLD, NetworkConfig.PACKET_PROCESSOR_THREAD_KILL_THRESHOLD,
			new ExecuteWrapper());

	/**
	 * AionConnection 可能的连接状态。
	 * Possible states of an AionConnection.
	 */
	public static enum State {
		/**
		 * 客户端刚连接。
		 * Client just connected.
		 */
		CONNECTED,
		/**
		 * 客户端已认证。
		 * Client is authenticated.
		 */
		AUTHED,
		/**
		 * 客户端已进入游戏世界。
		 * Client entered the world.
		 */
		IN_GAME;
	}

	/**
	 * 待发送服务端包队列。
	 * Server packet "to send" queue.
	 */
	private final List<AionServerPacket> sendMsgQueue = new ArrayList<AionServerPacket>();

	/**
	 * 当前连接状态。
	 * Current state of this connection.
	 */
	private volatile State state;

	/**
	 * 通过账号 ID 完成认证后关联的账号对象。
	 * Account associated after the client authenticates by account id.
	 */
	private Account account;

	/**
	 * 负责加解密本连接包的 Crypt。
	 * Crypt that encrypts/decrypts packets on this connection.
	 */
	private final Crypt crypt = new Crypt();

	/**
	 * 当前在线玩家（已进入游戏）。
	 * Active player that the owner of this connection is playing.
	 */
	private AtomicReference<Player> activePlayer = new AtomicReference<Player>();
	private String lastPlayerName = "";

	private AionPacketHandler aionPacketHandler;
	private long lastPingTimeMS;

	private int nbInvalidPackets = 0;
	private final static int MAX_INVALID_PACKETS = 3;

	private String macAddress;

	/** 挂起连接的 Ping 检测器 / Ping checker for hanged-up connections */
	private PingChecker pingChecker;

	/** 包洪泛过滤表与最近请求时间 / packet flood filter table and last request times */
	private int[] pff;
	private long[] pffRequests;

	/**
	 * 基于传输层创建连接并初始化。
	 * Creates a connection from the transport and initializes it.
	 *
	 * connection transport
	 */
	public AionConnection(ConnectionTransport transport) {
		super(transport, 8192 * 2, 0xFFFF);
		initialize();
	}

	/**
	 * 初始化包处理器、状态、Ping 检测与洪泛过滤表。
	 * Initializes packet handler, state, ping checker and flood filter table.
	 */
	private void initialize() {
		AionPacketHandlerFactory aionPacketHandlerFactory = GameServerNetworkServices.aionPacketHandlerFactory();
		this.aionPacketHandler = aionPacketHandlerFactory.getPacketHandler();

		state = State.CONNECTED;

		String ip = getIP();
		log.info(I18n.get("log.ccb9b7995758", ip));

		pingChecker = new PingChecker();
		pingChecker.start();

		syncPacketFloodFilter();
	}

	/**
	 * 同步最新的包洪泛过滤表；长度变化时重建请求时间数组。
	 * Syncs the latest packet flood filter table; rebuilds request times when length changes.
	 */
	private void syncPacketFloodFilter() {
		pff = GameServerNetworkServices.packetFloodFilter().getPackets();
		if (pffRequests == null || pffRequests.length != pff.length) {
			pffRequests = new long[pff.length];
		}
	}

	/**
	 * 传输就绪后发送 SM_KEY。
	 * Sends SM_KEY once the transport is ready.
	 */
	@Override
	protected void initialized() {
		/** 发送 SM_KEY 数据包 / Send SM_KEY packet */
		sendPacket(new SM_KEY());
	}

	/**
	 * 启用加密密钥：生成随机密钥加密后续服务端包并解密客户端包。
	 * 由 SM_KEY 调用，将密钥发送给 Aion 客户端。
	 * Enables the crypt key — generates a random key to encrypt subsequent server packets
	 * and decrypt client packets. Called from SM_KEY which sends the key to the client.
	 *
	 * @return 发给客户端的“伪密钥” / "false key" for the Aion client
	 */
	public final int enableCryptKey() {
		return crypt.enableKey();
	}

	/**
	 * 由传输帧处理器调用：解密并处理一个客户端包。
	 * Called by the transport frame handler; decrypts and processes one client packet.
	 *
	 * packet data
	 *
	 * @param data
	 * @return true 处理成功；false 表示应立即关闭连接 / true if ok, false to close now
	 */
	@Override
	protected final boolean processData(ByteBuffer data) {
		try {
			if (!crypt.decrypt(data)) {
				nbInvalidPackets++;
				log.info(I18n.get("log.d968eb33925d", nbInvalidPackets, MAX_INVALID_PACKETS));
				if (nbInvalidPackets >= MAX_INVALID_PACKETS) {
					log.warn(I18n.get("log.76b586319b01"));
					return false;
				}
				return true;
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.885daea16916", ex.getMessage()));
			return false;
		}

		if (data.remaining() < 5) {// op + static code + op == 5 bytes
			log.error(I18n.get("log.a4bdacc14a6e", this));
			return false;
		}

		AionClientPacket pck = aionPacketHandler.handle(data, this);

		/**
		 * 仅当包存在且读取成功时执行。
		 * Execute packet only if it exists (!= null) and read was ok.
		 */
		if (pck != null) {
			if (SecurityConfig.PFF_ENABLE) {
				syncPacketFloodFilter();
				int opcode = pck.getOpcode();
				if (pff.length > opcode) {
					if (pff[opcode] > 0) {
						long last = this.pffRequests[opcode];
						if (last == 0) {
							this.pffRequests[opcode] = System.currentTimeMillis();
						} else {
							long diff = System.currentTimeMillis() - last;
							if (diff < pff[opcode]) {
								log.warn(I18n.get("log.03483d765cbc", this, pck.getClass().getSimpleName(), diff));
								switch (SecurityConfig.PFF_LEVEL) {
								case 1: // disconnect
									return false;
								case 2:
									break;
								}
							} else
								this.pffRequests[opcode] = System.currentTimeMillis();
						}
					}
				}
			}

			GameServerNetworkServices.packetLoggerService().logPacketCM(pck.getPacketName());

			if (pck.read()) {
				packetProcessor.executePacket(pck);
			}
		}
		return true;
	}

	/**
	 * 由传输帧处理器调用：从发送队列写入一个服务端包，直到返回 false。
	 * Called by the transport frame handler; writes one server packet until false.
	 *
	 * @param data 写出缓冲区 / write buffer
	 * @return true 已写入；false 表示无更多数据 / true if written, false if queue empty
	 */
	@Override
	protected final boolean writeData(ByteBuffer data) {
		synchronized (guard) {
			final long begin = System.nanoTime();
			if (sendMsgQueue.isEmpty()) {
				return false;
			}
			AionServerPacket packet = sendMsgQueue.removeFirst();
			GameServerNetworkServices.packetLoggerService().logPacketSM(packet.getPacketName());
			try {
				packet.write(this, data);
				return true;
			} finally {
				RunnableStatsManager.handleStats(packet.getClass(), "runImpl()", System.nanoTime() - begin);
			}
		}
	}

	/**
	 * 连接准备关闭时由传输调用。
	 * Called by the transport when the connection is ready to be closed.
	 *
	 * @return 调用 onDisconnect 前的延迟毫秒数，本实现恒为 0 / delay before onDisconnect, always 0
	 */
	@Override
	protected final long getDisconnectionDelay() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onDisconnect() {
		/**
		 * 客户端开始认证流程时的清理。
		 * disconnects.
		 */
		pingChecker.stop();
		if (getAccount() != null) {
			com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().aionClientDisconnected(getAccount().getId());
			com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_MAC(getAccount().getId(), macAddress));
		}

		Player player = getActivePlayer();

		if (player != null) {
			PlayerLeaveWorldService.tryLeaveWorld(player);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onServerClose() {
		close(/* packet, */true);
	}

	/**
	 * 加密缓冲区中的服务端包。
	 * Encrypts the server packet buffer.
	 *
	 * @param buf 待加密缓冲区 / buffer to encrypt
	 */
	public final void encrypt(ByteBuffer buf) {
		crypt.encrypt(buf);
	}

	/**
	 * 向本客户端发送服务端包。
	 * Sends an AionServerPacket to this client.
	 *
	 * @param bp 待发送包 / packet to send
	 */
	public final void sendPacket(AionServerPacket bp) {
		synchronized (guard) {
			/**
			 * 连接已关闭或正在等待最后关闭包
			 * Connection already closed or waiting for last (close) packet
			 */
			if (isWriteDisabled()) {
				return;
			}
			sendMsgQueue.addLast(bp);
			enableWriteInterest();
		}
	}

	/**
	 * 保证 closePacket 在关闭前发出，此前/此后的包均不会发送。
	 * 连接由传输关闭，并调用 onDisconnect 清理。forced 在本实现无实际效果。
	 * Guarantees closePacket is sent before closing; past and future packets are dropped.
	 * Connection is closed by the transport and onDisconnect cleans up. forced has no effect here.
	 *
	 * @param closePacket 关闭前发送的包 / packet sent before closing
	 * @param forced 本实现中无效果 / has no effect in this implementation
	 */
	public final void close(AionServerPacket closePacket, boolean forced) {
		synchronized (guard) {
			if (isWriteDisabled()) {
				return;
			}
			pendingClose = true;
			isForcedClosing = forced;
			sendMsgQueue.clear();
			sendMsgQueue.addLast(closePacket);
			enableWriteInterest();
		}
	}

	/**
	 * 获取当前连接状态。
	 * Returns the current state of this connection.
	 *
	 * state
	 */
	public final State getState() {
		return state;
	}

	/**
	 * 设置连接状态。
	 * Sets the state of this connection.
	 *
	 * @param state 连接状态 / state
	 */
	public void setState(State state) {
		this.state = state;
	}

	/**
	 * 返回与本连接关联的账号。
	 * Returns the account associated with this connection.
	 *
	 * account object
	 */
	public Account getAccount() {
		return account;
	}

	/**
	 * 设置与本连接关联的账号（不可为 null）。
	 * Sets the account associated with this connection (must not be null).
	 *
	 * account object
	 */
	public void setAccount(Account account) {
		Preconditions.checkArgument(account != null, "Account can't be null");
		this.account = account;
	}

	/**
	 * 设置当前在线玩家并同步连接状态。
	 * Sets the active player and updates connection state accordingly.
	 *
	 * @param player 玩家，null 表示离开世界 / player, null means leave world
	 * @return 是否成功设置 / true if active player was set
	 */
	public boolean setActivePlayer(Player player) {
		if (player == null) {
			activePlayer.set(player);
			setState(State.AUTHED);
		} else if (activePlayer.compareAndSet(null, player)) {
			setState(State.IN_GAME);
			lastPlayerName = player.getName();
		} else {
			return false;
		}
		return true;
	}

	/**
	 * 返回当前在线玩家，可能为 null。
	 * Returns the active player or null.
	 *
	 * @return 在线玩家或 null / active player or null
	 */
	public Player getActivePlayer() {
		return activePlayer.get();
	}

	/**
	 * 获取最近一次 Ping 时间（毫秒）。
	 * Returns the last ping time in milliseconds.
	 *
	 * last ping time ms
	 */
	public long getLastPingTimeMS() {
		return lastPingTimeMS;
	}

	/**
	 * 设置最近一次 Ping 时间（毫秒）。
	 * Sets the last ping time in milliseconds.
	 *
	 * last ping time ms
	 */
	public void setLastPingTimeMS(long lastPingTimeMS) {
		this.lastPingTimeMS = lastPingTimeMS;
	}

	/**
	 * 立即关闭连接。
	 * Closes the connection immediately.
	 */
	public void closeNow() {
		this.close(false);
	}

	/**
	 * 设置客户端 MAC 地址。
	 * Sets the client MAC address.
	 *
	 * MAC address
	 */
	public void setMacAddress(String mac) {
		this.macAddress = mac;
	}

	/**
	 * 获取客户端 MAC 地址。
	 * Returns the client MAC address.
	 *
	 * MAC address
	 */
	public String getMacAddress() {
		return macAddress;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		Player player = activePlayer.get();
		if (player != null) {
			return "AionConnection [state=" + state + ", account=" + account + ", getObjectId()=" + player.getObjectId()
					+ ", lastPlayerName=" + lastPlayerName + ", macAddress=" + macAddress + ", getIP()=" + getIP()
					+ "]";
		}
		return "";
	}

	/**
	 * 定期检测无 Ping 的挂死连接并关闭。
	 * Periodically detects hanged-up connections without ping and closes them.
	 */
	private class PingChecker implements Runnable {

		// 不必立即检测挂起连接 / we don't have to detect hanged connections immediately
		// 极少见情况，10 分钟检查应足够。 / its rather some very rare case so 10 minutes check should be enough
		private static final int checkTime = 10 * 60 * 1000;
		private ScheduledFuture<?> task;
		private boolean started;

		private void start() {
			Preconditions.checkState(!started, "PingChecker can be started only one time!");
			started = true;
			task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(this, checkTime, checkTime);
		}

		private void stop() {
			task.cancel(false);
		}

		@Override
		public void run() {
			if (System.currentTimeMillis() - getLastPingTimeMS() > checkTime) {
				log.info(I18n.get("log.8bd98b94e34d", AionConnection.this));
				closeNow();
			}
		}
	}
}
