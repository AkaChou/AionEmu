package com.aionemu.gameserver.dataholders;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.InstanceCooltime;
import com.aionemu.gameserver.services.instance.InstanceService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 副本冷却时间数据容器，按世界 ID 索引冷却配置并计算下次可进入时间。
 * Instance cooltime data holder, indexing cooltime configs by world id and computing next entry times.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "instance_cooltimes")
public class InstanceCooltimeData {

	@XmlElement(name = "instance_cooltime", required = true)
	protected List<InstanceCooltime> instanceCooltime;
	private Map<Integer, InstanceCooltime> instanceCooltimes = new LinkedHashMap<Integer, InstanceCooltime>();
	private HashMap<Integer, Integer> syncIdToMapId = new HashMap<Integer, Integer>();

	/**
	 * JAXB 反序列化完成后，按世界 ID 与同步 ID 建立索引并释放列表。
	 * After JAXB unmarshalling, indexes by world id and sync id, then clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (InstanceCooltime tmp : instanceCooltime) {
			instanceCooltimes.put(tmp.getWorldId(), tmp);
			syncIdToMapId.put(tmp.getId(), tmp.getWorldId());
		}
		instanceCooltime.clear();
	}

	/**
	 * 返回全部副本冷却配置映射。
	 * Returns the full instance cooltime map.
	 *
	 * @return 世界 ID 到冷却配置的映射 / map of world id to cooltime config
	 */
	public Map<Integer, InstanceCooltime> getAllInstances() {
		return instanceCooltimes;
	}

	/**
	 * 按世界 ID 获取副本冷却配置。
	 * Returns the instance cooltime config for the given world id.
	 *
	 * @param worldId 世界 ID / world id
	 * @return 冷却配置或 null / cooltime config or null
	 */
	public InstanceCooltime getInstanceCooltimeByWorldId(int worldId) {
		return instanceCooltimes.get(worldId);
	}

	/**
	 * 将同步 ID 转换为世界 ID。
	 * Converts a sync id to its world id.
	 *
	 * @param syncId 同步 ID / sync id
	 * @return 世界 ID，不存在则为 0 / world id or 0
	 */
	public int getWorldId(int syncId) {
		if (!syncIdToMapId.containsKey(syncId)) {
			return 0;
		}
		return syncIdToMapId.get(syncId);
	}

	/**
	 * 按同步 ID 计算玩家下次可进入副本的时间戳。
	 * Computes the next entry timestamp for the player by sync id.
	 *
	 * @param player 玩家 / player
	 * @param syncId 同步 ID / sync id
	 * @return 下次进入时间（毫秒），不存在则为 0 / next entry time in ms or 0
	 */
	public long getInstanceEntranceCooltimeById(Player player, int syncId) {
		if (!syncIdToMapId.containsKey(syncId)) {
			return 0;
		}
		return getInstanceEntranceCooltime(player, syncIdToMapId.get(syncId));
	}

	/**
	 * 按世界 ID 返回每日最大进入次数。
	 * Returns the max daily entry count for the given world id.
	 *
	 * @param worldId 世界 ID / world id
	 * @return 最大进入次数，无配置则为 0 / max entry count or 0
	 */
	public int getInstanceEntranceCountByWorldId(int worldId) {
		InstanceCooltime clt = getInstanceCooltimeByWorldId(worldId);
		if (clt != null) {
			return clt.getMaxEntriesCount();
		} else {
			return 0;
		}
	}

	/**
	 * 按世界 ID 计算玩家下次可进入副本的时间戳，支持日 / 周 / 相对冷却并应用冷却倍率。
	 * Computes the next entry timestamp for the player by world id, supporting daily / weekly / relative
	 * cooltimes and applying the instance cooldown rate.
	 *
	 * @param player 玩家 / player
	 * @param worldId 世界 ID / world id
	 * @return 下次进入时间（毫秒） / next entry time in ms
	 */
	public long getInstanceEntranceCooltime(Player player, int worldId) {
		int instanceCooldownRate = InstanceService.getInstanceRate(player, worldId);
		long instanceCoolTime = 0;
		InstanceCooltime clt = getInstanceCooltimeByWorldId(worldId);
		if (clt != null) {
			instanceCoolTime = clt.getEntCoolTime();
			if (clt.getCoolTimeType().isDaily()) {
				ZonedDateTime now = ZonedDateTime.now();
				int hour = (int) (clt.getEntCoolTime() / 100);
				ZonedDateTime repeatDate = now.withHour(hour).withMinute(0).withSecond(0).withNano(0);

				if (now.isAfter(repeatDate)) {
					repeatDate = repeatDate.plusDays(1);
				}
				instanceCoolTime = repeatDate.toInstant().toEpochMilli();

			} else if (clt.getCoolTimeType().isWeekly()) {
				String[] days = clt.getTypeValue().split(",");
				int hour = (int) (clt.getEntCoolTime() / 100);
				instanceCoolTime = getUpdateHours(days, hour);

			} else if (clt.getCoolTimeType().isRelative()) {
				switch (worldId) {
				case 300480000: // Sealed Danuar Mysticarium.
				case 300560000: // Shugo Imperial Tomb.
				case 301160000: // Nightmare Circus.
				case 301200000: // The Nightmare Circus.
				case 301320000: // Lucky Ophidan Bridge.
				case 301330000: // Lucky Danuar Reliquary.
				case 301400000: // The Shugo Emperor's Vault.
				case 301590000: // Emperor Trillirunerk's Safe.
				case 302350000: // Windy Gorge 5.5
				case 302370000: // 5.6
				case 302420000: // 5.6
					ZonedDateTime now = ZonedDateTime.now();
					ZonedDateTime repeatDate = now.withHour(9).withMinute(0).withSecond(0).withNano(0);
					if (now.isAfter(repeatDate)) {
						repeatDate = repeatDate.plusDays(1);
					}
					instanceCoolTime = repeatDate.toInstant().toEpochMilli();
					// 注意：原版有两种计算，为兼容性保留两者。 / Note: The original had both calculations, keeping both for compatibility
					instanceCoolTime = System.currentTimeMillis() + (clt.getEntCoolTime() * 60 * 1000);
					break;
				}
			}
		}
		if (instanceCooldownRate != 1) {
			instanceCoolTime = System.currentTimeMillis() + ((instanceCoolTime - System.currentTimeMillis()) / instanceCooldownRate);
		}
		return instanceCoolTime;
	}

	private long getUpdateHours(String[] days, int hour) {
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime repeatDate = now.withHour(hour).withMinute(0).withSecond(0).withNano(0);

		int currentDay = now.getDayOfWeek().getValue(); // 1 (Monday) to 7 (Sunday)

		for (String name : days) {
			int day = getDay(name);
			if (day < currentDay) {
				continue;
			}
			if (day == currentDay) {
				if (now.isBefore(repeatDate)) {
					return repeatDate.toInstant().toEpochMilli();
				}
			} else {
				repeatDate = repeatDate.plusDays(day - currentDay);
				return repeatDate.toInstant().toEpochMilli();
			}
		}

		// 若本周所有天已过，取下周第一天 / If all days passed, take the first day of next week
		int firstDay = getDay(days[0]);
		repeatDate = repeatDate.plusDays((7 - currentDay) + firstDay);
		return repeatDate.toInstant().toEpochMilli();
	}

	private int getDay(String day) {
		switch (day) {
			case "Mon": return 1;
			case "Tue": return 2;
			case "Wed": return 3;
			case "Thu": return 4;
			case "Fri": return 5;
			case "Sat": return 6;
			case "Sun": return 7;
			default: throw new IllegalArgumentException("Invalid Day: " + day);
		}
	}

	/**
	 * 返回已加载的副本冷却配置数量。
	 * Returns the number of loaded instance cooltime configs.
	 *
	 * @return 已加载的副本冷却配置数量 / Returns the number of loaded instance cooltime configs.
	 */
	public Integer size() {
		return instanceCooltimes.size();
	}
}
