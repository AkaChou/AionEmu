/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 * Aion-Lightning is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Aion-Lightning is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. *
 *
 * You should have received a copy of the GNU General Public License along with Aion-Lightning. If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.commons.utils;

import java.util.Iterator;
import java.util.Set;

import javolution.util.FastCollection.Record;
import javolution.util.FastMap;

/**
 * 基于FastMap实现的高性能Set集合类
 * High-performance Set implementation based on FastMap
 *
 * @author NB4L1
 * @param <E> 集合中元素的类型 / The type of elements in this set
 */
@SuppressWarnings("unchecked")
public class AEFastSet<E> extends AEFastCollection<E> implements Set<E> {

    /**
     * 用于表示空值的对象
     * Object used to represent null values
     */
    private static final Object NULL = new Object();

    /**
     * 内部存储使用的FastMap
     * Internal FastMap used for storage
     */
    private final FastMap<E, Object> map;

    /**
     * 创建一个空的AEFastSet
     * Create an empty AEFastSet
     */
    public AEFastSet() {
        map = new FastMap<E, Object>();
    }

    /**
     * 创建一个具有指定初始容量的AEFastSet
     * Create an AEFastSet with the specified initial capacity
     *
     * @param capacity 初始容量 / Initial capacity
     */
    public AEFastSet(int capacity) {
        map = new FastMap<E, Object>(capacity);
    }

    /**
     * 创建一个包含指定Set中所有元素的AEFastSet
     * Create an AEFastSet containing all elements from the specified Set
     *
     * @param elements 要添加的元素集合 / Set of elements to add
     */
    public AEFastSet(Set<? extends E> elements) {
        map = new FastMap<E, Object>(elements.size());
        addAll(elements);
    }

    /**
     * 检查此集合是否为共享模式
     * Check if this set is in shared mode
     */
    public boolean isShared() {
        return map.isShared();
    }

    @Override
    public Record head() {
        return map.head();
    }

    @Override
    public Record tail() {
        return map.tail();
    }

    @Override
    public E valueOf(Record record) {
        return ((FastMap.Entry<E, Object>) record).getKey();
    }

    @Override
    public void delete(Record record) {
        map.remove(((FastMap.Entry<E, Object>) record).getKey());
    }

    @Override
    public void delete(Record record, E value) {
        map.remove(value);
    }

    @Override
    public boolean add(E value) {
        return map.put(value, NULL) == null;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public String toString() {
        return super.toString() + "-" + map.keySet().toString();
    }
}
