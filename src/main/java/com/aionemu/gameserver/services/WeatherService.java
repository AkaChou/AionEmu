package com.aionemu.gameserver.services;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.world.WeatherEntry;
import com.aionemu.gameserver.model.templates.world.WeatherTable;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_WEATHER;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.gametime.DayTime;
import com.aionemu.gameserver.utils.gametime.GameTime;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import com.aionemu.gameserver.world.World;

/**
 * 天气服务，按地图区域维护天气条目，并在变更时同步客户端与攻城系统。
 * Weather service that maintains per-map zone weather entries and syncs clients and siege systems on change.
 */
public class WeatherService {
	private static volatile ObjectProvider<WeatherService> instanceProvider;
	/** 各地图天气键到区域天气数组的映射。 / Map of weather keys to per-zone weather entry arrays. */
	private final Map<WeatherKey, WeatherEntry[]> worldZoneWeathers;

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 *
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static final WeatherService getInstance() {
		ObjectProvider<WeatherService> provider = instanceProvider;
		WeatherService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("WeatherService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 注入 Spring ObjectProvider。
	 * Injects the Spring ObjectProvider.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<WeatherService> instanceProvider) {
		WeatherService.instanceProvider = instanceProvider;
	}

	/**
	 * 初始化所有带天气表的地图区域天气。
	 * Initializes weather for all maps that have a weather table.
	 */
	public WeatherService() {
		worldZoneWeathers = new HashMap<WeatherKey, WeatherEntry[]>();
		GameTime gameTime = (GameTime) GameTimeManager.getGameTime().clone();
		for (Iterator<WorldMapTemplate> mapIterator = DataManager.WORLD_MAPS_DATA.iterator(); mapIterator.hasNext();) {
			int mapId = mapIterator.next().getMapId();
			WeatherTable table = DataManager.MAP_WEATHER_DATA.getWeather(mapId);
			if (table != null) {
				WeatherKey key = new WeatherKey(gameTime, mapId);
				worldZoneWeathers.put(key, new WeatherEntry[table.getZoneCount()]);
				setNextWeather(key);
			}
		}
	}

	/**
	 * 地图天气键，按 mapId 相等比较。
	 * Weather key for a map; equality is based on mapId only.
	 */
	private class WeatherKey {
		private GameTime created;
		private final int mapId;

		public WeatherKey(GameTime createdTime, int mapId) {
			this.created = createdTime;
			this.mapId = mapId;
		}

		public int getMapId() {
			return mapId;
		}

		public GameTime getCreatedTime() {
			return created;
		}

		@Override
		public boolean equals(Object o) {
			WeatherKey other = (WeatherKey) o;
			return this.mapId == other.mapId;
		}

		@Override
		public int hashCode() {
			return Integer.valueOf(mapId).hashCode();
		}
	}

	/**
	 * 检查并推进所有地图的天气到下一阶段，随后广播变更。
	 * Advances weather for all maps to the next stage and broadcasts the change.
	 */
	public void checkWeathersTime() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				for (WeatherKey key : worldZoneWeathers.keySet()) {
					setNextWeather(key);
					onWeatherChange(key.getMapId(), null);
				}
			}
		}, 0);
	}

	/**
	 * 为指定地图键计算并写入下一轮区域天气。
	 * Computes and stores the next weather entries for the given map key.
	 *
	 * weather key
	 */
	private synchronized void setNextWeather(WeatherKey key) {
		WeatherEntry[] weatherEntries = getWeatherEntries(key.getMapId());
		WeatherTable table = DataManager.MAP_WEATHER_DATA.getWeather(key.getMapId());
		key.created = (GameTime) GameTimeManager.getGameTime().clone();
		for (int zoneIndex = 0; zoneIndex < weatherEntries.length; zoneIndex++) {
			WeatherEntry oldEntry = weatherEntries[zoneIndex];
			WeatherEntry newEntry = null;
			if (oldEntry == null) {
				newEntry = getRandomWeather(key.getCreatedTime(), table, zoneIndex + 1);
			} else {
				newEntry = table.getWeatherAfter(oldEntry);
				if (newEntry == null) {
					newEntry = getRandomWeather(key.getCreatedTime(), table, zoneIndex + 1);
				}
			}
			weatherEntries[zoneIndex] = newEntry;
		}
	}

	/**
	 * 按属性等级与时段修正随机选取天气条目；前兆（before）与残留（after）档不作为独立天气参与抽取。
	 * Randomly picks a weather entry by attribute ranking with daytime correction; sign (before) and remain
	 * (after) entries never roll as standalone weather.
	 *
	 * @param createdTime 天气创建时间，用于时段修正 / creation time used for the daytime correction
	 * @param table 该地图的天气表 / weather table of the map
	 * @param zoneId 天气区序号 / weather-zone ordinal
	 * @return 选中的天气条目；未选中时为该区域的放晴条目 / the chosen entry, or a clear-weather entry for the zone
	 */
	private WeatherEntry getRandomWeather(GameTime createdTime, WeatherTable table, int zoneId) {
		List<WeatherEntry> weathers = table.getWeathersForZone(zoneId);
		int attRanking = 2;
		int chance = Rnd.get(1, 100);
		if (chance > 33) {
			attRanking = 0;
		} else if (chance > 50) {
			attRanking = 1;
		}
		List<WeatherEntry> chosenWeather = new ArrayList<WeatherEntry>();
		while (attRanking >= 0) {
			for (WeatherEntry entry : weathers) {
				if (entry.getAttRanking() == -1) {
					return entry;
				}
				// 前兆/残留档只作为过渡态，不能被抽成当前天气。
				// Sign and remain entries are transitional states and must never become the current weather.
				if (entry.isBefore() || entry.isAfter()) {
					continue;
				}
				if (entry.getAttRanking() == attRanking) {
					chosenWeather.add(entry);
				}
			}
			if (chosenWeather.size() > 0) {
				attRanking = -1;
				break;
			}
			attRanking--;
		}
		WeatherEntry newWeather = null;
		if (chosenWeather.size() == 0) {
			newWeather = clearWeather(zoneId);
		} else {
			newWeather = chosenWeather.get(Rnd.get(chosenWeather.size()));
			int dayTimeCorrection = 1;
			if (createdTime.getDayTime() == DayTime.AFTERNOON) {
				dayTimeCorrection *= 2;
				chance = Rnd.get(1, 100);
			}
			if ((newWeather.getAttRanking() == 0 && chance > 33 / dayTimeCorrection)
					|| (newWeather.getAttRanking() == 1 && chance > 50 / dayTimeCorrection)
					|| (newWeather.getAttRanking() == 2 && chance > 66 / dayTimeCorrection)) {
				newWeather = clearWeather(zoneId);
			}
		}
		return newWeather;
	}

	/**
	 * 生成某个天气区的放晴条目，并保留正确的天气区序号。
	 * Creates a clear-weather entry for the given zone, keeping the correct weather-zone ordinal.
	 *
	 * @param zoneId 天气区序号 / weather-zone ordinal
	 * @return 放晴条目（code=0） / clear-weather entry (code=0)
	 */
	private WeatherEntry clearWeather(int zoneId) {
		return new WeatherEntry(zoneId, 0);
	}

	/**
	 * 为玩家加载当前地图天气。
	 * Loads current map weather for the given player.
	 *
	 * target player
	 */
	public void loadWeather(Player player) {
		onWeatherChange(player.getWorldId(), player);
	}

	/**
	 * 按地图 ID 查找天气键。
	 * Finds the weather key by map id.
	 *
	 * map id
	 * weather key or null
	 */
	private WeatherKey getWeatherKeyByMapId(int mapId) {
		for (WeatherKey key : worldZoneWeathers.keySet()) {
			if (key.getMapId() == mapId) {
				return key;
			}
		}
		return null;
	}

	/**
	 * 获取指定地图的区域天气数组。
	 * Returns the per-zone weather array for the map.
	 *
	 * map id
	 *
	 * @param mapId
	 * @return 天气条目数组 / weather entry array
	 */
	private WeatherEntry[] getWeatherEntries(int mapId) {
		WeatherKey key = getWeatherKeyByMapId(mapId);
		if (key == null) {
			return null;
		}
		return worldZoneWeathers.get(key);
	}

	/**
	 * 强制将指定地图所有区域天气改为给定代码。
	 * Forces all weather zones of the map to the given weather code.
	 *
	 * map id
	 * weather code
	 */
	public synchronized void changeRegionWeather(int mapId, int weatherCode) {
		WeatherKey key = new WeatherKey(null, mapId);
		WeatherEntry[] weatherEntries = worldZoneWeathers.get(key);
		if (weatherEntries == null) {
			return;
		}
		for (int i = 0; i < weatherEntries.length; i++) {
			// 天气区序号固定取 i + 1，避免旧条目 zoneId=0 污染后续查询。
			// The zone ordinal is always i + 1 so a polluted zone id (0) can never leak into later lookups.
			weatherEntries[i] = new WeatherEntry(i + 1, weatherCode);
		}
		onWeatherChange(mapId, null);
	}

	/**
	 * 重置所有已加载地图天气为晴朗（code 0）。
	 * Resets weather of all loaded maps to clear (code 0).
	 */
	public synchronized void resetWeather() {
		Set<WeatherKey> loadedWeathers = new HashSet<WeatherKey>(worldZoneWeathers.keySet());
		for (WeatherKey key : loadedWeathers) {
			WeatherEntry[] oldEntries = worldZoneWeathers.get(key);
			if (oldEntries == null) {
				continue;
			}
			for (int i = 0; i < oldEntries.length; i++) {
				oldEntries[i] = clearWeather(i + 1);
			}
			onWeatherChange(key.getMapId(), null);
		}
	}

	/**
	 * 查询地图指定天气区域的天气代码。
	 * Returns the weather code for a map weather zone.
	 *
	 * map id
	 * weather zone id
	 * weather code
	 */
	public int getWeatherCode(int mapId, int weatherZoneId) {
		WeatherEntry[] weatherEntries = getWeatherEntries(mapId);
		if (weatherEntries == null) {
			return 0;
		}
		for (WeatherEntry entry : weatherEntries) {
			if (entry != null && entry.getZoneId() == weatherZoneId) {
				return entry.getCode();
			}
		}
		return 0;
	}

	/**
	 * 返回指定地图当前天气条目的只读快照，供管理命令排查展示。
	 * Returns a read-only snapshot of the given map's current weather entries for admin diagnostics.
	 *
	 * @param mapId 地图 ID / map id
	 * @return 天气条目快照；该地图没有天气表时为空数组 / weather-entry snapshot; empty when the map has no weather table
	 */
	public WeatherEntry[] getWeatherSnapshot(int mapId) {
		WeatherEntry[] weatherEntries = getWeatherEntries(mapId);
		return weatherEntries == null ? new WeatherEntry[0] : weatherEntries.clone();
	}

	/**
	 * 天气变更后向玩家发送 SM_WEATHER，并通知攻城服务。
	 * After weather change, sends SM_WEATHER to players and notifies the siege service.
	 *
	 * map id
	 * @param player 单播目标；null 表示广播该地图所有在线玩家 / unicast target; null broadcasts to all online players on the map
	 */
	private void onWeatherChange(int mapId, Player player) {
		WeatherEntry[] weatherEntries = getWeatherEntries(mapId);
		if (weatherEntries == null) {
			return;
		}
		if (player == null) {
			for (Iterator<Player> playerIterator = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator(); playerIterator
					.hasNext();) {
				Player currentPlayer = playerIterator.next();
				if (!currentPlayer.isSpawned()) {
					continue;
				}
				if (currentPlayer.getWorldId() == mapId) {
					PacketSendUtility.sendPacket(currentPlayer, new SM_WEATHER(weatherEntries));
				}
			}
		} else {
			PacketSendUtility.sendPacket(player, new SM_WEATHER(weatherEntries));
		}
		for (WeatherEntry entry : weatherEntries) {
			GameFeatureServices.siegeService().onWeatherChanged(entry);
		}
	}
}
