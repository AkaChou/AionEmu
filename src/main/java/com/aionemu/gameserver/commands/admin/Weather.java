package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.world.WeatherTable;
import com.aionemu.gameserver.model.templates.zone.ZoneClassName;
import com.aionemu.gameserver.services.WeatherService;
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
	 *
	 * @param admin 执行 GM / Admin player
	 * @param params 地图名与天气码或 reset / Map name and weather code, or reset
	 */
	@Override
	public void execute(Player admin, String... params) {
		String regionName = null;
		if (params.length == 0) {
			int weatherCode = -1;
			List<ZoneInstance> zones = admin.getActiveRegion().getZones(admin);
			for (ZoneInstance regionZone : zones) {
				if (regionZone.getZoneTemplate().getZoneType() == ZoneClassName.WEATHER) {
					int weatherZoneId = DataManager.ZONE_DATA.getWeatherZoneId(regionZone.getZoneTemplate());
					weatherCode = GameRuntimeServices.weatherService().getWeatherCode(admin.getWorldId(), weatherZoneId);
					regionName = regionZone.getZoneTemplate().getXmlName();
					break;
				}
			} if (weatherCode == -1) {
				PacketSendUtility.sendMessage(admin, "No weather.");
			} else {
				PacketSendUtility.sendMessage(admin, "Weather code for region " + regionName + " is " + weatherCode);
			}
			return;
		} if (params.length > 2) {
			onFail(admin, null);
			return;
		}
		int weatherType = -1;
		regionName = new String(params[0]);
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
			if (worldMapType.name().toLowerCase().equals(regionName.toLowerCase())) {
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
				return;
			}
		} else {
			PacketSendUtility.sendMessage(admin, "Region " + regionName + " not found");
			return;
		}
	}

	/**
	 * 参数错误时的用法提示。
	 * Usage hint on invalid parameters.
	 *
	 * 玩家 / Player
	 * Failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //weather <regionName(poeta, ishalgen, etc ...)> <value(0->12)> OR //weather reset");
	}
}
