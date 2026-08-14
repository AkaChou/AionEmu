package com.aionemu.gameserver.network.aion;

import lombok.extern.slf4j.Slf4j;
import java.nio.ByteBuffer;

import com.aionemu.commons.network.packet.BaseServerPacket;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.network.Crypt;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 游戏服 → Aion 客户端服务端包基类：写操作码、载荷并加密。
 * Base class for GS → Aion server packets: writes opcode, payload and encrypts.
 *
 * @author -Nemesiss-
 */
@Slf4j
public abstract class AionServerPacket extends BaseServerPacket {


	/**
	 * 构造服务端包，并从 opcode 表解析操作码。
	 * Constructs a server packet and resolves opcode from the opcode table.
	 */
	protected AionServerPacket() {
		super();
		setOpcode(ServerPacketsOpcodes.getOpcode(getClass()));
	}

	/**
	 * 写入混淆后的操作码及两个附加字节。
	 * Writes obfuscated opcode and two additional bytes.
	 *
	 * @param value 原始操作码 / raw opcode
	 */
	private final void writeOP(int value) {
		/** 混淆包 ID / obfuscate packet id */
		int op = Crypt.encodeOpcodec(value);
		buf.putShort((short) (op));
		/** 写入服务端静态码 / put static server packet code */
		buf.put(Crypt.staticServerPacketCode);
		/** 校验用反码 / for checksum? */
		buf.putShort((short) (~op));
	}

	/**
	 * 使用内部缓冲区向连接写入本包。
	 * Writes this packet to the connection using the internal buffer.
	 *
	 * @param con 目标连接 / target connection
	 */
	public final void write(AionConnection con) {
		write(con, buf);
	}

	/**
	 * 向给定缓冲区写入并加密本包数据。
	 * Writes and encrypts this packet data for the given connection into the buffer.
	 *
	 * @param con 目标连接 / target connection
	 * @param buffer 写出缓冲区 / write buffer
	 */
	public final synchronized void write(AionConnection con, ByteBuffer buffer) {
		if (con.getState().equals(AionConnection.State.IN_GAME)
				&& con.getActivePlayer().getPlayerAccount().getAccessLevel() == 5 && NetworkConfig.DISPLAY_PACKETS) {
			if (!this.getPacketName().equals("SM_MESSAGE")) {
				PacketSendUtility.sendMessage(con.getActivePlayer(),
						"0x" + Integer.toHexString(this.getOpcode()).toUpperCase() + " : " + this.getPacketName());
			}
		}
		this.setBuf(buffer);
		buf.putShort((short) 0);
		writeOP(getOpcode());
		writeImpl(con);
		buf.flip();
		buf.putShort((short) buf.limit());
		ByteBuffer b = buf.slice();
		buf.position(0);
		con.encrypt(b);
	}

	/**
	 * 将本包载荷写入内部缓冲区；子类覆盖实现。
	 * Writes this packet's payload into the internal buffer; subclasses override.
	 *
	 * @param con 目标连接 / target connection
	 */
	protected void writeImpl(AionConnection con) {

	}

	/**
	 * 获取内部写缓冲区。
	 * Returns the internal write buffer.
	 *
	 * @return 内部缓冲区 / buffer
	 */
	public final ByteBuffer getBuf() {
		return this.buf;
	}

	/**
	 * 写入定长字符串（不足部分补 0）。
	 * Writes a fixed-size string (zero-padded).
	 *
	 * @param text 文本 / text
	 * @param size 字段字节数 / field size in bytes
	 */
	protected final void writeS(String text, int size) {
		if (text == null) {
			buf.put(new byte[size]);
		} else {
			final int len = text.length();
			for (int i = 0; i < len; i++) {
				buf.putChar(text.charAt(i));
			}
			buf.put(new byte[size - (len * 2)]);
		}
	}

	/**
	 * 写入名称 ID 块（固定头 + nameId + 尾）。
	 * Writes a name-id block (fixed header + nameId + trailer).
	 *
	 * @param nameId 名称 ID / name id
	 */
	protected void writeNameId(int nameId) {
		writeH(0x24);
		writeD(nameId);
		writeH(0x00);
	}
}
