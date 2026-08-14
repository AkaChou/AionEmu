package com.aionemu.gameserver.geoEngine.scene.mesh;

import java.nio.Buffer;

/**
 * 整数索引缓冲抽象，用于在不关心底层存储格式（ushort/uint 等）的情况下读写索引。
 * Abstraction over integer index buffers, used to read/write indices without knowing the storage format (ushort/uint, etc.).
 *
 * @author lex
 */
public abstract class IndexBuffer {

	/**
	 * 读取指定位置的索引值。
	 * Reads the index value at the given position.
	 *
	 * @param i 索引位置 / index position
	 * @return 索引值 / index value
	 */
	public abstract int get(int i);

	/**
	 * 写入指定位置的索引值。
	 * Writes an index value at the given position.
	 *
	 * @param i 索引位置 / index position
	 * @param value 索引值 / index value
	 */
	public abstract void put(int i, int value);

	/**
	 * 返回缓冲中的元素个数。
	 * Returns the number of elements in the buffer.
	 *
	 * @return 元素个数 / element count
	 */
	public abstract int size();

	/**
	 * 返回底层 NIO 缓冲。
	 * Returns the underlying NIO buffer.
	 *
	 * @return 底层缓冲 / underlying buffer
	 */
	public abstract Buffer getBuffer();
}
