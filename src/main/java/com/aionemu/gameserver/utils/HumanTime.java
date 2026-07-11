package com.aionemu.gameserver.utils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;

/**
 * 人类可读时间跨度工具：支持毫秒增量的构建、解析与格式化。
 * Human-readable time span utility: build, parse, and format millisecond deltas.
 *
 * <p>
 * 支持精确（exactly）与近似（approximately）两种输出；可用链式 API（y/d/h/m/s/ms）
 * 累加时长，也可用 {@link #eval(CharSequence)} 解析如 {@code "2h 30m"} 的字符串。
 * Supports exact and approximate formatting; duration can be built via fluent
 * y/d/h/m/s/ms methods or parsed from strings such as {@code "2h 30m"}.
 * </p>
 */
public class HumanTime implements Externalizable, Comparable<HumanTime>, Cloneable {

	/** 序列化版本号 / Serialization version UID */
	private static final long serialVersionUID = 5179328390732826722L;

	/** 一秒的毫秒数 / Milliseconds in one second */
	private static final long SECOND = 1000;

	/** 一分钟的毫秒数 / Milliseconds in one minute */
	private static final long MINUTE = SECOND * 60;

	/** 一小时的毫秒数 / Milliseconds in one hour */
	private static final long HOUR = MINUTE * 60;

	/** 一天的毫秒数 / Milliseconds in one day */
	private static final long DAY = HOUR * 24;

	/** 一年的毫秒数（按 365 天） / Milliseconds in one year (365 days) */
	private static final long YEAR = DAY * 365;

	/** 近似进位/舍去的百分比阈值 / Percentage threshold for approximate ceiling/floor */
	private static final int CEILING_PERCENTAGE = 15;

	/**
	 * 解析状态机状态。
	 * Parser state-machine states.
	 */
	static enum State {
		/** 数字字符 / Numeric character */
		NUMBER,
		/** 忽略字符 / Ignored character */
		IGNORED,
		/** 时间单位字符 / Time-unit character */
		UNIT
	}

	/**
	 * 根据字符判断解析状态。
	 * Resolve the parser state for a character.
	 *
	 * @param c 输入字符 / Input character
	 * Matching state
	 */
	static State getState(char c) {
		State out;
		switch (c) {
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
			out = State.NUMBER;
			break;
		case 's':
		case 'm':
		case 'h':
		case 'd':
		case 'y':
		case 'S':
		case 'M':
		case 'H':
		case 'D':
		case 'Y':
			out = State.UNIT;
			break;
		default:
			out = State.IGNORED;
		}
		return out;
	}

	/**
	 * 解析人类可读时间字符串为 {@link HumanTime}。
	 * Parse a human-readable time string into a {@link HumanTime}.
	 *
	 * @param s 时间字符串，如 {@code "1y 2d 3h"} / Time string, e.g. {@code "1y 2d 3h"}
	 * Parsed instance
	 */
	public static HumanTime eval(final CharSequence s) {
		HumanTime out = new HumanTime(0L);
		int num = 0;
		int start = 0;
		int end = 0;
		State oldState = State.IGNORED;
		for (char c : new Iterable<Character>() {
			public Iterator<Character> iterator() {
				return new Iterator<Character>() {
					private int p = 0;

					public boolean hasNext() {
						return p < s.length();
					}

					public Character next() {
						return s.charAt(p++);
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		}) {
			State newState = getState(c);
			if (oldState != newState) {
				if (oldState == State.NUMBER && (newState == State.IGNORED || newState == State.UNIT)) {
					num = Integer.parseInt(s.subSequence(start, end).toString());
				} else if (oldState == State.UNIT && (newState == State.IGNORED || newState == State.NUMBER)) {
					out.nTimes(s.subSequence(start, end).toString(), num);
					num = 0;
				}
				start = end;
			}
			++end;
			oldState = newState;
		}
		if (oldState == State.UNIT) {
			out.nTimes(s.subSequence(start, end).toString(), num);
		}
		return out;
	}

	/**
	 * 将时间字符串格式化为精确描述。
	 * Format a time string as an exact description.
	 *
	 * @param in 输入时间字符串 / Input time string
	 * @return 精确格式化结果 / Exact formatted result
	 */
	public static String exactly(CharSequence in) {
		return eval(in).getExactly();
	}

	/**
	 * 将毫秒增量格式化为精确描述。
	 * Format a millisecond delta as an exact description.
	 *
	 * @param l 毫秒增量 / Delta in milliseconds
	 * @return 精确格式化结果 / Exact formatted result
	 */
	public static String exactly(long l) {
		return new HumanTime(l).getExactly();
	}

	/**
	 * 将时间字符串格式化为近似描述。
	 * Format a time string as an approximate description.
	 *
	 * @param in 输入时间字符串 / Input time string
	 * @return 近似格式化结果 / Approximate formatted result
	 */
	public static String approximately(CharSequence in) {
		return eval(in).getApproximately();
	}

	/**
	 * 将毫秒增量格式化为近似描述。
	 * Format a millisecond delta as an approximate description.
	 *
	 * @param l 毫秒增量 / Delta in milliseconds
	 * @return 近似格式化结果 / Approximate formatted result
	 */
	public static String approximately(long l) {
		return new HumanTime(l).getApproximately();
	}

	/** 内部毫秒增量（始终非负） / Internal millisecond delta (always non-negative) */
	private long delta;

	/**
	 * 构造零时长实例。
	 * Construct a zero-duration instance.
	 */
	public HumanTime() {
		this(0L);
	}

	/**
	 * 以指定毫秒增量构造（取绝对值）。
	 * Construct with the absolute value of the given millisecond delta.
	 *
	 * @param delta 毫秒增量 / Delta in milliseconds
	 */
	public HumanTime(long delta) {
		super();
		this.delta = Math.abs(delta);
	}

	/**
	 * 按单位字符串累加 n 个单位。
	 * Add n units of the given unit string.
	 *
	 * @param unit Unit (ms / s/m/h/d/y)
	 * @param n 数量 / Count
	 */
	private void nTimes(String unit, int n) {
		if ("ms".equalsIgnoreCase(unit)) {
			ms(n);
		} else if ("s".equalsIgnoreCase(unit)) {
			s(n);
		} else if ("m".equalsIgnoreCase(unit)) {
			m(n);
		} else if ("h".equalsIgnoreCase(unit)) {
			h(n);
		} else if ("d".equalsIgnoreCase(unit)) {
			d(n);
		} else if ("y".equalsIgnoreCase(unit)) {
			y(n);
		}
	}

	/**
	 * 计算上阈值（近似进位用）。
	 * Compute the upper ceiling used for approximate rounding.
	 *
	 * @param x 单位毫秒数 / Unit size in milliseconds
	 * Upper ceiling
	 */
	private long upperCeiling(long x) {
		return (x / 100) * (100 - CEILING_PERCENTAGE);
	}

	/**
	 * 计算下阈值（近似舍去用）。
	 * Compute the lower ceiling used for approximate flooring.
	 *
	 * @param x 单位毫秒数 / Unit size in milliseconds
	 * Lower ceiling
	 */
	private long lowerCeiling(long x) {
		return (x / 100) * CEILING_PERCENTAGE;
	}

	/**
	 * 向上取整后的单位个数字符串。
	 * Ceil-divide d by n and return as string.
	 *
	 * @param d 被除数 / Dividend
	 * @param n 除数 / Divisor
	 * @return 结果字符串 / Result string
	 */
	private String ceil(long d, long n) {
		return Integer.toString((int) Math.ceil((double) d / n));
	}

	/**
	 * 向下取整后的单位个数字符串。
	 * Floor-divide d by n and return as string.
	 *
	 * @param d 被除数 / Dividend
	 * @param n 除数 / Divisor
	 * @return 结果字符串 / Result string
	 */
	private String floor(long d, long n) {
		return Integer.toString((int) Math.floor((double) d / n));
	}

	/**
	 * 增加 1 年。
	 * Add one year.
	 *
	 * @return 当前实例（链式） / This instance for chaining
	 */
	public HumanTime y() {
		return y(1);
	}

	/**
	 * 增加 n 年。
	 * Add n years.
	 *
	 * @param n 年数 / Number of years
	 * @return 当前实例（链式） / This instance for chaining
	 */
	public HumanTime y(int n) {
		delta += YEAR * Math.abs(n);
		return this;
	}

	/**
	 * 增加 1 天。
	 * Add one day.
	 *
	 * @return 当前实例（链式） / This instance for chaining
	 */
	public HumanTime d() {
		return d(1);
	}

	/**
	 * 增加 n 天。
	 * Add n days.
	 *
	 * @param n 天数 / Number of days
	 * @return 当前实例（链式） / This instance for chaining
	 */
	public HumanTime d(int n) {
		delta += DAY * Math.abs(n);
		return this;
	}

	/**
	 * 增加 1 小时。
	 * Add one hour.
	 *
	 * @return 当前实例（链式） / This instance for chaining
	 */
	public HumanTime h() {
		return h(1);
	}

	/**
	 * 增加 n 小时。
	 * Add n hours.
	 *
	 * @param n 小时数 / Number of hours
	 * @return 当前实例（链式） / This instance for chaining
	 */
	public HumanTime h(int n) {
		delta += HOUR * Math.abs(n);
		return this;
	}

	/**
	 * 增加 1 分钟。
	 * Add one minute.
	 *
	 * @return 当前实例（链式） / This instance for chaining
	 */
	public HumanTime m() {
		return m(1);
	}

	/**
	 * 增加 n 分钟。
	 * Add n minutes.
	 *
	 * @param n 分钟数 / Number of minutes
	 * @return 当前实例（链式） / This instance for chaining
	 */
	public HumanTime m(int n) {
		delta += MINUTE * Math.abs(n);
		return this;
	}

	/**
	 * 增加 1 秒。
	 * Add one second.
	 *
	 * @return 当前实例（链式） / This instance for chaining
	 */
	public HumanTime s() {
		return s(1);
	}

	/**
	 * 增加 n 秒。
	 * Add n seconds.
	 *
	 * @param n 秒数 / Number of seconds
	 * @return 当前实例（链式） / This instance for chaining
	 */
	public HumanTime s(int n) {
		delta += SECOND * Math.abs(n);
		return this;
	}

	/**
	 * 增加 1 毫秒。
	 * Add one millisecond.
	 *
	 * @return 当前实例（链式） / This instance for chaining
	 */
	public HumanTime ms() {
		return ms(1);
	}

	/**
	 * 增加 n 毫秒。
	 * Add n milliseconds.
	 *
	 * @param n 毫秒数 / Number of milliseconds
	 * @return 当前实例（链式） / This instance for chaining
	 */
	public HumanTime ms(int n) {
		delta += Math.abs(n);
		return this;
	}

	/**
	 * 获取精确的人类可读描述。
	 * Get the exact human-readable description.
	 *
	 * @return 精确描述字符串 / Exact description string
	 */
	public String getExactly() {
		return getExactly(new StringBuilder()).toString();
	}

	/**
	 * 将精确描述追加到给定 {@link Appendable}。
	 * Append the exact description to the given {@link Appendable}.
	 *
	 * Target appendable
	 * Appendable type
	 * The same appendable
	 */
	public <T extends Appendable> T getExactly(T a) {
		try {
			boolean prependBlank = false;
			long d = delta;
			if (d >= YEAR) {
				a.append(floor(d, YEAR));
				a.append(' ');
				a.append('y');
				prependBlank = true;
			}
			d %= YEAR;
			if (d >= DAY) {
				if (prependBlank) {
					a.append(' ');
				}
				a.append(floor(d, DAY));
				a.append(' ');
				a.append('d');
				prependBlank = true;
			}
			d %= DAY;
			if (d >= HOUR) {
				if (prependBlank) {
					a.append(' ');
				}
				a.append(floor(d, HOUR));
				a.append(' ');
				a.append('h');
				prependBlank = true;
			}
			d %= HOUR;
			if (d >= MINUTE) {
				if (prependBlank) {
					a.append(' ');
				}
				a.append(floor(d, MINUTE));
				a.append(' ');
				a.append('m');
				prependBlank = true;
			}
			d %= MINUTE;
			if (d >= SECOND) {
				if (prependBlank) {
					a.append(' ');
				}
				a.append(floor(d, SECOND));
				a.append(' ');
				a.append('s');
				prependBlank = true;
			}
			d %= SECOND;
			if (d > 0) {
				if (prependBlank) {
					a.append(' ');
				}
				a.append(Integer.toString((int) d));
				a.append(' ');
				a.append('m');
				a.append('s');
			}
		} catch (IOException ex) {
		}
		return a;
	}

	/**
	 * 获取近似的人类可读描述（最多约两个主要单位）。
	 * Get an approximate human-readable description (about two major units).
	 *
	 * @return 近似描述字符串 / Approximate description string
	 */
	public String getApproximately() {
		return getApproximately(new StringBuilder()).toString();
	}

	/**
	 * 将近似描述追加到给定 {@link Appendable}。
	 * Append the approximate description to the given {@link Appendable}.
	 *
	 * Target appendable
	 * Appendable type
	 * The same appendable
	 */
	public <T extends Appendable> T getApproximately(T a) {
		try {
			int parts = 0;
			boolean rounded = false;
			boolean prependBlank = false;
			long d = delta;
			long mod = d % YEAR;
			if (mod >= upperCeiling(YEAR)) {
				a.append(ceil(d, YEAR));
				a.append(' ');
				a.append('y');
				++parts;
				rounded = true;
				prependBlank = true;
			} else if (d >= YEAR) {
				a.append(floor(d, YEAR));
				a.append(' ');
				a.append('y');
				++parts;
				rounded = mod <= lowerCeiling(YEAR);
				prependBlank = true;
			}
			if (!rounded) {
				d %= YEAR;
				mod = d % DAY;
				if (mod >= upperCeiling(DAY)) {
					if (prependBlank) {
						a.append(' ');
					}
					a.append(ceil(d, DAY));
					a.append(' ');
					a.append('d');
					++parts;
					rounded = true;
					prependBlank = true;
				} else if (d >= DAY) {
					if (prependBlank) {
						a.append(' ');
					}
					a.append(floor(d, DAY));
					a.append(' ');
					a.append('d');
					++parts;
					rounded = mod <= lowerCeiling(DAY);
					prependBlank = true;
				}
				if (parts < 2) {
					d %= DAY;
					mod = d % HOUR;
					if (mod >= upperCeiling(HOUR)) {
						if (prependBlank) {
							a.append(' ');
						}
						a.append(ceil(d, HOUR));
						a.append(' ');
						a.append('h');
						++parts;
						rounded = true;
						prependBlank = true;
					} else if (d >= HOUR && !rounded) {
						if (prependBlank) {
							a.append(' ');
						}
						a.append(floor(d, HOUR));
						a.append(' ');
						a.append('h');
						++parts;
						rounded = mod <= lowerCeiling(HOUR);
						prependBlank = true;
					}
					if (parts < 2) {
						d %= HOUR;
						mod = d % MINUTE;
						if (mod >= upperCeiling(MINUTE)) {
							if (prependBlank) {
								a.append(' ');
							}
							a.append(ceil(d, MINUTE));
							a.append(' ');
							a.append('m');
							++parts;
							rounded = true;
							prependBlank = true;
						} else if (d >= MINUTE && !rounded) {
							if (prependBlank) {
								a.append(' ');
							}
							a.append(floor(d, MINUTE));
							a.append(' ');
							a.append('m');
							++parts;
							rounded = mod <= lowerCeiling(MINUTE);
							prependBlank = true;
						}
						if (parts < 2) {
							d %= MINUTE;
							mod = d % SECOND;
							if (mod >= upperCeiling(SECOND)) {
								if (prependBlank) {
									a.append(' ');
								}
								a.append(ceil(d, SECOND));
								a.append(' ');
								a.append('s');
								++parts;
								rounded = true;
								prependBlank = true;
							} else if (d >= SECOND && !rounded) {
								if (prependBlank) {
									a.append(' ');
								}
								a.append(floor(d, SECOND));
								a.append(' ');
								a.append('s');
								++parts;
								rounded = mod <= lowerCeiling(SECOND);
								prependBlank = true;
							}
							if (parts < 2) {
								d %= SECOND;

								if (d > 0 && !rounded) {
									if (prependBlank) {
										a.append(' ');
									}
									a.append(Integer.toString((int) d));
									a.append(' ');
									a.append('m');
									a.append('s');
								}
							}
						}
					}
				}
			}
		} catch (IOException ex) {
		}
		return a;
	}

	/**
	 * 获取内部毫秒增量。
	 * Get the internal millisecond delta.
	 *
	 * Delta in milliseconds
	 */
	public long getDelta() {
		return delta;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof HumanTime)) {
			return false;
		}
		return delta == ((HumanTime) obj).delta;
	}

	@Override
	public int hashCode() {
		return (int) (delta ^ (delta >> 32));
	}

	/**
	 * 默认字符串表示，等同于精确描述。
	 * Default string form, same as the exact description.
	 *
	 * Exact description
	 */
	@Override
	public String toString() {
		return getExactly();
	}

	/**
	 * 按毫秒增量比较。
	 * Compare by millisecond delta.
	 *
	 * Other HumanTime
	 * Comparison result
	 */
	public int compareTo(HumanTime t) {
		return delta == t.delta ? 0 : (delta < t.delta ? -1 : 1);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	/**
	 * 从外部输入读取 delta。
	 * Read delta from external input.
	 *
	 * @param in 输入流 / Input stream
	 * I/O exception
	 */
	public void readExternal(ObjectInput in) throws IOException {
		delta = in.readLong();
	}

	/**
	 * 将 delta 写出到外部输出。
	 * Write delta to external output.
	 *
	 * Output stream
	 *
	 * @param out I / O exception
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(delta);
	}
}
