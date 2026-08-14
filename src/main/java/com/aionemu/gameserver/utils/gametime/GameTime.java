package com.aionemu.gameserver.utils.gametime;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import java.security.InvalidParameterException;

import com.aionemu.gameserver.services.WeatherService;
import com.aionemu.gameserver.spawnengine.TemporarySpawnEngine;

/**
 * Aion 世界内部游戏时钟（年/月/日/时/分，独立于真实时间）。
 * In-game clock for the Aion world (year/month/day/hour/minute, independent of real time).
 *
 * @author Ben
 * @author vlog
 */
public class GameTime implements Cloneable {

	/**
	 * 一小时的分钟数。
	 * Minutes in an hour.
	 */
	private static final int MINUTES_IN_HOUR = 60;
	/**
	 * 一天的分钟数。
	 * Minutes in a day.
	 */
	private static final int MINUTES_IN_DAY = MINUTES_IN_HOUR * 24;
	/**
	 * 一年的分钟数（固定月份天数）。
	 * Minutes in a year (fixed month lengths).
	 */
	private static final int MINUTES_IN_YEAR = (31 * 7 + 30 * 4 + 28 * 1) * MINUTES_IN_DAY;
	/**
	 * 自 01.01.0000 00:00 起的游戏分钟数。
	 * Game minutes since 01.01.0000 00:00.
	 */
	private int gameTime = 0;
	/**
	 * 当前时段。
	 * Current day-time period.
	 */
	private DayTime dayTime;

	/**
	 * 游戏月份及其天数。
	 * Game months and their day counts.
	 */
	private enum Monthes {

		JANUARY(31), FEBRUARY(28), MARCH(31), APRIL(30), MAY(31), JUNE(30), JULY(31), AUGUST(31), SEPTEMBER(30),
		OCTOBER(31), NOVEMBER(30), DECEMBER(31);

		/**
		 * 该月天数。
		 * Days in this month.
		 */
		private int _days;

		/**
		 * @param days 天数 / Day count
		 */
		Monthes(int days) {
			_days = days;
		}

		/**
		 * 获取该月天数。
		 * Get days in this month.
		 *
		 * @return 天数 / Days
		 */
		public int getDays() {
			return _days;
		}
	};

	/**
	 * 以自 01.01.0000 起的分钟数构造游戏时间。
	 * Construct game time from minutes since 01.01.0000.
	 *
	 * @param time 自 01.01.0000 午夜起的分钟数 / Minutes since midnight 01.01.0000
	 */
	public GameTime(int time) {
		if (time < 0) {
			throw new InvalidParameterException("Time must be >= 0");
		}
		gameTime = time;
		calculateDayTime();
	}

	/**
	 * 获取该月对应的游戏分钟数。
	 * Minutes contained in the given month.
	 *
	 * @param m 月份 / Month
	 * @return 该月分钟数 / Minutes in this month
	 */
	public int getProperMinutesInMonth(Monthes m) {
		return m.getDays() * MINUTES_IN_DAY;
	}

	/**
	 * 获取游戏时间总分钟数。
	 * Get total in-game minutes.
	 *
	 * @return 自 01.01.0000 00:00:00 起的分钟数 / Minutes since 01.01.0000 00:00:00
	 */
	public int getTime() {
		return gameTime;
	}

	/**
	 * 将游戏时间增加一分钟；整点时检查时段变化。
	 * Increase game time by one minute; check day-time change on the hour.
	 */
	public void increase() {
		gameTime++;
		if (getMinute() == 0) {
			checkDayTimeChange();
		}
	}

	/**
	 * 重新计算时段，触发整点与时段变更事件。
	 * Recalculate day-time and fire hour/day-time change events.
	 */
	public void checkDayTimeChange() {
		DayTime oldDayTime = this.dayTime;
		calculateDayTime();
		onHourChange();
		if (oldDayTime != this.dayTime) {
			onDayTimeChange();
		}
	}

	/**
	 * 根据当前小时计算时段。
	 * Calculate day-time period from the current hour.
	 */
	public void calculateDayTime() {
		int hour = getHour();
		if (hour > 21 || hour < 4) {
			dayTime = DayTime.NIGHT;
		} else if (hour > 16) {
			dayTime = DayTime.EVENING;
		} else if (hour > 8) {
			dayTime = DayTime.AFTERNOON;
		} else {
			dayTime = DayTime.MORNING;
		}
	}

	/**
	 * 整点回调：通知临时刷新引擎。
	 * Hour-change callback: notify temporary spawn engine.
	 */
	private void onHourChange() {
		TemporarySpawnEngine.onHourChange();
	}

	/**
	 * 时段变更回调：检查天气时间。
	 * Day-time change callback: check weather time.
	 */
	private void onDayTimeChange() {
		GameRuntimeServices.weatherService().checkWeathersTime();
	}

	/**
	 * 获取游戏年份（0 起）。
	 * Get game year (from 0).
	 *
	 * @return 年份 / Year
	 */
	public int getYear() {
		return gameTime / MINUTES_IN_YEAR;
	}

	/**
	 * 获取游戏月份（1–12）。
	 * Get game month (1–12).
	 *
	 * @return 月份 1–12 / Month 1–12
	 */
	public int getMonth() {
		int answer = 1;
		int minutesInYear = gameTime % MINUTES_IN_YEAR;
		for (Monthes m : Monthes.values()) {
			if ((minutesInYear - getProperMinutesInMonth(m)) > 0) {
				minutesInYear = minutesInYear - getProperMinutesInMonth(m);
				answer = answer + 1;
			} else if ((minutesInYear - getProperMinutesInMonth(m)) == 0) {
				answer = answer + 1;
				break;
			} else {
				break;
			}
		}
		return answer;
	}

	/**
	 * 获取游戏日（1–当月天数）。
	 * Get game day (1–days in month).
	 *
	 * @return 日 / Day
	 */
	public int getDay() {
		int answer = 1;
		int minutesInYear = gameTime % MINUTES_IN_YEAR;
		for (Monthes m : Monthes.values()) {
			if ((minutesInYear - getProperMinutesInMonth(m)) > 0) {
				minutesInYear = minutesInYear - getProperMinutesInMonth(m);
			} else if ((minutesInYear - getProperMinutesInMonth(m)) == 0) {
				break;
			} else {
				answer = minutesInYear / MINUTES_IN_DAY + 1;
				break;
			}
		}
		return answer;
	}

	/**
	 * 获取游戏小时（0–23）。
	 * Get game hour (0–23).
	 *
	 * @return 小时 0–23 / Hour 0–23
	 */
	public int getHour() {
		return (gameTime % MINUTES_IN_DAY) / (MINUTES_IN_HOUR);
	}

	/**
	 * 获取游戏分钟（0–59）。
	 * Get game minute (0–59).
	 *
	 * @return 分钟 0–59 / Minute 0–59
	 */
	public int getMinute() {
		return (gameTime % MINUTES_IN_HOUR);
	}

	/**
	 * 获取当前时段。
	 * Get current day-time period.
	 *
	 * @return 时段 / DayTime
	 */
	public DayTime getDayTime() {
		return dayTime;
	}

	/**
	 * 将游戏时间换算为近似真实时间（÷12）。
	 * Convert game time to approximate real time (÷12).
	 *
	 * @return 换算后的值 / Converted value
	 * @author vlog
	 */
	public int convertTime() {
		return this.getTime() / 12;
	}

	/**
	 * 减去给定游戏时间，返回新实例。
	 * Subtract the given game time; returns a new instance.
	 *
	 * @param gt 要减去的时间 / Time to subtract
	 * @return 新游戏时间 / New game time
	 */
	public GameTime minus(GameTime gt) {
		return new GameTime(this.getTime() - gt.getTime());
	}

	/**
	 * 加上给定游戏时间，返回新实例。
	 * Add the given game time; returns a new instance.
	 *
	 * @param gt 要加上的时间 / Time to add
	 * @return 新游戏时间 / New game time
	 */
	public GameTime plus(GameTime gt) {
		return new GameTime(this.getTime() + gt.getTime());
	}

	/**
	 * 是否大于给定游戏时间。
	 * Whether this time is greater than the given one.
	 *
	 * @param gt 比较对象 / Other game time
	 * @return 若大于则为 true / True if greater
	 */
	public boolean isGreaterThan(GameTime gt) {
		return this.getTime() > gt.getTime();
	}

	/**
	 * 是否小于给定游戏时间。
	 * Whether this time is less than the given one.
	 *
	 * @param gt 比较对象 / Other game time
	 * @return 若小于则为 true / True if less
	 */
	public boolean isLessThan(GameTime gt) {
		return this.getTime() < gt.getTime();
	}

	/**
	 * 按总分钟数比较相等。
	 * Equality by total minutes.
	 *
	 * @param o 对象 / Object
	 * @return 若相等则为 true / True if equal
	 * @author vlog
	 */
	@Override
	public boolean equals(Object o) {
		return o instanceof GameTime other && gameTime == other.gameTime;
	}

	/**
	 * 按总分钟数生成哈希码。
	 * Hash code by total minutes.
	 *
	 * @return 哈希码 / Hash code
	 */
	@Override
	public int hashCode() {
		return Integer.hashCode(gameTime);
	}

	/**
	 * 克隆为相同分钟数的新实例。
	 * Clone as a new instance with the same minutes.
	 *
	 * @return 克隆实例 / Clone
	 */
	@Override
	public Object clone() {
		return new GameTime(gameTime);
	}
}
