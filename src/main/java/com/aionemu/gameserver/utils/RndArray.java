package com.aionemu.gameserver.utils;

import java.util.List;

import com.aionemu.commons.utils.MTRandom;

/**
 * 基于 {@link MTRandom} 的随机数与随机选取工具。
 * Random number and selection utilities based on {@link MTRandom}.
 */
public class RndArray {
	private static final MTRandom rnd = new MTRandom();

	/**
	 * 返回 [0, 1) 范围内的随机浮点数。
	 * Returns a random float in the range [0, 1).
	 *
	 * @return 随机浮点数 / Random float
	 */
	public static float get() {
		return rnd.nextFloat();
	}

	/**
	 * 返回 [0, n) 范围内的随机整数。
	 * Returns a random int in the range [0, n).
	 *
	 * @param n 上界（不含） / Exclusive upper bound
	 * Random int
	 */
	public static int get(int n) {
		return (int) Math.floor(rnd.nextDouble() * n);
	}

	/**
	 * 返回 [min, max] 范围内的随机整数。
	 * Returns a random int in the range [min, max].
	 *
	 * Inclusive lower bound
	 * Inclusive upper bound
	 * Random int
	 */
	public static int get(int min, int max) {
		return min + (int) Math.floor(rnd.nextDouble() * (max - min + 1));
	}

	/**
	 * 按百分比概率判定是否命中（整数百分比）。
	 * Checks whether an integer percentage chance succeeds.
	 *
	 * Hit chance (1–100)
	 * Whether the chance hit
	 */
	public static boolean chance(int chance) {
		return (chance >= 1) && ((chance > 99) || (nextInt(99) + 1 <= chance));
	}

	/**
	 * 按百分比概率判定是否命中（双精度百分比）。
	 * Checks whether a double percentage chance succeeds.
	 *
	 * Hit chance (0–100)
	 * Whether the chance hit
	 */
	public static boolean chance(double chance) {
		return nextDouble() <= chance / 100.0D;
	}

	/**
	 * 从数组中随机选取一个元素。
	 * Selects a random element from an array.
	 *
	 * Array
	 * @param <E> 元素类型 / Element type
	 * Random element
	 */
	public static <E> E get(E[] list) {
		return list[get(list.length)];
	}

	/**
	 * 从 int 数组中随机选取一个值。
	 * Selects a random value from an int array.
	 *
	 * Int array
	 * Random value
	 */
	public static int get(int[] list) {
		return list[get(list.length)];
	}

	/**
	 * 从列表中随机选取一个元素。
	 * Selects a random element from a list.
	 *
	 * List
	 * @param <E> 元素类型 / Element type
	 * Random element
	 */
	public static <E> E get(List<E> list) {
		return list.get(get(list.size()));
	}

	/**
	 * 返回 [0, n) 范围内的随机整数。
	 * Returns a random int in the range [0, n).
	 *
	 * @param n 上界（不含） / Exclusive upper bound
	 * Random int
	 */
	public static int nextInt(int n) {
		return (int) Math.floor(rnd.nextDouble() * n);
	}

	/**
	 * 返回下一个随机 int（全范围）。
	 * Returns the next random int (full range).
	 *
	 * Random int
	 */
	public static int nextInt() {
		return rnd.nextInt();
	}

	/**
	 * 返回 [0, 1) 范围内的随机双精度数。
	 * Returns a random double in the range [0, 1).
	 *
	 * @return 随机双精度数 / Random double
	 */
	public static double nextDouble() {
		return rnd.nextDouble();
	}

	/**
	 * 返回下一个高斯分布随机数。
	 * Returns the next Gaussian-distributed random value.
	 *
	 * @return 高斯随机数 / Gaussian random value
	 */
	public static double nextGaussian() {
		return rnd.nextGaussian();
	}

	/**
	 * 返回下一个随机布尔值。
	 * Returns the next random boolean.
	 *
	 * @return 随机布尔值 / Random boolean
	 */
	public static boolean nextBoolean() {
		return rnd.nextBoolean();
	}
}
