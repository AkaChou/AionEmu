package com.aionemu.commons.utils;

import java.util.List;
import lombok.experimental.UtilityClass;

/**
 * 基于 Mersenne Twister 的随机数与随机选择工具。
 * Random helpers backed by Mersenne Twister.
 */
@UtilityClass
public class Rnd {

    /**
     * Mersenne Twister 随机源。
     * Mersenne Twister random source.
     */
    private static final MTRandom rnd = new MTRandom();

    /**
     * 获取 {@code [0, 1)} 随机浮点数。
     * Random float in {@code [0, 1)}.
     *
     * @return 随机浮点数 / Random float
     */
    public float get() {
        return rnd.nextFloat();
    }

    /**
     * 获取 {@code [0, n)} 随机整数。
     * Random int in {@code [0, n)}.
     *
     * @param n 上限（不含） / Upper bound (exclusive)
     * @return 随机整数 / Random int
     */
    public int get(int n) {
        return (int) Math.floor(rnd.nextDouble() * (double) n);
    }

    /**
     * 获取 {@code [min, max]} 随机整数。
     * Random int in {@code [min, max]}.
     *
     * @param min 下限（含） / Lower bound (inclusive)
     * @param max 上限（含） / Upper bound (inclusive)
     * @return 随机整数 / Random int
     */
    public int get(int min, int max) {
        return min + (int) Math.floor(rnd.nextDouble() * (double) (max - min + 1));
    }

    /**
     * 按百分比概率判定是否成功（1–100）。
     * Chance success by percentage (1–100).
     *
     * @param chance 成功概率（百分比） / Success chance (percent)
     * @return 是否成功 / Whether successful
     */
    public boolean chance(int chance) {
        return chance >= 1 && (chance > 99 || nextInt(99) + 1 <= chance);
    }

    /**
     * 按百分比概率判定是否成功（支持小数，0–100）。
     * Chance success by percentage with decimals (0–100).
     *
     * @param chance 成功概率（百分比） / Success chance (percent)
     * @return 是否成功 / Whether successful
     */
    public boolean chance(double chance) {
        return nextDouble() <= chance / 100.0D;
    }

    /**
     * 从数组中随机取一个元素。
     * Pick a random element from an array.
     *
     * @param <E>  元素类型 / Element type
     * @param list 源数组 / Source array
     * @return 随机元素 / Random element
     */
    public <E> E get(E[] list) {
        return list[get(list.length)];
    }

    /**
     * 从 int 数组中随机取一个元素。
     * Pick a random element from an int array.
     *
     * @param list 整数数组 / Int array
     * @return 随机整数 / Random int
     */
    public int get(int[] list) {
        return list[get(list.length)];
    }

    /**
     * 从列表中随机取一个元素。
     * Pick a random element from a list.
     *
     * @param <E>  元素类型 / Element type
     * @param list 源列表 / Source list
     * @return 随机元素 / Random element
     */
    public <E> E get(List<E> list) {
        return list.get(get(list.size()));
    }

    /**
     * 获取 {@code [0, n)} 随机整数。
     * Random int in {@code [0, n)}.
     *
     * @param n 上限（不含） / Upper bound (exclusive)
     * @return 随机整数 / Random int
     */
    public int nextInt(int n) {
        return (int) Math.floor(rnd.nextDouble() * (double) n);
    }

    /**
     * 获取随机整数。
     * Random int value.
     *
     * @return 随机整数 / Random int
     */
    public int nextInt() {
        return rnd.nextInt();
    }

    /**
     * 获取 {@code [0, 1)} 随机双精度数。
     * Random double in {@code [0, 1)}.
     *
     * @return 随机双精度 / Random double
     */
    public double nextDouble() {
        return rnd.nextDouble();
    }

    /**
     * 获取高斯分布随机数。
     * Random Gaussian value.
     *
     * @return 高斯随机数 / Gaussian value
     */
    public double nextGaussian() {
        return rnd.nextGaussian();
    }

    /**
     * 获取随机布尔值。
     * Random boolean value.
     *
     * @return 随机布尔值 / Random boolean
     */
    public boolean nextBoolean() {
        return rnd.nextBoolean();
    }
}
