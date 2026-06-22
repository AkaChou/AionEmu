package com.aionemu.commons.utils;

import java.util.List;

/**
 * 随机数工具类，提供各种随机数生成和随机选择功能
 * Random number utility class that provides various random number generation and random selection functions
 */
public class Rnd {
   /**
    * 基于Mersenne Twister算法的随机数生成器实例
    * Random number generator instance based on Mersenne Twister algorithm
    */
   private static final MTRandom rnd = new MTRandom();

   /**
    * 获取0到1之间的随机浮点数
    * Get a random float between 0 and 1
    * 
    * @return 随机浮点数 / random float value
    */
   public static float get() {
      return rnd.nextFloat();
   }

   /**
    * 获取0到n-1之间的随机整数
    * Get a random integer between 0 and n-1
    * 
    * @param n 上限值(不包含) / upper bound (exclusive)
    * @return 随机整数 / random integer value
    */
   public static int get(int n) {
      return (int)Math.floor(rnd.nextDouble() * (double)n);
   }

   /**
    * 获取min到max之间的随机整数(包含边界值)
    * Get a random integer between min and max (inclusive)
    * 
    * @param min 下限值(包含) / lower bound (inclusive)
    * @param max 上限值(包含) / upper bound (inclusive)
    * @return 随机整数 / random integer value
    */
   public static int get(int min, int max) {
      return min + (int)Math.floor(rnd.nextDouble() * (double)(max - min + 1));
   }

   /**
    * 根据给定概率判断是否成功
    * Determine success based on given chance percentage
    * 
    * @param chance 成功概率(百分比，1-100) / success chance (percentage, 1-100)
    * @return 是否成功 / whether successful
    */
   public static boolean chance(int chance) {
      return chance >= 1 && (chance > 99 || nextInt(99) + 1 <= chance);
   }

   /**
    * 根据给定概率判断是否成功(支持小数)
    * Determine success based on given chance percentage (supports decimals)
    * 
    * @param chance 成功概率(百分比，0-100) / success chance (percentage, 0-100)
    * @return 是否成功 / whether successful
    */
   public static boolean chance(double chance) {
      return nextDouble() <= chance / 100.0D;
   }

   /**
    * 从数组中随机选择一个元素
    * Randomly select an element from an array
    * 
    * @param <E> 元素类型 / element type
    * @param list 源数组 / source array
    * @return 随机选择的元素 / randomly selected element
    */
   public static <E> E get(E[] list) {
      return list[get(list.length)];
   }

   /**
    * 从整数数组中随机选择一个元素
    * Randomly select an element from an integer array
    * 
    * @param list 整数数组 / integer array
    * @return 随机选择的整数 / randomly selected integer
    */
   public static int get(int[] list) {
      return list[get(list.length)];
   }

   /**
    * 从列表中随机选择一个元素
    * Randomly select an element from a list
    * 
    * @param <E> 元素类型 / element type
    * @param list 源列表 / source list
    * @return 随机选择的元素 / randomly selected element
    */
   public static <E> E get(List<E> list) {
      return list.get(get(list.size()));
   }

   /**
    * 获取0到n-1之间的随机整数
    * Get a random integer between 0 and n-1
    * 
    * @param n 上限值(不包含) / upper bound (exclusive)
    * @return 随机整数 / random integer value
    */
   public static int nextInt(int n) {
      return (int)Math.floor(rnd.nextDouble() * (double)n);
   }

   /**
    * 获取随机整数
    * Get a random integer
    * 
    * @return 随机整数 / random integer value
    */
   public static int nextInt() {
      return rnd.nextInt();
   }

   /**
    * 获取0到1之间的随机双精度浮点数
    * Get a random double between 0 and 1
    * 
    * @return 随机双精度浮点数 / random double value
    */
   public static double nextDouble() {
      return rnd.nextDouble();
   }

   /**
    * 获取符合高斯分布的随机数
    * Get a random number following Gaussian distribution
    * 
    * @return 随机高斯分布数 / random Gaussian value
    */
   public static double nextGaussian() {
      return rnd.nextGaussian();
   }

   /**
    * 获取随机布尔值
    * Get a random boolean value
    * 
    * @return 随机布尔值 / random boolean value
    */
   public static boolean nextBoolean() {
      return rnd.nextBoolean();
   }
}
