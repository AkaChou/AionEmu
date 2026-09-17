package com.aionemu.gameserver.model.templates.spawns;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.utils.gametime.GameTime;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import lombok.Getter;

/**
 * Temporary 刷新点模板（静态数据/XML）。
 * XML template.
 */

@Getter
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "TemporarySpawn")
public class TemporarySpawn {
	/** 返回刷新时间 / Returns the spawn time*/
	@XmlAttribute(name = "spawn_time")
	private String spawnTime;

	/** 返回消失时间 / Returns the despawn time*/
	@XmlAttribute(name = "despawn_time")
	private String despawnTime;

	/**
	 * 解析后的刷新时间窗缓存（小时/日/月；{@code *} 与缺省为 {@code null}）。
	 * Cached parsed spawn window (hour/day/month; {@code *} and absent segments are {@code null}).
	 *
	 * <p>XML 在加载期一次性注入字符串，之后模板不再变化；因此首次使用时解析并缓存，避免每次
	 * {@link #isInSpawnTime()} 都对同一字符串做 6 次 {@code String.split("\\.")}（JFR 实测 300s 内占 17.2% 分配）。
	 * The XML injects the strings once at load time and templates never change afterwards, so the window is parsed on
	 * first use and cached instead of running six {@code String.split("\\.")} calls per {@link #isInSpawnTime()}.</p>
	 */
	private volatile Integer[] spawnTimeParts;
	private volatile Integer[] despawnTimeParts;

	/** 获取刷新小时 / Gets the spawn hour */
	public Integer geSpawnHour() {
		return spawnTimeParts()[0];
	}

	/** 获取刷新日 / Gets the spawn day */
	public Integer geSpawnDay() {
		return spawnTimeParts()[1];
	}

	/** 返回刷新月 / Returns the spawn month */
	public Integer getSpawnMonth() {
		return spawnTimeParts()[2];
	}

	/** 获取消失小时 / Gets the despawn hour. */
	public Integer geDespawnHour() {
		return despawnTimeParts()[0];
	}

	/** 获取消失日 / Gets the despawn day. */
	public Integer geDespawnDay() {
		return despawnTimeParts()[1];
	}

	/** 返回消失月 / Returns the despawn month */
	public Integer getDespawnMonth() {
		return despawnTimeParts()[2];
	}

	/**
	 * 刷新时间窗（首次调用时解析并缓存）。
	 * The spawn window, parsed and cached on first use.
	 *
	 * @return 长度 3 的数组 / a three-element array
	 */
	private Integer[] spawnTimeParts() {
		Integer[] parts = spawnTimeParts;
		if (parts == null) {
			parts = parseTime(spawnTime);
			spawnTimeParts = parts;
		}
		return parts;
	}

	/**
	 * 消失时间窗（首次调用时解析并缓存）。
	 * The despawn window, parsed and cached on first use.
	 *
	 * @return 长度 3 的数组 / a three-element array
	 */
	private Integer[] despawnTimeParts() {
		Integer[] parts = despawnTimeParts;
		if (parts == null) {
			parts = parseTime(despawnTime);
			despawnTimeParts = parts;
		}
		return parts;
	}

	/**
	 * 解析 {@code 时.日.月} 形式的时间窗；{@code *} 表示“不限”，整体为 {@code null} 时三段都是 {@code null}。
	 * Parses a {@code hour.day.month} window; {@code *} means "any" and a {@code null} input yields three {@code null}s.
	 *
	 * @param time 原始时间串 / raw time string
	 * @return 长度 3 的数组 / a three-element array
	 */
	private static Integer[] parseTime(String time) {
		Integer[] parts = new Integer[3];
		if (time == null) {
			return parts;
		}
		// 与旧实现保持同样的索引语义：段数不足时抛出数组越界。 / Same indexing contract as before: a short string raises an index error.
		String[] values = time.split("\\.");
		for (int i = 0; i < parts.length; i++) {
			String result = values[i];
			parts[i] = result.equals("*") ? null : Integer.parseInt(result);
		}
		return parts;
	}

	private boolean isTime(Integer hour, Integer day, Integer month) {
		GameTime gameTime = GameTimeManager.getGameTime();
		if (hour != null && hour == gameTime.getHour()) {
			if (day == null) {
				return true;
			}
			if (day == gameTime.getDay()) {
				return month == null || month == gameTime.getMonth();
			}
		}
		return false;
	}

	/** 是否可以刷新点。 / Whether spawn. */
	public boolean canSpawn() {
		return isTime(geSpawnHour(), geSpawnDay(), getSpawnMonth());
	}

	/** 是否消失 / Whether despawn*/
	public boolean canDespawn() {
		return isTime(geDespawnHour(), geDespawnDay(), getDespawnMonth());
	}

	/** 是否刷新时间 / Whether in spawn time */
	public boolean isInSpawnTime() {
		GameTime gameTime = GameTimeManager.getGameTime();
		Integer spawnHour = geSpawnHour();
		Integer spawnDay = geSpawnDay();
		Integer spawnMonth = getSpawnMonth();
		Integer despawnHour = geDespawnHour();
		Integer despawnDay = geDespawnDay();
		Integer despawnMonth = getDespawnMonth();
		int curentHour = gameTime.getHour();
		int curentDay = gameTime.getDay();
		int curentMonth = gameTime.getMonth();
		if (spawnMonth != null) {
			if (!checkTime(curentMonth, spawnMonth, despawnMonth)) {
				return false;
			}
		}
		if (spawnDay != null) {
			if (!checkTime(curentDay, spawnDay, despawnDay)) {
				return false;
			}
		}
		return spawnMonth != null || spawnDay != null || checkHour(curentHour, spawnHour, despawnHour);
	}

	private boolean checkTime(int curentTime, int spawnTime, int despawnTime) {
		if (spawnTime < despawnTime) {
			return curentTime >= spawnTime && curentTime <= despawnTime;
		} else if (spawnTime > despawnTime) {
			return curentTime >= spawnTime || curentTime <= despawnTime;
		}
		return true;
	}

	private boolean checkHour(int curentTime, int spawnTime, int despawnTime) {
		if (spawnTime < despawnTime) {
			return curentTime >= spawnTime && curentTime < despawnTime;
		} else if (spawnTime > despawnTime) {
			return curentTime >= spawnTime || curentTime < despawnTime;
		}
		return true;
	}
}
