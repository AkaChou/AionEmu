package com.aionemu.gameserver.network;

import java.nio.ByteBuffer;

/**
 * 包写入辅助基类，提供向 ByteBuffer 写入各基本类型的方法。
 * Packet write helper base providing typed write methods into a ByteBuffer.
 *
 * @author -Nemesiss-
 */
public abstract class PacketWriteHelper {

	/**
	 * 子类实现的实际写入逻辑。
	 * Subclass write implementation.
	 *
	 * @param buf 目标缓冲区 / target buffer
	 */
	protected abstract void writeMe(ByteBuffer buf);

	/**
	 * 写入 int（4 字节）。
	 * Writes an int (4 bytes).
	 *
	 * @param buf 目标缓冲区 / target buffer
	 * value
	 */
	protected final void writeD(ByteBuffer buf, int value) {
		buf.putInt(value);
	}

	/**
	 * 写入 short（2 字节）。
	 * Writes a short (2 bytes).
	 *
	 * @param buf 目标缓冲区 / target buffer
	 * value
	 */
	protected final void writeH(ByteBuffer buf, int value) {
		buf.putShort((short) value);
	}

	/**
	 * 写入 byte（1 字节）。
	 * Writes a byte (1 byte).
	 *
	 * @param buf 目标缓冲区 / target buffer
	 * value
	 */
	protected final void writeC(ByteBuffer buf, int value) {
		buf.put((byte) value);
	}

	/**
	 * 写入 double（8 字节）。
	 * Writes a double (8 bytes).
	 *
	 * @param buf 目标缓冲区 / target buffer
	 * value
	 */
	protected final void writeDF(ByteBuffer buf, double value) {
		buf.putDouble(value);
	}

	/**
	 * 写入 float（4 字节）。
	 * Writes a float (4 bytes).
	 *
	 * @param buf 目标缓冲区 / target buffer
	 * value
	 */
	protected final void writeF(ByteBuffer buf, float value) {
		buf.putFloat(value);
	}

	/**
	 * 写入 long（8 字节）。
	 * Writes a long (8 bytes).
	 *
	 * @param buf 目标缓冲区 / target buffer
	 * value
	 */
	protected final void writeQ(ByteBuffer buf, long value) {
		buf.putLong(value);
	}

	/**
	 * 写入以 null 结尾的 UTF-16 字符串。
	 * Writes a null-terminated UTF-16 string.
	 *
	 * @param buf 目标缓冲区 / target buffer
	 * @param text 文本，null 则写空串 / text; null writes empty
	 */
	protected final void writeS(ByteBuffer buf, String text) {
		if (text == null) {
			buf.putChar('\000');
		} else {
			final int len = text.length();
			for (int i = 0; i < len; i++) {
				buf.putChar(text.charAt(i));
			}
			buf.putChar('\000');
		}
	}

	/**
	 * 写入字节数组。
	 * Writes a byte array.
	 *
	 * @param buf 目标缓冲区 / target buffer
	 * @param data 字节数据 / byte data
	 */
	protected final void writeB(ByteBuffer buf, byte[] data) {
		buf.put(data);
	}

	/**
	 * 跳过（填充）指定字节数。
	 * Skips (zero-fills) the given number of bytes.
	 *
	 * @param buf 目标缓冲区 / target buffer
	 * byte count
	 */
	protected final void skip(ByteBuffer buf, int bytes) {
		buf.put(new byte[bytes]);
	}
}
