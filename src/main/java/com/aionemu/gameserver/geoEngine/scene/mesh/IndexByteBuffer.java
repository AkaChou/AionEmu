package com.aionemu.gameserver.geoEngine.scene.mesh;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * 基于 {@link ByteBuffer} 的无符号字节索引缓冲实现。
 * {@link IndexBuffer} implementation backed by an unsigned-byte {@link ByteBuffer}.
 *
 * @author lex
 */
public class IndexByteBuffer extends IndexBuffer {

	/** 底层字节缓冲。 / Underlying byte buffer. */
	private ByteBuffer buf;

	/**
	 * 使用给定字节缓冲构造索引缓冲。
	 * Constructs an index buffer over the given byte buffer.
	 *
	 * byte buffer
	 */
	public IndexByteBuffer(ByteBuffer buffer) {
		this.buf = buffer;
	}

	/**
	 * 读取无符号字节索引（0–255）。
	 * Reads an unsigned-byte index (0–255).
	 *
	 * @param i 索引位置 / index position
	 * @return 无符号索引值 / unsigned index value
	 */
	@Override
	public int get(int i) {
		return buf.get(i) & 0x000000FF;
	}

	/**
	 * 写入字节索引值。
	 * Writes a byte index value.
	 *
	 * @param i 索引位置 / index position
	 * index value
	 */
	@Override
	public void put(int i, int value) {
		buf.put(i, (byte) value);
	}

	/**
	 * 返回缓冲 limit（元素个数）。
	 * Returns the buffer limit (element count).
	 *
	 * element count
	 */
	@Override
	public int size() {
		return buf.limit();
	}

	/**
	 * 返回底层字节缓冲。
	 * Returns the underlying byte buffer.
	 *
	 * byte buffer
	 */
	@Override
	public Buffer getBuffer() {
		return buf;
	}
}
