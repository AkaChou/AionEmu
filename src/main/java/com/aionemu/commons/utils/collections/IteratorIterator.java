package com.aionemu.commons.utils.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 双层迭代器实现，用于遍历嵌套集合结构
 * Implementation of a two-level iterator for traversing nested collection structures
 * 
 * @param <V> 迭代器返回的元素类型 / The type of elements returned by this iterator
 */
public class IteratorIterator<V> implements Iterator<V> {
    /** 第一层迭代器，用于遍历外层集合 / First level iterator for traversing the outer collection */
    private Iterator<? extends Iterable<V>> firstLevelIterator;
    
    /** 第二层迭代器，用于遍历内层集合 / Second level iterator for traversing the inner collection */
    private Iterator<V> secondLevelIterator;

    /**
     * 构造函数，初始化双层迭代器
     * Constructor to initialize the two-level iterator
     *
     * @param itit 包含可迭代对象的外层集合 / Outer collection containing iterable objects
     */
    public IteratorIterator(Iterable<? extends Iterable<V>> itit) {
        this.firstLevelIterator = itit.iterator();
    }

    /**
     * 检查是否还有下一个元素
     * Check if there are more elements to iterate
     *
     * @return true 如果还有下一个元素，false 如果已经遍历完所有元素
     *         true if there are more elements, false if all elements have been traversed
     */
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
     * 返回下一个元素
     * Returns the next element in the iteration
     *
     * @return 迭代器中的下一个元素 / The next element in the iteration
     * @throws NoSuchElementException 如果没有更多的元素 / If there are no more elements
     */
    public V next() {
        if (this.secondLevelIterator != null && this.secondLevelIterator.hasNext()) {
            return this.secondLevelIterator.next();
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * 移除操作（不支持）
     * Remove operation (not supported)
     *
     * @throws UnsupportedOperationException 该操作不被支持 / This operation is not supported
     */
    public void remove() {
        throw new UnsupportedOperationException("This operation is not supported.");
    }
}
