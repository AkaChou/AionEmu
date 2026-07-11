package com.aionemu.gameserver.network.loginserver;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameServerNetworkServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.gameserver.network.factories.LsPacketHandlerFactory;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_GS_AUTH;

/**
 * 表示游戏服与登录服之间的一条网络连接。
 * Object representing a connection between LoginServer and GameServer.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class LoginServerConnection extends AConnection {


	/**
	 * 登录服连接可能的状态。
	 * Possible states of a LoginServer connection.
	 */
	public static enum State {
		/**
		 * 已连接但尚未认证。
		 * Connected but not yet authenticated.
		 */
		CONNECTED,
		/**
		 * 游戏服已通过认证。
		 * GameServer is authenticated.
		 */
		AUTHED
	}

	/**
	 * 待发送服务端封包队列。
	 * Queue of server packets waiting to be sent.
	 */
	private final Deque<LsServerPacket> sendMsgQueue = new ArrayDeque<LsServerPacket>();

	/**
	 * 当前连接状态。
	 * Current state of this connection.
	 */
	private State state;

	/**
	 * LS 封包处理器。
	 * LS packet handler.
	 */
	private LsPacketHandler lsPacketHandler;

	/**
	 * 基于传输层创建登录服连接。
	 * Create a LoginServer connection over the given transport.
	 *
	 * Connection transport
	 */
	public LoginServerConnection(ConnectionTransport transport) {
		super(transport, 8192 * 8, 8192 * 8);
		init();
	}

	/**
	 * 初始化封包处理器与连接状态，并记录连接日志。
	 * Initialize packet handler and connection state, then log the connect event.
	 */
	private void init() {
		LsPacketHandlerFactory lsPacketHandlerFactory = GameServerNetworkServices.lsPacketHandlerFactory();
		this.lsPacketHandler = lsPacketHandlerFactory.getPacketHandler();
		state = State.CONNECTED;
		log.info(I18n.get("log.344132a769b8"));
	}

	/**
	 * 连接初始化完成后发送首个认证包。
	 * After connection is initialized, send the first authentication packet.
	 */
	@Override
	protected void initialized() {
		/**
		 * 发送首个封包 —— 游戏服认证。
		 * Send first packet — GameServer authentication.
		 */
		this.sendPacket(new SM_GS_AUTH());
	}

	/**
	 * 由传输层帧处理器调用；缓冲区中包含一个待处理封包。
	 * Called by the transport frame handler; buffer holds one packet to process.
	 *
	 * @param data 封包数据 / Packet data
	 * @return 是否处理成功；失败时应立即关闭连接 / True if processed OK; false to close connection now
	 */
	@Override
	public boolean processData(ByteBuffer data) {
		LsClientPacket pck = lsPacketHandler.handle(data, this);
		log.debug("received packet: " + pck);

		/**
		 * 仅当封包存在且读取成功时才执行。
		 * Execute packet only if it exists and was read successfully.
		 */
		if (pck != null && pck.read()) {
			GameThreadPoolServices.threadPoolManager().executeLsPacket(pck);
		}
		return true;
	}

	/**
	 * 由传输层反复调用直至返回 false，用于写出下一个待发封包。
	 * Called repeatedly by the transport until false; writes the next pending packet.
	 *
	 * @param data 输出缓冲区 / Output buffer
	 * @return 是否写入了数据；false 表示无更多数据 / True if data was written; false if nothing left
	 */
	@Override
	protected final boolean writeData(ByteBuffer data) {
		synchronized (guard) {
			LsServerPacket packet = sendMsgQueue.pollFirst();
			if (packet == null) {
				return false;
			}
			packet.write(this, data);
			return true;
		}
	}

	/**
	 * 传输层准备关闭连接时调用，返回 onDisconnect 延迟毫秒数。
	 * Called by the transport when the connection is ready to close; returns delay before onDisconnect().
	 *
	 * @return 延迟毫秒，始终为 0 / Delay in ms; always 0
	 */
	@Override
	protected final long getDisconnectionDelay() {
		return 0;
	}

	/**
	 * 连接断开时通知 LoginServer 门面处理断线逻辑。
	 * On disconnect, notify the LoginServer facade to handle the down event.
	 */
	@Override
	protected final void onDisconnect() {
		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().loginServerDown();
	}

	/**
	 * 服务端主动关闭时的回调（本实现直接强制关闭）。
	 * Callback when the server side initiates close (this implementation force-closes immediately).
	 */
	@Override
	protected final void onServerClose() {
		close(/* packet, */true);
	}

	/**
	 * 向登录服发送服务端封包。
	 * Send an LS server packet to the LoginServer.
	 *
	 * @param bp 待发送的服务端封包 / Server packet to send
	 */
	public final void sendPacket(LsServerPacket bp) {
		synchronized (guard) {
			/**
			 * 连接已关闭或正在等待最后关闭包发送时忽略。
			 * Ignore when connection is already closed or waiting for the last close packet.
			 */
			if (isWriteDisabled()) {
				return;
			}
			log.debug("sending packet: " + bp);

			sendMsgQueue.addLast(bp);
			enableWriteInterest();
		}
	}

	/**
	 * 保证 closePacket 在关闭前发出，之后清空队列；forced 在本实现中无效果。
	 * Guarantees closePacket is sent before closing; clears past/future packets. forced has no effect here.
	 *
	 * @param closePacket 关闭前发送的封包 / Packet sent before closing
	 * @param forced 强制关闭标志（本实现忽略） / Forced close flag (ignored in this implementation)
	 */
	public final void close(LsServerPacket closePacket, boolean forced) {
		synchronized (guard) {
			if (isWriteDisabled()) {
				return;
			}
			log.debug("sending packet: " + closePacket + " and closing connection after that.");

			pendingClose = true;
			isForcedClosing = forced;
			sendMsgQueue.clear();
			sendMsgQueue.addLast(closePacket);
			enableWriteInterest();
		}
	}

	/**
	 * 返回当前连接状态。
	 * Returns the current connection state.
	 *
	 * Current state
	 */
	public State getState() {
		return state;
	}

	/**
	 * 设置当前连接状态。
	 * Sets the current connection state.
	 *
	 * New state
	 */
	public void setState(State state) {
		this.state = state;
	}

	/**
	 * 返回本连接的字符串描述。
	 * Returns a string description of this connection.
	 *
	 * Connection info
	 */
	@Override
	public String toString() {
		return "LoginServer " + getIP();
	}
}
