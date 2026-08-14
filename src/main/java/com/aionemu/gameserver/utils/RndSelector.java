package com.aionemu.gameserver.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aionemu.commons.utils.Rnd;

/**
 * 按权重随机选取元素的选择器。
 * Weighted random selector for choosing elements.
 *
 * @author Ranastic
 * @param <E> 元素类型 / Element type
 */
public class RndSelector<E> {

	/**
	 * 带权重的节点，用于内部排序与选取。
	 * Weighted node used for internal sorting and selection.
	 *
	 * @param <T> 节点值类型 / Node value type
	 */
	private class RndNode<T> implements Comparable<RndNode<T>> {
		private final T value;
		private final int weight;

		/**
		 * 创建带权重节点。
		 * Creates a weighted node.
		 *
		 * @param value 节点值 / Node value
		 * @param weight 权重 / Weight
		 */
		public RndNode(T value, int weight) {
			this.value = value;
			this.weight = weight;
		}

		/**
		 * 按权重比较节点。
		 * Compares nodes by weight.
		 *
		 * @param o 另一个节点 / Other node
		 * @return 权重差 / Weight difference
		 */
		@Override
		public int compareTo(RndNode<T> o) {
			return this.weight - weight;
		}
	}

	/** 权重总和 / Total weight of all nodes */
	private int totalWeight = 0;
	/** 节点列表 / Node list */
	private final List<RndNode<E>> nodes;

	/**
	 * 使用默认容量创建选择器。
	 * Creates a selector with default capacity.
	 */
	public RndSelector() {
		nodes = new ArrayList<RndNode<E>>();
	}

	/**
	 * 使用指定初始容量创建选择器。
	 * Creates a selector with the given initial capacity.
	 *
	 * @param initialCapacity 初始容量 / Initial capacity
	 */
	public RndSelector(int initialCapacity) {
		nodes = new ArrayList<RndNode<E>>(initialCapacity);
	}

	/**
	 * 添加带权重的元素；值为 null 或权重不大于 0 时忽略。
	 * Adds a weighted value; ignored when value is null or weight is not positive.
	 *
	 * @param value 元素值 / Element value
	 * @param weight 权重 / Weight
	 */
	public void add(E value, int weight) {
		if (value == null || weight <= 0) {
			return;
		}
		totalWeight += weight;
		nodes.add(new RndNode<E>(value, weight));
	}

	/**
	 * 在给定最大权重范围内按权重抽取；未命中时返回 null。
	 * Selects by weight within the given max weight; returns null on miss.
	 *
	 * @param maxWeight 最大权重（抽签上限） / Max weight (draw ceiling)
	 * @return 选中的元素，或 null / Selected element, or null
	 */
	public E chance(int maxWeight) {
		if (maxWeight <= 0) {
			return null;
		}
		Collections.sort(nodes);
		int r = Rnd.get(maxWeight);
		int weight = 0;
		for (int i = 0; i < nodes.size(); i++) {
			if ((weight += nodes.get(i).weight) > r) {
				return nodes.get(i).value;
			}
		}
		return null;
	}

	/**
	 * 按 100 为上限进行加权抽取。
	 * Weighted selection with max weight of 100.
	 *
	 * @return 选中的元素，或 null / Selected element, or null
	 */
	public E chance() {
		return chance(100);
	}

	/**
	 * 按当前总权重进行加权抽取（必中其一）。
	 * Weighted selection using the current total weight (always hits one).
	 *
	 * @return 选中的元素，或 null / Selected element, or null
	 */
	public E select() {
		return chance(totalWeight);
	}

	/**
	 * 清空所有节点并重置总权重。
	 * Clears all nodes and resets total weight.
	 */
	public void clear() {
		totalWeight = 0;
		nodes.clear();
	}
}
