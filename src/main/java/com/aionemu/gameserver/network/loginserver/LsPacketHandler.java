package com.aionemu.gameserver.network.loginserver;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.network.loginserver.LoginServerConnection.State;

/**
 * 登录服客户端封包处理器：按连接状态与 opcode 分发原型并克隆实例。
 * LoginServer client-packet handler: dispatches prototypes by connection state and opcode, then clones instances.
 *
 * @author -Nemesiss-
 * @author Luno
 */
@Slf4j
public class LsPacketHandler {


	/**
	 * 按连接状态索引的 opcode → 封包原型表。
	 * Map of connection state to (opcode → packet prototype).
	 */
	private static Map<State, Map<Integer, LsClientPacket>> packetPrototypes = new HashMap<State, Map<Integer, LsClientPacket>>();

	/**
	 * 从给定 ByteBuffer 读取并构造一个客户端封包。
	 * Read one client packet from the given ByteBuffer.
	 *
	 * @param data 封包数据 / Packet data
	 * @param client 登录服连接 / LoginServer connection
	 * @return 解析得到的 LsClientPacket；未知 opcode 时返回 null / Parsed LsClientPacket, or null for unknown opcode
	 */
	public LsClientPacket handle(ByteBuffer data, LoginServerConnection client) {
		State state = client.getState();
		int id = data.get() & 0xff;

		return getPacket(state, id, data, client);
	}

	/**
	 * 为指定连接状态注册封包原型。
	 * Register a packet prototype for the given connection states.
	 *
	 * Packet prototype
	 * @param states 适用的连接状态 / Applicable connection states
	 */
	public void addPacketPrototype(LsClientPacket packetPrototype, State... states) {
		for (State state : states) {
			Map<Integer, LsClientPacket> pm = packetPrototypes.get(state);
			if (pm == null) {
				pm = new HashMap<Integer, LsClientPacket>();
				packetPrototypes.put(state, pm);
			}
			pm.put(packetPrototype.getOpcode(), packetPrototype);
		}
	}

	/**
	 * 按状态与 opcode 查找原型，克隆并绑定 buffer/connection。
	 * Look up prototype by state and opcode, clone it, and bind buffer/connection.
	 *
	 * @param state 连接状态 / Connection state
	 * Packet opcode
	 * @param buf 数据缓冲区 / Data buffer
	 * @param con 登录服连接 / LoginServer connection
	 * @return 就绪的封包实例；未知时返回 null / Ready packet instance, or null if unknown
	 */
	private LsClientPacket getPacket(State state, int id, ByteBuffer buf, LoginServerConnection con) {
		LsClientPacket prototype = null;

		Map<Integer, LsClientPacket> pm = packetPrototypes.get(state);
		if (pm != null) {
			prototype = pm.get(id);
		}

		if (prototype == null) {
			unknownPacket(state, id);
			return null;
		}

		LsClientPacket res = prototype.clonePacket();
		res.setBuffer(buf);
		res.setConnection(con);

		return res;
	}

	/**
	 * 记录未知封包日志。
	 * Log an unknown packet.
	 *
	 * @param state 连接状态 / Connection state
	 * Packet opcode
	 */
	private void unknownPacket(State state, int id) {
			log.warn(I18n.get("log.16bfb32106d1", String.format("%02X", id), state));
	}
}
