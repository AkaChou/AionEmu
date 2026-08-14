package com.aionemu.gameserver.utils.collections;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 将集合按固定块大小切分并顺序取出。
 * Split a collection into fixed-size chunks and yield them in order.
 *
 * @param <T> 元素类型 / Element type
 * @author xTz
 */
public class ListSplitter<T> {

	/**
	 * 源数组。
	 * Source array.
	 */
	private T[] objects;

	/**
	 * 组件类型，用于创建子数组。
	 * Component type used to allocate sub-arrays.
	 */
	private Class<?> componentType;

	/**
	 * 每次切分的元素个数。
	 * Number of elements per split.
	 */
	private int splitCount;

	/**
	 * 当前读取下标。
	 * Current read index.
	 */
	private int curentIndex = 0;

	/**
	 * 源集合长度。
	 * Source collection length.
	 */
	private int length = 0;

	/**
	 * 用集合与切分大小构造。
	 * Construct from a collection and split size.
	 *
	 * @param collection 源集合 / Source collection
	 * @param splitCount 分块大小 / Chunk size
	 */
	@SuppressWarnings("unchecked")
	public ListSplitter(Collection<T> collection, int splitCount) {
		if (collection != null && collection.size() > 0) {
			this.splitCount = splitCount;
			length = collection.size();
			this.objects = collection.toArray((T[]) new Object[length]);
			componentType = objects.getClass().getComponentType();
		}
	}

	/**
	 * 使用新的切分大小并取下一块。
	 * Use a new split size and return the next chunk.
	 *
	 * @param splitCount 新的每块大小 / New chunk size
	 * @return 下一块列表 / Next chunk as list
	 */
	public List<T> getNext(int splitCount) {
		this.splitCount = splitCount;
		return getNext();
	}

	/**
	 * 取下一块元素。
	 * Return the next chunk of elements.
	 *
	 * @return 下一块列表 / Next chunk as list
	 */
	public List<T> getNext() {
		@SuppressWarnings("unchecked")
		T[] subArray = (T[]) Array.newInstance(componentType, Math.min(splitCount, length - curentIndex));
		if (subArray.length > 0) {
			System.arraycopy(objects, curentIndex, subArray, 0, subArray.length);
			curentIndex += subArray.length;
		}
		return Arrays.asList(subArray);
	}

	/**
	 * 源集合总长度。
	 * Total length of the source collection.
	 *
	 * @return 长度 / Length
	 */
	public int size() {
		return length;
	}

	/**
	 * 是否仍在第一块范围内。
	 * Whether still within the first chunk.
	 *
	 * @return 第一块则为 true / True if first chunk
	 */
	public boolean isFirst() {
		return curentIndex <= splitCount;
	}

	/**
	 * 是否已取完所有元素。
	 * Whether all elements have been consumed.
	 *
	 * @return 已取完则为 true / True if finished
	 */
	public boolean isLast() {
		return curentIndex == length;
	}
}
