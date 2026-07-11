package com.aionemu.commons.utils.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 双层迭代器，用于扁平遍历嵌套可迭代结构。
 * Two-level iterator for flat traversal of nested iterables.
 *
 * @param <V> 元素类型 / Element type
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
        if (this.secondLevelIterator != null && this.secondLevelIterator.hasNext()) {
            return true;
        } else {
            while (this.firstLevelIterator.hasNext()) {
                Iterable<V> iterable = (Iterable) this.firstLevelIterator.next();
                if (iterable != null) {
                    this.secondLevelIterator = iterable.iterator();
                    if (this.secondLevelIterator.hasNext()) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * 返回下一个元素。
     * Return the next element.
     *
     * Next element
     *
     * @return @throws NoSuchElementException 无更多元素时 / When exhausted
     */
    @Override
    public V next() {
        if (this.secondLevelIterator != null && this.secondLevelIterator.hasNext()) {
            return this.secondLevelIterator.next();
        } else {
            throw new NoSuchElementException();
        }
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
