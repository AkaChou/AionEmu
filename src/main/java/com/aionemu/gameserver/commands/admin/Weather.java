package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.world.WeatherEntry;
import com.aionemu.gameserver.model.templates.world.WeatherTable;
import com.aionemu.gameserver.model.templates.zone.ZoneClassName;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.WorldMapType;
import com.aionemu.gameserver.world.zone.ZoneInstance;

import java.util.List;

/**
 * 查询或修改地图天气的管理员命令。
 * Admin command to query or change map weather.
 */
public class Weather extends AdminCommand
{
	/**
	 * 构造 weather 命令。
	 * Creates the weather command.
	 */
	public Weather() {
		super("weather");
	}

	/**
	 * 无参时显示当前区域天气；否则按地图名设置或 reset。
	 * With no args shows current zone weather; otherwise sets by map name or resets.
	 * @param admin 执行 GM / Admin player
	 * @param params 地图名与天气码或 reset / Map name and weather code, or reset
	 */
	@Override
	public void execute(Player admin, String... params) {
		String regionName = null;
		if (params.length == 0) {
			int weatherCode = -1;
			int weatherZoneId = 0;
			List<ZoneInstance> zones = admin.getActiveRegion().getZones(admin);
			for (ZoneInstance regionZone : zones) {
				if (regionZone.getZoneTemplate().getZoneType() == ZoneClassName.WEATHER) {
					weatherZoneId = DataManager.ZONE_DATA.getWeatherZoneId(regionZone.getZoneTemplate());
					weatherCode = GameRuntimeServices.weatherService().getWeatherCode(admin.getWorldId(), weatherZoneId);
					regionName = regionZone.getZoneTemplate().getXmlName();
					break;
				}
			} if (weatherCode == -1) {
				PacketSendUtility.sendMessage(admin, "No weather.");
			} else {
				// 一并打印天气区序号：未登记的 zone id 与服务端晴天都会输出 0，只给代码无法区分。
				// Also print the weather-zone ordinal: an unregistered zone id and a real clear state both read as 0.
				PacketSendUtility.sendMessage(admin, "Weather code for region " + regionName + " (weather zone "
					+ weatherZoneId + ") is " + weatherCode);
			}
			// 玩家可能站在天气区外，故无条件打印整图快照，便于排查客户端与服务器天气不一致。
			// The player may stand outside every weather zone, so always print the whole-map snapshot.
			PacketSendUtility.sendMessage(admin,
				"Server weather of map " + admin.getWorldId() + ": " + describeWeather(admin.getWorldId()));
			return;
		} if (params.length > 2) {
			onFail(admin, null);
			return;
		}
		int weatherType = -1;
		regionName = params[0];
		if (params.length == 2) {
			try {
				weatherType = Integer.parseInt(params[1]);
			} catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "weather type parameter need to be an integer [0-12].");
				return;
			}
		} if (regionName.equals("reset")) {
			GameRuntimeServices.weatherService().resetWeather();
			return;
		}
		WorldMapType region = null;
		for (WorldMapType worldMapType : WorldMapType.values()) {
			if (worldMapType.name().equalsIgnoreCase(regionName)) {
				region = worldMapType;
				break;
			}
		} if (region != null) {
			if (weatherType > -1 && weatherType < 13) {
				WeatherTable table = DataManager.MAP_WEATHER_DATA.getWeather(region.getId());
				if (table == null || table.getZoneCount() == 0) {
					PacketSendUtility.sendMessage(admin, "Region has no weather defined");
					return;
				}
				GameRuntimeServices.weatherService().changeRegionWeather(region.getId(), weatherType);
			} else {
				PacketSendUtility.sendMessage(admin, "Weather type must be between 0 and 12");
			}
		} else {
			PacketSendUtility.sendMessage(admin, "Region " + regionName + " not found");
		}
	}

	/**
	 * 汇总指定地图的服务端天气快照，形如 {@code zone 1=1(Sand_Rain)}。
	 * Summarizes the server-side weather snapshot of a map, e.g. {@code zone 1=1(Sand_Rain)}.
	 * @param mapId 地图 ID / map id
	 * @return 快照文本；无天气表时为 {@code no weather table} / snapshot text, or {@code no weather table}
	 */
	private String describeWeather(int mapId) {
		WeatherEntry[] entries = GameRuntimeServices.weatherService().getWeatherSnapshot(mapId);
		if (entries.length == 0) {
			return "no weather table";
		}
		StringBuilder description = new StringBuilder();
		for (int i = 0; i < entries.length; i++) {
			if (i > 0) {
				description.append(", ");
			}
			WeatherEntry entry = entries[i];
			description.append("zone ").append(i + 1).append('=');
			if (entry == null) {
				description.append("unset");
				continue;
			}
			description.append(entry.getCode());
			if (entry.getWeatherName() != null) {
				description.append('(').append(entry.getWeatherName()).append(')');
			}
		}
		return description.toString();
	}

	/**
	 * 参数错误时的用法提示。
	 * Usage hint on invalid parameters.
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //weather <regionName(poeta, ishalgen, etc ...)> <value(0->12)> OR //weather reset");
	}
}
