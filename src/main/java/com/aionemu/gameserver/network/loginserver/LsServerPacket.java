package com.aionemu.gameserver.network.loginserver;

import java.nio.ByteBuffer;

import com.aionemu.commons.network.packet.BaseServerPacket;

/**
 * 所有游戏服 → 登录服服务端封包的基类。
 * Base class for every GameServer → LoginServer server packet.
 *
 * @author -Nemesiss-
 */
public abstract class LsServerPacket extends BaseServerPacket {

	/**
	 * 以指定 opcode 构造服务端封包。
	 * Construct server packet with the given opcode.
	 *
	 * Packet opcode
	 */
	protected LsServerPacket(int opcode) {
		super(opcode);
	}

	/**
	 * 将本封包写入指定连接的缓冲区（含长度前缀与 opcode）。
	 * Write this packet into the buffer for the given connection (including length prefix and opcode).
	 *
	 * @param con 目标登录服连接 / Target LoginServer connection
	 * @param buffer 输出缓冲区 / Output buffer
	 */
	public final synchronized void write(LoginServerConnection con, ByteBuffer buffer) {
		setBuf(buffer);
		buf.putShort((short) 0);
		buf.put((byte) this.getOpcode());
		writeImpl(con);
		buf.flip();
		buf.putShort((short) buf.limit());
		buf.position(0);
	}

	/**
	 * 将封包载荷写入缓冲区（子类实现）。
	 * Write packet payload into the buffer (implemented by subclasses).
	 *
	 * @param con 目标登录服连接 / Target LoginServer connection
	 */
	protected abstract void writeImpl(LoginServerConnection con);
}
