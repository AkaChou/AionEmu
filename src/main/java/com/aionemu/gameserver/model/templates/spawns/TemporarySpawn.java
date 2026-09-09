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

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "TemporarySpawn")
public class TemporarySpawn {
	/** 返回刷新时间 / Returns the spawn time*/
	@Getter
	@XmlAttribute(name = "spawn_time")
	private String spawnTime;

	/** 返回消失时间 / Returns the despawn time*/
	@Getter
	@XmlAttribute(name = "despawn_time")
	private String despawnTime;

	/** 获取刷新小时 / Gets the spawn hour */
	public Integer geSpawnHour() {
		return getTime(spawnTime, 0);
	}

	/** 获取刷新日 / Gets the spawn day */
	public Integer geSpawnDay() {
		return getTime(spawnTime, 1);
	}

	/** 返回刷新月 / Returns the spawn month */
	public Integer getSpawnMonth() {
		return getTime(spawnTime, 2);
	}

	/** 获取消失小时 / Gets the despawn hour. */
	public Integer geDespawnHour() {
		return getTime(despawnTime, 0);
	}

	/** 获取消失日 / Gets the despawn day. */
	public Integer geDespawnDay() {
		return getTime(despawnTime, 1);
	}

	/** 返回消失月 / Returns the despawn month */
	public Integer getDespawnMonth() {
		return getTime(despawnTime, 2);
	}

	private Integer getTime(String time, int type) {
		if (time == null) {
			return null;
		}
		String result = time.split("\\.")[type];
		if (result.equals("*")) {
			return null;
		}
		return Integer.parseInt(result);
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
