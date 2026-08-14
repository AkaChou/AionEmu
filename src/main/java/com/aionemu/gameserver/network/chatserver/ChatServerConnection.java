package com.aionemu.gameserver.network.chatserver;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.gameserver.network.chatserver.serverpackets.SM_CS_AUTH;

/**
 * 游戏服与聊天服之间的网络连接。
 * Network connection between the game server and the chat server.
 */
@Slf4j
public class ChatServerConnection extends AConnection {

	/**
	 * 聊天服连接状态。
	 * Chat-server connection state.
	 */
	public static enum State {
		/**
		 * 已连接但尚未认证。
		 * Connected but not yet authenticated.
		 */
		CONNECTED,
		/**
		 * 已通过认证。
		 * Authenticated successfully.
		 */
		AUTHED
	}

	/**
	 * 待发送服务端包队列。
	 * Outbound server-packet queue.
	 */
	private final Deque<CsServerPacket> sendMsgQueue = new ArrayDeque<CsServerPacket>();

	/**
	 * 当前连接状态。
	 * Current connection state.
	 */
	private State state;

	/**
	 * 所属 ChatServer 门面。
	 * Owning ChatServer facade.
	 */
	private ChatServer chatServer;

	/**
	 * 入站包处理器。
	 * Inbound packet handler.
	 */
	private CsPacketHandler csPacketHandler;

	/**
	 * 基于传输层创建聊天服连接。
	 * Creates a chat-server connection on the given transport.
	 *
	 * @param transport 传输 / transport
	 * @param csPacketHandler 入站包处理器 / inbound packet handler
	 */
	public ChatServerConnection(ConnectionTransport transport, CsPacketHandler csPacketHandler) {
		super(transport, 8192 * 2, 8192 * 2);
		init(csPacketHandler);
	}

	/**
	 * 初始化门面引用、处理器与初始状态。
	 * Initializes facade reference, handler, and initial state.
	 *
	 * @param csPacketHandler 入站包处理器 / inbound packet handler
	 */
	private void init(CsPacketHandler csPacketHandler) {
		this.chatServer = com.aionemu.gameserver.lifecycle.GameServerNetworkServices.chatServer();
		this.csPacketHandler = csPacketHandler;
		state = State.CONNECTED;
		log.info(I18n.get("log.6edc59d72a57"));
	}

	/**
	 * 连接初始化完成后发送认证包。
	 * Sends the authentication packet after the connection is initialized.
	 */
	@Override
	protected void initialized() {
		this.sendPacket(new SM_CS_AUTH());
	}

	/**
	 * 解析并投递一个入站包到线程池。
	 * Parses and dispatches one inbound packet to the thread pool.
	 *
	 * @param data 包数据缓冲 / packet data buffer
	 * @return 始终为 true（由上层决定是否关闭） / always true (upper layer decides close)
	 */
	@Override
	public boolean processData(ByteBuffer data) {
		CsClientPacket pck = csPacketHandler.handle(data, this);
		if (pck != null && pck.read()) {
			GameThreadPoolServices.threadPoolManager().executeLsPacket(pck);
		}
		return true;
	}

	/**
	 * 从发送队列取出一个包写入缓冲。
	 * Dequeues one packet and writes it into the buffer.
	 *
	 * @param data 写出缓冲 / write buffer
	 * @return 是否写出了数据 / whether data was written
	 */
	@Override
	protected final boolean writeData(ByteBuffer data) {
		synchronized (guard) {
			CsServerPacket packet = sendMsgQueue.pollFirst();
			if (packet == null) {
				return false;
			}
			packet.write(this, data);
			return true;
		}
	}

	/**
	 * 返回断开延迟（聊天服连接立即断开）。
	 * Returns disconnection delay (immediate for chat-server connections).
	 *
	 * @return 0
	 */
	@Override
	protected final long getDisconnectionDelay() {
		return 0;
	}

	/**
	 * 连接断开时通知 ChatServer 门面。
	 * Notifies the ChatServer facade when the connection drops.
	 */
	@Override
	protected final void onDisconnect() {
		chatServer.chatServerDown();
	}

	/**
	 * 服务器关闭时强制关闭本连接。
	 * Force-closes this connection when the server is shutting down.
	 */
	@Override
	protected final void onServerClose() {
		close(/* packet, */true);
	}

	/**
	 * 将服务端包加入发送队列并触发写兴趣。
	 * Enqueues a server packet and enables write interest.
	 *
	 * @param bp 待发送包 / packet to send
	 */
	public final void sendPacket(CsServerPacket bp) {
		synchronized (guard) {
			if (isWriteDisabled()) {
				return;
			}
			sendMsgQueue.addLast(bp);
			enableWriteInterest();
		}
	}

	/**
	 * 清空发送队列后以指定包关闭连接。
	 * Clears the send queue and closes the connection with the given packet.
	 *
	 * @param closePacket 关闭前最后发送的包 / last packet before close
	 * @param forced 是否强制关闭 / whether forced close
	 */
	public final void close(CsServerPacket closePacket, boolean forced) {
		synchronized (guard) {
			if (isWriteDisabled()) {
				return;
			}
			log.info(I18n.get("log.38bd4ad41d74", closePacket));
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
	 * @return 连接状态 / connection state
	 */
	public State getState() {
		return state;
	}

	/**
	 * 设置连接状态。
	 * Sets the connection state.
	 *
	 * @param state 新状态 / new state
	 */
	public void setState(State state) {
		this.state = state;
	}

	/**
	 * 返回连接的可读描述。
	 * Returns a human-readable description of this connection.
	 *
	 * @return 描述字符串 / description string
	 */
	@Override
	public String toString() {
		return "ChatServer " + getIP();
	}
}
