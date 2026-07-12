package com.aionemu.gameserver.utils.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 双层迭代器，用于扁平遍历 {@code Iterable&lt;Iterable&lt;V&gt;&gt;} 结构。
 * Two-level iterator for flat traversal of {@code Iterable&lt;Iterable&lt;V&gt;&gt;}.
 *
 * <pre>
 * 用法 / Usage:
 * List&lt;List&lt;Integer&gt;&gt; someList = ....
 * IteratorIterator&lt;Integer&gt; iterator = new IteratorIterator&lt;Integer&gt;(someList)
 *
 * 或 / OR:
 *
 * Map&lt;Integer, Set&lt;SomeClass&gt;&gt; mapOfSets = ....
 * IteratorIterator&lt;SomeClass&gt; iterator = new IteratorIterator&lt;SomeClass&gt;(mapOfSets.values());
 * </pre>
 * <p>
 * 非线程安全。外层集合中的 null 会被跳过。
 * Not thread-safe. Null entries in the outer collection are omitted.
 * <p>
 * 例如外层集合包含 null 与某个含 1、2 的集合时，本迭代器只返回 1 和 2。
 * E.g. if the outer set holds null and a set of 1 and 2, only 1 and 2 are returned.
 *
 *
 * @param <V> 元素类型 / Element type
 * @author Luno
 */
public class IteratorIterator<V> implements Iterator<V> {

	/**
	 * 外层迭代器。
	 * Outer-level iterator.
	 */
	private Iterator<? extends Iterable<V>> firstLevelIterator;

	/**
	 * 内层迭代器。
	 * Inner-level iterator.
	 */
	private Iterator<V> secondLevelIterator;

	/**
	 * 使用外层可迭代对象构造。
	 * Construct from an outer iterable of iterables.
	 *
	 * @param itit 外层集合 / Outer collection
	 */
	public IteratorIterator(Iterable<? extends Iterable<V>> itit) {
		this.firstLevelIterator = itit.iterator();
	}

	/**
	 * 是否还有下一个元素。
	 * Whether another element is available.
	 *
	 * @return 若 more elements 则为 true / True if more elements
	 */
	@Override
	public boolean hasNext() {
		if (secondLevelIterator != null && secondLevelIterator.hasNext()) {
			return true;
		}
		while (firstLevelIterator.hasNext()) {
			Iterable<V> iterable = firstLevelIterator.next();

			if (iterable != null) {
				secondLevelIterator = iterable.iterator();

				if (secondLevelIterator.hasNext()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 返回下一个元素；无更多元素时抛出 {@link NoSuchElementException}。
	 * Return the next element; throws {@link NoSuchElementException} when exhausted.
	 *
	 * Next element
	 *
	 * @return
	 * @throws NoSuchElementException 无更多元素时 / When exhausted
	 */
	@Override
	public V next() {
		if (secondLevelIterator == null || !secondLevelIterator.hasNext()) {
			throw new NoSuchElementException();
		}
		return secondLevelIterator.next();
	}

	/**
	 * 不支持移除。
	 * Remove is not supported.
	 *
	 * Always thrown
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException("This operation is not supported.");
	}
}
