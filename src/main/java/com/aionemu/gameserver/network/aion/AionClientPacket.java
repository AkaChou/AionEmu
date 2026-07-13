package com.aionemu.gameserver.network.aion;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.EnumSet;
import java.util.Set;

import com.aionemu.commons.network.packet.BaseClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * Aion 客户端包基类：校验连接状态后执行业务逻辑。
 * Base class for Aion client packets; runs business logic only when connection state is valid.
 *
 * @author -Nemesiss-
 */
@Slf4j
public abstract class AionClientPacket extends BaseClientPacket<AionConnection> implements Cloneable {


	/** 允许处理本包的连接状态集合 / connection states valid for this packet */
	private final Set<State> validStates;

	/**
	 * 构造客户端包原型；缓冲区与连接需稍后手动设置。
	 * Constructs a client packet prototype; buffer and connection are set later manually.
	 *
	 * packet id
	 * @param state 合法连接状态 / valid connection state
	 * @param restStates 其余合法状态（可选） / additional valid states (optional)
	 */
	protected AionClientPacket(int opcode, State state, State... restStates) {
		super(opcode);
		validStates = EnumSet.of(state, restStates);
	}

	/**
	 * 在捕获并记录异常的前提下执行 runImpl。
	 * Runs runImpl while catching and logging any Throwable.
	 */
	@Override
	public final void run() {

		try {
			// 仅当包仍合法时运行（连接状态未变）
			// 仅在数据包仍有效时运行（连接状态未变） / run only if packet is still valid (connection state didn't change)
			if (isValid()) {
				runImpl();
			}
		} catch (Throwable e) {
			String name = getConnection().getAccount().getName();
			if (name == null) {
				name = getConnection().getIP();
			}
			log.error(I18n.get("log.ec7e1a30c38c", name, this), e);
		}
	}

	/**
	 * 向本包所属连接发送服务端包，等价于 getConnection().sendPacket(msg)。
	 * Sends an AionServerPacket to this packet's owner connection.
	 *
	 * @param msg 服务端包 / server packet
	 */
	protected void sendPacket(AionServerPacket msg) {
		getConnection().sendPacket(msg);
	}

	/**
	 * 克隆本包对象（用于原型模式分发）。
	 * Clones this packet object (prototype dispatch).
	 *
	 * @return 克隆实例，失败返回 null / clone, or null on failure
	 */
	public AionClientPacket clonePacket() {
		try {
			return (AionClientPacket) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	/**
	 * 读取定长 UTF-16 字符串字段（含填充）。
	 * Reads a fixed-size UTF-16 string field (including padding).
	 *
	 * @param size 字段总字节数 / total field size in bytes
	 * @return 读取到的字符串 / read string
	 */
	protected final String readS(int size) {
		String string = readS();
		if (string != null) {
			readB(size - (string.length() * 2 + 2));
		} else {
			readB(size);
		}
		return string;
	}

	/**
	 * 检查包对当前连接状态是否仍有效。
	 * Checks whether the packet is still valid for its connection state.
	 *
	 * @return 是否应继续处理 / true if the packet should be processed
	 */
	public final boolean isValid() {
		State state = getConnection().getState();
		boolean valid = validStates.contains(state);

		if (!valid) {
			log.debug(this + " wont be processed cuz its valid state don't match current connection state: " + state);
		}
		return valid;
	}
}
