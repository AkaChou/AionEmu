package com.aionemu.gameserver.geoEngine.scene.mesh;

import java.nio.Buffer;
import java.nio.IntBuffer;

/**
 * 基于 {@link IntBuffer} 的整数索引缓冲实现。
 * {@link IndexBuffer} implementation backed by an {@link IntBuffer}.
 *
 * @author lex
 */
public class IndexIntBuffer extends IndexBuffer {

	/** 底层整型缓冲。 / Underlying int buffer. */
	private IntBuffer buf;

	/**
	 * 使用给定整型缓冲构造索引缓冲。
	 * Constructs an index buffer over the given int buffer.
	 *
	 * @param buffer 整型缓冲 / int buffer
	 */
	public IndexIntBuffer(IntBuffer buffer) {
		this.buf = buffer;
	}

	/**
	 * 读取整数索引。
	 * Reads an integer index.
	 *
	 * @param i 索引位置 / index position
	 * @return 索引值 / index value
	 */
	@Override
	public int get(int i) {
		return buf.get(i);
	}

	/**
	 * 写入整数索引。
	 * Writes an integer index.
	 *
	 * @param i 索引位置 / index position
	 * @param value 索引值 / index value
	 */
	@Override
	public void put(int i, int value) {
		buf.put(i, value);
	}

	/**
	 * 返回缓冲 limit（元素个数）。
	 * Returns the buffer limit (element count).
	 *
	 * @return 元素个数 / element count
	 */
	@Override
	public int size() {
		return buf.limit();
	}

	/**
	 * 返回底层整型缓冲。
	 * Returns the underlying int buffer.
	 *
	 * @return 底层整型缓冲 / int buffer
	 */
	@Override
	public Buffer getBuffer() {
		return buf;
	}
}
