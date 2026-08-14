package com.aionemu.gameserver.utils;

/**
 * 带溢出检测的安全加减乘运算。
 * Safe add/multiply operations with overflow detection.
 *
 * @author MrPoke
 */
public class SafeMath {

	/**
	 * 安全的 int 加法，溢出时抛出 {@link OverfowException}。
	 * Safe int addition; throws {@link OverfowException} on overflow.
	 *
	 * @param source 左操作数 / Left operand
	 * @param value 右操作数 / Right operand
	 * @return 和 / Sum
	 * @throws OverfowException 溢出时 / On overflow
	 */
	public static int addSafe(int source, int value) throws OverfowException {
		long s = (long) source + (long) value;
		if (s < Integer.MIN_VALUE || s > Integer.MAX_VALUE) {
			throw new OverfowException(source + " + " + value + " = " + ((long) source + (long) value));
		}
		return (int) s;
	}

	/**
	 * 安全的 long 加法，溢出时抛出 {@link OverfowException}。
	 * Safe long addition; throws {@link OverfowException} on overflow.
	 *
	 * @param source 左操作数 / Left operand
	 * @param value 右操作数 / Right operand
	 * @return 和 / Sum
	 * @throws OverfowException 溢出时 / On overflow
	 */
	public static long addSafe(long source, long value) throws OverfowException {
		if ((source > 0 && value > Long.MAX_VALUE - source) || (source < 0 && value < Long.MIN_VALUE - source)) {
			throw new OverfowException(source + " + " + value + " = " + ((long) source + (long) value));
		}
		return source + value;
	}

	/**
	 * 安全的 int 乘法，溢出时抛出 {@link OverfowException}。
	 * Safe int multiplication; throws {@link OverfowException} on overflow.
	 *
	 * @param source 左操作数 / Left operand
	 * @param value 右操作数 / Right operand
	 * @return 积 / Product
	 * @throws OverfowException 溢出时 / On overflow
	 */
	public static int multSafe(int source, int value) throws OverfowException {
		long m = ((long) source) * ((long) value);
		if (m < Integer.MIN_VALUE || m > Integer.MAX_VALUE) {
			throw new OverfowException(source + " * " + value + " = " + ((long) source * (long) value));
		}
		return (int) m;
	}

	/**
	 * 安全的 long 乘法，溢出时抛出 {@link OverfowException}。
	 * Safe long multiplication; throws {@link OverfowException} on overflow.
	 *
	 * @param a 被乘数 / Left operand
	 * @param b 乘数 / Right operand
	 * @return 积 / Product
	 * @throws OverfowException 溢出时 / On overflow
	 */
	public static long multSafe(long a, long b) throws OverfowException {

		long ret;
		String msg = "overflow: multiply";
		if (a > b) {
			// 利用对称减少边界情况 / use symmetry to reduce boundry cases
			ret = multSafe(b, a);
		} else {
			if (a < 0) {
				if (b < 0) {
					// 检查 a 负 b 负时的正溢出 / check for positive overflow with negative a, negative b
					if (a >= Long.MAX_VALUE / b) {
						ret = a * b;
					} else {
						throw new OverfowException(msg);
					}
				} else if (b > 0) {
					// 检查 a 负 b 正时的负溢出 / check for negative overflow with negative a, positive b
					if (Long.MIN_VALUE / b <= a) {
						ret = a * b;
					} else {
						throw new OverfowException(msg);

					}
				} else {
					ret = 0;
				}
			} else if (a > 0) {
				// 检查 a 正 b 正时的正溢出 / check for positive overflow with positive a, positive b
				if (a <= Long.MAX_VALUE / b) {
					ret = a * b;
				} else {
					throw new OverfowException(msg);
				}
			} else {
				ret = 0;
			}
		}
		return ret;
	}
}
