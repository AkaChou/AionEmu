package com.aionemu.commons.utils.collections;

import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * 基于原生 {@code int[]} 数组的高性能动态整数列表（避免 {@link Integer} 装箱与节点分配）。
 * High-performance dynamic int list backed by primitive {@code int[]} array (avoids {@link Integer} boxing).
 * <p>提供零装箱原生接口，同时继承 {@link AbstractList} 实现 {@link RandomAccess} 以保持集合生态兼容。
 * Offers zero-boxing primitive methods while extending {@link AbstractList} and implementing {@link RandomAccess} for compatibility.</p>
 */
public class IntArrayList extends AbstractList<Integer> implements RandomAccess, Cloneable, Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private static final int DEFAULT_CAPACITY = 10;
	private static final int[] EMPTY_DATA = new int[0];

	private int[] data;
	private int size;

	/**
	 * 创建默认容量的整数列表。
	 * Creates an empty int list with default capacity.
	 */
	public IntArrayList() {
		this.data = new int[DEFAULT_CAPACITY];
		this.size = 0;
	}

	/**
	 * 创建指定初始容量的整数列表。
	 * Creates an empty int list with the specified initial capacity.
	 *
	 * @param initialCapacity 初始容量 / initial capacity
	 */
	public IntArrayList(int initialCapacity) {
		if (initialCapacity < 0) {
			throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
		}
		this.data = initialCapacity == 0 ? EMPTY_DATA : new int[initialCapacity];
		this.size = 0;
	}

	/**
	 * 基于已有集合初始化整数列表。
	 * Initializes with the given collection of values.
	 *
	 * @param values 初始集合 / initial collection
	 */
	public IntArrayList(Collection<Integer> values) {
		if (values == null || values.isEmpty()) {
			this.data = new int[DEFAULT_CAPACITY];
			this.size = 0;
		} else {
			this.data = new int[values.size()];
			this.size = 0;
			for (Integer v : values) {
				if (v != null) {
					data[size++] = v;
				}
			}
		}
	}

	/**
	 * 基于已有原始 int 数组初始化列表。
	 * Initializes with the given primitive int array.
	 *
	 * @param values 初始 int 数组 / initial int array
	 */
	public IntArrayList(int[] values) {
		if (values == null || values.length == 0) {
			this.data = new int[DEFAULT_CAPACITY];
			this.size = 0;
		} else {
			this.data = Arrays.copyOf(values, values.length);
			this.size = values.length;
		}
	}

	/**
	 * 添加原始 int 值（零装箱）。
	 * Adds a primitive int value without boxing.
	 *
	 * @param value 要添加的 int 值 / int value to add
	 * @return 恒为 true / always true
	 */
	public boolean add(int value) {
		ensureExplicitCapacity(size + 1);
		data[size++] = value;
		modCount++;
		return true;
	}

	/**
	 * 是否包含指定原始 int 值（零装箱）。
	 * Checks if the list contains the primitive int value without boxing.
	 *
	 * @param value 要检查的 int 值 / int value to check
	 * @return 存在返回 true / true if present
	 */
	public boolean contains(int value) {
		for (int i = 0; i < size; i++) {
			if (data[i] == value) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 按索引读取原始 int 值（零装箱）。
	 * Gets primitive int value at the specified index without boxing.
	 *
	 * @param index 索引 / index
	 * @return 原始 int 值 / primitive int value
	 */
	public int getInt(int index) {
		rangeCheck(index);
		return data[index];
	}

	/**
	 * 按索引设置原始 int 值，返回旧值。
	 * Sets primitive int value at index, returning the old value.
	 *
	 * @param index 索引 / index
	 * @param value 新值 / new value
	 * @return 旧值 / old value
	 */
	public int setInt(int index, int value) {
		rangeCheck(index);
		int old = data[index];
		data[index] = value;
		return old;
	}

	/**
	 * 移除指定索引位置的元素，返回被移除的值。
	 * Removes element at index, returning the removed primitive value.
	 *
	 * @param index 索引 / index
	 * @return 被移除的 int 值 / removed int value
	 */
	public int removeAt(int index) {
		rangeCheck(index);
		int oldValue = data[index];
		int numMoved = size - index - 1;
		if (numMoved > 0) {
			System.arraycopy(data, index + 1, data, index, numMoved);
		}
		size--;
		modCount++;
		return oldValue;
	}

	/**
	 * 遍历全部元素；过程返回 false 时提前结束（零装箱、零迭代器）。
	 * Iterates over elements; aborts early when procedure returns false (zero boxing, zero iterator).
	 *
	 * @param procedure 回调过程 / callback procedure
	 * @return 全部遍历完为 true，提前中断为 false / true if completed, false if aborted
	 */
	public boolean forEach(IntProcedure procedure) {
		Objects.requireNonNull(procedure);
		for (int i = 0; i < size; i++) {
			if (!procedure.execute(data[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 导出为紧凑的原始 int 数组快照。
	 * Exports a compact snapshot as primitive int array.
	 *
	 * @return 原始 int 数组 / primitive int array
	 */
	public int[] toIntArray() {
		return Arrays.copyOf(data, size);
	}

	/**
	 * 确保列表容量可容纳指定元素数量。
	 * Ensures the capacity can hold at least the specified element count.
	 *
	 * @param minCapacity 期望最小容量 / minimum capacity
	 */
	public void ensureCapacity(int minCapacity) {
		if (minCapacity > data.length) {
			grow(minCapacity);
		}
	}

	/**
	 * 缩容至当前实际大小以节省内存。
	 * Trims the capacity to current actual size to conserve memory.
	 */
	public void trimToSize() {
		if (size < data.length) {
			data = (size == 0) ? EMPTY_DATA : Arrays.copyOf(data, size);
		}
	}

	// ==================== List 接口兼容重写 / List Interface Compatibility ====================

	@Override
	public Integer get(int index) {
		return getInt(index);
	}

	@Override
	public boolean add(Integer value) {
		if (value == null) {
			throw new NullPointerException("IntArrayList does not permit null values");
		}
		return add(value.intValue());
	}

	@Override
	public Integer set(int index, Integer element) {
		if (element == null) {
			throw new NullPointerException("IntArrayList does not permit null values");
		}
		return setInt(index, element);
	}

	@Override
	public Integer remove(int index) {
		return removeAt(index);
	}

	@Override
	public boolean contains(Object o) {
		if (o instanceof Integer val) {
			return contains(val.intValue());
		}
		return false;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public void clear() {
		modCount++;
		size = 0;
	}

	@Override
	public IntArrayList clone() {
		try {
			IntArrayList clone = (IntArrayList) super.clone();
			clone.data = Arrays.copyOf(data, size);
			clone.modCount = 0;
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	// ==================== 内部容量维护 / Internal Capacity Management ====================

	private void rangeCheck(int index) {
		if (index < 0 || index >= size) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
		}
	}

	private void ensureExplicitCapacity(int minCapacity) {
		if (minCapacity > data.length) {
			grow(minCapacity);
		}
	}

	private void grow(int minCapacity) {
		int oldCapacity = data.length;
		int newCapacity = oldCapacity + (oldCapacity >> 1);
		if (newCapacity < minCapacity) {
			newCapacity = minCapacity;
		}
		if (newCapacity < DEFAULT_CAPACITY) {
			newCapacity = DEFAULT_CAPACITY;
		}
		data = Arrays.copyOf(data, newCapacity);
	}
}
