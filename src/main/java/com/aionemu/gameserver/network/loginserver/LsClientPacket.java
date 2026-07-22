package com.aionemu.gameserver.network.loginserver;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.network.packet.BaseClientPacket;

/**
 * 所有登录服 → 游戏服客户端封包的基类。
 * Base class for every LoginServer → GameServer client packet.
 *
 * @author -Nemesiss-
 */
@Slf4j
public abstract class LsClientPacket extends BaseClientPacket<LoginServerConnection> implements Cloneable {


	/**
	 * 以指定 opcode 构造客户端封包；之后需手动设置 buffer 与 connection。
	 * Construct client packet with the given opcode; buffer and connection must be set later.
	 *
	 * Packet opcode
	 */
	protected LsClientPacket(int opcode) {
		super(opcode);
	}

	/**
	 * 执行 {@link #runImpl()} 并捕获、记录异常。
	 * Run {@link #runImpl()} while catching and logging any throwable.
	 */
	@Override
	public final void run() {
		try {
			runImpl();
		} catch (Throwable e) {
			log.warn(I18n.get("log.8f589268f23f", getConnection().getIP(), this, e), e);
		}
	}

	/**
	 * 向本封包所属连接发送服务端封包，等价于 {@code getConnection().sendPacket(msg)}。
	 * Send a server packet to the owning connection; equivalent to {@code getConnection().sendPacket(msg)}.
	 *
	 * @param msg 待发送的服务端封包 / Server packet to send
	 */
	protected void sendPacket(LsServerPacket msg) {
		getConnection().sendPacket(msg);
	}

	/**
	 * 克隆本封包对象（原型模式，供处理器实例化）。
	 * Clone this packet object (prototype pattern for handler instantiation).
	 *
	 * @return 克隆后的 LsClientPacket；失败时返回 null / Cloned LsClientPacket, or null on failure
	 */
	public LsClientPacket clonePacket() {
		try {
			return (LsClientPacket) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}
