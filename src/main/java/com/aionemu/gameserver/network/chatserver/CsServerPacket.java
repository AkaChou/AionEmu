package com.aionemu.gameserver.network.chatserver;

import java.nio.ByteBuffer;

import com.aionemu.commons.network.packet.BaseServerPacket;

/**
 * 游戏服发往聊天服的服务端包基类。
 * Base class for server packets sent from the game server to the chat server.
 */
public abstract class CsServerPacket extends BaseServerPacket {
	/**
	 * 使用指定操作码构造服务端包。
	 * Constructs a server packet with the given opcode.
	 *
	 * @param opcode 包操作码 / packet opcode
	 */
	protected CsServerPacket(int opcode) {
		super(opcode);
	}

	/**
	 * 将本包写入给定连接的缓冲（含长度与操作码头）。
	 * Writes this packet into the given connection buffer (including length and opcode header).
	 *
	 * @param con 目标连接 / target connection
	 * @param buffer 写出缓冲 / write buffer
	 */
	public final synchronized void write(ChatServerConnection con, ByteBuffer buffer) {
		setBuf(buffer);
		buf.putShort((short) 0);
		buf.put((byte) getOpcode());
		writeImpl(con);
		buf.flip();
		buf.putShort((short) buf.limit());
		buf.position(0);
	}

	/**
	 * 写出包体数据。
	 * Writes the packet body.
	 *
	 * @param con 目标连接 / target connection
	 */
	protected abstract void writeImpl(ChatServerConnection con);
}
