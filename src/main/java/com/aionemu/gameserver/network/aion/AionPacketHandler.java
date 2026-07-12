package com.aionemu.gameserver.network.aion;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.configs.administration.DeveloperConfig;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;

/**
 * Aion 客户端包分发器：按 opcode 查找原型、克隆并绑定连接与缓冲区。
 * Aion client packet dispatcher: looks up prototypes by opcode, clones and binds connection/buffer.
 */
@Slf4j
public class AionPacketHandler {

	/** 操作码到客户端包原型 / opcode to client packet prototype */
	private Map<Integer, AionClientPacket> packetsPrototypes = new HashMap<Integer, AionClientPacket>();

	/**
	 * 从给定 ByteBuffer 读取一个包。
	 * Reads one packet from the given ByteBuffer.
	 *
	 * packet data
	 *
	 * @param client 客户端连接 / client connection
	 * @param client
	 * @return 解析出的客户端包，未知包返回 null / client packet, or null if unknown
	 */
	public AionClientPacket handle(ByteBuffer data, AionConnection client) {
		State state = client.getState();
		int id = data.getShort() & 0xffff;
		/* Second opcodec. */
		data.position(data.position() + 3);

		return getPacket(state, id, data, client);
	}

	/**
	 * 注册客户端包原型。
	 * Registers a client packet prototype.
	 *
	 * packet prototype
	 */
	public void addPacketPrototype(AionClientPacket packetPrototype) {
		AionClientPacket previous = packetsPrototypes.putIfAbsent(packetPrototype.getOpcode(), packetPrototype);
		if (previous != null)
			throw new IllegalArgumentException(String.format("Duplicate opcode 0x%04X: %s and %s", packetPrototype.getOpcode(),
					previous.getPacketName(), packetPrototype.getPacketName()));
	}

	/**
	 * 按 opcode 获取并克隆包实例；可选在聊天中展示包名/十六进制。
	 * Resolves and clones a packet by opcode; optionally shows name/hex in chat.
	 *
	 * @param state 连接状态 / connection state
	 * @param id opcode
	 * @param buf 包缓冲区 / packet buffer
	 * connection
	 * packet instance or null
	 */
	private AionClientPacket getPacket(State state, int id, ByteBuffer buf, AionConnection con) {
		AionClientPacket prototype = packetsPrototypes.get(id);

		if (prototype == null) {
			unknownPacket(state, id, buf);
			return null;
		}

		/**
		 * 在聊天窗口展示包名与十六进制字节
		 * Display packet name + hex bytes in chat window
		 */
		Player player = con.getActivePlayer();

		if (con.getState().equals(State.IN_GAME) && player != null
				&& player.getAccessLevel() >= DeveloperConfig.SHOW_PACKETS_INCHAT_ACCESSLEVEL) {
			if (isPacketFilterd(DeveloperConfig.FILTERED_PACKETS_INCHAT, prototype.getPacketName())) {
				if (DeveloperConfig.SHOW_PACKET_BYTES_INCHAT) {
					String PckName = String.format("0x%04X : %s", id, prototype.getPacketName());
					PacketSendUtility.sendMessage(player, "********************************************");
					PacketSendUtility.sendMessage(player, PckName);
					PacketSendUtility.sendMessage(player,
							Util.toHexStream(getByteBuffer(buf, DeveloperConfig.TOTAL_PACKET_BYTES_INCHAT)));
					buf.position(5);

				} else if (DeveloperConfig.SHOW_PACKET_NAMES_INCHAT) {
					String PckName = String.format("0x%04X : %s", id, prototype.getPacketName());
					PacketSendUtility.sendMessage(player, PckName);
				}
			}
		}
		AionClientPacket res = prototype.clonePacket();
		res.setBuffer(buf);
		res.setConnection(con);

		if (con.getState().equals(State.IN_GAME) && con.getActivePlayer().getPlayerAccount().getMembership() == 10) {
			PacketSendUtility.sendMessage(con.getActivePlayer(),
					"0x" + Integer.toHexString(res.getOpcode()).toUpperCase() + " : " + res.getPacketName());
		}
		return res;
	}

	/**
	 * 判断包名是否命中聊天展示过滤列表（* 或空表示全部）。
	 * Whether the packet name matches the chat display filter (* or empty = all).
	 *
	 * @param filterlist 逗号分隔过滤列表 / comma-separated filter list
	 * packet name
	 *
	 * @return 若 shown 则为 true / true if shown
	 */
	private boolean isPacketFilterd(String filterlist, String PacketName) {

		// 若 FilterList 为空，将显示全部数据包。 / If FilterList was empty, all packets will be shown.
		if (filterlist == null || filterlist.equalsIgnoreCase("*")) {
			return true;
		}
		String[] Parts = null;
		Parts = filterlist.trim().split(",");

		for (String p : Parts) {
			if (p.trim().equalsIgnoreCase(PacketName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 从缓冲区复制前 count 字节（从偏移 5 起）用于调试展示。
	 * Copies the first count bytes from the buffer (from offset 5) for debug display.
	 *
	 * @param buf 源缓冲区 / source buffer
	 * byte count
	 * copy buffer
	 */
	private ByteBuffer getByteBuffer(ByteBuffer buf, int count) {

		count = (count <= buf.capacity()) ? count : buf.capacity();
		ByteBuffer tmpBuffer = buf.asReadOnlyBuffer();
		tmpBuffer.position(5);
		tmpBuffer.limit(count);

		// 按请求容量创建空 ByteBuffer。 / Create an empty ByteBuffer with a Requested Capacity.
		ByteBuffer PckBuffer = ByteBuffer.allocate(count);
		try {
			do {
				PckBuffer.put(tmpBuffer.get());
			} while (tmpBuffer.remaining() > 0);
		} catch (Exception e) {
			log.warn(I18n.get("log.142161450d0f", e));
		}
		PckBuffer.position(0);
		return PckBuffer;
	}

	/**
	 * 记录未知客户端包。
	 * Logs an unknown client packet.
	 *
	 * @param state 连接状态 / connection state
	 * @param id opcode
	 * packet data
	 */
	private void unknownPacket(State state, int id, ByteBuffer data) {
		if (NetworkConfig.DISPLAY_UNKNOWNPACKETS) {
			log.warn(I18n.get("log.e672a6931c69", String.format("%04X", id), state, Util.toHex(data)));
		}
	}
}
