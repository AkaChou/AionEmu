package com.aionemu.gameserver.network.chatserver;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.network.chatserver.ChatServerConnection.State;

/**
 * 聊天服入站包分发器：按连接状态与操作码查找并克隆包原型。
 * Inbound chat-server packet dispatcher: looks up and clones prototypes by state and opcode.
 */
@Slf4j
public class CsPacketHandler {
	/**
	 * 按状态分组的操作码 → 包原型映射。
	 * Opcode-to-prototype map grouped by connection state.
	 */
	private Map<State, Map<Integer, CsClientPacket>> packetPrototypes = new HashMap<State, Map<Integer, CsClientPacket>>();

	/**
	 * 从缓冲中读取操作码并构造对应入站包。
	 * Reads the opcode from the buffer and builds the matching inbound packet.
	 *
	 * @param data 包数据 / packet data
	 * @param client 所属连接 / owning connection
	 * @return 入站包实例，未知操作码时为 null / inbound packet, or null for unknown opcode
	 */
	public CsClientPacket handle(ByteBuffer data, ChatServerConnection client) {
		State state = client.getState();
		int id = data.get() & 0xff;
		return getPacket(state, id, data, client);
	}

	/**
	 * 为指定状态注册包原型。
	 * Registers a packet prototype for the given states.
	 *
	 * @param packetPrototype 包原型 / packet prototype
	 * @param states 适用连接状态 / applicable connection states
	 */
	public void addPacketPrototype(CsClientPacket packetPrototype, State... states) {
		for (State state : states) {
			Map<Integer, CsClientPacket> pm = packetPrototypes.get(state);
			if (pm == null) {
				pm = new HashMap<Integer, CsClientPacket>();
				packetPrototypes.put(state, pm);
			}
			pm.put(packetPrototype.getOpcode(), packetPrototype);
		}
	}

	/**
	 * 按状态与操作码查找原型，克隆并绑定缓冲与连接。
	 * Looks up a prototype by state and opcode, then clones and binds buffer and connection.
	 *
	 * @param state 连接状态 / connection state
	 * @param id 操作码 / opcode
	 * @param buf 数据缓冲 / data buffer
	 * @param con 所属连接 / owning connection
	 * @return 就绪的入站包，或 null / ready inbound packet, or null
	 */
	private CsClientPacket getPacket(State state, int id, ByteBuffer buf, ChatServerConnection con) {
		CsClientPacket prototype = null;
		Map<Integer, CsClientPacket> pm = packetPrototypes.get(state);
		if (pm != null) {
			prototype = pm.get(id);
		}
		if (prototype == null) {
			unknownPacket(state, id);
			return null;
		}
		CsClientPacket res = prototype.clonePacket();
		res.setBuffer(buf);
		res.setConnection(con);
		return res;
	}

	/**
	 * 记录未知入站包。
	 * Logs an unknown inbound packet.
	 *
	 * @param state 连接状态 / connection state
	 * @param id 操作码 / opcode
	 */
	private void unknownPacket(State state, int id) {
			log.warn(I18n.get("log.7b9610795a80", String.format("%02X", id), state));
	}
}
