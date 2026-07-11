package com.aionemu.gameserver.network.chatserver;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.network.packet.BaseClientPacket;

/**
 * 聊天服入站客户端包基类。
 * Base class for inbound client packets from the chat server.
 */
@Slf4j
public abstract class CsClientPacket extends BaseClientPacket<ChatServerConnection> implements Cloneable {

	/**
	 * 使用指定操作码构造客户端包。
	 * Constructs a client packet with the given opcode.
	 *
	 * packet opcode
	 */
	protected CsClientPacket(int opcode) {
		super(opcode);
	}

	/**
	 * 执行业务逻辑并捕获、记录所有异常。
	 * Runs business logic while catching and logging any throwable.
	 */
	@Override
	public final void run() {
		try {
			runImpl();
		} catch (Throwable e) {
			log.warn(I18n.get("log.8f589268f23f", getConnection().getIP(), this, e));
		}
	}

	/**
	 * 向所属连接发送一个服务端包。
	 * Sends a server packet on the owning connection.
	 *
	 * @param msg 待发送包 / packet to send
	 */
	protected void sendPacket(CsServerPacket msg) {
		getConnection().sendPacket(msg);
	}

	/**
	 * 克隆本包原型以供处理器复用。
	 * Clones this packet prototype for handler reuse.
	 *
	 * @return 克隆实例，失败时返回 null / clone instance, or null on failure
	 */
	public CsClientPacket clonePacket() {
		try {
			return (CsClientPacket) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}
