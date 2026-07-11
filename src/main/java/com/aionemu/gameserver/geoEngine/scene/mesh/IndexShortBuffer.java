package com.aionemu.gameserver.geoEngine.scene.mesh;

import java.nio.Buffer;
import java.nio.ShortBuffer;

/**
 * 基于 {@link ShortBuffer} 的无符号 short 索引缓冲实现。
 * {@link IndexBuffer} implementation backed by an unsigned-short {@link ShortBuffer}.
 *
 * @author lex
 */
public class IndexShortBuffer extends IndexBuffer {

	/** 底层 short 缓冲 / Underlying short buffer */
	private ShortBuffer buf;

	/**
	 * 使用给定 short 缓冲构造索引缓冲。
	 * Constructs an index buffer over the given short buffer.
	 *
	 * short buffer
	 */
	public IndexShortBuffer(ShortBuffer buffer) {
		this.buf = buffer;
	}

	/**
	 * 读取无符号 short 索引（0–65535）。
	 * Reads an unsigned-short index (0–65535).
	 *
	 * @param i 索引位置 / index position
	 * @return 无符号索引值 / unsigned index value
	 */
	@Override
	public int get(int i) {
		return buf.get(i) & 0x0000FFFF;
	}

	/**
	 * 写入 short 索引值。
	 * Writes a short index value.
	 *
	 * @param i 索引位置 / index position
	 * index value
	 */
	@Override
	public void put(int i, int value) {
		buf.put(i, (short) value);
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
	 * 返回底层 short 缓冲。
	 * Returns the underlying short buffer.
	 *
	 * short buffer
	 */
	@Override
	public Buffer getBuffer() {
		return buf;
	}
}
